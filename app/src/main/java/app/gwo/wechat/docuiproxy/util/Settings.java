package app.gwo.wechat.docuiproxy.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class Settings {

    public static final String PREF_NAME = "settings";

    public static final String KEY_PREFERRED_CAMERA = "preferred_camera";

    private volatile static Settings sInstance = null;

    public static void init(@NonNull Context appContext) {
        sInstance = new Settings(appContext);
    }

    @NonNull
    public static Settings getInstance() {
        return sInstance;
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

}
