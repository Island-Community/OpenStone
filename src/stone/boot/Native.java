package stone.boot;

public final class Native {
    private Native() {}

    public static native Class<?> defineClass(ClassLoader loader, String binaryName, byte[] bytes);

    public static native Class<?>[] loadedClasses();

    public static native void retransform(Class<?>[] classes);

    public static native void setTargets(String[] internalNames);

    public static native void requestUnload();
}
