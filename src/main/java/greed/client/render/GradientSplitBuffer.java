package greed.client.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.lwjgl.system.MemoryStack;

public final class GradientSplitBuffer {
   public static final int intVal = 16;
   private static final float[] splitData = new float[192];
   private static int splitCount;
   private static GpuBuffer gpuBuffer2;

   public static void updateState() {
      splitCount = 0;
      Arrays.fill(splitData, 0.0F);
   }

   public static GpuBuffer getGpuBuffer() {
      return getGpuBuffer2();
   }

   public static void updateState2() {
      GpuBuffer gpuBuffer = getGpuBuffer2();
      if (gpuBuffer != null) {
         try {
            MemoryStack memoryStack = MemoryStack.stackPush();

            try {
               ByteBuffer byteBuffer = memoryStack.calloc(768);

               for (int index = 0; index < 192; index++) {
                  byteBuffer.putFloat(index * 4, splitData[index]);
               }

               byteBuffer.position(0);
               RenderSystem.getDevice().createCommandEncoder().writeToBuffer(gpuBuffer.slice(0L, 768L), byteBuffer);
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

   private GradientSplitBuffer() {
   }

   public static void updateState3() {
      if (gpuBuffer2 != null) {
         gpuBuffer2.close();
         gpuBuffer2 = null;
      }
   }

   private static GpuBuffer getGpuBuffer2() {
      if (gpuBuffer2 != null && !gpuBuffer2.isClosed() && gpuBuffer2.size() >= 768L) {
         return gpuBuffer2;
      }

      updateState3();

      try {
         gpuBuffer2 = RenderSystem.getDevice().createBuffer(() -> "greed_client_splits", 136, 768L);
         return gpuBuffer2;
      } catch (RuntimeException exception) {
         return null;
      }
   }

   public static int computeInt(
      float parentCenterX,
      float parentCenterY,
      float parentHalfWidth,
      float parentHalfHeight,
      float childCenterX,
      float childCenterY,
      float childHalfWidth,
      float childHalfHeight,
      float cornerRadius,
      float smoothing
   ) {
      if (splitCount >= 16) {
         return 0;
      }

      int splitIndex = splitCount++;
      int bufferOffset = splitIndex * 3 * 4;
      splitData[bufferOffset] = parentCenterX;
      splitData[bufferOffset + 1] = parentCenterY;
      splitData[bufferOffset + 2] = parentHalfWidth;
      splitData[bufferOffset + 3] = parentHalfHeight;
      splitData[bufferOffset + 4] = childCenterX;
      splitData[bufferOffset + 5] = childCenterY;
      splitData[bufferOffset + 6] = childHalfWidth;
      splitData[bufferOffset + 7] = childHalfHeight;
      splitData[bufferOffset + 8] = cornerRadius;
      splitData[bufferOffset + 9] = smoothing;
      return splitIndex + 1;
   }
}
