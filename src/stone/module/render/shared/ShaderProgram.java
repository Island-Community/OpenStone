package stone.module.render.shared;

import stone.core.Assets;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL20;

public class ShaderProgram {
    public final int programId;
    private final Map<String, Integer> uniformLocations = new HashMap<>();

    public ShaderProgram(String var1, String var2) {
        int var3 = GL20.glCreateProgram();

        try {
            ShaderLoader.attachShaders(var3, openFragmentShaderStream(var1), openVertexShaderStream(var2));
        } catch (IOException var5) {
            var5.printStackTrace();
        }

        GL20.glLinkProgram(var3);
        if (GL20.glGetProgrami(var3, 35714) == 0) {
            throw new IllegalStateException("Shader failed to link!");
        } else {
            this.programId = var3;
        }
    }

    public ShaderProgram(String var1) {
        this(var1, "minecraft:shaders/vertex.vsh");
    }

    private static InputStream openFragmentShaderStream(String var0) throws IOException {
        InputStream var1 = ShaderRegistry.openShaderStream(var0);
        return var1 != null ? var1 : Minecraft.getMinecraft().getResourceManager().getResource(new ResourceLocation(var0)).getInputStream();
    }

    private static InputStream openVertexShaderStream(String var0) throws IOException {
        InputStream var1 = Assets.open("/assets/" + var0.replace(':', '/'));
        return var1 != null ? var1 : Minecraft.getMinecraft().getResourceManager().getResource(new ResourceLocation(var0)).getInputStream();
    }

    public static void drawQuad(float var0, float var1, float var2, float var3) {
        ScreenQuad.drawQuad(var0, var1, var2, var3);
    }

    public void eBml() {
        GL20.glUseProgram(this.programId);
    }

    public void unbindProgram() {
        GL20.glUseProgram(0);
    }

    private int uniformLocation(String var1) {
        return this.uniformLocations.computeIfAbsent(var1, var2 -> GL20.glGetUniformLocation(this.programId, var2));
    }

    public void setUniform(String var1, float... var2) {
        int var3 = this.uniformLocation(var1);
        if (var3 != -1) {
            switch (var2.length) {
                case 1:
                    GL20.glUniform1f(var3, var2[0]);
                    break;
                case 2:
                    GL20.glUniform2f(var3, var2[0], var2[1]);
                    break;
                case 3:
                    GL20.glUniform3f(var3, var2[0], var2[1], var2[2]);
                    break;
                case 4:
                    GL20.glUniform4f(var3, var2[0], var2[1], var2[2], var2[3]);
            }
        }
    }

    public void lyik(String var1, int... var2) {
        int var3 = this.uniformLocation(var1);
        if (var3 != -1) {
            if (var2.length > 1) {
                GL20.glUniform2i(var3, var2[0], var2[1]);
            } else {
                GL20.glUniform1i(var3, var2[0]);
            }
        }
    }
}
