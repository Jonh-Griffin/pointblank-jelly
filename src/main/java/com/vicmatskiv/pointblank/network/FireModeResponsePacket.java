package com.vicmatskiv.pointblank.network;

import com.vicmatskiv.pointblank.client.GunClientState;
import com.vicmatskiv.pointblank.item.FireModeInstance;
import com.vicmatskiv.pointblank.item.GunItem;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent.Context;

public class FireModeResponsePacket extends GunStateResponsePacket {
   private FireModeInstance fireModeInstance;

   public FireModeResponsePacket() {
   }

   public FireModeResponsePacket(UUID stateId, int slotIndex, int correlationId, boolean isSuccess, FireModeInstance fireModeInstance) {
      super(stateId, slotIndex, correlationId, isSuccess);
      this.fireModeInstance = fireModeInstance;
   }

   protected void doEncode(FriendlyByteBuf buffer) {
      this.fireModeInstance.writeToBuf(buffer);
   }

   public static FireModeResponsePacket decode(FriendlyByteBuf buffer) {
      GunStateResponsePacket header = GunStateResponsePacket.decodeHeader(buffer);
      FireModeInstance fireModeInstance = FireModeInstance.readFromBuf(buffer);
      return new FireModeResponsePacket(header.stateId, header.slotIndex, header.correlationId, header.isSuccess, fireModeInstance);
   }

   protected <T extends GunStateResponsePacket> void handleEnqueued(Supplier<Context> ctx, ItemStack itemStack, GunClientState gunClientState) {
      ((GunItem)itemStack.m_41720_()).processServerFireModeResponse(this.stateId, this.correlationId, this.isSuccess, itemStack, gunClientState, this.fireModeInstance);
   }
}
