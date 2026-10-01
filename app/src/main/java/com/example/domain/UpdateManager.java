package com.example.domain;

import com.example.model.InstalledMod;
import com.example.model.Instance;
import com.example.model.ModVersion;
import com.example.model.Source;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UpdateManager {
    private final ModRepository modRepository;
    private final InstallationManager installationManager;

    public UpdateManager(ModRepository modRepository, InstallationManager installationManager) {
        this.modRepository = modRepository;
        this.installationManager = installationManager;
    }

    public static class UpdateCheckResult {
        public final InstalledMod originalMod;
        public final ModVersion latestVersion;

        public UpdateCheckResult(InstalledMod originalMod, ModVersion latestVersion) {
            this.originalMod = originalMod;
            this.latestVersion = latestVersion;
        }
    }

    public List<UpdateCheckResult> checkForUpdates(Instance instance, List<InstalledMod> installedMods) {
        if (instance == null || installedMods == null || installedMods.isEmpty()) {
            return Collections.emptyList();
        }

        List<UpdateCheckResult> updates = new ArrayList<>();
        for (InstalledMod mod : installedMods) {
            try {
                List<ModVersion> versions = modRepository.getVersions(
                        mod.getModId(),
                        mod.getSource(),
                        instance.getMinecraftVersion(),
                        instance.getLoader()
                );
                if (versions != null && !versions.isEmpty()) {
                    ModVersion newest = versions.get(0);
                    // Compare version numbers
                    if (!newest.getVersionNumber().equalsIgnoreCase(mod.getVersion()) &&
                        !mod.getVersion().equals("1.0.0")) {
                        updates.add(new UpdateCheckResult(mod, newest));
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return updates;
    }

    public void updateMod(InstalledMod mod, ModVersion newVersion, Instance instance) {
        installationManager.queueVersionDownload(mod.getName(), newVersion, instance);
    }
}
