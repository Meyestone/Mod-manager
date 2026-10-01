package com.example.data.storage;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import com.example.model.InstalledMod;
import com.example.model.Instance;
import com.example.model.Loader;
import com.example.model.Source;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class StorageManager {
    private static final String PREFS_NAME = "mod_manage_storage";
    private static final String KEY_GRANTED_URIS = "granted_tree_uris";
    private static final String KEY_SELECTED_INSTANCE_ID = "selected_instance_id";
    private static final String KEY_CURSEFORGE_KEY = "curseforge_api_key";
    private static final String KEY_AUTO_INSTALL_DEPS = "auto_install_dependencies";
    private static final String KEY_DEFAULT_MC_VERSION = "default_mc_version";
    private static final String KEY_DEFAULT_LOADER = "default_loader";
    private static final String KEY_ONBOARDING_COMPLETED = "onboarding_completed";

    private final Context context;
    private final SharedPreferences prefs;

    public StorageManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveGrantedUri(Uri treeUri) {
        if (treeUri == null) return;
        try {
            int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
            context.getContentResolver().takePersistableUriPermission(treeUri, takeFlags);
        } catch (Exception ignored) {
        }
        prefs.edit().putString(KEY_GRANTED_URIS, treeUri.toString()).apply();
    }

    public Uri getGrantedUri() {
        String uriStr = prefs.getString(KEY_GRANTED_URIS, null);
        return uriStr != null ? Uri.parse(uriStr) : null;
    }

    public void setSelectedInstanceId(String instanceId) {
        prefs.edit().putString(KEY_SELECTED_INSTANCE_ID, instanceId).apply();
    }

    public String getSelectedInstanceId() {
        return prefs.getString(KEY_SELECTED_INSTANCE_ID, null);
    }

    public void setCurseForgeApiKey(String key) {
        prefs.edit().putString(KEY_CURSEFORGE_KEY, key != null ? key.trim() : "").apply();
    }

    public String getCurseForgeApiKey() {
        return prefs.getString(KEY_CURSEFORGE_KEY, "");
    }

    public void setAutoInstallDependencies(boolean autoInstall) {
        prefs.edit().putBoolean(KEY_AUTO_INSTALL_DEPS, autoInstall).apply();
    }

    public boolean isAutoInstallDependencies() {
        return prefs.getBoolean(KEY_AUTO_INSTALL_DEPS, true);
    }

    public void setDefaultMinecraftVersion(String version) {
        prefs.edit().putString(KEY_DEFAULT_MC_VERSION, version).apply();
    }

    public String getDefaultMinecraftVersion() {
        return prefs.getString(KEY_DEFAULT_MC_VERSION, "1.21.1");
    }

    public void setDefaultLoader(Loader loader) {
        prefs.edit().putString(KEY_DEFAULT_LOADER, loader != null ? loader.name() : Loader.FABRIC.name()).apply();
    }

    public Loader getDefaultLoader() {
        String name = prefs.getString(KEY_DEFAULT_LOADER, Loader.FABRIC.name());
        try {
            return Loader.valueOf(name);
        } catch (Exception ignored) {
            return Loader.FABRIC;
        }
    }

    public void setOnboardingCompleted(boolean completed) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply();
    }

    public boolean isOnboardingCompleted() {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false);
    }

    public List<InstalledMod> scanInstalledMods(Instance instance) {
        if (instance == null) return Collections.emptyList();
        List<InstalledMod> mods = new ArrayList<>();

        File modsDir = getModsDirectoryForInstance(instance);
        if (modsDir.exists() && modsDir.isDirectory()) {
            File[] files = modsDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        String name = file.getName();
                        if (name.endsWith(".jar") || name.endsWith(".jar.disabled")) {
                            boolean isEnabled = name.endsWith(".jar");
                            InstalledMod parsed = inspectJarFile(file, isEnabled, instance.getLoader());
                            mods.add(parsed);
                        }
                    }
                }
            }
        }

        return mods;
    }

    public File getModsDirectoryForInstance(Instance instance) {
        // First try instance modsPathUri as File
        if (instance.getModsPathUri() != null && !instance.getModsPathUri().isEmpty() && !instance.getModsPathUri().startsWith("content://")) {
            File dir = new File(instance.getModsPathUri());
            if (!dir.exists()) dir.mkdirs();
            return dir;
        }

        // Dedicated local storage fallback per instance
        File appInstances = new File(context.getFilesDir(), "instances/" + instance.getId() + "/mods");
        if (!appInstances.exists()) {
            appInstances.mkdirs();
        }
        return appInstances;
    }

    public boolean toggleModEnabled(Instance instance, InstalledMod mod) {
        File modsDir = getModsDirectoryForInstance(instance);
        File currentFile = new File(modsDir, mod.getFileName());
        if (!currentFile.exists()) return false;

        String targetName;
        if (mod.isEnabled()) {
            targetName = mod.getFileName() + ".disabled";
        } else {
            if (mod.getFileName().endsWith(".disabled")) {
                targetName = mod.getFileName().substring(0, mod.getFileName().length() - ".disabled".length());
            } else {
                targetName = mod.getFileName() + ".jar";
            }
        }

        File targetFile = new File(modsDir, targetName);
        return currentFile.renameTo(targetFile);
    }

    public boolean deleteMod(Instance instance, InstalledMod mod) {
        File modsDir = getModsDirectoryForInstance(instance);
        File file = new File(modsDir, mod.getFileName());
        return file.exists() && file.delete();
    }

    public File createTempDownloadFile(String fileName) {
        File tempDir = new File(context.getCacheDir(), "mod_downloads");
        if (!tempDir.exists()) tempDir.mkdirs();
        return new File(tempDir, fileName + ".tmp");
    }

    public boolean installFileToInstance(File sourceTempFile, String finalFileName, Instance instance) {
        if (sourceTempFile == null || !sourceTempFile.exists()) return false;
        File targetDir = getModsDirectoryForInstance(instance);
        if (!targetDir.exists()) targetDir.mkdirs();

        File destination = new File(targetDir, finalFileName);
        try (InputStream in = new FileInputStream(sourceTempFile);
             OutputStream out = new FileOutputStream(destination)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.flush();
            sourceTempFile.delete();
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * Inspects a jar file safely without executing it to extract mod metadata.
     */
    private InstalledMod inspectJarFile(File file, boolean isEnabled, Loader fallbackLoader) {
        String fileName = file.getName();
        String modName = fileName.replace(".jar.disabled", "").replace(".jar", "");
        String version = "1.0.0";
        String modId = modName;
        String iconUrl = "";
        Loader detectedLoader = fallbackLoader;
        List<String> dependencies = new ArrayList<>();

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(file))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();
                if ("fabric.mod.json".equals(entryName)) {
                    detectedLoader = Loader.FABRIC;
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        baos.write(buffer, 0, len);
                    }
                    JSONObject json = new JSONObject(baos.toString("UTF-8"));
                    modId = json.optString("id", modId);
                    modName = json.optString("name", modName);
                    version = json.optString("version", version);

                    JSONObject depends = json.optJSONObject("depends");
                    if (depends != null) {
                        Iterator<String> it = depends.keys();
                        while (it.hasNext()) {
                            dependencies.add(it.next());
                        }
                    }
                    break;
                } else if ("quilt.mod.json".equals(entryName)) {
                    detectedLoader = Loader.QUILT;
                    break;
                } else if ("META-INF/mods.toml".equals(entryName)) {
                    detectedLoader = Loader.FORGE;
                    break;
                } else if ("META-INF/neoforge.mods.toml".equals(entryName)) {
                    detectedLoader = Loader.NEOFORGE;
                    break;
                }
            }
        } catch (Exception ignored) {
        }

        return new InstalledMod(
                modId,
                modName,
                fileName,
                version,
                detectedLoader,
                Source.MODRINTH,
                isEnabled,
                file.length(),
                file.lastModified(),
                modId,
                iconUrl,
                "",
                dependencies,
                false,
                "",
                ""
        );
    }
}
