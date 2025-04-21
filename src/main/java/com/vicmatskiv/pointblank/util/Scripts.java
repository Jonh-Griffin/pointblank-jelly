package com.vicmatskiv.pointblank.util;

import com.vicmatskiv.pointblank.network.ClientBoundScriptInvoker;
import com.vicmatskiv.pointblank.network.Network;
import com.vicmatskiv.pointblank.network.ServerBoundScriptInvoker;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;

public final class Scripts {

    public static void runFunctionOnServer(String functionName, ItemStack stack) {
        Network.networkChannel.sendToServer(new ServerBoundScriptInvoker(functionName, stack));
    }

    public static void runFunctionOnClient(String functionName, ItemStack stack, ServerPlayer player) {
        Network.networkChannel.send(PacketDistributor.PLAYER.with(() -> player), new ClientBoundScriptInvoker(functionName, stack));
    }

}
