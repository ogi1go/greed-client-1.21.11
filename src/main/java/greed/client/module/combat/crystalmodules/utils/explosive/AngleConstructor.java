package greed.client.module.combat.crystalmodules.utils.explosive;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

/** A rotation request: target angles plus the smoothing strategy that should reach them. */
public final class AngleConstructor {
    private final RotateConstructor constructor;
    private final Angle target;
    private final Vec3d point;
    private final Entity entity;
    private final int ticks;

    AngleConstructor(
            RotateConstructor constructor, Angle target, Vec3d point, Entity entity, int ticks) {
        this.constructor = constructor;
        this.target = target;
        this.point = point;
        this.entity = entity;
        this.ticks = Math.max(1, ticks);
    }

    public RotateConstructor constructor() {
        return constructor;
    }

    public Angle target() {
        return target;
    }

    public Vec3d point() {
        return point;
    }

    public Entity entity() {
        return entity;
    }

    public int ticks() {
        return ticks;
    }
}
