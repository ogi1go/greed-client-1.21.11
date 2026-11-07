package greed.client.ui.panel;

import greed.client.render.AccountPreviewRenderer;
import greed.client.render.FontFace;
import greed.client.render.FontText;
import greed.client.render.Render2D;
import greed.client.storage.AccountEntry;
import greed.client.storage.AccountStore;
import greed.client.ui.AnimatedValue;
import greed.client.ui.UiTheme;
import greed.client.util.GuiScale;
import greed.client.util.NicknameGenerator;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

public final class AccountManagerPanel {
    private static final float ZOOM = 1.18F;
    private static final float W = 226.5F;
    private static final float ROW_W = 218.5F;
    private static final float ROW_H = 20.0F;
    private static final float ROW_PITCH = 24.0F;
    private static final float LIST_H = 92.0F;
    private static final float FIELD_W = 118.0F;
    private static final float BTN_H = 19.5F;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private AccountStore accountStore = AccountStore.getAccountStore();
    private AccountManagerPanel.TypeOption activeType = AccountManagerPanel.TypeOption.NONE;
    private String inputText = "";
    private boolean replaceTextOnInput;
    private NicknameGenerator nicknameGenerator = new NicknameGenerator();
    private HashMap<String, AnimatedValue> hoverAnimations = new HashMap<>();
    private float scrollOffset;
    private float scrollOffsetSmooth;
    private float maxScroll;
    private long lastFrameTimeMillis = System.nanoTime() / 1000000L;
    private long animationStartTimeMillis;
    private Runnable backAction = () -> {};
    private float listTop;
    private float listBottom;
    private float scale = 1.0F;
    private boolean continueAvatarVisible;
    private float continueAvatarX;
    private float continueY;
    private ArrayList<AccountManagerPanel.DataRecord2> hitRegions = new ArrayList<>();
    private ArrayList<AccountEntry> visibleAccounts = new ArrayList<>();

    public void renderElement(DrawContext drawContext, float x, float scale, float y, float mouseX, float mouseY) {
        float contentX = x + 51.5F * scale;

        this.scale = scale * ZOOM;
        float deltaSeconds = Math.min(0.05F, (float) (System.nanoTime() / 1000000L - this.lastFrameTimeMillis) / 1000.0F);
        this.lastFrameTimeMillis = System.nanoTime() / 1000000L;

        this.hitRegions.clear();
        this.renderTitle(contentX, y);
        this.renderInputRow(contentX, y, mouseX, mouseY);
        this.renderAccountList(drawContext, contentX, y, mouseX, mouseY, deltaSeconds);
        this.renderContinueButton(contentX, y, mouseX, mouseY);
        this.renderBackButton(contentX, y, mouseX, mouseY);
    }

    private float computeAppearProgress(int index) {
        return UiTheme.computeFloat2(UiTheme.computeFloat3(this.animationStartTimeMillis, 40L + index * 55L, 420L));
    }

    private void renderTitle(float x, float y) {
        float progress = this.computeAppearProgress(0);
        Render2D.pushAlpha(progress);
        String title = "Выберите существующий, или создайте новый аккаунт";
        float size = 7.5F * this.scale;
        float textX = x + (W * this.scale - FontText.computeFloat(FontFace.SEMI, title, size)) / 2.0F;
        FontText.processData3(FontFace.SEMI, title, textX, y + 150.0F * this.scale, size, UiTheme.intVal29);
        Render2D.popAlpha();
    }

