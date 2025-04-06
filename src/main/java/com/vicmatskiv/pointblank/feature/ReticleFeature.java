package com.vicmatskiv.pointblank.feature;

import com.google.gson.JsonObject;
import com.vicmatskiv.pointblank.util.Conditions;
import com.vicmatskiv.pointblank.util.JsonUtil;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class ReticleFeature extends ConditionalFeature {
   public static final float DEFAULT_MAX_ANGULAR_OFFSET_DEGREES = 5.0F;
   public static final float DEFAULT_MAX_ANGULAR_OFFSET_COS = Mth.m_14089_(0.08726646F);
   private final ResourceLocation texture;
   private final boolean isParallaxEnabled;
   private final float maxAngularOffsetCos;

   private ReticleFeature(FeatureProvider owner, Predicate<ConditionContext> condition, ResourceLocation texture, boolean isParallaxEnabled, float maxAngularOffsetCos) {
      super(owner, condition);
      this.texture = texture;
      this.isParallaxEnabled = isParallaxEnabled;
      this.maxAngularOffsetCos = maxAngularOffsetCos;
   }

   public ResourceLocation getTexture() {
      return this.texture;
   }

   public boolean isParallaxEnabled() {
      return this.isParallaxEnabled;
   }

   public float getMaxAngularOffsetCos() {
      return this.maxAngularOffsetCos;
   }

   public static class Builder implements FeatureBuilder<Builder, ReticleFeature> {
      private Predicate<ConditionContext> condition = (ctx) -> {
         return true;
      };
      private boolean isParallaxEnabled;
      private float maxAngularOffsetCos;
      private ResourceLocation texture;

      public Builder() {
         this.maxAngularOffsetCos = ReticleFeature.DEFAULT_MAX_ANGULAR_OFFSET_COS;
      }

      public Builder withCondition(Predicate<ConditionContext> condition) {
         this.condition = condition;
         return this;
      }

      public Builder withTexture(String texture) {
         this.texture = new ResourceLocation("pointblank", texture);
         return this;
      }

      public Builder withParallaxEnabled(boolean isParallaxEnabled) {
         this.isParallaxEnabled = isParallaxEnabled;
         return this;
      }

      public Builder withMaxAngularOffset(float maxAngularOffsetDegrees) {
         this.maxAngularOffsetCos = Mth.m_14089_(0.017453292F * Mth.m_14036_(maxAngularOffsetDegrees, 0.0F, 45.0F));
         return this;
      }

      public Builder withJsonObject(JsonObject obj) {
         if (obj.has("condition")) {
            this.withCondition(Conditions.fromJson(obj.getAsJsonObject("condition")));
         }

         this.isParallaxEnabled = JsonUtil.getJsonBoolean(obj, "parallax", false);
         if (obj.has("texture")) {
            this.withTexture(JsonUtil.getJsonString(obj, "texture"));
         } else if (this.isParallaxEnabled) {
            this.withTexture("textures/item/reticle4.png");
         }

         this.withMaxAngularOffset(JsonUtil.getJsonFloat(obj, "maxAngularOffset", 5.0F));
         return this;
      }

      public ReticleFeature build(FeatureProvider featureProvider) {
         ResourceLocation texture = this.texture;
         if (texture == null) {
            if (this.isParallaxEnabled) {
               texture = new ResourceLocation("pointblank", "textures/item/reticle4.png");
            } else {
               texture = new ResourceLocation("pointblank", "textures/item/reticle.png");
            }
         }

         return new ReticleFeature(featureProvider, this.condition, texture, this.isParallaxEnabled, this.maxAngularOffsetCos);
      }
   }
}
