package greed.client.module.render;

import greed.client.GreedClient;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.setting.NumberSetting;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.world.ClientWorld;
import org.lwjgl.system.MemoryStack;

@ModuleInfo(name = "MotionBlur", description = "Смазывает картинку шлейфом из предыдущих кадров.", category = ModuleCategory.RENDER)
public final class MotionBlurModule extends Module {
   private static MotionBlurModule motionBlurModule;
   private static final RenderPipeline pipeline2 = RenderPipeline.builder()
      .withLocation(GreedClient.id("pipeline/motion_blur"))
      .withVertexShader(GreedClient.id("core/motion_blur"))
      .withFragmentShader(GreedClient.id("core/motion_blur"))
      .withVertexFormat(VertexFormats.POSITION, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("MotionBlurParams", UniformType.UNIFORM_BUFFER)
      .build();
   public NumberSetting numberSetting = this.addSetting(
      new NumberSetting("Сила", "Насколько долго тянется шлейф за кадром.").withRange(1.0F, 10.0F).withStep(1.0F).setValue(5.0F)
   );
   private SimpleFramebuffer framebuffer;
   private GpuBuffer gpuBuffer;
   private GpuBuffer gpuBuffer2;
   private boolean flag;
   private long timeMillis;
   private ClientWorld clientWorld;

   @Override
   protected void onEnable() {
      this.flag = false;
   }

   private MotionBlurModule() {
   }

   public static MotionBlurModule getInstance() {
      if (motionBlurModule == null) {
         motionBlurModule = new MotionBlurModule();
      }

      return motionBlurModule;
   }

   @Override
   protected void onDisable() {
      this.flag = false;
      if (this.framebuffer != null) {
         this.framebuffer.delete();
         this.framebuffer = null;
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

   public static void updateState() {
      if (motionBlurModule != null && motionBlurModule.isEnabled()) {
         motionBlurModule.updateState2();
      }
   }

   private void updateState2() {
      if (mc.world != null
         && mc.getFramebuffer() != null
         && mc.getFramebuffer().getColorAttachmentView() != null
         && mc.getFramebuffer().textureWidth > 0
         && mc.getFramebuffer().textureHeight > 0) {
         if (this.clientWorld != mc.world) {
            this.clientWorld = mc.world;
            this.flag = false;
         }

         if (this.framebuffer == null) {
            this.framebuffer = new SimpleFramebuffer(
               "greed_motion_blur_history", mc.getFramebuffer().textureWidth, mc.getFramebuffer().textureHeight, false
            );
            this.flag = false;
         }

         if (this.framebuffer.textureWidth != mc.getFramebuffer().textureWidth
            || this.framebuffer.textureHeight != mc.getFramebuffer().textureHeight) {
            this.framebuffer.resize(mc.getFramebuffer().textureWidth, mc.getFramebuffer().textureHeight);
            this.flag = false;
         }

         if (this.flag) {
            if (this.gpuBuffer == null || this.gpuBuffer.isClosed()) {
               try (BufferAllocator bufferAllocator = new BufferAllocator(4 * VertexFormats.POSITION.getVertexSize())) {
                  BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION);
                  bufferBuilder.vertex(-1.0F, -1.0F, 0.0F);
                  bufferBuilder.vertex(-1.0F, 1.0F, 0.0F);
                  bufferBuilder.vertex(1.0F, 1.0F, 0.0F);
                  bufferBuilder.vertex(1.0F, -1.0F, 0.0F);

                  try (BuiltBuffer builtBuffer = bufferBuilder.end()) {
                     this.gpuBuffer = RenderSystem.getDevice().createBuffer(() -> "greed_motion_blur_quad", 32, builtBuffer.getBuffer());
                  }
               }
            }

            if (this.gpuBuffer2 == null || this.gpuBuffer2.isClosed()) {
               this.gpuBuffer2 = RenderSystem.getDevice().createBuffer(() -> "greed_motion_blur_params", 136, 16L);
            }

            MemoryStack memoryStack = MemoryStack.stackPush();

            try {
               ByteBuffer byteBuffer = memoryStack.calloc(16);
               byteBuffer.putFloat(
                  0,
                  (float)Math.pow(
                     this.numberSetting.floatValue() / 100.0F * 0.9F, Math.max(1L, Math.min(100L, System.currentTimeMillis() - this.timeMillis)) / 50.0
                  )
               );
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

            try (RenderPass local6319_1x = RenderSystem.getDevice()
                  .createCommandEncoder()
                  .createRenderPass(
                     () -> "greed_motion_blur", mc.getFramebuffer().getColorAttachmentView(), OptionalInt.empty(), null, OptionalDouble.empty()
                  )) {
               local6319_1x.setPipeline(pipeline2);
               local6319_1x.bindTexture("Sampler0", this.framebuffer.getColorAttachmentView(), RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
               local6319_1x.setUniform("MotionBlurParams", this.gpuBuffer2);
               local6319_1x.setVertexBuffer(0, this.gpuBuffer);
               local6319_1x.setIndexBuffer(
                  RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexBuffer(6), RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexType()
               );
               local6319_1x.drawIndexed(0, 0, 6, 1);
            }
         }

         RenderSystem.getDevice()
            .createCommandEncoder()
            .copyTextureToTexture(
               mc.getFramebuffer().getColorAttachment(),
               this.framebuffer.getColorAttachment(),
               0,
               0,
               0,
               0,
               0,
               mc.getFramebuffer().textureWidth,
               mc.getFramebuffer().textureHeight
            );
         this.flag = true;
         this.timeMillis = System.currentTimeMillis();
      }
   }
}
