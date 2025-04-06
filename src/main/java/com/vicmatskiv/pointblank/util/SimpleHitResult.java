package com.vicmatskiv.pointblank.util;

import java.util.Optional;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf.Reader;
import net.minecraft.network.FriendlyByteBuf.Writer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class SimpleHitResult extends HitResult {
   private Type type;
   private Direction direction;
   private int entityId;

   public SimpleHitResult(Vec3 location, Type type, Direction direction, int entityId) {
      super(location);
      this.type = type;
      this.direction = direction;
      this.entityId = entityId;
   }

   public Type m_6662_() {
      return this.type;
   }

   public Direction getDirection() {
      return this.direction;
   }

   public int getEntityId() {
      return this.entityId;
   }

   public static SimpleHitResult fromHitResult(HitResult hitResult) {
      if (hitResult == null) {
         return null;
      } else {
         SimpleHitResult simpleHitResult;
         if (hitResult instanceof SimpleHitResult) {
            simpleHitResult = (SimpleHitResult)hitResult;
            return simpleHitResult;
         } else {
            simpleHitResult = null;
            switch(hitResult.m_6662_()) {
            case BLOCK:
               simpleHitResult = new SimpleHitResult(hitResult.m_82450_(), Type.BLOCK, ((BlockHitResult)hitResult).m_82434_(), -1);
               break;
            case ENTITY:
               simpleHitResult = new SimpleHitResult(hitResult.m_82450_(), Type.ENTITY, (Direction)null, ((EntityHitResult)hitResult).m_82443_().m_19879_());
               break;
            case MISS:
               simpleHitResult = new SimpleHitResult(hitResult.m_82450_(), Type.MISS, (Direction)null, -1);
            }

            return simpleHitResult;
         }
      }
   }

   public static Writer<SimpleHitResult> writer() {
      return (buf, hitResult) -> {
         buf.m_130068_(hitResult.type);
         buf.writeDouble(hitResult.f_82445_.f_82479_);
         buf.writeDouble(hitResult.f_82445_.f_82480_);
         buf.writeDouble(hitResult.f_82445_.f_82481_);
         buf.m_236835_(Optional.ofNullable(hitResult.direction), (b, e) -> {
            b.m_130068_(e);
         });
         buf.writeInt(hitResult.entityId);
      };
   }

   public static Reader<SimpleHitResult> reader() {
      return (buf) -> {
         Type type = (Type)buf.m_130066_(Type.class);
         double x = buf.readDouble();
         double y = buf.readDouble();
         double z = buf.readDouble();
         Optional<Direction> direction = buf.m_236860_((b) -> {
            return (Direction)b.m_130066_(Direction.class);
         });
         int entityId = buf.readInt();
         return new SimpleHitResult(new Vec3(x, y, z), type, (Direction)direction.orElse((Object)null), entityId);
      };
   }
}
