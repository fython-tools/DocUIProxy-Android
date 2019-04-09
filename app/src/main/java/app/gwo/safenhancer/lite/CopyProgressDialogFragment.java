package app.gwo.safenhancer.lite;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import androidx.annotation.NonNull;
import app.gwo.safenhancer.lite.util.IOUtils;

import static app.gwo.safenhancer.lite.Constants.EXTRA_PREFIX;
import static java.util.Objects.requireNonNull;

public final class CopyProgressDialogFragment extends DialogFragment
        implements DialogInterface.OnShowListener {

    public static CopyProgressDialogFragment newInstance(
            @NonNull Uri inputUri, @NonNull Uri outputUri) {
        final CopyProgressDialogFragment fragment = new CopyProgressDialogFragment();
        final Bundle args = new Bundle();

        args.putParcelable(EXTRA_INPUT, requireNonNull(inputUri));
        args.putParcelable(EXTRA_OUTPUT, requireNonNull(outputUri));

        fragment.setArguments(args);
        return fragment;
    }

    public static final String EXTRA_INPUT = EXTRA_PREFIX + ".INPUT";
    public static final String EXTRA_OUTPUT = EXTRA_PREFIX + ".OUTPUT";

    private Uri mInputUri, mOutputUri;

    private CopyTask mCopyTask;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requireNonNull(getArguments(), "Arguments cannot be null.");
        mInputUri = getArguments().getParcelable(EXTRA_INPUT);
        mOutputUri = getArguments().getParcelable(EXTRA_OUTPUT);

        if (mCopyTask != null && !mCopyTask.isCancelled()) {
            mCopyTask.cancel(true);
        }
        mCopyTask = new CopyTask();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final ProgressDialog dialog = new ProgressDialog(getActivity(), android.R.style.Theme_DeviceDefault_Dialog);
        dialog.setMessage(getString(R.string.copy_dialog_message));
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setIndeterminate(true);
        dialog.setOnShowListener(this);
        return dialog;
    }

    @Override
    public void onShow(DialogInterface dialog) {
        mCopyTask.execute();
    }

    @SuppressLint("StaticFieldLeak")
    private class CopyTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            try (InputStream in = IOUtils.openInputStreamAdaptive(getActivity(), mInputUri);
                 OutputStream out = IOUtils.openOutputStreamAdaptive(getActivity(), mOutputUri)) {
                IOUtils.copy(in, out);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(@NonNull Boolean success) {
            if (getActivity() != null && !getActivity().isFinishing()) {
                ((ProxyCameraActivity) getActivity()).onCopyResult(success);
            }
        }

    }

}
