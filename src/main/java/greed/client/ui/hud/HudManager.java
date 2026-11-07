package greed.client.ui.hud;

import greed.client.ui.AnimatedValue;
import greed.client.render.ArcColorSpan;
import greed.client.storage.ConfigStore;
import greed.client.render.FontText;
import greed.client.util.GuiScale;
import greed.client.ui.UiTheme;

import greed.client.render.FontFace;
import greed.client.render.Render2D;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;

public final class HudManager {
   private static final Map<String, float[]> elementPositions = new HashMap<>();
   private static final Map<String, float[]> elementSettings = new HashMap<>();
   private static final Map<String, HudManager.SubContext> elementContexts = new LinkedHashMap<>();
   private static final Map<String, float[]> anchorOffsets = new HashMap<>();
   private static final Map<String, float[]> elementCenters = new HashMap<>();
   private static final Map<String, Long> hoverStartMillis = new HashMap<>();
   private static final Map<String, Float> pinnedCenterXs = new HashMap<>();
   private static final Map<String, float[]> pinnedBounds = new HashMap<>();
   private static final AnimatedValue gridOverlayAnimation = new AnimatedValue(160L).getAnimatedValue(0.0);
   private static final AnimatedValue moveHintAnimation = new AnimatedValue(160L).getAnimatedValue(0.0);
   private static final Map<String, AnimatedValue> scaleAnimations = new HashMap<>();
   private static final Map<String, AnimatedValue> handleAnimations = new HashMap<>();
   private static final AnimatedValue scaleLabelAnimation = new AnimatedValue(160L).getAnimatedValue(0.0);
   private static float[] moveHintBounds;
   private static float[] scaleLabelBounds;
   private static String scaleLabelKey;
   private static final List<String> elementOrder = new ArrayList<>(
      List.of("watermark", "keybinds", "serverbinds", "potions", "cooldowns", "staffs", "target", "inventory", "Target", "arrows")
   );
   private static String draggedKey;
   private static String selectedKey;
   private static float dragAnchorX;
   private static float dragAnchorY;
   private static float selectionStartX;
   private static float selectionStartY;
   private static float selectionStartScale;
   private static float selectionStartRadiusX;
   private static float selectionStartRadiusY;
   private static float viewportWidth;
   private static float viewportHeight;
   private static boolean initialized;
   private static Path configPath;

   public static List<String> getList() {
      return elementOrder;
   }

   public static boolean matchesCriteria(String elementKey) {
      return getElementSettings(elementKey)[3] > 0.5F;
   }

   public static String findSnapAnchor(float pointX, float pointY) {
      List<Entry<String, HudManager.SubContext>> entries = new ArrayList<>(elementContexts.entrySet());

      for (int index = entries.size() - 1; index >= 0; index--) {
         HudManager.SubContext value = entries.get(index).getValue();
         if (pointX >= value.minX
            && pointX <= value.minX + value.width
            && pointY >= value.minY
            && pointY <= value.minY + value.height) {
            return entries.get(index).getKey();
         }
      }

      return null;
   }

   public static boolean matchesCriteria2(String elementKey) {
      return false;
   }

   private static float[] getElementSettings(String elementKey) {
      ensureConfigLoaded();
      return elementSettings.computeIfAbsent(elementKey, key -> new float[]{0.9F, 1.0F, 0.0F, 0.0F});
   }

   public static void processData(String elementKey, boolean pinned) {
      getElementSettings(elementKey)[3] = pinned ? 1.0F : 0.0F;
      if (pinned) {
         pinnedCenterXs.put(elementKey, elementCenters.containsKey(elementKey) ? elementCenters.get(elementKey)[0] : viewportWidth / 2.0F);
      } else {
         pinnedCenterXs.remove(elementKey);
      }

      saveHudConfig();
   }

   public static float computeFloat2() {
      return viewportWidth;
   }

   public static float[] computeFloat3(String elementKey) {
      HudManager.SubContext context = elementContexts.get(elementKey);
      return context == null ? null : new float[]{context.minX, context.minY, context.width, context.height};
   }

   public static float computeFloat4() {
      return viewportHeight;
   }

