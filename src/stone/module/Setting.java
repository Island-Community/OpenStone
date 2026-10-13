package stone.module;

import stone.core.Color;
import stone.core.Maths;
import java.util.List;
import java.util.function.BooleanSupplier;

public final class Setting {
    public enum Type { BOOLEAN, NUMBER, CHOICE, KEY, COLOUR }

    public final Type type;
    public final String name;
    public final String description;

    private boolean bool;
    private float number;
    float minimum;
    float maximum = 1.0f;
    float step = 0.01f;
    int decimals = 2;
    private int choice;
    List<String> choices = List.of();
    private int key;
    private Color colour = Color.rgb(0x58C6FF);
    private BooleanSupplier visible;
    private boolean shown = true;
    Runnable onChange;

    Setting(Type type, String name, String description) {
        this.type = type;
        this.name = name;
        this.description = description;
    }

    public boolean bool() {
        return bool;
    }

    public void setBool(boolean value) {
        boolean previous = bool;
        bool = value;
        changed(previous != bool);
    }

    private void changed(boolean differs) {
        if (differs && onChange != null) {
            onChange.run();
        }
    }

    public float number() {
        return number;
    }

    public int intValue() {
        return Math.round(number);
    }

    public void setNumber(float value) {
        float previous = number;
        number = Maths.clamp(value, minimum, maximum);
        if (step > 0.0f) {
            number = Maths.clamp(minimum + Math.round((number - minimum) / step) * step, minimum, maximum);
        }
        changed(previous != number);
    }

    public float minimum() {
        return minimum;
    }

    public float maximum() {
        return maximum;
    }

    public int decimals() {
        return decimals;
    }

    public float normalised() {
        float span = maximum - minimum;
        return span <= 0.0f ? 0.0f : Maths.clamp((number - minimum) / span, 0.0f, 1.0f);
    }

    public void setNormalised(float fraction) {
        setNumber(minimum + Maths.clamp(fraction, 0.0f, 1.0f) * (maximum - minimum));
    }

    public int choice() {
        return choice;
    }

    public void setChoice(int index) {
        int previous = choice;
        choice = choices.isEmpty() ? 0 : Math.floorMod(index, choices.size());
        changed(previous != choice);
    }

    public List<String> choices() {
        return choices;
    }

    public String choiceName() {
        return choices.isEmpty() ? "" : choices.get(choice);
    }

    public boolean is(String choiceName) {
        return choiceName().equals(choiceName);
    }

    public int key() {
        return key;
    }

    public void setKey(int value) {
        key = value;
    }

    public Color colour() {
        return colour;
    }

    public void setColour(Color value) {
        colour = value;
    }

    public Setting visibleWhen(BooleanSupplier predicate) {
        visible = predicate;
        return this;
    }

    public void setVisible(boolean value) {
        shown = value;
    }

    public boolean visible() {
        return shown && (visible == null || visible.getAsBoolean());
    }
}
