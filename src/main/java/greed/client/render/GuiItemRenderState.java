package greed.client.render;

import greed.client.util.GuiScale;
import greed.client.render.ItemGeometryQuad;
import greed.client.util.TextureBinding;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

final class GuiItemRenderState implements SimpleGuiElementRenderState {
   private final Matrix3x2f matrix;
   private final TextureBinding textureBinding;
   private float[] floatValArray = new float[256];
   private int[] intValArray = new int[16];
   private float[] floatValArray2 = new float[16];
   private ScreenRect rect;
   private int intVal;
   private float floatVal = Float.MAX_VALUE;
   private float floatVal2 = Float.MAX_VALUE;
   private float floatVal3 = -Float.MAX_VALUE;
   private float floatVal4 = -Float.MAX_VALUE;

   @Override
   public void setupVertices(VertexConsumer vertexConsumer) {
      for (int index = 0; index < this.intVal; index++) {
         int local4066_3 = ItemRenderer.getItemRenderer().computeInt7(this.floatValArray2[index]);
         if (local4066_3 >= 0) {
            int local4066_4 = index * 16;
            int local4066_5 = this.intValArray[index];
            this.vertex(
               vertexConsumer,
               this.floatValArray[local4066_4],
               this.floatValArray[local4066_4 + 1],
               this.floatValArray[local4066_4 + 2],
               this.floatValArray[local4066_4 + 3],
               local4066_5,
               local4066_3
            );
            this.vertex(
               vertexConsumer,
               this.floatValArray[local4066_4 + 4],
               this.floatValArray[local4066_4 + 5],
               this.floatValArray[local4066_4 + 6],
               this.floatValArray[local4066_4 + 7],
               local4066_5,
               local4066_3
            );
            this.vertex(
               vertexConsumer,
               this.floatValArray[local4066_4 + 8],
               this.floatValArray[local4066_4 + 9],
               this.floatValArray[local4066_4 + 10],
               this.floatValArray[local4066_4 + 11],
               local4066_5,
               local4066_3
            );
            this.vertex(
               vertexConsumer,
               this.floatValArray[local4066_4 + 12],
               this.floatValArray[local4066_4 + 13],
               this.floatValArray[local4066_4 + 14],
               this.floatValArray[local4066_4 + 15],
               local4066_5,
               local4066_3
            );
         }
      }
   }

   private void ensureCapacity(int local4067_1) {
      if (local4067_1 > this.intValArray.length) {
         int local4067_2 = Math.max(local4067_1, this.intValArray.length * 2);
         float[] floatArray = new float[local4067_2 * 16];
         int[] intArray = new int[local4067_2];
         float[] floatArray2 = new float[local4067_2];
         System.arraycopy(this.floatValArray, 0, floatArray, 0, this.intVal * 16);
         System.arraycopy(this.intValArray, 0, intArray, 0, this.intVal);
         System.arraycopy(this.floatValArray2, 0, floatArray2, 0, this.intVal);
         this.floatValArray = floatArray;
         this.intValArray = intArray;
         this.floatValArray2 = floatArray2;
      }
   }

   private void vertex(VertexConsumer vertexConsumer, float local4068_2, float local4068_3, float local4068_4, float local4068_5, int local4068_6, int local4068_7) {
      vertexConsumer.vertex(this.matrix, local4068_2, local4068_3).texture(local4068_4, local4068_5).color(local4068_6).lineWidth(local4068_7 + 1);
   }

   GuiItemRenderState(Matrix3x2f matrix3x2f, TextureBinding initialTextureBinding, ScreenRect screenRect) {
      this.matrix = new Matrix3x2f(matrix3x2f);
      this.textureBinding = initialTextureBinding;
      this.rect = screenRect;
   }

   void add(ItemGeometryQuad itemGeometryQuad, float local4070_2, float local4070_3, float local4070_4, int local4070_5, float local4070_6) {
      this.ensureCapacity(this.intVal + 1);
      int local4070_7 = this.intVal * 16;
      float local4070_8 = local4070_2 + itemGeometryQuad.x0() * local4070_4;
      float local4070_9 = local4070_3 + itemGeometryQuad.y0() * local4070_4;
      float local4070_10 = local4070_2 + itemGeometryQuad.x1() * local4070_4;
      float local4070_11 = local4070_3 + itemGeometryQuad.y1() * local4070_4;
      float local4070_12 = local4070_2 + itemGeometryQuad.x2() * local4070_4;
      float local4070_13 = local4070_3 + itemGeometryQuad.y2() * local4070_4;
      float local4070_14 = local4070_2 + itemGeometryQuad.x3() * local4070_4;
      float local4070_15 = local4070_3 + itemGeometryQuad.y3() * local4070_4;
      this.floatValArray[local4070_7] = local4070_8;
      this.floatValArray[local4070_7 + 1] = local4070_9;
      this.floatValArray[local4070_7 + 2] = itemGeometryQuad.u0();
      this.floatValArray[local4070_7 + 3] = itemGeometryQuad.v0();
      this.floatValArray[local4070_7 + 4] = local4070_10;
      this.floatValArray[local4070_7 + 5] = local4070_11;
      this.floatValArray[local4070_7 + 6] = itemGeometryQuad.u1();
      this.floatValArray[local4070_7 + 7] = itemGeometryQuad.v1();
      this.floatValArray[local4070_7 + 8] = local4070_12;
      this.floatValArray[local4070_7 + 9] = local4070_13;
      this.floatValArray[local4070_7 + 10] = itemGeometryQuad.u2();
      this.floatValArray[local4070_7 + 11] = itemGeometryQuad.v2();
      this.floatValArray[local4070_7 + 12] = local4070_14;
      this.floatValArray[local4070_7 + 13] = local4070_15;
      this.floatValArray[local4070_7 + 14] = itemGeometryQuad.u3();
      this.floatValArray[local4070_7 + 15] = itemGeometryQuad.v3();
      this.intValArray[this.intVal] = local4070_5;
      this.floatValArray2[this.intVal] = local4070_6;
      this.intVal++;
      this.include(local4070_8, local4070_9);
      this.include(local4070_10, local4070_11);
      this.include(local4070_12, local4070_13);
      this.include(local4070_14, local4070_15);
   }

   @Override
   public ScreenRect bounds() {
      if (this.intVal == 0) {
         return new ScreenRect(0, 0, 1, 1);
      }

      int local4071_1 = (int)Math.floor(this.floatVal);
      int local4071_2 = (int)Math.floor(this.floatVal2);
      int local4071_3 = Math.max(1, (int)Math.ceil(this.floatVal3 - this.floatVal));
      int local4071_4 = Math.max(1, (int)Math.ceil(this.floatVal4 - this.floatVal2));
      return new ScreenRect(local4071_1, local4071_2, local4071_3, local4071_4).transformEachVertex(this.matrix);
   }

   @Override
   public ScreenRect scissorArea() {
      return this.rect;
   }

   @Override
   public TextureSetup textureSetup() {
      return this.matrix.m00() * GuiScale.computeInt() < GuiScale.computeFloat()
         ? this.textureBinding.linearSetup()
         : this.textureBinding.setup();
   }

   @Override
   public RenderPipeline pipeline() {
      return ItemRenderer.pipeline2;
   }

   private void include(float local4075_1, float local4075_2) {
      this.floatVal = Math.min(this.floatVal, local4075_1);
      this.floatVal2 = Math.min(this.floatVal2, local4075_2);
      this.floatVal3 = Math.max(this.floatVal3, local4075_1);
      this.floatVal4 = Math.max(this.floatVal4, local4075_2);
   }
}
