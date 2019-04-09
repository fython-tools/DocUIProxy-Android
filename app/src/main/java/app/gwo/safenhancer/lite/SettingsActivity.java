package app.gwo.safenhancer.lite;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.widget.Toast;

import java.util.List;

import androidx.annotation.Nullable;
import app.gwo.safenhancer.lite.util.Settings;

public final class SettingsActivity extends BaseActivity {

    private static final int REQUEST_CODE_PACKAGES_SELECTOR = 10;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[] {
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, 0);
            }
        }

        if (savedInstanceState == null) {
            getFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.content, new SettingsFragment())
                    .commit();
        }
    }

    public static final class SettingsFragment extends PreferenceFragment {

        private static final String KEY_PREFERRED_CAMERA_CLEAR = "clear_preferred_camera";
        private static final String KEY_HANDLED_APPS_CHOOSE = "handled_apps_choose";

        private Preference mHandledAppsChoose;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings_screen);

            findPreference(KEY_PREFERRED_CAMERA_CLEAR).setOnPreferenceClickListener(p -> {
                Settings.getInstance().setPreferredCamera(null);
                Toast.makeText(getActivity(), R.string.toast_cleared, Toast.LENGTH_LONG).show();
                return true;
            });

            mHandledAppsChoose = findPreference(KEY_HANDLED_APPS_CHOOSE);
            mHandledAppsChoose.setOnPreferenceClickListener(p -> {
                startActivityForResult(
                        new Intent(getActivity(), PackagesSelectorActivity.class),
                        REQUEST_CODE_PACKAGES_SELECTOR
                );
                return true;
            });
            updateHandledAppsSummary();
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (REQUEST_CODE_PACKAGES_SELECTOR == requestCode
                    && RESULT_OK == resultCode
                    && data != null) {
                final List<String> result = PackagesSelectorActivity.getResult(data);
                Settings.getInstance().setHandledApps(result);
                updateHandledAppsSummary(result.size());
            }
        }

        private void updateHandledAppsSummary() {
            updateHandledAppsSummary(Settings.getInstance().getHandledApps().size());
        }

        private void updateHandledAppsSummary(int count) {
            mHandledAppsChoose.setSummary(getString(
                    R.string.handled_apps_choose_apps_summary, count));
        }

    }

}
