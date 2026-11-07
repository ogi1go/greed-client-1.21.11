package greed.client.module.misc;

import greed.client.event.ChatMessageEvent;
import greed.client.event.EventListener;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.module.combat.AttackAuraModule;
import greed.client.ui.hud.NotificationHud;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

@ModuleInfo(name = "SpecNotifier",description = "Оповещает когда таргет пишет спек!",category = ModuleCategory.MISC)
public class SpecNotifierModule extends Module {
    private static SpecNotifierModule instance;

    public SpecNotifierModule() {}

    public static SpecNotifierModule getInstance() {
        if(instance == null) {
            instance = new SpecNotifierModule();
        }
        return instance;
    }


    private final EventListener<ChatMessageEvent> onChatMessageEvent = event -> {
        if (mc.player == null || mc.world == null) return;


        PlayerEntity target = AttackAuraModule.getInstance().getTarget().getAttackingPlayer();

        if (target == null) return;


        String targetName = target.getName().getString();
        String messageText = event.getMessage();


        if (messageText.contains(targetName) && (messageText.toLowerCase().contains("spec") || messageText.toLowerCase().contains("спек") ||  messageText.toLowerCase().contains("спекк")
                || messageText.toLowerCase().contains("specc"))) {


            NotificationHud.notifyWithText(
                    this.category.getIcon(), "Осторожно!", Text.literal("Таргет " + targetName + " написал в чат: " + messageText), true
            );
        }
    };
}



