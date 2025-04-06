package com.vicmatskiv.pointblank.client.controller;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.vicmatskiv.pointblank.client.GunClientState;
import com.vicmatskiv.pointblank.client.render.GunItemRenderer;
import com.vicmatskiv.pointblank.item.GunItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import software.bernie.geckolib.cache.object.GeoBone;

public class RotationAnimationController extends AbstractProceduralAnimationController {
   private static float MODEL_SCALE = 0.0625F;
   private Phase phase;
   private long phaseStartTime;
   private double phaseStartDistance;
   private double rotationsPerSecond;
   private double decelerationRate;
   private double accelerationRate;
   private PhaseMapper phaseMapper;
   private DistanceFunction phaseDistanceFunction;

   private RotationAnimationController() {
      super(0L);
      this.phase = Phase.IDLE;
      this.rotationsPerSecond = 5.0D;
      this.decelerationRate = 5.0D;
      this.accelerationRate = 1.0D;
      this.phaseDistanceFunction = ZeroDistance.INSTANCE;
   }

   public void onStateTick(LivingEntity player, GunClientState state) {
      GunClientState.FireState fireState = state.getFireState();
      Phase newPhase = this.phaseMapper.getPhase(fireState);
      if (newPhase != this.phase) {
         double elapsedPhaseTime = (double)(System.nanoTime() - this.phaseStartTime) / 1.0E9D;
         double previousVelocity = this.phaseDistanceFunction.getVelocity(elapsedPhaseTime);
         double previousPhaseDurationSeconds = (double)(System.nanoTime() - this.phaseStartTime) / 1.0E9D;
         double previousPhaseDistance = this.phaseDistanceFunction.getDistance(previousPhaseDurationSeconds);
         this.phaseStartDistance += previousPhaseDistance;
         this.phaseStartTime = System.nanoTime();
         switch(newPhase) {
         case IDLE:
            this.phaseDistanceFunction = new DecelerationDistance(previousVelocity, this.decelerationRate);
            break;
         case PREPARING:
            this.phaseDistanceFunction = new AccelerationDistance(previousVelocity, this.accelerationRate);
            break;
         case RUNNING:
            this.phaseDistanceFunction = new AccelerationDistance(previousVelocity, this.accelerationRate);
            break;
         case COMPLETING:
            this.phaseDistanceFunction = new DecelerationDistance(previousVelocity, this.decelerationRate);
         }

         this.phase = newPhase;
      }

   }

   public void render(GunItemRenderer renderer, GunClientState gunClientState, PoseStack poseStack, GeoBone bone, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
      poseStack.m_85836_();
      float xPivot = bone.getPivotX() * MODEL_SCALE;
      float yPivot = bone.getPivotY() * MODEL_SCALE;
      float zPivot = bone.getPivotZ() * MODEL_SCALE;
      double elapsedPhaseTime = (double)(System.nanoTime() - this.phaseStartTime) / 1.0E9D;
      double elapsedPhaseDistance = this.phaseDistanceFunction.getDistance(elapsedPhaseTime);
      double distance = this.phaseStartDistance + elapsedPhaseDistance;
      double currentDistanceInDegress = this.rotationsPerSecond * distance * 360.0D;
      poseStack.m_252880_(xPivot, yPivot, zPivot);
      poseStack.m_252781_((new Quaternionf()).rotationXYZ(0.017453292F * (float)(0.0D + 0.0D * this.progress), 0.017453292F * (float)(0.0D + 0.0D * this.progress), 0.017453292F * (float)(0.0D - currentDistanceInDegress)));
      poseStack.m_252880_(-xPivot, -yPivot, -zPivot);
      renderer.renderCubesOfBoneParent(poseStack, bone, buffer, packedLight, packedOverlay, red, green, blue, alpha);
      poseStack.m_85849_();
   }

