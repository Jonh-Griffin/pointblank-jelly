package com.vicmatskiv.pointblank.client.particle;

import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.vicmatskiv.pointblank.client.effect.Effect;
import com.vicmatskiv.pointblank.client.effect.EffectRenderContext;
import com.vicmatskiv.pointblank.client.uv.SpriteUVProvider;
import com.vicmatskiv.pointblank.util.MiscUtil;
import com.vicmatskiv.pointblank.util.TimeUnit;
import java.util.Objects;
import java.util.Random;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.extensions.IForgeBlockEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@OnlyIn(Dist.CLIENT)
public class EffectParticles {
   private static final Logger LOGGER = LogManager.getLogger("pointblank");
   private static Function<EffectRenderKey, ParticleRenderType> effectRenderTypes = Util.m_143827_((key) -> {
      return new EffectParticleRenderType(key.texture, key.blendMode, key.isDepthTestEnabled);
   });

   private static class EffectParticleRenderType implements ParticleRenderType {
      private ResourceLocation texture;
      private Effect.BlendMode blendMode;
      private boolean isDepthTestEnabled;

      public EffectParticleRenderType(ResourceLocation texture, Effect.BlendMode blendMode, boolean isDepthTestEnabled) {
         this.texture = texture;
         this.blendMode = blendMode;
         this.isDepthTestEnabled = isDepthTestEnabled;
      }

      public void m_6505_(BufferBuilder bufferBuilder, TextureManager textureManager) {
         RenderSystem.setShaderTexture(0, this.texture);
         RenderSystem.enableBlend();
         if (this.isDepthTestEnabled) {
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
         } else {
            RenderSystem.depthMask(false);
            RenderSystem.disableDepthTest();
         }

         RenderSystem.disableCull();
         switch(this.blendMode) {
         case ADDITIVE:
            RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
            break;
         default:
            RenderSystem.defaultBlendFunc();
         }

         Minecraft mc = Minecraft.m_91087_();
         mc.f_91063_.m_109154_().m_109896_();
         bufferBuilder.m_166779_(Mode.QUADS, DefaultVertexFormat.f_85813_);
      }

      public void m_6294_(Tesselator tesselator) {
         tesselator.m_85914_();
      }
   }

   private static class EffectRenderKey {
      ResourceLocation texture;
      Effect.BlendMode blendMode;
      boolean isDepthTestEnabled;

      EffectRenderKey(ResourceLocation texture, Effect.BlendMode blendMode, boolean isDepthTestEnabled) {
         this.texture = texture;
         this.blendMode = blendMode;
         this.isDepthTestEnabled = isDepthTestEnabled;
      }

      public int hashCode() {
         return Objects.hash(new Object[]{this.blendMode, this.isDepthTestEnabled, this.texture});
      }

