package com.vicmatskiv.pointblank.network;

import com.vicmatskiv.pointblank.client.GunClientState;
import com.vicmatskiv.pointblank.util.MiscUtil;
import java.util.Iterator;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.NetworkEvent.Context;

public class MainHeldSimplifiedStateSyncRequest extends GunStateRequestPacket {
   private static final int MAX_STATE_SYNC_DISTANCE_SQR = 10000;
   private GunClientState.FireState simplifiedState;

   public MainHeldSimplifiedStateSyncRequest() {
   }

   public MainHeldSimplifiedStateSyncRequest(UUID stateId, GunClientState.FireState simplifiedState) {
      super(stateId, 0);
      this.simplifiedState = simplifiedState;
   }

   protected void doEncode(FriendlyByteBuf buffer) {
      buffer.writeInt(this.simplifiedState.ordinal());
   }

   public static MainHeldSimplifiedStateSyncRequest decode(FriendlyByteBuf buffer) {
      GunStateRequestPacket header = GunStateRequestPacket.decodeHeader(buffer);
      GunClientState.FireState state = GunClientState.FireState.values()[buffer.readInt()];
      return new MainHeldSimplifiedStateSyncRequest(header.stateId, state);
   }

   protected <T extends GunStateRequestPacket> void handleEnqueued(Supplier<Context> ctx) {
      ServerPlayer sender = ((Context)ctx.get()).getSender();
      Iterator var3 = ((ServerLevel)MiscUtil.getLevel(sender)).m_8795_((p) -> {
         return true;
      }).iterator();

      while(var3.hasNext()) {
         ServerPlayer player = (ServerPlayer)var3.next();
         if (player != sender && player.m_20238_(player.m_20182_()) < 10000.0D) {
            Network.networkChannel.send(PacketDistributor.PLAYER.with(() -> {
               return player;
            }), new MainHeldSimplifiedStateBroadcastPacket(sender, this.stateId, this.simplifiedState));
         }
      }

   }
}
