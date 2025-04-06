package com.vicmatskiv.pointblank.client.controller;

import com.eliotlash.mclib.math.Constant;
import com.eliotlash.mclib.math.IValue;
import com.vicmatskiv.pointblank.client.RealtimeLinearEaser;
import com.vicmatskiv.pointblank.util.ClientUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animatable.model.CoreGeoModel;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.EasingType;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animation.AnimationController.AnimationStateHandler;
import software.bernie.geckolib.core.animation.AnimationController.State;
import software.bernie.geckolib.core.animation.AnimationProcessor.QueuedAnimation;
import software.bernie.geckolib.core.keyframe.AnimationPoint;
import software.bernie.geckolib.core.keyframe.BoneAnimation;
import software.bernie.geckolib.core.keyframe.BoneAnimationQueue;
import software.bernie.geckolib.core.keyframe.Keyframe;
import software.bernie.geckolib.core.keyframe.KeyframeLocation;
import software.bernie.geckolib.core.keyframe.KeyframeStack;
import software.bernie.geckolib.core.keyframe.event.CustomInstructionKeyframeEvent;
import software.bernie.geckolib.core.keyframe.event.ParticleKeyframeEvent;
import software.bernie.geckolib.core.keyframe.event.SoundKeyframeEvent;
import software.bernie.geckolib.core.keyframe.event.data.CustomInstructionKeyframeData;
import software.bernie.geckolib.core.keyframe.event.data.KeyFrameData;
import software.bernie.geckolib.core.keyframe.event.data.ParticleKeyframeData;
import software.bernie.geckolib.core.keyframe.event.data.SoundKeyframeData;
import software.bernie.geckolib.core.molang.MolangParser;
import software.bernie.geckolib.core.object.Axis;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.state.BoneSnapshot;

public class BlendingAnimationController<T extends GeoAnimatable> extends AnimationController<T> {
   private static final String ANY_ANIMATION = "_any";
   protected final Map<String, BoneSnapshot> topSnapshots = new Object2ObjectOpenHashMap();
   private BiFunction<Player, BlendingAnimationController<T>, Double> speedProvider = (p, c) -> {
      return 1.0D;
   };
   private boolean justStopped = true;
   private final Set<KeyFrameData> executedKeyFrames = new ObjectOpenHashSet();
   private final Map<String, Map<String, TransitionDescriptor>> transitionDescriptors = new HashMap();
   protected boolean shouldResetTickOnTransition = true;
   private final TransitionDescriptor defaultTransitionDescriptor;
   protected QueuedAnimation previousAnimation;
   private final RealtimeLinearEaser currentSpeed = new RealtimeLinearEaser(400L);
   private double animationProgress;

   public BlendingAnimationController(T animatable, String name, int defaultTransitionTickTime, boolean defaultResetTickAfterTransition, AnimationStateHandler<T> animationHandler) {
      super(animatable, name, defaultTransitionTickTime, animationHandler);
      this.defaultTransitionDescriptor = new TransitionDescriptor(defaultTransitionTickTime, defaultResetTickAfterTransition, 1.0D);
   }

   public BlendingAnimationController<T> withTransition(String targetAnimation, int tickTime, boolean resetTickAfterTransition) {
      Map<String, TransitionDescriptor> fromMap = (Map)this.transitionDescriptors.computeIfAbsent(targetAnimation, (k) -> {
         return new HashMap();
      });
      fromMap.put("_any", new TransitionDescriptor(tickTime, resetTickAfterTransition, 1.0D));
      return this;
   }

   public BlendingAnimationController<T> withTransition(String sourceAnimation, String targetAnimation, int tickTime, boolean resetTickAfterTransition) {
      return this.withTransition(sourceAnimation, targetAnimation, tickTime, resetTickAfterTransition, 1.0D);
   }

