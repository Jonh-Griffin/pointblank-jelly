package com.vicmatskiv.pointblank.client;

import com.vicmatskiv.pointblank.util.TimeUnit;
import net.minecraft.util.Mth;

public class SegmentsProviders {
   public static final SegmentsProvider ZERO_PROVIDER = new ZeroProvider();

   private static class ZeroProvider implements SegmentsProvider {
      private static float[][] SEGMENT = new float[][]{{0.0F, 0.0F}};

      public float[][] getSegments(float length, float progress, long lifetimeNanos) {
         return SEGMENT;
      }

      public boolean shouldDetach() {
         return true;
      }
   }

   public interface SegmentsProvider {
      float[][] getSegments(float var1, float var2, long var3);

      boolean shouldDetach();
   }

   public static class MovingPointProvider implements SegmentsProvider {
      public static final int DEFAULT_SPEED_BLOCKS_PER_SECOND = 100;
      public static final int DEFAULT_ACCELERATION = -10;
      private float speedBlocksPerSecond;
      private float acceleration;

      public MovingPointProvider(float speedBlocksPerSecond, float accelerationBlocksPerSecond) {
         this.speedBlocksPerSecond = speedBlocksPerSecond;
         this.acceleration = accelerationBlocksPerSecond;
      }

      public float[][] getSegments(float length, float progress, long lifetimeNanos) {
         float start = 0.0F;
         float initialSpeed = this.speedBlocksPerSecond;
         float lifetimeSeconds = (float)TimeUnit.NANOSECOND.toSeconds(lifetimeNanos);
         float time = lifetimeSeconds * progress;
         float speed = initialSpeed + this.acceleration * time;
         start = Mth.m_14036_(speed * time, 0.0F, length);
         return new float[][]{{start, start}};
      }

      public boolean shouldDetach() {
         return true;
      }
   }

   public static class StaticBeamSegmentsProvider implements SegmentsProvider {
      public float[][] getSegments(float length, float progress, long lifetimeNanos) {
         float start = 0.0F;
         float offset = Mth.m_14036_(length * 0.1F, 0.01F, 1.0F);
         float[][] beamSegments = new float[][]{{start, start + offset}, {start + offset, length - offset}, {length - offset, length}};
         return beamSegments;
      }

      public boolean shouldDetach() {
         return false;
      }
   }

   public static class MovingSegmentsProvider implements SegmentsProvider {
      public static final int DEFAULT_SPEED_BLOCKS_PER_SECOND = 100;
      public static final int DEFAULT_ACCELERATION = -10;
      public static final float DEFAULT_MAX_SEGMENT_LENGTH = 10.0F;
      public static final float DEFAULT_MIN_SEGMENT_LENGTH = 0.1F;
      private float speedBlocksPerSecond;
      private float acceleration;
      private float minSegmentLength;
      private float maxSegmentLength;

      public MovingSegmentsProvider(float speedBlocksPerSecond, float accelerationBlocksPerSecond) {
         this(speedBlocksPerSecond, accelerationBlocksPerSecond, 0.1F, 10.0F);
      }

      public MovingSegmentsProvider(float speedBlocksPerSecond, float accelerationBlocksPerSecond, float minSegmentLength, float maxSegmentLength) {
         this.minSegmentLength = 0.1F;
         this.maxSegmentLength = 10.0F;
         this.speedBlocksPerSecond = speedBlocksPerSecond;
         this.acceleration = accelerationBlocksPerSecond;
         this.minSegmentLength = minSegmentLength;
         this.maxSegmentLength = maxSegmentLength;
      }

      public float[][] getSegments(float length, float progress, long lifetimeNanos) {
         float start = 0.0F;
         float initialSpeed = this.speedBlocksPerSecond;
         float lifetimeSeconds = (float)TimeUnit.NANOSECOND.toSeconds(lifetimeNanos);
         float time = lifetimeSeconds * progress;
         float speed = Mth.m_14036_(initialSpeed + this.acceleration * time, 0.0F, Float.MAX_VALUE);
         start = Mth.m_14036_(speed * time, 0.0F, length);
         float segmentLength = Mth.m_14036_(length * 0.5F, this.minSegmentLength, this.maxSegmentLength);
         float end = Mth.m_14036_(start + segmentLength, 1.0F, length);
         float[][] beamSegments = new float[][]{{start, end}};
         return beamSegments;
      }

      public boolean shouldDetach() {
         return true;
      }
   }

   public static class SingleSegmentProvider implements SegmentsProvider {
      public float[][] getSegments(float length, float progress, long lifetimeNanos) {
         return new float[][]{{0.0F, length}};
      }

      public boolean shouldDetach() {
         return true;
      }
   }
}
