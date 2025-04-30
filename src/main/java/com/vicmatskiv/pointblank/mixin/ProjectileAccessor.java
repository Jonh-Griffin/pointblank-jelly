package com.vicmatskiv.pointblank.mixin;

import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = Projectile.class, priority = 1100)
public interface ProjectileAccessor {
    @Mutable
    @Accessor("leftOwner")
    void setOwner(boolean var1);

    @Mutable
    @Accessor("leftOwner")
    boolean getOwner();

    @Mutable
    @Accessor("hasBeenShot")
    void setHasBeenShot(boolean var1);

    @Mutable
    @Accessor("hasBeenShot")
    boolean hasBeenShot();
}
