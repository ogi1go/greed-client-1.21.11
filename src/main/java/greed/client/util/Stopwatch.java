package greed.client.util;


public class Stopwatch {
   private long startedAtMillis;

   public String formatElapsedTime() {
      long local4167_1 = this.elapsedMillis();
      long local4167_3 = local4167_1 / 3600000L;
      long local4167_5 = local4167_1 % 3600000L / 60000L;
      long local4167_7 = local4167_1 % 60000L / 1000L;
      StringBuilder stringBuilder = new StringBuilder();
      if (local4167_3 > 0L) {
         stringBuilder.append(local4167_3).append("ч, ");
      }

      if (local4167_5 > 0L || local4167_3 > 0L) {
         stringBuilder.append(local4167_5).append("м, ");
      }

      stringBuilder.append(local4167_7).append("с");
      return stringBuilder.toString();
   }

   public void reset() {
      this.startedAtMillis = System.currentTimeMillis();
   }

   public boolean hasElapsed(double local4169_1) {
      return System.currentTimeMillis() - local4169_1 >= this.startedAtMillis;
   }

   public boolean hasExceededMillis(long local4170_1) {
      return System.currentTimeMillis() - this.startedAtMillis > local4170_1;
   }

   public long getStartedAtMillis() {
      return this.startedAtMillis;
   }

   public void delayStart(long local4172_1) {
      this.startedAtMillis = System.currentTimeMillis() + local4172_1;
   }

   public Stopwatch() {
      this.reset();
   }

   public long elapsedMillis() {
      return System.currentTimeMillis() - this.startedAtMillis;
   }
}
