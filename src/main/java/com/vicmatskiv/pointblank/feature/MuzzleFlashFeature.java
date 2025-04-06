package com.vicmatskiv.pointblank.feature;

import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.vicmatskiv.pointblank.client.effect.EffectBuilder;
import com.vicmatskiv.pointblank.client.effect.MuzzleFlashEffect;
import com.vicmatskiv.pointblank.item.GunItem;
import com.vicmatskiv.pointblank.registry.EffectRegistry;
import com.vicmatskiv.pointblank.util.Conditions;
import com.vicmatskiv.pointblank.util.JsonUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.world.item.ItemStack;

public final class MuzzleFlashFeature extends ConditionalFeature {
   private List<Pair<MuzzleFlashEffect.Builder, Predicate<ConditionContext>>> muzzleEffectBuilders = new ArrayList();

   public MuzzleFlashFeature(FeatureProvider owner, Predicate<ConditionContext> predicate, Map<GunItem.FirePhase, List<Pair<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>, Predicate<ConditionContext>>>> effectBuilders) {
      super(owner, predicate, effectBuilders);
      Iterator var4 = effectBuilders.entrySet().iterator();

      while(var4.hasNext()) {
         Entry<GunItem.FirePhase, List<Pair<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>, Predicate<ConditionContext>>>> e = (Entry)var4.next();
         Iterator var6 = ((List)e.getValue()).iterator();

         while(var6.hasNext()) {
            Pair<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>, Predicate<ConditionContext>> ebs = (Pair)var6.next();
            EffectBuilder<? extends EffectBuilder<?, ?>, ?> eb = (EffectBuilder)((Supplier)ebs.getFirst()).get();
            if (eb instanceof MuzzleFlashEffect.Builder) {
               MuzzleFlashEffect.Builder mfeb = (MuzzleFlashEffect.Builder)eb;
               this.muzzleEffectBuilders.add(Pair.of(mfeb, (Predicate)ebs.getSecond()));
            }
         }
      }

   }

   public List<Pair<MuzzleFlashEffect.Builder, Predicate<ConditionContext>>> getMuzzleFlashEffectBuilders() {
      return this.muzzleEffectBuilders;
   }

   public boolean isEnabled(ItemStack itemStack) {
      return super.isEnabled(itemStack);
   }

   public static class Builder implements FeatureBuilder<Builder, MuzzleFlashFeature> {
      private Map<GunItem.FirePhase, List<Pair<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>, Predicate<ConditionContext>>>> effectBuilders = new HashMap();
      private Predicate<ConditionContext> condition = (ctx) -> {
         return true;
      };

      public Builder withEffect(GunItem.FirePhase firePhase, Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> effectBuilder) {
         List<Pair<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>, Predicate<ConditionContext>>> builders = (List)this.effectBuilders.computeIfAbsent(firePhase, (k) -> {
            return new ArrayList();
         });
         builders.add(Pair.of(effectBuilder, (ctx) -> {
            return true;
         }));
         return this;
      }

      public Builder withEffect(GunItem.FirePhase firePhase, Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> effectBuilder, Predicate<ConditionContext> condition) {
         List<Pair<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>, Predicate<ConditionContext>>> builders = (List)this.effectBuilders.computeIfAbsent(firePhase, (k) -> {
            return new ArrayList();
         });
         builders.add(Pair.of(effectBuilder, condition));
         return this;
      }

      public Builder withCondition(Predicate<ConditionContext> condition) {
         this.condition = condition;
         return this;
      }

      public Builder withJsonObject(JsonObject obj) {
         if (obj.has("condition")) {
            this.withCondition(Conditions.fromJson(obj.getAsJsonObject("condition")));
         }

         GunItem.FirePhase firePhase;
         Supplier supplier;
         Predicate condition;
         for(Iterator var2 = JsonUtil.getJsonObjects(obj, "effects").iterator(); var2.hasNext(); this.withEffect(firePhase, supplier, condition)) {
            JsonObject effect = (JsonObject)var2.next();
            firePhase = (GunItem.FirePhase)JsonUtil.getEnum(effect, "phase", GunItem.FirePhase.class, (Enum)null, true);
            String effectName = JsonUtil.getJsonString(effect, "name");
            supplier = () -> {
               return (EffectBuilder)EffectRegistry.getEffectBuilderSupplier(effectName).get();
            };
            if (effect.has("condition")) {
               JsonObject conditionObj = effect.getAsJsonObject("condition");
               condition = Conditions.fromJson(conditionObj);
            } else {
               condition = (ctx) -> {
                  return true;
               };
            }
         }

         return this;
      }

      public MuzzleFlashFeature build(FeatureProvider featureProvider) {
         return new MuzzleFlashFeature(featureProvider, this.condition, this.effectBuilders);
      }
   }
}
