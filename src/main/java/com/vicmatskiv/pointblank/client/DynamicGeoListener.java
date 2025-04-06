package com.vicmatskiv.pointblank.client;

import com.vicmatskiv.pointblank.item.GunItem;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimationController;

public class DynamicGeoListener implements GunStateListener {
   public void onStartFiring(LivingEntity player, GunClientState state, ItemStack itemStack) {
      GunItem gunItem = (GunItem)itemStack.m_41720_();
      Map<String, AnimationController<GeoAnimatable>> controllers = gunItem.getGeoAnimationControllers(itemStack);
      Iterator var6 = controllers.values().iterator();

      while(var6.hasNext()) {
         AnimationController<GeoAnimatable> controller = (AnimationController)var6.next();
         if (controller instanceof GunStateListener) {
            GunStateListener gunStateListener = (GunStateListener)controller;
            gunStateListener.onStartFiring(player, state, itemStack);
         }
      }

   }

   public void onStartReloading(LivingEntity player, GunClientState state, ItemStack itemStack) {
      GunItem gunItem = (GunItem)itemStack.m_41720_();
      Map<String, AnimationController<GeoAnimatable>> controllers = gunItem.getGeoAnimationControllers(itemStack);
      Iterator var6 = controllers.values().iterator();

      while(var6.hasNext()) {
         AnimationController<GeoAnimatable> controller = (AnimationController)var6.next();
         if (controller instanceof GunStateListener) {
            GunStateListener gunStateListener = (GunStateListener)controller;
            gunStateListener.onStartReloading(player, state, itemStack);
         }
      }

   }

   public void onPrepareReloading(LivingEntity player, GunClientState state, ItemStack itemStack) {
      GunItem gunItem = (GunItem)itemStack.m_41720_();
      Map<String, AnimationController<GeoAnimatable>> controllers = gunItem.getGeoAnimationControllers(itemStack);
      Iterator var6 = controllers.values().iterator();

      while(var6.hasNext()) {
         AnimationController<GeoAnimatable> controller = (AnimationController)var6.next();
         if (controller instanceof GunStateListener) {
            GunStateListener gunStateListener = (GunStateListener)controller;
            gunStateListener.onPrepareReloading(player, state, itemStack);
         }
      }

   }

   public void onCompleteReloading(LivingEntity player, GunClientState state, ItemStack itemStack) {
      GunItem gunItem = (GunItem)itemStack.m_41720_();
      Map<String, AnimationController<GeoAnimatable>> controllers = gunItem.getGeoAnimationControllers(itemStack);
      Iterator var6 = controllers.values().iterator();

      while(var6.hasNext()) {
         AnimationController<GeoAnimatable> controller = (AnimationController)var6.next();
         if (controller instanceof GunStateListener) {
            GunStateListener gunStateListener = (GunStateListener)controller;
            gunStateListener.onCompleteReloading(player, state, itemStack);
         }
      }

   }

   public void onPrepareIdle(LivingEntity player, GunClientState state, ItemStack itemStack) {
      GunItem gunItem = (GunItem)itemStack.m_41720_();
      Map<String, AnimationController<GeoAnimatable>> controllers = gunItem.getGeoAnimationControllers(itemStack);
      Iterator var6 = controllers.values().iterator();

      while(var6.hasNext()) {
         AnimationController<GeoAnimatable> controller = (AnimationController)var6.next();
         if (controller instanceof GunStateListener) {
            GunStateListener gunStateListener = (GunStateListener)controller;
            gunStateListener.onPrepareIdle(player, state, itemStack);
         }
      }

   }

   public void onIdle(LivingEntity player, GunClientState state, ItemStack itemStack) {
      GunItem gunItem = (GunItem)itemStack.m_41720_();
      Map<String, AnimationController<GeoAnimatable>> controllers = gunItem.getGeoAnimationControllers(itemStack);
      Iterator var6 = controllers.values().iterator();

      while(var6.hasNext()) {
         AnimationController<GeoAnimatable> controller = (AnimationController)var6.next();
         if (controller instanceof GunStateListener) {
            GunStateListener gunStateListener = (GunStateListener)controller;
            gunStateListener.onIdle(player, state, itemStack);
         }
      }

   }

   public void onDrawing(LivingEntity player, GunClientState state, ItemStack itemStack) {
      GunItem gunItem = (GunItem)itemStack.m_41720_();
      Map<String, AnimationController<GeoAnimatable>> controllers = gunItem.getGeoAnimationControllers(itemStack);
      Iterator var6 = controllers.values().iterator();

      while(var6.hasNext()) {
         AnimationController<GeoAnimatable> controller = (AnimationController)var6.next();
         if (controller instanceof GunStateListener) {
            GunStateListener gunStateListener = (GunStateListener)controller;
            gunStateListener.onDrawing(player, state, itemStack);
         }
      }

   }

   public void onInspecting(LivingEntity player, GunClientState state, ItemStack itemStack) {
      GunItem gunItem = (GunItem)itemStack.m_41720_();
      Map<String, AnimationController<GeoAnimatable>> controllers = gunItem.getGeoAnimationControllers(itemStack);
      Iterator var6 = controllers.values().iterator();

      while(var6.hasNext()) {
         AnimationController<GeoAnimatable> controller = (AnimationController)var6.next();
         if (controller instanceof GunStateListener) {
            GunStateListener gunStateListener = (GunStateListener)controller;
            gunStateListener.onInspecting(player, state, itemStack);
         }
      }

   }

   public void onPrepareFiring(LivingEntity player, GunClientState state, ItemStack itemStack) {
      GunItem gunItem = (GunItem)itemStack.m_41720_();
      Map<String, AnimationController<GeoAnimatable>> controllers = gunItem.getGeoAnimationControllers(itemStack);
      Iterator var6 = controllers.values().iterator();

      while(var6.hasNext()) {
         AnimationController<GeoAnimatable> controller = (AnimationController)var6.next();
         if (controller instanceof GunStateListener) {
            GunStateListener gunStateListener = (GunStateListener)controller;
            gunStateListener.onPrepareFiring(player, state, itemStack);
         }
      }

   }

   public void onCompleteFiring(LivingEntity player, GunClientState state, ItemStack itemStack) {
      GunItem gunItem = (GunItem)itemStack.m_41720_();
      Map<String, AnimationController<GeoAnimatable>> controllers = gunItem.getGeoAnimationControllers(itemStack);
      Iterator var6 = controllers.values().iterator();

      while(var6.hasNext()) {
         AnimationController<GeoAnimatable> controller = (AnimationController)var6.next();
         if (controller instanceof GunStateListener) {
            GunStateListener gunStateListener = (GunStateListener)controller;
            gunStateListener.onCompleteFiring(player, state, itemStack);
         }
      }

   }

   public void onEnablingFireMode(LivingEntity player, GunClientState state, ItemStack itemStack) {
      GunItem gunItem = (GunItem)itemStack.m_41720_();
      Map<String, AnimationController<GeoAnimatable>> controllers = gunItem.getGeoAnimationControllers(itemStack);
      Iterator var6 = controllers.values().iterator();

      while(var6.hasNext()) {
         AnimationController<GeoAnimatable> controller = (AnimationController)var6.next();
         if (controller instanceof GunStateListener) {
            GunStateListener gunStateListener = (GunStateListener)controller;
            gunStateListener.onEnablingFireMode(player, state, itemStack);
         }
      }

   }
}
