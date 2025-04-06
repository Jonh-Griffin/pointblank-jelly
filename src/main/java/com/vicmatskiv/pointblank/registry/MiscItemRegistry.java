package com.vicmatskiv.pointblank.registry;

import com.vicmatskiv.pointblank.client.render.DefaultModelRenderer;
import com.vicmatskiv.pointblank.item.ItemBuilder;
import com.vicmatskiv.pointblank.item.MiscItem;
import com.vicmatskiv.pointblank.item.PrinterItem;
import com.vicmatskiv.pointblank.item.WorkstationItem;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

public class MiscItemRegistry {
   public static final Supplier<Item> PROCESSOR;
   public static final Supplier<Item> GUNINTERNALS;
   public static final Supplier<Item> MOTOR;
   public static final Supplier<Item> GUNMETAL_MESH;
   public static final Supplier<Item> GUNMETAL_INGOT;
   public static final Supplier<Item> GUNMETAL_NUGGET;
   public static final Supplier<BlockItem> WORKSTATION;
   public static final Supplier<BlockItem> PRINTER;

   public static void init() {
   }

   public static void registerTabItems(Consumer<ItemLike> entries) {
      entries.accept((ItemLike)PROCESSOR.get());
      entries.accept((ItemLike)GUNINTERNALS.get());
      entries.accept((ItemLike)MOTOR.get());
      entries.accept((ItemLike)GUNMETAL_MESH.get());
      entries.accept((ItemLike)GUNMETAL_INGOT.get());
      entries.accept((ItemLike)GUNMETAL_NUGGET.get());
      entries.accept((ItemLike)WORKSTATION.get());
      entries.accept((ItemLike)PRINTER.get());
   }

   static {
      PROCESSOR = ItemRegistry.ITEMS.register((ItemBuilder)(new MiscItem.MiscItemBuilder()).withName("processor").withRenderer(() -> {
         return new DefaultModelRenderer("processor");
      }).withTradePrice(100.0D, 1));
      GUNINTERNALS = ItemRegistry.ITEMS.register((ItemBuilder)(new MiscItem.MiscItemBuilder()).withName("guninternals").withRenderer(() -> {
         return new DefaultModelRenderer("guninternals");
      }).withTradePrice(500.0D, 2));
      MOTOR = ItemRegistry.ITEMS.register((ItemBuilder)(new MiscItem.MiscItemBuilder()).withName("motor").withRenderer(() -> {
         return new DefaultModelRenderer("motor");
      }).withTradePrice(30000.0D, 2));
      GUNMETAL_MESH = ItemRegistry.itemRegistry.register("gunmetal_mesh", () -> {
         return new Item(new Properties());
      });
      GUNMETAL_INGOT = ItemRegistry.itemRegistry.register("gunmetal_ingot", () -> {
         return new Item(new Properties());
      });
      GUNMETAL_NUGGET = ItemRegistry.itemRegistry.register("gunmetal_nugget", () -> {
         return new Item(new Properties());
      });
      WORKSTATION = ItemRegistry.itemRegistry.register("workstation", () -> {
         return new WorkstationItem((Block)BlockRegistry.WORKSTATION.get(), new Properties());
      });
      PRINTER = ItemRegistry.itemRegistry.register("printer", () -> {
         return new PrinterItem((Block)BlockRegistry.PRINTER.get(), new Properties());
      });
   }
}
