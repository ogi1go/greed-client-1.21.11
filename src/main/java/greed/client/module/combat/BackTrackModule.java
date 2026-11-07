package greed.client.module.combat;

import greed.client.setting.BooleanSetting;
import greed.client.util.Colors;
import greed.mixin.EntityS2CPacketMixin;
import greed.client.event.EventListener;
import greed.client.setting.LegacyNumberSetting;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.event.PacketEvent;
import greed.client.event.PlayerTickEvent;
import greed.client.render.ThemeColors;
import greed.client.util.Stopwatch;
import greed.client.event.WorldRenderEvent;

import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionSyncS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;
import org.joml.Matrix4f;

@ModuleInfo(name = "BackTrack", description = "Замораживает цель на прошлой позиции, реальную показывает боксом.", category = ModuleCategory.COMBAT)
public final class BackTrackModule extends Module {
   private static BackTrackModule backTrackModule;
   public LegacyNumberSetting delay;
   public LegacyNumberSetting maxSeparation;
   public BooleanSetting booleanSetting;
   private volatile int trackedTargetId = -1;
   private volatile Vec3d realTargetPos;
   private final CopyOnWriteArrayList<BackTrackModule.DelayedPacket> delayedPackets = new CopyOnWriteArrayList<>();
   private final Stopwatch stopwatch = new Stopwatch();
   private Box renderBoundingBox;
   private float renderFade;
   private long lastTrackMillis;
   private final EventListener<PacketEvent> backTrackPacketListener = packetEvent -> {
      if (!packetEvent.isOutgoing()) {
         if (packetEvent.getPacket() instanceof PlayerPositionLookS2CPacket) {
            this.trackedTargetId = -1;
            this.realTargetPos = null;
         } else if (this.trackedTargetId != -1) {
            Packet<?> packet = packetEvent.getPacket();
            if (packet instanceof EntityS2CPacket entityS2CPacket && ((EntityS2CPacketMixin)entityS2CPacket).entityId() == this.trackedTargetId) {
               if (entityS2CPacket.isPositionChanged() && this.realTargetPos != null) {
                  this.realTargetPos = this.realTargetPos.add(entityS2CPacket.getDeltaX() / 4096.0, entityS2CPacket.getDeltaY() / 4096.0, entityS2CPacket.getDeltaZ() / 4096.0);
               }
            } else if (packet instanceof EntityPositionSyncS2CPacket entityPositionSyncS2CPacket && entityPositionSyncS2CPacket.id() == this.trackedTargetId) {
               this.realTargetPos = entityPositionSyncS2CPacket.values().position();
            } else if (packet instanceof EntityPositionS2CPacket entityPositionS2CPacket && entityPositionS2CPacket.entityId() == this.trackedTargetId) {
               if (entityPositionS2CPacket.relatives().isEmpty()) {
                  this.realTargetPos = entityPositionS2CPacket.change().position();
               }
            } else if (!(packet instanceof EntityVelocityUpdateS2CPacket entityVelocityUpdateS2CPacket) || entityVelocityUpdateS2CPacket.getEntityId() != this.trackedTargetId) {
               return;
            }

            this.delayedPackets.add(new BackTrackModule.DelayedPacket(packet, System.currentTimeMillis()));
            packetEvent.cancel();
         }
      }
   };
   private final EventListener<PlayerTickEvent> backTrackTickListener = playerTickEvent -> {
      Entity entity = this.trackedTargetId != -1 && mc.world != null ? mc.world.getEntityById(this.trackedTargetId) : null;
      boolean local4551_3 = entity != null
         && this.realTargetPos != null
         && mc.player != null
         && this.realTargetPos.squaredDistanceTo(mc.player.getEntityPos())
            < entity.getEntityPos().squaredDistanceTo(mc.player.getEntityPos());
      this.processData(entity != null && !local4551_3 ? (long)this.delay.getValue() : 0L);
      if (this.trackedTargetId != -1) {
         if (entity == null
            || !entity.isAlive()
            || mc.player == null
            || this.stopwatch.hasExceededMillis(1500L)
            || this.realTargetPos != null && this.realTargetPos.distanceTo(entity.getEntityPos()) > this.maxSeparation.getValue()) {
            this.updateState();
         }
      }
   };
   private final EventListener<WorldRenderEvent> backTrackRenderListener = worldRenderEvent -> {
      if (mc.player != null && mc.world != null && this.booleanSetting.isChecked()) {
         float local4552_2 = Math.max(1.0F, Math.min((float)(System.currentTimeMillis() - this.lastTrackMillis), 100.0F)) / 16.666666F;
         this.lastTrackMillis = System.currentTimeMillis();
         Entity entity2 = this.trackedTargetId == -1 ? null : mc.world.getEntityById(this.trackedTargetId);
         Vec3d vec3d = this.realTargetPos;
         Box box = entity2 != null && vec3d != null && !(vec3d.distanceTo(entity2.getEntityPos()) < 0.3)
            ? entity2.getBoundingBox().offset(vec3d.subtract(entity2.getEntityPos()))
            : null;
         if (box != null) {
            this.renderBoundingBox = this.renderBoundingBox != null && !(this.renderFade < 0.003921569F)
               ? new Box(
                  MathHelper.lerp(Math.min(1.0F, 0.45F * local4552_2), this.renderBoundingBox.minX, box.minX),
                  MathHelper.lerp(Math.min(1.0F, 0.45F * local4552_2), this.renderBoundingBox.minY, box.minY),
                  MathHelper.lerp(Math.min(1.0F, 0.45F * local4552_2), this.renderBoundingBox.minZ, box.minZ),
                  MathHelper.lerp(Math.min(1.0F, 0.45F * local4552_2), this.renderBoundingBox.maxX, box.maxX),
                  MathHelper.lerp(Math.min(1.0F, 0.45F * local4552_2), this.renderBoundingBox.maxY, box.maxY),
                  MathHelper.lerp(Math.min(1.0F, 0.45F * local4552_2), this.renderBoundingBox.maxZ, box.maxZ)
               )
               : box;
         }

         this.renderFade = MathHelper.clamp(this.renderFade + (box == null ? -0.25F : 0.25F) * local4552_2, 0.0F, 1.0F);
         if (this.renderBoundingBox != null && !(this.renderFade < 0.003921569F)) {
            worldRenderEvent.getMatrixStack().push();
            worldRenderEvent.getMatrixStack()
               .translate(
                  this.renderBoundingBox.minX - mc.gameRenderer.getCamera().getCameraPos().x,
                  this.renderBoundingBox.minY - mc.gameRenderer.getCamera().getCameraPos().y,
                  this.renderBoundingBox.minZ - mc.gameRenderer.getCamera().getCameraPos().z
               );
            Matrix4f positionMatrix = worldRenderEvent.getMatrixStack().peek().getPositionMatrix();
            VertexConsumer vertexConsumer = mc.getBufferBuilders().getEntityVertexConsumers().getBuffer(RenderLayers.debugQuads());
            float[] floatArray = new float[]{0.0F, (float)(this.renderBoundingBox.maxX - this.renderBoundingBox.minX)};
            float[] floatArray2 = new float[]{0.0F, (float)(this.renderBoundingBox.maxY - this.renderBoundingBox.minY)};
            float[] floatArray3 = new float[]{0.0F, (float)(this.renderBoundingBox.maxZ - this.renderBoundingBox.minZ)};
            int[][] intArrayArray = new int[][]{{0, 1, 3, 2}, {4, 6, 7, 5}, {0, 4, 5, 1}, {2, 3, 7, 6}, {0, 2, 6, 4}, {1, 5, 7, 3}};

            for (int[] intArray : intArrayArray) {
               for (int local4552_19 : intArray) {
                  vertexConsumer.vertex(positionMatrix, floatArray[local4552_19 >> 2], floatArray2[local4552_19 >> 1 & 1], floatArray3[local4552_19 & 1])
                     .color(Colors.computeInt6(ThemeColors.intVal9, 0.14F * this.renderFade));
               }
            }

            VertexRendering.drawOutline(
               worldRenderEvent.getMatrixStack(),
               mc.getBufferBuilders().getEntityVertexConsumers().getBuffer(RenderLayers.lines()),
               VoxelShapes.cuboid(new Box(0.0, 0.0, 0.0, floatArray[1], floatArray2[1], floatArray3[1])),
               0.0,
               0.0,
               0.0,
               Colors.computeInt6(ThemeColors.intVal9, 0.85F * this.renderFade),
               2.0F
            );
            mc.getBufferBuilders().getEntityVertexConsumers().draw();
            worldRenderEvent.getMatrixStack().pop();
         }
      } else {
         this.renderBoundingBox = null;
         this.renderFade = 0.0F;
      }
   };

