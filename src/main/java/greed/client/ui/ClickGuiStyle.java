package greed.client.ui;

import greed.client.render.ThemeColors;

import greed.client.module.ModuleCategory;

import java.awt.Color;

public final class ClickGuiStyle {
   public static int intVal = new Color(71, 123, 255, 255).getRGB();
   public static int intVal2 = new Color(9, 16, 32, 194).getRGB();
   public static int intVal3 = new Color(71, 123, 255, 15).getRGB();
   public static int intVal4 = new Color(255, 255, 255, 128).getRGB();
   public static int intVal5 = new Color(71, 123, 255, 13).getRGB();
   public static int intVal6 = new Color(255, 255, 255, 5).getRGB();
   public static int intVal7 = new Color(255, 255, 255, 89).getRGB();
   public static int intVal8 = new Color(255, 255, 255, 26).getRGB();
   public static int intVal9 = new Color(71, 123, 255, 51).getRGB();
   public static int intVal10 = new Color(255, 255, 255, 77).getRGB();
   public static int intVal11 = new Color(71, 123, 255, 128).getRGB();
   public static int intVal12 = new Color(255, 255, 255, 153).getRGB();
   public static int intVal13 = new Color(255, 255, 255, 51).getRGB();
   public static int intVal14 = new Color(71, 123, 255, 51).getRGB();
   public static int intVal15 = new Color(255, 255, 255, 26).getRGB();
   public static int intVal16 = new Color(255, 255, 255, 51).getRGB();
   public static int intVal17 = new Color(71, 123, 255, 26).getRGB();
   public static int intVal18 = new Color(255, 255, 255, 15).getRGB();
   public static int intVal19 = new Color(255, 255, 255, 20).getRGB();
   public static int intVal20 = new Color(71, 123, 255, 26).getRGB();
   public static int intVal21 = new Color(71, 123, 255, 41).getRGB();
   public static int intVal22 = new Color(255, 255, 255, 38).getRGB();
   public static int intVal23 = new Color(255, 255, 255, 38).getRGB();
   public static int intVal24 = new Color(255, 255, 255, 20).getRGB();
   public static int intVal25 = new Color(255, 255, 255, 15).getRGB();
   public static int intVal26 = new Color(255, 255, 255, 5).getRGB();
   public static int intVal27 = new Color(255, 255, 255, 153).getRGB();
   public static int intVal28 = new Color(255, 255, 255, 51).getRGB();
   public static int intVal29 = new Color(255, 255, 255, 26).getRGB();
   public static int intVal30 = new Color(255, 255, 255, 153).getRGB();
   public static int intVal31 = new Color(71, 123, 255, 26).getRGB();
   public static int intVal32 = new Color(255, 255, 255, 5).getRGB();
   public static int intVal33 = new Color(255, 255, 255, 5).getRGB();
   public static int intVal34 = new Color(255, 255, 255, 38).getRGB();
   public static int intVal35 = new Color(255, 255, 255, 20).getRGB();
   public static int intVal36 = new Color(255, 255, 255, 15).getRGB();
   public static int intVal37 = new Color(255, 255, 255, 5).getRGB();
   public static int intVal38 = new Color(0, 0, 0, 33).getRGB();
   public static int intVal39 = new Color(0, 0, 0, 26).getRGB();
   public static int intVal40 = new Color(188, 188, 188, 255).getRGB();
   public static int intVal41 = new Color(15, 14, 17, 96).getRGB();
   public static int intVal42 = new Color(15, 14, 17, 245).getRGB();
   public static int intVal43 = new Color(15, 14, 17, 80).getRGB();
   public static int intVal44 = new Color(255, 255, 255, 3).getRGB();
   public static int intVal45 = new Color(15, 14, 17, 110).getRGB();
   public static int intVal46 = new Color(16, 15, 19, 204).getRGB();
   public static int intVal47 = new Color(255, 255, 255, 3).getRGB();
   public static int intVal48 = new Color(255, 255, 255, 5).getRGB();
   public static int intVal49 = new Color(71, 123, 255, 20).getRGB();
   public static final String strVal = "assets/greed/textures/gui/clickgui/search.png";
   public static final String strVal2 = "assets/greed/textures/gui/clickgui/chev.png";
   public static final String strVal3 = "assets/greed/textures/gui/clickgui/chevright.png";
   public static final String strVal4 = "assets/greed/textures/gui/clickgui/check.png";
   public static final String strVal5 = "assets/greed/textures/gui/clickgui/picker.png";
   public static final String strVal6 = "assets/greed/textures/gui/clickgui/bindkey.png";

   private ClickGuiStyle() {
   }

   public static String formatText(ModuleCategory moduleCategory) {
      return switch (moduleCategory) {
         case COMBAT -> "assets/greed/textures/gui/clickgui/cat_combat.png";
         case MOVEMENT -> "assets/greed/textures/gui/clickgui/cat_movement.png";
         case RENDER -> "assets/greed/textures/gui/clickgui/cat_visuals.png";
         case PLAYER -> "assets/greed/textures/gui/clickgui/cat_player.png";
         case MISC -> "assets/greed/textures/gui/clickgui/cat_misc.png";
      };
   }

   public static void processData(int local5147_0) {
      int local5147_1 = 0xFF000000 | local5147_0 & 16777215;
      intVal = local5147_1;
      intVal2 = computeInt(UiTheme.computeInt2(-16777216, local5147_1, 0.126F), 194);
      intVal3 = computeInt(local5147_1, 15);
      intVal5 = computeInt(local5147_1, 13);
      intVal9 = computeInt(local5147_1, 51);
      intVal11 = computeInt(local5147_1, 128);
      intVal14 = computeInt(local5147_1, 51);
      intVal17 = computeInt(local5147_1, 26);
      intVal20 = computeInt(local5147_1, 26);
      intVal21 = computeInt(local5147_1, 41);
      intVal31 = computeInt(local5147_1, 26);
      intVal49 = computeInt(local5147_1, 20);
      UiTheme.intVal7 = local5147_1;
      UiTheme.intVal9 = UiTheme.computeInt2(local5147_1, -1, 0.14F);
      UiTheme.intVal8 = UiTheme.computeInt2(local5147_1, -1, 0.28F);
      UiTheme.intVal34 = UiTheme.computeInt2(local5147_1, -1, 0.17F);
      UiTheme.intVal16 = computeInt(local5147_1, 20);
      ThemeColors.intVal9 = local5147_1;
   }

   private static int computeInt(int local5148_0, int local5148_1) {
      return local5148_1 << 24 | local5148_0 & 16777215;
   }
}
