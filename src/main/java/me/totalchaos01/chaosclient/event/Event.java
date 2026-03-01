package me.totalchaos01.chaosclient.event;

/**
 * Base class for all events in ChaosClient.
 */
public class Event {
    private boolean cancelled;

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}