   public static float[] computeFloat5(String elementKey, float defaultCenterX, float defaultCenterY) {
      ensureConfigLoaded();
      float[] floatArray = elementPositions.get(elementKey);
      float centerX = floatArray != null ? floatArray[0] : defaultCenterX;
      float centerY = floatArray != null ? floatArray[1] : defaultCenterY;
      if (!(viewportWidth <= 0.0F) && !(viewportHeight <= 0.0F)) {
         float[] floatArray2 = anchorOffsets.get(elementKey);
         float elementWidth = floatArray2 != null ? floatArray2[0] : 100.0F; // 100 пикселей дефолтная ширина вместо 0.0F
         float elementHeight = floatArray2 != null ? floatArray2[1] : 15.0F;  // 15 пикселей высота
         float anchorOffsetX = floatArray2 != null ? floatArray2[2] : 0.0F;
         float anchorOffsetY = floatArray2 != null ? floatArray2[3] : 0.0F;
         if (matchesCriteria(elementKey) && floatArray2 != null && pinnedCenterXs.containsKey(elementKey)) {
            centerX = pinnedCenterXs.get(elementKey) + anchorOffsetX - elementWidth / 2.0F;
         }

         float clampedMinX = Math.max(5.0F, Math.min(Math.max(5.0F, viewportWidth - elementWidth - 5.0F), centerX - anchorOffsetX));
         float clampedMinY = Math.max(5.0F, Math.min(Math.max(5.0F, viewportHeight - elementHeight - 5.0F), centerY - anchorOffsetY));
         if (floatArray2 != null) {
            clampedMinY = resolveVerticalOverlap(elementKey, clampedMinX, clampedMinY, elementWidth, elementHeight);
         }

         return new float[]{clampedMinX + anchorOffsetX, clampedMinY + anchorOffsetY};
      } else {
         return new float[]{centerX, centerY};
      }
   }

   public static void processData2(String elementKey, boolean hideF1) {
      getElementSettings(elementKey)[2] = hideF1 ? 1.0F : 0.0F;
      saveHudConfig();
   }

   public static void processData3(String elementKey, float scale) {
      getElementSettings(elementKey)[0] = Math.max(0.25F, Math.min(2.0F, scale));
   }

   public static float[] computeFloat6(String elementKey) {
      return elementCenters.get(elementKey);
   }

   private static void loadElementsFromJson(JsonObject jsonObject) {
      for (String elementKey : jsonObject.keySet()) {
         JsonObject asJsonObject = jsonObject.getAsJsonObject(elementKey);
         if (asJsonObject.has("x") && asJsonObject.has("y")) {
            elementPositions.put(elementKey, new float[]{asJsonObject.get("x").getAsFloat(), asJsonObject.get("y").getAsFloat()});
         }

         if (asJsonObject.has("scale") || asJsonObject.has("auto") || asJsonObject.has("hideF1") || asJsonObject.has("pin")) {
            float[] floatArray = elementSettings.computeIfAbsent(elementKey, key -> new float[]{0.9F, 1.0F, 0.0F, 0.0F});
            if (asJsonObject.has("scale")) {
               floatArray[0] = asJsonObject.get("scale").getAsFloat();
            }

            if (asJsonObject.has("auto")) {
               floatArray[1] = asJsonObject.get("auto").getAsBoolean() ? 1.0F : 0.0F;
            }

            if (asJsonObject.has("hideF1")) {
               floatArray[2] = asJsonObject.get("hideF1").getAsBoolean() ? 1.0F : 0.0F;
            }

            if (asJsonObject.has("pin")) {
               floatArray[3] = asJsonObject.get("pin").getAsBoolean() ? 1.0F : 0.0F;
            }
         }
      }
   }

   public static void processData5(int button) {
      if (button == 0 && draggedKey != null) {
         saveHudConfig();
         draggedKey = null;
      }

      if (button == 0 && selectedKey != null) {
         saveHudConfig();
         selectedKey = null;
      }
   }

