package com.vicmatskiv.pointblank.network;

import com.vicmatskiv.pointblank.client.ClientEventHandler;
import com.vicmatskiv.pointblank.client.GunClientState;
import com.vicmatskiv.pointblank.client.GunClientState.FireState;
import com.vicmatskiv.pointblank.util.ClientUtil;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;
import software.bernie.geckolib.util.ClientUtils;

public class MainHeldSimplifiedStateBroadcastPacket {
   protected Player owner;
   protected GunClientState.FireState simplifiedState;

   public MainHeldSimplifiedStateBroadcastPacket() {
   }

   public MainHeldSimplifiedStateBroadcastPacket(Player owner, UUID stateId, GunClientState.FireState simplifiedState) {
      this.owner = owner;
      this.simplifiedState = simplifiedState;
   }

   public static <T extends MainHeldSimplifiedStateBroadcastPacket> void encode(T packet, FriendlyByteBuf buffer) {
      buffer.writeInt(packet.owner.getId());
      buffer.writeInt(packet.simplifiedState.ordinal());
   }

   public static MainHeldSimplifiedStateBroadcastPacket decode(FriendlyByteBuf buffer) {
      Entity effectOwnerEntity = ClientUtils.getLevel().getEntity(buffer.readInt());
      GunClientState.FireState fireState = FireState.values()[buffer.readInt()];
      return new MainHeldSimplifiedStateBroadcastPacket((Player)effectOwnerEntity, null, fireState);
   }

   public static <T extends MainHeldSimplifiedStateBroadcastPacket> void handle(T packet, Supplier<NetworkEvent.Context> ctx) {
      ctx.get().enqueueWork(() -> ClientEventHandler.runSyncTick(() -> {
         if (packet.owner != ClientUtil.getClientPlayer()) {
            GunClientState otherPlayerClientState = GunClientState.getMainHeldState(packet.owner);
            if (otherPlayerClientState != null) {
               otherPlayerClientState.setSimplifiedState(packet.simplifiedState);
            }

         }
      }));
      ctx.get().setPacketHandled(true);
   }
}