      public boolean equals(Object obj) {
         if (this == obj) {
            return true;
         } else if (obj == null) {
            return false;
         } else if (this.getClass() != obj.getClass()) {
            return false;
         } else {
            EffectRenderKey other = (EffectRenderKey)obj;
            return this.blendMode == other.blendMode && this.isDepthTestEnabled == other.isDepthTestEnabled && Objects.equals(this.texture, other.texture);
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class EffectParticleProvider implements ParticleProvider<SimpleParticleType> {
      public EffectParticleProvider(SpriteSet spriteSet) {
      }

      public Particle createParticle(SimpleParticleType particleType, ClientLevel level, double posX, double posY, double posZ, double xd, double yd, double zd) {
         EffectParticle particle = new EffectParticle(level, posX, posY, posZ);
         return particle;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class EffectParticle extends Particle {
      private ParticleRenderType renderType;
      private Entity owner;
      private float initialRoll;
      protected Effect effect;
      private boolean hasInfiniteBounds;
      protected SpriteUVProvider spriteUVProvider;
      private int delay;

      protected EffectParticle(ClientLevel level, double x, double y, double z) {
         super(level, x, y, z);
      }

      public EffectParticle(Entity owner, Effect effect) {
         super((ClientLevel)MiscUtil.getLevel(owner), 0.0D, 0.0D, 0.0D);
         this.effect = effect;
         this.hasInfiniteBounds = effect.hasInfiniteBounds();
         Vec3 startPosition = (Vec3)effect.getStartPositionProvider().get();
         Random random = new Random();
         this.m_107264_(startPosition.f_82479_ + (double)(random.nextFloat() * 0.01F), startPosition.f_82480_ + (double)(random.nextFloat() * 0.01F), startPosition.f_82481_ + (double)(random.nextFloat() * 0.01F));
         this.f_107209_ = this.f_107212_;
         this.f_107210_ = this.f_107213_;
         this.f_107211_ = this.f_107214_;
         this.f_107215_ *= 0.10000000149011612D;
         this.f_107216_ *= 0.10000000149011612D;
         this.f_107217_ *= 0.10000000149011612D;
         Vec3 velocity = (Vec3)effect.getVelocityProvider().get();
         float c = 1.0F;
         this.f_107215_ += velocity.f_82479_ * (double)c;
         this.f_107216_ += velocity.f_82480_ * (double)c;
         this.f_107217_ += velocity.f_82481_ * (double)c;
         this.f_107219_ = effect.hasPhysics();
         this.owner = owner;
         this.delay = (int)TimeUnit.MILLISECOND.toTicks(effect.getDelay());
         this.f_107225_ = (int)TimeUnit.MILLISECOND.toTicks(effect.getDuration()) + this.delay;
         this.renderType = (ParticleRenderType)EffectParticles.effectRenderTypes.apply(new EffectRenderKey(effect.getTexture(), effect.getBlendMode(), effect.isDepthTestEnabled()));
         this.initialRoll = effect.getInitialRoll();
         this.f_172258_ = effect.getFriction();
         this.f_107226_ = effect.getGravity();
         this.f_172259_ = true;
         this.spriteUVProvider = effect.getSpriteUVProvider();
      }

      protected float getProgress(float partialTick) {
         int adjustedAge = this.f_107224_ - this.delay;
         float elapsedTimeTicks = (float)adjustedAge + partialTick;
         float progress;
         if (adjustedAge < 0) {
            progress = elapsedTimeTicks / (float)this.delay;
         } else {
            progress = elapsedTimeTicks / (float)(this.f_107225_ - this.delay);
         }

         return Mth.m_14036_(progress, -1.0F, 1.0F);
      }

      public void m_5744_(VertexConsumer vertexConsumer, Camera camera, float partialTick) {
         try {
            if (camera.m_90592_() != this.owner) {
            }

            int lightColor = this.m_6355_(partialTick);
            double posX = Mth.m_14139_((double)partialTick, this.f_107209_, this.f_107212_);
            double posY = Mth.m_14139_((double)partialTick, this.f_107210_, this.f_107213_);
            double posZ = Mth.m_14139_((double)partialTick, this.f_107211_, this.f_107214_);
            float progress = this.getProgress(partialTick);
            EffectRenderContext effectRenderContext = (new EffectRenderContext()).withCamera(camera).withRotation(this.effect.getRotation()).withPosition(new Vec3(posX, posY, posZ)).withInitialAngle(this.initialRoll).withVertexBuffer(vertexConsumer).withProgress(progress).withLightColor(lightColor).withSpriteUVProvider(this.spriteUVProvider);
            this.effect.render(effectRenderContext);
         } catch (Exception var13) {
            EffectParticles.LOGGER.error("Failed to render effect particle: {}", var13);
         }

      }

      public ParticleRenderType m_7556_() {
         return this.renderType;
      }

      public AABB m_107277_() {
         return this.hasInfiniteBounds ? IForgeBlockEntity.INFINITE_EXTENT_AABB : super.m_107277_();
      }

      public void m_5989_() {
         super.m_5989_();
      }
   }
}
