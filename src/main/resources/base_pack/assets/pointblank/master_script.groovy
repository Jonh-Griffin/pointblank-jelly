package base_pack.assets.pointblank

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.datafixers.util.Pair
import com.vicmatskiv.pointblank.client.GunClientState
import com.vicmatskiv.pointblank.feature.*
import com.vicmatskiv.pointblank.item.AmmoItem
import com.vicmatskiv.pointblank.item.AnimationProvider
import com.vicmatskiv.pointblank.item.FireModeInstance
import com.vicmatskiv.pointblank.item.GunItem
import com.vicmatskiv.pointblank.util.Conditions
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level
import net.minecraft.world.phys.HitResult
import software.bernie.geckolib.cache.object.BakedGeoModel

import java.util.function.Predicate

/ ^^ Above are all the imports that would be needed for these methods! Useful if you aren't using Intellij /
/ If you get a crash related to "Invalid Dist", you need to move all the client related methods in that script to a seperate script inside of a client folder /

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
void appendHoverText(ItemStack stack, Level world, List<Component> tooltip, TooltipFlag flag) {}
/** Runs on the <code>Client</code> every tick
 * @param plr
 * @param itemStack
 * @param isSelected
 * @param state
 */
void onClientTick(Player plr, ItemStack itemStack, boolean isSelected, GunClientState state) {}
/** Runs on <code>Both</code> sides every tick, use <code>if (level.isClientSide)</code> to choose which side to run on
 * also runs if not selected so use <code>if (isSelected)</code> in most cases
 * @param plr
 * @param itemStack
 * @param level
 * @param isSelected
 */
void onInventoryTick(ItemStack itemStack, Level level, Player plr, boolean isSelected) {}
/** Runs when the gun is "swung" with an entity in the way, return false to enable melee functionality
 * @param itemStack
 * @param plr Player who used the item
 * @param swungAt Entity swung at
 */
boolean onLeftClickEntity(ItemStack itemStack, Player plr, Entity swungAt) { return true }
/** Runs when the gun is "swung", return false to enable melee functionality
 * @param itemStack
 * @param heldEntity Entity holding the item, usually can be cast to Player with <code>if(heldEntity instanceof Player plr)</code>
 */
boolean onEntitySwing(ItemStack itemStack, LivingEntity heldEntity) { return true }
/** Runs when the gun right clicks a living entity, could be used for some melee weapon functions
 *
 * @param itemStack
 * @param player
 * @param entity
 * @param hand
 */
void interactLivingEntity(ItemStack itemStack, Player player, LivingEntity entity, InteractionHand hand) {}
/** Overrides the PhasedReload(s) added when the gun is built in code.
 *
 * @param builder Contains lots of information on what the gun is made with, including the original phased reloads as well as stuff like damage and rpm
 * @return <code>List(PhasedReload)</code>
 */
List<GunItem.PhasedReload> overrideReloads(GunItem.Builder builder) {
    //Example usage
    return [
            new GunItem.PhasedReload(
            GunItem.ReloadPhase.RELOADING,
            1000,
            "animation.model.name",
                    Conditions.hasAmmoCount(0) & { it.player().health > 10 })
    ]
}

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
float getAccuracyModifier(ItemStack stack, AccuracyFeature feature) { return 0.0f }
/** Adds a new accuracy modifier on top of the base <code>AccuracyFeature</code> modifier
 *
 * @param stack
 * @param feature
 * @return <code>float<code>
 */
float addAccuracyModifier(ItemStack stack, AccuracyFeature feature) { return 0.0f }

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
/** Overrides the compatible ammo for this FireModeInstance
 *
 * @param ammoItem
 * @param gunItem
 * @param fireMode
 * @return <code>boolean</code>
 */
boolean isCompatibleBullet(AmmoItem ammoItem, ItemStack gunItem, FireModeInstance fireMode) { return false }
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

