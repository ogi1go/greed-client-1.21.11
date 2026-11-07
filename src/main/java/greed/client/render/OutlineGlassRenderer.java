package greed.client.render;

import greed.mixin.DrawContextMixin;
import greed.client.util.GuiScale;
import greed.client.render.BlurDrawBatch;
import greed.client.render.GlassOutlineDrawData;
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

public final class OutlineGlassRenderer implements AutoCloseable {
   private static volatile OutlineGlassRenderer outlineGlassRenderer;
   public static final RenderPipeline pipeline2 = RenderPipeline.builder()
      .withLocation(getIdentifier("pipeline/outline_glass"))
      .withVertexShader(getIdentifier("core/outline_glass"))
      .withFragmentShader(getIdentifier("core/outline_glass"))
      .withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("GlassOutlineParamsArray", UniformType.UNIFORM_BUFFER)
      .build();
   private final List<GlassOutlineDrawData> list = new ArrayList<>(128);
   private final List<BlurDrawBatch> list2 = new ArrayList<>(128);
   private DrawContext drawContext2;
   private GpuBuffer gpuBuffer2;
   private boolean flag = true;

   public void processData(RenderPass renderPass) {
      if (renderPass != null && !this.list.isEmpty()) {
         GpuBuffer gpuBuffer = this.getGpuBuffer();
         if (gpuBuffer != null) {
            renderPass.setUniform("GlassOutlineParamsArray", gpuBuffer);
         }
      }
   }

   private void updateState() {
      if (this.gpuBuffer2 != null) {
         this.gpuBuffer2.close();
         this.gpuBuffer2 = null;
      }
   }

   private GpuBuffer getGpuBuffer() {
      if (!this.flag && this.gpuBuffer2 != null) {
         return this.gpuBuffer2;
      }

      this.updateState3();
      if (!this.flag && this.gpuBuffer2 != null) {
         return this.gpuBuffer2;
      }

      this.updateState();
      this.updateState3();
      return this.gpuBuffer2;
   }

   @Override
   public void close() {
      this.list.clear();
      this.list2.clear();
      this.drawContext2 = null;
      this.updateState();
   }

   private static float computeFloat(float local3815_0, float local3815_1, float local3815_2) {
      return Math.max(local3815_1, Math.min(local3815_2, local3815_0));
   }

