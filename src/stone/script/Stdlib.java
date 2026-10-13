package stone.script;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.luaj.vm2.Buffer;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

final class Stdlib {
    private static final long MAX_UTF = 0x7FFFFFFFL;
    private static final long MAX_UNICODE = 0x10FFFFL;
    private static final String INVALID_UTF8 = "invalid UTF-8 code";
    private static final long[] UTF8_LIMITS = {~0L, 0x80, 0x800, 0x10000, 0x200000, 0x4000000};

    private Stdlib() {}

    static void install(Globals globals) {
        globals.get("string").set("format", Lua.fn(Stdlib::format));
        globals.set("utf8", utf8());
        globals.set("tostring", Lua.fn(args -> LuaValue.valueOf(Lua.tostring(args.checkvalue(1)))));
    }

    static String number(double value) {
        if (value == Math.rint(value) && value >= -0x1p63 && value < 0x1p63) {
            return Long.toString((long) value);
        }
        return new Spec("", -1, 14, false, "%.14g").decimal(value, 'g');
    }

    private static Varargs format(Varargs args) {
        LuaString spec = args.checkstring(1);
        Buffer out = new Buffer(spec.m_length);
        int argument = 1;
        int i = 0;
        while (i < spec.m_length) {
            int c = spec.luaByte(i++);
            if (c != '%') {
                out.append((byte) c);
                continue;
            }
            if (i < spec.m_length && spec.luaByte(i) == '%') {
                out.append((byte) '%');
                i++;
                continue;
            }
            int start = i;
            while (i < spec.m_length && "-+ #0".indexOf(spec.luaByte(i)) >= 0) {
                i++;
            }
            String flags = spec.substring(start, i).tojstring();
            int width = -1;
            int precision = -1;
            int digits = 0;
            while (i < spec.m_length && Character.isDigit(spec.luaByte(i)) && digits < 3) {
                width = Math.max(width, 0) * 10 + spec.luaByte(i++) - '0';
                digits++;
            }
            if (i < spec.m_length && spec.luaByte(i) == '.') {
                i++;
                precision = 0;
                digits = 0;
                while (i < spec.m_length && Character.isDigit(spec.luaByte(i)) && digits < 3) {
                    precision = precision * 10 + spec.luaByte(i++) - '0';
                    digits++;
                }
            }
            if (i >= spec.m_length) {
                throw Lua.error("invalid conversion '%%%s' to 'format'", spec.substring(start, i).tojstring());
            }
            int conversion = spec.luaByte(i++);
            String form = "%" + spec.substring(start, i).tojstring();
            boolean modified = i - start > 1;
            if (++argument > args.narg()) {
                throw new LuaError("bad argument #" + argument + " to 'format' (no value)");
            }
            Spec s = new Spec(flags, width, precision, Character.toLowerCase(conversion) != conversion, form);
            switch (conversion) {
                case 'c' -> {
                    s.check("-", false);
                    padded(out, LuaString.valueOf(new byte[] {(byte) checkInteger(args, argument)}), width, flags);
                }
                case 'd', 'i' -> {
                    s.check("-+ 0", true);
                    out.append(s.integer(checkInteger(args, argument), 10));
                }
                case 'u' -> {
                    s.check("-0", true);
                    out.append(s.integer(checkInteger(args, argument), 0));
                }
                case 'o' -> {
                    s.check("-#0", true);
                    out.append(s.integer(checkInteger(args, argument), 8));
                }
                case 'x', 'X' -> {
                    s.check("-#0", true);
                    out.append(s.integer(checkInteger(args, argument), 16));
                }
                case 'a', 'A' -> {
                    s.check("-+ #0", true);
                    out.append(s.hexFloat(args.checkdouble(argument)));
                }
                case 'e', 'E', 'f', 'F', 'g', 'G' -> {
                    s.check("-+ #0", true);
                    out.append(s.decimal(args.checkdouble(argument), Character.toLowerCase(conversion)));
                }
                case 'p' -> {
                    s.check("-", false);
                    LuaValue value = args.arg(argument);
                    boolean reference = value.istable() || value.isfunction() || value.isuserdata() || value.isthread();
                    out.append(s.pad(reference ? "0x" + Integer.toHexString(System.identityHashCode(value))
                            : "(null)", false));
                }
                case 'q' -> {
                    if (modified) {
                        throw new LuaError("specifier '%q' cannot have modifiers");
                    }
                    quote(out, args, argument);
                }
                case 's' -> {
                    LuaValue value = args.arg(argument);
                    LuaString text = value.type() == LuaValue.TSTRING ? value.checkstring()
                            : LuaValue.valueOf(Lua.tostring(value));
                    if (!modified || (precision < 0 && text.m_length >= 100)) {
                        out.append(text);
                    } else {
                        s.check("-", true);
                        padded(out, precision >= 0 && precision < text.m_length ? text.substring(0, precision) : text,
                                width, flags);
                    }
                }
                default -> throw Lua.error("invalid conversion '%s' to 'format'", form);
            }
        }
        return out.tostring();
    }

