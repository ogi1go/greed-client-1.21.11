package greed.client.ui;

import greed.client.render.FontText;

import greed.client.event.EventBus;
import greed.client.event.EventListener;
import greed.client.render.FontFace;
import greed.client.event.HudRenderEvent;
import greed.client.module.ModuleCategory;
import greed.client.render.Render2D;

public final class UiWarmup {
   private static final UiWarmup uiWarmup = new UiWarmup();
   private static volatile int intVal = 5;
   private static volatile boolean flag;
   private final EventListener<HudRenderEvent> hudRenderListener = hudRenderEvent -> {
      if (intVal > 0) {
         intVal--;
         if (!flag) {
            updateState();
         }

         try {
            this.updateState2();
         } catch (Throwable throwable) {
         }

         try {
            this.updateState4();
         } catch (Throwable throwable2) {
         }

         if (intVal <= 0) {
            EventBus.getInstance().unsubscribe(uiWarmup);
         }
      }
   };

   public static void updateState() {
      String local3048_0 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 .,:;!?-_()[]{}<>/\\|@#$%^&*+=\"'`~АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдеёжзийклмнопрстуфхцчшщъыьэюя";

      for (FontFace fontFace : FontFace.values()) {
         try {
            FontText.computeFloat(
               fontFace,
               fontFace == FontFace.ICONS ? "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789αβγδεζηθικλμνξ" : local3048_0,
               8.0F
            );
         } catch (Throwable throwable) {
         }
      }

      flag = true;
   }

   private void updateState2() {
      Render2D.drawRect(-4000.0F, 0.0F, 2.0F, 2.0F, 1.0F, 33554431);
      Render2D.drawGradientRect(-4000.0F, 0.0F, 2.0F, 2.0F, 1.0F, 33554431, 33554431, 33554431, 33554431);
      Render2D.drawOutline(-4000.0F, 0.0F, 2.0F, 2.0F, 1.0F, 0.5F, 33554431);
      Render2D.drawOutlineArcUniformVarargs(-4000.0F, 0.0F, 2.0F, 2.0F, 1.0F, 0.5F, 33554431);
      Render2D.drawCircle(-4000.0F, 1.0F, 1.0F, 33554431);
      Render2D.drawCircleOutline(-4000.0F, 1.0F, 1.0F, 0.5F, 33554431);
      Render2D.drawCornerBracket(-4000.0F, 0.0F, 2.0F, 1.0F, 0.25F, 2, 33554431);
      Render2D.drawBlob(-4000.0F, 0.0F, 4.0F, 4.0F, 2.0F, 2.0F, 2.0F, 2.0F, 3.0F, 1.0F, 0.0F, 0.0F, 1.0F, 33554431);
      Render2D.drawHueSlider(-4000.0F, 0.0F, 2.0F, 2.0F, 0.003921569F);
      Render2D.drawAlphaSlider(-4000.0F, 0.0F, 2.0F, 2.0F, 33554431, 0.003921569F);
      Render2D.drawBlur(-4000.0F, 0.0F, 2.0F, 2.0F, 1.0F, 8.0F);
      Render2D.drawBlurSmooth(-4000.0F, 0.0F, 2.0F, 2.0F, 1.0F, 8.0F, 1.0F, 33554431);
      Render2D.drawBackdropBlur(-4000.0F, 0.0F, 2.0F, 2.0F, 1.0F);
      Render2D.drawGlassDistorted(-4000.0F, 0.0F, 2.0F, 2.0F, 0.5F, 0.5F, 1.0F, 33554431);
      Render2D.drawGlassOutlineDistorted(-4000.0F, 0.0F, 2.0F, 2.0F, 0.5F, 0.5F, 1.0F, 0.5F, 33554431);
      Render2D.drawSoftGlow(-4000.0F, 0.0F, 2.0F, 2.0F, 1.0F, 33554431, 0.5F);
      Render2D.drawSoftGlowPoint(-4000.0F, 1.0F, 33554431, 0.5F, 0.5F);
      Render2D.drawRipple(-4000.0F, 0.0F, 2.0F, 2.0F, 1.0F, 1.0F, 1.0F, 0.5F, 33554431, 33554431);
      Render2D.drawHalfIconSimple(-4000.0F, 0.0F, 2.0F, 2.0F, 1.0F, 33554431, 33554431, "icons", "a", 1.0F, 2.0F, 2.0F, 0.0F);
      Render2D.drawTintedText(FontFace.SEMI_BOLD.getResourceName(), "Ag", -4000.0F, 0.0F, 6.0F, 0.5F, 0.35F, 0.5F, 0.003921569F);

      for (FontFace fontFace : FontFace.values()) {
         FontText.processData3(fontFace, fontFace == FontFace.ICONS ? "aM" : "Ag", -4000.0F, 0.0F, 6.0F, 33554431);
      }
   }

   private UiWarmup() {
   }

   public static void updateState3() {
      intVal = 5;
      flag = false;
      EventBus.getInstance().unsubscribe(uiWarmup);
      EventBus.getInstance().subscribe(uiWarmup);
   }

   private void updateState4() {
      for (ModuleCategory moduleCategory : ModuleCategory.values()) {
         Render2D.drawImageSized(ClickGuiStyle.formatText(moduleCategory), -4000.0F, 0.0F, 2.0F, 2.0F, 0.0F, 33554431);
      }

      for (String local3054_4 : new String[]{
         "assets/greed/textures/gui/clickgui/search.png",
         "assets/greed/textures/gui/clickgui/chev.png",
         "assets/greed/textures/gui/clickgui/chevright.png",
         "assets/greed/textures/gui/clickgui/check.png",
         "assets/greed/textures/gui/clickgui/picker.png",
         "assets/greed/textures/gui/clickgui/bindkey.png"
      }) {
         Render2D.drawImageSized(local3054_4, -4000.0F, 0.0F, 2.0F, 2.0F, 0.0F, 33554431);
      }
   }

   public static void updateState5() {
      EventBus.getInstance().subscribe(uiWarmup);
   }
}
