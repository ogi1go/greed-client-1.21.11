package greed.client.render;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class FontRegistry {
   public static final String TEXT_MONTSERRAT = "montserrat";
   private static final Map<String, String> resourcePaths = new HashMap<>();
   private static final Map<String, FontAtlas> loadedAtlases = new HashMap<>();
   private static final Map<String, Boolean> failedAtlases = new HashMap<>();
   private static final String[] warmupNames = new String[]{"semi", "semi_bold", "bold", "mini", "thin", "icons", "nametagicon"};
   private static int warmupIndex;

   public static void clearCache() {
      loadedAtlases.clear();
      failedAtlases.clear();
      warmupIndex = 0;
   }

   private FontRegistry() {
   }

   static {
      registerFont("montserrat", "fonts/monsterat/montserrat-regular");
      registerFont("montserrat-regular", "fonts/monsterat/montserrat-regular");
      registerFont("montserrat-medium", "fonts/monsterat/montserrat-medium");
      registerFont("montserrat-semibold", "fonts/monsterat/montserrat-semibold");
      registerFont("montserrat-bold", "fonts/monsterat/montserrat-bold");
      registerFont("montserrat-extrabold", "fonts/monsterat/montserrat-extrabold");
      registerFont("montserrat-black", "fonts/monsterat/montserrat-black");
      registerFont("montserrat-light", "fonts/monsterat/montserrat-light");
      registerFont("montserrat-extralight", "fonts/monsterat/montserrat-extralight");
      registerFont("montserrat-thin", "fonts/monsterat/montserrat-thin");
      registerFont("icons", "msdf/icons");
      registerFont("nametagicon", "msdf/nametagicon");
      registerFont("test", "msdf/test");
      registerFont("thin", "msdf/thin");
      registerFont("mini", "msdf/mini");
      registerFont("semi", "msdf/semi");
      registerFont("semi_bold", "msdf/semi_bold");
      registerFont("semi-bold", "msdf/semi_bold");
      registerFont("bold", "msdf/bold");
      registerFont("kimiko", "fonts/kimiko/kimiko");
      registerFont("event-icons", "fonts/event-icons/event-icons");
      registerFont("heart", "fonts/heart/heart");
      registerFont("sf", "fonts/sf-pro/sf-pro-regular");
      registerFont("sf-regular", "fonts/sf-pro/sf-pro-regular");
      registerFont("sf-medium", "fonts/sf-pro/sf-pro-medium");
      registerFont("sf-bold", "fonts/sf-pro/sf-pro-bold");
      registerFont("small-pixel", "fonts/smallpixel/small-pixel");
   }

   public static boolean warmNextFont() {
      if (warmupIndex >= warmupNames.length) {
         return false;
      }

      getAtlas(warmupNames[warmupIndex++]);
      return true;
   }

   public static FontAtlas getAtlas(String local6722_0) {
      String local6722_1 = local6722_0 != null && !local6722_0.isBlank() ? local6722_0.toLowerCase(Locale.ROOT) : "montserrat";
      FontAtlas fontAtlas = loadedAtlases.get(local6722_1);
      if (fontAtlas != null) {
         return fontAtlas;
      } else if (Boolean.TRUE.equals(failedAtlases.get(local6722_1))) {
         return local6722_1.equals("montserrat") ? null : getAtlas("montserrat");
      } else {
         String local6722_3 = resourcePaths.get(local6722_1);
         if (local6722_3 == null) {
            failedAtlases.put(local6722_1, true);
            return local6722_1.equals("montserrat") ? null : getAtlas("montserrat");
         } else {
            FontAtlas fontAtlas2 = FontAtlasLoader.loadAtlas(local6722_3);
            if (fontAtlas2 == null) {
               failedAtlases.put(local6722_1, true);
               return local6722_1.equals("montserrat") ? null : getAtlas("montserrat");
            } else {
               loadedAtlases.put(local6722_1, fontAtlas2);
               return fontAtlas2;
            }
         }
      }
   }

   public static String[] getWarmupNames() {
      return warmupNames;
   }

   private static void registerFont(String local6724_0, String local6724_1) {
      resourcePaths.put(local6724_0.toLowerCase(Locale.ROOT), local6724_1);
   }
}
