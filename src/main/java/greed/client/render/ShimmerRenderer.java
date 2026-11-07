package greed.client.render;

import greed.client.GreedClient;
import greed.mixin.DrawContextMixin;
import greed.client.util.GuiScale;
import greed.client.render.GuiLayerSerialAccess;
import greed.client.render.ArcDividerDrawData;
import greed.client.render.ScissorStack;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;
import org.lwjgl.system.MemoryStack;

public final class ShimmerRenderer implements AutoCloseable {
   private static volatile ShimmerRenderer shimmerRenderer;
   private static final VertexFormat vertexFormat = VertexFormat.builder()
      .add("Position", VertexFormatElement.POSITION)
      .add("UV0", VertexFormatElement.UV0)
      .add("Color", VertexFormatElement.COLOR)
      .add("LineWidth", VertexFormatElement.LINE_WIDTH)
      .build();
   public static final RenderPipeline pipeline2 = RenderPipeline.builder()
      .withLocation(getIdentifier("pipeline/shimmer"))
      .withVertexShader(getIdentifier("core/shimmer"))
      .withFragmentShader(getIdentifier("core/shimmer"))
      .withVertexFormat(vertexFormat, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("ShimmerParamsArray", UniformType.UNIFORM_BUFFER)
      .build();
   private final List<ArcDividerDrawData> list = new ArrayList<>(32);
   private final Map<ShimmerRenderer.DataRecord, ArcDividerRenderState> map = new LinkedHashMap<>(16);
   private DrawContext drawContext2;
   private GpuBuffer gpuBuffer2;
   private boolean flag;

   int computeInt(ArcDividerRenderState arcDividerRenderState) {
      int local5055_2 = this.list.size();
      if (local5055_2 >= 256) {
         return -1;
      }

      this.list.add(arcDividerRenderState.shimmer());
      this.flag = true;
      return local5055_2;
   }

   public void updateState() {
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

   public void processData(ArcDividerDrawData arcDividerDrawData) {
      if (this.drawContext2 != null && arcDividerDrawData != null) {
         try {
            GuiRenderState guiRenderState = ((DrawContextMixin)(Object)this.drawContext2).getGuiRenderState();
            int layerSerial = ((GuiLayerSerialAccess)guiRenderState).getLayerSerial();
            Matrix3x2f matrix3x2f = GuiScale.renderElement(this.drawContext2);
            ScreenRect screenRect = ScissorStack.getScreenRect();
            ShimmerRenderer.DataRecord local5057_6 = new ShimmerRenderer.DataRecord(guiRenderState, layerSerial, screenRect);
            ArcDividerRenderState arcDividerRenderState = this.map.get(local5057_6);
            if (arcDividerRenderState == null) {
               arcDividerRenderState = new ArcDividerRenderState(matrix3x2f, arcDividerDrawData, screenRect);
               this.map.put(local5057_6, arcDividerRenderState);
               guiRenderState.addSimpleElement(arcDividerRenderState);
            }
         } catch (RuntimeException exception) {
            GreedClient.logger().warn("[ShimmerRenderer] submit failed", exception);
         }
      }
   }

   public void renderElement(DrawContext drawContext) {
      if (this.drawContext2 != drawContext) {
         this.map.clear();
      }

      this.drawContext2 = drawContext;
   }

   public boolean matchesCriteria(RenderPipeline renderPipeline) {
      return renderPipeline == pipeline2;
   }

   public static ShimmerRenderer getShimmerRenderer() {
      ShimmerRenderer currentShimmerRenderer = shimmerRenderer;
      if (currentShimmerRenderer == null) {
         synchronized (ShimmerRenderer.class) {
            currentShimmerRenderer = shimmerRenderer;
            if (currentShimmerRenderer == null) {
               currentShimmerRenderer = new ShimmerRenderer();
               shimmerRenderer = currentShimmerRenderer;
            }
         }
      }

      return currentShimmerRenderer;
   }

   @Override
   public void close() {
      this.list.clear();
      this.map.clear();
      this.updateState3();
      this.drawContext2 = null;
   }

   public void updateState2() {
      this.drawContext2 = null;
      this.map.clear();
   }

   public void processData2(RenderPass renderPass) {
      if (renderPass != null && !this.list.isEmpty()) {
         GpuBuffer gpuBuffer = this.getGpuBuffer();
         if (gpuBuffer != null) {
            renderPass.setUniform("ShimmerParamsArray", gpuBuffer);
         }
      }
   }

   private static Identifier getIdentifier(String local5064_0) {
      return Identifier.of("greed", local5064_0);
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
      MemoryStack memoryStack = MemoryStack.stackPush();

      GpuBuffer gpuBuffer;
      try {
         this.gpuBuffer2 = RenderSystem.getDevice().createBuffer(() -> "greed_shimmer_params", 128, this.getByteBuffer(memoryStack));
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

   private void updateState3() {
      if (this.gpuBuffer2 != null) {
         this.gpuBuffer2.close();
         this.gpuBuffer2 = null;
      }
   }

   public static void updateState4() {
      ShimmerRenderer currentShimmerRenderer = shimmerRenderer;
      if (currentShimmerRenderer != null) {
         currentShimmerRenderer.close();
         shimmerRenderer = null;
      }
   }

   private ByteBuffer getByteBuffer(MemoryStack memoryStack) {
      ByteBuffer byteBuffer = memoryStack.calloc(4096);

      for (int index = 0; index < this.list.size(); index++) {
         ArcDividerDrawData arcDividerDrawData = this.list.get(index);
         int local5068_5 = index * 4 * 4;
         byteBuffer.putFloat(local5068_5, arcDividerDrawData.progress());
         byteBuffer.putFloat(local5068_5 + 4, arcDividerDrawData.halfWidth());
         byteBuffer.putFloat(local5068_5 + 8, arcDividerDrawData.intensity());
         byteBuffer.putFloat(local5068_5 + 12, 0.0F);
      }

      byteBuffer.position(0);
      return byteBuffer;
   }

   private GpuBuffer getGpuBuffer2() {
      if (this.gpuBuffer2 != null && !this.gpuBuffer2.isClosed() && this.gpuBuffer2.size() >= 4096L) {
         return this.gpuBuffer2;
      }

      this.updateState3();

      try {
         this.gpuBuffer2 = RenderSystem.getDevice().createBuffer(() -> "greed_shimmer_params", 136, 4096L);
         return this.gpuBuffer2;
      } catch (RuntimeException exception) {
         return null;
      }
   }

   private ShimmerRenderer() {
   }

   public void updateState5() {
      this.list.clear();
      this.flag = false;
   }

   private record DataRecord(GuiRenderState state, int layer, ScreenRect scissor) {
   }
}
