package greed.client.module.render;

import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.setting.BooleanSetting;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.EXTEfx;

@ModuleInfo(name = "PhysicSounds", description = "Классные 3д звуки брат", category = ModuleCategory.RENDER)
public class PhysicSounds extends Module {
    public static PhysicSounds INSTANCE = new PhysicSounds();
    public BooleanSetting underWater;
    public BooleanSetting caveEcho;
    public BooleanSetting outdoorEcho;
    public BooleanSetting occlusion;
    private boolean initialized = false;
    private boolean efxSupported = false;
    private long lastContext = 0L;

    private int slotTunnel = 0;
    private int slotCave = 0;
    private int slotOpen = 0;
    private int filterOcclusion = 0;
    private int filterUnderwater = 0;

    public PhysicSounds() {
        underWater = this.addSetting(new BooleanSetting("Вода", "Крутой звук под водой").setValue(true));
        caveEcho = this.addSetting(new BooleanSetting("Эхо", "Крутое эхо").setValue(true));
        outdoorEcho = this.addSetting(new BooleanSetting("Эхо на улице", "Эхо в горах и между постройками").setValue(true));
        occlusion = this.addSetting(new BooleanSetting("За Стеной", "Крутой звук за стеной").setValue(true));
        this.initialized = false;
        this.efxSupported = false;
        this.lastContext = 0L;
        this.slotTunnel = 0;
        this.slotCave = 0;
        this.slotOpen = 0;
        this.filterOcclusion = 0;
        this.filterUnderwater = 0;
    }

