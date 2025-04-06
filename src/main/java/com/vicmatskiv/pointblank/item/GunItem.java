package com.vicmatskiv.pointblank.item;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.vicmatskiv.pointblank.Config;
import com.vicmatskiv.pointblank.Nameable;
import com.vicmatskiv.pointblank.attachment.Attachment;
import com.vicmatskiv.pointblank.attachment.AttachmentCategory;
import com.vicmatskiv.pointblank.attachment.AttachmentHost;
import com.vicmatskiv.pointblank.attachment.Attachments;
import com.vicmatskiv.pointblank.client.BiDirectionalInterpolator;
import com.vicmatskiv.pointblank.client.DynamicGeoListener;
import com.vicmatskiv.pointblank.client.GunClientState;
import com.vicmatskiv.pointblank.client.GunStatePoseProvider;
import com.vicmatskiv.pointblank.client.LockableTarget;
import com.vicmatskiv.pointblank.client.controller.AbstractProceduralAnimationController;
import com.vicmatskiv.pointblank.client.controller.BlendingAnimationController;
import com.vicmatskiv.pointblank.client.controller.GlowAnimationController;
import com.vicmatskiv.pointblank.client.controller.GunRandomizingAnimationController;
import com.vicmatskiv.pointblank.client.controller.GunRecoilAnimationController;
import com.vicmatskiv.pointblank.client.controller.GunStateAnimationController;
import com.vicmatskiv.pointblank.client.controller.PlayerRecoilController;
import com.vicmatskiv.pointblank.client.controller.RotationAnimationController;
import com.vicmatskiv.pointblank.client.controller.TimerController;
import com.vicmatskiv.pointblank.client.controller.ViewShakeAnimationController;
import com.vicmatskiv.pointblank.client.controller.ViewShakeAnimationController2;
import com.vicmatskiv.pointblank.client.effect.AbstractEffect;
import com.vicmatskiv.pointblank.client.effect.EffectBuilder;
import com.vicmatskiv.pointblank.client.effect.EffectLauncher;
import com.vicmatskiv.pointblank.client.effect.MuzzleFlashEffect;
import com.vicmatskiv.pointblank.client.render.GunItemRenderer;
import com.vicmatskiv.pointblank.crafting.Craftable;
import com.vicmatskiv.pointblank.entity.ProjectileLike;
import com.vicmatskiv.pointblank.feature.AccuracyFeature;
import com.vicmatskiv.pointblank.feature.ActiveMuzzleFeature;
import com.vicmatskiv.pointblank.feature.AimingFeature;
import com.vicmatskiv.pointblank.feature.AmmoCapacityFeature;
import com.vicmatskiv.pointblank.feature.ConditionContext;
import com.vicmatskiv.pointblank.feature.Feature;
import com.vicmatskiv.pointblank.feature.FeatureBuilder;
import com.vicmatskiv.pointblank.feature.Features;
import com.vicmatskiv.pointblank.feature.FireModeFeature;
import com.vicmatskiv.pointblank.feature.GlowFeature;
import com.vicmatskiv.pointblank.feature.MuzzleFlashFeature;
import com.vicmatskiv.pointblank.feature.PipFeature;
import com.vicmatskiv.pointblank.feature.ReloadFeature;
import com.vicmatskiv.pointblank.feature.ReticleFeature;
import com.vicmatskiv.pointblank.feature.SoundFeature;
import com.vicmatskiv.pointblank.network.FireModeRequestPacket;
import com.vicmatskiv.pointblank.network.FireModeResponsePacket;
import com.vicmatskiv.pointblank.network.HitScanFireRequestPacket;
import com.vicmatskiv.pointblank.network.HitScanFireResponsePacket;
import com.vicmatskiv.pointblank.network.Network;
import com.vicmatskiv.pointblank.network.ProjectileFireRequestPacket;
import com.vicmatskiv.pointblank.network.ReloadRequestPacket;
import com.vicmatskiv.pointblank.network.ReloadResponsePacket;
import com.vicmatskiv.pointblank.registry.AmmoRegistry;
import com.vicmatskiv.pointblank.registry.EffectRegistry;
import com.vicmatskiv.pointblank.registry.ItemRegistry;
import com.vicmatskiv.pointblank.registry.SoundRegistry;
import com.vicmatskiv.pointblank.util.ClientUtil;
import com.vicmatskiv.pointblank.util.Conditions;
import com.vicmatskiv.pointblank.util.HitScan;
import com.vicmatskiv.pointblank.util.JsonUtil;
import com.vicmatskiv.pointblank.util.MiscUtil;
import com.vicmatskiv.pointblank.util.SimpleHitResult;
import com.vicmatskiv.pointblank.util.TimeUnit;
import com.vicmatskiv.pointblank.util.Tradeable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractGlassBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.TriPredicate;
import net.minecraftforge.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.core.keyframe.event.data.SoundKeyframeData;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.ClientUtils;
import software.bernie.geckolib.util.GeckoLibUtil;

public class GunItem extends HurtingItem implements Craftable, AttachmentHost, Nameable, GeoItem, LockableTarget.TargetLocker, Tradeable {
   private static final Logger LOGGER = LogManager.getLogger("pointblank");
   private static final String DEFAULT_ANIMATION_IDLE = "animation.model.idle";
   private static final String DEFAULT_ANIMATION_RELOAD = "animation.model.reload";
   private static final String DEFAULT_ANIMATION_INSPECT = "animation.model.inspect";
   private static final String DEFAULT_ANIMATION_DRAW = "animation.model.draw";
   private static final String DEFAULT_ANIMATION_ENABLE_FIRE_MODE = "animation.model.enablefiremode";
   private static final String DEFAULT_ANIMATION_COMPLETE_FIRE = "animation.model.completefire";
   private static final String DEFAULT_ANIMATION_PREPARE_FIRE = "animation.model.preparefire";
   public static final String DEFAULT_ANIMATION_FIRE = "animation.model.fire";
   public static final String DEFAULT_ANIMATION_OFF_GROUND = "animation.model.off_ground";
   public static final String DEFAULT_ANIMATION_OFF_GROUND_SPRINTING = "animation.model.off_ground_sprinting";
   public static final String DEFAULT_ANIMATION_STANDING = "animation.model.standing";
   public static final String DEFAULT_ANIMATION_WALKING = "animation.model.walking";
   public static final String DEFAULT_ANIMATION_CROUCHING = "animation.model.crouching";
   public static final String DEFAULT_ANIMATION_WALKING_AIMING = "animation.model.walking_aiming";
   public static final String DEFAULT_ANIMATION_WALKING_BACKWARDS = "animation.model.walking_backwards";
   public static final String DEFAULT_ANIMATION_WALKING_LEFT = "animation.model.walking_left";
   public static final String DEFAULT_ANIMATION_WALKING_RIGHT = "animation.model.walking_right";
   public static final String DEFAULT_ANIMATION_RUNNING = "animation.model.running";
   public static final String DEFAULT_ANIMATION_PREPARE_RUNNING = "animation.model.runningstart";
   public static final String DEFAULT_ANIMATION_COMPLETE_RUNNING = "animation.model.runningend";
   public static final int INFINITE_AMMO = Integer.MAX_VALUE;
   public static final String DEFAULT_RETICLE_OVERLAY = "textures/item/reticle.png";
   public static final String DEFAULT_RETICLE_OVERLAY_PARALLAX = "textures/item/reticle4.png";
   public static final RawAnimation RAW_ANIMATION_OFF_GROUND = RawAnimation.begin().thenPlay("animation.model.off_ground");
   public static final RawAnimation RAW_ANIMATION_OFF_GROUND_SPRINTING = RawAnimation.begin().thenPlay("animation.model.off_ground_sprinting");
   public static final RawAnimation RAW_ANIMATION_STANDING = RawAnimation.begin().thenPlay("animation.model.standing");
   public static final RawAnimation RAW_ANIMATION_WALKING = RawAnimation.begin().thenPlay("animation.model.walking");
   public static final RawAnimation RAW_ANIMATION_CROUCHING = RawAnimation.begin().thenPlay("animation.model.crouching");
   public static final RawAnimation RAW_ANIMATION_WALKING_AIMING = RawAnimation.begin().thenPlay("animation.model.walking_aiming");
   public static final RawAnimation RAW_ANIMATION_WALKING_BACKWARDS = RawAnimation.begin().thenPlay("animation.model.walking_backwards");
   public static final RawAnimation RAW_ANIMATION_WALKING_LEFT = RawAnimation.begin().thenPlay("animation.model.walking_left");
   public static final RawAnimation RAW_ANIMATION_WALKING_RIGHT = RawAnimation.begin().thenPlay("animation.model.walking_right");
   public static final RawAnimation RAW_ANIMATION_PREPARE_RUNNING = RawAnimation.begin().thenPlay("animation.model.runningstart");
   public static final RawAnimation RAW_ANIMATION_COMPLETE_RUNNING = RawAnimation.begin().thenPlay("animation.model.runningend");
   public static final RawAnimation RAW_ANIMATION_RUNNING = RawAnimation.begin().thenPlay("animation.model.running");
   private static final List<ResourceLocation> FALLBACK_COMMON_ANIMATIONS = List.of(new ResourceLocation("pointblank", "common"));
   private static final List<ResourceLocation> FALLBACK_PISTOL_ANIMATIONS = List.of(new ResourceLocation("pointblank", "pistol"), new ResourceLocation("pointblank", "common"));
   private static Random random = new Random();
   private static final double MAX_SHOOTING_DISTANCE_WITHOUT_AIMING = 100.0D;
   private static final ResourceLocation DEFAULT_SCOPE_OVERLAY = new ResourceLocation("pointblank", "textures/gui/scope.png");
   private static final int MAX_ATTACHMENT_CATEGORIES = 11;
   private final String name;
   private float tradePrice;
   private int tradeLevel;
   private int tradeBundleQuantity;
   private int maxAmmoCapacity;
   private boolean requiresPhasedReload;
   private boolean isAimingEnabled;
   private final int rpm;
   private final long prepareIdleCooldownDuration;
   private final long prepareFireCooldownDuration;
   private final long completeFireCooldownDuration;
   private final long enableFireModeCooldownDuration;
   private long targetLockTimeTicks;
   private final Set<Supplier<AmmoItem>> compatibleBullets;
   private double viewRecoilAmplitude;
   private double shakeRecoilAmplitude;
   private int viewRecoilMaxPitch;
   private long viewRecoilDuration;
   private double shakeRecoilSpeed;
   private double shakeDecay;
   private long shakeRecoilDuration;
   private double gunRecoilInitialAmplitude;
   private double gunRecoilRateOfAmplitudeDecay;
   private double gunRecoilInitialAngularFrequency;
   private double gunRecoilRateOfFrequencyIncrease;
   private double gunRecoilPitchMultiplier;
   private long gunRecoilDuration;
   private int shotsPerRecoil;
   private int shotsPerTrace;
   private long idleRandomizationDuration;
   private long recoilRandomizationDuration;
   private double gunRandomizationAmplitude;
   private int burstShots;
   private SoundEvent fireSound;
   private float fireSoundVolume;
   private SoundEvent targetLockedSound;
   private SoundEvent targetStartLockingSound;
   private long reloadCooldownTime;
   private float bobbing;
   private float bobbingOnAim;
   private float bobbingRollMultiplier;
   private double jumpMultiplier;
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private double aimingCurveX;
   private double aimingCurveY;
   private double aimingCurveZ;
   private double aimingCurvePitch;
   private double aimingCurveYaw;
   private double aimingCurveRoll;
   private double aimingZoom;
   private double pipScopeZoom;
   private ResourceLocation scopeOverlay;
   private ResourceLocation targetLockOverlay;
   private ResourceLocation modelResourceLocation;
   private List<PhasedReload> phasedReloads;
   private AnimationProvider drawAnimationProvider;
   private AnimationProvider inspectAnimationProvider;
   private AnimationProvider idleAnimationProvider;
   private String reloadAnimation;
   private int pelletCount;
   private double pelletSpread;
   private double inaccuracy;
   private double inaccuracyAiming;
   private double inaccuracySprinting;
   private List<Tuple<Long, AbstractProceduralAnimationController>> reloadEffectControllers;
   private List<GlowAnimationController.Builder> glowEffectBuilders;
   private List<RotationAnimationController.Builder> rotationEffectBuilders;
   private EffectLauncher effectLauncher;
   private float hitScanSpeed;
   private float hitScanAcceleration;
   private float modelScale;
   private List<Supplier<Attachment>> compatibleAttachmentSuppliers;
   private List<String> compatibleAttachmentGroups;
   private Set<Attachment> compatibleAttachments;
   private List<Supplier<Attachment>> defaultAttachmentSuppliers;
   private long craftingDuration;
   private Map<Class<? extends Feature>, Feature> features;
   private AnimationType animationType;
   private ResourceLocation firstPersonFallbackAnimations;
   private String thirdPersonFallbackAnimations;

   private GunItem(Builder builder, String namespace) {
      super(new Properties(), builder);
      this.name = builder.name;
      if (this.name.contains(":")) {
         this.modelResourceLocation = new ResourceLocation(this.name);
      } else {
         this.modelResourceLocation = new ResourceLocation(namespace, this.name);
      }

      this.modelScale = builder.modelScale;
      this.tradePrice = builder.tradePrice;
      this.tradeLevel = builder.tradeLevel;
      this.tradeBundleQuantity = builder.tradeBundleQuantity;
      this.maxAmmoCapacity = builder.maxAmmoCapacity;
      this.rpm = builder.rpm;
      this.isAimingEnabled = builder.isAimingEnabled;
      this.compatibleBullets = builder.compatibleAmmo != null && builder.compatibleAmmo.size() > 0 ? builder.compatibleAmmo : Collections.emptySet();
      this.targetLockTimeTicks = builder.targetLockTimeTicks;
      this.hitScanSpeed = builder.hitScanSpeed;
      this.hitScanAcceleration = builder.hitScanAcceleration;
      this.viewRecoilAmplitude = builder.viewRecoilAmplitude;
      this.shakeRecoilAmplitude = builder.shakeRecoilAmplitude;
      this.viewRecoilMaxPitch = builder.viewRecoilMaxPitch;
      this.viewRecoilDuration = builder.viewRecoilDuration;
      this.shakeRecoilSpeed = builder.shakeRecoilSpeed;
      this.shakeRecoilDuration = builder.shakeRecoilDuration;
      this.shakeDecay = builder.shakeDecay;
      this.gunRecoilInitialAmplitude = builder.gunRecoilInitialAmplitude;
      this.gunRecoilRateOfAmplitudeDecay = builder.gunRecoilRateOfAmplitudeDecay;
      this.gunRecoilInitialAngularFrequency = builder.gunRecoilInitialAngularFrequency;
      this.gunRecoilRateOfFrequencyIncrease = builder.gunRecoilRateOfFrequencyIncrease;
      this.gunRecoilPitchMultiplier = builder.gunRecoilPitchMultiplier;
      this.gunRecoilDuration = builder.gunRecoilDuration;
      this.shotsPerRecoil = builder.shotsPerRecoil;
      this.shotsPerTrace = builder.shotsPerTrace;
      this.gunRandomizationAmplitude = builder.gunRandomizationAmplitude;
      this.idleRandomizationDuration = builder.idleRandomizationDuration;
      this.recoilRandomizationDuration = builder.recoilRandomizationDuration;
      this.jumpMultiplier = builder.jumpMultiplier;
      this.burstShots = builder.burstShots;
      this.fireSound = builder.fireSound != null ? (SoundEvent)builder.fireSound.get() : null;
      this.fireSoundVolume = builder.fireSoundVolume;
      this.prepareIdleCooldownDuration = builder.prepareIdleCooldownDuration;
      this.prepareFireCooldownDuration = builder.prepareFireCooldownDuration;
      this.completeFireCooldownDuration = builder.completeFireCooldownDuration;
      this.enableFireModeCooldownDuration = builder.enableFireModeCooldownDuration;
      this.craftingDuration = builder.craftingDuration;
      this.aimingCurveX = builder.aimingCurveX;
      this.aimingCurveY = builder.aimingCurveY;
      this.aimingCurveZ = builder.aimingCurveZ;
      this.aimingCurvePitch = builder.aimingCurvePitch;
      this.aimingCurveYaw = builder.aimingCurveYaw;
      this.aimingCurveRoll = builder.aimingCurveRoll;
      this.pipScopeZoom = builder.pipScopeZoom;
      this.aimingZoom = builder.aimingZoom;
      this.scopeOverlay = builder.scopeOverlay != null ? new ResourceLocation("pointblank", builder.scopeOverlay) : null;
      this.targetLockOverlay = builder.targetLockOverlay != null ? new ResourceLocation("pointblank", builder.targetLockOverlay) : null;
      this.targetLockedSound = builder.targetLockedSound != null ? (SoundEvent)builder.targetLockedSound.get() : null;
      this.targetStartLockingSound = builder.targetStartLockingSound != null ? (SoundEvent)builder.targetStartLockingSound.get() : null;
      this.bobbing = builder.bobbing;
      this.bobbingOnAim = builder.bobbingOnAim;
      this.bobbingRollMultiplier = builder.bobbingRollMultiplier;
      this.reloadEffectControllers = Collections.unmodifiableList(builder.reloadEffectControllers);
      this.phasedReloads = builder.phasedReloads;
      if (!builder.drawAnimationsBuilder.getAnimations().isEmpty()) {
         this.drawAnimationProvider = builder.drawAnimationsBuilder.build();
      }

      if (!builder.inspectAnimationsBuilder.getAnimations().isEmpty()) {
         this.inspectAnimationProvider = builder.inspectAnimationsBuilder.build();
      }

      if (!builder.idleAnimationBuilder.getAnimations().isEmpty()) {
         this.idleAnimationProvider = builder.idleAnimationBuilder.build();
      }

      this.animationType = builder.animationType;
      if (builder.firstPersonFallbackAnimations != null) {
         this.firstPersonFallbackAnimations = new ResourceLocation("pointblank", builder.firstPersonFallbackAnimations);
      }

      this.thirdPersonFallbackAnimations = builder.thirdPersonFallbackAnimations;
      this.pelletSpread = builder.pelletSpread;
      if (builder.pelletCount > 1) {
         this.maxShootingDistance = Math.min(this.maxShootingDistance, 50.0D);
      }

      this.inaccuracyAiming = builder.inaccuracyAiming;
      this.inaccuracy = builder.inaccuracy;
      this.inaccuracySprinting = builder.inaccuracySprinting;
      this.reloadCooldownTime = builder.reloadCooldownTime;
      this.reloadAnimation = builder.reloadAnimation;
      if (this.phasedReloads.isEmpty() && this.reloadAnimation != null) {
         this.phasedReloads.add(new PhasedReload(ReloadPhase.RELOADING, this.reloadCooldownTime, this.reloadAnimation));
      } else {
         this.requiresPhasedReload = true;
      }

      this.compatibleAttachmentSuppliers = Collections.unmodifiableList(builder.compatibleAttachments);
      this.compatibleAttachmentGroups = Collections.unmodifiableList(builder.compatibleAttachmentGroups);
      this.defaultAttachmentSuppliers = Collections.unmodifiableList(builder.defaultAttachments);
      Map<Class<? extends Feature>, Feature> features = new HashMap();
      Iterator var4 = builder.featureBuilders.iterator();

      while(var4.hasNext()) {
         FeatureBuilder<?, ?> featureBuilder = (FeatureBuilder)var4.next();
         Feature feature = featureBuilder.build(this);
         features.put(feature.getClass(), feature);
      }

      ActiveMuzzleFeature activeMuzzleFeature = (ActiveMuzzleFeature)features.get(ActiveMuzzleFeature.class);
      if (activeMuzzleFeature == null) {
         FeatureBuilder<?, ?> activeMuzzleFeatureBuilder = (new ActiveMuzzleFeature.Builder()).withCondition(Conditions.isUsingDefaultMuzzle().and(Conditions.doesNotHaveAttachmentInCategory(AttachmentCategory.MUZZLE)));
         features.put(ActiveMuzzleFeature.class, activeMuzzleFeatureBuilder.build(this));
      }

      FireModeFeature fireModeFeature = (FireModeFeature)features.get(FireModeFeature.class);
      if (fireModeFeature == null) {
         FireModeFeature.Builder fireModeFeatureBuilder = new FireModeFeature.Builder();
         if (builder.fireModes != null) {
            FireMode[] var7 = builder.fireModes;
            int var8 = var7.length;

            for(int var9 = 0; var9 < var8; ++var9) {
               FireMode fireMode = var7[var9];
               AnimationProvider fireAnimationProvider = builder.fireAnimationsBuilder.build();
               fireModeFeatureBuilder.withFireMode((new FireModeFeature.FireModeDescriptor.Builder()).withName(fireMode.name()).withType(fireMode).withDisplayName(Component.m_237115_(String.format("label.%s.fireMode.%s", "pointblank", fireMode.name().toLowerCase()))).withMaxAmmoCapacity(this.maxAmmoCapacity).withRpm(builder.rpm).withBurstShots(builder.burstShots).withDamage((double)this.getDamage()).withMaxShootingDistance((int)this.maxShootingDistance).withPelletCount(builder.pelletCount).withPelletSpread(builder.pelletSpread).withIsUsingDefaultMuzzle(true).withFireAnimationProvider(fireAnimationProvider).build());
            }
         }

         features.put(FireModeFeature.class, fireModeFeatureBuilder.build(this));
      }

      MuzzleFlashFeature muzzleFlashFeature = (MuzzleFlashFeature)features.get(MuzzleFlashFeature.class);
      if (muzzleFlashFeature == null) {
         MuzzleFlashFeature.Builder muzzleFlashFeatureBulder = new MuzzleFlashFeature.Builder();
         List<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>> fbsl = (List)builder.effectBuilders.get(FirePhase.FIRING);
         if (fbsl != null) {
            Iterator it = fbsl.iterator();

            while(it.hasNext()) {
               Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> s = (Supplier)it.next();
               EffectBuilder<? extends EffectBuilder<?, ?>, ?> eb = (EffectBuilder)s.get();
               if (eb instanceof MuzzleFlashEffect.Builder) {
                  muzzleFlashFeatureBulder.withEffect(FirePhase.FIRING, s);
               }
            }
         }

         muzzleFlashFeature = muzzleFlashFeatureBulder.build(this);
         features.put(MuzzleFlashFeature.class, muzzleFlashFeature);
      }

      ReticleFeature reticleFeature = (ReticleFeature)features.get(ReticleFeature.class);
      if (reticleFeature == null && builder.reticleOverlay != null && !MiscUtil.isGreaterThanZero(builder.pipScopeZoom)) {
         features.put(ReticleFeature.class, (new ReticleFeature.Builder()).withTexture(builder.reticleOverlay).build(this));
      }

      AimingFeature aimingFeature = (AimingFeature)features.get(AimingFeature.class);
      if (aimingFeature == null && builder.isAimingEnabled) {
         features.put(AimingFeature.class, (new AimingFeature.Builder()).withZoom(this.aimingZoom).build(this));
      }

      PipFeature pipFeature = (PipFeature)features.get(PipFeature.class);
      if (pipFeature == null && MiscUtil.isGreaterThanZero(builder.pipScopeZoom)) {
         features.put(PipFeature.class, (new PipFeature.Builder()).withZoom(builder.pipScopeZoom).withOverlayTexture(builder.reticleOverlay).build(this));
      }

      ReloadFeature reloadFeature = (ReloadFeature)features.get(ReloadFeature.class);
      if (reloadFeature == null) {
         features.put(ReloadFeature.class, (new ReloadFeature.Builder()).withMaxAmmoPerReloadIteration(builder.maxAmmoPerReloadIteration).build(this));
      }

      this.glowEffectBuilders = builder.glowEffectBuilders;
      this.rotationEffectBuilders = builder.rotationEffectBuilders;
      this.effectLauncher = new EffectLauncher(builder.effectBuilders);
      if (!this.glowEffectBuilders.isEmpty() && !features.containsKey(GlowFeature.class)) {
         features.put(GlowFeature.class, new GlowFeature());
      }

      this.features = Collections.unmodifiableMap(features);
      SingletonGeoAnimatable.registerSyncedAnimatable(this);
   }

