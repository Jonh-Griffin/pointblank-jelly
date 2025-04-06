package com.vicmatskiv.pointblank.network;

import com.vicmatskiv.pointblank.item.FireModeInstance;
import com.vicmatskiv.pointblank.item.GunItem;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent.Context;

public class FireModeRequestPacket extends GunStateRequestPacket {
   private FireModeInstance fireModeInstance;

   public FireModeRequestPacket() {
   }

   public FireModeRequestPacket(UUID stateId, int slotIndex, FireModeInstance fireModeInstance) {
      super(stateId, slotIndex);
      this.fireModeInstance = fireModeInstance;
   }

   public static FireModeRequestPacket decode(FriendlyByteBuf buffer) {
      GunStateRequestPacket header = GunStateRequestPacket.decodeHeader(buffer);
      FireModeInstance fireModeInstance = FireModeInstance.readFromBuf(buffer);
      return new FireModeRequestPacket(header.stateId, header.slotIndex, fireModeInstance);
   }

   protected void doEncode(FriendlyByteBuf buffer) {
      this.fireModeInstance.writeToBuf(buffer);
   }

   protected <T extends GunStateRequestPacket> void handleEnqueued(Supplier<Context> ctx) {
      ServerPlayer player = ((Context)ctx.get()).getSender();
      if (player != null) {
         ItemStack itemStack = player.m_150109_().m_8020_(this.slotIndex);
         if (itemStack != null && itemStack.m_41720_() instanceof GunItem) {
            ((GunItem)itemStack.m_41720_()).handleClientFireModeRequest(player, this.stateId, this.slotIndex, this.correlationId, this.fireModeInstance);
         } else {
            System.err.println("Mismatching item in slot " + this.slotIndex);
         }
      }

   }
}
