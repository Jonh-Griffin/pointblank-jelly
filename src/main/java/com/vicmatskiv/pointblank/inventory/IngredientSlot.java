package com.vicmatskiv.pointblank.inventory;

import com.vicmatskiv.pointblank.crafting.PointBlankIngredient;
import java.util.Collections;
import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class IngredientSlot extends Slot {
   private static final int SHOW_ROLLING_INGREDIENT_DURATION_MILLIS = 800;
   private int slotIndex;
   private PointBlankIngredient ingredient;
   boolean isIngredientAvailable;

   public IngredientSlot(Container container, int slotIndex, int posX, int posY) {
      super(container, slotIndex, posX, posY);
      this.slotIndex = slotIndex;
   }

   public boolean m_280329_() {
      return false;
   }

   public void setIngredient(PointBlankIngredient ingredient, boolean isIngredientAvailable) {
      this.ingredient = ingredient;
      this.isIngredientAvailable = isIngredientAvailable;
      List<ItemStack> itemStacks = ingredient != null ? ingredient.getItemStacks() : Collections.emptyList();
      int itemStackCount = itemStacks.size();
      ItemStack currentStack;
      if (itemStackCount == 1) {
         currentStack = (ItemStack)itemStacks.get(0);
      } else if (itemStackCount > 0) {
         int index = (int)((long)((double)System.currentTimeMillis() / 800.0D) % (long)itemStackCount);
         currentStack = (ItemStack)itemStacks.get(index);
      } else {
         currentStack = ItemStack.f_41583_;
      }

      this.m_5852_(currentStack);
   }

   public PointBlankIngredient getIngredient() {
      return this.ingredient;
   }

   public boolean m_8010_(Player player) {
      return false;
   }

   public boolean m_5857_(ItemStack itemStack) {
      return false;
   }

   public String toString() {
      return String.format("IngredientSlot {index: %d, container: %s}", this.slotIndex, System.identityHashCode(this.f_40218_));
   }

   public boolean isIngredientAvailable() {
      return this.isIngredientAvailable;
   }
}
