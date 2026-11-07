package greed.client.util.greedtone;

import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FallingBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.chunk.WorldChunk;

import java.util.Set;

public final class Terrain {

    public static final double INF = Double.POSITIVE_INFINITY;

    private static final Set<Block> HAZARD = Set.of(
            Blocks.FIRE,
            Blocks.SOUL_FIRE,
            Blocks.SWEET_BERRY_BUSH,
            Blocks.COBWEB,
            Blocks.POWDER_SNOW,
            Blocks.WITHER_ROSE,
            Blocks.BUBBLE_COLUMN,
            Blocks.NETHER_PORTAL,
            Blocks.END_PORTAL,
            Blocks.END_GATEWAY
    );

    private static final Set<Block> BAD_FLOOR = Set.of(
            Blocks.MAGMA_BLOCK,
            Blocks.CACTUS,
            Blocks.CAMPFIRE,
            Blocks.SOUL_CAMPFIRE
    );

    private static final int[][] NEIGHBORS = {
            {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}
    };

    private final Snapshot snap;
    private final Long2IntOpenHashMap flagCache = new Long2IntOpenHashMap();
    private final Long2DoubleOpenHashMap breakCache = new Long2DoubleOpenHashMap();
    private final BlockPos.Mutable mut = new BlockPos.Mutable();

    public Terrain(Snapshot snap) {
        this.snap = snap;
        flagCache.defaultReturnValue(-1);
        breakCache.defaultReturnValue(-1.0);
    }

    public static boolean isDoor(BlockState state) {
        return (state.isIn(BlockTags.DOORS) && !state.isOf(Blocks.IRON_DOOR))
                || state.getBlock() instanceof FenceGateBlock;
    }

    public static boolean isClosed(BlockState state) {
        return state.contains(Properties.OPEN) && !state.get(Properties.OPEN);
    }

    public static double height(BlockState state, BlockView view, BlockPos pos) {
        VoxelShape shape = state.getCollisionShape(view, pos);
        return shape.isEmpty() ? 0.0 : shape.getMax(Direction.Axis.Y);
    }

    public static boolean blocking(BlockState state, BlockView view, BlockPos pos) {
        if (state.isIn(BlockTags.CLIMBABLE) || isDoor(state)) {
            return false;
        }
        return height(state, view, pos) > 0.25;
    }

    public int flags(int x, int y, int z) {
        long key = BlockPos.asLong(x, y, z);
        int cached = flagCache.get(key);
        if (cached != -1) {
            return cached;
        }
        int computed = compute(x, y, z);
        flagCache.put(key, computed);
        return computed;
    }

    private int compute(int x, int y, int z) {
        WorldChunk chunk = snap.chunk(x >> 4, z >> 4);
        if (chunk == null) {
            return Flags.UNKNOWN;
        }
        int bottom = chunk.getBottomY();
        if (y < bottom) {
            return Flags.UNKNOWN;
        }
        if (y >= bottom + chunk.getHeight()) {
            return Flags.AIR;
        }
        mut.set(x, y, z);
        BlockState state = chunk.getBlockState(mut);
        Block block = state.getBlock();
        FluidState fluid = state.getFluidState();
        int f = 0;
        if (state.isAir()) {
            f |= Flags.AIR;
        }
        if (!fluid.isEmpty()) {
            f |= Flags.FLUID;
            if (fluid.isIn(FluidTags.WATER)) {
                f |= Flags.WATER;
            }
            if (fluid.isIn(FluidTags.LAVA)) {
                f |= Flags.LAVA;
            }
        }
        boolean climb = state.isIn(BlockTags.CLIMBABLE);
        if (climb) {
            f |= Flags.CLIMB;
        }
        if (isDoor(state)) {
            f |= Flags.DOOR;
            if (isClosed(state)) {
                f |= Flags.DOOR_CLOSED;
            }
        }
        if (HAZARD.contains(block)) {
            f |= Flags.HAZARD;
        }
        if (BAD_FLOOR.contains(block)) {
            f |= Flags.FLOOR_BAD;
        }
        if (block instanceof FallingBlock) {
            f |= Flags.FALLING;
        }
        if (state.isIn(BlockTags.ICE)) {
            f |= Flags.ICE;
        }
        if (block == Blocks.SOUL_SAND || block == Blocks.HONEY_BLOCK) {
            f |= Flags.SLOW;
        }
        double h = height(state, chunk, mut);
        if (h > 0.25) {
            f |= Flags.SOLID;
        }
        if (h >= 0.5 && h <= 1.0001) {
            f |= Flags.STAND;
        }
        if (!climb && fluid.isEmpty() && (state.isAir() || state.isReplaceable())) {
            f |= Flags.REPLACEABLE;
        }
        return f;
    }

