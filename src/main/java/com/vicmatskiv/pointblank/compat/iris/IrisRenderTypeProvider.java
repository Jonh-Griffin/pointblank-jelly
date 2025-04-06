package com.vicmatskiv.pointblank.compat.iris;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.vicmatskiv.pointblank.client.ClientSystem;
import com.vicmatskiv.pointblank.client.render.Flushable;
import com.vicmatskiv.pointblank.client.render.RenderTypeProvider;
import java.io.FileNotFoundException;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import net.irisshaders.batchedentityrendering.impl.FullyBufferedMultiBufferSource;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.RenderStateShard.EmptyTextureStateShard;
import net.minecraft.client.renderer.RenderStateShard.ShaderStateShard;
import net.minecraft.client.renderer.RenderStateShard.TextureStateShard;
import net.minecraft.client.renderer.RenderType.CompositeState;
import net.minecraft.client.renderer.RenderType.CompositeState.CompositeStateBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

public class IrisRenderTypeProvider extends RenderStateShard implements RenderTypeProvider {
   private Function<Boolean, RenderType> pipRenderTypes = Util.m_143827_((isMasked) -> {
      return this.createPipRenderType(isMasked);
   });
   private final Function<ResourceLocation, RenderType> pipMaskRenderTypes = Util.m_143827_((texture) -> {
      return this.createPipMaskRenderType(getIrisTexture(texture), this::getPipMaskShader);
   });
   private final Function<ResourceLocation, RenderType> pipOverlayRenderTypes = Util.m_143827_((texture) -> {
      return createPipOverlayRenderType(getIrisTexture(texture), GameRenderer::m_172820_, false);
   });
   private final Function<ResourceLocation, RenderType> pipOverlayMaskedRenderTypes = Util.m_143827_((texture) -> {
      return createPipOverlayRenderType(getIrisTexture(texture), this::getPipOverlayShader, true);
   });
   private final Function<ResourceLocation, RenderType> reticleRenderTypes = Util.m_143827_((texture) -> {
      return createReticleRenderType(getIrisTexture(texture));
   });
   private final Function<ResourceLocation, RenderType> reticleRenderTypesWithParallax = Util.m_143827_((texture) -> {
      return createReticleRenderType(getIrisTexture(texture), GameRenderer::m_172820_);
   });
   private final Function<ResourceLocation, RenderType> glowRenderTypes = Util.m_143827_((texture) -> {
      return GlowRenderType.createRenderType(getIrisTexture(texture));
   });

   public IrisRenderTypeProvider() {
      super((String)null, (Runnable)null, (Runnable)null);
   }

   public MultiBufferSource wrapBufferSource(MultiBufferSource source) {
      return new WrappedBufferSource(source);
   }

   public RenderType getPipRenderType(boolean isMasked) {
      return (RenderType)this.pipRenderTypes.apply(isMasked);
   }

   private ShaderInstance getPipShader() {
      WorldRenderingPipeline pipeline = (WorldRenderingPipeline)Iris.getPipelineManager().getPipeline().orElse((Object)null);
      ShaderInstance shader = null;
      if (pipeline instanceof IrisAuxShaderProvider) {
         IrisAuxShaderProvider auxShaderProvider = (IrisAuxShaderProvider)pipeline;
         shader = auxShaderProvider.getPointblankAuxShader();
      }

      if (shader == null) {
         shader = GameRenderer.m_172820_();
      }

      return shader;
   }

   private ShaderInstance getPipOverlayShader() {
      WorldRenderingPipeline pipeline = (WorldRenderingPipeline)Iris.getPipelineManager().getPipeline().orElse((Object)null);
      ShaderInstance shader = null;
      if (pipeline instanceof IrisAuxShaderProvider) {
         IrisAuxShaderProvider auxShaderProvider = (IrisAuxShaderProvider)pipeline;
         shader = auxShaderProvider.getPointblankAuxPlainShader();
      }

      if (shader == null) {
         shader = GameRenderer.m_172820_();
      }

      return shader;
   }

