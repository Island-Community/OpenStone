package stone.module.render.shared;

import stone.core.Assets;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class ShaderRegistry {
    private static final Map<String, String> IjM;

    private ShaderRegistry() {
    }

    public static String ITAJb(String var0) {
        String var1 = IjM.get(var0);
        return var1 == null ? null : "/assets/jade/shaders/" + var1;
    }

    public static InputStream openShaderStream(String var0) {
        String var1 = ITAJb(var0);
        return var1 == null ? null : Assets.open(var1);
    }

    static {
        HashMap var0 = new HashMap();
        var0.put("roundedRectRise", "rounded_rise.fsh");
        var0.put("roundedRect", "rounded_rect.fsh");
        IjM = Collections.unmodifiableMap(var0);
    }
}