   private static float resolveVerticalOverlap(String elementKey, float elementMinX, float elementMinY, float elementWidth, float elementHeight) {
      for (Entry<String, float[]> entry : pinnedBounds.entrySet()) {
         if (!entry.getKey().equals(elementKey) && (!matchesCriteria(elementKey) || getOrderIndex(entry.getKey()) < getOrderIndex(elementKey))) {
            float[] value = entry.getValue();
            if (elementMinX < value[0] + value[2]
               && elementMinX + elementWidth > value[0]
               && elementMinY < value[1] + value[3]
               && elementMinY + elementHeight > value[1]) {
               elementMinY = value[1] + value[3] + 4.0F;
            }
         }
      }

      return Math.max(5.0F, Math.min(Math.max(5.0F, viewportHeight - elementHeight - 5.0F), elementMinY));
   }

   private HudManager() {
   }

   public static void processData6(
      String elementKey, float boundsX, float boundsY, float boundsWidth, float boundsHeight, float anchorX, float anchorY
   ) {
      float scale = computeFloat8(elementKey);
      float centerX = boundsX + boundsWidth / 2.0F;
      float centerY = boundsY + boundsHeight / 2.0F;
      HudManager.SubContext context = new HudManager.SubContext();
      context.minX = centerX - boundsWidth * scale / 2.0F;
      context.minY = centerY - boundsHeight * scale / 2.0F;
      context.width = boundsWidth * scale;
      context.height = boundsHeight * scale;
      context.anchorPointX = anchorX;
      context.anchorPointY = anchorY;
      elementContexts.put(elementKey, context);
      anchorOffsets.put(
         elementKey,
         new float[]{context.width, context.height, anchorX - context.minX, anchorY - context.minY}
      );
      elementCenters.put(elementKey, new float[]{centerX, centerY});
      if (matchesCriteria(elementKey)) {
         pinnedBounds.put(elementKey, new float[]{context.minX, context.minY, context.width, context.height});
         pinnedCenterXs.putIfAbsent(elementKey, centerX);
      } else {
         pinnedBounds.remove(elementKey);
      }
   }

   public static boolean checkState() {
      return draggedKey != null;
   }

   public static boolean matchesCriteria3(double mouseX, double mouseY, int button) {
      if (button != 0) {
         return false;
      }

      float scaleFactor = getInverseScaleFactor();
      float scaledMouseX = (float)(mouseX * scaleFactor);
      float scaledMouseY = (float)(mouseY * scaleFactor);
      String resizeHandleKey = formatText(scaledMouseX, scaledMouseY);
      if (resizeHandleKey != null) {
         HudManager.SubContext context = elementContexts.get(resizeHandleKey);
         selectedKey = resizeHandleKey;
         selectionStartX = scaledMouseX;
         selectionStartY = scaledMouseY;
         selectionStartScale = Math.max(0.25F, Math.min(2.0F, computeFloat9(resizeHandleKey)));
         selectionStartRadiusX = Math.max(1.0F, context.width / computeFloat8(resizeHandleKey) / 2.0F);
         selectionStartRadiusY = Math.max(1.0F, context.height / computeFloat8(resizeHandleKey) / 2.0F);
         if (elementOrder.remove(selectedKey)) {
            elementOrder.add(selectedKey);
         }

         return true;
      } else {
         String anchorKey = findSnapAnchor(scaledMouseX, scaledMouseY);
         if (anchorKey == null) {
            return false;
         }

         HudManager.SubContext anchorContext = elementContexts.get(anchorKey);
         draggedKey = anchorKey;
         dragAnchorX = scaledMouseX - anchorContext.anchorPointX;
         dragAnchorY = scaledMouseY - anchorContext.anchorPointY;
         if (elementOrder.remove(draggedKey)) {
            elementOrder.add(draggedKey);
         }

         return true;
      }
   }

   private static void ensureConfigLoaded() {
      if (!initialized) {
         initialized = true;
         configPath = FabricLoader.getInstance().getConfigDir().resolve("greed").resolve("draggables.json");

         try {
            if (!Files.exists(configPath)) {
               return;
            }

            loadElementsFromJson(JsonParser.parseString(Files.readString(configPath, StandardCharsets.UTF_8)).getAsJsonObject());
         } catch (Exception exception) {
         }
      }
   }

