package base_pack.assets.pointblank.scripts


import com.vicmatskiv.pointblank.feature.ConditionContext
import com.vicmatskiv.pointblank.item.AnimationProvider
import com.vicmatskiv.pointblank.item.FireModeInstance
import com.vicmatskiv.pointblank.item.GunItem
import com.vicmatskiv.pointblank.util.Conditions
import net.minecraft.world.item.ItemStack

String getFireAnimation(ItemStack stack, FireModeInstance fireMode, AnimationProvider.Descriptor descriptor) {
    if (Conditions.hasAttachment("m9_stock").test(new ConditionContext(stack))) {
       // println("test complete")
        return !GunItem.isAiming(stack) ? "animation.model.fire" : "animation.model.firestock"
    }
    return "animation.model.fire"
}