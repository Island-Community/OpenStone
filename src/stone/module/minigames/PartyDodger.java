package stone.module.minigames;

import stone.event.ChatReceivedEvent;
import stone.event.EntityJoinWorldEvent;
import stone.event.PreUpdateEvent;
import stone.event.Subscribe;
import stone.module.Category;
import stone.module.Context;
import stone.module.Module;
import stone.module.Setting;
import stone.module.Subcategory;
import stone.util.ClientUtils;
import stone.util.rotation.RotationUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C03PacketPlayer.C05PacketPlayerLook;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;

public class PartyDodger extends Module {
    private final Setting gameMode;
    private final Setting minPartySize;
    private final Setting dodgeMultiple2qs;
    private final Setting preventLateJoin;
    private final Setting autoQueue;
    private static final double[] fourTeamQueueLookTarget;
    private static final double[] threeTeamQueueLookTarget;
    private PartyDodger$2 dodgeState = PartyDodger$2.IDLE;
    private long XIibl = 0L;
    private final Set<String> clda = new HashSet<>();
    private int nickedPlayerJoinCount = 0;
    private boolean dodgeTriggered = false;
    private long nickedJoinDodgeAt = -1L;
    private final LinkedList<Long> nickedPlayerJoinTimes = new LinkedList<>();
    private int lastBedWarsState = -1;
    private final LinkedList<Long> MFf = new LinkedList<>();
    private long CWA = -1L;
    private long initialLateJoinCheckAt = -1L;
    private long lateJoinCheckAt = -1L;
    private long lateJoinRescheduleAt = -1L;
    private final LinkedList<Long> lateJoinRequeueTimes = new LinkedList<>();
    private PartyDodger$1 Xn9 = PartyDodger$1.NONE;
    private int QxR = -1;
    private long lookPacketSentAt = 0L;
    private long chestRightClickAt = 0L;

    public PartyDodger() {
        super("PartyDodger", "Dodges enemies in BedWars queue", Category.MINIGAMES, Subcategory.ADVANCED);
        this.gameMode = addChoice("Game Mode", "", List.of("4s", "3s"), 0);
        this.minPartySize = addNumber("Min Party Size", "", 2.0F, 2.0F, 4.0F, 0);
        this.dodgeMultiple2qs = addBoolean("Dodge Multiple 2qs", "", false);
        this.preventLateJoin = addBoolean("Prevent Late Join", "", false);
        this.autoQueue = addBoolean("Auto-Queue", "", false);
    }

    @Override
    public void guiUpdate() {
        this.gameMode.setVisible(this.autoQueue.bool());
        this.dodgeMultiple2qs.setVisible(this.minPartySize.number() >= 3.0);
    }

    @Override
    public void onEnable(Context context) {
        this.resetModuleState();
    }

    @Override
    public void onDisable(Context context) {
        this.resetModuleState();
    }

    @Subscribe
    public void onEntityJoinWorld(EntityJoinWorldEvent var1) {
        if (var1.entity == mc.thePlayer) {
            this.lateJoinRescheduleAt = -1L;
            this.resetTracking();
            this.lastBedWarsState = -1;
            if (this.preventLateJoin.bool()) {
                this.initialLateJoinCheckAt = System.currentTimeMillis() + 100L;
            }
        }
    }

    @Subscribe
    public void onChatReceived(ChatReceivedEvent var1) {
        if (var1.messageType != 2 && ClientUtils.isInWorld()) {
            String var2 = var1.component.getFormattedText();
            String var3 = ClientUtils.AOAtn(var1.component.getUnformattedText());
            if (ClientUtils.getBedWarsBoardType() == 1 && var3.contains(" has joined ")) {
                boolean var4 = var2.contains("§k");
                if (!var4) {
                    String var5 = this.TegAt(var3);
                    if (var5 != null && !var5.equalsIgnoreCase(mc.thePlayer.getName())) {
                        this.clda.add(var5.toLowerCase());
                        this.MFf.add(System.currentTimeMillis());
                        if (!this.dodgeTriggered && this.dodgeState == PartyDodger$2.IDLE) {
                            this.CWA = System.currentTimeMillis() + 300L;
                        }
                    }
                } else {
                    this.nickedPlayerJoinCount++;
                    this.nickedPlayerJoinTimes.add(System.currentTimeMillis());
                    if (!this.dodgeTriggered && this.dodgeState == PartyDodger$2.IDLE) {
                        this.nickedJoinDodgeAt = System.currentTimeMillis() + 300L;
                    }
                }
            }
        }
    }

