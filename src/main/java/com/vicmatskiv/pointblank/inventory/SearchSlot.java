package com.vicmatskiv.pointblank.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class SearchSlot extends Slot {
   private int slotIndex;
   private boolean isHighlightable;

   public SearchSlot(Container container, int slotIndex, int posX, int posY, boolean isHighlightable) {
      super(container, slotIndex, posX, posY);
      this.slotIndex = slotIndex;
      this.isHighlightable = isHighlightable;
   }

   public boolean m_280329_() {
      return this.isHighlightable;
   }

   public boolean m_8010_(Player player) {
      return false;
   }

   public boolean m_5857_(ItemStack itemStack) {
      return false;
   }

   public String toString() {
      return String.format("SearchSlot {index: %d, container: %s}", this.slotIndex, System.identityHashCode(this.f_40218_));
   }
}
