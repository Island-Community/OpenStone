package stone.module.render.blockesp;

import stone.util.ClientUtils;
import java.util.List;

public final class BedWarsDetector {
    private BedWarsDetector() {
    }

    public static boolean isBedWarsGame() {
        return matchesBedWarsTabList(ClientUtils.PxSw4()) || ClientUtils.getSkyWarsBoardType() == 2;
    }

    public static boolean matchesBedWarsTabList(List<String> var0) {
        if (var0.size() >= 7 && stripColors((String)var0.get(0)).startsWith("BED WARS")) {
            String[] var1 = stripColors((String)var0.get(1)).split("  ");
            if (var1.length > 1) {
                String var2 = var1[1];
                if (var2.endsWith("]")) {
                    var2 = var2.split(" ")[0];
                }

                if (var2.startsWith("L")) {
                    return false;
                }
            }

            if (stripColors((String)var0.get(5)).startsWith("R Red:") && stripColors((String)var0.get(6)).startsWith("B Blue:")) {
                return true;
            } else {
                String var3 = stripColors((String)var0.get(6));
                return !var3.equals("Waiting...") && !var3.startsWith("Starting in") ? false : false;
            }
        } else {
            return false;
        }
    }

    private static String stripColors(String var0) {
        return ClientUtils.zaUnpz(var0);
    }
}
