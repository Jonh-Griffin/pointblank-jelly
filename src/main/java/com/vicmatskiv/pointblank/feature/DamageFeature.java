package com.vicmatskiv.pointblank.feature;

import com.google.gson.JsonObject;
import com.vicmatskiv.pointblank.util.Conditions;
import com.vicmatskiv.pointblank.util.JsonUtil;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public class DamageFeature extends ConditionalFeature {
   private static final float MIN_DAMAGE_MODIFIER = 0.01F;
   private static final float MAX_DAMAGE_MODIFIER = 10.0F;
   private final float hitScanDamageModifier;
   private final Component description;

   private DamageFeature(FeatureProvider owner, Predicate<ConditionContext> predicate, float hitScanDamageModifier) {
      super(owner, predicate);
      this.hitScanDamageModifier = hitScanDamageModifier;
      if (hitScanDamageModifier < 1.0F) {
         this.description = Component.translatable("description.pointblank.reducesDamage").append(Component.literal(String.format(" %.0f%%", 100.0F * (1.0F - hitScanDamageModifier))));
      } else {
         this.description = Component.translatable("description.pointblank.increasesDamage").append(Component.literal(String.format(" %.0f%%", 100.0F * (hitScanDamageModifier - 1.0F))));
      }

   }

   public Component getDescription() {
      return this.description;
   }

   public float getHitScanDamageModifier() {
      return this.hitScanDamageModifier;
   }

   public static float getHitScanDamageModifier(ItemStack itemStack) {
      List<Features.EnabledFeature> enabledDamageFeatures = Features.getEnabledFeatures(itemStack, DamageFeature.class);
      float hitScanDamageModifier = 1.0F;

      for(Features.EnabledFeature enabledFeature : enabledDamageFeatures) {
         DamageFeature damageFeature = (DamageFeature)enabledFeature.feature();
         hitScanDamageModifier *= damageFeature.getHitScanDamageModifier();
      }

      return Mth.clamp(hitScanDamageModifier, 0.01F, 10.0F);
   }

   public static class Builder implements FeatureBuilder<Builder, DamageFeature> {
      private Predicate<ConditionContext> condition = (ctx) -> true;
      private float hitScanDamageModifier;

      public Builder() {
      }

      public Builder withCondition(Predicate<ConditionContext> condition) {
         this.condition = condition;
         return this;
      }

      public Builder withHitScanDamageModifier(double damageModifier) {
         this.hitScanDamageModifier = (float)damageModifier;
         return this;
      }

      public Builder withJsonObject(JsonObject obj) {
         if (obj.has("condition")) {
            this.withCondition(Conditions.fromJson(obj.getAsJsonObject("condition")));
         }

         this.withHitScanDamageModifier(JsonUtil.getJsonFloat(obj, "hitScanDamageModifier"));
         return this;
      }

      public DamageFeature build(FeatureProvider featureProvider) {
         return new DamageFeature(featureProvider, this.condition, this.hitScanDamageModifier);
      }
   }
}
