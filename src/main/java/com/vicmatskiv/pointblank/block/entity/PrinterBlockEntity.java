package com.vicmatskiv.pointblank.block.entity;

import com.vicmatskiv.pointblank.Enableable;
import com.vicmatskiv.pointblank.crafting.Craftable;
import com.vicmatskiv.pointblank.crafting.PointBlankRecipe;
import com.vicmatskiv.pointblank.inventory.CraftingContainerMenu;
import com.vicmatskiv.pointblank.registry.BlockEntityRegistry;
import com.vicmatskiv.pointblank.registry.SoundRegistry;
import com.vicmatskiv.pointblank.util.InventoryUtils;
import com.vicmatskiv.pointblank.util.MiscUtil;
import com.vicmatskiv.pointblank.util.StateMachine;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.core.keyframe.event.data.SoundKeyframeData;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.ClientUtils;
import software.bernie.geckolib.util.GeckoLibUtil;

public class PrinterBlockEntity extends BlockEntity implements GeoBlockEntity, MenuProvider {
   private static final int OPENING_DURATION = 655;
   private static final int CLOSING_DURATION = 577;
   protected final ContainerData dataAccess = new ContainerData() {
      public int m_6413_(int dataSlotIndex) {
         switch(dataSlotIndex) {
         case 0:
            return PrinterBlockEntity.this.getState().ordinal();
         case 1:
            return PrinterBlockEntity.this.craftingPlayer != null ? PrinterBlockEntity.this.craftingPlayer.m_19879_() : -1;
         default:
            return 0;
         }
      }

      public void m_8050_(int dataSlotIndex, int value) {
         switch(dataSlotIndex) {
         default:
         }
      }

      public int m_6499_() {
         return 2;
      }
   };
   private static final Component CONTAINER_TITLE = Component.m_237115_("screen.pointblank.crafting");
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private List<Player> nearbyEntities;
   private long lastNearbyEntityUpdateTimestamp;
   private Player craftingPlayer;
   private PointBlankRecipe craftingRecipe;
   private long craftingStartTime;
   private long craftingDuration;
   private long openingDuration = 655L;
   private long closingStartTime;
   private long closingDuration = 577L;
   private long openingStartTime;
   private CraftingEventHandler craftingEventHandler;
   private StateMachine<State, Context> stateMachine;
   private State clientState;
   private static final RawAnimation ANIMATION_OPEN = RawAnimation.begin().thenPlay("animation.model.open").thenLoop("animation.model.idle");
   private static final RawAnimation ANIMATION_CLOSE = RawAnimation.begin().thenPlay("animation.model.close");
   private static final RawAnimation ANIMATION_IDLE = RawAnimation.begin().thenPlay("animation.model.idle");
   private static final RawAnimation ANIMATION_CRAFTING = RawAnimation.begin().thenPlay("animation.model.crafting").thenLoop("animation.model.idle");

