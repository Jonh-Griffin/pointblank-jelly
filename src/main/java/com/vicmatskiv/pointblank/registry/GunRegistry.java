package com.vicmatskiv.pointblank.registry;

import com.vicmatskiv.pointblank.feature.*;
import com.vicmatskiv.pointblank.item.AnimationProvider;
import com.vicmatskiv.pointblank.item.FireMode;
import com.vicmatskiv.pointblank.item.FireModeInstance;
import com.vicmatskiv.pointblank.item.GunItem;
import com.vicmatskiv.pointblank.util.Conditions;
import com.vicmatskiv.pointblank.util.TimeUnit;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ItemLike;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class GunRegistry {
   //public static final Supplier<GunItem> M1911A1;
   //public static final Supplier<GunItem> TTI_VIPER;
   //public static final Supplier<GunItem> P30L;
   //public static final Supplier<GunItem> MK23;
   //public static final Supplier<GunItem> GLOCK17;
   //public static final Supplier<GunItem> GLOCK18;
   //public static final Supplier<GunItem> M9;
   //public static final Supplier<GunItem> DESERTEAGLE;
   //public static final Supplier<GunItem> RHINO;
   //public static final Supplier<GunItem> M4A1;
   //public static final Supplier<GunItem> M4A1MOD1;
   //public static final Supplier<GunItem> AR57;
   //public static final Supplier<GunItem> RO635;
   //public static final Supplier<GunItem> STAR15;
   //public static final Supplier<GunItem> M4SOPMODII;
   //public static final Supplier<GunItem> M16A1;
   //public static final Supplier<GunItem> HK416;
   //public static final Supplier<GunItem> SCARL;
   //public static final Supplier<GunItem> XM7;
   //public static final Supplier<GunItem> XM29;
   //public static final Supplier<GunItem> G36C;
   //public static final Supplier<GunItem> G36K;
   //public static final Supplier<GunItem> SL8;
   /*public static final Supplier<GunItem> MK14EBR;
   public static final Supplier<GunItem> UAR10;
   //yesterday public static final Supplier<GunItem> AK47;
   //public static final Supplier<GunItem> AK74;
   //public static final Supplier<GunItem> AK12;
   //public static final Supplier<GunItem> AN94;
   //public static final Supplier<GunItem> AUG;
   //public static final Supplier<GunItem> AUGHBAR;
   //today public static final Supplier<GunItem> MP5;
   //public static final Supplier<GunItem> MP7;
   //public static final Supplier<GunItem> UMP45;
   //public static final Supplier<GunItem> VECTOR;
   //public static final Supplier<GunItem> P90;
   //public static final Supplier<GunItem> TMP;
   //public static final Supplier<GunItem> M950;
   //public static final Supplier<GunItem> G41;
   //public static final Supplier<GunItem> G3;
   //public static final Supplier<GunItem> WA2000;
   //public static final Supplier<GunItem> XM3;
   //public static final Supplier<GunItem> C14;
   //public static final Supplier<GunItem> L96A1;
   //public static final Supplier<GunItem> BALLISTA;
   //public static final Supplier<GunItem> GM6LYNX;
   public static final Supplier<GunItem> M590;
   public static final Supplier<GunItem> M870;
   public static final Supplier<GunItem> SPAS12;
   public static final Supplier<GunItem> M1014;
   public static final Supplier<GunItem> AA12;
   public static final Supplier<GunItem> CITORICXS;
   public static final Supplier<GunItem> HS12;
   public static final Supplier<GunItem> LAMG;
   public static final Supplier<GunItem> MK48;
   public static final Supplier<GunItem> M249;
   public static final Supplier<GunItem> M32MGL;
   public static final Supplier<GunItem> SMAW;
   public static final Supplier<GunItem> JAVELIN;
   public static final Supplier<GunItem> AT4;
   public static final Supplier<GunItem> M134MINIGUN;

   @Deprecated
   public static void registerTabItems(Consumer<ItemLike> entries) {

   }

   public static void init() {
   }

   static {
     /*XM29 = ItemRegistry.ITEMS.register((new GunItem.Builder()).withName("xm29").withMaxAmmoCapacity(30).withCompatibleAmmo(AmmoRegistry.AMMOCREATIVE).withCompatibleAmmo(AmmoRegistry.AMMO556).withRpm(800).withFireModes(FireMode.SINGLE, FireMode.AUTOMATIC).withDrawCooldownDuration(650, TimeUnit.MILLISECOND).withInspectCooldownDuration(4200, TimeUnit.MILLISECOND).withGunRecoilInitialAmplitude(0.3D).withShakeRecoilAmplitude(0.35D).withShakeRecoilSpeed(2.0D).withViewRecoilAmplitude(1.6D).withModelScale(0.5D).withGunRecoilPitchMultiplier(2.0D).withGunRandomizationAmplitude(0.0025D).withBobbing(0.6D).withBobbingRollMultiplier(1.0D).withJumpMultiplier(1.0D).withEffect(GunItem.FirePhase.HIT_SCAN_ACQUIRED, EffectRegistry.TRACER).withFeature((new MuzzleFlashFeature.Builder()).withEffect(GunItem.FirePhase.FIRING, EffectRegistry.MUZZLE_FLASH, Conditions.unselectedFireMode("grenade_launcher").and(Conditions.doesNotHaveAttachmentGroup("ar_suppressors"))).withEffect(GunItem.FirePhase.FIRING, EffectRegistry.LAUNCHER_MUZZLE, Conditions.selectedFireMode("grenade_launcher"))).withFeature((new ActiveMuzzleFeature.Builder()).withPart("muzzleflash", Conditions.unselectedFireMode("grenade_launcher").and(Conditions.doesNotHaveAttachmentGroup("ar_suppressors"))).withPart("muzzleflash2", Conditions.selectedFireMode("grenade_launcher"))).withFeature((new AimingFeature.Builder()).withCondition(Conditions.doesNotHaveAttachmentGroup("m16_sightsandscopes")).withZoom(0.35D)).withFeature(
             (new PipFeature.Builder())
                     .withCondition(Conditions.doesNotHaveAttachmentGroup("m16_sightsandscopes"))
                     .withOverlayTexture("textures/gui/starscope_pip.png")
                     .withParallaxEnabled(true).
                     withZoom(0.75D)
     ).withFeature(
             (new PartVisibilityFeature.Builder())
                     .withHiddenPart("xm29scope", Conditions.hasAttachmentGroup("m16_sightsandscopes"))
     ).withFeature(
             (new SoundFeature.Builder())
                     .withFireSound(SoundRegistry.XM29, 6.0D, Conditions.unselectedFireMode("grenade_launcher").and(Conditions.doesNotHaveAttachmentGroup("ar_suppressors")))
                     .withFireSound(SoundRegistry.M4A1_SILENCED, 1.0D, Conditions.unselectedFireMode("grenade_launcher").and(Conditions.hasAttachmentGroup("ar_suppressors")))
                     .withFireSound(SoundRegistry.XM29_GRENADE, 6.0D, Conditions.selectedFireMode("grenade_launcher")))
             .withCompatibleAttachmentGroup("ar_sightsandscopes").withCompatibleAttachment(AttachmentRegistry.CANTED_RAIL).withCompatibleAttachmentGroup("ar_muzzle").withCompatibleAttachmentGroup("xm29_skins")
             .withFeature(
                     (new FireModeFeature.Builder())
                             .withFireMode("single", FireMode.SINGLE, Component.translatable("label.pointblank.fireMode.single"), 800, 5.0D, true, "animation.model.fire", new FireModeInstance.ViewShakeDescriptor(350L, 0.35D, 2.0D)).withFireMode("automatic", FireMode.AUTOMATIC, Component.translatable("label.pointblank.fireMode.automatic"), 800, 5.0D, true, "animation.model.fire", new FireModeInstance.ViewShakeDescriptor(350L, 0.35D, 2.0D))
                             .withFireMode("grenade_launcher", FireMode.SINGLE, Component.translatable("label.pointblank.fireMode.grenade"),
                             AmmoRegistry.GRENADE20MM, 6, 190, 20.0D, false, "animation.model.firegrenade", new FireModeInstance.ViewShakeDescriptor(350L, 2.0D, 2.0D)))
             .withPhasedReload(GunItem.ReloadPhase.RELOADING, Conditions.onEmptyReload().and(Conditions.unselectedFireMode("grenade_launcher")), 2980L, new GunItem.ReloadAnimation("animation.model.reloadempty")).withPhasedReload(GunItem.ReloadPhase.RELOADING, Conditions.onNonEmptyReload().and(Conditions.unselectedFireMode("grenade_launcher")), 2480L, new GunItem.ReloadAnimation("animation.model.reload")).withPhasedReload(GunItem.ReloadPhase.RELOADING, Conditions.onEmptyReload().and(Conditions.selectedFireMode("grenade_launcher")), 2370L, new GunItem.ReloadAnimation("animation.model.reloadgrenadeempty", List.of(new GunItem.ReloadShakeEffect(0L, 800L, 0.1D, 0.4D), new GunItem.ReloadShakeEffect(430L, 1000L, 0.25D, 0.5D), new GunItem.ReloadShakeEffect(870L, 500L, 0.2D, 0.5D), new GunItem.ReloadShakeEffect(1130L, 600L, 0.25D, 0.5D), new GunItem.ReloadShakeEffect(1580L, 600L, 0.2D, 0.5D), new GunItem.ReloadShakeEffect(1720L, 600L, 0.25D, 0.5D)))).withPhasedReload(GunItem.ReloadPhase.RELOADING, Conditions.onNonEmptyReload().and(Conditions.selectedFireMode("grenade_launcher")), 1770L, new GunItem.ReloadAnimation("animation.model.reloadgrenade", List.of(new GunItem.ReloadShakeEffect(0L, 800L, 0.1D, 0.4D), new GunItem.ReloadShakeEffect(430L, 1000L, 0.25D, 0.5D), new GunItem.ReloadShakeEffect(870L, 500L, 0.2D, 0.5D), new GunItem.ReloadShakeEffect(1130L, 600L, 0.25D, 0.5D)))));
   */
}
