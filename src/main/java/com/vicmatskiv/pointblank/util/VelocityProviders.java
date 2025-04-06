package com.vicmatskiv.pointblank.util;

import java.util.Random;
import java.util.function.Supplier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class VelocityProviders {
   private static Random random = new Random();

   public static Supplier<Vec3> randomVelocityProvider(double magnitude) {
      return () -> {
         return new Vec3((random.nextDouble() - 0.5D) * 2.0D * magnitude, (random.nextDouble() - 0.5D) * 2.0D * magnitude, (random.nextDouble() - 0.5D) * 2.0D * magnitude);
      };
   }

   public static Supplier<Vec3> sphereVelocityProvider(float radius, Distribution radialDistribution) {
      return () -> {
         double adjustedRadius = radialDistribution.transform((double)radius);
         float theta = 6.2831855F * random.nextFloat();
         float phi = (float)Math.acos((double)(2.0F * random.nextFloat() - 1.0F));
         float sinPhi = Mth.m_14031_(phi);
         double x = adjustedRadius * (double)sinPhi * (double)Mth.m_14089_(theta);
         double z = adjustedRadius * (double)sinPhi * (double)Mth.m_14031_(theta);
         double y = adjustedRadius * Math.cos((double)phi);
         return new Vec3(x, y, z);
      };
   }

   public static Supplier<Vec3> hemisphereVelocityProvider(double radius, Distribution radialDistribution) {
      return () -> {
         double adjustedRadius = radialDistribution.transform(radius);
         float theta = 6.2831855F * random.nextFloat();
         float phi = (float)Math.acos((double)random.nextFloat());
         float sinPhi = Mth.m_14031_(phi);
         double x = adjustedRadius * (double)sinPhi * (double)Mth.m_14089_(theta);
         double z = adjustedRadius * (double)sinPhi * (double)Mth.m_14031_(theta);
         double y = adjustedRadius * Math.cos((double)phi);
         return new Vec3(x, y, z);
      };
   }

   public static enum Distribution {
      CONSTANT,
      UNIFORM,
      NORMAL(0.0F, 1.0F, 0.0F, Float.POSITIVE_INFINITY),
      TIGHT(0.5F, 0.25F, 0.25F, 2.0F);

      private float mean;
      private float standardDeviation;
      private float lowerBound;
      private float upperBound;

      private Distribution(float mean, float standardDeviation, float lowerBound, float upperBound) {
         this.mean = mean;
         this.standardDeviation = standardDeviation;
         this.lowerBound = lowerBound;
         this.upperBound = upperBound;
      }

      private Distribution() {
      }

      private double transform(double value) {
         double adjustedValue;
         switch(this) {
         case CONSTANT:
            adjustedValue = value;
            break;
         case UNIFORM:
            adjustedValue = VelocityProviders.random.nextDouble() * value;
            break;
         default:
            adjustedValue = Mth.m_14008_((VelocityProviders.random.nextGaussian() * (double)this.mean + (double)this.standardDeviation) * value, (double)this.lowerBound, (double)this.upperBound);
         }

         return adjustedValue;
      }

      // $FF: synthetic method
      private static Distribution[] $values() {
         return new Distribution[]{CONSTANT, UNIFORM, NORMAL, TIGHT};
      }
   }
}
