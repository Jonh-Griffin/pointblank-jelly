package com.vicmatskiv.pointblank.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.vicmatskiv.pointblank.client.render.layer.AttachmentLayer;
import com.vicmatskiv.pointblank.feature.ConditionContext;
import com.vicmatskiv.pointblank.feature.Feature;
import com.vicmatskiv.pointblank.feature.PartVisibilityFeature;
import com.vicmatskiv.pointblank.item.ArmorItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.checkerframework.checker.units.qual.A;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;

import java.util.Collection;

public class ArmorInHandRenderer extends GeoItemRenderer<ArmorItem> implements RenderPassGeoRenderer<ArmorItem> {
    public ArmorInHandRenderer(ResourceLocation assetPath) {
        super(new DefaultedItemGeoModel<>(assetPath));

        this.addRenderLayer(new AttachmentLayer<>(this));
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext transformType, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        try (HierarchicalRenderContext hrc = HierarchicalRenderContext.push(stack, transformType)) {
            poseStack.pushPose();
            switch (((ArmorItem) stack.getItem()).getEquipmentSlot()) {
                case HEAD -> poseStack.translate(0, -1.5, 0);
                case CHEST -> poseStack.translate(0, -1, 0);
                case LEGS -> poseStack.translate(0, -0.5, 0);
            }
            super.renderByItem(stack, transformType, poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.popPose();
        }
    }

    @Override
    public void renderRecursively(PoseStack poseStack, ArmorItem animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        if(approveRendering(bone.getName(), this.currentItemStack))
            super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

    public boolean approveRendering(String boneName, ItemStack rootStack) {
        Collection<Feature> features = ((ArmorItem)rootStack.getItem()).getFeatures();
        ConditionContext conditionContext = new ConditionContext(null, rootStack, rootStack, null, null);

        for(Feature feature : features) {
            if(feature instanceof PartVisibilityFeature visibilityFeature) {
                if(visibilityFeature.isEnabled(rootStack)) {
                    if (!visibilityFeature.isPartVisible(rootStack.getItem(), boneName, conditionContext)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    @Override
    public GeoRenderer<ArmorItem> getRenderer() {
        return this;
    }

    @Override
    public boolean isEffectLayer() {
        return false;
    }

    @Override
    public RenderType getRenderType() {
        return null;
    }

    @Override
    public boolean isSupportedItemDisplayContext(ItemDisplayContext var1) {
        return false;
    }

    @Override
    public RenderPass getRenderPass() {
        return RenderPass.MAIN_ITEM;
    }
}
