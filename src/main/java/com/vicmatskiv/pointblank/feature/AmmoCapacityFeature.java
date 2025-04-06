package com.vicmatskiv.pointblank.feature;

import com.google.gson.JsonObject;
import com.vicmatskiv.pointblank.util.Conditions;
import com.vicmatskiv.pointblank.util.JsonUtil;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public class AmmoCapacityFeature extends ConditionalFeature {
   private static final int MIN_AMMO = 1;
   private static final int MAX_AMMO = Integer.MAX_VALUE;
   private IntUnaryOperator ammoCapacityTransformer;
   private Component description;

   private AmmoCapacityFeature(FeatureProvider owner, Predicate<ConditionContext> predicate, IntUnaryOperator ammoCapacityTransformer, Component description) {
      super(owner, predicate);
      this.description = description;
      this.ammoCapacityTransformer = ammoCapacityTransformer;
   }

   public Component getDescription() {
      return this.description;
   }

   public static int modifyAmmoCapacity(ItemStack itemStack, int ammoCapacity) {
      Features.EnabledFeature enabledExtendedAmmoFeature = Features.getFirstEnabledFeature(itemStack, AmmoCapacityFeature.class);
      return enabledExtendedAmmoFeature != null ? ((AmmoCapacityFeature)enabledExtendedAmmoFeature.feature()).ammoCapacityTransformer.applyAsInt(ammoCapacity) : ammoCapacity;
   }

   public static class Builder implements FeatureBuilder<Builder, AmmoCapacityFeature> {
      private Predicate<ConditionContext> condition = (ctx) -> {
         return true;
      };
      private IntUnaryOperator ammoCapacityTransformer;
      private Component description;

      public Builder withCondition(Predicate<ConditionContext> condition) {
         this.condition = condition;
         return this;
      }

      public Builder withAmmoCapacityModifier(int ammoCapacityModifier) {
         this.ammoCapacityTransformer = (ammo) -> {
            return Mth.m_14045_(ammo * ammoCapacityModifier, 1, Integer.MAX_VALUE);
         };
         this.description = Component.m_237115_("description.pointblank.extendsAmmoCapacity").m_7220_(Component.m_237113_(String.format(" %d%%", (ammoCapacityModifier - 1) * 100)));
         return this;
      }

      public Builder withAmmoCapacity(int ammoCapacity) {
         this.ammoCapacityTransformer = (ammo) -> {
            return Mth.m_14045_(ammoCapacity, 1, Integer.MAX_VALUE);
         };
         this.description = Component.m_237115_("description.pointblank.changesAmmoCapacity").m_7220_(Component.m_237113_(" " + ammoCapacity));
         return this;
      }

      public Builder withJsonObject(JsonObject obj) {
         if (obj.has("condition")) {
            this.withCondition(Conditions.fromJson(obj.getAsJsonObject("condition")));
         }

         if (obj.has("ammoCapacityModifier")) {
            this.withAmmoCapacityModifier(JsonUtil.getJsonInt(obj, "ammoCapacityModifier"));
         } else if (obj.has("ammoCapacity")) {
            this.withAmmoCapacity(JsonUtil.getJsonInt(obj, "ammoCapacity"));
         }

         return this;
      }

      public AmmoCapacityFeature build(FeatureProvider featureProvider) {
         if (this.ammoCapacityTransformer == null) {
            throw new IllegalStateException("Either ammoCapacity ammoCapacityModifier must be set");
         } else {
            return new AmmoCapacityFeature(featureProvider, this.condition, this.ammoCapacityTransformer, this.description);
         }
      }
   }
}
