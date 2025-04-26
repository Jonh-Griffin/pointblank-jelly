package base_pack.assets.pointblank.scripts


import com.vicmatskiv.pointblank.item.GunItem
import com.vicmatskiv.pointblank.util.Conditions

List<GunItem.PhasedReload> overrideReloads(GunItem.Builder builder) {
    return [
            new GunItem.PhasedReload(
                    GunItem.ReloadPhase.RELOADING,
                    1700,
                    "animation.model.reloadempty",
                    Conditions.onReloadIteration(0)
            ),
            new GunItem.PhasedReload(
                    GunItem.ReloadPhase.RELOADING,
                    1700,
                    "animation.model.reload",
                    Conditions.onNonEmptyReload()
            )
    ]
}