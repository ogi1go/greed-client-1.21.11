package greed.client.ui.hud;

import greed.client.render.FontText;
import greed.client.render.ThemeColors;
import greed.client.ui.UiTheme;

import greed.client.render.FontFace;
import greed.client.render.Render2D;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.text.Text;

public final class NotificationHud {
   private static final List<NotificationHud.ToastNotification> notifications = new ArrayList<>();
   private static NotificationHud.ToastNotification activeToast;

   public static void processData(boolean showDemo) {
      long nowMillis = System.currentTimeMillis();
      boolean hasActiveToast = false;

      for (NotificationHud.ToastNotification toast : notifications) {
         if (toast != activeToast && nowMillis - toast.createdAtMillis <= 3220L) {
            hasActiveToast = true;
            break;
         }
      }

      if (showDemo && !hasActiveToast) {
         if (activeToast == null || !notifications.contains(activeToast)) {
            NotificationHud.ToastNotification demoToast = new NotificationHud.ToastNotification();
            demoToast.icon = "F";
            demoToast.title = "Example";
            demoToast.description = "Notification!";
            demoToast.highlighted = true;
            activeToast = demoToast;
            notifications.add(demoToast);
            pruneExpiredToasts();
         }
      } else if (activeToast != null) {
         if (nowMillis - activeToast.createdAtMillis < 3000L) {
            activeToast.createdAtMillis = nowMillis - 3000L;
         }

         activeToast = null;
      }
   }

   private static void pruneExpiredToasts() {
      long nowMillis = System.currentTimeMillis();
      notifications.removeIf(toast -> nowMillis - toast.createdAtMillis > 3220L);
      int activeCount = 0;

      for (NotificationHud.ToastNotification candidateToast : notifications) {
         if (nowMillis - candidateToast.createdAtMillis < 3000L) {
            activeCount++;
         }
      }

      for (NotificationHud.ToastNotification expiringToast : notifications) {
         if (activeCount <= 6) {
            return;
         }

         if (nowMillis - expiringToast.createdAtMillis < 3000L) {
            expiringToast.createdAtMillis = nowMillis - 3000L;
            activeCount--;
         }
      }
   }

   public static void processData2(float viewportWidth, float viewportHeight) {
      long nowMillis = System.currentTimeMillis();
      Iterator<NotificationHud.ToastNotification> toastIterator = notifications.iterator();

      while (toastIterator.hasNext()) {
         if (nowMillis - toastIterator.next().createdAtMillis > 3220L) {
            toastIterator.remove();
         }
      }

      if (!notifications.isEmpty()) {
         float[] floatArray = HudManager.computeFloat5("toasts", viewportWidth / 2.0F, viewportHeight * 0.62F);
         float baseY = floatArray[1];
         float runningY = baseY;
         float maxWidth = 0.0F;

         for (int index = notifications.size() - 1; index >= 0; index--) {
            NotificationHud.ToastNotification toast = notifications.get(index);
            long ageMillis = nowMillis - toast.createdAtMillis;
            float inAlpha = ThemeColors.computeFloat((float)ageMillis / 220.0F);
            float alpha = Math.min(inAlpha, ThemeColors.computeFloat((float)(3220L - ageMillis) / 220.0F));
            if (!(alpha < 0.01F)) {
               maxWidth = Math.max(maxWidth, measureToastWidth(toast));
               runningY = renderToast(toast, floatArray[0], runningY, inAlpha, ageMillis, alpha);
            }
         }

         if (runningY - baseY > 0.5F) {
            HudManager.processData6(
               "toasts", floatArray[0] - maxWidth / 2.0F, baseY, maxWidth, runningY - baseY, floatArray[0], baseY
            );
         }
      }
   }

   public static void processData3(String icon, String title, String description, boolean highlighted) {
      NotificationHud.ToastNotification toast = new NotificationHud.ToastNotification();
      toast.icon = icon;
      toast.title = title;
      toast.description = description;
      toast.highlighted = highlighted;
      notifications.add(toast);
      pruneExpiredToasts();
   }

