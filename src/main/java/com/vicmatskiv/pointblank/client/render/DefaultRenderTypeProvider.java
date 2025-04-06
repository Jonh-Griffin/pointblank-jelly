package com.vicmatskiv.pointblank.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.vicmatskiv.pointblank.client.ClientSystem;
import com.vicmatskiv.pointblank.compat.iris.IrisCompat;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.RenderStateShard.EmptyTextureStateShard;
import net.minecraft.client.renderer.RenderStateShard.ShaderStateShard;
import net.minecraft.client.renderer.RenderStateShard.TextureStateShard;
import net.minecraft.client.renderer.RenderType.CompositeRenderType;
import net.minecraft.client.renderer.RenderType.CompositeState;
import net.minecraft.client.renderer.RenderType.CompositeState.CompositeStateBuilder;
import net.minecraft.resources.ResourceLocation;

public class DefaultRenderTypeProvider extends RenderStateShard implements RenderTypeProvider {
   private static DefaultRenderTypeProvider instance;
   private static Optional<ShaderInstance> posTexColorShader;
   private static Optional<ShaderInstance> auxShader;
   private Function<Boolean, RenderType> pipRenderTypes = Util.m_143827_((isMasked) -> {
      return createPipRenderType(isMasked);
   });
   private Function<ResourceLocation, RenderType> pipOverlayRenderTypes = Util.m_143827_((texture) -> {
      return createPipOverlayRenderType(texture, false);
   });
   private Function<ResourceLocation, RenderType> pipOverlayRenderTypesMasked = Util.m_143827_((texture) -> {
      return createPipOverlayRenderType(texture, true);
   });
   private Function<ResourceLocation, RenderType> pipMaskRenderTypes = Util.m_143827_((texture) -> {
      return createPipMaskRenderType(texture);
   });
   private Function<ResourceLocation, RenderType> muzzleFlashRenderTypes = Util.m_143827_((texture) -> {
      return createMuzzleFlashRenderType(texture);
   });
   private Function<ResourceLocation, RenderType> reticleRenderTypes = Util.m_143827_((texture) -> {
      return createReticleRenderType(texture);
   });
   private Function<ResourceLocation, RenderType> reticleRenderTypesWithParallax = Util.m_143827_((texture) -> {
      ClientSystem var10001 = ClientSystem.getInstance();
      Objects.requireNonNull(var10001);
      return createReticleRenderType(texture, var10001::getTexColorShaderInstance);
   });
   private Function<ResourceLocation, RenderType> glowEntityRenderTypes = Util.m_143827_((texture) -> {
      return GlowEntityRenderType.createRenderType(texture);
   });
   private static final PipTextureStateShard PIP_TEXTURE_STATE_SHARD = new PipTextureStateShard();

   public static DefaultRenderTypeProvider getInstance() {
      if (instance == null) {
         instance = new DefaultRenderTypeProvider();
      }

      return instance;
   }

   public DefaultRenderTypeProvider() {
      super((String)null, (Runnable)null, (Runnable)null);
   }

   public RenderType getPipRenderType(boolean isMasked) {
      return (RenderType)this.pipRenderTypes.apply(isMasked);
   }

   public RenderType getPipOverlayRenderType(ResourceLocation texture, boolean isMasked) {
      return isMasked ? (RenderType)this.pipOverlayRenderTypesMasked.apply(texture) : (RenderType)this.pipOverlayRenderTypes.apply(texture);
   }

   public RenderType getPipMaskRenderType(ResourceLocation texture) {
      return (RenderType)this.pipMaskRenderTypes.apply(texture);
   }

   public RenderType getGlowRenderType(ResourceLocation texture) {
      return RenderType.m_234335_(texture, true);
   }

   public RenderType getMuzzleFlashRenderType(ResourceLocation texture) {
      return (RenderType)this.muzzleFlashRenderTypes.apply(texture);
   }

   public RenderType getReticleRenderType(ResourceLocation texture, boolean isParallaxEnabled) {
      return isParallaxEnabled ? (RenderType)this.reticleRenderTypesWithParallax.apply(texture) : (RenderType)this.reticleRenderTypes.apply(texture);
   }

   public RenderType getGlowBlockEntityRenderType(ResourceLocation texture) {
      return IrisCompat.getInstance().isIrisLoaded() ? RenderType.m_110473_(texture) : (RenderType)this.glowEntityRenderTypes.apply(texture);
   }

   private static ShaderInstance getPosTexColorShader() {
      if (posTexColorShader != null) {
         return (ShaderInstance)posTexColorShader.orElse((Object)null);
      } else {
         String shaderName = "pointblank_position_tex_color";

         try {
            Minecraft mc = Minecraft.m_91087_();
            posTexColorShader = Optional.of(new ShaderInstance(mc.m_91098_(), shaderName, DefaultVertexFormat.f_85819_));
         } catch (Exception var2) {
            var2.printStackTrace();
            posTexColorShader = Optional.empty();
         }

         return (ShaderInstance)posTexColorShader.get();
      }
   }

   private static ShaderInstance getAuxShader() {
      if (auxShader != null) {
         return (ShaderInstance)auxShader.orElse((Object)null);
      } else {
         String shaderName = "pointblank_aux";

         try {
            Minecraft mc = Minecraft.m_91087_();
            auxShader = Optional.of(new ShaderInstance(mc.m_91098_(), shaderName, DefaultVertexFormat.f_85819_));
         } catch (Exception var2) {
            var2.printStackTrace();
            auxShader = Optional.empty();
         }

         return (ShaderInstance)auxShader.get();
      }
   }

