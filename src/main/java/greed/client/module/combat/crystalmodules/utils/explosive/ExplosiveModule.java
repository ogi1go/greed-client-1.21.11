package greed.client.module.combat.crystalmodules.utils.explosive;

import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.setting.BooleanSetting;
import greed.client.setting.NumberSetting;
import net.minecraft.entity.player.PlayerEntity;



public abstract class ExplosiveModule extends Module {
    public final NumberSetting range =
            new NumberSetting("Дистанция взаимодействия", "Блоки от глаз до точки взаимодействия")
                    .withRange(1f, 5f)
                    .setValue(4.5f);
    public final NumberSetting targetRange =
            new NumberSetting("Дальность цели", "Радиус выбора игрока")
                    .withRange(2f, 12f)
                    .setValue(8f);
    public final NumberSetting minimum =
            new NumberSetting("Min Target Damage", "Минимальный урон цели в HP")
                    .withRange(1f, 20f)
                    .setValue(4f);
    public final NumberSetting maximum =
            new NumberSetting("Max Self Damage", "Максимальный урон себе в HP")
                    .withRange(0f, 20f)
                    .setValue(6f);
    public final NumberSetting reserve =
            new NumberSetting("Запас здоровья", "С учетом еще не подтвержденного урона")
                    .withRange(1f, 16f)
                    .setValue(4f);
    public final NumberSetting predictionTicks =
            new NumberSetting(
                            "Предикт, тики",
                            "Максимум прогноза движения с учетом пинга; 0 — выключить")
                    .withRange(0f, 8f)
                    .setValue(6f);
    public final NumberSetting rotationSpeed =
            new NumberSetting(
                            "Скорость наведения", "Максимум градусов за тик без разворота камеры")
                    .withRange(5f, 180f)
                    .setValue(120f);
    public final NumberSetting rotationSmoothness = this.addSetting(
            
            new NumberSetting(
                            "Плавность наведения",
                            "Меньше — быстрее, больше — мягче разгон и торможение")
                    .withRange(0.7f, 3f)
                    .setValue(1f));
    public final BooleanSetting silentAim =
            new BooleanSetting(
                            "Тихая наводка",
                            "Сервер получает наводку на цель, а клиентская камера остаётся свободной")
                    .setValue(true);
    public final BooleanSetting walls =
            new BooleanSetting(
                            "Сквозь блоки", "Скрытая установка и атака; сервер может отклонить взаимодействие")
                    .setValue(false);
    public final BooleanSetting clearBlocks =
            new BooleanSetting("Расчистка / AutoTool", "Расчистить место установки в 4 блоках от цели; копает только при подходящем инструменте в хотбаре")
                    .setValue(true);
    public final BooleanSetting friends =
            new BooleanSetting("Защита друзей", "Не взрывать при опасном уроне друзьям")
                    .setValue(true);

    protected ExplosiveModule(String name, String description) {
        super(name, description, ModuleCategory.COMBAT);
        this.addSettings(
                range,
                targetRange,
                minimum,
                maximum,
                reserve,
                predictionTicks,
                rotationSpeed,
                rotationSmoothness,
                silentAim,
                walls,
                clearBlocks,
                friends);
        ExplosiveCoordinator.ensureRegistered();
    }

    public PlayerEntity getTarget() {
        return  ExplosiveCoordinator.target() ;
    }

    /** Alias kept for the ported coordinator, which uses the older state naming. */
    public boolean isState() {
        return isEnabled();
    }

    @Override
    public void onEnable() {
        ExplosiveCoordinator.cancelPreview();
    }

    @Override
    public void onDisable() {
        ExplosiveCoordinator.cancelPreview();
    }
}