   public static void processData4(StatusEffectInstance statusEffectInstance, String title, String description, boolean highlighted) {
      NotificationHud.ToastNotification toast = new NotificationHud.ToastNotification();
      toast.statusEffect = statusEffectInstance;
      toast.title = title;
      toast.description = description;
      toast.highlighted = highlighted;
      notifications.add(toast);
      pruneExpiredToasts();
   }

   private static float measureToastWidth(NotificationHud.ToastNotification toast) {
      return 16.0F
         + FontText.computeFloat(FontFace.SEMI_BOLD, toast.title, 5.5F)
         + 1.0F
         + FontText.computeFloat(FontFace.SEMI_BOLD, toast.description, 5.5F)
         + 4.5F
         + 5.25F
         + 4.5F;
   }

   public static void notifyWithText(String icon, String title, Text text, boolean highlighted) {
      NotificationHud.ToastNotification toast = new NotificationHud.ToastNotification();
      toast.icon = icon;
      toast.title = title;
      toast.description = text.getString();
      toast.text = text;
      toast.highlighted = highlighted;
      notifications.add(toast);
      pruneExpiredToasts();
   }

   private static float renderToast(
      NotificationHud.ToastNotification toast, float centerX, float originY, float progress, long ageMillis, float alpha
   ) {
      float toastWidth = measureToastWidth(toast);
      float toastX = centerX - toastWidth / 2.0F;
      float toastY = originY - 4.0F * (1.0F - progress);
      float iconY = toastY + 7.5F;
      float remainingFraction = ThemeColors.computeFloat(1.0F - (float)ageMillis / 3000.0F);
      Render2D.pushAlpha(alpha);
      HudStyle.processData4(toastX, toastY, toastWidth, 15.0F, alpha, 5.5F);
      if (toast.statusEffect != null) {
         Render2D.drawStatusEffectIconWhite(toast.statusEffect.getEffectType(), toastX + 4.5F, iconY - 3.5F, 7.0F);
      } else {
         ThemeColors.processData(
            toast.icon,
            toastX + 4.5F + 3.5F,
            iconY,
            7.0F,
            toast.highlighted ? ThemeColors.intVal9 : ThemeColors.intVal9 & 16777215 | 2046820352
         );
      }

      float titleX = toastX + 4.5F + 7.0F + 2.0F + 0.5F + 2.0F;
      float titleY = iconY - 2.75F;
      FontText.processData3(
         FontFace.SEMI_BOLD,
         toast.title,
         titleX,
         titleY,
         5.5F,
         toast.highlighted ? HudStyle.computeInt2(1.0F) : HudStyle.computeInt2(0.6F)
      );
      float descriptionX = titleX + FontText.computeFloat(FontFace.SEMI_BOLD, toast.title, 5.5F) + 1.0F;
      int descriptionColor = toast.highlighted ? HudStyle.computeInt4(1.0F) : HudStyle.computeInt2(1.0F);
      if (toast.text != null) {
         FontText.processData5(FontFace.SEMI_BOLD, toast.text, descriptionX, titleY, 5.5F, descriptionColor);
      } else {
         FontText.processData3(FontFace.SEMI_BOLD, toast.description, descriptionX, titleY, 5.5F, descriptionColor);
      }

      ThemeColors.processData5(
         toastX + toastWidth - 4.5F - 2.625F,
         iconY,
         5.25F,
         remainingFraction,
         UiTheme.computeInt2(
            ThemeColors.intVal4, toast.highlighted ? ThemeColors.intVal9 : ThemeColors.intVal7, remainingFraction
         )
      );
      Render2D.popAlpha();
      return originY + 18.5F * alpha;
   }

   private static final class ToastNotification {
      String icon;
      StatusEffectInstance statusEffect;
      String title;
      String description;
      Text text;
      boolean highlighted;
      long createdAtMillis = System.currentTimeMillis();
   }
}
