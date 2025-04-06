package com.vicmatskiv.pointblank.network;

import com.vicmatskiv.pointblank.client.ClientEventHandler;
import com.vicmatskiv.pointblank.client.GunClientState;
import com.vicmatskiv.pointblank.util.InventoryUtils;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent.Context;
import software.bernie.geckolib.util.ClientUtils;

public class GunStateResponsePacket {
   protected UUID stateId;
   protected int slotIndex;
   protected int correlationId;
   protected boolean isSuccess;

   public GunStateResponsePacket() {
   }

   public GunStateResponsePacket(UUID stateId, int slotIndex, int correlationId, boolean isSuccess) {
      this.stateId = stateId;
      this.slotIndex = slotIndex;
      this.correlationId = correlationId;
      this.isSuccess = isSuccess;
   }

   public static <T extends GunStateResponsePacket> void encode(T packet, FriendlyByteBuf buffer) {
      buffer.writeLong(packet.stateId.getMostSignificantBits());
      buffer.writeLong(packet.stateId.getLeastSignificantBits());
      buffer.writeInt(packet.slotIndex);
      buffer.writeInt(packet.correlationId);
      buffer.writeBoolean(packet.isSuccess);
      packet.doEncode(buffer);
   }

   protected void doEncode(FriendlyByteBuf buffer) {
   }

   protected static GunStateResponsePacket decodeHeader(FriendlyByteBuf buffer) {
      UUID stateId = new UUID(buffer.readLong(), buffer.readLong());
      int slotIndex = buffer.readInt();
      int correlationId = buffer.readInt();
      boolean isSuccess = buffer.readBoolean();
      return new GunStateResponsePacket(stateId, slotIndex, correlationId, isSuccess);
   }

   public static <T extends GunStateResponsePacket> void handle(T packet, Supplier<Context> ctx) {
      ((Context)ctx.get()).enqueueWork(() -> {
         Player player = ClientUtils.getClientPlayer();
         ClientEventHandler.runSyncTick(() -> {
            Tuple<ItemStack, GunClientState> targetTuple = packet.getItemStackAndState(packet, player);
            if (targetTuple != null) {
               packet.handleEnqueued(ctx, (ItemStack)targetTuple.m_14418_(), (GunClientState)targetTuple.m_14419_());
            }

         });
      });
      ((Context)ctx.get()).setPacketHandled(true);
   }

   protected <T extends GunStateResponsePacket> Tuple<ItemStack, GunClientState> getItemStackAndState(T packet, Entity entity) {
      if (entity instanceof Player) {
         Player player = (Player)entity;
         return InventoryUtils.getItemStackByStateId(player, packet.stateId, packet.slotIndex);
      } else {
         return null;
      }
   }

   protected <T extends GunStateResponsePacket> void handleEnqueued(Supplier<Context> ctx, ItemStack itemStack, GunClientState gunClientState) {
   }
}
