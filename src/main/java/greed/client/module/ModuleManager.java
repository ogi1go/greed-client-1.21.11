package greed.client.module;

import greed.client.module.combat.*;
import greed.client.module.combat.crystalmodules.AutoExplosionModule;
import greed.client.module.combat.crystalmodules.AnchorAura;
import greed.client.module.combat.crystalmodules.CrystalAura;
import greed.client.module.misc.*;
import greed.client.module.movement.*;
import greed.client.module.render.*;
import greed.client.module.player.AutoExpModule;
import greed.client.module.player.ClickActionModule;
import greed.client.module.player.ElytraHelperModule;
import greed.client.module.player.ItemScrollerModule;
import greed.client.module.player.NoPlayerDelayModule;
import greed.client.module.player.NoPushModule;
import greed.client.module.player.PotionThrowerModule;
import ru.dreamix.protection.api.impl.profile.ProfileAPI;
import ru.dreamix.protection.api.impl.profile.objects.RoleObject;
import greed.client.module.player.ServerHelperModule;
import greed.client.module.player.UseTrackerModule;

import java.util.ArrayList;
import java.util.List;

public final class ModuleManager {
    private static final List<Module> modules = new ArrayList<>();
    private static boolean initialized;

    public static Module findByName(String local866_0) {
        for (Module module : modules) {
            if (module.name.equalsIgnoreCase(local866_0)) {
                return module;
            }
        }

        return null;
    }

    public static void registerModule(Module module) {
        if (module != null && !modules.contains(module)) {
            if (!(module instanceof AutoMineModule)
                    && !(module instanceof AutoBuyModule)
                    && !(module instanceof AutoVillagerModule)
                    && !(module instanceof AutoAppleFarmModule)
                    && !(module instanceof AutoWardenModule)
                    || ProfileAPI.get().getModel().getRole().priorityHigherOrEquals(RoleObject.BETA)) {
                if (!(module instanceof YTMODEModule) || ProfileAPI.get().getModel().getRole().priorityHigherOrEquals(RoleObject.YOUTUBE)) {
                    if (!(module instanceof AdminShardModule) || ProfileAPI.get().getModel().getRole().priorityHigherOrEquals(RoleObject.ADMIN)) {
                        if (!(module instanceof AdminRecordModule) || ProfileAPI.get().getModel().getRole().priorityHigherOrEquals(RoleObject.ADMIN)) {
                            modules.add(module);
                        }
                    }
                }
            }
        }
    }

    public static List<Module> getModules() {
        return modules;
    }

    private ModuleManager() {
    }

    public static void unregisterModule(Module module) {
        modules.remove(module);
    }

    public static List<Module> getModulesByCategory(ModuleCategory moduleCategory) {
        List<Module> local871_1 = new ArrayList<>();

        for (Module module : modules) {
            if (module.category == moduleCategory) {
                local871_1.add(module);
            }
        }

        return local871_1;
    }

