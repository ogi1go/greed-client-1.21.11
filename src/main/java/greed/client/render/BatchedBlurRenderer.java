package greed.client.render;

import greed.mixin.DrawContextMixin;
import greed.client.util.GuiScale;
import greed.client.render.BlurDrawBatch;
import greed.client.render.ScissorStack;
import greed.client.render.GuiEffectsState;
import greed.client.render.BlurDrawData;

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
import java.util.Arrays;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.Window;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;
import org.lwjgl.system.MemoryStack;

public final class BatchedBlurRenderer implements AutoCloseable {
   private static volatile BatchedBlurRenderer batchedBlurRenderer;
   private static float tintRed = 0.55F;
   private static float tintGreen = 0.65F;
   private static float tintBlue = 0.78F;
   private static float tintAlpha;
   public static final RenderPipeline pipeline2 = RenderPipeline.builder()
      .withLocation(createIdentifier("pipeline/batched_blur"))
      .withVertexShader(createIdentifier("ui/batched_blur/batched_blur"))
      .withFragmentShader(createIdentifier("ui/batched_blur/batched_blur"))
      .withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("BlurParamsArray", UniformType.UNIFORM_BUFFER)
      .build();
   public static final RenderPipeline pipeline3 = RenderPipeline.builder()
      .withLocation(createIdentifier("pipeline/kawase_down"))
      .withVertexShader(createIdentifier("ui/kawase/down"))
      .withFragmentShader(createIdentifier("ui/kawase/down"))
      .withVertexFormat(VertexFormats.POSITION, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("KawaseParams", UniformType.UNIFORM_BUFFER)
      .build();
   public static final RenderPipeline pipeline4 = RenderPipeline.builder()
      .withLocation(createIdentifier("pipeline/kawase_up"))
      .withVertexShader(createIdentifier("ui/kawase/down"))
      .withFragmentShader(createIdentifier("ui/kawase/up"))
      .withVertexFormat(VertexFormats.POSITION, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("KawaseParams", UniformType.UNIFORM_BUFFER)
      .build();
   public static final RenderPipeline pipeline5 = RenderPipeline.builder()
      .withLocation(createIdentifier("pipeline/gaussian_blur"))
      .withVertexShader(createIdentifier("ui/kawase/down"))
      .withFragmentShader(createIdentifier("ui/kawase/gaussian"))
      .withVertexFormat(VertexFormats.POSITION, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("KawaseParams", UniformType.UNIFORM_BUFFER)
      .build();
   private final List<BatchedBlurRenderer.SubContext3> blurContexts = new ArrayList<>(32);
   private final List<BatchedBlurRenderer.DataRecord> drawRecords = new ArrayList<>(32);
   private DrawContext drawContext2;
   private final BatchedBlurRenderer.SubContext2 resultPool = new BatchedBlurRenderer.SubContext2("greed_blur_result");
   private final BatchedBlurRenderer.SubContext2 scratchPool = new BatchedBlurRenderer.SubContext2("greed_blur_scratch");
   private final SimpleFramebuffer[] framebufferArray = new SimpleFramebuffer[4];
   private final SimpleFramebuffer[] framebufferArray2 = new SimpleFramebuffer[4];
   private SimpleFramebuffer framebuffer2;
   private long frameCounter;
   private long levelFrameCounter;
   private boolean blurBelowLayer;
   private boolean blurAboveLayer;
   private boolean heavyPassPending = true;
   private long globalBlurTimestamp;
   private long secondaryBlurTimestamp;
   private long stratumBlurTimestamp;
   private long heavyBlurTimestamp;
   private SimpleFramebuffer framebuffer3;
   private boolean globalBlurReady;
   private float cachedGlobalRadius;
   private SimpleFramebuffer framebuffer4;
   private boolean secondaryBlurReady;
   private float cachedSecondaryRadius;
   private int blurMode;
   private SimpleFramebuffer framebuffer5;
   private SimpleFramebuffer framebuffer6;
   private SimpleFramebuffer framebuffer7;
   private GpuBuffer gpuBuffer3;
   private GpuBuffer gpuBuffer4;
   private GpuBuffer gpuBuffer5;
   private boolean hasBlurContexts;
   private boolean heavyBlurReady;
   private boolean useGlobalTint;
   private boolean paramsDirty = true;
   private int activeLevelCount;

   private void enqueueBlurContext(DrawContext drawContext, BlurDrawData newBlurDrawData) {
      if (newBlurDrawData != null && newBlurDrawData.visible()) {
         if (drawContext != null) {
            try {
               BlurDrawData currentBlurDrawData = this.normalizeBlurDrawData(newBlurDrawData);
               Matrix3x2f matrix3x2f = GuiScale.renderElement(drawContext);
               ScreenRect screenRect = ScissorStack.getScreenRect();
               BlurDrawBatch currentBlurDrawBatch = new BlurDrawBatch();
               this.blurContexts.add(new BatchedBlurRenderer.SubContext3(currentBlurDrawData, matrix3x2f, screenRect, currentBlurDrawBatch, false));
               ((DrawContextMixin)drawContext).getGuiRenderState().addSimpleElement(new BlurRenderState(matrix3x2f, currentBlurDrawData, screenRect, currentBlurDrawBatch));
            } catch (RuntimeException exception) {
            }
         }
      }
   }

   public void processData(BlurDrawData newBlurDrawData) {
      DrawContext drawContext = this.drawContext2;
      if (newBlurDrawData != null && newBlurDrawData.visible() && drawContext != null) {
         try {
            BlurDrawData currentBlurDrawData = this.normalizeBlurDrawData(newBlurDrawData);
            Matrix3x2f matrix3x2f = GuiScale.renderElement(drawContext);
            ScreenRect screenRect = ScissorStack.getScreenRect();
            BlurDrawBatch currentBlurDrawBatch = new BlurDrawBatch();
            this.blurContexts.add(new BatchedBlurRenderer.SubContext3(currentBlurDrawData, matrix3x2f, screenRect, currentBlurDrawBatch, false, true));
            ((DrawContextMixin)drawContext).getGuiRenderState().addSimpleElement(new BlurRenderState(matrix3x2f, currentBlurDrawData, screenRect, currentBlurDrawBatch));
         } catch (RuntimeException exception) {
         }
      }
   }

   private static int clampInt(int clampValue, int clampMin, int clampMax) {
      return Math.max(clampMin, Math.min(clampMax, clampValue));
   }

   private void closeBlurParamsBuffer() {
      if (this.gpuBuffer4 != null) {
         this.gpuBuffer4.close();
         this.gpuBuffer4 = null;
      }
   }

   private static int ceilToMultiple(int sourceValue, int multipleOf) {
      return -Math.floorDiv(-sourceValue, multipleOf) * multipleOf;
   }

   private void runBlurPipeline(
      int layerId, Framebuffer framebuffer, SimpleFramebuffer simpleFramebuffer, BatchedBlurRenderer.DataRecord5 blurArea, float blurRadiusValue
   ) {
      if (simpleFramebuffer != null && blurArea != null) {
         int levelCount = this.activeLevelCount > 0 ? this.activeLevelCount : this.resolveLevelCount(blurRadiusValue);
         this.useGlobalTint = layerId == 65 || layerId == 66;
         this.prepareBlurLevels(simpleFramebuffer.textureWidth, simpleFramebuffer.textureHeight, levelCount);
         SimpleFramebuffer[] simpleFramebufferArray = this.framebufferArray;
         SimpleFramebuffer[] simpleFramebufferArray2 = this.framebufferArray2;
         float kawaseOffset = 1.0F;
         GpuSampler gpuSampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
         GpuTextureView gpuTextureView = framebuffer.getColorAttachmentView();
         int levelWidth = framebuffer.textureWidth;
         int levelHeight = framebuffer.textureHeight;
         float uvLeft = (float)blurArea.x() / Math.max(framebuffer.textureWidth, 1);
         float uvTop = (float)blurArea.y() / Math.max(framebuffer.textureHeight, 1);
         float uvRight = (float)blurArea.width() / Math.max(framebuffer.textureWidth, 1);
         float uvBottom = (float)blurArea.height() / Math.max(framebuffer.textureHeight, 1);

         for (int index = 0; index < levelCount; index++) {
            SimpleFramebuffer simpleFramebuffer2 = simpleFramebufferArray[index];
            this.runKawasePass(
               pipeline3,
               gpuTextureView,
               levelWidth,
               levelHeight,
               simpleFramebuffer2,
               kawaseOffset,
               uvLeft,
               uvTop,
               uvRight,
               uvBottom,
               gpuSampler
            );
            gpuTextureView = simpleFramebuffer2.getColorAttachmentView();
            levelWidth = simpleFramebuffer2.textureWidth;
            levelHeight = simpleFramebuffer2.textureHeight;
            uvLeft = 0.0F;
            uvTop = 0.0F;
            uvRight = 1.0F;
            uvBottom = 1.0F;
         }

         SimpleFramebuffer simpleFramebuffer3 = simpleFramebufferArray[levelCount - 1];
         SimpleFramebuffer simpleFramebuffer4 = this.framebuffer2;
         if (simpleFramebuffer3 != null && simpleFramebuffer4 != null && simpleFramebuffer4.getColorAttachmentView() != null) {
            float baseBlurPixels = blurRadiusValue * 0.5F * (Math.max(framebuffer.textureHeight, 1) / 1080.0F);
            float gaussianRadius = baseBlurPixels * ((float)simpleFramebuffer3.textureHeight / Math.max(blurArea.height(), 1));
            gaussianRadius = clampFloat(gaussianRadius, 0.6F, 12.0F);
            int gaussianSamples = Math.min(31, Math.max(2, (int)Math.ceil(gaussianRadius * 3.0F)));
            boolean heavyPass = layerId == 66;
            this.submitGaussianDraw(
               simpleFramebuffer3.getColorAttachmentView(),
               simpleFramebuffer4.getColorAttachmentView(),
               1.0F / Math.max(simpleFramebuffer3.textureWidth, 1),
               0.0F,
               gaussianRadius,
               gaussianSamples,
               1.0F,
               1.0F,
               0.0F,
               gpuSampler
            );
            this.submitGaussianDraw(
               simpleFramebuffer4.getColorAttachmentView(),
               simpleFramebuffer3.getColorAttachmentView(),
               0.0F,
               1.0F / Math.max(simpleFramebuffer3.textureHeight, 1),
               gaussianRadius,
               gaussianSamples,
               heavyPass ? 1.25F : 1.0F,
               heavyPass ? 1.0F : 1.0F,
               heavyPass ? 0.78F : 0.0F,
               gpuSampler
            );
            gpuTextureView = simpleFramebuffer3.getColorAttachmentView();
            levelWidth = simpleFramebuffer3.textureWidth;
            levelHeight = simpleFramebuffer3.textureHeight;
         }

         for (int index2 = levelCount - 2; index2 >= 0; index2--) {
            SimpleFramebuffer simpleFramebuffer5 = simpleFramebufferArray2[index2];
            this.runKawasePass(pipeline4, gpuTextureView, levelWidth, levelHeight, simpleFramebuffer5, kawaseOffset, 0.0F, 0.0F, 1.0F, 1.0F, gpuSampler);
            gpuTextureView = simpleFramebuffer5.getColorAttachmentView();
            levelWidth = simpleFramebuffer5.textureWidth;
            levelHeight = simpleFramebuffer5.textureHeight;
         }

         this.runKawasePass(pipeline4, gpuTextureView, levelWidth, levelHeight, simpleFramebuffer, kawaseOffset, 0.0F, 0.0F, 1.0F, 1.0F, gpuSampler);
      }
   }

   private SimpleFramebuffer getHeavyStratumBuffer(int sourceWidth, int sourceHeight) {
      int heavyWidth = Math.max(Math.round(sourceWidth * 0.25F), 1);
      int heavyHeight = Math.max(Math.round(sourceHeight * 0.25F), 1);
      this.framebuffer6 = this.resizeOrCreateBuffer(this.framebuffer6, "greed_blur_stratum_heavy", heavyWidth, heavyHeight);
      return this.framebuffer6;
   }

   public void updateState2() {
      if (!this.blurContexts.isEmpty()) {
         MinecraftClient minecraftClient = MinecraftClient.getInstance();
         Framebuffer framebuffer = minecraftClient == null ? null : minecraftClient.getFramebuffer();
         if (framebuffer != null && framebuffer.getColorAttachment() != null) {
            this.activeLevelCount = this.resolveLevelCount(0.0F);
            long frameStartNanos = System.nanoTime();
            long frameId = ++this.frameCounter;
            GpuSampler gpuSampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
            this.globalBlurReady = false;
            this.secondaryBlurReady = false;
            float globalBlurRadius = -1.0F;
            float secondaryBlurRadius = -1.0F;

            for (BatchedBlurRenderer.SubContext3 firstContext : this.blurContexts) {
               if (firstContext.isGlobalBlur()) {
                  if (firstContext.getLayerIndex() == 0) {
                     globalBlurRadius = Math.max(globalBlurRadius, firstContext.getBlurDrawData().blurRadius());
                  } else {
                     secondaryBlurRadius = Math.max(secondaryBlurRadius, firstContext.getBlurDrawData().blurRadius());
                  }
               }
            }

            if (globalBlurRadius >= 0.0F) {
               float globalScale = this.computeBlurScale(globalBlurRadius, framebuffer.textureWidth, framebuffer.textureHeight);
               int globalWidth = Math.max(Math.round(framebuffer.textureWidth * globalScale), 1);
               int globalHeight = Math.max(Math.round(framebuffer.textureHeight * globalScale), 1);
               this.framebuffer3 = this.resizeOrCreateBuffer(this.framebuffer3, "greed_blur_global", globalWidth, globalHeight);
               this.globalBlurTimestamp = frameStartNanos;
               if (this.framebuffer3 != null && this.framebuffer3.getColorAttachmentView() != null) {
                  this.globalBlurReady = true;
                  this.cachedGlobalRadius = globalBlurRadius;
                  TextureSetup textureSetup = TextureSetup.of(this.framebuffer3.getColorAttachmentView(), gpuSampler);

                  for (BatchedBlurRenderer.SubContext3 globalContext : this.blurContexts) {
                     if (globalContext.isGlobalBlur() && globalContext.getLayerIndex() == 0) {
                        BlurDrawBatch currentBlurDrawBatch = globalContext.getBlurDrawBatch();
                        currentBlurDrawBatch.floatVal = 0.0F;
                        currentBlurDrawBatch.floatVal2 = 0.0F;
                        currentBlurDrawBatch.floatVal3 = framebuffer.textureWidth;
                        currentBlurDrawBatch.floatVal4 = framebuffer.textureHeight;
                        currentBlurDrawBatch.textureSetup = textureSetup;
                     }
                  }

                  this.hasBlurContexts = true;
               }
            }

            if (secondaryBlurRadius >= 0.0F) {
               float secondaryScale = this.computeBlurScale(secondaryBlurRadius, framebuffer.textureWidth, framebuffer.textureHeight);
               int secondaryWidth = Math.max(Math.round(framebuffer.textureWidth * secondaryScale), 1);
               int secondaryHeight = Math.max(Math.round(framebuffer.textureHeight * secondaryScale), 1);
               this.framebuffer4 = this.resizeOrCreateBuffer(this.framebuffer4, "greed_blur_global2", secondaryWidth, secondaryHeight);
               this.secondaryBlurTimestamp = frameStartNanos;
               if (this.framebuffer4 != null && this.framebuffer4.getColorAttachmentView() != null) {
                  this.secondaryBlurReady = true;
                  this.cachedSecondaryRadius = secondaryBlurRadius;
                  TextureSetup textureSetup2 = TextureSetup.of(this.framebuffer4.getColorAttachmentView(), gpuSampler);

                  for (BatchedBlurRenderer.SubContext3 secondaryContext : this.blurContexts) {
                     if (secondaryContext.isGlobalBlur() && secondaryContext.getLayerIndex() == 1) {
                        BlurDrawBatch currentBlurDrawBatch2 = secondaryContext.getBlurDrawBatch();
                        currentBlurDrawBatch2.floatVal = 0.0F;
                        currentBlurDrawBatch2.floatVal2 = 0.0F;
                        currentBlurDrawBatch2.floatVal3 = framebuffer.textureWidth;
                        currentBlurDrawBatch2.floatVal4 = framebuffer.textureHeight;
                        currentBlurDrawBatch2.textureSetup = textureSetup2;
                     }
                  }

                  this.hasBlurContexts = true;
               }
            }

            boolean hasSecondary = false;

            for (BatchedBlurRenderer.SubContext3 secondaryScanContext : this.blurContexts) {
               if (secondaryScanContext.isSecondaryBlur()) {
                  hasSecondary = true;
                  break;
               }
            }

            if (hasSecondary) {
               SimpleFramebuffer simpleFramebuffer = this.getHeavyStratumBuffer(framebuffer.textureWidth, framebuffer.textureHeight);
               this.heavyBlurTimestamp = frameStartNanos;
               if (simpleFramebuffer != null && simpleFramebuffer.getColorAttachmentView() != null) {
                  this.heavyBlurReady = true;
                  TextureSetup textureSetup3 = TextureSetup.of(simpleFramebuffer.getColorAttachmentView(), gpuSampler);

                  for (BatchedBlurRenderer.SubContext3 heavyContext : this.blurContexts) {
                     if (heavyContext.isSecondaryBlur()) {
                        BlurDrawBatch currentBlurDrawBatch3 = heavyContext.getBlurDrawBatch();
                        currentBlurDrawBatch3.floatVal = 0.0F;
                        currentBlurDrawBatch3.floatVal2 = 0.0F;
                        currentBlurDrawBatch3.floatVal3 = framebuffer.textureWidth;
                        currentBlurDrawBatch3.floatVal4 = framebuffer.textureHeight;
                        currentBlurDrawBatch3.textureSetup = textureSetup3;
                     }
                  }

                  this.hasBlurContexts = true;
               }
            }

            for (int index = 0; index < this.blurContexts.size() && index < 64; index++) {
               BatchedBlurRenderer.SubContext3 pendingContext = this.blurContexts.get(index);
               if (!pendingContext.isGlobalBlur() && !pendingContext.isSecondaryBlur()) {
                  BlurDrawBatch currentBlurDrawBatch4 = pendingContext.getBlurDrawBatch();
                  BatchedBlurRenderer.DataRecord5 pendingRegion = this.computeBlurRegion(pendingContext, minecraftClient, framebuffer);
                  if (pendingRegion != null) {
                     float pendingScale = this.computeBlurScale(pendingContext.getBlurDrawData().blurRadius(), pendingRegion.width(), pendingRegion.height());
                     int pendingWidth = Math.max(Math.round(pendingRegion.width() * pendingScale), 1);
                     int pendingHeight = Math.max(Math.round(pendingRegion.height() * pendingScale), 1);
                     SimpleFramebuffer simpleFramebuffer2 = this.resultPool.acquirePooledBuffer(pendingWidth, pendingHeight, frameId, frameStartNanos);
                     if (simpleFramebuffer2 != null && simpleFramebuffer2.getColorAttachmentView() != null) {
                        pendingContext.setFramebuffer(simpleFramebuffer2);
                        pendingContext.setBlurRegion(pendingRegion);
                        currentBlurDrawBatch4.floatVal = pendingRegion.x();
                        currentBlurDrawBatch4.floatVal2 = pendingRegion.y();
                        currentBlurDrawBatch4.floatVal3 = pendingRegion.width();
                        currentBlurDrawBatch4.floatVal4 = pendingRegion.height();
                        currentBlurDrawBatch4.textureSetup = TextureSetup.of(simpleFramebuffer2.getColorAttachmentView(), gpuSampler);
                        this.hasBlurContexts = true;
                     }
                  }
               }
            }
         } else {
            this.blurContexts.clear();
         }
      }
   }

   private GpuBuffer acquireScratchParamsBuffer() {
      if (this.gpuBuffer4 != null && !this.gpuBuffer4.isClosed() && this.gpuBuffer4.size() >= 24576L) {
         return this.gpuBuffer4;
      }

      this.closeBlurParamsBuffer();

      try {
         this.gpuBuffer4 = RenderSystem.getDevice().createBuffer(() -> "greed_blur_params", 136, 24576L);
         return this.gpuBuffer4;
      } catch (RuntimeException exception) {
         return null;
      }
   }

   private void runKawasePass(
      RenderPipeline renderPipeline,
      GpuTextureView gpuTextureView,
      int passWidth,
      int passHeight,
      SimpleFramebuffer simpleFramebuffer,
      float passOffset,
      float passUvLeft,
      float passUvTop,
      float passUvRight,
      float passUvBottom,
      GpuSampler gpuSampler
   ) {
      if (simpleFramebuffer != null) {
         this.submitKawaseDraw(
            renderPipeline,
            gpuTextureView,
            passWidth,
            passHeight,
            simpleFramebuffer.getColorAttachmentView(),
            passOffset,
            passUvLeft,
            passUvTop,
            passUvRight,
            passUvBottom,
            gpuSampler,
            true,
            1.0F
         );
      }
   }

   private BlurDrawData normalizeBlurDrawData(BlurDrawData newBlurDrawData) {
      float maxCornerRadius = Math.max(0.0F, Math.min(newBlurDrawData.width(), newBlurDrawData.height()) * 0.5F);
      float clampedTopLeft = clampFloat(newBlurDrawData.radiusTopLeft(), 0.0F, maxCornerRadius);
      float clampedTopRight = clampFloat(newBlurDrawData.radiusTopRight(), 0.0F, maxCornerRadius);
      float clampedBottomRight = clampFloat(newBlurDrawData.radiusBottomRight(), 0.0F, maxCornerRadius);
      float clampedBottomLeft = clampFloat(newBlurDrawData.radiusBottomLeft(), 0.0F, maxCornerRadius);
      float clampedSmoothness = Math.max(0.0F, newBlurDrawData.smoothness());
      float clampedBlurRadius = Math.max(0.0F, newBlurDrawData.blurRadius());
      return clampedTopLeft == newBlurDrawData.radiusTopLeft()
            && clampedTopRight == newBlurDrawData.radiusTopRight()
            && clampedBottomRight == newBlurDrawData.radiusBottomRight()
            && clampedBottomLeft == newBlurDrawData.radiusBottomLeft()
            && clampedSmoothness == newBlurDrawData.smoothness()
            && clampedBlurRadius == newBlurDrawData.blurRadius()
         ? newBlurDrawData
         : new BlurDrawData(
            newBlurDrawData.x(),
            newBlurDrawData.y(),
            newBlurDrawData.width(),
            newBlurDrawData.height(),
            clampedTopLeft,
            clampedTopRight,
            clampedBottomRight,
            clampedBottomLeft,
            clampedSmoothness,
            clampedBlurRadius,
            newBlurDrawData.colorTopLeft(),
            newBlurDrawData.colorTopRight(),
            newBlurDrawData.colorBottomRight(),
            newBlurDrawData.colorBottomLeft()
         );
   }

   public void updateState3() {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      Framebuffer framebuffer = minecraftClient == null ? null : minecraftClient.getFramebuffer();
      if (framebuffer != null
         && framebuffer.getColorAttachmentView() != null
         && this.heavyBlurReady
         && this.framebuffer6 != null
         && this.framebuffer6.getColorAttachmentView() != null) {
         if (this.heavyPassPending) {
            this.heavyPassPending = false;
            this.runBlurPipeline(
               66, framebuffer, this.framebuffer6, new BatchedBlurRenderer.DataRecord5(0, 0, framebuffer.textureWidth, framebuffer.textureHeight), 72.0F
            );
         }
      }
   }

   public void updateState4() {
      this.processData8(Integer.MAX_VALUE, 0);
   }

   public void updateState5() {
      if (!this.drawRecords.isEmpty() && this.paramsDirty) {
         GpuBuffer gpuBuffer = this.acquireScratchParamsBuffer();
         if (gpuBuffer != null) {
            try {
               MemoryStack memoryStack = MemoryStack.stackPush();

               try {
                  ByteBuffer byteBuffer = this.packDrawRecords(memoryStack, this.drawRecords);
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

   public void renderElement2(DrawContext drawContext, BlurDrawData newBlurDrawData) {
      this.renderElement3(drawContext);
      this.processData14(newBlurDrawData);
      this.updateState10();
   }

   private static float clampFloat(float floatValue, float floatMin, float floatMax) {
      return Math.max(floatMin, Math.min(floatMax, floatValue));
   }

   public void renderElement3(DrawContext drawContext) {
      this.drawContext2 = drawContext;
   }

   private BatchedBlurRenderer.DataRecord5 computeBlurRegion(BatchedBlurRenderer.SubContext3 candidateContext, MinecraftClient minecraftClient, Framebuffer framebuffer) {
      Window window = minecraftClient.getWindow();
      int scaledWidth = Math.max(window.getScaledWidth(), 1);
      int scaledHeight = Math.max(window.getScaledHeight(), 1);
      float scaleX = (float)framebuffer.textureWidth / scaledWidth;
      float scaleY = (float)framebuffer.textureHeight / scaledHeight;
      BlurDrawData currentBlurDrawData = candidateContext.getBlurDrawData();
      float padding = GuiScale.computeFloat4(Math.max(16.0F, currentBlurDrawData.blurRadius() * 2.0F));
      ScreenRect screenRect = new ScreenRect(
            Math.round(currentBlurDrawData.x()), Math.round(currentBlurDrawData.y()), Math.round(currentBlurDrawData.width()), Math.round(currentBlurDrawData.height())
         )
         .transformEachVertex(candidateContext.getMatrix3x2f());
      if (candidateContext.getScreenRect() != null) {
         screenRect = candidateContext.getScreenRect().intersection(screenRect);
         if (screenRect == null) {
            return null;
         }
      }

      float regionLeft = clampFloat(screenRect.getLeft() - padding, 0.0F, scaledWidth);
      float regionTop = clampFloat(screenRect.getTop() - padding, 0.0F, scaledHeight);
      float regionRight = clampFloat(screenRect.getRight() + padding, regionLeft + 1.0F, scaledWidth);
      float regionBottom = clampFloat(screenRect.getBottom() + padding, regionTop + 1.0F, scaledHeight);
      int alignSize = 64;
      int regionMinX = clampInt(floorToMultiple((int)Math.floor(regionLeft * scaleX), alignSize), 0, framebuffer.textureWidth - 1);
      int regionMaxX = clampInt(ceilToMultiple((int)Math.ceil(regionRight * scaleX), alignSize), regionMinX + 1, framebuffer.textureWidth);
      int regionMinY = clampInt(floorToMultiple((int)Math.floor((scaledHeight - regionBottom) * scaleY), alignSize), 0, framebuffer.textureHeight - 1);
      int regionMaxY = clampInt(
         ceilToMultiple((int)Math.ceil((scaledHeight - regionTop) * scaleY), alignSize), regionMinY + 1, framebuffer.textureHeight
      );
      int regionWidth = Math.max(regionMaxX - regionMinX, 1);
      int regionHeight = Math.max(regionMaxY - regionMinY, 1);
      return new BatchedBlurRenderer.DataRecord5(regionMinX, regionMinY, regionWidth, regionHeight);
   }

   private void prepareBlurLevels(int targetWidth, int targetHeight, int mipCount) {
      long levelFrameId = ++this.levelFrameCounter;
      long levelStartNanos = System.nanoTime();
      int currentLevelWidth = Math.max(targetWidth, 1);
      int currentLevelHeight = Math.max(targetHeight, 1);
      int lastLevelWidth = currentLevelWidth;
      int lastLevelHeight = currentLevelHeight;
      Arrays.fill(this.framebufferArray, null);
      Arrays.fill(this.framebufferArray2, null);

      for (int index = 0; index < mipCount; index++) {
         this.framebufferArray[index] = this.scratchPool.acquirePooledBuffer(currentLevelWidth, currentLevelHeight, levelFrameId, levelStartNanos);
         if (index < mipCount - 1) {
            this.framebufferArray2[index] = this.scratchPool.acquirePooledBuffer(currentLevelWidth, currentLevelHeight, levelFrameId, levelStartNanos);
         }

         lastLevelWidth = currentLevelWidth;
         lastLevelHeight = currentLevelHeight;
         currentLevelWidth = Math.max(currentLevelWidth / 2, 1);
         currentLevelHeight = Math.max(currentLevelHeight / 2, 1);
      }

      this.framebuffer2 = this.scratchPool.acquirePooledBuffer(lastLevelWidth, lastLevelHeight, levelFrameId, levelStartNanos);
   }

   public void updateState6() {
      this.drawRecords.clear();
      this.blurBelowLayer = false;
      this.blurAboveLayer = false;
      this.heavyPassPending = true;
      this.releaseExpiredBlurBuffers(System.nanoTime());
      this.hasBlurContexts = false;
      this.heavyBlurReady = false;
      this.secondaryBlurReady = false;
      this.blurMode = 0;
      this.paramsDirty = false;
      this.activeLevelCount = 0;
   }

   private GpuBuffer getKawaseParamsBuffer() {
      if (this.gpuBuffer5 == null || this.gpuBuffer5.isClosed() || this.gpuBuffer5.size() < 64L) {
         this.closeKawaseParamsBuffer();
         this.gpuBuffer5 = RenderSystem.getDevice().createBuffer(() -> "greed_kawase_params", 136, 64L);
      }

      return this.gpuBuffer5;
   }

   @Override
   public void close() {
      this.blurContexts.clear();
      this.drawRecords.clear();
      this.drawContext2 = null;
      this.hasBlurContexts = false;
      this.closeBlurParamsBuffer();
      this.closeKawaseParamsBuffer();
      this.resultPool.releaseAllFramebuffers();
      this.scratchPool.releaseAllFramebuffers();
      Arrays.fill(this.framebufferArray, null);
      Arrays.fill(this.framebufferArray2, null);
      this.framebuffer2 = null;
      if (this.framebuffer3 != null) {
         this.framebuffer3.delete();
         this.framebuffer3 = null;
      }

      if (this.framebuffer4 != null) {
         this.framebuffer4.delete();
         this.framebuffer4 = null;
      }

      if (this.framebuffer5 != null) {
         this.framebuffer5.delete();
         this.framebuffer5 = null;
      }

      if (this.framebuffer6 != null) {
         this.framebuffer6.delete();
         this.framebuffer6 = null;
      }

      if (this.framebuffer7 != null) {
         this.framebuffer7.delete();
         this.framebuffer7 = null;
      }

      this.globalBlurReady = false;
      if (this.gpuBuffer3 != null) {
         this.gpuBuffer3.close();
         this.gpuBuffer3 = null;
      }
   }

   public void processData5(RenderPass renderPass) {
      if (renderPass != null && !this.drawRecords.isEmpty()) {
         GpuBuffer gpuBuffer = this.getBlurParamsBuffer();
         if (gpuBuffer != null) {
            renderPass.setUniform("BlurParamsArray", gpuBuffer);
         }
      }
   }

   private GpuBuffer getFullscreenQuadBuffer() {
      if (this.gpuBuffer3 != null) {
         return this.gpuBuffer3;
      }

      try (
         BufferAllocator bufferAllocator = new BufferAllocator(4 * VertexFormats.POSITION.getVertexSize());
         BuiltBuffer builtBuffer = this.buildFullscreenQuad(bufferAllocator);
      ) {
         this.gpuBuffer3 = RenderSystem.getDevice().createBuffer(() -> "greed_blur_fullscreen_quad", 32, builtBuffer.getBuffer());
         return this.gpuBuffer3;
      }
   }

   private void closeKawaseParamsBuffer() {
      if (this.gpuBuffer5 != null) {
         this.gpuBuffer5.close();
         this.gpuBuffer5 = null;
      }
   }

   public static void processData6(float alphaValue) {
      tintAlpha = Math.max(0.0F, Math.min(1.0F, alphaValue));
   }

   private void submitKawaseDraw(
      RenderPipeline renderPipeline,
      GpuTextureView gpuTextureView,
      int drawTargetWidth,
      int drawTargetHeight,
      GpuTextureView gpuTextureView2,
      float blurOffset,
      float drawUvLeft,
      float drawUvTop,
      float drawUvRight,
      float drawUvBottom,
      GpuSampler gpuSampler,
      boolean clearDepth,
      float gaussianStrength
   ) {
      if (gpuTextureView != null && gpuTextureView2 != null) {
         GpuBuffer gpuBuffer = this.getFullscreenQuadBuffer();
         if (gpuBuffer != null) {
            CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
            ShapeIndexBuffer sequentialBuffer = RenderSystem.getSequentialBuffer(DrawMode.QUADS);
            GpuBuffer indexBuffer = sequentialBuffer.getIndexBuffer(6);
            MemoryStack memoryStack = MemoryStack.stackPush();

            label91: {
               try {
                  ByteBuffer byteBuffer = memoryStack.calloc(64);
                  byteBuffer.putFloat(0, drawUvLeft);
                  byteBuffer.putFloat(4, drawUvTop);
                  byteBuffer.putFloat(8, drawUvRight);
                  byteBuffer.putFloat(12, drawUvBottom);
                  byteBuffer.putFloat(16, blurOffset / Math.max(drawTargetWidth, 1));
                  byteBuffer.putFloat(20, blurOffset / Math.max(drawTargetHeight, 1));
                  float tintAlphaScaled = tintAlpha * 0.97F;
                  byteBuffer.putFloat(32, this.useGlobalTint ? tintAlphaScaled : tintRed);
                  byteBuffer.putFloat(36, this.useGlobalTint ? tintAlphaScaled : tintGreen);
                  byteBuffer.putFloat(40, this.useGlobalTint ? tintAlphaScaled : tintBlue);
                  byteBuffer.putFloat(44, gaussianStrength);
                  byteBuffer.position(0);
                  GpuBuffer gpuBuffer2 = this.getKawaseParamsBuffer();
                  if (gpuBuffer2 == null) {
                     break label91;
                  }

                  commandEncoder.writeToBuffer(gpuBuffer2.slice(0L, 64L), byteBuffer);

                  try (RenderPass renderPass = commandEncoder.createRenderPass(
                        () -> "greed_kawase_blur", gpuTextureView2, clearDepth ? OptionalInt.of(0) : OptionalInt.empty(), null, OptionalDouble.empty()
                     )) {
                     renderPass.setPipeline(renderPipeline);
                     renderPass.bindTexture("Sampler0", gpuTextureView, gpuSampler);
                     renderPass.setUniform("KawaseParams", gpuBuffer2);
                     renderPass.setVertexBuffer(0, gpuBuffer);
                     renderPass.setIndexBuffer(indexBuffer, sequentialBuffer.getIndexType());
                     renderPass.drawIndexed(0, 0, 6, 1);
                  }
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

               return;
            }

            if (memoryStack != null) {
               memoryStack.close();
            }
         }
      }
   }

   public void processData8(int blurLayer, int drawCount) {
      boolean needsBlur = drawCount > blurLayer && (GuiEffectsState.checkState2() || GuiEffectsState.checkState());
      this.heavyPassPending = !needsBlur || this.blurAboveLayer;
      if (!this.hasBlurContexts) {
         this.blurContexts.clear();
      } else {
         MinecraftClient minecraftClient = MinecraftClient.getInstance();
         Framebuffer framebuffer = minecraftClient == null ? null : minecraftClient.getFramebuffer();
         if (framebuffer != null && framebuffer.getColorAttachmentView() != null) {
            if (this.globalBlurReady && this.framebuffer3 != null) {
               this.runBlurPipeline(
                  64,
                  framebuffer,
                  this.framebuffer3,
                  new BatchedBlurRenderer.DataRecord5(0, 0, framebuffer.textureWidth, framebuffer.textureHeight),
                  this.cachedGlobalRadius
               );
            }

            if (this.heavyBlurReady && (this.blurBelowLayer || !needsBlur) && this.framebuffer6 != null && this.framebuffer6.getColorAttachmentView() != null) {
               this.runBlurPipeline(
                  66, framebuffer, this.framebuffer6, new BatchedBlurRenderer.DataRecord5(0, 0, framebuffer.textureWidth, framebuffer.textureHeight), 72.0F
               );
            }

            for (int index = 0; index < this.blurContexts.size() && index < 64; index++) {
               BatchedBlurRenderer.SubContext3 layerContext = this.blurContexts.get(index);
               if (!layerContext.isGlobalBlur() && !layerContext.isSecondaryBlur()) {
                  BatchedBlurRenderer.DataRecord5 layerRegion = layerContext.getBlurRegion();
                  SimpleFramebuffer simpleFramebuffer = layerContext.getSimpleFramebuffer4();
                  if (layerRegion != null && simpleFramebuffer != null) {
                     this.runBlurPipeline(index, framebuffer, simpleFramebuffer, layerRegion, layerContext.getBlurDrawData().blurRadius());
                  }
               }
            }

            this.blurContexts.clear();
         } else {
            this.blurContexts.clear();
         }
      }
   }

   public static BatchedBlurRenderer getBatchedBlurRenderer() {
      BatchedBlurRenderer currentBatchedBlurRenderer = batchedBlurRenderer;
      if (currentBatchedBlurRenderer == null) {
         synchronized (BatchedBlurRenderer.class) {
            currentBatchedBlurRenderer = batchedBlurRenderer;
            if (currentBatchedBlurRenderer == null) {
               currentBatchedBlurRenderer = new BatchedBlurRenderer();
               batchedBlurRenderer = currentBatchedBlurRenderer;
            }
         }
      }

      return currentBatchedBlurRenderer;
   }

   public static void processData9(float tintR, float tintG, float tintB) {
      if (Float.isFinite(tintR) && Float.isFinite(tintG) && Float.isFinite(tintB)) {
         tintRed = clampFloat(tintR, 0.0F, 1.0F);
         tintGreen = clampFloat(tintG, 0.0F, 1.0F);
         tintBlue = clampFloat(tintB, 0.0F, 1.0F);
      }
   }

   private BatchedBlurRenderer() {
   }

   public void processData10(BlurDrawData newBlurDrawData) {
      DrawContext drawContext = this.drawContext2;
      if (newBlurDrawData != null && newBlurDrawData.visible() && drawContext != null) {
         try {
            BlurDrawData currentBlurDrawData = this.normalizeBlurDrawData(newBlurDrawData);
            Matrix3x2f matrix3x2f = GuiScale.renderElement(drawContext);
            ScreenRect screenRect = ScissorStack.getScreenRect();
            BlurDrawBatch currentBlurDrawBatch = new BlurDrawBatch();
            BatchedBlurRenderer.SubContext3 pendingDrawContext = new BatchedBlurRenderer.SubContext3(currentBlurDrawData, matrix3x2f, screenRect, currentBlurDrawBatch, true);
            pendingDrawContext.setLayerIndex(this.blurMode);
            this.blurContexts.add(pendingDrawContext);
            ((DrawContextMixin)drawContext).getGuiRenderState().addSimpleElement(new BlurRenderState(matrix3x2f, currentBlurDrawData, screenRect, currentBlurDrawBatch));
         } catch (RuntimeException exception) {
         }
      }
   }

   int computeInt3(BlurDrawData newBlurDrawData, BlurDrawBatch newBlurDrawBatch) {
      int recordIndex = this.drawRecords.size();
      if (recordIndex == 512) {
         return -1;
      }

      this.drawRecords.add(new BatchedBlurRenderer.DataRecord(newBlurDrawData, newBlurDrawBatch));
      this.paramsDirty = true;
      return recordIndex;
   }

   public static void processData11(int requestedBlurMode) {
      getBatchedBlurRenderer().blurMode = Math.max(0, Math.min(1, requestedBlurMode));
   }

   private float computeBlurScale(float scaleBlurRadius, int scaleWidth, int scaleHeight) {
      return scaleBlurRadius >= 40.0F ? 0.25F : 0.5F;
   }

   public boolean matchesCriteria(RenderPipeline renderPipeline) {
      return renderPipeline == pipeline2;
   }

   private static int floorToMultiple(int floorValue, int floorMultiple) {
      return Math.floorDiv(floorValue, floorMultiple) * floorMultiple;
   }

   private static Identifier createIdentifier(String identifierPath) {
      return Identifier.of("greed", identifierPath);
   }

   private BuiltBuffer buildFullscreenQuad(BufferAllocator bufferAllocator) {
      BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION);
      bufferBuilder.vertex(-1.0F, -1.0F, 0.0F);
      bufferBuilder.vertex(-1.0F, 1.0F, 0.0F);
      bufferBuilder.vertex(1.0F, 1.0F, 0.0F);
      bufferBuilder.vertex(1.0F, -1.0F, 0.0F);
      return bufferBuilder.end();
   }

   public void processData12(RenderPipeline renderPipeline, TextureSetup textureSetup, int processedCount, int layerBoundary) {
      if (this.heavyBlurReady && renderPipeline == pipeline2 && textureSetup != null && this.framebuffer6 != null) {
         GpuTextureView colorAttachmentView = this.framebuffer6.getColorAttachmentView();
         if (colorAttachmentView != null && textureSetup.texure0() == colorAttachmentView) {
            if (processedCount < layerBoundary) {
               this.blurBelowLayer = true;
            } else {
               this.blurAboveLayer = true;
            }
         }
      }
   }

   private SimpleFramebuffer resizeOrCreateBuffer(SimpleFramebuffer simpleFramebuffer, String targetBufferName, int targetBufferWidth, int targetBufferHeight) {
      if (simpleFramebuffer == null) {
         return new SimpleFramebuffer(targetBufferName, targetBufferWidth, targetBufferHeight, false);
      }

      if (simpleFramebuffer.textureWidth != targetBufferWidth || simpleFramebuffer.textureHeight != targetBufferHeight) {
         simpleFramebuffer.resize(targetBufferWidth, targetBufferHeight);
      }

      return simpleFramebuffer;
   }

   private int resolveLevelCount(float mipBlurRadius) {
      return 2;
   }

   private void releaseExpiredBlurBuffers(long currentNanos) {
      this.resultPool.releaseStaleFramebuffers(currentNanos, 5000000000L);
      this.scratchPool.releaseStaleFramebuffers(currentNanos, 5000000000L);
      if (this.framebuffer3 != null && currentNanos - this.globalBlurTimestamp > 5000000000L) {
         this.framebuffer3.delete();
         this.framebuffer3 = null;
      }

      if (this.framebuffer4 != null && currentNanos - this.secondaryBlurTimestamp > 5000000000L) {
         this.framebuffer4.delete();
         this.framebuffer4 = null;
      }

      if (this.framebuffer5 != null && currentNanos - this.stratumBlurTimestamp > 5000000000L) {
         this.framebuffer5.delete();
         this.framebuffer5 = null;
      }

      if (this.framebuffer6 != null && currentNanos - this.heavyBlurTimestamp > 5000000000L) {
         this.framebuffer6.delete();
         this.framebuffer6 = null;
      }
   }

   private GpuBuffer getBlurParamsBuffer() {
      if (this.paramsDirty) {
         this.updateState5();
      }

      if (!this.paramsDirty && this.gpuBuffer4 != null) {
         return this.gpuBuffer4;
      }

      if (this.paramsDirty) {
         this.closeBlurParamsBuffer();
         MemoryStack memoryStack = MemoryStack.stackPush();

         try {
            ByteBuffer byteBuffer = this.packDrawRecords(memoryStack, this.drawRecords);
            this.gpuBuffer4 = RenderSystem.getDevice().createBuffer(() -> "greed_blur_params", 136, byteBuffer);
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

         this.paramsDirty = false;
         return this.gpuBuffer4;
      } else {
         if (this.gpuBuffer4 == null || this.gpuBuffer4.isClosed() || this.gpuBuffer4.size() < 24576L) {
            this.closeBlurParamsBuffer();
            this.gpuBuffer4 = RenderSystem.getDevice().createBuffer(() -> "greed_blur_params", 136, 24576L);
         }

         return this.gpuBuffer4;
      }
   }

   public static void updateState8() {
      BatchedBlurRenderer currentBatchedBlurRenderer = batchedBlurRenderer;
      if (currentBatchedBlurRenderer != null) {
         currentBatchedBlurRenderer.close();
         batchedBlurRenderer = null;
      }
   }

   public void updateState9() {
      if (this.secondaryBlurReady && this.framebuffer4 != null && this.framebuffer4.getColorAttachmentView() != null) {
         MinecraftClient minecraftClient = MinecraftClient.getInstance();
         Framebuffer framebuffer = minecraftClient == null ? null : minecraftClient.getFramebuffer();
         if (framebuffer != null && framebuffer.getColorAttachmentView() != null) {
            this.runBlurPipeline(
               67, framebuffer, this.framebuffer4, new BatchedBlurRenderer.DataRecord5(0, 0, framebuffer.textureWidth, framebuffer.textureHeight), this.cachedSecondaryRadius
            );
            this.secondaryBlurReady = false;
         }
      }
   }

   public void renderElement4(DrawContext drawContext, BlurDrawData newBlurDrawData, BlurDrawBatch newBlurDrawBatch) {
      if (drawContext != null && newBlurDrawData != null && newBlurDrawBatch != null && newBlurDrawData.visible()) {
         try {
            BlurDrawData currentBlurDrawData = this.normalizeBlurDrawData(newBlurDrawData);
            Matrix3x2f matrix3x2f = GuiScale.renderElement(drawContext);
            this.blurContexts.add(new BatchedBlurRenderer.SubContext3(currentBlurDrawData, matrix3x2f, ScissorStack.getScreenRect(), newBlurDrawBatch, true));
         } catch (RuntimeException exception) {
         }
      }
   }

   public void processData14(BlurDrawData newBlurDrawData) {
      this.enqueueBlurContext(this.drawContext2, newBlurDrawData);
   }

   private ByteBuffer packDrawRecords(MemoryStack memoryStack, List<BatchedBlurRenderer.DataRecord> records) {
      int bufferSize = Math.max(1, records.size()) * 3 * 4 * 4;
      ByteBuffer byteBuffer = memoryStack.calloc(bufferSize);

      for (int index = 0; index < records.size(); index++) {
         BatchedBlurRenderer.DataRecord dataRecord = records.get(index);
         BlurDrawData currentBlurDrawData = dataRecord.blur();
         BlurDrawBatch currentBlurDrawBatch = dataRecord.capture();
         int recordOffset = index * 3 * 4 * 4;
         byteBuffer.putFloat(recordOffset, currentBlurDrawData.radiusTopLeft());
         byteBuffer.putFloat(recordOffset + 4, currentBlurDrawData.radiusTopRight());
         byteBuffer.putFloat(recordOffset + 8, currentBlurDrawData.radiusBottomRight());
         byteBuffer.putFloat(recordOffset + 12, currentBlurDrawData.radiusBottomLeft());
         byteBuffer.putFloat(recordOffset + 16, currentBlurDrawData.width());
         byteBuffer.putFloat(recordOffset + 20, currentBlurDrawData.height());
         byteBuffer.putFloat(recordOffset + 24, currentBlurDrawData.smoothness());
         byteBuffer.putFloat(recordOffset + 28, currentBlurDrawData.blurRadius());
         byteBuffer.putFloat(recordOffset + 32, currentBlurDrawBatch.floatVal);
         byteBuffer.putFloat(recordOffset + 36, currentBlurDrawBatch.floatVal2);
         byteBuffer.putFloat(recordOffset + 40, currentBlurDrawBatch.floatVal3);
         byteBuffer.putFloat(recordOffset + 44, currentBlurDrawBatch.floatVal4);
      }

      byteBuffer.position(0);
      return byteBuffer;
   }

   private void submitGaussianDraw(
      GpuTextureView gpuTextureView,
      GpuTextureView gpuTextureView2,
      float sampleStepX,
      float sampleStepY,
      float sampleRadius,
      int sampleCount,
      float sampleGainX,
      float sampleGainY,
      float sampleTint,
      GpuSampler gpuSampler
   ) {
      if (gpuTextureView != null && gpuTextureView2 != null) {
         GpuBuffer gpuBuffer = this.getFullscreenQuadBuffer();
         if (gpuBuffer != null) {
            CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
            ShapeIndexBuffer sequentialBuffer = RenderSystem.getSequentialBuffer(DrawMode.QUADS);
            GpuBuffer indexBuffer = sequentialBuffer.getIndexBuffer(6);
            MemoryStack memoryStack = MemoryStack.stackPush();

            label86: {
               try {
                  ByteBuffer byteBuffer = memoryStack.calloc(64);
                  byteBuffer.putFloat(0, 0.0F);
                  byteBuffer.putFloat(4, 0.0F);
                  byteBuffer.putFloat(8, 1.0F);
                  byteBuffer.putFloat(12, 1.0F);
                  byteBuffer.putFloat(16, sampleStepX);
                  byteBuffer.putFloat(20, sampleStepY);
                  byteBuffer.putFloat(24, sampleRadius);
                  byteBuffer.putFloat(28, sampleCount);
                  float gaussianTintAlpha = tintAlpha * 0.97F;
                  byteBuffer.putFloat(32, this.useGlobalTint ? gaussianTintAlpha : tintRed);
                  byteBuffer.putFloat(36, this.useGlobalTint ? gaussianTintAlpha : tintGreen);
                  byteBuffer.putFloat(40, this.useGlobalTint ? gaussianTintAlpha : tintBlue);
                  byteBuffer.putFloat(44, 1.0F);
                  byteBuffer.putFloat(48, sampleGainX);
                  byteBuffer.putFloat(52, sampleGainY);
                  byteBuffer.putFloat(56, sampleTint);
                  byteBuffer.putFloat(60, tintAlpha);
                  byteBuffer.position(0);
                  GpuBuffer gpuBuffer2 = this.getKawaseParamsBuffer();
                  if (gpuBuffer2 == null) {
                     break label86;
                  }

                  commandEncoder.writeToBuffer(gpuBuffer2.slice(0L, 64L), byteBuffer);

                  try (RenderPass renderPass = commandEncoder.createRenderPass(
                        () -> "greed_gaussian_blur", gpuTextureView2, OptionalInt.of(0), null, OptionalDouble.empty()
                     )) {
                     renderPass.setPipeline(pipeline5);
                     renderPass.bindTexture("Sampler0", gpuTextureView, gpuSampler);
                     renderPass.setUniform("KawaseParams", gpuBuffer2);
                     renderPass.setVertexBuffer(0, gpuBuffer);
                     renderPass.setIndexBuffer(indexBuffer, sequentialBuffer.getIndexType());
                     renderPass.drawIndexed(0, 0, 6, 1);
                  }
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

               return;
            }

            if (memoryStack != null) {
               memoryStack.close();
            }
         }
      }
   }

   public void updateState10() {
      this.drawContext2 = null;
   }

   public void processData16(float blurRadiusParam, float intensityParam) {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      Framebuffer framebuffer = minecraftClient == null ? null : minecraftClient.getFramebuffer();
      if (framebuffer != null && framebuffer.getColorAttachmentView() != null) {
         float intensity = clampFloat(intensityParam, 0.0F, 1.0F);
         if (intensity > 0.004F) {
            float blurScale = this.computeBlurScale(blurRadiusParam, framebuffer.textureWidth, framebuffer.textureHeight);
            int scaledBlurWidth = Math.max(Math.round(framebuffer.textureWidth * blurScale), 1);
            int scaledBlurHeight = Math.max(Math.round(framebuffer.textureHeight * blurScale), 1);
            this.framebuffer5 = this.resizeOrCreateBuffer(this.framebuffer5, "greed_blur_stratum", scaledBlurWidth, scaledBlurHeight);
            this.stratumBlurTimestamp = System.nanoTime();
            if (this.framebuffer5 != null && this.framebuffer5.getColorAttachmentView() != null) {
               this.runBlurPipeline(
                  65, framebuffer, this.framebuffer5, new BatchedBlurRenderer.DataRecord5(0, 0, framebuffer.textureWidth, framebuffer.textureHeight), blurRadiusParam
               );
               GpuSampler gpuSampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
               float blurPixels = 5.0F * intensity * intensity;
               float blurOffsetX = blurPixels / Math.max(framebuffer.textureWidth, 1);
               float blurOffsetY = blurPixels / Math.max(framebuffer.textureHeight, 1);
               this.submitKawaseDraw(
                  pipeline4,
                  this.framebuffer5.getColorAttachmentView(),
                  this.framebuffer5.textureWidth,
                  this.framebuffer5.textureHeight,
                  framebuffer.getColorAttachmentView(),
                  1.0F,
                  blurOffsetX,
                  blurOffsetY,
                  1.0F - 2.0F * blurOffsetX,
                  1.0F - 2.0F * blurOffsetY,
                  gpuSampler,
                  false,
                  intensity
               );
            }
         }
      }
   }

   private record DataRecord(BlurDrawData blur, BlurDrawBatch capture) {
   }

   private static final class SubContext2 {
      private final String namePrefix;
      private final List<BatchedBlurRenderer.SubContext4> framebufferPool = new ArrayList<>();
      private int nextFramebufferId;

      private void releaseStaleFramebuffers(long nowNanos, long maxAgeNanos) {
         for (int index = this.framebufferPool.size() - 1; index >= 0; index--) {
            BatchedBlurRenderer.SubContext4 pooledEntry = this.framebufferPool.get(index);
            if (nowNanos - pooledEntry.lastUsedNanos > maxAgeNanos) {
               pooledEntry.framebuffer9.delete();
               this.framebufferPool.remove(index);
            }
         }
      }

      private void releaseAllFramebuffers() {
         for (BatchedBlurRenderer.SubContext4 expiredEntry : this.framebufferPool) {
            expiredEntry.framebuffer9.delete();
         }

         this.framebufferPool.clear();
      }

      private SubContext2(String bufferNamePrefix) {
         this.namePrefix = bufferNamePrefix;
      }

      private SimpleFramebuffer acquirePooledBuffer(int requestedWidth, int requestedHeight, long frameIdParam, long usedNanos) {
         if (requestedWidth > 0 && requestedHeight > 0) {
            for (int index = 0; index < this.framebufferPool.size(); index++) {
               BatchedBlurRenderer.SubContext4 poolEntry = this.framebufferPool.get(index);
               if (poolEntry.lastFrameId != frameIdParam
                  && poolEntry.framebuffer9.textureWidth == requestedWidth
                  && poolEntry.framebuffer9.textureHeight == requestedHeight) {
                  poolEntry.lastFrameId = frameIdParam;
                  poolEntry.lastUsedNanos = usedNanos;
                  return poolEntry.framebuffer9;
               }
            }

            SimpleFramebuffer simpleFramebuffer = new SimpleFramebuffer(this.namePrefix + "_" + this.nextFramebufferId++, requestedWidth, requestedHeight, false);
            this.framebufferPool.add(new BatchedBlurRenderer.SubContext4(simpleFramebuffer, frameIdParam, usedNanos));
            return simpleFramebuffer;
         } else {
            return null;
         }
      }
   }

   private static final class SubContext3 {
      private final BlurDrawData blurDrawData;
      private final Matrix3x2f matrix;
      private final ScreenRect rect;
      private final BlurDrawBatch blurDrawBatch;
      private final boolean globalBlurEnabled;
      private final boolean secondaryBlurEnabled;
      private BatchedBlurRenderer.DataRecord5 blurRegion;
      private SimpleFramebuffer framebuffer8;
      private int layerIndex;

      SimpleFramebuffer getSimpleFramebuffer4() {
         return this.framebuffer8;
      }

      BlurDrawData getBlurDrawData() {
         return this.blurDrawData;
      }

      void setFramebuffer(SimpleFramebuffer simpleFramebuffer) {
         this.framebuffer8 = simpleFramebuffer;
      }

      BlurDrawBatch getBlurDrawBatch() {
         return this.blurDrawBatch;
      }

      ScreenRect getScreenRect() {
         return this.rect;
      }

      BatchedBlurRenderer.DataRecord5 getBlurRegion() {
         return this.blurRegion;
      }

      void setBlurRegion(BatchedBlurRenderer.DataRecord5 regionRecord) {
         this.blurRegion = regionRecord;
      }

      int getLayerIndex() {
         return this.layerIndex;
      }

      Matrix3x2f getMatrix3x2f() {
         return this.matrix;
      }

      void setLayerIndex(int layerIndexParam) {
         this.layerIndex = layerIndexParam;
      }

      private SubContext3(BlurDrawData initialBlurDrawData, Matrix3x2f matrix3x2f, ScreenRect screenRect, BlurDrawBatch initialBlurDrawBatch, boolean globalBlurFlag) {
         this(initialBlurDrawData, matrix3x2f, screenRect, initialBlurDrawBatch, globalBlurFlag, false);
      }

      boolean isGlobalBlur() {
         return this.globalBlurEnabled;
      }

      boolean isSecondaryBlur() {
         return this.secondaryBlurEnabled;
      }

      private SubContext3(
         BlurDrawData initialBlurDrawData,
         Matrix3x2f matrix3x2f,
         ScreenRect screenRect,
         BlurDrawBatch initialBlurDrawBatch,
         boolean globalBlurFlag2,
         boolean secondaryBlurFlag
      ) {
         this.blurDrawData = initialBlurDrawData;
         this.matrix = matrix3x2f;
         this.rect = screenRect;
         this.blurDrawBatch = initialBlurDrawBatch;
         this.globalBlurEnabled = globalBlurFlag2;
         this.secondaryBlurEnabled = secondaryBlurFlag;
      }
   }

   private static final class SubContext4 {
      private final SimpleFramebuffer framebuffer9;
      private long lastFrameId;
      private long lastUsedNanos;

      private SubContext4(SimpleFramebuffer simpleFramebuffer, long entryFrameId, long entryUsedNanos) {
         this.framebuffer9 = simpleFramebuffer;
         this.lastFrameId = entryFrameId;
         this.lastUsedNanos = entryUsedNanos;
      }
   }

   private record DataRecord5(int x, int y, int width, int height) {
   }
}
