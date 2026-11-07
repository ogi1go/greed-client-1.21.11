package greed.client.module.combat.crystalmodules;

import greed.client.module.Module;
import greed.client.RotationAngles;
import greed.client.RotationManager;
import greed.client.RotationSpeed;
import greed.client.util.EntityUtils;
import greed.client.util.ServerRotation;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public abstract class CrystalModule extends Module {


    protected static final int MAX_AIM_TICKS = 20;

    protected boolean getItemFromHotBar(ItemStack targetStack) {
        if (mc.player == null) return false;

        for (int slot = 0; slot < 9; slot++) {
            ItemStack stackInSlot = mc.player.getInventory().getStack(slot);

            if (!stackInSlot.isEmpty() && stackInSlot.getItem() == targetStack.getItem()) {
                mc.player.getInventory().setSelectedSlot(slot);
                return true;
            }
        }
        return false;
    }
    protected boolean hasItem(ItemStack targetStack) {
        if (mc.player == null) return false;

        for (int slot = 0; slot < 9; slot++) {
            ItemStack stackInSlot = mc.player.getInventory().getStack(slot);

            if (!stackInSlot.isEmpty() && stackInSlot.getItem() == targetStack.getItem()) {
                return true;
            }
        }
        return false;
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

    protected void aimAt(BlockPos pos) {
        aimAt(pos.toCenterPos());
    }

    protected BlockHitResult raycastWithServerRotation() {
        if (mc.player == null || mc.world == null || !ServerRotation.isValid()) return null;

        Vec3d start = mc.player.getEyePos();
        Vec3d look = EntityUtils.getLookVector(ServerRotation.getYaw(), ServerRotation.getPitch());
        Vec3d end = start.add(look.multiply(mc.player.getBlockInteractionRange()));

        BlockHitResult hit = mc.world.raycast(new RaycastContext(
                start, end, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player
        ));
        return hit != null && hit.getType() == HitResult.Type.BLOCK ? hit : null;
    }


    protected BlockHitResult prepareBlockInteraction(BlockPos support, boolean topFaceOnly, boolean autoRotate) {
        if (mc.player == null || mc.world == null) return null;

        BlockHitResult hit = raycastWithServerRotation();
        if (hit != null
                && hit.getBlockPos().equals(support)
                && (!topFaceOnly || hit.getSide() == Direction.UP)) {
            return hit;
        }

        if (autoRotate) {
            aimAt(topFaceOnly ? support.toCenterPos().add(0.0, 0.5, 0.0) : support.toCenterPos());
        }
        return null;
    }


    protected boolean prepareEntityInteraction(Entity entity, double reach, boolean autoRotate) {
        if (mc.player == null || entity == null) return false;

        double effectiveReach = Math.min(reach, mc.player.getEntityInteractionRange());
        if (ServerRotation.isValid()
                && EntityUtils.isLookingAtEntity(entity, effectiveReach, ServerRotation.getYaw(), ServerRotation.getPitch())) {
            return true;
        }

        if (autoRotate) {
            aimAt(entity.getBoundingBox().getCenter());
        }
        return false;
    }

    /** true, если цель находится в реальной дистанции удара (той, что проверяет сервер). */
    protected boolean isWithinEntityReach(Entity entity) {
        if (mc.player == null || entity == null) return false;

        double reach = mc.player.getEntityInteractionRange();
        return mc.player.getEyePos().squaredDistanceTo(entity.getBoundingBox().getCenter()) <= reach * reach;
    }

    /** Считает yaw/pitch, которые направлены из глаз игрока в точку. */
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
}
