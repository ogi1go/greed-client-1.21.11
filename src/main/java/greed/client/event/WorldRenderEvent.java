package greed.client.event;

import net.minecraft.client.util.math.MatrixStack;

public final class WorldRenderEvent extends TickEvent {
   private final MatrixStack matrixStack2;
   private final float floatVal;

   public float computeFloat() {
      return this.floatVal;
   }

   public WorldRenderEvent(MatrixStack matrixStack, float local825_2) {
      this.matrixStack2 = matrixStack;
      this.floatVal = local825_2;
   }

   public MatrixStack getMatrixStack() {
      return this.matrixStack2;
   }
}
