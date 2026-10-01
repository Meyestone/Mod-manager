package com.example.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InstalledMod {
    private final String id;
    private final String name;
    private final String fileName;
    private final String version;
    private final Loader loader;
    private final Source source;
    private final boolean isEnabled;
    private final long fileSizeBytes;
    private final long lastModified;
    private final String modId;
    private final String iconUrl;
    private final String projectUrl;
    private final List<String> dependencies;
    private final boolean hasUpdate;
    private final String latestVersion;
    private final String latestVersionId;

    public InstalledMod(String id, String name, String fileName, String version,
                        Loader loader, Source source, boolean isEnabled,
                        long fileSizeBytes, long lastModified, String modId,
                        String iconUrl, String projectUrl, List<String> dependencies,
                        boolean hasUpdate, String latestVersion, String latestVersionId) {
        this.id = id != null ? id : (modId != null && !modId.isEmpty() ? modId : fileName);
        this.name = name != null ? name : fileName;
        this.fileName = fileName != null ? fileName : "mod.jar";
        this.version = version != null ? version : "1.0.0";
        this.loader = loader != null ? loader : Loader.FABRIC;
        this.source = source != null ? source : Source.MODRINTH;
        this.isEnabled = isEnabled;
        this.fileSizeBytes = fileSizeBytes;
        this.lastModified = lastModified;
        this.modId = modId != null ? modId : id;
        this.iconUrl = iconUrl != null ? iconUrl : "";
        this.projectUrl = projectUrl != null ? projectUrl : "";
        this.dependencies = dependencies != null ? new ArrayList<>(dependencies) : Collections.emptyList();
        this.hasUpdate = hasUpdate;
        this.latestVersion = latestVersion != null ? latestVersion : "";
        this.latestVersionId = latestVersionId != null ? latestVersionId : "";
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getFileName() {
        return fileName;
    }

    public String getVersion() {
        return version;
    }

    public Loader getLoader() {
        return loader;
    }

    public Source getSource() {
        return source;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public long getLastModified() {
        return lastModified;
    }

    public String getModId() {
        return modId;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public String getProjectUrl() {
        return projectUrl;
    }

    public List<String> getDependencies() {
        return Collections.unmodifiableList(dependencies);
    }

    public boolean hasUpdate() {
        return hasUpdate;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public String getLatestVersionId() {
        return latestVersionId;
    }

    public InstalledMod withEnabled(boolean enabled) {
        String newFileName = this.fileName;
        if (enabled && newFileName.endsWith(".disabled")) {
            newFileName = newFileName.substring(0, newFileName.length() - ".disabled".length());
        } else if (!enabled && !newFileName.endsWith(".disabled")) {
            newFileName = newFileName + ".disabled";
        }
        return new InstalledMod(id, name, newFileName, version, loader, source,
                enabled, fileSizeBytes, lastModified, modId, iconUrl, projectUrl,
                dependencies, hasUpdate, latestVersion, latestVersionId);
    }

    public InstalledMod withUpdate(boolean hasUpdate, String latestVersion, String latestVersionId) {
        return new InstalledMod(id, name, fileName, version, loader, source,
                isEnabled, fileSizeBytes, lastModified, modId, iconUrl, projectUrl,
                dependencies, hasUpdate, latestVersion, latestVersionId);
    }

    public String getFormattedSize() {
        if (fileSizeBytes <= 0) return "Unknown size";
        if (fileSizeBytes < 1024) return fileSizeBytes + " B";
        if (fileSizeBytes < 1024 * 1024) return String.format(java.util.Locale.US, "%.1f KB", fileSizeBytes / 1024.0);
        return String.format(java.util.Locale.US, "%.2f MB", fileSizeBytes / (1024.0 * 1024.0));
    }
}
