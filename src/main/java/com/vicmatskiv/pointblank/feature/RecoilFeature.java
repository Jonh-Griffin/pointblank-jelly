package com.vicmatskiv.pointblank.feature;

import com.google.gson.JsonObject;
import com.vicmatskiv.pointblank.util.Conditions;
import com.vicmatskiv.pointblank.util.JsonUtil;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public class RecoilFeature extends ConditionalFeature {
   private static final float MIN_RECOIL_MODIFIER = 0.01F;
   private static final float MAX_RECOIL_MODIFIER = 10.0F;
   private float recoilModifier;
   private Component description;

   private RecoilFeature(FeatureProvider owner, Predicate<ConditionContext> predicate, float recoilModifier) {
      super(owner, predicate);
      this.recoilModifier = recoilModifier;
      if (recoilModifier < 1.0F) {
         this.description = Component.m_237115_("description.pointblank.reducesRecoil").m_7220_(Component.m_237113_(String.format(" %.0f%%", 100.0F * (1.0F - recoilModifier))));
      } else {
         this.description = Component.m_237115_("description.pointblank.increasesRecoil").m_7220_(Component.m_237113_(String.format(" %.0f%%", 100.0F * (recoilModifier - 1.0F))));
      }

   }

   public Component getDescription() {
      return this.description;
   }

   public float getRecoilModifier() {
      return this.recoilModifier;
   }

   public static float getRecoilModifier(ItemStack itemStack) {
      List<Features.EnabledFeature> enabledRecoilFeatures = Features.getEnabledFeatures(itemStack, RecoilFeature.class);
      float recoilModifier = 1.0F;

      RecoilFeature recoilFeature;
      for(Iterator var3 = enabledRecoilFeatures.iterator(); var3.hasNext(); recoilModifier *= recoilFeature.getRecoilModifier()) {
         Features.EnabledFeature enabledFeature = (Features.EnabledFeature)var3.next();
         recoilFeature = (RecoilFeature)enabledFeature.feature();
      }

      return Mth.m_14036_(recoilModifier, 0.01F, 10.0F);
   }

   public static class Builder implements FeatureBuilder<Builder, RecoilFeature> {
      private Predicate<ConditionContext> condition = (ctx) -> {
         return true;
      };
      private float recoilModifier;

      public Builder withCondition(Predicate<ConditionContext> condition) {
         this.condition = condition;
         return this;
      }

      public Builder withRecoilModifier(double recoilModifier) {
         this.recoilModifier = (float)recoilModifier;
         return this;
      }

      public Builder withJsonObject(JsonObject obj) {
         if (obj.has("condition")) {
            this.withCondition(Conditions.fromJson(obj.getAsJsonObject("condition")));
         }

         this.withRecoilModifier((double)JsonUtil.getJsonFloat(obj, "recoilModifier"));
         return this;
      }

      public RecoilFeature build(FeatureProvider featureProvider) {
         return new RecoilFeature(featureProvider, this.condition, this.recoilModifier);
      }
   }
}
