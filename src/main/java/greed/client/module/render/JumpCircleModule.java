package greed.client.module.render;

import greed.client.setting.BooleanSetting;
import greed.client.event.EventListener;
import greed.client.setting.LegacyNumberSetting;
import greed.client.setting.ModeSetting;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.event.PlayerTickEvent;
import greed.client.setting.ColorSetting;
import greed.client.setting.SectionSetting;

import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

@ModuleInfo(name = "JumpCircle", description = "Светящийся круг на земле в месте прыжка.", category = ModuleCategory.RENDER)
public final class JumpCircleModule extends Module {
   private static JumpCircleModule jumpCircleModule;
   public LegacyNumberSetting range;
   public LegacyNumberSetting glow;
   public LegacyNumberSetting distortion;
   public ModeSetting modeSetting;
   public BooleanSetting radiusWave;
   public LegacyNumberSetting lifetime;
   public BooleanSetting customColor;
   public ColorSetting colorSetting;
   private final CopyOnWriteArrayList<JumpCircleModule.DataRecord> list = new CopyOnWriteArrayList<>();
   private boolean flag;
   private final EventListener<PlayerTickEvent> playerTickListener = playerTickEvent -> {
      if (mc.player != null) {
         if (this.flag && !mc.player.isOnGround() && mc.player.getVelocity().y > 0.3) {
            if (this.list.size() >= 16) {
               this.list.remove(0);
            }

            float[] floatArray = new float[32];
            Vec3d vec3d = new Vec3d(mc.player.getX(), mc.player.lastRenderY + 0.6, mc.player.getZ());
            double local1110_4 = this.range.getValue() + this.glow.getValue() * 2.0F + 1.0F;

            for (int index = 0; index < 32; index++) {
               HitResult hitResult = mc.world
                  .raycast(
                     new RaycastContext(
                        vec3d,
                        new Vec3d(
                           vec3d.x + Math.cos(index * Math.PI / 16.0) * local1110_4,
                           vec3d.y,
                           vec3d.z + Math.sin(index * Math.PI / 16.0) * local1110_4
                        ),
                        ShapeType.COLLIDER,
                        FluidHandling.NONE,
                        mc.player
                     )
                  );
               floatArray[index] = hitResult.getType() == Type.MISS ? 999.0F : (float)hitResult.getPos().distanceTo(vec3d);
            }

            this.list
               .add(
                  new JumpCircleModule.DataRecord(
                     mc.player.getX(), mc.player.lastRenderY, mc.player.getZ(), System.currentTimeMillis(), floatArray
                  )
               );
         }

         this.flag = mc.player.isOnGround();
         this.list.removeIf(local1115_1 -> (float)(System.currentTimeMillis() - local1115_1.bornMs()) > this.lifetime.getValue());
      }
   };

   private JumpCircleModule() {
      this.addSetting(new SectionSetting("Кольцо"));
      this.range = this.addSetting(new LegacyNumberSetting("Радиус", "Радиус круга в блоках.", 1.5, 0.5, 4.0, 0.1));
      this.glow = this.addSetting(new LegacyNumberSetting("Свечение", "Мягкость дистанс-поля кольца.", 0.35, 0.05, 1.5, 0.05));
      this.radiusWave = this.addSetting(new BooleanSetting("Волна радиуса", "Радиус расходится волной от точки прыжка, а не статичен.", true));
      this.lifetime = this.addSetting(new LegacyNumberSetting("Время жизни", "Сколько миллисекунд живёт круг.", 600.0, 300.0, 3000.0, 50.0));
      this.addSetting(new SectionSetting("Искажение"));
      this.distortion = this.addSetting(new LegacyNumberSetting("Искажение", "Сила преломления картинки.", 0.35, 0.0, 1.0, 0.05));
      this.modeSetting = this.addSetting(
         new ModeSetting("Эффект", "Как искажать картинку: волна вокруг кольца или вода с тенью.", "Волна", "Волна", "Вода")
      );
      this.addSetting(new SectionSetting("Цвет"));
      this.customColor = this.addSetting(new BooleanSetting("Свой цвет", "Использовать свой цвет вместо ACCENT.", false));
      this.colorSetting = this.addSetting(new ColorSetting("Цвет", "Цвет круга."));
      this.colorSetting.withVisibility(() -> this.customColor.isChecked());
   }

   public CopyOnWriteArrayList<JumpCircleModule.DataRecord> getCopyOnWriteArrayList() {
      return this.list;
   }

   @Override
   protected void onDisable() {
      this.list.clear();
   }

   public static JumpCircleModule getInstance() {
      if (jumpCircleModule == null) {
         jumpCircleModule = new JumpCircleModule();
      }

      return jumpCircleModule;
   }

   public float computeFloat(JumpCircleModule.DataRecord local1116_1) {
      return Math.min(1.0F, (float)(System.currentTimeMillis() - local1116_1.bornMs()) / this.lifetime.getValue());
   }

   public record DataRecord(double x, double y, double z, long bornMs, float[] wallDistBlocks) {
   }
}
