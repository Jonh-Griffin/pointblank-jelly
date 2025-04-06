package com.vicmatskiv.pointblank.crafting;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.advancements.RequirementsStrategy;
import net.minecraft.advancements.Advancement.Builder;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;

public class PointBlankRecipeBuilder implements RecipeBuilder {
   private final Item result;
   private final int count;
   private final List<PointBlankIngredient> ingredients = Lists.newArrayList();
   private final Builder advancement = Builder.m_285878_();
   @Nullable
   private String group;
   private RecipeSerializer<?> serializer;
   private RecipeCategory category;

   public PointBlankRecipeBuilder(RecipeSerializer<?> serializer, ItemLike item, int count) {
      this.category = RecipeCategory.COMBAT;
      this.serializer = serializer;
      this.result = item.m_5456_();
      this.count = count;
   }

   public PointBlankRecipeBuilder requires(TagKey<Item> tagKey, int count) {
      return this.requires(PointBlankIngredient.of(tagKey, count));
   }

   public PointBlankRecipeBuilder requires(ItemLike item, int count) {
      return this.requires(PointBlankIngredient.of(item, count));
   }

   public PointBlankRecipeBuilder requires(PointBlankIngredient ingredient) {
      this.ingredients.add(ingredient);
      return this;
   }

   public PointBlankRecipeBuilder unlockedBy(String p_126197_, CriterionTriggerInstance p_126198_) {
      this.advancement.m_138386_(p_126197_, p_126198_);
      return this;
   }

   public PointBlankRecipeBuilder group(@Nullable String p_126195_) {
      this.group = p_126195_;
      return this;
   }

   public Item m_142372_() {
      return this.result;
   }

   public void m_126140_(Consumer<FinishedRecipe> consumer, ResourceLocation recipeId) {
      this.ensureValid(recipeId);
      this.advancement.m_138396_(f_236353_).m_138386_("has_the_recipe", RecipeUnlockedTrigger.m_63728_(recipeId)).m_138354_(net.minecraft.advancements.AdvancementRewards.Builder.m_10009_(recipeId)).m_138360_(RequirementsStrategy.f_15979_);
      consumer.accept(new Result(this.serializer, recipeId, this.result, this.count, this.group == null ? "" : this.group, this.ingredients, this.advancement, recipeId.m_246208_("recipes/" + this.category.m_247710_() + "/")));
   }

   private void ensureValid(ResourceLocation p_126208_) {
      if (this.advancement.m_138405_().isEmpty()) {
         throw new IllegalStateException("No way of obtaining recipe " + p_126208_);
      }
   }

   public static class Result implements FinishedRecipe {
      private final ResourceLocation id;
      private final Item result;
      private final int count;
      private final String group;
      private final List<PointBlankIngredient> ingredients;
      private final Builder advancement;
      private final ResourceLocation advancementId;
      private RecipeSerializer<?> serializer;

      public Result(RecipeSerializer<?> serializer, ResourceLocation recipeId, Item resultItem, int count, String group, List<PointBlankIngredient> ingredients, Builder advancementBuilder, ResourceLocation advancementId) {
         this.serializer = serializer;
         this.id = recipeId;
         this.result = resultItem;
         this.count = count;
         this.group = group;
         this.ingredients = ingredients;
         this.advancement = advancementBuilder;
         this.advancementId = advancementId;
      }

      public void m_7917_(JsonObject jsonObject) {
         if (!this.group.isEmpty()) {
            jsonObject.addProperty("group", this.group);
         }

         JsonArray jsonarray = new JsonArray();
         Iterator var3 = this.ingredients.iterator();

         while(var3.hasNext()) {
            PointBlankIngredient ingredient = (PointBlankIngredient)var3.next();
            jsonarray.add(ingredient.toJson());
         }

         jsonObject.add("ingredients", jsonarray);
         JsonObject jsonobject = new JsonObject();
         jsonobject.addProperty("item", BuiltInRegistries.f_257033_.m_7981_(this.result).toString());
         if (this.count > 1) {
            jsonobject.addProperty("count", this.count);
         }

         jsonObject.add("result", jsonobject);
      }

      public RecipeSerializer<?> m_6637_() {
         return this.serializer;
      }

      public ResourceLocation m_6445_() {
         return this.id;
      }

      @Nullable
      public JsonObject m_5860_() {
         return this.advancement.m_138400_();
      }

      @Nullable
      public ResourceLocation m_6448_() {
         return this.advancementId;
      }
   }
}