   private static RenderType createPipRenderType(boolean isMasked) {
      Runnable setup = isMasked ? SETUP_STENCIL_RENDER : () -> {
      };
      Runnable clear = isMasked ? CLEAR_STENCIL_RENDER : () -> {
      };
      return RenderTypeProvider.wrapRenderType(RenderType.m_173215_("pointblank:pip", DefaultVertexFormat.f_85819_, Mode.QUADS, 256, true, false, CompositeState.m_110628_().m_173292_(new ShaderStateShard(DefaultRenderTypeProvider::getAuxShader)).m_173290_(PIP_TEXTURE_STATE_SHARD).m_110685_(f_110134_).m_110663_(f_110113_).m_110671_(f_110153_).m_110677_(f_110155_).m_110661_(f_110110_).m_110691_(true)), setup, clear);
   }

   private static RenderType createPipMaskRenderType(ResourceLocation maskTexture) {
      return RenderTypeProvider.wrapRenderType(CompositeRenderType.m_173215_("pointblank:pip_mask", DefaultVertexFormat.f_85819_, Mode.QUADS, 2097152, true, false, CompositeState.m_110628_().m_110685_(f_110134_).m_173290_(new TextureStateShard(maskTexture, false, false)).m_173292_(new ShaderStateShard(GameRenderer::m_172820_)).m_110691_(false)), RenderTypeProvider.SETUP_STENCIL_MASK_RENDER, RenderTypeProvider.CLEAR_STENCIL_MASK_RENDER);
   }

   private static RenderType createPipOverlayRenderType(ResourceLocation overlayTexture, boolean isMasked) {
      Runnable setup = isMasked ? SETUP_STENCIL_RENDER : () -> {
      };
      Runnable clear = isMasked ? CLEAR_STENCIL_RENDER : () -> {
      };
      return RenderTypeProvider.wrapRenderType(RenderType.m_173215_("pointblank:pip_overlay_" + isMasked, DefaultVertexFormat.f_85819_, Mode.QUADS, 256, true, false, CompositeState.m_110628_().m_110685_(RenderStateShard.f_110139_).m_110661_(f_110110_).m_110663_(f_110113_).m_110671_(f_110153_).m_110677_(f_110155_).m_173290_(new TextureStateShard(overlayTexture, false, false)).m_173292_(new ShaderStateShard(DefaultRenderTypeProvider::getPosTexColorShader)).m_110691_(false)), setup, clear);
   }

   private static RenderType createMuzzleFlashRenderType(ResourceLocation texture) {
      VertexFormat var10001 = DefaultVertexFormat.f_85818_;
      Mode var10002 = Mode.QUADS;
      CompositeStateBuilder var10006 = CompositeState.m_110628_().m_110685_(f_110136_).m_110661_(f_110110_).m_110663_(f_110113_).m_110671_(f_110152_).m_110677_(f_110154_).m_173290_(new TextureStateShard(texture, false, false));
      ClientSystem var10009 = ClientSystem.getInstance();
      Objects.requireNonNull(var10009);
      return RenderType.m_173215_("pointblank:muzzle_flash", var10001, var10002, 256, true, false, var10006.m_173292_(new ShaderStateShard(var10009::getColorTexLightmapShaderInstance)).m_110691_(false));
   }

   private static RenderType createReticleRenderType(ResourceLocation texture, Supplier<ShaderInstance> shaderSupplier) {
      VertexFormat var10001 = DefaultVertexFormat.f_85819_;
      Mode var10002 = Mode.QUADS;
      CompositeStateBuilder var10006 = CompositeState.m_110628_().m_110685_(RenderStateShard.f_110139_).m_110661_(f_110110_).m_110663_(f_110113_).m_110671_(f_110153_).m_110677_(f_110155_).m_173290_(new TextureStateShard(texture, false, false));
      Objects.requireNonNull(shaderSupplier);
      return RenderType.m_173215_("pointblank:reticle_parallax", var10001, var10002, 256, true, false, var10006.m_173292_(new ShaderStateShard(shaderSupplier::get)).m_110691_(false));
   }

   private static RenderType createReticleRenderType(ResourceLocation reticleResource) {
      VertexFormat var10001 = DefaultVertexFormat.f_85820_;
      Mode var10002 = Mode.QUADS;
      CompositeStateBuilder var10006 = CompositeState.m_110628_().m_110685_(f_110139_).m_110661_(f_110110_).m_110663_(f_110113_).m_110671_(f_110152_).m_110677_(f_110154_).m_173290_(new TextureStateShard(reticleResource, false, false));
      ClientSystem var10009 = ClientSystem.getInstance();
      Objects.requireNonNull(var10009);
      return RenderType.m_173215_("pointblank:reticle", var10001, var10002, 256, true, false, var10006.m_173292_(new ShaderStateShard(var10009::getColorTexLightmapShaderInstance)).m_110691_(false));
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

   private static final class GlowEntityRenderType extends RenderType {
      public GlowEntityRenderType(String renderTypeName, VertexFormat p_173179_, Mode p_173180_, int p_173181_, boolean p_173182_, boolean p_173183_, Runnable p_173184_, Runnable p_173185_) {
         super(renderTypeName, p_173179_, p_173180_, p_173181_, p_173182_, p_173183_, p_173184_, p_173185_);
      }

      private static RenderType createRenderType(ResourceLocation texture) {
         return RenderType.m_173215_("pointblank:glowy_entity_block", DefaultVertexFormat.f_85818_, Mode.QUADS, 256, true, false, CompositeState.m_110628_().m_110685_(f_110136_).m_110661_(f_110110_).m_110671_(f_110152_).m_110677_(f_110154_).m_173290_(new TextureStateShard(texture, false, false)).m_173292_(RenderStateShard.f_173101_).m_110691_(false));
      }
   }
}
