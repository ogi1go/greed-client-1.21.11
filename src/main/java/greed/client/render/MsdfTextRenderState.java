package greed.client.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

final class MsdfTextRenderState implements SimpleGuiElementRenderState {
   private final Matrix3x2f matrix;
   private final TextureSetup textureSetup;
   private final ScreenRect rect;
   private final boolean shimmer;
   private final float lineWidth;
   private final List<MsdfTextRenderState.DataRecord> glyphs = new ArrayList<>(128);
   private float minX = Float.MAX_VALUE;
   private float minY = Float.MAX_VALUE;
   private float maxX = -Float.MAX_VALUE;
   private float maxY = -Float.MAX_VALUE;

   @Override
   public TextureSetup textureSetup() {
      return this.textureSetup;
   }

   @Override
   public RenderPipeline pipeline() {
      return this.shimmer ? MsdfTextRenderer.pipeline3 : MsdfTextRenderer.pipeline2;
   }

   @Override
   public ScreenRect scissorArea() {
      return this.rect;
   }

   private void include(float x, float y) {
      this.minX = Math.min(this.minX, x);
      this.minY = Math.min(this.minY, y);
      this.maxX = Math.max(this.maxX, x);
      this.maxY = Math.max(this.maxY, y);
   }

   @Override
   public void setupVertices(VertexConsumer vertexConsumer) {
      if (this.shimmer) {
         for (MsdfTextRenderState.DataRecord record : this.glyphs) {
            this.shimmerVertex(
               vertexConsumer,
               record.x0(),
               record.y0(),
               record.u0(),
               record.v0(),
               record.colorTopLeft(),
               record.quadIndex0()
            );
            this.shimmerVertex(
               vertexConsumer,
               record.x1(),
               record.y1(),
               record.u0(),
               record.v1(),
               record.colorBottomLeft(),
               record.quadIndex0()
            );
            this.shimmerVertex(
               vertexConsumer,
               record.x2(),
               record.y2(),
               record.u1(),
               record.v1(),
               record.colorBottomRight(),
               record.quadIndex1()
            );
            this.shimmerVertex(
               vertexConsumer,
               record.x3(),
               record.y3(),
               record.u1(),
               record.v0(),
               record.colorTopRight(),
               record.quadIndex1()
            );
         }
      } else {
         for (MsdfTextRenderState.DataRecord record : this.glyphs) {
            this.vertex(vertexConsumer, record.x0(), record.y0(), record.u0(), record.v0(), record.colorTopLeft());
            this.vertex(vertexConsumer, record.x1(), record.y1(), record.u0(), record.v1(), record.colorBottomLeft());
            this.vertex(
               vertexConsumer,
               record.x2(),
               record.y2(),
               record.u1(),
               record.v1(),
               record.colorBottomRight()
            );
            this.vertex(
               vertexConsumer, record.x3(), record.y3(), record.u1(), record.v0(), record.colorTopRight()
            );
         }
      }
   }

   void addGlyph(
      float left,
      float top,
      float right,
      float bottom,
      float u0,
      float v0,
      float u1,
      float v1,
      int colorTopLeft,
      int colorTopRight,
      int colorBottomRight,
      int colorBottomLeft,
      float quadIndex0,
      float quadIndex1,
      float rotation,
      float pivotX,
      float pivotY
   ) {
      float x0 = left;
      float y0 = top;
      float x1 = left;
      float y1 = bottom;
      float x2 = right;
      float y2 = bottom;
      float x3 = right;
      float y3 = top;
      if (Math.abs(rotation) >= 0.001F) {
         float radians = (float)Math.toRadians(rotation);
         float sin = (float)Math.sin(radians);
         float cos = (float)Math.cos(radians);
         float[] rotatedTopLeft = this.rotate(x0, y0, pivotX, pivotY, sin, cos);
         float[] rotatedBottomLeft = this.rotate(x1, y1, pivotX, pivotY, sin, cos);
         float[] rotatedBottomRight = this.rotate(x2, y2, pivotX, pivotY, sin, cos);
         float[] rotatedTopRight = this.rotate(x3, y3, pivotX, pivotY, sin, cos);
         x0 = rotatedTopLeft[0];
         y0 = rotatedTopLeft[1];
         x1 = rotatedBottomLeft[0];
         y1 = rotatedBottomLeft[1];
         x2 = rotatedBottomRight[0];
         y2 = rotatedBottomRight[1];
         x3 = rotatedTopRight[0];
         y3 = rotatedTopRight[1];
      }

      this.glyphs
         .add(
            new MsdfTextRenderState.DataRecord(
               x0,
               y0,
               x1,
               y1,
               x2,
               y2,
               x3,
               y3,
               u0,
               v0,
               u1,
               v1,
               colorTopLeft,
               colorTopRight,
               colorBottomRight,
               colorBottomLeft,
               quadIndex0,
               quadIndex1
            )
         );
      this.include(x0, y0);
      this.include(x1, y1);
      this.include(x2, y2);
      this.include(x3, y3);
   }

   private void vertex(VertexConsumer vertexConsumer, float x, float y, float u, float v, int color) {
      vertexConsumer.vertex(this.matrix, x, y).texture(u, v).color(color).lineWidth(this.lineWidth);
   }

   private float[] rotate(float x, float y, float pivotX, float pivotY, float sin, float cos) {
      float dx = x - pivotX;
      float dy = y - pivotY;
      return new float[]{
         pivotX + dx * cos - dy * sin, pivotY + dx * sin + dy * cos
      };
   }

   @Override
   public ScreenRect bounds() {
      if (this.glyphs.isEmpty()) {
         return new ScreenRect(0, 0, 1, 1);
      }

      int minXFloor = (int)Math.floor(this.minX);
      int minYFloor = (int)Math.floor(this.minY);
      int boundsWidth = Math.max(1, (int)Math.ceil(this.maxX - this.minX));
      int boundsHeight = Math.max(1, (int)Math.ceil(this.maxY - this.minY));
      ScreenRect screenRect = new ScreenRect(minXFloor, minYFloor, boundsWidth, boundsHeight).transformEachVertex(this.matrix);
      return this.rect == null ? screenRect : this.rect.intersection(screenRect);
   }

   private void shimmerVertex(
      VertexConsumer vertexConsumer, float x, float y, float u, float v, int color, float lineWidth
   ) {
      vertexConsumer.vertex(this.matrix, x, y).texture(u, v).color(color).lineWidth(lineWidth);
   }

   MsdfTextRenderState(Matrix3x2f matrix3x2f, TextureSetup textureSetup, ScreenRect screenRect, boolean shimmer, float lineWidth) {
      this.matrix = new Matrix3x2f(matrix3x2f);
      this.textureSetup = textureSetup;
      this.rect = screenRect;
      this.shimmer = shimmer;
      this.lineWidth = lineWidth;
   }

   private record DataRecord(
      float x0,
      float y0,
      float x1,
      float y1,
      float x2,
      float y2,
      float x3,
      float y3,
      float u0,
      float v0,
      float u1,
      float v1,
      int colorTopLeft,
      int colorTopRight,
      int colorBottomRight,
      int colorBottomLeft,
      float quadIndex0,
      float quadIndex1
   ) {
   }
}
