// PointBlank API Documentation - JavaScript Version
// All imports would be handled by the mod's JavaScript engine

/* If you get a crash related to "Invalid Dist", you need to move all the client related methods in that script to a separate script inside of a client folder */

/* GunItem \ Root Methods */

/**
 * This script is executed after the gun is fired on the Server.
 * It can be used to add custom behavior when the gun is fired.
 *
 * @param {ItemStack} stack - The ItemStack of the gun being fired
 * @param {Player} plr - The player firing the gun
 * @param {GunItem} gunItem - The GunItem instance of the gun being fired
 * @param {HitResult} result - The HitResult of the shot
 */
function postFire(stack, plr, gunItem, result) {}

/**
 * This script is executed before the gun is fired on the Client side.
 * It can be used to add custom behavior before the gun is fired or cancel firing by returning a false value.
 *
 * @param {GunClientStateContext} context - The GunClientStateContext of the gun being fired
 * @param {GunClientState} state - The GunClientState of the gun being fired
 * @returns {boolean} true if the gun should be fired, false otherwise
 */
function preFire(context, state) {
    return true;
}

/**
 * Used to add hover text to the gun when it is hovered over in the inventory.
 * Recommended use is tooltip.push(Component)
 * @param {ItemStack} stack - The ItemStack of the gun being hovered over
 * @param {Level} world - The world the player is in
 * @param {Array<Component>} tooltip - The list of tooltip components to add to
 * @param {TooltipFlag} flag - The tooltip flag
 */
function appendHoverText(stack, world, tooltip, flag) {}

/**
 * Runs on the Client every tick
 * @param {Player} plr
 * @param {ItemStack} itemStack
 * @param {boolean} isSelected
 * @param {GunClientState} state
 */
function onClientTick(plr, itemStack, isSelected, state) {}

/**
 * Runs on Both sides every tick, use if (level.isClientSide) to choose which side to run on
 * also runs if not selected so use if (isSelected) in most cases
 * @param {Player} plr
 * @param {ItemStack} itemStack
 * @param {Level} level
 * @param {boolean} isSelected
 */
function onInventoryTick(itemStack, level, plr, isSelected) {}

/**
 * Runs when the gun is "swung" with an entity in the way, return false to enable melee functionality
 * @param {ItemStack} itemStack
 * @param {Player} plr - Player who used the item
 * @param {Entity} swungAt - Entity swung at
 * @returns {boolean}
 */
function onLeftClickEntity(itemStack, plr, swungAt) {
    return true;
}

/**
 * Runs when the gun is "swung", return false to enable melee functionality
 * @param {ItemStack} itemStack
 * @param {LivingEntity} heldEntity - Entity holding the item, usually can be cast to Player
 * @returns {boolean}
 */
function onEntitySwing(itemStack, heldEntity) {
    return true;
}

/**
 * Runs when the gun right clicks a living entity, could be used for some melee weapon functions
 * @param {ItemStack} itemStack
 * @param {Player} player
 * @param {LivingEntity} entity
 * @param {InteractionHand} hand
 */
function interactLivingEntity(itemStack, player, entity, hand) {}

/**
 * Overrides the PhasedReload(s) added when the gun is built in code.
 * @param {GunItemBuilder} builder - Contains lots of information on what the gun is made with
 * @returns {Array<PhasedReload>}
 */
function overrideReloads(builder) {
    // Example usage
    return [
            new GunItem.PhasedReload(
                    GunItem.ReloadPhase.RELOADING,
                    1000,
                    "animation.model.name",
                    Conditions.hasAmmoCount(0).and(ctx => ctx.player().health > 10)
            )
    ];
}

/* -Feature Methods- */

/* Damage Feature */

/**
 * Overrides the damage modifier
 * @param {ItemStack} stack
 * @param {DamageFeature} feature
 * @returns {number}
 */
function getDamageModifier(stack, feature) {
    return 0.0;
}

/**
 * Adds a new damage modifier on top of the base DamageFeature modifier
 * @param {ItemStack} stack
 * @param {DamageFeature} feature
 * @returns {number}
 */
function addDamageModifier(stack, feature) {
    return 0.0;
}

/* Accuracy Feature */

/**
 * Overrides the accuracy modifier
 * @param {ItemStack} stack
 * @param {AccuracyFeature} feature
 * @returns {number}
 */
