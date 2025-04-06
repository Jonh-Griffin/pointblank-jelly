package com.vicmatskiv.pointblank.util;

import net.minecraft.world.phys.Vec3;

public class DirectAttackTrajectory implements Trajectory<DirectAttackTrajectory.Phase> {
   private double gravity;
   private Vec3 deltaMovement;
   private Vec3 endOfTickPosition;
   private Vec3 startOfTickPosition;

   public DirectAttackTrajectory(Vec3 startPosition, Vec3 deltaMovement, double gravity) {
      this.deltaMovement = deltaMovement;
      this.startOfTickPosition = startPosition;
      this.endOfTickPosition = startPosition;
      this.gravity = gravity;
   }

   public Vec3 getStartOfTickPosition() {
      return this.startOfTickPosition;
   }

   public Vec3 getEndOfTickPosition() {
      return this.endOfTickPosition;
   }

   public Vec3 getDeltaMovement() {
      return this.deltaMovement;
   }

   public void setTargetPosition(Vec3 updatedTargetPosition) {
   }

   public void tick() {
      this.startOfTickPosition = this.endOfTickPosition;
      this.deltaMovement = this.deltaMovement.m_82492_(0.0D, this.gravity, 0.0D);
      this.endOfTickPosition = this.startOfTickPosition.m_82549_(this.deltaMovement);
   }

   public boolean isCompleted() {
      return false;
   }

   public static enum Phase {
      NONE;

      // $FF: synthetic method
      private static Phase[] $values() {
         return new Phase[]{NONE};
      }
   }
}
