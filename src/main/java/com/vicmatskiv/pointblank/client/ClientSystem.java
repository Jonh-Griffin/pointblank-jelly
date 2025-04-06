package com.vicmatskiv.pointblank.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.vicmatskiv.pointblank.Config;
import com.vicmatskiv.pointblank.client.render.AuxLevelRenderer;
import com.vicmatskiv.pointblank.compat.playeranimator.PlayerAnimatorCompat;
import com.vicmatskiv.pointblank.feature.PipFeature;
import com.vicmatskiv.pointblank.item.GunItem;
import com.vicmatskiv.pointblank.util.MiscUtil;
import com.vicmatskiv.pointblank.util.ReloadableMemoize;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.util.ClientUtils;

public class ClientSystem {
   private static ClientSystem instance;
   private AuxLevelRenderer auxLevelRenderer;
   private ShaderInstance texColorShaderInstance;
   private ShaderInstance colorTexLightmapShaderInstance;
   private List<ResourceManagerReloadListener> resourceManagerReloadListeners;

   public static ClientSystem getInstance() {
      if (RenderSystem.isOnRenderThreadOrInit()) {
         if (instance == null) {
            instance = new ClientSystem();
         }
      } else {
         new IllegalStateException("getInstance() called from wrong thread");
      }

      return instance;
   }

   private ClientSystem() {
      Minecraft mc = Minecraft.m_91087_();
      RenderTarget mainRenderTarget = mc.m_91385_();
      this.auxLevelRenderer = new AuxLevelRenderer(mainRenderTarget.f_83915_, mainRenderTarget.f_83916_);
      String texColorShaderName = "pointblank_position_tex_color";

      try {
         this.texColorShaderInstance = new ShaderInstance(mc.m_91098_(), texColorShaderName, DefaultVertexFormat.f_85819_);
      } catch (Exception var8) {
         throw new IllegalStateException("could not preload shader " + texColorShaderName, var8);
      }

      String colorTexLightmapShaderName = "pointblank_position_color_tex_lightmap";

      try {
         this.colorTexLightmapShaderInstance = new ShaderInstance(mc.m_91098_(), colorTexLightmapShaderName, DefaultVertexFormat.f_85820_);
      } catch (Exception var7) {
         throw new IllegalStateException("could not preload shader " + colorTexLightmapShaderName, var7);
      }

      this.resourceManagerReloadListeners = new ArrayList();
      ResourceManager var6 = Minecraft.m_91087_().m_91098_();
      if (var6 instanceof ReloadableResourceManager) {
         ReloadableResourceManager rrm = (ReloadableResourceManager)var6;
         rrm.m_7217_(this.resourceManagerReloadListener());
      }

      this.addReloadListener(PlayerAnimatorCompat.getInstance());
   }

   private ResourceManagerReloadListener resourceManagerReloadListener() {
      return this::onResourceManagerReload;
   }

   private void onResourceManagerReload(ResourceManager resourceManager) {
      Iterator var2 = this.resourceManagerReloadListeners.iterator();

      while(var2.hasNext()) {
         ResourceManagerReloadListener reloadListener = (ResourceManagerReloadListener)var2.next();
         reloadListener.m_6213_(resourceManager);
      }

   }

   public void addReloadListener(ResourceManagerReloadListener listener) {
      this.resourceManagerReloadListeners.add(listener);
   }

   public void removeReloadListener(ResourceManagerReloadListener listener) {
      this.resourceManagerReloadListeners.remove(listener);
   }

   public <T, U, R> BiFunction<T, U, R> createReloadableMemoize(BiFunction<T, U, R> f) {
      ReloadableMemoize<T, U, R> reloadable = new ReloadableMemoize(f);
      this.resourceManagerReloadListeners.add(reloadable);
      return reloadable.getMemoizedFunction();
   }

   public AuxLevelRenderer getAuxLevelRenderer() {
      return this.auxLevelRenderer;
   }

   public ShaderInstance getTexColorShaderInstance() {
      return Config.customShadersEnabled ? this.texColorShaderInstance : GameRenderer.m_172820_();
   }

   public ShaderInstance getColorTexLightmapShaderInstance() {
      return Config.customShadersEnabled ? this.colorTexLightmapShaderInstance : GameRenderer.m_172835_();
   }

   public void renderAux(GunClientState state, float partialTick, long time) {
      Minecraft mc = Minecraft.m_91087_();
      Optional<Float> pipZoom = PipFeature.getZoom(mc.f_91074_.m_21205_());
      if (pipZoom.isPresent() && state.isAiming()) {
         getInstance().getAuxLevelRenderer().renderToTarget(partialTick, time, (Float)pipZoom.get());
      }

   }

   public static double modifyMouseSensitivity(double originalValue) {
      GunClientState state = GunClientState.getMainHeldState();
      ItemStack mainHeldItem = ClientUtils.getClientPlayer().m_21205_();
      if (mainHeldItem != null) {
         Item var5 = mainHeldItem.m_41720_();
         if (var5 instanceof GunItem) {
            GunItem gunItem = (GunItem)var5;
            if (state != null && state.isAiming() && (gunItem.getScopeOverlay() != null || MiscUtil.isGreaterThanZero(gunItem.getPipScopeZoom()) || (Boolean)ClientEventHandler.runSyncCompute(() -> {
               return !PipFeature.getZoom(mainHeldItem).isEmpty();
            }))) {
               return originalValue * Config.scopeAimingMouseSensitivity;
            }
         }
      }

      return originalValue;
   }
}
