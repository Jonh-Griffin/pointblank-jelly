package com.vicmatskiv.pointblank.feature;

import com.google.gson.JsonObject;
import com.vicmatskiv.pointblank.util.Conditions;
import com.vicmatskiv.pointblank.util.JsonUtil;
import groovy.lang.Script;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

public class DefenseFeature extends ConditionalFeature {
    private final float defenseModifier;
    private final Script script;
    private final int defense;

    public DefenseFeature(FeatureProvider owner, Predicate<ConditionContext> predicate, float defenseModifier, int defense, @Nullable Script script) {
        super(owner, predicate);
        this.defenseModifier = defenseModifier;
        this.script = script;
        this.defense = defense;
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

        for(Features.EnabledFeature enabledFeature : enabledDefenseFeatures) {
            DefenseFeature defenseFeature = (DefenseFeature) enabledFeature.feature();
            if(defenseFeature.hasScript() && defenseFeature.hasFunction("addDefense"))
                defenseModifier += (int)defenseFeature.invokeFunction("addDefense", itemStack, defenseFeature);
            if(defenseFeature.hasScript() && defenseFeature.hasFunction("getDefense"))
                defenseModifier += (int)defenseFeature.invokeFunction("getDefense", itemStack, defenseFeature);
            else
                defenseModifier += defenseFeature.getDefenseAdditive();
        }

        return defenseModifier;
    }

    public float getDefenseModifier() {
        return defenseModifier;
    }

    public int getDefenseAdditive() {
        return defense;
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
            this.withScript(JsonUtil.getJsonScript(obj));
            return this;
        }

        public Builder withDefense(int defenseAdditive) {
            this.defense = defenseAdditive;
            return this;
        }

        public DefenseFeature build(FeatureProvider featureProvider) {
            return new DefenseFeature(featureProvider, this.condition, this.defenseModifier, this.defense, script);
        }
    }
}
