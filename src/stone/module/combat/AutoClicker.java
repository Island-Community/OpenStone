package stone.module.combat;

import stone.event.GameLoopEvent;
import stone.event.Subscribe;
import stone.event.TickStartEvent;
import stone.module.Category;
import stone.module.Context;
import stone.module.Module;
import stone.module.ModuleManager;
import stone.module.Setting;
import stone.module.Subcategory;
import stone.module.player.InventoryManager;
import stone.ui.Client;
import stone.util.ClientUtils;
import stone.util.InputHookManager;
import java.util.SplittableRandom;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.input.Mouse;

public class AutoClicker extends Module {
    private static final double INVENTORY_INTERVAL_MS = 1000.0 / 16.0;
    private static final int ROLLING_BUFFER_SIZE = 32;

    private final Setting minCps = addNumber("Min CPS", "Lowest clicks per second", 14, 1, 20, 1);
    private final Setting maxCps = addNumber("Max CPS", "Highest clicks per second", 16, 1, 20, 1);
    private final Setting randomization = addBoolean("Randomization", "Human timing model instead of a uniform range", true);
    private final Setting randomizationPercent = addNumber("Randomization (%)", "How much of the human model applies",
            100, 0, 100, 0).visibleWhen(randomization::bool);
    private final Setting stable = addBoolean("Stable CPS", "Click at one fixed rate", false);
    private final Setting stableCps = addNumber("CPS (stable)", "Fixed clicks per second", 17, 1, 25, 0)
            .visibleWhen(stable::bool);
    private final Setting weaponOnly = addBoolean("Weapon Only", "Only click holding a sword or an axe", false);
    private final Setting breakBlocks = addBoolean("Break Blocks", "Stop clicking while looking at a block", false);
    private final Setting inventory = addBoolean("Inventory", "Shift and hold left in a container to quick-move", true);

    private final SplittableRandom random = new SplittableRandom();
    private double nextClickAt = -1;
    private boolean breaking;
    private GuiContainer inventoryGui;
    private Slot inventorySlot;
    private double nextInventoryClickAt = -1;

    private float tempo;
    private float drift;
    private float rhythmPhase;
    private float prevMotorDelay;
    private float fatigue;
    private float fatigueTarget;
    private int fatigueClicksLeft;
    private long sessionStart;

    private final float[] recentIntervals = new float[ROLLING_BUFFER_SIZE];
    private int recentIndex = 0;
    private int recentCount = 0;
    private float lastInterval = 65.0f;

    private int batchSwings = 0;
    private float batchElapsedMs = 0.0f;
    private double lastClickTimeMs = 0.0;

    public AutoClicker() {
        super("AutoClicker", "Clicks for you while you hold left", Category.COMBAT, Subcategory.BASIC);
    }

    @Override
    public String suffix() {
        return stable.bool() ? "Stable" : "Human";
    }

    @Override
    public void onEnable(Context context) {
        nextClickAt = -1;
        nextInventoryClickAt = -1;
        breaking = false;
        clearInventoryTarget();

        recentIndex = 0;
        recentCount = 0;
        lastInterval = 65.0f;
        batchSwings = 0;
        batchElapsedMs = 0.0f;
        lastClickTimeMs = 0.0;

        sessionStart = System.currentTimeMillis();
        tempo = clamp((minCps.number() + maxCps.number()) * 0.5f, 1.0f, 25.0f);
        drift = 0.0f;
        rhythmPhase = uniform(0.0f, (float) (2.0 * Math.PI));
        prevMotorDelay = (float) nextExponential(16.0);
        fatigue = 1.0f;
        fatigueTarget = 1.0f;
        fatigueClicksLeft = 0;
    }

    private void recordInterval(float interval) {
        recentIntervals[recentIndex] = interval;
        recentIndex = (recentIndex + 1) % ROLLING_BUFFER_SIZE;
        if (recentCount < ROLLING_BUFFER_SIZE) {
            recentCount++;
        }
        lastInterval = interval;
    }

    private float prospectiveStdDev(float cand, int count) {
        if (recentCount < count - 1) {
            return 45.0f;
        }
        int n = Math.min(count - 1, recentCount);
        float sum = cand;
        for (int i = 0; i < n; i++) {
            int idx = (recentIndex - 1 - i + ROLLING_BUFFER_SIZE) % ROLLING_BUFFER_SIZE;
            sum += recentIntervals[idx];
        }
        int total = n + 1;
        float mean = sum / total;
        float sqDiff = (cand - mean) * (cand - mean);
        for (int i = 0; i < n; i++) {
            int idx = (recentIndex - 1 - i + ROLLING_BUFFER_SIZE) % ROLLING_BUFFER_SIZE;
            float diff = recentIntervals[idx] - mean;
            sqDiff += diff * diff;
        }
        return (float) Math.sqrt(sqDiff / (total - 1));
    }

