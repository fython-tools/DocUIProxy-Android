package app.gwo.wechat.docuiproxy;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.widget.Toast;

import androidx.annotation.Nullable;
import app.gwo.wechat.docuiproxy.util.Settings;

public final class SettingsActivity extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            getFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.content, new SettingsFragment())
                    .commit();
        }
    }

    public static final class SettingsFragment extends PreferenceFragment {

        private static final String KEY_PREFERRED_CAMERA_CLEAR = "clear_preferred_camera";

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings_screen);

            findPreference(KEY_PREFERRED_CAMERA_CLEAR).setOnPreferenceClickListener(p -> {
                Settings.getInstance().setPreferredCamera(null);
                Toast.makeText(getActivity(), R.string.toast_cleared, Toast.LENGTH_LONG).show();
                return true;
            });
        }

    }

}
