package greed.client.render;

import greed.client.render.BlurDrawBatch;
import greed.client.render.GlassDrawData;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

final class GlassRenderState implements SimpleGuiElementRenderState {
   private final GlassDrawData glassDrawData;
   private final Matrix3x2f matrix;
   private final ScreenRect rect;
   private final ScreenRect rect2;
   private final BlurDrawBatch blurDrawBatch;

   @Override
   public TextureSetup textureSetup() {
      return this.blurDrawBatch.textureSetup;
   }

   @Override
   public ScreenRect bounds() {
      return this.rect2;
   }

   GlassRenderState(Matrix3x2f matrix3x2f, GlassDrawData initialGlassDrawData, ScreenRect screenRect, BlurDrawBatch initialBlurDrawBatch) {
      this.glassDrawData = initialGlassDrawData;
      this.matrix = new Matrix3x2f(matrix3x2f);
      this.rect = screenRect;
      this.blurDrawBatch = initialBlurDrawBatch;
      ScreenRect screenRect2 = new ScreenRect(
            Math.round(initialGlassDrawData.x()), Math.round(initialGlassDrawData.y()), Math.round(initialGlassDrawData.width()), Math.round(initialGlassDrawData.height())
         )
         .transformEachVertex(this.matrix);
      this.rect2 = screenRect == null ? screenRect2 : screenRect.intersection(screenRect2);
   }

   private void vertex(VertexConsumer vertexConsumer, float local5937_2, float local5937_3, int local5937_4) {
      vertexConsumer.vertex(this.matrix, local5937_2, local5937_3).color(this.glassDrawData.color()).lineWidth(local5937_4 + 1);
   }

   @Override
   public ScreenRect scissorArea() {
      return this.rect;
   }

   @Override
   public RenderPipeline pipeline() {
      return GlassRenderer.pipeline2;
   }

   @Override
   public void setupVertices(VertexConsumer vertexConsumer) {
      int local5940_2 = GlassRenderer.getGlassRenderer().computeInt(this.glassDrawData, this.blurDrawBatch);
      if (local5940_2 >= 0) {
         float local5940_3 = this.glassDrawData.x();
         float local5940_4 = this.glassDrawData.y();
         float local5940_5 = this.glassDrawData.x() + this.glassDrawData.width();
         float local5940_6 = this.glassDrawData.y() + this.glassDrawData.height();
         this.vertex(vertexConsumer, local5940_3, local5940_4, local5940_2);
         this.vertex(vertexConsumer, local5940_3, local5940_6, local5940_2);
         this.vertex(vertexConsumer, local5940_5, local5940_6, local5940_2);
         this.vertex(vertexConsumer, local5940_5, local5940_4, local5940_2);
      }
   }
}
