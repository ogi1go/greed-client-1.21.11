package greed.client.module.combat.crystalmodules.utils.explosive;

import java.util.List;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;

/** Complete anchor bursts compete with ready crystal explosions; crystal preparation is lower priority. */
public final class ExplosiveCycle {
    public enum Step {
        BLOCK_CLEAR,
        OBSIDIAN_PLACE,
        CRYSTAL_PLACE,
        CRYSTAL_BREAK,
        ANCHOR_PLACE,
        ANCHOR_CHARGE,
        ANCHOR_BREAK
    }

    public record Observation(boolean crystal, boolean obsidian, boolean anchor, int charges) {}

    private ExplosiveCycle() {}

    public static boolean confirmed(Step step, int expected, Observation state) {
        return switch (step) {
            case BLOCK_CLEAR -> false; // Mining confirms the actual world change separately.
            case OBSIDIAN_PLACE -> state.obsidian;
            case CRYSTAL_PLACE -> state.crystal;
            case CRYSTAL_BREAK -> !state.crystal;
            case ANCHOR_PLACE -> state.anchor;
            case ANCHOR_CHARGE -> state.anchor && state.charges >= expected;
            case ANCHOR_BREAK -> !state.anchor;
        };
    }

    public static int rank(Step step) {
        return switch (step) {
            // A fully prepared explosion always beats placing or charging another one.
            case CRYSTAL_BREAK, ANCHOR_BREAK -> 4;
            case ANCHOR_PLACE, ANCHOR_CHARGE -> 3;
            case CRYSTAL_PLACE -> 1;
            default -> 0;
        };
    }

    public static <T> T select(
            List<T> candidates,
            ToIntFunction<T> rank,
            ToDoubleFunction<T> score,
            Function<T, Object> key,
            Object previous) {
        T best = null;
        for (T candidate : candidates)
            if (best == null
                    || rank.applyAsInt(candidate) > rank.applyAsInt(best)
                    || rank.applyAsInt(candidate) == rank.applyAsInt(best)
                            && score.applyAsDouble(candidate) > score.applyAsDouble(best))
                best = candidate;
        if (best != null && previous != null)
            for (T candidate : candidates)
                if (previous.equals(key.apply(candidate))
                        && rank.applyAsInt(candidate) == rank.applyAsInt(best)
                        && score.applyAsDouble(candidate) >= score.applyAsDouble(best) * .8)
                    return candidate;
        return best;
    }
}
