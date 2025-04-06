package com.vicmatskiv.pointblank;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@EventBusSubscriber(
   modid = "pointblank",
   bus = Bus.MOD
)
public class Config {
   private static final Builder BUILDER = new Builder();
   public static final ConfigValue<AutoReload> AUTO_RELOAD;
   private static final DoubleValue SCOPE_AIMING_MOUSE_SENSITIVITY;
   private static final BooleanValue RESET_AUTO_FIRE_PITCH_ENABLED;
   private static final DoubleValue KNOCKBACK;
   private static final BooleanValue PARTICLE_EFFECTS_ENABLED;
   private static final IntValue ARMS_DEALER_HOUSE_WEIGHT;
   private static final DoubleValue EMERALD_EXCHANGE_RATE;
   private static final BooleanValue THIRD_PERSON_ARM_POSE_ALWAYS_ON;
   private static final IntValue PIP_SCOPE_REFRESH_FRAME;
   public static final BooleanValue PIP_SCOPES_ENABLED;
   private static final IntValue PIP_SCOPE_COLOR_BALANCE_RED;
   private static final IntValue PIP_SCOPE_COLOR_BALANCE_GREEN;
   private static final IntValue PIP_SCOPE_COLOR_BALANCE_BLUE;
   public static final BooleanValue CUSTOM_SHADERS_ENABLED;
   public static final BooleanValue EXPLOSION_DESTROY_BLOCKS_ENABLED;
   private static final DoubleValue ITEM_DROP_CHANCE;
   private static final IntValue MAX_ITEM_DROP_COUNT;
   public static final BooleanValue BULLETS_BREAK_GLASS_ENABLED;
   private static final DoubleValue HITSCAN_DAMAGE_MODIFIER;
   private static final DoubleValue HEADSHOT_DAMAGE_MODIFIER;
   public static final ConfigValue<CrosshairType> CROSSHAIR_TYPE;
   public static final BooleanValue GORE_ENABLED;
   public static final BooleanValue ADVANCE_IRIS_INTEGRATION_ENABLED;
   public static final BooleanValue FIRST_PERSON_ANIMATIONS_ENABLED;
   public static final BooleanValue THIRD_PERSON_ANIMATIONS_ENABLED;
   static final ForgeConfigSpec SPEC;
   public static AutoReload autoReload;
   public static double scopeAimingMouseSensitivity;
   public static boolean resetAutoFirePitchEnabled;
   public static double knockback;
   public static boolean particleEffectsEnabled;
   public static int armsDealerHouseWeight;
   public static double emeraldExchangeRate;
   public static boolean thirdPersonArmPoseAlwaysOn;
   public static boolean pipScopesEnabled;
   public static int pipScopeRefreshFrame;
   public static int pipScopeColorBalanceRed;
   public static int pipScopeColorBalanceGreen;
   public static int pipScopeColorBalanceBlue;
   public static boolean customShadersEnabled;
   public static boolean explosionDestroyBlocksEnabled;
   public static double itemDropChance;
   public static int maxItemDropCount;
   public static boolean bulletsBreakGlassEnabled;
   public static double hitscanDamageModifier;
   public static double headshotDamageModifier;
   public static CrosshairType crosshairType;
   public static boolean goreEnabled;
   public static boolean advancedIrisIntegrationEnabled;
   public static boolean firstPersonAnimationsEnabled;
   public static boolean thirdPersonAnimationsEnabled;

