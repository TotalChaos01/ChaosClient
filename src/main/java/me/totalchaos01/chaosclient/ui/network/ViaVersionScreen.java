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
        int panelY = this.height / 2 - 60;

        // Protocol selector button
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Протокол: " + PROTOCOLS[selectedProtocolIndex]),
                button -> {
                    selectedProtocolIndex = (selectedProtocolIndex + 1) % PROTOCOLS.length;
                    this.clearAndInit();
                }
        ).dimensions(cx - 110, panelY + 50, 220, 20).build());

        // Back button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Назад"), button -> {
            this.client.setScreen(parent);
        }).dimensions(cx - 60, panelY + 80, 120, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        RenderUtil.blurBackground(context, this.width, this.height, 6);

        int panelX = this.width / 2 - 140;
        int panelY = this.height / 2 - 60;
        int panelW = 280;
        int panelH = 120;

        RenderUtil.roundedRectSimple(context, panelX, panelY, panelW, panelH, 12, 0xDD101018);
        RenderUtil.roundedRectOutline(context, panelX, panelY, panelW, panelH, 12, 1, 0x665CC9A7);

        // Title
        context.drawCenteredTextWithShadow(this.textRenderer, this.title,
                this.width / 2, panelY + 10, 0xFFFFFFFF);

        // Current protocol info
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Выбран: " + PROTOCOLS[selectedProtocolIndex]),
                this.width / 2, panelY + 28, 0xFFB8FFD8);

        // Note
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("§7Настройка сохранена в рамках сессии"),
                this.width / 2, panelY + panelH + 6, 0xFF888899);

        super.render(context, mouseX, mouseY, delta);
    }
}
