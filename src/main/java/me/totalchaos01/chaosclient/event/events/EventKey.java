package me.totalchaos01.chaosclient.event.events;

import me.totalchaos01.chaosclient.event.Event;

/**
 * Fired when a key is pressed.
 */
public class EventKey extends Event {
    private final int key;
    private final int scanCode;

    public EventKey(int key, int scanCode) {
        this.key = key;
        this.scanCode = scanCode;
    }

    public int getKey() {
        return key;
    }

    public int getScanCode() {
        return scanCode;
    }
}

