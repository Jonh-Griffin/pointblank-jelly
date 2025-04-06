package com.vicmatskiv.pointblank.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Quaterniond;
import org.joml.Vector3d;

public class TopDownAttackTrajectory implements Trajectory<TopDownAttackTrajectory.Phase> {
   private static final Logger LOGGER = LogManager.getFormatterLogger("pointblank");
   private static final double TIME_PER_TICK = 0.05D;
   private static final double DEFAULT_CLIMB_ACCELERATION = 0.25D;
   private static final double DEFAULT_SOFT_LAUNCH_CURVATURE = Math.toRadians(20.0D);
   private static final double DEFAULT_SOFT_LAUNCH_SPEED = 10.0D;
   private static final double MIN_CURVATURE_ANGLE = Math.toRadians(15.0D);
   private static final double COS_45 = Math.cos(0.7853981633974483D);
   private static final double MIN_ATTACK_ANGLE = Math.toRadians(30.0D);
   private static final double SOFT_LAUNCH_DISTANCE = 5.0D;
   private static final double MIN_DISTANCE_TO_TARGET_FOR_TOP_DOWN_ATTACK = 25.0D;
   private static final Vec3 UP = new Vec3(0.0D, 1.0D, 0.0D);
   private static final Vec3 DOWN = new Vec3(0.0D, -1.0D, 0.0D);
   private Vec3 targetPosition;
   private Vec3 startToTargetNormalized;
   private double defaultSpeed;
   private double currentSpeed;
   private double remainingClimbSpeed;
   private double climbToDescendTurnSpeed;
   private int currentTick;
   private int climbTicks;
   private int descendTicks;
   private double climbToDescendCurvature;
   private Vec3 climbVector;
   private Vec3 endOfClimbPosition;
   private Vec3 deltaMovement;
   private Vec3 startOfTickPosition;
   private Vec3 endOfTickPosition;
   private Vec3 verticalRotationAxis;
   private Phase phase;
   private Runnable pendingCorrection;
   private double wouldBeLaunchOffset;
   private double startClimbHOffset;
   private double startClimbVOffset;
   private double softLaunchCurvature;
   private int softLaunchTicks;
   private int softLaunchTurnTicks;
   private int softLaunchTicksBeforeTurn;
   private double defaultSoftLaunchSpeed;
   private double softLaunchSpeedBeforeTurn;
   private double climbAcceleration = 0.25D;
   private List<TrajectoryPhaseListener<Phase>> trajectoryPhaseListeners = new ArrayList();

