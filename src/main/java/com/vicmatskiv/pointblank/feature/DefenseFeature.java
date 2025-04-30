package com.vicmatskiv.pointblank.feature;

import com.google.gson.JsonObject;
import com.vicmatskiv.pointblank.util.Conditions;
import com.vicmatskiv.pointblank.util.JsonUtil;
import groovy.lang.Script;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

public class DefenseFeature extends ConditionalFeature {
    private final float defenseModifier;
    private final Script script;
    private final int defense;
    private final float toughnessModifier;
    private final float toughness;

    public DefenseFeature(FeatureProvider owner, Predicate<ConditionContext> predicate, float defenseModifier, int defense, float toughness, float toughnessModifier, @Nullable Script script) {
        super(owner, predicate);
        this.defenseModifier = defenseModifier;
        this.script = script;
        this.defense = defense;
        this.toughnessModifier = toughnessModifier;
        this.toughness = toughness;
    }

    public static float getToughnessModifier(ItemStack itemStack) {
        List<Features.EnabledFeature> enabledDefenseFeatures = Features.getEnabledFeatures(itemStack, DefenseFeature.class);
        float toughnessModifier = 1.0F;

        for(Features.EnabledFeature enabledFeature : enabledDefenseFeatures) {
            DefenseFeature defenseFeature = (DefenseFeature) enabledFeature.feature();
            if(defenseFeature.hasScript() && defenseFeature.hasFunction("addToughnessModifier"))
                toughnessModifier *= (float)defenseFeature.invokeFunction("addToughnessModifier", itemStack, defenseFeature);
            if(defenseFeature.hasScript() && defenseFeature.hasFunction("getToughnessModifier"))
                toughnessModifier *= (float)defenseFeature.invokeFunction("getToughnessModifier", itemStack, defenseFeature);
            else
                toughnessModifier *= defenseFeature.getToughnessModifier();
        }

        return Mth.clamp(toughnessModifier, 0.01F, 10.0F);
    }

    public static float getToughnessAdditive(ItemStack itemStack) {
        List<Features.EnabledFeature> enabledDefenseFeatures = Features.getEnabledFeatures(itemStack, DefenseFeature.class);
        float toughness = 0.0f;

        for(Features.EnabledFeature enabledFeature : enabledDefenseFeatures) {
            DefenseFeature defenseFeature = (DefenseFeature) enabledFeature.feature();
            if(defenseFeature.hasScript() && defenseFeature.hasFunction("addToughness"))
                toughness += (float) defenseFeature.invokeFunction("addToughness", itemStack, defenseFeature);
            if(defenseFeature.hasScript() && defenseFeature.hasFunction("getToughness"))
                toughness += (float) defenseFeature.invokeFunction("getToughness", itemStack, defenseFeature);
            else
                toughness += defenseFeature.getToughnessAdditive();
        }

        return toughness;
    }

    public static float getDefenseModifier(ItemStack itemStack) {
        List<Features.EnabledFeature> enabledDefenseFeatures = Features.getEnabledFeatures(itemStack, DefenseFeature.class);
        float defenseModifier = 1.0F;

        for(Features.EnabledFeature enabledFeature : enabledDefenseFeatures) {
            DefenseFeature defenseFeature = (DefenseFeature) enabledFeature.feature();
            if(defenseFeature.hasScript() && defenseFeature.hasFunction("addDefenseModifier"))
                defenseModifier *= (float)defenseFeature.invokeFunction("addDefenseModifier", itemStack, defenseFeature);
            if(defenseFeature.hasScript() && defenseFeature.hasFunction("getDefenseModifier"))
                defenseModifier *= (float)defenseFeature.invokeFunction("getDefenseModifier", itemStack, defenseFeature);
            else
                defenseModifier *= defenseFeature.getDefenseModifier();
        }

        return Mth.clamp(defenseModifier, 0.01F, 10.0F);
    }

