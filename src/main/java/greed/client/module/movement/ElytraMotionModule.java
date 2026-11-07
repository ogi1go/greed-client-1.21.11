package greed.client.module.movement;

import greed.client.event.EventListener;
import greed.client.event.PlayerTickEvent;
import greed.client.module.Module;
import greed.client.module.ModuleInfo;
import greed.client.module.combat.AttackAuraModule;
import greed.client.setting.BooleanSetting;
import greed.client.setting.NumberSetting;
import net.minecraft.entity.LivingEntity;

@ModuleInfo(name = "ElytraMotion",description= "Стопает на элитре",category = greed.client.module.ModuleCategory.MOVEMENT)
public final class ElytraMotionModule extends Module {
    public static ElytraMotionModule instance;
    private boolean waitTarget;
    private final NumberSetting distance = this.addSetting(new NumberSetting ("Distance","Дистанция поиска таргета").withRange(0.1f,6f).setValue(3f).withStep(0.1f));
    private final BooleanSetting bypass = this.addSetting(new BooleanSetting("Bypass","жоски бупа$$", false));
    public ElytraMotionModule(){
        addSettings(distance,bypass);
    }

    private final EventListener<PlayerTickEvent> playerTickEventEventListener = playerTickEvent -> {
        if (mc.player == null) {
            return;
        }
        LivingEntity target = AttackAuraModule.getInstance().getTarget();
        if (target == null) {
            if (!this.waitTarget) {
                mc.player.setNoGravity(false);
                this.waitTarget = true;
            }
            return;
        }
        this.waitTarget = false;
        float dist = (float)mc.player.getEyePos().distanceTo(target.getBoundingBox().getCenter());
        if (mc.player.isGliding() && dist < this.distance.getValue()) {
            if (this.bypass.getValue()) {
                float yaw = mc.player.getYaw();
                double rad = Math.toRadians(yaw);
                double forward = 0.01;
                double down = -1.0E-4;
                double moveX = -Math.sin(rad) * forward;
                double moveZ = Math.cos(rad) * forward;
                mc.player.setVelocity(moveX, down, moveZ);
            } else {
                mc.player.setVelocity(0.0, 0.0, 0.0);
            }
            mc.player.setNoGravity(true);
        } else {
            mc.player.setNoGravity(false);
        }
    };
    public static ElytraMotionModule getInstance() {
        if (instance == null) {
            instance = new ElytraMotionModule();
        }
        return instance;
    }
    @Override
    public void onDisable() {
        super.onDisable();
        if (mc.player == null) {
            return;
        }
        this.waitTarget = false;
        mc.player.setNoGravity(false);
    }
}
