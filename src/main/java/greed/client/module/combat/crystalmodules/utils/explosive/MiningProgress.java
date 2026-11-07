package greed.client.module.combat.crystalmodules.utils.explosive;

/** Accumulates vanilla mining delta once per client tick, then waits for server removal. */
public final class MiningProgress {
    private int firstTick, lastTick;
    private double progress;
    private boolean active, stopped;

    public void begin(int tick) {
        firstTick = tick;
        lastTick = tick - 1;
        progress = 0;
        active = true;
        stopped = false;
    }

    public boolean advance(int tick, double delta) {
        if (!active || stopped || tick <= lastTick || !Double.isFinite(delta) || delta <= 0) return false;
        lastTick = tick;
        progress += Math.min(1, delta);
        if (progress + 1e-7 < 1) return false;
        stopped = true;
        return true;
    }

    public boolean expired(int tick) {
        return active && (tick < firstTick || (long) tick - firstTick >= 200);
    }

    public float fraction() { return active ? (float)Math.clamp(progress, 0, 1) : 0; }
    public boolean stopped() { return stopped; }
    public void clear() { active = stopped = false; progress = 0; }
}
