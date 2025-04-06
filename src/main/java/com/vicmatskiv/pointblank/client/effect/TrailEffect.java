package com.vicmatskiv.pointblank.client.effect;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.vicmatskiv.pointblank.client.particle.EffectParticles;
import com.vicmatskiv.pointblank.client.uv.SpriteUVProvider;
import com.vicmatskiv.pointblank.item.GunItem;
import com.vicmatskiv.pointblank.util.JsonUtil;
import java.util.Collection;
import java.util.Collections;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class TrailEffect extends AbstractEffect {
   private Vec3 previousPosition;
   private Vec3 previousVelocity;
   private double longitudeOffset;

   public boolean hasInfiniteBounds() {
      return false;
   }

   public void render(EffectRenderContext effectRenderContext) {
      TrailRenderContext trailRenderContext = (TrailRenderContext)effectRenderContext;
      float progress = effectRenderContext.getProgress();
      float partialTick = effectRenderContext.getPartialTick();
      Camera camera = effectRenderContext.getCamera();
      Vec3 cameraPos = camera.m_90583_();
      Vector3f viewDirection3f = camera.m_253058_();
      Vec3 viewDirection = new Vec3((double)viewDirection3f.x, (double)viewDirection3f.y, (double)viewDirection3f.z);
      Vec3 cameraUpVector = new Vec3(0.0D, 1.0D, 0.0D);
      Vec3 cameraRightVector = viewDirection.m_82537_(cameraUpVector);
      Vec3 position = effectRenderContext.getPosition();
      Vec3 velocity = effectRenderContext.getVelocity();
      Vec3 r2 = viewDirection.m_82537_(velocity).m_82541_();
      Vec3 up2 = r2;
      if (r2.m_82556_() < 1.0E-5D) {
         up2 = cameraRightVector;
      }

      float halfWidth = this.widthProvider.getValue(progress) * 0.5F;
      Vec3 bottomLeft;
      Vec3 topLeft;
      Vec3 bottomRight;
      Vec3 prevUp2;
      if (trailRenderContext.prevVelocity != null && trailRenderContext.prevPos != null) {
         bottomRight = viewDirection.m_82537_(trailRenderContext.prevVelocity).m_82541_();
         prevUp2 = bottomRight;
         if (bottomRight.m_82556_() < 1.0E-5D) {
            prevUp2 = cameraRightVector;
         }

         bottomLeft = this.getVerticePos(trailRenderContext.prevPos, trailRenderContext.prevVelocity, prevUp2, 1.0F, -halfWidth);
         topLeft = this.getVerticePos(trailRenderContext.prevPos, trailRenderContext.prevVelocity, prevUp2, 1.0F, halfWidth);
      } else {
         bottomLeft = this.getVerticePos(position, velocity, up2, 0.0F, -halfWidth);
         topLeft = this.getVerticePos(position, velocity, up2, 0.0F, halfWidth);
      }

      bottomRight = this.getVerticePos(position, velocity, up2, partialTick, -halfWidth);
      prevUp2 = this.getVerticePos(position, velocity, up2, partialTick, halfWidth);
      bottomLeft = bottomLeft.m_82546_(cameraPos);
      bottomRight = bottomRight.m_82546_(cameraPos);
      topLeft = topLeft.m_82546_(cameraPos);
      prevUp2 = prevUp2.m_82546_(cameraPos);
      SpriteUVProvider spriteUVProvider = effectRenderContext.getSpriteUVProvider();
      float[] uv = spriteUVProvider.getSpriteUV(progress);
      float u0 = uv[0];
      float v0 = uv[1];
      float u1 = uv[2];
      float v1 = uv[3];
      int lightColor = effectRenderContext.getLightColor();
      float alpha = this.alphaProvider.getValue(progress);
      float rCol = 1.0F;
      float gCol = 1.0F;
      float bCol = 1.0F;
      VertexConsumer vertexConsumer = effectRenderContext.getVertexBuffer();
      vertexConsumer.m_5483_(prevUp2.f_82479_, prevUp2.f_82480_, prevUp2.f_82481_).m_7421_(u1, v1).m_85950_(rCol, gCol, bCol, alpha).m_85969_(lightColor).m_5752_();
      vertexConsumer.m_5483_(bottomRight.f_82479_, bottomRight.f_82480_, bottomRight.f_82481_).m_7421_(u1, v0).m_85950_(rCol, gCol, bCol, alpha).m_85969_(lightColor).m_5752_();
      vertexConsumer.m_5483_(bottomLeft.f_82479_, bottomLeft.f_82480_, bottomLeft.f_82481_).m_7421_(u0, v0).m_85950_(rCol, gCol, bCol, alpha).m_85969_(lightColor).m_5752_();
      vertexConsumer.m_5483_(topLeft.f_82479_, topLeft.f_82480_, topLeft.f_82481_).m_7421_(u0, v1).m_85950_(rCol, gCol, bCol, alpha).m_85969_(lightColor).m_5752_();
   }

   private Vec3 getVerticePos(Vec3 position, Vec3 velocity, Vec3 side, float segmentProgress, float sideOffset) {
      Vec3 vertixPos = position.m_82549_(velocity.m_82490_((double)segmentProgress)).m_82549_(side.m_82490_((double)sideOffset));
      return vertixPos;
   }

   public void launch(Entity player) {
      Minecraft mc = Minecraft.m_91087_();
      Particle particle = new EffectParticles.EffectParticle(player, this);
      mc.f_91061_.m_107344_(particle);
   }

   public void launchNext(Entity owner, Vec3 position, Vec3 velocity) {
      Minecraft mc = Minecraft.m_91087_();
      Vec3 positionWithOffset = position.m_82546_(velocity.m_82541_().m_82490_(this.longitudeOffset));
      if (this.previousPosition != null) {
         Particle particle = new TrailParticle(owner, this, positionWithOffset, velocity, this.previousPosition, this.previousVelocity);
         mc.f_91061_.m_107344_(particle);
      }

      this.previousVelocity = velocity;
      this.previousPosition = positionWithOffset;
   }

   private static class TrailRenderContext extends EffectRenderContext {
      private Vec3 prevPos;
      private Vec3 prevVelocity;
   }

   private static class TrailParticle extends EffectParticles.EffectParticle {
      private Vec3 position;
      private Vec3 velocity;
      private Vec3 prevVelocity;
      private Vec3 prevPos;
      private float segmentProgress;

      TrailParticle(ClientLevel level, double x, double y, double z) {
         super(level, x, y, z);
      }

      public TrailParticle(Entity owner, TrailEffect effect, Vec3 position, Vec3 velocity, Vec3 prevPos, Vec3 prevVelocity) {
         super(owner, effect);
         this.m_107264_(position.f_82479_, position.f_82480_, position.f_82481_);
         this.f_107209_ = this.f_107212_;
         this.f_107210_ = this.f_107213_;
         this.f_107211_ = this.f_107214_;
         this.position = position;
         this.velocity = velocity;
         this.prevPos = prevPos;
         this.prevVelocity = prevVelocity;
      }

      public boolean shouldCull() {
         return false;
      }

      public void m_5744_(VertexConsumer vertexConsumer, Camera camera, float partialTick) {
         int lightColor = this.m_6355_(partialTick);
         float progress = this.getProgress(partialTick);
         if (this.segmentProgress < partialTick) {
            this.segmentProgress = partialTick;
         } else {
            this.segmentProgress = 1.0F;
         }

         TrailRenderContext effectRenderContext = (TrailRenderContext)(new TrailRenderContext()).withCamera(camera).withPosition(this.position).withVelocity(this.velocity).withVertexBuffer(vertexConsumer).withProgress(progress).withPartialTick(this.segmentProgress).withLightColor(lightColor).withSpriteUVProvider(this.spriteUVProvider);
         effectRenderContext.prevPos = this.prevPos;
         effectRenderContext.prevVelocity = this.prevVelocity;
         this.effect.render(effectRenderContext);
      }
   }

   public static class Builder extends AbstractEffectBuilder<Builder, TrailEffect> {
      private Collection<GunItem.FirePhase> compatiblePhases;
      private double longitudeOffset;

      public Builder() {
         this.compatiblePhases = Collections.singletonList(GunItem.FirePhase.FLYING);
      }

      public Builder withLongitudeOffset(double longitudeOffset) {
         this.longitudeOffset = longitudeOffset;
         return this;
      }

      public Builder withJsonObject(JsonObject obj) {
         super.withJsonObject(obj);
         this.withLongitudeOffset((double)JsonUtil.getJsonFloat(obj, "longitudeOffset", 0.0F));
         return this;
      }

      public Collection<GunItem.FirePhase> getCompatiblePhases() {
         return this.compatiblePhases;
      }

      public boolean isEffectAttached() {
         return false;
      }

      public TrailEffect build(Context effectContext) {
         TrailEffect effect = new TrailEffect();
         this.apply(effect, effectContext);
         effect.longitudeOffset = this.longitudeOffset;
         return effect;
      }
   }
}
