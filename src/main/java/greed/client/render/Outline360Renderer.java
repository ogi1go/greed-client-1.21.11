package greed.client.render;

import greed.mixin.DrawContextMixin;
import greed.client.render.Outline360DrawData;
import greed.client.util.GuiScale;
import greed.client.render.ArcColorSpan;

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

public final class Outline360Renderer implements AutoCloseable {
   private static volatile Outline360Renderer outline360Renderer;
   public static final RenderPipeline pipeline2 = RenderPipeline.builder()
      .withLocation(getIdentifier("pipeline/outline_360"))
      .withVertexShader(getIdentifier("core/outline_360"))
      .withFragmentShader(getIdentifier("core/outline_360"))
      .withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("Outline360ParamsArray", UniformType.UNIFORM_BUFFER)
      .withUniform("Outline360RangesArray", UniformType.UNIFORM_BUFFER)
      .build();
   private final List<Outline360DrawData> list = new ArrayList<>(64);
   private DrawContext drawContext2;
   private GpuBuffer gpuBuffer;
   private GpuBuffer gpuBuffer2;
   private boolean flag = true;

   public static void updateState() {
      Outline360Renderer currentOutline360Renderer = outline360Renderer;
      if (currentOutline360Renderer != null) {
         currentOutline360Renderer.close();
         outline360Renderer = null;
      }
   }