    public static int getDefenseAdditive(ItemStack itemStack) {
        List<Features.EnabledFeature> enabledDefenseFeatures = Features.getEnabledFeatures(itemStack, DefenseFeature.class);
        int defenseModifier = 0;

        for (Features.EnabledFeature enabledFeature : enabledDefenseFeatures) {
            DefenseFeature defenseFeature = (DefenseFeature) enabledFeature.feature();
            if (defenseFeature.hasScript() && defenseFeature.hasFunction("addDefense"))
                defenseModifier += (int) defenseFeature.invokeFunction("addDefense", itemStack, defenseFeature);
            if (defenseFeature.hasScript() && defenseFeature.hasFunction("getDefense"))
                defenseModifier += (int) defenseFeature.invokeFunction("getDefense", itemStack, defenseFeature);
            else
                defenseModifier += defenseFeature.getDefenseAdditive();
        }

        return defenseModifier;
    }

    @Override
    public Component getDescription() {
        return this.defenseModifier < (double)1.0F ? Component.translatable("description.pointblank.reducesDefense").append(getDefenseComponent()).append(getToughnessComponent()) : Component.translatable("description.pointblank.increasesDefense").append(getDefenseComponent());
    }

    private MutableComponent getToughnessComponent() {
        MutableComponent start = this.toughnessModifier < 1.0f ? Component.translatable("description.pointblank.reducesToughness") : Component.translatable("description.pointblank.increasesToughness");
        start.append(String.format(" %.0f%%", (double)100.0F * ((double)1.0F - this.toughnessModifier)))
                .append("\n")
                .append(Component.literal("description.pointblank.toughness").append(" +%s".formatted(this.toughness)));
        return start;
    }

    public MutableComponent getDefenseComponent() {
        return Component.literal(String.format(" %.0f%%", (double)100.0F * ((double)1.0F - this.defenseModifier)))
                .append("\n")
                .append(Component.literal("description.pointblank.defense").append(" +%s".formatted(this.defense)))
                .append("\n");
    }

    public float getDefenseModifier() {
        return defenseModifier;
    }

    public int getDefenseAdditive() {
        return defense;
    }

    public float getToughnessModifier() {
        return toughnessModifier;
    }

    public float getToughnessAdditive() {
        return toughness;
    }

    @Override
    public @Nullable Script getScript() {
        return script;
    }

    public static class Builder implements FeatureBuilder<Builder, DefenseFeature> {
        private Predicate<ConditionContext> condition = (ctx) -> true;
        private float defenseModifier;
        private Script script;
        private int defense;
        private float toughness;
        private float toughnessModifier;

        public Builder() {
        }

        public Builder withCondition(Predicate<ConditionContext> condition) {
            this.condition = condition;
            return this;
        }

        public Builder withDefenseModifier(double defenseModifier) {
            this.defenseModifier = (float)defenseModifier;
            return this;
        }
        public Builder withScript(Script script) {
            this.script = script;
            return this;
        }
        public Builder withJsonObject(JsonObject obj) {
            if (obj.has("condition")) {
                this.withCondition(Conditions.fromJson(obj.getAsJsonObject("condition")));
            }

            this.withDefenseModifier(JsonUtil.getJsonFloat(obj, "defenseModifier", 1.0F));
            this.withDefense(JsonUtil.getJsonInt(obj, "defenseAdditive", 0));
            this.withToughnessModifier(JsonUtil.getJsonFloat(obj, "toughnessModifier", 1.0f));
            this.withToughness(JsonUtil.getJsonFloat(obj, "toughnessAdditive", 0f));
            this.withScript(JsonUtil.getJsonScript(obj));
            return this;
        }

        public Builder withDefense(int defenseAdditive) {
            this.defense = defenseAdditive;
            return this;
        }

        public Builder withToughness(float toughness) {
            this.toughness = toughness;
            return this;
        }

        public Builder withToughnessModifier(float toughnessModifier) {
            this.toughnessModifier = toughnessModifier;
            return this;
        }

        public DefenseFeature build(FeatureProvider featureProvider) {
            return new DefenseFeature(featureProvider, this.condition, this.defenseModifier, this.defense, toughness, toughnessModifier, script);
        }
    }
}
