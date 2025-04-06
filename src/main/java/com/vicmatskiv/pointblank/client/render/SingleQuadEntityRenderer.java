package com.vicmatskiv.pointblank.client.render;

import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.vicmatskiv.pointblank.entity.SlowProjectile;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector4f;

@OnlyIn(Dist.CLIENT)
public class SingleQuadEntityRenderer extends EntityRenderer<SlowProjectile> {
   private ResourceLocation texture = new ResourceLocation("pointblank:textures/effect/laser3.png");

   public SingleQuadEntityRenderer(Context context) {
      super(context);
   }

   public void render(SlowProjectile projectile, float p_114657_, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int lightColor) {
      this.renderOrig(projectile, p_114657_, partialTick, poseStack, bufferSource, lightColor);
   }

   public void renderOrig(SlowProjectile projectile, float p_114657_, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int lightColor) {
      Minecraft mc = Minecraft.m_91087_();
      Camera camera = mc.f_91063_.m_109153_();
      poseStack.m_252781_(camera.m_253121_());
      float width = 0.5F;
      float length = 0.0F;
      poseStack.m_85836_();
      float u = 1.0F;
      float brightness = 1.0F;
      BufferBuilder bufferbuilder = Tesselator.m_85913_().m_85915_();
      RenderSystem.setShaderTexture(0, this.texture);
      RenderSystem.setShader(GameRenderer::m_172820_);
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
      RenderSystem.disableCull();
      RenderSystem.disableDepthTest();
      Matrix4f matrix4f = poseStack.m_85850_().m_252922_();
      Vector4f t1 = new Vector4f(width, width, 0.0F, 1.0F);
      matrix4f.transform(t1);
      bufferbuilder.m_166779_(Mode.QUADS, DefaultVertexFormat.f_85819_);
      bufferbuilder.m_252986_(matrix4f, -width + length, width, 0.0F).m_7421_(0.0F, 0.0F).m_85950_(1.0F, 1.0F, 1.0F, brightness).m_5752_();
      bufferbuilder.m_252986_(matrix4f, width, width, 0.0F).m_7421_(u, 0.0F).m_85950_(1.0F, 1.0F, 1.0F, brightness).m_5752_();
      bufferbuilder.m_252986_(matrix4f, width, -width, 0.0F).m_7421_(u, 1.0F).m_85950_(1.0F, 1.0F, 1.0F, brightness).m_5752_();
      bufferbuilder.m_252986_(matrix4f, -width + length, -width, 0.0F).m_7421_(0.0F, 1.0F).m_85950_(1.0F, 1.0F, 1.0F, brightness).m_5752_();
      BufferUploader.m_231202_(bufferbuilder.m_231175_());
      RenderSystem.disableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.enableCull();
      RenderSystem.enableDepthTest();
      poseStack.m_85849_();
   }

   public ResourceLocation getTextureLocation(SlowProjectile entity) {
      return this.texture;
   }
}
