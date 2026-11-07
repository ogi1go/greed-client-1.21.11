package greed.client.render;

import greed.client.render.FontTexture;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphMetrics;
import java.awt.font.GlyphVector;
import java.awt.font.LineMetrics;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.text.Bidi;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class RasterGlyphCache implements AutoCloseable {
   private final SystemFontProvider systemFontProvider;
   private final float fontSize;
   private final int pixelScale;
   private final int glyphPadding;
   private final FontRenderContext fontRenderContext;
   private final Map<RasterGlyphCache.DataRecord, RasterGlyph> glyphCache = new HashMap<>();
   private final Map<Font, Font> derivedFontCache = new HashMap<>();
   private final Map<String, RasterTextLayout> layoutCache = new LinkedHashMap<String, RasterTextLayout>(256, 0.75F, true) {
      @Override
      protected boolean removeEldestEntry(Entry<String, RasterTextLayout> eldestEntry) {
         return this.size() > 1024;
      }
   };
   private final Map<String, Float> widthCache = new LinkedHashMap<String, Float>(256, 0.75F, true) {
      @Override
      protected boolean removeEldestEntry(Entry<String, Float> eldestEntry) {
         return this.size() > 1024;
      }
   };
   private final List<FontTexture> texturePages = new ArrayList<>();
   private final float lineAscent;
   private final float lineHeight;
   private final float spaceAdvance;

   public void updateState() {
      for (FontTexture fontTexture : this.texturePages) {
         fontTexture.updateState();
      }
   }

   private FontTexture.DataRecord allocatePageRecord(int width, int height) {
      for (FontTexture fontTexture : this.texturePages) {
         FontTexture.DataRecord pageRecord = fontTexture.getNested0490(width, height);
         if (pageRecord != null) {
            return pageRecord;
         }
      }

      FontTexture fontTexture2 = new FontTexture(512);
      this.texturePages.add(fontTexture2);
      return fontTexture2.getNested0490(width, height);
   }

   private RasterTextLayout buildTextLayout(String text) {
      Map<FontTexture, List<RasterGlyphQuad>> quadsByPage = new LinkedHashMap<>();
      float cursorX = 0.0F;
      float cursorY = 0.0F;
      float maxLineWidth = 0.0F;
      float baselineStartY = this.lineAscent;
      int textIndex = 0;

      while (textIndex < text.length()) {
         int codePoint = text.codePointAt(textIndex);
         if (codePoint == 10) {
            textIndex += Character.charCount(codePoint);
            maxLineWidth = Math.max(maxLineWidth, cursorX);
            cursorX = 0.0F;
            cursorY += this.lineHeight;
         } else if (codePoint == 9) {
            textIndex += Character.charCount(codePoint);
            cursorX += this.getGlyphForCodePoint(9).advance();
         } else if (Character.isISOControl(codePoint)) {
            textIndex += Character.charCount(codePoint);
         } else {
            Font font = this.getFont(codePoint);
            int runStartIndex = textIndex;
            textIndex += Character.charCount(codePoint);

            while (textIndex < text.length()) {
               int nextCodePoint = text.codePointAt(textIndex);
               if (nextCodePoint == 10 || nextCodePoint == 9 || Character.isISOControl(nextCodePoint)) {
                  break;
               }

               Font font2 = this.getFont(nextCodePoint);
               if (!SystemFontProvider.formatText(font).equals(SystemFontProvider.formatText(font2))) {
                  break;
               }

               textIndex += Character.charCount(nextCodePoint);
            }

            RasterGlyphCache.DataRecord3 glyphRun = this.buildGlyphRun(font, text.substring(runStartIndex, textIndex));

            for (RasterGlyphCache.DataRecord2 glyphData : glyphRun.glyphs()) {
               RasterGlyph rasterGlyph = glyphData.glyph();
               if (rasterGlyph.drawable()) {
                  float glyphX0 = cursorX + glyphData.x() + rasterGlyph.xOffset();
                  float glyphY0 = baselineStartY + cursorY + glyphData.y() + rasterGlyph.yOffset();
                  float glyphX1 = glyphX0 + rasterGlyph.width();
                  float glyphY1 = glyphY0 + rasterGlyph.height();
                  quadsByPage.computeIfAbsent(rasterGlyph.page(), fontTexture -> new ArrayList<>())
                     .add(
                        new RasterGlyphQuad(
                           rasterGlyph.page(),
                           glyphX0,
                           glyphY0,
                           glyphX1,
                           glyphY1,
                           rasterGlyph.u0(),
                           rasterGlyph.v0(),
                           rasterGlyph.u1(),
                           rasterGlyph.v1()
                        )
                     );
               }
            }

            cursorX += glyphRun.advance();
         }
      }

      maxLineWidth = Math.max(maxLineWidth, cursorX);
      List<RasterTextLayout.DataRecord> layoutRecords = new ArrayList<>(quadsByPage.size());

      for (Entry<FontTexture, List<RasterGlyphQuad>> pageEntry : quadsByPage.entrySet()) {
         layoutRecords.add(new RasterTextLayout.DataRecord(pageEntry.getKey(), pageEntry.getValue()));
      }

      return new RasterTextLayout(layoutRecords, maxLineWidth, cursorY + this.lineHeight);
   }

   private float measureRunWidth(Font font, String text) {
      if (text != null && !text.isEmpty()) {
         Font font2 = this.getDerivedFont(font);
         char[] charArray = text.toCharArray();
         int bidiLevel = Bidi.requiresBidi(charArray, 0, charArray.length) && new Bidi(text, -2).isRightToLeft() ? 1 : 0;
         GlyphVector glyphVector = font2.layoutGlyphVector(this.fontRenderContext, charArray, 0, charArray.length, bidiLevel);
         int numGlyphs = glyphVector.getNumGlyphs();
         if (numGlyphs == 0) {
            return 0.0F;
         }

         float textWidth = Math.abs((float)glyphVector.getGlyphPosition(numGlyphs).getX() / this.pixelScale);
         return textWidth > 0.0F ? textWidth : (float)font2.getStringBounds(text, this.fontRenderContext).getWidth() / this.pixelScale;
      } else {
         return 0.0F;
      }
   }

   RasterGlyphCache.DataRecord3 buildGlyphRun(Font font, String text) {
      if (text != null && !text.isEmpty()) {
         Font font2 = this.getDerivedFont(font);
         char[] charArray = text.toCharArray();
         int bidiLevel = Bidi.requiresBidi(charArray, 0, charArray.length) && new Bidi(text, -2).isRightToLeft() ? 1 : 0;
         GlyphVector glyphVector = font2.layoutGlyphVector(this.fontRenderContext, charArray, 0, charArray.length, bidiLevel);
         int numGlyphs = glyphVector.getNumGlyphs();
         if (numGlyphs == 0) {
            return RasterGlyphCache.DataRecord3.EMPTY;
         }

         List<RasterGlyphCache.DataRecord2> glyphs = new ArrayList<>(numGlyphs);
         float minX = 0.0F;
         float maxX = 0.0F;

         for (int index = 0; index < numGlyphs; index++) {
            Point2D glyphPosition = glyphVector.getGlyphPosition(index);
            Point2D glyphPosition2 = glyphVector.getGlyphPosition(index + 1);
            float glyphX = (float)glyphPosition.getX() / this.pixelScale;
            float glyphY = (float)glyphPosition.getY() / this.pixelScale;
            float glyphAdvance = Math.abs((float)(glyphPosition2.getX() - glyphPosition.getX()) / this.pixelScale);
            RasterGlyph rasterGlyph = this.getGlyph(font, glyphVector.getGlyphCode(index), glyphAdvance);
            if (rasterGlyph.drawable()) {
               minX = Math.min(minX, glyphX + rasterGlyph.xOffset());
               maxX = Math.max(maxX, glyphX + rasterGlyph.xOffset() + rasterGlyph.width());
            } else {
               maxX = Math.max(maxX, glyphX + rasterGlyph.advance());
            }

            glyphs.add(new RasterGlyphCache.DataRecord2(rasterGlyph, glyphX, glyphY));
         }

         float totalAdvance = Math.max(Math.abs((float)glyphVector.getGlyphPosition(numGlyphs).getX() / this.pixelScale), maxX - minX);
         float xShift = minX < 0.0F ? -minX : 0.0F;
         if (xShift > 0.0F) {
            List<RasterGlyphCache.DataRecord2> shiftedGlyphs = new ArrayList<>(glyphs.size());

            for (RasterGlyphCache.DataRecord2 glyphData : glyphs) {
               shiftedGlyphs.add(new RasterGlyphCache.DataRecord2(glyphData.glyph(), glyphData.x() + xShift, glyphData.y()));
            }

            glyphs = shiftedGlyphs;
            totalAdvance += xShift;
         }

         return new RasterGlyphCache.DataRecord3(glyphs, totalAdvance);
      } else {
         return RasterGlyphCache.DataRecord3.EMPTY;
      }
   }

   Font getFont(int codePoint) {
      return this.systemFontProvider.getFont(codePoint);
   }

   public float computeFloat2(String text) {
      return text != null && !text.isEmpty() ? this.widthCache.computeIfAbsent(text, this::measureTextWidth) : 0.0F;
   }

   float getLineAscent() {
      return this.lineAscent;
   }

   private float measureTextWidth(String text) {
      float currentLineWidth = 0.0F;
      float maxLineWidth = 0.0F;
      int textIndex = 0;

      while (textIndex < text.length()) {
         int codePoint = text.codePointAt(textIndex);
         if (codePoint == 10) {
            textIndex += Character.charCount(codePoint);
            maxLineWidth = Math.max(maxLineWidth, currentLineWidth);
            currentLineWidth = 0.0F;
         } else if (codePoint == 9) {
            textIndex += Character.charCount(codePoint);
            currentLineWidth += this.spaceAdvance * 4.0F;
         } else if (Character.isISOControl(codePoint)) {
            textIndex += Character.charCount(codePoint);
         } else {
            Font font = this.getFont(codePoint);
            int runStartIndex = textIndex;
            textIndex += Character.charCount(codePoint);

            while (true) {
               if (textIndex < text.length()) {
                  int nextCodePoint = text.codePointAt(textIndex);
                  if (nextCodePoint != 10 && nextCodePoint != 9 && !Character.isISOControl(nextCodePoint)) {
                     Font font2 = this.getFont(nextCodePoint);
                     if (SystemFontProvider.formatText(font).equals(SystemFontProvider.formatText(font2))) {
                        textIndex += Character.charCount(nextCodePoint);
                        continue;
                     }
                  }
               }

               currentLineWidth += this.measureRunWidth(font, text.substring(runStartIndex, textIndex));
               break;
            }
         }
      }

      return Math.max(maxLineWidth, currentLineWidth);
   }

   private RasterGlyph getGlyph(Font font, int glyphCode, float glyphAdvance) {
      RasterGlyphCache.DataRecord cacheKey = new RasterGlyphCache.DataRecord(SystemFontProvider.formatText(font), glyphCode);
      return this.glyphCache.computeIfAbsent(cacheKey, key -> this.rasterizeGlyph(font, glyphCode, glyphAdvance));
   }

   @Override
   public void close() {
      this.glyphCache.clear();
      this.derivedFontCache.clear();
      this.layoutCache.clear();
      this.widthCache.clear();

      for (FontTexture fontTexture : this.texturePages) {
         fontTexture.close();
      }

      this.texturePages.clear();
   }

   float getLineHeight() {
      return this.lineHeight;
   }

   private Font getDerivedFont(Font font) {
      return this.derivedFontCache.computeIfAbsent(font, font2 -> font2.deriveFont(0, this.fontSize * this.pixelScale));
   }

   private RasterGlyph rasterizeGlyph(Font font, int glyphCode, float glyphAdvance) {
      Font font2 = this.getDerivedFont(font);
      GlyphVector glyphVector = font2.createGlyphVector(this.fontRenderContext, new int[]{glyphCode});
      GlyphMetrics glyphMetrics = glyphVector.getGlyphMetrics(0);
      float advance = Math.max(glyphAdvance > 0.0F ? glyphAdvance : glyphMetrics.getAdvance() / this.pixelScale, 0.0F);
      Rectangle pixelBounds = glyphVector.getPixelBounds(this.fontRenderContext, 0.0F, 0.0F);
      if (pixelBounds.width > 0 && pixelBounds.height > 0) {
         int imageWidth = pixelBounds.width + this.glyphPadding * 2;
         int imageHeight = pixelBounds.height + this.glyphPadding * 2;
         if (imageWidth > 0 && imageHeight > 0 && imageWidth <= 512 && imageHeight <= 512) {
            BufferedImage bufferedImage = new BufferedImage(imageWidth, imageHeight, 2);
            Graphics2D graphics2D = bufferedImage.createGraphics();
            graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics2D.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
            graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics2D.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            graphics2D.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            graphics2D.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
            graphics2D.setColor(Color.WHITE);
            graphics2D.drawGlyphVector(glyphVector, this.glyphPadding - pixelBounds.x, this.glyphPadding - pixelBounds.y);
            graphics2D.dispose();
            FontTexture.DataRecord pageRecord = this.allocatePageRecord(imageWidth, imageHeight);
            if (pageRecord == null) {
               return RasterGlyph.empty(advance);
            }

            pageRecord.page().processData(bufferedImage, pageRecord.x(), pageRecord.y());
            float offsetX = (float)(pixelBounds.x - this.glyphPadding) / this.pixelScale;
            float offsetY = (float)(pixelBounds.y - this.glyphPadding) / this.pixelScale;
            float width = (float)imageWidth / this.pixelScale;
            float height = (float)imageHeight / this.pixelScale;
            return new RasterGlyph(
               pageRecord.page(),
               pageRecord.u0(),
               pageRecord.v0(),
               pageRecord.u1(),
               pageRecord.v1(),
               offsetX,
               offsetY,
               width,
               height,
               advance
            );
         } else {
            return RasterGlyph.empty(advance);
         }
      } else {
         return RasterGlyph.empty(advance);
      }
   }

   public RasterTextLayout getRasterTextLayout2(String text) {
      return text != null && !text.isEmpty() ? this.layoutCache.computeIfAbsent(text, this::buildTextLayout) : RasterTextLayout.rasterTextLayout;
   }

   RasterGlyph getGlyphForCodePoint(int codePoint) {
      if (codePoint == 9) {
         RasterGlyph rasterGlyph = this.getGlyphForCodePoint(32);
         return rasterGlyph.withAdvance(rasterGlyph.advance() * 4.0F);
      } else {
         Font font = this.systemFontProvider.getFont(codePoint);
         Font font2 = this.getDerivedFont(font);
         GlyphVector glyphVector = font2.createGlyphVector(this.fontRenderContext, new String(Character.toChars(codePoint)));
         int currentGlyphCode = glyphVector.getGlyphCode(0);
         float advance = glyphVector.getGlyphMetrics(0).getAdvance() / this.pixelScale;
         return this.getGlyph(font, currentGlyphCode, advance);
      }
   }

   public RasterGlyphCache(SystemFontProvider initialSystemFontProvider, float fontSize) {
      this.systemFontProvider = initialSystemFontProvider;
      this.fontSize = fontSize;
      this.pixelScale = PixelSnap.computeInt(fontSize);
      this.glyphPadding = PixelSnap.computeInt2(this.pixelScale);
      this.fontRenderContext = new FontRenderContext(new AffineTransform(), RenderingHints.VALUE_TEXT_ANTIALIAS_ON, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
      Font font = initialSystemFontProvider.getFont3().deriveFont(0, fontSize * this.pixelScale);
      LineMetrics lineMetrics = font.getLineMetrics("Mg", this.fontRenderContext);
      this.lineAscent = (float)Math.round(lineMetrics.getAscent()) / this.pixelScale;
      this.lineHeight = (float)Math.round(Math.max(fontSize * this.pixelScale, lineMetrics.getHeight())) / this.pixelScale;
      this.spaceAdvance = Math.max(font.createGlyphVector(this.fontRenderContext, " ").getGlyphMetrics(0).getAdvance() / this.pixelScale, fontSize * 0.25F);
   }

   private record DataRecord(String fontKey, int glyphCode) {
   }

   record DataRecord2(RasterGlyph glyph, float x, float y) {
   }

   record DataRecord3(List<RasterGlyphCache.DataRecord2> glyphs, float advance) {
      static final RasterGlyphCache.DataRecord3 EMPTY = new RasterGlyphCache.DataRecord3(List.of(), 0.0F);
   }
}
