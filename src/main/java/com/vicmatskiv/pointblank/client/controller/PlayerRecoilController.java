package com.vicmatskiv.pointblank.client.controller;

import com.vicmatskiv.pointblank.Config;
import com.vicmatskiv.pointblank.client.GunClientState;
import com.vicmatskiv.pointblank.client.GunStateListener;
import com.vicmatskiv.pointblank.feature.RecoilFeature;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class PlayerRecoilController implements GunStateListener {
   private static double middleProgress = 1.0D - Math.sqrt(2.0D) / 2.0D;
   private int elapsedTime;
   private double ticksPerTransition;
   private double progress;
   private double prevProgress;
   private boolean isForward;
   private double actualAmplitude;
   private double amplitude;
   private double dPitch;
   private double dPitchResetRatio = 1.0D;
   private double startPitch;
   private double maxPitch;
   private double initialPitch;
   private long startTime;
   private long nanoSecPerTransition;
   private long resetDurationNano;
   private State currentState;

   public PlayerRecoilController(double amplitude, double maxPitch, double duration) {
      this.currentState = State.IDLE;
      this.nanoSecPerTransition = (long)(duration * 1000000.0D);
      this.amplitude = amplitude;
      this.actualAmplitude = amplitude;
      this.ticksPerTransition = duration;
      this.maxPitch = -Math.abs(maxPitch);
      this.resetDurationNano = 200000000L;
   }

   public void onUpdateState(LivingEntity player, GunClientState state) {
      if ((double)this.elapsedTime < this.ticksPerTransition) {
         ++this.elapsedTime;
      }

   }

   public void onRenderTick(LivingEntity player, GunClientState state, ItemStack itemStack, ItemDisplayContext itemDisplayContext, float partialTicks) {
      if (this.currentState != State.IDLE) {
         this.prevProgress = this.progress;
         this.progress = this.getProgress(state, partialTicks);
         if (this.currentState == State.RECOILING && this.progress >= 1.0D) {
            if (Config.resetAutoFirePitchEnabled && this.resetDurationNano > 0L) {
               this.currentState = State.RESETTING;
               this.startTime = System.nanoTime();
               this.startPitch = (double)player.m_146909_();
               this.progress = this.prevProgress = 0.0D;
               this.nanoSecPerTransition = this.resetDurationNano;
            } else {
               this.currentState = State.IDLE;
            }
         }

         if (this.currentState == State.RESETTING) {
            this.progress = 0.5D - 0.5D * Math.cos(3.141592653589793D * this.progress);
            if (this.progress >= 1.0D) {
               this.currentState = State.IDLE;
            }
         }

         double dProgress;
         float prevPitch;
         float newXRot;
         if (this.currentState == State.RESETTING) {
            dProgress = this.progress - this.prevProgress;
            prevPitch = player.m_146909_();
            this.dPitch = (this.initialPitch - this.startPitch) * dProgress;
            newXRot = (float)((double)prevPitch + this.dPitch);
            player.m_146926_(newXRot);
         } else {
            if (this.isForward && this.progress > middleProgress) {
               this.isForward = false;
            }

            dProgress = this.progress - this.prevProgress;
            if (this.isForward) {
               this.dPitch = -this.actualAmplitude * dProgress / middleProgress;
            } else {
               this.dPitch = this.dPitchResetRatio * this.actualAmplitude * dProgress / (1.0D - middleProgress);
            }

            prevPitch = player.m_146909_();
            newXRot = (float)((double)prevPitch + this.dPitch);
            if (!this.isForward) {
               if ((double)newXRot < this.startPitch) {
                  player.m_146926_(newXRot);
               }
            } else {
               player.m_146926_(newXRot);
            }

         }
      }
   }

   public void onStartFiring(LivingEntity player, GunClientState state, ItemStack itemStack) {
      this.isForward = true;
      this.currentState = State.RECOILING;
      this.elapsedTime = -1;
      this.startTime = System.nanoTime();
      this.startPitch = (double)player.m_146909_();
      this.progress = this.prevProgress = 0.0D;
      this.actualAmplitude = state.isAiming() ? this.amplitude * 0.5D : this.amplitude;
      this.actualAmplitude *= (double)RecoilFeature.getRecoilModifier(itemStack);
      if (state.getTotalUninterruptedShots() <= 1 || this.startPitch > this.initialPitch) {
         this.initialPitch = this.startPitch;
      }

      if (state.getTotalUninterruptedShots() > 1 && !(Math.abs(this.startPitch - this.initialPitch) >= Math.abs(this.maxPitch))) {
         this.dPitchResetRatio = 0.1D;
      } else {
         this.dPitchResetRatio = 1.0D;
      }

   }

   protected double getProgress(GunClientState gunClientState, float partialTicks) {
      double progress = (double)(System.nanoTime() - this.startTime) / (double)this.nanoSecPerTransition;
      if (progress > 1.0D) {
         progress = 1.0D;
      }

      return progress;
   }

   private static enum State {
      IDLE,
      RECOILING,
      RESETTING;

      // $FF: synthetic method
      private static State[] $values() {
         return new State[]{IDLE, RECOILING, RESETTING};
      }
   }
}
