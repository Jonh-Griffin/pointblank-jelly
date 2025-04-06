package com.vicmatskiv.pointblank.item;

import com.google.gson.JsonObject;
import com.vicmatskiv.pointblank.attachment.Attachment;
import com.vicmatskiv.pointblank.attachment.AttachmentCategory;
import com.vicmatskiv.pointblank.attachment.AttachmentHost;
import com.vicmatskiv.pointblank.attachment.Attachments;
import com.vicmatskiv.pointblank.client.render.AttachmentModelRenderer;
import com.vicmatskiv.pointblank.crafting.Craftable;
import com.vicmatskiv.pointblank.feature.Feature;
import com.vicmatskiv.pointblank.feature.FeatureBuilder;
import com.vicmatskiv.pointblank.feature.FeatureProvider;
import com.vicmatskiv.pointblank.feature.Features;
import com.vicmatskiv.pointblank.registry.ItemRegistry;
import com.vicmatskiv.pointblank.util.JsonUtil;
import com.vicmatskiv.pointblank.util.TimeUnit;
import com.vicmatskiv.pointblank.util.Tradeable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class AttachmentItem extends Item implements GeoItem, Attachment, AttachmentHost, FeatureProvider, Craftable, Tradeable {
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private String name;
   private AttachmentCategory category;
   private List<String> compatibleAttachmentGroups;
   private List<Supplier<Attachment>> compatibleAttachmentSuppliers;
   private List<Attachment> compatibleAttachments;
   private Set<String> groups;
   private Map<Class<? extends Feature>, Feature> features;
   private long craftingDuration;
   private float tradePrice;
   private int tradeBundleQuantity;
   private int tradeLevel;
   private List<Component> descriptionLines;
   public List<Supplier<Attachment>> defaultAttachmentSuppliers;

   public AttachmentItem() {
      super(new Properties());
      SingletonGeoAnimatable.registerSyncedAnimatable(this);
   }

   public void initializeClient(Consumer<IClientItemExtensions> consumer) {
      consumer.accept(new IClientItemExtensions() {
         private AttachmentModelRenderer renderer;

         public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            if (this.renderer == null) {
               this.renderer = new AttachmentModelRenderer(AttachmentItem.this.name);
            }

            return this.renderer;
         }
      });
   }

   public void registerControllers(ControllerRegistrar controllers) {
   }

   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }

   public String getName() {
      return this.name;
   }

   public Collection<Feature> getFeatures() {
      return Collections.unmodifiableCollection(this.features.values());
   }

   public List<Component> getDescriptionTooltipLines() {
      return this.descriptionLines;
   }

   public void m_7373_(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag flag) {
   }

   public AttachmentCategory getCategory() {
      return this.category;
   }

   public Set<String> getGroups() {
      return this.groups;
   }

   public int getMaxStackSize(ItemStack stack) {
      return 1;
   }

   public Collection<Attachment> getCompatibleAttachments() {
      if (this.compatibleAttachments == null) {
         this.compatibleAttachments = new ArrayList();
         Iterator var1 = this.compatibleAttachmentSuppliers.iterator();

         while(var1.hasNext()) {
            Supplier<Attachment> cas = (Supplier)var1.next();
            this.compatibleAttachments.add((Attachment)cas.get());
         }

         var1 = this.compatibleAttachmentGroups.iterator();

         while(var1.hasNext()) {
            String group = (String)var1.next();
            List<Supplier<? extends Item>> groupAtttachments = ItemRegistry.ITEMS.getAttachmentsForGroup(group);
            Iterator var4 = groupAtttachments.iterator();

            while(var4.hasNext()) {
               Supplier<? extends Item> ga = (Supplier)var4.next();
               Item item = (Item)ga.get();
               if (item instanceof Attachment) {
                  Attachment attachment = (Attachment)item;
                  this.compatibleAttachments.add(attachment);
               }
            }
         }
      }

      return this.compatibleAttachments;
   }

   public Component m_7626_(ItemStack itemStack) {
      return Component.m_237115_(this.m_5671_(itemStack));
   }

   public <T extends Feature> T getFeature(Class<T> featureClass) {
      return (Feature)featureClass.cast(this.features.get(featureClass));
   }

   public long getCraftingDuration() {
      return this.craftingDuration;
   }

   public float getPrice() {
      return this.tradePrice;
   }

   public int getTradeLevel() {
      return this.tradeLevel;
   }

   public int getBundleQuantity() {
      return this.tradeBundleQuantity;
   }

   public List<Attachment> getDefaultAttachments() {
      return this.defaultAttachmentSuppliers.stream().map(Supplier::get).toList();
   }

   public void m_6883_(ItemStack itemStack, Level level, Entity entity, int itemSlot, boolean isSelected) {
      if (!level.f_46443_) {
         this.ensureItemStack(itemStack);
      }

   }

   public void ensureItemStack(ItemStack itemStack) {
      CompoundTag stateTag = itemStack.m_41783_();
      if (stateTag == null) {
         stateTag = new CompoundTag();
         itemStack.m_41751_(stateTag);
         Item var4 = itemStack.m_41720_();
         if (var4 instanceof AttachmentHost) {
            AttachmentHost attachmentHost = (AttachmentHost)var4;
            Collection<Attachment> defaultAttachments = attachmentHost.getDefaultAttachments();
            Iterator var5 = defaultAttachments.iterator();

            while(var5.hasNext()) {
               Attachment attachment = (Attachment)var5.next();
               Attachments.addAttachment(itemStack, new ItemStack(attachment), true);
            }
         }
      }

   }

   public static class Builder extends ItemBuilder<Builder> {
      private static final int DEFAULT_CRAFTING_DURATION = 750;
      private static final float DEFAULT_PRICE = Float.NaN;
      private static final int DEFAULT_TRADE_LEVEL = 0;
      private static final int DEFAULT_TRADE_BUNDLE_QUANTITY = 1;
      private String name;
      private AttachmentCategory category;
      private List<Supplier<Attachment>> compatibleAttachmentSuppliers = new ArrayList();
      private List<String> compatibleAttachmentGroups = new ArrayList();
      private Set<String> groups = new HashSet();
      private List<FeatureBuilder<?, ?>> featureBuilders = new ArrayList();
      private long craftingDuration = 750L;
      private float tradePrice = Float.NaN;
      private int tradeBundleQuantity = 1;
      private int tradeLevel = 0;
      private List<Component> descriptionLines = new ArrayList();
      private List<Supplier<Attachment>> defaultAttachments = new ArrayList();

      public Builder withName(String name) {
         this.name = name;
         return this;
      }

      public Builder withCategory(String categoryName) {
         this.category = AttachmentCategory.fromString(categoryName);
         return this;
      }

      public Builder withCategory(AttachmentCategory category) {
         this.category = category;
         return this;
      }

      public Builder withDescription(Component description) {
         this.descriptionLines.add(description);
         return this;
      }

      public Builder withDescription(String description) {
         this.descriptionLines.add(Component.m_237115_(description).m_130940_(ChatFormatting.RED).m_130940_(ChatFormatting.ITALIC));
         return this;
      }

      public Builder withFeature(FeatureBuilder<?, ?> featureBuilder) {
         this.featureBuilders.add(featureBuilder);
         return this;
      }

      @SafeVarargs
      public final Builder withCompatibleAttachment(Supplier<? extends Attachment>... attachmentSuppliers) {
         Supplier[] var2 = attachmentSuppliers;
         int var3 = attachmentSuppliers.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            Supplier<? extends Attachment> s = var2[var4];
            List var10000 = this.compatibleAttachmentSuppliers;
            Objects.requireNonNull(s);
            var10000.add(s::get);
         }

         return this;
      }

      public Builder withCompatibleAttachmentGroup(String... groups) {
         this.compatibleAttachmentGroups.addAll(Set.of(groups));
         return this;
      }

      public Builder withGroup(String... groups) {
         this.groups.addAll(Set.of(groups));
         return this;
      }

      public Builder withCraftingDuration(int duration, TimeUnit timeUnit) {
         this.craftingDuration = timeUnit.toMillis((long)duration);
         return this;
      }

      public Builder withTradePrice(double price, int tradeLevel) {
         this.tradePrice = (float)price;
         this.tradeLevel = tradeLevel;
         this.tradeBundleQuantity = 1;
         return this;
      }

      public Set<String> getGroups() {
         return Collections.unmodifiableSet(this.groups);
      }

      @SafeVarargs
      public final Builder withDefaultAttachment(Supplier<? extends Attachment>... attachmentSuppliers) {
         Supplier[] var2 = attachmentSuppliers;
         int var3 = attachmentSuppliers.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            Supplier<? extends Attachment> s = var2[var4];
            List var10000 = this.defaultAttachments;
            Objects.requireNonNull(s);
            var10000.add(s::get);
         }

         return this;
      }

      public Builder withJsonObject(JsonObject obj) {
         this.withName(JsonUtil.getJsonString(obj, "name"));
         String description = JsonUtil.getJsonString(obj, "description", (String)null);
         if (description != null) {
            this.withDescription(description);
         }

         this.withCategory(JsonUtil.getJsonString(obj, "category"));
         this.withTradePrice((double)JsonUtil.getJsonFloat(obj, "tradePrice", Float.NaN), JsonUtil.getJsonInt(obj, "tradeLevel", 0));
         this.withCraftingDuration(JsonUtil.getJsonInt(obj, "craftingDuration", 750), TimeUnit.MILLISECOND);
         List<String> groups = JsonUtil.getStrings(obj, "groups");
         this.groups.addAll(groups);
         Iterator var4 = JsonUtil.getJsonObjects(obj, "features").iterator();

         while(var4.hasNext()) {
            JsonObject featureObj = (JsonObject)var4.next();
            FeatureBuilder<?, ?> featureBuilder = Features.fromJson(featureObj);
            this.withFeature(featureBuilder);
         }

         List<String> compatibleAttachmentNames = JsonUtil.getStrings(obj, "compatibleAttachments");
         Iterator var11 = compatibleAttachmentNames.iterator();

         while(var11.hasNext()) {
            String compatibleAttachmentName = (String)var11.next();
            Supplier<Item> ri = ItemRegistry.ITEMS.getDeferredRegisteredObject(compatibleAttachmentName);
            if (ri != null) {
               this.withCompatibleAttachment(() -> {
                  return (Attachment)ri.get();
               });
            }
         }

         List<String> compatibleAttachmentGroups = JsonUtil.getStrings(obj, "compatibleAttachmentGroups");
         this.compatibleAttachmentGroups.addAll(compatibleAttachmentGroups);
         List<String> defaultAttachmentNames = JsonUtil.getStrings(obj, "defaultAttachments");
         Iterator var15 = defaultAttachmentNames.iterator();

         while(var15.hasNext()) {
            String defaultAttachmentName = (String)var15.next();
            Supplier<Item> ri = ItemRegistry.ITEMS.getDeferredRegisteredObject(defaultAttachmentName);
            if (ri != null) {
               this.withDefaultAttachment(() -> {
                  return (Attachment)ri.get();
               });
            }
         }

         return this;
      }

      public AttachmentItem build() {
         if (this.name == null) {
            throw new IllegalStateException("Attachment name not set");
         } else if (this.category == null) {
            throw new IllegalStateException("Attachment category not set");
         } else {
            AttachmentItem attachment = new AttachmentItem();
            attachment.name = this.name;
            attachment.descriptionLines = Collections.unmodifiableList(this.descriptionLines);
            attachment.category = this.category;
            attachment.compatibleAttachmentSuppliers = Collections.unmodifiableList(this.compatibleAttachmentSuppliers);
            attachment.compatibleAttachmentGroups = Collections.unmodifiableList(this.compatibleAttachmentGroups);
            attachment.groups = Collections.unmodifiableSet(this.groups);
            Map<Class<? extends Feature>, Feature> features = new HashMap();
            Iterator var3 = this.category.getDefaultFeatures().iterator();

            FeatureBuilder featureBuilder;
            Feature feature;
            while(var3.hasNext()) {
               featureBuilder = (FeatureBuilder)var3.next();
               feature = featureBuilder.build(attachment);
               features.put(feature.getClass(), feature);
            }

            var3 = this.featureBuilders.iterator();

            while(var3.hasNext()) {
               featureBuilder = (FeatureBuilder)var3.next();
               feature = featureBuilder.build(attachment);
               features.put(feature.getClass(), feature);
            }

            attachment.features = Collections.unmodifiableMap(features);
            attachment.craftingDuration = this.craftingDuration;
            attachment.tradePrice = this.tradePrice;
            attachment.tradeBundleQuantity = this.tradeBundleQuantity;
            attachment.tradeLevel = this.tradeLevel;
            attachment.defaultAttachmentSuppliers = Collections.unmodifiableList(this.defaultAttachments);
            return attachment;
         }
      }

      public String getName() {
         return this.name;
      }
   }
}
