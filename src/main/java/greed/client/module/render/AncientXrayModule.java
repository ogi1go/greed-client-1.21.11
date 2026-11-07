package greed.client.module.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import greed.client.GreedClient;
import greed.client.event.EventListener;
import greed.client.event.PacketEvent;
import greed.client.event.PlayerTickEvent;
import greed.client.event.WorldRenderEvent;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.util.ClientChat;
import greed.client.util.Colors;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.BufferAllocator.CloseableBuffer;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ModuleInfo(name = "AncientXray", description = "Подсвечивает все обломки после взрыва", category = ModuleCategory.RENDER)
public final class AncientXrayModule extends Module {
    public static AncientXrayModule instance;
    private final Set<BlockPos> set = ConcurrentHashMap.newKeySet();
    private final Set<BlockPos> set2 = ConcurrentHashMap.newKeySet();
    private final List<DelayBlock> list = new ArrayList<>();
    private static final int[] DELAYS = {4, 10, 20, 40};
    private long lng = 0L;

    private static final RenderPipeline pipeline = RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
            .withLocation(GreedClient.id("pipeline/ancient_xray"))
            .withVertexShader(Identifier.ofVanilla("core/position_color"))
            .withFragmentShader(Identifier.ofVanilla("core/position_color"))
            .withVertexFormat(VertexFormats.POSITION_COLOR, DrawMode.QUADS)
            .withBlend(BlendFunction.LIGHTNING)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .build();

    private static final Vector4f vector4f = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
    private static final Vector3f vector3f = new Vector3f();
    private static final Matrix4f matrix4f = new Matrix4f();
    private static final int[][] intValArray = new int[][]{{0, 1, 5, 4}, {2, 3, 7, 6}, {0, 1, 3, 2}, {4, 5, 7, 6}, {0, 2, 6, 4}, {1, 3, 7, 5}};
    private static final float[] floatValArray = new float[]{0.45F, 1.0F, 0.7F, 0.7F, 0.55F, 0.55F};

    private GpuBuffer gpuBuffer;
    private BufferAllocator bufferAllocator;

    private static class DelayBlock {
        private final BlockPos pos;
        private int ticks;

        private DelayBlock(BlockPos pos, int ticks) {
            this.pos = pos;
            this.ticks = ticks;
        }
    }

    private final EventListener<PlayerTickEvent> playerTickEventEventListener = playerTickEvent -> {
        if (mc.player == null || mc.world == null)
            return;

        Iterator<DelayBlock> iterator = this.list.iterator();

        while (iterator.hasNext()) {
            DelayBlock delayBlock = iterator.next();
            delayBlock.ticks--;

            if (delayBlock.ticks > 0)
                continue;

            this.checkBlocks(delayBlock.pos, 28);
            iterator.remove();
        }

        if (System.currentTimeMillis() - this.lng > 50L) {
            for (BlockPos blockPos : this.set) {
                if (this.set2.contains(blockPos))
                    continue;

                this.set2.add(blockPos);
                this.startStopDestroy(blockPos);
                this.lng = System.currentTimeMillis();
                break;
            }
        }
    };

