package greed.client.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import org.lwjgl.system.MemoryStack;

public final class ThemePaletteBuffer {
   public static final int intVal = 6;
   private static final int[] basePaletteColors = new int[]{8150271, 6069759, 16739229};
   private static GpuBuffer paletteGpuBuffer;
   private static int[] currentPaletteColors = new int[]{16777215};
   private static int colorCount = 1;
   private static float gradientScale;
   private static long lastUpdateMillis;
   private static float styleIndex;
   private static float previousStyleIndex;
   private static float lastStyleIndex = -1.0F;
   private static float closedAmount;

   private static int[] buildPaletteRamp(int[] intArray, int paletteSize) {
      int[] intArray2 = new int[paletteSize];
      if (intArray.length != 1) {
         for (int index = 0; index < paletteSize; index++) {
            float progress = paletteSize <= 1 ? 0.0F : (float)index / (paletteSize - 1);
            float scaledIndex = progress * (intArray.length - 1);
            int lowerIndex = (int)scaledIndex;
            if (lowerIndex > intArray.length - 2) {
               lowerIndex = intArray.length - 2;
            }

            intArray2[index] = lerpColor(intArray[lowerIndex], intArray[lowerIndex + 1], scaledIndex - lowerIndex);
         }

         return intArray2;
      } else {
         int singleColor = intArray[0] & 16777215;

         for (int index2 = 0; index2 < paletteSize; index2++) {
            intArray2[index2] = singleColor;
         }

         return intArray2;
      }
   }

   public static int[] computeInt2() {
      return currentPaletteColors;
   }

   private ThemePaletteBuffer() {
   }

   public static float computeFloat() {
      return styleIndex;
   }

