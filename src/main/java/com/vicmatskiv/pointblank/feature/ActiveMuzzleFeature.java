package com.vicmatskiv.pointblank.feature;

import com.google.gson.JsonObject;
import com.vicmatskiv.pointblank.client.GunClientState;
import com.vicmatskiv.pointblank.util.Conditions;
import com.vicmatskiv.pointblank.util.JsonUtil;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class ActiveMuzzleFeature extends ConditionalFeature {
   private Map<String, Predicate<ConditionContext>> muzzleParts;

   private ActiveMuzzleFeature(FeatureProvider owner, Predicate<ConditionContext> predicate, Map<String, Predicate<ConditionContext>> muzzleParts) {
      super(owner, predicate);
      this.muzzleParts = muzzleParts;
   }

   public MutableComponent getDescription() {
      return Component.m_237119_();
   }

   public static boolean isActiveMuzzle(ItemStack rootStack, ItemStack currentStack, ItemDisplayContext itemDisplayContext, String partName) {
      Item var5 = currentStack.m_41720_();
      if (var5 instanceof FeatureProvider) {
         FeatureProvider featureProvider = (FeatureProvider)var5;
         ActiveMuzzleFeature feature = (ActiveMuzzleFeature)featureProvider.getFeature(ActiveMuzzleFeature.class);
         if (feature == null) {
            return rootStack == currentStack;
         } else {
            Predicate<ConditionContext> predicate = (Predicate)feature.muzzleParts.get(partName);
            if (predicate == null) {
               predicate = feature.predicate;
            }

            return feature.predicate.test(new ConditionContext((LivingEntity)null, rootStack, currentStack, (GunClientState)null, itemDisplayContext));
         }
      } else {
         return rootStack == currentStack;
      }
   }

   public static class Builder implements FeatureBuilder<Builder, ActiveMuzzleFeature> {
      private Predicate<ConditionContext> condition = (ctx) -> {
         return true;
      };
      private Map<String, Predicate<ConditionContext>> muzzleParts = new HashMap();

      public Builder withCondition(Predicate<ConditionContext> condition) {
         this.condition = condition;
         return this;
      }

      public Builder withPart(String partName, Predicate<ConditionContext> condition) {
         this.muzzleParts.put(partName, condition);
         return this;
      }

      public Builder withJsonObject(JsonObject obj) {
         if (obj.has("condition")) {
            this.withCondition(Conditions.fromJson(obj.getAsJsonObject("condition")));
         }

         String partName;
         Predicate condition;
         for(Iterator var2 = JsonUtil.getJsonObjects(obj, "parts").iterator(); var2.hasNext(); this.withPart(partName, condition)) {
            JsonObject partObj = (JsonObject)var2.next();
            partName = JsonUtil.getJsonString(partObj, "name");
            if (partObj.has("condition")) {
               JsonObject conditionObj = partObj.getAsJsonObject("condition");
               condition = Conditions.fromJson(conditionObj);
            } else {
               condition = (ctx) -> {
                  return true;
               };
            }
         }

         return this;
      }

      public ActiveMuzzleFeature build(FeatureProvider featureProvider) {
         Map<String, Predicate<ConditionContext>> muzzleParts = new HashMap(this.muzzleParts);
         if (muzzleParts.isEmpty()) {
            muzzleParts.put("muzzleflash", this.condition);
            muzzleParts.put("muzzle", this.condition);
         }

         return new ActiveMuzzleFeature(featureProvider, this.condition, Collections.unmodifiableMap(muzzleParts));
      }
   }
}
