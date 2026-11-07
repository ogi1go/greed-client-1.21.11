package greed.client.ui;

import greed.client.storage.AccountEntry;
import greed.client.render.AccountPreviewRenderer;
import greed.client.render.FontFace;
import greed.client.module.render.InterfaceModule;
import greed.client.util.GuiScale;
import greed.client.render.FontText;
import greed.client.ui.panel.AccountManagerPanel;
import greed.client.render.Render2D;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import static greed.client.MinecraftAccess.mc;

public class MainMenuScreen extends Screen {
   private final AccountManagerPanel accountManagerPanel = new AccountManagerPanel();
   private final CosmeticsSelectionScreen cosmeticsSelectionScreen = new CosmeticsSelectionScreen();
   private boolean accountManagerOpen = false;
   private boolean cosmeticsOpen = false;
   private final AnimatedValue animation = new AnimatedValue(260L);
   private final AnimatedValue animation2 = new AnimatedValue(260L);
   private final Map<String, AnimatedValue> hoverAnimations = new HashMap<>();
   private final List<MainMenuScreen.DataRecord> clickableRows = new ArrayList<>();
   private boolean welcomeAnimation;
   private long startTimeMillis = System.currentTimeMillis();

   private String welcomeName() {
      return mc.getSession().getUsername();
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return false;
   }

   @Override
   public boolean mouseReleased(Click click) {
      return this.cosmeticsOpen && click.button() == 0 && this.cosmeticsSelectionScreen.checkState() ? true : super.mouseReleased(click);
   }

   @Override
   protected void init() {
      super.init();
      if (System.currentTimeMillis() - this.startTimeMillis > 600L) {
         this.startTimeMillis = System.currentTimeMillis();
         this.welcomeAnimation = false;
         this.accountManagerOpen = false;
         this.cosmeticsOpen = false;
         this.animation.getAnimatedValue(0.0);
         this.animation2.getAnimatedValue(0.0);
         this.accountManagerPanel.updateState();
         this.cosmeticsSelectionScreen.updateState2();
      }
   }

   public MainMenuScreen(boolean showWelcome) {
      super(Text.literal("Greed"));
      this.welcomeAnimation = showWelcome;
   }

   @Override
   public boolean mouseClicked(Click click, boolean doubled) {
      if (click.button() == 0) {
         float spaceScale = spaceFactor();
         float mouseX = (float)click.x() * spaceScale;
         float mouseY = (float)click.y() * spaceScale;
         if (this.accountManagerOpen && this.animation.computeDouble() > 0.6 && this.accountManagerPanel.matchesCriteria(mouseX, mouseY)) {
            return true;
         }

         if (this.cosmeticsOpen && this.animation2.computeDouble() > 0.6 && this.cosmeticsSelectionScreen.matchesCriteria5(mouseX, mouseY)) {
            return true;
         }

         for (int index = this.clickableRows.size() - 1; index >= 0; index--) {
            if (UiTheme.matchesCriteria(
                    mouseX,
                    mouseY,
                    this.clickableRows.get(index).x(),
                    this.clickableRows.get(index).y(),
                    this.clickableRows.get(index).w(),
                    this.clickableRows.get(index).h()
            )) {
               this.clickableRows.get(index).action().run();
               return true;
            }
         }
      }

      return super.mouseClicked(click, doubled);
   }

   public MainMenuScreen() {
      this(false);
   }

