package com.vicmatskiv.pointblank.item;

import com.google.gson.JsonObject;
import com.vicmatskiv.pointblank.Nameable;
import com.vicmatskiv.pointblank.client.render.DefaultModelRenderer;
import com.vicmatskiv.pointblank.crafting.Craftable;
import com.vicmatskiv.pointblank.util.JsonUtil;
import com.vicmatskiv.pointblank.util.TimeUnit;
import com.vicmatskiv.pointblank.util.Tradeable;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.fml.loading.FMLLoader;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.util.GeckoLibUtil;

public class MiscItem extends Item implements Nameable, Tradeable, Craftable {
   private String name;
   private float tradePrice;
   private int tradeBundleQuantity;
   private int tradeLevel;
   private long craftingDuration;

   public MiscItem(String name, float tradePrice, int tradeBundleQuantity, int tradeLevel, long craftingDuration) {
      super(new Properties());
      this.name = name;
      this.tradePrice = tradePrice;
      this.tradeLevel = tradeLevel;
      this.tradeBundleQuantity = tradeBundleQuantity;
      this.craftingDuration = craftingDuration;
   }

   public String getName() {
      return this.name;
   }

   public float getPrice() {
      return this.tradePrice;
   }

   public int getTradeLevel() {
      return this.tradeLevel;
   }

   public int getBundleQuantity() {
      return this.tradeBundleQuantity;
   }

   public long getCraftingDuration() {
      return this.craftingDuration;
   }

   private static class MiscModelItem extends MiscItem implements GeoItem {
      private Supplier<Object> rendererSupplier;
      private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

      public MiscModelItem(String name, Supplier<Object> rendererSupplier, float tradePrice, int tradeBundleQuantity, int tradeLevel, long craftingDuration) {
         super(name, tradePrice, tradeBundleQuantity, tradeLevel, craftingDuration);
         this.rendererSupplier = rendererSupplier;
      }

      public void registerControllers(ControllerRegistrar registry) {
      }

      public AnimatableInstanceCache getAnimatableInstanceCache() {
         return this.cache;
      }

      public void initializeClient(Consumer<IClientItemExtensions> consumer) {
         consumer.accept(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;
            // $FF: synthetic field
            final MiscModelItem this$0;

            {
               this.this$0 = this$0;
            }

            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
               if (this.renderer == null) {
                  this.renderer = (BlockEntityWithoutLevelRenderer)this.this$0.rendererSupplier.get();
               }

               return this.renderer;
            }
         });
      }
   }

   public static class MiscItemBuilder extends ItemBuilder<MiscItemBuilder> implements Nameable {
      private static final float DEFAULT_PRICE = Float.NaN;
      private static final int DEFAULT_TRADE_LEVEL = 0;
      private static final int DEFAULT_TRADE_BUNDLE_QUANTITY = 1;
      private static final int DEFAULT_CRAFTING_DURATION = 500;
      private String name;
      private Supplier<Object> rendererBuilder;
      private float tradePrice = Float.NaN;
      private int tradeBundleQuantity = 1;
      private int tradeLevel = 0;
      private long craftingDuration = 500L;

      public MiscItemBuilder withName(String name) {
         this.name = name;
         return this;
      }

      public MiscItemBuilder withRenderer(Supplier<Object> rendererBuilder) {
         this.rendererBuilder = rendererBuilder;
         return this;
      }

      public MiscItemBuilder withTradePrice(double price, int tradeBundleQuantity, int tradeLevel) {
         this.tradePrice = (float)price;
         this.tradeLevel = tradeLevel;
         this.tradeBundleQuantity = tradeBundleQuantity;
         return this;
      }

      public MiscItemBuilder withTradePrice(double price, int tradeLevel) {
         return this.withTradePrice(price, 1, tradeLevel);
      }

      public MiscItemBuilder withCraftingDuration(int duration, TimeUnit timeUnit) {
         this.craftingDuration = timeUnit.toMillis((long)duration);
         return this;
      }

      public MiscItemBuilder withJsonObject(JsonObject obj) {
         this.withName(JsonUtil.getJsonString(obj, "name"));
         this.withTradePrice((double)JsonUtil.getJsonFloat(obj, "tradePrice", Float.NaN), JsonUtil.getJsonInt(obj, "traceBundleQuantity", 1), JsonUtil.getJsonInt(obj, "tradeLevel", 0));
         this.withCraftingDuration(JsonUtil.getJsonInt(obj, "craftingDuration", 500), TimeUnit.MILLISECOND);
         JsonObject rendererObj = obj.getAsJsonObject("renderer");
         Dist side = FMLLoader.getDist();
         if (side.isClient() && rendererObj != null) {
            String rendererType = JsonUtil.getJsonString(rendererObj, "type");
            if (rendererType.toLowerCase().equals("model")) {
               this.withRenderer(() -> {
                  return new DefaultModelRenderer(this.name);
               });
            }
         }

         return this;
      }

      public String getName() {
         return this.name;
      }

      public Item build() {
         return (Item)(this.rendererBuilder != null ? new MiscModelItem(this.name, this.rendererBuilder, this.tradePrice, this.tradeBundleQuantity, this.tradeLevel, this.craftingDuration) : new MiscItem(this.name, this.tradePrice, this.tradeBundleQuantity, this.tradeLevel, this.craftingDuration));
      }
   }
}
