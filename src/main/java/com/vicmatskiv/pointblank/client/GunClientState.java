package com.vicmatskiv.pointblank.client;

import com.vicmatskiv.pointblank.client.effect.MuzzleFlashEffect;
import com.vicmatskiv.pointblank.feature.FireModeFeature;
import com.vicmatskiv.pointblank.feature.ReloadFeature;
import com.vicmatskiv.pointblank.item.AmmoCount;
import com.vicmatskiv.pointblank.item.FireMode;
import com.vicmatskiv.pointblank.item.FireModeInstance;
import com.vicmatskiv.pointblank.item.GunItem;
import com.vicmatskiv.pointblank.network.MainHeldSimplifiedStateSyncRequest;
import com.vicmatskiv.pointblank.network.Network;
import com.vicmatskiv.pointblank.util.ClientUtil;
import com.vicmatskiv.pointblank.util.MiscUtil;
import com.vicmatskiv.pointblank.util.StateMachine;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.Map.Entry;
import java.util.function.Predicate;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.bernie.geckolib.util.ClientUtils;

public class GunClientState {
   private static final Logger LOGGER = LogManager.getLogger("pointblank");
   private static final Map<PlayerSlot, GunClientState> localSlotStates = new HashMap();
   private static final Map<UUID, GunClientState> noSlotStates = new WeakHashMap();
   private UUID id;
   private GunItem gunItem;
   protected int totalUninterruptedFireTime;
   private long reloadCooldownStartTime;
   private long reloadCooldownDuration;
   private long prepareFireCooldownStartTime;
   private long completeFireCooldownStartTime;
   private long drawCooldownDuration;
   private long drawCooldownStartTime;
   private long inspectCooldownStartTime;
   private long inspectCooldownDuration;
   private long prepareIdleCooldownStartTime;
   private long prepareIdleCooldownDuration = 0L;
   private long idleCooldownStartTime;
   private long idleCooldownDuration;
   private long enableFireModeCooldownStartTime;
   protected long fireCooldownStartTime;
   protected long lastSyncTime;
   protected long lastShotFiredTime;
   protected AmmoCount ammoCount = new AmmoCount();
   protected boolean isTriggerOn;
   protected int totalUninterruptedShots;
   private Map<String, GunStateListener> animationControllers = new HashMap();
   private List<GunStateListener> stateListeners = new ArrayList();
   private List<MuzzleFlashEffect> muzzleFlashEffects = new ArrayList();
   private long clientSyncTimeoutTicks = 20L;
   private boolean isAiming;
   private int remainingAmmoToReload;
   private AmmoCount reloadAmmoCount = new AmmoCount();
   private int reloadIterationIndex;
   private Component temporaryMessage;
   private long messageEndTime;
   private Predicate<GunClientState> messagePredicate;
   private Predicate<Context> hasAmmo = (context) -> {
      return this.ammoCount.getAmmoCount(context.getFireModeInstance()) > 0;
   };
   private Predicate<Context> isValidGameMode = (context) -> {
      return context.player != null && !context.player.m_5833_();
   };
   private FireState simplifiedFireState;
   private StateMachine<FireState, Context> stateMachine;
   private static final Map<UUID, GunClientState> statesById = new HashMap();

   public GunClientState(UUID id, GunItem item) {
      this.id = id;
      this.gunItem = item;
      this.stateMachine = this.createStateMachine();
      this.prepareIdleCooldownDuration = 1000000L * this.gunItem.getPrepareIdleCooldownDuration();
      statesById.put(id, this);
   }

