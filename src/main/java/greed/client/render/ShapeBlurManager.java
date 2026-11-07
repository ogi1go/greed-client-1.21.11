package greed.client.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.client.util.Window;
import org.lwjgl.system.MemoryStack;

public enum ShapeBlurManager {
   INSTANCE;

   private static final float[] blurRadii = new float[]{1.1F, 1.0F, 1.4F};
   private static final int[] mipCounts = new int[]{2, 3, 4};
   private final SimpleFramebuffer[] framebufferArray = new SimpleFramebuffer[3];
   private final boolean[] pendingBlurFlags = new boolean[3];
   private final boolean[] readyBlurFlags = new boolean[3];
   private SimpleFramebuffer[] framebufferArray2 = new SimpleFramebuffer[0];
   private final List<SimpleFramebuffer> slotFramebuffers = new ArrayList<>();
   private final List<GpuTextureView> slotTextureViews = new ArrayList<>();
   private final List<Integer> slotLevels = new ArrayList<>();
   private final List<float[]> slotRects = new ArrayList<>();
   private final List<ShapeBlurManager.SubContext> slotStates = new ArrayList<>();
   private final int[] scratchBounds = new int[4];
   private final float[] scratchRect = new float[4];
   private int requestCount;
   private int retainedCount;
   private long lastTrimNanos;
   private SimpleFramebuffer framebuffer2;
   private GpuBuffer gpuBuffer;
   private long lastUpdateNanos;

   public void updateState() {
      long currentTimeNanos = System.nanoTime();
      if (this.requestCount >= this.retainedCount || currentTimeNanos - this.lastTrimNanos > 3000000000L) {
         this.retainedCount = this.requestCount;
         this.lastTrimNanos = currentTimeNanos;
      }

      while (this.slotFramebuffers.size() > this.retainedCount) {
         int lastIndex = this.slotFramebuffers.size() - 1;
         this.slotFramebuffers.remove(lastIndex).delete();
         this.slotTextureViews.remove(lastIndex);
         this.slotLevels.remove(lastIndex);
         this.slotRects.remove(lastIndex);
         this.slotStates.remove(lastIndex);
      }

      this.requestCount = 0;
   }

   public void updateState2() {
      long currentTimeNanos = System.nanoTime();
      if (currentTimeNanos - this.lastUpdateNanos >= 16666666L) {
         this.lastUpdateNanos = currentTimeNanos;
         this.updateBlurPasses();
      }
   }

   private void updateBlurPasses() {
      Framebuffer framebuffer = getFramebuffer2();
      if (framebuffer != null) {
         boolean hasActiveBlur = false;

         for (boolean activeFlag : this.pendingBlurFlags) {
            hasActiveBlur |= activeFlag;
         }

         if (!hasActiveBlur) {
            Arrays.fill(this.readyBlurFlags, false);
         } else {
            Window window = MinecraftClient.getInstance().getWindow();
            int blurWidth = Math.max(1, window.getFramebufferWidth() / 2);
            int blurHeight = Math.max(1, window.getFramebufferHeight() / 2);
            GpuTextureView colorAttachmentView = framebuffer.getColorAttachmentView();

            try (ShapeBlurPass.SubContext passContext = ShapeBlurPass.getNested0426()) {
               for (int index = 0; index < 3; index++) {
                  this.readyBlurFlags[index] = this.pendingBlurFlags[index];
                  this.pendingBlurFlags[index] = false;
                  if (this.readyBlurFlags[index]) {
                     this.framebufferArray[index] = getSimpleFramebuffer(this.framebufferArray[index], "greed_shape_blur_l" + index, blurWidth, blurHeight);
                     this.applyBlurChain(
                        colorAttachmentView, computeMipCount(index), blurRadii[index], blurRadii[index] * 2.0F, this.framebufferArray[index], null, 0.0F
                     );
                  }
               }
            }
         }
      }
   }

   private static int computeMipCount(int blurIndex) {
      int mipOffset = 31 - Integer.numberOfLeadingZeros(Math.max(1, 2));
      return Math.max(1, mipCounts[blurIndex] + 1 - mipOffset);
   }

