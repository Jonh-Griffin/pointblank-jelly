package com.vicmatskiv.pointblank.item;

import com.vicmatskiv.pointblank.client.GunClientState;
import com.vicmatskiv.pointblank.feature.ConditionContext;
import com.vicmatskiv.pointblank.util.Conditions;
import com.vicmatskiv.pointblank.util.TimeUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class ConditionalAnimationProvider implements AnimationProvider {
   private List<Descriptor> conditionalAnimations;
   private int randomUpperBound;
   private static final Random random = new Random();

   private ConditionalAnimationProvider(List<Descriptor> conditionalAnimations) {
      this.conditionalAnimations = Collections.unmodifiableList(conditionalAnimations);
      this.randomUpperBound = (int)conditionalAnimations.stream().filter((p) -> {
         return p.predicate() == Conditions.RANDOM_PICK;
      }).count();
   }

   public Descriptor getDescriptor(LivingEntity player, ItemStack itemStack, GunClientState gunClientState) {
      Descriptor result = null;
      int randomValue = this.randomUpperBound > 0 ? random.nextInt(this.randomUpperBound) : 0;
      int i = 0;
      Iterator var7 = this.conditionalAnimations.iterator();

      while(var7.hasNext()) {
         Descriptor descriptor = (Descriptor)var7.next();
         ConditionContext ctx = new ConditionContext(player, itemStack, itemStack, gunClientState, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, randomValue, i);
         if (descriptor.predicate() == Conditions.RANDOM_PICK) {
            ++i;
         }

         if (descriptor.predicate().test(ctx)) {
            result = descriptor;
            break;
         }
      }

      return result;
   }

   public static class Builder {
      private List<Descriptor> conditionalAnimations = new ArrayList();

      public Builder withAnimation(String animation, Predicate<ConditionContext> condition, long duration, TimeUnit timeUnit) {
         this.conditionalAnimations.add(new Descriptor(condition, duration, timeUnit, animation));
         return this;
      }

      public Builder withAnimation(String animation, Predicate<ConditionContext> condition) {
         return this.withAnimation(animation, condition, 0L, TimeUnit.MILLISECOND);
      }

      public List<Descriptor> getAnimations() {
         return this.conditionalAnimations;
      }

      public AnimationProvider build() {
         return new ConditionalAnimationProvider(this.conditionalAnimations);
      }
   }
}
