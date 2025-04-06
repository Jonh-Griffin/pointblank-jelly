package com.vicmatskiv.pointblank.registry;

import com.vicmatskiv.pointblank.block.entity.PrinterBlockEntity;
import com.vicmatskiv.pointblank.block.entity.WorkstationBlockEntity;
import com.vicmatskiv.pointblank.block.entity.PrinterBlockEntity.State;
import com.vicmatskiv.pointblank.client.model.BaseBlockModel;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;

public class BlockModelRegistry {
   public static Supplier<BaseBlockModel<WorkstationBlockEntity>> WORKSTATION_BLOCK_MODEL = () -> (new BaseBlockModel.Builder<WorkstationBlockEntity>()).withResource(new ResourceLocation("pointblank", "workstation")).build();
   public static Supplier<BaseBlockModel<PrinterBlockEntity>> PRINTER_BLOCK_MODEL = () -> (new BaseBlockModel.Builder<PrinterBlockEntity>()).withResource(new ResourceLocation("pointblank", "printer")).withGlow("glowy", (e) -> {
      boolean var10000;
      if (e instanceof PrinterBlockEntity pbe) {
         if (pbe.getState() != State.CLOSED) {
            var10000 = true;
            return var10000;
         }
      }

      var10000 = false;
      return var10000;
   }).withGlow("screendefault", (e) -> {
      boolean var10000;
      if (e instanceof PrinterBlockEntity pbe) {
         if (pbe.getState() != State.CLOSED) {
            var10000 = true;
            return var10000;
         }
      }

      var10000 = false;
      return var10000;
   }).withGlow("screenfinal", (e) -> {
      boolean var10000;
      if (e instanceof PrinterBlockEntity pbe) {
         if (pbe.getState() != State.CLOSED) {
            var10000 = true;
            return var10000;
         }
      }

      var10000 = false;
      return var10000;
   }).withGlow("screenfinal2", (e) -> {
      boolean var10000;
      if (e instanceof PrinterBlockEntity pbe) {
         if (pbe.getState() != State.CLOSED) {
            var10000 = true;
            return var10000;
         }
      }

      var10000 = false;
      return var10000;
   }).withGlow("screenfinal3", (e) -> {
      boolean var10000;
      if (e instanceof PrinterBlockEntity pbe) {
         if (pbe.getState() != State.CLOSED) {
            var10000 = true;
            return var10000;
         }
      }

      var10000 = false;
      return var10000;
   }).build();

   public BlockModelRegistry() {
   }
}
