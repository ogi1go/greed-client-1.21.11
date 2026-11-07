package greed.client.ui.setting;

import greed.client.ui.AnimatedValue;
import greed.client.ui.ClickGuiStyle;
import greed.client.setting.ColorSetting;
import greed.client.render.FontText;
import greed.client.render.GuiEffectsState;
import greed.client.util.GuiScale;
import greed.client.render.RectDrawData;
import greed.client.ui.UiTheme;

import greed.client.util.Colors;
import greed.client.render.FontFace;
import greed.client.setting.KeybindSetting;
import greed.client.module.Module;
import greed.client.setting.NumberSetting;
import greed.client.render.Render2D;
import greed.client.setting.TextSetting;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.gui.DrawContext;

public final class SettingRenderContext {
   public static final float floatVal = 30.0F;
   public DrawContext drawContext;
   public float scale = 1.0F;
   public float mouseX;
   public float mouseY;
   private final Map<String, AnimatedValue> stringAnimationCache = new HashMap<>();
   private final Map<Object, AnimatedValue> objectAnimationCache = new IdentityHashMap<>();
   private final Map<SettingRenderContext.DataRecord3, List<String>> wrappedTextCache = new LinkedHashMap<SettingRenderContext.DataRecord3, List<String>>(64, 0.75F, true) {
      @Override
      protected boolean removeEldestEntry(Entry<SettingRenderContext.DataRecord3, List<String>> eldestEntry) {
         return this.size() > 256;
      }
   };
   private final Map<Object, Boolean> toggleStateMap = new HashMap<>();
   private final Map<Object, Object> genericValueCache = new IdentityHashMap<>();
   private final Map<ColorSetting, float[]> colorHsbCache = new IdentityHashMap<>();
   public final Map<Object, float[]> map7 = new IdentityHashMap<>();
   public final Map<Object, float[]> map8 = new IdentityHashMap<>();
   public final List<SettingRenderContext.DataRecord2> list = new ArrayList<>();
   public int intVal;
   public NumberSetting numberSetting;
   public ColorSetting colorSetting;
   public int intVal2;
   public TextSetting textSetting;
   public KeybindSetting keybindSetting;
   public Module module;
   public SettingRenderContext.SubInterface nested0492;
   public String strVal = "";
   public Object object;
   public String strVal2;
   public String strVal3 = "";
   public final List<Runnable> list2 = new ArrayList<>();
   private Map<String, long[]> marqueeStateCache = new HashMap<>();
   public Map<Object, Float> map10 = new IdentityHashMap<>();
   public Object object2;
   public float[] floatValArray;
   public float floatVal2;

   // Bounds of the currently active scrollable area
   public float[] categoryBounds;

   // Maximum scroll offset for the active area
   public float openProgressStart;

   private List<String> getList(String text, float size, float width) {
      SettingRenderContext.DataRecord3 cacheKey = new SettingRenderContext.DataRecord3(text, size, width);
      List<String> wrappedLines = this.wrappedTextCache.get(cacheKey);
      if (wrappedLines == null) {
         wrappedLines = List.copyOf(UiTheme.getList(FontFace.SF, text, size, width));
         this.wrappedTextCache.put(cacheKey, wrappedLines);
      }

      return wrappedLines;
   }

   public void processData(TextSetting newTextSetting, float x, float y, float fontSize, int color) {
      if (newTextSetting.hasSelection()) {
         Render2D.drawRect(
            x + FontText.computeFloat(FontFace.SF, newTextSetting.getValue().substring(0, newTextSetting.getSelectionStart()), fontSize),
            y - 1.0F * this.scale,
            FontText.computeFloat(
               FontFace.SF, newTextSetting.getValue().substring(newTextSetting.getSelectionStart(), newTextSetting.getSelectionEnd()), fontSize
            ),
            fontSize + 2.0F * this.scale,
            1.0F * this.scale,
            UiTheme.computeInt(UiTheme.intVal7, 0.4F)
         );
      }

      FontText.processData3(FontFace.SF, newTextSetting.getValue(), x, y, fontSize, color);
      if (System.currentTimeMillis() / 530L % 2L == 0L) {
         Render2D.drawRect(
            x + FontText.computeFloat(FontFace.SF, newTextSetting.getValue().substring(0, newTextSetting.getCaret()), fontSize),
            y - 0.5F * this.scale,
            0.5F * this.scale,
            fontSize + 1.0F * this.scale,
            0.25F * this.scale,
            color
         );
      }
   }

