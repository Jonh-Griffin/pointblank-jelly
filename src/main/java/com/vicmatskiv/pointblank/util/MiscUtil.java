package com.vicmatskiv.pointblank.util;

import com.vicmatskiv.pointblank.item.GunItem;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf.Reader;
import net.minecraft.network.FriendlyByteBuf.Writer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;
import org.joml.Quaternionf;

public class MiscUtil {
   private static final double EPSILON = 1.0E-8D;
   public static final Writer<Vec3> VEC3_WRITER = (buf, vec3) -> {
      buf.writeDouble(vec3.f_82479_);
      buf.writeDouble(vec3.f_82480_);
      buf.writeDouble(vec3.f_82481_);
   };
   public static final Reader<Vec3> VEC3_READER = (buf) -> {
      return new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
   };

   public static boolean isNearlyZero(double value) {
      return Math.abs(value) < 1.0E-8D;
   }

   public static boolean isGreaterThanZero(double value) {
      return value > 1.0E-8D;
   }

   public static Level getLevel(Entity entity) {
      return entity.m_9236_();
   }

   public static boolean isClientSide(Entity entity) {
      return entity.m_9236_().f_46443_;
   }

   public LivingEntity asLivingEntity(Entity entity) {
      if (entity instanceof LivingEntity) {
         LivingEntity livingEntity = (LivingEntity)entity;
         return livingEntity;
      } else {
         if (entity instanceof PartEntity) {
            PartEntity entityPart = (PartEntity)entity;
            Entity var5 = entityPart.getParent();
            if (var5 instanceof LivingEntity) {
               LivingEntity livingEntity = (LivingEntity)var5;
               return livingEntity;
            }
         }

         return null;
      }
   }

   public static boolean isProtected(Entity entity) {
      return entity instanceof Cat || entity instanceof Ocelot;
   }

   public static Optional<GunItem> getMainHeldGun(LivingEntity entity) {
      ItemStack itemStack = entity.m_21205_();
      return itemStack != null && itemStack.m_41720_() instanceof GunItem ? Optional.of((GunItem)itemStack.m_41720_()) : Optional.empty();
   }

   public static Quaternionf getRotation(Direction face) {
      Quaternionf quaternionf = null;
      switch(face) {
      case DOWN:
         quaternionf = (new Quaternionf()).rotationXYZ(1.5707964F, 0.0F, 0.0F);
      case UP:
         break;
      case NORTH:
         quaternionf = (new Quaternionf()).rotationXYZ(0.0F, 0.0F, 1.5707964F);
         break;
      case SOUTH:
         quaternionf = (new Quaternionf()).rotationXYZ(0.0F, 0.0F, 1.5707964F);
         break;
      case WEST:
         quaternionf = (new Quaternionf()).rotationXYZ(0.0F, 1.5707964F, 0.0F);
         break;
      case EAST:
         quaternionf = (new Quaternionf()).rotationXYZ(0.0F, 1.5707964F, 0.0F);
         break;
      default:
         throw new IncompatibleClassChangeError();
      }

      return quaternionf;
   }

   public static double timeToTravel(double initialSpeed, double acceleration, double distance) {
      if (acceleration == 0.0D) {
         return distance / initialSpeed;
      } else {
         double a = 0.5D * acceleration;
         double c = -distance;
         double discriminant = initialSpeed * initialSpeed - 4.0D * a * c;
         return discriminant < 0.0D ? -1.0D : (-initialSpeed + Math.sqrt(discriminant)) / (2.0D * a);
      }
   }

   public static double adjustDivisor(double dividend, double divisor) {
      if (divisor == 0.0D) {
         throw new IllegalArgumentException("Divisor cannot be zero.");
      } else {
         double quotient = dividend / divisor;
         long roundedQuotient = Math.round(quotient);
         double adjustedDivisor = dividend / (double)roundedQuotient;
         return adjustedDivisor;
      }
   }

   public static UUID getTagId(CompoundTag tag) {
      return tag != null ? new UUID(tag.m_128454_("mid"), tag.m_128454_("lid")) : null;
   }

   public static UUID getItemStackId(ItemStack itemStack) {
      CompoundTag idTag = itemStack.m_41783_();
      return getTagId(idTag);
   }
}