   public void renderRecursively(GunItemRenderer renderer, PoseStack poseStack, GunItem animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
      poseStack.m_85836_();
      float xPivot = bone.getPivotX() * MODEL_SCALE;
      float yPivot = bone.getPivotY() * MODEL_SCALE;
      float zPivot = bone.getPivotZ() * MODEL_SCALE;
      double elapsedPhaseTime = (double)(System.nanoTime() - this.phaseStartTime) / 1.0E9D;
      double elapsedPhaseDistance = this.phaseDistanceFunction.getDistance(elapsedPhaseTime);
      double distance = this.phaseStartDistance + elapsedPhaseDistance;
      double currentDistanceInDegress = this.rotationsPerSecond * distance * 360.0D;
      poseStack.m_252880_(xPivot, yPivot, zPivot);
      poseStack.m_252781_((new Quaternionf()).rotationXYZ(0.017453292F * (float)(0.0D + 0.0D * this.progress), 0.017453292F * (float)(0.0D + 0.0D * this.progress), 0.017453292F * (float)(0.0D - currentDistanceInDegress)));
      poseStack.m_252880_(-xPivot, -yPivot, -zPivot);
      renderer.renderRecursivelySuper(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
      poseStack.m_85849_();
   }

   public static enum Phase {
      IDLE,
      PREPARING,
      RUNNING,
      COMPLETING;

      // $FF: synthetic method
      private static Phase[] $values() {
         return new Phase[]{IDLE, PREPARING, RUNNING, COMPLETING};
      }
   }

   private static class ZeroDistance implements DistanceFunction {
      static final ZeroDistance INSTANCE = new ZeroDistance();

      public double getDistance(double t) {
         return 0.0D;
      }

      public double getVelocity(double t) {
         return 0.0D;
      }
   }

   private interface DistanceFunction {
      double getDistance(double var1);

      double getVelocity(double var1);
   }

   public interface PhaseMapper {
      Phase getPhase(GunClientState.FireState var1);
   }

   private static class DecelerationDistance implements DistanceFunction {
      private double deceleration;
      private double initialVelocity;

      public DecelerationDistance(double initialVelocity, double deceleration) {
         this.deceleration = deceleration;
         this.initialVelocity = initialVelocity;
      }

      public double getDistance(double t) {
         double finalVelocity = this.initialVelocity - t * this.deceleration;
         if (finalVelocity < 0.0D) {
            double tStop = this.initialVelocity / this.deceleration;
            return this.initialVelocity * tStop - 0.5D * this.deceleration * tStop * tStop;
         } else {
            return this.initialVelocity * t - 0.5D * this.deceleration * t * t;
         }
      }

      public double getVelocity(double t) {
         double finalVelocity = this.initialVelocity - t * this.deceleration;
         return Math.max(finalVelocity, 0.0D);
      }
   }

   private static class AccelerationDistance implements DistanceFunction {
      private double acceleration;
      private double initialVelocity;

      public AccelerationDistance(double initialVelocity, double acceleration) {
         this.initialVelocity = initialVelocity;
         this.acceleration = acceleration;
      }

      public double getDistance(double t) {
         double distanceWithConstantSpeed = 0.0D;
         double finalVelocity = this.initialVelocity + this.acceleration * t;
         double tMax;
         if (finalVelocity > 1.0D) {
            tMax = Math.max((1.0D - this.initialVelocity) / this.acceleration, 0.0D);
            distanceWithConstantSpeed = t - tMax;
         } else {
            tMax = t;
         }

         return this.initialVelocity * tMax + 0.5D * this.acceleration * tMax * tMax + distanceWithConstantSpeed;
      }

      public double getVelocity(double t) {
         double finalVelocity = this.initialVelocity + this.acceleration * t;
         return Math.min(finalVelocity, 1.0D);
      }
   }

   public static class Builder {
      private String modelPartName;
      private double rotationsPerSecond = 5.0D;
      private double decelerationRate = 5.0D;
      private double accelerationRate = 1.0D;
      private PhaseMapper phaseMapper;

      public Builder() {
         this.phaseMapper = FirePhaseMapper.INSTANCE;
      }

      public String getModelPartName() {
         return this.modelPartName;
      }

      public Builder withPhase(String phase) {
         byte var3 = -1;
         switch(phase.hashCode()) {
         case -934641255:
            if (phase.equals("reload")) {
               var3 = 1;
            }
            break;
         case 3143222:
            if (phase.equals("fire")) {
               var3 = 0;
            }
         }

         switch(var3) {
         case 0:
            this.phaseMapper = FirePhaseMapper.INSTANCE;
            break;
         case 1:
            this.phaseMapper = ReloadPhaseMapper.INSTANCE;
            break;
         default:
            throw new IllegalArgumentException("Invalid phase name: " + phase);
         }

         return this;
      }

      public Builder withPhaseMapper(PhaseMapper phaseMapper) {
         this.phaseMapper = phaseMapper;
         return this;
      }

      public Builder withModelPart(String modelPartName) {
         this.modelPartName = modelPartName;
         return this;
      }

      public Builder withDecelerationRate(double decelerationRate) {
         this.decelerationRate = decelerationRate;
         return this;
      }

      public Builder withAccelerationRate(double accelerationRate) {
         this.accelerationRate = accelerationRate;
         return this;
      }

      public Builder withRotationsPerMinute(double rotationsPerMinute) {
         this.rotationsPerSecond = rotationsPerMinute / 60.0D;
         return this;
      }

      public RotationAnimationController build() {
         if (this.modelPartName == null) {
            throw new IllegalStateException("Invalid rotation configutaion, missing model part name");
         } else {
            RotationAnimationController controller = new RotationAnimationController();
            controller.accelerationRate = this.accelerationRate;
            controller.decelerationRate = this.decelerationRate;
            controller.rotationsPerSecond = this.rotationsPerSecond;
            controller.phaseMapper = this.phaseMapper;
            return controller;
         }
      }
   }

   private static class ConstantDistance implements DistanceFunction {
      static final ConstantDistance INSTANCE = new ConstantDistance();

      public double getDistance(double t) {
         return t;
      }

      public double getVelocity(double t) {
         return t;
      }
   }

   private static class ReloadPhaseMapper implements PhaseMapper {
      static final ReloadPhaseMapper INSTANCE = new ReloadPhaseMapper();

      public Phase getPhase(GunClientState.FireState fireState) {
         Phase phase;
         switch(fireState) {
         case PREPARE_RELOAD:
         case PREPARE_RELOAD_COOLDOWN:
         case PREPARE_RELOAD_ITER:
         case PREPARE_RELOAD_COOLDOWN_ITER:
            phase = Phase.PREPARING;
            break;
         case RELOAD:
         case RELOAD_COOLDOWN:
         case RELOAD_ITER:
         case RELOAD_COOLDOWN_ITER:
            phase = Phase.RUNNING;
            break;
         case COMPLETE_RELOAD:
         case COMPLETE_RELOAD_COOLDOWN:
            phase = Phase.COMPLETING;
            break;
         default:
            phase = Phase.IDLE;
         }

         return phase;
      }
   }

   private static class FirePhaseMapper implements PhaseMapper {
      static final FirePhaseMapper INSTANCE = new FirePhaseMapper();

      public Phase getPhase(GunClientState.FireState fireState) {
         Phase phase;
         switch(fireState) {
         case PREPARE_FIRE_SINGLE:
         case PREPARE_FIRE_COOLDOWN_SINGLE:
         case PREPARE_FIRE_AUTO:
         case PREPARE_FIRE_COOLDOWN_AUTO:
         case PREPARE_FIRE_BURST:
         case PREPARE_FIRE_COOLDOWN_BURST:
            phase = Phase.PREPARING;
            break;
         case FIRE_SINGLE:
         case FIRE_COOLDOWN_SINGLE:
         case FIRE_AUTO:
         case FIRE_COOLDOWN_AUTO:
         case FIRE_BURST:
         case FIRE_COOLDOWN_BURST:
            phase = Phase.RUNNING;
            break;
         case COMPLETE_FIRE:
         case COMPLETE_FIRE_COOLDOWN:
            phase = Phase.COMPLETING;
            break;
         default:
            phase = Phase.IDLE;
         }

         return phase;
      }
   }
}
