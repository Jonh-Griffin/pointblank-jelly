package com.vicmatskiv.pointblank.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.vicmatskiv.pointblank.Config;
import com.vicmatskiv.pointblank.client.ClientSystem;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;

public class RenderUtil {
   private static final double FOV_NORMAL_DEGREES = 70.0D;
   private static Matrix4f projectionMatrix;
   private static Matrix4f projectionMatrixInverted;
   private static Matrix4f modelViewMatrix;
   private static Matrix4f modelViewMatrixInverted;

   public static void renderQuad(PoseStack poseStack, GeoQuad quad, VertexConsumer buffer, float dx, float dy, float red, float green, float blue, float alpha) {
      Matrix4f poseState = poseStack.m_85850_().m_252922_();
      float[][] texUV = new float[][]{{1.0F + dx, 0.0F + dy}, {0.0F + dx, 0.0F + dy}, {0.0F + dx, 1.0F + dy}, {1.0F + dx, 1.0F + dy}};

      for(int i = 0; i < 4; ++i) {
         GeoVertex vertex = quad.vertices()[i];
         float u = Mth.m_14036_(texUV[i][0], 0.0F, 1.0F);
         float v = Mth.m_14036_(texUV[i][1], 0.0F, 1.0F);
         buffer.m_252986_(poseState, vertex.position().x, vertex.position().y, vertex.position().z).m_7421_(u, v).m_85950_(red, green, blue, alpha).m_5752_();
      }

   }

   public static void renderQuadColorTex(PoseStack poseStack, GeoQuad quad, VertexConsumer buffer, float dx, float dy, float red, float green, float blue, float alpha) {
      Matrix4f poseState = poseStack.m_85850_().m_252922_();
      float[][] texUV = new float[][]{{1.0F + dx, 0.0F + dy}, {0.0F + dx, 0.0F + dy}, {0.0F + dx, 1.0F + dy}, {1.0F + dx, 1.0F + dy}};

      for(int i = 0; i < 4; ++i) {
         GeoVertex vertex = quad.vertices()[i];
         float u = Mth.m_14036_(texUV[i][0], 0.0F, 1.0F);
         float v = Mth.m_14036_(texUV[i][1], 0.0F, 1.0F);
         buffer.m_252986_(poseState, vertex.position().x, vertex.position().y, vertex.position().z).m_85950_(red, green, blue, alpha).m_7421_(u, v).m_86008_(655360).m_85969_(15728640).m_5601_(1.0F, 1.0F, 1.0F).m_5752_();
      }

   }

   public static void renderColoredQuad(PoseStack poseStack, GeoQuad quad, VertexConsumer buffer, float red, float green, float blue, float alpha) {
      Matrix4f poseState = poseStack.m_85850_().m_252922_();

      for(int i = 0; i < 4; ++i) {
         GeoVertex v = quad.vertices()[i];
         buffer.m_252986_(poseState, v.position().x, v.position().y, v.position().z).m_85950_(red, green, blue, alpha).m_5752_();
      }

   }

   public static void blit(GuiGraphics guiGraphics, ResourceLocation textureResource, float posX, float posY, int zLevel, float minU, float minV, int width, int height, int actualWidth, int actualHeight) {
      blit(guiGraphics, textureResource, posX, posX + (float)width, posY, posY + (float)height, zLevel, width, height, minU, minV, actualWidth, actualHeight);
   }

   public static void blit(GuiGraphics guiGraphics, ResourceLocation textureResource, float posXStart, float posXEnd, float posYStart, float posYEnd, int zLevel, int width, int height, float minU, float minV, int actualWidth, int actualHeight) {
      blit(guiGraphics, textureResource, posXStart, posXEnd, posYStart, posYEnd, (float)zLevel, (minU + 0.0F) / (float)actualWidth, (minU + (float)width) / (float)actualWidth, (minV + 0.0F) / (float)actualHeight, (minV + (float)height) / (float)actualHeight);
   }

