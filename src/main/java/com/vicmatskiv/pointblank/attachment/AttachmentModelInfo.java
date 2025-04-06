package com.vicmatskiv.pointblank.attachment;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Axis;
import com.vicmatskiv.pointblank.client.ClientSystem;
import com.vicmatskiv.pointblank.item.GunItem;
import com.vicmatskiv.pointblank.util.LRUCache;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.util.RenderUtils;

@OnlyIn(Dist.CLIENT)
public record AttachmentModelInfo(BakedGeoModel baseModel, CoreGeoBone baseBone, BakedGeoModel attachmentModel, CoreGeoBone attachmentBone) {
   private static final Logger LOGGER = LogManager.getLogger("pointblank");
   private static final String PREFIX_CONNECTOR_BASE = "_cb_";
   private static final String PREFIX_CONNECTOR_ATTACHMENT = "_ca_";
   private static final LRUCache<Tag, NavigableMap<String, Pair<ItemStack, Matrix4f>>> stackTagPosisionCache = new LRUCache(100);
   public static final Function<ModelBoneKey, Optional<Pair<Matrix4f, Matrix4f>>> modelBonePositions = Util.m_143827_(AttachmentModelInfo::getRefBoneMatrices);
   private static final BiFunction<BakedGeoModel, String, Optional<? extends CoreGeoBone>> modelBones = Util.m_143821_((model, boneName) -> {
      return model.getBone(boneName);
   });
   public static final BiFunction<AttachmentHost, Attachment, Optional<AttachmentModelInfo>> attachmentInfos = ClientSystem.getInstance().createReloadableMemoize(AttachmentModelInfo::getAttachmentModelInfo);
   private static final Function<BakedGeoModel, List<? extends CoreGeoBone>> modelABBones = Util.m_143827_((model) -> {
      return findBones(model, (b) -> {
         return b.getName().startsWith("_ca_");
      });
   });

   public AttachmentModelInfo(BakedGeoModel baseModel, CoreGeoBone baseBone, BakedGeoModel attachmentModel, CoreGeoBone attachmentBone) {
      this.baseModel = baseModel;
      this.baseBone = baseBone;
      this.attachmentModel = attachmentModel;
      this.attachmentBone = attachmentBone;
   }

   private static Optional<AttachmentModelInfo> getAttachmentModelInfo(AttachmentHost attachmentHost, Attachment attachment) {
      AttachmentModelInfo attachmentInfo = null;
      BakedGeoModel attachmentModel = getModel(attachment.getName());
      if (attachmentModel != null) {
         BakedGeoModel baseModel = getModel(attachmentHost.getName());
         Iterator var5 = ((List)modelABBones.apply(attachmentModel)).iterator();

         while(var5.hasNext()) {
            CoreGeoBone connectorAttachmentBone = (CoreGeoBone)var5.next();
            String suffix = connectorAttachmentBone.getName().substring("_ca_".length());
            CoreGeoBone baseBone = (CoreGeoBone)((Optional)modelBones.apply(baseModel, "_cb_" + suffix)).orElse((Object)null);
            if (baseBone != null) {
               attachmentInfo = new AttachmentModelInfo(baseModel, baseBone, attachmentModel, connectorAttachmentBone);
               break;
            }
         }
      }

      return Optional.ofNullable(attachmentInfo);
   }

   private static List<? extends CoreGeoBone> findBones(BakedGeoModel model, Predicate<CoreGeoBone> predicate) {
      if (model == null) {
         return null;
      } else {
         Objects.requireNonNull(model);
         return findBonesRecursively(model::getBones, predicate, new ArrayList());
      }
   }

   private static List<? extends CoreGeoBone> findBonesRecursively(Supplier<List<? extends CoreGeoBone>> boneListSupplier, Predicate<CoreGeoBone> predicate, List<CoreGeoBone> results) {
      Iterator var3 = ((List)boneListSupplier.get()).iterator();

      while(var3.hasNext()) {
         CoreGeoBone childBone = (CoreGeoBone)var3.next();
         if (predicate.test(childBone)) {
            results.add(childBone);
         }

         Objects.requireNonNull(childBone);
         findBonesRecursively(childBone::getChildBones, predicate, results);
      }

      return results;
   }

