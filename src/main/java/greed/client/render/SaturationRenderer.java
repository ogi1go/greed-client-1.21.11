package greed.client.render;

import greed.client.module.render.CustomWorldModule;
import greed.client.GreedClient;
import greed.client.render.SceneCopyBuffer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import org.lwjgl.system.MemoryStack;

public final class SaturationRenderer {
   private static SaturationRenderer saturationRenderer;
   private static final RenderPipeline pipeline2 = RenderPipeline.builder()
      .withLocation(GreedClient.id("pipeline/saturation"))
      .withVertexShader(GreedClient.id("core/saturation"))
      .withFragmentShader(GreedClient.id("core/saturation"))
      .withVertexFormat(VertexFormats.POSITION, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("SaturationParams", UniformType.UNIFORM_BUFFER)
      .build();
   private GpuBuffer gpuBuffer;
   private GpuBuffer gpuBuffer2;

   public void updateState() {
      if (CustomWorldModule.getInstance().isEnabled() && CustomWorldModule.getInstance().saturation.getValue() != 1.0F) {
         Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
         if (framebuffer != null
            && framebuffer.getColorAttachment() != null
            && framebuffer.getColorAttachmentView() != null
            && framebuffer.textureWidth > 0
            && framebuffer.textureHeight > 0) {
            GpuTextureView gpuTextureView = SceneCopyBuffer.getGpuTextureView(framebuffer);
            if (gpuTextureView != null) {
               if (this.gpuBuffer == null || this.gpuBuffer.isClosed()) {
                  try (BufferAllocator bufferAllocator = new BufferAllocator(4 * VertexFormats.POSITION.getVertexSize())) {
                     BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION);
                     bufferBuilder.vertex(-1.0F, -1.0F, 0.0F);
                     bufferBuilder.vertex(-1.0F, 1.0F, 0.0F);
                     bufferBuilder.vertex(1.0F, 1.0F, 0.0F);
                     bufferBuilder.vertex(1.0F, -1.0F, 0.0F);

                     try (BuiltBuffer builtBuffer = bufferBuilder.end()) {
                        this.gpuBuffer = RenderSystem.getDevice().createBuffer(() -> "greed_saturation_quad", 32, builtBuffer.getBuffer());
                     }
                  }
               }

               if (this.gpuBuffer2 == null || this.gpuBuffer2.isClosed()) {
                  this.gpuBuffer2 = RenderSystem.getDevice().createBuffer(() -> "greed_saturation_params", 136, 16L);
               }

               MemoryStack memoryStack = MemoryStack.stackPush();

               try {
                  ByteBuffer byteBuffer = memoryStack.calloc(16);
                  byteBuffer.putFloat(0, CustomWorldModule.getInstance().saturation.getValue());
                  RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.gpuBuffer2.slice(0L, 16L), byteBuffer);
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

               try (RenderPass local3385_3x = RenderSystem.getDevice()
                     .createCommandEncoder()
                     .createRenderPass(() -> "greed_saturation", framebuffer.getColorAttachmentView(), OptionalInt.empty(), null, OptionalDouble.empty())) {
                  local3385_3x.setPipeline(pipeline2);
                  local3385_3x.bindTexture("Sampler0", gpuTextureView, RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
                  local3385_3x.setUniform("SaturationParams", this.gpuBuffer2);
                  local3385_3x.setVertexBuffer(0, this.gpuBuffer);
                  local3385_3x.setIndexBuffer(
                     RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexBuffer(6), RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexType()
                  );
                  local3385_3x.drawIndexed(0, 0, 6, 1);
               }
            }
         }
      } else {
         this.updateState2();
      }
   }

   public static SaturationRenderer getInstance() {
      if (saturationRenderer == null) {
         saturationRenderer = new SaturationRenderer();
      }

      return saturationRenderer;
   }

   private SaturationRenderer() {
   }

   public void updateState2() {
      SceneCopyBuffer.updateState2();
      if (this.gpuBuffer != null && !this.gpuBuffer.isClosed()) {
         this.gpuBuffer.close();
      }

      this.gpuBuffer = null;
      if (this.gpuBuffer2 != null && !this.gpuBuffer2.isClosed()) {
         this.gpuBuffer2.close();
      }

      this.gpuBuffer2 = null;
   }
}
