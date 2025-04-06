package com.vicmatskiv.pointblank.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

final class AttachmentHeadingSlot extends Slot implements Activatable, HierarchicalSlot {
   private final SimpleAttachmentContainer attachmentContainer;
   private boolean isActive;
   private HierarchicalSlot parentSlot;

   AttachmentHeadingSlot(Container container, int index, int x, int y, SimpleAttachmentContainer attachmentContainer) {
      super(container, index, x, y);
      this.attachmentContainer = attachmentContainer;
   }

   public ItemStack m_7993_() {
      return this.attachmentContainer.getVirtualInventory() != null ? this.attachmentContainer.getVirtualInventory().getItemStack() : ItemStack.f_41583_;
   }

   public boolean m_280329_() {
      return false;
   }

   public boolean m_8010_(Player player) {
      return false;
   }

   public boolean m_5857_(ItemStack itemStack) {
      return false;
   }

   public boolean m_6659_() {
      return this.isActive;
   }

   public void setActive(boolean isActive) {
      this.isActive = isActive;
   }

   void clear() {
      this.parentSlot = null;
   }

   public String getPath() {
      String parentPath = this.getParentSlot() != null ? this.getParentSlot().getPath() : "/";
      return parentPath + "/";
   }

   public HierarchicalSlot getParentSlot() {
      return this.parentSlot;
   }

   public void setParentSlot(HierarchicalSlot parentSlot) {
      this.parentSlot = parentSlot;
   }
}
