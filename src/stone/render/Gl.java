package stone.render;

import stone.core.Log;
import java.nio.ByteBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;

public final class Gl {
    private static final String SCOPE = "gl";

    private Gl() {}

    public static void checkError(String stage) {
        int error = GL11.glGetError();
        for (int guard = 0; error != GL11.GL_NO_ERROR && guard < 8; ++guard) {
            Log.error(SCOPE, "GL error 0x%04X during %s", error, stage);
            error = GL11.glGetError();
        }
    }

    static void disableClientArrays(int units) {
        for (int unit = 0; unit < units; ++unit) {
            GL13.glClientActiveTexture(GL13.GL_TEXTURE0 + unit);
            GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
            GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
            GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        }
        GL13.glClientActiveTexture(GL13.GL_TEXTURE0);
    }

    static int uploadField(int texture, int width, int height, byte[] pixels) {
        if (texture == 0) {
            texture = GL11.glGenTextures();
        }
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);

        int alignment = GL11.glGetInteger(GL11.GL_UNPACK_ALIGNMENT);
        int rowLength = GL11.glGetInteger(GL11.GL_UNPACK_ROW_LENGTH);
        int skipRows = GL11.glGetInteger(GL11.GL_UNPACK_SKIP_ROWS);
        int skipPixels = GL11.glGetInteger(GL11.GL_UNPACK_SKIP_PIXELS);
        int swapBytes = GL11.glGetInteger(GL11.GL_UNPACK_SWAP_BYTES);
        int lsbFirst = GL11.glGetInteger(GL11.GL_UNPACK_LSB_FIRST);
        int imageHeight = GL11.glGetInteger(GL12.GL_UNPACK_IMAGE_HEIGHT);
        int skipImages = GL11.glGetInteger(GL12.GL_UNPACK_SKIP_IMAGES);
        int unpackBuffer = GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
        GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SWAP_BYTES, GL11.GL_FALSE);
        GL11.glPixelStorei(GL11.GL_UNPACK_LSB_FIRST, GL11.GL_FALSE);
        GL11.glPixelStorei(GL12.GL_UNPACK_IMAGE_HEIGHT, 0);
        GL11.glPixelStorei(GL12.GL_UNPACK_SKIP_IMAGES, 0);
        if (unpackBuffer != 0) {
            GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);
        }

        ByteBuffer data = BufferUtils.createByteBuffer(pixels.length);
        data.put(pixels).flip();
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_R8, width, height, 0, GL11.GL_RED,
                GL11.GL_UNSIGNED_BYTE, data);

        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, alignment);
        GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, rowLength);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, skipRows);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, skipPixels);
        GL11.glPixelStorei(GL11.GL_UNPACK_SWAP_BYTES, swapBytes);
        GL11.glPixelStorei(GL11.GL_UNPACK_LSB_FIRST, lsbFirst);
        GL11.glPixelStorei(GL12.GL_UNPACK_IMAGE_HEIGHT, imageHeight);
        GL11.glPixelStorei(GL12.GL_UNPACK_SKIP_IMAGES, skipImages);
        if (unpackBuffer != 0) {
            GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, unpackBuffer);
        }

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        return texture;
    }
}
