package com.vicmatskiv.pointblank.item;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.vicmatskiv.pointblank.Config;
import com.vicmatskiv.pointblank.Enableable;
import com.vicmatskiv.pointblank.client.effect.EffectBuilder;
import com.vicmatskiv.pointblank.config.ConfigManager;
import com.vicmatskiv.pointblank.config.Configurable;
import com.vicmatskiv.pointblank.entity.EntityExt;
import com.vicmatskiv.pointblank.event.BlockHitEvent;
import com.vicmatskiv.pointblank.explosion.CustomExplosion;
import com.vicmatskiv.pointblank.feature.DamageFeature;
import com.vicmatskiv.pointblank.feature.FireModeFeature;
import com.vicmatskiv.pointblank.network.CustomClientBoundExplosionPacket;
import com.vicmatskiv.pointblank.network.Network;
import com.vicmatskiv.pointblank.network.SpawnParticlePacket;
import com.vicmatskiv.pointblank.registry.EffectRegistry;
import com.vicmatskiv.pointblank.registry.SoundRegistry;
import com.vicmatskiv.pointblank.util.HitScan;
import com.vicmatskiv.pointblank.util.JsonUtil;
import com.vicmatskiv.pointblank.util.MiscUtil;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.PacketDistributor;

public abstract class HurtingItem extends Item implements Enableable {
   private static final float DEFAULT_EXPLOSION_SOUND_VOLUME = 4.0F;
   public static final double DEFAULT_MAX_SHOOTING_DISTANCE = 200.0D;
   public static final float DEFAULT_DAMAGE = 5.0F;
   public static final float DEFAULT_HEADSHOT_SOUND_VOLUME = 3.0F;
   public static final float DEFAULT_LIGHT_DAMAGE_SOUND_VOLUME = 3.0F;
   public static final float DEFAULT_HEAVY_DAMAGE_SOUND_VOLUME = 3.0F;
   public static final float MAX_LIGHT_DAMAGE = 15.0F;
   public static final long DEFAULT_ENTITY_HIT_SOUND_COOLDOWN = 100L;
   protected ExplosionDescriptor explosionDescriptor;
   protected double maxShootingDistance;
   protected float damage;
   protected boolean isEnabled;
   protected float headshotSoundVolume;
   protected Supplier<SoundEvent> headshotSound;
   protected float lightDamageSoundVolume;
   protected Supplier<SoundEvent> lightDamageSound;
   protected float heavyDamageSoundVolume;
   protected Supplier<SoundEvent> heavyDamageSound;
   protected long entityHitSoundCooldown = 100L;

   public HurtingItem(Properties properties, Builder<?> builder) {
      super(properties);
      if (builder != null) {
         this.explosionDescriptor = builder.explosionDescriptor;
         this.maxShootingDistance = builder.maxShootingDistance;
         this.isEnabled = (Boolean)builder.configOptionEnabled.get();
         this.damage = ((Double)builder.configOptionDamage.get()).floatValue();
         this.headshotSound = builder.headshotSound;
         this.headshotSoundVolume = builder.headshotSoundVolume;
         this.lightDamageSound = builder.lightDamageSound;
         this.lightDamageSoundVolume = builder.lightDamageSoundVolume;
         this.heavyDamageSound = builder.heavyDamageSound;
         this.heavyDamageSoundVolume = builder.heavyDamageSoundVolume;
      }

   }

   public float getDamage() {
      return this.damage;
   }

   public ExplosionDescriptor getExplosion() {
      return this.explosionDescriptor;
   }

   public boolean isEnabled() {
      return this.isEnabled;
   }

