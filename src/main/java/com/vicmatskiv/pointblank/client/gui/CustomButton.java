package com.vicmatskiv.pointblank.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.vicmatskiv.pointblank.util.Interpolators;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CustomButton extends AbstractButton {
   public static final ResourceLocation BUTTON_RESOURCE = new ResourceLocation("pointblank", "textures/gui/buttons.png");
   public static final int SMALL_WIDTH = 120;
   public static final int DEFAULT_WIDTH = 150;
   public static final int DEFAULT_HEIGHT = 20;
   protected static final CreateNarration DEFAULT_NARRATION = (p_253298_) -> {
      return (MutableComponent)p_253298_.get();
   };
   private final Event onPress;
   private Event onRelease;
   protected final CreateNarration createNarration;
   private boolean isPressed;
   private Interpolators.FloatProvider progressProvider;

   public static Builder builder(Component title, Event onPress) {
      return new Builder(title, onPress);
   }

   protected CustomButton(int x, int y, int width, int height, Component title, Event onPress, Event onRelease, CreateNarration narration, Interpolators.FloatProvider progressProvider) {
      super(x, y, width, height, title);
      this.onPress = onPress;
      this.onRelease = onRelease;
      this.createNarration = narration;
      this.progressProvider = progressProvider;
   }

   protected CustomButton(Builder builder) {
      this(builder.x, builder.y, builder.width, builder.height, builder.message, builder.onPress, builder.onRelease, builder.createNarration, builder.progressProvider);
      this.m_257544_(builder.tooltip);
   }

   public boolean isPressed() {
      return this.isPressed;
   }

   public void m_5691_() {
      this.onPress.handle(this);
   }

   public void m_5716_(double mouseX, double mouseY) {
      this.isPressed = true;
      super.m_5716_(mouseX, mouseY);
   }

   public void m_7691_(double mouseX, double mouseY) {
      super.m_7691_(mouseX, mouseY);
      this.release();
   }

   protected void m_87963_(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      Minecraft minecraft = Minecraft.m_91087_();
      guiGraphics.m_280246_(1.0F, 1.0F, 1.0F, this.f_93625_);
      RenderSystem.enableBlend();
      RenderSystem.enableDepthTest();
      int textureWidth = 48;
      int textureHeight = 20;
      int vOffset = 32;
      if (!this.f_93623_) {
         vOffset += 40;
      } else if (this.isPressed) {
         vOffset += 20;
      } else if (this.m_198029_()) {
         vOffset += 60;
      }

      guiGraphics.m_280027_(BUTTON_RESOURCE, this.m_252754_(), this.m_252907_(), this.m_5711_(), this.m_93694_(), 20, 4, textureWidth, textureHeight, 0, vOffset);
      if (this.progressProvider != null) {
         float progress = this.progressProvider.getValue();
         int height = this.m_93694_() - 2;
         guiGraphics.m_280509_(this.m_252754_() + 1, this.m_252907_() + 1 + height - (int)((float)height * progress), this.m_252754_() + this.m_5711_() - 1, this.m_252907_() + 1 + height, -2147434496);
      }

      guiGraphics.m_280246_(1.0F, 1.0F, 1.0F, 1.0F);
      int i = this.getFGColor();
      m_280138_(guiGraphics, minecraft.f_91062_, this.m_6035_(), this.m_252754_() + 8, this.m_252907_(), this.m_252754_() + this.m_5711_() - 8, this.m_252907_() + this.m_93694_(), i | Mth.m_14167_(this.f_93625_ * 255.0F) << 24);
   }

   public void m_280139_(GuiGraphics guiGraphics, Font font, int color) {
      this.m_280372_(guiGraphics, font, 2, color);
   }

   public int getFGColor() {
      return super.getFGColor();
   }

   protected MutableComponent m_5646_() {
      return this.createNarration.createNarrationMessage(() -> {
         return super.m_5646_();
      });
   }

   public void m_168797_(NarrationElementOutput p_259196_) {
      this.m_168802_(p_259196_);
   }

   public void release() {
      if (this.isPressed && this.onRelease != null) {
         this.onRelease.handle(this);
      }

      this.isPressed = false;
   }

   @OnlyIn(Dist.CLIENT)
   public static class Builder {
      private final Component message;
      private final Event onPress;
      private Event onRelease;
      private Interpolators.FloatProvider progressProvider;
      @Nullable
      private Tooltip tooltip;
      private int x;
      private int y;
      private int width = 150;
      private int height = 20;
      private CreateNarration createNarration;

      public Builder(Component p_254097_, Event onPress) {
         this.createNarration = CustomButton.DEFAULT_NARRATION;
         this.message = p_254097_;
         this.onPress = onPress;
      }

      public Builder progressProvider(Interpolators.FloatProvider progressProvider) {
         this.progressProvider = progressProvider;
         return this;
      }

      public Builder onRelease(Event onRelease) {
         this.onRelease = onRelease;
         return this;
      }

      public Builder pos(int posX, int posY) {
         this.x = posX;
         this.y = posY;
         return this;
      }

      public Builder width(int width) {
         this.width = width;
         return this;
      }

      public Builder size(int width, int height) {
         this.width = width;
         this.height = height;
         return this;
      }

      public Builder bounds(int posX, int posY, int width, int height) {
         return this.pos(posX, posY).size(width, height);
      }

      public Builder tooltip(@Nullable Tooltip tooltip) {
         this.tooltip = tooltip;
         return this;
      }

      public Builder createNarration(CreateNarration p_253638_) {
         this.createNarration = p_253638_;
         return this;
      }

      public CustomButton build() {
         return this.build(CustomButton::new);
      }

      public CustomButton build(Function<Builder, CustomButton> builder) {
         return (CustomButton)builder.apply(this);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public interface Event {
      void handle(CustomButton var1);
   }

   @OnlyIn(Dist.CLIENT)
   public interface CreateNarration {
      MutableComponent createNarrationMessage(Supplier<MutableComponent> var1);
   }
}
