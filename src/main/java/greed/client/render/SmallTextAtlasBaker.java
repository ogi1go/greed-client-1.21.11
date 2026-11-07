package greed.client.render;

import greed.client.GreedClient;
import greed.client.util.GuiScale;
import greed.client.render.MsdfGlyph;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.imageio.ImageIO;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.resource.Resource;
import org.joml.Matrix3x2f;

final class SmallTextAtlasBaker implements AutoCloseable {
   static final float floatConstant = 20.0F;
   static final int intConstant = 4;
   private static final SmallTextAtlasBaker.AtlasGlyphKey missingGlyphKey = new SmallTextAtlasBaker.AtlasGlyphKey(null, 0.0F, 0.0F, 0.0F, 0.0F, 0, 0, 0, 0);
   private static final FontRenderContext fontRenderContext = new FontRenderContext(
      new AffineTransform(), RenderingHints.VALUE_TEXT_ANTIALIAS_ON, RenderingHints.VALUE_FRACTIONALMETRICS_OFF
   );
   private static final Font font3 = new Font("SansSerif", 0, 32);
   private static final ExecutorService executorService = Executors.newSingleThreadExecutor(runnable -> {
      Thread thread = new Thread(runnable, "greed-msdf-face-loader");
      thread.setDaemon(true);
      return thread;
   });
   private static final Map<Integer, Float> advanceCache = new ConcurrentHashMap<>();
   private final Map<FontAtlas, Optional<SmallTextAtlasBaker.SubContext2>> subContextCache = new ConcurrentHashMap<>();
   private final Map<FontAtlas, Optional<Font>> fontCache = new ConcurrentHashMap<>();
   private final Set<FontAtlas> pendingFonts = ConcurrentHashMap.newKeySet();
   private final Map<FontAtlas, Map<Long, SmallTextAtlasBaker.AtlasGlyphKey>> glyphCacheByFont = new HashMap<>();
   private final Map<Long, SmallTextAtlasBaker.AtlasGlyphKey> rasterGlyphCache = new HashMap<>();
   private final List<SmallTextAtlasBaker.SubContext> subContexts = new ArrayList<>();
   private boolean resetScheduled;
   private long frameCounter;
   private long budgetNanos = 2000000L;

   void updateState() {
      this.clearGlyphCaches();
      this.subContextCache.clear();
      this.fontCache.clear();
      this.pendingFonts.clear();
      this.resetScheduled = false;
   }

   static boolean matchesCriteria(int codePoint) {
      return codePoint > 32 && !Character.isWhitespace(codePoint) && Character.isDefined(codePoint) && font3.canDisplay(codePoint);
   }

   private void clearGlyphCaches() {
      for (SmallTextAtlasBaker.SubContext subContext : this.subContexts) {
         subContext.close();
      }

      this.subContexts.clear();
      this.glyphCacheByFont.clear();
      this.rasterGlyphCache.clear();
   }

   private SmallTextAtlasBaker.AtlasGlyphKey getGlyphKey(
      FontAtlas fontAtlas, int codePoint2, MsdfGlyph msdfGlyph, float scale, float offsetY
   ) {
      Font font = this.getLoadedFont(fontAtlas);
      if (font != null) {
         SmallTextAtlasBaker.AtlasGlyphKey glyphKey = this.bakeRasterGlyph(font, codePoint2, scale, offsetY);
         if (glyphKey != null) {
            return glyphKey;
         }
      }

      return this.bakeMsdfGlyph(fontAtlas, msdfGlyph, scale, offsetY);
   }

