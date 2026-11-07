package greed.client.render;

import greed.client.render.LineDrawData;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

final class LineRenderState implements SimpleGuiElementRenderState {
   private final LineDrawData lineDrawData;
   private final Matrix3x2f matrix;
   private final ScreenRect rect;
   private final ScreenRect rect2;

   LineRenderState(Matrix3x2f matrix3x2f, LineDrawData initialLineDrawData, ScreenRect screenRect) {
      this.lineDrawData = initialLineDrawData;
      this.matrix = new Matrix3x2f(matrix3x2f);
      this.rect = screenRect;
      float local1305_4 = Math.min(initialLineDrawData.x1(), initialLineDrawData.x2()) - initialLineDrawData.thickness();
      float local1305_5 = Math.min(initialLineDrawData.y1(), initialLineDrawData.y2()) - initialLineDrawData.thickness();
      float local1305_6 = Math.max(initialLineDrawData.x1(), initialLineDrawData.x2()) + initialLineDrawData.thickness();
      float local1305_7 = Math.max(initialLineDrawData.y1(), initialLineDrawData.y2()) + initialLineDrawData.thickness();
      ScreenRect screenRect2 = new ScreenRect(
            Math.round(local1305_4), Math.round(local1305_5), Math.round(local1305_6 - local1305_4), Math.round(local1305_7 - local1305_5)
         )
         .transformEachVertex(this.matrix);
      this.rect2 = screenRect == null ? screenRect2 : screenRect.intersection(screenRect2);
   }

   @Override
   public ScreenRect bounds() {
      return this.rect2;
   }

   @Override
   public void setupVertices(VertexConsumer vertexConsumer) {
      int local1307_2 = LineRenderer.getLineRenderer().computeInt(this.lineDrawData);
      if (local1307_2 >= 0) {
         float[] floatArray = this.lineDrawData.corners();
         this.vertex(vertexConsumer, floatArray[0], floatArray[1], 0, 0, local1307_2);
         this.vertex(vertexConsumer, floatArray[2], floatArray[3], 0, 255, local1307_2);
         this.vertex(vertexConsumer, floatArray[4], floatArray[5], 255, 255, local1307_2);
         this.vertex(vertexConsumer, floatArray[6], floatArray[7], 255, 0, local1307_2);
      }
   }

   @Override
   public ScreenRect scissorArea() {
      return this.rect;
   }

   private void vertex(VertexConsumer vertexConsumer, float local1309_2, float local1309_3, int local1309_4, int local1309_5, int local1309_6) {
      vertexConsumer.vertex(this.matrix, local1309_2, local1309_3).color(local1309_4, local1309_5, 255, 255).lineWidth(local1309_6 + 1);
   }

   @Override
   public TextureSetup textureSetup() {
      return TextureSetup.empty();
   }

   @Override
   public RenderPipeline pipeline() {
      return LineRenderer.pipeline2;
   }
}