   private void drawMenuContent(float scale, float verticalOffset, float mouseX, float mouseY, float viewportWidth, float viewportHeight) {
      MinecraftClient minecraftClient = this.client != null ? this.client : MinecraftClient.getInstance();
      float startX = 64.0F * scale;
      float startY = verticalOffset + 130.0F * scale;

      FontText.processData3(
              FontFace.SEMI, "Welcome to", startX, startY, 22.0F * scale, UiTheme.intVal
      );
      FontText.processData3(
              FontFace.SEMI, "Greed", startX + 132.0F * scale, startY, 22.0F * scale, UiTheme.intVal28
      );
      FontText.processData3(
              FontFace.SEMI, "long time no see.", startX, startY + 26.0F * scale, 6.5F * scale, UiTheme.intVal29
      );

      float buttonsY = startY + 54.0F * scale;
      float buttonWidth = 226.5F * scale;
      float buttonHeight = 24.0F * scale;
      float spacing = 8.0F * scale;

      this.drawMenuButton(
              startX, buttonsY, buttonWidth, buttonHeight, scale,
              "Singleplayer", "assets/greed/textures/gui/mainmenu/icons/singleplayer.png",
              mouseX, mouseY, true, () -> minecraftClient.setScreen(new SelectWorldScreen(this)), false
      );

      this.drawMenuButton(
              startX, buttonsY + buttonHeight + spacing, buttonWidth, buttonHeight, scale,
              "Multiplayer", "assets/greed/textures/gui/mainmenu/icons/pattern.png",
              mouseX, mouseY, true, () -> minecraftClient.setScreen(new MultiplayerScreen(this)), true
      );

      this.drawMenuButton(
              startX, buttonsY + (buttonHeight + spacing) * 2, buttonWidth, buttonHeight, scale,
              "Settings", "assets/greed/textures/gui/mainmenu/icons/settings.png",
              mouseX, mouseY, true, () -> minecraftClient.setScreen(new OptionsScreen(this, minecraftClient.options)), false
      );

      float smallBtnWidth = (buttonWidth - spacing) / 2.0F;
      float row4Y = buttonsY + (buttonHeight + spacing) * 3;

      this.drawMenuButton(
              startX, row4Y, smallBtnWidth, buttonHeight, scale,
              "Account Manager", "assets/greed/textures/gui/mainmenu/icons/accounts.png",
              mouseX, mouseY, true, this::toggleAlt, false
      );

      this.drawMenuButton(
              startX + smallBtnWidth + spacing, row4Y, smallBtnWidth, buttonHeight, scale,
              "Cosmetics", "assets/greed/textures/gui/mainmenu/icons/cos_menu.png",
              mouseX, mouseY, true, this::toggleCosmetics, false
      );

      float row5Y = buttonsY + (buttonHeight + spacing) * 4;
      this.drawMenuButton(
              startX, row5Y, buttonWidth, buttonHeight, scale,
              "Quit", "assets/greed/textures/gui/mainmenu/icons/exit.png",
              mouseX, mouseY, true, minecraftClient::scheduleStop, false
      );

      this.drawProfileWidget(viewportWidth, scale, mouseX, mouseY);
      this.drawSocials(viewportHeight, scale, mouseX, mouseY);
   }

   private void drawMenuButton(
           float x, float y, float w, float h, float scale,
           String label, String iconPath, float mouseX, float mouseY,
           boolean interactive, Runnable runnable, boolean isAccent
   ) {
      boolean hovered = interactive && UiTheme.matchesCriteria(mouseX, mouseY, x, y, w, h);
      float hoverAmount = this.hoverPC("btn_" + label, hovered);

      int bgColor = isAccent
              ? UiTheme.computeInt2(UiTheme.intVal7, UiTheme.intVal8, 0.6f + hoverAmount * 0.2f)
              : UiTheme.computeInt(UiTheme.intVal, 0.04F + 0.04F * hoverAmount);

      Render2D.drawRect(x, y, w, h, 6.0F * scale, bgColor);
      Render2D.drawOutline(x, y, w, h, 6.0F * scale, 0.5F, UiTheme.computeInt(UiTheme.intVal, 0.08F + 0.04F * hoverAmount));

      FontText.processData3(
              FontFace.SEMI,
              label,
              x + 12.0F * scale,
              y + (h / 2.0F) - (3.2F * scale),
              6.5F * scale,
              isAccent ? UiTheme.intVal10 : UiTheme.intVal
      );

      if (iconPath != null) {
         float iconSize = 13.0F * scale;
         UiTheme.processData3(
                 iconPath,
                 x + w - iconSize - 8.0F * scale,
                 y + (h - iconSize) + 1,
                 iconSize,
                 iconSize,
                 isAccent ? UiTheme.intVal10 : UiTheme.computeInt2(UiTheme.intVal28, UiTheme.intVal, hoverAmount)
         );
      }

      if (interactive) {
         this.clickableRows.add(new MainMenuScreen.DataRecord(x, y, w, h, runnable));
      }
   }