/ PiP Feature /
/** Returns a resource location of the PiP stencil mask, used to change the shape of a PiP scope
 * @param stack
 * @param feature
 * @return <code>ResourceLocation</code>
 */
ResourceLocation getMaskTexture(ItemStack stack, PipFeature feature) { return ResourceLocation.parse("") }
/** Returns a float for the PiP zoom level, could be used to make multi-zoom scopes like LPVOs
 * @param stack
 * @param feature
 * @return <code>float</code>
 */
float getPipZoom(ItemStack stack, PipFeature feature) { return 0f }

/ Recoil Feature /
/** Overrides the recoil modifier
 *
 * @param stack
 * @param feature
 * @return <code>float</code>
 */
float getRecoilModifier(ItemStack stack, RecoilFeature feature) { return 0.0f }
/** Adds a new recoil modifier on top of the base <code>RecoilFeature</code> modifier
 *
 * @param stack
 * @param feature
 * @return <code>float<code>
 */
float addRecoilModifier(ItemStack stack, RecoilFeature feature) { return 0.0f }

/ Reticle Feature aka Red Dot sights and Holographics /
/** Overrides the reticle texture
 *
 * @param stack
 * @param feature
 * @return <code>ResourceLocation</code>
 */
ResourceLocation getReticleTexture(ItemStack stack, ReticleFeature feature) { return ResourceLocation.parse("") }
/** Advanced function, should only be used if you are familiar with Minecraft's rendering, runs right before the reticle's rendering
 * @param feature
 * @param attachmentModel
 * @param poseStack Not popped or pushed, you will need to do that yourself if its necessary
 * @param source
 * @param animatable
 * @param renderType
 * @param consumer
 * @param pDelta Also known as the partial tick
 * @param packedLight
 * @param reticleBrightness 0-1.0 float
 */
void renderReticleBefore(ReticleFeature feature, BakedGeoModel attachmentModel, PoseStack poseStack, MultiBufferSource source, GunItem animatable, RenderType renderType, VertexConsumer consumer, float pDelta, int packedLight, float reticleBrightness) {}
/** Advanced function, should only be used if you are familiar with Minecraft's rendering, runs right after the reticle's rendering
 * @param feature
 * @param attachmentModel
 * @param poseStack Not popped or pushed, you will need to do that yourself if its necessary
 * @param source
 * @param animatable
 * @param renderType
 * @param consumer
 * @param pDelta Also known as the partial tick
 * @param packedLight
 * @param reticleBrightness 0-1.0 float
 */
void renderReticleAfter(ReticleFeature feature, BakedGeoModel attachmentModel, PoseStack poseStack, MultiBufferSource source, GunItem animatable, RenderType renderType, VertexConsumer consumer, float pDelta, int packedLight, float reticleBrightness) {}

/ Skin Feature /
/** Returns the skin's texture, can be used to apply the same skin to multiple guns
 * @param stack
 * @param feature
 * @return <code>ResourceLocation</code>
 */
ResourceLocation getSkinTexture(ItemStack stack, SkinFeature feature) { return ResourceLocation.parse("") }

/ Sound Feature /
/** Returns the sound id and the volume of the sound
 * @param stack
 * @param feature
 * @return <code>Pair(String, Float)</code>
 */
Pair<String, Float> getSoundAndVolume(ItemStack stack, SoundFeature feature) { return Pair.of("", 1f) }

/ Armor Item /
/** Runs every inventory tick, regardless if you are wearing the armor or not
 * @param stack
 * @param pLevel
 * @param pEntity
 */
void inventoryTick(ItemStack stack, Level pLevel, LivingEntity pEntity) {}
/** Runs every tick if you are wearing the armor
 * @param stack
 * @param pLevel
 * @param pEntity
 */
void armorTick(ItemStack stack, Level pLevel, LivingEntity pEntity) {}
/** Runs when added to the armor slot, use this to add custom armor effects
 * @param stack
 */
