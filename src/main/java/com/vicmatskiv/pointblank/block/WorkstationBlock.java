package com.vicmatskiv.pointblank.block;

import com.vicmatskiv.pointblank.registry.BlockEntityRegistry;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;

public class WorkstationBlock extends BaseEntityBlock implements EntityBlock {
   public static final DirectionProperty FACING;

   public WorkstationBlock() {
      super(Properties.m_284310_().m_60978_(2.0F).m_60955_());
   }

   public RenderShape m_7514_(BlockState state) {
      return RenderShape.ENTITYBLOCK_ANIMATED;
   }

   protected void m_7926_(Builder<Block, BlockState> builder) {
      builder.m_61104_(new Property[]{FACING});
   }

   @Nullable
   public BlockState m_5573_(BlockPlaceContext context) {
      return (BlockState)this.m_49966_().m_61124_(FACING, context.m_8125_().m_122427_().m_122427_());
   }

   @Nullable
   public BlockEntity m_142194_(BlockPos blockPos, BlockState blockState) {
      return ((BlockEntityType)BlockEntityRegistry.WORKSTATION_BLOCK_ENTITY.get()).m_155264_(blockPos, blockState);
   }

   public boolean m_7898_(BlockState state, LevelReader world, BlockPos pos) {
      return true;
   }

   static {
      FACING = BlockStateProperties.f_61372_;
   }
}