    private final EventListener<WorldRenderEvent> worldRenderListener = worldRenderEvent -> {
        if (mc.player != null && mc.world != null && !this.set.isEmpty()) {
            if (mc.getFramebuffer() != null && mc.getFramebuffer().getColorAttachmentView() != null) {
                float cameraX = (float) mc.gameRenderer.getCamera().getCameraPos().x;
                float cameraY = (float) mc.gameRenderer.getCamera().getCameraPos().y;
                float cameraZ = (float) mc.gameRenderer.getCamera().getCameraPos().z;
                int indexCount = 0;
                float shrink = 0.04F;

                if (this.bufferAllocator == null) {
                    this.bufferAllocator = new BufferAllocator(12288 * VertexFormats.POSITION_COLOR.getVertexSize());
                }

                CloseableBuffer allocated = this.bufferAllocator.getAllocated();
                if (allocated != null) {
                    allocated.close();
                }

                BufferBuilder bufferBuilder = new BufferBuilder(this.bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION_COLOR);

                Iterator<BlockPos> iterator = this.set.iterator();
                while (iterator.hasNext()) {
                    BlockPos blockPos = iterator.next();

                    if (!mc.world.getBlockState(blockPos).isOf(Blocks.ANCIENT_DEBRIS)) {
                        iterator.remove();
                        this.set2.remove(blockPos);
                        continue;
                    }

                    float minX = blockPos.getX() - cameraX + shrink;
                    float minY = blockPos.getY() - cameraY + shrink;
                    float minZ = blockPos.getZ() - cameraZ + shrink;
                    float maxX = minX + 1.0F - shrink * 2.0F;
                    float maxY = minY + 1.0F - shrink * 2.0F;
                    float maxZ = minZ + 1.0F - shrink * 2.0F;

                    int baseColor = 0xFF5D3A1A;
                    int rawColor = Float.floatToRawIntBits(baseColor);

                    for (int index3 = 0; index3 < 6; index3++) {
                        int finalColor = Colors.computeInt6(
                                Colors.computeInt2(-16777216, rawColor, floatValArray[index3]), 0.5f
                        );

                        for (int v : intValArray[index3]) {
                            bufferBuilder.vertex(
                                    (v & 4) == 0 ? minX : maxX,
                                    (v & 2) == 0 ? minY : maxY,
                                    (v & 1) == 0 ? minZ : maxZ
                            ).color(finalColor);
                        }
                        indexCount++;
                    }
                }

                try (BuiltBuffer builtBuffer = bufferBuilder.end()) {
                    if (this.gpuBuffer != null && !this.gpuBuffer.isClosed() && this.gpuBuffer.size() >= builtBuffer.getBuffer().remaining()) {
                        RenderSystem.getDevice()
                                .createCommandEncoder()
                                .writeToBuffer(this.gpuBuffer.slice(0L, builtBuffer.getBuffer().remaining()), builtBuffer.getBuffer());
                    } else {
                        if (this.gpuBuffer != null && !this.gpuBuffer.isClosed()) {
                            this.gpuBuffer.close();
                        }
                        this.gpuBuffer = RenderSystem.getDevice().createBuffer(() -> "greed_ancient_xray_vertices", 40, builtBuffer.getBuffer());
                    }

                    GpuBufferSlice gpuBufferSlice = RenderSystem.getDynamicUniforms()
                            .write(worldRenderEvent.getMatrixStack().peek().getPositionMatrix(), vector4f, vector3f, matrix4f);

                    try (RenderPass renderPass = RenderSystem.getDevice()
                            .createCommandEncoder()
                            .createRenderPass(
                                    () -> "greed_ancient_xray", mc.getFramebuffer().getColorAttachmentView(), OptionalInt.empty(), null, OptionalDouble.empty()
                            )) {
                        renderPass.setPipeline(pipeline);
                        renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
                        renderPass.setUniform("Projection", RenderSystem.getProjectionMatrixBuffer());
                        renderPass.setVertexBuffer(0, this.gpuBuffer);
                        renderPass.setIndexBuffer(
                                RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexBuffer(indexCount * 6),
                                RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexType()
                        );
                        renderPass.drawIndexed(0, 0, indexCount * 6, 1);
                    }
                }
            }
        }
    };

    private void startStopDestroy(BlockPos blockPos) {
        if (mc.getNetworkHandler() == null)
            return;

        mc.getNetworkHandler().sendPacket(
                new PlayerActionC2SPacket(
                        PlayerActionC2SPacket.Action.START_DESTROY_BLOCK,
                        blockPos,
                        Direction.UP
                )
        );

        mc.getNetworkHandler().sendPacket(
                new PlayerActionC2SPacket(
                        PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK,
                        blockPos,
                        Direction.UP
                )
        );
    }

    private void handleBlockUpdate(BlockPos blockPos, Block block) {
        BlockPos blockPos2 = blockPos.toImmutable();

        if (block == Blocks.ANCIENT_DEBRIS) {
            if (this.isValidDebris(blockPos2) && this.set.add(blockPos2)) {
                ClientChat.sendBaritoneMessage(
                        "§6[AncientXray] §fОбломок найден §e"
                                + blockPos2.toShortString()
                );
            }
        } else {
            this.set.remove(blockPos2);
            this.set2.remove(blockPos2);
        }
    }

    private void checkBlocks(BlockPos blockPos, int n) {
        if (mc.world == null)
            return;

        BlockPos.Mutable mutable = new BlockPos.Mutable();

        for (int i = -n; i <= n; ++i) {
            for (int j = -n; j <= n; ++j) {
                for (int k = -n; k <= n; ++k) {
                    mutable.set(
                            blockPos.getX() + i,
                            blockPos.getY() + j,
                            blockPos.getZ() + k
                    );

                    if (!this.isValidDebris(mutable))
                        continue;

                    BlockPos blockPos2 = mutable.toImmutable();

                    if (!this.set.add(blockPos2))
                        continue;

                    ClientChat.sendBaritoneMessage(
                            "§6[AncientXray] §fОбнаружен обломок: §e"
                                    + blockPos2.toShortString()
                    );
                }
            }
        }
    }

