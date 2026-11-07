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
import org.joml.Matrix3x2f;

public final class TexturedShapeRenderer {
   static final int intVal = 0;
   static final int intVal2 = 1;
   static final int intVal3 = 2;
   static final int intVal4 = 3;
   static final int intVal5 = 4;
   static final int intVal6 = 5;

   public static void renderElement(
      DrawContext drawContext,
      Matrix3x2f matrix3x2f,
      float local2560_2,
      float local2560_3,
      float local2560_4,
      float local2560_5,
      CornerRadii cornerRadii,
      float local2560_7,
      float local2560_8,
      float local2560_9,
      int local2560_10,
      int local2560_11,
      int local2560_12,
      int local2560_13,
      int local2560_14
   ) {
      if (drawContext != null && !(local2560_4 <= 0.0F) && !(local2560_5 <= 0.0F) && !(local2560_9 <= 5.0E-4F)) {
         float local2560_15 = local2560_14 != 4 && local2560_14 != 5 ? local2560_7 : local2560_8;
         float local2560_16 = MinecraftClient.getInstance().getWindow().getScaleFactor() * matrix3x2f.m00();
         float local2560_17 = 0.0F;
         if (local2560_14 == 5) {
            local2560_17 = local2560_8 * 0.5F + 1.0F / Math.max(local2560_16, 1.0E-4F);
            local2560_2 -= local2560_17;
            local2560_3 -= local2560_17;
            local2560_4 += local2560_17 * 2.0F;
            local2560_5 += local2560_17 * 2.0F;
         }

         ScreenRect screenRect = ScissorStack.getScreenRect();
         ScreenRect screenRect2 = ShapeDrawing.getScreenRect(matrix3x2f, screenRect, local2560_2, local2560_3, local2560_4, local2560_5);
         if (screenRect2 != null) {
            int local2560_20 = ShapeDrawing.computeInt(local2560_10, local2560_9);
            int local2560_21 = ShapeDrawing.computeInt(local2560_11, local2560_9);
            int local2560_22 = ShapeDrawing.computeInt(local2560_12, local2560_9);
            int local2560_23 = ShapeDrawing.computeInt(local2560_13, local2560_9);
            short local2560_24 = ShapeDrawing.getShort(
               (cornerRadii.getTopLeft() + (cornerRadii.getTopLeft() > 0.0F ? local2560_17 : 0.0F)) * local2560_16, cornerRadii.isRelative()
            );
            short local2560_25 = ShapeDrawing.getShort(
               (cornerRadii.getTopRight() + (cornerRadii.getTopRight() > 0.0F ? local2560_17 : 0.0F)) * local2560_16, cornerRadii.isRelative()
            );
            short local2560_26 = ShapeDrawing.getShort(
               (cornerRadii.getBottomRight() + (cornerRadii.getBottomRight() > 0.0F ? local2560_17 : 0.0F)) * local2560_16, cornerRadii.isRelative()
            );
            short local2560_27 = ShapeDrawing.getShort(
               (cornerRadii.getBottomLeft() + (cornerRadii.getBottomLeft() > 0.0F ? local2560_17 : 0.0F)) * local2560_16, cornerRadii.isRelative()
            );
            ((DrawContextMixin)drawContext)
               .getGuiRenderState()
               .addSimpleElement(
                  new TexturedShapeRenderer.DataRecord(
                     new Matrix3x2f(matrix3x2f),
                     local2560_2,
                     local2560_3,
                     local2560_2 + local2560_4,
                     local2560_3 + local2560_5,
                     local2560_20,
                     local2560_21,
                     local2560_22,
                     local2560_23,
                     local2560_24,
                     local2560_25,
                     local2560_26,
                     local2560_27,
                     local2560_15 * local2560_16,
                     computeFloat(local2560_14),
                     screenRect,
                     screenRect2
                  )
               );
         }
      }
   }

   private TexturedShapeRenderer() {
   }

   private static float computeFloat(int local2562_0) {
      return switch (local2562_0) {
         case 1 -> 0.0F;
         case 2 -> -0.5F;
         case 3 -> 0.25F;
         case 4 -> 1.0F;
         case 5 -> 2.0F;
         default -> -1.0F;
      };
   }

   record DataRecord(
      Matrix3x2f pose,
      float x1,
      float y1,
      float x2,
      float y2,
      int colorTopLeft,
      int colorBottomLeft,
      int colorBottomRight,
      int colorTopRight,
      short rBR,
      short rTR,
      short rBL,
      short rTL,
      float param,
      float typeSignal,
      ScreenRect scissorArea,
      ScreenRect bounds
   ) implements SimpleGuiElementRenderState {
      @Override
      public RenderPipeline pipeline() {
         return ShapePipelines.ROUNDED;
      }

      private void emit(VertexConsumer vertexConsumer, float local2575_2, float local2575_3, float local2575_4, float local2575_5, int local2575_6) {
         vertexConsumer.vertex(this.pose, local2575_2, local2575_3)
            .color(local2575_6)
            .texture(local2575_4, local2575_5)
            .overlay(this.rBR, this.rTR)
            .light(this.rBL, this.rTL)
            .normal(this.typeSignal, 0.0F, 0.0F)
            .lineWidth(this.param);
      }

      @Override
      public void setupVertices(VertexConsumer vertexConsumer) {
         this.emit(vertexConsumer, this.x1, this.y1, 0.0F, 0.0F, this.colorTopLeft);
         this.emit(vertexConsumer, this.x1, this.y2, 0.0F, 1.0F, this.colorBottomLeft);
         this.emit(vertexConsumer, this.x2, this.y2, 1.0F, 1.0F, this.colorBottomRight);
         this.emit(vertexConsumer, this.x2, this.y1, 1.0F, 0.0F, this.colorTopRight);
      }

      @Override
      public TextureSetup textureSetup() {
         return TextureSetup.empty();
      }
   }
}
