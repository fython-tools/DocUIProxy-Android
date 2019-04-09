package app.gwo.safenhancer.lite;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.util.Log;
import android.widget.Toast;

import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import app.gwo.safenhancer.lite.util.Settings;
import moe.shizuku.redirectstorage.StorageRedirectManager;

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
        private static final String KEY_ABOUT_VERSION = "version";
        private static final String KEY_ABOUT_GITHUB = "github";
        private static final String KEY_SR_API_PERMISSION = "sr_api_permission";

        private static final int REQUEST_CODE_SR_PERMISSION = 1;

        private Preference mHandledAppsChoose;
        private SwitchPreference mSRPermission;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings_screen);
            PackageManager pm = getActivity().getPackageManager();

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

            mSRPermission = (SwitchPreference) findPreference(KEY_SR_API_PERMISSION);
            mSRPermission.setOnPreferenceChangeListener((pref, newValue) -> {
                boolean newBool = (boolean) newValue;
                if (newBool) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(new String[] {
                                StorageRedirectManager.PERMISSION
                        }, REQUEST_CODE_SR_PERMISSION);
                    }
                } else {
                    // TODO Jump to settings
                }
                return false;
            });
            mSRPermission.setEnabled(
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                            StorageRedirectManager.installed(pm)
            );
            mSRPermission.setChecked(
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    getActivity().checkSelfPermission(StorageRedirectManager.PERMISSION)
                            == PackageManager.PERMISSION_GRANTED
            );

            Preference versionPref = findPreference(KEY_ABOUT_VERSION);
            String version = "Unknown";
            try {
                PackageInfo pi = pm.getPackageInfo(getActivity().getPackageName(), 0);
                version = getString(R.string.version_format, pi.versionName, pi.versionCode);
            } catch (Exception e) {
                e.printStackTrace();
            }
            versionPref.setSummary(version);

            findPreference(KEY_ABOUT_GITHUB).setOnPreferenceClickListener(p -> {
                startActivity(
                        new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.github_url)))
                );
                return true;
            });
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

        @Override
        public void onRequestPermissionsResult(int requestCode,
                                               @NonNull String[] permissions,
                                               @NonNull int[] grantResults) {
            if (REQUEST_CODE_SR_PERMISSION == requestCode) {
                if (StorageRedirectManager.PERMISSION.equals(permissions[0]) &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mSRPermission.setChecked(true);
                }
            }
        }
    }

}
