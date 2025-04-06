package com.vicmatskiv.pointblank.compat.playeranimator;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.data.gson.AnimationSerializing;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;

public class PlayerAnimationRegistryImpl implements PlayerAnimationRegistry<KeyframeAnimation> {
   private static final Gson GSON = new Gson();
   private static final String ATTR_NAME = "name";
   static final KeyframeAnimation AUX_ANIMATION = createAux();
   private final Map<String, Map<PlayerAnimationType, List<PlayerAnimation<KeyframeAnimation>>>> registeredAnimations = new HashMap();
   private final Map<String, List<Supplier<Reader>>> registrations = new HashMap();

   PlayerAnimationRegistryImpl() {
   }

   public void reload() {
      this.registeredAnimations.clear();
      Iterator var1 = this.registrations.entrySet().iterator();

      while(var1.hasNext()) {
         Entry<String, List<Supplier<Reader>>> e = (Entry)var1.next();
         Iterator var3 = ((List)e.getValue()).iterator();

         while(var3.hasNext()) {
            Supplier<Reader> readerFactory = (Supplier)var3.next();
            this.read((String)e.getKey(), readerFactory);
         }
      }

   }

   public boolean isRegistered(String ownerId) {
      return this.registrations.containsKey(ownerId);
   }

   public void register(String ownerId, Supplier<Reader> readerFactory) {
      ((List)this.registrations.computeIfAbsent(ownerId, (o) -> {
         return new ArrayList();
      })).add(readerFactory);
      this.read(ownerId, readerFactory);
   }

   private void read(String ownerId, Supplier<Reader> readerFactory) {
      try {
         Reader reader = (Reader)readerFactory.get();

         try {
            PlayerAnimationPreprocessor.preprocess(reader, (outputReader) -> {
               this.readOne(ownerId, outputReader);
            });
         } catch (Throwable var7) {
            if (reader != null) {
               try {
                  reader.close();
               } catch (Throwable var6) {
                  var7.addSuppressed(var6);
               }
            }

            throw var7;
         }

         if (reader != null) {
            reader.close();
         }
      } catch (IOException var8) {
         var8.printStackTrace();
      }

   }

   private void readOne(String ownerId, Reader reader) {
      Iterator var3 = AnimationSerializing.deserializeAnimation(reader).iterator();

      while(var3.hasNext()) {
         KeyframeAnimation keyframeAnimation = (KeyframeAnimation)var3.next();
         if (keyframeAnimation.extraData != null) {
            Object var6 = keyframeAnimation.extraData.get("name");
            if (var6 instanceof String) {
               String encoded = (String)var6;
               String animationName = ((String)GSON.fromJson(encoded, String.class)).toLowerCase(Locale.ROOT);
               int index = animationName.lastIndexOf(46);
               if (index > 0) {
                  String baseAnimationName = animationName.substring(0, index);
                  String group = animationName.substring(index + 1);
                  PlayerAnimationType playerAnimationType = PlayerAnimationType.fromBaseAnimationName(baseAnimationName);
                  if (playerAnimationType != null) {
                     Map<PlayerAnimationType, List<PlayerAnimation<KeyframeAnimation>>> keyframeAnimations = (Map)this.registeredAnimations.computeIfAbsent(ownerId, (key) -> {
                        return new HashMap();
                     });
                     List<PlayerAnimation<KeyframeAnimation>> playerAnimations = (List)keyframeAnimations.computeIfAbsent(playerAnimationType, (t) -> {
                        return new ArrayList();
                     });
                     playerAnimations.add(new PlayerAnimation(animationName, ownerId, keyframeAnimation, PlayerAnimationPartGroup.fromName(group)));
                  }
               }
            }
         }
      }

   }

   public List<PlayerAnimation<KeyframeAnimation>> getAnimations(String ownerId, PlayerAnimationType animationType) {
      Map<PlayerAnimationType, List<PlayerAnimation<KeyframeAnimation>>> ownerAnimations = (Map)this.registeredAnimations.get(ownerId);
      List<PlayerAnimation<KeyframeAnimation>> result = null;
      if (ownerAnimations != null) {
         result = (List)ownerAnimations.get(animationType);
      }

      if (result == null) {
         result = Collections.emptyList();
      }

      return result;
   }

   private static KeyframeAnimation createAux() {
      JsonObject auxGroup = new JsonObject();
      auxGroup.addProperty("format_version", "1.8.0");
      JsonObject auxAnimation = new JsonObject();
      auxAnimation.addProperty("loop", true);
      JsonObject bonesObject = new JsonObject();
      JsonObject bodyObject = new JsonObject();
      bonesObject.add("body", bodyObject);
      auxAnimation.add("bones", bonesObject);
      JsonObject animations = new JsonObject();
      animations.add("aux", auxAnimation);
      auxGroup.add("animations", animations);
      String s = GSON.toJson(auxGroup);
      List<KeyframeAnimation> auxAnimations = AnimationSerializing.deserializeAnimation(new StringReader(s));
      return (KeyframeAnimation)auxAnimations.get(0);
   }
}
