package base_pack.assets.pointblank.scripts

import com.vicmatskiv.pointblank.item.GunItem
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack


boolean onLeftClickEntity(ItemStack itemStack, Player plr, Entity swungAt) {
    if (swungAt instanceof LivingEntity) {
        swungAt.hurt(plr.damageSources().mobAttack(plr), GunItem.getFireModeInstance(itemStack).damage)
    }
    return false
}

boolean onEntitySwing(ItemStack itemStack, LivingEntity heldEntity) {
    return false
}

