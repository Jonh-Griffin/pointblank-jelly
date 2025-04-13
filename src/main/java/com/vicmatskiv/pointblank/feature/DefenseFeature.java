package com.vicmatskiv.pointblank.feature;

import groovy.lang.Script;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public class DefenseFeature extends ConditionalFeature {

    public DefenseFeature(FeatureProvider owner, Predicate<ConditionContext> predicate) {
        super(owner, predicate);
    }

    @Override
    public @Nullable Script getScript() {
        return null;
    }
}
