package me.totalchaos01.chaosclient.event.events;

import me.totalchaos01.chaosclient.event.Event;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Fired during world rendering (3D).
 */
public class EventRender3D extends Event {
    private final MatrixStack matrixStack;
    private final float tickDelta;

    public EventRender3D(MatrixStack matrixStack, float tickDelta) {
        this.matrixStack = matrixStack;
        this.tickDelta = tickDelta;
    }

    public MatrixStack getMatrixStack() {
        return matrixStack;
    }

    public float getTickDelta() {
        return tickDelta;
    }
}

