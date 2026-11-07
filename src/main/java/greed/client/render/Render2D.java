package greed.client.render;

import greed.client.GreedClient;
import greed.mixin.DrawContextMixin;
import greed.client.util.FormattedText;
import greed.client.module.misc.NameProtectModule;
import greed.client.render.RasterTextDrawData;
import greed.client.render.Outline360DrawData;
import greed.client.util.GuiScale;
import greed.client.render.GuiLayerSerialAccess;
import greed.client.render.ArcColorSpan;
import greed.client.render.WarpTextDrawData;
import greed.client.render.BlobDrawData;
import greed.client.render.RippleDrawData;
import greed.client.render.LineDrawData;
import greed.client.render.PickerDrawData;
import greed.client.render.CircleDrawData;
import greed.client.render.ThemePaletteBuffer;
import greed.client.render.ItemRenderCache;
import greed.client.render.ImageDrawData;
import greed.client.render.CornerBracketDrawData;
import greed.client.render.SoftGlowDrawData;
import greed.client.render.ArcDividerDrawData;
import greed.client.render.MenuGridDrawData;
import greed.client.render.HalfIconDrawData;
import greed.client.render.DitherDrawData;
import greed.client.render.GlassDrawData;
import greed.client.render.ShapeDrawData;
import greed.client.render.BlurDrawBuilder;
import greed.client.render.RectDrawData;
import greed.client.render.GlassOutlineDrawData;
import greed.client.render.ScissorStack;
import greed.client.render.BlurDrawData;
import greed.client.render.MsdfTextDrawData;
import greed.client.render.GlowCompositeDrawData;
import greed.client.render.ShimmerDrawData;
import greed.client.render.OutlineDrawData;
import greed.client.render.RoundedClipStack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraft.util.Identifier;

public final class Render2D {
   private static boolean msdfReloaderRegistered;
   private static boolean renderPassActive;
   private static String fallbackGlyphs = "∅∆∏∑√∞∫≈≠₡₣₤₦₩₫₭₮₱₲₴₵₸₹₺₼₽ƏƒƠƷǅǎǓǤǯǺȞȫȷʒѢѫѲҊҝҠӿḈḝṚẅẎẓẞΔΩμπ№™℮‰†‡ĐđĦħĿŀŁłŊŋŒœŦŧĲĳĸſǆǇǈǉǊǋǌǪǫǮǻǼǽǾǿȀȁȂȃȄȅȆȇȈȉȊȋȌȍȎȏȐȑȒȓȘșȚțȟȪȬȭȰȱȲȳəʹʻʾˆˇ˘˙˚˛˜˝ЀЂЃЄЅІЇЈЉЊЋЌЍЎЏѣѳѴѵҋҌҍҎҏҐґҒғҔҕҖҗҘҙҚқҜҡҢңҤҥҦҧҨҩҪҫҬҭҮүҰұҲҳҴҵҶҷҸҹҺһҼҽҾҿӀӁӂӃӄӆӇӈӉӊӋӌӍӎӏӔӕӘәӚӛӜӝӞӟӠӡӢӣӤӥӦӧӨөӪӫӬӭӮӯӰӱӲӳӴӵӶӷӸӹӺӻӼӽӾԯḉḌḍḎḏḔḕḖḗḜḠḡḤḥḪḫḮḯḶḷḺḻṂṃṄṅṆṇṈṉṌṍṎṏṐṑṒṓṛṞṟṠṡṢṣṤṥṦṧṨṩṬṭṮṯṸṹṺṻẀẁẂẃẄẏẒẗẠạẢảẤấẦầẨẩẪẫẬậẮắẰằẲẳẴẵẶặẸẹẺẻẼẽẾếỀềỂểỄễỆệỈỉỊịỌọỎỏỐốỒồỔổỖỗỘộỚớỜờỞởỠỡỢợỤụỦủỨứỪừỬửỮữỰựỲỳỴỵỶỷỸỹ⁰⁴⁵⁶⁷⁸⁹₀₁₂₃₄₅₆₇₈₉€₧ℓ′″⁄⁒•…アイウエオカキクケコサシスセソタチツテトナニヌネノハヒフヘホマミムメモヤユヨラリルレロワヲン天地火水木金土日月山川風雨雷電光闇魔剣龍神鬼王力心目手口人大小中上下左右前後東西南北春夏秋冬星空海島城門道車馬鳥魚虎狼熊蛇竜獣霊魂死生愛憎戦争平和";
   private static final HashMap<String, String> obfuscationCache = new HashMap<>();
   private static final ArrayDeque<Float> alphaStack = new ArrayDeque<>();
   private static float globalAlpha = 1.0F;

   public static void drawText(String fontName, String text, float x, float y, float size, int color) {
      refreshRendererState();
      if (text != null && text.indexOf(167) >= 0) {
         StringBuilder accumulatedText = new StringBuilder();

         for (FormattedText.DataRecord dataRecord : FormattedText.getArrayList(text, color)) {
            String formattedText = dataRecord.text().indexOf(167) >= 0 ? FormattedText.formatText(dataRecord.text()) : dataRecord.text();
            if (!formattedText.isEmpty()) {
               if (dataRecord.obfuscated()) {
                  drawObfuscatedText(
                     fontName,
                     formattedText,
                     x + measureText(fontName, accumulatedText.toString(), size),
                     y,
                     size,
                     dataRecord.color() & 16777215 | color & 0xFF000000
                  );
               } else {
                  drawNameProtectedText(
                     fontName,
                     formattedText,
                     x + measureText(fontName, accumulatedText.toString(), size),
                     y,
                     size,
                     dataRecord.color() & 16777215 | color & 0xFF000000
                  );
               }

               accumulatedText.append(formattedText);
            }
         }
      } else {
         drawNameProtectedText(fontName, text, x, y, size, color);
      }
   }

   private static WarpTextRenderer getWarpTextRenderer() {
      return WarpTextRenderer.getWarpTextRenderer();
   }

   private static RectHalfIconRenderer getRectHalfIconRenderer() {
      return RectHalfIconRenderer.getRectHalfIconRenderer();
   }

   public static void drawTextRotated(
      String fontName,
      String text,
      float x,
      float y,
      float size,
      int color,
      float rotationDegrees,
      float rotationOriginX,
      float rotationOriginY
   ) {
      refreshRendererState();
      text = NameProtectModule.getInstance().formatText(text);
      String resolvedFontName = normalizeFontName(fontName);
      if (resolvedFontName != null) {
         getMsdfTextRenderer()
            .processData(
               new MsdfTextDrawData(
                  resolvedFontName, text, x, y, size, applyAlpha(color), rotationDegrees, rotationOriginX, rotationOriginY
               )
            );
      } else {
         getTextAtlasRenderer()
            .processData(
               new RasterTextDrawData(
                  fontName,
                  text,
                  x,
                  y,
                  size,
                  color,
                  color,
                  color,
                  color,
                  rotationDegrees,
                  rotationOriginX,
                  rotationOriginY,
                  false,
                  false,
                  0.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  true
               )
            );
      }
   }

   public static void drawOutlineArc(
      float x,
      float y,
      float width,
      float height,
      float radiusTopLeft,
      float radiusTopRight,
      float radiusBottomRight,
      float radiusBottomLeft,
      float thickness,
      int defaultColor,
      List<ArcColorSpan> ranges
   ) {
      refreshRendererState();
      defaultColor = applyAlpha(defaultColor);
      ranges = applyAlphaToSpans(ranges);
      getOutline360Renderer()
         .processData4(
            new Outline360DrawData(
               x, y, width, height, radiusTopLeft, radiusTopRight, radiusBottomRight, radiusBottomLeft, thickness, defaultColor, ranges
            )
         );
   }

   private static ImageRenderer getImageRenderer() {
      return ImageRenderer.getImageRenderer();
   }

   public static void drawSoftGlow(
      float x, float y, float width, float height, float radius, int color, float softness, float intensity
   ) {
      drawSoftGlowData(new SoftGlowDrawData(x, y, width, height, radius, color, softness).withIntensity(intensity));
   }

