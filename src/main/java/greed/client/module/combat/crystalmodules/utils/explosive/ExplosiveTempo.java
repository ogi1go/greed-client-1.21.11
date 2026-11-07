package greed.client.module.combat.crystalmodules.utils.explosive;

/** Fast dispatch readiness; dependent actions are sequenced by ExplosivePackets and its shared budget. */
public final class ExplosiveTempo {
    public static final float INTERNAL_FACTOR = 12.5f;
    public static final int ACTION_INTERVAL = 1;
    public static final float MAX_TURN = 180f;
    public static final float MIN_SMOOTHNESS = .5f;

    private ExplosiveTempo() {}

    public static float factor(float value) {
        return Float.isFinite(value) ? Math.clamp(value, 1, INTERNAL_FACTOR) : 1;
    }

    public static int delay(int configured, float factor) {
        return Math.max(0, (int) Math.ceil(Math.max(1, configured) / (double) factor(factor)));
    }

    public static float speed(float configured, float factor) {
        return Math.clamp(configured * factor(factor), 5, MAX_TURN);
    }

    public static float smoothness(float configured, float factor) {
        return Math.clamp(configured / factor(factor), MIN_SMOOTHNESS, 3f);
    }

    public static boolean ready(int tick, int nextTick, boolean pending) {
        return !pending && tick >= nextTick;
    }
}
