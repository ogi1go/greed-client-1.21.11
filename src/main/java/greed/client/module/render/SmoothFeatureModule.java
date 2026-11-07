package greed.client.module.render;

import greed.client.setting.BooleanSetting;
import greed.mixin.ChatHudMixin;
import greed.client.event.ContainerRenderEvent;
import greed.client.event.EventListener;
import greed.mixin.HandledScreenMixin;
import greed.client.util.MathUtils;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.module.ModuleManager;
import greed.client.event.PacketEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.entity.EntityPosition;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(name = "SmoothFeature", description = "Плавные мелочи клиента: предметы под курсором, чат, камера при флагах.", category = ModuleCategory.RENDER)
public final class SmoothFeatureModule extends Module {
   private static SmoothFeatureModule smoothFeatureModule;
   public BooleanSetting itemScaleOnCursor;
   public BooleanSetting smoothChat;
   public BooleanSetting chatAntispam;
   public BooleanSetting smoothCameraOnFlags;
   private final HashMap<Slot, Float> map = new HashMap<>();
   private long timeMillis;
   private boolean flag;
   private String strVal = "";
   private int intVal = 1;
   private long timeMillis2;
   private boolean flag2;
   private Vec3d vec3d2;
   private long timeMillis3;
   private long timeMillis4;
   private final EventListener<ContainerRenderEvent> containerRenderListener = containerRenderEvent -> {
      long local5342_2 = System.currentTimeMillis();
      float local5342_4 = 1.0F - (float)Math.exp(-Math.min(100L, local5342_2 - this.timeMillis) / 60.0);
      this.timeMillis = local5342_2;
      Slot slot = ((HandledScreenMixin)containerRenderEvent.getHandledScreen()).hoveredSlot();
      if (slot != null && this.map.size() < 256) {
         this.map.putIfAbsent(slot, 0.0F);
      }

      this.map.entrySet().removeIf(local5336_1 -> local5336_1.getValue() < 0.004F && local5336_1.getKey() != slot);

      for (Entry<Slot, Float> local5342_7 : this.map.entrySet()) {
         local5342_7.setValue(local5342_7.getValue() + ((local5342_7.getKey() == slot ? 1.0F : 0.0F) - local5342_7.getValue()) * local5342_4);
      }
   };
   private final EventListener<PacketEvent> packetListener = packetEvent -> {
      if (mc.player != null
         && this.smoothCameraOnFlags.isChecked()
         && packetEvent.isIncoming()
         && packetEvent.getPacket() instanceof PlayerPositionLookS2CPacket playerPositionLookS2CPacket) {
         Vec3d vec3d = EntityPosition.apply(EntityPosition.fromEntity(mc.player), playerPositionLookS2CPacket.change(), playerPositionLookS2CPacket.relatives()).position();
         double local5334_4 = mc.player.getEntityPos().distanceTo(vec3d);
         if (!(local5334_4 < 0.05) && !(local5334_4 > 24.0)) {
            this.vec3d2 = mc.player.getEntityPos().subtract(vec3d);
            this.timeMillis3 = System.currentTimeMillis();
            this.timeMillis4 = (long)MathUtils.computeFloat12(160.0F, 420.0F, 140.0F + (float)local5334_4 * 40.0F);
         }
      }
   };

   public void renderElement(DrawContext drawContext, Slot slot) {
      this.flag = false;
      if (this.isEnabled() && this.itemScaleOnCursor.isChecked()) {
         Float local5333_3 = this.map.get(slot);
         if (local5333_3 != null && !(local5333_3 < 0.004F)) {
            this.flag = true;
            float local5333_4 = 1.0F + 0.13F * local5333_3;
            float local5333_5 = slot.x + 8.0F;
            float local5333_6 = slot.y + 8.0F;
            drawContext.getMatrices().pushMatrix();
            drawContext.getMatrices().translate(local5333_5, local5333_6);
            drawContext.getMatrices().scale(local5333_4, local5333_4);
            drawContext.getMatrices().translate(-local5333_5, -local5333_6);
         }
      }
   }

   public void updateState() {
      this.timeMillis2 = System.currentTimeMillis();
   }

