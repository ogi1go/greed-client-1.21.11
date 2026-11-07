
package greed.client.module.combat.attackaura;

import greed.client.MinecraftAccess;
import greed.client.RotationAngles;
import greed.client.RotationManager;
import greed.client.RotationSpeed;
import greed.client.module.combat.AttackAuraModule;
import greed.client.util.MathUtils;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class OreMineAuraRotation implements MinecraftAccess {

    private boolean rotationActive;
    private float yawOffset;
    private float pitchOffset;
    private float previousYaw;
    private float previousPitch;
    private float yawNoise;
    private float pitchNoise;
    private float yawNoiseTarget;
    private float pitchNoiseTarget;
    private long lastTargetSwitchTime;
    private long targetSwitchDelay;
    private long lastNoiseUpdate;

    private float calculateSpeedFactor() {
        float cooldownProgress = mc.player.getAttackCooldownProgress(1.0F);
        double attackSpeed = mc.player.getAttributeValue(EntityAttributes.ATTACK_SPEED);

        if (attackSpeed > 1.6) {
            cooldownProgress *= (float) (1.6 / attackSpeed);
        }

        float cooldownFactor;

        if (attackSpeed < 2.0) {
            cooldownFactor = 1.70158F * cooldownProgress * cooldownProgress * cooldownProgress
                    - 0.70158F * cooldownProgress * cooldownProgress;
        } else if (cooldownProgress < 0.5F) {
            cooldownFactor = 8.0F * cooldownProgress * cooldownProgress
                    * cooldownProgress * cooldownProgress;
        } else {
            float difference = cooldownProgress - 1.0F;
            cooldownFactor = 1.0F - 8.0F * difference * difference * difference * difference;
        }

        return (1.0F + 29.0F * cooldownFactor) / 50.0F;
    }

    private void updateNoise(long currentTime) {
        if (currentTime - lastNoiseUpdate >= 80L) {
            yawNoiseTarget = MathUtils.randomFloat(-0.95F, 1.35F);
            pitchNoiseTarget = MathUtils.randomFloat(-0.2F, 1.2F);
            lastNoiseUpdate = currentTime;
        }

        yawNoise += (yawNoiseTarget - yawNoise) * 0.58F;
        pitchNoise += (pitchNoiseTarget - pitchNoise) * 0.18F;
    }

    public void update(LivingEntity target, boolean targetSwitched, float maxDistance) {
        if (target == null || mc.player == null || mc.world == null) {
            return;
        }

        Vec3d eyePos = mc.player.getEyePos();

        Vec3d targetMovement = new Vec3d(
                target.getX() - target.lastRenderX,
                target.getY() - target.lastRenderY,
                target.getZ() - target.lastRenderZ
        );

        Box predictedBox = target.getBoundingBox()
                .offset(
                        targetMovement.lengthSquared() > 0.36
                                ? targetMovement.normalize().multiply(0.6)
                                : targetMovement
                )
                .contract(0.08);

        double minY = predictedBox.minY + 0.15;
        double maxY = Math.max(
                minY,
                predictedBox.minY + predictedBox.getLengthY() * 0.7
        );

        Vec3d closestPoint = new Vec3d(
                MathHelper.clamp(eyePos.x, predictedBox.minX, predictedBox.maxX),
                MathHelper.clamp(eyePos.y, minY, maxY),
                MathHelper.clamp(eyePos.z, predictedBox.minZ, predictedBox.maxZ)
        );

        Vec3d horizontalDirection = new Vec3d(
                (predictedBox.minX + predictedBox.maxX) * 0.5 - closestPoint.x,
                0.0,
                (predictedBox.minZ + predictedBox.maxZ) * 0.5 - closestPoint.z
        );

        double forwardOffset = MathHelper.clamp(
                (maxDistance - 0.15 - eyePos.distanceTo(closestPoint))
                        / Math.max(0.05, horizontalDirection.length()),
                0.0,
                0.8
        );

        Vec3d aimPoint = closestPoint.add(horizontalDirection.multiply(forwardOffset));

        Box targetBox = target.getBoundingBox().contract(0.1);

        Vec3d direction = new Vec3d(
                MathHelper.clamp(aimPoint.x, targetBox.minX, targetBox.maxX),
                MathHelper.clamp(aimPoint.y, targetBox.minY, targetBox.maxY),
                MathHelper.clamp(aimPoint.z, targetBox.minZ, targetBox.maxZ)
        ).subtract(eyePos);

        float targetYaw = (float) MathHelper.wrapDegrees(
                Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0
        );

        float targetPitch = (float) MathHelper.clamp(
                -Math.toDegrees(
                        Math.atan2(
                                direction.y,
                                Math.hypot(direction.x, direction.z)
                        )
                ),
                -90.0,
                90.0
        );

        float playerYaw = mc.player.getYaw();
        float playerPitch = mc.player.getPitch();

        long currentTime = System.currentTimeMillis();

        if (targetSwitched) {
            yawOffset = 0.0F;
            pitchOffset = 0.0F;
            yawNoise = 0.0F;
            pitchNoise = 0.0F;
            yawNoiseTarget = 0.0F;
            pitchNoiseTarget = 0.0F;
            lastTargetSwitchTime = currentTime;
            targetSwitchDelay = (long) MathUtils.randomFloat(30.0F, 60.0F);
            previousYaw = targetYaw;
            previousPitch = targetPitch;
            lastNoiseUpdate = currentTime;
        } else if (currentTime - lastTargetSwitchTime < targetSwitchDelay) {
            targetYaw = previousYaw;
            targetPitch = previousPitch;
        }

        updateNoise(currentTime);

        float cooldownProgress = mc.player.getAttackCooldownProgress(1.0F);
        float speedFactor = MathHelper.clamp(
                calculateSpeedFactor() * 1.8F,
                0.35F,
                1.0F
        );

        float yawDifference = MathHelper.wrapDegrees(targetYaw - playerYaw);
        float pitchDifference = targetPitch - playerPitch;

        float desiredYaw = yawDifference + yawNoise;
        float desiredPitch = pitchDifference + pitchNoise;

        float yawSpeed = (rotationActive ? 0.65F : 0.35F) * speedFactor;
        float pitchSpeed = (rotationActive ? 0.75F : 0.6F) * speedFactor;

        float maxYawStep = 19.0F + cooldownProgress * 25.0F;
        float maxPitchStep = 10.0F + cooldownProgress * 15.0F;

        float yawStep = MathHelper.clamp(
                desiredYaw * yawSpeed - yawOffset,
                -maxYawStep,
                maxYawStep
        );

        float pitchStep = MathHelper.clamp(
                desiredPitch * pitchSpeed - pitchOffset,
                -maxPitchStep,
                maxPitchStep
        );

        yawOffset += yawStep;
        pitchOffset += pitchStep;

        yawOffset = MathHelper.clamp(
                yawOffset,
                -Math.abs(yawDifference),
                Math.abs(yawDifference)
        );

        pitchOffset = MathHelper.clamp(
                pitchOffset,
                -Math.abs(pitchDifference),
                Math.abs(pitchDifference)
        );
        if (!isWithinFov(targetYaw, mc.player.getYaw())) {
            return;
        }
        RotationAngles rotation = new RotationAngles(
                playerYaw + yawOffset,
                MathHelper.clamp(playerPitch + pitchOffset, -90.0F, 90.0F)
        );

        RotationManager.getRotationManager().setRotation(
                rotation,
                new RotationSpeed(
                        Math.abs(MathHelper.wrapDegrees(rotation.getYaw() - playerYaw)),
                        Math.abs(rotation.getPitch() - playerPitch),
                        maxYawStep,
                        maxPitchStep
                ),
                RotationManager.ReturnMode.Empty
        );
    }

    public void setActive(boolean active) {
        rotationActive = active;
    }
    private boolean isWithinFov(float targetYaw, float playerYaw) {
        return Math.abs(MathHelper.wrapDegrees(targetYaw - playerYaw)) <= 90.0F;
    }
    public void reset() {
        rotationActive = false;
        yawOffset = 0.0F;
        pitchOffset = 0.0F;
        previousYaw = 0.0F;
        previousPitch = 0.0F;
        yawNoise = 0.0F;
        pitchNoise = 0.0F;
        yawNoiseTarget = 0.0F;
        pitchNoiseTarget = 0.0F;
        lastTargetSwitchTime = 0L;
        targetSwitchDelay = 0L;
        lastNoiseUpdate = 0L;
    }

}
