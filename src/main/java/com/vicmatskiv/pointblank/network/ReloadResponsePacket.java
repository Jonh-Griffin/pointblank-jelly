package com.vicmatskiv.pointblank.network;

import com.vicmatskiv.pointblank.client.GunClientState;
import com.vicmatskiv.pointblank.item.FireModeInstance;
import com.vicmatskiv.pointblank.item.GunItem;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent.Context;

public class ReloadResponsePacket extends GunStateResponsePacket {
   private int ammo;
   private FireModeInstance fireModeInstance;

   public ReloadResponsePacket() {
   }

   public ReloadResponsePacket(UUID stateId, int slotIndex, int correlationId, boolean isSuccess, int ammo, FireModeInstance fireModeInstance) {
      super(stateId, slotIndex, correlationId, isSuccess);
      this.ammo = ammo;
      this.fireModeInstance = fireModeInstance;
   }

   protected void doEncode(FriendlyByteBuf buffer) {
      buffer.writeInt(this.ammo);
      this.fireModeInstance.writeToBuf(buffer);
   }

   public static ReloadResponsePacket decode(FriendlyByteBuf buffer) {
      GunStateResponsePacket header = GunStateResponsePacket.decodeHeader(buffer);
      int ammo = buffer.readInt();
      FireModeInstance fireMode = FireModeInstance.readFromBuf(buffer);
      return new ReloadResponsePacket(header.stateId, header.slotIndex, header.correlationId, header.isSuccess, ammo, fireMode);
   }

   protected <T extends GunStateResponsePacket> void handleEnqueued(Supplier<Context> ctx, ItemStack itemStack, GunClientState gunClientState) {
      ((GunItem)itemStack.m_41720_()).processServerReloadResponse(this.correlationId, this.isSuccess, itemStack, gunClientState, this.ammo, this.fireModeInstance);
   }
}
