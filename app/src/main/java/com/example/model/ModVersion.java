package com.example.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ModVersion {
    private final String versionId;
    private final String projectId;
    private final String versionNumber;
    private final String name;
    private final String changelog;
    private final List<String> gameVersions;
    private final List<Loader> loaders;
    private final List<ModFile> files;
    private final List<Dependency> dependencies;
    private final Source source;
    private final String datePublished;

    public ModVersion(String versionId, String projectId, String versionNumber, String name,
                      String changelog, List<String> gameVersions, List<Loader> loaders,
                      List<ModFile> files, List<Dependency> dependencies, Source source,
                      String datePublished) {
        this.versionId = versionId != null ? versionId : "";
        this.projectId = projectId != null ? projectId : "";
        this.versionNumber = versionNumber != null ? versionNumber : "";
        this.name = name != null && !name.isEmpty() ? name : versionNumber;
        this.changelog = changelog != null ? changelog : "";
        this.gameVersions = gameVersions != null ? new ArrayList<>(gameVersions) : Collections.emptyList();
        this.loaders = loaders != null ? new ArrayList<>(loaders) : Collections.emptyList();
        this.files = files != null ? new ArrayList<>(files) : Collections.emptyList();
        this.dependencies = dependencies != null ? new ArrayList<>(dependencies) : Collections.emptyList();
        this.source = source != null ? source : Source.MODRINTH;
        this.datePublished = datePublished != null ? datePublished : "";
    }

    public String getVersionId() {
        return versionId;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getVersionNumber() {
        return versionNumber;
    }

    public String getName() {
        return name;
    }

    public String getChangelog() {
        return changelog;
    }

    public List<String> getGameVersions() {
        return Collections.unmodifiableList(gameVersions);
    }

    public List<Loader> getLoaders() {
        return Collections.unmodifiableList(loaders);
    }

    public List<ModFile> getFiles() {
        return Collections.unmodifiableList(files);
    }

    public List<Dependency> getDependencies() {
        return Collections.unmodifiableList(dependencies);
    }

    public List<Dependency> getRequiredDependencies() {
        List<Dependency> req = new ArrayList<>();
        for (Dependency d : dependencies) {
            if (d.isRequired()) req.add(d);
        }
        return req;
    }

    public List<Dependency> getOptionalDependencies() {
        List<Dependency> opt = new ArrayList<>();
        for (Dependency d : dependencies) {
            if (d.isOptional()) opt.add(d);
        }
        return opt;
    }

    public Source getSource() {
        return source;
    }

    public String getDatePublished() {
        return datePublished;
    }

    public ModFile getPrimaryFile() {
        for (ModFile f : files) {
            if (f.isPrimary()) return f;
        }
        return !files.isEmpty() ? files.get(0) : null;
    }
}
