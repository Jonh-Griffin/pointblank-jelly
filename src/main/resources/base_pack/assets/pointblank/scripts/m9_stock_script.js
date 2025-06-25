//Imports
importClass(com.vicmatskiv.pointblank.feature.ConditionContext)
importClass(com.vicmatskiv.pointblank.item.GunItem)
importClass(com.vicmatskiv.pointblank.util.Conditions)
//var {Integer, String:JString} = com.vicmatskiv.pointblank
function getFireAnimation(stack, fireMode, descriptor) {
    if (Conditions.hasAttachment("m9_stock").test(new ConditionContext(stack))) {
        return !GunItem.isAiming(stack) ? "animation.model.fire" : "animation.model.firestock"
    }
    return "animation.model.fire"
}