   public static TopDownAttackTrajectory createTrajectory(Vec3 startPosition, Vec3 targetPosition, double defaultSpeedBlocksPerSecond) {
      LOGGER.trace("Initializing trajectory");
      Vec3 startToTarget = targetPosition.m_82546_(startPosition);
      Vec3 startToTargetNormalized = startToTarget.m_82541_();
      Vec3 wouldBeClimbStartPosition = startPosition.m_82549_(startToTargetNormalized.m_82490_(5.0D));
      LOGGER.trace("Start of soft launch position: (%.5f, %.5f, %.5f)", startPosition.f_82479_, startPosition.f_82480_, startPosition.f_82481_);
      LOGGER.trace("End of soft launch position: (%.5f, %.5f, %.5f)", wouldBeClimbStartPosition.f_82479_, wouldBeClimbStartPosition.f_82480_, wouldBeClimbStartPosition.f_82481_);
      double climbToDescendCurvature = MIN_CURVATURE_ANGLE;
      double defaultSpeed = defaultSpeedBlocksPerSecond * 0.05D;
      double climbToDescendTurnSpeed = defaultSpeed * 0.75D;
      double distanceToTarget = startToTarget.m_82553_();
      if (distanceToTarget < 25.0D) {
         return null;
      } else {
         ClimbInfo climbInfo = getClimbInfo(startToTarget, targetPosition, wouldBeClimbStartPosition, climbToDescendCurvature, climbToDescendTurnSpeed);
         if (climbInfo == null) {
            return null;
         } else {
            LOGGER.trace("Climb to descend curvature: %.3f°", Math.toDegrees(climbToDescendCurvature));
            LOGGER.trace("Target pos: (%.5f, %.5f, %.5f)", targetPosition.f_82479_, targetPosition.f_82480_, targetPosition.f_82481_);
            Vec3 normalizedClimbVector = climbInfo.climbVector.m_82541_();
            LOGGER.trace("Original normalized climb vector: (%.3f, %.3f, %.3f)", normalizedClimbVector.f_82479_, normalizedClimbVector.f_82480_, normalizedClimbVector.f_82481_);
            TopDownAttackTrajectory topDownAttackTrajectory = new TopDownAttackTrajectory();
            initSoftLaunch(topDownAttackTrajectory, climbInfo);
            topDownAttackTrajectory.climbToDescendTurnSpeed = climbToDescendTurnSpeed;
            topDownAttackTrajectory.climbToDescendCurvature = climbToDescendCurvature;
            topDownAttackTrajectory.climbVector = climbInfo.climbVector;
            topDownAttackTrajectory.endOfClimbPosition = wouldBeClimbStartPosition.m_82549_(climbInfo.climbVector);
            LOGGER.trace("Start of climb position: (%.5f, %.5f, %.5f)", wouldBeClimbStartPosition.f_82479_, wouldBeClimbStartPosition.f_82480_, wouldBeClimbStartPosition.f_82481_);
            LOGGER.trace("End of climb position: (%.5f, %.5f, %.5f)", topDownAttackTrajectory.endOfClimbPosition.f_82479_, topDownAttackTrajectory.endOfClimbPosition.f_82480_, topDownAttackTrajectory.endOfClimbPosition.f_82481_);
            topDownAttackTrajectory.setPhase(Phase.SOFT_LAUNCH);
            topDownAttackTrajectory.targetPosition = targetPosition;
            topDownAttackTrajectory.startToTargetNormalized = startToTargetNormalized;
            topDownAttackTrajectory.defaultSpeed = defaultSpeedBlocksPerSecond * 0.05D;
            topDownAttackTrajectory.currentSpeed = defaultSpeed;
            topDownAttackTrajectory.startOfTickPosition = topDownAttackTrajectory.endOfTickPosition = startPosition;
            return topDownAttackTrajectory;
         }
      }
   }

   private static ClimbInfo getClimbInfo(Vec3 startToTarget, Vec3 targetPosition, Vec3 wouldBeClimbStartPosition, double climbToDescendCurvature, double climbToDescendTurnSpeed) {
      double downSimilarity = startToTarget.m_82541_().m_82526_(DOWN);
      if (downSimilarity > Math.cos(Math.toDegrees(10.0D))) {
         return null;
      } else {
         double radius = getRadius(climbToDescendTurnSpeed, climbToDescendCurvature);
         LOGGER.trace("Radius: %.5f", radius);
         Vec3 pivot = getPivot(wouldBeClimbStartPosition, targetPosition, radius, 0.0D);
         LOGGER.trace("Pivot: (%.5f, %.5f, %.5f)", pivot.f_82479_, pivot.f_82480_, pivot.f_82481_);
         return getClimbInfo(wouldBeClimbStartPosition, pivot, radius);
      }
   }

