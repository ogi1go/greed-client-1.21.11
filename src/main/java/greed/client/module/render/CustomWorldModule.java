package greed.client.module.render;

import greed.client.setting.BooleanSetting;
import greed.client.GreedClient;
import greed.client.setting.LegacyNumberSetting;
import greed.client.setting.ModeSetting;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.render.WorldProjection;
import greed.client.ui.UiTheme;
import greed.client.setting.ColorSetting;
import greed.client.setting.SectionSetting;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.awt.Color;
import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Random;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

@ModuleInfo(name = "CustomWorld", description = "Насыщенность, освещение, время, погода и шейдерное небо.", category = ModuleCategory.RENDER)
public final class CustomWorldModule extends Module {
   private static CustomWorldModule customWorldModule;
   private static final RenderPipeline pipeline2 = RenderPipeline.builder()
      .withLocation(GreedClient.id("pipeline/sky_plasma"))
      .withVertexShader(GreedClient.id("core/sky"))
      .withFragmentShader(GreedClient.id("core/plasma"))
      .withVertexFormat(VertexFormats.POSITION, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .withUniform("SkyParams", UniformType.UNIFORM_BUFFER)
      .build();
   private static final RenderPipeline pipeline3 = RenderPipeline.builder()
      .withLocation(GreedClient.id("pipeline/sky_space"))
      .withVertexShader(GreedClient.id("core/sky"))
      .withFragmentShader(GreedClient.id("core/space"))
      .withVertexFormat(VertexFormats.POSITION, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .withUniform("SkyParams", UniformType.UNIFORM_BUFFER)
      .build();
   private static final RenderPipeline pipeline4 = getRenderPipeline("sky_galaxy");
   private static final RenderPipeline pipeline5 = getRenderPipeline("sky_overcast");
   private static final RenderPipeline pipeline6 = getRenderPipeline("sky_purple");
   private static final RenderPipeline pipeline7 = getRenderPipeline("sky_rays");
   private static final RenderPipeline pipeline8 = getRenderPipeline("sky_nord");
   private static final RenderPipeline pipeline9 = RenderPipeline.builder()
      .withLocation(GreedClient.id("pipeline/sky_aurora_layer"))
      .withVertexShader(GreedClient.id("core/sky_post"))
      .withFragmentShader(GreedClient.id("core/sky_aurora_layer"))
      .withVertexFormat(VertexFormats.POSITION, DrawMode.QUADS)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .withUniform("SkyPostParams", UniformType.UNIFORM_BUFFER)
      .build();
   private static final RenderPipeline pipeline10 = RenderPipeline.builder()
      .withLocation(GreedClient.id("pipeline/sky_aurora"))
      .withVertexShader(GreedClient.id("core/sky_post"))
      .withFragmentShader(GreedClient.id("core/sky_aurora"))
      .withVertexFormat(VertexFormats.POSITION, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("SkyPostParams", UniformType.UNIFORM_BUFFER)
      .build();
   public LegacyNumberSetting saturation;
   public LegacyNumberSetting brightness;
   public LegacyNumberSetting skyStrength;
   public BooleanSetting freezeTime;
   public LegacyNumberSetting timeOfDay;
   public ModeSetting weather;
   public BooleanSetting customPrecipitationColor;
   public ColorSetting precipitationColor;
   public ModeSetting shaderSky;
   public BooleanSetting shaderRain;
   public BooleanSetting wetSurfaces;
   public BooleanSetting onlyUnderRain;
   public LegacyNumberSetting wetness;
   public LegacyNumberSetting reflections;
   public LegacyNumberSetting glare;
   public LegacyNumberSetting ripples;
   public BooleanSetting onlyFloor;
   public LegacyNumberSetting reflectSky;
   public BooleanSetting customReflectionColor;
   public ColorSetting reflectionColor;
   public LegacyNumberSetting hitThickness;
   public LegacyNumberSetting reflectionDistance;
   public BooleanSetting worldRain;
   public BooleanSetting onlyInRain;
   public LegacyNumberSetting rainStreams;
   public LegacyNumberSetting rainDensity;
   public LegacyNumberSetting rainSpeed;
   public LegacyNumberSetting streamLength;
   public BooleanSetting hideVanillaRain;
   public LegacyNumberSetting screenDroplets;
   public LegacyNumberSetting groundSplashes;
   public BooleanSetting checkRoof;
   public BooleanSetting customRainColor;
   public ColorSetting rainColor;
   public BooleanSetting fog;
   public ModeSetting fogType;
   public LegacyNumberSetting fogDistance;
   public ColorSetting fogColor;
   public LegacyNumberSetting fogVolume;
   private static final RenderPipeline pipeline11 = RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
      .withLocation(GreedClient.id("pipeline/custom_world_fog"))
      .withVertexShader(Identifier.ofVanilla("core/position_tex_color"))
      .withFragmentShader(Identifier.ofVanilla("core/position_tex_color"))
      .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS)
      .withBlend(BlendFunction.LIGHTNING)
      .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .withSampler("Sampler0")
      .build();
   private static final RenderLayer renderLayer = RenderLayer.of(
      "custom_world_fog",
      RenderSetup.builder(pipeline11)
         .texture("Sampler0", GreedClient.id("textures/glow.png"))
         .expectedBufferSize(320 * VertexFormats.POSITION_TEXTURE_COLOR.getVertexSize())
         .build()
   );
   private final Immediate immediate2 = VertexConsumerProvider.immediate(new BufferAllocator(320 * VertexFormats.POSITION_TEXTURE_COLOR.getVertexSize()));
   private final float[] floatValArray = new float[400];
   private final Quaternionf quaternionf = new Quaternionf();
   private final Vector3f vector3f = new Vector3f();
   private float floatVal = -1.0F;
   private static final RenderPipeline pipeline12 = RenderPipeline.builder()
      .withLocation(GreedClient.id("pipeline/haze"))
      .withVertexShader(GreedClient.id("core/sky_post"))
      .withFragmentShader(GreedClient.id("core/haze"))
      .withVertexFormat(VertexFormats.POSITION, DrawMode.QUADS)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .withSampler("Sampler1")
      .withUniform("SkyPostParams", UniformType.UNIFORM_BUFFER)
      .build();
   private static final RenderPipeline pipeline13 = RenderPipeline.builder()
      .withLocation(GreedClient.id("pipeline/haze_blit"))
      .withVertexShader(GreedClient.id("core/sky_post"))
      .withFragmentShader(GreedClient.id("core/haze_blit"))
      .withVertexFormat(VertexFormats.POSITION, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .withSampler("Sampler0")
      .withSampler("Sampler1")
      .withUniform("SkyPostParams", UniformType.UNIFORM_BUFFER)
      .build();
   private final Matrix4f matrix4f3 = new Matrix4f();
   private SimpleFramebuffer framebuffer2;
   private GpuBuffer gpuBuffer;
   private GpuBuffer gpuBuffer2;
   public static final float[] floatValArray2 = new float[]{0.45F, 0.6F, 0.9F};
   private GpuBuffer gpuBuffer3;
   private GpuBuffer gpuBuffer4;
   private GpuBuffer gpuBuffer5;
   private SimpleFramebuffer framebuffer3;

   private boolean checkState() {
      return this.shaderSky.isSelected("Галактика")
         || this.shaderSky.isSelected("Пасмурно")
         || this.shaderSky.isSelected("Фиолетовый")
         || this.shaderSky.isSelected("Аврора")
         || this.shaderSky.isSelected("Сияние")
         || this.shaderSky.isSelected("Норд");
   }

   public void processData(Matrix4f matrix4f, Matrix4f matrix4f2) {
      if (!this.checkState2()) {
         this.updateState2();
      } else {
         Framebuffer framebuffer = mc.getFramebuffer();
         if (framebuffer != null
            && framebuffer.getColorAttachmentView() != null
            && framebuffer.getDepthAttachmentView() != null
            && framebuffer.textureWidth > 0
            && framebuffer.textureHeight > 0) {
            if (this.gpuBuffer3 == null || this.gpuBuffer3.isClosed()) {
               try (BufferAllocator bufferAllocator = new BufferAllocator(4 * VertexFormats.POSITION.getVertexSize())) {
                  BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION);
                  bufferBuilder.vertex(-1.0F, -1.0F, 0.0F);
                  bufferBuilder.vertex(-1.0F, 1.0F, 0.0F);
                  bufferBuilder.vertex(1.0F, 1.0F, 0.0F);
                  bufferBuilder.vertex(1.0F, -1.0F, 0.0F);

                  try (BuiltBuffer builtBuffer = bufferBuilder.end()) {
                     this.gpuBuffer3 = RenderSystem.getDevice().createBuffer(() -> "greed_sky_quad", 32, builtBuffer.getBuffer());
                  }
               }
            }

            if (this.framebuffer3 != null && !this.shaderSky.isSelected("Аврора")) {
               this.framebuffer3.delete();
               this.framebuffer3 = null;
            }

            if (!this.shaderSky.isSelected("Плазма") && !this.shaderSky.isSelected("Космос")) {
               if (this.gpuBuffer5 == null || this.gpuBuffer5.isClosed()) {
                  this.gpuBuffer5 = RenderSystem.getDevice().createBuffer(() -> "greed_sky_post_params", 136, 128L);
               }

               MemoryStack memoryStack = MemoryStack.stackPush();

               try {
                  ByteBuffer byteBuffer = memoryStack.calloc(128);
                  new Matrix4f(matrix4f2).mul(matrix4f).invert().get(0, byteBuffer);
                  byteBuffer.putFloat(64, (UiTheme.intVal7 >> 16 & 0xFF) / 255.0F);
                  byteBuffer.putFloat(68, (UiTheme.intVal7 >> 8 & 0xFF) / 255.0F);
                  byteBuffer.putFloat(72, (UiTheme.intVal7 & 0xFF) / 255.0F);
                  byteBuffer.putFloat(76, this.shaderSky.isSelected("Фиолетовый") ? 1.0F : 0.5F);
                  byteBuffer.putFloat(80, (UiTheme.intVal9 >> 16 & 0xFF) / 255.0F);
                  byteBuffer.putFloat(84, (UiTheme.intVal9 >> 8 & 0xFF) / 255.0F);
                  byteBuffer.putFloat(88, (UiTheme.intVal9 & 0xFF) / 255.0F);
                  byteBuffer.putFloat(92, 0.46F);
                  byteBuffer.putFloat(96, (float)(System.currentTimeMillis() % 100000L) / 1000.0F);
                  byteBuffer.putFloat(100, this.skyStrength.getValue());
                  byteBuffer.putFloat(104, (float)framebuffer.textureWidth / framebuffer.textureHeight);
                  byteBuffer.putFloat(108, framebuffer.textureHeight);
                  byteBuffer.putFloat(112, (float)mc.gameRenderer.getCamera().getCameraPos().x);
                  byteBuffer.putFloat(116, (float)mc.gameRenderer.getCamera().getCameraPos().y);
                  byteBuffer.putFloat(120, (float)mc.gameRenderer.getCamera().getCameraPos().z);
                  RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.gpuBuffer5.slice(0L, 128L), byteBuffer);
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

               if (this.shaderSky.isSelected("Аврора")) {
                  int local3417_4x = Math.max(1, framebuffer.textureWidth / 2);
                  int local3417_5 = Math.max(1, framebuffer.textureHeight / 2);
                  if (this.framebuffer3 == null) {
                     this.framebuffer3 = new SimpleFramebuffer("greed_sky_aurora_layer", local3417_4x, local3417_5, false);
                  }

                  if (this.framebuffer3.textureWidth != local3417_4x || this.framebuffer3.textureHeight != local3417_5) {
                     this.framebuffer3.resize(local3417_4x, local3417_5);
                  }

                  try (RenderPass renderPass = RenderSystem.getDevice()
                        .createCommandEncoder()
                        .createRenderPass(
                           () -> "greed_sky_aurora_layer", this.framebuffer3.getColorAttachmentView(), OptionalInt.empty(), null, OptionalDouble.empty()
                        )) {
                     renderPass.setPipeline(pipeline9);
                     renderPass.setUniform("SkyPostParams", this.gpuBuffer5);
                     renderPass.setVertexBuffer(0, this.gpuBuffer3);
                     renderPass.setIndexBuffer(
                        RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexBuffer(6), RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexType()
                     );
                     renderPass.drawIndexed(0, 0, 6, 1);
                  }

                  try (RenderPass renderPass2 = RenderSystem.getDevice()
                        .createCommandEncoder()
                        .createRenderPass(
                           () -> "greed_sky_aurora",
                           framebuffer.getColorAttachmentView(),
                           OptionalInt.empty(),
                           framebuffer.getDepthAttachmentView(),
                           OptionalDouble.empty()
                        )) {
                     renderPass2.setPipeline(pipeline10);
                     renderPass2.bindTexture("Sampler0", this.framebuffer3.getColorAttachmentView(), RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
                     renderPass2.setUniform("SkyPostParams", this.gpuBuffer5);
                     renderPass2.setVertexBuffer(0, this.gpuBuffer3);
                     renderPass2.setIndexBuffer(
                        RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexBuffer(6), RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexType()
                     );
                     renderPass2.drawIndexed(0, 0, 6, 1);
                  }
               } else {
                  try (RenderPass local3417_4x = RenderSystem.getDevice()
                        .createCommandEncoder()
                        .createRenderPass(
                           () -> "greed_sky_post",
                           framebuffer.getColorAttachmentView(),
                           OptionalInt.empty(),
                           framebuffer.getDepthAttachmentView(),
                           OptionalDouble.empty()
                        )) {
                     local3417_4x.setPipeline(
                        this.shaderSky.isSelected("Галактика")
                           ? pipeline4
                           : (
                              this.shaderSky.isSelected("Пасмурно")
                                 ? pipeline5
                                 : (this.shaderSky.isSelected("Сияние") ? pipeline7 : (this.shaderSky.isSelected("Норд") ? pipeline8 : pipeline6))
                           )
                     );
                     local3417_4x.setUniform("SkyPostParams", this.gpuBuffer5);
                     local3417_4x.setVertexBuffer(0, this.gpuBuffer3);
                     local3417_4x.setIndexBuffer(
                        RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexBuffer(6), RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexType()
                     );
                     local3417_4x.drawIndexed(0, 0, 6, 1);
                  }
               }
            } else {
               if (this.gpuBuffer4 == null || this.gpuBuffer4.isClosed()) {
                  this.gpuBuffer4 = RenderSystem.getDevice().createBuffer(() -> "greed_sky_params", 136, 96L);
               }

               MemoryStack memoryStack2 = MemoryStack.stackPush();

               try {
                  ByteBuffer byteBuffer2 = memoryStack2.calloc(96);
                  new Matrix4f(matrix4f2).mul(matrix4f).invert().get(0, byteBuffer2);
                  byteBuffer2.putFloat(64, (UiTheme.intVal7 >> 16 & 0xFF) / 255.0F);
                  byteBuffer2.putFloat(68, (UiTheme.intVal7 >> 8 & 0xFF) / 255.0F);
                  byteBuffer2.putFloat(72, (UiTheme.intVal7 & 0xFF) / 255.0F);
                  byteBuffer2.putFloat(76, 1.0F);
                  byteBuffer2.putFloat(80, (float)(System.currentTimeMillis() % 1000000L) / 1000.0F);
                  byteBuffer2.putFloat(84, 1.0F);
                  RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.gpuBuffer4.slice(0L, 96L), byteBuffer2);
               } catch (Throwable throwable3) {
                  if (memoryStack2 != null) {
                     try {
                        memoryStack2.close();
                     } catch (Throwable throwable4) {
                        throwable3.addSuppressed(throwable4);
                     }
                  }

                  throw throwable3;
               }

               if (memoryStack2 != null) {
                  memoryStack2.close();
               }

               try (RenderPass local3417_4x = RenderSystem.getDevice()
                     .createCommandEncoder()
                     .createRenderPass(
                        () -> "greed_sky",
                        framebuffer.getColorAttachmentView(),
                        OptionalInt.empty(),
                        framebuffer.getDepthAttachmentView(),
                        OptionalDouble.empty()
                     )) {
                  local3417_4x.setPipeline(this.shaderSky.isSelected("Космос") ? pipeline3 : pipeline2);
                  local3417_4x.setUniform("SkyParams", this.gpuBuffer4);
                  local3417_4x.setVertexBuffer(0, this.gpuBuffer3);
                  local3417_4x.setIndexBuffer(
                     RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexBuffer(6), RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexType()
                  );
                  local3417_4x.drawIndexed(0, 0, 6, 1);
               }
            }
         }
      }
   }

   public boolean checkState2() {
      return this.isEnabled() && !this.shaderSky.isSelected("Выкл");
   }

   private void processData2(float local3427_1) {
      Random random = new Random(42L);
      float local3427_3 = local3427_1 * 0.2F;
      float local3427_4 = local3427_1 * 0.95F;

      for (int index = 0; index < 80; index++) {
         float local3427_6 = local3427_3 + random.nextFloat() * (local3427_4 - local3427_3);
         float local3427_7 = random.nextFloat() * (float) (Math.PI * 2);
         int local3427_8 = index * 5;
         this.floatValArray[local3427_8] = MathHelper.sin(local3427_7) * local3427_6;
         this.floatValArray[local3427_8 + 1] = (random.nextFloat() - 0.4F) * local3427_1 * 0.4F;
         this.floatValArray[local3427_8 + 2] = MathHelper.cos(local3427_7) * local3427_6;
         this.floatValArray[local3427_8 + 3] = local3427_1 * (0.3F + random.nextFloat() * 0.5F);
         this.floatValArray[local3427_8 + 4] = (local3427_6 - local3427_3) / (local3427_4 - local3427_3);
      }

      this.floatVal = local3427_1;
   }

   private void processData3(
      VertexConsumer vertexConsumer,
      Matrix4f matrix4f,
      int local3428_3,
      float local3428_4,
      float local3428_5,
      float local3428_6,
      float local3428_7,
      int local3428_8
   ) {
      this.quaternionf.transform(this.vector3f.set(local3428_4, local3428_5, 0.0F));
      vertexConsumer.vertex(
            matrix4f,
            this.floatValArray[local3428_3] + this.vector3f.x,
            this.floatValArray[local3428_3 + 1] + this.vector3f.y,
            this.floatValArray[local3428_3 + 2] + this.vector3f.z
         )
         .texture(local3428_6, local3428_7)
         .color(local3428_8);
   }

   private CustomWorldModule() {
      this.saturation = this.addSetting(new LegacyNumberSetting("Насыщенность", "Насыщенность цветов мира.", 1.0, 0.0, 2.0, 0.05));
      this.brightness = this.addSetting(new LegacyNumberSetting("Освещение", "Яркость освещения мира.", 1.0, 0.0, 2.0, 0.05));
      this.addSetting(new SectionSetting("Туман"));
      this.fog = this.addSetting(new BooleanSetting("Туман", "Свой туман вокруг игрока.", false));
      this.fogType = this.addSetting(
         new ModeSetting("Тип тумана", "Облака — объёмные клубы вокруг, Дымка — плотная пелена, съедающая даль.", "Облака", "Облака", "Дымка")
      );
      this.fogType.withVisibility(() -> this.fog.isChecked());
      this.fogDistance = this.addSetting(new LegacyNumberSetting("Дальность тумана", "Дальность отрисовки тумана в блоках.", 80.0, 10.0, 255.0, 5.0));
      this.fogColor = this.addSetting(new ColorSetting("Цвет тумана", "Цвет тумана.", new Color(214, 216, 222)));
      this.fogVolume = this.addSetting(new LegacyNumberSetting("Объём тумана", "Плотность тумана.", 0.3, 0.05, 1.0, 0.05));
      this.fogDistance.withVisibility(() -> this.fog.isChecked());
      this.fogColor.withVisibility(() -> this.fog.isChecked());
      this.fogVolume.withVisibility(() -> this.fog.isChecked());
      this.addSetting(new SectionSetting("Время и погода"));
      this.timeOfDay = this.addSetting(
         new LegacyNumberSetting(
            "Время суток",
            "Время суток в тиках: 0 рассвет, 6000 полдень, 12000 закат, 18000 полночь. Сдвинь — время зафиксируется само.",
            6000.0,
            0.0,
            24000.0,
            100.0
         )
      );
      this.freezeTime = this.addSetting(new BooleanSetting("Фиксировать время", "Зафиксировать время суток слайдером.", false));
      this.weather = this.addSetting(new ModeSetting("Погода", "Зафиксировать погоду.", "Выкл", "Выкл", "Ясно", "Дождь", "Гроза", "Снег"));
      this.customPrecipitationColor = this.addSetting(new BooleanSetting("Свой цвет осадков", "Красить дождь и снег своим цветом.", false));
      this.precipitationColor = this.addSetting(new ColorSetting("Цвет осадков", "Цвет дождя и снега."));
      this.addSetting(new SectionSetting("Небо и осадки"));
      this.shaderSky = this.addSetting(
         new ModeSetting(
            "Шейдерное небо", "Заменить небо шейдером.", "Выкл", "Выкл", "Плазма", "Космос", "Галактика", "Пасмурно", "Фиолетовый", "Аврора", "Сияние", "Норд"
         )
      );
      this.skyStrength = this.addSetting(new LegacyNumberSetting("Сила неба", "Насколько плотно шейдер неба закрашивает мир.", 0.65, 0.05, 1.0, 0.05));
      this.shaderRain = new BooleanSetting("Шейдерный дождь", "Заменить дождь шейдером.", false);
      this.addSetting(new SectionSetting("Дождь"));
      this.worldRain = this.addSetting(new BooleanSetting("Дождь в мире", "Капли на камере и брызги по земле.", false));
      this.onlyInRain = this.addSetting(new BooleanSetting("Только в дождь", "Показывать только когда идёт дождь и над головой открытое небо.", true));
      this.rainStreams = this.addSetting(new LegacyNumberSetting("Струи дождя", "Сила падающих струй дождя в воздухе.", 0.7, 0.0, 1.0, 0.05));
      this.rainDensity = this.addSetting(new LegacyNumberSetting("Плотность дождя", "Сколько струй в воздухе.", 1.0, 0.1, 2.0, 0.05));
      this.rainSpeed = this.addSetting(new LegacyNumberSetting("Скорость дождя", "Скорость падения и наклон струй.", 1.0, 0.3, 2.0, 0.05));
      this.streamLength = this.addSetting(new LegacyNumberSetting("Длина струй", "Во сколько раз струя вытягивается вдоль падения.", 10.0, 1.0, 24.0, 0.5));
      this.hideVanillaRain = this.addSetting(new BooleanSetting("Скрыть ванильный дождь", "Убрать ванильные полосы дождя, оставив только шейдерные.", true));
      this.screenDroplets = this.addSetting(new LegacyNumberSetting("Капли на экране", "Сила капель на камере.", 0.6, 0.0, 1.0, 0.05));
      this.groundSplashes = this.addSetting(new LegacyNumberSetting("Брызги на земле", "Сила брызг по горизонтальным поверхностям.", 0.6, 0.0, 1.0, 0.05));
      this.checkRoof = this.addSetting(new BooleanSetting("Учитывать крышу", "Не брызгать под крышей и в помещениях.", true));
      this.customRainColor = this.addSetting(new BooleanSetting("Свой цвет дождя", "Использовать свой цвет вместо цвета горизонта.", false));
      this.rainColor = this.addSetting(new ColorSetting("Цвет дождя", "Цвет капель и брызг.", new Color(179, 199, 217)));
      this.addSetting(new SectionSetting("Мокрый мир"));
      this.wetSurfaces = this.addSetting(new BooleanSetting("Мокрые поверхности", "Мокрые блики и экранные отражения на поверхностях.", false));
      this.onlyUnderRain = this.addSetting(new BooleanSetting("Только под дождём", "Сила эффекта следует за уровнем дождя.", false));
      this.wetness = this.addSetting(new LegacyNumberSetting("Влажность", "Насколько поверхности выглядят мокрыми.", 0.6, 0.0, 1.0, 0.05));
      this.reflections = this.addSetting(new LegacyNumberSetting("Отражения", "Сила экранных отражений.", 0.7, 0.0, 1.0, 0.05));
      this.glare = this.addSetting(new LegacyNumberSetting("Блик", "Сила солнечного блика на мокрой поверхности.", 0.5, 0.0, 2.0, 0.05));
      this.ripples = this.addSetting(new LegacyNumberSetting("Рябь", "Рябь по лужам.", 0.35, 0.0, 1.0, 0.05));
      this.onlyFloor = this.addSetting(new BooleanSetting("Только пол", "Мочить только горизонтальные поверхности.", true));
      this.reflectSky = this.addSetting(new LegacyNumberSetting("Отражать небо", "Насколько промахнувшийся луч берёт цвет неба с экрана.", 0.8, 0.0, 1.0, 0.05));
      this.customReflectionColor = this.addSetting(new BooleanSetting("Свой цвет отражений", "Использовать свой цвет вместо цвета горизонта.", false));
      this.reflectionColor = this.addSetting(new ColorSetting("Цвет отражений", "Цвет отражений там, где луч не попал в геометрию."));
      this.hitThickness = this.addSetting(new LegacyNumberSetting("Толщина попадания", "Допуск толщины геометрии при попадании луча.", 0.6, 0.1, 3.0, 0.1));
      this.reflectionDistance = this.addSetting(new LegacyNumberSetting("Дальность отражений", "Максимальная длина луча отражения в блоках.", 24.0, 4.0, 96.0, 1.0));
      this.skyStrength.withVisibility(() -> this.checkState());
      this.timeOfDay.onChange(() -> this.freezeTime.setValue(true));
      this.precipitationColor.withVisibility(() -> this.customPrecipitationColor.isChecked());
      this.onlyUnderRain.withVisibility(() -> this.wetSurfaces.isChecked());
      this.wetness.withVisibility(() -> this.wetSurfaces.isChecked());
      this.reflections.withVisibility(() -> this.wetSurfaces.isChecked());
      this.glare.withVisibility(() -> this.wetSurfaces.isChecked());
      this.ripples.withVisibility(() -> this.wetSurfaces.isChecked());
      this.onlyFloor.withVisibility(() -> this.wetSurfaces.isChecked());
      this.reflectSky.withVisibility(() -> this.wetSurfaces.isChecked());
      this.customReflectionColor.withVisibility(() -> this.wetSurfaces.isChecked());
      this.reflectionColor.withVisibility(() -> this.wetSurfaces.isChecked() && this.customReflectionColor.isChecked());
      this.hitThickness.withVisibility(() -> this.wetSurfaces.isChecked());
      this.reflectionDistance.withVisibility(() -> this.wetSurfaces.isChecked());
      this.onlyInRain.withVisibility(() -> this.worldRain.isChecked());
      this.rainStreams.withVisibility(() -> this.worldRain.isChecked());
      this.rainDensity.withVisibility(() -> this.worldRain.isChecked() && this.rainStreams.getValue() > 0.0F);
      this.rainSpeed.withVisibility(() -> this.worldRain.isChecked() && this.rainStreams.getValue() > 0.0F);
      this.streamLength.withVisibility(() -> this.worldRain.isChecked() && this.rainStreams.getValue() > 0.0F);
      this.hideVanillaRain.withVisibility(() -> this.worldRain.isChecked());
      this.screenDroplets.withVisibility(() -> this.worldRain.isChecked());
      this.groundSplashes.withVisibility(() -> this.worldRain.isChecked());
      this.checkRoof.withVisibility(() -> this.worldRain.isChecked());
      this.customRainColor.withVisibility(() -> this.worldRain.isChecked());
      this.rainColor.withVisibility(() -> this.worldRain.isChecked() && this.customRainColor.isChecked());
   }

   public static CustomWorldModule getInstance() {
      if (customWorldModule == null) {
         customWorldModule = new CustomWorldModule();
      }

      return customWorldModule;
   }

   private void updateState() {
      if (this.gpuBuffer != null && !this.gpuBuffer.isClosed()) {
         this.gpuBuffer.close();
      }

      this.gpuBuffer = null;
      if (this.gpuBuffer2 != null && !this.gpuBuffer2.isClosed()) {
         this.gpuBuffer2.close();
      }

      this.gpuBuffer2 = null;
      if (this.framebuffer2 != null) {
         this.framebuffer2.delete();
         this.framebuffer2 = null;
      }
   }

   private void processData4(Matrix4f matrix4f, Matrix4f matrix4f2) {
      Framebuffer framebuffer = mc.getFramebuffer();
      if (framebuffer != null
         && framebuffer.getColorAttachmentView() != null
         && framebuffer.getDepthAttachmentView() != null
         && framebuffer.textureWidth > 0
         && framebuffer.textureHeight > 0) {
         if (this.gpuBuffer == null || this.gpuBuffer.isClosed()) {
            try (BufferAllocator bufferAllocator = new BufferAllocator(4 * VertexFormats.POSITION.getVertexSize())) {
               BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION);
               bufferBuilder.vertex(-1.0F, -1.0F, 0.0F);
               bufferBuilder.vertex(-1.0F, 1.0F, 0.0F);
               bufferBuilder.vertex(1.0F, 1.0F, 0.0F);
               bufferBuilder.vertex(1.0F, -1.0F, 0.0F);

               try (BuiltBuffer builtBuffer = bufferBuilder.end()) {
                  this.gpuBuffer = RenderSystem.getDevice().createBuffer(() -> "greed_haze_quad", 32, builtBuffer.getBuffer());
               }
            }
         }

         if (this.gpuBuffer2 == null || this.gpuBuffer2.isClosed()) {
            this.gpuBuffer2 = RenderSystem.getDevice().createBuffer(() -> "greed_haze_params", 136, 128L);
         }

         int value = this.fogColor.getValue();
         MemoryStack memoryStack = MemoryStack.stackPush();

         try {
            ByteBuffer byteBuffer = memoryStack.calloc(128);
            this.matrix4f3.set(matrix4f2).mul(matrix4f).invert().get(0, byteBuffer);
            byteBuffer.putFloat(64, (value >> 16 & 0xFF) / 255.0F);
            byteBuffer.putFloat(68, (value >> 8 & 0xFF) / 255.0F);
            byteBuffer.putFloat(72, (value & 0xFF) / 255.0F);
            byteBuffer.putFloat(76, (value >>> 24 & 0xFF) / 255.0F);
            byteBuffer.putFloat(80, this.fogDistance.getValue());
            byteBuffer.putFloat(84, this.fogDistance.getValue() * 0.3F);
            byteBuffer.putFloat(88, this.fogVolume.getValue());
            byteBuffer.putFloat(92, (float)(System.currentTimeMillis() % 1000000L) / 1000.0F);
            byteBuffer.putFloat(112, (float)mc.gameRenderer.getCamera().getCameraPos().x);
            byteBuffer.putFloat(116, (float)mc.gameRenderer.getCamera().getCameraPos().y);
            byteBuffer.putFloat(120, (float)mc.gameRenderer.getCamera().getCameraPos().z);
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.gpuBuffer2.slice(0L, 128L), byteBuffer);
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

         int local3447_5x = Math.max(1, framebuffer.textureWidth / 2);
         int local3447_6 = Math.max(1, framebuffer.textureHeight / 2);
         if (this.framebuffer2 == null) {
            this.framebuffer2 = new SimpleFramebuffer("greed_haze_layer", local3447_5x, local3447_6, false);
         }

         if (this.framebuffer2.textureWidth != local3447_5x || this.framebuffer2.textureHeight != local3447_6) {
            this.framebuffer2.resize(local3447_5x, local3447_6);
         }

         try (RenderPass renderPass = RenderSystem.getDevice()
               .createCommandEncoder()
               .createRenderPass(() -> "greed_haze", this.framebuffer2.getColorAttachmentView(), OptionalInt.empty(), null, OptionalDouble.empty())) {
            renderPass.setPipeline(pipeline12);
            renderPass.bindTexture("Sampler1", framebuffer.getDepthAttachmentView(), RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
            renderPass.setUniform("SkyPostParams", this.gpuBuffer2);
            renderPass.setVertexBuffer(0, this.gpuBuffer);
            renderPass.setIndexBuffer(
               RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexBuffer(6), RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexType()
            );
            renderPass.drawIndexed(0, 0, 6, 1);
         }

         try (RenderPass renderPass2 = RenderSystem.getDevice()
               .createCommandEncoder()
               .createRenderPass(() -> "greed_haze_blit", framebuffer.getColorAttachmentView(), OptionalInt.empty(), null, OptionalDouble.empty())) {
            renderPass2.setPipeline(pipeline13);
            renderPass2.bindTexture("Sampler0", this.framebuffer2.getColorAttachmentView(), RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
            renderPass2.bindTexture("Sampler1", framebuffer.getDepthAttachmentView(), RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
            renderPass2.setUniform("SkyPostParams", this.gpuBuffer2);
            renderPass2.setVertexBuffer(0, this.gpuBuffer);
            renderPass2.setIndexBuffer(
               RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexBuffer(6), RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexType()
            );
            renderPass2.drawIndexed(0, 0, 6, 1);
         }
      }
   }

   public void processData5(Matrix4f matrix4f, Matrix4f matrix4f2) {
      if (this.checkState4() && mc.world != null) {
         if (this.checkState3()) {
            this.processData4(matrix4f, matrix4f2);
         } else {
            this.updateState();
            float value = this.fogDistance.getValue();
            if (this.floatVal != value) {
               this.processData2(value);
            }

            int local3449_4 = this.fogColor.getValue() & 16777215;
            float value2 = this.fogVolume.getValue();
            this.quaternionf.set(mc.gameRenderer.getCamera().getRotation()).rotateY((float) Math.PI);
            Vec3d cameraPos = mc.gameRenderer.getCamera().getCameraPos();
            VertexConsumer vertexConsumer = this.immediate2.getBuffer(renderLayer);

            for (int index = 0; index < 80; index++) {
               int local3449_9 = index * 5;
               float local3449_10 = this.floatValArray[local3449_9 + 4];
               int local3449_11 = MathHelper.clamp((int)(value2 * (0.03F + local3449_10 * local3449_10 * 0.12F) * 255.0F), 0, 255);
               if (local3449_11 > 1) {
                  float local3449_12 = this.floatValArray[local3449_9 + 3] * 0.5F;
                  if (WorldProjection.matchesCriteria2(
                     cameraPos.x + this.floatValArray[local3449_9],
                     cameraPos.y + this.floatValArray[local3449_9 + 1],
                     cameraPos.z + this.floatValArray[local3449_9 + 2],
                     local3449_12 * 1.42F
                  )) {
                     int local3449_13 = local3449_11 << 24 | local3449_4;
                     this.processData3(vertexConsumer, matrix4f, local3449_9, -local3449_12, -local3449_12, 0.0F, 0.0F, local3449_13);
                     this.processData3(vertexConsumer, matrix4f, local3449_9, -local3449_12, local3449_12, 0.0F, 1.0F, local3449_13);
                     this.processData3(vertexConsumer, matrix4f, local3449_9, local3449_12, local3449_12, 1.0F, 1.0F, local3449_13);
                     this.processData3(vertexConsumer, matrix4f, local3449_9, local3449_12, -local3449_12, 1.0F, 0.0F, local3449_13);
                  }
               }
            }

            this.immediate2.draw();
         }
      } else {
         this.updateState();
      }
   }

   public boolean checkState3() {
      return this.checkState4() && this.fogType.isSelected("Дымка");
   }

   public void updateState2() {
      if (this.gpuBuffer3 != null && !this.gpuBuffer3.isClosed()) {
         this.gpuBuffer3.close();
      }

      this.gpuBuffer3 = null;
      if (this.gpuBuffer4 != null && !this.gpuBuffer4.isClosed()) {
         this.gpuBuffer4.close();
      }

      this.gpuBuffer4 = null;
      if (this.gpuBuffer5 != null && !this.gpuBuffer5.isClosed()) {
         this.gpuBuffer5.close();
      }

      this.gpuBuffer5 = null;
      if (this.framebuffer3 != null) {
         this.framebuffer3.delete();
         this.framebuffer3 = null;
      }
   }

   public static int computeInt() {
      int local3456_0 = Math.round(Math.min(Math.max(floatValArray2[0], 0.0F), 1.0F) * 255.0F);
      int local3456_1 = Math.round(Math.min(Math.max(floatValArray2[1], 0.0F), 1.0F) * 255.0F);
      int local3456_2 = Math.round(Math.min(Math.max(floatValArray2[2], 0.0F), 1.0F) * 255.0F);
      return 0xFF000000 | local3456_0 << 16 | local3456_1 << 8 | local3456_2;
   }

   private static RenderPipeline getRenderPipeline(String local3460_0) {
      return RenderPipeline.builder()
         .withLocation(GreedClient.id("pipeline/" + local3460_0))
         .withVertexShader(GreedClient.id("core/sky_post"))
         .withFragmentShader(GreedClient.id("core/" + local3460_0))
         .withVertexFormat(VertexFormats.POSITION, DrawMode.QUADS)
         .withBlend(BlendFunction.TRANSLUCENT)
         .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
         .withDepthWrite(false)
         .withCull(false)
         .withUniform("SkyPostParams", UniformType.UNIFORM_BUFFER)
         .build();
   }

   public boolean checkState4() {
      return this.isEnabled() && this.fog.isChecked();
   }
}
