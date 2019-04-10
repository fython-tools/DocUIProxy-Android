package app.gwo.safenhancer.lite;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import app.gwo.safenhancer.lite.compat.Optional;
import app.gwo.safenhancer.lite.util.BuildUtils;
import app.gwo.safenhancer.lite.util.DumpUtils;
import app.gwo.safenhancer.lite.util.Settings;
import moe.shizuku.redirectstorage.RedirectPackageInfo;
import moe.shizuku.redirectstorage.StorageRedirectManager;

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
            final String referrerPackage = getReferrerPackage();

            boolean isolatedStoragePathProceed = false;

            if (mExpectedOutput != null
                    && "file".equals(mExpectedOutput.getScheme())
                    && referrerPackage != null
                    && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    && StorageRedirectManager.isSupported(getPackageManager())
                    && checkSelfPermission(StorageRedirectManager.PERMISSION) == PERMISSION_GRANTED
            ) {
                StorageRedirectManager srm = StorageRedirectManager.create();
                if (srm != null) {
                    try {
                        RedirectPackageInfo rpi = srm.getRedirectPackageInfo(
                                referrerPackage, 0, 0);
                        if (rpi != null && rpi.enabled) {
                            Log.d(TAG, "Package " + referrerPackage + " is enabled redirect.");
                            String originalPath = mExpectedOutput.toString();
                            String externalRoot = Environment.getExternalStorageDirectory()
                                    .getAbsolutePath();
                            String redirectTarget = rpi.redirectTarget;
                            if (redirectTarget == null) {
                                redirectTarget = srm.getDefaultRedirectTarget();
                            }
                            if (rpi.redirectTarget.contains("%s")) {
                                redirectTarget = String.format(redirectTarget, referrerPackage);
                            }
                            String newExternalRoot = externalRoot;
                            if (!redirectTarget.isEmpty()) {
                                newExternalRoot = newExternalRoot + "/" + redirectTarget;
                            }
                            mExpectedOutput = Uri.parse(
                                    originalPath.replace(externalRoot, newExternalRoot)
                            );
                            isolatedStoragePathProceed = true;
                            Log.d(TAG, "Original path: " + originalPath + ", external root: " +
                                    externalRoot + ", redirect target: " + rpi.redirectTarget +
                                    ", after: " + mExpectedOutput);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (DEBUG) {
                            throw e;
                        }
                    }
                }
            }

            if (mExpectedOutput != null
                    && "file".equals(mExpectedOutput.getScheme())
                    && referrerPackage != null
                    && BuildUtils.isAtLeastQ()
                    && !isolatedStoragePathProceed) {
                // TODO Check LEGACY_STORAGE app ops
                final Uri rootUri = Settings.getInstance().getRootStorageUri().get();
                if (rootUri == null) {
                    // TODO Show warning
                    Log.w(TAG, "Android Q+ cannot work without Storage Access Framework API.");
                } else {
                    DocumentFile rootFile = DocumentFile.fromTreeUri(this, rootUri);
                    DocumentFile sandboxRoot = Optional.ofNullable(rootFile)
                            .map(file -> file.findFile("Android"))
                            .filter(DocumentFile::isDirectory)
                            .map(file -> file.findFile("sandbox"))
                            .filter(DocumentFile::isDirectory)
                            .map(file -> {
                                String sandboxDirName = referrerPackage;
                                try {
                                    PackageInfo pi = getPackageManager()
                                            .getPackageInfo(referrerPackage, 0);
                                    if (pi.sharedUserId != null) {
                                        sandboxDirName = "shared-" + pi.sharedUserId;
                                    }
                                } catch (PackageManager.NameNotFoundException e) {
                                    e.printStackTrace();
                                }
                                return file.findFile(sandboxDirName);
                            })
                            .filter(DocumentFile::isDirectory)
                            .get();
                    if (sandboxRoot == null) {
                        sandboxRoot = rootFile;
                    }
                    String externalRoot = Environment.getExternalStorageDirectory()
                            .getAbsolutePath();
                    String originalPath = mExpectedOutput.toString();
                    try {
                        originalPath = URLDecoder.decode(originalPath, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    originalPath = originalPath.replace("file://" + externalRoot + "/", "");
                    String[] pathSegs = Optional.ofNullable(originalPath.split("/"))
                            .orElse(new String[0]);
                    Optional<DocumentFile> curFile = Optional.ofNullable(sandboxRoot);
                    for (int i = 0; i < pathSegs.length; i++) {
                        String path = pathSegs[i];
                        curFile = curFile.map(file -> file.findFile(path));
                        if (i != pathSegs.length - 1) {
                            curFile = curFile.filter(DocumentFile::isDirectory);
                            if (!curFile.isPresent()) {
                                break;
                            }
                        }
                    }
                    mExpectedOutput = curFile.map(DocumentFile::getUri).orElse(mExpectedOutput);
                }
            }

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