   private SmallTextAtlasBaker.AtlasGlyphKey bakeRasterGlyph(Font font, int codePoint3, float scale2, float offsetY2) {
      float scaledSize = scale2 * GuiScale.computeFloat();
      int supersample = 3;
      Font font2 = font.deriveFont(0, scaledSize * supersample);
      GlyphVector glyphVector = font2.createGlyphVector(fontRenderContext, new String(Character.toChars(codePoint3)));
      if (glyphVector.getNumGlyphs() == 0) {
         return missingGlyphKey;
      }

      float scaledOffsetY = offsetY2 * supersample;
      Rectangle pixelBounds = glyphVector.getPixelBounds(fontRenderContext, scaledOffsetY, 0.0F);
      if (pixelBounds.width > 0 && pixelBounds.height > 0) {
         int minCellX = Math.floorDiv(pixelBounds.x, supersample);
         int minCellY = Math.floorDiv(pixelBounds.y, supersample);
         int maxCellX = -Math.floorDiv(-(pixelBounds.x + pixelBounds.width), supersample);
         int maxCellY = -Math.floorDiv(-(pixelBounds.y + pixelBounds.height), supersample);
         int cellWidth = maxCellX - minCellX + 4;
         int cellHeight = maxCellY - minCellY + 4;
         if (cellWidth <= 1024 && cellHeight <= 1024) {
            int pixelWidth = cellWidth * supersample;
            int pixelHeight = cellHeight * supersample;
            BufferedImage bufferedImage = new BufferedImage(pixelWidth, pixelHeight, 2);
            Graphics2D graphics2D = bufferedImage.createGraphics();
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics2D.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
            graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics2D.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
            graphics2D.setColor(Color.WHITE);
            graphics2D.drawGlyphVector(glyphVector, (2 - minCellX) * supersample + scaledOffsetY, (2 - minCellY) * supersample);
            graphics2D.dispose();
            int[] rGB = bufferedImage.getRGB(0, 0, pixelWidth, pixelHeight, null, 0, pixelWidth);
            int[] intArray = new int[cellWidth * cellHeight];
            int supersampleArea = supersample * supersample;

            for (int index = 0; index < cellHeight; index++) {
               for (int index2 = 0; index2 < cellWidth; index2++) {
                  int alphaSum = 0;
                  int blockStart = index * supersample * pixelWidth + index2 * supersample;

                  for (int index3 = 0; index3 < supersample; index3++) {
                     int texelOffset = blockStart + index3 * pixelWidth;

                     for (int index4 = 0; index4 < supersample; index4++) {
                        alphaSum += rGB[texelOffset + index4] >>> 24 & 0xFF;
                     }
                  }

                  intArray[index * cellWidth + index2] = encodeIntensity((float)alphaSum / supersampleArea);
               }
            }

            SmallTextAtlasBaker.DataRecord3 placement = this.allocateGlyphRegion(cellWidth, cellHeight);
            if (placement == null) {
               return missingGlyphKey;
            }

            placement.page.blitPixels(intArray, cellWidth, cellHeight, placement.x, placement.y);
            float invAtlasSize = 9.765625E-4F;
            return new SmallTextAtlasBaker.AtlasGlyphKey(
               placement.page,
               placement.x * invAtlasSize,
               placement.y * invAtlasSize,
               (placement.x + cellWidth) * invAtlasSize,
               (placement.y + cellHeight) * invAtlasSize,
               minCellX - 2,
               -minCellY + 2,
               cellWidth,
               cellHeight
            );
         } else {
            return missingGlyphKey;
         }
      } else {
         return missingGlyphKey;
      }
   }

   static boolean matchesCriteria2(Matrix3x2f matrix3x2f) {
      float guiScale = GuiScale.computeFloat();
      int guiScaleInt = GuiScale.computeInt();
      return Math.abs(matrix3x2f.m01()) < 1.0E-4F
         && Math.abs(matrix3x2f.m10()) < 1.0E-4F
         && Math.abs(matrix3x2f.m00() * guiScaleInt - guiScale) < 0.001F
         && Math.abs(matrix3x2f.m11() * guiScaleInt - guiScale) < 0.001F;
   }

