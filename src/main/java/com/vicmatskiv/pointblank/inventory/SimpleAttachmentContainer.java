package com.vicmatskiv.pointblank.inventory;

import com.google.common.collect.Lists;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.ContainerListener;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.StackedContentsCompatible;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SimpleAttachmentContainer implements Container, StackedContentsCompatible {
   private static final Logger LOGGER = LogManager.getLogger("pointblank");
   private int containerIndex;
   private final int size;
   private final NonNullList<ItemStack> items;
   private List<ContainerListener> listeners;
   private AttachmentContainerMenu menu;
   private VirtualInventory virtualInventory;
   private SimpleAttachmentContainer parentContainer;

   public SimpleAttachmentContainer(int containerIndex, AttachmentContainerMenu menu, int size) {
      this.containerIndex = containerIndex;
      this.menu = menu;
      this.size = size;
      this.items = NonNullList.m_122780_(size, ItemStack.f_41583_);
   }

   public SimpleAttachmentContainer(ItemStack... itemStacks) {
      this.size = itemStacks.length;
      this.items = NonNullList.m_122783_(ItemStack.f_41583_, itemStacks);
   }

   public int getContainerIndex() {
      return this.containerIndex;
   }

   void setParentContainer(SimpleAttachmentContainer parentContainer) {
      this.parentContainer = parentContainer;
   }

   public SimpleAttachmentContainer getParentContainer() {
      return this.parentContainer;
   }

   void setVirtualInventory(VirtualInventory virtualInventory) {
      this.virtualInventory = virtualInventory;
   }

   public VirtualInventory getVirtualInventory() {
      return this.virtualInventory;
   }

   public void addListener(ContainerListener listener) {
      if (this.listeners == null) {
         this.listeners = Lists.newArrayList();
      }

      this.listeners.add(listener);
   }

   public void removeListener(ContainerListener listeners) {
      if (this.listeners != null) {
         this.listeners.remove(listeners);
      }

   }

   public void removeAllListeners() {
      this.listeners = null;
   }

   public ItemStack m_8020_(int index) {
      return index >= 0 && index < this.items.size() ? (ItemStack)this.items.get(index) : ItemStack.f_41583_;
   }

   public List<ItemStack> removeAllItems() {
      List<ItemStack> list = (List)this.items.stream().filter((itemStack) -> {
         return !itemStack.m_41619_();
      }).collect(Collectors.toList());
      this.m_6211_();
      return list;
   }

   public ItemStack m_7407_(int p_19159_, int p_19160_) {
      ItemStack itemstack = ContainerHelper.m_18969_(this.items, p_19159_, p_19160_);
      if (!itemstack.m_41619_()) {
         this.m_6596_();
      }

      return itemstack;
   }

   public ItemStack removeItemType(Item item, int p_19172_) {
      ItemStack itemstack = new ItemStack(item, 0);

      for(int i = this.size - 1; i >= 0; --i) {
         ItemStack itemstack1 = this.m_8020_(i);
         if (itemstack1.m_41720_().equals(item)) {
            int j = p_19172_ - itemstack.m_41613_();
            ItemStack itemstack2 = itemstack1.m_41620_(j);
            itemstack.m_41769_(itemstack2.m_41613_());
            if (itemstack.m_41613_() == p_19172_) {
               break;
            }
         }
      }

      if (!itemstack.m_41619_()) {
         this.m_6596_();
      }

      return itemstack;
   }

   public ItemStack addItem(ItemStack itemStack) {
      if (itemStack.m_41619_()) {
         return ItemStack.f_41583_;
      } else {
         ItemStack itemStackCopy = itemStack.m_41777_();
         this.moveItemToOccupiedSlotsWithSameType(itemStackCopy);
         if (itemStackCopy.m_41619_()) {
            return ItemStack.f_41583_;
         } else {
            this.moveItemToEmptySlots(itemStackCopy);
            return itemStackCopy.m_41619_() ? ItemStack.f_41583_ : itemStackCopy;
         }
      }
   }

   public boolean canAddItem(ItemStack item) {
      boolean flag = false;
      Iterator var3 = this.items.iterator();

      while(var3.hasNext()) {
         ItemStack itemstack = (ItemStack)var3.next();
         if (itemstack.m_41619_() || ItemStack.m_150942_(itemstack, item) && itemstack.m_41613_() < itemstack.m_41741_()) {
            flag = true;
            break;
         }
      }

      return flag;
   }

   public ItemStack m_8016_(int itemIndex) {
      ItemStack itemstack = (ItemStack)this.items.get(itemIndex);
      if (itemstack.m_41619_()) {
         return ItemStack.f_41583_;
      } else {
         this.items.set(itemIndex, ItemStack.f_41583_);
         return itemstack;
      }
   }

   public void m_6836_(int index, ItemStack itemStack) {
      LOGGER.debug("Setting item {} in container {} to stack {} with tag {}", index, this, itemStack, itemStack.m_41783_());
      this.items.set(index, itemStack);
      if (!itemStack.m_41619_() && itemStack.m_41613_() > this.m_6893_()) {
         itemStack.m_41764_(this.m_6893_());
      }

      this.m_6596_();
   }

   public int m_6643_() {
      return this.size;
   }

   public boolean m_7983_() {
      Iterator var1 = this.items.iterator();

      ItemStack itemstack;
      do {
         if (!var1.hasNext()) {
            return true;
         }

         itemstack = (ItemStack)var1.next();
      } while(itemstack.m_41619_());

      return false;
   }

   public void m_6596_() {
      if (this.listeners != null) {
         Iterator var1 = this.listeners.iterator();

         while(var1.hasNext()) {
            ContainerListener containerlistener = (ContainerListener)var1.next();
            containerlistener.m_5757_(this);
         }
      }

   }

   public boolean m_6542_(Player p_19167_) {
      return true;
   }

   public void m_6211_() {
      this.items.clear();
      this.m_6596_();
   }

   public void m_5809_(StackedContents p_19169_) {
      Iterator var2 = this.items.iterator();

      while(var2.hasNext()) {
         ItemStack itemstack = (ItemStack)var2.next();
         p_19169_.m_36491_(itemstack);
      }

   }

   public String toString() {
      return String.format("{Container #%d id: %d, items: %s }", this.containerIndex, System.identityHashCode(this), this.items);
   }

   private void moveItemToEmptySlots(ItemStack p_19190_) {
      for(int i = 0; i < this.size; ++i) {
         ItemStack itemstack = this.m_8020_(i);
         if (itemstack.m_41619_()) {
            this.m_6836_(i, p_19190_.m_278832_());
            return;
         }
      }

   }

   private void moveItemToOccupiedSlotsWithSameType(ItemStack p_19192_) {
      for(int i = 0; i < this.size; ++i) {
         ItemStack itemstack = this.m_8020_(i);
         if (ItemStack.m_150942_(itemstack, p_19192_)) {
            this.moveItemsBetweenStacks(p_19192_, itemstack);
            if (p_19192_.m_41619_()) {
               return;
            }
         }
      }

   }

   private void moveItemsBetweenStacks(ItemStack fromStack, ItemStack toStack) {
      int i = Math.min(this.m_6893_(), toStack.m_41741_());
      int j = Math.min(fromStack.m_41613_(), i - toStack.m_41613_());
      if (j > 0) {
         toStack.m_41769_(j);
         fromStack.m_41774_(j);
         this.m_6596_();
      }

   }

   public void fromTag(ListTag tag) {
      this.m_6211_();

      for(int i = 0; i < tag.size(); ++i) {
         ItemStack itemstack = ItemStack.m_41712_(tag.m_128728_(i));
         if (!itemstack.m_41619_()) {
            this.addItem(itemstack);
         }
      }

   }

   public ListTag createTag() {
      ListTag listtag = new ListTag();

      for(int i = 0; i < this.m_6643_(); ++i) {
         ItemStack itemstack = this.m_8020_(i);
         if (!itemstack.m_41619_()) {
            listtag.add(itemstack.m_41739_(new CompoundTag()));
         }
      }

      return listtag;
   }

   AttachmentContainerMenu getMenu() {
      return this.menu;
   }

   public static int getContainerStartIndex(SimpleAttachmentContainer[] attachmentContainers, int containerIndex) {
      int startIndex = 0;

      for(int i = 0; i < containerIndex; ++i) {
         SimpleAttachmentContainer container = attachmentContainers[i];
         startIndex += container.m_6643_();
      }

      return startIndex;
   }
}