   public BlendingAnimationController<T> withTransition(String sourceAnimation, String targetAnimation, int tickTime, boolean resetTickAfterTransition, double speed) {
      Map<String, TransitionDescriptor> fromMap = (Map)this.transitionDescriptors.computeIfAbsent(targetAnimation, (k) -> {
         return new HashMap();
      });
      fromMap.put(sourceAnimation, new TransitionDescriptor(tickTime, resetTickAfterTransition, speed));
      return this;
   }

   public BlendingAnimationController<T> withSpeedProvider(BiFunction<Player, BlendingAnimationController<T>, Double> speedProvider) {
      this.speedProvider = speedProvider;
      return this;
   }

   public RawAnimation getTriggeredAnimation() {
      return this.triggeredAnimation;
   }

   private TransitionDescriptor getCurrentTransitionDescriptor() {
      if (this.currentAnimation == null) {
         return this.defaultTransitionDescriptor;
      } else {
         String currentAnimationName = this.currentAnimation.animation().name();
         Map<String, TransitionDescriptor> fromMap = (Map)this.transitionDescriptors.get(currentAnimationName);
         if (fromMap == null) {
            return this.defaultTransitionDescriptor;
         } else if (this.previousAnimation != null) {
            String previousAnimationName = this.previousAnimation.animation().name();
            return (TransitionDescriptor)fromMap.getOrDefault(previousAnimationName, this.defaultTransitionDescriptor);
         } else {
            return (TransitionDescriptor)fromMap.getOrDefault("_any", this.defaultTransitionDescriptor);
         }
      }
   }

   protected PlayState handleAnimationState(AnimationState<T> state) {
      if (this.triggeredAnimation != null) {
         if (this.currentRawAnimation != this.triggeredAnimation) {
            this.previousAnimation = this.currentAnimation;
            this.currentAnimation = null;
         }

         this.setAnimation(this.triggeredAnimation);
         if (!this.hasAnimationFinished() && (!this.handlingTriggeredAnimations || this.stateHandler.handle(state) == PlayState.CONTINUE)) {
            return PlayState.CONTINUE;
         }

         this.triggeredAnimation = null;
         this.needsAnimationReload = true;
      }

      return this.stateHandler.handle(state);
   }

   public void clearAll() {
      this.topSnapshots.clear();
      this.animationQueue.clear();
      this.currentRawAnimation = null;
      this.currentAnimation = null;
      this.triggeredAnimation = null;
   }

   public boolean tryTriggerAnimation(String animName) {
      RawAnimation anim = (RawAnimation)this.triggerableAnimations.get(animName);
      if (anim == null) {
         return false;
      } else {
         this.triggeredAnimation = anim;
         if (this.animationState == State.STOPPED) {
            this.animationState = State.TRANSITIONING;
            this.shouldResetTick = true;
            this.justStartedTransition = true;
         }

         return true;
      }
   }

   public void setAnimation(RawAnimation rawAnimation) {
      if (rawAnimation != null && !rawAnimation.getAnimationStages().isEmpty()) {
         if (this.needsAnimationReload || !rawAnimation.equals(this.currentRawAnimation)) {
            if (this.lastModel != null) {
               Queue<QueuedAnimation> animations = this.lastModel.getAnimationProcessor().buildAnimationQueue(this.animatable, rawAnimation);
               if (animations != null) {
                  this.animationQueue = animations;
                  this.currentRawAnimation = rawAnimation;
                  this.shouldResetTick = true;
                  this.animationState = State.TRANSITIONING;
                  this.justStartedTransition = true;
                  this.needsAnimationReload = false;
                  TransitionDescriptor transitionDescriptor = this.getCurrentTransitionDescriptor();
                  this.transitionLength = (double)transitionDescriptor.length;
                  this.shouldResetTickOnTransition = transitionDescriptor.resetTickOnTransition;
                  return;
               }
            }

            this.stop();
         }

      } else {
         this.stop();
      }
   }

