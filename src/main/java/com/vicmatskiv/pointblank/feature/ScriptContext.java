package com.vicmatskiv.pointblank.feature;

import groovy.lang.Script;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public record ScriptContext(ItemStack gunStack, Player player, Object... extraArgs) {
    public void setBindingContext(Script script) {
        script.setProperty("context", this);
    }
}
