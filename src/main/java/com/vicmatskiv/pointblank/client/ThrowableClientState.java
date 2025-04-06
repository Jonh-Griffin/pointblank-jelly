package com.vicmatskiv.pointblank.client;

import com.vicmatskiv.pointblank.item.Drawable;
import com.vicmatskiv.pointblank.item.GunItem;
import com.vicmatskiv.pointblank.item.ThrowableItem;
import com.vicmatskiv.pointblank.item.ThrowableLike;
import com.vicmatskiv.pointblank.util.ClientUtil;
import com.vicmatskiv.pointblank.util.MiscUtil;
import com.vicmatskiv.pointblank.util.StateMachine;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.Map.Entry;
import java.util.function.Predicate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ThrowableClientState {
   private static final Logger LOGGER = LogManager.getLogger("pointblank");
   private static final Map<PlayerSlot, ThrowableClientState> localSlotStates = new HashMap();
   private static final Map<UUID, ThrowableClientState> noSlotStates = new WeakHashMap();
   private UUID id;
   private ThrowableLike item;
   private long prepareThrowCooldownStartTime;
   private long completeThrowCooldownStartTime;
   private long drawCooldownDuration;
   private long drawCooldownStartTime;
   private long inspectCooldownStartTime;
   private long inspectCooldownDuration;
   private long throwCooldownDuration;
   private long prepareIdleCooldownDuration;
   private long prepareIdleCooldownStartTime;
   private long idleCooldownStartTime;
   private long idleCooldownDuration;
   protected long throwCooldownStartTime;
   protected long lastThrowTime;
   protected boolean isTriggerOn;
   private Predicate<Context> isValidGameMode = (context) -> {
      return context.player != null && !context.player.m_5833_();
   };
   private State simplifiedThrowState;
   private StateMachine<State, Context> stateMachine;
   private static final Map<UUID, ThrowableClientState> statesById = new HashMap();

   public ThrowableClientState(UUID id, ThrowableLike item) {
      this.id = id;
      this.item = item;
      this.stateMachine = this.createStateMachine();
      statesById.put(id, this);
   }

   private StateMachine<State, Context> createStateMachine() {
      StateMachine.Builder<State, Context> builder = new StateMachine.Builder();
      builder.withTransition((List)List.of(State.PREPARE_IDLE, State.IDLE, State.IDLE_COOLDOWN), State.DRAW, this.isValidGameMode, StateMachine.TransitionMode.EVENT, (StateMachine.Action)null, this::actionDraw);
      builder.withTransition((Enum) State.DRAW, State.DRAW_COOLDOWN, (context) -> {
         return true;
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, (ctx, f, t) -> {
         this.drawCooldownStartTime = System.nanoTime();
         this.drawCooldownDuration = this.item.getDrawCooldownDuration(ctx.player, this, ctx.itemStack) * 1000000L;
      });
      builder.withTransition(State.DRAW_COOLDOWN, State.PREPARE_IDLE, Predicate.not(this::isDrawCooldownInProgress));
      builder.withTransition((Enum) State.PREPARE_IDLE, State.IDLE, (ctx) -> {
         return !this.isPrepareIdleCooldownInProgress(ctx) && this.item.hasIdleAnimations();
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, (StateMachine.Action)null);
      builder.withTransition((Enum) State.IDLE, State.IDLE_COOLDOWN, (ctx) -> {
         return this.item.hasIdleAnimations();
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, (ctx, f, t) -> {
         this.idleCooldownStartTime = System.nanoTime();
         this.idleCooldownDuration = this.item.getIdleCooldownDuration(ctx.player, this, ctx.itemStack) * 1000000L;
      });
      builder.withTransition(State.IDLE_COOLDOWN, State.IDLE, Predicate.not(this::isIdleCooldownInProgress));
      builder.withTransition((List)List.of(State.IDLE, State.IDLE_COOLDOWN), State.PREPARE_IDLE, (ctx) -> {
         return true;
      }, StateMachine.TransitionMode.EVENT, (StateMachine.Action)null, (StateMachine.Action)null);
      builder.withTransition((List)List.of(State.PREPARE_IDLE, State.IDLE, State.IDLE_COOLDOWN), State.INSPECT, this.isValidGameMode, StateMachine.TransitionMode.EVENT, (StateMachine.Action)null, this::actionInspect);
      builder.withTransition((Enum) State.INSPECT, State.INSPECT_COOLDOWN, (context) -> {
         return true;
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, (ctx, f, t) -> {
         this.inspectCooldownStartTime = System.nanoTime();
         this.inspectCooldownDuration = this.item.getInspectCooldownDuration(ctx.player, this, ctx.itemStack) * 1000000L;
      });
      builder.withTransition(State.INSPECT_COOLDOWN, State.PREPARE_IDLE, Predicate.not(this::inspectCooldownInProgress));
      builder.withTransition((Enum) State.PREPARE_IDLE, State.PREPARE_THROW, this.isValidGameMode.and((context) -> {
         return this.isTriggerOn && context.itemStack == ((Player)context.player).m_21205_();
      }), StateMachine.TransitionMode.EVENT, (StateMachine.Action)null, this::actionPrepareThrow);
      builder.withTransition((List)List.of(State.PREPARE_IDLE, State.IDLE, State.IDLE_COOLDOWN), State.PREPARE_THROW, this.isValidGameMode.and((context) -> {
         return this.isTriggerOn && context.itemStack == ((Player)context.player).m_21205_();
      }), StateMachine.TransitionMode.EVENT, (StateMachine.Action)null, this::actionPrepareThrow);
      builder.withTransition((List)List.of(State.PREPARE_IDLE, State.IDLE, State.IDLE_COOLDOWN), State.PREPARE_THROW, this.isValidGameMode.and((context) -> {
         return this.isTriggerOn && context.itemStack == ((Player)context.player).m_21205_();
      }), StateMachine.TransitionMode.EVENT, (StateMachine.Action)null, this::actionPrepareThrow);
      builder.withTransition((Enum) State.INSPECT_COOLDOWN, State.PREPARE_THROW, (context) -> {
         return this.isTriggerOn && context.itemStack == ((Player)context.player).m_21205_();
      }, StateMachine.TransitionMode.EVENT, (StateMachine.Action)null, this::actionPrepareThrow);
      builder.withTransition((Enum) State.PREPARE_THROW, State.PREPARE_THROW_COOLDOWN, (context) -> {
         return true;
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, (ctx, f, t) -> {
         this.prepareThrowCooldownStartTime = System.nanoTime();
      });
      builder.withTransition((Enum) State.PREPARE_THROW_COOLDOWN, State.THROW, (context) -> {
         return !this.isPrepareThrowCooldownInProgress(context);
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, this::actionThrow);
      builder.withTransition((Enum) State.THROW, State.THROW_COOLDOWN, (context) -> {
         return true;
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, (ctx, f, t) -> {
         this.throwCooldownStartTime = System.nanoTime();
         this.throwCooldownDuration = this.item.getThrowCooldownDuration(ctx.player, this, ctx.itemStack) * 1000000L;
      });
      builder.withTransition((Enum) State.THROW_COOLDOWN, State.COMPLETE_THROW, Predicate.not(this::isThrowCooldownInProgress), StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, this::actionCompleteThrow);
      builder.withTransition((Enum) State.COMPLETE_THROW, State.COMPLETE_THROW_COOLDOWN, (context) -> {
         return true;
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, (ctx, f, t) -> {
         this.completeThrowCooldownStartTime = System.nanoTime();
      });
      builder.withTransition((Enum) State.COMPLETE_THROW_COOLDOWN, State.PREPARE_IDLE, Predicate.not(this::isCompleteFireCooldownInProgress), StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, (StateMachine.Action)null);
      builder.withOnSetStateAction(State.PREPARE_IDLE, (ctx, f, t) -> {
         this.actionPrepareIdle(ctx);
      });
      builder.withOnSetStateAction(State.IDLE, (ctx, f, t) -> {
         this.actionFiddle(ctx);
      });
      builder.withOnChangeStateAction((ctx, f, t) -> {
         LOGGER.debug("Throwable state changed from {} to {}", f, t);
      });
      return builder.build(State.PREPARE_IDLE);
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

   private boolean isCompleteFireCooldownInProgress(Context context) {
      return (double)(System.nanoTime() - this.completeThrowCooldownStartTime) <= 1000000.0D * (double)this.item.getCompleteThrowCooldownDuration(context.player, this, context.itemStack);
   }

   private boolean isThrowCooldownInProgress(Context context) {
      return System.nanoTime() - this.throwCooldownStartTime <= this.throwCooldownDuration;
   }

   private boolean isPrepareThrowCooldownInProgress(Context context) {
      return (double)(System.nanoTime() - this.prepareThrowCooldownStartTime) <= 1000000.0D * (double)this.item.getPrepareThrowCooldownDuration(context.player, this, context.itemStack);
   }

   private void actionPrepareThrow(Context context, State fromState, State toState) {
      this.item.prepareThrow(this, (Player)context.player, context.itemStack, context.targetEntity);
   }

   private void actionThrow(Context context, State fromState, State toState) {
      this.item.requestThrowFromServer(this, (Player)context.player, context.itemStack, context.targetEntity);
      this.lastThrowTime = MiscUtil.getLevel(context.player).m_46467_();
   }

   private void actionDraw(Context context, State fromState, State toState) {
      ThrowableLike var5 = this.item;
      if (var5 instanceof Drawable) {
         Drawable drawable = (Drawable)var5;
         drawable.draw((Player)context.player, context.itemStack);
      }

   }

   private void actionInspect(Context context, State fromState, State toState) {
   }

   private void actionCompleteThrow(Context context, State fromState, State toState) {
      LOGGER.debug("{} Completing firing in state {}", System.currentTimeMillis() % 100000L, toState);
   }

   private void actionPrepareIdle(Context context) {
      this.prepareIdleCooldownStartTime = System.nanoTime();
      this.prepareIdleCooldownDuration = this.item.getPrepareIdleCooldownDuration();
   }

   private void actionFiddle(Context context) {
   }

   public boolean tryThrow(LivingEntity player, ItemStack itemStack, Entity targetEntity) {
      Context context = new Context(player, itemStack, targetEntity);
      return this.stateMachine.setStateToAnyOf(context, List.of(State.PREPARE_THROW)) != null;
   }

   public boolean tryDraw(LivingEntity player, ItemStack itemStack) {
      Context context = new Context(player, itemStack);
      return this.stateMachine.setState(context, (Enum) State.DRAW) != null;
   }

   public boolean tryDeactivate(LivingEntity player, ItemStack itemStack) {
      Context context = new Context(player, itemStack);
      return this.stateMachine.setState(context, (Enum) State.PREPARE_IDLE) != null;
   }

   public boolean tryInspect(LivingEntity player, ItemStack itemStack) {
      Context context = new Context(player, itemStack);
      return this.stateMachine.setState(context, (Enum) State.INSPECT) != null;
   }

   public UUID getId() {
      return this.id;
   }

   public void setTrigger(boolean isTriggerOn) {
      this.isTriggerOn = isTriggerOn;
   }

   public boolean isIdle() {
      State fireState = (State)this.stateMachine.getCurrentState();
      return this.simplifiedThrowState == State.IDLE || fireState == State.PREPARE_IDLE || fireState == State.IDLE || fireState == State.IDLE_COOLDOWN;
   }

   public boolean isThrowing() {
      State fireState = (State)this.stateMachine.getCurrentState();
      return isThrowing(fireState) || this.simplifiedThrowState == State.THROW;
   }

   private static boolean isThrowing(State fireState) {
      return fireState == State.PREPARE_THROW || fireState == State.PREPARE_THROW_COOLDOWN || fireState == State.THROW || fireState == State.THROW_COOLDOWN || fireState == State.COMPLETE_THROW || fireState == State.COMPLETE_THROW_COOLDOWN;
   }

   public boolean isPreparingThrowing() {
      State fireState = (State)this.stateMachine.getCurrentState();
      return fireState == State.PREPARE_THROW || fireState == State.PREPARE_THROW_COOLDOWN;
   }

   public boolean isCompletingThrowing() {
      State state = (State)this.stateMachine.getCurrentState();
      return state == State.COMPLETE_THROW || state == State.COMPLETE_THROW_COOLDOWN;
   }

   public boolean isDrawing() {
      State state = (State)this.stateMachine.getCurrentState();
      return state == State.DRAW || state == State.DRAW_COOLDOWN;
   }

   public boolean isInspecting() {
      State state = (State)this.stateMachine.getCurrentState();
      return state == State.INSPECT || state == State.INSPECT_COOLDOWN;
   }

   public void updateState(LivingEntity player, ItemStack itemStack, boolean isSelected) {
      if (player == ClientUtil.getClientPlayer()) {
         Context context = new Context(player, itemStack);
         this.stateMachine.update(context);
         State updatedSimplifiedFireState = State.getSimplifiedState((State)this.stateMachine.getCurrentState());
         if (updatedSimplifiedFireState != this.simplifiedThrowState) {
            this.simplifiedThrowState = updatedSimplifiedFireState;
         }

      }
   }

   public String toString() {
      return String.format("ThrowableClientState[sid=%s,id=%s]", System.identityHashCode(this), this.id);
   }

   public static ThrowableClientState getMainHeldState() {
      Player player = ClientUtil.getClientPlayer();
      return getMainHeldState(player);
   }

   public static ThrowableClientState getMainHeldState(Player player) {
      if (player == null) {
         return null;
      } else {
         ItemStack itemStack = player.m_21205_();
         int activeSlot = player.m_150109_().f_35977_;
         return itemStack != null && itemStack.m_41720_() instanceof ThrowableItem ? getState(player, itemStack, activeSlot, false) : null;
      }
   }

   public static ThrowableClientState getState(Player player, ItemStack itemStack, int slotIndex, boolean isOffhand) {
      Level level = MiscUtil.getLevel(player);
      UUID stackId = GunItem.getItemStackId(itemStack);
      if (stackId == null) {
         return null;
      } else if (slotIndex == -1) {
         ThrowableClientState state = null;
         Iterator var12 = localSlotStates.entrySet().iterator();

         while(var12.hasNext()) {
            Entry<PlayerSlot, ThrowableClientState> entry = (Entry)var12.next();
            if (((PlayerSlot)entry.getKey()).isClientSide == level.f_46443_ && Objects.equals(((ThrowableClientState)entry.getValue()).getId(), stackId)) {
               PlayerSlot playerSlot = (PlayerSlot)entry.getKey();
               slotIndex = playerSlot.slotId;
               if (slotIndex >= 0 && playerSlot.playerEntityId == player.m_19879_()) {
                  ItemStack stackAtSlotIndex = player.m_150109_().m_8020_(slotIndex);
                  if (stackAtSlotIndex != null && stackAtSlotIndex.m_41720_() instanceof ThrowableLike) {
                     state = (ThrowableClientState)entry.getValue();
                     break;
                  }
               }
            }
         }

         if (state == null) {
            Map var10000 = noSlotStates;
            ThrowableLike var10002 = (ThrowableLike)itemStack.m_41720_();
            Objects.requireNonNull(var10002);
            state = (ThrowableClientState)var10000.computeIfAbsent(stackId, var10002::createState);
         }

         return state;
      } else {
         int adjustedSlotIndex = isOffhand ? -5 : slotIndex;
         PlayerSlot playerSlot = new PlayerSlot(player.m_19879_(), adjustedSlotIndex, level.f_46443_);
         ThrowableClientState slotState = (ThrowableClientState)localSlotStates.get(playerSlot);
         if (slotState == null || !Objects.equals(slotState.getId(), stackId)) {
            slotState = ((ThrowableLike)itemStack.m_41720_()).createState(stackId);
            localSlotStates.put(playerSlot, slotState);
         }

         return slotState;
      }
   }

   public static ThrowableClientState getState(UUID stateId) {
      return (ThrowableClientState)statesById.get(stateId);
   }

   public void setSimplifiedState(State state) {
      this.simplifiedThrowState = state;
   }

   public static enum State {
      PREPARE_IDLE,
      IDLE,
      IDLE_COOLDOWN,
      DRAW,
      DRAW_COOLDOWN,
      PREPARE_THROW,
      PREPARE_THROW_COOLDOWN,
      THROW,
      THROW_COOLDOWN,
      COMPLETE_THROW,
      COMPLETE_THROW_COOLDOWN,
      INSPECT,
      INSPECT_COOLDOWN;

      public static State getSimplifiedState(State state) {
         State var10000;
         switch(state) {
         case DRAW:
         case DRAW_COOLDOWN:
            var10000 = DRAW;
            break;
         case PREPARE_THROW:
         case PREPARE_THROW_COOLDOWN:
         case THROW:
         case THROW_COOLDOWN:
         case COMPLETE_THROW:
         case COMPLETE_THROW_COOLDOWN:
            var10000 = THROW;
            break;
         case INSPECT:
         case INSPECT_COOLDOWN:
            var10000 = INSPECT;
            break;
         default:
            var10000 = IDLE;
         }

         return var10000;
      }

      // $FF: synthetic method
      private static State[] $values() {
         return new State[]{PREPARE_IDLE, IDLE, IDLE_COOLDOWN, DRAW, DRAW_COOLDOWN, PREPARE_THROW, PREPARE_THROW_COOLDOWN, THROW, THROW_COOLDOWN, COMPLETE_THROW, COMPLETE_THROW_COOLDOWN, INSPECT, INSPECT_COOLDOWN};
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
