package com.example.data.provider;

import com.example.model.Loader;
import com.example.model.Mod;
import com.example.model.ModVersion;
import com.example.model.Source;

import java.util.List;

public interface ModProvider {
    Source getSource();
    boolean isConfigured();
    String getConfigurationNote();
    List<Mod> searchMods(String query, String gameVersion, Loader loader, int offset, int limit) throws Exception;
    Mod getMod(String idOrSlug) throws Exception;
    List<ModVersion> getVersions(String modId, String gameVersion, Loader loader) throws Exception;
    ModVersion getVersion(String versionId) throws Exception;
}
