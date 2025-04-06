package com.vicmatskiv.pointblank.network;

import com.vicmatskiv.pointblank.client.ClientEventHandler;
import com.vicmatskiv.pointblank.client.GunClientState;
import com.vicmatskiv.pointblank.util.ClientUtil;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent.Context;
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
      buffer.writeInt(packet.owner.m_19879_());
      buffer.writeInt(packet.simplifiedState.ordinal());
   }

   public static MainHeldSimplifiedStateBroadcastPacket decode(FriendlyByteBuf buffer) {
      Entity effectOwnerEntity = ClientUtils.getLevel().m_6815_(buffer.readInt());
      GunClientState.FireState fireState = GunClientState.FireState.values()[buffer.readInt()];
      return new MainHeldSimplifiedStateBroadcastPacket((Player)effectOwnerEntity, (UUID)null, fireState);
   }

   public static <T extends MainHeldSimplifiedStateBroadcastPacket> void handle(T packet, Supplier<Context> ctx) {
      ((Context)ctx.get()).enqueueWork(() -> {
         ClientEventHandler.runSyncTick(() -> {
            if (packet.owner != ClientUtil.getClientPlayer()) {
               GunClientState otherPlayerClientState = GunClientState.getMainHeldState(packet.owner);
               if (otherPlayerClientState != null) {
                  otherPlayerClientState.setSimplifiedState(packet.simplifiedState);
               }

            }
         });
      });
      ((Context)ctx.get()).setPacketHandled(true);
   }
}
