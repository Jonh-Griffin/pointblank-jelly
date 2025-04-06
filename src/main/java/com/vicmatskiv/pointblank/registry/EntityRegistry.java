package com.vicmatskiv.pointblank.registry;

import com.vicmatskiv.pointblank.entity.EntityBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class EntityRegistry {
   public static final DeferredRegister<EntityType<?>> ENTITIES;
   private static final List<EntityBuilder<?, ?>> entityBuilders;
   private static final Map<EntityKey, Supplier<EntityBuilder<?, ?>>> entityBuildersByNameType;
   private static final Map<String, RegistryObject<EntityType<?>>> typesByName;
   private static final Map<RegistryObject<EntityType<?>>, Supplier<EntityBuilder<?, ?>>> itemEntityBuilders;

   public static Supplier<EntityBuilder<?, ?>> getEntityBuilder(String name, EntityBuilder.EntityTypeExt type) {
      Supplier<EntityBuilder<?, ?>> supplier = entityBuildersByNameType.get(new EntityKey(name, type));
      if (supplier == null) {
         throw new IllegalArgumentException("Entity '" + name + "' of type '" + type + "' not found");
      } else {
         return supplier;
      }
   }

   public static List<EntityBuilder<?, ?>> getEntityBuilders() {
      return Collections.unmodifiableList(entityBuilders);
   }

   public static RegistryObject<EntityType<?>> getTypeByName(String name) {
      return typesByName.get(name);
   }

   public static Map<RegistryObject<EntityType<?>>, Supplier<EntityBuilder<?, ?>>> getItemEntityBuilders() {
      return Collections.unmodifiableMap(itemEntityBuilders);
   }

   public static RegistryObject<EntityType<?>> registerItemEntity(String name, Supplier<EntityBuilder<?, ?>> entityBuilderSupplier) {
      Supplier<EntityType<?>> sup = () -> (entityBuilderSupplier.get()).getEntityTypeBuilder().build(name);
      RegistryObject<EntityType<?>> registeredEntityType = ENTITIES.register(name, sup);
      typesByName.put(name, registeredEntityType);
      itemEntityBuilders.put(registeredEntityType, entityBuilderSupplier);
      return registeredEntityType;
   }

   static {
      ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "pointblank");
      entityBuilders = new ArrayList<>();
      entityBuildersByNameType = new HashMap<>();
      typesByName = new HashMap<>();
      itemEntityBuilders = new HashMap<>();
   }

   private record EntityKey(String name, EntityBuilder.EntityTypeExt type) {

      public boolean equals(Object obj) {
            if (this == obj) {
               return true;
            } else if (obj == null) {
               return false;
            } else if (this.getClass() != obj.getClass()) {
               return false;
            } else {
               EntityKey other = (EntityKey) obj;
               return Objects.equals(this.name, other.name) && this.type == other.type;
            }
         }
      }
}
