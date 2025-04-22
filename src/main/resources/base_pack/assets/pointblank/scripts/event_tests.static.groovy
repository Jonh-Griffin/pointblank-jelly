import com.vicmatskiv.pointblank.PointBlankJelly
import com.vicmatskiv.pointblank.util.Scripts
import com.vicmatskiv.pointblank.util.Scripts.PacketContext
import groovy.transform.Field
import net.minecraft.client.KeyMapping
import net.minecraft.nbt.CompoundTag
import net.minecraftforge.client.event.RegisterKeyMappingsEvent
import net.minecraftforge.event.TickEvent
import net.minecraftforge.network.NetworkEvent.Context
import org.lwjgl.glfw.GLFW

@Field //@Field is required for static (static means you can call it from outside scripts in this case)
// other scripts can call this with {script_name}.keyMapping, main usage is keyMapping.consumeClick()
static final KeyMapping keyMapping = new KeyMapping
        (
                "Test Key", //Key name, can also be in lang file format
                GLFW.GLFW_KEY_J, //Default key, use GLFW for the key codes
                "content.category" //Category, can be in string or lang file format
        )

Scripts.registerForgeEvent({testClickEvent(it)}, TickEvent.ClientTickEvent.class)
Scripts.registerModEvent({registerKeys(it)},RegisterKeyMappingsEvent.class)

static void testClickEvent(TickEvent.ClientTickEvent event) {
    //For all keys you must send a packet, unless its a client action.
    if(keyMapping.consumeClick()) {
        println("groovy key pressed")
        Scripts.runFunctionOnServer(new PacketContext("event_tests:examplePacket", 1, false, new CompoundTag()))
    }
}
//ItemStack can be ItemStack.EMPTY if no itemstack is supplied into the function
static void examplePacket(int i, boolean b, CompoundTag tag, Context ctx) {
    PointBlankJelly.LOGGER.info("LOGGED FROM GROOVY PACKET ON SIDE {} | {} | {} | {}", ctx.direction.receptionSide, i, b, tag)
}

static void registerKeys(RegisterKeyMappingsEvent keyMappingsEvent) {
    keyMappingsEvent.register(keyMapping)
}