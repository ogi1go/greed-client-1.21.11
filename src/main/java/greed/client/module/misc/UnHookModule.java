package greed.client.module.misc;

import greed.client.event.ClientTickEvent;
import greed.client.event.EventListener;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.module.ModuleManager;
import greed.client.util.IrcClient;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import net.fabricmc.loader.api.FabricLoader;

@ModuleInfo(
   name = "UnHook",
   description = "Полностью прячет клиент: чистит лог, убирает меню, выключает модули, команды и косметику. Вернуть — написать в чат 1337.",
   category = ModuleCategory.MISC
)
public final class UnHookModule extends Module {
   private static UnHookModule unHookModule;
   private static volatile boolean unhooked;
   private static final List<Module> reenableLater = new ArrayList<>();
   private static final String[] CLIENT_LOGGERS = new String[]{"greed", "greed-pipelines", "greed-glass-vapor"};
   private static final Pattern CLIENT_LOGGER_LINE = Pattern.compile("\\(greed[^)]*\\)");
   private static final Pattern EMBEDDED_CLIENT_TOKEN = Pattern.compile("(?i)\\s*,\\s*greed\\b|\\bgreed\\s*,\\s*");
   private static final Pattern MOD_LIST_ENTRY = Pattern.compile("^\\s*-\\s*greed\\b.*$");
   private static final Pattern MOD_COUNT = Pattern.compile("Loading\\s+(\\d+)\\s+mods:");
   private long lastSanitizeMillis;
   private long lastLogSize = -1L;
   public final EventListener<ClientTickEvent> unhookTickListener = clientTickEvent -> {
      if (!unhooked || System.currentTimeMillis() - this.lastSanitizeMillis < 5000L) {
         return;
      }

      this.lastSanitizeMillis = System.currentTimeMillis();

      try {
         Path latest = FabricLoader.getInstance().getGameDir().resolve("logs").resolve("latest.log");
         if (Files.isRegularFile(latest)) {
            long size = Files.size(latest);
            if (size != this.lastLogSize && size < 8000000L) {
               sanitizeLatestLog();
               this.lastLogSize = Files.size(latest);
            }
         }
      } catch (Throwable ignored) {
      }
   };

   public static boolean isUnhooked() {
      return unhooked;
   }

   private UnHookModule() {
   }

   public static UnHookModule getInstance() {
      if (unHookModule == null) {
         unHookModule = new UnHookModule();
      }

      return unHookModule;
   }

   @Override
   protected void onEnable() {
      unhooked = true;
      reenableLater.clear();

      List<Module> snapshot = new ArrayList<>(ModuleManager.getModules());
      for (Module module : snapshot) {
         if (module != this && module.isEnabled()) {
            reenableLater.add(module);
         }
      }

      for (Module module : snapshot) {
         if (module != this) {
            try {
               module.setEnabled(false);
            } catch (Throwable ignored) {
            }
         }
      }

      silenceClientLoggers(true);
      sanitizeLatestLog();
      IrcClient.getInstance().setSuspended(true);
      closeClientScreens();
      refreshWindowTitle();
   }

   @Override
   protected void onDisable() {
      unhooked = false;
      silenceClientLoggers(false);
      IrcClient.getInstance().setSuspended(false);

      for (Module module : new ArrayList<>(reenableLater)) {
         try {
            module.setEnabled(true);
         } catch (Throwable ignored) {
         }
      }

      reenableLater.clear();
      refreshWindowTitle();
   }

   private static void refreshWindowTitle() {
      net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
      if (client == null) {
         return;
      }

      client.execute(() -> {
         try {
            client.updateWindowTitle();
         } catch (Throwable ignored) {
         }
      });
   }

   private static void closeClientScreens() {
      net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
      if (client == null) {
         return;
      }

      client.execute(() -> {
         try {
            if (client.currentScreen != null && client.currentScreen.getClass().getName().equals("greed.client.ui.ClickGuiScreen")) {
               client.setScreen(client.world != null ? null : new net.minecraft.client.gui.screen.TitleScreen());
            }
         } catch (Throwable ignored) {
         }
      });
   }

   private static void silenceClientLoggers(boolean off) {
      try {
         Class<?> configurator = Class.forName("org.apache.logging.log4j.core.config.Configurator");
         Class<?> level = Class.forName("org.apache.logging.log4j.Level");
         Object target = level.getField(off ? "OFF" : "INFO").get(null);
         Method setLevel = configurator.getMethod("setLevel", String.class, level);

         for (String name : CLIENT_LOGGERS) {
            setLevel.invoke(null, name, target);
         }
      } catch (Throwable ignored) {
      }
   }

   private static void sanitizeLatestLog() {
      try {
         Path latest = FabricLoader.getInstance().getGameDir().resolve("logs").resolve("latest.log");
         if (!Files.isRegularFile(latest)) {
            return;
         }

         List<String> kept = new ArrayList<>();
         int removedModEntries = 0;

         for (String line : Files.readAllLines(latest, StandardCharsets.UTF_8)) {
            String cleaned = sanitizeLine(line);
            if (cleaned != null) {
               kept.add(cleaned);
            } else if (MOD_LIST_ENTRY.matcher(line).matches()) {
               removedModEntries++;
            }
         }

         if (removedModEntries > 0) {
            for (int index = 0; index < kept.size(); index++) {
               java.util.regex.Matcher matcher = MOD_COUNT.matcher(kept.get(index));
               if (matcher.find()) {
                  int remaining = Math.max(Integer.parseInt(matcher.group(1)) - removedModEntries, 0);
                  kept.set(index, matcher.replaceFirst("Loading " + remaining + " mods:"));
                  break;
               }
            }
         }

         Files.write(
            latest,
            kept,
            StandardCharsets.UTF_8,
            StandardOpenOption.WRITE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.CREATE
         );
      } catch (Throwable ignored) {
      }
   }

   private static String sanitizeLine(String line) {
      if (line == null) {
         return null;
      }

      if (CLIENT_LOGGER_LINE.matcher(line).find()) {
         return null;
      }

      String cleaned = EMBEDDED_CLIENT_TOKEN.matcher(line).replaceAll("");
      return cleaned.toLowerCase(Locale.ROOT).contains("greed") ? null : cleaned;
   }
}
