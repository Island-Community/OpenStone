package stone.module.render;

import stone.module.Category;
import stone.module.Module;
import stone.module.Subcategory;

public class NoCameraClip extends Module {
    public NoCameraClip() {
        super("NoCameraClip", "Keeps the third person camera out of walls", Category.VISUALS, Subcategory.BASIC);
    }
}
