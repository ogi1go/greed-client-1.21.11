package greed.client.util.greedtone;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;

import java.util.Random;

public final class Rotation {

    private static final Random RNG = new Random();

    private static float baseYaw;
    private static float basePitch;
    private static float offYaw;
    private static float offPitch;
    private static float goalYaw;
    private static float goalPitch;
    private static int timer;

    private Rotation() {
    }

    public static void reset(ClientPlayerEntity p) {
        baseYaw = p.getYaw();
        basePitch = p.getPitch();
        offYaw = 0.0f;
        offPitch = 0.0f;
        goalYaw = 0.0f;
        goalPitch = 0.0f;
        timer = 0;
    }

    public static float baseYaw() {
        return baseYaw;
    }

    public static float yawTo(double dx, double dz) {
        return (float) (MathHelper.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
    }

    public static float pitchTo(double dx, double dy, double dz) {
        return (float) -(MathHelper.atan2(dy, Math.max(Math.hypot(dx, dz), 0.001)) * 180.0 / Math.PI);
    }

    public static float yawError(ClientPlayerEntity p, float yaw) {
        return Math.abs(MathHelper.wrapDegrees(yaw - p.getYaw()));
    }

    public static boolean hold(ClientPlayerEntity p, float pitch, float speed) {
        return look(p, baseYaw, pitch, speed, false);
    }

    public static boolean look(ClientPlayerEntity p, float yaw, float pitch, float speed, boolean precise) {
        baseYaw = yaw;
        basePitch = pitch;

        float amp = precise ? 0.25f : 1.0f;
        if (--timer <= 0) {
            timer = 8 + RNG.nextInt(14);
            goalYaw = (RNG.nextFloat() * 2.0f - 1.0f) * 1.6f * amp;
            goalPitch = (RNG.nextFloat() * 2.0f - 1.0f) * 1.0f * amp;
        }
        offYaw += (goalYaw - offYaw) * 0.08f;
        offPitch += (goalPitch - offPitch) * 0.08f;

        float targetYaw = yaw + offYaw;
        float targetPitch = MathHelper.clamp(pitch + offPitch, -90.0f, 90.0f);
        float dy = MathHelper.wrapDegrees(targetYaw - p.getYaw());
        float dp = targetPitch - p.getPitch();
        float dist = (float) Math.hypot(dy, dp);
        if (dist < 0.01f) {
            return true;
        }

        float step = Math.min(dist, Math.min(speed, Math.max(dist * 0.35f, 1.2f)));
        float k = step / dist;
        p.setYaw(p.getYaw() + dy * k);
        p.setPitch(MathHelper.clamp(p.getPitch() + dp * k, -90.0f, 90.0f));
        return dist - step < (precise ? 2.5f : 15.0f);
    }
}
