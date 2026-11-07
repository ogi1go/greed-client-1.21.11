package greed.client.module.combat.crystalmodules.utils.explosive;


import greed.client.util.player.placement.InteractionGate;
import greed.mixin.ClientWorldAccessor;
import greed.mixin.InteractionManagerSlotAccessor;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

final class ExplosiveMining {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final MiningProgress progress = new MiningProgress();
    private ClientPlayerEntity player;
    private ClientWorld world;
    private BlockPos block;
    private BlockState initial;
    private Direction side;
    private int original = -1, tool = -1, stoppedTick = -1;

    boolean active() {
        return block != null;
    }

    BlockPos block() {
        return block;
    }

    float fraction() {
        return progress.fraction();
    }

    boolean stopping() {
        return active() && progress.stopped();
    }

    boolean mineable(BlockPos pos) {
        if (mc.player == null || mc.world == null || !mc.player.getAbilities().allowModifyWorld
                || !mc.world.isInBuildLimit(pos) || !mc.world.getWorldBorder().contains(pos)) return false;
        BlockState state = mc.world.getBlockState(pos);
        return !state.isAir() && state.getFluidState().isEmpty() && !state.hasBlockEntity()
                && state.getHardness(mc.world, pos) >= 0 && bestTool(state) >= 0;
    }

    private int bestTool(BlockState state) {
        int selected = mc.player.getInventory().getSelectedSlot();
        int best = -1;
        double speed = -1;
        for (int offset = 0; offset < 9; offset++) {
            int slot = (selected + offset) % 9;
            var stack = mc.player.getInventory().getStack(slot);
            if (state.isToolRequired() && !stack.isSuitableFor(state)) continue;
            double value = stack.getMiningSpeedMultiplier(state);
            if (value <= 1 && !mc.player.isCreative()) continue;
            if (value > 1) {
                var enchants = stack.getEnchantments();
                for (var enchant : enchants.getEnchantments()) {
                    if (enchant.matchesKey(Enchantments.EFFICIENCY)) {
                        int level = enchants.getLevel(enchant);
                        if (level > 0) value += (double) level * level + 1;
                    }
                }
            }
            if (value > speed) {
                speed = value;
                best = slot;
            }
        }
        return best;
    }

    void observe() {
        if (!active()) return;
        if (mc.player != player || mc.world != world || !player.isAlive()
                || player.getInventory().getSelectedSlot() != tool
                || !world.getBlockState(block).equals(initial)
                || progress.expired(player.age)
                || (stoppedTick >= 0 && player.age - stoppedTick >= confirmationTicks())) cancel();
    }

    private int confirmationTicks() {
        var handler = mc.getNetworkHandler();
        var entry = handler == null ? null : handler.getPlayerListEntry(player.getUuid());
        return Math.clamp(entry == null ? 6 : (entry.getLatency() + 49) / 50 * 2 + 4, 6, 40);
    }

    boolean work(BlockHitResult hit) {
        if (hit == null || !InteractionGate.available()) return false;
        if (!active()) {
            if (!mineable(hit.getBlockPos())) return false;
            player = mc.player;
            world = mc.world;
            block = hit.getBlockPos().toImmutable();
            initial = world.getBlockState(block);
            side = hit.getSide();
            original = player.getInventory().getSelectedSlot();
            tool = bestTool(initial);
            if (tool < 0) tool = original;
            select(tool);
            progress.begin(player.age);
            send(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK);
            if (progress.advance(player.age, player.isCreative() ? 1 : initial.calcBlockBreakingDelta(player, world, block)))
                stoppedTick = player.age;
            player.swingHand(Hand.MAIN_HAND);
            return true;
        }
        if (!block.equals(hit.getBlockPos()) || progress.stopped()) return false;
        if (progress.advance(player.age, initial.calcBlockBreakingDelta(player, world, block))) {
            send(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK);
            stoppedTick = player.age;
            player.swingHand(Hand.MAIN_HAND);
        } else if (player.age % 4 == 0) {
            player.swingHand(Hand.MAIN_HAND);
        }
        return true;
    }

    private void send(PlayerActionC2SPacket.Action action) {
        var handler = mc.getNetworkHandler();
        if (handler == null) return;
        try (var updates = ((ClientWorldAccessor) world).greed$getPendingUpdateManager().incrementSequence()) {
            handler.sendPacket(new PlayerActionC2SPacket(action, block, side, updates.getSequence()));
        }
    }

    private void select(int slot) {
        if (player.getInventory().getSelectedSlot() == slot) return;
        player.getInventory().setSelectedSlot(slot);
        var handler = mc.getNetworkHandler();
        if (handler != null) handler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        if (mc.interactionManager != null)
            ((InteractionManagerSlotAccessor) mc.interactionManager).rich$setLastSelectedSlot(slot);
    }

    void cancel() {
        if (!active()) return;
        try {
            if (mc.player == player && mc.world == world && mc.getNetworkHandler() != null) {
                if (world.getBlockState(block).equals(initial))
                    send(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK);
                if (player.getInventory().getSelectedSlot() == tool) select(original);
            }
        } finally {
            block = null;
            player = null;
            world = null;
            initial = null;
            original = tool = stoppedTick = -1;
            progress.clear();
        }
    }
}