    private void initEfx() {
        long context = ALC10.alcGetCurrentContext();
        if (context == 0L) {
            this.initialized = false;
            return;
        }

        if (context != this.lastContext) {
            this.initialized = false;
            this.lastContext = context;
        }

        if (this.initialized) return;
        this.initialized = true;

        try {
            long device = ALC10.alcGetContextsDevice(context);
            if (!ALC10.alcIsExtensionPresent(device, "ALC_EXT_EFX")) {
                this.efxSupported = false;
                return;
            }

            while (AL10.alGetError() != AL10.AL_NO_ERROR) {}

            this.filterOcclusion = EXTEfx.alGenFilters();
            EXTEfx.alFilteri(this.filterOcclusion, EXTEfx.AL_FILTER_TYPE, EXTEfx.AL_FILTER_LOWPASS);
            EXTEfx.alFilterf(this.filterOcclusion, EXTEfx.AL_LOWPASS_GAIN, 0.70F);
            EXTEfx.alFilterf(this.filterOcclusion, EXTEfx.AL_LOWPASS_GAINHF, 0.22F);

            this.filterUnderwater = EXTEfx.alGenFilters();
            EXTEfx.alFilteri(this.filterUnderwater, EXTEfx.AL_FILTER_TYPE, EXTEfx.AL_FILTER_LOWPASS);
            EXTEfx.alFilterf(this.filterUnderwater, EXTEfx.AL_LOWPASS_GAIN, 0.75F);
            EXTEfx.alFilterf(this.filterUnderwater, EXTEfx.AL_LOWPASS_GAINHF, 0.05F);

            int effectTunnel = EXTEfx.alGenEffects();
            EXTEfx.alEffecti(effectTunnel, EXTEfx.AL_EFFECT_TYPE, EXTEfx.AL_EFFECT_REVERB);
            EXTEfx.alEffectf(effectTunnel, EXTEfx.AL_REVERB_DENSITY, 0.9F);
            EXTEfx.alEffectf(effectTunnel, EXTEfx.AL_REVERB_DIFFUSION, 0.85F);
            EXTEfx.alEffectf(effectTunnel, EXTEfx.AL_REVERB_GAIN, 0.38F);
            EXTEfx.alEffectf(effectTunnel, EXTEfx.AL_REVERB_GAINHF, 0.88F);
            EXTEfx.alEffectf(effectTunnel, EXTEfx.AL_REVERB_DECAY_TIME, 1.25F);
            EXTEfx.alEffectf(effectTunnel, EXTEfx.AL_REVERB_DECAY_HFRATIO, 0.75F);
            EXTEfx.alEffectf(effectTunnel, EXTEfx.AL_REVERB_REFLECTIONS_GAIN, 0.65F);
            EXTEfx.alEffectf(effectTunnel, EXTEfx.AL_REVERB_LATE_REVERB_GAIN, 0.80F);

            this.slotTunnel = EXTEfx.alGenAuxiliaryEffectSlots();
            EXTEfx.alAuxiliaryEffectSloti(this.slotTunnel, EXTEfx.AL_EFFECTSLOT_EFFECT, effectTunnel);

            int effectCave = EXTEfx.alGenEffects();
            EXTEfx.alEffecti(effectCave, EXTEfx.AL_EFFECT_TYPE, EXTEfx.AL_EFFECT_REVERB);
            EXTEfx.alEffectf(effectCave, EXTEfx.AL_REVERB_DENSITY, 1.0F);
            EXTEfx.alEffectf(effectCave, EXTEfx.AL_REVERB_DIFFUSION, 0.95F);
            EXTEfx.alEffectf(effectCave, EXTEfx.AL_REVERB_GAIN, 0.48F);
            EXTEfx.alEffectf(effectCave, EXTEfx.AL_REVERB_GAINHF, 0.82F);
            EXTEfx.alEffectf(effectCave, EXTEfx.AL_REVERB_DECAY_TIME, 2.6F);
            EXTEfx.alEffectf(effectCave, EXTEfx.AL_REVERB_DECAY_HFRATIO, 0.65F);
            EXTEfx.alEffectf(effectCave, EXTEfx.AL_REVERB_REFLECTIONS_GAIN, 0.55F);
            EXTEfx.alEffectf(effectCave, EXTEfx.AL_REVERB_LATE_REVERB_GAIN, 1.1F);

            this.slotCave = EXTEfx.alGenAuxiliaryEffectSlots();
            EXTEfx.alAuxiliaryEffectSloti(this.slotCave, EXTEfx.AL_EFFECTSLOT_EFFECT, effectCave);

            int effectOpen = EXTEfx.alGenEffects();
            EXTEfx.alEffecti(effectOpen, EXTEfx.AL_EFFECT_TYPE, EXTEfx.AL_EFFECT_REVERB);
            EXTEfx.alEffectf(effectOpen, EXTEfx.AL_REVERB_DENSITY, 0.4F);
            EXTEfx.alEffectf(effectOpen, EXTEfx.AL_REVERB_DIFFUSION, 0.7F);
            EXTEfx.alEffectf(effectOpen, EXTEfx.AL_REVERB_GAIN, 0.25F);
            EXTEfx.alEffectf(effectOpen, EXTEfx.AL_REVERB_GAINHF, 0.7F);
            EXTEfx.alEffectf(effectOpen, EXTEfx.AL_REVERB_DECAY_TIME, 3.2F);
            EXTEfx.alEffectf(effectOpen, EXTEfx.AL_REVERB_DECAY_HFRATIO, 0.5F);
            EXTEfx.alEffectf(effectOpen, EXTEfx.AL_REVERB_REFLECTIONS_GAIN, 0.35F);
            EXTEfx.alEffectf(effectOpen, EXTEfx.AL_REVERB_REFLECTIONS_DELAY, 0.12F);
            EXTEfx.alEffectf(effectOpen, EXTEfx.AL_REVERB_LATE_REVERB_GAIN, 0.6F);
            EXTEfx.alEffectf(effectOpen, EXTEfx.AL_REVERB_LATE_REVERB_DELAY, 0.06F);

            this.slotOpen = EXTEfx.alGenAuxiliaryEffectSlots();
            EXTEfx.alAuxiliaryEffectSloti(this.slotOpen, EXTEfx.AL_EFFECTSLOT_EFFECT, effectOpen);

            this.efxSupported = (AL10.alGetError() == AL10.AL_NO_ERROR);
        } catch (Throwable t) {
            this.efxSupported = false;
        }
    }

