package greed.client.util.greedtone;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public final class Pathfinder {

    public record Result(List<Step> path, boolean reachedGoal) {
    }

    private record Edge(int x, int y, int z, MoveType type, double cost) {
    }

    private static final class Node {
        final int x;
        final int y;
        final int z;
        final Node parent;
        final MoveType type;
        final double g;
        final double h;
        final double f;
        boolean closed;

        Node(int x, int y, int z, Node parent, MoveType type, double g, double h) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.parent = parent;
            this.type = type;
            this.g = g;
            this.h = h;
            this.f = g + h;
        }
    }

    private static final int[][] CARDINAL = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    private static final int[][] DIAGONAL = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

    private final Terrain t;
    private final Goal goal;

    public Pathfinder(Terrain terrain, Goal goal) {
        this.t = terrain;
        this.goal = goal;
    }

    public Result find(BlockPos start) {
        long deadline = System.nanoTime() + Settings.timeoutMs * 1_000_000L;
        Long2ObjectOpenHashMap<Node> nodes = new Long2ObjectOpenHashMap<>();
        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingDouble(n -> n.f));
        Node first = new Node(start.getX(), start.getY(), start.getZ(), null, MoveType.TRAVERSE, 0.0,
                goal.heuristic(start.getX(), start.getY(), start.getZ()));
        nodes.put(BlockPos.asLong(first.x, first.y, first.z), first);
        open.add(first);
        Node best = first;
        int expanded = 0;
        List<Edge> edges = new ArrayList<>(64);

        while (!open.isEmpty()) {
            Node cur = open.poll();
            if (cur.closed) {
                continue;
            }
            cur.closed = true;
            if (goal.isEnd(cur.x, cur.y, cur.z)) {
                return new Result(build(cur), true);
            }
            if (cur.h < best.h) {
                best = cur;
            }
            expanded++;
            if (expanded > Settings.maxNodes
                    || ((expanded & 255) == 0
                    && System.nanoTime() > deadline)) {
                break;
            }
            if (expanded > Settings.maxNodes || ((expanded & 255) == 0 && System.nanoTime() > deadline)) {
                break;
            }

            edges.clear();
            expand(cur, edges);
            for (Edge e : edges) {
                long key = BlockPos.asLong(e.x(), e.y(), e.z());
                double g = cur.g + e.cost();
                Node old = nodes.get(key);
                if (old == null) {
                    Node created = new Node(e.x(), e.y(), e.z(), cur, e.type(), g, goal.heuristic(e.x(), e.y(), e.z()));
                    nodes.put(key, created);
                    open.add(created);
                } else if (g < old.g) {
                    old.closed = true;
                    Node created = new Node(e.x(), e.y(), e.z(), cur, e.type(), g, old.h);
                    nodes.put(key, created);
                    open.add(created);
                }
            }
        }

        if (best == first) {
            return new Result(List.of(), false);
        }
        return new Result(build(best), false);
    }

    private static List<Step> build(Node end) {
        ArrayList<Step> list = new ArrayList<>();
        for (Node n = end; n != null; n = n.parent) {
            list.add(new Step(new BlockPos(n.x, n.y, n.z), n.type));
        }
        Collections.reverse(list);
        return list;
    }

    private double step(int x, int y, int z) {
        int f = t.flags(x, y, z);
        double c;
        if ((f & Flags.WATER) != 0) {
            c = Costs.SWIM;
        } else {
            int below = t.flags(x, y - 1, z);
            c = Costs.SPRINT;
            if ((below & Flags.SLOW) != 0) {
                c *= Costs.SLOW_MULT;
            } else if ((below & Flags.ICE) != 0) {
                c *= Costs.ICE_MULT;
            }
        }
        if ((f & Flags.DOOR_CLOSED) != 0) {
            c += Costs.DOOR;
        }
        if ((t.flags(x, y + 1, z) & Flags.DOOR_CLOSED) != 0) {
            c += Costs.DOOR;
        }
        return c;
    }

    private void expand(Node n, List<Edge> out) {
        int x = n.x;
        int y = n.y;
        int z = n.z;
        int here = t.flags(x, y, z);
        boolean inWater = (here & Flags.WATER) != 0;
        boolean onClimb = (here & Flags.CLIMB) != 0;
        boolean floor = t.hasFloor(x, y - 1, z);
        boolean grounded = floor && !onClimb;
        Node parent = n.parent;

        for (int[] d : CARDINAL) {
            int dx = d[0];
            int dz = d[1];
            int tx = x + dx;
            int tz = z + dz;

            if (parent != null && tx == parent.x && y == parent.y && tz == parent.z) {
                continue;
            }

            double feet = t.clearCost(tx, y, tz);
            double head = t.clearCost(tx, y + 1, tz);
            if (feet < Terrain.INF && head < Terrain.INF) {
                if (t.supported(tx, y, tz)) {
                    out.add(new Edge(tx, y, tz, MoveType.TRAVERSE, step(tx, y, tz) + feet + head));
                } else if (feet == 0.0 && head == 0.0 && grounded && t.canPlace() && t.replaceable(tx, y - 1, tz)) {
                    out.add(new Edge(tx, y, tz, MoveType.BRIDGE, Costs.WALK + Costs.PLACE));
                }
            }

            if (grounded || inWater) {
                double above = t.clearCost(x, y + 2, z);
                double upFeet = t.clearCost(tx, y + 1, tz);
                double upHead = t.clearCost(tx, y + 2, tz);
                if (above < Terrain.INF && upFeet < Terrain.INF && upHead < Terrain.INF && t.supported(tx, y + 1, tz)) {
                    out.add(new Edge(tx, y + 1, tz, MoveType.ASCEND, Costs.ASCEND + above + upFeet + upHead));
                }
            }
            if (isWaterSurface(x, y, z)
                    && isWaterSurface(tx, y, tz)) {

                out.add(new Edge(
                        tx,
                        y,
                        tz,
                        MoveType.SWIM_SURFACE,
                        Costs.SWIM
                ));
            }
            if (!t.supported(tx, y, tz) && feet < Terrain.INF && head < Terrain.INF) {
                for (int fall = 1; fall <= Settings.maxFallWater; fall++) {
                    int qy = y - fall;
                    if (!t.free(tx, qy, tz)) {
                        break;
                    }
                    if (t.supported(tx, qy, tz)) {
                        if (fall <= Settings.maxFall || t.water(tx, qy, tz)) {
                            out.add(new Edge(tx, qy, tz, MoveType.DESCEND,
                                    Costs.SPRINT + Costs.fall(fall) + feet + head));
                        }
                        break;
                    }
                }
            }
        }

        for (int[] d : DIAGONAL) {
            int dx = d[0];
            int dz = d[1];
            int tx = x + dx;
            int tz = z + dz;
            if (parent != null && tx == parent.x && y == parent.y && tz == parent.z) {
                continue;
            }
            if (!(t.strictFree(x + dx, y, z) && t.strictFree(x + dx, y + 1, z)
                    && t.strictFree(x, y, z + dz) && t.strictFree(x, y + 1, z + dz))) {
                continue;
            }
            if (!t.canOccupy(tx, y, tz) || t.door(tx, y, tz)) {
                continue;
            }
            out.add(new Edge(tx, y, tz, MoveType.DIAGONAL, step(tx, y, tz) * Costs.SQRT2));
        }

        if (onClimb && t.climb(x, y + 1, z) && t.canOccupy(x, y + 1, z)) {
            out.add(new Edge(x, y + 1, z, MoveType.CLIMB_UP, Costs.CLIMB_UP));
        } else if (inWater && t.water(x, y + 1, z) && t.canOccupy(x, y + 1, z)) {
            out.add(new Edge(x, y + 1, z, MoveType.SWIM_UP, Costs.SWIM));
        }
        if (inWater
                && !t.water(x, y + 1, z)
                && t.free(x, y + 1, z)
                && t.free(x, y + 2, z)) {

            out.add(new Edge(
                    x,
                    y + 1,
                    z,
                    MoveType.SWIM_SURFACE,
                    Costs.SWIM
            ));
        }

        if ((onClimb || inWater) && t.canOccupy(x, y - 1, z)) {
            if (onClimb) {
                out.add(new Edge(x, y - 1, z, MoveType.CLIMB_DOWN, Costs.CLIMB_DOWN));
            } else {
                out.add(new Edge(x, y - 1, z, MoveType.SWIM_DOWN, Costs.SWIM));
            }
        }

        if (grounded && !inWater && Settings.allowBreak) {
            double bc = t.breakCost(x, y - 1, z);
            if (bc < Terrain.INF && t.hasFloor(x, y - 2, z)) {
                out.add(new Edge(x, y - 1, z, MoveType.DIG_DOWN, bc + Costs.fall(1)));
            }
        }

        if (grounded && !inWater && t.canPlace()) {
            double above = t.clearCost(x, y + 2, z);
            if (above < Terrain.INF) {
                out.add(new Edge(x, y + 1, z, MoveType.PILLAR, Costs.ASCEND + Costs.PLACE + above));
            }
        }

        if (Settings.allowParkour && grounded && !inWater && t.free(x, y + 2, z)) {
            for (int[] d : CARDINAL) {
                int dx = d[0];
                int dz = d[1];
                boolean run = t.strictFree(x - dx, y, z - dz)
                        && t.strictFree(x - dx, y + 1, z - dz)
                        && t.hasFloor(x - dx, y - 1, z - dz);
                for (int dist = 2; dist <= 4; dist++) {
                    if (dist >= 3 && !run) {
                        break;
                    }
                    for (int dy = -1; dy <= 1; dy++) {
                        if (dy == 1 && dist > 3) {
                            continue;
                        }
                        int lx = x + dx * dist;
                        int lz = z + dz * dist;
                        int ly = y + dy;
                        if (!t.hasFloor(lx, ly - 1, lz) || !t.canOccupy(lx, ly, lz) || t.water(lx, ly, lz)) {
                            continue;
                        }
                        if (dy == -1 && !t.free(lx, y + 1, lz)) {
                            continue;
                        }
                        if (dy == 0 && dist == 2 && t.hasFloor(x + dx, y - 1, z + dz)) {
                            continue;
                        }
                        boolean clear = true;
                        for (int i = 1; i < dist; i++) {
                            int cx = x + dx * i;
                            int cz = z + dz * i;
                            if (!(t.free(cx, y, cz) && t.free(cx, y + 1, cz) && t.free(cx, y + 2, cz))) {
                                clear = false;
                                break;
                            }
                        }

                        if (!clear) {
                            continue;
                        }
                        double cost = dist * Costs.SPRINT + Costs.PARKOUR_JUMP
                                + (dy == -1 ? Costs.fall(1) : 0.0)
                                + (dy == 1 ? 2.0 : 0.0);
                        out.add(new Edge(lx, ly, lz, MoveType.PARKOUR, cost));
                    }
                }
            }
        }
        if (Settings.allowFlying && t.canFly()) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        int tx = x + dx, ty = y + dy, tz = z + dz;
                        if (!t.free(tx, ty, tz) || !t.free(tx, ty + 1, tz)) continue;
                        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
                        out.add(new Edge(tx, ty, tz, MoveType.FLYING, len * Costs.FLY));
                    }
                }
            }
        }
    }
    private boolean isWaterSurface(int x, int y, int z) {
        return t.water(x, y - 1, z)
                && t.free(x, y, z)
                && !t.water(x, y, z)
                && t.free(x, y + 1, z);
    }
}
