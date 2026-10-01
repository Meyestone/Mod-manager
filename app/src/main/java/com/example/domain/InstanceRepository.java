package com.example.domain;

import android.content.Context;
import android.net.Uri;

import com.example.data.launcher.AmethystLauncherProvider;
import com.example.data.launcher.LauncherProvider;
import com.example.data.launcher.PojavLauncherProvider;
import com.example.data.storage.StorageManager;
import com.example.model.InstalledMod;
import com.example.model.Instance;
import com.example.model.LauncherType;
import com.example.model.Loader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InstanceRepository {
    private final Context context;
    private final StorageManager storageManager;
    private final List<LauncherProvider> launcherProviders;
    private final List<Instance> instances = new ArrayList<>();
    private Instance activeInstance;

    public InstanceRepository(Context context, StorageManager storageManager) {
        this.context = context.getApplicationContext();
        this.storageManager = storageManager;
        this.launcherProviders = new ArrayList<>();
        this.launcherProviders.add(new PojavLauncherProvider());
        this.launcherProviders.add(new AmethystLauncherProvider());
    }

    public boolean isPojavInstalled() {
        for (LauncherProvider p : launcherProviders) {
            if (p.getType() == LauncherType.POJAV_LAUNCHER) return p.isPackageInstalled(context);
        }
        return false;
    }

    public boolean isAmethystInstalled() {
        for (LauncherProvider p : launcherProviders) {
            if (p.getType() == LauncherType.AMETHYST_LAUNCHER) return p.isPackageInstalled(context);
        }
        return false;
    }

    public boolean isAnyLauncherAvailable() {
        return isPojavInstalled() || isAmethystInstalled() || storageManager.getGrantedUri() != null;
    }

    public synchronized List<Instance> refreshInstances() {
        instances.clear();
        Uri customUri = storageManager.getGrantedUri();

        for (LauncherProvider provider : launcherProviders) {
            try {
                List<Instance> found = provider.discoverInstances(context, customUri);
                if (found != null) {
                    for (Instance inst : found) {
                        List<InstalledMod> mods = storageManager.scanInstalledMods(inst);
                        instances.add(inst.withInstalledModCount(mods.size()));
                    }
                }
            } catch (Exception ignored) {
            }
        }

        // Restore selected instance or choose the first one
        String savedId = storageManager.getSelectedInstanceId();
        if (savedId != null) {
            for (Instance inst : instances) {
                if (inst.getId().equals(savedId)) {
                    activeInstance = inst;
                    break;
                }
            }
        }

        if (activeInstance == null && !instances.isEmpty()) {
            activeInstance = instances.get(0);
            storageManager.setSelectedInstanceId(activeInstance.getId());
        }

        return Collections.unmodifiableList(instances);
    }

    public synchronized List<Instance> getInstances() {
        if (instances.isEmpty()) {
            return refreshInstances();
        }
        return Collections.unmodifiableList(instances);
    }

    public synchronized Instance getActiveInstance() {
        if (activeInstance == null) {
            getInstances();
        }
        return activeInstance;
    }

    public synchronized void setActiveInstance(Instance instance) {
        if (instance == null) return;
        this.activeInstance = instance;
        this.storageManager.setSelectedInstanceId(instance.getId());
    }

    public synchronized void addCustomInstance(String name, String mcVersion, Loader loader, String loaderVersion) {
        String id = "custom_" + System.currentTimeMillis();
        Instance custom = new Instance(
                id,
                name != null && !name.trim().isEmpty() ? name : "Custom Instance",
                mcVersion != null ? mcVersion : "1.21.1",
                loader != null ? loader : Loader.FABRIC,
                loaderVersion != null ? loaderVersion : "",
                LauncherType.CUSTOM,
                "",
                "",
                true,
                0,
                instances.isEmpty()
        );
        instances.add(custom);
        if (activeInstance == null) {
            setActiveInstance(custom);
        }
    }
}