   private ShaderInstance getPipMaskShader() {
      WorldRenderingPipeline pipeline = (WorldRenderingPipeline)Iris.getPipelineManager().getPipeline().orElse((Object)null);
      ShaderInstance shader = null;
      if (pipeline instanceof IrisAuxShaderProvider) {
         IrisAuxShaderProvider auxShaderProvider = (IrisAuxShaderProvider)pipeline;
         shader = auxShaderProvider.getPointblankMaskShader();
      }

      if (shader == null) {
         shader = GameRenderer.m_172820_();
      }

      return shader;
   }

   public RenderType getPipOverlayRenderType(ResourceLocation texture, boolean isMasked) {
      return isMasked ? (RenderType)this.pipOverlayMaskedRenderTypes.apply(texture) : (RenderType)this.pipOverlayRenderTypes.apply(texture);
   }

   public RenderType getPipMaskRenderType(ResourceLocation texture) {
      return (RenderType)this.pipMaskRenderTypes.apply(texture);
   }

   public RenderType getGlowRenderType(ResourceLocation texture) {
      return (RenderType)this.glowRenderTypes.apply(texture);
   }

   public RenderType getGlowBlockEntityRenderType(ResourceLocation texture) {
      return RenderType.m_110473_(texture);
   }

   public RenderType getMuzzleFlashRenderType(ResourceLocation texture) {
      return RenderType.m_234338_(texture);
   }

   public RenderType getReticleRenderType(ResourceLocation texture, boolean isParallaxEnabled) {
      return isParallaxEnabled ? (RenderType)this.reticleRenderTypesWithParallax.apply(texture) : (RenderType)this.reticleRenderTypes.apply(texture);
   }

   private static ResourceLocation getIrisTexture(ResourceLocation originalTexture) {
      Minecraft mc = Minecraft.m_91087_();
      ResourceManager resourceManager = mc.m_91098_();
      String path = originalTexture.m_135815_();
      if (path.endsWith(".png")) {
         String modifiedPath = path.replace(".png", "_iris.png");
         ResourceLocation irisTexture = new ResourceLocation(originalTexture.m_135827_(), modifiedPath);

         try {
            resourceManager.m_215593_(irisTexture);
            return irisTexture;
         } catch (FileNotFoundException var7) {
         }
      }

      return originalTexture;
   }

   private static RenderType createReticleRenderType(ResourceLocation texture, Supplier<ShaderInstance> shaderSupplier) {
      VertexFormat var10001 = DefaultVertexFormat.f_85819_;
      Mode var10002 = Mode.QUADS;
      CompositeStateBuilder var10006 = CompositeState.m_110628_().m_110685_(RenderStateShard.f_110139_).m_110661_(f_110110_).m_110663_(f_110113_).m_110671_(f_110153_).m_110677_(f_110155_).m_173290_(new TextureStateShard(texture, false, false));
      Objects.requireNonNull(shaderSupplier);
      return RenderType.m_173215_("pointblank:reticle_iris_with_parallax", var10001, var10002, 256, true, false, var10006.m_173292_(new ShaderStateShard(shaderSupplier::get)).m_110691_(false));
   }

   private static RenderType createReticleRenderType(ResourceLocation texture) {
      CompositeState compositeState = CompositeState.m_110628_().m_173292_(f_234323_).m_173290_(new TextureStateShard(texture, false, false)).m_110685_(f_110139_).m_110661_(f_110110_).m_110691_(true);
      return RenderType.m_173215_("pointblank:reticle_iris", DefaultVertexFormat.f_85818_, Mode.QUADS, 256, true, false, compositeState);
   }

   private RenderType createPipRenderType(boolean isMasked) {
      CompositeState compositeState = CompositeState.m_110628_().m_173292_(new ShaderStateShard(this::getPipShader)).m_173290_(new PipTextureStateShard()).m_110685_(f_110134_).m_110671_(f_110153_).m_110677_(f_110155_).m_110691_(true);
      Runnable setup = isMasked ? SETUP_STENCIL_RENDER : () -> {
      };
      Runnable clear = isMasked ? CLEAR_STENCIL_RENDER : () -> {
      };
      return RenderTypeProvider.wrapRenderType(RenderType.m_173215_("pointblank:pip_iris_" + isMasked, DefaultVertexFormat.f_85819_, Mode.QUADS, 256, true, false, compositeState), setup, clear);
   }