   private void ensureTopSnapshots(Map<String, CoreGeoBone> bones) {
      Iterator var2 = bones.entrySet().iterator();

      while(var2.hasNext()) {
         Entry<String, CoreGeoBone> e = (Entry)var2.next();
         String boneName = (String)e.getKey();
         if (!this.topSnapshots.containsKey(boneName)) {
            this.topSnapshots.put(boneName, BoneSnapshot.copy(((CoreGeoBone)e.getValue()).getInitialSnapshot()));
         }
      }

   }

   public void process(CoreGeoModel<T> model, AnimationState<T> state, Map<String, CoreGeoBone> origBones, Map<String, BoneSnapshot> snapshotsIgnored, double seekTime, boolean crashWhenCantFindBone) {
      this.ensureTopSnapshots(origBones);
      this.processInternal(model, state, origBones, this.topSnapshots, seekTime, crashWhenCantFindBone);
      Iterator var8 = this.getBoneAnimationQueues().values().iterator();

      while(var8.hasNext()) {
         BoneAnimationQueue boneAnimationQueue = (BoneAnimationQueue)var8.next();
         CoreGeoBone bone = boneAnimationQueue.bone();
         BoneSnapshot snapshot = (BoneSnapshot)this.topSnapshots.get(bone.getName());
         BoneSnapshot initialSnapshot = bone.getInitialSnapshot();
         AnimationPoint rotXPoint = (AnimationPoint)boneAnimationQueue.rotationXQueue().peek();
         AnimationPoint rotYPoint = (AnimationPoint)boneAnimationQueue.rotationYQueue().peek();
         AnimationPoint rotZPoint = (AnimationPoint)boneAnimationQueue.rotationZQueue().peek();
         AnimationPoint posXPoint = (AnimationPoint)boneAnimationQueue.positionXQueue().peek();
         AnimationPoint posYPoint = (AnimationPoint)boneAnimationQueue.positionYQueue().peek();
         AnimationPoint posZPoint = (AnimationPoint)boneAnimationQueue.positionZQueue().peek();
         AnimationPoint scaleXPoint = (AnimationPoint)boneAnimationQueue.scaleXQueue().peek();
         AnimationPoint scaleYPoint = (AnimationPoint)boneAnimationQueue.scaleYQueue().peek();
         AnimationPoint scaleZPoint = (AnimationPoint)boneAnimationQueue.scaleZQueue().peek();
         EasingType easingType = (EasingType)this.overrideEasingTypeFunction.apply(this.animatable);
         float posX;
         float posY;
         float posZ;
         if (rotXPoint != null && rotYPoint != null && rotZPoint != null) {
            posX = (float)EasingType.lerpWithOverride(rotXPoint, easingType);
            posY = (float)EasingType.lerpWithOverride(rotYPoint, easingType);
            posZ = (float)EasingType.lerpWithOverride(rotZPoint, easingType);
            snapshot.updateRotation(posX + initialSnapshot.getRotX(), posY + initialSnapshot.getRotY(), posZ + initialSnapshot.getRotZ());
            snapshot.startRotAnim();
         }

         if (posXPoint != null && posYPoint != null && posZPoint != null) {
            posX = (float)EasingType.lerpWithOverride(posXPoint, easingType);
            posY = (float)EasingType.lerpWithOverride(posYPoint, easingType);
            posZ = (float)EasingType.lerpWithOverride(posZPoint, easingType);
            snapshot.updateOffset(posX, posY, posZ);
            snapshot.startPosAnim();
         }

         if (scaleXPoint != null && scaleYPoint != null && scaleZPoint != null) {
            snapshot.updateScale((float)EasingType.lerpWithOverride(scaleXPoint, easingType), (float)EasingType.lerpWithOverride(scaleYPoint, easingType), (float)EasingType.lerpWithOverride(scaleZPoint, easingType));
            snapshot.startScaleAnim();
         }
      }

   }