   public float hurtEntity(LivingEntity player, EntityHitResult entityHitResult, Entity projectile, ItemStack gunStack) {
      float damage = 0.0F;
      Vec3 pos;
      if (this.getExplosion() != null) {
         Vec3 hitLocation = entityHitResult.m_82443_().m_20318_(0.0F);
         if (MiscUtil.isProtected(entityHitResult.m_82443_())) {
            pos = entityHitResult.m_82443_().m_20182_();
            MiscUtil.getLevel(player).m_6263_((Player)null, pos.f_82479_, pos.f_82480_, pos.f_82481_, SoundEvents.f_11785_, SoundSource.AMBIENT, 3.0F, 1.0F);
         } else {
            this.explode(MiscUtil.getLevel(player), projectile, (DamageSource)null, hitLocation.f_82479_, hitLocation.f_82480_, hitLocation.f_82481_);
         }
      } else {
         Entity hitEntity = entityHitResult.m_82443_();
         if (MiscUtil.isProtected(hitEntity)) {
            pos = hitEntity.m_20182_();
            MiscUtil.getLevel(player).m_6263_((Player)null, pos.f_82479_, pos.f_82480_, pos.f_82481_, SoundEvents.f_11785_, SoundSource.AMBIENT, 3.0F, 1.0F);
         } else {
            boolean isHeadshot = false;
            if (hitEntity instanceof LivingEntity) {
               LivingEntity livingEntity = (LivingEntity)hitEntity;
               isHeadshot = HitScan.isHeadshot(livingEntity, entityHitResult.m_82450_());
            }

            DamageSource damageSource;
            if (player instanceof Player) {
               damageSource = player.m_269291_().m_269075_((Player)player);
            } else {
               DamageSources damageSources = MiscUtil.getLevel(player).m_269111_();
               damageSource = damageSources.m_269264_();
            }

            int origInvulnerableTime = hitEntity.f_19802_;
            hitEntity.f_19802_ = 0;
            double distanceToPlayer = entityHitResult.m_82448_(player);
            double adjustedDamage = (double)FireModeFeature.getDamage(gunStack);
            if (isHeadshot) {
               adjustedDamage *= Config.headshotDamageModifier;
            }

            adjustedDamage *= Mth.m_14008_(1.0D - Math.pow(Math.sqrt(distanceToPlayer) / this.maxShootingDistance * 0.5D, 6.0D), 0.0D, 1.0D);
            if (gunStack != null) {
               adjustedDamage *= (double)DamageFeature.getHitScanDamageModifier(gunStack);
               adjustedDamage *= Config.hitscanDamageModifier;
            }

            hitEntity.m_6469_(damageSource, (float)adjustedDamage);
            EntityExt entityExt = (EntityExt)hitEntity;
            if (System.currentTimeMillis() - entityExt.getLastHitSoundTimestamp() > this.entityHitSoundCooldown) {
               entityExt.setLastHitSoundTimestamp(System.currentTimeMillis());
               if (isHeadshot && !MiscUtil.getLevel(hitEntity).f_46443_) {
                  MiscUtil.getLevel(hitEntity).m_6263_((Player)null, hitEntity.m_20185_(), hitEntity.m_20186_(), hitEntity.m_20189_(), (SoundEvent)this.headshotSound.get(), SoundSource.PLAYERS, this.headshotSoundVolume, 1.0F);
               } else if (adjustedDamage < 15.0D) {
                  MiscUtil.getLevel(hitEntity).m_6263_((Player)null, hitEntity.m_20185_(), hitEntity.m_20186_(), hitEntity.m_20189_(), (SoundEvent)this.lightDamageSound.get(), SoundSource.PLAYERS, this.lightDamageSoundVolume, 1.0F);
               } else {
                  MiscUtil.getLevel(hitEntity).m_6263_((Player)null, hitEntity.m_20185_(), hitEntity.m_20186_(), hitEntity.m_20189_(), (SoundEvent)this.heavyDamageSound.get(), SoundSource.PLAYERS, this.heavyDamageSoundVolume, 1.0F);
               }
            }

            hitEntity.f_19802_ = origInvulnerableTime;
            damage = (float)adjustedDamage;
         }
      }

      if (projectile != null) {
         projectile.m_146870_();
      }

      return damage;
   }

