package io.protop.core.logs;

public class Logs {

    private static Boolean enabled = false;

    public static void enableIf(Boolean condition) {
        enabled = condition;
    }

    public static void enable() {
        enabled = true;
    }

    public static void disable() {
        enabled = false;
    }

    public static Boolean areEnabled() {
        return enabled;
    }
}