   public void processData2(float x, float y, float width, float height, float radius, int color) {
      Render2D.drawBackdropBlur(x, y, width, height, radius);
      Render2D.drawRect(x, y, width, height, radius, ClickGuiStyle.intVal41);
      Render2D.drawRect(x, y, width, height, radius, ClickGuiStyle.intVal43);
      Render2D.drawRect(x, y, width, height, radius, ClickGuiStyle.intVal44);
      Render2D.drawOutline(x, y, width, height, radius, 0.3F, color);
   }

   public static float computeFloat(float value, float min, float max) {
      return Math.max(min, Math.min(max, value));
   }

   public static String formatText() {
      return System.currentTimeMillis() / 500L % 2L == 0L ? "_" : "";
   }

   public void processData3(float x, float y, float width, float height, float radius, int fillColor, int borderColor) {
      Render2D.drawBackdropBlur(x, y, width, height, radius);
      Render2D.drawRect(x, y, width, height, radius, ClickGuiStyle.intVal41);
      Render2D.drawRect(x, y, width, height, radius, ClickGuiStyle.intVal45);
      Render2D.drawRect(x, y, width, height, radius, fillColor);
      Render2D.drawOutline(x, y, width, height, radius, 0.3F, borderColor);
   }

   public float computeFloat2(String key, boolean active) {
      AnimatedValue animatedValue = this.stringAnimationCache
         .computeIfAbsent(key, ignoredKey -> new AnimatedValue(150L).getAnimatedValue(active ? 1.0 : 0.0));
      animatedValue.processData(active ? 1.0 : 0.0);
      return (float)animatedValue.computeDouble();
   }

   public static float[] computeFloat3(float[] floatArray, float x, float y, float width, float height) {
      if (floatArray == null) {
         return new float[]{x, y, Math.max(0.0F, width), Math.max(0.0F, height)};
      }

      float intersectLeft = Math.max(floatArray[0], x);
      float intersectTop = Math.max(floatArray[1], y);
      float intersectRight = Math.min(floatArray[0] + floatArray[2], x + width);
      return new float[]{
         intersectLeft,
         intersectTop,
         Math.max(0.0F, intersectRight - intersectLeft),
         Math.max(0.0F, Math.min(floatArray[1] + floatArray[3], y + height) - intersectTop)
      };
   }

   public float computeFloat4(String key) {
      AnimatedValue animatedValue = this.stringAnimationCache.computeIfAbsent(key, ignoredKey -> new AnimatedValue(440L));
      animatedValue.processData(1.0);
      return (float)animatedValue.computeDouble();
   }

   public void processData4(String text, float x, float y, float width, int color) {
      UiTheme.processData7(text, x, y - 0.5F * this.scale, width, color);
   }

   public boolean matchesCriteria(Object key, boolean defaultValue) {
      return this.toggleStateMap.getOrDefault(key, defaultValue);
   }

   public void processData5(Object key, boolean defaultValue) {
      this.toggleStateMap.put(key, !this.matchesCriteria(key, defaultValue));
   }

   public void processData6(
      float x, float y, float width, float height, float[] floatArray, Runnable runnable, Runnable runnable2
   ) {
      this.processData11(x, y, width, height, floatArray, runnable, runnable2, null);
   }

