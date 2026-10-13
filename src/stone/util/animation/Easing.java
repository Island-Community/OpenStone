package stone.util.animation;

import java.util.function.Function;

public enum Easing {
    LINEAR(d -> d),
    EASE_IN_QUAD(d -> d * d),
    EASE_OUT_QUAD(d -> d * (2.0 - d)),
    EASE_IN_OUT_QUAD(d -> {
        if (d < 0.5) {
            return 2.0 * d * d;
        }
        return -1.0 + (4.0 - 2.0 * d) * d;
    }),
    EASE_IN_CUBIC(d -> d * d * d),
    EASE_OUT_CUBIC(d -> {
        double d2 = d - 1.0;
        return d2 * d2 * d2 + 1.0;
    }),
    EASE_IN_OUT_CUBIC(d -> {
        if (d < 0.5) {
            return 4.0 * d * d * d;
        }
        return (d - 1.0) * (2.0 * d - 2.0) * (2.0 * d - 2.0) + 1.0;
    }),
    EASE_IN_QUART(d -> d * d * d * d),
    EASE_OUT_QUART(d -> {
        double d2 = d - 1.0;
        return 1.0 - d2 * d2 * d2 * d2;
    }),
    EASE_IN_OUT_QUART(d -> {
        if (d < 0.5) {
            return 8.0 * d * d * d * d;
        }
        double d3 = d - 1.0;
        return 1.0 - 8.0 * d3 * d3 * d3 * d3;
    }),
    EASE_IN_QUINT(d -> d * d * d * d * d),
    EASE_OUT_QUINT(d -> {
        double d2 = d - 1.0;
        return 1.0 + d2 * d2 * d2 * d2 * d2;
    }),
    EASE_IN_OUT_QUINT(d -> {
        if (d < 0.5) {
            return 16.0 * d * d * d * d * d;
        }
        double d3 = d - 1.0;
        return 1.0 + 16.0 * d3 * d3 * d3 * d3 * d3;
    }),
    EASE_IN_SINE(d -> 1.0 - Math.cos(d * Math.PI / 2.0)),
    EASE_OUT_SINE(d -> Math.sin(d * Math.PI / 2.0)),
    EASE_IN_OUT_SINE(d -> 1.0 - Math.cos(Math.PI * d / 2.0)),
    EASE_IN_EXPO(d -> {
        if (d == 0.0) {
            return 0.0;
        }
        return Math.pow(2.0, 10.0 * d - 10.0);
    }),
    EASE_OUT_EXPO(d -> {
        if (d == 1.0) {
            return 1.0;
        }
        return 1.0 - Math.pow(2.0, -10.0 * d);
    }),
    EASE_IN_OUT_EXPO(d -> {
        if (d == 0.0) {
            return 0.0;
        }
        if (d == 1.0) {
            return 1.0;
        }
        if (d < 0.5) {
            return Math.pow(2.0, 20.0 * d - 10.0) / 2.0;
        }
        return (2.0 - Math.pow(2.0, -20.0 * d + 10.0)) / 2.0;
    }),
    EASE_IN_CIRC(d -> 1.0 - Math.sqrt(1.0 - d * d)),
    EASE_OUT_CIRC(d -> {
        double d2 = d - 1.0;
        return Math.sqrt(1.0 - d2 * d2);
    }),
    EASE_IN_OUT_CIRC(d -> {
        if (d < 0.5) {
            return (1.0 - Math.sqrt(1.0 - 4.0 * d * d)) / 2.0;
        }
        return (Math.sqrt(1.0 - 4.0 * (d - 1.0) * d) + 1.0) / 2.0;
    }),
    SIGMOID(d -> 1.0 / (1.0 + Math.exp(-d))),
    EASE_OUT_ELASTIC(d -> {
        if (d == 0.0) {
            return 0.0;
        }
        if (d == 1.0) {
            return 1.0;
        }
        return Math.pow(2.0, -10.0 * d) * Math.sin((d * 10.0 - 0.75) * 2.0943951023931953) * 0.5 + 1.0;
    }),
    EASE_IN_BACK(d -> 2.70158 * d * d * d - 1.70158 * d * d);

    private final Function<Double, Double> function;

    Easing(Function<Double, Double> function) {
        this.function = function;
    }

    public Function<Double, Double> getFunction() {
        return this.function;
    }
}
