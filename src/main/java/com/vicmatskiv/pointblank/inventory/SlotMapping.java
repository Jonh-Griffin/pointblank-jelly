package com.vicmatskiv.pointblank.inventory;

import com.vicmatskiv.pointblank.attachment.AttachmentCategory;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.player.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SlotMapping {
   private static final int CURRENT_VERSION = 1;
   private static final long EXPIRATION_DURATION = 604800000L;
   public static final String TAG_SLOT_MAPPING = "pointblank:attachmentSlotMapping";
   private static final Logger logger = LogManager.getLogger("pointblank");
   private Map<String, Map<Integer, AttachmentCategory>> mapping = new HashMap();
   private Player player;

   static SlotMapping getOrCreate(Player player) {
      SlotMapping slotMapping = new SlotMapping(player);
      slotMapping.loadSlotMapping();
      return slotMapping;
   }

   SlotMapping(Player player) {
      this.player = player;
   }

   Map<Integer, AttachmentCategory> getOrCreateSlotMapping(VirtualInventory virtualInventory) {
      String key = virtualInventory.getPath();
      Map<Integer, AttachmentCategory> result = null;
      if (key == null) {
         return null;
      } else {
         result = (Map)this.mapping.get(key);
         if (result != null && !virtualInventory.isValidSlotMapping(result)) {
            logger.debug("Invalid slot mapping found for key {} in inventory {}. Here it is: {}", key, virtualInventory, result);
            result = null;
         }

         if (result == null) {
            logger.debug("Creating a slot mapping for key {} in inventory {}", key, virtualInventory);
            result = virtualInventory.createSlotMapping(key);
            if (key != null) {
               this.mapping.put(key, result);
            }

            this.saveSlotMapping();
            logger.debug("Created a slot mapping for key {} in inventory {}. Here it is: {}", key, virtualInventory, result);
         }

         return result;
      }
   }

   Map<Integer, AttachmentCategory> getStackSlotMapping(VirtualInventory virtualInventory) {
      return (Map)this.mapping.get(virtualInventory.getPath());
   }

   void saveSlotMapping() {
      CompoundTag persistentData = this.player.getPersistentData();
      CompoundTag slotMappingTag = this.serializeSlotMapping();
      persistentData.m_128365_("pointblank:attachmentSlotMapping", slotMappingTag);
   }

   private void loadSlotMapping() {
      CompoundTag persistentData = this.player.getPersistentData();
      CompoundTag slotMappingTag = persistentData.m_128469_("pointblank:attachmentSlotMapping");
      if (slotMappingTag != null) {
         this.deserializeStackIdSlotMapping(slotMappingTag);
      }

   }

   CompoundTag serializeSlotMapping() {
      CompoundTag rootTag = new CompoundTag();
      long currentTime = System.currentTimeMillis();
      rootTag.m_128405_("Version", 1);
      rootTag.m_128356_("Timestamp", currentTime);
      ListTag mappingsList = new ListTag();
      Iterator var5 = this.mapping.entrySet().iterator();

      while(var5.hasNext()) {
         Entry<String, Map<Integer, AttachmentCategory>> entry = (Entry)var5.next();
         CompoundTag mappingTag = new CompoundTag();
         mappingTag.m_128359_("id", (String)entry.getKey());
         mappingTag.m_128356_("CreationTime", currentTime);
         CompoundTag slotMappingsTag = new CompoundTag();
         Iterator var9 = ((Map)entry.getValue()).entrySet().iterator();

         while(var9.hasNext()) {
            Entry<Integer, AttachmentCategory> slotEntry = (Entry)var9.next();
            slotMappingsTag.m_128359_(((Integer)slotEntry.getKey()).toString(), ((AttachmentCategory)slotEntry.getValue()).getName());
         }

         mappingTag.m_128365_("SlotMappings", slotMappingsTag);
         mappingsList.add(mappingTag);
      }

      rootTag.m_128365_("Mappings", mappingsList);
      return rootTag;
   }

   void deserializeStackIdSlotMapping(CompoundTag rootTag) {
      int storedVersion = rootTag.m_128451_("Version");
      long storedTimestamp = rootTag.m_128454_("Timestamp");
      long currentTime = System.currentTimeMillis();
      if (storedVersion == 1 && currentTime - storedTimestamp <= 604800000L) {
         this.mapping.clear();
         ListTag mappingsList = rootTag.m_128437_("Mappings", 10);

         for(int i = 0; i < mappingsList.size(); ++i) {
            CompoundTag mappingTag = mappingsList.m_128728_(i);
            long creationTime = mappingTag.m_128454_("CreationTime");
            if (currentTime - creationTime <= 604800000L) {
               String stackId = mappingTag.m_128461_("id");
               CompoundTag slotMappingsTag = mappingTag.m_128469_("SlotMappings");
               Map<Integer, AttachmentCategory> slotMappings = new HashMap();
               Iterator var15 = slotMappingsTag.m_128431_().iterator();

               while(var15.hasNext()) {
                  String slotKey = (String)var15.next();
                  String slotValue = slotMappingsTag.m_128461_(slotKey);
                  slotMappings.put(Integer.parseInt(slotKey), AttachmentCategory.fromString(slotValue));
               }

               this.mapping.put(stackId, slotMappings);
            }
         }

      } else {
         logger.warn("Skipping loading of outdated or expired slot mappings.");
      }
   }
}
