package greed.client.module.render;

import greed.client.setting.BooleanSetting;
import greed.client.util.Colors;
import greed.client.GreedClient;
import greed.client.util.DepthSnapshot;
import greed.client.event.EventListener;
import greed.client.setting.LegacyNumberSetting;
import greed.client.MinecraftAccess;
import greed.client.setting.ModeSetting;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.setting.MultiSelectSetting;
import greed.client.event.PacketEvent;
import greed.client.event.PlayerTickEvent;
import greed.client.render.WorldProjection;
import greed.client.render.ThemePaletteBuffer;
import greed.client.setting.ColorSetting;
import greed.client.setting.SectionSetting;
import greed.client.event.WorldRenderEvent;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.BufferAllocator.CloseableBuffer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

@ModuleInfo(name = "Particles", description = "Частицы вокруг игрока: метеоры, фигуры и хит эффекты.", category = ModuleCategory.RENDER)
public final class ParticlesModule extends Module {
   private static ParticlesModule particlesModule;
   private static final Vector4f whiteColor = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
   private static final Vector3f zeroOffset = new Vector3f();
   private static final Matrix4f uniformMatrix = new Matrix4f();
   private static final RenderPipeline glowPipeline = RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
      .withLocation(GreedClient.id("pipeline/particles_glow"))
      .withVertexShader(Identifier.ofVanilla("core/position_tex_color"))
      .withFragmentShader(Identifier.ofVanilla("core/position_tex_color"))
      .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS)
      .withBlend(BlendFunction.LIGHTNING)
      .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .withSampler("Sampler0")
      .build();
   private static final RenderPipeline wiresPipeline = RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
      .withLocation(GreedClient.id("pipeline/particles_wires"))
      .withVertexShader(Identifier.ofVanilla("core/position_color"))
      .withFragmentShader(Identifier.ofVanilla("core/position_color"))
      .withVertexFormat(VertexFormats.POSITION_COLOR, DrawMode.QUADS)
      .withBlend(BlendFunction.LIGHTNING)
      .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .build();
   private static final int[] cubeEdgeIndices = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 0, 2, 1, 3, 4, 6, 5, 7, 0, 4, 1, 5, 2, 6, 3, 7};
   private static final int[] pyramidEdgeIndices = new int[]{0, 1, 1, 2, 2, 3, 3, 0, 0, 4, 1, 4, 2, 4, 3, 4};
   private static final int[] octahedronEdgeIndices = new int[]{0, 2, 0, 3, 0, 4, 0, 5, 1, 2, 1, 3, 1, 4, 1, 5, 2, 5, 5, 3, 3, 4, 4, 2};
   private static final int[] tetrahedronEdgeIndices = new int[]{0, 1, 0, 2, 0, 3, 1, 2, 2, 3, 3, 1};
   public MultiSelectSetting effects;
   public LegacyNumberSetting count;
   public LegacyNumberSetting lifetime;
   public LegacyNumberSetting size;
   public LegacyNumberSetting spawnMin;
   public LegacyNumberSetting spawnMax;
   public LegacyNumberSetting shapeHeight;
   public LegacyNumberSetting glow;
   public ModeSetting modeSetting;
   public MultiSelectSetting worldImages;
   public LegacyNumberSetting worldCount;
   public LegacyNumberSetting worldSize;
   public LegacyNumberSetting distanceMin;
   public LegacyNumberSetting distanceMax;
   public LegacyNumberSetting worldHeight;
   public BooleanSetting hitParticles;
   public MultiSelectSetting images;
   public LegacyNumberSetting count2;
   public LegacyNumberSetting hitSize;
   public BooleanSetting onlyCrits;
   public BooleanSetting customColor;
   public ColorSetting colorSetting;
   private final ArrayList<ParticlesModule.SubContext> cubeParticles = new ArrayList<>();
   private final ArrayList<ParticlesModule.SubContext> pyramidParticles = new ArrayList<>();
   private final ArrayList<ParticlesModule.SubContext> totemParticles = new ArrayList<>();
   private final ArrayList<ParticlesModule.SubContext> crescentParticles = new ArrayList<>();
   private final ArrayList<ParticlesModule.SubContext> heartParticles = new ArrayList<>();
   private final ArrayList<ParticlesModule.SubContext> starParticles = new ArrayList<>();
   private final ArrayList<ParticlesModule.SubContext> greedParticles = new ArrayList<>();
   private final ArrayList<ParticlesModule.SubContext4> hitParticleList = new ArrayList<>();
   private final ArrayList<ParticlesModule.SubContext4> worldParticleList = new ArrayList<>();
   private final ConcurrentLinkedQueue<ParticlesModule.DataRecord5> pendingHitParticles = new ConcurrentLinkedQueue<>();
   private long lastUpdateMillis;
   private final HashMap<String, GpuBuffer> textureBufferMap = new HashMap<>();
   private GpuBuffer wiresGpuBuffer;
   private final Matrix3f rotationMatrix = new Matrix3f();
   private final Vector3f[] cubeCornerOffsets = new Vector3f[8];
   private final Vector3f modelVertexA = new Vector3f();
   private final Vector3f modelVertexB = new Vector3f();
   private float[] totemModelData;
   private float[] starModelData;
   private float[] heartModelData;
   private float[] crescentModelData;
   private float[] greedModelData;
   private boolean totemModelLoaded;
   private boolean starModelLoaded;
   private boolean heartModelLoaded;
   private boolean greedModelLoaded;
   private volatile boolean modelsPreloadStarted;
   private BufferAllocator bufferAllocator;
   private final int[] imageOptionCounts;
   private final ArrayList<ParticlesModule.SubContext4> collisionParticleList;
   private final EventListener<PacketEvent> packetListener;
   private final EventListener<PlayerTickEvent> playerTickListener;
   private final EventListener<WorldRenderEvent> worldRenderListener;
   private static final String[] modelNames = new String[]{"Светлячок", "Звезда", "Сердце", "Снежинка", "Доллар"};

   private boolean isCriticalHit() {
      return mc.player != null
         && mc.player.fallDistance > 0.0
         && !mc.player.isOnGround()
         && !mc.player.isClimbing()
         && !mc.player.isTouchingWater()
         && !mc.player.hasVehicle()
         && !mc.player.isSprinting();
   }

   private void drawBillboardQuad(
      BufferBuilder bufferBuilder,
      Quaternionf quaternionf,
      Vector3f vector3f,
      double worldX,
      double worldY,
      double worldZ,
      float halfSize,
      int colorArgb
   ) {
      double relX = worldX - mc.gameRenderer.getCamera().getCameraPos().x;
      double relY = worldY - mc.gameRenderer.getCamera().getCameraPos().y;
      double relZ = worldZ - mc.gameRenderer.getCamera().getCameraPos().z;
      quaternionf.transform(vector3f.set(-halfSize, -halfSize, 0.0F));
      bufferBuilder.vertex((float)(relX + vector3f.x), (float)(relY + vector3f.y), (float)(relZ + vector3f.z))
         .texture(0.0F, 0.0F)
         .color(colorArgb);
      quaternionf.transform(vector3f.set(-halfSize, halfSize, 0.0F));
      bufferBuilder.vertex((float)(relX + vector3f.x), (float)(relY + vector3f.y), (float)(relZ + vector3f.z))
         .texture(0.0F, 1.0F)
         .color(colorArgb);
      quaternionf.transform(vector3f.set(halfSize, halfSize, 0.0F));
      bufferBuilder.vertex((float)(relX + vector3f.x), (float)(relY + vector3f.y), (float)(relZ + vector3f.z))
         .texture(1.0F, 1.0F)
         .color(colorArgb);
      quaternionf.transform(vector3f.set(halfSize, -halfSize, 0.0F));
      bufferBuilder.vertex((float)(relX + vector3f.x), (float)(relY + vector3f.y), (float)(relZ + vector3f.z))
         .texture(1.0F, 0.0F)
         .color(colorArgb);
   }

   private static int findModelIndex(String modelName2) {
      for (int index = 0; index < modelNames.length; index++) {
         if (modelNames[index].equals(modelName2)) {
            return index;
         }
      }

      return -1;
   }

   private static void resolveNodeTranslation(
      JsonArray jsonArray, int nodeIndex, float baseX, float baseY, float baseZ, float[][] floatArrayArray, boolean[] booleanArray
   ) {
      if (nodeIndex >= 0 && nodeIndex < jsonArray.size() && !booleanArray[nodeIndex]) {
         booleanArray[nodeIndex] = true;
         JsonObject asJsonObject = jsonArray.get(nodeIndex).getAsJsonObject();
         float resolvedX = baseX;
         float resolvedY = baseY;
         float resolvedZ = baseZ;
         if (asJsonObject.has("translation")) {
            JsonArray asJsonArray = asJsonObject.getAsJsonArray("translation");
            resolvedX = baseX + asJsonArray.get(0).getAsFloat();
            resolvedY = baseY + asJsonArray.get(1).getAsFloat();
            resolvedZ = baseZ + asJsonArray.get(2).getAsFloat();
         }

         floatArrayArray[nodeIndex][0] = resolvedX;
         floatArrayArray[nodeIndex][1] = resolvedY;
         floatArrayArray[nodeIndex][2] = resolvedZ;
         if (asJsonObject.has("children")) {
            JsonArray asJsonArray2 = asJsonObject.getAsJsonArray("children");

            for (int index = 0; index < asJsonArray2.size(); index++) {
               resolveNodeTranslation(jsonArray, asJsonArray2.get(index).getAsInt(), resolvedX, resolvedY, resolvedZ, floatArrayArray, booleanArray);
            }
         }
      }
   }

   private void updateShapeParticles(long nowMillis) {
      if (this.effects.isSelected("Кубы")
         || this.effects.isSelected("Пирамиды")
         || this.effects.isSelected("Тотем")
         || this.effects.isSelected("Полумесяц")
         || this.effects.isSelected("Сердце")
         || this.effects.isSelected("Звёзды")
         || this.effects.isSelected("Делюкс")) {
         Vec3d vec3d = mc.player.getEntityPos();
         double minSpawnDistance = Math.min(this.spawnMin.getValue(), this.spawnMax.getValue());
         double maxSpawnDistance = Math.max(this.spawnMax.getValue(), minSpawnDistance + 0.1);
         int particleCount = Math.max(1, (int)this.count.getValue());
         if (this.effects.isSelected("Кубы")) {
            this.syncParticleCount(this.cubeParticles, vec3d, minSpawnDistance, maxSpawnDistance, nowMillis, particleCount, 6.0F, 6.0F, 6.0F);
         } else {
            this.cubeParticles.clear();
         }

         if (this.effects.isSelected("Пирамиды")) {
            this.syncParticleCount(this.pyramidParticles, vec3d, minSpawnDistance, maxSpawnDistance, nowMillis, particleCount, 7.0F, 7.0F, 7.0F);
         } else {
            this.pyramidParticles.clear();
         }

         if (this.effects.isSelected("Тотем")) {
            this.syncParticleCount(this.totemParticles, vec3d, minSpawnDistance, maxSpawnDistance, nowMillis, particleCount, 5.0F, 8.0F, 5.0F);
         } else {
            this.totemParticles.clear();
         }

         if (this.effects.isSelected("Полумесяц")) {
            this.syncParticleCount(this.crescentParticles, vec3d, minSpawnDistance, maxSpawnDistance, nowMillis, particleCount, 6.0F, 6.0F, 6.0F);
         } else {
            this.crescentParticles.clear();
         }

         if (this.effects.isSelected("Сердце")) {
            this.syncParticleCount(this.heartParticles, vec3d, minSpawnDistance, maxSpawnDistance, nowMillis, particleCount, 6.0F, 6.0F, 6.0F);
         } else {
            this.heartParticles.clear();
         }

         if (this.effects.isSelected("Звёзды")) {
            this.syncParticleCount(this.starParticles, vec3d, minSpawnDistance, maxSpawnDistance, nowMillis, particleCount, 6.0F, 6.0F, 6.0F);
         } else {
            this.starParticles.clear();
         }

         if (this.effects.isSelected("Делюкс")) {
            this.syncParticleCount(this.greedParticles, vec3d, minSpawnDistance, maxSpawnDistance, nowMillis, particleCount, 6.0F, 6.0F, 6.0F);
         } else {
            this.greedParticles.clear();
         }
      }
   }

   private ParticlesModule() {
      this.imageOptionCounts = new int[modelNames.length];
      this.collisionParticleList = new ArrayList<>();
      this.packetListener = packetEvent -> {
         if (packetEvent.isOutgoing() && packetEvent.getPacket() instanceof HandSwingC2SPacket && this.hitParticles.isChecked()) {
            if (mc.crosshairTarget instanceof EntityHitResult entityHitResult
               && entityHitResult.getEntity() instanceof LivingEntity livingEntity
               && entityHitResult.getEntity() != mc.player) {
               if (!this.onlyCrits.isChecked() || this.isCriticalHit()) {
                  this.pendingHitParticles.add(new ParticlesModule.DataRecord5(livingEntity.getEntityPos(), livingEntity.getHeight()));
               }
            }
         }
      };
      this.playerTickListener = playerTickEvent -> {
         if (mc.player != null && mc.world != null) {
            long tickNowMillis = System.currentTimeMillis();
            this.spawnHitParticles(tickNowMillis);
            this.hitParticleList
               .removeIf(hitParticle -> hitParticle.isExpired(tickNowMillis) || WorldProjection.getVec3d2(hitParticle.position, 1.0F, 1.0F) == null);
            if (this.effects.isSelected("Мир")) {
               this.worldParticleList
                  .removeIf(
                     worldParticle -> worldParticle.isExpired(tickNowMillis)
                        || worldParticle.position.distanceTo(mc.player.getEntityPos()) > this.distanceMax.getValue() + 10.0
                        || WorldProjection.getVec3d2(worldParticle.position, 1.0F, 1.0F) == null
                  );

               for (int index = 0;
                  index < (int)this.worldCount.getValue() && this.worldParticleList.size() < (int)this.worldCount.getValue();
                  index++
               ) {
                  this.spawnWorldParticle(tickNowMillis);
               }
            } else {
               this.worldParticleList.clear();
            }

            for (ParticlesModule.SubContext4 activeParticle : this.hitParticleList) {
               activeParticle.updatePhysics();
            }

            for (ParticlesModule.SubContext4 activeParticle : this.worldParticleList) {
               activeParticle.updatePhysics();
            }

            this.resolveCollisions();
            this.applyPlayerPush(this.hitParticleList);
            if (this.effects.isSelected("Мир")) {
               this.applyPlayerPush(this.worldParticleList);
            }
         }
      };
      this.worldRenderListener = worldRenderEvent -> {
         if (mc.player != null && mc.world != null) {
            long renderNowMillis = System.currentTimeMillis();
            this.removeExpiredShapes(renderNowMillis);
            this.updateShapeParticles(renderNowMillis);
            this.renderGlowParticles(worldRenderEvent.getMatrixStack(), worldRenderEvent.computeFloat(), renderNowMillis);
            this.renderImageParticles(this.hitParticleList, this.images, this.hitSize.getValue(), worldRenderEvent.getMatrixStack(), worldRenderEvent.computeFloat(), renderNowMillis);
            if (this.effects.isSelected("Мир")) {
               this.renderImageParticles(this.worldParticleList, this.worldImages, this.worldSize.getValue(), worldRenderEvent.getMatrixStack(), worldRenderEvent.computeFloat(), renderNowMillis);
            }

            this.renderShapeWires(worldRenderEvent.getMatrixStack(), renderNowMillis);
         }
      };
      this.effects = this.addSetting(
         new MultiSelectSetting(
            "Эффекты", "Какие частицы живут вокруг игрока.", new String[]{"Мир", "Кубы", "Пирамиды", "Тотем", "Полумесяц", "Сердце", "Звёзды", "Грид"}, "Мир"
         )
      );
      this.count = this.addSetting(new LegacyNumberSetting("Количество", "Сколько частиц каждой фигуры держать вокруг.", 14.0, 1.0, 80.0, 1.0));
      this.lifetime = this.addSetting(new LegacyNumberSetting("Жизнь", "Время жизни частицы в миллисекундах.", 1600.0, 500.0, 5000.0, 100.0));
      this.size = this.addSetting(new LegacyNumberSetting("Размер", "Множитель размера фигур.", 0.45, 0.1, 1.5, 0.05));
      this.spawnMin = this.addSetting(new LegacyNumberSetting("Спавн от", "Ближе этой дистанции фигуры не спавнятся.", 4.0, 0.5, 80.0, 0.5));
      this.spawnMax = this.addSetting(new LegacyNumberSetting("Спавн до", "Дальше этой дистанции фигуры не спавнятся.", 10.0, 1.0, 80.0, 0.5));
      this.shapeHeight = this.addSetting(new LegacyNumberSetting("Высота фигур", "Вертикальный разброс спавна фигур вверх и вниз.", 6.0, 0.5, 40.0, 0.5));
      this.glow = this.addSetting(new LegacyNumberSetting("Свечение", "Сила свечения фигур и картинок.", 1.0, 0.0, 3.0, 0.1));
      this.addSetting(new SectionSetting("Мир"));
      this.modeSetting = this.addSetting(new ModeSetting("Спавн мира", "Где живут мировые частицы.", "Небо", "Небо", "Земля"));
      this.worldImages = this.addSetting(
         new MultiSelectSetting(
            "Картинки мира", "Какие картинки летают в мире.", new String[]{"Светлячок", "Звезда", "Сердце", "Снежинка", "Доллар"}, "Светлячок"
         )
      );
      this.worldCount = this.addSetting(new LegacyNumberSetting("Кол-во в мире", "Сколько частиц держать в мире.", 100.0, 10.0, 300.0, 10.0));
      this.worldSize = this.addSetting(new LegacyNumberSetting("Размер в мире", "Размер мировых картинок.", 0.1, 0.03, 0.5, 0.005));
      this.distanceMin = this.addSetting(new LegacyNumberSetting("Дистанция от", "Ближе этой дистанции мировые частицы не спавнятся.", 2.0, 1.0, 100.0, 1.0));
      this.distanceMax = this.addSetting(new LegacyNumberSetting("Дистанция до", "Дальше этой дистанции мировые частицы не спавнятся.", 16.0, 5.0, 120.0, 1.0));
      this.worldHeight = this.addSetting(new LegacyNumberSetting("Высота мира", "Насколько высоко над игроком спавнятся небесные частицы.", 8.0, 1.5, 40.0, 0.5));
      this.addSetting(new SectionSetting("Хит партиклы"));
      this.hitParticles = this.addSetting(new BooleanSetting("Хит партиклы", "Разлетающиеся картинки при ударе по цели.", false));
      this.images = this.addSetting(
         new MultiSelectSetting(
            "Картинки", "Какие картинки вылетают из цели.", new String[]{"Светлячок", "Звезда", "Сердце", "Снежинка", "Доллар"}, "Светлячок"
         )
      );
      this.count2 = this.addSetting(new LegacyNumberSetting("Кол-во", "Сколько картинок спавнить на удар.", 12.0, 1.0, 50.0, 1.0));
      this.hitSize = this.addSetting(new LegacyNumberSetting("Размер удара", "Размер картинок при ударе.", 0.1, 0.03, 0.5, 0.005));
      this.onlyCrits = this.addSetting(new BooleanSetting("Только криты", "Спавнить хит частицы только на критах.", false));
      this.addSetting(new SectionSetting("Цвет"));
      this.customColor = this.addSetting(new BooleanSetting("Свой цвет", "Красить частицы своим цветом вместо темы клиента.", false));
      this.colorSetting = this.addSetting(new ColorSetting("Цвет", "Кастомный цвет частиц."));
      this.glow.withVisibility(() -> this.effects.getSelectedCount() > 0 || this.hitParticles.isChecked());
      this.modeSetting.withVisibility(() -> this.effects.isSelected("Мир"));
      this.worldImages.withVisibility(() -> this.effects.isSelected("Мир"));
      this.worldCount.withVisibility(() -> this.effects.isSelected("Мир"));
      this.worldSize.withVisibility(() -> this.effects.isSelected("Мир"));
      this.distanceMin.withVisibility(() -> this.effects.isSelected("Мир"));
      this.distanceMax.withVisibility(() -> this.effects.isSelected("Мир"));
      this.worldHeight.withVisibility(() -> this.effects.isSelected("Мир") && this.modeSetting.isSelected("Небо"));
      this.images.withVisibility(() -> this.hitParticles.isChecked());
      this.count2.withVisibility(() -> this.hitParticles.isChecked());
      this.hitSize.withVisibility(() -> this.hitParticles.isChecked());
      this.onlyCrits.withVisibility(() -> this.hitParticles.isChecked());
      this.colorSetting.withVisibility(() -> this.customColor.isChecked());

      for (int index2 = 0; index2 < 8; index2++) {
         this.cubeCornerOffsets[index2] = new Vector3f();
      }
   }

   @Override
   protected void onEnable() {
      this.cubeParticles.clear();
      this.pyramidParticles.clear();
      this.totemParticles.clear();
      this.crescentParticles.clear();
      this.heartParticles.clear();
      this.starParticles.clear();
      this.greedParticles.clear();
      this.hitParticleList.clear();
      this.worldParticleList.clear();
      this.pendingHitParticles.clear();
      this.lastUpdateMillis = System.currentTimeMillis();
      if (!this.modelsPreloadStarted) {
         this.modelsPreloadStarted = true;
         Thread thread = new Thread(() -> {
            this.loadTotemModel();
            this.loadStarModel();
            this.loadHeartModel();
            this.loadGreedModel();
            this.loadCrescentModel();
         }, "greed-particles-models");
         thread.setDaemon(true);
         thread.start();
      }
   }

   private ParticlesModule.SubContext createShapeParticle(
      Vec3d vec3d, double minRadius, double maxRadius, long startTimeMillis, float spinX, float spinY, float spinZ
   ) {
      ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();
      double azimuth = threadLocalRandom.nextDouble() * Math.PI * 2.0;
      double spawnRadius = minRadius + threadLocalRandom.nextDouble() * (maxRadius - minRadius);
      double velocityAngle = threadLocalRandom.nextDouble() * Math.PI * 2.0;
      double polarAngle = Math.acos(2.0 * threadLocalRandom.nextDouble() - 1.0);
      double speed = 1.6 + threadLocalRandom.nextDouble() * 1.8;
      return new ParticlesModule.SubContext(
         vec3d.add(
            Math.cos(azimuth) * spawnRadius,
            1.0 + threadLocalRandom.nextDouble(-this.shapeHeight.getValue(), this.shapeHeight.getValue()),
            Math.sin(azimuth) * spawnRadius
         ),
         azimuth,
         startTimeMillis,
         this.randomizedLifetime(this.lifetime.getValue()),
         threadLocalRandom.nextInt(180),
         (threadLocalRandom.nextFloat() - 0.5F) * spinX,
         (threadLocalRandom.nextFloat() - 0.5F) * spinY,
         (threadLocalRandom.nextFloat() - 0.5F) * spinZ,
         Math.sin(polarAngle) * Math.cos(velocityAngle) * speed,
         Math.cos(polarAngle) * speed,
         Math.sin(polarAngle) * Math.sin(velocityAngle) * speed
      );
   }

   private int renderPyramidParticles(BufferBuilder bufferBuilder, long pyramidTimeMillis) {
      float pyramidBaseSize = this.size.getValue() * 0.75F;
      int pyramidVertexCount = 0;

      for (ParticlesModule.SubContext pyramidParticle : this.pyramidParticles) {
         float pyramidProgress = (float)(pyramidTimeMillis - pyramidParticle.particleStartMillis) / (float)pyramidParticle.particleLifetimeMillis;
         if (!(pyramidProgress >= 1.0F)) {
            Vec3d vec3d = pyramidParticle.getVec3d(pyramidProgress);
            if (WorldProjection.matchesCriteria2(vec3d.x, vec3d.y, vec3d.z, 2.0F)) {
               float pyramidHalfSize = pyramidBaseSize * (0.65F + pyramidProgress * 0.55F) * 0.5F;
               this.rotationMatrix
                  .identity()
                  .rotateY(pyramidParticle.rotationSpeedY * pyramidProgress)
                  .rotateX(pyramidParticle.rotationSpeedX * pyramidProgress)
                  .rotateZ(pyramidParticle.rotationSpeedZ * pyramidProgress);
               this.rotationMatrix.transform(this.cubeCornerOffsets[0].set(-pyramidHalfSize, -pyramidHalfSize, -pyramidHalfSize));
               this.rotationMatrix.transform(this.cubeCornerOffsets[1].set(pyramidHalfSize, -pyramidHalfSize, -pyramidHalfSize));
               this.rotationMatrix.transform(this.cubeCornerOffsets[2].set(pyramidHalfSize, -pyramidHalfSize, pyramidHalfSize));
               this.rotationMatrix.transform(this.cubeCornerOffsets[3].set(-pyramidHalfSize, -pyramidHalfSize, pyramidHalfSize));
               this.rotationMatrix.transform(this.cubeCornerOffsets[4].set(0.0F, pyramidHalfSize, 0.0F));
               int pyramidColor = Colors.computeInt6(this.getParticleColor(pyramidParticle.colorSeed), this.fadeInOutAlpha(pyramidProgress));

               for (int index = 0; index < pyramidEdgeIndices.length; index += 2) {
                  pyramidVertexCount += this.drawGlowLine(
                     bufferBuilder,
                     vec3d.x + this.cubeCornerOffsets[pyramidEdgeIndices[index]].x,
                     vec3d.y + this.cubeCornerOffsets[pyramidEdgeIndices[index]].y,
                     vec3d.z + this.cubeCornerOffsets[pyramidEdgeIndices[index]].z,
                     vec3d.x + this.cubeCornerOffsets[pyramidEdgeIndices[index + 1]].x,
                     vec3d.y + this.cubeCornerOffsets[pyramidEdgeIndices[index + 1]].y,
                     vec3d.z + this.cubeCornerOffsets[pyramidEdgeIndices[index + 1]].z,
                     pyramidColor
                  );
               }
            }
         }
      }

      return pyramidVertexCount;
   }

   private static void accumulateEdge(
      HashMap<String, ParticlesModule.SubContext2> edgeMap,
      float x1,
      float y1,
      float z1,
      float x2,
      float y2,
      float z2,
      float normalX,
      float normalY,
      float normalZ
   ) {
      int fixedX1 = Math.round(x1 * 10000.0F);
      int fixedY1 = Math.round(y1 * 10000.0F);
      int fixedZ1 = Math.round(z1 * 10000.0F);
      int fixedX2 = Math.round(x2 * 10000.0F);
      int fixedY2 = Math.round(y2 * 10000.0F);
      int fixedZ2 = Math.round(z2 * 10000.0F);
      boolean reversed = fixedX1 != fixedX2
         ? fixedX1 > fixedX2
         : (fixedY1 != fixedY2 ? fixedY1 > fixedY2 : fixedZ1 > fixedZ2);
      String edgeKey = reversed
         ? fixedX2 + "," + fixedY2 + "," + fixedZ2 + "|" + fixedX1 + "," + fixedY1 + "," + fixedZ1
         : fixedX1 + "," + fixedY1 + "," + fixedZ1 + "|" + fixedX2 + "," + fixedY2 + "," + fixedZ2;
      ParticlesModule.SubContext2 edge = edgeMap.get(edgeKey);
      if (edge == null) {
         edgeMap.put(
            edgeKey,
            new ParticlesModule.SubContext2(x1, y1, z1, x2, y2, z2, normalX, normalY, normalZ)
         );
      } else {
         edge.useCount++;
         if (Math.abs(edge.edgeNormalX * normalX + edge.edgeNormalY * normalY + edge.edgeNormalZ * normalZ) < 0.985F) {
            edge.hasConsistentNormal = false;
         }
      }
   }

   private void drawWires(MatrixStack matrixStack, BufferBuilder bufferBuilder, int vertexCount) {
      BuiltBuffer builtBuffer = bufferBuilder.endNullable();
      if (builtBuffer != null) {
         try (builtBuffer) {
            if (vertexCount != 0 && mc.getFramebuffer() != null && mc.getFramebuffer().getColorAttachmentView() != null) {
               if (this.wiresGpuBuffer != null && !this.wiresGpuBuffer.isClosed() && this.wiresGpuBuffer.size() >= builtBuffer.getBuffer().remaining()) {
                  RenderSystem.getDevice()
                     .createCommandEncoder()
                     .writeToBuffer(this.wiresGpuBuffer.slice(0L, builtBuffer.getBuffer().remaining()), builtBuffer.getBuffer());
               } else {
                  if (this.wiresGpuBuffer != null && !this.wiresGpuBuffer.isClosed()) {
                     this.wiresGpuBuffer.close();
                  }

                  this.wiresGpuBuffer = RenderSystem.getDevice().createBuffer(() -> "greed_particles_wires", 40, builtBuffer.getBuffer());
               }

               GpuBufferSlice gpuBufferSlice = RenderSystem.getDynamicUniforms().write(matrixStack.peek().getPositionMatrix(), whiteColor, zeroOffset, uniformMatrix);

               try (RenderPass renderPass = RenderSystem.getDevice()
                     .createCommandEncoder()
                     .createRenderPass(
                        () -> "greed_particles_wires",
                        mc.getFramebuffer().getColorAttachmentView(),
                        OptionalInt.empty(),
                        DepthSnapshot.getGpuTextureView(),
                        OptionalDouble.empty()
                     )) {
                  renderPass.setPipeline(wiresPipeline);
                  renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
                  renderPass.setUniform("Projection", RenderSystem.getProjectionMatrixBuffer());
                  renderPass.setVertexBuffer(0, this.wiresGpuBuffer);
                  renderPass.setIndexBuffer(
                     RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexBuffer(vertexCount * 6),
                     RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexType()
                  );
                  renderPass.drawIndexed(0, 0, vertexCount * 6, 1);
                  return;
               }
            }
         }
      }
   }

   private synchronized float[] loadTotemModel() {
      if (this.totemModelLoaded) {
         return this.totemModelData;
      }

      this.totemModelLoaded = true;
      this.totemModelData = this.loadObjModel("/assets/greed/models/particles/totem_undying.obj", -0.75F, false);
      return this.totemModelData;
   }

   private synchronized float[] loadGreedModel() {
      if (this.greedModelLoaded) {
         return this.greedModelData;
      }

      this.greedModelLoaded = true;
      InputStream resourceAsStream = ParticlesModule.class.getResourceAsStream("/assets/greed/models/particles/greed.obj");
      if (resourceAsStream == null) {
         return null;
      }

      try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(resourceAsStream, StandardCharsets.UTF_8))) {
         ArrayList<Vector3f> vertices = new ArrayList<>();
         HashMap<String, ParticlesModule.SubContext2> greedEdgeMap = new HashMap<>();
         float modelMinX = Float.POSITIVE_INFINITY;
         float modelMinY = Float.POSITIVE_INFINITY;
         float modelMinZ = Float.POSITIVE_INFINITY;
         float modelMaxX = Float.NEGATIVE_INFINITY;
         float modelMaxY = Float.NEGATIVE_INFINITY;
         float modelMaxZ = Float.NEGATIVE_INFINITY;

         String line;
         while ((line = bufferedReader.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("v ")) {
               String[] stringArray = line.split("\\s+");
               Vector3f vector3f = new Vector3f(Float.parseFloat(stringArray[1]), -Float.parseFloat(stringArray[2]), Float.parseFloat(stringArray[3]));
               vertices.add(vector3f);
               modelMinX = Math.min(modelMinX, vector3f.x);
               modelMaxX = Math.max(modelMaxX, vector3f.x);
               modelMinY = Math.min(modelMinY, vector3f.y);
               modelMaxY = Math.max(modelMaxY, vector3f.y);
               modelMinZ = Math.min(modelMinZ, vector3f.z);
               modelMaxZ = Math.max(modelMaxZ, vector3f.z);
            } else if (line.startsWith("f ")) {
               String[] stringArray2 = line.split("\\s+");
               Vector3f vector3f2 = vertices.get(Integer.parseInt(stringArray2[1].split("/")[0]) - 1);
               Vector3f vector3f3 = vertices.get(Integer.parseInt(stringArray2[2].split("/")[0]) - 1);
               Vector3f vector3f4 = vertices.get(Integer.parseInt(stringArray2[3].split("/")[0]) - 1);
               float normalX2 = (vector3f3.y - vector3f2.y) * (vector3f4.z - vector3f2.z)
                  - (vector3f3.z - vector3f2.z) * (vector3f4.y - vector3f2.y);
               float normalY2 = (vector3f3.z - vector3f2.z) * (vector3f4.x - vector3f2.x)
                  - (vector3f3.x - vector3f2.x) * (vector3f4.z - vector3f2.z);
               float normalZ2 = (vector3f3.x - vector3f2.x) * (vector3f4.y - vector3f2.y)
                  - (vector3f3.y - vector3f2.y) * (vector3f4.x - vector3f2.x);
               float normalLength = (float)Math.sqrt(normalX2 * normalX2 + normalY2 * normalY2 + normalZ2 * normalZ2);
               if (!(normalLength < 1.0E-8F)) {
                  accumulateEdge(
                     greedEdgeMap,
                     vector3f2.x,
                     vector3f2.y,
                     vector3f2.z,
                     vector3f3.x,
                     vector3f3.y,
                     vector3f3.z,
                     normalX2 / normalLength,
                     normalY2 / normalLength,
                     normalZ2 / normalLength
                  );
                  accumulateEdge(
                     greedEdgeMap,
                     vector3f3.x,
                     vector3f3.y,
                     vector3f3.z,
                     vector3f4.x,
                     vector3f4.y,
                     vector3f4.z,
                     normalX2 / normalLength,
                     normalY2 / normalLength,
                     normalZ2 / normalLength
                  );
                  accumulateEdge(
                     greedEdgeMap,
                     vector3f4.x,
                     vector3f4.y,
                     vector3f4.z,
                     vector3f2.x,
                     vector3f2.y,
                     vector3f2.z,
                     normalX2 / normalLength,
                     normalY2 / normalLength,
                     normalZ2 / normalLength
                  );
               }
            }
         }

         float centerX = (modelMinX + modelMaxX) * 0.5F;
         float centerY = (modelMinY + modelMaxY) * 0.5F;
         float centerZ = (modelMinZ + modelMaxZ) * 0.5F;
         float modelScale = Math.max(modelMaxX - modelMinX, Math.max(modelMaxY - modelMinY, modelMaxZ - modelMinZ)) > 1.0E-5F
            ? 1.0F / Math.max(modelMaxX - modelMinX, Math.max(modelMaxY - modelMinY, modelMaxZ - modelMinZ))
            : 1.0F;
         ArrayList<Float> greedModelVertices = new ArrayList<>(greedEdgeMap.size() * 6);

         for (ParticlesModule.SubContext2 greedEdge : greedEdgeMap.values()) {
            if (greedEdge.useCount == 1 || !greedEdge.hasConsistentNormal) {
               greedModelVertices.add((greedEdge.edgeAX - centerX) * modelScale);
               greedModelVertices.add((greedEdge.edgeAY - centerY) * modelScale);
               greedModelVertices.add((greedEdge.edgeAZ - centerZ) * modelScale);
               greedModelVertices.add((greedEdge.edgeBX - centerX) * modelScale);
               greedModelVertices.add((greedEdge.edgeBY - centerY) * modelScale);
               greedModelVertices.add((greedEdge.edgeBZ - centerZ) * modelScale);
            }
         }

         this.greedModelData = new float[greedModelVertices.size()];

         for (int index = 0; index < greedModelVertices.size(); index++) {
            this.greedModelData[index] = greedModelVertices.get(index);
         }
      } catch (Exception exception) {
         this.greedModelData = null;
      }

      return this.greedModelData;
   }

   private void renderShapeWires(MatrixStack matrixStack, long renderTimeMillis) {
      boolean hasShapes = this.effects.isSelected("Кубы") && !this.cubeParticles.isEmpty()
         || this.effects.isSelected("Пирамиды") && !this.pyramidParticles.isEmpty()
         || this.effects.isSelected("Тотем") && !this.totemParticles.isEmpty()
         || this.effects.isSelected("Полумесяц") && !this.crescentParticles.isEmpty()
         || this.effects.isSelected("Сердце") && !this.heartParticles.isEmpty()
         || this.effects.isSelected("Звёзды") && !this.starParticles.isEmpty()
         || this.effects.isSelected("Делюкс") && !this.greedParticles.isEmpty();
      if (hasShapes) {
         BufferBuilder bufferBuilder = new BufferBuilder(this.getBufferAllocator(), DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         int shapeVertexCount = 0;
         if (this.effects.isSelected("Кубы")) {
            shapeVertexCount += this.renderCubeParticles(bufferBuilder, renderTimeMillis);
         }

         if (this.effects.isSelected("Пирамиды")) {
            shapeVertexCount += this.renderPyramidParticles(bufferBuilder, renderTimeMillis);
         }

         if (this.effects.isSelected("Тотем")) {
            shapeVertexCount += this.renderModelShape(this.totemParticles, this.loadTotemModel(), 0.78F, 0.75F, 0.35F, true, bufferBuilder, renderTimeMillis);
         }

         if (this.effects.isSelected("Полумесяц")) {
            shapeVertexCount += this.renderModelShape(this.crescentParticles, this.loadCrescentModel(), 0.72F, 0.7F, 0.45F, false, bufferBuilder, renderTimeMillis);
         }

         if (this.effects.isSelected("Сердце")) {
            shapeVertexCount += this.renderModelShape(this.heartParticles, this.loadHeartModel(), 0.7F, 0.7F, 0.45F, false, bufferBuilder, renderTimeMillis);
         }

         if (this.effects.isSelected("Звёзды")) {
            shapeVertexCount += this.renderModelShape(this.starParticles, this.loadStarModel(), 0.9F, 0.7F, 0.45F, false, bufferBuilder, renderTimeMillis);
         }

         if (this.effects.isSelected("Делюкс")) {
            shapeVertexCount += this.renderModelShape(this.greedParticles, this.loadGreedModel(), 0.9F, 0.7F, 0.45F, false, bufferBuilder, renderTimeMillis);
         }

         this.drawWires(matrixStack, bufferBuilder, shapeVertexCount);
      }
   }

   private void drawTexturedParticles(String texturePath, MatrixStack matrixStack, BufferBuilder bufferBuilder, int vertexCount2) {
      BuiltBuffer builtBuffer = bufferBuilder.endNullable();
      if (builtBuffer != null) {
         try (builtBuffer) {
            if (vertexCount2 != 0
               && mc.getTextureManager().getTexture(Identifier.of("greed", texturePath)).getGlTextureView() != null
               && mc.getFramebuffer() != null
               && mc.getFramebuffer().getColorAttachmentView() != null) {
               GpuBuffer gpuBuffer = this.textureBufferMap.get(texturePath);
               if (gpuBuffer != null && !gpuBuffer.isClosed() && gpuBuffer.size() >= builtBuffer.getBuffer().remaining()) {
                  RenderSystem.getDevice()
                     .createCommandEncoder()
                     .writeToBuffer(gpuBuffer.slice(0L, builtBuffer.getBuffer().remaining()), builtBuffer.getBuffer());
               } else {
                  if (gpuBuffer != null && !gpuBuffer.isClosed()) {
                     gpuBuffer.close();
                  }

                  gpuBuffer = RenderSystem.getDevice().createBuffer(() -> "greed_particles_" + texturePath, 40, builtBuffer.getBuffer());
                  this.textureBufferMap.put(texturePath, gpuBuffer);
               }

               GpuBufferSlice gpuBufferSlice = RenderSystem.getDynamicUniforms().write(matrixStack.peek().getPositionMatrix(), whiteColor, zeroOffset, uniformMatrix);

               try (RenderPass renderPass = RenderSystem.getDevice()
                     .createCommandEncoder()
                     .createRenderPass(
                        () -> "greed_particles",
                        mc.getFramebuffer().getColorAttachmentView(),
                        OptionalInt.empty(),
                        DepthSnapshot.getGpuTextureView(),
                        OptionalDouble.empty()
                     )) {
                  renderPass.setPipeline(glowPipeline);
                  renderPass.bindTexture(
                     "Sampler0",
                     mc.getTextureManager().getTexture(Identifier.of("greed", texturePath)).getGlTextureView(),
                     RenderSystem.getSamplerCache().get(FilterMode.LINEAR)
                  );
                  renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
                  renderPass.setUniform("Projection", RenderSystem.getProjectionMatrixBuffer());
                  renderPass.setVertexBuffer(0, gpuBuffer);
                  renderPass.setIndexBuffer(
                     RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexBuffer(vertexCount2 * 6),
                     RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexType()
                  );
                  renderPass.drawIndexed(0, 0, vertexCount2 * 6, 1);
                  return;
               }
            }
         }
      }
   }

   private BufferAllocator getBufferAllocator() {
      if (this.bufferAllocator == null) {
         this.bufferAllocator = new BufferAllocator(65536);
      }

      CloseableBuffer allocated = this.bufferAllocator.getAllocated();
      if (allocated != null) {
         allocated.close();
      }

      return this.bufferAllocator;
   }

   private synchronized float[] loadCrescentModel() {
      if (this.crescentModelData != null) {
         return this.crescentModelData;
      }

      float outerRadius = 0.5F;
      float innerRadius = 0.42F;
      float arcOffsetX = 0.3F;
      float maxThickness = 0.13F;
      float minThickness = 0.022F;
      float arcCenter = (outerRadius * outerRadius - innerRadius * innerRadius + arcOffsetX * arcOffsetX) / (2.0F * arcOffsetX);
      float chordHalfLength = (float)Math.sqrt(Math.max(0.0F, outerRadius * outerRadius - arcCenter * arcCenter));
      float outerStartAngle = (float)Math.atan2(chordHalfLength, arcCenter);
      float innerStartAngle = (float)Math.atan2(chordHalfLength, arcCenter - arcOffsetX);
      int segmentCount = 18;
      int ringStep = 3;
      float centerOffset = (outerRadius - arcCenter) * 0.5F;
      float outerArcSpan = (float) (Math.PI * 2) - 2.0F * outerStartAngle;
      float innerArcSpan = (float) (Math.PI * 2) - 2.0F * innerStartAngle;
      float[] floatArray = new float[segmentCount + 1];
      float[] floatArray2 = new float[segmentCount + 1];
      float[] floatArray3 = new float[segmentCount + 1];
      float[] floatArray4 = new float[segmentCount + 1];
      float[] floatArray5 = new float[segmentCount + 1];
      float[] floatArray6 = new float[segmentCount + 1];

      for (int index = 0; index <= segmentCount; index++) {
         float outerSegmentAngle = outerStartAngle + outerArcSpan / segmentCount * index;
         floatArray[index] = (float)Math.cos(outerSegmentAngle) * outerRadius + centerOffset;
         floatArray2[index] = (float)Math.sin(outerSegmentAngle) * outerRadius;
         floatArray3[index] = minThickness + (maxThickness - minThickness) * (float)Math.sin((outerSegmentAngle - outerStartAngle) / outerArcSpan * Math.PI);
         float innerSegmentAngle = innerStartAngle + innerArcSpan / segmentCount * index;
         floatArray4[index] = (float)Math.cos(innerSegmentAngle) * innerRadius + arcOffsetX + centerOffset;
         floatArray5[index] = (float)Math.sin(innerSegmentAngle) * innerRadius;
         floatArray6[index] = minThickness + (maxThickness - minThickness) * (float)Math.sin((innerSegmentAngle - innerStartAngle) / innerArcSpan * Math.PI);
      }

      ArrayList<Float> vertexData = new ArrayList<>(segmentCount * 28 + 64);

      for (int index2 = 0; index2 < segmentCount; index2++) {
         vertexData.add(floatArray[index2]);
         vertexData.add(floatArray2[index2]);
         vertexData.add(floatArray3[index2]);
         vertexData.add(floatArray[index2 + 1]);
         vertexData.add(floatArray2[index2 + 1]);
         vertexData.add(floatArray3[index2 + 1]);
         vertexData.add(floatArray[index2]);
         vertexData.add(floatArray2[index2]);
         vertexData.add(-floatArray3[index2]);
         vertexData.add(floatArray[index2 + 1]);
         vertexData.add(floatArray2[index2 + 1]);
         vertexData.add(-floatArray3[index2 + 1]);
      }

      for (int index3 = 0; index3 < segmentCount; index3++) {
         vertexData.add(floatArray4[index3]);
         vertexData.add(floatArray5[index3]);
         vertexData.add(floatArray6[index3]);
         vertexData.add(floatArray4[index3 + 1]);
         vertexData.add(floatArray5[index3 + 1]);
         vertexData.add(floatArray6[index3 + 1]);
         vertexData.add(floatArray4[index3]);
         vertexData.add(floatArray5[index3]);
         vertexData.add(-floatArray6[index3]);
         vertexData.add(floatArray4[index3 + 1]);
         vertexData.add(floatArray5[index3 + 1]);
         vertexData.add(-floatArray6[index3 + 1]);
      }

      for (int index4 = 0; index4 <= segmentCount; index4 += ringStep) {
         vertexData.add(floatArray[index4]);
         vertexData.add(floatArray2[index4]);
         vertexData.add(floatArray3[index4]);
         vertexData.add(floatArray[index4]);
         vertexData.add(floatArray2[index4]);
         vertexData.add(-floatArray3[index4]);
      }

      for (int index5 = ringStep; index5 < segmentCount; index5 += ringStep) {
         vertexData.add(floatArray4[index5]);
         vertexData.add(floatArray5[index5]);
         vertexData.add(floatArray6[index5]);
         vertexData.add(floatArray4[index5]);
         vertexData.add(floatArray5[index5]);
         vertexData.add(-floatArray6[index5]);
      }

      this.crescentModelData = new float[vertexData.size()];

      for (int index6 = 0; index6 < vertexData.size(); index6++) {
         this.crescentModelData[index6] = vertexData.get(index6);
      }

      return this.crescentModelData;
   }

   private void resolveCollisions() {
      ArrayList<ParticlesModule.SubContext4> collisions = this.collisionParticleList;
      collisions.clear();
      collisions.addAll(this.hitParticleList);
      if (this.effects.isSelected("Мир")) {
         collisions.addAll(this.worldParticleList);
      }

      int outerIndex = 0;

      for (int index = collisions.size(); outerIndex < index; outerIndex++) {
         ParticlesModule.SubContext4 particleA = collisions.get(outerIndex);

         for (int index2 = outerIndex + 1; index2 < index; index2++) {
            ParticlesModule.SubContext4 particleB = collisions.get(index2);
            double minDistance = particleA.radius + particleB.radius;
            double deltaX = particleB.position.x - particleA.position.x;
            if (!(deltaX >= minDistance) && !(-deltaX >= minDistance)) {
               double deltaY = particleB.position.y - particleA.position.y;
               if (!(deltaY >= minDistance) && !(-deltaY >= minDistance)) {
                  double deltaZ = particleB.position.z - particleA.position.z;
                  if (!(deltaZ >= minDistance) && !(-deltaZ >= minDistance)) {
                     double distanceSq = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
                     if (!(distanceSq >= minDistance * minDistance) && !(distanceSq < 1.0E-8)) {
                        Vec3d vec3d = new Vec3d(deltaX, deltaY, deltaZ);
                        double distance = Math.sqrt(distanceSq);
                        Vec3d vec3d2 = vec3d.multiply(1.0 / distance);
                        particleA.position = particleA.position.subtract(vec3d2.multiply((minDistance - distance) * 0.5));
                        particleB.position = particleB.position.add(vec3d2.multiply((minDistance - distance) * 0.5));
                        double approachSpeed = particleB.velocity.subtract(particleA.velocity).dotProduct(vec3d2);
                        if (approachSpeed < 0.0) {
                           particleA.velocity = particleA.velocity.subtract(vec3d2.multiply(-approachSpeed * 0.45));
                           particleB.velocity = particleB.velocity.add(vec3d2.multiply(-approachSpeed * 0.45));
                        }
                     }
                  }
               }
            }
         }
      }

      collisions.clear();
   }

   private void renderGlowParticles(MatrixStack matrixStack, float tickDelta, long renderTimeMillis2) {
      boolean hasHitGlow = this.glow.getValue() > 0.0F && this.hitParticles.isChecked() && !this.hitParticleList.isEmpty();
      boolean hasWorldGlow = this.glow.getValue() > 0.0F && this.effects.isSelected("Мир") && !this.worldParticleList.isEmpty();
      boolean hasShapeGlow = this.glow.getValue() > 0.0F
         && (
            this.effects.isSelected("Кубы") && !this.cubeParticles.isEmpty()
               || this.effects.isSelected("Пирамиды") && !this.pyramidParticles.isEmpty()
               || this.effects.isSelected("Тотем") && !this.totemParticles.isEmpty()
               || this.effects.isSelected("Полумесяц") && !this.crescentParticles.isEmpty()
               || this.effects.isSelected("Сердце") && !this.heartParticles.isEmpty()
               || this.effects.isSelected("Звёзды") && !this.starParticles.isEmpty()
         );
      if (hasWorldGlow || hasShapeGlow || hasHitGlow) {
         Quaternionf quaternionf = new Quaternionf(mc.gameRenderer.getCamera().getRotation()).rotateY((float) Math.PI);
         Vector3f vector3f = new Vector3f();
         BufferBuilder bufferBuilder = new BufferBuilder(this.getBufferAllocator(), DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
         int glowVertexCount = 0;
         if (hasShapeGlow) {
            if (this.effects.isSelected("Кубы")) {
               glowVertexCount += this.renderShapeParticles(this.cubeParticles, this.size.getValue() * 1.7F, false, bufferBuilder, quaternionf, vector3f, renderTimeMillis2);
            }

            if (this.effects.isSelected("Пирамиды")) {
               glowVertexCount += this.renderShapeParticles(this.pyramidParticles, this.size.getValue() * 1.7F, false, bufferBuilder, quaternionf, vector3f, renderTimeMillis2);
            }

            if (this.effects.isSelected("Тотем")) {
               glowVertexCount += this.renderShapeParticles(this.totemParticles, this.size.getValue() * 2.0F, true, bufferBuilder, quaternionf, vector3f, renderTimeMillis2);
            }

            if (this.effects.isSelected("Полумесяц")) {
               glowVertexCount += this.renderShapeParticles(this.crescentParticles, this.size.getValue() * 1.8F, false, bufferBuilder, quaternionf, vector3f, renderTimeMillis2);
            }

            if (this.effects.isSelected("Сердце")) {
               glowVertexCount += this.renderShapeParticles(this.heartParticles, this.size.getValue() * 1.8F, false, bufferBuilder, quaternionf, vector3f, renderTimeMillis2);
            }

            if (this.effects.isSelected("Звёзды")) {
               glowVertexCount += this.renderShapeParticles(this.starParticles, this.size.getValue() * 1.8F, false, bufferBuilder, quaternionf, vector3f, renderTimeMillis2);
            }
         }

         if (hasHitGlow) {
            glowVertexCount += this.renderHitParticles(this.hitParticleList, this.hitSize.getValue() / 1.5F, tickDelta, bufferBuilder, quaternionf, vector3f, renderTimeMillis2);
         }

         if (hasWorldGlow) {
            glowVertexCount += this.renderHitParticles(this.worldParticleList, this.worldSize.getValue(), tickDelta, bufferBuilder, quaternionf, vector3f, renderTimeMillis2);
         }

         this.drawTexturedParticles("textures/particles/glowboost.png", matrixStack, bufferBuilder, glowVertexCount);
      }
   }

   private int renderShapeParticles(
      ArrayList<ParticlesModule.SubContext> shapeParticles,
      float baseSize,
      boolean wavy,
      BufferBuilder bufferBuilder,
      Quaternionf quaternionf,
      Vector3f vector3f,
      long timeMillisArg
   ) {
      int vertexCount3 = 0;

      for (ParticlesModule.SubContext shapeParticle : shapeParticles) {
         float progress = (float)(timeMillisArg - shapeParticle.particleStartMillis) / (float)shapeParticle.particleLifetimeMillis;
         if (!(progress >= 1.0F)) {
            Vec3d vec3d = wavy ? shapeParticle.getVec3d2(progress) : shapeParticle.getVec3d(progress);
            if (WorldProjection.matchesCriteria2(vec3d.x, vec3d.y, vec3d.z, 2.0F)) {
               int shapeColor = this.getParticleColor(shapeParticle.colorSeed);
               float flicker = 0.85F + 0.15F * MathHelper.sin((float)shapeParticle.phase * 3.0F + progress * 6.0F);
               this.drawBillboardQuad(
                  bufferBuilder,
                  quaternionf,
                  vector3f,
                  vec3d.x,
                  vec3d.y,
                  vec3d.z,
                  baseSize * (0.95F + progress * 0.25F) * flicker,
                  Colors.computeInt6(shapeColor, this.fadeInOutAlpha(progress) * 0.22F * this.glow.getValue())
               );
               this.drawBillboardQuad(
                  bufferBuilder,
                  quaternionf,
                  vector3f,
                  vec3d.x,
                  vec3d.y,
                  vec3d.z,
                  baseSize * 0.55F * (0.9F + progress * 0.2F) * flicker,
                  Colors.computeInt6(shapeColor, this.fadeInOutAlpha(progress) * 0.55F * Math.min(1.0F, this.glow.getValue()))
               );
               vertexCount3 += 2;
            }
         }
      }

      return vertexCount3;
   }

   private boolean hasLineOfSight(Vec3d vec3d) {
      if (mc.getCameraEntity() == null) {
         return false;
      } else {
         Vec3d cameraPos = mc.gameRenderer.getCamera().getCameraPos();
         HitResult hitResult = mc.world
            .raycast(new RaycastContext(cameraPos, vec3d, ShapeType.COLLIDER, FluidHandling.NONE, mc.getCameraEntity()));
         if (hitResult.getType() == Type.BLOCK && hitResult.getPos().squaredDistanceTo(cameraPos) < vec3d.squaredDistanceTo(cameraPos) - 0.001) {
            return false;
         } else {
            return mc.player.getBoundingBox().expand(0.15).raycast(cameraPos, vec3d).isPresent()
               ? false
               : mc.world
                  .getOtherEntities(
                     mc.getCameraEntity(),
                     mc.getCameraEntity().getBoundingBox().stretch(vec3d.subtract(cameraPos)).expand(0.3),
                     entity -> entity.getBoundingBox().expand(0.1).raycast(cameraPos, vec3d).isPresent()
                  )
                  .isEmpty();
         }
      }
   }

   public static ParticlesModule getInstance() {
      if (particlesModule == null) {
         particlesModule = new ParticlesModule();
      }

      return particlesModule;
   }

   private int renderHitParticles(
      ArrayList<ParticlesModule.SubContext4> hitGlowParticles,
      float baseSize2,
      float tickDelta2,
      BufferBuilder bufferBuilder,
      Quaternionf quaternionf,
      Vector3f vector3f,
      long timeMillisArg2
   ) {
      int vertexCount4 = 0;

      for (ParticlesModule.SubContext4 hitParticleElement : hitGlowParticles) {
         float alpha = hitParticleElement.getAlpha(timeMillisArg2);
         if (!(alpha <= 0.0F)) {
            Vec3d vec3d = hitParticleElement.previousPosition.lerp(hitParticleElement.position, tickDelta2);
            if (WorldProjection.matchesCriteria2(vec3d.x, vec3d.y, vec3d.z, 2.0F)) {
               int hitColor = this.getParticleColor(hitParticleElement.hitColorSeed);
               float flicker2 = 0.85F + 0.15F * MathHelper.sin(hitParticleElement.hitColorSeed + hitParticleElement.getFadeProgress(timeMillisArg2) * 6.0F);
               this.drawBillboardQuad(
                  bufferBuilder,
                  quaternionf,
                  vector3f,
                  vec3d.x,
                  vec3d.y,
                  vec3d.z,
                  baseSize2 * 1.9F * flicker2,
                  Colors.computeInt6(hitColor, alpha * 0.22F * this.glow.getValue())
               );
               this.drawBillboardQuad(
                  bufferBuilder,
                  quaternionf,
                  vector3f,
                  vec3d.x,
                  vec3d.y,
                  vec3d.z,
                  baseSize2 * 1.1F * flicker2,
                  Colors.computeInt6(hitColor, alpha * 0.55F * Math.min(1.0F, this.glow.getValue()))
               );
               vertexCount4 += 2;
            }
         }
      }

      return vertexCount4;
   }

   private float fadeInOutAlpha(float progress2) {
      if (progress2 < 0.15F) {
         return progress2 / 0.15F;
      } else {
         return progress2 > 0.75F ? Math.max(0.0F, (1.0F - progress2) / 0.25F) : 1.0F;
      }
   }

   @Override
   protected void onDisable() {
      this.cubeParticles.clear();
      this.pyramidParticles.clear();
      this.totemParticles.clear();
      this.crescentParticles.clear();
      this.heartParticles.clear();
      this.starParticles.clear();
      this.greedParticles.clear();
      this.hitParticleList.clear();
      this.worldParticleList.clear();
      this.pendingHitParticles.clear();

      for (GpuBuffer gpuBuffer : this.textureBufferMap.values()) {
         if (!gpuBuffer.isClosed()) {
            gpuBuffer.close();
         }
      }

      this.textureBufferMap.clear();
      if (this.wiresGpuBuffer != null && !this.wiresGpuBuffer.isClosed()) {
         this.wiresGpuBuffer.close();
      }

      this.wiresGpuBuffer = null;
      if (this.bufferAllocator != null) {
         this.bufferAllocator.close();
         this.bufferAllocator = null;
      }

      this.collisionParticleList.clear();
   }

   private synchronized float[] loadStarModel() {
      if (this.starModelLoaded) {
         return this.starModelData;
      }

      this.starModelLoaded = true;
      this.starModelData = this.loadObjModel("/assets/greed/models/particles/fire_nether_star.obj", 0.0F, true);
      return this.starModelData;
   }

   private int drawGlowLine(
      BufferBuilder bufferBuilder,
      double startX,
      double startY,
      double startZ,
      double endX,
      double endY,
      double endZ,
      int lineColor
   ) {
      float relStartX = (float)(startX - mc.gameRenderer.getCamera().getCameraPos().x);
      float relStartY = (float)(startY - mc.gameRenderer.getCamera().getCameraPos().y);
      float relStartZ = (float)(startZ - mc.gameRenderer.getCamera().getCameraPos().z);
      float relEndX = (float)(endX - mc.gameRenderer.getCamera().getCameraPos().x);
      float relEndY = (float)(endY - mc.gameRenderer.getCamera().getCameraPos().y);
      float relEndZ = (float)(endZ - mc.gameRenderer.getCamera().getCameraPos().z);
      float dirX = relEndX - relStartX;
      float dirY = relEndY - relStartY;
      float dirZ = relEndZ - relStartZ;
      float midX = (relStartX + relEndX) * 0.5F;
      float midY = (relStartY + relEndY) * 0.5F;
      float midZ = (relStartZ + relEndZ) * 0.5F;
      float perpX = dirY * midZ - dirZ * midY;
      float perpY = dirZ * midX - dirX * midZ;
      float perpZ = dirX * midY - dirY * midX;
      float perpLength = MathHelper.sqrt(perpX * perpX + perpY * perpY + perpZ * perpZ);
      if (perpLength < 1.0E-6F) {
         return 0;
      }

      int quadCount = 0;
      if (this.glow.getValue() > 0.05F) {
         float halfWidth = 0.0256F / perpLength;
         int glowColor = Colors.computeInt6(lineColor, 0.22F + 0.1F * this.glow.getValue());
         bufferBuilder.vertex(relStartX - perpX * halfWidth, relStartY - perpY * halfWidth, relStartZ - perpZ * halfWidth)
            .color(glowColor);
         bufferBuilder.vertex(relStartX + perpX * halfWidth, relStartY + perpY * halfWidth, relStartZ + perpZ * halfWidth)
            .color(glowColor);
         bufferBuilder.vertex(relEndX + perpX * halfWidth, relEndY + perpY * halfWidth, relEndZ + perpZ * halfWidth)
            .color(glowColor);
         bufferBuilder.vertex(relEndX - perpX * halfWidth, relEndY - perpY * halfWidth, relEndZ - perpZ * halfWidth)
            .color(glowColor);
         quadCount++;
      }

      float halfWidth = 0.008F / perpLength;
      bufferBuilder.vertex(relStartX - perpX * halfWidth, relStartY - perpY * halfWidth, relStartZ - perpZ * halfWidth)
         .color(lineColor);
      bufferBuilder.vertex(relStartX + perpX * halfWidth, relStartY + perpY * halfWidth, relStartZ + perpZ * halfWidth)
         .color(lineColor);
      bufferBuilder.vertex(relEndX + perpX * halfWidth, relEndY + perpY * halfWidth, relEndZ + perpZ * halfWidth)
         .color(lineColor);
      bufferBuilder.vertex(relEndX - perpX * halfWidth, relEndY - perpY * halfWidth, relEndZ - perpZ * halfWidth)
         .color(lineColor);
      return quadCount + 1;
   }

   private long randomizedLifetime(float baseLifetime) {
      return Math.max(50L, Math.round(baseLifetime * (0.75F + ThreadLocalRandom.current().nextFloat() * 0.5F)));
   }

   private void removeExpiredShapes(long purgeNowMillis) {
      this.cubeParticles.removeIf(cubeExpired -> purgeNowMillis - cubeExpired.particleStartMillis > cubeExpired.particleLifetimeMillis);
      this.pyramidParticles.removeIf(pyramidExpired -> purgeNowMillis - pyramidExpired.particleStartMillis > pyramidExpired.particleLifetimeMillis);
      this.totemParticles.removeIf(totemExpired -> purgeNowMillis - totemExpired.particleStartMillis > totemExpired.particleLifetimeMillis);
      this.crescentParticles.removeIf(crescentExpired -> purgeNowMillis - crescentExpired.particleStartMillis > crescentExpired.particleLifetimeMillis);
      this.heartParticles.removeIf(heartExpired -> purgeNowMillis - heartExpired.particleStartMillis > heartExpired.particleLifetimeMillis);
      this.starParticles.removeIf(starExpired -> purgeNowMillis - starExpired.particleStartMillis > starExpired.particleLifetimeMillis);
      this.greedParticles.removeIf(greedExpired -> purgeNowMillis - greedExpired.particleStartMillis > greedExpired.particleLifetimeMillis);
   }

   private void applyPlayerPush(ArrayList<ParticlesModule.SubContext4> particles3) {
      Vec3d vec3d = mc.player.getEntityPos().subtract(mc.player.lastRenderX, mc.player.lastRenderY, mc.player.lastRenderZ);

      for (ParticlesModule.SubContext4 hitParticleElement2 : particles3) {
         double clampedX = MathHelper.clamp(hitParticleElement2.position.x, mc.player.getBoundingBox().minX, mc.player.getBoundingBox().maxX);
         double clampedY = MathHelper.clamp(hitParticleElement2.position.y, mc.player.getBoundingBox().minY, mc.player.getBoundingBox().maxY);
         double clampedZ = MathHelper.clamp(hitParticleElement2.position.z, mc.player.getBoundingBox().minZ, mc.player.getBoundingBox().maxZ);
         Vec3d vec3d2 = hitParticleElement2.position.subtract(clampedX, clampedY, clampedZ);
         double distanceSq2 = vec3d2.lengthSquared();
         if (!(distanceSq2 >= hitParticleElement2.radius * hitParticleElement2.radius)) {
            double distance2 = Math.sqrt(Math.max(distanceSq2, 1.0E-8));
            Vec3d vec3d3 = distance2 < 1.0E-4
               ? hitParticleElement2.position.subtract(mc.player.getEntityPos())
               : vec3d2.multiply(1.0 / distance2);
            vec3d3 = vec3d3.lengthSquared() < 1.0E-8 ? new Vec3d(0.0, 1.0, 0.0) : vec3d3.normalize();
            hitParticleElement2.position = hitParticleElement2.position.add(vec3d3.multiply(hitParticleElement2.radius - distance2 + 0.01));
            hitParticleElement2.velocity = hitParticleElement2.velocity
               .add(vec3d3.multiply(0.035 + Math.min(vec3d.length() * 0.8, 0.25)))
               .add(vec3d.x * 0.4, Math.max(vec3d.y * 0.2, 0.0), vec3d.z * 0.4);
         }
      }
   }

   private int renderModelShape(
      ArrayList<ParticlesModule.SubContext> particles4,
      float[] floatArray,
      float sizeMultiplier,
      float baseScale,
      float scaleGrowth,
      boolean wavy2,
      BufferBuilder bufferBuilder,
      long timeMillisArg3
   ) {
      if (floatArray != null && floatArray.length != 0) {
         float scaledSize = this.size.getValue() * sizeMultiplier;
         int vertexCount5 = 0;

         for (ParticlesModule.SubContext shapeParticle2 : particles4) {
            float progress3 = (float)(timeMillisArg3 - shapeParticle2.particleStartMillis) / (float)shapeParticle2.particleLifetimeMillis;
            if (!(progress3 >= 1.0F)) {
               Vec3d vec3d = wavy2 ? shapeParticle2.getVec3d2(progress3) : shapeParticle2.getVec3d(progress3);
               if (WorldProjection.matchesCriteria2(vec3d.x, vec3d.y, vec3d.z, 2.0F)) {
                  float particleScale = scaledSize * (baseScale + progress3 * scaleGrowth);
                  this.rotationMatrix
                     .identity()
                     .rotateY(shapeParticle2.rotationSpeedY * progress3)
                     .rotateX(shapeParticle2.rotationSpeedX * progress3)
                     .rotateZ(shapeParticle2.rotationSpeedZ * progress3);
                  int color3 = Colors.computeInt6(this.getParticleColor(shapeParticle2.colorSeed), this.fadeInOutAlpha(progress3));

                  for (int index = 0; index < floatArray.length; index += 6) {
                     this.rotationMatrix
                        .transform(
                           this.modelVertexA
                              .set(floatArray[index] * particleScale, floatArray[index + 1] * particleScale, floatArray[index + 2] * particleScale)
                        );
                     this.rotationMatrix
                        .transform(
                           this.modelVertexB
                              .set(
                                 floatArray[index + 3] * particleScale,
                                 floatArray[index + 4] * particleScale,
                                 floatArray[index + 5] * particleScale
                              )
                        );
                     vertexCount5 += this.drawGlowLine(
                        bufferBuilder,
                        vec3d.x + this.modelVertexA.x,
                        vec3d.y + this.modelVertexA.y,
                        vec3d.z + this.modelVertexA.z,
                        vec3d.x + this.modelVertexB.x,
                        vec3d.y + this.modelVertexB.y,
                        vec3d.z + this.modelVertexB.z,
                        color3
                     );
                  }
               }
            }
         }

         return vertexCount5;
      } else {
         return 0;
      }
   }

   private void syncParticleCount(
      ArrayList<ParticlesModule.SubContext> particles5,
      Vec3d vec3d,
      double minRadius2,
      double maxRadius2,
      long startTimeMillis2,
      int targetCount,
      float spinX2,
      float spinY2,
      float spinZ2
   ) {
      while (particles5.size() > targetCount) {
         particles5.remove(0);
      }

      while (particles5.size() < targetCount) {
         particles5.add(this.createShapeParticle(vec3d, minRadius2, maxRadius2, startTimeMillis2, spinX2, spinY2, spinZ2));
      }
   }

   private void spawnWorldParticle(long spawnTimeMillis) {
      ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();
      double minDistance2 = Math.min(this.distanceMin.getValue(), this.distanceMax.getValue());
      double distance3 = minDistance2 + threadLocalRandom.nextDouble() * Math.max(0.1, this.distanceMax.getValue() - minDistance2);
      double angleRadians = Math.toRadians(mc.player.getYaw() + threadLocalRandom.nextDouble(-80.0, 80.0));
      double spawnX = mc.player.getX() - Math.sin(angleRadians) * distance3;
      double spawnZ = mc.player.getZ() + Math.cos(angleRadians) * distance3;
      if (this.modeSetting.isSelected("Земля")) {
         BlockPos blockPos = mc.world
            .getTopPosition(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING, BlockPos.ofFloored(spawnX, mc.player.getY(), spawnZ));
         Vec3d vec3d = new Vec3d(
            spawnX + threadLocalRandom.nextDouble(), blockPos.getY() + threadLocalRandom.nextDouble(0.02, 0.12), spawnZ + threadLocalRandom.nextDouble()
         );
         if (WorldProjection.getVec3d2(vec3d, 1.0F, 1.0F) != null) {
            this.worldParticleList
               .add(
                  new ParticlesModule.SubContext4(
                     vec3d,
                     new Vec3d(threadLocalRandom.nextDouble(-0.02, 0.02), threadLocalRandom.nextDouble(0.02, 0.05), threadLocalRandom.nextDouble(-0.02, 0.02)),
                     spawnTimeMillis,
                     2400L,
                     true,
                     threadLocalRandom.nextInt(360),
                     this.worldImages.getSelectedCount() == 0 ? "Светлячок" : this.worldImages.getSelectedOptions().get(threadLocalRandom.nextInt(this.worldImages.getSelectedCount())),
                     this.worldSize.getValue() * 0.42F
                  )
               );
         }
      } else {
         Vec3d vec3d2 = new Vec3d(
            spawnX, mc.player.getY() + threadLocalRandom.nextDouble(1.0, Math.max(1.5, this.worldHeight.getValue())), spawnZ
         );
         if (WorldProjection.getVec3d2(vec3d2, 1.0F, 1.0F) != null) {
            this.worldParticleList
               .add(
                  new ParticlesModule.SubContext4(
                     vec3d2,
                     Vec3d.ZERO,
                     spawnTimeMillis,
                     5000L,
                     false,
                     threadLocalRandom.nextInt(360),
                     this.worldImages.getSelectedCount() == 0 ? "Светлячок" : this.worldImages.getSelectedOptions().get(threadLocalRandom.nextInt(this.worldImages.getSelectedCount())),
                     this.worldSize.getValue() * 0.42F
                  )
               );
         }
      }
   }

   private int renderCubeParticles(BufferBuilder bufferBuilder, long timeMillisArg4) {
      float cubeSize = this.size.getValue() * 0.6F;
      int vertexCount6 = 0;

      for (ParticlesModule.SubContext cubeParticle : this.cubeParticles) {
         float progress4 = (float)(timeMillisArg4 - cubeParticle.particleStartMillis) / (float)cubeParticle.particleLifetimeMillis;
         if (!(progress4 >= 1.0F)) {
            Vec3d vec3d = cubeParticle.getVec3d(progress4);
            if (WorldProjection.matchesCriteria2(vec3d.x, vec3d.y, vec3d.z, 2.0F)) {
               float cubeHalfSize = cubeSize * (0.6F + progress4 * 0.6F) * 0.5F;
               this.rotationMatrix
                  .identity()
                  .rotateY(cubeParticle.rotationSpeedY * progress4)
                  .rotateX(cubeParticle.rotationSpeedX * progress4)
                  .rotateZ(cubeParticle.rotationSpeedZ * progress4);

               for (int index = 0; index < 8; index++) {
                  this.rotationMatrix
                     .transform(
                        this.cubeCornerOffsets[index]
                           .set(
                              (index & 1) == 0 ? -cubeHalfSize : cubeHalfSize,
                              (index & 2) == 0 ? -cubeHalfSize : cubeHalfSize,
                              (index & 4) == 0 ? -cubeHalfSize : cubeHalfSize
                           )
                     );
               }

               int color4 = Colors.computeInt6(this.getParticleColor(cubeParticle.colorSeed), this.fadeInOutAlpha(progress4));

               for (int index2 = 0; index2 < cubeEdgeIndices.length; index2 += 2) {
                  vertexCount6 += this.drawGlowLine(
                     bufferBuilder,
                     vec3d.x + this.cubeCornerOffsets[cubeEdgeIndices[index2]].x,
                     vec3d.y + this.cubeCornerOffsets[cubeEdgeIndices[index2]].y,
                     vec3d.z + this.cubeCornerOffsets[cubeEdgeIndices[index2]].z,
                     vec3d.x + this.cubeCornerOffsets[cubeEdgeIndices[index2 + 1]].x,
                     vec3d.y + this.cubeCornerOffsets[cubeEdgeIndices[index2 + 1]].y,
                     vec3d.z + this.cubeCornerOffsets[cubeEdgeIndices[index2 + 1]].z,
                     color4
                  );
               }
            }
         }
      }

      return vertexCount6;
   }

   private synchronized float[] loadHeartModel() {
      if (this.heartModelLoaded) {
         return this.heartModelData;
      }

      this.heartModelLoaded = true;
      InputStream resourceAsStream = ParticlesModule.class.getResourceAsStream("/assets/greed/models/particles/heart.gltf");
      if (resourceAsStream == null) {
         return null;
      }

      try {
         JsonObject asJsonObject = JsonParser.parseString(new String(resourceAsStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
         String asString = asJsonObject.getAsJsonArray("buffers").get(0).getAsJsonObject().get("uri").getAsString();
         ByteBuffer byteBuffer = ByteBuffer.wrap(Base64.getDecoder().decode(asString.substring(asString.indexOf(44) + 1))).order(ByteOrder.LITTLE_ENDIAN);
         JsonArray asJsonArray = asJsonObject.getAsJsonArray("bufferViews");
         int[] intArray = new int[asJsonArray.size()];

         for (int index = 0; index < asJsonArray.size(); index++) {
            intArray[index] = asJsonArray.get(index).getAsJsonObject().has("byteOffset")
               ? asJsonArray.get(index).getAsJsonObject().get("byteOffset").getAsInt()
               : 0;
         }

         JsonArray asJsonArray2 = asJsonObject.getAsJsonArray("accessors");
         int[] intArray2 = new int[asJsonArray2.size()];
         int[] intArray3 = new int[asJsonArray2.size()];
         int[] intArray4 = new int[asJsonArray2.size()];
         int[] intArray5 = new int[asJsonArray2.size()];

         for (int index2 = 0; index2 < asJsonArray2.size(); index2++) {
            JsonObject asJsonObject2 = asJsonArray2.get(index2).getAsJsonObject();
            intArray2[index2] = asJsonObject2.get("bufferView").getAsInt();
            intArray3[index2] = asJsonObject2.get("componentType").getAsInt();
            intArray4[index2] = asJsonObject2.get("count").getAsInt();
            intArray5[index2] = asJsonObject2.has("byteOffset") ? asJsonObject2.get("byteOffset").getAsInt() : 0;
         }

         JsonArray asJsonArray3 = asJsonObject.getAsJsonArray("nodes");
         float[][] floatArrayArray = new float[asJsonArray3.size()][3];
         boolean[] booleanArray = new boolean[asJsonArray3.size()];
         JsonArray asJsonArray4 = asJsonObject.getAsJsonArray("scenes")
            .get(asJsonObject.has("scene") ? asJsonObject.get("scene").getAsInt() : 0)
            .getAsJsonObject()
            .getAsJsonArray("nodes");

         for (int index3 = 0; index3 < asJsonArray4.size(); index3++) {
            resolveNodeTranslation(asJsonArray3, asJsonArray4.get(index3).getAsInt(), 0.0F, 0.0F, 0.0F, floatArrayArray, booleanArray);
         }

         JsonArray asJsonArray5 = asJsonObject.getAsJsonArray("meshes");
         int[] intArray6 = new int[asJsonArray5.size()];
         int[] intArray7 = new int[asJsonArray5.size()];

         for (int index4 = 0; index4 < asJsonArray5.size(); index4++) {
            JsonObject asJsonObject3 = asJsonArray5.get(index4).getAsJsonObject().getAsJsonArray("primitives").get(0).getAsJsonObject();
            intArray6[index4] = asJsonObject3.getAsJsonObject("attributes").get("POSITION").getAsInt();
            intArray7[index4] = asJsonObject3.get("indices").getAsInt();
         }

         HashMap<String, ParticlesModule.SubContext2> heartEdgeMap = new HashMap<>();
         float heartMinX = Float.POSITIVE_INFINITY;
         float heartMinY = Float.POSITIVE_INFINITY;
         float heartMinZ = Float.POSITIVE_INFINITY;
         float heartMaxX = Float.NEGATIVE_INFINITY;
         float heartMaxY = Float.NEGATIVE_INFINITY;
         float heartMaxZ = Float.NEGATIVE_INFINITY;

         for (int index5 = 0; index5 < asJsonArray3.size(); index5++) {
            JsonObject asJsonObject4 = asJsonArray3.get(index5).getAsJsonObject();
            if (asJsonObject4.has("mesh")) {
               int asInt = asJsonObject4.get("mesh").getAsInt();
               int positionOffset = intArray[intArray2[intArray6[asInt]]] + intArray5[intArray6[asInt]];
               float[] floatArray = new float[intArray4[intArray6[asInt]] * 3];

               for (int index6 = 0; index6 < intArray4[intArray6[asInt]]; index6++) {
                  floatArray[index6 * 3] = byteBuffer.getFloat(positionOffset + index6 * 12) + floatArrayArray[index5][0];
                  floatArray[index6 * 3 + 1] = byteBuffer.getFloat(positionOffset + index6 * 12 + 4) + floatArrayArray[index5][1];
                  floatArray[index6 * 3 + 2] = byteBuffer.getFloat(positionOffset + index6 * 12 + 8) + floatArrayArray[index5][2];
                  heartMinX = Math.min(heartMinX, floatArray[index6 * 3]);
                  heartMaxX = Math.max(heartMaxX, floatArray[index6 * 3]);
                  heartMinY = Math.min(heartMinY, floatArray[index6 * 3 + 1]);
                  heartMaxY = Math.max(heartMaxY, floatArray[index6 * 3 + 1]);
                  heartMinZ = Math.min(heartMinZ, floatArray[index6 * 3 + 2]);
                  heartMaxZ = Math.max(heartMaxZ, floatArray[index6 * 3 + 2]);
               }

               int indexOffset = intArray[intArray2[intArray7[asInt]]] + intArray5[intArray7[asInt]];
               int[] intArray8 = new int[intArray4[intArray7[asInt]]];

               for (int index7 = 0; index7 < intArray8.length; index7++) {
                  if (intArray3[intArray7[asInt]] == 5123) {
                     intArray8[index7] = byteBuffer.getShort(indexOffset + index7 * 2) & '\uffff';
                  } else if (intArray3[intArray7[asInt]] == 5125) {
                     intArray8[index7] = byteBuffer.getInt(indexOffset + index7 * 4);
                  } else {
                     intArray8[index7] = byteBuffer.get(indexOffset + index7) & 255;
                  }
               }

               for (int index8 = 0; index8 + 2 < intArray8.length; index8 += 3) {
                  float triAX = floatArray[intArray8[index8] * 3];
                  float triAY = floatArray[intArray8[index8] * 3 + 1];
                  float triAZ = floatArray[intArray8[index8] * 3 + 2];
                  float triBX = floatArray[intArray8[index8 + 1] * 3];
                  float triBY = floatArray[intArray8[index8 + 1] * 3 + 1];
                  float triBZ = floatArray[intArray8[index8 + 1] * 3 + 2];
                  float triCX = floatArray[intArray8[index8 + 2] * 3];
                  float triCY = floatArray[intArray8[index8 + 2] * 3 + 1];
                  float triCZ = floatArray[intArray8[index8 + 2] * 3 + 2];
                  float triNormalX = (triBY - triAY) * (triCZ - triAZ) - (triBZ - triAZ) * (triCY - triAY);
                  float triNormalY = (triBZ - triAZ) * (triCX - triAX) - (triBX - triAX) * (triCZ - triAZ);
                  float triNormalZ = (triBX - triAX) * (triCY - triAY) - (triBY - triAY) * (triCX - triAX);
                  float triNormalLength = (float)Math.sqrt(triNormalX * triNormalX + triNormalY * triNormalY + triNormalZ * triNormalZ);
                  if (!(triNormalLength < 1.0E-8F)) {
                     accumulateEdge(
                        heartEdgeMap,
                        triAX,
                        triAY,
                        triAZ,
                        triBX,
                        triBY,
                        triBZ,
                        triNormalX / triNormalLength,
                        triNormalY / triNormalLength,
                        triNormalZ / triNormalLength
                     );
                     accumulateEdge(
                        heartEdgeMap,
                        triBX,
                        triBY,
                        triBZ,
                        triCX,
                        triCY,
                        triCZ,
                        triNormalX / triNormalLength,
                        triNormalY / triNormalLength,
                        triNormalZ / triNormalLength
                     );
                     accumulateEdge(
                        heartEdgeMap,
                        triCX,
                        triCY,
                        triCZ,
                        triAX,
                        triAY,
                        triAZ,
                        triNormalX / triNormalLength,
                        triNormalY / triNormalLength,
                        triNormalZ / triNormalLength
                     );
                  }
               }
            }
         }

         float centerX2 = (heartMinX + heartMaxX) * 0.5F;
         float centerY2 = (heartMinY + heartMaxY) * 0.5F;
         float centerZ2 = (heartMinZ + heartMaxZ) * 0.5F;
         float heartModelScale = Math.max(heartMaxX - heartMinX, Math.max(heartMaxY - heartMinY, heartMaxZ - heartMinZ)) > 1.0E-5F
            ? 1.2F / Math.max(heartMaxX - heartMinX, Math.max(heartMaxY - heartMinY, heartMaxZ - heartMinZ))
            : 1.0F;
         ArrayList<Float> vertexData2 = new ArrayList<>(heartEdgeMap.size() * 6);

         for (ParticlesModule.SubContext2 heartEdge : heartEdgeMap.values()) {
            if (heartEdge.useCount == 1 || !heartEdge.hasConsistentNormal) {
               vertexData2.add((heartEdge.edgeAX - centerX2) * heartModelScale);
               vertexData2.add((heartEdge.edgeAY - centerY2) * heartModelScale);
               vertexData2.add((heartEdge.edgeAZ - centerZ2) * heartModelScale);
               vertexData2.add((heartEdge.edgeBX - centerX2) * heartModelScale);
               vertexData2.add((heartEdge.edgeBY - centerY2) * heartModelScale);
               vertexData2.add((heartEdge.edgeBZ - centerZ2) * heartModelScale);
            }
         }

         this.heartModelData = new float[vertexData2.size()];

         for (int index9 = 0; index9 < vertexData2.size(); index9++) {
            this.heartModelData[index9] = vertexData2.get(index9);
         }
      } catch (Exception exception) {
         this.heartModelData = null;
      }

      return this.heartModelData;
   }

   private void renderImageParticles(
      ArrayList<ParticlesModule.SubContext4> particles6,
      MultiSelectSetting multiSelectSetting,
      float baseSize3,
      MatrixStack matrixStack,
      float tickDelta3,
      long timeMillisArg5
   ) {
      if (!particles6.isEmpty()) {
         Quaternionf quaternionf = new Quaternionf(mc.gameRenderer.getCamera().getRotation()).rotateY((float) Math.PI);
         Vector3f vector3f = new Vector3f();
         Arrays.fill(this.imageOptionCounts, 0);
         int unknownImageCount = 0;
         int optionIndex = 0;

         for (int index = particles6.size(); optionIndex < index; optionIndex++) {
            int imageOptionIndex = particles6.get(optionIndex).modelIndex;
            if (imageOptionIndex >= 0) {
               this.imageOptionCounts[imageOptionIndex]++;
            } else {
               unknownImageCount++;
            }
         }

         for (String optionName : multiSelectSetting.getOptions()) {
            int imageOptionIndex = findModelIndex(optionName);
            if (imageOptionIndex >= 0 ? this.imageOptionCounts[imageOptionIndex] != 0 : unknownImageCount != 0) {
               BufferBuilder bufferBuilder = new BufferBuilder(this.getBufferAllocator(), DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
               int matchedVertexCount = 0;
               int imageParticleIndex = 0;

               for (int index2 = particles6.size(); imageParticleIndex < index2; imageParticleIndex++) {
                  ParticlesModule.SubContext4 imageParticle = particles6.get(imageParticleIndex);
                  if (imageOptionIndex >= 0 ? imageParticle.modelIndex == imageOptionIndex : imageParticle.modelName.equals(optionName)) {
                     float alpha2 = imageParticle.getAlpha(timeMillisArg5);
                     if (!(alpha2 <= 0.0F)) {
                        Vec3d vec3d = imageParticle.previousPosition.lerp(imageParticle.position, tickDelta3);
                        if (WorldProjection.matchesCriteria2(vec3d.x, vec3d.y, vec3d.z, 2.0F)) {
                           this.drawBillboardQuad(
                              bufferBuilder,
                              quaternionf,
                              vector3f,
                              vec3d.x,
                              vec3d.y,
                              vec3d.z,
                              baseSize3 / 2.0F,
                              Colors.computeInt6(this.getParticleColor(imageParticle.hitColorSeed), alpha2)
                           );
                           matchedVertexCount++;
                        }
                     }
                  }
               }

               this.drawTexturedParticles(this.resolveImageTexture(optionName), matrixStack, bufferBuilder, matchedVertexCount);
            }
         }
      }
   }

   private float[] loadObjModel(String resourcePath, float yOffset, boolean centerVertically) {
      InputStream resourceAsStream = ParticlesModule.class.getResourceAsStream(resourcePath);
      if (resourceAsStream == null) {
         return null;
      }

      try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(resourceAsStream, StandardCharsets.UTF_8))) {
         ArrayList<Vector3f> vertices2 = new ArrayList<>();
         HashMap<Long, ParticlesModule.SubContext3> objEdgeMap = new HashMap<>();
         float objMinX = Float.POSITIVE_INFINITY;
         float objMinY = Float.POSITIVE_INFINITY;
         float objMinZ = Float.POSITIVE_INFINITY;
         float objMaxX = Float.NEGATIVE_INFINITY;
         float objMaxY = Float.NEGATIVE_INFINITY;
         float objMaxZ = Float.NEGATIVE_INFINITY;

         String line2;
         while ((line2 = bufferedReader.readLine()) != null) {
            line2 = line2.trim();
            if (line2.startsWith("v ")) {
               String[] stringArray = line2.split("\\s+");
               Vector3f vector3f = new Vector3f(
                  Float.parseFloat(stringArray[1]), Float.parseFloat(stringArray[2]) + yOffset, Float.parseFloat(stringArray[3])
               );
               vertices2.add(vector3f);
               objMinX = Math.min(objMinX, vector3f.x);
               objMaxX = Math.max(objMaxX, vector3f.x);
               objMinY = Math.min(objMinY, vector3f.y);
               objMaxY = Math.max(objMaxY, vector3f.y);
               objMinZ = Math.min(objMinZ, vector3f.z);
               objMaxZ = Math.max(objMaxZ, vector3f.z);
            } else if (line2.startsWith("f ")) {
               String[] stringArray2 = line2.split("\\s+");

               for (int index = 1; index < stringArray2.length; index++) {
                  int vertexIndexA = Integer.parseInt(stringArray2[index].split("/")[0]) - 1;
                  int vertexIndexB = Integer.parseInt(stringArray2[index == stringArray2.length - 1 ? 1 : index + 1].split("/")[0]) - 1;
                  if (vertexIndexA != vertexIndexB) {
                     String[] stringArray3 = stringArray2[index].split("/");
                     int uvIndex2 = stringArray3.length > 2 && !stringArray3[2].isEmpty() ? Integer.parseInt(stringArray3[2]) - 1 : -1;
                     long edgeKey2 = (long)Math.min(vertexIndexA, vertexIndexB) << 32 | Math.max(vertexIndexA, vertexIndexB) & 4294967295L;
                     ParticlesModule.SubContext3 existingEdge = objEdgeMap.get(edgeKey2);
                     if (existingEdge == null) {
                        objEdgeMap.put(edgeKey2, new ParticlesModule.SubContext3(vertices2.get(vertexIndexA), vertices2.get(vertexIndexB), uvIndex2));
                     } else {
                        existingEdge.useCount2++;
                        if (existingEdge.uvIndex != uvIndex2) {
                           existingEdge.hasConsistentUv = false;
                        }
                     }
                  }
               }
            }
         }

         float centerX3 = centerVertically ? (objMinX + objMaxX) * 0.5F : 0.0F;
         float centerY3 = centerVertically ? (objMinY + objMaxY) * 0.5F : 0.0F;
         float centerZ3 = centerVertically ? (objMinZ + objMaxZ) * 0.5F : 0.0F;
         float objModelScale = centerVertically && Math.max(objMaxX - objMinX, Math.max(objMaxY - objMinY, objMaxZ - objMinZ)) > 1.0E-5F
            ? 1.0F / Math.max(objMaxX - objMinX, Math.max(objMaxY - objMinY, objMaxZ - objMinZ))
            : 1.0F;
         ArrayList<Float> vertexData3 = new ArrayList<>();

         for (ParticlesModule.SubContext3 objEdge : objEdgeMap.values()) {
            if (objEdge.useCount2 == 1 || !objEdge.hasConsistentUv) {
               vertexData3.add((objEdge.vertexAX - centerX3) * objModelScale);
               vertexData3.add((objEdge.vertexAY - centerY3) * objModelScale);
               vertexData3.add((objEdge.vertexAZ - centerZ3) * objModelScale);
               vertexData3.add((objEdge.vertexBX - centerX3) * objModelScale);
               vertexData3.add((objEdge.vertexBY - centerY3) * objModelScale);
               vertexData3.add((objEdge.vertexBZ - centerZ3) * objModelScale);
            }
         }

         float[] floatArray = new float[vertexData3.size()];

         for (int index2 = 0; index2 < vertexData3.size(); index2++) {
            floatArray[index2] = vertexData3.get(index2);
         }

         return floatArray;
      } catch (Exception exception) {
         return null;
      }
   }

   private String resolveImageTexture(String imageName) {
      return switch (imageName) {
         case "Звезда" -> "textures/particles/star.png";
         case "Сердце" -> "textures/particles/heart.png";
         case "Снежинка" -> "textures/particles/snowflake.png";
         case "Доллар" -> "textures/particles/dollar.png";
         default -> "textures/particles/firefly.png";
      };
   }

   private int getParticleColor(int hueOffset) {
      return this.customColor.isChecked()
         ? this.colorSetting.getValue()
         : 0xFF000000 | ThemePaletteBuffer.computeInt5((float)(System.currentTimeMillis() % 3600L) / 3600.0F + hueOffset / 360.0F);
   }

   private void spawnHitParticles(long spawnTimeMillis2) {
      ParticlesModule.DataRecord5 hitRecord;
      while ((hitRecord = this.pendingHitParticles.poll()) != null) {
         if (this.hitParticles.isChecked() && this.images.getSelectedCount() != 0) {
            for (int index = 0; index < Math.max(1, (int)this.count2.getValue()); index++) {
               ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();
               if (this.hitParticleList.size() < 200) {
                  this.hitParticleList
                     .add(
                        new ParticlesModule.SubContext4(
                           new Vec3d(
                              hitRecord.feetPos().x, hitRecord.feetPos().y + threadLocalRandom.nextDouble() * hitRecord.heightBlocks(), hitRecord.feetPos().z
                           ),
                           new Vec3d(threadLocalRandom.nextDouble(-2.0, 2.0) * 0.08, threadLocalRandom.nextDouble(0.05, 0.2) * 0.08, threadLocalRandom.nextDouble(-2.0, 2.0) * 0.08),
                           spawnTimeMillis2,
                           2200L,
                           false,
                           threadLocalRandom.nextInt(360),
                           this.images.getSelectedOptions().get(threadLocalRandom.nextInt(this.images.getSelectedCount())),
                           this.hitSize.getValue() * 0.42F
                        )
                     );
               }
            }
         }
      }
   }

   private static final class SubContext {
      final Vec3d position;
      final double phase;
      final long particleStartMillis;
      final long particleLifetimeMillis;
      final int colorSeed;
      final float rotationSpeedX;
      final float rotationSpeedY;
      final float rotationSpeedZ;
      final double velocityX;
      final double velocityY;
      final double velocityZ;

      Vec3d getVec3d(float progressFactor) {
         return new Vec3d(
            this.position.x + this.velocityX * progressFactor,
            this.position.y + this.velocityY * progressFactor,
            this.position.z + this.velocityZ * progressFactor
         );
      }

      Vec3d getVec3d2(float progressFactor2) {
         return new Vec3d(
            this.position.x + MathHelper.sin((float)this.phase * 7.0F + progressFactor2 * 4.0F) * 0.6F,
            this.position.y + MathHelper.sin((float)this.phase * 5.0F + progressFactor2 * 3.0F) * 0.4F + progressFactor2 * 0.6F,
            this.position.z + MathHelper.cos((float)this.phase * 9.0F + progressFactor2 * 5.0F) * 0.6F
         );
      }

      SubContext(
         Vec3d vec3d,
         double phase2,
         long startTimeMillis3,
         long lifetimeMillis,
         int colorSeed2,
         float spinX3,
         float spinY3,
         float spinZ3,
         double velocityX2,
         double velocityY2,
         double velocityZ2
      ) {
         this.position = vec3d;
         this.phase = phase2;
         this.particleStartMillis = startTimeMillis3;
         this.particleLifetimeMillis = lifetimeMillis;
         this.colorSeed = colorSeed2;
         this.rotationSpeedX = spinX3;
         this.rotationSpeedY = spinY3;
         this.rotationSpeedZ = spinZ3;
         this.velocityX = velocityX2;
         this.velocityY = velocityY2;
         this.velocityZ = velocityZ2;
      }
   }

   private static final class SubContext2 {
      final float edgeAX;
      final float edgeAY;
      final float edgeAZ;
      final float edgeBX;
      final float edgeBY;
      final float edgeBZ;
      final float edgeNormalX;
      final float edgeNormalY;
      final float edgeNormalZ;
      int useCount = 1;
      boolean hasConsistentNormal = true;

      SubContext2(
         float edgeAX2,
         float edgeAY2,
         float edgeAZ2,
         float edgeBX2,
         float edgeBY2,
         float edgeBZ2,
         float edgeNormalX2,
         float edgeNormalY2,
         float edgeNormalZ2
      ) {
         this.edgeAX = edgeAX2;
         this.edgeAY = edgeAY2;
         this.edgeAZ = edgeAZ2;
         this.edgeBX = edgeBX2;
         this.edgeBY = edgeBY2;
         this.edgeBZ = edgeBZ2;
         this.edgeNormalX = edgeNormalX2;
         this.edgeNormalY = edgeNormalY2;
         this.edgeNormalZ = edgeNormalZ2;
      }
   }

   private static final class SubContext3 {
      final float vertexAX;
      final float vertexAY;
      final float vertexAZ;
      final float vertexBX;
      final float vertexBY;
      final float vertexBZ;
      final int uvIndex;
      int useCount2 = 1;
      boolean hasConsistentUv = true;

      SubContext3(Vector3f vector3f, Vector3f vector3f2, int uvIndexParam) {
         this.vertexAX = vector3f.x;
         this.vertexAY = vector3f.y;
         this.vertexAZ = vector3f.z;
         this.vertexBX = vector3f2.x;
         this.vertexBY = vector3f2.y;
         this.vertexBZ = vector3f2.z;
         this.uvIndex = uvIndexParam;
      }
   }

   private static final class SubContext4 {
      Vec3d position;
      Vec3d previousPosition;
      Vec3d velocity;
      final long worldStartMillis;
      final long worldLifetimeMillis;
      final boolean gravityAffected;
      final int hitColorSeed;
      final String modelName;
      final int modelIndex;
      final float radius;

      SubContext4(Vec3d vec3d, Vec3d vec3d2, long startTimeParam, long lifetimeParam, boolean gravityParam, int colorSeedParam, String modelNameParam, float radiusParam) {
         this.position = vec3d;
         this.previousPosition = vec3d;
         this.velocity = vec3d2;
         this.worldStartMillis = startTimeParam;
         this.worldLifetimeMillis = lifetimeParam;
         this.gravityAffected = gravityParam;
         this.hitColorSeed = colorSeedParam;
         this.modelName = modelNameParam;
         this.modelIndex = ParticlesModule.findModelIndex(modelNameParam);
         this.radius = radiusParam;
      }

      float getFadeProgress(long nowMillis4) {
         return MathHelper.clamp((float)(nowMillis4 - this.worldStartMillis) / (float)this.worldLifetimeMillis, 0.0F, 1.0F);
      }

      void updatePhysics() {
         this.previousPosition = this.position;
         if (this.gravityAffected) {
            this.velocity = this.velocity.add(0.0, -0.0045, 0.0);
         }

         double nextX = this.position.x + this.velocity.x;
         double nextY = this.position.y + this.velocity.y;
         double nextZ = this.position.z + this.velocity.z;
         if (!MinecraftAccess.mc
            .world
            .getBlockState(BlockPos.ofFloored(this.position.x, this.position.y, nextZ))
            .getCollisionShape(MinecraftAccess.mc.world, BlockPos.ofFloored(this.position.x, this.position.y, nextZ))
            .isEmpty()) {
            this.velocity = new Vec3d(this.velocity.x, this.velocity.y, this.velocity.z * -0.8);
         }

         if (!MinecraftAccess.mc
            .world
            .getBlockState(BlockPos.ofFloored(this.position.x, nextY, this.position.z))
            .getCollisionShape(MinecraftAccess.mc.world, BlockPos.ofFloored(this.position.x, nextY, this.position.z))
            .isEmpty()) {
            if (this.gravityAffected && this.velocity.y < 0.0) {
               this.velocity = this.velocity
                  .lerp(new Vec3d(this.velocity.x * 0.82, ThreadLocalRandom.current().nextDouble(0.012, 0.028), this.velocity.z * 0.82), 0.42);
            } else {
               this.velocity = new Vec3d(this.velocity.x * 0.999, this.velocity.y * -0.7, this.velocity.z * 0.999);
            }
         }

         if (!MinecraftAccess.mc
            .world
            .getBlockState(BlockPos.ofFloored(nextX, this.position.y, this.position.z))
            .getCollisionShape(MinecraftAccess.mc.world, BlockPos.ofFloored(nextX, this.position.y, this.position.z))
            .isEmpty()) {
            this.velocity = new Vec3d(this.velocity.x * -0.8, this.velocity.y, this.velocity.z);
         }

         if (this.gravityAffected) {
            this.velocity = this.velocity.multiply(0.996);
         } else {
            this.velocity = new Vec3d(
               MathHelper.clamp(this.velocity.x + (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.005, -0.5, 0.5),
               MathHelper.clamp(this.velocity.y + (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.005, -0.5, 0.5),
               MathHelper.clamp(this.velocity.z + (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.005, -0.5, 0.5)
            );
         }

         this.position = this.position.add(this.velocity);
         if (!this.gravityAffected) {
            this.velocity = new Vec3d(this.velocity.x / 0.999999, this.velocity.y - 5.0E-5, this.velocity.z / 0.999999);
         }
      }

      float getAlpha(long nowMillis5) {
         long elapsedMillis = nowMillis5 - this.worldStartMillis;
         if (elapsedMillis >= this.worldLifetimeMillis) {
            return 0.0F;
         } else if (elapsedMillis < 250L) {
            return (float)elapsedMillis / 250.0F;
         } else {
            return elapsedMillis >= this.worldLifetimeMillis - 250L ? (float)(this.worldLifetimeMillis - elapsedMillis) / 250.0F : 1.0F;
         }
      }

      boolean isExpired(long nowMillis6) {
         return nowMillis6 - this.worldStartMillis >= this.worldLifetimeMillis;
      }
   }

   private record DataRecord5(Vec3d feetPos, float heightBlocks) {
   }
}
