package base_pack.assets.pointblank.scripts

import com.vicmatskiv.pointblank.util.Scripts
import net.minecraft.client.player.LocalPlayer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

void armorTick$A(ItemStack stack, Level pLevel, Player pEntity) {
    pEntity.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 200, 0, true, false, false))
    if(pEntity instanceof ServerPlayer) {
        Scripts.runFunctionOnClient("test_client", stack, pEntity)
    }
}

void test_client(LocalPlayer player, ItemStack stack) {
    println("on client " + player.getName().contents.toString())
}