package greed.client.render;

import greed.mixin.DrawContextMixin;
import greed.client.util.GuiScale;
import greed.client.render.ThemePaletteBuffer;
import greed.client.render.GradientSplitBuffer;
import greed.client.render.BlurDrawBatch;
import greed.client.render.ShapeDrawData;
import greed.client.render.ScissorStack;
import greed.client.render.BlurDrawData;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;
import org.lwjgl.system.MemoryStack;

public final class ShapeRenderer implements AutoCloseable {
   static final int intVal = 16;
   static final int intVal2 = 64;
   static final int intVal3 = 73;
   private static volatile ShapeRenderer shapeRenderer;
   public static final RenderPipeline pipeline2 = RenderPipeline.builder()
      .withLocation(getIdentifier("pipeline/shape"))
      .withVertexShader(getIdentifier("ui/shape/shape"))
      .withFragmentShader(getIdentifier("ui/shape/shape"))
      .withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("ShapeParamsArray", UniformType.UNIFORM_BUFFER)
      .withUniform("PaletteParams", UniformType.UNIFORM_BUFFER)
      .withUniform("SplitParams", UniformType.UNIFORM_BUFFER)
      .build();
   private final List<ShapeDrawData> list = new ArrayList<>(32);
   private final List<BlurDrawBatch> list2 = new ArrayList<>(32);
   private DrawContext drawContext2;
   private GpuBuffer gpuBuffer4;
   private boolean flag = true;

   private GpuBuffer getGpuBuffer() {
      if (this.gpuBuffer4 != null && !this.gpuBuffer4.isClosed() && this.gpuBuffer4.size() >= 18688L) {
         return this.gpuBuffer4;
      }

      this.updateState4();

      try {
         this.gpuBuffer4 = RenderSystem.getDevice().createBuffer(() -> "greed_shape_params", 136, 18688L);
         return this.gpuBuffer4;
      } catch (RuntimeException exception) {
         return null;
      }
   }

   private static float computeFloat(float local6471_0, float local6471_1, float local6471_2) {
      return Math.max(local6471_1, Math.min(local6471_2, local6471_0));
   }

   public void updateState() {
      if (!this.list.isEmpty() && this.flag) {
         GpuBuffer gpuBuffer = this.getGpuBuffer();
         if (gpuBuffer != null) {
            try {
               MemoryStack memoryStack = MemoryStack.stackPush();

               try {
                  ByteBuffer byteBuffer = this.getByteBuffer(memoryStack, this.list);
                  RenderSystem.getDevice().createCommandEncoder().writeToBuffer(gpuBuffer.slice(0L, byteBuffer.remaining()), byteBuffer);
                  this.flag = false;
               } catch (Throwable throwable) {
                  if (memoryStack != null) {
                     try {
                        memoryStack.close();
                     } catch (Throwable throwable2) {
                        throwable.addSuppressed(throwable2);
                     }
                  }

                  throw throwable;
               }

               if (memoryStack != null) {
                  memoryStack.close();
               }
            } catch (RuntimeException exception) {
               this.flag = true;
            }
         }
      }
   }

   public void renderElement(DrawContext drawContext, ShapeDrawData shapeDrawData) {
      this.renderElement3(drawContext);
      this.processData3(shapeDrawData);
      this.updateState5();
   }

   private void renderElement2(DrawContext drawContext, ShapeDrawData shapeDrawData) {
      if (drawContext != null && shapeDrawData != null && shapeDrawData.visible()) {
         try {
            ShapeDrawData shapeDrawData2 = this.getShapeDrawData(shapeDrawData);
            Matrix3x2f matrix3x2f = GuiScale.renderElement(drawContext);
            BlurDrawBatch blurDrawBatch = new BlurDrawBatch();
            BatchedBlurRenderer.getBatchedBlurRenderer()
               .renderElement4(
                  drawContext,
                  new BlurDrawData(
                     shapeDrawData2.x(),
                     shapeDrawData2.y(),
                     shapeDrawData2.width(),
                     shapeDrawData2.height(),
                     shapeDrawData2.radiusTopLeft(),
                     shapeDrawData2.radiusTopRight(),
                     shapeDrawData2.radiusBottomRight(),
                     shapeDrawData2.radiusBottomLeft(),
                     1.0F,
                     shapeDrawData2.blurRadius(),
                     -1
                  ),
                  blurDrawBatch
               );
            ((DrawContextMixin)drawContext)
               .getGuiRenderState()
               .addSimpleElement(new ShapeRenderState(matrix3x2f, shapeDrawData2, ScissorStack.getScreenRect(), blurDrawBatch));
         } catch (RuntimeException exception) {
         }
      }
   }

