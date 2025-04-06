package com.vicmatskiv.pointblank.util;

import com.vicmatskiv.pointblank.client.GunClientState;
import com.vicmatskiv.pointblank.crafting.PointBlankIngredient;
import com.vicmatskiv.pointblank.item.GunItem;
import java.util.IntSummaryStatistics;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class InventoryUtils {
   public static Tuple<ItemStack, GunClientState> getItemStackByStateId(Player player, UUID gunStateId, int slotIndex) {
      ItemStack targetStack = null;
      GunClientState targetGunState = null;
      ItemStack itemStack = player.m_150109_().m_8020_(slotIndex);
      boolean isOffhand = player.m_21206_() == itemStack;
      if (itemStack.m_41720_() instanceof GunItem) {
         GunClientState gunClientState = GunClientState.getState(player, itemStack, slotIndex, isOffhand);
         if (gunClientState != null && Objects.equals(GunItem.getItemStackId(itemStack), gunStateId)) {
            targetStack = itemStack;
            targetGunState = gunClientState;
         }
      }

      return targetStack != null ? new Tuple(targetStack, targetGunState) : null;
   }

   public static boolean hasIngredient(Player player, PointBlankIngredient ingredient) {
      Stream var10000 = player.m_150109_().f_35974_.stream();
      Objects.requireNonNull(ingredient);
      return ((IntSummaryStatistics)var10000.filter(ingredient::matches).collect(Collectors.summarizingInt(ItemStack::m_41613_))).getSum() >= (long)ingredient.getCount();
   }

   public static boolean removeItem(Player player, Predicate<ItemStack> matchingPredicate, int count) {
      Inventory inventory = player.m_150109_();
      int remainingCount = count;

      for(int i = 0; i < player.m_150109_().f_35974_.size(); ++i) {
         ItemStack inventoryItem = (ItemStack)inventory.f_35974_.get(i);
         if (matchingPredicate.test(inventoryItem)) {
            int availableCount = inventoryItem.m_41613_();
            if (availableCount <= remainingCount) {
               remainingCount -= availableCount;
               inventory.f_35974_.set(i, ItemStack.f_41583_);
            } else {
               inventoryItem.m_41774_(remainingCount);
               remainingCount = 0;
            }

            ((ServerPlayer)inventory.f_35978_).f_8906_.m_9829_(new ClientboundContainerSetSlotPacket(-2, 0, i, inventory.m_8020_(i)));
            if (remainingCount == 0) {
               break;
            }
         }
      }

      return remainingCount <= 0;
   }

   public static int addItem(Player player, Item item, int count) {
      Inventory inventory = player.m_150109_();
      ItemStack stackToAdd = new ItemStack(item, 1);
      int remainingCount = count;
      int i;
      if (stackToAdd.m_41753_()) {
         for(i = 0; i < inventory.f_35974_.size(); ++i) {
            ItemStack inventoryStack = (ItemStack)inventory.f_35974_.get(i);
            if (inventoryStack.m_41720_() == item && inventoryStack.m_41613_() < inventoryStack.m_41741_()) {
               int spaceLeft = inventoryStack.m_41741_() - inventoryStack.m_41613_();
               int itemsToAdd = Math.min(spaceLeft, remainingCount);
               inventoryStack.m_41769_(itemsToAdd);
               ((ServerPlayer)inventory.f_35978_).f_8906_.m_9829_(new ClientboundContainerSetSlotPacket(-2, 0, i, inventory.m_8020_(i)));
               remainingCount -= itemsToAdd;
               if (remainingCount == 0) {
                  return 0;
               }
            }
         }
      }

      for(i = 0; i < inventory.f_35974_.size(); ++i) {
         if (inventory.m_8020_(i).m_41619_()) {
            if (stackToAdd.m_41753_()) {
               int stackSize = Math.min(stackToAdd.m_41741_(), remainingCount);
               inventory.m_6836_(i, new ItemStack(item, stackSize));
               ((ServerPlayer)inventory.f_35978_).f_8906_.m_9829_(new ClientboundContainerSetSlotPacket(-2, 0, i, inventory.m_8020_(i)));
               remainingCount -= stackSize;
            } else {
               inventory.m_6836_(i, stackToAdd.m_41777_());
               ((ServerPlayer)inventory.f_35978_).f_8906_.m_9829_(new ClientboundContainerSetSlotPacket(-2, 0, i, inventory.m_8020_(i)));
               --remainingCount;
            }

            if (remainingCount == 0) {
               return 0;
            }
         }
      }

      return remainingCount;
   }
}
