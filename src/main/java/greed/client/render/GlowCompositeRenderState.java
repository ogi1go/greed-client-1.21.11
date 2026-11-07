package greed.client.render;

import greed.client.render.GlowDrawBatch;
import greed.client.render.GlowCompositeDrawData;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

final class GlowCompositeRenderState implements SimpleGuiElementRenderState {
   private final GlowCompositeDrawData glowCompositeDrawData;
   private final Matrix3x2f matrix;
   private final ScreenRect rect;
   private final ScreenRect rect2;
   private final GlowDrawBatch glowDrawBatch;

   @Override
   public RenderPipeline pipeline() {
      return GlowCompositeRenderer.pipeline2;
   }

   @Override
   public ScreenRect scissorArea() {
      return this.rect;
   }

   @Override
   public TextureSetup textureSetup() {
      return this.glowDrawBatch.textureSetup;
   }

   GlowCompositeRenderState(Matrix3x2f matrix3x2f, GlowCompositeDrawData initialGlowCompositeDrawData, ScreenRect screenRect, GlowDrawBatch initialGlowDrawBatch) {
      this.glowCompositeDrawData = initialGlowCompositeDrawData;
      this.matrix = new Matrix3x2f(matrix3x2f);
      this.rect = screenRect;
      this.glowDrawBatch = initialGlowDrawBatch;
      float local6412_5 = initialGlowCompositeDrawData.effectivePad();
      ScreenRect screenRect2 = new ScreenRect(
            Math.round(initialGlowCompositeDrawData.x() - local6412_5),
            Math.round(initialGlowCompositeDrawData.y() - local6412_5),
            Math.round(initialGlowCompositeDrawData.width() + local6412_5 * 2.0F),
            Math.round(initialGlowCompositeDrawData.height() + local6412_5 * 2.0F)
         )
         .transformEachVertex(this.matrix);
      this.rect2 = screenRect == null ? screenRect2 : screenRect.intersection(screenRect2);
   }

   @Override
   public ScreenRect bounds() {
      return this.rect2;
   }

   private void vertex(VertexConsumer vertexConsumer, float local6414_2, float local6414_3, int local6414_4) {
      vertexConsumer.vertex(this.matrix, local6414_2, local6414_3).color(this.glowCompositeDrawData.color()).lineWidth(local6414_4 + 1);
   }

   @Override
   public void setupVertices(VertexConsumer vertexConsumer) {
      int local6415_2 = GlowCompositeRenderer.getGlowCompositeRenderer().computeInt2(this.glowCompositeDrawData, this.glowDrawBatch);
      if (local6415_2 >= 0) {
         float local6415_3 = this.glowCompositeDrawData.effectivePad();
         float local6415_4 = this.glowCompositeDrawData.x() - local6415_3;
         float local6415_5 = this.glowCompositeDrawData.y() - local6415_3;
         float local6415_6 = this.glowCompositeDrawData.x() + this.glowCompositeDrawData.width() + local6415_3;
         float local6415_7 = this.glowCompositeDrawData.y() + this.glowCompositeDrawData.height() + local6415_3;
         this.vertex(vertexConsumer, local6415_4, local6415_5, local6415_2);
         this.vertex(vertexConsumer, local6415_4, local6415_7, local6415_2);
         this.vertex(vertexConsumer, local6415_6, local6415_7, local6415_2);
         this.vertex(vertexConsumer, local6415_6, local6415_5, local6415_2);
      }
   }
}