   private static void initSoftLaunch(TopDownAttackTrajectory topDownAttackTrajectory, ClimbInfo climbInfo) {
      double wouldBeLaunchOffset = 5.0D;
      double defaultSoftLaunchCurvature = DEFAULT_SOFT_LAUNCH_CURVATURE;
      double softLaunchSpeed = 0.5D;
      topDownAttackTrajectory.defaultSoftLaunchSpeed = softLaunchSpeed;
      if (defaultSoftLaunchCurvature > climbInfo.launchAngle) {
         defaultSoftLaunchCurvature = climbInfo.launchAngle / 10.0D;
      }

      double adjustedCurvature = MiscUtil.adjustDivisor(climbInfo.launchAngle, defaultSoftLaunchCurvature);
      if (Math.abs(adjustedCurvature - defaultSoftLaunchCurvature) > 1.0E-6D) {
         LOGGER.trace("Adjusted phi to: %.3f°", Math.toDegrees(adjustedCurvature));
      }

      double softLaunchRadius = softLaunchSpeed * 0.5D / Math.sin(adjustedCurvature * 0.5D);
      int n = (int)Math.round(climbInfo.launchAngle / adjustedCurvature);
      double dnm1 = 2.0D * softLaunchRadius * Math.sin(adjustedCurvature * (double)(n - 1) * 0.5D);
      double turnPointDistanceOffset = dnm1 * 0.5D / Math.cos(adjustedCurvature * (double)n * 0.5D);
      double distanceToTurn = wouldBeLaunchOffset - turnPointDistanceOffset;
      LOGGER.trace("phi: %.3f°, L: %.3f°", Math.toDegrees(adjustedCurvature), Math.toDegrees(climbInfo.launchAngle));
      double D = 2.0D * softLaunchRadius * Math.sin(adjustedCurvature * (double)n * 0.5D);
      topDownAttackTrajectory.startClimbHOffset = D * Math.cos(adjustedCurvature * (double)(n + 1) * 0.5D) + distanceToTurn;
      topDownAttackTrajectory.startClimbVOffset = D * Math.sin(adjustedCurvature * (double)(n + 1) * 0.5D);
      LOGGER.trace("Start climb at hv offsets: (%.3f, %.3f)", topDownAttackTrajectory.startClimbHOffset, topDownAttackTrajectory.startClimbVOffset);
      topDownAttackTrajectory.softLaunchSpeedBeforeTurn = MiscUtil.adjustDivisor(distanceToTurn, softLaunchSpeed);
      topDownAttackTrajectory.softLaunchTicksBeforeTurn = (int)Math.round(distanceToTurn / softLaunchSpeed);
      LOGGER.trace("Ticks before turn: %d, Ticks to turn: %d, R: %.3f, speed: %.3f, speed before turn: %.3f, ", topDownAttackTrajectory.softLaunchTicksBeforeTurn, n, softLaunchRadius, topDownAttackTrajectory.defaultSoftLaunchSpeed, topDownAttackTrajectory.softLaunchSpeedBeforeTurn);
      LOGGER.trace("Would be launch offset: %.3f, Turn offset:  %.3f", topDownAttackTrajectory.wouldBeLaunchOffset, distanceToTurn);
      topDownAttackTrajectory.softLaunchTurnTicks = n;
      topDownAttackTrajectory.softLaunchCurvature = adjustedCurvature;
      topDownAttackTrajectory.softLaunchTicks = topDownAttackTrajectory.softLaunchTicksBeforeTurn + topDownAttackTrajectory.softLaunchTurnTicks;
      LOGGER.trace("Total soft launch ticks: %d", topDownAttackTrajectory.softLaunchTicks);
   }

   private TopDownAttackTrajectory() {
   }

   public void addListener(TrajectoryPhaseListener<Phase> listener) {
      this.trajectoryPhaseListeners.add(listener);
   }

