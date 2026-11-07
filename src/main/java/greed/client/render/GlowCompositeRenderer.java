package greed.client.render;

import greed.mixin.DrawContextMixin;
import greed.client.util.GuiScale;
import greed.client.render.ThemePaletteBuffer;
import greed.client.render.GradientSplitBuffer;
import greed.client.render.GlowDrawBatch;
import greed.client.render.ScissorStack;
import greed.client.render.PaletteTransition;
import greed.client.render.GlowCompositeDrawData;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.RenderSystem.ShapeIndexBuffer;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public final class GlowCompositeRenderer implements AutoCloseable {
   private static volatile GlowCompositeRenderer glowCompositeRenderer;
   public static final RenderPipeline pipeline2 = RenderPipeline.builder()
      .withLocation(greedIdentifier("pipeline/glow_composite"))
      .withVertexShader(greedIdentifier("ui/glow/glow_composite"))
      .withFragmentShader(greedIdentifier("ui/glow/glow_composite"))
      .withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, DrawMode.QUADS)
      .withBlend(BlendFunction.LIGHTNING)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("GlowParamsArray", UniformType.UNIFORM_BUFFER)
      .build();
   private static final RenderPipeline pipeline3 = RenderPipeline.builder()
      .withLocation(greedIdentifier("pipeline/glow_shape"))
      .withVertexShader(greedIdentifier("ui/glow/glow_shape"))
      .withFragmentShader(greedIdentifier("ui/glow/glow_shape"))
      .withVertexFormat(VertexFormats.POSITION, DrawMode.QUADS)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withUniform("GlowParamsArray", UniformType.UNIFORM_BUFFER)
      .withUniform("SplitParams", UniformType.UNIFORM_BUFFER)
      .build();
   private static final RenderPipeline pipeline4 = RenderPipeline.builder()
      .withLocation(greedIdentifier("pipeline/glow_source"))
      .withVertexShader(greedIdentifier("ui/glow/glow_source"))
      .withFragmentShader(greedIdentifier("ui/glow/glow_shape"))
      .withVertexFormat(VertexFormats.POSITION, DrawMode.QUADS)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withUniform("GlowParamsArray", UniformType.UNIFORM_BUFFER)
      .withUniform("SplitParams", UniformType.UNIFORM_BUFFER)
      .build();
   private static final RenderPipeline pipeline5 = RenderPipeline.builder()
      .withLocation(greedIdentifier("pipeline/glow_blur_down"))
      .withVertexShader(greedIdentifier("ui/glow/glow_blur"))
      .withFragmentShader(greedIdentifier("ui/glow/glow_blur_down"))
      .withVertexFormat(VertexFormats.POSITION, DrawMode.QUADS)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("KawaseParams", UniformType.UNIFORM_BUFFER)
      .build();
   private static final RenderPipeline pipeline6 = RenderPipeline.builder()
      .withLocation(greedIdentifier("pipeline/glow_blur_up"))
      .withVertexShader(greedIdentifier("ui/glow/glow_blur"))
      .withFragmentShader(greedIdentifier("ui/glow/glow_blur_up"))
      .withVertexFormat(VertexFormats.POSITION, DrawMode.QUADS)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("KawaseParams", UniformType.UNIFORM_BUFFER)
      .build();
   private static final RenderPipeline pipeline7 = RenderPipeline.builder()
      .withLocation(greedIdentifier("pipeline/glow_cutout"))
      .withVertexShader(greedIdentifier("ui/glow/glow_blur"))
      .withFragmentShader(greedIdentifier("ui/glow/glow_cutout"))
      .withVertexFormat(VertexFormats.POSITION, DrawMode.QUADS)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withSampler("Sampler0")
      .withSampler("Sampler1")
      .withUniform("KawaseParams", UniformType.UNIFORM_BUFFER)
      .build();
   private final List<SimpleFramebuffer[]> blurDownChains = new ArrayList<>(224);
   private final List<SimpleFramebuffer[]> blurUpChains = new ArrayList<>(224);
   private final List<SimpleFramebuffer> shapeFramebuffers = new ArrayList<>(224);
   private final List<SimpleFramebuffer> blurFramebuffers = new ArrayList<>(224);
   private final List<SimpleFramebuffer> resultFramebuffers = new ArrayList<>(224);
   private int targetFramebufferHeight;
   private long lastResizeNanos;
   private final List<GlowCompositeRenderer.DataRecord> glowDataRecords = new ArrayList<>(32);
   private final List<GlowCompositeDrawData> activeGlowData = new ArrayList<>(32);
   private final List<GlowDrawBatch> activeGlowBatches = new ArrayList<>(32);
   private int[] atlasOffsetXArray = new int[224];
   private int[] atlasOffsetYArray = new int[224];
   private int[] atlasWidthArray = new int[224];
   private int[] atlasHeightArray = new int[224];
   private DrawContext currentDrawContext;
   private GpuBuffer glowParamsBuffer;
   private GpuBuffer kawaseParamsBuffer;
   private GpuBuffer fullscreenQuadBuffer;
   private GpuBuffer sourceQuadsBuffer;
   private GpuBuffer sourceParamsBuffer;
   private ByteBuffer vertexScratchBuffer;
   private ByteBuffer uniformScratchBuffer;
   private boolean paramsDirty = true;
   private boolean deferredDirtyFlag;
   private final GlowDrawBatch glowDrawBatch = new GlowDrawBatch();

   public void updateState() {
      this.activeGlowData.clear();
      this.activeGlowBatches.clear();
      this.paramsDirty = false;
      this.deferredDirtyFlag = false;
   }

   private ByteBuffer getOrGrowVertexBuffer(int requiredBytes) {
      if (this.vertexScratchBuffer == null || this.vertexScratchBuffer.capacity() < requiredBytes) {
         if (this.vertexScratchBuffer != null) {
            MemoryUtil.memFree(this.vertexScratchBuffer);
         }

         this.vertexScratchBuffer = MemoryUtil.memAlloc(Math.max(requiredBytes, 64 * VertexFormats.POSITION.getVertexSize()));
      }

      this.vertexScratchBuffer.clear();
      return this.vertexScratchBuffer;
   }

   public void renderElement(DrawContext drawContext) {
      this.currentDrawContext = drawContext;
   }

   @Override
   public void close() {
      this.activeGlowData.clear();
      this.currentDrawContext = null;
      this.targetFramebufferHeight = 0;
      this.disposeGlowParamsBuffer();
      this.disposeKawaseParamsBuffer();
      if (this.fullscreenQuadBuffer != null) {
         this.fullscreenQuadBuffer.close();
         this.fullscreenQuadBuffer = null;
      }

      if (this.sourceQuadsBuffer != null) {
         this.sourceQuadsBuffer.close();
         this.sourceQuadsBuffer = null;
      }

      if (this.sourceParamsBuffer != null) {
         this.sourceParamsBuffer.close();
         this.sourceParamsBuffer = null;
      }

      if (this.vertexScratchBuffer != null) {
         MemoryUtil.memFree(this.vertexScratchBuffer);
         this.vertexScratchBuffer = null;
      }

      if (this.uniformScratchBuffer != null) {
         MemoryUtil.memFree(this.uniformScratchBuffer);
         this.uniformScratchBuffer = null;
      }

      for (SimpleFramebuffer simpleFramebuffer : this.shapeFramebuffers) {
         if (simpleFramebuffer != null) {
            simpleFramebuffer.delete();
         }
      }

      for (SimpleFramebuffer simpleFramebuffer2 : this.blurFramebuffers) {
         if (simpleFramebuffer2 != null) {
            simpleFramebuffer2.delete();
         }
      }

      for (SimpleFramebuffer simpleFramebuffer3 : this.resultFramebuffers) {
         if (simpleFramebuffer3 != null) {
            simpleFramebuffer3.delete();
         }
      }

      this.shapeFramebuffers.clear();
      this.blurFramebuffers.clear();
      this.resultFramebuffers.clear();
      deleteFramebufferRows(this.blurDownChains);
      deleteFramebufferRows(this.blurUpChains);
   }

   private GpuBuffer acquireGlowParamsBuffer() {
      if (this.glowParamsBuffer != null && !this.glowParamsBuffer.isClosed() && this.glowParamsBuffer.size() >= 17920L) {
         return this.glowParamsBuffer;
      }

      this.disposeGlowParamsBuffer();

      try {
         this.glowParamsBuffer = RenderSystem.getDevice().createBuffer(() -> "greed_glow_params", 136, 17920L);
         return this.glowParamsBuffer;
      } catch (RuntimeException exception) {
         return null;
      }
   }

   private static void writeVertex(ByteBuffer byteBuffer, int offset, float x, float y) {
      byteBuffer.putFloat(offset, x);
      byteBuffer.putFloat(offset + 4, y);
      byteBuffer.putFloat(offset + 8, 0.0F);
   }

   private SimpleFramebuffer getBlurFramebuffer(int index, int width, int height) {
      while (this.blurFramebuffers.size() <= index) {
         this.blurFramebuffers.add(null);
      }

      SimpleFramebuffer simpleFramebuffer = this.resizeOrCreateFramebuffer(this.blurFramebuffers.get(index), "greed_glow_blur_" + index, width, height);
      this.blurFramebuffers.set(index, simpleFramebuffer);
      return simpleFramebuffer;
   }

   public void renderElement2(DrawContext drawContext, GlowCompositeDrawData glowCompositeDrawData) {
      this.renderElement(drawContext);
      this.processData13(glowCompositeDrawData);
      this.updateState6();
   }

   private SimpleFramebuffer getShapeFramebuffer(int index, int width, int height) {
      while (this.shapeFramebuffers.size() <= index) {
         this.shapeFramebuffers.add(null);
      }

      SimpleFramebuffer simpleFramebuffer = this.resizeOrCreateFramebuffer(this.shapeFramebuffers.get(index), "greed_glow_shape_" + index, width, height);
      this.shapeFramebuffers.set(index, simpleFramebuffer);
      return simpleFramebuffer;
   }

   private void enqueueGlowElement(DrawContext drawContext, GlowCompositeDrawData glowCompositeDrawData) {
      if (drawContext != null && glowCompositeDrawData != null && glowCompositeDrawData.visible()) {
         try {
            Matrix3x2f matrix3x2f = GuiScale.renderElement(drawContext);
            GlowDrawBatch currentGlowDrawBatch = new GlowDrawBatch();
            if (this.glowDataRecords.size() < 224) {
               currentGlowDrawBatch.intVal = this.glowDataRecords.size();
               this.glowDataRecords.add(new GlowCompositeRenderer.DataRecord(glowCompositeDrawData, currentGlowDrawBatch, matrix3x2f));
            }

            ((DrawContextMixin)drawContext)
               .getGuiRenderState()
               .addSimpleElement(new GlowCompositeRenderState(matrix3x2f, glowCompositeDrawData, ScissorStack.getScreenRect(), currentGlowDrawBatch));
         } catch (RuntimeException exception) {
         }
      }
   }

   private int layoutGlowAtlas(int atlasWidth) {
      int recordCount = this.glowDataRecords.size();
      if (this.atlasOffsetXArray.length < recordCount) {
         this.atlasOffsetXArray = new int[recordCount];
         this.atlasOffsetYArray = new int[recordCount];
         this.atlasWidthArray = new int[recordCount];
         this.atlasHeightArray = new int[recordCount];
      }

      float maxPad = 8.0F;

      for (int index = 0; index < recordCount; index++) {
         maxPad = Math.max(maxPad, this.glowDataRecords.get(index).glow().effectivePad());
      }

      int basePadding = Math.max(8, Math.round(maxPad * 1.0F));
      int cursorX = basePadding;
      int cursorY = basePadding;
      int rowHeight = 0;

      for (int index2 = 0; index2 < recordCount; index2++) {
         GlowCompositeDrawData glowCompositeDrawData = this.glowDataRecords.get(index2).glow();
         float itemPadding = glowCompositeDrawData.effectivePad();
         int itemWidth = Math.max(Math.round((glowCompositeDrawData.width() + itemPadding * 2.0F) * 1.0F), 1);
         int itemHeight = Math.max(Math.round((glowCompositeDrawData.height() + itemPadding * 2.0F) * 1.0F), 1);
         if (cursorX + itemWidth + basePadding > atlasWidth) {
            cursorX = basePadding;
            cursorY += rowHeight + basePadding;
            rowHeight = 0;
         }

         if (itemWidth + basePadding * 2 <= atlasWidth && cursorY + itemHeight + basePadding <= 2048) {
            this.atlasOffsetXArray[index2] = cursorX;
            this.atlasOffsetYArray[index2] = cursorY;
            this.atlasWidthArray[index2] = itemWidth;
            this.atlasHeightArray[index2] = itemHeight;
            cursorX += itemWidth + basePadding;
            rowHeight = Math.max(rowHeight, itemHeight);
         } else {
            this.atlasWidthArray[index2] = 0;
            this.atlasHeightArray[index2] = 0;
         }
      }

      return cursorY + rowHeight + basePadding;
   }

   private void renderGlowSource(SimpleFramebuffer simpleFramebuffer, int targetWidth, int targetHeight) {
      int recordCount = this.glowDataRecords.size();
      if (simpleFramebuffer != null && recordCount != 0) {
         int alignment = Math.max(1, RenderSystem.getDevice().getUniformOffsetAlignment());
         int uniformStride = (1248 + alignment - 1) / alignment * alignment;
         int vertexStride = 4 * VertexFormats.POSITION.getVertexSize();
         ByteBuffer byteBuffer = this.getOrGrowVertexBuffer(recordCount * vertexStride);
         ByteBuffer byteBuffer2 = this.getOrGrowUniformBuffer(recordCount * uniformStride);
         int drawCount = 0;

         for (int index = 0; index < recordCount; index++) {
            GlowCompositeRenderer.DataRecord dataRecord = this.glowDataRecords.get(index);
            GlowDrawBatch currentGlowDrawBatch = dataRecord.capture();
            if (this.atlasWidthArray[index] > 0 && this.atlasHeightArray[index] > 0) {
               GlowCompositeDrawData glowCompositeDrawData = dataRecord.glow();
               int offsetX = this.atlasOffsetXArray[index];
               int offsetY = this.atlasOffsetYArray[index];
               float itemPadding = glowCompositeDrawData.effectivePad();
               float quadWidth = Math.min((glowCompositeDrawData.width() + itemPadding * 2.0F) * 1.0F, this.atlasWidthArray[index]);
               float quadHeight = Math.min((glowCompositeDrawData.height() + itemPadding * 2.0F) * 1.0F, this.atlasHeightArray[index]);
               currentGlowDrawBatch.floatVal = (float)offsetX / targetWidth;
               currentGlowDrawBatch.floatVal2 = (float)offsetY / targetHeight;
               currentGlowDrawBatch.floatVal3 = Math.max(quadWidth / targetWidth, 1.0E-6F);
               currentGlowDrawBatch.floatVal4 = Math.max(quadHeight / targetHeight, 1.0E-6F);
               float leftNdc = (float)offsetX / targetWidth * 2.0F - 1.0F;
               float topNdc = (float)offsetY / targetHeight * 2.0F - 1.0F;
               float rightNdc = (offsetX + quadWidth) / targetWidth * 2.0F - 1.0F;
               float bottomNdc = (offsetY + quadHeight) / targetHeight * 2.0F - 1.0F;
               int vertexByteOffset = drawCount * vertexStride;
               writeVertex(byteBuffer, vertexByteOffset, leftNdc, topNdc);
               writeVertex(byteBuffer, vertexByteOffset + 12, leftNdc, bottomNdc);
               writeVertex(byteBuffer, vertexByteOffset + 24, rightNdc, bottomNdc);
               writeVertex(byteBuffer, vertexByteOffset + 36, rightNdc, topNdc);
               ByteBuffer byteBuffer3 = byteBuffer2.slice(drawCount * uniformStride, 1248).order(byteBuffer2.order());
               MemoryUtil.memSet(MemoryUtil.memAddress(byteBuffer3), 0, 1248L);
               writeGlowDrawData(byteBuffer3, 0, glowCompositeDrawData, 0.0F, 0.0F, 1.0F, 1.0F);
               byteBuffer3.putFloat(48, glowCompositeDrawData.splitIndex());
               writeSpanUniforms(byteBuffer3, glowCompositeDrawData);
               writePaletteUniforms(byteBuffer3);
               drawCount++;
            } else {
               currentGlowDrawBatch.floatVal = 0.0F;
               currentGlowDrawBatch.floatVal2 = 0.0F;
               currentGlowDrawBatch.floatVal3 = 0.0F;
               currentGlowDrawBatch.floatVal4 = 0.0F;
            }
         }

         if (drawCount != 0) {
            int vertexBytes = drawCount * vertexStride;
            int uniformBytes = drawCount * uniformStride;

            try {
               CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
               GpuBuffer gpuBuffer = this.acquireSourceQuadsBuffer(vertexBytes);
               GpuBuffer gpuBuffer2 = this.acquireSourceParamsBuffer(uniformBytes);
               byteBuffer.position(0).limit(vertexBytes);
               commandEncoder.writeToBuffer(gpuBuffer.slice(0L, vertexBytes), byteBuffer);
               byteBuffer2.position(0).limit(uniformBytes);
               commandEncoder.writeToBuffer(gpuBuffer2.slice(0L, uniformBytes), byteBuffer2);
               byteBuffer.clear();
               byteBuffer2.clear();
               ShapeIndexBuffer sequentialBuffer = RenderSystem.getSequentialBuffer(DrawMode.QUADS);
               GpuBuffer indexBuffer = sequentialBuffer.getIndexBuffer(6 * drawCount);

               try (RenderPass renderPass = commandEncoder.createRenderPass(
                     () -> "greed_glow_source", simpleFramebuffer.getColorAttachmentView(), OptionalInt.empty(), null, OptionalDouble.empty()
                  )) {
                  renderPass.setPipeline(pipeline4);
                  GpuBuffer gpuBuffer3 = GradientSplitBuffer.getGpuBuffer();
                  if (gpuBuffer3 != null) {
                     renderPass.setUniform("SplitParams", gpuBuffer3);
                  }

                  renderPass.setVertexBuffer(0, gpuBuffer);
                  renderPass.setIndexBuffer(indexBuffer, sequentialBuffer.getIndexType());

                  for (int index2 = 0; index2 < drawCount; index2++) {
                     renderPass.setUniform("GlowParamsArray", gpuBuffer2.slice((long)index2 * uniformStride, 1248L));
                     renderPass.drawIndexed(0, index2 * 6, 6, 1);
                  }
               }
            } catch (RuntimeException exception) {
               byteBuffer.clear();
               byteBuffer2.clear();
            }
         }
      }
   }

   private void disposeGlowParamsBuffer() {
      if (this.glowParamsBuffer != null) {
         this.glowParamsBuffer.close();
         this.glowParamsBuffer = null;
      }
   }

   private BuiltBuffer getBuiltBuffer(BufferAllocator bufferAllocator) {
      BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION);
      bufferBuilder.vertex(-1.0F, -1.0F, 0.0F);
      bufferBuilder.vertex(-1.0F, 1.0F, 0.0F);
      bufferBuilder.vertex(1.0F, 1.0F, 0.0F);
      bufferBuilder.vertex(1.0F, -1.0F, 0.0F);
      return bufferBuilder.end();
   }

   private GpuBuffer acquireKawaseParamsBuffer() {
      if (this.kawaseParamsBuffer != null && !this.kawaseParamsBuffer.isClosed() && this.kawaseParamsBuffer.size() >= 48L) {
         return this.kawaseParamsBuffer;
      }

      this.disposeKawaseParamsBuffer();

      try {
         this.kawaseParamsBuffer = RenderSystem.getDevice().createBuffer(() -> "greed_glow_kawase_params", 136, 48L);
         return this.kawaseParamsBuffer;
      } catch (RuntimeException exception) {
         return null;
      }
   }

   private static float clampFloat(float value, float minValue, float maxValue) {
      return Math.max(minValue, Math.min(maxValue, value));
   }

   private static Identifier greedIdentifier(String path) {
      return Identifier.of("greed", path);
   }

   private GlowCompositeRenderer() {
   }

   int computeInt2(GlowCompositeDrawData glowCompositeDrawData, GlowDrawBatch newGlowDrawBatch) {
      int index = this.activeGlowData.size();
      if (index >= 224) {
         return -1;
      }

      this.activeGlowData.add(glowCompositeDrawData);
      this.activeGlowBatches.add(newGlowDrawBatch);
      this.paramsDirty = true;
      return index;
   }

   private ByteBuffer buildGlowUniformData(MemoryStack memoryStack) {
      int batchCount = Math.max(1, this.activeGlowData.size());
      int bufferBytes = batchCount * 5 * 4 * 4;
      ByteBuffer byteBuffer = memoryStack.calloc(bufferBytes);

      for (int index = 0; index < this.activeGlowData.size(); index++) {
         GlowCompositeDrawData glowCompositeDrawData = this.activeGlowData.get(index);
         GlowDrawBatch currentGlowDrawBatch = index < this.activeGlowBatches.size() ? this.activeGlowBatches.get(index) : null;
         float normX = currentGlowDrawBatch != null ? currentGlowDrawBatch.floatVal : 0.0F;
         float normY = currentGlowDrawBatch != null ? currentGlowDrawBatch.floatVal2 : 0.0F;
         float normWidth = currentGlowDrawBatch != null ? currentGlowDrawBatch.floatVal3 : 1.0F;
         float normHeight = currentGlowDrawBatch != null ? currentGlowDrawBatch.floatVal4 : 1.0F;
         writeGlowDrawData(byteBuffer, index, glowCompositeDrawData, normX, normY, normWidth, normHeight);
      }

      byteBuffer.position(0);
      return byteBuffer;
   }

   private void clearFramebuffer(SimpleFramebuffer simpleFramebuffer) {
      if (simpleFramebuffer != null) {
         CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
         RenderPass renderPass = commandEncoder.createRenderPass(
            () -> "greed_glow_clear", simpleFramebuffer.getColorAttachmentView(), OptionalInt.of(0), null, OptionalDouble.empty()
         );
         if (renderPass != null) {
            renderPass.close();
         }
      }
   }

   public void updateState3() {
      if (!this.glowDataRecords.isEmpty()) {
         MinecraftClient minecraftClient = MinecraftClient.getInstance();
         if (minecraftClient != null && minecraftClient.getWindow() != null) {
            int atlasHeight = this.layoutGlowAtlas(2048);
            int targetWidth = 2048;
            int roundedHeight = Math.max(256, Math.min(2048, (atlasHeight + 256 - 1) / 256 * 256));
            long nowNanos = System.nanoTime();
            if (roundedHeight >= this.targetFramebufferHeight) {
               this.targetFramebufferHeight = roundedHeight;
               this.lastResizeNanos = nowNanos;
            } else if (nowNanos - this.lastResizeNanos > 3000000000L) {
               this.targetFramebufferHeight = roundedHeight;
               this.lastResizeNanos = nowNanos;
            }

            int framebufferHeight = this.targetFramebufferHeight;
            SimpleFramebuffer simpleFramebuffer = this.getShapeFramebuffer(0, targetWidth, framebufferHeight);
            SimpleFramebuffer simpleFramebuffer2 = this.getBlurFramebuffer(0, targetWidth, framebufferHeight);
            SimpleFramebuffer simpleFramebuffer3 = this.getResultFramebuffer(0, targetWidth, framebufferHeight);
            if (simpleFramebuffer != null && simpleFramebuffer2 != null && simpleFramebuffer3 != null) {
               GpuSampler gpuSampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
               this.clearFramebuffer(simpleFramebuffer);
               float maxBlurRadius = 0.0F;

               for (int index = 0; index < this.glowDataRecords.size(); index++) {
                  maxBlurRadius = Math.max(maxBlurRadius, this.glowDataRecords.get(index).glow().blurRadius());
               }

               this.renderGlowSource(simpleFramebuffer, targetWidth, framebufferHeight);
               int blurPassCount = 3;
               float blurRadiusScaled = clampFloat(maxBlurRadius / 20.0F, 0.5F, 4.0F) * 1.0F;
               this.ensureBlurFramebuffers(0, targetWidth, framebufferHeight, blurPassCount);
               SimpleFramebuffer[] simpleFramebufferArray = this.blurDownChains.get(0);
               SimpleFramebuffer[] simpleFramebufferArray2 = this.blurUpChains.get(0);
               GpuTextureView gpuTextureView = simpleFramebuffer.getColorAttachmentView();
               int textureWidth = simpleFramebuffer.textureWidth;
               int textureHeight = simpleFramebuffer.textureHeight;

               for (int index2 = 0; index2 < blurPassCount; index2++) {
                  this.runBlurPass(
                     pipeline5, gpuTextureView, textureWidth, textureHeight, simpleFramebufferArray[index2], blurRadiusScaled, 0.0F, 0.0F, 1.0F, 1.0F, gpuSampler
                  );
                  gpuTextureView = simpleFramebufferArray[index2].getColorAttachmentView();
                  textureWidth = simpleFramebufferArray[index2].textureWidth;
                  textureHeight = simpleFramebufferArray[index2].textureHeight;
               }

               for (int index3 = blurPassCount - 2; index3 >= 0; index3--) {
                  this.runBlurPass(
                     pipeline6, gpuTextureView, textureWidth, textureHeight, simpleFramebufferArray2[index3], blurRadiusScaled, 0.0F, 0.0F, 1.0F, 1.0F, gpuSampler
                  );
                  gpuTextureView = simpleFramebufferArray2[index3].getColorAttachmentView();
                  textureWidth = simpleFramebufferArray2[index3].textureWidth;
                  textureHeight = simpleFramebufferArray2[index3].textureHeight;
               }

               this.runBlurPass(pipeline6, gpuTextureView, textureWidth, textureHeight, simpleFramebuffer2, blurRadiusScaled, 0.0F, 0.0F, 1.0F, 1.0F, gpuSampler);
               this.runCutoutPass(simpleFramebuffer2, simpleFramebuffer, simpleFramebuffer3, gpuSampler);
               TextureSetup textureSetup = TextureSetup.of(simpleFramebuffer3.getColorAttachmentView(), gpuSampler);

               for (int index4 = 0; index4 < this.glowDataRecords.size(); index4++) {
                  this.glowDataRecords.get(index4).capture().textureSetup = textureSetup;
               }

               this.glowDataRecords.clear();
            } else {
               this.glowDataRecords.clear();
            }
         } else {
            this.glowDataRecords.clear();
         }
      }
   }

   private GpuBuffer acquireSourceQuadsBuffer(int requiredBytes) {
      if (this.sourceQuadsBuffer == null || this.sourceQuadsBuffer.isClosed() || this.sourceQuadsBuffer.size() < requiredBytes) {
         if (this.sourceQuadsBuffer != null) {
            this.sourceQuadsBuffer.close();
         }

         int bufferSize = Math.max(requiredBytes, 64 * VertexFormats.POSITION.getVertexSize());
         bufferSize = Integer.highestOneBit(bufferSize - 1) << 1;
         this.sourceQuadsBuffer = RenderSystem.getDevice().createBuffer(() -> "greed_glow_src_quads", 40, bufferSize);
      }

      return this.sourceQuadsBuffer;
   }

   public static void updateState4() {
      GlowCompositeRenderer currentGlowCompositeRenderer = glowCompositeRenderer;
      if (currentGlowCompositeRenderer != null) {
         currentGlowCompositeRenderer.close();
         glowCompositeRenderer = null;
      }
   }

   private void ensureBlurFramebuffers(int level, int width, int height, int passCount) {
      while (this.blurDownChains.size() <= level) {
         this.blurDownChains.add(new SimpleFramebuffer[4]);
         this.blurUpChains.add(new SimpleFramebuffer[4]);
      }

      SimpleFramebuffer[] simpleFramebufferArray = this.blurDownChains.get(level);
      SimpleFramebuffer[] simpleFramebufferArray2 = this.blurUpChains.get(level);
      int mipWidth = Math.max(width, 1);
      int mipHeight = Math.max(height, 1);

      for (int index = 0; index < passCount; index++) {
         simpleFramebufferArray[index] = this.resizeOrCreateFramebuffer(simpleFramebufferArray[index], "greed_glow_down_" + level + "_" + index, mipWidth, mipHeight);
         if (index < passCount - 1) {
            simpleFramebufferArray2[index] = this.resizeOrCreateFramebuffer(simpleFramebufferArray2[index], "greed_glow_up_" + level + "_" + index, mipWidth, mipHeight);
         }

         mipWidth = Math.max(mipWidth / 2, 1);
         mipHeight = Math.max(mipHeight / 2, 1);
      }
   }

   private static void deleteFramebufferRows(List<SimpleFramebuffer[]> framebufferRows) {
      for (SimpleFramebuffer[] simpleFramebufferArray : framebufferRows) {
         for (int index = 0; index < simpleFramebufferArray.length; index++) {
            if (simpleFramebufferArray[index] != null) {
               simpleFramebufferArray[index].delete();
               simpleFramebufferArray[index] = null;
            }
         }
      }

      framebufferRows.clear();
   }

   private static void writePaletteUniforms(ByteBuffer byteBuffer) {
      int paletteOffset = 1120;
      int colorCount = Math.max(1, Math.min(ThemePaletteBuffer.computeInt3(), 6));
      int[] intArray = ThemePaletteBuffer.computeInt2();
      byteBuffer.putFloat(paletteOffset, colorCount);
      byteBuffer.putFloat(paletteOffset + 4, ThemePaletteBuffer.computeFloat4());
      byteBuffer.putFloat(paletteOffset + 8, ThemePaletteBuffer.computeFloat());
      byteBuffer.putFloat(paletteOffset + 12, PaletteTransition.getProgress());
      int colorsOffset = paletteOffset + 112;
      byteBuffer.putFloat(colorsOffset, ThemePaletteBuffer.computeFloat3());
      byteBuffer.putFloat(colorsOffset + 4, ThemePaletteBuffer.computeFloat5());

      for (int index = 0; index < colorCount && index < intArray.length; index++) {
         int rgb = intArray[index] & 16777215;
         int entryOffset = paletteOffset + (1 + index) * 4 * 4;
         byteBuffer.putFloat(entryOffset, (rgb >>> 16 & 0xFF) / 255.0F);
         byteBuffer.putFloat(entryOffset + 4, (rgb >>> 8 & 0xFF) / 255.0F);
         byteBuffer.putFloat(entryOffset + 8, (rgb & 0xFF) / 255.0F);
         byteBuffer.putFloat(entryOffset + 12, 1.0F);
      }
   }

   private void renderShape(int shapeIndex, GlowCompositeDrawData glowCompositeDrawData, SimpleFramebuffer simpleFramebuffer) {
      GpuBuffer gpuBuffer = this.ensureFullscreenQuadBuffer();
      if (gpuBuffer != null && simpleFramebuffer != null) {
         GpuBuffer gpuBuffer2 = null;

         try {
            MemoryStack memoryStack = MemoryStack.stackPush();

            try {
               ByteBuffer byteBuffer = memoryStack.calloc(1248);
               writeGlowDrawData(byteBuffer, 0, glowCompositeDrawData, 0.0F, 0.0F, 1.0F, 1.0F);
               byteBuffer.putFloat(48, glowCompositeDrawData.splitIndex());
               writeSpanUniforms(byteBuffer, glowCompositeDrawData);
               writePaletteUniforms(byteBuffer);
               byteBuffer.position(0);
               gpuBuffer2 = RenderSystem.getDevice().createBuffer(() -> "greed_glow_shape_params_" + shapeIndex, 136, byteBuffer);
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
            return;
         }

         try {
            CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
            ShapeIndexBuffer sequentialBuffer = RenderSystem.getSequentialBuffer(DrawMode.QUADS);
            GpuBuffer indexBuffer = sequentialBuffer.getIndexBuffer(6);

            try (RenderPass renderPass = commandEncoder.createRenderPass(
                  () -> "greed_glow_shape_" + shapeIndex, simpleFramebuffer.getColorAttachmentView(), OptionalInt.of(0), null, OptionalDouble.empty()
               )) {
               renderPass.setPipeline(pipeline3);
               renderPass.setUniform("GlowParamsArray", gpuBuffer2);
               GpuBuffer gpuBuffer3 = GradientSplitBuffer.getGpuBuffer();
               if (gpuBuffer3 != null) {
                  renderPass.setUniform("SplitParams", gpuBuffer3);
               }

               renderPass.setVertexBuffer(0, gpuBuffer);
               renderPass.setIndexBuffer(indexBuffer, sequentialBuffer.getIndexType());
               renderPass.drawIndexed(0, 0, 6, 1);
            }
         } finally {
            gpuBuffer2.close();
         }
      }
   }

   private void disposeKawaseParamsBuffer() {
      if (this.kawaseParamsBuffer != null) {
         this.kawaseParamsBuffer.close();
         this.kawaseParamsBuffer = null;
      }
   }

   public void updateState6() {
      this.currentDrawContext = null;
   }

   private void runBlurPass(
      RenderPipeline renderPipeline,
      GpuTextureView gpuTextureView,
      int width,
      int height,
      SimpleFramebuffer simpleFramebuffer,
      float radius,
      float rectU,
      float rectV,
      float rectWidth,
      float rectHeight,
      GpuSampler gpuSampler
   ) {
      if (gpuTextureView != null && simpleFramebuffer != null) {
         GpuBuffer gpuBuffer = this.ensureFullscreenQuadBuffer();
         if (gpuBuffer != null) {
            GpuBuffer gpuBuffer2 = this.acquireKawaseParamsBuffer();
            if (gpuBuffer2 != null) {
               CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
               ShapeIndexBuffer sequentialBuffer = RenderSystem.getSequentialBuffer(DrawMode.QUADS);
               GpuBuffer indexBuffer = sequentialBuffer.getIndexBuffer(6);
               MemoryStack memoryStack = MemoryStack.stackPush();

               try {
                  ByteBuffer byteBuffer = memoryStack.calloc(48);
                  byteBuffer.putFloat(0, rectU);
                  byteBuffer.putFloat(4, rectV);
                  byteBuffer.putFloat(8, rectWidth);
                  byteBuffer.putFloat(12, rectHeight);
                  byteBuffer.putFloat(16, radius / Math.max(width, 1));
                  byteBuffer.putFloat(20, radius / Math.max(height, 1));
                  byteBuffer.putFloat(32, 0.0F);
                  byteBuffer.putFloat(36, 0.0F);
                  byteBuffer.putFloat(40, 0.0F);
                  byteBuffer.putFloat(44, 0.0F);
                  byteBuffer.position(0);
                  commandEncoder.writeToBuffer(gpuBuffer2.slice(0L, 48L), byteBuffer);
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

               try (RenderPass renderPass = commandEncoder.createRenderPass(
                     () -> "greed_glow_kawase", simpleFramebuffer.getColorAttachmentView(), OptionalInt.of(0), null, OptionalDouble.empty()
                  )) {
                  renderPass.setPipeline(renderPipeline);
                  renderPass.bindTexture("Sampler0", gpuTextureView, gpuSampler);
                  renderPass.setUniform("KawaseParams", gpuBuffer2);
                  renderPass.setVertexBuffer(0, gpuBuffer);
                  renderPass.setIndexBuffer(indexBuffer, sequentialBuffer.getIndexType());
                  renderPass.drawIndexed(0, 0, 6, 1);
               }
            }
         }
      }
   }

   private static void writeSpanUniforms(ByteBuffer byteBuffer, GlowCompositeDrawData glowCompositeDrawData) {
      float[] floatArray = glowCompositeDrawData.spans();
      int spanCount = glowCompositeDrawData.spanCount();
      if (floatArray != null && spanCount > 0) {
         float padding = glowCompositeDrawData.effectivePad();
         float minRadius = Math.min(
            Math.min(glowCompositeDrawData.radiusTopLeft(), glowCompositeDrawData.radiusTopRight()), Math.min(glowCompositeDrawData.radiusBottomRight(), glowCompositeDrawData.radiusBottomLeft())
         );
         int headerOffset = 80;
         byteBuffer.putFloat(headerOffset, Math.min(spanCount, 64));
         byteBuffer.putFloat(headerOffset + 4, minRadius);
         byteBuffer.putFloat(headerOffset + 8, glowCompositeDrawData.leftAligned());
         byteBuffer.putFloat(headerOffset + 12, glowCompositeDrawData.bottomAnchored() > 0.5F ? 0.0F : 1.0F);
         int spansOffset = headerOffset + 16;
         int clampedSpanCount = Math.min(spanCount, 64);
         float totalHeight = glowCompositeDrawData.height() + padding * 2.0F;

         for (int index = 0; index < clampedSpanCount; index++) {
            int spanByteOffset = index * 4;
            float left = floatArray[spanByteOffset] + padding;
            float top = floatArray[spanByteOffset + 1] + padding;
            float right = floatArray[spanByteOffset + 2] + padding;
            float bottom = floatArray[spanByteOffset + 3] + padding;
            float flippedBottom = totalHeight - bottom;
            float flippedRight = totalHeight - right;
            int spanEntryOffset = spansOffset + index * 4 * 4;
            byteBuffer.putFloat(spanEntryOffset, left);
            byteBuffer.putFloat(spanEntryOffset + 4, top);
            byteBuffer.putFloat(spanEntryOffset + 8, flippedBottom);
            byteBuffer.putFloat(spanEntryOffset + 12, flippedRight);
         }
      }
   }

   public void updateState7() {
      if (!this.activeGlowData.isEmpty() && this.paramsDirty) {
         GpuBuffer gpuBuffer = this.acquireGlowParamsBuffer();
         if (gpuBuffer != null) {
            try {
               MemoryStack memoryStack = MemoryStack.stackPush();

               try {
                  ByteBuffer byteBuffer = this.buildGlowUniformData(memoryStack);
                  RenderSystem.getDevice().createCommandEncoder().writeToBuffer(gpuBuffer.slice(0L, byteBuffer.remaining()), byteBuffer);
                  this.paramsDirty = false;
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
               this.paramsDirty = true;
            }
         }
      }
   }

   public static GlowCompositeRenderer getGlowCompositeRenderer() {
      GlowCompositeRenderer currentGlowCompositeRenderer = glowCompositeRenderer;
      if (currentGlowCompositeRenderer == null) {
         synchronized (GlowCompositeRenderer.class) {
            currentGlowCompositeRenderer = glowCompositeRenderer;
            if (currentGlowCompositeRenderer == null) {
               currentGlowCompositeRenderer = new GlowCompositeRenderer();
               glowCompositeRenderer = currentGlowCompositeRenderer;
            }
         }
      }

      return currentGlowCompositeRenderer;
   }

   public boolean matchesCriteria(RenderPipeline renderPipeline) {
      return renderPipeline == pipeline2;
   }

   private GpuBuffer ensureGlowParamsBuffer() {
      if (!this.paramsDirty && this.glowParamsBuffer != null) {
         return this.glowParamsBuffer;
      }

      this.updateState7();
      if (!this.paramsDirty && this.glowParamsBuffer != null) {
         return this.glowParamsBuffer;
      }

      this.disposeGlowParamsBuffer();
      MemoryStack memoryStack = MemoryStack.stackPush();

      GpuBuffer gpuBuffer;
      try {
         ByteBuffer byteBuffer = this.buildGlowUniformData(memoryStack);
         this.glowParamsBuffer = RenderSystem.getDevice().createBuffer(() -> "greed_glow_params", 128, byteBuffer);
         this.paramsDirty = false;
         gpuBuffer = this.glowParamsBuffer;
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

   private static void writeGlowDrawData(
      ByteBuffer byteBuffer, int index, GlowCompositeDrawData glowCompositeDrawData, float rectU, float rectV, float rectWidth, float rectHeight
   ) {
      int baseOffset = index * 5 * 4 * 4;
      byteBuffer.putFloat(baseOffset, glowCompositeDrawData.radiusTopLeft());
      byteBuffer.putFloat(baseOffset + 4, glowCompositeDrawData.radiusTopRight());
      byteBuffer.putFloat(baseOffset + 8, glowCompositeDrawData.radiusBottomRight());
      byteBuffer.putFloat(baseOffset + 12, glowCompositeDrawData.radiusBottomLeft());
      byteBuffer.putFloat(baseOffset + 16, glowCompositeDrawData.width());
      byteBuffer.putFloat(baseOffset + 20, glowCompositeDrawData.height());
      byteBuffer.putFloat(baseOffset + 24, glowCompositeDrawData.effectivePad());
      byteBuffer.putFloat(baseOffset + 28, 2.0F);
      int color = glowCompositeDrawData.color();
      byteBuffer.putFloat(baseOffset + 32, (color >>> 16 & 0xFF) / 255.0F);
      byteBuffer.putFloat(baseOffset + 36, (color >>> 8 & 0xFF) / 255.0F);
      byteBuffer.putFloat(baseOffset + 40, (color & 0xFF) / 255.0F);
      byteBuffer.putFloat(baseOffset + 44, glowCompositeDrawData.intensity() * glowCompositeDrawData.alpha());
      byteBuffer.putFloat(baseOffset + 48, rectU);
      byteBuffer.putFloat(baseOffset + 52, rectV);
      byteBuffer.putFloat(baseOffset + 56, rectWidth);
      byteBuffer.putFloat(baseOffset + 60, rectHeight);
      int secondColor = glowCompositeDrawData.secondColor();
      byteBuffer.putFloat(baseOffset + 64, (secondColor >>> 16 & 0xFF) / 255.0F);
      byteBuffer.putFloat(baseOffset + 68, (secondColor >>> 8 & 0xFF) / 255.0F);
      byteBuffer.putFloat(baseOffset + 72, (secondColor & 0xFF) / 255.0F);
      byteBuffer.putFloat(baseOffset + 76, glowCompositeDrawData.colorOffset());
   }

   public void processData11(RenderPass renderPass) {
      if (renderPass != null && !this.activeGlowData.isEmpty()) {
         GpuBuffer gpuBuffer = this.ensureGlowParamsBuffer();
         if (gpuBuffer != null) {
            renderPass.setUniform("GlowParamsArray", gpuBuffer);
         }
      }
   }

   private void runCutoutPass(SimpleFramebuffer simpleFramebuffer, SimpleFramebuffer simpleFramebuffer2, SimpleFramebuffer simpleFramebuffer3, GpuSampler gpuSampler) {
      if (simpleFramebuffer != null && simpleFramebuffer2 != null && simpleFramebuffer3 != null) {
         GpuBuffer gpuBuffer = this.ensureFullscreenQuadBuffer();
         GpuBuffer gpuBuffer2 = this.acquireKawaseParamsBuffer();
         if (gpuBuffer != null && gpuBuffer2 != null) {
            CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
            ShapeIndexBuffer sequentialBuffer = RenderSystem.getSequentialBuffer(DrawMode.QUADS);
            GpuBuffer indexBuffer = sequentialBuffer.getIndexBuffer(6);
            MemoryStack memoryStack = MemoryStack.stackPush();

            try {
               ByteBuffer byteBuffer = memoryStack.calloc(48);
               byteBuffer.putFloat(0, 0.0F);
               byteBuffer.putFloat(4, 0.0F);
               byteBuffer.putFloat(8, 1.0F);
               byteBuffer.putFloat(12, 1.0F);
               byteBuffer.position(0);
               commandEncoder.writeToBuffer(gpuBuffer2.slice(0L, 48L), byteBuffer);
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

            try (RenderPass renderPass = commandEncoder.createRenderPass(
                  () -> "greed_glow_cutout", simpleFramebuffer3.getColorAttachmentView(), OptionalInt.of(0), null, OptionalDouble.empty()
               )) {
               renderPass.setPipeline(pipeline7);
               renderPass.bindTexture("Sampler0", simpleFramebuffer.getColorAttachmentView(), gpuSampler);
               renderPass.bindTexture("Sampler1", simpleFramebuffer2.getColorAttachmentView(), gpuSampler);
               renderPass.setUniform("KawaseParams", gpuBuffer2);
               renderPass.setVertexBuffer(0, gpuBuffer);
               renderPass.setIndexBuffer(indexBuffer, sequentialBuffer.getIndexType());
               renderPass.drawIndexed(0, 0, 6, 1);
            }
         }
      }
   }

   public void processData13(GlowCompositeDrawData glowCompositeDrawData) {
      this.enqueueGlowElement(this.currentDrawContext, glowCompositeDrawData);
   }

   private SimpleFramebuffer resizeOrCreateFramebuffer(SimpleFramebuffer simpleFramebuffer, String name, int width, int height) {
      if (simpleFramebuffer == null) {
         return new SimpleFramebuffer(name, width, height, false);
      }

      if (simpleFramebuffer.textureWidth != width || simpleFramebuffer.textureHeight != height) {
         simpleFramebuffer.resize(width, height);
      }

      return simpleFramebuffer;
   }

   private GpuBuffer acquireSourceParamsBuffer(int requiredBytes) {
      if (this.sourceParamsBuffer == null || this.sourceParamsBuffer.isClosed() || this.sourceParamsBuffer.size() < requiredBytes) {
         if (this.sourceParamsBuffer != null) {
            this.sourceParamsBuffer.close();
         }

         int bufferSize = Integer.highestOneBit(Math.max(requiredBytes, 2) - 1) << 1;
         this.sourceParamsBuffer = RenderSystem.getDevice().createBuffer(() -> "greed_glow_src_params", 136, bufferSize);
      }

      return this.sourceParamsBuffer;
   }

   private ByteBuffer getOrGrowUniformBuffer(int requiredBytes) {
      if (this.uniformScratchBuffer == null || this.uniformScratchBuffer.capacity() < requiredBytes) {
         if (this.uniformScratchBuffer != null) {
            MemoryUtil.memFree(this.uniformScratchBuffer);
         }

         this.uniformScratchBuffer = MemoryUtil.memAlloc(requiredBytes);
      }

      this.uniformScratchBuffer.clear();
      return this.uniformScratchBuffer;
   }

   private SimpleFramebuffer getResultFramebuffer(int index, int width, int height) {
      while (this.resultFramebuffers.size() <= index) {
         this.resultFramebuffers.add(null);
      }

      SimpleFramebuffer simpleFramebuffer = this.resizeOrCreateFramebuffer(this.resultFramebuffers.get(index), "greed_glow_result_" + index, width, height);
      this.resultFramebuffers.set(index, simpleFramebuffer);
      return simpleFramebuffer;
   }

   private GpuBuffer ensureFullscreenQuadBuffer() {
      if (this.fullscreenQuadBuffer != null) {
         return this.fullscreenQuadBuffer;
      }

      try (
         BufferAllocator bufferAllocator = new BufferAllocator(4 * VertexFormats.POSITION.getVertexSize());
         BuiltBuffer builtBuffer = this.getBuiltBuffer(bufferAllocator);
      ) {
         this.fullscreenQuadBuffer = RenderSystem.getDevice().createBuffer(() -> "greed_glow_fullscreen_quad", 32, builtBuffer.getBuffer());
      }

      return this.fullscreenQuadBuffer;
   }

   private record DataRecord(GlowCompositeDrawData glow, GlowDrawBatch capture, Matrix3x2f pose) {
   }
}