    private static void padded(Buffer out, LuaString text, int width, String flags) {
        String padding = " ".repeat(Math.max(0, width - text.m_length));
        boolean left = flags.contains("-");
        if (!left) {
            out.append(padding);
        }
        out.append(text);
        if (left) {
            out.append(padding);
        }
    }

    private static long checkInteger(Varargs args, int index) {
        double value = args.checkdouble(index);
        if (value != Math.rint(value) || value < -0x1p63 || value >= 0x1p63) {
            throw new LuaError("bad argument #" + index + " to 'format' (number has no integer representation)");
        }
        return (long) value;
    }

    private static void quote(Buffer out, Varargs args, int index) {
        LuaValue value = args.arg(index);
        if (value.type() == LuaValue.TSTRING) {
            LuaString text = value.checkstring();
            out.append((byte) '"');
            for (int i = 0; i < text.m_length; i++) {
                int c = text.luaByte(i);
                if (c == '"' || c == '\\' || c == '\n') {
                    out.append((byte) '\\');
                    out.append((byte) c);
                } else if (c < 32 || c == 127) {
                    boolean digitFollows = i + 1 < text.m_length && Character.isDigit(text.luaByte(i + 1));
                    out.append(digitFollows ? String.format(Locale.ROOT, "\\%03d", c) : "\\" + c);
                } else {
                    out.append((byte) c);
                }
            }
            out.append((byte) '"');
        } else if (value.type() == LuaValue.TNUMBER) {
            double number = value.todouble();
            if (number == Math.rint(number) && number >= -0x1p63 && number < 0x1p63) {
                long integer = (long) number;
                out.append(integer == Long.MIN_VALUE ? "0x8000000000000000" : Long.toString(integer));
            } else if (Double.isNaN(number)) {
                out.append("(0/0)");
            } else if (Double.isInfinite(number)) {
                out.append(number > 0 ? "1e9999" : "-1e9999");
            } else {
                out.append(new Spec("", -1, -1, false, "%a").hexFloat(number));
            }
        } else if (value.isnil() || value.isboolean()) {
            out.append(value.tojstring());
        } else {
            throw new LuaError("bad argument #" + index + " to 'format' (value has no literal form)");
        }
    }

    private record Spec(String flags, int width, int precision, boolean upper, String form) {
        void check(String allowed, boolean takesPrecision) {
            for (int i = 0; i < flags.length(); i++) {
                if (allowed.indexOf(flags.charAt(i)) < 0) {
                    throw invalid();
                }
            }
            if (width > 99 || precision > 99 || (!takesPrecision && precision >= 0)) {
                throw invalid();
            }
        }

        private LuaError invalid() {
            return Lua.error("invalid conversion specification: '%s'", form);
        }

        private boolean has(char flag) {
            return flags.indexOf(flag) >= 0;
        }

        String pad(String sign, String body, boolean zeroAllowed) {
            int padding = width - sign.length() - body.length();
            if (padding <= 0) {
                return sign + body;
            }
            if (has('-')) {
                return sign + body + " ".repeat(padding);
            }
            if (zeroAllowed && has('0')) {
                return sign + "0".repeat(padding) + body;
            }
            return " ".repeat(padding) + sign + body;
        }

