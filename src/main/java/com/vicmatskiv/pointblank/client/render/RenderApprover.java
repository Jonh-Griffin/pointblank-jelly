package com.vicmatskiv.pointblank.client.render;

import com.vicmatskiv.pointblank.feature.Feature;
import com.vicmatskiv.pointblank.feature.Features;
import java.util.Iterator;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public interface RenderApprover {
   boolean canRenderPart(String var1);

   Class<? extends Feature> getFeatureType();

   default boolean approveRendering(RenderPass renderPass, String partName, ItemStack rootStack, ItemStack currentStack, String path, ItemDisplayContext itemDisplayContext) {
      if (renderPass != RenderPass.current()) {
         return true;
      } else if (!this.canRenderPart(partName)) {
         return false;
      } else {
         Class<? extends Feature> approvedFeatureType = this.getFeatureType();
         if (approvedFeatureType == null) {
            return true;
         } else {
            boolean isApproved = false;
            Iterator var9 = Features.getEnabledFeatures(rootStack, approvedFeatureType).iterator();

            while(var9.hasNext()) {
               Features.EnabledFeature enabledFeature = (Features.EnabledFeature)var9.next();
               if (enabledFeature.ownerPath().equals(path)) {
                  isApproved = true;
                  break;
               }
            }

            return isApproved;
         }
      }
   }
}
