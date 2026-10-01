package com.example.data.launcher;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;

import com.example.model.Instance;
import com.example.model.LauncherType;
import com.example.model.Loader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AmethystLauncherProvider implements LauncherProvider {
    private static final String MAIN_PACKAGE = "org.amethyst.launcher";
    private static final String ALT_PACKAGE = "com.amethyst.launcher";

    @Override
    public LauncherType getType() {
        return LauncherType.AMETHYST_LAUNCHER;
    }

    @Override
    public String getDisplayName() {
        return "Amethyst Launcher";
    }

    @Override
    public String getPackageName() {
        return MAIN_PACKAGE;
    }

    @Override
    public boolean isPackageInstalled(Context context) {
        if (context == null) return false;
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(MAIN_PACKAGE, 0);
            return true;
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        try {
            pm.getPackageInfo(ALT_PACKAGE, 0);
            return true;
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        return false;
    }

    @Override
    public List<Instance> discoverInstances(Context context, Uri customRootUri) {
        List<Instance> instances = new ArrayList<>();

        if (customRootUri != null) {
            String uriStr = customRootUri.toString();
            instances.add(new Instance(
                    "amethyst_custom_main",
                    "Amethyst — Default Instance",
                    "1.21.1",
                    Loader.FABRIC,
                    "0.16.9",
                    LauncherType.AMETHYST_LAUNCHER,
                    uriStr,
                    uriStr + "/mods",
                    true,
                    0,
                    false
            ));
            return instances;
        }

        // Try standard storage directories
        File amethystDir = new File("/sdcard/AmethystLauncher/instances");
        if (!amethystDir.exists()) {
            amethystDir = new File(context.getExternalFilesDir(null), "amethyst_instances");
        }

        if (amethystDir.exists() && amethystDir.isDirectory()) {
            File[] files = amethystDir.listFiles();
            if (files != null) {
                for (File instDir : files) {
                    if (instDir.isDirectory()) {
                        File modsDir = new File(instDir, "mods");
                        if (!modsDir.exists()) {
                            modsDir = new File(new File(instDir, ".minecraft"), "mods");
                        }
                        int modCount = countJarFiles(modsDir);
                        instances.add(new Instance(
                                "amethyst_" + instDir.getName(),
                                "Amethyst: " + instDir.getName(),
                                "1.21.1",
                                Loader.FABRIC,
                                "0.16.9",
                                LauncherType.AMETHYST_LAUNCHER,
                                instDir.getAbsolutePath(),
                                modsDir.getAbsolutePath(),
                                modsDir.exists() && modsDir.canWrite(),
                                modCount,
                                false
                        ));
                    }
                }
            }
        }

        if (instances.isEmpty()) {
            instances.add(new Instance(
                    "amethyst_fabric_1201",
                    "Amethyst — Fabric 1.20.1",
                    "1.20.1",
                    Loader.FABRIC,
                    "0.15.11",
                    LauncherType.AMETHYST_LAUNCHER,
                    amethystDir.getAbsolutePath() + "/Fabric_1201",
                    amethystDir.getAbsolutePath() + "/Fabric_1201/mods",
                    false,
                    0,
                    false
            ));
        }

        return instances;
    }

    private int countJarFiles(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) return 0;
        File[] files = dir.listFiles();
        if (files == null) return 0;
        int count = 0;
        for (File f : files) {
            if (f.isFile() && (f.getName().endsWith(".jar") || f.getName().endsWith(".jar.disabled"))) {
                count++;
            }
        }
        return count;
    }
}
