package com.vicmatskiv.pointblank.client.gui;

import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Axis;
import com.vicmatskiv.pointblank.attachment.Attachment;
import com.vicmatskiv.pointblank.attachment.AttachmentHost;
import com.vicmatskiv.pointblank.event.AttachmentAddedEvent;
import com.vicmatskiv.pointblank.event.AttachmentRemovedEvent;
import com.vicmatskiv.pointblank.inventory.AttachmentContainerMenu;
import com.vicmatskiv.pointblank.inventory.AttachmentSlot;
import com.vicmatskiv.pointblank.inventory.HierarchicalSlot;
import com.vicmatskiv.pointblank.inventory.SimpleAttachmentContainer;
import com.vicmatskiv.pointblank.registry.SoundRegistry;
import com.vicmatskiv.pointblank.util.MiscUtil;
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.util.thread.EffectiveSide;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;
import software.bernie.geckolib.util.ClientUtils;

public class AttachmentManagerScreen extends AbstractContainerScreen<AttachmentContainerMenu> {
   private static final ResourceLocation GUI_TEXTURES = new ResourceLocation("pointblank", "textures/gui/attachments4.png");
   private static final ResourceLocation BACKGROUND_TEXTURE = new ResourceLocation("pointblank", "textures/gui/blueprint-background-2.png");
   protected static final ResourceLocation WIDGETS_LOCATION = new ResourceLocation("textures/gui/widgets.png");
   private static final Vector4f COLOR_GREEN = new Vector4f(0.0F, 1.0F, 0.0F, 1.0F);
   private final Inventory playerInventory;
   private MouseInteractionHandler mouseInteractionHandler;
   private final int inventoryWidth = 176;
   private final int inventoryHeight = 90;
   private final int slotWidth = 18;
   private final int slotHeight = 18;
   private final int slotRightPadding = 4;
   private int headerBottomPadding = 2;
   private AttachmentContainerMenu menu;
   private String selectedAttachmentPath;
   private Queue<AttachmentHighlightEvent> attachmentEventQueue = new ArrayDeque();

   public AttachmentManagerScreen(AttachmentContainerMenu menu, Inventory playerInventory, Component titleIn) {
      super(menu, playerInventory, titleIn);
      this.menu = menu;
      this.playerInventory = playerInventory;
      this.f_97727_ = 250;
      this.f_97726_ = 370;
      this.mouseInteractionHandler = new MouseInteractionHandler(this::isMouseInScreen, 0.5F, 2.0F, 0.1F);
      MinecraftForge.EVENT_BUS.register(this);
   }

   protected void m_7856_() {
      super.m_7856_();
   }

   public void m_181908_() {
      super.m_181908_();
      if (this.f_96541_ != null && this.f_96541_.f_91074_ != null) {
         ItemStack selectedStack = this.playerInventory.m_36056_();
         if (!(selectedStack.m_41720_() instanceof AttachmentHost)) {
            Minecraft.m_91087_().m_91152_((Screen)null);
         } else {
            AttachmentHighlightEvent event = (AttachmentHighlightEvent)this.attachmentEventQueue.peek();
            if (event != null && event.isExpired()) {
               this.attachmentEventQueue.poll();
            }
         }
      }

   }

   public void m_88315_(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
      PoseStack poseStack = guiGraphics.m_280168_();
      poseStack.m_85836_();
      this.m_280273_(guiGraphics);
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
      guiGraphics.m_280398_(BACKGROUND_TEXTURE, 0, 0, 1, 0.0F, 0.0F, this.f_96543_, this.f_96544_, this.f_96543_, this.f_96544_);
      RenderSystem.disableBlend();
      super.m_88315_(guiGraphics, mouseX, mouseY, partialTicks);
      this.m_280072_(guiGraphics, mouseX, mouseY);
      poseStack.m_85849_();
   }

