package greed.client.event;


public class CancellableEvent extends Event {
   private boolean flag;

   public boolean checkState() {
      return this.flag;
   }

   public void updateState() {
      this.flag = true;
   }

   public void cancel() {
      this.flag = true;
   }

   public boolean isCancelled() {
      return this.flag;
   }

   public void processData(boolean local7442_1) {
      this.flag = local7442_1;
   }
}
