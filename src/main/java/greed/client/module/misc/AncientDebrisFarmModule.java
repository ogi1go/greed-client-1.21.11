package greed.client.module.misc;

import greed.client.event.EventListener;
import greed.client.event.PlayerTickEvent;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.module.render.AncientXrayModule;
import greed.client.setting.ModeSetting;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potions;

@ModuleInfo(name = "AncientDebrisFarm",description = "Автоматически фармит древние обломки в аду",category = ModuleCategory.MISC)
public class AncientDebrisFarmModule extends PveModule {
    public static AncientDebrisFarmModule instance;
    private ModeSetting territorySearchMode;
    private static AncientXrayModule ancientXrayModule = AncientXrayModule.getInstance();

    private final EventListener<PlayerTickEvent> tickEventEventListener = event ->{
        if(mc.player==null)return;
        if(!getFireResistancePotion()){

        }
    };
    protected boolean getFireResistancePotion() {
        if (mc.player == null) return false;

        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = mc.player.getInventory().getStack(slot);

            if (stack.isOf(Items.POTION)) {
                PotionContentsComponent contents =
                        stack.get(DataComponentTypes.POTION_CONTENTS);

                if (contents != null &&
                        contents.potion().isPresent() &&
                        (contents.potion().get().matchesKey(Potions.FIRE_RESISTANCE.getKey().orElseThrow()) ||
                                contents.potion().get().matchesKey(Potions.LONG_FIRE_RESISTANCE.getKey().orElseThrow()))) {

                    mc.player.getInventory().setSelectedSlot(slot);
                    return true;
                }
            }
        }

        return false;
    }

    public static AncientDebrisFarmModule getInstance() {
        if(instance == null) {
            instance = new AncientDebrisFarmModule();
        }
        return instance;
    }
    public AncientDebrisFarmModule() {
        this.territorySearchMode = this.addSetting(new ModeSetting("Режим поиска","В какой части мира искать","Сверху","Снизу"));
    }
}
