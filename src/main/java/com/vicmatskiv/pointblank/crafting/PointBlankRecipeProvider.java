package com.vicmatskiv.pointblank.crafting;

import com.vicmatskiv.pointblank.item.AmmoItem;
import com.vicmatskiv.pointblank.item.GunItem;
import com.vicmatskiv.pointblank.registry.ItemRegistry;
import com.vicmatskiv.pointblank.registry.RecipeTypeRegistry;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.common.Tags.Items;

public class PointBlankRecipeProvider extends RecipeProvider {
   public PointBlankRecipeProvider(PackOutput output) {
      super(output);
   }

   protected void m_245200_(Consumer<FinishedRecipe> consumer) {
      Iterator var2 = ItemRegistry.ITEMS.getItemsByName().entrySet().iterator();

      while(var2.hasNext()) {
         Entry<String, Supplier<? extends Item>> e = (Entry)var2.next();
         Item item = (Item)((Supplier)e.getValue()).get();
         int quantity = 0;
         PointBlankRecipeBuilder sampleBuilder = new PointBlankRecipeBuilder((RecipeSerializer)RecipeTypeRegistry.DEFAULT_SERIALIZER.get(), item, quantity);
         if (item instanceof GunItem) {
            quantity = 1;
            sampleBuilder.requires(PointBlankIngredient.of((TagKey)Items.INGOTS_GOLD, 1)).requires(PointBlankIngredient.of((TagKey)Items.INGOTS_IRON, 3));
         } else if (item instanceof AmmoItem) {
            quantity = 30;
            sampleBuilder.requires(PointBlankIngredient.of((TagKey)Items.INGOTS_COPPER, 1)).requires(PointBlankIngredient.of((TagKey)Items.INGOTS_IRON, 1)).requires(PointBlankIngredient.of((TagKey)Items.GUNPOWDER, 1));
         }

         if (quantity > 0) {
            sampleBuilder.unlockedBy("has_iron", m_206406_(Items.INGOTS_IRON));
            sampleBuilder.m_176498_(consumer);
         }
      }

   }
}