   public double getAnimationSpeed() {
      double targetSpeed = (Double)this.speedProvider.apply(ClientUtil.getClientPlayer(), this);
      return (double)this.currentSpeed.update((float)targetSpeed);
   }

   protected double adjustTick(double tick) {
      if (!this.shouldResetTick) {
         return this.getAnimationSpeed() * Math.max(tick - this.tickOffset, 0.0D);
      } else {
         if (this.getAnimationState() != State.STOPPED) {
            this.tickOffset = tick;
         }

         this.shouldResetTick = false;
         return 0.0D;
      }
   }

   public void processInternal(CoreGeoModel<T> model, AnimationState<T> state, Map<String, CoreGeoBone> origBones, Map<String, BoneSnapshot> snapshots, double seekTime, boolean crashWhenCantFindBone) {
      double adjustedTick = this.adjustTick(seekTime);
      this.lastModel = model;
      if (this.animationState == State.TRANSITIONING && adjustedTick >= this.transitionLength) {
         this.shouldResetTick = this.shouldResetTickOnTransition;
         this.animationState = State.RUNNING;
         adjustedTick = this.adjustTick(seekTime);
      }

      PlayState playState = this.handleAnimationState(state);
      if (playState != PlayState.STOP && (this.currentAnimation != null || !this.animationQueue.isEmpty())) {
         this.createInitialQueues(origBones.values());
         if (this.justStartedTransition && (this.shouldResetTick || this.justStopped)) {
            this.justStopped = false;
            adjustedTick = this.adjustTick(seekTime);
            if (this.currentAnimation == null) {
               this.animationState = State.TRANSITIONING;
            }
         } else if (this.currentAnimation == null) {
            this.shouldResetTick = true;
            this.animationState = State.TRANSITIONING;
            this.justStartedTransition = true;
            this.needsAnimationReload = false;
            adjustedTick = this.adjustTick(seekTime);
         } else if (this.animationState != State.TRANSITIONING) {
            this.animationState = State.RUNNING;
         }

         if (this.getAnimationState() == State.RUNNING) {
            this.processCurrentAnimation(adjustedTick, seekTime, crashWhenCantFindBone, snapshots);
         } else if (this.animationState == State.TRANSITIONING) {
            if (this.lastPollTime != seekTime && (adjustedTick == 0.0D || this.isJustStarting)) {
               this.justStartedTransition = false;
               this.lastPollTime = seekTime;
               this.currentAnimation = (QueuedAnimation)this.animationQueue.poll();
               this.resetEventKeyFrames();
               if (this.currentAnimation == null) {
                  return;
               }

               TransitionDescriptor transitionDescriptor = this.getCurrentTransitionDescriptor();
               this.transitionLength = (double)transitionDescriptor.length;
               this.shouldResetTickOnTransition = transitionDescriptor.resetTickOnTransition;
               this.saveSnapshotsForAnimation(this.currentAnimation, snapshots);
            }

            if (this.currentAnimation != null) {
               MolangParser.INSTANCE.setValue("query.anim_time", () -> {
                  return 0.0D;
               });
               BoneAnimation[] var25 = this.currentAnimation.animation().boneAnimations();
               int var12 = var25.length;

               for(int var13 = 0; var13 < var12; ++var13) {
                  BoneAnimation boneAnimation = var25[var13];
                  BoneAnimationQueue boneAnimationQueue = (BoneAnimationQueue)this.boneAnimationQueues.get(boneAnimation.boneName());
                  BoneSnapshot boneSnapshot = (BoneSnapshot)this.boneSnapshots.get(boneAnimation.boneName());
                  if (boneSnapshot != null) {
                     KeyframeStack<Keyframe<IValue>> rotationKeyFrames = boneAnimation.rotationKeyFrames();
                     KeyframeStack<Keyframe<IValue>> positionKeyFrames = boneAnimation.positionKeyFrames();
                     KeyframeStack<Keyframe<IValue>> scaleKeyFrames = boneAnimation.scaleKeyFrames();
                     double startTick = this.shouldResetTickOnTransition ? 0.0D : this.transitionLength;
                     if (!rotationKeyFrames.xKeyframes().isEmpty()) {
                        AnimationPoint xPointNext = getAnimationPointAtTick(rotationKeyFrames.xKeyframes(), startTick, true, Axis.X);
                        AnimationPoint yPointNext = getAnimationPointAtTick(rotationKeyFrames.yKeyframes(), startTick, true, Axis.Y);
                        AnimationPoint zPointNext = getAnimationPointAtTick(rotationKeyFrames.zKeyframes(), startTick, true, Axis.Z);
                        this.addNextRotation(boneAnimationQueue, (Keyframe)null, adjustedTick, this.transitionLength, boneSnapshot, boneSnapshot.getBone().getInitialSnapshot(), xPointNext, yPointNext, zPointNext);
                     }

                     if (!positionKeyFrames.xKeyframes().isEmpty()) {
                        this.addNextPosition(boneAnimationQueue, (Keyframe)null, adjustedTick, this.transitionLength, boneSnapshot, getAnimationPointAtTick(positionKeyFrames.xKeyframes(), startTick, false, Axis.X), getAnimationPointAtTick(positionKeyFrames.yKeyframes(), startTick, false, Axis.Y), getAnimationPointAtTick(positionKeyFrames.zKeyframes(), startTick, false, Axis.Z));
                     }

                     if (!scaleKeyFrames.xKeyframes().isEmpty()) {
                        this.addNextScale(boneAnimationQueue, (Keyframe)null, adjustedTick, this.transitionLength, boneSnapshot, getAnimationPointAtTick(scaleKeyFrames.xKeyframes(), startTick, false, Axis.X), getAnimationPointAtTick(scaleKeyFrames.yKeyframes(), startTick, false, Axis.Y), getAnimationPointAtTick(scaleKeyFrames.zKeyframes(), startTick, false, Axis.Z));
                     }
                  }
               }
            }
         }

         if (this.currentAnimation != null) {
            this.animationProgress = Mth.m_14008_(adjustedTick / this.currentAnimation.animation().length(), 0.0D, 1.0D);
         } else {
            this.animationProgress = 0.0D;
         }

      } else {
         this.animationState = State.STOPPED;
         this.justStopped = true;
      }
   }

