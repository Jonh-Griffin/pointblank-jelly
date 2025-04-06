package com.vicmatskiv.pointblank.item;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.world.level.ItemLike;

public class AmmoCount {
   private Map<ItemLike, AtomicInteger> ammoByType = new HashMap<>();

   private AtomicInteger get(FireModeInstance fireModeInstance) {
      return (AtomicInteger)this.ammoByType.computeIfAbsent(fireModeInstance.getAmmo(), (ati) -> {
         return new AtomicInteger();
      });
   }

   public int incrementAmmoCount(FireModeInstance fireMode, int delta) {
      return this.get(fireMode).addAndGet(delta);
   }

   public void setAmmoCount(FireModeInstance fireMode, int mainAmmoCount) {
      this.get(fireMode).set(mainAmmoCount);
   }

   public int getAmmoCount(FireModeInstance fireMode) {
       if (fireMode.getMaxAmmoCapacity() == Integer.MAX_VALUE) { //This is a hacky work around in my opinion, though it does seem to work without much issue.
          return Integer.MAX_VALUE;
       }
       return this.get(fireMode).get();
   }
}
