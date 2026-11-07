package greed.client.module.movement;

import greed.client.event.EntityMovementEvent;
import greed.client.event.EventListener;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.event.PacketEvent;

import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(name = "AirStuck", description = "Зависает в воздухе.", category = ModuleCategory.MOVEMENT)
public final class AirStuckModule extends Module {
   private static AirStuckModule airStuckModule;
   private Vec3d vec3d = Vec3d.ZERO;
   private boolean flag;
   private final EventListener<EntityMovementEvent> entityMovementListener = entityMovementEvent -> {
      if (mc.player != null) {
         if (!this.flag) {
            boolean local4386_2 = (mc.player.getVelocity().y < -0.05 || entityMovementEvent.getVec3d().y < -0.05) && !mc.player.isOnGround();
            if (local4386_2 && mc.player.fallDistance > 0.0) {
               this.vec3d = mc.player.getEntityPos();
               this.flag = true;
            }
         }

         if (this.flag) {
            entityMovementEvent.processData(Vec3d.ZERO);
            mc.player.setPosition(this.vec3d.x, this.vec3d.y, this.vec3d.z);
            mc.player.setVelocity(Vec3d.ZERO);
         }
      }
   };
   private final EventListener<PacketEvent> packetListener = packetEvent -> {
      if (this.flag && packetEvent.isOutgoing() && packetEvent.getPacket() instanceof PlayerMoveC2SPacket) {
         packetEvent.cancel();
      }
   };

   public static AirStuckModule getInstance() {
      if (airStuckModule == null) {
         airStuckModule = new AirStuckModule();
      }

      return airStuckModule;
   }

   public boolean isFrozenInAir() {
      return this.isEnabled() && this.flag;
   }

   @Override
   protected void onDisable() {
      this.flag = false;
   }

   @Override
   protected void onEnable() {
      this.flag = false;
   }

   private AirStuckModule() {
   }
}