   private void drawProfileWidget(float viewportWidth, float scale, float mouseX, float mouseY) {
      MinecraftClient minecraftClient = this.client != null ? this.client : MinecraftClient.getInstance();
      float widgetW = 100.0F * scale;
      float widgetH = 28.0F * scale;
      float widgetX = viewportWidth - widgetW - 20.0F * scale;
      float widgetY = 16.0F * scale;

      Render2D.drawRect(widgetX, widgetY, widgetW, widgetH, widgetH / 2.0F, UiTheme.computeInt(UiTheme.intVal, 0.12F));
      Render2D.drawOutline(widgetX, widgetY, widgetW, widgetH, widgetH / 2.0F, 0.5F, UiTheme.computeInt(UiTheme.intVal, 0.18F));

      String username = this.welcomeName();
      String skinTexturePath = AccountPreviewRenderer.getSkinTextures(username, AccountEntry.getUUID(username)).body().texturePath().toString();

      Render2D.drawImageUvNearest(
              skinTexturePath,
              widgetX + 6.0F * scale,
              widgetY + 6.5F * scale,
              15.0F * scale,
              15.0F * scale,
              5.0F * scale,
              1.0F,
              0.125F,
              0.125F,
              0.25F,
              0.25F,
              UiTheme.intVal
      );
      Render2D.drawImageUvNearest(
              skinTexturePath,
              widgetX + 6.0F * scale,
              widgetY + 6.5F * scale,
              15.0F * scale,
              15.0F * scale,
              5.0F * scale,
              1.0F,
              0.625F,
              0.125F,
              0.75F,
              0.25F,
              UiTheme.intVal
      );

      FontText.processData3(
              FontFace.SEMI,
              username,
              widgetX + 28.0F * scale,
              widgetY + 6.5F * scale,
              6.0F * scale,
              UiTheme.intVal
      );
      FontText.processData3(
              FontFace.SEMI,
              "uid 1337",
              widgetX + 28.0F * scale,
              widgetY + 15.0F * scale,
              4.5F * scale,
              UiTheme.intVal29
      );
   }

