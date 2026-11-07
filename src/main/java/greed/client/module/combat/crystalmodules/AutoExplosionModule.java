package greed.client.module.combat.crystalmodules;

import greed.client.event.EventListener;
import greed.client.event.PacketEvent;
import greed.client.event.PlayerTickEvent;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.setting.BooleanSetting;
import greed.client.setting.MultiSelectSetting;
import greed.client.util.InventoryUtils;
import greed.client.util.player.placement.BlockPlacement;
import greed.client.util.player.placement.InteractionGate;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

/**
 * Автоматически подрывает поставленный кристалл эндера или якорь возрождения.
 *
 * <p>Все взаимодействия отправляются через {@link BlockPlacement}: он держит один
 * пакет-действие на тик, меняет слот «тихо» (UpdateSelectedSlot -> interact -> swing ->
 * restore) и синхронизирует кэш {@code lastSelectedSlot}. Это убирает жалобы Grim вида
 * {@code Post held item change} / {@code PacketOrderO}. Дополнительно модуль ждёт отпускания
 * ПКМ, чтобы удар не попадал в тик ванильного right-click ({@code PacketOrderE ... rightClicking=true}).
 */
@ModuleInfo(
        name = "AutoExplosion",
        description = "Автоматически подрывает поставленный кристалл эндера или якорь возрождения.",
        category = ModuleCategory.COMBAT
)
public final class AutoExplosionModule extends CrystalModule {
    private static AutoExplosionModule instance;

    private static final String TRIGGER_CRYSTALS = "Кристаллы";
    private static final String TRIGGER_ANCHORS = "Якоря";

    private static final int STAGE_IDLE = 0;
    private static final int STAGE_CHARGE = 1;
    private static final int STAGE_EXPLODE = 2;
    private static final int MAX_WAIT_TICKS = 10;
    private static final int MAX_TARGET_ATTEMPTS = 40;
    private static final double MAX_ANCHOR_DISTANCE_SQ = 36.0;

    private final BlockPlacement placement = new BlockPlacement();

    private final BooleanSetting crystals = this.addSetting(
            new BooleanSetting("Кристаллы", "Подрывать кристаллы эндера.", true)
    );
    private final BooleanSetting anchor = this.addSetting(
            new BooleanSetting("Якоря возрождения", "Взаимодействовать с якорями возрождения.", true)
    );
    private final MultiSelectSetting triggers = this.addSetting(
            new MultiSelectSetting(
                    "Триггеры",
                    "Какие цели обрабатывать автоматически.",
                    new String[]{TRIGGER_CRYSTALS, TRIGGER_ANCHORS},
                    TRIGGER_CRYSTALS,
                    TRIGGER_ANCHORS
            )
    );
    private final BooleanSetting anyItemClick = this.addSetting(
            new BooleanSetting(
                    "Любым предметом",
                    "Реагировать на клик по блоку любым предметом, а не только кристаллом или светокамнем.",
                    false
            )
    );

    private BlockPos detonateTarget;
    private BlockPos placeTarget;
    private BlockPos anchorTarget;

    private int anchorStage = STAGE_IDLE;
    private int anchorWait;
    private int targetAttempts;

    private boolean useKeyWasPressed;

    private final EventListener<PlayerTickEvent> tickListener = event -> {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            this.resetTargets();
            this.useKeyWasPressed = false;
            return;
        }

        boolean pressed = mc.options.useKey.isPressed();
        boolean justPressed = pressed && !this.useKeyWasPressed;
        this.useKeyWasPressed = pressed;

        if (justPressed
                && this.detonateTarget == null
                && this.placeTarget == null
                && this.anchorTarget == null) {
            this.detectClick();
        }

        // Grim: не отправляем свои действия в тик, когда клиент сам держит right-click.
        // Ждём отпускания кнопки — тогда upstream уже отправил RELEASE_USE_ITEM и
        // предсказание rightClicking=false.
        if (pressed) {
            return;
        }

        if (this.detonateTarget != null) {
            if (++this.targetAttempts > MAX_TARGET_ATTEMPTS || this.detonateCrystal(this.detonateTarget)) {
                this.detonateTarget = null;
                this.targetAttempts = 0;
            }
            return;
        }

