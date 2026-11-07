package greed.client.module.combat.crystalmodules.utils;

import java.util.HashMap;
import java.util.Map;

/** Rate limits retries of one world action, including rejected client interactions. */
public final class ActionRetry<K> {
    private record Attempt(int at, int count, int next) {}

    private final Map<K, Attempt> attempts = new HashMap<>();

    public boolean ready(K key, int tick) {
        Attempt a = attempts.get(key);
        return a == null || tick < a.at || tick >= a.next;
    }

    public void sent(K key, int tick, int delay) {
        attempts.entrySet().removeIf(e -> tick < e.getValue().at || tick - e.getValue().at > 200);
        Attempt old = attempts.get(key);
        int count = old == null || tick < old.at || tick - old.at >= 40 ? 1 : old.count + 1;
        int wait = Math.max(2, delay);
        if (count >= 3) {
            wait = Math.max(40, wait);
            count = 0;
        }
        attempts.put(key, new Attempt(tick, count, tick + wait));
    }

    /** A server-confirmed action starts a new retry cycle, not a sequence of failures. */
    public void acknowledge(K key) {
        attempts.remove(key);
    }

    public void clear() {
        attempts.clear();
    }
}
