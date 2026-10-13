package stone.render;

public enum FontRole {
    REGULAR("Inter-Regular"),
    MEDIUM("Inter-Medium"),
    SEMI_BOLD("Inter-SemiBold"),
    BOLD("Inter-Bold"),
    MONO("JetBrainsMono-Medium"),
    MONO_BOLD("JetBrainsMono-Bold");

    final String file;

    FontRole(String file) {
        this.file = file;
    }
}
