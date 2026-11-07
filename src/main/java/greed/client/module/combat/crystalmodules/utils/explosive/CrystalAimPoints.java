package greed.client.module.combat.crystalmodules.utils.explosive;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Tests multiple visible parts, rather than rejecting a crystal for one hidden corner. */
public final class CrystalAimPoints {
    private CrystalAimPoints() {}

    public static Vec3d find(
            Box box, Vec3d eye, Vec3d direction, double reach, Predicate<Vec3d> visible) {
        if (!Double.isFinite(reach) || reach <= 0) return null;
        if (box.contains(eye)) return eye;
        List<Vec3d> points = new ArrayList<>(30);
        box.raycast(eye, eye.add(direction.normalize().multiply(reach))).ifPresent(points::add);
        Box inner = box.contract(.08);
        points.add(
                new Vec3d(
                        Math.clamp(eye.x, inner.minX, inner.maxX),
                        Math.clamp(eye.y, inner.minY, inner.maxY),
                        Math.clamp(eye.z, inner.minZ, inner.maxZ)));
        for (double x : new double[] {inner.minX, (inner.minX + inner.maxX) * .5, inner.maxX})
            for (double y : new double[] {inner.minY, (inner.minY + inner.maxY) * .5, inner.maxY})
                for (double z :
                        new double[] {inner.minZ, (inner.minZ + inner.maxZ) * .5, inner.maxZ})
                    points.add(new Vec3d(x, y, z));
        Vec3d best = null;
        double bestCost = Double.POSITIVE_INFINITY;
        Vec3d forward = direction.normalize();
        for (Vec3d inside : points) {
            Vec3d ray = inside.subtract(eye);
            if (ray.lengthSquared() < 1e-10) continue;
            Vec3d entry = box.raycast(eye, eye.add(ray.normalize().multiply(reach))).orElse(null);
            if (entry == null
                    || eye.squaredDistanceTo(entry) > reach * reach
                    || !visible.test(entry)) continue;
            double cost =
                    1 - forward.dotProduct(ray.normalize()) + eye.squaredDistanceTo(entry) * .0001;
            if (cost < bestCost) {
                bestCost = cost;
                best = inside;
            }
        }
        return best;
    }
}
