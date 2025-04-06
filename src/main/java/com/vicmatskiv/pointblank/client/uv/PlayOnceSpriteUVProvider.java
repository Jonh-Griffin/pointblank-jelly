package com.vicmatskiv.pointblank.client.uv;

import net.minecraft.util.Mth;

public class PlayOnceSpriteUVProvider implements SpriteUVProvider {
   private int totalSprites;
   private double spriteFrameDurationMillis;
   private int rows;
   private int columns;
   private long lifetimeMillis;

   public PlayOnceSpriteUVProvider(int rows, int columns, int spritesPerSecond, long lifetimeMillis) {
      this.rows = rows;
      this.columns = columns;
      this.totalSprites = rows * columns;
      this.spriteFrameDurationMillis = 1000.0D / (double)spritesPerSecond;
      this.lifetimeMillis = lifetimeMillis;
   }

   private int getSpriteIndex(float progress) {
      double elapsedTimeMillis = (double)(progress * (float)this.lifetimeMillis);
      return Mth.m_14045_((int)(elapsedTimeMillis / this.spriteFrameDurationMillis), 0, this.totalSprites);
   }

   public float[] getSpriteUV(float progress) {
      int spriteIndex = this.getSpriteIndex(progress);
      return spriteIndex >= this.totalSprites ? null : SpriteUVProvider.getSpriteUV(spriteIndex, this.rows, this.columns);
   }
}
