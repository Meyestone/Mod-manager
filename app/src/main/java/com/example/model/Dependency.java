package com.example.model;

import java.util.Objects;

public class Dependency {
    private final String projectId;
    private final String versionId;
    private final DependencyType dependencyType;
    private final String fileName;
    private final String title;
    private final Source source;

    public Dependency(String projectId, String versionId, DependencyType dependencyType, String fileName, String title, Source source) {
        this.projectId = projectId != null ? projectId : "";
        this.versionId = versionId != null ? versionId : "";
        this.dependencyType = dependencyType != null ? dependencyType : DependencyType.REQUIRED;
        this.fileName = fileName != null ? fileName : "";
        this.title = title != null && !title.isEmpty() ? title : (fileName != null && !fileName.isEmpty() ? fileName : projectId);
        this.source = source != null ? source : Source.MODRINTH;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getVersionId() {
        return versionId;
    }

    public DependencyType getDependencyType() {
        return dependencyType;
    }

    public String getFileName() {
        return fileName;
    }

    public String getTitle() {
        return title;
    }

    public Source getSource() {
        return source;
    }

    public boolean isRequired() {
        return dependencyType == DependencyType.REQUIRED;
    }

    public boolean isOptional() {
        return dependencyType == DependencyType.OPTIONAL;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Dependency that = (Dependency) o;
        return Objects.equals(projectId, that.projectId) &&
               dependencyType == that.dependencyType &&
               source == that.source;
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectId, dependencyType, source);
    }
}
