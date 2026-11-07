package greed.client.event;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;

public class ContainerRenderEvent extends TickEvent {
   private final HandledScreen<?> handledScreen;
   private final DrawContext drawContext2;

   public HandledScreen<?> getHandledScreen() {
      return this.handledScreen;
   }

   public ContainerRenderEvent(HandledScreen<?> local5188_1, DrawContext drawContext) {
      this.handledScreen = local5188_1;
      this.drawContext2 = drawContext;
   }

   public DrawContext getDrawContext() {
      return this.drawContext2;
   }
}