   public double getAnimationProgress() {
      return this.animationProgress;
   }

   public void addNextRotation(BoneAnimationQueue boneAnimationQueue, Keyframe<?> keyFrame, double lerpedTick, double transitionLength, BoneSnapshot startSnapshot, BoneSnapshot initialSnapshot, AnimationPoint nextXPoint, AnimationPoint nextYPoint, AnimationPoint nextZPoint) {
      EasingType easingType = (EasingType)this.overrideEasingTypeFunction.apply(this.animatable);
      float nextX = (float)EasingType.lerpWithOverride(nextXPoint, easingType);
      float nextY = (float)EasingType.lerpWithOverride(nextYPoint, easingType);
      float nextZ = (float)EasingType.lerpWithOverride(nextZPoint, easingType);
      boneAnimationQueue.addRotationXPoint(keyFrame, lerpedTick, transitionLength, (double)(startSnapshot.getRotX() - initialSnapshot.getRotX()), (double)nextX);
      boneAnimationQueue.addRotationYPoint(keyFrame, lerpedTick, transitionLength, (double)(startSnapshot.getRotY() - initialSnapshot.getRotY()), (double)nextY);
      boneAnimationQueue.addRotationZPoint(keyFrame, lerpedTick, transitionLength, (double)(startSnapshot.getRotZ() - initialSnapshot.getRotZ()), (double)nextZ);
   }

