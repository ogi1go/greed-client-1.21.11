package greed.client.module.misc;

import greed.client.module.combat.AttackAuraModule;
import greed.client.setting.BooleanSetting;
import greed.client.util.ClientChat;
import greed.client.event.EventListener;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.event.PacketEvent;
import greed.client.event.PlayerTickEvent;
import greed.client.util.EntityUtils;
import greed.client.util.MouseSensitivity;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.Handler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(name = "AdminShard", description = "Читает детект античита над хотбаром и пишет, какие ротации его двигают.", category = ModuleCategory.MISC)
public final class AdminShardModule extends Module {
   private static AdminShardModule adminShardModule;
   private static final Pattern pattern = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*%");
   private static final Pattern pattern2 = Pattern.compile("([+\\-−]?)\\s*(\\d+(?:[.,]\\d+)?)");
   private static final String[] strValArray = new String[]{
      "yaw ср",
      "yaw макс",
      "pitch ср",
      "pitch макс",
      "рывок yaw",
      "рывок pitch",
      "pitch стоит %",
      "GCD %",
      "ошибка °",
      "удары",
      "в цель %",
      "снап удара",
      "дистанция",
      "ротаций/тик"
   };
   public BooleanSetting writeToChat;
   public BooleanSetting logTicks;
   private final double[] doubleValArray = new double[strValArray.length];
   private final double[] doubleValArray2 = new double[strValArray.length];
   private int intVal;
   private int intVal2;
   private int intVal3;
   private int intVal4;
   private int intVal5;
   private int intVal6;
   private int intVal7;
   private int intVal8;
   private int intVal9;
   private double doubleVal;
   private double doubleVal2;
   private double doubleVal3;
   private double doubleVal4;
   private double doubleVal5;
   private double doubleVal6;
   private double doubleVal7;
   private double doubleVal8;
   private double doubleVal9;
   private float floatVal;
   private float floatVal2;
   private float floatVal3;
   private float floatVal4;
   private float floatVal5 = -1.0F;
   private float floatVal6 = -1.0F;
   private boolean flag;
   private String strVal = "";
   private BufferedWriter bufferedWriter;
   private BufferedWriter bufferedWriter2;
   private final EventListener<PlayerTickEvent> playerTickListener = playerTickEvent -> this.intVal++;
   private final EventListener<PacketEvent> packetListener = packetEvent -> {
      if (mc.player != null) {
         if (packetEvent.isIncoming()) {
            Text text = packetEvent.getPacket() instanceof OverlayMessageS2CPacket overlayMessageS2CPacket
               ? overlayMessageS2CPacket.text()
               : (packetEvent.getPacket() instanceof GameMessageS2CPacket gameMessageS2CPacket && gameMessageS2CPacket.overlay() ? gameMessageS2CPacket.content() : null);
            if (text != null) {
               String string = text.getString();
               mc.execute(() -> this.processData2(string));
            }
         } else {
            if (packetEvent.getPacket() instanceof PlayerMoveC2SPacket playerMoveC2SPacket && playerMoveC2SPacket.changesLook()) {
               this.processData3(playerMoveC2SPacket.getYaw(0.0F), playerMoveC2SPacket.getPitch(0.0F));
            } else if (packetEvent.getPacket() instanceof PlayerInteractEntityC2SPacket playerInteractEntityC2SPacket) {
               playerInteractEntityC2SPacket.handle(new Handler() {
                  @Override
                  public void interact(Hand hand) {
                  }

                  @Override
                  public void interactAt(Hand hand, Vec3d vec3d) {
                  }

                  @Override
                  public void attack() {
                     AdminShardModule.this.updateState3();
                  }
               });
            }
         }
      }
   };

   private AdminShardModule() {
      this.writeToChat = this.addSetting(new BooleanSetting("Писать в чат", "Каждое изменение буфера с разбором ротаций за это время.", true));
      this.logTicks = this.addSetting(new BooleanSetting("Лог тиков", "Каждый пакет ротации и удара в ticks.csv.", true));
   }

   private static boolean matchesCriteria(float local4636_0, float local4636_1) {
      float local4636_2 = Math.abs(local4636_0) / local4636_1;
      return Math.abs(local4636_2 - Math.round(local4636_2)) < 0.02F + local4636_2 * 1.0E-4F;
   }

