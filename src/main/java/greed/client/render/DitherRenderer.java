package greed.client.render;

import greed.client.GreedClient;
import greed.mixin.DrawContextMixin;
import greed.client.util.GuiScale;
import greed.client.render.GuiLayerSerialAccess;
import greed.client.render.DitherDrawData;
import greed.client.render.ScissorStack;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;
import org.lwjgl.system.MemoryStack;

public final class DitherRenderer implements AutoCloseable {
   private static volatile DitherRenderer ditherRenderer;
   private static final VertexFormat vertexFormat = VertexFormat.builder()
      .add("Position", VertexFormatElement.POSITION)
      .add("UV0", VertexFormatElement.UV0)
      .add("Color", VertexFormatElement.COLOR)
      .add("LineWidth", VertexFormatElement.LINE_WIDTH)
      .build();
   public static final RenderPipeline pipeline2 = RenderPipeline.builder()
      .withLocation(getIdentifier("pipeline/dither"))
      .withVertexShader(getIdentifier("core/dither"))
      .withFragmentShader(getIdentifier("core/dither"))
      .withVertexFormat(vertexFormat, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("DitherParamsArray", UniformType.UNIFORM_BUFFER)
      .build();
   private final List<DitherDrawData> list = new ArrayList<>(16);
   private final Map<DitherRenderer.DataRecord, DitherRenderState> map = new LinkedHashMap<>(8);
   private DrawContext drawContext2;
   private GpuBuffer gpuBuffer2;
   private boolean flag;

   public static void updateState() {
      DitherRenderer currentDitherRenderer = ditherRenderer;
      if (currentDitherRenderer != null) {
         currentDitherRenderer.close();
         ditherRenderer = null;
      }
   }

   public void renderElement(DrawContext drawContext) {
      if (this.drawContext2 != drawContext) {
         this.map.clear();
      }

      this.drawContext2 = drawContext;
   }

   private void updateState2() {
      if (this.gpuBuffer2 != null) {
         this.gpuBuffer2.close();
         this.gpuBuffer2 = null;
      }
   }

   private ByteBuffer getByteBuffer(MemoryStack memoryStack) {
      ByteBuffer byteBuffer = memoryStack.calloc(8192);
      float local5452_3 = (float)(System.nanoTime() % 1000000000000L) / 1.0E9F;

      for (int index = 0; index < this.list.size(); index++) {
         DitherDrawData ditherDrawData = this.list.get(index);
         int local5452_6 = index * 8 * 4;
         byteBuffer.putFloat(local5452_6, ditherDrawData.cellSize());
         byteBuffer.putFloat(local5452_6 + 4, ditherDrawData.width());
         byteBuffer.putFloat(local5452_6 + 8, ditherDrawData.height());
         byteBuffer.putFloat(local5452_6 + 12, ditherDrawData.radius());
         byteBuffer.putFloat(local5452_6 + 16, local5452_3);
         byteBuffer.putFloat(local5452_6 + 20, 7.31F);
         byteBuffer.putFloat(local5452_6 + 24, ditherDrawData.spread());
         byteBuffer.putFloat(local5452_6 + 28, 0.0F);
      }

      byteBuffer.position(0);
      return byteBuffer;
   }

   public void updateState3() {
      if (!this.list.isEmpty() && this.flag) {
         GpuBuffer gpuBuffer = this.getGpuBuffer2();
         if (gpuBuffer != null) {
            try {
               MemoryStack memoryStack = MemoryStack.stackPush();

               try {
                  ByteBuffer byteBuffer = this.getByteBuffer(memoryStack);
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

   int computeInt(DitherDrawData ditherDrawData) {
      int local5454_2 = this.list.size();
      if (local5454_2 >= 256) {
         return -1;
      }

      this.list.add(ditherDrawData);
      this.flag = true;
      return local5454_2;
   }

   public void processData(DitherDrawData ditherDrawData) {
      if (this.drawContext2 != null && ditherDrawData != null) {
         try {
            GuiRenderState guiRenderState = ((DrawContextMixin)(Object)this.drawContext2).getGuiRenderState();
            int layerSerial = ((GuiLayerSerialAccess)guiRenderState).getLayerSerial();
            Matrix3x2f matrix3x2f = GuiScale.renderElement(this.drawContext2);
            ScreenRect screenRect = ScissorStack.getScreenRect();
            DitherRenderer.DataRecord local5455_6 = new DitherRenderer.DataRecord(guiRenderState, layerSerial, screenRect);
            DitherRenderState ditherRenderState = this.map.get(local5455_6);
            if (ditherRenderState == null) {
               ditherRenderState = new DitherRenderState(matrix3x2f, screenRect);
               ditherRenderState.add(ditherDrawData);
               this.map.put(local5455_6, ditherRenderState);
               guiRenderState.addSimpleElement(ditherRenderState);
            } else {
               ditherRenderState.add(ditherDrawData);
            }
         } catch (RuntimeException exception) {
            GreedClient.logger().warn("[DitherRenderer] submit failed", exception);
         }
      }
   }

   public void updateState4() {
      this.list.clear();
      this.flag = false;
   }

   private GpuBuffer getGpuBuffer() {
      if (!this.flag && this.gpuBuffer2 != null) {
         return this.gpuBuffer2;
      }

      this.updateState3();
      if (!this.flag && this.gpuBuffer2 != null) {
         return this.gpuBuffer2;
      }

      this.updateState2();
      MemoryStack memoryStack = MemoryStack.stackPush();

      GpuBuffer gpuBuffer;
      try {
         this.gpuBuffer2 = RenderSystem.getDevice().createBuffer(() -> "greed_dither_params", 128, this.getByteBuffer(memoryStack));
         this.flag = false;
         gpuBuffer = this.gpuBuffer2;
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

   private static Identifier getIdentifier(String local5458_0) {
      return Identifier.of("greed", local5458_0);
   }

   private DitherRenderer() {
   }

   @Override
   public void close() {
      this.list.clear();
      this.map.clear();
      this.updateState2();
      this.drawContext2 = null;
   }

   public void updateState5() {
      this.drawContext2 = null;
      this.map.clear();
   }

   public boolean matchesCriteria(RenderPipeline renderPipeline) {
      return renderPipeline == pipeline2;
   }

   private GpuBuffer getGpuBuffer2() {
      if (this.gpuBuffer2 != null && !this.gpuBuffer2.isClosed() && this.gpuBuffer2.size() >= 8192L) {
         return this.gpuBuffer2;
      }

      this.updateState2();

      try {
         this.gpuBuffer2 = RenderSystem.getDevice().createBuffer(() -> "greed_dither_params", 136, 8192L);
         return this.gpuBuffer2;
      } catch (RuntimeException exception) {
         return null;
      }
   }

   public void processData2(RenderPass renderPass) {
      if (renderPass != null && !this.list.isEmpty()) {
         GpuBuffer gpuBuffer = this.getGpuBuffer();
         if (gpuBuffer != null) {
            renderPass.setUniform("DitherParamsArray", gpuBuffer);
         }
      }
   }

   public static DitherRenderer getDitherRenderer() {
      DitherRenderer currentDitherRenderer = ditherRenderer;
      if (currentDitherRenderer == null) {
         synchronized (DitherRenderer.class) {
            currentDitherRenderer = ditherRenderer;
            if (currentDitherRenderer == null) {
               currentDitherRenderer = new DitherRenderer();
               ditherRenderer = currentDitherRenderer;
            }
         }
      }

      return currentDitherRenderer;
   }

   private record DataRecord(GuiRenderState state, int layer, ScreenRect scissor) {
   }
}