   private SmoothFeatureModule() {
      this.itemScaleOnCursor = this.addSetting(
         new BooleanSetting("Увеличение предмета под курсором", "Плавно подращивает предмет при наводке во всех экранах и шалкерах.", true)
      );
      this.smoothChat = this.addSetting(new BooleanSetting("Плавный чат", "Новые сообщения мягко выезжают снизу, а не появляются рывком.", true));
      this.chatAntispam = this.addSetting(new BooleanSetting("Антиспам чата", "Повторы одного сообщения не плодятся, а помечаются счётчиком (x2).", true));
      this.smoothCameraOnFlags = this.addSetting(
         new BooleanSetting("Плавная камера при флагах", "Откат позиции сервером не дёргает камеру, а плавно доводит её.", false)
      );
      ModuleManager.unregisterModule(this);
      this.setEnabledWithoutCallbacks(true);
   }

   @Override
   protected void onDisable() {
      this.map.clear();
      this.flag = this.flag2 = false;
      this.strVal = "";
      this.intVal = 1;
      this.vec3d2 = null;
   }

   public static SmoothFeatureModule getInstance() {
      if (smoothFeatureModule == null) {
         smoothFeatureModule = new SmoothFeatureModule();
      }

      return smoothFeatureModule;
   }

   public void renderElement2(DrawContext drawContext) {
      if (this.flag) {
         this.flag = false;
         drawContext.getMatrices().popMatrix();
      }
   }

   public void renderElement3(DrawContext drawContext) {
      if (this.flag2) {
         this.flag2 = false;
         drawContext.getMatrices().popMatrix();
      }
   }

   public boolean checkState() {
      return this.isEnabled()
         && this.smoothCameraOnFlags.isChecked()
         && this.vec3d2 != null
         && System.currentTimeMillis() - this.timeMillis3 < this.timeMillis4
         && !FreeCameraModule.getInstance().checkState2();
   }

   public Vec3d getVec3d() {
      float local5344_1 = MathUtils.clampUnit((float)(System.currentTimeMillis() - this.timeMillis3) / (float)this.timeMillis4);
      return this.vec3d2.multiply((1.0F - local5344_1) * (1.0F - local5344_1) * (1.0F - local5344_1));
   }

   public void renderElement4(ChatHud chatHud, DrawContext drawContext) {
      this.flag2 = false;
      if (this.isEnabled() && this.smoothChat.isChecked() && ((ChatHudMixin)chatHud).chatScrollbarPos() == 0) {
         float local5345_3 = MathUtils.clampUnit((float)(System.currentTimeMillis() - this.timeMillis2) / 240.0F);
         if (!(local5345_3 >= 1.0F)) {
            float local5345_4 = 1.0F - (1.0F - local5345_3) * (1.0F - local5345_3) * (1.0F - local5345_3);
            this.flag2 = true;
            drawContext.getMatrices().pushMatrix();
            drawContext.getMatrices().translate(0.0F, 9.0F * mc.options.getChatScale().getValue().floatValue() * (1.0F - local5345_4));
         }
      }
   }

   public boolean matchesCriteria(ChatHud chatHud, Text text) {
      if (this.isEnabled() && this.chatAntispam.isChecked()) {
         String string = text.getString();
         List<ChatHudLine> local5346_4 = ((ChatHudMixin)chatHud).allMessages();
         if (!string.isBlank() && !local5346_4.isEmpty() && string.equals(this.strVal)) {
            this.intVal++;
            ChatHudLine chatHudLine = local5346_4.get(0);
            local5346_4.set(
               0,
               new ChatHudLine(
                  chatHudLine.creationTick(),
                  text.copy().append(Text.literal(" (x" + this.intVal + ")").formatted(Formatting.GRAY)),
                  chatHudLine.signature(),
                  chatHudLine.indicator()
               )
            );
            ((ChatHudMixin)chatHud).invokeRefreshTrimmedMessages();
            this.updateState();
            return true;
         } else {
            this.strVal = string;
            this.intVal = 1;
            return false;
         }
      } else {
         return false;
      }
   }
}