   public static void updateState() {
      int[] intArray = buildPaletteRamp(basePaletteColors, 6);
      currentPaletteColors = intArray;
      colorCount = 6;
      closedAmount = 0.0F;
      lastUpdateMillis = System.currentTimeMillis();
      styleIndex = 1.0F;
      if (lastStyleIndex >= 0.0F && styleIndex != lastStyleIndex) {
         previousStyleIndex = lastStyleIndex;
         PaletteTransition.start();
      }

      lastStyleIndex = styleIndex;
      GpuBuffer gpuBuffer = getGpuBuffer();
      if (gpuBuffer != null) {
         try {
            MemoryStack memoryStack = MemoryStack.stackPush();

            try {
               ByteBuffer byteBuffer = memoryStack.calloc(128);
               byteBuffer.putFloat(0, colorCount);
               byteBuffer.putFloat(4, gradientScale);
               byteBuffer.putFloat(8, styleIndex);
               byteBuffer.putFloat(12, PaletteTransition.getProgress());
               byteBuffer.putFloat(112, previousStyleIndex);
               byteBuffer.putFloat(116, closedAmount);

               for (int index = 0; index < colorCount; index++) {
                  int color = intArray[index] & 16777215;
                  int byteOffset = (1 + index) * 4 * 4;
                  byteBuffer.putFloat(byteOffset, (color >>> 16 & 0xFF) / 255.0F);
                  byteBuffer.putFloat(byteOffset + 4, (color >>> 8 & 0xFF) / 255.0F);
                  byteBuffer.putFloat(byteOffset + 8, (color & 0xFF) / 255.0F);
                  byteBuffer.putFloat(byteOffset + 12, 1.0F);
               }

               byteBuffer.position(0);
               RenderSystem.getDevice().createCommandEncoder().writeToBuffer(gpuBuffer.slice(0L, 128L), byteBuffer);
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
         }
      }
   }

   public static boolean checkState() {
      return false;
   }

   private static float extractChannel(int[] intArray, int colorIndex, int channelIndex) {
      int colorValue = intArray[Math.max(0, Math.min(intArray.length - 1, colorIndex))];
      return (colorValue >> 16 - channelIndex * 8 & 0xFF) / 255.0F;
   }

   public static float computeFloat3() {
      return previousStyleIndex;
   }

   public static int computeInt3() {
      return colorCount;
   }

   public static float computeFloat4() {
      return gradientScale;
   }

   private static GpuBuffer getGpuBuffer() {
      if (paletteGpuBuffer != null && !paletteGpuBuffer.isClosed() && paletteGpuBuffer.size() >= 128L) {
         return paletteGpuBuffer;
      }

      updateState2();

      try {
         paletteGpuBuffer = RenderSystem.getDevice().createBuffer(() -> "greed_client_palette", 136, 128L);
         return paletteGpuBuffer;
      } catch (RuntimeException exception) {
         return null;
      }
   }

   private static int lerpColor(int startColor, int endColor, float amount) {
      float clampedAmount = amount < 0.0F ? 0.0F : Math.min(amount, 1.0F);
      int startRed = startColor >> 16 & 0xFF;
      int startGreen = startColor >> 8 & 0xFF;
      int startBlue = startColor & 0xFF;
      int endRed = endColor >> 16 & 0xFF;
      int endGreen = endColor >> 8 & 0xFF;
      int endBlue = endColor & 0xFF;
      int red = Math.round(startRed + (endRed - startRed) * clampedAmount);
      int green = Math.round(startGreen + (endGreen - startGreen) * clampedAmount);
      int blue = Math.round(startBlue + (endBlue - startBlue) * clampedAmount);
      return red << 16 | green << 8 | blue;
   }

   public static float computeFloat5() {
      return closedAmount;
   }

   public static GpuBuffer getGpuBuffer2() {
      return getGpuBuffer();
   }

   public static void updateState2() {
      if (paletteGpuBuffer != null) {
         paletteGpuBuffer.close();
         paletteGpuBuffer = null;
      }
   }

   private static float[] sampleSmoothColor(float position) {
      int[] intArray = currentPaletteColors;
      int currentCount = colorCount;
      if (currentCount <= 1) {
         return new float[]{extractChannel(intArray, 0, 0), extractChannel(intArray, 0, 1), extractChannel(intArray, 0, 2)};
      }

      float clampedPosition = Math.max(0.0F, Math.min(1.0F, position));
      float scaledPosition = clampedPosition * (currentCount - 1);
      int lowerIndex = Math.max(0, Math.min(currentCount - 1, (int)Math.floor(scaledPosition)));
      int upperIndex = Math.min(lowerIndex + 1, currentCount - 1);
      float fraction = Math.max(0.0F, Math.min(1.0F, scaledPosition - lowerIndex));
      fraction = fraction * fraction * (3.0F - 2.0F * fraction);
      float[] floatArray = new float[3];

      for (int index = 0; index < 3; index++) {
         float startChannel = extractChannel(intArray, lowerIndex, index);
         float endChannel = extractChannel(intArray, upperIndex, index);
         floatArray[index] = startChannel + (endChannel - startChannel) * fraction;
      }

      return floatArray;
   }

   public static int computeInt5(float position) {
      int[] intArray = currentPaletteColors;
      int currentCount = colorCount;
      if (intArray == null || intArray.length == 0) {
         return 16777215;
      }

      if (currentCount <= 1) {
         return intArray[0] & 16777215;
      }

      float cycleFraction = position - (float)Math.floor(position);
      float scaledPosition = cycleFraction * currentCount;
      int colorIndex = Math.max(0, Math.min(currentCount - 1, (int)Math.floor(scaledPosition)));
      float fraction = Math.max(0.0F, Math.min(1.0F, scaledPosition - colorIndex));
      int prevIndex = colorIndex - 1;
      if (prevIndex < 0) {
         prevIndex += currentCount;
      }

      int nextIndex = colorIndex + 1;
      if (nextIndex >= currentCount) {
         nextIndex -= currentCount;
      }

      int nextNextIndex = colorIndex + 2;
      if (nextNextIndex >= currentCount) {
         nextNextIndex -= currentCount;
      }

      float fractionSquared = fraction * fraction;
      float fractionCubed = fractionSquared * fraction;
      float smoothedFraction = 0.5F - 0.5F * (float)Math.cos((float) (Math.PI * 2) * cycleFraction);
      float[] floatArray = sampleSmoothColor(smoothedFraction);
      float blendAmount = Math.max(0.0F, Math.min(1.0F, closedAmount));
      int resultColor = 0;

      for (int index = 0; index < 3; index++) {
         float prevChannel = extractChannel(intArray, prevIndex, index);
         float currentChannel = extractChannel(intArray, colorIndex, index);
         float nextChannel = extractChannel(intArray, nextIndex, index);
         float nextNextChannel = extractChannel(intArray, nextNextIndex, index);
         float interpolatedValue = 0.5F
            * (
               2.0F * currentChannel
                  + (-prevChannel + nextChannel) * fraction
                  + (2.0F * prevChannel - 5.0F * currentChannel + 4.0F * nextChannel - nextNextChannel) * fractionSquared
                  + (-prevChannel + 3.0F * currentChannel - 3.0F * nextChannel + nextNextChannel) * fractionCubed
            );
         float blendedValue = floatArray[index] + (interpolatedValue - floatArray[index]) * blendAmount;
         int channelByte = Math.round(Math.max(0.0F, Math.min(1.0F, blendedValue)) * 255.0F);
         resultColor |= channelByte << 16 - index * 8;
      }

      return resultColor;
   }
}