   private void applyBlurChain(
      GpuTextureView gpuTextureView, int mipLevels, float blurRadius, float initialRadius, SimpleFramebuffer simpleFramebuffer, float[] floatArray, float clampOffset
   ) {
      this.ensureMipFramebuffers(mipLevels, simpleFramebuffer.textureWidth, simpleFramebuffer.textureHeight);
      GpuSampler gpuSampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
      GpuTextureView gpuTextureView2 = gpuTextureView;
      float expandedRadius = blurRadius + 1.0F;
      float halfRadiusStep = blurRadius * 0.5F + 1.0F;

      for (int index = 0; index < mipLevels; index++) {
         int[] intArray = null;
         if (floatArray != null) {
            float passOffset = computeMipOffset(clampOffset, expandedRadius, mipLevels - 1);

            for (int index2 = mipLevels - 1; index2 > index; index2--) {
               passOffset += halfRadiusStep * (1 << index2);
            }

            intArray = this.computeScissorBounds(this.framebufferArray2[index], simpleFramebuffer, floatArray, passOffset);
         }

         ShapeBlurPass.processData5(
            ShapePipelines.BLUR_DOWN, this.framebufferArray2[index], gpuTextureView2, gpuSampler, index == 0 ? initialRadius : blurRadius, intArray
         );
         gpuTextureView2 = this.framebufferArray2[index].getColorAttachmentView();
      }

      for (int index3 = mipLevels - 2; index3 >= 0; index3--) {
         int[] intArray2 = floatArray == null
            ? null
            : this.computeScissorBounds(this.framebufferArray2[index3], simpleFramebuffer, floatArray, computeMipOffset(clampOffset, expandedRadius, index3));
         ShapeBlurPass.processData5(
            ShapePipelines.BLUR_UP,
            this.framebufferArray2[index3],
            this.framebufferArray2[index3 + 1].getColorAttachmentView(),
            gpuSampler,
            blurRadius,
            intArray2
         );
      }

      int[] intArray3 = floatArray == null ? null : this.computeScissorBounds(simpleFramebuffer, simpleFramebuffer, floatArray, clampOffset);
      ShapeBlurPass.processData5(
         ShapePipelines.BLUR_UP, simpleFramebuffer, this.framebufferArray2[0].getColorAttachmentView(), gpuSampler, blurRadius, intArray3
      );
   }

   private static float computeMipOffset(float baseOffset, float stepScale, int level) {
      return baseOffset + stepScale * ((1 << level + 2) - 2);
   }

   private static float computeClampOffset(float baseOffset, int levelCount, float radiusScale, float extraScale) {
      float accumulated = computeMipOffset(baseOffset, radiusScale + 1.0F, levelCount - 1);

      for (int index = levelCount - 1; index > 0; index--) {
         accumulated += (radiusScale * 0.5F + 1.0F) * (1 << index);
      }

      return accumulated + extraScale * 0.5F + 1.0F;
   }

   private int[] computeScissorBounds(Framebuffer framebuffer, SimpleFramebuffer simpleFramebuffer, float[] floatArray, float clampOffset) {
      int sourceWidth = Math.max(1, framebuffer.textureWidth);
      int sourceHeight = Math.max(1, framebuffer.textureHeight);
      float offsetScaleX = clampOffset / Math.max(1, simpleFramebuffer.textureWidth);
      float offsetScaleY = clampOffset / Math.max(1, simpleFramebuffer.textureHeight);
      int minX = Math.max(0, (int)Math.floor((floatArray[0] - offsetScaleX) * sourceWidth) - 1);
      int minY = Math.max(0, (int)Math.floor((floatArray[1] - offsetScaleY) * sourceHeight) - 1);
      int maxX = Math.min(sourceWidth, (int)Math.ceil((floatArray[2] + offsetScaleX) * sourceWidth) + 1);
      int maxY = Math.min(sourceHeight, (int)Math.ceil((floatArray[3] + offsetScaleY) * sourceHeight) + 1);
      int[] intArray = this.scratchBounds;
      intArray[0] = Math.min(minX, sourceWidth - 1);
      intArray[1] = Math.min(minY, sourceHeight - 1);
      intArray[2] = Math.max(1, maxX - intArray[0]);
      intArray[3] = Math.max(1, maxY - intArray[1]);
      return intArray;
   }

