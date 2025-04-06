package com.vicmatskiv.pointblank.client;

import com.vicmatskiv.pointblank.client.controller.BlendingAnimationController;
import com.vicmatskiv.pointblank.item.GunItem;
import com.vicmatskiv.pointblank.util.StateMachine;
import java.util.Collection;
import java.util.List;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animation.AnimationController.State;
import software.bernie.geckolib.core.animation.AnimationProcessor.QueuedAnimation;
import software.bernie.geckolib.util.ClientUtils;

public class FirstPersonWalkingAnimationHandler {
   public static final String WALKING_CONTROLLER_NAME = "walking";
   private static final float MIN_SPEED_SQUARED = 1.0E-4F;
   private final StateMachine<PlayerWalkingState, PlayerWalkingStateContext> firstPersonAnimationStateMachine = this.createFirstPersonAnimationStateMachine();
   private long ts;

   private StateMachine<PlayerWalkingState, PlayerWalkingStateContext> createFirstPersonAnimationStateMachine() {
      StateMachine.Builder<PlayerWalkingState, PlayerWalkingStateContext> builder = new StateMachine.Builder();
      Minecraft mc = Minecraft.m_91087_();
      builder.withTransition((Enum) PlayerWalkingState.NONE, PlayerWalkingState.STANDING, (ctx) -> {
         return mc.f_91066_.m_92176_() == CameraType.FIRST_PERSON && !ctx.state.isDrawing() && System.currentTimeMillis() - this.ts > 100L;
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, (StateMachine.Action)null);
      builder.withTransition((List)List.of(PlayerWalkingState.STANDING, PlayerWalkingState.WALKING, PlayerWalkingState.WALKING_BACKWARDS, PlayerWalkingState.WALKING_LEFT, PlayerWalkingState.WALKING_RIGHT, PlayerWalkingState.RUNNING, PlayerWalkingState.PREPARE_RUNNING, PlayerWalkingState.COMPLETE_RUNNING, PlayerWalkingState.OFF_GROUND, PlayerWalkingState.OFF_GROUND_SPRINTING), PlayerWalkingState.NONE, (ctx) -> {
         return mc.f_91066_.m_92176_() != CameraType.FIRST_PERSON;
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, (StateMachine.Action)null);
      builder.withTransition((List)List.of(PlayerWalkingState.STANDING, PlayerWalkingState.WALKING, PlayerWalkingState.WALKING_BACKWARDS, PlayerWalkingState.WALKING_LEFT, PlayerWalkingState.WALKING_RIGHT), PlayerWalkingState.OFF_GROUND, (ctx) -> {
         return !ctx.player.m_20096_() && !ctx.player.m_20142_();
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, (StateMachine.Action)null);
      builder.withTransition((Enum) PlayerWalkingState.COMPLETE_RUNNING, PlayerWalkingState.OFF_GROUND, (ctx) -> {
         return ctx.speedSquared > 1.0E-4F && !ctx.player.m_20096_() && (!ctx.player.m_20142_() || ctx.player.m_108635_()) && ctx.isMovingForward() && !ctx.state.isAiming() && !isPlayingAnimation(GunItem.RAW_ANIMATION_COMPLETE_RUNNING, ctx.itemStack);
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, (StateMachine.Action)null);
      builder.withTransition((List)List.of(PlayerWalkingState.RUNNING), PlayerWalkingState.OFF_GROUND_SPRINTING, (ctx) -> {
         return !ctx.player.m_20096_() && ctx.player.m_20142_();
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, (StateMachine.Action)null);
      builder.withTransition((Enum) PlayerWalkingState.PREPARE_RUNNING, PlayerWalkingState.OFF_GROUND_SPRINTING, (ctx) -> {
         return ctx.player.m_20142_() && !ctx.player.m_20096_() && !ctx.player.m_108635_() && !isPlayingAnimation(GunItem.RAW_ANIMATION_PREPARE_RUNNING, ctx.itemStack);
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, (StateMachine.Action)null);
      builder.withTransition((List)List.of(PlayerWalkingState.STANDING, PlayerWalkingState.WALKING, PlayerWalkingState.WALKING_BACKWARDS, PlayerWalkingState.WALKING_LEFT, PlayerWalkingState.WALKING_RIGHT), PlayerWalkingState.WALKING_AIMING, (ctx) -> {
         return ctx.speedSquared > 1.0E-4F && ctx.player.m_20096_() && !ctx.player.m_20142_() && ctx.state.isAiming();
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, (StateMachine.Action)null);
      builder.withTransition((Enum) PlayerWalkingState.COMPLETE_RUNNING, PlayerWalkingState.WALKING_AIMING, (ctx) -> {
         return ctx.speedSquared > 1.0E-4F && ctx.player.m_20096_() && !ctx.player.m_20142_() && ctx.state.isAiming() && !isPlayingAnimation(GunItem.RAW_ANIMATION_COMPLETE_RUNNING, ctx.itemStack);
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, (StateMachine.Action)null);
      builder.withTransition((List)List.of(PlayerWalkingState.OFF_GROUND, PlayerWalkingState.STANDING, PlayerWalkingState.WALKING_AIMING, PlayerWalkingState.WALKING_BACKWARDS, PlayerWalkingState.WALKING_LEFT, PlayerWalkingState.WALKING_RIGHT), PlayerWalkingState.WALKING, (ctx) -> {
         return ctx.speedSquared > 1.0E-4F && ctx.player.m_20096_() && !ctx.player.m_20142_() && ctx.isMovingForward() && !ctx.state.isAiming();
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, (StateMachine.Action)null);
      builder.withTransition((Enum) PlayerWalkingState.COMPLETE_RUNNING, PlayerWalkingState.WALKING, (ctx) -> {
         return ctx.speedSquared > 1.0E-4F && ctx.player.m_20096_() && (!ctx.player.m_20142_() || ctx.player.m_108635_()) && ctx.isMovingForward() && !ctx.state.isAiming() && !isPlayingAnimation(GunItem.RAW_ANIMATION_COMPLETE_RUNNING, ctx.itemStack);
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, (StateMachine.Action)null);
      builder.withTransition((List)List.of(PlayerWalkingState.OFF_GROUND, PlayerWalkingState.STANDING, PlayerWalkingState.WALKING, PlayerWalkingState.WALKING_AIMING, PlayerWalkingState.WALKING_LEFT, PlayerWalkingState.WALKING_RIGHT), PlayerWalkingState.WALKING_BACKWARDS, (ctx) -> {
         return ctx.speedSquared > 1.0E-4F && ctx.player.m_20096_() && (!ctx.player.m_20142_() || ctx.player.m_108635_()) && ctx.isMovingBackward() && !ctx.state.isAiming();
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, (StateMachine.Action)null);
      builder.withTransition((Enum) PlayerWalkingState.COMPLETE_RUNNING, PlayerWalkingState.WALKING_BACKWARDS, (ctx) -> {
         return ctx.speedSquared > 1.0E-4F && (!ctx.player.m_20142_() || ctx.player.m_108635_()) && ctx.isMovingBackward() && !ctx.state.isAiming() && !isPlayingAnimation(GunItem.RAW_ANIMATION_COMPLETE_RUNNING, ctx.itemStack);
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, (StateMachine.Action)null);
      builder.withTransition((List)List.of(PlayerWalkingState.OFF_GROUND, PlayerWalkingState.STANDING, PlayerWalkingState.WALKING, PlayerWalkingState.WALKING_AIMING, PlayerWalkingState.WALKING_BACKWARDS, PlayerWalkingState.WALKING_RIGHT), PlayerWalkingState.WALKING_LEFT, (ctx) -> {
         return ctx.speedSquared > 1.0E-4F && ctx.player.m_20096_() && (!ctx.player.m_20142_() || ctx.player.m_108635_()) && ctx.isMovingLeft() && !ctx.state.isAiming();
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, (StateMachine.Action)null);
      builder.withTransition((Enum) PlayerWalkingState.COMPLETE_RUNNING, PlayerWalkingState.WALKING_LEFT, (ctx) -> {
         return ctx.speedSquared > 1.0E-4F && ctx.player.m_20096_() && (!ctx.player.m_20142_() || ctx.player.m_108635_()) && ctx.isMovingLeft() && !ctx.state.isAiming() && !isPlayingAnimation(GunItem.RAW_ANIMATION_COMPLETE_RUNNING, ctx.itemStack);
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, (StateMachine.Action)null);
      builder.withTransition((List)List.of(PlayerWalkingState.OFF_GROUND, PlayerWalkingState.STANDING, PlayerWalkingState.WALKING, PlayerWalkingState.WALKING_AIMING, PlayerWalkingState.WALKING_BACKWARDS, PlayerWalkingState.WALKING_LEFT), PlayerWalkingState.WALKING_RIGHT, (ctx) -> {
         return ctx.speedSquared > 1.0E-4F && ctx.player.m_20096_() && !ctx.player.m_20142_() && !ctx.state.isAiming() && ctx.isMovingRight();
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, (StateMachine.Action)null);
      builder.withTransition((Enum) PlayerWalkingState.COMPLETE_RUNNING, PlayerWalkingState.WALKING_RIGHT, (ctx) -> {
         return ctx.speedSquared > 1.0E-4F && ctx.player.m_20096_() && (!ctx.player.m_20142_() || ctx.player.m_108635_()) && ctx.isMovingRight() && !ctx.state.isAiming() && !isPlayingAnimation(GunItem.RAW_ANIMATION_COMPLETE_RUNNING, ctx.itemStack);
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, (StateMachine.Action)null);
      builder.withTransition((List)List.of(PlayerWalkingState.OFF_GROUND, PlayerWalkingState.WALKING, PlayerWalkingState.WALKING_AIMING, PlayerWalkingState.WALKING_BACKWARDS, PlayerWalkingState.WALKING_LEFT, PlayerWalkingState.WALKING_RIGHT), PlayerWalkingState.STANDING, (ctx) -> {
         return ctx.player.m_20096_() && !ctx.player.m_20142_() && ctx.speedSquared <= 1.0E-4F;
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, (StateMachine.Action)null);
      builder.withTransition((Enum) PlayerWalkingState.COMPLETE_RUNNING, PlayerWalkingState.STANDING, (ctx) -> {
         return ctx.player.m_20096_() && !ctx.player.m_20142_() && ctx.speedSquared <= 1.0E-4F && !isPlayingAnimation(GunItem.RAW_ANIMATION_COMPLETE_RUNNING, ctx.itemStack);
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, (StateMachine.Action)null);
      builder.withTransition((List)List.of(PlayerWalkingState.OFF_GROUND, PlayerWalkingState.STANDING, PlayerWalkingState.WALKING, PlayerWalkingState.WALKING_AIMING, PlayerWalkingState.WALKING_BACKWARDS, PlayerWalkingState.WALKING_LEFT, PlayerWalkingState.WALKING_RIGHT, PlayerWalkingState.COMPLETE_RUNNING), PlayerWalkingState.PREPARE_RUNNING, (ctx) -> {
         return ctx.player.m_20142_() && !ctx.player.m_108635_() && !isPlayingAnimation("animation.model.fire", ctx.itemStack, "fire_controller");
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, (StateMachine.Action)null);
      builder.withTransition((List)List.of(PlayerWalkingState.OFF_GROUND_SPRINTING), PlayerWalkingState.RUNNING, (ctx) -> {
         return ctx.player.m_20096_();
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, (StateMachine.Action)null);
      builder.withTransition((Enum) PlayerWalkingState.PREPARE_RUNNING, PlayerWalkingState.RUNNING, (ctx) -> {
         return ctx.player.m_20142_() && ctx.player.m_20096_() && !ctx.player.m_108635_() && !isPlayingAnimation(GunItem.RAW_ANIMATION_PREPARE_RUNNING, ctx.itemStack);
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, (StateMachine.Action)null);
      builder.withTransition((List)List.of(PlayerWalkingState.PREPARE_RUNNING, PlayerWalkingState.RUNNING, PlayerWalkingState.OFF_GROUND_SPRINTING), PlayerWalkingState.COMPLETE_RUNNING, (ctx) -> {
         return !ctx.player.m_20142_() || ctx.player.m_108635_();
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, (StateMachine.Action)null);
      builder.withOnChangeStateAction((ctx, f, t) -> {
         long id = GeoItem.getId(ctx.itemStack);
         GunItem gunItem = (GunItem)ctx.itemStack.m_41720_();
         switch(t) {
         case STANDING:
            gunItem.triggerAnim(ctx.player, id, "walking", "animation.model.standing");
            break;
         case WALKING_AIMING:
            gunItem.triggerAnim(ctx.player, id, "walking", "animation.model.walking_aiming");
            break;
         case WALKING:
            gunItem.triggerAnim(ctx.player, id, "walking", "animation.model.walking");
            break;
         case WALKING_BACKWARDS:
            gunItem.triggerAnim(ctx.player, id, "walking", "animation.model.walking_backwards");
            break;
         case WALKING_LEFT:
            gunItem.triggerAnim(ctx.player, id, "walking", "animation.model.walking_left");
            break;
         case WALKING_RIGHT:
            gunItem.triggerAnim(ctx.player, id, "walking", "animation.model.walking_right");
            break;
         case PREPARE_RUNNING:
            gunItem.triggerAnim(ctx.player, id, "walking", "animation.model.runningstart");
            break;
         case RUNNING:
            gunItem.triggerAnim(ctx.player, id, "walking", "animation.model.running");
            break;
         case COMPLETE_RUNNING:
            gunItem.triggerAnim(ctx.player, id, "walking", "animation.model.runningend");
            break;
         case OFF_GROUND:
            gunItem.triggerAnim(ctx.player, id, "walking", "animation.model.off_ground");
            break;
         case OFF_GROUND_SPRINTING:
            gunItem.triggerAnim(ctx.player, id, "walking", "animation.model.off_ground_sprinting");
            break;
         case NONE:
            AnimationController<GeoAnimatable> controller = gunItem.getGeoAnimationController("walking", ctx.itemStack);
            controller.forceAnimationReset();
            controller.setAnimation((RawAnimation)null);
            ((BlendingAnimationController)controller).clearAll();
         }

      });
      return builder.build(PlayerWalkingState.STANDING);
   }

   public void handlePlayerFirstPersonMovement(Player player, ItemStack itemStack) {
      if (itemStack.m_41720_() instanceof GunItem) {
         Minecraft mc = Minecraft.m_91087_();
         Player mainPlayer = ClientUtils.getClientPlayer();
         if (player == mainPlayer) {
            PlayerWalkingStateContext context = new PlayerWalkingStateContext((LocalPlayer)player, itemStack);
            if (mc.f_91066_.m_92176_() == CameraType.FIRST_PERSON) {
               this.firstPersonAnimationStateMachine.update(context);
            } else {
               this.firstPersonAnimationStateMachine.update(context);
            }

         }
      }
   }

   public void reset(Player player, ItemStack itemStack) {
      if (itemStack.m_41720_() instanceof GunItem) {
         this.ts = System.currentTimeMillis();
         this.firstPersonAnimationStateMachine.resetToState(PlayerWalkingState.NONE);
      }
   }

   public static boolean isPlayingAnimation(RawAnimation rawAnimation, ItemStack itemStack) {
      Item var3 = itemStack.m_41720_();
      if (!(var3 instanceof GunItem)) {
         return false;
      } else {
         boolean var10000;
         label22: {
            GunItem gunItem = (GunItem)var3;
            AnimationController controller = gunItem.getGeoAnimationController("walking", itemStack);
            if (rawAnimation != controller.getCurrentRawAnimation()) {
               if (!(controller instanceof BlendingAnimationController)) {
                  break label22;
               }

               BlendingAnimationController<?> bac = (BlendingAnimationController)controller;
               if (bac.getTriggeredAnimation() != rawAnimation) {
                  break label22;
               }
            }

            if (controller.getAnimationState() != State.STOPPED) {
               var10000 = true;
               return var10000;
            }
         }

         var10000 = false;
         return var10000;
      }
   }

   public static boolean isPlayingBlendingAnimation(Collection<RawAnimation> rawAnimations, ItemStack itemStack, double minProgress, double maxProgress) {
      Item var7 = itemStack.m_41720_();
      if (var7 instanceof GunItem) {
         GunItem gunItem = (GunItem)var7;
         AnimationController controller = gunItem.getGeoAnimationController("walking", itemStack);
         if (!(controller instanceof BlendingAnimationController)) {
            return false;
         } else {
            BlendingAnimationController<GeoAnimatable> bac = (BlendingAnimationController)controller;
            boolean result = collectionContains(rawAnimations, bac.getTriggeredAnimation()) && bac.getAnimationState() != State.STOPPED;
            if (!result) {
               return false;
            } else {
               double progress = bac.getAnimationProgress();
               return minProgress <= progress && progress < maxProgress;
            }
         }
      } else {
         return false;
      }
   }

   private static <T> boolean collectionContains(Collection<T> items, T item) {
      return item != null && items.contains(item);
   }

   public static boolean isPlayingAnimation(String animationName, ItemStack itemStack, String controllerName) {
      Item var4 = itemStack.m_41720_();
      if (!(var4 instanceof GunItem)) {
         return false;
      } else {
         GunItem gunItem = (GunItem)var4;
         AnimationController controller = gunItem.getGeoAnimationController(controllerName, itemStack);
         QueuedAnimation currentAnimation = controller.getCurrentAnimation();
         return currentAnimation != null && animationName.equals(currentAnimation.animation().name()) && controller.getAnimationState() != State.STOPPED;
      }
   }

   private static enum PlayerWalkingState {
      NONE,
      OFF_GROUND,
      OFF_GROUND_SPRINTING,
      STANDING,
      WALKING,
      WALKING_BACKWARDS,
      WALKING_LEFT,
      WALKING_RIGHT,
      WALKING_AIMING,
      PREPARE_RUNNING,
      RUNNING,
      COMPLETE_RUNNING;

      // $FF: synthetic method
      private static PlayerWalkingState[] $values() {
         return new PlayerWalkingState[]{NONE, OFF_GROUND, OFF_GROUND_SPRINTING, STANDING, WALKING, WALKING_BACKWARDS, WALKING_LEFT, WALKING_RIGHT, WALKING_AIMING, PREPARE_RUNNING, RUNNING, COMPLETE_RUNNING};
      }
   }

   private static class PlayerWalkingStateContext {
      final LocalPlayer player;
      final ItemStack itemStack;
      final float speedSquared;
      final GunClientState state;

      public PlayerWalkingStateContext(LocalPlayer player, ItemStack itemStack) {
         this.player = player;
         this.itemStack = itemStack;
         this.speedSquared = player.f_108618_.m_108575_().m_165912_();
         this.state = GunClientState.getMainHeldState();
      }

      boolean isMovingForward() {
         return this.player.f_108618_.f_108568_;
      }

      boolean isMovingBackward() {
         return this.player.f_108618_.f_108569_;
      }

      boolean isMovingLeft() {
         return this.player.f_108618_.f_108570_ && !this.player.f_108618_.f_108568_ && !this.player.f_108618_.f_108569_;
      }

      boolean isMovingRight() {
         return this.player.f_108618_.f_108571_ && !this.player.f_108618_.f_108568_ && !this.player.f_108618_.f_108569_;
      }
   }
}
