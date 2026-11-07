package greed.client.util.player.placement;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** Closest point on the face; tangential inset avoids edge/corner ray ambiguity. */
public final class PlacementGeometry {
    private PlacementGeometry() {}

    public static Vec3d facePoint(Box box, Direction face, Vec3d eye) {
        double ex = Math.min(.08, (box.maxX - box.minX) / 4);
        double ey = Math.min(.08, (box.maxY - box.minY) / 4);
        double ez = Math.min(.08, (box.maxZ - box.minZ) / 4);
        double x = MathHelper.clamp(eye.x, box.minX + ex, box.maxX - ex);
        double y = MathHelper.clamp(eye.y, box.minY + ey, box.maxY - ey);
        double z = MathHelper.clamp(eye.z, box.minZ + ez, box.maxZ - ez);
        return switch (face) {
            case WEST -> new Vec3d(box.minX, y, z);
            case EAST -> new Vec3d(box.maxX, y, z);
            case DOWN -> new Vec3d(x, box.minY, z);
            case UP -> new Vec3d(x, box.maxY, z);
            case NORTH -> new Vec3d(x, y, box.minZ);
            case SOUTH -> new Vec3d(x, y, box.maxZ);
        };
    }
    /** Closest inset point plus a face grid: a hidden near corner must not hide the whole face. */
    public static java.util.List<Vec3d> facePoints(Box box, Direction face, Vec3d eye) {
        java.util.List<Vec3d> points = new java.util.ArrayList<>(10);
        points.add(facePoint(box, face, eye));
        for (double a : new double[]{.2,.5,.8}) for (double b : new double[]{.2,.5,.8}) {
            double x=box.minX+(box.maxX-box.minX)*a;
            double y=box.minY+(box.maxY-box.minY)*a;
            double z=box.minZ+(box.maxZ-box.minZ)*b;
            points.add(switch (face) {
                case WEST -> new Vec3d(box.minX, y, z);
                case EAST -> new Vec3d(box.maxX, y, z);
                case DOWN -> new Vec3d(x, box.minY, z);
                case UP -> new Vec3d(x, box.maxY, z);
                case NORTH -> new Vec3d(x, box.minY+(box.maxY-box.minY)*b, box.minZ);
                case SOUTH -> new Vec3d(x, box.minY+(box.maxY-box.minY)*b, box.maxZ);
            });
        }
        return points;
    }

}
