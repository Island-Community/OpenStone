package stone.core;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Locale;

public final class Log {
    public enum Level {
        TRACE("TRACE"), DEBUG("DEBUG"), INFO("INFO "), WARN("WARN "), ERROR("ERROR");

        final String tag;

        Level(String tag) {
            this.tag = tag;
        }
    }

    private static FileOutputStream out;
    private static long epochNanos = System.nanoTime();
    private static volatile Level minimum = Level.DEBUG;

    private Log() {}

    public static synchronized void open(Path file, long epoch) throws IOException {
        epochNanos = epoch;
        out = new FileOutputStream(file.toFile(), true);
    }

    public static synchronized void close() {
        if (out == null) {
            return;
        }
        try {
            out.close();
        } catch (IOException ignored) {

        }
        out = null;
    }

    public static void trace(String scope, String format, Object... args) {
        write(Level.TRACE, scope, format, args);
    }

    public static void debug(String scope, String format, Object... args) {
        write(Level.DEBUG, scope, format, args);
    }

    public static void info(String scope, String format, Object... args) {
        write(Level.INFO, scope, format, args);
    }

    public static void warn(String scope, String format, Object... args) {
        write(Level.WARN, scope, format, args);
    }

    public static void error(String scope, String format, Object... args) {
        write(Level.ERROR, scope, format, args);
    }

    public static void error(String scope, Throwable thrown, String format, Object... args) {
        StringWriter trace = new StringWriter();
        thrown.printStackTrace(new PrintWriter(trace));
        write(Level.ERROR, scope, format, args);
        writeLine(Level.ERROR, scope, trace.toString().stripTrailing().replace("\n", "\n    "));
    }

    private static void write(Level level, String scope, String format, Object... args) {
        if (level.ordinal() < minimum.ordinal()) {
            return;
        }
        writeLine(level, scope, args.length == 0 ? format : String.format(Locale.ROOT, format, args));
    }

    private static synchronized void writeLine(Level level, String scope, String text) {
        long micros = (System.nanoTime() - epochNanos) / 1000;
        String line = String.format(Locale.ROOT, "[%7d.%03d] %s [%s] %s\r\n",
                micros / 1_000_000, (micros / 1000) % 1000, level.tag, scope, text);
        if (out == null) {
            return;
        }
        try {
            out.write(line.getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) {

        }
    }
}