   public static JsonObject getJsonObject() {
      ensureConfigLoaded();
      JsonObject jsonObject = new JsonObject();
      Set<String> elementKeys = new LinkedHashSet<>(elementPositions.keySet());
      elementKeys.addAll(elementSettings.keySet());

      for (String elementKey : elementKeys) {
         JsonObject jsonObject2 = new JsonObject();
         float[] floatArray = elementPositions.get(elementKey);
         if (floatArray != null) {
            jsonObject2.addProperty("x", floatArray[0]);
            jsonObject2.addProperty("y", floatArray[1]);
         }

         float[] floatArray2 = elementSettings.get(elementKey);
         if (floatArray2 != null) {
            jsonObject2.addProperty("scale", floatArray2[0]);
            jsonObject2.addProperty("auto", floatArray2[1] > 0.5F);
            jsonObject2.addProperty("hideF1", floatArray2[2] > 0.5F);
            jsonObject2.addProperty("pin", floatArray2.length > 3 && floatArray2[3] > 0.5F);
         }

         jsonObject.add(elementKey, jsonObject2);
      }

      return jsonObject;
   }

   static void processData7(float mouseX, float mouseY, boolean dragging, float newViewportWidth, float newViewportHeight) {
      viewportWidth = newViewportWidth;
      viewportHeight = newViewportHeight;
      if (!dragging) {
         if (draggedKey != null) {
            saveHudConfig();
            draggedKey = null;
         }

         if (selectedKey != null) {
            saveHudConfig();
            selectedKey = null;
         }

         hoverStartMillis.clear();
      } else {
         if (selectedKey != null) {
            processData3(selectedKey, selectionStartScale + ((mouseX - selectionStartX) / selectionStartRadiusX + (mouseY - selectionStartY) / selectionStartRadiusY) / 2.0F);
         }

         if (draggedKey != null) {
            float[] floatArray = anchorOffsets.get(draggedKey);
            float anchorOffsetX = floatArray != null ? floatArray[2] : 0.0F;
            float anchorOffsetY = floatArray != null ? floatArray[3] : 0.0F;
            float anchorX = mouseX - dragAnchorX;
            float anchorY = mouseY - dragAnchorY;
            if (checkState2() && floatArray != null) {
               if (Math.abs(anchorX - anchorOffsetX + floatArray[0] / 2.0F - viewportWidth / 2.0F) < 8.0F) {
                  anchorX = viewportWidth / 2.0F - floatArray[0] / 2.0F + anchorOffsetX;
               }

               if (Math.abs(anchorY - anchorOffsetY + floatArray[1] / 2.0F - viewportHeight / 2.0F) < 8.0F) {
                  anchorY = viewportHeight / 2.0F - floatArray[1] / 2.0F + anchorOffsetY;
               }
            }

            elementPositions.put(draggedKey, new float[]{anchorX, anchorY});
            if (matchesCriteria(draggedKey) && floatArray != null) {
               pinnedCenterXs.put(draggedKey, anchorX - anchorOffsetX + floatArray[0] / 2.0F);
            }
         }
      }
   }

   private static String formatText(float pointX, float pointY) {
      List<Entry<String, HudManager.SubContext>> entries = new ArrayList<>(elementContexts.entrySet());

      for (int index = entries.size() - 1; index >= 0; index--) {
         HudManager.SubContext value = entries.get(index).getValue();
         float handleSize = Math.min(9.0F, Math.min(value.width, value.height) / 3.0F);
         if (pointX >= value.minX + value.width - handleSize
            && pointX <= value.minX + value.width + 4.0F
            && pointY >= value.minY + value.height - handleSize
            && pointY <= value.minY + value.height + 4.0F) {
            return entries.get(index).getKey();
         }
      }

      return null;
   }

   public static float computeFloat8(String elementKey) {
      float targetScale = Math.max(0.25F, Math.min(2.0F, getElementSettings(elementKey)[0]));
      AnimatedValue animatedValue = scaleAnimations.computeIfAbsent(elementKey, key -> new AnimatedValue(140L).getAnimatedValue(targetScale));
      animatedValue.processData(targetScale);
      return (float)animatedValue.computeDouble();
   }

   private static int getOrderIndex(String elementKey) {
      int orderIndex = elementOrder.indexOf(elementKey);
      return orderIndex < 0 ? Integer.MAX_VALUE : orderIndex;
   }

   public static boolean matchesCriteria4(String elementKey) {
      return getElementSettings(elementKey)[2] > 0.5F;
   }