   private static void processData(ByteBuffer byteBuffer, int local4231_1, int local4231_2) {
      byteBuffer.putFloat(local4231_1, (local4231_2 >>> 16 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local4231_1 + 4, (local4231_2 >>> 8 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local4231_1 + 8, (local4231_2 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local4231_1 + 12, (local4231_2 >>> 24 & 0xFF) / 255.0F);
   }

   private static float computeFloat(float local4232_0, float local4232_1, float local4232_2) {
      return Math.max(local4232_1, Math.min(local4232_2, local4232_0));
   }

   private void updateState2() {
      if (this.flag || this.gpuBuffer == null || this.gpuBuffer2 == null) {
         this.updateState5();
         if (this.flag || this.gpuBuffer == null || this.gpuBuffer2 == null) {
            this.updateState4();
            MemoryStack memoryStack = MemoryStack.stackPush();

            try {
               ByteBuffer byteBuffer = memoryStack.calloc(16384);
               ByteBuffer byteBuffer2 = memoryStack.calloc(32768);
               this.processData3(byteBuffer, byteBuffer2, this.list);
               this.gpuBuffer = RenderSystem.getDevice().createBuffer(() -> "greed_outline_360_params", 128, byteBuffer);
               this.gpuBuffer2 = RenderSystem.getDevice().createBuffer(() -> "greed_outline_360_ranges", 128, byteBuffer2);
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
         }
      }
   }

   private boolean checkState() {
      boolean local4234_1 = this.gpuBuffer != null && !this.gpuBuffer.isClosed() && this.gpuBuffer.size() >= 16384L;
      boolean local4234_2 = this.gpuBuffer2 != null && !this.gpuBuffer2.isClosed() && this.gpuBuffer2.size() >= 32768L;
      if (local4234_1 && local4234_2) {
         return true;
      }

      this.updateState4();

      try {
         this.gpuBuffer = RenderSystem.getDevice().createBuffer(() -> "greed_outline_360_params", 136, 16384L);
         this.gpuBuffer2 = RenderSystem.getDevice().createBuffer(() -> "greed_outline_360_ranges", 136, 32768L);
         return true;
      } catch (RuntimeException exception) {
         this.updateState4();
         return false;
      }
   }

   public void renderElement(DrawContext drawContext) {
      this.drawContext2 = drawContext;
   }

   public void updateState3() {
      this.list.clear();
      this.flag = false;
   }

   public void processData2(RenderPass renderPass) {
      if (renderPass != null && !this.list.isEmpty()) {
         this.updateState2();
         if (this.gpuBuffer != null) {
            renderPass.setUniform("Outline360ParamsArray", this.gpuBuffer);
         }

         if (this.gpuBuffer2 != null) {
            renderPass.setUniform("Outline360RangesArray", this.gpuBuffer2);
         }
      }
   }

   public boolean matchesCriteria(RenderPipeline renderPipeline) {
      return renderPipeline == pipeline2;
   }

   private Outline360DrawData getOutline360DrawData(Outline360DrawData outline360DrawData) {
      float local4241_2 = Math.max(0.0F, Math.min(outline360DrawData.width(), outline360DrawData.height()) * 0.5F);
      float local4241_3 = Math.max(0.0F, Math.min(outline360DrawData.width(), outline360DrawData.height()) * 0.5F);
      return new Outline360DrawData(
         outline360DrawData.x(),
         outline360DrawData.y(),
         outline360DrawData.width(),
         outline360DrawData.height(),
         computeFloat(outline360DrawData.radiusTopLeft(), 0.0F, local4241_2),
         computeFloat(outline360DrawData.radiusTopRight(), 0.0F, local4241_2),
         computeFloat(outline360DrawData.radiusBottomRight(), 0.0F, local4241_2),
         computeFloat(outline360DrawData.radiusBottomLeft(), 0.0F, local4241_2),
         computeFloat(outline360DrawData.thickness(), 0.0F, local4241_3),
         outline360DrawData.defaultColor(),
         Math.max(outline360DrawData.smoothness(), 0.0F),
         Math.max(outline360DrawData.blendDegrees(), 0.0F),
         outline360DrawData.angleOffsetDegrees(),
         outline360DrawData.ranges()
      );
   }

   @Override
   public void close() {
      this.list.clear();
      this.drawContext2 = null;
      this.updateState4();
   }

   private void renderElement2(DrawContext drawContext, Outline360DrawData outline360DrawData) {
      if (drawContext != null && outline360DrawData != null && outline360DrawData.visible()) {
         try {
            Outline360DrawData outline360DrawData2 = this.getOutline360DrawData(outline360DrawData);
            Matrix3x2f matrix3x2f = GuiScale.renderElement(drawContext);
            ((DrawContextMixin)drawContext).getGuiRenderState().addSimpleElement(new Outline360RenderState(matrix3x2f, outline360DrawData2));
         } catch (RuntimeException exception) {
         }
      }
   }

   public void renderElement3(DrawContext drawContext, Outline360DrawData outline360DrawData) {
      this.renderElement(drawContext);
      this.processData4(outline360DrawData);
      this.updateState6();
   }

   int computeInt(Outline360DrawData outline360DrawData) {
      int local4245_2 = this.list.size();
      if (local4245_2 == 256) {
         return -1;
      }

      this.list.add(outline360DrawData);
      this.flag = true;
      return local4245_2;
   }

   public static Outline360Renderer getOutline360Renderer() {
      Outline360Renderer currentOutline360Renderer = outline360Renderer;
      if (currentOutline360Renderer == null) {
         synchronized (Outline360Renderer.class) {
            currentOutline360Renderer = outline360Renderer;
            if (currentOutline360Renderer == null) {
               currentOutline360Renderer = new Outline360Renderer();
               outline360Renderer = currentOutline360Renderer;
            }
         }
      }

      return currentOutline360Renderer;
   }

   private Outline360Renderer() {
   }

   private void updateState4() {
      if (this.gpuBuffer != null) {
         this.gpuBuffer.close();
         this.gpuBuffer = null;
      }

      if (this.gpuBuffer2 != null) {
         this.gpuBuffer2.close();
         this.gpuBuffer2 = null;
      }
   }

   private void processData3(ByteBuffer byteBuffer, ByteBuffer byteBuffer2, List<Outline360DrawData> local4251_3) {
      int local4251_4 = 0;

      for (int index = 0; index < local4251_3.size(); index++) {
         Outline360DrawData outline360DrawData = local4251_3.get(index);
         int local4251_7 = index * 4 * 4 * 4;
         int local4251_8 = Math.min(outline360DrawData.ranges().size(), 1024 - local4251_4);
         byteBuffer.putFloat(local4251_7, outline360DrawData.radiusTopLeft());
         byteBuffer.putFloat(local4251_7 + 4, outline360DrawData.radiusTopRight());
         byteBuffer.putFloat(local4251_7 + 8, outline360DrawData.radiusBottomRight());
         byteBuffer.putFloat(local4251_7 + 12, outline360DrawData.radiusBottomLeft());
         byteBuffer.putFloat(local4251_7 + 16, outline360DrawData.width());
         byteBuffer.putFloat(local4251_7 + 20, outline360DrawData.height());
         byteBuffer.putFloat(local4251_7 + 24, outline360DrawData.thickness());
         byteBuffer.putFloat(local4251_7 + 28, outline360DrawData.smoothness());
         processData(byteBuffer, local4251_7 + 32, outline360DrawData.defaultColor());
         byteBuffer.putFloat(local4251_7 + 48, local4251_4);
         byteBuffer.putFloat(local4251_7 + 52, local4251_8);
         byteBuffer.putFloat(local4251_7 + 56, outline360DrawData.blendDegrees());
         byteBuffer.putFloat(local4251_7 + 60, outline360DrawData.angleOffsetDegrees());

         for (int index2 = 0; index2 < local4251_8; index2++) {
            ArcColorSpan arcColorSpan = outline360DrawData.ranges().get(index2);
            int local4251_11 = (local4251_4 + index2) * 2 * 4 * 4;
            byteBuffer2.putFloat(local4251_11, arcColorSpan.startDegrees());
            byteBuffer2.putFloat(local4251_11 + 4, arcColorSpan.endDegrees());
            byteBuffer2.putFloat(local4251_11 + 8, 0.0F);
            byteBuffer2.putFloat(local4251_11 + 12, 0.0F);
            processData(byteBuffer2, local4251_11 + 16, arcColorSpan.color());
         }

         local4251_4 += local4251_8;
      }

      byteBuffer.position(0);
      byteBuffer2.position(0);
   }

   public void processData4(Outline360DrawData outline360DrawData) {
      this.renderElement2(this.drawContext2, outline360DrawData);
   }

   public void updateState5() {
      if (!this.list.isEmpty() && this.flag) {
         if (this.checkState()) {
            try {
               MemoryStack memoryStack = MemoryStack.stackPush();

               try {
                  ByteBuffer byteBuffer = memoryStack.calloc(16384);
                  ByteBuffer byteBuffer2 = memoryStack.calloc(32768);
                  this.processData3(byteBuffer, byteBuffer2, this.list);
                  RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.gpuBuffer.slice(0L, byteBuffer.remaining()), byteBuffer);
                  RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.gpuBuffer2.slice(0L, byteBuffer2.remaining()), byteBuffer2);
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

   public void updateState6() {
      this.drawContext2 = null;
   }

   private static Identifier getIdentifier(String local4255_0) {
      return Identifier.of("greed", local4255_0);
   }
}
