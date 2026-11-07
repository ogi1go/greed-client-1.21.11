package greed.client.render;

import greed.client.render.CircleDrawData;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

final class CircleRenderState implements SimpleGuiElementRenderState {
   private final CircleDrawData circleDrawData;
   private final Matrix3x2f matrix;
   private final ScreenRect rect;
   private final ScreenRect rect2;

   @Override
   public ScreenRect bounds() {
      return this.rect2;
   }

   @Override
   public ScreenRect scissorArea() {
      return this.rect;
   }

   private static float extent(CircleDrawData newCircleDrawData) {
      return newCircleDrawData.radius() + Math.max(newCircleDrawData.smoothness(), 0.0F);
   }

   private void vertex(VertexConsumer vertexConsumer, float local7336_2, float local7336_3, int local7336_4, int local7336_5, int local7336_6) {
      vertexConsumer.vertex(this.matrix, local7336_2, local7336_3).color(local7336_4, local7336_5, 255, 255).lineWidth(local7336_6 + 1);
   }

   CircleRenderState(Matrix3x2f matrix3x2f, CircleDrawData initialCircleDrawData, ScreenRect screenRect) {
      this.circleDrawData = initialCircleDrawData;
      this.matrix = new Matrix3x2f(matrix3x2f);
      this.rect = screenRect;
      float local7337_4 = extent(initialCircleDrawData);
      int local7337_5 = (int)Math.floor(initialCircleDrawData.x() - local7337_4);
      int local7337_6 = (int)Math.floor(initialCircleDrawData.y() - local7337_4);
      int local7337_7 = (int)Math.ceil(local7337_4 * 2.0F);
      ScreenRect screenRect2 = new ScreenRect(local7337_5, local7337_6, local7337_7, local7337_7).transformEachVertex(this.matrix);
      this.rect2 = screenRect == null ? screenRect2 : screenRect.intersection(screenRect2);
   }

   @Override
   public TextureSetup textureSetup() {
      return TextureSetup.empty();
   }

   @Override
   public void setupVertices(VertexConsumer vertexConsumer) {
      int local7339_2 = CircleRenderer.getCircleRenderer().computeInt(this.circleDrawData);
      if (local7339_2 >= 0) {
         float local7339_3 = extent(this.circleDrawData);
         float local7339_4 = this.circleDrawData.x() - local7339_3;
         float local7339_5 = this.circleDrawData.y() - local7339_3;
         float local7339_6 = this.circleDrawData.x() + local7339_3;
         float local7339_7 = this.circleDrawData.y() + local7339_3;
         this.vertex(vertexConsumer, local7339_4, local7339_5, 0, 0, local7339_2);
         this.vertex(vertexConsumer, local7339_4, local7339_7, 0, 255, local7339_2);
         this.vertex(vertexConsumer, local7339_6, local7339_7, 255, 255, local7339_2);
         this.vertex(vertexConsumer, local7339_6, local7339_5, 255, 0, local7339_2);
      }
   }

   @Override
   public RenderPipeline pipeline() {
      return CircleRenderer.pipeline2;
   }
}
