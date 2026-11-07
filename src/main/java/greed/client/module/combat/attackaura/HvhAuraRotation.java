
package greed.client.module.combat.attackaura;

import greed.client.MinecraftAccess;
import greed.client.RotationAngles;
import greed.client.RotationManager;
import greed.client.RotationSpeed;
import greed.client.module.combat.AttackAuraModule;
import greed.client.module.combat.MaceHelperModule;
import greed.client.util.MathUtils;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class HvhAuraRotation implements MinecraftAccess {
    private boolean isRotationActive;

    public void update(LivingEntity livingEntity, boolean targetChanged, float attackRange) {
        if (livingEntity != null && mc.player != null && mc.world != null) {
            Vec3d eyePos = mc.player.getEyePos();


            Box box = livingEntity.getBoundingBox();
            Vec3d targetVec = new Vec3d(
                    (box.minX + box.maxX) * 0.5,
                    box.minY + (box.maxY - box.minY) * 0.45,
                    (box.minZ + box.maxZ) * 0.5
            );

            Vec3d diff = targetVec.subtract(eyePos);


            float targetYaw = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0);
            float targetPitch = (float) MathHelper.clamp(-Math.toDegrees(Math.atan2(diff.y, Math.hypot(diff.x, diff.z))), -90.0, 90.0);



            RotationAngles rotationAngles = new RotationAngles(targetYaw, targetPitch);

            RotationManager.getRotationManager()
                    .setRotation(
                            rotationAngles,
                            new RotationSpeed(360.0F, 360.0F, 180.0F, 180.0F), // Максимальная скорость по Yaw и Pitch
                            RotationManager.ReturnMode.SmoothFast
                    );
        }
    }

    public void setActive(boolean active) {
        this.isRotationActive = active;
    }

    public void reset() {
        this.isRotationActive = false;
    }
}