   public void addNextPosition(BoneAnimationQueue boneAnimationQueue, Keyframe<?> keyFrame, double lerpedTick, double transitionLength, BoneSnapshot startSnapshot, AnimationPoint nextXPoint, AnimationPoint nextYPoint, AnimationPoint nextZPoint) {
      EasingType easingType = (EasingType)this.overrideEasingTypeFunction.apply(this.animatable);
      float nextX = (float)EasingType.lerpWithOverride(nextXPoint, easingType);
      float nextY = (float)EasingType.lerpWithOverride(nextYPoint, easingType);
      float nextZ = (float)EasingType.lerpWithOverride(nextZPoint, easingType);
      boneAnimationQueue.addPosXPoint(keyFrame, lerpedTick, transitionLength, (double)startSnapshot.getOffsetX(), (double)nextX);
      boneAnimationQueue.addPosYPoint(keyFrame, lerpedTick, transitionLength, (double)startSnapshot.getOffsetY(), (double)nextY);
      boneAnimationQueue.addPosZPoint(keyFrame, lerpedTick, transitionLength, (double)startSnapshot.getOffsetZ(), (double)nextZ);
   }

   public void addNextScale(BoneAnimationQueue boneAnimationQueue, Keyframe<?> keyFrame, double lerpedTick, double transitionLength, BoneSnapshot startSnapshot, AnimationPoint nextXPoint, AnimationPoint nextYPoint, AnimationPoint nextZPoint) {
      EasingType easingType = (EasingType)this.overrideEasingTypeFunction.apply(this.animatable);
      float nextX = (float)EasingType.lerpWithOverride(nextXPoint, easingType);
      float nextY = (float)EasingType.lerpWithOverride(nextYPoint, easingType);
      float nextZ = (float)EasingType.lerpWithOverride(nextZPoint, easingType);
      boneAnimationQueue.addScaleXPoint(keyFrame, lerpedTick, transitionLength, (double)startSnapshot.getScaleX(), (double)nextX);
      boneAnimationQueue.addScaleYPoint(keyFrame, lerpedTick, transitionLength, (double)startSnapshot.getScaleY(), (double)nextY);
      boneAnimationQueue.addScaleZPoint(keyFrame, lerpedTick, transitionLength, (double)startSnapshot.getScaleZ(), (double)nextZ);
   }

   private void saveSnapshotsForAnimation(QueuedAnimation animation, Map<String, BoneSnapshot> snapshots) {
      Iterator var3 = snapshots.values().iterator();

      while(true) {
         while(true) {
            BoneSnapshot snapshot;
            do {
               if (!var3.hasNext()) {
                  return;
               }

               snapshot = (BoneSnapshot)var3.next();
            } while(animation.animation().boneAnimations() == null);

            BoneAnimation[] var5 = animation.animation().boneAnimations();
            int var6 = var5.length;

            for(int var7 = 0; var7 < var6; ++var7) {
               BoneAnimation boneAnimation = var5[var7];
               if (boneAnimation.boneName().equals(snapshot.getBone().getName())) {
                  this.boneSnapshots.put(boneAnimation.boneName(), BoneSnapshot.copy(snapshot));
                  break;
               }
            }
         }
      }
   }

   private void createInitialQueues(Collection<CoreGeoBone> modelRendererList) {
      this.boneAnimationQueues.clear();
      Iterator var2 = modelRendererList.iterator();

      while(var2.hasNext()) {
         CoreGeoBone bone = (CoreGeoBone)var2.next();
         this.boneAnimationQueues.put(bone.getName(), new BoneAnimationQueue(bone));
      }

   }

