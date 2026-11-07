package greed.client.event;

import net.minecraft.util.math.BlockPos;

public class WorldSoundEvent extends TickEvent {
   private final int intVal;
   private final BlockPos blockPos2;

   public WorldSoundEvent(int local5150_1, BlockPos blockPos) {
      this.intVal = local5150_1;
      this.blockPos2 = blockPos;
   }

   public int computeInt() {
      return this.intVal;
   }

   public BlockPos getBlockPos() {
      return this.blockPos2;
   }
}
