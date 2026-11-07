package greed.client;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;

/**
 * «Тихая» наводка: сервер получает нужные yaw/pitch в пакетах движения, а клиентская камера
 * остаётся полностью под управлением игрока.
 *
 * <p>Модуль каждый тик просит нужный угол через {@link #request(float, float)}. Перед сборкой
 * пакета движения {@link #begin()} подменяет ротацию игрока, а {@link #end()} возвращает
 * исходные значения сразу после отправки. Между пакетами клиентский игрок смотрит туда, куда
 * его направила камера, поэтому рендер, движение и ванильные проверки не затрагиваются.
 */
public final class SilentRotation implements MinecraftAccess {
    private static final int HOLD_TICKS = 1;

    private static float yaw;
    private static float pitch;
    private static int lastRequestAge = Integer.MIN_VALUE;

    private static boolean spoofing;
    private static float savedYaw;
    private static float savedPitch;

    private SilentRotation() {}

    /** Запрашивает угол для следующего пакета движения. */
    public static void request(float newYaw, float newPitch) {
        if (mc.player == null) return;
        yaw = MathHelper.wrapDegrees(newYaw);
        pitch = MathHelper.clamp(newPitch, -90.0F, 90.0F);
        lastRequestAge = mc.player.age;
    }

    public static boolean isActive() {
        return mc.player != null
                && lastRequestAge != Integer.MIN_VALUE
                && mc.player.age - lastRequestAge <= HOLD_TICKS;
    }

    public static float getYaw() {
        return yaw;
    }

    public static float getPitch() {
        return pitch;
    }

    /** Подменяет ротацию игрока непосредственно перед сборкой пакета движения. */
    public static void begin() {
        spoofing = false;
        ClientPlayerEntity player = mc.player;
        if (player == null || !isActive()) return;

        savedYaw = player.getYaw();
        savedPitch = player.getPitch();
        player.setYaw(yaw);
        player.setPitch(pitch);
        spoofing = true;
    }

    /** Возвращает клиентскую ротацию сразу после отправки пакета. */
    public static void end() {
        ClientPlayerEntity player = mc.player;
        if (!spoofing || player == null) {
            spoofing = false;
            return;
        }
        player.setYaw(savedYaw);
        player.setPitch(savedPitch);
        spoofing = false;
    }

    /** Полностью выключает наводку; следующий пакет движения вернёт камере управление сервером. */
    public static void clear() {
        end();
        lastRequestAge = Integer.MIN_VALUE;
    }
}
