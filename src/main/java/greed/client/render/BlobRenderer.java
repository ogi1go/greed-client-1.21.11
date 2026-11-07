package greed.client.render;

import greed.mixin.DrawContextMixin;
import greed.client.util.GuiScale;
import greed.client.render.BlobDrawData;
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

public final class BlobRenderer implements AutoCloseable {
   private static volatile BlobRenderer blobRenderer;
   public static final RenderPipeline pipeline2 = RenderPipeline.builder()
      .withLocation(getIdentifier("pipeline/blob"))
      .withVertexShader(getIdentifier("core/blob"))
      .withFragmentShader(getIdentifier("core/blob"))
      .withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("BlobParamsArray", UniformType.UNIFORM_BUFFER)
      .build();
   private final List<BlobDrawData> list = new ArrayList<>(16);
   private DrawContext drawContext2;
   private GpuBuffer gpuBuffer2;
   private boolean flag;

   public void updateState() {
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

   private static void processData(ByteBuffer byteBuffer, int local4877_1, int local4877_2) {
      byteBuffer.putFloat(local4877_1, (local4877_2 >>> 16 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local4877_1 + 4, (local4877_2 >>> 8 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local4877_1 + 8, (local4877_2 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local4877_1 + 12, (local4877_2 >>> 24 & 0xFF) / 255.0F);
   }

   public void updateState2() {
      this.drawContext2 = null;
   }

   private void updateState3() {
      if (this.gpuBuffer2 != null) {
         this.gpuBuffer2.close();
         this.gpuBuffer2 = null;
      }
   }

   public void updateState4() {
      this.list.clear();
      this.flag = false;
   }

   public void processData2(BlobDrawData blobDrawData) {
      this.renderElement2(this.drawContext2, blobDrawData);
   }

   private BlobRenderer() {
   }

   public void renderElement(DrawContext drawContext) {
      this.drawContext2 = drawContext;
   }

   int computeInt(BlobDrawData blobDrawData) {
      int local4885_2 = this.list.size();
      if (local4885_2 == 128) {
         return -1;
      }

      this.list.add(blobDrawData);
      this.flag = true;
      return local4885_2;
   }

   private GpuBuffer getGpuBuffer() {
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

   public static void updateState5() {
      BlobRenderer currentBlobRenderer = blobRenderer;
      if (currentBlobRenderer != null) {
         currentBlobRenderer.close();
         blobRenderer = null;
      }
   }

   public void processData3(RenderPass renderPass) {
      if (renderPass != null && !this.list.isEmpty()) {
         GpuBuffer gpuBuffer = this.getGpuBuffer();
         if (gpuBuffer != null) {
            renderPass.setUniform("BlobParamsArray", gpuBuffer);
         }
      }
   }

   @Override
   public void close() {
      this.list.clear();
      this.drawContext2 = null;
      this.updateState3();
   }

   private GpuBuffer getGpuBuffer2() {
      if (this.gpuBuffer2 != null && !this.gpuBuffer2.isClosed() && this.gpuBuffer2.size() >= 8192L) {
         return this.gpuBuffer2;
      }

      this.updateState3();

      try {
         this.gpuBuffer2 = RenderSystem.getDevice().createBuffer(() -> "greed_blob_params", 136, 8192L);
         return this.gpuBuffer2;
      } catch (RuntimeException exception) {
         return null;
      }
   }

   private static Identifier getIdentifier(String local4891_0) {
      return Identifier.of("greed", local4891_0);
   }

   private ByteBuffer getByteBuffer(MemoryStack memoryStack, List<BlobDrawData> local4893_2) {
      ByteBuffer byteBuffer = memoryStack.calloc(Math.min(8192, local4893_2.size() * 4 * 4 * 4));

      for (int index = 0; index < local4893_2.size(); index++) {
         BlobDrawData blobDrawData = local4893_2.get(index);
         int local4893_6 = index * 4 * 4 * 4;
         byteBuffer.putFloat(local4893_6, blobDrawData.w());
         byteBuffer.putFloat(local4893_6 + 4, blobDrawData.h());
         byteBuffer.putFloat(local4893_6 + 8, blobDrawData.centerX());
         byteBuffer.putFloat(local4893_6 + 12, blobDrawData.centerY());
         byteBuffer.putFloat(local4893_6 + 16, blobDrawData.radiusX());
         byteBuffer.putFloat(local4893_6 + 20, blobDrawData.radiusY());
         byteBuffer.putFloat(local4893_6 + 24, blobDrawData.falloff());
         byteBuffer.putFloat(local4893_6 + 28, Math.max(blobDrawData.feather(), 0.001F));
         byteBuffer.putFloat(local4893_6 + 32, blobDrawData.clipTL());
         byteBuffer.putFloat(local4893_6 + 36, blobDrawData.clipTR());
         byteBuffer.putFloat(local4893_6 + 40, blobDrawData.clipBR());
         byteBuffer.putFloat(local4893_6 + 44, blobDrawData.clipBL());
         processData(byteBuffer, local4893_6 + 48, blobDrawData.color());
      }

      byteBuffer.position(0);
      return byteBuffer;
   }

   private void renderElement2(DrawContext drawContext, BlobDrawData blobDrawData) {
      if (drawContext != null && blobDrawData != null && blobDrawData.visible()) {
         try {
            Matrix3x2f matrix3x2f = GuiScale.renderElement(drawContext);
            ((DrawContextMixin)drawContext)
               .getGuiRenderState()
               .addSimpleElement(new BlobRenderState(matrix3x2f, blobDrawData, ScissorStack.getScreenRect()));
         } catch (RuntimeException exception) {
         }
      }
   }

   public static BlobRenderer getBlobRenderer() {
      BlobRenderer currentBlobRenderer = blobRenderer;
      if (currentBlobRenderer == null) {
         synchronized (BlobRenderer.class) {
            currentBlobRenderer = blobRenderer;
            if (currentBlobRenderer == null) {
               currentBlobRenderer = new BlobRenderer();
               blobRenderer = currentBlobRenderer;
            }
         }
      }

      return currentBlobRenderer;
   }

   public boolean matchesCriteria(RenderPipeline renderPipeline) {
      return renderPipeline == pipeline2;
   }
}
