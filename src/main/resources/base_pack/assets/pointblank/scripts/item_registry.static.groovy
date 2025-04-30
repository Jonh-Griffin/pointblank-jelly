package base_pack.assets.pointblank.scripts

import groovy.transform.Field
import net.minecraft.world.item.Item
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries

@Field
static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "content_pack")
ITEMS.register("test_item", { new Item(new Item.Properties()) })

static void init(IEventBus modEventBus) {
    ITEMS.register(modEventBus)
}