    private void renderInputRow(float x, float y, float mouseX, float mouseY) {
        float progress = this.computeAppearProgress(1);
        Render2D.pushAlpha(progress);
        float rowX = x - (1.0F - progress) * 14.0F * this.scale;
        float rowY = y + 168.0F * this.scale;
        boolean fieldActive = this.activeType == AccountManagerPanel.TypeOption.INPUT;
        float focus = this.computeHoverProgress("fieldFocus", fieldActive);

        Render2D.drawRect(rowX, rowY, FIELD_W * this.scale, BTN_H * this.scale, 6.0F * this.scale, UiTheme.computeInt(UiTheme.intVal, 0.031F));
        Render2D.drawOutline(
                rowX,
                rowY,
                FIELD_W * this.scale,
                BTN_H * this.scale,
                6.0F * this.scale,
                0.5F,
                UiTheme.computeInt2(UiTheme.intVal33, UiTheme.computeInt(UiTheme.intVal34, 0.8F), focus)
        );

        if (fieldActive && this.replaceTextOnInput && !this.inputText.isEmpty()) {
            Render2D.drawRect(
                    rowX + 8.0F * this.scale,
                    rowY + 5.5F * this.scale,
                    FontText.computeFloat(FontFace.SEMI, this.inputText, 7.0F * this.scale) + 1.0F * this.scale,
                    9.0F * this.scale,
                    2.0F * this.scale,
                    UiTheme.computeInt(UiTheme.intVal7, 0.3F)
            );
        }

        String caret = fieldActive && System.currentTimeMillis() % 1000L < 500L ? "|" : "";
        String shown = this.inputText.isEmpty() && !fieldActive ? "Имя пользователя" : this.inputText + caret;
        FontText.processData3(
                FontFace.SEMI,
                shown,
                rowX + 8.0F * this.scale,
                rowY + 6.25F * this.scale,
                7.0F * this.scale,
                this.inputText.isEmpty() ? UiTheme.intVal30 : UiTheme.intVal
        );
        this.hitRegions.add(new AccountManagerPanel.DataRecord2(rowX, rowY, FIELD_W * this.scale, BTN_H * this.scale, false, () -> {
            this.activeType = AccountManagerPanel.TypeOption.INPUT;
            this.replaceTextOnInput = false;
        }));

        float diceX = rowX + (FIELD_W + 6.0F) * this.scale;
        float diceHover = this.computeHoverProgress(
                "dice", UiTheme.matchesCriteria(mouseX, mouseY, diceX, rowY, BTN_H * this.scale, BTN_H * this.scale)
        );
        Render2D.drawRect(
                diceX, rowY, BTN_H * this.scale, BTN_H * this.scale, 6.0F * this.scale, UiTheme.computeInt(UiTheme.intVal, 0.031F + 0.05F * diceHover)
        );
        Render2D.drawOutline(
                diceX, rowY, BTN_H * this.scale, BTN_H * this.scale, 6.0F * this.scale, 0.5F, UiTheme.computeInt(UiTheme.intVal, 0.078F + 0.09F * diceHover)
        );
        UiTheme.processData3(
                "assets/greed/textures/gui/mainmenu/icons/dice.png",
                diceX + (BTN_H * this.scale - 7.5F * this.scale) / 2.0F + 4f,
                rowY + BTN_H * this.scale / 2.0F,
                8.5F * this.scale,
                8.5F * this.scale,
                UiTheme.computeInt2(UiTheme.intVal28, UiTheme.intVal, diceHover)
        );
        this.hitRegions.add(new AccountManagerPanel.DataRecord2(diceX, rowY, BTN_H * this.scale, BTN_H * this.scale, false, () -> {
            this.inputText = this.nicknameGenerator.formatText11();
            this.activeType = AccountManagerPanel.TypeOption.INPUT;
            this.replaceTextOnInput = false;
        }));

        float addX = diceX + (BTN_H + 6.0F) * this.scale;
        float addW = (W - FIELD_W - BTN_H - 12.0F) * this.scale;
        float addHover = this.computeHoverProgress("add", UiTheme.matchesCriteria(mouseX, mouseY, addX, rowY, addW, BTN_H * this.scale));
        Render2D.drawRect(addX, rowY, addW, BTN_H * this.scale, 6.0F * this.scale, UiTheme.computeInt(UiTheme.intVal, 0.031F + 0.05F * addHover));
        Render2D.drawOutline(addX, rowY, addW, BTN_H * this.scale, 6.0F * this.scale, 0.5F, UiTheme.computeInt(UiTheme.intVal, 0.078F + 0.09F * addHover));

        String addText = "Добавить аккаунт";
        FontText.processData3(
                FontFace.SEMI,
                addText,
                addX + (addW - FontText.computeFloat(FontFace.SEMI, addText, 7.0F * this.scale)) / 2.0F,
                rowY + 6.25F * this.scale,
                7.0F * this.scale,
                UiTheme.computeInt2(UiTheme.intVal28, UiTheme.intVal, addHover)
        );
        this.hitRegions.add(new AccountManagerPanel.DataRecord2(addX, rowY, addW, BTN_H * this.scale, false, this::submitInput));
        Render2D.popAlpha();
    }

