package me.totalchaos01.chaosclient.event;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Simple annotation-based event bus.
 * Subscribers register with @EventTarget annotated methods.
 */
public class EventBus {

    private final Map<Class<?>, List<EventSubscriber>> subscribers = new ConcurrentHashMap<>();

    /**
     * Register all @EventTarget methods of the given object.
     */
    public void register(Object listener) {
        for (Method method : listener.getClass().getDeclaredMethods()) {
            if (!method.isAnnotationPresent(EventTarget.class)) continue;
            if (method.getParameterCount() != 1) continue;

            Class<?> eventType = method.getParameterTypes()[0];
            EventTarget annotation = method.getAnnotation(EventTarget.class);
            method.setAccessible(true);

            subscribers
                    .computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                    .add(new EventSubscriber(listener, method, annotation.priority()));

            // Sort by priority (higher first)
            subscribers.get(eventType).sort(Comparator.comparingInt(EventSubscriber::priority).reversed());
        }
    }

    /**
     * Unregister all event handlers for the given object.
     */
    public void unregister(Object listener) {
        subscribers.values().forEach(list ->
                list.removeIf(sub -> sub.instance() == listener)
        );
    }

    /**
     * Post an event to all registered handlers.
     */
    public <T extends Event> T post(T event) {
        List<EventSubscriber> subs = subscribers.get(event.getClass());
        if (subs == null) return event;

        for (EventSubscriber sub : subs) {
            try {
                sub.method().invoke(sub.instance(), event);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return event;
    }

    private record EventSubscriber(Object instance, Method method, int priority) {
    }
}

