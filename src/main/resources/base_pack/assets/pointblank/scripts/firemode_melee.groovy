package base_pack.assets.pointblank.scripts


import com.vicmatskiv.pointblank.item.AnimationProvider
import com.vicmatskiv.pointblank.item.FireModeInstance
import net.minecraft.world.item.ItemStack

String getFireAnimation(ItemStack stack, FireModeInstance fireMode, AnimationProvider.Descriptor descriptor) {
    return "animation.model.melee"
}