   protected void m_280003_(GuiGraphics guiGraphics, int mouseX, int mouseY) {
      ItemStack selectedItem = this.f_96541_.f_91074_.m_21205_();
      if (selectedItem != null && selectedItem.m_41720_() instanceof AttachmentHost) {
         Component label = selectedItem.m_41720_().m_7626_(selectedItem);
         guiGraphics.m_280653_(this.f_96547_, label, 180, 15, 16776960);
      }

   }

   private void renderItemInHand(GuiGraphics guiGraphics, int mouseX, int mouseY) {
      Minecraft minecraft = Minecraft.m_91087_();
      PoseStack poseStack = guiGraphics.m_280168_();
      poseStack.m_85836_();
      poseStack.m_252880_((float)(this.f_96543_ / 2 + 5), (float)(this.f_96544_ / 2 - 30), 180.0F);
      this.applyMouseInteractionTransforms(poseStack, mouseX, mouseY);
      float zoom = this.mouseInteractionHandler.getZoom();
      poseStack.m_85841_(zoom, zoom, zoom);
      poseStack.m_252781_(Axis.f_252436_.m_252977_(-90.0F));
      poseStack.m_252931_((new Matrix4f()).scaling(1.0F, -1.0F, 1.0F));
      poseStack.m_85841_(100.0F, 100.0F, 100.0F);
      PoseStack modelStack = RenderSystem.getModelViewStack();
      modelStack.m_85836_();
      modelStack.m_252931_(poseStack.m_85850_().m_252922_());
      RenderSystem.applyModelViewMatrix();
      BufferSource buffer = this.f_96541_.m_91269_().m_110104_();
      ItemStack itemStack = minecraft.f_91074_.m_21205_();
      BakedModel model = minecraft.m_91291_().m_174264_(itemStack, MiscUtil.getLevel(minecraft.f_91074_), minecraft.f_91074_, minecraft.f_91074_.m_19879_() + ItemDisplayContext.GROUND.ordinal());
      minecraft.m_91291_().m_115143_(itemStack, ItemDisplayContext.GROUND, false, new PoseStack(), buffer, 15728880, OverlayTexture.f_118083_, model);
      buffer.m_109911_();
      modelStack.m_85849_();
      poseStack.m_85849_();
      RenderSystem.applyModelViewMatrix();
   }

   private void applyMouseInteractionTransforms(PoseStack poseStack, int mouseX, int mouseY) {
      float interactionOffsetX = (float)this.mouseInteractionHandler.getX() + (this.mouseInteractionHandler.isInteracting() && this.mouseInteractionHandler.isTranslating() ? (float)mouseX - this.mouseInteractionHandler.getMouseClickedX() : 0.0F);
      float interactionOffsetY = (float)this.mouseInteractionHandler.getY() + (this.mouseInteractionHandler.isInteracting() && this.mouseInteractionHandler.isTranslating() ? (float)mouseY - this.mouseInteractionHandler.getMouseClickedY() : 0.0F);
      poseStack.m_252880_(interactionOffsetX, interactionOffsetY, 0.0F);
      float interactionPitch = this.mouseInteractionHandler.getRotationPitch() - (this.mouseInteractionHandler.isInteracting() && this.mouseInteractionHandler.isRotating() ? (float)mouseY - this.mouseInteractionHandler.getMouseClickedY() : 0.0F) - 30.0F;
      float interactionYaw = this.mouseInteractionHandler.getRotationYaw() + (this.mouseInteractionHandler.isInteracting() && this.mouseInteractionHandler.isRotating() ? (float)mouseX - this.mouseInteractionHandler.getMouseClickedX() : 0.0F) + 150.0F;
      poseStack.m_252781_((new Quaternionf()).rotationXYZ(interactionPitch * 0.017453292F, interactionYaw * 0.017453292F, 0.0F));
   }

   public String getSelectedAttachmentPath() {
      return this.selectedAttachmentPath;
   }

