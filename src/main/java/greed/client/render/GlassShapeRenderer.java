package greed.client.render;

import greed.mixin.DrawContextMixin;
import greed.client.render.ShapeDrawing;
import greed.client.render.ScissorStack;
import greed.client.render.CornerRadii;
import greed.client.render.ShapePipelines;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix3x2f;

public final class GlassShapeRenderer {
   private GlassShapeRenderer() {
   }

   public static void renderElement(
      DrawContext drawContext,
      TextureSetup newTextureSetup,
      Matrix3x2f matrix3x2f,
      float local6018_3,
      float local6018_4,
      float local6018_5,
      float local6018_6,
      CornerRadii cornerRadii,
      float local6018_8,
      int local6018_9,
      float local6018_10,
      float local6018_11,
      int local6018_12,
      float local6018_13,
      boolean local6018_14
   ) {
      if (drawContext != null && newTextureSetup != null && !(local6018_5 <= 0.0F) && !(local6018_6 <= 0.0F) && !(local6018_8 <= 0.0F)) {
         float local6018_15 = MinecraftClient.getInstance().getWindow().getScaleFactor() * matrix3x2f.m00();
         ScreenRect screenRect = ScissorStack.getScreenRect();
         ScreenRect screenRect2 = ShapeDrawing.getScreenRect(matrix3x2f, screenRect, local6018_3, local6018_4, local6018_5, local6018_6);
         if (screenRect2 != null) {
            int local6018_18 = MathHelper.clamp(Math.round(MathHelper.clamp(local6018_10, 0.0F, 1.0F) * 255.0F), 0, 255) << 24 | local6018_9 & 16777215;
            ((DrawContextMixin)drawContext)
               .getGuiRenderState()
               .addSimpleElement(
                  new GlassShapeRenderer.DataRecord(
                     newTextureSetup,
                     new Matrix3x2f(matrix3x2f),
                     local6018_3,
                     local6018_4,
                     local6018_3 + local6018_5,
                     local6018_4 + local6018_6,
                     local6018_18,
                     ShapeDrawing.getShort(cornerRadii.maxRadius() * local6018_15, cornerRadii.isRelative()),
                     getShort(local6018_11),
                     getShort(local6018_13),
                     (local6018_12 >> 16 & 0xFF) / 255.0F,
                     (local6018_12 >> 8 & 0xFF) / 255.0F,
                     (local6018_12 & 0xFF) / 255.0F,
                     local6018_8 * local6018_15,
                     local6018_14,
                     screenRect,
                     screenRect2
                  )
               );
         }
      }
   }

   private static short getShort(float local6019_0) {
      return (short)MathHelper.clamp(Math.round(MathHelper.clamp(local6019_0, 0.0F, 1.0F) * 8192.0F), 0, 32767);
   }

   record DataRecord(
      TextureSetup textureSetup,
      Matrix3x2f pose,
      float x1,
      float y1,
      float x2,
      float y2,
      int color,
      short radius,
      short tintAmount,
      short shadowStrength,
      float shadowR,
      float shadowG,
      float shadowB,
      float spread,
      boolean include2D,
      ScreenRect scissorArea,
      ScreenRect bounds
   ) implements SimpleGuiElementRenderState {
      private void emit(VertexConsumer vertexConsumer, float local6029_2, float local6029_3, float local6029_4, float local6029_5) {
         vertexConsumer.vertex(this.pose, local6029_2, local6029_3)
            .color(this.color)
            .texture(local6029_4, local6029_5)
            .overlay(this.radius, this.tintAmount)
            .light(this.shadowStrength, 0)
            .normal(this.shadowR, this.shadowG, this.shadowB)
            .lineWidth(this.spread);
      }

      @Override
      public void setupVertices(VertexConsumer vertexConsumer) {
         this.emit(vertexConsumer, this.x1, this.y1, 0.0F, 0.0F);
         this.emit(vertexConsumer, this.x1, this.y2, 0.0F, 1.0F);
         this.emit(vertexConsumer, this.x2, this.y2, 1.0F, 1.0F);
         this.emit(vertexConsumer, this.x2, this.y1, 1.0F, 0.0F);
      }

      @Override
      public RenderPipeline pipeline() {
         return this.include2D ? ShapePipelines.GLASS_BLUR : ShapePipelines.GLASS;
      }
   }
}
