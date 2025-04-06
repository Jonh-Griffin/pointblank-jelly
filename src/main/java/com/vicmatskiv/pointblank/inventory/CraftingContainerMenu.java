package com.vicmatskiv.pointblank.inventory;

import com.vicmatskiv.pointblank.Enableable;
import com.vicmatskiv.pointblank.block.entity.PrinterBlockEntity;
import com.vicmatskiv.pointblank.crafting.PointBlankIngredient;
import com.vicmatskiv.pointblank.crafting.PointBlankRecipe;
import com.vicmatskiv.pointblank.registry.ItemRegistry;
import com.vicmatskiv.pointblank.registry.MenuRegistry;
import com.vicmatskiv.pointblank.util.InventoryUtils;
import com.vicmatskiv.pointblank.util.MiscUtil;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.client.searchtree.FullTextSearchTree;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag.Default;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.util.ClientUtils;

public class CraftingContainerMenu extends AbstractContainerMenu {
   private static final int SEARCH_COLS = 6;
   private static final int SEARCH_ROWS = 7;
   private static final int INGREDIENT_COLS = 5;
   private static final int INGREDIENT_ROWS = 2;
   public static final SimpleContainer INGREDIENT_CONTAINER = new SimpleContainer(10);
   public static final SimpleContainer SEARCH_CONTAINER = new SimpleContainer(42);
   private final NonNullList<ItemStack> items;
   private final AbstractContainerMenu inventoryMenu;
   private List<ItemStack> displayItems;
   private PrinterBlockEntity craftingBlockEntity;
   private ContainerData containerData;
   private FullTextSearchTree<ItemStack> searchTree;
   private Player player;

   public CraftingContainerMenu(int containerId, Inventory playerInventory) {
      this(containerId, playerInventory, (PrinterBlockEntity)null, new SimpleContainerData(2));
   }

   public CraftingContainerMenu(int containerId, Inventory playerInventory, PrinterBlockEntity craftingBlockEntity, ContainerData dataAccess) {
      super((MenuType)MenuRegistry.CRAFTING.get(), containerId);
      this.items = NonNullList.m_122779_();
      this.displayItems = new ArrayList();
      this.craftingBlockEntity = craftingBlockEntity;
      this.containerData = dataAccess;
      this.player = playerInventory.f_35978_;
      this.inventoryMenu = playerInventory.f_35978_.f_36095_;
      this.m_38884_(dataAccess);

      int i;
      int j;
      for(i = 0; i < 7; ++i) {
         for(j = 0; j < 6; ++j) {
            SearchSlot slot = new SearchSlot(SEARCH_CONTAINER, i * 6 + j, 9 + j * 18, 18 + i * 18, true);
            this.m_38897_(slot);
         }
      }

      for(i = 0; i < 2; ++i) {
         for(j = 0; j < 5; ++j) {
            IngredientSlot slot = new IngredientSlot(INGREDIENT_CONTAINER, i * 5 + j, 151 + j * 18, 108 + i * 18);
            this.m_38897_(slot);
         }
      }

      Iterator var10 = ItemRegistry.ITEMS.getItemsByName().values().iterator();

      while(true) {
         PointBlankRecipe recipe;
         Enableable e;
         do {
            Item item;
            do {
               if (!var10.hasNext()) {
                  if (MiscUtil.isClientSide(playerInventory.f_35978_)) {
                     this.searchTree = new FullTextSearchTree((itemStack) -> {
                        return itemStack.m_41651_((Player)null, Default.f_256752_.m_257777_()).stream().map((component) -> {
                           return ChatFormatting.m_126649_(component.getString()).trim();
                        }).filter((p_210809_) -> {
                           return !p_210809_.isEmpty();
                        });
                     }, (itemStack) -> {
                        return Stream.of(BuiltInRegistries.f_257033_.m_7981_(itemStack.m_41720_()));
                     }, this.displayItems);
                     this.searchTree.m_214078_();
                  }

                  this.scrollTo(0.0F);
                  return;
               }

               Supplier<? extends Item> itemSupplier = (Supplier)var10.next();
               item = (Item)itemSupplier.get();
               recipe = PointBlankRecipe.getRecipe(playerInventory.f_35978_.m_9236_(), item);
            } while(recipe == null);

            if (!(item instanceof Enableable)) {
               break;
            }

            e = (Enableable)item;
         } while(!e.isEnabled());

         this.displayItems.add(recipe.getInitializedStack());
      }
   }

   public PrinterBlockEntity getWorkstationBlockEntity() {
      return this.craftingBlockEntity;
   }

   public void m_182406_(int slotIndex, int stateId, ItemStack itemStack) {
      super.m_182406_(slotIndex, stateId, itemStack);
   }

   public void m_182410_(int stateId, List<ItemStack> itemStacks, ItemStack carriedStack) {
   }

