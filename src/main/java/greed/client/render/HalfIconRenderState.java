package greed.client.render;

import greed.client.render.HalfIconDrawData;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

final class HalfIconRenderState implements SimpleGuiElementRenderState {
   private final Matrix3x2f matrix;
   private final TextureSetup textureSetup2;
   private final List<HalfIconRenderState.DataRecord2> list = new ArrayList<>(8);
   private float floatVal = Float.MAX_VALUE;
   private float floatVal2 = Float.MAX_VALUE;
   private float floatVal3 = -Float.MAX_VALUE;
   private float floatVal4 = -Float.MAX_VALUE;

   @Override
   public TextureSetup textureSetup() {
      return this.textureSetup2;
   }

   boolean empty() {
      return this.list.isEmpty();
   }

   @Override
   public ScreenRect scissorArea() {
      return null;
   }

   @Override
   public RenderPipeline pipeline() {
      return RectHalfIconRenderer.pipeline2;
   }

   void add(HalfIconDrawData halfIconDrawData, List<HalfIconRenderState.DataRecord> local4312_2) {
      if (!local4312_2.isEmpty()) {
         this.list.add(new HalfIconRenderState.DataRecord2(halfIconDrawData, local4312_2));
         this.floatVal = Math.min(this.floatVal, halfIconDrawData.x());
         this.floatVal2 = Math.min(this.floatVal2, halfIconDrawData.y());
         this.floatVal3 = Math.max(this.floatVal3, halfIconDrawData.x() + halfIconDrawData.width());
         this.floatVal4 = Math.max(this.floatVal4, halfIconDrawData.y() + halfIconDrawData.height());
      }
   }

   @Override
   public void setupVertices(VertexConsumer vertexConsumer) {
      for (HalfIconRenderState.DataRecord2 local4313_3 : this.list) {
         int local4313_4 = RectHalfIconRenderer.getRectHalfIconRenderer().computeInt2(local4313_3.rectangle());
         if (local4313_4 >= 0) {
            for (HalfIconRenderState.DataRecord local4313_6 : local4313_3.quads()) {
               this.vertex(
                  vertexConsumer, local4313_6.x0(), local4313_6.y0(), local4313_6.u0(), local4313_6.v0(), local4313_6.color(), local4313_4
               );
               this.vertex(
                  vertexConsumer, local4313_6.x1(), local4313_6.y1(), local4313_6.u1(), local4313_6.v1(), local4313_6.color(), local4313_4
               );
               this.vertex(
                  vertexConsumer,
                  local4313_6.x2(),
                  local4313_6.y2(),
                  local4313_6.u2(),
                  local4313_6.v2(),
                  local4313_6.color(),
                  local4313_4
               );
               this.vertex(
                  vertexConsumer,
                  local4313_6.x3(),
                  local4313_6.y3(),
                  local4313_6.u3(),
                  local4313_6.v3(),
                  local4313_6.color(),
                  local4313_4
               );
            }
         }
      }
   }

   @Override
   public ScreenRect bounds() {
      return this.list.isEmpty()
         ? new ScreenRect(0, 0, 1, 1)
         : new ScreenRect(
               (int)Math.floor(this.floatVal),
               (int)Math.floor(this.floatVal2),
               Math.max(1, (int)Math.ceil(this.floatVal3 - this.floatVal)),
               Math.max(1, (int)Math.ceil(this.floatVal4 - this.floatVal2))
            )
            .transformEachVertex(this.matrix);
   }

   private void vertex(VertexConsumer vertexConsumer, float local4315_2, float local4315_3, float local4315_4, float local4315_5, int local4315_6, int local4315_7) {
      vertexConsumer.vertex(this.matrix, local4315_2, local4315_3).texture(local4315_4, local4315_5).color(local4315_6).lineWidth(local4315_7 + 1);
   }

   HalfIconRenderState(Matrix3x2f matrix3x2f, TextureSetup textureSetup) {
      this.matrix = new Matrix3x2f(matrix3x2f);
      this.textureSetup2 = textureSetup;
   }

   record DataRecord(
      float x0,
      float y0,
      float u0,
      float v0,
      float x1,
      float y1,
      float u1,
      float v1,
      float x2,
      float y2,
      float u2,
      float v2,
      float x3,
      float y3,
      float u3,
      float v3,
      int color
   ) {
   }

   private record DataRecord2(HalfIconDrawData rectangle, List<HalfIconRenderState.DataRecord> quads) {
   }
}
