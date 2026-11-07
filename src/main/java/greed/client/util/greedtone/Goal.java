package greed.client.util.greedtone;

import net.minecraft.util.math.BlockPos;

public interface Goal {

    boolean isEnd(int x, int y, int z);

    double heuristic(int x, int y, int z);

    static double horizontal(int dx, int dz) {
        int ax = Math.abs(dx);
        int az = Math.abs(dz);
        int diag = Math.min(ax, az);
        int straight = Math.max(ax, az) - diag;
        return (diag * Costs.SQRT2 + straight) * Costs.H_HORIZONTAL;
    }

    record AtBlock(BlockPos pos) implements Goal {
        @Override
        public boolean isEnd(int x, int y, int z) {
            return x == pos.getX() && y == pos.getY() && z == pos.getZ();
        }

        @Override
        public double heuristic(int x, int y, int z) {
            int dy = pos.getY() - y;
            return horizontal(pos.getX() - x, pos.getZ() - z) + (dy > 0 ? dy * Costs.H_UP : 0.0);
        }
    }

    record AtXZ(int gx, int gz) implements Goal {
        @Override
        public boolean isEnd(int x, int y, int z) {
            return x == gx && z == gz;
        }

        @Override
        public double heuristic(int x, int y, int z) {
            return horizontal(gx - x, gz - z);
        }
    }
}
