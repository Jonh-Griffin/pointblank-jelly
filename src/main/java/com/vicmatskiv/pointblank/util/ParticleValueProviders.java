package com.vicmatskiv.pointblank.util;

import com.vicmatskiv.pointblank.client.effect.EffectBuilder;
import java.util.Random;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import software.bernie.geckolib.util.ClientUtils;

public class ParticleValueProviders {
   public static class BoundingBoxBasedParticleWidthProvider implements ParticleWidthProvider {
      private double maxBoundBoxSize;
      private double maxWidth;

      public BoundingBoxBasedParticleWidthProvider(double maxBoundBoxSize, double maxWidth) {
         this.maxBoundBoxSize = maxBoundBoxSize;
         this.maxWidth = maxWidth;
      }

      public float getWidth(EffectBuilder.Context effectContext) {
         float result = (float)this.maxWidth;
         HitResult hitResult = effectContext.getHitResult();
         if (hitResult instanceof SimpleHitResult) {
            SimpleHitResult simpleHitResult = (SimpleHitResult)hitResult;
            int entityId = simpleHitResult.getEntityId();
            Entity entity = ClientUtils.getLevel().m_6815_(entityId);
            if (entity != null) {
               AABB bb = entity.m_20191_();
               if (bb != null) {
                  result = (float)Mth.m_14008_(this.maxWidth * bb.m_82309_() / this.maxBoundBoxSize, 0.0D, this.maxWidth);
               }
            }
         }

         return result;
      }
   }

   public static class DamageBasedParticleCountProvider implements ParticleCountProvider {
      private int maxCount;
      private float maxDamage;

      public DamageBasedParticleCountProvider(int maxCount, float maxDamage) {
         this.maxCount = maxCount;
         this.maxDamage = maxDamage;
      }

      public int getCount(EffectBuilder.Context effectContext) {
         float damage = effectContext.getDamage();
         return Math.round(Mth.m_14036_((float)this.maxCount * damage / this.maxDamage, 0.0F, (float)this.maxCount));
      }
   }

   public static class RandomParticleCountProvider implements ParticleCountProvider {
      private Random random = new Random();
      private int origin;
      private int bound;

      public RandomParticleCountProvider(int origin, int bound) {
         this.origin = origin;
         this.bound = bound;
      }

      public int getCount(EffectBuilder.Context effectContext) {
         return this.random.nextInt(this.origin, this.bound);
      }
   }

   public static class ConstantParticleCountProvider implements ParticleCountProvider {
      private int count;

      public ConstantParticleCountProvider(int count) {
         this.count = count;
      }

      public int getCount(EffectBuilder.Context effectContext) {
         return this.count;
      }
   }

   public interface ParticleWidthProvider {
      float getWidth(EffectBuilder.Context var1);
   }

   public interface ParticleCountProvider {
      int getCount(EffectBuilder.Context var1);
   }
}