   private static void drawNameProtectedText(String fontName, String text, float x, float y, float size, int color) {
      if (text != null && !NameProtectModule.getInstance().formatText(text).equals(text)) {
         String formattedText = NameProtectModule.getInstance().formatText(text);
         String numericText = NameProtectModule.getInstance().numericTextSetting.getText();
         float currentX = x;
         int cursor = 0;

         while (cursor < formattedText.length()) {
            int matchIndex = formattedText.indexOf(numericText, cursor);
            if (matchIndex < 0) {
               drawTextRun(fontName, formattedText.substring(cursor), currentX, y, size, color);
               return;
            }

            if (matchIndex > cursor) {
               drawTextRun(fontName, formattedText.substring(cursor, matchIndex), currentX, y, size, color);
               currentX += measureText(fontName, formattedText.substring(cursor, matchIndex), size);
            }

            for (int index = 0; index < numericText.length(); index++) {
               drawTextRun(
                  fontName,
                  String.valueOf(numericText.charAt(index)),
                  currentX,
                  y,
                  size,
                  NameProtectModule.getInstance().computeInt(index, color)
               );
               currentX += measureText(fontName, String.valueOf(numericText.charAt(index)), size);
            }

            cursor = matchIndex + numericText.length();
         }
      } else {
         drawTextRun(fontName, text, x, y, size, color);
      }
   }

   public static void drawImageColored(String texture, float x, float y, float size, float radius, int... colors) {
      int[] resolvedColors = new int[colors.length];

      for (int index = 0; index < colors.length; index++) {
         resolvedColors[index] = applyAlpha(colors[index]);
      }

      getImageRenderer().processData2(new ImageDrawData(texture, x, y, size, radius).withColors(resolvedColors));
   }

   private static GlowCompositeRenderer getGlowCompositeRenderer() {
      return GlowCompositeRenderer.getGlowCompositeRenderer();
   }

   public static void setBlurMode(int blurMode) {
      BatchedBlurRenderer.processData11(blurMode);
   }

   public static void popAlpha() {
      globalAlpha = alphaStack.isEmpty() ? 1.0F : alphaStack.pop();
   }

   private static TextAtlasRenderer getTextAtlasRenderer() {
      return TextAtlasRenderer.getTextAtlasRenderer();
   }

   public static void reloadTextures() {
      ImageRenderer.getImageRenderer().updateState5();
      MsdfTextRenderer.getMsdfTextRenderer().updateState6();
      EffectIconRenderer.getEffectIconRenderer().updateState();
      FontRegistry.clearCache();
      GreedClient.logger().info("[Render2D] textures reloaded");
   }

   private static CornerBracketRenderer getCornerBracketRenderer() {
      return CornerBracketRenderer.getCornerBracketRenderer();
   }

   public static void drawGradientTextRotated(
      String fontName,
      String text,
      float x,
      float y,
      float size,
      int colorTopLeft,
      int colorTopRight,
      int colorBottomRight,
      int colorBottomLeft,
      float rotationDegrees,
      float rotationOriginX,
      float rotationOriginY
   ) {
      refreshRendererState();
      text = NameProtectModule.getInstance().formatText(text);
      String resolvedFontName = normalizeFontName(fontName);
      if (resolvedFontName != null) {
         getMsdfTextRenderer()
            .processData(
               new MsdfTextDrawData(
                  resolvedFontName,
                  text,
                  x,
                  y,
                  size,
                  applyAlpha(colorTopLeft),
                  applyAlpha(colorTopRight),
                  applyAlpha(colorBottomRight),
                  applyAlpha(colorBottomLeft),
                  rotationDegrees,
                  rotationOriginX,
                  rotationOriginY
               )
            );
      } else {
         getTextAtlasRenderer()
            .processData(
               new RasterTextDrawData(
                  fontName,
                  text,
                  x,
                  y,
                  size,
                  colorTopLeft,
                  colorTopRight,
                  colorBottomRight,
                  colorBottomLeft,
                  rotationDegrees,
                  rotationOriginX,
                  rotationOriginY,
                  false,
                  false,
                  0.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  true
               )
            );
      }
   }

   public static float[] measureMsdfText(String fontName, String text, float size) {
      return getMsdfTextRenderer().computeFloat2(fontName, text, size);
   }

   private static OutlineDefaultRenderer getOutlineDefaultRenderer() {
      return OutlineDefaultRenderer.getOutlineDefaultRenderer();
   }

   public static void drawRipple(
      float x,
      float y,
      float width,
      float height,
      float radius,
      float centerX,
      float centerY,
      float rippleRadius,
      int sourceColor,
      int targetColor
   ) {
      drawRippleSmooth(x, y, width, height, radius, 0.5F, centerX, centerY, rippleRadius, 25.0F, sourceColor, targetColor);
   }

   private Render2D() {
   }

   public static void drawImageRotated(
      String texture,
      float x,
      float y,
      float size,
      float radius,
      float rotationDegrees,
      float rotationOriginX,
      float rotationOriginY,
      int color
   ) {
      getImageRenderer()
         .processData2(
            new ImageDrawData(texture, x, y, size, radius, applyAlpha(color))
               .withRotation(rotationDegrees, rotationOriginX, rotationOriginY)
         );
   }

   public static void drawDashedLine(
      float x1,
      float y1,
      float x2,
      float y2,
      float thickness,
      int color,
      float dashLength,
      float gapLength,
      int maxSegments
   ) {
      float dx = x2 - x1;
      float dy = y2 - y1;
      float length = (float)Math.sqrt(dx * dx + dy * dy);
      if (!(length < 0.01F) && color >>> 24 != 0 && !(thickness <= 0.0F)) {
         float dirX = dx / length;
         float dirY = dy / length;
         float period = Math.max(0.5F, dashLength + gapLength);
         int segmentCount = (int)Math.ceil(length / period);
         if (maxSegments > 0 && segmentCount > maxSegments) {
            segmentCount = maxSegments;
         }

         for (int index = 0; index < segmentCount; index++) {
            float distance = index * period;
            float endDistance = Math.min(distance + dashLength, length);
            if (!(endDistance <= distance)) {
               drawLine(
                  x1 + dirX * distance,
                  y1 + dirY * distance,
                  x1 + dirX * endDistance,
                  y1 + dirY * endDistance,
                  thickness,
                  color
               );
            }
         }
      }
   }

   public static void drawTextWithTint(
      String fontName,
      String text,
      float x,
      float y,
      float size,
      int color,
      float red,
      float green,
      float blue,
      float alpha
   ) {
      refreshRendererState();
      getMsdfTextRenderer()
         .processData4(
            new MsdfTextDrawData(fontName, text, x, y, size, color),
            red,
            green,
            blue,
            alpha
         );
   }

   public static void endScissor(DrawContext drawContext) {
      ScissorStack.updateState2();
      if (drawContext != null) {
         drawContext.disableScissor();
      }
   }

   public static void drawSoftGlow(
      float x, float y, float width, float height, float radius, int color, float softness
   ) {
      drawSoftGlowData(new SoftGlowDrawData(x, y, width, height, radius, color, softness));
   }

   private static DitherRenderer getDitherRenderer() {
      return DitherRenderer.getDitherRenderer();
   }

   public static void drawCircleOutline(float x, float y, float radius, float thickness, int color) {
      refreshRendererState();
      getCircleRenderer().processData2(new CircleDrawData(x, y, radius, applyAlpha(color)).withThickness(thickness));
   }

   public static void drawBlurColor(float x, float y, float width, float height, float radius, int color) {
      refreshRendererState();
      getBatchedBlurRenderer()
         .processData(new BlurDrawData(x, y, width, height, radius, 1.0F, 36.0F).withColor(applyAlpha(color)));
   }

   public static void drawGlassArray(
      float x,
      float y,
      float width,
      float height,
      float[] cornerRadii,
      int color,
      float globalAlpha,
      float fresnelPower,
      int fresnelColor,
      float baseAlpha,
      boolean fresnelInvert,
      float fresnelMix,
      float distortStrength,
      float squirt,
      float z
   ) {
      refreshRendererState();
      getGlassRenderer()
         .processData2(
            new GlassDrawData(
               x,
               y,
               width,
               height,
               cornerRadii,
               color,
               globalAlpha,
               fresnelPower,
               fresnelColor,
               baseAlpha,
               fresnelInvert,
               fresnelMix,
               distortStrength,
               squirt,
               z
            )
         );
   }

