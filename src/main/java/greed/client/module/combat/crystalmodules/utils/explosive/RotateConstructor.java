package greed.client.module.combat.crystalmodules.utils.explosive;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

/**
 * Pluggable smoothing strategy for a rotation plan.
 *
 * <p>The coordinator supplies one implementation that advances {@link ExplosiveAim}; the
 * rotation connection calls it once per planning pass so the existing angular velocity state is
 * preserved.
 */
public abstract class RotateConstructor {
    private final String name;

    protected RotateConstructor(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public abstract Angle limitAngleChange(
            Angle current, Angle desired, Vec3d point, Entity entity);

    public abstract Vec3d randomValue();
}
