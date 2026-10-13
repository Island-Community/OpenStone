package stone.module.shared;

public final class TargetEligibility {
    private TargetEligibility() {
    }

    public static boolean isEligibleTarget(boolean var0, boolean var1, boolean var2, boolean var3, int var4, boolean var5, boolean var6, boolean var7, boolean var8) {
        return var0 && !var1 && !var2 && !var3 && var4 == 0 && !var5 && !var6 && (!var8 || !var7);
    }
}
