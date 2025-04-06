package com.vicmatskiv.pointblank.client.gui;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.vicmatskiv.pointblank.Enableable;
import com.vicmatskiv.pointblank.client.render.RenderUtil;
import com.vicmatskiv.pointblank.client.uv.PlayOnceSpriteUVProvider;
import com.vicmatskiv.pointblank.client.uv.SpriteUVProvider;
import com.vicmatskiv.pointblank.crafting.Craftable;
import com.vicmatskiv.pointblank.crafting.PointBlankRecipe;
import com.vicmatskiv.pointblank.inventory.CraftingContainerMenu;
import com.vicmatskiv.pointblank.inventory.IngredientSlot;
import com.vicmatskiv.pointblank.inventory.SearchSlot;
import com.vicmatskiv.pointblank.network.CraftingRequestPacket;
import com.vicmatskiv.pointblank.network.Network;
import com.vicmatskiv.pointblank.registry.SoundRegistry;
import com.vicmatskiv.pointblank.util.CancellableSound;
import com.vicmatskiv.pointblank.util.MiscUtil;
import com.vicmatskiv.pointblank.util.StateMachine;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.CreativeInventoryListener;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.TooltipFlag.Default;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import software.bernie.geckolib.util.ClientUtils;

@OnlyIn(Dist.CLIENT)
public class CraftingScreen extends EffectRenderingInventoryScreen<CraftingContainerMenu> {
   public static final int CELL_WIDTH = 18;
   public static final int CELL_HEIGHT = 18;
   private static final ResourceLocation BACKGROUND_TEXTURE = new ResourceLocation("pointblank", "textures/gui/craftnew.png");
   private static final ResourceLocation GLOW_OUTLINE = new ResourceLocation("pointblank", "textures/gui/glow_outline.png");
   private static final ResourceLocation TABS_TEXTURE = new ResourceLocation("minecraft:textures/gui/container/creative_inventory/tabs.png");
   private StateMachine<CraftingState, Context> stateMachine = this.createStateMachine();
   private float scrollOffs;
   private boolean scrolling;
   private EditBox searchBox;
   private CustomButton craftButton;
   private CreativeInventoryListener listener;
   private boolean ignoreTextInput;
   private boolean hasClickedOutside;
   private final Set<TagKey<Item>> visibleTags = new HashSet();
   private int scrollbarXOffset = 121;
   private int scrollbarYOffset = 18;
   private int scrollbarWidth = 14;
   private int scrollbarHeight = 126;
   private ItemStack selectedItem;
   private PointBlankRecipe selectedItemRecipe;
   private long craftingStartTime;
   private long craftingCompletedCooldownStartTime;
   private long craftingCompletedCooldownDuration = 2000L;
   private float itemRotationAngleDegrees;
   private float itemRotationAngleDegreesPerTick;
   private float idleItemRotationsPerSecond = 0.1F;
   private SpriteUVProvider glowOutlineSpriteUVProvider;

