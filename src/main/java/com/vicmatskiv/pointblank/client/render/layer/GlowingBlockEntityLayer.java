package com.vicmatskiv.pointblank.client.render.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.vicmatskiv.pointblank.client.model.BaseBlockModel;
import com.vicmatskiv.pointblank.client.render.BaseModelBlockRenderer;
import com.vicmatskiv.pointblank.client.render.RenderTypeProvider;
import java.util.function.Predicate;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.animatable.GeoAnimatable;

public class GlowingBlockEntityLayer<T extends BlockEntity & GeoAnimatable> extends BaseModelBlockLayer<T> {
   private BaseBlockModel<T> model;
   private ResourceLocation texture;

   public GlowingBlockEntityLayer(BaseModelBlockRenderer<T> renderer) {
      super(renderer);
      this.renderer = renderer;
      this.model = renderer.getModel();
      this.texture = renderer.getTextureLocation(renderer.getAnimatable());
   }

   public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
      RenderTypeProvider renderTypeProvider = RenderTypeProvider.getInstance();
      RenderType glowRenderType = renderTypeProvider.getGlowBlockEntityRenderType(this.texture);
      int packedLight = 240;
      super.render(poseStack, (BlockEntity)animatable, bakedModel, glowRenderType, bufferSource, buffer, partialTick, packedLight, packedOverlay);
   }

   public boolean shouldRender(String boneName, BlockEntity blockEntity) {
      Predicate<BlockEntity> predicate = (Predicate)this.model.getGlowingParts().get(boneName);
      return !this.isRendering || predicate != null && predicate.test(blockEntity);
   }
}
