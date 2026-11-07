package greed.client.event;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;

public final class HeldItemRenderEvent extends TickEvent {
   private final MatrixStack matrixStack2;
   private final ItemStack itemStack2;
   private final Hand hand2;
   private final Arm arm2;
   private final float floatVal;

   public float computeFloat() {
      return this.floatVal;
   }

   public Arm getArm() {
      return this.arm2;
   }

   public MatrixStack getMatrixStack() {
      return this.matrixStack2;
   }

   public Hand getHand() {
      return this.hand2;
   }

   public ItemStack getItemStack() {
      return this.itemStack2;
   }

   public HeldItemRenderEvent(MatrixStack matrixStack, ItemStack itemStack, Hand hand, Arm arm, float local1180_5) {
      this.matrixStack2 = matrixStack;
      this.itemStack2 = itemStack;
      this.hand2 = hand;
      this.arm2 = arm;
      this.floatVal = local1180_5;
   }
}
