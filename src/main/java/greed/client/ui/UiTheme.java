package greed.client.ui;

import greed.client.render.ArcColorSpan;
import greed.client.render.FontText;
import greed.client.render.Outline360DrawData;

import greed.client.render.FontFace;
import greed.client.render.Render2D;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public final class UiTheme {
   public static int intVal = new Color(255, 255, 255, 255).getRGB();
   public static int intVal2 = new Color(255, 255, 255, 184).getRGB();
   public static int intVal3 = new Color(255, 255, 255, 122).getRGB();
   public static int intVal4 = new Color(255, 255, 255, 61).getRGB();
   public static int intVal5 = new Color(255, 255, 255, 26).getRGB();
   public static int intVal6 = new Color(255, 255, 255, 10).getRGB();
   public static int intVal7 = new Color(71, 123, 255, 255).getRGB();
   public static int intVal8 = new Color(123, 163, 255, 255).getRGB();
   public static int intVal9 = new Color(97, 142, 255, 255).getRGB();
   public static int intVal10 = new Color(255, 255, 255, 255).getRGB();
   public static int intVal11 = new Color(15, 14, 17, 255).getRGB();
   public static int intVal12 = new Color(255, 255, 255, 255).getRGB();
   public static int intVal13 = new Color(19, 18, 22, 255).getRGB();
   public static int intVal14 = new Color(26, 25, 31, 255).getRGB();
   public static int intVal15 = new Color(21, 20, 23, 255).getRGB();
   public static int intVal16 = new Color(71, 123, 255, 20).getRGB();
   public static int intVal17 = new Color(255, 255, 255, 5).getRGB();
   public static int intVal18 = new Color(255, 255, 255, 20).getRGB();
   public static int intVal19 = new Color(255, 255, 255, 10).getRGB();
   public static int intVal20 = new Color(255, 255, 255, 5).getRGB();
   public static int intVal21 = new Color(255, 255, 255, 20).getRGB();
   public static final int intVal22 = new Color(248, 197, 111, 255).getRGB();
   public static final int intVal23 = new Color(231, 130, 137, 255).getRGB();
   public static int intVal24 = new Color(255, 255, 255, 8).getRGB();
   public static int intVal25 = new Color(255, 255, 255, 20).getRGB();
   public static int intVal26 = new Color(255, 255, 255, 6).getRGB();
   public static int intVal27 = new Color(255, 255, 255, 26).getRGB();
   public static int intVal28 = new Color(255, 255, 255, 178).getRGB();
   public static int intVal29 = new Color(255, 255, 255, 128).getRGB();
   public static int intVal30 = new Color(255, 255, 255, 102).getRGB();
   public static int intVal31 = new Color(255, 255, 255, 38).getRGB();
   public static int intVal32 = new Color(255, 255, 255, 8).getRGB();
   public static int intVal33 = new Color(255, 255, 255, 20).getRGB();
   public static int intVal34 = new Color(102, 144, 255, 255).getRGB();
   public static int intVal35 = new Color(255, 81, 81, 255).getRGB();
   public static final String strVal = "assets/greed/textures/gui/mainmenu/background/loading.png";
   public static final String strVal2 = "assets/greed/textures/gui/mainmenu/background/menu_blur.png";
   public static final String strVal3 = "assets/greed/textures/gui/mainmenu/background/menu.png";
   public static final String strVal4 = "assets/greed/textures/gui/mainmenu/background/welcome.png";
   public static final String strVal5 = "assets/greed/textures/gui/mainmenu/icons/logo.png";
   public static final String strVal6 = "assets/greed/textures/gui/mainmenu/icons/arrow.png";
   public static final String strVal7 = "assets/greed/textures/gui/mainmenu/icons/person.png";
   public static final String strVal8 = "assets/greed/textures/gui/mainmenu/icons/telegram.png";
   public static final String strVal9 = "assets/greed/textures/gui/mainmenu/icons/discord.png";
   public static final String strVal10 = "assets/greed/textures/gui/mainmenu/icons/chevron.png";
   public static final String strVal11 = "assets/greed/textures/gui/mainmenu/icons/exit.png";
   public static final String strVal12 = "assets/greed/textures/gui/mainmenu/icons/settings.png";
   public static final String strVal13 = "assets/greed/textures/gui/mainmenu/icons/accounts.png";
   public static final String strVal14 = "assets/greed/textures/gui/mainmenu/icons/singleplayer.png";
   public static final String strVal15 = "assets/greed/textures/gui/mainmenu/icons/bell.png";
   public static final String strVal16 = "assets/greed/textures/gui/mainmenu/icons/pattern.png";
   public static final String strVal17 = "assets/greed/textures/gui/mainmenu/icons/search.png";
   public static final String strVal18 = "assets/greed/textures/gui/mainmenu/icons/back.png";
   public static final String strVal19 = "assets/greed/textures/gui/mainmenu/icons/dice.png";
   public static final String strVal20 = "assets/greed/textures/gui/mainmenu/icons/plus.png";
   public static final String strVal21 = "assets/greed/textures/gui/mainmenu/icons/user.png";
   public static final String strVal22 = "assets/greed/textures/gui/mainmenu/icons/delete.png";
   public static final String strVal23 = "assets/greed/textures/gui/mainmenu/icons/rename.png";
   public static final String strVal24 = "assets/greed/textures/gui/mainmenu/icons/grass.png";
   public static final String strVal25 = "assets/greed/textures/gui/mainmenu/icons/greed_serif.png";
   public static final String strVal26 = "assets/greed/textures/gui/mainmenu/icons/cos_menu.png";
   public static boolean flag;
   public static final String TEXT_ICONS = "icons";
   public static final String strVal27 = "α";
   public static final String strVal28 = "β";
   public static final String strVal29 = "γ";
   public static final String strVal30 = "δ";
   public static final String strVal31 = "ε";
   public static final String strVal32 = "b";
   public static final String strVal33 = "9";
   public static final String strVal34 = "B";
   public static final String strVal35 = "e";
   public static final String strVal36 = "d";
   public static final String strVal37 = "ζ";
   public static final String strVal38 = "E";
   public static final String strVal39 = "S";
   public static final String strVal40 = "W";
   public static final String strVal41 = "λ";
   public static final String strVal42 = "μ";
   public static final String strVal43 = "θ";
   public static final String strVal44 = "η";
   public static final String strVal45 = "N";
   public static final String strVal46 = "O";
   public static final String strVal47 = "P";
   public static final String strVal48 = "ι";
   public static final String strVal49 = "R";

   public static void processData(
      float x, float y, float width, float height, float radius, int fillColor, int borderColor
   ) {
      Render2D.drawRect(x, y, width, height, radius, fillColor);
      Render2D.drawOutline(x, y, width, height, radius, 0.5F, borderColor);
   }

   public static void processData2(float centerX, float centerY, float arcRadius, float thickness, int arcColor) {
      long nowMillis = System.currentTimeMillis();
      float startAngleDegrees = -((float)(nowMillis % 1100L) / 1100.0F * 360.0F);
      float pulseProgress = (float)(nowMillis % 1800L) / 1800.0F;
      pulseProgress = pulseProgress < 0.5F ? computeFloat2(pulseProgress * 2.0F) : 1.0F - computeFloat2((pulseProgress - 0.5F) * 2.0F);
      float sweepDegrees = 55.0F + 230.0F * pulseProgress;
      Render2D.drawOutline360Data(
         new Outline360DrawData(
               centerX - arcRadius,
               centerY - arcRadius,
               arcRadius * 2.0F,
               arcRadius * 2.0F,
               arcRadius,
               thickness,
               Render2D.applyAlpha(computeInt(intVal, 0.051F)),
               List.of(ArcColorSpan.of(startAngleDegrees, startAngleDegrees + sweepDegrees, Render2D.applyAlpha(arcColor)))
            )
            .withBlendDegrees(Math.min(48.0F, sweepDegrees * 0.42F))
      );
   }

   public static List<String> getList(FontFace fontFace, String text, float fontSize, float maxWidth) {
      List<String> lines = new ArrayList<>();
      StringBuilder stringBuilder = new StringBuilder();

      for (String word : text.split(" ")) {
         String candidateLine = stringBuilder.length() == 0 ? word : stringBuilder + " " + word;
         if (stringBuilder.length() != 0 && !(FontText.computeFloat(fontFace, candidateLine, fontSize) <= maxWidth)) {
            lines.add(stringBuilder.toString());
            stringBuilder = new StringBuilder(word);
         } else {
            stringBuilder = new StringBuilder(candidateLine);
         }
      }

      if (stringBuilder.length() > 0) {
         lines.add(stringBuilder.toString());
      }

      return lines;
   }

   public static float computeFloat(float progress) {
      progress = Math.max(0.0F, Math.min(1.0F, progress));
      return 1.0F + 2.70158F * (progress - 1.0F) * (progress - 1.0F) * (progress - 1.0F) + 1.70158F * (progress - 1.0F) * (progress - 1.0F);
   }

   public static float computeFloat2(float progress2) {
      progress2 = Math.max(0.0F, Math.min(1.0F, progress2));
      return 1.0F - (1.0F - progress2) * (1.0F - progress2);
   }

   public static void processData3(String texturePath, float iconCenterX, float iconCenterY, float iconWidth, float iconHeight, int tintColor) {
      Render2D.drawImageSized(
         texturePath, iconCenterX - iconWidth / 2.0F, iconCenterY - iconHeight / 2.0F, iconWidth, iconHeight, 0.0F, tintColor
      );
   }

   public static int computeInt(int color, float alphaFactor) {
      return Math.round((color >>> 24 & 0xFF) * Math.max(0.0F, Math.min(1.0F, alphaFactor))) << 24 | color & 16777215;
   }

   private UiTheme() {
   }

   public static void processData4(long startTimeMillis, float viewportWidth, float viewportHeight) {
      float scale = Math.min(viewportWidth / 960.0F, viewportHeight / 540.0F);
      float offsetY = (viewportHeight - 540.0F * scale) / 2.0F;
      long frameTimeMillis = System.currentTimeMillis();
      float logoProgress = computeFloat(computeFloat3(startTimeMillis, 0L, 520L));
      float screenCenterX = viewportWidth / 2.0F;
      float logoCenterY = offsetY + 229.0F * scale;
      Render2D.pushAlpha(Math.min(1.0F, logoProgress * 2.0F));
      processData2(screenCenterX, logoCenterY, 28.5F * scale * logoProgress, 2.7F * scale * Math.max(0.001F, logoProgress), intVal7);
      float pulseScale = 1.0F + 0.045F * (float)Math.sin((float)(frameTimeMillis % 1600L) / 1600.0F * Math.PI * 2.0);
      processData3(
         "assets/greed/textures/gui/mainmenu/icons/logo.png",
         screenCenterX,
         logoCenterY,
         15.5F * scale * logoProgress * pulseScale,
         12.0F * scale * logoProgress * pulseScale,
         intVal7
      );
      Render2D.popAlpha();
      float titleProgress = computeFloat2(computeFloat3(startTimeMillis, 140L, 480L));
      float titleWidth = FontText.computeFloat(FontFace.SEMI, "Запускаем Greed...", 15.0F * scale);
      Render2D.pushAlpha(titleProgress);
      FontText.processData3(
         FontFace.SEMI,
         "Запускаем Greed" + ".".repeat(1 + (int)(frameTimeMillis / 450L % 3L)),
         screenCenterX - titleWidth / 2.0F,
         offsetY + 283.0F * scale + (1.0F - titleProgress) * 10.0F * scale,
         15.0F * scale,
         intVal
      );
      Render2D.popAlpha();
      float descriptionProgress = computeFloat2(computeFloat3(startTimeMillis, 240L, 480L));
      Render2D.pushAlpha(descriptionProgress);
      float lineY = offsetY + 313.0F * scale + (1.0F - descriptionProgress) * 10.0F * scale;

      for (String line : getList(
         FontFace.SEMI,
         "Загружаем необходимые компоненты и применяем ваши настройки. Ещё немного - и Greed будет готов к работе.",
         7.5F * scale,
         190.0F * scale
      )) {
         FontText.processData3(
            FontFace.SEMI,
            line,
            screenCenterX - FontText.computeFloat(FontFace.SEMI, line, 7.5F * scale) / 2.0F,
            lineY,
            7.5F * scale,
            intVal29
         );
         lineY += 9.0F * scale;
      }

      Render2D.popAlpha();
   }

   public static boolean matchesCriteria(float pointX, float pointY, float rectX, float rectY, float rectWidth, float rectHeight) {
      return pointX >= rectX && pointX <= rectX + rectWidth && pointY >= rectY && pointY <= rectY + rectHeight;
   }

   public static void processData5(String texture, String label, float textCenterX, float textCenterY, float textSize, int textColor) {
      float[] floatArray = Render2D.measureMsdfText(texture, label, textSize);
      Render2D.drawMsdfText(
         texture,
         label,
         textCenterX - (floatArray[0] + floatArray[2]) / 2.0F,
         textCenterY - (floatArray[1] + floatArray[3]) / 2.0F + 1.0F,
         textSize,
         textColor
      );
   }

   public static int computeInt2(int fromColor, int toColor, float blendFactor) {
      float clampedFactor = Math.max(0.0F, Math.min(1.0F, blendFactor));
      return lerpChannel(fromColor >>> 24 & 0xFF, toColor >>> 24 & 0xFF, clampedFactor) << 24
         | lerpChannel(fromColor >>> 16 & 0xFF, toColor >>> 16 & 0xFF, clampedFactor) << 16
         | lerpChannel(fromColor >>> 8 & 0xFF, toColor >>> 8 & 0xFF, clampedFactor) << 8
         | lerpChannel(fromColor & 0xFF, toColor & 0xFF, clampedFactor);
   }

   public static void processData6(String backgroundTexture, float screenWidth, float screenHeight, float alpha) {
      float coverScale = Math.max(screenWidth / 960.0F, screenHeight / 540.0F);
      Render2D.drawImageSized(
         backgroundTexture,
         (screenWidth - 960.0F * coverScale) / 2.0F,
         (screenHeight - 540.0F * coverScale) / 2.0F,
         960.0F * coverScale,
         540.0F * coverScale,
         0.0F,
         computeInt(intVal, alpha)
      );
   }

   private static int lerpChannel(int fromChannel, int toChannel, float factor) {
      return Math.round(fromChannel + (toChannel - fromChannel) * factor);
   }

   public static void processData7(String iconTexture, float iconX, float iconY, float iconSize, int iconColor) {
      processData5("icons", iconTexture, iconX, iconY, iconSize, iconColor);
   }

   public static float computeFloat3(long animationStartMillis, long delayMillis, long durationMillis) {
      return Math.max(0.0F, Math.min(1.0F, (float)(System.currentTimeMillis() - animationStartMillis - delayMillis) / (float)durationMillis));
   }
}
