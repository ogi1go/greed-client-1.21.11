package greed.client.util.player.placement;

import greed.client.event.AttackEvent;
import greed.client.event.EventBus;
import greed.client.event.InteractEntityEvent;
import greed.mixin.ClientWorldAccessor;
import greed.mixin.InteractionManagerSlotAccessor;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;



public final class BlockPlacement {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private ClientPlayerEntity player;
    private ClientWorld world;
    private int lastActionTick = -1;
    private boolean busy;

    private boolean available() { return available(false); }

    private boolean available(boolean afterMovement) {
        if (mc.player == null
                || mc.world == null
                || mc.getNetworkHandler() == null
                || mc.interactionManager == null
                || !mc.isOnThread()) return false;
        if (player != mc.player || world != mc.world || mc.player.age < lastActionTick) {
            player = mc.player;
            world = mc.world;
            lastActionTick = -1;
        }
        return !busy && lastActionTick != mc.player.age
                && (afterMovement ? InteractionGate.availableAfterMovement() : InteractionGate.available());
    }

    public boolean interact(Hand hand, int hotbarSlot, BlockHitResult hit) {
        return interact(hand, hotbarSlot, hit, false);
    }

    /** Hidden geometry still requires a real support, actual face, reach and sequence. */
    public boolean interact(Hand hand, int hotbarSlot, BlockHitResult hit, boolean allowHidden) {
        return interact(hand, hotbarSlot, hit, allowHidden, false);
    }

    /** Use the rotation/position already sent by vanilla; does not emit extra movement packets. */
    public boolean interactAfterMovement(Hand hand, int hotbarSlot, BlockHitResult hit) {
        return interact(hand, hotbarSlot, hit, false, true);
    }

    private boolean interact(Hand hand, int hotbarSlot, BlockHitResult hit, boolean allowHidden, boolean afterMovement) {
        if (!available(afterMovement)
                || hit == null
                || !mc.world.isInBuildLimit(hit.getBlockPos())
                || !mc.world.getWorldBorder().contains(hit.getBlockPos())) return false;
        double reach = mc.player.getBlockInteractionRange();
        Vec3d p = hit.getPos();
        BlockPos b = hit.getBlockPos();
        if (!Double.isFinite(p.x)
                || !Double.isFinite(p.y)
                || !Double.isFinite(p.z)
                || p.x < b.getX()
                || p.x > b.getX() + 1
                || p.y < b.getY()
                || p.y > b.getY() + 1
                || p.z < b.getZ()
                || p.z > b.getZ() + 1
                || mc.player.getEyePos().squaredDistanceTo(p) > reach * reach
                || InteractionGate.serverEye().squaredDistanceTo(p) > reach * reach
                || mc.world.getBlockState(b).getOutlineShape(mc.world, b).isEmpty()) return false;
        if (hit.isInsideBlock()) return false;
        if (!allowHidden && !visibleHit(hit)) return false;
        int original = mc.player.getInventory().getSelectedSlot();
        int selected = hand == Hand.OFF_HAND ? original : hotbarSlot;
        if (selected < 0 || selected > 8) return false;
        BlockHitResult packetHit =
                new BlockHitResult(hit.getPos(), hit.getSide(), hit.getBlockPos(), false);
        busy = true;
        lastActionTick = mc.player.age;
        try (PendingUpdateManager updates =
                ((ClientWorldAccessor) mc.world).greed$getPendingUpdateManager().incrementSequence()) {
            int sequence = updates.getSequence();
            // C2S order: required slot -> sequenced use-on-block -> hand swing -> original slot.
            SilentSwitch.execute(
                    original,
                    selected,
                    InteractionGate.serverSlot(),
                    slot ->
                            mc.player.networkHandler.sendPacket(
                                    new UpdateSelectedSlotC2SPacket(slot)),
                    () ->
                            mc.player.networkHandler.sendPacket(
                                    new PlayerInteractBlockC2SPacket(hand, packetHit, sequence)),
                    () -> mc.player.swingHand(hand),
                    slot ->
                            ((InteractionManagerSlotAccessor) mc.interactionManager)
                                    .rich$setLastSelectedSlot(slot));
            return true; // Attempt sent; acceptance is decided only by a server world update.
        } finally {
            busy = false;
        }
    }

    public boolean attack(Entity target, int hotbarSlot) {
        if (!available()
                || target == null
                || !target.isAlive()
                || target.isRemoved()
                || hotbarSlot < 0
                || hotbarSlot > 8) return false;
        Vec3d eye = InteractionGate.serverEye();
        Box box = target.getBoundingBox();
        Vec3d closest = new Vec3d(MathHelper.clamp(eye.x,box.minX,box.maxX),
                MathHelper.clamp(eye.y,box.minY,box.maxY),MathHelper.clamp(eye.z,box.minZ,box.maxZ));
        double reach = mc.player.getEntityInteractionRange();
        if (eye.squaredDistanceTo(closest) > reach*reach) return false;
        InteractEntityEvent event = new InteractEntityEvent(target);
        EventBus.getInstance().post(event);
        if (event.isCancelled()) return false;
        EventBus.getInstance().post(new AttackEvent(target));
        if (!available()) return false;
        int original = mc.player.getInventory().getSelectedSlot();
        busy = true;
        lastActionTick = mc.player.age;
        try {
            SilentSwitch.execute(
                    original,
                    hotbarSlot,
                    InteractionGate.serverSlot(),
                    slot ->
                            mc.player.networkHandler.sendPacket(
                                    new UpdateSelectedSlotC2SPacket(slot)),
                    () -> {
                        mc.player.networkHandler.sendPacket(
                                PlayerInteractEntityC2SPacket.attack(
                                        target, mc.player.isSneaking()));
                        mc.player.resetTicksSince();
                    },
                    () -> mc.player.swingHand(Hand.MAIN_HAND),
                    slot ->
                            ((InteractionManagerSlotAccessor) mc.interactionManager)
                                    .rich$setLastSelectedSlot(slot));
            return true;
        } finally {
            busy = false;
        }
    }