   @SubscribeEvent
   static void onLoad(ModConfigEvent event) {
      autoReload = (AutoReload)AUTO_RELOAD.get();
      scopeAimingMouseSensitivity = (Double)SCOPE_AIMING_MOUSE_SENSITIVITY.get();
      resetAutoFirePitchEnabled = (Boolean)RESET_AUTO_FIRE_PITCH_ENABLED.get();
      knockback = (Double)KNOCKBACK.get();
      particleEffectsEnabled = (Boolean)PARTICLE_EFFECTS_ENABLED.get();
      armsDealerHouseWeight = (Integer)ARMS_DEALER_HOUSE_WEIGHT.get();
      emeraldExchangeRate = (Double)EMERALD_EXCHANGE_RATE.get();
      thirdPersonArmPoseAlwaysOn = (Boolean)THIRD_PERSON_ARM_POSE_ALWAYS_ON.get();
      pipScopeRefreshFrame = (Integer)PIP_SCOPE_REFRESH_FRAME.get();
      pipScopesEnabled = (Boolean)PIP_SCOPES_ENABLED.get();
      pipScopeColorBalanceRed = (Integer)PIP_SCOPE_COLOR_BALANCE_RED.get();
      pipScopeColorBalanceGreen = (Integer)PIP_SCOPE_COLOR_BALANCE_GREEN.get();
      pipScopeColorBalanceBlue = (Integer)PIP_SCOPE_COLOR_BALANCE_BLUE.get();
      customShadersEnabled = (Boolean)CUSTOM_SHADERS_ENABLED.get();
      explosionDestroyBlocksEnabled = (Boolean)EXPLOSION_DESTROY_BLOCKS_ENABLED.get();
      itemDropChance = (Double)ITEM_DROP_CHANCE.get();
      maxItemDropCount = (Integer)MAX_ITEM_DROP_COUNT.get();
      bulletsBreakGlassEnabled = (Boolean)BULLETS_BREAK_GLASS_ENABLED.get();
      hitscanDamageModifier = (Double)HITSCAN_DAMAGE_MODIFIER.get();
      headshotDamageModifier = (Double)HEADSHOT_DAMAGE_MODIFIER.get();
      crosshairType = (CrosshairType)CROSSHAIR_TYPE.get();
      goreEnabled = (Boolean)GORE_ENABLED.get();
      advancedIrisIntegrationEnabled = (Boolean)ADVANCE_IRIS_INTEGRATION_ENABLED.get();
      firstPersonAnimationsEnabled = (Boolean)FIRST_PERSON_ANIMATIONS_ENABLED.get();
      thirdPersonAnimationsEnabled = (Boolean)THIRD_PERSON_ANIMATIONS_ENABLED.get();
   }

