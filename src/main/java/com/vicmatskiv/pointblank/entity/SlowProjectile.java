package com.vicmatskiv.pointblank.entity;

import com.google.gson.JsonObject;
import com.vicmatskiv.pointblank.client.EntityRendererBuilder;
import com.vicmatskiv.pointblank.client.effect.AttachedProjectileEffect;
import com.vicmatskiv.pointblank.client.effect.Effect;
import com.vicmatskiv.pointblank.client.effect.EffectBuilder;
import com.vicmatskiv.pointblank.client.effect.TrailEffect;
import com.vicmatskiv.pointblank.item.AmmoItem;
import com.vicmatskiv.pointblank.item.EffectBuilderInfo;
import com.vicmatskiv.pointblank.item.GunItem;
import com.vicmatskiv.pointblank.item.HurtingItem;
import com.vicmatskiv.pointblank.registry.EntityRegistry;
import com.vicmatskiv.pointblank.util.DirectAttackTrajectory;
import com.vicmatskiv.pointblank.util.HitScan;
import com.vicmatskiv.pointblank.util.MiscUtil;
import com.vicmatskiv.pointblank.util.TimeUnit;
import com.vicmatskiv.pointblank.util.TopDownAttackTrajectory;
import com.vicmatskiv.pointblank.util.Trajectory;
import com.vicmatskiv.pointblank.util.TrajectoryProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.network.NetworkHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SlowProjectile extends AbstractHurtingProjectile implements ProjectileLike, IEntityAdditionalSpawnData {
   private static final Logger LOGGER = LogManager.getLogger("pointblank");
   private static final double MAX_RENDER_DISTANCE_SQR = 40000.0D;
   private static final float MAX_HIT_SCAN_DISTANCE = 10.0F;
   static final float DEFAULT_MAX_DISTANCE = 150.0F;
   private static final double DEFAULT_GRAVITY = 0.05D;
   private static final EntityDataAccessor<OptionalInt> DATA_ATTACHED_TO_TARGET;
   private static final EntityDataAccessor<Boolean> DATA_SHOT_AT_ANGLE;
   private double initialVelocityBlocksPerTick;
   private int life;
   private int lifetime;
   private HitResult hitScanTarget;
   private double gravity;
   @Nullable
   private LivingEntity attachedToEntity;
   private float initialAngle;
   private long startTimeMillis;
   private List<EffectInfo> trailEffects;
   private List<EffectInfo> attachedEffects;
   private List<Effect> activeTrailEffects = Collections.emptyList();
   private List<Effect> activeAttachedEffects = Collections.emptyList();
   private Trajectory<?> trajectory;
   private Entity targetEntity;
   public HurtingItem hurtingItem;
   private ItemStack hurtingItemStack;

   public static Builder builder() {
      return new Builder();
   }

   public SlowProjectile(EntityType<? extends SlowProjectile> entityType, Level level) {
      super(entityType, level);
      this.initialAngle = this.f_19796_.m_188501_() * 360.0F;
      this.startTimeMillis = System.currentTimeMillis();
   }

   public void launchAtLookTarget(LivingEntity entity, double inaccuracy, long seed) {
      this.hitScanTarget = HitScan.getNearestObjectInCrosshair(entity, 0.0F, 150.0D, inaccuracy, seed, (block) -> {
         return false;
      }, (block) -> {
         return false;
      }, new ArrayList());
      Vec3 hitLocation = this.hitScanTarget.m_82450_();
      Vec3 muzzleWorldPos = this.m_20182_();
      Vec3 eyePos = entity.m_146892_();
      Vec3 viewHitVector = hitLocation.m_82546_(eyePos);
      Vec3 spawnOffset = muzzleWorldPos.m_82546_(eyePos);
      Vec3 direction = viewHitVector.m_82546_(spawnOffset).m_82541_();
      this.shoot(direction.f_82479_, direction.f_82480_, direction.f_82481_, this.getInitialVelocityBlocksPerTick());
   }

   public void launchAtTargetEntity(LivingEntity player, HitResult hitResult, Entity targetEntity) {
      this.hitScanTarget = hitResult;
      this.targetEntity = targetEntity;
      Vec3 hitLocation = this.hitScanTarget.m_82450_();
      Vec3 muzzleWorldPos = this.m_20182_();
      Vec3 eyePos = player.m_146892_();
      Vec3 viewHitVector = hitLocation.m_82546_(eyePos);
      Vec3 spawnOffset = muzzleWorldPos.m_82546_(eyePos);
      Vec3 direction = viewHitVector.m_82546_(spawnOffset).m_82541_();
      this.shoot(direction.f_82479_, direction.f_82480_, direction.f_82481_, this.getInitialVelocityBlocksPerTick());
   }

   public void shoot(double dx, double dy, double dz, double initialSpeed) {
      Vec3 deltaMovement = (new Vec3(dx, dy, dz)).m_82541_().m_82490_(initialSpeed);
      this.m_20256_(deltaMovement);
      double horizontalDistance = deltaMovement.m_165924_();
      this.m_146922_((float)(Mth.m_14136_(deltaMovement.f_82479_, deltaMovement.f_82481_) * 57.2957763671875D));
      this.m_146926_((float)(Mth.m_14136_(deltaMovement.f_82480_, horizontalDistance) * 57.2957763671875D));
      this.f_19859_ = this.m_146908_();
      this.f_19860_ = this.m_146909_();
      LOGGER.debug("{} initializing projectile trajectory", System.currentTimeMillis() % 100000L);
      this.initTrajectory(this.hitScanTarget.m_82450_());
      LOGGER.debug("{} performed projectile shot", System.currentTimeMillis() % 100000L);
   }

   private void initTrajectory(Vec3 targetLocation) {
      Vec3 startPosition = new Vec3(this.m_20185_(), this.m_20186_(), this.m_20189_());
      if (this.hurtingItem instanceof TrajectoryProvider) {
         TrajectoryProvider trajectoryProvider = (TrajectoryProvider)this.hurtingItem;
         this.trajectory = trajectoryProvider.createTrajectory(this.m_9236_(), startPosition, targetLocation);
      }

      if (this.trajectory == null) {
         this.trajectory = new DirectAttackTrajectory(startPosition, this.m_20184_(), this.gravity);
      }

   }

   public int getLife() {
      return this.life;
   }

   public int getLifetime() {
      return this.lifetime;
   }

   protected void m_8097_() {
      this.f_19804_.m_135372_(DATA_ATTACHED_TO_TARGET, OptionalInt.empty());
      this.f_19804_.m_135372_(DATA_SHOT_AT_ANGLE, false);
   }

   public boolean m_6783_(double distance) {
      return distance < 40000.0D;
   }

   public void m_8119_() {
      Level level = MiscUtil.getLevel(this);
      if (MiscUtil.isClientSide(this)) {
         Vec3 dm = this.m_20184_();
         Iterator var3 = this.getActiveTrailEffects().iterator();

         while(var3.hasNext()) {
            Effect trailEffect = (Effect)var3.next();
            ((TrailEffect)trailEffect).launchNext(this, new Vec3(this.m_20185_(), this.m_20186_(), this.m_20189_()), dm);
         }
      }

      if (level.f_46443_ && this.attachedToEntity == null) {
         int entityId = ((OptionalInt)this.f_19804_.m_135370_(DATA_ATTACHED_TO_TARGET)).orElse(-1);
         if (entityId >= 0) {
            Entity entity = MiscUtil.getLevel(this).m_6815_(entityId);
            if (entity instanceof LivingEntity) {
               this.attachedToEntity = (LivingEntity)entity;
            }
         }
      }

      if (this.life > this.getLifetime()) {
         this.m_146870_();
      }

      if (this.trajectory != null) {
         this.trajectory.tick();
         if (!level.f_46443_ && this.trajectory.isCompleted()) {
            this.explode();
         }
      }

      if (level.f_46443_) {
         this.activeAttachedEffects = this.attachedEffects.stream().filter((ei) -> {
            return ei.predicate().test(this);
         }).map((ei) -> {
            return ei.effect();
         }).toList();
         this.activeTrailEffects = this.trailEffects.stream().filter((ei) -> {
            return ei.predicate().test(this);
         }).map((ei) -> {
            return ei.effect();
         }).toList();
      }

      if (this.targetEntity != null) {
         this.trajectory.setTargetPosition(this.targetEntity.m_20318_(0.0F));
      }

      Entity entity = this.m_19749_();
      if (!level.f_46443_ && (entity != null && entity.m_213877_() || !MiscUtil.getLevel(this).m_46805_(this.m_20183_()))) {
         this.m_146870_();
      } else {
         HitResult hitresult = this.getHitResultOnMoveOrViewVector();
         if (hitresult.m_6662_() != Type.MISS && !ForgeEventFactory.onProjectileImpact(this, hitresult)) {
            this.m_6532_(hitresult);
         }

         if (this.trajectory != null) {
            this.m_20256_(this.trajectory.getDeltaMovement());
            this.m_146884_(this.trajectory.getEndOfTickPosition());
         } else {
            Vec3 deltaMovement = this.m_20184_();
            double d0 = this.m_20185_() + deltaMovement.f_82479_;
            double d1 = this.m_20186_() + deltaMovement.f_82480_;
            double d2 = this.m_20189_() + deltaMovement.f_82481_;
            this.m_20334_(deltaMovement.f_82479_, deltaMovement.f_82480_ - this.getGravity(), deltaMovement.f_82481_);
            this.m_6034_(d0, d1, d2);
         }
      }

      if (!level.f_46443_ && this.life > this.getLifetime()) {
         this.explode();
      }

      ++this.life;
   }

   public void m_6453_(double x, double y, double z, float xRot, float yRot, int p_19901_, boolean p_19902_) {
   }

   public void m_20334_(double dx, double dy, double dz) {
      super.m_20334_(dx, dy, dz);
      double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
      this.m_146922_((float)(Mth.m_14136_(dx, dz) * 57.29577951308232D));
      this.m_146926_((float)(Mth.m_14136_(dy, horizontalDistance) * 57.29577951308232D));
   }

   public void m_20256_(Vec3 dm) {
      super.m_20256_(dm);
      double horizontalDistance = Math.sqrt(dm.f_82479_ * dm.f_82479_ + dm.f_82481_ * dm.f_82481_);
      this.m_146922_((float)(Mth.m_14136_(dm.f_82479_, dm.f_82481_) * 57.29577951308232D));
      this.m_146926_((float)(Mth.m_14136_(dm.f_82480_, horizontalDistance) * 57.29577951308232D));
   }

   private HitResult getHitResultOnMoveOrViewVector() {
      Entity owner = this.m_19749_();
      HitResult hitResult = ProjectileUtil.m_278158_(this, this::m_5603_);
      if (owner != null) {
         Vec3 ownerPos = owner.m_20182_();
         Vec3 originalHitPos = hitResult.m_82450_();
         double distanceToOwner = originalHitPos.m_82554_(ownerPos);
         if (distanceToOwner <= 10.0D && this.hitScanTarget != null && this.hitScanTarget.m_82450_().m_82554_(ownerPos) <= 10.0D) {
            hitResult = this.hitScanTarget;
         }
      }

      return hitResult;
   }

   protected boolean m_5603_(Entity entity) {
      return entity != this.m_19749_() && super.m_5603_(entity) && !entity.f_19794_;
   }

   protected float getWaterInertia() {
      return 0.6F;
   }

   protected EntityHitResult findHitEntity(Vec3 p_36758_, Vec3 p_36759_) {
      return ProjectileUtil.m_37304_(MiscUtil.getLevel(this), this, p_36758_, p_36759_, this.m_20191_().m_82369_(this.m_20184_()).m_82400_(1.0D), this::m_5603_);
   }

   protected void m_6532_(HitResult result) {
      if (result.m_6662_() == Type.MISS || !ForgeEventFactory.onProjectileImpact(this, result)) {
         super.m_6532_(result);
         this.m_146870_();
      }

   }

   private void explode() {
      if (this.hurtingItem != null) {
         this.hurtingItem.explodeProjectile(this);
      }

      this.m_146870_();
   }

   protected void m_5790_(EntityHitResult entityHitResult) {
      super.m_5790_(entityHitResult);
      Entity owner = this.m_19749_();
      HurtingItem var4 = this.hurtingItem;
      if (var4 instanceof AmmoItem) {
         AmmoItem ammoItem = (AmmoItem)var4;
         if (owner instanceof LivingEntity) {
            this.hurtingItem.hurtEntity((LivingEntity)owner, entityHitResult, this, new ItemStack(ammoItem));
         }
      }

   }

   protected void m_8060_(BlockHitResult blockHitResult) {
      BlockPos blockpos = new BlockPos(blockHitResult.m_82425_());
      MiscUtil.getLevel(this).m_8055_(blockpos).m_60682_(MiscUtil.getLevel(this), blockpos, this);
      Entity owner = this.m_19749_();
      if (this.hurtingItem != null && !MiscUtil.isClientSide(this) && owner instanceof LivingEntity) {
         this.hurtingItem.handleBlockHit((Player)owner, blockHitResult, this);
      }

      super.m_8060_(blockHitResult);
   }

   public boolean isShotAtAngle() {
      return (Boolean)this.f_19804_.m_135370_(DATA_SHOT_AT_ANGLE);
   }

   public void m_7380_(CompoundTag tag) {
      super.m_7380_(tag);
      tag.m_128405_("Life", this.life);
      tag.m_128405_("LifeTime", this.getLifetime());
      tag.m_128379_("ShotAtAngle", (Boolean)this.f_19804_.m_135370_(DATA_SHOT_AT_ANGLE));
   }

   public void m_7378_(CompoundTag compoundTag) {
      this.m_146870_();
   }

   public boolean m_6097_() {
      return false;
   }

   public Packet<ClientGamePacketListener> m_5654_() {
      return NetworkHooks.getEntitySpawningPacket(this);
   }

   public void writeSpawnData(FriendlyByteBuf buffer) {
      Vec3 movement = this.m_20184_();
      buffer.writeInt(this.getLifetime());
      buffer.writeDouble(this.m_20185_());
      buffer.writeDouble(this.m_20186_());
      buffer.writeDouble(this.m_20189_());
      buffer.writeDouble(movement.f_82479_);
      buffer.writeDouble(movement.f_82480_);
      buffer.writeDouble(movement.f_82481_);
      buffer.writeFloat(this.m_146909_());
      buffer.writeFloat(this.m_6080_());
      Vec3 targetLocation;
      if (this.hitScanTarget != null) {
         targetLocation = this.hitScanTarget.m_82450_();
      } else {
         targetLocation = Vec3.f_82478_;
      }

      buffer.writeDouble(targetLocation.f_82479_);
      buffer.writeDouble(targetLocation.f_82480_);
      buffer.writeDouble(targetLocation.f_82481_);
      buffer.writeInt(this.targetEntity != null ? this.targetEntity.m_19879_() : -1);
   }

   public void readSpawnData(FriendlyByteBuf buffer) {
      this.setLifetime(buffer.readInt());
      double x = buffer.readDouble();
      double y = buffer.readDouble();
      double z = buffer.readDouble();
      double dx = buffer.readDouble();
      double dy = buffer.readDouble();
      double dz = buffer.readDouble();
      float xRot = buffer.readFloat();
      float yRot = buffer.readFloat();
      double hitLocationX = buffer.readDouble();
      double hitLocationY = buffer.readDouble();
      double hitLocationZ = buffer.readDouble();
      int entityId = buffer.readInt();
      this.m_19890_(x, y, z, yRot, xRot);
      this.m_20334_(dx, dy, dz);
      this.m_5616_(yRot);
      this.m_5618_(yRot);
      if (entityId >= 0) {
         this.targetEntity = this.m_9236_().m_6815_(entityId);
      }

      this.initTrajectory(new Vec3(hitLocationX, hitLocationY, hitLocationZ));
   }

   public float getProgress(float partialTick) {
      return ((float)this.life + partialTick) / (float)this.getLifetime();
   }

   public long getElapsedTimeMillis() {
      return System.currentTimeMillis() - this.startTimeMillis;
   }

   public float getInitialAngle() {
      return this.initialAngle;
   }

   public ItemStack getItem() {
      return this.hurtingItemStack;
   }

   public void setLifetime(int lifetime) {
      this.lifetime = lifetime;
   }

   public double getGravity() {
      return this.gravity;
   }

   private void setGravity(double gravity) {
      this.gravity = gravity;
   }

   public double getInitialVelocityBlocksPerTick() {
      return this.initialVelocityBlocksPerTick;
   }

   private void setInitialVelocityBlocksPerTick(double initialVelocityBlocksPerTick) {
      this.initialVelocityBlocksPerTick = initialVelocityBlocksPerTick;
   }

   public List<Effect> getActiveAttachedEffects() {
      return this.activeAttachedEffects;
   }

   public List<Effect> getActiveTrailEffects() {
      return this.activeTrailEffects;
   }

   public void setTrailEffects(List<EffectInfo> trailEffects) {
      this.trailEffects = trailEffects;
   }

   public void setAttachedEffects(List<EffectInfo> attachedEffects) {
      this.attachedEffects = attachedEffects;
   }

   public Trajectory<?> getTrajectory() {
      return this.trajectory;
   }

   public static Predicate<ProjectileLike> topDownTrajectoryPhasePredicate(Predicate<TopDownAttackTrajectory> tp) {
      return (projectile) -> {
         boolean var10000;
         if (projectile.getTrajectory() != null) {
            Trajectory patt29669$temp = projectile.getTrajectory();
            if (patt29669$temp instanceof TopDownAttackTrajectory) {
               TopDownAttackTrajectory tdat = (TopDownAttackTrajectory)patt29669$temp;
               if (tp.test(tdat)) {
                  var10000 = true;
                  return var10000;
               }
            }
         }

         var10000 = false;
         return var10000;
      };
   }

   static {
      DATA_ATTACHED_TO_TARGET = SynchedEntityData.m_135353_(SlowProjectile.class, EntityDataSerializers.f_135044_);
      DATA_SHOT_AT_ANGLE = SynchedEntityData.m_135353_(SlowProjectile.class, EntityDataSerializers.f_135035_);
   }

   public static class Builder implements EntityBuilder<Builder, SlowProjectile> {
      private static final float DEFAULT_WIDTH = 0.25F;
      private static final float DEFAULT_HEIGHT = 0.25F;
      private static final int DEFAULT_TRACKING_RANGE = 1024;
      private static final int DEFAULT_CLIENT_TRACKING_RANGE = 1024;
      private static final int DEFAULT_UPDATE_INTERVAL = 1;
      private static final int DEFAULT_LIFETIME_TICKS = 200;
      private String name;
      private float width = 0.25F;
      private float height = 0.25F;
      private int clientTrackingRange = 1024;
      private int trackingRange = 1024;
      private int updateInterval = 50;
      private double initialVelocityBlocksPerSecond;
      private int lifetimeTicks = 200;
      private double gravity;
      private List<EffectBuilderInfo> effectBuilderSuppliers = new ArrayList();
      private Supplier<EntityRendererBuilder<?, Entity, EntityRenderer<Entity>>> rendererBuilder;
      private Supplier<HurtingItem> hurtingItem;

      private Builder() {
         this.updateInterval = 1;
      }

      public String getName() {
         return this.name;
      }

      public Builder withItem(Supplier<Item> hurtingItem) {
         this.hurtingItem = () -> {
            return (HurtingItem)hurtingItem.get();
         };
         return this;
      }

      public Builder withName(String name) {
         this.name = name;
         return this;
      }

      public Builder withMaxLifetime(long lifetimeMillis) {
         this.lifetimeTicks = (int)TimeUnit.MILLISECOND.toTicks(lifetimeMillis);
         return this;
      }

      public Builder withSize(float width, float height) {
         this.width = width;
         this.height = height;
         return this;
      }

      public EntityBuilder<?, ?> withRicochet(boolean ricochet) {
         return this;
      }

      public Builder withInitialVelocity(double initialVelocityBlocksPerSecond) {
         this.initialVelocityBlocksPerSecond = initialVelocityBlocksPerSecond;
         return this;
      }

      public Builder withRenderer(Supplier<EntityRendererBuilder<?, Entity, EntityRenderer<Entity>>> rendererBuilder2) {
         this.rendererBuilder = rendererBuilder2;
         return this;
      }

      public Builder withGravity(boolean isGravityEnabled) {
         this.gravity = isGravityEnabled ? 0.05D : 0.0D;
         return this;
      }

      public Builder withGravity(double gravity) {
         this.gravity = Mth.m_14008_(gravity, -1.0D, 1.0D);
         return this;
      }

      public Builder withEffect(EffectBuilderInfo effectInfo) {
         this.effectBuilderSuppliers.add(effectInfo);
         return this;
      }

      public EntityTypeExt getEntityTypeExt() {
         return EntityTypeExt.PROJECTILE;
      }

      public EntityType.Builder<SlowProjectile> getEntityTypeBuilder() {
         return EntityType.Builder.m_20704_(this::create, MobCategory.MISC).m_20699_(this.width, this.height).m_20702_(this.clientTrackingRange).setTrackingRange(this.trackingRange).m_20717_(this.updateInterval).setShouldReceiveVelocityUpdates(false);
      }

      public SlowProjectile create(EntityType<SlowProjectile> entityType, Level level) {
         SlowProjectile projectile = new SlowProjectile(entityType, level);
         if (level.f_46443_) {
            this.initEffects(projectile);
         }

         projectile.setGravity(this.gravity);
         if (this.hurtingItem != null) {
            projectile.hurtingItem = (HurtingItem)this.hurtingItem.get();
            projectile.hurtingItemStack = new ItemStack(projectile.hurtingItem);
         }

         return projectile;
      }

      public SlowProjectile build(Level level) {
         Supplier<EntityType<?>> entityType = EntityRegistry.getTypeByName(this.name);
         SlowProjectile projectile = new SlowProjectile((EntityType)entityType.get(), level);
         projectile.setInitialVelocityBlocksPerTick(this.initialVelocityBlocksPerSecond * 0.05000000074505806D);
         projectile.setLifetime(this.lifetimeTicks);
         projectile.m_20242_(MiscUtil.isNearlyZero(this.gravity));
         projectile.setGravity(this.gravity);
         if (this.hurtingItem != null) {
            projectile.hurtingItem = (HurtingItem)this.hurtingItem.get();
            projectile.hurtingItemStack = new ItemStack(projectile.hurtingItem);
         }

         if (level.f_46443_) {
            this.initEffects(projectile);
         }

         return projectile;
      }

      public void initEffects(SlowProjectile projectile) {
         List<EffectInfo> trailEffects = new ArrayList();
         List<EffectInfo> attachedEffects = new ArrayList();
         GunItem.FirePhase phase = GunItem.FirePhase.FLYING;
         Iterator var5 = this.effectBuilderSuppliers.iterator();

         while(var5.hasNext()) {
            EffectBuilderInfo effectBuilderInfo = (EffectBuilderInfo)var5.next();
            EffectBuilder<?, ?> effectBuilder = (EffectBuilder)effectBuilderInfo.effectSupplier().get();
            EffectBuilder.Context context;
            if (effectBuilder.getCompatiblePhases().contains(GunItem.FirePhase.FLYING)) {
               context = new EffectBuilder.Context();
               TrailEffect effect = (TrailEffect)effectBuilder.build(context);
               trailEffects.add(new EffectInfo(effect, effectBuilderInfo.predicate()));
            } else {
               if (!(effectBuilder instanceof AttachedProjectileEffect.Builder)) {
                  throw new IllegalStateException("Effect builder " + effectBuilder + " is not compatible with phase '" + phase + "'. Check how you construct projectile: " + this.getName());
               }

               context = new EffectBuilder.Context();
               AttachedProjectileEffect effect = (AttachedProjectileEffect)effectBuilder.build(context);
               attachedEffects.add(new EffectInfo(effect, effectBuilderInfo.predicate()));
            }
         }

         projectile.trailEffects = Collections.unmodifiableList(trailEffects);
         projectile.attachedEffects = Collections.unmodifiableList(attachedEffects);
      }

      public boolean hasRenderer() {
         return this.rendererBuilder != null;
      }

      @OnlyIn(Dist.CLIENT)
      public EntityRenderer<Entity> createEntityRenderer(Context context) {
         return ((EntityRendererBuilder)this.rendererBuilder.get()).build(context);
      }

      public Builder withJsonObject(JsonObject obj) {
         throw new UnsupportedOperationException();
      }
   }
}
