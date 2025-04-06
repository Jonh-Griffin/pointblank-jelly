package com.vicmatskiv.pointblank.feature;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.vicmatskiv.pointblank.attachment.AttachmentCategory;
import com.vicmatskiv.pointblank.attachment.AttachmentModelInfo;
import com.vicmatskiv.pointblank.attachment.Attachments;
import com.vicmatskiv.pointblank.client.GunStateListener;
import com.vicmatskiv.pointblank.item.GunItem;
import com.vicmatskiv.pointblank.util.Conditions;
import com.vicmatskiv.pointblank.util.JsonUtil;
import java.util.NavigableMap;
import java.util.function.Predicate;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

public final class AimingFeature extends ConditionalFeature implements GunStateListener {
   private static final Matrix4f IDENTITY_MATRIX = new Matrix4f();
   private final float zoom;
   private final float viewBobbing;

   @OnlyIn(Dist.CLIENT)
   public static void applyAimingPosition(ItemStack itemStack, PoseStack poseStack, float rescale, float aimingProgress) {
      if (itemStack.m_41720_() instanceof GunItem) {
         if (aimingProgress > 0.0F) {
            NavigableMap<String, Pair<ItemStack, Matrix4f>> poseMatrices = AttachmentModelInfo.findInverseBoneMatrices(itemStack, "scope", rescale);
            if (!poseMatrices.isEmpty()) {
               Features.EnabledFeature aimingFeature = Features.getFirstEnabledFeature(itemStack, AimingFeature.class);
               Pair<ItemStack, Matrix4f> attachmentPos = null;
               if (aimingFeature != null) {
                  attachmentPos = (Pair)poseMatrices.get(aimingFeature.ownerPath());
               }

               if (attachmentPos == null) {
                  attachmentPos = (Pair)poseMatrices.firstEntry().getValue();
               }

               if (attachmentPos == null) {
                  return;
               }

               if (aimingProgress < 1.0F) {
                  poseStack.m_252931_(AimSwitchAnimation.INSTANCE.update(IDENTITY_MATRIX, (Matrix4f)attachmentPos.getSecond(), aimingProgress));
               } else {
                  poseStack.m_252931_(AimSwitchAnimation.INSTANCE.update((Matrix4f)attachmentPos.getSecond()));
               }
            }

            poseStack.m_252880_(0.0F, aimingProgress * -0.6095F * rescale, aimingProgress * -0.7F * rescale);
         }

      }
   }

   private AimingFeature(FeatureProvider owner, Predicate<ConditionContext> predicate, float zoom, float viewBobbing) {
      super(owner, predicate);
      this.zoom = zoom;
      this.viewBobbing = viewBobbing;
   }

   public MutableComponent getDescription() {
      return Component.m_237115_("description.pointblank.enablesAimingWithZoom").m_7220_(Component.m_237113_(String.format(" %.0f%%", this.zoom * 100.0F)));
   }

   public float getZoom() {
      return this.zoom;
   }

   public float getViewBobbing() {
      return this.viewBobbing;
   }

   public static float getZoom(ItemStack itemStack) {
      Pair<String, ItemStack> selected = Attachments.getSelectedAttachment(itemStack, AttachmentCategory.SCOPE);
      ItemStack selectedStack = null;
      if (selected != null) {
         selectedStack = (ItemStack)selected.getSecond();
      } else {
         selectedStack = itemStack;
      }

      Item var5 = selectedStack.m_41720_();
      if (var5 instanceof FeatureProvider) {
         FeatureProvider fp = (FeatureProvider)var5;
         AimingFeature feature = (AimingFeature)fp.getFeature(AimingFeature.class);
         if (feature != null) {
            return feature.getZoom();
         }
      } else {
         var5 = selectedStack.m_41720_();
         if (var5 instanceof GunItem) {
            GunItem gunItem = (GunItem)var5;
            return (float)gunItem.getAimingZoom();
         }
      }

      return 0.0F;
   }

