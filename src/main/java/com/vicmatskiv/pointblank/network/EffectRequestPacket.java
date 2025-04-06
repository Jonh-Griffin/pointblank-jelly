package com.vicmatskiv.pointblank.network;

import com.vicmatskiv.pointblank.util.MiscUtil;
import com.vicmatskiv.pointblank.util.SimpleHitResult;
import java.util.Iterator;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.NetworkEvent.Context;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EffectRequestPacket {
   private static final Logger LOGGER = LogManager.getLogger("pointblank");
   private static final int MAX_DISTANCE_SQR = 22500;
   protected int playerEntityId;
   protected UUID gunStateId;
   protected UUID effectId;
   protected Vec3 startPosition;
   protected SimpleHitResult hitResult;
   protected boolean hasMuzzlePositionProvider;

   public EffectRequestPacket() {
   }

   public EffectRequestPacket(int playerEntityId, UUID gunStateId, UUID effectId, Vec3 startPosition, SimpleHitResult hitResult, boolean hasMuzzlePositionProvider) {
      this.playerEntityId = playerEntityId;
      this.gunStateId = gunStateId;
      this.effectId = effectId;
      this.startPosition = startPosition;
      this.hitResult = hitResult;
      this.hasMuzzlePositionProvider = hasMuzzlePositionProvider;
   }

   public static <T extends EffectRequestPacket> void encode(T packet, FriendlyByteBuf buffer) {
      buffer.writeInt(packet.playerEntityId);
      buffer.writeLong(packet.gunStateId.getMostSignificantBits());
      buffer.writeLong(packet.gunStateId.getLeastSignificantBits());
      buffer.writeLong(packet.effectId.getMostSignificantBits());
      buffer.writeLong(packet.effectId.getLeastSignificantBits());
      buffer.m_236835_(Optional.ofNullable(packet.startPosition), MiscUtil.VEC3_WRITER);
      buffer.m_236835_(Optional.ofNullable(packet.hitResult), SimpleHitResult.writer());
      buffer.writeBoolean(packet.hasMuzzlePositionProvider);
      packet.doEncode(buffer);
   }

   protected void doEncode(FriendlyByteBuf buffer) {
   }

   protected static EffectRequestPacket decode(FriendlyByteBuf buffer) {
      int playerEntityId = buffer.readInt();
      UUID gunStateId = new UUID(buffer.readLong(), buffer.readLong());
      UUID effectId = new UUID(buffer.readLong(), buffer.readLong());
      Vec3 startPosition = (Vec3)buffer.m_236860_(MiscUtil.VEC3_READER).orElse((Object)null);
      SimpleHitResult hitResult = (SimpleHitResult)buffer.m_236860_(SimpleHitResult.reader()).orElse((Object)null);
      boolean hasMuzzlePositionProvider = buffer.readBoolean();
      return new EffectRequestPacket(playerEntityId, gunStateId, effectId, startPosition, hitResult, hasMuzzlePositionProvider);
   }

   public static <T extends EffectRequestPacket> void handle(T packet, Supplier<Context> ctx) {
      ((Context)ctx.get()).enqueueWork(() -> {
         packet.handleEnqueued(ctx);
      });
      ((Context)ctx.get()).setPacketHandled(true);
   }

   protected <T extends EffectRequestPacket> void handleEnqueued(Supplier<Context> ctx) {
      ServerPlayer sender = ((Context)ctx.get()).getSender();
      LOGGER.debug("Received effect request {} from {}", this, sender);
      Iterator var3 = ((ServerLevel)MiscUtil.getLevel(sender)).m_8795_((p) -> {
         return true;
      }).iterator();

      while(var3.hasNext()) {
         ServerPlayer player = (ServerPlayer)var3.next();
         if (player.m_20238_(this.startPosition) < 22500.0D) {
            Network.networkChannel.send(PacketDistributor.PLAYER.with(() -> {
               return player;
            }), new EffectBroadcastPacket(this.playerEntityId, this.gunStateId, this.effectId, this.startPosition, this.hitResult, this.hasMuzzlePositionProvider));
         }
      }

   }
}