   public Phase getPhase() {
      return this.phase;
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

   public boolean isCompleted() {
      return this.phase == Phase.COMPLETED;
   }

   public void tick() {
      switch(this.phase) {
      case SOFT_LAUNCH:
         this.tickSoftLaunch();
         break;
      case CLIMB:
         this.tickClimb();
         break;
      case CLIMB_TO_DESCEND_TURN:
         this.tickTurn();
         break;
      case DESCEND:
         this.tickDescend();
      }

      this.startOfTickPosition = this.endOfTickPosition;
      this.endOfTickPosition = this.startOfTickPosition.m_82549_(this.deltaMovement);
      if (this.phase != Phase.COMPLETED) {
         if (this.pendingCorrection != null) {
            this.pendingCorrection.run();
            this.pendingCorrection = null;
         }

         this.debugDeltaMovement();
      }

      LOGGER.trace("Tick #%d, phase: %s, start: (%.5f, %.5f, %.5f), end: (%.5f, %.5f, %.5f), mv (%.5f, %.5f, %.5f)\n", new Object[]{this.currentTick, this.phase, this.startOfTickPosition.f_82479_, this.startOfTickPosition.f_82480_, this.startOfTickPosition.f_82481_, this.endOfTickPosition.f_82479_, this.endOfTickPosition.f_82480_, this.endOfTickPosition.f_82481_, this.deltaMovement.f_82479_, this.deltaMovement.f_82480_, this.deltaMovement.f_82481_});
      ++this.currentTick;
   }

   private void setPhase(Phase phase) {
      this.phase = phase;
      LOGGER.trace("Started phase: %s", phase);
      Iterator var2 = this.trajectoryPhaseListeners.iterator();

      while(var2.hasNext()) {
         TrajectoryPhaseListener<Phase> trajectoryPhaseListener = (TrajectoryPhaseListener)var2.next();
         trajectoryPhaseListener.onStartPhase(phase, this.endOfTickPosition);
      }

   }

   private void tickDescend() {
      --this.descendTicks;
      if (this.descendTicks < -5) {
         this.setPhase(Phase.COMPLETED);
      } else {
         if (this.currentSpeed < this.defaultSpeed) {
            this.currentSpeed += this.climbAcceleration;
         }

         if (this.checkTargetProximity()) {
            this.setPhase(Phase.COMPLETED);
         } else {
            Vec3 currentToTarget = this.correctTrajectory();
            this.deltaMovement = currentToTarget.m_82490_(this.currentSpeed);
         }
      }
   }

   private Vec3 correctTrajectory() {
      Vec3 currentToTarget = this.targetPosition.m_82546_(this.endOfTickPosition).m_82541_();
      double targetDirSimilarity = currentToTarget.m_82541_().m_82526_(this.deltaMovement.m_82541_());
      LOGGER.trace("Trajectory similarity: %.3f", targetDirSimilarity);
      Vec3 newHorizontalRotationAxis = currentToTarget.m_82537_(DOWN);
      double rotationAxisSimilarity;
      if (this.verticalRotationAxis != null) {
         rotationAxisSimilarity = newHorizontalRotationAxis.m_82541_().m_82526_(this.verticalRotationAxis.m_82541_());
         if (rotationAxisSimilarity > 0.0D && targetDirSimilarity < COS_45) {
            this.verticalRotationAxis = newHorizontalRotationAxis;
         }
      } else {
         this.verticalRotationAxis = newHorizontalRotationAxis;
      }

      rotationAxisSimilarity = Math.acos(targetDirSimilarity);
      double rotationAngle = Math.min(rotationAxisSimilarity, this.climbToDescendCurvature);
      double rotationAngleDegrees = Math.toDegrees(rotationAngle);
      LOGGER.trace("Descend rotation angle degrees: %.3f", rotationAngleDegrees);
      Quaterniond rotation = new Quaterniond();
      rotation.rotateTo(this.deltaMovement.f_82479_, this.deltaMovement.f_82480_, this.deltaMovement.f_82481_, currentToTarget.f_82479_, currentToTarget.f_82480_, currentToTarget.f_82481_);
      rotation.slerp(rotation, rotationAngle / this.climbToDescendCurvature);
      Vector3d dm = new Vector3d(this.deltaMovement.f_82479_, this.deltaMovement.f_82480_, this.deltaMovement.f_82481_);
      dm.rotate(rotation);
      return currentToTarget;
   }

   private boolean checkTargetProximity() {
      Vec3 startOfTickToTarget = this.targetPosition.m_82546_(this.startOfTickPosition);
      Vec3 endOfTickToTarget = this.targetPosition.m_82546_(this.endOfTickPosition);
      double magnitudeDeltaMovementSqr = this.deltaMovement.m_82556_();
      if (startOfTickToTarget.m_82556_() < magnitudeDeltaMovementSqr && endOfTickToTarget.m_82556_() < magnitudeDeltaMovementSqr) {
         Vec3 crossProduct = this.deltaMovement.m_82537_(startOfTickToTarget);
         double distanceFromTargetToTrajectory = crossProduct.m_82553_() / Math.sqrt(magnitudeDeltaMovementSqr);
         LOGGER.trace("Distance from trajectory to target: %.3f", distanceFromTargetToTrajectory);
         return distanceFromTargetToTrajectory < 0.5D;
      } else {
         return false;
      }
   }

   private void tickSoftLaunch() {
      if (this.softLaunchTicksBeforeTurn > 0) {
         LOGGER.trace("Soft launch ticks before turn left: %d", this.softLaunchTicksBeforeTurn);
         this.deltaMovement = this.startToTargetNormalized.m_82490_(this.softLaunchSpeedBeforeTurn);
         --this.softLaunchTicksBeforeTurn;
      } else if (this.softLaunchTurnTicks > 0) {
         LOGGER.trace("Soft launch turn ticks before climb: %d", this.softLaunchTurnTicks);
         Vec3 ndm = this.deltaMovement.m_82541_();
         this.deltaMovement = ndm.m_82490_(this.defaultSoftLaunchSpeed);
         Vec3 right = this.startToTargetNormalized.m_82537_(UP);
         Quaterniond rotation = new Quaterniond();
         LOGGER.trace("Rotating %.3f°", Math.toDegrees(this.softLaunchCurvature));
         rotation.rotateAxis(this.softLaunchCurvature, right.f_82479_, right.f_82480_, right.f_82481_);
         Vector3d dm = new Vector3d(this.deltaMovement.f_82479_, this.deltaMovement.f_82480_, this.deltaMovement.f_82481_);
         dm.rotate(rotation);
         this.deltaMovement = new Vec3(dm.x, dm.y, dm.z);
         --this.softLaunchTurnTicks;
      } else {
         this.prepareClimb();
         this.tickClimb();
      }

   }

   private void tickClimb() {
      LOGGER.trace("Climb tick #%d, speed: %.3f", this.climbTicks, this.currentSpeed);
      this.deltaMovement = (new Vec3(this.climbVector.f_82479_, this.climbVector.f_82480_, this.climbVector.f_82481_)).m_82541_().m_82490_(this.currentSpeed);
      if (this.climbTicks-- <= 0) {
         this.prepareTurn();
         this.tickTurn();
      } else if (this.currentSpeed < this.defaultSpeed - this.climbAcceleration) {
         this.currentSpeed += this.climbAcceleration;
      } else {
         this.currentSpeed = this.remainingClimbSpeed;
      }

   }

   private void tickTurn() {
      this.currentSpeed = this.climbToDescendTurnSpeed;
      LOGGER.trace("Speed: %.3f", this.currentSpeed);
      Vec3 currentToTarget = this.targetPosition.m_82546_(this.startOfTickPosition);
      Vec3 normalizedDeltaMovement = this.deltaMovement.m_82541_();
      this.deltaMovement = normalizedDeltaMovement.m_82490_(this.currentSpeed);
      double targetDirSimilarity = currentToTarget.m_82541_().m_82526_(normalizedDeltaMovement);
      double targetDirOffset = Math.acos(targetDirSimilarity);
      double targetDirOffsetDegrees = Math.toDegrees(targetDirOffset);
      LOGGER.trace("Target direction similarity: %.5f, degrees: %.5f", targetDirSimilarity, targetDirOffsetDegrees);
      double rotationAngle = this.climbToDescendCurvature;
      Vec3 newHorizontalRotationAxis = currentToTarget.m_82537_(DOWN);
      if (this.verticalRotationAxis != null) {
         double rotationAxisSimilarity = newHorizontalRotationAxis.m_82541_().m_82526_(this.verticalRotationAxis.m_82541_());
         if (rotationAxisSimilarity > 0.0D && targetDirSimilarity < COS_45) {
            this.verticalRotationAxis = newHorizontalRotationAxis;
         }
      } else {
         this.verticalRotationAxis = newHorizontalRotationAxis;
      }

      LOGGER.trace("Right: (%.5f, %.5f, %.5f)", this.verticalRotationAxis.f_82479_, this.verticalRotationAxis.f_82480_, this.verticalRotationAxis.f_82481_);
      if (targetDirOffsetDegrees < Math.toDegrees(this.climbToDescendCurvature)) {
         this.prepareDescend(this.verticalRotationAxis, targetDirOffset);
         this.tickDescend();
      } else {
         Quaterniond rotation = new Quaterniond();
         rotation.rotateAxis(rotationAngle, this.verticalRotationAxis.f_82479_, this.verticalRotationAxis.f_82480_, this.verticalRotationAxis.f_82481_);
         Vector3d dm = new Vector3d(this.deltaMovement.f_82479_, this.deltaMovement.f_82480_, this.deltaMovement.f_82481_);
         dm.rotate(rotation);
         this.deltaMovement = new Vec3(dm.x, dm.y, dm.z);
      }

   }

   private void debugDeltaMovement() {
      Vec3 directionToTarget = this.targetPosition.m_82546_(this.startOfTickPosition).m_82541_();
      LOGGER.trace("Direction to target: (%.3f, %.3f, %.3f)", directionToTarget.f_82479_, directionToTarget.f_82480_, directionToTarget.f_82481_);
      Vec3 directionToTargetRight = directionToTarget.m_82537_(UP).m_82541_();
      Vec3 updatedRight = this.deltaMovement.m_82537_(UP).m_82541_();
      double horizontalAngleOffset = Math.toDegrees(Math.acos(updatedRight.m_82526_(directionToTargetRight)));
      LOGGER.trace("Horizontal azimuth offset: %.3f°", horizontalAngleOffset);
   }

   public void setTargetPosition(Vec3 updatedTargetPosition) {
      if (!(updatedTargetPosition.m_82557_(this.targetPosition) < 0.001D)) {
         if (this.phase != Phase.SOFT_LAUNCH && this.phase != Phase.CLIMB_TO_DESCEND_TURN) {
            this.pendingCorrection = () -> {
               Vec3 currentRight = this.deltaMovement.m_82537_(UP).m_82541_();
               Vec3 newDirection = updatedTargetPosition.m_82546_(this.endOfTickPosition);
               Vec3 newRight = newDirection.m_82537_(UP).m_82541_();
               double rightSimilarity = currentRight.m_82526_(newRight);
               double angle = Math.acos(rightSimilarity < 0.0D ? -rightSimilarity : rightSimilarity);
               LOGGER.trace("Correction angle: %.3f, old right vector: (%.5f, %.5f, %.5f), new right vector: (%.5f, %.5f, %.5f)", Math.toDegrees(angle), currentRight.f_82479_, currentRight.f_82480_, currentRight.f_82481_, newRight.f_82479_, newRight.f_82480_, newRight.f_82481_);
               Quaterniond rotation = new Quaterniond();
               rotation.rotateAxis(angle, UP.f_82479_, UP.f_82480_, UP.f_82481_);
               Vector3d dm = new Vector3d(this.deltaMovement.f_82479_, this.deltaMovement.f_82480_, this.deltaMovement.f_82481_);
               dm.rotate(rotation);
               Vec3 updatedDeltaMovement = new Vec3(dm.x, dm.y, dm.z);
               LOGGER.trace("Updated delta movement from (%.5f, %.5f, %.5f) to (%.5f, %.5f, %.5f), angle: %.3f°", this.deltaMovement.f_82479_, this.deltaMovement.f_82480_, this.deltaMovement.f_82481_, updatedDeltaMovement.f_82479_, updatedDeltaMovement.f_82480_, updatedDeltaMovement.f_82481_, Math.toDegrees(angle));
               this.deltaMovement = updatedDeltaMovement;
               this.targetPosition = updatedTargetPosition;
               this.debugDeltaMovement();
            };
         }
      }
   }

   private void prepareClimb() {
      this.climbVector = this.endOfClimbPosition.m_82546_(this.endOfTickPosition);
      Vec3 normalizedClimbVector = this.climbVector.m_82541_();
      LOGGER.trace("Recomputed normalized climb vector: (%.3f, %.3f, %.3f)", normalizedClimbVector.f_82479_, normalizedClimbVector.f_82480_, normalizedClimbVector.f_82481_);
      double climbDistanceBeforeTurn = this.climbVector.m_82553_();
      this.climbAcceleration = MiscUtil.adjustDivisor(this.defaultSpeed - this.defaultSoftLaunchSpeed, this.climbAcceleration);
      int climbAccelerationTicks = (int)Math.round((this.defaultSpeed - this.defaultSoftLaunchSpeed) / this.climbAcceleration);
      double averageSpeed = (this.defaultSpeed + this.defaultSoftLaunchSpeed - this.climbAcceleration) * 0.5D;
      double climbAccelerationDistance = averageSpeed * (double)climbAccelerationTicks;
      if (climbAccelerationDistance > climbDistanceBeforeTurn) {
         double tickD = (-this.defaultSoftLaunchSpeed + Math.sqrt(this.defaultSoftLaunchSpeed * this.defaultSoftLaunchSpeed + 2.0D * this.climbAcceleration * climbDistanceBeforeTurn)) / this.climbAcceleration;
         climbAccelerationTicks = (int)Math.round(tickD);
      }

      Vec3 stopClimbAccelerationPos = this.endOfTickPosition.m_82549_(this.climbVector.m_82541_().m_82490_(climbAccelerationDistance));
      LOGGER.trace("Climb acceleration distance: %.3f. Stop acceleration at: (%.3f, %.3f, %.3f)", climbAccelerationDistance, stopClimbAccelerationPos.f_82479_, stopClimbAccelerationPos.f_82480_, stopClimbAccelerationPos.f_82481_);
      double remainingClimbDistance = climbDistanceBeforeTurn - climbAccelerationDistance;
      if (remainingClimbDistance > this.defaultSpeed) {
         this.remainingClimbSpeed = MiscUtil.adjustDivisor(remainingClimbDistance, this.defaultSpeed);
         int remainingClimbTicks = (int)Math.round(remainingClimbDistance / this.remainingClimbSpeed);
         this.climbTicks = climbAccelerationTicks + remainingClimbTicks;
      } else {
         this.climbTicks = climbAccelerationTicks;
         this.remainingClimbSpeed = this.defaultSpeed;
      }

      this.currentSpeed = this.defaultSoftLaunchSpeed;
      this.setPhase(Phase.CLIMB);
   }

   private void prepareTurn() {
      this.setPhase(Phase.CLIMB_TO_DESCEND_TURN);
   }

   private void prepareDescend(Vec3 right, double targetDirAngle) {
      this.setPhase(Phase.DESCEND);
      Vec3 targetDir = this.targetPosition.m_82546_(this.endOfTickPosition);
      double distanceToTarget = targetDir.m_82553_();
      this.currentSpeed = MiscUtil.adjustDivisor(distanceToTarget, this.currentSpeed);
      this.descendTicks = (int)Math.round(distanceToTarget / this.currentSpeed);
      LOGGER.trace("Ticks to target: %d", this.descendTicks);
      this.deltaMovement = targetDir.m_82541_().m_82490_(this.currentSpeed);
   }

   public static double getRadius(double segmentLength, double curvatureAngle) {
      return segmentLength * 0.5D / Math.sin(curvatureAngle * 0.5D);
   }

   public static Vec3 getHorizontalProjection(Vec3 source, Vec3 target) {
      Vec3 sourceTargetDirection = target.m_82546_(source);
      Vec3 right = sourceTargetDirection.m_82537_(UP);
      Vec3 hp = UP.m_82537_(right).m_82541_();
      return hp;
   }

   public static Vec3 getPivot(Vec3 source, Vec3 target, double radius, double offset) {
      double s = radius / Math.tan(MIN_ATTACK_ANGLE);
      double normalizedOffset = s * Mth.m_14008_(offset, -1.0D, 1.0D);
      Vec3 hp = getHorizontalProjection(source, target);
      return target.m_82549_(hp.m_82490_(-s + normalizedOffset)).m_82549_(UP.m_82490_(radius));
   }

   public static ClimbInfo getClimbInfo(Vec3 source, Vec3 pivot, double radius) {
      Vec3 sourceToPivot = pivot.m_82546_(source);
      Vec3 right = sourceToPivot.m_82537_(UP);
      double alpha = -Math.asin(radius / sourceToPivot.m_82553_());
      Vec3 horizontalProjection = getHorizontalProjection(source, pivot);
      double sourceToPivotAngle = Math.acos(sourceToPivot.m_82541_().m_82526_(horizontalProjection.m_82541_()));
      double sourceToPivotAngleDeg = Math.toDegrees(sourceToPivotAngle);
      double launchAngle = Math.abs(sourceToPivotAngle) + Math.abs(alpha);
      if (launchAngle > 1.5707963267948966D) {
         LOGGER.trace("Launch angle %.5f exceeds 90 degrees", Math.toDegrees(launchAngle));
         return null;
      } else {
         LOGGER.trace("Angle to offset sourcePivot: %.5f, source-pivot angle: %.5f", Math.toDegrees(alpha), sourceToPivotAngleDeg);
         Quaterniond rotation = new Quaterniond();
         rotation.rotateAxis(-alpha, right.f_82479_, right.f_82480_, right.f_82481_);
         Vector3d v = (new Vector3d(sourceToPivot.f_82479_, sourceToPivot.f_82480_, sourceToPivot.f_82481_)).rotate(rotation).mul(Math.cos(alpha));
         return new ClimbInfo(launchAngle, new Vec3(v.x, v.y, v.z));
      }
   }

   public static record ClimbInfo(double launchAngle, Vec3 climbVector) {
      public ClimbInfo(double launchAngle, Vec3 climbVector) {
         this.launchAngle = launchAngle;
         this.climbVector = climbVector;
      }

      public double launchAngle() {
         return this.launchAngle;
      }

      public Vec3 climbVector() {
         return this.climbVector;
      }
   }

   public static enum Phase {
      SOFT_LAUNCH,
      CLIMB,
      CLIMB_TO_DESCEND_TURN,
      DESCEND,
      COMPLETED;

      // $FF: synthetic method
      private static Phase[] $values() {
         return new Phase[]{SOFT_LAUNCH, CLIMB, CLIMB_TO_DESCEND_TURN, DESCEND, COMPLETED};
      }
   }
}
