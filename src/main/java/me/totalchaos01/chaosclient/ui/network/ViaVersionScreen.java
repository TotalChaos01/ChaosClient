package me.totalchaos01.chaosclient.ui.network;

import me.totalchaos01.chaosclient.util.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class ViaVersionScreen extends Screen {

    private static final String[] PROTOCOLS = {
            "Auto (1.21.11)",
            "1.21.4",
            "1.20.6",
            "1.19.4",
            "1.8.x"
    };

    private static int selectedProtocolIndex = 0;

    private final Screen parent;

    public ViaVersionScreen(Screen parent) {
        super(Text.literal("ViaVersion"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height / 2;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Протокол: " + PROTOCOLS[selectedProtocolIndex]), button -> {
            selectedProtocolIndex = (selectedProtocolIndex + 1) % PROTOCOLS.length;
            this.clearAndInit();
        }).dimensions(cx - 110, y - 10, 220, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Назад"), button -> {
            this.client.setScreen(parent);
        }).dimensions(cx - 60, y + 20, 120, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        RenderUtil.blurBackground(context, this.width, this.height, 6);
        RenderUtil.roundedRectSimple(context, this.width / 2 - 160, this.height / 2 - 70, 320, 150, 10, 0xCC101018);
        RenderUtil.roundedRectOutline(context, this.width / 2 - 160, this.height / 2 - 70, 320, 150, 10, 1, 0x665CC9A7);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 52, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Текущий протокол: " + PROTOCOLS[selectedProtocolIndex]),
                this.width / 2, this.height / 2 - 30, 0xFFB8FFD8);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Настройка сохранена в рамках сессии клиента."),
                this.width / 2, this.height / 2 - 14, 0xFFB8C0D8);

        super.render(context, mouseX, mouseY, delta);
    }
}
