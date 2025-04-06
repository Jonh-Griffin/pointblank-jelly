package com.vicmatskiv.pointblank.registry;

import com.vicmatskiv.pointblank.feature.AccuracyFeature;
import com.vicmatskiv.pointblank.feature.ActiveMuzzleFeature;
import com.vicmatskiv.pointblank.feature.AimingFeature;
import com.vicmatskiv.pointblank.feature.AmmoCapacityFeature;
import com.vicmatskiv.pointblank.feature.DamageFeature;
import com.vicmatskiv.pointblank.feature.Feature;
import com.vicmatskiv.pointblank.feature.FireModeFeature;
import com.vicmatskiv.pointblank.feature.GlowFeature;
import com.vicmatskiv.pointblank.feature.MuzzleFlashFeature;
import com.vicmatskiv.pointblank.feature.PartVisibilityFeature;
import com.vicmatskiv.pointblank.feature.PipFeature;
import com.vicmatskiv.pointblank.feature.RecoilFeature;
import com.vicmatskiv.pointblank.feature.ReticleFeature;
import com.vicmatskiv.pointblank.feature.SkinFeature;
import com.vicmatskiv.pointblank.feature.SoundFeature;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class FeatureTypeRegistry {
   private static int featureTypeId;
   private static final Map<Class<? extends Feature>, Integer> typeToId = new HashMap();
   private static final Map<Integer, Class<? extends Feature>> idToType = new HashMap();
   public static final int AIMING_FEATURE = registerFeatureType(AimingFeature.class);
   public static final int PIP_FEATURE = registerFeatureType(PipFeature.class);
   public static final int RETICLE_FEATURE = registerFeatureType(ReticleFeature.class);
   public static final int MUZZLE_FLASH_FEATURE = registerFeatureType(MuzzleFlashFeature.class);
   public static final int GLOW_FEATURE = registerFeatureType(GlowFeature.class);
   public static final int VISIBILITY_FEATURE = registerFeatureType(PartVisibilityFeature.class);
   public static final int RECOIL_FEATURE = registerFeatureType(RecoilFeature.class);
   public static final int ACCURACY_FEATURE = registerFeatureType(AccuracyFeature.class);
   public static final int ACTIVE_MUZZLE_FEATURE = registerFeatureType(ActiveMuzzleFeature.class);
   public static final int AMMO_CAPACITY_FEATURE = registerFeatureType(AmmoCapacityFeature.class);
   public static final int DAMAGE_FEATURE = registerFeatureType(DamageFeature.class);
   public static final int SKIN_FEATURE = registerFeatureType(SkinFeature.class);
   public static final int SOUND_FEATURE = registerFeatureType(SoundFeature.class);
   public static final int FIRE_MODE_FEATURE = registerFeatureType(FireModeFeature.class);

   private static int registerFeatureType(Class<? extends Feature> featureType) {
      ++featureTypeId;
      if (typeToId.put(featureType, featureTypeId) != null) {
         throw new IllegalArgumentException("Duplicate feature type: " + featureType);
      } else {
         idToType.put(featureTypeId, featureType);
         return featureTypeId;
      }
   }

   public static Map<Integer, Class<? extends Feature>> getFeatureTypes() {
      return Collections.unmodifiableMap(idToType);
   }

   public static Class<? extends Feature> getFeatureType(int featureTypeId) {
      return (Class)idToType.get(featureTypeId);
   }

   public static int getFeatureTypeId(Class<? extends Feature> featureType) {
      Integer id = (Integer)typeToId.get(featureType);
      if (id == null) {
         throw new IllegalArgumentException("Feature type not registered: " + featureType);
      } else {
         return id;
      }
   }

   public static void init() {
   }
}
