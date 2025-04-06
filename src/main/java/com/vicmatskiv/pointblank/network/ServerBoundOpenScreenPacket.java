package com.vicmatskiv.pointblank.network;

import com.vicmatskiv.pointblank.attachment.AttachmentHost;
import com.vicmatskiv.pointblank.inventory.AttachmentContainerMenu;
import com.vicmatskiv.pointblank.inventory.CraftingContainerMenu;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.NetworkEvent.Context;

public class ServerBoundOpenScreenPacket {
   private ScreenType screenType;

   public ServerBoundOpenScreenPacket(ScreenType sreenType) {
      this.screenType = sreenType;
   }

   public ServerBoundOpenScreenPacket() {
   }

   public static void encode(ServerBoundOpenScreenPacket packet, FriendlyByteBuf buf) {
      buf.m_130068_(packet.screenType);
   }

   public static ServerBoundOpenScreenPacket decode(FriendlyByteBuf buf) {
      return new ServerBoundOpenScreenPacket((ScreenType)buf.m_130066_(ScreenType.class));
   }

   public static void handle(ServerBoundOpenScreenPacket packet, Supplier<Context> context) {
      ((Context)context.get()).enqueueWork(() -> {
         ServerPlayer player = ((Context)context.get()).getSender();
         switch(packet.screenType) {
         case CRAFTING:
            NetworkHooks.openScreen(player, new SimpleMenuProvider((windowId, playerInventory, p) -> {
               return new CraftingContainerMenu(windowId, playerInventory);
            }, Component.m_237115_("screen.pointblank.crafting")));
            break;
         case ATTACHMENTS:
            ItemStack heldItem = player.m_21205_();
            if (player != null && heldItem.m_41720_() instanceof AttachmentHost) {
               NetworkHooks.openScreen(player, new SimpleMenuProvider((windowId, playerInventory, p) -> {
                  return new AttachmentContainerMenu(windowId, playerInventory, heldItem);
               }, Component.m_237115_("screen.pointblank.attachments")));
            }
         }

      });
      ((Context)context.get()).setPacketHandled(true);
   }

   public static enum ScreenType {
      ATTACHMENTS,
      CRAFTING;

      // $FF: synthetic method
      private static ScreenType[] $values() {
         return new ScreenType[]{ATTACHMENTS, CRAFTING};
      }
   }
}
