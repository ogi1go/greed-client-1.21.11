package greed.client.util;

import java.util.HashSet;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;

public final class InputBinding {
   public static final InputBinding UNBOUND = new InputBinding(InputBinding.DeviceType.NONE, -1);
   private final InputBinding.DeviceType device;
   private final int code;
   private static final HashSet<Integer> pressedKeys = new HashSet<>();

   public boolean isBound() {
      return this.device != InputBinding.DeviceType.NONE && this.code != -1;
   }

   public static InputBinding mouseButton(int newCode) {
      return new InputBinding(InputBinding.DeviceType.MOUSE, newCode);
   }

   private InputBinding(InputBinding.DeviceType initialDevice, int initialCode) {
      this.device = initialDevice;
      this.code = initialCode;
   }

   public InputBinding.DeviceType getDevice() {
      return this.device;
   }

   public static void updatePressedKeys(KeyInput keyInput, boolean local4296_1) {
      if (!isMovementKey(keyInput.key())) {
         if (!local4296_1) {
            pressedKeys.remove(keyInput.key());
         } else {
            for (KeyBinding keyBinding : MinecraftClient.getInstance().options.allKeys) {
               if (!keyBinding.isUnbound() && keyBinding.matchesKey(keyInput)) {
                  return;
               }
            }

            pressedKeys.add(keyInput.key());
         }
      }
   }

   public boolean isPressed(long local4297_1, boolean local4297_3) {
      if (!this.isBound()) {
         return false;
      } else if (!local4297_3 && MinecraftClient.getInstance().currentScreen != null) {
         return false;
      } else if (GLFW.glfwGetWindowAttrib(local4297_1, 131073) == 0) {
         pressedKeys.clear();
         return false;
      } else if (this.device == InputBinding.DeviceType.MOUSE) {
         return GLFW.glfwGetMouseButton(local4297_1, this.code) == 1;
      } else {
         return isMovementKey(this.code) && !pressedKeys.isEmpty() ? false : GLFW.glfwGetKey(local4297_1, this.code) == 1;
      }
   }

   public String getDisplayName() {
      if (!this.isBound()) {
         return "NONE";
      }

      if (this.device == InputBinding.DeviceType.MOUSE) {
         return "M" + (this.code + 1);
      }

      if (this.code > 32 && this.code <= 96) {
         return String.valueOf((char)this.code);
      }

      if (this.code >= 290 && this.code <= 314) {
         return "F" + (this.code - 290 + 1);
      }

      if (this.code >= 320 && this.code <= 329) {
         return "NUM" + (this.code - 320);
      }

      return switch (this.code) {
         case 32 -> "SPACE";
         case 256 -> "ESC";
         case 257 -> "ENTER";
         case 258 -> "TAB";
         case 259 -> "BACKSPACE";
         case 260 -> "INSERT";
         case 261 -> "DELETE";
         case 262 -> "RIGHT";
         case 263 -> "LEFT";
         case 264 -> "DOWN";
         case 265 -> "UP";
         case 266 -> "PGUP";
         case 267 -> "PGDN";
         case 268 -> "HOME";
         case 269 -> "END";
         case 280 -> "CAPSLOCK";
         case 281 -> "SCROLLLOCK";
         case 282 -> "NUMLOCK";
         case 283 -> "PRTSC";
         case 284 -> "PAUSE";
         case 330 -> "NUM.";
         case 331 -> "NUM/";
         case 332 -> "NUM*";
         case 333 -> "NUM-";
         case 334 -> "NUM+";
         case 335 -> "NUMENTER";
         case 336 -> "NUM=";
         case 340, 344 -> "SHIFT";
         case 341, 345 -> "CTRL";
         case 342, 346 -> "ALT";
         case 343, 347 -> "WIN";
         case 348 -> "MENU";
         default -> "KEY" + this.code;
      };
   }

   public int getCode() {
      return this.code;
   }

   private static boolean isMovementKey(int local4300_0) {
      return local4300_0 == 342
         || local4300_0 == 346
         || local4300_0 == 341
         || local4300_0 == 345
         || local4300_0 == 340
         || local4300_0 == 344
         || local4300_0 == 343
         || local4300_0 == 347;
   }

   public static InputBinding keyboardKey(int newCode) {
      return newCode == -1 ? UNBOUND : new InputBinding(InputBinding.DeviceType.KEYBOARD, newCode);
   }

   public boolean isPressed(long local4302_1) {
      return this.isPressed(local4302_1, false);
   }

   public enum DeviceType {
      NONE,
      KEYBOARD,
      MOUSE;
   }
}
