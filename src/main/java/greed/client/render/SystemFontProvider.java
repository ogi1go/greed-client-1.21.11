package greed.client.render;

import java.awt.Font;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public final class SystemFontProvider {
   private static final FontRenderContext fontRenderContext = new FontRenderContext(
      new AffineTransform(), RenderingHints.VALUE_TEXT_ANTIALIAS_ON, RenderingHints.VALUE_FRACTIONALMETRICS_OFF
   );
   private final String strVal;
   private final Supplier<Font> supplier;
   private final List<Supplier<Font>> list;
   private final List<Font> list2;
   private final IntFunction<List<Font>> intFunction;
   private final Map<Integer, Font> map = new HashMap<>();
   private Font font4;

   private static boolean matchesCriteria(Font font, int local4942_1) {
      try {
         if (font != null && font.canDisplay(local4942_1)) {
            GlyphVector glyphVector = font.createGlyphVector(fontRenderContext, new String(Character.toChars(local4942_1)));
            return glyphVector.getNumGlyphs() > 0 && glyphVector.getGlyphCode(0) != font.getMissingGlyphCode();
         } else {
            return false;
         }
      } catch (IllegalArgumentException exception) {
         return false;
      }
   }

   public SystemFontProvider(String local4943_1, Supplier<Font> local4943_2, List<Supplier<Font>> local4943_3, IntFunction<List<Font>> local4943_4) {
      this.strVal = local4943_1;
      this.supplier = local4943_2;
      this.list = new ArrayList<>(local4943_3);
      this.list2 = new ArrayList<>(local4943_3.size());

      for (int index = 0; index < local4943_3.size(); index++) {
         this.list2.add(null);
      }

      this.intFunction = local4943_4;
   }

   static String formatText(Font font) {
      return font == null ? "missing" : font.getPSName() + "/" + font.getFontName(Locale.ROOT);
   }

   Font getFont(int local4945_1) {
      return this.map.computeIfAbsent(local4945_1, this::getFont2);
   }

   private Font getFont2(int local4946_1) {
      if (!Character.isValidCodePoint(local4946_1)) {
         return this.getFont3();
      }

      Font font = this.getFont3();
      if (matchesCriteria(font, local4946_1)) {
         return font;
      }

      for (int index = 0; index < this.list.size(); index++) {
         Font font2 = this.list2.get(index);
         if (font2 == null) {
            font2 = this.list.get(index).get();
            this.list2.set(index, font2);
         }

         if (matchesCriteria(font2, local4946_1)) {
            return font2;
         }
      }

      if (this.intFunction != null) {
         for (Font font3 : this.intFunction.apply(local4946_1)) {
            if (matchesCriteria(font3, local4946_1)) {
               return font3;
            }
         }
      }

      return font;
   }

   Font getFont3() {
      if (this.font4 == null) {
         this.font4 = this.supplier.get();
      }

      return this.font4;
   }

   public String formatText2() {
      return this.strVal;
   }
}
