package com.vicmatskiv.pointblank.network;

import com.vicmatskiv.pointblank.block.entity.PrinterBlockEntity;
import com.vicmatskiv.pointblank.inventory.CraftingContainerMenu;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.NetworkEvent.Context;

public class CraftingRequestPacket {
   private RequestType requestType;
   private ResourceLocation recipeId;

   public CraftingRequestPacket(RequestType requestType, ResourceLocation recipeId) {
      this.requestType = requestType;
      this.recipeId = recipeId;
   }

   public CraftingRequestPacket() {
   }

   public static void encode(CraftingRequestPacket packet, FriendlyByteBuf buf) {
      buf.m_130068_(packet.requestType);
      buf.m_130085_(packet.recipeId);
   }

   public static CraftingRequestPacket decode(FriendlyByteBuf buf) {
      return new CraftingRequestPacket((RequestType)buf.m_130066_(RequestType.class), buf.m_130281_());
   }

   public static void handle(CraftingRequestPacket packet, Supplier<Context> context) {
      ((Context)context.get()).enqueueWork(() -> {
         ServerPlayer player = ((Context)context.get()).getSender();
         AbstractContainerMenu patt1720$temp = player.f_36096_;
         if (patt1720$temp instanceof CraftingContainerMenu) {
            CraftingContainerMenu containerMenu = (CraftingContainerMenu)patt1720$temp;
            PrinterBlockEntity craftingBlockEntity = containerMenu.getWorkstationBlockEntity();
            if (packet.requestType == RequestType.START_CRAFTING) {
               PrinterBlockEntity.CraftingEventHandler eventHandler = new PrinterBlockEntity.CraftingEventHandler() {
                  public void onCraftingCompleted(Player player, ItemStack craftingItemStack, boolean isAddedToInventory) {
                     Network.networkChannel.send(PacketDistributor.PLAYER.with(() -> {
                        return (ServerPlayer)player;
                     }), new CraftingResponsePacket(craftingItemStack, CraftingResponsePacket.CraftingResult.COMPLETED, isAddedToInventory));
                  }

                  public void onCraftingFailed(Player player, ItemStack craftingItemStack, Exception craftingException) {
                     Network.networkChannel.send(PacketDistributor.PLAYER.with(() -> {
                        return (ServerPlayer)player;
                     }), new CraftingResponsePacket(ItemStack.f_41583_, CraftingResponsePacket.CraftingResult.FAILED, false));
                  }
               };
               if (!craftingBlockEntity.tryCrafting(player, packet.recipeId, eventHandler)) {
                  Network.networkChannel.send(PacketDistributor.PLAYER.with(() -> {
                     return player;
                  }), new CraftingResponsePacket(ItemStack.f_41583_, CraftingResponsePacket.CraftingResult.FAILED, false));
               }
            } else if (packet.requestType == RequestType.CANCEL_CRAFTING) {
               craftingBlockEntity.cancelCrafting(player, packet.recipeId);
            }
         }

      });
      ((Context)context.get()).setPacketHandled(true);
   }

   public static enum RequestType {
      START_CRAFTING,
      CANCEL_CRAFTING;

      // $FF: synthetic method
      private static RequestType[] $values() {
         return new RequestType[]{START_CRAFTING, CANCEL_CRAFTING};
      }
   }
}
