package com.vicmatskiv.pointblank.client.render;

import com.vicmatskiv.pointblank.client.GunClientState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public interface RenderListener {
   void onRenderTick(LivingEntity var1, GunClientState var2, ItemStack var3, ItemDisplayContext var4, float var5);
}