        String pad(String body, boolean zeroAllowed) {
            return pad("", body, zeroAllowed);
        }

        private String sign(boolean negative) {
            return negative ? "-" : has('+') ? "+" : has(' ') ? " " : "";
        }

        String integer(long value, int radix) {
            String digits = switch (radix) {
                case 10 -> value == Long.MIN_VALUE ? "9223372036854775808" : Long.toString(Math.abs(value));
                case 0 -> Long.toUnsignedString(value);
                default -> Long.toUnsignedString(value, radix);
            };
            if (upper) {
                digits = digits.toUpperCase(Locale.ROOT);
            }
            if (precision >= 0) {
                digits = precision == 0 && value == 0 ? "" : "0".repeat(Math.max(0, precision - digits.length())) + digits;
            }
            String prefix = radix == 10 ? sign(value < 0) : "";
            if (has('#') && value != 0 && radix == 16) {
                prefix = upper ? "0X" : "0x";
            } else if (has('#') && radix == 8 && !digits.startsWith("0")) {
                digits = "0" + digits;
            }
            return pad(prefix, digits, precision < 0);
        }

        String decimal(double value, int conversion) {
            boolean negative = Math.copySign(1.0, value) < 0;
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                String word = Double.isNaN(value) ? "nan" : "inf";
                return pad(sign(negative), upper ? word.toUpperCase(Locale.ROOT) : word, false);
            }
            double magnitude = Math.abs(value);
            int digits = precision < 0 ? 6 : precision;
            String body = switch (conversion) {
                case 'f' -> fixed(magnitude, digits);
                case 'e' -> scientific(magnitude, digits);
                default -> general(magnitude, precision < 0 ? 6 : Math.max(precision, 1));
            };
            return pad(sign(negative), upper ? body.toUpperCase(Locale.ROOT) : body, true);
        }

        private String fixed(double magnitude, int digits) {
            String text = new BigDecimal(magnitude).setScale(digits, RoundingMode.HALF_EVEN).toPlainString();
            return digits == 0 && has('#') ? text + "." : text;
        }

        private String scientific(double magnitude, int digits) {
            BigDecimal rounded = rounded(magnitude, digits + 1);
            String mantissa = rounded.unscaledValue().toString();
            mantissa += "0".repeat(digits + 1 - mantissa.length());
            int exponent = exponent(rounded);
            StringBuilder text = new StringBuilder().append(mantissa.charAt(0));
            if (digits > 0 || has('#')) {
                text.append('.').append(mantissa, 1, digits + 1);
            }
            text.append('e').append(exponent < 0 ? '-' : '+');
            String power = Integer.toString(Math.abs(exponent));
            return text.append(power.length() < 2 ? "0" + power : power).toString();
        }

        private String general(double magnitude, int significant) {
            int exponent = exponent(rounded(magnitude, significant));
            String text = significant > exponent && exponent >= -4
                    ? fixed(magnitude, significant - 1 - exponent)
                    : scientific(magnitude, significant - 1);
            if (has('#') || text.indexOf('.') < 0) {
                return text;
            }
            int e = text.indexOf('e');
            String mantissa = e < 0 ? text : text.substring(0, e);
            mantissa = mantissa.replaceAll("0+$", "");
            if (mantissa.endsWith(".")) {
                mantissa = mantissa.substring(0, mantissa.length() - 1);
            }
            return e < 0 ? mantissa : mantissa + text.substring(e);
        }

        private static BigDecimal rounded(double magnitude, int significant) {
            return new BigDecimal(magnitude).round(new MathContext(significant, RoundingMode.HALF_EVEN));
        }

        private static int exponent(BigDecimal rounded) {
            return rounded.signum() == 0 ? 0 : rounded.precision() - 1 - rounded.scale();
        }