   protected void m_7286_(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
      PoseStack poseStack = guiGraphics.m_280168_();
      poseStack.m_85836_();
      poseStack.m_252880_(0.0F, 0.0F, 250.0F);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      int left = this.getLeft();
      int top = this.getTop();
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      guiGraphics.m_280398_(GUI_TEXTURES, left, top, 1000, 0.0F, 0.0F, 176, 90, 256, 256);
      int attLeft = this.f_97735_ + 5;
      int attTop = this.f_97736_ + 8;
      int slotTextureLeftOffset = 176;
      int unavailableSlotTextureTopOffset = 36;
      SimpleAttachmentContainer childContainer = null;
      Slot var14 = this.f_97734_;
      if (var14 instanceof HierarchicalSlot) {
         HierarchicalSlot hSlot = (HierarchicalSlot)var14;
         this.selectedAttachmentPath = hSlot.getPath();
      }

      var14 = this.f_97734_;
      int i;
      if (var14 instanceof AttachmentSlot) {
         AttachmentSlot attachmentSlot = (AttachmentSlot)var14;
         childContainer = attachmentSlot.getChildContainer();
         if (childContainer != null) {
            i = childContainer.getContainerIndex();
            poseStack.m_85836_();
            int elementsCount = childContainer.getVirtualInventory().getElements().size();
            guiGraphics.m_280637_(attLeft + i * 22 - 2, attTop - 2, 22, 24 + 18 * elementsCount, -1061093377);
            poseStack.m_85849_();
         }
      }

      SimpleAttachmentContainer[] attachmentContainers = this.menu.getAttachmentContainers();

      for(i = 0; i < attachmentContainers.length; ++i) {
         SimpleAttachmentContainer attachmentContainer = attachmentContainers[i];
         int availableSlotTextureTopOffset = i == 0 ? 0 : 18;
         int containerStartIndex = SimpleAttachmentContainer.getContainerStartIndex(attachmentContainers, i);
         if (attachmentContainer.getVirtualInventory() != null && !attachmentContainer.getVirtualInventory().getElements().isEmpty()) {
            if (this.menu.m_38853_(containerStartIndex).m_6659_()) {
               guiGraphics.m_280398_(GUI_TEXTURES, attLeft + i * 22, attTop, 1000, (float)slotTextureLeftOffset, 54.0F, 18, 18, 256, 256);
            }

            for(int j = 1; j < attachmentContainer.m_6643_(); ++j) {
               int adjustedSlotIndex = containerStartIndex + j;
               AttachmentSlot slot = (AttachmentSlot)this.menu.m_38853_(adjustedSlotIndex);
               if (slot.m_6659_()) {
                  int textureTopOffset = this.menu.m_142621_() != null && !this.menu.m_142621_().m_41619_() && !slot.m_5857_(this.menu.m_142621_()) ? unavailableSlotTextureTopOffset : availableSlotTextureTopOffset;
                  guiGraphics.m_280398_(GUI_TEXTURES, attLeft + i * 22, attTop + this.headerBottomPadding + j * 18, 1000, (float)slotTextureLeftOffset, (float)textureTopOffset, 18, 18, 256, 256);
               }
            }
         }
      }

      poseStack.m_85849_();
      poseStack.m_85836_();
      this.renderItemInHand(guiGraphics, mouseX, mouseY);
      this.selectedAttachmentPath = null;
      poseStack.m_85849_();
      RenderSystem.disableBlend();
      poseStack.m_252880_(0.0F, 0.0F, 2000.0F);
   }

   public boolean m_6050_(double mouseX, double mouseY, double scroll) {
      return this.mouseInteractionHandler.onMouseScrolled(mouseX, mouseY, scroll);
   }

   public boolean m_6375_(double mouseX, double mouseY, int button) {
      return this.mouseInteractionHandler.onMouseButtonClicked(mouseX, mouseY, button) || super.m_6375_(mouseX, mouseY, button);
   }

