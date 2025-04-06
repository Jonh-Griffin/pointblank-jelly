package com.vicmatskiv.pointblank.client.effect;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.vicmatskiv.pointblank.client.GunClientState;
import com.vicmatskiv.pointblank.client.uv.SpriteUVProvider;
import com.vicmatskiv.pointblank.item.GunItem;
import com.vicmatskiv.pointblank.util.TimeUnit;
import java.util.Collection;
import java.util.Set;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class MuzzleFlashEffect extends AbstractEffect {
   private GunClientState gunState;
   private long startTime;
   private long nanoDuration;
   private SpriteUVProvider spriteUVProvider;

   public boolean isExpired() {
      return System.nanoTime() - this.startTime > this.nanoDuration;
   }

   private float getProgress() {
      float progress = (float)((double)(System.nanoTime() - this.startTime) / (double)this.nanoDuration);
      return Mth.m_14036_(progress, 0.0F, 1.0F);
   }

   public void render(EffectRenderContext effectRenderContext) {
      PoseStack poseStack = effectRenderContext.getPoseStack();
      if (poseStack != null) {
         poseStack.m_85836_();
         this.renderQuad(poseStack, effectRenderContext.getPosition(), effectRenderContext.getVertexBuffer(), effectRenderContext.getLightColor(), 1.0F, 1.0F, 1.0F, 1.0F, this.getProgress());
         poseStack.m_85849_();
      }
   }

   protected void renderQuad(PoseStack poseStack, Vec3 position, VertexConsumer buffer, int packedLight, float red, float green, float blue, float alpha, float progress) {
      Matrix4f poseState = poseStack.m_85850_().m_252922_();
      this.createVerticesOfQuad(position, poseState, buffer, packedLight, red, green, blue, progress);
   }

   protected void createVerticesOfQuad(Vec3 position, Matrix4f poseState, VertexConsumer buffer, int packedLight, float red, float green, float blue, float progress) {
      float[] uv = this.spriteUVProvider.getSpriteUV(progress);
      float alpha = this.alphaProvider.getValue(progress);
      if (uv != null) {
         float minU = uv[0];
         float minV = uv[1];
         float maxU = uv[2];
         float maxV = uv[3];
         float[][] texUV = new float[][]{{maxU, maxV}, {minU, maxV}, {minU, minV}, {maxU, minV}};
         float expand = this.widthProvider.getValue(progress);
         float zOffset = 0.0F;
         float[][] positionOffsets = new float[][]{{expand, -expand, zOffset}, {-expand, -expand, zOffset}, {-expand, expand, zOffset}, {expand, expand, zOffset}};
         if (this.isGlowEnabled) {
            packedLight = 240;
         }

         for(int i = 0; i < 4; ++i) {
            buffer.m_252986_(poseState, (float)position.m_7096_() + positionOffsets[i][0], (float)position.m_7098_() + positionOffsets[i][1], (float)position.m_7094_() + positionOffsets[i][2]).m_85950_(red, green, blue, alpha).m_7421_(texUV[i][0], texUV[i][1]).m_86008_(0).m_85969_(packedLight).m_5601_(0.0F, 1.0F, 0.0F).m_5752_();
         }

      }
   }

   public void launch(Entity player) {
      this.gunState.addMuzzleEffect(this);
   }

   public boolean hasInfiniteBounds() {
      return false;
   }

   public static class Builder extends AbstractEffectBuilder<Builder, MuzzleFlashEffect> {
      private static final Set<GunItem.FirePhase> COMPATIBLE_PHASES;

      public Collection<GunItem.FirePhase> getCompatiblePhases() {
         return COMPATIBLE_PHASES;
      }

      public boolean isEffectAttached() {
         return true;
      }

      public Builder withJsonObject(JsonObject obj) {
         return (Builder)super.withJsonObject(obj);
      }

      public MuzzleFlashEffect build(Context context) {
         MuzzleFlashEffect effect = new MuzzleFlashEffect();
         super.apply(effect, context);
         effect.gunState = context.getGunClientState();
         effect.startTime = System.nanoTime();
         effect.spriteUVProvider = effect.getSpriteUVProvider();
         effect.nanoDuration = TimeUnit.MILLISECOND.toNanos(effect.duration);
         return effect;
      }

      static {
         COMPATIBLE_PHASES = Set.of(GunItem.FirePhase.FIRING);
      }
   }
}