   public void handleBlockHit(LivingEntity player, BlockHitResult blockHitResult, @Nullable Entity projectile) {
      BlockHitEvent event = new BlockHitEvent(player, blockHitResult, projectile);
      MinecraftForge.EVENT_BUS.post(event);
      if (!event.isCanceled()) {
         spawnBlockBreakParticles((ServerPlayer)player, blockHitResult.m_82425_(), blockHitResult.m_82450_());
         if (this.getExplosion() != null) {
            Vec3 hitLocation = blockHitResult.m_82450_();
            this.explode(MiscUtil.getLevel(player), projectile, (DamageSource)null, hitLocation.f_82479_, hitLocation.f_82480_, hitLocation.f_82481_);
         }

         if (projectile != null) {
            projectile.m_146870_();
         }
      }

   }

   public void discardProjectile(Entity projectile) {
      this.explodeProjectile(projectile);
   }

   public void explodeProjectile(Entity projectile) {
      this.explode(MiscUtil.getLevel(projectile), projectile, (DamageSource)null, projectile.m_20185_(), projectile.m_20186_(), projectile.m_20189_());
   }

   private static void spawnBlockBreakParticles(ServerPlayer player, BlockPos blockPos, Vec3 hitLocation) {
      Level level = MiscUtil.getLevel(player);
      BlockState blockState = level.m_8055_(blockPos);
      if (blockState != null) {
         Network.networkChannel.send(PacketDistributor.PLAYER.with(() -> {
            return player;
         }), new SpawnParticlePacket(ParticleTypes.f_123762_, hitLocation.f_82479_, hitLocation.f_82480_, hitLocation.f_82481_, 5));
      }

   }

   private void explode(Level level, @Nullable Entity entity, @Nullable DamageSource damageSource, double posX, double posY, double posZ) {
      if (level.f_46443_) {
         throw new IllegalArgumentException("Cannot use this method on the client side");
      } else {
         float power = this.explosionDescriptor.power();
         boolean fire = this.explosionDescriptor.fire();
         ExplosionDamageCalculator calc = null;
         CustomExplosion customExplosion = CustomExplosion.explode(level, this, entity, damageSource, (ExplosionDamageCalculator)calc, posX, posY, posZ, power, fire, this.explosionDescriptor.interaction(), false);
         if (!customExplosion.m_254884_()) {
            customExplosion.m_46080_();
         }

         Iterator var14 = ((ServerLevel)level).m_8795_((p) -> {
            return true;
         }).iterator();

         while(var14.hasNext()) {
            ServerPlayer player = (ServerPlayer)var14.next();
            if (player.m_20275_(posX, posY, posZ) < 22000.0D) {
               Network.networkChannel.send(PacketDistributor.PLAYER.with(() -> {
                  return player;
               }), new CustomClientBoundExplosionPacket(this, posX, posY, posZ, power, customExplosion.m_46081_(), (Vec3)customExplosion.m_46078_().get(player)));
            }
         }

      }
   }

   public abstract static class Builder<T extends ItemBuilder<T>> extends ItemBuilder<T> implements Configurable, Enableable {
      private ExplosionDescriptor explosionDescriptor;
      private double maxShootingDistance = 200.0D;
      private float damage = 5.0F;
      private Supplier<Boolean> configOptionEnabled;
      private Supplier<Double> configOptionDamage;
      private float headshotSoundVolume = 3.0F;
      private Supplier<SoundEvent> headshotSound;
      protected float lightDamageSoundVolume;
      protected Supplier<SoundEvent> lightDamageSound;
      protected float heavyDamageSoundVolume;
      protected Supplier<SoundEvent> heavyDamageSound;

      public Builder() {
         this.headshotSound = SoundRegistry.HIT_HEADSHOT;
         this.lightDamageSoundVolume = 3.0F;
         this.lightDamageSound = SoundRegistry.HIT_LIGHT;
         this.heavyDamageSoundVolume = 3.0F;
         this.heavyDamageSound = SoundRegistry.HIT_HEAVY;
      }

      protected T cast(Builder<T> _this) {
         return _this;
      }

      public T withDamage(float damage) {
         this.damage = damage;
         return this.cast(this);
      }

