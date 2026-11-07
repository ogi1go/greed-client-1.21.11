package greed.client.render;

import greed.client.render.IconDrawData;
import greed.client.render.TextureReference;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

final class IconRenderState implements SimpleGuiElementRenderState {
   private final Matrix3x2f matrix;
   private final TextureReference textureReference;
   private final List<IconDrawData> list = new ArrayList<>(16);
   private float floatVal = Float.MAX_VALUE;
   private float floatVal2 = Float.MAX_VALUE;
   private float floatVal3 = -Float.MAX_VALUE;
   private float floatVal4 = -Float.MAX_VALUE;

   @Override
   public void setupVertices(VertexConsumer vertexConsumer) {
      for (IconDrawData iconDrawData : this.list) {
         float local7343_4 = iconDrawData.x();
         float local7343_5 = iconDrawData.y();
         float local7343_6 = iconDrawData.x() + iconDrawData.size();
         float local7343_7 = iconDrawData.y() + iconDrawData.size();
         int local7343_8 = iconDrawData.color();
         this.vertex(vertexConsumer, local7343_4, local7343_5, iconDrawData.u0(), iconDrawData.v0(), local7343_8);
         this.vertex(vertexConsumer, local7343_4, local7343_7, iconDrawData.u0(), iconDrawData.v1(), local7343_8);
         this.vertex(vertexConsumer, local7343_6, local7343_7, iconDrawData.u1(), iconDrawData.v1(), local7343_8);
         this.vertex(vertexConsumer, local7343_6, local7343_5, iconDrawData.u1(), iconDrawData.v0(), local7343_8);
      }
   }

   @Override
   public ScreenRect scissorArea() {
      return null;
   }

   private void vertex(VertexConsumer vertexConsumer, float local7345_2, float local7345_3, float local7345_4, float local7345_5, int local7345_6) {
      vertexConsumer.vertex(this.matrix, local7345_2, local7345_3).texture(local7345_4, local7345_5).color(local7345_6);
   }

   @Override
   public RenderPipeline pipeline() {
      return EffectIconRenderer.pipeline2;
   }

   void add(IconDrawData iconDrawData) {
      this.list.add(iconDrawData);
      this.floatVal = Math.min(this.floatVal, iconDrawData.x());
      this.floatVal2 = Math.min(this.floatVal2, iconDrawData.y());
      this.floatVal3 = Math.max(this.floatVal3, iconDrawData.x() + iconDrawData.size());
      this.floatVal4 = Math.max(this.floatVal4, iconDrawData.y() + iconDrawData.size());
   }

   @Override
   public TextureSetup textureSetup() {
      return this.textureReference.setup();
   }

   @Override
   public ScreenRect bounds() {
      if (this.list.isEmpty()) {
         return new ScreenRect(0, 0, 1, 1);
      }

      int local7349_1 = (int)Math.floor(this.floatVal);
      int local7349_2 = (int)Math.floor(this.floatVal2);
      int local7349_3 = Math.max(1, (int)Math.ceil(this.floatVal3 - this.floatVal));
      int local7349_4 = Math.max(1, (int)Math.ceil(this.floatVal4 - this.floatVal2));
      return new ScreenRect(local7349_1, local7349_2, local7349_3, local7349_4).transformEachVertex(this.matrix);
   }

   IconRenderState(Matrix3x2f matrix3x2f, TextureReference initialTextureReference) {
      this.matrix = new Matrix3x2f(matrix3x2f);
      this.textureReference = initialTextureReference;
   }
}
