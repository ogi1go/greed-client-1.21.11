package greed.client.render;

import greed.mixin.DrawContextMixin;
import greed.client.util.GuiScale;
import greed.client.render.ScissorStack;
import greed.client.render.OutlineDrawData;
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

public final class OutlineDefaultRenderer implements AutoCloseable {
   private static volatile OutlineDefaultRenderer outlineDefaultRenderer;
   public static final RenderPipeline pipeline2 = RenderPipeline.builder()
      .withLocation(getIdentifier("pipeline/outline_default"))
      .withVertexShader(getIdentifier("core/outline_default"))
      .withFragmentShader(getIdentifier("core/outline_default"))
      .withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("OutlineParamsArray", UniformType.UNIFORM_BUFFER)
      .withUniform("OutlineParamsArray2", UniformType.UNIFORM_BUFFER)
      .build();
   private final List<OutlineDrawData> list = new ArrayList<>(128);
   private DrawContext drawContext2;
   private GpuBuffer gpuBuffer2;
   private ByteBuffer byteBuffer2;
   private boolean flag = true;

   public void processData(RenderPass renderPass) {
      if (renderPass != null && !this.list.isEmpty()) {
         GpuBuffer gpuBuffer = this.getGpuBuffer();
         if (gpuBuffer != null) {
            renderPass.setUniform("OutlineParamsArray", gpuBuffer.slice(0L, 65536L));
            renderPass.setUniform("OutlineParamsArray2", gpuBuffer.slice(65536L, 65536L));
         }
      }
   }

