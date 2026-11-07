package greed.client.module.movement;

import greed.client.module.combat.AttackAuraModule;
import greed.client.setting.BooleanSetting;
import greed.client.event.EventListener;
import greed.client.util.ItemCooldownAccess;
import greed.client.setting.KeybindSetting;
import greed.client.setting.LegacyNumberSetting;
import greed.mixin.LivingEntityMixin;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.event.MovementInputEvent;
import greed.client.event.PacketEvent;
import greed.client.event.PlayerTickEvent;
import greed.client.util.InventoryUtils;
import greed.client.ui.hud.NotificationHud;
import greed.client.util.InventorySlotManager;
import greed.client.RotationAngles;
import greed.client.RotationManager;
import greed.client.RotationSpeed;
import greed.client.module.player.ServerHelperModule;

import java.util.Locale;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

@ModuleInfo(name = "MaceTest", description = "Порт MaceHelper из devgreed: заряд ветра по бинду и булава в падение.", category = ModuleCategory.MOVEMENT)
public final class MaceTestModule extends Module {
   private static MaceTestModule maceTestModule;
   private KeybindSetting keybindSetting;
   public BooleanSetting assistAuraHit;
   public LegacyNumberSetting legacyNumberSetting;
   public BooleanSetting takeOnFall;
   public BooleanSetting autoTakeMace;
   private boolean flag;
   private boolean flag2;
   private boolean flag3;
   private boolean flag4;
   private boolean flag5;
   private boolean flag6;
   private boolean flag7;
   private boolean flag8;
   private long timeMillis;
   private int intVal;
   private int intVal2;
   private long timeMillis2;
   private float floatVal;
   private float floatVal2;
   private int intVal3 = -1;
   private int intVal4 = -1;
   private final EventListener<PlayerTickEvent> playerTickListener = playerTickEvent -> {
      if (mc.player != null) {
         if (this.keybindSetting.isBound()
            && this.keybindSetting.getBinding().isPressed(mc.getWindow().getHandle())
            && !this.flag
            && mc.currentScreen == null
            && InventoryUtils.findInventorySlot(Items.WIND_CHARGE) != -1) {
            this.flag2 = true;
            this.flag3 = false;
         }

         this.flag = this.keybindSetting.isBound() && this.keybindSetting.getBinding().isPressed(mc.getWindow().getHandle());
         if (this.intVal > 0 && --this.intVal == 0 && mc.player.isOnGround()) {
            ((LivingEntityMixin)mc.player).setNoJumpDelay(0);
            this.intVal2 = 1;
         }

         if (this.flag6) {
            this.flag6 = false;
            this.flag3 = true;
            this.updateState();
         }

         if (this.flag5) {
            this.flag5 = false;
            this.updateState4();
         }

         if (this.flag7) {
            if (!mc.player.isOnGround()) {
               this.flag8 = true;
            } else if (this.flag8) {
               this.flag7 = false;
               this.flag8 = false;
               this.updateState4();
            }
         }

         if (this.flag4) {
            if (mc.player.isOnGround()) {
               this.flag4 = false;
               this.flag5 = true;
            }
         } else if (this.takeOnFall.isChecked() && mc.currentScreen == null && !this.flag7) {
            if (!mc.player.isOnGround() && !(mc.player.fallDistance < 2.3F)) {
               if (mc.player.getMainHandStack().getItem() != Items.MACE) {
                  if (InventoryUtils.findInventorySlot(Items.MACE) != -1) {
                     this.flag4 = true;
                     this.updateState2();
                  }
               }
            }
         }
      }
   };
   private final EventListener<MovementInputEvent> movementInputListener = movementInputEvent -> {
      if (mc.player != null) {
         if (this.flag2) {
            this.timeMillis = System.currentTimeMillis();
            if (AttackAuraModule.getInstance().isEnabled() && AttackAuraModule.getInstance().getTarget() != null) {
               RotationManager.getRotationManager().resetRotation();
            }

            if (!ScreenWalkModule.getInstance().checkState()) {
               RotationManager.getRotationManager()
                  .setRotationSimple(
                     new RotationAngles(mc.player.getYaw(), 90.0F),
                     new RotationSpeed(360.0F, ServerHelperModule.getInstance().modeSetting.isSelected("SpookyTime") ? 360.0F : 30.0F, 20.0F, 20.0F)
                  );
               if (!ServerHelperModule.getInstance().modeSetting.isSelected("SpookyTime") && Math.abs(mc.player.getPitch() - 90.0F) >= 3.0F) {
                  return;
               }
            }

            this.flag2 = false;
            this.updateState3();
         } else if (this.intVal2 > 0) {
            this.intVal2--;
            movementInputEvent.setJumping(true);
         }
      }
   };
   private final EventListener<PacketEvent> packetListener = packetEvent -> {
      if (mc.player != null) {
         if (packetEvent.isOutgoing()
            && System.currentTimeMillis() < this.timeMillis2
            && packetEvent.getPacket() instanceof PlayerInteractItemC2SPacket playerInteractItemC2SPacket) {
            packetEvent.setPacket(new PlayerInteractItemC2SPacket(playerInteractItemC2SPacket.getHand(), playerInteractItemC2SPacket.getSequence(), this.floatVal, this.floatVal2));
            this.timeMillis2 = 0L;
            this.intVal = 1;
         }
      }
   };