   public static void drawHalfIconAdvanced(
      float x,
      float y,
      float width,
      float height,
      float radiusTopLeft,
      float radiusTopRight,
      float radiusBottomRight,
      float radiusBottomLeft,
      int color,
      int iconColor,
      String fontName,
      String icon,
      float iconSize,
      float spacingX,
      float spacingY,
      float paddingX,
      float paddingY,
      float jitterX,
      float jitterY,
      float rotationMaxDegrees,
      long seed
   ) {
      drawHalfIcon(
         new HalfIconDrawData(
            x,
            y,
            width,
            height,
            radiusTopLeft,
            radiusTopRight,
            radiusBottomRight,
            radiusBottomLeft,
            color,
            color,
            color,
            color,
            0.0F,
            fontName,
            icon,
            iconSize,
            iconColor,
            spacingX,
            spacingY,
            paddingX,
            paddingY,
            jitterX,
            jitterY,
            rotationMaxDegrees,
            seed
         )
      );
   }

   public static void drawGlowComposite(GlowCompositeDrawData glowCompositeDrawData) {
      if (glowCompositeDrawData != null) {
         getGlowCompositeRenderer().processData13(glowCompositeDrawData);
      }
   }

   public static void drawGradientText(
      String fontName,
      String text,
      float x,
      float y,
      float size,
      int colorTopLeft,
      int colorTopRight,
      int colorBottomRight,
      int colorBottomLeft
   ) {
      refreshRendererState();
      text = NameProtectModule.getInstance().formatText(text);
      String resolvedFontName = normalizeFontName(fontName);
      if (resolvedFontName != null) {
         getMsdfTextRenderer()
            .processData(
               new MsdfTextDrawData(
                  resolvedFontName,
                  text,
                  x,
                  y,
                  size,
                  applyAlpha(colorTopLeft),
                  applyAlpha(colorTopRight),
                  applyAlpha(colorBottomRight),
                  applyAlpha(colorBottomLeft)
               )
            );
      } else {
         getTextAtlasRenderer()
            .processData(
               new RasterTextDrawData(fontName, text, x, y, size, colorTopLeft, colorTopRight, colorBottomRight, colorBottomLeft)
            );
      }
   }

   private static SoftGlowRenderer getSoftGlowRenderer() {
      return SoftGlowRenderer.getSoftGlowRenderer();
   }

   public static void drawOutlineCorners(
      float x,
      float y,
      float width,
      float height,
      float radiusTopLeft,
      float radiusTopRight,
      float radiusBottomRight,
      float radiusBottomLeft,
      float thickness,
      int color
   ) {
      refreshRendererState();
      getOutlineDefaultRenderer()
         .processData3(
            new OutlineDrawData(
               x, y, width, height, radiusTopLeft, radiusTopRight, radiusBottomRight, radiusBottomLeft, thickness, applyAlpha(color)
            )
         );
   }

   public static void drawShape(ShapeDrawData shapeDrawData) {
      refreshRendererState();
      getShapeRenderer().processData3(shapeDrawData);
   }

   public static void drawImageRotatedGlow(
      String texture,
      float x,
      float y,
      float size,
      float radius,
      float rotationDegrees,
      float rotationOriginX,
      float rotationOriginY,
      int color
   ) {
      getImageRenderer()
         .processData2(
            new ImageDrawData(texture, x, y, size, radius, applyAlpha(color))
               .withRotation(rotationDegrees, rotationOriginX, rotationOriginY)
               .withGlow()
         );
   }

   public static void drawTintedText(
      String fontName,
      String text,
      float x,
      float y,
      float size,
      float red,
      float green,
      float blue,
      float alpha
   ) {
      refreshRendererState();
      getMsdfTextRenderer()
         .processData4(
            new MsdfTextDrawData(fontName, text, x, y, size, -1), red, green, blue, alpha
         );
   }

   public static void drawGlassOutlineData(GlassOutlineDrawData glassOutlineDrawData) {
      refreshRendererState();
      getOutlineGlassRenderer().processData3(glassOutlineDrawData);
   }

   public static void drawBlur(float x, float y, float width, float height, float radius, float blurRadius) {
      drawBlurColored(x, y, width, height, radius, blurRadius, 1.0F, -1);
   }

   public static void drawImageUvNearest(
      String texture,
      float x,
      float y,
      float explicitWidth,
      float explicitHeight,
      float radius,
      float smoothness,
      float u0,
      float v0,
      float u1,
      float v1,
      int color
   ) {
      if (!(explicitWidth <= 0.0F) && !(explicitHeight <= 0.0F)) {
         getImageRenderer()
            .processData2(
               new ImageDrawData(texture, x, y, explicitWidth, explicitHeight, radius, applyAlpha(color))
                  .withSmoothness(smoothness)
                  .withUv(u0, v0, u1, v1)
                  .withNearest()
            );
      }
   }

   public static void beginScissor(DrawContext drawContext, float x, float y, float width, float height) {
      if (drawContext == null) {
         ScissorStack.processData2(x, y, width, height);
      } else {
         ScissorStack.processData(GuiScale.renderElement(drawContext), x, y, width, height);
         drawContext.enableScissor(
            GuiScale.computeInt2(x),
            GuiScale.computeInt2(y),
            GuiScale.computeInt2(x + width),
            GuiScale.computeInt2(y + height)
         );
      }
   }

   public static void drawLine(float x1, float y1, float x2, float y2, float thickness, int color) {
      getLineRenderer().processData2(new LineDrawData(x1, y1, x2, y2, thickness, color));
   }

   public static void drawGlassOutlineDistorted(
      float x,
      float y,
      float width,
      float height,
      float size,
      float distortStrength,
      float radiusScale,
      float thickness,
      int color
   ) {
      drawGlassOutlineArray(
         x,
         y,
         width,
         height,
         new float[]{radiusScale * size / 2.0F, radiusScale * size / 2.0F, radiusScale * size / 2.0F, radiusScale * size / 2.0F},
         thickness,
         color,
         (color >>> 24 & 0xFF) / 255.0F,
         height == 240.0F ? 100.0F : 50.0F,
         color | 0xFF000000,
         1.0F,
         true,
         0.0F,
         distortStrength,
         size,
         0.0F
      );
   }

   public static void drawStatusEffectIcon(RegistryEntry<StatusEffect> effect, float x, float y, float size, int color) {
      getEffectIconRenderer().processData(new EffectIconDraw(effect, x, y, size, applyAlpha(color)));
   }

   public static void drawCircleSmooth(float x, float y, float radius, float smoothness, int color) {
      refreshRendererState();
      getCircleRenderer().processData2(new CircleDrawData(x, y, radius, applyAlpha(color)).withSmoothness(smoothness));
   }

   public static void drawStatusEffectIconInstance(StatusEffectInstance statusEffectInstance, float x, float y, float size, int color) {
      if (statusEffectInstance != null && statusEffectInstance.shouldShowIcon()) {
         getEffectIconRenderer().processData(new EffectIconDraw(statusEffectInstance, x, y, size, applyAlpha(color)));
      }
   }

   private static OutlineGlassRenderer getOutlineGlassRenderer() {
      return OutlineGlassRenderer.getOutlineGlassRenderer();
   }

   public static void drawRippleSmooth(
      float x,
      float y,
      float width,
      float height,
      float radius,
      float smoothness,
      float centerX,
      float centerY,
      float rippleRadius,
      float rippleSmoothness,
      int sourceColor,
      int targetColor
   ) {
      getRippleRenderer()
         .processData2(
            new RippleDrawData(
               x,
               y,
               width,
               height,
               radius,
               radius,
               radius,
               radius,
               smoothness,
               centerX,
               centerY,
               rippleRadius,
               rippleSmoothness,
               sourceColor,
               targetColor,
               false
            )
         );
   }

   public static void drawTextImmediate(
      DrawContext drawContext, String fontName, String text, float x, float y, float size, int color
   ) {
      flush(drawContext);
      drawText(fontName, text, x, y, size, color);
      endFrame();
   }

   public static void drawGlassCorners(
      float x,
      float y,
      float width,
      float height,
      float radiusTopLeft,
      float radiusTopRight,
      float radiusBottomRight,
      float radiusBottomLeft,
      int color,
      float globalAlpha,
      float fresnelPower,
      int fresnelColor,
      float baseAlpha,
      boolean fresnelInvert,
      float fresnelMix,
      float distortStrength,
      float squirt,
      float z
   ) {
      refreshRendererState();
      getGlassRenderer()
         .processData2(
            new GlassDrawData(
               x,
               y,
               width,
               height,
               radiusTopLeft,
               radiusTopRight,
               radiusBottomRight,
               radiusBottomLeft,
               color,
               globalAlpha,
               fresnelPower,
               fresnelColor,
               baseAlpha,
               fresnelInvert,
               fresnelMix,
               distortStrength,
               squirt,
               z
            )
         );
   }