    @Subscribe
    public void onPreUpdate(PreUpdateEvent var1) {
        if (ClientUtils.isInWorld()) {
            int var2 = ClientUtils.getBedWarsBoardType();
            if (var2 == 1 && this.lastBedWarsState != 1) {
                this.resetTracking();
            }

            this.lastBedWarsState = var2;
            if (var2 == 1) {
                this.scanQueueState();
            }

            if (this.initialLateJoinCheckAt > 0L && System.currentTimeMillis() >= this.initialLateJoinCheckAt) {
                this.initialLateJoinCheckAt = -1L;
                this.IUXol();
            }

            if (this.lateJoinCheckAt > 0L && System.currentTimeMillis() >= this.lateJoinCheckAt) {
                this.lateJoinCheckAt = -1L;
                long var3 = System.currentTimeMillis();
                this.lateJoinRequeueTimes.removeIf((recoveredArg0) -> PartyDodger.isRequeueExpired(var3, (java.lang.Long) recoveredArg0));
                if (this.lateJoinRequeueTimes.size() < 2) {
                    this.lateJoinRequeueTimes.add(var3);
                    ClientUtils.sendColoredMessage("&cLate join! Requeuing...");
                    this.bhal();
                    this.lateJoinRescheduleAt = var3 + 500L;
                }
            }

            if (this.lateJoinRescheduleAt > 0L && System.currentTimeMillis() >= this.lateJoinRescheduleAt) {
                this.lateJoinRescheduleAt = -1L;
                long var5 = 4500L + (long)(Math.random() * 500.0);
                this.lateJoinCheckAt = System.currentTimeMillis() + var5;
            }

            this.guiUpdate();
            switch (this.dodgeState) {
                case IDLE:
                    if (this.nickedJoinDodgeAt > 0L && System.currentTimeMillis() >= this.nickedJoinDodgeAt) {
                        this.nickedJoinDodgeAt = -1L;
                        this.evaluatePartySizeDodge();
                    }

                    if (this.CWA > 0L && System.currentTimeMillis() >= this.CWA) {
                        this.CWA = -1L;
                        this.checkQueueStarted();
                    }
                    break;
                case PENDING_DODGE:
                    if (this.getStateElapsedMs() >= 250L) {
                        this.bhal();
                    }
                    break;
                case WARPING_TRANSFER:
                    if (this.isInLobby()) {
                        if (this.autoQueue.bool()) {
                            this.fsqzy(PartyDodger$2.WAITING_LOBBY);
                        } else {
                            this.fsqzy(PartyDodger$2.IDLE);
                            this.resetTracking();
                        }
                    } else if (this.getStateElapsedMs() >= 20000L) {
                        this.fsqzy(PartyDodger$2.IDLE);
                    }
                    break;
                case WAITING_LOBBY:
                    if (this.getStateElapsedMs() >= 1500L) {
                        this.KvtPw();
                        this.fsqzy(PartyDodger$2.QUEUEING);
                    }
                    break;
                case QUEUEING:
                    long var6 = this.getStateElapsedMs();
                    if (this.lookPacketSentAt == 0L && var6 >= 300L) {
                        this.sendQueueLookPacket();
                    } else if (this.lookPacketSentAt != 0L && this.chestRightClickAt == 0L && var6 - 300L >= 150L) {
                        if (mc.currentScreen == null) {
                            mc.entityRenderer.getMouseOver(1.0F);
                            mc.rightClickMouse();
                            this.chestRightClickAt = System.currentTimeMillis();
                        }
                    } else if (this.chestRightClickAt != 0L && System.currentTimeMillis() - this.chestRightClickAt >= 350L) {
                        this.openChestWithClick();
                    }
                    break;
                case CLICKING_CHEST:
                    this.clickChestSignSlot();
            }
        }
    }

    private void scanQueueState() {
        List var1 = this.getSidebarLines();
        PartyDodger$1 var2 = PartyDodger$1.NONE;
        int var3 = -1;

        for (String var5 : (java.lang.Iterable<String>) (java.lang.Iterable<?>) (var1)) {
            if (var5.contains("Waiting...")) {
                var2 = PartyDodger$1.WAITING;
                break;
            }

            int var6 = this.parseStartingCountdown(var5);
            if (var6 >= 0) {
                var2 = PartyDodger$1.STARTING;
                var3 = var6;
                break;
            }
        }

        this.Xn9 = var2;
        this.QxR = var3;
    }

    private List<String> getSidebarLines() {
        ArrayList var1 = new ArrayList();
        if (mc.theWorld == null) {
            return var1;
        } else {
            Scoreboard var2 = mc.theWorld.getScoreboard();
            ScoreObjective var3 = var2.getObjectiveInDisplaySlot(1);
            if (var3 == null) {
                return var1;
            } else {
                for (Score var5 : var2.getSortedScores(var3)) {
                    if (!var5.getPlayerName().startsWith("#")) {
                        ScorePlayerTeam var6 = var2.getPlayersTeam(var5.getPlayerName());
                        String var7 = ScorePlayerTeam.formatPlayerName(var6, var5.getPlayerName());
                        var1.add(ClientUtils.AOAtn(var7).trim());
                    }
                }

                return var1;
            }
        }
    }

