package com.vicmatskiv.pointblank.entity;

import com.google.gson.JsonObject;
import com.vicmatskiv.pointblank.client.EntityRendererBuilder;
import com.vicmatskiv.pointblank.client.effect.AttachedProjectileEffect;
import com.vicmatskiv.pointblank.client.effect.Effect;
import com.vicmatskiv.pointblank.client.effect.EffectBuilder;
import com.vicmatskiv.pointblank.client.effect.TrailEffect;
import com.vicmatskiv.pointblank.item.EffectBuilderInfo;
import com.vicmatskiv.pointblank.item.GunItem;
import com.vicmatskiv.pointblank.item.HurtingItem;
import com.vicmatskiv.pointblank.registry.EntityRegistry;
import com.vicmatskiv.pointblank.util.HitScan;
import com.vicmatskiv.pointblank.util.MiscUtil;
import com.vicmatskiv.pointblank.util.TimeUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;

public class GenericThrowableProjectile extends ThrowableProjectile implements ProjectileLike, IEntityAdditionalSpawnData {
   private static final float DEFAULT_GRAVITY = 0.05F;
   private static final float DEFAULT_WIDTH = 0.25F;
   private static final float DEFAULT_HEIGHT = 0.25F;
   private static final int DEFAULT_CLIENT_TRACKING_RANGE = 1024;
   private static final int DEFAULT_UPDATE_INTERVAL = 1;
   private static final int DEFAULT_LIFETIME_TICKS = 200;
   private static final double MIN_SPEED_THRESHOLD = 0.01D;
   private HurtingItem throwableItem;
   private ItemStack throwableItemStack;
   private double initialVelocityBlocksPerTick;
   private boolean isRicochet;
   private float gravity;
   private int maxLifetimeTicks;
   private List<EffectInfo> trailEffects;
   private List<EffectInfo> attachedEffects;
   private List<Effect> activeTrailEffects = Collections.emptyList();
   private List<Effect> activeAttachedEffects = Collections.emptyList();

   public static Builder builder() {
      return new Builder();
   }

   public GenericThrowableProjectile(EntityType<? extends GenericThrowableProjectile> entityType, Level level) {
      super(entityType, level);
   }

   public GenericThrowableProjectile(EntityType<? extends GenericThrowableProjectile> entityType, LivingEntity owner, Level level) {
      super(entityType, owner, level);
   }

   private void setInitialVelocityBlocksPerTick(double initialVelocityBlocksPerTick) {
      this.initialVelocityBlocksPerTick = initialVelocityBlocksPerTick;
   }

   private void setGravity(float gravity) {
      this.gravity = gravity;
   }

   protected float m_7139_() {
      return this.gravity;
   }

   public void setMaxLifetimeTicks(int maxLifetimeTicks) {
      this.maxLifetimeTicks = maxLifetimeTicks;
   }

   public ItemStack getItem() {
      return this.throwableItemStack;
   }

   public List<Effect> getActiveAttachedEffects() {
      return this.activeTrailEffects;
   }

   public float getProgress(float partialTick) {
      return 0.0F;
   }

   public long getElapsedTimeMillis() {
      return 0L;
   }

   public double getInitialVelocityBlocksPerTick() {
      return this.initialVelocityBlocksPerTick;
   }

   public void launchAtTargetEntity(LivingEntity player, HitResult hitResult, Entity targetEntity) {
      Vec3 hitLocation = hitResult.m_82450_();
      Vec3 muzzleWorldPos = this.m_20182_();
      Vec3 eyePos = player.m_146892_();
      Vec3 viewHitVector = hitLocation.m_82546_(eyePos);
      Vec3 spawnOffset = muzzleWorldPos.m_82546_(eyePos);
      Vec3 direction = viewHitVector.m_82546_(spawnOffset).m_82541_();
      this.m_6686_(direction.f_82479_, direction.f_82480_, direction.f_82481_, (float)this.getInitialVelocityBlocksPerTick(), 0.0F);
   }

