package greed.client.render;

import greed.client.render.RasterGlyphQuad;
import greed.client.render.PixelSnap;
import greed.client.render.FontVertexQuad;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

final class RasterGlyphRenderState implements SimpleGuiElementRenderState {
   private final Matrix3x2f matrix;
   private final FontTexture fontTexture;
   private final ScreenRect rect;
   private final RenderPipeline pipeline2;
   private final List<FontVertexQuad> quads = new ArrayList<>(128);
   private float minX = Float.MAX_VALUE;
   private float minY = Float.MAX_VALUE;
   private float maxX = -Float.MAX_VALUE;
   private float maxY = -Float.MAX_VALUE;

   @Override
   public ScreenRect bounds() {
      if (this.quads.isEmpty()) {
         return new ScreenRect(0, 0, 1, 1);
      }

      int screenMinX = (int)Math.floor(this.minX);
      int screenMinY = (int)Math.floor(this.minY);
      int screenWidth = Math.max(1, (int)Math.ceil(this.maxX - this.minX));
      int screenHeight = Math.max(1, (int)Math.ceil(this.maxY - this.minY));
      ScreenRect screenRect = new ScreenRect(screenMinX, screenMinY, screenWidth, screenHeight).transformEachVertex(this.matrix);
      return this.rect == null ? screenRect : this.rect.intersection(screenRect);
   }

   private void vertex(VertexConsumer vertexConsumer, float x, float y, float u, float v, int color) {
      vertexConsumer.vertex(this.matrix, x, y).texture(u, v).color(color);
   }

   private void include(float x, float y) {
      this.minX = Math.min(this.minX, x);
      this.minY = Math.min(this.minY, y);
      this.maxX = Math.max(this.maxX, x);
      this.maxY = Math.max(this.maxY, y);
   }

   @Override
   public ScreenRect scissorArea() {
      return this.rect;
   }

   @Override
   public void setupVertices(VertexConsumer vertexConsumer) {
      for (FontVertexQuad fontVertexQuad : this.quads) {
         this.vertex(vertexConsumer, fontVertexQuad.x0(), fontVertexQuad.y0(), fontVertexQuad.u0(), fontVertexQuad.v0(), fontVertexQuad.colorTopLeft());
         this.vertex(vertexConsumer, fontVertexQuad.x1(), fontVertexQuad.y1(), fontVertexQuad.u0(), fontVertexQuad.v1(), fontVertexQuad.colorBottomLeft());
         this.vertex(
            vertexConsumer, fontVertexQuad.x2(), fontVertexQuad.y2(), fontVertexQuad.u1(), fontVertexQuad.v1(), fontVertexQuad.colorBottomRight()
         );
         this.vertex(
            vertexConsumer, fontVertexQuad.x3(), fontVertexQuad.y3(), fontVertexQuad.u1(), fontVertexQuad.v0(), fontVertexQuad.colorTopRight()
         );
      }
   }

