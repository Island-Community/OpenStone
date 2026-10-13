package stone.ui;

import stone.boot.Native;
import stone.core.Color;
import stone.core.Log;
import stone.core.Maths;
import stone.core.Time;
import stone.event.EventBus;
import stone.game.FrameState;
import stone.game.Game;
import stone.game.PlayerRadar;
import stone.game.Projection;
import stone.game.Raycast;
import stone.game.RotationEngine;
import stone.hook.Input;
import stone.hook.MenuScreen;
import stone.module.Context;
import stone.module.ModuleManager;
import stone.module.client.ClickGuiModule;
import stone.module.client.RadarModule;
import stone.module.minigames.BedwarsSpawnTracker;
import stone.render.Backdrop;
import stone.render.Renderer;
import stone.render.StateGuard;
import stone.script.ScriptManager;
import stone.util.SkywarsGameState;
import stone.util.rotation.RotationHandler;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class Client {
    private static final String SCOPE = "client";
    private static final float TICK_SECONDS = 0.05f;
    private static final long STOP_TIMEOUT_SECONDS = 5;

    private static final Backdrop backdrop = new Backdrop();
    private static final Projection projection = new Projection();
    private static final StateGuard guard = new StateGuard();

    private static Path directory;
    private static Path configPath;
    private static ClickGui gui;
    private static Context lastContext;
    private static Hud hud;
    private static Radar radar;
    private static volatile boolean running;
    private static boolean disabled;
    private static boolean renderReady;
    private static boolean loggedFirstFrame;
    private static int faultGuard;
    private static double lastFrameTime;

    private Client() {}

    public static ClickGui gui() {
        return gui;
    }

    public static Hud hud() {
        return hud;
    }

    public static Radar radar() {
        return radar;
    }

    public static Backdrop backdrop() {
        return backdrop;
    }

    public static Path directory() {
        return directory;
    }

    public static void start(Path home, Path arrivals) {
        directory = home;
        configPath = home.resolve("stone.cfg");
        try {
            migrateLegacyData(arrivals, home);
            migrateLegacyData(home.resolveSibling("coldwtf"), home);
        } catch (IOException e) {
            Log.warn(SCOPE, "legacy data not migrated: %s", e.getMessage());
        }
        Log.info(SCOPE, "Stone starting in %s", home);

        Renderer.instance().prepare();
        ModuleManager.registerModules();

        EventBus.register(RotationHandler.getInstance());
        EventBus.register(new BedwarsSpawnTracker());
        EventBus.register(SkywarsGameState.YFaX());
        ModuleManager.loadConfig(configPath);

        gui = new ClickGui();
        hud = new Hud();
        radar = new Radar();

        gui.loadLayout(ModuleManager.config());
        hud.loadLayout(ModuleManager.config());

        ScriptManager.initialise(home);
        running = true;
    }

    public static void stop() throws InterruptedException, ExecutionException, TimeoutException {
        Game.mc().addScheduledTask(Client::teardown).get(STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    public static void requestShutdown() {
        Native.requestUnload();
    }

    private static void teardown() {
        running = false;

        MenuScreen.hide();

        gui.saveLayout(ModuleManager.config());
        hud.saveLayout(ModuleManager.config());
        ModuleManager.saveConfig(configPath);

        ScriptManager.shutdown();
        if (renderReady) {
            Renderer.instance().destroy();
            backdrop.destroy();
            renderReady = false;
        }
        Log.info(SCOPE, "Stone unloaded");
    }

    public static void tick() {

        if (!running || disabled || Game.mc().isGamePaused()) {
            return;
        }
        FrameState state = Game.capture(1.0f);
        RotationEngine.tick(state);
        if (!state.player.valid) {
            PlayerRadar.clear();
            return;
        }
        float time = (float) Time.nowSeconds();
        Context context = context(state, TICK_SECONDS, time);

        Raycast.resetBlockCache();
        ModuleManager.tick(context);

        PlayerRadar.update(state, time);
        ScriptManager.tick(context);
    }

    public static void frame() {
        if (!running || disabled) {
            return;
        }
        try {
            renderFrame();
            faultGuard = 0;
        } catch (RuntimeException thrown) {
            recover(thrown);
        }
    }

    private static void recover(RuntimeException thrown) {
        Log.error(SCOPE, thrown, "frame failed");
        if (faultGuard >= 2) {
            disable("repeated frame faults");
            return;
        }
        ++faultGuard;
        PlayerRadar.clear();
        Renderer.instance().destroy();
        backdrop.destroy();
        renderReady = false;
        Log.warn(SCOPE, "recovered from a frame fault; rebuilding resources");
    }

    private static void disable(String reason) {
        disabled = true;
        MenuScreen.hide();
        Log.error(SCOPE, "%s; overlay disabled", reason);
    }

    private static boolean createRenderResources() {
        if (!Renderer.instance().createResources()) {
            return false;
        }
        if (!backdrop.createResources()) {
            Log.warn(SCOPE, "backdrop blur unavailable; panels fall back to solid fills");
        }
        return true;
    }

    private static void renderFrame() {
        Renderer gfx = Renderer.instance();
        if (!renderReady) {
            if (!createRenderResources()) {
                disable("renderer setup failed");
                return;
            }
            gui.build();
            renderReady = true;
            lastFrameTime = Time.nowSeconds();
        } else if (!gfx.resourcesValid()) {

            Log.warn(SCOPE, "GL resources vanished (world switch?); rebuilding");
            gfx.destroy();
            backdrop.destroy();
            if (!createRenderResources()) {
                disable("renderer rebuild failed");
                return;
            }
        }

        double now = Time.nowSeconds();
        float deltaTime = Maths.clamp((float) (now - lastFrameTime), 0.0001f, 0.25f);
        lastFrameTime = now;
        float time = (float) now;

        FrameState state = Game.capture(Game.mc().timer.renderPartialTicks);
        if (!loggedFirstFrame) {
            loggedFirstFrame = true;
            Log.info(SCOPE, "first frame: %dx%d framebuffer, %d fps", state.windowWidth, state.windowHeight,
                    state.fps);
        }
        projection.update(state, state.windowWidth, state.windowHeight);
        Context context = context(state, deltaTime, time);

        ScriptManager.beginFrame(context);
        Input.poll();
        dispatchInput(context);
        ModuleManager.advanceAnimations(deltaTime);

        guard.capture();
        try {
            if (gui.visible() && gui.blurEnabled()) {
                backdrop.capture(state.windowWidth, state.windowHeight, gui.blurStrength(), Color.rgb(0x0A0D12, 0.35f));
            }
            gfx.beginFrame(state.windowWidth, state.windowHeight, time);
            ModuleManager.render(context);
            ScriptManager.render();
            hud.render(context);
            ScriptManager.renderHud(context, gui.interactive());
            gui.render(context);
            radar.render(context);
            gfx.endFrame();
        } finally {
            guard.restore();
        }
        ScriptManager.endFrame();
    }

    public static Context context() {
        return lastContext;
    }

    private static Context context(FrameState state, float deltaTime, float time) {
        Context context = new Context();
        context.state = state;
        context.projection = projection;
        context.deltaTime = deltaTime;
        context.time = time;
        lastContext = context;
        return context;
    }

    private static void dispatchInput(Context context) {
        ClickGuiModule guiModule = ModuleManager.get(ClickGuiModule.class);
        RadarModule radarModule = ModuleManager.get(RadarModule.class);
        int guiKey = guiModule.key();
        int radarKey = radarModule.key();

        boolean overlayOwnsKeys = gui.interactive() || radar.interactive();
        hud.setMenuOpen(gui.interactive());

        for (Input.Event event : Input.drain()) {
            int code = event.code();
            if (event.kind() == Input.Event.Kind.KEY && event.action() == Input.PRESS && code != 0) {
                if (!overlayOwnsKeys) {
                    if (!context.state.screenOpen) {
                        ModuleManager.handleKey(code, context);
                    }
                } else if (gui.interactive() && !gui.capturingKey() && code == guiKey) {
                    guiModule.setEnabled(false, context);
                } else if (radar.interactive() && code == radarKey) {
                    radarModule.setEnabled(false, context);
                } else if (radar.interactive() && code == guiKey) {

                    guiModule.setEnabled(true, context);
                } else if (gui.interactive() && !gui.capturingKey() && code == radarKey) {
                    radarModule.setEnabled(true, context);
                }
            }
            gui.handleEvent(event, context);
            radar.handleEvent(event);
            hud.handleEvent(event, context);

            if (gui.interactive()) {
                float x = (float) event.x();
                float y = (float) event.y();
                if (event.kind() == Input.Event.Kind.MOUSE_BUTTON && code == Input.MOUSE_LEFT) {
                    if (event.action() == Input.PRESS) {
                        if (!gui.contains(x, y)) {
                            ScriptManager.hudPress(x, y);
                        }
                    } else {
                        ScriptManager.hudRelease();
                    }
                } else if (event.kind() == Input.Event.Kind.MOUSE_MOVE) {
                    ScriptManager.hudDrag(x, y);
                }
                continue;
            }
            switch (event.kind()) {
                case KEY -> {
                    if (event.action() != Input.REPEAT) {
                        ScriptManager.key(code, event.action() == Input.PRESS, event.modifiers());
                    }
                }
                case CHAR -> ScriptManager.charTyped(code);
                case MOUSE_BUTTON -> ScriptManager.mouse(code, event.action() == Input.PRESS, event.x(), event.y());
                case MOUSE_MOVE -> ScriptManager.mouseMove(event.x(), event.y());
                case SCROLL -> ScriptManager.scroll(event.x(), event.y());
            }
        }
    }

    private static void migrateLegacyData(Path from, Path to) throws IOException {
        if (!Files.isDirectory(from)) {
            return;
        }
        Path scripts = to.resolve("scripts");
        Files.createDirectories(scripts);
        copyIfMissing(from.resolve("coldwtf.cfg"), to.resolve("stone.cfg"));
        copyIfMissing(from.resolve("coldwtf.scripts.cfg"), to.resolve("stone.scripts.cfg"));
        try (DirectoryStream<Path> configs = Files.newDirectoryStream(from, "coldwtf-*.cfg")) {
            for (Path config : configs) {
                String name = config.getFileName().toString();
                copyIfMissing(config, to.resolve("stone" + name.substring("coldwtf".length())));
            }
        }
        Path legacyScripts = from.resolve("scripts");
        if (!Files.isDirectory(legacyScripts)) {
            return;
        }
        try (DirectoryStream<Path> files = Files.newDirectoryStream(legacyScripts, "*.lua")) {
            for (Path file : files) {
                copyIfMissing(file, scripts.resolve(file.getFileName()));
            }
        }
    }

    private static void copyIfMissing(Path from, Path to) throws IOException {
        if (Files.exists(from) && !Files.exists(to)) {
            Files.copy(from, to);
        }
    }
}
