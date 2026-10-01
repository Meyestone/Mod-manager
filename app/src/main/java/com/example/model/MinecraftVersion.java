package com.example.model;

import java.util.Objects;

public class MinecraftVersion implements Comparable<MinecraftVersion> {
    private final String versionString;
    private final boolean isRelease;
    private final int major;
    private final int minor;
    private final int patch;

    public MinecraftVersion(String versionString) {
        this.versionString = versionString != null ? versionString.trim() : "";
        this.isRelease = !this.versionString.contains("-") && !this.versionString.contains("w") && !this.versionString.contains("pre");
        
        int maj = 1, min = 0, pat = 0;
        try {
            String clean = this.versionString.replaceAll("[^0-9.]", "");
            String[] parts = clean.split("\\.");
            if (parts.length > 0 && !parts[0].isEmpty()) maj = Integer.parseInt(parts[0]);
            if (parts.length > 1 && !parts[1].isEmpty()) min = Integer.parseInt(parts[1]);
            if (parts.length > 2 && !parts[2].isEmpty()) pat = Integer.parseInt(parts[2]);
        } catch (Exception ignored) {
        }
        this.major = maj;
        this.minor = min;
        this.patch = pat;
    }

    public String getVersionString() {
        return versionString;
    }

    public boolean isRelease() {
        return isRelease;
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getPatch() {
        return patch;
    }

    @Override
    public int compareTo(MinecraftVersion o) {
        if (this.major != o.major) return Integer.compare(o.major, this.major); // descending
        if (this.minor != o.minor) return Integer.compare(o.minor, this.minor);
        if (this.patch != o.patch) return Integer.compare(o.patch, this.patch);
        return o.versionString.compareTo(this.versionString);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MinecraftVersion that = (MinecraftVersion) o;
        return Objects.equals(versionString, that.versionString);
    }

    @Override
    public int hashCode() {
        return Objects.hash(versionString);
    }

    @Override
    public String toString() {
        return versionString;
    }
}
