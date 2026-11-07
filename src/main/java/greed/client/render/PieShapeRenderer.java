package greed.client.render;

import greed.mixin.DrawContextMixin;
import greed.client.render.ShapeDrawing;
import greed.client.render.ScissorStack;
import greed.client.render.ShapePipelines;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;

public final class PieShapeRenderer {
   public static void renderElement(
      DrawContext drawContext,
      TextureSetup newTextureSetup,
      Matrix3x2f matrix3x2f,
      float centerX,
      float centerY,
      float innerRadius,
      float radius,
      float startAngle,
      float sweepAngle,
      float thickness,
      float cornerRadius,
      int color,
      float alpha,
      float tintMix
   ) {
      if (drawContext != null && !(alpha <= 5.0E-4F)) {
         List<Vector2f> shapePoints = buildRingOutline(innerRadius, radius, sweepAngle, thickness, cornerRadius);
         if (shapePoints != null) {
            float midAngleRad = (float)Math.toRadians(startAngle + sweepAngle / 2.0F);
            float cosMid = (float)Math.cos(midAngleRad);
            float sinMid = (float)Math.sin(midAngleRad);
            float centroidX = 0.0F;
            float centroidY = 0.0F;

            for (Vector2f vector2f : shapePoints) {
               vector2f.set(
                  centerX + vector2f.x * cosMid - vector2f.y * sinMid,
                  centerY + vector2f.x * sinMid + vector2f.y * cosMid
               );
               centroidX += vector2f.x / shapePoints.size();
               centroidY += vector2f.y / shapePoints.size();
            }

            ScreenRect screenRect = ScissorStack.getScreenRect();
            ScreenRect screenRect2 = ShapeDrawing.getScreenRect(
               matrix3x2f,
               screenRect,
               centerX - radius - 2.0F,
               centerY - radius - 2.0F,
               radius * 2.0F + 4.0F,
               radius * 2.0F + 4.0F
            );
            if (screenRect2 != null) {
               boolean hasTexture = newTextureSetup != null;
               ((DrawContextMixin)drawContext)
                  .getGuiRenderState()
                  .addSimpleElement(
                     new PieShapeRenderer.DataRecord(
                        hasTexture ? newTextureSetup : TextureSetup.empty(),
                        new Matrix3x2f(matrix3x2f),
                        shapePoints,
                        centroidX,
                        centroidY,
                        ShapeDrawing.computeInt(color, alpha),
                        tintMix,
                        hasTexture,
                        screenRect,
                        screenRect2
                     )
                  );
            }
         }
      }
   }

   private static List<Vector2f> buildArcOutline(
      float centerX, float centerY, float radius, float startAngle, float sweepAngle, float thickness, float cornerRadius
   ) {
      if (!(radius <= 0.0F) && !(Math.abs(sweepAngle) < 0.01F)) {
         if (sweepAngle < 0.0F) {
            startAngle += sweepAngle;
            sweepAngle = -sweepAngle;
         }

         List<Vector2f> points = buildSectorOutline(radius, sweepAngle, thickness, cornerRadius);
         if (points == null) {
            return null;
         }

         float midAngleRad = (float)Math.toRadians(startAngle + sweepAngle / 2.0F);
         float cosMid = (float)Math.cos(midAngleRad);
         float sinMid = (float)Math.sin(midAngleRad);

         for (Vector2f vector2f : points) {
            vector2f.set(
               centerX + vector2f.x * cosMid - vector2f.y * sinMid,
               centerY + vector2f.x * sinMid + vector2f.y * cosMid
            );
         }

         return points;
      } else {
         return null;
      }
   }