   public boolean m_6348_(double mouseX, double mouseY, int button) {
      return this.mouseInteractionHandler.onMouseButtonReleased(mouseX, mouseY, button) || super.m_6348_(mouseX, mouseY, button);
   }

   private int getLeft() {
      return (this.f_96543_ - 176) / 2;
   }

   private int getTop() {
      return (this.f_96544_ - 90) / 2 + 74;
   }

   private boolean isMouseInScreen(double mouseX, double mouseY) {
      int width = 176;
      int left = this.getLeft();
      int top = this.getTop();
      boolean isMouseInInventory = mouseX >= (double)left && mouseX <= (double)(left + width) && mouseY >= (double)top && mouseY <= (double)(top + 90);
      if (isMouseInInventory) {
         return false;
      } else {
         int attLeft = this.f_97735_ + 5;
         int attTop = this.f_97736_ + 8;
         boolean isMouseInAttachmentContainer = false;
         SimpleAttachmentContainer[] attachmentContainers = this.menu.getAttachmentContainers();

         for(int i = 0; i < attachmentContainers.length; ++i) {
            SimpleAttachmentContainer attachmentContainer = attachmentContainers[i];
            int containerStartIndex = SimpleAttachmentContainer.getContainerStartIndex(attachmentContainers, i);
            if (attachmentContainer.getVirtualInventory() != null && !attachmentContainer.getVirtualInventory().getElements().isEmpty()) {
               int slotLeft = attLeft + i * 22;
               int slotRight = slotLeft + 18 + 4;
               if (!(mouseX < (double)slotLeft) && !(mouseX > (double)slotRight) && !(mouseY < (double)attTop)) {
                  int yBottom = 0;

                  for(int j = 1; j < attachmentContainer.m_6643_(); ++j) {
                     int adjustedSlotIndex = containerStartIndex + j;
                     AttachmentSlot slot = (AttachmentSlot)this.menu.m_38853_(adjustedSlotIndex);
                     if (!slot.m_6659_()) {
                        yBottom = attTop + this.headerBottomPadding + j * 18;
                        break;
                     }
                  }

                  if (mouseY < (double)yBottom) {
                     isMouseInAttachmentContainer = true;
                     break;
                  }
               }
            }
         }

         return !isMouseInAttachmentContainer;
      }
   }

   public void m_7379_() {
      this.selectedAttachmentPath = null;
      MinecraftForge.EVENT_BUS.unregister(this);
      super.m_7379_();
   }

   public void beforeRenderingSlot(GuiGraphics guiGraphics, Slot slot) {
      if (!(slot instanceof AttachmentSlot)) {
         if (this.menu.getPlayerInventory().f_35977_ == 9 - (this.menu.f_38839_.size() - slot.f_40219_)) {
            guiGraphics.m_280509_(slot.f_40220_, slot.f_40221_, slot.f_40220_ + 16, slot.f_40221_ + 16, 1348235500);
         }

         if (slot.f_40219_ >= this.menu.getTotalAttachmentSlots() && this.mayPlaceAttachment(slot.m_7993_())) {
            guiGraphics.m_280509_(slot.f_40220_, slot.f_40221_, slot.f_40220_ + 16, slot.f_40221_ + 16, -1605328816);
         }

      }
   }

