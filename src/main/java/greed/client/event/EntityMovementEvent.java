package greed.client.event;

import net.minecraft.util.math.Vec3d;

public final class EntityMovementEvent extends CancellableEvent {
   private Vec3d vec3d2;

   public EntityMovementEvent(Vec3d vec3d) {
      this.vec3d2 = vec3d;
   }

   public Vec3d getVec3d() {
      return this.vec3d2;
   }

   public void processData(Vec3d vec3d) {
      this.vec3d2 = vec3d;
   }
}
