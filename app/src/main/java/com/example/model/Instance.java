package com.example.model;

import java.util.Objects;

public class Instance {
    private final String id;
    private final String name;
    private final String minecraftVersion;
    private final Loader loader;
    private final String loaderVersion;
    private final LauncherType launcherType;
    private final String instancePathUri;
    private final String modsPathUri;
    private final boolean hasFolderAccess;
    private final int installedModCount;
    private final boolean isDefault;

    public Instance(String id, String name, String minecraftVersion, Loader loader,
                    String loaderVersion, LauncherType launcherType, String instancePathUri,
                    String modsPathUri, boolean hasFolderAccess, int installedModCount,
                    boolean isDefault) {
        this.id = id != null ? id : java.util.UUID.randomUUID().toString();
        this.name = name != null ? name : "Unnamed Instance";
        this.minecraftVersion = minecraftVersion != null ? minecraftVersion : "1.21.1";
        this.loader = loader != null ? loader : Loader.FABRIC;
        this.loaderVersion = loaderVersion != null ? loaderVersion : "";
        this.launcherType = launcherType != null ? launcherType : LauncherType.POJAV_LAUNCHER;
        this.instancePathUri = instancePathUri != null ? instancePathUri : "";
        this.modsPathUri = modsPathUri != null ? modsPathUri : "";
        this.hasFolderAccess = hasFolderAccess;
        this.installedModCount = installedModCount;
        this.isDefault = isDefault;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getMinecraftVersion() {
        return minecraftVersion;
    }

    public Loader getLoader() {
        return loader;
    }

    public String getLoaderVersion() {
        return loaderVersion;
    }

    public LauncherType getLauncherType() {
        return launcherType;
    }

    public String getInstancePathUri() {
        return instancePathUri;
    }

    public String getModsPathUri() {
        return modsPathUri;
    }

    public boolean hasFolderAccess() {
        return hasFolderAccess;
    }

    public int getInstalledModCount() {
        return installedModCount;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public Instance withInstalledModCount(int count) {
        return new Instance(id, name, minecraftVersion, loader, loaderVersion,
                launcherType, instancePathUri, modsPathUri, hasFolderAccess, count, isDefault);
    }

    public Instance withDefault(boolean isDefault) {
        return new Instance(id, name, minecraftVersion, loader, loaderVersion,
                launcherType, instancePathUri, modsPathUri, hasFolderAccess, installedModCount, isDefault);
    }

    public Instance withFolderAccess(boolean access, String instanceUri, String modsUri) {
        return new Instance(id, name, minecraftVersion, loader, loaderVersion,
                launcherType, instanceUri, modsUri, access, installedModCount, isDefault);
    }

    public boolean isCompatibleWith(String targetMcVersion, Loader targetLoader) {
        boolean mcMatches = targetMcVersion == null || targetMcVersion.isEmpty() ||
                targetMcVersion.equalsIgnoreCase("All") ||
                this.minecraftVersion.equalsIgnoreCase(targetMcVersion);
        boolean loaderMatches = targetLoader == null || targetLoader == Loader.UNKNOWN ||
                this.loader.isCompatibleWith(targetLoader);
        return mcMatches && loaderMatches;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Instance instance = (Instance) o;
        return Objects.equals(id, instance.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