        String hexFloat(double value) {
            boolean negative = Math.copySign(1.0, value) < 0;
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                String word = Double.isNaN(value) ? "nan" : "inf";
                return pad(sign(negative), upper ? word.toUpperCase(Locale.ROOT) : word, false);
            }
            long bits = Double.doubleToRawLongBits(value);
            int biased = (int) ((bits >>> 52) & 0x7FF);
            long fraction = bits & 0xFFFFFFFFFFFFFL;
            long lead = biased == 0 ? 0 : 1;
            int exponent = biased == 0 ? (fraction == 0 ? 0 : -1022) : biased - 1023;
            String hex;
            if (precision >= 0 && precision < 13) {
                int drop = (13 - precision) * 4;
                long remainder = fraction & ((1L << drop) - 1);
                long half = 1L << (drop - 1);
                fraction >>>= drop;
                if (remainder > half || (remainder == half && (fraction & 1) == 1)) {
                    fraction++;
                }
                if (fraction >>> (precision * 4) != 0) {
                    lead++;
                    fraction &= (1L << (precision * 4)) - 1;
                }
                hex = precision == 0 ? "" : String.format(Locale.ROOT, "%0" + precision + "x", fraction);
            } else {
                hex = String.format(Locale.ROOT, "%013x", fraction);
                hex = precision < 0 ? hex.replaceAll("0+$", "") : hex + "0".repeat(precision - 13);
            }
            String body = "0x" + lead + (hex.isEmpty() && !has('#') ? "" : "." + hex)
                    + "p" + (exponent < 0 ? "-" : "+") + Math.abs(exponent);
            return pad(sign(negative), upper ? body.toUpperCase(Locale.ROOT) : body, true);
        }
    }

    private static LuaTable utf8() {
        LuaTable utf8 = new LuaTable();
        utf8.set("charpattern", LuaString.valueOf(new byte[] {
            '[', 0, '-', 0x7F, (byte) 0xC2, '-', (byte) 0xFD, ']', '[', (byte) 0x80, '-', (byte) 0xBF, ']', '*',
        }));
        utf8.set("char", Lua.fn(Stdlib::utf8Char));
        utf8.set("codepoint", Lua.fn(Stdlib::utf8Codepoint));
        utf8.set("len", Lua.fn(Stdlib::utf8Len));
        utf8.set("offset", Lua.fn(Stdlib::utf8Offset));
        LuaValue strict = Lua.fn(args -> iterate(args, true));
        LuaValue lax = Lua.fn(args -> iterate(args, false));
        utf8.set("codes", Lua.fn(args -> {
            LuaString text = args.checkstring(1);
            if (continuation(text, 0)) {
                throw new LuaError("bad argument #1 to 'codes' (" + INVALID_UTF8 + ")");
            }
            return LuaValue.varargsOf(args.arg(2).toboolean() ? lax : strict, text, LuaValue.valueOf(0));
        }));
        return utf8;
    }

    private static boolean continuation(LuaString text, int index) {
        return index < text.m_length && (text.luaByte(index) & 0xC0) == 0x80;
    }

    private static long relative(long position, int length) {
        if (position >= 0) {
            return position;
        }
        return -position > length ? 0 : length + position + 1;
    }

    private static long[] decode(LuaString text, int index, boolean strict) {
        int c = text.luaByte(index);
        long code = 0;
        int count = 0;
        if (c < 0x80) {
            code = c;
        } else {
            for (; (c & 0x40) != 0; c <<= 1) {
                int at = index + ++count;
                if (at >= text.m_length || (text.luaByte(at) & 0xC0) != 0x80) {
                    return null;
                }
                code = (code << 6) | (text.luaByte(at) & 0x3F);
            }
            code |= (long) (c & 0x7F) << (count * 5);
            if (count > 5 || code > MAX_UTF || code < UTF8_LIMITS[count]) {
                return null;
            }
        }
        if (strict && (code > MAX_UNICODE || (code >= 0xD800 && code <= 0xDFFF))) {
            return null;
        }
        return new long[] {code, index + count + 1};
    }

    private static Varargs utf8Char(Varargs args) {
        Buffer out = new Buffer();
        for (int i = 1; i <= args.narg(); i++) {
            long code = args.checklong(i);
            if (code < 0 || code > MAX_UTF) {
                throw new LuaError("bad argument #" + i + " to 'char' (value out of range)");
            }
            if (code < 0x80) {
                out.append((byte) code);
                continue;
            }
            byte[] bytes = new byte[6];
            int n = 0;
            long mfb = 0x3F;
            do {
                bytes[5 - n++] = (byte) (0x80 | (code & 0x3F));
                code >>>= 6;
                mfb >>>= 1;
            } while (code > mfb);
            bytes[5 - n] = (byte) ((~mfb << 1) | code);
            out.append(LuaString.valueOf(bytes, 5 - n, n + 1));
        }
        return out.tostring();
    }

    private static Varargs utf8Codepoint(Varargs args) {
        LuaString text = args.checkstring(1);
        long first = relative(args.optlong(2, 1), text.m_length);
        long last = relative(args.optlong(3, first), text.m_length);
        boolean strict = !args.arg(4).toboolean();
        if (first < 1) {
            throw new LuaError("bad argument #2 to 'codepoint' (out of bounds)");
        }
        if (last > text.m_length) {
            throw new LuaError("bad argument #3 to 'codepoint' (out of bounds)");
        }
        List<LuaValue> codes = new ArrayList<>();
        for (int index = (int) first - 1; index < last;) {
            long[] decoded = decode(text, index, strict);
            if (decoded == null) {
                throw new LuaError(INVALID_UTF8);
            }
            codes.add(LuaValue.valueOf((double) decoded[0]));
            index = (int) decoded[1];
        }
        return LuaValue.varargsOf(codes.toArray(new LuaValue[0]));
    }

    private static Varargs utf8Len(Varargs args) {
        LuaString text = args.checkstring(1);
        long first = relative(args.optlong(2, 1), text.m_length);
        long last = relative(args.optlong(3, -1), text.m_length);
        boolean strict = !args.arg(4).toboolean();
        if (first < 1 || --first > text.m_length) {
            throw new LuaError("bad argument #2 to 'len' (initial position out of bounds)");
        }
        if (--last >= text.m_length) {
            throw new LuaError("bad argument #3 to 'len' (final position out of bounds)");
        }
        int count = 0;
        for (long index = first; index <= last;) {
            long[] decoded = decode(text, (int) index, strict);
            if (decoded == null) {
                return LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf(index + 1.0));
            }
            index = decoded[1];
            count++;
        }
        return LuaValue.valueOf(count);
    }

    private static Varargs utf8Offset(Varargs args) {
        LuaString text = args.checkstring(1);
        long n = args.checklong(2);
        long position = relative(args.optlong(3, n >= 0 ? 1 : text.m_length + 1), text.m_length);
        if (position < 1 || --position > text.m_length) {
            throw new LuaError("bad argument #3 to 'offset' (position out of bounds)");
        }
        int index = (int) position;
        if (n == 0) {
            while (index > 0 && continuation(text, index)) {
                index--;
            }
        } else {
            if (continuation(text, index)) {
                throw new LuaError("initial position is a continuation byte");
            }
            if (n < 0) {
                while (n < 0 && index > 0) {
                    do {
                        index--;
                    } while (index > 0 && continuation(text, index));
                    n++;
                }
            } else {
                n--;
                while (n > 0 && index < text.m_length) {
                    do {
                        index++;
                    } while (continuation(text, index));
                    n--;
                }
            }
        }
        return n == 0 ? LuaValue.valueOf(index + 1) : LuaValue.NIL;
    }

    private static Varargs iterate(Varargs args, boolean strict) {
        LuaString text = args.checkstring(1);
        int index = args.checkint(2);
        if (index < 0) {
            return LuaValue.NONE;
        }
        while (continuation(text, index)) {
            index++;
        }
        if (index >= text.m_length) {
            return LuaValue.NONE;
        }
        long[] decoded = decode(text, index, strict);
        if (decoded == null || continuation(text, (int) decoded[1])) {
            throw new LuaError(INVALID_UTF8);
        }
        return LuaValue.varargsOf(LuaValue.valueOf(index + 1), LuaValue.valueOf((double) decoded[0]));
    }
}