      public T withHeadshotSound(Supplier<SoundEvent> headshotSound, float headshotSoundVolume) {
         this.headshotSound = headshotSound;
         this.headshotSoundVolume = headshotSoundVolume;
         return this.cast(this);
      }

      public T withLightDamageSound(Supplier<SoundEvent> lightDamageSound, float lightDamageSoundVolume) {
         this.lightDamageSound = lightDamageSound;
         this.lightDamageSoundVolume = lightDamageSoundVolume;
         return this.cast(this);
      }

      public T withHeavyDamageSound(Supplier<SoundEvent> heavyDamageSound, float heavyDamageSoundVolume) {
         this.heavyDamageSound = heavyDamageSound;
         this.heavyDamageSoundVolume = heavyDamageSoundVolume;
         return this.cast(this);
      }

      public T withMaxShootingDistance(double distance) {
         this.maxShootingDistance = distance;
         return this.cast(this);
      }

      public T withExplosion(float power, boolean fire, ExplosionInteraction explosionInteraction, List<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>> effects) {
         return this.withExplosion(power, fire, explosionInteraction, (String)null, 4.0F, effects);
      }

      public T withExplosion(float power, boolean fire, ExplosionInteraction explosionInteraction, @Nullable String soundName, float soundVolume, List<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>> effects) {
         this.explosionDescriptor = new ExplosionDescriptor(power, fire, explosionInteraction, soundName, soundVolume, effects);
         return this.cast(this);
      }

      public void configure(ConfigManager.Builder builder) {
         this.configOptionEnabled = builder.createBooleanOption().withName(this.getName() + ".enabled").withDescription("Set to `false` to remove the item from the game.").withDefault(true).getSupplier();
         this.configOptionDamage = builder.createDoubleOption().withName(this.getName() + ".damage").withDescription("Sets this item damage.").withRange(0.01D, 100.0D).withDefault((double)this.damage).getSupplier();
      }

      public T withJsonObject(JsonObject obj) {
         this.withMaxShootingDistance(JsonUtil.getJsonDouble(obj, "maxShootingDistance", 200.0D));
         this.withDamage(JsonUtil.getJsonFloat(obj, "damage", 5.0F));
         float headshotSoundVolume = JsonUtil.getJsonFloat(obj, "headshotSoundVolume", 3.0F);
         JsonElement headshotSoundElem = obj.get("headshotSound");
         if (headshotSoundElem != null && !headshotSoundElem.isJsonNull()) {
            String headshotSoundName = headshotSoundElem.getAsString();
            this.withHeadshotSound(() -> {
               return SoundRegistry.getSoundEvent(headshotSoundName);
            }, headshotSoundVolume);
         }

         JsonObject jsExplosion = obj.getAsJsonObject("explosion");
         if (jsExplosion != null) {
            float power = JsonUtil.getJsonFloat(jsExplosion, "power", 1.0F);
            boolean fire = JsonUtil.getJsonBoolean(jsExplosion, "fire", false);
            String soundName = JsonUtil.getJsonString(jsExplosion, "sound", (String)null);
            float soundVolume = JsonUtil.getJsonFloat(jsExplosion, "soundVolume", 4.0F);
            ExplosionInteraction interaction = (ExplosionInteraction)JsonUtil.getEnum(jsExplosion, "interaction", ExplosionInteraction.class, ExplosionInteraction.BLOCK, true);
            List<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>> explosionEffects = new ArrayList();
            Iterator var11 = JsonUtil.getStrings(jsExplosion, "effects").iterator();

            while(var11.hasNext()) {
               String effectName = (String)var11.next();
               Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> supplier = () -> {
                  return (EffectBuilder)EffectRegistry.getEffectBuilderSupplier(effectName).get();
               };
               explosionEffects.add(supplier);
            }

            this.withExplosion(power, fire, interaction, soundName, soundVolume, explosionEffects);
         }

         return this.cast(this);
      }

      public float getDamage() {
         return this.damage;
      }

      public boolean isEnabled() {
         return this.configOptionEnabled == null || (Boolean)this.configOptionEnabled.get();
      }
   }
}
