package com.vicmatskiv.pointblank.util;

import java.util.Random;
import net.minecraft.util.Mth;

public class Interpolators {
   public static class EaseOutFloatProvider implements FloatInterpolator {
      private float value;

      public EaseOutFloatProvider(float value) {
         this.value = value;
      }

      public float getValue(float progress) {
         return this.value * Mth.m_14089_(progress * 3.1415927F * 0.5F);
      }
   }

   public static class EaseInFloatProvider implements FloatInterpolator {
      private float value;

      public EaseInFloatProvider(float value) {
         this.value = value;
      }

      public float getValue(float progress) {
         return this.value * Mth.m_14031_(progress * 3.1415927F * 0.5F);
      }
   }

   public static class AnotherEaseInEaseOutFloatProvider implements FloatInterpolator {
      private float value;
      private float fadeIn;
      private float fadeOut;

      public AnotherEaseInEaseOutFloatProvider(float value, float fadeIn, float fadeOut) {
         this.value = value;
         this.fadeIn = fadeIn;
         this.fadeOut = fadeOut;
      }

      public float getValue(float progress) {
         float factor;
         float ap;
         if (progress < this.fadeIn) {
            ap = progress / this.fadeIn;
            factor = Mth.m_14031_(1.5707964F * ap);
         } else if (progress > this.fadeOut) {
            ap = (progress - this.fadeOut) / (1.0F - this.fadeOut);
            factor = Mth.m_14089_(1.5707964F * ap);
         } else {
            factor = 1.0F;
         }

         factor = Mth.m_14036_(factor, 0.0F, 1.0F);
         return factor * this.value;
      }
   }

   public static class EaseInEaseOutFloatProvider2 implements FloatInterpolator {
      private float value;

      public EaseInEaseOutFloatProvider2(float value) {
         this.value = value;
      }

      public float getValue(float progress) {
         return this.value * Mth.m_14031_(progress * 3.1415927F);
      }
   }

   public static class EaseInEaseOutFloatProvider implements FloatInterpolator {
      private float value;

      public EaseInEaseOutFloatProvider(float value) {
         this.value = value;
      }

      public float getValue(float progress) {
         return this.value * Mth.m_14031_(Mth.m_14116_(progress) * 3.1415927F);
      }
   }

   public static class LinearInterpolatorFloatProvider implements FloatInterpolator {
      private float startValue;
      private float endValue;

      public LinearInterpolatorFloatProvider(float startValue, float endValue) {
         this.startValue = startValue;
         this.endValue = endValue;
      }

      public float getValue(float progress) {
         return Mth.m_14179_(progress, this.startValue, this.endValue);
      }
   }

   public static class ConstantFloatProvider implements FloatInterpolator {
      private float value;

      public ConstantFloatProvider(float value) {
         this.value = value;
      }

      public float getValue(float progress) {
         return this.value;
      }
   }

   @FunctionalInterface
   public interface FloatInterpolator {
      float getValue(float var1);
   }

   public static class RandomFloatProvider implements FloatProvider {
      private static Random random = new Random();
      private float value;

      public RandomFloatProvider(float maxValue) {
         this.value = maxValue;
      }

      public float getValue() {
         return this.value * random.nextFloat();
      }
   }

   public static class RealTimeProgressProvider implements FloatProvider {
      private long startTime;
      private long lifetimeNanos;

      public RealTimeProgressProvider(long lifetimeMillis) {
         this(lifetimeMillis, 0L);
      }

      public RealTimeProgressProvider(long lifetimeMillis, long delayMillis) {
         this.startTime = System.nanoTime() + TimeUnit.MILLISECOND.toNanos(delayMillis);
         this.lifetimeNanos = TimeUnit.MILLISECOND.toNanos(lifetimeMillis);
      }

      public float getValue() {
         if (this.startTime > System.nanoTime()) {
            return Float.NEGATIVE_INFINITY;
         } else {
            float progress = Mth.m_14036_((float)(System.nanoTime() - this.startTime) / (float)this.lifetimeNanos, 0.0F, 1.0F);
            return progress;
         }
      }
   }

   public interface FloatProvider {
      float getValue();
   }
}
