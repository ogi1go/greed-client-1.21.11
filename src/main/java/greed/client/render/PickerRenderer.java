package greed.client.render;

import greed.mixin.DrawContextMixin;
import greed.client.util.GuiScale;
import greed.client.render.PickerDrawData;
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

public final class PickerRenderer implements AutoCloseable {
   private static volatile PickerRenderer pickerRenderer;
   public static final RenderPipeline pipeline2 = RenderPipeline.builder()
      .withLocation(getIdentifier("pipeline/picker"))
      .withVertexShader(getIdentifier("core/picker"))
      .withFragmentShader(getIdentifier("core/picker"))
      .withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("PickerParamsArray", UniformType.UNIFORM_BUFFER)
      .build();
   private final List<PickerDrawData> list = new ArrayList<>(64);
   private DrawContext drawContext2;
   private GpuBuffer gpuBuffer2;
   private boolean flag = true;

   public boolean matchesCriteria(RenderPipeline renderPipeline) {
      return renderPipeline == pipeline2;
   }

   private static void processData(ByteBuffer byteBuffer, int local1222_1, int local1222_2) {
      byteBuffer.putFloat(local1222_1, (local1222_2 >>> 16 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local1222_1 + 4, (local1222_2 >>> 8 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local1222_1 + 8, (local1222_2 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local1222_1 + 12, (local1222_2 >>> 24 & 0xFF) / 255.0F);
   }

   public void processData2(RenderPass renderPass) {
      if (renderPass != null && !this.list.isEmpty()) {
         GpuBuffer gpuBuffer = this.getGpuBuffer2();
         if (gpuBuffer != null) {
            renderPass.setUniform("PickerParamsArray", gpuBuffer);
         }
      }
   }

   public void updateState() {
      this.list.clear();
      this.flag = false;
   }

   public void processData3(PickerDrawData pickerDrawData) {
      this.renderElement(this.drawContext2, pickerDrawData);
   }

   @Override
   public void close() {
      this.list.clear();
      this.drawContext2 = null;
      this.updateState2();
   }

   private void updateState2() {
      if (this.gpuBuffer2 != null) {
         this.gpuBuffer2.close();
         this.gpuBuffer2 = null;
      }
   }

   private void renderElement(DrawContext drawContext, PickerDrawData pickerDrawData) {
      if (drawContext != null && pickerDrawData != null && pickerDrawData.visible()) {
         try {
            Matrix3x2f matrix3x2f = GuiScale.renderElement(drawContext);
            ((DrawContextMixin)drawContext)
               .getGuiRenderState()
               .addSimpleElement(new PickerRenderState(matrix3x2f, pickerDrawData, ScissorStack.getScreenRect()));
         } catch (RuntimeException exception) {
         }
      }
   }

   private static Identifier getIdentifier(String local1231_0) {
      return Identifier.of("greed", local1231_0);
   }

   private GpuBuffer getGpuBuffer() {
      if (this.gpuBuffer2 != null && !this.gpuBuffer2.isClosed() && this.gpuBuffer2.size() >= 12288L) {
         return this.gpuBuffer2;
      }

      this.updateState2();

      try {
         this.gpuBuffer2 = RenderSystem.getDevice().createBuffer(() -> "greed_picker_params", 136, 12288L);
         return this.gpuBuffer2;
      } catch (RuntimeException exception) {
         return null;
      }
   }

   private GpuBuffer getGpuBuffer2() {
      if (!this.flag && this.gpuBuffer2 != null) {
         return this.gpuBuffer2;
      }

      this.updateState4();
      if (!this.flag && this.gpuBuffer2 != null) {
         return this.gpuBuffer2;
      }

      this.updateState2();
      this.updateState4();
      return this.gpuBuffer2;
   }

   public static void updateState3() {
      PickerRenderer currentPickerRenderer = pickerRenderer;
      if (currentPickerRenderer != null) {
         currentPickerRenderer.close();
         pickerRenderer = null;
      }
   }

   private ByteBuffer getByteBuffer(MemoryStack memoryStack, List<PickerDrawData> local1235_2) {
      ByteBuffer byteBuffer = memoryStack.calloc(Math.min(12288, local1235_2.size() * 3 * 4 * 4));

      for (int index = 0; index < local1235_2.size(); index++) {
         PickerDrawData pickerDrawData = local1235_2.get(index);
         int local1235_6 = index * 3 * 4 * 4;
         byteBuffer.putFloat(local1235_6, pickerDrawData.mode());
         byteBuffer.putFloat(local1235_6 + 4, Math.max(pickerDrawData.radius(), 0.0F));
         byteBuffer.putFloat(local1235_6 + 8, Math.max(pickerDrawData.smoothness(), 0.5F));
         byteBuffer.putFloat(local1235_6 + 12, Math.max(0.0F, Math.min(1.0F, pickerDrawData.alpha())));
         byteBuffer.putFloat(local1235_6 + 16, pickerDrawData.w());
         byteBuffer.putFloat(local1235_6 + 20, pickerDrawData.h());
         byteBuffer.putFloat(local1235_6 + 24, Math.max(pickerDrawData.checkerPx(), 1.0F));
         byteBuffer.putFloat(local1235_6 + 28, 0.0F);
         processData(byteBuffer, local1235_6 + 32, pickerDrawData.solidColor());
      }

      byteBuffer.position(0);
      return byteBuffer;
   }

   public void renderElement2(DrawContext drawContext) {
      this.drawContext2 = drawContext;
   }

   private PickerRenderer() {
   }

   public static PickerRenderer getPickerRenderer() {
      PickerRenderer currentPickerRenderer = pickerRenderer;
      if (currentPickerRenderer == null) {
         synchronized (PickerRenderer.class) {
            currentPickerRenderer = pickerRenderer;
            if (currentPickerRenderer == null) {
               currentPickerRenderer = new PickerRenderer();
               pickerRenderer = currentPickerRenderer;
            }
         }
      }

      return currentPickerRenderer;
   }

   public void updateState4() {
      if (!this.list.isEmpty() && this.flag) {
         GpuBuffer gpuBuffer = this.getGpuBuffer();
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

   public void updateState5() {
      this.drawContext2 = null;
   }

   public void renderElement3(DrawContext drawContext, PickerDrawData pickerDrawData) {
      this.renderElement2(drawContext);
      this.processData3(pickerDrawData);
      this.updateState5();
   }

   int computeInt(PickerDrawData pickerDrawData) {
      int local1242_2 = this.list.size();
      if (local1242_2 == 256) {
         return -1;
      }

      this.list.add(pickerDrawData);
      this.flag = true;
      return local1242_2;
   }
}