    private float rowOffset(AccountEntry accountEntry, int index) {
        float appearProgress = UiTheme.computeFloat2(UiTheme.computeFloat3(this.animationStartTimeMillis, 200L + Math.min(index, 8) * 45L, 380L));
        float fadeProgress = UiTheme.computeFloat2(Math.min(1.0F, (float) (System.currentTimeMillis() - accountEntry.computeLong()) / 260.0F));
        return (1.0F - appearProgress) * -8.0F * this.scale + (1.0F - fadeProgress) * -8.0F * this.scale;
    }

    private void renderAccountList(DrawContext drawContext, float x, float y, float mouseX, float mouseY, float deltaSeconds) {
        float progress = this.computeAppearProgress(2);
        Render2D.pushAlpha(progress);
        float listX = x - (1.0F - progress) * 14.0F * this.scale;
        this.listTop = y + 196.0F * this.scale;
        this.listBottom = this.listTop + LIST_H * this.scale;

        ArrayList<AccountEntry> accounts = this.accountStore.getArrayList2("");
        this.visibleAccounts = accounts;
        float contentHeight = accounts.size() * ROW_PITCH * this.scale;
        this.maxScroll = Math.max(0.0F, contentHeight - LIST_H * this.scale);
        this.scrollOffset = clamp(this.scrollOffset, 0.0F, this.maxScroll);
        this.scrollOffsetSmooth = smoothTowards(this.scrollOffsetSmooth, this.scrollOffset, deltaSeconds);

        Render2D.beginScissor(drawContext, listX, this.listTop, W * this.scale, LIST_H * this.scale);
        if (accounts.isEmpty()) {
            String emptyText = "Аккаунтов пока нет";
            FontText.processData3(
                    FontFace.SEMI,
                    emptyText,
                    listX + (W * this.scale - FontText.computeFloat(FontFace.SEMI, emptyText, 7.0F * this.scale)) / 2.0F,
                    this.listTop + 40.0F * this.scale,
                    7.0F * this.scale,
                    UiTheme.intVal30
            );
        }

        float rowY = this.listTop - this.roundToPixelGrid(this.scrollOffsetSmooth);
        int index = 0;
        for (AccountEntry accountEntry : accounts) {
            if (rowY + ROW_H * this.scale >= this.listTop - 6.0F * this.scale && rowY <= this.listBottom + 6.0F * this.scale) {
                this.renderAccountRow(accountEntry, listX, rowY, index, mouseX, mouseY);
            }
            rowY += ROW_PITCH * this.scale;
            index++;
        }

        Render2D.endScissor(drawContext);
        if (this.maxScroll > 0.5F) {
            float thumbHeight = Math.max(16.0F, (LIST_H * this.scale) * ((LIST_H * this.scale) / contentHeight));
            Render2D.drawRect(
                    listX + 225.5F * this.scale, this.listTop, 1.0F * this.scale, LIST_H * this.scale, 0.5F * this.scale, UiTheme.computeInt(UiTheme.intVal, 0.05F)
            );
            Render2D.drawRect(
                    listX + 225.5F * this.scale,
                    this.listTop + this.scrollOffsetSmooth / this.maxScroll * (LIST_H * this.scale - thumbHeight),
                    1.0F * this.scale,
                    thumbHeight,
                    0.5F * this.scale,
                    UiTheme.intVal31
            );
        }
        Render2D.popAlpha();
    }

