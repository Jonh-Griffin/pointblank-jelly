package com.vicmatskiv.pointblank.mixin;

import com.vicmatskiv.pointblank.util.Metadata;
import groovy.lang.Script;
import org.jetbrains.annotations.ApiStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** May be used in the future to apply more default methods to scripts, currently useless.
 *  Add methods to the Metadata interface to add more default methods to scripts.
 */

@ApiStatus.Internal
@Mixin(value = {Script.class})
public class ScriptAttacher implements Metadata {

    @Unique
    public boolean clientOnly;
    @Override
    public void clientOnly() {
        this.clientOnly = true;
    }

    @Override
    public boolean getClientStatus() {
        return this.clientOnly;
    }
}
