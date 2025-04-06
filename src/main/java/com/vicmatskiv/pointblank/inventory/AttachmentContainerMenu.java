package com.vicmatskiv.pointblank.inventory;

import com.vicmatskiv.pointblank.attachment.AttachmentCategory;
import com.vicmatskiv.pointblank.attachment.AttachmentHost;
import com.vicmatskiv.pointblank.registry.MenuRegistry;
import java.util.Map;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AttachmentContainerMenu extends AbstractContainerMenu {
   private static final Logger LOGGER = LogManager.getLogger("pointblank");
   private static final int MAX_TOP_LEVEL_ATTACHMENTS = 11;
   private static final int ATTACHMENT_CONTAINERS = 10;
   private static final int ATTACHMENTS_PER_CONTAINER = 6;
   private SlotMapping slotMapping;
   private Inventory playerInventory;
   private VirtualInventory virtualInventory;
   private SimpleAttachmentContainer[] attachmentContainers;
   private int totalAttachmentSlots;
   private int slotWidth;
   private int slotHeight;
   private int mainInventoryLeftOffset;
   private int mainInventoryTopOffset;
   private int hotBarTopOffset;
   private int attachmentsLeftOffset;
   private int attachmentsTopOffset;
   private int attachmentsSlotRightPadding;
   private int attachmentsHeaderBottomPadding;

   public AttachmentContainerMenu(int containerId, Inventory playerInventory) {
      this(containerId, playerInventory, playerInventory.m_36056_());
   }

   public AttachmentContainerMenu(int containerId, Inventory playerInventory, ItemStack itemStack) {
      super((MenuType)MenuRegistry.ATTACHMENTS.get(), containerId);
      AttachmentHost attachmentHost = (AttachmentHost)itemStack.m_41720_();
      this.slotWidth = 18;
      this.slotHeight = 18;
      this.hotBarTopOffset = 220;
      this.mainInventoryLeftOffset = 105;
      this.mainInventoryTopOffset = 162;
      this.attachmentsSlotRightPadding = 4;
      this.attachmentsHeaderBottomPadding = 2;
      this.attachmentsLeftOffset = 6;
      this.attachmentsTopOffset = 9;
      this.playerInventory = playerInventory;
      this.slotMapping = SlotMapping.getOrCreate(playerInventory.f_35978_);
      this.virtualInventory = VirtualInventory.createInventory(playerInventory.f_35978_, playerInventory.m_36056_());
      this.attachmentContainers = new SimpleAttachmentContainer[10];
      this.totalAttachmentSlots = Math.min(attachmentHost.getMaxAttachmentCategories(), 11) + 63;

      try {
         this.initAttachmentContainers();
         this.updateAttachmentSlots();
         this.addInventorySlots();
         this.addHotbarSlots();
      } catch (Exception var6) {
         LOGGER.error("Failed to initialize attachment container menu", var6);
      }

   }

   public Inventory getPlayerInventory() {
      return this.playerInventory;
   }

   public SimpleAttachmentContainer[] getAttachmentContainers() {
      return this.attachmentContainers;
   }

   public int getTotalAttachmentSlots() {
      return this.totalAttachmentSlots;
   }

   private void addInventorySlots() {
      for(int i = 0; i < 3; ++i) {
         for(int j = 0; j < 9; ++j) {
            this.m_38897_(new Slot(this.playerInventory, j + i * 9 + 9, this.mainInventoryLeftOffset + j * this.slotWidth, this.mainInventoryTopOffset + i * this.slotHeight));
         }
      }

   }

   private void addHotbarSlots() {
      for(int i = 0; i < 9; ++i) {
         if (i == this.playerInventory.f_35977_) {
            this.m_38897_(new Slot(this.playerInventory, i, this.mainInventoryLeftOffset + i * this.slotWidth, this.hotBarTopOffset) {
               public boolean m_8010_(Player playerIn) {
                  return false;
               }
            });
         } else {
            this.m_38897_(new Slot(this.playerInventory, i, this.mainInventoryLeftOffset + i * this.slotWidth, this.hotBarTopOffset));
         }
      }

   }

   private void initAttachmentContainers() {
      for(int containerIndex = 0; containerIndex < 10; ++containerIndex) {
         this.attachmentContainers[containerIndex] = this.initAttachmentContainer(containerIndex);
      }

   }

   private SimpleAttachmentContainer initAttachmentContainer(int containerIndex) {
      int attachmentSlotCount = containerIndex == 0 ? 11 : 6;
      SimpleAttachmentContainer attachmentContainer = new SimpleAttachmentContainer(containerIndex, this, attachmentSlotCount + 1);
      LOGGER.debug("Created container {}", containerIndex);
      Slot attachmentHeadingSlot = new AttachmentHeadingSlot(attachmentContainer, 0, this.attachmentsLeftOffset + containerIndex * (this.slotWidth + this.attachmentsSlotRightPadding), this.attachmentsTopOffset, attachmentContainer);
      this.m_150443_();

      try {
         this.m_38897_(attachmentHeadingSlot);

         for(int attachmentInContainerIndex = 1; attachmentInContainerIndex < attachmentSlotCount + 1; ++attachmentInContainerIndex) {
            AttachmentSlot attachmentSlot = new AttachmentSlot(this.playerInventory.f_35978_, this, attachmentContainer, attachmentInContainerIndex, this.attachmentsLeftOffset + containerIndex * (this.slotWidth + this.attachmentsSlotRightPadding), this.attachmentsTopOffset + this.attachmentsHeaderBottomPadding + attachmentInContainerIndex * this.slotHeight);
            attachmentSlot.setActive(false);
            this.m_38897_(attachmentSlot);
            LOGGER.debug("Added attachment slot {} to container {}", attachmentInContainerIndex, attachmentContainer);
         }
      } finally {
         this.m_150444_();
      }

      return attachmentContainer;
   }

   private void clearContainer(int i, SimpleAttachmentContainer container) {
      container.setParentContainer(container);
      container.removeAllListeners();
      container.m_6211_();
      container.setVirtualInventory((VirtualInventory)null);
      int startSlotIndex = SimpleAttachmentContainer.getContainerStartIndex(this.attachmentContainers, i);

      for(int attachmentSlotIndex = 1; attachmentSlotIndex < container.m_6643_(); ++attachmentSlotIndex) {
         int absoluteSlotIndex = startSlotIndex + attachmentSlotIndex;
         ((Activatable)this.m_38853_(absoluteSlotIndex)).setActive(false);
      }

   }

   void updateAttachmentSlots() {
      this.m_150443_();

      try {
         int lastContainerIndex = this.updateAttachmentSlots((AttachmentSlot)null, (SimpleAttachmentContainer)null, this.virtualInventory, 0);

         for(int i = lastContainerIndex + 1; i < this.attachmentContainers.length; ++i) {
            this.clearContainer(i, this.attachmentContainers[i]);
         }
      } finally {
         this.m_150444_();
      }

   }

   private int updateAttachmentSlots(AttachmentSlot parentSlot, SimpleAttachmentContainer parentContainer, VirtualInventory virtualInventory, int containerIndex) {
      if (virtualInventory == null) {
         return -1;
      } else if (containerIndex >= this.attachmentContainers.length) {
         LOGGER.error("Requested container index {} exceeds the max {}", containerIndex, this.attachmentContainers.length - 1);
         return -1;
      } else {
         int maxContainerIndex = containerIndex;
         SimpleAttachmentContainer container = this.attachmentContainers[containerIndex];
         container.removeAllListeners();
         container.m_6211_();
         container.setVirtualInventory(virtualInventory);
         container.setParentContainer(parentContainer);
         if (parentSlot != null) {
            parentSlot.setChildContainer(container);
         }

         LOGGER.debug("Updating attachment slots for inventory {} for container {}", virtualInventory, containerIndex);
         Map<Integer, AttachmentCategory> mapping = this.slotMapping.getOrCreateSlotMapping(virtualInventory);
         int startSlotIndex = SimpleAttachmentContainer.getContainerStartIndex(this.attachmentContainers, containerIndex);
         ItemStack headerStack = virtualInventory.getItemStack().m_41777_();
         container.m_6836_(0, headerStack);
         HierarchicalSlot headerSlot = (HierarchicalSlot)this.m_38853_(startSlotIndex);
         headerSlot.setParentSlot(parentSlot);
         int activeAttachmentCount = 0;

         for(int attachmentSlotIndex = 1; attachmentSlotIndex < container.m_6643_(); ++attachmentSlotIndex) {
            int absoluteSlotIndex = startSlotIndex + attachmentSlotIndex;
            AttachmentSlot slot = (AttachmentSlot)this.m_38853_(absoluteSlotIndex);
            slot.clear();
            slot.setParentSlot(parentSlot);
            if (attachmentSlotIndex - 1 < virtualInventory.getElements().size()) {
               AttachmentCategory category = (AttachmentCategory)mapping.get(attachmentSlotIndex);
               if (category != null) {
                  VirtualInventory mappedElement = virtualInventory.getElement(category);
                  if (mappedElement != null) {
                     container.m_6836_(attachmentSlotIndex, mappedElement.getItemStack().m_41777_());
                     LOGGER.debug("Updated slot #{} in container {} with item {}", attachmentSlotIndex, container, container.m_8020_(attachmentSlotIndex));
                     if (!mappedElement.getElements().isEmpty()) {
                        int subIndex = this.updateAttachmentSlots(slot, container, mappedElement, maxContainerIndex + 1);
                        if (subIndex > maxContainerIndex) {
                           maxContainerIndex = subIndex;
                        }
                     }
                  }
               } else {
                  container.m_6836_(attachmentSlotIndex, ItemStack.f_41583_);
                  LOGGER.debug("Updated slot #{} in container {} with empty item {}", attachmentSlotIndex, container, container.m_8020_(attachmentSlotIndex));
               }

               slot.setActive(true);
               ++activeAttachmentCount;
            } else {
               slot.setActive(false);
            }
         }

         LOGGER.debug("Container active attachment count: {} in container {}", activeAttachmentCount, container);
         ((Activatable)this.m_38853_(startSlotIndex)).setActive(activeAttachmentCount > 0);
         container.addListener(virtualInventory);
         LOGGER.debug("Updated attachment slots for container {}", container);
         return maxContainerIndex;
      }
   }

   SlotMapping getSlotMapping() {
      return this.slotMapping;
   }

   public boolean m_6875_(Player player) {
      return true;
   }

   public void m_6199_(Container inventory) {
      LOGGER.debug("Slots changed for container {}", inventory);
      super.m_38946_();
   }

   public ItemStack m_7648_(Player player, int slotIndex) {
      ItemStack copyStack = ItemStack.f_41583_;
      Slot slot = (Slot)this.f_38839_.get(slotIndex);
      if (slot != null && slot.m_6657_()) {
         ItemStack slotStack = slot.m_7993_();
         copyStack = slotStack.m_41777_();
         if (slotIndex < this.totalAttachmentSlots) {
            if (!this.m_38903_(slotStack, this.totalAttachmentSlots, this.f_38839_.size(), true)) {
               return ItemStack.f_41583_;
            }
         } else if (!this.m_38903_(slotStack, 0, this.totalAttachmentSlots, false)) {
            return ItemStack.f_41583_;
         }

         if (slotStack.m_41619_()) {
            slot.m_5852_(ItemStack.f_41583_);
         } else {
            slot.m_6654_();
         }
      }

      return copyStack;
   }
}
