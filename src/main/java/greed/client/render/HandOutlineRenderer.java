package greed.client.render;

import greed.client.GreedClient;
import greed.client.module.render.SwordAnimationsModule;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import org.lwjgl.system.MemoryStack;

public final class HandOutlineRenderer {
   private static HandOutlineRenderer handOutlineRenderer;
   private static final RenderPipeline pipeline2 = RenderPipeline.builder()
           .withLocation(GreedClient.id("pipeline/hand_outline"))
           .withVertexShader(GreedClient.id("core/hand_outline"))
           .withFragmentShader(GreedClient.id("core/hand_outline"))
           .withVertexFormat(VertexFormats.POSITION, DrawMode.QUADS)
           .withBlend(BlendFunction.TRANSLUCENT)
           .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
           .withDepthWrite(false)
           .withCull(false)
           .withSampler("Sampler0")
           .withSampler("Sampler1")
           .withUniform("HandOutlineParams", UniformType.UNIFORM_BUFFER)
           .build();
   private static final RenderPipeline pipeline3 = RenderPipeline.builder()
           .withLocation(GreedClient.id("pipeline/hand_outline_blur"))
           .withVertexShader(GreedClient.id("core/hand_outline_blur"))
           .withFragmentShader(GreedClient.id("core/hand_outline_blur"))
           .withVertexFormat(VertexFormats.POSITION, DrawMode.QUADS)
           .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
           .withDepthWrite(false)
           .withCull(false)
           .withSampler("Sampler0")
           .withUniform("HandOutlineParams", UniformType.UNIFORM_BUFFER)
           .build();
   private static final RenderPipeline beautifulPipeline = RenderPipeline.builder()
           .withLocation(GreedClient.id("pipeline/hand_beautiful"))
           .withVertexShader(GreedClient.id("core/hand_beautiful"))
           .withFragmentShader(GreedClient.id("core/hand_beautiful"))
           .withVertexFormat(VertexFormats.POSITION, DrawMode.QUADS)
           .withBlend(BlendFunction.TRANSLUCENT)
           .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
           .withDepthWrite(false)
           .withCull(false)
           .withSampler("Sampler0")
           .withUniform("HandBeautifulParams", UniformType.UNIFORM_BUFFER)
           .build();

   private SimpleFramebuffer framebuffer2;
   private SimpleFramebuffer framebuffer3;
   private GpuTexture gpuTexture;
   private GpuTextureView gpuTextureView;
   private SimpleFramebuffer framebuffer4;
   private GpuBuffer gpuBuffer;
   private GpuBuffer gpuBuffer2;
   private GpuBuffer beautifulUniformBuffer;
   private boolean flag;
   private boolean flag2;

