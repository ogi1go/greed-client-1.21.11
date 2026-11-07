package greed.client.util.player.placement;

import java.util.function.IntConsumer;

/** A synchronous hotbar transaction. Client inventory selection never changes. */
public final class SilentSwitch {
    private SilentSwitch() {}

    public static void execute(
            int original,
            int required,
            IntConsumer sendSlot,
            Runnable interact,
            Runnable swing,
            IntConsumer alignVanillaCache) {
        execute(original, required, -1, sendSlot, interact, swing, alignVanillaCache);
    }

    public static void execute(int original, int required, int serverSlot, IntConsumer sendSlot,
            Runnable interact, Runnable swing, IntConsumer alignVanillaCache) {
        if (original < 0 || original > 8 || required < 0 || required > 8)
            throw new IllegalArgumentException("Hotbar slots must be 0..8");
        boolean selected = false;
        try {
            // Unchanged slots are not retransmitted; observe other senders through InteractionGate.
            if (required != serverSlot) sendSlot.accept(required);
            selected = true;
            interact.run();
            swing.run();
        } finally {
            // No task scheduling or movement packet is inserted between these operations.
            int aligned = original;
            try {
                if (required != original || !selected) sendSlot.accept(original);
            } catch (RuntimeException | Error failure) {
                aligned = -1; // Force vanilla to resync next tick if restoring failed.
                throw failure;
            } finally {
                alignVanillaCache.accept(aligned);
            }
        }
    }
    public record Use(int slot, Runnable interaction, Runnable swing) {}

    /** A whole dependent sequence has one slot owner and exactly one final restoration. */
    public static void executeBatch(int original, int serverSlot, java.util.List<Use> uses,
            IntConsumer sendSlot, IntConsumer alignVanillaCache) {
        if (original < 0 || original > 8 || uses.isEmpty()) throw new IllegalArgumentException("Invalid batch");
        for (Use use : uses) if (use.slot < 0 || use.slot > 8) throw new IllegalArgumentException("Invalid slot");
        int held = serverSlot;
        boolean uncertain = false;
        try {
            for (Use use : uses) {
                if (held != use.slot) {
                    uncertain = true;
                    sendSlot.accept(use.slot);
                    held = use.slot;
                    uncertain = false;
                }
                use.interaction.run();
                use.swing.run();
            }
        } finally {
            int aligned = original;
            try { if (held != original || uncertain) sendSlot.accept(original); }
            catch (RuntimeException | Error failure) { aligned = -1; throw failure; }
            finally { alignVanillaCache.accept(aligned); }
        }
    }

}
