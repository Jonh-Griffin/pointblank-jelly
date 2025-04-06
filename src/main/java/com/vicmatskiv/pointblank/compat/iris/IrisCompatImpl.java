package com.vicmatskiv.pointblank.compat.iris;

import com.vicmatskiv.pointblank.Config;
import com.vicmatskiv.pointblank.client.render.RenderTypeProvider;
import net.irisshaders.iris.Iris;

class IrisCompatImpl extends IrisCompat {
   private boolean isRenderingShadows;
   private RenderTypeProvider renderTypeProvider = new IrisRenderTypeProvider();

   protected IrisCompatImpl() {
   }

   public boolean isIrisLoaded() {
      return true;
   }

   public boolean isShaderPackEnabled() {
      return !Iris.getCurrentPack().isEmpty();
   }

   public void onStartRenderShadows() {
      this.isRenderingShadows = true;
   }

   public void onEndRenderShadows() {
      this.isRenderingShadows = false;
   }

   public boolean isRenderingShadows() {
      return this.isRenderingShadows;
   }

   public RenderTypeProvider getRenderTypeProvider() {
      return this.renderTypeProvider;
   }

   public int getColorBalance() {
      int colorBalance = Config.pipScopeColorBalanceRed << 24 | Config.pipScopeColorBalanceGreen << 16 | Config.pipScopeColorBalanceBlue << 8 | 255;
      return colorBalance;
   }
}