   private void ensureMipFramebuffers(int mipLevels, int baseWidth, int baseHeight) {
      if (this.framebufferArray2.length < mipLevels) {
         this.framebufferArray2 = Arrays.copyOf(this.framebufferArray2, mipLevels);
      }

      for (int index = 0; index < mipLevels; index++) {
         this.framebufferArray2[index] = getSimpleFramebuffer(
            this.framebufferArray2[index],
            "greed_shape_blur_mip" + index,
            Math.max(1, baseWidth >> index + 1),
            Math.max(1, baseHeight >> index + 1)
         );
      }
   }

   public int computeInt3(int blurIndex, float rectMinX, float rectMinY, float rectMaxX, float rectMaxY, float rectExtra) {
      Window window = MinecraftClient.getInstance().getWindow();
      int blurWidth = Math.max(1, window.getFramebufferWidth() / 2);
      int blurHeight = Math.max(1, window.getFramebufferHeight() / 2);
      int slotIndex = this.requestCount++;

      while (this.slotFramebuffers.size() <= slotIndex) {
         this.slotFramebuffers.add(new SimpleFramebuffer("greed_shape_blur_2d_res_" + this.slotFramebuffers.size(), blurWidth, blurHeight, false));
         this.slotTextureViews.add(null);
         this.slotLevels.add(0);
         this.slotRects.add(new float[5]);
         this.slotStates.add(new ShapeBlurManager.SubContext());
      }

      SimpleFramebuffer simpleFramebuffer = this.slotFramebuffers.get(slotIndex);
      if (simpleFramebuffer.textureWidth != blurWidth || simpleFramebuffer.textureHeight != blurHeight) {
         simpleFramebuffer.resize(blurWidth, blurHeight);
         this.slotStates.get(slotIndex).activeBlur = false;
      }

      this.slotTextureViews.set(slotIndex, simpleFramebuffer.getColorAttachmentView());
      this.slotLevels.set(slotIndex, clampBlurLevel(blurIndex));
      float[] floatArray = this.slotRects.get(slotIndex);
      floatArray[0] = rectMinX;
      floatArray[1] = rectMinY;
      floatArray[2] = rectMaxX;
      floatArray[3] = rectMaxY;
      floatArray[4] = rectExtra;
      return slotIndex;
   }

   private TextureSetup getTextureSetup(int slotIndex) {
      if (slotIndex >= 0 && slotIndex < this.slotTextureViews.size()) {
         GpuTextureView gpuTextureView = this.slotTextureViews.get(slotIndex);
         return gpuTextureView != null && !gpuTextureView.isClosed() ? TextureSetup.of(gpuTextureView, RenderSystem.getSamplerCache().get(FilterMode.LINEAR)) : null;
      } else {
         return null;
      }
   }

   public int computeInt4(GpuTextureView gpuTextureView) {
      if (gpuTextureView == null) {
         return -1;
      }

      for (int index = 0; index < this.slotTextureViews.size(); index++) {
         if (this.slotTextureViews.get(index) == gpuTextureView) {
            return index;
         }
      }

      return -1;
   }

   public boolean matchesCriteria(int slotIndex) {
      return slotIndex >= 0 && slotIndex < this.slotFramebuffers.size() && this.shouldRefreshSlot(slotIndex, System.nanoTime());
   }

   private boolean shouldRefreshSlot(int slotIndex, long currentTimeNanos) {
      ShapeBlurManager.SubContext state = this.slotStates.get(slotIndex);
      if (!state.activeBlur) {
         return true;
      } else {
         float[] floatArray = this.slotRects.get(slotIndex);
         if (state.blurLevel == this.slotLevels.get(slotIndex) && !rectsOverlap(state.cachedRect, floatArray)) {
            boolean rectChanged = !Arrays.equals(state.cachedRect, floatArray);
            return rectChanged && !state.isWithinCachedRect(floatArray, this.slotFramebuffers.get(slotIndex))
               ? true
               : currentTimeNanos - state.lastCaptureNanos >= (rectChanged ? 6944444L : 16666666L);
         } else {
            return true;
         }
      }
   }

