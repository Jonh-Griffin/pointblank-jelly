package com.vicmatskiv.pointblank.client;

import com.mojang.blaze3d.platform.InputConstants.Type;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Either;
import com.mojang.math.Axis;
import com.vicmatskiv.pointblank.Config;
import com.vicmatskiv.pointblank.Enableable;
import com.vicmatskiv.pointblank.Nameable;
import com.vicmatskiv.pointblank.attachment.Attachment;
import com.vicmatskiv.pointblank.attachment.AttachmentCategory;
import com.vicmatskiv.pointblank.attachment.AttachmentHost;
import com.vicmatskiv.pointblank.attachment.AttachmentModelInfo;
import com.vicmatskiv.pointblank.attachment.Attachments;
import com.vicmatskiv.pointblank.client.controller.AbstractProceduralAnimationController;
import com.vicmatskiv.pointblank.client.controller.GunJumpAnimationController;
import com.vicmatskiv.pointblank.client.controller.GunRandomizingAnimationController;
import com.vicmatskiv.pointblank.client.controller.GunRecoilAnimationController;
import com.vicmatskiv.pointblank.client.controller.InertiaController;
import com.vicmatskiv.pointblank.client.controller.PostPassEffectController;
import com.vicmatskiv.pointblank.client.controller.PryAnimationController;
import com.vicmatskiv.pointblank.client.controller.TimerController;
import com.vicmatskiv.pointblank.client.controller.ViewShakeAnimationController2;
import com.vicmatskiv.pointblank.client.gui.AttachmentManagerScreen;
import com.vicmatskiv.pointblank.client.gui.CraftingScreen;
import com.vicmatskiv.pointblank.client.gui.GunItemOverlay;
import com.vicmatskiv.pointblank.client.model.BaseBlockModel;
import com.vicmatskiv.pointblank.client.particle.EffectParticles;
import com.vicmatskiv.pointblank.client.render.BaseModelBlockRenderer;
import com.vicmatskiv.pointblank.client.render.CrosshairRenderer;
import com.vicmatskiv.pointblank.client.render.RenderUtil;
import com.vicmatskiv.pointblank.compat.playeranimator.PlayerAnimatorCompat;
import com.vicmatskiv.pointblank.entity.EntityBuilder;
import com.vicmatskiv.pointblank.explosion.ExplosionEvent;
import com.vicmatskiv.pointblank.feature.AimingFeature;
import com.vicmatskiv.pointblank.feature.FeatureProvider;
import com.vicmatskiv.pointblank.item.ExplosionDescriptor;
import com.vicmatskiv.pointblank.item.GunItem;
import com.vicmatskiv.pointblank.item.ThrowableItem;
import com.vicmatskiv.pointblank.network.AimingChangeRequestPacket;
import com.vicmatskiv.pointblank.network.Network;
import com.vicmatskiv.pointblank.registry.BlockEntityRegistry;
import com.vicmatskiv.pointblank.registry.BlockModelRegistry;
import com.vicmatskiv.pointblank.registry.EntityRegistry;
import com.vicmatskiv.pointblank.registry.MenuRegistry;
import com.vicmatskiv.pointblank.registry.ParticleRegistry;
import com.vicmatskiv.pointblank.registry.ThirdPersonAnimationRegistry;
import com.vicmatskiv.pointblank.util.ClientUtil;
import com.vicmatskiv.pointblank.util.HitScan;
import com.vicmatskiv.pointblank.util.MiscUtil;
import com.vicmatskiv.pointblank.util.UpDownCounter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.minecraftforge.client.event.InputEvent.InteractionKeyMappingTriggered;
import net.minecraftforge.client.event.RenderLivingEvent.Post;
import net.minecraftforge.client.event.RenderLivingEvent.Pre;
import net.minecraftforge.client.event.RenderTooltipEvent.GatherComponents;
import net.minecraftforge.client.event.ViewportEvent.ComputeCameraAngles;
import net.minecraftforge.client.event.ViewportEvent.ComputeFov;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.TickEvent.RenderTickEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.minecraftforge.event.level.LevelEvent.Load;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Quaternionf;
import org.joml.Vector2ic;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.util.ClientUtils;

@OnlyIn(Dist.CLIENT)
public class ClientEventHandler {
   private static final Logger LOGGER = LogManager.getLogger("pointblank");
   public static final Lazy<KeyMapping> RELOAD_KEY = Lazy.of(() -> {
      return new KeyMapping("key.pointblack.reload", Type.KEYSYM, 82, "key.categories.pointblank");
   });
   public static final Lazy<KeyMapping> FIRE_MODE_KEY = Lazy.of(() -> {
      return new KeyMapping("key.pointblack.firemode", Type.KEYSYM, 66, "key.categories.pointblank");
   });
   public static final Lazy<KeyMapping> INSPECT_KEY = Lazy.of(() -> {
      return new KeyMapping("key.pointblack.inspect", Type.KEYSYM, 73, "key.categories.pointblank");
   });
   public static final Lazy<KeyMapping> ATTACHMENT_KEY = Lazy.of(() -> {
      return new KeyMapping("key.pointblack.attachments", Type.KEYSYM, 89, "key.categories.pointblank");
   });
   public static final Lazy<KeyMapping> SCOPE_SWITCH_KEY = Lazy.of(() -> {
      return new KeyMapping("key.pointblack.scope_switch", Type.KEYSYM, 86, "key.categories.pointblank");
   });
   private static ReentrantLock mainLoopLock = new ReentrantLock();
   private InertiaController scopeInertiaController = new InertiaController(0.06D, 0.2D, 0.1D);
   private InertiaController inertiaController = new InertiaController(0.01D, 0.1D, 1.2217304706573486D);
   public static InertiaController reticleInertiaController = new InertiaController(0.005D, 0.05D, 1.0D);
   private GunJumpAnimationController jumpController = new GunJumpAnimationController(0.3D, 0.8D, 1.3D, 0.05D, 2000L);
   private ViewShakeAnimationController2 sharedViewShakeController = new ViewShakeAnimationController2(0.15D, 0.3D, 1.0D, 0.01D, 500L);
   private int currentInventorySlot;
   private int previousInventorySlot;
   private boolean inventorySlotChanged;
   private boolean currentSlotHasGun;
   private boolean currentSlotHasGunChanged;
   private boolean leftMouseButtonDown = false;
   private boolean rightMouseButtonDown = false;
   private float previousPlayerXRot;
   private float previousPlayerYRot;
   private float playerDeltaXRot;
   private float playerDeltaYRot;
   private double previousPlayerPosX;
   private double previousPlayerPosY;
   private double previousPlayerPosZ;
   private double playerDeltaX;
   private double playerDeltaY;
   private double playerDeltaZ;
   private static LivingEntity currentEntityLiving;
   private RealtimeLinearEaser bobbingValue = new RealtimeLinearEaser(200L);
   private RealtimeLinearEaser bobbingYawValue = new RealtimeLinearEaser(200L);
   private RealtimeLinearEaser zoomValue = new RealtimeLinearEaser(200L);
   private RealtimeLinearEaser crossHairExp = new RealtimeLinearEaser(100L);
   private LockableTarget lockableTarget = new LockableTarget();
   private static PostPassEffectController postPassEffectController = new PostPassEffectController(2000L);
   private static final ResourceLocation crossHairOverlay = new ResourceLocation("pointblank", "textures/gui/crosshair.png");
   private final FirstPersonWalkingAnimationHandler firstPersonWalkingAnimationHandler = new FirstPersonWalkingAnimationHandler();

