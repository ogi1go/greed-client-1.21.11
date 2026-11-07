package greed.client.util;

import greed.client.module.player.ServerHelperModule;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public enum ServerItemType {
   CRACKER(
      "${rgba(255,20,147,0)}[${rgba(231,41,152,0)}★${rgba(208,62,157,0)}]${rgba(185,84,162,0)} ${rgba(162,105,167,0)}Х${rgba(139,126,172,0)}л${rgba(115,148,178,0)}о${rgba(92,169,183,0)}п${rgba(69,190,188,0)}у${rgba(46,212,193,0)}ш${rgba(23,233,198,0)}к${rgba(0,255,204,0)}а",
      16738740,
      new ServerItemType.DataRecord(StatusEffects.SLOWNESS, 200, 9),
      new ServerItemType.DataRecord(StatusEffects.BLINDNESS, 100, 9)
   ),
   RADIATION(
      "${rgba(255,247,58,0)}[${rgba(248,247,58,0)}★${rgba(242,247,58,0)}]${rgba(235,248,58,0)} ${rgba(229,248,58,0)}З${rgba(223,249,59,0)}е${rgba(216,249,59,0)}л${rgba(210,250,59,0)}ь${rgba(204,250,59,0)}е${rgba(197,251,60,0)} ${rgba(191,251,60,0)}Р${rgba(185,252,60,0)}а${rgba(178,252,60,0)}д${rgba(172,253,61,0)}и${rgba(166,253,61,0)}а${rgba(159,254,61,0)}ц${rgba(153,254,61,0)}и${rgba(147,255,62,0)}и",
      3329330,
      new ServerItemType.DataRecord(StatusEffects.POISON, 1200, 1),
      new ServerItemType.DataRecord(StatusEffects.WITHER, 1200, 1),
      new ServerItemType.DataRecord(StatusEffects.SLOWNESS, 1800, 2)
   ),
   SLEEPING(
      "${rgba(63,13,154,0)}[${rgba(66,23,160,0)}★${rgba(69,33,166,0)}]${rgba(72,43,172,0)} ${rgba(75,53,178,0)}С${rgba(79,64,184,0)}н${rgba(82,74,190,0)}о${rgba(85,84,196,0)}т${rgba(88,94,202,0)}в${rgba(92,105,208,0)}о${rgba(95,115,214,0)}р${rgba(98,125,220,0)}н${rgba(101,135,226,0)}о${rgba(105,146,233,0)}е",
      254,
      new ServerItemType.DataRecord(StatusEffects.WEAKNESS, 1800, 1),
      new ServerItemType.DataRecord(StatusEffects.MINING_FATIGUE, 200, 1),
      new ServerItemType.DataRecord(StatusEffects.WITHER, 1800, 2),
      new ServerItemType.DataRecord(StatusEffects.BLINDNESS, 200, 0)
   ),
   WRATH(
      "${rgba(139,0,0,0)}[${rgba(147,10,0,0)}★${rgba(155,20,0,0)}]${rgba(163,30,0,0)} ${rgba(172,40,0,0)}З${rgba(180,50,0,0)}е${rgba(188,60,0,0)}л${rgba(197,70,0,0)}ь${rgba(205,80,0,0)}е${rgba(213,89,0,0)} ${rgba(221,100,0,0)}Г${rgba(230,109,0,0)}н${rgba(238,120,0,0)}е${rgba(246,129,0,0)}в${rgba(255,140,0,0)}а",
      10040115,
      new ServerItemType.DataRecord(StatusEffects.STRENGTH, 600, 4),
      new ServerItemType.DataRecord(StatusEffects.SLOWNESS, 600, 3)
   ),
   ASSASSIN(
      "${rgba(65,67,69,0)}[${rgba(68,63,64,0)}★${rgba(72,59,60,0)}]${rgba(76,55,56,0)} ${rgba(79,51,52,0)}З${rgba(83,47,48,0)}е${rgba(87,43,44,0)}л${rgba(90,39,40,0)}ь${rgba(94,35,36,0)}е${rgba(98,31,32,0)} ${rgba(102,27,28,0)}А${rgba(105,23,24,0)}с${rgba(109,19,20,0)}с${rgba(113,15,16,0)}а${rgba(116,11,12,0)}с${rgba(120,7,8,0)}и${rgba(124,3,4,0)}н${rgba(128,0,0,0)}а",
      3355443,
      new ServerItemType.DataRecord(StatusEffects.STRENGTH, 1200, 3)
   ),
   PALADIN(
      "${rgba(209,255,91,0)}[${rgba(209,255,91,0)}★${rgba(209,255,91,0)}]${rgba(209,255,91,0)} ${rgba(209,255,91,0)}З${rgba(209,255,91,0)}е${rgba(209,255,91,0)}л${rgba(209,255,91,0)}ь${rgba(209,255,91,0)}е${rgba(209,255,91,0)} ${rgba(209,255,91,0)}П${rgba(209,255,91,0)}а${rgba(209,255,91,0)}л${rgba(209,255,91,0)}л${rgba(209,255,91,0)}а${rgba(209,255,91,0)}д${rgba(209,255,91,0)}и${rgba(209,255,91,0)}н${rgba(209,255,91,0)}а",
      65535,
      new ServerItemType.DataRecord(StatusEffects.HEALTH_BOOST, 1200, 2)
   ),
   HOLY_WATER(
      "${rgba(255,255,203,0)}[${rgba(255,254,192,0)}★${rgba(255,254,181,0)}]${rgba(255,254,170,0)} ${rgba(255,254,159,0)}С${rgba(255,254,148,0)}в${rgba(255,254,137,0)}я${rgba(255,254,126,0)}т${rgba(255,254,115,0)}а${rgba(255,254,104,0)}я${rgba(255,254,93,0)} ${rgba(255,254,82,0)}в${rgba(255,254,71,0)}о${rgba(255,254,60,0)}д${rgba(255,254,50,0)}а",
      16777215,
      new ServerItemType.DataRecord(StatusEffects.REGENERATION, 900, 1)
   );

   private final int intVal;
   private final List<ServerItemType.DataRecord> list;
   private final Text text2;
   private final Text text3;
   private final String strVal;

   private ServerItemType(String local3862_3, int local3862_4, ServerItemType.DataRecord... nested0434Array) {
      this.intVal = local3862_4;
      this.list = List.of(nested0434Array);
      this.text2 = getMutableText(local3862_3, 0);
      this.text3 = getMutableText(local3862_3, 4);
      this.strVal = this.text2.getString();
   }

   public int computeInt() {
      return this.intVal;
   }

   public Text getText() {
      return this.text3;
   }

   public static ServerItemType getServerItemType(String local3865_0) {
      for (ServerItemType serverItemType : values()) {
         if (serverItemType.text3.getString().equalsIgnoreCase(local3865_0.trim())) {
            return serverItemType;
         }
      }

      return null;
   }

   public List<ServerItemType.DataRecord> getList() {
      return this.list;
   }

   public Text getText2() {
      return this.text2;
   }

   public String formatText() {
      return this.strVal;
   }

   public int computeInt2(double local3869_1) {
      if (local3869_1 <= 0.0) {
         return 0;
      }

      int local3869_3 = 0;

      for (ServerItemType.DataRecord local3869_5 : this.list) {
         int local3869_6 = MathHelper.floor(local3869_5.ticks() * local3869_1 + 0.5);
         if (local3869_6 > 20) {
            local3869_3 = Math.max(local3869_3, local3869_6);
         }
      }

      return local3869_3;
   }

   public static ServerItemType getServerItemType2(int local3870_0) {
      int local3870_1 = local3870_0 & 16777215;

      for (ServerItemType serverItemType : values()) {
         if (serverItemType.intVal == local3870_1) {
            return serverItemType;
         }
      }

      int local3870_2 = local3870_1 >> 16 & 0xFF;
      int local3870_3 = local3870_1 >> 8 & 0xFF;
      int local3870_4 = local3870_1 & 0xFF;
      boolean local3870_5 = Math.abs(local3870_2 - local3870_3) <= 10
         && Math.abs(local3870_3 - local3870_4) <= 10
         && Math.abs(local3870_2 - local3870_4) <= 10
         && local3870_2 >= 14
         && local3870_2 <= 88;
      return local3870_5 ? ASSASSIN : null;
   }

   public static ServerItemType getServerItemType3(BlockPos blockPos) {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      if (minecraftClient.world == null) {
         return null;
      }

      for (PotionEntity potionEntity : minecraftClient.world.getNonSpectatingEntities(PotionEntity.class, new Box(blockPos).expand(3.0))) {
         String local3871_4 = Formatting.strip(potionEntity.getStack().getName().getString()).toLowerCase();

         for (ServerItemType serverItemType : values()) {
            if (local3871_4.contains(serverItemType.text3.getString().toLowerCase())) {
               return serverItemType;
            }
         }
      }

      return null;
   }

   public static boolean matchesCriteria(int local3872_0, int local3872_1) {
      return Math.abs(local3872_0 - local3872_1) <= 1;
   }

   public static double computeDouble(Box box, Vec3d vec3d) {
      return 1.0 - Math.sqrt(box.squaredMagnitude(vec3d)) / 4.0;
   }

   public static boolean checkState() {
      ServerHelperModule serverHelperModule = ServerHelperModule.getInstance();
      return serverHelperModule.isEnabled() && (serverHelperModule.modeSetting.isSelected("FunTime") || serverHelperModule.modeSetting.isSelected("SpookyTime"));
   }

   private static MutableText getMutableText(String local3875_0, int local3875_1) {
      MutableText mutableText = Text.empty();
      Matcher matcher = Pattern.compile("\\$\\{rgba\\((\\d+),(\\d+),(\\d+),(\\d+)\\)\\}([^$]*)").matcher(local3875_0);
      int local3875_4 = 0;

      while (matcher.find()) {
         if (local3875_4++ >= local3875_1) {
            mutableText.append(
               Text.literal(matcher.group(5))
                  .setStyle(
                     Style.EMPTY
                        .withColor(
                           TextColor.fromRgb(
                              Integer.parseInt(matcher.group(1)) << 16
                                 | Integer.parseInt(matcher.group(2)) << 8
                                 | Integer.parseInt(matcher.group(3))
                           )
                        )
                  )
            );
         }
      }

      return mutableText;
   }

   public record DataRecord(RegistryEntry<StatusEffect> effect, int ticks, int amplifier) {
   }
}
