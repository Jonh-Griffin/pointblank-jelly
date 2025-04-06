package com.vicmatskiv.pointblank.feature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;

public interface FeatureProvider {
   default <T extends Feature> T getFeature(Class<T> featureType) {
      return null;
   }

   default boolean hasFeature(Feature feature) {
      return this.getFeature(feature.getClass()) == feature;
   }

   Collection<Feature> getFeatures();

   default List<Component> getDescriptionTooltipLines() {
      return Collections.emptyList();
   }

   default List<Component> getFeatureTooltipLines() {
      List<Component> tooltipLines = new ArrayList<>();

      for(Feature feature : this.getFeatures()) {
         MutableComponent featureDescription = feature.getDescription().copy().withStyle(ChatFormatting.RED).withStyle(ChatFormatting.ITALIC);
         if (featureDescription.getContents() != ComponentContents.EMPTY) {
            tooltipLines.add(featureDescription);
         }
      }

      return tooltipLines;
   }
}
