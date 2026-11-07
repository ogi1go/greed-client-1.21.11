package greed.client.render;

import greed.mixin.DrawContextMixin;
import greed.client.util.GuiScale;
import greed.client.render.SoftGlowDrawData;
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

public final class SoftGlowRenderer implements AutoCloseable {
   private static volatile SoftGlowRenderer softGlowRenderer;
   public static final RenderPipeline pipeline2 = RenderPipeline.builder()
      .withLocation(getIdentifier("pipeline/soft_glow"))
      .withVertexShader(getIdentifier("core/soft_glow"))
      .withFragmentShader(getIdentifier("core/soft_glow"))
      .withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, DrawMode.QUADS)
      .withBlend(BlendFunction.LIGHTNING)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("SoftGlowParams", UniformType.UNIFORM_BUFFER)
      .build();
   private final List<SoftGlowDrawData> list = new ArrayList<>(64);
   private DrawContext drawContext2;
   private GpuBuffer gpuBuffer2;
   private boolean flag = true;

   public static void updateState() {
      SoftGlowRenderer currentSoftGlowRenderer = softGlowRenderer;
      if (currentSoftGlowRenderer != null) {
         currentSoftGlowRenderer.close();
         softGlowRenderer = null;
      }
   }

   public static SoftGlowRenderer getSoftGlowRenderer() {
      SoftGlowRenderer currentSoftGlowRenderer = softGlowRenderer;
      if (currentSoftGlowRenderer == null) {
         synchronized (SoftGlowRenderer.class) {
            currentSoftGlowRenderer = softGlowRenderer;
            if (currentSoftGlowRenderer == null) {
               currentSoftGlowRenderer = new SoftGlowRenderer();
               softGlowRenderer = currentSoftGlowRenderer;
            }
         }
      }

      return currentSoftGlowRenderer;
   }

   public boolean matchesCriteria(RenderPipeline renderPipeline) {
      return renderPipeline == pipeline2;
   }

   public void updateState2() {
      this.list.clear();
      this.flag = false;
   }

   private void renderElement(DrawContext drawContext, SoftGlowDrawData softGlowDrawData) {
      if (drawContext != null && softGlowDrawData != null && softGlowDrawData.visible()) {
         try {
            Matrix3x2f matrix3x2f = GuiScale.renderElement(drawContext);
            ((DrawContextMixin)drawContext)
               .getGuiRenderState()
               .addSimpleElement(new SoftGlowRenderState(matrix3x2f, softGlowDrawData, ScissorStack.getScreenRect()));
         } catch (RuntimeException exception) {
         }
      }
   }

   private static Identifier getIdentifier(String local3912_0) {
      return Identifier.of("greed", local3912_0);
   }

   public void renderElement2(DrawContext drawContext) {
      this.drawContext2 = drawContext;
   }

   private ByteBuffer getByteBuffer(MemoryStack memoryStack, List<SoftGlowDrawData> local3914_2) {
      ByteBuffer byteBuffer = memoryStack.calloc(Math.min(10240, local3914_2.size() * 5 * 4 * 4));

      for (int index = 0; index < local3914_2.size(); index++) {
         SoftGlowDrawData softGlowDrawData = local3914_2.get(index);
         int local3914_6 = index * 5 * 4 * 4;
         float local3914_7 = Math.max(softGlowDrawData.width() / 2.0F, 0.0F);
         float local3914_8 = Math.max(softGlowDrawData.height() / 2.0F, 0.0F);
         float local3914_9 = Math.min(local3914_7, local3914_8);
         byteBuffer.putFloat(local3914_6, local3914_7);
         byteBuffer.putFloat(local3914_6 + 4, local3914_8);
         byteBuffer.putFloat(local3914_6 + 8, softGlowDrawData.pad());
         byteBuffer.putFloat(local3914_6 + 12, Math.max(softGlowDrawData.softness(), 1.0E-4F));
         byteBuffer.putFloat(local3914_6 + 16, computeFloat(softGlowDrawData.radiusTopLeft(), 0.0F, local3914_9));
         byteBuffer.putFloat(local3914_6 + 20, computeFloat(softGlowDrawData.radiusTopRight(), 0.0F, local3914_9));
         byteBuffer.putFloat(local3914_6 + 24, computeFloat(softGlowDrawData.radiusBottomRight(), 0.0F, local3914_9));
         byteBuffer.putFloat(local3914_6 + 28, computeFloat(softGlowDrawData.radiusBottomLeft(), 0.0F, local3914_9));
         int local3914_10 = softGlowDrawData.color();
         float local3914_11 = (local3914_10 >>> 24 & 0xFF) / 255.0F * Math.max(softGlowDrawData.intensity(), 0.0F);
         byteBuffer.putFloat(local3914_6 + 32, (local3914_10 >>> 16 & 0xFF) / 255.0F);
         byteBuffer.putFloat(local3914_6 + 36, (local3914_10 >>> 8 & 0xFF) / 255.0F);
         byteBuffer.putFloat(local3914_6 + 40, (local3914_10 & 0xFF) / 255.0F);
         byteBuffer.putFloat(local3914_6 + 44, local3914_11);
         byteBuffer.putFloat(local3914_6 + 48, softGlowDrawData.dirX());
         byteBuffer.putFloat(local3914_6 + 52, softGlowDrawData.dirY());
         byteBuffer.putFloat(local3914_6 + 56, computeFloat(softGlowDrawData.dirStrength(), 0.0F, 1.0F));
         byteBuffer.putFloat(local3914_6 + 60, softGlowDrawData.mode());
         byteBuffer.putFloat(local3914_6 + 64, Math.max(softGlowDrawData.falloff(), 0.25F));
         byteBuffer.putFloat(local3914_6 + 68, 0.0F);
         byteBuffer.putFloat(local3914_6 + 72, 0.0F);
         byteBuffer.putFloat(local3914_6 + 76, 0.0F);
      }

      byteBuffer.position(0);
      return byteBuffer;
   }

   private SoftGlowRenderer() {
   }

   public void processData(RenderPass renderPass) {
      if (renderPass != null && !this.list.isEmpty()) {
         GpuBuffer gpuBuffer = this.getGpuBuffer();
         if (gpuBuffer != null) {
            renderPass.setUniform("SoftGlowParams", gpuBuffer);
         }
      }
   }

   public void updateState3() {
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

   private GpuBuffer getGpuBuffer() {
      if (!this.flag && this.gpuBuffer2 != null) {
         return this.gpuBuffer2;
      }

      this.updateState3();
      if (!this.flag && this.gpuBuffer2 != null) {
         return this.gpuBuffer2;
      }

      this.updateState5();
      this.updateState3();
      return this.gpuBuffer2;
   }

   public void updateState4() {
      this.drawContext2 = null;
   }

   @Override
   public void close() {
      this.list.clear();
      this.drawContext2 = null;
      this.updateState5();
   }

   private GpuBuffer getGpuBuffer2() {
      if (this.gpuBuffer2 != null && !this.gpuBuffer2.isClosed() && this.gpuBuffer2.size() >= 10240L) {
         return this.gpuBuffer2;
      }

      this.updateState5();

      try {
         this.gpuBuffer2 = RenderSystem.getDevice().createBuffer(() -> "greed_soft_glow_params", 136, 10240L);
         return this.gpuBuffer2;
      } catch (RuntimeException exception) {
         return null;
      }
   }

   public void processData2(SoftGlowDrawData softGlowDrawData) {
      this.renderElement(this.drawContext2, softGlowDrawData);
   }

   private static float computeFloat(float local3923_0, float local3923_1, float local3923_2) {
      return Math.max(local3923_1, Math.min(local3923_2, local3923_0));
   }

   int computeInt(SoftGlowDrawData softGlowDrawData) {
      int local3924_2 = this.list.size();
      if (local3924_2 >= 128) {
         return -1;
      }

      this.list.add(softGlowDrawData);
      this.flag = true;
      return local3924_2;
   }

   private void updateState5() {
      if (this.gpuBuffer2 != null) {
         this.gpuBuffer2.close();
         this.gpuBuffer2 = null;
      }
   }

   public void renderElement3(DrawContext drawContext, SoftGlowDrawData softGlowDrawData) {
      this.renderElement2(drawContext);
      this.processData2(softGlowDrawData);
      this.updateState4();
   }
}
