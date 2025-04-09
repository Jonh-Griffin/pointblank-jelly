package com.vicmatskiv.pointblank.feature;

import com.google.gson.JsonObject;
import com.vicmatskiv.pointblank.util.Conditions;
import com.vicmatskiv.pointblank.util.JsonUtil;
import java.util.function.Predicate;

import groovy.lang.Script;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class SkinFeature extends ConditionalFeature {
   private ResourceLocation texture;
   private final @Nullable Script script;

   private SkinFeature(FeatureProvider owner, Predicate<ConditionContext> predicate, ResourceLocation texture, Script script) {
      super(owner, predicate);
      this.texture = texture;
      this.script = script;
   }

   public MutableComponent getDescription() {
      return Component.literal("Changes skin");
   }

   public static ResourceLocation getTexture(ItemStack itemStack) {
      Features.EnabledFeature enabledSkinTexture = Features.getFirstEnabledFeature(itemStack, SkinFeature.class);
      if(enabledSkinTexture != null && enabledSkinTexture.feature() instanceof SkinFeature feature) {
         if(feature.hasFunction("getSkinTexture"))
            return (ResourceLocation) feature.invokeFunction("getSkinTexture", itemStack, feature);

         return feature.texture;
      }
      return null;
   }

   @Override
   public @Nullable Script getScript() {
      return script;
   }

   public static class Builder implements FeatureBuilder<Builder, SkinFeature> {
      private Predicate<ConditionContext> condition = (ctx) -> true;
      private ResourceLocation skinResource;
      private Script script;

      public Builder withCondition(Predicate<ConditionContext> condition) {
         this.condition = condition;
         return this;
      }

      public Builder withTexture(String texture) {
         this.skinResource = new ResourceLocation("pointblank", texture);
         return this;
      }

      public Builder withScript(Script script) {
         this.script = script;
         return this;
      }

      public Builder withJsonObject(JsonObject obj) {
         if (obj.has("condition")) {
            this.withCondition(Conditions.fromJson(obj.getAsJsonObject("condition")));
         }
         this.withScript(JsonUtil.getJsonScript(obj));
         this.withTexture(JsonUtil.getJsonString(obj, "texture"));
         return this;
      }

      public SkinFeature build(FeatureProvider featureProvider) {
         return new SkinFeature(featureProvider, this.condition, this.skinResource, this.script);
      }
   }
}