   private static boolean rectsOverlap(float[] floatArray, float[] floatArray2) {
      if (floatArray != null && floatArray2 != null) {
         float overlapWidth = Math.max(0.0F, Math.min(floatArray[2], floatArray2[2]) - Math.max(floatArray[0], floatArray2[0]));
         float overlapHeight = Math.max(0.0F, Math.min(floatArray[3], floatArray2[3]) - Math.max(floatArray[1], floatArray2[1]));
         float overlapArea = overlapWidth * overlapHeight;
         float rectArea = Math.max(0.0F, floatArray[2] - floatArray[0]) * Math.max(0.0F, floatArray[3] - floatArray[1]);
         float otherRectArea = Math.max(0.0F, floatArray2[2] - floatArray2[0]) * Math.max(0.0F, floatArray2[3] - floatArray2[1]);
         float unionArea = rectArea + otherRectArea - overlapArea;
         return unionArea > 1.0E-9F && overlapArea / unionArea < 0.75F;
      } else {
         return floatArray != floatArray2;
      }
   }

   public void processData3(int slotIndex, Framebuffer framebuffer) {
      if (slotIndex >= 0
         && slotIndex < this.slotFramebuffers.size()
         && framebuffer != null
         && framebuffer.getColorAttachmentView() != null
         && !framebuffer.getColorAttachmentView().isClosed()) {
         SimpleFramebuffer simpleFramebuffer = this.slotFramebuffers.get(slotIndex);
         long currentTimeNanos = System.nanoTime();
         if (this.shouldRefreshSlot(slotIndex, currentTimeNanos)) {
            float[] floatArray = this.slotRects.get(slotIndex);
            int blurLevel = this.slotLevels.get(slotIndex);
            boolean didClamp = this.applyClampedBlur(framebuffer, simpleFramebuffer, floatArray, blurLevel);
            ShapeBlurManager.SubContext state = this.slotStates.get(slotIndex);
            state.activeBlur = true;
            state.lastCaptureNanos = currentTimeNanos;
            state.blurLevel = blurLevel;
            System.arraycopy(floatArray, 0, state.cachedRect, 0, 5);
            state.clamped = didClamp;
         }
      }
   }

   private boolean applyClampedBlur(Framebuffer framebuffer, SimpleFramebuffer simpleFramebuffer, float[] floatArray, int blurIndex) {
      GpuTextureView gpuTextureView = framebuffer.getColorAttachmentView();
      float radius = blurRadii[blurIndex] * 2.0F;
      boolean didClamp = false;

      try (ShapeBlurPass.SubContext passContext = ShapeBlurPass.getNested0426()) {
         float[] floatArray2 = null;
         float clampBase = 9.0F;
         if (floatArray != null && floatArray[2] > floatArray[0] && floatArray[3] > floatArray[1]) {
            radius = blurRadii[blurIndex];
            floatArray2 = this.scratchRect;
            System.arraycopy(floatArray, 0, floatArray2, 0, 4);
            didClamp = true;
            this.framebuffer2 = getSimpleFramebuffer(this.framebuffer2, "greed_shape_blur_clamp", simpleFramebuffer.textureWidth, simpleFramebuffer.textureHeight);
            int[] intArray = this.computeScissorBounds(
               this.framebuffer2, simpleFramebuffer, floatArray2, computeClampOffset(clampBase, computeMipCount(blurIndex), blurRadii[blurIndex], radius)
            );
            ShapeBlurPass.processData3(
               ShapePipelines.CLAMP_COPY,
               this.framebuffer2,
               gpuTextureView,
               RenderSystem.getSamplerCache().get(FilterMode.LINEAR),
               this.getGpuBuffer(floatArray),
               intArray
            );
            gpuTextureView = this.framebuffer2.getColorAttachmentView();
         }

         this.applyBlurChain(gpuTextureView, computeMipCount(blurIndex), blurRadii[blurIndex], radius, simpleFramebuffer, floatArray2, clampBase);
      }

      return didClamp;
   }