    private int parseStartingCountdown(String var1) {
        if (!var1.startsWith("Starting in ")) {
            return -1;
        } else {
            StringBuilder var2 = new StringBuilder();

            for (char var6 : var1.substring(12).toCharArray()) {
                if (!Character.isDigit(var6)) {
                    break;
                }

                var2.append(var6);
            }

            if (var2.length() == 0) {
                return -1;
            } else {
                try {
                    return Integer.parseInt(var2.toString());
                } catch (NumberFormatException var7) {
                    return -1;
                }
            }
        }
    }

    private void evaluatePartySizeDodge() {
        int var1 = (int)this.minPartySize.number();
        int var2 = this.countPartySize();
        if (var2 >= var1) {
            if (this.DAkU1()) {
                mc.thePlayer.sendChatMessage("/pc Party Detected " + var2 + "q. Risky Dodge.");
            } else {
                this.startDodge(var2 + "q");
            }
        } else {
            if (this.dodgeMultiple2qs.bool() && var1 >= 3 && this.countMultiple2qPairs() >= 2) {
                if (this.DAkU1()) {
                    mc.thePlayer.sendChatMessage("/pc Party Detected multiple 2qs. Risky Dodge.");
                    return;
                }

                this.startDodge("multiple 2qs");
            }
        }
    }

    private void checkQueueStarted() {
        if (!this.dodgeTriggered) {
            boolean var1 = (int)this.gameMode.choice() == 0;
            int var2 = var1 ? 4000 : 3000;
            int var3 = var1 ? 4 : 3;
            long var4 = System.currentTimeMillis();
            int var6 = 0;

            for (Long var8 : this.MFf) {
                if (var4 - var8 <= var2) {
                    var6++;
                }
            }

            if (var6 > var3) {
                if (this.DAkU1()) {
                    mc.thePlayer.sendChatMessage("/pc Party Detected " + var6 + "q. Risky Dodge.");
                } else {
                    ClientUtils.sendColoredMessage("&cDidn't start queue!");
                    this.dodgeTriggered = true;
                    this.fsqzy(PartyDodger$2.PENDING_DODGE);
                }
            }
        }
    }

    private boolean DAkU1() {
        return this.Xn9 == PartyDodger$1.STARTING && this.QxR >= 0 && this.QxR < 2;
    }

    private int countPartySize() {
        if (this.nickedPlayerJoinTimes.isEmpty()) {
            return 0;
        } else {
            Long[] var1 = (Long[]) this.nickedPlayerJoinTimes.toArray(new Long[0]);
            int var2 = 0;

            for (int var3 = 0; var3 < var1.length; var3++) {
                int var4 = 0;

                for (int var5 = var3; var5 < var1.length && var1[var5] - var1[var3] <= 500L; var5++) {
                    var4++;
                }

                if (var4 > var2) {
                    var2 = var4;
                }
            }

            return var2;
        }
    }

    private void startDodge(String var1) {
        ClientUtils.sendColoredMessage("&cDodging &7" + var1 + " detected!");
        this.dodgeTriggered = true;
        this.fsqzy(PartyDodger$2.PENDING_DODGE);
    }

    private void bhal() {
        mc.thePlayer.sendChatMessage("/l");
        if (this.autoQueue.bool()) {
            this.fsqzy(PartyDodger$2.WARPING_TRANSFER);
        } else {
            this.fsqzy(PartyDodger$2.IDLE);
            this.resetTracking();
        }
    }

    private void KvtPw() {
        this.lookPacketSentAt = 0L;
        this.chestRightClickAt = 0L;
        mc.thePlayer.inventory.currentItem = 5;
        mc.playerController.syncCurrentPlayItem();
    }

    private void sendQueueLookPacket() {
        double[] var1 = (int)this.gameMode.choice() == 0 ? fourTeamQueueLookTarget : threeTeamQueueLookTarget;
        double var2 = var1[0] - mc.thePlayer.posX;
        double var4 = var1[1] + 0.5 - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double var6 = var1[2] - mc.thePlayer.posZ;
        double var8 = Math.sqrt(var2 * var2 + var6 * var6);
        float var10 = (float)Math.toDegrees(Math.atan2(var6, var2)) - 90.0F;
        float var11 = (float)(-Math.toDegrees(Math.atan2(var4, var8)));
        float[] var12 = RotationUtils.NSsr(var10, var11, RotationUtils.lastSentRotation[0], RotationUtils.lastSentRotation[1]);
        var10 = var12[0];
        var11 = var12[1];
        mc.thePlayer.sendQueue.addToSendQueue(new C05PacketPlayerLook(var10, var11, mc.thePlayer.onGround));
        mc.thePlayer.rotationYaw = var10;
        mc.thePlayer.rotationPitch = var11;
        this.lookPacketSentAt = System.currentTimeMillis();
    }

