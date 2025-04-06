package com.vicmatskiv.pointblank.network;

import com.vicmatskiv.pointblank.item.ThrowableLike;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent.Context;

public class ThrowProjectileRequestPacket {
   protected UUID stateId;
   protected int slotIndex;

   public ThrowProjectileRequestPacket() {
   }

   public ThrowProjectileRequestPacket(UUID stateId, int slotIndex) {
      this.stateId = stateId;
      this.slotIndex = slotIndex;
   }

   public static <T extends ThrowProjectileRequestPacket> void encode(T packet, FriendlyByteBuf buffer) {
      buffer.writeLong(packet.stateId.getMostSignificantBits());
      buffer.writeLong(packet.stateId.getLeastSignificantBits());
      buffer.writeInt(packet.slotIndex);
   }

   public static void handle(ThrowProjectileRequestPacket packet, Supplier<Context> ctx) {
      ((Context)ctx.get()).enqueueWork(() -> {
         packet.handleEnqueued(ctx);
      });
      ((Context)ctx.get()).setPacketHandled(true);
   }

   public static ThrowProjectileRequestPacket decode(FriendlyByteBuf buffer) {
      UUID stateId = new UUID(buffer.readLong(), buffer.readLong());
      int slotIndex = buffer.readInt();
      return new ThrowProjectileRequestPacket(stateId, slotIndex);
   }

   protected <T extends ThrowProjectileRequestPacket> void handleEnqueued(Supplier<Context> ctx) {
      ServerPlayer player = ((Context)ctx.get()).getSender();
      if (player != null) {
         ItemStack itemStack = player.m_150109_().m_8020_(this.slotIndex);
         if (itemStack != null && itemStack.m_41720_() instanceof ThrowableLike) {
            ((ThrowableLike)itemStack.m_41720_()).handleClientThrowRequest(player, this.stateId, this.slotIndex);
         } else {
            System.err.println("Mismatching item in slot " + this.slotIndex);
         }
      }

   }
}
