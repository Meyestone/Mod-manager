package com.example.model;

public class DownloadTask {
    public enum Status {
        PENDING,
        DOWNLOADING,
        VERIFYING,
        INSTALLING,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    private final String taskId;
    private final String modId;
    private final String modName;
    private final String iconUrl;
    private final String versionNumber;
    private final String downloadUrl;
    private final String targetFileName;
    private final String targetInstanceId;
    private final int progress;
    private final long bytesDownloaded;
    private final long totalBytes;
    private final Status status;
    private final String errorMessage;

    public DownloadTask(String taskId, String modId, String modName, String iconUrl,
                        String versionNumber, String downloadUrl, String targetFileName,
                        String targetInstanceId, int progress, long bytesDownloaded,
                        long totalBytes, Status status, String errorMessage) {
        this.taskId = taskId != null ? taskId : java.util.UUID.randomUUID().toString();
        this.modId = modId != null ? modId : "";
        this.modName = modName != null ? modName : "Mod";
        this.iconUrl = iconUrl != null ? iconUrl : "";
        this.versionNumber = versionNumber != null ? versionNumber : "";
        this.downloadUrl = downloadUrl != null ? downloadUrl : "";
        this.targetFileName = targetFileName != null ? targetFileName : "mod.jar";
        this.targetInstanceId = targetInstanceId != null ? targetInstanceId : "";
        this.progress = progress;
        this.bytesDownloaded = bytesDownloaded;
        this.totalBytes = totalBytes;
        this.status = status != null ? status : Status.PENDING;
        this.errorMessage = errorMessage != null ? errorMessage : "";
    }

    public String getTaskId() {
        return taskId;
    }

    public String getModId() {
        return modId;
    }

    public String getModName() {
        return modName;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public String getVersionNumber() {
        return versionNumber;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public String getTargetFileName() {
        return targetFileName;
    }

    public String getTargetInstanceId() {
        return targetInstanceId;
    }

    public int getProgress() {
        return progress;
    }

    public long getBytesDownloaded() {
        return bytesDownloaded;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public Status getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public DownloadTask withProgress(int progress, long downloaded, long total) {
        return new DownloadTask(taskId, modId, modName, iconUrl, versionNumber,
                downloadUrl, targetFileName, targetInstanceId, progress, downloaded, total,
                Status.DOWNLOADING, "");
    }

    public DownloadTask withStatus(Status newStatus, String error) {
        return new DownloadTask(taskId, modId, modName, iconUrl, versionNumber,
                downloadUrl, targetFileName, targetInstanceId, progress, bytesDownloaded,
                totalBytes, newStatus, error != null ? error : errorMessage);
    }
}
