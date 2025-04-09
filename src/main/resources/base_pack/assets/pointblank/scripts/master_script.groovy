import com.mojang.datafixers.util.Pair
import com.vicmatskiv.pointblank.client.GunClientState
import com.vicmatskiv.pointblank.feature.*
import com.vicmatskiv.pointblank.item.AnimationProvider
import com.vicmatskiv.pointblank.item.FireModeInstance
import com.vicmatskiv.pointblank.item.GunItem
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.phys.HitResult

import javax.annotation.Nullable
import java.util.logging.Level

/ ^^ Above are all the imports that would be needed for these methods! Useful if you aren't using Intellij /

/ GunItem \ Root Methods /
/**
 * This script is executed after the gun is fired <code>Server</code>.
 * It can be used to add custom behavior when the gun is fired.
 *
 * @param stack The ItemStack of the gun being fired
 * @param plr The player firing the gun
 * @param gunItem The GunItem instance of the gun being fired
 * @param result The HitResult of the shot
 */
void postFire(ItemStack stack, Player plr, GunItem gunItem, HitResult result) {}

/**
 * This script is executed before the gun is fired on the <code>Client</code> side.
 * It can be used to add custom behavior before the gun is fired or cancel firing by returning a false value.
 *
 * @param context The GunClientStateContext of the gun being fired
 * @param state The GunClientState of the gun being fired
 * @return true if the gun should be fired, false otherwise
 */
boolean preFire(GunClientState.GunClientStateContext context, GunClientState state) {return true}

/**
 * Used to add hover text to the gun when it is hovered over in the inventory.
 * Recommended use is <code>tooltip.add(Component)</code>
 * @param stack The ItemStack of the gun being hovered over
 * @param world The world the player is in
 * @param tooltip The list of tooltip components to add to
 * @param flag The tooltip flag
 */
void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag flag) {}
/ -Feature Methods- /

/ Damage Feature /
/** Overrides the damage modifier
 *
 * @param stack
 * @param feature
 * @return <code>float</code>
 */
float getDamageModifier(ItemStack stack, DamageFeature feature) { return 0.0f }
/** Adds a new damage modifier on top of the base <code>DamageFeature</code> modifier
 *
 * @param stack
 * @param feature
 * @return <code>float<code>
 */
float addDamageModifier(ItemStack stack, DamageFeature feature) { return 0.0f }

/ Accuracy Feature /
/** Overrides the accuracy modifier
 *
 * @param stack
 * @param feature
 * @return <code>float</code>
 */
float getAccuracyModifier(ItemStack stack, DamageFeature feature) { return 0.0f }
/** Adds a new accuracy modifier on top of the base <code>AccuracyFeature</code> modifier
 *
 * @param stack
 * @param feature
 * @return <code>float<code>
 */
float addAccuracyModifier(ItemStack stack, DamageFeature feature) { return 0.0f }

/ Aiming Feature /
/** Overrides the zoom value, you can modify the zoom value by returning a modified value of <code>feature.zoom</code>
 *
 * @param stack
 * @param feature
 * @return <code>float</code>
 */
float getZoom(ItemStack stack, AimingFeature feature) { return 0.0f }

/ FireMode Feature /
/** Overrides the rpm value, you can modify the value as well since you can get it from the FireModeInstance
 *
 * @param stack
 * @param fireMode
 * @return <code>int</code>
 */
int getRpm(ItemStack stack, FireModeInstance fireMode) { return 0 }
/** Overrides the damage value, you can modify the value as well since you can get it from the FireModeInstance
 *
 * @param stack
 * @param fireMode
 * @return <code>float</code>
 */
float getDamage(ItemStack stack, FireModeInstance fireMode) { return 0 }
/** Overrides the maxShootingDistance value, you can modify the value as well since you can get it from the FireModeInstance
 *
 * @param stack
 * @param fireMode
 * @return <code>int</code>
 */
int getMaxShootingDistance(ItemStack stack, FireModeInstance fireMode) { return 0 }
/** Overrides the pellet count and spread of those pellets, you can modify the value as well since you can get it from the FireModeInstance.
 *  |
 * Useful for creating a shotgun with random spread, or spread based on player velocity, etc. | Use Pair.of() to create the values
 * @param stack
 * @param fireMode
 * @param plr
 * @param state
 * @return <code>int</code>
 */
Pair<Integer, Double> getPelletCountAndSpread(ItemStack stack, FireModeInstance fireMode, Player plr, GunClientState state) { return Pair.of(0,0D) }
// FireMode Animations
/** Overrides the fire animation, return a String like <code>animation.model.fire</code> to use a custom animation
 *
 * @param stack
 * @param fireMode
 * @param descriptor Information about the original animation, duration, name, etc
 * @return <code>String</code>
 */
String getFireAnimation(ItemStack stack, FireModeInstance fireMode, AnimationProvider.Descriptor descriptor) { return "" }
/** Overrides the prepare fire animation, return a String like <code>animation.model.preparefire</code> to use a custom animation
 *
 * @param stack
 * @param fireMode
 * @param descriptor
 * @return <code>String</code>
 */
String getPrepareFireAnimation(ItemStack stack, FireModeInstance fireMode, AnimationProvider.Descriptor descriptor) { return "" }
/** Overrides the complete fire animation, return a String like <code>animation.model.completefire</code> to use a custom animation
 *
 * @param stack
 * @param fireMode
 * @param descriptor Information about the original animation, duration, name, etc
 * @return <code>String</code>
 */
String getCompleteFireAnimation(ItemStack stack, FireModeInstance fireMode, AnimationProvider.Descriptor descriptor) { return "" }
/** Overrides the enable firemode animation, return a String like <code>animation.model.enablefire</code> to use a custom animation,
 * this is used when the firemode is changed like from semi to auto
 *
 * @param stack
 * @param fireMode
 * @param descriptor Information about the original animation, duration, name, etc
 * @return <code>String</code>
 */
String getEnableFireModeAnimation(ItemStack stack, FireModeInstance fireMode, AnimationProvider.Descriptor descriptor) { return "" }
// Animation Cooldowns
/** Overrides the prepare fire duration, you can modify the value as well since you can get it from the <code>descriptor</code>
 *
 * @param stack
 * @param fireMode
 * @param descriptor
 * @return <code>long</code>
 */
long getPrepareFireCooldown(ItemStack stack, FireModeInstance fireMode, AnimationProvider.Descriptor descriptor) { return 0L }
/** Overrides the complete fire duration, you can modify the value as well since you can get it from the <code>descriptor</code>
 *
 * @param stack
 * @param fireMode
 * @param descriptor
 * @return <code>long</code>
 */
long getCompleteFireCooldown(ItemStack stack, FireModeInstance fireMode, AnimationProvider.Descriptor descriptor) { return 0L }
/** Overrides the enable firemode duration, you can modify the value as well since you can get it from the <code>descriptor</code>
 *
 * @param stack
 * @param fireMode
 * @param descriptor
 * @return <code>long</code>
 */
long getEnableFireModeCooldown(ItemStack stack, FireModeInstance fireMode, AnimationProvider.Descriptor descriptor) { return 0L }

/ Ammo Capacity /
/** Overrides the ammo capacity
 *
 * @param original Original ammo capacity
 * @param stack
 * @param feature
 * @return <code>int</code>
 */
int modifyAmmoCapacity(int original, ItemStack stack, AmmoCapacityFeature feature) { return 0 }