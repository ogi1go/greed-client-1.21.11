package greed.client.module.render;

import greed.client.module.combat.AimAssistModule;
import greed.client.module.combat.AttackAuraModule;
import greed.client.setting.BooleanSetting;
import greed.client.util.Colors;
import greed.client.GreedClient;
import greed.client.util.DepthSnapshot;
import greed.client.util.Easings;
import greed.client.event.EventListener;
import greed.client.setting.ModeSetting;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.render.WorldProjection;
import greed.client.util.EntityUtils;
import greed.client.ui.UiTheme;
import greed.client.util.RaycastUtils;
import greed.client.ui.AnimatedValue;
import greed.client.render.ThemeColors;
import greed.client.module.combat.TriggerBotModule;
import greed.client.event.WorldRenderEvent;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.BufferAllocator.CloseableBuffer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

@ModuleInfo(name = "TargetEsp", description = "Подсвечивает цель AttackAura, AimAssist или TrigerBot призраками и кольцом.", category = ModuleCategory.RENDER)
public final class TargetEspModule extends Module {
   private static TargetEspModule targetEspModule;
   private static final RenderPipeline pipeline2 = RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
      .withLocation(GreedClient.id("pipeline/target_ghosts"))
      .withVertexShader(Identifier.ofVanilla("core/position_tex_color"))
      .withFragmentShader(Identifier.ofVanilla("core/position_tex_color"))
      .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS)
      .withBlend(BlendFunction.LIGHTNING)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .withSampler("Sampler0")
      .build();
   private static final RenderPipeline pipeline3 = RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
      .withLocation(GreedClient.id("pipeline/target_ghosts_depth"))
      .withVertexShader(Identifier.ofVanilla("core/position_tex_color"))
      .withFragmentShader(Identifier.ofVanilla("core/position_tex_color"))
      .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS)
      .withBlend(BlendFunction.LIGHTNING)
      .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .withSampler("Sampler0")
      .build();
   private static final RenderPipeline pipeline4 = RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
      .withLocation(GreedClient.id("pipeline/target_crystals"))
      .withVertexShader(Identifier.ofVanilla("core/position_color"))
      .withFragmentShader(Identifier.ofVanilla("core/position_color"))
      .withVertexFormat(VertexFormats.POSITION_COLOR, DrawMode.QUADS)
      .withBlend(BlendFunction.LIGHTNING)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .build();
   private static final RenderPipeline pipeline5 = RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
      .withLocation(GreedClient.id("pipeline/target_crystals_depth"))
      .withVertexShader(Identifier.ofVanilla("core/position_color"))
      .withFragmentShader(Identifier.ofVanilla("core/position_color"))
      .withVertexFormat(VertexFormats.POSITION_COLOR, DrawMode.QUADS)
      .withBlend(BlendFunction.LIGHTNING)
      .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .build();
   private static final RenderPipeline pipeline6 = RenderPipeline.builder()
      .withLocation(GreedClient.id("pipeline/target_scan"))
      .withVertexShader(GreedClient.id("core/scan"))
      .withFragmentShader(GreedClient.id("core/scan"))
      .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS)
      .withBlend(BlendFunction.LIGHTNING)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("ScanParams", UniformType.UNIFORM_BUFFER)
      .build();
   private static final Vector4f vector4f = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
   private static final Vector3f vector3f2 = new Vector3f();
   private static final Matrix4f matrix4f = new Matrix4f();
   private static final Vector3f[] crystalVertices = new Vector3f[]{
      new Vector3f(0.0F, 1.5F, 0.0F),
      new Vector3f(0.0F, -1.5F, 0.0F),
      new Vector3f(1.0F, 0.0F, 0.0F),
      new Vector3f(-1.0F, 0.0F, 0.0F),
      new Vector3f(0.0F, 0.0F, 1.0F),
      new Vector3f(0.0F, 0.0F, -1.0F)
   };
   private static final int[][] crystalTriangleIndices = new int[][]{{0, 2, 4}, {0, 4, 3}, {0, 3, 5}, {0, 5, 2}, {1, 4, 2}, {1, 3, 4}, {1, 5, 3}, {1, 2, 5}};
   private static final float[] crystalLayerAlphas = new float[]{1.0F, 0.8F, 0.6F, 0.9F, 0.7F, 0.5F, 0.4F, 0.6F};
   public ModeSetting mode;
   public ModeSetting ghostStyle;
   public ModeSetting circleStyle;
   public BooleanSetting trident;
   public BooleanSetting projectiles;
   private final AnimatedValue animation = new AnimatedValue(350L);
   private LivingEntity livingEntity2;
   private float effectTime;
   private long effectLastTime = System.currentTimeMillis();
   private float lightningHurt;
   private float circleTime;
   private long circleLastTime = System.currentTimeMillis();
   private float circleHurt;
   private LivingEntity livingEntity3;
   private long nestedVisibleStart;
   private long nestedLastSeen;
   private float[] ribbonPoints = new float[192];
   private float[] ribbonNormals = new float[192];
   private float[] ribbonFalloffs = new float[64];
   private int ribbonPointCount;
   private ArrayList<float[]> lightningBranches = new ArrayList<>();
   private BufferBuilder bufferBuilder5;
   private BufferBuilder bufferBuilder6;
   private Quaternionf quaternionf2;
   private Vector3f vector3f3 = new Vector3f();
   private float ribbonWidthUnit;
   private float unusedFloat;
   private float ribbonRadiusUnit;
   private int lightningPrimaryColor;
   private int lightningSecondaryColor;
   private final HashMap<Integer, float[]> projectileFade = new HashMap<>();
   private float rhombusFade;
   private float ghostTime;
   private long ghostLastTime;
   private final HashMap<Integer, Vec3d> entityPositionCache = new HashMap<>();
   private final AnimatedValue animation2 = new AnimatedValue(400L);
   private float chainTime;
   private long chainLastTime;
   private final float[] skullTierGlow = new float[3];
   private long skullLastTime = System.currentTimeMillis();
   private GpuBuffer gpuBuffer2;
   private final GpuBuffer[] gpuBufferArray = new GpuBuffer[8];
   private int drawBufferCursor;
   private final BufferAllocator[] bufferAllocatorArray = new BufferAllocator[2];
   private final HashMap<String, Identifier> textureIdentifierCache = new HashMap<>();
   private GpuBuffer gpuBuffer3;
   private boolean pendingGhostRender;
   private float pendingAlpha;
   private float pendingHurt;
   private float pendingTickDelta;
   private final Matrix4f matrix4f2 = new Matrix4f();
   private GpuBufferSlice gpuBufferSlice2;
   private ProjectionType projectionType;
   private final EventListener<WorldRenderEvent> worldRenderListener = worldRenderEvent -> {
      if (mc.player != null && mc.world != null) {
         this.drawBufferCursor = 0;
         if (this.mode.isSelected("Цепи") && this.trident.isChecked()) {
            this.renderProjectileChains(worldRenderEvent.getMatrixStack(), worldRenderEvent.computeFloat());
         }

         if (this.mode.isSelected("Молнии") && this.projectiles.isChecked()) {
            this.renderProjectileLightning(worldRenderEvent.getMatrixStack(), worldRenderEvent.computeFloat());
         }

         LivingEntity livingEntity = AttackAuraModule.getInstance().isEnabled() && AttackAuraModule.getInstance().getTarget() != null
            ? AttackAuraModule.getInstance().getTarget()
            : (
               AimAssistModule.getInstance().isEnabled() && AimAssistModule.getInstance().getTarget() != null
                  ? AimAssistModule.getInstance().getTarget()
                  : (TriggerBotModule.getInstance().isEnabled() ? TriggerBotModule.getInstance().getTarget() : null)
            );
         if (livingEntity != null && livingEntity != this.livingEntity2) {
            this.livingEntity2 = livingEntity;
            this.animation.getAnimatedValue(0.0);
            this.ghostTime = 0.0F;
            this.ghostLastTime = System.currentTimeMillis();
            this.animation2.getAnimatedValue(0.0);
            this.chainLastTime = System.currentTimeMillis();
         }

         this.animation.processData(livingEntity != null ? 1.0 : 0.0);
         if (this.livingEntity2 != null) {
            if (this.livingEntity2.isRemoved()) {
               this.livingEntity2 = null;
            } else {
               float ghostFade = (float)this.animation.computeDouble();
               if (ghostFade < 0.003921569F) {
                  if (livingEntity == null) {
                     this.livingEntity2 = null;
                  }

                  this.effectTime = 0.0F;
               } else {
                  float hurtFraction = this.livingEntity2.hurtTime / 10.0F;
                  if (this.mode.isSelected("Призраки")) {
                     if (TagsModule.getInstance().checkState()) {
                        this.pendingGhostRender = true;
                        this.pendingAlpha = ghostFade;
                        this.pendingHurt = hurtFraction;
                        this.pendingTickDelta = worldRenderEvent.computeFloat();
                        this.matrix4f2.set(worldRenderEvent.getMatrixStack().peek().getPositionMatrix());
                        this.gpuBufferSlice2 = RenderSystem.getProjectionMatrixBuffer();
                        this.projectionType = RenderSystem.getProjectionType();
                     } else if (this.ghostStyle.isSelected("Второй")) {
                        this.renderGhostOrbs(worldRenderEvent.getMatrixStack(), this.livingEntity2, worldRenderEvent.computeFloat(), ghostFade, hurtFraction);
                     } else {
                        this.renderGhostSprites(worldRenderEvent.getMatrixStack(), this.livingEntity2, worldRenderEvent.computeFloat(), ghostFade, hurtFraction);
                     }
                  }

                  if (this.mode.isSelected("Молнии")) {
                     this.renderLightning(worldRenderEvent.getMatrixStack(), this.livingEntity2, worldRenderEvent.computeFloat(), ghostFade, hurtFraction);
                  }

                  if (this.mode.isSelected("Круг")) {
                     this.renderCircle(worldRenderEvent.getMatrixStack(), this.livingEntity2, worldRenderEvent.computeFloat(), ghostFade, hurtFraction);
                  }

                  if (this.mode.isSelected("Кристаллы")) {
                     this.renderCrystals(worldRenderEvent.getMatrixStack(), this.livingEntity2, worldRenderEvent.computeFloat(), ghostFade, hurtFraction);
                  }

                  if (this.mode.isSelected("Ромб")) {
                     this.renderRhombus(worldRenderEvent.getMatrixStack(), this.livingEntity2, worldRenderEvent.computeFloat(), ghostFade, hurtFraction);
                  }

                  if (this.mode.isSelected("Цепи")) {
                     this.renderChains(worldRenderEvent.getMatrixStack(), this.livingEntity2, worldRenderEvent.computeFloat(), ghostFade, hurtFraction);
                  }

                  if (this.mode.isSelected("Череп")) {
                     this.renderSkull(worldRenderEvent.getMatrixStack(), this.livingEntity2, worldRenderEvent.computeFloat(), ghostFade, hurtFraction);
                  }
               }
            }
         }
      }
   };

   private BufferAllocator getBufferAllocator(int allocatorIndex, int allocatorCapacity) {
      BufferAllocator bufferAllocator = this.bufferAllocatorArray[allocatorIndex];
      if (bufferAllocator == null) {
         bufferAllocator = this.bufferAllocatorArray[allocatorIndex] = new BufferAllocator(allocatorCapacity);
      }

      CloseableBuffer allocated = bufferAllocator.getAllocated();
      if (allocated != null) {
         allocated.close();
      }

      return bufferAllocator;
   }

   private void renderGhostSprites(MatrixStack matrixStack, LivingEntity entity, float tickDelta, float alpha, float colorPhase) {
      if (mc.getTextureManager().getTexture(Identifier.of("greed", "textures/particles/bloom.png")).getGlTextureView() != null
         && mc.getFramebuffer() != null
         && mc.getFramebuffer().getColorAttachmentView() != null) {
         Quaternionf rotation = mc.gameRenderer.getCamera().getRotation();
         double ghostOffsetX = MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX()) - mc.gameRenderer.getCamera().getCameraPos().x;
         double ghostOffsetY = MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY()) - mc.gameRenderer.getCamera().getCameraPos().y;
         double ghostOffsetZ = MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ()) - mc.gameRenderer.getCamera().getCameraPos().z;
         this.ghostTime = this.ghostTime + 3.5F * (float)(System.currentTimeMillis() - this.ghostLastTime) / 1000.0F;
         this.ghostLastTime = System.currentTimeMillis();
         int ghostStrandCount = 4;
         int ghostStrandSteps = 15;
         float ghostSpiralRadiusX = 0.68F;
         float ghostHeightOffset = 1.05F;
         float ghostBobAmplitude = 0.24F;
         float ghostBobFrequency = 0.12F;
         float ghostSpiralRadiusZ = 0.45F;
         float ghostRadialDrift = -0.02F;
         float ghostSpiralPhase = 0.12F;
         float ghostHeightStart = 0.02F;
         float ghostHeightSpan = 0.05F;
         float ghostAngleStep = (float)(0.5 * Math.asin(ghostHeightSpan / 0.5F));
         int ghostQuadCount = 0;
         BufferAllocator bufferAllocator = this.getBufferAllocator(1, ghostStrandCount * ghostStrandSteps * 4 * VertexFormats.POSITION_TEXTURE_COLOR.getVertexSize());
         BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
         Vector3f vector3f = new Vector3f();

         for (int index = 0; index < ghostStrandCount; index++) {
            float ghostStrandSign = index < 2 ? 1.0F : -1.0F;
            float ghostSideSign = index % 2 == 0 ? -1.0F : 1.0F;
            float ghostDriftOffsetX = index % 2 == 0 ? ghostRadialDrift : -ghostRadialDrift;
            float ghostDriftOffsetZ = index % 2 == 0 ? ghostSpiralRadiusZ : -ghostSpiralRadiusZ;

            for (int index2 = 0; index2 < ghostStrandSteps; index2++) {
               int ghostStepIndex = index % 2 == 0 ? index2 : ghostStrandSteps - 1 - index2;
               float ghostStrandAngle = this.ghostTime + ghostStepIndex * ghostAngleStep;
               float ghostStrandWidth = 5.0F * MathHelper.lerp((float)ghostStepIndex / ghostStrandSteps, ghostHeightStart, ghostHeightSpan);
               this.emitBillboard(
                  bufferBuilder,
                  rotation,
                  vector3f,
                  ghostOffsetX + ghostStrandSign * (ghostSpiralRadiusX * MathHelper.sin(ghostStrandAngle + ghostSpiralPhase) + ghostDriftOffsetX),
                  ghostOffsetY + ghostHeightOffset + ghostBobAmplitude * MathHelper.sin(this.ghostTime + ghostStepIndex * ghostBobFrequency) + ghostDriftOffsetZ,
                  ghostOffsetZ + ghostStrandSign * ghostSideSign * ghostSpiralRadiusX * MathHelper.cos(ghostStrandAngle - ghostSpiralPhase),
                  ghostStrandWidth,
                  0.0F,
                  Colors.computeInt6(this.themeColor(index * 90, colorPhase), alpha)
               );
               ghostQuadCount++;
            }
         }

         try (BuiltBuffer builtBuffer = bufferBuilder.end()) {
            this.drawQuads(
               matrixStack, "textures/particles/bloom.png", builtBuffer, ghostQuadCount, RaycastUtils.matchesCriteria(mc.player, entity), false
            );
         }
      }
   }

   private float smoothStep(float smoothEdgeStart, float smoothEdgeEnd, float smoothValue) {
      return MathHelper.clamp((smoothValue - smoothEdgeStart) / (smoothEdgeEnd - smoothEdgeStart), 0.0F, 1.0F)
         * MathHelper.clamp((smoothValue - smoothEdgeStart) / (smoothEdgeEnd - smoothEdgeStart), 0.0F, 1.0F)
         * (3.0F - 2.0F * MathHelper.clamp((smoothValue - smoothEdgeStart) / (smoothEdgeEnd - smoothEdgeStart), 0.0F, 1.0F));
   }

   private void renderChains(MatrixStack matrixStack, LivingEntity entity, float chainTickDelta, float chainAlpha, float chainColorPhase) {
      this.chainTime = (this.chainTime + 75.0F * (float)(System.currentTimeMillis() - this.chainLastTime) / 1000.0F) % 720.0F;
      this.chainLastTime = System.currentTimeMillis();
      this.animation2.processData(chainAlpha >= 0.95F ? 1.0 : 0.0);
      float chainWaveA = 20.0F * Math.min(1.0F + MathHelper.sin((float)Math.toRadians(this.chainTime)), 1.0F);
      float chainWaveB = 20.0F * MathHelper.sin((float)Math.toRadians(this.chainTime));
      float chainOrbitRadius = entity.getWidth() * 1.5F;
      float chainBandHeight = 1.0F;
      float chainAngleStep = 22.5F;
      double chainCenterX = MathHelper.lerp(chainTickDelta, entity.lastRenderX, entity.getX()) - mc.gameRenderer.getCamera().getCameraPos().x;
      double chainCenterY = MathHelper.lerp(chainTickDelta, entity.lastRenderY, entity.getY())
         + entity.getHeight() * 0.5F
         - chainBandHeight / 2.0F
         - mc.gameRenderer.getCamera().getCameraPos().y;
      double chainCenterZ = MathHelper.lerp(chainTickDelta, entity.lastRenderZ, entity.getZ()) - mc.gameRenderer.getCamera().getCameraPos().z;
      int chainQuadCount = 0;
      Vector3f vector3f = new Vector3f();
      BufferAllocator bufferAllocator = this.getBufferAllocator(0, 256 * VertexFormats.POSITION_TEXTURE_COLOR.getVertexSize());
      BufferAllocator bufferAllocator2 = this.getBufferAllocator(1, 256 * VertexFormats.POSITION_TEXTURE_COLOR.getVertexSize());
      BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      BufferBuilder bufferBuilder2 = new BufferBuilder(bufferAllocator2, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

      for (int index = 0; index < 2; index++) {
         float chainLayerAlpha = index == 0 ? chainAlpha : (float)this.animation2.computeDouble();
         if (!(chainLayerAlpha < 0.003921569F)) {
            float chainLayerRadius = chainOrbitRadius * (1.2F - 0.5F * chainLayerAlpha);
            float chainTubeOffsetX = (index == 0 ? chainWaveA : -chainWaveA) / 100.0F;
            float chainTubeOffsetZ = (index == 0 ? -chainWaveB : chainWaveB) / 100.0F;
            Quaternionf quaternionf = new Quaternionf()
               .rotateZ((float)Math.toRadians(index == 0 ? chainWaveA : -chainWaveA))
               .rotateX((float)Math.toRadians(index == 0 ? chainWaveB : -chainWaveB));

            for (float chainTubeAngle = chainAngleStep; chainTubeAngle <= 720.0F; chainTubeAngle += chainAngleStep) {
               int chainColor = Colors.computeInt6(this.themeColor(chainTubeAngle * 3.0F, chainColorPhase), chainLayerAlpha);
               float chainInnerX0 = chainTubeOffsetX + MathHelper.sin((float)Math.toRadians(chainTubeAngle - chainAngleStep + this.chainTime * 0.5F)) * chainLayerRadius;
               float chainInnerZ0 = chainTubeOffsetZ + MathHelper.cos((float)Math.toRadians(chainTubeAngle - chainAngleStep + this.chainTime * 0.5F)) * chainLayerRadius;
               float chainInnerX1 = chainTubeOffsetX + MathHelper.sin((float)Math.toRadians(chainTubeAngle + this.chainTime * 0.5F)) * chainLayerRadius;
               float chainInnerZ1 = chainTubeOffsetZ + MathHelper.cos((float)Math.toRadians(chainTubeAngle + this.chainTime * 0.5F)) * chainLayerRadius;
               quaternionf.transform(vector3f.set(chainInnerX0, 0.0F, chainInnerZ0));
               bufferBuilder.vertex((float)(chainCenterX + vector3f.x), (float)(chainCenterY + vector3f.y), (float)(chainCenterZ + vector3f.z))
                  .texture((chainTubeAngle - chainAngleStep) / 360.0F * 3.0F, 0.0F)
                  .color(chainColor);
               quaternionf.transform(vector3f.set(chainInnerX1, 0.0F, chainInnerZ1));
               bufferBuilder.vertex((float)(chainCenterX + vector3f.x), (float)(chainCenterY + vector3f.y), (float)(chainCenterZ + vector3f.z))
                  .texture(chainTubeAngle / 360.0F * 3.0F, 0.0F)
                  .color(chainColor);
               quaternionf.transform(vector3f.set(chainInnerX1, chainBandHeight, chainInnerZ1));
               bufferBuilder.vertex((float)(chainCenterX + vector3f.x), (float)(chainCenterY + vector3f.y), (float)(chainCenterZ + vector3f.z))
                  .texture(chainTubeAngle / 360.0F * 3.0F, 1.0F)
                  .color(chainColor);
               quaternionf.transform(vector3f.set(chainInnerX0, chainBandHeight, chainInnerZ0));
               bufferBuilder.vertex((float)(chainCenterX + vector3f.x), (float)(chainCenterY + vector3f.y), (float)(chainCenterZ + vector3f.z))
                  .texture((chainTubeAngle - chainAngleStep) / 360.0F * 3.0F, 1.0F)
                  .color(chainColor);
               quaternionf.transform(
                  vector3f.set(
                     MathHelper.lerp(0.87F, (chainInnerX0 + chainInnerX1) / 2.0F, chainInnerX0),
                     MathHelper.lerp(0.87F, chainBandHeight / 2.0F, 0.0F),
                     MathHelper.lerp(0.87F, (chainInnerZ0 + chainInnerZ1) / 2.0F, chainInnerZ0)
                  )
               );
               bufferBuilder2.vertex((float)(chainCenterX + vector3f.x), (float)(chainCenterY + vector3f.y), (float)(chainCenterZ + vector3f.z))
                  .texture(0.0F, 0.0F)
                  .color(Colors.computeInt6(chainColor, 0.05F));
               quaternionf.transform(
                  vector3f.set(
                     MathHelper.lerp(0.87F, (chainInnerX0 + chainInnerX1) / 2.0F, chainInnerX1),
                     MathHelper.lerp(0.87F, chainBandHeight / 2.0F, 0.0F),
                     MathHelper.lerp(0.87F, (chainInnerZ0 + chainInnerZ1) / 2.0F, chainInnerZ1)
                  )
               );
               bufferBuilder2.vertex((float)(chainCenterX + vector3f.x), (float)(chainCenterY + vector3f.y), (float)(chainCenterZ + vector3f.z))
                  .texture(1.0F, 0.0F)
                  .color(Colors.computeInt6(chainColor, 0.05F));
               quaternionf.transform(
                  vector3f.set(
                     MathHelper.lerp(0.87F, (chainInnerX0 + chainInnerX1) / 2.0F, chainInnerX1),
                     MathHelper.lerp(0.87F, chainBandHeight / 2.0F, chainBandHeight),
                     MathHelper.lerp(0.87F, (chainInnerZ0 + chainInnerZ1) / 2.0F, chainInnerZ1)
                  )
               );
               bufferBuilder2.vertex((float)(chainCenterX + vector3f.x), (float)(chainCenterY + vector3f.y), (float)(chainCenterZ + vector3f.z))
                  .texture(1.0F, 1.0F)
                  .color(Colors.computeInt6(chainColor, 0.05F));
               quaternionf.transform(
                  vector3f.set(
                     MathHelper.lerp(0.87F, (chainInnerX0 + chainInnerX1) / 2.0F, chainInnerX0),
                     MathHelper.lerp(0.87F, chainBandHeight / 2.0F, chainBandHeight),
                     MathHelper.lerp(0.87F, (chainInnerZ0 + chainInnerZ1) / 2.0F, chainInnerZ0)
                  )
               );
               bufferBuilder2.vertex((float)(chainCenterX + vector3f.x), (float)(chainCenterY + vector3f.y), (float)(chainCenterZ + vector3f.z))
                  .texture(0.0F, 1.0F)
                  .color(Colors.computeInt6(chainColor, 0.05F));
               chainQuadCount++;
            }
         }
      }

      try (BuiltBuffer builtBuffer = bufferBuilder2.end()) {
         this.drawQuads(
            matrixStack, "textures/particles/glow.png", builtBuffer, chainQuadCount, RaycastUtils.matchesCriteria(mc.player, entity), false
         );
      }

      try (BuiltBuffer builtBuffer2 = bufferBuilder.end()) {
         this.drawQuads(
            matrixStack, "textures/particles/chain.png", builtBuffer2, chainQuadCount, RaycastUtils.matchesCriteria(mc.player, entity), true
         );
      }
   }

   private int emitBandSegment(
      BufferBuilder bufferBuilder,
      float bandCenterX,
      float bandCenterY,
      float bandCenterZ,
      LivingEntity livingEntity,
      float bandHeight,
      float bandPhase,
      float bandLowerY,
      float bandUpperY,
      float bandLowerOffset,
      float bandUpperOffset,
      int bandLowerColor,
      int bandUpperColor
   ) {
      int bandQuadCount = 0;

      for (int index = 0; index < 360; index += 2) {
         float bandCosA = MathHelper.cos((float)Math.toRadians(index));
         float bandSinA = MathHelper.sin((float)Math.toRadians(index));
         float bandCosB = MathHelper.cos((float)Math.toRadians(index + 2));
         float bandSinB = MathHelper.sin((float)Math.toRadians(index + 2));
         bufferBuilder.vertex(
               bandCenterX + bandCosA * (this.tubeRadius(livingEntity, bandLowerY / bandHeight, bandPhase) + bandLowerOffset),
               bandCenterY + bandLowerY,
               bandCenterZ + bandSinA * (this.tubeRadius(livingEntity, bandLowerY / bandHeight, bandPhase) + bandLowerOffset)
            )
            .color(bandLowerColor);
         bufferBuilder.vertex(
               bandCenterX + bandCosB * (this.tubeRadius(livingEntity, bandLowerY / bandHeight, bandPhase) + bandLowerOffset),
               bandCenterY + bandLowerY,
               bandCenterZ + bandSinB * (this.tubeRadius(livingEntity, bandLowerY / bandHeight, bandPhase) + bandLowerOffset)
            )
            .color(bandLowerColor);
         bufferBuilder.vertex(
               bandCenterX + bandCosB * (this.tubeRadius(livingEntity, bandUpperY / bandHeight, bandPhase) + bandUpperOffset),
               bandCenterY + bandUpperY,
               bandCenterZ + bandSinB * (this.tubeRadius(livingEntity, bandUpperY / bandHeight, bandPhase) + bandUpperOffset)
            )
            .color(bandUpperColor);
         bufferBuilder.vertex(
               bandCenterX + bandCosA * (this.tubeRadius(livingEntity, bandUpperY / bandHeight, bandPhase) + bandUpperOffset),
               bandCenterY + bandUpperY,
               bandCenterZ + bandSinA * (this.tubeRadius(livingEntity, bandUpperY / bandHeight, bandPhase) + bandUpperOffset)
            )
            .color(bandUpperColor);
         bandQuadCount++;
      }

      return bandQuadCount;
   }

   private int buildRibbon(float ribbonScale, boolean ribbonTaper) {
      if (this.ribbonPointCount < 2) {
         return 0;
      }

      for (int index = 0; index < this.ribbonPointCount; index++) {
         float ribbonPointX = this.ribbonPoints[index * 3];
         float ribbonPointY = this.ribbonPoints[index * 3 + 1];
         float ribbonPointZ = this.ribbonPoints[index * 3 + 2];
         float ribbonNormalX = 0.0F;
         float ribbonNormalY = 0.0F;
         float ribbonNormalZ = 0.0F;
         float ribbonNextNormalX = 0.0F;
         float ribbonNextNormalY = 0.0F;
         float ribbonNextNormalZ = 0.0F;
         boolean ribbonHasPrev = index > 0;
         boolean ribbonHasNext = index < this.ribbonPointCount - 1;
         if (ribbonHasPrev) {
            float ribbonSegNormalX = ribbonPointX - this.ribbonPoints[index * 3 - 3];
            float ribbonSegNormalY = ribbonPointY - this.ribbonPoints[index * 3 - 2];
            float ribbonSegNormalZ = ribbonPointZ - this.ribbonPoints[index * 3 - 1];
            ribbonNormalX = ribbonSegNormalY * ribbonPointZ - ribbonSegNormalZ * ribbonPointY;
            ribbonNormalY = ribbonSegNormalZ * ribbonPointX - ribbonSegNormalX * ribbonPointZ;
            ribbonNormalZ = ribbonSegNormalX * ribbonPointY - ribbonSegNormalY * ribbonPointX;
            float ribbonSegLength = MathHelper.sqrt(ribbonNormalX * ribbonNormalX + ribbonNormalY * ribbonNormalY + ribbonNormalZ * ribbonNormalZ);
            if (ribbonSegLength < 1.0E-6F) {
               ribbonHasPrev = false;
            } else {
               ribbonNormalX /= ribbonSegLength;
               ribbonNormalY /= ribbonSegLength;
               ribbonNormalZ /= ribbonSegLength;
            }
         }

         if (ribbonHasNext) {
            float ribbonSegNormalX = this.ribbonPoints[index * 3 + 3] - ribbonPointX;
            float ribbonSegNormalY = this.ribbonPoints[index * 3 + 4] - ribbonPointY;
            float ribbonSegNormalZ = this.ribbonPoints[index * 3 + 5] - ribbonPointZ;
            ribbonNextNormalX = ribbonSegNormalY * ribbonPointZ - ribbonSegNormalZ * ribbonPointY;
            ribbonNextNormalY = ribbonSegNormalZ * ribbonPointX - ribbonSegNormalX * ribbonPointZ;
            ribbonNextNormalZ = ribbonSegNormalX * ribbonPointY - ribbonSegNormalY * ribbonPointX;
            float ribbonSegLength = MathHelper.sqrt(ribbonNextNormalX * ribbonNextNormalX + ribbonNextNormalY * ribbonNextNormalY + ribbonNextNormalZ * ribbonNextNormalZ);
            if (ribbonSegLength < 1.0E-6F) {
               ribbonHasNext = false;
            } else {
               ribbonNextNormalX /= ribbonSegLength;
               ribbonNextNormalY /= ribbonSegLength;
               ribbonNextNormalZ /= ribbonSegLength;
            }
         }

         float ribbonSegLength = 1.0F;
         float ribbonSegNormalX;
         float ribbonSegNormalY;
         float ribbonSegNormalZ;
         if (ribbonHasPrev && ribbonHasNext) {
            if (ribbonNormalX * ribbonNextNormalX + ribbonNormalY * ribbonNextNormalY + ribbonNormalZ * ribbonNextNormalZ < 0.0F) {
               ribbonNextNormalX = -ribbonNextNormalX;
               ribbonNextNormalY = -ribbonNextNormalY;
               ribbonNextNormalZ = -ribbonNextNormalZ;
            }

            ribbonSegNormalX = ribbonNormalX + ribbonNextNormalX;
            ribbonSegNormalY = ribbonNormalY + ribbonNextNormalY;
            ribbonSegNormalZ = ribbonNormalZ + ribbonNextNormalZ;
            float ribbonSumLength = MathHelper.sqrt(ribbonSegNormalX * ribbonSegNormalX + ribbonSegNormalY * ribbonSegNormalY + ribbonSegNormalZ * ribbonSegNormalZ);
            if (ribbonSumLength < 1.0E-6F) {
               ribbonSegNormalX = ribbonNormalX;
               ribbonSegNormalY = ribbonNormalY;
               ribbonSegNormalZ = ribbonNormalZ;
            } else {
               ribbonSegNormalX /= ribbonSumLength;
               ribbonSegNormalY /= ribbonSumLength;
               ribbonSegNormalZ /= ribbonSumLength;
               ribbonSegLength = 1.0F / Math.max(ribbonSegNormalX * ribbonNormalX + ribbonSegNormalY * ribbonNormalY + ribbonSegNormalZ * ribbonNormalZ, 0.35F);
            }
         } else if (ribbonHasPrev) {
            ribbonSegNormalX = ribbonNormalX;
            ribbonSegNormalY = ribbonNormalY;
            ribbonSegNormalZ = ribbonNormalZ;
         } else if (ribbonHasNext) {
            ribbonSegNormalX = ribbonNextNormalX;
            ribbonSegNormalY = ribbonNextNormalY;
            ribbonSegNormalZ = ribbonNextNormalZ;
         } else {
            ribbonSegNormalX = 0.0F;
            ribbonSegNormalY = 0.0F;
            ribbonSegNormalZ = 0.0F;
         }

         this.ribbonNormals[index * 3] = ribbonSegNormalX;
         this.ribbonNormals[index * 3 + 1] = ribbonSegNormalY;
         this.ribbonNormals[index * 3 + 2] = ribbonSegNormalZ;
         this.ribbonFalloffs[index] = ribbonSegLength;
      }

      float ribbonWidthA = this.ribbonWidthUnit * ribbonScale;
      float ribbonPointX = this.ribbonRadiusUnit * ribbonScale;

      for (int index2 = 0; index2 < this.ribbonPointCount - 1; index2++) {
         float ribbonPointZ = 1.0F;
         float ribbonNormalX = 1.0F;
         if (ribbonTaper) {
            float ribbonNormalY = MathHelper.clamp(((float)index2 / (this.ribbonPointCount - 1) - 0.35F) / 0.65F, 0.0F, 1.0F);
            ribbonPointZ = 1.0F - 0.75F * ribbonNormalY * ribbonNormalY * (3.0F - 2.0F * ribbonNormalY);
            ribbonNormalY = MathHelper.clamp(((float)(index2 + 1) / (this.ribbonPointCount - 1) - 0.35F) / 0.65F, 0.0F, 1.0F);
            ribbonNormalX = 1.0F - 0.75F * ribbonNormalY * ribbonNormalY * (3.0F - 2.0F * ribbonNormalY);
         }

         float ribbonNormalZ = this.ribbonNormals[index2 * 3] * ribbonWidthA * this.ribbonFalloffs[index2] * ribbonPointZ;
         float ribbonNextNormalX = this.ribbonNormals[index2 * 3 + 1] * ribbonWidthA * this.ribbonFalloffs[index2] * ribbonPointZ;
         float ribbonNextNormalY = this.ribbonNormals[index2 * 3 + 2] * ribbonWidthA * this.ribbonFalloffs[index2] * ribbonPointZ;
         float ribbonNextNormalZ = this.ribbonNormals[index2 * 3 + 3] * ribbonWidthA * this.ribbonFalloffs[index2 + 1] * ribbonNormalX;
         float ribbonHasPrev = this.ribbonNormals[index2 * 3 + 4] * ribbonWidthA * this.ribbonFalloffs[index2 + 1] * ribbonNormalX;
         float ribbonHasNext = this.ribbonNormals[index2 * 3 + 5] * ribbonWidthA * this.ribbonFalloffs[index2 + 1] * ribbonNormalX;
         this.bufferBuilder5
            .vertex(
               this.ribbonPoints[index2 * 3] - ribbonNormalZ,
               this.ribbonPoints[index2 * 3 + 1] - ribbonNextNormalX,
               this.ribbonPoints[index2 * 3 + 2] - ribbonNextNormalY
            )
            .color(this.lightningPrimaryColor);
         this.bufferBuilder5
            .vertex(
               this.ribbonPoints[index2 * 3 + 3] - ribbonNextNormalZ,
               this.ribbonPoints[index2 * 3 + 4] - ribbonHasPrev,
               this.ribbonPoints[index2 * 3 + 5] - ribbonHasNext
            )
            .color(this.lightningPrimaryColor);
         this.bufferBuilder5
            .vertex(
               this.ribbonPoints[index2 * 3 + 3] + ribbonNextNormalZ,
               this.ribbonPoints[index2 * 3 + 4] + ribbonHasPrev,
               this.ribbonPoints[index2 * 3 + 5] + ribbonHasNext
            )
            .color(this.lightningPrimaryColor);
         this.bufferBuilder5
            .vertex(
               this.ribbonPoints[index2 * 3] + ribbonNormalZ,
               this.ribbonPoints[index2 * 3 + 1] + ribbonNextNormalX,
               this.ribbonPoints[index2 * 3 + 2] + ribbonNextNormalY
            )
            .color(this.lightningPrimaryColor);
         this.emitBillboard(
            this.bufferBuilder6,
            this.quaternionf2,
            this.vector3f3,
            (this.ribbonPoints[index2 * 3] + this.ribbonPoints[index2 * 3 + 3]) * 0.5F,
            (this.ribbonPoints[index2 * 3 + 1] + this.ribbonPoints[index2 * 3 + 4]) * 0.5F,
            (this.ribbonPoints[index2 * 3 + 2] + this.ribbonPoints[index2 * 3 + 5]) * 0.5F,
            ribbonPointX * (ribbonPointZ + ribbonNormalX) * 0.5F,
            0.0F,
            this.lightningSecondaryColor
         );
      }

      return this.ribbonPointCount - 1;
   }

   public void updateState() {
      if (this.pendingGhostRender) {
         this.pendingGhostRender = false;
         if (this.livingEntity2 != null && !this.livingEntity2.isRemoved()) {
            RenderSystem.backupProjectionMatrix();
            RenderSystem.setProjectionMatrix(this.gpuBufferSlice2, this.projectionType);
            MatrixStack matrixStack = new MatrixStack();
            matrixStack.multiplyPositionMatrix(this.matrix4f2);
            if (this.ghostStyle.isSelected("Второй")) {
               this.renderGhostOrbs(matrixStack, this.livingEntity2, this.pendingTickDelta, this.pendingAlpha, this.pendingHurt);
            } else {
               this.renderGhostSprites(matrixStack, this.livingEntity2, this.pendingTickDelta, this.pendingAlpha, this.pendingHurt);
            }

            RenderSystem.restoreProjectionMatrix();
         }
      }
   }

   private void renderProjectileChains(MatrixStack matrixStack, float tridentTickDelta) {
      for (Entity entity : mc.world.getEntities()) {
         if (entity instanceof PersistentProjectileEntity entity2
            && entity2.getOwner() == mc.player
            && !(entity2.getVelocity().lengthSquared() < 1.0E-5)) {
            double tridentPlayerX = MathHelper.lerp(tridentTickDelta, mc.player.lastRenderX, mc.player.getX())
               - mc.gameRenderer.getCamera().getCameraPos().x;
            double tridentPlayerY = MathHelper.lerp(tridentTickDelta, mc.player.lastRenderY, mc.player.getY())
               + mc.player.getStandingEyeHeight() * 0.75F
               - mc.gameRenderer.getCamera().getCameraPos().y;
            double tridentPlayerZ = MathHelper.lerp(tridentTickDelta, mc.player.lastRenderZ, mc.player.getZ())
               - mc.gameRenderer.getCamera().getCameraPos().z;
            double tridentProjX = MathHelper.lerp(tridentTickDelta, entity2.lastRenderX, entity2.getX())
               - mc.gameRenderer.getCamera().getCameraPos().x;
            double tridentProjY = MathHelper.lerp(tridentTickDelta, entity2.lastRenderY, entity2.getY())
               - mc.gameRenderer.getCamera().getCameraPos().y;
            double tridentProjZ = MathHelper.lerp(tridentTickDelta, entity2.lastRenderZ, entity2.getZ())
               - mc.gameRenderer.getCamera().getCameraPos().z;
            float tridentDistance = (float)Math.sqrt(
               (tridentProjX - tridentPlayerX) * (tridentProjX - tridentPlayerX)
                  + (tridentProjY - tridentPlayerY) * (tridentProjY - tridentPlayerY)
                  + (tridentProjZ - tridentPlayerZ) * (tridentProjZ - tridentPlayerZ)
            );
            float tridentWidth = 1.0F;
            float tridentSag = tridentDistance * 0.06F;
            int tridentSegmentCount = 16;
            float[] floatArray = new float[tridentSegmentCount + 1];
            float[] floatArray2 = new float[tridentSegmentCount + 1];
            float[] floatArray3 = new float[tridentSegmentCount + 1];
            float[] floatArray4 = new float[tridentSegmentCount + 1];
            float[] floatArray5 = new float[tridentSegmentCount + 1];
            float[] floatArray6 = new float[tridentSegmentCount + 1];
            BufferAllocator bufferAllocator = this.getBufferAllocator(1, tridentSegmentCount * 4 * VertexFormats.POSITION_TEXTURE_COLOR.getVertexSize());
            BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

            for (int index = 0; index <= tridentSegmentCount; index++) {
               floatArray[index] = (float)MathHelper.lerp((double)index / tridentSegmentCount, tridentPlayerX, tridentProjX);
               floatArray2[index] = (float)(
                  MathHelper.lerp((double)index / tridentSegmentCount, tridentPlayerY, tridentProjY)
                     - tridentSag * 4.0F * ((float)index / tridentSegmentCount) * (1.0F - (float)index / tridentSegmentCount)
               );
               floatArray3[index] = (float)MathHelper.lerp((double)index / tridentSegmentCount, tridentPlayerZ, tridentProjZ);
            }

            for (int index2 = 0; index2 <= tridentSegmentCount; index2++) {
               float tridentTangentX = floatArray[Math.min(index2 + 1, tridentSegmentCount)] - floatArray[Math.max(index2 - 1, 0)];
               float tridentTangentY = floatArray2[Math.min(index2 + 1, tridentSegmentCount)] - floatArray2[Math.max(index2 - 1, 0)];
               float tridentTangentZ = floatArray3[Math.min(index2 + 1, tridentSegmentCount)] - floatArray3[Math.max(index2 - 1, 0)];
               floatArray4[index2] = tridentTangentY * floatArray3[index2] - tridentTangentZ * floatArray2[index2];
               floatArray5[index2] = tridentTangentZ * floatArray[index2] - tridentTangentX * floatArray3[index2];
               floatArray6[index2] = tridentTangentX * floatArray2[index2] - tridentTangentY * floatArray[index2];
               float tridentSegLength = Math.max(
                  1.0E-5F,
                  (float)Math.sqrt(
                     floatArray4[index2] * floatArray4[index2]
                        + floatArray5[index2] * floatArray5[index2]
                        + floatArray6[index2] * floatArray6[index2]
                  )
               );
               floatArray4[index2] *= tridentWidth / 2.0F / tridentSegLength;
               floatArray5[index2] *= tridentWidth / 2.0F / tridentSegLength;
               floatArray6[index2] *= tridentWidth / 2.0F / tridentSegLength;
            }

            for (int index3 = 0; index3 < tridentSegmentCount; index3++) {
               bufferBuilder.vertex(
                     floatArray[index3] - floatArray4[index3],
                     floatArray2[index3] - floatArray5[index3],
                     floatArray3[index3] - floatArray6[index3]
                  )
                  .texture((float)index3 / tridentSegmentCount * tridentDistance / (tridentWidth * 1.3333F), 0.0F)
                  .color(this.themeColor(index3 * 24.0F, 0.0F));
               bufferBuilder.vertex(
                     floatArray[index3 + 1] - floatArray4[index3 + 1],
                     floatArray2[index3 + 1] - floatArray5[index3 + 1],
                     floatArray3[index3 + 1] - floatArray6[index3 + 1]
                  )
                  .texture((float)(index3 + 1) / tridentSegmentCount * tridentDistance / (tridentWidth * 1.3333F), 0.0F)
                  .color(this.themeColor((index3 + 1) * 24.0F, 0.0F));
               bufferBuilder.vertex(
                     floatArray[index3 + 1] + floatArray4[index3 + 1],
                     floatArray2[index3 + 1] + floatArray5[index3 + 1],
                     floatArray3[index3 + 1] + floatArray6[index3 + 1]
                  )
                  .texture((float)(index3 + 1) / tridentSegmentCount * tridentDistance / (tridentWidth * 1.3333F), 1.0F)
                  .color(this.themeColor((index3 + 1) * 24.0F, 0.0F));
               bufferBuilder.vertex(
                     floatArray[index3] + floatArray4[index3],
                     floatArray2[index3] + floatArray5[index3],
                     floatArray3[index3] + floatArray6[index3]
                  )
                  .texture((float)index3 / tridentSegmentCount * tridentDistance / (tridentWidth * 1.3333F), 1.0F)
                  .color(this.themeColor(index3 * 24.0F, 0.0F));
            }

            try (BuiltBuffer builtBuffer = bufferBuilder.end()) {
               this.drawQuads(
                  matrixStack, "textures/particles/chain.png", builtBuffer, tridentSegmentCount, RaycastUtils.matchesCriteria(mc.player, entity2), true
               );
            }
         }
      }
   }

   public TargetEspModule.DataRecord getNested0418(PlayerEntity playerEntity) {
      if (this.isEnabled()
         && this.mode.isSelected("Круг")
         && this.circleStyle.isSelected("Четвёртый")
         && this.livingEntity2 == playerEntity
         && !(this.animation.computeDouble() <= 0.001F)) {
         if (this.livingEntity3 != playerEntity || System.currentTimeMillis() - this.nestedLastSeen > 250L) {
            this.livingEntity3 = playerEntity;
            this.nestedVisibleStart = System.currentTimeMillis();
         }

         this.nestedLastSeen = System.currentTimeMillis();
         float nestedPhase = (float)((System.currentTimeMillis() - this.nestedVisibleStart) % 1800L) / 1800.0F;
         return new TargetEspModule.DataRecord(
            this.themeColor(0.0F, this.circleHurt),
            -49088,
            nestedPhase,
            this.smoothStep(0.0F, 1.0F, MathHelper.clamp((float)this.animation.computeDouble(), 0.0F, 1.0F))
               * this.smoothStep(0.0F, 0.08F, nestedPhase)
               * (1.0F - this.smoothStep(0.88F, 1.0F, nestedPhase)),
            MathHelper.clamp(this.circleHurt, 0.0F, 1.0F),
            1.0F
         );
      } else {
         return null;
      }
   }

   private void renderLightning(MatrixStack matrixStack, LivingEntity entity, float boltTickDelta, float boltAlpha, float boltColorPhase) {
      long boltNow = System.currentTimeMillis();
      this.effectTime = this.effectTime + (float)Math.min(boltNow - this.effectLastTime, 100L) / 1000.0F;
      this.effectLastTime = boltNow;
      this.lightningHurt = entity.hurtTime > 0 ? entity.hurtTime / 10.0F : Math.max(0.0F, this.lightningHurt - 0.1F);
      int boltWaveIndex = (int)this.effectTime;
      float boltWaveFraction = this.effectTime - boltWaveIndex;
      float height = entity.getHeight();
      float boltRadius = entity.getWidth() * 0.75F + 0.16F;
      float boltCenterX = (float)(
         MathHelper.lerp(boltTickDelta, entity.lastRenderX, entity.getX()) - mc.gameRenderer.getCamera().getCameraPos().x
      );
      float boltCenterY = (float)(
         MathHelper.lerp(boltTickDelta, entity.lastRenderY, entity.getY()) - mc.gameRenderer.getCamera().getCameraPos().y
      );
      float boltCenterZ = (float)(
         MathHelper.lerp(boltTickDelta, entity.lastRenderZ, entity.getZ()) - mc.gameRenderer.getCamera().getCameraPos().z
      );
      boolean boltVisible = RaycastUtils.matchesCriteria(mc.player, entity);
      this.ribbonWidthUnit = height * 0.005F;
      this.ribbonRadiusUnit = height * 0.09F;
      this.quaternionf2 = mc.gameRenderer.getCamera().getRotation();
      int boltGlowQuads = 0;
      int boltLineQuads = 0;
      BufferAllocator bufferAllocator = this.getBufferAllocator(0, 2048 * VertexFormats.POSITION_COLOR.getVertexSize());
      BufferAllocator bufferAllocator2 = this.getBufferAllocator(1, 2048 * VertexFormats.POSITION_TEXTURE_COLOR.getVertexSize());
      this.bufferBuilder5 = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      this.bufferBuilder6 = new BufferBuilder(bufferAllocator2, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

      for (int index = 0; index < 3; index++) {
         float boltPhase = this.effectTime * 1.3F + index / 3.0F;
         int boltIndex = (int)Math.floor(boltPhase);
         float boltFraction = boltPhase - boltIndex;
         float boltIntensity = MathHelper.clamp(boltFraction / 0.08F, 0.0F, 1.0F)
            * (1.0F - MathHelper.clamp((boltFraction - 0.42F) / 0.3F, 0.0F, 1.0F))
            * MathHelper.clamp(0.85F + 0.15F * this.valueNoise(index, 900, boltWaveIndex, boltWaveFraction), 0.0F, 1.0F)
            * boltAlpha;
         if (!(boltIntensity < 0.003921569F)) {
            this.lightningPrimaryColor = Colors.computeInt6(
               Colors.computeInt7(
                  Colors.computeInt2(
                     Colors.computeInt2(-1, -1, (float)(Math.sin(System.currentTimeMillis() / 700.0 + Math.toRadians(44.0)) + 1.0) / 2.0F),
                     -49088,
                     boltColorPhase
                  ),
                  255
               ),
               boltIntensity
            );
            this.lightningSecondaryColor = Colors.computeInt6(this.lightningPrimaryColor, 0.13F);
            float boltStartAngle = this.hashRandom(index, 11, boltIndex) * (float) Math.PI;
            float boltAngleSpan = 0.6F + 0.5F * Math.abs(this.hashRandom(index, 12, boltIndex));
            float boltAngleSign = this.hashRandom(index, 13, boltIndex) >= 0.0F ? 1.0F : -1.0F;
            float boltTopY = height * (0.85F + 0.05F * this.hashRandom(index, 14, boltIndex));
            float boltHeightDrop = height * 0.67F;
            float boltSpread = boltRadius * (1.0F + 0.12F * this.hashRandom(index, 15, boltIndex));
            float boltWobble = height * 0.16F * MathHelper.sin(this.effectTime * 1.7F + index * 2.1F);
            float boltPrevX = 0.0F;
            float boltPrevY = 0.0F;
            float boltPrevZ = 0.0F;

            for (int index2 = 0; index2 <= 3; index2++) {
               float boltSegmentT = index2 / 3.0F;
               float boltAngle = boltStartAngle + this.effectTime * 0.9F + boltAngleSign * boltAngleSpan * (float) (Math.PI * 2) * boltSegmentT;
               float boltPointX = boltCenterX + MathHelper.cos(boltAngle) * boltSpread;
               float boltPointY = boltCenterY + boltTopY - boltHeightDrop * boltSegmentT + boltWobble;
               float boltPointZ = boltCenterZ + MathHelper.sin(boltAngle) * boltSpread;
               if (index2 > 0) {
                  boltLineQuads += this.emitLightningArc(
                     boltPrevX,
                     boltPrevY,
                     boltPrevZ,
                     boltPointX,
                     boltPointY,
                     boltPointZ,
                     (index * 131 + index2) * 17 + boltIndex * 101 | 1,
                     boltWaveIndex,
                     boltWaveFraction,
                     index2 == 3
                  );
               }

               boltPrevX = boltPointX;
               boltPrevY = boltPointY;
               boltPrevZ = boltPointZ;
            }
         }
      }

      boltGlowQuads = boltLineQuads;
      if (boltGlowQuads > 0) {
         try (BuiltBuffer builtBuffer = this.bufferBuilder6.end()) {
            this.drawQuads(matrixStack, "textures/particles/glow.png", builtBuffer, boltGlowQuads, boltVisible, false);
         }
      }

      if (boltLineQuads > 0) {
         try (BuiltBuffer builtBuffer2 = this.bufferBuilder5.end()) {
            this.drawQuads(matrixStack, null, builtBuffer2, boltLineQuads, boltVisible, false);
         }
      }
   }

   private int themeColor(float themeHueDegrees, float themeBlend) {
      return Colors.computeInt2(
         Colors.computeInt2(
            UiTheme.intVal7,
            UiTheme.intVal8,
            (float)(Math.sin(System.currentTimeMillis() / 700.0 + Math.toRadians(themeHueDegrees)) + 1.0) / 2.0F
         ),
         -49088,
         themeBlend
      );
   }

   private void renderGhostOrbs(MatrixStack matrixStack, LivingEntity livingEntity, float trailTickDelta, float trailAlpha, float trailColorPhase) {
      Quaternionf rotation = mc.gameRenderer.getCamera().getRotation();
      double trailCenterX = MathHelper.lerp(trailTickDelta, livingEntity.lastRenderX, livingEntity.getX()) - mc.gameRenderer.getCamera().getCameraPos().x;
      double trailCenterY = MathHelper.lerp(trailTickDelta, livingEntity.lastRenderY, livingEntity.getY()) - mc.gameRenderer.getCamera().getCameraPos().y;
      double trailCenterZ = MathHelper.lerp(trailTickDelta, livingEntity.lastRenderZ, livingEntity.getZ()) - mc.gameRenderer.getCamera().getCameraPos().z;
      int trailRingCount = 4;
      int trailOrbCount = 15;
      float width = livingEntity.getWidth();
      float trailVerticalOffset = livingEntity.getHeight() / 2.0F + 0.2F;
      float trailOrbitRadius = 0.7F;
      float trailSizeStart = 0.2F;
      float trailSizeEnd = 0.6F;
      double trailTimeSeconds = System.currentTimeMillis() / 1000.0;
      double trailBaseAngle = trailTimeSeconds * 3.0 % (Math.PI * 2);
      double trailAngleStep = Math.toRadians(60.0) / (trailOrbCount - 1);
      int trailQuadCount = 0;
      BufferAllocator bufferAllocator = this.getBufferAllocator(1, trailRingCount * trailOrbCount * 4 * VertexFormats.POSITION_TEXTURE_COLOR.getVertexSize());
      BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      Vector3f vector3f = new Vector3f();

      for (int index = 0; index < trailRingCount; index++) {
         double trailPhase = trailTimeSeconds + index * 15.0;
         double trailRingAngle = index * (Math.PI / 2);

         for (int index2 = 0; index2 < trailOrbCount; index2++) {
            double trailTiltAngle = index2 * trailAngleStep;
            double trailAngle = trailTiltAngle + trailBaseAngle + trailRingAngle;
            this.emitBillboard(
               bufferBuilder,
               rotation,
               vector3f,
               trailCenterX + width * Math.cos(trailAngle),
               trailCenterY + trailVerticalOffset + Math.sin(trailPhase + trailTiltAngle + index) * trailOrbitRadius,
               trailCenterZ + width * Math.sin(trailAngle),
               MathHelper.lerp((float)index2 / (trailOrbCount - 1), trailSizeStart, trailSizeEnd) / 2.0F,
               0.0F,
               Colors.computeInt6(this.themeColor(index2 * 14, trailColorPhase), trailAlpha)
            );
            trailQuadCount++;
         }
      }

      try (BuiltBuffer builtBuffer = bufferBuilder.end()) {
         this.drawQuads(matrixStack, "textures/particles/glow.png", builtBuffer, trailQuadCount, false, false);
      }
   }

   private void buildLightning(
      float branchStartX,
      float branchStartY,
      float branchStartZ,
      float branchEndX,
      float branchEndY,
      float branchEndZ,
      int branchDepth,
      int branchSeed,
      int branchLevel,
      int branchWaveIndex,
      float branchWaveFraction
   ) {
      float branchDx = branchEndX - branchStartX;
      float branchDy = branchEndY - branchStartY;
      float branchDz = branchEndZ - branchStartZ;
      float branchLength = MathHelper.sqrt(branchDx * branchDx + branchDy * branchDy + branchDz * branchDz);
      if (branchDepth > 0 && !(branchLength < 1.0E-4F)) {
         float branchDirX = branchDx / branchLength;
         float branchDirY = branchDy / branchLength;
         float branchDirZ = branchDz / branchLength;
         float branchFlagA = Math.abs(branchDirY) < 0.9F ? 0.0F : 1.0F;
         float branchFlagB = Math.abs(branchDirY) < 0.9F ? 1.0F : 0.0F;
         float branchRightX = -branchDirZ * branchFlagB;
         float branchRightY = branchDirZ * branchFlagA;
         float branchRightZ = branchDirX * branchFlagB - branchDirY * branchFlagA;
         float branchRightLen = MathHelper.sqrt(branchRightX * branchRightX + branchRightY * branchRightY + branchRightZ * branchRightZ);
         if (branchRightLen < 1.0E-5F) {
            branchRightLen = 1.0F;
         }

         branchRightX /= branchRightLen;
         branchRightY /= branchRightLen;
         branchRightZ /= branchRightLen;
         float branchUpX = branchDirY * branchRightZ - branchDirZ * branchRightY;
         float branchUpY = branchDirZ * branchRightX - branchDirX * branchRightZ;
         float branchUpZ = branchDirX * branchRightY - branchDirY * branchRightX;
         float branchOffsetA = this.valueNoise(branchSeed, 0, branchWaveIndex, branchWaveFraction);
         float branchOffsetB = this.valueNoise(branchSeed, 1, branchWaveIndex, branchWaveFraction);
         float branchOffsetScale = branchLength * 0.3F;
         float branchMidX = (branchStartX + branchEndX) * 0.5F + (branchRightX * branchOffsetA + branchUpX * branchOffsetB) * branchOffsetScale;
         float branchMidY = (branchStartY + branchEndY) * 0.5F + (branchRightY * branchOffsetA + branchUpY * branchOffsetB) * branchOffsetScale;
         float branchMidZ = (branchStartZ + branchEndZ) * 0.5F + (branchRightZ * branchOffsetA + branchUpZ * branchOffsetB) * branchOffsetScale;
         this.buildLightning(
            branchStartX,
            branchStartY,
            branchStartZ,
            branchMidX,
            branchMidY,
            branchMidZ,
            branchDepth - 1,
            branchSeed * 2 + 7,
            branchLevel,
            branchWaveIndex,
            branchWaveFraction
         );
         this.buildLightning(
            branchMidX,
            branchMidY,
            branchMidZ,
            branchEndX,
            branchEndY,
            branchEndZ,
            branchDepth - 1,
            branchSeed * 2 + 9,
            branchLevel,
            branchWaveIndex,
            branchWaveFraction
         );
         if (branchLevel < 2 && branchDepth >= 2 && this.valueNoise(branchSeed, 2, branchWaveIndex, branchWaveFraction) > 0.45F) {
            float branchBendA = this.valueNoise(branchSeed, 3, branchWaveIndex, branchWaveFraction);
            float branchBendB = this.valueNoise(branchSeed, 4, branchWaveIndex, branchWaveFraction);
            float branchDeltaX = branchDirX * 0.4F + branchRightX * branchBendA + branchUpX * branchBendB;
            float branchDeltaY = branchDirY * 0.4F + branchRightY * branchBendA + branchUpY * branchBendB;
            float branchDeltaZ = branchDirZ * 0.4F + branchRightZ * branchBendA + branchUpZ * branchBendB;
            float branchSubLength = MathHelper.sqrt(branchDeltaX * branchDeltaX + branchDeltaY * branchDeltaY + branchDeltaZ * branchDeltaZ);
            if (branchSubLength < 1.0E-5F) {
               branchSubLength = 1.0F;
            }

            float branchTailLength = branchLength * 0.75F;
            this.lightningBranches
               .add(
                  new float[]{
                     branchMidX,
                     branchMidY,
                     branchMidZ,
                     branchMidX + branchDeltaX / branchSubLength * branchTailLength,
                     branchMidY + branchDeltaY / branchSubLength * branchTailLength,
                     branchMidZ + branchDeltaZ / branchSubLength * branchTailLength,
                     Math.min(branchDepth - 1, 3),
                     branchSeed * 3 + 13,
                     branchLevel + 1,
                     0.55F
                  }
               );
         }
      } else {
         this.addLightningPoint(branchEndX, branchEndY, branchEndZ);
      }
   }

   private void renderScanOverlay(MatrixStack matrixStack, LivingEntity livingEntity, float scanTickDelta, float scanAlpha, float scanColorPhase) {
      if (mc.getFramebuffer() != null && mc.getFramebuffer().getColorAttachmentView() != null && DepthSnapshot.getGpuTextureView() != null) {
         float scanCenterX = (float)(
            MathHelper.lerp(scanTickDelta, livingEntity.lastRenderX, livingEntity.getX()) - mc.gameRenderer.getCamera().getCameraPos().x
         );
         float scanCenterY = (float)(
            MathHelper.lerp(scanTickDelta, livingEntity.lastRenderY, livingEntity.getY()) - mc.gameRenderer.getCamera().getCameraPos().y
         );
         float scanCenterZ = (float)(
            MathHelper.lerp(scanTickDelta, livingEntity.lastRenderZ, livingEntity.getZ()) - mc.gameRenderer.getCamera().getCameraPos().z
         );
         float scanBoxHeight = livingEntity.getHeight() + 0.3F;
         float scanBoxWidth = livingEntity.getWidth() * 0.9F + 0.3F;
         float scanSweepHeight = livingEntity.getHeight() - 0.25F;
         float scanSweepAngle = (float)(System.currentTimeMillis() % 2200L) / 2200.0F * (float) (Math.PI * 2);
         float scanHeightFraction = 0.5F + 0.5F * MathHelper.sin(scanSweepAngle);
         float scanDirection = MathHelper.cos(scanSweepAngle) >= 0.0F ? 1.0F : -1.0F;
         float scanCosAbs = Math.abs(MathHelper.cos(scanSweepAngle));
         float scanSweepY = scanCenterY + 0.12F + scanSweepHeight * scanHeightFraction;
         int scanColor = this.themeColor(0.0F, scanColorPhase);
         if (this.gpuBuffer3 == null || this.gpuBuffer3.isClosed()) {
            this.gpuBuffer3 = RenderSystem.getDevice().createBuffer(() -> "greed_target_scan_params", 136, 112L);
         }

         MemoryStack memoryStack = MemoryStack.stackPush();

         try {
            ByteBuffer byteBuffer = memoryStack.calloc(112);
            new Matrix4f(WorldProjection.getMatrix4f()).mul(matrixStack.peek().getPositionMatrix()).invert().get(0, byteBuffer);
            byteBuffer.putFloat(64, scanCenterX);
            byteBuffer.putFloat(68, scanCenterY);
            byteBuffer.putFloat(72, scanCenterZ);
            byteBuffer.putFloat(76, scanBoxWidth);
            byteBuffer.putFloat(80, scanSweepY);
            byteBuffer.putFloat(84, scanDirection);
            byteBuffer.putFloat(88, scanCosAbs);
            byteBuffer.putFloat(92, (float)(System.currentTimeMillis() % 100000L) / 1000.0F);
            byteBuffer.putFloat(96, scanCenterY + 0.06F);
            byteBuffer.putFloat(100, scanCenterY - 0.1F + scanBoxHeight);
            byteBuffer.putFloat(104, scanAlpha);
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.gpuBuffer3.slice(0L, 112L), byteBuffer);
         } catch (Throwable throwable) {
            if (memoryStack != null) {
               try {
                  memoryStack.close();
               } catch (Throwable throwable2) {
                  throwable.addSuppressed(throwable2);
               }
            }

            throw throwable;
         }

         if (memoryStack != null) {
            memoryStack.close();
         }

         try (BufferAllocator scanAllocator = new BufferAllocator(4 * VertexFormats.POSITION_TEXTURE_COLOR.getVertexSize())) {
            BufferBuilder bufferBuilder = new BufferBuilder(scanAllocator, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            bufferBuilder.vertex(-1.0F, -1.0F, 0.0F).texture(0.0F, 0.0F).color(scanColor);
            bufferBuilder.vertex(-1.0F, 1.0F, 0.0F).texture(0.0F, 1.0F).color(scanColor);
            bufferBuilder.vertex(1.0F, 1.0F, 0.0F).texture(1.0F, 1.0F).color(scanColor);
            bufferBuilder.vertex(1.0F, -1.0F, 0.0F).texture(1.0F, 0.0F).color(scanColor);

            try (BuiltBuffer builtBuffer = bufferBuilder.end()) {
               if (this.gpuBuffer2 != null && !this.gpuBuffer2.isClosed() && this.gpuBuffer2.size() >= builtBuffer.getBuffer().remaining()) {
                  RenderSystem.getDevice()
                     .createCommandEncoder()
                     .writeToBuffer(this.gpuBuffer2.slice(0L, builtBuffer.getBuffer().remaining()), builtBuffer.getBuffer());
               } else {
                  if (this.gpuBuffer2 != null && !this.gpuBuffer2.isClosed()) {
                     this.gpuBuffer2.close();
                  }

                  this.gpuBuffer2 = RenderSystem.getDevice().createBuffer(() -> "greed_target_ghosts_vertices", 40, builtBuffer.getBuffer());
               }

               try (RenderPass renderPass = RenderSystem.getDevice()
                     .createCommandEncoder()
                     .createRenderPass(
                        () -> "greed_target_scan", mc.getFramebuffer().getColorAttachmentView(), OptionalInt.empty(), null, OptionalDouble.empty()
                     )) {
                  renderPass.setPipeline(pipeline6);
                  renderPass.bindTexture("Sampler0", DepthSnapshot.getGpuTextureView(), RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
                  renderPass.setUniform("ScanParams", this.gpuBuffer3);
                  renderPass.setVertexBuffer(0, this.gpuBuffer2);
                  renderPass.setIndexBuffer(
                     RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexBuffer(6), RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexType()
                  );
                  renderPass.drawIndexed(0, 0, 6, 1);
               }
            }
         }
      }
   }

   private float valueNoise(int noiseSeed, int noiseChannel, int noiseWaveIndex, float noiseWaveFraction) {
      return MathHelper.lerp(
         noiseWaveFraction * noiseWaveFraction * (3.0F - 2.0F * noiseWaveFraction),
         this.hashRandom(noiseSeed, noiseChannel, noiseWaveIndex),
         this.hashRandom(noiseSeed, noiseChannel, noiseWaveIndex + 1)
      );
   }

   private void renderCrystals(MatrixStack matrixStack, LivingEntity entity, float crystalTickDelta, float crystalAlpha, float crystalColorPhase) {
      long crystalNow = System.currentTimeMillis();
      float crystalOrbitRadius = (1.2F - 0.5F * crystalAlpha) * entity.getWidth() * 1.5F;
      float crystalScale = 0.1F;
      float crystalHeight = 1.0F;
      int crystalBaseColor = Colors.computeInt6(this.themeColor(0.0F, crystalColorPhase), crystalAlpha);
      matrixStack.push();
      matrixStack.translate(
         entity.getLerpedPos(crystalTickDelta).x - mc.gameRenderer.getCamera().getCameraPos().x,
         entity.getLerpedPos(crystalTickDelta).y - mc.gameRenderer.getCamera().getCameraPos().y,
         entity.getLerpedPos(crystalTickDelta).z - mc.gameRenderer.getCamera().getCameraPos().z
      );
      BufferAllocator bufferAllocator = this.getBufferAllocator(0, 18 * crystalTriangleIndices.length * 4 * VertexFormats.POSITION_COLOR.getVertexSize());
      BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      MatrixStack matrixStack2 = new MatrixStack();
      int crystalQuadCount = 0;

      for (int index = 0; index < 360; index += 20) {
         float crystalAngle = (float)Math.toRadians(
            index + (float)((crystalNow + (long)(7333.0F * (index / 360.0F))) % 7333L) / 7333.0F * 360.0F
         );
         float crystalPosX = MathHelper.sin(crystalAngle) * crystalOrbitRadius;
         float crystalPosZ = MathHelper.cos(crystalAngle) * crystalOrbitRadius;
         float crystalPosY = 0.1F + entity.getHeight() * (float)Easings.EASE_IN_OUT_QUAD.apply(Math.abs(MathHelper.sin(index)));
         matrixStack2.push();
         matrixStack2.translate(crystalPosX, crystalPosY, crystalPosZ);
         matrixStack2.multiply(
            new Quaternionf()
               .rotationTo(
                  new Vector3f(0.0F, 1.0F, 0.0F), new Vector3f(-crystalPosX, entity.getHeight() * 0.5F - crystalPosY, -crystalPosZ).normalize()
               )
         );
         matrixStack2.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(((float)(crystalNow % 1200L) / 1200.0F * 360.0F + index * 13.7F) % 360.0F));
         matrixStack2.scale(crystalScale, crystalScale, crystalScale);

         for (int index2 = 0; index2 < crystalTriangleIndices.length; index2++) {
            for (int index3 = 0; index3 < 4; index3++) {
               Vector3f vector3f = crystalVertices[crystalTriangleIndices[index2][Math.min(index3, 2)]];
               bufferBuilder.vertex(matrixStack2.peek().getPositionMatrix(), vector3f.x, vector3f.y, vector3f.z)
                  .color(
                     Colors.computeInt2(
                        crystalBaseColor, Colors.computeInt7(0, Colors.computeInt4(crystalBaseColor)), 1.0F - crystalLayerAlphas[index2]
                     )
                  );
            }

            crystalQuadCount++;
         }

         matrixStack2.pop();
      }

      try (BuiltBuffer builtBuffer = bufferBuilder.end()) {
         this.drawQuads(matrixStack, null, builtBuffer, crystalQuadCount, RaycastUtils.matchesCriteria(mc.player, entity), false);
      }

      bufferAllocator = this.getBufferAllocator(1, 72 * VertexFormats.POSITION_TEXTURE_COLOR.getVertexSize());
      bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      Vector3f crystalVector = new Vector3f();

      for (int index4 = 0; index4 < 360; index4 += 20) {
         float crystalAngle2 = (float)Math.toRadians(
            index4 + (float)((crystalNow + (long)(7333.0F * (index4 / 360.0F))) % 7333L) / 7333.0F * 360.0F
         );
         this.emitBillboard(
            bufferBuilder,
            mc.gameRenderer.getCamera().getRotation(),
            crystalVector,
            MathHelper.sin(crystalAngle2) * crystalOrbitRadius,
            0.1F + entity.getHeight() * (float)Easings.EASE_IN_OUT_QUAD.apply(Math.abs(MathHelper.sin(index4))),
            MathHelper.cos(crystalAngle2) * crystalOrbitRadius,
            crystalHeight / 2.0F,
            0.0F,
            Colors.computeInt6(crystalBaseColor, 0.2F)
         );
      }

      try (BuiltBuffer builtBuffer2 = bufferBuilder.end()) {
         this.drawQuads(matrixStack, "textures/particles/glow.png", builtBuffer2, 18, RaycastUtils.matchesCriteria(mc.player, entity), false);
      }

      matrixStack.pop();
   }

   private void renderCircle(MatrixStack matrixStack, LivingEntity livingEntity, float circleTickDelta, float circleAlpha, float circleColorPhase) {
      long circleNow = System.currentTimeMillis();
      this.circleTime = this.circleTime + (float)Math.min(circleNow - this.circleLastTime, 100L) / 1000.0F;
      this.circleLastTime = circleNow;
      this.circleHurt = livingEntity.hurtTime > 0 ? livingEntity.hurtTime / 10.0F : Math.max(0.0F, this.circleHurt - 0.1F);
      if (!this.circleStyle.isSelected("Четвёртый")) {
         if (this.circleStyle.isSelected("Третий")) {
            float height = livingEntity.getHeight();
            float circlePhase = (float)(System.currentTimeMillis() % 3600L) / 3600.0F * (float) (Math.PI * 2);
            float circleWave = 0.5F + 0.5F * MathHelper.cos(circlePhase);
            float circleSine = MathHelper.sin(circlePhase);
            float circleModulation = this.smoothStep(0.0F, 1.0F, this.smoothStep(0.0F, 1.0F, MathHelper.clamp(circleAlpha, 0.0F, 1.0F)));
            float circleOffsetA = MathHelper.lerp(circleWave, 0.035F, height - 0.035F);
            float circleOffsetB = MathHelper.clamp(
               circleOffsetA + circleSine * Math.min(0.55F, height * 0.55F) * this.smoothStep(0.0F, 1.0F, MathHelper.clamp(circleAlpha, 0.0F, 1.0F)),
               0.02F,
               height - 0.02F
            );
            float circleCenterX = (float)(
               MathHelper.lerp(circleTickDelta, livingEntity.lastRenderX, livingEntity.getX()) - mc.gameRenderer.getCamera().getCameraPos().x
            );
            float circleCenterZ = (float)(
               MathHelper.lerp(circleTickDelta, livingEntity.lastRenderZ, livingEntity.getZ()) - mc.gameRenderer.getCamera().getCameraPos().z
            );
            float circleCenterY = (float)(
               MathHelper.lerp(circleTickDelta, livingEntity.lastRenderY, livingEntity.getY()) - mc.gameRenderer.getCamera().getCameraPos().y
            );
            int circleColorA = this.themeColor(0.0F, this.circleHurt);
            int circleColorB = Colors.computeInt2(circleColorA, -1, 0.72F * (1.0F - this.circleHurt));
            float circleColorC = circleModulation * (float)Math.sqrt(Math.abs(circleSine));
            float circleColorD = height * 0.019F;
            boolean circleVisible = RaycastUtils.matchesCriteria(mc.player, livingEntity);
            float[] floatArray = new float[]{0.0F, 0.18F, 0.52F, 1.0F};
            int[] intArray = new int[]{125, 72, 24, 0};
            int circleBandQuads = 0;
            BufferAllocator bufferAllocator = this.getBufferAllocator(0, 16384 * VertexFormats.POSITION_COLOR.getVertexSize());
            BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION_COLOR);

            for (int index = 0; index < floatArray.length - 1; index++) {
               circleBandQuads += this.emitBandSegment(
                  bufferBuilder,
                  circleCenterX,
                  circleCenterY,
                  circleCenterZ,
                  livingEntity,
                  height,
                  circleModulation,
                  MathHelper.lerp(floatArray[index], circleOffsetA, circleOffsetB),
                  MathHelper.lerp(floatArray[index + 1], circleOffsetA, circleOffsetB),
                  0.0F,
                  0.0F,
                  Colors.computeInt7(circleColorA, (int)(intArray[index] * circleColorC)),
                  Colors.computeInt7(circleColorA, (int)(intArray[index + 1] * circleColorC))
               );
            }

            circleBandQuads += this.emitBandSegment(
               bufferBuilder,
               circleCenterX,
               circleCenterY,
               circleCenterZ,
               livingEntity,
               height,
               circleModulation,
               Math.max(0.0F, circleOffsetA - 0.075F),
               circleOffsetA,
               0.025F,
               0.035F,
               Colors.computeInt7(circleColorA, 0),
               Colors.computeInt7(circleColorA, (int)(72.0F * circleModulation))
            );
            circleBandQuads += this.emitBandSegment(
               bufferBuilder,
               circleCenterX,
               circleCenterY,
               circleCenterZ,
               livingEntity,
               height,
               circleModulation,
               circleOffsetA,
               Math.min(height, circleOffsetA + 0.075F),
               0.035F,
               0.025F,
               Colors.computeInt7(circleColorA, (int)(72.0F * circleModulation)),
               Colors.computeInt7(circleColorA, 0)
            );
            circleBandQuads += this.emitBandSegment(
               bufferBuilder,
               circleCenterX,
               circleCenterY,
               circleCenterZ,
               livingEntity,
               height,
               circleModulation,
               circleOffsetA - circleColorD,
               circleOffsetA + circleColorD,
               0.045F,
               0.045F,
               Colors.computeInt7(circleColorB, (int)(245.0F * circleModulation)),
               Colors.computeInt7(circleColorB, (int)(245.0F * circleModulation))
            );

            for (int index2 = 1; index2 <= 5; index2++) {
               circleBandQuads += this.emitBandSegment(
                  bufferBuilder,
                  circleCenterX,
                  circleCenterY,
                  circleCenterZ,
                  livingEntity,
                  height,
                  circleModulation,
                  MathHelper.lerp(index2 / 6.0F, circleOffsetA, circleOffsetB) - circleColorD,
                  MathHelper.lerp(index2 / 6.0F, circleOffsetA, circleOffsetB) + circleColorD,
                  0.012F,
                  0.012F,
                  Colors.computeInt7(circleColorA, (int)((1.0F - index2 / 6.0F) * 34.0F * circleColorC)),
                  Colors.computeInt7(circleColorA, (int)((1.0F - index2 / 6.0F) * 34.0F * circleColorC))
               );
            }

            if (circleBandQuads > 0) {
               try (BuiltBuffer builtBuffer = bufferBuilder.end()) {
                  this.drawQuads(matrixStack, null, builtBuffer, circleBandQuads, circleVisible, false);
               }
            }
         } else if (this.circleStyle.isSelected("Второй")) {
            float circleTimeMs = (float)(System.currentTimeMillis() % 1750L);
            float circlePhase = circleTimeMs > 875.0F ? -1.0F : 1.0F;
            float circleWave = circlePhase < 0.0F ? circleTimeMs / 875.0F - 1.0F : 1.0F - circleTimeMs / 875.0F;
            float circleSine = circleWave < 0.5F ? 2.0F * circleWave * circleWave : 1.0F - (float)Math.pow(-2.0F * circleWave + 2.0F, 2.0) / 2.0F;
            float circleModulation = livingEntity.getHeight() + 0.15F;
            float circleOffsetA = livingEntity.getWidth() * 0.8F;
            float circleOffsetB = livingEntity.getHeight() * 0.006F;
            float circleCenterX = (float)(
               MathHelper.lerp(circleTickDelta, livingEntity.lastRenderX, livingEntity.getX()) - mc.gameRenderer.getCamera().getCameraPos().x
            );
            float circleCenterZ = (float)(
               MathHelper.lerp(circleTickDelta, livingEntity.lastRenderZ, livingEntity.getZ()) - mc.gameRenderer.getCamera().getCameraPos().z
            );
            float circleCenterY = (float)(
                  MathHelper.lerp(circleTickDelta, livingEntity.lastRenderY, livingEntity.getY()) - mc.gameRenderer.getCamera().getCameraPos().y
               )
               + circleModulation * circleSine;
            float circleColorA = circleModulation * 0.8F * Math.min(circleSine, 1.0F - circleSine) * circlePhase;
            int circleColorB = Colors.computeInt6(this.themeColor(0.0F, this.circleHurt), circleAlpha);
            int circleColorC = Colors.computeInt6(circleColorB, 0.55F);
            int circleColorD = Colors.computeInt7(circleColorB, 0);
            boolean circleVisible = RaycastUtils.matchesCriteria(mc.player, livingEntity);
            int circleQuadCount = 0;
            BufferAllocator bufferAllocator2 = this.getBufferAllocator(0, 8192 * VertexFormats.POSITION_COLOR.getVertexSize());
            BufferBuilder bufferBuilder2 = new BufferBuilder(bufferAllocator2, DrawMode.QUADS, VertexFormats.POSITION_COLOR);

            for (int index3 = 0; index3 < 360; index3 += 3) {
               float circleCosA = MathHelper.cos((float)Math.toRadians(index3));
               float circleSinA = MathHelper.sin((float)Math.toRadians(index3));
               float circleCosB = MathHelper.cos((float)Math.toRadians(index3 + 3));
               float circleSinB = MathHelper.sin((float)Math.toRadians(index3 + 3));
               bufferBuilder2.vertex(
                     circleCenterX + circleCosA * (circleOffsetA - circleOffsetB), circleCenterY, circleCenterZ + circleSinA * (circleOffsetA - circleOffsetB)
                  )
                  .color(circleColorB);
               bufferBuilder2.vertex(
                     circleCenterX + circleCosB * (circleOffsetA - circleOffsetB), circleCenterY, circleCenterZ + circleSinB * (circleOffsetA - circleOffsetB)
                  )
                  .color(circleColorB);
               bufferBuilder2.vertex(
                     circleCenterX + circleCosB * (circleOffsetA + circleOffsetB), circleCenterY, circleCenterZ + circleSinB * (circleOffsetA + circleOffsetB)
                  )
                  .color(circleColorB);
               bufferBuilder2.vertex(
                     circleCenterX + circleCosA * (circleOffsetA + circleOffsetB), circleCenterY, circleCenterZ + circleSinA * (circleOffsetA + circleOffsetB)
                  )
                  .color(circleColorB);
               circleQuadCount++;
               bufferBuilder2.vertex(circleCenterX + circleCosA * circleOffsetA, circleCenterY, circleCenterZ + circleSinA * circleOffsetA).color(circleColorC);
               bufferBuilder2.vertex(circleCenterX + circleCosB * circleOffsetA, circleCenterY, circleCenterZ + circleSinB * circleOffsetA).color(circleColorC);
               bufferBuilder2.vertex(circleCenterX + circleCosB * circleOffsetA, circleCenterY + circleColorA, circleCenterZ + circleSinB * circleOffsetA)
                  .color(circleColorD);
               bufferBuilder2.vertex(circleCenterX + circleCosA * circleOffsetA, circleCenterY + circleColorA, circleCenterZ + circleSinA * circleOffsetA)
                  .color(circleColorD);
               circleQuadCount++;
            }

            if (circleQuadCount > 0) {
               try (BuiltBuffer builtBuffer2 = bufferBuilder2.end()) {
                  this.drawQuads(matrixStack, null, builtBuffer2, circleQuadCount, circleVisible, false);
               }
            }
         } else {
            float circleTimeMs = this.circleTime * 2.4F;
            float circlePhase = 0.5F + 0.5F * MathHelper.sin(circleTimeMs);
            float circleWave = -MathHelper.cos(circleTimeMs);
            float circleSine = this.circleTime * 1.6F;
            float circleModulation = (float)(
               MathHelper.lerp(circleTickDelta, livingEntity.lastRenderX, livingEntity.getX()) - mc.gameRenderer.getCamera().getCameraPos().x
            );
            float circleOffsetA = (float)(
               MathHelper.lerp(circleTickDelta, livingEntity.lastRenderZ, livingEntity.getZ()) - mc.gameRenderer.getCamera().getCameraPos().z
            );
            float circleOffsetB = (float)(
               MathHelper.lerp(circleTickDelta, livingEntity.lastRenderY, livingEntity.getY())
                  + (livingEntity.getHeight() + 0.1F) * circlePhase
                  - mc.gameRenderer.getCamera().getCameraPos().y
            );
            float circleCenterX = livingEntity.getWidth() * 0.9F + 1.0F * (1.0F - circleAlpha);
            float circleCenterZ = livingEntity.getHeight() * 0.006F;
            int circleCenterY = Colors.computeInt6(this.themeColor(0.0F, this.circleHurt), circleAlpha);
            int circleColorA = Colors.computeInt6(circleCenterY, 0.28F);
            int circleColorB = Colors.computeInt6(circleCenterY, 0.03F);
            int circleColorC = Colors.computeInt6(circleCenterY, 0.12F);
            boolean circleColorD = RaycastUtils.matchesCriteria(mc.player, livingEntity);
            int circleVisible = 0;
            int circleQuadCount = 0;
            BufferAllocator bufferAllocator3 = this.getBufferAllocator(0, 8192 * VertexFormats.POSITION_COLOR.getVertexSize());
            BufferAllocator bufferAllocator4 = this.getBufferAllocator(1, 48 * VertexFormats.POSITION_TEXTURE_COLOR.getVertexSize());
            BufferBuilder bufferBuilder3 = new BufferBuilder(bufferAllocator3, DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            BufferBuilder bufferBuilder4 = new BufferBuilder(bufferAllocator4, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            Vector3f vector3f = new Vector3f();

            for (int index4 = 0; index4 < 360; index4 += 3) {
               float circleSinB = MathHelper.cos((float)Math.toRadians(index4));
               float circleSinA2 = MathHelper.sin((float)Math.toRadians(index4));
               float circleCosB2 = MathHelper.cos((float)Math.toRadians(index4 + 3));
               float circleSinB2 = MathHelper.sin((float)Math.toRadians(index4 + 3));
               bufferBuilder3.vertex(
                     circleModulation + circleSinB * (circleCenterX - circleCenterZ), circleOffsetB, circleOffsetA + circleSinA2 * (circleCenterX - circleCenterZ)
                  )
                  .color(circleCenterY);
               bufferBuilder3.vertex(
                     circleModulation + circleCosB2 * (circleCenterX - circleCenterZ), circleOffsetB, circleOffsetA + circleSinB2 * (circleCenterX - circleCenterZ)
                  )
                  .color(circleCenterY);
               bufferBuilder3.vertex(
                     circleModulation + circleCosB2 * (circleCenterX + circleCenterZ), circleOffsetB, circleOffsetA + circleSinB2 * (circleCenterX + circleCenterZ)
                  )
                  .color(circleCenterY);
               bufferBuilder3.vertex(
                     circleModulation + circleSinB * (circleCenterX + circleCenterZ), circleOffsetB, circleOffsetA + circleSinA2 * (circleCenterX + circleCenterZ)
                  )
                  .color(circleCenterY);
               circleVisible++;
               float circleRibbonY0 = (0.45F + 0.12F * MathHelper.sin((float)Math.toRadians(index4) * 3.0F - circleSine)) * circleWave;
               float circleRibbonY1 = (0.45F + 0.12F * MathHelper.sin((float)Math.toRadians(index4 + 3) * 3.0F - circleSine)) * circleWave;
               bufferBuilder3.vertex(circleModulation + circleSinB * circleCenterX, circleOffsetB, circleOffsetA + circleSinA2 * circleCenterX).color(circleColorA);
               bufferBuilder3.vertex(circleModulation + circleCosB2 * circleCenterX, circleOffsetB, circleOffsetA + circleSinB2 * circleCenterX).color(circleColorA);
               bufferBuilder3.vertex(circleModulation + circleCosB2 * circleCenterX, circleOffsetB + circleRibbonY1, circleOffsetA + circleSinB2 * circleCenterX)
                  .color(circleColorB);
               bufferBuilder3.vertex(circleModulation + circleSinB * circleCenterX, circleOffsetB + circleRibbonY0, circleOffsetA + circleSinA2 * circleCenterX)
                  .color(circleColorB);
               circleVisible++;
            }

            for (int index5 = 0; index5 < 360; index5 += 7) {
               this.emitBillboard(
                  bufferBuilder4,
                  mc.gameRenderer.getCamera().getRotation(),
                  vector3f,
                  circleModulation + MathHelper.cos((float)Math.toRadians(index5)) * circleCenterX,
                  circleOffsetB,
                  circleOffsetA + MathHelper.sin((float)Math.toRadians(index5)) * circleCenterX,
                  circleCenterX * 0.2F,
                  0.0F,
                  circleColorC
               );
               circleQuadCount++;
            }

            if (circleQuadCount > 0) {
               try (BuiltBuffer builtBuffer3 = bufferBuilder4.end()) {
                  this.drawQuads(matrixStack, "textures/particles/glow.png", builtBuffer3, circleQuadCount, circleColorD, false);
               }
            }

            if (circleVisible > 0) {
               try (BuiltBuffer builtBuffer4 = bufferBuilder3.end()) {
                  this.drawQuads(matrixStack, null, builtBuffer4, circleVisible, circleColorD, false);
               }
            }
         }
      }
   }

   private int emitLightningArc(
      float arcStartX,
      float arcStartY,
      float arcStartZ,
      float arcEndX,
      float arcEndY,
      float arcEndZ,
      int arcSeed,
      int arcWaveIndex,
      float arcWaveFraction,
      boolean arcTaper
   ) {
      this.lightningBranches.clear();
      this.ribbonPointCount = 0;
      this.addLightningPoint(arcStartX, arcStartY, arcStartZ);
      this.buildLightning(arcStartX, arcStartY, arcStartZ, arcEndX, arcEndY, arcEndZ, 4, arcSeed, 0, arcWaveIndex, arcWaveFraction);
      int arcQuadCount = this.buildRibbon(1.0F, arcTaper);

      for (int index = 0; index < this.lightningBranches.size(); index++) {
         float[] floatArray = this.lightningBranches.get(index);
         this.ribbonPointCount = 0;
         this.addLightningPoint(floatArray[0], floatArray[1], floatArray[2]);
         this.buildLightning(
            floatArray[0],
            floatArray[1],
            floatArray[2],
            floatArray[3],
            floatArray[4],
            floatArray[5],
            (int)floatArray[6],
            (int)floatArray[7],
            (int)floatArray[8],
            arcWaveIndex,
            arcWaveFraction
         );
         arcQuadCount += this.buildRibbon(floatArray[9], true);
      }

      this.lightningBranches.clear();
      return arcQuadCount;
   }

   private float tubeRadius(LivingEntity livingEntity, float tubeHeightFraction, float tubePhase) {
      return (
            MathHelper.clamp(tubeHeightFraction, 0.0F, 1.0F) < 0.48F
               ? MathHelper.lerp(
                  this.smoothStep(0.24F, 0.46F, MathHelper.clamp(tubeHeightFraction, 0.0F, 1.0F)),
                  (livingEntity.getWidth() * 0.5F + 0.1F) * 1.02F,
                  (livingEntity.getWidth() * 0.5F + 0.1F) * 1.46F
               )
               : (
                  MathHelper.clamp(tubeHeightFraction, 0.0F, 1.0F) > 0.68F
                     ? MathHelper.lerp(
                        this.smoothStep(0.68F, 0.84F, MathHelper.clamp(tubeHeightFraction, 0.0F, 1.0F)),
                        (livingEntity.getWidth() * 0.5F + 0.1F) * 1.46F,
                        (livingEntity.getWidth() * 0.5F + 0.1F) * 1.16F
                     )
                     : (livingEntity.getWidth() * 0.5F + 0.1F) * 1.46F
               )
         )
         * (0.86F + 0.14F * this.smoothStep(0.0F, 1.0F, tubePhase));
   }

   private void addLightningPoint(float pathPointX, float pathPointY, float pathPointZ) {
      if (this.ribbonPointCount * 3 + 3 > this.ribbonPoints.length) {
         this.ribbonPoints = Arrays.copyOf(this.ribbonPoints, this.ribbonPoints.length * 2);
         this.ribbonNormals = Arrays.copyOf(this.ribbonNormals, this.ribbonNormals.length * 2);
         this.ribbonFalloffs = Arrays.copyOf(this.ribbonFalloffs, this.ribbonFalloffs.length * 2);
      }

      this.ribbonPoints[this.ribbonPointCount * 3] = pathPointX;
      this.ribbonPoints[this.ribbonPointCount * 3 + 1] = pathPointY;
      this.ribbonPoints[this.ribbonPointCount * 3 + 2] = pathPointZ;
      this.ribbonPointCount++;
   }

   private void emitBillboard(
      BufferBuilder bufferBuilder,
      Quaternionf quaternionf,
      Vector3f vector3f,
      double billboardCenterX,
      double billboardCenterY,
      double billboardCenterZ,
      float billboardHalfWidth,
      float billboardHalfHeight,
      int billboardColor
   ) {
      quaternionf.transform(vector3f.set(-billboardHalfWidth, -billboardHalfWidth, -billboardHalfHeight));
      bufferBuilder.vertex((float)(billboardCenterX + vector3f.x), (float)(billboardCenterY + vector3f.y), (float)(billboardCenterZ + vector3f.z))
         .texture(0.0F, 0.0F)
         .color(billboardColor);
      quaternionf.transform(vector3f.set(-billboardHalfWidth, billboardHalfWidth, -billboardHalfHeight));
      bufferBuilder.vertex((float)(billboardCenterX + vector3f.x), (float)(billboardCenterY + vector3f.y), (float)(billboardCenterZ + vector3f.z))
         .texture(0.0F, 1.0F)
         .color(billboardColor);
      quaternionf.transform(vector3f.set(billboardHalfWidth, billboardHalfWidth, -billboardHalfHeight));
      bufferBuilder.vertex((float)(billboardCenterX + vector3f.x), (float)(billboardCenterY + vector3f.y), (float)(billboardCenterZ + vector3f.z))
         .texture(1.0F, 1.0F)
         .color(billboardColor);
      quaternionf.transform(vector3f.set(billboardHalfWidth, -billboardHalfWidth, -billboardHalfHeight));
      bufferBuilder.vertex((float)(billboardCenterX + vector3f.x), (float)(billboardCenterY + vector3f.y), (float)(billboardCenterZ + vector3f.z))
         .texture(1.0F, 0.0F)
         .color(billboardColor);
   }

   private void drawQuads(MatrixStack matrixStack, String drawTexturePath, BuiltBuffer builtBuffer, int drawQuadCount, boolean drawDepthTest, boolean drawRepeat) {
      if (mc.getFramebuffer() != null && mc.getFramebuffer().getColorAttachmentView() != null) {
         Identifier identifier = drawTexturePath == null
            ? null
            : this.textureIdentifierCache.computeIfAbsent(drawTexturePath, drawIdentifierPath -> Identifier.of("greed", drawIdentifierPath));
         if (identifier == null || mc.getTextureManager().getTexture(identifier).getGlTextureView() != null) {
            int drawBufferIndex = this.drawBufferCursor++ % this.gpuBufferArray.length;
            GpuBuffer gpuBuffer = this.gpuBufferArray[drawBufferIndex];
            if (gpuBuffer != null && !gpuBuffer.isClosed() && gpuBuffer.size() >= builtBuffer.getBuffer().remaining()) {
               RenderSystem.getDevice()
                  .createCommandEncoder()
                  .writeToBuffer(gpuBuffer.slice(0L, builtBuffer.getBuffer().remaining()), builtBuffer.getBuffer());
            } else {
               if (gpuBuffer != null && !gpuBuffer.isClosed()) {
                  gpuBuffer.close();
               }

               gpuBuffer = this.gpuBufferArray[drawBufferIndex] = RenderSystem.getDevice()
                  .createBuffer(() -> "greed_target_quads_vertices", 40, builtBuffer.getBuffer());
            }

            GpuBufferSlice gpuBufferSlice = RenderSystem.getDynamicUniforms().write(matrixStack.peek().getPositionMatrix(), vector4f, vector3f2, matrix4f);

            try (RenderPass renderPass = RenderSystem.getDevice()
                  .createCommandEncoder()
                  .createRenderPass(
                     () -> "greed_target_quads",
                     mc.getFramebuffer().getColorAttachmentView(),
                     OptionalInt.empty(),
                     drawDepthTest ? DepthSnapshot.getGpuTextureView() : null,
                     OptionalDouble.empty()
                  )) {
               renderPass.setPipeline(drawTexturePath == null ? (drawDepthTest ? pipeline5 : pipeline4) : (drawDepthTest ? pipeline3 : pipeline2));
               if (identifier != null) {
                  renderPass.bindTexture(
                     "Sampler0",
                     mc.getTextureManager().getTexture(identifier).getGlTextureView(),
                     drawRepeat ? RenderSystem.getSamplerCache().getRepeated(FilterMode.LINEAR) : RenderSystem.getSamplerCache().get(FilterMode.LINEAR)
                  );
               }

               renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
               renderPass.setUniform("Projection", RenderSystem.getProjectionMatrixBuffer());
               renderPass.setVertexBuffer(0, gpuBuffer);
               renderPass.setIndexBuffer(
                  RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexBuffer(drawQuadCount * 6),
                  RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexType()
               );
               renderPass.drawIndexed(0, 0, drawQuadCount * 6, 1);
            }
         }
      }
   }

   private void renderRhombus(MatrixStack matrixStack, LivingEntity livingEntity, float rhombusTickDelta, float rhombusAlpha, float rhombusColorPhase) {
      this.rhombusFade = MathHelper.lerp(0.22F, this.rhombusFade, livingEntity.hurtTime > 0 ? 0.0F : 1.0F);
      float rhombusHalfSize = (2.0F - rhombusAlpha + 0.3F * this.rhombusFade) / 2.0F;
      Quaternionf quaternionf = new Quaternionf(mc.gameRenderer.getCamera().getRotation())
         .rotateZ((float)Math.toRadians((float)(System.currentTimeMillis() % 3000L) / 3000.0F * 360.0F));
      matrixStack.push();
      matrixStack.translate(
         livingEntity.getLerpedPos(rhombusTickDelta).x - mc.gameRenderer.getCamera().getCameraPos().x,
         livingEntity.getLerpedPos(rhombusTickDelta).y + livingEntity.getHeight() * 0.5F - mc.gameRenderer.getCamera().getCameraPos().y,
         livingEntity.getLerpedPos(rhombusTickDelta).z - mc.gameRenderer.getCamera().getCameraPos().z
      );
      BufferAllocator bufferAllocator = this.getBufferAllocator(1, 8 * VertexFormats.POSITION_TEXTURE_COLOR.getVertexSize());
      BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      Vector3f vector3f = new Vector3f();

      for (float rhombusLayerScale : new float[]{1.0F, 0.5F}) {
         quaternionf.transform(vector3f.set(-rhombusHalfSize, -rhombusHalfSize, 0.0F));
         bufferBuilder.vertex(vector3f.x, vector3f.y, vector3f.z)
            .texture(0.0F, 0.0F)
            .color(Colors.computeInt6(this.themeColor(270.0F, rhombusColorPhase), rhombusAlpha * rhombusLayerScale));
         quaternionf.transform(vector3f.set(-rhombusHalfSize, rhombusHalfSize, 0.0F));
         bufferBuilder.vertex(vector3f.x, vector3f.y, vector3f.z)
            .texture(0.0F, 1.0F)
            .color(Colors.computeInt6(this.themeColor(0.0F, rhombusColorPhase), rhombusAlpha * rhombusLayerScale));
         quaternionf.transform(vector3f.set(rhombusHalfSize, rhombusHalfSize, 0.0F));
         bufferBuilder.vertex(vector3f.x, vector3f.y, vector3f.z)
            .texture(1.0F, 1.0F)
            .color(Colors.computeInt6(this.themeColor(180.0F, rhombusColorPhase), rhombusAlpha * rhombusLayerScale));
         quaternionf.transform(vector3f.set(rhombusHalfSize, -rhombusHalfSize, 0.0F));
         bufferBuilder.vertex(vector3f.x, vector3f.y, vector3f.z)
            .texture(1.0F, 0.0F)
            .color(Colors.computeInt6(this.themeColor(90.0F, rhombusColorPhase), rhombusAlpha * rhombusLayerScale));
      }

      try (BuiltBuffer builtBuffer = bufferBuilder.end()) {
         this.drawQuads(matrixStack, "textures/particles/target.png", builtBuffer, 2, false, false);
      }

      matrixStack.pop();
   }

   @Override
   protected void onDisable() {
      this.livingEntity2 = null;
      this.ghostTime = 0.0F;
      this.chainTime = 0.0F;
      this.animation2.getAnimatedValue(0.0);
      this.effectTime = 0.0F;
      this.effectLastTime = System.currentTimeMillis();
      this.lightningHurt = 0.0F;
      this.circleTime = 0.0F;
      this.circleLastTime = System.currentTimeMillis();
      this.circleHurt = 0.0F;
      this.livingEntity3 = null;
      this.nestedVisibleStart = 0L;
      this.nestedLastSeen = 0L;
      this.rhombusFade = 0.0F;
      Arrays.fill(this.skullTierGlow, 0.0F);
      this.skullLastTime = System.currentTimeMillis();
      if (this.gpuBuffer2 != null && !this.gpuBuffer2.isClosed()) {
         this.gpuBuffer2.close();
      }

      this.gpuBuffer2 = null;

      for (int index = 0; index < this.gpuBufferArray.length; index++) {
         if (this.gpuBufferArray[index] != null && !this.gpuBufferArray[index].isClosed()) {
            this.gpuBufferArray[index].close();
         }

         this.gpuBufferArray[index] = null;
      }

      this.drawBufferCursor = 0;

      for (int index2 = 0; index2 < this.bufferAllocatorArray.length; index2++) {
         if (this.bufferAllocatorArray[index2] != null) {
            this.bufferAllocatorArray[index2].close();
         }

         this.bufferAllocatorArray[index2] = null;
      }
   }

   public static TargetEspModule getInstance() {
      if (targetEspModule == null) {
         targetEspModule = new TargetEspModule();
      }

      return targetEspModule;
   }

   private void renderSkull(MatrixStack matrixStack, LivingEntity livingEntity, float skullTickDelta, float skullAlpha, float skullColorPhase) {
      long skullNow = System.currentTimeMillis();
      float skullDeltaSeconds = (float)Math.min(skullNow - this.skullLastTime, 100L) / 1000.0F;
      this.skullLastTime = skullNow;
      float skullHealth = livingEntity instanceof PlayerEntity ? EntityUtils.getTotalHealth(livingEntity) : livingEntity.getHealth();
      int skullHealthTier = skullHealth < 8.0F ? 2 : (skullHealth < 13.0F ? 1 : 0);

      for (int index = 0; index < this.skullTierGlow.length; index++) {
         this.skullTierGlow[index] = MathHelper.clamp(
            this.skullTierGlow[index] + (index == skullHealthTier ? skullDeltaSeconds : -skullDeltaSeconds) / 0.25F, 0.0F, 1.0F
         );
      }

      float skullIconHalfSize = 0.42F;
      double skullCenterX = MathHelper.lerp(skullTickDelta, livingEntity.lastRenderX, livingEntity.getX()) - mc.gameRenderer.getCamera().getCameraPos().x;
      double skullCenterY = MathHelper.lerp(skullTickDelta, livingEntity.lastRenderY, livingEntity.getY())
         + livingEntity.getHeight() * 0.55F
         - mc.gameRenderer.getCamera().getCameraPos().y;
      double skullCenterZ = MathHelper.lerp(skullTickDelta, livingEntity.lastRenderZ, livingEntity.getZ()) - mc.gameRenderer.getCamera().getCameraPos().z;
      Quaternionf rotation = mc.gameRenderer.getCamera().getRotation();
      int skullBaseColor = Colors.computeInt6(Colors.computeInt2(ThemeColors.intVal9, -49088, skullColorPhase), skullAlpha);
      Vector3f vector3f = new Vector3f();
      BufferBuilder bufferBuilder = new BufferBuilder(
         this.getBufferAllocator(1, 4 * VertexFormats.POSITION_TEXTURE_COLOR.getVertexSize()), DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR
      );
      this.emitBillboard(
         bufferBuilder,
         rotation,
         vector3f,
         skullCenterX,
         skullCenterY,
         skullCenterZ,
         skullIconHalfSize * 1.6F,
         0.0F,
         Colors.computeInt6(skullBaseColor, 0.22F)
      );

      try (BuiltBuffer builtBuffer = bufferBuilder.end()) {
         this.drawQuads(matrixStack, "textures/particles/glow.png", builtBuffer, 1, false, false);
      }

      String[] stringArray = new String[]{"textures/img.png", "textures/img_1.png", "textures/img_2.png"};

      for (int index2 = 0; index2 < stringArray.length; index2++) {
         if (!(this.skullTierGlow[index2] < 0.003921569F)) {
            int skullIconColor = Colors.computeInt6(skullBaseColor, this.skullTierGlow[index2]);
            BufferBuilder bufferBuilder2 = new BufferBuilder(
               this.getBufferAllocator(1, 4 * VertexFormats.POSITION_TEXTURE_COLOR.getVertexSize()), DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR
            );
            rotation.transform(vector3f.set(-skullIconHalfSize, -skullIconHalfSize, 0.0F));
            bufferBuilder2.vertex((float)(skullCenterX + vector3f.x), (float)(skullCenterY + vector3f.y), (float)(skullCenterZ + vector3f.z))
               .texture(0.0F, 1.0F)
               .color(skullIconColor);
            rotation.transform(vector3f.set(-skullIconHalfSize, skullIconHalfSize, 0.0F));
            bufferBuilder2.vertex((float)(skullCenterX + vector3f.x), (float)(skullCenterY + vector3f.y), (float)(skullCenterZ + vector3f.z))
               .texture(0.0F, 0.0F)
               .color(skullIconColor);
            rotation.transform(vector3f.set(skullIconHalfSize, skullIconHalfSize, 0.0F));
            bufferBuilder2.vertex((float)(skullCenterX + vector3f.x), (float)(skullCenterY + vector3f.y), (float)(skullCenterZ + vector3f.z))
               .texture(1.0F, 0.0F)
               .color(skullIconColor);
            rotation.transform(vector3f.set(skullIconHalfSize, -skullIconHalfSize, 0.0F));
            bufferBuilder2.vertex((float)(skullCenterX + vector3f.x), (float)(skullCenterY + vector3f.y), (float)(skullCenterZ + vector3f.z))
               .texture(1.0F, 1.0F)
               .color(skullIconColor);

            try (BuiltBuffer builtBuffer2 = bufferBuilder2.end()) {
               this.drawQuads(matrixStack, stringArray[index2], builtBuffer2, 1, false, false);
            }
         }
      }
   }

   private float hashRandom(int hashSeed, int hashChannel, int hashWaveIndex) {
      int hashValue = hashSeed * 668265261 + hashChannel * 374761393 + hashWaveIndex * -1640531535;
      hashValue ^= hashValue >>> 15;
      hashValue *= -2048144789;
      hashValue ^= hashValue >>> 13;
      return (hashValue >>> 8) / 8388608.0F - 1.0F;
   }

   private TargetEspModule() {
      this.mode = this.addSetting(
         new ModeSetting("Мод", "Какой эффект рисовать на цели.", "Призраки", "Призраки", "Молнии", "Круг", "Кристаллы", "Ромб", "Цепи", "Череп")
      );
      this.ghostStyle = this.addSetting(new ModeSetting("Стиль призраков", "Какими рисовать призраков на цели.", "Второй", "Второй"));
      this.ghostStyle.withVisibility(() -> false);
      this.circleStyle = this.addSetting(new ModeSetting("Стиль круга", "Каким рисовать круг на цели.", "Второй", "Второй"));
      this.circleStyle.withVisibility(() -> false);
      this.trident = this.addSetting(
         new BooleanSetting("Трезубец", "Тянет цепь от руки к летящим трезубцу и стрелам.", true).withVisibility(() -> this.mode.isSelected("Цепи"))
      );
      this.projectiles = this.addSetting(
         new BooleanSetting("Снаряды", "Тянет молнию от руки к летящим трезубцу и стрелам.", true).withVisibility(() -> this.mode.isSelected("Молнии"))
      );
   }

   private void renderProjectileLightning(MatrixStack matrixStack, float plightTickDelta) {
      long plightNow = System.currentTimeMillis();
      float plightDeltaSeconds = (float)Math.min(plightNow - this.effectLastTime, 100L) / 1000.0F;
      this.effectTime += plightDeltaSeconds;
      this.effectLastTime = plightNow;
      this.projectileFade.keySet().removeIf(plightEntityId -> mc.world.getEntityById(plightEntityId) == null);
      int plightWaveIndex = (int)this.effectTime;
      float plightWaveFraction = this.effectTime - plightWaveIndex;
      float plightScroll = this.effectTime * 1.7F - (int)(this.effectTime * 1.7F);
      float plightPlayerX = (float)(
         MathHelper.lerp(plightTickDelta, mc.player.lastRenderX, mc.player.getX()) - mc.gameRenderer.getCamera().getCameraPos().x
      );
      float plightPlayerY = (float)(
         MathHelper.lerp(plightTickDelta, mc.player.lastRenderY, mc.player.getY())
            + mc.player.getStandingEyeHeight() * 0.75F
            - mc.gameRenderer.getCamera().getCameraPos().y
      );
      float plightPlayerZ = (float)(
         MathHelper.lerp(plightTickDelta, mc.player.lastRenderZ, mc.player.getZ()) - mc.gameRenderer.getCamera().getCameraPos().z
      );
      this.ribbonWidthUnit = 0.0037F;
      this.ribbonRadiusUnit = 0.047F;
      this.quaternionf2 = mc.gameRenderer.getCamera().getRotation();

      for (Entity entity : mc.world.getEntities()) {
         if (entity instanceof PersistentProjectileEntity entity2 && entity2.getOwner() == mc.player) {
            boolean plightIsFlying = !entity2.isOnGround() && entity2.getVelocity().lengthSquared() > 1.0E-5;
            float[] floatArray = this.projectileFade.computeIfAbsent(entity2.getId(), plightTargetId -> new float[1]);
            floatArray[0] = MathHelper.clamp(floatArray[0] + (plightIsFlying ? plightDeltaSeconds / 0.15F : -plightDeltaSeconds / 0.3F), 0.0F, 1.0F);
            if (!(floatArray[0] < 0.003921569F)) {
               float plightTargetX = (float)(
                  MathHelper.lerp(plightTickDelta, entity2.lastRenderX, entity2.getX()) - mc.gameRenderer.getCamera().getCameraPos().x
               );
               float plightTargetY = (float)(
                  MathHelper.lerp(plightTickDelta, entity2.lastRenderY, entity2.getY()) - mc.gameRenderer.getCamera().getCameraPos().y
               );
               float plightTargetZ = (float)(
                  MathHelper.lerp(plightTickDelta, entity2.lastRenderZ, entity2.getZ()) - mc.gameRenderer.getCamera().getCameraPos().z
               );
               float plightDeltaX = plightTargetX - plightPlayerX;
               float plightDeltaY = plightTargetY - plightPlayerY;
               float plightDeltaZ = plightTargetZ - plightPlayerZ;
               float plightDistance = MathHelper.sqrt(plightDeltaX * plightDeltaX + plightDeltaY * plightDeltaY + plightDeltaZ * plightDeltaZ);
               if (!(plightDistance < 1.0F)) {
                  float plightDirX = plightDeltaX / plightDistance;
                  float plightDirY = plightDeltaY / plightDistance;
                  float plightDirZ = plightDeltaZ / plightDistance;
                  float plightFlagA = Math.abs(plightDirY) < 0.9F ? 0.0F : 1.0F;
                  float plightFlagB = Math.abs(plightDirY) < 0.9F ? 1.0F : 0.0F;
                  float plightRightX = -plightDirZ * plightFlagB;
                  float plightRightY = plightDirZ * plightFlagA;
                  float plightRightZ = plightDirX * plightFlagB - plightDirY * plightFlagA;
                  float plightRightLen = MathHelper.sqrt(plightRightX * plightRightX + plightRightY * plightRightY + plightRightZ * plightRightZ);
                  if (plightRightLen < 1.0E-5F) {
                     plightRightLen = 1.0F;
                  }

                  plightRightX /= plightRightLen;
                  plightRightY /= plightRightLen;
                  plightRightZ /= plightRightLen;
                  float plightUpX = plightDirY * plightRightZ - plightDirZ * plightRightY;
                  float plightUpY = plightDirZ * plightRightX - plightDirX * plightRightZ;
                  float plightUpZ = plightDirX * plightRightY - plightDirY * plightRightX;
                  float plightIntensity = MathHelper.clamp(0.8F + 0.2F * this.valueNoise(entity2.getId(), 900, plightWaveIndex, plightWaveFraction), 0.0F, 1.0F);
                  int plightSegmentCount = MathHelper.clamp((int)Math.ceil(plightDistance / 2.2F), 2, 24);
                  int plightLineQuads = 0;
                  BufferAllocator bufferAllocator = this.getBufferAllocator(0, 2048 * VertexFormats.POSITION_COLOR.getVertexSize());
                  BufferAllocator bufferAllocator2 = this.getBufferAllocator(1, 2048 * VertexFormats.POSITION_TEXTURE_COLOR.getVertexSize());
                  this.bufferBuilder5 = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION_COLOR);
                  this.bufferBuilder6 = new BufferBuilder(bufferAllocator2, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                  float plightPrevX = plightPlayerX;
                  float plightPrevY = plightPlayerY;
                  float plightPrevZ = plightPlayerZ;

                  for (int index = 1; index <= plightSegmentCount; index++) {
                     float plightSegmentT = (float)index / plightSegmentCount;
                     float plightSegmentWeight = MathHelper.sin((float) Math.PI * plightSegmentT);
                     float plightAngle = plightSegmentT * 7.0F + this.effectTime * 4.0F;
                     float plightSpread = Math.min(0.3F, plightDistance * 0.06F) * plightSegmentWeight;
                     float plightPointX = plightPlayerX
                        + plightDeltaX * plightSegmentT
                        + (plightRightX * MathHelper.sin(plightAngle) + plightUpX * MathHelper.cos(plightAngle)) * plightSpread;
                     float plightPointY = plightPlayerY
                        + plightDeltaY * plightSegmentT
                        + (plightRightY * MathHelper.sin(plightAngle) + plightUpY * MathHelper.cos(plightAngle)) * plightSpread
                        - Math.min(0.6F, plightDistance * 0.035F) * plightSegmentWeight;
                     float plightPointZ = plightPlayerZ
                        + plightDeltaZ * plightSegmentT
                        + (plightRightZ * MathHelper.sin(plightAngle) + plightUpZ * MathHelper.cos(plightAngle)) * plightSpread;
                     float plightScrollDistance = Math.abs((index - 0.5F) / plightSegmentCount - plightScroll);
                     plightScrollDistance = Math.min(plightScrollDistance, 1.0F - plightScrollDistance);
                     float plightGlow = Math.max(0.0F, 1.0F - plightScrollDistance / 0.22F);
                     this.lightningPrimaryColor = Colors.computeInt6(
                        Colors.computeInt7(Colors.computeInt2(UiTheme.intVal7, -1, 0.4F + 0.6F * plightGlow), 255),
                        (0.5F + 0.5F * plightGlow * plightGlow) * plightIntensity * floatArray[0]
                     );
                     this.lightningSecondaryColor = Colors.computeInt6(this.lightningPrimaryColor, 0.13F);
                     plightLineQuads += this.emitLightningArc(
                        plightPrevX,
                        plightPrevY,
                        plightPrevZ,
                        plightPointX,
                        plightPointY,
                        plightPointZ,
                        (entity2.getId() * 131 + index) * 17 | 1,
                        plightWaveIndex,
                        plightWaveFraction,
                        index == plightSegmentCount
                     );
                     plightPrevX = plightPointX;
                     plightPrevY = plightPointY;
                     plightPrevZ = plightPointZ;
                  }

                  int plightGlowQuads = plightLineQuads;
                  this.emitBillboard(
                     this.bufferBuilder6,
                     this.quaternionf2,
                     this.vector3f3,
                     plightTargetX,
                     plightTargetY,
                     plightTargetZ,
                     0.067F + 0.02F * MathHelper.sin(this.effectTime * 9.0F + entity2.getId()),
                     0.0F,
                     Colors.computeInt6(
                        Colors.computeInt7(Colors.computeInt2(UiTheme.intVal7, -1, 0.7F), 255),
                        0.45F * plightIntensity * floatArray[0]
                     )
                  );
                  plightGlowQuads++;
                  boolean plightVisible = RaycastUtils.matchesCriteria(mc.player, entity2);

                  try (BuiltBuffer builtBuffer = this.bufferBuilder6.end()) {
                     this.drawQuads(matrixStack, "textures/particles/glow.png", builtBuffer, plightGlowQuads, plightVisible, false);
                  }

                  if (plightLineQuads > 0) {
                     try (BuiltBuffer builtBuffer2 = this.bufferBuilder5.end()) {
                        this.drawQuads(matrixStack, null, builtBuffer2, plightLineQuads, plightVisible, false);
                     }
                  }
               }
            }
         }
      }
   }

   public record DataRecord(int primaryColor, int secondaryColor, float phase, float alpha, float hit, float glow) {
   }
}
