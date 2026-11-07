package greed.client.render;

import greed.client.GreedClient;
import greed.mixin.DrawContextMixin;
import greed.client.util.GuiScale;
import greed.client.render.GuiLayerSerialAccess;
import greed.client.render.HalfIconDrawData;

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
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;
import org.lwjgl.system.MemoryStack;

public final class RectHalfIconRenderer implements AutoCloseable {
   private static final VertexFormat vertexFormat = VertexFormat.builder()
      .add("Position", VertexFormatElement.POSITION)
      .add("UV0", VertexFormatElement.UV0)
      .add("Color", VertexFormatElement.COLOR)
      .add("LineWidth", VertexFormatElement.LINE_WIDTH)
      .build();
   private static volatile RectHalfIconRenderer rectHalfIconRenderer;
   public static final RenderPipeline pipeline2 = RenderPipeline.builder()
      .withLocation(getIdentifier("pipeline/rect_half_icon"))
      .withVertexShader(getIdentifier("core/rect_half_icon"))
      .withFragmentShader(getIdentifier("core/rect_half_icon"))
      .withVertexFormat(vertexFormat, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("RectHalfIconParamsArray", UniformType.UNIFORM_BUFFER)
      .build();
   private final List<HalfIconDrawData> drawDataList = new ArrayList<>(32);
   private final Map<RectHalfIconRenderer.DataRecord, HalfIconRenderState> renderStateMap = new LinkedHashMap<>(32);
   private DrawContext drawContext2;
   private GpuBuffer gpuBuffer2;
   private boolean bufferDirty = true;

   private static int multiplyAlpha(int argb, float alpha) {
      int scaledAlpha = Math.round((argb >>> 24 & 0xFF) * clamp(alpha, 0.0F, 1.0F));
      return scaledAlpha << 24 | argb & 16777215;
   }

   private static float seededRandom(long seed, int gridX, int gridY, int channel) {
      long hash = seed ^ gridX * -7046029254386353131L ^ gridY * -4658895280553007687L ^ channel * -7723592293110705685L;
      hash ^= hash >>> 30;
      hash *= -4658895280553007687L;
      hash ^= hash >>> 27;
      hash *= -7723592293110705685L;
      hash ^= hash >>> 31;
      return (float)(hash & 16777215L) / 1.6777215E7F * 2.0F - 1.0F;
   }

   public void updateState() {
      if (!this.drawDataList.isEmpty() && this.bufferDirty) {
         GpuBuffer gpuBuffer = this.getGpuBuffer2();
         if (gpuBuffer != null) {
            try {
               MemoryStack memoryStack = MemoryStack.stackPush();

               try {
                  ByteBuffer byteBuffer = this.getByteBuffer(memoryStack, this.drawDataList);
                  RenderSystem.getDevice().createCommandEncoder().writeToBuffer(gpuBuffer.slice(0L, byteBuffer.remaining()), byteBuffer);
                  this.bufferDirty = false;
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
               this.bufferDirty = true;
            }
         }
      }
   }

   private static float clamp(float value, float min, float max) {
      return Math.max(min, Math.min(max, value));
   }

   private void submitHalfIcon(DrawContext drawContext, HalfIconDrawData halfIconDrawData) {
      if (drawContext != null && halfIconDrawData != null && halfIconDrawData.iconsVisible()) {
         try {
            HalfIconDrawData halfIconDrawData2 = this.getHalfIconDrawData(halfIconDrawData);
            TextAtlasRenderer.DataRecord2 atlasRecord = TextAtlasRenderer.getTextAtlasRenderer()
               .getNested0465(halfIconDrawData2.fontName(), halfIconDrawData2.icon(), halfIconDrawData2.iconSize());
            if (atlasRecord.empty()) {
               return;
            }

            Matrix3x2f matrix3x2f = GuiScale.renderElement(drawContext);
            GuiRenderState guiRenderState = ((DrawContextMixin)drawContext).getGuiRenderState();
            int currentLayerSerial = ((GuiLayerSerialAccess)guiRenderState).getLayerSerial();
            RectHalfIconRenderer.DataRecord3 poseRecord = RectHalfIconRenderer.DataRecord3.of(matrix3x2f);

            for (TextAtlasRenderer.DataRecord4 page : atlasRecord.pages()) {
               if (!page.glyphs().isEmpty()) {
                  List<HalfIconRenderState.DataRecord> vertices = this.getList(halfIconDrawData2, atlasRecord, page);
                  if (!vertices.isEmpty()) {
                     RectHalfIconRenderer.DataRecord renderStateKey = new RectHalfIconRenderer.DataRecord(
                        guiRenderState, currentLayerSerial, page.textureSetup(), poseRecord
                     );
                     HalfIconRenderState halfIconRenderState = this.renderStateMap.get(renderStateKey);
                     if (halfIconRenderState == null) {
                        halfIconRenderState = new HalfIconRenderState(matrix3x2f, page.textureSetup());
                        halfIconRenderState.add(halfIconDrawData2, vertices);
                        this.renderStateMap.put(renderStateKey, halfIconRenderState);
                        guiRenderState.addSimpleElement(halfIconRenderState);
                     } else {
                        halfIconRenderState.add(halfIconDrawData2, vertices);
                     }
                  }
               }
            }
         } catch (RuntimeException exception) {
            GreedClient.logger().warn("[HalfIconRectangle] Failed to submit icon rectangle", exception);
         }
      }
   }

   private List<HalfIconRenderState.DataRecord> getList(
      HalfIconDrawData halfIconDrawData, TextAtlasRenderer.DataRecord2 atlasRecord, TextAtlasRenderer.DataRecord4 page
   ) {
      List<HalfIconRenderState.DataRecord> vertices = new ArrayList<>(64);
      float stepX = Math.max(halfIconDrawData.spacingX(), Math.max(atlasRecord.width(), 1.0F));
      float stepY = Math.max(halfIconDrawData.spacingY(), Math.max(atlasRecord.height(), 1.0F));
      float minX = halfIconDrawData.x() + halfIconDrawData.paddingX() + atlasRecord.width() * 0.5F;
      float minY = halfIconDrawData.y() + halfIconDrawData.paddingY() + atlasRecord.height() * 0.5F;
      float maxX = halfIconDrawData.x() + halfIconDrawData.width() - halfIconDrawData.paddingX() - atlasRecord.width() * 0.5F;
      float maxY = halfIconDrawData.y() + halfIconDrawData.height() - halfIconDrawData.paddingY() - atlasRecord.height() * 0.5F;
      if (!(maxX < minX) && !(maxY < minY)) {
         int rows = Math.min(96, Math.max(1, (int)Math.floor((maxY - minY) / stepY) + 1));
         int columns = Math.min(96, Math.max(1, (int)Math.floor((maxX - minX) / stepX) + 1));

         for (int index = 0; index < rows; index++) {
            for (int index2 = 0; index2 < columns; index2++) {
               float centerX = minX
                  + index2 * stepX
                  + seededRandom(halfIconDrawData.seed(), index2, index, 1) * halfIconDrawData.jitterX();
               float centerY = minY
                  + index * stepY
                  + seededRandom(halfIconDrawData.seed(), index2, index, 2) * halfIconDrawData.jitterY();
               float rotationRadians = (float)Math.toRadians(seededRandom(halfIconDrawData.seed(), index2, index, 3) * halfIconDrawData.rotationMaxDegrees());
               float sinRotation = (float)Math.sin(rotationRadians);
               float cosRotation = (float)Math.cos(rotationRadians);
               float iconMinX = centerX - atlasRecord.width() * 0.5F;
               float iconMinY = centerY - atlasRecord.height() * 0.5F;
               int fadedColor = multiplyAlpha(
                  halfIconDrawData.iconColor(), 1.0F - clamp((centerY - halfIconDrawData.y()) / Math.max(halfIconDrawData.height(), 1.0F), 0.0F, 1.0F) * 0.92F
               );

               for (TextAtlasRenderer.DataRecord3 glyph : page.glyphs()) {
                  float leftX = iconMinX + glyph.x0();
                  float topY = iconMinY + glyph.y0();
                  float rightX = iconMinX + glyph.x1();
                  float bottomY = iconMinY + glyph.y1();
                  RectHalfIconRenderer.DataRecord2 rotatedTopLeft = rotatePoint(leftX, topY, centerX, centerY, sinRotation, cosRotation);
                  RectHalfIconRenderer.DataRecord2 rotatedBottomLeft = rotatePoint(leftX, bottomY, centerX, centerY, sinRotation, cosRotation);
                  RectHalfIconRenderer.DataRecord2 rotatedBottomRight = rotatePoint(rightX, bottomY, centerX, centerY, sinRotation, cosRotation);
                  RectHalfIconRenderer.DataRecord2 rotatedTopRight = rotatePoint(rightX, topY, centerX, centerY, sinRotation, cosRotation);
                  vertices.add(
                     new HalfIconRenderState.DataRecord(
                        rotatedTopLeft.x(),
                        rotatedTopLeft.y(),
                        glyph.u0(),
                        glyph.v0(),
                        rotatedBottomLeft.x(),
                        rotatedBottomLeft.y(),
                        glyph.u0(),
                        glyph.v1(),
                        rotatedBottomRight.x(),
                        rotatedBottomRight.y(),
                        glyph.u1(),
                        glyph.v1(),
                        rotatedTopRight.x(),
                        rotatedTopRight.y(),
                        glyph.u1(),
                        glyph.v0(),
                        fadedColor
                     )
                  );
               }
            }
         }

         return vertices;
      } else {
         return List.of();
      }
   }

   private ByteBuffer getByteBuffer(MemoryStack memoryStack, List<HalfIconDrawData> drawDataEntries) {
      ByteBuffer byteBuffer = memoryStack.calloc(Math.min(12288, drawDataEntries.size() * 3 * 4 * 4));

      for (int index = 0; index < drawDataEntries.size(); index++) {
         HalfIconDrawData halfIconDrawData = drawDataEntries.get(index);
         int offset = index * 3 * 4 * 4;
         byteBuffer.putFloat(offset, halfIconDrawData.x());
         byteBuffer.putFloat(offset + 4, halfIconDrawData.y());
         byteBuffer.putFloat(offset + 8, halfIconDrawData.width());
         byteBuffer.putFloat(offset + 12, halfIconDrawData.height());
         byteBuffer.putFloat(offset + 16, halfIconDrawData.radiusTopLeft());
         byteBuffer.putFloat(offset + 20, halfIconDrawData.radiusTopRight());
         byteBuffer.putFloat(offset + 24, halfIconDrawData.radiusBottomRight());
         byteBuffer.putFloat(offset + 28, halfIconDrawData.radiusBottomLeft());
         byteBuffer.putFloat(offset + 32, halfIconDrawData.smoothness());
         byteBuffer.putFloat(offset + 36, 0.0F);
         byteBuffer.putFloat(offset + 40, 0.0F);
         byteBuffer.putFloat(offset + 44, 0.0F);
      }

      byteBuffer.position(0);
      return byteBuffer;
   }

   private static RectHalfIconRenderer.DataRecord2 rotatePoint(
      float pointX, float pointY, float centerX, float centerY, float sinRotation, float cosRotation
   ) {
      float deltaX = pointX - centerX;
      float deltaY = pointY - centerY;
      return new RectHalfIconRenderer.DataRecord2(
         centerX + deltaX * cosRotation - deltaY * sinRotation, centerY + deltaX * sinRotation + deltaY * cosRotation
      );
   }

   private HalfIconDrawData getHalfIconDrawData(HalfIconDrawData halfIconDrawData) {
      float maxCornerRadius = Math.max(0.0F, Math.min(halfIconDrawData.width(), halfIconDrawData.height()) * 0.5F);
      float iconSize = Math.max(halfIconDrawData.iconSize(), 1.0F);
      return new HalfIconDrawData(
         halfIconDrawData.x(),
         halfIconDrawData.y(),
         halfIconDrawData.width(),
         halfIconDrawData.height(),
         clamp(halfIconDrawData.radiusTopLeft(), 0.0F, maxCornerRadius),
         clamp(halfIconDrawData.radiusTopRight(), 0.0F, maxCornerRadius),
         clamp(halfIconDrawData.radiusBottomRight(), 0.0F, maxCornerRadius),
         clamp(halfIconDrawData.radiusBottomLeft(), 0.0F, maxCornerRadius),
         halfIconDrawData.colorTopLeft(),
         halfIconDrawData.colorTopRight(),
         halfIconDrawData.colorBottomRight(),
         halfIconDrawData.colorBottomLeft(),
         Math.max(halfIconDrawData.smoothness(), 0.0F),
         halfIconDrawData.fontName(),
         halfIconDrawData.icon(),
         iconSize,
         halfIconDrawData.iconColor(),
         Math.max(halfIconDrawData.spacingX(), iconSize),
         Math.max(halfIconDrawData.spacingY(), iconSize),
         Math.max(halfIconDrawData.paddingX(), 0.0F),
         Math.max(halfIconDrawData.paddingY(), 0.0F),
         Math.max(halfIconDrawData.jitterX(), 0.0F),
         Math.max(halfIconDrawData.jitterY(), 0.0F),
         clamp(halfIconDrawData.rotationMaxDegrees(), 0.0F, 90.0F),
         halfIconDrawData.seed()
      );
   }

   private void releaseGpuBuffer() {
      if (this.gpuBuffer2 != null) {
         this.gpuBuffer2.close();
         this.gpuBuffer2 = null;
      }
   }

   public static RectHalfIconRenderer getRectHalfIconRenderer() {
      RectHalfIconRenderer currentRectHalfIconRenderer = rectHalfIconRenderer;
      if (currentRectHalfIconRenderer == null) {
         synchronized (RectHalfIconRenderer.class) {
            currentRectHalfIconRenderer = rectHalfIconRenderer;
            if (currentRectHalfIconRenderer == null) {
               currentRectHalfIconRenderer = new RectHalfIconRenderer();
               rectHalfIconRenderer = currentRectHalfIconRenderer;
            }
         }
      }

      return currentRectHalfIconRenderer;
   }

   private GpuBuffer getGpuBuffer() {
      if (!this.bufferDirty && this.gpuBuffer2 != null) {
         return this.gpuBuffer2;
      }

      this.updateState();
      if (!this.bufferDirty && this.gpuBuffer2 != null) {
         return this.gpuBuffer2;
      }

      this.releaseGpuBuffer();
      this.updateState();
      return this.gpuBuffer2;
   }

   int computeInt2(HalfIconDrawData halfIconDrawData) {
      int index = this.drawDataList.size();
      if (index == 256) {
         return -1;
      }

      this.drawDataList.add(halfIconDrawData);
      this.bufferDirty = true;
      return index;
   }

   public void processData(RenderPass renderPass) {
      if (renderPass != null && !this.drawDataList.isEmpty()) {
         GpuBuffer gpuBuffer = this.getGpuBuffer();
         if (gpuBuffer != null) {
            renderPass.setUniform("RectHalfIconParamsArray", gpuBuffer);
         }
      }
   }

   private static Identifier getIdentifier(String path) {
      return Identifier.of("greed", path);
   }

   public boolean matchesCriteria(RenderPipeline renderPipeline) {
      return renderPipeline == pipeline2;
   }

   public void processData2(HalfIconDrawData halfIconDrawData) {
      this.submitHalfIcon(this.drawContext2, halfIconDrawData);
   }

   public static void updateState3() {
      RectHalfIconRenderer currentRectHalfIconRenderer = rectHalfIconRenderer;
      if (currentRectHalfIconRenderer != null) {
         currentRectHalfIconRenderer.close();
         rectHalfIconRenderer = null;
      }
   }

   @Override
   public void close() {
      this.drawDataList.clear();
      this.renderStateMap.clear();
      this.drawContext2 = null;
      this.releaseGpuBuffer();
   }

   private GpuBuffer getGpuBuffer2() {
      if (this.gpuBuffer2 != null && !this.gpuBuffer2.isClosed() && this.gpuBuffer2.size() >= 12288L) {
         return this.gpuBuffer2;
      }

      this.releaseGpuBuffer();

      try {
         this.gpuBuffer2 = RenderSystem.getDevice().createBuffer(() -> "greed_rect_half_icon_params", 136, 12288L);
         return this.gpuBuffer2;
      } catch (RuntimeException exception) {
         return null;
      }
   }

   private RectHalfIconRenderer() {
   }

   public void renderElement2(DrawContext drawContext, HalfIconDrawData halfIconDrawData) {
      this.renderElement3(drawContext);
      this.processData2(halfIconDrawData);
      this.updateState5();
   }

   public void updateState4() {
      this.drawDataList.clear();
      this.bufferDirty = false;
   }

   public void renderElement3(DrawContext drawContext) {
      if (this.drawContext2 != drawContext) {
         this.renderStateMap.clear();
      }

      this.drawContext2 = drawContext;
   }

   public void updateState5() {
      this.drawContext2 = null;
      this.renderStateMap.clear();
   }

   private record DataRecord(GuiRenderState state, int layerSerial, TextureSetup textureSetup, RectHalfIconRenderer.DataRecord3 pose) {
   }

   private record DataRecord2(float x, float y) {
   }

   private record DataRecord3(float m00, float m01, float m10, float m11, float m20, float m21) {
      static RectHalfIconRenderer.DataRecord3 of(Matrix3x2f matrix3x2f) {
         return new RectHalfIconRenderer.DataRecord3(
            matrix3x2f.m00(), matrix3x2f.m01(), matrix3x2f.m10(), matrix3x2f.m11(), matrix3x2f.m20(), matrix3x2f.m21()
         );
      }
   }
}
