package com.example.domain;

import com.example.model.Dependency;
import com.example.model.DependencyType;
import com.example.model.InstalledMod;
import com.example.model.Instance;
import com.example.model.Loader;
import com.example.model.Mod;
import com.example.model.ModVersion;
import com.example.model.Source;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DependencyResolver {
    private final ModRepository modRepository;

    public DependencyResolver(ModRepository modRepository) {
        this.modRepository = modRepository;
    }

    public static class ResolutionPlan {
        private final Mod targetMod;
        private final ModVersion targetVersion;
        private final List<ModVersion> requiredDependenciesToInstall;
        private final List<Dependency> optionalDependencies;
        private final List<String> incompatibleReasons;
        private final boolean isCompatible;

        public ResolutionPlan(Mod targetMod, ModVersion targetVersion,
                              List<ModVersion> requiredDependenciesToInstall,
                              List<Dependency> optionalDependencies,
                              List<String> incompatibleReasons,
                              boolean isCompatible) {
            this.targetMod = targetMod;
            this.targetVersion = targetVersion;
            this.requiredDependenciesToInstall = requiredDependenciesToInstall != null ? requiredDependenciesToInstall : Collections.emptyList();
            this.optionalDependencies = optionalDependencies != null ? optionalDependencies : Collections.emptyList();
            this.incompatibleReasons = incompatibleReasons != null ? incompatibleReasons : Collections.emptyList();
            this.isCompatible = isCompatible;
        }

        public Mod getTargetMod() {
            return targetMod;
        }

        public ModVersion getTargetVersion() {
            return targetVersion;
        }

        public List<ModVersion> getRequiredDependenciesToInstall() {
            return requiredDependenciesToInstall;
        }

        public List<Dependency> getOptionalDependencies() {
            return optionalDependencies;
        }

        public List<String> getIncompatibleReasons() {
            return incompatibleReasons;
        }

        public boolean isCompatible() {
            return isCompatible;
        }

        public int getTotalModsToInstallCount() {
            return 1 + requiredDependenciesToInstall.size();
        }
    }

    public ResolutionPlan resolveDependencies(Mod mod, ModVersion version, Instance instance, List<InstalledMod> installedMods) {
        List<String> incompatibleReasons = new ArrayList<>();
        List<ModVersion> toInstall = new ArrayList<>();
        List<Dependency> optionals = new ArrayList<>();

        // 1. Check Minecraft version compatibility
        if (!version.getGameVersions().isEmpty()) {
            boolean mcCompatible = false;
            for (String gv : version.getGameVersions()) {
                if (gv.equalsIgnoreCase(instance.getMinecraftVersion())) {
                    mcCompatible = true;
                    break;
                }
            }
            if (!mcCompatible) {
                incompatibleReasons.add("Mod version " + version.getVersionNumber() + " is built for Minecraft " +
                        version.getGameVersions() + ", but instance is " + instance.getMinecraftVersion() + ".");
            }
        }

        // 2. Check Loader compatibility
        if (!version.getLoaders().isEmpty()) {
            boolean loaderCompatible = false;
            for (Loader l : version.getLoaders()) {
                if (instance.getLoader().isCompatibleWith(l)) {
                    loaderCompatible = true;
                    break;
                }
            }
            if (!loaderCompatible) {
                incompatibleReasons.add("Mod requires " + version.getLoaders() + ", but instance uses " + instance.getLoader().getDisplayName() + ".");
            }
        }

        // 3. Inspect dependencies
        Set<String> visited = new HashSet<>();
        visited.add(version.getProjectId());
        visited.add(version.getVersionId());

        resolveDependenciesRecursive(version, instance, installedMods, toInstall, optionals, incompatibleReasons, visited);

        boolean compatible = incompatibleReasons.isEmpty();
        return new ResolutionPlan(mod, version, toInstall, optionals, incompatibleReasons, compatible);
    }

    private void resolveDependenciesRecursive(ModVersion version, Instance instance, List<InstalledMod> installedMods,
                                              List<ModVersion> outToInstall, List<Dependency> outOptionals,
                                              List<String> outIncompatibleReasons, Set<String> visited) {
        for (Dependency dep : version.getDependencies()) {
            if (dep.getDependencyType() == DependencyType.OPTIONAL) {
                if (!outOptionals.contains(dep)) {
                    outOptionals.add(dep);
                }
                continue;
            }

            if (dep.getDependencyType() == DependencyType.INCOMPATIBLE) {
                if (isDependencyAlreadyInstalled(dep, installedMods)) {
                    outIncompatibleReasons.add("Incompatible mod is currently installed: " + dep.getTitle());
                }
                continue;
            }

            if (dep.getDependencyType() == DependencyType.REQUIRED) {
                // If already installed in the instance, skip
                if (isDependencyAlreadyInstalled(dep, installedMods)) {
                    continue;
                }

                // Check circular dependency
                String idKey = dep.getProjectId() + "_" + dep.getSource();
                if (visited.contains(idKey)) {
                    continue; // circular dependency handled safely
                }
                visited.add(idKey);

                // Fetch compatible version of this required dependency
                try {
                    ModVersion depVersion = findCompatibleDependencyVersion(dep, instance);
                    if (depVersion != null) {
                        outToInstall.add(depVersion);
                        // Recursively resolve dependency's own required dependencies
                        resolveDependenciesRecursive(depVersion, instance, installedMods, outToInstall, outOptionals, outIncompatibleReasons, visited);
                    } else {
                        outIncompatibleReasons.add("Missing required dependency: '" + dep.getTitle() +
                                "' could not be found for Minecraft " + instance.getMinecraftVersion() + " (" + instance.getLoader().getDisplayName() + ").");
                    }
                } catch (Exception e) {
                    outIncompatibleReasons.add("Failed to resolve required dependency '" + dep.getTitle() + "': " + e.getMessage());
                }
            }
        }
    }

    private boolean isDependencyAlreadyInstalled(Dependency dep, List<InstalledMod> installedMods) {
        if (installedMods == null) return false;
        String query = dep.getTitle().toLowerCase().replaceAll("[^a-z0-9]", "");
        String idQuery = dep.getProjectId().toLowerCase();

        for (InstalledMod inst : installedMods) {
            String instName = inst.getName().toLowerCase().replaceAll("[^a-z0-9]", "");
            String instId = inst.getModId().toLowerCase();
            String fileName = inst.getFileName().toLowerCase();

            if (instId.equals(idQuery) || instName.contains(query) || query.contains(instName) || fileName.contains(query)) {
                return true;
            }
        }
        return false;
    }

    private ModVersion findCompatibleDependencyVersion(Dependency dep, Instance instance) throws Exception {
        if (dep.getSource() == Source.MODRINTH) {
            List<ModVersion> versions = modRepository.getVersions(
                    dep.getProjectId(),
                    Source.MODRINTH,
                    instance.getMinecraftVersion(),
                    instance.getLoader()
            );
            if (versions != null && !versions.isEmpty()) {
                return versions.get(0);
            }
        } else {
            List<ModVersion> versions = modRepository.getVersions(
                    dep.getProjectId(),
                    Source.CURSEFORGE,
                    instance.getMinecraftVersion(),
                    instance.getLoader()
            );
            if (versions != null && !versions.isEmpty()) {
                return versions.get(0);
            }
        }
        return null;
    }
}
