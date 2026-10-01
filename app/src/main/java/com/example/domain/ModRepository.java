package com.example.domain;

import com.example.data.provider.CurseForgeProvider;
import com.example.data.provider.ModProvider;
import com.example.data.provider.ModrinthProvider;
import com.example.model.Loader;
import com.example.model.Mod;
import com.example.model.ModVersion;
import com.example.model.Source;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ModRepository {
    private final ModrinthProvider modrinthProvider;
    private final CurseForgeProvider curseForgeProvider;

    private boolean modrinthEnabled = true;
    private boolean curseForgeEnabled = true;

    // In-memory cache for fast search responses & offline recovery
    private final Map<String, List<Mod>> searchCache = new ConcurrentHashMap<>();
    private final Map<String, Mod> modCache = new ConcurrentHashMap<>();
    private final Map<String, List<ModVersion>> versionsCache = new ConcurrentHashMap<>();

    public ModRepository(ModrinthProvider modrinthProvider, CurseForgeProvider curseForgeProvider) {
        this.modrinthProvider = modrinthProvider != null ? modrinthProvider : new ModrinthProvider();
        this.curseForgeProvider = curseForgeProvider != null ? curseForgeProvider : new CurseForgeProvider("");
    }

    public boolean isModrinthEnabled() {
        return modrinthEnabled;
    }

    public void setModrinthEnabled(boolean enabled) {
        this.modrinthEnabled = enabled;
    }

    public boolean isCurseForgeEnabled() {
        return curseForgeEnabled;
    }

    public void setCurseForgeEnabled(boolean enabled) {
        this.curseForgeEnabled = enabled;
    }

    public ModrinthProvider getModrinthProvider() {
        return modrinthProvider;
    }

    public CurseForgeProvider getCurseForgeProvider() {
        return curseForgeProvider;
    }

    public static class SearchResult {
        public final List<Mod> mods;
        public final List<String> warnings;
        public final boolean noSourcesActive;

        public SearchResult(List<Mod> mods, List<String> warnings, boolean noSourcesActive) {
            this.mods = mods != null ? mods : Collections.emptyList();
            this.warnings = warnings != null ? warnings : Collections.emptyList();
            this.noSourcesActive = noSourcesActive;
        }
    }

    public SearchResult searchMods(String query, String gameVersion, Loader loader, int offset, int limit) {
        if (!modrinthEnabled && !curseForgeEnabled) {
            return new SearchResult(Collections.emptyList(),
                    Collections.singletonList("Both Modrinth and CurseForge are disabled in source filters. Enable at least one source to browse mods."),
                    true);
        }

        String cacheKey = (query != null ? query : "") + "_" + (gameVersion != null ? gameVersion : "") + "_" +
                (loader != null ? loader.name() : "") + "_" + offset + "_" + limit + "_" + modrinthEnabled + "_" + curseForgeEnabled;

        List<Mod> combined = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // 1. Modrinth
        if (modrinthEnabled) {
            try {
                List<Mod> modrinthResults = modrinthProvider.searchMods(query, gameVersion, loader, offset, limit);
                if (modrinthResults != null) {
                    for (Mod m : modrinthResults) {
                        combined.add(m);
                        modCache.put(m.getSource() + "_" + m.getId(), m);
                        modCache.put(m.getSource() + "_" + m.getSlug(), m);
                    }
                }
            } catch (Exception e) {
                warnings.add("Modrinth: " + (e.getMessage() != null ? e.getMessage() : "Network error"));
            }
        }

        // 2. CurseForge
        if (curseForgeEnabled) {
            if (curseForgeProvider.isConfigured()) {
                try {
                    List<Mod> curseResults = curseForgeProvider.searchMods(query, gameVersion, loader, offset, limit);
                    if (curseResults != null) {
                        for (Mod m : curseResults) {
                            combined.add(m);
                            modCache.put(m.getSource() + "_" + m.getId(), m);
                            modCache.put(m.getSource() + "_" + m.getSlug(), m);
                        }
                    }
                } catch (Exception e) {
                    warnings.add("CurseForge: " + (e.getMessage() != null ? e.getMessage() : "Failed to load"));
                }
            } else {
                warnings.add("CurseForge: API key not set in Settings. Only Modrinth results are shown.");
            }
        }

        // If offline and online search returned empty, check cache
        if (combined.isEmpty() && searchCache.containsKey(cacheKey)) {
            combined.addAll(searchCache.get(cacheKey));
            warnings.add("Showing cached search results (offline mode).");
        } else if (!combined.isEmpty()) {
            searchCache.put(cacheKey, new ArrayList<>(combined));
        }

        return new SearchResult(combined, warnings, false);
    }

    public Mod getModDetails(String idOrSlug, Source source) throws Exception {
        String cacheKey = source + "_" + idOrSlug;
        if (modCache.containsKey(cacheKey)) {
            return modCache.get(cacheKey);
        }

        Mod mod;
        if (source == Source.MODRINTH) {
            mod = modrinthProvider.getMod(idOrSlug);
        } else {
            mod = curseForgeProvider.getMod(idOrSlug);
        }

        if (mod != null) {
            modCache.put(cacheKey, mod);
        }
        return mod;
    }

    public List<ModVersion> getVersions(String modId, Source source, String gameVersion, Loader loader) throws Exception {
        String cacheKey = source + "_" + modId + "_" + (gameVersion != null ? gameVersion : "") + "_" + (loader != null ? loader.name() : "");
        if (versionsCache.containsKey(cacheKey)) {
            return versionsCache.get(cacheKey);
        }

        List<ModVersion> list;
        if (source == Source.MODRINTH) {
            list = modrinthProvider.getVersions(modId, gameVersion, loader);
        } else {
            list = curseForgeProvider.getVersions(modId, gameVersion, loader);
        }

        if (list != null) {
            versionsCache.put(cacheKey, list);
        }
        return list != null ? list : Collections.emptyList();
    }

    public void clearCache() {
        searchCache.clear();
        modCache.clear();
        versionsCache.clear();
    }
}