        if (this.placeTarget != null) {
            if (++this.targetAttempts > MAX_TARGET_ATTEMPTS || this.placeCrystal(this.placeTarget)) {
                this.placeTarget = null;
                this.targetAttempts = 0;
            }
            return;
        }

        if (this.anchorTarget != null) {
            if (++this.targetAttempts > MAX_TARGET_ATTEMPTS || this.useAnchor(this.anchorTarget)) {
                this.anchorTarget = null;
                this.targetAttempts = 0;
            }
        }
    };

    private final EventListener<PacketEvent> packetListener = event -> {
        if (!event.isIncoming() || mc.world == null || mc.player == null || !this.isEnabled()) {
            return;
        }

        Packet<?> packet = event.getPacket();
        if (!(packet instanceof BlockUpdateS2CPacket update)) {
            return;
        }

        BlockPos pos = update.getPos().toImmutable();
        Block block = update.getState().getBlock();

        if (this.anchorEnabled()
                && block == Blocks.RESPAWN_ANCHOR
                && update.getState().get(RespawnAnchorBlock.CHARGES) == 0) {
            mc.execute(() -> {
                if (mc.player == null
                        || this.anchorTarget != null
                        || this.anchorStage != STAGE_IDLE) {
                    return;
                }
                if (mc.player.squaredDistanceTo(pos.toCenterPos()) > MAX_ANCHOR_DISTANCE_SQ) {
                    return;
                }
                this.anchorTarget = pos;
                this.anchorStage = STAGE_IDLE;
                this.targetAttempts = 0;
            });
        } else if (this.crystalsEnabled() && this.isCrystalSurface(block)) {
            mc.execute(() -> {
                if (this.detonateTarget == null && this.findCrystal(pos) != null) {
                    this.detonateTarget = pos;
                    this.targetAttempts = 0;
                }
            });
        }
    };

    private void detectClick() {
        if (!(mc.crosshairTarget instanceof BlockHitResult hit)) {
            return;
        }

        BlockPos pos = hit.getBlockPos();
        Block block = mc.world.getBlockState(pos).getBlock();
        ItemStack held = mc.player.getStackInHand(Hand.MAIN_HAND);

        if (this.crystalsEnabled() && this.isCrystalSurface(block)) {
            if (this.findCrystal(pos) != null) {
                if (held.isOf(Items.END_CRYSTAL) || this.anyItemClick.isChecked()) {
                    this.detonateTarget = pos;
                    this.targetAttempts = 0;
                }
            } else if (!held.isOf(Items.END_CRYSTAL) && this.anyItemClick.isChecked()) {
                // Если кристалл в руке — установку делает ванильный клик, не дублируем пакет.
                this.placeTarget = pos;
                this.targetAttempts = 0;
            }
        } else if (this.anchorEnabled()
                && block == Blocks.RESPAWN_ANCHOR
                && this.anyItemClick.isChecked()
                && !held.isOf(Items.GLOWSTONE)
                && !held.isOf(Items.RESPAWN_ANCHOR)) {
            // Светокамень/якорь в руке обрабатывает ванильный right-click.
            this.anchorTarget = pos;
            this.anchorStage = STAGE_IDLE;
            this.targetAttempts = 0;
        }
    }

    private boolean detonateCrystal(BlockPos pos) {
        EndCrystalEntity crystal = this.findCrystal(pos);
        if (crystal == null) {
            return true;
        }

        if (!this.isWithinEntityReach(crystal)) {
            return true;
        }

        // Сначала доводим серверный поворот до кристалла, потом бьём.
        if (!this.prepareEntityInteraction(crystal, mc.player.getEntityInteractionRange(), true)) {
            return false;
        }

        if (!InteractionGate.available()) {
            return false;
        }

        this.placement.attack(crystal, mc.player.getInventory().getSelectedSlot());
        return true;
    }

    private boolean placeCrystal(BlockPos pos) {
        if (!this.isCrystalSurface(mc.world.getBlockState(pos).getBlock()) || this.findCrystal(pos) != null) {
            return true;
        }

        int slot = InventoryUtils.findHotbarSlot(Items.END_CRYSTAL);
        if (slot < 0) {
            return true;
        }

        if (!InteractionGate.available()) {
            return false;
        }

        BlockHitResult hit = this.prepareBlockInteraction(pos, true, true);
        if (hit == null) {
            return false;
        }

        return this.placement.interact(Hand.MAIN_HAND, slot, hit, false);
    }

    private boolean useAnchor(BlockPos pos) {
        if (!mc.world.getBlockState(pos).isOf(Blocks.RESPAWN_ANCHOR)) {
            this.finishAnchor();
            return true;
        }

        int charges = mc.world.getBlockState(pos).get(RespawnAnchorBlock.CHARGES);

        if (this.anchorStage == STAGE_IDLE) {
            this.anchorWait = 0;
            this.anchorStage = charges == 0 ? STAGE_CHARGE : STAGE_EXPLODE;
        }

        if (this.anchorStage == STAGE_CHARGE) {
            int slot = InventoryUtils.findHotbarSlot(Items.GLOWSTONE);
            if (slot < 0) {
                this.finishAnchor();
                return true;
            }

            if (!InteractionGate.available()) {
                return false;
            }

            BlockHitResult hit = this.prepareBlockInteraction(pos, false, true);
            if (hit == null) {
                return false;
            }

            if (!this.placement.interact(Hand.MAIN_HAND, slot, hit, false)) {
                return false;
            }

            this.anchorStage = STAGE_EXPLODE;
            this.anchorWait = 0;
            return false;
        }

        if (charges == 0) {
            this.anchorWait++;
            if (this.anchorWait > MAX_WAIT_TICKS) {
                this.finishAnchor();
                return true;
            }
            return false;
        }

        int slot = InventoryUtils.findHotbarSlot(Items.RESPAWN_ANCHOR);
        if (slot < 0) {
            this.finishAnchor();
            return true;
        }

        if (!InteractionGate.available()) {
            return false;
        }

        BlockHitResult hit = this.prepareBlockInteraction(pos, false, true);
        if (hit == null) {
            return false;
        }

        if (!this.placement.interact(Hand.MAIN_HAND, slot, hit, false)) {
            return false;
        }

        this.finishAnchor();
        return true;
    }

    private void finishAnchor() {
        this.anchorStage = STAGE_IDLE;
        this.anchorWait = 0;
        this.targetAttempts = 0;
    }

    private EndCrystalEntity findCrystal(BlockPos pos) {
        BlockPos checkPos = this.isCrystalSurface(mc.world.getBlockState(pos).getBlock()) ? pos.up() : pos;

        for (Entity entity : mc.world.getEntitiesByClass(EndCrystalEntity.class, new Box(checkPos), Entity::isAlive)) {
            if (entity instanceof EndCrystalEntity crystal) {
                return crystal;
            }
        }
        return null;
    }

    private boolean isCrystalSurface(Block block) {
        return block == Blocks.OBSIDIAN || block == Blocks.BEDROCK;
    }

    private boolean crystalsEnabled() {
        return this.crystals.isChecked() && this.triggers.isSelected(TRIGGER_CRYSTALS);
    }

    private boolean anchorEnabled() {
        return this.anchor.isChecked() && this.triggers.isSelected(TRIGGER_ANCHORS);
    }

    private void resetTargets() {
        this.detonateTarget = null;
        this.placeTarget = null;
        this.anchorTarget = null;
        this.anchorStage = STAGE_IDLE;
        this.anchorWait = 0;
        this.targetAttempts = 0;
    }

    @Override
    protected void onEnable() {
        this.resetTargets();
        this.useKeyWasPressed = false;
    }

    @Override
    protected void onDisable() {
        this.resetTargets();
        this.useKeyWasPressed = false;
    }

    private AutoExplosionModule() {
    }

    public static AutoExplosionModule getInstance() {
        if (instance == null) {
            instance = new AutoExplosionModule();
        }
        return instance;
    }
}
