package stone.util;

import stone.module.Module;
import stone.util.block.MiningTargetCheck;
import stone.util.block.PassableBlocks;
import stone.util.item.WeaponCheck;
import stone.util.rotation.AngleUtils;
import stone.util.rotation.EntityAngles;
import stone.util.rotation.FovUtils;
import stone.util.rotation.MovementMathHelper;
import stone.util.rotation.RotationUtils;
import stone.util.team.RelationManager;
import stone.util.team.TeamColors;
import stone.util.team.TeamUtils;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Random;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemSword;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class ClientUtils implements IMinecraft {
    private static final Random RANDOM = new Random();
    public static boolean canRiderInteract(Entity var0) {
        return AttackHelper.invokeCanRiderInteract(var0);
    }

    public static boolean addEnemy(String var0) {
        return RelationManager.INSTANCE.addEnemy(var0);
    }

    public static boolean isRealPlayer(EntityPlayer var0) {
        return TabListPlayers.isListed(mc, var0);
    }

    public static boolean vAxywR() {
        return GameStateUtils.isInGameWithFocus(mc);
    }

    public static int oCoqd(Entity var0) {
        return TeamColors.getEntityTeamColor(var0);
    }

    public static String formatNumberAsString(double var0) {
        return TextUtils.ASXEvc(var0);
    }

    public static int randomInt(int var0, int var1) {
        return TextUtils.randomIntInclusive(RANDOM, var0, var1);
    }

    public static double randomDouble(double var0, double var2) {
        return TextUtils.randomDoubleInRange(RANDOM, var0, var2);
    }

    public static boolean isLookingAtEntity(float var0, Entity var1) {
        return dtpL7(var0, var1.posX, var1.posZ);
    }

    public static boolean dtpL7(float var0, double var1, double var3) {
        return isFacingCoordinates(mc.thePlayer, var0, var1, var3);
    }

    public static boolean isFacingCoordinates(Entity var0, float var1, double var2, double var4) {
        return FovUtils.isAngleWithinFov(var0.rotationYaw, var1, RotationUtils.ilaZ(var2, var4));
    }

    public static boolean Bhr95(float var0, float var1, float var2) {
        return FovUtils.isAngleWithinFov(var0, var1, var2);
    }

    public static Vec3 getLookVector(float var0, float var1) {
        return AngleUtils.getLookVector(var0, var1);
    }

    public static void sendColoredMessage(String var0) {
        ChatSender.HARd(mc, var0);
    }

    public static String formatTitle(String var0) {
        return ChatSender.formatTitle(var0);
    }

    public static void sendTitledMessage(String var0, String var1) {
        ChatSender.daPdo(mc, var0, " " + var1);
    }

    public static void sendDebugMessage(String var0) {
        ChatSender.sendDebugMessage(mc, var0);
    }

    public static void aidx(Entity var0, boolean var1, boolean var2) {
        AttackHelper.performAttack(mc, var0, var1, var2);
    }

    public static boolean xusXfhC(KeyBinding var0) {
        return KeyCodes.isDown(var0.getKeyCode(), Keyboard::isKeyDown, Mouse::isButtonDown);
    }

    public static boolean isEnemy(EntityPlayer var0) {
        return var0 != null && isEnemyName(var0.getName());
    }

    public static boolean isEnemyName(String var0) {
        return RelationManager.INSTANCE.isEnemy(var0);
    }

    public static String translateColorCodes(String var0) {
        return TextUtils.QJdK(var0);
    }

    public static String getFirstColorCode(String var0) {
        return TextUtils.NTfs(var0);
    }

    public static boolean isFriend(EntityPlayer var0) {
        return var0 != null && isFriendName(var0.getName());
    }

    public static boolean isFriendName(String var0) {
        return RelationManager.INSTANCE.isFriend(var0);
    }

    public static boolean isInWorld() {
        return GameStateUtils.isInWorld(mc);
    }

    public static boolean isOnHypixel() {
        return GameStateUtils.isOnHypixel(mc);
    }

    public static int YVVZ(int var0, int var1) {
        return TextUtils.withAlpha(var0, var1);
    }

    public static boolean isTeammate(Entity var0) {
        return TeamUtils.isTeammate(mc, var0);
    }

    public static String RcNd() {
        return TeamUtils.getOwnFormattedName(mc);
    }

    public static void eqyXnoq() {
        mc.timer.timerSpeed = 1.0F;
    }

    public static boolean YDGRCC() {
        return GameStateUtils.isPlayerInventoryOpen(mc);
    }

    public static int getSkyWarsBoardType() {
        return ScoreboardUtils.getSkyWarsBoardType(mc);
    }

    public static int getBedWarsBoardType() {
        return ScoreboardUtils.getBedWarsBoardType(mc);
    }

    public static String zaUnpz(String var0) {
        return ScoreboardUtils.IwugSbg(var0);
    }

    public static List<String> PxSw4() {
        return ScoreboardUtils.dj854(mc);
    }

    public static Random getRandom() {
        return RANDOM;
    }

    public static boolean uUwk() {
        return mc.thePlayer.moveForward != 0.0F || mc.thePlayer.moveStrafing != 0.0F;
    }

    public static double getYawDifferenceToEntity(Entity var0, boolean var1) {
        float var2 = var1 ? RotationUtils.lastSentRotation[0] : mc.thePlayer.rotationYaw;
        return EntityAngles.wrapAngleDifference(var2, VrOe(var0));
    }

    public static double coPw(Entity var0, boolean var1) {
        float var2 = var1 ? RotationUtils.lastSentRotation[1] : mc.thePlayer.rotationPitch;
        return EntityAngles.wrapAngleDifference(var2, getPitchToEntity(var0));
    }

    public static float VrOe(Entity var0) {
        return EntityAngles.getYawTo(mc.thePlayer, var0);
    }

    public static float getPitchToEntity(Entity var0) {
        return EntityAngles.getPitchTo(mc.thePlayer, var0);
    }

    public static void setHeldSlot(int var0, boolean var1) {
        mc.thePlayer.inventory.currentItem = var0;
        if (var1) {
            mc.playerController.syncCurrentPlayItem();
        }
    }

    public static MovingObjectPosition rayTraceBlocksWithRotations(double var0, float var2, float var3) {
        return RotationUtils.traceBlockHit(var0, var2, var3);
    }

    public static boolean isJumpKeyDown() {
        return GameStateUtils.yrvB(mc);
    }

    public static boolean isMiningBlock() {
        return MiningTargetCheck.isMiningBlock(mc);
    }

    public static long LvhY(long var0, long var2) {
        return Math.abs(var2 - var0);
    }

    public static void sendModuleMessage(Module var0, String var1) {
        sendTitledMessage("Stone", "&f" + var0.name + "&7: " + var1);
    }

    public static double WXYd(double var0, int var2) {
        if (var2 == 0) {
            return Math.round(var0);
        } else {
            double var3 = Math.pow(10.0, var2);
            return Math.round(var0 * var3) / var3;
        }
    }

    public static String AOAtn(String var0) {
        return var0.isEmpty() ? var0 : ScoreboardUtils.IwugSbg(var0);
    }

    public static List<String> getSidebarLines() {
        return ScoreboardLines.getSidebarLines(mc);
    }

    public static void startSwing() {
        AttackHelper.forceSwing(mc);
    }

    public static boolean isHoldingWeapon() {
        return PIRHgte(mc.thePlayer);
    }

    public static boolean PIRHgte(EntityLivingBase var0) {
        return WeaponCheck.isHoldingEnabledWeapon(var0);
    }

    public static boolean CqWuiK() {
        return mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword;
    }

    public static float computeInputMovementAngle() {
        return lgSi(mc.thePlayer.rotationYaw, mc.thePlayer.movementInput.moveForward, mc.thePlayer.movementInput.moveStrafe);
    }

    public static float lgSi(float var0, float var1, float var2) {
        return MovementMathHelper.computeMoveDirectionRadians(var0, var1, var2);
    }

    public static boolean isPassableBlock(ItemBlock var0) {
        return PassableBlocks.isPassable(var0.getBlock());
    }

    public static String readStreamText(InputStream var0) {
        StringBuilder var1 = new StringBuilder();

        try {
            BufferedReader var2 = new BufferedReader(new InputStreamReader(var0));

            for (String var3 = var2.readLine(); var3 != null; var3 = var2.readLine()) {
                var1.append(var3).append('\n');
            }
        } catch (Exception var4) {
            var4.printStackTrace();
        }

        return var1.toString();
    }

    public static boolean mdeQ() {
        return isOnHypixel() && ScoreboardUtils.ATIk(mc);
    }

    public static Vec3 LHYz(double var0) {
        return TabListPlayers.nearestPosition(mc, var0);
    }
}
