package greed.client.module.combat.crystalmodules.utils.explosive;

import greed.client.RotationAngles;
import greed.client.event.*;
import greed.client.event.EventListener;
import greed.client.module.combat.AttackAuraModule;
import greed.client.module.combat.AutoTotemModule;
import greed.client.module.combat.crystalmodules.AnchorAura;
import greed.client.module.combat.crystalmodules.CrystalAura;
import greed.client.module.combat.crystalmodules.utils.CrystalDamage;
import greed.client.module.combat.crystalmodules.utils.math.TaskPriority;
import greed.client.storage.FriendStore;
import greed.client.util.player.placement.BlockPlacement;
import greed.client.util.player.placement.PlacementGeometry;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.*;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.attribute.EnvironmentAttributes;
import greed.client.module.combat.crystalmodules.utils.explosive.ExplosiveCycle.Step;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;

/** Shared fast sequences, native entity confirmations and a common self-damage budget. */
public final class ExplosiveCoordinator {
    private static final ExplosiveCoordinator INSTANCE = new ExplosiveCoordinator();
    private static boolean registered;
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final BlockPlacement placement = new BlockPlacement();
    private final ExplosivePackets packets = new ExplosivePackets();
    private Action transmitted;
    private boolean createdAnchor, waitingCreatedAnchor, predictedAir, optimisticMissed;
    private int optimisticAirTick = -1;
    private int replacedCrystalId = -1;
    private Object detonationKey;
    private double detonationRisk;
    private final ExplosiveMining mining = new ExplosiveMining();
    private record ClearanceGoal(ExplosiveModule module, BlockPos base, boolean anchor,
                                 boolean underTarget, Set<BlockPos> removed) {}
    private ClearanceGoal clearanceGoal;
    private PlayerEntity clearanceVictim;
    private Action clearanceAction;
    private final Map<BlockPos, Integer> miningBackoff = new HashMap<>();

    private record Action(
            ExplosiveCycle.Step step,
            ExplosiveModule module,
            BlockPos block,
            BlockHitResult hit,
            EndCrystalEntity crystal,
            Vec3d point,
            double enemy,
            double self,
            double priority,
            int expectedCharge) {
        Vec3d pointForBlast() {
            return crystal != null
                    ? crystal.getEntityPos()
                    : step == Step.CRYSTAL_PLACE || step == Step.OBSIDIAN_PLACE
                            ? new Vec3d(block.getX() + .5, block.getY() + 1, block.getZ() + .5)
                            : Vec3d.ofCenter(block);
        }

        Object explosionKey() {
            return crystal != null
                    ? crystal.getUuid()
                    : step.name().startsWith("ANCHOR")
                            ? INSTANCE.anchorKey(block, step == Step.ANCHOR_PLACE)
                            : "future:" + block;
        }

        String retryKey() {
            return step + ":" + (crystal == null ? block : crystal.getUuid());
        }

        boolean explosive() {
            return step == Step.CRYSTAL_BREAK || step == Step.ANCHOR_BREAK;
        }
    }

    private record Envelope(ClientWorld world, Packet<?> packet) {}

    private record Attempt(int count, int blockedUntil) {}

    private final ConcurrentLinkedQueue<Envelope> inbox = new ConcurrentLinkedQueue<>();
    private final Map<BlockPos, BlockState> confirmedAnchors = new HashMap<>();
    private final Map<String, Attempt> retries = new HashMap<>();
    private final MotionPrediction motion = new MotionPrediction();
    private BlockPos crystalFocus, anchorFocus;
    private int crystalFocusUntil, anchorFocusUntil;
    private int observedSpawnId = -1;
    private boolean placementGone, anchorSeen, obsidianSeen;
    private final ExplosionRules budget = new ExplosionRules();
    private ClientWorld world;
    private PlayerEntity victim;
    private PlayerEntity localPlayer;

    private record AnchorKey(BlockPos position, int generation) {}

    private final Map<BlockPos, Integer> anchorGenerations = new HashMap<>();

    private Object anchorKey(BlockPos pos, boolean future) {
        return new AnchorKey(pos, anchorGenerations.getOrDefault(pos, 0) + (future ? 1 : 0));
    }

    private Action planned, pending, lastAim;

    private ExplosiveModule focus;
    private int focusSince;
    private final ExplosiveAim aimMotion = new ExplosiveAim();
    private AngleConstructor rotationPlan;
    private ExplosiveModule rotationOwner;

    private record DamageKey(
            Vec3d source,
            int player,
            boolean anchor,
            BlockPos ignored,
            boolean conservative,
            BlockPos ghost,
            Vec3d offset) {}

    private final Map<DamageKey, Double> damageCache = new HashMap<>();

    private record ForecastKey(UUID player, double ticks, BlockPos ghost) {}

    private final Map<ForecastKey, Vec3d> forecasts = new HashMap<>();
    private int sentTick, nextTick, ownedTick = -1, urgentId = -1, lastPlanned = -1;
    private String state = "Idle";

    public static void ensureRegistered() {
        if (!registered) {
            registered = true;
            EventBus.getInstance().subscribe(INSTANCE);
        }
    }

    public static PlayerEntity target() {
        return INSTANCE.victim;
    }

    public static boolean ownsTick() {
        return INSTANCE.mc.player != null && INSTANCE.ownedTick == INSTANCE.mc.player.age;
    }

    public record MiningView(BlockPos position, float progress) {}
    public static MiningView miningView() {
        return INSTANCE.mining.active() ? new MiningView(INSTANCE.mining.block(), INSTANCE.mining.fraction()) : null;
    }

    public static String status() {
        return INSTANCE.state;
    }

    public static void cancelPreview() {
        INSTANCE.planned = null;
        INSTANCE.stopMining();
        INSTANCE.release();
    }

    private final RotateConstructor smooth =
            new RotateConstructor("ExplosiveSilent") {
                @Override
                public Angle limitAngleChange(
                        Angle current, Angle desired, Vec3d point, Entity entity) {
                    if (rotationOwner == null) return current;
                    ExplosiveAim.Rotation next = aimMotion.step(
                            current.getYaw(), current.getPitch(), desired.getYaw(), desired.getPitch(),
                            rotationOwner.rotationSpeed.getValue(), rotationOwner.rotationSmoothness.getValue());
                    return new Angle(next.yaw(), next.pitch());
                }

                @Override
                public Vec3d randomValue() {
                    return Vec3d.ZERO;
                }
            };

    private void release() {
        if (rotationOwner != null) AngleConnection.INSTANCE.release(rotationOwner, rotationPlan);
        rotationPlan = null;
        rotationOwner = null;
        lastAim = null;
        aimMotion.reset();
    }

    private boolean enabled(ExplosiveModule m) {
        return m != null && m.isState();
    }

    private void reset() {
        stopMining();
        miningBackoff.clear();
        release();
        victim = null;
        planned = pending = null;
        transmitted = null;
        waitingCreatedAnchor = createdAnchor = false;
        predictedAir = optimisticMissed = false;
        optimisticAirTick = -1;
        replacedCrystalId = -1;
        budget.clear();
        inbox.clear();
        confirmedAnchors.clear();
        anchorGenerations.clear();
        retries.clear();
        motion.clear();
        observedSpawnId = -1;
        placementGone = anchorSeen = obsidianSeen = false;
        crystalFocus = anchorFocus = null;
        nextTick = 0;
        ownedTick = -1;
        urgentId = -1;
        lastPlanned = -1;
        focus = null;
        focusSince = 0;
        state = "Idle";
    }




    private final EventListener<PacketEvent> swapSlotClickListener = event -> {
        if (event.isOutgoing() || mc.world == null) return;
        Packet<?> p = event.getPacket();
        if (p instanceof EntitySpawnS2CPacket
                || p instanceof EntitiesDestroyS2CPacket
                || p instanceof ExplosionS2CPacket
                || p instanceof BlockUpdateS2CPacket
                || p instanceof ChunkDeltaUpdateS2CPacket) {
            if (inbox.size() > 2048) inbox.clear();
            inbox.add(new Envelope(mc.world, p));
        }
    };


    private final EventListener<PlayerTickEvent> playerTickEventEventListener = event -> {
        if (mc.player == null || mc.world == null) return;
        CrystalAura crystal = CrystalAura.getInstance();
        AnchorAura anchor = AnchorAura.getInstance();
        if ((!enabled(crystal) && !enabled(anchor)) || paused()) return;
        if (lastPlanned == mc.player.age) return;
        EventBus.getInstance().post(new RotationUpdateEvent());
    };

