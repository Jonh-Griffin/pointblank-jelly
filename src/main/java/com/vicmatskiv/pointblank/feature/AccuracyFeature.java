package com.vicmatskiv.pointblank.feature;

import com.google.gson.JsonObject;
import com.vicmatskiv.pointblank.util.Conditions;
import com.vicmatskiv.pointblank.util.JsonUtil;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public class AccuracyFeature extends ConditionalFeature {
   private static final float MIN_ACCURACY_MODIFIER = 0.1F;
   private static final float MAX_ACCURACY_MODIFIER = 10.0F;
   private float accuracyModifier;

   private AccuracyFeature(FeatureProvider owner, Predicate<ConditionContext> predicate, float accuracyModifier) {
      super(owner, predicate);
      this.accuracyModifier = accuracyModifier;
   }

   public MutableComponent getDescription() {
      return this.accuracyModifier < 1.0F ? Component.m_237115_("description.pointblank.reducesAccuracy").m_7220_(Component.m_237113_(String.format(" %.0f%%", 100.0F * (1.0F - this.accuracyModifier)))) : Component.m_237115_("description.pointblank.increasesAccuracy").m_7220_(Component.m_237113_(String.format(" %.0f%%", 100.0F * (this.accuracyModifier - 1.0F))));
   }

   public float getAccuracyModifier() {
      return this.accuracyModifier;
   }

   public static float getAccuracyModifier(ItemStack itemStack) {
      List<Features.EnabledFeature> enabledAccuracyFeatures = Features.getEnabledFeatures(itemStack, AccuracyFeature.class);
      float accuracyModifier = 1.0F;

      AccuracyFeature accuracyFeature;
      for(Iterator var3 = enabledAccuracyFeatures.iterator(); var3.hasNext(); accuracyModifier *= accuracyFeature.getAccuracyModifier()) {
         Features.EnabledFeature enabledFeature = (Features.EnabledFeature)var3.next();
         accuracyFeature = (AccuracyFeature)enabledFeature.feature();
      }

      return Mth.m_14036_(accuracyModifier, 0.1F, 10.0F);
   }

   public static class Builder implements FeatureBuilder<Builder, AccuracyFeature> {
      private Predicate<ConditionContext> condition = (ctx) -> {
         return true;
      };
      private float accuracyModifier;

      public Builder withCondition(Predicate<ConditionContext> condition) {
         this.condition = condition;
         return this;
      }

      public Builder withAccuracyModifier(double accuracyModifier) {
         this.accuracyModifier = (float)accuracyModifier;
         return this;
      }

      public Builder withJsonObject(JsonObject obj) {
         if (obj.has("condition")) {
            this.withCondition(Conditions.fromJson(obj.getAsJsonObject("condition")));
         }

         this.withAccuracyModifier((double)JsonUtil.getJsonFloat(obj, "accuracyModifier"));
         return this;
      }

      public AccuracyFeature build(FeatureProvider featureProvider) {
         return new AccuracyFeature(featureProvider, this.condition, this.accuracyModifier);
      }
   }
}