   private StateMachine<FireState, Context> createStateMachine() {
      StateMachine.Builder<FireState, Context> builder = new StateMachine.Builder();
      builder.withTransition((List)List.of(FireState.PREPARE_IDLE, FireState.IDLE, FireState.IDLE_COOLDOWN), FireState.DRAW, this.isValidGameMode, StateMachine.TransitionMode.EVENT, (StateMachine.Action)null, this::actionDraw);
      builder.withTransition((Enum) FireState.DRAW, FireState.DRAW_COOLDOWN, (context) -> {
         return true;
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, (ctx, f, t) -> {
         this.drawCooldownStartTime = System.nanoTime();
         this.drawCooldownDuration = this.gunItem.getDrawCooldownDuration(ctx.player, this, ctx.itemStack) * 1000000L;
      });
      builder.withTransition(FireState.DRAW_COOLDOWN, FireState.PREPARE_IDLE, Predicate.not(this::isDrawCooldownInProgress));
      builder.withTransition((Enum) FireState.PREPARE_IDLE, FireState.IDLE, (ctx) -> {
         return !this.isPrepareIdleCooldownInProgress(ctx) && this.gunItem.hasIdleAnimations();
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, (StateMachine.Action)null);
      builder.withTransition((Enum) FireState.IDLE, FireState.IDLE_COOLDOWN, (ctx) -> {
         return this.gunItem.hasIdleAnimations();
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, (ctx, f, t) -> {
         this.idleCooldownStartTime = System.nanoTime();
         this.idleCooldownDuration = this.gunItem.getIdleCooldownDuration(ctx.player, this, ctx.itemStack) * 1000000L;
      });
      builder.withTransition(FireState.IDLE_COOLDOWN, FireState.IDLE, Predicate.not(this::isIdleCooldownInProgress));
      builder.withTransition((List)List.of(FireState.IDLE, FireState.IDLE_COOLDOWN), FireState.PREPARE_IDLE, (ctx) -> {
         return true;
      }, StateMachine.TransitionMode.EVENT, (StateMachine.Action)null, (StateMachine.Action)null);
      builder.withTransition((List)List.of(FireState.PREPARE_IDLE, FireState.IDLE, FireState.IDLE_COOLDOWN), FireState.INSPECT, this.isValidGameMode, StateMachine.TransitionMode.EVENT, (StateMachine.Action)null, this::actionInspect);
      builder.withTransition((Enum) FireState.INSPECT, FireState.INSPECT_COOLDOWN, (context) -> {
         return true;
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, (ctx, f, t) -> {
         this.inspectCooldownStartTime = System.nanoTime();
         this.inspectCooldownDuration = this.gunItem.getInspectCooldownDuration(ctx.player, this, ctx.itemStack) * 1000000L;
      });
      builder.withTransition(FireState.INSPECT_COOLDOWN, FireState.PREPARE_IDLE, Predicate.not(this::inspectCooldownInProgress));
      builder.withTransition((Enum) FireState.PREPARE_IDLE, FireState.CHANGE_FIRE_MODE, this.isValidGameMode, StateMachine.TransitionMode.EVENT, (StateMachine.Action)null, this::actionEnableFireMode);
      builder.withTransition((List)List.of(FireState.PREPARE_IDLE, FireState.IDLE, FireState.IDLE_COOLDOWN), FireState.CHANGE_FIRE_MODE, this.isValidGameMode, StateMachine.TransitionMode.EVENT, (StateMachine.Action)null, this::actionEnableFireMode);
      builder.withTransition((Enum) FireState.CHANGE_FIRE_MODE, FireState.CHANGE_FIRE_MODE_COOLDOWN, (context) -> {
         return true;
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, (ctx, f, t) -> {
         this.enableFireModeCooldownStartTime = System.nanoTime();
      });
      builder.withTransition(FireState.CHANGE_FIRE_MODE_COOLDOWN, FireState.PREPARE_IDLE, Predicate.not(this::isEnableFireModeCooldownInProgress));
      builder.withTransition((Enum) FireState.PREPARE_IDLE, FireState.PREPARE_RELOAD, this.isValidGameMode.and(Predicate.not(this::requiresPhasedReload)), StateMachine.TransitionMode.EVENT, (StateMachine.Action)null, this::actionPrepareReload);
      builder.withTransition((List)List.of(FireState.PREPARE_IDLE, FireState.IDLE, FireState.IDLE_COOLDOWN), FireState.PREPARE_RELOAD, this.isValidGameMode.and(Predicate.not(this::requiresPhasedReload)), StateMachine.TransitionMode.EVENT, (StateMachine.Action)null, this::actionPrepareReload);
      builder.withTransition((Enum) FireState.PREPARE_RELOAD, FireState.PREPARE_RELOAD_COOLDOWN, (context) -> {
         return true;
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, (ctx, f, t) -> {
         this.reloadCooldownStartTime = System.nanoTime();
         this.reloadCooldownDuration = this.gunItem.getReloadingCooldownTime(GunItem.ReloadPhase.PREPARING, ctx.player, this, ctx.itemStack) * 1000000L;
      });
      builder.withTransition((Enum) FireState.PREPARE_RELOAD_COOLDOWN, FireState.RELOAD, Predicate.not(this::isReloadCooldownInProgress), StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, this::actionReload);
      builder.withTransition((Enum) FireState.RELOAD, FireState.RELOAD_COOLDOWN, (context) -> {
         return true;
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, (ctx, f, t) -> {
         this.reloadCooldownStartTime = System.nanoTime();
         this.reloadCooldownDuration = this.gunItem.getReloadingCooldownTime(GunItem.ReloadPhase.RELOADING, ctx.player, this, ctx.itemStack) * 1000000L;
      });
      builder.withTransition((Enum) FireState.RELOAD_COOLDOWN, FireState.COMPLETE_RELOAD, Predicate.not(this::isReloadCooldownInProgress), StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, this::actionCompleteReload);
      builder.withTransition((Enum) FireState.PREPARE_IDLE, FireState.PREPARE_RELOAD_ITER, (context) -> {
         return this.isValidGameMode.test(context) && this.canReload(context) && this.requiresPhasedReload(context);
      }, StateMachine.TransitionMode.EVENT, (StateMachine.Action)null, this::actionPrepareReload);
      builder.withTransition((List)List.of(FireState.PREPARE_IDLE, FireState.IDLE, FireState.IDLE_COOLDOWN), FireState.PREPARE_RELOAD_ITER, (context) -> {
         return this.isValidGameMode.test(context) && this.canReload(context) && this.requiresPhasedReload(context);
      }, StateMachine.TransitionMode.EVENT, (StateMachine.Action)null, this::actionPrepareReload);
      builder.withTransition((Enum) FireState.PREPARE_RELOAD_ITER, FireState.PREPARE_RELOAD_COOLDOWN_ITER, (context) -> {
         return true;
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, (ctx, f, t) -> {
         this.reloadCooldownStartTime = System.nanoTime();
         this.reloadCooldownDuration = this.gunItem.getReloadingCooldownTime(GunItem.ReloadPhase.PREPARING, ctx.player, this, ctx.itemStack) * 1000000L;
      });
      builder.withTransition((Enum) FireState.PREPARE_RELOAD_COOLDOWN_ITER, FireState.RELOAD_ITER, Predicate.not(this::isReloadCooldownInProgress), StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, this::actionReload);
      builder.withTransition((Enum) FireState.RELOAD_ITER, FireState.RELOAD_COOLDOWN_ITER, (context) -> {
         return true;
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, (ctx, f, t) -> {
         this.reloadCooldownStartTime = System.nanoTime();
         this.reloadCooldownDuration = this.gunItem.getReloadingCooldownTime(GunItem.ReloadPhase.RELOADING, ctx.player, this, ctx.itemStack) * 1000000L;
      });
      builder.withTransition((Enum) FireState.RELOAD_COOLDOWN_ITER, FireState.RELOAD_ITER, (context) -> {
         return !this.isReloadCooldownInProgress(context) && this.remainingAmmoToReload > 0;
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, this::actionReload);
      builder.withTransition((Enum) FireState.RELOAD_COOLDOWN_ITER, FireState.COMPLETE_RELOAD, (context) -> {
         return !this.isReloadCooldownInProgress(context);
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, this::actionCompleteReload);
      builder.withTransition((Enum) FireState.COMPLETE_RELOAD, FireState.COMPLETE_RELOAD_COOLDOWN, (context) -> {
         return true;
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, (ctx, f, t) -> {
         this.reloadCooldownStartTime = System.nanoTime();
         this.reloadCooldownDuration = this.gunItem.getReloadingCooldownTime(GunItem.ReloadPhase.COMPLETETING, ctx.player, this, ctx.itemStack) * 1000000L;
      });
      builder.withTransition((Enum) FireState.COMPLETE_RELOAD_COOLDOWN, FireState.PREPARE_IDLE, Predicate.not(this::isReloadCooldownInProgress).and(this::canCompleteReload), StateMachine.TransitionMode.AUTO, this::actionApplyReloadAmmo, (StateMachine.Action)null);
      builder.withTransition((List)List.of(FireState.PREPARE_IDLE, FireState.IDLE, FireState.IDLE_COOLDOWN, FireState.INSPECT_COOLDOWN), FireState.PREPARE_FIRE_SINGLE, this.isValidGameMode.and(this.hasAmmo.and((context) -> {
         return GunItem.getSelectedFireModeType(context.itemStack) == FireMode.SINGLE;
      }).and((context) -> {
         return this.isTriggerOn && context.itemStack == context.player.m_21205_() && !context.player.m_20142_() && !FirstPersonWalkingAnimationHandler.isPlayingBlendingAnimation(Set.of(GunItem.RAW_ANIMATION_PREPARE_RUNNING, GunItem.RAW_ANIMATION_COMPLETE_RUNNING, GunItem.RAW_ANIMATION_RUNNING), context.itemStack, 0.0D, 0.25D);
      })), StateMachine.TransitionMode.EVENT, (StateMachine.Action)null, this::actionPrepareFire);
      builder.withTransition((Enum) FireState.PREPARE_FIRE_SINGLE, FireState.PREPARE_FIRE_COOLDOWN_SINGLE, (context) -> {
         return true;
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, (ctx, f, t) -> {
         this.prepareFireCooldownStartTime = System.nanoTime();
      });
      builder.withTransition((Enum) FireState.PREPARE_FIRE_COOLDOWN_SINGLE, FireState.PREPARE_IDLE, (context) -> {
         return !this.isTriggerOn;
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, (StateMachine.Action)null);
      builder.withTransition((Enum) FireState.PREPARE_FIRE_COOLDOWN_SINGLE, FireState.FIRE_SINGLE, (context) -> {
         return this.isTriggerOn && !this.isPrepareFireCooldownInProgress(context);
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, this::actionFire);
      builder.withTransition((Enum) FireState.FIRE_SINGLE, FireState.FIRE_COOLDOWN_SINGLE, (context) -> {
         return true;
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, (ctx, f, t) -> {
         this.fireCooldownStartTime = System.nanoTime();
      });
      builder.withTransition((Enum) FireState.FIRE_COOLDOWN_SINGLE, FireState.COMPLETE_FIRE, Predicate.not(this::isFireCooldownInProgress), StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, this::actionCompleteFire);
      builder.withTransition((List)List.of(FireState.PREPARE_IDLE, FireState.IDLE, FireState.IDLE_COOLDOWN, FireState.INSPECT_COOLDOWN), FireState.PREPARE_FIRE_BURST, this.isValidGameMode.and(this.hasAmmo.and((context) -> {
         return GunItem.getSelectedFireModeType(context.itemStack) == FireMode.BURST;
      }).and((context) -> {
         return this.isTriggerOn && context.itemStack == context.player.m_21205_() && !context.player.m_20142_() && !FirstPersonWalkingAnimationHandler.isPlayingBlendingAnimation(Set.of(GunItem.RAW_ANIMATION_PREPARE_RUNNING, GunItem.RAW_ANIMATION_COMPLETE_RUNNING, GunItem.RAW_ANIMATION_RUNNING), context.itemStack, 0.0D, 0.25D);
      })), StateMachine.TransitionMode.EVENT, (StateMachine.Action)null, this::actionPrepareFire);
      builder.withTransition((Enum) FireState.PREPARE_FIRE_BURST, FireState.PREPARE_FIRE_COOLDOWN_BURST, (context) -> {
         return true;
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, (ctx, f, t) -> {
         this.prepareFireCooldownStartTime = System.nanoTime();
      });
      builder.withTransition((Enum) FireState.PREPARE_FIRE_COOLDOWN_BURST, FireState.PREPARE_IDLE, (context) -> {
         return !this.isTriggerOn;
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, (StateMachine.Action)null);
      builder.withTransition((Enum) FireState.PREPARE_FIRE_COOLDOWN_BURST, FireState.FIRE_BURST, (context) -> {
         return this.isTriggerOn && !this.isPrepareFireCooldownInProgress(context);
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, this::actionFire);
      builder.withTransition((Enum) FireState.FIRE_BURST, FireState.FIRE_COOLDOWN_BURST, (context) -> {
         return true;
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, (ctx, f, t) -> {
         this.fireCooldownStartTime = System.nanoTime();
      });
      builder.withTransition((Enum) FireState.FIRE_COOLDOWN_BURST, FireState.FIRE_BURST, Predicate.not(this::isFireCooldownInProgress).and(this.hasAmmo).and((context) -> {
         return GunItem.getSelectedFireModeType(context.itemStack) == FireMode.BURST;
      }).and((context) -> {
         return this.isTriggerOn && context.itemStack == ((Player)context.player).m_21205_();
      }).and((context) -> {
         return this.totalUninterruptedShots < this.gunItem.getBurstShots(context.itemStack, context.getFireModeInstance());
      }), StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, this::actionFire);
      builder.withTransition((Enum) FireState.FIRE_COOLDOWN_BURST, FireState.COMPLETE_FIRE, Predicate.not(this::isFireCooldownInProgress).and(this.hasAmmo.negate().or((context) -> {
         return !this.isTriggerOn;
      }).or((context) -> {
         return context.itemStack != ((Player)context.player).m_21205_();
      }).or((context) -> {
         return this.totalUninterruptedShots >= this.gunItem.getBurstShots(context.itemStack, context.getFireModeInstance());
      })), StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, this::actionCompleteFire);
      builder.withTransition((List)List.of(FireState.PREPARE_IDLE, FireState.IDLE, FireState.IDLE_COOLDOWN, FireState.INSPECT_COOLDOWN), FireState.PREPARE_FIRE_AUTO, this.isValidGameMode.and(this.hasAmmo.and((context) -> {
         return GunItem.getSelectedFireModeType(context.itemStack) == FireMode.AUTOMATIC;
      }).and((context) -> {
         return this.isTriggerOn && context.itemStack == context.player.m_21205_() && !context.player.m_20142_() && !FirstPersonWalkingAnimationHandler.isPlayingBlendingAnimation(Set.of(GunItem.RAW_ANIMATION_PREPARE_RUNNING, GunItem.RAW_ANIMATION_COMPLETE_RUNNING, GunItem.RAW_ANIMATION_RUNNING), context.itemStack, 0.0D, 0.25D);
      })), StateMachine.TransitionMode.EVENT, (StateMachine.Action)null, this::actionPrepareFire);
      builder.withTransition((Enum) FireState.PREPARE_FIRE_AUTO, FireState.PREPARE_FIRE_COOLDOWN_AUTO, (context) -> {
         return true;
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, (ctx, f, t) -> {
         this.prepareFireCooldownStartTime = System.nanoTime();
      });
      builder.withTransition((Enum) FireState.PREPARE_FIRE_COOLDOWN_AUTO, FireState.PREPARE_IDLE, (context) -> {
         return !this.isTriggerOn;
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, (StateMachine.Action)null);
      builder.withTransition((Enum) FireState.PREPARE_FIRE_COOLDOWN_AUTO, FireState.FIRE_AUTO, (context) -> {
         return this.isTriggerOn && !this.isPrepareFireCooldownInProgress(context);
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, this::actionFire);
      builder.withTransition((Enum) FireState.FIRE_AUTO, FireState.FIRE_COOLDOWN_AUTO, (context) -> {
         return true;
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, (ctx, f, t) -> {
         this.fireCooldownStartTime = System.nanoTime();
      });
      builder.withTransition((Enum) FireState.FIRE_COOLDOWN_AUTO, FireState.FIRE_AUTO, Predicate.not(this::isFireCooldownInProgress).and(this.hasAmmo).and((context) -> {
         return GunItem.getSelectedFireModeType(context.itemStack) == FireMode.AUTOMATIC;
      }).and((context) -> {
         return this.isTriggerOn && context.itemStack == ((Player)context.player).m_21205_();
      }), StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, this::actionFire);
      builder.withTransition((Enum) FireState.FIRE_COOLDOWN_AUTO, FireState.COMPLETE_FIRE, Predicate.not(this::isFireCooldownInProgress).and(this.hasAmmo.negate().or((context) -> {
         return !this.isTriggerOn;
      }).or((context) -> {
         return context.itemStack != ((Player)context.player).m_21205_();
      })), StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, this::actionCompleteFire);
      builder.withTransition((Enum) FireState.COMPLETE_FIRE, FireState.COMPLETE_FIRE_COOLDOWN, (context) -> {
         return true;
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, (ctx, f, t) -> {
         this.completeFireCooldownStartTime = System.nanoTime();
      });
      builder.withTransition((Enum) FireState.COMPLETE_FIRE_COOLDOWN, FireState.PREPARE_IDLE, Predicate.not(this::isCompleteFireCooldownInProgress), StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, (StateMachine.Action)null);
      builder.withOnSetStateAction(FireState.PREPARE_IDLE, (ctx, f, t) -> {
         this.actionPrepareIdle(ctx);
      });
      builder.withOnSetStateAction(FireState.IDLE, (ctx, f, t) -> {
         this.actionFiddle(ctx);
      });
      builder.withOnChangeStateAction((ctx, f, t) -> {
         boolean wasFiring = isFiring(f);
         boolean isFiring = isFiring(t);
         if (isFiring && !wasFiring) {
            this.actionStartAutoFiring(ctx);
         } else if (!isFiring && wasFiring) {
            this.actionStopAutoFiring(ctx);
         }

      });
      return builder.build(FireState.PREPARE_IDLE);
   }

   private boolean isPrepareIdleCooldownInProgress(Context context) {
      return System.nanoTime() - this.prepareIdleCooldownStartTime <= this.prepareIdleCooldownDuration;
   }

   private boolean isIdleCooldownInProgress(Context context) {
      return System.nanoTime() - this.idleCooldownStartTime <= this.idleCooldownDuration;
   }

   private boolean isDrawCooldownInProgress(Context context) {
      return System.nanoTime() - this.drawCooldownStartTime <= this.drawCooldownDuration;
   }

   private boolean inspectCooldownInProgress(Context context) {
      return System.nanoTime() - this.inspectCooldownStartTime <= this.inspectCooldownDuration;
   }

   private boolean isEnableFireModeCooldownInProgress(Context context) {
      return (double)(System.nanoTime() - this.enableFireModeCooldownStartTime) <= 1000000.0D * (double)FireModeFeature.getEnableFireModeCooldownDuration(context.player, this, context.itemStack);
   }

   private boolean isCompleteFireCooldownInProgress(Context context) {
      return (double)(System.nanoTime() - this.completeFireCooldownStartTime) <= 1000000.0D * (double)FireModeFeature.getCompleteFireCooldownDuration(context.player, this, context.itemStack);
   }

   private boolean isFireCooldownInProgress(Context context) {
      return System.nanoTime() - this.fireCooldownStartTime <= this.getFireCooldownDuration(context);
   }

   private long getFireCooldownDuration(Context context) {
      return (long)(6.0E10D / (double)FireModeFeature.getRpm(context.itemStack));
   }

   private boolean isPrepareFireCooldownInProgress(Context context) {
      return (double)(System.nanoTime() - this.prepareFireCooldownStartTime) <= 1000000.0D * (double)FireModeFeature.getPrepareFireCooldownDuration(context.player, this, context.itemStack);
   }

   private boolean isReloadCooldownInProgress(Context context) {
      return System.nanoTime() - this.reloadCooldownStartTime <= this.reloadCooldownDuration;
   }

   private boolean requiresPhasedReload(Context context) {
      return this.gunItem.requiresPhasedReload();
   }

   private boolean canReload(Context context) {
      int ammoToReload = this.gunItem.canReloadGun(context.itemStack, (Player)context.player, context.getFireModeInstance());
      return ammoToReload > 0;
   }

   private boolean canCompleteReload(Context context) {
      return this.reloadAmmoCount.getAmmoCount(context.getFireModeInstance()) > 0 || this.isClientSyncTimeoutExpired(MiscUtil.getLevel(context.player));
   }

   private void actionPrepareReload(Context context, FireState fromState, FireState toState) {
      this.reloadIterationIndex = this.ammoCount.getAmmoCount(context.getFireModeInstance()) - 1;
      this.remainingAmmoToReload = this.gunItem.canReloadGun(context.itemStack, (Player)context.player, context.getFireModeInstance());
      LOGGER.debug("Preparing to reload ammo: {}", this.remainingAmmoToReload);
      this.publishMessage(Component.m_237115_("message.pointblank.reloading").m_130946_("..."), 10000L, (state) -> {
         return state.isReloading();
      });
      LOGGER.debug("{} Set reload iteration index to {}", System.currentTimeMillis() % 100000L, this.reloadIterationIndex);
      Iterator var4 = this.stateListeners.iterator();

      while(var4.hasNext()) {
         GunStateListener listener = (GunStateListener)var4.next();
         listener.onPrepareReloading(context.player, this, context.itemStack);
      }

   }

   private void actionReload(Context context, FireState fromState, FireState toState) {
      ++this.reloadIterationIndex;
      int ammoToReloadPerIteration = Math.min(this.remainingAmmoToReload, ReloadFeature.getMaxAmmoPerReloadIteration(context.itemStack));
      this.remainingAmmoToReload -= ammoToReloadPerIteration;
      this.ammoCount.incrementAmmoCount(context.getFireModeInstance(), ammoToReloadPerIteration);
      LOGGER.debug("Ammo to reload per iteration: {}, remaining ammo to reload: {}, current ammo: {}, reload iteration index: {}", ammoToReloadPerIteration, this.remainingAmmoToReload, this.ammoCount, this.reloadIterationIndex);
      this.gunItem.requestReloadFromServer((Player)context.player, context.itemStack);
      Iterator var5 = this.stateListeners.iterator();

      while(var5.hasNext()) {
         GunStateListener listener = (GunStateListener)var5.next();
         listener.onStartReloading(context.player, this, context.itemStack);
      }

   }

   private void actionCompleteReload(Context context, FireState fromState, FireState toState) {
      this.reloadIterationIndex = 0;
      LOGGER.debug("Completing reload");
      Iterator var4 = this.stateListeners.iterator();

      while(var4.hasNext()) {
         GunStateListener listener = (GunStateListener)var4.next();
         listener.onCompleteReloading(context.player, this, context.itemStack);
      }

   }

   private void actionPrepareFire(Context context, FireState fromState, FireState toState) {
      this.publishMessage(Component.m_237115_("message.pointblank.preparing").m_130946_("..."), 3500L, (state) -> {
         return state.isPreparingFiring();
      });
      Iterator var4 = this.stateListeners.iterator();

      while(var4.hasNext()) {
         GunStateListener listener = (GunStateListener)var4.next();
         listener.onPrepareFiring(context.player, this, context.itemStack);
      }

   }

   private void actionFire(Context context, FireState fromState, FireState toState) {
      this.gunItem.requestFireFromServer(this, (Player)context.player, context.itemStack, context.targetEntity);
      if (this.gunItem.getMaxAmmoCapacity(context.itemStack, context.getFireModeInstance()) < Integer.MAX_VALUE) {
         this.ammoCount.incrementAmmoCount(context.getFireModeInstance(), -1);
      }

      ++this.totalUninterruptedShots;
      this.lastShotFiredTime = MiscUtil.getLevel(context.player).m_46467_();
      Iterator var4 = this.stateListeners.iterator();

      while(var4.hasNext()) {
         GunStateListener listener = (GunStateListener)var4.next();
         listener.onStartFiring(context.player, this, context.itemStack);
      }

   }

   private void actionDraw(Context context, FireState fromState, FireState toState) {
      Iterator var4 = this.stateListeners.iterator();

      while(var4.hasNext()) {
         GunStateListener listener = (GunStateListener)var4.next();
         listener.onDrawing(context.player, this, context.itemStack);
      }

   }

   private void actionInspect(Context context, FireState fromState, FireState toState) {
      Iterator var4 = this.stateListeners.iterator();

      while(var4.hasNext()) {
         GunStateListener listener = (GunStateListener)var4.next();
         listener.onInspecting(context.player, this, context.itemStack);
      }

   }

   private void actionEnableFireMode(Context context, FireState fromState, FireState toState) {
      ((GunItem)context.itemStack.m_41720_()).initiateClientSideFireMode((Player)context.player, context.itemStack);
      Iterator var4 = this.stateListeners.iterator();

      while(var4.hasNext()) {
         GunStateListener listener = (GunStateListener)var4.next();
         listener.onEnablingFireMode(context.player, this, context.itemStack);
      }

   }

   private void actionCompleteFire(Context context, FireState fromState, FireState toState) {
      this.publishMessage(Component.m_237115_("message.pointblank.completing").m_130946_("..."), 3500L, (state) -> {
         return state.isCompletingFiring();
      });
      LOGGER.debug("{} Completing firing in state {}", System.currentTimeMillis() % 100000L, toState);
      Iterator var4 = this.stateListeners.iterator();

      while(var4.hasNext()) {
         GunStateListener listener = (GunStateListener)var4.next();
         listener.onCompleteFiring(context.player, this, context.itemStack);
      }

   }

   private void actionPrepareIdle(Context context) {
      this.prepareIdleCooldownStartTime = System.nanoTime();
      this.totalUninterruptedShots = 0;
      Iterator var2 = this.stateListeners.iterator();

      while(var2.hasNext()) {
         GunStateListener listener = (GunStateListener)var2.next();
         listener.onPrepareIdle(context.player, this, context.itemStack);
      }

   }

   private void actionFiddle(Context context) {
      this.totalUninterruptedShots = 0;
      Iterator var2 = this.stateListeners.iterator();

      while(var2.hasNext()) {
         GunStateListener listener = (GunStateListener)var2.next();
         listener.onIdle(context.player, this, context.itemStack);
      }

   }

   private void actionStartAutoFiring(Context context) {
      Iterator var2 = this.stateListeners.iterator();

      while(var2.hasNext()) {
         GunStateListener listener = (GunStateListener)var2.next();
         listener.onStartAutoFiring(context.player, this, context.itemStack);
      }

   }

   private void actionStopAutoFiring(Context context) {
      Iterator var2 = this.stateListeners.iterator();

      while(var2.hasNext()) {
         GunStateListener listener = (GunStateListener)var2.next();
         listener.onStopAutoFiring(context.player, this, context.itemStack);
      }

   }

   private void actionApplyReloadAmmo(Context context, FireState fromState, FireState toState) {
      FireModeInstance fireModeInstance = context.getFireModeInstance();
      if (this.reloadAmmoCount.getAmmoCount(fireModeInstance) > 0) {
         LOGGER.debug("Applying reload ammo count: {}", this.reloadAmmoCount);
         this.ammoCount.setAmmoCount(context.getFireModeInstance(), this.reloadAmmoCount.getAmmoCount(fireModeInstance));
         this.reloadAmmoCount.setAmmoCount(fireModeInstance, 0);
      }

   }

   public boolean tryReload(LivingEntity player, ItemStack itemStack) {
      Context context = new Context(player, itemStack);
      return this.stateMachine.setStateToAnyOf(context, List.of(FireState.PREPARE_RELOAD, FireState.PREPARE_RELOAD_ITER)) != null;
   }

   public boolean tryFire(LivingEntity player, ItemStack itemStack, Entity targetEntity) {
      Context context = new Context(player, itemStack, targetEntity);
      return this.stateMachine.setStateToAnyOf(context, List.of(FireState.PREPARE_FIRE_SINGLE, FireState.PREPARE_FIRE_BURST, FireState.PREPARE_FIRE_AUTO)) != null;
   }

   public boolean tryDraw(LivingEntity player, ItemStack itemStack) {
      Context context = new Context(player, itemStack);
      return this.stateMachine.setState(context, (Enum) FireState.DRAW) != null;
   }

   public boolean tryDeactivate(LivingEntity player, ItemStack itemStack) {
      Context context = new Context(player, itemStack);
      return this.stateMachine.setState(context, (Enum) FireState.PREPARE_IDLE) != null;
   }

   public boolean tryInspect(LivingEntity player, ItemStack itemStack) {
      Context context = new Context(player, itemStack);
      return this.stateMachine.setState(context, (Enum) FireState.INSPECT) != null;
   }

   public boolean tryChangeFireMode(LivingEntity player, ItemStack itemStack) {
      Context context = new Context(player, itemStack);
      return this.stateMachine.setState(context, (Enum) FireState.CHANGE_FIRE_MODE) != null;
   }

   public UUID getId() {
      return this.id;
   }

   public GunItem getGunItem() {
      return this.gunItem;
   }

   public void setTrigger(boolean isTriggerOn) {
      this.isTriggerOn = isTriggerOn;
   }

   public boolean isIdle() {
      FireState fireState = (FireState)this.stateMachine.getCurrentState();
      return this.simplifiedFireState == FireState.IDLE || fireState == FireState.PREPARE_IDLE || fireState == FireState.IDLE || fireState == FireState.IDLE_COOLDOWN;
   }

   public boolean isPreparingReload() {
      FireState fireState = (FireState)this.stateMachine.getCurrentState();
      return fireState == FireState.PREPARE_RELOAD || fireState == FireState.PREPARE_RELOAD_ITER || fireState == FireState.PREPARE_RELOAD_COOLDOWN || fireState == FireState.PREPARE_RELOAD_COOLDOWN_ITER;
   }

   public boolean isReloading() {
      FireState fireState = (FireState)this.stateMachine.getCurrentState();
      return this.simplifiedFireState == FireState.RELOAD || fireState == FireState.PREPARE_RELOAD || fireState == FireState.PREPARE_RELOAD_COOLDOWN || fireState == FireState.PREPARE_RELOAD_ITER || fireState == FireState.PREPARE_RELOAD_COOLDOWN_ITER || fireState == FireState.RELOAD || fireState == FireState.RELOAD_ITER || fireState == FireState.RELOAD_COOLDOWN || fireState == FireState.RELOAD_COOLDOWN_ITER || fireState == FireState.COMPLETE_RELOAD || fireState == FireState.COMPLETE_RELOAD_COOLDOWN;
   }

   public boolean isFiring() {
      FireState fireState = (FireState)this.stateMachine.getCurrentState();
      return isFiring(fireState) || this.simplifiedFireState == FireState.FIRE_SINGLE;
   }

   private static boolean isFiring(FireState fireState) {
      return fireState == FireState.PREPARE_FIRE_SINGLE || fireState == FireState.PREPARE_FIRE_COOLDOWN_SINGLE || fireState == FireState.PREPARE_FIRE_AUTO || fireState == FireState.PREPARE_FIRE_COOLDOWN_AUTO || fireState == FireState.PREPARE_FIRE_BURST || fireState == FireState.PREPARE_FIRE_COOLDOWN_BURST || fireState == FireState.FIRE_SINGLE || fireState == FireState.FIRE_BURST || fireState == FireState.FIRE_AUTO || fireState == FireState.FIRE_COOLDOWN_SINGLE || fireState == FireState.FIRE_COOLDOWN_BURST || fireState == FireState.FIRE_COOLDOWN_AUTO || fireState == FireState.COMPLETE_FIRE || fireState == FireState.COMPLETE_FIRE_COOLDOWN;
   }

   public boolean isPreparingFiring() {
      FireState fireState = (FireState)this.stateMachine.getCurrentState();
      return fireState == FireState.PREPARE_FIRE_SINGLE || fireState == FireState.PREPARE_FIRE_COOLDOWN_SINGLE || fireState == FireState.PREPARE_FIRE_AUTO || fireState == FireState.PREPARE_FIRE_COOLDOWN_AUTO || fireState == FireState.PREPARE_FIRE_BURST || fireState == FireState.PREPARE_FIRE_COOLDOWN_BURST;
   }

   public boolean isCompletingFiring() {
      FireState fireState = (FireState)this.stateMachine.getCurrentState();
      return fireState == FireState.COMPLETE_FIRE || fireState == FireState.COMPLETE_FIRE_COOLDOWN;
   }

   public boolean isDrawing() {
      FireState fireState = (FireState)this.stateMachine.getCurrentState();
      return this.simplifiedFireState == FireState.DRAW || fireState == FireState.DRAW || fireState == FireState.DRAW_COOLDOWN;
   }

   public boolean isInspecting() {
      FireState fireState = (FireState)this.stateMachine.getCurrentState();
      return this.simplifiedFireState == FireState.INSPECT || fireState == FireState.INSPECT || fireState == FireState.INSPECT_COOLDOWN;
   }

   public boolean isChangingFireMode() {
      FireState fireState = (FireState)this.stateMachine.getCurrentState();
      return fireState == FireState.CHANGE_FIRE_MODE || fireState == FireState.CHANGE_FIRE_MODE_COOLDOWN;
   }

   public void inventoryTick(LivingEntity player, ItemStack itemStack, boolean isSelected) {
      this.updateState(player, itemStack, isSelected);
      Iterator var4 = this.stateListeners.iterator();

      while(var4.hasNext()) {
         GunStateListener listener = (GunStateListener)var4.next();
         listener.onGameTick(player, this);
      }

   }

   public void stateTick(LivingEntity player, ItemStack itemStack, boolean isSelected) {
      this.updateState(player, itemStack, isSelected);
      Iterator it = this.stateListeners.iterator();

      while(it.hasNext()) {
         GunStateListener listener = (GunStateListener)it.next();
         listener.onStateTick(player, this);
      }

      it = this.muzzleFlashEffects.iterator();

      while(it.hasNext()) {
         MuzzleFlashEffect effect = (MuzzleFlashEffect)it.next();
         if (effect.isExpired()) {
            LOGGER.debug("Effect {} expired, removing", effect);
            it.remove();
         }
      }

   }

   private boolean isClientSyncTimeoutExpired(Level level) {
      return level.m_46467_() - Math.max(this.lastSyncTime, this.lastShotFiredTime) > this.clientSyncTimeoutTicks;
   }

   public void updateState(LivingEntity player, ItemStack itemStack, boolean isSelected) {
      if (player == ClientUtil.getClientPlayer()) {
         if (!isSelected && this.isAiming) {
            this.setAiming(false);
            LOGGER.debug("Turning the aiming off");
         }

         Context context = new Context(player, itemStack);
         this.stateMachine.update(context);
         Level level = MiscUtil.getLevel(player);
         if (!this.isFiring() && !this.isReloading() && this.isClientSyncTimeoutExpired(level)) {
            this.syncAmmo(level, itemStack);
         }

         Iterator var6 = this.stateListeners.iterator();

         while(var6.hasNext()) {
            GunStateListener listener = (GunStateListener)var6.next();
            listener.onUpdateState(player, this);
         }

         FireState updatedSimplifiedFireState = FireState.getSimplifiedFireState((FireState)this.stateMachine.getCurrentState());
         if (updatedSimplifiedFireState != this.simplifiedFireState) {
            this.simplifiedFireState = updatedSimplifiedFireState;
            Network.networkChannel.sendToServer(new MainHeldSimplifiedStateSyncRequest(this.id, updatedSimplifiedFireState));
         }

      }
   }

   private void syncAmmo(Level level, ItemStack itemStack) {
      List<FireModeInstance> fireModeInstances = GunItem.getFireModes(itemStack);

      FireModeInstance fireModeInstance;
      int stackAmmo;
      for(Iterator var4 = fireModeInstances.iterator(); var4.hasNext(); this.ammoCount.setAmmoCount(fireModeInstance, stackAmmo)) {
         fireModeInstance = (FireModeInstance)var4.next();
         stackAmmo = GunItem.getAmmo(itemStack, fireModeInstance);
         if (stackAmmo != this.ammoCount.getAmmoCount(fireModeInstance)) {
            LOGGER.debug("Current ammo {} is out of sync with server count {} ", this.ammoCount, stackAmmo);
         }
      }

      this.lastSyncTime = level.m_46467_();
   }

   public int getTotalUninterruptedShots() {
      return this.totalUninterruptedShots;
   }

   public void jump(LivingEntity player, ItemStack itemStack) {
      Iterator var3 = this.stateListeners.iterator();

      while(var3.hasNext()) {
         GunStateListener listener = (GunStateListener)var3.next();
         listener.onJumping(player, this, itemStack);
      }

   }

   public void confirmHitScanTarget(LivingEntity player, ItemStack itemStack, HitResult hitResult, float damage) {
      Iterator var5 = this.stateListeners.iterator();

      while(var5.hasNext()) {
         GunStateListener listener = (GunStateListener)var5.next();
         listener.onHitScanTargetConfirmed(player, this, itemStack, hitResult, damage);
      }

   }

   public void acquireHitScan(LivingEntity player, ItemStack itemStack, HitResult hitResult) {
      Iterator var4 = this.stateListeners.iterator();

      while(var4.hasNext()) {
         GunStateListener listener = (GunStateListener)var4.next();
         listener.onHitScanTargetAcquired(player, this, itemStack, hitResult);
      }

   }

   public void renderTick(LivingEntity player, ItemStack itemStack, float partialTicks) {
      Iterator var4 = this.stateListeners.iterator();

      while(var4.hasNext()) {
         GunStateListener listener = (GunStateListener)var4.next();
         listener.onRenderTick(player, this, itemStack, (ItemDisplayContext)null, partialTicks);
      }

   }

   public GunStateListener getAnimationController(String controllerId) {
      return (GunStateListener)this.animationControllers.get(controllerId);
   }

   public void setAnimationController(String controllerId, GunStateListener controller) {
      this.animationControllers.put(controllerId, controller);
      this.addListener(controller);
   }

   public void addListener(GunStateListener listener) {
      this.stateListeners.add(listener);
   }

   public int getAmmoCount(FireModeInstance fireModeInstance) {
      return this.ammoCount.getAmmoCount(fireModeInstance);
   }

   public FireState getFireState() {
      return (FireState)this.stateMachine.getCurrentState();
   }

   public boolean isAiming() {
      return this.isAiming;
   }

   public void setAiming(boolean isAiming) {
      if (isAiming != this.isAiming) {
         this.isAiming = isAiming;
         this.toggleAiming(isAiming);
      }

   }

   private void toggleAiming(boolean isAiming) {
      Iterator var2 = this.stateListeners.iterator();

      while(var2.hasNext()) {
         GunStateListener listener = (GunStateListener)var2.next();
         listener.onToggleAiming(isAiming);
      }

   }

   public void reloadAmmo(Level level, FireModeInstance fireModeInstance, int reloadAmmoCount) {
      this.lastSyncTime = level.m_46467_();
      this.reloadAmmoCount.setAmmoCount(fireModeInstance, reloadAmmoCount);
   }

   public int getReloadIterationIndex() {
      return this.reloadIterationIndex;
   }

   public GunItem.ReloadPhase getReloadPhase() {
      GunItem.ReloadPhase phase;
      switch((FireState)this.stateMachine.getCurrentState()) {
      case PREPARE_RELOAD:
      case PREPARE_RELOAD_COOLDOWN:
      case PREPARE_RELOAD_ITER:
      case PREPARE_RELOAD_COOLDOWN_ITER:
         phase = GunItem.ReloadPhase.PREPARING;
         break;
      case RELOAD:
      case RELOAD_COOLDOWN:
      case RELOAD_ITER:
      case RELOAD_COOLDOWN_ITER:
         phase = GunItem.ReloadPhase.RELOADING;
         break;
      case COMPLETE_RELOAD:
      case COMPLETE_RELOAD_COOLDOWN:
         phase = GunItem.ReloadPhase.COMPLETETING;
         break;
      default:
         phase = null;
      }

      return phase;
   }

   public void publishMessage(Component message, long displayDurationMillis, Predicate<GunClientState> messagePredicate) {
      this.temporaryMessage = message;
      this.messageEndTime = System.currentTimeMillis() + displayDurationMillis;
      this.messagePredicate = messagePredicate;
   }

   public Component getCurrentMessage() {
      if (this.temporaryMessage != null && System.currentTimeMillis() < this.messageEndTime) {
         return this.messagePredicate != null && !this.messagePredicate.test(this) ? null : this.temporaryMessage;
      } else {
         return null;
      }
   }

   public String toString() {
      return String.format("GunClientState[sid=%s,id=%s]", System.identityHashCode(this), this.id);
   }

   public static GunClientState getMainHeldState() {
      Player player = ClientUtils.getClientPlayer();
      return getMainHeldState(player);
   }

   public static GunClientState getMainHeldState(Player player) {
      if (player == null) {
         return null;
      } else {
         ItemStack itemStack = player.m_21205_();
         int activeSlot = player.m_150109_().f_35977_;
         return itemStack != null && itemStack.m_41720_() instanceof GunItem ? getState(player, itemStack, activeSlot, false) : null;
      }
   }

   public static GunClientState getState(Player player, ItemStack itemStack, int slotIndex, boolean isOffhand) {
      Level level = MiscUtil.getLevel(player);
      UUID stackId = GunItem.getItemStackId(itemStack);
      if (stackId == null) {
         return null;
      } else if (slotIndex == -1) {
         GunClientState state = null;
         Iterator var12 = localSlotStates.entrySet().iterator();

         while(var12.hasNext()) {
            Entry<PlayerSlot, GunClientState> entry = (Entry)var12.next();
            if (((PlayerSlot)entry.getKey()).isClientSide == level.f_46443_ && Objects.equals(((GunClientState)entry.getValue()).getId(), stackId)) {
               PlayerSlot playerSlot = (PlayerSlot)entry.getKey();
               slotIndex = playerSlot.slotId;
               if (slotIndex >= 0 && playerSlot.playerEntityId == player.m_19879_()) {
                  ItemStack stackAtSlotIndex = player.m_150109_().m_8020_(slotIndex);
                  if (stackAtSlotIndex != null && stackAtSlotIndex.m_41720_() instanceof GunItem) {
                     state = (GunClientState)entry.getValue();
                     break;
                  }
               }
            }
         }

         if (state == null) {
            Map var10000 = noSlotStates;
            GunItem var10002 = (GunItem)itemStack.m_41720_();
            Objects.requireNonNull(var10002);
            state = (GunClientState)var10000.computeIfAbsent(stackId, var10002::createState);
         }

         return state;
      } else {
         int adjustedSlotIndex = isOffhand ? -5 : slotIndex;
         PlayerSlot playerSlot = new PlayerSlot(player.m_19879_(), adjustedSlotIndex, level.f_46443_);
         GunClientState slotState = (GunClientState)localSlotStates.get(playerSlot);
         if (slotState == null || !Objects.equals(slotState.getId(), stackId)) {
            slotState = ((GunItem)itemStack.m_41720_()).createState(stackId);
            localSlotStates.put(playerSlot, slotState);
         }

         return slotState;
      }
   }

   public static GunClientState getState(UUID stateId) {
      return (GunClientState)statesById.get(stateId);
   }

   public void addMuzzleEffect(MuzzleFlashEffect muzzleFlashEffect) {
      this.muzzleFlashEffects.add(muzzleFlashEffect);
   }

   public List<MuzzleFlashEffect> getMuzzleFlashEffects() {
      return Collections.unmodifiableList(this.muzzleFlashEffects);
   }

   public void setSimplifiedState(FireState simplifiedFireState) {
      this.simplifiedFireState = simplifiedFireState;
   }

   public static enum FireState {
      PREPARE_IDLE,
      IDLE,
      IDLE_COOLDOWN,
      DRAW,
      DRAW_COOLDOWN,
      CHANGE_FIRE_MODE,
      CHANGE_FIRE_MODE_COOLDOWN,
      PREPARE_FIRE_SINGLE,
      PREPARE_FIRE_COOLDOWN_SINGLE,
      PREPARE_FIRE_AUTO,
      PREPARE_FIRE_COOLDOWN_AUTO,
      PREPARE_FIRE_BURST,
      PREPARE_FIRE_COOLDOWN_BURST,
      FIRE_SINGLE,
      FIRE_COOLDOWN_SINGLE,
      FIRE_AUTO,
      FIRE_COOLDOWN_AUTO,
      FIRE_BURST,
      FIRE_COOLDOWN_BURST,
      COMPLETE_FIRE,
      COMPLETE_FIRE_COOLDOWN,
      PREPARE_RELOAD,
      PREPARE_RELOAD_COOLDOWN,
      PREPARE_RELOAD_ITER,
      PREPARE_RELOAD_COOLDOWN_ITER,
      RELOAD,
      RELOAD_COOLDOWN,
      RELOAD_ITER,
      RELOAD_COOLDOWN_ITER,
      COMPLETE_RELOAD,
      COMPLETE_RELOAD_COOLDOWN,
      INSPECT,
      INSPECT_COOLDOWN;

      public static FireState getSimplifiedFireState(FireState fireState) {
         FireState simplifiedFireState;
         switch(fireState) {
         case DRAW:
         case DRAW_COOLDOWN:
            simplifiedFireState = DRAW;
            break;
         case PREPARE_FIRE_SINGLE:
         case PREPARE_FIRE_COOLDOWN_SINGLE:
         case PREPARE_FIRE_AUTO:
         case PREPARE_FIRE_COOLDOWN_AUTO:
         case PREPARE_FIRE_BURST:
         case PREPARE_FIRE_COOLDOWN_BURST:
         case FIRE_SINGLE:
         case FIRE_COOLDOWN_SINGLE:
         case FIRE_AUTO:
         case FIRE_COOLDOWN_AUTO:
         case FIRE_BURST:
         case FIRE_COOLDOWN_BURST:
         case COMPLETE_FIRE:
         case COMPLETE_FIRE_COOLDOWN:
            simplifiedFireState = FIRE_SINGLE;
            break;
         case PREPARE_RELOAD:
         case PREPARE_RELOAD_COOLDOWN:
         case PREPARE_RELOAD_ITER:
         case PREPARE_RELOAD_COOLDOWN_ITER:
         case RELOAD:
         case RELOAD_COOLDOWN:
         case RELOAD_ITER:
         case RELOAD_COOLDOWN_ITER:
         case COMPLETE_RELOAD:
         case COMPLETE_RELOAD_COOLDOWN:
            simplifiedFireState = RELOAD;
            break;
         case INSPECT:
         case INSPECT_COOLDOWN:
            simplifiedFireState = INSPECT;
            break;
         default:
            simplifiedFireState = IDLE;
         }

         return simplifiedFireState;
      }

      // $FF: synthetic method
      private static FireState[] $values() {
         return new FireState[]{PREPARE_IDLE, IDLE, IDLE_COOLDOWN, DRAW, DRAW_COOLDOWN, CHANGE_FIRE_MODE, CHANGE_FIRE_MODE_COOLDOWN, PREPARE_FIRE_SINGLE, PREPARE_FIRE_COOLDOWN_SINGLE, PREPARE_FIRE_AUTO, PREPARE_FIRE_COOLDOWN_AUTO, PREPARE_FIRE_BURST, PREPARE_FIRE_COOLDOWN_BURST, FIRE_SINGLE, FIRE_COOLDOWN_SINGLE, FIRE_AUTO, FIRE_COOLDOWN_AUTO, FIRE_BURST, FIRE_COOLDOWN_BURST, COMPLETE_FIRE, COMPLETE_FIRE_COOLDOWN, PREPARE_RELOAD, PREPARE_RELOAD_COOLDOWN, PREPARE_RELOAD_ITER, PREPARE_RELOAD_COOLDOWN_ITER, RELOAD, RELOAD_COOLDOWN, RELOAD_ITER, RELOAD_COOLDOWN_ITER, COMPLETE_RELOAD, COMPLETE_RELOAD_COOLDOWN, INSPECT, INSPECT_COOLDOWN};
      }
   }

   private class Context {
      LivingEntity player;
      ItemStack itemStack;
      Entity targetEntity;

      public Context(LivingEntity player, ItemStack itemStack) {
         this.player = player;
         this.itemStack = itemStack;
      }

      public Context(LivingEntity player, ItemStack itemStack, Entity targetEntity) {
         this.player = player;
         this.itemStack = itemStack;
         this.targetEntity = targetEntity;
      }

      public FireModeInstance getFireModeInstance() {
         return GunItem.getFireModeInstance(this.itemStack);
      }
   }

   private static class PlayerSlot {
      int playerEntityId;
      int slotId;
      boolean isClientSide;

      PlayerSlot(int playerEntityId, int slotId, boolean isClientSide) {
         this.playerEntityId = playerEntityId;
         this.slotId = slotId;
         this.isClientSide = isClientSide;
      }

      public int hashCode() {
         return Objects.hash(new Object[]{this.isClientSide, this.playerEntityId, this.slotId});
      }

      public boolean equals(Object obj) {
         if (this == obj) {
            return true;
         } else if (obj == null) {
            return false;
         } else if (this.getClass() != obj.getClass()) {
            return false;
         } else {
            PlayerSlot other = (PlayerSlot)obj;
            return this.isClientSide == other.isClientSide && this.playerEntityId == other.playerEntityId && this.slotId == other.slotId;
         }
      }
   }
}