   public ClientEventHandler() {
      this.startTicker();
   }

   private void startTicker() {
      GunStateTicker gunStateTicker = new GunStateTicker(this);
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
         gunStateTicker.shutdown();
      }));
      gunStateTicker.start();
   }

   @SubscribeEvent
   public void onWorldLoad(Load event) {
      if (event.getLevel().m_5776_()) {
         this.currentInventorySlot = -1;
      }

   }

   @SubscribeEvent
   public void onExplosion(ExplosionEvent event) {
      double distanceToPlayer = event.getLocation().m_82554_(ClientUtils.getClientPlayer().m_20182_());
      ExplosionDescriptor descriptor = event.getExplosionDescriptor();
      distanceToPlayer = Mth.m_14008_(distanceToPlayer, 1.0D, Double.MAX_VALUE);
      double lambda = 0.2D;
      double adjustedPower = (double)descriptor.power() * Math.exp(-lambda * distanceToPlayer);
      this.sharedViewShakeController.reset(adjustedPower, 0.5D, 1.5D, 0.01D);
   }

   @SubscribeEvent
   public void onRenderTick(RenderTickEvent event) {
      Player player = ClientUtils.getClientPlayer();
      if (event.phase == Phase.START) {
         mainLoopLock.lock();
         if (player != null) {
            GunClientState state = GunClientState.getMainHeldState();
            if (state != null) {
               ItemStack gunStack = GunItem.getMainHeldGunItemStack(player);
               Minecraft mc = Minecraft.m_91087_();
               state.renderTick(player, gunStack, event.renderTickTime);
               if (!mc.f_91066_.m_92176_().m_90612_()) {
                  return;
               }

               GunItem gunItem = state.getGunItem();
               ItemDisplayContext itemDisplayContext = ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
               this.inertiaController.onRenderTick(player, state, gunStack, itemDisplayContext, event.renderTickTime);
               if (gunItem.getScopeOverlay() != null && state.isAiming() && state.isFiring()) {
                  this.scopeInertiaController.setDynamicModifier(0.01F);
               } else {
                  this.scopeInertiaController.setDynamicModifier(1.0F);
               }

               this.scopeInertiaController.onRenderTick(player, state, gunStack, itemDisplayContext, event.renderTickTime);
               this.jumpController.onRenderTick(player, state, gunStack, itemDisplayContext, event.renderTickTime);
               postPassEffectController.onRenderTick(player, state, gunStack, itemDisplayContext, event.renderTickTime);
               this.sharedViewShakeController.onRenderTick(player, state, gunStack, itemDisplayContext, event.renderTickTime);
            }
         }
      } else if (event.phase == Phase.END && mainLoopLock.isLocked()) {
         mainLoopLock.unlock();
      }

   }

   public static PostPassEffectController getPostPassEffectController() {
      return postPassEffectController;
   }

   private boolean autoReloadEnabled(Player player) {
      Config.AutoReload autoReload = (Config.AutoReload)Config.AUTO_RELOAD.get();
      return autoReload == Config.AutoReload.ENABLED || autoReload == Config.AutoReload.CREATIVE && player.m_7500_() || autoReload == Config.AutoReload.SURVIVAL && !player.m_7500_();
   }

   @SubscribeEvent
   public void onClientTick(ClientTickEvent event) {
      Minecraft mc = Minecraft.m_91087_();
      LocalPlayer player = mc.f_91074_;
      if (event.phase == Phase.START) {
         mainLoopLock.lock();
      } else if (event.phase == Phase.END) {
         if (player != null) {
            this.playerDeltaX = player.m_20185_() - this.previousPlayerPosX;
            this.playerDeltaY = player.m_20186_() - this.previousPlayerPosY;
            this.playerDeltaZ = player.m_20189_() - this.previousPlayerPosZ;
            this.previousPlayerPosX = player.m_20185_();
            this.previousPlayerPosY = player.m_20186_();
            this.previousPlayerPosZ = player.m_20189_();
            this.playerDeltaXRot = player.m_146909_() - this.previousPlayerXRot;
            this.playerDeltaYRot = player.m_146908_() - this.previousPlayerYRot;
            this.previousPlayerXRot = player.m_146909_();
            this.previousPlayerYRot = player.m_146908_();
            ItemStack heldItem = player.m_21205_();
            int activeSlot = player.m_150109_().f_35977_;
            if (activeSlot != this.currentInventorySlot) {
               this.inventorySlotChanged = true;
               this.previousInventorySlot = this.currentInventorySlot;
               this.currentInventorySlot = activeSlot;
            } else {
               this.inventorySlotChanged = false;
            }

            boolean updatedSlotHasGun = heldItem != null && heldItem.m_41720_() instanceof GunItem;
            if (updatedSlotHasGun != this.currentSlotHasGun) {
               this.currentSlotHasGun = updatedSlotHasGun;
               this.currentSlotHasGunChanged = true;
            } else {
               this.currentSlotHasGunChanged = false;
            }

            boolean var10000;
            label210: {
               Item var9 = heldItem.m_41720_();
               if (var9 instanceof Enableable) {
                  Enableable e = (Enableable)var9;
                  if (!e.isEnabled()) {
                     var10000 = false;
                     break label210;
                  }
               }

               var10000 = true;
            }

            boolean isEnabled = var10000;
            if (isEnabled && this.autoReloadEnabled(player) && heldItem.m_41720_() instanceof GunItem && !this.inventorySlotChanged) {
               LazyOptional<Integer> optionalAmmo = GunItem.getClientSideAmmo(player, heldItem, this.currentInventorySlot);
               optionalAmmo.ifPresent((ammo) -> {
                  if (ammo <= 0) {
                     ((GunItem)heldItem.m_41720_()).tryReload(player, heldItem);
                  }

               });
            }

            while(isEnabled && ((KeyMapping)RELOAD_KEY.get()).m_90859_() && !this.inventorySlotChanged) {
               if (heldItem.m_41720_() instanceof GunItem) {
                  ((GunItem)heldItem.m_41720_()).tryReload(player, heldItem);
               }
            }

            GunClientState state;
            while(((KeyMapping)FIRE_MODE_KEY.get()).m_90859_()) {
               state = GunClientState.getMainHeldState();
               if (state != null) {
                  state.tryChangeFireMode(player, player.m_21205_());
               }
            }

            while(((KeyMapping)INSPECT_KEY.get()).m_90859_() && !this.inventorySlotChanged) {
               state = GunClientState.getMainHeldState();
               if (state != null) {
                  state.tryInspect(player, player.m_21205_());
               }
            }

            while(((KeyMapping)ATTACHMENT_KEY.get()).m_90859_()) {
               if (heldItem != null && heldItem.m_41720_() instanceof AttachmentHost) {
                  Attachments.tryAttachmentMode(player, heldItem);
               }
            }

            boolean leftMouseButtonDown = mc.f_91066_.f_92096_.m_90857_();
            if (leftMouseButtonDown && !this.leftMouseButtonDown) {
               this.leftMouseDown();
            } else if (!leftMouseButtonDown && this.leftMouseButtonDown) {
               this.leftMouseButtonRelease();
            }

            this.leftMouseButtonDown = leftMouseButtonDown;
            boolean rightMouseButtonDown = mc.f_91066_.f_92095_.m_90857_();
            if (rightMouseButtonDown && !this.rightMouseButtonDown) {
               this.rightMouseButtonDown();
            } else if (!rightMouseButtonDown && this.rightMouseButtonDown) {
               this.rightMouseButtonRelease();
            }

            GunClientState state = GunClientState.getMainHeldState();

            while(((KeyMapping)SCOPE_SWITCH_KEY.get()).m_90859_()) {
               if (heldItem != null && heldItem.m_41720_() instanceof GunItem && state != null && state.isAiming()) {
                  Attachments.tryNextAttachment(player, heldItem, AttachmentCategory.SCOPE, AimingFeature.class);
               }
            }

            if (heldItem.m_41720_() instanceof LockableTarget.TargetLocker) {
               this.lockableTarget.setLocker((LockableTarget.TargetLocker)heldItem.m_41720_());
            } else {
               this.lockableTarget.setLocker((LockableTarget.TargetLocker)null);
            }

            if (state != null && state.isAiming() && state.isIdle() && this.lockableTarget.getLockTimeTicks() > 0L) {
               HitResult hitResult = HitScan.getNearestObjectInCrosshair(player, 0.0F, 400.0D, (b) -> {
                  return false;
               }, (b) -> {
                  return false;
               }, new ArrayList());
               if (hitResult.m_6662_() == HitResult.Type.ENTITY) {
                  Entity hitScanEntity = ((EntityHitResult)hitResult).m_82443_();
                  if (MiscUtil.isProtected(hitScanEntity) || !this.lockableTarget.tryLock(hitScanEntity)) {
                     this.lockableTarget.unlock(hitScanEntity);
                  }
               } else {
                  this.lockableTarget.unlock((Entity)null);
               }
            } else {
               this.lockableTarget.unlock((Entity)null);
            }

            if (this.inventorySlotChanged) {
               this.lockableTarget.unlock((Entity)null);
               this.inertiaController.reset(player);
               this.scopeInertiaController.reset();
               reticleInertiaController.reset();
               ItemStack mainHeldItem;
               if (this.previousInventorySlot >= 0) {
                  label228: {
                     mainHeldItem = player.m_150109_().m_8020_(this.previousInventorySlot);
                     if (mainHeldItem != null) {
                        Item var13 = mainHeldItem.m_41720_();
                        if (var13 instanceof GunItem) {
                           GunItem previousGunItem = (GunItem)var13;
                           GunClientState previousState = GunClientState.getState(player, mainHeldItem, this.previousInventorySlot, false);
                           if (previousState != null) {
                              previousState.tryDeactivate(player, mainHeldItem);
                           }

                           AnimationController<GeoAnimatable> walkingController = previousGunItem.getGeoAnimationController("walking", mainHeldItem);
                           if (walkingController != null) {
                              walkingController.tryTriggerAnimation("animation.model.standing");
                           }
                           break label228;
                        }
                     }

                     if (mainHeldItem != null && mainHeldItem.m_41720_() instanceof ThrowableItem) {
                        ThrowableClientState previousState = ThrowableClientState.getState(player, mainHeldItem, this.previousInventorySlot, false);
                        if (previousState != null) {
                           previousState.tryDeactivate(player, mainHeldItem);
                        }
                     }
                  }
               }

               mainHeldItem = player.m_21205_();
               if (state != null) {
                  state.tryDraw(player, mainHeldItem);
                  if (heldItem.m_41720_() instanceof GunItem) {
                     this.firstPersonWalkingAnimationHandler.reset(player, heldItem);
                  }
               } else if (mainHeldItem.m_41720_() instanceof ThrowableItem) {
                  ThrowableClientState throwableState = ThrowableClientState.getMainHeldState();
                  if (throwableState != null) {
                     throwableState.tryDraw(player, mainHeldItem);
                  }
               }
            }

            if (this.inventorySlotChanged || this.currentSlotHasGunChanged) {
               this.jumpController.reset();
            }

            this.leftMouseButtonDown = leftMouseButtonDown;
            this.rightMouseButtonDown = rightMouseButtonDown;
         }

         mainLoopLock.unlock();
      }

   }

   @SubscribeEvent
   public void onRenderLivingEvent(Pre<LivingEntity, EntityModel<LivingEntity>> e) {
      currentEntityLiving = e.getEntity();
      if (e.getEntity() instanceof Player) {
         Player player = (Player)e.getEntity();
         ItemStack itemStack = player.m_21205_();
         int activeSlot = player.m_150109_().f_35977_;
         if (itemStack != null && itemStack.m_41720_() instanceof GunItem && !PlayerAnimatorCompat.getInstance().isEnabled()) {
            GunClientState gunClientState = GunClientState.getState(player, itemStack, activeSlot, false);
            if (Config.thirdPersonArmPoseAlwaysOn || gunClientState != null && (gunClientState.isAiming() || gunClientState.isFiring())) {
               LivingEntityRenderer<LivingEntity, EntityModel<LivingEntity>> r = e.getRenderer();
               EntityModel<LivingEntity> model = r.m_7200_();
               if (model instanceof PlayerModel) {
                  PlayerModel<?> playerModel = (PlayerModel)model;
                  playerModel.f_102816_ = ArmPose.BOW_AND_ARROW;
               }
            }
         }

         Minecraft mc = Minecraft.m_91087_();
         this.handlePlayerFirstPersonMovement(player, itemStack);
         if (Config.thirdPersonAnimationsEnabled) {
            PlayerAnimatorCompat.getInstance().handlePlayerThirdPersonMovement(player, mc.getPartialTick());
         } else {
            PlayerAnimatorCompat.getInstance().clearAll(player);
         }
      }

   }

   public void onRenderLivingEvent(Post<LivingEntity, EntityModel<LivingEntity>> e) {
      currentEntityLiving = null;
   }

   public static LivingEntity getCurrentEntityLiving() {
      return currentEntityLiving;
   }

   @SubscribeEvent(
      priority = EventPriority.NORMAL
   )
   public void onRenderGameOverlay(net.minecraftforge.client.event.RenderGuiEvent.Post event) {
      Minecraft mc = Minecraft.m_91087_();
      if (mc.f_91074_ != null) {
         if (!(mc.f_91080_ instanceof AttachmentManagerScreen)) {
            ItemStack stack = mc.f_91074_.m_21205_();
            GuiGraphics guiGraphics = event.getGuiGraphics();
            if (stack != null && stack.m_41720_() instanceof GunItem) {
               GunItemOverlay.renderGunOverlay2(guiGraphics, stack);
            }

         }
      }
   }

   @SubscribeEvent
   public void onFovUpdate(ComputeFov event) {
      Minecraft minecraft = Minecraft.m_91087_();
      if (minecraft.f_91074_ != null && !minecraft.f_91074_.m_21205_().m_41619_() && minecraft.f_91066_.m_92176_() == CameraType.FIRST_PERSON) {
         ItemStack itemStack = minecraft.f_91074_.m_21205_();
         if (itemStack.m_41720_() instanceof GunItem) {
            int activeSlot = minecraft.f_91074_.m_150109_().f_35977_;
            GunClientState gunClientState = GunClientState.getState(minecraft.f_91074_, itemStack, activeSlot, false);
            if (gunClientState != null) {
               float aimingZoom = this.zoomValue.update(AimingFeature.getZoom(itemStack));
               BiDirectionalInterpolator aimingController = (BiDirectionalInterpolator)gunClientState.getAnimationController("aiming");
               float zoomAdj = aimingZoom * (float)aimingController.getValue();
               event.setFOV(event.getFOV() - event.getFOV() * (double)zoomAdj - (double)((float)this.sharedViewShakeController.getRoll()));
            }

         }
      }
   }

   @SubscribeEvent
   public void onRenderHandEvent(RenderHandEvent event) {
      ItemStack heldItem = event.getItemStack();
      if (event.getHand() == InteractionHand.MAIN_HAND) {
         Item var4 = heldItem.m_41720_();
         if (var4 instanceof GunItem) {
            GunItem gunItem = (GunItem)var4;
            int slot = ClientUtils.getClientPlayer().m_150109_().m_36030_(heldItem);
            Minecraft minecraft = Minecraft.m_91087_();
            GunClientState gunClientState = GunClientState.getState(minecraft.f_91074_, heldItem, slot, false);
            if (gunClientState != null) {
               Player player;
               if ((Boolean)minecraft.f_91066_.m_231830_().m_231551_() && minecraft.m_91288_() instanceof Player) {
                  player = (Player)minecraft.m_91288_();
                  PoseStack poseStack = event.getPoseStack();
                  float f = player.f_19787_ - player.f_19867_;
                  float partialTick = event.getPartialTick();
                  float walkDistance = -(player.f_19787_ + f * partialTick);
                  float bobbing = Mth.m_14179_(partialTick, player.f_36099_, player.f_36100_);
                  poseStack.m_252781_(Axis.f_252529_.m_252977_(-Math.abs(Mth.m_14089_(walkDistance * 3.1415927F - 0.2F) * bobbing) * 5.0F));
                  poseStack.m_252781_(Axis.f_252403_.m_252977_(-Mth.m_14031_(walkDistance * 3.1415927F) * bobbing * 3.0F));
                  poseStack.m_252880_(-Mth.m_14031_(walkDistance * 3.1415927F) * bobbing * 0.5F, Math.abs(Mth.m_14089_(walkDistance * 3.1415927F) * bobbing), 0.0F);
                  float randomPitch = 0.0F;
                  float randomYaw = 0.0F;
                  float targetBobbing = 1.0F;
                  float targetYaw;
                  if (player.m_20142_()) {
                     targetYaw = 10.0F;
                     targetBobbing = gunItem.getBobbing();
                  } else if (gunClientState.isAiming()) {
                     targetBobbing = gunItem.getBobbingOnAim();
                     float afc = AimingFeature.getViewBobbing(heldItem);
                     targetYaw = 5.0F * afc;
                     targetBobbing *= afc;
                  } else {
                     targetBobbing = gunItem.getBobbing();
                     targetYaw = 5.0F;
                  }

                  if (!Config.firstPersonAnimationsEnabled) {
                     bobbing *= this.bobbingValue.update(targetBobbing);
                     float bobbingRoll = bobbing * 10.0F * gunItem.getBobbingRollMultiplier();
                     float bobbingPitch = bobbing * 5.0F;
                     float bobbingYaw = bobbing * this.bobbingYawValue.update(targetYaw);
                     poseStack.m_85837_((double)(-Mth.m_14031_(walkDistance * 3.1415927F) * bobbing * 0.5F), (double)Math.abs(Mth.m_14089_(walkDistance * 3.1415927F) * bobbing) * 0.35D + (double)(randomPitch * 0.01F), (double)(randomYaw * 0.0F));
                     poseStack.m_252781_(Axis.f_252403_.m_252977_(Mth.m_14031_(walkDistance * 3.1415927F) * bobbingRoll));
                     poseStack.m_252781_(Axis.f_252529_.m_252977_(Math.abs(Mth.m_14089_(walkDistance * 3.1415927F - 0.2F)) * bobbingPitch));
                     poseStack.m_252781_(Axis.f_252436_.m_252977_(Mth.m_14031_(walkDistance * 3.1415927F) * bobbingYaw));
                  }
               }

               if (Config.firstPersonAnimationsEnabled) {
                  player = ClientUtil.getClientPlayer();
                  this.handlePlayerFirstPersonMovement(player, player.m_21205_());
               }

               GunRandomizingAnimationController randomizerController = (GunRandomizingAnimationController)gunClientState.getAnimationController("randomizer");
               if (randomizerController != null) {
                  if (this.inventorySlotChanged || this.currentSlotHasGunChanged) {
                     randomizerController.reset();
                  }

                  double posX = randomizerController.getPosX();
                  double posY = randomizerController.getPosY();
                  double posZ = randomizerController.getPosZ();
                  double roll = randomizerController.getRoll();
                  double yaw = randomizerController.getYaw();
                  double pitch = randomizerController.getPitch();
                  PoseStack poseStack = event.getPoseStack();
                  Quaternionf q = new Quaternionf(pitch, yaw, roll, 1.0D);
                  poseStack.m_85837_(posX, posY, posZ);
                  poseStack.m_252781_(q);
               }

               GunRecoilAnimationController recoilController = (GunRecoilAnimationController)gunClientState.getAnimationController("recoil2");
               double roll;
               double yaw;
               double posY;
               double posZ;
               double roll;
               if (recoilController != null) {
                  if (this.inventorySlotChanged || this.currentSlotHasGunChanged) {
                     recoilController.reset();
                  }

                  roll = recoilController.getPosX();
                  yaw = recoilController.getPosY();
                  posY = recoilController.getPosZ();
                  posZ = recoilController.getRoll();
                  roll = recoilController.getPitch();
                  PoseStack poseStack = event.getPoseStack();
                  Quaternionf q = new Quaternionf(roll, 0.0D, posZ, 1.0D);
                  poseStack.m_85837_(roll, yaw, posY);
                  poseStack.m_252781_(q);
               }

               if (this.jumpController != null) {
                  roll = gunItem.getJumpMultiplier();
                  yaw = this.jumpController.getPosX() * roll;
                  posY = this.jumpController.getPosY() * roll;
                  posZ = this.jumpController.getPosZ() * roll;
                  roll = this.jumpController.getRoll() * roll;
                  double pitch = this.jumpController.getPitch() * roll;
                  double yaw = this.jumpController.getYaw() * roll;
                  PoseStack poseStack = event.getPoseStack();
                  Quaternionf q = new Quaternionf(pitch, yaw, roll, 1.0D);
                  poseStack.m_85837_(yaw, posY, posZ);
                  poseStack.m_252781_(q);
               }

               if (this.inertiaController != null) {
                  roll = this.inertiaController.getRoll();
                  yaw = 0.0D;
                  PoseStack poseStack = event.getPoseStack();
                  Quaternionf q = new Quaternionf(0.0D, yaw, roll, 1.0D);
                  poseStack.m_252781_(q);
               }
            }
         }
      }

   }

   private void handlePlayerFirstPersonMovement(Player player, ItemStack itemStack) {
      this.firstPersonWalkingAnimationHandler.handlePlayerFirstPersonMovement(player, itemStack);
   }

   @SubscribeEvent
   public void onClickEvent(InteractionKeyMappingTriggered event) {
      Minecraft mc = Minecraft.m_91087_();
      LocalPlayer player = mc.f_91074_;
      ItemStack heldItem = player.m_21205_();
      if (heldItem.m_41720_() instanceof GunItem || heldItem.m_41720_() instanceof ThrowableItem) {
         event.setCanceled(true);
      }

   }

   @SubscribeEvent
   public void onPreRenderHandEvent(com.vicmatskiv.pointblank.event.RenderHandEvent.Pre event) {
      ItemStack itemStack = GunItem.getMainHeldGunItemStack(ClientUtils.getClientPlayer());
      if (itemStack != null) {
         Item var4 = itemStack.m_41720_();
         if (var4 instanceof Nameable) {
            Nameable gunItem = (Nameable)var4;
            BakedGeoModel model = AttachmentModelInfo.getModel(gunItem.getName());
            if (model != null) {
               GeoBone cameraBone = (GeoBone)model.getBone("_camera_").orElse((Object)null);
               if (cameraBone != null) {
                  PoseStack poseStack = event.getPoseStack();
                  if (cameraBone.getRotY() != 0.0F) {
                     poseStack.m_252781_(Axis.f_252436_.m_252961_(-cameraBone.getRotY()));
                  }

                  if (cameraBone.getRotX() != 0.0F) {
                     poseStack.m_252781_(Axis.f_252529_.m_252961_(-cameraBone.getRotX()));
                  }

                  if (cameraBone.getRotZ() != 0.0F) {
                     poseStack.m_252781_(Axis.f_252403_.m_252961_(-cameraBone.getRotZ()));
                  }
               }
            }
         }
      }

   }

   @SubscribeEvent
   public void onComputeCameraAngles(ComputeCameraAngles event) {
      Minecraft mc = Minecraft.m_91087_();
      if (mc.f_91066_.m_92176_().m_90612_()) {
         GunClientState state = GunClientState.getMainHeldState();
         if (state != null) {
            PryAnimationController shakeAnimationController = (PryAnimationController)state.getAnimationController("shake");
            if (shakeAnimationController != null) {
               float yawModifier = state.isAiming() ? 0.1F : 1.0F;
               float rollModifier = state.isAiming() ? 1.5F : 1.0F;
               event.setRoll(event.getRoll() + (float)shakeAnimationController.getRoll() * rollModifier);
               event.setYaw(event.getYaw() + (float)shakeAnimationController.getYaw() * yawModifier);
            }

            TimerController reloadTimer = (TimerController)state.getAnimationController("reloadTimer");
            if (reloadTimer != null && !reloadTimer.isDone()) {
               Iterator var13 = reloadTimer.getActiveHandlers(ClientUtils.getClientPlayer(), state, ClientUtils.getClientPlayer().m_21205_()).iterator();

               while(var13.hasNext()) {
                  AbstractProceduralAnimationController activeHandler = (AbstractProceduralAnimationController)var13.next();
                  event.setRoll(event.getRoll() + (float)activeHandler.getRoll());
                  event.setPitch(event.getPitch() + (float)activeHandler.getPitch());
               }
            }

            if (this.sharedViewShakeController != null) {
               event.setRoll(event.getRoll() + (float)this.sharedViewShakeController.getRoll() * 0.5F);
            }

            if (this.inertiaController != null) {
               double roll = this.inertiaController.getYaw();
               double yaw = this.inertiaController.getYaw();
               event.setRoll(event.getRoll() + (float)roll * 15.0F);
               event.setYaw(event.getYaw() + (float)yaw * 15.0F);
            }
         }

         ItemStack itemStack = ClientUtil.getClientPlayer().m_21205_();
         Item var15 = itemStack.m_41720_();
         if (var15 instanceof Nameable) {
            Nameable gunItem = (Nameable)var15;
            BakedGeoModel model = AttachmentModelInfo.getModel(gunItem.getName());
            if (model != null) {
               GeoBone cameraBone = (GeoBone)model.getBone("_camera_").orElse((Object)null);
               if (cameraBone != null) {
                  event.setPitch(event.getPitch() - cameraBone.getRotX() * 57.295776F);
                  event.setYaw(event.getYaw() - cameraBone.getRotY() * 57.295776F);
                  event.setRoll(event.getRoll() - cameraBone.getRotZ() * 57.295776F);
               }
            }
         }

      }
   }

   @SubscribeEvent
   public void onRenderOverlay(net.minecraftforge.client.event.RenderGuiOverlayEvent.Pre event) {
      Minecraft minecraft = Minecraft.m_91087_();
      if (minecraft.f_91074_ != null) {
         ItemStack itemStack = minecraft.f_91074_.m_21205_();
         if (itemStack.m_41720_() instanceof GunItem) {
            if (event.getOverlay() == VanillaGuiOverlay.HOTBAR.type() && minecraft.f_91080_ instanceof AttachmentManagerScreen) {
               event.setCanceled(true);
            } else if (minecraft.f_91066_.m_92176_().m_90612_()) {
               int activeSlot = minecraft.f_91074_.m_150109_().f_35977_;
               GunClientState gunClientState = GunClientState.getState(minecraft.f_91074_, itemStack, activeSlot, false);
               if (gunClientState != null) {
                  GunItem item = (GunItem)itemStack.m_41720_();
                  ResourceLocation scopeOverlay = item.getScopeOverlay();
                  if (event.getOverlay() == VanillaGuiOverlay.CROSSHAIR.type()) {
                     if (Config.crosshairType == Config.CrosshairType.DEFAULT && !gunClientState.isAiming() && (gunClientState.isFiring() || gunClientState.isIdle())) {
                        float crossHairExpansionRate = this.getCrosshairExpansionRatio(minecraft.f_91074_, gunClientState);
                        double originalAspectRatio = 1.0D;
                        int width = event.getWindow().m_85445_();
                        int height = event.getWindow().m_85446_();
                        double scaleFactor = 3.3D;
                        int renderWidth = (int)((double)width * scaleFactor);
                        int renderHeight = (int)((double)renderWidth / originalAspectRatio);
                        if (renderHeight > height) {
                           renderHeight = (int)((double)height * scaleFactor);
                           int var10000 = (int)((double)renderHeight * originalAspectRatio);
                        }

                        int renderWidth = 80;
                        int renderHeight = 80;
                        float posX = (float)(width - renderWidth) / 2.0F;
                        float posY = (float)(height - renderHeight) / 2.0F;
                        CrosshairRenderer.renderCrosshairOverlay3(event.getGuiGraphics(), event.getPartialTick(), crossHairOverlay, crossHairExpansionRate - 1.0F, posX, posY, renderWidth, renderHeight);
                     }

                     if (Config.crosshairType == Config.CrosshairType.DEFAULT || Config.crosshairType == Config.CrosshairType.DISABLED) {
                        event.setCanceled(true);
                     }
                  }

                  if (gunClientState.isAiming()) {
                     boolean pipZoomEnabled = MiscUtil.isGreaterThanZero(gunClientState.getGunItem().getPipScopeZoom());
                     if (scopeOverlay != null && !pipZoomEnabled) {
                        event.setCanceled(true);
                     }

                     if (!pipZoomEnabled || !Config.pipScopesEnabled) {
                        BiDirectionalInterpolator aimingController = (BiDirectionalInterpolator)gunClientState.getAnimationController("aiming");
                        double aimingProgress = aimingController.getValue();
                        if (aimingController != null && scopeOverlay != null && !gunClientState.isReloading()) {
                           this.renderTextureOverlay(item, gunClientState, event.getGuiGraphics(), event.getPartialTick(), item.getScopeOverlay(), event.getWindow().m_85445_(), event.getWindow().m_85446_(), (float)aimingProgress);
                        }
                     }
                  }

               }
            }
         }
      }
   }

   protected void renderTextureOverlay(GunItem gunItem, GunClientState gunClientState, GuiGraphics guiGraphics, float partialTick, ResourceLocation textureLocation, int width, int height, float alpha) {
      double originalAspectRatio = 1.0D;
      double scaleFactor = 3.3D;
      int renderWidth = (int)((double)width * scaleFactor);
      int renderHeight = (int)((double)renderWidth / originalAspectRatio);
      if (renderHeight > height) {
         renderHeight = (int)((double)height * scaleFactor);
         renderWidth = (int)((double)renderHeight * originalAspectRatio);
      }

      float posX = (float)(width - renderWidth) / 2.0F;
      float posY = (float)(height - renderHeight) / 2.0F;
      if (this.scopeInertiaController != null) {
         double yaw = this.scopeInertiaController.getYaw();
         double pitch = this.scopeInertiaController.getPitch();
         posX = (float)((double)posX - yaw * 5000.0D);
         posY = (float)((double)posY - pitch * 5000.0D);
      }

      GunRecoilAnimationController recoilController = (GunRecoilAnimationController)gunClientState.getAnimationController("recoil2");
      if (recoilController != null) {
         if (this.inventorySlotChanged || this.currentSlotHasGunChanged) {
            recoilController.reset();
         }

         double posZ1 = recoilController.getPosZ();
         posY = (float)((double)posY + posZ1 * 50.0D);
      }

      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);
      guiGraphics.m_280246_(1.0F, 1.0F, 1.0F, alpha);
      RenderUtil.blit(guiGraphics, textureLocation, posX, posY, -90, 0.0F, 0.0F, renderWidth, renderHeight, renderWidth, renderHeight);
      ResourceLocation targetLockOverlay = gunItem.getTargetLockOverlay();
      if (targetLockOverlay != null) {
         this.renderTargetLockOverlay(guiGraphics, partialTick, targetLockOverlay, posX, posY, renderWidth, renderHeight);
      }

      RenderSystem.depthMask(true);
      RenderSystem.enableDepthTest();
      guiGraphics.m_280246_(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private void renderTargetLockOverlay(GuiGraphics guiGraphics, float partialTick, ResourceLocation targetLockonOverlay, float posX, float posY, int renderWidth, int renderHeight) {
      UpDownCounter lockCounter = this.lockableTarget.getLockCounter();
      float targetLockCount = Mth.m_14179_(partialTick, (float)lockCounter.getPreviousValue(), (float)lockCounter.getCurrentValue()) / (float)lockCounter.getMaxValue();
      float lockProgress2 = 1.0F - targetLockCount;
      float lockRatio = 0.2F;
      int halfLockWidth = (int)((float)renderWidth * lockRatio * 0.5F);
      int halfLockHeight = (int)((float)renderHeight * lockRatio * 0.5F);
      float centerX = posX + (float)renderWidth * 0.5F;
      float centerY = posY + (float)renderHeight * 0.5F;
      float xOffset = (float)halfLockWidth * 0.4F * lockProgress2;
      float yOffset = (float)halfLockHeight * 0.4F * lockProgress2;
      float posXStart = 0.0F;
      float posXEnd = 0.0F;
      float posYStart = 0.5F;
      float posYEnd = 0.5F;
      float minU = centerX - (float)halfLockWidth - xOffset;
      float minV = centerX - xOffset;
      float maxU = centerY - (float)halfLockHeight - yOffset;
      float maxV = centerY - yOffset;
      RenderUtil.blit(guiGraphics, targetLockonOverlay, minU, minV, maxU, maxV, -90.0F, posXStart, posYStart, posXEnd, posYEnd);
      posXStart = centerX + xOffset;
      posXEnd = centerX + (float)halfLockWidth + xOffset;
      posYStart = centerY - (float)halfLockHeight - yOffset;
      posYEnd = centerY - yOffset;
      minU = 0.5F;
      minV = 0.0F;
      maxU = 1.0F;
      maxV = 0.5F;
      RenderUtil.blit(guiGraphics, targetLockonOverlay, posXStart, posXEnd, posYStart, posYEnd, -90.0F, minU, maxU, minV, maxV);
      posXStart = centerX + xOffset;
      posXEnd = centerX + (float)halfLockWidth + xOffset;
      posYStart = centerY + yOffset;
      posYEnd = centerY + (float)halfLockHeight + yOffset;
      minU = 0.5F;
      minV = 0.5F;
      maxU = 1.0F;
      maxV = 1.0F;
      RenderUtil.blit(guiGraphics, targetLockonOverlay, posXStart, posXEnd, posYStart, posYEnd, -90.0F, minU, maxU, minV, maxV);
      posXStart = centerX - (float)halfLockWidth - xOffset;
      posXEnd = centerX - xOffset;
      posYStart = centerY + yOffset;
      posYEnd = centerY + (float)halfLockHeight + yOffset;
      minU = 0.0F;
      minV = 0.5F;
      maxU = 0.5F;
      maxV = 1.0F;
      RenderUtil.blit(guiGraphics, targetLockonOverlay, posXStart, posXEnd, posYStart, posYEnd, -90.0F, minU, maxU, minV, maxV);
   }

   private void setTriggerOff(LocalPlayer player) {
      ItemStack heldItem = player.m_21205_();
      Item var5 = heldItem.m_41720_();
      if (var5 instanceof GunItem) {
         GunItem gunItem = (GunItem)var5;
         if (player.m_20142_()) {
            player.m_6858_(false);
         } else {
            gunItem.setTriggerOff(player, heldItem);
         }
      } else {
         var5 = heldItem.m_41720_();
         if (var5 instanceof ThrowableItem) {
            ThrowableItem throwableItem = (ThrowableItem)var5;
            throwableItem.setTriggerOff(player, heldItem);
         }
      }

   }

   private void leftMouseDown() {
      Minecraft mc = Minecraft.m_91087_();
      LocalPlayer player = mc.f_91074_;
      if (player != null) {
         this.tryFire(player);
      }
   }

   private void rightMouseButtonDown() {
      Minecraft mc = Minecraft.m_91087_();
      LocalPlayer player = mc.f_91074_;
      if (player != null) {
         if (player.m_20142_()) {
            player.m_6858_(false);
         }

         MiscUtil.getMainHeldGun(player).ifPresent((item) -> {
            if (item.isAimingEnabled()) {
               this.toggleAiming(player, true);
            }

         });
      }
   }

   private void rightMouseButtonRelease() {
      Minecraft mc = Minecraft.m_91087_();
      LocalPlayer player = mc.f_91074_;
      this.toggleAiming(player, false);
   }

   private void leftMouseButtonRelease() {
      Minecraft mc = Minecraft.m_91087_();
      LocalPlayer player = mc.f_91074_;
      this.setTriggerOff(player);
   }

   public void tickMainHeldGun() {
      Player player = ClientUtils.getClientPlayer();
      if (player != null) {
         GunClientState state = GunClientState.getMainHeldState();
         if (state != null) {
            state.stateTick(player, player.m_21205_(), true);
            this.scopeInertiaController.onUpdateState(player, state);
            reticleInertiaController.onUpdateState(player, state);
            this.inertiaController.onUpdateState(player, state);
         }
      }

   }

   public static void runSyncTick(Runnable runnable) {
      mainLoopLock.lock();

      try {
         runnable.run();
      } catch (Exception var5) {
         LOGGER.error("Client sync tick failed: {}", var5);
      } finally {
         mainLoopLock.unlock();
      }

   }

   public static <T> T runSyncCompute(Supplier<T> resultSupplier) {
      mainLoopLock.lock();

      Object result;
      try {
         result = resultSupplier.get();
      } catch (Exception var6) {
         LOGGER.error("Run sync compute failed: {}", var6);
         throw var6;
      } finally {
         mainLoopLock.unlock();
      }

      return result;
   }

   private boolean toggleAiming(LocalPlayer player, boolean isAiming) {
      boolean toggled = false;
      ItemStack itemStack = player.m_21205_();
      if (itemStack != null && itemStack.m_41720_() instanceof GunItem) {
         int activeSlot = player.m_150109_().f_35977_;
         GunClientState gunClientState = GunClientState.getState(player, itemStack, activeSlot, false);
         if (gunClientState != null) {
            toggled = gunClientState.isAiming() != isAiming;
            gunClientState.setAiming(isAiming);
            if (toggled) {
               Network.networkChannel.sendToServer(new AimingChangeRequestPacket(gunClientState.getId(), activeSlot, isAiming));
            }
         }
      }

      return toggled;
   }

   private boolean tryFire(LocalPlayer player) {
      boolean result = false;
      ItemStack heldItemStack = player.m_21205_();
      Item var6;
      if (heldItemStack != null) {
         var6 = heldItemStack.m_41720_();
         if (var6 instanceof GunItem) {
            GunItem gunItem = (GunItem)var6;
            if (player.m_21206_() != heldItemStack && gunItem.isEnabled()) {
               long minTargetLockTime = gunItem.getTargetLockTimeTicks();
               if (minTargetLockTime == 0L || this.lockableTarget.getLockCounter().isAtMax()) {
                  result = gunItem.tryFire(player, heldItemStack, this.lockableTarget.getTargetEntity());
               }

               return result;
            }
         }
      }

      if (heldItemStack != null) {
         var6 = heldItemStack.m_41720_();
         if (var6 instanceof ThrowableItem) {
            ThrowableItem throwableItem = (ThrowableItem)var6;
            if (player.m_21206_() != heldItemStack && throwableItem.isEnabled()) {
               result = throwableItem.tryThrow(player, heldItemStack, this.lockableTarget.getTargetEntity());
            }
         }
      }

      return result;
   }

   @SubscribeEvent
   public void onJump(LivingJumpEvent event) {
      Level level = MiscUtil.getLevel(event.getEntity());
      if (level.f_46443_ && event.getEntity() instanceof Player) {
         Player player = (Player)event.getEntity();
         ItemStack heldItem = player.m_21205_();
         if (player == ClientUtils.getClientPlayer() && heldItem.m_41720_() instanceof GunItem) {
            GunClientState state = GunClientState.getState(player, heldItem, this.currentInventorySlot, false);
            if (state != null) {
               state.jump(player, heldItem);
            }

            if (this.jumpController != null) {
               this.jumpController.onJumping(player, state, heldItem);
            }
         }
      }

   }

   @SubscribeEvent
   public void onPlayerTick(PlayerTickEvent event) {
      if (event.side == LogicalSide.CLIENT) {
         Player mainPlayer = ClientUtils.getClientPlayer();
         if (event.player != mainPlayer) {
            ItemStack itemStack = event.player.m_21205_();
            GunClientState state = GunClientState.getMainHeldState(event.player);
            if (state != null) {
               state.stateTick(event.player, itemStack, false);
            }
         }
      }

      ItemStack itemStack = event.player.m_21205_();
      if (itemStack != null && itemStack.m_41720_() instanceof GunItem) {
         if (GunItem.isAiming(itemStack)) {
            event.player.m_6858_(false);
         }

         if (MiscUtil.isClientSide(event.player)) {
            GunClientState state = GunClientState.getMainHeldState(event.player);
            if (state != null && !state.isIdle() && !state.isDrawing()) {
               event.player.m_6858_(false);
            }
         }
      }

   }

   @SubscribeEvent
   public void onRenderTooltip(net.minecraftforge.client.event.RenderTooltipEvent.Pre event) {
      ItemStack itemStack = event.getItemStack();
      if (itemStack.m_41720_() instanceof AttachmentHost) {
         event.setCanceled(true);
         Minecraft mc = Minecraft.m_91087_();
         GuiGraphics guiGraphics = event.getGraphics();
         List<ClientTooltipComponent> tooltipComponents = event.getComponents();
         int i = 0;
         int j = tooltipComponents.size() == 1 ? -2 : 0;

         ClientTooltipComponent clienttooltipcomponent;
         for(Iterator var8 = tooltipComponents.iterator(); var8.hasNext(); j += clienttooltipcomponent.m_142103_()) {
            clienttooltipcomponent = (ClientTooltipComponent)var8.next();
            int k = clienttooltipcomponent.m_142069_(event.getFont());
            if (k > i) {
               i = k;
            }
         }

         Vector2ic vector2ic = event.getTooltipPositioner().m_262814_(guiGraphics.m_280182_(), guiGraphics.m_280206_(), event.getX(), event.getY(), i, j);
         int l = vector2ic.x();
         int i1 = vector2ic.y();
         PoseStack poseStack = guiGraphics.m_280168_();
         poseStack.m_85836_();
         guiGraphics.m_286007_(() -> {
            int background = 1343229968;
            int borderStart = 1342218495;
            int borderEnd = 1342197879;
            if (!(mc.f_91080_ instanceof AttachmentManagerScreen)) {
               background = -267382768;
            }

            TooltipRenderUtil.renderTooltipBackground(guiGraphics, l, i1, i, j, 400, background, background, borderStart, borderEnd);
         });
         poseStack.m_252880_(0.0F, 0.0F, 400.0F);
         int k1 = i1;

         int k2;
         ClientTooltipComponent component;
         for(k2 = 0; k2 < tooltipComponents.size(); ++k2) {
            component = (ClientTooltipComponent)tooltipComponents.get(k2);
            component.m_142440_(event.getFont(), l, k1, poseStack.m_85850_().m_252922_(), guiGraphics.m_280091_());
            k1 += component.m_142103_() + (k2 == 0 ? 2 : 0);
         }

         k1 = i1;

         for(k2 = 0; k2 < tooltipComponents.size(); ++k2) {
            component = (ClientTooltipComponent)tooltipComponents.get(k2);
            component.m_183452_(event.getFont(), l, k1, guiGraphics);
            k1 += component.m_142103_() + (k2 == 0 ? 2 : 0);
         }

         poseStack.m_85849_();
      }
   }

   @SubscribeEvent
   public void onGatherTooltipComponents(GatherComponents event) {
      ItemStack itemStack = event.getItemStack();
      Item item = itemStack.m_41720_();
      if (item instanceof FeatureProvider) {
         FeatureProvider featureProvider = (FeatureProvider)item;
         List<Either<FormattedText, TooltipComponent>> elements = event.getTooltipElements();
         List<Component> components = new ArrayList();
         components.addAll(featureProvider.getDescriptionTooltipLines());
         components.addAll(featureProvider.getFeatureTooltipLines());
         if (item instanceof AttachmentHost) {
            AttachmentHost attachmentHost = (AttachmentHost)item;
            Collection<Attachment> compatibleAttachments = attachmentHost.getCompatibleAttachments();
            if (!compatibleAttachments.isEmpty()) {
               if (Screen.m_96638_()) {
                  components.addAll(attachmentHost.getCompatibleAttachmentTooltipLines(itemStack));
               } else {
                  components.add(Component.m_237119_());
                  components.add(Component.m_237115_("message.pointblank.holdShiftForCompatibleAttachments").m_130940_(ChatFormatting.ITALIC).m_130940_(ChatFormatting.DARK_AQUA));
               }
            }
         }

         Iterator var9 = components.iterator();

         while(var9.hasNext()) {
            Component c = (Component)var9.next();
            elements.add(Either.left(c));
         }
      }

   }

   private float getCrosshairExpansionRatio(Player player, GunClientState gunClientState) {
      float inaccuracy = 2.0F;
      float baseExpansion = 1.0F;
      float speedMultiplier = 4.0F * inaccuracy;
      float rotationMultiplier = 0.05F * inaccuracy;
      float sprintMultiplier = 1.5F * inaccuracy;
      float jumpMultiplier = 1.2F * inaccuracy;
      float sneakMultiplier = 0.8F;
      float crouchMultiplier = 0.7F;
      float fireMultiplier = 2.0F * inaccuracy;
      double horizontalSpeed = (double)Mth.m_14116_((float)(this.playerDeltaX * this.playerDeltaX + this.playerDeltaY * this.playerDeltaY + this.playerDeltaZ * this.playerDeltaZ));
      float expansionRatio = baseExpansion + (float)horizontalSpeed * speedMultiplier;
      if (player.m_20142_()) {
         expansionRatio *= sprintMultiplier;
      }

      if (!player.m_20096_()) {
         expansionRatio *= jumpMultiplier;
      }

      if (player.m_20143_()) {
         expansionRatio *= sneakMultiplier;
      }

      if (player.m_6047_()) {
         expansionRatio *= crouchMultiplier;
      }

      float yawChangeRate = Math.abs(this.playerDeltaYRot);
      float pitchChangeRate = Math.abs(this.playerDeltaXRot);
      expansionRatio += yawChangeRate * rotationMultiplier;
      expansionRatio += pitchChangeRate * rotationMultiplier;
      if (gunClientState.getTotalUninterruptedShots() > 0) {
         expansionRatio *= fireMultiplier;
      }

      return Mth.m_14036_(this.crossHairExp.update(expansionRatio), 1.0F, 7.0F);
   }

   @EventBusSubscriber(
      modid = "pointblank",
      value = {Dist.CLIENT},
      bus = Bus.MOD
   )
   public static class ModBusRegistration {
      @SubscribeEvent
      public static void registerKeybindings(RegisterKeyMappingsEvent event) {
         event.register((KeyMapping)ClientEventHandler.RELOAD_KEY.get());
         event.register((KeyMapping)ClientEventHandler.FIRE_MODE_KEY.get());
         event.register((KeyMapping)ClientEventHandler.INSPECT_KEY.get());
         event.register((KeyMapping)ClientEventHandler.ATTACHMENT_KEY.get());
         event.register((KeyMapping)ClientEventHandler.SCOPE_SWITCH_KEY.get());
      }

      @SubscribeEvent
      public static void registerRenderers(RegisterRenderers event) {
         event.registerBlockEntityRenderer((BlockEntityType)BlockEntityRegistry.WORKSTATION_BLOCK_ENTITY.get(), (context) -> {
            return new BaseModelBlockRenderer((BaseBlockModel)BlockModelRegistry.WORKSTATION_BLOCK_MODEL.get());
         });
         event.registerBlockEntityRenderer((BlockEntityType)BlockEntityRegistry.PRINTER_BLOCK_ENTITY.get(), (context) -> {
            return new BaseModelBlockRenderer((BaseBlockModel)BlockModelRegistry.PRINTER_BLOCK_MODEL.get());
         });
         Iterator var1 = EntityRegistry.getItemEntityBuilders().entrySet().iterator();

         while(var1.hasNext()) {
            Entry<RegistryObject<EntityType<?>>, Supplier<EntityBuilder<?, ?>>> e = (Entry)var1.next();
            Supplier<EntityBuilder<?, ?>> supplier = (Supplier)e.getValue();
            EntityBuilder<?, ?> builder = (EntityBuilder)supplier.get();
            if (builder.hasRenderer()) {
               EntityType entityType = (EntityType)((RegistryObject)e.getKey()).get();
               event.registerEntityRenderer(entityType, (context) -> {
                  return builder.createEntityRenderer(context);
               });
            }
         }

      }

      @SubscribeEvent
      public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
         event.registerSpriteSet((ParticleType)ParticleRegistry.IMPACT_PARTICLE.get(), EffectParticles.EffectParticleProvider::new);
      }

      @SubscribeEvent
      public static void setupClient(FMLClientSetupEvent evt) {
         MenuScreens.m_96206_((MenuType)MenuRegistry.ATTACHMENTS.get(), AttachmentManagerScreen::new);
         MenuScreens.m_96206_((MenuType)MenuRegistry.CRAFTING.get(), CraftingScreen::new);
         PlayerAnimatorCompat.getInstance().registerAnimationTypes();
         ThirdPersonAnimationRegistry.init();
      }
   }
}
