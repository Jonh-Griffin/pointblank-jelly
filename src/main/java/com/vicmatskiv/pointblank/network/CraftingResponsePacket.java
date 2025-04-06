package com.vicmatskiv.pointblank.network;

import com.vicmatskiv.pointblank.client.ClientEventHandler;
import com.vicmatskiv.pointblank.client.gui.CraftingScreen;
import com.vicmatskiv.pointblank.client.gui.NotificationToast;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent.Context;

public class CraftingResponsePacket {
   private ItemStack itemStack;
   private CraftingResult result;
   private boolean isAddedToInventory;

   public CraftingResponsePacket(ItemStack itemStack, CraftingResult result, boolean isAddedToInventory) {
      this.itemStack = itemStack;
      this.result = result;
      this.isAddedToInventory = isAddedToInventory;
   }

   public CraftingResponsePacket() {
   }

   public static void encode(CraftingResponsePacket packet, FriendlyByteBuf buf) {
      buf.writeItemStack(packet.itemStack, false);
      buf.m_130068_(packet.result);
      buf.writeBoolean(packet.isAddedToInventory);
   }

   public static CraftingResponsePacket decode(FriendlyByteBuf buf) {
      return new CraftingResponsePacket(buf.m_130267_(), (CraftingResult)buf.m_130066_(CraftingResult.class), buf.readBoolean());
   }

   public static void handle(CraftingResponsePacket packet, Supplier<Context> context) {
      ((Context)context.get()).enqueueWork(() -> {
         ClientEventHandler.runSyncTick(() -> {
            Component message = null;
            switch(packet.result) {
            case COMPLETED:
               if (packet.isAddedToInventory) {
                  message = Component.m_237115_("").m_7220_(packet.itemStack.m_41720_().m_7626_(packet.itemStack)).m_130946_(" ").m_7220_(Component.m_237115_("message.pointblank.added_to_the_inventory")).m_130946_("!");
               } else {
                  message = Component.m_237115_("").m_7220_(packet.itemStack.m_41720_().m_7626_(packet.itemStack)).m_130946_(" ").m_7220_(Component.m_237115_("message.pointblank.dropped_on_the_gound")).m_130946_("!");
               }
               break;
            case FAILED:
               Minecraft mc = Minecraft.m_91087_();
               Screen patt2905$temp = mc.f_91080_;
               if (patt2905$temp instanceof CraftingScreen) {
                  CraftingScreen craftingScreen = (CraftingScreen)patt2905$temp;
                  craftingScreen.cancelCrafting();
               }

               message = Component.m_237115_("message.pointblank.failed_to_craft");
               break;
            case CANCELLED:
               message = Component.m_237115_("").m_7220_(packet.itemStack.m_41720_().m_7626_(packet.itemStack)).m_130946_(" ").m_7220_(Component.m_237115_("message.pointblank.cancelled_to_craft"));
            }

            if (message != null) {
               notifyUser(message);
            }

         });
      });
      ((Context)context.get()).setPacketHandled(true);
   }

   @OnlyIn(Dist.CLIENT)
   private static void notifyUser(Component message) {
      Minecraft.m_91087_().m_91300_().m_94922_(new NotificationToast(message, 3000L));
   }

   public static enum CraftingResult {
      COMPLETED,
      FAILED,
      CANCELLED;

      // $FF: synthetic method
      private static CraftingResult[] $values() {
         return new CraftingResult[]{COMPLETED, FAILED, CANCELLED};
      }
   }
}
