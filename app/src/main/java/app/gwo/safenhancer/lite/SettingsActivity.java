package app.gwo.safenhancer.lite;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.provider.DocumentsContract;
import android.widget.Toast;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import app.gwo.safenhancer.lite.compat.Optional;
import app.gwo.safenhancer.lite.util.BuildUtils;
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
        private static final String KEY_Q_ISOLATED_SUPPORT = "q_isolated_support";

        private static final int REQUEST_CODE_SR_PERMISSION = 1;
        private static final int REQUEST_CODE_OPEN_ROOT_URI = 2;

        private Preference mHandledAppsChoose;
        private SwitchPreference mSRPermission;
        private SwitchPreference mQIsolatedSupport;

        @SuppressLint("InlinedApi")
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
                    try {
                        Intent intent = new Intent(
                                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.setData(Uri.fromParts("package",
                                getActivity().getPackageName(), null));
                        startActivity(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return false;
            });
            mSRPermission.setEnabled(StorageRedirectManager.isSupported(pm));
            mSRPermission.setChecked(
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    getActivity().checkSelfPermission(StorageRedirectManager.PERMISSION)
                            == PackageManager.PERMISSION_GRANTED
            );

            mQIsolatedSupport = (SwitchPreference) findPreference(KEY_Q_ISOLATED_SUPPORT);
            mQIsolatedSupport.setEnabled(BuildUtils.isAtLeastQ());
            if (!BuildUtils.isAtLeastQ()) {
                mQIsolatedSupport.setSummary(
                        R.string.isolated_storage_support_for_q_summary_disabled);
            }
            mQIsolatedSupport.setOnPreferenceChangeListener((pref, newValue) -> {
                boolean newBool = (boolean) newValue;
                if (newBool) {
                    new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.isolated_storage_support_for_q_dialog_title)
                            .setMessage(R.string.isolated_storage_support_for_q_dialog_message)
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI,
                                        Uri.parse("content://" +
                                                "com.android.externalstorage.documents" +
                                                "/tree/primary%3A"));
                                if (intent.resolveActivity(pm) != null) {
                                    startActivityForResult(intent, REQUEST_CODE_OPEN_ROOT_URI);
                                } else {
                                    // TODO Show error
                                }
                            })
                            .show();
                    return false;
                } else {
                    Settings.getInstance().setRootStorageUri(null);
                    return true;
                }
            });
            final Optional<Uri> rootStorageUri = Settings.getInstance().getRootStorageUri();
            mQIsolatedSupport.setChecked(rootStorageUri.isPresent());
            if (rootStorageUri.isPresent()) {
                mQIsolatedSupport.setSummaryOn(getString(
                        R.string.isolated_storage_support_for_q_summary_checked,
                        rootStorageUri.get().toString()
                ));
            }

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
            } else if (REQUEST_CODE_OPEN_ROOT_URI == requestCode
                    && RESULT_OK == resultCode
                    && data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    getActivity().getContentResolver().takePersistableUriPermission(uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION |
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    Settings.getInstance().setRootStorageUri(uri);
                    mQIsolatedSupport.setChecked(true);
                    mQIsolatedSupport.setSummaryOn(getString(
                            R.string.isolated_storage_support_for_q_summary_checked,
                            uri.toString()
                    ));
                }
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
