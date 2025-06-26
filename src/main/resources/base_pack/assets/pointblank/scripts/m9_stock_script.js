//Imports

//StandardLib println
println("M9 Stock Script Loaded")
//Different Ways to Import
jImport(["mod.pbj", "script.Scripts", "feature.AimingFeature"]) //requires "" and [] (array of strings), does not require Packages
//Requires Packages, does not require "" or []
importClass(Packages.mod.pbj.util.MiscUtil)
importPackage(Packages.mod.pbj.explosion) //Imports all classes in the package
// Be careful of duplicate imports, they can cause issues, classes with the same name will conflict

function getFireAnimation(stack, fireMode, descriptor) {
    if (Conditions.hasAttachment("m9_stock").test(new ConditionContext(stack))) {
        return !GunItem.isAiming(stack) ? "animation.model.fire" : "animation.model.firestock"
    }
    return "animation.model.fire"
}