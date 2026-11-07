package greed.client.module.movement;

import greed.client.event.EventListener;
import greed.client.event.PlayerTickEvent;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.setting.ModeSetting;
import greed.client.setting.NumberSetting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(
        name = "Speed",
        description = "Ускоряет вас",
        category = ModuleCategory.MOVEMENT
)
public final class SpeedModule extends Module {
    public static SpeedModule instance;
    public ModeSetting mode = this.addSetting(new ModeSetting("Режим","","Грим","Грим","Стрейф","Гроунд"));
    public NumberSetting speed = this.addSetting(new NumberSetting("Speed", "").withIntegerRange(0, 2).withStep(0.1f).setValue(1));
    private int ticks;

    private final EventListener<PlayerTickEvent> playerTickListener = playerTickEvent -> {
        if (mc.player == null || mc.world == null) return;
        if (mc.player.isGliding()) return;

        switch (mode.getValue()) {
            case "Грим" -> handleGrim();
            case "Стрейф" -> handleStrafe();
            case "Гроунд" -> handleGround();
        }
    };

    private double getForwardInput() {
        double f = 0;
        if (mc.player.input.playerInput.forward()) f++;
        if (mc.player.input.playerInput.backward()) f--;
        return f;
    }

    private double getStrafeInput() {
        double s = 0;
        if (mc.player.input.playerInput.left()) s++;
        if (mc.player.input.playerInput.right()) s--;
        return s;
    }

    private void handleGrim() {
        double forward = getForwardInput();
        double strafe = getStrafeInput();
        if (forward <= 0 && strafe == 0) return;

        ticks++;

        if (mc.player.isOnGround() && ticks > 5) {
            mc.player.jump();
            ticks = 0;
        }

        Vec3d vel = mc.player.getVelocity();
        double baseSpeed = speed.getValue() * 0.28;
        float f = mc.player.getYaw() * 0.017453292F;
        Vec3d direction = new Vec3d(
                -MathHelper.sin(f) * 0.02,
                0,
                MathHelper.cos(f) * 0.02
        );

        mc.player.setVelocity(
                vel.x + direction.x * baseSpeed,
                mc.player.isOnGround() ? 0.42 : vel.y,
                vel.z + direction.z * baseSpeed
        );
    }

    private void handleStrafe() {
        double forward = getForwardInput();
        double strafe = getStrafeInput();
        if (forward <= 0 && strafe == 0) return;

        Vec3d vel = mc.player.getVelocity();
        double baseSpeed = speed.getValue() * 0.36;

        float yaw = mc.player.getYaw();

        if (forward != 0) {
            if (strafe > 0) {
                yaw += forward > 0 ? -45 : 45;
            } else if (strafe < 0) {
                yaw += forward > 0 ? 45 : -45;
            }
            strafe = 0;
            forward = forward > 0 ? 1 : -1;
        }

        double rad = Math.toRadians(yaw + 90);
        double motionX = forward * baseSpeed * Math.cos(rad) + strafe * baseSpeed * Math.sin(rad);
        double motionZ = forward * baseSpeed * Math.sin(rad) - strafe * baseSpeed * Math.cos(rad);

        if (mc.player.isOnGround()) {
            mc.player.jump();
            mc.player.setVelocity(motionX, 0.42, motionZ);
        } else {
            mc.player.setVelocity(motionX, mc.player.getVelocity().y, motionZ);
        }
    }

    private void handleGround() {
        double forward = getForwardInput();
        double strafe = getStrafeInput();
        if (forward <= 0 && strafe == 0) return;
        if (!mc.player.isOnGround()) return;

        Vec3d vel = mc.player.getVelocity();
        double baseSpeed = speed.getValue() * 0.32;

        float yaw = mc.player.getYaw();

        if (forward != 0) {
            if (strafe > 0) {
                yaw += forward > 0 ? -45 : 45;
            } else if (strafe < 0) {
                yaw += forward > 0 ? 45 : -45;
            }
            strafe = 0;
            forward = forward > 0 ? 1 : -1;
        }

        double rad = Math.toRadians(yaw + 90);
        double motionX = forward * baseSpeed * Math.cos(rad) + strafe * baseSpeed * Math.sin(rad);
        double motionZ = forward * baseSpeed * Math.sin(rad) - strafe * baseSpeed * Math.cos(rad);

        mc.player.setVelocity(motionX, vel.y, motionZ);
    }

    public static SpeedModule getInstance() {
        return new SpeedModule();
    }
}