package greed.client.ui;

import greed.client.util.GuiScale;

public final class SmoothScroll {
   public float targetOffset;
   public float currentOffset;
   public float maximumOffset;

   public float getPixelAlignedOffset() {
      float local7435_1 = GuiScale.computeFloat() / GuiScale.computeInt();
      return local7435_1 > 1.0E-4F ? Math.round(this.currentOffset * local7435_1) / local7435_1 : this.currentOffset;
   }

   public void update(float local7437_1) {
      float local7437_2 = local7437_1;
      float currentTargetOffset = this.targetOffset;
      float currentMaximumOffset = this.maximumOffset;
      this.targetOffset = Math.max(0.0F, Math.min(currentTargetOffset, currentMaximumOffset));
      this.currentOffset = this.currentOffset + (this.targetOffset - this.currentOffset) * (1.0F - (float)Math.exp(-local7437_2 * 22.0F));
      if (Math.abs(this.targetOffset - this.currentOffset) < 0.1F) {
         this.currentOffset = this.targetOffset;
      }
   }
}
