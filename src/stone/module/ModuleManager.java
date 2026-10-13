package stone.module;

import stone.core.Color;
import stone.core.Config;
import stone.core.Log;
import stone.event.EventBus;
import stone.hook.Input;
import stone.ui.Client;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class ModuleManager {
    private static final List<Module> modules = new ArrayList<>();
    private static final Config config = new Config();

    private ModuleManager() {}

    public static void registerModules() {
        if (!modules.isEmpty()) {
            return;
        }
        for (Module module : ModuleList.create()) {
            add(module);
        }
        sort();
        Log.info("modules", "%d modules registered", modules.size());
    }

    public static void registerModule(Module module) {
        if (find(module.name) != null) {
            return;
        }
        applyConfig(module);
        add(module);
        sort();
    }

    public static void unregisterModule(Module module, Context context) {
        if (!modules.contains(module)) {
            return;
        }

        captureConfig(config, module);
        module.setEnabled(false, context);
        modules.remove(module);
        EventBus.unregister(module);
        Log.debug("modules", "unregistered %s", module.name);
    }

    private static void add(Module module) {
        Log.debug("modules", "registered %s (%s)", module.name, module.category.displayName);
        modules.add(module);
        EventBus.register(module);
    }

    private static void sort() {
        modules.sort(Comparator.<Module, Category>comparing(module -> module.category)
                .thenComparing(module -> module.name));
    }

    public static Config config() {
        return config;
    }

    public static List<Module> all() {
        return Collections.unmodifiableList(modules);
    }

    public static List<Module> byCategory(Category category) {
        List<Module> result = new ArrayList<>();
        for (Module module : modules) {
            if (module.category == category) {
                result.add(module);
            }
        }
        return result;
    }

    public static Module find(String name) {
        for (Module module : modules) {
            if (module.name.equals(name)) {
                return module;
            }
        }
        return null;
    }

    public static <T extends Module> T get(Class<T> type) {
        for (Module module : modules) {
            if (type.isInstance(module)) {
                return type.cast(module);
            }
        }
        return null;
    }

    private static String colourToHex(Color colour) {
        return String.format("#%08X", colour.packed());
    }

    private static Color colourFromHex(String text, Color fallback) {
        if (text.length() != 9 || text.charAt(0) != '#') {
            return fallback;
        }
        try {
            return Color.rgba(Integer.parseUnsignedInt(text.substring(1), 16));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static void applyConfig(Module module) {
        String section = module.name;
        if (!config.hasSection(section)) {
            return;
        }
        module.setKey(config.getInt(section, "key", module.key()));

        module.setEnabled(config.getBool(section, "enabled", module.enabled()), null);
        for (Setting setting : module.settings()) {
            String key = setting.name;
            switch (setting.type) {
                case BOOLEAN -> setting.setBool(config.getBool(section, key, setting.bool()));
                case NUMBER -> setting.setNumber(config.getNumber(section, key, setting.number()));
                case CHOICE -> {
                    int index = setting.choices().indexOf(config.get(section, key, setting.choiceName()));
                    if (index >= 0) {
                        setting.setChoice(index);
                    }
                }
                case KEY -> setting.setKey(config.getInt(section, key, setting.key()));
                case COLOUR -> setting.setColour(colourFromHex(
                        config.get(section, key, colourToHex(setting.colour())), setting.colour()));
            }
        }
    }

    private static void captureConfig(Config document, Module module) {
        String section = module.name;

        document.clearSection(section);
        document.setBool(section, "enabled", module.enabled());
        document.setInt(section, "key", module.key());
        for (Setting setting : module.settings()) {
            String key = setting.name;
            switch (setting.type) {
                case BOOLEAN -> document.setBool(section, key, setting.bool());
                case NUMBER -> document.setNumber(section, key, setting.number(), setting.decimals());
                case CHOICE -> document.set(section, key, setting.choiceName());
                case KEY -> document.setInt(section, key, setting.key());
                case COLOUR -> document.set(section, key, colourToHex(setting.colour()));
            }
        }
    }

    public static void loadConfig(Path path) {
        if (!config.load(path)) {
            return;
        }
        for (Module module : modules) {
            applyConfig(module);
        }
        Log.info("modules", "configuration applied");
    }

    public static void saveConfig(Path path) {
        for (Module module : modules) {
            captureConfig(config, module);
        }
        config.save(path);
    }

    public static void tick(Context context) {
        for (Module module : modules) {
            if (module.holdsKey() && module.key() != 0) {
                module.keyHeld(!context.state.screenOpen && Input.bindHeld(module.key()), context);
            }
            module.sync(context);
            if (module.enabled()) {
                module.onTick(context);
            }
        }
    }

    public static void update() {
        boolean menuOpen = Client.gui().visible();
        for (Module module : modules) {
            if (module.enabled()) {
                module.onUpdate();
            }
            if (menuOpen) {
                module.guiUpdate();
            }
        }
    }

    public static void render(Context context) {
        for (Module module : modules) {
            if (module.enabled() || module.animation() > 0.001f) {
                module.onRender(context);
            }
        }
    }

    public static void advanceAnimations(float deltaTime) {
        for (Module module : modules) {
            module.advanceAnimation(deltaTime);
        }
    }

    public static boolean handleKey(int key, Context context) {
        if (key == 0) {
            return false;
        }
        boolean consumed = false;
        for (Module module : modules) {
            if (module.key() == key) {
                if (!module.holdsKey()) {
                    module.toggle(context);
                }
                consumed = true;
            }
        }
        return consumed;
    }
}