    private final EventListener<RotationUpdateEvent> rotationUpdateEventEventListener = event ->{
        if (event.isPost()) return;
        if (world != mc.world) {
            reset();
            world = mc.world;
        }
        if (mc.player == null || world == null || mc.interactionManager == null) return;
        if (localPlayer != mc.player || lastPlanned > mc.player.age) {
            reset();
            localPlayer = mc.player;
        }
        int tick = mc.player.age;
        if (lastPlanned == tick) return;
        BlockPos previousMine = mining.block();
        mining.observe();
        if (previousMine != null && !mining.active()) {
            if (!world.getBlockState(previousMine).isAir()) miningBackoff.put(previousMine, tick + 40);
            clearanceGoal = null;
            clearanceAction = null;
        }
        miningBackoff.entrySet().removeIf(e -> tick >= e.getValue());
        lastPlanned = tick;
        damageCache.clear();
        forecasts.clear();
        budget.observe(hp(mc.player), tick);
        for (PlayerEntity player : world.getPlayers())
            motion.observe(player.getUuid(), player.getEntityPos(), tick, player.isOnGround(),
                    player.hasNoGravity() || player.isTouchingWater() || player.isClimbing() ? 0
                            : player.hasStatusEffect(StatusEffects.SLOW_FALLING) ? .01 : .08);
        drain();
        reconcile();
        retries.entrySet().removeIf(entry -> tick > entry.getValue().blockedUntil + 100);
        if (tick >= crystalFocusUntil) crystalFocus = null;
        if (tick >= anchorFocusUntil) anchorFocus = null;
        ownedTick = -1;
        planned = null;
        if (pending != null && (!enabled(pending.module)
                || victim == null || !valid(victim, pending.module.targetRange.getValue())))
            abandonPending();
        if (pending != null) {
            // Two-tick tempo: scheduling continues two ticks after every sent action instead of
            // idling the whole ping for an acknowledgement. An observation that arrives earlier
            // still confirms and clears the slot without any retry; an unconfirmed action simply
            // becomes a fresh rate-limited candidate, with a backoff after repeated dead cycles.
            if (tick - sentTick < 2) {
                state = "Await " + pending.step;
                if (enabled(pending.module) && !paused()) requestAim(pending);
                else release();
                return;
            }
            boolean expired = tick - sentTick >= timeout();
            String key = pending.retryKey();
            Attempt old = retries.get(key);
            int count = old == null ? 1 : old.count + 1;
            retries.put(key, new Attempt(count,
                    tick + (count >= 3 ? 40 : expired ? Math.min(4, Math.max(1, pingTicks())) : 1)));
            abandonPending();
            state = "Tempo";
        }
        CrystalAura crystal = CrystalAura.getInstance();
        AnchorAura anchor = AnchorAura.getInstance();
        if ((!enabled(crystal) && !enabled(anchor)) || paused()) {
            state = "Paused";
            stopMining();
            victim = null;
            release();
            return;
        }
        double targetRange =
                Math.max(
                        enabled(crystal) ? crystal.targetRange.getValue() : 0,
                        enabled(anchor) ? anchor.targetRange.getValue() : 0);
        victim = selectTarget(targetRange);
        if (victim == null) {
            state = "No target";
            stopMining();
            release();
            return;
        }
        if (mining.active()) {
            BlockHitResult miningHit = clearanceGoal == null ? null
                    : placement.interactionHit(mining.block(), clearanceGoal.module.range.getValue(), false);
            if (clearanceGoal == null || clearanceAction == null || victim != clearanceVictim
                    || !enabled(clearanceGoal.module) || !clearanceGoal.module.clearBlocks.getValue()
                    || !clearanceValid(clearanceGoal) || !clearanceSafe(clearanceGoal)
                    || miningHit == null) {
                stopMining();
            } else {
                clearanceAction = new Action(Step.BLOCK_CLEAR, clearanceGoal.module, mining.block(),
                        miningHit, null, miningHit.getPos(), 0, 0, clearanceAction.priority, 0);
                planned = clearanceAction;
                ownedTick = tick;
                state = "Clear " + mining.block().toShortString();
                requestAim(planned);
                return;
            }
        }
        // Plan and rotate during cooldown; normal pre-movement ordering gates interaction dispatch.
        boolean roof =
                !world.getBlockState(victim.getBlockPos().up(2))
                        .getCollisionShape(world, victim.getBlockPos().up(2))
                        .isEmpty();
        List<Action> candidates = new ArrayList<>();
        if (enabled(crystal)) crystals(crystal, candidates, !roof);
        if (enabled(anchor)) anchors(anchor, candidates, roof);
        candidates.removeIf(
                action -> {
                    Attempt attempt = retries.get(action.retryKey());
                    return attempt != null && tick < attempt.blockedUntil;
                });
        // Fair rotation: both auras keep getting turns instead of one of them starving the other.
        if (focus != crystal && focus != anchor) {
            focus = crystal;
            focusSince = tick;
        }
        if (focus != null && !enabled(focus)) {
            focus = focus == crystal ? anchor : crystal;
            focusSince = tick;
        }
        if (focus != null && !enabled(focus)) focus = null;
        ExplosiveModule other = focus == crystal ? anchor : crystal;
        Action focusBest = bestFor(focus, candidates);
        Action otherBest = bestFor(other, candidates);
        Action best;
        if (focusBest == null) {
            best = otherBest;
        } else if (otherBest != null
                && (otherBest.explosive() && !focusBest.explosive()
                        || otherBest.module == anchor
                                && (otherBest.step == Step.ANCHOR_CHARGE
                                        || otherBest.step == Step.ANCHOR_BREAK)
                                && otherBest.block.equals(anchorFocus)
                                && !focusBest.explosive()
                        || otherBest.module == crystal
                                && (otherBest.step == Step.CRYSTAL_PLACE
                                        || otherBest.step == Step.CRYSTAL_BREAK)
                                && otherBest.block.equals(crystalFocus)
                                && !focusBest.explosive()
                        || tick - focusSince > 40)) {
            // Finish the anchor or crystal we already placed, and never make a ready explosion
            // wait behind a preparation from the other module.
            focus = other;
            focusSince = tick;
            best = otherBest;
        } else {
            best = focusBest;
        }
        if (best == null) {
            best = planUnderTarget(anchor, null);
            if (best == null) best = planClearance(crystal, anchor);
        } else if (!best.explosive() && best.step != Step.ANCHOR_CHARGE) {
            Action under = planUnderTarget(anchor, best);
            if (under != null) best = under;
        }
        if (best == null) {
            state = "No safe action";
            release();
            return;
        }
        planned = best;
        ownedTick = tick;
        state = "Aim " + best.step;
        requestAim(best);
    };

    /** Hands the next turn to the other aura so both keep acting. */
    private void handOverTurn(ExplosiveModule module) {
        focus = module == CrystalAura.getInstance()
                ? AnchorAura.getInstance()
                : CrystalAura.getInstance();
        focusSince = mc.player.age;
    }

    /**
     * Ranks candidates inside a single module. Crystal and anchor used to share one ranking, so
     * whichever module had the higher fixed step rank starved the other one completely.
     */
    private Action bestFor(ExplosiveModule module, List<Action> candidates) {
        if (module == null) return null;
        List<Action> own = new ArrayList<>();
        for (Action candidate : candidates)
            if (candidate.module == module) own.add(candidate);
        if (own.isEmpty()) return null;
        Object previous = lastAim != null && lastAim.module == module ? lastAim.retryKey() : null;
        return ExplosiveCycle.select(
                own,
                action -> ExplosiveCycle.rank(action.step),
                Action::priority,
                Action::retryKey,
                previous);
    }