    public void processAcoustics(int sourceId, SoundInstance sound) {
        if (sourceId <= 0) return;

        initEfx();

        if (!this.efxSupported) return;

        if (mc.player == null || mc.world == null || isIgnored(sound)) {
            reset(sourceId);
            return;
        }

        Vec3d soundPos = new Vec3d(
                sound.getX(),
                sound.getY(),
                sound.getZ()
        );

        BlockPos soundBlock = BlockPos.ofFloored(soundPos);
        Vec3d playerEyes = mc.player.getEyePos();

        boolean inWater = mc.player.isSubmergedInWater()
                || mc.world.getFluidState(soundBlock).isStill();

        if (inWater && this.underWater.getValue()) {
            AL10.alSourcei(
                    sourceId,
                    EXTEfx.AL_DIRECT_FILTER,
                    this.filterUnderwater
            );
        } else if (this.occlusion.getValue()
                && isBehindSolidWall(playerEyes, soundPos, soundBlock)) {
            AL10.alSourcei(
                    sourceId,
                    EXTEfx.AL_DIRECT_FILTER,
                    this.filterOcclusion
            );
        } else {
            AL10.alSourcei(
                    sourceId,
                    EXTEfx.AL_DIRECT_FILTER,
                    EXTEfx.AL_FILTER_NULL
            );
        }

        int slot = EXTEfx.AL_EFFECTSLOT_NULL;

        if (!inWater && (this.caveEcho.getValue() || this.outdoorEcho.getValue())) {
            BlockPos acousticOrigin =
                    mc.world.getBlockState(soundBlock).isOpaqueFullCube()
                            ? mc.player.getBlockPos()
                            : soundBlock;

            int type = getEnvironmentType(acousticOrigin);

            if (type == 1 && this.caveEcho.getValue()) slot = this.slotTunnel;
            else if (type == 2 && this.caveEcho.getValue()) slot = this.slotCave;
            else if (type == 3 && this.outdoorEcho.getValue()) slot = this.slotOpen;
        }

        AL11.alSource3i(sourceId, EXTEfx.AL_AUXILIARY_SEND_FILTER, slot, 0, EXTEfx.AL_FILTER_NULL);
    }

    private boolean isBehindSolidWall(
            Vec3d eyes,
            Vec3d soundPos,
            BlockPos targetBlock
    ) {
        if (eyes.squaredDistanceTo(soundPos) < 2.25) {
            return false;
        }

        BlockHitResult hit = mc.world.raycast(
                new RaycastContext(
                        eyes,
                        soundPos,
                        RaycastContext.ShapeType.COLLIDER,
                        RaycastContext.FluidHandling.NONE,
                        mc.player
                )
        );

        if (hit.getType() == HitResult.Type.BLOCK) {
            return !hit.getBlockPos().equals(targetBlock);
        }

        return false;
    }

    private int getEnvironmentType(BlockPos center) {
        boolean sky = mc.world.isSkyVisible(center.up());

        BlockPos.Mutable mPos = center.mutableCopy();
        int ceilingHeight = 0;
        boolean solidCeiling = false;

        for (int y = 1; y <= 24; y++) {
            mPos.setY(center.getY() + y);
            var state = mc.world.getBlockState(mPos);

            if (state.isAir()) continue;
            if (state.isIn(BlockTags.LEAVES)) break;

            if (state.isOpaqueFullCube()) {
                ceilingHeight = y;
                solidCeiling = true;
                break;
            }
        }

        int maxRange = (sky || !solidCeiling) ? 32 : 16;
        int totalDist = 0;
        int walls = 0;

        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4.0;
            double dx = Math.cos(angle);
            double dz = Math.sin(angle);

            for (int step = 1; step <= maxRange; step++) {
                mPos.set(center.getX() + (int) Math.round(dx * step),
                        center.getY() + 1,
                        center.getZ() + (int) Math.round(dz * step));

                if (mc.world.getBlockState(mPos).isOpaqueFullCube()) {
                    totalDist += step;
                    walls++;
                    break;
                }
            }
        }

        if (sky || !solidCeiling) {
            if (walls >= 4) return 3;
            if (walls >= 2 && totalDist / walls <= 20) return 3;
            return 0;
        }

        if (walls < 3) return 0;

        int avgDist = totalDist / walls;

        if (ceilingHeight >= 5 && avgDist >= 4) return 2;
        return 1;
    }

    public void reset(int sourceId) {
        if (sourceId <= 0 || !this.efxSupported) return;
        AL10.alSourcei(sourceId, EXTEfx.AL_DIRECT_FILTER, EXTEfx.AL_FILTER_NULL);
        AL11.alSource3i(sourceId, EXTEfx.AL_AUXILIARY_SEND_FILTER, EXTEfx.AL_EFFECTSLOT_NULL, 0, EXTEfx.AL_FILTER_NULL);
    }

    public void invalidate() {
        this.slotTunnel = 0;
        this.slotCave = 0;
        this.slotOpen = 0;
        this.filterOcclusion = 0;
        this.filterUnderwater = 0;
        this.initialized = false;
        this.efxSupported = false;
        this.lastContext = 0L;
    }

    private boolean isIgnored(SoundInstance sound) {
        if (sound == null || sound.isRelative()) {
            return true;
        }

        SoundCategory src = sound.getCategory();

        return src == SoundCategory.UI
                || src == SoundCategory.MUSIC
                || src == SoundCategory.RECORDS;
    }

    public static PhysicSounds getINSTANCE() {
        if (INSTANCE == null) INSTANCE = new PhysicSounds();
        return INSTANCE;
    }
}