package greed.client.render;

import greed.client.render.CornerBracketDrawData;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

final class CornerBracketRenderState implements SimpleGuiElementRenderState {
   private final CornerBracketDrawData cornerBracketDrawData;
   private final Matrix3x2f matrix;
   private final ScreenRect rect;
   private final ScreenRect rect2;

   CornerBracketRenderState(Matrix3x2f matrix3x2f, CornerBracketDrawData initialCornerBracketDrawData, ScreenRect screenRect) {
      this.cornerBracketDrawData = initialCornerBracketDrawData;
      this.matrix = new Matrix3x2f(matrix3x2f);
      this.rect = screenRect;
      float local5325_4 = pad(initialCornerBracketDrawData);
      int local5325_5 = (int)Math.floor(initialCornerBracketDrawData.x() - local5325_4);
      int local5325_6 = (int)Math.floor(initialCornerBracketDrawData.y() - local5325_4);
      int local5325_7 = (int)Math.ceil(initialCornerBracketDrawData.x() + initialCornerBracketDrawData.size() + local5325_4) - local5325_5;
      int local5325_8 = (int)Math.ceil(initialCornerBracketDrawData.y() + initialCornerBracketDrawData.size() + local5325_4) - local5325_6;
      ScreenRect screenRect2 = new ScreenRect(local5325_5, local5325_6, local5325_7, local5325_8).transformEachVertex(this.matrix);
      this.rect2 = screenRect == null ? screenRect2 : screenRect.intersection(screenRect2);
   }

   @Override
   public void setupVertices(VertexConsumer vertexConsumer) {
      int local5326_2 = CornerBracketRenderer.getCornerBracketRenderer().computeInt(this.cornerBracketDrawData);
      if (local5326_2 >= 0) {
         float local5326_3 = pad(this.cornerBracketDrawData);
         float local5326_4 = this.cornerBracketDrawData.x() - local5326_3;
         float local5326_5 = this.cornerBracketDrawData.y() - local5326_3;
         float local5326_6 = this.cornerBracketDrawData.x() + this.cornerBracketDrawData.size() + local5326_3;
         float local5326_7 = this.cornerBracketDrawData.y() + this.cornerBracketDrawData.size() + local5326_3;
         int local5326_8 = this.cornerBracketDrawData.quadrant() & 3;
         boolean local5326_9 = local5326_8 == 0 || local5326_8 == 3;
         boolean local5326_10 = local5326_8 == 0 || local5326_8 == 1;
         int local5326_11 = local5326_9 ? 255 : 0;
         int local5326_12 = 255 - local5326_11;
         int local5326_13 = local5326_10 ? 255 : 0;
         int local5326_14 = 255 - local5326_13;
         this.vertex(vertexConsumer, local5326_4, local5326_5, local5326_11, local5326_13, local5326_2);
         this.vertex(vertexConsumer, local5326_4, local5326_7, local5326_11, local5326_14, local5326_2);
         this.vertex(vertexConsumer, local5326_6, local5326_7, local5326_12, local5326_14, local5326_2);
         this.vertex(vertexConsumer, local5326_6, local5326_5, local5326_12, local5326_13, local5326_2);
      }
   }

   private void vertex(VertexConsumer vertexConsumer, float local5327_2, float local5327_3, int local5327_4, int local5327_5, int local5327_6) {
      vertexConsumer.vertex(this.matrix, local5327_2, local5327_3).color(local5327_4, local5327_5, 255, 255).lineWidth(local5327_6 + 1);
   }

   private static float pad(CornerBracketDrawData newCornerBracketDrawData) {
      return Math.max(newCornerBracketDrawData.smoothness(), 0.05F);
   }

   @Override
   public TextureSetup textureSetup() {
      return TextureSetup.empty();
   }

   @Override
   public ScreenRect bounds() {
      return this.rect2;
   }

   @Override
   public RenderPipeline pipeline() {
      return CornerBracketRenderer.pipeline2;
   }

   @Override
   public ScreenRect scissorArea() {
      return this.rect;
   }
}
