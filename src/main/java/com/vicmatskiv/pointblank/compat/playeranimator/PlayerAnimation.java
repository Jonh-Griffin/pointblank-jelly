package com.vicmatskiv.pointblank.compat.playeranimator;

public record PlayerAnimation<T>(String name, String ownerId, T keyframeAnimation, PlayerAnimationPartGroup group) {
   public PlayerAnimation(String name, String ownerId, T keyframeAnimation, PlayerAnimationPartGroup group) {
      this.name = name;
      this.ownerId = ownerId;
      this.keyframeAnimation = keyframeAnimation;
      this.group = group;
   }

   public String name() {
      return this.name;
   }

   public String ownerId() {
      return this.ownerId;
   }

   public T keyframeAnimation() {
      return this.keyframeAnimation;
   }

   public PlayerAnimationPartGroup group() {
      return this.group;
   }
}