   public String getName() {
      return this.name;
   }

   public Component m_7626_(ItemStack itemStack) {
      return Component.m_237115_(this.m_5671_(itemStack));
   }

   public float getModelScale() {
      return this.modelScale;
   }

   public Collection<Feature> getFeatures() {
      return this.features.values();
   }

   public void m_7373_(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.m_237115_("label.pointblank.damage").m_130946_(": ").m_130946_(String.format("%.2f", this.getDamage())).m_130940_(ChatFormatting.RED).m_130940_(ChatFormatting.ITALIC));
      tooltip.add(Component.m_237115_("label.pointblank.rpm").m_130946_(": ").m_130946_(String.format("%d", this.rpm)).m_130940_(ChatFormatting.RED).m_130940_(ChatFormatting.ITALIC));
      MutableComponent ammoDescription = Component.m_237115_("label.pointblank.ammo").m_130946_(": ");
      boolean isFirst = true;

      for(Iterator var7 = this.compatibleBullets.iterator(); var7.hasNext(); isFirst = false) {
         Supplier<AmmoItem> ammoItemSupplier = (Supplier)var7.next();
         if (!isFirst) {
            ammoDescription.m_130946_(", ");
         }

         AmmoItem ammoItem = (AmmoItem)ammoItemSupplier.get();
         if (ammoItem != null) {
            ammoDescription.m_7220_(Component.m_237115_(((AmmoItem)ammoItemSupplier.get()).m_5524_()));
         } else {
            ammoDescription.m_7220_(Component.m_237115_("missing_ammo"));
         }
      }

