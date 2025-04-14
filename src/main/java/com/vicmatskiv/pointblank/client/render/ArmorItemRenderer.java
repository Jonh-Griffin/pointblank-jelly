package com.vicmatskiv.pointblank.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.vicmatskiv.pointblank.client.controller.GlowAnimationController;
import com.vicmatskiv.pointblank.client.render.layer.AttachmentLayer;
import com.vicmatskiv.pointblank.client.render.layer.GlowingItemLayer;
import com.vicmatskiv.pointblank.feature.*;
import com.vicmatskiv.pointblank.item.ArmorItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;

import java.util.Collection;
import java.util.List;

public class ArmorItemRenderer extends GeoArmorRenderer<ArmorItem> implements RenderPassGeoRenderer<ArmorItem> {

    public ArmorItemRenderer(ResourceLocation assetSubpath, List<GlowAnimationController.Builder> glowEffectBuilders) {
        super(new DefaultedItemGeoModel<>(assetSubpath));
        this.addRenderLayer(new AttachmentLayer<>(this));

        for(GlowAnimationController.Builder glowEffectBuilder : glowEffectBuilders) {
            ResourceLocation glowTexture = glowEffectBuilder.getTexture();
            if (glowTexture == null) {
                glowTexture = this.getGeoModel().getTextureResource(this.animatable);
            }

            this.addRenderLayer(new GlowingItemLayer<>(this, glowEffectBuilder.getEffectId(), glowTexture));
        }
    }

    public boolean approveRendering(String boneName, ItemStack rootStack) {
        if(boneName.charAt(0) == '_') return false;

        Collection<Feature> features = ((ArmorItem)rootStack.getItem()).getFeatures();
        ConditionContext conditionContext = new ConditionContext(null, rootStack, currentStack, null, null);

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
    public void defaultRender(PoseStack poseStack, ArmorItem animatable, MultiBufferSource bufferSource, @Nullable RenderType renderType, @Nullable VertexConsumer buffer, float yaw, float partialTick, int packedLight) {
        try (HierarchicalRenderContext hrc = HierarchicalRenderContext.push(getCurrentStack(), ItemDisplayContext.FIXED)) {
            super.defaultRender(poseStack, animatable, bufferSource, renderType, buffer, yaw, partialTick, packedLight);
        }
    }

    @Override
    public void renderRecursively(PoseStack poseStack, ArmorItem animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        if(approveRendering(bone.getName(), this.currentStack))
            super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

    public ResourceLocation getTextureLocation(ArmorItem animatable) {
        ResourceLocation texture = null;
        HierarchicalRenderContext hrc = HierarchicalRenderContext.getRoot();
        if (hrc != null) {
            ItemStack itemStack = hrc.getItemStack();
            texture = SkinFeature.getTexture(itemStack);
        }

        if (texture == null) {
            texture = super.getTextureLocation(animatable);
        }
        return texture;
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
