package com.vicmatskiv.pointblank.client.effect;

import com.vicmatskiv.pointblank.Config;
import com.vicmatskiv.pointblank.client.GunClientState;
import com.vicmatskiv.pointblank.client.GunStateListener;
import com.vicmatskiv.pointblank.client.GunStatePoseProvider;
import com.vicmatskiv.pointblank.client.PoseProvider;
import com.vicmatskiv.pointblank.client.PositionProvider;
import com.vicmatskiv.pointblank.client.VertexConsumers;
import com.vicmatskiv.pointblank.feature.Features;
import com.vicmatskiv.pointblank.item.GunItem;
import com.vicmatskiv.pointblank.network.EffectBroadcastPacket;
import com.vicmatskiv.pointblank.network.Network;
import com.vicmatskiv.pointblank.registry.EffectRegistry;
import com.vicmatskiv.pointblank.util.MiscUtil;
import com.vicmatskiv.pointblank.util.SimpleHitResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.bernie.geckolib.util.ClientUtils;

public class EffectLauncher implements GunStateListener {
   private static final Logger LOGGER = LogManager.getLogger("pointblank");
   private static final int MAX_DISTANCE_SQR = 22500;
   private Map<GunItem.FirePhase, List<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>>> effectBuilders;

   public EffectLauncher(Map<GunItem.FirePhase, List<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>>> effectBuilders) {
      this.effectBuilders = effectBuilders;
   }

   public void onPrepareFiring(LivingEntity player, GunClientState gunClientState, ItemStack itemStack) {
      this.applyPhaseEffects(GunItem.FirePhase.PREPARING, player, gunClientState, itemStack, (HitResult)null, 0.0F, false);
   }

   public void onStartFiring(LivingEntity player, GunClientState state, ItemStack itemStack) {
      this.applyPhaseEffects(GunItem.FirePhase.FIRING, player, state, itemStack, (HitResult)null, 0.0F, false);
   }

   public void onCompleteFiring(LivingEntity player, GunClientState state, ItemStack itemStack) {
      this.applyPhaseEffects(GunItem.FirePhase.COMPLETETING, player, state, itemStack, (HitResult)null, 0.0F, false);
   }

   public void onHitScanTargetAcquired(LivingEntity player, GunClientState state, ItemStack itemStack, HitResult hitResult) {
      this.applyPhaseEffects(GunItem.FirePhase.HIT_SCAN_ACQUIRED, player, state, itemStack, hitResult, 0.0F, false);
   }

   public void onHitScanTargetConfirmed(LivingEntity player, GunClientState state, ItemStack itemStack, HitResult hitResult, float damage) {
      this.applyPhaseEffects(GunItem.FirePhase.HIT_TARGET, player, state, itemStack, hitResult, damage, false);
   }

   private static List<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>> getGlobalEffectBuilders(GunItem.FirePhase phase, LivingEntity player, GunClientState gunClientState, ItemStack itemStack, HitResult hitResult, boolean thirdPersonOnly) {
      if (phase == GunItem.FirePhase.HIT_TARGET && hitResult.m_6662_() == Type.ENTITY && hitResult instanceof SimpleHitResult) {
         SimpleHitResult simpleHitResult = (SimpleHitResult)hitResult;
         int entityId = simpleHitResult.getEntityId();
         if (entityId > 0) {
            Entity entity = ClientUtils.getLevel().m_6815_(entityId);
            if (entity instanceof LivingEntity) {
               return EffectRegistry.getEntityHitEffects(entity);
            }
         }
      }

      return Collections.emptyList();
   }

