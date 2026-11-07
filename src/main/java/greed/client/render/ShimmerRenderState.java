package greed.client.render;

import greed.client.render.ShimmerDrawData;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

final class ShimmerRenderState implements SimpleGuiElementRenderState {
   private final ShimmerDrawData shimmerDrawData;
   private final Matrix3x2f matrix;
   private final ScreenRect rect;
   private final ScreenRect rect2;

   ShimmerRenderState(Matrix3x2f matrix3x2f, ShimmerDrawData initialShimmerDrawData, ScreenRect screenRect) {
      this.shimmerDrawData = initialShimmerDrawData;
      this.matrix = new Matrix3x2f(matrix3x2f);
      this.rect = screenRect;
      float local5765_4 = initialShimmerDrawData.spanX();
      float local5765_5 = initialShimmerDrawData.spanY();
      float local5765_6 = initialShimmerDrawData.centerX();
      float local5765_7 = initialShimmerDrawData.centerY() - initialShimmerDrawData.lift() * 0.5F;
      int local5765_8 = (int)Math.floor(local5765_6 - local5765_4);
      int local5765_9 = (int)Math.floor(local5765_7 - local5765_5);
      int local5765_10 = (int)Math.ceil(local5765_4 * 2.0F);
      int local5765_11 = (int)Math.ceil(local5765_5 * 2.0F);
      ScreenRect screenRect2 = new ScreenRect(local5765_8, local5765_9, local5765_10, local5765_11).transformEachVertex(this.matrix);
      this.rect2 = screenRect == null ? screenRect2 : screenRect.intersection(screenRect2);
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
   public void setupVertices(VertexConsumer vertexConsumer) {
      int local5768_2 = ArcDividerRenderer.getArcDividerRenderer().computeInt(this.shimmerDrawData);
      if (local5768_2 >= 0) {
         float local5768_3 = this.shimmerDrawData.spanX();
         float local5768_4 = this.shimmerDrawData.spanY();
         float local5768_5 = this.shimmerDrawData.centerX();
         float local5768_6 = this.shimmerDrawData.centerY() - this.shimmerDrawData.lift() * 0.5F;
         float local5768_7 = local5768_5 - local5768_3;
         float local5768_8 = local5768_6 - local5768_4;
         float local5768_9 = local5768_5 + local5768_3;
         float local5768_10 = local5768_6 + local5768_4;
         this.vertex(vertexConsumer, local5768_7, local5768_8, 0, 0, local5768_2);
         this.vertex(vertexConsumer, local5768_7, local5768_10, 0, 255, local5768_2);
         this.vertex(vertexConsumer, local5768_9, local5768_10, 255, 255, local5768_2);
         this.vertex(vertexConsumer, local5768_9, local5768_8, 255, 0, local5768_2);
      }
   }

   @Override
   public RenderPipeline pipeline() {
      return ArcDividerRenderer.pipeline2;
   }

   private void vertex(VertexConsumer vertexConsumer, float local5770_2, float local5770_3, int local5770_4, int local5770_5, int local5770_6) {
      vertexConsumer.vertex(this.matrix, local5770_2, local5770_3).color(local5770_4, local5770_5, 255, 255).lineWidth(local5770_6 + 1);
   }

   @Override
   public ScreenRect scissorArea() {
      return this.rect;
   }
}