   public static ShapeRenderer getShapeRenderer() {
      ShapeRenderer currentShapeRenderer = shapeRenderer;
      if (currentShapeRenderer == null) {
         synchronized (ShapeRenderer.class) {
            currentShapeRenderer = shapeRenderer;
            if (currentShapeRenderer == null) {
               currentShapeRenderer = new ShapeRenderer();
               shapeRenderer = currentShapeRenderer;
            }
         }
      }

      return currentShapeRenderer;
   }

   public void updateState2() {
      this.list.clear();
      this.list2.clear();
      this.flag = false;
   }

   public static void updateState3() {
      ShapeRenderer currentShapeRenderer = shapeRenderer;
      if (currentShapeRenderer != null) {
         currentShapeRenderer.close();
         shapeRenderer = null;
      }
   }

   private ShapeRenderer() {
   }

   private static void processData(ByteBuffer byteBuffer, int local6480_1, int local6480_2) {
      byteBuffer.putFloat(local6480_1, (local6480_2 >>> 16 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local6480_1 + 4, (local6480_2 >>> 8 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local6480_1 + 8, (local6480_2 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local6480_1 + 12, (local6480_2 >>> 24 & 0xFF) / 255.0F);
   }

   private ByteBuffer getByteBuffer(MemoryStack memoryStack, List<ShapeDrawData> local6481_2) {
      int local6481_3 = Math.max(1, local6481_2.size()) * 73 * 4 * 4;
      ByteBuffer byteBuffer = memoryStack.calloc(local6481_3);

      for (int index = 0; index < local6481_2.size(); index++) {
         ShapeDrawData shapeDrawData = local6481_2.get(index);
         int local6481_7 = index * 73 * 4 * 4;
         byteBuffer.putFloat(local6481_7, shapeDrawData.radiusTopLeft());
         byteBuffer.putFloat(local6481_7 + 4, shapeDrawData.radiusTopRight());
         byteBuffer.putFloat(local6481_7 + 8, shapeDrawData.radiusBottomRight());
         byteBuffer.putFloat(local6481_7 + 12, shapeDrawData.radiusBottomLeft());
         byteBuffer.putFloat(local6481_7 + 16, shapeDrawData.width());
         byteBuffer.putFloat(local6481_7 + 20, shapeDrawData.height());
         byteBuffer.putFloat(local6481_7 + 24, computeFloat2(shapeDrawData));
         byteBuffer.putFloat(local6481_7 + 28, Math.max(shapeDrawData.squirt(), 0.001F));
         byteBuffer.putFloat(local6481_7 + 32, shapeDrawData.globalAlpha());
         byteBuffer.putFloat(local6481_7 + 36, shapeDrawData.fresnelPower());
         byteBuffer.putFloat(local6481_7 + 40, shapeDrawData.baseAlpha());
         byteBuffer.putFloat(local6481_7 + 44, shapeDrawData.fresnelMix());
         processData(byteBuffer, local6481_7 + 48, shapeDrawData.fresnelColor());
         byteBuffer.putFloat(local6481_7 + 48, shapeDrawData.splitIndex());
         byteBuffer.putFloat(local6481_7 + 64, shapeDrawData.fresnelInvert() ? 1.0F : 0.0F);
         byteBuffer.putFloat(local6481_7 + 68, shapeDrawData.distortStrength());
         byteBuffer.putFloat(local6481_7 + 72, shapeDrawData.z());
         byteBuffer.putFloat(local6481_7 + 76, shapeDrawData.colorOffset());
         processData(byteBuffer, local6481_7 + 80, shapeDrawData.color());
         processData(byteBuffer, local6481_7 + 96, shapeDrawData.secondColor());
         BlurDrawBatch blurDrawBatch = index < this.list2.size() ? this.list2.get(index) : null;
         if (blurDrawBatch != null) {
            byteBuffer.putFloat(local6481_7 + 112, blurDrawBatch.floatVal);
            byteBuffer.putFloat(local6481_7 + 116, blurDrawBatch.floatVal2);
            byteBuffer.putFloat(local6481_7 + 120, blurDrawBatch.floatVal3);
            byteBuffer.putFloat(local6481_7 + 124, blurDrawBatch.floatVal4);
         }

         int local6481_9 = shapeDrawData.spanCount();
         byteBuffer.putFloat(local6481_7 + 128, local6481_9);
         byteBuffer.putFloat(local6481_7 + 132, shapeDrawData.innerRadius());
         byteBuffer.putFloat(local6481_7 + 136, shapeDrawData.leftAligned());
         byteBuffer.putFloat(local6481_7 + 140, shapeDrawData.bottomAnchored());
         float[] floatArray = shapeDrawData.spans();
         int local6481_11 = Math.min(local6481_9, 64);

         for (int index2 = 0; index2 < local6481_11; index2++) {
            int local6481_13 = index2 * 4;
            byteBuffer.putFloat(local6481_7 + 144 + index2 * 16, floatArray[local6481_13]);
            byteBuffer.putFloat(local6481_7 + 144 + index2 * 16 + 4, floatArray[local6481_13 + 1]);
            byteBuffer.putFloat(local6481_7 + 144 + index2 * 16 + 8, floatArray[local6481_13 + 2]);
            byteBuffer.putFloat(local6481_7 + 144 + index2 * 16 + 12, floatArray[local6481_13 + 3]);
         }
      }

      byteBuffer.position(0);
      return byteBuffer;
   }

   @Override
   public void close() {
      this.list.clear();
      this.drawContext2 = null;
      this.updateState4();
   }

   private void updateState4() {
      if (this.gpuBuffer4 != null) {
         this.gpuBuffer4.close();
         this.gpuBuffer4 = null;
      }
   }

   public boolean matchesCriteria(RenderPipeline renderPipeline) {
      return renderPipeline == pipeline2;
   }

   private GpuBuffer getGpuBuffer2() {
      if (!this.flag && this.gpuBuffer4 != null) {
         return this.gpuBuffer4;
      }

      this.updateState();
      if (!this.flag && this.gpuBuffer4 != null) {
         return this.gpuBuffer4;
      }

      this.updateState4();
      MemoryStack memoryStack = MemoryStack.stackPush();

      GpuBuffer gpuBuffer;
      try {
         ByteBuffer byteBuffer = this.getByteBuffer(memoryStack, this.list);
         this.gpuBuffer4 = RenderSystem.getDevice().createBuffer(() -> "greed_shape_params", 128, byteBuffer);
         this.flag = false;
         gpuBuffer = this.gpuBuffer4;
      } catch (Throwable throwable) {
         if (memoryStack != null) {
            try {
               memoryStack.close();
            } catch (Throwable throwable2) {
               throwable.addSuppressed(throwable2);
            }
         }

         throw throwable;
      }

      if (memoryStack != null) {
         memoryStack.close();
      }

      return gpuBuffer;
   }

   private static float computeFloat2(ShapeDrawData shapeDrawData) {
      int local6486_1 = Math.min(shapeDrawData.spanCount(), 64);
      float[] floatArray = shapeDrawData.spans();
      if (local6486_1 > 0 && floatArray != null) {
         float local6486_3 = Float.MAX_VALUE;

         for (int index = 0; index < local6486_1; index++) {
            float local6486_5 = floatArray[index * 4 + 2];
            float local6486_6 = floatArray[index * 4 + 3];
            float local6486_7 = local6486_6 - local6486_5;
            if (local6486_7 > 0.001F) {
               local6486_3 = Math.min(local6486_3, local6486_7);
            }
         }

         return local6486_3 == Float.MAX_VALUE ? 0.0F : Math.max(local6486_3 * 0.5F, 1.5F);
      } else {
         return 0.0F;
      }
   }

   public void processData2(RenderPass renderPass) {
      if (renderPass != null && !this.list.isEmpty()) {
         GpuBuffer gpuBuffer = this.getGpuBuffer2();
         if (gpuBuffer != null) {
            renderPass.setUniform("ShapeParamsArray", gpuBuffer);
         }

         GpuBuffer gpuBuffer2 = ThemePaletteBuffer.getGpuBuffer2();
         if (gpuBuffer2 != null) {
            renderPass.setUniform("PaletteParams", gpuBuffer2);
         }

         GpuBuffer gpuBuffer3 = GradientSplitBuffer.getGpuBuffer();
         if (gpuBuffer3 != null) {
            renderPass.setUniform("SplitParams", gpuBuffer3);
         }
      }
   }

   int computeInt(ShapeDrawData shapeDrawData, BlurDrawBatch blurDrawBatch) {
      int local6488_3 = this.list.size();
      if (local6488_3 == 16) {
         return -1;
      }

      this.list.add(shapeDrawData);
      this.list2.add(blurDrawBatch);
      this.flag = true;
      return local6488_3;
   }

   private static Identifier getIdentifier(String local6490_0) {
      return Identifier.of("greed", local6490_0);
   }

   private ShapeDrawData getShapeDrawData(ShapeDrawData shapeDrawData) {
      float local6491_2 = Math.max(0.0F, Math.min(shapeDrawData.width(), shapeDrawData.height()) * 0.5F);
      float local6491_3 = computeFloat(shapeDrawData.radiusTopLeft(), 0.0F, local6491_2);
      float local6491_4 = computeFloat(shapeDrawData.radiusTopRight(), 0.0F, local6491_2);
      float local6491_5 = computeFloat(shapeDrawData.radiusBottomRight(), 0.0F, local6491_2);
      float local6491_6 = computeFloat(shapeDrawData.radiusBottomLeft(), 0.0F, local6491_2);
      float local6491_7 = computeFloat(shapeDrawData.globalAlpha(), 0.0F, 1.0F);
      float local6491_8 = Math.max(shapeDrawData.fresnelPower(), 0.001F);
      float local6491_9 = computeFloat(shapeDrawData.baseAlpha(), 0.0F, 1.0F);
      float local6491_10 = computeFloat(shapeDrawData.fresnelMix(), 0.0F, 1.0F);
      float local6491_11 = Math.max(shapeDrawData.squirt(), 0.001F);
      float local6491_12 = Float.isFinite(shapeDrawData.blurRadius()) ? computeFloat(shapeDrawData.blurRadius(), 0.1F, 64.0F) : 0.1F;
      float local6491_13 = Float.isFinite(shapeDrawData.colorOffset()) ? shapeDrawData.colorOffset() : 0.0F;
      local6491_13 -= (float)Math.floor(local6491_13);
      return local6491_3 == shapeDrawData.radiusTopLeft()
            && local6491_4 == shapeDrawData.radiusTopRight()
            && local6491_5 == shapeDrawData.radiusBottomRight()
            && local6491_6 == shapeDrawData.radiusBottomLeft()
            && local6491_7 == shapeDrawData.globalAlpha()
            && local6491_8 == shapeDrawData.fresnelPower()
            && local6491_9 == shapeDrawData.baseAlpha()
            && local6491_10 == shapeDrawData.fresnelMix()
            && local6491_11 == shapeDrawData.squirt()
            && local6491_12 == shapeDrawData.blurRadius()
            && local6491_13 == shapeDrawData.colorOffset()
         ? shapeDrawData
         : new ShapeDrawData(
            shapeDrawData.x(),
            shapeDrawData.y(),
            shapeDrawData.width(),
            shapeDrawData.height(),
            shapeDrawData.spans(),
            shapeDrawData.spanCount(),
            shapeDrawData.innerRadius(),
            local6491_3,
            local6491_4,
            local6491_5,
            local6491_6,
            shapeDrawData.color(),
            local6491_7,
            local6491_8,
            shapeDrawData.fresnelColor(),
            local6491_9,
            shapeDrawData.fresnelInvert(),
            local6491_10,
            shapeDrawData.distortStrength(),
            local6491_11,
            shapeDrawData.z(),
            local6491_12,
            shapeDrawData.secondColor(),
            local6491_13,
            shapeDrawData.leftAligned(),
            shapeDrawData.bottomAnchored(),
            shapeDrawData.splitIndex()
         );
   }

   public void renderElement3(DrawContext drawContext) {
      this.drawContext2 = drawContext;
   }

   public void updateState5() {
      this.drawContext2 = null;
   }

   public void processData3(ShapeDrawData shapeDrawData) {
      this.renderElement2(this.drawContext2, shapeDrawData);
   }
}
