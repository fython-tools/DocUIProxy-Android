package app.gwo.safenhancer.lite.util;

import android.os.Bundle;

import androidx.annotation.Nullable;

/**
 * Dump program data while running
 *
 * @author Fung Gwo (fython@163.com)
 * @hide
 */
public final class DumpUtils {

    private DumpUtils() {}

    public static String toString(@Nullable Bundle bundle) {
        final StringBuilder sb = new StringBuilder();
        if (bundle == null) {
            sb.append("Bundle[null]");
        } else {
            sb.append("Bundle[");
            for (String key : bundle.keySet()) {
                sb.append(key).append("=").append(bundle.get(key)).append(",");
            }
            if (sb.lastIndexOf(",") == sb.length() - 1) {
                sb.setLength(sb.length() - 1);
            }
            sb.append("]");
        }
        return sb.toString();
    }

}
