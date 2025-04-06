package com.vicmatskiv.pointblank.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.vicmatskiv.pointblank.client.model.BaseBlockModel;
import com.vicmatskiv.pointblank.client.render.layer.BaseModelBlockLayer;
import com.vicmatskiv.pointblank.client.render.layer.GlowingBlockEntityLayer;
import com.vicmatskiv.pointblank.compat.iris.IrisCompat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class BaseModelBlockRenderer<T extends BlockEntity & GeoAnimatable> extends GeoBlockRenderer<T> {
   private final BaseBlockModel<T> baseBlockModel;
   private BlockEntity currentBlockEntity;

   public BaseModelBlockRenderer(BaseBlockModel<T> baseBlockModel) {
      super(baseBlockModel);
      this.baseBlockModel = baseBlockModel;
      if (!baseBlockModel.getGlowingParts().isEmpty()) {
         this.addRenderLayer(new GlowingBlockEntityLayer<>(this));
      }

   }

   public BaseBlockModel<T> getModel() {
      return this.baseBlockModel;
   }

   private boolean shouldRender(String boneName) {
      boolean shouldRender = true;

      for(GeoRenderLayer<T> layer : this.getRenderLayers()) {
         if (layer instanceof BaseModelBlockLayer<T> baseModelBlockLayer) {
            if (!baseModelBlockLayer.shouldRender(boneName, this.currentBlockEntity)) {
               shouldRender = false;
               break;
            }
         }
      }

      return shouldRender;
   }

   public void render(BlockEntity animatable, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
      if (!IrisCompat.getInstance().isRenderingShadows()) {
         this.currentBlockEntity = animatable;
         super.render(animatable, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
      }
   }

   public void renderCubesOfBone(PoseStack poseStack, GeoBone bone, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
      if (this.shouldRender(bone.getName())) {
         super.renderCubesOfBone(poseStack, bone, buffer, packedLight, packedOverlay, red, green, blue, alpha);
      }

   }

   public enum BoneRenderVote {
      ALLOW,
      DENY;

      BoneRenderVote() {
      }
   }

   public record RenderInfo(BaseModelBlockLayer<?> layer, BoneRenderVote mode) {

       public BaseModelBlockLayer<?> layer() {
         return this.layer;
      }

      public BoneRenderVote mode() {
         return this.mode;
      }
   }
}
