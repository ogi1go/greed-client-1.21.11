package greed.client.module.misc;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import greed.client.event.EventListener;
import greed.client.event.PlayerTickEvent;
import greed.client.event.WorldRenderEvent;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.setting.BooleanSetting;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

@ModuleInfo(name = "SusChunks", description = "Подсвечивает чанки в которых могут быть зомби", category = ModuleCategory.MISC)
public class SusChunks extends Module {
    private static SusChunks instance;

    private static final int SCAN_INTERVAL_TICKS = 5;
    private static final int CHUNKS_PER_SCAN = 3;
    private static final int RESCAN_TICKS = 600;
    private static final int OBSIDIAN_THRESHOLD = 12;
    private static final int STORAGE_THRESHOLD = 6;
    private static final int BASE_SCORE_THRESHOLD = 10;
    private static final int LAYER_OFFSET = 7;
    private static final int FILL_ALPHA = 0x66;

    private static final RenderPipeline FILL_PIPELINE = RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
            .withLocation(Identifier.of("greed", "pipeline/sus_chunks_fill"))
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .build();

    private static final RenderLayer FILL_LAYER = RenderLayer.of(
            "greed_sus_chunks_fill",
            RenderSetup.builder(FILL_PIPELINE).translucent().build()
    );

    private final BooleanSetting spawners = this.addSetting(
            new BooleanSetting("Спавнеры", "Подсвечивать чанки со спавнерами.", true)
    );

    private final BooleanSetting bases = this.addSetting(
            new BooleanSetting("Базы", "Искать чанки с обсидианом, сундуками и странными блоками.", true)
    );

    private static final Predicate<BlockState> OBSIDIAN = s -> s.isOf(Blocks.OBSIDIAN);
    private static final Predicate<BlockState> SUS = s -> isSusBlock(s.getBlock());

    private final Map<ChunkPos, Result> results = new HashMap<>();
    private final Map<ChunkPos, Integer> scannedAt = new HashMap<>();

    private Object lastWorld;
    private int tickCounter;

    private static final class Result {
        boolean spawner;
        boolean zombie;
        int obsidian;
        int storage;
        int sus;

        int baseScore() {
            return this.obsidian / 2 + this.storage * 2 + this.sus * 3;
        }
    }

