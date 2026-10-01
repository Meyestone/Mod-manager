package com.example.model;

public enum Source {
    MODRINTH("Modrinth", "https://modrinth.com"),
    CURSEFORGE("CurseForge", "https://www.curseforge.com");

    private final String displayName;
    private final String baseUrl;

    Source(String displayName, String baseUrl) {
        this.displayName = displayName;
        this.baseUrl = baseUrl;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}
