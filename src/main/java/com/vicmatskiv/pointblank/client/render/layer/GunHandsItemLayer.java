package com.vicmatskiv.pointblank.client.render.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.vicmatskiv.pointblank.client.render.HierarchicalRenderContext;
import com.vicmatskiv.pointblank.client.render.RenderPass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class GunHandsItemLayer<T extends GeoAnimatable> extends GeoRenderLayer<T> {
   public GunHandsItemLayer(GeoRenderer<T> renderer) {
      super(renderer);
   }

   public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
      Minecraft mc = Minecraft.m_91087_();
      ResourceLocation texture = mc.f_91074_.m_108560_();
      ItemDisplayContext itemDisplayContext = HierarchicalRenderContext.current().getItemDisplayContext();
      if (itemDisplayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || itemDisplayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
         RenderType handRenderType = this.renderer.getRenderType(animatable, texture, bufferSource, Minecraft.m_91087_().m_91296_());
         RenderPass.push(RenderPass.HANDS);

         try {
            poseStack.m_85836_();
            this.getRenderer().reRender(this.getDefaultBakedModel(animatable), poseStack, bufferSource, animatable, handRenderType, bufferSource.m_6299_(handRenderType), partialTick, packedLight, OverlayTexture.f_118083_, 1.0F, 1.0F, 1.0F, 1.0F);
            poseStack.m_85849_();
         } finally {
            RenderPass.pop();
         }
      }

   }
}
