package stone.render;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class Svg {
    private static final Pattern ELEMENT = Pattern.compile("<(path|circle|line|polyline|rect)\\b([^>]*)>");
    private static final Pattern ATTRIBUTE = Pattern.compile("([a-zA-Z0-9-]+)\\s*=\\s*\"([^\"]*)\"");

    private Svg() {}

    static void paint(String source, Graphics2D graphics) {
        Matcher element = ELEMENT.matcher(source);
        while (element.find()) {
            Map<String, String> attributes = new HashMap<>();
            Matcher attribute = ATTRIBUTE.matcher(element.group(2));
            while (attribute.find()) {
                attributes.put(attribute.group(1), attribute.group(2));
            }
            Shape shape = shape(element.group(1), attributes);
            String fill = attributes.get("fill");
            if (fill != null && !fill.equals("none")) {
                graphics.fill(shape);
            }
            graphics.draw(shape);
        }
    }

    private static Shape shape(String kind, Map<String, String> attributes) {
        return switch (kind) {
            case "path" -> path(attributes.get("d"));
            case "circle" -> {
                double radius = number(attributes, "r");
                yield new Ellipse2D.Double(number(attributes, "cx") - radius, number(attributes, "cy") - radius,
                        radius * 2.0, radius * 2.0);
            }
            case "line" -> new Line2D.Double(number(attributes, "x1"), number(attributes, "y1"),
                    number(attributes, "x2"), number(attributes, "y2"));
            case "polyline" -> {
                Path2D.Double polyline = new Path2D.Double();
                Tokens tokens = new Tokens(attributes.get("points"));
                polyline.moveTo(tokens.number(), tokens.number());
                while (tokens.more()) {
                    polyline.lineTo(tokens.number(), tokens.number());
                }
                yield polyline;
            }
            default -> {
                double x = number(attributes, "x");
                double y = number(attributes, "y");
                double width = number(attributes, "width");
                double height = number(attributes, "height");

                String rx = attributes.getOrDefault("rx", attributes.get("ry"));
                String ry = attributes.getOrDefault("ry", rx);
                if (rx == null) {
                    yield new Rectangle2D.Double(x, y, width, height);
                }
                yield new RoundRectangle2D.Double(x, y, width, height, Double.parseDouble(rx) * 2.0,
                        Double.parseDouble(ry) * 2.0);
            }
        };
    }

    private static double number(Map<String, String> attributes, String name) {
        String value = attributes.get(name);
        return value != null ? Double.parseDouble(value) : 0.0;
    }

    private static Path2D path(String data) {
        Path2D.Double path = new Path2D.Double();
        Tokens tokens = new Tokens(data);
        char command = 0;
        char previous = 0;
        double x = 0.0;
        double y = 0.0;
        double startX = 0.0;
        double startY = 0.0;
        double controlX = 0.0;
        double controlY = 0.0;
        while (tokens.more()) {
            if (tokens.atCommand()) {
                command = tokens.command();
            } else if (command == 0) {
                throw new IllegalArgumentException("path data starts without a command: " + data);
            }
            boolean relative = Character.isLowerCase(command);
            double baseX = relative ? x : 0.0;
            double baseY = relative ? y : 0.0;
            char upper = Character.toUpperCase(command);
            switch (upper) {
                case 'M' -> {
                    x = baseX + tokens.number();
                    y = baseY + tokens.number();
                    path.moveTo(x, y);
                    startX = x;
                    startY = y;

                    command = relative ? 'l' : 'L';
                }
                case 'L' -> {
                    x = baseX + tokens.number();
                    y = baseY + tokens.number();
                    path.lineTo(x, y);
                }
                case 'H' -> {
                    x = baseX + tokens.number();
                    path.lineTo(x, y);
                }
                case 'V' -> {
                    y = baseY + tokens.number();
                    path.lineTo(x, y);
                }
                case 'C', 'S' -> {
                    double firstX;
                    double firstY;
                    if (upper == 'C') {
                        firstX = baseX + tokens.number();
                        firstY = baseY + tokens.number();
                    } else if (previous == 'C' || previous == 'S') {
                        firstX = 2.0 * x - controlX;
                        firstY = 2.0 * y - controlY;
                    } else {
                        firstX = x;
                        firstY = y;
                    }
                    controlX = baseX + tokens.number();
                    controlY = baseY + tokens.number();
                    x = baseX + tokens.number();
                    y = baseY + tokens.number();
                    path.curveTo(firstX, firstY, controlX, controlY, x, y);
                }
                case 'Q', 'T' -> {
                    if (upper == 'Q') {
                        controlX = baseX + tokens.number();
                        controlY = baseY + tokens.number();
                    } else if (previous == 'Q' || previous == 'T') {
                        controlX = 2.0 * x - controlX;
                        controlY = 2.0 * y - controlY;
                    } else {
                        controlX = x;
                        controlY = y;
                    }
                    x = baseX + tokens.number();
                    y = baseY + tokens.number();
                    path.quadTo(controlX, controlY, x, y);
                }
                case 'A' -> {
                    double radiusX = tokens.number();
                    double radiusY = tokens.number();
                    double rotation = tokens.number();
                    boolean large = tokens.flag();
                    boolean sweep = tokens.flag();
                    double endX = baseX + tokens.number();
                    double endY = baseY + tokens.number();
                    arc(path, x, y, radiusX, radiusY, rotation, large, sweep, endX, endY);
                    x = endX;
                    y = endY;
                }
                case 'Z' -> {
                    path.closePath();
                    x = startX;
                    y = startY;
                }
                default -> throw new IllegalArgumentException("unsupported path command " + command);
            }
            previous = upper;
        }
        return path;
    }

    private static void arc(Path2D path, double x0, double y0, double radiusX, double radiusY, double rotation,
            boolean large, boolean sweep, double x, double y) {
        if (x0 == x && y0 == y) {
            return;
        }
        if (radiusX == 0.0 || radiusY == 0.0) {
            path.lineTo(x, y);
            return;
        }
        double rx = Math.abs(radiusX);
        double ry = Math.abs(radiusY);
        double phi = Math.toRadians(rotation);
        double cos = Math.cos(phi);
        double sin = Math.sin(phi);
        double halfX = (x0 - x) / 2.0;
        double halfY = (y0 - y) / 2.0;
        double primeX = cos * halfX + sin * halfY;
        double primeY = -sin * halfX + cos * halfY;

        double lambda = (primeX * primeX) / (rx * rx) + (primeY * primeY) / (ry * ry);
        if (lambda > 1.0) {
            rx *= Math.sqrt(lambda);
            ry *= Math.sqrt(lambda);
        }
        double numerator = rx * rx * ry * ry - rx * rx * primeY * primeY - ry * ry * primeX * primeX;
        double denominator = rx * rx * primeY * primeY + ry * ry * primeX * primeX;
        double coefficient = Math.sqrt(Math.max(0.0, numerator / denominator));
        if (large == sweep) {
            coefficient = -coefficient;
        }
        double centrePrimeX = coefficient * rx * primeY / ry;
        double centrePrimeY = -coefficient * ry * primeX / rx;
        double centreX = cos * centrePrimeX - sin * centrePrimeY + (x0 + x) / 2.0;
        double centreY = sin * centrePrimeX + cos * centrePrimeY + (y0 + y) / 2.0;

        double startUx = (primeX - centrePrimeX) / rx;
        double startUy = (primeY - centrePrimeY) / ry;
        double endUx = (-primeX - centrePrimeX) / rx;
        double endUy = (-primeY - centrePrimeY) / ry;
        double start = Math.atan2(startUy, startUx);
        double extent = Math.atan2(startUx * endUy - startUy * endUx, startUx * endUx + startUy * endUy);
        if (!sweep && extent > 0.0) {
            extent -= Math.PI * 2.0;
        } else if (sweep && extent < 0.0) {
            extent += Math.PI * 2.0;
        }

        int segments = (int) Math.ceil(Math.abs(extent) / (Math.PI / 2.0) - 1e-9);
        double step = extent / segments;
        double handle = 4.0 / 3.0 * Math.tan(step / 4.0);
        for (int segment = 0; segment < segments; ++segment) {
            double a1 = start + step * segment;
            double a2 = a1 + step;
            double cos1 = Math.cos(a1);
            double sin1 = Math.sin(a1);
            double cos2 = Math.cos(a2);
            double sin2 = Math.sin(a2);
            double[] points = {
                cos1 - handle * sin1, sin1 + handle * cos1,
                cos2 + handle * sin2, sin2 - handle * cos2,
                cos2, sin2,
            };
            for (int index = 0; index < points.length; index += 2) {
                double u = points[index] * rx;
                double v = points[index + 1] * ry;
                points[index] = centreX + u * cos - v * sin;
                points[index + 1] = centreY + u * sin + v * cos;
            }
            path.curveTo(points[0], points[1], points[2], points[3], points[4], points[5]);
        }
    }

    private static final class Tokens {
        private final String text;
        private int position;

        Tokens(String text) {
            this.text = text;
        }

        private void skipSeparators() {
            while (position < text.length()
                    && (Character.isWhitespace(text.charAt(position)) || text.charAt(position) == ',')) {
                ++position;
            }
        }

        boolean more() {
            skipSeparators();
            return position < text.length();
        }

        boolean atCommand() {
            skipSeparators();
            char c = text.charAt(position);
            return Character.isLetter(c) && c != 'e' && c != 'E';
        }

        char command() {
            return text.charAt(position++);
        }

        boolean flag() {
            skipSeparators();
            return text.charAt(position++) == '1';
        }

        double number() {
            skipSeparators();
            int begin = position;
            if (position < text.length() && (text.charAt(position) == '-' || text.charAt(position) == '+')) {
                ++position;
            }
            boolean dot = false;
            while (position < text.length()) {
                char c = text.charAt(position);
                if (Character.isDigit(c)) {
                    ++position;
                } else if (c == '.' && !dot) {
                    dot = true;
                    ++position;
                } else {
                    break;
                }
            }
            if (position < text.length() && (text.charAt(position) == 'e' || text.charAt(position) == 'E')) {
                ++position;
                if (text.charAt(position) == '-' || text.charAt(position) == '+') {
                    ++position;
                }
                while (position < text.length() && Character.isDigit(text.charAt(position))) {
                    ++position;
                }
            }
            if (position == begin) {
                throw new IllegalArgumentException("expected a number at " + begin + " in " + text);
            }
            return Double.parseDouble(text.substring(begin, position));
        }
    }
}