    private final EventListener<PacketEvent> onPacketReceive = event -> {
        if (event.getPacket() instanceof PlayerActionC2SPacket) {}

        if (mc.world == null ||
                event.getDirection() == PacketEvent.Direction.SENT)
            return;

        Packet<?> packet = event.getPacket();

        if (packet instanceof ExplosionS2CPacket explosionS2CPacket) {
            BlockPos pos =
                    BlockPos.ofFloored(
                            explosionS2CPacket.center()
                    );

            for (int delay : DELAYS) {
                this.list.add(
                        new DelayBlock(
                                pos.toImmutable(),
                                delay
                        )
                );
            }
        } else if (packet instanceof BlockUpdateS2CPacket blockUpdateS2CPacket) {
            this.handleBlockUpdate(
                    blockUpdateS2CPacket.getPos(),
                    blockUpdateS2CPacket.getState().getBlock()
            );
        } else if (packet instanceof ChunkDeltaUpdateS2CPacket chunkDeltaUpdateS2CPacket) {
            chunkDeltaUpdateS2CPacket.visitUpdates(
                    (blockPos, blockState) ->
                            this.handleBlockUpdate(
                                    blockPos,
                                    blockState.getBlock()
                            )
            );
        }
    };

    public List<BlockPos> getFoundBlocks() {
        synchronized (this.set) {
            return new ArrayList<>(this.set);
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.set.clear();
        this.set2.clear();
        this.list.clear();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.set.clear();
        this.set2.clear();
        this.list.clear();

        if (this.bufferAllocator != null) {
            this.bufferAllocator.close();
            this.bufferAllocator = null;
        }

        if (this.gpuBuffer != null && !this.gpuBuffer.isClosed()) {
            this.gpuBuffer.close();
        }
        this.gpuBuffer = null;
    }

    private boolean checkDebrisNearby(BlockPos blockPos) {
        int n = 0;

        for (int i = -3; i <= 2; ++i) {
            for (int j = -2; j <= 2; ++j) {
                for (int k = -2; k <= 3; ++k) {
                    if (mc.world.getBlockState(
                            blockPos.add(i, j, k)
                    ).getBlock() != Blocks.ANCIENT_DEBRIS ||
                            ++n <= 6)
                        continue;

                    return true;
                }
            }
        }

        return false;
    }

    private boolean checkAirNearby(BlockPos blockPos) {
        int n = 0;

        for (Direction direction : Direction.values()) {
            Block block = mc.world.getBlockState(
                    blockPos.offset(direction)
            ).getBlock();

            if (block != Blocks.AIR &&
                    block != Blocks.LAVA &&
                    block != Blocks.CAVE_AIR ||
                    ++n < 2)
                continue;

            return true;
        }

        return false;
    }

    private boolean checkQuartzOrGold(BlockPos blockPos) {
        int n = 0;

        for (int i = -1; i <= 1; ++i) {
            for (int j = -1; j <= 1; ++j) {
                for (int k = -1; k <= 1; ++k) {
                    Block block = mc.world.getBlockState(
                            blockPos.add(i, j, k)
                    ).getBlock();

                    if (block != Blocks.NETHER_QUARTZ_ORE &&
                            block != Blocks.NETHER_GOLD_ORE ||
                            ++n < 4)
                        continue;

                    return true;
                }
            }
        }

        return false;
    }

    public static AncientXrayModule getInstance() {
        if (instance == null) {
            instance=new AncientXrayModule();
        }
        return instance;
    }

    private boolean checkAirLavaNearbyBig(BlockPos blockPos) {
        int n = 0;

        for (int i = -1; i <= 1; ++i) {
            for (int j = -1; j <= 1; ++j) {
                for (int k = -1; k <= 1; ++k) {
                    Block block = mc.world.getBlockState(
                            blockPos.add(i, j, k)
                    ).getBlock();

                    if (block != Blocks.AIR &&
                            block != Blocks.LAVA &&
                            block != Blocks.CAVE_AIR ||
                            ++n < 4)
                        continue;

                    return true;
                }
            }
        }

        return n >= 4;
    }

    private boolean isValidDebris(BlockPos blockPos) {
        if (mc.world == null)
            return false;

        Block block = mc.world
                .getBlockState(blockPos)
                .getBlock();

        if (block != Blocks.ANCIENT_DEBRIS)
            return false;

        if (!this.checkAirNearby(blockPos))
            return false;

        if (this.checkQuartzOrGold(blockPos))
            return false;

        if (!this.checkAirLavaNearbyBig(blockPos))
            return false;

        if (this.checkDebrisNearby(blockPos))
            return false;

        return true;
    }
}