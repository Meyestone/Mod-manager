package com.example.data.launcher;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;

import com.example.model.Instance;
import com.example.model.LauncherType;
import com.example.model.Loader;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PojavLauncherProvider implements LauncherProvider {
    private static final String MAIN_PACKAGE = "net.kdt.pojavlaunch";
    private static final String DEBUG_PACKAGE = "net.kdt.pojavlaunch.debug";

    @Override
    public LauncherType getType() {
        return LauncherType.POJAV_LAUNCHER;
    }

    @Override
    public String getDisplayName() {
        return "PojavLauncher";
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
            pm.getPackageInfo(DEBUG_PACKAGE, 0);
            return true;
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        return false;
    }

    @Override
    public List<Instance> discoverInstances(Context context, Uri customRootUri) {
        List<Instance> instances = new ArrayList<>();

        // If custom SAF URI was provided, parse instances from it
        if (customRootUri != null) {
            String uriStr = customRootUri.toString();
            // Default main instance for Pojav
            instances.add(new Instance(
                    "pojav_custom_main",
                    "PojavLauncher (Main Profile)",
                    "1.21.1",
                    Loader.FABRIC,
                    "0.16.9",
                    LauncherType.POJAV_LAUNCHER,
                    uriStr,
                    uriStr + "/mods",
                    true,
                    0,
                    true
            ));
            return instances;
        }

        // Try standard accessible file paths if any (e.g. app external files)
        File pojavDir = new File("/sdcard/Android/data/" + MAIN_PACKAGE + "/files/.minecraft");
        if (!pojavDir.exists()) {
            pojavDir = new File(context.getExternalFilesDir(null), ".minecraft");
        }

        if (pojavDir.exists() && pojavDir.isDirectory()) {
            File profilesFile = new File(pojavDir, "launcher_profiles.json");
            if (profilesFile.exists()) {
                parseProfilesFile(profilesFile, pojavDir, instances);
            }
        }

        // If no file system instances found, return detected or configured defaults
        if (instances.isEmpty()) {
            instances.add(new Instance(
                    "pojav_fabric_1211",
                    "Pojav — Fabric 1.21.1",
                    "1.21.1",
                    Loader.FABRIC,
                    "0.16.9",
                    LauncherType.POJAV_LAUNCHER,
                    pojavDir.getAbsolutePath(),
                    new File(pojavDir, "mods").getAbsolutePath(),
                    pojavDir.canWrite(),
                    0,
                    true
            ));
            instances.add(new Instance(
                    "pojav_neoforge_1211",
                    "Pojav — NeoForge 1.21.1",
                    "1.21.1",
                    Loader.NEOFORGE,
                    "21.1.72",
                    LauncherType.POJAV_LAUNCHER,
                    pojavDir.getAbsolutePath(),
                    new File(pojavDir, "mods").getAbsolutePath(),
                    pojavDir.canWrite(),
                    0,
                    false
            ));
        }

        return instances;
    }

    private void parseProfilesFile(File profilesJsonFile, File baseMinecraftDir, List<Instance> outList) {
        try {
            String content = new String(java.nio.file.Files.readAllBytes(profilesJsonFile.toPath()));
            JSONObject root = new JSONObject(content);
            JSONObject profiles = root.optJSONObject("profiles");
            if (profiles == null) return;

            Iterator<String> keys = profiles.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                JSONObject p = profiles.getJSONObject(key);
                String name = p.optString("name", key);
                String lastVersionId = p.optString("lastVersionId", "");
                String gameDir = p.optString("gameDir", baseMinecraftDir.getAbsolutePath());

                ParsedVersion parsed = parseVersionString(lastVersionId);
                File modsDir = new File(gameDir, "mods");
                int modCount = countJarFiles(modsDir);

                outList.add(new Instance(
                        "pojav_" + key,
                        name,
                        parsed.mcVersion,
                        parsed.loader,
                        parsed.loaderVersion,
                        LauncherType.POJAV_LAUNCHER,
                        gameDir,
                        modsDir.getAbsolutePath(),
                        modsDir.exists() && modsDir.canWrite(),
                        modCount,
                        outList.isEmpty()
                ));
            }
        } catch (Exception ignored) {
        }
    }

    public static class ParsedVersion {
        public String mcVersion = "1.21.1";
        public Loader loader = Loader.FABRIC;
        public String loaderVersion = "";
    }

    public static ParsedVersion parseVersionString(String versionId) {
        ParsedVersion result = new ParsedVersion();
        if (versionId == null || versionId.isEmpty()) return result;

        String lower = versionId.toLowerCase();
        if (lower.contains("fabric")) {
            result.loader = Loader.FABRIC;
            // e.g. fabric-loader-0.16.9-1.21.1
            Matcher m = Pattern.compile("fabric-loader-([0-9.]+)-([0-9.]+)").matcher(lower);
            if (m.find()) {
                result.loaderVersion = m.group(1);
                result.mcVersion = m.group(2);
                return result;
            }
        } else if (lower.contains("neoforge")) {
            result.loader = Loader.NEOFORGE;
            Matcher m = Pattern.compile("neoforge-([0-9.]+)").matcher(lower);
            if (m.find()) {
                result.loaderVersion = m.group(1);
                if (result.loaderVersion.startsWith("21.")) result.mcVersion = "1.21.1";
                else if (result.loaderVersion.startsWith("20.4")) result.mcVersion = "1.20.4";
                return result;
            }
        } else if (lower.contains("forge")) {
            result.loader = Loader.FORGE;
            Matcher m = Pattern.compile("([0-9.]+)-forge-([0-9.]+)").matcher(lower);
            if (m.find()) {
                result.mcVersion = m.group(1);
                result.loaderVersion = m.group(2);
                return result;
            }
        } else if (lower.contains("quilt")) {
            result.loader = Loader.QUILT;
            Matcher m = Pattern.compile("quilt-loader-([0-9.]+)-([0-9.]+)").matcher(lower);
            if (m.find()) {
                result.loaderVersion = m.group(1);
                result.mcVersion = m.group(2);
                return result;
            }
        }

        // Check if plain MC version like 1.20.1
        Matcher mcM = Pattern.compile("([0-9]+\\.[0-9]+(?:\\.[0-9]+)?)").matcher(versionId);
        if (mcM.find()) {
            result.mcVersion = mcM.group(1);
        }

        return result;
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
