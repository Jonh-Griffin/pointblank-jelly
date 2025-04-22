package com.vicmatskiv.pointblank.util;

import com.vicmatskiv.pointblank.PointBlankJelly;
import com.vicmatskiv.pointblank.network.ClientBoundScriptInvoker;
import com.vicmatskiv.pointblank.network.Network;
import com.vicmatskiv.pointblank.network.ServerBoundScriptInvoker;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Consumer;

public final class Scripts {

    public static void runFunctionOnServer(String functionName, ItemStack stack) {
        Network.networkChannel.sendToServer(new ServerBoundScriptInvoker(functionName, stack));
    }

    public static void runFunctionOnClient(String functionName, ItemStack stack, ServerPlayer player) {
        Network.networkChannel.send(PacketDistributor.PLAYER.with(() -> player), new ClientBoundScriptInvoker(functionName, stack));
    }

    public static <E extends Event> void registerEvent(Consumer<E> event, Class<E> register, boolean forge) {
        if(forge)
            MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, true, register, event);
        else
            PointBlankJelly.modEventBus.addListener(EventPriority.NORMAL, true, register, event);
    }

    public static <E extends Event> void registerForgeEvent(Consumer<E> event, Class<E> register) {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, true, register, event);
    }

    public static <E extends Event> void registerModEvent(Consumer<E> event, Class<E> register) {
        PointBlankJelly.modEventBus.addListener(EventPriority.NORMAL, true, register, event);
    }
}
