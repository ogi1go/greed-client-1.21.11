package greed.client.util.player.placement;

/** One automatic interaction before movement, reset by the real client tick-end packet. */
public final class InteractionWindow {
    private boolean used, moved;
    private int slot = -1;
    public boolean available() { return !used && !moved; }
    /** A post-movement use still shares the same one-action budget with all earlier actions. */
    public boolean availableAfterMovement() { return !used; }
    public void action() { used = true; }
    public void movement() { moved = true; }
    public void endTick() { used = moved = false; }
    public void reset() { endTick(); slot = -1; }
    public int slot() { return slot; }
    public void slot(int value) { slot = value >= 0 && value <= 8 ? value : -1; }
}
