package com.vicmatskiv.pointblank.item;

import com.vicmatskiv.pointblank.client.effect.EffectBuilder;
import com.vicmatskiv.pointblank.entity.ProjectileLike;
import java.util.function.Predicate;
import java.util.function.Supplier;

public record EffectBuilderInfo(Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> effectSupplier, Predicate<ProjectileLike> predicate) {
   public EffectBuilderInfo(Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> effectSupplier, Predicate<ProjectileLike> predicate) {
      this.effectSupplier = effectSupplier;
      this.predicate = predicate;
   }

   public Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> effectSupplier() {
      return this.effectSupplier;
   }

   public Predicate<ProjectileLike> predicate() {
      return this.predicate;
   }
}
