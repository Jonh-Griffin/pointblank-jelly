package com.vicmatskiv.pointblank.item;

import com.google.gson.JsonObject;
import com.vicmatskiv.pointblank.Nameable;
import com.vicmatskiv.pointblank.client.EntityRendererBuilder;
import com.vicmatskiv.pointblank.client.effect.AbstractEffect;
import com.vicmatskiv.pointblank.client.effect.Effect;
import com.vicmatskiv.pointblank.client.effect.EffectBuilder;
import com.vicmatskiv.pointblank.client.render.ProjectileItemEntityRenderer;
import com.vicmatskiv.pointblank.client.render.ProjectileItemRenderer;
import com.vicmatskiv.pointblank.client.render.SpriteEntityRenderer;
import com.vicmatskiv.pointblank.crafting.Craftable;
import com.vicmatskiv.pointblank.entity.EntityBuilder;
import com.vicmatskiv.pointblank.entity.EntityBuilderProvider;
import com.vicmatskiv.pointblank.entity.ProjectileLike;
import com.vicmatskiv.pointblank.entity.SlowProjectile;
import com.vicmatskiv.pointblank.registry.EffectRegistry;
import com.vicmatskiv.pointblank.util.JsonUtil;
import com.vicmatskiv.pointblank.util.MiscUtil;
import com.vicmatskiv.pointblank.util.SoundInfo;
import com.vicmatskiv.pointblank.util.TimeUnit;
import com.vicmatskiv.pointblank.util.TopDownAttackTrajectory;
import com.vicmatskiv.pointblank.util.Tradeable;
import com.vicmatskiv.pointblank.util.Trajectory;
import com.vicmatskiv.pointblank.util.TrajectoryPhaseListener;
import com.vicmatskiv.pointblank.util.TrajectoryProvider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.fml.loading.FMLLoader;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.util.GeckoLibUtil;

public class AmmoItem extends HurtingItem implements ExplosionProvider, TrajectoryProvider, Craftable, Nameable, GeoItem, Tradeable {
   private static final RawAnimation ANIMATION_DRAW = RawAnimation.begin().thenPlay("animation.model.draw");
   private static final RawAnimation ANIMATION_THROW = RawAnimation.begin().thenPlay("animation.model.throw");
   private final AnimatableInstanceCache cache;
   private boolean hasProjectile;
   private String name;
   private float tradePrice;
   private int tradeBundleQuantity;
   private int tradeLevel;
   private List<EffectBuilderInfo> projectileEffectBuilderSuppliers;
   private EntityBuilder<?, ?> entityBuilder;
   private boolean isTopDownAttackEnabled;
   private double initialVelocity;
   private Map<TopDownAttackTrajectory.Phase, SoundInfo> topDownProjectileSoundEvents;
   private long craftingDuration;

   public AmmoItem(String name) {
      this(name, (Builder)null);
   }

   public AmmoItem(String name, Builder builder) {
      super(new Properties(), builder);
      this.cache = GeckoLibUtil.createInstanceCache(this);
      this.name = name;
      if (name.equals("grenade")) {
         SingletonGeoAnimatable.registerSyncedAnimatable(this);
      }

      if (builder != null) {
         this.tradePrice = builder.tradePrice;
         this.tradeBundleQuantity = builder.tradeBundleQuantity;
         this.tradeLevel = builder.tradeLevel;
         this.setHasProjectile(builder.hasProjectile);
         this.entityBuilder = builder.getOrCreateEntityBuilder();
         this.isTopDownAttackEnabled = builder.isTopDownAttackEnabled;
         this.initialVelocity = builder.initialVelocity;
         this.topDownProjectileSoundEvents = builder.topDownProjectileSoundEvents;
         this.craftingDuration = builder.craftingDuration;
      }

   }

   public String getName() {
      return this.name;
   }

   public ProjectileLike createProjectile(LivingEntity player, double posX, double posY, double posZ) {
      ProjectileLike projectile = (ProjectileLike)this.entityBuilder.build(MiscUtil.getLevel(player));
      ((Entity)projectile).m_6034_(posX, posY, posZ);
      ((Projectile)projectile).m_5602_(player);
      return projectile;
   }

