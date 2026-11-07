package greed.client.ui;

import greed.client.util.GuiScale;
import greed.client.ui.UiTheme;
import greed.client.ui.panel.AutoBuyPanel;
import greed.client.ui.setting.SettingRenderContext;
import greed.client.ui.hud.HudRenderContext;
import greed.client.render.Render2D;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class GreedAutoBuyScreen extends Screen {
   private static GreedAutoBuyScreen greedAutoBuyScreen;
   private SettingRenderContext settingRenderContext = new SettingRenderContext();
   private long timeMillis = System.currentTimeMillis();

   @Override
   public boolean shouldPause() {
      return false;
   }

   @Override
   public boolean charTyped(CharInput charInput) {
      if (this.settingRenderContext.textSetting == null) {
         return super.charTyped(charInput);
      }

      if (charInput.codepoint() >= 32) {
         this.settingRenderContext.textSetting.insertText(String.valueOf((char)charInput.codepoint()));
      }

      return true;
   }

   @Override
   public void renderBackground(DrawContext drawContext, int local6952_2, int local6952_3, float local6952_4) {
   }

   private GreedAutoBuyScreen() {
      super(Text.literal("Greed AutoBuy"));
   }

   @Override
   public boolean mouseReleased(Click click) {
      this.settingRenderContext.numberSetting = null;
      return super.mouseReleased(click);
   }

   @Override
   public boolean mouseClicked(Click click, boolean local6955_2) {
      if (click.button() == 0) {
         this.settingRenderContext.textSetting = null;
      }

      for (int index = this.settingRenderContext.list.size() - 1; index >= this.settingRenderContext.intVal; index--) {
         if (UiTheme.matchesCriteria(
            (float)click.x() * sf(),
            (float)click.y() * sf(),
            this.settingRenderContext.list.get(index).x(),
            this.settingRenderContext.list.get(index).y(),
            this.settingRenderContext.list.get(index).w(),
            this.settingRenderContext.list.get(index).h()
         )) {
            if (click.button() == 0) {
               this.settingRenderContext.list.get(index).action().run();
               return true;
            }

            if (click.button() == 1 && this.settingRenderContext.list.get(index).rightAction() != null) {
               this.settingRenderContext.list.get(index).rightAction().run();
               return true;
            }
         }
      }

      return true;
   }

   @Override
   public boolean mouseScrolled(double local6956_1, double local6956_3, double local6956_5, double local6956_7) {
      return AutoBuyPanel.getInstance().matchesCriteria((float)local6956_1 * sf(), (float)local6956_3 * sf(), local6956_7, this.settingRenderContext.scale);
   }

   private static float sf() {
      return GuiScale.computeInt() / GuiScale.computeFloat();
   }

   @Override
   public void render(DrawContext drawContext, int local6958_2, int local6958_3, float local6958_4) {
      this.settingRenderContext
         .renderElement(drawContext, Math.min(1.0F, Math.min(this.width * sf() / 640.0F, this.height * sf() / 360.0F)), local6958_2 * sf(), local6958_3 * sf());
      Render2D.flush(drawContext);
      HudRenderContext.renderElement(drawContext);
      AutoBuyPanel.getInstance()
         .processData4(
            this.settingRenderContext,
            this.width * sf(),
            this.height * sf(),
            AutoBuyPanel.getInstance().computeFloat3(),
            Math.min(0.05F, (float)(System.currentTimeMillis() - this.timeMillis) / 1000.0F)
         );
      if (this.settingRenderContext.numberSetting != null && GLFW.glfwGetMouseButton(MinecraftClient.getInstance().getWindow().getHandle(), 0) != 1) {
         this.settingRenderContext.numberSetting = null;
      } else if (this.settingRenderContext.numberSetting != null && this.settingRenderContext.map7.get(this.settingRenderContext.numberSetting) != null) {
         this.settingRenderContext
            .numberSetting
            .setFraction(
               SettingRenderContext.computeFloat(
                  (local6958_2 * sf() - this.settingRenderContext.map7.get(this.settingRenderContext.numberSetting)[0])
                     / this.settingRenderContext.map7.get(this.settingRenderContext.numberSetting)[1],
                  0.0F,
                  1.0F
               )
            );
      }

      this.timeMillis = System.currentTimeMillis();
      Render2D.endFrame();
      if (!AutoBuyPanel.getInstance().checkState2() && AutoBuyPanel.getInstance().computeFloat3() <= 0.003F) {
         MinecraftClient.getInstance().setScreen(null);
      }
   }

   public static GreedAutoBuyScreen instance() {
      if (greedAutoBuyScreen == null) {
         greedAutoBuyScreen = new GreedAutoBuyScreen();
      }

      return greedAutoBuyScreen;
   }

   @Override
   public boolean keyPressed(KeyInput keyInput) {
      if (keyInput.key() == 256) {
         if (!AutoBuyPanel.getInstance().checkState()) {
            AutoBuyPanel.getInstance().updateState2();
         }

         return true;
      } else {
         if (this.settingRenderContext.textSetting == null) {
            return super.keyPressed(keyInput);
         }

         if (keyInput.key() == 259) {
            this.settingRenderContext.textSetting.backspace();
         } else if (keyInput.key() == 261) {
            this.settingRenderContext.textSetting.deleteForward();
         } else if (keyInput.key() == 263) {
            this.settingRenderContext.textSetting.moveCaret(-1, (keyInput.modifiers() & 1) != 0);
         } else if (keyInput.key() == 262) {
            this.settingRenderContext.textSetting.moveCaret(1, (keyInput.modifiers() & 1) != 0);
         } else if (keyInput.key() == 268) {
            this.settingRenderContext.textSetting.moveToStart((keyInput.modifiers() & 1) != 0);
         } else if (keyInput.key() == 269) {
            this.settingRenderContext.textSetting.moveToEnd((keyInput.modifiers() & 1) != 0);
         } else if (keyInput.key() == 65 && (keyInput.modifiers() & 2) != 0) {
            this.settingRenderContext.textSetting.selectAll();
         } else if (keyInput.key() == 257) {
            this.settingRenderContext.textSetting = null;
         }

         return true;
      }
   }
}
