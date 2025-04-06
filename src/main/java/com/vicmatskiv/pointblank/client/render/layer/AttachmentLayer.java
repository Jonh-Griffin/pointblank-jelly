package com.vicmatskiv.pointblank.client.render.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.datafixers.util.Pair;
import com.vicmatskiv.pointblank.attachment.Attachment;
import com.vicmatskiv.pointblank.attachment.AttachmentCategory;
import com.vicmatskiv.pointblank.attachment.AttachmentHost;
import com.vicmatskiv.pointblank.attachment.AttachmentModelInfo;
import com.vicmatskiv.pointblank.attachment.Attachments;
import com.vicmatskiv.pointblank.client.GunClientState;
import com.vicmatskiv.pointblank.client.gui.AttachmentManagerScreen;
import com.vicmatskiv.pointblank.client.render.HierarchicalRenderContext;
import com.vicmatskiv.pointblank.client.render.RenderApprover;
import com.vicmatskiv.pointblank.client.render.RenderPass;
import com.vicmatskiv.pointblank.client.render.RenderPassGeoRenderer;
import com.vicmatskiv.pointblank.client.render.RenderPassRenderer;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class AttachmentLayer<T extends GeoAnimatable> extends FeaturePassLayer<T> {
   private static final Vector4f COLOR_GREEN = new Vector4f(0.0F, 1.0F, 0.0F, 1.0F);
   private static final Vector4f COLOR_NORMAL = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
   private RenderPassRenderer<T> currentRenderer;
   private RenderPassGeoRenderer<T> mainRenderer;

   public AttachmentLayer(RenderPassGeoRenderer<T> renderer) {
      super(renderer, (Class)null, RenderPass.ATTACHMENTS, ALL_PARTS, true, (Object)null);
      this.mainRenderer = renderer;
   }

   public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
      if (animatable instanceof AttachmentHost) {
         ItemStack rootStack = HierarchicalRenderContext.current().getItemStack();
         this.currentRenderer = this.mainRenderer;
         this.renderPass(() -> {
            this.renderAttachments((GunClientState)null, (RenderType)null, animatable, "/", rootStack, poseStack, bufferSource, buffer, partialTick, packedLight, packedOverlay);
            Iterator var9 = this.mainRenderer.getRenderLayers().iterator();

            while(var9.hasNext()) {
               GeoRenderLayer<T> effectLayer = (GeoRenderLayer)var9.next();
               if (effectLayer != this && effectLayer instanceof RenderPassRenderer) {
                  RenderPassRenderer<T> renderPassRenderer = (RenderPassRenderer)effectLayer;
                  if (renderPassRenderer.isEffectLayer()) {
                     GunClientState state = GunClientState.getMainHeldState();
                     if (state != null) {
                        renderPassRenderer.renderPass(() -> {
                           this.currentRenderer = renderPassRenderer;
                           this.renderAttachments(state, (RenderType)null, animatable, "/", rootStack, poseStack, bufferSource, buffer, partialTick, packedLight, packedOverlay);
                           this.currentRenderer = null;
                        });
                     }
                  }
               }
            }

         });
      }
   }

   public void renderAttachments(GunClientState state, RenderType renderTypeOverride, T gunItem, String parentPath, ItemStack baseItemStack, PoseStack poseStack, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
      Item var13 = baseItemStack.m_41720_();
      if (var13 instanceof AttachmentHost) {
         AttachmentHost attachmentHost = (AttachmentHost)var13;
         Collection var18 = Attachments.getAttachments(baseItemStack);
         Iterator var14 = var18.iterator();

         while(var14.hasNext()) {
            ItemStack attachmentItemStack = (ItemStack)var14.next();
            Item var17 = attachmentItemStack.m_41720_();
            if (var17 instanceof Attachment) {
               Attachment attachment = (Attachment)var17;
               AttachmentModelInfo attachmentInfo = (AttachmentModelInfo)((Optional)AttachmentModelInfo.attachmentInfos.apply(attachmentHost, attachment)).orElse((Object)null);
               if (attachmentInfo != null) {
                  this.renderAttachment(state, renderTypeOverride, gunItem, parentPath, baseItemStack, attachmentInfo.baseModel(), attachmentInfo.baseBone(), attachmentItemStack, attachmentInfo.attachmentModel(), attachmentInfo.attachmentBone(), poseStack, bufferSource, buffer, partialTick, packedLight, packedOverlay);
               }
            }
         }

      }
   }

   protected void renderAttachment(GunClientState state, RenderType renderTypeOverride, T gunItem, String parentPath, ItemStack baseItemStack, BakedGeoModel baseModel, CoreGeoBone baseBone, ItemStack attachmentItemStack, BakedGeoModel attachmentModel, CoreGeoBone attachmentBone, PoseStack poseStack, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
      Item var18 = attachmentItemStack.m_41720_();
      if (var18 instanceof Attachment) {
         Attachment attachment = (Attachment)var18;
         ItemDisplayContext itemDisplayContext = HierarchicalRenderContext.current().getItemDisplayContext();
         HierarchicalRenderContext hrc = HierarchicalRenderContext.push(attachmentItemStack, itemDisplayContext);

         try {
            String attachmentPath = parentPath + "/" + attachment.getName();
            Pair<Matrix4f, Matrix4f> basePos = (Pair)((Optional)AttachmentModelInfo.modelBonePositions.apply(new AttachmentModelInfo.ModelBoneKey(baseModel, baseBone.getName(), 1.0F))).orElse((Object)null);
            Pair<Matrix4f, Matrix4f> attachmentPos = (Pair)((Optional)AttachmentModelInfo.modelBonePositions.apply(new AttachmentModelInfo.ModelBoneKey(attachmentModel, attachmentBone.getName(), 1.0F))).orElse((Object)null);
            if (basePos != null && attachmentPos != null) {
               RenderType attachmentRenderType = null;
               Vector4f color = COLOR_NORMAL;
               if (renderTypeOverride == null) {
                  Pair<RenderType, Vector4f> p = this.getRenderType(baseItemStack, attachmentItemStack, attachment.getName(), attachmentPath, itemDisplayContext);
                  attachmentRenderType = (RenderType)p.getFirst();
                  color = (Vector4f)p.getSecond();
               } else {
                  attachmentRenderType = renderTypeOverride;
                  color = COLOR_GREEN;
               }

               if (attachmentRenderType != null) {
                  poseStack.m_85836_();
                  AttachmentModelInfo.preparePoseStackForBoneInHierarchy(poseStack, baseBone);
                  poseStack.m_252931_((Matrix4f)basePos.getFirst());
                  poseStack.m_252931_((Matrix4f)attachmentPos.getSecond());
                  this.currentRenderer.render(attachmentModel, poseStack, bufferSource, gunItem, attachmentRenderType, bufferSource.m_6299_(attachmentRenderType), partialTick, packedLight, OverlayTexture.f_118083_, color.x, color.y, color.z, color.w);
                  this.renderAttachments(state, renderTypeOverride, gunItem, attachmentPath, attachmentItemStack, poseStack, bufferSource, buffer, partialTick, packedLight, packedOverlay);
                  poseStack.m_85849_();
               }
            }
         } catch (Throwable var27) {
            if (hrc != null) {
               try {
                  hrc.close();
               } catch (Throwable var26) {
                  var27.addSuppressed(var26);
               }
            }

            throw var27;
         }

         if (hrc != null) {
            hrc.close();
         }

      }
   }

   private Pair<RenderType, Vector4f> getRenderType(ItemStack baseItemStack, ItemStack attachmentItemStack, String attachmentName, String attachmentPath, ItemDisplayContext itemDisplayContext) {
      RenderType renderType = null;
      ResourceLocation texture = new ResourceLocation("pointblank", "textures/item/" + attachmentName + ".png");
      Vector4f color = null;
      Minecraft mc = Minecraft.m_91087_();
      if (itemDisplayContext == ItemDisplayContext.GROUND) {
         Screen var11 = mc.f_91080_;
         if (var11 instanceof AttachmentManagerScreen) {
            AttachmentManagerScreen ams = (AttachmentManagerScreen)var11;
            Pair<RenderType, Vector4f> p = ams.getRenderTypeOverride(baseItemStack, attachmentItemStack, attachmentName, attachmentPath);
            if (p != null) {
               renderType = (RenderType)p.getFirst();
               color = (Vector4f)p.getSecond();
            }
         }
      }

      if (renderType == null) {
         renderType = this.currentRenderer.getRenderType();
      }

      if (renderType == null) {
         renderType = RenderType.m_110458_(texture);
      }

      if (color == null) {
         color = COLOR_NORMAL;
      }

      return Pair.of(renderType, color);
   }

   public boolean approveRendering(RenderPass renderPass, String boneName, ItemStack rootStack, ItemStack currentStack, String path, ItemDisplayContext itemDisplayContext) {
      if (renderPass == RenderPass.ATTACHMENTS) {
         return true;
      } else {
         HierarchicalRenderContext hrc = HierarchicalRenderContext.current();
         Item var9 = hrc.getItemStack().m_41720_();
         if (var9 instanceof Attachment) {
            Attachment currentAttachment = (Attachment)var9;
            AttachmentCategory category = currentAttachment.getCategory();
            Pair activeCategoryAttachment = Attachments.getSelectedAttachment(rootStack, category);
            if (activeCategoryAttachment != null && !Objects.equals(activeCategoryAttachment.getFirst(), hrc.getPath())) {
               return false;
            } else {
               RenderPassRenderer var12 = this.currentRenderer;
               if (var12 instanceof RenderApprover) {
                  RenderApprover renderApprover = (RenderApprover)var12;
                  return renderApprover.approveRendering(renderPass, boneName, rootStack, currentStack, path, itemDisplayContext);
               } else {
                  return true;
               }
            }
         } else {
            return true;
         }
      }
   }

   public RenderType getRenderType() {
      return null;
   }

   public boolean isSupportedItemDisplayContext(ItemDisplayContext context) {
      return context != ItemDisplayContext.GUI;
   }
}
