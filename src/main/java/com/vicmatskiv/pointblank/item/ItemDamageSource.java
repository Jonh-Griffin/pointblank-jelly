package com.vicmatskiv.pointblank.item;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.Item;

public class ItemDamageSource extends DamageSource {
   private final Item item;

   public ItemDamageSource(DamageSource source, Item item) {
      super(source.m_269150_(), source.m_7640_(), source.m_7639_(), source.m_269181_());
      this.item = item;
   }

   public Item getItem() {
      return this.item;
   }
}