   public static void drawHalfIcon(HalfIconDrawData halfIconDrawData) {
      refreshRendererState();
      if (halfIconDrawData != null && halfIconDrawData.backgroundVisible()) {
         drawGradientRectCorners(
            halfIconDrawData.x(),
            halfIconDrawData.y(),
            halfIconDrawData.width(),
            halfIconDrawData.height(),
            halfIconDrawData.radiusTopLeft(),
            halfIconDrawData.radiusTopRight(),
            halfIconDrawData.radiusBottomRight(),
            halfIconDrawData.radiusBottomLeft(),
            halfIconDrawData.colorTopLeft(),
            halfIconDrawData.colorTopRight(),
            halfIconDrawData.colorBottomRight(),
            halfIconDrawData.colorBottomLeft()
         );
      }

      if (halfIconDrawData != null && halfIconDrawData.iconsVisible()) {
         getRectHalfIconRenderer().processData2(halfIconDrawData);
      }
   }

   public static void drawSoftGlowRing(
      float x, float y, float width, float height, float radius, int color, float softness, float intensity
   ) {
      drawSoftGlowData(
         new SoftGlowDrawData(x, y, width, height, radius, color, softness).withIntensity(intensity).asRing()
      );
   }

   public static void drawOutlineData(OutlineDrawData outlineDrawData) {
      refreshRendererState();
      getOutlineDefaultRenderer().processData3(outlineDrawData);
   }

   public static void drawGradientRect(
      float x,
      float y,
      float width,
      float height,
      float radius,
      int colorTopLeft,
      int colorTopRight,
      int colorBottomRight,
      int colorBottomLeft
   ) {
      drawGradientRectCorners(
         x,
         y,
         width,
         height,
         radius,
         radius,
         radius,
         radius,
         colorTopLeft,
         colorTopRight,
         colorBottomRight,
         colorBottomLeft
      );
   }

   public static void bumpGuiLayer(DrawContext drawContext) {
      if (drawContext != null) {
         ((GuiLayerSerialAccess)((DrawContextMixin)drawContext).getGuiRenderState()).bumpLayerSerial();
      }
   }

   private static ShapeRenderer getShapeRenderer() {
      return ShapeRenderer.getShapeRenderer();
   }

   public static void drawTextFaded(
      String fontName,
      String text,
      float x,
      float y,
      float size,
      int color,
      float fadeLeftX,
      float fadeRightX,
      float fadeWidth,
      float fadeLeftStrength,
      float fadeRightStrength
   ) {
      refreshRendererState();
      getMsdfTextRenderer()
         .processData(
            new MsdfTextDrawData(fontName, text, x, y, size, applyAlpha(color))
               .withHorizontalFade(fadeLeftX, fadeRightX, fadeWidth, fadeLeftStrength, fadeRightStrength)
         );
   }

   public static void drawImageUv(
      String texture,
      float x,
      float y,
      float explicitWidth,
      float explicitHeight,
      float radius,
      float u0,
      float v0,
      float u1,
      float v1,
      int color
   ) {
      if (!(explicitWidth <= 0.0F) && !(explicitHeight <= 0.0F)) {
         getImageRenderer()
            .processData2(
               new ImageDrawData(texture, x, y, explicitWidth, explicitHeight, radius, applyAlpha(color))
                  .withUv(u0, v0, u1, v1)
            );
      }
   }

   public static void drawGradientRectCorners(
      float x,
      float y,
      float width,
      float height,
      float radiusTopLeft,
      float radiusTopRight,
      float radiusBottomRight,
      float radiusBottomLeft,
      int colorTopLeft,
      int colorTopRight,
      int colorBottomRight,
      int colorBottomLeft
   ) {
      refreshRendererState();
      getRectDefaultRenderer()
         .processData2(
            new RectDrawData(
               x,
               y,
               width,
               height,
               radiusTopLeft,
               radiusTopRight,
               radiusBottomRight,
               radiusBottomLeft,
               applyAlpha(colorTopLeft),
               applyAlpha(colorTopRight),
               applyAlpha(colorBottomRight),
               applyAlpha(colorBottomLeft),
               0.5F
            )
         );
   }

   private static GlassRenderer getGlassRenderer() {
      return GlassRenderer.getGlassRenderer();
   }

   public static void drawGlassOutlineArray(
      float x,
      float y,
      float width,
      float height,
      float[] cornerRadii,
      float thickness,
      int color,
      float globalAlpha,
      float fresnelPower,
      int fresnelColor,
      float baseAlpha,
      boolean fresnelInvert,
      float fresnelMix,
      float distortStrength,
      float squirt,
      float z
   ) {
      refreshRendererState();
      getOutlineGlassRenderer()
         .processData3(
            new GlassOutlineDrawData(
               x,
               y,
               width,
               height,
               cornerRadii,
               thickness,
               color,
               globalAlpha,
               fresnelPower,
               fresnelColor,
               baseAlpha,
               fresnelInvert,
               fresnelMix,
               distortStrength,
               squirt,
               z
            )
         );
   }

   public static void drawOutline360Data(Outline360DrawData outline360DrawData) {
      refreshRendererState();
      getOutline360Renderer().processData4(outline360DrawData);
   }

   public static ArcColorSpan getArcColorSpan(float startDegrees, float endDegrees, int color) {
      return ArcColorSpan.of(startDegrees, endDegrees, color);
   }

   public static void drawWarpText(
      String fontName,
      String text,
      float x,
      float y,
      float size,
      float padding,
      float mouseX,
      float mouseY,
      float influenceRadiusPx,
      float cellPx,
      int color
   ) {
      FontAtlas fontAtlas = FontRegistry.getAtlas(fontName);
      if (fontAtlas != null) {
         float[] glyphQuads = fontAtlas.computeFloat6(text, size);

         for (int index = 0; index < glyphQuads.length; index += 8) {
            glyphQuads[index] += padding;
            glyphQuads[index + 1] = glyphQuads[index + 1] + padding;
            glyphQuads[index + 2] = glyphQuads[index + 2] + padding;
            glyphQuads[index + 3] = glyphQuads[index + 3] + padding;
         }

         getWarpTextRenderer()
            .processData2(
               new WarpTextDrawData(
                  x - padding,
                  y - padding,
                  fontAtlas.computeFloat7(text, size) + padding * 2.0F,
                  fontAtlas.computeFloat3(size) + padding * 2.0F,
                  glyphQuads,
                  mouseX - x + padding,
                  mouseY - y + padding,
                  influenceRadiusPx,
                  cellPx,
                  fontAtlas.computeFloat8(),
                  fontAtlas.getIdentifier(),
                  applyAlpha(color)
               )
            );
      }
   }

   public static void drawBackdropBlur(float x, float y, float width, float height, float radius) {
      refreshRendererState();
      getBatchedBlurRenderer().processData(new BlurDrawData(x, y, width, height, radius, 1.0F, 36.0F).withColor(applyAlpha(-1)));
   }

   private static ArcDividerRenderer getArcDividerRenderer() {
      return ArcDividerRenderer.getArcDividerRenderer();
   }

   public static void drawMsdfTextRotated(
      String fontName,
      String text,
      float x,
      float y,
      float size,
      int color,
      float rotationDegrees,
      float rotationOriginX,
      float rotationOriginY
   ) {
      refreshRendererState();
      getMsdfTextRenderer()
         .processData(
            new MsdfTextDrawData(
               fontName, text, x, y, size, applyAlpha(color), rotationDegrees, rotationOriginX, rotationOriginY
            )
         );
   }

   public static void pushAlpha(float alphaScale) {
      alphaStack.push(globalAlpha);
      globalAlpha = globalAlpha * Math.max(0.0F, Math.min(1.0F, alphaScale));
   }

   private static void ensureMsdfReloaderRegistered() {
      if (!msdfReloaderRegistered) {
         Identifier identifier = Identifier.of("greed", "render2d_msdf_cache");
         ResourceLoader.get(ResourceType.CLIENT_RESOURCES)
            .registerReloader(identifier, (SynchronousResourceReloader)resourceManager -> MsdfTextRenderer.updateState4());
         msdfReloaderRegistered = true;
      }
   }

