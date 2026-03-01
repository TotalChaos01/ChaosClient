package me.totalchaos01.chaosclient.event.events;

import me.totalchaos01.chaosclient.event.Event;

/**
 * Fired when a chat message is about to be sent.
 * Can be cancelled to prevent sending.
 */
public class EventChatSend extends Event {
    private String message;

    public EventChatSend(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