   public static void blit(GuiGraphics guiGraphics, ResourceLocation textureResource, float posXStart, float posXEnd, float posYStart, float posYEnd, float zLevel, float minU, float maxU, float minV, float maxV) {
      RenderSystem.setShaderTexture(0, textureResource);
      RenderSystem.setShader(GameRenderer::m_172817_);
      Matrix4f matrix4f = guiGraphics.m_280168_().m_85850_().m_252922_();
      BufferBuilder bufferbuilder = Tesselator.m_85913_().m_85915_();
      bufferbuilder.m_166779_(Mode.QUADS, DefaultVertexFormat.f_85817_);
      bufferbuilder.m_252986_(matrix4f, posXStart, posYStart, zLevel).m_7421_(minU, minV).m_5752_();
      bufferbuilder.m_252986_(matrix4f, posXStart, posYEnd, zLevel).m_7421_(minU, maxV).m_5752_();
      bufferbuilder.m_252986_(matrix4f, posXEnd, posYEnd, zLevel).m_7421_(maxU, maxV).m_5752_();
      bufferbuilder.m_252986_(matrix4f, posXEnd, posYStart, zLevel).m_7421_(maxU, minV).m_5752_();
      BufferUploader.m_231202_(bufferbuilder.m_231175_());
   }

   public static void blit(GuiGraphics guiGraphics, ResourceLocation resourceLocation, int startX, int endX, int startY, int endY, int zLevel, float minU, float maxU, float minV, float maxV, float colorR, float colorG, float colorB, float alpha) {
      RenderSystem.setShaderTexture(0, resourceLocation);
      Supplier var17;
      if (Config.customShadersEnabled) {
         ClientSystem var10000 = ClientSystem.getInstance();
         Objects.requireNonNull(var10000);
         var17 = var10000::getTexColorShaderInstance;
      } else {
         var17 = GameRenderer::m_172820_;
      }

      RenderSystem.setShader(var17);
      RenderSystem.enableBlend();
      Matrix4f matrix4f = guiGraphics.m_280168_().m_85850_().m_252922_();
      BufferBuilder bufferbuilder = Tesselator.m_85913_().m_85915_();
      bufferbuilder.m_166779_(Mode.QUADS, DefaultVertexFormat.f_85819_);
      bufferbuilder.m_252986_(matrix4f, (float)startX, (float)startY, (float)zLevel).m_7421_(minU, minV).m_85950_(colorR, colorG, colorB, alpha).m_5752_();
      bufferbuilder.m_252986_(matrix4f, (float)startX, (float)endY, (float)zLevel).m_7421_(minU, maxV).m_85950_(colorR, colorG, colorB, alpha).m_5752_();
      bufferbuilder.m_252986_(matrix4f, (float)endX, (float)endY, (float)zLevel).m_7421_(maxU, maxV).m_85950_(colorR, colorG, colorB, alpha).m_5752_();
      bufferbuilder.m_252986_(matrix4f, (float)endX, (float)startY, (float)zLevel).m_7421_(maxU, minV).m_85950_(colorR, colorG, colorB, alpha).m_5752_();
      BufferUploader.m_231202_(bufferbuilder.m_231175_());
      RenderSystem.disableBlend();
   }

   public static Matrix4f getProjectionMatrixInverted() {
      Matrix4f projectionMatrixNew = RenderSystem.getProjectionMatrix();
      if (projectionMatrixNew == null || projectionMatrixNew != projectionMatrix) {
         projectionMatrix = projectionMatrixNew;
         projectionMatrixInverted = (new Matrix4f(projectionMatrixNew)).invert();
      }

      return projectionMatrixInverted;
   }

   public static Matrix4f getModelViewMatrixInverted() {
      Matrix4f modelViewMatrixNew = RenderSystem.getModelViewMatrix();
      if (modelViewMatrixNew == null || modelViewMatrixNew != modelViewMatrix) {
         modelViewMatrix = modelViewMatrixNew;
         modelViewMatrixInverted = (new Matrix4f(modelViewMatrixNew)).invert();
      }

      return modelViewMatrixInverted;
   }

   public static Matrix4f getProjectionMatrixNormalFov() {
      Minecraft mc = Minecraft.m_91087_();
      return mc.f_91063_.m_253088_(70.0D);
   }
}
