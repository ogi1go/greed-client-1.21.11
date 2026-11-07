package greed.client.render;

import greed.mixin.DrawContextMixin;
import greed.client.util.GuiScale;
import greed.client.render.ThemePaletteBuffer;
import greed.client.render.GradientSplitBuffer;
import greed.client.render.BlurDrawBatch;
import greed.client.render.GlassDrawData;
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

public final class GlassRenderer implements AutoCloseable {
   private static volatile GlassRenderer glassRenderer;
   public static final RenderPipeline pipeline2 = RenderPipeline.builder()
      .withLocation(getIdentifier("pipeline/glass"))
      .withVertexShader(getIdentifier("ui/glass/glass"))
      .withFragmentShader(getIdentifier("ui/glass/glass"))
      .withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("GlassParamsArray", UniformType.UNIFORM_BUFFER)
      .withUniform("PaletteParams", UniformType.UNIFORM_BUFFER)
      .withUniform("SplitParams", UniformType.UNIFORM_BUFFER)
      .build();
   private final List<GlassDrawData> list = new ArrayList<>(64);
   private final List<BlurDrawBatch> list2 = new ArrayList<>(64);
   private DrawContext drawContext2;
   private GpuBuffer gpuBuffer4;
   private boolean flag = true;

   private GpuBuffer getGpuBuffer() {
      if (!this.flag && this.gpuBuffer4 != null) {
         return this.gpuBuffer4;
      }

      this.updateState3();
      if (!this.flag && this.gpuBuffer4 != null) {
         return this.gpuBuffer4;
      }

      this.updateState();
      MemoryStack memoryStack = MemoryStack.stackPush();

      GpuBuffer gpuBuffer;
      try {
         ByteBuffer byteBuffer = this.getByteBuffer(memoryStack, this.list);
         this.gpuBuffer4 = RenderSystem.getDevice().createBuffer(() -> "greed_glass_params", 128, byteBuffer);
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

   private GpuBuffer getGpuBuffer2() {
      if (this.gpuBuffer4 != null && !this.gpuBuffer4.isClosed() && this.gpuBuffer4.size() >= 57344L) {
         return this.gpuBuffer4;
      }

      this.updateState();

      try {
         this.gpuBuffer4 = RenderSystem.getDevice().createBuffer(() -> "greed_glass_params", 136, 57344L);
         return this.gpuBuffer4;
      } catch (RuntimeException exception) {
         return null;
      }
   }

   int computeInt(GlassDrawData glassDrawData, BlurDrawBatch blurDrawBatch) {
      int local4207_3 = this.list.size();
      if (local4207_3 == 448) {
         return -1;
      }

      this.list.add(glassDrawData);
      this.list2.add(blurDrawBatch);
      this.flag = true;
      return local4207_3;
   }

   private static Identifier getIdentifier(String local4208_0) {
      return Identifier.of("greed", local4208_0);
   }

   @Override
   public void close() {
      this.list.clear();
      this.drawContext2 = null;
      this.updateState();
   }

   private ByteBuffer getByteBuffer(MemoryStack memoryStack, List<GlassDrawData> local4210_2) {
      int local4210_3 = Math.max(1, local4210_2.size()) * 8 * 4 * 4;
      ByteBuffer byteBuffer = memoryStack.calloc(local4210_3);

      for (int index = 0; index < local4210_2.size(); index++) {
         GlassDrawData glassDrawData = local4210_2.get(index);
         int local4210_7 = index * 8 * 4 * 4;
         byteBuffer.putFloat(local4210_7, glassDrawData.radiusTopLeft());
         byteBuffer.putFloat(local4210_7 + 4, glassDrawData.radiusTopRight());
         byteBuffer.putFloat(local4210_7 + 8, glassDrawData.radiusBottomRight());
         byteBuffer.putFloat(local4210_7 + 12, glassDrawData.radiusBottomLeft());
         byteBuffer.putFloat(local4210_7 + 16, glassDrawData.width());
         byteBuffer.putFloat(local4210_7 + 20, glassDrawData.height());
         byteBuffer.putFloat(local4210_7 + 24, glassDrawData.splitIndex());
         byteBuffer.putFloat(local4210_7 + 28, Math.max(glassDrawData.squirt(), 0.001F));
         byteBuffer.putFloat(local4210_7 + 32, glassDrawData.globalAlpha());
         byteBuffer.putFloat(local4210_7 + 36, glassDrawData.fresnelPower());
         byteBuffer.putFloat(local4210_7 + 40, glassDrawData.baseAlpha());
         byteBuffer.putFloat(local4210_7 + 44, glassDrawData.fresnelMix());
         processData3(byteBuffer, local4210_7 + 48, glassDrawData.fresnelColor());
         byteBuffer.putFloat(local4210_7 + 64, glassDrawData.fresnelInvert() ? 1.0F : 0.0F);
         byteBuffer.putFloat(local4210_7 + 68, glassDrawData.distortStrength());
         byteBuffer.putFloat(local4210_7 + 72, glassDrawData.z());
         byteBuffer.putFloat(local4210_7 + 76, glassDrawData.colorOffset());
         processData3(byteBuffer, local4210_7 + 80, glassDrawData.color());
         processData3(byteBuffer, local4210_7 + 96, glassDrawData.secondColor());
         BlurDrawBatch blurDrawBatch = index < this.list2.size() ? this.list2.get(index) : null;
         if (blurDrawBatch != null) {
            byteBuffer.putFloat(local4210_7 + 112, blurDrawBatch.floatVal);
            byteBuffer.putFloat(local4210_7 + 116, blurDrawBatch.floatVal2);
            byteBuffer.putFloat(local4210_7 + 120, blurDrawBatch.floatVal3);
            byteBuffer.putFloat(local4210_7 + 124, blurDrawBatch.floatVal4);
         }
      }

      byteBuffer.position(0);
      return byteBuffer;
   }

   public void renderElement(DrawContext drawContext) {
      this.drawContext2 = drawContext;
   }

   public void processData(RenderPass renderPass) {
      if (renderPass != null && !this.list.isEmpty()) {
         GpuBuffer gpuBuffer = this.getGpuBuffer();
         if (gpuBuffer != null) {
            renderPass.setUniform("GlassParamsArray", gpuBuffer);
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

   private void updateState() {
      if (this.gpuBuffer4 != null) {
         this.gpuBuffer4.close();
         this.gpuBuffer4 = null;
      }
   }

   public void updateState2() {
      this.list.clear();
      this.list2.clear();
      this.flag = false;
   }

   public static GlassRenderer getGlassRenderer() {
      GlassRenderer currentGlassRenderer = glassRenderer;
      if (currentGlassRenderer == null) {
         synchronized (GlassRenderer.class) {
            currentGlassRenderer = glassRenderer;
            if (currentGlassRenderer == null) {
               currentGlassRenderer = new GlassRenderer();
               glassRenderer = currentGlassRenderer;
            }
         }
      }

      return currentGlassRenderer;
   }

   public void processData2(GlassDrawData glassDrawData) {
      this.renderElement3(this.drawContext2, glassDrawData);
   }

   private static void processData3(ByteBuffer byteBuffer, int local4219_1, int local4219_2) {
      byteBuffer.putFloat(local4219_1, (local4219_2 >>> 16 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local4219_1 + 4, (local4219_2 >>> 8 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local4219_1 + 8, (local4219_2 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local4219_1 + 12, (local4219_2 >>> 24 & 0xFF) / 255.0F);
   }

   private static float computeFloat(float local4220_0, float local4220_1, float local4220_2) {
      return Math.max(local4220_1, Math.min(local4220_2, local4220_0));
   }

   public void updateState3() {
      if (!this.list.isEmpty() && this.flag) {
         GpuBuffer gpuBuffer = this.getGpuBuffer2();
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

   public void renderElement2(DrawContext drawContext, GlassDrawData glassDrawData) {
      this.renderElement(drawContext);
      this.processData2(glassDrawData);
      this.updateState5();
   }

   private GlassDrawData getGlassDrawData(GlassDrawData glassDrawData) {
      float local4223_2 = Math.max(0.0F, Math.min(glassDrawData.width(), glassDrawData.height()) * 0.5F);
      float local4223_3 = computeFloat(glassDrawData.radiusTopLeft(), 0.0F, local4223_2);
      float local4223_4 = computeFloat(glassDrawData.radiusTopRight(), 0.0F, local4223_2);
      float local4223_5 = computeFloat(glassDrawData.radiusBottomRight(), 0.0F, local4223_2);
      float local4223_6 = computeFloat(glassDrawData.radiusBottomLeft(), 0.0F, local4223_2);
      float local4223_7 = computeFloat(glassDrawData.globalAlpha(), 0.0F, 1.0F);
      float local4223_8 = Math.max(glassDrawData.fresnelPower(), 0.001F);
      float local4223_9 = computeFloat(glassDrawData.baseAlpha(), 0.0F, 1.0F);
      float local4223_10 = computeFloat(glassDrawData.fresnelMix(), 0.0F, 1.0F);
      float local4223_11 = Math.max(glassDrawData.squirt(), 0.001F);
      float local4223_12 = Float.isFinite(glassDrawData.blurRadius()) ? computeFloat(glassDrawData.blurRadius(), 0.1F, 64.0F) : 0.1F;
      float local4223_13 = Float.isFinite(glassDrawData.colorOffset()) ? glassDrawData.colorOffset() : 0.0F;
      local4223_13 -= (float)Math.floor(local4223_13);
      return local4223_3 == glassDrawData.radiusTopLeft()
            && local4223_4 == glassDrawData.radiusTopRight()
            && local4223_5 == glassDrawData.radiusBottomRight()
            && local4223_6 == glassDrawData.radiusBottomLeft()
            && local4223_7 == glassDrawData.globalAlpha()
            && local4223_8 == glassDrawData.fresnelPower()
            && local4223_9 == glassDrawData.baseAlpha()
            && local4223_10 == glassDrawData.fresnelMix()
            && local4223_11 == glassDrawData.squirt()
            && local4223_12 == glassDrawData.blurRadius()
            && local4223_13 == glassDrawData.colorOffset()
         ? glassDrawData
         : new GlassDrawData(
               glassDrawData.x(),
               glassDrawData.y(),
               glassDrawData.width(),
               glassDrawData.height(),
               local4223_3,
               local4223_4,
               local4223_5,
               local4223_6,
               glassDrawData.color(),
               local4223_7,
               local4223_8,
               glassDrawData.fresnelColor(),
               local4223_9,
               glassDrawData.fresnelInvert(),
               local4223_10,
               glassDrawData.distortStrength(),
               local4223_11,
               glassDrawData.z(),
               local4223_12,
               glassDrawData.secondColor(),
               local4223_13
            )
            .withSplitIndex(glassDrawData.splitIndex());
   }

   public static void updateState4() {
      GlassRenderer currentGlassRenderer = glassRenderer;
      if (currentGlassRenderer != null) {
         currentGlassRenderer.close();
         glassRenderer = null;
      }
   }

   private GlassRenderer() {
   }

   private void renderElement3(DrawContext drawContext, GlassDrawData glassDrawData) {
      if (drawContext != null && glassDrawData != null && glassDrawData.visible()) {
         try {
            GlassDrawData glassDrawData2 = this.getGlassDrawData(glassDrawData);
            Matrix3x2f matrix3x2f = GuiScale.renderElement(drawContext);
            BlurDrawBatch blurDrawBatch = new BlurDrawBatch();
            BatchedBlurRenderer.getBatchedBlurRenderer()
               .renderElement4(
                  drawContext,
                  new BlurDrawData(
                     glassDrawData2.x(),
                     glassDrawData2.y(),
                     glassDrawData2.width(),
                     glassDrawData2.height(),
                     glassDrawData2.radiusTopLeft(),
                     glassDrawData2.radiusTopRight(),
                     glassDrawData2.radiusBottomRight(),
                     glassDrawData2.radiusBottomLeft(),
                     1.0F,
                     glassDrawData2.blurRadius(),
                     -1
                  ),
                  blurDrawBatch
               );
            ((DrawContextMixin)drawContext)
               .getGuiRenderState()
               .addSimpleElement(new GlassRenderState(matrix3x2f, glassDrawData2, ScissorStack.getScreenRect(), blurDrawBatch));
         } catch (RuntimeException exception) {
         }
      }
   }

   public boolean matchesCriteria(RenderPipeline renderPipeline) {
      return renderPipeline == pipeline2;
   }

   public void updateState5() {
      this.drawContext2 = null;
   }
}