   public static void drawOutlineGradient(
      float x,
      float y,
      float width,
      float height,
      float radius,
      float thickness,
      int colorTopLeft,
      int colorTopRight,
      int colorBottomRight,
      int colorBottomLeft
   ) {
      drawOutlineGradientCorners(
         x,
         y,
         width,
         height,
         radius,
         radius,
         radius,
         radius,
         thickness,
         colorTopLeft,
         colorTopRight,
         colorBottomRight,
         colorBottomLeft
      );
   }

   private static BatchedBlurRenderer getBatchedBlurRenderer() {
      return BatchedBlurRenderer.getBatchedBlurRenderer();
   }

   public static void drawShimmer(
      float x, float y, float width, float height, float progress, float halfWidth, float intensity, int color
   ) {
      getShimmerRenderer().processData(new ArcDividerDrawData(x, y, width, height, progress, halfWidth, intensity, color));
   }

   public static void drawDashedLineFaded(
      float x1,
      float y1,
      float x2,
      float y2,
      float thickness,
      int color,
      float dashLength,
      float gapLength,
      int maxSegments,
      float alphaStart,
      float alphaEnd
   ) {
      float dx = x2 - x1;
      float dy = y2 - y1;
      float length = (float)Math.sqrt(dx * dx + dy * dy);
      if (!(length < 0.01F) && color >>> 24 != 0 && !(thickness <= 0.0F)) {
         float dirX = dx / length;
         float dirY = dy / length;
         float period = Math.max(0.5F, dashLength + gapLength);
         int segmentCount = (int)Math.ceil(length / period);
         if (maxSegments > 0 && segmentCount > maxSegments) {
            segmentCount = maxSegments;
         }

         for (int index = 0; index < segmentCount; index++) {
            float distance = index * period;
            float endDistance = Math.min(distance + dashLength, length);
            if (!(endDistance <= distance)) {
               float fadeStart = easeAlphaAt(distance / length, alphaStart, alphaEnd);
               float fadeEnd = easeAlphaAt(endDistance / length, alphaStart, alphaEnd);
               drawLineFaded(
                  x1 + dirX * distance,
                  y1 + dirY * distance,
                  x1 + dirX * endDistance,
                  y1 + dirY * endDistance,
                  thickness,
                  color,
                  fadeStart,
                  fadeEnd
               );
            }
         }
      }
   }

   public static void drawBlurGradient(
      float x,
      float y,
      float width,
      float height,
      float radius,
      float blurRadius,
      float smoothness,
      int colorTopLeft,
      int colorTopRight,
      int colorBottomRight,
      int colorBottomLeft
   ) {
      refreshRendererState();
      getBatchedBlurRenderer()
         .processData14(
            new BlurDrawData(x, y, width, height, radius, smoothness, blurRadius)
               .withColors(colorTopLeft, colorTopRight, colorBottomRight, colorBottomLeft)
         );
   }

   public static void flush(DrawContext drawContext) {
      if (!renderPassActive) {
         renderPassActive = true;
         globalAlpha = 1.0F;
         alphaStack.clear();
         ScissorStack.updateState();
         RoundedClipStack.updateState();
         getBatchedBlurRenderer().renderElement3(drawContext);
         getGlassRenderer().renderElement(drawContext);
         getShapeRenderer().renderElement3(drawContext);
         getOutlineGlassRenderer().renderElement3(drawContext);
         getCircleRenderer().renderElement2(drawContext);
         getCornerBracketRenderer().renderElement2(drawContext);
         getBlobRenderer().renderElement(drawContext);
         getArcDividerRenderer().renderElement2(drawContext);
         getPickerRenderer().renderElement2(drawContext);
         getRectDefaultRenderer().renderElement(drawContext);
         getRectHalfIconRenderer().renderElement3(drawContext);
         getOutlineDefaultRenderer().renderElement3(drawContext);
         getOutline360Renderer().renderElement(drawContext);
         getTextAtlasRenderer().renderElement3(drawContext);
         getMsdfTextRenderer().renderElement(drawContext);
         getShimmerRenderer().renderElement(drawContext);
         getDitherRenderer().renderElement(drawContext);
         getMenuGridRenderer().renderElement(drawContext);
         getWarpTextRenderer().renderElement(drawContext);
         getImageRenderer().renderElement2(drawContext);
         getLineRenderer().renderElement(drawContext);
         getEffectIconRenderer().renderElement(drawContext);
         getRippleRenderer().renderElement2(drawContext);
         getGlowCompositeRenderer().renderElement(drawContext);
         getSoftGlowRenderer().renderElement2(drawContext);
         ItemRenderCache.renderElement(drawContext);
      }
   }

   public static void shutdown() {
      BatchedBlurRenderer.updateState8();
      GlassRenderer.updateState4();
      ShapeRenderer.updateState3();
      OutlineGlassRenderer.updateState5();
      CircleRenderer.updateState2();
      CornerBracketRenderer.updateState2();
      BlobRenderer.updateState5();
      ArcDividerRenderer.updateState5();
      PickerRenderer.updateState3();
      RectDefaultRenderer.updateState2();
      RectHalfIconRenderer.updateState3();
      OutlineDefaultRenderer.updateState3();
      Outline360Renderer.updateState();
      TextAtlasRenderer.updateState2();
      MsdfTextRenderer.updateState2();
      ShimmerRenderer.updateState4();
      DitherRenderer.updateState();
      MenuGridRenderer.updateState2();
      WarpTextRenderer.updateState3();
      ImageRenderer.updateState();
      LineRenderer.updateState4();
      EffectIconRenderer.updateState4();
      RippleRenderer.updateState4();
      GlowCompositeRenderer.updateState4();
      SoftGlowRenderer.updateState();
      ThemePaletteBuffer.updateState2();
      GreedClient.logger().info("[Render2D] Render2D closed");
   }

   public static void drawRect(float x, float y, float width, float height, float radius, int color) {
      refreshRendererState();
      getRectDefaultRenderer().processData2(new RectDrawData(x, y, width, height, radius, applyAlpha(color)));
   }

   public static void drawOutlineArcVarargs(
      float x,
      float y,
      float width,
      float height,
      float radiusTopLeft,
      float radiusTopRight,
      float radiusBottomRight,
      float radiusBottomLeft,
      float thickness,
      int color,
      ArcColorSpan... ranges
   ) {
      refreshRendererState();
      drawOutlineArc(
         x,
         y,
         width,
         height,
         radiusTopLeft,
         radiusTopRight,
         radiusBottomRight,
         radiusBottomLeft,
         thickness,
         color,
         Arrays.asList(ranges)
      );
   }

   public static void drawImageCorners(
      String texture,
      float x,
      float y,
      float size,
      float radiusTopLeft,
      float radiusTopRight,
      float radiusBottomRight,
      float radiusBottomLeft,
      int color
   ) {
      int resolvedColor = applyAlpha(color);
      getImageRenderer()
         .processData2(
            new ImageDrawData(
               texture,
               x,
               y,
               size,
               radiusTopLeft,
               radiusTopRight,
               radiusBottomRight,
               radiusBottomLeft,
               resolvedColor,
               resolvedColor,
               resolvedColor,
               resolvedColor
            )
         );
   }

   public static void drawObfuscatedText(String fontName, String text, float x, float y, float size, int color) {
      String obfuscationChars = buildObfuscationChars(fontName);
      getMsdfTextRenderer().processData5(true);
      float currentX = x;
      int cursor = 0;

      while (cursor < text.length()) {
         int codePoint = text.codePointAt(cursor);
         cursor += Character.charCount(codePoint);
         if (!Character.isWhitespace(codePoint)) {
            String randomChar = String.valueOf(
               obfuscationChars.charAt((int)(((System.currentTimeMillis() / 10L + cursor * 1337L) * -7046029254386353131L >>> 10) % obfuscationChars.length()))
            );
            drawText(
               fontName,
               randomChar,
               currentX
                  + (measureText(fontName, new String(Character.toChars(codePoint)), size) - measureText(fontName, randomChar, size))
                     / 2.0F,
               y,
               size,
               color
            );
         }

         currentX += measureText(fontName, new String(Character.toChars(codePoint)), size);
      }

      getMsdfTextRenderer().processData5(false);
   }

