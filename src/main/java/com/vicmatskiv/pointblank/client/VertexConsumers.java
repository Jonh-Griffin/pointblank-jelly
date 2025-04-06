package com.vicmatskiv.pointblank.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.function.Function;
import org.joml.Matrix4f;

public class VertexConsumers {
   public static Function<VertexConsumer, VertexConsumer> PARTICLE = (vc) -> {
      return new ParticleVertexConsumer(vc);
   };
   public static Function<VertexConsumer, VertexConsumer> ENTITY = (vc) -> {
      return new ColorTexLightmapWrapper(vc);
   };

   private static class ColorTexLightmapWrapper extends VertexConsumerWrapper {
      ColorTexLightmapWrapper(VertexConsumer vertexConsumer) {
         super(vertexConsumer);
      }

      public void m_5752_() {
         this.vertexConsumer.m_252986_(this.transform, (float)this.x, (float)this.y, (float)this.z).m_6122_(this.r, this.g, this.b, this.a).m_7421_(this.u, this.v).m_86008_(0).m_7120_(this.uv1, this.uv2).m_5601_(0.0F, 0.0F, 1.0F).m_5752_();
      }
   }

   private static class ParticleVertexConsumer extends VertexConsumerWrapper {
      ParticleVertexConsumer(VertexConsumer vertexConsumer) {
         super(vertexConsumer);
      }

      public void m_5752_() {
         VertexConsumer vc;
         if (this.transform != null) {
            vc = this.vertexConsumer.m_252986_(this.transform, (float)this.x, (float)this.y, (float)this.z);
         } else {
            vc = this.vertexConsumer.m_5483_((double)((float)this.x), (double)((float)this.y), (double)((float)this.z));
         }

         vc.m_7421_(this.u, this.v).m_6122_(this.r, this.g, this.b, this.a).m_7120_(this.uv1, this.uv2).m_5752_();
      }
   }

   private abstract static class VertexConsumerWrapper implements VertexConsumer {
      protected Matrix4f transform;
      protected float u;
      protected float v;
      protected double x;
      protected double y;
      protected double z;
      protected int r;
      protected int g;
      protected int b;
      protected int a;
      protected int o1;
      protected int o2;
      protected int uv1;
      protected int uv2;
      protected VertexConsumer vertexConsumer;

      VertexConsumerWrapper(VertexConsumer vertexConsumer) {
         this.vertexConsumer = vertexConsumer;
      }

      public VertexConsumer m_252986_(Matrix4f transform, float x, float y, float z) {
         this.transform = transform;
         this.x = (double)x;
         this.y = (double)y;
         this.z = (double)z;
         return this;
      }

      public VertexConsumer m_5483_(double x, double y, double z) {
         this.x = x;
         this.y = y;
         this.z = z;
         return this;
      }

      public VertexConsumer m_6122_(int r, int g, int b, int a) {
         this.r = r;
         this.g = g;
         this.b = b;
         this.a = a;
         return this;
      }

      public VertexConsumer m_7421_(float u, float v) {
         this.u = u;
         this.v = v;
         return this;
      }

      public VertexConsumer m_7122_(int o1, int o2) {
         this.o1 = o1;
         this.o2 = o2;
         return this;
      }

      public VertexConsumer m_7120_(int uv1, int uv2) {
         this.uv1 = uv1;
         this.uv2 = uv2;
         return this;
      }

      public VertexConsumer m_5601_(float p_86005_, float p_86006_, float p_86007_) {
         throw new UnsupportedOperationException();
      }

      public abstract void m_5752_();

      public void m_7404_(int p_166901_, int p_166902_, int p_166903_, int p_166904_) {
         throw new UnsupportedOperationException();
      }

      public void m_141991_() {
         throw new UnsupportedOperationException();
      }
   }
}
