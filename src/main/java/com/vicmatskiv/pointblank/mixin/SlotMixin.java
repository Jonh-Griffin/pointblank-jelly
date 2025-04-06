package com.vicmatskiv.pointblank.mixin;

import com.vicmatskiv.pointblank.item.GunItem;
import com.vicmatskiv.pointblank.item.ThrowableItem;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Slot.class})
public class SlotMixin {
   @Shadow
   @Final
   private int f_40217_;

   @Inject(
      method = {"mayPlace"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onMayPlace(ItemStack itemStack, CallbackInfoReturnable<Boolean> cir) {
      if (this.f_40217_ == 40 && (itemStack.m_41720_() instanceof GunItem || itemStack.m_41720_() instanceof ThrowableItem)) {
         cir.setReturnValue(false);
         cir.cancel();
      }

   }
}
