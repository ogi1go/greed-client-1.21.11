package greed.client.render;

import greed.client.GreedClient;
import greed.client.render.RasterGlyphCache;
import greed.client.render.SystemFontProvider;

import java.awt.Font;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;

final class RasterFontAtlas implements AutoCloseable {
   private final Map<String, SystemFontProvider> map = new LinkedHashMap<>();
   private final List<Supplier<Font>> list = new ArrayList<>();
   private final Map<String, Font> map2 = new HashMap<>();
   private final ArrayList<RasterGlyphCache> list2 = new ArrayList<>();
   private final Map<RasterFontAtlas.DataRecord, RasterGlyphCache> map3 = new LinkedHashMap<RasterFontAtlas.DataRecord, RasterGlyphCache>(
      64, 0.75F, true
   ) {
      @Override
      protected boolean removeEldestEntry(Entry<RasterFontAtlas.DataRecord, RasterGlyphCache> local7999_1) {
         if (this.size() <= 160) {
            return false;
         }

         RasterFontAtlas.this.list2.add(local7999_1.getValue());
         return true;
      }
   };
   private final Font font3 = new Font("SansSerif", 0, 1);
   private Supplier<Font> supplier;

   void updateState() {
      for (RasterGlyphCache rasterGlyphCache : this.list2) {
         rasterGlyphCache.close();
      }

      this.list2.clear();
   }

   @Override
   public void close() {
      for (RasterGlyphCache rasterGlyphCache : this.map3.values()) {
         rasterGlyphCache.close();
      }

      this.map3.clear();
      this.map.clear();
      this.list.clear();
      this.map2.clear();
   }

   private Font getFont(String local7970_1) {
      return this.getFont3(local7970_1, () -> this.supplier != null ? this.supplier.get() : this.font3);
   }

   private void updateState2() {
      Supplier<Font> local7971_1 = this.getSupplier("SegoeProDisplay-Semibold.ttf", () -> this.font3);
      Supplier<Font> local7971_2 = this.getSupplier("semi_bold.otf", local7971_1);
      Supplier<Font> local7971_3 = this.getSupplier("open-sans.bold.ttf", local7971_1);
      Supplier<Font> local7971_4 = this.getSupplier("open-sans.semibold.ttf", local7971_3);
      Supplier<Font> local7971_5 = this.getSupplier("bold.otf", local7971_3);
      Supplier<Font> local7971_6 = this.getSupplier("medium.otf", local7971_2);
      Supplier<Font> local7971_7 = this.getSupplier("ZenterSPDemo-Black.otf", local7971_1);
      Supplier<Font> local7971_8 = this.getSupplier("icons.ttf", local7971_1);
      Supplier<Font> local7971_9 = this.getSupplier("NotoSans-Regular.ttf", local7971_1);
      Supplier<Font> local7971_10 = this.getSupplier("NotoSansSymbols-Regular.ttf", local7971_1);
      Supplier<Font> local7971_11 = this.getSupplier("NotoSansSymbols2-Regular.ttf", local7971_1);
      Supplier<Font> local7971_12 = this.getSupplier("NotoNaskhArabic-Regular.ttf", local7971_1);
      this.supplier = local7971_9;
      this.processData(local7971_8, local7971_10, local7971_11, local7971_12, local7971_1, local7971_9);
      this.processData2("default", local7971_1);
      this.processData2("segoe", local7971_1);
      this.processData2("segoe_semibold", local7971_1);
      this.processData2("semi_bold", local7971_2);
      this.processData2("semibold", local7971_2);
      this.processData2("open_sans_bold", local7971_3);
      this.processData2("opensans_bold", local7971_3);
      this.processData2("open_sans_semibold", local7971_4);
      this.processData2("opensans_semibold", local7971_4);
      this.processData2("open_sans", local7971_4);
      this.processData2("opensans", local7971_4);
      this.processData2("bold", local7971_5);
      this.processData2("bold_otf", local7971_5);
      this.processData2("medium", local7971_6);
      this.processData2("medium_otf", local7971_6);
      this.processData2("zenter", local7971_7);
      this.processData2("zenter_black", local7971_7);
      this.processData2("noto", local7971_9);
      this.processData2("noto_sans", local7971_9);
      this.processData2("noto_symbols", local7971_10);
      this.processData2("noto_symbols_1", local7971_10);
      this.processData2("noto_symbols_2", local7971_11);
      this.processData2("noto_symbols2", local7971_11);
      this.processData2("noto_arabic", local7971_12);
      this.processData2("arabic", local7971_12);
      this.processData2("noto_cjk_sc", local7971_9);
      this.processData2("noto_chinese", local7971_9);
      this.processData2("chinese", local7971_9);
      this.processData2("noto_cjk_kr", local7971_9);
      this.processData2("noto_korean", local7971_9);
      this.processData2("korean", local7971_9);
      this.processData2("noto_cjk_jp", local7971_9);
      this.processData2("noto_japanese", local7971_9);
      this.processData2("japanese", local7971_9);
      this.processData2("icons", local7971_8);
      this.processData2("icon", local7971_8);
   }