   public void registerControllers(ControllerRegistrar registry) {
   }

   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }

   public void initializeClient(Consumer<IClientItemExtensions> consumer) {
      consumer.accept(new IClientItemExtensions() {
         private BlockEntityWithoutLevelRenderer renderer;

         public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            if (this.renderer == null) {
               this.renderer = new ProjectileItemRenderer(AmmoItem.this.name);
            }

            return this.renderer;
         }
      });
   }

   public boolean isHasProjectile() {
      return this.hasProjectile;
   }

   public void setHasProjectile(boolean hasProjectile) {
      this.hasProjectile = hasProjectile;
   }

   public float getPrice() {
      return this.tradePrice;
   }

   public int getTradeLevel() {
      return this.tradeLevel;
   }

   public int getBundleQuantity() {
      return this.tradeBundleQuantity;
   }

   public long getCraftingDuration() {
      return this.craftingDuration;
   }

   public Trajectory<?> createTrajectory(final Level level, Vec3 startPosition, Vec3 targetLocation) {
      Trajectory<?> trajectory = null;
      if (this.isTopDownAttackEnabled) {
         TopDownAttackTrajectory topDownAttackTrajectory = TopDownAttackTrajectory.createTrajectory(startPosition, targetLocation, this.initialVelocity);
         if (topDownAttackTrajectory != null) {
            topDownAttackTrajectory.addListener(new TrajectoryPhaseListener<TopDownAttackTrajectory.Phase>() {
               public void onStartPhase(TopDownAttackTrajectory.Phase phase, Vec3 position) {
                  if (AmmoItem.this.topDownProjectileSoundEvents != null) {
                     SoundInfo soundInfo = (SoundInfo)AmmoItem.this.topDownProjectileSoundEvents.get(phase);
                     if (soundInfo != null) {
                        level.m_7785_(position.f_82479_, position.f_82480_, position.f_82481_, (SoundEvent)soundInfo.soundEvent().get(), SoundSource.BLOCKS, soundInfo.volume(), 1.0F, false);
                     }
                  }

               }
            });
         }

         trajectory = topDownAttackTrajectory;
      }

      return trajectory;
   }

   public static class Builder extends HurtingItem.Builder<Builder> implements Nameable {
      private static final double DEFAULT_GRAVITY = 0.05D;
      private static final float DEFAULT_INITIAL_VELOCITY = 50.0F;
      private static final float DEFAULT_WIDTH = 0.25F;
      private static final float DEFAULT_HEIGHT = 0.25F;
      private static final float DEFAULT_PRICE = Float.NaN;
      private static final int DEFAULT_TRADE_LEVEL = 0;
      private static final int DEFAULT_TRADE_BUNDLE_QUANTITY = 1;
      private static final int DEFAULT_CRAFTING_DURATION = 500;
      private static Effect.BlendMode DEFAULT_BLEND_MODE;
      private String name;
      private float tradePrice = Float.NaN;
      private int tradeBundleQuantity = 1;
      private int tradeLevel = 0;
      private Supplier<EntityBuilder<?, ?>> entityBuilderSupplier;
      private boolean hasProjectile;
      private boolean isTopDownAttackEnabled;
      private List<EffectBuilderInfo> projectileEffectBuilderSuppliers = new ArrayList();
      private Supplier<EntityRendererBuilder<?, Entity, EntityRenderer<Entity>>> rendererBuilder;
      private double gravity = 0.05D;
      private double initialVelocity = 50.0D;
      private float boundingBoxWidth = 0.25F;
      private float boundingBoxHeight = 0.25F;
      private EntityBuilder<?, ?> entityBuilder;
      private Map<TopDownAttackTrajectory.Phase, SoundInfo> topDownProjectileSoundEvents = new HashMap();
      private long craftingDuration = 500L;
      private AmmoItem builtItem;

      public Builder withName(String name) {
         this.name = name;
         return this;
      }

      public Builder withCraftingDuration(int duration, TimeUnit timeUnit) {
         this.craftingDuration = timeUnit.toMillis((long)duration);
         return this;
      }

      public Builder withTradePrice(double price, int tradeBundleQuantity, int tradeLevel) {
         this.tradePrice = (float)price;
         this.tradeLevel = tradeLevel;
         this.tradeBundleQuantity = tradeBundleQuantity;
         return this;
      }

      public Builder withTradePrice(double price, int tradeLevel) {
         return this.withTradePrice(price, 1, tradeLevel);
      }

      public Builder withProjectileInitialVelocity(double initialVelocity) {
         this.hasProjectile = true;
         this.initialVelocity = initialVelocity;
         return this;
      }

      public Builder withProjectileGravity(double gravity) {
         this.hasProjectile = true;
         this.gravity = gravity;
         return this;
      }

      public Builder withProjectileRenderer(Supplier<EntityRendererBuilder<?, Entity, EntityRenderer<Entity>>> rendererBuilder) {
         this.hasProjectile = true;
         this.rendererBuilder = rendererBuilder;
         return this;
      }

      public Builder withProjectileEffect(Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> effectSupplier) {
         this.hasProjectile = true;
         this.projectileEffectBuilderSuppliers.add(new EffectBuilderInfo(effectSupplier, (p) -> {
            return true;
         }));
         return this;
      }

      public Builder withProjectileEffect(Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> effectSupplier, Predicate<ProjectileLike> predicate) {
         this.hasProjectile = true;
         this.projectileEffectBuilderSuppliers.add(new EffectBuilderInfo(effectSupplier, predicate));
         return this;
      }

      public Builder withProjectileTopDownAttackEnabled(boolean topDownAttackEnabled) {
         this.isTopDownAttackEnabled = topDownAttackEnabled;
         return this;
      }

      public Builder withProjectileTopDownAttackPhaseSound(TopDownAttackTrajectory.Phase phase, Supplier<SoundEvent> sound, float volume) {
         this.topDownProjectileSoundEvents.put(phase, new SoundInfo(sound, volume));
         return this;
      }

      public Builder withProjectileBoundingBoxSize(float width, float height) {
         this.hasProjectile = true;
         this.boundingBoxWidth = width;
         this.boundingBoxHeight = height;
         return this;
      }

      public Builder withEntityBuilderProvider(Supplier<EntityBuilder<?, ?>> entityBuilderSupplier) {
         this.entityBuilderSupplier = entityBuilderSupplier;
         return this;
      }

      public Builder withJsonObject(JsonObject obj) {
         super.withJsonObject(obj);
         this.withName(JsonUtil.getJsonString(obj, "name"));
         this.withTradePrice((double)JsonUtil.getJsonFloat(obj, "tradePrice", Float.NaN), JsonUtil.getJsonInt(obj, "traceBundleQuantity", 1), JsonUtil.getJsonInt(obj, "tradeLevel", 0));
         this.withCraftingDuration(JsonUtil.getJsonInt(obj, "craftingDuration", 500), TimeUnit.MILLISECOND);
         JsonObject projectileObj = obj.getAsJsonObject("projectile");
         if (projectileObj != null) {
            float size = JsonUtil.getJsonFloat(projectileObj, "boundingBoxSize", Float.NEGATIVE_INFINITY);
            if (size > 0.0F) {
               this.withProjectileBoundingBoxSize(size, size);
            } else {
               float width = JsonUtil.getJsonFloat(projectileObj, "width", 0.25F);
               float height = JsonUtil.getJsonFloat(projectileObj, "height", 0.25F);
               this.withProjectileBoundingBoxSize(width, height);
               size = Math.max(width, height);
            }

            this.withProjectileGravity(JsonUtil.getJsonDouble(projectileObj, "gravity", 0.05D));
            this.withProjectileInitialVelocity(JsonUtil.getJsonDouble(projectileObj, "initialVelocity", 0.05D));
            this.withProjectileTopDownAttackEnabled(JsonUtil.getJsonBoolean(projectileObj, "topDownAttackEnabled", false));
            JsonObject rendererObj = projectileObj.getAsJsonObject("renderer");
            Dist side = FMLLoader.getDist();
            if (rendererObj != null && side.isClient()) {
               String rendererType = JsonUtil.getJsonString(rendererObj, "type");
               if (rendererType.toLowerCase().equals("sprite")) {
                  SpriteEntityRenderer.Builder rendererBuilder = new SpriteEntityRenderer.Builder();
                  rendererBuilder.withTexture(JsonUtil.getJsonString(rendererObj, "texture"));
                  rendererBuilder.withSize(JsonUtil.getJsonFloat(rendererObj, "size", size));
                  JsonObject spritesObj = rendererObj.getAsJsonObject("sprites");
                  if (spritesObj == null) {
                     throw new IllegalArgumentException("Element 'sprites' not defined in json: " + rendererObj);
                  }

                  int rows = JsonUtil.getJsonInt(spritesObj, "rows", 1);
                  int columns = JsonUtil.getJsonInt(spritesObj, "columns", 1);
                  int fps = JsonUtil.getJsonInt(spritesObj, "fps", 60);
                  AbstractEffect.SpriteAnimationType spriteAnimationType = (AbstractEffect.SpriteAnimationType)JsonUtil.getEnum(spritesObj, "type", AbstractEffect.SpriteAnimationType.class, AbstractEffect.SpriteAnimationType.LOOP, true);
                  rendererBuilder.withSprites(rows, columns, fps, spriteAnimationType);
                  rendererBuilder.withBlendMode((Effect.BlendMode)JsonUtil.getEnum(rendererObj, "blendMode", Effect.BlendMode.class, DEFAULT_BLEND_MODE, true));
                  rendererBuilder.withDepthTest(JsonUtil.getJsonBoolean(rendererObj, "depthTest", true));
                  rendererBuilder.withGlow(JsonUtil.getJsonBoolean(rendererObj, "glow", false));
                  rendererBuilder.withRotations((double)JsonUtil.getJsonFloat(rendererObj, "rotations", 0.0F));
                  this.withProjectileRenderer(() -> {
                     return rendererBuilder;
                  });
               } else if (rendererType.toLowerCase().equals("model")) {
                  this.withProjectileRenderer(() -> {
                     return new ProjectileItemEntityRenderer.Builder();
                  });
               }
            }

            Iterator var15 = JsonUtil.getStrings(projectileObj, "effects").iterator();

            while(var15.hasNext()) {
               String effectName = (String)var15.next();
               Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> supplier = () -> {
                  return (EffectBuilder)EffectRegistry.getEffectBuilderSupplier(effectName).get();
               };
               this.withProjectileEffect(supplier);
            }
         }

         return this;
      }

      public String getName() {
         return this.name;
      }

      public AmmoItem build() {
         if (this.builtItem == null) {
            this.builtItem = new AmmoItem(this.name, this);
         }

         return this.builtItem;
      }

      public EntityBuilderProvider getEntityBuilderProvider() {
         return this.hasProjectile ? () -> {
            return this.getOrCreateEntityBuilder();
         } : null;
      }

      private EntityBuilder<?, ?> getOrCreateEntityBuilder() {
         if (this.entityBuilder == null) {
            if (this.entityBuilderSupplier != null) {
               this.entityBuilder = (EntityBuilder)this.entityBuilderSupplier.get();
            } else {
               this.entityBuilder = SlowProjectile.builder();
            }

            this.entityBuilder.withItem(this::build);
            if (this.rendererBuilder != null) {
               this.entityBuilder.withRenderer(this.rendererBuilder);
            }

            this.entityBuilder.withName(this.name);
            this.entityBuilder.withInitialVelocity(this.initialVelocity);
            this.entityBuilder.withGravity(this.gravity);
            Iterator var1 = this.projectileEffectBuilderSuppliers.iterator();

            while(var1.hasNext()) {
               EffectBuilderInfo ebi = (EffectBuilderInfo)var1.next();
               this.entityBuilder.withEffect(ebi);
            }
         }

         return this.entityBuilder;
      }

      static {
         DEFAULT_BLEND_MODE = Effect.BlendMode.NORMAL;
      }
   }
}
