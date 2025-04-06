package com.vicmatskiv.pointblank.registry;

import com.vicmatskiv.pointblank.block.PrinterBlock;
import com.vicmatskiv.pointblank.block.WorkstationBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class BlockRegistry {
   public static final DeferredRegister<Block> BLOCKS;
   public static final RegistryObject<Block> WORKSTATION;
   public static final RegistryObject<Block> PRINTER;

   static {
      BLOCKS = DeferredRegister.create(Registries.f_256747_, "pointblank");
      WORKSTATION = BLOCKS.register("workstation", WorkstationBlock::new);
      PRINTER = BLOCKS.register("printer", PrinterBlock::new);
   }
}
