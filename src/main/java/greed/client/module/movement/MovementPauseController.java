package greed.client.module.movement;

import greed.client.event.EventBus;
import greed.client.event.EventListener;
import greed.client.event.EventPriority;
import greed.client.MinecraftAccess;
import greed.client.event.MovementInputEvent;
import greed.client.event.PlayerTickEvent;

public class MovementPauseController implements MinecraftAccess {
   private static MovementPauseController movementPauseController;
   private int remainingTicks = -1;
   @EventPriority(Integer.MIN_VALUE)
   private final EventListener<PlayerTickEvent> playerTickListener = playerTickEvent -> {
      if (this.remainingTicks > 0) {
         this.remainingTicks--;
      } else if (this.remainingTicks == 0) {
         this.remainingTicks = -1;
      }
   };
   @EventPriority(Integer.MIN_VALUE)
   private final EventListener<MovementInputEvent> movementInputListener = movementInputEvent -> {
      if (this.remainingTicks >= 0) {
         movementInputEvent.setForward(0.0F);
         movementInputEvent.setSideways(0.0F);
         movementInputEvent.setJumping(false);
      }
   };

   public boolean isPaused() {
      return this.remainingTicks != -1;
   }

   private MovementPauseController() {
   }

   public void pauseForTicks(int newRemainingTicks) {
      this.remainingTicks = newRemainingTicks;
   }

   public static MovementPauseController getInstance() {
      if (movementPauseController == null) {
         movementPauseController = new MovementPauseController();
         EventBus.getInstance().subscribe(movementPauseController);
      }

      return movementPauseController;
   }

   public int getRemainingTicks() {
      return this.remainingTicks;
   }
}
