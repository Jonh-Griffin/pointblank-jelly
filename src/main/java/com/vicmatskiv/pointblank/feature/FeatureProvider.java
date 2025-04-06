package com.vicmatskiv.pointblank.feature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
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
      List<Component> tooltipLines = new ArrayList();
      Iterator var2 = this.getFeatures().iterator();

      while(var2.hasNext()) {
         Feature feature = (Feature)var2.next();
         MutableComponent featureDescription = feature.getDescription().m_6881_().m_130940_(ChatFormatting.RED).m_130940_(ChatFormatting.ITALIC);
         if (featureDescription.m_214077_() != ComponentContents.f_237124_) {
            tooltipLines.add(featureDescription);
         }
      }

      return tooltipLines;
   }
}