   RasterFontAtlas() {
      this.updateState2();
   }

   @SafeVarargs
   private void processData(Supplier<Font>... local7973_1) {
      this.list.clear();

      for (Supplier<Font> local7973_5 : local7973_1) {
         this.processData3(local7973_5);
      }

      this.processData3(() -> this.font3);
   }

   private static boolean matchesCriteria(int local7974_0) {
      return local7974_0 >= 4352 && local7974_0 <= 4607
         || local7974_0 >= 12592 && local7974_0 <= 12687
         || local7974_0 >= 43360 && local7974_0 <= 43391
         || local7974_0 >= 44032 && local7974_0 <= 55215;
   }

   private static boolean matchesCriteria2(int local7977_0) {
      return local7977_0 >= 13312 && local7977_0 <= 19903
         || local7977_0 >= 19968 && local7977_0 <= 40959
         || local7977_0 >= 63744 && local7977_0 <= 64255
         || local7977_0 >= 131072 && local7977_0 <= 195103;
   }

   SystemFontProvider getSystemFontProvider(String local7979_1) {
      SystemFontProvider systemFontProvider = this.map.get(formatText(local7979_1));
      return systemFontProvider != null ? systemFontProvider : this.map.get("default");
   }

   private void processData2(String local7981_1, Supplier<Font> local7981_2) {
      this.map.put(formatText(local7981_1), new SystemFontProvider(formatText(local7981_1), local7981_2, this.list, this::getList));
   }

   private Font getFont2(String local7982_1, Font font) {
      try (InputStream resourceAsStream = RasterFontAtlas.class.getResourceAsStream("/assets/client/fonts/" + local7982_1)) {
         if (resourceAsStream == null) {
            GreedClient.logger().warn("[FontRenderer] Font asset is missing: {}", local7982_1);
            return font;
         } else {
            return Font.createFont(0, resourceAsStream).deriveFont(0, 1.0F);
         }
      } catch (Exception exception) {
         GreedClient.logger().warn("[FontRenderer] Failed to load font asset: {}", local7982_1, exception);
         return font;
      }
   }

   private List<Font> getList(int local7983_1) {
      if (matchesCriteria(local7983_1)) {
         return Collections.singletonList(this.getFont("NotoSansCJKkr-Regular.otf"));
      } else if (matchesCriteria3(local7983_1)) {
         return Collections.singletonList(this.getFont("NotoSansCJKjp-Regular.otf"));
      } else {
         return matchesCriteria2(local7983_1) ? Collections.singletonList(this.getFont("NotoSansCJKsc-Regular.otf")) : List.of();
      }
   }

   private static boolean matchesCriteria3(int local7985_0) {
      return local7985_0 >= 12352 && local7985_0 <= 12543 || local7985_0 >= 12784 && local7985_0 <= 12799;
   }

   private Supplier<Font> getSupplier(String local7986_1, Supplier<Font> local7986_2) {
      return () -> this.getFont3(local7986_1, local7986_2);
   }

   private void processData3(Supplier<Font> local7987_1) {
      if (local7987_1 != null) {
         this.list.add(local7987_1);
      }
   }

   private static String formatText(String local7988_0) {
      return local7988_0 != null && !local7988_0.isBlank() ? local7988_0.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_') : "default";
   }

   private static float computeFloat(float local7989_0) {
      return Math.max(1.0F, Math.min(512.0F, Math.round(local7989_0 * 4.0F) / 4.0F));
   }

   private synchronized Font getFont3(String local7990_1, Supplier<Font> local7990_2) {
      Font font = this.map2.get(local7990_1);
      if (font != null) {
         return font;
      }

      Font font2 = this.getFont2(local7990_1, local7990_2.get());
      this.map2.put(local7990_1, font2);
      return font2;
   }

   RasterGlyphCache getRasterGlyphCache(String local7991_1, float local7991_2) {
      String local7991_3 = formatText(local7991_1);
      SystemFontProvider systemFontProvider = this.getSystemFontProvider(local7991_3);
      float local7991_5 = computeFloat(local7991_2);
      RasterFontAtlas.DataRecord local7991_6 = new RasterFontAtlas.DataRecord(systemFontProvider.formatText2(), local7991_5);
      return this.map3.computeIfAbsent(local7991_6, local7978_2 -> new RasterGlyphCache(systemFontProvider, local7991_5));
   }

   private record DataRecord(String name, float size) {
   }
}