void equipArmor(ItemStack stack) { }
/** Runs when removed from the armor slot, use this to remove custom armor effects
 * @param stack
 */
void unequipArmor(ItemStack stack) { }
/** Adds the amount of armor to the item
 * @param stack
 * @return <code>int</code>
 */
int addArmorDefense(ItemStack stack) { return 0 }
/** Multiplies the amount of armor to the item, called before script adding, rounded because defense is always an integer
 * @param stack
 * @return <code>float</code>
 */
float mulArmorDefense(ItemStack stack) { return 1f }
/** Adds the amount of toughness to the item
 * @param stack
 * @return <code>float</code>
 */
float addArmorToughness(ItemStack stack) { return 0f }
/** Multiplies the amount of toughness to the item, called before script adding
 * @param stack
 * @return <code>float</code>
 */
float mulArmorToughness(ItemStack stack) { return 1f }

/ Defense Feature /

int addDefenseModifier(ItemStack stack) { return 0 }

float getDefenseModifier(ItemStack stack) { return 1f }

float addToughnessModifier(ItemStack stack) { return 0f }

float getToughnessModifier(ItemStack stack) { return 1f }

int addDefense(ItemStack stack) { return 0 }

int getDefense(ItemStack stack) { return 0 }

float addToughness(ItemStack stack) { return 0f }

float getToughness(ItemStack stack) { return 0f }

/ Attachment Script /
/** Runs every inventory tick, regardless if you are wearing the armor or not
 * @param stack
 * @param pLevel
 * @param pEntity
 */
void inventoryTick$A(ItemStack stack, Level pLevel, LivingEntity pEntity) {}
/** Runs every tick if you are wearing the armor
 * @param stack
 * @param pLevel
 * @param pEntity
 */
void armorTick$A(ItemStack stack, Level pLevel, LivingEntity pEntity) {}

/Tips and tricks/
/** Conditions ->
 *
 */
private void exampleConditions() {
    //Conditions are applicable anywhere where Predicate<ConditionContext> is asked as a parameter
    Predicate<ConditionContext> examplePredicate = Conditions.hasAttachment("attachment")
    //Predicates can also have multiple conditions using either ".and / &" or ".or / |" (.or is recommended as it supplies autocorrect)
    Predicate<ConditionContext> examplePredicateOr = examplePredicate.or { it.player().health > 10 }
    Predicate<ConditionContext> examplePredicateAnd = examplePredicate.and { it.player().health > 10 }

    /** Clarification, ".and {} \ .or {}" is only used if you are using the "it" method,
     *  and not "Conditions", if you use Conditions.something() you should use something like
     *  examplePredicate.and(Conditions.something()) with "()" not "{}"
     */

    //Example simple predicate, this predicate applies if the player has more than 0 ammo and has a specified attachment
    Predicate<ConditionContext> examplePredicateAmmo = Conditions.hasAmmoCount(0).and(Conditions.hasAttachment("attachment"))

    //Predicates can also be made very flexible by simply returning a boolean
    //Example usage of a predicate that would only apply when the player is aiming and has no ammo
    Predicate<ConditionContext> examplePredicateAim = new Predicate<ConditionContext>() {
        boolean test(ConditionContext ctx) {
            return GunItem.isAiming(ctx.currentItemStack()) && GunItem.getAmmo(ctx.currentItemStack(), GunItem.getFireModeInstance(ctx.currentItemStack())) == 0
        }
    }
    //Advanced predicates can also use Conditions.something() to apply predefined conditions, you just need to add .test(ctx) with ctx being the ConditionContext parameter
    Predicate<ConditionContext> examplePredicateAdvanced = new Predicate<ConditionContext>() {
        boolean test(ConditionContext ctx) {
            return GunItem.isAiming(ctx.currentItemStack()) && Conditions.onNonEmptyReload().test(ctx)
        }
    }
}