   @Override
   protected void onDisable() {
      this.flag2 = false;
      this.flag3 = false;
      this.flag4 = false;
      this.flag5 = false;
      this.flag6 = false;
      this.timeMillis2 = 0L;
      this.intVal = 0;
      this.intVal2 = 0;
      this.flag7 = false;
      this.flag8 = false;
      if (mc.player != null) {
         this.updateState4();
      }
   }

   private boolean matchesCriteria(float local5416_1, float local5416_2) {
      return mc.world
            .raycast(
               new RaycastContext(
                  mc.player.getEyePos(),
                  mc.player.getEyePos().add(new RotationAngles(local5416_2, local5416_1).getDirectionVector().multiply(1.5)),
                  ShapeType.COLLIDER,
                  FluidHandling.NONE,
                  mc.player
               )
            )
            .getType()
         == Type.BLOCK;
   }

   public boolean checkState() {
      return this.flag2 || System.currentTimeMillis() - this.timeMillis < 300L;
   }

   private void updateState() {
      if (mc.player != null && this.autoTakeMace.isChecked() && !this.flag7 && !this.flag4) {
         int local5418_1 = InventoryUtils.findHotbarSlot(Items.MACE) != -1
            ? InventoryUtils.findHotbarSlot(Items.MACE)
            : InventoryUtils.findInventorySlot(Items.MACE);
         if (local5418_1 != -1) {
            if (mc.player.getItemCooldownManager().isCoolingDown(mc.player.getInventory().getStack(local5418_1))) {
               NotificationHud.notifyWithText(
                  this.category.getIcon(),
                  "Булава",
                  Text.literal("в кд ещё ")
                     .setStyle(Style.EMPTY.withColor(Formatting.GRAY))
                     .append(
                        Text.literal(
                              String.format(
                                 Locale.US,
                                 "%.1fс",
                                 ((ItemCooldownAccess)mc.player.getItemCooldownManager())
                                       .remainingCooldownTicks(mc.player.getInventory().getStack(local5418_1))
                                    / 20.0F
                              )
                           )
                           .setStyle(Style.EMPTY.withColor(Formatting.RED))
                     ),
                  true
               );
            } else {
               this.flag7 = true;
               this.flag8 = false;
               this.updateState2();
            }
         }
      }
   }

   public boolean checkState2() {
      return this.flag4;
   }

