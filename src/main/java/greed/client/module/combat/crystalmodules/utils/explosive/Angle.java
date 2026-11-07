package greed.client.module.combat.crystalmodules.utils.explosive;

/** Immutable yaw/pitch pair used by the explosive rotation pipeline. */
public record Angle(float yaw, float pitch) {
    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }
}
