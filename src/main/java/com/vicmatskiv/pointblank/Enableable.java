package com.vicmatskiv.pointblank;

public interface Enableable {
   default boolean isEnabled() {
      return true;
   }
}
