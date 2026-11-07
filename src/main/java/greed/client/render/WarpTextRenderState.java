package greed.client.render;

import greed.client.render.WarpTextDrawData;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

final class WarpTextRenderState implements SimpleGuiElementRenderState {
   private final Matrix3x2f matrix;
   private final TextureSetup textureSetup2;
   private final ScreenRect rect;
   private final WarpTextDrawData warpTextDrawData;

   private void vertex(VertexConsumer vertexConsumer, float local4118_2, float local4118_3, float local4118_4, float local4118_5, int local4118_6) {
      vertexConsumer.vertex(this.matrix, local4118_2, local4118_3).texture(local4118_4, local4118_5).color(this.warpTextDrawData.color()).lineWidth(local4118_6 + 1);
   }

   @Override
   public ScreenRect bounds() {
      return this.rect != null
         ? this.rect
         : new ScreenRect(
               (int)Math.floor(this.warpTextDrawData.x()),
               (int)Math.floor(this.warpTextDrawData.y()),
               Math.max(1, (int)Math.ceil(this.warpTextDrawData.width())),
               Math.max(1, (int)Math.ceil(this.warpTextDrawData.height()))
            )
            .transformEachVertex(this.matrix);
   }

   @Override
   public void setupVertices(VertexConsumer vertexConsumer) {
      int local4120_2 = WarpTextRenderer.getWarpTextRenderer().computeInt(this.warpTextDrawData);
      if (local4120_2 >= 0) {
         this.vertex(vertexConsumer, this.warpTextDrawData.x(), this.warpTextDrawData.y(), 0.0F, 0.0F, local4120_2);
         this.vertex(vertexConsumer, this.warpTextDrawData.x(), this.warpTextDrawData.y() + this.warpTextDrawData.height(), 0.0F, 1.0F, local4120_2);
         this.vertex(vertexConsumer, this.warpTextDrawData.x() + this.warpTextDrawData.width(), this.warpTextDrawData.y() + this.warpTextDrawData.height(), 1.0F, 1.0F, local4120_2);
         this.vertex(vertexConsumer, this.warpTextDrawData.x() + this.warpTextDrawData.width(), this.warpTextDrawData.y(), 1.0F, 0.0F, local4120_2);
      }
   }

   @Override
   public RenderPipeline pipeline() {
      return WarpTextRenderer.pipeline2;
   }

   @Override
   public ScreenRect scissorArea() {
      return this.rect;
   }

   WarpTextRenderState(Matrix3x2f matrix3x2f, TextureSetup textureSetup, ScreenRect screenRect, WarpTextDrawData initialWarpTextDrawData) {
      this.matrix = new Matrix3x2f(matrix3x2f);
      this.textureSetup2 = textureSetup;
      this.rect = screenRect;
      this.warpTextDrawData = initialWarpTextDrawData;
   }

   @Override
   public TextureSetup textureSetup() {
      return this.textureSetup2;
   }
}
