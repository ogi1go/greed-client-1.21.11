package greed.client.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.RenderSystem.ShapeIndexBuffer;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.util.BufferAllocator;

final class ShapeBlurPass {
   private static final Map<Float, GpuBuffer> map = new HashMap<>();

   static void processData(RenderPipeline renderPipeline, Framebuffer framebuffer, GpuTextureView gpuTextureView, GpuSampler gpuSampler, GpuBuffer gpuBuffer) {
      processData2(renderPipeline, framebuffer, gpuTextureView, gpuSampler, 0.0F, gpuBuffer, null);
   }

   private static void processData2(
      RenderPipeline renderPipeline,
      Framebuffer framebuffer,
      GpuTextureView gpuTextureView,
      GpuSampler gpuSampler,
      float local3551_4,
      GpuBuffer gpuBuffer,
      int[] intArray
   ) {
      if (renderPipeline != null && framebuffer != null && gpuTextureView != null && gpuSampler != null && framebuffer.getColorAttachmentView() != null) {
         GpuBuffer gpuBuffer2 = getGpuBuffer(local3551_4);
         ShapeIndexBuffer sequentialBuffer = RenderSystem.getSequentialBuffer(DrawMode.QUADS);
         GpuBuffer indexBuffer = sequentialBuffer.getIndexBuffer(6);

         try (RenderPass renderPass = RenderSystem.getDevice()
               .createCommandEncoder()
               .createRenderPass(
                  () -> "greed_shape_blur_pass", framebuffer.getColorAttachmentView(), intArray == null ? OptionalInt.of(0) : OptionalInt.empty()
               )) {
            renderPass.setPipeline(renderPipeline);
            if (intArray != null) {
               renderPass.enableScissor(intArray[0], intArray[1], intArray[2], intArray[3]);
            }

            renderPass.bindTexture("Sampler0", gpuTextureView, gpuSampler);
            if (gpuBuffer != null) {
               renderPass.setUniform("GreedBlurRect", gpuBuffer);
            }

            renderPass.setVertexBuffer(0, gpuBuffer2);
            renderPass.setIndexBuffer(indexBuffer, sequentialBuffer.getIndexType());
            renderPass.drawIndexed(0, 0, 6, 1);
         }
      }
   }

   static void processData3(
      RenderPipeline renderPipeline, Framebuffer framebuffer, GpuTextureView gpuTextureView, GpuSampler gpuSampler, GpuBuffer gpuBuffer, int[] intArray
   ) {
      processData2(renderPipeline, framebuffer, gpuTextureView, gpuSampler, 0.0F, gpuBuffer, intArray);
   }

   static void processData4(RenderPipeline renderPipeline, Framebuffer framebuffer, GpuTextureView gpuTextureView, GpuSampler gpuSampler, float local3553_4) {
      processData2(renderPipeline, framebuffer, gpuTextureView, gpuSampler, local3553_4, null, null);
   }

   static void processData5(
      RenderPipeline renderPipeline, Framebuffer framebuffer, GpuTextureView gpuTextureView, GpuSampler gpuSampler, float local3554_4, int[] intArray
   ) {
      processData2(renderPipeline, framebuffer, gpuTextureView, gpuSampler, local3554_4, null, intArray);
   }

   private static GpuBuffer getGpuBuffer(float local3555_0) {
      GpuBuffer gpuBuffer = map.get(local3555_0);
      if (gpuBuffer != null && !gpuBuffer.isClosed()) {
         return gpuBuffer;
      }

      try (BufferAllocator bufferAllocator = new BufferAllocator(4 * GreedVertexFormats.vertexFormat2.getVertexSize())) {
         BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, GreedVertexFormats.vertexFormat2);
         bufferBuilder.vertex(-1.0F, 1.0F, 0.0F).texture(0.0F, 1.0F).lineWidth(local3555_0);
         bufferBuilder.vertex(-1.0F, -1.0F, 0.0F).texture(0.0F, 0.0F).lineWidth(local3555_0);
         bufferBuilder.vertex(1.0F, -1.0F, 0.0F).texture(1.0F, 0.0F).lineWidth(local3555_0);
         bufferBuilder.vertex(1.0F, 1.0F, 0.0F).texture(1.0F, 1.0F).lineWidth(local3555_0);

         try (BuiltBuffer builtBuffer = bufferBuilder.end()) {
            GpuBuffer gpuBuffer2 = RenderSystem.getDevice().createBuffer(() -> "greed_shape_blur_quad", 32, builtBuffer.getBuffer());
            map.put(local3555_0, gpuBuffer2);
            return gpuBuffer2;
         }
      }
   }

   private ShapeBlurPass() {
   }

   static ShapeBlurPass.SubContext getNested0426() {
      return new ShapeBlurPass.SubContext();
   }

   static final class SubContext implements AutoCloseable {
      @Override
      public void close() {
         GlStateManager._glBindFramebuffer(36160, 0);
         Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
         if (framebuffer != null) {
            GlStateManager._viewport(0, 0, framebuffer.textureWidth, framebuffer.textureHeight);
         }

         GlStateManager._disableScissorTest();
      }
   }
}
