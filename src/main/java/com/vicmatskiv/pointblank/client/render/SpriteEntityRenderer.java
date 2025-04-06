package com.vicmatskiv.pointblank.client.render;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.vicmatskiv.pointblank.client.EntityRendererBuilder;
import com.vicmatskiv.pointblank.client.effect.AbstractEffect;
import com.vicmatskiv.pointblank.client.effect.Effect;
import com.vicmatskiv.pointblank.client.uv.LoopingSpriteUVProvider;
import com.vicmatskiv.pointblank.client.uv.PlayOnceSpriteUVProvider;
import com.vicmatskiv.pointblank.client.uv.RandomSpriteUVProvider;
import com.vicmatskiv.pointblank.client.uv.SpriteUVProvider;
import com.vicmatskiv.pointblank.client.uv.StaticSpriteUVProvider;
import com.vicmatskiv.pointblank.entity.ProjectileLike;
import java.util.Random;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderStateShard.TextureStateShard;
import net.minecraft.client.renderer.RenderStateShard.TransparencyStateShard;
import net.minecraft.client.renderer.RenderType.CompositeState;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public class SpriteEntityRenderer<T extends Entity & ProjectileLike> extends EntityRenderer<T> {
   private ResourceLocation texture;
   private RenderType renderType;
   private SpriteUVProvider spriteUVProvider;
   private float initialRoll;
   private boolean isGlowEnabled;
   private int brightness;
   private float rotationsPerSecond;
   private float width;
   private float height;

   public SpriteEntityRenderer(Context context) {
      super(context);
   }

   public void m_7392_(T projectile, float p_114657_, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int lightColor) {
      float progress = (float)((ProjectileLike)projectile).getElapsedTimeMillis() / 1000.0F;
      poseStack.m_85836_();
      Minecraft mc = Minecraft.m_91087_();
      Camera camera = mc.f_91063_.m_109153_();
      poseStack.m_252781_(camera.m_253121_());
      float[] uv = this.spriteUVProvider.getSpriteUV(progress);
      if (uv != null) {
         VertexConsumer vertexConsumer = bufferSource.m_6299_(this.renderType);
         if (this.isGlowEnabled) {
            lightColor = 240;
         }

         float alpha = 1.0F;
         float spriteWidth = uv[2] - uv[0];
         float textureMinU = 0.0F;
         float textureMaxU = 1.0F;
         float minU = uv[0] + spriteWidth * textureMinU;
         float minV = uv[1];
         float maxU = uv[0] + spriteWidth * textureMaxU;
         float maxV = uv[3];
         Quaternionf rotation = new Quaternionf();
         float roll = this.initialRoll + this.rotationsPerSecond * 360.0F * progress;
         rotation.rotateZ(0.017453292F * roll);
         poseStack.m_252781_(rotation);
         Matrix4f matrix4f = poseStack.m_85850_().m_252922_();
         float halfWidth = (this.width > 0.0F ? this.width : projectile.m_20205_()) * 0.5F;
         float halfHeight = (this.height > 0.0F ? this.height : projectile.m_20206_()) * 0.5F;

         for(int k = 0; k < this.brightness; ++k) {
            vertexConsumer.m_252986_(matrix4f, -halfWidth, halfHeight, 0.0F).m_85950_(1.0F, 1.0F, 1.0F, alpha).m_7421_(maxU, minV).m_85969_(lightColor).m_5752_();
            vertexConsumer.m_252986_(matrix4f, halfWidth, halfHeight, 0.0F).m_85950_(1.0F, 1.0F, 1.0F, alpha).m_7421_(minU, minV).m_85969_(lightColor).m_5752_();
            vertexConsumer.m_252986_(matrix4f, halfWidth, -halfHeight, 0.0F).m_85950_(1.0F, 1.0F, 1.0F, alpha).m_7421_(minU, maxV).m_85969_(lightColor).m_5752_();
            vertexConsumer.m_252986_(matrix4f, -halfWidth, -halfHeight, 0.0F).m_85950_(1.0F, 1.0F, 1.0F, (float)this.brightness).m_7421_(maxU, maxV).m_85969_(lightColor).m_5752_();
         }

         poseStack.m_85849_();
      }
   }

   public ResourceLocation m_5478_(Entity entity) {
      return this.texture;
   }

   public static class Builder<T extends Entity & ProjectileLike> implements EntityRendererBuilder<Builder<T>, T, EntityRenderer<T>> {
      private static long counter;
      private static final SpriteUVProvider DEFAULT_SPRITE_UV_PROVIDER;
      private static final String DEFAULT_NAME_PREFIX;
      private static Effect.BlendMode DEFAULT_BLEND_MODE;
      private static final int DEFAULT_BRIGHTNESS = 1;
      private static final float DEFAULT_NUM_ROTATIONS = 0.0F;
      private ResourceLocation texture;
      private Effect.BlendMode blendMode;
      private boolean isDepthTestEnabled;
      private boolean isGlowEnabled;
      private int brightness;
      private AbstractEffect.SpriteInfo spriteInfo;
      private float numRotations;
      private float width;
      private float height;

      public Builder() {
         ++counter;
         this.blendMode = DEFAULT_BLEND_MODE;
         this.brightness = 1;
         this.numRotations = 0.0F;
         this.width = Float.NEGATIVE_INFINITY;
         this.height = Float.NEGATIVE_INFINITY;
      }

      public Builder<T> withSize(float width, float hight) {
         this.width = width;
         return this;
      }

      public Builder<T> withSize(float size) {
         this.width = size;
         this.height = size;
         return this;
      }

      public Builder<T> withTexture(ResourceLocation texture) {
         this.texture = texture;
         return this;
      }

      public Builder<T> withTexture(String textureName) {
         this.texture = new ResourceLocation("pointblank", textureName);
         return this;
      }

      public Builder<T> withBlendMode(Effect.BlendMode blendMode) {
         this.blendMode = blendMode;
         return this;
      }

      public Builder<T> withDepthTest(boolean isDepthTestEnabled) {
         this.isDepthTestEnabled = isDepthTestEnabled;
         return this;
      }

      public Builder<T> withGlow(boolean isGlowEnabled) {
         this.isGlowEnabled = isGlowEnabled;
         return this;
      }

      public Builder<T> withBrightness(int brightness) {
         this.brightness = brightness;
         return this;
      }

      public Builder<T> withSprites(int rows, int columns, int spritesPerSecond, AbstractEffect.SpriteAnimationType type) {
         this.spriteInfo = new AbstractEffect.SpriteInfo(rows, columns, spritesPerSecond, type);
         return this;
      }

      public Builder<T> withRotations(double numRotations) {
         this.numRotations = (float)numRotations;
         return this;
      }

      public Builder<T> withJsonObject(JsonObject obj) {
         return null;
      }

      public EntityRenderer<T> build(Context context) {
         SpriteEntityRenderer<T> renderer = new SpriteEntityRenderer(context);
         renderer.width = this.width;
         renderer.height = this.height;
         renderer.initialRoll = (new Random()).nextFloat() * 360.0F;
         renderer.isGlowEnabled = this.isGlowEnabled;
         renderer.brightness = this.brightness;
         renderer.rotationsPerSecond = this.numRotations;
         renderer.texture = this.texture;
         String renderTypeName = DEFAULT_NAME_PREFIX + "." + counter;
         renderer.renderType = EffectRenderType.createRenderType(renderTypeName, this.texture, this.blendMode, this.isDepthTestEnabled);
         if (this.spriteInfo != null) {
            switch(this.spriteInfo.type()) {
            case STATIC:
               renderer.spriteUVProvider = StaticSpriteUVProvider.INSTANCE;
               break;
            case LOOP:
               SpriteUVProvider spriteUVProvider = new LoopingSpriteUVProvider(this.spriteInfo.rows(), this.spriteInfo.columns(), this.spriteInfo.spritesPerSecond(), 1000L);
               renderer.spriteUVProvider = spriteUVProvider;
               break;
            case RANDOM:
               renderer.spriteUVProvider = new RandomSpriteUVProvider(this.spriteInfo.rows(), this.spriteInfo.columns(), this.spriteInfo.spritesPerSecond(), 1000L);
               break;
            case PLAY_ONCE:
               SpriteUVProvider spriteUVProvider = new PlayOnceSpriteUVProvider(this.spriteInfo.rows(), this.spriteInfo.columns(), this.spriteInfo.spritesPerSecond(), 1000L);
               renderer.spriteUVProvider = spriteUVProvider;
            }
         } else {
            renderer.spriteUVProvider = DEFAULT_SPRITE_UV_PROVIDER;
         }

         return renderer;
      }

      static {
         DEFAULT_SPRITE_UV_PROVIDER = StaticSpriteUVProvider.INSTANCE;
         DEFAULT_NAME_PREFIX = "pointblank:" + SpriteEntityRenderer.class.getSimpleName();
         DEFAULT_BLEND_MODE = Effect.BlendMode.NORMAL;
      }
   }

   private static final class EffectRenderType extends RenderType {
      private EffectRenderType(String renderTypeName, VertexFormat vertexFormat, Mode p_173180_, int p_173181_, boolean p_173182_, boolean p_173183_, Runnable p_173184_, Runnable runnable) {
         super(renderTypeName, vertexFormat, p_173180_, p_173181_, p_173182_, p_173183_, p_173184_, runnable);
      }

      private static RenderType createRenderType(String name, ResourceLocation texture, Effect.BlendMode blendMode, boolean isDepthTestEnabled) {
         TransparencyStateShard transparencyShard;
         switch(blendMode) {
         case NORMAL:
            transparencyShard = f_110139_;
            break;
         default:
            transparencyShard = f_110136_;
         }

         return RenderType.m_173215_(name, DefaultVertexFormat.f_85820_, Mode.QUADS, 256, true, false, CompositeState.m_110628_().m_110685_(transparencyShard).m_110663_(isDepthTestEnabled ? f_110113_ : f_110111_).m_110661_(f_110110_).m_110671_(f_110152_).m_110677_(f_110154_).m_173290_(new TextureStateShard(texture, false, false)).m_173292_(RenderStateShard.f_173103_).m_110691_(false));
      }
   }
}