function getAccuracyModifier(stack, feature) {
    return 0.0;
}

/**
 * Adds a new accuracy modifier on top of the base AccuracyFeature modifier
 * @param {ItemStack} stack
 * @param {AccuracyFeature} feature
 * @returns {number}
 */
function addAccuracyModifier(stack, feature) {
    return 0.0;
}

/* Aiming Feature */

/**
 * Overrides the zoom value, you can modify the zoom value by returning a modified value of feature.zoom
 * @param {ItemStack} stack
 * @param {AimingFeature} feature
 * @returns {number}
 */
function getZoom(stack, feature) {
    return 0.0;
}

/* FireMode Feature */

/**
 * Overrides the rpm value, you can modify the value as well since you can get it from the FireModeInstance
 * @param {ItemStack} stack
 * @param {FireModeInstance} fireMode
 * @returns {number}
 */
function getRpm(stack, fireMode) {
    return 0;
}

/**
 * Overrides the damage value, you can modify the value as well since you can get it from the FireModeInstance
 * @param {ItemStack} stack
 * @param {FireModeInstance} fireMode
 * @returns {number}
 */
function getDamage(stack, fireMode) {
    return 0;
}

/**
 * Overrides the maxShootingDistance value, you can modify the value as well since you can get it from the FireModeInstance
 * @param {ItemStack} stack
 * @param {FireModeInstance} fireMode
 * @returns {number}
 */
function getMaxShootingDistance(stack, fireMode) {
    return 0;
}

/**
 * Overrides the compatible ammo for this FireModeInstance
 * @param {AmmoItem} ammoItem
 * @param {ItemStack} gunItem
 * @param {FireModeInstance} fireMode
 * @returns {boolean}
 */
function isCompatibleBullet(ammoItem, gunItem, fireMode) {
    return false;
}

/**
 * Overrides the pellet count and spread of those pellets, you can modify the value as well since you can get it from the FireModeInstance.
 * Useful for creating a shotgun with random spread, or spread based on player velocity, etc.
 * @param {ItemStack} stack
 * @param {FireModeInstance} fireMode
 * @param {Player} plr
 * @param {GunClientState} state
 * @returns {Object} Object with count and spread properties: {count: number, spread: number}
 */
function getPelletCountAndSpread(stack, fireMode, plr, state) {
    return { count: 0, spread: 0.0 };
}

// FireMode Animations

/**
 * Overrides the fire animation, return a String like "animation.model.fire" to use a custom animation
 * @param {ItemStack} stack
 * @param {FireModeInstance} fireMode
 * @param {AnimationDescriptor} descriptor - Information about the original animation, duration, name, etc
 * @returns {string}
 */
function getFireAnimation(stack, fireMode, descriptor) {
    return "";
}

/**
 * Overrides the prepare fire animation, return a String like "animation.model.preparefire" to use a custom animation
 * @param {ItemStack} stack
 * @param {FireModeInstance} fireMode
 * @param {AnimationDescriptor} descriptor
 * @returns {string}
 */
function getPrepareFireAnimation(stack, fireMode, descriptor) {
    return "";
}

/**
 * Overrides the complete fire animation, return a String like "animation.model.completefire" to use a custom animation
 * @param {ItemStack} stack
 * @param {FireModeInstance} fireMode
 * @param {AnimationDescriptor} descriptor - Information about the original animation, duration, name, etc
 * @returns {string}
 */
function getCompleteFireAnimation(stack, fireMode, descriptor) {
    return "";
}

/**
 * Overrides the enable firemode animation, return a String like "animation.model.enablefire" to use a custom animation,
 * this is used when the firemode is changed like from semi to auto
 * @param {ItemStack} stack
 * @param {FireModeInstance} fireMode
 * @param {AnimationDescriptor} descriptor - Information about the original animation, duration, name, etc
 * @returns {string}
 */
function getEnableFireModeAnimation(stack, fireMode, descriptor) {
    return "";
}

// Animation Cooldowns

/**
 * Overrides the prepare fire duration, you can modify the value as well since you can get it from the descriptor
 * @param {ItemStack} stack
 * @param {FireModeInstance} fireMode
 * @param {AnimationDescriptor} descriptor
 * @returns {number}
 */