   public void updateState() {
      if (!this.list2.isEmpty()) {
         Render2D.bumpGuiLayer(this.drawContext);
         if (!GuiEffectsState.checkState2()) {
            GuiEffectsState.processData3(true);
         }

         this.drawContext.createNewRootLayer();

         try {
            this.drawContext.applyBlur();
         } catch (IllegalStateException exception) {
         }

         Render2D.setBlurMode(1);

         for (Runnable runnable : List.copyOf(this.list2)) {
            runnable.run();
         }

         Render2D.bumpGuiLayer(this.drawContext);
         Render2D.setBlurMode(0);
      }
   }

   public float[] computeFloat5(ColorSetting newColorSetting) {
      return this.colorHsbCache
         .computeIfAbsent(
            newColorSetting,
            newColorSetting2 -> new float[]{newColorSetting2.getHue(), newColorSetting2.getSaturation(), newColorSetting2.getBrightness(), newColorSetting2.getAlpha() / 255.0F}
         );
   }

   public void processData7(float x, float y, float width, float height, float[] floatArray, Runnable runnable) {
      this.processData6(x, y, width, height, floatArray, runnable, null);
   }

   public void processData8(String text, String description, float x, float y, float maxWidth, float blendFactor) {
      this.processData15(
         text,
         x,
         y,
         5.5F * this.scale,
         UiTheme.computeInt2(UiTheme.intVal2, UiTheme.intVal, blendFactor),
         maxWidth
      );
      if (description != null && !description.isEmpty()) {
         float lineY = y + 8.0F * this.scale;

         for (String line : this.getList(description, 5.0F * this.scale, maxWidth)) {
            FontText.processData3(
               FontFace.SF,
               line,
               x,
               lineY,
               5.0F * this.scale,
               UiTheme.computeInt2(UiTheme.intVal3, UiTheme.intVal2, blendFactor)
            );
            lineY += 6.0F * this.scale;
         }
      }
   }

   public float computeFloat6(String text, float width) {
      int lineCount = this.computeInt(text, width);
      return 7.0F * this.scale + (lineCount > 0 ? 1.0F * this.scale + lineCount * 6.0F * this.scale : 0.0F);
   }

   public void processData9(float x, float y, float width, float height, float radius, int fillColor, int borderColor) {
      Render2D.drawBlurColor(x, y, width, height, radius, -1090519041);
      Render2D.drawRect(x, y, width, height, radius, ClickGuiStyle.intVal41);
      Render2D.drawRect(x, y, width, height, radius, Colors.computeInt6(fillColor, 0.8F));
      Render2D.drawOutline(x, y, width, height, radius, 0.5F, borderColor);
   }

   public void processData10(ColorSetting newColorSetting, float[] floatArray) {
      newColorSetting.setValue(
         Math.round(Math.max(0.0F, Math.min(1.0F, floatArray[3])) * 255.0F) << 24 | Color.HSBtoRGB(floatArray[0], floatArray[1], floatArray[2]) & 16777215
      );
   }

   public void processData11(
      float x,
      float y,
      float width,
      float height,
      float[] floatArray,
      Runnable runnable,
      Runnable runnable2,
      Runnable runnable3
   ) {
      if (floatArray != null) {
         float clipRight = Math.min(x + width, floatArray[0] + floatArray[2]);
         float clipBottom = Math.min(y + height, floatArray[1] + floatArray[3]);
         x = Math.max(x, floatArray[0]);
         y = Math.max(y, floatArray[1]);
         width = clipRight - x;
         height = clipBottom - y;
         if (width <= 0.0F || height <= 0.0F) {
            return;
         }
      }

      this.list.add(new SettingRenderContext.DataRecord2(x, y, width, height, runnable, runnable2, runnable3));
   }

   public float computeFloat7(String key, float target) {
      AnimatedValue animatedValue = this.stringAnimationCache.computeIfAbsent(key, ignoredKey -> new AnimatedValue(150L).getAnimatedValue(target));
      animatedValue.processData(target);
      return (float)animatedValue.computeDouble();
   }

