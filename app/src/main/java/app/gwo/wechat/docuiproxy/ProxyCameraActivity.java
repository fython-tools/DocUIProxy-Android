package app.gwo.wechat.docuiproxy;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import app.gwo.wechat.docuiproxy.util.DumpUtils;
import app.gwo.wechat.docuiproxy.util.Settings;

import static app.gwo.wechat.docuiproxy.BuildConfig.DEBUG;
import static java.util.Objects.requireNonNull;

/**
 * Proxy open result to WeChat (include other apps who call camera apps)
 *
 * @author Fung Gwo (fython@163.com)
 */
public final class ProxyCameraActivity extends BaseActivity {

    public static final String TAG = ProxyCameraActivity.class.getSimpleName();

    public static final int REQUEST_CODE_OPEN = 1;
    public static final int REQUEST_CODE_CAPTURE = 2;

    private boolean shouldBeHandled = false;

    private Uri mExpectedOutput = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Prevent invalid actions
        final Intent intent = getIntent();
        if (intent == null || intent.getAction() == null) {
            Log.e(TAG, "Invalid intent. Activity will exit now.");
            finish();
            return;
        }

        if (!MediaStore.ACTION_IMAGE_CAPTURE.equals(intent.getAction())) {
            Log.e(TAG, "ProxyCameraActivity can receive Media.ACTION_IMAGE_CAPTURE only. " +
                    "But its action is " + intent.getAction());
            finish();
            return;
        }

        // Get extras from current capture intent
        getExtrasFromCaptureIntent(intent);

        // Start process
        shouldBeHandled = Settings.isSourceAppShouldBeHandled(getReferrerPackage());
        if (shouldBeHandled) {
            Log.d(TAG, "Receive an valid capture intent from WeChat. " +
                    "Now we start process it.");
            processIntentForWeChat(intent);
        } else {
            Log.v(TAG, "Receive an valid capture intent from other apps. " +
                    "We should open a preferred camera application or start chooser.");
            processIntentForOthers(intent);
        }
    }

    private void getExtrasFromCaptureIntent(@NonNull Intent intent) {
        if (DEBUG) {
            Log.d(TAG, "Received intent extras=" + DumpUtils.toString(intent.getExtras()));
        }

        if (intent.hasExtra(MediaStore.EXTRA_OUTPUT)) {
            mExpectedOutput = intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT);
            Log.d(TAG, "Expected output path: " + mExpectedOutput);
        }
    }

    private void processIntentForWeChat(@NonNull Intent intent) {
        // TODO Choose Documents UI or camera app
        Intent openIntent = new Intent(Intent.ACTION_GET_CONTENT);
        openIntent.setType("image/*");
        startActivityForResult(openIntent, REQUEST_CODE_OPEN);
    }

    private void processIntentForOthers(@NonNull Intent intent) {
        final ComponentName preferredCamera = Settings.getInstance().getPreferredCamera();
        boolean launched = false;
        if (preferredCamera != null) {
            Log.d(TAG, "Launch preferred camera: " + preferredCamera.toString());
            try {
                onStartCameraApp(preferredCamera);
                launched = true;
            } catch (ActivityNotFoundException e) {
                e.printStackTrace();
                Settings.getInstance().setPreferredCamera(null);
            }
        }
        if (!launched) {
            CameraChooserDialogFragment
                    .newInstance()
                    .show(getFragmentManager(), "CameraChooser");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (DEBUG) {
            Log.d(TAG, "onActivityResult: requestCode=" + requestCode +
                    ", resultCode=" + resultCode);
            if (data != null) {
                Log.d(TAG, "data=" + data.toString());
                Log.d(TAG, "extras=" + data.getExtras());
            }
        }
        if (RESULT_CANCELED == resultCode) {
            setResult(RESULT_CANCELED);
            finish();
        } else if (RESULT_OK == resultCode) {
            switch (requestCode) {
                case REQUEST_CODE_OPEN: {
                    if (data != null && data.getData() != null) {
                        CopyProgressDialogFragment
                                .newInstance(data.getData(), mExpectedOutput)
                                .show(getFragmentManager(), "Copy");
                    } else {
                        // TODO Show failed
                        finish();
                    }
                    break;
                }
                case REQUEST_CODE_CAPTURE: {
                    if (data != null) {
                        setResult(RESULT_OK, new Intent());
                        finish();
                    } else {
                        // TODO Show failed
                        finish();
                    }
                    break;
                }
                default: {
                    Log.e(TAG, "onActivityResult: Unsupported requestCode!");
                    finish();
                }
            }
        }
    }

    void onCopyResult(boolean success) {
        if (success) {
            setResult(RESULT_OK, new Intent());
        } else {
            Log.e(TAG, "Failed to copy.");
        }
        finish();
    }

    void onStartCameraApp(@NonNull ComponentName target) {
        requireNonNull(target);
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.setComponent(target);
        cameraIntent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, mExpectedOutput);
        startActivityForResult(cameraIntent, REQUEST_CODE_CAPTURE);
    }

}
