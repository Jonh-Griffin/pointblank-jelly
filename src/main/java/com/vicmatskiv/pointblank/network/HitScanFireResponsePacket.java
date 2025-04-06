package com.vicmatskiv.pointblank.network;

import com.vicmatskiv.pointblank.client.ClientEventHandler;
import com.vicmatskiv.pointblank.client.GunClientState;
import com.vicmatskiv.pointblank.item.GunItem;
import com.vicmatskiv.pointblank.util.InventoryUtils;
import com.vicmatskiv.pointblank.util.SimpleHitResult;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent.Context;
import software.bernie.geckolib.util.ClientUtils;

public class HitScanFireResponsePacket extends GunStateResponsePacket {
   private SimpleHitResult hitResult;
   private int ownerEntityId;
   private float damage;

   public HitScanFireResponsePacket() {
   }

   public HitScanFireResponsePacket(int ownerEntityId, UUID stateId, int slotIndex, int correlationId, SimpleHitResult hitResult, float damage) {
      super(stateId, slotIndex, correlationId, true);
      this.ownerEntityId = ownerEntityId;
      this.hitResult = hitResult;
      this.damage = damage;
   }

   protected void doEncode(FriendlyByteBuf buffer) {
      buffer.writeInt(this.ownerEntityId);
      SimpleHitResult.writer().accept(buffer, this.hitResult);
      buffer.writeFloat(this.damage);
   }

   public static HitScanFireResponsePacket decode(FriendlyByteBuf buffer) {
      GunStateResponsePacket header = GunStateResponsePacket.decodeHeader(buffer);
      int ownerEntityId = buffer.readInt();
      SimpleHitResult hitResult = (SimpleHitResult)SimpleHitResult.reader().apply(buffer);
      float damage = buffer.readFloat();
      return new HitScanFireResponsePacket(ownerEntityId, header.stateId, header.slotIndex, header.correlationId, hitResult, damage);
   }

   public static void handle(HitScanFireResponsePacket packet, Supplier<Context> ctx) {
      ((Context)ctx.get()).enqueueWork(() -> {
         ClientEventHandler.runSyncTick(() -> {
            Level level = ClientUtils.getLevel();
            Entity entity = level.m_6815_(packet.ownerEntityId);
            if (entity instanceof Player) {
               Player player = (Player)entity;
               Tuple<ItemStack, GunClientState> targetTuple = packet.getItemStackAndState(packet, player);
               if (targetTuple != null) {
                  packet.handleEnqueued(player, (ItemStack)targetTuple.m_14418_(), (GunClientState)targetTuple.m_14419_());
               }
            }

         });
      });
      ((Context)ctx.get()).setPacketHandled(true);
   }

   protected <T extends GunStateResponsePacket> Tuple<ItemStack, GunClientState> getItemStackAndState(T packet, Entity entity) {
      Player clientPlayer = ClientUtils.getClientPlayer();
      if (entity instanceof Player) {
         Player player = (Player)entity;
         return InventoryUtils.getItemStackByStateId(player, packet.stateId, clientPlayer == player ? packet.slotIndex : 0);
      } else {
         return null;
      }
   }

   protected <T extends GunStateResponsePacket> void handleEnqueued(Player player, ItemStack itemStack, GunClientState gunClientState) {
      Item var5 = itemStack.m_41720_();
      if (var5 instanceof GunItem) {
         GunItem gunItem = (GunItem)var5;
         gunItem.processServerHitScanFireResponse(player, this.stateId, itemStack, gunClientState, this.hitResult, this.damage);
      }

   }
}
