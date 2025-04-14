package com.vicmatskiv.pointblank.client.render;

import com.google.common.eventbus.Subscribe;
import com.vicmatskiv.pointblank.item.ArmorItem;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.util.RenderUtils;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class GeckolibArmorRender {

}