   public static float computeFloat8(float value) {
      float guiScale = GuiScale.computeFloat() / GuiScale.computeInt();
      return guiScale > 1.0E-4F ? Math.round(value * guiScale) / guiScale : value;
   }

   public <T> T getT(Object key, T defaultValue) {
      return (T)(Object)this.genericValueCache.computeIfAbsent(key, ignoredKey -> defaultValue);
   }

   public void processData12(float x, float y, float width, float height, float radius, int fillColor, int borderColor) {
      Render2D.drawRect(x, y, width, height, radius, ClickGuiStyle.intVal45);
      Render2D.drawRect(x, y, width, height, radius, fillColor);
      Render2D.drawOutline(x, y, width, height, radius, 0.3F, borderColor);
   }

   public static float computeFloat9(float value) {
      return GuiScale.computeFloat4(value);
   }

   public void processData13(float x, float y, float width, float height, float radius, int fillColor, int borderColor) {
      Render2D.drawBackdropBlur(x, y, width, height, radius);
      Render2D.drawRect(x, y, width, height, radius, ClickGuiStyle.intVal41);
      Render2D.drawRect(x, y, width, height, radius, Colors.computeInt6(fillColor, 0.8F));
      Render2D.drawOutline(x, y, width, height, radius, 0.5F, borderColor);
   }

   public float computeFloat10(Object key, boolean active) {
      AnimatedValue animatedValue = this.objectAnimationCache.get(key);
      if (animatedValue == null) {
         this.objectAnimationCache.put(key, animatedValue = new AnimatedValue(220L).getAnimatedValue(active ? 1.0 : 0.0));
      }

      animatedValue.processData(active ? 1.0 : 0.0);
      return (float)animatedValue.computeDouble();
   }

   public void processData14(String icon, float x, float y, float size, int color, float rotation) {
      if (Math.abs(rotation) < 0.01F) {
         UiTheme.processData7(icon, x, y - 0.5F * this.scale, size, color);
      } else {
         float centerY = y - 0.5F * this.scale;
         float[] floatArray = Render2D.measureMsdfText("icons", icon, size);
         float iconX = x - (floatArray[0] + floatArray[2]) / 2.0F;
         Render2D.drawMsdfTextRotated(
            "icons",
            icon,
            iconX,
            centerY - (floatArray[1] + floatArray[3]) / 2.0F + 1.0F,
            size,
            color,
            rotation,
            x,
            centerY + 1.0F
         );
      }
   }

   public void processData15(String text, float x, float y, float size, int color, float maxWidth) {
      float overflowWidth = FontText.computeFloat(FontFace.SF, text, size) - maxWidth;
      if (overflowWidth <= 0.0F) {
         FontText.processData3(FontFace.SF, text, x, y, size, color);
      } else {
         String marqueeKey = "marquee:" + text + ":" + Math.round(x) + ":" + Math.round(maxWidth);
         boolean hovered = UiTheme.matchesCriteria(
            this.mouseX, this.mouseY, x, y - 1.5F * this.scale, maxWidth, size + 3.0F * this.scale
         );
         long nowMillis = System.currentTimeMillis();
         long[] longArray = this.marqueeStateCache.get(marqueeKey);
         float progress = 0.0F;
         if (hovered) {
            if (longArray == null) {
               this.marqueeStateCache.put(marqueeKey, longArray = new long[]{nowMillis, nowMillis});
            }

            if (nowMillis - longArray[1] > 120L) {
               longArray[0] = nowMillis;
            }

            longArray[1] = nowMillis;
            long scrollDurationMillis = Math.max(700L, (long)(overflowWidth / this.scale * 55.0F));
            long pauseDurationMillis = 650L;
            long elapsedMillis = nowMillis - longArray[0] - 100L;
            long cycleMillis = Math.max(0L, elapsedMillis) % (2L * (scrollDurationMillis + pauseDurationMillis));
            progress = elapsedMillis < 0L
               ? 0.0F
               : (
                  cycleMillis < scrollDurationMillis
                     ? (float)cycleMillis / (float)scrollDurationMillis
                     : (
                        cycleMillis < scrollDurationMillis + pauseDurationMillis
                           ? 1.0F
                           : (
                              cycleMillis < 2L * scrollDurationMillis + pauseDurationMillis
                                 ? 1.0F - (float)(cycleMillis - scrollDurationMillis - pauseDurationMillis) / (float)scrollDurationMillis
                                 : 0.0F
                           )
                     )
               );
            progress = progress * progress * (3.0F - 2.0F * progress);
         }

         boolean marqueeActive = longArray != null && nowMillis - longArray[1] < 400L;
         float scrollOffset = marqueeActive ? overflowWidth * this.computeFloat7(marqueeKey, progress) : 0.0F;
         float fadeWidth = 4.0F * this.scale;
         FontText.processData6(
            FontFace.SF,
            text,
            x - scrollOffset,
            y,
            size,
            color,
            x,
            x + maxWidth,
            fadeWidth,
            computeFloat(scrollOffset / fadeWidth, 0.0F, 1.0F),
            computeFloat((overflowWidth - scrollOffset) / fadeWidth, 0.0F, 1.0F)
         );
      }
   }

