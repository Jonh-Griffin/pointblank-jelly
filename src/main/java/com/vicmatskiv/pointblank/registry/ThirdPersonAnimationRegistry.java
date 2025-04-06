package com.vicmatskiv.pointblank.registry;

import com.vicmatskiv.pointblank.compat.playeranimator.PlayerAnimationRegistry;
import com.vicmatskiv.pointblank.compat.playeranimator.PlayerAnimatorCompat;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Objects;
import java.util.function.Supplier;

public class ThirdPersonAnimationRegistry {
   public static final String DEFAULT_RIFLE_ANIMATIONS = "__DEFAULT_RIFLE_ANIMATIONS__";
   public static final String DEFAULT_PISTOL_ANIMATIONS = "__DEFAULT_PISTOL_ANIMATIONS__";
   private static final PlayerAnimationRegistry<?> REGISTRY = PlayerAnimatorCompat.getInstance().getAnimationRegistry();

   private static Reader getResourceReader(String resourceName) {
      return new InputStreamReader((InputStream)Objects.requireNonNull(ThirdPersonAnimationRegistry.class.getResourceAsStream(resourceName)));
   }

   public static void register(String ownerId, Supplier<Reader> resourceReaderFactory) {
      REGISTRY.register(ownerId, resourceReaderFactory);
   }

   public static void register(String ownerId, String resourceName) {
      register(ownerId, () -> {
         return getResourceReader(resourceName);
      });
   }

   public static void init() {
      register("__DEFAULT_RIFLE_ANIMATIONS__", "/assets/pointblank/animations/player/rifle.animation.json");
      register("__DEFAULT_PISTOL_ANIMATIONS__", "/assets/pointblank/animations/player/pistol.animation.json");
   }
}
