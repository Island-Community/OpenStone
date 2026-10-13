package stone.boot;

import stone.core.Log;
import stone.hook.Hooks;
import stone.ui.Client;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.objectweb.asm.Opcodes;

public final class Boot {
    private static final String SCOPE = "boot";
    private static final String BRIDGE = Bridge.internalName();

    private static final int FAILURE_LIMIT = 10;
    private static final MethodType FAILED_TYPE =
            MethodType.methodType(void.class, String.class, Throwable.class);

    private static volatile ClassLoader gameLoader;
    private static volatile Map<String, List<Hook>> targets = Map.of();
    private static Class<?> bridge;
    private static final Map<String, Integer> failures = new ConcurrentHashMap<>();

    private Boot() {}

    public static boolean start(String directory, String arrivals, long epochNanos) {
        Path home = Path.of(directory);
        try {
            Log.open(home.resolve("stone.log"), epochNanos);
        } catch (IOException e) {

        }
        try {
            gameLoader = Boot.class.getClassLoader().getParent();
            Client.start(home, Path.of(arrivals));

            bridge = defineBridge();
            bind(true);
            Map<String, List<Hook>> byOwner = Hook.ALL.stream().collect(Collectors.groupingBy(Hook::owner));
            targets = byOwner;
            Native.setTargets(byOwner.keySet().toArray(new String[0]));
            Class<?>[] loaded = loadedTargets();
            Native.retransform(loaded);
            Log.info(SCOPE, "hooks installed; %d of %d classes patched now, the rest as they load",
                    loaded.length, byOwner.size());
            return true;
        } catch (Throwable thrown) {
            Log.error(SCOPE, thrown, "start failed");
            restore();
            Log.close();
            return false;
        }
    }

    public static void stop() {
        Log.info(SCOPE, "stopping");
        try {
            Client.stop();
        } catch (Throwable thrown) {
            Log.error(SCOPE, thrown, "client stop failed");
        }
        restore();
        Log.info(SCOPE, "unloaded");
        Log.close();
    }

    public static byte[] transform(ClassLoader loader, String name, byte[] bytes) {
        List<Hook> hooks = targets.get(name);
        if (loader != gameLoader || hooks == null) {
            return null;
        }
        try {
            byte[] patched = Patcher.apply(bytes, hooks, BRIDGE);
            Log.debug(SCOPE, "patched %s", name);
            return patched;
        } catch (Throwable thrown) {
            Log.error(SCOPE, thrown, "patching %s failed; left as is", name);
            return null;
        }
    }

    private static void restore() {
        try {
            if (bridge != null) {
                bind(false);
            }
            Native.setTargets(new String[0]);
            Class<?>[] patched = loadedTargets();
            targets = Map.of();
            Native.retransform(patched);
            Log.info(SCOPE, "restored %d classes", patched.length);
        } catch (Throwable thrown) {
            Log.error(SCOPE, thrown, "restoring the game classes failed");
        }
    }

    private static Class<?> defineBridge() {
        String binaryName = BRIDGE.replace('/', '.');
        for (Class<?> type : Native.loadedClasses()) {
            if (type.getClassLoader() == gameLoader && type.getName().equals(binaryName)) {
                Log.debug(SCOPE, "reusing %s", binaryName);
                return type;
            }
        }
        return Native.defineClass(gameLoader, BRIDGE, Bridge.generate(BRIDGE));
    }

    private static Class<?>[] loadedTargets() {
        List<Class<?>> found = new ArrayList<>();
        for (Class<?> type : Native.loadedClasses()) {
            if (type.getClassLoader() == gameLoader && targets.containsKey(type.getName().replace('.', '/'))) {
                found.add(type);
            }
        }
        return found.toArray(new Class<?>[0]);
    }

    private static void bind(boolean live) throws ReflectiveOperationException {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        if (live) {
            set(Bridge.FAILED, lookup.findStatic(Boot.class, "failed", FAILED_TYPE));
        }
        for (Hook hook : Hook.ALL) {
            set(Bridge.methodName(hook),
                    live ? lookup.findStatic(Hooks.class, hook.handler(), type(hook)) : idle(hook));
        }
        if (!live) {
            set(Bridge.FAILED, MethodHandles.empty(FAILED_TYPE));
        }
    }

    private static MethodType type(Hook hook) {
        return MethodType.fromMethodDescriptorString(hook.handlerDescriptor(), gameLoader);
    }

    private static MethodHandle idle(Hook hook) throws ReflectiveOperationException {
        Hook.At at = hook.at();
        if (at.kind() != Hook.At.Kind.REDIRECT) {
            return MethodHandles.empty(type(hook));
        }
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        Class<?> owner = Class.forName(at.owner().replace('/', '.'), false, gameLoader);
        MethodType member = MethodType.fromMethodDescriptorString(
                at.isField() ? "()" + at.descriptor() : at.descriptor(), gameLoader);
        return switch (at.opcode()) {
            case Opcodes.GETFIELD -> lookup.findGetter(owner, at.name(), member.returnType());
            case Opcodes.PUTFIELD -> lookup.findSetter(owner, at.name(), member.returnType());
            case Opcodes.GETSTATIC -> lookup.findStaticGetter(owner, at.name(), member.returnType());
            case Opcodes.PUTSTATIC -> lookup.findStaticSetter(owner, at.name(), member.returnType());
            case Opcodes.INVOKESTATIC -> lookup.findStatic(owner, at.name(), member);
            default -> lookup.findVirtual(owner, at.name(), member);
        };
    }

    private static void set(String field, MethodHandle handle) throws ReflectiveOperationException {
        bridge.getField(field).set(null, handle);
    }

    private static void failed(String handler, Throwable thrown) {
        int count = failures.merge(handler, 1, Integer::sum);
        if (count <= 3) {
            Log.error(SCOPE, thrown, "hook %s threw", handler);
        }
        if (count != FAILURE_LIMIT) {
            return;
        }
        for (Hook hook : Hook.ALL) {
            if (hook.handler().equals(handler)) {
                try {
                    set(Bridge.methodName(hook), idle(hook));
                } catch (ReflectiveOperationException e) {
                    Log.error(SCOPE, e, "could not disable hook %s", handler);
                }
            }
        }
        Log.error(SCOPE, "hook %s disabled after %d failures", handler, count);
    }
}