   SmallTextAtlasBaker.AtlasGlyphKey getNested04963(int codePoint4, float scale3, int phase) {
      long cacheKey = (long)codePoint4 << 16 | (long)Math.round(scale3 * 4.0F) << 2 | phase;
      SmallTextAtlasBaker.AtlasGlyphKey glyphKey2 = this.rasterGlyphCache.get(cacheKey);
      if (glyphKey2 == null) {
         long startNanos = System.nanoTime();
         glyphKey2 = this.bakeRasterGlyph(font3, codePoint4, scale3, phase / 4.0F);
         this.budgetNanos = this.budgetNanos - (System.nanoTime() - startNanos);
         this.rasterGlyphCache.put(cacheKey, glyphKey2);
      }

      if (glyphKey2.page() != null) {
         glyphKey2.page().lastUsedFrame = this.frameCounter;
      }

      return glyphKey2;
   }

   void updateState3() {
      for (SmallTextAtlasBaker.SubContext subContext2 : this.subContexts) {
         subContext2.flushDirtyRegion();
      }
   }

   private SmallTextAtlasBaker.SubContext2 getSubContext(FontAtlas fontAtlas) {
      Optional<SmallTextAtlasBaker.SubContext2> cachedSubContext = this.subContextCache.get(fontAtlas);
      return cachedSubContext == null ? null : cachedSubContext.orElse(null);
   }

   void updateState4() {
      this.budgetNanos = 2000000L;
      this.frameCounter++;
   }

   private static int encodeIntensity(float intensity) {
      double normalized = clamp01(intensity / 255.0F);
      return (int)Math.min(255L, Math.round(Math.pow(normalized, 0.72) * 255.0));
   }

   boolean matchesCriteria3(FontAtlas fontAtlas, int codePoint5, MsdfGlyph msdfGlyph, float scale4, int phase2) {
      Map<Long, SmallTextAtlasBaker.AtlasGlyphKey> glyphCache = this.glyphCacheByFont.computeIfAbsent(fontAtlas, fontAtlas2 -> new HashMap<>());
      long cacheKey2 = (long)codePoint5 << 16 | (long)Math.round(scale4 * 4.0F) << 2 | phase2;
      if (glyphCache.containsKey(cacheKey2)) {
         return true;
      }

      if (this.budgetNanos <= 0L) {
         return false;
      }

      long startNanos2 = System.nanoTime();
      glyphCache.put(cacheKey2, this.getGlyphKey(fontAtlas, codePoint5, msdfGlyph, scale4, phase2 / 4.0F));
      this.budgetNanos = this.budgetNanos - (System.nanoTime() - startNanos2);
      return true;
   }

   boolean hasBudget() {
      return this.budgetNanos > 0L;
   }

   private static float sampleMsdfValue(
      SmallTextAtlasBaker.SubContext2 subContext3,
      FontAtlas fontAtlas,
      MsdfGlyph msdfGlyph,
      float x0,
      float x1,
      float y0,
      float y1,
      float u,
      float v,
      float padding
   ) {
      float planeWidth = msdfGlyph.planeRight() - msdfGlyph.planeLeft();
      float planeHeight = msdfGlyph.planeTop() - msdfGlyph.planeBottom();
      float sampleX = x0 + (u - msdfGlyph.planeLeft()) / planeWidth * (x1 - x0);
      float sampleY = y0 + (msdfGlyph.planeTop() - v) / planeHeight * (y1 - y0);
      boolean outsidePadding = u < msdfGlyph.planeLeft() + padding
         || u > msdfGlyph.planeRight() - padding
         || v < msdfGlyph.planeBottom() + padding
         || v > msdfGlyph.planeTop() - padding;
      sampleX = clamp(sampleX, x0 + 0.5F, x1 - 0.5F);
      sampleY = clamp(sampleY, y0 + 0.5F, y1 - 0.5F);
      float msdfValue = subContext3.sampleMsdf(sampleX, sampleY);
      float distance = (msdfValue - 0.5F) * fontAtlas.computeFloat8();
      return outsidePadding ? Math.min(distance, -0.01F) : distance;
   }

