package com.vicmatskiv.pointblank.util;

import java.util.function.Predicate;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

public final class CancellableSound extends AbstractTickableSoundInstance {
   private Predicate<CancellableSound> predicate;
   private Player player;

   public CancellableSound(Player player, SoundEvent soundEvent, SoundSource soundSource, RandomSource randomSource, Predicate<CancellableSound> predicate) {
      super(soundEvent, soundSource, randomSource);
      this.player = player;
      this.predicate = predicate;
   }

   public void m_7788_() {
      if (!this.predicate.test(this)) {
         this.m_119609_();
      }

   }

   public double m_7772_() {
      return this.player.m_20185_();
   }

   public double m_7780_() {
      return this.player.m_20186_();
   }

   public double m_7778_() {
      return this.player.m_20189_();
   }
}
