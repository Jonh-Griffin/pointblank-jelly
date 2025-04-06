package com.vicmatskiv.pointblank.attachment;

import com.vicmatskiv.pointblank.feature.ActiveMuzzleFeature;
import com.vicmatskiv.pointblank.feature.AimingFeature;
import com.vicmatskiv.pointblank.feature.Feature;
import com.vicmatskiv.pointblank.feature.FeatureBuilder;
import com.vicmatskiv.pointblank.feature.PipFeature;
import com.vicmatskiv.pointblank.feature.ReticleFeature;
import com.vicmatskiv.pointblank.util.Conditions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Predicate;

public class AttachmentCategory implements Comparable<AttachmentCategory> {
   private static final SortedMap<String, AttachmentCategory> categories = new TreeMap<>();
   public static final AttachmentCategory SCOPE = fromString("scope", (c) -> {
      return c == AimingFeature.class || c == ReticleFeature.class || c == PipFeature.class;
   });
   public static final AttachmentCategory MUZZLE = fromString("muzzle", (c) -> {
      return false;
   }).withDefaultFeatures(List.of((new ActiveMuzzleFeature.Builder()).withCondition(Conditions.isUsingDefaultMuzzle())));
   public static final AttachmentCategory RAIL = fromString("rail", (c) -> {
      return false;
   });
   public static final AttachmentCategory UNDERBARREL = fromString("underbarrel", (c) -> {
      return false;
   });
   public static final AttachmentCategory SKIN = fromString("skin", (c) -> {
      return false;
   });
   public static final AttachmentCategory STOCK = fromString("stock", (c) -> {
      return false;
   });
   public static final AttachmentCategory MAGAZINE = fromString("magazine", (c) -> {
      return false;
   });
   private final String name;
   private final Predicate<Class<? extends Feature>> isActiveAttachmentRequiredPredicate;
   private final List<FeatureBuilder<?, ?>> defaultFeatures;

   public static Collection<AttachmentCategory> values() {
      return categories.values();
   }

   public static AttachmentCategory fromString(String categoryName) {
      return fromString(categoryName, (c) -> {
         return false;
      });
   }

   private static AttachmentCategory fromString(String categoryName, Predicate<Class<? extends Feature>> isActiveAttachmentRequiredPredicate) {
       return categories.computeIfAbsent(categoryName.toLowerCase(), (n) -> {
          return new AttachmentCategory(n, isActiveAttachmentRequiredPredicate);
       });
   }

   public static AttachmentCategory fromOrdinal(int ordinal) {
      int index = 0;
      AttachmentCategory result = null;
      if (ordinal >= categories.size()) {
         throw new IllegalArgumentException("Invalid ordinal " + ordinal);
      } else {
         for(Iterator<AttachmentCategory> var3 = categories.values().iterator(); var3.hasNext(); ++index) {
            AttachmentCategory ac = var3.next();
            if (index == ordinal) {
               result = ac;
               break;
            }
         }

         return result;
      }
   }

   private AttachmentCategory(String name, Predicate<Class<? extends Feature>> isActiveAttachmentRequiredPredicate) {
      this.name = name;
      this.isActiveAttachmentRequiredPredicate = isActiveAttachmentRequiredPredicate;
      this.defaultFeatures = new ArrayList<>();
   }

   private AttachmentCategory withDefaultFeatures(List<FeatureBuilder<?, ?>> defaultFeatures) {
      this.defaultFeatures.addAll(defaultFeatures);
      return this;
   }

   public String getName() {
      return this.name;
   }

   public List<FeatureBuilder<?, ?>> getDefaultFeatures() {
      return this.defaultFeatures;
   }

   public boolean requiresAttachmentSelection(Class<? extends Feature> featureType) {
      return this.isActiveAttachmentRequiredPredicate.test(featureType);
   }

   public int ordinal() {
      int result = 0;

      for(Iterator<AttachmentCategory> var2 = categories.values().iterator(); var2.hasNext(); ++result) {
         AttachmentCategory ac = var2.next();
         if (ac == this) {
            return result;
         }
      }

      return -1;
   }

   public int hashCode() {
      return Objects.hash(this.name);
   }

   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      } else if (obj == null) {
         return false;
      } else if (this.getClass() != obj.getClass()) {
         return false;
      } else {
         AttachmentCategory other = (AttachmentCategory)obj;
         return Objects.equals(this.name, other.name);
      }
   }

   public int compareTo(AttachmentCategory o) {
      return this.name.compareTo(o.name);
   }

   public String toString() {
      return String.format("AttachmentCategory[%s]", this.name);
   }
}