   private void processData(
      String local4637_1,
      float local4637_2,
      float local4637_3,
      float local4637_4,
      float local4637_5,
      boolean local4637_6,
      LivingEntity entity,
      float local4637_8,
      boolean local4637_9
   ) {
      if (this.logTicks.isChecked() && mc.player != null) {
         this.processData4(
            false,
            String.format(
               Locale.ROOT,
               "%d;%d;%s;%.3f;%.3f;%.3f;%.3f;%.3f;%.3f;%d;%s;%.2f;%.2f;%d;%d;%d;%s;%.0f;%.2f%n",
               System.currentTimeMillis(),
               mc.player.age,
               local4637_1,
               this.floatVal,
               this.floatVal2,
               local4637_2,
               local4637_3,
               local4637_4,
               local4637_5,
               local4637_6 ? 1 : 0,
               entity == null ? "-" : entity.getName().getString(),
               entity == null ? -1.0 : EntityUtils.getDistanceToBox(entity),
               local4637_8,
               local4637_9 ? 1 : 0,
               mc.player.isSprinting() ? 1 : 0,
               mc.player.isOnGround() ? 1 : 0,
               AttackAuraModule.getInstance().isEnabled() ? AttackAuraModule.getInstance().rotationType.getSelectedOption() : "-",
               this.floatVal5,
               this.floatVal6
            )
         );
      }
   }

   @Override
   protected void onDisable() {
      this.updateState();
      this.updateState4();
   }

   private static float computeFloat(String local4640_0) {
      return Float.parseFloat(local4640_0.replace(',', '.'));
   }

   private void updateState() {
      if (this.intVal8 + this.intVal9 != 0) {
         StringBuilder stringBuilder = new StringBuilder(String.format(Locale.ROOT, "# рост %d, падение %d%n", this.intVal8, this.intVal9));
         ClientChat.processData8(
            String.format(Locale.ROOT, "&fAdminShard: &cрост %d &7/ &aпадение %d &7(среднее за окно)", this.intVal8, this.intVal9)
         );

         for (int index = 0; index < strValArray.length; index++) {
            double local4641_3 = this.intVal8 == 0 ? 0.0 : this.doubleValArray[index] / this.intVal8;
            double local4641_5 = this.intVal9 == 0 ? 0.0 : this.doubleValArray2[index] / this.intVal9;
            double local4641_7 = Math.abs(local4641_3 - local4641_5) / Math.max(0.001, Math.max(Math.abs(local4641_3), Math.abs(local4641_5)));
            ClientChat.processData8(
               String.format(Locale.ROOT, "&7%s: &c%.2f &7/ &a%.2f%s", strValArray[index], local4641_3, local4641_5, local4641_7 > 0.25 ? " &6◀" : "")
            );
            stringBuilder.append(String.format(Locale.ROOT, "# %s;%.3f;%.3f;%.2f%n", strValArray[index], local4641_3, local4641_5, local4641_7));
         }

         this.processData4(true, stringBuilder.toString());
      }
   }

   @Override
   protected void onEnable() {
      this.updateState2();
      this.flag = false;
      this.strVal = "";
      this.floatVal5 = this.floatVal6 = -1.0F;
      Arrays.fill(this.doubleValArray, 0.0);
      Arrays.fill(this.doubleValArray2, 0.0);
      this.intVal8 = this.intVal9 = 0;
   }

   private void updateState2() {
      this.intVal = this.intVal2 = this.intVal3 = this.intVal4 = this.intVal5 = this.intVal6 = this.intVal7 = 0;
      this.doubleVal = this.doubleVal2 = this.doubleVal3 = this.doubleVal4 = this.doubleVal5 = this.doubleVal6 = this.doubleVal7 = this.doubleVal8 = this.doubleVal9 = 0.0;
   }

   private void updateState3() {
      LivingEntity entity = this.getLivingEntity();
      boolean local4644_2 = entity != null && this.flag && EntityUtils.isLookingAtEntity(entity, 6.0, this.floatVal, this.floatVal2);
      this.intVal5++;
      if (local4644_2) {
         this.intVal6++;
      }

      this.doubleVal9 = this.doubleVal9 + Math.hypot(this.floatVal3, this.floatVal4);
      this.processData(
         "attack",
         this.floatVal3,
         this.floatVal4,
         0.0F,
         0.0F,
         true,
         entity,
         entity == null ? -1.0F : computeFloat2(entity, this.floatVal, this.floatVal2),
         local4644_2
      );
   }

   private double[] computeDouble() {
      int local4645_1 = Math.max(1, this.intVal2);
      int local4645_2 = Math.max(1, this.intVal7);
      int local4645_3 = Math.max(1, this.intVal5);
      return new double[]{
         this.doubleVal / local4645_1,
         this.doubleVal2,
         this.doubleVal3 / local4645_1,
         this.doubleVal4,
         this.doubleVal5 / local4645_1,
         this.doubleVal6 / local4645_1,
         this.intVal3 * 100.0 / local4645_1,
         this.intVal4 * 100.0 / local4645_1,
         this.doubleVal7 / local4645_2,
         this.intVal5,
         this.intVal6 * 100.0 / local4645_3,
         this.doubleVal9 / local4645_3,
         this.doubleVal8 / local4645_2,
         (double)this.intVal2 / Math.max(1, this.intVal)
      };
   }

