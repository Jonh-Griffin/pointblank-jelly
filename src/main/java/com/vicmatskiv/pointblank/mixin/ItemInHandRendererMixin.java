package com.vicmatskiv.pointblank.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.vicmatskiv.pointblank.event.RenderHandEvent;
import com.vicmatskiv.pointblank.item.GunItem;
import com.vicmatskiv.pointblank.item.ThrowableItem;
import net.minecraft.client.Camera;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ItemInHandRenderer.class})
public class ItemInHandRendererMixin {
   @Inject(
      method = {"renderArmWithItem"},
      cancellable = true,
      at = {@At("HEAD")}
   )
   public void onRenderArmWithItem(AbstractClientPlayer player, float p_109373_, float p_109374_, InteractionHand hand, float p_109376_, ItemStack itemStack, float p_109378_, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, CallbackInfo callbackInfo) {
      if (itemStack.m_41720_() instanceof GunItem || itemStack.m_41720_() instanceof ThrowableItem) {
         boolean flag = hand == InteractionHand.MAIN_HAND;
         HumanoidArm humanoidarm = flag ? player.m_5737_() : player.m_5737_().m_20828_();
         boolean isRightHand = humanoidarm == HumanoidArm.RIGHT;
         ItemInHandRenderer itemInHandRenderer = (ItemInHandRenderer)this;
         poseStack.m_85836_();
         itemInHandRenderer.m_269530_(player, itemStack, isRightHand ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND, !isRightHand, poseStack, bufferSource, packedLight);
         poseStack.m_85849_();
         callbackInfo.cancel();
      }

   }

   @Inject(
      method = {"renderHandsWithItems"},
      at = {@At("HEAD")}
   )
   private void onRenderHandsWithItems(float p_109315_, PoseStack poseStack, BufferSource bufferSource, LocalPlayer player, int p_109319_, CallbackInfo callbackInfo) {
      RenderHandEvent event = new RenderHandEvent.Pre(poseStack, (Camera)null, 0.0F);
      MinecraftForge.EVENT_BUS.post(event);
   }
}
