package com.vicmatskiv.pointblank.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HitScan {
   private static final Logger LOGGER = LogManager.getLogger("pointblank");

   public static List<HitResult> getObjectsInCrosshair1(LivingEntity player, float partialTicks, double maxDistance, int count, double inaccuracy, long seed, Predicate<Block> isBreakableBlock, Predicate<Block> isPassThroughBlock, List<BlockPos> blockPosToBreakOutput) {
      Vec3 startPos = player.m_146892_();
      Vec3 direction = player.m_20252_(partialTicks);
      return getObjectsInCrosshair(player, startPos, direction, partialTicks, maxDistance, count, inaccuracy, seed, isBreakableBlock, isPassThroughBlock, blockPosToBreakOutput);
   }

   public static List<HitResult> getObjectsInCrosshair(LivingEntity player, Vec3 startPos, Vec3 direction, float partialTicks, double maxDistance, int count, double inaccuracy, long seed, Predicate<Block> isBreakableBlock, Predicate<Block> isPassThroughBlock, List<BlockPos> blockPosToBreakOutput) {
      List<HitResult> hitResults = new ArrayList();
      Random random = new Random(seed);

      for(int i = 0; i < count; ++i) {
         long startTime = System.currentTimeMillis();
         long iterationSeed = seed ^ random.nextLong();
         Vec3 deviatedLookVec = getDeviatedDirectionVector(player, direction, partialTicks, inaccuracy, iterationSeed);
         HitResult hitResult = getNearestObjectInCrosshair(player, startPos, deviatedLookVec, partialTicks, maxDistance, isBreakableBlock, isPassThroughBlock, blockPosToBreakOutput);
         if (hitResult != null) {
            hitResults.add(hitResult);
         }

         long endTime = System.currentTimeMillis();
         LOGGER.debug("{} - obtained hit result in {}ms", System.currentTimeMillis() % 100000L, endTime - startTime);
      }

      return hitResults;
   }

   public static HitResult ensureEntityInCrosshair(LivingEntity player, Entity targetEntity, float partialTicks, double maxDistance, float bbExpansion) {
      Vec3 direction = player.m_20252_(partialTicks);
      Vec3 startPos = player.m_146892_();
      return ensureEntityInCrosshair(player, targetEntity, startPos, direction, partialTicks, maxDistance, bbExpansion);
   }

   public static HitResult getNearestObjectInCrosshair(LivingEntity player, float partialTicks, double maxDistance, double inaccuracy, long seed, Predicate<Block> isBreakableBlock, Predicate<Block> isPassThroughBlock, List<BlockPos> blockPosToBreakOutput) {
      Vec3 direction = player.m_20252_(partialTicks);
      Vec3 deviatedLookVec = getDeviatedDirectionVector(player, direction, partialTicks, inaccuracy, seed);
      Vec3 startPos = player.m_146892_();
      return getNearestObjectInCrosshair(player, startPos, deviatedLookVec, partialTicks, maxDistance, isBreakableBlock, isPassThroughBlock, blockPosToBreakOutput);
   }

   public static HitResult getNearestObjectInCrosshair(LivingEntity player, float partialTicks, double maxDistance, Predicate<Block> isBreakableBlock, Predicate<Block> isPassThroughBlock, List<BlockPos> blockPosToBreakOutput) {
      Vec3 direction = player.m_20252_(partialTicks);
      Vec3 startPos = player.m_146892_();
      return getNearestObjectInCrosshair(player, startPos, direction, partialTicks, maxDistance, isBreakableBlock, isPassThroughBlock, blockPosToBreakOutput);
   }

   private static Vec3 getDeviatedDirectionVector(LivingEntity player, Vec3 dirVector, float partialTicks, double inaccuracy, long seed) {
      Random random = new Random(seed);
      double deviationX = (random.nextDouble() - 0.5D) * 2.0D * inaccuracy;
      double deviationY = (random.nextDouble() - 0.5D) * 2.0D * inaccuracy;
      double deviationZ = (random.nextDouble() - 0.5D) * 2.0D * inaccuracy;
      return dirVector.m_82520_(deviationX, deviationY, deviationZ).m_82541_();
   }

   public static HitResult getNearestObjectInCrosshair(LivingEntity player, Vec3 startPos, Vec3 directionVector, float partialTicks, double maxDistance, Predicate<Block> isBreakableBlock, Predicate<Block> isPassThroughBlock, List<BlockPos> blockPosToBreakOutput) {
      Vec3 endVec = startPos.m_82520_(directionVector.f_82479_ * maxDistance, directionVector.f_82480_ * maxDistance, directionVector.f_82481_ * maxDistance);
      AABB playerBox = player.m_20191_();
      AABB expandedBox = playerBox.m_82363_(directionVector.f_82479_ * maxDistance, directionVector.f_82480_ * maxDistance, directionVector.f_82481_ * maxDistance);
      Entity closestEntity = null;
      double closestEntityDistance = maxDistance;
      Vec3 closestEntityHitVec = null;
      Iterator var16 = MiscUtil.getLevel(player).m_45933_(player, expandedBox).iterator();

      double blockDistance;
      while(var16.hasNext()) {
         Entity entity = (Entity)var16.next();
         if (!entity.m_5833_() && entity.m_6087_() && entity.m_6084_()) {
            AABB entityBox = entity.m_20191_().m_82400_(0.3D);
            Optional<Vec3> hitVec = entityBox.m_82371_(startPos, endVec);
            if (hitVec.isPresent()) {
               blockDistance = startPos.m_82554_((Vec3)hitVec.get());
               if (blockDistance < closestEntityDistance) {
                  closestEntity = entity;
                  closestEntityDistance = blockDistance;
                  closestEntityHitVec = (Vec3)hitVec.get();
               }
            }
         }
      }

      BlockHitResult blockHit = MiscUtil.getLevel(player).m_45547_(new ClipContext(startPos, endVec, ClipContext.Block.COLLIDER, Fluid.NONE, player));

      while(blockHit.m_6662_() != Type.MISS) {
         BlockPos blockPos = blockHit.m_82425_();
         BlockState blockState = MiscUtil.getLevel(player).m_8055_(blockPos);
         Block block = blockState.m_60734_();
         if (isPassThroughBlock.test(block)) {
            blockHit = MiscUtil.getLevel(player).m_45547_(new ClipContext(blockHit.m_82450_(), endVec, ClipContext.Block.COLLIDER, Fluid.NONE, player));
         } else {
            if (!isBreakableBlock.test(block)) {
               blockDistance = startPos.m_82554_(blockHit.m_82450_());
               if (blockDistance < closestEntityDistance) {
                  return blockHit;
               }
               break;
            }

            blockPosToBreakOutput.add(blockPos);
            blockHit = MiscUtil.getLevel(player).m_45547_(new ClipContext(blockHit.m_82450_(), endVec, ClipContext.Block.COLLIDER, Fluid.NONE, player));
         }
      }

      return (HitResult)(closestEntity != null ? new EntityHitResult(closestEntity, closestEntityHitVec) : blockHit);
   }

   public static boolean isHeadshot(LivingEntity entity, Vec3 hitVec) {
      AABB entityBox = entity.m_20191_();
      double headHeightStart = entityBox.f_82289_ + (double)entity.m_20192_() - (double)entity.m_20206_() * 0.12D;
      double hOffset = 0.301D;
      double babyVOffset = entity.m_6162_() ? 0.5D : 0.0D;
      AABB headBox = new AABB(entityBox.f_82288_ - hOffset, headHeightStart, entityBox.f_82290_ - hOffset, entityBox.f_82291_ + hOffset, entityBox.f_82292_ + (double)entity.m_20206_() * 0.1D + babyVOffset, entityBox.f_82293_ + hOffset);
      return headBox.m_82390_(hitVec);
   }

   protected static HitResult ensureEntityInCrosshair(LivingEntity player, Entity targetEntity, Vec3 startPos, Vec3 directionVector, float partialTicks, double maxDistance, float bbExpansion) {
      Vec3 endVec = startPos.m_82520_(directionVector.f_82479_ * maxDistance, directionVector.f_82480_ * maxDistance, directionVector.f_82481_ * maxDistance);
      AABB playerBox = player.m_20191_();
      AABB expandedBox = playerBox.m_82363_(directionVector.f_82479_ * maxDistance, directionVector.f_82480_ * maxDistance, directionVector.f_82481_ * maxDistance);
      Vec3 closestEntityHitVec = null;
      Iterator var12 = MiscUtil.getLevel(player).m_45933_(player, expandedBox).iterator();

      while(var12.hasNext()) {
         Entity entity = (Entity)var12.next();
         if (entity == targetEntity && !entity.m_5833_() && entity.m_6087_() && entity.m_6084_()) {
            AABB entityBox = entity.m_20191_().m_82400_((double)bbExpansion);
            Optional<Vec3> hitVec = entityBox.m_82371_(startPos, endVec);
            if (hitVec.isPresent()) {
               closestEntityHitVec = (Vec3)hitVec.get();
            }
         }
      }

      return closestEntityHitVec != null ? new EntityHitResult(targetEntity, closestEntityHitVec) : null;
   }
}
