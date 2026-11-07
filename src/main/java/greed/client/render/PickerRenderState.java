package greed.client.render;

import greed.client.render.PickerDrawData;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

final class PickerRenderState implements SimpleGuiElementRenderState {
   private final PickerDrawData pickerDrawData;
   private final Matrix3x2f matrix;
   private final ScreenRect rect;
   private final ScreenRect rect2;

   @Override
   public void setupVertices(VertexConsumer vertexConsumer) {
      int local2063_2 = PickerRenderer.getPickerRenderer().computeInt(this.pickerDrawData);
      if (local2063_2 >= 0) {
         float local2063_3 = this.pickerDrawData.x();
         float local2063_4 = this.pickerDrawData.y();
         float local2063_5 = this.pickerDrawData.x() + this.pickerDrawData.w();
         float local2063_6 = this.pickerDrawData.y() + this.pickerDrawData.h();
         this.vertex(vertexConsumer, local2063_3, local2063_4, 0, 0, local2063_2);
         this.vertex(vertexConsumer, local2063_3, local2063_6, 0, 255, local2063_2);
         this.vertex(vertexConsumer, local2063_5, local2063_6, 255, 255, local2063_2);
         this.vertex(vertexConsumer, local2063_5, local2063_4, 255, 0, local2063_2);
      }
   }

   @Override
   public TextureSetup textureSetup() {
      return TextureSetup.empty();
   }

   @Override
   public ScreenRect bounds() {
      return this.rect2;
   }

   private void vertex(VertexConsumer vertexConsumer, float local2066_2, float local2066_3, int local2066_4, int local2066_5, int local2066_6) {
      vertexConsumer.vertex(this.matrix, local2066_2, local2066_3).color(local2066_4, local2066_5, 255, 255).lineWidth(local2066_6 + 1);
   }

   @Override
   public ScreenRect scissorArea() {
      return this.rect;
   }

   @Override
   public RenderPipeline pipeline() {
      return PickerRenderer.pipeline2;
   }

   PickerRenderState(Matrix3x2f matrix3x2f, PickerDrawData initialPickerDrawData, ScreenRect screenRect) {
      this.pickerDrawData = initialPickerDrawData;
      this.matrix = new Matrix3x2f(matrix3x2f);
      this.rect = screenRect;
      int local2069_4 = (int)Math.floor(initialPickerDrawData.x());
      int local2069_5 = (int)Math.floor(initialPickerDrawData.y());
      int local2069_6 = (int)Math.ceil(initialPickerDrawData.w());
      int local2069_7 = (int)Math.ceil(initialPickerDrawData.h());
      ScreenRect screenRect2 = new ScreenRect(local2069_4, local2069_5, local2069_6, local2069_7).transformEachVertex(this.matrix);
      this.rect2 = screenRect == null ? screenRect2 : screenRect.intersection(screenRect2);
   }
}
