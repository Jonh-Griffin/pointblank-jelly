package com.vicmatskiv.pointblank.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.vicmatskiv.pointblank.compat.iris.IrisCompat;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;

public interface RenderTypeProvider {
   RenderType NO_RENDER_TYPE = new RenderType((String)null, (VertexFormat)null, (Mode)null, 0, false, false, (Runnable)null, (Runnable)null) {
   };
   AtomicInteger wrappedCounter = new AtomicInteger();
   Runnable SETUP_STENCIL_MASK_RENDER = () -> {
      GL11.glEnable(2960);
      RenderSystem.clear(1024, Minecraft.f_91002_);
      RenderSystem.clearStencil(0);
      RenderSystem.stencilMask(255);
      RenderSystem.colorMask(false, false, false, false);
      RenderSystem.depthMask(false);
      RenderSystem.stencilFunc(519, 1, 255);
      RenderSystem.stencilOp(7680, 7680, 7681);
   };
   Runnable CLEAR_STENCIL_MASK_RENDER = () -> {
      RenderSystem.depthMask(true);
      RenderSystem.colorMask(true, true, true, true);
      RenderSystem.stencilMask(0);
      GL11.glDisable(2960);
   };
   Runnable SETUP_STENCIL_RENDER = () -> {
      GL11.glEnable(2960);
      RenderSystem.stencilOp(7680, 7680, 7680);
      GL11.glStencilFunc(514, 1, 255);
   };
   Runnable CLEAR_STENCIL_RENDER = () -> {
      GL11.glStencilMask(255);
      GL11.glDisable(2960);
   };

   static RenderTypeProvider getInstance() {
      IrisCompat irisCompat = IrisCompat.getInstance();
      return (RenderTypeProvider)(irisCompat.isShaderPackEnabled() ? irisCompat.getRenderTypeProvider() : DefaultRenderTypeProvider.getInstance());
   }

   RenderType getPipRenderType(boolean var1);

   RenderType getPipOverlayRenderType(ResourceLocation var1, boolean var2);

   RenderType getPipMaskRenderType(ResourceLocation var1);

   RenderType getGlowRenderType(ResourceLocation var1);

   RenderType getMuzzleFlashRenderType(ResourceLocation var1);

   RenderType getReticleRenderType(ResourceLocation var1, boolean var2);

   RenderType getGlowBlockEntityRenderType(ResourceLocation var1);

   static RenderType wrapRenderType(RenderType renderType, Runnable setupRenderState, Runnable clearRenderState) {
      return new RenderType("pointblank:" + renderType + ":" + wrappedCounter.incrementAndGet(), renderType.m_110508_(), renderType.m_173186_(), renderType.m_110507_(), renderType.m_110405_(), false, () -> {
         renderType.m_110185_();
         setupRenderState.run();
      }, () -> {
         clearRenderState.run();
         renderType.m_110188_();
      }) {
      };
   }

   default MultiBufferSource wrapBufferSource(MultiBufferSource source) {
      return (renderType) -> {
         return renderType == NO_RENDER_TYPE ? null : source.m_6299_(renderType);
      };
   }

   default float getReticleBrightness() {
      return 1.0F;
   }

   default float getGlowBrightness() {
      return 1.0F;
   }

   public static enum Key {
      MUZZLE_FLASH,
      PIP,
      PIP_OVERLAY,
      RETICLE;

      // $FF: synthetic method
      private static Key[] $values() {
         return new Key[]{MUZZLE_FLASH, PIP, PIP_OVERLAY, RETICLE};
      }
   }
}
