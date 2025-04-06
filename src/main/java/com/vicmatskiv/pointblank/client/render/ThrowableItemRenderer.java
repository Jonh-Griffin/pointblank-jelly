package com.vicmatskiv.pointblank.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.vicmatskiv.pointblank.client.ClientEventHandler;
import com.vicmatskiv.pointblank.client.render.layer.GunHandsItemLayer;
import com.vicmatskiv.pointblank.compat.iris.IrisCompat;
import com.vicmatskiv.pointblank.feature.Feature;
import com.vicmatskiv.pointblank.feature.SkinFeature;
import com.vicmatskiv.pointblank.item.ThrowableItem;
import com.vicmatskiv.pointblank.mixin.BakedModelMixin;
import com.vicmatskiv.pointblank.util.MiscUtil;
import java.util.Iterator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.model.SeparateTransformsModel.Baked;
import org.joml.Quaternionf;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.ClientUtils;

public class ThrowableItemRenderer extends GeoItemRenderer<ThrowableItem> implements RenderPassGeoRenderer<ThrowableItem>, RenderApprover {
   public static final String BONE_RIGHTARM = "rightarm";
   public static final String BONE_LEFTARM = "leftarm";
   public static final String BONE_CAMERA = "_camera_";
   private ItemTransforms transforms;
   private final ResourceLocation leftHandModelResource = new ResourceLocation("pointblank", "geo/item/left_arm.geo.json");
   private final ResourceLocation rightHandModelResource = new ResourceLocation("pointblank", "geo/item/right_arm.geo.json");

   public ThrowableItemRenderer(ResourceLocation modelResource) {
      super(new DefaultedItemGeoModel(modelResource));
      this.addRenderLayer(new GunHandsItemLayer(this));
   }

