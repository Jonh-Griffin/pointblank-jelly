package com.vicmatskiv.pointblank.jei;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.vicmatskiv.pointblank.crafting.PointBlankIngredient;
import com.vicmatskiv.pointblank.crafting.PointBlankRecipe;
import com.vicmatskiv.pointblank.registry.BlockRegistry;
import com.vicmatskiv.pointblank.util.MiscUtil;
import java.util.List;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.joml.Quaternionf;

public class PointBlankRecipeCategory implements IRecipeCategory<PointBlankRecipe> {
   public static final ResourceLocation BACKGROUND_RESOURCE = new ResourceLocation("pointblank", "textures/gui/jei.png");
   private IDrawableStatic backgroundDrawable;
   private IDrawableStatic itemDrawable;
   private IDrawableStatic inventoryDrawable;
   private IDrawable icon;
   private Component title;

   public PointBlankRecipeCategory(IGuiHelper gui) {
      this.backgroundDrawable = gui.createBlankDrawable(180, 124);
      this.itemDrawable = gui.createDrawable(BACKGROUND_RESOURCE, 2, 2, 180, 102);
      this.inventoryDrawable = gui.createDrawable(BACKGROUND_RESOURCE, 2, 108, 180, 19);
      this.icon = gui.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack((ItemLike)BlockRegistry.PRINTER.get()));
      this.title = Component.m_237115_("block.pointblank.printer");
   }

   public RecipeType<PointBlankRecipe> getRecipeType() {
      return PointBlankJeiPlugin.POINTBLANK_RECIPE_TYPE;
   }

   public Component getTitle() {
      return this.title;
   }

   public IDrawable getBackground() {
      return this.backgroundDrawable;
   }

   public IDrawable getIcon() {
      return this.icon;
   }

   public void setRecipe(IRecipeLayoutBuilder builder, PointBlankRecipe recipe, IFocusGroup focuses) {
      ItemStack resultStack = recipe.m_8043_((RegistryAccess)null);
      List<PointBlankIngredient> pbi = recipe.getPointBlankIngredients();

      for(int i = 0; i < pbi.size(); ++i) {
         builder.addSlot(RecipeIngredientRole.INPUT, i * 18 + 1, 106).addIngredients(VanillaTypes.ITEM_STACK, ((PointBlankIngredient)pbi.get(i)).getItemStacks());
      }

      builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStack(resultStack);
   }

   public void draw(PointBlankRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
      Minecraft mc = Minecraft.m_91087_();
      this.itemDrawable.draw(guiGraphics, 0, 0);
      this.inventoryDrawable.draw(guiGraphics, 0, this.itemDrawable.getHeight() + 3);
      Font font = mc.f_91062_;
      ItemStack itemStack = recipe.getInitializedStack();
      MutableComponent displayName = itemStack.m_41786_().m_6881_();
      if (itemStack.m_41613_() > 1) {
         displayName.m_7220_(Component.m_237113_(" x " + itemStack.m_41613_()));
      }

      int offsetX = this.itemDrawable.getWidth() >> 1;
      guiGraphics.m_280653_(font, displayName, offsetX, 4, -256);
      PoseStack poseStack = RenderSystem.getModelViewStack();
      float fullTick = (float)mc.f_91074_.f_19797_ + mc.m_91296_();
      float yOffset = Mth.m_14089_(fullTick * 3.1415927F * 0.02F) * 2.0F;
      poseStack.m_85836_();
      poseStack.m_252931_(guiGraphics.m_280168_().m_85850_().m_252922_());
      poseStack.m_252880_(90.0F, 55.0F + yOffset, 1000.0F);
      poseStack.m_252781_((new Quaternionf()).rotationXYZ(-0.34906584F, fullTick * 0.017453292F * 2.0F, 0.0F));
      poseStack.m_85841_(60.0F, -60.0F, 60.0F);
      RenderSystem.applyModelViewMatrix();
      BakedModel model = mc.m_91291_().m_174264_(itemStack, MiscUtil.getLevel(mc.f_91074_), mc.f_91074_, mc.f_91074_.m_19879_() + ItemDisplayContext.GROUND.ordinal());
      Lighting.m_84931_();
      BufferSource buffer = mc.m_91269_().m_110104_();
      mc.m_91291_().m_115143_(itemStack, ItemDisplayContext.GROUND, false, new PoseStack(), buffer, 15728880, OverlayTexture.f_118083_, model);
      buffer.m_109911_();
      poseStack.m_85849_();
      RenderSystem.applyModelViewMatrix();
   }
}
