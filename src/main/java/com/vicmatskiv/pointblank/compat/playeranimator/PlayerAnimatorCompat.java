package com.vicmatskiv.pointblank.compat.playeranimator;

import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.Util;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.loading.LoadingModList;
import net.minecraftforge.fml.loading.moddiscovery.ModFileInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class PlayerAnimatorCompat implements ResourceManagerReloadListener {
   private static final Logger LOGGER = LogManager.getLogger("pointblank");
   private static PlayerAnimatorCompat instance;
   private Function<List<PlayerAnimationType>, List<String>> animationsToPlay;
   private static final PlayerAnimationRegistry<?> noOpRegistry = new PlayerAnimationRegistry<Object>() {
      public void register(String ownerId, Supplier<Reader> reader) {
      }

      public boolean isRegistered(String ownerId) {
         return false;
      }

      public List<PlayerAnimation<Object>> getAnimations(String ownerId, PlayerAnimationType animationType) {
         return Collections.emptyList();
      }

      public void reload() {
      }
   };

   public static PlayerAnimatorCompat getInstance() {
      if (instance == null) {
         ModFileInfo compatModFileInfo = LoadingModList.get().getModFileById("playeranimator");
         if (compatModFileInfo != null) {
            String playerAnimatorClassName = "com.vicmatskiv.pointblank.compat.playeranimator.PlayerAnimatorCompatImpl";

            try {
               Class<?> playerAnimatorClass = Class.forName(playerAnimatorClassName);
               instance = (PlayerAnimatorCompat)playerAnimatorClass.getDeclaredConstructor().newInstance();
               LOGGER.info("Compatibility with Player Animator version {} enabled", compatModFileInfo.versionString());
            } catch (ClassNotFoundException | InvocationTargetException | InstantiationException | IllegalAccessException | IllegalArgumentException | NoSuchMethodException | SecurityException | NoClassDefFoundError var3) {
               LOGGER.error("Player Animator mod version {} detected, but compatibility could not be enabled. This is likely due to an outdated and/or incompatible version of the Player Animator mod. ", compatModFileInfo.versionString(), var3);
            }
         }

         if (instance == null) {
            instance = new PlayerAnimatorCompat() {
            };
         }
      }

      return instance;
   }

   protected PlayerAnimatorCompat() {
      this.init();
   }

   protected void init() {
      this.animationsToPlay = Util.m_143827_(PlayerAnimatorCompat::getAnimationsToPlay);
   }

   public void m_6213_(ResourceManager resourceManager) {
      this.init();
   }

   public boolean isPlayerAnimatorLoaded() {
      return false;
   }

   public void registerAnimationTypes() {
   }

   public boolean isEnabled() {
      return false;
   }

   public PlayerAnimationRegistry<?> getAnimationRegistry() {
      return noOpRegistry;
   }

   public void handlePlayerThirdPersonMovement(Player player, float partialTick) {
   }

   public void playAnimation(Player player, String ownerId, String fallbackOwnerId, String animationName) {
   }

   public void stopAnimation(Player player, PlayerAnimationPartGroup animationLayerType) {
   }

   public void clearAll(Player player) {
   }

   protected void aux(Player player) {
   }

   public void playEnsemble(Player player, String ownerId, String fallbackOwnerId, List<PlayerAnimationType> animationTypes) {
      List<String> animationNames = (List)this.animationsToPlay.apply(animationTypes);
      Iterator var6 = animationNames.iterator();

      while(var6.hasNext()) {
         String animationName = (String)var6.next();
         this.playAnimation(player, ownerId, fallbackOwnerId, animationName);
      }

      this.aux(player);
   }

   private static List<String> getAnimationsToPlay(List<PlayerAnimationType> types) {
      List<String> animationsToPlay = new ArrayList();
      Iterator var2 = PlayerAnimationType.compose(types).entrySet().iterator();

      while(var2.hasNext()) {
         Entry<PlayerAnimationPartGroup, PlayerAnimationType> e = (Entry)var2.next();
         PlayerAnimationPartGroup group = (PlayerAnimationPartGroup)e.getKey();
         PlayerAnimationType animationType = (PlayerAnimationType)e.getValue();
         String animationName = constructAnimationName(animationType.getBaseAnimationName(), group);
         animationsToPlay.add(animationName);
      }

      return animationsToPlay;
   }

   private static String constructAnimationName(String baseAnimationName, PlayerAnimationPartGroup group) {
      return baseAnimationName + "." + group.getGroupName();
   }
}