   public static void drawCircle(float x, float y, float radius, int color) {
      refreshRendererState();
      getCircleRenderer().processData2(new CircleDrawData(x, y, radius, applyAlpha(color)));
   }

   public static void drawHueSlider(float x, float y, float width, float height, float alpha) {
      refreshRendererState();
      getPickerRenderer().processData3(PickerDrawData.hue(x, y, width, height, alpha * globalAlpha));
   }

   public static void drawHalfIconSimple(
      float x,
      float y,
      float width,
      float height,
      float radius,
      int color,
      int iconColor,
      String fontName,
      String icon,
      float iconSize,
      float spacingX,
      float spacingY,
      float rotationMaxDegrees
   ) {
      drawHalfIcon(
         new HalfIconDrawData(
            x,
            y,
            width,
            height,
            radius,
            color,
            fontName,
            icon,
            iconSize,
            iconColor,
            spacingX,
            spacingY,
            rotationMaxDegrees
         )
      );
   }

   public static void drawSoftGlowData(SoftGlowDrawData softGlowDrawData) {
      if (softGlowDrawData != null) {
         refreshRendererState();
         getSoftGlowRenderer().processData2(softGlowDrawData);
      }
   }

   public static void drawOutlineArcUniform(
      float x,
      float y,
      float width,
      float height,
      float radius,
      float thickness,
      int color,
      List<ArcColorSpan> ranges
   ) {
      refreshRendererState();
      getOutline360Renderer()
         .processData4(
            new Outline360DrawData(
               x, y, width, height, radius, thickness, applyAlpha(color), applyAlphaToSpans(ranges)
            )
         );
   }

   public static int applyAlpha(int color) {
      if (globalAlpha >= 0.999F) {
         return color;
      }

      int alpha = Math.round((color >>> 24 & 0xFF) * globalAlpha);
      return alpha << 24 | color & 16777215;
   }

   public static void drawBlurSmooth(
      float x, float y, float width, float height, float radius, float blurRadius, float smoothness, int color
   ) {
      refreshRendererState();
      getBatchedBlurRenderer()
         .processData10(
            new BlurDrawData(x, y, width, height, radius, smoothness, blurRadius)
               .withColor(applyAlpha(color))
         );
   }

   public static void drawGlassData(GlassDrawData glassDrawData) {
      refreshRendererState();
      getGlassRenderer().processData2(glassDrawData);
   }

   public static void drawRectSharp(float x, float y, float width, float height, int color) {
      drawRect(x, y, width, height, 0.0F, color);
   }

   public static void initialize() {
      ensureMsdfReloaderRegistered();
      GreedClient.logger().info("[Render2D] Render2D initialized");
   }

   public static void drawImage(String texture, float x, float y, float size, float radius) {
      getImageRenderer().processData2(new ImageDrawData(texture, x, y, size, radius));
   }

   public static void drawRectImmediate(
      DrawContext drawContext, float x, float y, float width, float height, float radius, int color
   ) {
      flush(drawContext);
      drawRect(x, y, width, height, radius, color);
      endFrame();
   }

   public static void drawBlurColored(
      float x, float y, float width, float height, float radius, float blurRadius, float smoothness, int color
   ) {
      refreshRendererState();
      getBatchedBlurRenderer()
         .processData14(new BlurDrawData(x, y, width, height, radius, smoothness, blurRadius).withColor(color));
   }

   public static void drawRasterText(RasterTextDrawData rasterTextDrawData) {
      refreshRendererState();
      if (rasterTextDrawData != null && !rasterTextDrawData.hasHorizontalFade() && Math.abs(rasterTextDrawData.rotationDegrees()) < 0.001F) {
         String resolvedFontName = normalizeFontName(rasterTextDrawData.fontName());
         if (resolvedFontName != null) {
            getMsdfTextRenderer()
               .processData(
                  new MsdfTextDrawData(
                     resolvedFontName,
                     rasterTextDrawData.text(),
                     rasterTextDrawData.x(),
                     rasterTextDrawData.y(),
                     rasterTextDrawData.size(),
                     applyAlpha(rasterTextDrawData.colorTopLeft()),
                     applyAlpha(rasterTextDrawData.colorTopRight()),
                     applyAlpha(rasterTextDrawData.colorBottomRight()),
                     applyAlpha(rasterTextDrawData.colorBottomLeft())
                  )
               );
            return;
         }
      }

      getTextAtlasRenderer().processData(rasterTextDrawData);
   }

   public static void drawMsdfTextDrawData(MsdfTextDrawData msdfTextDrawData) {
      refreshRendererState();
      if (msdfTextDrawData != null) {
         getMsdfTextRenderer()
            .processData(
               new MsdfTextDrawData(
                     msdfTextDrawData.fontName(),
                     msdfTextDrawData.text(),
                     msdfTextDrawData.x(),
                     msdfTextDrawData.y(),
                     msdfTextDrawData.size(),
                     applyAlpha(msdfTextDrawData.colorTopLeft()),
                     applyAlpha(msdfTextDrawData.colorTopRight()),
                     applyAlpha(msdfTextDrawData.colorBottomRight()),
                     applyAlpha(msdfTextDrawData.colorBottomLeft()),
                     msdfTextDrawData.rotationDegrees(),
                     msdfTextDrawData.rotationOriginX(),
                     msdfTextDrawData.rotationOriginY()
                  )
                  .withHorizontalFade(
                     msdfTextDrawData.fadeLeftX(),
                     msdfTextDrawData.fadeRightX(),
                     msdfTextDrawData.fadeWidth(),
                     msdfTextDrawData.fadeLeftStrength(),
                     msdfTextDrawData.fadeRightStrength()
                  )
            );
      }
   }

   public static void drawBlurImmediate(
      DrawContext drawContext, float x, float y, float width, float height, float radius, float blurRadius
   ) {
      flush(drawContext);
      drawBlur(x, y, width, height, radius, blurRadius);
      endFrame();
   }

   private static List<ArcColorSpan> applyAlphaToSpans(List<ArcColorSpan> spans) {
      if (!(globalAlpha >= 0.999F) && spans != null && !spans.isEmpty()) {
         List<ArcColorSpan> dimmedSpans = new ArrayList<>(spans.size());

         for (ArcColorSpan arcColorSpan : spans) {
            dimmedSpans.add(new ArcColorSpan(arcColorSpan.startDegrees(), arcColorSpan.endDegrees(), applyAlpha(arcColorSpan.color())));
         }

         return dimmedSpans;
      } else {
         return spans;
      }
   }

   public static boolean hasTexture(String texture) {
      return getImageRenderer().matchesCriteria2(texture);
   }

   public static void drawTextFadedFormatted(
      String fontName,
      String text,
      float x,
      float y,
      float size,
      int color,
      float fadeLeftX,
      float fadeRightX,
      float fadeWidth,
      float fadeLeftStrength,
      float fadeRightStrength
   ) {
      refreshRendererState();
      text = NameProtectModule.getInstance().formatText(text);
      if (text != null && text.indexOf(167) >= 0) {
         StringBuilder accumulatedText = new StringBuilder();

         for (FormattedText.DataRecord dataRecord : FormattedText.getArrayList(text, color)) {
            String formattedText = dataRecord.text().indexOf(167) >= 0 ? FormattedText.formatText(dataRecord.text()) : dataRecord.text();
            if (!formattedText.isEmpty()) {
               if (dataRecord.obfuscated()) {
                  drawObfuscatedText(
                     fontName,
                     formattedText,
                     x + measureText(fontName, accumulatedText.toString(), size),
                     y,
                     size,
                     dataRecord.color() & 16777215 | color & 0xFF000000
                  );
               } else {
                  drawTextFadedFormatted(
                     fontName,
                     formattedText,
                     x + measureText(fontName, accumulatedText.toString(), size),
                     y,
                     size,
                     dataRecord.color() & 16777215 | color & 0xFF000000,
                     fadeLeftX,
                     fadeRightX,
                     fadeWidth,
                     fadeLeftStrength,
                     fadeRightStrength
                  );
               }

               accumulatedText.append(formattedText);
            }
         }
      } else {
         String resolvedFontName = normalizeFontName(fontName);
         if (resolvedFontName != null) {
            getMsdfTextRenderer()
               .processData(
                  new MsdfTextDrawData(resolvedFontName, text, x, y, size, applyAlpha(color))
                     .withHorizontalFade(fadeLeftX, fadeRightX, fadeWidth, fadeLeftStrength, fadeRightStrength)
               );
         } else {
            getTextAtlasRenderer()
               .processData(
                  new RasterTextDrawData(fontName, text, x, y, size, color)
                     .withHorizontalFade(fadeLeftX, fadeRightX, fadeWidth, fadeLeftStrength, fadeRightStrength)
               );
         }
      }
   }

