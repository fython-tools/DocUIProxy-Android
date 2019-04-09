package app.gwo.safenhancer.lite;

import android.app.Activity;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.MenuItem;

import java.lang.reflect.Field;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import app.gwo.safenhancer.lite.compat.Optional;

public abstract class BaseActivity extends Activity {

    private static final String TAG = BaseActivity.class.getSimpleName();

    private static Field field_Activity_mReferrer = null;
    private static boolean field_Activity_mReferrer_fallback =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P;

    @Nullable
    private static String getReferrerPackage(@NonNull Activity activity) {
        if (field_Activity_mReferrer == null && !field_Activity_mReferrer_fallback) {
            try {
                field_Activity_mReferrer = activity.getClass().getDeclaredField("mReferrer");
                field_Activity_mReferrer.setAccessible(true);
            } catch (Exception e) {
                Log.e(TAG, "Failed to reflect Activity#mReferrer. " +
                        "Fallback to unstable method.");
                field_Activity_mReferrer_fallback = true;
            }
        }
        if (field_Activity_mReferrer != null) {
            try {
                return (String) field_Activity_mReferrer.get(activity);
            } catch (IllegalAccessException e) {
                Log.e(TAG, "Cannot access Activity#mReferrer. " +
                        "Fallback to unstable method.");
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            return Optional.ofNullable(activity.getReferrer())
                    .map(Uri::getAuthority)
                    .orElseNullable(activity.getCallingPackage());
        } else {
            return activity.getCallingPackage();
        }
    }

    @Nullable
    public String getReferrerPackage() {
        return BaseActivity.getReferrerPackage(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (android.R.id.home == item.getItemId()) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
