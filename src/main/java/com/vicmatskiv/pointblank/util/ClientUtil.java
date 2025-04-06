package com.vicmatskiv.pointblank.util;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class ClientUtil {
   public static Player getClientPlayer() {
      Minecraft mc = Minecraft.m_91087_();
      return mc.f_91074_;
   }

   public static boolean isFirstPerson(LivingEntity livingEntity) {
      Minecraft mc = Minecraft.m_91087_();
      return livingEntity == mc.f_91074_ && mc.f_91066_.m_92176_() == CameraType.FIRST_PERSON;
   }

   public static boolean isFirstPerson() {
      Minecraft mc = Minecraft.m_91087_();
      return mc.f_91066_.m_92176_() == CameraType.FIRST_PERSON;
   }
}