    private final EventListener<PlayerTickEvent> tickListener = event -> {
        if (mc.player == null || mc.world == null) {
            return;
        }

        if (this.lastWorld != mc.world) {
            this.lastWorld = mc.world;
            this.results.clear();
            this.scannedAt.clear();
        }

        this.tickCounter++;
        if (this.tickCounter % SCAN_INTERVAL_TICKS != 0) {
            return;
        }

        int radius = mc.options.getViewDistance().getValue();
        int cx = mc.player.getChunkPos().x;
        int cz = mc.player.getChunkPos().z;
        int budget = CHUNKS_PER_SCAN;

        for (int r = 0; r <= radius && budget > 0; r++) {
            for (int dx = -r; dx <= r && budget > 0; dx++) {
                for (int dz = -r; dz <= r && budget > 0; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }

                    ChunkPos cp = new ChunkPos(cx + dx, cz + dz);
                    Integer last = this.scannedAt.get(cp);
                    if (last != null && this.tickCounter - last < RESCAN_TICKS) {
                        continue;
                    }

                    WorldChunk chunk = mc.world.getChunkManager().getWorldChunk(cp.x, cp.z);
                    if (chunk == null) {
                        continue;
                    }

                    this.scanChunk(chunk);
                    this.scannedAt.put(cp, this.tickCounter);
                    budget--;
                }
            }
        }
    };

    private final EventListener<WorldRenderEvent> renderListener = event -> {
        if (mc.player == null || mc.world == null || this.results.isEmpty()) {
            return;
        }

        MatrixStack matrices = event.getMatrixStack();
        VertexConsumerProvider.Immediate provider = mc.getBufferBuilders().getEntityVertexConsumers();
        VertexConsumer fill = provider.getBuffer(FILL_LAYER);
        Vec3d cam = mc.gameRenderer.getCamera().getCameraPos();

        float layerY = (float) (Math.floor(mc.player.getY()) - LAYER_OFFSET);

        matrices.push();
        matrices.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        for (Map.Entry<ChunkPos, Result> entry : this.results.entrySet()) {
            ChunkPos cp = entry.getKey();
            Result res = entry.getValue();

            boolean showSpawner = this.spawners.isChecked() && res.spawner;
            boolean showBase = this.bases.isChecked()
                    && res.baseScore() >= BASE_SCORE_THRESHOLD
                    && (res.obsidian >= OBSIDIAN_THRESHOLD || res.storage >= STORAGE_THRESHOLD || res.sus > 0);

            if (!showSpawner && !showBase) {
                continue;
            }

            int rgb;
            if (showSpawner) {
                rgb = res.zombie ? 0x1AFF33 : 0xFF2A2A;
            } else {
                rgb = 0xB333FF;
            }
            int color = (FILL_ALPHA << 24) | rgb;

            float x1 = cp.getStartX();
            float z1 = cp.getStartZ();
            float x2 = x1 + 16.0f;
            float z2 = z1 + 16.0f;

            fill.vertex(matrix, x1, layerY, z1).color(color);
            fill.vertex(matrix, x1, layerY, z2).color(color);
            fill.vertex(matrix, x2, layerY, z2).color(color);
            fill.vertex(matrix, x2, layerY, z1).color(color);

            fill.vertex(matrix, x1, layerY, z1).color(color);
            fill.vertex(matrix, x2, layerY, z1).color(color);
            fill.vertex(matrix, x2, layerY, z2).color(color);
            fill.vertex(matrix, x1, layerY, z2).color(color);
        }

        matrices.pop();
        provider.draw(FILL_LAYER);
    };

    private void scanChunk(WorldChunk chunk) {
        Result res = new Result();

        for (BlockEntity be : chunk.getBlockEntities().values()) {
            if (be instanceof MobSpawnerBlockEntity spawner) {
                res.spawner = true;
                if (this.isZombieSpawner(spawner)) {
                    res.zombie = true;
                }
            } else if (be instanceof ChestBlockEntity
                    || be instanceof BarrelBlockEntity
                    || be instanceof ShulkerBoxBlockEntity
                    || be instanceof EnderChestBlockEntity
                    || be instanceof HopperBlockEntity
                    || be instanceof AbstractFurnaceBlockEntity) {
                res.storage++;
            }
        }

        if (this.bases.isChecked()) {
            ChunkSection[] sections = chunk.getSectionArray();

            for (ChunkSection section : sections) {
                if (section == null || section.isEmpty()) {
                    continue;
                }

                if (!section.hasAny(OBSIDIAN) && !section.hasAny(SUS)) {
                    continue;
                }

                for (int x = 0; x < 16; x++) {
                    for (int y = 0; y < 16; y++) {
                        for (int z = 0; z < 16; z++) {
                            Block block = section.getBlockState(x, y, z).getBlock();
                            if (block == Blocks.OBSIDIAN) {
                                res.obsidian++;
                            } else if (isSusBlock(block)) {
                                res.sus++;
                            }
                        }
                    }
                }
            }
        }

        if (!res.spawner && res.baseScore() < BASE_SCORE_THRESHOLD) {
            this.results.remove(chunk.getPos());
        } else {
            this.results.put(chunk.getPos(), res);
        }
    }

    private boolean isZombieSpawner(MobSpawnerBlockEntity spawner) {
        try {
            NbtCompound nbt = spawner.createNbt(mc.world.getRegistryManager());
            String id = nbt.getCompound("SpawnData")
                    .flatMap(d -> d.getCompound("entity"))
                    .flatMap(e -> e.getString("id"))
                    .orElse("");
            return id.contains("zombie");
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isSusBlock(Block block) {
        return block == Blocks.CRYING_OBSIDIAN
                || block == Blocks.RESPAWN_ANCHOR
                || block == Blocks.BEACON
                || block == Blocks.ENCHANTING_TABLE
                || block == Blocks.ANVIL
                || block == Blocks.CHIPPED_ANVIL
                || block == Blocks.DAMAGED_ANVIL
                || block == Blocks.NETHERITE_BLOCK
                || block == Blocks.DIAMOND_BLOCK
                || block == Blocks.EMERALD_BLOCK
                || block == Blocks.TNT
                || block == Blocks.CRAFTING_TABLE
                || block == Blocks.BREWING_STAND
                || block == Blocks.CONDUIT;
    }

    @Override
    protected void onEnable() {
        this.results.clear();
        this.scannedAt.clear();
        this.lastWorld = null;
                this.tickCounter = 0;
    }

    @Override
    protected void onDisable() {
        this.results.clear();
        this.scannedAt.clear();
    }

    private SusChunks() {
    }

    public static SusChunks getInstance() {
        if (instance == null) {
            instance = new SusChunks();
        }
        return instance;
    }
}