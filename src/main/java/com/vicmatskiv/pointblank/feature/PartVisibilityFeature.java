package com.vicmatskiv.pointblank.feature;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.vicmatskiv.pointblank.util.Conditions;
import com.vicmatskiv.pointblank.util.JsonUtil;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.world.level.ItemLike;

public class PartVisibilityFeature implements Feature {
   private FeatureProvider owner;
   private Map<String, Predicate<ConditionContext>> predicates = new HashMap();

   private PartVisibilityFeature(FeatureProvider owner, Map<String, Predicate<ConditionContext>> partPredicates) {
      this.owner = owner;
      this.predicates = Collections.unmodifiableMap(partPredicates);
   }

   public FeatureProvider getOwner() {
      return this.owner;
   }

   public boolean isPartVisible(ItemLike partOwner, String partName, ConditionContext conditionContext) {
      if (partOwner != this.owner) {
         return true;
      } else {
         Predicate<ConditionContext> bonePredicate = (Predicate)this.predicates.get(partName);
         if (bonePredicate == null) {
            return true;
         } else {
            boolean result = bonePredicate.test(conditionContext);
            if (partName.contains("muzzle")) {
            }

            return result;
         }
      }
   }

   public static class Builder implements FeatureBuilder<Builder, PartVisibilityFeature> {
      private Map<String, Predicate<ConditionContext>> partPredicates = new HashMap();

      public Builder withShownPart(String partName, Predicate<ConditionContext> condition) {
         if (this.partPredicates.put(partName, condition) != null) {
            throw new IllegalArgumentException("Duplicate part: " + partName);
         } else {
            return this;
         }
      }

      public Builder withHiddenPart(String partName, Predicate<ConditionContext> condition) {
         if (this.partPredicates.put(partName, condition.negate()) != null) {
            throw new IllegalArgumentException("Duplicate part: " + partName);
         } else {
            return this;
         }
      }

      public Builder withJsonObject(JsonObject obj) {
         Iterator var2 = JsonUtil.getJsonObjects(obj, "parts").iterator();

         while(var2.hasNext()) {
            JsonObject partObj = (JsonObject)var2.next();
            String partName = JsonUtil.getJsonString(partObj, "name");
            boolean isVisible = JsonUtil.getJsonBoolean(partObj, "visible", true);
            JsonElement conditionObj = partObj.get("condition");
            Predicate<ConditionContext> condition = Conditions.fromJson(conditionObj);
            this.withShownPart(partName, isVisible ? condition : condition.negate());
         }

         return this;
      }

      public PartVisibilityFeature build(FeatureProvider featureProvider) {
         return new PartVisibilityFeature(featureProvider, this.partPredicates);
      }
   }
}
