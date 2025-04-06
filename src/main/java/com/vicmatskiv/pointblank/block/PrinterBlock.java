package com.vicmatskiv.pointblank.block;

import com.vicmatskiv.pointblank.block.entity.PrinterBlockEntity;
import com.vicmatskiv.pointblank.registry.BlockEntityRegistry;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

public class PrinterBlock extends BaseEntityBlock implements EntityBlock {
   public static final DirectionProperty FACING;

   public PrinterBlock() {
      super(Properties.m_284310_().m_284180_(MapColor.f_283906_).m_280658_(NoteBlockInstrument.IRON_XYLOPHONE).m_60999_().m_60913_(1.0F, 2.0F).m_60918_(SoundType.f_56743_).m_60955_());
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
      return ((BlockEntityType)BlockEntityRegistry.PRINTER_BLOCK_ENTITY.get()).m_155264_(blockPos, blockState);
   }

   public boolean m_7898_(BlockState state, LevelReader world, BlockPos pos) {
      return true;
   }

   public InteractionResult m_6227_(BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand interationHand, BlockHitResult blockHitResult) {
      if (level.f_46443_) {
         return InteractionResult.SUCCESS;
      } else {
         BlockEntity blockEntity = level.m_7702_(blockPos);
         if (blockEntity instanceof PrinterBlockEntity) {
            PrinterBlockEntity wbe = (PrinterBlockEntity)blockEntity;
            if (wbe.getState() == PrinterBlockEntity.State.IDLE) {
               player.m_5893_(blockState.m_60750_(level, blockPos));
               player.m_36220_(Stats.f_12967_);
               return InteractionResult.CONSUME;
            }
         }

         return InteractionResult.FAIL;
      }
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> m_142354_(Level level, BlockState blockState, BlockEntityType<T> entityType) {
      return m_152132_(entityType, (BlockEntityType)BlockEntityRegistry.PRINTER_BLOCK_ENTITY.get(), level.f_46443_ ? PrinterBlockEntity::clientTick : PrinterBlockEntity::serverTick);
   }

   static {
      FACING = BlockStateProperties.f_61372_;
   }
}
