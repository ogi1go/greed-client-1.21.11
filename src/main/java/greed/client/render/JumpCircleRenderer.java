package greed.client.render;

import greed.client.GreedClient;
import greed.client.module.render.JumpCircleModule;
import greed.client.ui.UiTheme;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.util.Iterator;
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
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

public final class JumpCircleRenderer {
   private static JumpCircleRenderer jumpCircleRenderer;
   private static final RenderPipeline pipeline2 = RenderPipeline.builder()
      .withLocation(GreedClient.id("pipeline/jump_circle"))
      .withVertexShader(GreedClient.id("core/jump_circle"))
      .withFragmentShader(GreedClient.id("core/jump_circle"))
      .withVertexFormat(VertexFormats.POSITION, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .withSampler("Sampler0")
      .withSampler("Sampler1")
      .withUniform("JumpCircleParams", UniformType.UNIFORM_BUFFER)
      .build();
   private SimpleFramebuffer framebuffer2;
   private GpuBuffer gpuBuffer;
   private GpuBuffer gpuBuffer2;
   private long timeMillis;

   public void updateState() {
      if (this.framebuffer2 != null) {
         this.framebuffer2.delete();
         this.framebuffer2 = null;
      }

      if (this.gpuBuffer != null && !this.gpuBuffer.isClosed()) {
         this.gpuBuffer.close();
      }

      this.gpuBuffer = null;
      if (this.gpuBuffer2 != null && !this.gpuBuffer2.isClosed()) {
         this.gpuBuffer2.close();
      }

      this.gpuBuffer2 = null;
   }

   private JumpCircleRenderer() {
   }

   public void processData(Matrix4f matrix4f, Matrix4f matrix4f2) {
      JumpCircleModule jumpCircleModule = JumpCircleModule.getInstance();
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      if (!jumpCircleModule.isEnabled() || minecraftClient.world == null || minecraftClient.player == null) {
         this.updateState();
      } else if (jumpCircleModule.getCopyOnWriteArrayList().isEmpty()) {
         if (this.timeMillis == 0L) {
            this.timeMillis = System.currentTimeMillis();
         } else if (System.currentTimeMillis() - this.timeMillis > 5000L) {
            this.updateState();
         }
      } else {
         this.timeMillis = 0L;
         Framebuffer framebuffer = minecraftClient.getFramebuffer();
         if (framebuffer != null
            && framebuffer.getColorAttachment() != null
            && framebuffer.getColorAttachmentView() != null
            && framebuffer.getDepthAttachmentView() != null
            && framebuffer.textureWidth > 0
            && framebuffer.textureHeight > 0) {
            if (this.framebuffer2 == null) {
               this.framebuffer2 = new SimpleFramebuffer("greed_jump_circle_scene", framebuffer.textureWidth, framebuffer.textureHeight, false);
            }

            if (this.framebuffer2.textureWidth != framebuffer.textureWidth || this.framebuffer2.textureHeight != framebuffer.textureHeight) {
               this.framebuffer2.resize(framebuffer.textureWidth, framebuffer.textureHeight);
            }

            if (this.gpuBuffer == null || this.gpuBuffer.isClosed()) {
               try (BufferAllocator bufferAllocator = new BufferAllocator(4 * VertexFormats.POSITION.getVertexSize())) {
                  BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION);
                  bufferBuilder.vertex(-1.0F, -1.0F, 0.0F);
                  bufferBuilder.vertex(-1.0F, 1.0F, 0.0F);
                  bufferBuilder.vertex(1.0F, 1.0F, 0.0F);
                  bufferBuilder.vertex(1.0F, -1.0F, 0.0F);

                  try (BuiltBuffer builtBuffer = bufferBuilder.end()) {
                     this.gpuBuffer = RenderSystem.getDevice().createBuffer(() -> "greed_jump_circle_quad", 32, builtBuffer.getBuffer());
                  }
               }
            }

            if (this.gpuBuffer2 == null || this.gpuBuffer2.isClosed()) {
               this.gpuBuffer2 = RenderSystem.getDevice().createBuffer(() -> "greed_jump_circle_params", 136, 2416L);
            }

            Matrix4f matrix4f3 = new Matrix4f(matrix4f2).mul(matrix4f);
            Vec3d cameraPos = minecraftClient.gameRenderer.getCamera().getCameraPos();
            int local7108_8 = jumpCircleModule.customColor.isChecked() ? jumpCircleModule.colorSetting.getValue() : UiTheme.intVal7;
            float local7108_9 = jumpCircleModule.range.getValue() + jumpCircleModule.glow.getValue() * 3.0F + 0.5F;
            Vector4f vector4f = new Vector4f();
            float local7108_11 = 1.0F;
            float local7108_12 = 1.0F;
            float local7108_13 = -1.0F;
            float local7108_14 = -1.0F;
            boolean local7108_15 = false;
            int local7108_16 = 0;
            MemoryStack memoryStack = MemoryStack.stackPush();

            label295: {
               try {
                  ByteBuffer byteBuffer = memoryStack.calloc(2416);
                  new Matrix4f(matrix4f3).invert().get(0, byteBuffer);
                  byteBuffer.putFloat(64, jumpCircleModule.range.getValue());
                  byteBuffer.putFloat(68, jumpCircleModule.glow.getValue());
                  byteBuffer.putFloat(76, jumpCircleModule.distortion.getValue());
                  byteBuffer.putFloat(80, (local7108_8 >> 16 & 0xFF) / 255.0F);
                  byteBuffer.putFloat(84, (local7108_8 >> 8 & 0xFF) / 255.0F);
                  byteBuffer.putFloat(88, (local7108_8 & 0xFF) / 255.0F);
                  byteBuffer.putFloat(92, 1.0F);
                  byteBuffer.putFloat(96, jumpCircleModule.radiusWave.isChecked() ? 1.0F : 0.0F);
                  byteBuffer.putFloat(100, jumpCircleModule.modeSetting.isSelected("Вода") ? 1.0F : 0.0F);
                  Iterator iterator = jumpCircleModule.getCopyOnWriteArrayList().iterator();

                  label215:
                  while (true) {
                     JumpCircleModule.DataRecord local7108_20;
                     float local7108_21;
                     float local7108_22;
                     float local7108_23;
                     while (true) {
                        if (!iterator.hasNext()) {
                           break label215;
                        }

                        local7108_20 = (JumpCircleModule.DataRecord)iterator.next();
                        if (local7108_16 >= 16) {
                           break label215;
                        }

                        local7108_21 = (float)(local7108_20.x() - cameraPos.x);
                        local7108_22 = (float)(local7108_20.y() - cameraPos.y);
                        local7108_23 = (float)(local7108_20.z() - cameraPos.z);
                        vector4f.set(local7108_21, local7108_22, local7108_23, 1.0F);
                        matrix4f3.transform(vector4f);
                        if (!(vector4f.w <= 0.05F)) {
                           boolean local7108_24 = false;
                           float local7108_25 = 1.0F;
                           float local7108_26 = 1.0F;
                           float local7108_27 = -1.0F;
                           float local7108_28 = -1.0F;

                           for (int index = 0; index < 8; index++) {
                              vector4f.set(
                                 (index & 1) == 0 ? local7108_21 - local7108_9 : local7108_21 + local7108_9,
                                 (index & 2) == 0 ? local7108_22 - 1.35F : local7108_22 + 1.35F,
                                 (index & 4) == 0 ? local7108_23 - local7108_9 : local7108_23 + local7108_9,
                                 1.0F
                              );
                              matrix4f3.transform(vector4f);
                              if (vector4f.w <= 0.05F) {
                                 local7108_24 = true;
                                 break;
                              }

                              local7108_25 = Math.min(local7108_25, vector4f.x / vector4f.w);
                              local7108_27 = Math.max(local7108_27, vector4f.x / vector4f.w);
                              local7108_26 = Math.min(local7108_26, vector4f.y / vector4f.w);
                              local7108_28 = Math.max(local7108_28, vector4f.y / vector4f.w);
                           }

                           if (local7108_24) {
                              local7108_15 = true;
                              break;
                           }

                           if (!(local7108_27 < -1.0F) && !(local7108_25 > 1.0F) && !(local7108_28 < -1.0F) && !(local7108_26 > 1.0F)) {
                              local7108_11 = Math.min(local7108_11, local7108_25);
                              local7108_13 = Math.max(local7108_13, local7108_27);
                              local7108_12 = Math.min(local7108_12, local7108_26);
                              local7108_14 = Math.max(local7108_14, local7108_28);
                              break;
                           }
                        }
                     }

                     byteBuffer.putFloat(112 + local7108_16 * 16, local7108_21);
                     byteBuffer.putFloat(116 + local7108_16 * 16, local7108_22);
                     byteBuffer.putFloat(120 + local7108_16 * 16, local7108_23);
                     byteBuffer.putFloat(124 + local7108_16 * 16, jumpCircleModule.computeFloat(local7108_20));

                     for (int index2 = 0; index2 < 32; index2++) {
                        byteBuffer.putFloat(368 + local7108_16 * 128 + index2 * 4, local7108_20.wallDistBlocks()[index2]);
                     }

                     local7108_16++;
                  }

                  if (local7108_16 == 0) {
                     break label295;
                  }

                  byteBuffer.putFloat(72, local7108_16);
                  RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.gpuBuffer2.slice(0L, 2416L), byteBuffer);
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

               int local7108_17x = 0;
               int local7108_18 = 0;
               int local7108_19 = framebuffer.textureWidth;
               int local7108_20 = framebuffer.textureHeight;
               if (!local7108_15) {
                  local7108_17x = Math.max(0, (int)Math.floor((Math.max(local7108_11, -1.0F) * 0.5F + 0.5F) * framebuffer.textureWidth) - 8);
                  local7108_18 = Math.max(0, (int)Math.floor((Math.max(local7108_12, -1.0F) * 0.5F + 0.5F) * framebuffer.textureHeight) - 8);
                  local7108_19 = Math.min(framebuffer.textureWidth, (int)Math.ceil((Math.min(local7108_13, 1.0F) * 0.5F + 0.5F) * framebuffer.textureWidth) + 8)
                     - local7108_17x;
                  local7108_20 = Math.min(
                        framebuffer.textureHeight, (int)Math.ceil((Math.min(local7108_14, 1.0F) * 0.5F + 0.5F) * framebuffer.textureHeight) + 8
                     )
                     - local7108_18;
                  if (local7108_19 <= 0 || local7108_20 <= 0) {
                     return;
                  }
               }

               RenderSystem.getDevice()
                  .createCommandEncoder()
                  .copyTextureToTexture(
                     framebuffer.getColorAttachment(), this.framebuffer2.getColorAttachment(), 0, 0, 0, 0, 0, framebuffer.textureWidth, framebuffer.textureHeight
                  );

               try (RenderPass renderPass = RenderSystem.getDevice()
                     .createCommandEncoder()
                     .createRenderPass(() -> "greed_jump_circle", framebuffer.getColorAttachmentView(), OptionalInt.empty(), null, OptionalDouble.empty())) {
                  renderPass.setPipeline(pipeline2);
                  renderPass.enableScissor(local7108_17x, local7108_18, local7108_19, local7108_20);
                  renderPass.bindTexture("Sampler0", framebuffer.getDepthAttachmentView(), RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
                  renderPass.bindTexture("Sampler1", this.framebuffer2.getColorAttachmentView(), RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
                  renderPass.setUniform("JumpCircleParams", this.gpuBuffer2);
                  renderPass.setVertexBuffer(0, this.gpuBuffer);
                  renderPass.setIndexBuffer(
                     RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexBuffer(6), RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexType()
                  );
                  renderPass.drawIndexed(0, 0, 6, 1);
               }

               return;
            }

            if (memoryStack != null) {
               memoryStack.close();
            }
         }
      }
   }

   public static JumpCircleRenderer getInstance() {
      if (jumpCircleRenderer == null) {
         jumpCircleRenderer = new JumpCircleRenderer();
      }

      return jumpCircleRenderer;
   }
}