   private boolean mayPlaceAttachment(ItemStack attachmentStack) {
      if (!attachmentStack.m_41619_() && attachmentStack.m_41720_() instanceof Attachment) {
         SimpleAttachmentContainer[] attachmentContainers = this.menu.getAttachmentContainers();

         for(int i = 0; i < attachmentContainers.length; ++i) {
            SimpleAttachmentContainer attachmentContainer = attachmentContainers[i];
            int containerStartIndex = SimpleAttachmentContainer.getContainerStartIndex(attachmentContainers, i);
            if (attachmentContainer.getVirtualInventory() != null && !attachmentContainer.getVirtualInventory().getElements().isEmpty()) {
               for(int j = 1; j < attachmentContainer.m_6643_(); ++j) {
                  int adjustedSlotIndex = containerStartIndex + j;
                  AttachmentSlot slot = (AttachmentSlot)this.menu.m_38853_(adjustedSlotIndex);
                  if (slot.m_6659_() && slot.m_5857_(attachmentStack)) {
                     return true;
                  }
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public Pair<RenderType, Vector4f> getRenderTypeOverride(ItemStack baseItemStack, ItemStack attachmentItemStack, String attachmentName, String attachmentPath) {
      ResourceLocation texture = new ResourceLocation("pointblank", "textures/item/" + attachmentName + ".png");
      AttachmentHighlightEvent attachmentHighlight = (AttachmentHighlightEvent)this.attachmentEventQueue.peek();
      if (attachmentHighlight != null && ItemStack.m_41656_(attachmentItemStack, attachmentHighlight.attachmentStack)) {
         if (attachmentHighlight.isHighlighted()) {
            RenderType renderType = RenderType.m_110488_(texture);
            return Pair.of(renderType, COLOR_GREEN);
         } else {
            return null;
         }
      } else {
         String selectedAttachmentPath = this.getSelectedAttachmentPath();
         RenderType renderType;
         if (Objects.equals(attachmentPath, selectedAttachmentPath)) {
            renderType = RenderType.m_110488_(texture);
            return Pair.of(renderType, COLOR_GREEN);
         } else if (selectedAttachmentPath != null && selectedAttachmentPath.endsWith("/") && attachmentPath.startsWith(selectedAttachmentPath.substring(0, selectedAttachmentPath.length() - 1))) {
            renderType = RenderType.m_110488_(texture);
            return Pair.of(renderType, COLOR_GREEN);
         } else {
            return null;
         }
      }
   }

   @SubscribeEvent
   public void onAttachmentAdded(AttachmentAddedEvent event) {
      if (EffectiveSide.get() == LogicalSide.CLIENT) {
         if (this.f_96541_.f_91080_ != this) {
            return;
         }

         ClientUtils.getClientPlayer().m_5496_((SoundEvent)SoundRegistry.ATTACHMENT_ADDED.get(), 1.0F, 1.0F);
         this.attachmentEventQueue.add(new AttachmentHighlightEvent(System.currentTimeMillis(), 750L, event.getParentStack(), event.getAttachmentStack()));
      }

   }

   @SubscribeEvent
   public void onAttachmentRemoved(AttachmentRemovedEvent event) {
      if (EffectiveSide.get() == LogicalSide.CLIENT) {
         if (this.f_96541_.f_91080_ != this) {
            return;
         }

         ClientUtils.getClientPlayer().m_5496_((SoundEvent)SoundRegistry.ATTACHMENT_REMOVED.get(), 1.0F, 1.0F);
         this.attachmentEventQueue.add(new AttachmentHighlightEvent(System.currentTimeMillis(), 750L, event.getParentStack(), event.getAttachmentStack()));
      }

   }

   private static class AttachmentHighlightEvent {
      private long startTime;
      private long duration;
      private ItemStack parentStack;
      private ItemStack attachmentStack;
      private long blinkInterval;

      public AttachmentHighlightEvent(long startTime, long duration, ItemStack parentStack, ItemStack attachmentStack) {
         this.startTime = startTime;
         this.duration = duration;
         this.parentStack = parentStack;
         this.attachmentStack = attachmentStack;
         this.blinkInterval = 150L;
      }

      public boolean isHighlighted() {
         long elapsedTime = System.currentTimeMillis() - this.startTime;
         if (elapsedTime >= this.duration) {
            return false;
         } else {
            long k = elapsedTime / this.blinkInterval;
            return k % 2L == 0L;
         }
      }

      public boolean isExpired() {
         return System.currentTimeMillis() >= this.startTime + this.duration;
      }
   }
}
