package com.example.data.provider;

import com.example.model.Dependency;
import com.example.model.DependencyType;
import com.example.model.Loader;
import com.example.model.Mod;
import com.example.model.ModFile;
import com.example.model.ModVersion;
import com.example.model.Source;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CurseForgeProvider implements ModProvider {
    private static final String BASE_URL = "https://api.curseforge.com/v1";
    private static final int MINECRAFT_GAME_ID = 432;
    private static final int MODS_CLASS_ID = 6;

    private final OkHttpClient client;
    private String apiKey;

    public CurseForgeProvider(String apiKey) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build();
    }

    public void setApiKey(String key) {
        this.apiKey = key != null ? key.trim() : "";
    }

    public String getApiKey() {
        return apiKey;
    }

    @Override
    public Source getSource() {
        return Source.CURSEFORGE;
    }

    @Override
    public boolean isConfigured() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    @Override
    public String getConfigurationNote() {
        if (isConfigured()) {
            return "CurseForge API key configured.";
        } else {
            return "CurseForge API requires an official API key ($2a$10$...). You can add your CurseForge API key in Settings. Modrinth is active and works without any key.";
        }
    }

    private int getLoaderType(Loader loader) {
        if (loader == null) return 0;
        switch (loader) {
            case FORGE:
                return 1;
            case FABRIC:
                return 4;
            case QUILT:
                return 5;
            case NEOFORGE:
                return 6;
            default:
                return 0;
        }
    }

    @Override
    public List<Mod> searchMods(String query, String gameVersion, Loader loader, int offset, int limit) throws Exception {
        if (!isConfigured()) {
            return Collections.emptyList();
        }

        HttpUrl.Builder urlBuilder = HttpUrl.parse(BASE_URL + "/mods/search").newBuilder()
                .addQueryParameter("gameId", String.valueOf(MINECRAFT_GAME_ID))
                .addQueryParameter("classId", String.valueOf(MODS_CLASS_ID))
                .addQueryParameter("pageSize", String.valueOf(Math.min(limit, 50)))
                .addQueryParameter("index", String.valueOf(Math.max(offset, 0)));

        if (query != null && !query.trim().isEmpty()) {
            urlBuilder.addQueryParameter("searchFilter", query.trim());
        }

        if (gameVersion != null && !gameVersion.isEmpty() && !gameVersion.equalsIgnoreCase("All")) {
            urlBuilder.addQueryParameter("gameVersion", gameVersion.trim());
        }

        int loaderType = getLoaderType(loader);
        if (loaderType > 0) {
            urlBuilder.addQueryParameter("modLoaderType", String.valueOf(loaderType));
        }

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header("x-api-key", apiKey)
                .header("Accept", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                if (response.code() == 403 || response.code() == 401) {
                    throw new IOException("CurseForge: Invalid API key or unauthorized. Check your key in Settings.");
                }
                throw new IOException("CurseForge API error: HTTP " + response.code());
            }

            String body = response.body() != null ? response.body().string() : "";
            JSONObject json = new JSONObject(body);
            JSONArray data = json.optJSONArray("data");
            if (data == null) return Collections.emptyList();

            List<Mod> result = new ArrayList<>();
            for (int i = 0; i < data.length(); i++) {
                result.add(parseModFromJson(data.getJSONObject(i)));
            }
            return result;
        }
    }

    private Mod parseModFromJson(JSONObject obj) {
        String id = String.valueOf(obj.optInt("id", 0));
        String slug = obj.optString("slug", id);
        String name = obj.optString("name", slug);
        String summary = obj.optString("summary", "");
        long downloads = obj.optLong("downloadCount", 0);

        String author = "CurseForge Author";
        JSONArray authors = obj.optJSONArray("authors");
        if (authors != null && authors.length() > 0) {
            JSONObject aObj = authors.optJSONObject(0);
            if (aObj != null) author = aObj.optString("name", author);
        }

        String iconUrl = "";
        JSONObject logo = obj.optJSONObject("logo");
        if (logo != null) {
            iconUrl = logo.optString("thumbnailUrl", logo.optString("url", ""));
        }

        List<String> supportedVersions = new ArrayList<>();
        List<Loader> supportedLoaders = new ArrayList<>();
        JSONArray latestFiles = obj.optJSONArray("latestFilesIndexes");
        if (latestFiles != null) {
            for (int j = 0; j < latestFiles.length(); j++) {
                JSONObject lfi = latestFiles.optJSONObject(j);
                if (lfi == null) continue;
                String gv = lfi.optString("gameVersion", "");
                if (!gv.isEmpty() && !supportedVersions.contains(gv)) {
                    supportedVersions.add(gv);
                }
                int modLoaderType = lfi.optInt("modLoaderType", 0);
                Loader l = loaderFromCurseForgeType(modLoaderType);
                if (l != Loader.UNKNOWN && !supportedLoaders.contains(l)) {
                    supportedLoaders.add(l);
                }
            }
        }

        List<String> categories = new ArrayList<>();
        JSONArray cats = obj.optJSONArray("categories");
        if (cats != null) {
            for (int j = 0; j < cats.length(); j++) {
                JSONObject catObj = cats.optJSONObject(j);
                if (catObj != null) categories.add(catObj.optString("name", ""));
            }
        }

        JSONObject links = obj.optJSONObject("links");
        String websiteUrl = links != null ? links.optString("websiteUrl", "https://www.curseforge.com/minecraft/mc-mods/" + slug) : "";
        String sourceUrl = links != null ? links.optString("sourceUrl", "") : "";
        String issuesUrl = links != null ? links.optString("issuesUrl", "") : "";

        return new Mod(id, slug, name, author, summary, iconUrl, Source.CURSEFORGE,
                supportedVersions, supportedLoaders, downloads, categories, "Custom / CurseForge",
                websiteUrl, sourceUrl, issuesUrl);
    }

    private Loader loaderFromCurseForgeType(int type) {
        switch (type) {
            case 1:
                return Loader.FORGE;
            case 4:
                return Loader.FABRIC;
            case 5:
                return Loader.QUILT;
            case 6:
                return Loader.NEOFORGE;
            default:
                return Loader.UNKNOWN;
        }
    }

    @Override
    public Mod getMod(String idOrSlug) throws Exception {
        if (!isConfigured()) {
            throw new IOException("CurseForge API key not configured.");
        }

        Request request = new Request.Builder()
                .url(BASE_URL + "/mods/" + idOrSlug)
                .header("x-api-key", apiKey)
                .header("Accept", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("CurseForge mod not found: HTTP " + response.code());
            }

            String body = response.body() != null ? response.body().string() : "";
            JSONObject json = new JSONObject(body);
            JSONObject data = json.optJSONObject("data");
            if (data == null) throw new IOException("CurseForge mod response missing data");
            return parseModFromJson(data);
        }
    }

    @Override
    public List<ModVersion> getVersions(String modId, String gameVersion, Loader loader) throws Exception {
        if (!isConfigured()) {
            return Collections.emptyList();
        }

        HttpUrl.Builder urlBuilder = HttpUrl.parse(BASE_URL + "/mods/" + modId + "/files").newBuilder()
                .addQueryParameter("pageSize", "30");

        if (gameVersion != null && !gameVersion.isEmpty() && !gameVersion.equalsIgnoreCase("All")) {
            urlBuilder.addQueryParameter("gameVersion", gameVersion.trim());
        }

        int loaderType = getLoaderType(loader);
        if (loaderType > 0) {
            urlBuilder.addQueryParameter("modLoaderType", String.valueOf(loaderType));
        }

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header("x-api-key", apiKey)
                .header("Accept", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to fetch CurseForge files: HTTP " + response.code());
            }

            String body = response.body() != null ? response.body().string() : "";
            JSONObject json = new JSONObject(body);
            JSONArray data = json.optJSONArray("data");
            if (data == null) return Collections.emptyList();

            List<ModVersion> versions = new ArrayList<>();
            for (int i = 0; i < data.length(); i++) {
                versions.add(parseVersionFromJson(modId, data.getJSONObject(i)));
            }
            return versions;
        }
    }

    @Override
    public ModVersion getVersion(String versionId) throws Exception {
        // CurseForge gets file by /mods/{modId}/files/{fileId}
        // If modId unknown, can return null or fetch file directly if supported
        return null;
    }

    private ModVersion parseVersionFromJson(String modId, JSONObject fileObj) {
        String fileId = String.valueOf(fileObj.optInt("id", 0));
        String displayName = fileObj.optString("displayName", fileId);
        String fileName = fileObj.optString("fileName", "mod.jar");
        String downloadUrl = fileObj.optString("downloadUrl", "");
        long fileLength = fileObj.optLong("fileLength", 0);
        String fileDate = fileObj.optString("fileDate", "");

        List<String> gameVersions = new ArrayList<>();
        List<Loader> loaders = new ArrayList<>();
        JSONArray gvArr = fileObj.optJSONArray("gameVersions");
        if (gvArr != null) {
            for (int j = 0; j < gvArr.length(); j++) {
                String gv = gvArr.optString(j);
                Loader l = Loader.fromSlug(gv);
                if (l != Loader.UNKNOWN) {
                    if (!loaders.contains(l)) loaders.add(l);
                } else if (!gv.isEmpty()) {
                    gameVersions.add(gv);
                }
            }
        }

        List<ModFile> files = new ArrayList<>();
        files.add(new ModFile(fileId, downloadUrl, fileName, true, fileLength, "", ""));

        List<Dependency> dependencies = new ArrayList<>();
        JSONArray depArr = fileObj.optJSONArray("dependencies");
        if (depArr != null) {
            for (int j = 0; j < depArr.length(); j++) {
                JSONObject dObj = depArr.optJSONObject(j);
                if (dObj == null) continue;
                int depModId = dObj.optInt("modId", 0);
                int relationType = dObj.optInt("relationType", 3);
                DependencyType type = DependencyType.REQUIRED;
                if (relationType == 2) type = DependencyType.OPTIONAL;
                else if (relationType == 1) type = DependencyType.EMBEDDED;
                else if (relationType == 4) type = DependencyType.INCOMPATIBLE;

                dependencies.add(new Dependency(String.valueOf(depModId), "", type, "", "CurseForge Mod #" + depModId, Source.CURSEFORGE));
            }
        }

        return new ModVersion(fileId, modId, displayName, displayName, "",
                gameVersions, loaders, files, dependencies, Source.CURSEFORGE, fileDate);
    }
}
