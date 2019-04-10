package app.gwo.safenhancer.lite.util;

import android.os.Build;

public final class BuildUtils {

    public static class VERSION_CODE {

        public static int Q = 29;

    }

    public static boolean isAtLeastQ() {
        if (Build.VERSION.SDK_INT >= VERSION_CODE.Q) {
            return true;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && Build.VERSION.PREVIEW_SDK_INT > 0) {
            return true;
        }
        return false;
    }

}