    public boolean free(int x, int y, int z) {
        int f = flags(x, y, z);
        if ((f & (Flags.UNKNOWN | Flags.LAVA | Flags.HAZARD)) != 0) {
            return false;
        }
        if ((f & Flags.WATER) != 0 && !Settings.allowWater) {
            return false;
        }
        return (f & Flags.SOLID) == 0 || (f & (Flags.CLIMB | Flags.DOOR)) != 0;
    }

    public boolean strictFree(int x, int y, int z) {
        return free(x, y, z) && (flags(x, y, z) & (Flags.DOOR | Flags.CLIMB)) == 0;
    }

    public boolean hasFloor(int x, int y, int z) {
        int f = flags(x, y, z);
        return (f & (Flags.UNKNOWN | Flags.FLOOR_BAD | Flags.CLIMB | Flags.DOOR)) == 0
                && (f & Flags.STAND) != 0;
    }

    public boolean water(int x, int y, int z) {
        return (flags(x, y, z) & Flags.WATER) != 0;
    }

    public boolean climb(int x, int y, int z) {
        return (flags(x, y, z) & Flags.CLIMB) != 0;
    }

    public boolean door(int x, int y, int z) {
        return ((flags(x, y, z) | flags(x, y + 1, z)) & Flags.DOOR) != 0;
    }

    public boolean replaceable(int x, int y, int z) {
        int f = flags(x, y, z);
        return (f & Flags.UNKNOWN) == 0 && (f & Flags.REPLACEABLE) != 0;
    }

    public boolean supported(int x, int y, int z) {
        return hasFloor(x, y - 1, z) || (flags(x, y, z) & (Flags.WATER | Flags.CLIMB)) != 0;
    }

    public boolean canOccupy(int x, int y, int z) {
        return free(x, y, z) && free(x, y + 1, z) && supported(x, y, z);
    }

    public boolean canPlace() {
        return Settings.allowPlace && snap.placeable();
    }

    public double clearCost(int x, int y, int z) {
        if (free(x, y, z)) {
            return 0.0;
        }
        return breakCost(x, y, z);
    }

    public double breakCost(int x, int y, int z) {
        long key = BlockPos.asLong(x, y, z);
        double cached = breakCache.get(key);
        if (cached != -1.0) {
            return cached;
        }
        double computed = computeBreak(x, y, z);
        breakCache.put(key, computed);
        return computed;
    }

    private double computeBreak(int x, int y, int z) {
        if (!Settings.allowBreak) {
            return INF;
        }
        int f = flags(x, y, z);
        if ((f & (Flags.UNKNOWN | Flags.FLUID | Flags.DOOR | Flags.CLIMB | Flags.HAZARD)) != 0
                || (f & Flags.SOLID) == 0) {
            return INF;
        }
        if ((flags(x, y + 1, z) & Flags.FALLING) != 0) {
            return INF;
        }
        for (int[] n : NEIGHBORS) {
            int nf = flags(x + n[0], y + n[1], z + n[2]);
            if ((nf & (Flags.FLUID | Flags.UNKNOWN)) != 0) {
                return INF;
            }
        }
        WorldChunk chunk = snap.chunk(x >> 4, z >> 4);
        mut.set(x, y, z);
        BlockState state = chunk.getBlockState(mut);
        float hardness = state.getHardness(chunk, mut);
        if (hardness < 0) {
            return INF;
        }
        double speed = 1.0;
        boolean suitable = !state.isToolRequired();
        for (ItemStack stack : snap.hotbar()) {
            speed = Math.max(speed, stack.getMiningSpeedMultiplier(state));
            if (!suitable && stack.isSuitableFor(state)) {
                suitable = true;
            }
        }
        double ticks = Math.max(1.0, hardness * (suitable ? 30.0 : 100.0) / speed);
        return ticks + Costs.BREAK_PENALTY;
    }
    public boolean canFly() { return snap.canFly(); }
}