   private void drawSocials(float viewportHeight, float scale, float mouseX, float mouseY) {
      float startX = 24.0F * scale;
      float startY = viewportHeight - 36.0F * scale;
      float size = 16.5F * scale;
      float spacing = 6.0F * scale;

      float tgHover = this.hoverPC("socialTelegram", UiTheme.matchesCriteria(mouseX, mouseY, startX, startY, size, size));
      UiTheme.processData3(
              "assets/greed/textures/gui/mainmenu/icons/telegram.png",
              startX + 4.0F * scale,
              startY + 4.0F * scale - tgHover * 1.5F * scale,
              8.5F * scale,
              8.5F * scale,
              UiTheme.computeInt2(UiTheme.intVal28, UiTheme.intVal, tgHover)
      );
      this.clickableRows.add(new MainMenuScreen.DataRecord(startX, startY, size, size, () -> Util.getOperatingSystem().open("https://t.me/greedlc")));

      float dcHover = this.hoverPC("socialDiscord", UiTheme.matchesCriteria(mouseX, mouseY, startX + size + spacing, startY, size, size));
      UiTheme.processData3(
              "assets/greed/textures/gui/mainmenu/icons/discord.png",
              startX + size + spacing + 4.0F * scale,
              startY + 4.0F * scale - dcHover * 1.5F * scale,
              8.5F * scale,
              8.5F * scale,
              UiTheme.computeInt2(UiTheme.intVal28, UiTheme.intVal, dcHover)
      );
      this.clickableRows.add(new MainMenuScreen.DataRecord(startX + size + spacing, startY, size, size, () -> Util.getOperatingSystem().open("")));
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   @Override
   public void renderBackground(DrawContext drawContext, int mouseX, int mouseY, float tickDelta) {
   }

   @Override
   public boolean mouseDragged(Click click, double mouseX, double mouseY) {
      return this.cosmeticsOpen
              && click.button() == 0
              && this.cosmeticsSelectionScreen.matchesCriteria2((float)click.x() * spaceFactor(), (float)click.y() * spaceFactor())
              ? true
              : super.mouseDragged(click, mouseX, mouseY);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      float spaceScale = spaceFactor();
      if (this.accountManagerOpen && this.accountManagerPanel.matchesCriteria2((float)mouseX * spaceScale, (float)mouseY * spaceScale, (float)verticalAmount)) {
         return true;
      } else {
         return this.cosmeticsOpen && this.cosmeticsSelectionScreen.matchesCriteria3((float)mouseX * spaceScale, (float)mouseY * spaceScale, (float)verticalAmount)
                 ? true
                 : super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
      }
   }

   @Override
   public void render(DrawContext drawContext, int mouseX, int mouseY, float tickDelta) {
      float viewportWidth = this.width * spaceFactor();
      float viewportHeight = this.height * spaceFactor();
      float scale = Math.min(viewportWidth / 960.0F, viewportHeight / 540.0F);
      float verticalOffset = (viewportHeight - 540.0F * scale) / 2.0F;
      float scaledMouseX = mouseX * spaceFactor();
      float scaledMouseY = mouseY * spaceFactor();
      this.clickableRows.clear();
      InterfaceModule.getInstance().updateState();
      UiTheme.flag = true;
      Render2D.flush(drawContext);
      this.animation.processData(this.accountManagerOpen ? 1.0 : 0.0);
      this.animation2.processData(this.cosmeticsOpen ? 1.0 : 0.0);
      float accountManagerProgress = (float)this.animation.computeDouble();
      float cosmeticsProgress = (float)this.animation2.computeDouble();
      float maxPanelProgress = Math.max(accountManagerProgress, cosmeticsProgress);

      UiTheme.processData6("assets/greed/textures/gui/mainmenu/background/menu.png", viewportWidth, viewportHeight, 1.0F);
      if (this.welcomeAnimation) {
         float welcomeProgress = UiTheme.computeFloat2(UiTheme.computeFloat3(this.startTimeMillis, 100L, 600L));
         if (welcomeProgress < 0.999F) {
            UiTheme.processData6("assets/greed/textures/gui/mainmenu/background/welcome.png", viewportWidth, viewportHeight, 1.0F - welcomeProgress);
         }
      }

      if (maxPanelProgress < 0.999F) {
         Render2D.pushAlpha(1.0F - maxPanelProgress);
         this.drawMenuContent(scale, verticalOffset, scaledMouseX, scaledMouseY, viewportWidth, viewportHeight);
         Render2D.popAlpha();
      }

      if (accountManagerProgress > 0.001F) {
         Render2D.pushAlpha(accountManagerProgress);
         this.accountManagerPanel
                 .renderElement(drawContext, 36.0F * scale * (1.0F - accountManagerProgress), scale, verticalOffset, scaledMouseX, scaledMouseY);
         Render2D.popAlpha();
      }

      if (cosmeticsProgress > 0.001F) {
         Render2D.pushAlpha(cosmeticsProgress);
         this.cosmeticsSelectionScreen
                 .renderElement(
                         drawContext,
                         36.0F * scale * (1.0F - cosmeticsProgress),
                         scale,
                         verticalOffset,
                         viewportWidth,
                         viewportHeight,
                         scaledMouseX,
                         scaledMouseY
                 );
         Render2D.popAlpha();
      }

      Render2D.popAlpha();
      Render2D.endFrame();
      if (accountManagerProgress > 0.5F) {
         this.accountManagerPanel.renderElement3(drawContext, 36.0F * scale * (1.0F - accountManagerProgress), scale, verticalOffset);
      }
   }

   @Override
   public boolean keyPressed(KeyInput keyInput) {
      if (this.accountManagerOpen) {
         if (this.accountManagerPanel.matchesCriteria3(keyInput.key())) {
            return true;
         }

         if (keyInput.key() == 256) {
            this.toggleAlt();
            return true;
         }
      }

      if (this.cosmeticsOpen) {
         if (this.cosmeticsSelectionScreen.matchesCriteria(keyInput.key())) {
            return true;
         }

         if (keyInput.key() == 256) {
            this.toggleCosmetics();
            return true;
         }
      }

      return super.keyPressed(keyInput);
   }

   @Override
   public boolean charTyped(CharInput charInput) {
      if (this.accountManagerOpen && this.accountManagerPanel.matchesCriteria4(charInput.codepoint())) {
         return true;
      } else {
         return this.cosmeticsOpen && this.cosmeticsSelectionScreen.matchesCriteria4(charInput.codepoint()) ? true : super.charTyped(charInput);
      }
   }

   private float hoverPC(String key, boolean hovered) {
      AnimatedValue animatedValue = this.hoverAnimations.computeIfAbsent(key, ignoredKey -> new AnimatedValue(150L));
      animatedValue.processData(hovered ? 1.0 : 0.0);
      return (float)animatedValue.computeDouble();
   }

   private void toggleAlt() {
      this.accountManagerOpen = !this.accountManagerOpen;
      if (this.accountManagerOpen) {
         this.accountManagerPanel.processData2(this::toggleAlt);
      } else {
         this.accountManagerPanel.updateState();
      }
   }

   private void toggleCosmetics() {
      this.cosmeticsOpen = !this.cosmeticsOpen;
      if (this.cosmeticsOpen) {
         this.cosmeticsSelectionScreen.processData(this::toggleCosmetics);
      } else {
         this.cosmeticsSelectionScreen.updateState2();
      }
   }

   private static float spaceFactor() {
      return GuiScale.computeInt() / GuiScale.computeFloat();
   }

   private record DataRecord(float x, float y, float w, float h, Runnable action) {
   }
}