   private static void saveHudConfig() {
      ensureConfigLoaded();

      try {
         ConfigStore.writeAsync(configPath, getJsonObject().toString());
      } catch (Exception exception) {
      }
   }

   public static float computeFloat9(String elementKey) {
      return getElementSettings(elementKey)[0];
   }

   static void saveHudLayout() {
      elementContexts.clear();
   }

   public static void updateState2() {
      saveHudConfig();
   }

   static void processData8(float mouseX, float mouseY, boolean active) {
      if (active) {
         gridOverlayAnimation.processData(checkState2() && draggedKey != null ? 1.0 : 0.0);
         float gridProgress = (float)gridOverlayAnimation.computeDouble();
         if (gridProgress > 0.01F && viewportWidth > 0.0F && viewportHeight > 0.0F) {
            Render2D.drawRect(
               viewportWidth / 2.0F - 0.25F,
               viewportHeight / 2.0F * (1.0F - gridProgress),
               0.5F,
               viewportHeight * gridProgress,
               0.0F,
               UiTheme.computeInt(UiTheme.intVal, 0.14F * gridProgress)
            );
            Render2D.drawRect(
               viewportWidth / 2.0F * (1.0F - gridProgress),
               viewportHeight / 2.0F - 0.25F,
               viewportWidth * gridProgress,
               0.5F,
               0.0F,
               UiTheme.computeInt(UiTheme.intVal, 0.14F * gridProgress)
            );
         }

         long nowMillis = System.currentTimeMillis();

         for (Entry<String, HudManager.SubContext> elementEntry : elementContexts.entrySet()) {
            HudManager.SubContext value = elementEntry.getValue();
            boolean highlighted = elementEntry.getKey().equals(draggedKey)
               || mouseX >= value.minX
                  && mouseX <= value.minX + value.width
                  && mouseY >= value.minY
                  && mouseY <= value.minY + value.height;
            if (!highlighted) {
               hoverStartMillis.remove(elementEntry.getKey());
            } else {
               float hoverProgress = Math.min(
                  1.0F, Math.max(0.02F, (float)(nowMillis - hoverStartMillis.computeIfAbsent(elementEntry.getKey(), key -> nowMillis)) / 50.0F)
               );
               Render2D.drawOutlineArcUniformVarargs(
                  value.minX - 1.5F,
                  value.minY - 1.5F,
                  value.width + 3.0F,
                  value.height + 3.0F,
                  9.5F,
                  0.5F,
                  0,
                  ArcColorSpan.of(90.0F - hoverProgress * 360.0F, 90.0F, UiTheme.computeInt(UiTheme.intVal, 0.3F))
               );
            }
         }

         String resizeHandleKey = formatText(mouseX, mouseY);

         for (Entry<String, HudManager.SubContext> elementEntry2 : elementContexts.entrySet()) {
            HudManager.SubContext value2 = elementEntry2.getValue();
            AnimatedValue animatedValue = handleAnimations.computeIfAbsent(elementEntry2.getKey(), key -> new AnimatedValue(160L).getAnimatedValue(0.0));
            animatedValue.processData(
               !elementEntry2.getKey().equals(selectedKey)
                     && (
                        !(mouseX >= value2.minX - 4.0F)
                           || !(mouseX <= value2.minX + value2.width + 4.0F)
                           || !(mouseY >= value2.minY - 4.0F)
                           || !(mouseY <= value2.minY + value2.height + 4.0F)
                     )
                  ? 0.0
                  : 1.0
            );
            float handleProgress = (float)animatedValue.computeDouble();
            if (!(handleProgress < 0.01F)) {
               float handleAlpha = handleProgress * (!elementEntry2.getKey().equals(selectedKey) && !elementEntry2.getKey().equals(resizeHandleKey) ? 0.45F : 0.9F);
               float handleBaseX = value2.minX + value2.width + 1.5F;
               float handleBaseY = value2.minY + value2.height + 1.5F;

               for (int index = 0; index < 3; index++) {
                  for (int index2 = 0; index2 < 3; index2++) {
                     if (index + index2 >= 2) {
                        Render2D.drawRect(
                           handleBaseX - 2.6F - (2 - index) * 2.2F,
                           handleBaseY - 2.6F - (2 - index2) * 2.2F,
                           1.1F,
                           1.1F,
                           0.55F,
                           UiTheme.computeInt(UiTheme.intVal, handleAlpha)
                        );
                     }
                  }
               }
            }
         }

         scaleLabelAnimation.processData(selectedKey != null && elementContexts.containsKey(selectedKey) ? 1.0 : 0.0);
         if (selectedKey != null && elementContexts.containsKey(selectedKey)) {
            scaleLabelBounds = computeFloat3(selectedKey);
            scaleLabelKey = selectedKey;
         }

         float scaleLabelProgress = (float)scaleLabelAnimation.computeDouble();
         if (scaleLabelProgress > 0.01F && scaleLabelBounds != null) {
            String scaleText = Math.round(computeFloat9(scaleLabelKey) * 100.0F) + "%";
            float scaleTextWidth = FontText.computeFloat(FontFace.SEMI, scaleText, 5.0F);
            float labelX = scaleLabelBounds[0] + scaleLabelBounds[2] + 5.0F;
            if (labelX + scaleTextWidth > viewportWidth - 2.0F) {
               labelX = scaleLabelBounds[0] + scaleLabelBounds[2] - 10.0F - scaleTextWidth;
            }

            FontText.processData3(
               FontFace.SEMI,
               scaleText,
               labelX,
               scaleLabelBounds[1] + scaleLabelBounds[3] + 3.0F + (1.0F - scaleLabelProgress) * 2.0F,
               5.0F,
               UiTheme.computeInt(UiTheme.intVal, 0.8F * scaleLabelProgress)
            );
         }

         moveHintAnimation.processData(draggedKey != null && elementContexts.containsKey(draggedKey) ? 1.0 : 0.0);
         if (draggedKey != null && elementContexts.containsKey(draggedKey)) {
            moveHintBounds = computeFloat3(draggedKey);
         }

         float moveHintProgress = (float)moveHintAnimation.computeDouble();
         if (moveHintProgress > 0.01F && moveHintBounds != null) {
            FontText.processData3(
               FontFace.SEMI,
               "Зажмите LCTRL для перемещения по сетке",
               moveHintBounds[0]
                  + moveHintBounds[2] / 2.0F
                  - FontText.computeFloat(FontFace.SEMI, "Зажмите LCTRL для перемещения по сетке", 5.0F) / 2.0F,
               moveHintBounds[1] + moveHintBounds[3] + 4.0F + (1.0F - moveHintProgress) * 3.0F,
               5.0F,
               UiTheme.computeInt(UiTheme.intVal, 0.7F * moveHintProgress)
            );
         }
      }
   }

