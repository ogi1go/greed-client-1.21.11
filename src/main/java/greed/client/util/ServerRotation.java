package greed.client.util;

/**
 * Хранит последнюю ротацию (yaw/pitch), которая реально ушла на сервер в пакете движения.
 * <p>
 * Нужна, чтобы перед ударом/установкой блока точно знать, какую ротацию сейчас видит сервер,
 * и не совершать действие раньше, чем нужная ротация до него дойдёт (иначе Grim ловит
 * RotationPlace / Hitboxes / AirLiquidPlace).
 */
public final class ServerRotation {
    private static volatile float yaw;
    private static volatile float pitch;
    private static volatile boolean valid;

    private ServerRotation() {
    }

    public static void record(float yaw, float pitch) {
        ServerRotation.yaw = yaw;
        ServerRotation.pitch = pitch;
        ServerRotation.valid = true;
    }

    public static boolean isValid() {
        return valid;
    }

    public static float getYaw() {
        return yaw;
    }

    public static float getPitch() {
        return pitch;
    }
}
