package greed.client.ui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;

public final class ScreenClosingTransition {
   private static final List<ScreenClosingTransition.SubInterface> list = new ArrayList<>();

   private ScreenClosingTransition() {
   }

   public static boolean checkState() {
      return list.isEmpty();
   }

   public static void processData(ScreenClosingTransition.SubInterface local2137_0) {
      if (local2137_0 != null && !list.contains(local2137_0)) {
         list.add(local2137_0);
      }
   }

   public static void renderElement(DrawContext drawContext) {
      if (!list.isEmpty()) {
         for (ScreenClosingTransition.SubInterface local2138_2 : new ArrayList<>(list)) {
            if (!local2138_2.renderClosing(drawContext)) {
               list.remove(local2138_2);
            }
         }
      }
   }

   public static void processData2(Class<?> local2139_0) {
      list.removeIf(local2139_0::isInstance);
   }

   public static void processData3(ScreenClosingTransition.SubInterface local2140_0) {
      list.remove(local2140_0);
   }

   public interface SubInterface {
      boolean renderClosing(DrawContext drawContext);
   }
}
