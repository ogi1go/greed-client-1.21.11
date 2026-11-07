package greed.client.ui.hud;

import greed.client.render.CornerRadii;
import greed.client.render.FontText;
import greed.client.render.ShapeDrawing;
import greed.client.render.ThemeColors;
import greed.client.ui.UiTheme;

import greed.client.render.FontFace;

public final class HudStyle {
   public static final float floatVal = 13.0F;
   public static final float floatVal2 = 2.0F;
   public static final float floatVal3 = 11.0F;
   public static final float floatVal4 = 13.0F;
   public static final float floatVal5 = 4.5F;
   private static final CornerRadii cornerRadii2 = CornerRadii.uniform(4.5F);
   private static final CornerRadii cornerRadii3 = CornerRadii.uniform(0.25F);

   public static float computeFloat(float local8003_0, float local8003_1) {
      float local8003_2 = local8003_0 > 0.0F && local8003_1 > 0.0F ? local8003_0 + 4.0F + local8003_1 : local8003_0 + local8003_1;
      return local8003_2 > 0.0F ? 2.0F + local8003_2 + 4.0F : 0.0F;
   }

   public static void processData(float local8004_0, float local8004_1, float local8004_2, float local8004_3) {
      ShapeDrawing.processData9(local8004_0, local8004_1 - local8004_3 / 2.0F, 0.5F, local8004_3, computeInt2(1.0F), local8004_2 * 0.3F, cornerRadii3);
   }

   private HudStyle() {
   }

   public static int computeInt(float local8007_0) {
      return UiTheme.computeInt(ThemeColors.intVal9, local8007_0);
   }

   public static void processData2(
      float local8008_0,
      float local8008_1,
      float local8008_2,
      float local8008_3,
      float local8008_4,
      float topLeft,
      ShapeDrawing.TypeOption local8008_6
   ) {
      ShapeDrawing.processData7(
         local8008_6,
         true,
         local8008_0,
         local8008_1,
         local8008_2,
         local8008_3,
         local8008_4,
         CornerRadii.uniform(topLeft),
         computeInt3(),
         0.65F,
         UiTheme.computeInt2(-16777216, ThemeColors.intVal9, 0.1F)
      );
   }

   private static void processData3(float local8009_0, float local8009_1, float local8009_2, float local8009_3, float local8009_4, CornerRadii cornerRadii) {
      ShapeDrawing.processData4(local8009_0, local8009_1, local8009_2, local8009_3, local8009_4, cornerRadii, ShapeDrawing.TypeOption.LOW, true);
      ShapeDrawing.processData9(local8009_0, local8009_1, local8009_2, local8009_3, computeInt3(), local8009_4 * 0.185F, cornerRadii);
   }

   public static float computeFloat2(float local8010_0) {
      float local8010_1 = local8010_0 * local8010_0;
      float local8010_2 = local8010_1 >= 1.0F ? 1.0F : 1.0F - (float)Math.pow(2.0, -10.0 * local8010_1);
      return 7.0F * (1.0F - local8010_2);
   }

   public static int computeInt2(float local8011_0) {
      return UiTheme.computeInt(UiTheme.computeInt2(ThemeColors.intVal9, -1, 0.68F), local8011_0);
   }

   public static float computeFloat3(float local8012_0) {
      return local8012_0 + 5.5F - 3.5F;
   }

   public static void processData4(float local8013_0, float local8013_1, float local8013_2, float local8013_3, float local8013_4, float topLeft) {
      processData2(local8013_0, local8013_1, local8013_2, local8013_3, local8013_4, topLeft, ShapeDrawing.TypeOption.MIDDLE);
   }

   public static int computeInt3() {
      return UiTheme.computeInt2(ThemeColors.intVal9, -16777216, 0.9F);
   }

   public static void processData5(float local8015_0, float local8015_1, float local8015_2, float local8015_3, float local8015_4, float topLeft) {
      processData3(local8015_0, local8015_1, local8015_2, local8015_3, local8015_4, CornerRadii.uniform(topLeft));
   }

   public static int computeInt4(float local8016_0) {
      return UiTheme.computeInt(UiTheme.computeInt2(ThemeColors.intVal9, -1, 0.92F), local8016_0);
   }

   public static void processData6(float local8017_0, float local8017_1, float local8017_2, float local8017_3, float local8017_4) {
      processData3(local8017_0, local8017_1, local8017_2, local8017_3, local8017_4, cornerRadii2);
   }

   public static float computeFloat4(float local8018_0) {
      return local8018_0 > 0.0F ? 4.0F + local8018_0 + 4.0F : 0.0F;
   }

   public static float computeFloat5(String local8019_0) {
      return 16.0F + FontText.computeFloat(FontFace.SEMI_BOLD, local8019_0, 6.0F) + 4.0F;
   }

   public static void processData7(float local8020_0, float local8020_1, float local8020_2, float local8020_3, HudIcons hudIcons, String local8020_5) {
      processData2(local8020_0, local8020_1, local8020_2, 13.0F, local8020_3, 4.5F, ShapeDrawing.TypeOption.LOW);
      hudIcons.processData(local8020_0 + 4.0F, local8020_1 + (hudIcons.strVal != null ? 2.0F : 2.5F), 8.0F, computeInt(local8020_3));
      FontText.processData3(FontFace.SEMI_BOLD, local8020_5, local8020_0 + 16.0F, local8020_1 + 6.5F - 3.65F, 6.0F, computeInt4(local8020_3));
   }
}
