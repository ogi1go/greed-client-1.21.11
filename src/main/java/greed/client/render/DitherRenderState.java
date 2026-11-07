package greed.client.render;

import greed.client.render.DitherDrawData;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

final class DitherRenderState implements SimpleGuiElementRenderState {
   private final Matrix3x2f matrix;
   private final ScreenRect rect;
   private final List<DitherDrawData> list = new ArrayList<>(4);
   private float floatVal = Float.MAX_VALUE;
   private float floatVal2 = Float.MAX_VALUE;
   private float floatVal3 = -Float.MAX_VALUE;
   private float floatVal4 = -Float.MAX_VALUE;

   void add(DitherDrawData ditherDrawData) {
      this.list.add(ditherDrawData);
      this.floatVal = Math.min(this.floatVal, ditherDrawData.x());
      this.floatVal2 = Math.min(this.floatVal2, ditherDrawData.y());
      this.floatVal3 = Math.max(this.floatVal3, ditherDrawData.x() + ditherDrawData.width());
      this.floatVal4 = Math.max(this.floatVal4, ditherDrawData.y() + ditherDrawData.height());
   }

   @Override
   public TextureSetup textureSetup() {
      return TextureSetup.empty();
   }

   @Override
   public ScreenRect scissorArea() {
      return this.rect;
   }

   @Override
   public void setupVertices(VertexConsumer vertexConsumer) {
      for (DitherDrawData ditherDrawData : this.list) {
         int local7462_4 = DitherRenderer.getDitherRenderer().computeInt(ditherDrawData);
         if (local7462_4 >= 0) {
            float local7462_5 = ditherDrawData.x();
            float local7462_6 = ditherDrawData.y();
            float local7462_7 = ditherDrawData.x() + ditherDrawData.width();
            float local7462_8 = ditherDrawData.y() + ditherDrawData.height();
            int local7462_9 = ditherDrawData.color();
            this.vertex(vertexConsumer, local7462_5, local7462_6, 0.0F, 0.0F, local7462_9, local7462_4);
            this.vertex(vertexConsumer, local7462_5, local7462_8, 0.0F, 1.0F, local7462_9, local7462_4);
            this.vertex(vertexConsumer, local7462_7, local7462_8, 1.0F, 1.0F, local7462_9, local7462_4);
            this.vertex(vertexConsumer, local7462_7, local7462_6, 1.0F, 0.0F, local7462_9, local7462_4);
         }
      }
   }

   @Override
   public ScreenRect bounds() {
      if (this.rect != null) {
         return this.rect;
      }

      if (this.list.isEmpty()) {
         return new ScreenRect(0, 0, 1, 1);
      }

      int local7463_1 = (int)Math.floor(this.floatVal);
      int local7463_2 = (int)Math.floor(this.floatVal2);
      int local7463_3 = Math.max(1, (int)Math.ceil(this.floatVal3 - this.floatVal));
      int local7463_4 = Math.max(1, (int)Math.ceil(this.floatVal4 - this.floatVal2));
      return new ScreenRect(local7463_1, local7463_2, local7463_3, local7463_4).transformEachVertex(this.matrix);
   }

   DitherRenderState(Matrix3x2f matrix3x2f, ScreenRect screenRect) {
      this.matrix = new Matrix3x2f(matrix3x2f);
      this.rect = screenRect;
   }

   private void vertex(VertexConsumer vertexConsumer, float local7465_2, float local7465_3, float local7465_4, float local7465_5, int local7465_6, int local7465_7) {
      vertexConsumer.vertex(this.matrix, local7465_2, local7465_3).texture(local7465_4, local7465_5).color(local7465_6).lineWidth(local7465_7 + 1);
   }

   @Override
   public RenderPipeline pipeline() {
      return DitherRenderer.pipeline2;
   }
}
