package com.vicmatskiv.pointblank.feature;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.vicmatskiv.pointblank.util.Conditions;
import com.vicmatskiv.pointblank.util.JsonUtil;

import java.util.HashMap;
import java.util.Map;
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
   private Map<String , ResourceLocation> textures;

   private SkinFeature(FeatureProvider owner, Predicate<ConditionContext> predicate, ResourceLocation texture, Script script, Map<String, ResourceLocation> textures) {
      super(owner, predicate);
      this.texture = texture;
      this.script = script;
      this.textures = textures;
   }

   public MutableComponent getDescription() {
      return Component.literal("Changes skin");
   }

   public static ResourceLocation getTexture(ItemStack itemStack) {
      Features.EnabledFeature enabledSkinTexture = Features.getFirstEnabledFeature(itemStack, SkinFeature.class);
      if(enabledSkinTexture != null && enabledSkinTexture.feature() instanceof SkinFeature feature) {
         if(feature.hasFunction("getSkinTexture"))
            return (ResourceLocation) feature.invokeFunction("getSkinTexture", itemStack, feature);
         if(feature.textures != null) {
            String gunId = itemStack.getItem().toString();
            if (feature.textures.containsKey(gunId)) {
               return feature.textures.get(gunId);
            }
         }
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
      private Map<String, ResourceLocation> textures;

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

      public Builder withTextures(JsonArray tArr) {
            this.textures = new HashMap<>();
            for (int i = 0; i < tArr.size(); i++) {
                JsonObject obj = tArr.get(i).getAsJsonObject();
                String gunId = obj.get("id").getAsString();
                String texture = obj.get("texture").getAsString();
                this.textures.put(gunId, new ResourceLocation("pointblank", texture));
            }
            return this;
      }

      public Builder withJsonObject(JsonObject obj) {
         if (obj.has("condition")) {
            this.withCondition(Conditions.fromJson(obj.getAsJsonObject("condition")));
         }
         this.withScript(JsonUtil.getJsonScript(obj));
         this.withTexture(JsonUtil.getJsonString(obj, "texture"));
         if(obj.has("items")) {
            this.withTextures(obj.getAsJsonArray("items"));
         }
         return this;
      }

      public SkinFeature build(FeatureProvider featureProvider) {
         return new SkinFeature(featureProvider, this.condition, this.skinResource, this.script, this.textures);
      }
   }
}
