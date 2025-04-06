package com.vicmatskiv.pointblank.registry;

import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import com.vicmatskiv.pointblank.Config;
import com.vicmatskiv.pointblank.mixin.StructureTemplatePoolMixin;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool.Projection;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class VillagerRegistry {
   public static final DeferredRegister<VillagerProfession> PROFESSIONS;
   public static final DeferredRegister<PoiType> POI_TYPES;
   public static RegistryObject<PoiType> ARMS_DEALER_POI;
   public static RegistryObject<VillagerProfession> ARMS_DEALER_PROFESSION;
   private static final ResourceLocation ARMS_DEALER_BARN_RESOURCE;
   private static final ResourceKey<StructureProcessorList> EMPTY_PROCESSOR_LIST_KEY;
   private static final ResourceLocation[] HOUSES_RESOURCES;

   private static Set<BlockState> getBlockStates(Block block) {
      return ImmutableSet.copyOf(block.m_49965_().m_61056_());
   }

   private static RegistryObject<PoiType> registerPoiType(String name, Supplier<Block> block, int maxTickets, int validRange) {
      return POI_TYPES.register(name, () -> {
         return new PoiType(getBlockStates((Block)block.get()), maxTickets, validRange);
      });
   }

   private static RegistryObject<VillagerProfession> registerProfession(String name, RegistryObject<PoiType> poiHolder) {
      return PROFESSIONS.register(name, () -> {
         return new VillagerProfession(name, (holder) -> {
            return holder.m_203565_(poiHolder.getKey());
         }, (holder) -> {
            return holder.m_203565_(poiHolder.getKey());
         }, ImmutableSet.of(), ImmutableSet.of(), SoundEvents.f_12565_);
      });
   }

   public static void registerStructures(MinecraftServer server) {
      Registry<StructureTemplatePool> templatePoolRegistry = (Registry)server.m_206579_().m_6632_(Registries.f_256948_).orElseThrow();
      Registry<StructureProcessorList> processorListRegistry = (Registry)server.m_206579_().m_6632_(Registries.f_257011_).orElseThrow();
      ResourceLocation[] var3 = HOUSES_RESOURCES;
      int var4 = var3.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         ResourceLocation poolLocation = var3[var5];
         registerStructure(templatePoolRegistry, processorListRegistry, poolLocation, ARMS_DEALER_BARN_RESOURCE, Config.armsDealerHouseWeight);
      }

   }

   public static void registerStructure(Registry<StructureTemplatePool> templatePoolRegistry, Registry<StructureProcessorList> processorListRegistry, ResourceLocation targetPoolResource, ResourceLocation structureResource, int weight) {
      if (weight != 0) {
         Reference<StructureProcessorList> emptyProcessorList = processorListRegistry.m_246971_(EMPTY_PROCESSOR_LIST_KEY);
         StructureTemplatePool pool = (StructureTemplatePool)templatePoolRegistry.m_7745_(targetPoolResource);
         if (pool != null) {
            SinglePoolElement structurePoolElement = (SinglePoolElement)SinglePoolElement.m_210531_(structureResource.toString(), emptyProcessorList).apply(Projection.RIGID);

            for(int i = 0; i < weight; ++i) {
               ((StructureTemplatePoolMixin)pool).getTemplates().add(structurePoolElement);
            }

            List<Pair<StructurePoolElement, Integer>> rawTemplates = new ArrayList(((StructureTemplatePoolMixin)pool).getRawTemplates());
            rawTemplates.add(new Pair(structurePoolElement, weight));
            ((StructureTemplatePoolMixin)pool).setRawTemplates(rawTemplates);
         }
      }
   }

   static {
      PROFESSIONS = DeferredRegister.create(Registries.f_256749_, "pointblank");
      POI_TYPES = DeferredRegister.create(Registries.f_256805_, "pointblank");
      ARMS_DEALER_POI = registerPoiType("arms_dealer", () -> {
         return (Block)BlockRegistry.WORKSTATION.get();
      }, 1, 1);
      ARMS_DEALER_PROFESSION = registerProfession("arms_dealer", ARMS_DEALER_POI);
      ARMS_DEALER_BARN_RESOURCE = new ResourceLocation("pointblank", "village/all_terrain/arms_dealer_barn");
      EMPTY_PROCESSOR_LIST_KEY = ResourceKey.m_135785_(Registries.f_257011_, new ResourceLocation("minecraft", "empty"));
      HOUSES_RESOURCES = new ResourceLocation[]{new ResourceLocation("minecraft", "village/plains/houses"), new ResourceLocation("minecraft", "village/desert/houses"), new ResourceLocation("minecraft", "village/savanna/houses"), new ResourceLocation("minecraft", "village/snowy/houses"), new ResourceLocation("minecraft", "village/taiga/houses")};
   }
}