   boolean matchesCriteria4(int codePoint6, float scale5, int phase3) {
      long cacheKey3 = (long)codePoint6 << 16 | (long)Math.round(scale5 * 4.0F) << 2 | phase3;
      if (this.rasterGlyphCache.containsKey(cacheKey3)) {
         return true;
      }

      if (this.budgetNanos <= 0L) {
         return false;
      }

      long startNanos3 = System.nanoTime();
      this.rasterGlyphCache.put(cacheKey3, this.bakeRasterGlyph(font3, codePoint6, scale5, phase3 / 4.0F));
      this.budgetNanos = this.budgetNanos - (System.nanoTime() - startNanos3);
      return true;
   }

   private static float clamp(float candidate, float min, float max) {
      return Math.max(min, Math.min(max, candidate));
   }

   private SmallTextAtlasBaker.AtlasGlyphKey bakeMsdfGlyph(FontAtlas fontAtlas, MsdfGlyph msdfGlyph, float scale6, float offsetY3) {
      SmallTextAtlasBaker.SubContext2 subContext4 = this.getSubContext(fontAtlas);
      if (subContext4 == null) {
         return missingGlyphKey;
      }

      float planeWidth2 = msdfGlyph.planeRight() - msdfGlyph.planeLeft();
      float planeHeight2 = msdfGlyph.planeTop() - msdfGlyph.planeBottom();
      if (!(planeWidth2 <= 0.0F) && !(planeHeight2 <= 0.0F)) {
         float u0px = msdfGlyph.u0() * subContext4.atlasWidth;
         float u1px = msdfGlyph.u1() * subContext4.atlasWidth;
         float v0px = msdfGlyph.v0() * subContext4.atlasHeight;
         float v1px = msdfGlyph.v1() * subContext4.atlasHeight;
         float texelsPerUnit = (u1px - u0px) / planeWidth2;
         if (texelsPerUnit <= 0.0F) {
            return missingGlyphKey;
         }

         float scaledSize2 = scale6 * GuiScale.computeFloat();
         int cellMinX = Math.round(msdfGlyph.planeLeft() * scaledSize2 + offsetY3) - 2;
         int cellTop = Math.round(msdfGlyph.planeTop() * scaledSize2) + 2;
         int cellWidth2 = (int)Math.ceil(planeWidth2 * scaledSize2) + 4 + 2;
         int cellHeight2 = (int)Math.ceil(planeHeight2 * scaledSize2) + 4 + 1;
         if (cellWidth2 <= 1024 && cellHeight2 <= 1024) {
            float scalePerTexel = scaledSize2 / texelsPerUnit;
            float invTexelsPerUnit = 1.0F / texelsPerUnit;
            int[] intArray = new int[cellWidth2 * cellHeight2];

            for (int index = 0; index < cellHeight2; index++) {
               for (int index2 = 0; index2 < cellWidth2; index2++) {
                  float msdfSum = 0.0F;

                  for (int index3 = 0; index3 < 4; index3++) {
                     for (int index4 = 0; index4 < 4; index4++) {
                        float subpixelX = (index4 + 0.5F) / 4.0F - 0.5F;
                        float subpixelY = (index3 + 0.5F) / 4.0F - 0.5F;
                        float sampleX2 = (cellMinX + index2 + 0.5F + subpixelX - offsetY3) / scaledSize2;
                        float sampleY2 = (cellTop - index - 0.5F - subpixelY) / scaledSize2;
                        float msdfValue2 = sampleMsdfValue(
                           subContext4,
                           fontAtlas,
                           msdfGlyph,
                           u0px,
                           u1px,
                           v0px,
                           v1px,
                           sampleX2,
                           sampleY2,
                           invTexelsPerUnit
                        );
                        float scaledValue = msdfValue2 * scalePerTexel;
                        msdfSum += clamp01(scaledValue * 4.0F + 0.5F);
                     }
                  }

                  intArray[index * cellWidth2 + index2] = encodeIntensity(msdfSum / 16.0F * 255.0F);
               }
            }

            SmallTextAtlasBaker.DataRecord3 placement2 = this.allocateGlyphRegion(cellWidth2, cellHeight2);
            if (placement2 == null) {
               return missingGlyphKey;
            }

            placement2.page.blitPixels(intArray, cellWidth2, cellHeight2, placement2.x, placement2.y);
            float invAtlasSize2 = 9.765625E-4F;
            return new SmallTextAtlasBaker.AtlasGlyphKey(
               placement2.page,
               placement2.x * invAtlasSize2,
               placement2.y * invAtlasSize2,
               (placement2.x + cellWidth2) * invAtlasSize2,
               (placement2.y + cellHeight2) * invAtlasSize2,
               cellMinX,
               cellTop,
               cellWidth2,
               cellHeight2
            );
         } else {
            return missingGlyphKey;
         }
      } else {
         return missingGlyphKey;
      }
   }

