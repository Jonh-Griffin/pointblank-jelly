package com.vicmatskiv.pointblank.mixin;

import com.vicmatskiv.pointblank.Config;
import com.vicmatskiv.pointblank.item.GunItem;
import com.vicmatskiv.pointblank.util.MiscUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({LivingEntity.class})
public class LivingEntityMixin {
   @Unique
   private final ThreadLocal<DamageSource> hurtDamageSource = new ThreadLocal();

   @Inject(
      method = {"hurt"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDD)V"
)}
   )
   private void beforeHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> callbackInfo) {
      this.hurtDamageSource.set(source);
   }

   @Inject(
      method = {"hurt"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDD)V",
   shift = Shift.AFTER
)}
   )
   private void afterHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> callbackInfo) {
      this.hurtDamageSource.remove();
   }

   @ModifyArg(
      method = {"hurt"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDD)V"
),
      index = 0
   )
   private double onKnockback(double knockback) {
      DamageSource source = (DamageSource)this.hurtDamageSource.get();
      if (source != null) {
         Entity var5 = source.m_7639_();
         if (var5 instanceof LivingEntity) {
            LivingEntity hurtByEntity = (LivingEntity)var5;
            GunItem gunItem = (GunItem)MiscUtil.getMainHeldGun(hurtByEntity).orElse((Object)null);
            if (gunItem != null) {
               knockback = Mth.m_14008_(knockback * Config.knockback, 0.0D, 100.0D);
            }
         }
      }

      return knockback;
   }
}
