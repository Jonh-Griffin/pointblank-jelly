package com.vicmatskiv.pointblank.network;

import com.vicmatskiv.pointblank.registry.ItemRegistry;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

public class ClientBoundPlayerDataSyncPacket {
   private CompoundTag playerData;
   private List<Integer> enabledItems;

   public ClientBoundPlayerDataSyncPacket(CompoundTag playerData, List<Integer> enabledItems) {
      this.playerData = playerData;
      this.enabledItems = enabledItems;
   }

   public ClientBoundPlayerDataSyncPacket() {
   }

   public static ClientBoundPlayerDataSyncPacket decode(FriendlyByteBuf buf) {
      ClientBoundPlayerDataSyncPacket packet = new ClientBoundPlayerDataSyncPacket();
      packet.playerData = buf.m_130260_();
      int itemCount = buf.readInt();
      packet.enabledItems = new ArrayList();

      for(int i = 0; i < itemCount; ++i) {
         packet.enabledItems.add(buf.readInt());
      }

      return packet;
   }

   public static void encode(ClientBoundPlayerDataSyncPacket packet, FriendlyByteBuf buf) {
      buf.m_130079_(packet.playerData);
      buf.writeInt(packet.enabledItems.size());
      Iterator var2 = packet.enabledItems.iterator();

      while(var2.hasNext()) {
         int itemId = (Integer)var2.next();
         buf.writeInt(itemId);
      }

   }

   public static void handle(ClientBoundPlayerDataSyncPacket packet, Supplier<Context> ctx) {
      ((Context)ctx.get()).enqueueWork(() -> {
         Minecraft mc = Minecraft.m_91087_();
         CompoundTag serverSlotMapping = packet.playerData.m_128469_("pointblank:attachmentSlotMapping");
         if (serverSlotMapping != null) {
            mc.f_91074_.getPersistentData().m_128365_("pointblank:attachmentSlotMapping", serverSlotMapping);
         } else {
            mc.f_91074_.getPersistentData().m_128473_((String)null);
         }

         ItemRegistry.ITEMS.syncEnabledItems(packet.enabledItems);
      });
      ((Context)ctx.get()).setPacketHandled(true);
   }

   public CompoundTag getPlayerData() {
      return this.playerData;
   }
}
