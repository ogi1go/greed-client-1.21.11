package greed.client.event;

import net.minecraft.client.gui.DrawContext;

public class HudRenderEvent extends TickEvent {
   private final DrawContext drawContext2;
   private final float floatVal;
   private final int intVal;
   private final int intVal2;

   public DrawContext getDrawContext() {
      return this.drawContext2;
   }

   public HudRenderEvent(DrawContext drawContext, float local7536_2, int local7536_3, int local7536_4) {
      this.drawContext2 = drawContext;
      this.floatVal = local7536_2;
      this.intVal = local7536_3;
      this.intVal2 = local7536_4;
   }

   public int computeInt() {
      return this.intVal2;
   }

   public float computeFloat() {
      return this.floatVal;
   }

   public int computeInt2() {
      return this.intVal;
   }
}
