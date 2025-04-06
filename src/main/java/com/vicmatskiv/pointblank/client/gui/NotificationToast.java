package com.vicmatskiv.pointblank.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.gui.components.toasts.Toast.Visibility;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class NotificationToast implements Toast {
   public static final ResourceLocation BUTTON_RESOURCE = new ResourceLocation("pointblank", "textures/gui/buttons.png");
   private Component title;
   private long lastChanged;
   private boolean changed;
   private final int width;
   private long displayTime;

   public NotificationToast(Component title, long displayTime) {
      this.title = title;
      Minecraft mc = Minecraft.m_91087_();
      this.width = Math.max(90, 30 + mc.f_91062_.m_92852_(title));
      this.displayTime = displayTime;
   }

   public int m_7828_() {
      return this.width;
   }

   public int m_94899_() {
      return 26;
   }

   public Visibility m_7172_(GuiGraphics guiGraphics, ToastComponent toastComponent, long currentTime) {
      if (this.changed) {
         this.lastChanged = currentTime;
         this.changed = false;
      }

      int textureWidth = 160;
      int textureHeight = 32;
      guiGraphics.m_280027_(BUTTON_RESOURCE, 0, 0, this.m_7828_(), this.m_94899_(), 18, 4, textureWidth, textureHeight, 0, 0);
      Minecraft minecraft = toastComponent.m_94929_();
      guiGraphics.m_280614_(minecraft.f_91062_, this.title, 18, 9, -256, false);
      return (double)(currentTime - this.lastChanged) < (double)this.displayTime * toastComponent.m_264542_() ? Visibility.SHOW : Visibility.HIDE;
   }
}
