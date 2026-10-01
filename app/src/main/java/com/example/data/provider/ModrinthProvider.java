package com.example.data.provider;

import com.example.model.Dependency;
import com.example.model.DependencyType;
import com.example.model.Loader;
import com.example.model.Mod;
import com.example.model.ModFile;
import com.example.model.ModVersion;
import com.example.model.Source;

import org.json.JSONArray;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Response;

public class ModrinthProvider implements ModProvider {
    private final ModrinthApiService apiService;

    public ModrinthProvider() {
        this.apiService = ModrinthRetrofitClient.INSTANCE.getApi();
    }

    public ModrinthProvider(ModrinthApiService apiService) {
        this.apiService = apiService;
    }

    @Override
    public Source getSource() {
        return Source.MODRINTH;
    }

    @Override
    public boolean isConfigured() {
        return true;
    }

    @Override
    public String getConfigurationNote() {
        return "Modrinth v2 Retrofit API ready (no API key required).";
    }

    @Override
    public List<Mod> searchMods(String query, String gameVersion, Loader loader, int offset, int limit) throws Exception {
        JSONArray facets = new JSONArray();

        JSONArray typeFacet = new JSONArray();
        typeFacet.put("project_type:mod");
        facets.put(typeFacet);

        if (gameVersion != null && !gameVersion.isEmpty() && !gameVersion.equalsIgnoreCase("All")) {
            JSONArray versionFacet = new JSONArray();
            versionFacet.put("versions:" + gameVersion);
            facets.put(versionFacet);
        }

        if (loader != null && loader != Loader.UNKNOWN) {
            JSONArray loaderFacet = new JSONArray();
            loaderFacet.put("categories:" + loader.getSlug());
            facets.put(loaderFacet);
        }

        String facetsStr = facets.length() > 0 ? facets.toString() : null;
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        int safeOffset = Math.max(offset, 0);

        Response<ModrinthSearchResponse> response = apiService.searchMods(
                query != null && !query.trim().isEmpty() ? query.trim() : null,
                safeLimit,
                safeOffset,
                facetsStr
        ).execute();

        if (!response.isSuccessful()) {
            if (response.code() == 429) {
                throw new IOException("Modrinth rate limit exceeded. Please wait a moment.");
            }
            throw new IOException("Modrinth API error: HTTP " + response.code());
        }

        ModrinthSearchResponse body = response.body();
        if (body == null || body.getHits() == null) {
            return Collections.emptyList();
        }

        List<Mod> result = new ArrayList<>();
        for (ModrinthHit hit : body.getHits()) {
            result.add(parseModFromHit(hit));
        }
        return result;
    }

    private Mod parseModFromHit(ModrinthHit hit) {
        String id = hit.getProjectId();
        String slug = hit.getSlug() != null ? hit.getSlug() : id;
        String title = hit.getTitle() != null ? hit.getTitle() : slug;
        String description = hit.getDescription() != null ? hit.getDescription() : "";
        String author = hit.getAuthor() != null ? hit.getAuthor() : "Unknown";
        String iconUrl = hit.getIconUrl() != null ? hit.getIconUrl() : "";
        long downloads = hit.getDownloads();
        String license = hit.getLicense() != null ? hit.getLicense() : "";

        List<String> versions = hit.getVersions() != null ? new ArrayList<>(hit.getVersions()) : Collections.emptyList();
        List<Loader> loaders = new ArrayList<>();
        List<String> categories = new ArrayList<>();

        if (hit.getCategories() != null) {
            for (String cat : hit.getCategories()) {
                categories.add(cat);
                Loader l = Loader.fromSlug(cat);
                if (l != Loader.UNKNOWN && !loaders.contains(l)) {
                    loaders.add(l);
                }
            }
        }

        return new Mod(id, slug, title, author, description, iconUrl, Source.MODRINTH,
                versions, loaders, downloads, categories, license,
                "https://modrinth.com/mod/" + slug, "", "");
    }

    @Override
    public Mod getMod(String idOrSlug) throws Exception {
        Response<ModrinthProject> response = apiService.getProject(idOrSlug).execute();
        if (!response.isSuccessful() || response.body() == null) {
            throw new IOException("Modrinth mod not found: HTTP " + response.code());
        }

        ModrinthProject p = response.body();
        String id = p.getId();
        String slug = p.getSlug() != null ? p.getSlug() : id;
        String title = p.getTitle() != null ? p.getTitle() : slug;
        String description = p.getDescription() != null ? p.getDescription() : "";
        String iconUrl = p.getIconUrl() != null ? p.getIconUrl() : "";
        long downloads = p.getDownloads();
        String license = p.getLicense() != null ? p.getLicense().getName() : "";

        List<String> versions = p.getGameVersions() != null ? new ArrayList<>(p.getGameVersions()) : Collections.emptyList();
        List<Loader> loaders = new ArrayList<>();
        List<String> categories = new ArrayList<>();

        if (p.getLoaders() != null) {
            for (String cat : p.getLoaders()) {
                categories.add(cat);
                Loader l = Loader.fromSlug(cat);
                if (l != Loader.UNKNOWN && !loaders.contains(l)) loaders.add(l);
            }
        }

        String sourceUrl = p.getSourceUrl() != null ? p.getSourceUrl() : "";
        String issuesUrl = p.getIssuesUrl() != null ? p.getIssuesUrl() : "";

        return new Mod(id, slug, title, "Modrinth Author", description, iconUrl,
                Source.MODRINTH, versions, loaders, downloads, categories, license,
                "https://modrinth.com/mod/" + slug, sourceUrl, issuesUrl);
    }

