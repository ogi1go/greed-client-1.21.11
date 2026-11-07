package greed.client.cosmetic;

import java.util.HashMap;
import net.minecraft.client.network.AbstractClientPlayerEntity;

public final class PlayerMovementTracker {
   private static final HashMap<Integer, PlayerMovementTracker> map = new HashMap<>();
   private double previousX;
   private double previousZ;
   private int lastTick = Integer.MIN_VALUE;
   private int stationaryTicks;
   private boolean moving;

   private boolean update(AbstractClientPlayerEntity abstractClientPlayerEntity) {
      int currentLastTick = abstractClientPlayerEntity.age;
      if (currentLastTick == this.lastTick) {
         return this.moving;
      }

      if (currentLastTick >= this.lastTick && currentLastTick - this.lastTick <= 20) {
         double local7514_3 = abstractClientPlayerEntity.getX() - this.previousX;
         double local7514_5 = abstractClientPlayerEntity.getZ() - this.previousZ;
         this.previousX = abstractClientPlayerEntity.getX();
         this.previousZ = abstractClientPlayerEntity.getZ();
         this.lastTick = currentLastTick;
         if (local7514_3 * local7514_3 + local7514_5 * local7514_5 >= 4.0E-4) {
            this.stationaryTicks = 0;
            this.moving = true;
         } else if (++this.stationaryTicks >= 2) {
            this.moving = false;
         }

         return this.moving;
      } else {
         this.previousX = abstractClientPlayerEntity.getX();
         this.previousZ = abstractClientPlayerEntity.getZ();
         this.lastTick = currentLastTick;
         this.stationaryTicks = 0;
         this.moving = false;
         return false;
      }
   }

   public static boolean isMoving(AbstractClientPlayerEntity abstractClientPlayerEntity) {
      if (map.size() > 128) {
         map.clear();
      }

      return map.computeIfAbsent(abstractClientPlayerEntity.getId(), local7513_0 -> new PlayerMovementTracker()).update(abstractClientPlayerEntity);
   }

   private PlayerMovementTracker() {
   }
}
