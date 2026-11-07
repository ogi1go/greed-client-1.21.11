package greed.client.ui;

import greed.client.storage.AccountEntry;
import greed.client.render.AccountPreviewRenderer;
import greed.client.render.FontFace;
import greed.client.module.render.InterfaceModule;
import greed.client.util.GuiScale;
import greed.client.render.FontText;
import greed.client.render.Render2D;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class WelcomeScreen extends Screen {
   private final long timeMillis = System.currentTimeMillis();
   private long timeMillis2;
   private long timeMillis3;
   private final AnimatedValue animatedValue = new AnimatedValue(220L);
   private float buttonX;
   private float buttonY;
   private float buttonWidth;
   private float buttonHeight;

   public WelcomeScreen() {
      super(Text.literal("Greed"));
   }

   @Override
   public boolean mouseClicked(Click click, boolean unused) {
      if (click.button() == 0) {
         long now = System.currentTimeMillis();
         if (this.timeMillis2 == 0L) {
            if (now - this.timeMillis > 300L) {
               this.timeMillis2 = now;
            }
            return true;
         }

         float factor = spaceFactor();
         if (this.timeMillis3 == 0L && UiTheme.matchesCriteria(
                 (float) click.x() * factor,
                 (float) click.y() * factor,
                 this.buttonX, this.buttonY, this.buttonWidth, this.buttonHeight
         )) {
            this.timeMillis3 = now;
            return true;
         }
      }

      return super.mouseClicked(click, unused);
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return false;
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   @Override
   public void renderBackground(DrawContext drawContext, int mouseX, int mouseY, float delta) {
   }

   private static float spaceFactor() {
      return GuiScale.computeInt() / GuiScale.computeFloat();
   }

   @Override
   public void tick() {
      if (this.timeMillis3 != 0L && System.currentTimeMillis() - this.timeMillis3 >= 320L) {
         MinecraftClient mc = this.client != null ? this.client : MinecraftClient.getInstance();
         mc.setScreen(new MainMenuScreen(true));
      }
   }

   @Override
   public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
      InterfaceModule.getInstance().updateState();

      float screenWidth = this.width * spaceFactor();
      float screenHeight = this.height * spaceFactor();
      float scale = Math.min(screenWidth / 960.0F, screenHeight / 540.0F);
      long now = System.currentTimeMillis();

      UiTheme.flag = true;
      Render2D.flush(drawContext);

      if (this.timeMillis2 == 0L && now - this.timeMillis >= 2200L) {
         this.timeMillis2 = now;
      }

      float progressIn = this.timeMillis2 == 0L ? 0.0F : UiTheme.computeFloat2(UiTheme.computeFloat3(this.timeMillis2, 0L, 350L));

      UiTheme.processData6("assets/greed/textures/gui/mainmenu/background/menu.png", screenWidth, screenHeight, 1.0F);
      if (progressIn > 0.001F) {
         UiTheme.processData6("assets/greed/textures/gui/mainmenu/background/welcome.png", screenWidth, screenHeight, progressIn);
      }

      if (progressIn < 0.999F) {
         Render2D.pushAlpha(1.0F - progressIn);
         UiTheme.processData4(this.timeMillis, screenWidth, screenHeight);
         Render2D.popAlpha();
      }

      if (progressIn > 0.001F) {
         float progressOut = this.timeMillis3 == 0L ? 0.0F : UiTheme.computeFloat2(UiTheme.computeFloat3(this.timeMillis3, 0L, 280L));
         Render2D.pushAlpha(progressIn * (1.0F - progressOut));

         this.drawCenteredWelcome(
                 screenWidth, screenHeight, scale,
                 (float) mouseX * spaceFactor(), (float) mouseY * spaceFactor(),
                 progressOut
         );
         Render2D.popAlpha();
      }

      Render2D.endFrame();
   }

   private void drawAccountPill(float x, float y, float scale) {
      MinecraftClient mc = this.client != null ? this.client : MinecraftClient.getInstance();
      String username = mc != null && mc.getSession() != null ? mc.getSession().getUsername() : "Player";
      String skinPath = AccountPreviewRenderer.getSkinTextures(username, AccountEntry.getUUID(username)).body().texturePath().toString();

      float pillW = 115.0F * scale;
      float pillH = 22.0F * scale;
      float radius = pillH / 2.0F;

      Render2D.drawRect(x, y, pillW, pillH, radius, UiTheme.computeInt(UiTheme.intVal, 0.12F));
      Render2D.drawOutline(x, y, pillW, pillH, radius, 0.5F, UiTheme.computeInt(UiTheme.intVal, 0.18F));

      Render2D.drawImageUvNearest(skinPath, x + 3.5F * scale, y + 3.5F * scale, 15.0F * scale, 15.0F * scale, 5.0F * scale, 1.0F, 0.125F, 0.125F, 0.25F, 0.25F, UiTheme.intVal);
      Render2D.drawImageUvNearest(skinPath, x + 3.5F * scale, y + 3.5F * scale, 15.0F * scale, 15.0F * scale, 5.0F * scale, 1.0F, 0.625F, 0.125F, 0.75F, 0.25F, UiTheme.intVal);

      FontText.processData3(FontFace.SEMI, username, x + 23.0F * scale, y + 5.0F * scale, 6.0F * scale, UiTheme.intVal);
      FontText.processData3(FontFace.SEMI, "uid 1337", x + 23.0F * scale, y + 13.5F * scale, 4.5F * scale, UiTheme.intVal29);
   }

   private void drawCenteredWelcome(float screenW, float screenH, float scale, float mouseX, float mouseY, float progressOut) {
      long startTime = this.timeMillis2;
      float cardWidth = 340.0F * scale;
      float cardHeight = 310.0F * scale;
      float cardX = (screenW - cardWidth) / 2.0F;
      float cardY = (screenH - cardHeight) / 2.0F - (progressOut * 15.0F * scale);

      float animIn = UiTheme.computeFloat2(UiTheme.computeFloat3(startTime, 0L, 400L));
      Render2D.pushAlpha(animIn * (1.0F - progressOut));

//      Render2D.drawRect(cardX, cardY, cardWidth, cardHeight, 16.0F * scale, UiTheme.computeInt2(UiTheme.intVal7, UiTheme.intVal8, 0.15F));
//      Render2D.drawOutline(cardX, cardY, cardWidth, cardHeight, 16.0F * scale, 1.0F, UiTheme.intVal34);
      Render2D.popAlpha();

      float contentAlpha = UiTheme.computeFloat2(UiTheme.computeFloat3(startTime, 80L, 450L));
      Render2D.pushAlpha(contentAlpha * (1.0F - progressOut));

      Render2D.drawImageSized(
              "assets/greed/textures/gui/mainmenu/icons/logo.png",
              cardX + (cardWidth - 28.0F * scale) / 2.0F,
              cardY + 28.0F * scale,
              28.0F * scale,
              22.0F * scale,
              0.0F,
              UiTheme.intVal7
      );

      FontText.processData3(
              FontFace.SEMI,
              "Добро пожаловать в",
              cardX + (cardWidth - FontText.computeFloat(FontFace.SEMI, "Добро пожаловать в", 6.5F * scale)) / 2.0F,
              cardY + 62.0F * scale,
              6.5F * scale,
              UiTheme.intVal29
      );

      Render2D.drawImageSized(
              "assets/greed/textures/gui/mainmenu/icons/greed_serif.png",
              cardX + (cardWidth - 110.0F * scale) / 2.0F,
              cardY + 75.0F * scale,
              110.0F * scale,
              24.8F * scale,
              0.0F,
              UiTheme.intVal7
      );

      float descY = cardY + 115.0F * scale;
      for (String line : UiTheme.getList(
              FontFace.SEMI,
              "Всё готово для комфортной игры. Настройте клиент под себя, используйте любимые функции и откройте больше возможностей вместе с Greed.",
              6.5F * scale,
              280.0F * scale
      )) {
         FontText.processData3(
                 FontFace.SEMI,
                 line,
                 cardX + (cardWidth - FontText.computeFloat(FontFace.SEMI, line, 6.5F * scale)) / 2.0F,
                 descY,
                 6.5F * scale,
                 UiTheme.intVal29
         );
         descY += 10.0F * scale;
      }

      this.buttonX = cardX + (cardWidth - 260.0F * scale) / 2.0F;
      this.buttonY = cardY + cardHeight - 75.0F * scale;
      this.buttonWidth = 260.0F * scale;
      this.buttonHeight = 26.0F * scale;

      this.animatedValue.processData(
              this.timeMillis3 == 0L && UiTheme.matchesCriteria(mouseX, mouseY, this.buttonX, this.buttonY, this.buttonWidth, this.buttonHeight) ? 1.0 : 0.0
      );

      float animProgress = (float) this.animatedValue.computeDouble();
      Render2D.drawRect(
              this.buttonX,
              this.buttonY,
              this.buttonWidth,
              this.buttonHeight,
              8.0F * scale,
              UiTheme.computeInt2(UiTheme.intVal7, UiTheme.intVal8, animProgress * 0.8F)
      );
      Render2D.drawOutline(this.buttonX, this.buttonY, this.buttonWidth, this.buttonHeight, 8.0F * scale, 0.8F, UiTheme.intVal34);

      UiTheme.processData3(
              "assets/greed/textures/gui/mainmenu/icons/arrow.png",
              this.buttonX + this.buttonWidth / 2.0F + 65.0F * scale + animProgress * 2.0F * scale,
              this.buttonY + this.buttonHeight / 2.0F,
              7.0F * scale,
              7.0F * scale,
              UiTheme.intVal10
      );

      FontText.processData3(
              FontFace.SEMI_BOLD,
              "Перейти в главное меню",
              this.buttonX + (this.buttonWidth - FontText.computeFloat(FontFace.SEMI_BOLD, "Перейти в главное меню", 7.5F * scale)) / 2.0F - 4.0F * scale,
              this.buttonY + this.buttonHeight / 2.0F - 3.8F * scale,
              7.5F * scale,
              UiTheme.intVal10
      );

      Render2D.popAlpha();

      float chipAlpha = UiTheme.computeFloat2(UiTheme.computeFloat3(startTime, 150L, 450L));
      Render2D.pushAlpha(chipAlpha * (1.0F - progressOut));
      this.drawAccountPill(cardX + (cardWidth - 115.0F * scale) / 2.0F, cardY + cardHeight - 38.0F * scale, scale);
      Render2D.popAlpha();
   }
}