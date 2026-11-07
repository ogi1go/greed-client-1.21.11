package greed.client.render;

import greed.client.render.ArcDividerDrawData;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

final class ArcDividerRenderState implements SimpleGuiElementRenderState {
   private final Matrix3x2f matrix;
   private final ArcDividerDrawData arcDividerDrawData;
   private final ScreenRect rect;

   ArcDividerDrawData shimmer() {
      return this.arcDividerDrawData;
   }

   private void vertex(VertexConsumer vertexConsumer, float local3099_2, float local3099_3, float local3099_4, float local3099_5, int local3099_6, int local3099_7) {
      vertexConsumer.vertex(this.matrix, local3099_2, local3099_3).texture(local3099_4, local3099_5).color(local3099_6).lineWidth(local3099_7 + 1);
   }

   @Override
   public void setupVertices(VertexConsumer vertexConsumer) {
      int local3100_2 = ShimmerRenderer.getShimmerRenderer().computeInt(this);
      if (local3100_2 >= 0) {
         float local3100_3 = this.arcDividerDrawData.x();
         float local3100_4 = this.arcDividerDrawData.y();
         float local3100_5 = this.arcDividerDrawData.x() + this.arcDividerDrawData.width();
         float local3100_6 = this.arcDividerDrawData.y() + this.arcDividerDrawData.height();
         int local3100_7 = this.arcDividerDrawData.color();
         this.vertex(vertexConsumer, local3100_3, local3100_4, 0.0F, 0.0F, local3100_7, local3100_2);
         this.vertex(vertexConsumer, local3100_3, local3100_6, 0.0F, 1.0F, local3100_7, local3100_2);
         this.vertex(vertexConsumer, local3100_5, local3100_6, 1.0F, 1.0F, local3100_7, local3100_2);
         this.vertex(vertexConsumer, local3100_5, local3100_4, 1.0F, 0.0F, local3100_7, local3100_2);
      }
   }

   @Override
   public RenderPipeline pipeline() {
      return ShimmerRenderer.pipeline2;
   }

   ArcDividerRenderState(Matrix3x2f matrix3x2f, ArcDividerDrawData initialArcDividerDrawData, ScreenRect screenRect) {
      this.matrix = new Matrix3x2f(matrix3x2f);
      this.arcDividerDrawData = initialArcDividerDrawData;
      this.rect = screenRect;
   }

   @Override
   public ScreenRect bounds() {
      int local3103_1 = (int)Math.floor(this.arcDividerDrawData.x());
      int local3103_2 = (int)Math.floor(this.arcDividerDrawData.y());
      int local3103_3 = Math.max(1, (int)Math.ceil(this.arcDividerDrawData.width()));
      int local3103_4 = Math.max(1, (int)Math.ceil(this.arcDividerDrawData.height()));
      ScreenRect screenRect = new ScreenRect(local3103_1, local3103_2, local3103_3, local3103_4).transformEachVertex(this.matrix);
      return this.rect == null ? screenRect : this.rect.intersection(screenRect);
   }

   @Override
   public TextureSetup textureSetup() {
      return TextureSetup.empty();
   }

   @Override
   public ScreenRect scissorArea() {
      return this.rect;
   }
}
