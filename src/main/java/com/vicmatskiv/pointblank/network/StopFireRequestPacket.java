package com.vicmatskiv.pointblank.network;

import com.vicmatskiv.pointblank.item.GunItem;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent.Context;

public class StopFireRequestPacket extends GunStateRequestPacket {
   public StopFireRequestPacket() {
   }

   public StopFireRequestPacket(UUID stateId, int slotIndex) {
      super(stateId, slotIndex);
   }

   public static StopFireRequestPacket decode(FriendlyByteBuf buffer) {
      GunStateRequestPacket header = GunStateRequestPacket.decodeHeader(buffer);
      return new StopFireRequestPacket(header.stateId, header.slotIndex);
   }

   protected <T extends GunStateRequestPacket> void handleEnqueued(Supplier<Context> ctx) {
      ServerPlayer player = ((Context)ctx.get()).getSender();
      if (player != null) {
         ItemStack itemStack = player.m_150109_().m_8020_(this.slotIndex);
         if (itemStack != null && itemStack.m_41720_() instanceof GunItem) {
            ((GunItem)itemStack.m_41720_()).handleClientStopFireRequest(player, this.stateId, this.slotIndex, this.correlationId);
         } else {
            System.err.println("Mismatching item in slot " + this.slotIndex);
         }
      }

   }
}
