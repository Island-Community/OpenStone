package stone.util.font;

import java.io.File;

public final class FontManager$8 {
    final File file;
    final int style;
    final String cacheKey;

    FontManager$8(File var2, int var3) {
        this.file = var2;
        this.style = var3;
        this.cacheKey = var2.getAbsolutePath() + ":" + var2.lastModified() + ":" + var2.length();
    }
}
