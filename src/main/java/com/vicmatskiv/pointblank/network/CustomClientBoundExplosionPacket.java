package com.vicmatskiv.pointblank.network;

import com.google.common.collect.Lists;
import com.vicmatskiv.pointblank.client.ClientEventHandler;
import com.vicmatskiv.pointblank.explosion.CustomExplosion;
import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent.Context;

public class CustomClientBoundExplosionPacket {
   private Item item;
   private double x;
   private double y;
   private double z;
   private float power;
   private List<BlockPos> toBlow;
   private float knockbackX;
   private float knockbackY;
   private float knockbackZ;

   public CustomClientBoundExplosionPacket(Item item, double posX, double posY, double posZ, float damage, List<BlockPos> toBlow, @Nullable Vec3 knockback) {
      this.item = item;
      this.x = posX;
      this.y = posY;
      this.z = posZ;
      this.power = damage;
      this.toBlow = Lists.newArrayList(toBlow);
      if (knockback != null) {
         this.knockbackX = (float)knockback.f_82479_;
         this.knockbackY = (float)knockback.f_82480_;
         this.knockbackZ = (float)knockback.f_82481_;
      } else {
         this.knockbackX = 0.0F;
         this.knockbackY = 0.0F;
         this.knockbackZ = 0.0F;
      }

   }

   public CustomClientBoundExplosionPacket() {
   }

   public static CustomClientBoundExplosionPacket decode(FriendlyByteBuf buf) {
      CustomClientBoundExplosionPacket packet = new CustomClientBoundExplosionPacket();
      Item item = (Item)buf.m_236816_(BuiltInRegistries.f_257033_);
      if (item != Items.f_41852_) {
         packet.item = item;
      }

      packet.x = buf.readDouble();
      packet.y = buf.readDouble();
      packet.z = buf.readDouble();
      packet.power = buf.readFloat();
      int i = Mth.m_14107_(packet.x);
      int j = Mth.m_14107_(packet.y);
      int k = Mth.m_14107_(packet.z);
      packet.toBlow = buf.m_236845_((p_178850_) -> {
         int l = p_178850_.readByte() + i;
         int i1 = p_178850_.readByte() + j;
         int j1 = p_178850_.readByte() + k;
         return new BlockPos(l, i1, j1);
      });
      packet.knockbackX = buf.readFloat();
      packet.knockbackY = buf.readFloat();
      packet.knockbackZ = buf.readFloat();
      return packet;
   }

   public static void encode(CustomClientBoundExplosionPacket packet, FriendlyByteBuf buf) {
      buf.m_236818_(BuiltInRegistries.f_257033_, packet.item);
      buf.writeDouble(packet.x);
      buf.writeDouble(packet.y);
      buf.writeDouble(packet.z);
      buf.writeFloat(packet.power);
      int i = Mth.m_14107_(packet.x);
      int j = Mth.m_14107_(packet.y);
      int k = Mth.m_14107_(packet.z);
      buf.m_236828_(packet.toBlow, (p_178855_, p_178856_) -> {
         int l = p_178856_.m_123341_() - i;
         int i1 = p_178856_.m_123342_() - j;
         int j1 = p_178856_.m_123343_() - k;
         p_178855_.writeByte(l);
         p_178855_.writeByte(i1);
         p_178855_.writeByte(j1);
      });
      buf.writeFloat(packet.knockbackX);
      buf.writeFloat(packet.knockbackY);
      buf.writeFloat(packet.knockbackZ);
   }

   public static void handle(CustomClientBoundExplosionPacket packet, Supplier<Context> ctx) {
      ((Context)ctx.get()).enqueueWork(() -> {
         ClientEventHandler.runSyncTick(() -> {
            handleClient(packet, ctx);
         });
      });
      ((Context)ctx.get()).setPacketHandled(true);
   }

   @OnlyIn(Dist.CLIENT)
   private static void handleClient(CustomClientBoundExplosionPacket packet, Supplier<Context> ctx) {
      Minecraft mc = Minecraft.m_91087_();
      CustomExplosion explosion = new CustomExplosion(mc.f_91073_, packet.item, (Entity)null, packet.x, packet.y, packet.z, packet.power, packet.toBlow);
      explosion.finalizeClientExplosion();
      mc.f_91074_.m_20256_(mc.f_91074_.m_20184_().m_82520_((double)packet.knockbackX, (double)packet.knockbackY, (double)packet.knockbackZ));
   }

   public Item getItem() {
      return this.item;
   }

   public float getKnockbackX() {
      return this.knockbackX;
   }

   public float getKnockbackY() {
      return this.knockbackY;
   }

   public float getKnockbackZ() {
      return this.knockbackZ;
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

   public float getPower() {
      return this.power;
   }

   public List<BlockPos> getToBlow() {
      return this.toBlow;
   }
}
