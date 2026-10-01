package com.example.model;

public enum LauncherType {
    POJAV_LAUNCHER("PojavLauncher", "net.kdt.pojavlaunch"),
    AMETHYST_LAUNCHER("Amethyst Launcher", "org.amethyst.launcher"),
    CUSTOM("Custom / External Directory", "");

    private final String displayName;
    private final String packageName;

    LauncherType(String displayName, String packageName) {
        this.displayName = displayName;
        this.packageName = packageName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPackageName() {
        return packageName;
    }
}
