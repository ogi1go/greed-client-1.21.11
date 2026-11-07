package greed.client.util;

import greed.client.util.EasingFunction;
import greed.client.util.Easings;

public class Animation {
   private long startedAtMillis;
   private boolean running;
   private double durationMillis;
   private double startValue;
   private double targetValue;
   private double value;
   private double previousValue;
   private EasingFunction easingFunction = Easings.LINEAR;

   public Animation animateTo(double newTargetValue, double local4480_3, EasingFunction newEasingFunction) {
      if (this.alreadyAtTarget(newTargetValue)) {
         return this;
      }

      this.easingFunction = newEasingFunction;
      this.durationMillis = local4480_3 * 1000.0;
      this.startedAtMillis = System.currentTimeMillis();
      this.startValue = this.value;
      this.targetValue = newTargetValue;
      this.running = true;
      return this;
   }

   public Animation animateToLinear(double newTargetValue, double local4481_3) {
      return this.animateTo(newTargetValue, local4481_3, Easings.LINEAR);
   }

   public boolean isInProgress() {
      return !this.isFinished();
   }

   public float floatValue() {
      return (float)this.value;
   }

   private boolean alreadyAtTarget(double local4484_1) {
      return this.running ? this.nearlyEqual(local4484_1, this.targetValue) : this.nearlyEqual(local4484_1, this.value);
   }

   public double getProgress() {
      return this.running && !(this.durationMillis <= 0.0) ? (System.currentTimeMillis() - this.startedAtMillis) / this.durationMillis : 1.0;
   }

   public float previousFloatValue() {
      return (float)this.previousValue;
   }

   public double lerp(double local4488_1, double local4488_3, double local4488_5) {
      return local4488_1 + (local4488_3 - local4488_1) * local4488_5;
   }

   public boolean isFinished() {
      return !this.running || this.getProgress() >= 1.0;
   }

   public void resetTo(double newValue) {
      this.running = false;
      this.startedAtMillis = 0L;
      this.value = newValue;
      this.targetValue = newValue;
   }

   private boolean nearlyEqual(double local4491_1, double local4491_3) {
      return Math.abs(local4491_1 - local4491_3) <= 1.0E-6;
   }

   public boolean update() {
      this.previousValue = this.value;
      if (!this.running) {
         return false;
      }

      boolean isInProgress = this.isInProgress();
      if (isInProgress) {
         this.value = this.lerp(this.startValue, this.targetValue, this.easingFunction.apply(this.getProgress()));
      } else {
         this.startedAtMillis = 0L;
         this.running = false;
         this.value = this.targetValue;
      }

      return isInProgress;
   }
}
