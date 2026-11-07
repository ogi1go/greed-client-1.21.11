package greed.client.module.combat.crystalmodules;

import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.module.combat.crystalmodules.utils.explosive.ExplosiveModule;
import greed.client.setting.BooleanSetting;
import greed.client.setting.NumberSetting;

/**
 * Crystal aura settings holder. All planning, rotation and packet dispatch lives in {@link
 * greed.client.module.combat.crystalmodules.utils.explosive.ExplosiveCoordinator}; this module
 * only exposes the per-feature configuration and enable state to it.
 */
@ModuleInfo(
        name = "CrystalAura",
        description = "Автоматическая установка и подрыв кристаллов Энда.",
        category = ModuleCategory.COMBAT)
public final class CrystalAura extends ExplosiveModule {
    private static CrystalAura instance;

    public final NumberSetting attackRange =
            this.addSetting(
                    new NumberSetting("Дальность атаки", "Радиус удара по кристаллу")
                            .withRange(1f, 6f)
                            .setValue(4.5f));

    public final NumberSetting searchRadius =
            this.addSetting(
                    new NumberSetting("Радиус поиска", "Радиус поиска основания для кристалла")
                            .withIntegerRange(1, 5)
                            .setValue(4f));

    public final BooleanSetting placeObsidian =
            this.addSetting(
                    new BooleanSetting(
                                    "Ставить обсидиан",
                                    "Разрешить установку обсидиана под кристалл")
                            .setValue(true));

    private CrystalAura() {
        super("CrystalAura", "Автоматическая установка и подрыв кристаллов Энда.");
    }

    public static CrystalAura getInstance() {
        if (instance == null) {
            instance = new CrystalAura();
        }
        return instance;
    }
}
