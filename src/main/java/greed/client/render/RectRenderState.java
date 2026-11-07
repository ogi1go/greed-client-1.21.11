package greed.client.render;

import greed.client.render.RectDrawData;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

final class RectRenderState implements SimpleGuiElementRenderState {
   private final RectDrawData rectDrawData;
   private final Matrix3x2f matrix;
   private final ScreenRect rect;
   private final ScreenRect rect2;

   @Override
   public RenderPipeline pipeline() {
      return RectDefaultRenderer.pipeline2;
   }

   @Override
   public void setupVertices(VertexConsumer vertexConsumer) {
      int local3175_2 = RectDefaultRenderer.getRectDefaultRenderer().computeInt(this.rectDrawData);
      if (local3175_2 >= 0) {
         float local3175_3 = this.rectDrawData.x();
         float local3175_4 = this.rectDrawData.y();
         float local3175_5 = this.rectDrawData.x() + this.rectDrawData.width();
         float local3175_6 = this.rectDrawData.y() + this.rectDrawData.height();
         this.vertex(vertexConsumer, local3175_3, local3175_4, 0, 0, local3175_2);
         this.vertex(vertexConsumer, local3175_3, local3175_6, 0, 255, local3175_2);
         this.vertex(vertexConsumer, local3175_5, local3175_6, 255, 255, local3175_2);
         this.vertex(vertexConsumer, local3175_5, local3175_4, 255, 0, local3175_2);
      }
   }

   @Override
   public ScreenRect bounds() {
      return this.rect2;
   }

   @Override
   public TextureSetup textureSetup() {
      return TextureSetup.empty();
   }

   RectRenderState(Matrix3x2f matrix3x2f, RectDrawData initialRectDrawData, ScreenRect screenRect) {
      this.rectDrawData = initialRectDrawData;
      this.matrix = new Matrix3x2f(matrix3x2f);
      this.rect = screenRect;
      ScreenRect screenRect2 = new ScreenRect(
            (int)Math.floor(initialRectDrawData.x()),
            (int)Math.floor(initialRectDrawData.y()),
            (int)Math.ceil(initialRectDrawData.width()) + 1,
            (int)Math.ceil(initialRectDrawData.height()) + 1
         )
         .transformEachVertex(this.matrix);
      this.rect2 = screenRect == null ? screenRect2 : screenRect.intersection(screenRect2);
   }

   @Override
   public ScreenRect scissorArea() {
      return this.rect;
   }

   private void vertex(VertexConsumer vertexConsumer, float local3180_2, float local3180_3, int local3180_4, int local3180_5, int local3180_6) {
      vertexConsumer.vertex(this.matrix, local3180_2, local3180_3).color(local3180_4, local3180_5, 255, 255).lineWidth(local3180_6 + 1);
   }
}
