package greed.client.render;

import greed.mixin.DrawContextMixin;
import greed.client.util.GuiScale;
import greed.client.render.LineDrawData;
import greed.client.render.ScissorStack;

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

public final class LineRenderer implements AutoCloseable {
   private static volatile LineRenderer lineRenderer;
   public static final RenderPipeline pipeline2 = RenderPipeline.builder()
      .withLocation(getIdentifier("pipeline/line"))
      .withVertexShader(getIdentifier("core/line"))
      .withFragmentShader(getIdentifier("core/line"))
      .withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("LineParamsArray", UniformType.UNIFORM_BUFFER)
      .build();
   private final List<LineDrawData> list = new ArrayList<>(128);
   private DrawContext drawContext2;
   private GpuBuffer gpuBuffer2;
   private boolean flag = true;

   private static Identifier getIdentifier(String local1759_0) {
      return Identifier.of("greed", local1759_0);
   }

   @Override
   public void close() {
      this.list.clear();
      this.drawContext2 = null;
      this.updateState3();
   }

   public void processData(RenderPass renderPass) {
      if (renderPass != null && !this.list.isEmpty()) {
         GpuBuffer gpuBuffer = this.getGpuBuffer2();
         if (gpuBuffer != null) {
            renderPass.setUniform("LineParamsArray", gpuBuffer);
         }
      }
   }

   public void updateState() {
      this.list.clear();
      this.flag = false;
   }

   public void updateState2() {
      this.drawContext2 = null;
   }

   private LineRenderer() {
   }

   private void updateState3() {
      if (this.gpuBuffer2 != null) {
         this.gpuBuffer2.close();
         this.gpuBuffer2 = null;
      }
   }

   int computeInt(LineDrawData lineDrawData) {
      int local1766_2 = this.list.size();
      if (local1766_2 == 2048) {
         return -1;
      }

      this.list.add(lineDrawData);
      this.flag = true;
      return local1766_2;
   }

   public void renderElement(DrawContext drawContext) {
      this.drawContext2 = drawContext;
   }

   public void processData2(LineDrawData lineDrawData) {
      this.renderElement2(this.drawContext2, lineDrawData);
   }

   private void renderElement2(DrawContext drawContext, LineDrawData lineDrawData) {
      if (drawContext != null && lineDrawData != null && lineDrawData.visible()) {
         try {
            Matrix3x2f matrix3x2f = GuiScale.renderElement(drawContext);
            ((DrawContextMixin)drawContext)
               .getGuiRenderState()
               .addSimpleElement(new LineRenderState(matrix3x2f, lineDrawData, ScissorStack.getScreenRect()));
         } catch (RuntimeException exception) {
         }
      }
   }

   private GpuBuffer getGpuBuffer() {
      if (this.gpuBuffer2 != null && !this.gpuBuffer2.isClosed() && this.gpuBuffer2.size() >= 65536L) {
         return this.gpuBuffer2;
      }

      this.updateState3();

      try {
         this.gpuBuffer2 = RenderSystem.getDevice().createBuffer(() -> "greed_line_params", 136, 65536L);
         return this.gpuBuffer2;
      } catch (RuntimeException exception) {
         return null;
      }
   }

   public static void updateState4() {
      LineRenderer currentLineRenderer = lineRenderer;
      if (currentLineRenderer != null) {
         currentLineRenderer.close();
         lineRenderer = null;
      }
   }

   private ByteBuffer getByteBuffer(MemoryStack memoryStack, List<LineDrawData> local1772_2) {
      ByteBuffer byteBuffer = memoryStack.calloc(Math.min(65536, local1772_2.size() * 2 * 4 * 4));

      for (int index = 0; index < local1772_2.size(); index++) {
         LineDrawData lineDrawData = local1772_2.get(index);
         int local1772_6 = lineDrawData.color();
         int local1772_7 = index * 2 * 4 * 4;
         byteBuffer.putFloat(local1772_7, (local1772_6 >>> 16 & 0xFF) / 255.0F);
         byteBuffer.putFloat(local1772_7 + 4, (local1772_6 >>> 8 & 0xFF) / 255.0F);
         byteBuffer.putFloat(local1772_7 + 8, (local1772_6 & 0xFF) / 255.0F);
         byteBuffer.putFloat(local1772_7 + 12, (local1772_6 >>> 24 & 0xFF) / 255.0F);
         byteBuffer.putFloat(local1772_7 + 16, lineDrawData.length());
         byteBuffer.putFloat(local1772_7 + 20, lineDrawData.thickness());
         byteBuffer.putFloat(local1772_7 + 24, lineDrawData.fadeStart());
         byteBuffer.putFloat(local1772_7 + 28, lineDrawData.fadeEnd());
      }

      byteBuffer.position(0);
      return byteBuffer;
   }

   public static LineRenderer getLineRenderer() {
      LineRenderer currentLineRenderer = lineRenderer;
      if (currentLineRenderer == null) {
         synchronized (LineRenderer.class) {
            currentLineRenderer = lineRenderer;
            if (currentLineRenderer == null) {
               currentLineRenderer = new LineRenderer();
               lineRenderer = currentLineRenderer;
            }
         }
      }

      return currentLineRenderer;
   }

   private GpuBuffer getGpuBuffer2() {
      if (!this.flag && this.gpuBuffer2 != null) {
         return this.gpuBuffer2;
      }

      this.updateState5();
      if (!this.flag && this.gpuBuffer2 != null) {
         return this.gpuBuffer2;
      }

      this.updateState3();
      this.updateState5();
      return this.gpuBuffer2;
   }

   public boolean matchesCriteria(RenderPipeline renderPipeline) {
      return renderPipeline == pipeline2;
   }

   public void updateState5() {
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
}
