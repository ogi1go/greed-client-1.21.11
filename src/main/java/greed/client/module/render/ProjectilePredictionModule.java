package greed.client.module.render;

import greed.client.module.combat.AttackAuraModule;
import greed.client.setting.BooleanSetting;
import greed.client.util.Colors;
import greed.client.GreedClient;
import greed.client.util.DepthSnapshot;
import greed.client.event.EventListener;
import greed.client.render.FontFace;
import greed.client.event.HudRenderEvent;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.util.GuiScale;
import greed.client.render.WorldProjection;
import greed.client.render.FontText;
import greed.client.render.ItemRenderCache;
import greed.client.setting.ColorSetting;
import greed.client.render.ShapeDrawing;
import greed.client.render.ThemeColors;
import greed.client.render.CornerRadii;
import greed.client.ui.hud.HudRenderContext;
import greed.client.ui.hud.HudStyle;
import greed.client.event.WorldRenderEvent;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.EggItem;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.ExperienceBottleItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.LingeringPotionItem;
import net.minecraft.item.SnowballItem;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.item.TridentItem;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@ModuleInfo(
   name = "ProjectilePrediction",
   description = "Показывает, куда прилетит снаряд из руки: траектория, маркер приземления и радиус действия, а также траектории летящих снарядов.",
   category = ModuleCategory.RENDER
)
public final class ProjectilePredictionModule extends Module {
   private static ProjectilePredictionModule projectilePredictionModule;
   public BooleanSetting booleanSetting;
   public ColorSetting colorSetting;
   private static final RenderPipeline ribbonPipeline = RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
      .withLocation(GreedClient.id("pipeline/projectile_prediction_ribbon"))
      .withVertexShader(Identifier.ofVanilla("core/position_color"))
      .withFragmentShader(Identifier.ofVanilla("core/position_color"))
      .withVertexFormat(VertexFormats.POSITION_COLOR, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .build();
   private static final RenderLayer renderLayer = RenderLayer.of(
      "projectile_prediction_ribbon", RenderSetup.builder(ribbonPipeline).expectedBufferSize(32768).build()
   );
   private final float[] predictionAlpha = new float[8];
   private final ArrayList<ProjectilePredictionModule.DataRecord2> trackedProjectiles = new ArrayList<>();
   private final Int2ObjectOpenHashMap<ProjectilePredictionModule.SubContext> subContexts = new Int2ObjectOpenHashMap();
   private final Immediate immediate = VertexConsumerProvider.immediate(new BufferAllocator(131072));
   private ClientWorld clientWorld;
   private int playerAge = Integer.MIN_VALUE;
   private long lastUpdateNanos;
   private Vec3d lastPlayerPos;
   private final EventListener<WorldRenderEvent> worldRenderListener = worldRenderEvent -> {
      if (mc.player != null && mc.world != null) {
         ArrayList<ProjectilePredictionModule.DataRecord5> nearbyTargets = new ArrayList<>();

         for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof LivingEntity livingEntity && livingEntity != mc.player && livingEntity.isAlive()) {
               nearbyTargets.add(new ProjectilePredictionModule.DataRecord5(livingEntity, livingEntity.getBoundingBox().expand(0.3)));
            }
         }

         ArrayList<ProjectilePredictionModule.DataRecord4> predictions = new ArrayList<>();

         for (Hand hand : Hand.values()) {
            this.predictHeldProjectile(mc.player.getStackInHand(hand), hand, worldRenderEvent.computeFloat(), nearbyTargets, predictions);
         }

         if (mc.world != this.clientWorld || mc.player.age != this.playerAge) {
            this.scanProjectiles(nearbyTargets);
         }

         this.syncContexts(predictions, worldRenderEvent.computeFloat());

         for (int index = 0; index < predictions.size(); index++) {
            this.predictionAlpha[index % this.predictionAlpha.length] = this.predictionAlpha[index % this.predictionAlpha.length]
               + (
                     (predictions.get(index).landing().hitTarget() == null && !this.isNearAnyTarget(predictions.get(index), nearbyTargets) ? 0.0F : 1.0F)
                        - this.predictionAlpha[index % this.predictionAlpha.length]
                  )
                  * 0.3F;
         }

         if (!this.subContexts.isEmpty()) {
            DepthSnapshot.updateState2();
            Vec3d cameraPos = mc.gameRenderer.getCamera().getCameraPos();
            double pixelScale = 2.0
               * Math.tan(Math.toRadians(mc.options.getFov().getValue().intValue()) / 2.0)
               / Math.max(1, mc.getWindow().getFramebufferHeight());
            ObjectIterator objectIterator = this.subContexts.int2ObjectEntrySet().iterator();

            while (objectIterator.hasNext()) {
               Entry<ProjectilePredictionModule.SubContext> mapEntry = (Entry<ProjectilePredictionModule.SubContext>)objectIterator.next();
               ProjectilePredictionModule.SubContext subContext = (ProjectilePredictionModule.SubContext)mapEntry.getValue();
               if (mapEntry.getIntKey() < 0 || this.isVisibleOnScreen(subContext)) {
                  int ribbonColor = mapEntry.getIntKey() < 0
                     ? this.getPredictionColor(-1 - mapEntry.getIntKey())
                     : (subContext.hitTarget ? -54742 : ThemeColors.intVal9);
                  if (this.buildRibbonGeometry(subContext, cameraPos, pixelScale, ribbonColor)) {
                     renderRibbon(this.immediate.getBuffer(renderLayer), worldRenderEvent.getMatrixStack().peek().getPositionMatrix(), subContext.ribbon);
                  }
               }
            }

            this.immediate.draw();

            for (int index2 = 0; index2 < predictions.size(); index2++) {
               ProjectilePredictionModule.DataRecord4 prediction = predictions.get(index2);
               ProjectilePredictionModule.SubContext predictedContext = (ProjectilePredictionModule.SubContext)(Object)this.subContexts.get(-1 - index2);
               Vec3d vec3d = predictedContext != null ? predictedContext.anchorPos : prediction.landing().pos();
               this.drawLandingMarker(worldRenderEvent.getMatrixStack(), vec3d, this.getPredictionColor(index2));
               if (this.booleanSetting.isChecked() && prediction.areaRadiusBlocks() > 0.0F) {
                  this.drawAreaRadius(
                     worldRenderEvent.getMatrixStack(), vec3d, prediction.areaRadiusBlocks(), Colors.computeInt6(this.getPredictionColor(index2), 0.5F)
                  );
               }
            }

            mc.getBufferBuilders().getEntityVertexConsumers().draw();
            ArrayList<LivingEntity> hitEntities = new ArrayList<>();

            for (ProjectilePredictionModule.DataRecord4 predictionEntry : predictions) {
               if (predictionEntry.landing().hitTarget() != null && !hitEntities.contains(predictionEntry.landing().hitTarget())) {
                  hitEntities.add(predictionEntry.landing().hitTarget());
               }
            }

            for (ProjectilePredictionModule.DataRecord2 trackedProjectile : this.trackedProjectiles) {
               if (trackedProjectile.landing().hitTarget() != null && !hitEntities.contains(trackedProjectile.landing().hitTarget())) {
                  hitEntities.add(trackedProjectile.landing().hitTarget());
               }
            }

            for (LivingEntity livingEntity2 : hitEntities) {
               if (WorldProjection.matchesCriteria(livingEntity2.getBoundingBox())) {
                  this.drawTargetOutline(worldRenderEvent.getMatrixStack(), livingEntity2, worldRenderEvent.computeFloat());
               }
            }

            mc.getBufferBuilders().getEntityVertexConsumers().draw();
         }
      }
   };
   private String auraTargetName = "";
   private final EventListener<HudRenderEvent> hudRenderListener = hudRenderEvent -> {
      if (mc.player != null && mc.world != null) {
         HudRenderContext.renderElement(hudRenderEvent.getDrawContext());
         float guiScale = GuiScale.computeInt() / GuiScale.computeFloat();
         if (AttackAuraModule.getInstance().isEnabled() && AttackAuraModule.getInstance().getTarget() != null) {
            this.auraTargetName = AttackAuraModule.getInstance().getTarget().getName().getString();
         }

         ObjectIterator objectIterator2 = this.subContexts.int2ObjectEntrySet().iterator();

         while (objectIterator2.hasNext()) {
            Entry<ProjectilePredictionModule.SubContext> contextEntry = (Entry<ProjectilePredictionModule.SubContext>)objectIterator2.next();
            ProjectilePredictionModule.SubContext hudSubContext = (ProjectilePredictionModule.SubContext)contextEntry.getValue();
            if (!hudSubContext.icon.isEmpty()) {
               Vec3d vec3d2 = WorldProjection.getVec3d2(hudSubContext.anchorPos.add(0.0, 0.25, 0.0), hudRenderEvent.computeInt2(), hudRenderEvent.computeInt());
               if (vec3d2 != null) {
                  String ownerName = contextEntry.getIntKey() >= 0
                        && mc.world.getEntityById(contextEntry.getIntKey()) instanceof ProjectileEntity projectileEntity
                        && projectileEntity.getOwner() != null
                        && projectileEntity.getOwner() != mc.player
                     ? projectileEntity.getOwner().getName().getString()
                     : "";
                  float screenX = (float)vec3d2.x * guiScale;
                  float screenY = (float)vec3d2.y * guiScale;
                  boolean isTargetOwner = !ownerName.isEmpty() && ownerName.equals(this.auraTargetName);
                  FontFace fontFace = isTargetOwner ? FontFace.BOLD : FontFace.SEMI;
                  float ownerNameWidth = ownerName.isEmpty() ? 0.0F : FontText.computeFloat(fontFace, ownerName, 6.0F) + 3.0F;
                  float timerTextWidth = FontText.computeFloat(FontFace.SEMI, hudSubContext.flightTimeText, 6.0F);
                  float badgeWidth = 14.0F + ownerNameWidth + timerTextWidth + 4.0F;
                  float badgeHeight = 11.0F;
                  float badgeX = Math.round((screenX - badgeWidth / 2.0F) * 2.0F) / 2.0F;
                  float badgeY = Math.round((screenY - badgeHeight / 2.0F) * 2.0F) / 2.0F;
                  hudRenderEvent.getDrawContext().createNewRootLayer();
                  HudRenderContext.processData3(
                     screenX,
                     screenY,
                     Math.max(0.6F, 1.1F - (float)mc.gameRenderer.getCamera().getCameraPos().distanceTo(hudSubContext.anchorPos) * 0.004F)
                  );
                  ShapeDrawing.processData7(
                     ShapeDrawing.TypeOption.LOW,
                     true,
                     badgeX,
                     badgeY,
                     badgeWidth,
                     badgeHeight,
                     1.0F,
                     CornerRadii.uniform(3.5F),
                     HudStyle.computeInt3(),
                     0.65F,
                     Colors.computeInt2(-16777216, ThemeColors.intVal9, 0.1F)
                  );
                  ItemRenderCache.processData3(
                     hudSubContext.icon,
                     Math.round((badgeX + 3.0F) * 2.0F) / 2.0F,
                     Math.round((badgeY + (badgeHeight - 8.0F) / 2.0F) * 2.0F) / 2.0F,
                     8.0F
                  );
                  float textY = HudStyle.computeFloat3(badgeY);
                  if (!ownerName.isEmpty()) {
                     FontText.processData3(
                        fontFace, ownerName, badgeX + 14.0F, textY, 6.0F, isTargetOwner ? -42386 : HudStyle.computeInt4(1.0F)
                     );
                  }

                  FontText.processData3(
                     FontFace.SEMI,
                     hudSubContext.flightTimeText,
                     badgeX + 14.0F + ownerNameWidth,
                     textY,
                     6.0F,
                     HudStyle.computeInt2(1.0F)
                  );
                  HudRenderContext.updateState();
               }
            }
         }
      }
   };
   private final ArrayList<Vec3d> currentFlightPath = new ArrayList<>();

   public static ProjectilePredictionModule getInstance() {
      if (projectilePredictionModule == null) {
         projectilePredictionModule = new ProjectilePredictionModule();
      }

      return projectilePredictionModule;
   }

   private ProjectilePredictionModule.DataRecord6 traceFlightPath(
      Vec3d vec3d,
      Vec3d vec3d2,
      double gravity,
      double drag,
      double waterDrag,
      boolean thrownItemPhysics,
      Entity entity,
      Entity entity2,
      ArrayList<ProjectilePredictionModule.DataRecord5> targetCandidates
   ) {
      this.currentFlightPath.clear();
      this.currentFlightPath.add(vec3d);
      Vec3d vec3d3 = vec3d;
      Vec3d vec3d4 = vec3d2;

      for (int index = 0; index < 300; index++) {
         Vec3d vec3d5 = vec3d3;
         double currentDrag = mc.world.getBlockState(BlockPos.ofFloored(vec3d5)).getFluidState().isIn(FluidTags.WATER)
            ? waterDrag
            : drag;
         if (thrownItemPhysics) {
            vec3d4 = vec3d4.subtract(0.0, gravity, 0.0).multiply(currentDrag);
         }

         vec3d3 = vec3d3.add(vec3d4);
         if (!thrownItemPhysics) {
            vec3d4 = vec3d4.multiply(currentDrag).subtract(0.0, gravity, 0.0);
         }

         BlockHitResult blockHitResult = mc.world
            .raycast(new RaycastContext(vec3d5, vec3d3, ShapeType.COLLIDER, FluidHandling.NONE, entity));
         Vec3d vec3d6 = blockHitResult.getType() != Type.MISS ? blockHitResult.getPos() : vec3d3;
         this.currentFlightPath.add(vec3d6);

         for (ProjectilePredictionModule.DataRecord5 targetCandidate : targetCandidates) {
            if (targetCandidate.target() != entity2 && targetCandidate.box().intersects(vec3d5, vec3d6)) {
               BlockHitResult blockHitResult2 = Box.raycast(List.of(targetCandidate.box()), vec3d5, vec3d6, BlockPos.ORIGIN);
               ProjectilePredictionModule.DataRecord6 projectileHit = blockHitResult2 != null
                  ? new ProjectilePredictionModule.DataRecord6(blockHitResult2.getPos(), targetCandidate.target())
                  : new ProjectilePredictionModule.DataRecord6(
                     targetCandidate.box().raycast(vec3d5, vec3d6).orElse(vec3d6), targetCandidate.target()
                  );
               this.currentFlightPath.set(this.currentFlightPath.size() - 1, projectileHit.pos());
               return projectileHit;
            }
         }

         if (blockHitResult.getType() != Type.MISS) {
            return new ProjectilePredictionModule.DataRecord6(blockHitResult.getPos(), null);
         }

         if (vec3d3.y < -128.0) {
            break;
         }
      }

      return null;
   }

   private boolean hasDonItemTag(ItemStack itemStack) {
      return itemStack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt().contains("don-item");
   }

   private void updateContext(
      int contextKey, ArrayList<Vec3d> flightPath, ProjectilePredictionModule.DataRecord6 landing, ItemStack itemStack, float deltaTime
   ) {
      ProjectilePredictionModule.SubContext subContext = (ProjectilePredictionModule.SubContext)(Object)this.subContexts.get(contextKey);
      if (subContext == null) {
         subContext = new ProjectilePredictionModule.SubContext();
         this.subContexts.put(contextKey, subContext);
      }

      subContext.updatePath(flightPath, landing.pos(), landing.hitTarget() != null, itemStack, deltaTime);
      subContext.isActive = true;
   }

   private void predictHeldProjectile(
      ItemStack itemStack,
      Hand hand,
      float tickDelta,
      ArrayList<ProjectilePredictionModule.DataRecord5> targetCandidates,
      ArrayList<ProjectilePredictionModule.DataRecord4> predictions
   ) {
      Item item = itemStack.getItem();
      Vec3d vec3d = new Vec3d(mc.player.getX(), mc.player.getEyeY() - 0.1, mc.player.getZ());
      if (item instanceof BowItem && mc.player.isUsingItem() && mc.player.getActiveItem() == itemStack) {
         float pullProgress = BowItem.getPullProgress(mc.player.getItemUseTime());
         if (pullProgress >= 0.1F) {
            this.addPrediction(
               predictions,
               this.traceFlightPath(
                  vec3d, this.getVec3d(0.0F, pullProgress * 3.0F), 0.05, 0.99F, 0.6F, false, mc.player, mc.player, targetCandidates
               ),
               0.0F,
               new ItemStack(Items.ARROW),
               hand,
               tickDelta
            );
         }
      } else if (item instanceof CrossbowItem && CrossbowItem.isCharged(itemStack)) {
         ChargedProjectilesComponent orDefault = itemStack.getOrDefault(DataComponentTypes.CHARGED_PROJECTILES, ChargedProjectilesComponent.DEFAULT);
         List<ItemStack> projectiles = orDefault.getProjectiles();
         float projectileSpeed = orDefault.contains(Items.FIREWORK_ROCKET) ? 1.6F : 3.15F;
         float spreadAngle = projectiles.size() <= 1 ? 0.0F : 20.0F / (projectiles.size() - 1);
         Vec3d oppositeRotationVector = mc.player.getOppositeRotationVector(1.0F);

         for (int index = 0; index < projectiles.size(); index++) {
            float angleOffset = (projectiles.size() - 1) % 2 * spreadAngle / 2.0F
               + (index % 2 == 0 ? 1 : -1) * ((index + 1) / 2) * spreadAngle;
            Vector3f vector3f = mc.player
               .getRotationVec(1.0F)
               .toVector3f()
               .rotate(new Quaternionf().setAngleAxis(angleOffset * (float) (Math.PI / 180.0), oppositeRotationVector.x, oppositeRotationVector.y, oppositeRotationVector.z));
            Vec3d vec3d2 = new Vec3d(vector3f.x(), vector3f.y(), vector3f.z()).normalize().multiply(projectileSpeed);
            if (projectiles.get(index).isOf(Items.FIREWORK_ROCKET)) {
               this.addPrediction(
                  predictions,
                  this.traceFlightPath(
                     new Vec3d(mc.player.getX(), mc.player.getEyeY() - 0.15, mc.player.getZ()),
                     vec3d2,
                     0.0,
                     1.0,
                     1.0,
                     false,
                     mc.player,
                     mc.player,
                     targetCandidates
                  ),
                  0.0F,
                  projectiles.get(index),
                  hand,
                  tickDelta
               );
            } else {
               this.addPrediction(
                  predictions,
                  this.traceFlightPath(vec3d, vec3d2, 0.05, 0.99F, 0.6F, false, mc.player, mc.player, targetCandidates),
                  0.0F,
                  projectiles.get(index),
                  hand,
                  tickDelta
               );
            }
         }
      } else if (!(item instanceof SnowballItem) && !(item instanceof EggItem) && !(item instanceof EnderPearlItem)) {
         if (item instanceof TridentItem
            && mc.player.isUsingItem()
            && mc.player.getActiveItem() == itemStack
            && mc.player.getItemUseTime() >= 10) {
            this.addPrediction(
               predictions,
               this.traceFlightPath(vec3d, this.getVec3d(0.0F, 2.5F), 0.05, 0.99F, 0.99F, false, mc.player, mc.player, targetCandidates),
               0.0F,
               itemStack,
               hand,
               tickDelta
            );
         } else if (item instanceof SplashPotionItem || item instanceof LingeringPotionItem) {
            this.addPrediction(
               predictions,
               this.traceFlightPath(vec3d, this.getVec3d(-20.0F, 0.5F), 0.05, 0.99F, 0.8F, true, mc.player, mc.player, targetCandidates),
               item instanceof LingeringPotionItem ? 4.0F : 3.0F,
               ItemStack.EMPTY,
               hand,
               tickDelta
            );
         } else if (item instanceof ExperienceBottleItem) {
            this.addPrediction(
               predictions,
               this.traceFlightPath(vec3d, this.getVec3d(-20.0F, 0.7F), 0.07, 0.99F, 0.8F, true, mc.player, mc.player, targetCandidates),
               0.0F,
               ItemStack.EMPTY,
               hand,
               tickDelta
            );
         }
      } else {
         this.addPrediction(
            predictions,
            this.traceFlightPath(vec3d, this.getVec3d(0.0F, 1.5F), 0.03, 0.99F, 0.8F, true, mc.player, mc.player, targetCandidates),
            item instanceof SnowballItem && this.hasDonItemTag(itemStack) ? 7.0F : 0.0F,
            item instanceof EnderPearlItem ? itemStack : ItemStack.EMPTY,
            hand,
            tickDelta
         );
      }
   }

   private Vec3d getVec3d(float pitchOffset, float speed) {
      float pitch = mc.player.getPitch() * (float) (Math.PI / 180.0);
      float yaw = mc.player.getYaw() * (float) (Math.PI / 180.0);
      Vec3d vec3d = new Vec3d(
            -MathHelper.sin(yaw) * MathHelper.cos(pitch),
            -MathHelper.sin((mc.player.getPitch() + pitchOffset) * (float) (Math.PI / 180.0)),
            MathHelper.cos(yaw) * MathHelper.cos(pitch)
         )
         .normalize()
         .multiply(speed);
      Vec3d vec3d2 = mc.player.getEntityPos().subtract(mc.player.lastX, mc.player.lastY, mc.player.lastZ);
      return vec3d.add(vec3d2.x, mc.player.isOnGround() ? 0.0 : vec3d2.y, vec3d2.z);
   }

   private ProjectilePredictionModule() {
      this.booleanSetting = this.addSetting(new BooleanSetting("Радиус действия", "Рисует зону поражения зелий и особых снежков.", true));
      this.colorSetting = this.addSetting(new ColorSetting("Цвет", "Цвет маркера приземления.").withArgb(-12417035));
   }

   private void syncContexts(ArrayList<ProjectilePredictionModule.DataRecord4> predictions, float tickDelta) {
      Vec3d lerpedPos = mc.player.getLerpedPos(tickDelta);
      Vec3d vec3d = this.lastPlayerPos == null ? Vec3d.ZERO : lerpedPos.subtract(this.lastPlayerPos);
      this.lastPlayerPos = lerpedPos;
      if (vec3d.lengthSquared() < 64.0) {
         ObjectIterator objectIterator = this.subContexts.int2ObjectEntrySet().iterator();

         while (objectIterator.hasNext()) {
            Entry<ProjectilePredictionModule.SubContext> mapEntry = (Entry<ProjectilePredictionModule.SubContext>)objectIterator.next();
            if (mapEntry.getIntKey() < 0) {
               ((ProjectilePredictionModule.SubContext)mapEntry.getValue()).offsetPath(vec3d);
            }
         }
      }

      float deltaTime = this.lastUpdateNanos == 0L ? 0.05F : Math.min(0.1F, (float)(System.nanoTime() - this.lastUpdateNanos) / 1.0E9F);
      this.lastUpdateNanos = System.nanoTime();
      ObjectIterator objectIterator2 = this.subContexts.values().iterator();

      while (objectIterator2.hasNext()) {
         ProjectilePredictionModule.SubContext subContext = (ProjectilePredictionModule.SubContext)objectIterator2.next();
         subContext.isActive = false;
      }

      for (int index = 0; index < predictions.size(); index++) {
         this.updateContext(
            -1 - index,
            predictions.get(index).flightPath(),
            predictions.get(index).landing(),
            predictions.get(index).icon(),
            deltaTime
         );
      }

      for (ProjectilePredictionModule.DataRecord2 trackedProjectile : this.trackedProjectiles) {
         this.updateContext(trackedProjectile.entityId(), trackedProjectile.path(), trackedProjectile.landing(), trackedProjectile.icon(), deltaTime);
      }

      this.subContexts.values().removeIf(subContext -> !subContext.isActive);
   }

   private void addPrediction(
      ArrayList<ProjectilePredictionModule.DataRecord4> predictions,
      ProjectilePredictionModule.DataRecord6 landing,
      float areaRadius,
      ItemStack itemStack,
      Hand hand,
      float tickDelta
   ) {
      if (landing != null) {
         Vec3d vec3d = mc.player.getLerpedPos(tickDelta).subtract(mc.player.getEntityPos());
         ArrayList<Vec3d> flightPath = new ArrayList<>(this.currentFlightPath.size());

         for (Vec3d vec3d2 : this.currentFlightPath) {
            flightPath.add(vec3d2.add(vec3d));
         }

         landing = new ProjectilePredictionModule.DataRecord6(landing.pos().add(vec3d), landing.hitTarget());
         Vec3d rotationVec = mc.player.getRotationVec(tickDelta);
         Vec3d vec3d3 = Vec3d.fromPolar(0.0F, mc.player.getYaw(tickDelta) + 90.0F).multiply(hand == Hand.OFF_HAND ? -0.32 : 0.32);
         int sampleCount = Math.min(5, flightPath.size());

         for (int index = 0; index < sampleCount; index++) {
            float falloff = (1.0F - (float)index / sampleCount) * (1.0F - (float)index / sampleCount);
            flightPath.set(
               index,
               flightPath.get(index)
                  .add(
                     (vec3d3.x + rotationVec.x * 0.25) * falloff,
                     (vec3d3.y + rotationVec.y * 0.25 - 0.18) * falloff,
                     (vec3d3.z + rotationVec.z * 0.25) * falloff
                  )
            );
         }

         predictions.add(new ProjectilePredictionModule.DataRecord4(landing, areaRadius, flightPath, itemStack));
      }
   }

   private void drawRadiusCircle(VertexConsumer vertexConsumer, MatrixStack matrixStack, float radius, int segments, int color, float lineWidth) {
      for (int index = 0; index < segments; index++) {
         float angleStart = (float)(index * Math.PI * 2.0 / segments);
         float angleEnd = (float)((index + 1) * Math.PI * 2.0 / segments);
         float xStart = MathHelper.cos(angleStart) * radius;
         float zStart = MathHelper.sin(angleStart) * radius;
         float xEnd = MathHelper.cos(angleEnd) * radius;
         float zEnd = MathHelper.sin(angleEnd) * radius;
         float normalX = (xEnd - xStart)
            / MathHelper.sqrt((xEnd - xStart) * (xEnd - xStart) + (zEnd - zStart) * (zEnd - zStart));
         float normalZ = (zEnd - zStart)
            / MathHelper.sqrt((xEnd - xStart) * (xEnd - xStart) + (zEnd - zStart) * (zEnd - zStart));
         vertexConsumer.vertex(matrixStack.peek(), xStart, 0.0F, zStart)
            .color(color)
            .normal(matrixStack.peek(), normalX, 0.0F, normalZ)
            .lineWidth(lineWidth);
         vertexConsumer.vertex(matrixStack.peek(), xEnd, 0.0F, zEnd)
            .color(color)
            .normal(matrixStack.peek(), normalX, 0.0F, normalZ)
            .lineWidth(lineWidth);
      }
   }

   private static int lightenChannel(int channel, float progress) {
      return channel + (int)((255 - channel) * 0.45F * progress);
   }

   private void scanProjectiles(ArrayList<ProjectilePredictionModule.DataRecord5> targetCandidates) {
      this.trackedProjectiles.clear();
      this.clientWorld = mc.world;
      this.playerAge = mc.player.age;

      for (Entity entity : mc.world.getEntities()) {
         if ((entity instanceof PersistentProjectileEntity || entity instanceof ThrownItemEntity || entity instanceof ItemEntity)
            && !entity.isOnGround()
            && !entity.noClip
            && !(entity.getVelocity().lengthSquared() < 1.0E-4)) {
            ProjectilePredictionModule.DataRecord6 landing = this.traceFlightPath(
               entity.getEntityPos(),
               entity.getVelocity(),
               entity.getFinalGravity(),
               0.99F,
               entity instanceof TridentEntity ? 0.99F : (entity instanceof PersistentProjectileEntity ? 0.6F : 0.8F),
               entity instanceof ThrownItemEntity,
               entity,
               entity instanceof ProjectileEntity projectileEntity ? projectileEntity.getOwner() : null,
               targetCandidates
            );
            if (landing != null && this.currentFlightPath.size() > 2) {
               ItemStack itemStack = entity instanceof EnderPearlEntity
                  ? new ItemStack(Items.ENDER_PEARL)
                  : (
                     entity instanceof TridentEntity
                        ? new ItemStack(Items.TRIDENT)
                        : (entity instanceof PersistentProjectileEntity ? new ItemStack(Items.ARROW) : ItemStack.EMPTY)
                  );
               this.trackedProjectiles.add(new ProjectilePredictionModule.DataRecord2(entity.getId(), landing, new ArrayList<>(this.currentFlightPath), itemStack));
            }
         }
      }
   }

   private int getPredictionColor(int index) {
      return Colors.computeInt2(this.colorSetting.getValue(), -54742, this.predictionAlpha[index % this.predictionAlpha.length]);
   }

   @Override
   protected void onDisable() {
      this.trackedProjectiles.clear();
      this.subContexts.clear();
      this.clientWorld = null;
      this.playerAge = Integer.MIN_VALUE;
      this.lastUpdateNanos = 0L;
      this.lastPlayerPos = null;
   }

   private void drawLandingMarker(MatrixStack matrixStack, Vec3d vec3d, int color) {
      Vec3d cameraPos = mc.gameRenderer.getCamera().getCameraPos();
      matrixStack.push();
      matrixStack.translate(vec3d.x - cameraPos.x, vec3d.y - cameraPos.y, vec3d.z - cameraPos.z);
      Matrix4f matrix4f = matrixStack.peek().getPositionMatrix();
      VertexConsumer vertexConsumer = mc.getBufferBuilders().getEntityVertexConsumers().getBuffer(RenderLayers.debugQuads());
      int argb = color & 16777215 | 771751936;

      for (int index = 0; index < 10; index++) {
         float phi = (float)(index * Math.PI / 10.0);
         float phiNext = (float)((index + 1) * Math.PI / 10.0);

         for (int index2 = 0; index2 < 14; index2++) {
            float theta = (float)(index2 * Math.PI * 2.0 / 14.0);
            float thetaNext = (float)((index2 + 1) * Math.PI * 2.0 / 14.0);
            emitSphereVertex(vertexConsumer, matrix4f, phi, theta, argb);
            emitSphereVertex(vertexConsumer, matrix4f, phiNext, theta, argb);
            emitSphereVertex(vertexConsumer, matrix4f, phiNext, thetaNext, argb);
            emitSphereVertex(vertexConsumer, matrix4f, phi, thetaNext, argb);
         }
      }

      VertexConsumer vertexConsumer2 = mc.getBufferBuilders().getEntityVertexConsumers().getBuffer(RenderLayers.lines());
      this.drawRadiusCircle(vertexConsumer2, matrixStack, 0.1F, 32, 0xFF000000 | color, 2.0F);
      matrixStack.push();
      matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
      this.drawRadiusCircle(vertexConsumer2, matrixStack, 0.1F, 32, 0xFF000000 | color, 2.0F);
      matrixStack.pop();
      matrixStack.multiply(mc.gameRenderer.getCamera().getRotation());
      matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
      this.drawRadiusCircle(vertexConsumer2, matrixStack, 0.1F, 32, 0xFF000000 | color, 2.0F);
      matrixStack.pop();
   }

   private static void emitVertex(
      VertexConsumer vertexConsumer,
      Matrix4f matrix4f,
      double x,
      double y,
      double z,
      int r,
      int g,
      int b,
      int a
   ) {
      vertexConsumer.vertex(matrix4f, (float)x, (float)y, (float)z).color(r, g, b, a);
   }

   private boolean isNearAnyTarget(ProjectilePredictionModule.DataRecord4 prediction, ArrayList<ProjectilePredictionModule.DataRecord5> targetCandidates) {
      if (prediction.areaRadiusBlocks() <= 0.0F) {
         return false;
      }

      for (ProjectilePredictionModule.DataRecord5 targetCandidate : targetCandidates) {
         if (targetCandidate.target().getBoundingBox().getCenter().squaredDistanceTo(prediction.landing().pos())
            <= prediction.areaRadiusBlocks() * prediction.areaRadiusBlocks()) {
            return true;
         }
      }

      return false;
   }

   private boolean isVisibleOnScreen(ProjectilePredictionModule.SubContext subContext) {
      double minX = Double.MAX_VALUE;
      double minY = Double.MAX_VALUE;
      double minZ = Double.MAX_VALUE;
      double maxX = -Double.MAX_VALUE;
      double maxY = -Double.MAX_VALUE;
      double maxZ = -Double.MAX_VALUE;

      for (Vec3d vec3d : subContext.flightPath) {
         minX = Math.min(minX, vec3d.x);
         minY = Math.min(minY, vec3d.y);
         minZ = Math.min(minZ, vec3d.z);
         maxX = Math.max(maxX, vec3d.x);
         maxY = Math.max(maxY, vec3d.y);
         maxZ = Math.max(maxZ, vec3d.z);
      }

      return subContext.flightPath.isEmpty()
         || WorldProjection.matchesCriteria(new Box(minX, minY, minZ, maxX, maxY, maxZ).expand(0.5));
   }

   private void drawTargetOutline(MatrixStack matrixStack, LivingEntity livingEntity, float tickDelta) {
      Box boundingBox = livingEntity.getBoundingBox();
      Vec3d vec3d = livingEntity.getLerpedPos(tickDelta)
         .subtract(livingEntity.getEntityPos())
         .subtract(mc.gameRenderer.getCamera().getCameraPos());
      matrixStack.push();
      matrixStack.translate(boundingBox.minX + vec3d.x, boundingBox.minY + vec3d.y, boundingBox.minZ + vec3d.z);
      Matrix4f positionMatrix = matrixStack.peek().getPositionMatrix();
      VertexConsumer vertexConsumer = mc.getBufferBuilders().getEntityVertexConsumers().getBuffer(RenderLayers.debugQuads());
      float[] floatArray = new float[]{0.0F, (float)boundingBox.getLengthX()};
      float[] floatArray2 = new float[]{0.0F, (float)boundingBox.getLengthY()};
      float[] floatArray3 = new float[]{0.0F, (float)boundingBox.getLengthZ()};
      int[][] intArrayArray = new int[][]{{0, 1, 3, 2}, {4, 6, 7, 5}, {0, 4, 5, 1}, {2, 3, 7, 6}, {0, 2, 6, 4}, {1, 5, 7, 3}};

      for (int[] intArray : intArrayArray) {
         for (int cornerIndex : intArray) {
            vertexConsumer.vertex(positionMatrix, floatArray[cornerIndex >> 2], floatArray2[cornerIndex >> 1 & 1], floatArray3[cornerIndex & 1])
               .color(788486766);
         }
      }

      VertexConsumer vertexConsumer2 = mc.getBufferBuilders().getEntityVertexConsumers().getBuffer(RenderLayers.lines());
      VertexRendering.drawOutline(
         matrixStack,
         vertexConsumer2,
         VoxelShapes.cuboid(new Box(0.0, 0.0, 0.0, boundingBox.getLengthX(), boundingBox.getLengthY(), boundingBox.getLengthZ())),
         0.0,
         0.0,
         0.0,
         -42386,
         2.5F
      );
      matrixStack.pop();
   }

   private void drawAreaRadius(MatrixStack matrixStack, Vec3d vec3d, float radius, int color) {
      matrixStack.push();
      matrixStack.translate(
         vec3d.x - mc.gameRenderer.getCamera().getCameraPos().x,
         vec3d.y - mc.gameRenderer.getCamera().getCameraPos().y + 0.01,
         vec3d.z - mc.gameRenderer.getCamera().getCameraPos().z
      );
      this.drawRadiusCircle(
         mc.getBufferBuilders().getEntityVertexConsumers().getBuffer(RenderLayers.lines()), matrixStack, radius, 64, color, 1.5F
      );
      matrixStack.pop();
   }

   private static void emitSphereVertex(VertexConsumer vertexConsumer, Matrix4f matrix4f, float phi, float theta, int color) {
      vertexConsumer.vertex(
            matrix4f,
            MathHelper.sin(phi) * MathHelper.cos(theta) * 0.1F,
            MathHelper.cos(phi) * 0.1F,
            MathHelper.sin(phi) * MathHelper.sin(theta) * 0.1F
         )
         .color(color);
   }

   private boolean buildRibbonGeometry(ProjectilePredictionModule.SubContext subContext, Vec3d vec3d, double pixelScale, int ribbonColor) {
      int startIndex = 0;

      while (subContext.flightPath.size() - startIndex > 2 && subContext.flightPath.get(startIndex).squaredDistanceTo(vec3d) < 2.25) {
         startIndex++;
      }

      if (subContext.flightPath.size() - startIndex < 2) {
         return false;
      }

      ProjectilePredictionModule.SubContext3 ribbon = subContext.ribbon;
      ribbon.ensureRibbonCapacity(subContext.flightPath.size() - startIndex);
      ribbon.pointCount = subContext.flightPath.size() - startIndex;
      ribbon.color = ribbonColor;

      for (int index = 0; index < ribbon.pointCount; index++) {
         ribbon.offsetX[index] = subContext.flightPath.get(startIndex + index).x - vec3d.x;
         ribbon.offsetY[index] = subContext.flightPath.get(startIndex + index).y - vec3d.y;
         ribbon.offsetZ[index] = subContext.flightPath.get(startIndex + index).z - vec3d.z;
      }

      double traveledDistance = 0.0;
      double prevNormalX = 0.0;
      double prevNormalY = 1.0;
      double prevNormalZ = 0.0;

      for (int index2 = 0; index2 < ribbon.pointCount; index2++) {
         double tangentX = ribbon.offsetX[Math.min(index2 + 1, ribbon.pointCount - 1)]
            - ribbon.offsetX[Math.max(index2 - 1, 0)];
         double tangentY = ribbon.offsetY[Math.min(index2 + 1, ribbon.pointCount - 1)]
            - ribbon.offsetY[Math.max(index2 - 1, 0)];
         double tangentZ = ribbon.offsetZ[Math.min(index2 + 1, ribbon.pointCount - 1)]
            - ribbon.offsetZ[Math.max(index2 - 1, 0)];
         double sideX = tangentY * ribbon.offsetZ[index2] - tangentZ * ribbon.offsetY[index2];
         double sideY = tangentZ * ribbon.offsetX[index2] - tangentX * ribbon.offsetZ[index2];
         double sideZ = tangentX * ribbon.offsetY[index2] - tangentY * ribbon.offsetX[index2];
         if (sideX * sideX + sideY * sideY + sideZ * sideZ < 1.0E-8) {
            sideX = prevNormalX;
            sideY = prevNormalY;
            sideZ = prevNormalZ;
         } else {
            double sideLength = 1.0 / Math.sqrt(sideX * sideX + sideY * sideY + sideZ * sideZ);
            sideX *= sideLength;
            sideY *= sideLength;
            sideZ *= sideLength;
         }

         prevNormalX = sideX;
         ribbon.normalX[index2] = sideX;
         prevNormalY = sideY;
         ribbon.normalY[index2] = sideY;
         prevNormalZ = sideZ;
         ribbon.normalZ[index2] = sideZ;
         ribbon.segmentDistance[index2] = (float)(
            Math.sqrt(
                  ribbon.offsetX[index2] * ribbon.offsetX[index2]
                     + ribbon.offsetY[index2] * ribbon.offsetY[index2]
                     + ribbon.offsetZ[index2] * ribbon.offsetZ[index2]
               )
               * pixelScale
         );
         if (index2 > 0) {
            traveledDistance += subContext.flightPath.get(startIndex + index2).distanceTo(subContext.flightPath.get(startIndex + index2 - 1));
         }

         ribbon.progress[index2] = (float)MathHelper.clamp((traveledDistance - 0.1) / 0.55, 0.0, 1.0);
      }

      return true;
   }

   private static void renderRibbon(VertexConsumer vertexConsumer, Matrix4f matrix4f, ProjectilePredictionModule.SubContext3 ribbon) {
      int r = ribbon.color >> 16 & 0xFF;
      int g = ribbon.color >> 8 & 0xFF;
      int b = ribbon.color & 0xFF;

      for (int index = 0; index < ribbon.pointCount - 1; index++) {
         int alphaStart = (int)(ribbon.progress[index] * 255.0F);
         int alphaEnd = (int)(ribbon.progress[index + 1] * 255.0F);
         if (alphaStart > 0 || alphaEnd > 0) {
            float progressStart = (float)index / (ribbon.pointCount - 1);
            float progressEnd = (float)(index + 1) / (ribbon.pointCount - 1);
            int rStart = lightenChannel(r, progressStart);
            int gStart = lightenChannel(g, progressStart);
            int bStart = lightenChannel(b, progressStart);
            int rEnd = lightenChannel(r, progressEnd);
            int gEnd = lightenChannel(g, progressEnd);
            int bEnd = lightenChannel(b, progressEnd);
            double sideXStart = ribbon.normalX[index] * ribbon.segmentDistance[index];
            double sideYStart = ribbon.normalY[index] * ribbon.segmentDistance[index];
            double sideZStart = ribbon.normalZ[index] * ribbon.segmentDistance[index];
            double sideXEnd = ribbon.normalX[index + 1] * ribbon.segmentDistance[index + 1];
            double sideYEnd = ribbon.normalY[index + 1] * ribbon.segmentDistance[index + 1];
            double sideZEnd = ribbon.normalZ[index + 1] * ribbon.segmentDistance[index + 1];
            emitVertex(
               vertexConsumer,
               matrix4f,
               ribbon.offsetX[index] - sideXStart,
               ribbon.offsetY[index] - sideYStart,
               ribbon.offsetZ[index] - sideZStart,
               rStart,
               gStart,
               bStart,
               alphaStart
            );
            emitVertex(
               vertexConsumer,
               matrix4f,
               ribbon.offsetX[index + 1] - sideXEnd,
               ribbon.offsetY[index + 1] - sideYEnd,
               ribbon.offsetZ[index + 1] - sideZEnd,
               rEnd,
               gEnd,
               bEnd,
               alphaEnd
            );
            emitVertex(
               vertexConsumer,
               matrix4f,
               ribbon.offsetX[index + 1] + sideXEnd,
               ribbon.offsetY[index + 1] + sideYEnd,
               ribbon.offsetZ[index + 1] + sideZEnd,
               rEnd,
               gEnd,
               bEnd,
               alphaEnd
            );
            emitVertex(
               vertexConsumer,
               matrix4f,
               ribbon.offsetX[index] + sideXStart,
               ribbon.offsetY[index] + sideYStart,
               ribbon.offsetZ[index] + sideZStart,
               rStart,
               gStart,
               bStart,
               alphaStart
            );
            emitVertex(
               vertexConsumer,
               matrix4f,
               ribbon.offsetX[index] + sideXStart,
               ribbon.offsetY[index] + sideYStart,
               ribbon.offsetZ[index] + sideZStart,
               rStart,
               gStart,
               bStart,
               alphaStart
            );
            emitVertex(
               vertexConsumer,
               matrix4f,
               ribbon.offsetX[index + 1] + sideXEnd,
               ribbon.offsetY[index + 1] + sideYEnd,
               ribbon.offsetZ[index + 1] + sideZEnd,
               rEnd,
               gEnd,
               bEnd,
               alphaEnd
            );
            emitVertex(
               vertexConsumer,
               matrix4f,
               ribbon.offsetX[index + 1] + sideXEnd * 2.5,
               ribbon.offsetY[index + 1] + sideYEnd * 2.5,
               ribbon.offsetZ[index + 1] + sideZEnd * 2.5,
               rEnd,
               gEnd,
               bEnd,
               0
            );
            emitVertex(
               vertexConsumer,
               matrix4f,
               ribbon.offsetX[index] + sideXStart * 2.5,
               ribbon.offsetY[index] + sideYStart * 2.5,
               ribbon.offsetZ[index] + sideZStart * 2.5,
               rStart,
               gStart,
               bStart,
               0
            );
            emitVertex(
               vertexConsumer,
               matrix4f,
               ribbon.offsetX[index] - sideXStart,
               ribbon.offsetY[index] - sideYStart,
               ribbon.offsetZ[index] - sideZStart,
               rStart,
               gStart,
               bStart,
               alphaStart
            );
            emitVertex(
               vertexConsumer,
               matrix4f,
               ribbon.offsetX[index + 1] - sideXEnd,
               ribbon.offsetY[index + 1] - sideYEnd,
               ribbon.offsetZ[index + 1] - sideZEnd,
               rEnd,
               gEnd,
               bEnd,
               alphaEnd
            );
            emitVertex(
               vertexConsumer,
               matrix4f,
               ribbon.offsetX[index + 1] - sideXEnd * 2.5,
               ribbon.offsetY[index + 1] - sideYEnd * 2.5,
               ribbon.offsetZ[index + 1] - sideZEnd * 2.5,
               rEnd,
               gEnd,
               bEnd,
               0
            );
            emitVertex(
               vertexConsumer,
               matrix4f,
               ribbon.offsetX[index] - sideXStart * 2.5,
               ribbon.offsetY[index] - sideYStart * 2.5,
               ribbon.offsetZ[index] - sideZStart * 2.5,
               rStart,
               gStart,
               bStart,
               0
            );
         }
      }
   }

   private static final class SubContext {
      ArrayList<Vec3d> flightPath = new ArrayList<>();
      Vec3d anchorPos = Vec3d.ZERO;
      int lastPointCount = -1;
      String flightTimeText = "0.0с";
      boolean hitTarget;
      boolean isActive;
      ItemStack icon = ItemStack.EMPTY;
      ProjectilePredictionModule.SubContext3 ribbon = new ProjectilePredictionModule.SubContext3();
      double[] oldCumulativeLengths = new double[0];
      double[] newCumulativeLengths = new double[0];
      double[] resampledX = new double[0];
      double[] resampledY = new double[0];
      double[] resampledZ = new double[0];

      void offsetPath(Vec3d vec3d) {
         this.flightPath.replaceAll(vec3d2 -> vec3d2.add(vec3d));
         this.anchorPos = this.anchorPos.add(vec3d);
      }

      void updatePath(ArrayList<Vec3d> newPath, Vec3d vec3d, boolean hasHitTarget, ItemStack itemStack, float deltaTime) {
         float smoothing = 1.0F - (float)Math.exp(-deltaTime * 100.0F);
         if (!this.flightPath.isEmpty() && !(this.anchorPos.squaredDistanceTo(vec3d) > 4096.0)) {
            this.anchorPos = this.anchorPos.lerp(vec3d, smoothing);
            this.resamplePath(newPath, smoothing);
         } else {
            this.flightPath = new ArrayList<>(newPath);
            this.anchorPos = vec3d;
         }

         if (this.lastPointCount != newPath.size() - 1) {
            this.lastPointCount = newPath.size() - 1;
            this.flightTimeText = Math.max(0, Math.round(this.lastPointCount / 2.0F)) / 10 + "." + Math.max(0, Math.round(this.lastPointCount / 2.0F)) % 10 + "с";
         }

         this.hitTarget = hasHitTarget;
         this.icon = itemStack;
      }

      private void resamplePath(ArrayList<Vec3d> newPath, float smoothing) {
         if (newPath.isEmpty()) {
            this.flightPath.clear();
         } else if (Math.max(this.flightPath.size(), newPath.size()) <= 1) {
            this.flightPath = new ArrayList<>(newPath);
         } else if (this.flightPath.size() == newPath.size()) {
            for (int index = 0; index < this.flightPath.size(); index++) {
               this.flightPath
                  .set(
                     index,
                     this.flightPath
                        .get(index)
                        .lerp(newPath.get(index), smoothing * (0.15F + 0.85F * index / (this.flightPath.size() - 1)))
                  );
            }
         } else {
            int sampleCount = Math.max(this.flightPath.size(), newPath.size());
            if (this.resampledX.length < sampleCount) {
               this.oldCumulativeLengths = new double[Math.max(sampleCount, this.resampledX.length == 0 ? 64 : this.resampledX.length * 2)];
               this.newCumulativeLengths = new double[this.oldCumulativeLengths.length];
               this.resampledX = new double[this.oldCumulativeLengths.length];
               this.resampledY = new double[this.oldCumulativeLengths.length];
               this.resampledZ = new double[this.oldCumulativeLengths.length];
            }

            this.oldCumulativeLengths[0] = 0.0;

            for (int index2 = 1; index2 < this.flightPath.size(); index2++) {
               this.oldCumulativeLengths[index2] = this.oldCumulativeLengths[index2 - 1]
                  + this.flightPath.get(index2 - 1).distanceTo(this.flightPath.get(index2));
            }

            this.newCumulativeLengths[0] = 0.0;

            for (int index3 = 1; index3 < newPath.size(); index3++) {
               this.newCumulativeLengths[index3] = this.newCumulativeLengths[index3 - 1] + newPath.get(index3 - 1).distanceTo(newPath.get(index3));
            }

            int oldIndex = 0;

            for (int index4 = 0; index4 < sampleCount; index4++) {
               double oldTargetDistance = this.oldCumulativeLengths[this.flightPath.size() - 1] * index4 / (sampleCount - 1);

               while (oldIndex + 1 < this.flightPath.size() && this.oldCumulativeLengths[oldIndex + 1] < oldTargetDistance) {
                  oldIndex++;
               }

               if (oldIndex + 1 >= this.flightPath.size()) {
                  this.resampledX[index4] = this.flightPath.get(oldIndex).x;
                  this.resampledY[index4] = this.flightPath.get(oldIndex).y;
                  this.resampledZ[index4] = this.flightPath.get(oldIndex).z;
               } else {
                  double oldSegmentLength = this.oldCumulativeLengths[oldIndex + 1] - this.oldCumulativeLengths[oldIndex];
                  double oldLerpFactor = oldSegmentLength <= 1.0E-8 ? 0.0 : (oldTargetDistance - this.oldCumulativeLengths[oldIndex]) / oldSegmentLength;
                  this.resampledX[index4] = MathHelper.lerp(oldLerpFactor, this.flightPath.get(oldIndex).x, this.flightPath.get(oldIndex + 1).x);
                  this.resampledY[index4] = MathHelper.lerp(oldLerpFactor, this.flightPath.get(oldIndex).y, this.flightPath.get(oldIndex + 1).y);
                  this.resampledZ[index4] = MathHelper.lerp(oldLerpFactor, this.flightPath.get(oldIndex).z, this.flightPath.get(oldIndex + 1).z);
               }
            }

            int newIndex = 0;

            for (int index5 = 0; index5 < sampleCount; index5++) {
               double newTargetDistance = this.newCumulativeLengths[newPath.size() - 1] * index5 / (sampleCount - 1);

               while (newIndex + 1 < newPath.size() && this.newCumulativeLengths[newIndex + 1] < newTargetDistance) {
                  newIndex++;
               }

               double newX = newPath.get(newIndex).x;
               double newY = newPath.get(newIndex).y;
               double newZ = newPath.get(newIndex).z;
               if (newIndex + 1 < newPath.size()) {
                  double newSegmentLength = this.newCumulativeLengths[newIndex + 1] - this.newCumulativeLengths[newIndex];
                  double newLerpFactor = newSegmentLength <= 1.0E-8 ? 0.0 : (newTargetDistance - this.newCumulativeLengths[newIndex]) / newSegmentLength;
                  newX = MathHelper.lerp(newLerpFactor, newPath.get(newIndex).x, newPath.get(newIndex + 1).x);
                  newY = MathHelper.lerp(newLerpFactor, newPath.get(newIndex).y, newPath.get(newIndex + 1).y);
                  newZ = MathHelper.lerp(newLerpFactor, newPath.get(newIndex).z, newPath.get(newIndex + 1).z);
               }

               float blendFactor = smoothing * (0.15F + 0.85F * index5 / (sampleCount - 1));
               Vec3d vec3d = new Vec3d(
                  MathHelper.lerp(blendFactor, this.resampledX[index5], newX),
                  MathHelper.lerp(blendFactor, this.resampledY[index5], newY),
                  MathHelper.lerp(blendFactor, this.resampledZ[index5], newZ)
               );
               if (index5 < this.flightPath.size()) {
                  this.flightPath.set(index5, vec3d);
               } else {
                  this.flightPath.add(vec3d);
               }
            }

            while (this.flightPath.size() > sampleCount) {
               this.flightPath.removeLast();
            }
         }
      }
   }

   private record DataRecord2(int entityId, ProjectilePredictionModule.DataRecord6 landing, ArrayList<Vec3d> path, ItemStack icon) {
   }

   private static final class SubContext3 {
      double[] offsetX = new double[0];
      double[] offsetY = new double[0];
      double[] offsetZ = new double[0];
      double[] normalX = new double[0];
      double[] normalY = new double[0];
      double[] normalZ = new double[0];
      float[] progress = new float[0];
      float[] segmentDistance = new float[0];
      int pointCount;
      int color;

      void ensureRibbonCapacity(int count) {
         if (this.offsetX.length < count) {
            int capacity = Math.max(count, this.offsetX.length == 0 ? 32 : this.offsetX.length * 2);
            this.offsetX = new double[capacity];
            this.offsetY = new double[capacity];
            this.offsetZ = new double[capacity];
            this.normalX = new double[capacity];
            this.normalY = new double[capacity];
            this.normalZ = new double[capacity];
            this.progress = new float[capacity];
            this.segmentDistance = new float[capacity];
         }
      }
   }

   private record DataRecord4(ProjectilePredictionModule.DataRecord6 landing, float areaRadiusBlocks, ArrayList<Vec3d> flightPath, ItemStack icon) {
   }

   private record DataRecord5(LivingEntity target, Box box) {
   }

   private record DataRecord6(Vec3d pos, LivingEntity hitTarget) {
   }
}
