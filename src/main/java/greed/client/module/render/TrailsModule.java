package greed.client.module.render;

import greed.client.GreedClient;
import greed.client.event.EventListener;
import greed.client.event.WorldRenderEvent;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.setting.ColorSetting;
import greed.client.setting.NumberSetting;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.BufferAllocator.CloseableBuffer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

@ModuleInfo(name = "Trails", description = "Просто ходи", category = ModuleCategory.RENDER)
public final class TrailsModule extends Module {
    private static TrailsModule instance;
    private static final Vector4f vector4f = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
    private static final Vector3f vector3f = new Vector3f();
    private static final Matrix4f matrix4f2 = new Matrix4f();
    private static final Matrix4f matrix4f3 = new Matrix4f();

    private static final RenderPipeline TRAIL_PIPELINE = RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
            .withLocation(GreedClient.id("pipeline/trails"))
            .withVertexShader(Identifier.ofVanilla("core/position_color"))
            .withFragmentShader(Identifier.ofVanilla("core/position_color"))
            .withVertexFormat(VertexFormats.POSITION_COLOR, DrawMode.TRIANGLE_STRIP)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST) // Включаем тест глубины (теперь след не видно сквозь стены и игрока)
            .withDepthWrite(false)
            .withCull(false)
            .build();

    public NumberSetting length = this.addSetting(new NumberSetting("Длина", "Длина твоей линий").withRange(0.1f, 3f).setValue(1f));
    public NumberSetting duration = this.addSetting(new NumberSetting("Время", "Время жизни следа").withRange(100f, 3000f).setValue(500f));
    public ColorSetting color = this.addSetting(new ColorSetting("Цвет", "", Color.white));

    private GpuBuffer gpuBuffer;
    private BufferAllocator bufferAllocator;
    private final List<Point> points = new ArrayList<>();

    public TrailsModule() {
        instance = this;
    }

    @Override
    protected void onDisable() {
        if (this.gpuBuffer != null && !this.gpuBuffer.isClosed()) {
            this.gpuBuffer.close();
        }
        this.gpuBuffer = null;
        if (this.bufferAllocator != null) {
            this.bufferAllocator.close();
            this.bufferAllocator = null;
        }
        this.points.clear();
        super.onDisable();
    }

    private final EventListener<WorldRenderEvent> worldRenderListener = worldRenderEvent -> {
        if (mc.player == null || mc.world == null || mc.options.getPerspective().isFirstPerson()) return;

        long currentTime = System.currentTimeMillis();
        float durationVal = duration.getValue() * length.getValue();

        points.removeIf(p -> (currentTime - p.time) > durationVal);

        float tickDelta = worldRenderEvent.computeFloat();
        Vec3d pos = mc.player.getLerpedPos(tickDelta);

        if (points.isEmpty() || points.get(points.size() - 1).pos.squaredDistanceTo(pos) > 0.001) {
            points.add(new Point(pos));
        }

        if (points.size() < 2) return;

        if (this.bufferAllocator == null) {
            this.bufferAllocator = new BufferAllocator(4096 * VertexFormats.POSITION_COLOR.getVertexSize());
        }

        CloseableBuffer allocated = this.bufferAllocator.getAllocated();
        if (allocated != null) {
            allocated.close();
        }

        BufferBuilder bufferBuilder = new BufferBuilder(this.bufferAllocator, DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

        MatrixStack matrixStack = worldRenderEvent.getMatrixStack();
        matrixStack.push();
        Vec3d cameraPos = mc.gameRenderer.getCamera().getCameraPos();
        matrixStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        Matrix4f matrix = matrixStack.peek().getPositionMatrix();

        float playerHeight = mc.player.getHeight() * 1.1F;
        int vertexCount = 0;

        for (int i = 0; i < points.size(); i++) {
            Point p = points.get(i);
            float progress = (float) i / points.size();
            int colorVal = this.color.getArgb();

            int alpha = (int) (progress * 255);
            int r = (colorVal >> 16) & 0xFF;
            int g = (colorVal >> 8) & 0xFF;
            int b = colorVal & 0xFF;

            bufferBuilder.vertex(matrix, (float) p.pos.x, (float) p.pos.y, (float) p.pos.z).color(r, g, b, alpha);
            bufferBuilder.vertex(matrix, (float) p.pos.x, (float) p.pos.y + playerHeight, (float) p.pos.z).color(r, g, b, alpha);
            vertexCount += 2;
        }

        matrixStack.pop();

        BuiltBuffer builtBuffer = bufferBuilder.endNullable();
        if (builtBuffer != null) {
            try (builtBuffer) {
                if (vertexCount > 0 && mc.getFramebuffer() != null && mc.getFramebuffer().getColorAttachmentView() != null) {
                    if (this.gpuBuffer != null && !this.gpuBuffer.isClosed() && this.gpuBuffer.size() >= builtBuffer.getBuffer().remaining()) {
                        RenderSystem.getDevice()
                                .createCommandEncoder()
                                .writeToBuffer(this.gpuBuffer.slice(0L, builtBuffer.getBuffer().remaining()), builtBuffer.getBuffer());
                    } else {
                        if (this.gpuBuffer != null && !this.gpuBuffer.isClosed()) {
                            this.gpuBuffer.close();
                        }
                        this.gpuBuffer = RenderSystem.getDevice().createBuffer(() -> "greed_trails", 40, builtBuffer.getBuffer());
                    }

                    GpuBufferSlice gpuBufferSlice = RenderSystem.getDynamicUniforms().write(matrix4f3, vector4f, vector3f, matrix4f2);

                    try (RenderPass renderPass = RenderSystem.getDevice()
                            .createCommandEncoder()
                            .createRenderPass(
                                    () -> "greed_trails",
                                    mc.getFramebuffer().getColorAttachmentView(),
                                    OptionalInt.empty(),
                                    null,
                                    OptionalDouble.empty()
                            )) {
                        renderPass.setPipeline(TRAIL_PIPELINE);
                        renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
                        renderPass.setUniform("Projection", RenderSystem.getProjectionMatrixBuffer());
                        renderPass.setVertexBuffer(0, this.gpuBuffer);
                        renderPass.draw(0, vertexCount);
                    }
                }
            }
        }
    };

    public static TrailsModule getInstance() {
        if (instance == null) {
            instance = new TrailsModule();
        }
        return instance;
    }

    private static class Point {
        public Vec3d pos;
        public long time;

        Point(Vec3d pos) {
            this.pos = pos;
            this.time = System.currentTimeMillis();
        }
    }
}