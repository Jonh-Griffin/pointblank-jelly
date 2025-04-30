package base_pack.assets.pointblank.scripts

import com.vicmatskiv.pointblank.feature.ConditionContext
import com.vicmatskiv.pointblank.util.Conditions
import com.vicmatskiv.pointblank.util.Scripts
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ItemStack
import net.minecraftforge.event.TickEvent
import software.bernie.geckolib.animatable.GeoItem

static def toggleNightVision(ItemStack itemStack) {
    if(itemStack.getOrCreateTag().contains("nvg"))
        itemStack.getOrCreateTag().putBoolean("nvg", !itemStack.getOrCreateTag().getBoolean("nvg"))
    else
        itemStack.getOrCreateTag().putBoolean("nvg", true)
}

Scripts.registerForgeEvent({playerTickEvent(it)}, TickEvent.PlayerTickEvent.class)
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