   private static List<Vector2f> buildRingOutline(float innerRadius, float outerRadius, float sweepAngle, float ringThickness, float cornerRadius) {
      float halfSweepAngleRad = (float)Math.toRadians(Math.abs(sweepAngle)) / 2.0F;
      float effectiveCornerRadius = Math.max(0.5F, Math.min(cornerRadius, (outerRadius - innerRadius) * 0.45F));
      float innerArcRadius = Math.max(0.0F, ringThickness) / 2.0F + effectiveCornerRadius;
      float outerArcRadius = outerRadius - effectiveCornerRadius;
      float innerEdgeRadius = innerRadius + effectiveCornerRadius;
      if (!(innerEdgeRadius <= innerArcRadius) && !(outerArcRadius <= innerEdgeRadius)) {
         float outerHalfAngle = halfSweepAngleRad - (float)Math.asin(innerArcRadius / outerArcRadius);
         float innerHalfAngle = halfSweepAngleRad - (float)Math.asin(innerArcRadius / innerEdgeRadius);
         if (!(outerHalfAngle <= 0.0F) && !(innerHalfAngle <= 0.0F)) {
            List<Vector2f> points = new ArrayList<>();
            int outerSegmentCount = Math.max(2, Math.round((float)Math.toDegrees(outerHalfAngle * 2.0F) / 3.0F));

            for (int index = 0; index <= outerSegmentCount; index++) {
               float outerArcAngle = -outerHalfAngle + outerHalfAngle * 2.0F * index / outerSegmentCount;
               points.add(new Vector2f(outerArcRadius * (float)Math.cos(outerArcAngle), outerArcRadius * (float)Math.sin(outerArcAngle)));
            }

            int innerSegmentCount = Math.max(2, Math.round((float)Math.toDegrees(innerHalfAngle * 2.0F) / 3.0F));

            for (int index2 = 0; index2 <= innerSegmentCount; index2++) {
               float innerArcAngle = innerHalfAngle - innerHalfAngle * 2.0F * index2 / innerSegmentCount;
               points.add(new Vector2f(innerEdgeRadius * (float)Math.cos(innerArcAngle), innerEdgeRadius * (float)Math.sin(innerArcAngle)));
            }

            return roundOutlineCorners(points, effectiveCornerRadius);
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   private static float computeSegmentAngle(Vector2f vector2f, Vector2f vector2f2, float centerX, float centerY) {
      float deltaX = vector2f2.x - vector2f.x;
      float deltaY = vector2f2.y - vector2f.y;
      float normalX = deltaY;
      float normalY = -deltaX;
      float midOffsetX = (vector2f.x + vector2f2.x) / 2.0F - centerX;
      float midOffsetY = (vector2f.y + vector2f2.y) / 2.0F - centerY;
      if (normalX * midOffsetX + normalY * midOffsetY < 0.0F) {
         normalX = -normalX;
         normalY = -normalY;
      }

      return (float)Math.atan2(normalY, normalX);
   }

   public static void renderElement2(
      DrawContext drawContext,
      TextureSetup newTextureSetup,
      Matrix3x2f matrix3x2f,
      float centerX,
      float centerY,
      float radius,
      float startAngle,
      float sweepAngle,
      float thickness,
      float cornerRadius,
      int color,
      float alpha,
      float tintMix
   ) {
      if (drawContext != null && !(alpha <= 5.0E-4F)) {
         List<Vector2f> shapePoints = buildArcOutline(centerX, centerY, radius, startAngle, sweepAngle, thickness, cornerRadius);
         if (shapePoints != null) {
            ScreenRect screenRect = ScissorStack.getScreenRect();
            ScreenRect screenRect2 = ShapeDrawing.getScreenRect(
               matrix3x2f,
               screenRect,
               centerX - radius - 2.0F,
               centerY - radius - 2.0F,
               radius * 2.0F + 4.0F,
               radius * 2.0F + 4.0F
            );
            if (screenRect2 != null) {
               float centroidX = 0.0F;
               float centroidY = 0.0F;

               for (Vector2f vector2f : shapePoints) {
                  centroidX += vector2f.x / shapePoints.size();
                  centroidY += vector2f.y / shapePoints.size();
               }

               boolean hasTexture = newTextureSetup != null;
               ((DrawContextMixin)drawContext)
                  .getGuiRenderState()
                  .addSimpleElement(
                     new PieShapeRenderer.DataRecord(
                        hasTexture ? newTextureSetup : TextureSetup.empty(),
                        new Matrix3x2f(matrix3x2f),
                        shapePoints,
                        centroidX,
                        centroidY,
                        ShapeDrawing.computeInt(color, alpha),
                        tintMix,
                        hasTexture,
                        screenRect,
                        screenRect2
                     )
                  );
            }
         }
      }
   }

   private static List<Vector2f> buildSectorOutline(float radius, float sweepAngle, float thickness, float cornerRadius) {
      float halfSweepAngleRad = (float)Math.toRadians(sweepAngle) / 2.0F;
      float sinHalfAngle = (float)Math.sin(halfSweepAngleRad);
      if (sinHalfAngle < 1.0E-4F) {
         return null;
      }

      float innerRadius = Math.max(0.0F, thickness) / 2.0F;
      float effectiveCornerRadius = Math.max(0.5F, Math.min(cornerRadius, (radius - innerRadius) * 0.45F));
      float innerArcRadius = innerRadius + effectiveCornerRadius;
      float outerArcRadius = radius - effectiveCornerRadius;
      float apexDistance = innerArcRadius / sinHalfAngle;
      if (!(outerArcRadius <= 0.0F) && !(apexDistance >= outerArcRadius)) {
         float cosHalfAngle = (float)Math.cos(halfSweepAngleRad);
         float sinHalfAngle2 = (float)Math.sin(halfSweepAngleRad);
         float arcCenterX = apexDistance * cosHalfAngle;
         float arcCenterY = -arcCenterX
            + (float)Math.sqrt(Math.max(0.0F, arcCenterX * arcCenterX - apexDistance * apexDistance + outerArcRadius * outerArcRadius));
         float outerArcHalfAngle = (float)Math.atan2(sinHalfAngle2 * arcCenterY, apexDistance + cosHalfAngle * arcCenterY);
         if (outerArcHalfAngle <= 0.0F) {
            return null;
         }

         List<Vector2f> points = new ArrayList<>();
         points.add(new Vector2f(apexDistance, 0.0F));
         int segmentCount = Math.max(2, Math.round((float)Math.toDegrees(outerArcHalfAngle * 2.0F) / 4.0F));

         for (int index = 0; index <= segmentCount; index++) {
            float arcAngle = -outerArcHalfAngle + outerArcHalfAngle * 2.0F * index / segmentCount;
            points.add(new Vector2f(outerArcRadius * (float)Math.cos(arcAngle), outerArcRadius * (float)Math.sin(arcAngle)));
         }

         return roundOutlineCorners(points, effectiveCornerRadius);
      } else {
         return null;
      }
   }

   private static List<Vector2f> roundOutlineCorners(List<Vector2f> points, float cornerRadius) {
      int pointCount = points.size();
      float centroidX = 0.0F;
      float centroidY = 0.0F;

      for (Vector2f vector2f : points) {
         centroidX += vector2f.x / pointCount;
         centroidY += vector2f.y / pointCount;
      }

      List<Vector2f> roundedPoints = new ArrayList<>();

      for (int index = 0; index < pointCount; index++) {
         Vector2f vector2f2 = points.get((index - 1 + pointCount) % pointCount);
         Vector2f vector2f3 = points.get(index);
         Vector2f vector2f4 = points.get((index + 1) % pointCount);
         float previousEdgeAngle = computeSegmentAngle(vector2f2, vector2f3, centroidX, centroidY);
         float nextEdgeAngle = computeSegmentAngle(vector2f3, vector2f4, centroidX, centroidY);
         float angleDelta = nextEdgeAngle - previousEdgeAngle;

         while (angleDelta <= (float) -Math.PI) {
            angleDelta += (float) (Math.PI * 2);
         }

         while (angleDelta > (float) Math.PI) {
            angleDelta -= (float) (Math.PI * 2);
         }

         int segmentCount = Math.max(1, Math.round(Math.abs(angleDelta) / 0.2F));

         for (int index2 = 0; index2 <= segmentCount; index2++) {
            float arcAngle = previousEdgeAngle + angleDelta * index2 / segmentCount;
            roundedPoints.add(
               new Vector2f(vector2f3.x + (float)Math.cos(arcAngle) * cornerRadius, vector2f3.y + (float)Math.sin(arcAngle) * cornerRadius)
            );
         }
      }

      return roundedPoints;
   }

   private PieShapeRenderer() {
   }

   record DataRecord(
      TextureSetup textureSetup,
      Matrix3x2f pose,
      List<Vector2f> shape,
      float anchorX,
      float anchorY,
      int color,
      float tintMix,
      boolean blur,
      ScreenRect scissorArea,
      ScreenRect bounds
   ) implements SimpleGuiElementRenderState {
      @Override
      public RenderPipeline pipeline() {
         return this.blur ? ShapePipelines.PIE_BLUR : ShapePipelines.PIE;
      }

      @Override
      public void setupVertices(VertexConsumer vertexConsumer) {
         int pointCount = this.shape.size();

         for (int index = 0; index < pointCount; index++) {
            Vector2f vector2f = this.shape.get(index);
            Vector2f vector2f2 = this.shape.get((index + 1) % pointCount);
            this.emit(vertexConsumer, this.anchorX, this.anchorY, 0.0F);
            this.emit(vertexConsumer, this.anchorX, this.anchorY, 0.0F);
            this.emit(vertexConsumer, vector2f2.x, vector2f2.y, 1.0F);
            this.emit(vertexConsumer, vector2f.x, vector2f.y, 1.0F);
         }
      }

      private void emit(VertexConsumer vertexConsumer, float posX, float posY, float textureU) {
         vertexConsumer.vertex(this.pose, posX, posY)
            .color(this.color)
            .texture(textureU, 0.0F)
            .overlay(0, 0)
            .light(0, 0)
            .normal(0.0F, 0.0F, 0.0F)
            .lineWidth(this.tintMix);
      }
   }
}
