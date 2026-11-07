package greed.client.module.combat;

import greed.client.RotationAngles;
import greed.client.RotationManager;
import greed.client.RotationSpeed;
import greed.client.event.EventListener;
import greed.client.event.PlayerTickEvent;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.ui.hud.NotificationHud;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

@ModuleInfo(name = "AutoCart", description = "Просто выстрели из лука и ты всё поймёшь", category = ModuleCategory.COMBAT)
public class AutoCartModule extends Module {
    private static AutoCartModule instance;

    private static final int STAGE_WAIT_ARROW = 10;
    private static final int STAGE_RAIL = 1;
    private static final int STAGE_CART = 2;
    private static final double MAX_REACH = 4.5;
    private static final int MAX_SIM_TICKS = 300;

    private boolean wasDrawingBow = false;
    private int lastUseTicks = 0;

    private int stage = 0;
    private int previousSlot = -1;
    private BlockHitResult railHit;
    private BlockPos railPos;
    private int waitTicks = 0;

    private EventListener<PlayerTickEvent> eventListener = event -> {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            wasDrawingBow = false;
            lastUseTicks = 0;
            resetState();
            return;
        }

        if (stage != 0) {
            runStage();
        }

        boolean drawing = mc.player.isUsingItem() && mc.player.getActiveItem().isOf(Items.BOW);

        if (drawing) {
            lastUseTicks = mc.player.getItemUseTime();
        } else if (wasDrawingBow) {
            float pull = BowItem.getPullProgress(lastUseTicks);

            if (pull >= 0.05F && pull < 0.15F) {
                onWeakShot();
            }

            lastUseTicks = 0;
        }

