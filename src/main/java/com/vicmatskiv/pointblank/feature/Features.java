package com.vicmatskiv.pointblank.feature;

import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.vicmatskiv.pointblank.attachment.Attachment;
import com.vicmatskiv.pointblank.attachment.Attachments;
import com.vicmatskiv.pointblank.client.effect.EffectBuilder;
import com.vicmatskiv.pointblank.item.GunItem;
import com.vicmatskiv.pointblank.registry.FeatureTypeRegistry;
import com.vicmatskiv.pointblank.util.JsonUtil;
import com.vicmatskiv.pointblank.util.LRUCache;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.NavigableMap;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class Features {
   private static final LRUCache<Pair<Tag, Class<? extends Feature>>, List<EnabledFeature>> selectedItemFeatureCache = new LRUCache(200);
   private static final LRUCache<Pair<Tag, GunItem.FirePhase>, List<Pair<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>, Predicate<ConditionContext>>>> enabledPhaseEffects = new LRUCache(100);

   public static <T extends Feature> EnabledFeature getFirstEnabledFeature(ItemStack itemStack, Class<? extends Feature> featureClass) {
      List<EnabledFeature> enabledFeatures = getEnabledFeatures(itemStack, featureClass);
      return !enabledFeatures.isEmpty() ? (EnabledFeature)enabledFeatures.get(0) : null;
   }

   public static <T extends Feature> List<EnabledFeature> getEnabledFeatures(ItemStack itemStack, Class<? extends Feature> featureClass) {
      CompoundTag tag = itemStack.m_41783_();
      return tag == null ? Collections.emptyList() : (List)selectedItemFeatureCache.computeIfAbsent(Pair.of(tag, featureClass), (p) -> {
         return computeEnabledFeatures(itemStack, featureClass);
      });
   }

   private static List<EnabledFeature> computeEnabledFeatures(ItemStack rootStack, Class<? extends Feature> featureType) {
      NavigableMap<String, ItemStack> attachmentStacks = Attachments.getAttachments(rootStack, true);
      List<EnabledFeature> result = new ArrayList();
      Iterator var4 = attachmentStacks.entrySet().iterator();

      while(var4.hasNext()) {
         Entry<String, ItemStack> attachmentEntry = (Entry)var4.next();
         Item var7 = ((ItemStack)attachmentEntry.getValue()).m_41720_();
         if (var7 instanceof FeatureProvider) {
            FeatureProvider fp = (FeatureProvider)var7;
            Feature feature = fp.getFeature(featureType);
            if (feature != null && ((ItemStack)attachmentEntry.getValue()).m_41720_() instanceof Attachment && feature.isEnabledForAttachment(rootStack, (ItemStack)attachmentEntry.getValue())) {
               result.add(new EnabledFeature(feature, (ItemStack)attachmentEntry.getValue(), (String)attachmentEntry.getKey()));
            }
         }
      }

      Item var9 = rootStack.m_41720_();
      if (var9 instanceof FeatureProvider) {
         FeatureProvider fp = (FeatureProvider)var9;
         Feature feature = fp.getFeature(featureType);
         if (feature != null && feature.isEnabled(rootStack)) {
            result.add(new EnabledFeature(feature, rootStack, "/"));
         }
      }

      return result;
   }

   public static boolean hasFeature(ItemStack itemStack, Feature feature) {
      Item var3 = itemStack.m_41720_();
      if (var3 instanceof FeatureProvider) {
         FeatureProvider fp = (FeatureProvider)var3;
         return fp.hasFeature(feature);
      } else {
         return false;
      }
   }

   public static List<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>> getEnabledPhaseEffects(ItemStack itemStack, GunItem.FirePhase phase) {
      CompoundTag tag = itemStack.m_41783_();
      if (tag == null) {
         return Collections.emptyList();
      } else {
         List<Pair<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>, Predicate<ConditionContext>>> conditionalEffects = (List)enabledPhaseEffects.computeIfAbsent(Pair.of(tag, phase), (p) -> {
            return computeEnabledPhaseEffects(itemStack, phase);
         });
         ConditionContext context = new ConditionContext(itemStack);
         return conditionalEffects.stream().filter((p) -> {
            return ((Predicate)p.getSecond()).test(context);
         }).map((p) -> {
            return (Supplier)p.getFirst();
         }).toList();
      }
   }

   private static List<Pair<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>, Predicate<ConditionContext>>> computeEnabledPhaseEffects(ItemStack rootStack, GunItem.FirePhase firePhase) {
      NavigableMap<String, ItemStack> attachmentStacks = Attachments.getAttachments(rootStack, true);
      List<Pair<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>, Predicate<ConditionContext>>> result = new ArrayList();
      Iterator var4 = FeatureTypeRegistry.getFeatureTypes().values().iterator();

      while(var4.hasNext()) {
         Class<? extends Feature> featureType = (Class)var4.next();
         Iterator var6 = attachmentStacks.entrySet().iterator();

         while(var6.hasNext()) {
            Entry<String, ItemStack> attachmentEntry = (Entry)var6.next();
            Item var9 = ((ItemStack)attachmentEntry.getValue()).m_41720_();
            if (var9 instanceof FeatureProvider) {
               FeatureProvider fp = (FeatureProvider)var9;
               Feature feature = fp.getFeature(featureType);
               if (feature != null && ((ItemStack)attachmentEntry.getValue()).m_41720_() instanceof Attachment && feature.isEnabledForAttachment(rootStack, (ItemStack)attachmentEntry.getValue())) {
                  List<Pair<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>, Predicate<ConditionContext>>> effectBuilders = (List)feature.getEffectBuilders().get(firePhase);
                  if (effectBuilders != null) {
                     result.addAll(effectBuilders);
                  }
               }
            }
         }

         Item var12 = rootStack.m_41720_();
         if (var12 instanceof FeatureProvider) {
            FeatureProvider fp = (FeatureProvider)var12;
            Feature feature = fp.getFeature(featureType);
            if (feature != null && feature.isEnabled(rootStack)) {
               List<Pair<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>, Predicate<ConditionContext>>> effectBuilders = (List)feature.getEffectBuilders().get(firePhase);
               if (effectBuilders != null) {
                  result.addAll(effectBuilders);
               }
            }
         }
      }

      return result;
   }

   public static FeatureBuilder<?, ?> fromJson(JsonObject obj) {
      String featureType = JsonUtil.getJsonString(obj, "type");
      String var2 = featureType.toUpperCase(Locale.ROOT);
      byte var3 = -1;
      switch(var2.hashCode()) {
      case -1881579710:
         if (var2.equals("RECOIL")) {
            var3 = 9;
         }
         break;
      case -1881311847:
         if (var2.equals("RELOAD")) {
            var3 = 5;
         }
         break;
      case -1349080839:
         if (var2.equals("ACCURACY")) {
            var3 = 0;
         }
         break;
      case -1199894001:
         if (var2.equals("MUZZLEFLASH")) {
            var3 = 6;
         }
         break;
      case 79223:
         if (var2.equals("PIP")) {
            var3 = 8;
         }
         break;
      case 2547069:
         if (var2.equals("SKIN")) {
            var3 = 12;
         }
         break;
      case 79089903:
         if (var2.equals("SOUND")) {
            var3 = 11;
         }
         break;
      case 119904069:
         if (var2.equals("PARTVISIBILITY")) {
            var3 = 7;
         }
         break;
      case 219616473:
         if (var2.equals("FIREMODE")) {
            var3 = 3;
         }
         break;
      case 582290824:
         if (var2.equals("AMMOCAPACITY")) {
            var3 = 4;
         }
         break;
      case 1816086548:
         if (var2.equals("RETICLE")) {
            var3 = 10;
         }
         break;
      case 1930678397:
         if (var2.equals("AIMING")) {
            var3 = 1;
         }
         break;
      case 2009169775:
         if (var2.equals("DAMAGE")) {
            var3 = 2;
         }
      }

      switch(var3) {
      case 0:
         return (new AccuracyFeature.Builder()).withJsonObject(obj);
      case 1:
         return (new AimingFeature.Builder()).withJsonObject(obj);
      case 2:
         return (new DamageFeature.Builder()).withJsonObject(obj);
      case 3:
         return (new FireModeFeature.Builder()).withJsonObject(obj);
      case 4:
         return (new AmmoCapacityFeature.Builder()).withJsonObject(obj);
      case 5:
         return (new ReloadFeature.Builder()).withJsonObject(obj);
      case 6:
         return (new MuzzleFlashFeature.Builder()).withJsonObject(obj);
      case 7:
         return (new PartVisibilityFeature.Builder()).withJsonObject(obj);
      case 8:
         return (new PipFeature.Builder()).withJsonObject(obj);
      case 9:
         return (new RecoilFeature.Builder()).withJsonObject(obj);
      case 10:
         return (new ReticleFeature.Builder()).withJsonObject(obj);
      case 11:
         return (new SoundFeature.Builder()).withJsonObject(obj);
      case 12:
         return (new SkinFeature.Builder()).withJsonObject(obj);
      default:
         throw new IllegalArgumentException("Invalid feature type: " + featureType);
      }
   }

   public static record EnabledFeature(Feature feature, ItemStack ownerStack, String ownerPath) {
      public EnabledFeature(Feature feature, ItemStack ownerStack, String ownerPath) {
         this.feature = feature;
         this.ownerStack = ownerStack;
         this.ownerPath = ownerPath;
      }

      public Feature feature() {
         return this.feature;
      }

      public ItemStack ownerStack() {
         return this.ownerStack;
      }

      public String ownerPath() {
         return this.ownerPath;
      }
   }
}