   @OnlyIn(Dist.CLIENT)
   private void applyPhaseEffects(GunItem.FirePhase phase, LivingEntity player, GunClientState gunClientState, ItemStack itemStack, HitResult hitResult, float damage, boolean thirdPersonOnly) {
      if (Config.particleEffectsEnabled) {
         Item item = itemStack.m_41720_();
         if (item instanceof GunItem) {
            List<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>> mainPhaseEffectBuilders = (List)this.effectBuilders.computeIfAbsent(phase, (k) -> {
               return new ArrayList();
            });
            List<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>> featurePhaseEffectBuilders = Features.getEnabledPhaseEffects(itemStack, phase);
            List<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>> phaseEffectBuilders = new ArrayList(mainPhaseEffectBuilders);
            phaseEffectBuilders.addAll(featurePhaseEffectBuilders);
            phaseEffectBuilders.addAll(getGlobalEffectBuilders(phase, player, gunClientState, itemStack, hitResult, thirdPersonOnly));
            if (!phaseEffectBuilders.isEmpty()) {
               Minecraft mc = Minecraft.m_91087_();
               float maxDistance = Math.min((float)(mc.f_91066_.m_193772_() * 16), 200.0F);
               if (mc.f_91066_.m_92176_() != CameraType.FIRST_PERSON) {
                  thirdPersonOnly = true;
               }

               float distanceToTarget = 0.0F;
               if (hitResult != null) {
                  distanceToTarget = Mth.m_14036_((float)Math.sqrt(hitResult.m_82448_(player)) - 0.5F, 0.0F, maxDistance);
               } else {
                  distanceToTarget = maxDistance;
               }

               GunStatePoseProvider gunStatePoseProvider = GunStatePoseProvider.getInstance();
               Vec3 startPosition = null;
               if (!thirdPersonOnly) {
                  startPosition = gunStatePoseProvider.getPosition(gunClientState, GunStatePoseProvider.PoseContext.FIRST_PERSON_MUZZLE);
                  if (startPosition == null) {
                     startPosition = gunStatePoseProvider.getPosition(gunClientState, GunStatePoseProvider.PoseContext.FIRST_PERSON_MUZZLE_FLASH);
                  }

                  if (startPosition == null) {
                     startPosition = gunStatePoseProvider.getPosition(gunClientState, GunStatePoseProvider.PoseContext.THIRD_PERSON_MUZZLE);
                  }
               }

               if (startPosition == null) {
                  startPosition = gunStatePoseProvider.getPosition(gunClientState, GunStatePoseProvider.PoseContext.THIRD_PERSON_MUZZLE_FLASH);
               }

               Iterator var17 = phaseEffectBuilders.iterator();

               while(var17.hasNext()) {
                  Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> supplier = (Supplier)var17.next();
                  EffectBuilder<? extends EffectBuilder<?, ?>, ?> builder = (EffectBuilder)supplier.get();
                  if (!builder.getCompatiblePhases().contains(phase)) {
                     throw new IllegalStateException("Effect builder " + builder + " is not compatible with phase '" + phase + "'. Check how you construct item: " + item.m_7626_(itemStack));
                  }

                  boolean isEffectAttached = builder.isEffectAttached();
                  PoseProvider poseProvider = null;
                  PositionProvider positionProvider = null;
                  if (isEffectAttached) {
                     if (!thirdPersonOnly && gunStatePoseProvider.getPose(gunClientState, GunStatePoseProvider.PoseContext.FIRST_PERSON_MUZZLE_FLASH) != null) {
                        poseProvider = () -> {
                           return gunStatePoseProvider.getPose(gunClientState, GunStatePoseProvider.PoseContext.FIRST_PERSON_MUZZLE_FLASH);
                        };
                     } else if (!thirdPersonOnly && gunStatePoseProvider.getPose(gunClientState, GunStatePoseProvider.PoseContext.FIRST_PERSON_MUZZLE) != null) {
                        poseProvider = () -> {
                           return gunStatePoseProvider.getPose(gunClientState, GunStatePoseProvider.PoseContext.FIRST_PERSON_MUZZLE);
                        };
                     } else if (gunStatePoseProvider.getPose(gunClientState, GunStatePoseProvider.PoseContext.THIRD_PERSON_MUZZLE_FLASH) != null) {
                        poseProvider = () -> {
                           return gunStatePoseProvider.getPose(gunClientState, GunStatePoseProvider.PoseContext.THIRD_PERSON_MUZZLE_FLASH);
                        };
                     } else if (gunStatePoseProvider.getPose(gunClientState, GunStatePoseProvider.PoseContext.THIRD_PERSON_MUZZLE) != null) {
                        poseProvider = () -> {
                           return gunStatePoseProvider.getPose(gunClientState, GunStatePoseProvider.PoseContext.THIRD_PERSON_MUZZLE);
                        };
                     }

                     if (!thirdPersonOnly && gunStatePoseProvider.getPositionAndDirection(gunClientState, GunStatePoseProvider.PoseContext.FIRST_PERSON_MUZZLE_FLASH) != null) {
                        positionProvider = () -> {
                           return gunStatePoseProvider.getPositionAndDirection(gunClientState, GunStatePoseProvider.PoseContext.FIRST_PERSON_MUZZLE_FLASH);
                        };
                     } else if (!thirdPersonOnly && gunStatePoseProvider.getPositionAndDirection(gunClientState, GunStatePoseProvider.PoseContext.FIRST_PERSON_MUZZLE) != null) {
                        positionProvider = () -> {
                           return gunStatePoseProvider.getPositionAndDirection(gunClientState, GunStatePoseProvider.PoseContext.FIRST_PERSON_MUZZLE);
                        };
                     }
                  } else {
                     if (!thirdPersonOnly && gunStatePoseProvider.getPose(gunClientState, GunStatePoseProvider.PoseContext.FIRST_PERSON_MUZZLE) != null) {
                        poseProvider = () -> {
                           return gunStatePoseProvider.getPose(gunClientState, GunStatePoseProvider.PoseContext.FIRST_PERSON_MUZZLE);
                        };
                     } else if (!thirdPersonOnly && gunStatePoseProvider.getPose(gunClientState, GunStatePoseProvider.PoseContext.FIRST_PERSON_MUZZLE_FLASH) != null) {
                        poseProvider = () -> {
                           return gunStatePoseProvider.getPose(gunClientState, GunStatePoseProvider.PoseContext.FIRST_PERSON_MUZZLE_FLASH);
                        };
                     }

                     if (!thirdPersonOnly && gunStatePoseProvider.getPositionAndDirection(gunClientState, GunStatePoseProvider.PoseContext.FIRST_PERSON_MUZZLE) != null) {
                        positionProvider = () -> {
                           return gunStatePoseProvider.getPositionAndDirection(gunClientState, GunStatePoseProvider.PoseContext.FIRST_PERSON_MUZZLE);
                        };
                     } else if (!thirdPersonOnly && gunStatePoseProvider.getPositionAndDirection(gunClientState, GunStatePoseProvider.PoseContext.FIRST_PERSON_MUZZLE_FLASH) != null) {
                        positionProvider = () -> {
                           return gunStatePoseProvider.getPositionAndDirection(gunClientState, GunStatePoseProvider.PoseContext.FIRST_PERSON_MUZZLE_FLASH);
                        };
                     }
                  }

                  EffectBuilder.Context effectBuilderContext = (new EffectBuilder.Context()).withGunState(gunClientState).withStartPosition(startPosition).withDistance(distanceToTarget).withRandomization(0.0F).withVertexConsumerTransformer(VertexConsumers.PARTICLE).withPoseProvider(poseProvider).withPositionProvider(positionProvider).withDamage(damage).withHitResult(hitResult);
                  Effect effect = builder.build(effectBuilderContext);
                  LOGGER.debug("Launching effect {}", effect.getName());
                  effect.launch(player);
               }

            }
         }
      }
   }

   public static void broadcast(Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> effectSupplier, Player sourcePlayer, GunClientState state, LivingEntity targetEntity, SimpleHitResult hitResult) {
      Level level = MiscUtil.getLevel(sourcePlayer);
      if (!level.f_46443_) {
         Vec3 targetPos = targetEntity.m_20191_().m_82399_();
         Iterator var7 = ((ServerLevel)level).m_8795_((p) -> {
            return true;
         }).iterator();

         while(var7.hasNext()) {
            ServerPlayer nearbyPlayer = (ServerPlayer)var7.next();
            if (nearbyPlayer.m_20238_(targetPos) < 22500.0D) {
               Network.networkChannel.send(PacketDistributor.PLAYER.with(() -> {
                  return nearbyPlayer;
               }), new EffectBroadcastPacket(sourcePlayer.m_19879_(), state.getId(), EffectRegistry.getEffectId(((EffectBuilder)effectSupplier.get()).getName()), targetPos, hitResult, false));
            }
         }

      }
   }
}