    @Override
    public List<ModVersion> getVersions(String modId, String gameVersion, Loader loader) throws Exception {
        String loadersParam = null;
        if (loader != null && loader != Loader.UNKNOWN) {
            if (loader == Loader.QUILT) {
                loadersParam = "[\"quilt\",\"fabric\"]";
            } else {
                loadersParam = "[\"" + loader.getSlug() + "\"]";
            }
        }

        String gameVersionsParam = null;
        if (gameVersion != null && !gameVersion.isEmpty() && !gameVersion.equalsIgnoreCase("All")) {
            gameVersionsParam = "[\"" + gameVersion + "\"]";
        }

        Response<List<ModrinthVersion>> response = apiService.getVersions(modId, loadersParam, gameVersionsParam).execute();
        if (!response.isSuccessful() || response.body() == null) {
            throw new IOException("Failed to fetch Modrinth versions: HTTP " + response.code());
        }

        List<ModVersion> result = new ArrayList<>();
        for (ModrinthVersion mv : response.body()) {
            result.add(parseModVersion(mv));
        }
        return result;
    }

    @Override
    public ModVersion getVersion(String versionId) throws Exception {
        Response<ModrinthVersion> response = apiService.getVersion(versionId).execute();
        if (!response.isSuccessful() || response.body() == null) {
            throw new IOException("Failed to fetch Modrinth version: HTTP " + response.code());
        }
        return parseModVersion(response.body());
    }

    public List<String> getSupportedGameVersions() {
        try {
            Response<List<ModrinthGameVersionTag>> response = apiService.getGameVersions().execute();
            if (response.isSuccessful() && response.body() != null) {
                List<String> list = new ArrayList<>();
                for (ModrinthGameVersionTag tag : response.body()) {
                    if ("release".equalsIgnoreCase(tag.getVersionType())) {
                        list.add(tag.getVersion());
                    }
                }
                return list;
            }
        } catch (Exception ignored) {
        }
        return ModrinthProvider.getDefaultGameVersions();
    }

    public static List<String> getDefaultGameVersions() {
        List<String> fallback = new ArrayList<>();
        fallback.add("1.21.4");
        fallback.add("1.21.3");
        fallback.add("1.21.2");
        fallback.add("1.21.1");
        fallback.add("1.21");
        fallback.add("1.20.6");
        fallback.add("1.20.4");
        fallback.add("1.20.2");
        fallback.add("1.20.1");
        fallback.add("1.19.4");
        fallback.add("1.19.2");
        fallback.add("1.18.2");
        fallback.add("1.16.5");
        return fallback;
    }

    private ModVersion parseModVersion(ModrinthVersion mv) {
        String versionId = mv.getId();
        String projectId = mv.getProjectId();
        String versionNumber = mv.getVersionNumber();
        String name = mv.getName() != null && !mv.getName().isEmpty() ? mv.getName() : versionNumber;
        String changelog = mv.getChangelog() != null ? mv.getChangelog() : "";
        String datePublished = mv.getDatePublished() != null ? mv.getDatePublished() : "";

        List<String> gameVersions = mv.getGameVersions() != null ? new ArrayList<>(mv.getGameVersions()) : Collections.emptyList();
        List<Loader> loaders = new ArrayList<>();
        if (mv.getLoaders() != null) {
            for (String lSlug : mv.getLoaders()) {
                Loader l = Loader.fromSlug(lSlug);
                if (l != Loader.UNKNOWN && !loaders.contains(l)) loaders.add(l);
            }
        }

        List<ModFile> files = new ArrayList<>();
        if (mv.getFiles() != null) {
            for (int i = 0; i < mv.getFiles().size(); i++) {
                ModrinthFile f = mv.getFiles().get(i);
                String sha1 = f.getHashes() != null ? f.getHashes().getSha1() : "";
                String sha512 = f.getHashes() != null ? f.getHashes().getSha512() : "";
                files.add(new ModFile(versionId + "_" + i, f.getUrl(), f.getFilename(), f.getPrimary(), f.getSize(), sha1, sha512));
            }
        }

        List<Dependency> dependencies = new ArrayList<>();
        if (mv.getDependencies() != null) {
            for (ModrinthDependency d : mv.getDependencies()) {
                String depProjectId = d.getProjectId() != null ? d.getProjectId() : "";
                String depVersionId = d.getVersionId() != null ? d.getVersionId() : "";
                String depFileName = d.getFileName() != null ? d.getFileName() : "";
                String depTypeStr = d.getDependencyType() != null ? d.getDependencyType() : "required";

                DependencyType type = DependencyType.REQUIRED;
                if ("optional".equalsIgnoreCase(depTypeStr)) type = DependencyType.OPTIONAL;
                else if ("incompatible".equalsIgnoreCase(depTypeStr)) type = DependencyType.INCOMPATIBLE;
                else if ("embedded".equalsIgnoreCase(depTypeStr)) type = DependencyType.EMBEDDED;

                if (!depProjectId.isEmpty() || !depVersionId.isEmpty() || !depFileName.isEmpty()) {
                    dependencies.add(new Dependency(depProjectId, depVersionId, type, depFileName, depProjectId, Source.MODRINTH));
                }
            }
        }

        return new ModVersion(versionId, projectId, versionNumber, name, changelog,
                gameVersions, loaders, files, dependencies, Source.MODRINTH, datePublished);
    }
}