    private void renderAccountRow(AccountEntry accountEntry, float rowX, float rowY, int index, float mouseX, float mouseY) {
        float appearProgress = UiTheme.computeFloat2(UiTheme.computeFloat3(this.animationStartTimeMillis, 200L + Math.min(index, 8) * 45L, 380L));
        float fadeProgress = UiTheme.computeFloat2(Math.min(1.0F, (float) (System.currentTimeMillis() - accountEntry.computeLong()) / 260.0F));
        Render2D.pushAlpha(appearProgress * fadeProgress);

        float itemX = rowX + this.rowOffset(accountEntry, index);
        String name = accountEntry.formatText4();
        float activeProgress = this.computeHoverProgress("row_active_" + name, this.accountStore.matchesCriteria(accountEntry));
        boolean rowHovered = UiTheme.matchesCriteria(mouseX, mouseY, itemX, rowY, ROW_W * this.scale, ROW_H * this.scale)
                && mouseY >= this.listTop
                && mouseY <= this.listBottom;
        float hoverProgress = this.computeHoverProgress("row_" + name, activeProgress < 0.5F && rowHovered);

        Render2D.drawRect(
                itemX,
                rowY,
                ROW_W * this.scale,
                ROW_H * this.scale,
                6.0F * this.scale,
                UiTheme.computeInt2(UiTheme.computeInt(UiTheme.intVal, 0.031F + 0.045F * hoverProgress), UiTheme.intVal7, activeProgress)
        );
        Render2D.drawOutline(
                itemX,
                rowY,
                ROW_W * this.scale,
                ROW_H * this.scale,
                6.0F * this.scale,
                0.5F,
                UiTheme.computeInt2(UiTheme.computeInt(UiTheme.intVal, 0.078F + 0.09F * hoverProgress), UiTheme.intVal34, activeProgress)
        );

        FontText.processData3(
                activeProgress > 0.5F ? FontFace.SEMI_BOLD : FontFace.SEMI,
                name,
                itemX + 24.0F * this.scale,
                rowY + 4.0F * this.scale,
                7.0F * this.scale,
                UiTheme.computeInt2(UiTheme.computeInt2(UiTheme.intVal28, UiTheme.intVal, hoverProgress), UiTheme.intVal, activeProgress)
        );

        String created = "Создан " + Instant.ofEpochMilli(accountEntry.computeLong()).atZone(ZoneId.systemDefault()).format(DATE_FORMAT);
        FontText.processData3(
                FontFace.SEMI, created, itemX + 24.0F * this.scale, rowY + 11.5F * this.scale, 5.5F * this.scale, UiTheme.intVal30
        );

        this.hitRegions.add(
                new AccountManagerPanel.DataRecord2(itemX, rowY, ROW_W * this.scale, ROW_H * this.scale, true, () -> this.accountStore.processData5(accountEntry))
        );

        float deleteX = itemX + 140.0F * this.scale;
        float deleteY = rowY + 4.5F * this.scale;
        float deleteHover = this.computeHoverProgress(
                "row_delete_" + name, rowHovered && UiTheme.matchesCriteria(mouseX, mouseY, deleteX, deleteY, 11.0F * this.scale, 11.0F * this.scale)
        );
        UiTheme.processData3(
                "assets/greed/textures/gui/mainmenu/icons/delete.png",
                deleteX + 1.75F * this.scale,
                rowY + ROW_H * this.scale / 2.0F,
                7.5F * this.scale,
                7.0F * this.scale,
                UiTheme.computeInt2(UiTheme.computeInt(UiTheme.intVal10, 0.72F), UiTheme.intVal35, deleteHover)
        );
        this.hitRegions.add(
                new AccountManagerPanel.DataRecord2(
                        deleteX, deleteY, 11.0F * this.scale, 11.0F * this.scale, true, () -> this.accountStore.processData3(accountEntry)
                )
        );

        float selectW = 60.0F * this.scale;
        float selectH = 14.0F * this.scale;
        float selectX = itemX + (ROW_W - 64.0F) * this.scale;
        float selectY = rowY + (ROW_H * this.scale - selectH) / 2.0F;

        float selectHover = this.computeHoverProgress(
                "row_select_" + name, rowHovered && UiTheme.matchesCriteria(mouseX, mouseY, selectX, selectY, selectW, selectH)
        );
        Render2D.drawRect(
                selectX,
                selectY,
                selectW,
                selectH,
                5.0F * this.scale,
                UiTheme.computeInt2(UiTheme.computeInt(UiTheme.intVal, 0.05F + 0.06F * selectHover), UiTheme.intVal7, activeProgress)
        );

        String selectText = "Выбрать аккаунт";
        FontText.processData3(
                FontFace.SEMI,
                selectText,
                selectX + (selectW - FontText.computeFloat(FontFace.SEMI, selectText, 6.0F * this.scale)) / 2.0F,
                selectY + 4.25F * this.scale,
                6.0F * this.scale,
                UiTheme.computeInt2(UiTheme.intVal28, UiTheme.intVal, Math.max(selectHover, activeProgress))
        );
        this.hitRegions.add(new AccountManagerPanel.DataRecord2(selectX, selectY, selectW, selectH, true, () -> this.accountStore.processData5(accountEntry)));
        Render2D.popAlpha();
    }