   private static void processData2(ByteBuffer byteBuffer, int local2144_1, int local2144_2) {
      byteBuffer.putFloat(local2144_1, (local2144_2 >>> 16 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local2144_1 + 4, (local2144_2 >>> 8 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local2144_1 + 8, (local2144_2 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local2144_1 + 12, (local2144_2 >>> 24 & 0xFF) / 255.0F);
   }

   private void renderElement(DrawContext drawContext, OutlineDrawData outlineDrawData) {
      if (drawContext != null && outlineDrawData != null && outlineDrawData.visible()) {
         try {
            OutlineDrawData outlineDrawData2 = this.getOutlineDrawData(outlineDrawData);
            Matrix3x2f matrix3x2f = GuiScale.renderElement(drawContext);
            ((DrawContextMixin)drawContext)
               .getGuiRenderState()
               .addSimpleElement(new OutlineRenderState(matrix3x2f, outlineDrawData2, ScissorStack.getScreenRect()));
         } catch (RuntimeException exception) {
         }
      }
   }

   private OutlineDefaultRenderer() {
   }

   public void processData3(OutlineDrawData outlineDrawData) {
      this.renderElement(this.drawContext2, outlineDrawData);
   }

   private OutlineDrawData getOutlineDrawData(OutlineDrawData outlineDrawData) {
      float local2150_2 = Math.max(0.0F, Math.min(outlineDrawData.width(), outlineDrawData.height()) * 0.5F);
      float local2150_3 = Math.max(0.0F, Math.min(outlineDrawData.width(), outlineDrawData.height()) * 0.5F);
      float[] floatArray = null;
      if (RoundedClipStack.checkState()) {
         floatArray = new float[]{
            RoundedClipStack.computeFloat9(),
            RoundedClipStack.computeFloat4(),
            RoundedClipStack.computeFloat6(),
            RoundedClipStack.computeFloat2(),
            RoundedClipStack.computeFloat3(),
            RoundedClipStack.computeFloat(),
            RoundedClipStack.computeFloat5(),
            RoundedClipStack.computeFloat7()
         };
      }

      return new OutlineDrawData(
         outlineDrawData.x(),
         outlineDrawData.y(),
         outlineDrawData.width(),
         outlineDrawData.height(),
         computeFloat(outlineDrawData.radiusTopLeft(), 0.0F, local2150_2),
         computeFloat(outlineDrawData.radiusTopRight(), 0.0F, local2150_2),
         computeFloat(outlineDrawData.radiusBottomRight(), 0.0F, local2150_2),
         computeFloat(outlineDrawData.radiusBottomLeft(), 0.0F, local2150_2),
         computeFloat(outlineDrawData.thickness(), 0.0F, local2150_3),
         outlineDrawData.colorTopLeft(),
         outlineDrawData.colorTopRight(),
         outlineDrawData.colorBottomRight(),
         outlineDrawData.colorBottomLeft(),
         Math.max(outlineDrawData.smoothness(), 0.0F),
         floatArray
      );
   }

   public boolean matchesCriteria(RenderPipeline renderPipeline) {
      return renderPipeline == pipeline2;
   }

   public void updateState() {
      this.list.clear();
      this.flag = false;
   }

   private void updateState2() {
      if (this.gpuBuffer2 != null) {
         this.gpuBuffer2.close();
         this.gpuBuffer2 = null;
      }
   }

   public static void updateState3() {
      OutlineDefaultRenderer currentOutlineDefaultRenderer = outlineDefaultRenderer;
      if (currentOutlineDefaultRenderer != null) {
         currentOutlineDefaultRenderer.close();
         outlineDefaultRenderer = null;
      }
   }

   private static float computeFloat(float local2155_0, float local2155_1, float local2155_2) {
      return Math.max(local2155_1, Math.min(local2155_2, local2155_0));
   }

   private static Identifier getIdentifier(String local2156_0) {
      return Identifier.of("greed", local2156_0);
   }

   public void renderElement2(DrawContext drawContext, OutlineDrawData outlineDrawData) {
      this.renderElement3(drawContext);
      this.processData3(outlineDrawData);
      this.updateState5();
   }

   int computeInt(OutlineDrawData outlineDrawData) {
      int local2158_2 = this.list.size();
      if (local2158_2 == 1024) {
         return -1;
      }

      this.list.add(outlineDrawData);
      this.flag = true;
      return local2158_2;
   }

   private GpuBuffer getGpuBuffer() {
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

   public void renderElement3(DrawContext drawContext) {
      this.drawContext2 = drawContext;
   }

   @Override
   public void close() {
      this.list.clear();
      this.drawContext2 = null;
      this.updateState2();
      if (this.byteBuffer2 != null) {
         MemoryUtil.memFree(this.byteBuffer2);
         this.byteBuffer2 = null;
      }
   }

   public static OutlineDefaultRenderer getOutlineDefaultRenderer() {
      OutlineDefaultRenderer currentOutlineDefaultRenderer = outlineDefaultRenderer;
      if (currentOutlineDefaultRenderer == null) {
         synchronized (OutlineDefaultRenderer.class) {
            currentOutlineDefaultRenderer = outlineDefaultRenderer;
            if (currentOutlineDefaultRenderer == null) {
               currentOutlineDefaultRenderer = new OutlineDefaultRenderer();
               outlineDefaultRenderer = currentOutlineDefaultRenderer;
            }
         }
      }

      return currentOutlineDefaultRenderer;
   }

   private GpuBuffer getGpuBuffer2() {
      if (this.gpuBuffer2 != null && !this.gpuBuffer2.isClosed() && this.gpuBuffer2.size() >= 131072L) {
         return this.gpuBuffer2;
      }

      this.updateState2();

      try {
         this.gpuBuffer2 = RenderSystem.getDevice().createBuffer(() -> "greed_outline_default_params", 136, 131072L);
         return this.gpuBuffer2;
      } catch (RuntimeException exception) {
         return null;
      }
   }

   private ByteBuffer getByteBuffer(List<OutlineDrawData> local2164_1) {
      int local2164_2 = Math.min(131072, local2164_1.size() * 8 * 4 * 4);
      if (local2164_2 <= 0) {
         return null;
      }

      if (this.byteBuffer2 == null) {
         this.byteBuffer2 = MemoryUtil.memCalloc(131072);
      }

      ByteBuffer byteBuffer = this.byteBuffer2;
      byteBuffer.clear();
      MemoryUtil.memSet(MemoryUtil.memAddress(byteBuffer), 0, local2164_2);

      for (int index = 0; index < local2164_1.size(); index++) {
         OutlineDrawData outlineDrawData = local2164_1.get(index);
         int local2164_6 = index * 8 * 4 * 4;
         byteBuffer.putFloat(local2164_6, outlineDrawData.radiusTopLeft());
         byteBuffer.putFloat(local2164_6 + 4, outlineDrawData.radiusTopRight());
         byteBuffer.putFloat(local2164_6 + 8, outlineDrawData.radiusBottomRight());
         byteBuffer.putFloat(local2164_6 + 12, outlineDrawData.radiusBottomLeft());
         byteBuffer.putFloat(local2164_6 + 16, outlineDrawData.width());
         byteBuffer.putFloat(local2164_6 + 20, outlineDrawData.height());
         byteBuffer.putFloat(local2164_6 + 24, outlineDrawData.thickness());
         byteBuffer.putFloat(local2164_6 + 28, outlineDrawData.smoothness());
         processData2(byteBuffer, local2164_6 + 32, outlineDrawData.colorTopLeft());
         processData2(byteBuffer, local2164_6 + 48, outlineDrawData.colorTopRight());
         processData2(byteBuffer, local2164_6 + 64, outlineDrawData.colorBottomRight());
         processData2(byteBuffer, local2164_6 + 80, outlineDrawData.colorBottomLeft());
         float[] floatArray = outlineDrawData.scissor();
         if (floatArray != null) {
            float local2164_8 = GuiScale.computeFloat();
            float local2164_9 = GuiScale.computeFloat2();
            byteBuffer.putFloat(local2164_6 + 96, floatArray[0] * local2164_8);
            byteBuffer.putFloat(local2164_6 + 100, local2164_9 - (floatArray[1] + floatArray[3]) * local2164_8);
            byteBuffer.putFloat(local2164_6 + 104, floatArray[2] * local2164_8);
            byteBuffer.putFloat(local2164_6 + 108, floatArray[3] * local2164_8);
            byteBuffer.putFloat(local2164_6 + 112, floatArray[7] * local2164_8);
            byteBuffer.putFloat(local2164_6 + 116, floatArray[6] * local2164_8);
            byteBuffer.putFloat(local2164_6 + 120, floatArray[5] * local2164_8);
            byteBuffer.putFloat(local2164_6 + 124, floatArray[4] * local2164_8);
         }
      }

      byteBuffer.limit(local2164_2).position(0);
      return byteBuffer;
   }

   public void updateState4() {
      if (!this.list.isEmpty() && this.flag) {
         GpuBuffer gpuBuffer = this.getGpuBuffer2();
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

   public void updateState5() {
      this.drawContext2 = null;
   }
}
