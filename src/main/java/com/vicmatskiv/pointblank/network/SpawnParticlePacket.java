package com.vicmatskiv.pointblank.network;

import java.util.function.Supplier;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent.Context;
import net.minecraftforge.registries.ForgeRegistries;
import software.bernie.geckolib.util.ClientUtils;

public class SpawnParticlePacket {
   private final ParticleType<?> particleType;
   private final double x;
   private final double y;
   private final double z;
   private final int count;

   public SpawnParticlePacket(ParticleType<?> particleType, double x, double y, double z, int count) {
      this.particleType = particleType;
      this.x = x;
      this.y = y;
      this.z = z;
      this.count = count;
   }

   public static void encode(SpawnParticlePacket packet, FriendlyByteBuf buf) {
      buf.m_130085_(ForgeRegistries.PARTICLE_TYPES.getKey(packet.particleType));
      buf.writeDouble(packet.x);
      buf.writeDouble(packet.y);
      buf.writeDouble(packet.z);
      buf.writeInt(packet.count);
   }

   public static SpawnParticlePacket decode(FriendlyByteBuf buf) {
      return new SpawnParticlePacket((ParticleType)ForgeRegistries.PARTICLE_TYPES.getValue(buf.m_130281_()), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readInt());
   }

   public static void handle(SpawnParticlePacket packet, Supplier<Context> context) {
      ((Context)context.get()).enqueueWork(() -> {
         Level level = ClientUtils.getLevel();

         for(int i = 0; i < packet.count; ++i) {
            double offsetX = Math.random() * 0.5D - 0.25D;
            double offsetY = Math.random() * 0.5D - 0.25D;
            double offsetZ = Math.random() * 0.5D - 0.25D;
            double x = packet.x + offsetX;
            double y = packet.y + offsetY;
            double z = packet.z + offsetZ;
            level.m_7106_(ParticleTypes.f_123762_, x, y, z, 0.0D, 0.0D, 0.0D);
         }

      });
      ((Context)context.get()).setPacketHandled(true);
   }

   public ParticleType<?> getParticleType() {
      return this.particleType;
   }

   public double getX() {
      return this.x;
   }

   public double getY() {
      return this.y;
   }

   public double getZ() {
      return this.z;
   }

   public int getCount() {
      return this.count;
   }
}
