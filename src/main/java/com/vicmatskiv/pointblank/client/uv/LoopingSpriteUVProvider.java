package com.vicmatskiv.pointblank.client.uv;

public class LoopingSpriteUVProvider implements SpriteUVProvider {
   private int totalSprites;
   private double spriteFrameDurationMillis;
   private int rows;
   private int columns;
   private long lifetimeMillis;

   public LoopingSpriteUVProvider(int rows, int columns, int spritesPerSecond, long lifetimeMillis) {
      this.rows = rows;
      this.columns = columns;
      this.totalSprites = rows * columns;
      this.spriteFrameDurationMillis = 1000.0D / (double)spritesPerSecond;
      this.lifetimeMillis = lifetimeMillis;
   }

   public int getSpriteIndex(float progress) {
      double elapsedTimeMillis = (double)(progress * (float)this.lifetimeMillis);
      int elapsedSprites = (int)(elapsedTimeMillis / this.spriteFrameDurationMillis);
      int spriteIndex = elapsedSprites % this.totalSprites;
      return spriteIndex;
   }

   public float[] getSpriteUV(float progress) {
      int spriteIndex = this.getSpriteIndex(progress);
      return SpriteUVProvider.getSpriteUV(spriteIndex, this.rows, this.columns);
   }
}
