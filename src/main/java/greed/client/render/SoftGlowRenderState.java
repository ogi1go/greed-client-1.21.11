package greed.client.render;

import greed.client.render.SoftGlowDrawData;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

final class SoftGlowRenderState implements SimpleGuiElementRenderState {
   private final SoftGlowDrawData softGlowDrawData;
   private final Matrix3x2f matrix;
   private final ScreenRect rect;
   private final ScreenRect rect2;

   @Override
   public ScreenRect scissorArea() {
      return this.rect;
   }

   SoftGlowRenderState(Matrix3x2f matrix3x2f, SoftGlowDrawData initialSoftGlowDrawData, ScreenRect screenRect) {
      this.softGlowDrawData = initialSoftGlowDrawData;
      this.matrix = new Matrix3x2f(matrix3x2f);
      this.rect = screenRect;
      float local6593_4 = initialSoftGlowDrawData.centerX();
      float local6593_5 = initialSoftGlowDrawData.centerY();
      float local6593_6 = initialSoftGlowDrawData.extentX();
      float local6593_7 = initialSoftGlowDrawData.extentY();
      int local6593_8 = (int)Math.floor(local6593_4 - local6593_6);
      int local6593_9 = (int)Math.floor(local6593_5 - local6593_7);
      int local6593_10 = (int)Math.ceil(local6593_6 * 2.0F);
      int local6593_11 = (int)Math.ceil(local6593_7 * 2.0F);
      ScreenRect screenRect2 = new ScreenRect(local6593_8, local6593_9, local6593_10, local6593_11).transformEachVertex(this.matrix);
      this.rect2 = screenRect == null ? screenRect2 : screenRect.intersection(screenRect2);
   }

   @Override
   public ScreenRect bounds() {
      return this.rect2;
   }

   private void vertex(VertexConsumer vertexConsumer, float local6595_2, float local6595_3, int local6595_4, int local6595_5, int local6595_6) {
      vertexConsumer.vertex(this.matrix, local6595_2, local6595_3).color(local6595_4, local6595_5, 255, 255).lineWidth(local6595_6 + 1);
   }

   @Override
   public TextureSetup textureSetup() {
      return TextureSetup.empty();
   }

   @Override
   public void setupVertices(VertexConsumer vertexConsumer) {
      int local6597_2 = SoftGlowRenderer.getSoftGlowRenderer().computeInt(this.softGlowDrawData);
      if (local6597_2 >= 0) {
         float local6597_3 = this.softGlowDrawData.centerX();
         float local6597_4 = this.softGlowDrawData.centerY();
         float local6597_5 = this.softGlowDrawData.extentX();
         float local6597_6 = this.softGlowDrawData.extentY();
         float local6597_7 = local6597_3 - local6597_5;
         float local6597_8 = local6597_4 - local6597_6;
         float local6597_9 = local6597_3 + local6597_5;
         float local6597_10 = local6597_4 + local6597_6;
         this.vertex(vertexConsumer, local6597_7, local6597_8, 0, 0, local6597_2);
         this.vertex(vertexConsumer, local6597_7, local6597_10, 0, 255, local6597_2);
         this.vertex(vertexConsumer, local6597_9, local6597_10, 255, 255, local6597_2);
         this.vertex(vertexConsumer, local6597_9, local6597_8, 255, 0, local6597_2);
      }
   }

   @Override
   public RenderPipeline pipeline() {
      return SoftGlowRenderer.pipeline2;
   }
}