    private void renderContinueButton(float x, float y, float mouseX, float mouseY) {
        float progress = this.computeAppearProgress(3);
        Render2D.pushAlpha(progress);
        float buttonX = x - (1.0F - progress) * 14.0F * this.scale;
        float buttonY = y + 296.0F * this.scale;
        float buttonW = W * this.scale;
        float buttonH = BTN_H * this.scale;

        AccountEntry accountEntry = this.accountStore.getAccountEntry();
        boolean enabled = accountEntry != null;
        float hover = this.computeHoverProgress("continue", enabled && UiTheme.matchesCriteria(mouseX, mouseY, buttonX, buttonY, buttonW, buttonH));

        if (enabled) {
            Render2D.drawRect(buttonX, buttonY, buttonW, buttonH, 6.0F * this.scale, UiTheme.computeInt2(UiTheme.intVal7, UiTheme.computeInt(UiTheme.intVal7, 0.9F), hover));
            Render2D.drawOutline(buttonX, buttonY, buttonW, buttonH, 6.0F * this.scale, 0.5F, UiTheme.intVal34);
        } else {
            Render2D.drawRect(buttonX, buttonY, buttonW, buttonH, 6.0F * this.scale, UiTheme.computeInt(UiTheme.intVal, 0.031F));
            Render2D.drawOutline(buttonX, buttonY, buttonW, buttonH, 6.0F * this.scale, 0.5F, UiTheme.intVal33);
        }

        String text = enabled ? "Продолжить как " + accountEntry.formatText4() : "Выберите аккаунт";
        float textW = FontText.computeFloat(FontFace.SEMI, text, 7.0F * this.scale);
        float avatarW = enabled ? 14.0F * this.scale : 0.0F;
        float textX = buttonX + (buttonW - textW - avatarW) / 2.0F;

        FontText.processData3(FontFace.SEMI, text, textX, buttonY + 6.25F * this.scale, 7.0F * this.scale, enabled ? UiTheme.intVal : UiTheme.intVal30);

        this.continueAvatarVisible = enabled;
        this.continueAvatarX = textX + textW + 4.0F * this.scale;
        this.continueY = buttonY;

        if (enabled) {
            this.hitRegions.add(new AccountManagerPanel.DataRecord2(buttonX, buttonY, buttonW, buttonH, false, this.backAction));
        }
        Render2D.popAlpha();
    }

    private void renderBackButton(float x, float y, float mouseX, float mouseY) {
        float progress = this.computeAppearProgress(4);
        Render2D.pushAlpha(progress);
        float buttonX = x - (1.0F - progress) * 14.0F * this.scale;
        float buttonY = y + 320.0F * this.scale;
        float buttonW = W * this.scale;
        float buttonH = BTN_H * this.scale;

        float hover = this.computeHoverProgress("back", UiTheme.matchesCriteria(mouseX, mouseY, buttonX, buttonY, buttonW, buttonH));
        Render2D.drawRect(buttonX, buttonY, buttonW, buttonH, 6.0F * this.scale, UiTheme.computeInt(UiTheme.intVal, 0.031F + 0.05F * hover));
        Render2D.drawOutline(buttonX, buttonY, buttonW, buttonH, 6.0F * this.scale, 0.5F, UiTheme.computeInt(UiTheme.intVal, 0.078F + 0.09F * hover));

        String text = "Вернуться в меню";
        FontText.processData3(
                FontFace.SEMI,
                text,
                buttonX + (buttonW - FontText.computeFloat(FontFace.SEMI, text, 7.0F * this.scale)) / 2.0F,
                buttonY + 6.25F * this.scale,
                7.0F * this.scale,
                UiTheme.computeInt2(UiTheme.intVal28, UiTheme.intVal, hover)
        );
        this.hitRegions.add(new AccountManagerPanel.DataRecord2(buttonX, buttonY, buttonW, buttonH, false, this.backAction));
        Render2D.popAlpha();
    }

