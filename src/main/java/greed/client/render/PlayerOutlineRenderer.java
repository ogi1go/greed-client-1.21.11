package greed.client.render;

import greed.client.GreedClient;
import greed.client.render.WorldProjection;
import greed.client.cosmetic.BedrockVertexCollector;
import greed.client.module.render.TagsModule;

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
import java.util.ArrayList;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.BufferAllocator.CloseableBuffer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

public final class PlayerOutlineRenderer {
   private static PlayerOutlineRenderer playerOutlineRenderer;
   private static final RenderPipeline pipeline2 = RenderPipeline.builder()
      .withLocation(GreedClient.id("pipeline/player_outline"))
      .withVertexShader(GreedClient.id("core/player_outline"))
      .withFragmentShader(GreedClient.id("core/player_outline"))
      .withVertexFormat(VertexFormats.POSITION, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .withSampler("Sampler0")
      .withSampler("Sampler1")
      .withUniform("PlayerOutlineParams", UniformType.UNIFORM_BUFFER)
      .build();
   private static final RenderPipeline pipeline3 = RenderPipeline.builder()
      .withLocation(GreedClient.id("pipeline/player_outline_blur"))
      .withVertexShader(GreedClient.id("core/player_outline_blur"))
      .withFragmentShader(GreedClient.id("core/player_outline_blur"))
      .withVertexFormat(VertexFormats.POSITION, DrawMode.QUADS)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("PlayerOutlineParams", UniformType.UNIFORM_BUFFER)
      .build();
   private static final RenderPipeline pipeline4 = RenderPipeline.builder()
      .withLocation(GreedClient.id("pipeline/hitbox_mask"))
      .withVertexShader(GreedClient.id("core/hitbox_mask"))
      .withFragmentShader(GreedClient.id("core/hitbox_mask"))
      .withVertexFormat(VertexFormats.POSITION, DrawMode.QUADS)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .withUniform("HitboxMaskParams", UniformType.UNIFORM_BUFFER)
      .build();
   private static final RenderPipeline pipeline5 = RenderPipeline.builder()
      .withLocation(GreedClient.id("pipeline/cosmetic_mask"))
      .withVertexShader(GreedClient.id("core/cosmetic_mask"))
      .withFragmentShader(GreedClient.id("core/cosmetic_mask"))
      .withVertexFormat(VertexFormats.POSITION_TEXTURE, DrawMode.QUADS)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("HitboxMaskParams", UniformType.UNIFORM_BUFFER)
      .build();
   private static final int[][] intValArray = new int[][]{{0, 1, 3, 2}, {4, 6, 7, 5}, {0, 4, 5, 1}, {2, 3, 7, 6}, {0, 2, 6, 4}, {1, 5, 7, 3}};
   private SimpleFramebuffer framebuffer3;
   private GpuBuffer gpuBuffer;
   private GpuBuffer gpuBuffer2;
   private GpuBuffer gpuBuffer3;
   private GpuBuffer gpuBuffer4;
   private GpuBuffer gpuBuffer5;
   private BufferAllocator bufferAllocator2;
   private BufferAllocator bufferAllocator3;
   private final ArrayList<Box> list = new ArrayList<>();
   private final Matrix4f matrix4f = new Matrix4f();
   private final Vector4f vector4f = new Vector4f();
   private boolean flag;
   private boolean flag2;
   private float floatVal;
   private float floatVal2;
   private float floatVal3;
   private float floatVal4;
   private boolean flag3;
   private float floatVal5;
   private float floatVal6;
   private float floatVal7;
   private float floatVal8;

   public void updateState() {
      if (this.framebuffer3 != null) {
         this.framebuffer3.delete();
         this.framebuffer3 = null;
      }

      if (this.bufferAllocator2 != null) {
         this.bufferAllocator2.close();
         this.bufferAllocator2 = null;
      }

      if (this.bufferAllocator3 != null) {
         this.bufferAllocator3.close();
         this.bufferAllocator3 = null;
      }

      this.list.clear();
      this.flag = false;
      this.flag2 = false;
      if (this.gpuBuffer != null && !this.gpuBuffer.isClosed()) {
         this.gpuBuffer.close();
      }

      this.gpuBuffer = null;
      if (this.gpuBuffer2 != null && !this.gpuBuffer2.isClosed()) {
         this.gpuBuffer2.close();
      }

      this.gpuBuffer2 = null;
      if (this.gpuBuffer3 != null && !this.gpuBuffer3.isClosed()) {
         this.gpuBuffer3.close();
      }

      this.gpuBuffer3 = null;
      if (this.gpuBuffer4 != null && !this.gpuBuffer4.isClosed()) {
         this.gpuBuffer4.close();
      }

      this.gpuBuffer4 = null;
      if (this.gpuBuffer5 != null && !this.gpuBuffer5.isClosed()) {
         this.gpuBuffer5.close();
      }

      this.gpuBuffer5 = null;
      BedrockVertexCollector.updateState();
   }

   private PlayerOutlineRenderer() {
   }

   public void processData(Framebuffer framebuffer) {
      int local7385_2 = BedrockVertexCollector.computeInt4();
      this.flag = false;
      this.flag2 = false;
      if (local7385_2 != 0 && framebuffer != null && framebuffer.getColorAttachmentView() != null && TagsModule.getInstance().checkState()) {
         if (this.gpuBuffer3 == null || this.gpuBuffer3.isClosed()) {
            this.gpuBuffer3 = RenderSystem.getDevice().createBuffer(() -> "greed_hitbox_mask_params", 136, 80L);
         }

         MemoryStack memoryStack = MemoryStack.stackPush();

         try {
            ByteBuffer byteBuffer = memoryStack.calloc(80);
            this.matrix4f.set(WorldProjection.getMatrix4f()).mul(WorldProjection.getMatrix4f2()).get(0, byteBuffer);
            byteBuffer.putFloat(64, 1.0F);
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.gpuBuffer3.slice(0L, 80L), byteBuffer);
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

         if (this.bufferAllocator3 == null) {
            this.bufferAllocator3 = new BufferAllocator(Math.max(256, local7385_2) * 4 * VertexFormats.POSITION_TEXTURE.getVertexSize());
         }

         CloseableBuffer local7385_3x = this.bufferAllocator3.getAllocated();
         if (local7385_3x != null) {
            local7385_3x.close();
         }

         BufferBuilder bufferBuilder = new BufferBuilder(this.bufferAllocator3, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);

         for (int index = 0; index < local7385_2 * 4; index++) {
            int local7385_6 = index * 5;
            bufferBuilder.vertex(
                  BedrockVertexCollector.computeFloat(local7385_6), BedrockVertexCollector.computeFloat(local7385_6 + 1), BedrockVertexCollector.computeFloat(local7385_6 + 2)
               )
               .texture(BedrockVertexCollector.computeFloat(local7385_6 + 3), BedrockVertexCollector.computeFloat(local7385_6 + 4));
            this.processData4(
               BedrockVertexCollector.computeFloat(local7385_6), BedrockVertexCollector.computeFloat(local7385_6 + 1), BedrockVertexCollector.computeFloat(local7385_6 + 2)
            );
         }

         try (BuiltBuffer builtBuffer = bufferBuilder.end()) {
            if (this.gpuBuffer5 != null && !this.gpuBuffer5.isClosed() && this.gpuBuffer5.size() >= builtBuffer.getBuffer().remaining()) {
               RenderSystem.getDevice()
                  .createCommandEncoder()
                  .writeToBuffer(this.gpuBuffer5.slice(0L, builtBuffer.getBuffer().remaining()), builtBuffer.getBuffer());
            } else {
               if (this.gpuBuffer5 != null && !this.gpuBuffer5.isClosed()) {
                  this.gpuBuffer5.close();
               }

               this.gpuBuffer5 = RenderSystem.getDevice().createBuffer(() -> "greed_cosmetic_mask_mesh", 40, builtBuffer.getBuffer());
            }

            for (int index2 = 0; index2 < BedrockVertexCollector.computeInt3(); index2++) {
               if (BedrockVertexCollector.computeInt2(index2) != 0) {
                  GpuTextureView glTextureView = MinecraftClient.getInstance()
                     .getTextureManager()
                     .getTexture(BedrockVertexCollector.getIdentifier(index2))
                     .getGlTextureView();
                  if (glTextureView != null) {
                     try (RenderPass renderPass = RenderSystem.getDevice()
                           .createCommandEncoder()
                           .createRenderPass(
                              () -> "greed_cosmetic_mask", framebuffer.getColorAttachmentView(), OptionalInt.empty(), null, OptionalDouble.empty()
                           )) {
                        renderPass.setPipeline(pipeline5);
                        renderPass.bindTexture("Sampler0", glTextureView, RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
                        renderPass.setUniform("HitboxMaskParams", this.gpuBuffer3);
                        renderPass.setVertexBuffer(0, this.gpuBuffer5);
                        renderPass.setIndexBuffer(
                           RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexBuffer(local7385_2 * 6),
                           RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexType()
                        );
                        renderPass.drawIndexed(0, BedrockVertexCollector.computeInt(index2) * 6, BedrockVertexCollector.computeInt2(index2) * 6, 1);
                     }
                  }
               }
            }
         }

         BedrockVertexCollector.updateState();
      } else {
         BedrockVertexCollector.updateState();
      }
   }

   public void processData2(Framebuffer framebuffer, boolean local7387_2) {
      if (framebuffer != null
         && framebuffer.getColorAttachmentView() != null
         && MinecraftClient.getInstance().world != null
         && MinecraftClient.getInstance().gameRenderer != null) {
         ArrayList<Box> local7387_3 = this.list;
         local7387_3.clear();

         for (Entity entity : MinecraftClient.getInstance().world.getEntities()) {
            if (entity instanceof AbstractClientPlayerEntity abstractClientPlayerEntity
               && (abstractClientPlayerEntity != MinecraftClient.getInstance().player || !MinecraftClient.getInstance().options.getPerspective().isFirstPerson())
               && TagsModule.getInstance().matchesCriteria5(abstractClientPlayerEntity)) {
               local7387_3.add(
                  abstractClientPlayerEntity.getBoundingBox()
                     .offset(
                        abstractClientPlayerEntity.getLerpedPos(MinecraftClient.getInstance().getRenderTickCounter().getTickProgress(false))
                           .subtract(abstractClientPlayerEntity.getEntityPos())
                           .subtract(MinecraftClient.getInstance().gameRenderer.getCamera().getCameraPos())
                     )
               );
            }
         }

         if (!local7387_3.isEmpty()) {
            if (this.gpuBuffer3 == null || this.gpuBuffer3.isClosed()) {
               this.gpuBuffer3 = RenderSystem.getDevice().createBuffer(() -> "greed_hitbox_mask_params", 136, 80L);
            }

            MemoryStack memoryStack = MemoryStack.stackPush();

            try {
               ByteBuffer byteBuffer = memoryStack.calloc(80);
               this.matrix4f.set(WorldProjection.getMatrix4f()).mul(WorldProjection.getMatrix4f2()).get(0, byteBuffer);
               byteBuffer.putFloat(64, 1.0F);
               RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.gpuBuffer3.slice(0L, 80L), byteBuffer);
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

            if (this.bufferAllocator2 == null) {
               this.bufferAllocator2 = new BufferAllocator(384 * VertexFormats.POSITION.getVertexSize());
            }

            CloseableBuffer local7387_4x = this.bufferAllocator2.getAllocated();
            if (local7387_4x != null) {
               local7387_4x.close();
            }

            BufferBuilder bufferBuilder = new BufferBuilder(this.bufferAllocator2, DrawMode.QUADS, VertexFormats.POSITION);

            for (Box box : local7387_3) {
               for (int[] intArray : intValArray) {
                  for (int local7387_15 : intArray) {
                     bufferBuilder.vertex(
                        (float)(local7387_15 >> 2 == 0 ? box.minX : box.maxX),
                        (float)((local7387_15 >> 1 & 1) == 0 ? box.minY : box.maxY),
                        (float)((local7387_15 & 1) == 0 ? box.minZ : box.maxZ)
                     );
                  }
               }
            }

            try (BuiltBuffer builtBuffer = bufferBuilder.end()) {
               if (this.gpuBuffer4 != null && !this.gpuBuffer4.isClosed() && this.gpuBuffer4.size() >= builtBuffer.getBuffer().remaining()) {
                  RenderSystem.getDevice()
                     .createCommandEncoder()
                     .writeToBuffer(this.gpuBuffer4.slice(0L, builtBuffer.getBuffer().remaining()), builtBuffer.getBuffer());
               } else {
                  if (this.gpuBuffer4 != null && !this.gpuBuffer4.isClosed()) {
                     this.gpuBuffer4.close();
                  }

                  this.gpuBuffer4 = RenderSystem.getDevice().createBuffer(() -> "greed_hitbox_mask_mesh", 40, builtBuffer.getBuffer());
               }

               try (RenderPass renderPass = RenderSystem.getDevice()
                     .createCommandEncoder()
                     .createRenderPass(
                        () -> "greed_hitbox_mask",
                        framebuffer.getColorAttachmentView(),
                        local7387_2 ? OptionalInt.of(0) : OptionalInt.empty(),
                        null,
                        OptionalDouble.empty()
                     )) {
                  renderPass.setPipeline(pipeline4);
                  renderPass.setUniform("HitboxMaskParams", this.gpuBuffer3);
                  renderPass.setVertexBuffer(0, this.gpuBuffer4);
                  renderPass.setIndexBuffer(
                     RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexBuffer(local7387_3.size() * 36),
                     RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexType()
                  );
                  renderPass.drawIndexed(0, 0, local7387_3.size() * 36, 1);
               }
            }
         }
      }
   }

   private void processData3(RenderPass renderPass, int[] intArray) {
      if (!this.flag3) {
         renderPass.enableScissor(intArray[0], intArray[1], intArray[2], intArray[3]);
      }

      renderPass.setVertexBuffer(0, this.gpuBuffer);
      renderPass.setIndexBuffer(
         RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexBuffer(6), RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexType()
      );
      renderPass.drawIndexed(0, 0, 6, 1);
   }

   private void processData4(float local7394_1, float local7394_2, float local7394_3) {
      if (!this.flag2) {
         this.vector4f.set(local7394_1, local7394_2, local7394_3, 1.0F);
         this.matrix4f.transform(this.vector4f);
         if (this.vector4f.w <= 0.05F) {
            this.flag2 = true;
         } else {
            float local7394_4 = this.vector4f.x / this.vector4f.w;
            float local7394_5 = this.vector4f.y / this.vector4f.w;
            if (!this.flag) {
               this.floatVal = this.floatVal3 = local7394_4;
               this.floatVal2 = this.floatVal4 = local7394_5;
               this.flag = true;
            } else {
               this.floatVal = Math.min(this.floatVal, local7394_4);
               this.floatVal3 = Math.max(this.floatVal3, local7394_4);
               this.floatVal2 = Math.min(this.floatVal2, local7394_5);
               this.floatVal4 = Math.max(this.floatVal4, local7394_5);
            }
         }
      }
   }

   private int[] computeInt(int local7395_1, int local7395_2, int local7395_3) {
      if (this.flag3) {
         return new int[]{0, 0, local7395_2, local7395_3};
      }

      float local7395_4 = Math.max(Math.min(this.floatVal5, 4.0F), -4.0F);
      float local7395_5 = Math.max(Math.min(this.floatVal7, 4.0F), -4.0F);
      float local7395_6 = Math.max(Math.min(this.floatVal6, 4.0F), -4.0F);
      float local7395_7 = Math.max(Math.min(this.floatVal8, 4.0F), -4.0F);
      int local7395_8 = Math.max(0, (int)Math.floor((local7395_4 * 0.5F + 0.5F) * local7395_2) - local7395_1);
      int local7395_9 = Math.min(local7395_2, (int)Math.ceil((local7395_5 * 0.5F + 0.5F) * local7395_2) + local7395_1);
      int local7395_10 = Math.max(0, (int)Math.floor((local7395_6 * 0.5F + 0.5F) * local7395_3) - local7395_1);
      int local7395_11 = Math.min(local7395_3, (int)Math.ceil((local7395_7 * 0.5F + 0.5F) * local7395_3) + local7395_1);
      return local7395_9 > local7395_8 && local7395_11 > local7395_10
         ? new int[]{local7395_8, local7395_10, local7395_9 - local7395_8, local7395_11 - local7395_10}
         : null;
   }

   public static PlayerOutlineRenderer getInstance() {
      if (playerOutlineRenderer == null) {
         playerOutlineRenderer = new PlayerOutlineRenderer();
      }

      return playerOutlineRenderer;
   }

   private boolean checkState() {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      this.flag3 = this.flag2;
      if (this.flag3) {
         return true;
      }

      boolean local7400_2 = this.flag;
      if (local7400_2) {
         this.floatVal5 = this.floatVal;
         this.floatVal7 = this.floatVal3;
         this.floatVal6 = this.floatVal2;
         this.floatVal8 = this.floatVal4;
      }

      if (minecraftClient.world != null && minecraftClient.gameRenderer != null) {
         this.matrix4f.set(WorldProjection.getMatrix4f()).mul(WorldProjection.getMatrix4f2());
         float tickProgress = minecraftClient.getRenderTickCounter().getTickProgress(false);
         double local7400_4 = minecraftClient.gameRenderer.getCamera().getCameraPos().x;
         double local7400_6 = minecraftClient.gameRenderer.getCamera().getCameraPos().y;
         double local7400_8 = minecraftClient.gameRenderer.getCamera().getCameraPos().z;

         for (Entity entity : minecraftClient.world.getEntities()) {
            if (entity instanceof AbstractClientPlayerEntity abstractClientPlayerEntity
               && (abstractClientPlayerEntity != minecraftClient.player || !minecraftClient.options.getPerspective().isFirstPerson())
               && TagsModule.getInstance().matchesCriteria5(abstractClientPlayerEntity)) {
               Box boundingBox = abstractClientPlayerEntity.getBoundingBox();
               double local7400_14 = abstractClientPlayerEntity.getLerpedPos(tickProgress).x - abstractClientPlayerEntity.getEntityPos().x - local7400_4;
               double local7400_16 = abstractClientPlayerEntity.getLerpedPos(tickProgress).y - abstractClientPlayerEntity.getEntityPos().y - local7400_6;
               double local7400_18 = abstractClientPlayerEntity.getLerpedPos(tickProgress).z - abstractClientPlayerEntity.getEntityPos().z - local7400_8;
               float local7400_20 = (float)(boundingBox.minX + local7400_14 - 2.5);
               float local7400_21 = (float)(boundingBox.maxX + local7400_14 + 2.5);
               float local7400_22 = (float)(boundingBox.minY + local7400_16 - 2.0);
               float local7400_23 = (float)(boundingBox.maxY + local7400_16 + 2.0);
               float local7400_24 = (float)(boundingBox.minZ + local7400_18 - 2.5);
               float local7400_25 = (float)(boundingBox.maxZ + local7400_18 + 2.5);

               for (int index = 0; index < 8; index++) {
                  this.vector4f
                     .set(
                        (index & 4) == 0 ? local7400_20 : local7400_21,
                        (index & 2) == 0 ? local7400_22 : local7400_23,
                        (index & 1) == 0 ? local7400_24 : local7400_25,
                        1.0F
                     );
                  this.matrix4f.transform(this.vector4f);
                  if (this.vector4f.w <= 0.05F) {
                     this.flag3 = true;
                     return true;
                  }

                  float local7400_27 = this.vector4f.x / this.vector4f.w;
                  float local7400_28 = this.vector4f.y / this.vector4f.w;
                  if (!local7400_2) {
                     this.floatVal5 = this.floatVal7 = local7400_27;
                     this.floatVal6 = this.floatVal8 = local7400_28;
                     local7400_2 = true;
                  } else {
                     this.floatVal5 = Math.min(this.floatVal5, local7400_27);
                     this.floatVal7 = Math.max(this.floatVal7, local7400_27);
                     this.floatVal6 = Math.min(this.floatVal6, local7400_28);
                     this.floatVal8 = Math.max(this.floatVal8, local7400_28);
                  }
               }
            }
         }

         return local7400_2;
      } else {
         this.flag3 = true;
         return true;
      }
   }

   public void processData5(Framebuffer framebuffer) {
      if (TagsModule.getInstance().checkState()) {
         Framebuffer framebuffer2 = MinecraftClient.getInstance().getFramebuffer();
         if (framebuffer2 != null && framebuffer2.getColorAttachmentView() != null && framebuffer != null && framebuffer.getColorAttachmentView() != null) {
            if (framebuffer.textureWidth == framebuffer2.textureWidth && framebuffer.textureHeight == framebuffer2.textureHeight) {
               this.flag3 = true;
               this.flag = false;
               this.flag2 = false;
               int[] intArray = this.computeInt(50, framebuffer2.textureWidth, framebuffer2.textureHeight);
               if (intArray != null) {
                  int[] intArray2 = this.computeInt(100, framebuffer2.textureWidth, framebuffer2.textureHeight);
                  if (intArray2 != null) {
                     if (this.gpuBuffer == null || this.gpuBuffer.isClosed()) {
                        try (BufferAllocator bufferAllocator = new BufferAllocator(4 * VertexFormats.POSITION.getVertexSize())) {
                           BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION);
                           bufferBuilder.vertex(-1.0F, -1.0F, 0.0F);
                           bufferBuilder.vertex(-1.0F, 1.0F, 0.0F);
                           bufferBuilder.vertex(1.0F, 1.0F, 0.0F);
                           bufferBuilder.vertex(1.0F, -1.0F, 0.0F);

                           try (BuiltBuffer builtBuffer = bufferBuilder.end()) {
                              this.gpuBuffer = RenderSystem.getDevice().createBuffer(() -> "greed_player_outline_quad", 32, builtBuffer.getBuffer());
                           }
                        }
                     }

                     if (this.gpuBuffer2 == null || this.gpuBuffer2.isClosed()) {
                        this.gpuBuffer2 = RenderSystem.getDevice().createBuffer(() -> "greed_player_outline_params", 136, 48L);
                     }

                     MemoryStack memoryStack = MemoryStack.stackPush();

                     try {
                        ByteBuffer byteBuffer = memoryStack.calloc(48);
                        byteBuffer.putFloat(0, (TagsModule.getInstance().computeInt2() >> 16 & 0xFF) / 255.0F);
                        byteBuffer.putFloat(4, (TagsModule.getInstance().computeInt2() >> 8 & 0xFF) / 255.0F);
                        byteBuffer.putFloat(8, (TagsModule.getInstance().computeInt2() & 0xFF) / 255.0F);
                        byteBuffer.putFloat(12, TagsModule.getInstance().fill.getValue());
                        byteBuffer.putFloat(24, TagsModule.getInstance().outline.getValue());
                        byteBuffer.putFloat(28, TagsModule.getInstance().highlight.getValue());
                        byteBuffer.putFloat(32, 1.0F / framebuffer2.textureWidth);
                        byteBuffer.putFloat(36, 1.0F / framebuffer2.textureHeight);
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

                     if (this.framebuffer3 == null) {
                        this.framebuffer3 = new SimpleFramebuffer("greed_player_outline_blur", framebuffer2.textureWidth, framebuffer2.textureHeight, false);
                     }

                     if (this.framebuffer3.textureWidth != framebuffer2.textureWidth || this.framebuffer3.textureHeight != framebuffer2.textureHeight) {
                        this.framebuffer3.resize(framebuffer2.textureWidth, framebuffer2.textureHeight);
                     }

                     try (RenderPass local7401_5x = RenderSystem.getDevice()
                           .createCommandEncoder()
                           .createRenderPass(
                              () -> "greed_player_outline_blur", this.framebuffer3.getColorAttachmentView(), OptionalInt.empty(), null, OptionalDouble.empty()
                           )) {
                        local7401_5x.setPipeline(pipeline3);
                        local7401_5x.bindTexture("Sampler0", framebuffer.getColorAttachmentView(), RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
                        local7401_5x.setUniform("PlayerOutlineParams", this.gpuBuffer2);
                        this.processData3(local7401_5x, intArray2);
                     }

                     try (RenderPass local7401_5x = RenderSystem.getDevice()
                           .createCommandEncoder()
                           .createRenderPass(
                              () -> "greed_player_outline", framebuffer2.getColorAttachmentView(), OptionalInt.empty(), null, OptionalDouble.empty()
                           )) {
                        local7401_5x.setPipeline(pipeline2);
                        local7401_5x.bindTexture("Sampler0", this.framebuffer3.getColorAttachmentView(), RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
                        local7401_5x.bindTexture("Sampler1", framebuffer.getColorAttachmentView(), RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
                        local7401_5x.setUniform("PlayerOutlineParams", this.gpuBuffer2);
                        this.processData3(local7401_5x, intArray);
                     }
                  }
               }
            }
         }
      }
   }
}