   public void launchAtLookTarget(LivingEntity player, double inaccuracy, long seed) {
      HitResult hitScanTarget = HitScan.getNearestObjectInCrosshair(player, 0.0F, 150.0D, inaccuracy, seed, (block) -> {
         return false;
      }, (block) -> {
         return false;
      }, new ArrayList());
      Vec3 hitLocation = hitScanTarget.m_82450_();
      Vec3 muzzleWorldPos = this.m_20182_();
      Vec3 eyePos = player.m_146892_();
      Vec3 viewHitVector = hitLocation.m_82546_(eyePos);
      Vec3 spawnOffset = muzzleWorldPos.m_82546_(eyePos);
      Vec3 direction = viewHitVector.m_82546_(spawnOffset).m_82541_();
      this.m_6686_(direction.f_82479_, direction.f_82480_, direction.f_82481_, (float)this.getInitialVelocityBlocksPerTick(), 0.0F);
   }

   private double getSpeedSqr() {
      return this.m_20184_().m_82556_();
   }

   protected void m_8060_(BlockHitResult blockHitResult) {
      if (this.isRicochet) {
         BlockPos resultPos = blockHitResult.m_82425_();
         Level level = MiscUtil.getLevel(this);
         BlockState state = level.m_8055_(resultPos);
         SoundEvent event = state.m_60734_().m_49962_(state).m_56776_();
         if (this.getSpeedSqr() > 0.01D) {
            level.m_6263_((Player)null, blockHitResult.m_82450_().f_82479_, blockHitResult.m_82450_().f_82480_, blockHitResult.m_82450_().f_82481_, event, SoundSource.AMBIENT, 1.0F, 1.0F);
         }

         this.ricochet(blockHitResult.m_82434_());
      } else {
         Entity owner = this.m_19749_();
         if (owner instanceof LivingEntity) {
            LivingEntity player = (LivingEntity)owner;
            this.throwableItem.handleBlockHit(player, blockHitResult, this);
         }
      }

   }

   protected void m_5790_(EntityHitResult entityHitResult) {
      Entity owner = this.m_19749_();
      if (this.isRicochet) {
         if (owner instanceof LivingEntity && this.getSpeedSqr() > 0.01D) {
            Entity entity = entityHitResult.m_82443_();
            if (!MiscUtil.isProtected(entity)) {
               entity.m_6469_(entity.m_269291_().m_269390_(this, this.m_19749_()), 0.5F);
            }
         }

         this.ricochet(Direction.m_122366_(this.m_20184_().m_7096_(), this.m_20184_().m_7098_(), this.m_20184_().m_7094_()).m_122424_(), 0.3D, 1.0D, 0.3D);
      } else if (owner instanceof LivingEntity) {
         LivingEntity player = (LivingEntity)owner;
         this.throwableItem.hurtEntity(player, entityHitResult, this, this.throwableItemStack);
      }

   }

   private void ricochet(Direction direction) {
      this.ricochet(direction, 1.0D, 1.0D, 1.0D);
   }

   private void ricochet(Direction direction, double mx, double my, double mz) {
      Axis axis = direction.m_122434_();
      Vec3 delta = this.m_20184_();
      delta = delta.m_82542_(axis == Axis.X ? -0.5D : 0.7D, axis == Axis.Y ? -0.2D : 0.7D, axis == Axis.Z ? -0.5D : 0.7D);
      if (axis == Axis.Y && delta.f_82480_ < (double)this.m_7139_()) {
         delta = new Vec3(delta.f_82479_, 0.0D, delta.f_82481_);
      }

      this.m_20256_(delta.m_82542_(mx, my, mz));
   }

   public boolean m_20068_() {
      return false;
   }

   public void writeSpawnData(FriendlyByteBuf buffer) {
      buffer.m_130055_(this.throwableItemStack);
   }

   public void readSpawnData(FriendlyByteBuf buffer) {
      this.throwableItemStack = buffer.m_130267_();
   }

