package com.example.model;

import java.util.Locale;

public enum Loader {
    FABRIC("Fabric", "fabric"),
    FORGE("Forge", "forge"),
    NEOFORGE("NeoForge", "neoforge"),
    QUILT("Quilt", "quilt"),
    UNKNOWN("Unknown", "unknown");

    private final String displayName;
    private final String slug;

    Loader(String displayName, String slug) {
        this.displayName = displayName;
        this.slug = slug;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSlug() {
        return slug;
    }

    public static Loader fromSlug(String slug) {
        if (slug == null) return UNKNOWN;
        String s = slug.toLowerCase(Locale.ROOT).trim();
        switch (s) {
            case "fabric":
                return FABRIC;
            case "forge":
                return FORGE;
            case "neoforge":
            case "neo_forge":
            case "neo-forge":
                return NEOFORGE;
            case "quilt":
                return QUILT;
            default:
                return UNKNOWN;
        }
    }

    public boolean isCompatibleWith(Loader other) {
        if (this == other) return true;
        // Quilt can run most Fabric mods
        if (this == QUILT && other == FABRIC) return true;
        return false;
    }
}