   public ResourceLocation getTextureLocation(ThrowableItem animatable) {
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

   public GeoRenderer<ThrowableItem> getRenderer() {
      return this;
   }

   private BakedGeoModel getLeftHandModel() {
      return (BakedGeoModel)GeckoLibCache.getBakedModels().get(this.leftHandModelResource);
   }

   private BakedGeoModel getRightHandModel() {
      return (BakedGeoModel)GeckoLibCache.getBakedModels().get(this.rightHandModelResource);
   }

   private Player getPlayer(ItemDisplayContext itemDisplayContext) {
      if (itemDisplayContext != ItemDisplayContext.FIRST_PERSON_LEFT_HAND && itemDisplayContext != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND && itemDisplayContext != ItemDisplayContext.GROUND) {
         LivingEntity renderedEntity = ClientEventHandler.getCurrentEntityLiving();
         return renderedEntity instanceof Player ? (Player)renderedEntity : null;
      } else {
         return ClientUtils.getClientPlayer();
      }
   }

   public void m_108829_(ItemStack stack, ItemDisplayContext itemDisplayContext, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
      if (!IrisCompat.getInstance().isRenderingShadows()) {
         Player player = this.getPlayer(itemDisplayContext);
         if (player != null) {
            MultiBufferSource wrappedBufferSource = RenderTypeProvider.getInstance().wrapBufferSource(bufferSource);
            HierarchicalRenderContext hrc = HierarchicalRenderContext.push(stack, itemDisplayContext);

            try {
               this.renderPass(() -> {
                  boolean isFirstPerson = itemDisplayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || itemDisplayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
                  poseStack.m_85836_();
                  ThrowableItem gunItem = (ThrowableItem)stack.m_41720_();
                  GeoModel<ThrowableItem> geoModel = this.getGeoModel();
                  if (isFirstPerson) {
                     this.initTransforms(player, stack, itemDisplayContext);
                     this.adjustFirstPersonPose(stack, gunItem, poseStack, geoModel);
                  }

                  super.m_108829_(stack, itemDisplayContext, poseStack, wrappedBufferSource, packedLight, packedOverlay);
                  poseStack.m_85849_();
               });
            } catch (Throwable var13) {
               if (hrc != null) {
                  try {
                     hrc.close();
                  } catch (Throwable var12) {
                     var13.addSuppressed(var12);
                  }
               }

               throw var13;
            }

            if (hrc != null) {
               hrc.close();
            }

         }
      }
   }

   public void renderPass(Runnable runnablePass) {
      RenderPass.push(this.getRenderPass());

      try {
         runnablePass.run();
      } finally {
         RenderPass.pop();
      }

   }

   private void adjustFirstPersonPose(ItemStack itemStack, ThrowableItem throwableItem, PoseStack poseStack, GeoModel<ThrowableItem> geoModel) {
      if (this.transforms != null) {
         ItemTransform fprt = this.transforms.f_111790_;
         if (fprt != null) {
            float rescale = throwableItem.getModelScale();
            poseStack.m_252880_(0.5F, 0.5F, 0.5F);
            poseStack.m_252781_((new Quaternionf()).rotationXYZ(-fprt.f_111755_.x * 0.017453292F, -fprt.f_111755_.y * 0.017453292F, -fprt.f_111755_.z * 0.017453292F));
            poseStack.m_252880_(-fprt.f_111756_.x, -fprt.f_111756_.y, -fprt.f_111756_.z);
            poseStack.m_252880_(fprt.f_111756_.x * rescale, fprt.f_111756_.y * rescale, fprt.f_111756_.z * rescale);
            poseStack.m_252781_((new Quaternionf()).rotationXYZ(fprt.f_111755_.x * 0.017453292F, fprt.f_111755_.y * 0.017453292F, fprt.f_111755_.z * 0.017453292F));
            poseStack.m_85841_(rescale, rescale, rescale);
            poseStack.m_252880_(-0.5F, -0.5F, -0.5F);
            poseStack.m_252880_(0.48F, -1.12F, -0.72F);
            poseStack.m_252880_(-0.006F, 0.6F, 0.0F);
         }
      }
   }

   private void initTransforms(Player player, ItemStack stack, ItemDisplayContext itemDisplayContext) {
      if (this.transforms == null) {
         Minecraft minecraft = Minecraft.m_91087_();
         BakedModel bakedModel = minecraft.m_91291_().m_174264_(stack, MiscUtil.getLevel(player), player, player.m_19879_() + itemDisplayContext.ordinal());
         if (bakedModel instanceof Baked) {
            BakedModel baseModel = ((BakedModelMixin)bakedModel).getBaseModel();
            this.transforms = baseModel.m_7442_();
         } else {
            this.transforms = bakedModel.m_7442_();
         }
      }

   }

   private boolean shouldRenderBone(String boneName) {
      if (boneName.charAt(0) == '_') {
         return false;
      } else {
         HierarchicalRenderContext current = HierarchicalRenderContext.current();
         ItemStack rootStack = HierarchicalRenderContext.getRoot().getItemStack();
         boolean shouldRender = true;
         Iterator var5 = this.getRenderLayers().iterator();

         while(var5.hasNext()) {
            GeoRenderLayer<ThrowableItem> layer = (GeoRenderLayer)var5.next();
            if (layer instanceof RenderApprover) {
               RenderApprover renderApprover = (RenderApprover)layer;
               RenderPass var10000;
               if (layer instanceof RenderPassProvider) {
                  RenderPassProvider rp = (RenderPassProvider)layer;
                  var10000 = rp.getRenderPass();
               } else {
                  var10000 = null;
               }

               RenderPass renderPass = var10000;
               if (!renderApprover.approveRendering(renderPass, boneName, rootStack, current.getItemStack(), current.getPath(), current.getItemDisplayContext())) {
                  shouldRender = false;
                  break;
               }
            }
         }

         return shouldRender;
      }
   }

   public void renderRecursively(PoseStack poseStack, ThrowableItem animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
      HierarchicalRenderContext current = HierarchicalRenderContext.current();
      ItemStack rootStack = HierarchicalRenderContext.getRoot().getItemStack();
      boolean shouldRender = this.approveRendering(this.getRenderPass(), bone.getName(), rootStack, current.getItemStack(), current.getPath(), current.getItemDisplayContext());
      if (shouldRender) {
         super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
      }

   }

   public void renderCubesOfBone(PoseStack poseStack, GeoBone bone, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
      RenderPass renderPass = RenderPass.current();
      if (this.shouldRenderBone(bone.getName())) {
         switch(renderPass) {
         case HANDS:
            if (bone.getName().equals("rightarm")) {
               this.renderRightArm(poseStack, bone, buffer, packedLight, packedOverlay, red, green, blue, alpha);
            } else if (bone.getName().equals("leftarm")) {
               this.renderLeftArm(poseStack, bone, buffer, packedLight, packedOverlay, red, green, blue, alpha);
            }
            break;
         case MAIN_ITEM:
            if (this.canRenderPart(bone.getName())) {
               super.renderCubesOfBone(poseStack, bone, buffer, packedLight, packedOverlay, red, green, blue, alpha);
            }
            break;
         default:
            throw new IllegalArgumentException("Unexpected value: " + renderPass);
         }

      }
   }

   private void renderLeftArm(PoseStack poseStack, GeoBone bone, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
      BakedGeoModel handsBakedGeoModel = this.getLeftHandModel();
      GeoBone leftArmBone = (GeoBone)handsBakedGeoModel.getBone("leftarm").orElse((Object)null);
      if (leftArmBone != null) {
         poseStack.m_85836_();
         this.applyArmRefTransforms(poseStack, bone, leftArmBone);
         super.renderCubesOfBone(poseStack, leftArmBone, buffer, packedLight, packedOverlay, red, green, blue, alpha);
         poseStack.m_85849_();
      }

   }

   private void renderRightArm(PoseStack poseStack, GeoBone bone, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
      BakedGeoModel handsBakedGeoModel = this.getRightHandModel();
      GeoBone rightArmBone = (GeoBone)handsBakedGeoModel.getBone("rightarm").orElse((Object)null);
      if (rightArmBone != null) {
         poseStack.m_85836_();
         this.applyArmRefTransforms(poseStack, bone, rightArmBone);
         super.renderCubesOfBone(poseStack, rightArmBone, buffer, packedLight, packedOverlay, red, green, blue, alpha);
         poseStack.m_85849_();
      }

   }

   private void applyArmRefTransforms(PoseStack poseStack, GeoBone refBone, GeoBone leftArmBone) {
      GeoCube leftArmBoneCube = (GeoCube)leftArmBone.getCubes().get(0);
      GeoCube refCube = (GeoCube)refBone.getCubes().get(0);
      GeoVertex leftArmBoneVertex = leftArmBoneCube.quads()[0].vertices()[0];
      GeoVertex refVertex = refCube.quads()[0].vertices()[0];
      float dx = refVertex.position().x() - leftArmBoneVertex.position().x();
      float dy = refVertex.position().y() - leftArmBoneVertex.position().y();
      float dz = refVertex.position().z() - leftArmBoneVertex.position().z();
      poseStack.m_252880_(dx, dy, dz);
   }

   public boolean isEffectLayer() {
      return false;
   }

   public RenderType getRenderType() {
      return null;
   }

   public boolean isSupportedItemDisplayContext(ItemDisplayContext context) {
      return true;
   }

   public boolean canRenderPart(String boneName) {
      return !boneName.equals("leftarm") && !boneName.equals("rightarm");
   }

   public RenderPass getRenderPass() {
      return RenderPass.MAIN_ITEM;
   }

   public Class<? extends Feature> getFeatureType() {
      return null;
   }
}
