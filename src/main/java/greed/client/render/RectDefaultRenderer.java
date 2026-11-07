package greed.client.render;

import greed.mixin.DrawContextMixin;
import greed.client.util.GuiScale;
import greed.client.render.ThemePaletteBuffer;
import greed.client.render.RectDrawData;
import greed.client.render.ScissorStack;
import greed.client.render.RoundedClipStack;

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
import org.lwjgl.system.MemoryUtil;

public final class RectDefaultRenderer implements AutoCloseable {
   private static volatile RectDefaultRenderer rectDefaultRenderer;
   public static final RenderPipeline pipeline2 = RenderPipeline.builder()
      .withLocation(getIdentifier("pipeline/rect_default"))
      .withVertexShader(getIdentifier("core/rect_default"))
      .withFragmentShader(getIdentifier("core/rect_default"))
      .withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("RectangleParamsArray", UniformType.UNIFORM_BUFFER)
      .withUniform("RectangleParamsArray2", UniformType.UNIFORM_BUFFER)
      .withUniform("PaletteParams", UniformType.UNIFORM_BUFFER)
      .build();
   private final List<RectDrawData> list = new ArrayList<>(128);
   private DrawContext drawContext2;
   private GpuBuffer gpuBuffer3;
   private ByteBuffer byteBuffer2;
   private boolean flag = true;

   private static Identifier getIdentifier(String local7111_0) {
      return Identifier.of("greed", local7111_0);
   }

   public void updateState() {
      this.drawContext2 = null;
   }

   public void renderElement(DrawContext drawContext) {
      this.drawContext2 = drawContext;
   }

   public static RectDefaultRenderer getRectDefaultRenderer() {
      RectDefaultRenderer currentRectDefaultRenderer = rectDefaultRenderer;
      if (currentRectDefaultRenderer == null) {
         synchronized (RectDefaultRenderer.class) {
            currentRectDefaultRenderer = rectDefaultRenderer;
            if (currentRectDefaultRenderer == null) {
               currentRectDefaultRenderer = new RectDefaultRenderer();
               rectDefaultRenderer = currentRectDefaultRenderer;
            }
         }
      }

      return currentRectDefaultRenderer;
   }

