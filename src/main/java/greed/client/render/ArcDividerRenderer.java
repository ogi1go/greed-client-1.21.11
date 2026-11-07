package greed.client.render;

import greed.mixin.DrawContextMixin;
import greed.client.util.GuiScale;
import greed.client.render.ScissorStack;
import greed.client.render.ShimmerDrawData;

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

public final class ArcDividerRenderer implements AutoCloseable {
   private static volatile ArcDividerRenderer arcDividerRenderer;
   public static final RenderPipeline pipeline2 = RenderPipeline.builder()
      .withLocation(getIdentifier("pipeline/arc_divider"))
      .withVertexShader(getIdentifier("core/arc_divider"))
      .withFragmentShader(getIdentifier("core/arc_divider"))
      .withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("ArcParamsArray", UniformType.UNIFORM_BUFFER)
      .build();
   private final List<ShimmerDrawData> list = new ArrayList<>(64);
   private DrawContext drawContext2;
   private GpuBuffer gpuBuffer2;
   private boolean flag = true;

   public static ArcDividerRenderer getArcDividerRenderer() {
      ArcDividerRenderer currentArcDividerRenderer = arcDividerRenderer;
      if (currentArcDividerRenderer == null) {
         synchronized (ArcDividerRenderer.class) {
            currentArcDividerRenderer = arcDividerRenderer;
            if (currentArcDividerRenderer == null) {
               currentArcDividerRenderer = new ArcDividerRenderer();
               arcDividerRenderer = currentArcDividerRenderer;
            }
         }
      }

      return currentArcDividerRenderer;
   }

   private ByteBuffer getByteBuffer(MemoryStack memoryStack, List<ShimmerDrawData> local6184_2) {
      ByteBuffer byteBuffer = memoryStack.calloc(Math.min(8192, local6184_2.size() * 2 * 4 * 4));

      for (int index = 0; index < local6184_2.size(); index++) {
         ShimmerDrawData shimmerDrawData = local6184_2.get(index);
         int local6184_6 = index * 2 * 4 * 4;
         byteBuffer.putFloat(local6184_6, shimmerDrawData.halfWidth());
         byteBuffer.putFloat(local6184_6 + 4, Math.max(shimmerDrawData.lift(), 0.0F));
         byteBuffer.putFloat(local6184_6 + 8, Math.max(shimmerDrawData.thickness(), 0.5F));
         byteBuffer.putFloat(local6184_6 + 12, Math.max(shimmerDrawData.feather(), 0.5F));
         processData3(byteBuffer, local6184_6 + 16, shimmerDrawData.color());
      }

      byteBuffer.position(0);
      return byteBuffer;
   }

   private GpuBuffer getGpuBuffer() {
      if (this.gpuBuffer2 != null && !this.gpuBuffer2.isClosed() && this.gpuBuffer2.size() >= 8192L) {
         return this.gpuBuffer2;
      }

      this.updateState3();

      try {
         this.gpuBuffer2 = RenderSystem.getDevice().createBuffer(() -> "greed_arc_params", 136, 8192L);
         return this.gpuBuffer2;
      } catch (RuntimeException exception) {
         return null;
      }
   }

   public void processData(ShimmerDrawData shimmerDrawData) {
      this.renderElement(this.drawContext2, shimmerDrawData);
   }

   public void processData2(RenderPass renderPass) {
      if (renderPass != null && !this.list.isEmpty()) {
         GpuBuffer gpuBuffer = this.getGpuBuffer2();
         if (gpuBuffer != null) {
            renderPass.setUniform("ArcParamsArray", gpuBuffer);
         }
      }
   }

   int computeInt(ShimmerDrawData shimmerDrawData) {
      int local6188_2 = this.list.size();
      if (local6188_2 == 256) {
         return -1;
      }

      this.list.add(shimmerDrawData);
      this.flag = true;
      return local6188_2;
   }

   private ArcDividerRenderer() {
   }

   @Override
   public void close() {
      this.list.clear();
      this.drawContext2 = null;
      this.updateState3();
   }

   private static void processData3(ByteBuffer byteBuffer, int local6193_1, int local6193_2) {
      byteBuffer.putFloat(local6193_1, (local6193_2 >>> 16 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local6193_1 + 4, (local6193_2 >>> 8 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local6193_1 + 8, (local6193_2 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local6193_1 + 12, (local6193_2 >>> 24 & 0xFF) / 255.0F);
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

   private void renderElement(DrawContext drawContext, ShimmerDrawData shimmerDrawData) {
      if (drawContext != null && shimmerDrawData != null && shimmerDrawData.visible()) {
         try {
            Matrix3x2f matrix3x2f = GuiScale.renderElement(drawContext);
            ((DrawContextMixin)drawContext)
               .getGuiRenderState()
               .addSimpleElement(new ShimmerRenderState(matrix3x2f, shimmerDrawData, ScissorStack.getScreenRect()));
         } catch (RuntimeException exception) {
         }
      }
   }

   public void updateState2() {
      this.list.clear();
      this.flag = false;
   }

   private void updateState3() {
      if (this.gpuBuffer2 != null) {
         this.gpuBuffer2.close();
         this.gpuBuffer2 = null;
      }
   }

   public void updateState4() {
      this.drawContext2 = null;
   }

   private GpuBuffer getGpuBuffer2() {
      if (!this.flag && this.gpuBuffer2 != null) {
         return this.gpuBuffer2;
      }

      this.updateState();
      if (!this.flag && this.gpuBuffer2 != null) {
         return this.gpuBuffer2;
      }

      this.updateState3();
      this.updateState();
      return this.gpuBuffer2;
   }

   public boolean matchesCriteria(RenderPipeline renderPipeline) {
      return renderPipeline == pipeline2;
   }

   public static void updateState5() {
      ArcDividerRenderer currentArcDividerRenderer = arcDividerRenderer;
      if (currentArcDividerRenderer != null) {
         currentArcDividerRenderer.close();
         arcDividerRenderer = null;
      }
   }

   public void renderElement2(DrawContext drawContext) {
      this.drawContext2 = drawContext;
   }

   private static Identifier getIdentifier(String local6203_0) {
      return Identifier.of("greed", local6203_0);
   }
}
