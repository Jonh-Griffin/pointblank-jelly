package com.vicmatskiv.pointblank.network;

import com.vicmatskiv.pointblank.item.GunItem;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent.Context;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AimingChangeRequestPacket extends GunStateRequestPacket {
   private static final Logger LOGGER = LogManager.getLogger("pointblank");
   private boolean isAimingEnabled;

   public AimingChangeRequestPacket() {
   }

   public AimingChangeRequestPacket(UUID stateId, int slotIndex, boolean isAimingEnabled) {
      super(stateId, slotIndex);
      this.isAimingEnabled = isAimingEnabled;
   }

   public static AimingChangeRequestPacket decode(FriendlyByteBuf buffer) {
      GunStateRequestPacket header = GunStateRequestPacket.decodeHeader(buffer);
      return new AimingChangeRequestPacket(header.stateId, header.slotIndex, buffer.readBoolean());
   }

   protected void doEncode(FriendlyByteBuf buffer) {
      buffer.writeBoolean(this.isAimingEnabled);
   }

   protected <T extends GunStateRequestPacket> void handleEnqueued(Supplier<Context> ctx) {
      ServerPlayer player = ((Context)ctx.get()).getSender();
      if (player != null) {
         ItemStack itemStack = player.m_150109_().m_8020_(this.slotIndex);
         if (itemStack != null) {
            Item var5 = itemStack.m_41720_();
            if (var5 instanceof GunItem) {
               GunItem gunItem = (GunItem)var5;
               gunItem.handleAimingChangeRequest(player, itemStack, this.stateId, this.slotIndex, this.isAimingEnabled);
               return;
            }
         }

         LOGGER.warn("Mismatching item in slot {}", this.slotIndex);
      }

   }
}
