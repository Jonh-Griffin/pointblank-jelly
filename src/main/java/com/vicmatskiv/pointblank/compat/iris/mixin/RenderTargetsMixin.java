package com.vicmatskiv.pointblank.compat.iris.mixin;

import com.vicmatskiv.pointblank.compat.iris.RenderTargetsExt;
import net.irisshaders.iris.targets.RenderTargets;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin({RenderTargets.class})
public class RenderTargetsMixin implements RenderTargetsExt {
   @Shadow
   private boolean fullClearRequired;

   public void setPointblankRenderFullClearRequired(boolean fullClearRequired) {
      this.fullClearRequired = fullClearRequired;
   }
}