    private void requestAim(Action action) {
        if (rotationOwner != null && rotationOwner != action.module) {
            // Transfer ownership without discarding the current smoothed angle/velocity.
            Angle current = AngleConnection.INSTANCE.getRotation();
            AngleConnection.INSTANCE.release(rotationOwner, rotationPlan);
            AngleConnection.INSTANCE.setRotation(current);
        }
        rotationOwner = action.module;
        lastAim = action;
        ownedTick = mc.player.age;
        Vec3d eye = mc.player.getEyePos();
        Vec3d d = previewPoint(action).subtract(eye);
        if (action.crystal != null && action.crystal.getBoundingBox().contains(eye)) d = Vec3d.ZERO;
        Angle current = AngleConnection.INSTANCE.getRotation();
        float yaw =
                d.lengthSquared() < 1e-8
                        ? current.getYaw()
                        : (float) Math.toDegrees(Math.atan2(d.z, d.x)) - 90;
        float pitch =
                d.lengthSquared() < 1e-8
                        ? current.getPitch()
                        : (float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z)));
        rotationPlan =
                new AngleConfig(smooth, true, true)
                        .createRotationPlan(new Angle(yaw, pitch), Vec3d.ZERO, action.crystal, 1);
        AngleConnection.INSTANCE.rotateTo(
                rotationPlan, TaskPriority.HIGH_IMPORTANCE_3, rotationOwner);
    }

    private Vec3d previewPoint(Action action) {
        if (pending != action)
            return action.point;
        Vec3d eye = mc.player.getEyePos();
        // Aim preparation only: the world and interaction stage remain server-authoritative.
        if (action.step == Step.CRYSTAL_PLACE) {
            Vec3d source = action.pointForBlast();
            return inside(
                    new Box(
                            source.x - 1,
                            source.y,
                            source.z - 1,
                            source.x + 1,
                            source.y + 2,
                            source.z + 1),
                    eye);
        }
        if (action.step == Step.CRYSTAL_BREAK) {
            BlockHitResult next = face(action.block, action.module);
            return next == null ? action.point : next.getPos();
        }
        if (action.step != Step.OBSIDIAN_PLACE && action.step != Step.ANCHOR_PLACE)
            return action.point;
        Box box = new Box(action.block);
        Vec3d best = action.point;
        double distance = Double.POSITIVE_INFINITY;
        if (box.contains(eye)) return best;
        for (Direction side : Direction.values()) {
            Vec3d point = PlacementGeometry.facePoint(box, side, eye);
            BlockHitResult entry =
                    VoxelShapes.fullCube()
                            .raycast(
                                    eye,
                                    point.add(point.subtract(eye).normalize().multiply(.001)),
                                    action.block);
            if (entry == null || entry.isInsideBlock() || entry.getSide() != side) continue;
            double cost = eye.squaredDistanceTo(point);
            if (cost < distance) {
                distance = cost;
                best = point;
            }
        }
        return best;
    }

    public static void beforeMovement() {
        if (registered && INSTANCE.planned != null && INSTANCE.planned.step == Step.BLOCK_CLEAR)
            INSTANCE.dispatch(false);
    }

    public static void afterMovement() {
        if (registered && INSTANCE.planned != null && INSTANCE.planned.step != Step.BLOCK_CLEAR)
            INSTANCE.dispatch(true);
    }

    /** Called only after native packet handlers have applied their authoritative world changes. */
    public static void onWorldPacketApplied() {
        if (!registered) return;
        var self=INSTANCE;
        if (self.mc.player==null || self.mc.world!=self.world || !self.mc.isOnThread()
                || self.pending==null || self.packets.isBusy()) return;
        self.drain();
        self.reconcile();
        if (self.pending!=null) return;
        self.lastPlanned=-1;
        EventBus.getInstance().post(new RotationUpdateEvent());
        if(self.planned!=null && self.planned.step!=Step.BLOCK_CLEAR) self.dispatch(true);
    }

    private void dispatch(boolean afterMovement) {
        if (planned == null
                || mc.player == null
                || mc.world != world
                || mc.player != localPlayer
                || !afterMovement && (AngleConnection.INSTANCE.getLastRotationPlan() != rotationPlan
                        || !greed.client.util.player.placement.InteractionGate.available())
                || paused()) return;
        if (!ExplosiveTempo.ready(mc.player.age, nextTick, pending != null)) return;
        Action a = planned;
        damageCache.clear();
        forecasts.clear();
        if (a.step == Step.BLOCK_CLEAR) {
            if (clearanceGoal != null && enabled(a.module) && a.module.clearBlocks.getValue()
                    && victim != null && victim.isAlive() && aimed(a)
                    && visible(a.hit.getPos(), a.block)
                    && clearanceValid(clearanceGoal) && clearanceSafe(clearanceGoal)) {
                mining.work(a.hit);

                if (mining.stopping()) {
                    if (clearanceGoal.anchor && clearanceGoal.underTarget)
                        placeAnchorAfterClear(clearanceGoal);
                    else if (!clearanceGoal.anchor)
                        placeObsidianAfterClear(clearanceGoal);
                }
            }
            return;
        }
        if (!enabled(a.module)
                || victim == null
                || !victim.isAlive()
                || !safe(
                        a.module,
                        a.pointForBlast(),
                        a.step.name().startsWith("ANCHOR"),
                        a.explosionKey(),
                        a.step == Step.OBSIDIAN_PLACE ? a.block : null,
                        preparation(a),
                        !(a.step.name().startsWith("ANCHOR") && sheltered(a.block)))) return;
        if (!validate(a) || !aimed(a)) return;
        if (send(a)) markSent(a);
    }

    /** Shared tail of the normal and the optimistic post-mining dispatch paths. */
    private void markSent(Action a) {
        handOverTurn(a.module);
        planned = null;
        pending = transmitted == null ? a : transmitted;
        waitingCreatedAnchor = createdAnchor;
        observedSpawnId = -1;
        placementGone = false;
        anchorSeen =
                a.step.name().startsWith("ANCHOR")
                        && world.getBlockState(a.block).isOf(Blocks.RESPAWN_ANCHOR);
        obsidianSeen =
                a.step == Step.OBSIDIAN_PLACE
                        && world.getBlockState(a.block).isOf(Blocks.OBSIDIAN);
        sentTick = mc.player.age;
        nextTick = sentTick + ExplosiveTempo.ACTION_INTERVAL;
        if (a.step == Step.OBSIDIAN_PLACE || a.step == Step.CRYSTAL_PLACE) {
            crystalFocus = a.block;
            crystalFocusUntil = sentTick + Math.max(30, timeout() * 2);
        }
        if (a.step.name().startsWith("ANCHOR")) {
            anchorFocus = a.block;
            anchorFocusUntil = sentTick + Math.max(30, timeout() * 2);
        }
        if (detonationKey != null)
            budget.spend(detonationKey, detonationRisk, sentTick, Math.max(6, pingTicks() + 3));
        state = "Await " + pending.step;
    }

    /**
     * Puts the anchor into the hole together with the final break packet. The block below the
     * victim is still solid on this client, so the normal support lookup rejects it; the server
     * processes the break first and then sees an empty position.
     */
    private boolean placeAnchorAfterClear(ClearanceGoal goal) {
        if (goal == null
                || !goal.anchor
                || !goal.underTarget
                || pending != null
                || !enabled(goal.module)
                || !underVictim(goal.base)) return false;
        BlockHitResult hit = optimisticHit(goal.base, goal.module, goal.removed);
        if (hit == null) return false;
        Action place =
                new Action(
                        Step.ANCHOR_PLACE,
                        goal.module,
                        goal.base,
                        hit,
                        null,
                        hit.getPos(),
                        0,
                        0,
                        0,
                        0);
        if (!send(place, true)) return false;
        markSent(place);
        predictedAir = true;
        return true;
    }

    /**
     * The crystal equivalent of {@link #placeAnchorAfterClear}: fill the just-mined base while the
     * server is still processing the break. Re-planning after the break acknowledgement is too
     * late because the victim has already started moving and the normal safety forecast rejects
     * the base. This also covers the case where the base was already air and the mined block was
     * the obstruction above it; the base can be filled in the same tick instead of waiting for the
     * obstruction acknowledgement. The obsidian itself is harmless, so it is sent even if the
     * follow-up crystal is re-evaluated (and gated) after the block confirmation.
     */
    private boolean placeObsidianAfterClear(ClearanceGoal goal) {
        if (goal == null
                || goal.anchor
                || pending != null
                || !enabled(goal.module)
                || !(goal.module instanceof CrystalAura crystal)
                || !crystal.placeObsidian.getValue()
                || !available(Items.OBSIDIAN)
                || world.getBlockState(goal.base).isOf(Blocks.OBSIDIAN)
                || !mining.stopping()) return false;
        // The base is either the block we just broke (still solid on this client) or it was
        // already replaceable and the break exposed it. If it is still solid and we broke a
        // different obstruction, the obsidian would have nowhere to go.
        if (!goal.base.equals(mining.block())
                && !world.getBlockState(goal.base).isReplaceable()) return false;
        BlockHitResult hit = optimisticHit(goal.base, goal.module, goal.removed);
        if (hit == null) return false;
        Action place =
                new Action(
                        Step.OBSIDIAN_PLACE,
                        goal.module,
                        goal.base,
                        hit,
                        null,
                        hit.getPos(),
                        0,
                        0,
                        0,
                        0);
        if (!send(place, true)) return false;
        markSent(place);
        predictedAir = true;
        return true;
    }

    /**
     * Support lookup for the optimistic post-mining placements. The target block is still solid
     * on this client, so the normal placement helpers (which require an empty target) cannot be
     * used; only the support face has to be valid. Hidden faces are allowed because the server
     * applies the preceding break first.
     */
    private BlockHitResult optimisticHit(
            BlockPos target, ExplosiveModule module, Set<BlockPos> removed) {
        BlockHitResult hit = null;
        BlockHitResult below = null;
        double best = Double.POSITIVE_INFINITY;
        for (Direction direction : Direction.values()) {
            BlockPos support = target.offset(direction);
            if (removed.contains(support)) continue;
            BlockState state = world.getBlockState(support);
            if (state.isReplaceable() || state.hasBlockEntity()) continue;
            BlockHitResult candidate =
                    placement.faceHit(
                            support,
                            direction.getOpposite(),
                            module.range.getValue(),
                            true);
            if (candidate == null) continue;
            if (direction == Direction.DOWN) below = candidate;
            double cost = mc.player.getEyePos().squaredDistanceTo(candidate.getPos());
            if (cost < best) {
                best = cost;
                hit = candidate;
            }
        }
        // Clicking the top of the floor directly under the base matches the downward aim and
        // leaves the least room for a server-side reach/visibility disagreement.
        return below != null ? below : hit;
    }

    private boolean paused() {
        if (AutoTotemModule.getInstance().isTotemActive()
                || mc.player == null
                || !mc.player.isAlive()
                || mc.player.hasVehicle()
                || mc.getCameraEntity() != mc.player
                || mc.player.isUsingItem()
                || mc.player.isSneaking()
                || mc.currentScreen != null
                || mc.player.currentScreenHandler != mc.player.playerScreenHandler
                || !mc.player.currentScreenHandler.getCursorStack().isEmpty()
                || mc.interactionManager.isBreakingBlock()) return true;
        return false;
    }

    private PlayerEntity selectTarget(double range) {
        LivingEntity attackTarget = AttackAuraModule.getInstance().getTarget();
        if (attackTarget instanceof PlayerEntity player && valid(player, range)) return player;
        if (valid(victim, range)) return victim;
        return world.getPlayers().stream()
                .filter(p -> valid(p, range))
                .min(Comparator.comparingDouble(mc.player::squaredDistanceTo))
                .orElse(null);
    }

    private boolean valid(Entity e, double range) {
        return e instanceof PlayerEntity p
                && p != mc.player
                && p.isAlive()
                && !p.isRemoved()
                && !p.isCreative()
                && !p.isSpectator()
                && !FriendStore.isFriend(p.getGameProfile().name())
                && p.getEntityWorld() == world
                && mc.player.squaredDistanceTo(p) <= range * range;
    }

    private void stopMining() {
        mining.cancel();
        clearanceGoal = null;
        clearanceVictim = null;
        clearanceAction = null;
    }

    /** Only the chosen placement column and its blast obstruction are cleared, never a whole sphere. */
    private Action planClearance(CrystalAura crystal, AnchorAura anchor) {
        List<ClearanceGoal> goals = new ArrayList<>();
        BlockPos center = victim.getBlockPos();
        for (int x = -4; x <= 4; x++)
            for (int y = -3; y <= 2; y++)
                for (int z = -4; z <= 4; z++) {
                    BlockPos base = center.add(x, y, z);
                    if (base.getSquaredDistance(center) > 16
                            || mc.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(base)) > 30.25) continue;
                    if (enabled(crystal) && crystal.clearBlocks.getValue() && available(Items.END_CRYSTAL)) {
                        BlockState state = world.getBlockState(base);
                        boolean foundation = state.isOf(Blocks.OBSIDIAN) || state.isOf(Blocks.BEDROCK);
                        if ((foundation || crystal.placeObsidian.getValue() && available(Items.OBSIDIAN))
                                && base.getSquaredDistance(center) <= square(crystal.searchRadius.getValue())
                                && world.getOtherEntities(null, new Box(base.up()).stretch(0, 1, 0),
                                        e -> e.isAlive() && !e.isSpectator()).isEmpty()
                                && (foundation || world.getOtherEntities(null, new Box(base),
                                        e -> e.isAlive() && !e.isSpectator()).isEmpty())) {
                            Set<BlockPos> removed = new HashSet<>();
                            if (!foundation && !state.isReplaceable()) removed.add(base);
                            if (!world.getBlockState(base.up()).isAir()) removed.add(base.up());
                            addBlastObstruction(base, false, removed);
                            if (!removed.isEmpty() && removed.size() <= 4)
                                goals.add(new ClearanceGoal(crystal, base, false, false, Set.copyOf(removed)));
                        }
                    }
                    if (enabled(anchor) && anchor.clearBlocks.getValue()
                            && available(Items.RESPAWN_ANCHOR) && available(Items.GLOWSTONE)
                            && !world.getBlockState(base).isOf(Blocks.RESPAWN_ANCHOR)
                            && world.getOtherEntities(null, new Box(base),
                                    e -> e.isAlive() && !e.isSpectator()).isEmpty()
                            && !world.getEnvironmentAttributes().getAttributeValue(
                                    EnvironmentAttributes.RESPAWN_ANCHOR_WORKS_GAMEPLAY, Vec3d.ofCenter(base))) {
                        Set<BlockPos> removed = new HashSet<>();
                        if (!world.getBlockState(base).isReplaceable()) removed.add(base);
                        addBlastObstruction(base, true, removed);
                        if (!removed.isEmpty())
                            goals.add(new ClearanceGoal(anchor, base, true,
                                    base.equals(center.down()), Set.copyOf(removed)));
                    }
                }
        goals.sort(Comparator.comparingDouble(g -> victim.squaredDistanceTo(Vec3d.ofCenter(g.base))
                + g.removed.size() * 2));
        ClearanceGoal best = null;
        BlockHitResult bestHit = null;
        double bestScore = -Double.MAX_VALUE;
        int evaluated = 0;
        for (ClearanceGoal goal : goals) {
            if (!clearanceValid(goal)) continue;
            BlockHitResult hit = null;
            for (BlockPos block : goal.removed) {
                if (miningBackoff.containsKey(block)) continue;
                // Mining uses an actually visible face even when hidden placements are enabled.
                BlockHitResult candidate = placement.interactionHit(block, goal.module.range.getValue(), false);
                if (candidate != null && (hit == null || mc.player.getEyePos().squaredDistanceTo(candidate.getPos())
                        < mc.player.getEyePos().squaredDistanceTo(hit.getPos()))) hit = candidate;
            }
            if (hit == null) continue;
            if (++evaluated > 24) break;
            if (!clearanceSafe(goal)) continue;
            double score = clearedDamage(goal, victim, false) - 1.5 * clearedDamage(goal, mc.player, !(goal.anchor && sheltered(goal.base)))
                    - goal.removed.size() * .5;
            if (score > bestScore) { best = goal; bestHit = hit; bestScore = score; }
        }
        if (best == null) return null;
        clearanceGoal = best;
        clearanceVictim = victim;
        clearanceAction = new Action(Step.BLOCK_CLEAR, best.module, bestHit.getBlockPos(), bestHit,
                null, bestHit.getPos(), 0, 0, bestScore, 0);
        return clearanceAction;
    }

    /** True when the base block still sits directly below the victim's feet. */
    private boolean underVictim(BlockPos base) {
        if (victim == null || !victim.isOnGround()) return false;
        BlockPos feet = victim.getBlockPos();
        // Slabs, carpets and water mean the feet block is not empty; an anchor in the block
        // below would be covered and could not reach the victim.
        return base.getX() == feet.getX()
                && base.getZ() == feet.getZ()
                && base.getY() == feet.getY() - 1
                && world.getBlockState(feet).isAir();
    }

    /**
     * An anchor in a hole or trench is surrounded by real cover. The conservative self-damage
     * model treats every normal block as air, which rejected exactly the sheltered spots this
     * aura is supposed to use; count the actual cover for these positions instead.
     */
    private boolean sheltered(BlockPos pos) {
        if (world.getBlockState(pos.down()).getCollisionShape(world, pos.down()).isEmpty())
            return false;
        int walls = 0;
        if (!world.getBlockState(pos.north()).getCollisionShape(world, pos.north()).isEmpty())
            walls++;
        if (!world.getBlockState(pos.south()).getCollisionShape(world, pos.south()).isEmpty())
            walls++;
        if (!world.getBlockState(pos.west()).getCollisionShape(world, pos.west()).isEmpty())
            walls++;
        if (!world.getBlockState(pos.east()).getCollisionShape(world, pos.east()).isEmpty())
            walls++;
        return walls >= 2;
    }

    /**
     * On flat ground the block below the victim is the only reliable anchor spot: the victim's
     * own block cannot be used and there is no side support at their feet. This goal used to be
     * reachable only when no other action existed at all, so crystal setup always starved it.
     */
    private Action planUnderTarget(AnchorAura anchor, Action currentBest) {
        if (anchor == null
                || !enabled(anchor)
                || victim == null
                || pending != null
                || !victim.isOnGround()
                || !anchor.clearBlocks.getValue()
                || !available(Items.RESPAWN_ANCHOR)
                || !available(Items.GLOWSTONE)
                || currentBest != null
                        && (currentBest.explosive()
                                || currentBest.step == Step.ANCHOR_CHARGE
                                // Do not abandon the crystal we just prepared for a new
                                // under-target anchor; the obsidian is already in place and the
                                // victim only has to stay there for one more action.
                                || currentBest.step == Step.CRYSTAL_PLACE
                                        && currentBest.block.equals(crystalFocus)))
            return null;
        BlockPos base = victim.getBlockPos().down();
        BlockState state = world.getBlockState(base);
        float hardness = state.getHardness(world, base);
        if (state.isOf(Blocks.RESPAWN_ANCHOR)
                || state.isReplaceable()
                || hardness < 0
                || hardness > 4f
                || !world.isInBuildLimit(base)
                || !world.getWorldBorder().contains(base)
                || mc.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(base))
                        > square(anchor.range.getValue() + 1)
                || !world.getOtherEntities(null, new Box(base),
                        e -> e.isAlive() && !e.isSpectator()).isEmpty()
                || world.getEnvironmentAttributes().getAttributeValue(
                        EnvironmentAttributes.RESPAWN_ANCHOR_WORKS_GAMEPLAY, Vec3d.ofCenter(base)))
            return null;
        Set<BlockPos> removed = new HashSet<>();
        removed.add(base);
        ClearanceGoal goal = new ClearanceGoal(anchor, base, true, true, Set.copyOf(removed));
        if (!clearanceValid(goal) || !clearanceSafe(goal)) return null;
        BlockHitResult hit = null;
        for (BlockPos block : goal.removed) {
            if (miningBackoff.containsKey(block)) continue;
            // Mining uses an actually visible face even when hidden placements are enabled.
            BlockHitResult candidate =
                    placement.interactionHit(block, anchor.range.getValue(), false);
            if (candidate != null
                    && (hit == null
                            || mc.player.getEyePos().squaredDistanceTo(candidate.getPos())
                                    < mc.player.getEyePos().squaredDistanceTo(hit.getPos())))
                hit = candidate;
        }
        if (hit == null) return null;
        double score =
                clearedDamage(goal, victim, false)
                        - 1.5 * clearedDamage(goal, mc.player, !(goal.anchor && sheltered(goal.base)))
                        - goal.removed.size() * .5;
        if (score <= 0) return null;
        clearanceGoal = goal;
        clearanceVictim = victim;
        clearanceAction =
                new Action(
                        Step.BLOCK_CLEAR,
                        anchor,
                        hit.getBlockPos(),
                        hit,
                        null,
                        hit.getPos(),
                        0,
                        0,
                        score,
                        0);
        return clearanceAction;
    }

    private void addBlastObstruction(BlockPos base, boolean anchor, Set<BlockPos> removed) {
        Vec3d source = anchor ? Vec3d.ofCenter(base)
                : new Vec3d(base.getX() + .5, base.getY() + 1, base.getZ() + .5);
        BlockHitResult hit = world.raycast(new RaycastContext(source, victim.getBoundingBox().getCenter(),
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
        if (hit.getType() == HitResult.Type.BLOCK && !hit.getBlockPos().equals(base))
            removed.add(hit.getBlockPos().toImmutable());
    }

    private boolean clearanceValid(ClearanceGoal goal) {
        if (victim == null || !valid(victim, goal.module.targetRange.getValue())
                || !world.isInBuildLimit(goal.base) || !world.getWorldBorder().contains(goal.base)
                || (goal.anchor ? !available(Items.RESPAWN_ANCHOR) || !available(Items.GLOWSTONE)
                        : !available(Items.END_CRYSTAL))
                || goal.base.getSquaredDistance(victim.getBlockPos()) > 16
                || goal.anchor && world.getEnvironmentAttributes().getAttributeValue(
                        EnvironmentAttributes.RESPAWN_ANCHOR_WORKS_GAMEPLAY, Vec3d.ofCenter(goal.base))) return false;
        // Stop digging the moment the victim steps off the hole instead of wasting the whole
        // mining interval and placing the anchor behind them.
        if (goal.underTarget && !underVictim(goal.base)) return false;
        for (BlockPos block : goal.removed)
            if (!world.getBlockState(block).isAir()
                    && (block.getSquaredDistance(victim.getBlockPos()) > 16 || !mining.mineable(block))) return false;
        // A real adjacent support must survive the clearance.
        boolean support = false;
        boolean foundation = !goal.anchor && (world.getBlockState(goal.base).isOf(Blocks.OBSIDIAN)
                || world.getBlockState(goal.base).isOf(Blocks.BEDROCK));
        if (!foundation) {
            if (!goal.anchor && (!((CrystalAura)goal.module).placeObsidian.getValue()
                    || !available(Items.OBSIDIAN))) return false;
            if (!world.getOtherEntities(null, new Box(goal.base),
                    e -> e.isAlive() && !e.isSpectator()).isEmpty()) return false;
            for (Direction side : Direction.values()) {
                BlockPos pos = goal.base.offset(side);
                if (!goal.removed.contains(pos) && !world.getBlockState(pos).isReplaceable()
                        && !world.getBlockState(pos).getOutlineShape(world, pos).isEmpty()) support = true;
            }
            if (!support) return false;
        }
        if (!goal.anchor) {
            if (!world.getOtherEntities(null, new Box(goal.base.up()).stretch(0, 1, 0),
                    e -> e.isAlive() && !e.isSpectator()).isEmpty()) return false;
            Vec3d eye = mc.player.getEyePos();
            Vec3d source = new Vec3d(goal.base.getX()+.5, goal.base.getY()+1, goal.base.getZ()+.5);
            Box box = new Box(source.x-1, source.y, source.z-1, source.x+1, source.y+2, source.z+1);
            if (eye.squaredDistanceTo(closest(box, eye)) > square(Math.min(
                    ((CrystalAura)goal.module).attackRange.getValue(), mc.player.getEntityInteractionRange()))) return false;
        }
        return true;
    }

    private double clearedDamage(ClearanceGoal goal, PlayerEntity player, boolean conservative) {
        Vec3d source = goal.anchor ? Vec3d.ofCenter(goal.base)
                : new Vec3d(goal.base.getX()+.5, goal.base.getY()+1, goal.base.getZ()+.5);
        BlockPos ghost = goal.anchor ? null : goal.base;
        // An excavated base is refilled with obsidian before the crystal explosion.
        Set<BlockPos> removed = new HashSet<>(goal.removed);
        if (ghost != null) removed.remove(ghost);
        return CrystalDamage.sampleCleared(source, player, ghost, goal.anchor ? goal.base : null,
                goal.anchor, conservative, Vec3d.ZERO, removed);
    }

    private boolean clearanceSafe(ClearanceGoal goal) {
        double enemy = clearedDamage(goal, victim, false), own = clearedDamage(goal, mc.player, !(goal.anchor && sheltered(goal.base)));
        if (!budget.safe(enemy, own, hp(victim), hp(mc.player), goal.module.minimum.getValue(),
                goal.module.maximum.getValue(), goal.module.reserve.getValue(), "clear:" + goal.base)
                || enemy - 1.5 * own <= 0 && enemy < hp(victim)) return false;
        if (goal.module.friends.getValue())
            for (PlayerEntity friend : world.getPlayers())
                if (FriendStore.isFriend(friend.getGameProfile().name())) {
                    double hurt = clearedDamage(goal, friend, true);
                    if (hurt > goal.module.maximum.getValue()
                            || hp(friend) - hurt - .5 < goal.module.reserve.getValue()) return false;
                }
        return true;
    }

    private void crystals(CrystalAura module, List<Action> result, boolean preferred) {
        if (crystalAttackSlot() < 0) return;
        List<EndCrystalEntity> entities =
                world.getEntitiesByClass(
                        EndCrystalEntity.class,
                        mc.player.getBoundingBox().expand(module.attackRange.getValue() + 2),
                        c -> c.isAlive() && !c.isRemoved());
        entities.sort(
                Comparator.<EndCrystalEntity>comparingInt(
                                c ->
                                        c.getId() == urgentId
                                                ? 0
                                                : crystalFocus != null
                                                                && c.getBlockPos()
                                                                        .down()
                                                                        .equals(crystalFocus)
                                                        ? 1
                                                        : 2)
                        .thenComparingDouble(c -> c.squaredDistanceTo(victim)));
        int evaluated = 0;
        for (EndCrystalEntity c : entities) {
            if (evaluated >= 24) break;
            double reach =
                    Math.min(module.attackRange.getValue(), mc.player.getEntityInteractionRange());
            Vec3d aim = crystalAim(c.getBoundingBox(), module, reach);
            if (aim == null) continue;
            evaluated++;
            Vec3d source = c.getEntityPos();
            if (safe(module, source, false, c.getUuid()))
                result.add(
                        new Action(
                                Step.CRYSTAL_BREAK,
                                module,
                                c.getBlockPos().down(),
                                null,
                                c,
                                aim,
                                damage(source, victim, false, null),
                                selfDamage(source, false, null),
                                score(module, source, false, 0, preferred)
                                        + (c.getId() == urgentId ? .25 : 0),
                                0));
        }
        if (result.stream().anyMatch(action -> action.module==module && action.step==Step.CRYSTAL_BREAK)) return;
        if (!available(Items.END_CRYSTAL)) return;
        List<BlockPos> bases = new ArrayList<>();
        Vec3d targetPoint = victim.getEntityPos().add(forecast(victim,horizon(module,1),null));
        BlockPos center = BlockPos.ofFloored(targetPoint);
        int radius = module.searchRadius.getInt();
        for (int x = -radius; x <= radius; x++)
            for (int y = -radius; y <= radius; y++)
                for (int z = -radius; z <= radius; z++) {
                    BlockPos b = center.add(x, y, z);
                    if (b.getSquaredDistance(center) > radius * radius) continue;
                    BlockState state = world.getBlockState(b);
                    boolean foundation = state.isOf(Blocks.OBSIDIAN) || state.isOf(Blocks.BEDROCK);
                    if ((!foundation
                                    && (!module.placeObsidian.getValue()
                                            || !available(Items.OBSIDIAN)
                                            || !placeable(b)))
                            || !world.getBlockState(b.up()).isAir()) continue;
                    if (!world.getOtherEntities(
                                    null,
                                    new Box(b.up()).stretch(0, 1, 0),
                                    e -> e.isAlive() && !e.isSpectator())
                            .isEmpty()) continue;
                    if (mc.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(b))
                            > square(module.range.getValue() + 1)) continue;
                    bases.add(b);
                }
        bases.sort(
                Comparator.<BlockPos>comparingInt(b -> b.equals(crystalFocus) ? 0 : 1)
                        .thenComparingDouble(b -> targetPoint.squaredDistanceTo(Vec3d.ofCenter(b))));
        int existing = 0, prepared = 0;
        for (BlockPos b : bases) {
            BlockState state = world.getBlockState(b);
            boolean foundation = state.isOf(Blocks.OBSIDIAN) || state.isOf(Blocks.BEDROCK);
            if (foundation ? existing >= 24 : prepared >= 24) continue;
            BlockHitResult hit = foundation ? face(b, module) : support(b, module);
            if (hit == null) continue;
            if (foundation) existing++;
            else prepared++;
            BlockPos ghost = foundation ? null : b;
            Vec3d source = new Vec3d(b.getX() + .5, b.getY() + 1, b.getZ() + .5);
            Box futureCrystal =
                    new Box(
                            source.x - 1,
                            source.y,
                            source.z - 1,
                            source.x + 1,
                            source.y + 2,
                            source.z + 1);
            if (mc.player
                            .getEyePos()
                            .squaredDistanceTo(closest(futureCrystal, mc.player.getEyePos()))
                    > square(
                            Math.min(
                                    module.attackRange.getValue(),
                                    mc.player.getEntityInteractionRange()))) continue;
            if (crystalAim(
                            futureCrystal,
                            module,
                            Math.min(
                                    module.attackRange.getValue(),
                                    mc.player.getEntityInteractionRange()))
                    == null) continue;
            int preparation = foundation ? 1 : 2;
            if (safe(module, source, false, "future:" + b, ghost, preparation)) {
                double enemy = benefit(module, source, false, null, ghost, preparation),
                        own = risk(module, source, false, null, ghost, preparation);
                result.add(
                        new Action(
                                foundation ? Step.CRYSTAL_PLACE : Step.OBSIDIAN_PLACE,
                                module,
                                b,
                                hit,
                                null,
                                hit.getPos(),
                                enemy,
                                own,
                                ExplosionRules.score(enemy, own, foundation ? 1 : 2, preferred),
                                0));
            }
        }
    }

    private void anchors(AnchorAura module, List<Action> result, boolean preferred) {
        Vec3d targetPoint = victim.getEntityPos().add(forecast(victim,horizon(module,1),null));
        Set<BlockPos> centers = new LinkedHashSet<>(List.of(victim.getBlockPos(),BlockPos.ofFloored(targetPoint)));
        if(anchorFocus!=null)centers.add(anchorFocus);
        Set<BlockPos> unique = new LinkedHashSet<>();
        for (BlockPos center : centers)
        for (int x = -2; x <= 2; x++)
            for (int z = -2; z <= 2; z++)
                for (int y = -1; y <= 2; y++) {
                    BlockPos p = center.add(x, y, z);
                    if (!world.isInBuildLimit(p)
                            || !world.getWorldBorder().contains(p)
                            || world.getEnvironmentAttributes()
                                    .getAttributeValue(
                                            EnvironmentAttributes.RESPAWN_ANCHOR_WORKS_GAMEPLAY,
                                            Vec3d.ofCenter(p))) continue;
                    BlockState actual = world.getBlockState(p);
                    if (!actual.isOf(Blocks.RESPAWN_ANCHOR) && !actual.isReplaceable()) continue;
                    if (mc.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(p))
                            > square(module.range.getValue() + 1)) continue;
                    unique.add(p);
                }
        List<BlockPos> positions = new ArrayList<>(unique);
        positions.sort(
                Comparator.<BlockPos>comparingInt(
                                p ->
                                        p.equals(anchorFocus)
                                                ? 0
                                                : world.getBlockState(p).isOf(Blocks.RESPAWN_ANCHOR)
                                                        ? 1
                                                        : 2)
                        .thenComparingDouble(p -> targetPoint.squaredDistanceTo(Vec3d.ofCenter(p))));
        int evaluated = 0;
        for (BlockPos pos : positions) {
            if (evaluated >= 24) break;
            BlockState observed = world.getBlockState(pos);
            boolean exists = observed.isOf(Blocks.RESPAWN_ANCHOR);
            int charges = exists ? observed.get(RespawnAnchorBlock.CHARGES) : 0;
            int required =
                    ExplosionRules.effectiveCharges(
                            module.charges.getInt(),
                            offhandGlowstoneCount());
            Step step;
            BlockHitResult hit;
            int remaining;
            if (!exists) {
                if (!available(Items.RESPAWN_ANCHOR)
                        || !available(Items.GLOWSTONE)
                        || !placeable(pos)) continue;
                hit = support(pos, module);
                step = Step.ANCHOR_PLACE;
                remaining = required + 1;
            } else if (charges < required) {
                if (!available(Items.GLOWSTONE)) continue;
                hit = face(pos, module);
                step = Step.ANCHOR_CHARGE;
                remaining = required - charges;
            } else {
                if (neutralSlot() < 0 && mc.player.getOffHandStack().isOf(Items.GLOWSTONE))
                    continue;
                hit = face(pos, module);
                step = Step.ANCHOR_BREAK;
                remaining = 0;
            }
            if (hit == null) continue;
            evaluated++;
            Vec3d source = Vec3d.ofCenter(pos);
            // A sheltered anchor keeps its real surrounding cover for the self-damage estimate.
            boolean conservativeSelf = !sheltered(pos);
            if (safe(module, source, true, anchorKey(pos, !exists), null, remaining, conservativeSelf))
                result.add(
                        new Action(
                                step,
                                module,
                                pos,
                                hit,
                                null,
                                hit.getPos(),
                                benefit(module, source, true, pos, null, remaining),
                                risk(module, source, true, pos, null, remaining, conservativeSelf),
                                score(module, source, true, 0, preferred, conservativeSelf),
                                charges + 1));
        }
    }

    private int preparation(Action action) {
        return switch (action.step) {
            case BLOCK_CLEAR -> 3;
            case CRYSTAL_BREAK, ANCHOR_BREAK -> 0;
            case OBSIDIAN_PLACE -> 2;
            case CRYSTAL_PLACE -> 1;
            case ANCHOR_PLACE ->
                    ExplosionRules.effectiveCharges(
                                    ((AnchorAura) action.module).charges.getInt(),
                                    offhandGlowstoneCount())
                            + 1;
            case ANCHOR_CHARGE ->
                    Math.max(
                            1,
                            ExplosionRules.effectiveCharges(
                                            ((AnchorAura) action.module).charges.getInt(),
                                            offhandGlowstoneCount())
                                    - (action.expectedCharge - 1));
        };
    }

    private double horizon(ExplosiveModule module, int steps) {
        var entry=mc.getNetworkHandler()==null?null:mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        int latency=entry==null?75:entry.getLatency();
        return ExplosiveSequence.horizon(module.predictionTicks.getValue(),latency,module instanceof AnchorAura,steps);
    }

    private Vec3d forecast(PlayerEntity player, double ticks, BlockPos ghost) {
        if (ticks <= 0) return Vec3d.ZERO;
        return forecasts.computeIfAbsent(
                new ForecastKey(player.getUuid(), ticks, ghost),
                key ->
                        motion.offset(
                                player.getUuid(),
                                player.getBoundingBox(),
                                ticks,
                                box ->
                                        (ghost == null || !box.intersects(new Box(ghost)))
                                                && world.isSpaceEmpty(player, box)));
    }

    private double sample(
            Vec3d source,
            PlayerEntity player,
            boolean anchor,
            BlockPos ignored,
            BlockPos ghost,
            boolean conservative,
            Vec3d offset) {
        return damageCache.computeIfAbsent(
                new DamageKey(source, player.getId(), anchor, ignored, conservative, ghost, offset),
                key ->
                        (double)
                                CrystalDamage.sample(
                                        source,
                                        player,
                                        ghost,
                                        ignored,
                                        anchor,
                                        conservative,
                                        offset));
    }

    private double damage(Vec3d source, PlayerEntity player, boolean anchor, BlockPos ignored) {
        return damage(source, player, anchor, ignored, null);
    }

    private double damage(
            Vec3d source, PlayerEntity player, boolean anchor, BlockPos ignored, BlockPos ghost) {
        return sample(source, player, anchor, ignored, ghost, false, Vec3d.ZERO);
    }

    private double selfDamage(Vec3d source, boolean anchor, BlockPos ignored) {
        return selfDamage(source, anchor, ignored, null, true);
    }

    private double selfDamage(Vec3d source, boolean anchor, BlockPos ignored, BlockPos ghost) {
        return selfDamage(source, anchor, ignored, ghost, true);
    }

    private double selfDamage(
            Vec3d source,
            boolean anchor,
            BlockPos ignored,
            BlockPos ghost,
            boolean conservativeSelf) {
        return sample(
                source,
                mc.player,
                anchor,
                anchor ? ignored : null,
                ghost,
                conservativeSelf,
                Vec3d.ZERO);
    }

    private double benefit(
            ExplosiveModule module,
            Vec3d source,
            boolean anchor,
            BlockPos ignored,
            BlockPos ghost,
            int steps) {
        double current=damage(source,victim,anchor,ignored,ghost);
        double predicted=sample(source,victim,anchor,ignored,ghost,false,
                forecast(victim,horizon(module,steps),ghost));
        // Detonate if useful now OR at packet arrival; preparation prefers the arrival forecast.
        return steps<=0 ? Math.max(current,predicted*.9) : predicted*.85+current*.15;
    }

    private double risk(
            ExplosiveModule module,
            Vec3d source,
            boolean anchor,
            BlockPos ignored,
            BlockPos ghost,
            int steps) {
        return risk(module, source, anchor, ignored, ghost, steps, true);
    }

    private double risk(
            ExplosiveModule module,
            Vec3d source,
            boolean anchor,
            BlockPos ignored,
            BlockPos ghost,
            int steps,
            boolean conservativeSelf) {
        double current = selfDamage(source, anchor, ignored, ghost, conservativeSelf);
        double predicted =
                sample(
                        source,
                        mc.player,
                        anchor,
                        ignored,
                        ghost,
                        conservativeSelf,
                        forecast(mc.player, horizon(module, steps), ghost));
        return Math.max(current, predicted);
    }

    private double score(
            ExplosiveModule module, Vec3d source, boolean anchor, int steps, boolean preferred) {
        return score(module, source, anchor, steps, preferred, true);
    }

    private double score(
            ExplosiveModule module,
            Vec3d source,
            boolean anchor,
            int steps,
            boolean preferred,
            boolean conservativeSelf) {
        BlockPos ignored = anchor ? BlockPos.ofFloored(source) : null;
        return ExplosionRules.score(
                benefit(module, source, anchor, ignored, null, steps),
                risk(module, source, anchor, ignored, null, steps, conservativeSelf),
                steps,
                preferred);
    }

    private boolean safe(ExplosiveModule module, Vec3d source, boolean anchor, Object key) {
        return safe(module, source, anchor, key, null, 0, true);
    }

    private boolean safe(
            ExplosiveModule module,
            Vec3d source,
            boolean anchor,
            Object key,
            BlockPos ghost,
            int steps) {
        return safe(module, source, anchor, key, ghost, steps, true);
    }

    private boolean safe(
            ExplosiveModule module,
            Vec3d source,
            boolean anchor,
            Object key,
            BlockPos ghost,
            int steps,
            boolean conservativeSelf) {
        BlockPos ignored = anchor ? BlockPos.ofFloored(source) : null;
        double enemy = benefit(module, source, anchor, ignored, ghost, steps),
                own = risk(module, source, anchor, ignored, ghost, steps, conservativeSelf);
        if (!budget.safe(
                enemy,
                own,
                hp(victim),
                hp(mc.player),
                module.minimum.getValue(),
                module.maximum.getValue(),
                module.reserve.getValue(),
                key)) return false;
        if (module.friends.getValue())
            for (PlayerEntity friend : world.getPlayers())
                if (FriendStore.isFriend(friend.getGameProfile().name())) {
                    double hurt =
                            Math.max(
                                    damage(source, friend, anchor, ignored, ghost),
                                    sample(
                                            source,
                                            friend,
                                            anchor,
                                            ignored,
                                            ghost,
                                            false,
                                            forecast(friend, horizon(module, steps), ghost)));
                    if (hurt > module.maximum.getValue()
                            || hp(friend) - hurt - .5 < module.reserve.getValue()) return false;
                }
        return enemy >= hp(victim) || enemy - 1.5 * own > 0;
    }

    private boolean aimed(Action a) {
        Vec3d localEye = mc.player.getEyePos();
        Vec3d serverEye = greed.client.util.player.placement.InteractionGate.serverEye();
        return aimed(a, AngleConnection.INSTANCE.getRotation(), localEye)
                && aimed(a, AngleConnection.INSTANCE.getServerAngle(), serverEye);
    }

    private boolean aimed(Action a, Angle rotation, Vec3d eye) {
        double reach = a.crystal == null
                ? Math.min(a.module.range.getValue(), mc.player.getBlockInteractionRange())
                : Math.min(((CrystalAura)a.module).attackRange.getValue(), mc.player.getEntityInteractionRange());
        Vec3d end = eye.add(Vec3d.fromPolar(rotation.getPitch(),rotation.getYaw()).multiply(reach));
        if (a.crystal != null) {
            Vec3d entry = a.crystal.getBoundingBox().contains(eye) ? eye
                    : a.crystal.getBoundingBox().raycast(eye,end).orElse(null);
            return entry != null && (a.module.walls.getValue() || visible(eye,entry,null));
        }
        BlockHitResult hit = world.getBlockState(a.hit.getBlockPos())
                .getOutlineShape(world,a.hit.getBlockPos()).raycast(eye,end,a.hit.getBlockPos());
        return hit != null && !hit.isInsideBlock() && hit.getSide()==a.hit.getSide()
                && (a.module.walls.getValue() || visible(eye,hit.getPos(),hit.getBlockPos()));
    }

    private boolean validate(Action a) {
        if (a.crystal != null)
            return a.crystal.isAlive()
                    && !a.crystal.isRemoved()
                    && mc.player
                                    .getEyePos()
                                    .squaredDistanceTo(
                                            closest(
                                                    a.crystal.getBoundingBox(),
                                                    mc.player.getEyePos()))
                            <= square(
                                    Math.min(
                                            ((CrystalAura) a.module).attackRange.getValue(),
                                            mc.player.getEntityInteractionRange()));
        if (mc.player.getEyePos().squaredDistanceTo(a.hit.getPos())
                > square(a.module.range.getValue())) return false;
        if (a.step == Step.OBSIDIAN_PLACE) {
            return ((CrystalAura) a.module).placeObsidian.getValue()
                    && placeable(a.block)
                    && a.hit.getBlockPos().offset(a.hit.getSide()).equals(a.block)
                    && world.getBlockState(a.block.up()).isAir()
                    && world.getOtherEntities(
                                    null, new Box(a.block.up()).stretch(0, 1, 0), Entity::isAlive)
                            .isEmpty();
        }
        if (a.step == Step.CRYSTAL_PLACE) {
            BlockState b = world.getBlockState(a.block);
            return (b.isOf(Blocks.OBSIDIAN) || b.isOf(Blocks.BEDROCK))
                    && world.getBlockState(a.block.up()).isAir()
                    && world.getOtherEntities(
                                    null, new Box(a.block.up()).stretch(0, 1, 0), Entity::isAlive)
                            .isEmpty();
        }
        if (world.getEnvironmentAttributes()
                .getAttributeValue(
                        EnvironmentAttributes.RESPAWN_ANCHOR_WORKS_GAMEPLAY,
                        Vec3d.ofCenter(a.block))) return false;
        BlockState s = world.getBlockState(a.block);
        if (a.step == Step.ANCHOR_PLACE)
            return !s.isOf(Blocks.RESPAWN_ANCHOR) && placeable(a.block);
        if (!s.isOf(Blocks.RESPAWN_ANCHOR)) return false;
        int amount = s.get(RespawnAnchorBlock.CHARGES),
                required =
                        ExplosionRules.effectiveCharges(
                                ((AnchorAura) a.module).charges.getInt(),
                                offhandGlowstoneCount());
        return a.step == Step.ANCHOR_CHARGE ? amount < required : amount >= required;
    }

    private boolean send(Action a) {
        return send(a, false);
    }

    private boolean send(Action a, boolean expectClear) {
        transmitted = null; createdAnchor = false; detonationKey = null; detonationRisk = 0;
        predictedAir = optimisticMissed = false;
        optimisticAirTick = -1;
        obsidianSeen = false;
        List<ExplosivePackets.Command> commands = new ArrayList<>();
        int[] spent = new int[10];
        Object key = a.explosionKey();
        // The optimistic post-mining placement is intentionally sent while this client still
        // sees the old solid block; server-side reach/placement is authoritative for it.
        boolean hidden = a.module.walls.getValue() || expectClear;
        ExplosivePackets.Command command;
        switch (a.step) {
            case ANCHOR_PLACE -> {
                command = use(Items.RESPAWN_ANCHOR, a.hit, a.block, hidden, spent);
                createdAnchor = true;
            }
            case ANCHOR_CHARGE -> {
                command = use(Items.GLOWSTONE, a.hit, null, hidden, spent);
                // Wait for this charge before choosing the next stage.
                int expected = world.getBlockState(a.block).get(RespawnAnchorBlock.CHARGES) + 1;
                transmitted = new Action(a.step, a.module, a.block, a.hit, null, a.point,
                        a.enemy, a.self, a.priority, expected);
            }
            case ANCHOR_BREAK -> {
                int slot = neutralSlot();
                Hand hand = slot >= 0 ? Hand.MAIN_HAND : Hand.OFF_HAND;
                if (slot < 0 && mc.player.getOffHandStack().isOf(Items.GLOWSTONE)) return false;
                command = new ExplosivePackets.Command(hand,
                        slot >= 0 ? slot : mc.player.getInventory().getSelectedSlot(),
                        a.hit, null, null, hidden);
            }
            case CRYSTAL_BREAK -> {
                int slot = crystalAttackSlot();
                if (slot < 0) return false;
                command = new ExplosivePackets.Command(Hand.MAIN_HAND, slot, null, null,
                        a.crystal, hidden, a.point);
            }
            case OBSIDIAN_PLACE, CRYSTAL_PLACE -> command = use(
                    a.step == Step.OBSIDIAN_PLACE ? Items.OBSIDIAN : Items.END_CRYSTAL,
                    a.hit, a.step == Step.OBSIDIAN_PLACE ? a.block : null, hidden, spent);
            default -> { return false; }
        }
        if (command == null) return false;
        if (expectClear) command = command.clearing();
        commands.add(command);
        if (!packets.execute(commands)) return false;
        if (createdAnchor) {
            confirmedAnchors.putIfAbsent(a.block,world.getBlockState(a.block));
            anchorGenerations.merge(a.block.toImmutable(),1,Integer::sum);
        }
        replacedCrystalId = a.crystal != null && transmitted != null ? a.crystal.getId() : -1;
        Action finalAction = transmitted == null ? a : transmitted;
        if (a.explosive() || finalAction.explosive()) {
            detonationKey = key;
            detonationRisk = risk(a.module,a.pointForBlast(),finalAction.step==Step.ANCHOR_BREAK,
                    finalAction.step==Step.ANCHOR_BREAK ? a.block : null,null,0,
                    !(finalAction.step==Step.ANCHOR_BREAK && sheltered(a.block)));
        }
        return true;
    }

    private int offhandGlowstoneCount() {
        return mc.player.getOffHandStack().isOf(Items.GLOWSTONE) ? mc.player.getOffHandStack().getCount() : 0;
    }

    private ExplosivePackets.Command use(Item item, BlockHitResult hit, BlockPos creates, boolean hidden,int[] spent) {
        int original=mc.player.getInventory().getSelectedSlot();
        int[] counts=new int[10];
        for(int i=0;i<9;i++) {
            ItemStack stack=mc.player.getInventory().getStack(i);
            counts[i]=stack.isOf(item)?packets.availableCount(i,stack):0;
        }
        counts[9]=mc.player.getOffHandStack().isOf(item)?packets.availableCount(9,mc.player.getOffHandStack()):0;
        int selected=ExplosiveSequence.takeSlot(original,counts,spent);
        return selected<0?null:new ExplosivePackets.Command(selected==9?Hand.OFF_HAND:Hand.MAIN_HAND,
                selected==9?original:selected,hit,creates,null,hidden);
    }

    private BlockHitResult futureHit(BlockPos block,ExplosiveModule module) {
        double reach=module.range.getValue();
        Vec3d eye=mc.player.getEyePos(); BlockHitResult best=null; double cost=Double.POSITIVE_INFINITY;
        for (Direction side : Direction.values()) {
            Vec3d point = PlacementGeometry.facePoint(new Box(block),side,eye);
            if (eye.squaredDistanceTo(point)>square(Math.min(reach,mc.player.getBlockInteractionRange()))
                    || !module.walls.getValue()&&!visible(eye,point,block)) continue;
            var hit=VoxelShapes.fullCube().raycast(eye,point.add(point.subtract(eye).normalize().multiply(.001)),block);
            if (hit!=null&&!hit.isInsideBlock()&&hit.getSide()==side&&eye.squaredDistanceTo(point)<cost) {
                best=new BlockHitResult(point,side,block,false);cost=eye.squaredDistanceTo(point);
            }
        }
        return best;
    }

    private int find(Predicate<ItemStack> predicate) {
        int current=mc.player.getInventory().getSelectedSlot();
        if (predicate.test(mc.player.getInventory().getStack(current))) return current;
        for (int i = 0; i < 9; i++)
            if (predicate.test(mc.player.getInventory().getStack(i))) return i;
        return -1;
    }

    private boolean available(Item item) {
        return mc.player.getOffHandStack().isOf(item) || find(s -> s.isOf(item)) >= 0;
    }

    private int neutralSlot() {
        int selected = mc.player.getInventory().getSelectedSlot();
        if (!mc.player.getMainHandStack().isOf(Items.GLOWSTONE)) return selected;
        return find(s -> !s.isOf(Items.GLOWSTONE));
    }

    private int crystalAttackSlot() {
        int selected = mc.player.getInventory().getSelectedSlot();
        if (!mc.player.hasStatusEffect(StatusEffects.WEAKNESS)
                || attackDamage(mc.player.getMainHandStack()) > 0) return selected;
        int best = -1;
        double damage = 0;
        for (int i = 0; i < 9; i++) {
            double value = attackDamage(mc.player.getInventory().getStack(i));
            if (value > damage) {
                damage = value;
                best = i;
            }
        }
        return best;
    }

    private Vec3d crystalAim(Box box, CrystalAura module, double reach) {
        Angle angle = AngleConnection.INSTANCE.getServerAngle();
        return CrystalAimPoints.find(
                box,
                mc.player.getEyePos(),
                Vec3d.fromPolar(angle.getPitch(), angle.getYaw()),
                reach,
                point -> module.walls.getValue() || visible(point, null));
    }

    private double attackDamage(ItemStack s) {
        double damage =
                s.getOrDefault(
                                DataComponentTypes.ATTRIBUTE_MODIFIERS,
                                AttributeModifiersComponent.DEFAULT)
                        .applyOperations(EntityAttributes.ATTACK_DAMAGE, 1, EquipmentSlot.MAINHAND);
        var w = mc.player.getStatusEffect(StatusEffects.WEAKNESS);
        var b = mc.player.getStatusEffect(StatusEffects.STRENGTH);
        return damage
                - (w == null ? 0 : 4 * (w.getAmplifier() + 1))
                + (b == null ? 0 : 3 * (b.getAmplifier() + 1));
    }

    private boolean placeable(BlockPos p) {
        return world.isInBuildLimit(p)
                && world.getWorldBorder().contains(p)
                && world.getBlockState(p).isReplaceable()
                && world.getOtherEntities(null, new Box(p), e -> e.isAlive() && !e.isSpectator())
                        .isEmpty();
    }

    private BlockHitResult support(BlockPos target, ExplosiveModule module) {
        return placement.placementHit(target, module.range.getValue(), module.walls.getValue());
    }

    private BlockHitResult face(BlockPos block, ExplosiveModule module) {
        return placement.interactionHit(block, module.range.getValue(), module.walls.getValue());
    }

    private boolean visible(Vec3d p, BlockPos target) {
        return visible(mc.player.getEyePos(), p, target);
    }

    private boolean visible(Vec3d eye, Vec3d p, BlockPos target) {
        BlockHitResult hit =
                world.raycast(
                        new RaycastContext(
                                eye,
                                p,
                                RaycastContext.ShapeType.OUTLINE,
                                RaycastContext.FluidHandling.NONE,
                                mc.player));
        return hit.getType() == HitResult.Type.MISS
                || target != null && hit.getBlockPos().equals(target);
    }

    private int pingTicks() {
        var p =
                mc.getNetworkHandler() == null
                        ? null
                        : mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        return p == null ? 0 : MathHelper.clamp((p.getLatency() + 49) / 50, 0, 20);
    }

    private int timeout() {
        return MathHelper.clamp(pingTicks() + 3, 4, 24);
    }

    private static double hp(PlayerEntity p) {
        return p == null ? 0 : p.getHealth() + p.getAbsorptionAmount();
    }

    private static double square(double v) {
        return v * v;
    }

    private static Vec3d closest(Box b, Vec3d p) {
        return new Vec3d(
                MathHelper.clamp(p.x, b.minX, b.maxX),
                MathHelper.clamp(p.y, b.minY, b.maxY),
                MathHelper.clamp(p.z, b.minZ, b.maxZ));
    }

    private static Vec3d inside(Box b, Vec3d p) {
        return closest(b.contract(.05), p);
    }

    private void abandonPending() {
        pending = null;
        waitingCreatedAnchor = false;
        predictedAir = optimisticMissed = false;
        optimisticAirTick = -1;
        replacedCrystalId = -1;
        observedSpawnId = -1;
        placementGone = anchorSeen = obsidianSeen = false;
        // Real explosion attempts retain their health-budget debit until observation/expiry.
    }

    private void confirmed() {
        if (pending == null) return;
        retries.remove(pending.retryKey());
        pending = null;
        waitingCreatedAnchor = false;
        predictedAir = optimisticMissed = false;
        optimisticAirTick = -1;
        replacedCrystalId = -1;
        observedSpawnId = -1;
        placementGone = anchorSeen = obsidianSeen = false;
    }

    private void blockUpdate(BlockPos pos, BlockState state) {
        if (pending != null
                && pending.block.equals(pos)
                && (pending.step.name().startsWith("ANCHOR")
                        || pending.step == Step.OBSIDIAN_PLACE)) {
            if (pending.step == Step.OBSIDIAN_PLACE) {
                if (state.isOf(Blocks.OBSIDIAN)) {
                    obsidianSeen = true;
                    predictedAir = false;
                } else if (predictedAir) {
                    optimisticMissed = true;
                    if (optimisticAirTick < 0) optimisticAirTick = mc.player.age;
                } else if (obsidianSeen) {
                    placementGone = true;
                }
            } else if (state.isOf(Blocks.RESPAWN_ANCHOR)) {
                anchorSeen = true;
                predictedAir = false;
            } else if (predictedAir) {
                // The break packet produced air, but the optimistic placement may be right
                // behind it in the same network batch. Remember when the clearing arrived and
                // only retry after giving the placement acknowledgement time to be applied.
                optimisticMissed = true;
                if (optimisticAirTick < 0) optimisticAirTick = mc.player.age;
            } else if (anchorSeen || waitingCreatedAnchor) {
                placementGone = true;
            }
        }
        BlockState previous = confirmedAnchors.get(pos);
        if (state.isOf(Blocks.RESPAWN_ANCHOR)
                && previous != null
                && !previous.isOf(Blocks.RESPAWN_ANCHOR) && !waitingCreatedAnchor)
            anchorGenerations.merge(pos.toImmutable(), 1, Integer::sum);
        if (confirmedAnchors.containsKey(pos) || pending != null && pending.block.equals(pos))
            confirmedAnchors.put(pos.toImmutable(), state);
    }

    /** Our placement path never predicts blocks locally: real world state is a valid fallback. */
    private void reconcile() {
        if (pending == null) return;
        Action action = pending;
        BlockState block = world.getBlockState(action.block);
        boolean optimisticPlace =
                action.step == Step.ANCHOR_PLACE || action.step == Step.OBSIDIAN_PLACE;
        boolean placeSeen = action.step == Step.ANCHOR_PLACE ? anchorSeen : obsidianSeen;
        if (optimisticPlace
                && predictedAir && optimisticMissed && !placeSeen
                && optimisticAirTick >= 0 && mc.player.age - optimisticAirTick >= 2) {
            // The break acknowledgement arrives before the placement acknowledgement, so the
            // first air update is not a miss by itself. Only retry after two ticks of silence.
            abandonPending();
            state = "Place retry";
            return;
        }
        if (placementGone) {
            confirmed();
            return;
        }
        if (action.step == Step.ANCHOR_CHARGE && !block.isOf(Blocks.RESPAWN_ANCHOR)) {
            confirmed();
            return;
        }
        if (waitingCreatedAnchor && !anchorSeen) return;
        EndCrystalEntity crystal = action.crystal;
        if (action.step == Step.CRYSTAL_PLACE) {
            Vec3d source = action.pointForBlast();
            crystal =
                    world
                            .getEntitiesByClass(
                                    EndCrystalEntity.class,
                                    new Box(source, source).expand(.7),
                                    c ->
                                            c.isAlive()
                                                    && c.getId() != replacedCrystalId
                                                    && !c.isRemoved()
                                                    && c.getEntityPos().squaredDistanceTo(source)
                                                            < .3)
                            .stream()
                            .findFirst()
                            .orElse(null);
            if (crystal != null) urgentId = crystal.getId();
        }
        boolean present =
                crystal != null
                        && crystal.isAlive()
                        && !crystal.isRemoved()
                        && world.getEntityById(crystal.getId()) == crystal;
        boolean anchor = block.isOf(Blocks.RESPAWN_ANCHOR);
        var observation =
                new ExplosiveCycle.Observation(
                        present,
                        block.isOf(Blocks.OBSIDIAN),
                        anchor,
                        anchor ? block.get(RespawnAnchorBlock.CHARGES) : 0);
        if (ExplosiveCycle.confirmed(action.step, action.expectedCharge, observation)) {
            if (action.step.name().startsWith("ANCHOR")) blockUpdate(action.block, block);
            confirmed();
        }
    }

    private void drain() {
        Envelope envelope;
        while ((envelope = inbox.poll()) != null) {
            if (envelope.world != world) continue;
            Packet<?> packet = envelope.packet;
            if (packet instanceof BlockUpdateS2CPacket block)
                blockUpdate(block.getPos(), block.getState());
            else if (packet instanceof ChunkDeltaUpdateS2CPacket chunk)
                chunk.visitUpdates(this::blockUpdate);
            else if (packet instanceof EntitySpawnS2CPacket spawn
                    && spawn.getEntityType() == EntityType.END_CRYSTAL
                    && pending != null
                    && pending.step == Step.CRYSTAL_PLACE
                    && new Vec3d(spawn.getX(), spawn.getY(), spawn.getZ())
                                    .squaredDistanceTo(pending.pointForBlast())
                            < .3) {
                urgentId = spawn.getEntityId();
                observedSpawnId = spawn.getEntityId();
            } else if (packet instanceof EntitiesDestroyS2CPacket removed) {
                if (observedSpawnId >= 0 && removed.getEntityIds().contains(observedSpawnId))
                    placementGone = true;
                if (removed.getEntityIds().contains(urgentId)) urgentId = -1;
            }
            // Nearby explosion sounds/particles alone do not confirm removal of this entity.
        }
        confirmedAnchors
                .entrySet()
                .removeIf(
                        entry -> mc.player.squaredDistanceTo(Vec3d.ofCenter(entry.getKey())) > 144);
    }
}