   public void updateState() {
      if (this.flag2) {
         this.flag2 = false;
         this.flag = false;
         if (SwordAnimationsModule.getInstance().checkState4()) {
            Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
            if (framebuffer != null
                    && framebuffer.getColorAttachmentView() != null
                    && this.gpuTexture != null
                    && this.gpuTextureView != null
                    && !this.gpuTextureView.isClosed()) {
               if (this.gpuTexture.getWidth(0) == framebuffer.textureWidth && this.gpuTexture.getHeight(0) == framebuffer.textureHeight) {
                  if (this.gpuBuffer == null || this.gpuBuffer.isClosed()) {
                     try (BufferAllocator bufferAllocator = new BufferAllocator(4 * VertexFormats.POSITION.getVertexSize())) {
                        BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION);
                        bufferBuilder.vertex(-1.0F, -1.0F, 0.0F);
                        bufferBuilder.vertex(-1.0F, 1.0F, 0.0F);
                        bufferBuilder.vertex(1.0F, 1.0F, 0.0F);
                        bufferBuilder.vertex(1.0F, -1.0F, 0.0F);

                        try (BuiltBuffer builtBuffer = bufferBuilder.end()) {
                           this.gpuBuffer = RenderSystem.getDevice().createBuffer(() -> "greed_hand_outline_quad", 32, builtBuffer.getBuffer());
                        }
                     }
                  }

                  boolean isBeautiful = SwordAnimationsModule.getInstance().shaderMode.isSelected("Красивый");

                  if (isBeautiful) {
                     if (this.beautifulUniformBuffer == null || this.beautifulUniformBuffer.isClosed()) {
                        this.beautifulUniformBuffer = RenderSystem.getDevice().createBuffer(() -> "greed_hand_beautiful_params", 136, 128L);
                     }

                     try (MemoryStack memoryStack = MemoryStack.stackPush()) {
                        ByteBuffer byteBuffer = memoryStack.calloc(128);
                        SwordAnimationsModule module = SwordAnimationsModule.getInstance();
                        int colorRgb = module.computeInt2();
                        float r = (colorRgb >> 16 & 0xFF) / 255.0F;
                        float g = (colorRgb >> 8 & 0xFF) / 255.0F;
                        float b = (colorRgb & 0xFF) / 255.0F;

                        byteBuffer.putFloat(0, r);
                        byteBuffer.putFloat(4, g);
                        byteBuffer.putFloat(8, b);
                        byteBuffer.putFloat(12, 1.0F);
                        byteBuffer.putFloat(16, r + (1.0F - r) * 0.45F);
                        byteBuffer.putFloat(20, g + (1.0F - g) * 0.45F);
                        byteBuffer.putFloat(24, b + (1.0F - b) * 0.45F);
                        byteBuffer.putFloat(28, 1.0F);
                        byteBuffer.putFloat(32, (float)(System.currentTimeMillis() % 100000L) / 1000.0F);
                        byteBuffer.putFloat(36, 1.2F);
                        byteBuffer.putFloat(40, 1.0F);
                        byteBuffer.putFloat(44, (float)module.outline.getValue());
                        byteBuffer.putFloat(48, (float)module.highlight.getValue());
                        byteBuffer.putFloat(52, (float)module.fill.getValue());
                        byteBuffer.putFloat(56, 1.0F);
                        byteBuffer.putFloat(60, 0.0F);
                        byteBuffer.putFloat(64, 1.0F / framebuffer.textureWidth);
                        byteBuffer.putFloat(68, 1.0F / framebuffer.textureHeight);

                        RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.beautifulUniformBuffer.slice(0L, 128L), byteBuffer);
                     }

                     try (RenderPass renderPass = RenderSystem.getDevice()
                             .createCommandEncoder()
                             .createRenderPass(() -> "greed_hand_beautiful", framebuffer.getColorAttachmentView(), OptionalInt.empty(), null, OptionalDouble.empty())) {
                        renderPass.setPipeline(beautifulPipeline);
                        renderPass.bindTexture("Sampler0", this.gpuTextureView, RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
                        renderPass.setUniform("HandBeautifulParams", this.beautifulUniformBuffer);
                        this.processData(renderPass);
                     }
                  } else {
                     if (this.gpuBuffer2 == null || this.gpuBuffer2.isClosed()) {
                        this.gpuBuffer2 = RenderSystem.getDevice().createBuffer(() -> "greed_hand_outline_params", 136, 48L);
                     }

                     MemoryStack memoryStack = MemoryStack.stackPush();

                     try {
                        ByteBuffer byteBuffer = memoryStack.calloc(48);
                        byteBuffer.putFloat(0, (SwordAnimationsModule.getInstance().computeInt2() >> 16 & 0xFF) / 255.0F);
                        byteBuffer.putFloat(4, (SwordAnimationsModule.getInstance().computeInt2() >> 8 & 0xFF) / 255.0F);
                        byteBuffer.putFloat(8, (SwordAnimationsModule.getInstance().computeInt2() & 0xFF) / 255.0F);
                        byteBuffer.putFloat(12, (float)SwordAnimationsModule.getInstance().fill.getValue());
                        byteBuffer.putFloat(24, (float)SwordAnimationsModule.getInstance().outline.getValue());
                        byteBuffer.putFloat(28, (float)SwordAnimationsModule.getInstance().highlight.getValue());
                        byteBuffer.putFloat(32, 1.0F / framebuffer.textureWidth);
                        byteBuffer.putFloat(36, 1.0F / framebuffer.textureHeight);
                        RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.gpuBuffer2.slice(0L, 48L), byteBuffer);
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

                     this.framebuffer4 = this.getSimpleFramebuffer(this.framebuffer4, "greed_hand_outline_blur", framebuffer.textureWidth, framebuffer.textureHeight);

                     try (RenderPass local511_2x = RenderSystem.getDevice()
                             .createCommandEncoder()
                             .createRenderPass(
                                     () -> "greed_hand_outline_blur", this.framebuffer4.getColorAttachmentView(), OptionalInt.empty(), null, OptionalDouble.empty()
                             )) {
                        local511_2x.setPipeline(pipeline3);
                        local511_2x.bindTexture("Sampler0", this.gpuTextureView, RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
                        local511_2x.setUniform("HandOutlineParams", this.gpuBuffer2);
                        this.processData(local511_2x);
                     }

                     try (RenderPass local511_2x = RenderSystem.getDevice()
                             .createCommandEncoder()
                             .createRenderPass(() -> "greed_hand_outline", framebuffer.getColorAttachmentView(), OptionalInt.empty(), null, OptionalDouble.empty())) {
                        local511_2x.setPipeline(pipeline2);
                        local511_2x.bindTexture("Sampler0", this.framebuffer4.getColorAttachmentView(), RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
                        local511_2x.bindTexture("Sampler1", this.gpuTextureView, RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
                        local511_2x.setUniform("HandOutlineParams", this.gpuBuffer2);
                        this.processData(local511_2x);
                     }
                  }
               }
            }
         }
      }
   }

   public void updateState2() {
      if (this.flag && SwordAnimationsModule.getInstance().checkState4()) {
         Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
         if (framebuffer != null && framebuffer.getDepthAttachment() != null) {
            GpuTexture depthAttachment = framebuffer.getDepthAttachment();
            if (this.gpuTexture != null
                    && (
                    this.gpuTexture.isClosed()
                            || this.gpuTexture.getWidth(0) != framebuffer.textureWidth
                            || this.gpuTexture.getHeight(0) != framebuffer.textureHeight
                            || this.gpuTexture.getFormat() != depthAttachment.getFormat()
            )) {
               this.updateState3();
            }

            if (this.gpuTexture == null) {
               this.gpuTexture = RenderSystem.getDevice()
                       .createTexture(() -> "greed_hand_depth", 15, depthAttachment.getFormat(), framebuffer.textureWidth, framebuffer.textureHeight, 1, 1);
               this.gpuTextureView = RenderSystem.getDevice().createTextureView(this.gpuTexture);
            }

            RenderSystem.getDevice()
                    .createCommandEncoder()
                    .copyTextureToTexture(depthAttachment, this.gpuTexture, 0, 0, 0, 0, 0, framebuffer.textureWidth, framebuffer.textureHeight);
            this.flag2 = true;
         }
      }
   }

   private void updateState3() {
      if (this.gpuTextureView != null) {
         if (!this.gpuTextureView.isClosed()) {
            this.gpuTextureView.close();
         }

         this.gpuTextureView = null;
      }

      if (this.gpuTexture != null) {
         if (!this.gpuTexture.isClosed()) {
            this.gpuTexture.close();
         }

         this.gpuTexture = null;
      }
   }

   public static HandOutlineRenderer getInstance() {
      if (handOutlineRenderer == null) {
         handOutlineRenderer = new HandOutlineRenderer();
      }

      return handOutlineRenderer;
   }

   public void updateState4() {
      this.flag = false;
      this.flag2 = false;
      if (this.framebuffer2 != null) {
         this.framebuffer2.delete();
         this.framebuffer2 = null;
      }

      if (this.framebuffer3 != null) {
         this.framebuffer3.delete();
         this.framebuffer3 = null;
      }

      this.updateState3();
      if (this.framebuffer4 != null) {
         this.framebuffer4.delete();
         this.framebuffer4 = null;
      }

      if (this.gpuBuffer != null && !this.gpuBuffer.isClosed()) {
         this.gpuBuffer.close();
      }

      this.gpuBuffer = null;
      if (this.gpuBuffer2 != null && !this.gpuBuffer2.isClosed()) {
         this.gpuBuffer2.close();
      }

      this.gpuBuffer2 = null;

      if (this.beautifulUniformBuffer != null && !this.beautifulUniformBuffer.isClosed()) {
         this.beautifulUniformBuffer.close();
      }

      this.beautifulUniformBuffer = null;
   }

   private SimpleFramebuffer getSimpleFramebuffer(SimpleFramebuffer simpleFramebuffer, String local519_2, int local519_3, int local519_4) {
      if (simpleFramebuffer == null) {
         return new SimpleFramebuffer(local519_2, local519_3, local519_4, false);
      }

      if (simpleFramebuffer.textureWidth != local519_3 || simpleFramebuffer.textureHeight != local519_4) {
         simpleFramebuffer.resize(local519_3, local519_4);
      }

      return simpleFramebuffer;
   }

   private void processData(RenderPass renderPass) {
      renderPass.setVertexBuffer(0, this.gpuBuffer);
      renderPass.setIndexBuffer(
              RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexBuffer(6), RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexType()
      );
      renderPass.drawIndexed(0, 0, 6, 1);
   }

   private HandOutlineRenderer() {
   }

   public void updateState5() {
      if (!SwordAnimationsModule.getInstance().checkState4()) {
         this.updateState4();
      } else {
         Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
         if (framebuffer != null && framebuffer.getDepthAttachment() != null && framebuffer.textureWidth > 0 && framebuffer.textureHeight > 0) {
            this.flag = true;
         }
      }
   }
}