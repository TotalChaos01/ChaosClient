package me.totalchaos01.chaosclient.event.events;

import me.totalchaos01.chaosclient.event.Event;
import net.minecraft.client.gui.DrawContext;

/**
 * Fired when the in-game HUD is rendered (2D overlay).
 */
public class EventRender2D extends Event {
    private final DrawContext drawContext;
    private final float tickDelta;

    public EventRender2D(DrawContext drawContext, float tickDelta) {
        this.drawContext = drawContext;
        this.tickDelta = tickDelta;
    }

    public DrawContext getDrawContext() {
        return drawContext;
    }

    public float getTickDelta() {
        return tickDelta;
    }
}

