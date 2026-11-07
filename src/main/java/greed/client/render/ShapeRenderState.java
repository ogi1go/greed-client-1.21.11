package greed.client.render;

import greed.client.render.BlurDrawBatch;
import greed.client.render.ShapeDrawData;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

final class ShapeRenderState implements SimpleGuiElementRenderState {
   private final ShapeDrawData shapeDrawData;
   private final Matrix3x2f matrix;
   private final ScreenRect rect;
   private final ScreenRect rect2;
   private final BlurDrawBatch blurDrawBatch;

   private void vertex(VertexConsumer vertexConsumer, float local7506_2, float local7506_3, int local7506_4) {
      vertexConsumer.vertex(this.matrix, local7506_2, local7506_3).color(this.shapeDrawData.color()).lineWidth(local7506_4 + 1);
   }

   ShapeRenderState(Matrix3x2f matrix3x2f, ShapeDrawData initialShapeDrawData, ScreenRect screenRect, BlurDrawBatch initialBlurDrawBatch) {
      this.shapeDrawData = initialShapeDrawData;
      this.matrix = new Matrix3x2f(matrix3x2f);
      this.rect = screenRect;
      this.blurDrawBatch = initialBlurDrawBatch;
      ScreenRect screenRect2 = new ScreenRect(
            Math.round(initialShapeDrawData.x()), Math.round(initialShapeDrawData.y()), Math.round(initialShapeDrawData.width()), Math.round(initialShapeDrawData.height())
         )
         .transformEachVertex(this.matrix);
      this.rect2 = screenRect == null ? screenRect2 : screenRect.intersection(screenRect2);
   }

   @Override
   public TextureSetup textureSetup() {
      return this.blurDrawBatch.textureSetup;
   }

   @Override
   public RenderPipeline pipeline() {
      return ShapeRenderer.pipeline2;
   }

   @Override
   public ScreenRect bounds() {
      return this.rect2;
   }

   @Override
   public void setupVertices(VertexConsumer vertexConsumer) {
      int local7511_2 = ShapeRenderer.getShapeRenderer().computeInt(this.shapeDrawData, this.blurDrawBatch);
      if (local7511_2 >= 0) {
         float local7511_3 = this.shapeDrawData.x();
         float local7511_4 = this.shapeDrawData.y();
         float local7511_5 = this.shapeDrawData.x() + this.shapeDrawData.width();
         float local7511_6 = this.shapeDrawData.y() + this.shapeDrawData.height();
         this.vertex(vertexConsumer, local7511_3, local7511_4, local7511_2);
         this.vertex(vertexConsumer, local7511_3, local7511_6, local7511_2);
         this.vertex(vertexConsumer, local7511_5, local7511_6, local7511_2);
         this.vertex(vertexConsumer, local7511_5, local7511_4, local7511_2);
      }
   }

   @Override
   public ScreenRect scissorArea() {
      return this.rect;
   }
}
