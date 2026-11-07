package greed.client;

import java.nio.IntBuffer;
import java.util.Locale;
import net.minecraft.SharedConstants;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.system.JNI;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.windows.WindowsLibrary;

public final class WindowAppearance {
   public static final String strVal = formatText();
   private static WindowsLibrary windowsLibrary;
   private static long timeMillis;

   private static boolean checkState() {
      return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
   }

   public static String formatText() {
      return "Greed 1.0 - " + formatText2();
   }

   public static String formatText2() {
      return SharedConstants.getGameVersion().name();
   }

   public static String formatText3() {
      return "Greed LoL  ·  " + formatText2();
   }

   private static long computeLong() {
      if (timeMillis != 0L) {
         return timeMillis;
      }

      if (windowsLibrary == null) {
         windowsLibrary = new WindowsLibrary("dwmapi.dll");
      }

      timeMillis = windowsLibrary.getFunctionAddress("DwmSetWindowAttribute");
      return timeMillis;
   }

   public static void processData(long local7413_0) {
      if (checkState() && local7413_0 != 0L) {
         try {
            long local7413_2 = GLFWNativeWin32.glfwGetWin32Window(local7413_0);
            long local7413_4 = computeLong();
            if (local7413_2 == 0L || local7413_4 == 0L) {
               return;
            }

            MemoryStack memoryStack = MemoryStack.stackPush();

            try {
               IntBuffer intBuffer = memoryStack.ints(1);
               long local7413_8 = MemoryUtil.memAddress(intBuffer);
               int local7413_10 = JNI.callPPI(local7413_2, 20, local7413_8, 4, local7413_4);
               if (local7413_10 != 0) {
                  JNI.callPPI(local7413_2, 19, local7413_8, 4, local7413_4);
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
         } catch (Throwable throwable3) {
         }
      }
   }

   private WindowAppearance() {
   }
}