    private float computeHoverProgress(String key, boolean active) {
        AnimatedValue animatedValue = this.hoverAnimations.computeIfAbsent(key, absentKey -> new AnimatedValue(140L));
        animatedValue.processData(active ? 1.0 : 0.0);
        return (float) animatedValue.computeDouble();
    }

    private float roundToPixelGrid(float value) {
        float pixelScale = this.scale * GuiScale.computeFloat() / GuiScale.computeInt();
        return pixelScale > 1.0E-4F ? Math.round(value * pixelScale) / pixelScale : value;
    }

    public boolean matchesCriteria(float mouseX, float mouseY) {
        boolean wasActive = this.activeType != AccountManagerPanel.TypeOption.NONE;
        this.activeType = AccountManagerPanel.TypeOption.NONE;
        this.replaceTextOnInput = false;

        for (int index = this.hitRegions.size() - 1; index >= 0; index--) {
            AccountManagerPanel.DataRecord2 dataRecord = this.hitRegions.get(index);
            if ((!dataRecord.clipped() || !(mouseY < this.listTop) && !(mouseY > this.listBottom))
                    && UiTheme.matchesCriteria(mouseX, mouseY, dataRecord.x(), dataRecord.y(), dataRecord.w(), dataRecord.h())) {
                dataRecord.action().run();
                return true;
            }
        }

        return wasActive;
    }

    public void updateState() {
        this.activeType = AccountManagerPanel.TypeOption.NONE;
        this.replaceTextOnInput = false;
    }

    public boolean matchesCriteria2(float deltaX, float mouseY, float scrollDelta) {
        if (mouseY >= this.listTop - 30.0F * this.scale && mouseY <= this.listBottom + 10.0F * this.scale) {
            this.scrollOffset = clamp(this.scrollOffset - scrollDelta * ROW_PITCH, 0.0F, this.maxScroll);
            return true;
        }
        return false;
    }

    private static float smoothTowards(float current, float target, float deltaSeconds) {
        current += (target - current) * (1.0F - (float) Math.exp(-deltaSeconds * 22.0F));
        return Math.abs(target - current) < 0.1F ? target : current;
    }

