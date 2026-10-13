package stone.render;

import stone.core.Color;
import stone.core.Log;
import stone.core.Mat4;
import stone.core.Rect;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;
import org.lwjgl.opengl.GLContext;

public final class Renderer {
    private static final String SCOPE = "render";
    private static final Renderer INSTANCE = new Renderer();

    private static final int MAX_QUADS = 24576;

    private static final float ATLAS_PIXEL_HEIGHT = 96.0f;
    private static final int VERTEX_FLOATS = 16;
    private static final int QUAD_FLOATS = VERTEX_FLOATS * 4;
    private static final int STYLE_OFFSET = 8;

    private static final int MODE_FILL = 0;
    private static final int MODE_STROKE = 1;
    private static final int MODE_TEXT = 2;
    private static final int MODE_ICON = 3;
    private static final int MODE_IMAGE = 4;
    private static final int MODE_SHADOW = 5;
    private static final int MODE_GLOW = 6;
    private static final int MODE_RING = 7;
    private static final int MODE_SWEEP = 8;
    private static final int MODE_LINE = 9;
    private static final int MODE_TEXT_BLOOM = 10;

    private static final float TEXT_BLOOM_SOFTNESS = 0.30f;

    private final FontAtlas fonts = new FontAtlas();
    private final IconAtlas icons = new IconAtlas();
    private final Shader ui = new Shader();
    private final float[] vertices = new float[MAX_QUADS * QUAD_FLOATS];
    private final FloatBuffer vertexData = BufferUtils.createFloatBuffer(MAX_QUADS * QUAD_FLOATS);
    private final List<Rect> clipStack = new ArrayList<>();
    private int quadCount;
    private int vertexArray;
    private int vertexBuffer;
    private int indexBuffer;
    private int crispSampler;
    private boolean samplers;
    private int boundImage;
    private boolean boundCrisp;
    private boolean ready;
    private int width;
    private int height;

    private Renderer() {}

    public static Renderer instance() {
        return INSTANCE;
    }

    public void prepare() {
        fonts.build(ATLAS_PIXEL_HEIGHT);
        icons.build();
    }

