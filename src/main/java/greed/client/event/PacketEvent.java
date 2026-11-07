package greed.client.event;

import greed.client.module.misc.AutoBuyModule;
import net.minecraft.network.packet.Packet;

public class PacketEvent extends CancellableEvent {

   private final PacketEvent.Direction direction;
   private Packet<?> packet;

   public Packet<?> getPacket() {
      return this.packet;
   }

   public void setPacket(Packet<?> newPacket) {
      this.packet = newPacket;
   }

   public PacketEvent.Direction getDirection() {
      return this.direction;
   }

   public boolean isOutgoing() {
      return this.getDirection() == PacketEvent.Direction.SENT;
   }

   public boolean isIncoming() {
      return this.getDirection() == PacketEvent.Direction.RECEIVE;
   }

   public PacketEvent(PacketEvent.Direction initialDirection, Packet<?> initialPacket) {
      this.direction = initialDirection;
      this.packet = initialPacket;
   }

   public enum Direction {
      SENT,
      RECEIVE;
   }
}