   private void updateState2() {
      if (mc.player != null && mc.player.getMainHandStack().getItem() != Items.MACE) {
         if (InventoryUtils.findHotbarSlot(Items.MACE) != -1) {
            this.intVal4 = InventoryUtils.getSelectedSlot();
            InventoryUtils.setSelectedSlot(InventoryUtils.findHotbarSlot(Items.MACE), false);
         } else if (InventoryUtils.findInventorySlot(Items.MACE) != -1) {
            this.intVal3 = InventoryUtils.findInventorySlot(Items.MACE);
            InventoryUtils.swapSlotWithHand(this.intVal3, Hand.MAIN_HAND);
            InventoryUtils.closeHandledScreen(true);
         }
      }
   }

   public static MaceTestModule getInstance() {
      if (maceTestModule == null) {
         maceTestModule = new MaceTestModule();
      }

      return maceTestModule;
   }

   private MaceTestModule() {
      this.keybindSetting = this.addSetting(new KeybindSetting("Кнопка", "Кидает заряд ветра под себя с прыжком и берёт булаву."));
      this.assistAuraHit = this.addSetting(
         new BooleanSetting("Помогать киллауре в попадании", "Отдаёт киллауре расширенный радиус наводки на время захода.", false)
      );
      this.legacyNumberSetting = this.addSetting(
         new LegacyNumberSetting("Дистанция наводки", "На какой дистанции киллаура ищет цель во время захода.", 30.0, 3.0, 30.0, 0.1)
      );
      this.legacyNumberSetting.withVisibility(() -> this.assistAuraHit.isChecked());
      this.takeOnFall = this.addSetting(new BooleanSetting("Брать в падение", "Берёт булаву в руку, когда пролетел вниз 2.3 блока.", false));
      this.autoTakeMace = this.addSetting(
         new BooleanSetting("Автовзятие булавы", "После взлёта сама переключается на булаву и держит её до приземления.", true)
      );
   }

   private void updateState3() {
      if (mc.player != null && mc.world != null) {
         if (InventoryUtils.findInventorySlot(Items.WIND_CHARGE) != -1
            && !mc.player
               .getItemCooldownManager()
               .isCoolingDown(mc.player.getInventory().getStack(InventoryUtils.findInventorySlot(Items.WIND_CHARGE)))) {
            this.floatVal = mc.player.getYaw();
            this.floatVal2 = 90.0F;
            if (!mc.player.isOnGround()
               && mc.world.getBlockState(mc.player.getBlockPos().add(0, -2, 0)).isAir()
               && mc.player.getVelocity().y > 0.4) {
               this.floatVal2 = 75.0F;

               for (int index = 0; index < 360; index += 45) {
                  if (this.matchesCriteria(this.floatVal2, index)) {
                     this.floatVal = MathHelper.wrapDegrees(mc.player.getYaw() + index);
                  }
               }
            }

            this.timeMillis2 = System.currentTimeMillis() + 500L;
            RotationManager.getRotationManager().resetRotation();
            if (!ScreenWalkModule.getInstance().checkState()) {
               RotationManager.getRotationManager()
                  .setRotationSimple(new RotationAngles(this.floatVal, this.floatVal2), new RotationSpeed(360.0F, 360.0F, 20.0F, 20.0F));
            }

            InventorySlotManager.getInstance()
               .processData3(mc.player.getInventory().getStack(InventoryUtils.findInventorySlot(Items.WIND_CHARGE)).copy(), true);
            this.flag6 = true;
         }
      }
   }

   private void updateState4() {
      if (this.intVal3 != -1) {
         InventoryUtils.swapSlotWithHand(this.intVal3, Hand.MAIN_HAND);
         InventoryUtils.closeHandledScreen(true);
         this.intVal3 = -1;
      }

      if (this.intVal4 != -1) {
         InventoryUtils.setSelectedSlot(this.intVal4, false);
         this.intVal4 = -1;
      }
   }
}
