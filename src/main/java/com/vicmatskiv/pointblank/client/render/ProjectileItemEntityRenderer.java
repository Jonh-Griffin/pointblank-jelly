package com.vicmatskiv.pointblank.client.render;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Axis;
import com.vicmatskiv.pointblank.client.EntityRendererBuilder;
import com.vicmatskiv.pointblank.entity.ProjectileLike;
import com.vicmatskiv.pointblank.util.MiscUtil;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ProjectileItemEntityRenderer<T extends Entity & ProjectileLike> extends EntityRenderer<T> {
   private final ItemRenderer itemRenderer;
   private static ProjectileLike currentProjectile;
   private static Pose currentPose;

   public ProjectileItemEntityRenderer(Context context) {
      super(context);
      this.itemRenderer = context.m_174025_();
   }

   static ProjectileLike getCurrentProjectile() {
      return currentProjectile;
   }

   static Pose getCurrentPose() {
      return currentPose;
   }

   public void m_7392_(T projectile, float yRot, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
      poseStack.m_85836_();
      poseStack.m_252781_(Axis.f_252436_.m_252977_(projectile.m_146908_()));
      poseStack.m_252781_(Axis.f_252529_.m_252977_(180.0F - projectile.m_146909_()));
      currentProjectile = (ProjectileLike)projectile;
      currentPose = poseStack.m_85850_();
      this.itemRenderer.m_269128_(((ProjectileLike)projectile).getItem(), ItemDisplayContext.GROUND, packedLight, OverlayTexture.f_118083_, poseStack, bufferSource, MiscUtil.getLevel(projectile), projectile.m_19879_());
      currentProjectile = null;
      currentPose = null;
      poseStack.m_85849_();
   }

   public ResourceLocation m_5478_(Entity entity) {
      return InventoryMenu.f_39692_;
   }

   public static class Builder<T extends Entity & ProjectileLike> implements EntityRendererBuilder<Builder<T>, T, EntityRenderer<T>> {
      public Builder<T> withJsonObject(JsonObject obj) {
         return null;
      }

      public EntityRenderer<T> build(Context context) {
         return new ProjectileItemEntityRenderer(context);
      }
   }
}