   void add(
      List<RasterGlyphQuad> glyphs,
      float x,
      float y,
      int colorTopLeft,
      int colorTopRight,
      int colorBottomRight,
      int colorBottomLeft,
      float rotationDegrees,
      float rotationOriginX,
      float rotationOriginY,
      boolean fadeLeft,
      boolean fadeRight,
      float fadeLeftX,
      float fadeRightX,
      float fadeWidth,
      float fadeLeftStrength,
      float fadeRightStrength,
      boolean snapOrigin
   ) {
      boolean isAxisAligned = Math.abs(rotationDegrees) < 0.001F;
      if (isAxisAligned && snapOrigin) {
         x = PixelSnap.computeFloat2(x);
         y = PixelSnap.computeFloat2(y);
      }

      float rotationRadians = (float)Math.toRadians(rotationDegrees);
      float sinRotation = (float)Math.sin(rotationRadians);
      float cosRotation = (float)Math.cos(rotationRadians);

      for (RasterGlyphQuad rasterGlyphQuad : glyphs) {
         float topLeftX = rasterGlyphQuad.x0() + x;
         float topLeftY = rasterGlyphQuad.y0() + y;
         float bottomRightX = rasterGlyphQuad.x1() + x;
         float bottomRightY = rasterGlyphQuad.y1() + y;
         float[] floatArray = this.rotate(topLeftX, topLeftY, rotationOriginX, rotationOriginY, sinRotation, cosRotation);
         float[] floatArray2 = this.rotate(topLeftX, bottomRightY, rotationOriginX, rotationOriginY, sinRotation, cosRotation);
         float[] floatArray3 = this.rotate(bottomRightX, bottomRightY, rotationOriginX, rotationOriginY, sinRotation, cosRotation);
         float[] floatArray4 = this.rotate(bottomRightX, topLeftY, rotationOriginX, rotationOriginY, sinRotation, cosRotation);
         this.quads
            .add(
               new FontVertexQuad(
                  floatArray[0],
                  floatArray[1],
                  floatArray2[0],
                  floatArray2[1],
                  floatArray3[0],
                  floatArray3[1],
                  floatArray4[0],
                  floatArray4[1],
                  rasterGlyphQuad.u0(),
                  rasterGlyphQuad.v0(),
                  rasterGlyphQuad.u1(),
                  rasterGlyphQuad.v1(),
                  this.fadeColor(colorTopLeft, floatArray[0], fadeLeft, fadeRight, fadeLeftX, fadeRightX, fadeWidth, fadeLeftStrength, fadeRightStrength),
                  this.fadeColor(colorTopRight, floatArray4[0], fadeLeft, fadeRight, fadeLeftX, fadeRightX, fadeWidth, fadeLeftStrength, fadeRightStrength),
                  this.fadeColor(colorBottomRight, floatArray3[0], fadeLeft, fadeRight, fadeLeftX, fadeRightX, fadeWidth, fadeLeftStrength, fadeRightStrength),
                  this.fadeColor(colorBottomLeft, floatArray2[0], fadeLeft, fadeRight, fadeLeftX, fadeRightX, fadeWidth, fadeLeftStrength, fadeRightStrength)
               )
            );
         this.include(floatArray[0], floatArray[1]);
         this.include(floatArray2[0], floatArray2[1]);
         this.include(floatArray3[0], floatArray3[1]);
         this.include(floatArray4[0], floatArray4[1]);
      }
   }

   private float smoothstep(float progress) {
      return progress * progress * (3.0F - 2.0F * progress);
   }

   @Override
   public RenderPipeline pipeline() {
      return this.pipeline2;
   }

   RasterGlyphRenderState(Matrix3x2f matrix3x2f, FontTexture initialFontTexture, ScreenRect screenRect, RenderPipeline renderPipeline) {
      this.matrix = new Matrix3x2f(matrix3x2f);
      this.fontTexture = initialFontTexture;
      this.rect = screenRect;
      this.pipeline2 = renderPipeline;
   }

   private float clamp(float value, float min, float max) {
      return Math.max(min, Math.min(max, value));
   }

   private int fadeColor(
      int color,
      float x,
      boolean fadeLeft,
      boolean fadeRight,
      float fadeLeftX,
      float fadeRightX,
      float fadeWidth,
      float fadeLeftStrength,
      float fadeRightStrength
   ) {
      if ((fadeLeft || fadeRight) && !(fadeWidth <= 0.0F) && !(fadeRightX <= fadeLeftX) && (!(fadeLeftStrength <= 0.001F) || !(fadeRightStrength <= 0.001F))) {
         float alpha = 1.0F;
         if (fadeLeft) {
            float leftProgress = this.smoothstep(this.clamp((x - fadeLeftX) / fadeWidth, 0.0F, 1.0F));
            alpha = Math.min(alpha, 1.0F - this.clamp(fadeLeftStrength, 0.0F, 1.0F) * (1.0F - leftProgress));
         }

         if (fadeRight) {
            float rightProgress = this.smoothstep(this.clamp((fadeRightX - x) / fadeWidth, 0.0F, 1.0F));
            alpha = Math.min(alpha, 1.0F - this.clamp(fadeRightStrength, 0.0F, 1.0F) * (1.0F - rightProgress));
         }

         int alphaChannel = color >>> 24 & 0xFF;
         return Math.round(alphaChannel * alpha) << 24 | color & 16777215;
      } else {
         return color;
      }
   }

   @Override
   public TextureSetup textureSetup() {
      return this.fontTexture.getTextureSetup();
   }

   private float[] rotate(float x, float y, float originX, float originY, float sinRotation, float cosRotation) {
      float deltaX = x - originX;
      float deltaY = y - originY;
      return new float[]{
         originX + deltaX * cosRotation - deltaY * sinRotation, originY + deltaX * sinRotation + deltaY * cosRotation
      };
   }
}
