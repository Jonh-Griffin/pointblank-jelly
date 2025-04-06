package com.vicmatskiv.pointblank.inventory;

public interface HierarchicalSlot {
   String getPath();

   HierarchicalSlot getParentSlot();

   void setParentSlot(HierarchicalSlot var1);
}
