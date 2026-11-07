package greed.client.ui;

public final class AnimatedValue {
   private final DirectionalAnimation directionalAnimation = new DirectionalAnimation();
   private double doubleVal;
   private double doubleVal2;

   public void processData(double local6355_1) {
      if (!(Math.abs(local6355_1 - this.doubleVal2) < 1.0E-6)) {
         this.doubleVal = this.computeDouble();
         this.doubleVal2 = local6355_1;
         this.directionalAnimation.nested0525.updateState();
      }
   }

   public AnimatedValue getAnimatedValue(double local6356_1) {
      this.doubleVal = local6356_1;
      this.doubleVal2 = local6356_1;
      this.directionalAnimation.nested0525.updateState();
      return this;
   }

   public double computeDouble() {
      return this.doubleVal + (this.doubleVal2 - this.doubleVal) * this.directionalAnimation.computeDouble();
   }

   public double computeDouble2() {
      return this.doubleVal2;
   }

   public AnimatedValue(long local6359_1) {
      this.directionalAnimation.processData2(local6359_1);
      this.directionalAnimation.processData(1.0);
      this.directionalAnimation.setAnimationDirection(AnimationDirection.IN);
   }
}
