package com.vicmatskiv.pointblank.client.effect;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.vicmatskiv.pointblank.client.GunClientState;
import com.vicmatskiv.pointblank.client.PoseProvider;
import com.vicmatskiv.pointblank.client.PositionProvider;
import com.vicmatskiv.pointblank.item.GunItem;
import com.vicmatskiv.pointblank.util.JsonUtil;
import com.vicmatskiv.pointblank.util.MiscUtil;
import com.vicmatskiv.pointblank.util.SimpleHitResult;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import software.bernie.geckolib.util.ClientUtils;

public interface EffectBuilder<T extends EffectBuilder<T, E>, E extends Effect> {
   Collection<GunItem.FirePhase> getCompatiblePhases();

   static EffectBuilder<?, ?> fromZipEntry(ZipFile zipFile, ZipEntry entry) {
      try {
         BufferedReader reader = new BufferedReader(new InputStreamReader(zipFile.getInputStream(entry)));

         EffectBuilder var3;
         try {
            var3 = fromReader(reader);
         } catch (Throwable var6) {
            try {
               reader.close();
            } catch (Throwable var5) {
               var6.addSuppressed(var5);
            }

            throw var6;
         }

         reader.close();
         return var3;
      } catch (IOException var7) {
         throw new RuntimeException(var7);
      }
   }

   static EffectBuilder<?, ?> fromPath(Path path) {
      try {
         BufferedReader br = Files.newBufferedReader(path);

         EffectBuilder var2;
         try {
            var2 = fromReader(br);
         } catch (Throwable var5) {
            if (br != null) {
               try {
                  br.close();
               } catch (Throwable var4) {
                  var5.addSuppressed(var4);
               }
            }

            throw var5;
         }

         if (br != null) {
            br.close();
         }

         return var2;
      } catch (IOException var6) {
         throw new RuntimeException(var6);
      }
   }

   static EffectBuilder<?, ?> fromReader(Reader reader) {
      try {
         JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
         String name = JsonUtil.getJsonString(obj, "name");
         EffectType effectType = (EffectType)JsonUtil.getEnum(obj, "type", EffectType.class, (Enum)null, true);
         if (effectType == null) {
            throw new IllegalArgumentException("Missing effect 'type' in " + obj);
         } else if (effectType == EffectType.DETACHED_PROJECTILE) {
            return ((DetachedProjectileEffect.Builder)(new DetachedProjectileEffect.Builder()).withName(name)).withJsonObject(obj);
         } else if (effectType == EffectType.ATTACHED_PROJECTILE) {
            return ((AttachedProjectileEffect.Builder)(new AttachedProjectileEffect.Builder()).withName(name)).withJsonObject(obj);
         } else if (effectType == EffectType.IMPACT) {
            return ((ImpactEffect.Builder)(new ImpactEffect.Builder()).withName(name)).withJsonObject(obj);
         } else if (effectType == EffectType.MUZZLE_FLASH) {
            return ((MuzzleFlashEffect.Builder)(new MuzzleFlashEffect.Builder()).withName(name)).withJsonObject(obj);
         } else if (effectType == EffectType.TRAIL) {
            return ((TrailEffect.Builder)(new TrailEffect.Builder()).withName(name)).withJsonObject(obj);
         } else {
            throw new IllegalArgumentException("Invalid effect type: " + effectType);
         }
      } catch (Exception var4) {
         throw new RuntimeException("Error parsing JSON: " + var4.getMessage(), var4);
      }
   }

   boolean isEffectAttached();

   T withJsonObject(JsonObject var1);

   E build(Context var1);

   String getName();

   public static enum EffectType {
      DETACHED_PROJECTILE,
      ATTACHED_PROJECTILE,
      IMPACT,
      MUZZLE_FLASH,
      TRAIL;

      // $FF: synthetic method
      private static EffectType[] $values() {
         return new EffectType[]{DETACHED_PROJECTILE, ATTACHED_PROJECTILE, IMPACT, MUZZLE_FLASH, TRAIL};
      }
   }

   public static class EffectBuilderWrapper implements EffectBuilder<EffectBuilderWrapper, Effect> {
      private EffectBuilder<?, ?> delegate;
      private Supplier<EffectBuilder<?, ?>> supplier;
      private String name;

      public EffectBuilderWrapper(String name, Supplier<EffectBuilder<?, ?>> supplier) {
         this.name = name;
         this.supplier = supplier;
      }

      private EffectBuilder<?, ?> getOrCreate() {
         if (this.delegate == null) {
            this.delegate = (EffectBuilder)this.supplier.get();
         }

         return this.delegate;
      }

