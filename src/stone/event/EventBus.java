package stone.event;

import stone.core.Log;
import stone.module.Module;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class EventBus {
    private record Listener(Object owner, Method method, Subscribe.Priority priority) {}

    private static final Map<Class<?>, List<Listener>> byEvent = new HashMap<>();

    private static volatile Map<Class<?>, Listener[]> table = Map.of();

    private EventBus() {}

    public static synchronized void register(Object owner) {
        for (Method method : owner.getClass().getDeclaredMethods()) {
            Subscribe subscribe = method.getAnnotation(Subscribe.class);
            if (subscribe == null || method.getParameterCount() != 1
                    || !Event.class.isAssignableFrom(method.getParameterTypes()[0])) {
                continue;
            }
            method.setAccessible(true);
            byEvent.computeIfAbsent(method.getParameterTypes()[0], type -> new ArrayList<>())
                    .add(new Listener(owner, method, subscribe.priority()));
        }
        rebuild();
    }

    public static synchronized void unregister(Object owner) {
        for (List<Listener> listeners : byEvent.values()) {
            listeners.removeIf(listener -> listener.owner == owner);
        }
        rebuild();
    }

    private static void rebuild() {
        Map<Class<?>, Listener[]> next = new HashMap<>();
        byEvent.forEach((type, listeners) -> {
            if (!listeners.isEmpty()) {
                Listener[] sorted = listeners.toArray(new Listener[0]);
                Arrays.sort(sorted, Comparator.comparing(Listener::priority));
                next.put(type, sorted);
            }
        });
        table = Map.copyOf(next);
    }

    public static boolean hasListeners(Class<? extends Event> type) {
        return table.containsKey(type);
    }

    public static <T extends Event> T post(T event) {
        Listener[] listeners = table.get(event.getClass());
        if (listeners == null) {
            return event;
        }
        for (Listener listener : listeners) {
            if (listener.owner instanceof Module module && !module.enabled()) {
                continue;
            }
            try {
                listener.method.invoke(listener.owner, event);
            } catch (InvocationTargetException thrown) {
                Log.error("events", thrown.getCause(), "%s.%s failed on %s",
                        listener.owner.getClass().getSimpleName(), listener.method.getName(),
                        event.getClass().getSimpleName());
            } catch (IllegalAccessException thrown) {
                throw new IllegalStateException(thrown);
            }
        }
        return event;
    }
}