function getPrepareFireCooldown(stack, fireMode, descriptor) {
    return 0;
}

/**
 * Overrides the complete fire duration, you can modify the value as well since you can get it from the descriptor
 * @param {ItemStack} stack
 * @param {FireModeInstance} fireMode
 * @param {AnimationDescriptor} descriptor
 * @returns {number}
 */
function getCompleteFireCooldown(stack, fireMode, descriptor) {
    return 0;
}

/**
 * Overrides the enable firemode duration, you can modify the value as well since you can get it from the descriptor
 * @param {ItemStack} stack
 * @param {FireModeInstance} fireMode
 * @param {AnimationDescriptor} descriptor
 * @returns {number}
 */
function getEnableFireModeCooldown(stack, fireMode, descriptor) {
    return 0;
}

/* Ammo Capacity */

/**
 * Overrides the ammo capacity
 * @param {number} original - Original ammo capacity
 * @param {ItemStack} stack
 * @param {AmmoCapacityFeature} feature
 * @returns {number}
 */
function modifyAmmoCapacity(original, stack, feature) {
    return 0;
}

/* PiP Feature */

/**
 * Returns a resource location of the PiP stencil mask, used to change the shape of a PiP scope
 * @param {ItemStack} stack
 * @param {PipFeature} feature
 * @returns {string} Resource location string
 */
function getMaskTexture(stack, feature) {
    return "";
}

/**
 * Returns a float for the PiP zoom level, could be used to make multi-zoom scopes like LPVOs
 * @param {ItemStack} stack
 * @param {PipFeature} feature
 * @returns {number}
 */
function getPipZoom(stack, feature) {
    return 0.0;
}

/* Recoil Feature */

/**
 * Overrides the recoil modifier
 * @param {ItemStack} stack
 * @param {RecoilFeature} feature
 * @returns {number}
 */
function getRecoilModifier(stack, feature) {
    return 0.0;
}

/**
 * Adds a new recoil modifier on top of the base RecoilFeature modifier
 * @param {ItemStack} stack
 * @param {RecoilFeature} feature
 * @returns {number}
 */
function addRecoilModifier(stack, feature) {
    return 0.0;
}

/* Reticle Feature aka Red Dot sights and Holographics */

/**
 * Overrides the reticle texture
 * @param {ItemStack} stack
 * @param {ReticleFeature} feature
 * @returns {string} Resource location string
 */
function getReticleTexture(stack, feature) {
    return "";
}

/**
 * Advanced function, should only be used if you are familiar with Minecraft's rendering, 
 * runs right before the reticle's rendering
 * @param {ReticleFeature} feature
 * @param {BakedGeoModel} attachmentModel
 * @param {PoseStack} poseStack - Not popped or pushed, you will need to do that yourself if its necessary
 * @param {MultiBufferSource} source
 * @param {GunItem} animatable
 * @param {RenderType} renderType
 * @param {VertexConsumer} consumer
 * @param {number} pDelta - Also known as the partial tick
 * @param {number} packedLight
 * @param {number} reticleBrightness - 0-1.0 float
 */
function renderReticleBefore(feature, attachmentModel, poseStack, source, animatable, renderType, consumer, pDelta, packedLight, reticleBrightness) {}

/**
 * Advanced function, should only be used if you are familiar with Minecraft's rendering, 
 * runs right after the reticle's rendering
 * @param {ReticleFeature} feature
 * @param {BakedGeoModel} attachmentModel
 * @param {PoseStack} poseStack - Not popped or pushed, you will need to do that yourself if its necessary
 * @param {MultiBufferSource} source
 * @param {GunItem} animatable
 * @param {RenderType} renderType
 * @param {VertexConsumer} consumer
 * @param {number} pDelta - Also known as the partial tick
 * @param {number} packedLight
 * @param {number} reticleBrightness - 0-1.0 float
 */
function renderReticleAfter(feature, attachmentModel, poseStack, source, animatable, renderType, consumer, pDelta, packedLight, reticleBrightness) {}

/* Skin Feature */

/**
 * Returns the skin's texture, can be used to apply the same skin to multiple guns
 * @param {ItemStack} stack
 * @param {SkinFeature} feature
 * @returns {string} Resource location string
 */
function getSkinTexture(stack, feature) {
    return "";
}

/* Sound Feature */