   public static float measureText(String fontName, String text, float size) {
      if (text != null && text.indexOf(167) >= 0) {
         text = FormattedText.formatText(text);
      }

      text = NameProtectModule.getInstance().formatText(text);
      String resolvedFontName = normalizeFontName(fontName);
      return resolvedFontName != null
         ? getMsdfTextRenderer().computeFloat(resolvedFontName, text, size)
         : getTextAtlasRenderer().computeFloat(fontName, text, size);
   }

   private static RectDefaultRenderer getRectDefaultRenderer() {
      return RectDefaultRenderer.getRectDefaultRenderer();
   }

   private static void drawTextRun(String fontName, String text, float x, float y, float size, int color) {
      String resolvedFontName = normalizeFontName(fontName);
      if (resolvedFontName != null) {
         getMsdfTextRenderer().processData(new MsdfTextDrawData(resolvedFontName, text, x, y, size, applyAlpha(color)));
      } else {
         getTextAtlasRenderer().processData(new RasterTextDrawData(fontName, text, x, y, size, color));
      }
   }

   public static void drawStatusEffectIconWhite(RegistryEntry<StatusEffect> effect, float x, float y, float size) {
      getEffectIconRenderer().processData(new EffectIconDraw(effect, x, y, size, applyAlpha(-1)));
   }

   public static void drawCornerBracket(
      float x, float y, float size, float radius, float stroke, int quadrant, int color
   ) {
      refreshRendererState();
      getCornerBracketRenderer().processData(new CornerBracketDrawData(x, y, size, radius, stroke, quadrant, applyAlpha(color)));
   }

   public static void drawStatusEffectIconInstanceWhite(StatusEffectInstance statusEffectInstance, float x, float y, float size) {
      if (statusEffectInstance != null && statusEffectInstance.shouldShowIcon()) {
         getEffectIconRenderer().processData(new EffectIconDraw(statusEffectInstance, x, y, size, applyAlpha(-1)));
      }
   }

   public static void drawRectData(RectDrawData rectDrawData) {
      getRectDefaultRenderer().processData2(rectDrawData);
   }

   private static CircleRenderer getCircleRenderer() {
      return CircleRenderer.getCircleRenderer();
   }

   public static void drawMsdfText(String fontName, String text, float x, float y, float size, int color) {
      refreshRendererState();
      getMsdfTextRenderer().processData(new MsdfTextDrawData(fontName, text, x, y, size, applyAlpha(color)));
   }

   private static ShimmerRenderer getShimmerRenderer() {
      return ShimmerRenderer.getShimmerRenderer();
   }

   public static void drawBlurCorners(
      float x, float y, float width, float height, float radiusTopLeft, float radiusTopRight, float radiusBottomRight, float radiusBottomLeft
   ) {
      refreshRendererState();
      getBatchedBlurRenderer()
         .processData(
            new BlurDrawData(
               x, y, width, height, radiusTopLeft, radiusTopRight, radiusBottomRight, radiusBottomLeft, 1.0F, 36.0F, applyAlpha(-1)
            )
         );
   }

   public static void drawOutlineArcUniformVarargs(
      float x,
      float y,
      float width,
      float height,
      float radius,
      float thickness,
      int color,
      ArcColorSpan... ranges
   ) {
      refreshRendererState();
      drawOutlineArcUniform(x, y, width, height, radius, thickness, color, Arrays.asList(ranges));
   }

   private static RippleRenderer getRippleRenderer() {
      return RippleRenderer.getRippleRenderer();
   }

   public static void drawTextFadedSides(
      String fontName,
      String text,
      float x,
      float y,
      float size,
      int color,
      float fadeLeftX,
      float fadeRightX,
      float fadeWidth,
      boolean fadeLeftEnabled,
      boolean fadeRightEnabled
   ) {
      drawTextFadedFormatted(
         fontName,
         text,
         x,
         y,
         size,
         color,
         fadeLeftX,
         fadeRightX,
         fadeWidth,
         fadeLeftEnabled ? 1.0F : 0.0F,
         fadeRightEnabled ? 1.0F : 0.0F
      );
   }

   private static PickerRenderer getPickerRenderer() {
      return PickerRenderer.getPickerRenderer();
   }

   public static void drawAlphaSlider(float x, float y, float width, float height, int solidColor, float alpha) {
      refreshRendererState();
      getPickerRenderer().processData3(PickerDrawData.alpha(x, y, width, height, solidColor, alpha * globalAlpha));
   }

   public static void endFrame() {
      if (renderPassActive) {
         renderPassActive = false;
         getBatchedBlurRenderer().updateState10();
         getGlassRenderer().updateState5();
         getShapeRenderer().updateState5();
         getOutlineGlassRenderer().updateState2();
         getCircleRenderer().updateState();
         getCornerBracketRenderer().updateState3();
         getBlobRenderer().updateState2();
         getArcDividerRenderer().updateState4();
         getPickerRenderer().updateState5();
         getRectDefaultRenderer().updateState();
         getRectHalfIconRenderer().updateState5();
         getOutlineDefaultRenderer().updateState5();
         getOutline360Renderer().updateState6();
         getTextAtlasRenderer().updateState();
         getMsdfTextRenderer().updateState();
         getShimmerRenderer().updateState2();
         getDitherRenderer().updateState5();
         getMenuGridRenderer().updateState();
         getWarpTextRenderer().updateState5();
         getImageRenderer().updateState7();
         getLineRenderer().updateState2();
         getEffectIconRenderer().updateState2();
         getRippleRenderer().updateState5();
         getGlowCompositeRenderer().updateState6();
         getSoftGlowRenderer().updateState4();
         ItemRenderCache.updateState4();
      }
   }

   public static void drawMenuGrid(
      float x,
      float y,
      float width,
      float height,
      float cellPx,
      float lineWidthPx,
      float mouseX,
      float mouseY,
      float influenceRadiusPx,
      int color
   ) {
      getMenuGridRenderer()
         .processData2(
            new MenuGridDrawData(
               x, y, width, height, cellPx, lineWidthPx, mouseX, mouseY, influenceRadiusPx, color
            )
         );
   }

   public static void drawBlurData(BlurDrawData blurDrawData) {
      refreshRendererState();
      getBatchedBlurRenderer().processData14(blurDrawData);
   }

   public static void drawBlob(
      float x,
      float y,
      float width,
      float height,
      float centerX,
      float centerY,
      float radiusX,
      float radiusY,
      float falloff,
      float clipTopLeft,
      float clipTopRight,
      float clipBottomRight,
      float clipBottomLeft,
      int color
   ) {
      refreshRendererState();
      getBlobRenderer()
         .processData2(
            new BlobDrawData(
               x,
               y,
               width,
               height,
               centerX,
               centerY,
               radiusX,
               radiusY,
               falloff,
               0.25F,
               clipTopLeft,
               clipTopRight,
               clipBottomRight,
               clipBottomLeft,
               applyAlpha(color)
            )
         );
   }

   public static void drawRippleRounded(
      float x,
      float y,
      float width,
      float height,
      float radius,
      float centerX,
      float centerY,
      float rippleRadius,
      int sourceColor,
      int targetColor
   ) {
      getRippleRenderer()
         .processData2(
            new RippleDrawData(
               x,
               y,
               width,
               height,
               radius,
               radius,
               radius,
               radius,
               0.5F,
               centerX,
               centerY,
               rippleRadius,
               25.0F,
               sourceColor,
               targetColor,
               true
            )
         );
   }

