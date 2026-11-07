package greed.client.util.greedtone;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

public final class PathExecutor {

    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final Random RNG = new Random();

    private static CompletableFuture<Pathfinder.Result> pending;
    private static List<Step> path = List.of();
    private static Goal goal;
    private static int index;
    private static int stuckTicks;
    private static int stepTicks;
    private static int replans;
    private static int actionCooldown;
    private static boolean partial;
    private static double lastPartialH = Double.MAX_VALUE;
    private static Vec3d lastPos = Vec3d.ZERO;

    private static boolean eating;
    private static int eatTicks;
    private static int prevSlot;

    private static BlockPos aimKey;
    private static double aimA;
    private static double aimB;
    private static double aimC;
    private static boolean aimReady;
    private static BlockPos mineKey;
    private static int mineWait;

    private PathExecutor() {
    }

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
    }

    public static boolean isActive() {
        return goal != null;
    }

    public static void goTo(Goal target) {
        goal = target;
        replans = 0;
        lastPartialH = Double.MAX_VALUE;
        plan();
    }

    public static void stop() {
        goal = null;
        path = List.of();
        pending = null;
        if (eating) {
            eating = false;
            if (mc.options != null) {
                mc.options.useKey.setPressed(false);
            }
        }
        releaseKeys();
        if (mc.interactionManager != null) {
            mc.interactionManager.cancelBlockBreaking();
        }
    }

    private static void plan() {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null || goal == null) {
            return;
        }
        releaseKeys();
        Rotation.reset(p);
        path = List.of();
        index = 0;
        stuckTicks = 0;
        stepTicks = 0;
        BlockPos start = p.getBlockPos();
        if (goal.isEnd(start.getX(), start.getY(), start.getZ())) {
            msg("Ты уже на месте");
            stop();
            return;
        }
        Snapshot snapshot = Snapshot.capture(mc);
        Goal g = goal;
        pending = CompletableFuture.supplyAsync(() -> new Pathfinder(new Terrain(snapshot), g).find(start));
    }

    private static void replan() {
        if (++replans > Settings.maxReplans) {
            msg("Не получается дойти");
            stop();
            return;
        }
        plan();
    }

    private static void tick() {
        if (goal == null) {
            return;
        }
        ClientPlayerEntity p = mc.player;
        ClientWorld w = mc.world;
        if (p == null || w == null || mc.interactionManager == null) {
            stop();
            return;
        }
        if (actionCooldown > 0) {
            actionCooldown--;
        }

        if (handleEating(p)) {
            stuckTicks = 0;
            lastPos = pos(p);
            return;
        }

        if (pending != null) {
            if (!pending.isDone()) {
                return;
            }
            Pathfinder.Result r;
            try {
                r = pending.get();
            } catch (Exception e) {
                msg("Ошибка поиска пути");
                stop();
                return;
            }
            pending = null;
            if (r.path().size() < 2) {
                msg("Путь не найден");
                stop();
                return;
            }
            path = r.path();
            index = 1;
            partial = !r.reachedGoal();
            lastPos = pos(p);
            stuckTicks = 0;
            stepTicks = 0;
            if (partial && replans == 0) {
                msg("Полный путь не найден, иду к ближайшей точке");
            }
        }

        if (path.isEmpty()) {
            return;
        }

        if (index >= path.size()) {
            releaseKeys();
            BlockPos b = p.getBlockPos();
            if (!partial || goal.isEnd(b.getX(), b.getY(), b.getZ())) {
                msg("Прибыл");
                stop();
                return;
            }
            double h = goal.heuristic(b.getX(), b.getY(), b.getZ());
            if (h >= lastPartialH - 1.0) {
                msg("Цель недостижима, дальше пути нет");
                stop();
                return;
            }
            lastPartialH = h;
            replan();
            return;
        }

        Step step = path.get(index);
        Step next = index + 1 < path.size() ? path.get(index + 1) : null;
        BlockPos from = path.get(index - 1).pos();
        BlockPos to = step.pos();
        double dx = to.getX() + 0.5 - p.getX();
        double dz = to.getZ() + 0.5 - p.getZ();
        double dist = Math.hypot(dx, dz);
        boolean last = index == path.size() - 1;

        if (arrived(p, from, to, dist, last, step.type(), next == null ? null : next.type())) {
            index++;
            stepTicks = 0;
            stuckTicks = 0;
            lastPos = pos(p);
            return;
        }

        if (++stepTicks > 600 || pos(p).distanceTo(Vec3d.ofCenter(to)) > 8.0) {
            replan();
            return;
        }

        if (handleCells(p, w, step.type(), from, to)) {
            stuckTicks = 0;
            lastPos = pos(p);
            return;
        }
        if (goal == null) {
            return;
        }

        drive(p, step.type(), from, to, next, dist, last);
        if (goal == null) {
            return;
        }

        Vec3d now = pos(p);
        if (now.squaredDistanceTo(lastPos) < 0.0004) {
            if (++stuckTicks > 40) {
                stuckTicks = 0;
                replan();
            }
        } else {
            stuckTicks = 0;
            lastPos = now;
        }
    }

    private static boolean handleEating(ClientPlayerEntity p) {
        if (!Settings.autoEat || p.isCreative()) {
            return false;
        }
        if (eating) {
            eatTicks++;
            if ((eatTicks > 4 && !p.isUsingItem()) || eatTicks > 100) {
                eating = false;
                mc.options.useKey.setPressed(false);
                p.getInventory().setSelectedSlot(prevSlot);
                return false;
            }
            keys(false, false, false, false);
            mc.options.useKey.setPressed(true);
            return true;
        }
        if (p.getHungerManager().getFoodLevel() > Settings.eatBelow) {
            return false;
        }
        if (!(p.isOnGround() || p.isTouchingWater())) {
            return false;
        }
        int slot = findFood(p);
        if (slot < 0) {
            return false;
        }
        prevSlot = p.getInventory().getSelectedSlot();
        p.getInventory().setSelectedSlot(slot);
        mc.interactionManager.cancelBlockBreaking();
        eating = true;
        eatTicks = 0;
        keys(false, false, false, false);
        mc.options.useKey.setPressed(true);
        return true;
    }

    private static int findFood(ClientPlayerEntity p) {
        int best = -1;
        int bestNutrition = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = p.getInventory().getStack(i);
            if (stack.isEmpty() || Settings.badFood.contains(stack.getItem())) {
                continue;
            }
            FoodComponent food = stack.get(DataComponentTypes.FOOD);
            if (food == null) {
                continue;
            }
            if (food.nutrition() > bestNutrition) {
                bestNutrition = food.nutrition();
                best = i;
            }
        }
        return best;
    }

    private static boolean arrived(ClientPlayerEntity p, BlockPos from, BlockPos to, double dist, boolean last, MoveType type, MoveType nextType) {
        boolean cell = p.getBlockPos().equals(to);
        boolean level = Math.abs(p.getY() - to.getY()) < 0.7;
        boolean near = dist < (last ? 0.3 : 0.4) && level;
        boolean support = p.isOnGround() || p.isTouchingWater() || p.isClimbing();
        boolean vertical = nextType == MoveType.CLIMB_UP || nextType == MoveType.CLIMB_DOWN
                || nextType == MoveType.SWIM_UP || nextType == MoveType.SWIM_DOWN;
        int sx = Integer.signum(to.getX() - from.getX());
        int sz = Integer.signum(to.getZ() - from.getZ());
        double past = (p.getX() - (to.getX() + 0.5)) * sx + (p.getZ() - (to.getZ() + 0.5)) * sz;
        boolean passed = !last && !vertical && level && dist < 1.2 && past >= 0.0;
        return switch (type) {
            case CLIMB_UP, CLIMB_DOWN, SWIM_UP, SWIM_DOWN -> cell && dist < 0.9;
            case PILLAR, DIG_DOWN, DESCEND -> (cell || near) && support;
            case PARKOUR -> (cell || near) && p.isOnGround();
            case FLYING -> {
                double hd = Math.hypot(p.getX() - (to.getX() + 0.5),
                        p.getZ() - (to.getZ() + 0.5));
                double vd = Math.abs(p.getY() - to.getY());
                if (hd < 0.6 && vd < 0.7) yield true;
                if (last) yield false;

                int sy = Integer.signum(to.getY() - from.getY());

                double px = (p.getX() - (to.getX() + 0.5)) * sx;
                double py = (p.getY() - to.getY()) * sy;
                double pz = (p.getZ() - (to.getZ() + 0.5)) * sz;
                boolean pastt = (sx == 0 || px >= 0)
                        && (sy == 0 || py >= 0)
                        && (sz == 0 || pz >= 0);
                yield pastt && hd < 1.5 && vd < 1.2;
            }
            default -> near || passed || (vertical && cell);
        };
    }

    private static List<BlockPos> cells(MoveType type, BlockPos from, BlockPos to) {
        List<BlockPos> list = new ArrayList<>(4);
        switch (type) {
            case TRAVERSE, BRIDGE -> {
                list.add(to.up());
                list.add(to);
            }
            case ASCEND -> {
                list.add(from.up(2));
                list.add(to.up());
                list.add(to);
            }
            case DESCEND -> {
                list.add(new BlockPos(to.getX(), from.getY() + 1, to.getZ()));
                list.add(new BlockPos(to.getX(), from.getY(), to.getZ()));
            }
            case PILLAR -> list.add(from.up(2));
            case DIG_DOWN -> list.add(to);
            default -> {
            }
        }
        return list;
    }

    private static boolean handleCells(ClientPlayerEntity p, ClientWorld w, MoveType type, BlockPos from, BlockPos to) {
        for (BlockPos c : cells(type, from, to)) {
            BlockState s = w.getBlockState(c);
            if (Terrain.isDoor(s)) {
                if (Terrain.isClosed(s) && reach(p, c)) {
                    rollAim(c);
                    boolean ready = aimAt(p, Vec3d.ofCenter(c).add(aimA * 0.4, 0.0, aimC * 0.4));
                    keys(false, false, false, false);
                    if (ready && actionCooldown == 0) {
                        interact(p, c, Direction.UP, Vec3d.ofCenter(c));
                        actionCooldown = 6;
                    }
                    return true;
                }
                continue;
            }
            if (Terrain.blocking(s, w, c)) {
                if (!Settings.allowBreak || !reach(p, c)) {
                    continue;
                }
                mine(p, c, s);
                return true;
            }
        }
        return false;
    }

    private static boolean walking(MoveType type) {
        return type == MoveType.TRAVERSE || type == MoveType.DIAGONAL;
    }

    private static boolean faceMove(ClientPlayerEntity p, double dx, double dz, boolean falling) {
        double aim = Math.hypot(dx, dz);
        if (falling || aim < 0.3) {
            Rotation.hold(p, Settings.walkPitch, Settings.turnSpeed);
            return true;
        }
        float yaw = Rotation.yawTo(dx, dz);
        float speed = aim < 2.0 ? Settings.turnSpeed * 2.0f : Settings.turnSpeed;
        Rotation.look(p, yaw, Settings.walkPitch, speed, false);
        return Rotation.yawError(p, yaw) < (aim < 1.5 ? 25.0f : 50.0f);
    }

    private static void drive(ClientPlayerEntity p, MoveType type, BlockPos from, BlockPos to, Step next, double dist, boolean last) {
        boolean ground = p.isOnGround();
        boolean water = p.isTouchingWater();
        boolean falling = !ground && !water && !p.isClimbing() && p.getVelocity().y < -0.15;

        double tx = to.getX() + 0.5;
        double tz = to.getZ() + 0.5;
        if (next != null && dist < 0.9 && walking(type) && walking(next.type()) && next.pos().getY() == to.getY()) {
            double wgt = 0.3 * (1.0 - dist / 0.9);
            tx += (next.pos().getX() + 0.5 - tx) * wgt;
            tz += (next.pos().getZ() + 0.5 - tz) * wgt;
        }
        double dx = tx - p.getX();
        double dz = tz - p.getZ();

        int sx = Integer.signum(to.getX() - from.getX());
        int sz = Integer.signum(to.getZ() - from.getZ());
        double along = (p.getX() - (from.getX() + 0.5)) * sx + (p.getZ() - (from.getZ() + 0.5)) * sz;
        boolean push = dist > 0.25 && !(type == MoveType.DESCEND && falling);
        boolean sprint = push && !(last && dist < 1.5);
        boolean jump = ground && p.horizontalCollision;
        boolean sneak = false;
        if (water) {
            double ty = to.getY() + 0.3;
            jump = p.getY() < ty - 0.2 || p.horizontalCollision;
            sneak = p.getY() > ty + 0.8;
        }

        switch (type) {
            case TRAVERSE, DIAGONAL, DESCEND -> {
                boolean ok = faceMove(p, dx, dz, falling);
                keys(push && ok, sprint && ok, jump, sneak);
            }
            case ASCEND -> {
                boolean ok = faceMove(p, dx, dz, false);
                boolean up = water ? jump : ground && (along >= 0.1 || p.horizontalCollision);
                keys(ok, ok, up && ok, false);
            }
            case PARKOUR -> {
                boolean ok = faceMove(p, dx, dz, false);
                boolean exact = Rotation.yawError(p, Rotation.yawTo(dx, dz)) < 12.0f;
                keys(ok, ok, ground && exact && along >= 0.3, false);
            }
            case CLIMB_UP -> {
                boolean ok = faceMove(p, dx, dz, false);
                keys(dist > 0.45 && ok, false, true, false);
            }
            case CLIMB_DOWN -> {
                boolean ok = faceMove(p, dx, dz, false);
                keys(dist > 0.45 && ok, false, false, false);
            }
            case SWIM_UP -> {
                boolean ok = faceMove(p, dx, dz, false);
                keys(dist > 0.3 && ok, false, true, false);
            }
            case SWIM_DOWN -> {
                boolean ok = faceMove(p, dx, dz, false);
                keys(dist > 0.3 && ok, false, false, true);
            }
            case BRIDGE -> {
                BlockPos floorCell = new BlockPos(to.getX(), from.getY() - 1, to.getZ());
                if (replaceable(floorCell)) {
                    keys(false, false, false, false);
                    place(p, from.down(), dirOf(from, to), true);
                } else {
                    boolean ok = faceMove(p, dx, dz, false);
                    keys(push && ok, false, jump, false);
                }
            }
            case PILLAR -> {
                if (ground && dist > 0.25) {
                    boolean ok = faceMove(p, dx, dz, false);
                    keys(ok, false, false, false);
                } else {
                    boolean doIt = !ground && p.getY() >= from.getY() + 1.0 && replaceable(from);
                    place(p, from.down(), Direction.UP, doIt);
                    keys(false, false, ground && aimReady, false);
                }
            }
            case DIG_DOWN -> {
                boolean ok = faceMove(p, dx, dz, false);
                keys(dist > 0.3 && ground && ok, false, false, false);
            }
            case FLYING -> {
                p.getAbilities().flying = true;
                double blockDy = to.getY() - p.getY();
                float yaw = Rotation.yawTo(dx, dz);
                float pitch = Rotation.pitchTo(dx, blockDy, dz);
                Rotation.look(p, yaw, pitch, Settings.turnSpeed, true);
                keys(true, true, false, false);   // всегда W + спринт
            }
        }
    }

    private static void rollAim(BlockPos key) {
        if (!key.equals(aimKey)) {
            aimKey = key;
            aimA = (RNG.nextDouble() - 0.5) * 0.5;
            aimB = (RNG.nextDouble() - 0.5) * 0.5;
            aimC = (RNG.nextDouble() - 0.5) * 0.5;
        }
    }

    private static boolean aimAt(ClientPlayerEntity p, Vec3d point) {
        Vec3d e = p.getEyePos();
        double dx = point.x - e.x;
        double dy = point.y - e.y;
        double dz = point.z - e.z;
        float yaw = Math.hypot(dx, dz) > 0.1 ? Rotation.yawTo(dx, dz) : Rotation.baseYaw();
        float pitch = Rotation.pitchTo(dx, dy, dz);
        aimReady = Rotation.look(p, yaw, pitch, Settings.aimSpeed, true);
        return aimReady;
    }

    private static boolean looking(ClientPlayerEntity p, BlockPos pos) {
        HitResult hit = p.raycast(5.0, 1.0f, false);
        return hit.getType() == HitResult.Type.BLOCK && ((BlockHitResult) hit).getBlockPos().equals(pos);
    }

    private static void mine(ClientPlayerEntity p, BlockPos pos, BlockState state) {
        if (!pos.equals(mineKey)) {
            mineKey = pos;
            mineWait = 0;
        }
        selectTool(p, state);
        rollAim(pos);
        boolean ready = aimAt(p, Vec3d.ofCenter(pos).add(aimA, aimB, aimC));
        keys(false, false, false, false);
        if (!ready) {
            return;
        }
        if (!looking(p, pos) && mineWait++ < 30) {
            return;
        }
        Vec3d c = Vec3d.ofCenter(pos);
        Vec3d e = p.getEyePos();
        Direction side = Direction.getFacing(e.x - c.x, e.y - c.y, e.z - c.z);
        if (mc.interactionManager.updateBlockBreakingProgress(pos, side)) {
            p.swingHand(Hand.MAIN_HAND);
        }
    }

    private static boolean place(ClientPlayerEntity p, BlockPos against, Direction face, boolean doIt) {
        if (!selectPlaceable(p)) {
            msg("Нет блоков для установки");
            stop();
            return false;
        }
        rollAim(against);
        Vec3d c = Vec3d.ofCenter(against).add(face.getOffsetX() * 0.5, face.getOffsetY() * 0.5, face.getOffsetZ() * 0.5);
        Vec3d hit = switch (face.getAxis()) {
            case X -> c.add(0.0, aimB, aimC);
            case Y -> c.add(aimA, 0.0, aimC);
            case Z -> c.add(aimA, aimB, 0.0);
        };
        boolean ready = aimAt(p, hit);
        if (!ready || !doIt) {
            return false;
        }
        return interact(p, against, face, hit);
    }

    private static boolean interact(ClientPlayerEntity p, BlockPos pos, Direction face, Vec3d hit) {
        ActionResult r = mc.interactionManager.interactBlock(p, Hand.MAIN_HAND, new BlockHitResult(hit, face, pos, false));
        if (r.isAccepted()) {
            p.swingHand(Hand.MAIN_HAND);
            return true;
        }
        return false;
    }

    private static void selectTool(ClientPlayerEntity p, BlockState state) {
        int best = p.getInventory().getSelectedSlot();
        float bestSpeed = -1.0f;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = p.getInventory().getStack(i);
            float speed = stack.getMiningSpeedMultiplier(state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                best = i;
            }
        }
        p.getInventory().setSelectedSlot(best);
    }

    private static boolean selectPlaceable(ClientPlayerEntity p) {
        for (int i = 0; i < 9; i++) {
            if (Snapshot.isThrowaway(p.getInventory().getStack(i))) {
                p.getInventory().setSelectedSlot(i);
                return true;
            }
        }
        return false;
    }

    private static boolean replaceable(BlockPos pos) {
        BlockState s = mc.world.getBlockState(pos);
        return s.isAir() || (s.isReplaceable() && s.getFluidState().isEmpty());
    }

    private static boolean reach(ClientPlayerEntity p, BlockPos pos) {
        return p.getEyePos().squaredDistanceTo(Vec3d.ofCenter(pos)) <= 20.0;
    }

    private static Direction dirOf(BlockPos from, BlockPos to) {
        if (to.getX() > from.getX()) {
            return Direction.EAST;
        }
        if (to.getX() < from.getX()) {
            return Direction.WEST;
        }
        if (to.getZ() > from.getZ()) {
            return Direction.SOUTH;
        }
        return Direction.NORTH;
    }

    private static void keys(boolean forward, boolean sprint, boolean jump, boolean sneak) {
        GameOptions o = mc.options;
        o.forwardKey.setPressed(forward);
        o.sprintKey.setPressed(sprint);
        o.jumpKey.setPressed(jump);
        o.sneakKey.setPressed(sneak);
    }

    private static void releaseKeys() {
        if (mc.options != null) {
            keys(false, false, false, false);
        }
    }

    private static Vec3d pos(ClientPlayerEntity p) {
        return new Vec3d(p.getX(), p.getY(), p.getZ());
    }

    private static void msg(String s) {
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal("[nav] " + s), false);
        }
    }
}
