package greed.client.module.misc;

import greed.client.module.combat.AttackAuraModule;
import greed.client.setting.BooleanSetting;
import greed.client.util.ClientChat;
import greed.client.event.EventListener;
import greed.client.MinecraftAccess;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.event.PacketEvent;
import greed.client.event.PlayerTickEvent;
import greed.client.module.combat.AttackController;
import greed.client.util.EntityUtils;
import greed.client.util.MouseSensitivity;
import greed.client.RotationManager;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.Handler;
import net.minecraft.registry.Registries;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(
   name = "AdminRecord",
   description = "Пишет твои движения мышью по yaw и pitch, чтобы разобрать, как двигается рука.",
   category = ModuleCategory.MISC
)
public final class AdminRecordModule extends Module {
   private static AdminRecordModule adminRecordModule;
   public BooleanSetting onlyManual;
   public BooleanSetting onlyNearTarget;
   private float floatVal;
   private float floatVal2;
   private float floatVal3;
   private float floatVal4;
   private int intVal;
   private int intVal2;
   private int intVal3;
   private int intVal4;
   private boolean flag;
   private Path path2;
   private BufferedWriter bufferedWriter;
   private final EventListener<PlayerTickEvent> playerTickListener = playerTickEvent -> {
      if (mc.player != null && this.flag) {
         if (mc.player.age - this.intVal == 2) {
            this.processData2(mc.player.age - 1, 0.0F, 0.0F);
         }
      }
   };
   private final EventListener<PacketEvent> packetListener = packetEvent -> {
      if (mc.player != null && packetEvent.isOutgoing()) {
         if (packetEvent.getPacket() instanceof PlayerMoveC2SPacket playerMoveC2SPacket && playerMoveC2SPacket.changesLook()) {
            this.processData3(playerMoveC2SPacket.getYaw(0.0F), playerMoveC2SPacket.getPitch(0.0F));
         } else if (packetEvent.getPacket() instanceof PlayerInteractEntityC2SPacket playerInteractEntityC2SPacket) {
            playerInteractEntityC2SPacket.handle(new Handler() {
               @Override
               public void interactAt(Hand hand, Vec3d vec3d) {
               }

               @Override
               public void interact(Hand hand) {
               }

               @Override
               public void attack() {
                  AdminRecordModule.this.intVal3 = MinecraftAccess.mc.player.age;
               }
            });
         }
      }
   };

   private void updateState() {
      try {
         if (this.bufferedWriter != null) {
            this.bufferedWriter.close();
         }
      } catch (IOException exception) {
      }

      this.bufferedWriter = null;
   }

