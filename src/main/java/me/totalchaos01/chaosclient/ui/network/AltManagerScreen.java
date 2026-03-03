package me.totalchaos01.chaosclient.ui.network;

import me.totalchaos01.chaosclient.util.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class AltManagerScreen extends Screen {

    private final Screen parent;

    public AltManagerScreen(Screen parent) {
        super(Text.literal("AltManager"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height / 2 + 20;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Назад"), button -> {
            this.client.setScreen(parent);
        }).dimensions(cx - 60, y, 120, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        RenderUtil.blurBackground(context, this.width, this.height, 6);
        RenderUtil.roundedRectSimple(context, this.width / 2 - 150, this.height / 2 - 70, 300, 140, 10, 0xCC101018);
        RenderUtil.roundedRectOutline(context, this.width / 2 - 150, this.height / 2 - 70, 300, 140, 10, 1, 0x663A73FF);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 52, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Логин берётся из официальной сессии Minecraft."),
                this.width / 2, this.height / 2 - 24, 0xFFB8C0D8);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Ник: " + (this.client.getSession() != null ? this.client.getSession().getUsername() : "Unknown")),
                this.width / 2, this.height / 2 - 8, 0xFFD0D8FF);

        super.render(context, mouseX, mouseY, delta);
    }
}
