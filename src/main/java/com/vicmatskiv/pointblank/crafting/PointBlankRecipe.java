package com.vicmatskiv.pointblank.crafting;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.vicmatskiv.pointblank.item.GunItem;
import com.vicmatskiv.pointblank.registry.RecipeTypeRegistry;
import com.vicmatskiv.pointblank.util.InventoryUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.Util;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.Tags.Items;

public class PointBlankRecipe implements Recipe<Container> {
   private static final int MAX_INGREDIENTS = 10;
   private static Function<Level, Map<Item, PointBlankRecipe>> levelRecipesByItem = Util.m_143827_((level) -> {
      return (Map)level.m_7465_().m_44013_((RecipeType)RecipeTypeRegistry.DEFAULT_RECIPE_TYPE.get()).stream().collect(Collectors.toMap((r) -> {
         return r.getItem();
      }, (r) -> {
         return r;
      }));
   });
   private static Function<Level, Map<ResourceLocation, PointBlankRecipe>> levelRecipesById = Util.m_143827_((level) -> {
      return (Map)level.m_7465_().m_44013_((RecipeType)RecipeTypeRegistry.DEFAULT_RECIPE_TYPE.get()).stream().collect(Collectors.toMap((r) -> {
         return r.m_6423_();
      }, (r) -> {
         return r;
      }));
   });
   private static final int MAX_SIZE = 10;
   private final ResourceLocation id;
   private final String group;
   private final ItemStack result;
   private final ItemStack initializedItemStack;
   private final List<PointBlankIngredient> ingredients;
   private static NonNullList<Ingredient> defaultIngredients;

   public static PointBlankRecipe getRecipe(Level level, Item item) {
      return (PointBlankRecipe)((Map)levelRecipesByItem.apply(level)).get(item);
   }

   public static PointBlankRecipe getRecipe(Level level, ResourceLocation recipeId) {
      return (PointBlankRecipe)((Map)levelRecipesById.apply(level)).get(recipeId);
   }

   public static List<PointBlankRecipe> getRecipes(Level level) {
      return new ArrayList(((Map)levelRecipesById.apply(level)).values());
   }

   public PointBlankRecipe(ResourceLocation recipeId, String group, ItemStack resultItemStack, List<PointBlankIngredient> ingredients) {
      this.id = recipeId;
      this.group = group;
      this.result = resultItemStack.m_41777_();
      this.initializedItemStack = resultItemStack.m_41777_();
      if (this.initializedItemStack.m_41720_() instanceof GunItem) {
         GunItem.initStackForCrafting(this.initializedItemStack);
      }

      if (ingredients != null && !ingredients.isEmpty()) {
         if (ingredients.size() > 10) {
            throw new IllegalArgumentException("Recipe ingredients for item " + resultItemStack + " exceed maximum allowed count of 10");
         } else {
            this.ingredients = ingredients;
         }
      } else {
         throw new IllegalArgumentException("Recipe ingredients are not set for item " + resultItemStack);
      }
   }

   public ResourceLocation m_6423_() {
      return this.id;
   }

   public RecipeSerializer<?> m_7707_() {
      return (RecipeSerializer)RecipeTypeRegistry.DEFAULT_SERIALIZER.get();
   }

   public String m_6076_() {
      return this.group;
   }

   private Item getItem() {
      return this.result.m_41720_();
   }

   public ItemStack m_8043_(RegistryAccess registryAccess) {
      return this.result.m_41777_();
   }

   public ItemStack getInitializedStack() {
      return this.initializedItemStack;
   }

   public NonNullList<Ingredient> m_7527_() {
      return defaultIngredients;
   }

   public List<PointBlankIngredient> getPointBlankIngredients() {
      return this.ingredients;
   }

   public boolean m_5818_(Container container, Level level) {
      throw new UnsupportedOperationException("Implement me!");
   }

   public boolean canBeCrafted(Player player) {
      return this.ingredients.stream().anyMatch((ingredient) -> {
         return InventoryUtils.hasIngredient(player, ingredient);
      });
   }

   public void removeIngredients(Player player) {
      this.ingredients.stream().forEach((ingredient) -> {
         Objects.requireNonNull(ingredient);
         InventoryUtils.removeItem(player, ingredient::matches, ingredient.getCount());
      });
   }

   public ItemStack assemble(CraftingContainer craftingContainer, RegistryAccess registryAccess) {
      return this.result.m_41777_();
   }

   public boolean m_8004_(int m, int n) {
      return false;
   }

   public ItemStack m_5874_(Container container, RegistryAccess registryAccess) {
      throw new UnsupportedOperationException("Implement me!");
   }

   public RecipeType<?> m_6671_() {
      return (RecipeType)RecipeTypeRegistry.DEFAULT_RECIPE_TYPE.get();
   }

   static {
      defaultIngredients = NonNullList.m_122783_(Ingredient.m_204132_(Items.INGOTS), new Ingredient[0]);
   }

   public static class Serializer implements RecipeSerializer<PointBlankRecipe> {
      public PointBlankRecipe fromJson(ResourceLocation recipeId, JsonObject jsonObject) {
         String s = GsonHelper.m_13851_(jsonObject, "group", "");
         NonNullList<PointBlankIngredient> ingredients = itemsFromJson(GsonHelper.m_13933_(jsonObject, "ingredients"));
         if (ingredients.isEmpty()) {
            throw new JsonParseException("No ingredients for the recipe");
         } else if (ingredients.size() > 10) {
            throw new JsonParseException("Too many ingredients for the recipe. The maximum is 10");
         } else {
            ItemStack itemstack = ShapedRecipe.m_151274_(GsonHelper.m_13930_(jsonObject, "result"));
            return new PointBlankRecipe(recipeId, s, itemstack, ingredients);
         }
      }

      private static NonNullList<PointBlankIngredient> itemsFromJson(JsonArray jsonArray) {
         NonNullList<PointBlankIngredient> ingredients = NonNullList.m_122779_();

         for(int i = 0; i < jsonArray.size(); ++i) {
            PointBlankIngredient ingredient = PointBlankIngredient.fromJson(jsonArray.get(i));
            ingredients.add(ingredient);
         }

         return ingredients;
      }

      public PointBlankRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf byteBuf) {
         String recipeGroup = byteBuf.m_130277_();
         int ingredientSize = byteBuf.m_130242_();
         NonNullList<PointBlankIngredient> ingredients = NonNullList.m_122779_();

         for(int j = 0; j < ingredientSize; ++j) {
            ingredients.add(PointBlankIngredient.fromNetwork(byteBuf));
         }

         ItemStack recipeResult = byteBuf.m_130267_();
         return new PointBlankRecipe(recipeId, recipeGroup, recipeResult, ingredients);
      }

      public void toNetwork(FriendlyByteBuf byteBuf, PointBlankRecipe recipe) {
         byteBuf.m_130070_(recipe.group);
         byteBuf.m_130130_(recipe.ingredients.size());
         Iterator var3 = recipe.ingredients.iterator();

         while(var3.hasNext()) {
            PointBlankIngredient ingredient = (PointBlankIngredient)var3.next();
            ingredient.toNetwork(byteBuf);
         }

         byteBuf.m_130055_(recipe.result);
      }
   }
}
