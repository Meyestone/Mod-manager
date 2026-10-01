package com.example.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Mod {
    private final String id;
    private final String slug;
    private final String name;
    private final String author;
    private final String description;
    private final String iconUrl;
    private final Source source;
    private final List<String> supportedVersions;
    private final List<Loader> supportedLoaders;
    private final long downloads;
    private final List<String> categories;
    private final String license;
    private final String websiteUrl;
    private final String sourceUrl;
    private final String issuesUrl;

    public Mod(String id, String slug, String name, String author, String description,
               String iconUrl, Source source, List<String> supportedVersions,
               List<Loader> supportedLoaders, long downloads, List<String> categories,
               String license, String websiteUrl, String sourceUrl, String issuesUrl) {
        this.id = id != null ? id : "";
        this.slug = slug != null ? slug : id;
        this.name = name != null ? name : "";
        this.author = author != null ? author : "Unknown";
        this.description = description != null ? description : "";
        this.iconUrl = iconUrl != null ? iconUrl : "";
        this.source = source != null ? source : Source.MODRINTH;
        this.supportedVersions = supportedVersions != null ? new ArrayList<>(supportedVersions) : Collections.emptyList();
        this.supportedLoaders = supportedLoaders != null ? new ArrayList<>(supportedLoaders) : Collections.emptyList();
        this.downloads = downloads;
        this.categories = categories != null ? new ArrayList<>(categories) : Collections.emptyList();
        this.license = license != null ? license : "";
        this.websiteUrl = websiteUrl != null ? websiteUrl : "";
        this.sourceUrl = sourceUrl != null ? sourceUrl : "";
        this.issuesUrl = issuesUrl != null ? issuesUrl : "";
    }

    public String getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public String getName() {
        return name;
    }

    public String getAuthor() {
        return author;
    }

    public String getDescription() {
        return description;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public Source getSource() {
        return source;
    }

    public List<String> getSupportedVersions() {
        return Collections.unmodifiableList(supportedVersions);
    }

    public List<Loader> getSupportedLoaders() {
        return Collections.unmodifiableList(supportedLoaders);
    }

    public long getDownloads() {
        return downloads;
    }

    public List<String> getCategories() {
        return Collections.unmodifiableList(categories);
    }

    public String getLicense() {
        return license;
    }

    public String getWebsiteUrl() {
        if (!websiteUrl.isEmpty()) return websiteUrl;
        if (source == Source.MODRINTH) {
            return "https://modrinth.com/mod/" + (slug != null && !slug.isEmpty() ? slug : id);
        } else {
            return "https://www.curseforge.com/minecraft/mc-mods/" + (slug != null && !slug.isEmpty() ? slug : id);
        }
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public String getIssuesUrl() {
        return issuesUrl;
    }

    public boolean supportsVersion(String version) {
        if (version == null || version.isEmpty() || version.equalsIgnoreCase("All")) return true;
        for (String v : supportedVersions) {
            if (v.equalsIgnoreCase(version)) return true;
        }
        return false;
    }

    public boolean supportsLoader(Loader loader) {
        if (loader == null || loader == Loader.UNKNOWN) return true;
        for (Loader l : supportedLoaders) {
            if (l == loader || (loader == Loader.QUILT && l == Loader.FABRIC)) return true;
        }
        return false;
    }

    public String getFormattedDownloads() {
        if (downloads < 1000) return String.valueOf(downloads);
        if (downloads < 1000000) return String.format(java.util.Locale.US, "%.1fK", downloads / 1000.0);
        return String.format(java.util.Locale.US, "%.1fM", downloads / 1000000.0);
    }
}