   public static BakedGeoModel getModel(String itemName) {
      ResourceLocation attachmentModelResource = new ResourceLocation("pointblank", "geo/item/" + itemName + ".geo.json");
      return (BakedGeoModel)GeckoLibCache.getBakedModels().get(attachmentModelResource);
   }

   public static Optional<Pair<Matrix4f, Matrix4f>> getRefBoneMatrices(ModelBoneKey key) {
      Pair<Matrix4f, Matrix4f> result = null;
      GeoBone bone = (GeoBone)key.model.getBone(key.boneName).orElse((Object)null);
      if (bone != null) {
         List<GeoCube> cubes = bone.getCubes();
         if (cubes != null && !cubes.isEmpty()) {
            GeoCube cube = (GeoCube)cubes.get(0);
            GeoQuad upQuad = cube.quads()[4];
            GeoQuad downQuad = cube.quads()[5];
            GeoVertex[] upVertices = upQuad.vertices();
            GeoVertex[] downVertices = downQuad.vertices();
            GeoVertex upVertex = upVertices[0];
            GeoVertex downVertex = downVertices[0];
            Vector3f position = (new Vector3f(upVertex.position())).add(downVertex.position()).mul(0.5F);
            position.mul(key.scale);
            Matrix4f m = (new Matrix4f()).translate(position);
            Vec3 cubeRotation = cube.rotation();
            if (cubeRotation.f_82481_ != 0.0D) {
               m.rotate(Axis.f_252403_.m_252961_((float)cubeRotation.f_82481_));
            }

            if (cubeRotation.f_82480_ != 0.0D) {
               m.rotate(Axis.f_252436_.m_252961_((float)cubeRotation.f_82480_));
            }

            if (cubeRotation.f_82479_ != 0.0D) {
               m.rotate(Axis.f_252529_.m_252961_((float)cubeRotation.f_82479_));
            }

            result = Pair.of(m, (new Matrix4f(m)).invert());
         }
      }

      return Optional.ofNullable(result);
   }

   public static NavigableMap<String, Pair<ItemStack, Matrix4f>> findInverseBoneMatrices(ItemStack baseStack, String boneName, float scale) {
      if (baseStack != null) {
         Item var4 = baseStack.m_41720_();
         if (var4 instanceof GunItem) {
            GunItem gunItem = (GunItem)var4;
            BakedGeoModel gunItemModel = getModel(gunItem.getName());
            if (gunItemModel != null) {
               return findInverseBoneMatrices(baseStack, gunItemModel, boneName, scale);
            }
         }
      }

      return Collections.emptyNavigableMap();
   }

   private static NavigableMap<String, Pair<ItemStack, Matrix4f>> findInverseBoneMatrices(ItemStack baseStack, BakedGeoModel baseModel, String boneName, float scale) {
      CompoundTag tag = baseStack.m_41783_();
      NavigableMap<String, Pair<ItemStack, Matrix4f>> results = new TreeMap();
      if (tag == null) {
         return results;
      } else {
         Tag attachmentsTag = tag.m_128423_("as");
         if (attachmentsTag == null) {
            attachmentsTag = tag;
         }

         CompoundTag keyTag = new CompoundTag();
         keyTag.m_128365_("a", (Tag)attachmentsTag);
         Tag selectedAttachments = tag.m_128423_("sa");
         if (selectedAttachments != null) {
            keyTag.m_128365_("sa", tag.m_128423_("sa"));
         }

         keyTag.m_128356_("m", tag.m_128454_("mid"));
         keyTag.m_128356_("l", tag.m_128454_("lid"));
         return (NavigableMap)stackTagPosisionCache.computeIfAbsent(keyTag, (t) -> {
            return findBonePositions(baseModel, "/", baseStack, boneName, scale, new PoseStack(), results);
         });
      }
   }

