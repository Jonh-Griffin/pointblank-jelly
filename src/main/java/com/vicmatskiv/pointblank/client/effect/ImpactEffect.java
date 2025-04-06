package com.vicmatskiv.pointblank.client.effect;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.vicmatskiv.pointblank.client.particle.EffectParticles;
import com.vicmatskiv.pointblank.item.GunItem;
import com.vicmatskiv.pointblank.util.Interpolators;
import com.vicmatskiv.pointblank.util.ParticleValueProviders;
import java.util.Collection;
import java.util.Set;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ImpactEffect extends AbstractEffect {
   private float textureMinU = 0.0F;
   private float textureMaxU = 1.0F;
   private int count;

   private ImpactEffect() {
   }

   public boolean hasInfiniteBounds() {
      return false;
   }

   public void render(EffectRenderContext effectRenderContext) {
      Vec3 pos = effectRenderContext.getPosition();
      if (pos == null) {
         throw new IllegalArgumentException("Incorrect render context, missing position");
      } else {
         Camera camera = effectRenderContext.getCamera();
         if (camera == null) {
            throw new IllegalArgumentException("Incorrect render context, missing camera");
         } else {
            float progress = effectRenderContext.getProgress();
            if (!(progress < 0.0F)) {
               float[] uv = effectRenderContext.getSpriteUVProvider().getSpriteUV(progress);
               if (uv != null) {
                  VertexConsumer vertexConsumer = effectRenderContext.getVertexBuffer();
                  int lightColor = this.isGlowEnabled ? 240 : effectRenderContext.getLightColor();
                  float size = this.widthProvider.getValue(progress);
                  float alpha = this.alphaProvider.getValue(progress);
                  float spriteWidth = uv[2] - uv[0];
                  float minU = uv[0] + spriteWidth * this.textureMinU;
                  float minV = uv[1];
                  float maxU = uv[0] + spriteWidth * this.textureMaxU;
                  float maxV = uv[3];
                  float u0 = minU;
                  float u1 = maxU;
                  float v0 = minV;
                  float v1 = maxV;
                  Vec3 cameraPosition = camera.m_90583_();
                  float dx = (float)(pos.f_82479_ - cameraPosition.m_7096_());
                  float dy = (float)(pos.f_82480_ - cameraPosition.m_7098_());
                  float dz = (float)(pos.f_82481_ - cameraPosition.m_7094_());
                  Quaternionf rotation = new Quaternionf();
                  float roll = -(effectRenderContext.getInitialAngle() + this.numRotations * 360.0F * progress);
                  if (effectRenderContext.getRotation() != null) {
                     rotation.mul(effectRenderContext.getRotation());
                     rotation.rotateZ(0.017453292F * roll);
                  } else {
                     rotation.mul(camera.m_253121_());
                     rotation.rotateZ(0.017453292F * roll);
                  }

                  Vector3f[] avector3f = new Vector3f[]{new Vector3f(-1.0F, -1.0F, 0.0F), new Vector3f(-1.0F, 1.0F, 0.0F), new Vector3f(1.0F, 1.0F, 0.0F), new Vector3f(1.0F, -1.0F, 0.0F)};

                  int argbColor;
                  for(argbColor = 0; argbColor < 4; ++argbColor) {
                     Vector3f vector3f = avector3f[argbColor];
                     vector3f.rotate(rotation);
                     vector3f.mul(size);
                     vector3f.add(dx, dy, dz);
                  }

                  argbColor = (int)(alpha * 255.0F) << 24 | this.color;

                  for(int k = 0; k < this.brightness; ++k) {
                     vertexConsumer.m_5483_((double)avector3f[0].x(), (double)avector3f[0].y(), (double)avector3f[0].z()).m_7421_(u1, v1).m_193479_(argbColor).m_85969_(lightColor).m_5752_();
                     vertexConsumer.m_5483_((double)avector3f[1].x(), (double)avector3f[1].y(), (double)avector3f[1].z()).m_7421_(u1, v0).m_193479_(argbColor).m_85969_(lightColor).m_5752_();
                     vertexConsumer.m_5483_((double)avector3f[2].x(), (double)avector3f[2].y(), (double)avector3f[2].z()).m_7421_(u0, v0).m_193479_(argbColor).m_85969_(lightColor).m_5752_();
                     vertexConsumer.m_5483_((double)avector3f[3].x(), (double)avector3f[3].y(), (double)avector3f[3].z()).m_7421_(u0, v1).m_193479_(argbColor).m_85969_(lightColor).m_5752_();
                  }

               }
            }
         }
      }
   }

   public void launch(Entity player) {
      Minecraft mc = Minecraft.m_91087_();

      for(int i = 0; i < this.count; ++i) {
         Particle particle = new EffectParticles.EffectParticle(player, this);
         mc.f_91061_.m_107344_(particle);
      }

   }

   public static class Builder extends AbstractEffectBuilder<Builder, ImpactEffect> {
      private static final Set<GunItem.FirePhase> COMPATIBLE_PHASES;
      private ParticleValueProviders.ParticleCountProvider countProvider = new ParticleValueProviders.ConstantParticleCountProvider(1);
      private ParticleValueProviders.ParticleWidthProvider impactWidthProvider;

      public Builder withCount(int count) {
         this.countProvider = new ParticleValueProviders.ConstantParticleCountProvider(count);
         return this;
      }

      public Builder withCount(ParticleValueProviders.ParticleCountProvider countProvider) {
         this.countProvider = countProvider;
         return this;
      }

      public Builder withWidth(ParticleValueProviders.ParticleWidthProvider widthProvider) {
         this.impactWidthProvider = widthProvider;
         return this;
      }

      public boolean isEffectAttached() {
         return false;
      }

      public ImpactEffect build(Context effectContext) {
         ImpactEffect effect = new ImpactEffect();
         super.apply(effect, effectContext);
         effect.count = this.countProvider.getCount(effectContext);
         if (this.impactWidthProvider != null) {
            float width = this.impactWidthProvider.getWidth(effectContext);
            effect.widthProvider = new Interpolators.ConstantFloatProvider(width);
         }

         return effect;
      }

      public Collection<GunItem.FirePhase> getCompatiblePhases() {
         return COMPATIBLE_PHASES;
      }

      static {
         COMPATIBLE_PHASES = Set.of(GunItem.FirePhase.HIT_TARGET);
      }
   }
}