    public static void initializeModules() {
        if (!initialized) {
            initialized = true;
            registerModule(AttackAuraModule.getInstance());
            registerModule(MaceHelperModule.getInstance());
            registerModule(AutoSwapModule.getInstance());
            registerModule(AutoTotemModule.getInstance());
            registerModule(AnchorAura.getInstance());
            registerModule(TriggerBotModule.getInstance());
            registerModule(ItemHelperModule.getInstance());
            registerModule(AimAssistModule.getInstance());
            registerModule(NoEntityTraceModule.getInstance());
            registerModule(NoServerPackModule.getInstance());
            registerModule(HitBoxesModule.getInstance());
            registerModule(BackTrackModule.getInstance());
            registerModule(AutoRegisterModule.getInstance());
            registerModule(AuctionHelperModule.getInstance());
            registerModule(AutoLeaveModule.getInstance());
            registerModule(AutoToolModule.getInstance());
            registerModule(AutoEatModule.getInstance());
            registerModule(AutoAcceptModule.getInstance());
            registerModule(AutoDeliverModule.getInstance());
            registerModule(ClanUpgraderModule.getInstance());
            registerModule(CrystalAura.getInstance());
            registerModule(AutoExplosionModule.getInstance());

            registerModule(AutoMineModule.getInstance());
            registerModule(AutoBuyModule.getInstance());
            registerModule(AutoVillagerModule.getInstance());
            registerModule(AutoAppleFarmModule.getInstance());
            registerModule(AutoWardenModule.getInstance());
            registerModule(SpeedModule.getInstance());
            registerModule(NoWebModule.getInstance());

            registerModule(AutoSellModule.getInstance());
            registerModule(PotionCombinerModule.getInstance());
            registerModule(ChestStealerModule.getInstance());
            registerModule(InfoReplaceModule.getInstance());
            registerModule(StreamerModModule.getInstance());
            registerModule(YTMODEModule.getInstance());
            registerModule(NameProtectModule.getInstance());
//            registerModule(GlobalsModule.getInstance());
//            registerModule(PartyModule.getInstance());
//         registerModule(DiscordRPCModule.getInstance());
            registerModule(DuelHelperModule.getInstance());
            registerModule(AutoMessageModule.getInstance());
            registerModule(AirStuckModule.getInstance());
            registerModule(AutoSprintModule.getInstance());
            registerModule(FlightModule.getInstance());
            registerModule(ScreenWalkModule.getInstance());
            registerModule(SusChunks.getInstance());
            registerModule(VelocityModule.getInstance());
            registerModule(NoSlowModule.getInstance());
            registerModule(SpiderModule.getInstance());
            registerModule(ItemScrollerModule.getInstance());
            registerModule(NoPushModule.getInstance());
            registerModule(BlockSlotModule.getInstance());
            registerModule(NoRenderModule.getInstance());
            ProjectilePredictionModule.getInstance();
            registerModule(TagsModule.getInstance());
            registerModule(TargetEspModule.getInstance());
            registerModule(ArmorBarModule.getInstance());
            registerModule(ShulkerPreviewModule.getInstance());
            registerModule(SkeletonEspModule.getInstance());
            registerModule(ChamsModule.getInstance());
            registerModule(BlockEspModule.getInstance());
            registerModule(SeeInvisibleModule.getInstance());
            registerModule(TorusModule.getInstance());
            TotemGhostModule.getInstance();
            registerModule(CrosshairModule.getInstance());
            registerModule(ArrowsModule.getInstance());
            registerModule(BassBoostModule.getInstance());
            registerModule(PhysicSounds.INSTANCE);
            registerModule(ClickActionModule.getInstance());
            registerModule(AutoExpModule.getInstance());
            registerModule(ElytraHelperModule.getInstance());
            registerModule(NoPlayerDelayModule.getInstance());
            registerModule(FreeCameraModule.getInstance());
            registerModule(CameraFeatureModule.getInstance());
            registerModule(SwordAnimationsModule.getInstance());
            registerModule(ViewModelModule.getInstance());
            registerModule(InterfaceModule.getInstance());
            SmoothFeatureModule.getInstance();
            registerModule(AspectRatioModule.getInstance());
            registerModule(StructureInfoModule.getInstance());
            registerModule(WardenEspModule.getInstance());
            registerModule(JumpCircleModule.getInstance());
            registerModule(BlockOverlayModule.getInstance());
            registerModule(ParticlesModule.getInstance());
            registerModule(GlassVapor.getInstance());
            registerModule(MotionBlurModule.getInstance());
            registerModule(CustomWorldModule.getInstance());
            registerModule(BrightnessModule.getInstance());
            registerModule(ServerHelperModule.getInstance());
            registerModule(PotionThrowerModule.getInstance());
            registerModule(UseTrackerModule.getInstance());
            registerModule(ElytraMotionModule.getInstance());
            registerModule(AncientXrayModule.getInstance());
            registerModule(TrailsModule.getInstance());
            registerModule(UnHookModule.getInstance());
            registerModule(PhysicSounds.getINSTANCE());
            registerModule(SpecNotifierModule.getInstance());
            registerModule(AutoCartModule.getInstance());
        }
    }
}
