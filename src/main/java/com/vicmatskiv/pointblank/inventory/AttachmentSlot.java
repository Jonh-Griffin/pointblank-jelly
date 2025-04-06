package com.vicmatskiv.pointblank.inventory;

import com.vicmatskiv.pointblank.Nameable;
import com.vicmatskiv.pointblank.attachment.Attachment;
import com.vicmatskiv.pointblank.attachment.Attachments;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AttachmentSlot extends Slot implements Activatable, HierarchicalSlot {
   private static final Logger LOGGER = LogManager.getLogger("pointblank");
   private AttachmentContainerMenu menu;
   private boolean isActive;
   private SimpleAttachmentContainer container;
   private int slotIndexInContainer;
   private SimpleAttachmentContainer childContainer;
   private HierarchicalSlot parentSlot;

   public AttachmentSlot(Player player, AttachmentContainerMenu menu, SimpleAttachmentContainer container, int index, int x, int y) {
      super(container, index, x, y);
      this.slotIndexInContainer = index;
      this.container = container;
      this.menu = menu;
   }

   public SimpleAttachmentContainer getContainer() {
      return this.container;
   }

   public boolean m_6659_() {
      return this.isActive;
   }

   void clear() {
      this.childContainer = null;
      this.parentSlot = null;
   }

   public String getPath() {
      String var10000;
      String parentPath;
      label16: {
         parentPath = this.getParentSlot() != null ? this.getParentSlot().getPath() : "/";
         ItemStack itemStack = this.m_7993_();
         if (itemStack != null) {
            Item var5 = itemStack.m_41720_();
            if (var5 instanceof Nameable) {
               Nameable n = (Nameable)var5;
               var10000 = n.getName();
               break label16;
            }
         }

         var10000 = "?";
      }

      String name = var10000;
      return parentPath + "/" + name;
   }

   public HierarchicalSlot getParentSlot() {
      return this.parentSlot;
   }

   public void setParentSlot(HierarchicalSlot parentSlot) {
      this.parentSlot = parentSlot;
   }

   void setChildContainer(SimpleAttachmentContainer childContainer) {
      this.childContainer = childContainer;
   }

   public SimpleAttachmentContainer getChildContainer() {
      return this.childContainer;
   }

   public boolean m_5857_(ItemStack newAttachmentStack) {
      if (!this.isActive) {
         return false;
      } else if (!(newAttachmentStack.m_41720_() instanceof Attachment)) {
         return false;
      } else {
         ItemStack currentItemStack = this.m_7993_();
         if (currentItemStack != null && currentItemStack.m_41720_() instanceof Attachment) {
            return false;
         } else {
            VirtualInventory e = this.container.getVirtualInventory();
            return e != null ? e.mayPlace(newAttachmentStack, this) : false;
         }
      }
   }

   public int m_6641_() {
      return 1;
   }

   public boolean m_8010_(Player player) {
      if (!this.isActive) {
         return false;
      } else {
         ItemStack currentItemStack = this.m_7993_();
         if (currentItemStack != null && !currentItemStack.m_41619_() && !player.m_7500_()) {
            return !(currentItemStack.m_41720_() instanceof Attachment) ? true : Attachments.isRemoveable(currentItemStack.m_41783_());
         } else {
            return true;
         }
      }
   }

   public void setActive(boolean isActive) {
      if (isActive != this.isActive) {
         LOGGER.debug("Changing status for slot {} in container {} to {},", this.slotIndexInContainer, this.container, isActive);
      }

      this.isActive = isActive;
   }

   public void m_269060_(ItemStack itemStack) {
      this.doSet(itemStack);
   }

   private void doSet(ItemStack itemStack) {
      LOGGER.debug("Setting attachment slot {} for container {} to stack {} with tag {}", this.slotIndexInContainer, this.container, itemStack, itemStack.m_41783_());
      super.m_5852_(itemStack);
   }

   public void m_5852_(ItemStack itemStack) {
      this.doSet(itemStack);
   }

   public void m_6654_() {
   }
}