   private void processCurrentAnimation(double adjustedTick, double seekTime, boolean crashWhenCantFindBone, Map<String, BoneSnapshot> snapshots) {
      if (adjustedTick >= this.currentAnimation.animation().length()) {
         if (this.currentAnimation.loopType().shouldPlayAgain(this.animatable, this, this.currentAnimation.animation())) {
            if (this.animationState != State.PAUSED) {
               this.shouldResetTick = true;
               adjustedTick = this.adjustTick(seekTime);
               this.resetEventKeyFrames();
            }
         } else {
            QueuedAnimation nextAnimation = (QueuedAnimation)this.animationQueue.peek();
            this.resetEventKeyFrames();
            if (nextAnimation == null) {
               this.animationState = State.STOPPED;
               return;
            }

            this.animationState = State.TRANSITIONING;
            this.shouldResetTick = true;
            adjustedTick = this.adjustTick(seekTime);
            if (this.currentAnimation != null) {
               this.previousAnimation = this.currentAnimation;
            }

            this.currentAnimation = (QueuedAnimation)this.animationQueue.poll();
            TransitionDescriptor transitionDescriptor = this.getCurrentTransitionDescriptor();
            this.transitionLength = (double)transitionDescriptor.length;
            this.shouldResetTickOnTransition = transitionDescriptor.resetTickOnTransition;
            this.saveSnapshotsForAnimation(this.currentAnimation, snapshots);
         }
      }

      MolangParser.INSTANCE.setMemoizedValue("query.anim_time", () -> {
         return adjustedTick / 20.0D;
      });
      BoneAnimation[] var9 = this.currentAnimation.animation().boneAnimations();
      int var10 = var9.length;

      int var11;
      for(var11 = 0; var11 < var10; ++var11) {
         BoneAnimation boneAnimation = var9[var11];
         BoneAnimationQueue boneAnimationQueue = (BoneAnimationQueue)this.boneAnimationQueues.get(boneAnimation.boneName());
         if (boneAnimationQueue == null) {
            if (crashWhenCantFindBone) {
               throw new RuntimeException("Could not find bone: " + boneAnimation.boneName());
            }
         } else {
            KeyframeStack<Keyframe<IValue>> rotationKeyFrames = boneAnimation.rotationKeyFrames();
            KeyframeStack<Keyframe<IValue>> positionKeyFrames = boneAnimation.positionKeyFrames();
            KeyframeStack<Keyframe<IValue>> scaleKeyFrames = boneAnimation.scaleKeyFrames();
            AnimationPoint xp;
            AnimationPoint yp;
            AnimationPoint zp;
            if (!rotationKeyFrames.xKeyframes().isEmpty()) {
               xp = getAnimationPointAtTick(rotationKeyFrames.xKeyframes(), adjustedTick, true, Axis.X);
               yp = getAnimationPointAtTick(rotationKeyFrames.yKeyframes(), adjustedTick, true, Axis.Y);
               zp = getAnimationPointAtTick(rotationKeyFrames.zKeyframes(), adjustedTick, true, Axis.Z);
               boneAnimationQueue.addRotations(xp, yp, zp);
            }

            if (!positionKeyFrames.xKeyframes().isEmpty()) {
               xp = getAnimationPointAtTick(positionKeyFrames.xKeyframes(), adjustedTick, false, Axis.X);
               yp = getAnimationPointAtTick(positionKeyFrames.yKeyframes(), adjustedTick, false, Axis.Y);
               zp = getAnimationPointAtTick(positionKeyFrames.zKeyframes(), adjustedTick, false, Axis.Z);
               boneAnimationQueue.addPositions(xp, yp, zp);
            }

            if (!scaleKeyFrames.xKeyframes().isEmpty()) {
               boneAnimationQueue.addScales(getAnimationPointAtTick(scaleKeyFrames.xKeyframes(), adjustedTick, false, Axis.X), getAnimationPointAtTick(scaleKeyFrames.yKeyframes(), adjustedTick, false, Axis.Y), getAnimationPointAtTick(scaleKeyFrames.zKeyframes(), adjustedTick, false, Axis.Z));
            }
         }
      }

      adjustedTick += this.transitionLength;
      SoundKeyframeData[] var20 = this.currentAnimation.animation().keyFrames().sounds();
      var10 = var20.length;

      for(var11 = 0; var11 < var10; ++var11) {
         SoundKeyframeData keyframeData = var20[var11];
         if (adjustedTick >= keyframeData.getStartTick() && this.executedKeyFrames.add(keyframeData)) {
            if (this.soundKeyframeHandler == null) {
               break;
            }

            this.soundKeyframeHandler.handle(new SoundKeyframeEvent(this.animatable, adjustedTick, this, keyframeData));
         }
      }

      ParticleKeyframeData[] var21 = this.currentAnimation.animation().keyFrames().particles();
      var10 = var21.length;

      for(var11 = 0; var11 < var10; ++var11) {
         ParticleKeyframeData keyframeData = var21[var11];
         if (adjustedTick >= keyframeData.getStartTick() && this.executedKeyFrames.add(keyframeData)) {
            if (this.particleKeyframeHandler == null) {
               break;
            }

            this.particleKeyframeHandler.handle(new ParticleKeyframeEvent(this.animatable, adjustedTick, this, keyframeData));
         }
      }

      CustomInstructionKeyframeData[] var22 = this.currentAnimation.animation().keyFrames().customInstructions();
      var10 = var22.length;

      for(var11 = 0; var11 < var10; ++var11) {
         CustomInstructionKeyframeData keyframeData = var22[var11];
         if (adjustedTick >= keyframeData.getStartTick() && this.executedKeyFrames.add(keyframeData)) {
            if (this.customKeyframeHandler == null) {
               break;
            }

            this.customKeyframeHandler.handle(new CustomInstructionKeyframeEvent(this.animatable, adjustedTick, this, keyframeData));
         }
      }

      if (this.transitionLength == 0.0D && this.shouldResetTick && this.animationState == State.TRANSITIONING) {
         this.currentAnimation = (QueuedAnimation)this.animationQueue.poll();
      }

   }

