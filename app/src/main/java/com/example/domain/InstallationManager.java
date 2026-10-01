package com.example.domain;

import android.os.Handler;
import android.os.Looper;

import com.example.data.storage.StorageManager;
import com.example.model.DownloadTask;
import com.example.model.Instance;
import com.example.model.Mod;
import com.example.model.ModFile;
import com.example.model.ModVersion;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipFile;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class InstallationManager {
    public interface DownloadListener {
        void onTaskUpdated(DownloadTask task);
        void onAllCompleted();
    }

    private final StorageManager storageManager;
    private final OkHttpClient httpClient;
    private final ExecutorService downloadExecutor = Executors.newFixedThreadPool(2);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<DownloadTask> tasks = new ArrayList<>();
    private final List<DownloadListener> listeners = new ArrayList<>();

    public InstallationManager(StorageManager storageManager) {
        this.storageManager = storageManager;
        this.httpClient = new OkHttpClient();
    }

    public synchronized void addListener(DownloadListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public synchronized void removeListener(DownloadListener listener) {
        listeners.remove(listener);
    }

    public synchronized List<DownloadTask> getTasks() {
        return Collections.unmodifiableList(new ArrayList<>(tasks));
    }

    public synchronized void clearCompletedTasks() {
        tasks.removeIf(t -> t.getStatus() == DownloadTask.Status.COMPLETED || t.getStatus() == DownloadTask.Status.FAILED || t.getStatus() == DownloadTask.Status.CANCELLED);
    }

    public void installPlan(DependencyResolver.ResolutionPlan plan, Instance instance) {
        if (plan == null || instance == null) return;

        // 1. Queue all required dependencies
        for (ModVersion depVersion : plan.getRequiredDependenciesToInstall()) {
            queueVersionDownload(depVersion.getName(), depVersion, instance);
        }

        // 2. Queue main mod
        queueVersionDownload(plan.getTargetMod().getName(), plan.getTargetVersion(), instance);
    }

    public void queueVersionDownload(String modName, ModVersion version, Instance instance) {
        ModFile file = version.getPrimaryFile();
        if (file == null || file.getUrl().isEmpty()) return;

        DownloadTask task = new DownloadTask(
                UUID.randomUUID().toString(),
                version.getProjectId(),
                modName,
                "",
                version.getVersionNumber(),
                file.getUrl(),
                file.getFileName(),
                instance.getId(),
                0,
                0,
                file.getSizeBytes(),
                DownloadTask.Status.PENDING,
                ""
        );

        synchronized (this) {
            tasks.add(0, task);
        }
        notifyTaskUpdated(task);

        downloadExecutor.execute(() -> executeDownload(task, file, instance));
    }

    private void executeDownload(DownloadTask task, ModFile file, Instance instance) {
        updateTaskStatus(task, DownloadTask.Status.DOWNLOADING, "");

        File tempFile = storageManager.createTempDownloadFile(task.getTaskId());
        try {
            Request request = new Request.Builder()
                    .url(file.getUrl())
                    .header("User-Agent", "ModManage-Android/1.0.0")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new Exception("HTTP download failed: " + response.code());
                }

                long totalBytes = response.body() != null ? response.body().contentLength() : file.getSizeBytes();
                if (totalBytes <= 0) totalBytes = file.getSizeBytes();

                try (InputStream in = response.body().byteStream();
                     FileOutputStream out = new FileOutputStream(tempFile)) {
                    byte[] buffer = new byte[8192];
                    long downloaded = 0;
                    int read;
                    long lastProgressUpdate = 0;

                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                        downloaded += read;

                        long now = System.currentTimeMillis();
                        if (now - lastProgressUpdate > 150) {
                            lastProgressUpdate = now;
                            int progress = totalBytes > 0 ? (int) ((downloaded * 100) / totalBytes) : 50;
                            updateTaskProgress(task, progress, downloaded, totalBytes);
                        }
                    }
                    out.flush();
                }

                // Verify file is a valid .jar (ZIP header)
                updateTaskStatus(task, DownloadTask.Status.VERIFYING, "");
                try (ZipFile ignored = new ZipFile(tempFile)) {
                    // Valid zip/jar file
                } catch (Exception e) {
                    throw new Exception("Downloaded file is not a valid Minecraft .jar mod archive: " + e.getMessage());
                }

                // Move from temp into instance mods folder
                updateTaskStatus(task, DownloadTask.Status.INSTALLING, "");
                boolean installed = storageManager.installFileToInstance(tempFile, file.getFileName(), instance);
                if (!installed) {
                    throw new Exception("Failed to write mod to instance directory. Check storage permissions.");
                }

                updateTaskStatus(task, DownloadTask.Status.COMPLETED, "");
            }
        } catch (Exception e) {
            if (tempFile.exists()) tempFile.delete();
            updateTaskStatus(task, DownloadTask.Status.FAILED, e.getMessage() != null ? e.getMessage() : "Unknown error");
        } finally {
            checkAllCompleted();
        }
    }

    private void updateTaskProgress(DownloadTask task, int progress, long downloaded, long total) {
        DownloadTask updated = task.withProgress(progress, downloaded, total);
        synchronized (this) {
            int idx = findTaskIndex(task.getTaskId());
            if (idx >= 0) tasks.set(idx, updated);
        }
        notifyTaskUpdated(updated);
    }

    private void updateTaskStatus(DownloadTask task, DownloadTask.Status status, String error) {
        DownloadTask updated = task.withStatus(status, error);
        synchronized (this) {
            int idx = findTaskIndex(task.getTaskId());
            if (idx >= 0) tasks.set(idx, updated);
        }
        notifyTaskUpdated(updated);
    }

    private int findTaskIndex(String taskId) {
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).getTaskId().equals(taskId)) return i;
        }
        return -1;
    }

    private void notifyTaskUpdated(DownloadTask task) {
        mainHandler.post(() -> {
            for (DownloadListener l : listeners) {
                l.onTaskUpdated(task);
            }
        });
    }

    private void checkAllCompleted() {
        boolean allDone = true;
        synchronized (this) {
            for (DownloadTask t : tasks) {
                if (t.getStatus() == DownloadTask.Status.PENDING ||
                    t.getStatus() == DownloadTask.Status.DOWNLOADING ||
                    t.getStatus() == DownloadTask.Status.VERIFYING ||
                    t.getStatus() == DownloadTask.Status.INSTALLING) {
                    allDone = false;
                    break;
                }
            }
        }
        if (allDone) {
            mainHandler.post(() -> {
                for (DownloadListener l : listeners) {
                    l.onAllCompleted();
                }
            });
        }
    }
}
