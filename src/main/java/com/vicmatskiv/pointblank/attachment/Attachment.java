package com.vicmatskiv.pointblank.attachment;

import com.vicmatskiv.pointblank.Nameable;
import com.vicmatskiv.pointblank.feature.Feature;
import com.vicmatskiv.pointblank.feature.FeatureProvider;
import java.util.Set;
import net.minecraft.world.level.ItemLike;

public interface Attachment extends Nameable, ItemLike, FeatureProvider {
   AttachmentCategory getCategory();

   Set<String> getGroups();

   default void applyFeature(Class<Feature> featureType, Object context) {
   }
}
