package greed.client.module.combat.crystalmodules;

import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.module.combat.crystalmodules.utils.explosive.ExplosiveModule;
import greed.client.setting.NumberSetting;

/**
 * Respawn anchor aura settings holder. The shared {@link
 * greed.client.module.combat.crystalmodules.utils.explosive.ExplosiveCoordinator} performs the
 * placement, charging and detonation sequence.
 */
@ModuleInfo(
        name = "AnchorAura",
        description = "Автоматическая установка, зарядка и подрыв якорей возрождения.",
        category = ModuleCategory.COMBAT)
public final class AnchorAura extends ExplosiveModule {
    private static AnchorAura instance;

    public final NumberSetting charges =
            this.addSetting(
                    new NumberSetting("Заряды", "Количество светокамня для взрыва")
                            .withIntegerRange(1, 5)
                            .setValue(4f));

    private AnchorAura() {
        super("AnchorAura", "Автоматическая установка, зарядка и подрыв якорей возрождения.");
    }

    public static AnchorAura getInstance() {
        if (instance == null) {
            instance = new AnchorAura();
        }
        return instance;
    }
}
