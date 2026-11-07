package greed.client.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

final class RasterTextRenderState implements SimpleGuiElementRenderState {
   private final Matrix3x2f matrix;
   private final TextureSetup textureSetup2;
   private final ScreenRect rect;
   private final List<RasterTextRenderState.DataRecord> list = new ArrayList<>(64);
   private float floatVal = Float.MAX_VALUE;
   private float floatVal2 = Float.MAX_VALUE;
   private float floatVal3 = -Float.MAX_VALUE;
   private float floatVal4 = -Float.MAX_VALUE;

   @Override
   public void setupVertices(VertexConsumer vertexConsumer) {
      for (RasterTextRenderState.DataRecord local2674_3 : this.list) {
         this.vertex(vertexConsumer, local2674_3.x0(), local2674_3.y0(), local2674_3.u0(), local2674_3.v0(), local2674_3.colorTopLeft());
         this.vertex(vertexConsumer, local2674_3.x0(), local2674_3.y1(), local2674_3.u0(), local2674_3.v1(), local2674_3.colorBottomLeft());
         this.vertex(vertexConsumer, local2674_3.x1(), local2674_3.y1(), local2674_3.u1(), local2674_3.v1(), local2674_3.colorBottomRight());
         this.vertex(vertexConsumer, local2674_3.x1(), local2674_3.y0(), local2674_3.u1(), local2674_3.v0(), local2674_3.colorTopRight());
      }
   }

   @Override
   public ScreenRect bounds() {
      if (this.list.isEmpty()) {
         return new ScreenRect(0, 0, 1, 1);
      }

      int local2675_1 = (int)Math.floor(this.floatVal);
      int local2675_2 = (int)Math.floor(this.floatVal2);
      int local2675_3 = Math.max(1, (int)Math.ceil(this.floatVal3 - this.floatVal));
      int local2675_4 = Math.max(1, (int)Math.ceil(this.floatVal4 - this.floatVal2));
      ScreenRect screenRect = new ScreenRect(local2675_1, local2675_2, local2675_3, local2675_4).transformEachVertex(this.matrix);
      return this.rect == null ? screenRect : this.rect.intersection(screenRect);
   }

   @Override
   public ScreenRect scissorArea() {
      return this.rect;
   }

   @Override
   public RenderPipeline pipeline() {
      return TextAtlasRenderer.pipeline2;
   }

   private void vertex(VertexConsumer vertexConsumer, float local2678_2, float local2678_3, float local2678_4, float local2678_5, int local2678_6) {
      vertexConsumer.vertex(this.matrix, local2678_2, local2678_3).texture(local2678_4, local2678_5).color(local2678_6);
   }

   RasterTextRenderState(Matrix3x2f matrix3x2f, TextureSetup textureSetup, ScreenRect screenRect) {
      this.matrix = new Matrix3x2f(matrix3x2f);
      this.textureSetup2 = textureSetup;
      this.rect = screenRect;
   }

   void add(
      float local2680_1,
      float local2680_2,
      float local2680_3,
      float local2680_4,
      float local2680_5,
      float local2680_6,
      float local2680_7,
      float local2680_8,
      int local2680_9,
      int local2680_10,
      int local2680_11,
      int local2680_12
   ) {
      this.list
         .add(
            new RasterTextRenderState.DataRecord(
               local2680_1,
               local2680_2,
               local2680_3,
               local2680_4,
               local2680_5,
               local2680_6,
               local2680_7,
               local2680_8,
               local2680_9,
               local2680_10,
               local2680_11,
               local2680_12
            )
         );
      this.floatVal = Math.min(this.floatVal, local2680_1);
      this.floatVal2 = Math.min(this.floatVal2, local2680_2);
      this.floatVal3 = Math.max(this.floatVal3, local2680_3);
      this.floatVal4 = Math.max(this.floatVal4, local2680_4);
   }

   @Override
   public TextureSetup textureSetup() {
      return this.textureSetup2;
   }

   private record DataRecord(
      float x0,
      float y0,
      float x1,
      float y1,
      float u0,
      float v0,
      float u1,
      float v1,
      int colorTopLeft,
      int colorTopRight,
      int colorBottomRight,
      int colorBottomLeft
   ) {
   }
}