   private static float getInverseScaleFactor() {
      return GuiScale.computeInt() / GuiScale.computeFloat();
   }

   public static boolean checkState2() {
      return MinecraftClient.getInstance().getWindow() != null && InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow(), 341);
   }

   public static void processData9(String elementKey, float boundsX, float boundsY, float boundsWidth, float boundsHeight) {
      processData6(elementKey, boundsX, boundsY, boundsWidth, boundsHeight, boundsX, boundsY);
   }

   public static boolean matchesCriteria5(String elementKey) {
      ensureConfigLoaded();
      return elementPositions.containsKey(elementKey);
   }

   public static float[] computeFloat11(double screenX, double screenY) {
      float scaleFactor = getInverseScaleFactor();
      return new float[]{(float)(screenX * scaleFactor), (float)(screenY * scaleFactor)};
   }

   public static void processData10(JsonObject jsonObject) {
      ensureConfigLoaded();
      elementPositions.clear();
      elementSettings.clear();
      pinnedCenterXs.clear();
      loadElementsFromJson(jsonObject);
      saveHudConfig();
   }

   public static void processData11(String elementKey, boolean auto) {
      getElementSettings(elementKey)[1] = auto ? 1.0F : 0.0F;
      saveHudConfig();
   }

   public static void updateState3() {
      if (draggedKey != null) {
         saveHudConfig();
         draggedKey = null;
      }

      if (selectedKey != null) {
         saveHudConfig();
         selectedKey = null;
      }

      hoverStartMillis.clear();
   }

   private static final class SubContext {
      float minX;
      float minY;
      float width;
      float height;
      float anchorPointX;
      float anchorPointY;
   }
}
