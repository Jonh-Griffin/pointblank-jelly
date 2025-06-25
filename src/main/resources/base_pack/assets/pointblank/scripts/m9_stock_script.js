function getFireAnimation(stack, fireMode, descriptor) {
    if (Conditions.hasAttachment("m9_stock").test(new ConditionContext(stack))) {
        return !GunItem.isAiming(stack) ? "animation.model.fire" : "animation.model.firestock"
    }
    return "animation.model.fire"
}