    /** Click a real adjacent support, including UP on the block underneath. Never click air. */
    public BlockHitResult placementHit(BlockPos target, double reach) {
        return placementHit(target, reach, false);
    }

    public BlockHitResult placementHit(BlockPos target, double reach, boolean allowHidden) {
        if (mc.world == null
                || mc.player == null
                || !mc.world.isInBuildLimit(target)
                || !mc.world.getWorldBorder().contains(target)
                || !mc.world.getBlockState(target).isReplaceable()) return null;
        BlockHitResult best = null;
        double cost = Double.POSITIVE_INFINITY;
        for (Direction direction : Direction.values()) {
            BlockPos support = target.offset(direction);
            BlockState state = mc.world.getBlockState(support);
            if (state.isReplaceable() || state.hasBlockEntity() || interactive(state.getBlock()))
                continue;
            BlockHitResult hit = faceHit(support, direction.getOpposite(), reach, allowHidden);
            if (hit != null) {
                double next = cost(hit, reach);
                if (next < cost) {
                    cost = next;
                    best = hit;
                }
            }
        }
        return best;
    }

    /** Prefer visible samples; hidden geometry is an explicit module option. */
    public BlockHitResult interactionHit(BlockPos block, double reach) {
        return interactionHit(block, reach, false);
    }

    public BlockHitResult interactionHit(BlockPos block, double reach, boolean allowHidden) {
        if (mc.player == null || mc.world == null) return null;
        BlockHitResult best = null;
        double cost = Double.POSITIVE_INFINITY;
        for (Direction side : Direction.values()) {
            BlockHitResult hit = faceHit(block, side, reach, allowHidden);
            if (hit != null) {
                double next = cost(hit, reach);
                if (next < cost) {
                    cost = next;
                    best = hit;
                }
            }
        }
        return best;
    }

    public BlockHitResult faceHit(BlockPos block, Direction face, double reach) {
        return faceHit(block, face, reach, false);
    }

    public BlockHitResult faceHit(BlockPos block, Direction face, double reach, boolean allowHidden) {
        if (mc.player == null || mc.world == null || !Double.isFinite(reach) || reach <= 0)
            return null;
        reach = Math.min(reach, mc.player.getBlockInteractionRange());
        Vec3d eye = mc.player.getEyePos();
        var shape = mc.world.getBlockState(block).getOutlineShape(mc.world, block);
        BlockHitResult best = null;
        double cost = Double.POSITIVE_INFINITY;
        for (Box local : shape.getBoundingBoxes()) {
            for (Vec3d point : PlacementGeometry.facePoints(local.offset(block), face, eye)) {
                if (eye.squaredDistanceTo(point) > reach * reach
                        || InteractionGate.serverEye().squaredDistanceTo(point) > reach * reach) continue;
                BlockHitResult entry = shape.raycast(eye,
                        point.add(point.subtract(eye).normalize().multiply(.001)), block);
                if (entry == null || entry.isInsideBlock() || entry.getSide() != face) continue;
                BlockHitResult hit = new BlockHitResult(point, face, block, false);
                boolean visible = visibleHit(hit);
                if (!allowHidden && !visible) continue;
                double next = eye.squaredDistanceTo(point) + (visible ? 0 : reach * reach + 1);
                if (next < cost) { cost = next; best = hit; }
                // The closest sample is first; avoid tracing the entire grid on clear faces.
                if (visible) break;
            }
        }
        return best;
    }

    private boolean visibleHit(BlockHitResult hit) {
        Vec3d eye = mc.player.getEyePos();
        BlockHitResult actual = mc.world.raycast(new RaycastContext(eye,
                hit.getPos().add(hit.getPos().subtract(eye).normalize().multiply(.001)),
                RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player));
        return actual.getType() == HitResult.Type.BLOCK
                && actual.getBlockPos().equals(hit.getBlockPos()) && actual.getSide() == hit.getSide();
    }

    private double cost(BlockHitResult hit, double reach) {
        BlockHitResult obstruction =
                mc.world.raycast(
                        new RaycastContext(
                                mc.player.getEyePos(),
                                hit.getPos(),
                                RaycastContext.ShapeType.OUTLINE,
                                RaycastContext.FluidHandling.NONE,
                                mc.player));
        boolean visible =
                obstruction.getType() == HitResult.Type.MISS
                        || obstruction.getBlockPos().equals(hit.getBlockPos());
        return mc.player.getEyePos().squaredDistanceTo(hit.getPos())
                + (visible ? 0 : reach * reach + 1);
    }

    private boolean interactive(Block b) {
        return b instanceof CraftingTableBlock
                || b instanceof AnvilBlock
                || b instanceof StonecutterBlock
                || b instanceof GrindstoneBlock
                || b instanceof CartographyTableBlock
                || b instanceof SmithingTableBlock
                || b instanceof LoomBlock
                || b instanceof ComposterBlock
                || b instanceof RespawnAnchorBlock
                || b instanceof DoorBlock
                || b instanceof TrapdoorBlock
                || b instanceof FenceGateBlock
                || b instanceof LeverBlock
                || b instanceof ButtonBlock
                || b instanceof NoteBlock;
    }
}
