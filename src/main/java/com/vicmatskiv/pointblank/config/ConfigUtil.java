package com.vicmatskiv.pointblank.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ConfigUtil {
   public static <T, K> List<T> mergeCollections(Collection<T> refCollection, Collection<T> actualCollection, Function<T, K> getKey, Comparator<T> comparator, BiFunction<T, T, T> mergeRule) {
      Map<K, T> mergedMap = new LinkedHashMap();
      Iterator var6 = refCollection.iterator();

      Object item;
      while(var6.hasNext()) {
         item = var6.next();
         mergedMap.merge(getKey.apply(item), item, mergeRule);
      }

      var6 = actualCollection.iterator();

      while(var6.hasNext()) {
         item = var6.next();
         K key = getKey.apply(item);
         if (mergedMap.containsKey(key)) {
            mergedMap.merge(key, item, mergeRule);
         }
      }

      return (List)mergedMap.values().stream().sorted(comparator).collect(Collectors.toList());
   }

   static List<String> join(List<String> l, String keyValueEntry) {
      List<String> result = new ArrayList();

      String t;
      for(Iterator var3 = l.iterator(); var3.hasNext(); result.add(t)) {
         String e = (String)var3.next();
         t = e.trim();
         if (!t.startsWith("#")) {
            t = "#" + t;
         }
      }

      result.add(keyValueEntry);
      return result;
   }
}
