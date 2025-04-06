package com.vicmatskiv.pointblank.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.vicmatskiv.pointblank.client.model.BaseBlockModel;
import com.vicmatskiv.pointblank.client.render.layer.BaseModelBlockLayer;
import com.vicmatskiv.pointblank.client.render.layer.GlowingBlockEntityLayer;
import com.vicmatskiv.pointblank.compat.iris.IrisCompat;
import java.util.Iterator;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class BaseModelBlockRenderer<T extends BlockEntity & GeoAnimatable> extends GeoBlockRenderer<T> {
   private BaseBlockModel<T> baseBlockModel;
   private BlockEntity currentBlockEntity;

   public BaseModelBlockRenderer(BaseBlockModel<T> baseBlockModel) {
      super(baseBlockModel);
      this.baseBlockModel = baseBlockModel;
      if (!baseBlockModel.getGlowingParts().isEmpty()) {
         this.addRenderLayer(new GlowingBlockEntityLayer(this));
      }

   }

   public BaseBlockModel<T> getModel() {
      return this.baseBlockModel;
   }

   private boolean shouldRender(String boneName) {
      boolean shouldRender = true;
      Iterator var3 = this.getRenderLayers().iterator();

      while(var3.hasNext()) {
         GeoRenderLayer<T> layer = (GeoRenderLayer)var3.next();
         if (layer instanceof BaseModelBlockLayer) {
            BaseModelBlockLayer<T> baseModelBlockLayer = (BaseModelBlockLayer)layer;
            if (!baseModelBlockLayer.shouldRender(boneName, this.currentBlockEntity)) {
               shouldRender = false;
               break;
            }
         }
      }

      return shouldRender;
   }

   public void m_6922_(BlockEntity animatable, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
      if (!IrisCompat.getInstance().isRenderingShadows()) {
         this.currentBlockEntity = animatable;
         super.m_6922_(animatable, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
      }
   }

   public void renderCubesOfBone(PoseStack poseStack, GeoBone bone, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
      if (this.shouldRender(bone.getName())) {
         super.renderCubesOfBone(poseStack, bone, buffer, packedLight, packedOverlay, red, green, blue, alpha);
      }

   }

   public static record RenderInfo(BaseModelBlockLayer<?> layer, BoneRenderVote mode) {
      public RenderInfo(BaseModelBlockLayer<?> layer, BoneRenderVote mode) {
         this.layer = layer;
         this.mode = mode;
      }

      public BaseModelBlockLayer<?> layer() {
         return this.layer;
      }

      public BoneRenderVote mode() {
         return this.mode;
      }
   }

   public static enum BoneRenderVote {
      ALLOW,
      DENY;

      // $FF: synthetic method
      private static BoneRenderVote[] $values() {
         return new BoneRenderVote[]{ALLOW, DENY};
      }
   }
}
