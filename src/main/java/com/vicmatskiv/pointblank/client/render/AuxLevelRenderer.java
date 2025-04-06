package com.vicmatskiv.pointblank.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.vicmatskiv.pointblank.Config;
import com.vicmatskiv.pointblank.compat.iris.IrisCompat;
import com.vicmatskiv.pointblank.feature.PipFeature;
import com.vicmatskiv.pointblank.item.GunItem;
import com.vicmatskiv.pointblank.mixin.GameRendererAccessorMixin;
import com.vicmatskiv.pointblank.mixin.MinecraftAccessorMixin;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.util.ClientUtils;

public class AuxLevelRenderer {
   private RenderTarget renderTarget;
   private int textureWidth;
   private int textureHeight;
   private long frameCount = 0L;
   private boolean isRendering;
   private boolean isRenderingPip;
   private double fov;
   private double cullFrustrumFov;
   private boolean isStencilEnabled;

   public AuxLevelRenderer(int textureWidth, int textureHeight) {
      this.renderTarget = new TextureTarget(textureWidth, textureHeight, true, Minecraft.f_91002_);
      this.renderTarget.m_83931_(0.0F, 0.0F, 0.0F, 0.0F);
      this.textureWidth = textureWidth;
      this.textureHeight = textureHeight;
   }

   public RenderTarget getRenderTarget() {
      return this.renderTarget;
   }

   public boolean isRenderingPip() {
      return this.isRenderingPip;
   }

   public double getFov() {
      return this.fov;
   }

   public double getCullFrustrumFov() {
      return this.cullFrustrumFov;
   }

   public void renderToTarget(float partialTick, long time, float zoom) {
      Minecraft mc = Minecraft.m_91087_();
      if (!mc.f_91079_ && mc.f_91072_ != null && mc.f_91074_ != null) {
         if (Config.pipScopesEnabled && this.frameCount % (long)Config.pipScopeRefreshFrame == 0L) {
            this.isRenderingPip = true;
            double d0 = ((GameRendererAccessorMixin)mc.f_91063_).invokeGetFov(mc.f_91063_.m_109153_(), partialTick, true);
            this.fov = d0 * (double)(1.0F - zoom);
            this.cullFrustrumFov = 110.0D;
            RenderTarget origTarget = mc.m_91385_();
            MinecraftAccessorMixin mm = (MinecraftAccessorMixin)mc;
            int[] viewport = new int[4];
            GL11.glGetIntegerv(2978, viewport);
            int originalWidth = mc.m_91268_().m_85441_();
            int originalHeight = mc.m_91268_().m_85442_();
            if (!origTarget.isStencilEnabled()) {
               Player player = ClientUtils.getClientPlayer();
               if (player != null) {
                  ItemStack itemStack = GunItem.getMainHeldGunItemStack(player);
                  if (itemStack != null && PipFeature.getMaskTexture(itemStack) != null) {
                     origTarget.enableStencil();
                  }
               }
            }

            if (this.renderTarget.f_83915_ != origTarget.f_83915_ || this.renderTarget.f_83916_ != origTarget.f_83916_ || this.isStencilEnabled != origTarget.isStencilEnabled()) {
               this.renderTarget.m_83941_(origTarget.f_83915_, origTarget.f_83916_, true);
               this.textureWidth = originalWidth;
               this.textureHeight = originalHeight;
               if (origTarget.isStencilEnabled()) {
                  this.renderTarget.enableStencil();
               }
            }

            this.isStencilEnabled = origTarget.isStencilEnabled();
            mc.m_91385_().m_83970_();
            mc.m_91385_().m_83954_(false);
            mm.setMainRenderTarget(this.renderTarget);
            this.renderTarget.m_83947_(true);

            try {
               mc.f_91063_.m_172779_(true);
               mc.f_91063_.m_172775_(false);
               mc.f_91063_.m_172736_(false);
               RenderSystem.clear(0, Minecraft.f_91002_);
               this.renderTarget.m_83954_(false);
               this.renderTarget.m_83947_(false);
               mc.f_91063_.m_109089_(partialTick, time + 10000L, new PoseStack());
            } finally {
               mc.f_91063_.m_172779_(false);
               mc.f_91063_.m_172775_(true);
               mc.f_91063_.m_172736_(true);
               mc.m_91385_().m_83970_();
               mm.setMainRenderTarget(origTarget);
               RenderSystem.clear(0, Minecraft.f_91002_);
               mc.m_91385_().m_83954_(true);
               mc.m_91385_().m_83947_(true);
               this.isRenderingPip = false;
            }

            IrisCompat irisCompat = IrisCompat.getInstance();
            if (irisCompat.isIrisLoaded() && irisCompat.isShaderPackEnabled()) {
               GL11.glDepthMask(true);
               GL11.glClear(17664);
               if (ClientUtils.getLevel().m_46472_() != Level.f_46429_) {
                  GL11.glDepthMask(false);
               }
            }

         }
      }
   }

   public boolean isRendering() {
      return this.isRendering;
   }

   public void renderToBuffer(PoseStack poseStack, GeoQuad quad, VertexConsumer buffer, float red, float green, float blue, float alpha) {
      Matrix4f poseState = poseStack.m_85850_().m_252922_();
      float[][] texUV = new float[][]{{1.0F, 1.0F}, {0.0F, 1.0F}, {0.0F, 0.0F}, {1.0F, 0.0F}};

      for(int i = 0; i < 4; ++i) {
         GeoVertex v = quad.vertices()[i];
         buffer.m_252986_(poseState, v.position().x, v.position().y, v.position().z).m_7421_(texUV[i][0], texUV[i][1]).m_85950_(red, green, blue, alpha).m_5752_();
      }

   }

   public void renderToBuffer(PoseStack poseStack, GeoQuad quad, VertexConsumer buffer, int packedLight) {
      Matrix4f poseState = poseStack.m_85850_().m_252922_();
      float aspectRatio = Mth.m_14036_((float)this.textureHeight / (float)this.textureWidth, 0.0F, 1.0F);
      float arH = (1.0F - aspectRatio) * 0.5F;
      float minU = 0.0F + arH;
      float maxU = 1.0F - arH;
      float minV = 0.0F;
      float maxV = 1.0F;
      float[][] texUV = new float[][]{{maxU, maxV}, {minU, maxV}, {minU, minV}, {maxU, minV}};
      IrisCompat irisCompat = IrisCompat.getInstance();
      int red;
      int green;
      int blue;
      int alpha;
      int i;
      if (irisCompat.isShaderPackEnabled()) {
         i = irisCompat.getColorBalance();
         red = i >> 24 & 255;
         green = i >> 16 & 255;
         blue = i >> 8 & 255;
         alpha = i & 255;
      } else {
         alpha = 255;
         blue = 255;
         green = 255;
         red = 255;
      }

      for(i = 0; i < 4; ++i) {
         GeoVertex v = quad.vertices()[i];
         buffer.m_252986_(poseState, v.position().x, v.position().y, v.position().z).m_7421_(texUV[i][0], texUV[i][1]).m_6122_(red, green, blue, alpha).m_5752_();
      }

   }
}
