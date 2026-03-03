package me.totalchaos01.chaosclient.ui.network;

import me.totalchaos01.chaosclient.mixin.IMinecraftClientAccessor;
import me.totalchaos01.chaosclient.util.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.session.Session;
import net.minecraft.text.Text;

import java.util.Optional;
import java.util.UUID;

public class AltManagerScreen extends Screen {

    private final Screen parent;
    private TextFieldWidget usernameField;
    private String statusMessage = "";
    private int statusColor = 0xFFB8C0D8;

    public AltManagerScreen(Screen parent) {
        super(Text.literal("Alt Manager"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int panelY = this.height / 2 - 60;

        // Username text field
        usernameField = new TextFieldWidget(this.textRenderer, cx - 100, panelY + 40, 200, 20, Text.literal("Никнейм"));
        usernameField.setMaxLength(16);
        usernameField.setPlaceholder(Text.literal("Введите никнейм..."));
        String currentName = this.client.getSession() != null ? this.client.getSession().getUsername() : "";
        usernameField.setText(currentName);
        this.addDrawableChild(usernameField);

        // Login button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Войти (оффлайн)"), button -> {
            String username = usernameField.getText().trim();
            if (username.isEmpty() || username.length() < 3) {
                statusMessage = "§cНик должен быть от 3 символов!";
                statusColor = 0xFFFF5555;
                return;
            }
            try {
                Session newSession = new Session(
                        username,
                        UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes()),
                        "",
                        Optional.empty(),
                        Optional.empty()
                );
                ((IMinecraftClientAccessor) this.client).setSession(newSession);
                statusMessage = "§aУспешно! Ник: " + username;
                statusColor = 0xFF55FF55;
            } catch (Exception e) {
                statusMessage = "§cОшибка: " + e.getMessage();
                statusColor = 0xFFFF5555;
            }
        }).dimensions(cx - 100, panelY + 68, 200, 20).build());

        // Back button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Назад"), button -> {
            this.client.setScreen(parent);
        }).dimensions(cx - 60, panelY + 96, 120, 20).build());

        this.setFocused(usernameField);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        RenderUtil.blurBackground(context, this.width, this.height, 6);

        int panelX = this.width / 2 - 130;
        int panelY = this.height / 2 - 60;
        int panelW = 260;
        int panelH = 130;

        RenderUtil.roundedRectSimple(context, panelX, panelY, panelW, panelH, 12, 0xDD101018);
        RenderUtil.roundedRectOutline(context, panelX, panelY, panelW, panelH, 12, 1, 0x663A73FF);

        // Title
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, panelY + 8, 0xFFFFFFFF);

        // Current session info
        String current = "Текущий ник: " + (this.client.getSession() != null ? this.client.getSession().getUsername() : "?");
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(current),
                this.width / 2, panelY + 24, 0xFFB8C0D8);

        // Status message
        if (!statusMessage.isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(statusMessage),
                    this.width / 2, panelY + panelH + 6, statusColor);
        }

        super.render(context, mouseX, mouseY, delta);
    }
}