/**
 * Returns the sound id and the volume of the sound
 * @param {ItemStack} stack
 * @param {SoundFeature} feature
 * @returns {Object} Object with sound and volume properties: {sound: string, volume: number}
 */
function getSoundAndVolume(stack, feature) {
    return { sound: "", volume: 1.0 };
}

/* Armor Item */

/**
 * Runs every inventory tick, regardless if you are wearing the armor or not
 * @param {ItemStack} stack
 * @param {Level} pLevel
 * @param {LivingEntity} pEntity
 */
function inventoryTick(stack, pLevel, pEntity) {}

/**
 * Runs every tick if you are wearing the armor
 * @param {ItemStack} stack
 * @param {Level} pLevel
 * @param {LivingEntity} pEntity
 */
function armorTick(stack, pLevel, pEntity) {}

/**
 * Runs when added to the armor slot, use this to add custom armor effects
 * @param {ItemStack} stack
 */
function equipArmor(stack) {}

/**
 * Runs when removed from the armor slot, use this to remove custom armor effects
 * @param {ItemStack} stack
 */
function unequipArmor(stack) {}

/**
 * Adds the amount of armor to the item
 * @param {ItemStack} stack
 * @returns {number}
 */
function addArmorDefense(stack) {
    return 0;
}

/**
 * Multiplies the amount of armor to the item, called before script adding, rounded because defense is always an integer
 * @param {ItemStack} stack
 * @returns {number}
 */
function mulArmorDefense(stack) {
    return 1.0;
}

/**
 * Adds the amount of toughness to the item
 * @param {ItemStack} stack
 * @returns {number}
 */
function addArmorToughness(stack) {
    return 0.0;
}

/**
 * Multiplies the amount of toughness to the item, called before script adding
 * @param {ItemStack} stack
 * @returns {number}
 */
function mulArmorToughness(stack) {
    return 1.0;
}

/* Defense Feature */

function addDefenseModifier(stack) {
    return 0;
}

function getDefenseModifier(stack) {
    return 1.0;
}

function addToughnessModifier(stack) {
    return 0.0;
}

function getToughnessModifier(stack) {
    return 1.0;
}

function addDefense(stack) {
    return 0;
}

function getDefense(stack) {
    return 0;
}

function addToughness(stack) {
    return 0.0;
}

function getToughness(stack) {
    return 0.0;
}

/* Attachment Script */

/**
 * Runs every inventory tick, regardless if you are wearing the armor or not
 * @param {ItemStack} stack
 * @param {Level} pLevel
 * @param {LivingEntity} pEntity
 */
function inventoryTick$A(stack, pLevel, pEntity) {}

/**
 * Runs every tick if you are wearing the armor
 * @param {ItemStack} stack
 * @param {Level} pLevel
 * @param {LivingEntity} pEntity
 */
function armorTick$A(stack, pLevel, pEntity) {}

/* Tips and tricks */

/**
 * Example conditions usage
 * Conditions are applicable anywhere where a predicate function is asked as a parameter
 */
function exampleConditions() {
    // Simple predicate using Conditions
    const examplePredicate = Conditions.hasAttachment("attachment");

    // Predicates can also have multiple conditions using logical operators
    const examplePredicateOr = ctx => examplePredicate(ctx) || ctx.player().health > 10;
    const examplePredicateAnd = ctx => examplePredicate(ctx) && ctx.player().health > 10;

    // Example simple predicate, this predicate applies if the player has more than 0 ammo and has a specified attachment
    const examplePredicateAmmo = ctx => Conditions.hasAmmoCount(0)(ctx) && Conditions.hasAttachment("attachment")(ctx);

    // Predicates can also be made very flexible by simply returning a boolean
    // Example usage of a predicate that would only apply when the player is aiming and has no ammo
    const examplePredicateAim = ctx => {
        return GunItem.isAiming(ctx.currentItemStack()) &&
                GunItem.getAmmo(ctx.currentItemStack(), GunItem.getFireModeInstance(ctx.currentItemStack())) === 0;
    };

    // Advanced predicates can also use Conditions.something() to apply predefined conditions
    const examplePredicateAdvanced = ctx => {
        return GunItem.isAiming(ctx.currentItemStack()) && Conditions.onNonEmptyReload()(ctx);
    };
}