   private static String normalizeFontName(String input) {
      if (input != null && !input.isBlank()) {
         String normalized = input.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');

         return switch (normalized) {
            case "montserrat", "montserrat_regular" -> "montserrat-regular";
            case "montserrat_medium" -> "montserrat-medium";
            case "montserrat_semibold", "montserrat_semi_bold" -> "montserrat-semibold";
            case "montserrat_bold" -> "montserrat-bold";
            case "montserrat_extrabold", "montserrat_extra_bold" -> "montserrat-extrabold";
            case "montserrat_black" -> "montserrat-black";
            case "montserrat_light" -> "montserrat-light";
            case "montserrat_extralight", "montserrat_extra_light" -> "montserrat-extralight";
            case "montserrat_thin" -> "montserrat-thin";
            case "sf", "sf_regular" -> "sf-regular";
            case "sf_medium" -> "sf-medium";
            case "sf_bold" -> "sf-bold";
            case "kimiko" -> "kimiko";
            case "event_icons", "events_icons", "eventicons" -> "event-icons";
            case "icons", "icon" -> "icons";
            case "test", "semibold" -> "test";
            case "thin" -> "thin";
            case "mini", "light" -> "mini";
            case "semi", "medium" -> "semi";
            case "semi_bold", "semi-bold" -> "semi_bold";
            case "bold" -> "bold";
            default -> null;
         };
      } else {
         return null;
      }
   }

   public static void drawLineFaded(
      float x1, float y1, float x2, float y2, float thickness, int color, float fadeStart, float fadeEnd
   ) {
      getLineRenderer().processData2(new LineDrawData(x1, y1, x2, y2, thickness, color, fadeStart, fadeEnd));
   }

   public static float measureMsdfTextWidth(String fontName, String text, float size) {
      return getMsdfTextRenderer().computeFloat(fontName, text, size);
   }

   private static Outline360Renderer getOutline360Renderer() {
      return Outline360Renderer.getOutline360Renderer();
   }

   public static void drawDither(
      float x, float y, float width, float height, float radius, float cellSize, float spread, int color
   ) {
      getDitherRenderer().processData(new DitherDrawData(x, y, width, height, radius, cellSize, spread, color));
   }

   public static BlurDrawBuilder getBlurDrawBuilder() {
      return new BlurDrawBuilder();
   }

   private static EffectIconRenderer getEffectIconRenderer() {
      return EffectIconRenderer.getEffectIconRenderer();
   }

   public static void drawRectCorners(
      float x,
      float y,
      float width,
      float height,
      float radiusTopLeft,
      float radiusTopRight,
      float radiusBottomRight,
      float radiusBottomLeft,
      int color
   ) {
      refreshRendererState();
      getRectDefaultRenderer()
         .processData2(
            new RectDrawData(
               x, y, width, height, radiusTopLeft, radiusTopRight, radiusBottomRight, radiusBottomLeft, applyAlpha(color)
            )
         );
   }

   public static void drawGlass(
      float x,
      float y,
      float width,
      float height,
      float radius,
      int color,
      float globalAlpha,
      float fresnelPower,
      int fresnelColor,
      float baseAlpha,
      boolean fresnelInvert,
      float fresnelMix,
      float distortStrength,
      float squirt,
      float z
   ) {
      drawGlassCorners(
         x,
         y,
         width,
         height,
         radius,
         radius,
         radius,
         radius,
         color,
         globalAlpha,
         fresnelPower,
         fresnelColor,
         baseAlpha,
         fresnelInvert,
         fresnelMix,
         distortStrength,
         squirt,
         z
      );
   }

   public static void drawOutline(
      float x, float y, float width, float height, float radius, float thickness, int color
   ) {
      refreshRendererState();
      getOutlineDefaultRenderer().processData3(new OutlineDrawData(x, y, width, height, radius, thickness, applyAlpha(color)));
   }

   private static LineRenderer getLineRenderer() {
      return LineRenderer.getLineRenderer();
   }

   private static float easeAlphaAt(float t, float a, float b) {
      t = Math.max(0.0F, Math.min(1.0F, t));
      float baseValue = t < 0.5F ? a : b;
      float blendFactor = 1.0F - Math.abs(t - 0.5F) * 2.0F;
      return baseValue + (1.0F - baseValue) * blendFactor;
   }

   public static void drawArcDivider(float centerX, float centerY, float width, float lift, float thickness, int color) {
      refreshRendererState();
      getArcDividerRenderer().processData(new ShimmerDrawData(centerX, centerY, width, lift, thickness, 0.85F, color));
   }

   private static void refreshRendererState() {
      ImageRenderer.getImageRenderer().updateState6();
      EffectIconRenderer.getEffectIconRenderer().updateState3();
   }

   private static BlobRenderer getBlobRenderer() {
      return BlobRenderer.getBlobRenderer();
   }

   private static String buildObfuscationChars(String fontName) {
      String resolved = normalizeFontName(fontName);
      if (resolved == null) {
         return fallbackGlyphs;
      }

      String cached = obfuscationCache.get(resolved);
      if (cached != null) {
         return cached;
      }

      StringBuilder chars = new StringBuilder(fallbackGlyphs.length());

      for (int index = 0; index < fallbackGlyphs.length(); index++) {
         if (getMsdfTextRenderer().matchesCriteria2(resolved, fallbackGlyphs.charAt(index))) {
            chars.append(fallbackGlyphs.charAt(index));
         }
      }

      cached = chars.length() < 16 ? fallbackGlyphs : chars.toString();
      obfuscationCache.put(resolved, cached);
      return cached;
   }

   public static void drawMsdfGradientText(
      String fontName,
      String text,
      float x,
      float y,
      float size,
      int colorTopLeft,
      int colorTopRight,
      int colorBottomRight,
      int colorBottomLeft
   ) {
      refreshRendererState();
      getMsdfTextRenderer()
         .processData(
            new MsdfTextDrawData(
               fontName,
               text,
               x,
               y,
               size,
               applyAlpha(colorTopLeft),
               applyAlpha(colorTopRight),
               applyAlpha(colorBottomRight),
               applyAlpha(colorBottomLeft)
            )
         );
   }

   public static void drawImageSized(
      String texture, float x, float y, float explicitWidth, float explicitHeight, float radius, int color
   ) {
      getImageRenderer().processData2(new ImageDrawData(texture, x, y, explicitWidth, explicitHeight, radius, applyAlpha(color)));
   }

   public static void drawOutlineImmediate(
      DrawContext drawContext,
      float x,
      float y,
      float width,
      float height,
      float radius,
      float thickness,
      int color
   ) {
      flush(drawContext);
      drawOutline(x, y, width, height, radius, thickness, color);
      endFrame();
   }

   public static void drawBackdropBlurSoft(float x, float y, float width, float height, float radius) {
      drawBlurColored(x, y, width, height, radius, 16.0F, 1.0F, -1);
   }

   public static void drawSoftGlowPoint(float x, float y, int color, float softness, float intensity) {
      drawSoftGlowData(new SoftGlowDrawData(x, y, 0.0F, 0.0F, 0.0F, color, softness).withIntensity(intensity));
   }

   public static void drawOutlineGradientCorners(
      float x,
      float y,
      float width,
      float height,
      float radiusTopLeft,
      float radiusTopRight,
      float radiusBottomRight,
      float radiusBottomLeft,
      float thickness,
      int colorTopLeft,
      int colorTopRight,
      int colorBottomRight,
      int colorBottomLeft
   ) {
      refreshRendererState();
      getOutlineDefaultRenderer()
         .processData3(
            new OutlineDrawData(
               x,
               y,
               width,
               height,
               radiusTopLeft,
               radiusTopRight,
               radiusBottomRight,
               radiusBottomLeft,
               thickness,
               applyAlpha(colorTopLeft),
               applyAlpha(colorTopRight),
               applyAlpha(colorBottomRight),
               applyAlpha(colorBottomLeft),
               0.5F,
               null
            )
         );
   }

   public static void drawGlassDistorted(
      float x, float y, float width, float height, float size, float distortStrength, float radiusScale, int color
   ) {
      drawGlassArray(
         x,
         y,
         width,
         height,
         new float[]{radiusScale * size / 2.0F, radiusScale * size / 2.0F, radiusScale * size / 2.0F, radiusScale * size / 2.0F},
         color,
         (color >>> 24 & 0xFF) / 255.0F,
         height == 240.0F ? 100.0F : 50.0F,
         color | 0xFF000000,
         1.0F,
         true,
         0.0F,
         distortStrength,
         size,
         0.0F
      );
   }

   private static MsdfTextRenderer getMsdfTextRenderer() {
      return MsdfTextRenderer.getMsdfTextRenderer();
   }

   private static MenuGridRenderer getMenuGridRenderer() {
      return MenuGridRenderer.getMenuGridRenderer();
   }
}
