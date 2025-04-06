package com.vicmatskiv.pointblank.attachment;

import com.mojang.datafixers.util.Pair;
import com.vicmatskiv.pointblank.feature.AimingFeature;
import com.vicmatskiv.pointblank.feature.Feature;
import com.vicmatskiv.pointblank.feature.FeatureProvider;
import com.vicmatskiv.pointblank.network.Network;
import com.vicmatskiv.pointblank.network.ServerBoundNextAttachmentPacket;
import com.vicmatskiv.pointblank.network.ServerBoundOpenScreenPacket;
import com.vicmatskiv.pointblank.util.LRUCache;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Attachments {
   private static final Logger LOGGER = LogManager.getLogger("pointblank");
   private static final LRUCache<CompoundTag, NavigableMap<AttachmentCategory, String>> selectedAttachmentsCache = new LRUCache(100);
   private static final LRUCache<CompoundTag, NavigableMap<String, ItemStack>> tagAttachmentsCache = new LRUCache(100);
   private static final LRUCache<CompoundTag, NavigableMap<String, List<ItemStack>>> tagAttachmentGroupsCache = new LRUCache(100);
   private static final LRUCache<CompoundTag, NavigableMap<String, ItemStack>> recursiveAttachmentsCache = new LRUCache(100);
   public static final String ROOT_PATH = "/";

   public static void ensureValidAttachmentsSelected(ItemStack itemStack) {
      ensureValidAttachmentSelected(itemStack, AttachmentCategory.SCOPE, AimingFeature.class);
   }

   private static void ensureValidAttachmentSelected(ItemStack itemStack, AttachmentCategory category, Class<? extends Feature> featureType) {
      boolean isValid = false;
      Pair<String, ItemStack> selected = getSelectedAttachment(itemStack, category);
      if (selected != null) {
         Item var6 = ((ItemStack)selected.getSecond()).m_41720_();
         if (var6 instanceof FeatureProvider) {
            FeatureProvider fp = (FeatureProvider)var6;
            Feature feature = fp.getFeature(featureType);
            if (feature != null) {
               isValid = feature.isEnabled((ItemStack)selected.getSecond());
            }
         }
      }

      if (!isValid) {
         selectNextAttachment(itemStack, category, featureType);
      }

   }

   public static Collection<ItemStack> getAttachments(ItemStack itemStack) {
      return getAttachments(itemStack, false).values();
   }

   public static Pair<String, ItemStack> getSelectedAttachment(ItemStack rootItemStack, AttachmentCategory category) {
      NavigableMap<AttachmentCategory, String> selectedAttachments = getSelectedAttachments(rootItemStack);
      String selectedAttachmentPath = (String)selectedAttachments.get(category);
      if (selectedAttachmentPath == null) {
         return null;
      } else if ("/".equals(selectedAttachmentPath)) {
         return Pair.of(selectedAttachmentPath, rootItemStack);
      } else {
         NavigableMap<String, ItemStack> allAttachments = getAttachments(rootItemStack, true);
         ItemStack attachmentStack = (ItemStack)allAttachments.get(selectedAttachmentPath);
         return attachmentStack == null ? Pair.of(selectedAttachmentPath, ItemStack.f_41583_) : Pair.of(selectedAttachmentPath, attachmentStack);
      }
   }

   public static NavigableMap<AttachmentCategory, String> getSelectedAttachments(ItemStack itemStack) {
      return itemStack != null && itemStack.m_41720_() instanceof AttachmentHost && itemStack.m_41783_() != null ? (NavigableMap)selectedAttachmentsCache.computeIfAbsent(itemStack.m_41783_(), (tag) -> {
         return getSelectedAttachments(tag);
      }) : Collections.emptyNavigableMap();
   }

   private static NavigableMap<AttachmentCategory, String> getSelectedAttachments(CompoundTag tag) {
      if (tag != null && tag.m_128441_("sa")) {
         CompoundTag attachmentsTag = tag.m_128469_("sa");
         NavigableMap<AttachmentCategory, String> attachments = new TreeMap();
         Iterator var3 = AttachmentCategory.values().iterator();

         while(var3.hasNext()) {
            AttachmentCategory category = (AttachmentCategory)var3.next();
            if (attachmentsTag.m_128441_(category.getName())) {
               attachments.put(category, attachmentsTag.m_128461_(category.getName()));
            }
         }

         return attachments;
      } else {
         return Collections.emptyNavigableMap();
      }
   }

   private static void saveSelectedAttachments(ItemStack rootItemStack, Map<AttachmentCategory, String> attachments) {
      CompoundTag nbt = rootItemStack.m_41784_();
      CompoundTag attachmentsTag = new CompoundTag();
      Iterator var4 = attachments.entrySet().iterator();

      while(var4.hasNext()) {
         Entry<AttachmentCategory, String> entry = (Entry)var4.next();
         attachmentsTag.m_128359_(((AttachmentCategory)entry.getKey()).getName(), (String)entry.getValue());
      }

      nbt.m_128365_("sa", attachmentsTag);
   }

   public static Pair<String, ItemStack> selectNextAttachment(ItemStack rootItemStack, AttachmentCategory category, Class<? extends Feature> featureType) {
      Item var4 = rootItemStack.m_41720_();
      if (var4 instanceof AttachmentHost) {
         AttachmentHost attachmentHost = (AttachmentHost)var4;
         if (attachmentHost.getCompatibleAttachments().isEmpty()) {
            return null;
         } else {
            NavigableMap<String, ItemStack> attachmentsForCategory = getAttachmentsForCategory(rootItemStack, category);
            NavigableMap<AttachmentCategory, String> selectedAttachments = getSelectedAttachments(rootItemStack);
            String activeAttachmentPath = (String)selectedAttachments.get(category);
            Entry<String, ItemStack> next = null;
            Iterator var8;
            Entry e;
            FeatureProvider fp;
            Item var11;
            Feature feature;
            if (activeAttachmentPath != null) {
               var8 = attachmentsForCategory.tailMap(activeAttachmentPath, false).entrySet().iterator();

               while(var8.hasNext()) {
                  e = (Entry)var8.next();
                  var11 = ((ItemStack)e.getValue()).m_41720_();
                  if (var11 instanceof FeatureProvider) {
                     fp = (FeatureProvider)var11;
                     feature = fp.getFeature(featureType);
                     if (feature != null && feature.isEnabled((ItemStack)e.getValue())) {
                        next = e;
                        break;
                     }
                  }
               }
            }

            if (next == null) {
               Item var15 = rootItemStack.m_41720_();
               if (var15 instanceof FeatureProvider) {
                  FeatureProvider fp = (FeatureProvider)var15;
                  Feature feature = fp.getFeature(featureType);
                  if (feature != null && feature.isEnabled(rootItemStack)) {
                     next = new SimpleImmutableEntry("/", rootItemStack);
                  }
               }

               if (next == null) {
                  var8 = attachmentsForCategory.entrySet().iterator();

                  while(var8.hasNext()) {
                     e = (Entry)var8.next();
                     var11 = ((ItemStack)e.getValue()).m_41720_();
                     if (var11 instanceof FeatureProvider) {
                        fp = (FeatureProvider)var11;
                        feature = fp.getFeature(featureType);
                        if (feature != null && feature.isEnabled((ItemStack)e.getValue())) {
                           next = e;
                           break;
                        }
                     }
                  }
               }
            }

            NavigableMap<AttachmentCategory, String> selectedAttachments = new TreeMap(selectedAttachments);
            if (next != null) {
               selectedAttachments.put(category, (String)((Entry)next).getKey());
            } else {
               selectedAttachments.remove(category);
            }

            saveSelectedAttachments(rootItemStack, selectedAttachments);
            return next != null ? Pair.of((String)((Entry)next).getKey(), (ItemStack)((Entry)next).getValue()) : null;
         }
      } else {
         return null;
      }
   }

   public static NavigableMap<String, ItemStack> getAttachmentsForCategory(ItemStack itemStack, AttachmentCategory category) {
      NavigableMap<String, ItemStack> attachments = getAttachments(itemStack, true);
      NavigableMap<String, ItemStack> attachmentsForCategory = new TreeMap();
      Iterator var4 = attachments.entrySet().iterator();

      while(var4.hasNext()) {
         Entry<String, ItemStack> e = (Entry)var4.next();
         ItemStack attachmentStack = (ItemStack)e.getValue();
         Item var8 = attachmentStack.m_41720_();
         if (var8 instanceof Attachment) {
            Attachment attachment = (Attachment)var8;
            if (Objects.equals(category, attachment.getCategory())) {
               attachmentsForCategory.put((String)e.getKey(), (ItemStack)e.getValue());
            }
         }
      }

      return Collections.unmodifiableNavigableMap(attachmentsForCategory);
   }

   public static NavigableMap<String, List<ItemStack>> getAttachmentGroups(ItemStack itemStack) {
      return itemStack != null && itemStack.m_41720_() instanceof AttachmentHost && itemStack.m_41783_() != null ? (NavigableMap)tagAttachmentGroupsCache.computeIfAbsent(itemStack.m_41783_(), (tag) -> {
         return getAttachmentGroups(tag);
      }) : Collections.emptyNavigableMap();
   }

   private static NavigableMap<String, List<ItemStack>> getAttachmentGroups(CompoundTag tag) {
      NavigableMap<String, ItemStack> attachments = getAttachments(tag, "/", false, new TreeMap());
      NavigableMap<String, List<ItemStack>> attachmentGroups = new TreeMap();
      Iterator var3 = attachments.entrySet().iterator();

      while(var3.hasNext()) {
         Entry<String, ItemStack> e = (Entry)var3.next();
         Iterator var5 = ((Attachment)((ItemStack)e.getValue()).m_41720_()).getGroups().iterator();

         while(var5.hasNext()) {
            String group = (String)var5.next();
            List<ItemStack> attachmentList = (List)attachmentGroups.computeIfAbsent(group, (g) -> {
               return new ArrayList();
            });
            attachmentList.add((ItemStack)e.getValue());
         }
      }

      return attachmentGroups;
   }

   public static NavigableMap<String, ItemStack> getAttachments(ItemStack itemStack, boolean recursive) {
      if (itemStack != null && itemStack.m_41720_() instanceof AttachmentHost && itemStack.m_41783_() != null) {
         return recursive ? (NavigableMap)recursiveAttachmentsCache.computeIfAbsent(itemStack.m_41783_(), (tag) -> {
            return getAttachments(tag, "/", true, new TreeMap());
         }) : (NavigableMap)tagAttachmentsCache.computeIfAbsent(itemStack.m_41783_(), (tag) -> {
            return getAttachments(tag, "/", false, new TreeMap());
         });
      } else {
         return Collections.emptyNavigableMap();
      }
   }

   static NavigableMap<String, ItemStack> getAttachments(CompoundTag itemTag, String parentPath, boolean recursive, NavigableMap<String, ItemStack> attachmentStacks) {
      if (itemTag != null && itemTag.m_128425_("as", 9)) {
         ListTag attachmentsList = itemTag.m_128437_("as", 10);

         for(int i = 0; i < attachmentsList.size(); ++i) {
            CompoundTag attachmentTag = attachmentsList.m_128728_(i);
            String attachmentItemId = attachmentTag.m_128461_("id");
            Item item = (Item)ForgeRegistries.ITEMS.getValue(new ResourceLocation(attachmentItemId));
            if (item instanceof Attachment) {
               Attachment attachment = (Attachment)item;
               ItemStack attachmentStack = new ItemStack(item);
               attachmentStack.m_41751_(attachmentTag);
               String path = parentPath + "/" + attachment.getName();
               attachmentStacks.put(path, attachmentStack);
               if (recursive && item instanceof AttachmentHost) {
                  getAttachments(attachmentTag, path, recursive, attachmentStacks);
               }
            }
         }
      }

      return attachmentStacks;
   }

   public static CompoundTag addAttachment(ItemStack containerStack, ItemStack attachmentStack, boolean isRemoveable) {
      String attachmentId = ForgeRegistries.ITEMS.getKey(attachmentStack.m_41720_()).toString();
      CompoundTag containerTag = containerStack.m_41784_();
      ListTag attachmentsList = containerTag.m_128437_("as", 10);
      CompoundTag attachmentTag = attachmentStack.m_41782_() ? attachmentStack.m_41783_().m_6426_() : new CompoundTag();
      initAttachmentTag(attachmentTag, attachmentId, isRemoveable);
      attachmentsList.add(attachmentTag);
      containerTag.m_128365_("as", attachmentsList);
      return attachmentTag;
   }

   public static List<Attachment> removeAllAttachments(ItemStack containerStack) {
      CompoundTag containerTag = containerStack.m_41784_();
      ListTag attachmentsList1 = containerTag.m_128437_("as", 10);
      ListTag removedList = attachmentsList1.m_6426_();
      attachmentsList1.clear();
      List<Attachment> removedItems = new ArrayList();

      for(int i = 0; i < removedList.size(); ++i) {
         CompoundTag attachmentTag = removedList.m_128728_(i);
         String attachmentItemId = attachmentTag.m_128461_("id");
         Item item = (Item)ForgeRegistries.ITEMS.getValue(new ResourceLocation(attachmentItemId));
         if (item instanceof Attachment) {
            Attachment attachment = (Attachment)item;
            removedItems.add(attachment);
         }
      }

      return removedItems;
   }

   public static void initAttachmentTag(CompoundTag attachmentTag, String attachmentId, boolean isRemoveable) {
      attachmentTag.m_128359_("id", attachmentId);
      attachmentTag.m_128379_("rmv", isRemoveable);
   }

   public static boolean isRemoveable(CompoundTag containerTag) {
      return true;
   }

   public static void tryAttachmentMode(Player player, ItemStack heldItem) {
      Network.networkChannel.sendToServer(new ServerBoundOpenScreenPacket(ServerBoundOpenScreenPacket.ScreenType.ATTACHMENTS));
   }

   public static void tryNextAttachment(Player player, ItemStack heldItem, AttachmentCategory category, Class<? extends Feature> feature) {
      Network.networkChannel.sendToServer(new ServerBoundNextAttachmentPacket(category, feature));
   }
}