   private void processData2(String local4646_1) {
      if (mc.player != null && this.isEnabled() && !local4646_1.equals(this.strVal)) {
         this.strVal = local4646_1;
         this.processData("text " + local4646_1.replace(';', ',').replace('\n', ' '), 0.0F, 0.0F, 0.0F, 0.0F, true, null, -1.0F, false);
         Matcher matcher = pattern.matcher(local4646_1);
         if (matcher.find()) {
            float local4646_3 = computeFloat(matcher.group(1));
            float local4646_4 = Float.NaN;
            float local4646_5 = Float.NaN;
            Matcher matcher2 = pattern2.matcher(local4646_1.substring(matcher.end()));

            while (matcher2.find()) {
               float local4646_7 = computeFloat(matcher2.group(2));
               if (!matcher2.group(1).isEmpty() && Float.isNaN(local4646_4)) {
                  local4646_4 = matcher2.group(1).equals("+") ? local4646_7 : -local4646_7;
               } else {
                  local4646_5 = local4646_7;
               }
            }

            if (!Float.isNaN(local4646_5)) {
               float local4646_7 = this.floatVal6 < 0.0F ? (Float.isNaN(local4646_4) ? 0.0F : local4646_4) : local4646_5 - this.floatVal6;
               if (Float.isNaN(local4646_4)) {
                  local4646_4 = local4646_7;
               }

               this.floatVal5 = local4646_3;
               if (!(this.floatVal6 >= 0.0F) || local4646_7 != 0.0F) {
                  this.floatVal6 = local4646_5;
                  double[] doubleArray = this.computeDouble();
                  if (local4646_7 > 0.0F) {
                     this.intVal8++;
                  }

                  if (local4646_7 < 0.0F) {
                     this.intVal9++;
                  }

                  for (int index = 0; index < doubleArray.length; index++) {
                     if (local4646_7 > 0.0F) {
                        this.doubleValArray[index] = this.doubleValArray[index] + doubleArray[index];
                     }

                     if (local4646_7 < 0.0F) {
                        this.doubleValArray2[index] = this.doubleValArray2[index] + doubleArray[index];
                     }
                  }

                  if (this.writeToChat.isChecked()) {
                     ClientChat.processData8(
                        String.format(
                           Locale.ROOT,
                           "%s%+.2f &7→ &e%.2f &7(%.0f%%) &8| &7yaw &f%.1f/%.1f &7pitch &f%.1f/%.1f &7рывок &f%.1f/%.1f &7удары &f%d &7в цель &f%.0f%% &7ошибка &f%.1f° &7GCD &f%.0f%%",
                           local4646_7 > 0.0F ? "&c" : "&a",
                           local4646_7,
                           local4646_5,
                           local4646_3,
                           doubleArray[0],
                           doubleArray[1],
                           doubleArray[2],
                           doubleArray[3],
                           doubleArray[4],
                           doubleArray[5],
                           this.intVal5,
                           doubleArray[10],
                           doubleArray[8],
                           doubleArray[7]
                        )
                     );
                  }

                  StringBuilder stringBuilder = new StringBuilder(
                     String.format(
                        Locale.ROOT,
                        "%d;%.2f;%.2f;%.2f;%.2f;%d",
                        System.currentTimeMillis(),
                        local4646_3,
                        local4646_4,
                        local4646_7,
                        local4646_5,
                        this.intVal
                     )
                  );

                  for (double local4646_13 : doubleArray) {
                     stringBuilder.append(String.format(Locale.ROOT, ";%.3f", local4646_13));
                  }

                  this.processData4(
                     true,
                     stringBuilder.append(';')
                        .append(AttackAuraModule.getInstance().isEnabled() ? AttackAuraModule.getInstance().rotationType.getSelectedOption() : "-")
                        .append(';')
                        .append(local4646_1.replace(';', ',').replace('\n', ' '))
                        .append('\n')
                        .toString()
                  );
                  this.updateState2();
               }
            }
         }
      }
   }

