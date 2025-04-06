package com.vicmatskiv.pointblank.feature;

import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.vicmatskiv.pointblank.client.GunClientState;
import com.vicmatskiv.pointblank.client.effect.EffectBuilder;
import com.vicmatskiv.pointblank.item.AmmoItem;
import com.vicmatskiv.pointblank.item.AnimationProvider;
import com.vicmatskiv.pointblank.item.ConditionalAnimationProvider;
import com.vicmatskiv.pointblank.item.FireMode;
import com.vicmatskiv.pointblank.item.FireModeInstance;
import com.vicmatskiv.pointblank.item.GunItem;
import com.vicmatskiv.pointblank.registry.AmmoRegistry;
import com.vicmatskiv.pointblank.registry.EffectRegistry;
import com.vicmatskiv.pointblank.registry.ItemRegistry;
import com.vicmatskiv.pointblank.util.Conditions;
import com.vicmatskiv.pointblank.util.JsonUtil;
import com.vicmatskiv.pointblank.util.TimeUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class FireModeFeature extends ConditionalFeature {
   public static final int DEFAULT_RPM = -1;
   public static final int DEFAULT_BURST_SHOTS = -1;
   private static final int DEFAULT_MAX_AMMO_CAPACITY = 1;
   private static final double DEFAULT_SHAKE_RECOIL_AMPLITUDE = 0.5D;
   private static final double DEFAULT_SHAKE_RECOIL_SPEED = 8.0D;
   private static final int DEFAULT_SHAKE_RECOIL_DURATION = 400;
   private static final AnimationProvider DEFAULT_ANIMATION_PROVIDER = new AnimationProvider.Simple("animation.model.fire");
   private List<FireModeInstance> fireModeInstances;
   private Map<GunItem.FirePhase, List<Pair<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>, Predicate<ConditionContext>>>> effectBuilders;

   private FireModeFeature(FeatureProvider owner, Predicate<ConditionContext> predicate, List<FireModeInstance> fireModes) {
      super(owner, predicate);
      this.fireModeInstances = fireModes;
      this.effectBuilders = new HashMap();
      Iterator var4 = this.fireModeInstances.iterator();

      while(var4.hasNext()) {
         FireModeInstance fireModeInstance = (FireModeInstance)var4.next();
         Map<GunItem.FirePhase, List<Pair<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>, Predicate<ConditionContext>>>> fireModeInstanceEffectBuilders = fireModeInstance.getEffectBuilders();
         Iterator var7 = fireModeInstanceEffectBuilders.entrySet().iterator();

         while(var7.hasNext()) {
            Entry<GunItem.FirePhase, List<Pair<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>, Predicate<ConditionContext>>>> e = (Entry)var7.next();
            List<Pair<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>, Predicate<ConditionContext>>> firePhaseEffectBuilders = (List)this.effectBuilders.computeIfAbsent((GunItem.FirePhase)e.getKey(), (k) -> {
               return new ArrayList();
            });
            firePhaseEffectBuilders.addAll((Collection)e.getValue());
         }
      }

   }

   public List<FireModeInstance> getFireModes() {
      return this.fireModeInstances;
   }

   public Map<GunItem.FirePhase, List<Pair<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>, Predicate<ConditionContext>>>> getEffectBuilders() {
      return this.effectBuilders;
   }

   public MutableComponent getDescription() {
      MutableComponent description = Component.m_237115_("label.pointblank.fireMode").m_130946_(": ");
      boolean isFirst = true;
      Iterator var3 = this.fireModeInstances.iterator();

      while(var3.hasNext()) {
         FireModeInstance instance = (FireModeInstance)var3.next();
         if (!isFirst) {
            description.m_130946_(", ");
         }

         isFirst = false;
         description.m_7220_(instance.getDisplayName());
      }

      return description;
   }

   public static int getRpm(ItemStack itemStack) {
      Item var2 = itemStack.m_41720_();
      if (var2 instanceof GunItem) {
         GunItem gunItem = (GunItem)var2;
         FireModeInstance var4 = GunItem.getFireModeInstance(itemStack);
         int rpm = var4.getRpm();
         return rpm != -1 ? rpm : gunItem.getRpm();
      } else {
         return 0;
      }
   }

   public static float getDamage(ItemStack itemStack) {
      Item var2 = itemStack.m_41720_();
      if (var2 instanceof AmmoItem) {
         AmmoItem ammoItem = (AmmoItem)var2;
         return ammoItem.getDamage();
      } else if (!(itemStack.m_41720_() instanceof GunItem)) {
         return 0.0F;
      } else {
         FireModeInstance fireModeInstance = GunItem.getFireModeInstance(itemStack);
         return fireModeInstance.getDamage();
      }
   }

   public static int getMaxShootingDistance(ItemStack itemStack) {
      if (!(itemStack.m_41720_() instanceof GunItem)) {
         return 200;
      } else {
         FireModeInstance fireModeInstance = GunItem.getFireModeInstance(itemStack);
         return fireModeInstance.getMaxShootingDistance();
      }
   }

   public static String getFireAnimation(LivingEntity player, GunClientState state, ItemStack itemStack) {
      if (!(itemStack.m_41720_() instanceof GunItem)) {
         return null;
      } else {
         FireModeInstance fireModeInstance = GunItem.getFireModeInstance(itemStack);
         if (fireModeInstance == null) {
            return null;
         } else {
            AnimationProvider.Descriptor descriptor = fireModeInstance.getFireAnimationDescriptor(player, itemStack, state);
            return descriptor != null ? descriptor.animationName() : null;
         }
      }
   }

   public static String getPrepareFireAnimation(LivingEntity player, GunClientState state, ItemStack itemStack) {
      if (!(itemStack.m_41720_() instanceof GunItem)) {
         return null;
      } else {
         FireModeInstance fireModeInstance = GunItem.getFireModeInstance(itemStack);
         if (fireModeInstance == null) {
            return null;
         } else {
            AnimationProvider.Descriptor descriptor = fireModeInstance.getPrepareFireAnimationDescriptor(player, itemStack, state);
            return descriptor != null ? descriptor.animationName() : null;
         }
      }
   }

   public static String getCompleteFireAnimation(LivingEntity player, GunClientState state, ItemStack itemStack) {
      if (!(itemStack.m_41720_() instanceof GunItem)) {
         return null;
      } else {
         FireModeInstance fireModeInstance = GunItem.getFireModeInstance(itemStack);
         if (fireModeInstance == null) {
            return null;
         } else {
            AnimationProvider.Descriptor descriptor = fireModeInstance.getCompleteFireAnimationDescriptor(player, itemStack, state);
            return descriptor != null ? descriptor.animationName() : null;
         }
      }
   }

   public static String getEnableFireModeAnimation(LivingEntity player, GunClientState state, ItemStack itemStack) {
      if (!(itemStack.m_41720_() instanceof GunItem)) {
         return null;
      } else {
         FireModeInstance fireModeInstance = GunItem.getFireModeInstance(itemStack);
         if (fireModeInstance == null) {
            return null;
         } else {
            AnimationProvider.Descriptor descriptor = fireModeInstance.getEnableFireModeAnimationDescriptor(player, itemStack, state);
            return descriptor != null ? descriptor.animationName() : null;
         }
      }
   }

   public static FireModeInstance.ViewShakeDescriptor getViewShakeDescriptor(ItemStack itemStack) {
      if (!(itemStack.m_41720_() instanceof GunItem)) {
         return null;
      } else {
         FireModeInstance fireModeInstance = GunItem.getFireModeInstance(itemStack);
         return fireModeInstance.getViewShakeDescriptor();
      }
   }

   public static long getPrepareFireCooldownDuration(LivingEntity player, GunClientState state, ItemStack itemStack) {
      Item var4 = itemStack.m_41720_();
      if (var4 instanceof GunItem) {
         GunItem gunItem = (GunItem)var4;
         FireModeInstance fireModeInstance = GunItem.getFireModeInstance(itemStack);
         if (fireModeInstance == null) {
            return gunItem.getPrepareFireCooldownDuration();
         } else {
            AnimationProvider.Descriptor descriptor = fireModeInstance.getPrepareFireAnimationDescriptor(player, itemStack, state);
            return descriptor != null ? descriptor.timeUnit().toMillis(descriptor.duration()) : gunItem.getPrepareFireCooldownDuration();
         }
      } else {
         return 0L;
      }
   }

   public static long getCompleteFireCooldownDuration(LivingEntity player, GunClientState state, ItemStack itemStack) {
      Item var4 = itemStack.m_41720_();
      if (var4 instanceof GunItem) {
         GunItem gunItem = (GunItem)var4;
         FireModeInstance fireModeInstance = GunItem.getFireModeInstance(itemStack);
         if (fireModeInstance == null) {
            return gunItem.getCompleteFireCooldownDuration();
         } else {
            AnimationProvider.Descriptor descriptor = fireModeInstance.getCompleteFireAnimationDescriptor(player, itemStack, state);
            return descriptor != null ? descriptor.timeUnit().toMillis(descriptor.duration()) : gunItem.getCompleteFireCooldownDuration();
         }
      } else {
         return 0L;
      }
   }

   public static long getEnableFireModeCooldownDuration(LivingEntity player, GunClientState state, ItemStack itemStack) {
      Item var4 = itemStack.m_41720_();
      if (var4 instanceof GunItem) {
         GunItem gunItem = (GunItem)var4;
         FireModeInstance fireModeInstance = GunItem.getFireModeInstance(itemStack);
         if (fireModeInstance == null) {
            return gunItem.getEnableFireModeCooldownDuration();
         } else {
            AnimationProvider.Descriptor descriptor = fireModeInstance.getEnableFireModeAnimationDescriptor(player, itemStack, state);
            return descriptor != null ? descriptor.timeUnit().toMillis(descriptor.duration()) : gunItem.getEnableFireModeCooldownDuration();
         }
      } else {
         return 0L;
      }
   }

   public static Pair<Integer, Double> getPelletCountAndSpread(LivingEntity player, GunClientState state, ItemStack itemStack) {
      Item var4 = itemStack.m_41720_();
      if (var4 instanceof GunItem) {
         GunItem gunItem = (GunItem)var4;
         FireModeInstance fireModeInstance = GunItem.getFireModeInstance(itemStack);
         return fireModeInstance == null ? Pair.of(gunItem.getPelletCount(), gunItem.getPelletSpread()) : Pair.of(fireModeInstance.getPelletCount(), fireModeInstance.getPelletSpread());
      } else {
         return Pair.of(0, 1.0D);
      }
   }

   public static class Builder implements FeatureBuilder<Builder, FireModeFeature> {
      private Predicate<ConditionContext> condition = (ctx) -> {
         return true;
      };
      private List<FireModeDescriptor> fireModes = new ArrayList();

      public Builder withCondition(Predicate<ConditionContext> condition) {
         this.condition = condition;
         return this;
      }

      public Builder withFireMode(String name, FireMode type, Supplier<AmmoItem> ammoSupplier, int maxAmmoCapacity, int rpm, double damage, String fireAnimationName) {
         FireModeDescriptor fireModeDesriptor = new FireModeDescriptor(name, Component.m_237115_(name), type, ammoSupplier, maxAmmoCapacity, rpm, -1, (double)((float)damage), 200, 0, 1.0D, true, (AnimationProvider)null, new AnimationProvider.Simple(fireAnimationName), (AnimationProvider)null, (AnimationProvider)null, (FireModeInstance.ViewShakeDescriptor)null, Collections.emptyMap());
         return this.withFireMode(fireModeDesriptor);
      }

      public Builder withFireMode(String name, FireMode type, Supplier<AmmoItem> ammoSupplier, int maxAmmoCapacity, int rpm, double damage, AnimationProvider fireAnimationProvider) {
         FireModeDescriptor fireModeDesriptor = new FireModeDescriptor(name, Component.m_237115_(name), type, ammoSupplier, maxAmmoCapacity, rpm, -1, (double)((float)damage), 200, 0, 1.0D, true, (AnimationProvider)null, fireAnimationProvider, (AnimationProvider)null, (AnimationProvider)null, (FireModeInstance.ViewShakeDescriptor)null, Collections.emptyMap());
         return this.withFireMode(fireModeDesriptor);
      }

      public Builder withFireMode(String name, FireMode type, Supplier<AmmoItem> ammoSupplier, int maxAmmoCapacity, int rpm, int burstShots, double damage, String fireAnimationName) {
         FireModeDescriptor fireModeDesriptor = new FireModeDescriptor(name, Component.m_237115_(name), type, ammoSupplier, maxAmmoCapacity, rpm, burstShots, (double)((float)damage), 200, 0, 1.0D, true, (AnimationProvider)null, new AnimationProvider.Simple(fireAnimationName), (AnimationProvider)null, (AnimationProvider)null, (FireModeInstance.ViewShakeDescriptor)null, Collections.emptyMap());
         return this.withFireMode(fireModeDesriptor);
      }

      public Builder withFireMode(String name, FireMode type, Component displayName, int rpm, double damage, boolean isUsingDefaultMuzzle, String fireAnimationName, FireModeInstance.ViewShakeDescriptor viewShakeDescriptor) {
         return this.withFireMode(name, type, displayName, AmmoRegistry.DEFAULT_AMMO_POOL, 0, rpm, -1, damage, 200, isUsingDefaultMuzzle, (String)fireAnimationName, viewShakeDescriptor);
      }

      public Builder withFireMode(String name, FireMode type, Component displayName, int rpm, int burstShots, double damage, boolean isUsingDefaultMuzzle, String fireAnimationName, FireModeInstance.ViewShakeDescriptor viewShakeDescriptor) {
         return this.withFireMode(name, type, displayName, AmmoRegistry.DEFAULT_AMMO_POOL, 0, rpm, burstShots, damage, 200, isUsingDefaultMuzzle, (String)fireAnimationName, viewShakeDescriptor);
      }

      public Builder withFireMode(String name, FireMode type, Component displayName, Supplier<AmmoItem> ammoSupplier, int maxAmmoCapacity, int rpm, double damage, boolean isUsingDefaultMuzzle, String fireAnimationName, FireModeInstance.ViewShakeDescriptor viewShakeDescriptor) {
         FireModeDescriptor fireModeDesriptor = new FireModeDescriptor(name, displayName, type, ammoSupplier, maxAmmoCapacity, rpm, -1, damage, 200, 0, 1.0D, isUsingDefaultMuzzle, (AnimationProvider)null, new AnimationProvider.Simple(fireAnimationName), (AnimationProvider)null, (AnimationProvider)null, viewShakeDescriptor, Collections.emptyMap());
         return this.withFireMode(fireModeDesriptor);
      }

      public Builder withFireMode(String name, FireMode type, Component displayName, Supplier<AmmoItem> ammoSupplier, int maxAmmoCapacity, int rpm, int burstShots, double damage, int maxShootingDistance, boolean isUsingDefaultMuzzle, String fireAnimationName, FireModeInstance.ViewShakeDescriptor viewShakeDescriptor) {
         FireModeDescriptor fireModeDesriptor = new FireModeDescriptor(name, displayName, type, ammoSupplier, maxAmmoCapacity, rpm, burstShots, damage, 200, 0, 1.0D, isUsingDefaultMuzzle, (AnimationProvider)null, new AnimationProvider.Simple(fireAnimationName), (AnimationProvider)null, (AnimationProvider)null, viewShakeDescriptor, Collections.emptyMap());
         return this.withFireMode(fireModeDesriptor);
      }

      public Builder withFireMode(String name, FireMode type, Component displayName, Supplier<AmmoItem> ammoSupplier, int maxAmmoCapacity, int rpm, int burstShots, double damage, int maxShootingDistance, boolean isUsingDefaultMuzzle, AnimationProvider fireAnimationProvider, FireModeInstance.ViewShakeDescriptor viewShakeDescriptor) {
         FireModeDescriptor fireModeDesriptor = new FireModeDescriptor(name, displayName, type, ammoSupplier, maxAmmoCapacity, rpm, burstShots, damage, 200, 0, 1.0D, isUsingDefaultMuzzle, (AnimationProvider)null, fireAnimationProvider, (AnimationProvider)null, (AnimationProvider)null, viewShakeDescriptor, Collections.emptyMap());
         return this.withFireMode(fireModeDesriptor);
      }

      public Builder withFireMode(FireModeDescriptor descriptor) {
         this.fireModes.add(descriptor);
         return this;
      }

      public Builder withJsonObject(JsonObject obj) {
         if (obj.has("condition")) {
            this.withCondition(Conditions.fromJson(obj.getAsJsonObject("condition")));
         }

         Iterator var2 = JsonUtil.getJsonObjects(obj, "fireModes").iterator();

         while(var2.hasNext()) {
            JsonObject fireModeObj = (JsonObject)var2.next();
            FireModeDescriptor.Builder fireModeBuilder = new FireModeDescriptor.Builder();
            String name = JsonUtil.getJsonString(fireModeObj, "name");
            fireModeBuilder.withName(name);
            fireModeBuilder.withDisplayName(Component.m_237115_(JsonUtil.getJsonString(fireModeObj, "displayName", name)));
            fireModeBuilder.withType((FireMode)JsonUtil.getEnum(fireModeObj, "type", FireMode.class, FireMode.SINGLE, true));
            String ammoName = JsonUtil.getJsonString(fireModeObj, "ammo", (String)null);
            Supplier<?> ammoSupplier = null;
            if (ammoName != null) {
               ammoSupplier = ItemRegistry.ITEMS.getDeferredRegisteredObject(ammoName);
            } else {
               ammoSupplier = AmmoRegistry.DEFAULT_AMMO_POOL;
            }

            fireModeBuilder.withAmmoSupplier(ammoSupplier);
            fireModeBuilder.withRpm(JsonUtil.getJsonInt(fireModeObj, "rpm", -1));
            fireModeBuilder.withBurstShots(JsonUtil.getJsonInt(fireModeObj, "burstShots", -1));
            fireModeBuilder.withMaxAmmoCapacity(JsonUtil.getJsonInt(fireModeObj, "maxAmmoCapacity", 1));
            fireModeBuilder.withPelletCount(JsonUtil.getJsonInt(fireModeObj, "pelletCount", 0));
            fireModeBuilder.withPelletSpread(JsonUtil.getJsonDouble(fireModeObj, "pelletSpread", 1.0D));
            fireModeBuilder.withIsUsingDefaultMuzzle(JsonUtil.getJsonBoolean(fireModeObj, "isUsingDefaultMuzzle", true));
            FireModeInstance.ViewShakeDescriptor viewShakeDescriptor = null;
            if (fireModeObj.has("shakeRecoilAmplitude") || fireModeObj.has("shakeRecoilSpeed") || fireModeObj.has("shakeRecoilDuration")) {
               long shakeRecoilDuration = (long)JsonUtil.getJsonInt(fireModeObj, "shakeRecoilDuration", 400);
               double shakeRecoilAmplitude = JsonUtil.getJsonDouble(fireModeObj, "shakeRecoilAmplitude", 0.5D);
               double shakeRecoilSpeed = JsonUtil.getJsonDouble(fireModeObj, "shakeRecoilSpeed", 8.0D);
               viewShakeDescriptor = new FireModeInstance.ViewShakeDescriptor(shakeRecoilDuration, shakeRecoilAmplitude, shakeRecoilSpeed);
            }

            fireModeBuilder.withViewShakeDescriptor(viewShakeDescriptor);
            fireModeBuilder.withDamage(JsonUtil.getJsonDouble(fireModeObj, "damage", 5.0D));
            fireModeBuilder.withMaxShootingDistance(JsonUtil.getJsonInt(fireModeObj, "maxShootingDistance", 200));
            String fireAnimationName = JsonUtil.getJsonString(fireModeObj, "animationName", (String)null);
            if (fireAnimationName != null) {
               fireModeBuilder.withFireAnimationProvider(new AnimationProvider.Simple(fireAnimationName));
            }

            ConditionalAnimationProvider.Builder fireAnimationProvider = new ConditionalAnimationProvider.Builder();
            Iterator var22 = JsonUtil.getJsonObjects(fireModeObj, "fireAnimations").iterator();

            while(var22.hasNext()) {
               JsonObject fireAnimationObj = (JsonObject)var22.next();
               String animationName = JsonUtil.getJsonString(fireAnimationObj, "name");
               Predicate<ConditionContext> animationCondition = Conditions.fromJson(fireAnimationObj.get("condition"));
               fireAnimationProvider.withAnimation(animationName, animationCondition, 0L, TimeUnit.MILLISECOND);
            }

            if (!fireAnimationProvider.getAnimations().isEmpty()) {
               fireModeBuilder.withFireAnimationProvider(fireAnimationProvider.build());
            }

            ConditionalAnimationProvider.Builder prepareFireAnimationProvider = new ConditionalAnimationProvider.Builder();

            int animationDuration;
            Predicate animationCondition;
            String animationName;
            for(Iterator var24 = JsonUtil.getJsonObjects(fireModeObj, "prepareFireAnimations").iterator(); var24.hasNext(); prepareFireAnimationProvider.withAnimation(animationName, animationCondition, (long)animationDuration, TimeUnit.MILLISECOND)) {
               JsonObject prepareFireAnimationObj = (JsonObject)var24.next();
               animationName = JsonUtil.getJsonString(prepareFireAnimationObj, "name");
               animationDuration = JsonUtil.getJsonInt(prepareFireAnimationObj, "duration");
               if (prepareFireAnimationObj.has("condition")) {
                  animationCondition = Conditions.fromJson(prepareFireAnimationObj.get("condition"));
               } else {
                  animationCondition = (ctx) -> {
                     return true;
                  };
               }
            }

            if (!prepareFireAnimationProvider.getAnimations().isEmpty()) {
               fireModeBuilder.withPrepareFireAnimationProvider(prepareFireAnimationProvider.build());
            }

            ConditionalAnimationProvider.Builder completeFireAnimationProvider = new ConditionalAnimationProvider.Builder();

            Predicate animationCondition;
            String animationName;
            int animationDuration;
            for(Iterator var28 = JsonUtil.getJsonObjects(fireModeObj, "completeFireAnimations").iterator(); var28.hasNext(); completeFireAnimationProvider.withAnimation(animationName, animationCondition, (long)animationDuration, TimeUnit.MILLISECOND)) {
               JsonObject completeFireAnimationObj = (JsonObject)var28.next();
               animationName = JsonUtil.getJsonString(completeFireAnimationObj, "name");
               animationDuration = JsonUtil.getJsonInt(completeFireAnimationObj, "duration");
               if (completeFireAnimationObj.has("condition")) {
                  animationCondition = Conditions.fromJson(completeFireAnimationObj.get("condition"));
               } else {
                  animationCondition = (ctx) -> {
                     return true;
                  };
               }
            }

            if (!completeFireAnimationProvider.getAnimations().isEmpty()) {
               fireModeBuilder.withCompleteFireAnimationProvider(completeFireAnimationProvider.build());
            }

            ConditionalAnimationProvider.Builder enableFireModeAnimationProvider = new ConditionalAnimationProvider.Builder();
            Iterator var32 = JsonUtil.getJsonObjects(fireModeObj, "enableFireModeAnimations").iterator();

            JsonObject effect;
            while(var32.hasNext()) {
               effect = (JsonObject)var32.next();
               String animationName = JsonUtil.getJsonString(effect, "name");
               int animationDuration = JsonUtil.getJsonInt(effect, "duration");
               Predicate<ConditionContext> animationCondition = Conditions.fromJson(obj.get("condition"));
               enableFireModeAnimationProvider.withAnimation(animationName, animationCondition, (long)animationDuration, TimeUnit.MILLISECOND);
            }

            if (!enableFireModeAnimationProvider.getAnimations().isEmpty()) {
               fireModeBuilder.withEnableFireModeAnimationProvider(enableFireModeAnimationProvider.build());
            }

            Predicate condition;
            GunItem.FirePhase firePhase;
            Supplier supplier;
            for(var32 = JsonUtil.getJsonObjects(fireModeObj, "effects").iterator(); var32.hasNext(); fireModeBuilder.withEffect(firePhase, supplier, condition)) {
               effect = (JsonObject)var32.next();
               firePhase = (GunItem.FirePhase)JsonUtil.getEnum(effect, "phase", GunItem.FirePhase.class, (Enum)null, true);
               String effectName = JsonUtil.getJsonString(effect, "name");
               supplier = () -> {
                  return (EffectBuilder)EffectRegistry.getEffectBuilderSupplier(effectName).get();
               };
               condition = Conditions.selectedFireMode(name);
               if (effect.has("condition")) {
                  JsonObject conditionObj = effect.getAsJsonObject("condition");
                  condition = condition.and(Conditions.fromJson(conditionObj));
               }
            }

            this.withFireMode(fireModeBuilder.build());
         }

         return this;
      }

      public FireModeFeature build(FeatureProvider featureProvider) {
         return new FireModeFeature(featureProvider, this.condition, Collections.unmodifiableList(this.fireModes.stream().map((info) -> {
            return FireModeInstance.create(info.name, featureProvider, info.displayName, info.type, info.ammoSupplier, info.maxAmmoCapacity, info.rpm, info.burstShots, info.damage, info.maxShootingDistance, info.pelletCount, info.pelletSpread, info.isUsingDefaultMuzzle, info.prepareFireAnimationProvider, info.fireAnimationProvider, info.completeFireAnimationProvider, info.enableFireModeAnimationProvider, info.viewShakeDescriptor, info.effectBuilders);
         }).toList()));
      }
   }

   public static record FireModeDescriptor(String name, Component displayName, FireMode type, Supplier<AmmoItem> ammoSupplier, int maxAmmoCapacity, int rpm, int burstShots, double damage, int maxShootingDistance, int pelletCount, double pelletSpread, boolean isUsingDefaultMuzzle, AnimationProvider prepareFireAnimationProvider, AnimationProvider fireAnimationProvider, AnimationProvider completeFireAnimationProvider, AnimationProvider enableFireModeAnimationProvider, FireModeInstance.ViewShakeDescriptor viewShakeDescriptor, Map<GunItem.FirePhase, List<Pair<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>, Predicate<ConditionContext>>>> effectBuilders) {
      public FireModeDescriptor(String name, Component displayName, FireMode type, Supplier<AmmoItem> ammoSupplier, int maxAmmoCapacity, int rpm, int burstShots, double damage, int maxShootingDistance, int pelletCount, double pelletSpread, boolean isUsingDefaultMuzzle, AnimationProvider prepareFireAnimationProvider, AnimationProvider fireAnimationProvider, AnimationProvider completeFireAnimationProvider, AnimationProvider enableFireModeAnimationProvider, FireModeInstance.ViewShakeDescriptor viewShakeDescriptor, Map<GunItem.FirePhase, List<Pair<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>, Predicate<ConditionContext>>>> effectBuilders) {
         this.name = name;
         this.displayName = displayName;
         this.type = type;
         this.ammoSupplier = ammoSupplier;
         this.maxAmmoCapacity = maxAmmoCapacity;
         this.rpm = rpm;
         this.burstShots = burstShots;
         this.damage = damage;
         this.maxShootingDistance = maxShootingDistance;
         this.pelletCount = pelletCount;
         this.pelletSpread = pelletSpread;
         this.isUsingDefaultMuzzle = isUsingDefaultMuzzle;
         this.prepareFireAnimationProvider = prepareFireAnimationProvider;
         this.fireAnimationProvider = fireAnimationProvider;
         this.completeFireAnimationProvider = completeFireAnimationProvider;
         this.enableFireModeAnimationProvider = enableFireModeAnimationProvider;
         this.viewShakeDescriptor = viewShakeDescriptor;
         this.effectBuilders = effectBuilders;
      }

      public String name() {
         return this.name;
      }

      public Component displayName() {
         return this.displayName;
      }

      public FireMode type() {
         return this.type;
      }

      public Supplier<AmmoItem> ammoSupplier() {
         return this.ammoSupplier;
      }

      public int maxAmmoCapacity() {
         return this.maxAmmoCapacity;
      }

      public int rpm() {
         return this.rpm;
      }

      public int burstShots() {
         return this.burstShots;
      }

      public double damage() {
         return this.damage;
      }

      public int maxShootingDistance() {
         return this.maxShootingDistance;
      }

      public int pelletCount() {
         return this.pelletCount;
      }

      public double pelletSpread() {
         return this.pelletSpread;
      }

      public boolean isUsingDefaultMuzzle() {
         return this.isUsingDefaultMuzzle;
      }

      public AnimationProvider prepareFireAnimationProvider() {
         return this.prepareFireAnimationProvider;
      }

      public AnimationProvider fireAnimationProvider() {
         return this.fireAnimationProvider;
      }

      public AnimationProvider completeFireAnimationProvider() {
         return this.completeFireAnimationProvider;
      }

      public AnimationProvider enableFireModeAnimationProvider() {
         return this.enableFireModeAnimationProvider;
      }

      public FireModeInstance.ViewShakeDescriptor viewShakeDescriptor() {
         return this.viewShakeDescriptor;
      }

      public Map<GunItem.FirePhase, List<Pair<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>, Predicate<ConditionContext>>>> effectBuilders() {
         return this.effectBuilders;
      }

      public static class Builder {
         private String name;
         private FireMode type;
         private Component displayName;
         private Supplier<AmmoItem> ammoSupplier;
         private int maxAmmoCapacity;
         private int rpm;
         private int burstShots;
         private double damage;
         private int maxShootingDistance;
         private boolean isUsingDefaultMuzzle;
         private AnimationProvider prepareFireAnimationProvider;
         private AnimationProvider fireAnimationProvider;
         private AnimationProvider completeFireAnimationProvider;
         private AnimationProvider enableFireModeAnimationProvider;
         private FireModeInstance.ViewShakeDescriptor viewShakeDescriptor;
         private int pelletCount;
         private double pelletSpread;
         private Map<GunItem.FirePhase, List<Pair<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>, Predicate<ConditionContext>>>> effectBuilders;

         public Builder() {
            this.type = FireMode.SINGLE;
            this.ammoSupplier = AmmoRegistry.DEFAULT_AMMO_POOL;
            this.maxAmmoCapacity = 1;
            this.rpm = -1;
            this.burstShots = -1;
            this.damage = 5.0D;
            this.maxShootingDistance = 200;
            this.isUsingDefaultMuzzle = true;
            this.fireAnimationProvider = FireModeFeature.DEFAULT_ANIMATION_PROVIDER;
            this.pelletSpread = 1.0D;
            this.effectBuilders = new HashMap();
         }

         public Builder withName(String name) {
            this.name = name;
            return this;
         }

         public Builder withType(FireMode type) {
            this.type = type;
            return this;
         }

         public Builder withDisplayName(Component displayName) {
            this.displayName = displayName;
            return this;
         }

         public Builder withAmmoSupplier(Supplier<AmmoItem> ammoSupplier) {
            this.ammoSupplier = ammoSupplier;
            return this;
         }

         public Builder withMaxAmmoCapacity(int maxAmmoCapacity) {
            this.maxAmmoCapacity = maxAmmoCapacity;
            return this;
         }

         public Builder withRpm(int rpm) {
            this.rpm = rpm;
            return this;
         }

         public Builder withBurstShots(int burstShots) {
            this.burstShots = burstShots;
            return this;
         }

         public Builder withDamage(double damage) {
            this.damage = damage;
            return this;
         }

         public Builder withMaxShootingDistance(int maxShootingDistance) {
            this.maxShootingDistance = maxShootingDistance;
            return this;
         }

         public Builder withPelletCount(int pelletCount) {
            this.pelletCount = pelletCount;
            return this;
         }

         public Builder withPelletSpread(double pelletSpread) {
            this.pelletSpread = pelletSpread;
            return this;
         }

         public Builder withIsUsingDefaultMuzzle(boolean isUsingDefaultMuzzle) {
            this.isUsingDefaultMuzzle = isUsingDefaultMuzzle;
            return this;
         }

         public Builder withPrepareFireAnimationProvider(AnimationProvider prepareFireAnimationProvider) {
            this.prepareFireAnimationProvider = prepareFireAnimationProvider;
            return this;
         }

         public Builder withFireAnimationProvider(AnimationProvider fireAnimationProvider) {
            this.fireAnimationProvider = fireAnimationProvider;
            return this;
         }

         public Builder withCompleteFireAnimationProvider(AnimationProvider completeFireAnimationProvider) {
            this.completeFireAnimationProvider = completeFireAnimationProvider;
            return this;
         }

         public Builder withEnableFireModeAnimationProvider(AnimationProvider enableFireModeAnimationProvider) {
            this.enableFireModeAnimationProvider = enableFireModeAnimationProvider;
            return this;
         }

         public Builder withViewShakeDescriptor(FireModeInstance.ViewShakeDescriptor viewShakeDescriptor) {
            this.viewShakeDescriptor = viewShakeDescriptor;
            return this;
         }

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

         public FireModeDescriptor build() {
            if (this.pelletCount > 1) {
               this.maxShootingDistance = 50;
            }

            return new FireModeDescriptor(this.name, (Component)(this.displayName != null ? this.displayName : Component.m_237115_("label.pointblank.fireMode.single")), this.type, this.ammoSupplier, this.maxAmmoCapacity, this.rpm, this.burstShots, this.damage, this.maxShootingDistance, this.pelletCount, this.pelletSpread, this.isUsingDefaultMuzzle, this.prepareFireAnimationProvider, this.fireAnimationProvider, this.completeFireAnimationProvider, this.enableFireModeAnimationProvider, this.viewShakeDescriptor, this.effectBuilders);
         }
      }
   }
}