    private float prospectiveMean(float cand, int count) {
        if (recentCount < count - 1) {
            return cand;
        }
        int n = Math.min(count - 1, recentCount);
        float sum = cand;
        for (int i = 0; i < n; i++) {
            int idx = (recentIndex - 1 - i + ROLLING_BUFFER_SIZE) % ROLLING_BUFFER_SIZE;
            sum += recentIntervals[idx];
        }
        return sum / (n + 1);
    }

    @Override
    public void onDisable(Context context) {
        InputHookManager.simulateMouseButton(0, false);
        nextClickAt = -1;
        clearInventoryTarget();
    }

    public void setInventoryTarget(GuiContainer gui, Slot slot) {
        inventoryGui = gui;
        inventorySlot = slot;
    }

    private void clearInventoryTarget() {
        inventoryGui = null;
        inventorySlot = null;
    }

    @Subscribe
    public void onGameLoop(GameLoopEvent event) {
        if (!ClientUtils.isInWorld() || mc.thePlayer == null || mc.theWorld == null || mc.thePlayer.isDead) {
            breaking = false;
            if (nextClickAt >= 0) {
                InputHookManager.simulateMouseButton(0, false);
            }
            nextClickAt = -1;
            return;
        }
        if (Client.gui().visible() || mc.currentScreen != null || !mc.inGameHasFocus || !Mouse.isButtonDown(0)) {
            breaking = false;
            if (nextClickAt >= 0) {
                InputHookManager.simulateMouseButton(0, false);
            }
            nextClickAt = -1;
            return;
        }
        if (weaponOnly.bool() && !holdingWeapon()) {
            if (nextClickAt >= 0) {
                InputHookManager.simulateMouseButton(0, false);
            }
            nextClickAt = -1;
            return;
        }

        if (breakBlocks.bool() && targetingBlock()) {
            breaking = true;
            return;
        }
        if (breaking) {
            breaking = false;
            nextClickAt = -1;
        }

        double now = System.nanoTime() / 1.0e6;
        if (lastClickTimeMs > 0 && now - lastClickTimeMs > 2500.0) {
            batchSwings = 0;
            batchElapsedMs = 0.0f;
        }
        if (nextClickAt < 0) {
            nextClickAt = now + nextInterval();
            return;
        }

        if (now - nextClickAt > 250.0) {
            nextClickAt = now;
        }
        if (nextClickAt <= now) {
            mc.entityRenderer.getMouseOver(1.0f);
            mc.leftClickCounter = 0;
            mc.clickMouse();
            mc.leftClickCounter = 0;
            lastClickTimeMs = now;
            nextClickAt = now + nextInterval();
        }
    }

    @Subscribe
    public void onTickStart(TickStartEvent event) {
        GuiContainer gui = inventoryGui;
        Slot slot = inventorySlot;
        if (!inventoryClickActive(gui)) {
            nextInventoryClickAt = -1;
            return;
        }
        double now = System.nanoTime() / 1.0e6;
        if (nextInventoryClickAt < 0) {
            nextInventoryClickAt = now + INVENTORY_INTERVAL_MS;
            return;
        }
        if (nextInventoryClickAt > now) {
            return;
        }
        nextInventoryClickAt = Math.max(nextInventoryClickAt + INVENTORY_INTERVAL_MS, now);
        if (slot != null && slot.getHasStack() && slot.canTakeStack(mc.thePlayer)) {
            mc.playerController.windowClick(gui.inventorySlots.windowId, slot.slotNumber, 0, 1, mc.thePlayer);
        }
    }

    private boolean inventoryClickActive(GuiContainer gui) {
        if (!inventory.bool() || !ClientUtils.isInWorld() || gui == null || gui != mc.currentScreen
                || Client.gui().visible()) {
            return false;
        }
        InventoryManager manager = ModuleManager.get(InventoryManager.class);
        return gui.inventorySlots == mc.thePlayer.openContainer
                && GuiScreen.isShiftKeyDown()
                && Mouse.isButtonDown(0)
                && mc.thePlayer.inventory.getItemStack() == null
                && !manager.isExecutingPlan();
    }

    private static boolean holdingWeapon() {
        ItemStack held = mc.thePlayer.getHeldItem();
        if (held == null) {
            return false;
        }
        Item item = held.getItem();
        return item instanceof ItemSword || item instanceof ItemAxe;
    }