    public boolean createResources() {
        if (!ui.compile("stone.ui", Shaders.UI_VERTEX, Shaders.UI_FRAGMENT)) {
            return false;
        }
        fonts.upload();
        icons.upload();

        vertexArray = GL30.glGenVertexArrays();
        vertexBuffer = GL15.glGenBuffers();
        indexBuffer = GL15.glGenBuffers();

        GL30.glBindVertexArray(vertexArray);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertexBuffer);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (long) MAX_QUADS * QUAD_FLOATS * Float.BYTES, GL15.GL_STREAM_DRAW);
        int stride = VERTEX_FLOATS * Float.BYTES;
        int[] components = {2, 2, 4, 4, 4};
        long offset = 0;
        for (int index = 0; index < components.length; ++index) {
            GL20.glEnableVertexAttribArray(index);
            GL20.glVertexAttribPointer(index, components[index], GL11.GL_FLOAT, false, stride, offset);
            offset += (long) components[index] * Float.BYTES;
        }

        IntBuffer indices = BufferUtils.createIntBuffer(MAX_QUADS * 6);
        for (int quad = 0; quad < MAX_QUADS; ++quad) {
            int base = quad * 4;
            indices.put(base).put(base + 1).put(base + 2).put(base).put(base + 2).put(base + 3);
        }
        indices.flip();
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, indexBuffer);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indices, GL15.GL_STATIC_DRAW);

        GL30.glBindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);

        samplers = GLContext.getCapabilities().OpenGL33;
        if (samplers) {
            crispSampler = GL33.glGenSamplers();
            GL33.glSamplerParameteri(crispSampler, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL33.glSamplerParameteri(crispSampler, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        }
        Gl.checkError("renderer setup");
        ready = true;
        Log.info(SCOPE, "renderer ready (samplers %s)", samplers ? "on" : "off");
        return true;
    }

    public boolean resourcesValid() {
        return ready && GL11.glIsTexture(fonts.texture()) && GL11.glIsTexture(icons.texture()) && ui.valid();
    }

    public void destroy() {
        ui.destroy();
        fonts.destroy();
        icons.destroy();
        if (crispSampler != 0) {
            GL33.glDeleteSamplers(crispSampler);
            crispSampler = 0;
        }
        if (vertexBuffer != 0) {
            GL15.glDeleteBuffers(vertexBuffer);
        }
        if (indexBuffer != 0) {
            GL15.glDeleteBuffers(indexBuffer);
        }
        if (vertexArray != 0) {
            GL30.glDeleteVertexArrays(vertexArray);
        }
        vertexBuffer = indexBuffer = vertexArray = 0;
        ready = false;
    }

    public boolean ready() {
        return ready;
    }

    public void beginFrame(int frameWidth, int frameHeight, float time) {
        width = frameWidth;
        height = frameHeight;
        quadCount = 0;
        clipStack.clear();
        boundImage = 0;
        boundCrisp = false;

        GL30.glBindVertexArray(vertexArray);
        ui.bind();
        ui.set("uProjection", Mat4.orthographic(0.0f, frameWidth, frameHeight, 0.0f));
        ui.set("uTime", time);
        ui.set("uFontAtlas", 0);
        ui.set("uIconAtlas", 1);
        ui.set("uImage", 2);

        ui.set("uFontAtlasTexel", fonts.texelWidth(), fonts.texelHeight());
        ui.set("uIconAtlasTexel", icons.texelWidth(), icons.texelHeight());

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, fonts.texture());
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, icons.texture());
        GL13.glActiveTexture(GL13.GL_TEXTURE2);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        if (samplers) {

            GL33.glBindSampler(0, 0);
            GL33.glBindSampler(1, 0);
            GL33.glBindSampler(2, 0);
        }

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glDepthMask(false);
        GL11.glColorMask(true, true, true, true);
        GL11.glEnable(GL11.GL_BLEND);
        GL14.glBlendEquation(GL14.GL_FUNC_ADD);
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE,
                GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0.0, frameWidth, frameHeight, 0.0, -1.0, 1.0);
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GL11.glLoadIdentity();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glViewport(0, 0, frameWidth, frameHeight);

        Gl.disableClientArrays(3);
    }

    public void endFrame() {
        flush();
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    public void flush() {
        if (quadCount == 0) {
            return;
        }
        vertexData.clear();
        vertexData.put(vertices, 0, quadCount * QUAD_FLOATS).flip();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertexBuffer);

        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (long) MAX_QUADS * QUAD_FLOATS * Float.BYTES, GL15.GL_STREAM_DRAW);
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, vertexData);
        GL11.glDrawElements(GL11.GL_TRIANGLES, quadCount * 6, GL11.GL_UNSIGNED_INT, 0L);
        quadCount = 0;
    }

    public void pause() {
        flush();
        GL20.glUseProgram(0);
        GL30.glBindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
    }

    public void resume() {
        GL30.glBindVertexArray(vertexArray);
        ui.bind();
        ui.set("uProjection", Mat4.orthographic(0.0f, width, height, 0.0f));
        ui.set("uFontAtlas", 0);
        ui.set("uIconAtlas", 1);
        ui.set("uImage", 2);
        ui.set("uFontAtlasTexel", fonts.texelWidth(), fonts.texelHeight());
        ui.set("uIconAtlasTexel", icons.texelWidth(), icons.texelHeight());

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, fonts.texture());
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, icons.texture());
        GL13.glActiveTexture(GL13.GL_TEXTURE2);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, boundImage);
        if (samplers) {
            GL33.glBindSampler(0, 0);
            GL33.glBindSampler(1, 0);
            GL33.glBindSampler(2, 0);
        }

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDepthMask(false);
        GL11.glColorMask(true, true, true, true);
        GL11.glEnable(GL11.GL_BLEND);
        GL14.glBlendEquation(GL14.GL_FUNC_ADD);
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE,
                GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0.0, width, height, 0.0, -1.0, 1.0);
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GL11.glLoadIdentity();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glViewport(0, 0, width, height);
        Gl.disableClientArrays(3);

        applyClip();
    }

    private void applyClip() {
        if (clipStack.isEmpty()) {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            return;
        }
        Rect bounds = clipStack.get(clipStack.size() - 1);
        int x = (int) Math.floor(bounds.x());
        int y = (int) Math.floor(bounds.y());
        int w = (int) Math.ceil(bounds.width());
        int h = (int) Math.ceil(bounds.height());
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x, height - (y + h), Math.max(w, 0), Math.max(h, 0));
    }

    public void pushClip(Rect bounds) {
        flush();
        Rect clipped = bounds;
        if (!clipStack.isEmpty()) {
            Rect parent = clipStack.get(clipStack.size() - 1);
            float left = Math.max(clipped.x(), parent.x());
            float top = Math.max(clipped.y(), parent.y());
            float right = Math.min(clipped.right(), parent.right());
            float bottom = Math.min(clipped.bottom(), parent.bottom());
            clipped = new Rect(left, top, Math.max(0.0f, right - left), Math.max(0.0f, bottom - top));
        }
        clipStack.add(clipped);
        applyClip();
    }

    public void popClip() {
        flush();
        if (!clipStack.isEmpty()) {
            clipStack.remove(clipStack.size() - 1);
        }
        applyClip();
    }

    private void quad(Rect bounds, Rect sdf, float radius, float spread, int mode, float param, Color c0, Color c1,
            Color c2, Color c3, float u0, float v0, float u1, float v1) {
        float[] corners = {bounds.x(), bounds.y(), bounds.right(), bounds.y(), bounds.right(), bounds.bottom(),
            bounds.x(), bounds.bottom()};
        submit(corners, sdf, radius, spread, mode, param, c0, c1, c2, c3, u0, v0, u1, v1);
    }

    private void quad(Rect bounds, float radius, float spread, int mode, float param, Color color) {
        quad(bounds, bounds, radius, spread, mode, param, color, color, color, color, 0.0f, 0.0f, 0.0f, 0.0f);
    }

    private void submit(float[] corners, Rect sdf, float radius, float spread, int mode, float param, Color c0,
            Color c1, Color c2, Color c3, float u0, float v0, float u1, float v1) {
        if (quadCount >= MAX_QUADS) {
            flush();
        }
        int base = quadCount * QUAD_FLOATS;
        corner(base, corners[0], corners[1], u0, v0, c0);
        corner(base + VERTEX_FLOATS, corners[2], corners[3], u1, v0, c1);
        corner(base + VERTEX_FLOATS * 2, corners[4], corners[5], u1, v1, c2);
        corner(base + VERTEX_FLOATS * 3, corners[6], corners[7], u0, v1, c3);
        int style = base + STYLE_OFFSET;
        vertices[style] = sdf.centerX();
        vertices[style + 1] = sdf.centerY();
        vertices[style + 2] = sdf.width() * 0.5f;
        vertices[style + 3] = sdf.height() * 0.5f;
        vertices[style + 4] = radius;
        vertices[style + 5] = spread;
        vertices[style + 6] = mode;
        vertices[style + 7] = param;
        for (int vertex = 1; vertex < 4; ++vertex) {
            System.arraycopy(vertices, style, vertices, style + vertex * VERTEX_FLOATS, VERTEX_FLOATS - STYLE_OFFSET);
        }
        ++quadCount;
    }

    private void corner(int offset, float x, float y, float u, float v, Color color) {
        vertices[offset] = x;
        vertices[offset + 1] = y;
        vertices[offset + 2] = u;
        vertices[offset + 3] = v;
        vertices[offset + 4] = color.r();
        vertices[offset + 5] = color.g();
        vertices[offset + 6] = color.b();
        vertices[offset + 7] = color.a();
    }

    public void rect(Rect bounds, Color color) {
        rect(bounds, color, 0.0f);
    }

    public void rect(Rect bounds, Color color, float radius) {
        quad(bounds, radius, 0.0f, MODE_FILL, 0.0f, color);
    }

    public void rectVertical(Rect bounds, Color top, Color bottom, float radius) {
        quad(bounds, bounds, radius, 0.0f, MODE_FILL, 0.0f, top, top, bottom, bottom, 0.0f, 0.0f, 0.0f, 0.0f);
    }

    public void rectHorizontal(Rect bounds, Color left, Color right, float radius) {
        quad(bounds, bounds, radius, 0.0f, MODE_FILL, 0.0f, left, right, right, left, 0.0f, 0.0f, 0.0f, 0.0f);
    }

    public void outline(Rect bounds, Color color, float strokeWidth, float radius) {
        quad(bounds.inset(-strokeWidth), bounds, radius, 0.0f, MODE_STROKE, strokeWidth, color, color, color, color,
                0.0f, 0.0f, 0.0f, 0.0f);
    }

    public void sweepOutline(Rect bounds, Color color, float strokeWidth, float radius) {
        quad(bounds.inset(-strokeWidth), bounds, radius, 0.0f, MODE_SWEEP, strokeWidth, color, color, color, color,
                0.0f, 0.0f, 0.0f, 0.0f);
    }

    public void shadow(Rect bounds, Color color, float spread, float radius) {
        quad(bounds.inset(-spread * 1.6f), bounds, radius, spread, MODE_SHADOW, 0.0f, color, color, color, color,
                0.0f, 0.0f, 0.0f, 0.0f);
    }

    public void glow(Rect bounds, Color color, float spread, float radius) {
        quad(bounds.inset(-spread * 1.6f), bounds, radius, spread, MODE_GLOW, 0.0f, color, color, color, color,
                0.0f, 0.0f, 0.0f, 0.0f);
    }

    public void ring(float centreX, float centreY, float radius, float thickness, float progress, Color color) {
        quad(new Rect(centreX - radius, centreY - radius, radius * 2.0f, radius * 2.0f), radius, progress, MODE_RING,
                thickness, color);
    }

    public void line(float x0, float y0, float x1, float y1, float thickness, Color color) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 0.0001f) {
            return;
        }
        float half = thickness * 0.5f;
        float normalX = -dy / length * half;
        float normalY = dx / length * half;
        float[] corners = {x0 + normalX, y0 + normalY, x1 + normalX, y1 + normalY, x1 - normalX, y1 - normalY,
            x0 - normalX, y0 - normalY};

        submit(corners, new Rect(-half, -half, half * 2.0f, half * 2.0f), 0.0f, 0.0f, MODE_LINE, thickness, color,
                color, color, color, 0.0f, -1.0f, 1.0f, 1.0f);
    }

    public void face(float[] xs, float[] ys, Color color) {
        submit(polygon(xs, ys), polygonBox(xs, ys), 0.0f, 0.0f, MODE_FILL, 0.0f, color, color, color, color, 0.0f,
                0.0f, 0.0f, 0.0f);
    }

    private static float[] polygon(float[] xs, float[] ys) {
        return new float[] {xs[0], ys[0], xs[1], ys[1], xs[2], ys[2], xs[3], ys[3]};
    }

    private static Rect polygonBox(float[] xs, float[] ys) {
        float minX = xs[0];
        float minY = ys[0];
        float maxX = xs[0];
        float maxY = ys[0];
        for (int index = 1; index < 4; ++index) {
            minX = Math.min(minX, xs[index]);
            minY = Math.min(minY, ys[index]);
            maxX = Math.max(maxX, xs[index]);
            maxY = Math.max(maxY, ys[index]);
        }
        return new Rect(minX - 2.0f, minY - 2.0f, maxX - minX + 4.0f, maxY - minY + 4.0f);
    }

    private void bindImage(int texture, boolean crisp) {
        if (texture == boundImage && crisp == boundCrisp) {
            return;
        }
        flush();
        GL13.glActiveTexture(GL13.GL_TEXTURE2);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        if (samplers) {
            GL33.glBindSampler(2, crisp ? crispSampler : 0);
        }
        boundImage = texture;
        boundCrisp = crisp;
    }

    public void image(Rect bounds, Rect uv, int texture, Color tint, float radius) {
        bindImage(texture, false);
        quad(bounds, bounds, radius, 0.0f, MODE_IMAGE, 0.0f, tint, tint, tint, tint, uv.x(), uv.y(), uv.right(),
                uv.bottom());
    }

    public void imageCrisp(Rect bounds, Rect uv, int texture, Color tint, float radius) {
        bindImage(texture, true);
        quad(bounds, bounds, radius, 0.0f, MODE_IMAGE, 0.0f, tint, tint, tint, tint, uv.x(), uv.y(), uv.right(),
                uv.bottom());
    }

    public void imageFace(float[] xs, float[] ys, Rect uv, int texture, Color tint) {
        bindImage(texture, false);
        submit(polygon(xs, ys), polygonBox(xs, ys), 0.0f, 0.0f, MODE_IMAGE, 0.0f, tint, tint, tint, tint, uv.x(),
                uv.y(), uv.right(), uv.bottom());
    }

    public float measure(FontRole role, String value, float size) {
        return fonts.face(role).measure(value, size);
    }

    public float lineHeight(FontRole role, float size) {
        return fonts.face(role).lineHeight(size);
    }

    public String elide(FontRole role, String value, float size, float maxWidth) {
        if (maxWidth <= 0.0f) {
            return "";
        }
        if (measure(role, value, size) <= maxWidth) {
            return value;
        }
        float ellipsis = measure(role, "...", size);
        String trimmed = value;
        while (!trimmed.isEmpty() && measure(role, trimmed, size) + ellipsis > maxWidth) {
            trimmed = trimmed.substring(0, trimmed.offsetByCodePoints(trimmed.length(), -1));
        }
        return trimmed + "...";
    }

    public float text(FontRole role, String value, float x, float y, float size, Color color) {
        return text(role, value, x, y, size, color, Align.LEFT);
    }

    public float text(FontRole role, String value, float x, float y, float size, Color color, Align align) {
        FontFace face = fonts.face(role);
        if (!face.ready()) {
            return x;
        }

        float penX = Math.round(alignedStart(face, value, x, size, align));
        float baseline = Math.round(y + face.baseline(size));
        return glyphs(face, value, penX, baseline, size, color, MODE_TEXT, 0.0f, 0.0f);
    }

    public float textCap(FontRole role, String value, float x, float capTop, float size, Color color) {
        return textCap(role, value, x, capTop, size, color, Align.LEFT);
    }

    public float textCap(FontRole role, String value, float x, float capTop, float size, Color color, Align align) {
        return text(role, value, x, capTop - fonts.face(role).capTop(size), size, color, align);
    }

    public float textBloom(FontRole role, String value, float x, float capTop, float size, Color color, float spread,
            Align align) {
        FontFace face = fonts.face(role);
        if (!face.ready() || spread <= 0.0f) {
            return x;
        }
        float penX = Math.round(alignedStart(face, value, x, size, align));
        float baseline = Math.round(capTop - face.capTop(size) + face.baseline(size));

        return glyphs(face, value, penX, baseline, size, color, MODE_TEXT_BLOOM, TEXT_BLOOM_SOFTNESS, spread);
    }

    private static float alignedStart(FontFace face, String value, float x, float size, Align align) {
        return switch (align) {
            case LEFT -> x;
            case CENTER -> x - face.measure(value, size) * 0.5f;
            case RIGHT -> x - face.measure(value, size);
        };
    }

    private float glyphs(FontFace face, String value, float penX, float baseline, float size, Color color, int mode,
            float param, float spread) {
        float scale = face.scaleFor(size);
        for (int index = 0; index < value.length(); ) {
            int codepoint = value.codePointAt(index);
            index += Character.charCount(codepoint);
            FontFace.Glyph glyph = face.glyph(codepoint);
            if (glyph == null) {
                continue;
            }
            if (glyph.width() > 0.0f && glyph.height() > 0.0f) {
                Rect tile = new Rect(penX + glyph.offsetX() * scale, baseline + glyph.offsetY() * scale,
                        glyph.width() * scale, glyph.height() * scale).inset(-spread);
                quad(tile, tile, 0.0f, 0.0f, mode, param, color, color, color, color, glyph.u0(), glyph.v0(),
                        glyph.u1(), glyph.v1());
            }
            penX += glyph.advance() * scale;
        }
        return penX;
    }

    public void icon(Icon icon, Rect bounds, Color color) {
        IconAtlas.Tile tile = icons.tile(icon);
        quad(bounds, bounds, 0.0f, 0.0f, MODE_ICON, 0.0f, color, color, color, color, tile.u0(), tile.v0(),
                tile.u1(), tile.v1());
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public FontAtlas fonts() {
        return fonts;
    }

    public IconAtlas icons() {
        return icons;
    }
}
