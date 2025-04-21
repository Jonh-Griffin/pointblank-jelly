package com.vicmatskiv.pointblank.network;

import com.vicmatskiv.pointblank.item.ScriptHolder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerBoundScriptInvoker {
   private String function;
   private ItemStack stack;

   public ServerBoundScriptInvoker(String function, ItemStack stack) {
      this.function = function;
      this.stack = stack;
   }

   public ServerBoundScriptInvoker() {}

   public static void encode(ServerBoundScriptInvoker packet, FriendlyByteBuf buf) {
      buf.writeUtf(packet.function);
      buf.writeItemStack(packet.stack, false);
   }

   public static ServerBoundScriptInvoker decode(FriendlyByteBuf buf) {
      return new ServerBoundScriptInvoker(buf.readUtf(), buf.readItem());
   }

   public static void handle(ServerBoundScriptInvoker packet, Supplier<NetworkEvent.Context> context) {
      context.get().enqueueWork(() -> {
         ServerPlayer player = context.get().getSender();
         if(packet.stack.getItem() instanceof ScriptHolder scriptHolder)
            scriptHolder.invokeFunction(packet.function, player, packet.stack);

      });
      context.get().setPacketHandled(true);
   }
}
