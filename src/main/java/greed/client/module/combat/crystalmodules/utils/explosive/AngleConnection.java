package greed.client.module.combat.crystalmodules.utils.explosive;

import greed.client.RotationAngles;
import greed.client.RotationManager;
import greed.client.RotationSpeed;
import greed.client.SilentRotation;
import greed.client.module.combat.crystalmodules.utils.math.TaskPriority;
import greed.client.util.ServerRotation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

/**
 * Adapter between the explosive coordinator's rotation plans and the client {@link
 * RotationManager}.
 *
 * <p>The coordinator hands us a plan; we advance the plan's smoothing strategy by one step and
 * feed the resulting angle to the rotation manager, which performs the actual camera/movement
 * rotation. Ownership is tracked so switching between the crystal and anchor modules does not
 * drop the current aim state.
 */
public final class AngleConnection {
    public static final AngleConnection INSTANCE = new AngleConnection();

    private static final RotationSpeed SPEED =
            new RotationSpeed(180f, 180f, 180f, 180f);

    private AngleConstructor lastRotationPlan;
    private Object rotationOwner;
    private Angle lastRotation = new Angle(0f, 0f);
    private boolean silentLast;

    private AngleConnection() {}

    /** Current aim rotation: the silent aim while it is active, otherwise the local camera. */
    public Angle getRotation() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return lastRotation;
        if (SilentRotation.isActive())
            return new Angle(SilentRotation.getYaw(), SilentRotation.getPitch());
        return new Angle(mc.player.getYaw(), mc.player.getPitch());
    }

    /** Rotation the server has actually received, falling back to the client angle. */
    public Angle getServerAngle() {
        if (ServerRotation.isValid())
            return new Angle(ServerRotation.getYaw(), ServerRotation.getPitch());
        return getRotation();
    }

    public void setRotation(Angle angle) {
        if (angle != null) lastRotation = angle;
    }

    /** Drops the plan if the given owner still owns it. */
    public void release(Object owner, AngleConstructor plan) {
        if ((rotationOwner == owner || owner == null)
                && (plan == null || lastRotationPlan == plan)) {
            lastRotationPlan = null;
            rotationOwner = null;
            silentLast = false;
            SilentRotation.clear();
        }
    }

    public void rotateTo(
            AngleConstructor plan, TaskPriority priority, Object owner) {
        if (plan == null) return;
        Angle current = getRotation();
        Angle desired = plan.target();
        Angle next = plan.constructor().limitAngleChange(
                current, desired, plan.point(), plan.entity());
        if (next == null
                || !Float.isFinite(next.yaw())
                || !Float.isFinite(next.pitch())) next = desired;
        next = new Angle(MathHelper.wrapDegrees(next.yaw()), MathHelper.clamp(next.pitch(), -90f, 90f));
        lastRotation = next;
        lastRotationPlan = plan;
        rotationOwner = owner;
        if (owner instanceof ExplosiveModule module && module.silentAim.isChecked()) {
            if (!silentLast) {
                // Отключаем прежнюю видимую наводку, чтобы камера сразу стала свободной.
                RotationManager.getRotationManager().resetRotation();
                silentLast = true;
            }
            SilentRotation.request(next.yaw(), next.pitch());
        } else {
            if (silentLast) {
                SilentRotation.clear();
                silentLast = false;
            }
            RotationManager.getRotationManager()
                    .setRotation(
                            new RotationAngles(next.yaw(), next.pitch()),
                            SPEED,
                            RotationManager.ReturnMode.Keep);
        }
    }

    public AngleConstructor getLastRotationPlan() {
        return lastRotationPlan;
    }

    public Object getRotationOwner() {
        return rotationOwner;
    }
}
