package greed.client.render;

import greed.client.GreedClient;
import greed.mixin.DrawContextMixin;
import greed.client.util.GuiScale;
import greed.client.render.WarpTextDrawData;
import greed.client.render.ScissorStack;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryStack;

public final class WarpTextRenderer implements AutoCloseable {
   private static volatile WarpTextRenderer warpTextRenderer;
   private static final VertexFormat vertexFormat = VertexFormat.builder()
      .add("Position", VertexFormatElement.POSITION)
      .add("UV0", VertexFormatElement.UV0)
      .add("Color", VertexFormatElement.COLOR)
      .add("LineWidth", VertexFormatElement.LINE_WIDTH)
      .build();
   public static final RenderPipeline pipeline2 = RenderPipeline.builder()
      .withLocation(getIdentifier("pipeline/warp_text"))
      .withVertexShader(getIdentifier("core/warp_text"))
      .withFragmentShader(getIdentifier("core/warp_text"))
      .withVertexFormat(vertexFormat, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("WarpTextParams", UniformType.UNIFORM_BUFFER)
      .withSampler("Sampler0")
      .build();
   private final List<WarpTextDrawData> list = new ArrayList<>(2);
   private DrawContext drawContext2;
   private GpuBuffer gpuBuffer2;
   private boolean flag;

   public void renderElement(DrawContext drawContext) {
      this.drawContext2 = drawContext;
   }

   public boolean matchesCriteria(RenderPipeline renderPipeline) {
      return renderPipeline == pipeline2;
   }

   public void processData(RenderPass renderPass) {
      if (renderPass != null && !this.list.isEmpty()) {
         GpuBuffer gpuBuffer = this.getGpuBuffer();
         if (gpuBuffer != null) {
            renderPass.setUniform("WarpTextParams", gpuBuffer);
         }
      }
   }

   private void updateState() {
      if (this.gpuBuffer2 != null) {
         this.gpuBuffer2.close();
         this.gpuBuffer2 = null;
      }
   }

   private WarpTextRenderer() {
   }

   public void updateState2() {
      this.list.clear();
      this.flag = false;
   }

   @Override
   public void close() {
      this.list.clear();
      this.updateState();
      this.drawContext2 = null;
   }

   public void processData2(WarpTextDrawData warpTextDrawData) {
      if (this.drawContext2 != null && warpTextDrawData != null && warpTextDrawData.glyphQuads().length != 0) {
         try {
            TextureSetup textureSetup = this.getTextureSetup(warpTextDrawData.atlasTexture());
            if (textureSetup == null) {
               return;
            }

            GuiRenderState guiRenderState = ((DrawContextMixin)(Object)this.drawContext2).getGuiRenderState();
            guiRenderState.addSimpleElement(
               new WarpTextRenderState(GuiScale.renderElement(this.drawContext2), textureSetup, ScissorStack.getScreenRect(), warpTextDrawData)
            );
         } catch (RuntimeException exception) {
            GreedClient.logger().warn("[WarpTextRenderer] submit failed", exception);
         }
      }
   }

   private GpuBuffer getGpuBuffer() {
      if (!this.flag && this.gpuBuffer2 != null) {
         return this.gpuBuffer2;
      }

      this.updateState4();
      if (!this.flag && this.gpuBuffer2 != null) {
         return this.gpuBuffer2;
      }

      this.updateState();
      MemoryStack memoryStack = MemoryStack.stackPush();

      GpuBuffer gpuBuffer;
      try {
         this.gpuBuffer2 = RenderSystem.getDevice().createBuffer(() -> "greed_warp_text_params", 128, this.getByteBuffer(memoryStack));
         this.flag = false;
         gpuBuffer = this.gpuBuffer2;
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

      return gpuBuffer;
   }

   public static void updateState3() {
      WarpTextRenderer currentWarpTextRenderer = warpTextRenderer;
      if (currentWarpTextRenderer != null) {
         currentWarpTextRenderer.close();
         warpTextRenderer = null;
      }
   }

   private ByteBuffer getByteBuffer(MemoryStack memoryStack) {
      ByteBuffer byteBuffer = memoryStack.calloc(4096);
      float local1086_3 = (float)(System.nanoTime() % 1000000000000L) / 1.0E9F;

      for (int index = 0; index < this.list.size(); index++) {
         int local1086_5 = index * 64 * 4 * 4;
         int local1086_6 = Math.min(30, this.list.get(index).glyphQuads().length / 8);
         byteBuffer.putFloat(local1086_5, local1086_6);
         byteBuffer.putFloat(local1086_5 + 4, this.list.get(index).pxRange());
         byteBuffer.putFloat(local1086_5 + 8, this.list.get(index).influenceRadiusPx());
         byteBuffer.putFloat(local1086_5 + 12, this.list.get(index).cellPx());
         byteBuffer.putFloat(local1086_5 + 16, this.list.get(index).mouseLocalX());
         byteBuffer.putFloat(local1086_5 + 20, this.list.get(index).mouseLocalY());
         byteBuffer.putFloat(local1086_5 + 24, this.list.get(index).width());
         byteBuffer.putFloat(local1086_5 + 28, this.list.get(index).height());
         byteBuffer.putFloat(local1086_5 + 32, local1086_3);

         for (int index2 = 0; index2 < local1086_6; index2++) {
            int local1086_8 = local1086_5 + (3 + index2 * 2) * 4 * 4;

            for (int index3 = 0; index3 < 8; index3++) {
               byteBuffer.putFloat(local1086_8 + index3 * 4, this.list.get(index).glyphQuads()[index2 * 8 + index3]);
            }
         }
      }

      byteBuffer.position(0);
      return byteBuffer;
   }

   int computeInt(WarpTextDrawData warpTextDrawData) {
      if (this.list.size() >= 4) {
         return -1;
      }

      this.list.add(warpTextDrawData);
      this.flag = true;
      return this.list.size() - 1;
   }

   public static WarpTextRenderer getWarpTextRenderer() {
      WarpTextRenderer currentWarpTextRenderer = warpTextRenderer;
      if (currentWarpTextRenderer == null) {
         synchronized (WarpTextRenderer.class) {
            currentWarpTextRenderer = warpTextRenderer;
            if (currentWarpTextRenderer == null) {
               currentWarpTextRenderer = new WarpTextRenderer();
               warpTextRenderer = currentWarpTextRenderer;
            }
         }
      }

      return currentWarpTextRenderer;
   }

   public void updateState4() {
      if (!this.list.isEmpty() && this.flag) {
         GpuBuffer gpuBuffer = this.getGpuBuffer2();
         if (gpuBuffer != null) {
            try {
               MemoryStack memoryStack = MemoryStack.stackPush();

               try {
                  ByteBuffer byteBuffer = this.getByteBuffer(memoryStack);
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

   public void updateState5() {
      this.drawContext2 = null;
   }

   private static Identifier getIdentifier(String local1092_0) {
      return Identifier.of("greed", local1092_0);
   }

   private GpuBuffer getGpuBuffer2() {
      if (this.gpuBuffer2 != null && !this.gpuBuffer2.isClosed() && this.gpuBuffer2.size() >= 4096L) {
         return this.gpuBuffer2;
      }

      this.updateState();

      try {
         this.gpuBuffer2 = RenderSystem.getDevice().createBuffer(() -> "greed_warp_text_params", 136, 4096L);
         return this.gpuBuffer2;
      } catch (RuntimeException exception) {
         return null;
      }
   }

   private TextureSetup getTextureSetup(Identifier identifier) {
      if (MinecraftClient.getInstance() != null && MinecraftClient.getInstance().getTextureManager() != null) {
         AbstractTexture texture = MinecraftClient.getInstance().getTextureManager().getTexture(identifier);
         return texture != null && texture.getGlTextureView() != null
            ? TextureSetup.of(texture.getGlTextureView(), RenderSystem.getSamplerCache().get(FilterMode.LINEAR))
            : null;
      } else {
         return null;
      }
   }
}
