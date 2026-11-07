package greed.client.render;

import greed.client.util.GuiScale;

import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.gui.ScreenRect;
import org.joml.Matrix3x2f;

public final class ScissorStack {
   private static final ScreenRect rect = new ScreenRect(0, 0, 0, 0);
   private static final Deque<ScreenRect> deque = new ArrayDeque<>();

   public static void processData(Matrix3x2f matrix3x2f, float x, float y, float width, float height) {
      if (matrix3x2f == null) {
         processData2(x, y, width, height);
      } else {
         ScreenRect screenRect = new ScreenRect(
               Math.round(x), Math.round(y), Math.max(0, Math.round(width)), Math.max(0, Math.round(height))
            )
            .transformEachVertex(matrix3x2f);
         pushRect(screenRect);
      }
   }

   public static void processData2(float x, float y, float width, float height) {
      int minX = GuiScale.computeInt2(x);
      int minY = GuiScale.computeInt2(y);
      int maxX = GuiScale.computeInt2(x + width);
      int maxY = GuiScale.computeInt2(y + height);
      pushRect(new ScreenRect(minX, minY, Math.max(0, maxX - minX), Math.max(0, maxY - minY)));
   }

   public static ScreenRect getScreenRect() {
      return deque.peek();
   }

   private ScissorStack() {
   }

   private static void pushRect(ScreenRect screenRect) {
      ScreenRect screenRect2 = deque.peek();
      ScreenRect screenRect3 = screenRect2 == null ? screenRect : screenRect2.intersection(screenRect);
      deque.push(screenRect3 == null ? rect : screenRect3);
   }

   public static void updateState() {
      deque.clear();
   }

   public static void updateState2() {
      if (!deque.isEmpty()) {
         deque.pop();
      }
   }
}
