package greed.client.util;

import greed.client.MinecraftAccess;
import greed.client.module.misc.UnHookModule;
import greed.client.ui.UiTheme;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

public final class ClientChat implements MinecraftAccess {
   private static final boolean flag = FabricLoader.getInstance()
      .getAllMods()
      .stream()
      .anyMatch(modContainer -> modContainer.getMetadata().getId().startsWith("baritone"));

   public static void send(Object local3531_0, Object... objectArray) {
      if (UnHookModule.isUnhooked()) {
         return;
      }

      if (!checkState()) {
         if (!mc.isOnThread()) {
            mc.execute(() -> send(local3531_0, objectArray));
         } else if (local3531_0 == null) {
            send("Object is null");
         } else {
            mc.inGameHud.getChatHud().addMessage(Text.literal(local3531_0.toString().formatted(objectArray).replace('&', '§')));
         }
      }
   }

   private static boolean checkState() {
      return mc.player == null || mc.world == null;
   }

   public static void sendBaritoneMessage(String local3533_0) {
      if (UnHookModule.isUnhooked()) {
         return;
      }

      if (flag && mc.player != null) {
         mc.player.networkHandler.sendChatMessage(local3533_0);
      }
   }

   public static void processData3(String local3534_0) {
      if (UnHookModule.isUnhooked()) {
         return;
      }

      if (mc.player != null) {
         if (!mc.isOnThread()) {
            mc.execute(() -> processData3(local3534_0));
         } else {
            mc.inGameHud
               .getChatHud()
               .addMessage(
                  getMutableText("Greed 1.21.11", UiTheme.intVal7, UiTheme.intVal9, true)
                     .append(Text.literal(Formatting.DARK_GRAY + " » " + Formatting.RESET))
                     .append(Text.literal(local3534_0))
               );
         }
      }
   }

   public static void processData4(Text text) {
      if (UnHookModule.isUnhooked()) {
         return;
      }

      if (mc.player != null) {
         if (!mc.isOnThread()) {
            mc.execute(() -> processData4(text));
         } else {
            mc.inGameHud.getChatHud().addMessage(text);
         }
      }
   }

   public static void info(Text text) {
      if (UnHookModule.isUnhooked()) {
         return;
      }

      if (!checkState()) {
         if (!mc.isOnThread()) {
            mc.execute(() -> info(text));
         } else {
            mc.inGameHud
               .getChatHud()
               .addMessage(
                  getMutableText("Greed 1.21.11", UiTheme.intVal7, UiTheme.intVal9, true)
                     .append(Text.literal(Formatting.DARK_GRAY + " » " + Formatting.RESET))
                     .append(text)
               );
         }
      }
   }

   public static MutableText getMutableText(String local3537_0, int local3537_1, int local3537_2, Boolean... booleanArray) {
      MutableText mutableText = Text.empty();
      boolean local3537_5 = booleanArray.length > 0 && booleanArray[0];

      for (int index = 0; index < local3537_0.length(); index++) {
         float local3537_7 = index * 1.1F / local3537_0.length();
         local3537_7 = Math.min(1.0F, local3537_7);
         int local3537_8 = Colors.computeInt2(local3537_1, local3537_2, local3537_7) & 16777215;
         Style style = Style.EMPTY.withColor(TextColor.fromRgb(local3537_8));
         if (local3537_5) {
            style = style.withBold(true);
         }

         mutableText.append(Text.literal(String.valueOf(local3537_0.charAt(index))).setStyle(style));
      }

      return mutableText;
   }

   private ClientChat() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   public static void styled(
      String local3541_0,
      boolean local3541_1,
      boolean local3541_2,
      Integer local3541_3,
      boolean local3541_4,
      boolean local3541_5,
      Integer local3541_6,
      String... stringArray
   ) {
      if (UnHookModule.isUnhooked()) {
         return;
      }

      if (mc.player != null) {
         if (!mc.isOnThread()) {
            mc.execute(() -> styled(local3541_0, local3541_1, local3541_2, local3541_3, local3541_4, local3541_5, local3541_6, stringArray));
         } else {
            MutableText mutableText = getMutableText("Greed 1.21.11", UiTheme.intVal7, UiTheme.intVal9, true)
               .append(Text.literal(Formatting.DARK_GRAY + " » " + Formatting.RESET));
            MutableText mutableText2 = Text.literal(local3541_1 ? local3541_0.replace('&', '§').replaceAll("§[0-9a-fr]", "$0§l") : local3541_0.replace('&', '§'));
            Style style = mutableText2.getStyle();
            if (local3541_3 != null) {
               style = style.withColor(TextColor.fromRgb(local3541_3 & 16777215));
            }

            if (local3541_1) {
               style = style.withBold(true);
            }

            if (local3541_2) {
               style = style.withItalic(true);
            }

            mutableText2.setStyle(style);
            mutableText.append(mutableText2);

            for (String local3541_14 : stringArray) {
               MutableText mutableText3 = Text.literal(
                  local3541_4 ? local3541_14.replace('&', '§').replaceAll("§[0-9a-fr]", "$0§l") : local3541_14.replace('&', '§')
               );
               Style style2 = mutableText3.getStyle();
               if (local3541_6 != null) {
                  style2 = style2.withColor(TextColor.fromRgb(local3541_6 & 16777215));
               }

               if (local3541_4) {
                  style2 = style2.withBold(true);
               }

               if (local3541_5) {
                  style2 = style2.withItalic(true);
               }

               mutableText3.setStyle(style2);
               mutableText.append(Text.literal("\n ")).append(mutableText3);
            }

            mc.inGameHud.getChatHud().addMessage(mutableText);
         }
      }
   }

   public static void processData7(Object local3542_0, Object... objectArray) {
      if (UnHookModule.isUnhooked()) {
         return;
      }

      if (!checkState()) {
         if (local3542_0 == null) {
            send("Object is null");
         } else {
            mc.player.networkHandler.sendChatMessage(String.format(local3542_0.toString(), objectArray).replace('§', '&'));
         }
      }
   }

   public static void processData8(String local3544_0) {
      if (UnHookModule.isUnhooked()) {
         return;
      }

      if (!checkState()) {
         if (!mc.isOnThread()) {
            mc.execute(() -> processData8(local3544_0));
         } else {
            mc.inGameHud
               .getChatHud()
               .addMessage(
                  getMutableText("Greed 1.21.11", UiTheme.intVal7, UiTheme.intVal9, true)
                     .append(Text.literal(Formatting.DARK_GRAY + " » " + Formatting.RESET))
                     .append(Text.literal(local3544_0.replace('&', '§')))
               );
         }
      }
   }

   public static void processData9(String local3546_0, String... stringArray) {
      styled(local3546_0, false, false, null, false, false, null, stringArray);
   }

   public static void processData10(Object local3548_0, Object... objectArray) {
      send("[" + Formatting.RED + "❌" + Formatting.RESET + "] " + local3548_0, objectArray);
   }
}
