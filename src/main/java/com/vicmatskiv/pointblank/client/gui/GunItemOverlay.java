package com.vicmatskiv.pointblank.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.vicmatskiv.pointblank.client.GunClientState;
import com.vicmatskiv.pointblank.item.FireModeInstance;
import com.vicmatskiv.pointblank.item.GunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class GunItemOverlay {
   private static final ResourceLocation OVERLAY_RESOURCE = new ResourceLocation("pointblank", "textures/gui/ammo.png");

   public static void renderGunOverlay(GuiGraphics guiGraphics, ItemStack stack) {
      Minecraft mc = Minecraft.m_91087_();
      int slotIndex = mc.f_91074_.m_150109_().f_35977_;
      GunClientState gunClientState = GunClientState.getState(mc.f_91074_, stack, slotIndex, false);
      if (gunClientState != null) {
         Component message = gunClientState.getCurrentMessage();
         int messageColor;
         int currentAmmo;
         if (message != null) {
            messageColor = 14548736;
         } else {
            FireModeInstance fireModeInstance = GunItem.getFireModeInstance(stack);
            currentAmmo = gunClientState.getAmmoCount(fireModeInstance);
            GunItem gunItem = (GunItem)stack.m_41720_();
            int maxAmmo = gunItem.getMaxAmmoCapacity(stack, fireModeInstance);
            if (maxAmmo == Integer.MAX_VALUE) {
               message = Component.m_237113_("∞");
            } else {
               message = Component.m_237113_(String.format("%d/%d", currentAmmo, maxAmmo));
            }

            messageColor = 16776960;
         }

         Font font = mc.f_91062_;
         currentAmmo = mc.m_91268_().m_85445_();
         guiGraphics.m_280614_(font, (Component)message, currentAmmo - font.m_92852_((FormattedText)message) - 10, 10, messageColor, false);
      }
   }

   public static void renderGunOverlay2(GuiGraphics guiGraphics, ItemStack stack) {
      int textureWidth = 160;
      int textureHeight = 32;
      RenderSystem.enableBlend();
      RenderSystem.disableDepthTest();
      Minecraft mc = Minecraft.m_91087_();
      FireModeInstance fireModeInstance = GunItem.getFireModeInstance(stack);
      if (fireModeInstance != null) {
         String fireModeDisplayName = fireModeInstance.getDisplayName().getString();
         int width = 9 + mc.f_91062_.m_92895_(fireModeDisplayName);
         int height = 22;
         int vOffset = mc.m_91268_().m_85446_() - height;
         int hOffset = (mc.m_91268_().m_85445_() >> 1) + 97;
         guiGraphics.m_280027_(OVERLAY_RESOURCE, hOffset, vOffset, width, height, 18, 4, textureWidth, textureHeight, 0, 0);
         guiGraphics.m_280056_(mc.f_91062_, fireModeDisplayName, hOffset + 5 + 1, vOffset + 7, 0, false);
         guiGraphics.m_280056_(mc.f_91062_, fireModeDisplayName, hOffset + 5 - 1, vOffset + 7, 0, false);
         guiGraphics.m_280056_(mc.f_91062_, fireModeDisplayName, hOffset + 5, vOffset + 7 + 1, 0, false);
         guiGraphics.m_280056_(mc.f_91062_, fireModeDisplayName, hOffset + 5, vOffset + 7 - 1, 0, false);
         guiGraphics.m_280056_(mc.f_91062_, fireModeDisplayName, hOffset + 5, vOffset + 7, 8040160, false);
         int slotIndex = mc.f_91074_.m_150109_().f_35977_;
         GunClientState gunClientState = GunClientState.getState(mc.f_91074_, stack, slotIndex, false);
         if (gunClientState != null) {
            int currentAmmo = gunClientState.getAmmoCount(fireModeInstance);
            GunItem gunItem = (GunItem)stack.m_41720_();
            int maxAmmo = gunItem.getMaxAmmoCapacity(stack, fireModeInstance);
            String counter;
            if (maxAmmo == Integer.MAX_VALUE) {
               counter = "∞";
            } else {
               counter = String.format("%d/%d", currentAmmo, maxAmmo);
            }

            Component message = gunClientState.getCurrentMessage();
            if (message != null) {
               counter = message.getString();
            }

            guiGraphics.m_280056_(mc.f_91062_, counter, hOffset + 5 + 1, vOffset - 5, 0, false);
            guiGraphics.m_280056_(mc.f_91062_, counter, hOffset + 5 - 1, vOffset - 5, 0, false);
            guiGraphics.m_280056_(mc.f_91062_, counter, hOffset + 5, vOffset - 5 + 1, 0, false);
            guiGraphics.m_280056_(mc.f_91062_, counter, hOffset + 5, vOffset - 5 - 1, 0, false);
            guiGraphics.m_280056_(mc.f_91062_, counter, hOffset + 5, vOffset - 5, -1, false);
         }
      }
   }
}