   private void processData3(float local4648_1, float local4648_2) {
      if (!this.flag) {
         this.floatVal = local4648_1;
         this.floatVal2 = local4648_2;
         this.flag = true;
      } else {
         float local4648_3 = MathHelper.wrapDegrees(local4648_1 - this.floatVal);
         float local4648_4 = local4648_2 - this.floatVal2;
         float local4648_5 = Math.abs(local4648_3 - this.floatVal3);
         float local4648_6 = Math.abs(local4648_4 - this.floatVal4);
         float local4648_7 = MouseSensitivity.getSensitivityDelta();
         boolean local4648_8 = matchesCriteria(local4648_3, local4648_7) && matchesCriteria(local4648_4, local4648_7);
         this.floatVal = local4648_1;
         this.floatVal2 = local4648_2;
         this.floatVal3 = local4648_3;
         this.floatVal4 = local4648_4;
         this.intVal2++;
         this.doubleVal = this.doubleVal + Math.abs(local4648_3);
         this.doubleVal2 = Math.max(this.doubleVal2, Math.abs(local4648_3));
         this.doubleVal3 = this.doubleVal3 + Math.abs(local4648_4);
         this.doubleVal4 = Math.max(this.doubleVal4, Math.abs(local4648_4));
         this.doubleVal5 += local4648_5;
         this.doubleVal6 += local4648_6;
         if (local4648_4 == 0.0F && local4648_3 != 0.0F) {
            this.intVal3++;
         }

         if (local4648_8) {
            this.intVal4++;
         }

         LivingEntity entity = this.getLivingEntity();
         float local4648_10 = entity == null ? -1.0F : computeFloat2(entity, local4648_1, local4648_2);
         if (entity != null) {
            this.doubleVal7 += local4648_10;
            this.doubleVal8 = this.doubleVal8 + EntityUtils.getDistanceToBox(entity);
            this.intVal7++;
         }

         this.processData(
            "rot",
            local4648_3,
            local4648_4,
            local4648_5,
            local4648_6,
            local4648_8,
            entity,
            local4648_10,
            entity != null && EntityUtils.isLookingAtEntity(entity, 6.0, local4648_1, local4648_2)
         );
      }
   }

   private static float computeFloat2(LivingEntity livingEntity, float local4649_1, float local4649_2) {
      float[] floatArray = EntityUtils.calculateRotationsToVec(livingEntity.getBoundingBox().getCenter(), mc.player.getEyePos());
      return (float)Math.hypot(MathHelper.wrapDegrees(floatArray[0] - local4649_1), floatArray[1] - local4649_2);
   }

   private void updateState4() {
      try {
         if (this.bufferedWriter2 != null) {
            this.bufferedWriter2.close();
         }

         if (this.bufferedWriter != null) {
            this.bufferedWriter.close();
         }
      } catch (IOException exception) {
      }

      this.bufferedWriter2 = this.bufferedWriter = null;
   }

   public static AdminShardModule getInstance() {
      if (adminShardModule == null) {
         adminShardModule = new AdminShardModule();
      }

      return adminShardModule;
   }

   private LivingEntity getLivingEntity() {
      if (AttackAuraModule.getInstance().isEnabled() && AttackAuraModule.getInstance().getTarget() != null) {
         return AttackAuraModule.getInstance().getTarget();
      }

      LivingEntity livingEntity = null;

      for (PlayerEntity playerEntity : mc.world.getPlayers()) {
         if (playerEntity != mc.player
            && playerEntity.isAlive()
            && mc.player.distanceTo(playerEntity) < 8.0F
            && (livingEntity == null || mc.player.distanceTo(playerEntity) < mc.player.distanceTo(livingEntity))) {
            livingEntity = playerEntity;
         }
      }

      return livingEntity;
   }

   private void processData4(boolean local4653_1, String local4653_2) {
      try {
         if (this.bufferedWriter2 == null) {
            Path path = FabricLoader.getInstance().getConfigDir().resolve("greed").resolve("adminshard");
            Files.createDirectories(path);
            String local4653_4 = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
            this.bufferedWriter2 = Files.newBufferedWriter(path.resolve("events-" + local4653_4 + ".csv"), StandardCharsets.UTF_8);
            this.bufferedWriter2.write("time;percent;delta;change;buffer;ticks;" + String.join(";", strValArray) + ";rotation;text\n");
            this.bufferedWriter = Files.newBufferedWriter(path.resolve("ticks-" + local4653_4 + ".csv"), StandardCharsets.UTF_8);
            this.bufferedWriter
               .write("time;tick;type;yaw;pitch;dyaw;dpitch;accelYaw;accelPitch;gcd;target;distance;aimError;onTarget;sprint;ground;rotation;percent;buffer\n");
            ClientChat.processData8("&7AdminShard пишет в &f" + path);
         }

         if (local4653_1) {
            this.bufferedWriter2.write(local4653_2);
            this.bufferedWriter2.flush();
            this.bufferedWriter.flush();
         } else {
            this.bufferedWriter.write(local4653_2);
         }
      } catch (IOException exception) {
         this.updateState4();
         ClientChat.processData8("&cAdminShard: лог не пишется — " + exception.getMessage());
      }
   }
}
