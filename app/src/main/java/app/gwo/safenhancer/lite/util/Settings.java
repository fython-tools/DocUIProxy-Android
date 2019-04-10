package app.gwo.safenhancer.lite.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import app.gwo.safenhancer.lite.Constants;
import app.gwo.safenhancer.lite.compat.CollectionsCompat;
import app.gwo.safenhancer.lite.compat.Optional;

public final class Settings {

    public static final String PREF_NAME = "settings";

    public static final String KEY_PREFERRED_CAMERA = "preferred_camera";
    public static final String KEY_HANDLED_APPS = "handled_apps";
    public static final String KEY_ROOT_STORAGE_URI = "root_storage_uri";

    private volatile static Settings sInstance = null;

    public static void init(@NonNull Context appContext) {
        sInstance = new Settings(appContext);
    }

    @NonNull
    public static Settings getInstance() {
        return sInstance;
    }

    public static boolean isSourceAppShouldBeHandled(@Nullable String packageName) {
        if (packageName == null) {
            return false;
        }
        if (Constants.WECHAT_PACKAGE_NAME.equals(packageName)) {
            return true;
        }
        return CollectionsCompat.anyMatch(
                getInstance().getHandledApps(),
                item -> item.equals(packageName)
        );
    }

    private final SharedPreferences mPrefs;

    private Settings(@NonNull Context context) {
        mPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    @Nullable
    public ComponentName getPreferredCamera() {
        final String preferredCamera = mPrefs.getString(KEY_PREFERRED_CAMERA, null);
        if (preferredCamera == null) {
            return null;
        } else {
            return ComponentName.unflattenFromString(preferredCamera);
        }
    }

    public void setPreferredCamera(@Nullable ComponentName cn) {
        if (cn == null) {
            mPrefs.edit().remove(KEY_PREFERRED_CAMERA).apply();
        } else {
            mPrefs.edit().putString(KEY_PREFERRED_CAMERA, cn.flattenToString()).apply();
        }
    }

    @NonNull
    public List<String> getHandledApps() {
        final String listStr = mPrefs.getString(KEY_HANDLED_APPS, null);
        if (listStr != null) {
            try {
                final List<String> list = new ArrayList<>();
                for (String part : listStr.split(",")) {
                    if (!TextUtils.isEmpty(part)) {
                        list.add(part);
                    }
                }
                return Collections.unmodifiableList(list);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return Collections.emptyList();
    }

    public void setHandledApps(@Nullable List<String> handledApps) {
        if (handledApps == null) {
            mPrefs.edit().remove(KEY_HANDLED_APPS).apply();
        } else {
            final StringBuilder sb = new StringBuilder();
            final Iterator<String> iterator = handledApps.iterator();
            while (iterator.hasNext()) {
                sb.append(iterator.next());
                if (iterator.hasNext()) {
                    sb.append(",");
                }
            }
            mPrefs.edit().putString(KEY_HANDLED_APPS, sb.toString()).apply();
        }
    }

    @NonNull
    public Optional<Uri> getRootStorageUri() {
        return Optional.ofNullable(mPrefs.getString(KEY_ROOT_STORAGE_URI, null)).map(Uri::parse);
    }

    public void setRootStorageUri(@Nullable Uri rootUri) {
        if (rootUri == null) {
            mPrefs.edit().remove(KEY_ROOT_STORAGE_URI).apply();
        } else {
            mPrefs.edit().putString(KEY_ROOT_STORAGE_URI, rootUri.toString()).apply();
        }
    }

}
