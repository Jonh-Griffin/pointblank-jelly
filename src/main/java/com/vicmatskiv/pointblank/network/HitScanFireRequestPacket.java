package com.vicmatskiv.pointblank.network;

import com.vicmatskiv.pointblank.item.FireModeInstance;
import com.vicmatskiv.pointblank.item.GunItem;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent.Context;

public class HitScanFireRequestPacket extends GunStateRequestPacket {
   private FireModeInstance fireModeInstance;
   private boolean isAiming;
   private long seed;

   public HitScanFireRequestPacket() {
   }

   public HitScanFireRequestPacket(FireModeInstance fireModeInstance, UUID stateId, int slotIndex, boolean isAiming, long seed) {
      super(stateId, slotIndex);
      this.fireModeInstance = fireModeInstance;
      this.isAiming = isAiming;
      this.seed = seed;
   }

   public static HitScanFireRequestPacket decode(FriendlyByteBuf buffer) {
      GunStateRequestPacket header = GunStateRequestPacket.decodeHeader(buffer);
      FireModeInstance fireModeInstance = FireModeInstance.readFromBuf(buffer);
      boolean isAiming = buffer.readBoolean();
      long seed = buffer.readLong();
      return new HitScanFireRequestPacket(fireModeInstance, header.stateId, header.slotIndex, isAiming, seed);
   }

   protected void doEncode(FriendlyByteBuf buffer) {
      this.fireModeInstance.writeToBuf(buffer);
      buffer.writeBoolean(this.isAiming);
      buffer.writeLong(this.seed);
   }

   protected <T extends GunStateRequestPacket> void handleEnqueued(Supplier<Context> ctx) {
      ServerPlayer player = ((Context)ctx.get()).getSender();
      if (player != null) {
         ItemStack itemStack = player.m_150109_().m_8020_(this.slotIndex);
         if (itemStack != null && itemStack.m_41720_() instanceof GunItem) {
            ((GunItem)itemStack.m_41720_()).handleClientHitScanFireRequest(player, this.fireModeInstance, this.stateId, this.slotIndex, this.correlationId, this.isAiming, this.seed);
         } else {
            System.err.println("Mismatching item in slot " + this.slotIndex);
         }
      }

   }
}
