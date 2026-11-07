package greed.client.module.combat.crystalmodules.utils.explosive;

/** Critically damped angular tracking with bounded speed and a soft initial acceleration. */
public final class ExplosiveAim {
    public record Rotation(float yaw, float pitch) {}

    private double yawVelocity, pitchVelocity, lastYawStep, lastPitchStep;

    public void reset() {
        yawVelocity = pitchVelocity = lastYawStep = lastPitchStep = 0;
    }

    public Rotation step(
            float yaw,
            float pitch,
            float targetYaw,
            float targetPitch,
            float maxSpeed,
            float smoothness) {
        if (!Float.isFinite(yaw)
                || !Float.isFinite(pitch)
                || !Float.isFinite(targetYaw)
                || !Float.isFinite(targetPitch)) {
            reset();
            return new Rotation(
                    Float.isFinite(yaw) ? yaw : 0,
                    Float.isFinite(pitch) ? Math.clamp(pitch, -90, 90) : 0);
        }
        double speed = Float.isFinite(maxSpeed) ? Math.clamp(maxSpeed, 5, 180) : 80;
        double softness = Float.isFinite(smoothness) ? Math.clamp(smoothness, .14, 3) : 1.3;
        double dy = wrap(targetYaw - yaw), dp = Math.clamp(targetPitch, -90, 90) - pitch;
        double distance = Math.hypot(dy, dp);
        if (distance < Math.max(.15, speed * .01)) {
            reset();
            return new Rotation(yaw + (float) dy, Math.clamp(targetPitch, -90, 90));
        }
        double omega = 2.2 / softness, decay = Math.exp(-omega);
        double ty = yawVelocity - omega * dy, tp = pitchVelocity - omega * dp;
        double sy = dy + (-dy + ty) * decay, sp = dp + (-dp + tp) * decay;
        double vy = (yawVelocity - omega * ty) * decay, vp = (pitchVelocity - omega * tp) * decay;
        double cap = Math.min(1, speed / Math.max(.00001, Math.hypot(sy, sp)));
        sy *= cap;
        sp *= cap;
        vy *= cap;
        vp *= cap;
        double ay = sy - lastYawStep, ap = sp - lastPitchStep;
        double ramp = Math.min(1, speed * .6 / Math.max(.00001, Math.hypot(ay, ap)));
        sy = lastYawStep + ay * ramp;
        sp = lastPitchStep + ap * ramp;
        // Retargeting must not coast away from the new aim or overshoot a tiny face.
        if (sy * dy <= 0) {
            sy = 0;
            vy = 0;
        } else if (Math.abs(sy) >= Math.abs(dy)) {
            sy = dy;
            vy = 0;
        }
        if (sp * dp <= 0) {
            sp = 0;
            vp = 0;
        } else if (Math.abs(sp) >= Math.abs(dp)) {
            sp = dp;
            vp = 0;
        }
        cap = Math.min(1, speed / Math.max(.00001, Math.hypot(sy, sp)));
        sy *= cap;
        sp *= cap;
        yawVelocity = vy;
        pitchVelocity = vp;
        lastYawStep = sy;
        lastPitchStep = sp;
        return new Rotation(yaw + (float) sy, (float) Math.clamp(pitch + sp, -90, 90));
    }

    private static double wrap(double angle) {
        angle %= 360;
        if (angle >= 180) angle -= 360;
        if (angle < -180) angle += 360;
        return angle;
    }
}