   private static void processData(ByteBuffer byteBuffer, int local7115_1, int local7115_2) {
      byteBuffer.putFloat(local7115_1, (local7115_2 >>> 16 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local7115_1 + 4, (local7115_2 >>> 8 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local7115_1 + 8, (local7115_2 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local7115_1 + 12, (local7115_2 >>> 24 & 0xFF) / 255.0F);
   }

   public void renderElement2(DrawContext drawContext, RectDrawData rectDrawData) {
      this.renderElement(drawContext);
      this.processData2(rectDrawData);
      this.updateState();
   }

   public static void updateState2() {
      RectDefaultRenderer currentRectDefaultRenderer = rectDefaultRenderer;
      if (currentRectDefaultRenderer != null) {
         currentRectDefaultRenderer.close();
         rectDefaultRenderer = null;
      }
   }

   int computeInt(RectDrawData rectDrawData) {
      int local7118_2 = this.list.size();
      if (local7118_2 == 1024) {
         return -1;
      }

      this.list.add(rectDrawData);
      this.flag = true;
      return local7118_2;
   }

   public void updateState3() {
      if (!this.list.isEmpty() && this.flag) {
         GpuBuffer gpuBuffer = this.getGpuBuffer();
         if (gpuBuffer != null) {
            ByteBuffer byteBuffer = this.getByteBuffer(this.list);
            if (byteBuffer != null && byteBuffer.remaining() > 0) {
               try {
                  RenderSystem.getDevice().createCommandEncoder().writeToBuffer(gpuBuffer.slice(0L, byteBuffer.remaining()), byteBuffer);
                  this.flag = false;
               } catch (RuntimeException exception) {
                  this.flag = true;
               }
            }
         }
      }
   }

   public void processData2(RectDrawData rectDrawData) {
      this.renderElement3(this.drawContext2, rectDrawData);
   }

   public void updateState4() {
      this.list.clear();
      this.flag = false;
   }

   private static float computeFloat(float local7123_0, float local7123_1, float local7123_2) {
      return Math.max(local7123_1, Math.min(local7123_2, local7123_0));
   }

   private RectDefaultRenderer() {
   }

   private GpuBuffer getGpuBuffer() {
      if (this.gpuBuffer3 != null && !this.gpuBuffer3.isClosed() && this.gpuBuffer3.size() >= 131072L) {
         return this.gpuBuffer3;
      }

      this.updateState5();

      try {
         this.gpuBuffer3 = RenderSystem.getDevice().createBuffer(() -> "greed_rect_default_params", 136, 131072L);
         return this.gpuBuffer3;
      } catch (RuntimeException exception) {
         return null;
      }
   }

   private void renderElement3(DrawContext drawContext, RectDrawData rectDrawData) {
      if (drawContext != null && rectDrawData != null && rectDrawData.visible()) {
         try {
            RectDrawData rectDrawData2 = this.getRectDrawData(rectDrawData);
            Matrix3x2f matrix3x2f = GuiScale.renderElement(drawContext);
            ((DrawContextMixin)drawContext)
               .getGuiRenderState()
               .addSimpleElement(new RectRenderState(matrix3x2f, rectDrawData2, ScissorStack.getScreenRect()));
         } catch (RuntimeException exception) {
         }
      }
   }

   public boolean matchesCriteria(RenderPipeline renderPipeline) {
      return renderPipeline == pipeline2;
   }

   private void updateState5() {
      if (this.gpuBuffer3 != null) {
         this.gpuBuffer3.close();
         this.gpuBuffer3 = null;
      }
   }

   private GpuBuffer getGpuBuffer2() {
      if (!this.flag && this.gpuBuffer3 != null) {
         return this.gpuBuffer3;
      }

      this.updateState3();
      if (!this.flag && this.gpuBuffer3 != null) {
         return this.gpuBuffer3;
      }

      this.updateState5();
      this.updateState3();
      return this.gpuBuffer3;
   }

   private RectDrawData getRectDrawData(RectDrawData rectDrawData) {
      float local7131_2 = Math.max(0.0F, Math.min(rectDrawData.width(), rectDrawData.height()) * 0.5F);
      float local7131_3 = 0.0F;
      float local7131_4 = 0.0F;
      float local7131_5 = 0.0F;
      float local7131_6 = 0.0F;
      float local7131_7 = 0.0F;
      float local7131_8 = 0.0F;
      float local7131_9 = 0.0F;
      float local7131_10 = 0.0F;
      if (RoundedClipStack.checkState()) {
         local7131_3 = RoundedClipStack.computeFloat9();
         local7131_4 = RoundedClipStack.computeFloat4();
         local7131_5 = RoundedClipStack.computeFloat6();
         local7131_6 = RoundedClipStack.computeFloat2();
         local7131_7 = RoundedClipStack.computeFloat3();
         local7131_8 = RoundedClipStack.computeFloat();
         local7131_9 = RoundedClipStack.computeFloat5();
         local7131_10 = RoundedClipStack.computeFloat7();
      } else {
         local7131_3 = rectDrawData.scissorX();
         local7131_4 = rectDrawData.scissorY();
         local7131_5 = rectDrawData.scissorWidth();
         local7131_6 = rectDrawData.scissorHeight();
         local7131_7 = rectDrawData.scissorRadiusTopLeft();
         local7131_8 = rectDrawData.scissorRadiusTopRight();
         local7131_9 = rectDrawData.scissorRadiusBottomRight();
         local7131_10 = rectDrawData.scissorRadiusBottomLeft();
      }

      return new RectDrawData(
         Math.round(rectDrawData.x() * GuiScale.computeFloat()) / GuiScale.computeFloat(),
         Math.round(rectDrawData.y() * GuiScale.computeFloat()) / GuiScale.computeFloat(),
         Math.max(
            (
                  Math.round((rectDrawData.x() + rectDrawData.width()) * GuiScale.computeFloat())
                     - Math.round(rectDrawData.x() * GuiScale.computeFloat())
               )
               / GuiScale.computeFloat(),
            1.0F / GuiScale.computeFloat()
         ),
         Math.max(
            (
                  Math.round((rectDrawData.y() + rectDrawData.height()) * GuiScale.computeFloat())
                     - Math.round(rectDrawData.y() * GuiScale.computeFloat())
               )
               / GuiScale.computeFloat(),
            1.0F / GuiScale.computeFloat()
         ),
         computeFloat(rectDrawData.radiusTopLeft(), 0.0F, local7131_2),
         computeFloat(rectDrawData.radiusTopRight(), 0.0F, local7131_2),
         computeFloat(rectDrawData.radiusBottomRight(), 0.0F, local7131_2),
         computeFloat(rectDrawData.radiusBottomLeft(), 0.0F, local7131_2),
         rectDrawData.colorTopLeft(),
         rectDrawData.colorTopRight(),
         rectDrawData.colorBottomRight(),
         rectDrawData.colorBottomLeft(),
         Math.max(rectDrawData.smoothness(), 0.0F),
         local7131_3,
         local7131_4,
         local7131_5,
         local7131_6,
         local7131_7,
         local7131_8,
         local7131_9,
         local7131_10,
         rectDrawData.paletteMode(),
         rectDrawData.paletteTint(),
         rectDrawData.paletteAlpha()
      );
   }

   private ByteBuffer getByteBuffer(List<RectDrawData> local7132_1) {
      int local7132_2 = Math.min(131072, local7132_1.size() * 8 * 4 * 4);
      if (local7132_2 <= 0) {
         return null;
      }

      if (this.byteBuffer2 == null) {
         this.byteBuffer2 = MemoryUtil.memCalloc(131072);
      }

      ByteBuffer byteBuffer = this.byteBuffer2;
      byteBuffer.clear();
      MemoryUtil.memSet(MemoryUtil.memAddress(byteBuffer), 0, local7132_2);

      for (int index = 0; index < local7132_1.size(); index++) {
         RectDrawData rectDrawData = local7132_1.get(index);
         int local7132_6 = index * 8 * 4 * 4;
         byteBuffer.putFloat(local7132_6, rectDrawData.radiusTopLeft());
         byteBuffer.putFloat(local7132_6 + 4, rectDrawData.radiusTopRight());
         byteBuffer.putFloat(local7132_6 + 8, rectDrawData.radiusBottomRight());
         byteBuffer.putFloat(local7132_6 + 12, rectDrawData.radiusBottomLeft());
         byteBuffer.putFloat(local7132_6 + 16, rectDrawData.width());
         byteBuffer.putFloat(local7132_6 + 20, rectDrawData.height());
         byteBuffer.putFloat(local7132_6 + 24, rectDrawData.smoothness());
         byteBuffer.putFloat(local7132_6 + 28, rectDrawData.paletteMode());
         if (rectDrawData.paletteMode() != 0) {
            byteBuffer.putFloat(local7132_6 + 32, rectDrawData.paletteTint());
            byteBuffer.putFloat(local7132_6 + 36, rectDrawData.paletteAlpha());
         } else {
            processData(byteBuffer, local7132_6 + 32, rectDrawData.colorTopLeft());
            processData(byteBuffer, local7132_6 + 48, rectDrawData.colorTopRight());
            processData(byteBuffer, local7132_6 + 64, rectDrawData.colorBottomRight());
            processData(byteBuffer, local7132_6 + 80, rectDrawData.colorBottomLeft());
         }

         float local7132_7 = GuiScale.computeFloat();
         float local7132_8 = GuiScale.computeFloat2();
         byteBuffer.putFloat(local7132_6 + 96, rectDrawData.scissorX() * local7132_7);
         byteBuffer.putFloat(local7132_6 + 100, local7132_8 - (rectDrawData.scissorY() + rectDrawData.scissorHeight()) * local7132_7);
         byteBuffer.putFloat(local7132_6 + 104, rectDrawData.scissorWidth() * local7132_7);
         byteBuffer.putFloat(local7132_6 + 108, rectDrawData.scissorHeight() * local7132_7);
         byteBuffer.putFloat(local7132_6 + 112, rectDrawData.scissorRadiusBottomLeft() * local7132_7);
         byteBuffer.putFloat(local7132_6 + 116, rectDrawData.scissorRadiusBottomRight() * local7132_7);
         byteBuffer.putFloat(local7132_6 + 120, rectDrawData.scissorRadiusTopRight() * local7132_7);
         byteBuffer.putFloat(local7132_6 + 124, rectDrawData.scissorRadiusTopLeft() * local7132_7);
      }

      byteBuffer.limit(local7132_2).position(0);
      return byteBuffer;
   }

   @Override
   public void close() {
      this.list.clear();
      this.drawContext2 = null;
      this.updateState5();
      if (this.byteBuffer2 != null) {
         MemoryUtil.memFree(this.byteBuffer2);
         this.byteBuffer2 = null;
      }
   }

   public void processData3(RenderPass renderPass) {
      if (renderPass != null && !this.list.isEmpty()) {
         GpuBuffer gpuBuffer = this.getGpuBuffer2();
         if (gpuBuffer != null) {
            renderPass.setUniform("RectangleParamsArray", gpuBuffer.slice(0L, 65536L));
            renderPass.setUniform("RectangleParamsArray2", gpuBuffer.slice(65536L, 65536L));
         }

         GpuBuffer gpuBuffer2 = ThemePaletteBuffer.getGpuBuffer2();
         if (gpuBuffer2 != null) {
            renderPass.setUniform("PaletteParams", gpuBuffer2);
         }
      }
   }
}
