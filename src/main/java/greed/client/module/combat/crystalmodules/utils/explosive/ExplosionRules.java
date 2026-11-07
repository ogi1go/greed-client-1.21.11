package greed.client.module.combat.crystalmodules.utils.explosive;

import java.util.ArrayDeque;

/** Shared, bounded ledger for real explosion attempts, independent of either module's toggle. */
public final class ExplosionRules {
    private record Debit(Object key, double value, int expiry) {}

    private final ArrayDeque<Debit> debits = new ArrayDeque<>();
    private double previous = Double.NaN;
    private int last = -1;

    public void clear() {
        debits.clear();
        previous = Double.NaN;
        last = -1;
    }

    public void observe(double health, int tick) {
        if (tick < last) clear();
        double paid = Double.isFinite(previous) ? Math.max(0, previous - health) : 0;
        while (paid > 0 && !debits.isEmpty()) {
            Debit d = debits.remove();
            double amount = Math.min(paid, d.value);
            paid -= amount;
            if (d.value > amount + .001) {
                debits.addFirst(new Debit(d.key, d.value - amount, d.expiry));
                break;
            }
        }
        debits.removeIf(d -> tick >= d.expiry);
        previous = health;
        last = tick;
    }

    public double reserved() {
        return debits.stream().mapToDouble(Debit::value).sum();
    }

    public boolean safe(
            double enemy,
            double self,
            double targetHp,
            double health,
            double minimum,
            double maximum,
            double reserve,
            Object retry) {
        if (!Double.isFinite(enemy)
                || !Double.isFinite(self)
                || enemy <= 0
                || self < 0
                || self > maximum) return false;
        boolean lethal = enemy >= targetHp && targetHp > 0;
        // A hit that finishes the target does not need to meet the ordinary damage floor.
        if (!lethal && enemy < minimum) return false;
        double outstanding =
                debits.stream().filter(d -> !d.key.equals(retry)).mapToDouble(Debit::value).sum();
        double remaining = health - outstanding - self - .5;
        if (remaining < 0) return false;
        if (remaining >= reserve) return true;
        // The old reserve check made the aura completely passive at low health, even when the
        // explosion would finish the target. A lethal hit is always worth crossing the reserve
        // while we survive. If we are already inside the reserve, refusing every hit only keeps
        // us passive, so a survivable hit is allowed there too. Maximum self-damage is still
        // enforced above.
        return lethal || health - outstanding <= reserve;
    }

    public void spend(Object key, double own, int tick, int ttl) {
        if (!Double.isFinite(own) || own < 0) return;
        if (debits.stream().noneMatch(d -> d.key.equals(key)))
            debits.add(new Debit(key, own + .5, tick + Math.max(6, ttl)));
    }

    public static double score(
            double enemy, double self, int preparationSteps, boolean environmentPreferred) {
        double trade = enemy - 1.5 * self;
        return trade <= 0
                ? 0
                : trade
                        / (1 + .12 * Math.max(0, preparationSteps))
                        * (environmentPreferred ? 1.1 : 1);
    }

    public static int effectiveCharges(int configured, boolean glowstoneOffhand) {
        return Math.max(1, Math.min(4, configured));
    }
    public static int effectiveCharges(int configured, int offhandGlowstoneCount) {
        // One charge already detonates the anchor. Auto-using the whole offhand stack only adds
        // three confirmed round trips, during which the victim walks out of the blast.
        return Math.clamp(configured, 1, 4);
    }

}
