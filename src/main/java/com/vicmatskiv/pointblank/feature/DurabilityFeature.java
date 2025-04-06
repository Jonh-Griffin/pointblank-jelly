package com.vicmatskiv.pointblank.feature;

import com.google.gson.JsonObject;
import com.vicmatskiv.pointblank.item.GunItem;
import com.vicmatskiv.pointblank.util.Conditions;
import com.vicmatskiv.pointblank.util.JsonUtil;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public class DurabilityFeature extends ConditionalFeature {
   private static final double MIN_DEGRADATION_RATE = 1.0E-7D;
   private static final double MAX_DEGRADATION_RATE = 1.0D;
   private static final int MIN_DURABILITY = 1;
   private static final int MAX_DURABILITY = Integer.MAX_VALUE;
   private double degradationMultiplier;
   private int durability;

   private DurabilityFeature(FeatureProvider owner, Predicate<ConditionContext> predicate, int durability, double hitScanDamageModifier) {
      super(owner, predicate);
      this.durability = durability;
      this.degradationMultiplier = hitScanDamageModifier;
   }

   public MutableComponent getDescription() {
      return this.degradationMultiplier < 1.0D ? Component.m_237115_("description.pointblank.reducesDegradation").m_7220_(Component.m_237113_(String.format(" %.0f%%", 100.0D * (1.0D - this.degradationMultiplier)))) : Component.m_237115_("description.pointblank.increasesDegradation").m_7220_(Component.m_237113_(String.format(" %.0f%%", 100.0D * (this.degradationMultiplier - 1.0D))));
   }

   public static void ensureDurability(ItemStack itemStack) {
      if (itemStack.m_41720_() instanceof GunItem) {
         int baseMaxDurability = 0;
         double baseDegradationRate = 1.0D;
         int combinedMaxDurability = baseMaxDurability;
         double combinedDegradationRate = baseDegradationRate;
         List<Features.EnabledFeature> enabledDurabilityFeatures = Features.getEnabledFeatures(itemStack, DurabilityFeature.class);

         DurabilityFeature durabilityFeature;
         for(Iterator var8 = enabledDurabilityFeatures.iterator(); var8.hasNext(); combinedDegradationRate *= durabilityFeature.degradationMultiplier) {
            Features.EnabledFeature enabledFeature = (Features.EnabledFeature)var8.next();
            durabilityFeature = (DurabilityFeature)enabledFeature.feature();
            combinedMaxDurability += durabilityFeature.durability;
         }

         CompoundTag tag = itemStack.m_41783_();
         if (tag != null) {
            int currentMaxDurability = tag.m_128451_("mdu");
            if (currentMaxDurability == 0) {
               currentMaxDurability = combinedMaxDurability;
               tag.m_128405_("mdu", combinedMaxDurability);
            }

            if (!tag.m_128441_("du")) {
               tag.m_128405_("du", combinedMaxDurability);
            }

            int currentDurability = tag.m_128451_("du");
            if (currentMaxDurability != combinedMaxDurability) {
               tag.m_128405_("mdu", Mth.m_14045_(combinedMaxDurability, 1, Integer.MAX_VALUE));
               int durabilityDiff = currentMaxDurability - combinedMaxDurability;
               float diffRate = (float)durabilityDiff / (float)currentMaxDurability;
               currentDurability = (int)((float)currentDurability * Mth.m_14036_(diffRate, 0.0F, 2.14748365E9F));
               tag.m_128405_("du", currentDurability);
            }

            tag.m_128347_("dr", combinedDegradationRate);
         }

      }
   }

   public static void degradeDurability(ItemStack itemStack) {
      CompoundTag tag = itemStack.m_41783_();
      if (tag != null) {
         int currentMaxDurability = tag.m_128451_("mdu");
         int durability = tag.m_128451_("du");
         double degradationRate = tag.m_128459_("dr");
         durability = (int)Mth.m_14008_((double)durability * (1.0D - degradationRate), 0.0D, (double)currentMaxDurability);
         tag.m_128405_("du", durability);
      }

   }

   public static float getRelativeDurability(ItemStack itemStack) {
      if (!(itemStack.m_41720_() instanceof GunItem)) {
         return 0.0F;
      } else {
         CompoundTag tag = itemStack.m_41783_();
         if (tag == null) {
            return 0.0F;
         } else {
            int currentMaxDurability = tag.m_128451_("mdu");
            if (currentMaxDurability == 0) {
               return 0.0F;
            } else {
               float durability = (float)tag.m_128451_("du");
               return durability / (float)currentMaxDurability;
            }
         }
      }
   }

   public static class Builder implements FeatureBuilder<Builder, DurabilityFeature> {
      private Predicate<ConditionContext> condition = (ctx) -> {
         return true;
      };
      private double degradationMultiplier;
      private int durability;

      public Builder withCondition(Predicate<ConditionContext> condition) {
         this.condition = condition;
         return this;
      }

      public Builder withDurability(int durability) {
         this.durability = durability;
         return this;
      }

      public Builder withDegradationMultiplier(double degradationMultiplier) {
         this.degradationMultiplier = degradationMultiplier;
         return this;
      }

      public Builder withJsonObject(JsonObject obj) {
         if (obj.has("condition")) {
            this.withCondition(Conditions.fromJson(obj.getAsJsonObject("condition")));
         }

         this.withDegradationMultiplier((double)JsonUtil.getJsonFloat(obj, "degradationMultiplier"));
         return this;
      }

      public DurabilityFeature build(FeatureProvider featureProvider) {
         return new DurabilityFeature(featureProvider, this.condition, Mth.m_14045_(this.durability, 1, Integer.MAX_VALUE), Mth.m_14008_(this.degradationMultiplier, 1.0E-7D, 1.0D));
      }
   }
}