   public void m_8119_() {
      super.m_8119_();
      if (this.f_19797_ >= this.maxLifetimeTicks) {
         this.doDiscard();
      }

      if (MiscUtil.isClientSide(this)) {
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
         Vec3 dm = this.m_20184_();
         Iterator var2 = this.activeTrailEffects.iterator();

         while(var2.hasNext()) {
            Effect trailEffect = (Effect)var2.next();
            ((TrailEffect)trailEffect).launchNext(this, new Vec3(this.m_20185_(), this.m_20186_(), this.m_20189_()), dm);
         }
      }

   }

   public void doDiscard() {
      if (!MiscUtil.isClientSide(this)) {
         if (this.throwableItem != null) {
            this.throwableItem.discardProjectile(this);
         }

         this.m_146870_();
      }

   }

   protected void m_8097_() {
   }

   public Packet<ClientGamePacketListener> m_5654_() {
      return NetworkHooks.getEntitySpawningPacket(this);
   }

   public static class Builder implements EntityBuilder<Builder, GenericThrowableProjectile> {
      private String name;
      private float width = 0.25F;
      private float height = 0.25F;
      private double initialVelocityBlocksPerSecond;
      private int maxLifetimeTicks = 200;
      private float gravity;
      private boolean isRicochet;
      private final List<EffectBuilderInfo> effectBuilderSuppliers = new ArrayList();
      private Supplier<EntityRendererBuilder<?, Entity, EntityRenderer<Entity>>> rendererBuilder;
      private Supplier<HurtingItem> hurtingItem;

      private Builder() {
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
         this.maxLifetimeTicks = (int)TimeUnit.MILLISECOND.toTicks(lifetimeMillis);
         return this;
      }

      public Builder withSize(float width, float height) {
         this.width = width;
         this.height = height;
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

      public Builder withRicochet(boolean isRicochet) {
         this.isRicochet = isRicochet;
         return this;
      }

      public Builder withGravity(boolean isGravityEnabled) {
         this.gravity = isGravityEnabled ? 0.05F : 0.0F;
         return this;
      }

      public Builder withGravity(double gravity) {
         this.gravity = Mth.m_14036_((float)gravity, -1.0F, 1.0F);
         return this;
      }

      public Builder withEffect(EffectBuilderInfo effectInfo) {
         this.effectBuilderSuppliers.add(effectInfo);
         return this;
      }

      public EntityTypeExt getEntityTypeExt() {
         return EntityTypeExt.PROJECTILE;
      }

      public EntityType.Builder<GenericThrowableProjectile> getEntityTypeBuilder() {
         return EntityType.Builder.m_20704_(this::build, MobCategory.MISC).m_20699_(this.width, this.height).m_20702_(1024).m_20698_().m_20719_().m_20717_(1);
      }

      public GenericThrowableProjectile build(EntityType<?> entityType, Level level) {
         GenericThrowableProjectile projectile = new GenericThrowableProjectile(entityType, level);
         if (level.f_46443_) {
            this.initEffects(projectile);
         }

         if (this.hurtingItem != null) {
            projectile.throwableItem = (HurtingItem)this.hurtingItem.get();
            projectile.throwableItemStack = new ItemStack(projectile.throwableItem);
         }

         projectile.maxLifetimeTicks = this.maxLifetimeTicks;
         projectile.isRicochet = this.isRicochet;
         projectile.setInitialVelocityBlocksPerTick(this.initialVelocityBlocksPerSecond * 0.05000000074505806D);
         projectile.setMaxLifetimeTicks(this.maxLifetimeTicks);
         projectile.m_20242_(MiscUtil.isNearlyZero((double)this.gravity));
         projectile.setGravity(this.gravity);
         return projectile;
      }

      public GenericThrowableProjectile build(Level level) {
         Supplier<EntityType<?>> entityTypeSupplier = EntityRegistry.getTypeByName(this.name);
         return this.build((EntityType)entityTypeSupplier.get(), level);
      }

      public void initEffects(GenericThrowableProjectile projectile) {
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

      public EntityRenderer<Entity> createEntityRenderer(Context context) {
         return ((EntityRendererBuilder)this.rendererBuilder.get()).build(context);
      }

      public Builder withJsonObject(JsonObject obj) {
         throw new UnsupportedOperationException();
      }
   }
}
