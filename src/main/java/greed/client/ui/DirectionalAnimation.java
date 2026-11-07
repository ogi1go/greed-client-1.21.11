package greed.client.ui;

public class DirectionalAnimation {
   public final DirectionalAnimation.SubContext nested0525 = new DirectionalAnimation.SubContext();
   private long timeMillis = 200L;
   private double doubleVal = 1.0;
   private AnimationDirection animationDirection = AnimationDirection.IN;

   public boolean checkState() {
      return this.nested0525.computeLong() >= this.timeMillis;
   }

   public void processData(double local7542_1) {
      this.doubleVal = local7542_1;
   }

   public void processData2(long local7543_1) {
      this.timeMillis = local7543_1;
   }

   public void setAnimationDirection(AnimationDirection newAnimationDirection) {
      this.animationDirection = newAnimationDirection;
   }

   public double computeDouble() {
      double local7545_1 = this.nested0525.computeLong();
      double local7545_3 = this.timeMillis > 0L ? Math.min(local7545_1 / this.timeMillis, 1.0) : 1.0;
      double local7545_5 = 1.0 - (1.0 - local7545_3) * (1.0 - local7545_3);
      return this.animationDirection == AnimationDirection.OUT ? this.doubleVal * (1.0 - local7545_5) : this.doubleVal * local7545_5;
   }

   public AnimationDirection getAnimationDirection() {
      return this.animationDirection;
   }

   public static class SubContext {
      private long timeMillis2 = System.nanoTime();

      public long computeLong() {
         return (System.nanoTime() - this.timeMillis2) / 1000000L;
      }

      public void processData3(long local7549_1) {
         this.timeMillis2 = local7549_1;
      }

      public void updateState() {
         this.timeMillis2 = System.nanoTime();
      }
   }
}
