package greed.client.render;

import greed.client.render.OutlineDrawData;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

final class OutlineRenderState implements SimpleGuiElementRenderState {
   private final OutlineDrawData outlineDrawData;
   private final Matrix3x2f matrix;
   private final ScreenRect rect;
   private final ScreenRect rect2;

   @Override
   public ScreenRect scissorArea() {
      return this.rect;
   }

   @Override
   public RenderPipeline pipeline() {
      return OutlineDefaultRenderer.pipeline2;
   }

   OutlineRenderState(Matrix3x2f matrix3x2f, OutlineDrawData initialOutlineDrawData, ScreenRect screenRect) {
      this.outlineDrawData = initialOutlineDrawData;
      this.matrix = new Matrix3x2f(matrix3x2f);
      this.rect = screenRect;
      ScreenRect screenRect2 = new ScreenRect(
            Math.round(initialOutlineDrawData.x()), Math.round(initialOutlineDrawData.y()), Math.round(initialOutlineDrawData.width()), Math.round(initialOutlineDrawData.height())
         )
         .transformEachVertex(this.matrix);
      this.rect2 = screenRect == null ? screenRect2 : screenRect.intersection(screenRect2);
   }

   @Override
   public void setupVertices(VertexConsumer vertexConsumer) {
      int local5156_2 = OutlineDefaultRenderer.getOutlineDefaultRenderer().computeInt(this.outlineDrawData);
      if (local5156_2 >= 0) {
         float local5156_3 = this.outlineDrawData.x();
         float local5156_4 = this.outlineDrawData.y();
         float local5156_5 = this.outlineDrawData.x() + this.outlineDrawData.width();
         float local5156_6 = this.outlineDrawData.y() + this.outlineDrawData.height();
         this.vertex(vertexConsumer, local5156_3, local5156_4, 0, 0, local5156_2);
         this.vertex(vertexConsumer, local5156_3, local5156_6, 0, 255, local5156_2);
         this.vertex(vertexConsumer, local5156_5, local5156_6, 255, 255, local5156_2);
         this.vertex(vertexConsumer, local5156_5, local5156_4, 255, 0, local5156_2);
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

   private void vertex(VertexConsumer vertexConsumer, float local5159_2, float local5159_3, int local5159_4, int local5159_5, int local5159_6) {
      vertexConsumer.vertex(this.matrix, local5159_2, local5159_3).color(local5159_4, local5159_5, 255, 255).lineWidth(local5159_6 + 1);
   }
}