   @Override
   public void close() {
      this.updateState();
   }

   private static Font loadFont(FontAtlas fontAtlas) {
      String resourcePath = resolveFontPath(fontAtlas);
      if (resourcePath == null) {
         return null;
      }

      try (InputStream resourceAsStream = SmallTextAtlasBaker.class.getResourceAsStream(resourcePath)) {
         if (resourceAsStream == null) {
            GreedClient.logger().warn("[MSDF] Inter OTF not found for small-text bake: {}", resourcePath);
            return null;
         } else {
            return Font.createFont(0, resourceAsStream).deriveFont(0, 1.0F);
         }
      } catch (Exception exception) {
         GreedClient.logger().warn("[MSDF] Failed to load Inter OTF {}", resourcePath, exception);
         return null;
      }
   }

   private void scheduleFontLoad(FontAtlas fontAtlas) {
      if (this.pendingFonts.add(fontAtlas)) {
         executorService.submit(
            () -> {
               try {
                  font3.canDisplay('A');
                  if (MinecraftClient.getInstance() == null) {
                     this.pendingFonts.remove(fontAtlas);
                     return;
                  }

                  Optional<Font> loadedFont = Optional.ofNullable(loadFont(fontAtlas));
                  this.subContextCache
                     .put(fontAtlas, loadedFont.isPresent() ? Optional.empty() : Optional.ofNullable(SmallTextAtlasBaker.SubContext2.loadAtlas(fontAtlas)));
                  this.fontCache.put(fontAtlas, loadedFont);
               } catch (Throwable throwable) {
                  this.subContextCache.putIfAbsent(fontAtlas, Optional.empty());
                  this.fontCache.putIfAbsent(fontAtlas, Optional.empty());
               }
            }
         );
      }
   }

   private static String resolveFontPath(FontAtlas fontAtlas) {
      String path = fontAtlas.getIdentifier().getPath();
      String fileName = path.substring(path.lastIndexOf(47) + 1);
      if (fileName.endsWith(".png")) {
         fileName = fileName.substring(0, fileName.length() - 4);
      }

      String basePath = "/assets/greed/fonts/inter/";

      return switch (fileName.toLowerCase(Locale.ROOT)) {
         case "thin" -> basePath + "Inter-Thin.otf";
         case "mini" -> basePath + "Inter-Light.otf";
         case "semi" -> basePath + "Inter-Medium.otf";
         case "semi_bold" -> basePath + "Inter-SemiBold.otf";
         case "bold" -> basePath + "Inter-Bold.otf";
         default -> null;
      };
   }

   private SmallTextAtlasBaker.SubContext evictOldestPage() {
      SmallTextAtlasBaker.SubContext candidatePage = null;

      for (SmallTextAtlasBaker.SubContext subContext5 : this.subContexts) {
         if (subContext5.lastUsedFrame != this.frameCounter && (candidatePage == null || subContext5.lastUsedFrame < candidatePage.lastUsedFrame)) {
            candidatePage = subContext5;
         }
      }

      if (candidatePage == null) {
         return null;
      }

      SmallTextAtlasBaker.SubContext evictedPage = candidatePage;

      for (Map<Long, SmallTextAtlasBaker.AtlasGlyphKey> glyphCache2 : this.glyphCacheByFont.values()) {
         glyphCache2.values().removeIf(glyphKey3 -> glyphKey3.page() == evictedPage);
      }

      this.rasterGlyphCache.values().removeIf(glyphKey4 -> glyphKey4.page() == evictedPage);
      evictedPage.resetLayout();
      return evictedPage;
   }

