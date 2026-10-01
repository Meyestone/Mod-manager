package com.example.model;

public class ModFile {
    private final String fileId;
    private final String url;
    private final String fileName;
    private final boolean isPrimary;
    private final long sizeBytes;
    private final String sha1;
    private final String sha512;

    public ModFile(String fileId, String url, String fileName, boolean isPrimary, long sizeBytes, String sha1, String sha512) {
        this.fileId = fileId != null ? fileId : "";
        this.url = url != null ? url : "";
        this.fileName = fileName != null ? fileName : "mod.jar";
        this.isPrimary = isPrimary;
        this.sizeBytes = sizeBytes;
        this.sha1 = sha1 != null ? sha1 : "";
        this.sha512 = sha512 != null ? sha512 : "";
    }

    public String getFileId() {
        return fileId;
    }

    public String getUrl() {
        return url;
    }

    public String getFileName() {
        return fileName;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getSha1() {
        return sha1;
    }

    public String getSha512() {
        return sha512;
    }

    public String getFormattedSize() {
        if (sizeBytes <= 0) return "Unknown size";
        if (sizeBytes < 1024) return sizeBytes + " B";
        if (sizeBytes < 1024 * 1024) return String.format(java.util.Locale.US, "%.1f KB", sizeBytes / 1024.0);
        return String.format(java.util.Locale.US, "%.2f MB", sizeBytes / (1024.0 * 1024.0));
    }
}