   private void processData(String local1419_1) {
      try {
         if (this.bufferedWriter == null) {
            Path path = FabricLoader.getInstance().getConfigDir().resolve("greed").resolve("adminrecord");
            Files.createDirectories(path);
            this.path2 = path.resolve("rec-" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()) + ".csv");
            this.bufferedWriter = Files.newBufferedWriter(this.path2, StandardCharsets.UTF_8);
            this.bufferedWriter
               .write(
                  "time;tick;yaw;pitch;dyaw;dpitch;stepsYaw;stepsPitch;target;distance;flat;errYaw;errPitch;height;velYaw;velPitch;targetGround;ground;sprint;attack;strike;cooldown;msToReady;sinceAttack;swing;hurt;item;attackSpeed\n"
               );
            ClientChat.processData8("&7AdminRecord пишет в &f" + this.path2);
         }

         this.bufferedWriter.write(local1419_1);
         if (++this.intVal4 % 40 == 0) {
            this.bufferedWriter.flush();
         }
      } catch (IOException exception) {
         this.updateState();
         ClientChat.processData8("&cAdminRecord: запись не идёт — " + exception.getMessage());
      }
   }

   public static AdminRecordModule getInstance() {
      if (adminRecordModule == null) {
         adminRecordModule = new AdminRecordModule();
      }

      return adminRecordModule;
   }

   private LivingEntity getLivingEntity() {
      if (AttackAuraModule.getInstance().isEnabled() && AttackAuraModule.getInstance().getTarget() != null) {
         return AttackAuraModule.getInstance().getTarget();
      }

      LivingEntity livingEntity = null;

      for (PlayerEntity playerEntity : mc.world.getPlayers()) {
         if (playerEntity != mc.player
            && playerEntity.isAlive()
            && mc.player.distanceTo(playerEntity) < 6.0F
            && (livingEntity == null || mc.player.distanceTo(playerEntity) < mc.player.distanceTo(livingEntity))) {
            livingEntity = playerEntity;
         }
      }

      return livingEntity;
   }

   @Override
   protected void onDisable() {
      ClientChat.processData8(
         this.bufferedWriter == null
            ? "&7AdminRecord: ничего не записано"
            : String.format("&7AdminRecord: записано &f%d &7тиков в &f%s", this.intVal4, this.path2)
      );
      this.updateState();
   }

   private void processData2(int local1425_1, float local1425_2, float local1425_3) {
      if (mc.player != null && mc.world != null) {
         if (!this.onlyManual.isChecked() || !RotationManager.getRotationManager().isRotating()) {
            LivingEntity entity = this.getLivingEntity();
            if (entity != null || !this.onlyNearTarget.isChecked()) {
               float local1425_5 = MouseSensitivity.getSensitivityDelta();
               float local1425_6 = 0.0F;
               float local1425_7 = 0.0F;
               float local1425_8 = 0.0F;
               float local1425_9 = 0.0F;
               float local1425_10 = 0.0F;
               double local1425_11 = -1.0;
               double local1425_13 = -1.0;
               if (entity != null) {
                  Vec3d eyePos = mc.player.getEyePos();
                  Vec3d center = entity.getBoundingBox().getCenter();
                  local1425_13 = Math.hypot(center.x - eyePos.x, center.z - eyePos.z);
                  local1425_11 = EntityUtils.getDistanceToBox(entity);
                  local1425_10 = (float)(center.y - eyePos.y);
                  float local1425_17 = (float)Math.toDegrees(Math.atan2(center.z - eyePos.z, center.x - eyePos.x)) - 90.0F;
                  float local1425_18 = (float)(-Math.toDegrees(Math.atan2(center.y - eyePos.y, Math.max(0.6, local1425_13))));
                  local1425_6 = MathHelper.wrapDegrees(local1425_17 - this.floatVal);
                  local1425_7 = local1425_18 - this.floatVal2;
                  if (local1425_1 - this.intVal2 == 1) {
                     local1425_8 = MathHelper.wrapDegrees(local1425_17 - this.floatVal3);
                     local1425_9 = local1425_18 - this.floatVal4;
                  }

                  this.floatVal3 = local1425_17;
                  this.floatVal4 = local1425_18;
                  this.intVal2 = local1425_1;
               }

               this.processData(
                  String.format(
                     Locale.ROOT,
                     "%d;%d;%.3f;%.3f;%.3f;%.3f;%d;%d;%s;%.2f;%.2f;%.2f;%.2f;%.2f;%.2f;%.2f;%d;%d;%d;%d;%d;%.3f;%.0f;%d;%d;%d;%s;%.2f%n",
                     System.currentTimeMillis(),
                     local1425_1,
                     this.floatVal,
                     this.floatVal2,
                     local1425_2,
                     local1425_3,
                     Math.round(local1425_2 / local1425_5),
                     Math.round(local1425_3 / local1425_5),
                     entity == null ? "-" : entity.getName().getString(),
                     local1425_11,
                     local1425_13,
                     local1425_6,
                     local1425_7,
                     local1425_10,
                     local1425_8,
                     local1425_9,
                     entity != null && entity.isOnGround() ? 1 : 0,
                     mc.player.isOnGround() ? 1 : 0,
                     mc.player.isSprinting() ? 1 : 0,
                     this.intVal3 == local1425_1 ? 1 : 0,
                     AttackController.hasAttackCooldown(-150L) ? 1 : 0,
                     mc.player.getAttackCooldownProgress(0.0F),
                     Math.max(
                        0.0F,
                        (0.9F - mc.player.getAttackCooldownProgress(0.0F))
                           * 1000.0F
                           / (float)mc.player.getAttributeValue(EntityAttributes.ATTACK_SPEED)
                     ),
                     Math.min(100, local1425_1 - this.intVal3),
                     mc.player.handSwingTicks,
                     entity == null ? -1 : entity.hurtTime,
                     Registries.ITEM.getId(mc.player.getMainHandStack().getItem()).getPath(),
                     mc.player.getAttributeValue(EntityAttributes.ATTACK_SPEED)
                  )
               );
            }
         }
      }
   }

   private AdminRecordModule() {
      this.onlyManual = this.addSetting(new BooleanSetting("Только вручную", "Не писать тики, где прицел ведёт аура или другой модуль.", true));
      this.onlyNearTarget = this.addSetting(new BooleanSetting("Только рядом с целью", "Писать только когда рядом есть игрок в 6 блоках.", true));
   }

   private void processData3(float local1427_1, float local1427_2) {
      if (!this.flag) {
         this.floatVal = local1427_1;
         this.floatVal2 = local1427_2;
         this.flag = true;
         this.intVal = mc.player.age;
      } else {
         float local1427_3 = MathHelper.wrapDegrees(local1427_1 - this.floatVal);
         float local1427_4 = local1427_2 - this.floatVal2;
         this.floatVal = local1427_1;
         this.floatVal2 = local1427_2;
         this.intVal = mc.player.age;
         this.processData2(mc.player.age, local1427_3, local1427_4);
      }
   }

   @Override
   protected void onEnable() {
      this.flag = false;
      this.intVal4 = 0;
      this.intVal = this.intVal2 = this.intVal3 = -10;
   }
}
