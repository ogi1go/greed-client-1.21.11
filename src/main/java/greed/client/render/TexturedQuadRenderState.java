package greed.client.render;

import greed.client.render.CachedTexture;
import greed.client.render.TexturedQuadDrawData;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

final class TexturedQuadRenderState implements SimpleGuiElementRenderState {
   private final Matrix3x2f matrix;
   private final CachedTexture cachedTexture;
   private final ScreenRect rect;
   private final boolean flag;
   private final boolean flag2;
   private final List<TexturedQuadDrawData> list = new ArrayList<>(16);
   private float floatVal = Float.MAX_VALUE;
   private float floatVal2 = Float.MAX_VALUE;
   private float floatVal3 = -Float.MAX_VALUE;
   private float floatVal4 = -Float.MAX_VALUE;

   @Override
   public ScreenRect bounds() {
      if (this.list.isEmpty()) {
         return new ScreenRect(0, 0, 1, 1);
      }

      int local7473_1 = (int)Math.floor(this.floatVal);
      int local7473_2 = (int)Math.floor(this.floatVal2);
      int local7473_3 = Math.max(1, (int)Math.ceil(this.floatVal3 - this.floatVal));
      int local7473_4 = Math.max(1, (int)Math.ceil(this.floatVal4 - this.floatVal2));
      ScreenRect screenRect = new ScreenRect(local7473_1, local7473_2, local7473_3, local7473_4).transformEachVertex(this.matrix);
      return this.rect == null ? screenRect : this.rect.intersection(screenRect);
   }

   @Override
   public TextureSetup textureSetup() {
      return this.cachedTexture.setup(this.flag);
   }

   private void includeBounds(TexturedQuadDrawData texturedQuadDrawData) {
      float local7475_2 = texturedQuadDrawData.x();
      float local7475_3 = texturedQuadDrawData.y();
      float local7475_4 = texturedQuadDrawData.x() + texturedQuadDrawData.width();
      float local7475_5 = texturedQuadDrawData.y() + texturedQuadDrawData.height();
      this.include(rotatedX(texturedQuadDrawData, local7475_2, local7475_3), rotatedY(texturedQuadDrawData, local7475_2, local7475_3));
      this.include(rotatedX(texturedQuadDrawData, local7475_2, local7475_5), rotatedY(texturedQuadDrawData, local7475_2, local7475_5));
      this.include(rotatedX(texturedQuadDrawData, local7475_4, local7475_5), rotatedY(texturedQuadDrawData, local7475_4, local7475_5));
      this.include(rotatedX(texturedQuadDrawData, local7475_4, local7475_3), rotatedY(texturedQuadDrawData, local7475_4, local7475_3));
   }

   private void include(float local7476_1, float local7476_2) {
      this.floatVal = Math.min(this.floatVal, local7476_1);
      this.floatVal2 = Math.min(this.floatVal2, local7476_2);
      this.floatVal3 = Math.max(this.floatVal3, local7476_1);
      this.floatVal4 = Math.max(this.floatVal4, local7476_2);
   }

   @Override
   public void setupVertices(VertexConsumer vertexConsumer) {
      for (TexturedQuadDrawData texturedQuadDrawData : this.list) {
         int local7477_4 = ImageRenderer.getImageRenderer().computeInt3(texturedQuadDrawData);
         if (local7477_4 >= 0) {
            float local7477_5 = texturedQuadDrawData.x();
            float local7477_6 = texturedQuadDrawData.y();
            float local7477_7 = texturedQuadDrawData.x() + texturedQuadDrawData.width();
            float local7477_8 = texturedQuadDrawData.y() + texturedQuadDrawData.height();
            this.vertex(vertexConsumer, texturedQuadDrawData, local7477_5, local7477_6, texturedQuadDrawData.u0(), texturedQuadDrawData.v0(), 0, 0, local7477_4);
            this.vertex(vertexConsumer, texturedQuadDrawData, local7477_5, local7477_8, texturedQuadDrawData.u0(), texturedQuadDrawData.v1(), 0, 255, local7477_4);
            this.vertex(vertexConsumer, texturedQuadDrawData, local7477_7, local7477_8, texturedQuadDrawData.u1(), texturedQuadDrawData.v1(), 255, 255, local7477_4);
            this.vertex(vertexConsumer, texturedQuadDrawData, local7477_7, local7477_6, texturedQuadDrawData.u1(), texturedQuadDrawData.v0(), 255, 0, local7477_4);
         }
      }
   }

   TexturedQuadRenderState(Matrix3x2f matrix3x2f, CachedTexture initialCachedTexture, ScreenRect screenRect, boolean local7478_4, boolean local7478_5) {
      this.matrix = new Matrix3x2f(matrix3x2f);
      this.cachedTexture = initialCachedTexture;
      this.rect = screenRect;
      this.flag = local7478_4;
      this.flag2 = local7478_5;
   }

   private static float rotatedY(TexturedQuadDrawData texturedQuadDrawData, float local7479_1, float local7479_2) {
      float local7479_3 = texturedQuadDrawData.rotationDegrees();
      if (local7479_3 != 0.0F && Float.isFinite(local7479_3)) {
         double local7479_4 = Math.toRadians(local7479_3);
         float local7479_6 = local7479_1 - texturedQuadDrawData.rotationOriginX();
         float local7479_7 = local7479_2 - texturedQuadDrawData.rotationOriginY();
         return texturedQuadDrawData.rotationOriginY() + (float)(local7479_6 * Math.sin(local7479_4) + local7479_7 * Math.cos(local7479_4));
      } else {
         return local7479_2;
      }
   }

   void add(TexturedQuadDrawData texturedQuadDrawData) {
      this.list.add(texturedQuadDrawData);
      this.includeBounds(texturedQuadDrawData);
   }

   @Override
   public RenderPipeline pipeline() {
      return this.flag2 ? ImageRenderer.pipeline3 : ImageRenderer.pipeline2;
   }

   private static float rotatedX(TexturedQuadDrawData texturedQuadDrawData, float local7482_1, float local7482_2) {
      float local7482_3 = texturedQuadDrawData.rotationDegrees();
      if (local7482_3 != 0.0F && Float.isFinite(local7482_3)) {
         double local7482_4 = Math.toRadians(local7482_3);
         float local7482_6 = local7482_1 - texturedQuadDrawData.rotationOriginX();
         float local7482_7 = local7482_2 - texturedQuadDrawData.rotationOriginY();
         return texturedQuadDrawData.rotationOriginX() + (float)(local7482_6 * Math.cos(local7482_4) - local7482_7 * Math.sin(local7482_4));
      } else {
         return local7482_1;
      }
   }

   @Override
   public ScreenRect scissorArea() {
      return this.rect;
   }

   private void vertex(
      VertexConsumer vertexConsumer,
      TexturedQuadDrawData texturedQuadDrawData,
      float local7484_3,
      float local7484_4,
      float local7484_5,
      float local7484_6,
      int local7484_7,
      int local7484_8,
      int local7484_9
   ) {
      float local7484_10 = rotatedX(texturedQuadDrawData, local7484_3, local7484_4);
      float local7484_11 = rotatedY(texturedQuadDrawData, local7484_3, local7484_4);
      vertexConsumer.vertex(this.matrix, local7484_10, local7484_11)
         .texture(local7484_5, local7484_6)
         .color(local7484_7, local7484_8, 255, 255)
         .lineWidth(local7484_9 + 1);
   }
}
