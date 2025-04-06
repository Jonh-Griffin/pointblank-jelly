package com.vicmatskiv.pointblank.client.render;

import com.vicmatskiv.pointblank.item.AmmoItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class DefaultModelRenderer extends GeoItemRenderer<AmmoItem> {
   public DefaultModelRenderer(String resourceName) {
      super(new DefaultedItemGeoModel<>(new ResourceLocation("pointblank", resourceName)));
   }
}
