package com.vicmatskiv.pointblank.client.render.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.vicmatskiv.pointblank.client.render.HierarchicalRenderContext;
import com.vicmatskiv.pointblank.client.render.RenderApprover;
import com.vicmatskiv.pointblank.client.render.RenderPass;
import com.vicmatskiv.pointblank.client.render.RenderPassRenderer;
import com.vicmatskiv.pointblank.feature.Feature;
import com.vicmatskiv.pointblank.feature.FeatureProvider;
import java.util.Collection;
import java.util.Collections;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.Item;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public abstract class FeaturePassLayer<T extends GeoAnimatable> extends GeoRenderLayer<T> implements RenderPassRenderer<T>, RenderApprover {
   protected static final Collection<String> ALL_PARTS = Collections.emptySet();
   private RenderPass renderPass;
   private Class<? extends Feature> featureType;
   private boolean isEffectLayer;
   private Collection<String> renderedParts;
   protected Object effectId;

   public FeaturePassLayer(GeoRenderer<T> renderer, Class<? extends Feature> featureType, RenderPass renderPass, Collection<String> renderedParts, boolean isEffectLayer, Object effectId) {
      super(renderer);
      this.featureType = featureType;
      this.renderPass = renderPass;
      this.isEffectLayer = isEffectLayer;
      this.effectId = effectId;
      this.renderedParts = renderedParts != ALL_PARTS ? Collections.unmodifiableCollection(renderedParts) : ALL_PARTS;
   }

   public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
      this.renderPass(() -> {
         RenderType renderType2 = this.getRenderType();
         this.render(this.getDefaultBakedModel(animatable), poseStack, bufferSource, animatable, renderType2, bufferSource.m_6299_(renderType2), partialTick, packedLight, OverlayTexture.f_118083_, 1.0F, 1.0F, 1.0F, 1.0F);
      });
   }

   public RenderPass getRenderPass() {
      return this.renderPass;
   }

   public boolean canRenderPart(String partName) {
      return this.renderedParts == ALL_PARTS || this.renderedParts.contains(partName);
   }

   public Class<? extends Feature> getFeatureType() {
      return this.featureType;
   }

   public boolean isEffectLayer() {
      return this.isEffectLayer;
   }

   public Object getEffectId() {
      return this.effectId;
   }

   public static <F extends Feature> F getFeature(Class<F> featureType) {
      HierarchicalRenderContext hrc = HierarchicalRenderContext.current();
      if (hrc != null) {
         Item var3 = hrc.getItemStack().m_41720_();
         if (var3 instanceof FeatureProvider) {
            FeatureProvider fp = (FeatureProvider)var3;
            HierarchicalRenderContext root = HierarchicalRenderContext.getRoot();
            F feature = fp.getFeature(featureType);
            if (feature != null && feature.isEnabledForAttachment(root.getItemStack(), hrc.getItemStack())) {
               return feature;
            }
         }
      }

      return null;
   }
}
