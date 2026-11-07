package greed.client.render;

import greed.client.GreedClient;
import greed.mixin.DrawContextMixin;
import greed.client.util.GuiScale;
import greed.client.render.GuiLayerSerialAccess;
import greed.client.render.MenuGridDrawData;
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

public final class MenuGridRenderer implements AutoCloseable {
   private static volatile MenuGridRenderer menuGridRenderer;
   private static final VertexFormat vertexFormat = VertexFormat.builder()
      .add("Position", VertexFormatElement.POSITION)
      .add("UV0", VertexFormatElement.UV0)
      .add("Color", VertexFormatElement.COLOR)
      .add("LineWidth", VertexFormatElement.LINE_WIDTH)
      .build();
   public static final RenderPipeline pipeline2 = RenderPipeline.builder()
      .withLocation(getIdentifier("pipeline/menu_grid"))
      .withVertexShader(getIdentifier("core/menu_grid"))
      .withFragmentShader(getIdentifier("core/menu_grid"))
      .withVertexFormat(vertexFormat, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("MenuGridParamsArray", UniformType.UNIFORM_BUFFER)
      .build();
   private final List<MenuGridDrawData> list = new ArrayList<>(4);
   private final Map<MenuGridRenderer.DataRecord, MenuGridRenderState> map = new LinkedHashMap<>(4);
   private DrawContext drawContext2;
   private GpuBuffer gpuBuffer2;
   private boolean flag;

   private GpuBuffer getGpuBuffer() {
      if (this.gpuBuffer2 != null && !this.gpuBuffer2.isClosed() && this.gpuBuffer2.size() >= 2048L) {
         return this.gpuBuffer2;
      }

      this.updateState3();

      try {
         this.gpuBuffer2 = RenderSystem.getDevice().createBuffer(() -> "greed_menu_grid_params", 136, 2048L);
         return this.gpuBuffer2;
      } catch (RuntimeException exception) {
         return null;
      }
   }

   @Override
   public void close() {
      this.list.clear();
      this.map.clear();
      this.updateState3();
      this.drawContext2 = null;
   }

   private GpuBuffer getGpuBuffer2() {
      if (!this.flag && this.gpuBuffer2 != null) {
         return this.gpuBuffer2;
      }

      this.updateState5();
      if (!this.flag && this.gpuBuffer2 != null) {
         return this.gpuBuffer2;
      }

      this.updateState3();
      MemoryStack memoryStack = MemoryStack.stackPush();

      GpuBuffer gpuBuffer;
      try {
         this.gpuBuffer2 = RenderSystem.getDevice().createBuffer(() -> "greed_menu_grid_params", 128, this.getByteBuffer(memoryStack));
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

   public void updateState() {
      this.drawContext2 = null;
      this.map.clear();
   }

   public void processData(RenderPass renderPass) {
      if (renderPass != null && !this.list.isEmpty()) {
         GpuBuffer gpuBuffer = this.getGpuBuffer2();
         if (gpuBuffer != null) {
            renderPass.setUniform("MenuGridParamsArray", gpuBuffer);
         }
      }
   }

   public void renderElement(DrawContext drawContext) {
      if (this.drawContext2 != drawContext) {
         this.map.clear();
      }

      this.drawContext2 = drawContext;
   }

   private static Identifier getIdentifier(String local3740_0) {
      return Identifier.of("greed", local3740_0);
   }

   public void processData2(MenuGridDrawData menuGridDrawData) {
      if (this.drawContext2 != null && menuGridDrawData != null) {
         try {
            GuiRenderState guiRenderState = ((DrawContextMixin)(Object)this.drawContext2).getGuiRenderState();
            Matrix3x2f matrix3x2f = GuiScale.renderElement(this.drawContext2);
            ScreenRect screenRect = ScissorStack.getScreenRect();
            MenuGridRenderer.DataRecord local3741_5 = new MenuGridRenderer.DataRecord(
               guiRenderState, ((GuiLayerSerialAccess)guiRenderState).getLayerSerial(), screenRect
            );
            MenuGridRenderState menuGridRenderState = this.map.get(local3741_5);
            if (menuGridRenderState == null) {
               menuGridRenderState = new MenuGridRenderState(matrix3x2f, screenRect);
               menuGridRenderState.add(menuGridDrawData);
               this.map.put(local3741_5, menuGridRenderState);
               guiRenderState.addSimpleElement(menuGridRenderState);
            } else {
               menuGridRenderState.add(menuGridDrawData);
            }
         } catch (RuntimeException exception) {
            GreedClient.logger().warn("[MenuGridRenderer] submit failed", exception);
         }
      }
   }

   public static void updateState2() {
      MenuGridRenderer currentMenuGridRenderer = menuGridRenderer;
      if (currentMenuGridRenderer != null) {
         currentMenuGridRenderer.close();
         menuGridRenderer = null;
      }
   }

   public boolean matchesCriteria(RenderPipeline renderPipeline) {
      return renderPipeline == pipeline2;
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

   int computeInt(MenuGridDrawData menuGridDrawData) {
      if (this.list.size() >= 64) {
         return -1;
      }

      this.list.add(menuGridDrawData);
      this.flag = true;
      return this.list.size() - 1;
   }

   private MenuGridRenderer() {
   }

   private ByteBuffer getByteBuffer(MemoryStack memoryStack) {
      ByteBuffer byteBuffer = memoryStack.calloc(2048);
      float local3750_3 = (float)(System.nanoTime() % 1000000000000L) / 1.0E9F;

      for (int index = 0; index < this.list.size(); index++) {
         int local3750_5 = index * 8 * 4;
         byteBuffer.putFloat(local3750_5, this.list.get(index).width());
         byteBuffer.putFloat(local3750_5 + 4, this.list.get(index).height());
         byteBuffer.putFloat(local3750_5 + 8, this.list.get(index).cellPx());
         byteBuffer.putFloat(local3750_5 + 12, this.list.get(index).lineWidthPx());
         byteBuffer.putFloat(local3750_5 + 16, this.list.get(index).mouseX() - this.list.get(index).x());
         byteBuffer.putFloat(local3750_5 + 20, this.list.get(index).mouseY() - this.list.get(index).y());
         byteBuffer.putFloat(local3750_5 + 24, this.list.get(index).influenceRadiusPx());
         byteBuffer.putFloat(local3750_5 + 28, local3750_3);
      }

      byteBuffer.position(0);
      return byteBuffer;
   }

   public static MenuGridRenderer getMenuGridRenderer() {
      MenuGridRenderer currentMenuGridRenderer = menuGridRenderer;
      if (currentMenuGridRenderer == null) {
         synchronized (MenuGridRenderer.class) {
            currentMenuGridRenderer = menuGridRenderer;
            if (currentMenuGridRenderer == null) {
               currentMenuGridRenderer = new MenuGridRenderer();
               menuGridRenderer = currentMenuGridRenderer;
            }
         }
      }

      return currentMenuGridRenderer;
   }

   public void updateState5() {
      if (!this.list.isEmpty() && this.flag) {
         GpuBuffer gpuBuffer = this.getGpuBuffer();
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

   private record DataRecord(GuiRenderState state, int layer, ScreenRect scissor) {
   }
}