   private static NavigableMap<String, Pair<ItemStack, Matrix4f>> findBonePositions(BakedGeoModel baseModel, String componentName, ItemStack baseStack, String boneName, float scale, PoseStack poseStack, NavigableMap<String, Pair<ItemStack, Matrix4f>> results) {
      LOGGER.debug("Computing inverse bone position for component {}", componentName);
      Pair<Matrix4f, Matrix4f> abPos = (Pair)((Optional)modelBonePositions.apply(new ModelBoneKey(baseModel, boneName, scale))).orElse((Object)null);
      if (abPos != null) {
         poseStack.m_85836_();
         poseStack.m_252931_((Matrix4f)abPos.getFirst());
         results.put(componentName, Pair.of(baseStack, (new Matrix4f(poseStack.m_85850_().m_252922_())).invert()));
         poseStack.m_85849_();
      }

      Item var9 = baseStack.m_41720_();
      if (var9 instanceof AttachmentHost) {
         AttachmentHost attachmentHost = (AttachmentHost)var9;
         Collection var18 = Attachments.getAttachments(baseStack.m_41783_(), "/", false, new TreeMap()).values();
         Iterator var10 = var18.iterator();

         while(var10.hasNext()) {
            ItemStack attachmentStack = (ItemStack)var10.next();
            Item var13 = attachmentStack.m_41720_();
            if (var13 instanceof Attachment) {
               Attachment attachment = (Attachment)var13;
               AttachmentModelInfo attachmentInfo = (AttachmentModelInfo)((Optional)attachmentInfos.apply(attachmentHost, attachment)).orElse((Object)null);
               if (attachmentInfo != null) {
                  Pair<Matrix4f, Matrix4f> basePos = (Pair)((Optional)modelBonePositions.apply(new ModelBoneKey(attachmentInfo.baseModel(), attachmentInfo.baseBone().getName(), scale))).orElse((Object)null);
                  BakedGeoModel attachmentModel = attachmentInfo.attachmentModel();
                  Pair<Matrix4f, Matrix4f> attachmentPos = (Pair)((Optional)modelBonePositions.apply(new ModelBoneKey(attachmentModel, attachmentInfo.attachmentBone().getName(), scale))).orElse((Object)null);
                  if (basePos != null && attachmentPos != null) {
                     poseStack.m_85836_();
                     poseStack.m_252931_((Matrix4f)basePos.getFirst());
                     poseStack.m_252931_((Matrix4f)attachmentPos.getSecond());
                     findBonePositions(attachmentModel, componentName + "/" + attachment.getName(), attachmentStack, boneName, scale, poseStack, results);
                     poseStack.m_85849_();
                  }
               }
            }
         }

         return results;
      } else {
         return results;
      }
   }

   public static List<CoreGeoBone> getParentBoneHierarchy(CoreGeoBone bone) {
      LinkedList<CoreGeoBone> result = new LinkedList();

      for(CoreGeoBone current = bone; current != null; current = current.getParent()) {
         result.addFirst(current);
      }

      return result;
   }

   public static void preparePoseStackForBoneInHierarchy(PoseStack poseStack, CoreGeoBone bone) {
      List<CoreGeoBone> boneHierarchy = getParentBoneHierarchy(bone);
      Iterator var3 = boneHierarchy.iterator();

      while(var3.hasNext()) {
         CoreGeoBone parentBone = (CoreGeoBone)var3.next();
         RenderUtils.prepMatrixForBone(poseStack, parentBone);
      }

   }

   public BakedGeoModel baseModel() {
      return this.baseModel;
   }

   public CoreGeoBone baseBone() {
      return this.baseBone;
   }

   public BakedGeoModel attachmentModel() {
      return this.attachmentModel;
   }

   public CoreGeoBone attachmentBone() {
      return this.attachmentBone;
   }

   public static class ModelBoneKey {
      BakedGeoModel model;
      String boneName;
      float scale;

      public ModelBoneKey(BakedGeoModel model, String boneName, float scale) {
         this.model = model;
         this.boneName = boneName;
         this.scale = scale;
      }

      public int hashCode() {
         return Objects.hash(new Object[]{this.boneName, this.model, this.scale});
      }

      public boolean equals(Object obj) {
         if (this == obj) {
            return true;
         } else if (obj == null) {
            return false;
         } else if (this.getClass() != obj.getClass()) {
            return false;
         } else {
            ModelBoneKey other = (ModelBoneKey)obj;
            return Objects.equals(this.boneName, other.boneName) && Objects.equals(this.model, other.model) && Float.floatToIntBits(this.scale) == Float.floatToIntBits(other.scale);
         }
      }
   }
}
