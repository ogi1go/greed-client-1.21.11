package greed.client.module.combat.crystalmodules.utils.explosive;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

/** Small factory mirroring the rotation API the explosive port was written against. */
public final class AngleConfig {
    private final RotateConstructor constructor;
    private final boolean smooth;
    private final boolean randomize;
    private final boolean limit;

    public AngleConfig(RotateConstructor constructor, boolean smooth, boolean randomize) {
        this(constructor, smooth, randomize, true);
    }

    public AngleConfig(
            RotateConstructor constructor, boolean smooth, boolean randomize, boolean limit) {
        this.constructor = constructor;
        this.smooth = smooth;
        this.randomize = randomize;
        this.limit = limit;
    }

    public boolean isSmooth() {
        return smooth;
    }

    public boolean isRandomize() {
        return randomize;
    }

    public boolean isLimit() {
        return limit;
    }

    public AngleConstructor createRotationPlan(
            Angle target, Vec3d point, Entity entity, int ticks) {
        return new AngleConstructor(
                constructor,
                target == null ? new Angle(0f, 0f) : target,
                point == null ? Vec3d.ZERO : point,
                entity,
                ticks);
    }
}
