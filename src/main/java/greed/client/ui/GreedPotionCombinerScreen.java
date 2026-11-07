package greed.client.ui;

import greed.client.ui.panel.PotionPresetPanel;

import greed.client.util.InputBinding;
import greed.client.util.PotionPreset;
import greed.client.util.GuiScale;
import greed.client.ui.panel.PotionPresetPanel;
import greed.client.ui.UiTheme;
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

public final class GreedPotionCombinerScreen extends Screen {
   private static GreedPotionCombinerScreen greedPotionCombinerScreen;
   private SettingRenderContext settingRenderContext = new SettingRenderContext();
   private long timeMillis = System.currentTimeMillis();

   @Override
   public void render(DrawContext drawContext, int local827_2, int local827_3, float local827_4) {
      this.settingRenderContext
         .renderElement(
            drawContext, Math.min(1.66F, Math.min(this.width * sf() / 640.0F, this.height * sf() / 360.0F) * 1.66F), local827_2 * sf(), local827_3 * sf()
         );
      Render2D.flush(drawContext);
      HudRenderContext.renderElement(drawContext);
      PotionPresetPanel.getInstance()
         .processData7(
            this.settingRenderContext,
            this.width * sf(),
            this.height * sf(),
            PotionPresetPanel.getInstance().computeFloat(),
            Math.min(0.05F, (float)(System.currentTimeMillis() - this.timeMillis) / 1000.0F)
         );
      this.timeMillis = System.currentTimeMillis();
      Render2D.endFrame();
      if (!PotionPresetPanel.getInstance().checkState() && PotionPresetPanel.getInstance().computeFloat() <= 0.003F) {
         MinecraftClient.getInstance().setScreen(null);
      }
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   public static GreedPotionCombinerScreen instance() {
      if (greedPotionCombinerScreen == null) {
         greedPotionCombinerScreen = new GreedPotionCombinerScreen();
      }

      return greedPotionCombinerScreen;
   }

   private GreedPotionCombinerScreen() {
      super(Text.literal("Greed PotionCombiner"));
   }

   @Override
   public void renderBackground(DrawContext drawContext, int local831_2, int local831_3, float local831_4) {
   }

   @Override
   public boolean mouseScrolled(double local832_1, double local832_3, double local832_5, double local832_7) {
      return PotionPresetPanel.getInstance().matchesCriteria((float)local832_1 * sf(), (float)local832_3 * sf(), local832_7, this.settingRenderContext.scale);
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
   public boolean keyPressed(KeyInput keyInput) {
      if (this.settingRenderContext.keybindSetting != null) {
         this.settingRenderContext.keybindSetting.withKeyCode(keyInput.key() != 256 && keyInput.key() != 261 ? keyInput.key() : -1);
         this.settingRenderContext.keybindSetting = null;
         PotionPreset.getInstance().updateState();
         return true;
      }

      if (keyInput.key() == 256) {
         if (!PotionPresetPanel.getInstance().checkState2()) {
            PotionPresetPanel.getInstance().updateState();
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

   private static float sf() {
      return GuiScale.computeInt() / GuiScale.computeFloat();
   }

   @Override
   public boolean mouseClicked(Click click, boolean local836_2) {
      if (this.settingRenderContext.keybindSetting != null) {
         if (click.button() != 0) {
            this.settingRenderContext.keybindSetting.setBinding(InputBinding.mouseButton(click.button()));
         }

         this.settingRenderContext.keybindSetting = null;
         PotionPreset.getInstance().updateState();
         if (click.button() != 0) {
            return true;
         }
      }

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
}
