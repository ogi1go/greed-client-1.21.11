package greed.client.event;


public final class MouseLookEvent extends CancellableEvent {
   private double doubleVal;
   private double doubleVal2;
   private double doubleVal3;
   private boolean flag;

   public MouseLookEvent(double local6709_1, double local6709_3, double local6709_5, boolean local6709_7) {
      this.doubleVal = local6709_1;
      this.doubleVal2 = local6709_3;
      this.doubleVal3 = local6709_5;
      this.flag = local6709_7;
   }

   public double computeDouble() {
      return this.doubleVal3;
   }

   public void processData(double local6711_1) {
      this.doubleVal3 = local6711_1;
   }

   public double computeDouble2() {
      return this.doubleVal;
   }

   public void processData2(double local6713_1) {
      this.doubleVal = local6713_1;
   }

   public void processData3(double local6714_1) {
      this.doubleVal2 = local6714_1;
   }

   public void processData4(boolean local6715_1) {
      this.flag = local6715_1;
   }

   public boolean checkState() {
      return this.flag;
   }

   public double computeDouble3() {
      return this.doubleVal2;
   }
}