      public Collection<GunItem.FirePhase> getCompatiblePhases() {
         return this.getOrCreate().getCompatiblePhases();
      }

      public boolean isEffectAttached() {
         return this.getOrCreate().isEffectAttached();
      }

      public EffectBuilderWrapper withJsonObject(JsonObject obj) {
         throw new UnsupportedOperationException();
      }

      public Effect build(Context effectContext) {
         return this.getOrCreate().build(effectContext);
      }

      public String getName() {
         return this.name;
      }
   }

   public static class Context {
      private GunClientState gunClientState;
      private Vec3 startPosition;
      private Vec3 targetPosition;
      private Vec3 velocity;
      private Quaternionf rotation;
      private float distance;
      private float randomization;
      private PoseProvider poseProvider;
      private PositionProvider positionProvider;
      private Function<VertexConsumer, VertexConsumer> vertexConsumerTransformer;
      private HitResult hitResult;
      private float damage;

      public Context withGunState(GunClientState gunClientState) {
         this.gunClientState = gunClientState;
         return this;
      }

      public Context withStartPosition(Vec3 startPosition) {
         this.startPosition = startPosition;
         return this;
      }

      public Context withVelocity(Vec3 velocity) {
         this.velocity = velocity;
         return this;
      }

      public Context withRotation(Quaternionf rotation) {
         this.rotation = rotation;
         return this;
      }

      public Context withTargetPosition(Vec3 targetPosition) {
         this.targetPosition = targetPosition;
         return this;
      }

      public Context withDistance(float distance) {
         this.distance = distance;
         return this;
      }

      public Context withRandomization(float randomization) {
         this.randomization = randomization;
         return this;
      }

      public Context withPoseProvider(PoseProvider poseProvider) {
         this.poseProvider = poseProvider;
         return this;
      }

      public Context withPositionProvider(PositionProvider positionProvider) {
         this.positionProvider = positionProvider;
         return this;
      }

      public Context withVertexConsumerTransformer(Function<VertexConsumer, VertexConsumer> vertexConsumerTransformer) {
         this.vertexConsumerTransformer = vertexConsumerTransformer;
         return this;
      }

      public Context withHitResult(HitResult hitResult) {
         this.hitResult = hitResult;
         this.updateEffectContextWithLocationAndRotation(hitResult);
         return this;
      }

      public Context withDamage(float damage) {
         this.damage = damage;
         return this;
      }

      public float getDistance() {
         return this.distance;
      }

      public float getRandomization() {
         return this.randomization;
      }

      public Vec3 getStartPosition() {
         return this.startPosition;
      }

      public Vec3 getTargetPosition() {
         return this.targetPosition;
      }

      public Vec3 getVelocity() {
         return this.velocity;
      }

      public PoseProvider getPoseProvider() {
         return this.poseProvider;
      }

      public PositionProvider getPositionProvider() {
         return this.positionProvider;
      }

      public Function<VertexConsumer, VertexConsumer> getVertexConsumerTransformer() {
         return this.vertexConsumerTransformer;
      }

      public HitResult getHitResult() {
         return this.hitResult;
      }

      public float getDamage() {
         return this.damage;
      }

      public Quaternionf getRotation() {
         return this.rotation;
      }

      public GunClientState getGunClientState() {
         return this.gunClientState;
      }

      private void updateEffectContextWithLocationAndRotation(HitResult hitResult) {
         if (hitResult instanceof SimpleHitResult) {
            SimpleHitResult simpleHitResult = (SimpleHitResult)hitResult;
            Vec3 location = hitResult.m_82450_();
            switch(hitResult.m_6662_()) {
            case BLOCK:
               Direction direction = simpleHitResult.getDirection();
               Vec3i normal = direction.m_122436_();
               this.withStartPosition(new Vec3(location.f_82479_ + (double)normal.m_123341_() * 0.01D, location.f_82480_ + (double)normal.m_123342_() * 0.01D, location.f_82481_ + (double)normal.m_123343_() * 0.01D));
               this.withRotation(MiscUtil.getRotation(direction));
               break;
            case ENTITY:
               Vec3 shotOrigin = ClientUtils.getClientPlayer().m_146892_();
               Vec3 offset = shotOrigin.m_82546_(location).m_82541_().m_82542_(0.0D, 0.1D, 0.1D);
               double adjX = location.f_82479_ + offset.f_82479_;
               double adjY = location.f_82480_ + offset.f_82480_;
               double adjZ = location.f_82481_ + offset.f_82481_;
               this.withStartPosition(new Vec3(adjX, adjY, adjZ));
            case MISS:
            }
         }

      }
   }
}
