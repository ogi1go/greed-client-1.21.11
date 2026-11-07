package greed.client.render;

import greed.client.render.MenuGridDrawData;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

final class MenuGridRenderState implements SimpleGuiElementRenderState {
   private final Matrix3x2f matrix;
   private final ScreenRect rect;
   private final List<MenuGridDrawData> list = new ArrayList<>(2);
   private float floatVal = Float.MAX_VALUE;
   private float floatVal2 = Float.MAX_VALUE;
   private float floatVal3 = -Float.MAX_VALUE;
   private float floatVal4 = -Float.MAX_VALUE;

   @Override
   public void setupVertices(VertexConsumer vertexConsumer) {
      for (MenuGridDrawData menuGridDrawData : this.list) {
         int local5304_4 = MenuGridRenderer.getMenuGridRenderer().computeInt(menuGridDrawData);
         if (local5304_4 >= 0) {
            this.vertex(vertexConsumer, menuGridDrawData.x(), menuGridDrawData.y(), 0.0F, 0.0F, menuGridDrawData.color(), local5304_4);
            this.vertex(vertexConsumer, menuGridDrawData.x(), menuGridDrawData.y() + menuGridDrawData.height(), 0.0F, 1.0F, menuGridDrawData.color(), local5304_4);
            this.vertex(
               vertexConsumer, menuGridDrawData.x() + menuGridDrawData.width(), menuGridDrawData.y() + menuGridDrawData.height(), 1.0F, 1.0F, menuGridDrawData.color(), local5304_4
            );
            this.vertex(vertexConsumer, menuGridDrawData.x() + menuGridDrawData.width(), menuGridDrawData.y(), 1.0F, 0.0F, menuGridDrawData.color(), local5304_4);
         }
      }
   }

   MenuGridRenderState(Matrix3x2f matrix3x2f, ScreenRect screenRect) {
      this.matrix = new Matrix3x2f(matrix3x2f);
      this.rect = screenRect;
   }

   @Override
   public TextureSetup textureSetup() {
      return TextureSetup.empty();
   }

   private void vertex(VertexConsumer vertexConsumer, float local5307_2, float local5307_3, float local5307_4, float local5307_5, int local5307_6, int local5307_7) {
      vertexConsumer.vertex(this.matrix, local5307_2, local5307_3).texture(local5307_4, local5307_5).color(local5307_6).lineWidth(local5307_7 + 1);
   }

   @Override
   public ScreenRect bounds() {
      if (this.rect != null) {
         return this.rect;
      } else {
         return this.list.isEmpty()
            ? new ScreenRect(0, 0, 1, 1)
            : new ScreenRect(
                  (int)Math.floor(this.floatVal),
                  (int)Math.floor(this.floatVal2),
                  Math.max(1, (int)Math.ceil(this.floatVal3 - this.floatVal)),
                  Math.max(1, (int)Math.ceil(this.floatVal4 - this.floatVal2))
               )
               .transformEachVertex(this.matrix);
      }
   }

   @Override
   public ScreenRect scissorArea() {
      return this.rect;
   }

   @Override
   public RenderPipeline pipeline() {
      return MenuGridRenderer.pipeline2;
   }

   void add(MenuGridDrawData menuGridDrawData) {
      this.list.add(menuGridDrawData);
      this.floatVal = Math.min(this.floatVal, menuGridDrawData.x());
      this.floatVal2 = Math.min(this.floatVal2, menuGridDrawData.y());
      this.floatVal3 = Math.max(this.floatVal3, menuGridDrawData.x() + menuGridDrawData.width());
      this.floatVal4 = Math.max(this.floatVal4, menuGridDrawData.y() + menuGridDrawData.height());
   }
}