   static float computeFloat3(int glyphCode) {
      return advanceCache.computeIfAbsent(
         glyphCode,
         cachedCode -> font3.createGlyphVector(fontRenderContext, new String(Character.toChars(cachedCode))).getGlyphMetrics(0).getAdvance() / 32.0F
      );
   }

   private static float clamp01(float unitValue) {
      return Math.max(0.0F, Math.min(1.0F, unitValue));
   }

   private Font getLoadedFont(FontAtlas fontAtlas) {
      Optional<Font> cachedFont = this.fontCache.get(fontAtlas);
      return cachedFont == null ? null : cachedFont.orElse(null);
   }

   void updateState5() {
      if (this.resetScheduled) {
         this.resetScheduled = false;
         this.clearGlyphCaches();
      }
   }

   static float computeFloat5(float rawScale) {
      return Math.max(0.5F, Math.round(rawScale * 4.0F) / 4.0F);
   }

   SmallTextAtlasBaker.AtlasGlyphKey getNested04965(FontAtlas fontAtlas, int codePoint7, MsdfGlyph msdfGlyph, float scale7, int phase4) {
      Map<Long, SmallTextAtlasBaker.AtlasGlyphKey> glyphCache3 = this.glyphCacheByFont.computeIfAbsent(fontAtlas, fontAtlas2 -> new HashMap<>());
      long cacheKey4 = (long)codePoint7 << 16 | (long)Math.round(scale7 * 4.0F) << 2 | phase4;
      SmallTextAtlasBaker.AtlasGlyphKey glyphKey5 = glyphCache3.get(cacheKey4);
      if (glyphKey5 == null) {
         long startNanos4 = System.nanoTime();
         glyphKey5 = this.getGlyphKey(fontAtlas, codePoint7, msdfGlyph, scale7, phase4 / 4.0F);
         this.budgetNanos = this.budgetNanos - (System.nanoTime() - startNanos4);
         glyphCache3.put(cacheKey4, glyphKey5);
      }

      if (glyphKey5.page() != null) {
         glyphKey5.page().lastUsedFrame = this.frameCounter;
      }

      return glyphKey5;
   }

   private SmallTextAtlasBaker.DataRecord3 allocateGlyphRegion(int width, int height) {
      for (SmallTextAtlasBaker.SubContext subContext6 : this.subContexts) {
         SmallTextAtlasBaker.DataRecord3 placement3 = subContext6.allocateSlot(width, height);
         if (placement3 != null) {
            return placement3;
         }
      }

      if (this.subContexts.size() >= 16) {
         SmallTextAtlasBaker.SubContext reusedPage = this.evictOldestPage();
         if (reusedPage == null) {
            this.resetScheduled = true;
            GreedClient.logger().warn("[MSDF] Small-text atlas overflow ({} pages), reset scheduled", this.subContexts.size() + 1);
            return null;
         } else {
            return reusedPage.allocateSlot(width, height);
         }
      } else {
         SmallTextAtlasBaker.SubContext reusedPage = new SmallTextAtlasBaker.SubContext();
         this.subContexts.add(reusedPage);
         return reusedPage.allocateSlot(width, height);
      }
   }

   boolean matchesCriteria5(FontAtlas fontAtlas) {
      Optional<Font> cachedFont2 = this.fontCache.get(fontAtlas);
      Optional<SmallTextAtlasBaker.SubContext2> cachedSubContext2 = this.subContextCache.get(fontAtlas);
      if (cachedFont2 != null && cachedSubContext2 != null) {
         return cachedFont2.isPresent() || cachedSubContext2.isPresent();
      }

      this.scheduleFontLoad(fontAtlas);
      return false;
   }

