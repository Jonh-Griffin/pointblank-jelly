import com.vicmatskiv.pointblank.PointBlankJelly
import com.vicmatskiv.pointblank.feature.ConditionContext
import com.vicmatskiv.pointblank.util.Conditions
import com.vicmatskiv.pointblank.util.Scripts
import com.vicmatskiv.pointblank.util.Scripts.PacketContext
import groovy.transform.Field
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ItemStack
import net.minecraftforge.client.event.RegisterKeyMappingsEvent
import net.minecraftforge.event.TickEvent
import net.minecraftforge.network.NetworkEvent.Context
import org.lwjgl.glfw.GLFW
import software.bernie.geckolib.animatable.GeoItem

@Field //@Field is required for static (static means you can call it from outside scripts in this case)
// other scripts can call this with {script_name}.keyMapping, main usage is keyMapping.consumeClick()
static final KeyMapping keyMapping = new KeyMapping
        (
                "NVG Toggle", //Key name, can also be in lang file format
                GLFW.GLFW_KEY_J, //Default key, use GLFW for the key codes
                "content.category" //Category, can be in string or lang file format
        )

Scripts.registerForgeEvent({clientTickEvent(it)}, TickEvent.ClientTickEvent.class)
Scripts.registerForgeEvent({playerTickEvent(it)}, TickEvent.PlayerTickEvent.class)
Scripts.registerModEvent({registerKeys(it)}, RegisterKeyMappingsEvent.class)

static def clientTickEvent(TickEvent.ClientTickEvent event) {
    //For all keys you must send a packet, unless its a client action.
    if(keyMapping.consumeClick()) {
        ItemStack itemStack = Minecraft.getInstance().player.getItemBySlot(EquipmentSlot.HEAD)
        if(Conditions.hasAttachment("gpnvg").test(new ConditionContext(itemStack))) {
            Scripts.runFunctionOnServer(new PacketContext("events:toggleNightVision", itemStack))
            toggleNightVision(itemStack)
        }
    }
}
//ItemStack can be ItemStack.EMPTY if no itemstack is supplied into the function
static def packetSidePacket(int i, boolean b, CompoundTag tag, Context ctx) {
    PointBlankJelly.LOGGER.info("LOGGED FROM GROOVY PACKET ON SIDE {} | {} | {} | {}", ctx.direction.receptionSide, i, b, tag)
}

static def registerKeys(RegisterKeyMappingsEvent keyMappingsEvent) {
    keyMappingsEvent.register(keyMapping)
}

static def toggleNightVision(ItemStack itemStack) {
    if(itemStack.getOrCreateTag().contains("nvg"))
        itemStack.getOrCreateTag().putBoolean("nvg", !itemStack.getOrCreateTag().getBoolean("nvg"))
    else
        itemStack.getOrCreateTag().putBoolean("nvg", true)
}

static def playerTickEvent(TickEvent.PlayerTickEvent playerTickEvent) {
    def itemStack = playerTickEvent.player.getItemBySlot(EquipmentSlot.HEAD)
    if(itemStack.getOrCreateTag().getBoolean("nvg")) {
        playerTickEvent.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1000, 0, true, false, false))
        (itemStack.getItem() as GeoItem).triggerAnim(playerTickEvent.player,GeoItem.getId(itemStack), null, "animation.model.nvgupidle")
    } else if (Conditions.hasAttachment("gpnvg").test(new ConditionContext(itemStack))) {
        playerTickEvent.player.removeEffect(MobEffects.NIGHT_VISION)
        (itemStack.getItem() as GeoItem).triggerAnim(playerTickEvent.player,GeoItem.getId(itemStack), null, "animation.model.nvgdownidle")
    }
}