    private void openChestWithClick() {
        if (mc.currentScreen == null) {
            mc.entityRenderer.getMouseOver(1.0F);
            mc.clickMouse();
            this.fsqzy(PartyDodger$2.CLICKING_CHEST);
        }
    }

    private void clickChestSignSlot() {
        if (!(mc.currentScreen instanceof GuiChest)) {
            if (this.getStateElapsedMs() >= 2000L) {
                mc.thePlayer.sendChatMessage("/stuck");
                this.KvtPw();
                this.fsqzy(PartyDodger$2.WAITING_LOBBY);
            }
        } else {
            Container var1 = mc.thePlayer.openContainer;
            int var2 = var1.inventorySlots.size() - 36;

            for (int var3 = 0; var3 < var2; var3++) {
                Slot var4 = (Slot)var1.inventorySlots.get(var3);
                ItemStack var5 = var4.getStack();
                if (var5 != null && var5.getItem() == Items.sign) {
                    mc.playerController.windowClick(var1.windowId, var4.slotNumber, 0, 0, mc.thePlayer);
                    this.fsqzy(PartyDodger$2.IDLE);
                    this.resetTracking();

                    return;
                }
            }
        }
    }

    private int countMultiple2qPairs() {
        if (this.nickedPlayerJoinTimes.size() < 2) {
            return 0;
        } else {
            Long[] var1 = (Long[]) this.nickedPlayerJoinTimes.toArray(new Long[0]);
            int var2 = 0;

            for (int var3 = 0; var3 < var1.length - 1; var3++) {
                if (var1[var3 + 1] - var1[var3] <= 600L) {
                    var2++;
                    var3++;
                }
            }

            return var2;
        }
    }

    private void IUXol() {
        if (this.preventLateJoin.bool()) {
            for (String var3 : this.getSidebarLines()) {
                if (var3.startsWith("Players: ")) {
                    String var4 = var3.substring("Players: ".length());
                    int var5 = var4.indexOf(47);
                    if (var5 >= 0) {
                        try {
                            int var6 = Integer.parseInt(var4.substring(0, var5).trim());
                            int var7 = Integer.parseInt(var4.substring(var5 + 1).trim());
                            int var8 = var7 == 16 ? 4 : (var7 == 12 ? 3 : -1);
                            if (var8 >= 0) {
                                if (var6 > var8) {
                                    this.lateJoinCheckAt = System.currentTimeMillis() + 1000L;
                                }
                                break;
                            }
                        } catch (NumberFormatException var9) {
                            break;
                        }
                    }
                }
            }
        }
    }

    private String TegAt(String var1) {
        int var2 = var1.indexOf(" has joined ");
        if (var2 <= 0) {
            return null;
        } else {
            String var3 = var1.substring(0, var2).trim();
            int var4 = var3.lastIndexOf(93);
            if (var4 >= 0) {
                var3 = var3.substring(var4 + 1).trim();
            }

            return var3.isEmpty() ? null : var3;
        }
    }

    private boolean isInLobby() {
        return ClientUtils.getBedWarsBoardType() == 0;
    }

    private void fsqzy(PartyDodger$2 var1) {
        this.dodgeState = var1;
        this.XIibl = System.currentTimeMillis();
    }

    private long getStateElapsedMs() {
        return System.currentTimeMillis() - this.XIibl;
    }

    private void resetTracking() {
        this.clda.clear();
        this.MFf.clear();
        this.nickedPlayerJoinCount = 0;
        this.nickedPlayerJoinTimes.clear();
        this.dodgeTriggered = false;
        this.nickedJoinDodgeAt = -1L;
        this.CWA = -1L;
        this.Xn9 = PartyDodger$1.NONE;
        this.QxR = -1;
    }

    private void resetModuleState() {
        this.fsqzy(PartyDodger$2.IDLE);
        this.resetTracking();
        this.lookPacketSentAt = 0L;
        this.chestRightClickAt = 0L;
        this.lastBedWarsState = -1;
        this.initialLateJoinCheckAt = -1L;
        this.lateJoinCheckAt = -1L;
        this.lateJoinRescheduleAt = -1L;
        this.lateJoinRequeueTimes.clear();
    }

    private static boolean isRequeueExpired(long var0, Long var2) {
        return var0 - var2 > 15000L;
    }

    static {
        double[] var10000 = new double[3];
        var10000[0] = -3.5;
        var10000[1] = 67.0;
        var10000[2] = -3.5;
        fourTeamQueueLookTarget = var10000;
        var10000 = new double[3];
        var10000[0] = -3.5;
        var10000[1] = 67.0;
        var10000[2] = -7.5;
        threeTeamQueueLookTarget = var10000;
    }
}
