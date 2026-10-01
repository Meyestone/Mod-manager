package com.example;

import android.app.Application;

import com.example.data.provider.CurseForgeProvider;
import com.example.data.provider.ModrinthProvider;
import com.example.data.storage.StorageManager;
import com.example.domain.DependencyResolver;
import com.example.domain.InstallationManager;
import com.example.domain.InstanceRepository;
import com.example.domain.ModRepository;
import com.example.domain.UpdateManager;

public class ModManageApplication extends Application {
    private static ModManageApplication instance;

    private StorageManager storageManager;
    private ModrinthProvider modrinthProvider;
    private CurseForgeProvider curseForgeProvider;
    private ModRepository modRepository;
    private InstanceRepository instanceRepository;
    private DependencyResolver dependencyResolver;
    private InstallationManager installationManager;
    private UpdateManager updateManager;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        storageManager = new StorageManager(this);
        modrinthProvider = new ModrinthProvider();
        curseForgeProvider = new CurseForgeProvider(storageManager.getCurseForgeApiKey());
        modRepository = new ModRepository(modrinthProvider, curseForgeProvider);
        instanceRepository = new InstanceRepository(this, storageManager);
        dependencyResolver = new DependencyResolver(modRepository);
        installationManager = new InstallationManager(storageManager);
        updateManager = new UpdateManager(modRepository, installationManager);
    }

    public static ModManageApplication getInstance() {
        return instance;
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }

    public ModrinthProvider getModrinthProvider() {
        return modrinthProvider;
    }

    public CurseForgeProvider getCurseForgeProvider() {
        return curseForgeProvider;
    }

    public ModRepository getModRepository() {
        return modRepository;
    }

    public InstanceRepository getInstanceRepository() {
        return instanceRepository;
    }

    public DependencyResolver getDependencyResolver() {
        return dependencyResolver;
    }

    public InstallationManager getInstallationManager() {
        return installationManager;
    }

    public UpdateManager getUpdateManager() {
        return updateManager;
    }
}