   static final class SubContext implements AutoCloseable {
      private static int atlasSerial;
      private final NativeImage nativeImage;
      private final NativeImageBackedTexture nativeImageBackedTexture;
      private final TextureSetup textureSetup;
      private int cursorX = 1;
      private int cursorY = 1;
      private int rowHeight;
      private long lastUsedFrame;
      private int dirtyMinX = Integer.MAX_VALUE;
      private int dirtyMinY = Integer.MAX_VALUE;
      private int dirtyMaxX = Integer.MIN_VALUE;
      private int dirtyMaxY = Integer.MIN_VALUE;

      @Override
      public void close() {
         this.nativeImageBackedTexture.close();
      }

      private void blitPixels(int[] intArray, int width2, int height2, int destX, int destY) {
         for (int index = 0; index < height2; index++) {
            for (int index2 = 0; index2 < width2; index2++) {
               int argb = intArray[index * width2 + index2];
               this.nativeImage.setColor(destX + index2, destY + index, argb << 24 | 16777215);
            }
         }

         this.dirtyMinX = Math.min(this.dirtyMinX, destX);
         this.dirtyMinY = Math.min(this.dirtyMinY, destY);
         this.dirtyMaxX = Math.max(this.dirtyMaxX, destX + width2);
         this.dirtyMaxY = Math.max(this.dirtyMaxY, destY + height2);
      }

      private SmallTextAtlasBaker.DataRecord3 allocateSlot(int width3, int height3) {
         int slotWidth = width3 + 1;
         int slotHeight = height3 + 1;
         if (this.cursorX + slotWidth > 1024) {
            this.cursorX = 1;
            this.cursorY = this.cursorY + this.rowHeight;
            this.rowHeight = 0;
         }

         if (this.cursorY + slotHeight <= 1024 && slotWidth <= 1024) {
            int slotX = this.cursorX;
            int slotY = this.cursorY;
            this.cursorX += slotWidth;
            this.rowHeight = Math.max(this.rowHeight, slotHeight);
            return new SmallTextAtlasBaker.DataRecord3(this, slotX, slotY);
         } else {
            return null;
         }
      }

      private void resetLayout() {
         this.nativeImage.fillRect(0, 0, 1024, 1024, 0);
         this.cursorX = 1;
         this.cursorY = 1;
         this.rowHeight = 0;
         this.dirtyMinX = 0;
         this.dirtyMinY = 0;
         this.dirtyMaxX = 1024;
         this.dirtyMaxY = 1024;
      }

      SubContext() {
         this.nativeImage = new NativeImage(1024, 1024, true);
         this.nativeImageBackedTexture = new NativeImageBackedTexture(() -> "greed_msdf_small_atlas_" + atlasSerial++, this.nativeImage);
         this.textureSetup = TextureSetup.of(this.nativeImageBackedTexture.getGlTextureView(), RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
      }

      private void flushDirtyRegion() {
         if (this.dirtyMaxX > this.dirtyMinX && this.dirtyMaxY > this.dirtyMinY) {
            if (this.nativeImageBackedTexture.getGlTexture() != null) {
               int minX = Math.max(0, this.dirtyMinX);
               int minY = Math.max(0, this.dirtyMinY);
               int regionWidth = Math.min(1024, this.dirtyMaxX) - minX;
               int regionHeight = Math.min(1024, this.dirtyMaxY) - minY;
               if (regionWidth > 0 && regionHeight > 0) {
                  RenderSystem.getDevice()
                     .createCommandEncoder()
                     .writeToTexture(
                        this.nativeImageBackedTexture.getGlTexture(), this.nativeImage, 0, 0, minX, minY, regionWidth, regionHeight, minX, minY
                     );
               }

               this.dirtyMinX = Integer.MAX_VALUE;
               this.dirtyMinY = Integer.MAX_VALUE;
               this.dirtyMaxX = Integer.MIN_VALUE;
               this.dirtyMaxY = Integer.MIN_VALUE;
            }
         }
      }

      TextureSetup getTextureSetup() {
         return this.textureSetup;
      }
   }