   public static float getViewBobbing(ItemStack itemStack) {
      Pair<String, ItemStack> selected = Attachments.getSelectedAttachment(itemStack, AttachmentCategory.SCOPE);
      ItemStack selectedStack = null;
      if (selected != null) {
         selectedStack = (ItemStack)selected.getSecond();
      } else {
         selectedStack = itemStack;
      }

      Item var4 = selectedStack.m_41720_();
      if (var4 instanceof FeatureProvider) {
         FeatureProvider fp = (FeatureProvider)var4;
         AimingFeature feature = (AimingFeature)fp.getFeature(AimingFeature.class);
         if (feature != null) {
            return feature.getViewBobbing();
         }
      }

      return 1.0F;
   }

   @OnlyIn(Dist.CLIENT)
   private static class AimSwitchAnimation {
      private static final AimSwitchAnimation INSTANCE = new AimSwitchAnimation(200L);
      protected long startTime;
      protected long nanoDuration;
      protected boolean isDone;
      protected Matrix4f fromMatrix;
      protected Matrix4f toMatrix;

      protected AimSwitchAnimation(long durationMillis) {
         this.nanoDuration = durationMillis * 1000000L;
         this.fromMatrix = AimingFeature.IDENTITY_MATRIX;
         this.toMatrix = AimingFeature.IDENTITY_MATRIX;
      }

      protected float getProgress() {
         double progress = (double)(System.nanoTime() - this.startTime) / (double)this.nanoDuration;
         if (progress > 1.0D) {
            progress = 1.0D;
         }

         return Mth.m_14036_((float)progress, 0.0F, 1.0F);
      }

      public void reset() {
         this.isDone = false;
         this.startTime = System.nanoTime();
      }

      public Matrix4f update(Matrix4f matrix) {
         if (this.toMatrix != matrix) {
            this.fromMatrix = this.toMatrix;
            this.toMatrix = matrix;
            this.reset();
         }

         float progress = this.getProgress();
         if (progress >= 1.0F) {
            this.isDone = true;
         }

         return this.isDone ? this.toMatrix : (new Matrix4f(this.fromMatrix)).lerp(this.toMatrix, progress);
      }

      public Matrix4f update(Matrix4f fromMatrix, Matrix4f toMatrix, float progress) {
         Matrix4f resultMatrix = (new Matrix4f(fromMatrix)).lerp(toMatrix, progress);
         INSTANCE.fromMatrix = fromMatrix;
         INSTANCE.toMatrix = toMatrix;
         return resultMatrix;
      }
   }

   public static class Builder implements FeatureBuilder<Builder, AimingFeature> {
      private static final float DEFAULT_ZOOM = 0.1F;
      private Predicate<ConditionContext> condition = (ctx) -> {
         return true;
      };
      private float zoom;
      private float viewBobbing = 1.0F;

      public Builder withCondition(Predicate<ConditionContext> condition) {
         this.condition = condition;
         return this;
      }

      public Builder withZoom(double zoom) {
         this.zoom = (float)zoom;
         return this;
      }

      public Builder withViewBobbing(double viewBobbing) {
         this.viewBobbing = Mth.m_14036_((float)viewBobbing, 0.0F, 1.0F);
         return this;
      }

      public Builder withJsonObject(JsonObject obj) {
         if (obj.has("condition")) {
            this.withCondition(Conditions.fromJson(obj.getAsJsonObject("condition")));
         }

         this.withZoom((double)JsonUtil.getJsonFloat(obj, "zoom", 0.1F));
         this.withViewBobbing((double)JsonUtil.getJsonFloat(obj, "viewBobbing", 1.0F));
         return this;
      }

      public AimingFeature build(FeatureProvider featureProvider) {
         return new AimingFeature(featureProvider, this.condition, this.zoom, this.viewBobbing);
      }
   }
}