   private GpuBuffer getGpuBuffer(float[] floatArray) {
      if (this.gpuBuffer == null || this.gpuBuffer.isClosed()) {
         this.gpuBuffer = RenderSystem.getDevice().createBuffer(() -> "greed_shape_blur_rect", 136, 32L);
      }

      MemoryStack memoryStack = MemoryStack.stackPush();

      try {
         ByteBuffer byteBuffer = memoryStack.calloc(32);
         byteBuffer.putFloat(0, floatArray[0]);
         byteBuffer.putFloat(4, floatArray[1]);
         byteBuffer.putFloat(8, floatArray[2]);
         byteBuffer.putFloat(12, floatArray[3]);
         byteBuffer.putFloat(16, floatArray[4]);
         byteBuffer.position(0);
         RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.gpuBuffer.slice(0L, 32L), byteBuffer);
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

      return this.gpuBuffer;
   }

   public TextureSetup getTextureSetup2(
      int blurIndex, boolean useCache, float rectMinX, float rectMinY, float rectMaxX, float rectMaxY, float rectExtra
   ) {
      int blurLevel = clampBlurLevel(blurIndex);
      if (useCache) {
         int slotIndex = this.computeInt3(blurLevel, rectMinX, rectMinY, rectMaxX, rectMaxY, rectExtra);
         TextureSetup textureSetup = this.getTextureSetup(slotIndex);
         if (textureSetup != null) {
            return textureSetup;
         }
      }

      this.pendingBlurFlags[blurLevel] = true;
      if (!this.readyBlurFlags[blurLevel]) {
         return null;
      }

      SimpleFramebuffer simpleFramebuffer = this.framebufferArray[blurLevel];
      return simpleFramebuffer != null && simpleFramebuffer.getColorAttachmentView() != null && !simpleFramebuffer.getColorAttachmentView().isClosed()
         ? TextureSetup.of(simpleFramebuffer.getColorAttachmentView(), RenderSystem.getSamplerCache().get(FilterMode.LINEAR))
         : null;
   }

   private static int clampBlurLevel(int blurIndex) {
      return blurIndex < 0 ? 0 : Math.min(blurIndex, 2);
   }

   private static Framebuffer getFramebuffer2() {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      if (minecraftClient == null) {
         return null;
      } else {
         Framebuffer framebuffer = minecraftClient.getFramebuffer();
         if (framebuffer != null && framebuffer.getColorAttachment() != null && framebuffer.getColorAttachment().getFormat() == TextureFormat.RGBA8) {
            return framebuffer.getColorAttachmentView() != null && !framebuffer.getColorAttachmentView().isClosed() ? framebuffer : null;
         } else {
            return null;
         }
      }
   }

   private static SimpleFramebuffer getSimpleFramebuffer(SimpleFramebuffer simpleFramebuffer, String label, int width, int height) {
      if (simpleFramebuffer == null) {
         return new SimpleFramebuffer(label, width, height, false);
      }

      if (simpleFramebuffer.textureWidth != width || simpleFramebuffer.textureHeight != height) {
         simpleFramebuffer.resize(width, height);
      }

      return simpleFramebuffer;
   }

   private static final class SubContext {
      private boolean activeBlur;
      private boolean clamped;
      private long lastCaptureNanos;
      private int blurLevel;
      private final float[] cachedRect = new float[5];

      private boolean isWithinCachedRect(float[] floatArray, SimpleFramebuffer simpleFramebuffer) {
         if (!this.clamped) {
            return true;
         }

         float marginX = 6.0F / Math.max(1, simpleFramebuffer.textureWidth);
         float marginY = 6.0F / Math.max(1, simpleFramebuffer.textureHeight);
         return floatArray[0] >= this.cachedRect[0] - marginX
            && floatArray[1] >= this.cachedRect[1] - marginY
            && floatArray[2] <= this.cachedRect[2] + marginX
            && floatArray[3] <= this.cachedRect[3] + marginY;
      }
   }
}