   public boolean m_6875_(Player player) {
      return true;
   }

   protected int calculateRowCount() {
      return Mth.m_184652_(this.items.size(), 6) - 7;
   }

   public int getRowIndexForScroll(float scroll) {
      return Math.max((int)((double)(scroll * (float)this.calculateRowCount()) + 0.5D), 0);
   }

   public float getScrollForRowIndex(int rowIndex) {
      return Mth.m_14036_((float)rowIndex / (float)this.calculateRowCount(), 0.0F, 1.0F);
   }

   public float subtractInputFromScroll(float scrolloffs, double scroll) {
      return Mth.m_14036_(scrolloffs - (float)(scroll / (double)this.calculateRowCount()), 0.0F, 1.0F);
   }

   public void scrollTo(float scroll) {
      int i = this.getRowIndexForScroll(scroll);

      for(int j = 0; j < 7; ++j) {
         for(int k = 0; k < 6; ++k) {
            int l = k + (j + i) * 6;
            if (l >= 0 && l < this.items.size()) {
               SEARCH_CONTAINER.m_6836_(k + j * 6, (ItemStack)this.items.get(l));
            } else {
               SEARCH_CONTAINER.m_6836_(k + j * 6, ItemStack.f_41583_);
            }
         }
      }

   }

   public boolean canScroll() {
      return this.items.size() > 42;
   }

   public ItemStack m_7648_(Player player, int slotIndex) {
      if (slotIndex >= this.f_38839_.size() - 6 && slotIndex < this.f_38839_.size()) {
         Slot slot = (Slot)this.f_38839_.get(slotIndex);
         if (slot != null && slot.m_6657_()) {
            slot.m_269060_(ItemStack.f_41583_);
         }
      }

      return ItemStack.f_41583_;
   }

   public boolean m_5882_(ItemStack itemStack, Slot slot) {
      return slot.f_40218_ != SEARCH_CONTAINER;
   }

   public boolean m_5622_(Slot p_98653_) {
      return p_98653_.f_40218_ != SEARCH_CONTAINER;
   }

   public ItemStack m_142621_() {
      return this.inventoryMenu.m_142621_();
   }

   public void m_142503_(ItemStack itemStack) {
      this.inventoryMenu.m_142503_(itemStack);
   }

   public void refreshSearchResults(String s) {
      this.items.clear();
      if (s.isEmpty()) {
         this.items.addAll(this.displayItems);
      } else {
         this.items.addAll(this.searchTree.m_6293_(s.toLowerCase(Locale.ROOT)));
      }

      this.scrollTo(0.0F);
   }

   public boolean updateIngredientSlots(PointBlankRecipe selectedItemRecipe) {
      boolean isCraftable = true;
      List<PointBlankIngredient> ingredients = selectedItemRecipe.getPointBlankIngredients();
      if (ingredients.size() <= INGREDIENT_CONTAINER.m_6643_()) {
         int ingredientSlotOffset = SEARCH_CONTAINER.m_6643_();

         for(int i = 0; i < ingredients.size(); ++i) {
            PointBlankIngredient ingredient = (PointBlankIngredient)ingredients.get(i);
            IngredientSlot ingredientSlot = (IngredientSlot)this.f_38839_.get(ingredientSlotOffset + i);
            ClientUtils.getClientPlayer();
            boolean hasIngredient = InventoryUtils.hasIngredient(ClientUtils.getClientPlayer(), ingredient);
            ingredientSlot.setIngredient(ingredient, hasIngredient);
            isCraftable &= hasIngredient;
         }
      }

      return isCraftable;
   }

   public void clearIngredientSlots() {
      int ingredientSlotOffset = SEARCH_CONTAINER.m_6643_();

      for(int i = 0; i < INGREDIENT_CONTAINER.m_6643_(); ++i) {
         IngredientSlot ingredientSlot = (IngredientSlot)this.f_38839_.get(ingredientSlotOffset + i);
         ingredientSlot.setIngredient((PointBlankIngredient)null, true);
      }

      INGREDIENT_CONTAINER.m_6211_();
   }

   public boolean isCreativeSlot(Slot slot) {
      return slot != null && slot.f_40218_ == SEARCH_CONTAINER;
   }

   public boolean isIdle() {
      return PrinterBlockEntity.State.values()[this.containerData.m_6413_(0)] == PrinterBlockEntity.State.IDLE;
   }

   public boolean isCrafting() {
      boolean result = false;
      PrinterBlockEntity.State state = PrinterBlockEntity.State.values()[this.containerData.m_6413_(0)];
      if (state == PrinterBlockEntity.State.CRAFTING) {
         Level level = MiscUtil.getLevel(this.player);
         int playerEntityId = this.containerData.m_6413_(1);
         if (playerEntityId >= 0) {
            result = this.player == level.m_6815_(playerEntityId);
         }
      }

      return result;
   }
}
