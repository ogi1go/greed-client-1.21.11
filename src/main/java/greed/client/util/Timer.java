package greed.client.util;

public class Timer {
    private long lastTime = 0;

    public boolean hasReached(long milliseconds) {
        return (System.currentTimeMillis() - lastTime) >= milliseconds;
    }

    public void reset() {
        lastTime = System.currentTimeMillis();
    }
}