      ammoDescription.m_130940_(ChatFormatting.RED).m_130940_(ChatFormatting.ITALIC);
      tooltip.add(ammoDescription);
   }

   public MutableComponent getDisplayName() {
      return Component.m_237115_(this.m_5524_() + ".desc").m_130940_(ChatFormatting.YELLOW);
   }

   public boolean requiresPhasedReload() {
      return this.requiresPhasedReload;
   }

   public List<FireModeInstance> getMainFireModes() {
      FireModeFeature fireModeFeature = (FireModeFeature)this.getFeature(FireModeFeature.class);
      return fireModeFeature.getFireModes();
   }

   public int getRpm() {
      return this.rpm;
   }

   public boolean isAimingEnabled() {
      return this.isAimingEnabled;
   }

   public long getPrepareFireCooldownDuration() {
      return this.prepareFireCooldownDuration;
   }

   public long getCompleteFireCooldownDuration() {
      return this.completeFireCooldownDuration;
   }

   public long getEnableFireModeCooldownDuration() {
      return this.enableFireModeCooldownDuration;
   }

   public long getPrepareIdleCooldownDuration() {
      return this.prepareIdleCooldownDuration;
   }

   public int getPelletCount() {
      return this.pelletCount;
   }

   public double getPelletSpread() {
      return this.pelletSpread;
   }

   public long getDrawCooldownDuration(LivingEntity player, GunClientState state, ItemStack itemStack) {
      if (this.drawAnimationProvider == null) {
         return 0L;
      } else {
         AnimationProvider.Descriptor descriptor = this.drawAnimationProvider.getDescriptor(player, itemStack, state);
         return descriptor != null ? descriptor.timeUnit().toMillis(descriptor.duration()) : 0L;
      }
   }

   public long getInspectCooldownDuration(LivingEntity player, GunClientState state, ItemStack itemStack) {
      if (this.inspectAnimationProvider == null) {
         return 0L;
      } else {
         AnimationProvider.Descriptor descriptor = this.inspectAnimationProvider.getDescriptor(player, itemStack, state);
         return descriptor != null ? descriptor.timeUnit().toMillis(descriptor.duration()) : 0L;
      }
   }

   public long getIdleCooldownDuration(LivingEntity player, GunClientState state, ItemStack itemStack) {
      if (this.idleAnimationProvider == null) {
         return 0L;
      } else {
         AnimationProvider.Descriptor descriptor = this.idleAnimationProvider.getDescriptor(player, itemStack, state);
         return descriptor != null ? descriptor.timeUnit().toMillis(descriptor.duration()) : 0L;
      }
   }

   public long getReloadingCooldownTime(ReloadPhase phase, LivingEntity player, GunClientState state, ItemStack itemStack) {
      long cooldownTime = 0L;
      Iterator var7 = this.phasedReloads.iterator();

      while(var7.hasNext()) {
         PhasedReload reload = (PhasedReload)var7.next();
         if (phase == reload.phase && reload.predicate.test(new ConditionContext(player, itemStack, state, (ItemDisplayContext)null))) {
            cooldownTime = reload.timeUnit.toMillis(reload.cooldownTime);
            break;
         }
      }

      return cooldownTime;
   }

   public int getBurstShots(ItemStack itemStack, FireModeInstance fireModeInstance) {
      FireModeFeature mainFireModeFeature = (FireModeFeature)this.getFeature(FireModeFeature.class);
      if (mainFireModeFeature.getFireModes().contains(fireModeInstance)) {
         int burstShots;
         if (fireModeInstance.getBurstShots() == -1) {
            burstShots = this.burstShots;
         } else {
            burstShots = fireModeInstance.getBurstShots();
         }

         return burstShots;
      } else {
         List<FireModeInstance> allFireModes = getFireModes(itemStack);
         return allFireModes.contains(fireModeInstance) ? fireModeInstance.getBurstShots() : this.burstShots;
      }
   }

   public int getMaxAmmoCapacity(ItemStack itemStack, FireModeInstance fireModeInstance) {
      FireModeFeature mainFireModeFeature = (FireModeFeature)this.getFeature(FireModeFeature.class);
      if (mainFireModeFeature.getFireModes().contains(fireModeInstance)) {
         int ammoCapacity;
         if (fireModeInstance.getAmmo() == AmmoRegistry.DEFAULT_AMMO_POOL.get()) {
            ammoCapacity = this.maxAmmoCapacity;
         } else {
            ammoCapacity = fireModeInstance.getMaxAmmoCapacity();
         }

         return AmmoCapacityFeature.modifyAmmoCapacity(itemStack, ammoCapacity);
      } else {
         List<FireModeInstance> allFireModes = getFireModes(itemStack);
         if (allFireModes.contains(fireModeInstance)) {
            int ammoCapacity;
            if (fireModeInstance.getAmmo() == AmmoRegistry.DEFAULT_AMMO_POOL.get()) {
               ammoCapacity = this.maxAmmoCapacity;
            } else {
               ammoCapacity = fireModeInstance.getMaxAmmoCapacity();
            }

            return AmmoCapacityFeature.modifyAmmoCapacity(itemStack, ammoCapacity);
         } else {
            return 0;
         }
      }
   }

   public ResourceLocation getScopeOverlay() {
      return this.scopeOverlay == null && MiscUtil.isGreaterThanZero(this.pipScopeZoom) && !Config.pipScopesEnabled ? DEFAULT_SCOPE_OVERLAY : this.scopeOverlay;
   }

   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }

   public void initializeClient(Consumer<IClientItemExtensions> consumer) {
      consumer.accept(new IClientItemExtensions() {
         private GunItemRenderer renderer;

         public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            if (this.renderer == null) {
               Object fallbackAnimations;
               if (GunItem.this.firstPersonFallbackAnimations != null) {
                  fallbackAnimations = new ArrayList();
                  ((List)fallbackAnimations).add(GunItem.this.firstPersonFallbackAnimations);
                  ((List)fallbackAnimations).addAll(GunItem.this.animationType.getFallbackFirstPersonAnimations());
               } else {
                  fallbackAnimations = GunItem.this.animationType.getFallbackFirstPersonAnimations();
               }

               this.renderer = new GunItemRenderer(GunItem.this.modelResourceLocation, (List)fallbackAnimations, GunItem.this.glowEffectBuilders);
            }

            return this.renderer;
         }

         public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm, ItemStack itemInHand, float partialTick, float equipProcess, float swingProcess) {
            return true;
         }
      });
   }

   public int getMaxStackSize(ItemStack stack) {
      return 1;
   }

   public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
      return true;
   }

   public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
      return InteractionResult.SUCCESS;
   }

   public InteractionResult m_6880_(ItemStack itemStack, Player player, LivingEntity entity, InteractionHand hand) {
      return InteractionResult.SUCCESS;
   }

   public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
      return true;
   }

   public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
      return true;
   }

   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(new AnimationController[]{(new BlendingAnimationController(this, "walking", 2, false, (state) -> {
         return PlayState.STOP;
      })).withTransition("animation.model.standing", "animation.model.walking", 2, false).withTransition("animation.model.standing", "animation.model.walking_backwards", 2, false).withTransition("animation.model.standing", "animation.model.runningstart", 2, false).withTransition("animation.model.walking", "animation.model.runningstart", 2, false).withTransition("animation.model.runningstart", "animation.model.running", 2, false).withTransition("animation.model.runningstart", "animation.model.runningend", 3, false).withTransition("animation.model.running", "animation.model.runningend", 2, false).withTransition("animation.model.runningend", "animation.model.walking", 3, false).withTransition("animation.model.runningend", "animation.model.standing", 3, false).withTransition("animation.model.runningend", "animation.model.runningstart", 2, false).withSpeedProvider((p, c) -> {
         double speed = (double)p.m_6113_();
         AttributeInstance attribute = p.m_21051_(Attributes.f_22279_);
         double baseSpeed = attribute != null ? attribute.m_22115_() : speed;
         if (baseSpeed == 0.0D) {
            baseSpeed = speed;
         }

         if (p.m_20142_()) {
            baseSpeed += baseSpeed * 0.3D;
         }

         double ratio = speed / baseSpeed;
         if (((LocalPlayer)p).m_108635_() || p.m_6069_() || p.m_20069_()) {
            ratio *= 0.6D;
         }

         return Math.sqrt(Mth.m_14008_(ratio, 0.1D, 10.0D));
      }).triggerableAnim("animation.model.walking", RAW_ANIMATION_WALKING).triggerableAnim("animation.model.crouching", RAW_ANIMATION_CROUCHING).triggerableAnim("animation.model.walking_aiming", RAW_ANIMATION_WALKING_AIMING).triggerableAnim("animation.model.walking_backwards", RAW_ANIMATION_WALKING_BACKWARDS).triggerableAnim("animation.model.walking_left", RAW_ANIMATION_WALKING_LEFT).triggerableAnim("animation.model.walking_right", RAW_ANIMATION_WALKING_RIGHT).triggerableAnim("animation.model.runningstart", RAW_ANIMATION_PREPARE_RUNNING).triggerableAnim("animation.model.running", RAW_ANIMATION_RUNNING).triggerableAnim("animation.model.runningend", RAW_ANIMATION_COMPLETE_RUNNING).triggerableAnim("animation.model.standing", RAW_ANIMATION_STANDING).triggerableAnim("animation.model.off_ground", RAW_ANIMATION_OFF_GROUND).triggerableAnim("animation.model.off_ground_sprinting", RAW_ANIMATION_OFF_GROUND_SPRINTING).setSoundKeyframeHandler((event) -> {
         Player player = ClientUtil.getClientPlayer();
         if (player != null) {
            SoundKeyframeData soundKeyframeData = event.getKeyframeData();
            String soundName = soundKeyframeData.getSound();
            SoundEvent soundEvent = SoundRegistry.getSoundEvent(soundName);
            if (soundEvent != null) {
               player.m_5496_(soundEvent, 1.0F, 1.0F);
            }
         }

      })});
      List<GunStateAnimationController> reloadAnimationControllers = this.createReloadAnimationControllers();
      Iterator var3 = reloadAnimationControllers.iterator();

      GunStateAnimationController prepareFiringAnimationController;
      while(var3.hasNext()) {
         prepareFiringAnimationController = (GunStateAnimationController)var3.next();
         controllers.add(new AnimationController[]{prepareFiringAnimationController});
      }

      AnimationController<GunItem> fireAnimationController = new GunStateAnimationController(this, "fire_controller", "animation.model.fire", (ctx) -> {
         return ctx.gunClientState().getFireState() == GunClientState.FireState.FIRE_SINGLE || ctx.gunClientState().getFireState() == GunClientState.FireState.FIRE_AUTO || ctx.gunClientState().getFireState() == GunClientState.FireState.FIRE_BURST || ctx.gunClientState().getFireState() == GunClientState.FireState.FIRE_COOLDOWN_SINGLE || ctx.gunClientState().getFireState() == GunClientState.FireState.FIRE_COOLDOWN_AUTO || ctx.gunClientState().getFireState() == GunClientState.FireState.FIRE_COOLDOWN_BURST || this.completeFireCooldownDuration == 0L && ctx.gunClientState().isIdle();
      }) {
         public void onStartFiring(LivingEntity player, GunClientState state, ItemStack itemStack) {
            if (ClientUtil.isFirstPerson(player)) {
               this.scheduleReset(player, state, itemStack, FireModeFeature.getFireAnimation(player, state, itemStack));
            }

         }
      };
      fireAnimationController.setSoundKeyframeHandler((event) -> {
         Player player = ClientUtils.getClientPlayer();
         if (player != null) {
            SoundKeyframeData soundKeyframeData = event.getKeyframeData();
            String soundName = soundKeyframeData.getSound();
            SoundEvent soundEvent = SoundRegistry.getSoundEvent(soundName);
            if (soundEvent != null) {
               player.m_5496_(soundEvent, this.fireSoundVolume, 1.0F);
            }
         }

      });
      controllers.add(new AnimationController[]{fireAnimationController});
      prepareFiringAnimationController = new GunStateAnimationController(this, "prepare_fire_controller", "animation.model.preparefire", (ctx) -> {
         return ctx.gunClientState().isPreparingFiring();
      }) {
         public void onPrepareFiring(LivingEntity player, GunClientState state, ItemStack itemStack) {
            if (ClientUtil.isFirstPerson(player)) {
               this.scheduleReset(player, state, itemStack, FireModeFeature.getPrepareFireAnimation(player, state, itemStack));
            }

         }
      };
      prepareFiringAnimationController.setSoundKeyframeHandler((event) -> {
         Player player = ClientUtils.getClientPlayer();
         if (player != null) {
            SoundKeyframeData soundKeyframeData = event.getKeyframeData();
            String soundName = soundKeyframeData.getSound();
            SoundEvent soundEvent = SoundRegistry.getSoundEvent(soundName);
            if (soundEvent != null) {
               player.m_5496_(soundEvent, 1.0F, 1.0F);
            }
         }

      });
      controllers.add(new AnimationController[]{prepareFiringAnimationController});
      AnimationController<GunItem> completeFiringAnimationController = new GunStateAnimationController(this, "complete_fire_controller", "animation.model.completefire", (ctx) -> {
         return ctx.gunClientState().isCompletingFiring();
      }) {
         public void onCompleteFiring(LivingEntity player, GunClientState state, ItemStack itemStack) {
            if (ClientUtil.isFirstPerson(player)) {
               this.scheduleReset(player, state, itemStack, FireModeFeature.getCompleteFireAnimation(player, state, itemStack));
            }

         }
      };
      completeFiringAnimationController.setSoundKeyframeHandler((event) -> {
         Player player = ClientUtils.getClientPlayer();
         if (player != null) {
            SoundKeyframeData soundKeyframeData = event.getKeyframeData();
            String soundName = soundKeyframeData.getSound();
            SoundEvent soundEvent = SoundRegistry.getSoundEvent(soundName);
            if (soundEvent != null) {
               player.m_5496_(soundEvent, 1.0F, 1.0F);
            }
         }

      });
      controllers.add(new AnimationController[]{completeFiringAnimationController});
      AnimationController<GunItem> drawAnimationController = new GunStateAnimationController(this, "draw_controller", "animation.model.draw", (ctx) -> {
         return ctx.gunClientState().isDrawing();
      }) {
         private String getAnimationName(LivingEntity player, GunClientState state, ItemStack itemStack) {
            if (GunItem.this.drawAnimationProvider == null) {
               return null;
            } else {
               AnimationProvider.Descriptor descriptor = GunItem.this.drawAnimationProvider.getDescriptor(player, itemStack, state);
               return descriptor != null ? descriptor.animationName() : null;
            }
         }

         public void onDrawing(LivingEntity player, GunClientState state, ItemStack itemStack) {
            if (ClientUtil.isFirstPerson(player)) {
               this.scheduleReset(player, state, itemStack, this.getAnimationName(player, state, itemStack));
            }

         }
      };
      drawAnimationController.setSoundKeyframeHandler((event) -> {
         Player player = ClientUtils.getClientPlayer();
         if (player != null) {
            SoundKeyframeData soundKeyframeData = event.getKeyframeData();
            String soundName = soundKeyframeData.getSound();
            SoundEvent soundEvent = SoundRegistry.getSoundEvent(soundName);
            if (soundEvent != null) {
               player.m_5496_(soundEvent, 1.0F, 1.0F);
            }
         }

      });
      controllers.add(new AnimationController[]{drawAnimationController});
      AnimationController<GunItem> inspectAnimationController = new GunStateAnimationController(this, "inspect_controller", "animation.model.inspect", (ctx) -> {
         return ctx.gunClientState().isInspecting();
      }) {
         private String getAnimationName(LivingEntity player, GunClientState state, ItemStack itemStack) {
            if (GunItem.this.inspectAnimationProvider == null) {
               return null;
            } else {
               AnimationProvider.Descriptor descriptor = GunItem.this.inspectAnimationProvider.getDescriptor(player, itemStack, state);
               return descriptor != null ? descriptor.animationName() : null;
            }
         }

         public void onInspecting(LivingEntity player, GunClientState state, ItemStack itemStack) {
            if (ClientUtil.isFirstPerson(player)) {
               this.scheduleReset(player, state, itemStack, this.getAnimationName(player, state, itemStack));
            }

         }
      };
      inspectAnimationController.setSoundKeyframeHandler((event) -> {
         Player player = ClientUtils.getClientPlayer();
         if (player != null) {
            SoundKeyframeData soundKeyframeData = event.getKeyframeData();
            String soundName = soundKeyframeData.getSound();
            SoundEvent soundEvent = SoundRegistry.getSoundEvent(soundName);
            if (soundEvent != null) {
               player.m_5496_(soundEvent, 1.0F, 1.0F);
            }
         }

      });
      controllers.add(new AnimationController[]{inspectAnimationController});
      AnimationController<GunItem> idleAnimationController = new GunStateAnimationController(this, "idle_controller", "animation.model.idle", (ctx) -> {
         return ctx.gunClientState().getFireState() == GunClientState.FireState.IDLE || ctx.gunClientState().getFireState() == GunClientState.FireState.IDLE_COOLDOWN;
      }) {
         private String getAnimationName(LivingEntity player, GunClientState state, ItemStack itemStack) {
            if (GunItem.this.idleAnimationProvider == null) {
               return null;
            } else {
               AnimationProvider.Descriptor descriptor = GunItem.this.idleAnimationProvider.getDescriptor(player, itemStack, state);
               return descriptor != null ? descriptor.animationName() : null;
            }
         }

         public void onIdle(LivingEntity player, GunClientState state, ItemStack itemStack) {
            if (ClientUtil.isFirstPerson(player)) {
               this.scheduleReset(player, state, itemStack, this.getAnimationName(player, state, itemStack));
            }

         }
      };
      idleAnimationController.setSoundKeyframeHandler((event) -> {
         Player player = ClientUtils.getClientPlayer();
         if (player != null) {
            SoundKeyframeData soundKeyframeData = event.getKeyframeData();
            String soundName = soundKeyframeData.getSound();
            SoundEvent soundEvent = SoundRegistry.getSoundEvent(soundName);
            if (soundEvent != null) {
               player.m_5496_(soundEvent, 1.0F, 1.0F);
            }
         }

      });
      controllers.add(new AnimationController[]{idleAnimationController});
      AnimationController<GunItem> enableFireModeAimationController = new GunStateAnimationController(this, "enable_fire_mode_controller", "animation.model.enablefiremode", (ctx) -> {
         return ctx.gunClientState().isChangingFireMode();
      }) {
         public void onEnablingFireMode(LivingEntity player, GunClientState state, ItemStack itemStack) {
            if (ClientUtil.isFirstPerson(player)) {
               this.scheduleReset(player, state, itemStack, FireModeFeature.getEnableFireModeAnimation(player, state, itemStack));
            }

         }
      };
      enableFireModeAimationController.setSoundKeyframeHandler((event) -> {
         Player player = ClientUtils.getClientPlayer();
         if (player != null) {
            SoundKeyframeData soundKeyframeData = event.getKeyframeData();
            String soundName = soundKeyframeData.getSound();
            SoundEvent soundEvent = SoundRegistry.getSoundEvent(soundName);
            if (soundEvent != null) {
               player.m_5496_(soundEvent, 1.0F, 1.0F);
            }
         }

      });
      controllers.add(new AnimationController[]{enableFireModeAimationController});
   }

   private static boolean isCompatibleBullet(Item ammoItem, ItemStack gunStack, FireModeInstance fireModeInstance) {
      boolean result = false;
      Item var5 = gunStack.m_41720_();
      if (var5 instanceof GunItem) {
         GunItem gunItem = (GunItem)var5;
         List var8 = getFireModes(gunStack);
         if (!var8.contains(fireModeInstance)) {
            return false;
         } else {
            if (fireModeInstance.isUsingDefaultAmmoPool()) {
               Iterator var6 = gunItem.compatibleBullets.iterator();

               while(var6.hasNext()) {
                  Supplier<AmmoItem> compatibleBullet = (Supplier)var6.next();
                  if (Objects.equals(compatibleBullet.get(), ammoItem)) {
                     result = true;
                     break;
                  }
               }
            } else {
               result = ammoItem == fireModeInstance.getAmmo();
            }

            return result;
         }
      } else {
         return false;
      }
   }

   public int canReloadGun(ItemStack gunStack, Player player, FireModeInstance fireModeInstance) {
      GunItem gunItem = (GunItem)gunStack.m_41720_();
      int maxCapacity = gunItem.getMaxAmmoCapacity(gunStack, fireModeInstance);
      int currentAmmo = getAmmo(gunStack, fireModeInstance);
      int ammoNeeded = maxCapacity - currentAmmo;
      if (ammoNeeded <= 0) {
         return 0;
      } else if (player.m_7500_()) {
         return ammoNeeded;
      } else {
         int availableBullets = 0;

         int potentialReloadAmount;
         for(potentialReloadAmount = 0; potentialReloadAmount < player.m_150109_().m_6643_(); ++potentialReloadAmount) {
            ItemStack itemStack = player.m_150109_().m_8020_(potentialReloadAmount);
            if (isCompatibleBullet(itemStack.m_41720_(), gunStack, fireModeInstance)) {
               availableBullets += itemStack.m_41613_();
            }

            if (availableBullets >= ammoNeeded) {
               break;
            }
         }

         potentialReloadAmount = Math.min(ammoNeeded, availableBullets);
         return potentialReloadAmount;
      }
   }

   int reloadGun(ItemStack gunStack, Player player, FireModeInstance fireModeInstance) {
      if (!(gunStack.m_41720_() instanceof GunItem)) {
         return 0;
      } else {
         GunItem gunItem = (GunItem)gunStack.m_41720_();
         if (!gunItem.isEnabled()) {
            return 0;
         } else {
            int maxCapacity = gunItem.getMaxAmmoCapacity(gunStack, fireModeInstance);
            int currentAmmo = getAmmo(gunStack, fireModeInstance);
            int neededAmmo = maxCapacity - currentAmmo;
            if (neededAmmo <= 0) {
               return currentAmmo;
            } else {
               int foundAmmoCount;
               if (player.m_7500_()) {
                  foundAmmoCount = currentAmmo + neededAmmo;
                  setAmmo(gunStack, fireModeInstance, foundAmmoCount);
                  return foundAmmoCount;
               } else {
                  foundAmmoCount = 0;

                  int i;
                  for(i = 0; i < player.m_150109_().f_35974_.size(); ++i) {
                     ItemStack inventoryItem = (ItemStack)player.m_150109_().f_35974_.get(i);
                     if (isCompatibleBullet(inventoryItem.m_41720_(), gunStack, fireModeInstance)) {
                        int availableBullets = inventoryItem.m_41613_();
                        if (availableBullets <= neededAmmo) {
                           foundAmmoCount += availableBullets;
                           neededAmmo -= availableBullets;
                           player.m_150109_().f_35974_.set(i, ItemStack.f_41583_);
                        } else {
                           inventoryItem.m_41774_(neededAmmo);
                           foundAmmoCount += neededAmmo;
                           neededAmmo = 0;
                        }

                        if (neededAmmo == 0) {
                           break;
                        }
                     }
                  }

                  i = currentAmmo + foundAmmoCount;
                  setAmmo(gunStack, fireModeInstance, i);
                  return i;
               }
            }
         }
      }
   }

   public void handleClientReloadRequest(ServerPlayer player, ItemStack itemStack, UUID clientStateId, int slotIndex, FireModeInstance fireModeInstance) {
      boolean isOffhand = player.m_21206_() == itemStack;
      if (!isOffhand && itemStack != null) {
         int ammo = this.reloadGun(itemStack, player, fireModeInstance);
         UUID itemStackId = getItemStackId(itemStack);
         Network.networkChannel.send(PacketDistributor.PLAYER.with(() -> {
            return player;
         }), new ReloadResponsePacket(itemStackId, slotIndex, 0, ammo > 0, ammo, fireModeInstance));
      }

   }

   public static UUID getItemStackId(ItemStack itemStack) {
      CompoundTag idTag = itemStack.m_41783_();
      return idTag != null ? MiscUtil.getTagId(idTag) : null;
   }

   public void m_6883_(ItemStack itemStack, Level level, Entity entity, int itemSlot, boolean isSelected) {
      boolean isOffhand = entity instanceof Player && ((Player)entity).m_21206_() == itemStack;
      if (!level.f_46443_) {
         this.ensureItemStack(itemStack, level, entity, isOffhand);
      } else {
         GunClientState state = GunClientState.getState((Player)entity, itemStack, itemSlot, isOffhand);
         if (state != null && entity instanceof Player) {
            state.inventoryTick((Player)entity, itemStack, isSelected);
         }
      }

   }

   public void ensureItemStack(ItemStack itemStack, Level level, Entity entity, boolean isOffhand) {
      GeoItem.getOrAssignId(itemStack, (ServerLevel)level);
      getOrAssignRandomSeed(itemStack);
      CompoundTag stateTag = itemStack.m_41784_();
      long mid = stateTag.m_128454_("mid");
      long lid = stateTag.m_128454_("lid");
      if (mid == 0L && lid == 0L) {
         UUID newId = UUID.randomUUID();
         stateTag.m_128356_("mid", newId.getMostSignificantBits());
         stateTag.m_128356_("lid", newId.getLeastSignificantBits());
         stateTag.m_128405_("ammo", this.maxAmmoCapacity == Integer.MAX_VALUE ? Integer.MAX_VALUE : 0);
         stateTag.m_128365_("ammox", new CompoundTag());
         List<FireModeInstance> mainFireModes = this.getMainFireModes();
         if (mainFireModes != null && !mainFireModes.isEmpty()) {
            stateTag.m_128362_("fmid", ((FireModeInstance)this.getMainFireModes().get(0)).getId());
         }

         stateTag.m_128379_("aim", false);
         Item var13 = itemStack.m_41720_();
         if (var13 instanceof AttachmentHost) {
            AttachmentHost attachmentHost = (AttachmentHost)var13;
            Collection<Attachment> defaultAttachments = attachmentHost.getDefaultAttachments();
            Iterator var14 = defaultAttachments.iterator();

            while(var14.hasNext()) {
               Attachment attachment = (Attachment)var14.next();
               Attachments.addAttachment(itemStack, new ItemStack(attachment), true);
            }
         }
      } else {
         this.ensureValidFireModeSelected(itemStack);
      }

      Attachments.ensureValidAttachmentsSelected(itemStack);
   }

   public static void initStackForCrafting(ItemStack itemStack) {
      Item var2 = itemStack.m_41720_();
      if (var2 instanceof GunItem) {
         GunItem gunItem = (GunItem)var2;
         CompoundTag stateTag = itemStack.m_41784_();
         long mid = stateTag.m_128454_("mid");
         long lid = stateTag.m_128454_("lid");
         if (mid == 0L && lid == 0L) {
            UUID newId = UUID.randomUUID();
            stateTag.m_128356_("mid", newId.getMostSignificantBits());
            stateTag.m_128356_("lid", newId.getLeastSignificantBits());
            stateTag.m_128365_("ammox", new CompoundTag());
            List<FireModeInstance> mainFireModes = gunItem.getMainFireModes();
            if (mainFireModes != null && !mainFireModes.isEmpty()) {
               stateTag.m_128362_("fmid", ((FireModeInstance)gunItem.getMainFireModes().get(0)).getId());
            }

            stateTag.m_128379_("aim", false);
            Item var10 = itemStack.m_41720_();
            if (var10 instanceof AttachmentHost) {
               AttachmentHost attachmentHost = (AttachmentHost)var10;
               Collection<Attachment> defaultAttachments = attachmentHost.getDefaultAttachments();
               Iterator var11 = defaultAttachments.iterator();

               while(var11.hasNext()) {
                  Attachment attachment = (Attachment)var11.next();
                  Attachments.addAttachment(itemStack, new ItemStack(attachment), true);
               }
            }
         }

      }
   }

   private void ensureValidFireModeSelected(ItemStack itemStack) {
      CompoundTag idTag = itemStack.m_41783_();
      if (idTag != null) {
         UUID fireModeInstanceId = idTag.m_128403_("fmid") ? idTag.m_128342_("fmid") : null;
         FireModeInstance selectedModeInstance = FireModeInstance.getOrElse(fireModeInstanceId, (FireModeInstance)null);
         List<FireModeInstance> fireModes = getFireModes(itemStack);
         if (selectedModeInstance != null && !fireModes.contains(selectedModeInstance)) {
            selectedModeInstance = null;
         }

         if (selectedModeInstance == null && !fireModes.isEmpty()) {
            selectedModeInstance = (FireModeInstance)fireModes.get(0);
            setFireModeInstance(itemStack, selectedModeInstance);
         }

         if (selectedModeInstance == null) {
            idTag.m_128473_("fmid");
         }
      }

   }

   private static long getOrAssignRandomSeed(ItemStack stack) {
      CompoundTag tag = stack.m_41784_();
      long seed = tag.m_128454_("seed");
      if (tag.m_128425_("seed", 99)) {
         return seed;
      } else {
         seed = random.nextLong();
         tag.m_128356_("seed", seed);
         return seed;
      }
   }

   public void m_5551_(ItemStack stack, Level level, LivingEntity shooter, int ticksRemaining) {
   }

   private Predicate<Block> getDestroyBlockByHitScanPredicate() {
      return (block) -> {
         return Config.bulletsBreakGlassEnabled && (block instanceof AbstractGlassBlock || block instanceof StainedGlassPaneBlock || block == Blocks.f_50185_);
      };
   }

   private Predicate<Block> getPassThroughBlocksByHitScanPredicate() {
      return (block) -> {
         return block instanceof BushBlock || block instanceof LeavesBlock;
      };
   }

   @OnlyIn(Dist.CLIENT)
   public void requestFireFromServer(GunClientState gunClientState, Player player, ItemStack itemStack, Entity targetEntity) {
      int activeSlot = player.m_150109_().f_35977_;
      LOGGER.debug("{} requesting fire from server", System.currentTimeMillis() % 100000L);
      SoundFeature.playFireSound(player, itemStack);
      Pair<Integer, Double> pcs = FireModeFeature.getPelletCountAndSpread(player, gunClientState, itemStack);
      int shotCount = (Integer)pcs.getFirst() > 0 ? (Integer)pcs.getFirst() : 1;
      long requestSeed = random.nextLong();
      FireModeInstance fireModeInstance = getFireModeInstance(itemStack);
      AmmoItem projectileItem = this.getFirstCompatibleProjectile(itemStack, fireModeInstance);
      if (projectileItem != null) {
         GunStatePoseProvider gunStatePoseProvider = GunStatePoseProvider.getInstance();
         Vec3[] pd = gunStatePoseProvider.getPositionAndDirection(gunClientState, GunStatePoseProvider.PoseContext.FIRST_PERSON_MUZZLE);
         if (pd == null) {
            pd = gunStatePoseProvider.getPositionAndDirection(gunClientState, GunStatePoseProvider.PoseContext.FIRST_PERSON_MUZZLE_FLASH);
         }

         Vec3 startPos;
         Vec3 direction;
         if (pd != null) {
            startPos = pd[0];
            direction = pd[1];
         } else {
            startPos = player.m_146892_();
            direction = player.m_20252_(0.0F);
            startPos = startPos.m_82549_(direction.m_82541_().m_82542_(2.0D, 2.0D, 2.0D));
         }

         Network.networkChannel.sendToServer(new ProjectileFireRequestPacket(fireModeInstance, getItemStackId(itemStack), activeSlot, gunClientState.isAiming(), startPos.f_82479_, startPos.f_82480_, startPos.f_82481_, direction.f_82479_, direction.f_82480_, direction.f_82481_, targetEntity != null ? targetEntity.m_19879_() : -1, requestSeed));
      } else {
         double adjustedInaccuracy = this.adjustInaccuracy(player, itemStack, gunClientState.isAiming());
         long itemSeed = getOrAssignRandomSeed(itemStack);
         long xorSeed = itemSeed ^ requestSeed;
         this.acquireHitScan(player, itemStack, gunClientState, shotCount, xorSeed, adjustedInaccuracy);
         Network.networkChannel.sendToServer(new HitScanFireRequestPacket(fireModeInstance, getItemStackId(itemStack), activeSlot, gunClientState.isAiming(), requestSeed));
         LOGGER.debug("{} sent fire request to server", System.currentTimeMillis() % 100000L);
      }

   }

   private void acquireHitScan(Player player, ItemStack itemStack, @NotNull GunClientState gunClientState, int shotCount, long seed, double adjustedInaccuracy) {
      if (shotCount <= 1) {
         double maxDistance = this.getMaxClientShootingDistance(itemStack, gunClientState);
         List<HitResult> hitResults = HitScan.getObjectsInCrosshair(player, player.m_146892_(), player.m_20252_(0.0F), 1.0F, maxDistance, shotCount, adjustedInaccuracy, seed, this.getDestroyBlockByHitScanPredicate(), this.getPassThroughBlocksByHitScanPredicate(), new ArrayList());
         Iterator var12 = hitResults.iterator();

         while(var12.hasNext()) {
            HitResult hitResult = (HitResult)var12.next();
            gunClientState.acquireHitScan(player, itemStack, hitResult);
         }

      }
   }

   @OnlyIn(Dist.CLIENT)
   private double getMaxClientShootingDistance(ItemStack itemStack, GunClientState gunClientState) {
      Minecraft mc = Minecraft.m_91087_();
      double maxDistance = (double)Math.min(mc.f_91066_.m_193772_() * 16, FireModeFeature.getMaxShootingDistance(itemStack));
      if (!gunClientState.isAiming()) {
         maxDistance = Math.min(maxDistance, 100.0D);
      }

      return maxDistance;
   }

   private double adjustInaccuracy(Player player, ItemStack itemStack, boolean isAiming) {
      double adjustedInaccuracy;
      if (isAiming) {
         adjustedInaccuracy = this.inaccuracyAiming;
      } else if (player.m_20142_()) {
         adjustedInaccuracy = this.inaccuracySprinting;
      } else {
         adjustedInaccuracy = this.inaccuracy;
      }

      Pair<Integer, Double> pcs = FireModeFeature.getPelletCountAndSpread(player, (GunClientState)null, itemStack);
      if ((Integer)pcs.getFirst() > 0) {
         adjustedInaccuracy += (Double)pcs.getSecond();
      }

      float accuracyModifier = AccuracyFeature.getAccuracyModifier(itemStack);
      return adjustedInaccuracy / (double)accuracyModifier;
   }

   public static LazyOptional<Integer> getClientSideAmmo(Player player, ItemStack itemStack, int slotIndex) {
      GunClientState state = GunClientState.getState(player, itemStack, slotIndex, false);
      return LazyOptional.of(state != null ? () -> {
         return state.getAmmoCount(getFireModeInstance(itemStack));
      } : null);
   }

   public double getInacuracy() {
      return this.inaccuracy;
   }

   public int getShotsPerTrace() {
      return this.shotsPerTrace;
   }

   public static int getAmmo(ItemStack itemStack, FireModeInstance fireModeInstance) {
      if (!(itemStack.m_41720_() instanceof GunItem)) {
         return 0;
      } else {
         CompoundTag idTag = itemStack.m_41783_();
         if (idTag != null) {
            if (fireModeInstance.isUsingDefaultAmmoPool()) {
               return idTag.m_128451_("ammo");
            }

            CompoundTag auxAmmoTag = idTag.m_128469_("ammox");
            if (auxAmmoTag != null) {
               return auxAmmoTag.m_128451_(fireModeInstance.getAmmo().getName());
            }
         }

         return 0;
      }
   }

   public static void setAmmo(ItemStack itemStack, FireModeInstance fireModeInstance, int ammo) {
      if (itemStack.m_41720_() instanceof GunItem) {
         LOGGER.debug("Setting ammo in stack {} to {}", System.identityHashCode(itemStack), ammo);
         CompoundTag idTag = itemStack.m_41783_();
         if (idTag != null) {
            if (fireModeInstance.isUsingDefaultAmmoPool()) {
               idTag.m_128405_("ammo", ammo);
            } else {
               CompoundTag auxAmmoTag = idTag.m_128469_("ammox");
               auxAmmoTag.m_128405_(fireModeInstance.getAmmo().getName(), ammo);
               idTag.m_128365_("ammox", auxAmmoTag);
            }
         }

      }
   }

   public static int decrementAmmo(ItemStack itemStack) {
      if (!(itemStack.m_41720_() instanceof GunItem)) {
         return 0;
      } else {
         CompoundTag idTag = itemStack.m_41783_();
         if (idTag != null) {
            int ammo = idTag.m_128451_("ammo");
            if (ammo <= 0) {
               return -1;
            } else {
               --ammo;
               idTag.m_128405_("ammo", ammo);
               return ammo;
            }
         } else {
            return 0;
         }
      }
   }

   public static FireMode getSelectedFireModeType(ItemStack itemStack) {
      FireModeInstance fireModeInstance = getFireModeInstance(itemStack);
      return fireModeInstance != null ? fireModeInstance.getType() : null;
   }

   public static FireModeInstance getFireModeInstance(ItemStack itemStack) {
      Item var2 = itemStack.m_41720_();
      if (var2 instanceof GunItem) {
         GunItem gunItem = (GunItem)var2;
         CompoundTag idTag = itemStack.m_41783_();
         if (idTag != null) {
            UUID fireModeInstanceId = idTag.m_128403_("fmid") ? idTag.m_128342_("fmid") : null;
            FireModeInstance fireModeInstance = null;
            if (fireModeInstanceId != null) {
               fireModeInstance = FireModeInstance.getOrElse(fireModeInstanceId, (FireModeInstance)null);
            }

            if (fireModeInstance == null) {
               fireModeInstance = (FireModeInstance)gunItem.getMainFireModes().get(0);
            }

            return fireModeInstance;
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   private static void setFireModeInstance(ItemStack itemStack, FireModeInstance fireModeInstance) {
      if (itemStack.m_41720_() instanceof GunItem) {
         CompoundTag idTag = itemStack.m_41783_();
         if (idTag != null) {
            idTag.m_128362_("fmid", fireModeInstance.getId());
            LOGGER.debug("Set fire mode instance to {}, tag: {}", fireModeInstance.getDisplayName(), idTag);
         }

      }
   }

   public void handleClientProjectileFireRequest(ServerPlayer player, FireModeInstance fireModeInstance, UUID stateId, int slotIndex, int correlationId, boolean isAiming, double spawnPositionX, double spawnPositionY, double spawnPositionZ, double spawnDirectionX, double spawnDirectionY, double spawnDirectionZ, int targetEntityId, long requestSeed) {
      LOGGER.debug("Handling client projectile file request");
      ItemStack itemStack = player.m_150109_().m_8020_(slotIndex);
      AmmoItem projectileItem = this.getFirstCompatibleProjectile(itemStack, fireModeInstance);
      if (projectileItem == null) {
         LOGGER.error("Attempted to handle client projectile fire request with an item that does not support projectiles: " + this);
      } else if (this.isEnabled() && projectileItem.isEnabled()) {
         boolean isOffhand = player.m_21206_() == itemStack;
         if (itemStack != null && !isOffhand && itemStack.m_41720_() instanceof GunItem) {
            int ammo = false;
            int ammo;
            if ((ammo = getAmmo(itemStack, fireModeInstance)) > 0) {
               LOGGER.debug("Received client projectile file request");
               Entity targetEntity = null;
               if (targetEntityId >= 0) {
                  targetEntity = MiscUtil.getLevel(player).m_6815_(targetEntityId);
               }

               ProjectileLike projectile = null;
               if (targetEntity != null) {
                  HitResult hitResult = HitScan.ensureEntityInCrosshair(player, targetEntity, 0.0F, 400.0D, 2.0F);
                  if (hitResult != null && hitResult.m_6662_() == Type.ENTITY && ((EntityHitResult)hitResult).m_82443_() == targetEntity) {
                     projectile = projectileItem.createProjectile(player, spawnPositionX, spawnPositionY, spawnPositionZ);
                     projectile.launchAtTargetEntity(player, hitResult, targetEntity);
                  }
               } else {
                  projectile = projectileItem.createProjectile(player, spawnPositionX, spawnPositionY, spawnPositionZ);
                  long xorSeed = getOrAssignRandomSeed(itemStack) ^ requestSeed;
                  double adjustedInaccuracy = this.adjustInaccuracy(player, itemStack, isAiming);
                  projectile.launchAtLookTarget(player, adjustedInaccuracy, xorSeed);
               }

               if (projectile != null) {
                  if (this.getMaxAmmoCapacity(itemStack, fireModeInstance) < Integer.MAX_VALUE) {
                     setAmmo(itemStack, fireModeInstance, ammo - 1);
                  }

                  SoundFeature.playFireSound(player, itemStack);
                  MiscUtil.getLevel(player).m_7967_((Entity)projectile);
               } else {
                  LOGGER.debug("Did not fire projectile");
               }
            }

         }
      }
   }

   public List<AmmoItem> getCompatibleAmmo() {
      return this.compatibleBullets.stream().map(Supplier::get).toList();
   }

   private AmmoItem getFirstCompatibleProjectile(ItemStack gunStack, FireModeInstance fireModeInstance) {
      Item var4 = gunStack.m_41720_();
      if (var4 instanceof GunItem) {
         GunItem gunItem = (GunItem)var4;
         List var9 = getFireModes(gunStack);
         if (!var9.contains(fireModeInstance)) {
            return null;
         } else {
            AmmoItem projectileItem = null;
            if (fireModeInstance.isUsingDefaultAmmoPool()) {
               Iterator var6 = gunItem.compatibleBullets.iterator();

               while(var6.hasNext()) {
                  Supplier<AmmoItem> ammoSupplier = (Supplier)var6.next();
                  AmmoItem ammoItem = (AmmoItem)ammoSupplier.get();
                  if (ammoItem != null && ammoItem.isHasProjectile()) {
                     projectileItem = ammoItem;
                     break;
                  }
               }
            } else {
               AmmoItem ammoItem = fireModeInstance.getAmmo();
               if (ammoItem.isHasProjectile()) {
                  projectileItem = ammoItem;
               }
            }

            return projectileItem;
         }
      } else {
         return null;
      }
   }

   public void handleClientHitScanFireRequest(ServerPlayer player, FireModeInstance fireModeInstance, UUID stateId, int slotIndex, int correlationId, boolean isAiming, long requestSeed) {
      try {
         LOGGER.debug("{} handling client fire request", System.currentTimeMillis() % 100000L);
         ItemStack itemStack = player.m_150109_().m_8020_(slotIndex);
         AmmoItem projectileItem = this.getFirstCompatibleProjectile(itemStack, fireModeInstance);
         if (projectileItem != null) {
            LOGGER.error("Attempted to handle client hit scan fire request with an item that fires projectiles: " + this);
            return;
         }

         if (!this.isEnabled()) {
            return;
         }

         boolean isOffhand = player.m_21206_() == itemStack;
         if (itemStack == null || isOffhand || !(itemStack.m_41720_() instanceof GunItem)) {
            return;
         }

         List<HitResult> hitResults = new ArrayList();
         int ammo = false;
         int ammo;
         if ((ammo = getAmmo(itemStack, fireModeInstance)) > 0) {
            if (this.getMaxAmmoCapacity(itemStack, fireModeInstance) < Integer.MAX_VALUE) {
               setAmmo(itemStack, fireModeInstance, ammo - 1);
            }

            SoundFeature.playFireSound(player, itemStack);
            Pair<Integer, Double> pcs = FireModeFeature.getPelletCountAndSpread(player, (GunClientState)null, itemStack);
            int shotCount = (Integer)pcs.getFirst() > 0 ? (Integer)pcs.getFirst() : 1;
            double adjustedInaccuracy = this.adjustInaccuracy(player, itemStack, isAiming);
            long xorSeed = getOrAssignRandomSeed(itemStack) ^ requestSeed;
            Vec3 eyePos = player.m_146892_();
            Vec3 lookVec = player.m_20252_(0.0F);
            ServerLevel level = (ServerLevel)MiscUtil.getLevel(player);
            double maxHitScanDistance = this.getMaxServerShootingDistance(itemStack, isAiming, level);
            List<BlockPos> blockPosToDestroy = new ArrayList();
            hitResults.addAll(HitScan.getObjectsInCrosshair(player, eyePos, lookVec, 0.0F, maxHitScanDistance, shotCount, adjustedInaccuracy, xorSeed, this.getDestroyBlockByHitScanPredicate(), this.getPassThroughBlocksByHitScanPredicate(), blockPosToDestroy));
            LOGGER.debug("{} obtained hit results", System.currentTimeMillis() % 100000L);
            Iterator var26 = hitResults.iterator();

            while(var26.hasNext()) {
               HitResult hitResult = (HitResult)var26.next();
               this.hitScanTarget(player, itemStack, slotIndex, correlationId, hitResult, maxHitScanDistance, blockPosToDestroy);
            }
         }
      } catch (Exception var28) {
         LOGGER.error("Failed to handle client hit scan fire request: {}", var28);
      }

   }

   private double getMaxServerShootingDistance(ItemStack itemStack, boolean isAiming, ServerLevel level) {
      MinecraftServer server = level.m_7654_();
      double maxHitScanDistance = (double)Math.min(server.m_6846_().m_11312_() * 16, FireModeFeature.getMaxShootingDistance(itemStack));
      if (!isAiming) {
         maxHitScanDistance = Math.min(maxHitScanDistance, 100.0D);
      }

      return maxHitScanDistance;
   }

   private void hitScanTarget(Player player, ItemStack itemStack, int slotIndex, int correlationId, HitResult hitResult, double maxHitScanDistance, List<BlockPos> blockPosToDestroy) {
      float entityDamage = 0.0F;
      LOGGER.debug("Executing hit target task for hit result {}", hitResult);
      if (hitResult.m_6662_() == Type.ENTITY) {
         entityDamage = this.hurtEntity(player, (EntityHitResult)hitResult, (Entity)null, itemStack);
      } else if (hitResult.m_6662_() == Type.BLOCK) {
         this.handleBlockHit(player, (BlockHitResult)hitResult, (Entity)null);
      }

      Iterator var10 = blockPosToDestroy.iterator();

      while(var10.hasNext()) {
         BlockPos bp = (BlockPos)var10.next();
         MiscUtil.getLevel(player).m_46953_(bp, true, player);
      }

      double maxHitScanDistanceSqr = maxHitScanDistance * maxHitScanDistance;
      Iterator var12 = ((ServerLevel)MiscUtil.getLevel(player)).m_8795_((p) -> {
         return true;
      }).iterator();

      while(true) {
         ServerPlayer serverPlayer;
         do {
            if (!var12.hasNext()) {
               return;
            }

            serverPlayer = (ServerPlayer)var12.next();
         } while(serverPlayer != player && !(serverPlayer.m_20280_(player) < maxHitScanDistanceSqr));

         LOGGER.debug("{} sends hit scan notification to {}", player, serverPlayer);
         Network.networkChannel.send(PacketDistributor.PLAYER.with(() -> {
            return serverPlayer;
         }), new HitScanFireResponsePacket(player.m_19879_(), getItemStackId(itemStack), slotIndex, correlationId, SimpleHitResult.fromHitResult(hitResult), entityDamage));
      }
   }

   public void handleClientFireModeRequest(ServerPlayer player, UUID stateId, int slotIndex, int correlationId, FireModeInstance fireModeInstance) {
      ItemStack itemStack = player.m_150109_().m_8020_(slotIndex);
      if (itemStack != null && itemStack.m_41720_() instanceof GunItem) {
         boolean isSuccess = getFireModes(itemStack).contains(fireModeInstance);
         if (isSuccess) {
            setFireModeInstance(itemStack, fireModeInstance);
         }

         Network.networkChannel.send(PacketDistributor.PLAYER.with(() -> {
            return player;
         }), new FireModeResponsePacket(stateId, slotIndex, correlationId, isSuccess, fireModeInstance));
      } else {
         Network.networkChannel.send(PacketDistributor.PLAYER.with(() -> {
            return player;
         }), new FireModeResponsePacket(stateId, slotIndex, correlationId, false, fireModeInstance));
      }
   }

   public void processServerStateSyncResponse(UUID stateId, int correlationId, boolean isSuccess, ItemStack itemStack, GunClientState gunClientState) {
      if (isSuccess) {
      }

   }

   @OnlyIn(Dist.CLIENT)
   public void processServerHitScanFireResponse(Player player, UUID stateId, ItemStack itemStack, GunClientState gunClientState, SimpleHitResult hitResult, float damage) {
      if (gunClientState != null) {
         Player mainPlayer = ClientUtils.getClientPlayer();
         if (player == mainPlayer) {
            if (hitResult.m_6662_() != Type.MISS) {
               gunClientState.confirmHitScanTarget(mainPlayer, itemStack, hitResult, damage);
            }
         } else if (itemStack != null) {
            Item var9 = itemStack.m_41720_();
            if (var9 instanceof GunItem) {
               GunItem gunItem = (GunItem)var9;
               gunItem.effectLauncher.onStartFiring(mainPlayer, gunClientState, itemStack);
               gunItem.effectLauncher.onHitScanTargetAcquired(mainPlayer, gunClientState, itemStack, hitResult);
               if (hitResult.m_6662_() != Type.MISS) {
                  gunItem.effectLauncher.onHitScanTargetConfirmed(mainPlayer, gunClientState, itemStack, hitResult, damage);
               }
            }
         }
      }

   }

   @OnlyIn(Dist.CLIENT)
   public void processServerFireModeResponse(UUID stateId, int correlationId, boolean isSuccess, ItemStack itemStack, GunClientState gunClientState, FireModeInstance fireModeInstance) {
      LOGGER.debug("Process fire mode response: {}", isSuccess);
      if (isSuccess && getFireModes(itemStack).contains(fireModeInstance)) {
         setFireModeInstance(itemStack, fireModeInstance);
      }

   }

   @OnlyIn(Dist.CLIENT)
   public void processServerReloadResponse(int correlationId, boolean isSuccess, ItemStack itemStack, GunClientState gunClientState, int ammo, FireModeInstance fireModeInstance) {
      LOGGER.debug("Process server reload response with ammo count {}", ammo);
      gunClientState.reloadAmmo(ClientUtils.getLevel(), fireModeInstance, ammo);
   }

   @OnlyIn(Dist.CLIENT)
   public boolean tryReload(Player player, ItemStack itemStack) {
      boolean result = false;
      int activeSlot = player.m_150109_().f_35977_;
      boolean isMainHand = player.m_21205_() == itemStack;
      if (isMainHand) {
         GunClientState gunClientState = GunClientState.getState(player, itemStack, activeSlot, false);
         if (gunClientState != null) {
            result = gunClientState.tryReload(player, itemStack);
         }
      }

      return result;
   }

   @OnlyIn(Dist.CLIENT)
   public boolean tryFire(LocalPlayer player, ItemStack itemStack, Entity targetEntity) {
      boolean result = false;
      int activeSlot = player.m_150109_().f_35977_;
      boolean isMainHand = player.m_21205_() == itemStack;
      if (isMainHand) {
         GunClientState gunClientState = GunClientState.getState(player, itemStack, activeSlot, false);
         if (gunClientState != null) {
            gunClientState.setTrigger(true);
            result = gunClientState.tryFire(player, itemStack, targetEntity);
         }
      }

      return result;
   }

   @OnlyIn(Dist.CLIENT)
   public boolean requestReloadFromServer(Player player, ItemStack itemStack) {
      LOGGER.debug("{} Initiating client side reload", System.currentTimeMillis() % 100000L);
      boolean result = false;
      int activeSlot = player.m_150109_().f_35977_;
      boolean isMainHandItem = player.m_21205_() == itemStack;
      if (isMainHandItem) {
         FireModeInstance fireModeInstance = getFireModeInstance(itemStack);
         int ammoToReload = this.canReloadGun(itemStack, player, fireModeInstance);
         if (ammoToReload > 0) {
            Network.networkChannel.sendToServer(new ReloadRequestPacket(getItemStackId(itemStack), activeSlot, fireModeInstance));
            result = true;
         } else {
            LOGGER.debug("No ammo to reload");
         }
      }

      return result;
   }

   public double getAimingCurveX() {
      return this.aimingCurveX;
   }

   public double getAimingCurveY() {
      return this.aimingCurveY;
   }

   public double getAimingCurveZ() {
      return this.aimingCurveZ;
   }

   public double getAimingCurvePitch() {
      return this.aimingCurvePitch;
   }

   public double getAimingCurveYaw() {
      return this.aimingCurveYaw;
   }

   public double getAimingCurveRoll() {
      return this.aimingCurveRoll;
   }

   public double getAimingZoom() {
      return this.aimingZoom;
   }

   private static FireModeInstance getNextFireModeInstance(ItemStack itemStack, FireModeInstance currentMode) {
      List<FireModeInstance> allFireModes = getFireModes(itemStack);
      int currentIndex = allFireModes.indexOf(currentMode);
      int nextIndex = (currentIndex + 1) % allFireModes.size();
      return (FireModeInstance)allFireModes.get(nextIndex);
   }

   public static List<FireModeInstance> getFireModes(ItemStack itemStack) {
      List<FireModeInstance> allFireModes = new ArrayList();
      List<Features.EnabledFeature> enabledFireModeFeatures = Features.getEnabledFeatures(itemStack, FireModeFeature.class);
      Iterator var3 = enabledFireModeFeatures.iterator();

      while(var3.hasNext()) {
         Features.EnabledFeature efmf = (Features.EnabledFeature)var3.next();
         FireModeFeature fmf = (FireModeFeature)efmf.feature();
         allFireModes.addAll(fmf.getFireModes());
      }

      return allFireModes;
   }

   @OnlyIn(Dist.CLIENT)
   public void initiateClientSideFireMode(Player player, ItemStack itemStack) {
      int activeSlot = player.m_150109_().f_35977_;
      boolean isOffhand = player.m_21206_() == itemStack;
      if (!isOffhand) {
         GunClientState gunClientState = GunClientState.getState(player, itemStack, activeSlot, false);
         if (gunClientState != null && !gunClientState.isReloading() && !gunClientState.isFiring() && !gunClientState.isInspecting()) {
            FireModeInstance currentFireMode = getFireModeInstance(itemStack);
            FireModeInstance nextFireMode = getNextFireModeInstance(itemStack, currentFireMode);
            if (nextFireMode != currentFireMode) {
               LOGGER.debug("Requesting fire mode change from {} to {}", currentFireMode.getDisplayName(), nextFireMode.getDisplayName());
               Network.networkChannel.sendToServer(new FireModeRequestPacket(getItemStackId(itemStack), activeSlot, nextFireMode));
            }
         }
      }

   }

   public void handleClientStopFireRequest(ServerPlayer player, UUID stateId, int slotIndex, int correlationId) {
   }

   @OnlyIn(Dist.CLIENT)
   public void processStopServerFireResponse(UUID stateId, int correlationId, boolean isSuccess, ItemStack itemStack, GunClientState gunClientState) {
   }

   @OnlyIn(Dist.CLIENT)
   public void setTriggerOff(LocalPlayer player, ItemStack itemStack) {
      int activeSlot = player.m_150109_().f_35977_;
      boolean isOffhand = player.m_21206_() == itemStack;
      if (!isOffhand) {
         GunClientState gunClientState = GunClientState.getState(player, itemStack, activeSlot, false);
         if (gunClientState != null) {
            gunClientState.setTrigger(false);
         }
      }

   }

   public GunClientState createState(UUID stateId) {
      GunClientState state = new GunClientState(stateId, this);
      LOGGER.debug("Creating state {}", stateId);
      state.setAnimationController("playerRecoil", new PlayerRecoilController(this.viewRecoilAmplitude, (double)this.viewRecoilMaxPitch, (double)this.viewRecoilDuration));
      state.setAnimationController("shake", new ViewShakeAnimationController(this.shakeRecoilAmplitude, this.shakeRecoilSpeed, this.shakeDecay, this.shakeRecoilDuration) {
         public void onStartFiring(LivingEntity player, GunClientState state, ItemStack itemStack) {
            FireModeInstance.ViewShakeDescriptor viewShakeDescriptor = FireModeFeature.getViewShakeDescriptor(itemStack);
            this.reset(viewShakeDescriptor);
         }
      });
      state.setAnimationController("recoil2", new GunRecoilAnimationController(this.gunRecoilInitialAmplitude, this.gunRecoilRateOfAmplitudeDecay, this.gunRecoilInitialAngularFrequency, this.gunRecoilRateOfFrequencyIncrease, this.gunRecoilPitchMultiplier, this.gunRecoilDuration, this.shotsPerRecoil));
      state.setAnimationController("randomizer", new GunRandomizingAnimationController(this.gunRandomizationAmplitude, this.idleRandomizationDuration, this.recoilRandomizationDuration));
      state.setAnimationController("reloadTimer", this.createReloadTimerController());
      Iterator var3 = this.glowEffectBuilders.iterator();

      String controllerId;
      while(var3.hasNext()) {
         GlowAnimationController.Builder builder = (GlowAnimationController.Builder)var3.next();
         controllerId = "glowEffect" + builder.getEffectId();
         state.setAnimationController(controllerId, builder.build());
      }

      var3 = this.rotationEffectBuilders.iterator();

      while(var3.hasNext()) {
         RotationAnimationController.Builder builder = (RotationAnimationController.Builder)var3.next();
         controllerId = "rotation" + builder.getModelPartName();
         state.setAnimationController(controllerId, builder.build());
      }

      state.addListener(new DynamicGeoListener());
      state.addListener(this.effectLauncher);
      state.setAnimationController("aiming", new BiDirectionalInterpolator(400L) {
         public void onToggleAiming(boolean isAiming) {
            this.set(isAiming ? Position.END : Position.START, false);
         }
      });
      return state;
   }

   private List<GunStateAnimationController> createReloadAnimationControllers() {
      List<GunStateAnimationController> reloadAnimationControllers = new ArrayList();
      if (this.phasedReloads.isEmpty()) {
         GunStateAnimationController reloadAnimationController = new GunStateAnimationController(this, "reload_controller", "animation.model.reload", (ctx) -> {
            return ctx.gunClientState().isReloading();
         }) {
            public void onStartReloading(LivingEntity player, GunClientState state, ItemStack itemStack) {
               if (ClientUtil.isFirstPerson(player)) {
                  this.scheduleReset(player, state, itemStack);
               }

            }
         };
         reloadAnimationControllers.add(reloadAnimationController);
      } else {
         long maxReloadDuration = 0L;
         Iterator var4 = this.phasedReloads.iterator();

         while(var4.hasNext()) {
            PhasedReload phasedReload = (PhasedReload)var4.next();
            long conditionalReloadTimeMillis = phasedReload.timeUnit.toMillis(phasedReload.cooldownTime);
            if (conditionalReloadTimeMillis > maxReloadDuration) {
               maxReloadDuration = conditionalReloadTimeMillis;
            }
         }

         int counter = 0;
         Iterator var12 = this.phasedReloads.iterator();

         while(var12.hasNext()) {
            final PhasedReload phasedReload = (PhasedReload)var12.next();
            ReloadAnimation reloadAnimation = phasedReload.reloadAnimation;
            final Predicate<ConditionContext> combinedPredicate = (ctx) -> {
               return ctx.gunClientState().isReloading() && phasedReload.predicate.test(ctx);
            };
            GunStateAnimationController reloadAnimationController = new GunStateAnimationController(this, reloadAnimation.animationName + "_" + counter++, reloadAnimation.animationName, combinedPredicate) {
               public void onStartReloading(LivingEntity player, GunClientState state, ItemStack itemStack) {
                  if (ClientUtil.isFirstPerson(player) && phasedReload.phase == ReloadPhase.RELOADING && combinedPredicate.test(new ConditionContext((Player)player, itemStack, state, (ItemDisplayContext)null))) {
                     GunItem.LOGGER.debug("Reset {} on start reloading. Iter: {}", this.getName(), state.getReloadIterationIndex());
                     this.scheduleReset(player, state, itemStack);
                  }

               }

               public void onCompleteReloading(LivingEntity player, GunClientState state, ItemStack itemStack) {
                  if (ClientUtil.isFirstPerson(player) && phasedReload.phase == ReloadPhase.COMPLETETING && combinedPredicate.test(new ConditionContext((Player)player, itemStack, state, (ItemDisplayContext)null))) {
                     GunItem.LOGGER.debug("Reset {} on complete reloading. Iter: {}", this.getName(), state.getReloadIterationIndex());
                     this.scheduleReset(player, state, itemStack);
                  }

               }

               public void onPrepareReloading(LivingEntity player, GunClientState state, ItemStack itemStack) {
                  if (ClientUtil.isFirstPerson(player) && phasedReload.phase == ReloadPhase.PREPARING && combinedPredicate.test(new ConditionContext(player, itemStack, state, (ItemDisplayContext)null))) {
                     GunItem.LOGGER.debug("Reset {} on prepare reloading. Iter: {}", this.getName(), state.getReloadIterationIndex());
                     this.scheduleReset(player, state, itemStack);
                  }

               }
            };
            reloadAnimationController.setSoundKeyframeHandler((event) -> {
               Player player = ClientUtils.getClientPlayer();
               if (player != null) {
                  SoundKeyframeData soundKeyframeData = event.getKeyframeData();
                  String soundName = soundKeyframeData.getSound();
                  SoundEvent soundEvent = SoundRegistry.getSoundEvent(soundName);
                  if (soundEvent != null) {
                     player.m_5496_(soundEvent, 1.0F, 1.0F);
                  }
               }

            });
            reloadAnimationControllers.add(reloadAnimationController);
         }
      }

      return reloadAnimationControllers;
   }

   private TimerController createReloadTimerController() {
      TimerController reloadTimerController;
      if (this.phasedReloads.isEmpty()) {
         reloadTimerController = new TimerController(this.reloadCooldownTime) {
            public void onStartReloading(LivingEntity player, GunClientState state, ItemStack itemStack) {
               this.reset();
            }
         };
         Iterator var2 = this.reloadEffectControllers.iterator();

         while(var2.hasNext()) {
            Tuple<Long, AbstractProceduralAnimationController> t = (Tuple)var2.next();
            reloadTimerController.schedule(ReloadPhase.RELOADING, (Long)t.m_14418_(), TimeUnit.MILLISECOND, (AbstractProceduralAnimationController)t.m_14419_(), (Predicate)null);
         }
      } else {
         long maxReloadDuration = 0L;
         Iterator var4 = this.phasedReloads.iterator();

         PhasedReload phasedReload;
         while(var4.hasNext()) {
            phasedReload = (PhasedReload)var4.next();
            long conditionalReloadTimeMillis = phasedReload.timeUnit.toMillis(phasedReload.cooldownTime);
            if (conditionalReloadTimeMillis > maxReloadDuration) {
               maxReloadDuration = conditionalReloadTimeMillis;
            }
         }

         reloadTimerController = new TimerController(maxReloadDuration) {
            public void onStartReloading(LivingEntity player, GunClientState state, ItemStack itemStack) {
               if (ClientUtil.isFirstPerson(player)) {
                  this.reset();
               }

            }

            public void onCompleteReloading(LivingEntity player, GunClientState state, ItemStack itemStack) {
               if (ClientUtil.isFirstPerson(player)) {
                  this.reset();
               }

            }

            public void onPrepareReloading(LivingEntity player, GunClientState state, ItemStack itemStack) {
               if (ClientUtil.isFirstPerson(player)) {
                  this.reset();
               }

            }
         };
         var4 = this.phasedReloads.iterator();

         while(var4.hasNext()) {
            phasedReload = (PhasedReload)var4.next();
            ReloadAnimation reloadAnimation = phasedReload.reloadAnimation;
            Predicate<ConditionContext> combinedPredicate = (ctx) -> {
               return ctx.gunClientState().isReloading() && phasedReload.predicate.test(ctx);
            };
            Iterator var8 = reloadAnimation.shakeEffects.iterator();

            while(var8.hasNext()) {
               ReloadShakeEffect effect = (ReloadShakeEffect)var8.next();
               ViewShakeAnimationController2 controller = new ViewShakeAnimationController2(effect.initialAmplitude, effect.rateOfAmplitudeDecay, effect.initialAngularFrequency, effect.rateOfFrequencyIncrease, effect.duration);
               reloadTimerController.schedule(phasedReload.phase, phasedReload.timeUnit.toMillis(effect.startTime), TimeUnit.MILLISECOND, controller, combinedPredicate);
            }
         }
      }

      return reloadTimerController;
   }

   public Map<String, AnimationController<GeoAnimatable>> getGeoAnimationControllers(ItemStack itemStack) {
      long geoId = GeoItem.getId(itemStack);
      AnimatableManager<GeoAnimatable> animatableManager = this.cache.getManagerForId(geoId);
      return animatableManager.getAnimationControllers();
   }

   public AnimationController<GeoAnimatable> getGeoAnimationController(String controllerId, ItemStack itemStack) {
      Map<String, AnimationController<GeoAnimatable>> controllers = this.getGeoAnimationControllers(itemStack);
      return (AnimationController)controllers.get(controllerId);
   }

   public ResourceLocation getTargetLockOverlay() {
      return this.targetLockOverlay;
   }

   public long getTargetLockTimeTicks() {
      return this.targetLockTimeTicks;
   }

   @OnlyIn(Dist.CLIENT)
   public void onTargetStartLocking(Entity targetEntity) {
      LOGGER.debug("Locking target: {}", targetEntity);
      if (this.targetStartLockingSound != null) {
         Player player = ClientUtils.getClientPlayer();
         MiscUtil.getLevel(player).m_6263_(player, player.m_20185_(), player.m_20186_(), player.m_20189_(), this.targetStartLockingSound, SoundSource.PLAYERS, 1.0F, 1.0F);
      }

   }

   @OnlyIn(Dist.CLIENT)
   public void onTargetLocked(Entity targetEntity) {
      LOGGER.debug("Target locked: {}", targetEntity);
      Player player = ClientUtils.getClientPlayer();
      if (this.targetLockedSound != null) {
         MiscUtil.getLevel(player).m_6263_(player, player.m_20185_(), player.m_20186_(), player.m_20189_(), this.targetLockedSound, SoundSource.PLAYERS, 1.0F, 1.0F);
      }

      GunClientState state = GunClientState.getMainHeldState();
      if (state != null) {
         MutableComponent var10001 = Component.m_237115_("message.pointblank.targetAcquired").m_130946_(": ").m_7220_(targetEntity.m_7755_()).m_130946_(". ");
         MutableComponent var10002 = Component.m_237115_("message.pointblank.distance").m_130946_(": ");
         float var10003 = targetEntity.m_20270_(player);
         state.publishMessage(var10001.m_7220_(var10002.m_130946_(Math.round(var10003).makeConcatWithConstants<invokedynamic>(Math.round(var10003)))), 1000L, (s) -> {
            return true;
         });
      }

   }

   @OnlyIn(Dist.CLIENT)
   public void onTargetStartUnlocking(Entity targetEntity) {
      LOGGER.debug("Target unlocked: {}", targetEntity);
   }

   public float getPrice() {
      return this.tradePrice;
   }

   public int getBundleQuantity() {
      return this.tradeBundleQuantity;
   }

   public int getTradeLevel() {
      return this.tradeLevel;
   }

   public float getBobbing() {
      return this.bobbing;
   }

   public float getBobbingOnAim() {
      return this.bobbingOnAim;
   }

   public float getBobbingRollMultiplier() {
      return this.bobbingRollMultiplier;
   }

   public double getJumpMultiplier() {
      return this.jumpMultiplier;
   }

   public double getPipScopeZoom() {
      return this.pipScopeZoom;
   }

   public void handleAimingChangeRequest(Player player, ItemStack itemStack, UUID stateId, int slotIndex, boolean isAiming) {
      if (itemStack.m_41720_() instanceof GunItem) {
         CompoundTag idTag = itemStack.m_41783_();
         if (idTag != null) {
            idTag.m_128379_("aim", isAiming && this.isAimingEnabled);
         }

         if (player.m_20142_() && isAiming && this.isAimingEnabled) {
            player.m_6858_(false);
         }

      }
   }

   public static boolean isAiming(ItemStack itemStack) {
      Item var2 = itemStack.m_41720_();
      if (var2 instanceof GunItem) {
         GunItem gunItem = (GunItem)var2;
         CompoundTag idTag = itemStack.m_41783_();
         if (idTag == null) {
            return false;
         } else {
            return gunItem.isAimingEnabled && idTag.m_128471_("aim");
         }
      } else {
         return false;
      }
   }

   public int getMaxAttachmentCategories() {
      return 11;
   }

   public List<Attachment> getDefaultAttachments() {
      return this.defaultAttachmentSuppliers.stream().map(Supplier::get).toList();
   }

   public Collection<Attachment> getCompatibleAttachments() {
      if (this.compatibleAttachments == null) {
         Set<AttachmentCategory> attachmentCategories = new HashSet();
         Set<Attachment> compatibleAttachments = new LinkedHashSet();
         Iterator var3 = this.getDefaultAttachments().iterator();

         while(var3.hasNext()) {
            Attachment attachment = (Attachment)var3.next();
            if (attachmentCategories.size() >= this.getMaxAttachmentCategories()) {
               LOGGER.warn("Cannot add compatible attachment {} with category {} to  item {} because the existing number of compatible categories for this item cannot exceed {}", attachment.getName(), attachment.getCategory(), this.getName(), this.getMaxAttachmentCategories());
               break;
            }

            attachmentCategories.add(attachment.getCategory());
            compatibleAttachments.add(attachment);
         }

         var3 = this.compatibleAttachmentSuppliers.iterator();

         while(var3.hasNext()) {
            Supplier<Attachment> attachmentSupplier = (Supplier)var3.next();
            Attachment attachment = (Attachment)attachmentSupplier.get();
            if (attachmentCategories.size() >= this.getMaxAttachmentCategories()) {
               LOGGER.warn("Cannot add compatible attachment {} with category {} to  item {} because the existing number of compatible categories for this item cannot exceed {}", attachment.getName(), attachment.getCategory(), this.getName(), this.getMaxAttachmentCategories());
               break;
            }

            attachmentCategories.add(attachment.getCategory());
            compatibleAttachments.add(attachment);
         }

         var3 = this.compatibleAttachmentGroups.iterator();

         while(true) {
            while(var3.hasNext()) {
               String group = (String)var3.next();
               List<Supplier<? extends Item>> groupAtttachments = ItemRegistry.ITEMS.getAttachmentsForGroup(group);
               Iterator var6 = groupAtttachments.iterator();

               while(var6.hasNext()) {
                  Supplier<? extends Item> ga = (Supplier)var6.next();
                  Item item = (Item)ga.get();
                  if (item instanceof Attachment) {
                     Attachment attachment = (Attachment)item;
                     if (attachmentCategories.size() >= this.getMaxAttachmentCategories()) {
                        LOGGER.warn("Cannot add compatible attachment {} with category {} to  item {} because the existing number of compatible categories for this item cannot exceed {}", attachment.getName(), attachment.getCategory(), this.getName(), this.getMaxAttachmentCategories());
                        break;
                     }

                     compatibleAttachments.add(attachment);
                  }
               }
            }

            this.compatibleAttachments = compatibleAttachments;
            break;
         }
      }

      return this.compatibleAttachments;
   }

   public <T extends Feature> T getFeature(Class<T> featureClass) {
      return (Feature)featureClass.cast(this.features.get(featureClass));
   }

   public long getCraftingDuration() {
      return this.craftingDuration;
   }

   public SoundEvent getFireSound() {
      return this.fireSound;
   }

   public float getFireSoundVolume() {
      return this.fireSoundVolume;
   }

   public static ItemStack getMainHeldGunItemStack(LivingEntity player) {
      ItemStack itemStack = player.m_21205_();
      return itemStack != null && itemStack.m_41720_() instanceof GunItem ? itemStack : null;
   }

   public boolean hasIdleAnimations() {
      return this.idleAnimationProvider != null;
   }

   public AnimationType getAnimationType() {
      return this.animationType;
   }

   public String getThirdPersonFallbackAnimations() {
      return this.thirdPersonFallbackAnimations;
   }

   public static class Builder extends HurtingItem.Builder<Builder> implements Nameable {
      private static final float DEFAULT_PRICE = Float.NaN;
      private static final int DEFAULT_TRADE_LEVEL = 0;
      private static final int DEFAULT_TRADE_BUNDLE_QUANTITY = 1;
      private static final int DEFAULT_VIEW_RECOIL_DURATION = 100;
      private static final int DEFAULT_SHAKE_RECOIL_DURATION = 400;
      private static final int DEFAULT_GUN_RECOIL_DURATION = 500;
      private static final int DEFAULT_IDLE_RANDOMIZATION_DURATION = 2500;
      private static final int DEFAULT_RECOIL_RANDOMIZATION_DURATION = 250;
      private static final int DEFAULT_DRAW_COOLDOWN_DURATION = 500;
      private static final int DEFAULT_PREPARE_IDLE_COOLDOWN_DURATION = 0;
      private static final int DEFAULT_INSPECT_COOLDOWN_DURATION = 1000;
      private static final int DEFAULT_CRAFTING_DURATION = 1000;
      private static final float DEFAULT_HIT_SCAN_SPEED = 800.0F;
      private static final float DEFAULT_HIT_SCAN_ACCELERATION = 0.0F;
      private static final double DEFAULT_VIEW_RECOIL_AMPLITUDE = 1.0D;
      private static final double DEFAULT_SHAKE_RECOIL_AMPLITUDE = 0.5D;
      private static final int DEFAULT_VIEW_RECOIL_MAX_PITCH = 20;
      private static final double DEFAULT_SHAKE_RECOIL_SPEED = 8.0D;
      private static final double DEFAULT_SHAKE_DECAY = 0.98D;
      private static final double DEFAULT_GUN_RECOIL_INITIAL_AMPLITUDE = 0.3D;
      private static final double DEFAULT_GUN_RECOIL_RATE_OF_AMPLITUDE_DECAY = 0.8D;
      private static final double DEFAULT_GUN_RECOIL_INITIAL_ANGULAR_FREQUENCY = 1.0D;
      private static final double DEFAULT_GUN_RECOIL_RATE_OF_FREQUENCY_INCREASE = 0.05D;
      private static final double DEFAULT_GUN_RANDOMIZATION_AMPLITUDE = 0.01D;
      private static final double DEFAULT_GUN_RECOIL_PITCH_MULTIPLIER = 1.0D;
      private static final double DEFAULT_JUMP_MULTIPLIER = 1.0D;
      private static final double DEFAULT_RELOAD_SHAKE_INITIAL_AMPLITUDE = 0.15D;
      private static final double DEFAULT_RELOAD_SHAKE_RATE_OF_AMPLITUDE_DECAY = 0.3D;
      private static final double DEFAULT_RELOAD_SHAKE_INITIAL_ANGULAR_FREQUENCY = 1.0D;
      private static final double DEFAULT_RELOAD_SHAKE_RATE_OF_FREQUENCY_INCREASE = 0.01D;
      private static final int DEFAULT_BURST_SHOTS = 3;
      private static final int DEFAULT_RELOAD_COOLDOWN_TIME = 1000;
      public static final double DEFAULT_AIMING_CURVE_X = 0.0D;
      public static final double DEFAULT_AIMING_CURVE_Y = -0.07D;
      public static final double DEFAULT_AIMING_CURVE_Z = 0.3D;
      public static final double DEFAULT_AIMING_CURVE_PITCH = -0.01D;
      public static final double DEFAULT_AIMING_CURVE_YAW = -0.01D;
      public static final double DEFAULT_AIMING_CURVE_ROLL = -0.01D;
      public static final double DEFAULT_AIMING_ZOOM = 0.05D;
      public static final double DEFAULT_PELLET_SPREAD = 1.0D;
      private static final double DEFAULT_INACCURACY = 0.03D;
      private static final double DEFAULT_INACCURACY_AIMING = 0.0D;
      private static final double DEFAULT_INACCURACY_SPRINTING = 0.1D;
      private static final int DEFAULT_RPM = 600;
      private static final float DEFAULT_FIRE_SOUND_VOLUME = 5.0F;
      public static final int DEFAULT_MAX_AMMO_PER_RELOAD_ITERATION = Integer.MAX_VALUE;
      private static final String DEFAULT_RETICLE_OVERLAY = "textures/item/reticle.png";
      public static final int MAX_PELLET_SHOOTING_RANGE = 50;
      private static final float DEFAULT_BOBBING = 1.0F;
      private static final float DEFAULT_BOBBING_ON_AIM = 0.3F;
      private static final float DEFAULT_BOBBING_ROLL_MULTIPLIER = 1.0F;
      private long targetLockTimeTicks;
      private double viewRecoilAmplitude = 1.0D;
      private double shakeRecoilAmplitude = 0.5D;
      private int viewRecoilMaxPitch = 20;
      private long viewRecoilDuration = 100L;
      private double shakeRecoilSpeed = 8.0D;
      private double shakeDecay = 0.98D;
      private long shakeRecoilDuration = 400L;
      private double gunRecoilInitialAmplitude = 0.3D;
      private double gunRecoilRateOfAmplitudeDecay = 0.8D;
      private double gunRecoilInitialAngularFrequency = 1.0D;
      private double gunRecoilRateOfFrequencyIncrease = 0.05D;
      private double gunRandomizationAmplitude = 0.01D;
      private double gunRecoilPitchMultiplier = 1.0D;
      private long gunRecoilDuration = 500L;
      private double jumpMultiplier = 1.0D;
      private int shotsPerRecoil = 1;
      private int shotsPerTrace = 1;
      private long idleRandomizationDuration = 2500L;
      private long recoilRandomizationDuration = 250L;
      private int burstShots = 3;
      private long prepareIdleCooldownDuration = 0L;
      private long prepareFireCooldownDuration = 0L;
      private long completeFireCooldownDuration = 0L;
      private long enableFireModeCooldownDuration = 0L;
      private long craftingDuration = 1000L;
      private String name;
      private float tradePrice = Float.NaN;
      private int tradeBundleQuantity = 1;
      private int tradeLevel = 0;
      private int rpm = 600;
      private long reloadCooldownTime;
      private String reloadAnimation;
      private int maxAmmoCapacity;
      private int maxAmmoPerReloadIteration = Integer.MAX_VALUE;
      private FireMode[] fireModes;
      private Set<Supplier<AmmoItem>> compatibleAmmo = new LinkedHashSet();
      private Supplier<SoundEvent> fireSound;
      private float fireSoundVolume = 5.0F;
      private Supplier<SoundEvent> targetLockedSound;
      private Supplier<SoundEvent> targetStartLockingSound;
      private boolean isAimingEnabled = true;
      private double aimingCurveX = 0.0D;
      private double aimingCurveY = -0.07D;
      private double aimingCurveZ = 0.3D;
      private double aimingCurvePitch = -0.01D;
      private double aimingCurveYaw = -0.01D;
      private double aimingCurveRoll = -0.01D;
      private double aimingZoom = 0.05D;
      private double pipScopeZoom = 0.0D;
      private List<Tuple<Long, AbstractProceduralAnimationController>> reloadEffectControllers;
      private String scopeOverlay;
      private String reticleOverlay;
      private String targetLockOverlay;
      private List<PhasedReload> phasedReloads = new ArrayList();
      private ConditionalAnimationProvider.Builder drawAnimationsBuilder = new ConditionalAnimationProvider.Builder();
      private ConditionalAnimationProvider.Builder idleAnimationBuilder = new ConditionalAnimationProvider.Builder();
      private ConditionalAnimationProvider.Builder inspectAnimationsBuilder = new ConditionalAnimationProvider.Builder();
      private ConditionalAnimationProvider.Builder fireAnimationsBuilder = new ConditionalAnimationProvider.Builder();
      private int pelletCount = 0;
      private double pelletSpread = 1.0D;
      private double inaccuracy = 0.03D;
      private double inaccuracyAiming = 0.0D;
      private double inaccuracySprinting = 0.1D;
      private List<GlowAnimationController.Builder> glowEffectBuilders = new ArrayList();
      private List<RotationAnimationController.Builder> rotationEffectBuilders = new ArrayList();
      private Map<FirePhase, List<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>>> effectBuilders = new HashMap();
      private float hitScanSpeed = 800.0F;
      private float hitScanAcceleration = 0.0F;
      private float bobbing = 1.0F;
      private float bobbingOnAim = 0.3F;
      private float bobbingRollMultiplier = 1.0F;
      private float modelScale = 1.0F;
      private AnimationType animationType;
      private String firstPersonFallbackAnimations;
      private String thirdPersonFallbackAnimations;
      private List<Supplier<Attachment>> compatibleAttachments;
      private List<String> compatibleAttachmentGroups;
      private List<FeatureBuilder<?, ?>> featureBuilders;
      private List<Supplier<Attachment>> defaultAttachments;

      public Builder() {
         this.animationType = AnimationType.RIFLE;
         this.compatibleAttachments = new ArrayList();
         this.compatibleAttachmentGroups = new ArrayList();
         this.featureBuilders = new ArrayList();
         this.defaultAttachments = new ArrayList();
         this.reloadEffectControllers = new ArrayList();
      }

      public String getName() {
         return this.name;
      }

      public Builder withName(String name) {
         this.name = name;
         return this;
      }

      public Builder withAnimationType(AnimationType animationType) {
         this.animationType = animationType;
         return this;
      }

      public Builder withFirstPersonFallbackAnimations(String firstPersonFallbackAnimations) {
         this.firstPersonFallbackAnimations = firstPersonFallbackAnimations;
         return this;
      }

      public Builder withThirdPersonFallbackAnimations(String thirdPersonFallbackAnimations) {
         this.thirdPersonFallbackAnimations = thirdPersonFallbackAnimations;
         return this;
      }

      @SafeVarargs
      public final Builder withDefaultAttachment(Supplier<? extends Attachment>... attachmentSuppliers) {
         Supplier[] var2 = attachmentSuppliers;
         int var3 = attachmentSuppliers.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            Supplier<? extends Attachment> s = var2[var4];
            List var10000 = this.defaultAttachments;
            Objects.requireNonNull(s);
            var10000.add(s::get);
         }

         return this;
      }

      @SafeVarargs
      public final Builder withCompatibleAttachment(Supplier<? extends Attachment>... attachmentSuppliers) {
         Supplier[] var2 = attachmentSuppliers;
         int var3 = attachmentSuppliers.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            Supplier<? extends Attachment> s = var2[var4];
            List var10000 = this.compatibleAttachments;
            Objects.requireNonNull(s);
            var10000.add(s::get);
         }

         return this;
      }

      public Builder withCompatibleAttachmentGroup(String... groups) {
         this.compatibleAttachmentGroups.addAll(Set.of(groups));
         return this;
      }

      public Builder withFeature(FeatureBuilder<?, ?> featureBuilder) {
         this.featureBuilders.add(featureBuilder);
         return this;
      }

      public Builder withModelScale(double modelScale) {
         this.modelScale = Mth.m_14036_((float)modelScale, 0.1F, 1.0F);
         return this;
      }

      public Builder withTradePrice(float price, int tradeBundleQuantity, int tradeLevel) {
         this.tradePrice = price;
         this.tradeLevel = tradeLevel;
         this.tradeBundleQuantity = tradeBundleQuantity;
         return this;
      }

      public Builder withTradePrice(float price, int tradeLevel) {
         return this.withTradePrice(price, 1, tradeLevel);
      }

      public Builder withMaxAmmoCapacity(int maxAmmoCapacity) {
         this.maxAmmoCapacity = maxAmmoCapacity;
         return this;
      }

      public Builder withMaxAmmoPerReloadIteration(int maxAmmoPerReloadIteration) {
         this.maxAmmoPerReloadIteration = maxAmmoPerReloadIteration;
         return this;
      }

      public Builder withRpm(int rpm) {
         this.rpm = rpm;
         return this;
      }

      public Builder withReloadAnimation(String reloadAnimation) {
         this.reloadAnimation = reloadAnimation;
         return this;
      }

      public Builder withReloadCooldownDuration(long reloadCooldownTime, TimeUnit timeUnit) {
         this.reloadCooldownTime = timeUnit.toMillis(reloadCooldownTime);
         return this;
      }

      public Builder withFireModes(FireMode... fireModes) {
         this.fireModes = fireModes;
         return this;
      }

      public Builder withCraftingDuration(int duration, TimeUnit timeUnit) {
         this.craftingDuration = timeUnit.toMillis((long)duration);
         return this;
      }

      @SafeVarargs
      public final Builder withCompatibleAmmo(Supplier<AmmoItem>... ammo) {
         Supplier[] var2 = ammo;
         int var3 = ammo.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            Supplier<AmmoItem> b = var2[var4];
            this.compatibleAmmo.add(b);
         }

         return this;
      }

      public final Builder withCompatibleAmmo(List<Supplier<AmmoItem>> ammo) {
         this.compatibleAmmo.addAll(ammo);
         return this;
      }

      public final Builder withTargetLock(int minTargetLockTime, TimeUnit timeUnit) {
         this.targetLockTimeTicks = timeUnit.toTicks((long)minTargetLockTime);
         return this;
      }

      public Builder withViewRecoilAmplitude(double amplitude) {
         this.viewRecoilAmplitude = amplitude;
         return this;
      }

      public Builder withShakeRecoilAmplitude(double amplitude) {
         this.shakeRecoilAmplitude = amplitude;
         return this;
      }

      public Builder withViewRecoilMaxPitch(int pitch) {
         this.viewRecoilMaxPitch = pitch;
         return this;
      }

      public Builder withViewRecoilDuration(int duration, TimeUnit timeUnit) {
         this.viewRecoilDuration = timeUnit.toMillis((long)duration);
         return this;
      }

      public Builder withShakeRecoilSpeed(double speed) {
         this.shakeRecoilSpeed = speed;
         return this;
      }

      public Builder withShakeDecay(double shakeDecay) {
         this.shakeDecay = shakeDecay;
         return this;
      }

      public Builder withShakeRecoilDuration(int duration, TimeUnit timeUnit) {
         this.shakeRecoilDuration = timeUnit.toMillis((long)duration);
         return this;
      }

      public Builder withGunRecoilInitialAmplitude(double amplitude) {
         this.gunRecoilInitialAmplitude = amplitude;
         return this;
      }

      public Builder withGunRecoilRateOfAmplitudeDecay(double decayRate) {
         this.gunRecoilRateOfAmplitudeDecay = decayRate;
         return this;
      }

      public Builder withGunRecoilInitialAngularFrequency(double frequency) {
         this.gunRecoilInitialAngularFrequency = frequency;
         return this;
      }

      public Builder withGunRecoilRateOfFrequencyIncrease(double rate) {
         this.gunRecoilRateOfFrequencyIncrease = rate;
         return this;
      }

      public Builder withGunRecoilPitchMultiplier(double gunRecoilPitchMultiplier) {
         this.gunRecoilPitchMultiplier = gunRecoilPitchMultiplier;
         return this;
      }

      public Builder withGunRecoilDuration(int duration, TimeUnit timeUnit) {
         this.gunRecoilDuration = timeUnit.toMillis((long)duration);
         return this;
      }

      public Builder withJumpMultiplier(double jumpMultiplier) {
         this.jumpMultiplier = jumpMultiplier;
         return this;
      }

      public Builder withShotsPerRecoil(int shotsPerRecoil) {
         this.shotsPerRecoil = shotsPerRecoil;
         return this;
      }

      public Builder withShotsPerTrace(int shotsPerTrace) {
         this.shotsPerTrace = shotsPerTrace;
         return this;
      }

      public Builder withIdleRandomizationDuration(int duration, TimeUnit timeUnit) {
         this.idleRandomizationDuration = timeUnit.toMillis((long)duration);
         return this;
      }

      public Builder withRecoilRandomizationDuration(int duration, TimeUnit timeUnit) {
         this.recoilRandomizationDuration = timeUnit.toMillis((long)duration);
         return this;
      }

      public Builder withFireSound(Supplier<SoundEvent> fireSound) {
         this.fireSound = fireSound;
         return this;
      }

      public Builder withFireSound(Supplier<SoundEvent> fireSound, float fireSoundVolume) {
         this.fireSound = fireSound;
         this.fireSoundVolume = fireSoundVolume;
         return this;
      }

      public Builder withFireSound(SoundEvent fireSound) {
         this.fireSound = () -> {
            return fireSound;
         };
         return this;
      }

      public Builder withFireSound(SoundEvent fireSound, float fireSoundVolume) {
         this.fireSound = () -> {
            return fireSound;
         };
         this.fireSoundVolume = fireSoundVolume;
         return this;
      }

      public Builder withReloadSound(Supplier<SoundEvent> reloadSound) {
         return this;
      }

      public Builder withReloadSound(SoundEvent reloadSound) {
         return this;
      }

      public Builder withReloadShakeEffect(long startTime, long duration, TimeUnit timeUnit, double initialAmplitude, double rateOfAmplitudeDecay, double initialAngularFrequency, double rateOfFrequencyIncrease) {
         this.reloadEffectControllers.add(new Tuple(timeUnit.toMillis(startTime), new ViewShakeAnimationController2(initialAmplitude, rateOfAmplitudeDecay, initialAngularFrequency, rateOfFrequencyIncrease, duration)));
         return this;
      }

      public Builder withAimingEnabled(boolean aimingEnabled) {
         this.isAimingEnabled = aimingEnabled;
         return this;
      }

      public Builder withAimingCurveX(double aimingCurveX) {
         this.aimingCurveX = aimingCurveX;
         return this;
      }

      public Builder withAimingCurveY(double aimingCurveY) {
         this.aimingCurveY = aimingCurveY;
         return this;
      }

      public Builder withAimingCurveZ(double aimingCurveZ) {
         this.aimingCurveZ = aimingCurveZ;
         return this;
      }

      public Builder withAimingCurvePitch(double aimingCurvePitch) {
         this.aimingCurvePitch = aimingCurvePitch;
         return this;
      }

      public Builder withAimingCurveYaw(double aimingCurveYaw) {
         this.aimingCurveYaw = aimingCurveYaw;
         return this;
      }

      public Builder withAimingCurveRoll(double aimingCurveRoll) {
         this.aimingCurveRoll = aimingCurveRoll;
         return this;
      }

      public Builder withAimingZoom(double aimingZoom) {
         this.aimingZoom = aimingZoom;
         return this;
      }

      public Builder withPipScopeZoom(double pipScopeZoom) {
         this.pipScopeZoom = pipScopeZoom;
         return this;
      }

      public Builder withScopeOverlay(String scopeOverlay) {
         this.scopeOverlay = scopeOverlay;
         return this;
      }

      public Builder withReticleOverlay(String reticleOverlay) {
         this.reticleOverlay = reticleOverlay;
         return this;
      }

      public Builder withTargetLockOverlay(String targetLockOverlay) {
         this.targetLockOverlay = targetLockOverlay;
         return this;
      }

      public Builder withTargetLockedSound(Supplier<SoundEvent> targetLockedSound) {
         this.targetLockedSound = targetLockedSound;
         return this;
      }

      public Builder withTargetLockedSound(SoundEvent targetLockedSound) {
         this.targetLockedSound = () -> {
            return targetLockedSound;
         };
         return this;
      }

      public Builder withTargetStartLockingSound(Supplier<SoundEvent> targetStartLockingSound) {
         this.targetStartLockingSound = targetStartLockingSound;
         return this;
      }

      public Builder withTargetStartLockingSound(SoundEvent targetStartLockingSound) {
         this.targetStartLockingSound = () -> {
            return targetStartLockingSound;
         };
         return this;
      }

      public Builder withReticleOverlay() {
         this.reticleOverlay = "textures/item/reticle.png";
         return this;
      }

      public Builder withPrepareFireCooldownDuration(int duration, TimeUnit timeUnit) {
         this.prepareFireCooldownDuration = timeUnit.toMillis((long)duration);
         return this;
      }

      public Builder withCompleteFireCooldownDuration(int duration, TimeUnit timeUnit) {
         this.completeFireCooldownDuration = timeUnit.toMillis((long)duration);
         return this;
      }

      public Builder withEnableFireModeCooldownDuration(int duration, TimeUnit timeUnit) {
         this.enableFireModeCooldownDuration = timeUnit.toMillis((long)duration);
         return this;
      }

      public Builder withPrepareIdleCooldownDuration(int duration, TimeUnit timeUnit) {
         this.prepareIdleCooldownDuration = timeUnit.toMillis((long)duration);
         return this;
      }

      public Builder withDrawCooldownDuration(int duration, TimeUnit timeUnit) {
         this.withDrawAnimation("animation.model.draw", (ctx) -> {
            return true;
         }, duration, timeUnit);
         return this;
      }

      public Builder withDrawAnimation(String animationName, Predicate<ConditionContext> predicate, int duration, TimeUnit timeUnit) {
         this.drawAnimationsBuilder.withAnimation(animationName, predicate, (long)duration, timeUnit);
         return this;
      }

      public Builder withInspectCooldownDuration(int duration, TimeUnit timeUnit) {
         this.withInspectAnimation("animation.model.inspect", (ctx) -> {
            return true;
         }, duration, timeUnit);
         return this;
      }

      public Builder withInspectAnimation(String animationName, Predicate<ConditionContext> predicate, int duration, TimeUnit timeUnit) {
         this.inspectAnimationsBuilder.withAnimation(animationName, predicate, (long)duration, timeUnit);
         return this;
      }

      public Builder withIdleAnimation(String animationName, Predicate<ConditionContext> predicate, int duration, TimeUnit timeUnit) {
         this.idleAnimationBuilder.withAnimation(animationName, predicate, (long)duration, timeUnit);
         return this;
      }

      public Builder withFireAnimation(String animationName, Predicate<ConditionContext> predicate, int duration, TimeUnit timeUnit) {
         this.fireAnimationsBuilder.withAnimation(animationName, predicate, (long)duration, timeUnit);
         return this;
      }

      public Builder withPhasedReload(PhasedReload phasedReload) {
         this.phasedReloads.add(phasedReload);
         return this;
      }

      public Builder withPhasedReload(ReloadPhase phase, long cooldownTime, String animationName) {
         this.phasedReloads.add(new PhasedReload(phase, cooldownTime, animationName));
         return this;
      }

      public Builder withPhasedReload(ReloadPhase phase, long cooldownTime, ReloadAnimation reloadAnimation) {
         this.phasedReloads.add(new PhasedReload(phase, cooldownTime, reloadAnimation));
         return this;
      }

      public Builder withPhasedReload(ReloadPhase phase, TriPredicate<LivingEntity, GunClientState, ItemStack> predicate, long cooldownTime, ReloadAnimation reloadAnimation) {
         this.phasedReloads.add(new PhasedReload(phase, predicate, cooldownTime, reloadAnimation));
         return this;
      }

      public Builder withPhasedReload(ReloadPhase phase, Predicate<ConditionContext> predicate, long cooldownTime, ReloadAnimation reloadAnimation) {
         this.phasedReloads.add(new PhasedReload(phase, predicate, cooldownTime, TimeUnit.MILLISECOND, reloadAnimation));
         return this;
      }

      public Builder withPelletCount(int pelletCount) {
         this.pelletCount = pelletCount;
         return this;
      }

      public Builder withPelletSpread(double pelletSpread) {
         this.pelletSpread = pelletSpread;
         return this;
      }

      public Builder withGunRandomizationAmplitude(double gunRandomizationAmplitude) {
         this.gunRandomizationAmplitude = gunRandomizationAmplitude;
         return this;
      }

      public Builder withBurstShots(int burstShots) {
         this.burstShots = burstShots;
         return this;
      }

      public Builder withInaccuracy(double inaccuracy) {
         this.inaccuracy = inaccuracy;
         return this;
      }

      public Builder withInaccuracyAiming(double inaccuracyAiming) {
         this.inaccuracyAiming = inaccuracyAiming;
         return this;
      }

      public Builder withInaccuracySprinting(double inaccuracySprinting) {
         this.inaccuracySprinting = inaccuracySprinting;
         return this;
      }

      public Builder withGlow(String glowingPartName) {
         return this.withGlow((String)glowingPartName, (String)null);
      }

      public Builder withGlow(String glowingPartName, String textureName) {
         return this.withGlow(Collections.singleton(FirePhase.ANY), Collections.singleton(glowingPartName), textureName);
      }

      public Builder withGlow(Collection<FirePhase> firePhases, String glowingPartName) {
         return this.withGlow(firePhases, Collections.singleton(glowingPartName), (String)null);
      }

      public Builder withGlow(Collection<FirePhase> firePhases, Collection<String> glowingPartNames, String texture) {
         GlowAnimationController.Builder builder = (new GlowAnimationController.Builder()).withFirePhases(firePhases);
         if (texture != null) {
            builder.withTexture(new ResourceLocation("pointblank", texture));
         }

         builder.withGlowingPartNames(glowingPartNames);
         this.glowEffectBuilders.add(builder);
         return this;
      }

      public Builder withGlow(Collection<FirePhase> firePhases, String glowingPartName, String texture, AbstractEffect.SpriteAnimationType spriteAnimationType, int spriteRows, int spriteColumns, int spritesPerSecond, Direction... directions) {
         GlowAnimationController.Builder builder = (new GlowAnimationController.Builder()).withFirePhases(firePhases);
         if (texture != null) {
            builder.withTexture(new ResourceLocation("pointblank", texture));
         }

         builder.withGlowingPartNames(Collections.singleton(glowingPartName));
         builder.withSprites(spriteRows, spriteColumns, spritesPerSecond, spriteAnimationType);
         builder.withDirections(directions);
         this.glowEffectBuilders.add(builder);
         return this;
      }

      public Builder withRotation(String phase, String modelPart, double rpm, double acceleration, double deceleration) {
         RotationAnimationController.Builder builder = (new RotationAnimationController.Builder()).withPhase(phase).withModelPart(modelPart).withAccelerationRate(acceleration).withDecelerationRate(deceleration).withRotationsPerMinute(rpm);
         this.rotationEffectBuilders.add(builder);
         return this;
      }

      public Builder withRotation(RotationAnimationController.PhaseMapper phaseMapper, String modelPart, double rpm, double acceleration, double deceleration) {
         RotationAnimationController.Builder builder = (new RotationAnimationController.Builder()).withPhaseMapper(phaseMapper).withModelPart(modelPart).withAccelerationRate(acceleration).withDecelerationRate(deceleration).withRotationsPerMinute(rpm);
         this.rotationEffectBuilders.add(builder);
         return this;
      }

      public Builder withEffect(FirePhase firePhase, Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> effectBuilder) {
         List<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>> builders = (List)this.effectBuilders.computeIfAbsent(firePhase, (k) -> {
            return new ArrayList();
         });
         builders.add(effectBuilder);
         return this;
      }

      public Builder withHitScanSpeed(float hitScanSpeed) {
         this.hitScanSpeed = hitScanSpeed;
         return this;
      }

      public Builder withHitScanAcceleration(float hitScanAcceleration) {
         this.hitScanAcceleration = hitScanAcceleration;
         return this;
      }

      public Builder withBobbing(double bobbing) {
         this.bobbing = Mth.m_14036_((float)bobbing, 0.0F, 2.0F);
         return this;
      }

      public Builder withBobbingOnAim(double bobbingOnAim) {
         this.bobbingOnAim = Mth.m_14036_((float)bobbingOnAim, 0.0F, 2.0F);
         return this;
      }

      public Builder withBobbingRollMultiplier(double bobbingRollMultiplier) {
         this.bobbingRollMultiplier = Mth.m_14036_((float)bobbingRollMultiplier, 0.0F, 10.0F);
         return this;
      }

      public GunItem build() {
         return this.build("pointblank");
      }

      public GunItem build(String namespace) {
         return new GunItem(this, namespace);
      }

      public Builder withJsonObject(JsonObject obj) {
         super.withJsonObject(obj);
         Builder builder = this;
         this.withName(obj.getAsJsonPrimitive("name").getAsString());
         this.withAnimationType((AnimationType)JsonUtil.getEnum(obj, "animationType", AnimationType.class, AnimationType.RIFLE, true));
         this.withFirstPersonFallbackAnimations(JsonUtil.getJsonString(obj, "firstPersonFallbackAnimations", (String)null));
         this.withThirdPersonFallbackAnimations(JsonUtil.getJsonString(obj, "thirdPersonFallbackAnimations", (String)null));
         this.withModelScale((double)JsonUtil.getJsonFloat(obj, "modelScale", 1.0F));
         this.withTradePrice(JsonUtil.getJsonFloat(obj, "tradePrice", Float.NaN), JsonUtil.getJsonInt(obj, "traceBundleQuantity", 1), JsonUtil.getJsonInt(obj, "tradeLevel", 0));
         JsonPrimitive jsonMaxAmmoCapacity = obj.getAsJsonPrimitive("maxAmmoCapacity");
         if (jsonMaxAmmoCapacity.isString() && "infinite".equalsIgnoreCase(jsonMaxAmmoCapacity.getAsString())) {
            this.withMaxAmmoCapacity(Integer.MAX_VALUE);
         } else {
            this.withMaxAmmoCapacity(obj.getAsJsonPrimitive("maxAmmoCapacity").getAsInt());
         }

         this.withCraftingDuration(JsonUtil.getJsonInt(obj, "craftingDuration", 1000), TimeUnit.MILLISECOND);
         this.withMaxAmmoPerReloadIteration(JsonUtil.getJsonInt(obj, "maxAmmoPerReloadIteration", Integer.MAX_VALUE));
         this.withAimingEnabled(JsonUtil.getJsonBoolean(obj, "aimingEnabled", true));
         this.withTargetLock(JsonUtil.getJsonInt(obj, "minTargetLockTime", 0), TimeUnit.MILLISECOND);
         this.withRpm(JsonUtil.getJsonInt(obj, "rpm", 600));
         this.withPrepareIdleCooldownDuration(JsonUtil.getJsonInt(obj, "prepareIdleCooldownDuration", 0), TimeUnit.MILLISECOND);
         this.withViewRecoilMaxPitch(JsonUtil.getJsonInt(obj, "viewRecoilMaxPitch", 20));
         this.withViewRecoilDuration(JsonUtil.getJsonInt(obj, "viewRecoilDuration", 100), TimeUnit.MILLISECOND);
         this.withShakeRecoilDuration(JsonUtil.getJsonInt(obj, "shakeRecoilDuration", 400), TimeUnit.MILLISECOND);
         this.withGunRecoilDuration(JsonUtil.getJsonInt(obj, "gunRecoilDuration", 500), TimeUnit.MILLISECOND);
         this.withIdleRandomizationDuration(JsonUtil.getJsonInt(obj, "idleRandomizationDuration", 2500), TimeUnit.MILLISECOND);
         this.withRecoilRandomizationDuration(JsonUtil.getJsonInt(obj, "recoilRandomizationDuration", 250), TimeUnit.MILLISECOND);
         this.withBurstShots(JsonUtil.getJsonInt(obj, "burstShots", 3));
         this.withReloadCooldownDuration((long)JsonUtil.getJsonInt(obj, "reloadCooldownTime", 1000), TimeUnit.MILLISECOND);
         this.withPelletCount(JsonUtil.getJsonInt(obj, "pelletCount", 0));
         this.withPrepareFireCooldownDuration(JsonUtil.getJsonInt(obj, "prepareFireCooldownDuration", 0), TimeUnit.MILLISECOND);
         this.withCompleteFireCooldownDuration(JsonUtil.getJsonInt(obj, "completeFireCooldownDuration", 0), TimeUnit.MILLISECOND);
         this.withEnableFireModeCooldownDuration(JsonUtil.getJsonInt(obj, "enableFireModeCooldownDuration", 0), TimeUnit.MILLISECOND);
         this.withViewRecoilAmplitude(JsonUtil.getJsonDouble(obj, "viewRecoilAmplitude", 1.0D));
         this.withShakeRecoilAmplitude(JsonUtil.getJsonDouble(obj, "shakeRecoilAmplitude", 0.5D));
         this.withShakeRecoilSpeed(JsonUtil.getJsonDouble(obj, "shakeRecoilSpeed", 8.0D));
         this.withShakeDecay(JsonUtil.getJsonDouble(obj, "shakeDecay", 0.98D));
         this.withGunRecoilInitialAmplitude(JsonUtil.getJsonDouble(obj, "gunRecoilInitialAmplitude", 0.3D));
         this.withGunRecoilRateOfAmplitudeDecay(JsonUtil.getJsonDouble(obj, "gunRecoilRateOfAmplitudeDecay", 0.8D));
         this.withGunRecoilInitialAngularFrequency(JsonUtil.getJsonDouble(obj, "gunRecoilInitialAngularFrequency", 1.0D));
         this.withGunRecoilRateOfFrequencyIncrease(JsonUtil.getJsonDouble(obj, "gunRecoilRateOfFrequencyIncrease", 0.05D));
         this.withGunRecoilPitchMultiplier(JsonUtil.getJsonDouble(obj, "gunRecoilPitchMultiplier", 1.0D));
         this.withGunRandomizationAmplitude(JsonUtil.getJsonDouble(obj, "gunRandomizationAmplitude", 0.01D));
         this.withAimingCurveX(JsonUtil.getJsonDouble(obj, "aimingCurveX", 0.0D));
         this.withAimingCurveY(JsonUtil.getJsonDouble(obj, "aimingCurveY", -0.07D));
         this.withAimingCurveZ(JsonUtil.getJsonDouble(obj, "aimingCurveZ", 0.3D));
         this.withAimingCurvePitch(JsonUtil.getJsonDouble(obj, "aimingCurvePitch", -0.01D));
         this.withAimingCurveYaw(JsonUtil.getJsonDouble(obj, "aimingCurveYaw", -0.01D));
         this.withAimingCurveRoll(JsonUtil.getJsonDouble(obj, "aimingCurveRoll", -0.01D));
         this.withAimingZoom(JsonUtil.getJsonDouble(obj, "aimingZoom", 0.05D));
         this.withPipScopeZoom(JsonUtil.getJsonDouble(obj, "pipScopeZoom", 0.0D));
         this.withShotsPerRecoil(JsonUtil.getJsonInt(obj, "shotsPerRecoil", 1));
         this.withShotsPerTrace(JsonUtil.getJsonInt(obj, "shotsPerTrace", 1));
         this.withPelletSpread(JsonUtil.getJsonDouble(obj, "pelletSpread", 1.0D));
         this.withInaccuracy(JsonUtil.getJsonDouble(obj, "inaccuracy", 0.03D));
         this.withInaccuracyAiming(JsonUtil.getJsonDouble(obj, "inaccuracyAiming", 0.0D));
         this.withInaccuracySprinting(JsonUtil.getJsonDouble(obj, "inaccuracySprinting", 0.1D));
         this.withJumpMultiplier(JsonUtil.getJsonDouble(obj, "jumpMultiplier", 1.0D));
         this.withScopeOverlay(obj.has("scopeOverlay") ? obj.getAsJsonPrimitive("scopeOverlay").getAsString() : null);
         this.withReticleOverlay(obj.has("reticleOverlay") ? obj.getAsJsonPrimitive("reticleOverlay").getAsString() : null);
         this.withTargetLockOverlay(obj.has("targetLockOverlay") ? obj.getAsJsonPrimitive("targetLockOverlay").getAsString() : null);
         JsonElement targetLockedSoundElem = obj.get("targetLockedSound");
         if (targetLockedSoundElem != null && !targetLockedSoundElem.isJsonNull()) {
            String targetLockedSoundName = targetLockedSoundElem.getAsString();
            this.withTargetLockedSound(() -> {
               return SoundRegistry.getSoundEvent(targetLockedSoundName);
            });
         }

         JsonElement targetStartLockingSoundElem = obj.get("targetStartLockingSound");
         if (targetStartLockingSoundElem != null && !targetStartLockingSoundElem.isJsonNull()) {
            String targetStargetLockingdSoundName = targetStartLockingSoundElem.getAsString();
            this.withTargetStartLockingSound(() -> {
               return SoundRegistry.getSoundEvent(targetStargetLockingdSoundName);
            });
         }

         List<String> fireModeNames = JsonUtil.getStrings(obj, "fireModes");
         this.withFireModes((FireMode[])fireModeNames.stream().map((n) -> {
            return FireMode.valueOf(n.toUpperCase(Locale.ROOT));
         }).toArray((x$0) -> {
            return new FireMode[x$0];
         }));
         this.withHitScanSpeed(JsonUtil.getJsonFloat(obj, "hitScanSpeed", 800.0F));
         this.withHitScanAcceleration(JsonUtil.getJsonFloat(obj, "hitScanAcceleration", 0.0F));
         Iterator var7 = JsonUtil.getJsonObjects(obj, "reloadShakeEffects").iterator();

         while(var7.hasNext()) {
            JsonObject jsReloadShakeEffect = (JsonObject)var7.next();
            builder.withReloadShakeEffect((long)jsReloadShakeEffect.getAsJsonPrimitive("start").getAsInt(), (long)jsReloadShakeEffect.getAsJsonPrimitive("duration").getAsInt(), TimeUnit.MILLISECOND, JsonUtil.getJsonDouble(jsReloadShakeEffect, "initialAmplitude", 0.15D), JsonUtil.getJsonDouble(jsReloadShakeEffect, "rateOfAmplitudeDecay", 0.3D), JsonUtil.getJsonDouble(jsReloadShakeEffect, "initialAngularFrequency", 1.0D), JsonUtil.getJsonDouble(jsReloadShakeEffect, "rateOfFrequencyIncrease", 0.01D));
         }

         List<JsonObject> jsPhasedReloads = JsonUtil.getJsonObjects(obj, "phasedReloads");
         Iterator var30 = jsPhasedReloads.iterator();

         while(var30.hasNext()) {
            JsonObject jsPhasedReload = (JsonObject)var30.next();
            List<ReloadShakeEffect> shakeEffects = new ArrayList();
            Iterator var11 = JsonUtil.getJsonObjects(jsPhasedReload, "shakeEffects").iterator();

            while(var11.hasNext()) {
               JsonObject jsReloadShakeEffect = (JsonObject)var11.next();
               shakeEffects.add(new ReloadShakeEffect(jsReloadShakeEffect.getAsJsonPrimitive("start").getAsLong(), jsReloadShakeEffect.getAsJsonPrimitive("duration").getAsLong(), TimeUnit.MILLISECOND, JsonUtil.getJsonDouble(jsReloadShakeEffect, "initialAmplitude", 0.15D), JsonUtil.getJsonDouble(jsReloadShakeEffect, "rateOfAmplitudeDecay", 0.3D), JsonUtil.getJsonDouble(jsReloadShakeEffect, "initialAngularFrequency", 1.0D), JsonUtil.getJsonDouble(jsReloadShakeEffect, "rateOfFrequencyIncrease", 0.01D)));
            }

            Predicate<ConditionContext> condition = jsPhasedReload.has("condition") ? Conditions.fromJson(jsPhasedReload.get("condition")) : (ctx) -> {
               return true;
            };
            builder.withPhasedReload(new PhasedReload(ReloadPhase.valueOf(jsPhasedReload.getAsJsonPrimitive("phase").getAsString()), condition, (long)jsPhasedReload.getAsJsonPrimitive("duration").getAsInt(), TimeUnit.MILLISECOND, new ReloadAnimation(jsPhasedReload.getAsJsonPrimitive("animation").getAsString(), shakeEffects)));
         }

         JsonElement reloadAnimationElem = obj.get("reloadAnimation");
         String reloadAnimation = reloadAnimationElem != null ? reloadAnimationElem.getAsString() : null;
         builder.withReloadAnimation(reloadAnimation);
         float fireSoundVolume = JsonUtil.getJsonFloat(obj, "fireSoundVolume", 5.0F);
         JsonElement fireSoundElem = obj.get("fireSound");
         if (fireSoundElem != null && !fireSoundElem.isJsonNull()) {
            String fireSoundName = fireSoundElem.getAsString();
            builder.withFireSound(() -> {
               return SoundRegistry.getSoundEvent(fireSoundName);
            }, fireSoundVolume);
         }

         JsonElement reloadSoundElem = obj.get("reloadSound");
         if (reloadSoundElem != null && !reloadSoundElem.isJsonNull()) {
            String reloadSoundName = reloadSoundElem.getAsString();
            builder.withReloadSound(() -> {
               return SoundRegistry.getSoundEvent(reloadSoundName);
            });
         }

         List<String> compatibleAmmoNames = JsonUtil.getStrings(obj, "compatibleAmmo");
         List<Supplier<AmmoItem>> compatibleAmmo = new ArrayList();
         Iterator var15 = compatibleAmmoNames.iterator();

         while(var15.hasNext()) {
            String compatibleAmmoName = (String)var15.next();
            Supplier<Item> ri = ItemRegistry.ITEMS.getDeferredRegisteredObject(compatibleAmmoName);
            if (ri != null) {
               compatibleAmmo.add((Supplier)ri);
            }
         }

         builder.withCompatibleAmmo((List)compatibleAmmo);
         var15 = JsonUtil.getJsonObjects(obj, "rotations").iterator();

         JsonObject glowingPart;
         while(var15.hasNext()) {
            glowingPart = (JsonObject)var15.next();
            builder.withRotation(JsonUtil.getJsonString(glowingPart, "phase", "fire"), JsonUtil.getJsonString(glowingPart, "modelPart", (String)null), JsonUtil.getJsonDouble(glowingPart, "rpm", 180.0D), JsonUtil.getJsonDouble(glowingPart, "acceleration", 1.0D), JsonUtil.getJsonDouble(glowingPart, "deceleration", 5.0D));
         }

         var15 = JsonUtil.getJsonObjects(obj, "effects").iterator();

         while(var15.hasNext()) {
            glowingPart = (JsonObject)var15.next();
            FirePhase firePhase = (FirePhase)JsonUtil.getEnum(glowingPart, "phase", FirePhase.class, (Enum)null, true);
            String effectName = JsonUtil.getJsonString(glowingPart, "name");
            Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> supplier = () -> {
               return (EffectBuilder)EffectRegistry.getEffectBuilderSupplier(effectName).get();
            };
            builder.withEffect(firePhase, supplier);
         }

         builder.withBobbing(JsonUtil.getJsonDouble(obj, "bobbing", 1.0D));
         builder.withBobbingOnAim((double)JsonUtil.getJsonFloat(obj, "bobbingOnAim", 0.3F));
         builder.withBobbingRollMultiplier(JsonUtil.getJsonDouble(obj, "bobbingRollMultiplier", 1.0D));
         var15 = JsonUtil.getJsonObjects(obj, "glowingParts").iterator();

         JsonObject jsIdleAnimation;
         String partName;
         List jsIdleAnimations;
         List jsInspects;
         while(var15.hasNext()) {
            glowingPart = (JsonObject)var15.next();
            partName = JsonUtil.getJsonString(glowingPart, "name");
            jsIdleAnimations = JsonUtil.getStrings(obj, "phases");
            jsInspects = jsIdleAnimations.stream().map((n) -> {
               return FirePhase.valueOf(n.toUpperCase(Locale.ROOT));
            }).toList();
            if (jsInspects.isEmpty()) {
               jsInspects = Collections.singletonList(FirePhase.ANY);
            }

            String textureName = JsonUtil.getJsonString(glowingPart, "texture", (String)null);
            Direction direction = (Direction)JsonUtil.getEnum(glowingPart, "direction", Direction.class, (Enum)null, true);
            jsIdleAnimation = glowingPart.getAsJsonObject("sprites");
            if (jsIdleAnimation != null) {
               int rows = JsonUtil.getJsonInt(jsIdleAnimation, "rows", 1);
               int columns = JsonUtil.getJsonInt(jsIdleAnimation, "columns", 1);
               int fps = JsonUtil.getJsonInt(jsIdleAnimation, "fps", 60);
               AbstractEffect.SpriteAnimationType spriteAnimationType = (AbstractEffect.SpriteAnimationType)JsonUtil.getEnum(jsIdleAnimation, "type", AbstractEffect.SpriteAnimationType.class, AbstractEffect.SpriteAnimationType.LOOP, true);
               if (direction != null) {
                  builder.withGlow(jsInspects, partName, textureName, spriteAnimationType, rows, columns, fps, direction);
               } else {
                  builder.withGlow(jsInspects, partName, textureName, spriteAnimationType, rows, columns, fps);
               }
            } else {
               builder.withGlow(jsInspects, Collections.singletonList(partName), textureName);
            }
         }

         List<String> compatibleAttachmentNames = JsonUtil.getStrings(obj, "compatibleAttachments");
         Iterator var42 = compatibleAttachmentNames.iterator();

         while(var42.hasNext()) {
            partName = (String)var42.next();
            Supplier<Item> ri = ItemRegistry.ITEMS.getDeferredRegisteredObject(partName);
            if (ri != null) {
               this.withCompatibleAttachment(() -> {
                  return (Attachment)ri.get();
               });
            }
         }

         List<String> compatibleAttachmentGroups = JsonUtil.getStrings(obj, "compatibleAttachmentGroups");
         this.compatibleAttachmentGroups.addAll(compatibleAttachmentGroups);
         Iterator var46 = JsonUtil.getJsonObjects(obj, "features").iterator();

         while(var46.hasNext()) {
            JsonObject featureObj = (JsonObject)var46.next();
            FeatureBuilder<?, ?> featureBuilder = Features.fromJson(featureObj);
            this.withFeature(featureBuilder);
         }

         List<String> defaultAttachmentNames = JsonUtil.getStrings(obj, "defaultAttachments");
         Iterator var51 = defaultAttachmentNames.iterator();

         while(var51.hasNext()) {
            String defaultAttachmentName = (String)var51.next();
            Supplier<Item> ri = ItemRegistry.ITEMS.getDeferredRegisteredObject(defaultAttachmentName);
            if (ri != null) {
               this.withDefaultAttachment(() -> {
                  return (Attachment)ri.get();
               });
            }
         }

         jsIdleAnimations = JsonUtil.getJsonObjects(obj, "idleAnimations");
         Iterator var59 = jsIdleAnimations.iterator();

         while(var59.hasNext()) {
            JsonObject jsIdleAnimation = (JsonObject)var59.next();
            Predicate<ConditionContext> condition = jsIdleAnimation.has("condition") ? Conditions.fromJson(jsIdleAnimation.get("condition")) : (ctx) -> {
               return true;
            };
            builder.withIdleAnimation(JsonUtil.getJsonString(jsIdleAnimation, "name", "animation.model.idle"), condition, jsIdleAnimation.getAsJsonPrimitive("duration").getAsInt(), TimeUnit.MILLISECOND);
         }

         jsInspects = JsonUtil.getJsonObjects(obj, "inspectAnimations");
         Iterator var55 = jsInspects.iterator();

         while(var55.hasNext()) {
            JsonObject jsInspect = (JsonObject)var55.next();
            Predicate<ConditionContext> condition = jsInspect.has("condition") ? Conditions.fromJson(jsInspect.get("condition")) : (ctx) -> {
               return true;
            };
            builder.withInspectAnimation(JsonUtil.getJsonString(jsInspect, "name", "animation.model.inspect"), condition, jsInspect.getAsJsonPrimitive("duration").getAsInt(), TimeUnit.MILLISECOND);
         }

         if (jsInspects.isEmpty()) {
            builder.withInspectCooldownDuration(JsonUtil.getJsonInt(obj, "inspectCooldownDuration", 1000), TimeUnit.MILLISECOND);
         }

         List<JsonObject> jsDrawAnimations = JsonUtil.getJsonObjects(obj, "drawAnimations");
         Iterator var61 = jsDrawAnimations.iterator();

         while(var61.hasNext()) {
            jsIdleAnimation = (JsonObject)var61.next();
            Predicate<ConditionContext> condition = jsIdleAnimation.has("condition") ? Conditions.fromJson(jsIdleAnimation.get("condition")) : (ctx) -> {
               return true;
            };
            builder.withDrawAnimation(JsonUtil.getJsonString(jsIdleAnimation, "name", "animation.model.draw"), condition, jsIdleAnimation.getAsJsonPrimitive("duration").getAsInt(), TimeUnit.MILLISECOND);
         }

         if (jsDrawAnimations.isEmpty()) {
            builder.withDrawCooldownDuration(JsonUtil.getJsonInt(obj, "drawCooldownDuration", 500), TimeUnit.MILLISECOND);
         }

         List<JsonObject> jsFireAnimations = JsonUtil.getJsonObjects(obj, "fireAnimations");
         Iterator var64 = jsFireAnimations.iterator();

         while(var64.hasNext()) {
            JsonObject jsFireAnimation = (JsonObject)var64.next();
            Predicate<ConditionContext> condition = jsFireAnimation.has("condition") ? Conditions.fromJson(jsFireAnimation.get("condition")) : (ctx) -> {
               return true;
            };
            builder.withFireAnimation(JsonUtil.getJsonString(jsFireAnimation, "name", "animation.model.inspect"), condition, 0, TimeUnit.MILLISECOND);
         }

         return this;
      }
   }

   public static enum AnimationType {
      RIFLE("__DEFAULT_RIFLE_ANIMATIONS__", GunItem.FALLBACK_COMMON_ANIMATIONS),
      PISTOL("__DEFAULT_PISTOL_ANIMATIONS__", GunItem.FALLBACK_PISTOL_ANIMATIONS);

      private final String defaultThirdPersonAnimation;
      private final List<ResourceLocation> fallbackFirstPersonAnimations;

      private AnimationType(String defaultThirdPersonAnimation, List<ResourceLocation> fallbackFirstPersonAnimations) {
         this.defaultThirdPersonAnimation = defaultThirdPersonAnimation;
         this.fallbackFirstPersonAnimations = fallbackFirstPersonAnimations;
      }

      public String getDefaultThirdPersonAnimation() {
         return this.defaultThirdPersonAnimation;
      }

      private List<ResourceLocation> getFallbackFirstPersonAnimations() {
         return this.fallbackFirstPersonAnimations;
      }

      // $FF: synthetic method
      private static AnimationType[] $values() {
         return new AnimationType[]{RIFLE, PISTOL};
      }
   }

   public static record PhasedReload(ReloadPhase phase, Predicate<ConditionContext> predicate, long cooldownTime, TimeUnit timeUnit, ReloadAnimation reloadAnimation) {
      public PhasedReload(ReloadPhase phase, Predicate<ConditionContext> predicate, long cooldownTime, TimeUnit timeUnit, ReloadAnimation reloadAnimation) {
         if (cooldownTime == 0L) {
            throw new IllegalArgumentException("cooldownTime cannot be null");
         } else if (timeUnit == null) {
            throw new IllegalArgumentException("timeUnit cannot be null");
         } else if (predicate == null) {
            throw new IllegalArgumentException("predicate cannot be null");
         } else {
            this.phase = phase;
            this.predicate = predicate;
            this.cooldownTime = cooldownTime;
            this.timeUnit = timeUnit;
            this.reloadAnimation = reloadAnimation;
         }
      }

      public PhasedReload(ReloadPhase phase, TriPredicate<LivingEntity, GunClientState, ItemStack> predicate, long cooldownTime, ReloadAnimation reloadAnimation) {
         this(phase, (ctx) -> {
            return predicate.test(ctx.player(), ctx.gunClientState(), ctx.currentItemStack());
         }, cooldownTime, TimeUnit.MILLISECOND, reloadAnimation);
      }

      public PhasedReload(ReloadPhase phase, long cooldownTime, ReloadAnimation reloadAnimation) {
         this(phase, (ctx) -> {
            return true;
         }, cooldownTime, TimeUnit.MILLISECOND, reloadAnimation);
      }

      public PhasedReload(ReloadPhase phase, long cooldownTime, String animationName) {
         this(phase, (ctx) -> {
            return true;
         }, cooldownTime, TimeUnit.MILLISECOND, new ReloadAnimation(animationName));
      }

      public ReloadPhase phase() {
         return this.phase;
      }

      public Predicate<ConditionContext> predicate() {
         return this.predicate;
      }

      public long cooldownTime() {
         return this.cooldownTime;
      }

      public TimeUnit timeUnit() {
         return this.timeUnit;
      }

      public ReloadAnimation reloadAnimation() {
         return this.reloadAnimation;
      }
   }

   public static enum ReloadPhase {
      PREPARING,
      RELOADING,
      COMPLETETING;

      // $FF: synthetic method
      private static ReloadPhase[] $values() {
         return new ReloadPhase[]{PREPARING, RELOADING, COMPLETETING};
      }
   }

   public static enum FirePhase {
      PREPARING,
      FIRING,
      COMPLETETING,
      HIT_SCAN_ACQUIRED,
      HIT_TARGET,
      ANY,
      FLYING;

      // $FF: synthetic method
      private static FirePhase[] $values() {
         return new FirePhase[]{PREPARING, FIRING, COMPLETETING, HIT_SCAN_ACQUIRED, HIT_TARGET, ANY, FLYING};
      }
   }

   public static record ReloadAnimation(String animationName, List<ReloadShakeEffect> shakeEffects) {
      public ReloadAnimation(String animationName, List<ReloadShakeEffect> shakeEffects) {
         this.animationName = animationName;
         this.shakeEffects = shakeEffects;
      }

      public ReloadAnimation(String animationName) {
         this(animationName, Collections.emptyList());
      }

      public String animationName() {
         return this.animationName;
      }

      public List<ReloadShakeEffect> shakeEffects() {
         return this.shakeEffects;
      }
   }

   public static record ReloadShakeEffect(long startTime, long duration, TimeUnit timeUnit, double initialAmplitude, double rateOfAmplitudeDecay, double initialAngularFrequency, double rateOfFrequencyIncrease) {
      private static double DEFAULT_INITIAL_ANGULAR_FREQUENCY = 1.0D;
      private static double DEFAULT_RATE_OF_FREQUENCY_INCREASE = 0.01D;

      public ReloadShakeEffect(long startTime, long duration, TimeUnit timeUnit, double initialAmplitude, double rateOfAmplitudeDecay, double initialAngularFrequency, double rateOfFrequencyIncrease) {
         this.startTime = startTime;
         this.duration = duration;
         this.timeUnit = timeUnit;
         this.initialAmplitude = initialAmplitude;
         this.rateOfAmplitudeDecay = rateOfAmplitudeDecay;
         this.initialAngularFrequency = initialAngularFrequency;
         this.rateOfFrequencyIncrease = rateOfFrequencyIncrease;
      }

      public ReloadShakeEffect(long startTime, long duration, double initialAmplitude, double rateOfAmplitudeDecay) {
         this(startTime, duration, TimeUnit.MILLISECOND, initialAmplitude, rateOfAmplitudeDecay, DEFAULT_INITIAL_ANGULAR_FREQUENCY, DEFAULT_RATE_OF_FREQUENCY_INCREASE);
      }

      public long startTime() {
         return this.startTime;
      }

      public long duration() {
         return this.duration;
      }

      public TimeUnit timeUnit() {
         return this.timeUnit;
      }

      public double initialAmplitude() {
         return this.initialAmplitude;
      }

      public double rateOfAmplitudeDecay() {
         return this.rateOfAmplitudeDecay;
      }

      public double initialAngularFrequency() {
         return this.initialAngularFrequency;
      }

      public double rateOfFrequencyIncrease() {
         return this.rateOfFrequencyIncrease;
      }
   }
}
