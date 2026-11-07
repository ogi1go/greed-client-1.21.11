package greed.client.event;

import greed.client.event.EventPhaseAccess;


public abstract class Event implements EventPhaseAccess {
   private Event.Stage stage;

   protected boolean canEqual(Object local3034_1) {
      return local3034_1 instanceof Event;
   }

   @Override
   public boolean isPost() {
      return this.stage == Event.Stage.POST;
   }

   @Override
   public boolean isPre() {
      return this.stage == Event.Stage.PRE;
   }

   @Override
   public String toString() {
      return "EventBase(stage=" + this.getStage() + ")";
   }

   public void setStage(Event.Stage local3039_1) {
      this.stage = local3039_1;
   }

   public Event.Stage getStage() {
      return this.stage;
   }

   @Override
   public int hashCode() {
      int local3041_1 = 59;
      int local3041_2 = 1;
      Object local3041_3 = this.getStage();
      return local3041_2 * 59 + (local3041_3 == null ? 43 : local3041_3.hashCode());
   }

   @Override
   public boolean equals(Object local3042_1) {
      if (local3042_1 == this) {
         return true;
      } else if (!(local3042_1 instanceof Event event)) {
         return false;
      } else {
         if (!event.canEqual(this)) {
            return false;
         }

         Object local3042_3 = this.getStage();
         Object local3042_4 = event.getStage();
         return local3042_3 == null ? local3042_4 == null : local3042_3.equals(local3042_4);
      }
   }

   public enum Stage {
      PRE,
      POST;
   }
}
