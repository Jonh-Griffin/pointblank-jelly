package com.vicmatskiv.pointblank.registry;

import com.vicmatskiv.pointblank.Enableable;
import com.vicmatskiv.pointblank.Nameable;
import com.vicmatskiv.pointblank.config.ConfigManager;
import com.vicmatskiv.pointblank.config.Configurable;
import com.vicmatskiv.pointblank.entity.EntityBuilderProvider;
import com.vicmatskiv.pointblank.item.AttachmentItem;
import com.vicmatskiv.pointblank.item.ItemBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ItemRegistry {
   private Map<String, Supplier<? extends Item>> itemsByName = new LinkedHashMap();
   private Map<String, List<Supplier<? extends Item>>> itemsByGroup = new LinkedHashMap();
   private ConfigManager.Builder gunConfigBuilder = new ConfigManager.Builder();
   private List<DeferredRegistration> deferredRegistrations = new ArrayList();
   public static final ItemRegistry ITEMS = new ItemRegistry();
   public static final DeferredRegister<Item> itemRegistry;
   public static final DeferredRegister<CreativeModeTab> TABS;
   public static final Supplier<CreativeModeTab> POINTBLANK_TAB;

   public Map<String, Supplier<? extends Item>> getItemsByName() {
      return Collections.unmodifiableMap(this.itemsByName);
   }

   public List<Supplier<? extends Item>> getAttachmentsForGroup(String group) {
      List<Supplier<? extends Item>> groupAttachments = (List)this.itemsByGroup.get(group);
      return groupAttachments != null ? Collections.unmodifiableList(groupAttachments) : Collections.emptyList();
   }

   public <I extends Item, T extends ItemBuilder<T> & Nameable> Supplier<I> register(ItemBuilder<?> itemBuilder) {
      String name = itemBuilder.getName();
      if (itemBuilder instanceof Configurable) {
         Configurable configurable = (Configurable)itemBuilder;
         configurable.configure(this.gunConfigBuilder);
      }

      DeferredRegistration ro = new DeferredRegistration(itemBuilder);
      this.deferredRegistrations.add(ro);
      Map var10000 = this.itemsByName;
      Objects.requireNonNull(ro);
      var10000.put(name, ro::get);
      if (itemBuilder instanceof AttachmentItem.Builder) {
         AttachmentItem.Builder attachmentBuilder = (AttachmentItem.Builder)itemBuilder;
         Iterator var5 = attachmentBuilder.getGroups().iterator();

         while(var5.hasNext()) {
            String group = (String)var5.next();
            List<Supplier<? extends Item>> groupMembers = (List)this.itemsByGroup.computeIfAbsent(group, (g) -> {
               return new ArrayList();
            });
            Objects.requireNonNull(ro);
            groupMembers.add(ro::get);
         }
      }

      EntityBuilderProvider entityBuilderProvider = itemBuilder.getEntityBuilderProvider();
      if (entityBuilderProvider != null) {
         EntityRegistry.registerItemEntity(name, () -> {
            return entityBuilderProvider.getEntityBuilder();
         });
      }

      return () -> {
         return ro.get();
      };
   }

   public <I extends Item> Supplier<I> getDeferredRegisteredObject(String name) {
      return () -> {
         Supplier<?> registryObject = (Supplier)this.itemsByName.get(name);
         return registryObject != null ? (Item)registryObject.get() : null;
      };
   }

   public void register(IEventBus modEventBus) {
      itemRegistry.register(modEventBus);
   }

   public void syncEnabledItems(List<Integer> itemIds) {
   }

   public void complete() {
      this.gunConfigBuilder.build();
      Iterator it = this.deferredRegistrations.iterator();

      while(true) {
         while(it.hasNext()) {
            DeferredRegistration dr = (DeferredRegistration)it.next();
            ItemBuilder var4 = dr.itemBuilder;
            if (var4 instanceof Enableable) {
               Enableable e = (Enableable)var4;
               if (!e.isEnabled()) {
                  it.remove();
                  dr.resolve(false);
                  continue;
               }
            }

            dr.resolve(true);
         }

         return;
      }
   }

   static {
      itemRegistry = DeferredRegister.create(ForgeRegistries.ITEMS, "pointblank");
      TABS = DeferredRegister.create(Registries.f_279569_, "pointblank");
      MiscItemRegistry.init();
      AmmoRegistry.init();
      AttachmentRegistry.init();
      GunRegistry.init();
      POINTBLANK_TAB = TABS.register("pointblank", () -> {
         return CreativeModeTab.builder().m_257941_(Component.m_237115_("itemGroup.pointblank.items")).m_257737_(() -> {
            return new ItemStack((ItemLike)(GunRegistry.M4A1.get() != null ? (ItemLike)GunRegistry.M4A1.get() : Items.f_41852_));
         }).m_257501_((enabledFeatures, entries) -> {
            Consumer<ItemLike> output = new Consumer<ItemLike>() {
               public void accept(ItemLike itemLike) {
                  if (itemLike != null && itemLike != Items.f_41852_) {
                     if (itemLike instanceof Enableable) {
                        Enableable e = (Enableable)itemLike;
                        if (!e.isEnabled()) {
                           return;
                        }
                     }

                     entries.m_246326_(itemLike);
                  }

               }
            };
            GunRegistry.registerTabItems(output);
            AttachmentRegistry.registerTabItems(output);
            AmmoRegistry.registerTabItems(output);
            MiscItemRegistry.registerTabItems(output);
         }).m_257652_();
      });
   }

   private class DeferredRegistration {
      private Supplier<Item> supplier;
      private ItemBuilder<?> itemBuilder;

      DeferredRegistration(ItemBuilder<?> itemBuilder) {
         this.itemBuilder = itemBuilder;
      }

      void resolve(boolean register) {
         String name = this.itemBuilder.getName();
         if (register) {
            RegistryObject<Item> ro = ItemRegistry.itemRegistry.register(name, () -> {
               return this.itemBuilder.build();
            });
            this.supplier = () -> {
               return (Item)ro.orElse((Object)null);
            };
         } else {
            this.supplier = () -> {
               return null;
            };
         }

      }

      public Item get() {
         return (Item)this.supplier.get();
      }
   }
}
