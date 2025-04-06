package com.vicmatskiv.pointblank.client.render.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.vicmatskiv.pointblank.client.render.Flushable;
import com.vicmatskiv.pointblank.client.render.GunItemRenderer;
import com.vicmatskiv.pointblank.client.render.HierarchicalRenderContext;
import com.vicmatskiv.pointblank.client.render.RenderPass;
import com.vicmatskiv.pointblank.client.render.RenderTypeProvider;
import com.vicmatskiv.pointblank.feature.PipFeature;
import com.vicmatskiv.pointblank.item.GunItem;
import java.util.Collections;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import software.bernie.geckolib.cache.object.BakedGeoModel;

public class PipItemLayer extends FeaturePassLayer<GunItem> {
   private RenderPass currentRenderPass;

   public PipItemLayer(GunItemRenderer renderer) {
      super(renderer, PipFeature.class, RenderPass.PIP, Collections.singleton("scopepip"), true, (Object)null);
      this.currentRenderPass = RenderPass.PIP;
   }

   public RenderType getRenderType() {
      return RenderTypeProvider.NO_RENDER_TYPE;
   }

   public RenderPass getRenderPass() {
      return this.currentRenderPass;
   }

   public void render(BakedGeoModel attachmentModel, PoseStack poseStack, MultiBufferSource bufferSource, GunItem animatable, RenderType renderType, VertexConsumer ignoredBuffer, float partialTick, int packedLight, int overlay, float red, float green, float blue, float alpha) {
      PipFeature pipFeature = PipFeature.getSelected(HierarchicalRenderContext.getRootItemStack());
      if (pipFeature != null) {
         this.currentRenderPass = RenderPass.PIP;
         ResourceLocation maskTexture = pipFeature.getMaskTexture();
         ResourceLocation overlayTexture = pipFeature.getOverlayTexture();
         boolean isParallaxEnabled = pipFeature.isParallaxEnabled();
         RenderType maskRenderType;
         if (maskTexture != null) {
            RenderPass.push(RenderPass.PIP_MASK);
            this.currentRenderPass = RenderPass.PIP_MASK;

            try {
               maskRenderType = RenderTypeProvider.getInstance().getPipMaskRenderType(maskTexture);
               VertexConsumer maskBuffer = bufferSource.m_6299_(maskRenderType);
               super.render(attachmentModel, poseStack, bufferSource, animatable, maskRenderType, maskBuffer, partialTick, packedLight, overlay, red, green, blue, alpha);
               if (bufferSource instanceof Flushable) {
                  Flushable flushable = (Flushable)bufferSource;
                  flushable.flush();
               }
            } finally {
               RenderPass.pop();
               this.currentRenderPass = RenderPass.PIP;
            }
         }

         maskRenderType = RenderTypeProvider.getInstance().getPipRenderType(maskTexture != null);
         super.render(attachmentModel, poseStack, bufferSource, animatable, maskRenderType, bufferSource.m_6299_(maskRenderType), partialTick, packedLight, overlay, red, green, blue, alpha);
         if (bufferSource instanceof Flushable) {
            Flushable flushable = (Flushable)bufferSource;
            flushable.flush();
         }

         if (overlayTexture != null) {
            HierarchicalRenderContext subHrc = HierarchicalRenderContext.push();

            try {
               subHrc.setAttribute("is_parallax_enabled", isParallaxEnabled);
               RenderPass.push(RenderPass.PIP_OVERLAY);
               this.currentRenderPass = RenderPass.PIP_OVERLAY;

               try {
                  RenderType overlayRenderType = RenderTypeProvider.getInstance().getPipOverlayRenderType(overlayTexture, maskTexture != null);
                  VertexConsumer overlayBuffer = bufferSource.m_6299_(overlayRenderType);
                  super.render(attachmentModel, poseStack, bufferSource, animatable, overlayRenderType, overlayBuffer, partialTick, packedLight, overlay, red, green, blue, alpha);
                  if (bufferSource instanceof Flushable) {
                     Flushable flushable = (Flushable)bufferSource;
                     flushable.flush();
                  }
               } finally {
                  RenderPass.pop();
                  this.currentRenderPass = RenderPass.PIP;
               }
            } catch (Throwable var35) {
               if (subHrc != null) {
                  try {
                     subHrc.close();
                  } catch (Throwable var32) {
                     var35.addSuppressed(var32);
                  }
               }

               throw var35;
            }

            if (subHrc != null) {
               subHrc.close();
            }
         }

      }
   }

   public boolean isSupportedItemDisplayContext(ItemDisplayContext context) {
      return context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
   }

   public static boolean isParallaxEnabled() {
      HierarchicalRenderContext current = HierarchicalRenderContext.current();
      Boolean isParallaxEnabled = (Boolean)current.getAttribute("is_parallax_enabled");
      return isParallaxEnabled != null && isParallaxEnabled;
   }
}