   private static AnimationPoint getAnimationPointAtTick(List<Keyframe<IValue>> frames, double tick, boolean isRotation, Axis axis) {
      KeyframeLocation<Keyframe<IValue>> location = getCurrentKeyFrameLocation(frames, tick);
      Keyframe<IValue> currentFrame = location.keyframe();
      double startValue = currentFrame.startValue().get();
      double endValue = currentFrame.endValue().get();
      if (isRotation) {
         if (!(currentFrame.startValue() instanceof Constant)) {
            startValue = Math.toRadians(startValue);
            if (axis == Axis.X || axis == Axis.Y) {
               startValue *= -1.0D;
            }
         }

         if (!(currentFrame.endValue() instanceof Constant)) {
            endValue = Math.toRadians(endValue);
            if (axis == Axis.X || axis == Axis.Y) {
               endValue *= -1.0D;
            }
         }
      }

      return new AnimationPoint(currentFrame, location.startTick(), currentFrame.length(), startValue, endValue);
   }

   private static KeyframeLocation<Keyframe<IValue>> getCurrentKeyFrameLocation(List<Keyframe<IValue>> frames, double ageInTicks) {
      double totalFrameTime = 0.0D;
      Iterator var5 = frames.iterator();

      Keyframe frame;
      do {
         if (!var5.hasNext()) {
            return new KeyframeLocation((Keyframe)frames.get(frames.size() - 1), ageInTicks);
         }

         frame = (Keyframe)var5.next();
         totalFrameTime += frame.length();
      } while(!(totalFrameTime > ageInTicks));

      return new KeyframeLocation(frame, ageInTicks - (totalFrameTime - frame.length()));
   }

   private void resetEventKeyFrames() {
      this.executedKeyFrames.clear();
   }

   private static record TransitionDescriptor(int length, boolean resetTickOnTransition, double speed) {
      private TransitionDescriptor(int length, boolean resetTickOnTransition, double speed) {
         this.length = length;
         this.resetTickOnTransition = resetTickOnTransition;
         this.speed = speed;
      }

      public int length() {
         return this.length;
      }

      public boolean resetTickOnTransition() {
         return this.resetTickOnTransition;
      }

      public double speed() {
         return this.speed;
      }
   }
}