   public int computeInt(String text, float width) {
      return text != null && !text.isEmpty() ? this.getList(text, 5.0F * this.scale, width).size() : 0;
   }

   public void processData16(float x, float y, float width, float height, float radius, int color) {
      Render2D.drawBlurSmooth(x, y, width, height, radius, 30.0F, 1.0F, ClickGuiStyle.intVal46);
      Render2D.drawRect(x, y, width, height, radius, ClickGuiStyle.intVal41);
      Render2D.drawRect(x, y, width, height, radius, ClickGuiStyle.intVal42);
      Render2D.drawOutline(x, y, width, height, radius, 0.5F, color);
   }

   public void processData17(String tooltip, float x, float y, float width, float height) {
      if (tooltip != null && !tooltip.isEmpty()) {
         if (UiTheme.matchesCriteria(this.mouseX, this.mouseY, x, y, width, height)) {
            this.strVal2 = tooltip;
         }
      }
   }

   public void renderElement(DrawContext drawContext, float newScale, float newMouseX, float newMouseY) {
      this.drawContext = drawContext;
      this.scale = newScale;
      this.mouseX = newMouseX;
      this.mouseY = newMouseY;
      this.list.clear();
      this.intVal = 0;
      this.map7.clear();
      this.map8.clear();
      this.strVal2 = null;
      this.list2.clear();
      this.object2 = null;
      this.floatValArray = null;
   }

   public void processData18(float x, float y, float progress) {
      float trackWidth = 10.74F * this.scale;
      float trackHeight = 6.64F * this.scale;
      float knobSize = 4.88F * this.scale;
      Render2D.drawRectData(
         new RectDrawData(
               x,
               y,
               trackWidth,
               trackHeight,
               trackHeight / 2.0F,
               trackHeight / 2.0F,
               trackHeight / 2.0F,
               trackHeight / 2.0F,
               Render2D.applyAlpha(UiTheme.computeInt2(ClickGuiStyle.intVal15, ClickGuiStyle.intVal17, progress))
            )
            .withSmoothness(0.5F)
      );
      Render2D.drawCircleSmooth(
         x + 0.93F * this.scale + progress * (trackWidth - knobSize - 1.86F * this.scale) + knobSize / 2.0F,
         y + trackHeight / 2.0F,
         knobSize / 2.0F,
         0.5F,
         Render2D.applyAlpha(UiTheme.computeInt2(ClickGuiStyle.intVal16, UiTheme.intVal7, progress))
      );
   }

   public interface SubInterface {
      void processData19(int var1);

      void processData20(char var1);
   }

   public record DataRecord2(float x, float y, float w, float h, Runnable action, Runnable rightAction, Runnable middleAction) {
   }

   private record DataRecord3(String text, float size, float width) {
   }
}
