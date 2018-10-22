package app.gwo.wechat.docuiproxy;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import app.gwo.wechat.docuiproxy.util.DumpUtils;

import static app.gwo.wechat.docuiproxy.BuildConfig.DEBUG;

/**
 * Proxy open result to WeChat (include other apps who call camera apps)
 *
 * @author Fung Gwo (fython@163.com)
 */
public final class ProxyCameraActivity extends BaseActivity {

    public static final String TAG = ProxyCameraActivity.class.getSimpleName();

    public static final int REQUEST_CODE_OPEN = 1;
    public static final int REQUEST_CODE_CAPTURE = 2;

    private boolean isFromWeChat = false;

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
        isFromWeChat = Constants.WECHAT_PACKAGE_NAME.equals(getReferrerPackage());
        if (isFromWeChat) {
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
        // TODO Open camera app
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        File cacheFile = new File(getCacheDir(), "test");
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                FileProvider.getUriForFile(this, Constants.FILE_PROVIDER_AUTHORITY, cacheFile));
        startActivityForResult(cameraIntent, REQUEST_CODE_CAPTURE);
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

}
