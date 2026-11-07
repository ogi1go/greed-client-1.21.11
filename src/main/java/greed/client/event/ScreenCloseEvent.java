package greed.client.event;

import net.minecraft.client.gui.screen.Screen;

public class ScreenCloseEvent extends CancellableEvent {
   private final Screen screen2;

   public Screen getScreen() {
      return this.screen2;
   }

   public ScreenCloseEvent(Screen screen) {
      this.screen2 = screen;
   }
}
