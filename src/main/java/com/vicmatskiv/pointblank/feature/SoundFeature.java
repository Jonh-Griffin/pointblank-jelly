package com.vicmatskiv.pointblank.feature;

import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.vicmatskiv.pointblank.item.GunItem;
import com.vicmatskiv.pointblank.registry.SoundRegistry;
import com.vicmatskiv.pointblank.util.Conditions;
import com.vicmatskiv.pointblank.util.JsonUtil;
import com.vicmatskiv.pointblank.util.MiscUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class SoundFeature extends ConditionalFeature {
   private List<Pair<SoundDescriptor, Predicate<ConditionContext>>> fireSounds;

   private SoundFeature(FeatureProvider owner, Predicate<ConditionContext> predicate, List<Pair<SoundDescriptor, Predicate<ConditionContext>>> fireSounds) {
      super(owner, predicate);
      this.fireSounds = Collections.unmodifiableList(fireSounds);
   }

   public static SoundDescriptor getFireSoundAndVolume(ItemStack itemStack) {
      List<Features.EnabledFeature> enabledFeatures = Features.getEnabledFeatures(itemStack, SoundFeature.class);
      Iterator var2 = enabledFeatures.iterator();

      while(var2.hasNext()) {
         Features.EnabledFeature enabledFeature = (Features.EnabledFeature)var2.next();
         SoundFeature soundFeature = (SoundFeature)enabledFeature.feature();
         ConditionContext context = new ConditionContext(itemStack);
         Iterator var6 = soundFeature.fireSounds.iterator();

         while(var6.hasNext()) {
            Pair<SoundDescriptor, Predicate<ConditionContext>> fireSound = (Pair)var6.next();
            if (((Predicate)fireSound.getSecond()).test(context)) {
               return (SoundDescriptor)fireSound.getFirst();
            }
         }
      }

      return null;
   }

   public static void playFireSound(Player player, ItemStack itemStack) {
      SoundDescriptor fsv = getFireSoundAndVolume(itemStack);
      SoundEvent fireSound = null;
      float fireSoundVolume = 0.0F;
      if (fsv != null) {
         fireSound = (SoundEvent)fsv.soundSupplier.get();
         fireSoundVolume = fsv.volume;
      } else {
         Item var6 = itemStack.m_41720_();
         if (var6 instanceof GunItem) {
            GunItem gunItem = (GunItem)var6;
            fireSound = gunItem.getFireSound();
            fireSoundVolume = gunItem.getFireSoundVolume();
         }
      }

      if (fireSound != null && fireSoundVolume > 0.0F) {
         MiscUtil.getLevel(player).m_6263_(player, player.m_20185_(), player.m_20186_(), player.m_20189_(), fireSound, SoundSource.PLAYERS, fireSoundVolume, 1.0F);
      }

   }

   public static record SoundDescriptor(Supplier<SoundEvent> soundSupplier, float volume) {
      public SoundDescriptor(Supplier<SoundEvent> soundSupplier, float volume) {
         this.soundSupplier = soundSupplier;
         this.volume = volume;
      }

      public Supplier<SoundEvent> soundSupplier() {
         return this.soundSupplier;
      }

      public float volume() {
         return this.volume;
      }
   }

   public static class Builder implements FeatureBuilder<Builder, SoundFeature> {
      private static final float DEFAULT_FIRE_SOUND_VOLUME = 5.0F;
      private Predicate<ConditionContext> condition = (ctx) -> {
         return true;
      };
      private SoundDescriptor fireSoundDescriptor;
      private List<Pair<SoundDescriptor, Predicate<ConditionContext>>> fireSounds = new ArrayList();

      public Builder withCondition(Predicate<ConditionContext> condition) {
         this.condition = condition;
         return this;
      }

      public Builder withFireSound(Supplier<SoundEvent> sound, double volume) {
         this.fireSoundDescriptor = new SoundDescriptor(sound, (float)volume);
         return this;
      }

      public Builder withFireSound(Supplier<SoundEvent> sound, double volume, Predicate<ConditionContext> condition) {
         this.fireSounds.add(Pair.of(new SoundDescriptor(sound, (float)volume), condition));
         return this;
      }

      public Builder withJsonObject(JsonObject obj) {
         if (obj.has("condition")) {
            this.withCondition(Conditions.fromJson(obj.getAsJsonObject("condition")));
         }

         Iterator var2 = JsonUtil.getJsonObjects(obj, "fireSounds").iterator();

         while(var2.hasNext()) {
            JsonObject fireSoundObj = (JsonObject)var2.next();
            Predicate condition;
            if (fireSoundObj.has("condition")) {
               JsonObject conditionObj = fireSoundObj.getAsJsonObject("condition");
               condition = Conditions.fromJson(conditionObj);
            } else {
               condition = (ctx) -> {
                  return true;
               };
            }

            String fireSoundName = JsonUtil.getJsonString(fireSoundObj, "sound");
            float fireSoundVolume = JsonUtil.getJsonFloat(fireSoundObj, "volume", 5.0F);
            this.withFireSound(() -> {
               return SoundRegistry.getSoundEvent(fireSoundName);
            }, (double)fireSoundVolume, condition);
         }

         if (!obj.has("fireSounds")) {
            String fireSoundName = JsonUtil.getJsonString(obj, "fireSound");
            float fireSoundVolume = JsonUtil.getJsonFloat(obj, "fireSoundVolume", 5.0F);
            this.withFireSound(() -> {
               return SoundRegistry.getSoundEvent(fireSoundName);
            }, (double)fireSoundVolume);
         }

         return this;
      }

      public SoundFeature build(FeatureProvider featureProvider) {
         List<Pair<SoundDescriptor, Predicate<ConditionContext>>> fireSounds = new ArrayList(this.fireSounds);
         if (this.fireSoundDescriptor != null) {
            fireSounds.add(Pair.of(this.fireSoundDescriptor, this.condition));
         }

         return new SoundFeature(featureProvider, this.condition, fireSounds);
      }
   }
}
