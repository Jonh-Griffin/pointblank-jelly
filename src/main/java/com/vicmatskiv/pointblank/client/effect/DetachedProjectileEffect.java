package com.vicmatskiv.pointblank.client.effect;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.vicmatskiv.pointblank.client.SegmentsProviders;
import com.vicmatskiv.pointblank.client.VertexConsumers;
import com.vicmatskiv.pointblank.client.particle.EffectParticles;
import com.vicmatskiv.pointblank.client.uv.SpriteUVProvider;
import com.vicmatskiv.pointblank.item.GunItem;
import com.vicmatskiv.pointblank.util.Interpolators;
import com.vicmatskiv.pointblank.util.JsonUtil;
import com.vicmatskiv.pointblank.util.MiscUtil;
import java.util.Collection;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class DetachedProjectileEffect extends AbstractEffect {
   private float distanceToTarget;
   private Vec3 targetPosition;
   private int numBlades;
   private boolean isFaceEnabled;
   private float rCol = 1.0F;
   private float gCol = 1.0F;
   private float bCol = 1.0F;
   private int bladeBrightness;
   private int faceBrightness;
   private float bladeTextureMinU;
   private float bladeTextureMaxU;
   private float faceTextureMinU;
   private float faceTextureMaxU;
   private Quaternionf initialRotation;
   private SegmentsProviders.SegmentsProvider segmentsProvider;
   private Interpolators.FloatInterpolator faceWidthProvider;
   private Interpolators.FloatInterpolator bladeWidthProvider;
   private Vec3 startPosition;
   private Function<VertexConsumer, VertexConsumer> vertexConsumerTransformer;

   private DetachedProjectileEffect() {
   }

   public boolean hasInfiniteBounds() {
      return true;
   }

   public void render(EffectRenderContext effectRenderContext) {
      float progress = effectRenderContext.getProgress();
      if (!(progress < 0.0F)) {
         VertexConsumer vertexBuffer = (VertexConsumer)this.vertexConsumerTransformer.apply(effectRenderContext.getVertexBuffer());
         float[][] segments = this.segmentsProvider.getSegments(this.distanceToTarget, progress, this.lifetimeNanos);
         float distanceSoFar = segments[0][0] + (segments[0][1] - segments[0][0]) * 0.5F;
         if (!(distanceSoFar >= this.distanceToTarget)) {
            Minecraft mc = Minecraft.m_91087_();
            Camera camera = mc.f_91063_.m_109153_();
            if (this.initialRotation == null) {
               Vec3 direction = this.targetPosition.m_82546_(this.startPosition);
               this.initialRotation = (new Quaternionf()).rotateTo(0.0F, 0.0F, -1.0F, (float)direction.f_82479_, (float)direction.f_82480_, (float)direction.f_82481_);
            }

            Quaternionf faceRotation = new Quaternionf(camera.m_253121_());
            float distanceProgress = distanceSoFar / this.distanceToTarget;
            float currentPosX = (float)Mth.m_14139_((double)distanceProgress, this.startPosition.f_82479_, this.targetPosition.f_82479_);
            float currentPosY = (float)Mth.m_14139_((double)distanceProgress, this.startPosition.f_82480_, this.targetPosition.f_82480_);
            float currentPosZ = (float)Mth.m_14139_((double)distanceProgress, this.startPosition.f_82481_, this.targetPosition.f_82481_);
            Vec3 cameraPosition = camera.m_90583_();
            float dx = (float)((double)currentPosX - cameraPosition.m_7096_());
            float dy = (float)((double)currentPosY - cameraPosition.m_7098_());
            float dz = (float)((double)currentPosZ - cameraPosition.m_7094_());
            int lightColor = this.isGlowEnabled ? 240 : effectRenderContext.getLightColor();
            Quaternionf rotation = new Quaternionf(this.initialRotation);
            float roll = -(effectRenderContext.getInitialAngle() + this.numRotations * 360.0F * progress);
            rotation.rotateZ(-roll * 0.017453292F);
            SpriteUVProvider spriteUVProvider = effectRenderContext.getSpriteUVProvider();
            if (this.isFaceEnabled) {
               faceRotation.rotateZ(-roll * 0.017453292F);
               this.renderFace(dx, dy, dz, faceRotation, vertexBuffer, progress, lightColor, spriteUVProvider);
            }

            if (this.numBlades > 0) {
               float bladeStep = 180.0F / (float)this.numBlades;

               for(int k = 0; k < this.numBlades; ++k) {
                  Quaternionf bladeQuaternion = new Quaternionf(rotation);
                  float bladeRoll = (float)k * bladeStep;
                  bladeQuaternion.rotateZ(bladeRoll * 0.017453292F);
                  this.renderBlade(dx, dy, dz, bladeQuaternion, segments, vertexBuffer, progress, lightColor, spriteUVProvider);
               }
            }

         }
      }
   }

   private void renderFace(float dx, float dy, float dz, Quaternionf rotation, VertexConsumer vertexConsumer, float progress, int lightColor, SpriteUVProvider spriteUVProvider) {
      float halfWidth = this.faceWidthProvider.getValue(progress) * 0.5F;
      float alpha = this.alphaProvider.getValue(progress);
      float[] uv = spriteUVProvider.getSpriteUV(progress);
      float spriteWidth = uv[2] - uv[0];
      float u0 = uv[0] + spriteWidth * this.faceTextureMinU;
      float v0 = uv[1];
      float u1 = uv[0] + spriteWidth * this.faceTextureMaxU;
      float v1 = uv[3];
      Vector3f[] avector3f = new Vector3f[]{new Vector3f(-halfWidth, -halfWidth, 0.0F), new Vector3f(-halfWidth, halfWidth, 0.0F), new Vector3f(halfWidth, halfWidth, 0.0F), new Vector3f(halfWidth, -halfWidth, 0.0F)};

      int k;
      for(k = 0; k < 4; ++k) {
         Vector3f vector3f = avector3f[k];
         vector3f.rotate(rotation);
         vector3f.add(dx, dy, dz);
      }

      for(k = 0; k < this.faceBrightness; ++k) {
         vertexConsumer.m_5483_((double)avector3f[0].x(), (double)avector3f[0].y(), (double)avector3f[0].z()).m_7421_(u1, v1).m_85950_(this.rCol, this.gCol, this.bCol, alpha).m_85969_(lightColor).m_5752_();
         vertexConsumer.m_5483_((double)avector3f[1].x(), (double)avector3f[1].y(), (double)avector3f[1].z()).m_7421_(u1, v0).m_85950_(this.rCol, this.gCol, this.bCol, alpha).m_85969_(lightColor).m_5752_();
         vertexConsumer.m_5483_((double)avector3f[2].x(), (double)avector3f[2].y(), (double)avector3f[2].z()).m_7421_(u0, v0).m_85950_(this.rCol, this.gCol, this.bCol, alpha).m_85969_(lightColor).m_5752_();
         vertexConsumer.m_5483_((double)avector3f[3].x(), (double)avector3f[3].y(), (double)avector3f[3].z()).m_7421_(u0, v1).m_85950_(this.rCol, this.gCol, this.bCol, alpha).m_85969_(lightColor).m_5752_();
      }

   }

   private void renderBlade(float dx, float dy, float dz, Quaternionf rotation, float[][] beamSegments, VertexConsumer vertexConsumer, float progress, int lightColor, SpriteUVProvider spriteUVProvider) {
      float beamSegmentWidth = this.bladeWidthProvider.getValue(progress) * 0.5F;
      float alpha = this.alphaProvider.getValue(progress);
      int numBeamSpriteSegments = beamSegments.length;
      float[] uv = spriteUVProvider.getSpriteUV(progress * (float)this.lifetimeNanos);
      float spriteWidth = uv[2] - uv[0];
      float spriteSegmentWidth = spriteWidth * (this.bladeTextureMaxU - this.bladeTextureMinU) / (float)numBeamSpriteSegments;

      for(int beamSegmentIndex = 0; beamSegmentIndex < numBeamSpriteSegments; ++beamSegmentIndex) {
         float u0 = uv[0] + spriteWidth * this.bladeTextureMinU + spriteSegmentWidth * (float)beamSegmentIndex;
         float v0 = uv[1];
         float u1 = u0 + spriteSegmentWidth;
         float v1 = uv[3];
         float beamSegmentStart = beamSegments[beamSegmentIndex][0];
         float beamSegmentEnd = beamSegments[beamSegmentIndex][1];
         float beamSegmentLengthHalf = (beamSegmentEnd - beamSegmentStart) * 0.5F;
         Vector3f[] avector3f = new Vector3f[]{new Vector3f(0.0F, beamSegmentWidth, beamSegmentLengthHalf), new Vector3f(0.0F, beamSegmentWidth, -beamSegmentLengthHalf), new Vector3f(0.0F, -beamSegmentWidth, -beamSegmentLengthHalf), new Vector3f(0.0F, -beamSegmentWidth, beamSegmentLengthHalf)};

         int k;
         for(k = 0; k < 4; ++k) {
            Vector3f vector3f = avector3f[k];
            vector3f.rotate(rotation);
            vector3f.add(dx, dy, dz);
         }

         for(k = 0; k < this.bladeBrightness; ++k) {
            vertexConsumer.m_5483_((double)avector3f[0].x(), (double)avector3f[0].y(), (double)avector3f[0].z()).m_7421_(u1, v1).m_85950_(this.rCol, this.gCol, this.bCol, alpha).m_85969_(lightColor).m_5752_();
            vertexConsumer.m_5483_((double)avector3f[1].x(), (double)avector3f[1].y(), (double)avector3f[1].z()).m_7421_(u1, v0).m_85950_(this.rCol, this.gCol, this.bCol, alpha).m_85969_(lightColor).m_5752_();
            vertexConsumer.m_5483_((double)avector3f[2].x(), (double)avector3f[2].y(), (double)avector3f[2].z()).m_7421_(u0, v0).m_85950_(this.rCol, this.gCol, this.bCol, alpha).m_85969_(lightColor).m_5752_();
            vertexConsumer.m_5483_((double)avector3f[3].x(), (double)avector3f[3].y(), (double)avector3f[3].z()).m_7421_(u0, v1).m_85950_(this.rCol, this.gCol, this.bCol, alpha).m_85969_(lightColor).m_5752_();
         }
      }

   }

   public void launch(Entity player) {
      EffectParticles.EffectParticle particle = new EffectParticles.EffectParticle(player, this);
      Minecraft mc = Minecraft.m_91087_();
      mc.f_91061_.m_107344_(particle);
   }

   public static class Builder extends AbstractEffectBuilder<Builder, DetachedProjectileEffect> {
      private static final Set<GunItem.FirePhase> COMPATIBLE_PHASES;
      public static final long DEFAULT_DURATION = 250L;
      public static final float MAX_DISTANCE = 200.0F;
      public static final int DEFAULT_NUM_BLADES = 0;
      public static final int DEFAULT_NUM_SPRITES = 1;
      public static final int DEFAULT_FACE_BRIGHTNESS = 2;
      public static final int DEFAULT_BLADE_BRIGHTNESS = 2;
      public static final float DEFAULT_DETACH_AFTER = 1.0F;
      public static final int MAX_SPRITES = 200;
      public static final int MAX_BRIGHTNESS = 5;
      public static final int DEFAULT_SHOTS_PER_TRACE = 1;
      public static final SegmentsProviders.SegmentsProvider DEFAULT_SEGMENTS_PROVIDER;
      private Interpolators.FloatInterpolator bladeWidthProvider;
      private Interpolators.FloatInterpolator faceWidthProvider;
      private int bladeBrightness;
      private int faceBrightness;
      private SegmentsProviders.SegmentsProvider segmentsProvider;
      private int numBlades;
      private boolean isFaceEnabled;
      private float faceTextureMinU;
      private float faceTextureMaxU;
      private float bladeTextureMinU;
      private float bladeTextureMaxU;

      public Builder() {
         this.duration = 250L;
         this.numBlades = 0;
         this.faceBrightness = 2;
         this.bladeBrightness = 2;
         this.segmentsProvider = DEFAULT_SEGMENTS_PROVIDER;
         this.faceTextureMinU = 0.0F;
         this.faceTextureMaxU = 1.0F;
         this.bladeTextureMinU = 0.0F;
         this.bladeTextureMaxU = 1.0F;
      }

      public Builder withWidth(double width) {
         this.withWidthProvider(new Interpolators.EaseInEaseOutFloatProvider((float)width));
         return this;
      }

      public Builder withWidthProvider(Interpolators.FloatInterpolator widthProvider) {
         this.withBladeWidthProvider(widthProvider);
         this.withFaceWidthProvider(widthProvider);
         return this;
      }

      public Builder withBladeWidthProvider(Interpolators.FloatInterpolator widthProvider) {
         this.bladeWidthProvider = widthProvider;
         return this;
      }

      public Builder withFaceWidthProvider(Interpolators.FloatInterpolator widthProvider) {
         this.faceWidthProvider = widthProvider;
         return this;
      }

      public Builder withBlades(int numBlades, float bladeTextureMinU, float bladeTextureMaxU) {
         this.numBlades = numBlades;
         this.bladeTextureMinU = bladeTextureMinU;
         this.bladeTextureMaxU = bladeTextureMaxU;
         return this;
      }

      public Builder withBlade(float bladeTextureMinU, float bladeTextureMaxU) {
         return this.withBlades(1, bladeTextureMinU, bladeTextureMaxU);
      }

      public Builder withBlades(int numBlades) {
         return this.withBlades(numBlades, 0.0F, 1.0F);
      }

      public Builder withFace(float faceTextureMinU, float faceTextureMaxU) {
         this.faceTextureMinU = faceTextureMinU;
         this.faceTextureMaxU = faceTextureMaxU;
         this.isFaceEnabled = true;
         return this;
      }

      public Builder withBrightness(int brightness) {
         this.faceBrightness = brightness;
         this.bladeBrightness = brightness;
         return this;
      }

      public Builder withFaceBrightness(int brightness) {
         this.faceBrightness = Mth.m_14045_(brightness, 0, 5);
         return this;
      }

      public Builder withBladeBrightness(int brightness) {
         this.bladeBrightness = Mth.m_14045_(brightness, 0, 5);
         return this;
      }

      public Builder withSegmentsProvider(SegmentsProviders.SegmentsProvider segmentsProvider) {
         this.segmentsProvider = segmentsProvider;
         return this;
      }

      public Builder withJsonObject(JsonObject obj) {
         super.withJsonObject(obj);
         JsonObject faceObj = obj.getAsJsonObject("face");
         float initialSpeed;
         if (faceObj != null) {
            float minU = JsonUtil.getJsonFloat(faceObj, "minU", 0.0F);
            initialSpeed = JsonUtil.getJsonFloat(faceObj, "maxU", 1.0F);
            this.withFace(minU, initialSpeed);
            int brightness = JsonUtil.getJsonInt(faceObj, "brightness", 1);
            this.withFaceBrightness(brightness);
            Interpolators.FloatInterpolator widthInt = JsonUtil.getJsonInterpolator(faceObj, "width");
            if (widthInt != null) {
               this.withFaceWidthProvider(widthInt);
            }
         }

         JsonObject bladeObj = obj.getAsJsonObject("blade");
         float acceleration;
         if (bladeObj != null) {
            int count = JsonUtil.getJsonInt(bladeObj, "count", 1);
            acceleration = JsonUtil.getJsonFloat(bladeObj, "minU", 0.0F);
            float maxU = JsonUtil.getJsonFloat(bladeObj, "maxU", 1.0F);
            this.withBlades(count, acceleration, maxU);
            int brightness = JsonUtil.getJsonInt(bladeObj, "brightness", 1);
            this.withBladeBrightness(brightness);
            Interpolators.FloatInterpolator widthInt = JsonUtil.getJsonInterpolator(bladeObj, "width");
            if (widthInt != null) {
               this.withBladeWidthProvider(widthInt);
            }
         }

         initialSpeed = JsonUtil.getJsonFloat(obj, "initialSpeed", 0.0F);
         acceleration = JsonUtil.getJsonFloat(obj, "acceleration", 0.0F);
         if (this.numBlades > 0) {
            if (MiscUtil.isNearlyZero((double)initialSpeed) && MiscUtil.isNearlyZero((double)acceleration)) {
               this.withSegmentsProvider(new SegmentsProviders.StaticBeamSegmentsProvider());
            } else {
               this.withSegmentsProvider(new SegmentsProviders.MovingSegmentsProvider(initialSpeed, acceleration));
            }
         } else if (MiscUtil.isNearlyZero((double)initialSpeed) && MiscUtil.isNearlyZero((double)acceleration)) {
            this.withSegmentsProvider(SegmentsProviders.ZERO_PROVIDER);
         } else {
            this.withSegmentsProvider(new SegmentsProviders.MovingPointProvider(initialSpeed, acceleration));
         }

         return this;
      }

      public boolean isEffectAttached() {
         return false;
      }

      @OnlyIn(Dist.CLIENT)
      public DetachedProjectileEffect build(Context context) {
         DetachedProjectileEffect effect = new DetachedProjectileEffect();
         super.apply(effect, context);
         effect.startPosition = (Vec3)effect.startPositionProvider.get();
         effect.targetPosition = context.getHitResult().m_82450_();
         effect.distanceToTarget = (float)effect.startPosition.m_82554_(effect.targetPosition);
         effect.faceWidthProvider = this.faceWidthProvider;
         effect.bladeWidthProvider = this.bladeWidthProvider;
         effect.isFaceEnabled = this.isFaceEnabled;
         effect.numBlades = this.numBlades;
         effect.bladeTextureMinU = this.bladeTextureMinU;
         effect.bladeTextureMaxU = this.bladeTextureMaxU;
         effect.faceTextureMinU = this.faceTextureMinU;
         effect.faceTextureMaxU = this.faceTextureMaxU;
         effect.faceBrightness = this.faceBrightness;
         effect.bladeBrightness = this.bladeBrightness;
         effect.segmentsProvider = this.segmentsProvider;
         Function<VertexConsumer, VertexConsumer> contextVertexConsumerTransformer = context.getVertexConsumerTransformer();
         effect.vertexConsumerTransformer = contextVertexConsumerTransformer != null ? contextVertexConsumerTransformer : VertexConsumers.PARTICLE;
         effect.alphaProvider = this.alphaProvider;
         return effect;
      }

      public Collection<GunItem.FirePhase> getCompatiblePhases() {
         return COMPATIBLE_PHASES;
      }

      static {
         COMPATIBLE_PHASES = Set.of(GunItem.FirePhase.PREPARING, GunItem.FirePhase.FIRING, GunItem.FirePhase.HIT_SCAN_ACQUIRED, GunItem.FirePhase.COMPLETETING);
         DEFAULT_SEGMENTS_PROVIDER = new SegmentsProviders.MovingSegmentsProvider(200.0F, -10.0F);
      }
   }
}