   private void processData(long local4550_1) {
      for (BackTrackModule.DelayedPacket local4550_4 : this.delayedPackets) {
         if (System.currentTimeMillis() - local4550_4.timestamp < local4550_1) {
            break;
         }

         if (mc.getNetworkHandler() != null) {
            ((Packet<ClientPlayNetworkHandler>)local4550_4.packet).apply(mc.getNetworkHandler());
         }

         this.delayedPackets.remove(local4550_4);
      }
   }

   @Override
   protected void onDisable() {
      this.updateState();
      this.renderBoundingBox = null;
      this.renderFade = 0.0F;
   }

   private void updateState() {
      this.processData(0L);
      this.trackedTargetId = -1;
      this.realTargetPos = null;
   }

   private BackTrackModule() {
      this.delay = this.addSetting(new LegacyNumberSetting("Задержка", "", 10.0, 10.0, 600.0, 10.0));
      this.maxSeparation = this.addSetting(new LegacyNumberSetting("Максимальный отрыв", "", 3.7, 0.5, 6.0, 0.1));
      this.booleanSetting = this.addSetting(new BooleanSetting("Показывать бокс", "Рисует реальную позицию цели боксом.", true));
   }

   public void processData2(Entity entity) {
      if (this.isEnabled() && entity instanceof LivingEntity && entity != mc.player) {
         if (entity.getId() != this.trackedTargetId) {
            this.updateState();
            this.trackedTargetId = entity.getId();
            this.realTargetPos = entity.getTrackedPosition().getPos();
         }

         this.stopwatch.reset();
      }
   }

   public static BackTrackModule getInstance() {
      if (backTrackModule == null) {
         backTrackModule = new BackTrackModule();
      }

      return backTrackModule;
   }

   private static final class DelayedPacket {
      private final Packet<?> packet;
      private final long timestamp;

         public DelayedPacket(Packet<?> local4559_1, long local4559_2) {
         this.packet = local4559_1;
         this.timestamp = local4559_2;
      }
   }
}
