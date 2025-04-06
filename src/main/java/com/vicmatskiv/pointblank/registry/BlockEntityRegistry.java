package com.vicmatskiv.pointblank.registry;

import com.mojang.datafixers.types.Type;
import com.vicmatskiv.pointblank.block.entity.PrinterBlockEntity;
import com.vicmatskiv.pointblank.block.entity.WorkstationBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityType.Builder;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class BlockEntityRegistry {
   public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES;
   public static final RegistryObject<BlockEntityType<WorkstationBlockEntity>> WORKSTATION_BLOCK_ENTITY;
   public static final RegistryObject<BlockEntityType<PrinterBlockEntity>> PRINTER_BLOCK_ENTITY;

   static {
      BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "pointblank");
      WORKSTATION_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("workstation_block_entity", () -> {
         return Builder.m_155273_(WorkstationBlockEntity::new, new Block[]{(Block)BlockRegistry.WORKSTATION.get()}).m_58966_((Type)null);
      });
      PRINTER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("printer_block_entity", () -> {
         return Builder.m_155273_(PrinterBlockEntity::new, new Block[]{(Block)BlockRegistry.PRINTER.get()}).m_58966_((Type)null);
      });
   }
}
