package com.vicmatskiv.pointblank.network;

import com.vicmatskiv.pointblank.Config;
import com.vicmatskiv.pointblank.client.ClientEventHandler;
import com.vicmatskiv.pointblank.client.GunClientState;
import com.vicmatskiv.pointblank.client.GunStatePoseProvider;
import com.vicmatskiv.pointblank.client.PoseProvider;
import com.vicmatskiv.pointblank.client.PositionProvider;
import com.vicmatskiv.pointblank.client.VertexConsumers;
import com.vicmatskiv.pointblank.client.effect.Effect;
import com.vicmatskiv.pointblank.client.effect.EffectBuilder;
import com.vicmatskiv.pointblank.registry.EffectRegistry;
import com.vicmatskiv.pointblank.util.SimpleHitResult;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent.Context;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.bernie.geckolib.util.ClientUtils;

public class EffectBroadcastPacket {
   private static final Logger LOGGER = LogManager.getLogger("pointblank");
   protected int playerEntityId;
   protected UUID gunStateId;
   protected UUID effectId;
   protected Vec3 startPosition;
   protected SimpleHitResult hitResult;
   protected boolean hasMuzzlePositionProvider;

   public EffectBroadcastPacket() {
   }

   public EffectBroadcastPacket(int playerEntityId, UUID gunStateId, UUID effectId, Vec3 startPosition, SimpleHitResult hitResult, boolean hasMuzzlePositionProvider) {
      this.playerEntityId = playerEntityId;
      this.gunStateId = gunStateId;
      this.effectId = effectId;
      this.startPosition = startPosition;
      this.hitResult = hitResult;
      this.hasMuzzlePositionProvider = hasMuzzlePositionProvider;
   }

   public static <T extends EffectBroadcastPacket> void encode(T packet, FriendlyByteBuf buffer) {
      buffer.writeInt(packet.playerEntityId);
      buffer.writeLong(packet.gunStateId.getMostSignificantBits());
      buffer.writeLong(packet.gunStateId.getLeastSignificantBits());
      buffer.writeLong(packet.effectId.getMostSignificantBits());
      buffer.writeLong(packet.effectId.getLeastSignificantBits());
      buffer.writeDouble(packet.startPosition.f_82479_);
      buffer.writeDouble(packet.startPosition.f_82480_);
      buffer.writeDouble(packet.startPosition.f_82481_);
      buffer.m_236835_(Optional.ofNullable(packet.hitResult), SimpleHitResult.writer());
      buffer.writeBoolean(packet.hasMuzzlePositionProvider);
      packet.doEncode(buffer);
   }

   protected void doEncode(FriendlyByteBuf buffer) {
   }

   protected static EffectBroadcastPacket decode(FriendlyByteBuf buffer) {
      int playerEntityId = buffer.readInt();
      UUID gunStateId = new UUID(buffer.readLong(), buffer.readLong());
      UUID effectId = new UUID(buffer.readLong(), buffer.readLong());
      Vec3 startPosition = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
      SimpleHitResult hitResult = (SimpleHitResult)buffer.m_236860_(SimpleHitResult.reader()).orElse((Object)null);
      boolean hasMuzzlePositionProvider = buffer.readBoolean();
      return new EffectBroadcastPacket(playerEntityId, gunStateId, effectId, startPosition, hitResult, hasMuzzlePositionProvider);
   }

   public static <T extends EffectBroadcastPacket> void handle(T packet, Supplier<Context> ctx) {
      ((Context)ctx.get()).enqueueWork(() -> {
         ClientEventHandler.runSyncTick(() -> {
            if (Config.particleEffectsEnabled) {
               launchEffect(packet);
            }

         });
      });
      ((Context)ctx.get()).setPacketHandled(true);
   }

   private static <T extends EffectBroadcastPacket> void launchEffect(T packet) {
      Minecraft mc = Minecraft.m_91087_();
      Player clientPlayer = ClientUtils.getClientPlayer();
      Entity effectOwnerEntity = mc.f_91073_.m_6815_(packet.playerEntityId);
      if (effectOwnerEntity instanceof Player) {
         Player effectOwnerPlayer = (Player)effectOwnerEntity;
         if (effectOwnerPlayer == clientPlayer) {
         }

         Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> effectBuilderSupplier = EffectRegistry.getEffectBuilderSupplier(packet.effectId);
         if (effectBuilderSupplier != null) {
            EffectBuilder<? extends EffectBuilder<?, ?>, ?> effectBuilder = (EffectBuilder)effectBuilderSupplier.get();
            HitResult hitResult = packet.hitResult;
            double distanceToTarget;
            if (hitResult != null) {
               distanceToTarget = hitResult.m_82450_().m_82554_(packet.startPosition);
            } else {
               distanceToTarget = 400.0D;
            }

            GunClientState gunState = GunClientState.getState(packet.gunStateId);
            PositionProvider positionProvider = null;
            PoseProvider poseProvider = null;
            if (packet.hasMuzzlePositionProvider && gunState != null) {
               positionProvider = () -> {
                  return GunStatePoseProvider.getInstance().getPositionAndDirection(gunState, GunStatePoseProvider.PoseContext.THIRD_PERSON_MUZZLE);
               };
               poseProvider = () -> {
                  return GunStatePoseProvider.getInstance().getPose(gunState, GunStatePoseProvider.PoseContext.THIRD_PERSON_MUZZLE);
               };
            }

            EffectBuilder.Context effectBuilderContext = (new EffectBuilder.Context()).withGunState(gunState).withStartPosition(packet.startPosition).withDistance((float)distanceToTarget).withRandomization(0.0F).withVertexConsumerTransformer(VertexConsumers.PARTICLE).withPositionProvider(positionProvider).withPoseProvider(poseProvider).withHitResult(packet.hitResult);
            Effect effect = effectBuilder.build(effectBuilderContext);
            LOGGER.debug("Launching effect {}", effect);
            effect.launch(effectOwnerPlayer);
         }
      }

   }
}
