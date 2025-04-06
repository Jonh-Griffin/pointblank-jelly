package com.vicmatskiv.pointblank.inventory;

import com.vicmatskiv.pointblank.Nameable;
import com.vicmatskiv.pointblank.attachment.Attachment;
import com.vicmatskiv.pointblank.attachment.AttachmentCategory;
import com.vicmatskiv.pointblank.attachment.AttachmentHost;
import com.vicmatskiv.pointblank.attachment.Attachments;
import com.vicmatskiv.pointblank.event.AttachmentAddedEvent;
import com.vicmatskiv.pointblank.event.AttachmentRemovedEvent;
import com.vicmatskiv.pointblank.util.MiscUtil;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerListener;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class VirtualInventory implements ContainerListener {
   private static final Logger logger = LogManager.getLogger("pointblank");
   protected AttachmentCategory category;
   protected ItemStack itemStack;
   protected VirtualInventory parent;
   protected Map<AttachmentCategory, VirtualInventory> elements;
   protected Player owner;

   private VirtualInventory(Player owner, AttachmentCategory category, VirtualInventory parent, ItemStack itemStack) {
      this.owner = owner;
      this.category = category;
      this.parent = parent;
      this.itemStack = itemStack != null ? itemStack : ItemStack.f_41583_;
      this.elements = new LinkedHashMap();
   }

   public ItemStack getItemStack() {
      return this.itemStack;
   }

   private void addElement(VirtualInventory e) {
      AttachmentCategory category = e.category;
      if (category == null) {
         logger.error("Adding an element without a category");
      }

      this.elements.put(e.category, e);
   }

   public Map<AttachmentCategory, VirtualInventory> getElements() {
      return Collections.unmodifiableMap(this.elements);
   }

   public VirtualInventory getElement(AttachmentCategory category) {
      return (VirtualInventory)this.elements.get(category);
   }

   private Collection<AttachmentCategory> getCategories() {
      return this.elements.keySet();
   }

   public boolean mayPlace(ItemStack attachmentStack, AttachmentSlot attachmentSlot) {
      if (attachmentSlot.getSlotIndex() > this.elements.size()) {
         return false;
      } else if (!this.isCompatibleAttachment(attachmentStack)) {
         return false;
      } else {
         AttachmentCategory attachmentCategory = ((Attachment)attachmentStack.m_41720_()).getCategory();
         return !this.hasAttachmentInCategory(attachmentCategory);
      }
   }

   public boolean hasAttachments() {
      return this.elements.values().stream().anyMatch((e) -> {
         return e.itemStack != null && !e.itemStack.m_41619_();
      });
   }

   private boolean hasAttachmentInCategory(AttachmentCategory category) {
      VirtualInventory e = (VirtualInventory)this.elements.get(category);
      return e != null && e.itemStack != null && !e.itemStack.m_41619_();
   }

   private boolean isCompatibleAttachment(ItemStack attachmentStack) {
      Item var3 = attachmentStack.m_41720_();
      boolean var10000;
      if (var3 instanceof Attachment) {
         Attachment attachment = (Attachment)var3;
         if (((AttachmentHost)this.itemStack.m_41720_()).getCompatibleAttachments().contains(attachment)) {
            var10000 = true;
            return var10000;
         }
      }

      var10000 = false;
      return var10000;
   }

   public boolean mayPickup(Player player) {
      return false;
   }

   String getPath() {
      if (this.parent != null) {
         Item var2 = this.itemStack.m_41720_();
         String var10000;
         if (var2 instanceof Nameable) {
            Nameable nameable = (Nameable)var2;
            var10000 = this.parent.getPath();
            return var10000 + "/" + nameable.getName();
         } else {
            var10000 = this.parent.getPath();
            return var10000 + "/" + this.itemStack.m_41720_().toString();
         }
      } else {
         return MiscUtil.getItemStackId(this.itemStack).toString();
      }
   }

   private ItemStack getRootStack() {
      return this.parent != null ? this.parent.getRootStack() : this.itemStack;
   }

   public void m_5757_(Container c) {
      if (c instanceof SimpleAttachmentContainer) {
         SimpleAttachmentContainer container = (SimpleAttachmentContainer)c;
         AttachmentContainerMenu menu = container.getMenu();
         logger.debug("Virtual inventory {} handling changes in container {}, stack tags {}", this, container, this.itemStack.m_41783_());
         SlotMapping slotMapping = menu.getSlotMapping();
         Map stackSlotMapping = slotMapping.getStackSlotMapping(this);
         if (stackSlotMapping == null) {
            logger.warn("Slot mapping not found for container {}, stack tags {}", System.identityHashCode(container), this.itemStack.m_41783_());
         } else {
            List<Attachment> removedItems = Attachments.removeAllAttachments(this.itemStack);

            for(int i = 1; i < container.m_6643_(); ++i) {
               ItemStack slotStack = container.m_8020_(i);
               if (!slotStack.m_41619_()) {
                  Item var10 = slotStack.m_41720_();
                  if (var10 instanceof Attachment) {
                     Attachment a1 = (Attachment)var10;
                     logger.debug("Adding attachment '{}' from slot {} with tag {} to stack '{}'", slotStack, i, slotStack.m_41783_(), this.itemStack);
                     Attachments.addAttachment(this.itemStack, slotStack, true);
                     if (!removedItems.contains(a1)) {
                        AttachmentAddedEvent event = new AttachmentAddedEvent(this.itemStack, slotStack);
                        MinecraftForge.EVENT_BUS.post(event);
                        logger.debug("Added new attachment '{}' from slot {} with tag {} to stack '{}' with tag {} and path {}", slotStack, i, slotStack.m_41783_(), this.itemStack, this.itemStack.m_41783_());
                     } else {
                        logger.debug("Re-added existing attachment '{}' from slot {} with tag {} to stack '{}' with tag {}", slotStack, i, slotStack.m_41783_(), this.itemStack, this.itemStack.m_41783_());
                     }

                     stackSlotMapping.put(i, a1.getCategory());
                     VirtualInventory childInventory = createInventory(this.owner, this, slotStack);
                     if (childInventory != null) {
                        this.elements.put(a1.getCategory(), childInventory);
                     }
                     continue;
                  }
               }

               AttachmentCategory category = (AttachmentCategory)stackSlotMapping.remove(i);
               if (category != null) {
                  logger.debug("Removing attachment '{}' from slot {}, with tag {} from stack '{}'", slotStack, i, slotStack.m_41783_(), this.itemStack);
                  VirtualInventory e = (VirtualInventory)this.elements.get(category);
                  if (e != null) {
                     AttachmentRemovedEvent event = new AttachmentRemovedEvent(this.owner, this.getRootStack(), this.itemStack, slotStack);
                     MinecraftForge.EVENT_BUS.post(event);
                     e.itemStack = ItemStack.f_41583_;
                     e.elements.clear();
                  }
               }
            }

            slotMapping.saveSlotMapping();
            if (this.parent != null) {
               this.parent.onContentChange();
            }

            menu.updateAttachmentSlots();
            menu.m_38946_();
            logger.debug("Virtual inventory {} handled changes for container {}, stack tags {}", this, container, this.itemStack.m_41783_());
         }
      }
   }

   private void onContentChange() {
      logger.debug("Updating tag content for {}, tag: {}", this, this.itemStack.m_41783_());
      this.updateTag();
      logger.debug("Updating tag content for {}, tag: {}", this, this.itemStack.m_41783_());
      if (this.parent != null) {
         this.parent.updateTag();
      }

   }

   private CompoundTag updateTag() {
      if (this.itemStack == null) {
         return null;
      } else if (this.itemStack.m_41619_()) {
         logger.error("Virtual inventory {} attempted to update empty stack {}", this, this.itemStack);
         return null;
      } else {
         CompoundTag tag = this.itemStack.m_41784_();
         Item var3 = this.itemStack.m_41720_();
         if (var3 instanceof Attachment) {
            Attachment attachment = (Attachment)var3;
            String attachmentId = ForgeRegistries.ITEMS.getKey(attachment.m_5456_()).toString();
            tag.m_128359_("id", attachmentId);
            boolean isRemovable = tag.m_128425_("rmv", 99) ? tag.m_128471_("rmv") : true;
            tag.m_128379_("rmv", isRemovable);
         }

         ListTag nestedAttachments = tag.m_128437_("as", 10);
         nestedAttachments.clear();
         Iterator var8 = this.elements.entrySet().iterator();

         while(var8.hasNext()) {
            Entry<AttachmentCategory, VirtualInventory> e = (Entry)var8.next();
            CompoundTag nestedAttachmentTag = ((VirtualInventory)e.getValue()).updateTag();
            if (nestedAttachmentTag != null) {
               nestedAttachments.add(nestedAttachmentTag);
            }
         }

         tag.m_128365_("as", nestedAttachments);
         return tag;
      }
   }

   Map<Integer, AttachmentCategory> createSlotMapping(String stackId) {
      Map<Integer, AttachmentCategory> mapping = new HashMap();
      int i = 1;

      for(Iterator var4 = this.getCategories().iterator(); var4.hasNext(); ++i) {
         AttachmentCategory category = (AttachmentCategory)var4.next();
         VirtualInventory e = this.getElement(category);
         ItemStack itemStack = e.getItemStack();
         if (itemStack != null && !itemStack.m_41619_()) {
            mapping.put(i, category);
         }
      }

      return mapping;
   }

   boolean isValidSlotMapping(Map<Integer, AttachmentCategory> mapping) {
      return mapping.isEmpty() ? this.elements.isEmpty() : this.elements.keySet().containsAll(mapping.values());
   }

   public String toString() {
      return String.format("{vi: %d, path: %s, elements: %s}", System.identityHashCode(this), this.getPath(), this.elements);
   }

   private static VirtualInventory createInventory(Player owner, VirtualInventory parentInventory, ItemStack currentStack) {
      AttachmentCategory currentCategory = null;
      if (currentStack != null) {
         Item var5 = currentStack.m_41720_();
         if (var5 instanceof Attachment) {
            Attachment attachment = (Attachment)var5;
            currentCategory = attachment.getCategory();
         }
      }

      VirtualInventory currentInventory = new VirtualInventory(owner, currentCategory, parentInventory, currentStack);
      if (currentStack == null) {
         return currentInventory;
      } else {
         CompoundTag currentTag = currentStack.m_41784_();
         Item var7 = currentStack.m_41720_();
         if (!(var7 instanceof AttachmentHost)) {
            return currentInventory;
         } else {
            AttachmentHost attachmentHost = (AttachmentHost)var7;
            HashMap attachmentStacks = new HashMap();
            if (currentTag.m_128425_("as", 9)) {
               ListTag attachmentsList = currentTag.m_128437_("as", 10);

               for(int i = 0; i < attachmentsList.size(); ++i) {
                  CompoundTag attachmentTag = attachmentsList.m_128728_(i);
                  String itemId = attachmentTag.m_128461_("id");
                  Item item = (Item)ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId));
                  if (item instanceof Attachment) {
                     Attachment attachment = (Attachment)item;
                     ItemStack attachmentItemStack = new ItemStack(item);
                     attachmentItemStack.m_41751_(attachmentTag);
                     attachmentStacks.put(attachment.getCategory(), attachmentItemStack);
                  }
               }
            }

            Iterator var18 = attachmentHost.getCompatibleAttachmentCategories().iterator();

            while(var18.hasNext()) {
               AttachmentCategory category = (AttachmentCategory)var18.next();
               ItemStack attachmentStack = (ItemStack)attachmentStacks.get(category);
               VirtualInventory nestedInventory;
               if (attachmentStack == null) {
                  nestedInventory = new VirtualInventory(owner, category, currentInventory, (ItemStack)null);
               } else {
                  nestedInventory = createInventory(owner, currentInventory, attachmentStack);
               }

               if (nestedInventory != null) {
                  currentInventory.addElement(nestedInventory);
               }
            }

            logger.debug("Created {} with stack: {}, tag {}, elements: {}", currentInventory, currentStack, currentStack.m_41783_(), currentInventory.elements);
            return currentInventory;
         }
      }
   }

   public static VirtualInventory createInventory(Player owner, ItemStack mainStack) {
      return createInventory(owner, (VirtualInventory)null, mainStack);
   }
}
