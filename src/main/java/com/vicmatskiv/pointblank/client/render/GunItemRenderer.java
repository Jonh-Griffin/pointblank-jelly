package com.vicmatskiv.pointblank.client.render;

import com.google.common.base.Objects;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.datafixers.util.Pair;
import com.vicmatskiv.pointblank.Config;
import com.vicmatskiv.pointblank.client.BiDirectionalInterpolator;
import com.vicmatskiv.pointblank.client.ClientEventHandler;
import com.vicmatskiv.pointblank.client.ClientSystem;
import com.vicmatskiv.pointblank.client.GunClientState;
import com.vicmatskiv.pointblank.client.GunStatePoseProvider;
import com.vicmatskiv.pointblank.client.controller.GlowAnimationController;
import com.vicmatskiv.pointblank.client.controller.RotationAnimationController;
import com.vicmatskiv.pointblank.client.effect.EffectRenderContext;
import com.vicmatskiv.pointblank.client.effect.MuzzleFlashEffect;
import com.vicmatskiv.pointblank.client.gui.AttachmentManagerScreen;
import com.vicmatskiv.pointblank.client.model.GunGeoModel;
import com.vicmatskiv.pointblank.client.render.layer.AttachmentLayer;
import com.vicmatskiv.pointblank.client.render.layer.GlowingItemLayer;
import com.vicmatskiv.pointblank.client.render.layer.GunHandsItemLayer;
import com.vicmatskiv.pointblank.client.render.layer.MuzzleFlashItemLayer;
import com.vicmatskiv.pointblank.client.render.layer.PipItemLayer;
import com.vicmatskiv.pointblank.client.render.layer.ReticleItemLayer;
import com.vicmatskiv.pointblank.client.uv.SpriteUVProvider;
import com.vicmatskiv.pointblank.compat.iris.IrisCompat;
import com.vicmatskiv.pointblank.feature.ActiveMuzzleFeature;
import com.vicmatskiv.pointblank.feature.AimingFeature;
import com.vicmatskiv.pointblank.feature.ConditionContext;
import com.vicmatskiv.pointblank.feature.Feature;
import com.vicmatskiv.pointblank.feature.Features;
import com.vicmatskiv.pointblank.feature.PartVisibilityFeature;
import com.vicmatskiv.pointblank.feature.SkinFeature;
import com.vicmatskiv.pointblank.item.GunItem;
import com.vicmatskiv.pointblank.mixin.BakedModelMixin;
import com.vicmatskiv.pointblank.registry.EffectRegistry;
import com.vicmatskiv.pointblank.util.MiscUtil;
import java.nio.IntBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.SeparateTransformsModel.Baked;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL30;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.ClientUtils;
import software.bernie.geckolib.util.RenderUtils;

@OnlyIn(Dist.CLIENT)
public class GunItemRenderer extends GeoItemRenderer<GunItem> implements RenderPassGeoRenderer<GunItem>, RenderApprover {
   public static final String BONE_SCOPE = "scope";
   public static final String BONE_RETICLE = "reticle";
   public static final String BONE_SCOPE_PIP = "scopepip";
   public static final String BONE_RIGHTARM = "rightarm";
   public static final String BONE_LEFTARM = "leftarm";
   public static final String BONE_MUZZLE = "muzzle";
   public static final String BONE_MUZZLE2 = "muzzle2";
   public static final String BONE_MUZZLE3 = "muzzle3";
   public static final String BONE_MUZZLEFLASH = "muzzleflash";
   public static final String BONE_MUZZLEFLASH2 = "muzzleflash2";
   public static final String BONE_MUZZLEFLASH3 = "muzzleflash3";
   public static final String BONE_CAMERA = "_camera_";
   public static final float DEFAULT_MAX_ANGULAR_RETICLE_OFFSET = Mth.m_14089_(0.08726646F);
   public static final float DEFAULT_MAX_ANGULAR_RETICLE_OFFSET_NOT_AIMED = Mth.m_14089_(0.034906585F);
   private GunClientState gunClientState;
   private boolean hasScopeOverlay;
   private ItemTransforms transforms;
   private ResourceLocation leftHandModelResource = new ResourceLocation("pointblank", "geo/item/left_arm.geo.json");
   private ResourceLocation rightHandModelResource = new ResourceLocation("pointblank", "geo/item/right_arm.geo.json");
   private ResourceLocation reticleModelResource = new ResourceLocation("pointblank", "geo/item/reticle.geo.json");
   private boolean useCustomGlowingTexture;
   private Set<Direction> glowDirections;
   private Supplier<SpriteUVProvider> glowingSpriteUVProviderSupplier;
   private float glowingProgress;

