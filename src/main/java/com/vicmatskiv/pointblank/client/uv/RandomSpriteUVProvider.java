package com.vicmatskiv.pointblank.client.uv;

import java.util.Random;

public class RandomSpriteUVProvider implements SpriteUVProvider {
   private Random random = new Random();
   private int totalSprites;
   private int rows;
   private int columns;
   private int seed;
   private long frameDurationMillis;
   private long lifetimeMillis;

   public RandomSpriteUVProvider(int rows, int columns, int spritesPerSecond, long lifetimeMillis) {
      this.rows = rows;
      this.columns = columns;
      this.totalSprites = rows * columns;
      this.frameDurationMillis = (long)(1000.0D / (double)spritesPerSecond);
      this.seed = this.random.nextInt();
      this.lifetimeMillis = lifetimeMillis;
   }

   public float[] getSpriteUV(float progress) {
      double elapsedTimeMillis = (double)(progress * (float)this.lifetimeMillis);
      int elapsedTimeSeed = this.seed + (int)(elapsedTimeMillis / (double)this.frameDurationMillis);
      this.random.setSeed((long)elapsedTimeSeed);
      int spriteIndex = this.random.nextInt(this.totalSprites);
      return SpriteUVProvider.getSpriteUV(spriteIndex, this.rows, this.columns);
   }
}
