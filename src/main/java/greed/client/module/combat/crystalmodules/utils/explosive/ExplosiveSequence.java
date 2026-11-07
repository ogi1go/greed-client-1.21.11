package greed.client.module.combat.crystalmodules.utils.explosive;

import java.util.*;

import greed.client.module.combat.crystalmodules.utils.explosive.ExplosiveCycle.Step;
/** Ordered dependent actions; never guesses an entity ID or an unobserved crystal spawn. */
public final class ExplosiveSequence {
    private ExplosiveSequence() {}
    public static List<ExplosiveCycle.Step> anchor(boolean exists, int charges, int required) {
        int goal = Math.clamp(required, 1, 4), current = exists ? Math.clamp(charges, 0, 4) : 0;
        List<Step> steps = new ArrayList<>(6);
        if (!exists) steps.add(Step.ANCHOR_PLACE);
        for (int i=current; i<goal; i++) steps.add(Step.ANCHOR_CHARGE);
        steps.add(Step.ANCHOR_BREAK);
        return List.copyOf(steps);
    }
    public static double horizon(double cap, int latencyMs, boolean anchor, int preparation) {
        if (!Double.isFinite(cap) || cap <= 0) return 0;
        double roundTrip = Math.clamp(latencyMs, 0, 1000) / 50.0;
        // Remote snapshots are already one one-way trip old. Crystal creation also needs
        // a spawn acknowledgement before its ID is usable. Anchor stages are confirmed one by
        // one, so every remaining stage (place/charge/break) has to be included in the forecast.
        double tick;
        if (anchor) {
            double stage = roundTrip + .5 + ExplosiveTempo.ACTION_INTERVAL;
            tick = stage * (Math.max(0, preparation) + 1);
        } else {
            tick = roundTrip + .5 + (preparation > 0 ? roundTrip + .5 : 0);
        }
        return Math.min(Math.clamp(cap, 0, 8), tick);
    }
    /** 0..8 hotbar and 9 offhand. Counts are shared across every stage of one batch. */
    public static int takeSlot(int current, int[] counts, int[] consumed) {
        if (current<0||current>8||counts.length!=10||consumed.length!=10)
            throw new IllegalArgumentException("Invalid inventory snapshot");
        if (counts[9]>consumed[9]) { consumed[9]++; return 9; }
        for(int i=0;i<9;i++){int slot=(current+i)%9;
            if(counts[slot]>consumed[slot]){consumed[slot]++;return slot;}}
        return -1;
    }

}