    public boolean matchesCriteria3(int keyCode) {
        if (this.activeType == AccountManagerPanel.TypeOption.NONE) {
            return false;
        }

        long window = MinecraftClient.getInstance().getWindow().getHandle();
        if (GLFW.glfwGetKey(window, 341) == 1 || GLFW.glfwGetKey(window, 345) == 1) {
            if (keyCode == 65) {
                this.replaceTextOnInput = true;
                return true;
            }

            if (keyCode == 67) {
                MinecraftClient.getInstance().keyboard.setClipboard(this.inputText);
                return true;
            }

            if (keyCode == 86) {
                String pastedText = MinecraftClient.getInstance().keyboard.getClipboard().replaceAll("[\\r\\n\\t]", "");
                if (this.replaceTextOnInput) {
                    this.inputText = "";
                    this.replaceTextOnInput = false;
                }
                String combined = this.inputText + pastedText;
                this.inputText = combined.length() > 16 ? combined.substring(0, 16) : combined;
                return true;
            }
        }

        if (keyCode == 259) {
            if (this.replaceTextOnInput) {
                this.inputText = "";
                this.replaceTextOnInput = false;
            } else if (!this.inputText.isEmpty()) {
                this.inputText = this.inputText.substring(0, this.inputText.length() - 1);
            }
            return true;
        }

        if (keyCode == 257 || keyCode == 335) {
            this.submitInput();
            return true;
        }

        if (keyCode == 256) {
            this.activeType = AccountManagerPanel.TypeOption.NONE;
            this.replaceTextOnInput = false;
            return true;
        }

        return false;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public void processData2(Runnable runnable) {
        this.animationStartTimeMillis = System.currentTimeMillis();
        this.backAction = runnable;
        this.activeType = AccountManagerPanel.TypeOption.NONE;
        this.replaceTextOnInput = false;
        this.inputText = "";
        this.scrollOffset = 0.0F;
        this.scrollOffsetSmooth = 0.0F;
    }

    private void submitInput() {
        if (!this.inputText.isBlank()) {
            this.accountStore.processData5(this.accountStore.getAccountEntry3(this.inputText.trim()));
        }
        this.inputText = "";
        this.replaceTextOnInput = false;
        this.activeType = AccountManagerPanel.TypeOption.NONE;
    }

    public boolean matchesCriteria4(int codePoint) {
        if (codePoint >= 32 && codePoint <= 65535 && this.activeType != AccountManagerPanel.TypeOption.NONE) {
            if (this.replaceTextOnInput) {
                this.inputText = "";
                this.replaceTextOnInput = false;
            }
            if (this.inputText.length() < 16) {
                this.inputText = this.inputText + (char) codePoint;
            }
            return true;
        }
        return false;
    }

    private void drawAvatar(DrawContext drawContext, AccountEntry accountEntry, float left, float top, float s, float minY, float maxY, float progress) {
        float previewScale = GuiScale.computeInt() / GuiScale.computeFloat();
        float clipTop = Math.max(top + 1.5F * s, minY);
        float clipBottom = Math.min(top + 15.0F * s, maxY);

        if (clipBottom - clipTop <= 2.0F * s) {
            return;
        }

        drawContext.enableScissor(
                GuiScale.computeInt2(left + 3.5F * s),
                GuiScale.computeInt2(clipTop),
                GuiScale.computeInt2(left + 19.5F * s),
                GuiScale.computeInt2(clipBottom)
        );

        try {
            AccountPreviewRenderer.renderElement(drawContext, accountEntry, previewScale, left + 11.5F * s, top + 16.5F * s, 19.0F * s * progress);
        } catch (RuntimeException exception) {
        }

        drawContext.disableScissor();
    }

    public void renderElement3(DrawContext drawContext, float x, float scale, float y) {
        float contentX = x + 51.5F * scale;

        scale = scale * ZOOM;
        float progress = this.computeAppearProgress(2);
        if (!(progress < 0.05F)) {
            float listX = contentX - (1.0F - progress) * 14.0F * scale;
            float rowY = this.listTop - this.roundToPixelGrid(this.scrollOffsetSmooth);
            int index = 0;

            for (AccountEntry accountEntry : this.visibleAccounts) {
                if (rowY + ROW_H * scale >= this.listTop - 2.0F * scale && rowY <= this.listBottom + 2.0F * scale) {
                    float itemX = listX + this.rowOffset(accountEntry, index);
                    this.drawAvatar(drawContext, accountEntry, itemX, rowY + 1.75F * scale, scale, this.listTop, this.listBottom, progress);
                }
                rowY += ROW_PITCH * scale;
                index++;
            }
        }

        float continueProgress = this.computeAppearProgress(3);
        if (this.continueAvatarVisible && !(continueProgress < 0.05F)) {
            AccountEntry selected = this.accountStore.getAccountEntry();
            if (selected != null) {
                float s = scale * 0.62F;
                float left = this.continueAvatarX - 3.5F * s;
                float top = this.continueY + BTN_H * scale / 2.0F - 8.25F * s;

                this.drawAvatar(drawContext, selected, left, top, s, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, continueProgress);
            }
        }
    }

    public enum TypeOption {
        NONE,
        INPUT;
    }

    private record DataRecord2(float x, float y, float w, float h, boolean clipped, Runnable action) {
    }
}