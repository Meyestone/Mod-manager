package com.example.data.launcher;

import android.content.Context;
import android.net.Uri;

import com.example.model.Instance;
import com.example.model.LauncherType;

import java.util.List;

public interface LauncherProvider {
    LauncherType getType();
    String getDisplayName();
    String getPackageName();
    boolean isPackageInstalled(Context context);
    List<Instance> discoverInstances(Context context, Uri customRootUri);
}
