package com.vicmatskiv.pointblank.mixin;

import com.vicmatskiv.pointblank.util.Metadata;
import groovy.lang.Script;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

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