   public void updateState2() {
      this.drawContext2 = null;
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

   private void renderElement(DrawContext drawContext, GlassOutlineDrawData glassOutlineDrawData) {
      if (drawContext != null && glassOutlineDrawData != null && glassOutlineDrawData.visible()) {
         try {
            GlassOutlineDrawData glassOutlineDrawData2 = this.getGlassOutlineDrawData(glassOutlineDrawData);
            Matrix3x2f matrix3x2f = GuiScale.renderElement(drawContext);
            BlurDrawBatch blurDrawBatch = new BlurDrawBatch();
            BatchedBlurRenderer.getBatchedBlurRenderer()
               .renderElement4(
                  drawContext,
                  new BlurDrawData(
                     glassOutlineDrawData2.x(),
                     glassOutlineDrawData2.y(),
                     glassOutlineDrawData2.width(),
                     glassOutlineDrawData2.height(),
                     glassOutlineDrawData2.radiusTopLeft(),
                     glassOutlineDrawData2.radiusTopRight(),
                     glassOutlineDrawData2.radiusBottomRight(),
                     glassOutlineDrawData2.radiusBottomLeft(),
                     1.0F,
                     30.0F,
                     -1
                  ),
                  blurDrawBatch
               );
            ((DrawContextMixin)drawContext)
               .getGuiRenderState()
               .addSimpleElement(new GlassOutlineRenderState(matrix3x2f, glassOutlineDrawData2, ScissorStack.getScreenRect(), blurDrawBatch));
         } catch (RuntimeException exception) {
         }
      }
   }

   private OutlineGlassRenderer() {
   }

   private static void processData2(ByteBuffer byteBuffer, int local3821_1, int local3821_2) {
      byteBuffer.putFloat(local3821_1, (local3821_2 >>> 16 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local3821_1 + 4, (local3821_2 >>> 8 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local3821_1 + 8, (local3821_2 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local3821_1 + 12, (local3821_2 >>> 24 & 0xFF) / 255.0F);
   }

   private GpuBuffer getGpuBuffer2() {
      if (this.gpuBuffer2 != null && !this.gpuBuffer2.isClosed() && this.gpuBuffer2.size() >= 49056L) {
         return this.gpuBuffer2;
      }

      this.updateState();

      try {
         this.gpuBuffer2 = RenderSystem.getDevice().createBuffer(() -> "greed_outline_glass_params", 136, 49056L);
         return this.gpuBuffer2;
      } catch (RuntimeException exception) {
         return null;
      }
   }

   int computeInt(GlassOutlineDrawData glassOutlineDrawData, BlurDrawBatch blurDrawBatch) {
      int local3823_3 = this.list.size();
      if (local3823_3 == 438) {
         return -1;
      }

      this.list.add(glassOutlineDrawData);
      this.list2.add(blurDrawBatch);
      this.flag = true;
      return local3823_3;
   }

   public void updateState4() {
      this.list.clear();
      this.list2.clear();
      this.flag = false;
   }

   private static Identifier getIdentifier(String local3825_0) {
      return Identifier.of("greed", local3825_0);
   }

   public void processData3(GlassOutlineDrawData glassOutlineDrawData) {
      this.renderElement(this.drawContext2, glassOutlineDrawData);
   }

   private ByteBuffer getByteBuffer(MemoryStack memoryStack, List<GlassOutlineDrawData> local3827_2) {
      ByteBuffer byteBuffer = memoryStack.calloc(Math.min(49056, local3827_2.size() * 7 * 4 * 4));

      for (int index = 0; index < local3827_2.size(); index++) {
         GlassOutlineDrawData glassOutlineDrawData = local3827_2.get(index);
         int local3827_6 = index * 7 * 4 * 4;
         byteBuffer.putFloat(local3827_6, glassOutlineDrawData.radiusTopLeft());
         byteBuffer.putFloat(local3827_6 + 4, glassOutlineDrawData.radiusTopRight());
         byteBuffer.putFloat(local3827_6 + 8, glassOutlineDrawData.radiusBottomRight());
         byteBuffer.putFloat(local3827_6 + 12, glassOutlineDrawData.radiusBottomLeft());
         byteBuffer.putFloat(local3827_6 + 16, glassOutlineDrawData.width());
         byteBuffer.putFloat(local3827_6 + 20, glassOutlineDrawData.height());
         byteBuffer.putFloat(local3827_6 + 24, glassOutlineDrawData.thickness());
         byteBuffer.putFloat(local3827_6 + 28, glassOutlineDrawData.smoothness());
         byteBuffer.putFloat(local3827_6 + 32, glassOutlineDrawData.globalAlpha());
         byteBuffer.putFloat(local3827_6 + 36, glassOutlineDrawData.fresnelPower());
         byteBuffer.putFloat(local3827_6 + 40, glassOutlineDrawData.baseAlpha());
         byteBuffer.putFloat(local3827_6 + 44, glassOutlineDrawData.fresnelMix());
         processData2(byteBuffer, local3827_6 + 48, glassOutlineDrawData.fresnelColor());
         byteBuffer.putFloat(local3827_6 + 64, glassOutlineDrawData.fresnelInvert() ? 1.0F : 0.0F);
         byteBuffer.putFloat(local3827_6 + 68, glassOutlineDrawData.distortStrength());
         byteBuffer.putFloat(local3827_6 + 72, glassOutlineDrawData.z());
         byteBuffer.putFloat(local3827_6 + 76, Math.max(glassOutlineDrawData.squirt(), 0.001F));
         processData2(byteBuffer, local3827_6 + 80, glassOutlineDrawData.color());
         BlurDrawBatch blurDrawBatch = index < this.list2.size() ? this.list2.get(index) : null;
         if (blurDrawBatch != null) {
            byteBuffer.putFloat(local3827_6 + 96, blurDrawBatch.floatVal);
            byteBuffer.putFloat(local3827_6 + 100, blurDrawBatch.floatVal2);
            byteBuffer.putFloat(local3827_6 + 104, blurDrawBatch.floatVal3);
            byteBuffer.putFloat(local3827_6 + 108, blurDrawBatch.floatVal4);
         }
      }

      byteBuffer.position(0);
      return byteBuffer;
   }

   public boolean matchesCriteria(RenderPipeline renderPipeline) {
      return renderPipeline == pipeline2;
   }

   public void renderElement2(DrawContext drawContext, GlassOutlineDrawData glassOutlineDrawData) {
      this.renderElement3(drawContext);
      this.processData3(glassOutlineDrawData);
      this.updateState2();
   }

   public static OutlineGlassRenderer getOutlineGlassRenderer() {
      OutlineGlassRenderer currentOutlineGlassRenderer = outlineGlassRenderer;
      if (currentOutlineGlassRenderer == null) {
         synchronized (OutlineGlassRenderer.class) {
            currentOutlineGlassRenderer = outlineGlassRenderer;
            if (currentOutlineGlassRenderer == null) {
               currentOutlineGlassRenderer = new OutlineGlassRenderer();
               outlineGlassRenderer = currentOutlineGlassRenderer;
            }
         }
      }

      return currentOutlineGlassRenderer;
   }

   public void renderElement3(DrawContext drawContext) {
      this.drawContext2 = drawContext;
   }

   public static void updateState5() {
      OutlineGlassRenderer currentOutlineGlassRenderer = outlineGlassRenderer;
      if (currentOutlineGlassRenderer != null) {
         currentOutlineGlassRenderer.close();
         outlineGlassRenderer = null;
      }
   }

   private GlassOutlineDrawData getGlassOutlineDrawData(GlassOutlineDrawData glassOutlineDrawData) {
      float local3833_2 = Math.max(0.0F, Math.min(glassOutlineDrawData.width(), glassOutlineDrawData.height()) * 0.5F);
      float local3833_3 = Math.max(0.0F, Math.min(glassOutlineDrawData.width(), glassOutlineDrawData.height()) * 0.5F);
      return new GlassOutlineDrawData(
         glassOutlineDrawData.x(),
         glassOutlineDrawData.y(),
         glassOutlineDrawData.width(),
         glassOutlineDrawData.height(),
         computeFloat(glassOutlineDrawData.radiusTopLeft(), 0.0F, local3833_2),
         computeFloat(glassOutlineDrawData.radiusTopRight(), 0.0F, local3833_2),
         computeFloat(glassOutlineDrawData.radiusBottomRight(), 0.0F, local3833_2),
         computeFloat(glassOutlineDrawData.radiusBottomLeft(), 0.0F, local3833_2),
         computeFloat(glassOutlineDrawData.thickness(), 0.0F, local3833_3),
         glassOutlineDrawData.color(),
         computeFloat(glassOutlineDrawData.globalAlpha(), 0.0F, 1.0F),
         Math.max(glassOutlineDrawData.fresnelPower(), 0.001F),
         glassOutlineDrawData.fresnelColor(),
         computeFloat(glassOutlineDrawData.baseAlpha(), 0.0F, 1.0F),
         glassOutlineDrawData.fresnelInvert(),
         computeFloat(glassOutlineDrawData.fresnelMix(), 0.0F, 1.0F),
         glassOutlineDrawData.distortStrength(),
         Math.max(glassOutlineDrawData.squirt(), 0.001F),
         Math.max(glassOutlineDrawData.smoothness(), 0.0F),
         glassOutlineDrawData.z()
      );
   }
}