   public GunItemRenderer(ResourceLocation modelResource, List<ResourceLocation> fallbackAnimations, List<GlowAnimationController.Builder> glowEffectBuilders) {
      super(new GunGeoModel(modelResource, fallbackAnimations));
      this.addRenderLayer(new AttachmentLayer(this));

      GlowAnimationController.Builder glowEffectBuilder;
      ResourceLocation glowTexture;
      for(Iterator var4 = glowEffectBuilders.iterator(); var4.hasNext(); this.addRenderLayer(new GlowingItemLayer(this, glowEffectBuilder.getEffectId(), glowTexture))) {
         glowEffectBuilder = (GlowAnimationController.Builder)var4.next();
         glowTexture = glowEffectBuilder.getTexture();
         if (glowTexture == null) {
            glowTexture = this.getGeoModel().getTextureResource((GunItem)this.animatable);
         }
      }

      this.addRenderLayer(new GunHandsItemLayer(this));
      this.addRenderLayer(new PipItemLayer(this));
      this.addRenderLayer(new ReticleItemLayer(this));
      this.addRenderLayer(new MuzzleFlashItemLayer(this));
   }

   public ResourceLocation getTextureLocation(GunItem animatable) {
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

   public GeoRenderer<GunItem> getRenderer() {
      return this;
   }

   public GunClientState getGunClientState() {
      return this.gunClientState;
   }

   private BakedGeoModel getLeftHandModel() {
      return (BakedGeoModel)GeckoLibCache.getBakedModels().get(this.leftHandModelResource);
   }

   private BakedGeoModel getRightHandModel() {
      return (BakedGeoModel)GeckoLibCache.getBakedModels().get(this.rightHandModelResource);
   }

   private BakedGeoModel getReticleModel() {
      return (BakedGeoModel)GeckoLibCache.getBakedModels().get(this.reticleModelResource);
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
         Minecraft mc = Minecraft.m_91087_();
         if (!(mc.f_91080_ instanceof AttachmentManagerScreen) || itemDisplayContext == ItemDisplayContext.GROUND) {
            Player player = this.getPlayer(itemDisplayContext);
            if (player != null) {
               Minecraft.m_91087_().m_91385_().enableStencil();
               int depthTextureId = GL30.glGetFramebufferAttachmentParameteri(36160, 36096, 36049);
               int stencilTextureId = GL30.glGetFramebufferAttachmentParameteri(36160, 36128, 36048);
               if (depthTextureId != 0 && stencilTextureId == 0) {
                  GL30.glBindTexture(3553, depthTextureId);
                  int dataType = GL30.glGetTexLevelParameteri(3553, 0, 35862);
                  if (dataType == 35863) {
                     int width = GL30.glGetTexLevelParameteri(3553, 0, 4096);
                     int height = GL30.glGetTexLevelParameteri(3553, 0, 4097);
                     GlStateManager._texImage2D(3553, 0, 35056, width, height, 0, 34041, 34042, (IntBuffer)null);
                     GlStateManager._glFramebufferTexture2D(36160, 33306, 3553, depthTextureId, 0);
                  }
               }

               MultiBufferSource wrappedBufferSource = RenderTypeProvider.getInstance().wrapBufferSource(bufferSource);
               HierarchicalRenderContext hrc = HierarchicalRenderContext.push(stack, itemDisplayContext);

               try {
                  this.renderPass(() -> {
                     int slotIndex = player.m_150109_().m_36030_(stack);
                     boolean isOffhand = player != null && player.m_21206_() == stack;
                     GunClientState state = GunClientState.getState(player, stack, slotIndex, isOffhand);
                     if (state != null) {
                        GunStatePoseProvider.getInstance().clear(state.getId());
                     }

                     boolean isFirstPerson = itemDisplayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || itemDisplayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
                     poseStack.m_85836_();
                     GunItem gunItem = (GunItem)stack.m_41720_();
                     this.hasScopeOverlay = gunItem.getScopeOverlay() != null;
                     GeoModel<GunItem> geoModel = this.getGeoModel();
                     if (isFirstPerson) {
                        GeoBone scopeBone = (GeoBone)geoModel.getBone("scope").orElse((Object)null);
                        this.initTransforms(player, stack, itemDisplayContext);
                        this.adjustFirstPersonPose(stack, gunItem, state, poseStack, geoModel, scopeBone);
                     }

                     this.gunClientState = state;
                     super.m_108829_(stack, itemDisplayContext, poseStack, wrappedBufferSource, packedLight, packedOverlay);
                     poseStack.m_85849_();
                  });
               } catch (Throwable var16) {
                  if (hrc != null) {
                     try {
                        hrc.close();
                     } catch (Throwable var15) {
                        var16.addSuppressed(var15);
                     }
                  }

                  throw var16;
               }

               if (hrc != null) {
                  hrc.close();
               }

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

   private void adjustFirstPersonPose(ItemStack itemStack, GunItem gunItem, GunClientState state, PoseStack poseStack, GeoModel<GunItem> geoModel, GeoBone scopeBone) {
      if (this.transforms != null) {
         ItemTransform fprt = this.transforms.f_111790_;
         if (fprt != null) {
            float aimingProgress = 0.0F;
            if (state != null) {
               BiDirectionalInterpolator aimingController = (BiDirectionalInterpolator)state.getAnimationController("aiming");
               aimingProgress = (float)aimingController.getValue();
            }

            float v = 1.0F - aimingProgress;
            float rescale = gunItem.getModelScale();
            poseStack.m_252880_(0.5F, 0.5F, 0.5F);
            poseStack.m_252781_((new Quaternionf()).rotationXYZ(-fprt.f_111755_.x * 0.017453292F, -fprt.f_111755_.y * 0.017453292F, -fprt.f_111755_.z * 0.017453292F));
            poseStack.m_252880_(-fprt.f_111756_.x, -fprt.f_111756_.y, -fprt.f_111756_.z);
            poseStack.m_252880_(fprt.f_111756_.x * rescale, fprt.f_111756_.y * rescale, fprt.f_111756_.z * rescale);
            poseStack.m_252781_((new Quaternionf()).rotationXYZ(v * fprt.f_111755_.x * 0.017453292F, v * fprt.f_111755_.y * 0.017453292F, v * fprt.f_111755_.z * 0.017453292F));
            poseStack.m_252880_(aimingProgress * -fprt.f_111756_.x * rescale, aimingProgress * -fprt.f_111756_.y * rescale, aimingProgress * -fprt.f_111756_.z * rescale);
            AimingFeature.applyAimingPosition(itemStack, poseStack, rescale, aimingProgress);
            poseStack.m_85841_(rescale, rescale, rescale);
            poseStack.m_252880_(-0.5F, -0.5F, -0.5F);
            float curve = Mth.m_14031_(3.1415927F * aimingProgress);
            double aimingCurveX = gunItem.getAimingCurveX();
            double aimingCurveY = gunItem.getAimingCurveY();
            double aimingCurveZ = gunItem.getAimingCurveZ();
            double aimingCurvePitch = gunItem.getAimingCurvePitch();
            double aimingCurveYaw = gunItem.getAimingCurveYaw();
            double aimingCurveRoll = gunItem.getAimingCurveRoll();
            poseStack.m_85837_((double)curve * aimingCurveX, (double)curve * aimingCurveY, (double)curve * aimingCurveZ);
            poseStack.m_252781_(new Quaternionf((double)curve * aimingCurvePitch * 0.01745329238474369D, (double)curve * aimingCurveYaw * 0.01745329238474369D, (double)curve * aimingCurveRoll * 0.01745329238474369D, 1.0D));
            poseStack.m_252880_(0.48F * v, -1.12F * v, -0.72F * v);
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

   public void createVerticesOfQuad(GeoQuad quad, Matrix4f poseState, Vector3f normal, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
      if (RenderPass.current() != RenderPass.RETICLE || quad.direction() == Direction.SOUTH) {
         GeoVertex[] vertices = quad.vertices();
         float[][] texUV;
         float texU;
         float texV;
         if (this.glowingSpriteUVProviderSupplier != null) {
            SpriteUVProvider spriteUVProvider = (SpriteUVProvider)this.glowingSpriteUVProviderSupplier.get();
            float[] uv = spriteUVProvider.getSpriteUV(this.glowingProgress);
            float minU = uv[0];
            float minV = uv[1];
            texU = uv[2];
            texV = uv[3];
            texUV = new float[][]{{minU, minV}, {texU, minV}, {texU, texV}, {minU, texV}};
         } else {
            texUV = new float[][]{{0.0F, 0.0F}, {1.0F, 0.0F}, {1.0F, 1.0F}, {0.0F, 1.0F}};
         }

         for(int i = 0; i < vertices.length; ++i) {
            GeoVertex vertex = vertices[i];
            Vector3f position = vertex.position();
            Vector4f vector4f = poseState.transform(new Vector4f(position.x(), position.y(), position.z(), 1.0F));
            RenderPass renderPass = RenderPass.current();
            if (renderPass == RenderPass.GLOW && this.useCustomGlowingTexture) {
               texU = texUV[i][0];
               texV = texUV[i][1];
            } else {
               texU = vertex.texU();
               texV = vertex.texV();
            }

            buffer.m_5954_(vector4f.x(), vector4f.y(), vector4f.z(), red, green, blue, alpha, texU, texV, packedOverlay, packedLight, normal.x(), normal.y(), normal.z());
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
            GeoRenderLayer<GunItem> layer = (GeoRenderLayer)var5.next();
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

   public void renderRecursively(PoseStack poseStack, GunItem animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
      HierarchicalRenderContext current = HierarchicalRenderContext.current();
      ItemStack rootStack = HierarchicalRenderContext.getRoot().getItemStack();
      boolean shouldRender = this.approveRendering(this.getRenderPass(), bone.getName(), rootStack, current.getItemStack(), current.getPath(), current.getItemDisplayContext());
      if (shouldRender) {
         GlowAnimationController glowEffect = this.gunClientState != null ? (GlowAnimationController)this.gunClientState.getAnimationController("glowEffect" + RenderPass.getEffectId()) : null;
         Runnable r = () -> {
            RotationAnimationController spinner = this.gunClientState != null ? (RotationAnimationController)this.gunClientState.getAnimationController("rotation" + bone.getName()) : null;
            if (spinner != null) {
               spinner.renderRecursively(this, poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
            } else {
               super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
            }

         };
         boolean isGlowEnabled = glowEffect != null && glowEffect.getGlowingPartNames().contains(bone.getName());
         if (isGlowEnabled) {
            HierarchicalRenderContext subHrc = HierarchicalRenderContext.push(current.getItemStack(), current.getItemDisplayContext());

            try {
               GlowingItemLayer.setGlowEnabled(isGlowEnabled);
               r.run();
            } catch (Throwable var25) {
               if (subHrc != null) {
                  try {
                     subHrc.close();
                  } catch (Throwable var24) {
                     var25.addSuppressed(var24);
                  }
               }

               throw var25;
            }

            if (subHrc != null) {
               subHrc.close();
            }
         } else {
            r.run();
         }
      }

   }

   public void renderRecursivelySuper(PoseStack poseStack, GunItem animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
      super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
   }

   public void renderCubesOfBone(PoseStack poseStack, GeoBone bone, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
      RenderPass renderPass = RenderPass.current();
      if (this.shouldRenderBone(bone.getName())) {
         HierarchicalRenderContext hrc = HierarchicalRenderContext.current();
         ItemDisplayContext itemDisplayContext = hrc.getItemDisplayContext();
         boolean isFirstPerson = itemDisplayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || itemDisplayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
         double aimingProgress = 0.0D;
         if (this.gunClientState != null) {
            BiDirectionalInterpolator aimingController = (BiDirectionalInterpolator)this.gunClientState.getAnimationController("aiming");
            aimingProgress = aimingController.getValue();
            if ((!MiscUtil.isGreaterThanZero(this.gunClientState.getGunItem().getPipScopeZoom()) || !Config.pipScopesEnabled) && this.hasScopeOverlay && !this.gunClientState.isReloading() && isFirstPerson && aimingProgress > 0.4D) {
               return;
            }
         }

         switch(renderPass) {
         case GLOW:
            this.renderGlow(poseStack, bone, buffer, packedLight, packedOverlay, red, green, blue, alpha);
            break;
         case HANDS:
            if (bone.getName().equals("rightarm")) {
               this.renderRightArm(poseStack, bone, buffer, packedLight, packedOverlay, red, green, blue, alpha);
            } else if (bone.getName().equals("leftarm")) {
               this.renderLeftArm(poseStack, bone, buffer, packedLight, packedOverlay, red, green, blue, alpha);
            }
            break;
         case PIP:
            if (bone.getName().equals("scopepip")) {
               this.renderPip(poseStack, bone, buffer, packedLight, red, green, blue, aimingProgress);
            }
            break;
         case PIP_OVERLAY:
            if (bone.getName().equals("scopepip")) {
               this.renderPipOverlay(poseStack, bone, buffer, packedLight, red, green, blue, aimingProgress, PipItemLayer.isParallaxEnabled());
            }
            break;
         case PIP_MASK:
            if (bone.getName().equals("scopepip")) {
               this.renderPipMask(poseStack, bone, buffer, packedLight, red, green, blue, aimingProgress);
            }
            break;
         case RETICLE:
            boolean isParallaxEnabled = ReticleItemLayer.isParallaxEnabled();
            if (isParallaxEnabled && bone.getName().equals("reticle")) {
               this.renderReticleWithParallax(poseStack, bone, buffer, packedLight, red, green, blue, aimingProgress, (Float)hrc.getAttribute("max_angular_offset_cos", DEFAULT_MAX_ANGULAR_RETICLE_OFFSET));
            } else if (!isParallaxEnabled && bone.getName().equals("scope")) {
               this.renderReticle(poseStack, bone, buffer, packedLight, packedOverlay, red, green, blue, aimingProgress);
            }
            break;
         case MUZZLE_FLASH:
            if (bone.getName().equals("muzzleflash") || bone.getName().equals("muzzleflash2") || bone.getName().equals("muzzleflash3")) {
               this.renderMuzzleFlash(poseStack, bone, buffer, packedLight);
            }
            break;
         case MAIN_ITEM:
         case ATTACHMENTS:
            if (!bone.getName().equals("muzzleflash") && !bone.getName().equals("muzzleflash2") && !bone.getName().equals("muzzleflash3") && !bone.getName().equals("muzzle") && !bone.getName().equals("muzzle2") && !bone.getName().equals("muzzle3")) {
               if (this.canRenderPart(bone.getName())) {
                  super.renderCubesOfBone(poseStack, bone, buffer, packedLight, packedOverlay, red, green, blue, alpha);
               }
            } else if (ActiveMuzzleFeature.isActiveMuzzle(HierarchicalRenderContext.getRootItemStack(), hrc.getItemStack(), itemDisplayContext, bone.getName())) {
               this.captureMuzzlePose(bone, poseStack, itemDisplayContext);
            }
         }

      }
   }

   private void renderGlow(PoseStack poseStack, GeoBone bone, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
      if (this.gunClientState != null) {
         if (GlowingItemLayer.isGlowEnabled()) {
            GlowAnimationController glowEffect = (GlowAnimationController)this.gunClientState.getAnimationController("glowEffect" + RenderPass.getEffectId());
            if (glowEffect != null) {
               glowEffect.renderCubesOfBone(this, poseStack, bone, buffer, packedLight, packedOverlay, red, green, blue, alpha);
            }
         }

      }
   }

   private void renderMuzzleFlash(PoseStack poseStack, GeoBone bone, VertexConsumer buffer, int packedLight) {
      if (this.gunClientState != null && !this.gunClientState.isReloading() && !this.gunClientState.isInspecting()) {
         List<GeoCube> cubes = bone.getCubes();
         if (cubes != null && !cubes.isEmpty()) {
            GeoCube cube = (GeoCube)cubes.get(0);
            GeoQuad quad1 = cube.quads()[0];
            Vector3f v1Position = quad1.vertices()[0].position();
            GeoQuad quad2 = cube.quads()[2];
            Vector3f v3Position = quad2.vertices()[2].position();
            Vector3f position = new Vector3f();
            position = v3Position.sub(v1Position, position);
            position = position.mul(0.5F);
            position = position.add(v1Position);
            EffectRenderContext context = (new EffectRenderContext()).withPoseStack(poseStack).withPosition(new Vec3((double)position.x, (double)position.y, (double)position.z)).withVertexBuffer(buffer).withLightColor(packedLight);
            Iterator var13 = this.gunClientState.getMuzzleFlashEffects().iterator();

            while(var13.hasNext()) {
               MuzzleFlashEffect effect = (MuzzleFlashEffect)var13.next();
               UUID effectId = EffectRegistry.getEffectId(effect.getName());
               if (Objects.equal(effectId, RenderPass.getEffectId())) {
                  effect.render(context);
               }
            }
         }

      }
   }

   private void renderReticleWithParallax(PoseStack poseStack, GeoBone bone, VertexConsumer buffer, int packedLight, float red, float green, float blue, double aimingProgress, float maxAngularOffsetCos) {
      if (bone != null) {
         if (this.gunClientState != null) {
            if (!(aimingProgress < 0.8D)) {
               List<GeoCube> cubes = bone.getCubes();
               if (cubes != null && !cubes.isEmpty()) {
                  GeoCube cube = (GeoCube)cubes.get(0);
                  GeoQuad northQuad = cube.quads()[3];
                  Matrix4f modelMatrix = this.captureCubeMatrix(poseStack, cube, 3);
                  Vector4f p0 = new Vector4f(0.0F, 0.0F, 0.0F, 1.0F);
                  Vector4f n = new Vector4f(0.0F, 0.0F, -1.0F, 1.0F);
                  Pair<Vector4f, Float> intersection = getPlayerViewIntersection(modelMatrix, p0, n);
                  Vector4f intersectionPoint = (Vector4f)intersection.getFirst();
                  if (intersectionPoint == null) {
                     return;
                  }

                  float angularOffsetCos = (Float)intersection.getSecond();
                  float threshold = !(aimingProgress < 1.0D) && !this.gunClientState.isReloading() && !this.gunClientState.isInspecting() ? maxAngularOffsetCos : DEFAULT_MAX_ANGULAR_RETICLE_OFFSET_NOT_AIMED;
                  if (angularOffsetCos > threshold) {
                     poseStack.m_85836_();
                     float maxOffsetFromTheCenter = 1.0F;
                     Player player = ClientUtils.getClientPlayer();
                     float smoothFactor = 1.0F;
                     Vec3 mv = player.m_20184_();
                     if (mv.f_82479_ != 0.0D && mv.f_82481_ != 0.0D) {
                        smoothFactor = 0.8F;
                     }

                     poseStack.m_252880_(Mth.m_14036_(intersectionPoint.x() * smoothFactor, -maxOffsetFromTheCenter, maxOffsetFromTheCenter), Mth.m_14036_(intersectionPoint.y() * smoothFactor, -maxOffsetFromTheCenter, maxOffsetFromTheCenter), Mth.m_14036_(intersectionPoint.z() * smoothFactor, -maxOffsetFromTheCenter, maxOffsetFromTheCenter));
                     float alpha = (float)aimingProgress;
                     RenderUtil.renderQuad(poseStack, northQuad, buffer, 0.0F, 0.0F, red, green, blue, alpha);
                     poseStack.m_85849_();
                  }
               }

            }
         }
      }
   }

   private void renderReticle(PoseStack poseStack, GeoBone bone, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, double aimingProgress) {
      if (this.gunClientState != null && !this.gunClientState.isReloading() && !this.gunClientState.isInspecting()) {
         BakedGeoModel reticleBakedGeoModel = this.getReticleModel();
         GeoBone reticleBone = (GeoBone)reticleBakedGeoModel.getBone("scope").get();
         if (reticleBone != null) {
            poseStack.m_85836_();
            if (aimingProgress > 0.9D && !this.gunClientState.isReloading()) {
               float alpha = 0.7F;
               this.applyRefTransforms(poseStack, bone, reticleBone);
               double yaw = ClientEventHandler.reticleInertiaController.getYaw();
               double pitch = ClientEventHandler.reticleInertiaController.getPitch();
               Quaternionf q = new Quaternionf(pitch, yaw, 0.0D, 1.0D);
               poseStack.m_85837_(-yaw * 25.0D, pitch * 25.0D, -8.0D);
               poseStack.m_252781_(q);
               super.renderCubesOfBone(poseStack, reticleBone, buffer, packedLight, packedOverlay, red, green, blue, alpha);
            }

            poseStack.m_85849_();
         }

      }
   }

   private void renderPip(PoseStack poseStack, GeoBone bone, VertexConsumer buffer, int packedLight, float red, float green, float blue, double aimingProgress) {
      poseStack.m_85836_();
      if (aimingProgress > 0.9D && this.gunClientState != null && !this.gunClientState.isReloading()) {
         List<GeoCube> cubes = bone.getCubes();
         if (cubes != null && !cubes.isEmpty()) {
            GeoCube cube = (GeoCube)cubes.get(0);
            GeoQuad northQuad = cube.quads()[3];
            ClientSystem.getInstance().getAuxLevelRenderer().renderToBuffer(poseStack, northQuad, buffer, packedLight);
         }
      }

      poseStack.m_85849_();
   }

   private void renderPipOverlay(PoseStack poseStack, GeoBone bone, VertexConsumer buffer, int packedLight, float red, float green, float blue, double aimingProgress, boolean isParallaxEnabled) {
      List<GeoCube> cubes = bone.getCubes();
      if (cubes != null && !cubes.isEmpty()) {
         GeoCube cube = (GeoCube)cubes.get(0);
         GeoQuad northQuad = cube.quads()[3];
         if (isParallaxEnabled) {
            Matrix4f modelMatrix = this.captureCubeMatrix(poseStack, cube, 3);
            Vector4f p0 = new Vector4f(0.0F, 0.0F, 0.01F, 1.0F);
            Vector4f n = new Vector4f(0.0F, 0.0F, -1.0F, 1.0F);
            Pair<Vector4f, Float> intersection = getPlayerViewIntersection(modelMatrix, p0, n);
            Vector4f intersectionPoint = (Vector4f)intersection.getFirst();
            if (intersectionPoint != null) {
               float maxOffsetFromTheCenter = 1.0F;
               float smoothFactor = 0.8F;
               poseStack.m_252880_(Mth.m_14036_(intersectionPoint.x() * smoothFactor, -maxOffsetFromTheCenter, maxOffsetFromTheCenter), Mth.m_14036_(intersectionPoint.y() * smoothFactor, -maxOffsetFromTheCenter, maxOffsetFromTheCenter), Mth.m_14036_(intersectionPoint.z() * smoothFactor, -maxOffsetFromTheCenter, maxOffsetFromTheCenter));
            }
         }

         float alpha = (float)aimingProgress;
         poseStack.m_85836_();
         poseStack.m_252880_(0.0F, 0.0F, 0.001F);
         RenderUtil.renderQuad(poseStack, northQuad, buffer, 0.0F, 0.0F, red, green, blue, alpha);
         poseStack.m_85849_();
      }

   }

   private void renderPipMask(PoseStack poseStack, GeoBone bone, VertexConsumer buffer, int packedLight, float red, float green, float blue, double aimingProgress) {
      List<GeoCube> cubes = bone.getCubes();
      if (cubes != null && !cubes.isEmpty()) {
         GeoCube cube = (GeoCube)cubes.get(0);
         GeoQuad northQuad = cube.quads()[3];
         float alpha = (float)aimingProgress;
         poseStack.m_85836_();
         RenderUtil.renderQuad(poseStack, northQuad, buffer, 0.0F, 0.0F, red, green, blue, alpha);
         poseStack.m_85849_();
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

   private void captureMuzzlePose(GeoBone refBone, PoseStack poseStack, ItemDisplayContext itemDisplayContext) {
      if (this.gunClientState != null) {
         poseStack.m_85836_();
         GeoCube refCube = (GeoCube)refBone.getCubes().get(0);
         RenderUtils.translateToPivotPoint(poseStack, refCube);
         RenderUtils.rotateMatrixAroundCube(poseStack, refCube);
         RenderUtils.translateAwayFromPivotPoint(poseStack, refCube);
         poseStack.m_252880_(refBone.getPivotX() / 16.0F, refBone.getPivotY() / 16.0F, refBone.getPivotZ() / 16.0F);
         GunStatePoseProvider gunStatePoseProvider = GunStatePoseProvider.getInstance();
         GunStatePoseProvider.PoseContext poseContext;
         if (itemDisplayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
            poseContext = null;
            if (!refBone.getName().equals("muzzle") && !refBone.getName().equals("muzzle2")) {
               if (refBone.getName().equals("muzzleflash") || refBone.getName().equals("muzzleflash2")) {
                  poseContext = GunStatePoseProvider.PoseContext.FIRST_PERSON_MUZZLE_FLASH;
               }
            } else {
               poseContext = GunStatePoseProvider.PoseContext.FIRST_PERSON_MUZZLE;
            }

            if (poseContext != null) {
               gunStatePoseProvider.setPose(this.gunClientState, poseContext, poseStack.m_85850_());
               this.setCurrentMuzzlePosition(poseStack, poseContext);
            }
         } else if (itemDisplayContext == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
            poseContext = null;
            if (!refBone.getName().equals("muzzle") && !refBone.getName().equals("muzzle2")) {
               if (refBone.getName().equals("muzzleflash") || refBone.getName().equals("muzzleflash2")) {
                  poseContext = GunStatePoseProvider.PoseContext.THIRD_PERSON_MUZZLE_FLASH;
               }
            } else {
               poseContext = GunStatePoseProvider.PoseContext.THIRD_PERSON_MUZZLE;
            }

            if (poseContext != null) {
               gunStatePoseProvider.setPose(this.gunClientState, poseContext, poseStack.m_85850_());
               this.setCurrentMuzzlePosition(poseStack, poseContext);
            }
         }

         poseStack.m_85849_();
      }
   }

   private void setCurrentMuzzlePosition(PoseStack poseStack, GunStatePoseProvider.PoseContext poseContext) {
      poseStack.m_85836_();
      Minecraft mc = Minecraft.m_91087_();
      Camera camera = mc.f_91063_.m_109153_();
      Vec3 cameraPos = camera.m_90583_();
      double fov = ((Integer)mc.f_91066_.m_231837_().m_231551_()).doubleValue();
      Matrix4f fovProjectionMatrix = mc.f_91063_.m_253088_(fov);
      Matrix4f transform = (new Matrix4f(RenderSystem.getInverseViewRotationMatrix())).mul(fovProjectionMatrix.invert()).mul(RenderSystem.getProjectionMatrix()).mul(poseStack.m_85850_().m_252922_());
      Vector4f relPos = transform.transform(new Vector4f(0.0F, 0.0F, 0.0F, 1.0F));
      Vector4f pos = new Vector4f(relPos);
      pos.add((float)cameraPos.f_82479_, (float)cameraPos.f_82480_, (float)cameraPos.f_82481_, 1.0F);
      Vector4f direction = transform.transform(new Vector4f(0.0F, 0.0F, -1.0F, 1.0F));
      direction.sub(relPos);
      direction.normalize();
      GunStatePoseProvider.getInstance().setPositionAndDirection(this.gunClientState, poseContext, new Vec3((double)pos.x, (double)pos.y, (double)pos.z), new Vec3((double)direction.x, (double)direction.y, (double)direction.z));
      poseStack.m_85849_();
   }

   public void renderCubesOfBoneParent(PoseStack poseStack, GeoBone bone, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
      super.renderCubesOfBone(poseStack, bone, buffer, packedLight, packedOverlay, red, green, blue, alpha);
   }

   public void renderCubesOfBoneParent(PoseStack poseStack, GeoBone bone, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, boolean useCustomGlowingTexture, Set<Direction> directions, Supplier<SpriteUVProvider> glowingSpriteUVProviderSupplier, float glowingProgress) {
      boolean hadCustomGlowingTexture = this.useCustomGlowingTexture;
      this.useCustomGlowingTexture = useCustomGlowingTexture;
      this.glowDirections = directions;
      this.glowingSpriteUVProviderSupplier = glowingSpriteUVProviderSupplier;
      this.glowingProgress = glowingProgress;
      super.renderCubesOfBone(poseStack, bone, buffer, packedLight, packedOverlay, red, green, blue, alpha);
      this.glowDirections = null;
      this.useCustomGlowingTexture = hadCustomGlowingTexture;
   }

   public void renderCube(PoseStack poseStack, GeoCube cube, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
      RenderUtils.translateToPivotPoint(poseStack, cube);
      RenderUtils.rotateMatrixAroundCube(poseStack, cube);
      RenderUtils.translateAwayFromPivotPoint(poseStack, cube);
      Matrix3f normalisedPoseState = poseStack.m_85850_().m_252943_();
      Matrix4f poseState = poseStack.m_85850_().m_252922_();
      GeoQuad[] var12 = cube.quads();
      int var13 = var12.length;

      for(int var14 = 0; var14 < var13; ++var14) {
         GeoQuad quad = var12[var14];
         if (quad != null && (this.glowDirections == null || this.glowDirections.contains(quad.direction()))) {
            Vector3f normal = normalisedPoseState.transform(new Vector3f(quad.normal()));
            RenderUtils.fixInvertedFlatCube(cube, normal);
            this.createVerticesOfQuad(quad, poseState, normal, buffer, packedLight, packedOverlay, red, green, blue, alpha);
         }
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

   private void applyRefTransforms(PoseStack poseStack, GeoBone refBone, GeoBone actualBone) {
      GeoCube actualArmBoneCube = (GeoCube)actualBone.getCubes().get(0);
      GeoCube refCube = (GeoCube)refBone.getCubes().get(0);
      GeoVertex refQ0v0 = refCube.quads()[2].vertices()[0];
      GeoVertex refQ0v2 = refCube.quads()[2].vertices()[2];
      GeoVertex actualQ0v0 = actualArmBoneCube.quads()[2].vertices()[0];
      GeoVertex actualQ0v2 = actualArmBoneCube.quads()[2].vertices()[2];
      float refSizeX = Math.abs(refQ0v0.position().x - refQ0v2.position().x);
      float actualSizeX = Math.abs(actualQ0v0.position().x - actualQ0v2.position().x);
      float refSizeY = Math.abs(refQ0v0.position().y - refQ0v2.position().y);
      float actualSizeY = Math.abs(actualQ0v0.position().y - actualQ0v2.position().y);
      float refXLeft = refQ0v0.position().x;
      float refZLeft = refQ0v2.position().z;
      float actualXLeft = actualQ0v0.position().x;
      float actualYTop = actualQ0v0.position().y;
      float refYTop = refQ0v0.position().y;
      float actualZLeft = actualQ0v0.position().z;
      float dx = -(actualXLeft + (refXLeft - (refSizeX - actualSizeX) / 2.0F));
      float dy = refYTop + (actualSizeY - refSizeY) / 2.0F - actualYTop;
      float dz = refZLeft - actualZLeft;
      poseStack.m_252880_(dx, dy, dz);
   }

   public boolean approveRendering(RenderPass renderPass, String boneName, ItemStack rootStack, ItemStack currentStack, String path, ItemDisplayContext itemDisplayContext) {
      List<Features.EnabledFeature> enabledVisibilityFeatures = Features.getEnabledFeatures(rootStack, PartVisibilityFeature.class);
      ConditionContext conditionContext = new ConditionContext((LivingEntity)null, rootStack, currentStack, this.gunClientState, itemDisplayContext);
      Iterator var9 = enabledVisibilityFeatures.iterator();

      PartVisibilityFeature visibilityFeature;
      do {
         if (!var9.hasNext()) {
            return true;
         }

         Features.EnabledFeature enabledVisibilityFeature = (Features.EnabledFeature)var9.next();
         visibilityFeature = (PartVisibilityFeature)enabledVisibilityFeature.feature();
      } while(visibilityFeature.isPartVisible(currentStack.m_41720_(), boneName, conditionContext));

      return false;
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
      return !boneName.equals("leftarm") && !boneName.equals("rightarm") && !boneName.equals("scope") && !boneName.equals("reticle") && !boneName.equals("scopepip");
   }

   public RenderPass getRenderPass() {
      return RenderPass.MAIN_ITEM;
   }

   public Class<? extends Feature> getFeatureType() {
      return null;
   }

   private static Pair<Vector4f, Float> getPlayerViewIntersection(Matrix4f modelMatrix, Vector4f p0, Vector4f n) {
      Minecraft mc = Minecraft.m_91087_();
      Player player = ClientUtils.getClientPlayer();
      float partialTicks = mc.getPartialTick();
      Vec3 playerEyePositionRelativeToCamera = new Vec3(0.0D, 0.0D, 0.0D);
      Vec3 playerViewVector = player.m_20252_(partialTicks);
      Matrix4f transform3 = (new Matrix4f(modelMatrix)).invert().mul((new Matrix4f(RenderSystem.getInverseViewRotationMatrix())).invert());
      double farDistance = 5.0D;
      Vec3 farPointWorld = playerEyePositionRelativeToCamera.m_82549_(playerViewVector.m_82490_(farDistance));
      Vector4f eyePositionModel = new Vector4f();
      transform3.transform((float)playerEyePositionRelativeToCamera.f_82479_, (float)playerEyePositionRelativeToCamera.f_82480_, (float)playerEyePositionRelativeToCamera.f_82481_, 1.0F, eyePositionModel);
      Vector4f farPointModel = new Vector4f();
      transform3.transform((float)farPointWorld.f_82479_, (float)farPointWorld.f_82480_, (float)farPointWorld.f_82481_, 1.0F, farPointModel);
      Vector4f l = (new Vector4f(farPointModel)).sub(eyePositionModel).normalize();
      Vector4f p0MinusL0 = (new Vector4f(p0.x, p0.y, p0.z, 1.0F)).sub(eyePositionModel);
      float numerator = p0MinusL0.dot(n.x, n.y, n.z, 1.0F);
      float denominator = l.dot(n.x, n.y, n.z, 1.0F);
      Vector4f intersectionPoint = null;
      if (denominator > 0.0F) {
         float d = numerator / denominator;
         intersectionPoint = (new Vector4f(l)).mul(d).add(eyePositionModel);
      }

      return Pair.of(intersectionPoint, denominator);
   }

   public Matrix4f captureCubeMatrix(PoseStack poseStack, GeoCube cube, int side) {
      GeoQuad quad = cube.quads()[side];
      poseStack.m_85836_();
      RenderUtils.translateToPivotPoint(poseStack, cube);
      RenderUtils.rotateMatrixAroundCube(poseStack, cube);
      RenderUtils.translateAwayFromPivotPoint(poseStack, cube);
      Vector3f v0Pos = quad.vertices()[0].position();
      Vector3f v2Pos = quad.vertices()[2].position();
      poseStack.m_252880_((v0Pos.x + v2Pos.x) * 0.5F, (v0Pos.y + v2Pos.y) * 0.5F, (v0Pos.z + v2Pos.z) * 0.5F);
      Matrix4f cubeMatrix = poseStack.m_85850_().m_252922_();
      poseStack.m_85849_();
      return cubeMatrix;
   }
}