   record AtlasGlyphKey(
      SmallTextAtlasBaker.SubContext page,
      float u0,
      float v0,
      float u1,
      float v1,
      int offsetX,
      int offsetY,
      int width,
      int height
   ) {
      boolean drawable() {
         return this.page != null && this.width > 0 && this.height > 0;
      }
   }

   private static final class SubContext2 {
      final int atlasWidth;
      final int atlasHeight;
      private final int[] pixels;

      private float sampleChannel(int pixelX, int pixelY, int channelShift) {
         pixelX = Math.max(0, Math.min(this.atlasWidth - 1, pixelX));
         pixelY = Math.max(0, Math.min(this.atlasHeight - 1, pixelY));
         return (this.pixels[pixelY * this.atlasWidth + pixelX] >>> channelShift & 0xFF) / 255.0F;
      }

      static SmallTextAtlasBaker.SubContext2 loadAtlas(FontAtlas fontAtlas) {
         MinecraftClient minecraftClient = MinecraftClient.getInstance();
         if (minecraftClient == null) {
            return null;
         }

         Optional<Resource> atlasResource = minecraftClient.getResourceManager().getResource(fontAtlas.getIdentifier());
         if (atlasResource.isEmpty()) {
            GreedClient.logger().warn("[MSDF] Atlas PNG not readable for small-text bake: {}", fontAtlas.getIdentifier());
            return null;
         }

         try (InputStream inputStream = atlasResource.get().getInputStream()) {
            BufferedImage bufferedImage = ImageIO.read(inputStream);
            if (bufferedImage == null) {
               return null;
            }

            int currentWidth = bufferedImage.getWidth();
            int currentHeight = bufferedImage.getHeight();
            return new SmallTextAtlasBaker.SubContext2(currentWidth, currentHeight, bufferedImage.getRGB(0, 0, currentWidth, currentHeight, null, 0, currentWidth));
         } catch (Exception exception) {
            GreedClient.logger().warn("[MSDF] Failed to load atlas PNG for small-text bake: {}", fontAtlas.getIdentifier(), exception);
            return null;
         }
      }

      private float sampleBilinear(int samplePixelX, int samplePixelY, float fracX, float fracY, int channelShift2) {
         float c00 = this.sampleChannel(samplePixelX, samplePixelY, channelShift2);
         float c10 = this.sampleChannel(samplePixelX + 1, samplePixelY, channelShift2);
         float c01 = this.sampleChannel(samplePixelX, samplePixelY + 1, channelShift2);
         float c11 = this.sampleChannel(samplePixelX + 1, samplePixelY + 1, channelShift2);
         float top = c00 + (c10 - c00) * fracX;
         float bottom = c01 + (c11 - c01) * fracX;
         return top + (bottom - top) * fracY;
      }

      float sampleMsdf(float u2, float v2) {
         u2 -= 0.5F;
         v2 -= 0.5F;
         int sampleCellX = (int)Math.floor(u2);
         int sampleCellY = (int)Math.floor(v2);
         float cellFracX = u2 - sampleCellX;
         float cellFracY = v2 - sampleCellY;
         float redChannel = this.sampleBilinear(sampleCellX, sampleCellY, cellFracX, cellFracY, 16);
         float greenChannel = this.sampleBilinear(sampleCellX, sampleCellY, cellFracX, cellFracY, 8);
         float blueChannel = this.sampleBilinear(sampleCellX, sampleCellY, cellFracX, cellFracY, 0);
         return Math.max(Math.min(redChannel, greenChannel), Math.min(Math.max(redChannel, greenChannel), blueChannel));
      }

      private SubContext2(int width4, int height4, int[] intArray) {
         this.atlasWidth = width4;
         this.atlasHeight = height4;
         this.pixels = intArray;
      }
   }

   private record DataRecord3(SmallTextAtlasBaker.SubContext page, int x, int y) {
   }
}
