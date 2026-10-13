package stone.game;

import stone.core.Mat4;
import java.util.ArrayList;
import java.util.List;

public final class FrameState {
    public boolean inWorld;
    public boolean screenOpen;
    public float tickProgress;
    public int fps;
    public int ping = -1;

    public int windowWidth;
    public int windowHeight;
    public int scaledWidth;
    public int scaledHeight;
    public int scaleFactor = 1;
    public double mouseSensitivity = 0.5;

    public long worldTime;

    public final LocalPlayerView player = new LocalPlayerView();
    public final CameraView camera = new CameraView();
    public Mat4 projection = new Mat4();
    public Mat4 view = new Mat4();

    public boolean matricesValid;

    public final List<EntityView> entities = new ArrayList<>();
}
