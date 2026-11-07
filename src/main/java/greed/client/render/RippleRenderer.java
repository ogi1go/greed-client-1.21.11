package greed.client.render;

import greed.mixin.DrawContextMixin;
import greed.client.util.GuiScale;
import greed.client.render.RippleDrawData;
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
import java.util.List;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;
import org.lwjgl.system.MemoryStack;

public final class RippleRenderer implements AutoCloseable {
   private static volatile RippleRenderer rippleRenderer;
   private static final VertexFormat vertexFormat = VertexFormat.builder()
      .add("Position", VertexFormatElement.POSITION)
      .add("UV0", VertexFormatElement.UV0)
      .add("LineWidth", VertexFormatElement.LINE_WIDTH)
      .build();
   public static final RenderPipeline pipeline2 = RenderPipeline.builder()
      .withLocation(getIdentifier("pipeline/ripple"))
      .withVertexShader(getIdentifier("core/ripple"))
      .withFragmentShader(getIdentifier("core/ripple"))
      .withVertexFormat(vertexFormat, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("RippleParamsArray", UniformType.UNIFORM_BUFFER)
      .build();
   private final List<RippleDrawData> list = new ArrayList<>(128);
   private DrawContext drawContext2;
   private GpuBuffer gpuBuffer2;
   private boolean flag = true;

   private ByteBuffer getByteBuffer(MemoryStack memoryStack, List<RippleDrawData> local7312_2) {
      ByteBuffer byteBuffer = memoryStack.calloc(Math.min(40960, local7312_2.size() * 5 * 4 * 4));

      for (int index = 0; index < local7312_2.size(); index++) {
         RippleDrawData rippleDrawData = local7312_2.get(index);
         int local7312_6 = index * 5 * 4 * 4;
         byteBuffer.putFloat(local7312_6, rippleDrawData.radiusTopLeft());
         byteBuffer.putFloat(local7312_6 + 4, rippleDrawData.radiusTopRight());
         byteBuffer.putFloat(local7312_6 + 8, rippleDrawData.radiusBottomRight());
         byteBuffer.putFloat(local7312_6 + 12, rippleDrawData.radiusBottomLeft());
         byteBuffer.putFloat(local7312_6 + 16, rippleDrawData.width());
         byteBuffer.putFloat(local7312_6 + 20, rippleDrawData.height());
         byteBuffer.putFloat(local7312_6 + 24, rippleDrawData.smoothness());
         byteBuffer.putFloat(local7312_6 + 28, rippleDrawData.isTextured() ? 1.0F : 0.0F);
         byteBuffer.putFloat(local7312_6 + 32, rippleDrawData.centerX());
         byteBuffer.putFloat(local7312_6 + 36, rippleDrawData.centerY());
         byteBuffer.putFloat(local7312_6 + 40, rippleDrawData.rippleRadius());
         byteBuffer.putFloat(local7312_6 + 44, rippleDrawData.rippleSmoothness());
         processData(byteBuffer, local7312_6 + 48, rippleDrawData.sourceColor());
         processData(byteBuffer, local7312_6 + 64, rippleDrawData.targetColor());
      }

      byteBuffer.position(0);
      return byteBuffer;
   }

   private void updateState() {
      if (this.gpuBuffer2 != null) {
         this.gpuBuffer2.close();
         this.gpuBuffer2 = null;
      }
   }

   private static void processData(ByteBuffer byteBuffer, int local7314_1, int local7314_2) {
      byteBuffer.putFloat(local7314_1, (local7314_2 >>> 16 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local7314_1 + 4, (local7314_2 >>> 8 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local7314_1 + 8, (local7314_2 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local7314_1 + 12, (local7314_2 >>> 24 & 0xFF) / 255.0F);
   }

   private void renderElement(DrawContext drawContext, RippleDrawData rippleDrawData) {
      if (drawContext != null && rippleDrawData != null && rippleDrawData.visible()) {
         try {
            Matrix3x2f matrix3x2f = GuiScale.renderElement(drawContext);
            ((DrawContextMixin)drawContext)
               .getGuiRenderState()
               .addSimpleElement(new RippleRenderState(matrix3x2f, rippleDrawData, ScissorStack.getScreenRect()));
         } catch (RuntimeException exception) {
         }
      }
   }

   private GpuBuffer getGpuBuffer() {
      if (this.gpuBuffer2 != null && !this.gpuBuffer2.isClosed() && this.gpuBuffer2.size() >= 40960L) {
         return this.gpuBuffer2;
      }

      this.updateState();

      try {
         this.gpuBuffer2 = RenderSystem.getDevice().createBuffer(() -> "greed_ripple_params", 136, 40960L);
         return this.gpuBuffer2;
      } catch (RuntimeException exception) {
         return null;
      }
   }

   public void updateState2() {
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

   @Override
   public void close() {
      this.list.clear();
      this.drawContext2 = null;
      this.updateState();
   }

   public void processData2(RippleDrawData rippleDrawData) {
      this.renderElement(this.drawContext2, rippleDrawData);
   }

   public void updateState3() {
      this.list.clear();
      this.flag = false;
   }

   private static Identifier getIdentifier(String local7323_0) {
      return Identifier.of("greed", local7323_0);
   }

   public static void updateState4() {
      RippleRenderer currentRippleRenderer = rippleRenderer;
      if (currentRippleRenderer != null) {
         currentRippleRenderer.close();
         rippleRenderer = null;
      }
   }

   private RippleRenderer() {
   }

   public static RippleRenderer getRippleRenderer() {
      RippleRenderer currentRippleRenderer = rippleRenderer;
      if (currentRippleRenderer == null) {
         synchronized (RippleRenderer.class) {
            currentRippleRenderer = rippleRenderer;
            if (currentRippleRenderer == null) {
               currentRippleRenderer = new RippleRenderer();
               rippleRenderer = currentRippleRenderer;
            }
         }
      }

      return currentRippleRenderer;
   }

   public boolean matchesCriteria(RenderPipeline renderPipeline) {
      return renderPipeline == pipeline2;
   }

   private GpuBuffer getGpuBuffer2() {
      if (!this.flag && this.gpuBuffer2 != null) {
         return this.gpuBuffer2;
      }

      this.updateState2();
      if (!this.flag && this.gpuBuffer2 != null) {
         return this.gpuBuffer2;
      }

      this.updateState();
      this.updateState2();
      return this.gpuBuffer2;
   }

   int computeInt(RippleDrawData rippleDrawData) {
      int local7329_2 = this.list.size();
      if (local7329_2 == 512) {
         return -1;
      }

      this.list.add(rippleDrawData);
      this.flag = true;
      return local7329_2;
   }

   public void processData3(RenderPass renderPass) {
      if (renderPass != null && !this.list.isEmpty()) {
         GpuBuffer gpuBuffer = this.getGpuBuffer2();
         if (gpuBuffer != null) {
            renderPass.setUniform("RippleParamsArray", gpuBuffer);
         }
      }
   }

   public void renderElement2(DrawContext drawContext) {
      this.drawContext2 = drawContext;
   }

   public void updateState5() {
      this.drawContext2 = null;
   }
}