   private RenderType createPipMaskRenderType(ResourceLocation maskTexture, Supplier<ShaderInstance> shaderSupplier) {
      VertexFormat var10001 = DefaultVertexFormat.f_85819_;
      Mode var10002 = Mode.QUADS;
      CompositeStateBuilder var10006 = CompositeState.m_110628_().m_110663_(f_110111_).m_110671_(f_110153_).m_110677_(f_110155_).m_110685_(f_110134_).m_173290_(new TextureStateShard(maskTexture, false, false));
      Objects.requireNonNull(shaderSupplier);
      return RenderTypeProvider.wrapRenderType(RenderType.m_173215_("pointblank:pip_mask_iris", var10001, var10002, 256, true, false, var10006.m_173292_(new ShaderStateShard(shaderSupplier::get)).m_110691_(false)), RenderTypeProvider.SETUP_STENCIL_MASK_RENDER, RenderTypeProvider.CLEAR_STENCIL_MASK_RENDER);
   }

   private static RenderType createPipOverlayRenderType(ResourceLocation overlayTexture, Supplier<ShaderInstance> shaderSupplier, boolean isMasked) {
      Runnable setup = isMasked ? SETUP_STENCIL_RENDER : () -> {
      };
      Runnable clear = isMasked ? CLEAR_STENCIL_RENDER : () -> {
      };
      String var10000 = "pointblank:pip_overlay_iris_" + isMasked;
      VertexFormat var10001 = DefaultVertexFormat.f_85819_;
      Mode var10002 = Mode.QUADS;
      CompositeStateBuilder var10006 = CompositeState.m_110628_().m_110685_(RenderStateShard.f_110139_).m_110661_(f_110110_).m_110663_(f_110113_).m_110671_(f_110153_).m_110677_(f_110155_).m_173290_(new TextureStateShard(overlayTexture, false, false));
      Objects.requireNonNull(shaderSupplier);
      return RenderTypeProvider.wrapRenderType(RenderType.m_173215_(var10000, var10001, var10002, 256, true, false, var10006.m_173292_(new ShaderStateShard(shaderSupplier::get)).m_110691_(false)), setup, clear);
   }

   public float getReticleBrightness() {
      return 0.6F;
   }

   public float getGlowBrightness() {
      return 0.6F;
   }

   private static class WrappedBufferSource implements MultiBufferSource, Flushable {
      private final MultiBufferSource delegate;

      WrappedBufferSource(MultiBufferSource delegate) {
         this.delegate = delegate;
      }

      public void flush() {
         MultiBufferSource var2 = this.delegate;
         if (var2 instanceof FullyBufferedMultiBufferSource) {
            FullyBufferedMultiBufferSource fbmbs = (FullyBufferedMultiBufferSource)var2;
            fbmbs.m_109911_();
         }

      }

      public VertexConsumer m_6299_(RenderType renderType) {
         return renderType == RenderTypeProvider.NO_RENDER_TYPE ? null : this.delegate.m_6299_(renderType);
      }
   }

   private static class PipTextureStateShard extends EmptyTextureStateShard {
      public PipTextureStateShard() {
         super(() -> {
            int textureId = ClientSystem.getInstance().getAuxLevelRenderer().getRenderTarget().m_83975_();
            RenderSystem.setShaderTexture(0, textureId);
         }, () -> {
         });
      }
   }

   private static final class GlowRenderType extends RenderType {
      public GlowRenderType(String renderTypeName, VertexFormat vertexFormat, Mode mode, int p_173181_, boolean p_173182_, boolean p_173183_, Runnable p_173184_, Runnable p_173185_) {
         super(renderTypeName, vertexFormat, mode, p_173181_, p_173182_, p_173183_, p_173184_, p_173185_);
      }

      private static RenderType createRenderType(ResourceLocation glowTexture) {
         return RenderType.m_173215_("pointblank:glow_iris", DefaultVertexFormat.f_85812_, Mode.QUADS, 256, true, false, CompositeState.m_110628_().m_110685_(RenderStateShard.f_110139_).m_110661_(f_110110_).m_110663_(f_110113_).m_110671_(f_110152_).m_110677_(f_110155_).m_173290_(new TextureStateShard(glowTexture, false, false)).m_173292_(RenderStateShard.f_234323_).m_110691_(false));
      }
   }
}
