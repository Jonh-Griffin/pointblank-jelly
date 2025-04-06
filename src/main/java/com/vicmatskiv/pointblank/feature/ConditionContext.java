package com.vicmatskiv.pointblank.feature;

import com.vicmatskiv.pointblank.client.GunClientState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public record ConditionContext(LivingEntity player, ItemStack rootStack, ItemStack currentItemStack, GunClientState gunClientState, ItemDisplayContext itemDisplayContext, int randomSample1, int randomSample2) {
   public ConditionContext(LivingEntity player, ItemStack rootStack, ItemStack currentItemStack, GunClientState gunClientState, ItemDisplayContext itemDisplayContext) {
      this(player, rootStack, currentItemStack, gunClientState, itemDisplayContext, Integer.MAX_VALUE, 0);
   }

   public ConditionContext(LivingEntity player, ItemStack rootStack, GunClientState gunClientState, ItemDisplayContext itemDisplayContext) {
      this(player, rootStack, rootStack, gunClientState, itemDisplayContext, Integer.MAX_VALUE, 0);
   }

   public ConditionContext(LivingEntity player, GunClientState gunClientState) {
      this(player, (ItemStack)null, gunClientState, (ItemDisplayContext)null);
   }

   public ConditionContext(ItemStack itemStack) {
      this((LivingEntity)null, itemStack, itemStack, (GunClientState)null, (ItemDisplayContext)null);
   }

   public ConditionContext(LivingEntity player, ItemStack rootStack, ItemStack currentItemStack, GunClientState gunClientState, ItemDisplayContext itemDisplayContext, int randomSample1, int randomSample2) {
      this.player = player;
      this.rootStack = rootStack;
      this.currentItemStack = currentItemStack;
      this.gunClientState = gunClientState;
      this.itemDisplayContext = itemDisplayContext;
      this.randomSample1 = randomSample1;
      this.randomSample2 = randomSample2;
   }

   public LivingEntity player() {
      return this.player;
   }

   public ItemStack rootStack() {
      return this.rootStack;
   }

   public ItemStack currentItemStack() {
      return this.currentItemStack;
   }

   public GunClientState gunClientState() {
      return this.gunClientState;
   }

   public ItemDisplayContext itemDisplayContext() {
      return this.itemDisplayContext;
   }

   public int randomSample1() {
      return this.randomSample1;
   }

   public int randomSample2() {
      return this.randomSample2;
   }
}