   static {
      AUTO_RELOAD = BUILDER.comment("Enables auto-reloading of guns").defineEnum("autoReloadEnabled", AutoReload.CREATIVE);
      SCOPE_AIMING_MOUSE_SENSITIVITY = BUILDER.comment("Adjusts mouse sensitivity when using scopes").defineInRange("scopeAimingMouseSensitivity", 0.4D, 0.01D, 0.9D);
      RESET_AUTO_FIRE_PITCH_ENABLED = BUILDER.comment("Toggle to reset player pitch to original after auto fire").define("resetAutoFirePitchEnabled", true);
      KNOCKBACK = BUILDER.comment("Adjusts the knockback force applied to entities hit by gunfire, with higher values causing greater knockback distance.").defineInRange("knockback", 1.0D, 0.1D, 2.0D);
      PARTICLE_EFFECTS_ENABLED = BUILDER.comment("Enables particle effects").define("particleEffectsEnabled", true);
      ARMS_DEALER_HOUSE_WEIGHT = BUILDER.comment("Sets the likelihood of an 'Arms Dealer' house appearing in new villages, with higher values increasing frequency and lower values making it rarer.").defineInRange("armsDealerHouse", 10, 0, 20);
      EMERALD_EXCHANGE_RATE = BUILDER.comment("Set the exchange rate to determine how many in-game price units are equivalent to one emerald.").defineInRange("emeraldExchangeRate", 100.0D, 1.0D, 1000.0D);
      THIRD_PERSON_ARM_POSE_ALWAYS_ON = BUILDER.comment("Controls whether the player's arm pose is permanently set to the aiming/firing position in third-person view, regardless of their current action with a gun.").define("thirdPersonArmPoseAlwaysOn", true);
      PIP_SCOPE_REFRESH_FRAME = BUILDER.comment("Specifies how often the \"picture-in-picture\" scope updates, with 1 being every frame, 2 for every other frame, etc. A higher number may improve performance.").defineInRange("pipScopeRefreshRate", 2, 0, 5);
      PIP_SCOPES_ENABLED = BUILDER.comment("Enables pip scopes").define("pipScopesEnabled", true);
      PIP_SCOPE_COLOR_BALANCE_RED = BUILDER.comment("Sets pip scope red color balance. This options works for shaders only.").defineInRange("pipScopeColorBalanceRed", 90, 0, 255);
      PIP_SCOPE_COLOR_BALANCE_GREEN = BUILDER.comment("Sets pip scope green color balance. This options works for shaders only.").defineInRange("pipScopeColorBalanceGreen", 105, 0, 255);
      PIP_SCOPE_COLOR_BALANCE_BLUE = BUILDER.comment("Sets pip scope blue color balance. This options works for shaders only.").defineInRange("pipScopeColorBalanceBlue", 110, 0, 255);
      CUSTOM_SHADERS_ENABLED = BUILDER.comment("Enables custom shaders").define("customShadersEnabled", true);
      EXPLOSION_DESTROY_BLOCKS_ENABLED = BUILDER.comment("Enables explosions to destroy blocks").define("explosionDestroyBlocksEnabled", true);
      ITEM_DROP_CHANCE = BUILDER.comment("Sets the probability of dropping some item(s) by monster mobs.").defineInRange("itemDropChance", 0.35D, 0.0D, 1.0D);
      MAX_ITEM_DROP_COUNT = BUILDER.comment("Sets the max count of items to be dropped by a monster mob.").defineInRange("maxItemDropCount", 7, 2, 64);
      BULLETS_BREAK_GLASS_ENABLED = BUILDER.comment("Enables bullets breaking glass").define("bulletsBreakGlassEnabled", true);
      HITSCAN_DAMAGE_MODIFIER = BUILDER.comment("Modifier to adjust damage dealt by hitscan weapons.").defineInRange("hitscanDamageModifier", 1.0D, 0.1D, 10.0D);
      HEADSHOT_DAMAGE_MODIFIER = BUILDER.comment("Modifier to adjust damage dealt by headshot.").defineInRange("headshotDamageModifier", 3.0D, 1.0D, 10.0D);
      CROSSHAIR_TYPE = BUILDER.comment("Sets the crosshair type.").defineEnum("crosshair", CrosshairType.DEFAULT);
      GORE_ENABLED = BUILDER.comment("Enables gore effects").define("goreEnabled", true);
      ADVANCE_IRIS_INTEGRATION_ENABLED = BUILDER.comment("Enables advanced integraiton with Iris shaders").define("advancedIrisIntegrationEnabled", true);
      FIRST_PERSON_ANIMATIONS_ENABLED = BUILDER.comment("Enables advanced first person animations").define("firstPersonAnimationsEnabled", true);
      THIRD_PERSON_ANIMATIONS_ENABLED = BUILDER.comment("Enables advanced third person animations").define("thirdPersonAnimationsEnabled", true);
      SPEC = BUILDER.build();
   }

   public static enum AutoReload {
      CREATIVE,
      SURVIVAL,
      ENABLED,
      DISABLED;

      // $FF: synthetic method
      private static AutoReload[] $values() {
         return new AutoReload[]{CREATIVE, SURVIVAL, ENABLED, DISABLED};
      }
   }

   public static enum CrosshairType {
      DEFAULT,
      VANILLA,
      DISABLED;

      // $FF: synthetic method
      private static CrosshairType[] $values() {
         return new CrosshairType[]{DEFAULT, VANILLA, DISABLED};
      }
   }
}
