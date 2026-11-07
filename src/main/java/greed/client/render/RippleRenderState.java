package greed.client.render;

import greed.client.render.RippleDrawData;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

public final class RippleRenderState implements SimpleGuiElementRenderState {
   private final Matrix3x2f matrix;
   private final RippleDrawData rippleDrawData;
   private final ScreenRect rect;

   @Override
   public TextureSetup textureSetup() {
      return TextureSetup.empty();
   }

   @Override
   public void setupVertices(VertexConsumer vertexConsumer) {
      int local1013_2 = RippleRenderer.getRippleRenderer().computeInt(this.rippleDrawData);
      if (local1013_2 >= 0) {
         this.vertex(vertexConsumer, this.rippleDrawData.x(), this.rippleDrawData.y(), 0.0F, 0.0F, local1013_2);
         this.vertex(vertexConsumer, this.rippleDrawData.x(), this.rippleDrawData.y() + this.rippleDrawData.height(), 0.0F, 1.0F, local1013_2);
         this.vertex(vertexConsumer, this.rippleDrawData.x() + this.rippleDrawData.width(), this.rippleDrawData.y() + this.rippleDrawData.height(), 1.0F, 1.0F, local1013_2);
         this.vertex(vertexConsumer, this.rippleDrawData.x() + this.rippleDrawData.width(), this.rippleDrawData.y(), 1.0F, 0.0F, local1013_2);
      }
   }

   @Override
   public ScreenRect bounds() {
      return new ScreenRect(
            Math.round(this.rippleDrawData.x()), Math.round(this.rippleDrawData.y()), Math.round(this.rippleDrawData.width()), Math.round(this.rippleDrawData.height())
         )
         .transformEachVertex(this.matrix);
   }

   public RippleRenderState(Matrix3x2f matrix3x2f, RippleDrawData initialRippleDrawData, ScreenRect screenRect) {
      this.matrix = new Matrix3x2f(matrix3x2f);
      this.rippleDrawData = initialRippleDrawData;
      this.rect = screenRect;
   }

   private void vertex(VertexConsumer vertexConsumer, float local1016_2, float local1016_3, float local1016_4, float local1016_5, int local1016_6) {
      vertexConsumer.vertex(this.matrix, local1016_2, local1016_3).texture(local1016_4, local1016_5).lineWidth(local1016_6 + 1);
   }

   @Override
   public RenderPipeline pipeline() {
      return RippleRenderer.pipeline2;
   }

   @Override
   public ScreenRect scissorArea() {
      return this.rect;
   }
}
