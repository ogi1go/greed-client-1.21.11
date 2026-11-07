package greed.client.event;

import net.minecraft.util.PlayerInput;

public final class MovementInputEvent extends CancellableEvent {
   private PlayerInput playerInput2;
   private float floatVal;
   private float floatVal2;
   private boolean flag;
   private boolean flag2;

   public MovementInputEvent(PlayerInput playerInput, float local7487_2, float local7487_3, boolean local7487_4, boolean local7487_5) {
      this.playerInput2 = playerInput;
      this.floatVal = local7487_2;
      this.floatVal2 = local7487_3;
      this.flag = local7487_4;
      this.flag2 = local7487_5;
   }

   public boolean checkState() {
      return this.flag2;
   }

   public float getForward() {
      return this.floatVal;
   }

   public void setJumping(boolean local7490_1) {
      this.flag = local7490_1;
   }

   public PlayerInput getPlayerInput() {
      return this.playerInput2;
   }

   public void processData(PlayerInput playerInput) {
      this.playerInput2 = playerInput;
   }

   public boolean checkState2() {
      return this.flag;
   }

   public void setForward(float local7494_1) {
      this.floatVal = local7494_1;
   }

   public float getSideways() {
      return this.floatVal2;
   }

   public void setSneaking(boolean local7496_1) {
      this.flag2 = local7496_1;
   }

   public void setSideways(float local7497_1) {
      this.floatVal2 = local7497_1;
   }
}
