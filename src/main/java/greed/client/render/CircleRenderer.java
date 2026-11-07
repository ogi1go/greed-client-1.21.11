package greed.client.render;

import greed.mixin.DrawContextMixin;
import greed.client.util.GuiScale;
import greed.client.render.CircleDrawData;
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

public final class CircleRenderer implements AutoCloseable {
   private static volatile CircleRenderer circleRenderer;
   public static final RenderPipeline pipeline2 = RenderPipeline.builder()
      .withLocation(getIdentifier("pipeline/circle"))
      .withVertexShader(getIdentifier("core/circle"))
      .withFragmentShader(getIdentifier("core/circle"))
      .withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("CircleParamsArray", UniformType.UNIFORM_BUFFER)
      .build();
   private final List<CircleDrawData> list = new ArrayList<>(128);
   private DrawContext drawContext2;
   private GpuBuffer gpuBuffer2;
   private boolean flag = true;

   private CircleDrawData getCircleDrawData(CircleDrawData circleDrawData) {
      return new CircleDrawData(
         circleDrawData.x(),
         circleDrawData.y(),
         Math.max(circleDrawData.radius(), 0.0F),
         computeFloat(circleDrawData.thickness(), 0.0F, circleDrawData.radius()),
         Math.max(circleDrawData.smoothness(), 0.5F),
         circleDrawData.color()
      );
   }

   int computeInt(CircleDrawData circleDrawData) {
      int local7202_2 = this.list.size();
      if (local7202_2 == 512) {
         return -1;
      }

      this.list.add(circleDrawData);
      this.flag = true;
      return local7202_2;
   }

   public boolean matchesCriteria(RenderPipeline renderPipeline) {
      return renderPipeline == pipeline2;
   }

   private GpuBuffer getGpuBuffer() {
      if (!this.flag && this.gpuBuffer2 != null) {
         return this.gpuBuffer2;
      }

      this.updateState4();
      if (!this.flag && this.gpuBuffer2 != null) {
         return this.gpuBuffer2;
      }

      this.updateState3();
      this.updateState4();
      return this.gpuBuffer2;
   }

   public void renderElement(DrawContext drawContext, CircleDrawData circleDrawData) {
      this.renderElement2(drawContext);
      this.processData2(circleDrawData);
      this.updateState();
   }

   @Override
   public void close() {
      this.list.clear();
      this.drawContext2 = null;
      this.updateState3();
   }

   public static CircleRenderer getCircleRenderer() {
      CircleRenderer currentCircleRenderer = circleRenderer;
      if (currentCircleRenderer == null) {
         synchronized (CircleRenderer.class) {
            currentCircleRenderer = circleRenderer;
            if (currentCircleRenderer == null) {
               currentCircleRenderer = new CircleRenderer();
               circleRenderer = currentCircleRenderer;
            }
         }
      }

      return currentCircleRenderer;
   }

   private GpuBuffer getGpuBuffer2() {
      if (this.gpuBuffer2 != null && !this.gpuBuffer2.isClosed() && this.gpuBuffer2.size() >= 16384L) {
         return this.gpuBuffer2;
      }

      this.updateState3();

      try {
         this.gpuBuffer2 = RenderSystem.getDevice().createBuffer(() -> "greed_circle_params", 136, 16384L);
         return this.gpuBuffer2;
      } catch (RuntimeException exception) {
         return null;
      }
   }

   public void updateState() {
      this.drawContext2 = null;
   }

   private static Identifier getIdentifier(String local7211_0) {
      return Identifier.of("greed", local7211_0);
   }

   private CircleRenderer() {
   }

   public void processData(RenderPass renderPass) {
      if (renderPass != null && !this.list.isEmpty()) {
         GpuBuffer gpuBuffer = this.getGpuBuffer();
         if (gpuBuffer != null) {
            renderPass.setUniform("CircleParamsArray", gpuBuffer);
         }
      }
   }

   public void renderElement2(DrawContext drawContext) {
      this.drawContext2 = drawContext;
   }

   private void renderElement3(DrawContext drawContext, CircleDrawData circleDrawData) {
      if (drawContext != null && circleDrawData != null && circleDrawData.visible()) {
         try {
            CircleDrawData circleDrawData2 = this.getCircleDrawData(circleDrawData);
            Matrix3x2f matrix3x2f = GuiScale.renderElement(drawContext);
            ((DrawContextMixin)drawContext)
               .getGuiRenderState()
               .addSimpleElement(new CircleRenderState(matrix3x2f, circleDrawData2, ScissorStack.getScreenRect()));
         } catch (RuntimeException exception) {
         }
      }
   }

   public static void updateState2() {
      CircleRenderer currentCircleRenderer = circleRenderer;
      if (currentCircleRenderer != null) {
         currentCircleRenderer.close();
         circleRenderer = null;
      }
   }

   private ByteBuffer getByteBuffer(MemoryStack memoryStack, List<CircleDrawData> local7217_2) {
      ByteBuffer byteBuffer = memoryStack.calloc(Math.min(16384, local7217_2.size() * 2 * 4 * 4));

      for (int index = 0; index < local7217_2.size(); index++) {
         CircleDrawData circleDrawData = local7217_2.get(index);
         int local7217_6 = index * 2 * 4 * 4;
         byteBuffer.putFloat(local7217_6, circleDrawData.radius());
         byteBuffer.putFloat(local7217_6 + 4, Math.max(circleDrawData.smoothness(), 0.0F));
         byteBuffer.putFloat(local7217_6 + 8, circleDrawData.thickness());
         byteBuffer.putFloat(local7217_6 + 12, 0.0F);
         processData3(byteBuffer, local7217_6 + 16, circleDrawData.color());
      }

      byteBuffer.position(0);
      return byteBuffer;
   }

   private void updateState3() {
      if (this.gpuBuffer2 != null) {
         this.gpuBuffer2.close();
         this.gpuBuffer2 = null;
      }
   }

   public void updateState4() {
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

   public void processData2(CircleDrawData circleDrawData) {
      this.renderElement3(this.drawContext2, circleDrawData);
   }

   private static float computeFloat(float local7221_0, float local7221_1, float local7221_2) {
      return Math.max(local7221_1, Math.min(local7221_2, local7221_0));
   }

   public void updateState5() {
      this.list.clear();
      this.flag = false;
   }

   private static void processData3(ByteBuffer byteBuffer, int local7224_1, int local7224_2) {
      byteBuffer.putFloat(local7224_1, (local7224_2 >>> 16 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local7224_1 + 4, (local7224_2 >>> 8 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local7224_1 + 8, (local7224_2 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local7224_1 + 12, (local7224_2 >>> 24 & 0xFF) / 255.0F);
   }
}
