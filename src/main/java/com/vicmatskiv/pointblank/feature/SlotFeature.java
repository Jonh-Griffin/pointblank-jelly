package com.vicmatskiv.pointblank.feature;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import com.vicmatskiv.pointblank.util.Conditions;
import com.vicmatskiv.pointblank.util.JsonUtil;
import groovy.lang.Script;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class SlotFeature extends ConditionalFeature {
    public int weight = 64;
    @Nullable
    public List<Either<Either<Item, TagKey<Item>>, Class<? extends Item>>> whitelist = null;

    public final Script script;

    public SlotFeature(FeatureProvider owner, Predicate<ConditionContext> predicate, int weight, List<Either<Either<Item, TagKey<Item>>, Class<? extends Item>>> whitelist, Script script) {
        super(owner, predicate);
        this.script = script;
        this.weight = weight;
        this.whitelist = whitelist;
    }

    public static int getWeight(ItemStack itemStack, List<Either<Either<Item, TagKey<Item>>, Class<? extends Item>>> whitelist) {
        if (whitelist == null) return 64 / itemStack.getCount();
        for (Either<Either<Item, TagKey<Item>>, Class<? extends Item>> entry : whitelist) {
            if (entry.left().isPresent()) {
                Either<Item, TagKey<Item>> itemOrTag = entry.left().get();
                if (itemOrTag.left().isPresent()) {
                    Item item = itemOrTag.left().get();
                    if (item == itemStack.getItem()) return 64 / itemStack.getCount();
                } else {
                    TagKey<Item> tag = itemOrTag.right().get();
                    if (itemStack.is(tag)) return 64 / itemStack.getCount();
                }
            } else {
                Class<? extends Item> itemClass = entry.right().get();
                if (itemClass.isInstance(itemStack.getItem())) return 64 / itemStack.getCount();
            }
        }
        return -1;
    }

    public @Nullable Script getScript() {
        return script;
    }

    public static class Builder implements FeatureBuilder<Builder, SlotFeature> {
        public int maxStackSize = 64;
        public Predicate<ConditionContext> predicate = (ctx) -> true;
        public Script script = null;
        public List<Either<Either<Item, TagKey<Item>>, Class<? extends Item>>> whitelist;

        public Builder withMaxStackSize(int maxStackSize) {
            this.maxStackSize = maxStackSize;
            return this;
        }

        public Builder withPredicate(Predicate<ConditionContext> predicate) {
            this.predicate = predicate;
            return this;
        }

        public Builder withScript(Script script) {
            this.script = script;
            return this;
        }

        public Builder withWhitelist(JsonArray whitelist) {
            this.whitelist = new ArrayList<>();
            for (JsonElement jsonElement : whitelist) {
                String element = jsonElement.getAsString();
                if(element.startsWith("class")) {
                    String className = element.substring(6);
                    try {
                        Class<? extends Item> itemClass = (Class<? extends Item>) Class.forName(className);
                        this.whitelist.add(Either.right(itemClass));
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException("Class not found: " + className + " | a Pointblank Slot feature has malformed whitelist!", e);
                    }
                    continue;
                }
                String itemName = element;
                if(itemName.startsWith("#")) {
                    String tagName = itemName.substring(1);
                    TagKey<Item> tagKey = TagKey.create(ForgeRegistries.ITEMS.getRegistryKey(), new ResourceLocation(tagName));
                    this.whitelist.add(Either.left(Either.right(tagKey)));
                    continue;
                }

                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemName));
                if (item != null)
                    this.whitelist.add(Either.left(Either.left(item)));
                else
                    throw new RuntimeException("Item not found: " + itemName + " | a Pointblank Slot feature has malformed whitelist!");
            }
            return this;
        }


        @Override
        public Builder withJsonObject(JsonObject var1) {
            withScript(JsonUtil.getJsonScript(var1));
            withMaxStackSize(JsonUtil.getJsonInt(var1, "weight", 64));
            if (var1.has("condition"))
                this.withPredicate(Conditions.fromJson(var1.getAsJsonObject("condition")));
            if(var1.has("whitelist"))
                this.withWhitelist(var1.getAsJsonArray("whitelist"));
            return this;
        }

        @Override
        public SlotFeature build(FeatureProvider var1) {
            return new SlotFeature(var1, predicate, maxStackSize, whitelist, script);
        }
    }
}
