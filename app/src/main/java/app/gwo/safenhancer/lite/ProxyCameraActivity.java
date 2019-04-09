package app.gwo.safenhancer.lite;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import app.gwo.safenhancer.lite.util.DumpUtils;
import app.gwo.safenhancer.lite.util.Settings;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static app.gwo.safenhancer.lite.BuildConfig.DEBUG;
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
    public static final int REQUEST_CODE_REQUEST_CAMERA_PERMISSION = 3;

    private boolean shouldBeHandled = false;

    private Uri mExpectedOutput = null;

    @Nullable
    private ComponentName mExpectedCameraComponent = null;

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
        final Context themedContext = new ContextThemeWrapper(
                this, android.R.style.Theme_Material_Light_Dialog);
        final View view = LayoutInflater.from(themedContext)
                .inflate(R.layout.dialog_doc_or_cam_chooser_content, null);
        view.findViewById(R.id.action_camera).setOnClickListener(v -> {
            processIntentForOthers(intent);
        });
        view.findViewById(R.id.action_documents).setOnClickListener(v -> {
            Intent openIntent = new Intent(Intent.ACTION_GET_CONTENT);
            openIntent.setType("image/*");
            startActivityForResult(openIntent, REQUEST_CODE_OPEN);
        });
        new AlertDialog.Builder(themedContext)
                .setView(view)
                .setNegativeButton(android.R.string.cancel, null)
                .show();
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

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_REQUEST_CAMERA_PERMISSION:
                if (!Manifest.permission.CAMERA.equals(permissions[0]) ||
                        PERMISSION_GRANTED != grantResults[0]) {
                    Log.e(TAG, "No permission.");
                    finish();
                    return;
                }
                if (mExpectedCameraComponent != null) {
                    onStartCameraApp(mExpectedCameraComponent);
                } else {
                    finish();
                }
                break;
            default:
                Log.e(TAG, "Unsupported result.");
                finish();
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PERMISSION_GRANTED) {
                mExpectedCameraComponent = target;
                requestPermissions(
                        new String[] { Manifest.permission.CAMERA },
                        REQUEST_CODE_REQUEST_CAMERA_PERMISSION
                );
                return;
            }
        }
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.setComponent(target);
        cameraIntent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, mExpectedOutput);
        startActivityForResult(cameraIntent, REQUEST_CODE_CAPTURE);
    }

}
