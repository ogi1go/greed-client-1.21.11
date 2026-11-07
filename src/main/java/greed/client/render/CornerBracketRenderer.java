package greed.client.render;

import greed.mixin.DrawContextMixin;
import greed.client.util.GuiScale;
import greed.client.render.CornerBracketDrawData;
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

public final class CornerBracketRenderer implements AutoCloseable {
   private static volatile CornerBracketRenderer cornerBracketRenderer;
   public static final RenderPipeline pipeline2 = RenderPipeline.builder()
      .withLocation(getIdentifier("pipeline/corner_bracket"))
      .withVertexShader(getIdentifier("core/corner_bracket"))
      .withFragmentShader(getIdentifier("core/corner_bracket"))
      .withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("BracketParamsArray", UniformType.UNIFORM_BUFFER)
      .build();
   private final List<CornerBracketDrawData> list = new ArrayList<>(32);
   private DrawContext drawContext2;
   private GpuBuffer gpuBuffer2;
   private boolean flag = true;

   private void updateState() {
      if (this.gpuBuffer2 != null) {
         this.gpuBuffer2.close();
         this.gpuBuffer2 = null;
      }
   }

   public static void updateState2() {
      CornerBracketRenderer currentCornerBracketRenderer = cornerBracketRenderer;
      if (currentCornerBracketRenderer != null) {
         currentCornerBracketRenderer.close();
         cornerBracketRenderer = null;
      }
   }

   public void processData(CornerBracketDrawData cornerBracketDrawData) {
      this.renderElement3(this.drawContext2, cornerBracketDrawData);
   }

   public void updateState3() {
      this.drawContext2 = null;
   }

   private CornerBracketDrawData getCornerBracketDrawData(CornerBracketDrawData cornerBracketDrawData) {
      float local7181_2 = Math.max(cornerBracketDrawData.size(), 0.0F);
      float local7181_3 = computeFloat(cornerBracketDrawData.radius(), 0.0F, local7181_2);
      float local7181_4 = computeFloat(cornerBracketDrawData.stroke(), 0.0F, local7181_3 * 2.0F);
      return new CornerBracketDrawData(
         cornerBracketDrawData.x(),
         cornerBracketDrawData.y(),
         local7181_2,
         local7181_3,
         local7181_4,
         Math.max(cornerBracketDrawData.smoothness(), 0.05F),
         cornerBracketDrawData.quadrant() & 3,
         cornerBracketDrawData.color()
      );
   }

   @Override
   public void close() {
      this.list.clear();
      this.drawContext2 = null;
      this.updateState();
   }

   public void updateState4() {
      this.list.clear();
      this.flag = false;
   }

   public void processData2(RenderPass renderPass) {
      if (renderPass != null && !this.list.isEmpty()) {
         GpuBuffer gpuBuffer = this.getGpuBuffer2();
         if (gpuBuffer != null) {
            renderPass.setUniform("BracketParamsArray", gpuBuffer);
         }
      }
   }

   private GpuBuffer getGpuBuffer() {
      if (this.gpuBuffer2 != null && !this.gpuBuffer2.isClosed() && this.gpuBuffer2.size() >= 8192L) {
         return this.gpuBuffer2;
      }

      this.updateState();

      try {
         this.gpuBuffer2 = RenderSystem.getDevice().createBuffer(() -> "greed_bracket_params", 136, 8192L);
         return this.gpuBuffer2;
      } catch (RuntimeException exception) {
         return null;
      }
   }

   private CornerBracketRenderer() {
   }

   public void renderElement(DrawContext drawContext, CornerBracketDrawData cornerBracketDrawData) {
      this.renderElement2(drawContext);
      this.processData(cornerBracketDrawData);
      this.updateState3();
   }

   private static void processData3(ByteBuffer byteBuffer, int local7189_1, int local7189_2) {
      byteBuffer.putFloat(local7189_1, (local7189_2 >>> 16 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local7189_1 + 4, (local7189_2 >>> 8 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local7189_1 + 8, (local7189_2 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local7189_1 + 12, (local7189_2 >>> 24 & 0xFF) / 255.0F);
   }

   public void renderElement2(DrawContext drawContext) {
      this.drawContext2 = drawContext;
   }

   private static float computeFloat(float local7192_0, float local7192_1, float local7192_2) {
      return Math.max(local7192_1, Math.min(local7192_2, local7192_0));
   }

   private ByteBuffer getByteBuffer(MemoryStack memoryStack, List<CornerBracketDrawData> local7193_2) {
      ByteBuffer byteBuffer = memoryStack.calloc(Math.min(8192, local7193_2.size() * 2 * 4 * 4));

      for (int index = 0; index < local7193_2.size(); index++) {
         CornerBracketDrawData cornerBracketDrawData = local7193_2.get(index);
         int local7193_6 = index * 2 * 4 * 4;
         byteBuffer.putFloat(local7193_6, cornerBracketDrawData.size());
         byteBuffer.putFloat(local7193_6 + 4, cornerBracketDrawData.radius());
         byteBuffer.putFloat(local7193_6 + 8, cornerBracketDrawData.stroke());
         byteBuffer.putFloat(local7193_6 + 12, Math.max(cornerBracketDrawData.smoothness(), 0.05F));
         processData3(byteBuffer, local7193_6 + 16, cornerBracketDrawData.color());
      }

      byteBuffer.position(0);
      return byteBuffer;
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

   private static Identifier getIdentifier(String local7195_0) {
      return Identifier.of("greed", local7195_0);
   }

   public boolean matchesCriteria(RenderPipeline renderPipeline) {
      return renderPipeline == pipeline2;
   }

   public static CornerBracketRenderer getCornerBracketRenderer() {
      CornerBracketRenderer currentCornerBracketRenderer = cornerBracketRenderer;
      if (currentCornerBracketRenderer == null) {
         synchronized (CornerBracketRenderer.class) {
            currentCornerBracketRenderer = cornerBracketRenderer;
            if (currentCornerBracketRenderer == null) {
               currentCornerBracketRenderer = new CornerBracketRenderer();
               cornerBracketRenderer = currentCornerBracketRenderer;
            }
         }
      }

      return currentCornerBracketRenderer;
   }

   private GpuBuffer getGpuBuffer2() {
      if (!this.flag && this.gpuBuffer2 != null) {
         return this.gpuBuffer2;
      }

      this.updateState5();
      if (!this.flag && this.gpuBuffer2 != null) {
         return this.gpuBuffer2;
      }

      this.updateState();
      this.updateState5();
      return this.gpuBuffer2;
   }

   int computeInt(CornerBracketDrawData cornerBracketDrawData) {
      int local7199_2 = this.list.size();
      if (local7199_2 == 256) {
         return -1;
      }

      this.list.add(cornerBracketDrawData);
      this.flag = true;
      return local7199_2;
   }

   private void renderElement3(DrawContext drawContext, CornerBracketDrawData cornerBracketDrawData) {
      if (drawContext != null && cornerBracketDrawData != null) {
         try {
            CornerBracketDrawData cornerBracketDrawData2 = this.getCornerBracketDrawData(cornerBracketDrawData);
            if (!cornerBracketDrawData2.visible()) {
               return;
            }

            Matrix3x2f matrix3x2f = GuiScale.renderElement(drawContext);
            ((DrawContextMixin)drawContext)
               .getGuiRenderState()
               .addSimpleElement(new CornerBracketRenderState(matrix3x2f, cornerBracketDrawData2, ScissorStack.getScreenRect()));
         } catch (RuntimeException exception) {
         }
      }
   }
}