        wasDrawingBow = drawing;
    };

    public AutoCartModule() {

    }

    private void onWeakShot() {
        if (stage != 0) return;

        if (findRailSlot() == -1 || findCartSlot() == -1) {
            debug("нет рельс или вагонетки в хотбаре");
            return;
        }

        previousSlot = mc.player.getInventory().getSelectedSlot();
        waitTicks = 0;
        stage = STAGE_WAIT_ARROW;
    }

    private void runStage() {
        switch (stage) {
            case STAGE_WAIT_ARROW -> waitForArrow();
            case STAGE_RAIL -> {
                int slot = findRailSlot();
                if (slot == -1) {
                    finish();
                    return;
                }
                aimAt(railHit.getPos());
                mc.player.getInventory().setSelectedSlot(slot);
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, railHit);
                mc.player.swingHand(Hand.MAIN_HAND);
                stage = STAGE_CART;
            }
            case STAGE_CART -> {
                int slot = findCartSlot();
                if (slot == -1 || !mc.world.getBlockState(railPos).isIn(BlockTags.RAILS)) {
                    debug("рельсы не поставились");
                    finish();
                    return;
                }
                aimAt(cartPoint());
                mc.player.getInventory().setSelectedSlot(slot);
                BlockHitResult cartHit = new BlockHitResult(
                        new Vec3d(railPos.getX() + 0.5, railPos.getY() + 0.0625, railPos.getZ() + 0.5),
                        Direction.UP,
                        railPos,
                        false
                );
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, cartHit);
                mc.player.swingHand(Hand.MAIN_HAND);
                finish();
            }
            default -> finish();
        }
    }

    private void waitForArrow() {
        PersistentProjectileEntity arrow = findArrow();

        if (arrow == null) {
            if (++waitTicks > 20) {
                debug("стрела не найдена");
                finish();
            }
            return;
        }

        BlockHitResult landing = predictLanding(arrow);

        if (landing == null) {
            debug("не удалось предсказать падение стрелы");
            finish();
            return;
        }

        if (!setupPlacement(landing)) {
            debug("в точке падения нельзя поставить рельсы");
            finish();
            return;
        }

        if (mc.player.getEyePos().distanceTo(railHit.getPos()) > MAX_REACH) {
            debug("точка падения слишком далеко");
            finish();
            return;
        }

        stage = STAGE_RAIL;
    }

    private BlockHitResult predictLanding(PersistentProjectileEntity arrow) {
        Vec3d pos = new Vec3d(arrow.getX(), arrow.getY(), arrow.getZ());
        Vec3d vel = arrow.getVelocity();

        for (int i = 0; i < MAX_SIM_TICKS; i++) {
            Vec3d next = pos.add(vel);

            BlockHitResult hit = mc.world.raycast(new RaycastContext(
                    pos,
                    next,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    arrow
            ));

            if (hit.getType() == HitResult.Type.BLOCK) {
                return hit;
            }

            pos = next;

            if (pos.y < mc.world.getBottomY() - 10) {
                return null;
            }

            boolean water = mc.world.getFluidState(BlockPos.ofFloored(pos)).isIn(FluidTags.WATER);
            vel = vel.multiply(water ? 0.6 : 0.99).subtract(0.0, 0.05, 0.0);
        }

        return null;
    }

    private boolean setupPlacement(BlockHitResult hit) {
        BlockPos support;

        if (hit.getSide() == Direction.UP) {
            support = hit.getBlockPos();
        } else {
            BlockPos p = hit.getBlockPos().offset(hit.getSide());
            int steps = 0;

            while (steps < 6 && !isSolidTop(p.down())) {
                p = p.down();
                steps++;
            }

            support = p.down();
        }

        if (!isSolidTop(support)) return false;

        railPos = support.up();
        if (!mc.world.getBlockState(railPos).isReplaceable()) return false;

        Vec3d top = new Vec3d(support.getX() + 0.5, support.getY() + 1.0, support.getZ() + 0.5);
        railHit = new BlockHitResult(top, Direction.UP, support, false);
        return true;
    }

    private boolean isSolidTop(BlockPos pos) {
        return mc.world.getBlockState(pos).isSideSolidFullSquare(mc.world, pos, Direction.UP);
    }

    private PersistentProjectileEntity findArrow() {
        Box box = mc.player.getBoundingBox().expand(8.0);
        PersistentProjectileEntity best = null;

        for (PersistentProjectileEntity e : mc.world.getEntitiesByClass(
                PersistentProjectileEntity.class,
                box,
                p -> p.getOwner() == mc.player && p.age <= 5 && p.getVelocity().lengthSquared() > 0.01
        )) {
            if (best == null || e.age < best.age) {
                best = e;
            }
        }

        return best;
    }

    private Vec3d cartPoint() {
        return new Vec3d(railPos.getX() + 0.5, railPos.getY() + 0.1, railPos.getZ() + 0.5);
    }

    private void finish() {
        if (mc.player != null && previousSlot != -1) {
            mc.player.getInventory().setSelectedSlot(previousSlot);
        }
        resetState();
    }

    private void resetState() {
        previousSlot = -1;
        railHit = null;
        railPos = null;
        waitTicks = 0;
        stage = 0;
    }

    private int findCartSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isOf(Items.TNT_MINECART)) return i;
        }
        return -1;
    }

    private int findRailSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isOf(Items.RAIL) || stack.isOf(Items.POWERED_RAIL)) return i;
        }
        return -1;
    }

    protected void aimAt(Vec3d point) {
        if (mc.player == null) return;

        float[] angles = calculateRotations(point);
        float yawDelta = Math.abs(MathHelper.wrapDegrees(angles[0] - mc.player.getYaw()));
        float pitchDelta = Math.abs(angles[1] - mc.player.getPitch());

        RotationManager.getRotationManager().setRotation(
                new RotationAngles(angles[0], angles[1]),
                new RotationSpeed(yawDelta, pitchDelta, 360.0F, 360.0F),
                RotationManager.ReturnMode.Smooth
        );
    }

    protected float[] calculateRotations(Vec3d point) {
        Vec3d eye = mc.player.getEyePos();
        double dx = point.x - eye.x;
        double dy = point.y - eye.y;
        double dz = point.z - eye.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) MathHelper.clamp(-Math.toDegrees(Math.atan2(dy, horizontal)), -90.0, 90.0);
        return new float[]{yaw, pitch};
    }

    public static AutoCartModule getInstance() {
        if (instance == null) {
            instance = new AutoCartModule();
        }
        return instance;
    }

    private void debug(String msg) {
        if (mc.player != null) NotificationHud.notifyWithText( this.category.getIcon(),"AutoCart", Text.literal("[AutoCart] " + msg), false);
    }
}