   public CraftingScreen(CraftingContainerMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title);
      playerInventory.f_35978_.f_36096_ = this.f_97732_;
      this.f_97726_ = 312;
      this.f_97727_ = 151;
      this.itemRotationAngleDegreesPerTick = rotationsPerSecondToDegress(this.idleItemRotationsPerSecond);
      this.craftingCompletedCooldownDuration = 600L;
      this.glowOutlineSpriteUVProvider = new PlayOnceSpriteUVProvider(6, 6, 50, 600L);
   }

   private static float rotationsPerSecondToDegress(float rotationsPerSecond) {
      return 360.0F * rotationsPerSecond * 0.05F;
   }

   private StateMachine<CraftingState, Context> createStateMachine() {
      StateMachine.Builder<CraftingState, Context> builder = new StateMachine.Builder();
      builder.withTransition((Enum) CraftingState.IDLE, CraftingState.CRAFTING, (context) -> {
         boolean var10000;
         if (this.selectedItem != null && this.selectedItem.m_41720_() instanceof Craftable && this.selectedItemRecipe != null) {
            label20: {
               Item patt6136$temp = this.selectedItem.m_41720_();
               if (patt6136$temp instanceof Enableable) {
                  Enableable en = (Enableable)patt6136$temp;
                  if (!en.isEnabled()) {
                     break label20;
                  }
               }

               if (((CraftingContainerMenu)this.f_97732_).isIdle() && this.craftButton.isPressed()) {
                  var10000 = true;
                  return var10000;
               }
            }
         }

         var10000 = false;
         return var10000;
      }, StateMachine.TransitionMode.EVENT, (StateMachine.Action)null, this::actionStartCrafting);
      builder.withTransition((Enum) CraftingState.CRAFTING, CraftingState.IDLE, (context) -> {
         return !this.craftButton.isPressed();
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, this::actionCancelCrafting);
      builder.withTransition((Enum) CraftingState.CRAFTING, CraftingState.IDLE, (context) -> {
         return true;
      }, StateMachine.TransitionMode.EVENT, (StateMachine.Action)null, (StateMachine.Action)null);
      builder.withTransition((Enum) CraftingState.CRAFTING, CraftingState.CRAFTING_COMPLETED, (ctx) -> {
         return System.currentTimeMillis() - this.craftingStartTime >= ((Craftable)this.selectedItem.m_41720_()).getCraftingDuration();
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, this::actionCraftingCompleted);
      builder.withTransition((Enum) CraftingState.CRAFTING_COMPLETED, CraftingState.CRAFTING_COMPLETED_COOLDOWN, (context) -> {
         return true;
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, (ctx, f, t) -> {
         this.craftingCompletedCooldownStartTime = System.currentTimeMillis();
      });
      builder.withTransition((Enum) CraftingState.CRAFTING_COMPLETED_COOLDOWN, CraftingState.IDLE, (ctx) -> {
         return System.currentTimeMillis() - this.craftingCompletedCooldownStartTime >= this.craftingCompletedCooldownDuration;
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, (StateMachine.Action)null);
      builder.withOnSetStateAction(CraftingState.IDLE, this::actionIdle);
      return builder.build(CraftingState.IDLE);
   }

   private void actionIdle(Context context, CraftingState fromState, CraftingState toState) {
      this.craftButton.m_93692_(false);
      this.itemRotationAngleDegreesPerTick = rotationsPerSecondToDegress(this.idleItemRotationsPerSecond);
   }

   private void actionStartCrafting(Context context, CraftingState fromState, CraftingState toState) {
      this.craftingStartTime = System.currentTimeMillis();
      Player player = ClientUtils.getClientPlayer();
      this.f_96541_.m_91106_().m_120367_(new CancellableSound(player, (SoundEvent)SoundRegistry.CRAFTING_IN_PROGRESS.get(), player.m_5720_(), player.m_217043_(), (s) -> {
         return this.f_96541_.f_91080_ == this && this.stateMachine.getCurrentState() == CraftingState.CRAFTING;
      }));
      Network.networkChannel.sendToServer(new CraftingRequestPacket(CraftingRequestPacket.RequestType.START_CRAFTING, this.selectedItemRecipe.m_6423_()));
      this.craftButton.m_257544_((Tooltip)null);
   }

   public void m_7379_() {
      if (this.stateMachine.getCurrentState() == CraftingState.CRAFTING && this.selectedItemRecipe != null) {
         Network.networkChannel.sendToServer(new CraftingRequestPacket(CraftingRequestPacket.RequestType.CANCEL_CRAFTING, this.selectedItemRecipe.m_6423_()));
      }

      super.m_7379_();
   }

   private void actionCancelCrafting(Context context, CraftingState fromState, CraftingState toState) {
      Network.networkChannel.sendToServer(new CraftingRequestPacket(CraftingRequestPacket.RequestType.CANCEL_CRAFTING, this.selectedItemRecipe.m_6423_()));
   }

   private void actionCraftingCompleted(Context context, CraftingState fromState, CraftingState toState) {
      ClientUtils.getClientPlayer().m_5496_((SoundEvent)SoundRegistry.CRAFTING_COMPLETED.get(), 1.0F, 1.0F);
   }

   private float getCraftingProgress() {
      return this.stateMachine.getCurrentState() != CraftingState.CRAFTING ? 0.0F : Mth.m_14036_((float)(System.currentTimeMillis() - this.craftingStartTime) / (float)((Craftable)this.selectedItem.m_41720_()).getCraftingDuration(), 0.0F, 1.0F);
   }

   private float getCraftingCompletedCooldownProgress() {
      return this.stateMachine.getCurrentState() != CraftingState.CRAFTING_COMPLETED_COOLDOWN ? 0.0F : Mth.m_14036_((float)(System.currentTimeMillis() - this.craftingCompletedCooldownStartTime) / (float)this.craftingCompletedCooldownDuration, 0.0F, 1.0F);
   }

   private void refreshContents() {
      this.updateIngredientSlots();
   }

   public void m_181908_() {
      if (this.f_96541_ != null) {
         Context context = new Context();
         this.stateMachine.update(context);
         if (this.selectedItem == null || !((CraftingContainerMenu)this.f_97732_).isIdle() && !((CraftingContainerMenu)this.f_97732_).isCrafting() || this.stateMachine.getCurrentState() != CraftingState.IDLE && (this.stateMachine.getCurrentState() != CraftingState.CRAFTING || !this.craftButton.isPressed())) {
            this.craftButton.f_93623_ = false;
         } else {
            this.craftButton.f_93623_ = true;
         }

         this.itemRotationAngleDegrees += this.itemRotationAngleDegreesPerTick;
         if (this.f_96541_.f_91074_ != null) {
            this.refreshContents();
         }

         this.searchBox.m_94120_();
      }

   }

   protected void m_6597_(@Nullable Slot slot, int slotIndex, int mouseButton, ClickType clickType) {
      if (((CraftingContainerMenu)this.f_97732_).isIdle() && ((CraftingContainerMenu)this.f_97732_).isCreativeSlot(slot)) {
         this.searchBox.m_94201_();
         this.searchBox.m_94208_(0);
         this.craftButton.m_93692_(false);
         this.craftButton.f_93623_ = false;
         this.onSelectCraftableItem(slot.m_7993_());
      }

   }

   private void onSelectCraftableItem(ItemStack itemStack) {
      this.craftButton.f_93623_ = false;
      this.selectedItem = itemStack;
      ((CraftingContainerMenu)this.f_97732_).clearIngredientSlots();
      ClientUtils.getClientPlayer().m_5496_((SoundEvent)SoundRegistry.CRAFTING_ITEM_SELECTED.get(), 1.0F, 1.0F);
      if (this.selectedItem != null && !this.selectedItem.m_41619_() && this.selectedItem.m_41720_() instanceof Craftable) {
         this.selectedItemRecipe = PointBlankRecipe.getRecipe(ClientUtils.getLevel(), itemStack.m_41720_());
         if (this.selectedItemRecipe != null) {
            this.updateIngredientSlots();
         }
      } else {
         this.selectedItemRecipe = null;
      }

   }

   private void updateIngredientSlots() {
      if (this.selectedItemRecipe == null) {
         this.craftButton.f_93623_ = false;
      } else {
         this.craftButton.f_93623_ = this.stateMachine.getCurrentState() == CraftingState.IDLE && ((CraftingContainerMenu)this.f_97732_).updateIngredientSlots(this.selectedItemRecipe);
         if (this.selectedItem != null && (((CraftingContainerMenu)this.f_97732_).isIdle() || ((CraftingContainerMenu)this.f_97732_).isCrafting()) && (this.stateMachine.getCurrentState() == CraftingState.IDLE || this.stateMachine.getCurrentState() == CraftingState.CRAFTING) && ((CraftingContainerMenu)this.f_97732_).updateIngredientSlots(this.selectedItemRecipe)) {
            this.craftButton.f_93623_ = true;
         } else {
            this.craftButton.f_93623_ = false;
         }

      }
   }

   protected void m_7856_() {
      super.m_7856_();
      ((CraftingContainerMenu)this.f_97732_).clearIngredientSlots();
      int searchBoxLeftOffset = 46;
      this.searchBox = new EditBox(this.f_96547_, this.f_97735_ + searchBoxLeftOffset, this.f_97736_ + 6, 80, 9, Component.m_237115_("itemGroup.search"));
      this.searchBox.m_94199_(50);
      this.searchBox.m_94182_(false);
      this.searchBox.m_94194_(false);
      this.searchBox.m_94202_(16777215);
      this.m_7787_(this.searchBox);
      this.m_238391_();
      this.searchBox.m_94194_(true);
      this.searchBox.m_94190_(false);
      this.searchBox.m_93692_(true);
      this.searchBox.m_93674_(89);
      this.searchBox.m_252865_(this.f_97735_ + searchBoxLeftOffset + 89 - this.searchBox.m_5711_());
      this.craftButton = CustomButton.builder(Component.m_237115_("label.pointblank.craft"), (b) -> {
         Context context = new Context();
         this.stateMachine.setState(context, (Enum) CraftingState.CRAFTING);
      }).onRelease((b) -> {
      }).bounds(this.f_97735_ + 256, this.f_97736_ + 115, 46, 20).progressProvider(this::getCraftingProgress).tooltip(Tooltip.m_257550_(Component.m_237115_("message.pointblank.press_and_hold_to_craft"))).build();
      this.craftButton.m_93692_(false);
      this.craftButton.f_93623_ = false;
      this.m_142416_(this.craftButton);
      this.refreshSearchResults();
      this.scrollOffs = 0.0F;
      ((CraftingContainerMenu)this.f_97732_).scrollTo(0.0F);
      this.selectedItem = null;
      this.selectedItemRecipe = null;
      this.f_96541_.f_91074_.f_36095_.m_38943_(this.listener);
      this.listener = new CreativeInventoryListener(this.f_96541_);
      this.f_96541_.f_91074_.f_36095_.m_38893_(this.listener);
   }

   public void m_6574_(Minecraft minecraft, int width, int height) {
      int i = ((CraftingContainerMenu)this.f_97732_).getRowIndexForScroll(this.scrollOffs);
      String s = this.searchBox.m_94155_();
      ItemStack currentSelectedItem = this.selectedItem;
      this.m_6575_(minecraft, width, height);
      this.searchBox.m_94144_(s);
      if (!this.searchBox.m_94155_().isEmpty()) {
         this.refreshSearchResults();
      }

      this.onSelectCraftableItem(currentSelectedItem);
      this.scrollOffs = ((CraftingContainerMenu)this.f_97732_).getScrollForRowIndex(i);
      ((CraftingContainerMenu)this.f_97732_).scrollTo(this.scrollOffs);
   }

   public void m_7861_() {
      super.m_7861_();
      if (this.f_96541_.f_91074_ != null && this.f_96541_.f_91074_.m_150109_() != null) {
         this.f_96541_.f_91074_.f_36095_.m_38943_(this.listener);
      }

   }

   public boolean m_5534_(char p_98521_, int p_98522_) {
      if (this.ignoreTextInput) {
         return false;
      } else {
         String s = this.searchBox.m_94155_();
         if (this.searchBox.m_5534_(p_98521_, p_98522_)) {
            if (!Objects.equals(s, this.searchBox.m_94155_())) {
               this.refreshSearchResults();
            }

            return true;
         } else {
            return false;
         }
      }
   }

   public boolean m_7933_(int p_98547_, int p_98548_, int p_98549_) {
      this.ignoreTextInput = false;
      boolean flag = !((CraftingContainerMenu)this.f_97732_).isCreativeSlot(this.f_97734_) || this.f_97734_.m_6657_();
      boolean flag1 = InputConstants.m_84827_(p_98547_, p_98548_).m_84876_().isPresent();
      if (flag && flag1 && this.m_97805_(p_98547_, p_98548_)) {
         this.ignoreTextInput = true;
         return true;
      } else {
         String s = this.searchBox.m_94155_();
         if (this.searchBox.m_7933_(p_98547_, p_98548_, p_98549_)) {
            if (!Objects.equals(s, this.searchBox.m_94155_())) {
               this.refreshSearchResults();
            }

            return true;
         } else {
            return this.searchBox.m_93696_() && this.searchBox.m_94213_() && p_98547_ != 256 ? true : super.m_7933_(p_98547_, p_98548_, p_98549_);
         }
      }
   }

   public boolean m_7920_(int p_98612_, int p_98613_, int p_98614_) {
      this.ignoreTextInput = false;
      return super.m_7920_(p_98612_, p_98613_, p_98614_);
   }

   private void refreshSearchResults() {
      ((CraftingContainerMenu)this.f_97732_).refreshSearchResults(this.searchBox.m_94155_());
      this.scrollOffs = 0.0F;
   }

   protected void m_280003_(GuiGraphics guiGraphics, int mouseX, int mouseY) {
      Component label = this.selectedItem != null && !this.selectedItem.m_41619_() ? this.selectedItem.m_41720_().m_7626_(this.selectedItem) : Component.m_237115_("label.pointblank.craft");
      guiGraphics.m_280653_(this.f_96547_, (Component)label, 225, 8, 16776960);
   }

   public boolean m_6375_(double posX, double posY, int mouseButton) {
      if (mouseButton == 0 && this.insideScrollbar(posX, posY)) {
         this.scrolling = this.canScroll();
         return true;
      } else {
         return super.m_6375_(posX, posY, mouseButton);
      }
   }

   public boolean m_6348_(double posX, double posY, int mouseButton) {
      if (this.craftButton.isPressed()) {
         this.craftButton.release();
      }

      if (mouseButton == 0) {
         this.scrolling = false;
      }

      return super.m_6348_(posX, posY, mouseButton);
   }

   private boolean canScroll() {
      return ((CraftingContainerMenu)this.f_97732_).canScroll();
   }

   public boolean m_6050_(double mouseX, double mouseY, double scroll) {
      if (!this.canScroll()) {
         return false;
      } else {
         this.scrollOffs = ((CraftingContainerMenu)this.f_97732_).subtractInputFromScroll(this.scrollOffs, scroll);
         ((CraftingContainerMenu)this.f_97732_).scrollTo(this.scrollOffs);
         return true;
      }
   }

   protected boolean m_7467_(double mouseX, double mouseY, int p_98543_, int p_98544_, int mouseButton) {
      this.hasClickedOutside = mouseX < (double)p_98543_ || mouseY < (double)p_98544_ || mouseX >= (double)(p_98543_ + this.f_97726_) || mouseY >= (double)(p_98544_ + this.f_97727_);
      return this.hasClickedOutside;
   }

   protected boolean insideScrollbar(double mouseX, double mouseY) {
      int i = this.f_97735_;
      int j = this.f_97736_;
      int k = i + this.scrollbarXOffset;
      int l = j + this.scrollbarYOffset;
      int i1 = k + this.scrollbarWidth;
      int j1 = l + this.scrollbarHeight;
      boolean inside = mouseX >= (double)k && mouseY >= (double)l && mouseX < (double)i1 && mouseY < (double)j1;
      return inside;
   }

   public boolean m_7979_(double p_98535_, double p_98536_, int p_98537_, double p_98538_, double p_98539_) {
      if (this.scrolling) {
         int i = this.f_97736_ + 18;
         int j = i + 112;
         this.scrollOffs = ((float)p_98536_ - (float)i - 7.5F) / ((float)(j - i) - 15.0F);
         this.scrollOffs = Mth.m_14036_(this.scrollOffs, 0.0F, 1.0F);
         ((CraftingContainerMenu)this.f_97732_).scrollTo(this.scrollOffs);
         return true;
      } else {
         return super.m_7979_(p_98535_, p_98536_, p_98537_, p_98538_, p_98539_);
      }
   }

   public void m_88315_(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      this.m_280273_(guiGraphics);
      super.m_88315_(guiGraphics, mouseX, mouseY, partialTick);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      this.m_280072_(guiGraphics, mouseX, mouseY);
   }

   private void renderItemInHand(GuiGraphics guiGraphics, int mouseX, int mouseY) {
      if (this.selectedItem != null) {
         int left = (this.f_96543_ - this.f_97726_) / 2;
         int top = (this.f_96544_ - this.f_97727_) / 2;
         guiGraphics.m_280588_(left + 151, top + 23, left + 300, top + 98);
         PoseStack poseStack = guiGraphics.m_280168_();
         poseStack.m_85836_();
         float itemRotationAngleDegreesWithPartial = this.itemRotationAngleDegrees;
         float yOffset = 0.0F;
         float zoom = 1.0F;
         itemRotationAngleDegreesWithPartial += this.itemRotationAngleDegreesPerTick * this.f_96541_.getPartialTick();
         yOffset = Mth.m_14089_(itemRotationAngleDegreesWithPartial * 3.1415927F * 0.02F);
         if (this.stateMachine.getCurrentState() == CraftingState.CRAFTING_COMPLETED_COOLDOWN) {
            zoom += 0.2F * Mth.m_14031_(this.getCraftingCompletedCooldownProgress() * 3.1415927F * 2.0F + 3.1415927F);
         }

         poseStack.m_252880_((float)(left + 230), (float)(this.f_96544_ / 2 - 18) + yOffset * 2.0F, 100.0F);
         float interactionPitch = -30.0F;
         float interactionYaw = 150.0F;
         poseStack.m_252781_((new Quaternionf()).rotationXYZ(interactionPitch * 0.017453292F, interactionYaw * 0.017453292F, 0.0F));
         poseStack.m_85841_(zoom, zoom, zoom);
         poseStack.m_252781_(Axis.f_252436_.m_252977_(-90.0F + itemRotationAngleDegreesWithPartial));
         poseStack.m_252931_((new Matrix4f()).scaling(1.0F, -1.0F, 1.0F));
         poseStack.m_85841_(80.0F, 80.0F, 80.0F);
         PoseStack modelStack = RenderSystem.getModelViewStack();
         modelStack.m_85836_();
         modelStack.m_252931_(poseStack.m_85850_().m_252922_());
         RenderSystem.applyModelViewMatrix();
         BufferSource buffer = this.f_96541_.m_91269_().m_110104_();
         BakedModel model = this.f_96541_.m_91291_().m_174264_(this.selectedItem, MiscUtil.getLevel(this.f_96541_.f_91074_), this.f_96541_.f_91074_, this.f_96541_.f_91074_.m_19879_() + ItemDisplayContext.GROUND.ordinal());
         this.f_96541_.m_91291_().m_115143_(this.selectedItem, ItemDisplayContext.GROUND, false, new PoseStack(), buffer, 15728880, OverlayTexture.f_118083_, model);
         buffer.m_109911_();
         modelStack.m_85849_();
         poseStack.m_85849_();
         RenderSystem.applyModelViewMatrix();
         guiGraphics.m_280618_();
      }
   }

   public List<Component> m_280553_(ItemStack itemStack) {
      boolean flag = this.f_97734_ != null && this.f_97734_ instanceof SearchSlot;
      boolean flag2 = true;
      Default tooltipflag$default = this.f_96541_.f_91066_.f_92125_ ? Default.f_256730_ : Default.f_256752_;
      TooltipFlag tooltipflag = flag ? tooltipflag$default.m_257777_() : tooltipflag$default;
      List<Component> list = itemStack.m_41651_(this.f_96541_.f_91074_, tooltipflag);
      List<Component> tooltipComponents = Lists.newArrayList(list);
      if (flag2 && flag) {
         this.visibleTags.forEach((p_205407_) -> {
            if (itemStack.m_204117_(p_205407_)) {
               tooltipComponents.add(1, Component.m_237113_("#" + p_205407_.f_203868_()).m_130940_(ChatFormatting.DARK_PURPLE));
            }

         });
      }

      return tooltipComponents;
   }

   protected void m_7286_(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      guiGraphics.m_280163_(BACKGROUND_TEXTURE, this.f_97735_, this.f_97736_, 0.0F, 0.0F, this.f_97726_, this.f_97727_, 328, 328);
      this.searchBox.m_88315_(guiGraphics, mouseX, mouseY, partialTick);
      int j = this.f_97735_ + this.scrollbarXOffset;
      int k = this.f_97736_ + this.scrollbarYOffset;
      int i = k + this.scrollbarHeight;
      guiGraphics.m_280218_(TABS_TEXTURE, j, k + (int)((float)(i - k - 17) * this.scrollOffs), 232 + (this.canScroll() ? 0 : 12), 0, 12, 15);
      int ingredientSlotOffset = CraftingContainerMenu.SEARCH_CONTAINER.m_6643_();

      for(i = 0; i < CraftingContainerMenu.INGREDIENT_CONTAINER.m_6643_(); ++i) {
         IngredientSlot ingredientSlot = (IngredientSlot)((CraftingContainerMenu)this.f_97732_).f_38839_.get(ingredientSlotOffset + i);
         if (!ingredientSlot.isIngredientAvailable()) {
            guiGraphics.m_280163_(BACKGROUND_TEXTURE, this.f_97735_ + ingredientSlot.f_40220_ - 1, this.f_97736_ + ingredientSlot.f_40221_ - 1, 48.0F, 151.0F, 18, 18, 328, 328);
         }
      }

      int left = (this.f_96543_ - this.f_97726_) / 2;
      int top = (this.f_96544_ - this.f_97727_) / 2;
      if (this.stateMachine.getCurrentState() == CraftingState.CRAFTING) {
         int topOffset = 98 - (int)(75.0F * this.getCraftingProgress());
         guiGraphics.m_280024_(left + 151, top + topOffset, left + 300, top + 98, 268500736, 1342242560);
      }

      if (this.stateMachine.getCurrentState() == CraftingState.CRAFTING_COMPLETED_COOLDOWN) {
         float progress = this.getCraftingCompletedCooldownProgress();
         int minAlpha = 32;
         int maxAlpha = 96;
         int alpha1 = (int)((float)minAlpha - (float)minAlpha * progress);
         int alpha2 = (int)((float)maxAlpha - (float)maxAlpha * progress);
         guiGraphics.m_280024_(left + 151, top + 23, left + 300, top + 98, '\uff00' | alpha1 << 24, '\uff00' | alpha2 << 24);
         float[] spriteUV = this.glowOutlineSpriteUVProvider.getSpriteUV(progress);
         if (spriteUV != null) {
            float minU = spriteUV[0];
            float minV = spriteUV[1];
            float maxU = spriteUV[2];
            float maxV = spriteUV[3];
            RenderUtil.blit(guiGraphics, GLOW_OUTLINE, left + 139, left + 314, top - 30, top + 145, 0, minU, maxU, minV, maxV, 1.0F, 1.0F, 1.0F, 1.0F);
         }
      }

      RenderSystem.disableBlend();
      this.renderItemInHand(guiGraphics, mouseX, mouseY);
   }

   public void cancelCrafting() {
      this.stateMachine.setState(new Context(), (Enum) CraftingState.IDLE);
   }

   public static enum CraftingState {
      IDLE,
      CRAFTING,
      CRAFTING_COMPLETED,
      CRAFTING_COMPLETED_COOLDOWN;

      // $FF: synthetic method
      private static CraftingState[] $values() {
         return new CraftingState[]{IDLE, CRAFTING, CRAFTING_COMPLETED, CRAFTING_COMPLETED_COOLDOWN};
      }
   }

   private class Context {
   }
}
