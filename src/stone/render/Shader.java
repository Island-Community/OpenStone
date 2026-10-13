package stone.render;

import stone.core.Color;
import stone.core.Log;
import stone.core.Mat4;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

public final class Shader {
    private static final String SCOPE = "shader";

    private final Map<String, Integer> uniforms = new HashMap<>();
    private final FloatBuffer matrix = BufferUtils.createFloatBuffer(16);
    private String name = "";
    private int program;

    public boolean compile(String programName, String vertexSource, String fragmentSource) {
        destroy();
        name = programName;
        int vertex = compileStage(GL20.GL_VERTEX_SHADER, vertexSource);
        if (vertex == 0) {
            return false;
        }
        int fragment = compileStage(GL20.GL_FRAGMENT_SHADER, fragmentSource);
        if (fragment == 0) {
            GL20.glDeleteShader(vertex);
            return false;
        }
        int linked = GL20.glCreateProgram();
        GL20.glAttachShader(linked, vertex);
        GL20.glAttachShader(linked, fragment);
        GL20.glLinkProgram(linked);
        GL20.glDeleteShader(vertex);
        GL20.glDeleteShader(fragment);
        if (GL20.glGetProgrami(linked, GL20.GL_LINK_STATUS) != GL11.GL_TRUE) {
            int length = GL20.glGetProgrami(linked, GL20.GL_INFO_LOG_LENGTH);
            Log.error(SCOPE, "%s failed to link:\n%s", name, GL20.glGetProgramInfoLog(linked, Math.max(length, 1)));
            GL20.glDeleteProgram(linked);
            return false;
        }
        program = linked;
        Log.debug(SCOPE, "%s linked as program %d", name, program);
        return true;
    }

    private int compileStage(int type, String source) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_TRUE) {
            return shader;
        }
        int length = GL20.glGetShaderi(shader, GL20.GL_INFO_LOG_LENGTH);
        Log.error(SCOPE, "%s %s stage failed to compile:\n%s", name,
                type == GL20.GL_VERTEX_SHADER ? "vertex" : "fragment",
                GL20.glGetShaderInfoLog(shader, Math.max(length, 1)));
        GL20.glDeleteShader(shader);
        return 0;
    }

    public void destroy() {
        if (program != 0) {
            GL20.glDeleteProgram(program);
            program = 0;
        }
        uniforms.clear();
    }

    public boolean valid() {
        return program != 0 && GL20.glIsProgram(program);
    }

    public void bind() {
        GL20.glUseProgram(program);
    }

    private int uniform(String uniformName) {
        Integer cached = uniforms.get(uniformName);
        if (cached != null) {
            return cached;
        }
        int location = GL20.glGetUniformLocation(program, uniformName);
        uniforms.put(uniformName, location);
        if (location < 0) {
            Log.trace(SCOPE, "%s has no uniform '%s' (optimised out?)", name, uniformName);
        }
        return location;
    }

    public void set(String uniformName, int value) {
        int location = uniform(uniformName);
        if (location >= 0) {
            GL20.glUniform1i(location, value);
        }
    }

    public void set(String uniformName, float value) {
        int location = uniform(uniformName);
        if (location >= 0) {
            GL20.glUniform1f(location, value);
        }
    }

    public void set(String uniformName, float x, float y) {
        int location = uniform(uniformName);
        if (location >= 0) {
            GL20.glUniform2f(location, x, y);
        }
    }

    public void set(String uniformName, float x, float y, float z, float w) {
        int location = uniform(uniformName);
        if (location >= 0) {
            GL20.glUniform4f(location, x, y, z, w);
        }
    }

    public void set(String uniformName, Color color) {
        set(uniformName, color.r(), color.g(), color.b(), color.a());
    }

    public void set(String uniformName, Mat4 value) {
        int location = uniform(uniformName);
        if (location >= 0) {
            matrix.clear();
            matrix.put(value.m).flip();
            GL20.glUniformMatrix4(location, false, matrix);
        }
    }
}
