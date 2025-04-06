package com.vicmatskiv.pointblank.client.controller;

import com.vicmatskiv.pointblank.client.GunClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class PostPassEffectController extends AbstractProceduralAnimationController {
   public PostPassEffectController(long duration) {
      super(duration);
   }

   public void reset() {
      super.reset();
      this.nanoDuration = 300000000L;
      Minecraft mc = Minecraft.m_91087_();
      mc.f_91063_.m_109128_(new ResourceLocation("pointblank", "shaders/post/ripple.json"));
   }

   public void onRenderTick(LivingEntity player, GunClientState state, ItemStack itemStack, ItemDisplayContext itemDisplayContext, float partialTicks) {
      super.onRenderTick(player, state, itemStack, itemDisplayContext, partialTicks);
      if (this.isDone) {
         Minecraft mc = Minecraft.m_91087_();
         PostChain postChain = mc.f_91063_.m_109149_();
         if (postChain != null && postChain.m_110022_().startsWith("pointblank:")) {
            mc.f_91063_.m_109086_();
         }
      }

   }

   public void onGameTick(LivingEntity player, GunClientState gunClientState) {
   }

   public double getProgress() {
      return super.getProgress((GunClientState)null, 0.0F);
   }
}
