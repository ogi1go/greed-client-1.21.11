package greed.client.event;

import greed.client.event.TickEvent;


public class SubTickEvent extends TickEvent {
   private final int index;
   private final int count;
   private final float tickProgress;

   public SubTickEvent(int local1471_1, int local1471_2, float local1471_3) {
      this.index = local1471_1;
      this.count = local1471_2;
      this.tickProgress = local1471_3;
   }

   public int getCount() {
      return this.count;
   }

   public int getIndex() {
      return this.index;
   }

   public float getTickProgress() {
      return this.tickProgress;
   }
}