   public PrinterBlockEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType)BlockEntityRegistry.PRINTER_BLOCK_ENTITY.get(), pos, state);
   }

   public void m_142339_(Level level) {
      super.m_142339_(level);
      if (level.f_46443_) {
         this.clientState = State.CLOSED;
      } else {
         this.stateMachine = this.createStateMachine();
      }

   }

   public State getState() {
      if (this.f_58857_ == null) {
         return null;
      } else {
         return this.f_58857_.f_46443_ ? this.clientState : (State)this.stateMachine.getCurrentState();
      }
   }

   private StateMachine<State, Context> createStateMachine() {
      StateMachine.Builder<State, Context> builder = new StateMachine.Builder();
      builder.withTransition((Enum) State.CLOSED, State.OPENING, this::predicateIsPlayerNearby, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, this::actionOpen);
      builder.withTransition((Enum) State.OPENING, State.IDLE, this::openingTimeoutExpired, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, (StateMachine.Action)null);
      builder.withTransition((Enum) State.IDLE, State.CRAFTING, this::predicateIsPlayerNearby, StateMachine.TransitionMode.EVENT, (StateMachine.Action)null, this::actionStartCrafting);
      builder.withTransition((Enum) State.CRAFTING, State.IDLE, this::predicateIsPlayerNearby, StateMachine.TransitionMode.EVENT, (StateMachine.Action)null, (StateMachine.Action)null);
      builder.withTransition((Enum) State.CRAFTING, State.CRAFTING_COMPLETED, (ctx) -> {
         return this.predicateIsPlayerNearby(ctx) && this.craftingTimeoutExpired(ctx);
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, this::actionCompleteCrafting);
      builder.withTransition((Enum) State.CRAFTING, State.CLOSING, (ctx) -> {
         return !this.predicateIsPlayerNearby(ctx);
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, this::actionCancelCrafting);
      builder.withTransition((Enum) State.CRAFTING_COMPLETED, State.IDLE, (ctx) -> {
         return true;
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, (StateMachine.Action)null);
      builder.withTransition((Enum) State.IDLE, State.CLOSING, (ctx) -> {
         return !this.predicateIsPlayerNearby(ctx);
      }, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, (StateMachine.Action)null);
      builder.withTransition((Enum) State.CLOSING, State.CLOSED, this::closingTimeoutExpired, StateMachine.TransitionMode.AUTO, (StateMachine.Action)null, (StateMachine.Action)null);
      builder.withOnSetStateAction(State.IDLE, this::actionIdle);
      builder.withOnChangeStateAction(this::actionOnChangeState);
      return builder.build(State.CLOSED);
   }

   private boolean closingTimeoutExpired(Context ctx) {
      return System.currentTimeMillis() - this.closingStartTime >= this.closingDuration;
   }

   private boolean craftingTimeoutExpired(Context ctx) {
      return System.currentTimeMillis() - this.craftingStartTime >= this.craftingDuration;
   }

   private boolean openingTimeoutExpired(Context ctx) {
      return System.currentTimeMillis() - this.openingStartTime >= this.openingDuration;
   }

   private boolean predicateIsPlayerNearby(Context context) {
      return isPlayerNearby(this.m_58899_(), this.nearbyEntities);
   }

   private void actionStartCrafting(Context context, State fromState, State toState) {
      this.craftingPlayer = context.craftingPlayer;
      this.craftingRecipe = context.craftingRecipe;
      this.craftingStartTime = System.currentTimeMillis();
      this.craftingDuration = ((Craftable)context.craftingRecipe.m_8043_((RegistryAccess)null).m_41720_()).getCraftingDuration();
      this.craftingEventHandler = context.craftingEventHandler;
   }

   private void actionIdle(Context context, State fromState, State toState) {
      this.resetCrafting();
   }

   private void actionOpen(Context context, State fromState, State toState) {
      this.openingStartTime = System.currentTimeMillis();
   }

   private void actionCompleteCrafting(Context context, State fromState, State toState) {
      this.createCraftingItem();
   }

   private void actionCancelCrafting(Context context, State fromState, State toState) {
      this.cancelCrafting(context.craftingPlayer, context.craftingRecipe.m_6423_());
   }

   private void actionOnChangeState(Context context, State fromState, State toState) {
      this.f_58857_.m_7260_(this.m_58899_(), this.m_58900_(), this.m_58900_(), 3);
   }

   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(new AnimationController[]{(new AnimationController(this, (state) -> {
         PlayState playState = null;
         switch(this.getState()) {
         case CLOSED:
            playState = state.setAndContinue(ANIMATION_CLOSE);
            break;
         case OPENING:
            playState = state.setAndContinue(ANIMATION_OPEN);
            break;
         case IDLE:
            playState = state.setAndContinue(ANIMATION_IDLE);
            break;
         case CRAFTING:
            playState = state.setAndContinue(ANIMATION_CRAFTING);
            break;
         case CRAFTING_COMPLETED:
            playState = state.setAndContinue(ANIMATION_IDLE);
            break;
         case CLOSING:
            playState = state.setAndContinue(ANIMATION_CLOSE);
         }

         return playState;
      })).setSoundKeyframeHandler((event) -> {
         Player player = ClientUtils.getClientPlayer();
         if (player != null) {
            SoundKeyframeData soundKeyframeData = event.getKeyframeData();
            String soundName = soundKeyframeData.getSound();
            SoundEvent soundEvent = SoundRegistry.getSoundEvent(soundName);
            if (soundEvent != null) {
               BlockPos blockPos = this.m_58899_();
               this.f_58857_.m_7785_((double)blockPos.m_123341_(), (double)blockPos.m_123342_(), (double)blockPos.m_123343_(), soundEvent, SoundSource.BLOCKS, 2.0F, (1.0F + (this.f_58857_.f_46441_.m_188501_() - this.f_58857_.f_46441_.m_188501_()) * 0.2F) * 0.7F, false);
            }
         }

      })});
   }

   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }

   public static void clientTick(Level level, BlockPos blockPos, BlockState blockState, PrinterBlockEntity entity) {
   }

   private void updateEntities() {
      BlockPos blockpos = this.m_58899_();
      if (this.f_58857_.m_46467_() > this.lastNearbyEntityUpdateTimestamp + 50L || this.nearbyEntities == null) {
         this.lastNearbyEntityUpdateTimestamp = this.f_58857_.m_46467_();
         AABB aabb = (new AABB(blockpos)).m_82400_(15.0D);
         this.nearbyEntities = this.f_58857_.m_45976_(Player.class, aabb);
      }

   }

   private static boolean isPlayerNearby(BlockPos blockPos, List<Player> entities) {
      if (entities == null) {
         return false;
      } else {
         Iterator var2 = entities.iterator();

         LivingEntity entity;
         do {
            if (!var2.hasNext()) {
               return false;
            }

            entity = (LivingEntity)var2.next();
         } while(!(entity instanceof Player) || !entity.m_6084_() || entity.m_213877_() || !blockPos.m_203195_(entity.m_20182_(), 6.0D));

         return true;
      }
   }

   public AbstractContainerMenu m_7208_(int containerId, Inventory inventory, Player player) {
      return new CraftingContainerMenu(containerId, inventory, this, this.dataAccess);
   }

   public Component m_5446_() {
      return CONTAINER_TITLE;
   }

   public static void serverTick(Level level, BlockPos blockPos, BlockState blockState, PrinterBlockEntity entity) {
      entity.serverTick();
   }

   private void serverTick() {
      this.updateEntities();
      Context context = new Context(this.craftingPlayer, this.craftingRecipe, this.craftingEventHandler);
      this.stateMachine.update(context);
   }

   private void createCraftingItem() {
      ItemStack craftedStack = null;
      boolean isCraftingSuccessful = false;
      boolean isAddedToInventory = false;
      Exception craftingException = null;

      try {
         if (this.craftingRecipe.canBeCrafted(this.craftingPlayer)) {
            craftedStack = this.craftingRecipe.m_8043_((RegistryAccess)null);
            if (craftedStack != null && !craftedStack.m_41619_()) {
               this.craftingRecipe.removeIngredients(this.craftingPlayer);
               int remaingCount = InventoryUtils.addItem(this.craftingPlayer, craftedStack.m_41720_(), craftedStack.m_41613_());
               if (remaingCount > 0) {
                  BlockPos blockPos = this.m_58899_();
                  if (blockPos != null) {
                     Containers.m_18992_(MiscUtil.getLevel(this.craftingPlayer), (double)blockPos.m_123341_(), (double)((float)blockPos.m_123342_() + 1.25F), (double)blockPos.m_123343_(), craftedStack.m_41777_());
                     isCraftingSuccessful = true;
                  }
               } else {
                  isAddedToInventory = true;
                  isCraftingSuccessful = true;
               }
            }
         }
      } catch (Exception var7) {
         craftingException = var7;
         System.err.println("Caught exception during crafting " + var7);
      }

      if (craftedStack == null) {
         craftedStack = ItemStack.f_41583_;
      }

      if (this.craftingEventHandler != null) {
         if (isCraftingSuccessful) {
            this.craftingEventHandler.onCraftingCompleted(this.craftingPlayer, craftedStack, isAddedToInventory);
         } else {
            this.craftingEventHandler.onCraftingFailed(this.craftingPlayer, craftedStack, craftingException);
         }
      }

   }

   private void resetCrafting() {
      this.craftingPlayer = null;
      this.craftingRecipe = null;
      this.craftingStartTime = 0L;
      this.craftingDuration = 0L;
      this.craftingEventHandler = null;
   }

   public boolean tryCrafting(Player player, ResourceLocation recipeId, CraftingEventHandler craftingEventHandler) {
      if (this.nearbyEntities != null && !this.nearbyEntities.contains(player)) {
         return false;
      } else {
         PointBlankRecipe craftingRecipe = PointBlankRecipe.getRecipe(MiscUtil.getLevel(player), recipeId);
         if (craftingRecipe == null) {
            return false;
         } else {
            ItemStack craftingItemStack = craftingRecipe.m_8043_((RegistryAccess)null);
            if (craftingItemStack != null && !craftingItemStack.m_41619_() && craftingItemStack.m_41720_() instanceof Craftable) {
               Item var7 = craftingItemStack.m_41720_();
               if (!(var7 instanceof Enableable)) {
                  return this.stateMachine.setState(new Context(player, craftingRecipe, craftingEventHandler), (Enum) State.CRAFTING) == State.CRAFTING;
               }

               Enableable e = (Enableable)var7;
               if (e.isEnabled()) {
                  return this.stateMachine.setState(new Context(player, craftingRecipe, craftingEventHandler), (Enum) State.CRAFTING) == State.CRAFTING;
               }
            }

            return false;
         }
      }
   }

   public boolean cancelCrafting(Player player, ResourceLocation recipeId) {
      if (this.stateMachine.getCurrentState() != State.CRAFTING) {
         return false;
      } else if (player != this.craftingPlayer) {
         return false;
      } else if (!recipeId.equals(this.craftingRecipe.m_6423_())) {
         return false;
      } else {
         this.stateMachine.setState(new Context(player, this.craftingRecipe, this.craftingEventHandler), (Enum) State.IDLE);
         if (this.craftingEventHandler != null) {
            this.craftingEventHandler.onCraftingCancelled(this.craftingPlayer, this.craftingRecipe.m_8043_((RegistryAccess)null));
         }

         return true;
      }
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      CompoundTag tag = new CompoundTag();
      tag.m_128405_("clientState", ((State)this.stateMachine.getCurrentState()).ordinal());
      return ClientboundBlockEntityDataPacket.m_195642_(this, (e) -> {
         return tag;
      });
   }

   public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
      CompoundTag tag = packet.m_131708_();
      if (tag != null) {
         int ordinal = tag.m_128451_("clientState");
         this.clientState = State.values()[ordinal];
      }

   }

   public static enum State {
      CLOSED,
      OPENING,
      CLOSING,
      IDLE,
      CRAFTING,
      CRAFTING_COMPLETED;

      // $FF: synthetic method
      private static State[] $values() {
         return new State[]{CLOSED, OPENING, CLOSING, IDLE, CRAFTING, CRAFTING_COMPLETED};
      }
   }

   private static class Context {
      private Player craftingPlayer;
      private PointBlankRecipe craftingRecipe;
      private CraftingEventHandler craftingEventHandler;

      public Context(Player craftingPlayer, PointBlankRecipe craftingRecipe, CraftingEventHandler craftingEventHandler) {
         this.craftingPlayer = craftingPlayer;
         this.craftingRecipe = craftingRecipe;
         this.craftingEventHandler = craftingEventHandler;
      }
   }

   public interface CraftingEventHandler {
      default void onCraftingCompleted(Player player, ItemStack craftingItemStack, boolean isAddedToInventory) {
      }

      default void onCraftingCancelled(Player player, ItemStack craftingItemStack) {
      }

      default void onCraftingFailed(Player player, ItemStack craftingItemStack, Exception craftingException) {
      }
   }
}
