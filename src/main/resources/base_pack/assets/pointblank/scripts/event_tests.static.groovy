import com.vicmatskiv.pointblank.util.Scripts
import net.minecraftforge.event.TickEvent

Scripts.registerEvent({testClickEvent(it)}, TickEvent.ClientTickEvent.class, true)

static void testClickEvent(TickEvent.ClientTickEvent event) {
    println("groovy event tick + " + event.phase)
}