    private static boolean targetingBlock() {
        MovingObjectPosition hit = mc.objectMouseOver;
        if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            return false;
        }
        Block block = mc.theWorld.getBlockState(hit.getBlockPos()).getBlock();
        return block != Blocks.air && !(block instanceof BlockLiquid);
    }

    private double nextInterval() {
        if (stable.bool()) {
            return 1000.0 / stableCps.number();
        }
        float low = Math.min(minCps.number(), maxCps.number());
        float high = Math.max(minCps.number(), maxCps.number());
        if (!randomization.bool()) {
            float targetTempo = uniform(low, high);
            float base = 1000.0f / Math.max(targetTempo, 1.0f);
            float interval = uniform(base * 0.85f, base * 1.15f);
            recordInterval(interval);
            return interval;
        }
        updateFatigue();
        return humanInterval(low, high);
    }

    private float humanInterval(float low, float high) {
        low = clamp(low, 1.0f, 20.0f);
        high = clamp(high, low, 20.0f);
        float intensity = randomizationPercent.number() / 100.0f;
        batchSwings++;

        drift = clamp(0.98f * drift + (float) nextGaussian() * 0.04f, -0.22f, 0.22f);

        float targetTempo = uniform(low - 0.5f, high + 0.5f);
        tempo = clamp(0.92f * tempo + 0.08f * targetTempo, 1.0f, 25.0f);

        float clockBase = (1000.0f / tempo) * (1.0f + drift * (0.5f * intensity)) * fatigue;

        float muC = clockBase * 0.78f;
        float sigmaC = clockBase * 0.20f * intensity;
        float gaussC = muC + (float) (nextGaussian() * sigmaC);
        float expTail = (float) nextExponential(clockBase * 0.22f * intensity);
        float centralClock = gaussC + expTail;

        float nextMotorDelay = (float) nextExponential(16.0f * intensity);
        float interval = centralClock + nextMotorDelay - prevMotorDelay;
        prevMotorDelay = nextMotorDelay;

        rhythmPhase = (rhythmPhase + uniform(1.2f, 2.5f)) % (2.0f * (float) Math.PI);
        interval *= (1.0f + (float) Math.sin(rhythmPhase) * (0.08f * intensity));

        double roll = unit();
        if (roll < 0.15 * intensity) {
            interval = 36.0f + (float) nextGaussian() * 4.0f;
        } else if (roll < 0.185 * intensity) {

            interval += uniform(35.0f, 75.0f) * intensity;
        }

        if ((batchSwings == 20 || batchSwings == 40) && intensity > 0.3f) {
            interval += uniform(170.0f, 240.0f) * intensity;
        }

        float currentElapsed = batchElapsedMs + interval;
        int clicksLeft = 50 - batchSwings;
        if (clicksLeft > 0 && clicksLeft <= 4 && intensity > 0.3f) {
            float needed = 4080.0f - currentElapsed;
            if (needed > clicksLeft * 65.0f) {
                interval += (needed / (float) clicksLeft);
            }
        }

        interval = clamp(interval, 25.0f, 240.0f);

        float targetMinCv = 0.46f * intensity;
        float targetMinStd = Math.max(30.0f, clockBase * 0.40f) * intensity;

        for (int attempt = 0; attempt < 2; attempt++) {
            boolean lowVar = false;
            int[] windows = {6, 8, 10, 16};
            for (int w : windows) {
                float pStd = prospectiveStdDev(interval, w);
                float pMean = prospectiveMean(interval, w);
                float pCv = (pMean > 0.0f) ? (pStd / pMean) : 0.60f;
                if (pCv < targetMinCv || pStd < targetMinStd) {
                    lowVar = true;
                    if (interval >= pMean) {
                        interval = clamp(interval + uniform(28.0f, 48.0f), 25.0f, 240.0f);
                    } else {
                        interval = clamp(interval - uniform(18.0f, 32.0f), 25.0f, 240.0f);
                    }
                    break;
                }
            }
            if (!lowVar) {
                break;
            }
        }

        if (recentCount > 0 && Math.abs(interval - lastInterval) < 3.0f) {
            float delta = uniform(4.0f, 10.0f);
            interval += (interval >= lastInterval ? delta : -delta);
            interval = clamp(interval, 25.0f, 240.0f);
        }

        interval = Math.round(interval) + uniform(-0.15f, 0.15f);
        interval = clamp(interval, 25.0f, 240.0f);

        batchElapsedMs += interval;
        if (batchSwings >= 50) {
            batchSwings = 0;
            batchElapsedMs = 0.0f;
        }

        recordInterval(interval);
        return interval;
    }

    private void updateFatigue() {
        float sessionLoad = Math.min((System.currentTimeMillis() - sessionStart) / 1000L / 60.0f * 0.004f, 0.10f);
        if (fatigueClicksLeft <= 0) {
            fatigueTarget = 1.0f + sessionLoad + uniform(0.02f, 0.10f);
            fatigueClicksLeft = random.nextInt(40, 120);
        }
        --fatigueClicksLeft;
        fatigue += (fatigueTarget - fatigue) * 0.03f;
        fatigue = clamp(fatigue, 1.0f, 1.25f);
    }

    private double nextGaussian() {
        double u1 = Math.max(1e-9, random.nextDouble());
        double u2 = random.nextDouble();
        return Math.sqrt(-2.0 * Math.log(u1)) * Math.cos(2.0 * Math.PI * u2);
    }

    private double nextExponential(double scale) {
        double u = Math.max(1e-9, random.nextDouble());
        return -scale * Math.log(u);
    }

    private double unit() {
        return random.nextDouble();
    }

    private float uniform(float low, float high) {
        return low + (float) random.nextDouble() * (high - low);
    }

    private static float clamp(float value, float low, float high) {
        return Math.max(low, Math.min(high, value));
    }
}
