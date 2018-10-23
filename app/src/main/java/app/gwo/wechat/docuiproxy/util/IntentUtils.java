package app.gwo.wechat.docuiproxy.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.provider.MediaStore;

import java.util.List;

import androidx.annotation.NonNull;
import app.gwo.wechat.docuiproxy.compat.CollectionsCompat;

import static java.util.Objects.requireNonNull;

public final class IntentUtils {

    private IntentUtils() {}

    private static final Intent sCaptureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

    @NonNull
    public static List<ResolveInfo> queryCameraActivities(@NonNull Context context) {
        final PackageManager pm = context.getPackageManager();
        List<ResolveInfo> result = pm.queryIntentActivities(
                sCaptureIntent, PackageManager.MATCH_DEFAULT_ONLY);
        result = CollectionsCompat.filterToList(result, resolveInfo ->
                !context.getPackageName().equals(resolveInfo.activityInfo.packageName));
        return result;
    }

    @NonNull
    public static ComponentName toComponent(@NonNull ResolveInfo resolveInfo) {
        requireNonNull(resolveInfo.activityInfo, "ResolveInfo should contains a activity.");
        return new ComponentName(
                resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);
    }

}
