package greed.client.util.player.placement;

import greed.client.event.EventBus;
import greed.client.event.EventListener;
import greed.client.event.PacketEvent;
import greed.mixin.InteractionManagerSlotAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Vec3d;

/** Observes all native/module interactions; never cancels player packets or emits tick packets. */
public final class InteractionGate {
    private static final InteractionGate INSTANCE = new InteractionGate();
    private static boolean registered;
    private final InteractionWindow window = new InteractionWindow();
    private Object player, world;
    private Vec3d serverPosition;

    private final EventListener<PacketEvent> packetListener = this::packet;

    private static void ensure() {
        if (!registered) {
            registered = true;
            EventBus.getInstance().subscribe(INSTANCE);
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (INSTANCE.player != mc.player || INSTANCE.world != mc.world) {
            INSTANCE.window.reset();
            INSTANCE.serverPosition = null;
            INSTANCE.player = mc.player;
            INSTANCE.world = mc.world;
        }
    }

    public static boolean available() {
        ensure();
        return INSTANCE.window.available();
    }

    public static boolean availableAfterMovement() {
        ensure();
        return INSTANCE.window.availableAfterMovement();
    }

    public static int serverSlot() {
        ensure();
        if (INSTANCE.window.slot() >= 0) return INSTANCE.window.slot();
        var manager = MinecraftClient.getInstance().interactionManager;
        return manager == null ? -1 : ((InteractionManagerSlotAccessor) manager).rich$getLastSelectedSlot();
    }

    public static Vec3d serverEye() {
        ensure();
        var local = MinecraftClient.getInstance().player;
        if (local == null) return Vec3d.ZERO;
        Vec3d position = INSTANCE.serverPosition == null ? local.getEntityPos() : INSTANCE.serverPosition;
        return position.add(0, local.getEyeY() - local.getY(), 0);
    }

    private void packet(PacketEvent event) {
        if (event.isCancelled()) return;
        ensure();
        if (!event.isOutgoing()) {
            if (event.getPacket() instanceof PlayerPositionLookS2CPacket) serverPosition = null;
            return;
        }
        var packet = event.getPacket();
        if (packet instanceof ClientTickEndC2SPacket) window.endTick();
        else if (packet instanceof PlayerMoveC2SPacket movement) {
            var local = MinecraftClient.getInstance().player;
            if (local != null && movement.changesPosition())
                serverPosition =
                        new Vec3d(
                                movement.getX(local.getX()),
                                movement.getY(local.getY()),
                                movement.getZ(local.getZ()));
            window.movement();
        } else if (packet instanceof VehicleMoveC2SPacket) window.movement();
        else if (packet instanceof UpdateSelectedSlotC2SPacket selected)
            window.slot(selected.getSelectedSlot());
        else if (packet instanceof PlayerInteractBlockC2SPacket
                || packet instanceof PlayerInteractEntityC2SPacket
                || packet instanceof PlayerInteractItemC2SPacket
                || packet instanceof PlayerActionC2SPacket
                || packet instanceof ClickSlotC2SPacket
                || packet instanceof CloseHandledScreenC2SPacket) window.action();
    }
}
