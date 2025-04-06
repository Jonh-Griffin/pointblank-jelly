package com.vicmatskiv.pointblank.item;

import com.vicmatskiv.pointblank.client.GunClientState;
import com.vicmatskiv.pointblank.feature.ConditionContext;
import com.vicmatskiv.pointblank.util.TimeUnit;
import java.util.function.Predicate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

@FunctionalInterface
public interface AnimationProvider {
   Descriptor getDescriptor(LivingEntity var1, ItemStack var2, GunClientState var3);

   public static class Simple implements AnimationProvider {
      public Descriptor descriptor;

      public Simple(String animationName) {
         this.descriptor = new Descriptor((ctx) -> {
            return true;
         }, 0L, TimeUnit.MILLISECOND, animationName);
      }

      public Descriptor getDescriptor(LivingEntity player, ItemStack itemStack, GunClientState gunClientState) {
         return this.descriptor;
      }
   }

   public static record Descriptor(Predicate<ConditionContext> predicate, long duration, TimeUnit timeUnit, String animationName) {
      public Descriptor(Predicate<ConditionContext> predicate, long duration, TimeUnit timeUnit, String animationName) {
         this.predicate = predicate;
         this.duration = duration;
         this.timeUnit = timeUnit;
         this.animationName = animationName;
      }

      public Predicate<ConditionContext> predicate() {
         return this.predicate;
      }

      public long duration() {
         return this.duration;
      }

      public TimeUnit timeUnit() {
         return this.timeUnit;
      }

      public String animationName() {
         return this.animationName;
      }
   }
}
