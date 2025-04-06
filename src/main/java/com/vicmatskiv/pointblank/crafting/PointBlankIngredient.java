package com.vicmatskiv.pointblank.crafting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.vicmatskiv.pointblank.util.JsonUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public interface PointBlankIngredient {
   List<ItemStack> getItemStacks();

   JsonElement toJson();

   int getCount();

   default boolean matches(ItemStack itemStack) {
      return itemStack == null ? false : this.getItemStacks().stream().anyMatch((ingredientStack) -> {
         return ingredientStack.m_150930_(itemStack.m_41720_());
      });
   }

   void toNetwork(FriendlyByteBuf var1);

   static PointBlankIngredient of(TagKey<Item> tagKey, int count) {
      return new TagIngredient(tagKey, count);
   }

   static PointBlankIngredient of(ItemLike itemLike, int count) {
      return new ItemIngredient(new ItemStack(itemLike, count));
   }

   static PointBlankIngredient fromNetwork(FriendlyByteBuf byteBuf) {
      boolean isItem = byteBuf.readBoolean();
      return isItem ? ItemIngredient.fromNetwork(byteBuf) : TagIngredient.fromNetwork(byteBuf);
   }

   static PointBlankIngredient fromJson(JsonElement jsonElement) {
      if (jsonElement != null && !jsonElement.isJsonNull() && jsonElement.isJsonObject()) {
         JsonObject jsonObject = jsonElement.getAsJsonObject();
         if (jsonObject.has("item") && jsonObject.has("tag")) {
            throw new JsonParseException("An ingredient entry is either a tag or an item, not both");
         } else if (jsonObject.has("item")) {
            return ItemIngredient.fromJson(jsonObject);
         } else if (jsonObject.has("tag")) {
            return TagIngredient.fromJson(jsonObject);
         } else {
            throw new JsonParseException("An ingredient entry needs either a tag or an item");
         }
      } else {
         throw new JsonSyntaxException("Invalid json ingredient element: " + jsonElement);
      }
   }

   public static class TagIngredient implements PointBlankIngredient {
      private TagKey<Item> tagKey;
      private int count;
      private List<ItemStack> itemStacks;

      private TagIngredient(TagKey<Item> tagKey, int count) {
         this.tagKey = tagKey;
         this.count = count;
      }

      public int getCount() {
         return this.count;
      }

      public List<ItemStack> getItemStacks() {
         if (this.itemStacks == null) {
            List<ItemStack> itemStacks = new ArrayList();
            Iterator var2 = BuiltInRegistries.f_257033_.m_206058_(this.tagKey).iterator();

            while(var2.hasNext()) {
               Holder<Item> holder = (Holder)var2.next();
               itemStacks.add(new ItemStack((ItemLike)holder.m_203334_(), this.count));
            }

            this.itemStacks = Collections.unmodifiableList(itemStacks);
         }

         return this.itemStacks;
      }

      public JsonElement toJson() {
         JsonObject jsonObject = new JsonObject();
         jsonObject.addProperty("tag", this.tagKey.f_203868_().toString());
         jsonObject.addProperty("count", this.count);
         return jsonObject;
      }

      public static PointBlankIngredient fromJson(JsonObject jsonObject) {
         String tagResourceLocation = JsonUtil.getJsonString(jsonObject, "tag", (String)null);
         if (tagResourceLocation == null) {
            throw new JsonSyntaxException("Missing ingredient tag");
         } else {
            TagKey<Item> tagKey = TagKey.m_203882_(Registries.f_256913_, new ResourceLocation(tagResourceLocation));
            int count = JsonUtil.getJsonInt(jsonObject, "count", 1);
            return new TagIngredient(tagKey, count);
         }
      }

      public final void toNetwork(FriendlyByteBuf byteBuf) {
         byteBuf.writeBoolean(false);
         byteBuf.m_130085_(this.tagKey.f_203868_());
         byteBuf.writeInt(this.count);
      }

      public static PointBlankIngredient fromNetwork(FriendlyByteBuf byteBuf) {
         TagKey<Item> tagKey = TagKey.m_203882_(Registries.f_256913_, byteBuf.m_130281_());
         int count = byteBuf.readInt();
         return new TagIngredient(tagKey, count);
      }
   }

   public static class ItemIngredient implements PointBlankIngredient {
      private ItemStack itemStack;

      public ItemIngredient(ItemStack itemStack) {
         this.itemStack = itemStack;
      }

      public int getCount() {
         return this.itemStack.m_41613_();
      }

      public List<ItemStack> getItemStacks() {
         return Collections.singletonList(this.itemStack);
      }

      public JsonElement toJson() {
         JsonObject jsonObject = new JsonObject();
         jsonObject.addProperty("item", BuiltInRegistries.f_257033_.m_7981_(this.itemStack.m_41720_()).toString());
         jsonObject.addProperty("count", this.itemStack.m_41613_());
         return jsonObject;
      }

      public static PointBlankIngredient fromJson(JsonObject jsonObject) {
         String itemLocation = JsonUtil.getJsonString(jsonObject, "item", (String)null);
         if (itemLocation == null) {
            throw new JsonSyntaxException("Missing ingredient tag");
         } else {
            Item item = (Item)BuiltInRegistries.f_257033_.m_7745_(new ResourceLocation(itemLocation));
            int count = JsonUtil.getJsonInt(jsonObject, "count", 1);
            return new ItemIngredient(new ItemStack(item, count));
         }
      }

      public final void toNetwork(FriendlyByteBuf byteBuf) {
         byteBuf.writeBoolean(true);
         byteBuf.m_130055_(this.itemStack);
      }

      public static PointBlankIngredient fromNetwork(FriendlyByteBuf byteBuf) {
         return new ItemIngredient(byteBuf.m_130267_());
      }
   }
}
