package app.gwo.wechat.docuiproxy;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;
import app.gwo.wechat.docuiproxy.adapter.CameraChooserAdapter;
import app.gwo.wechat.docuiproxy.util.IntentUtils;
import app.gwo.wechat.docuiproxy.util.Settings;

import static app.gwo.wechat.docuiproxy.Constants.EXTRA_DATA;
import static java.util.Objects.requireNonNull;

public final class CameraChooserDialogFragment extends DialogFragment {

    @NonNull
    public static CameraChooserDialogFragment newInstance() {
        return new CameraChooserDialogFragment();
    }

    private static final String EXTRA_LIST_DATA = Constants.EXTRA_PREFIX + ".LIST_DATA";

    private View mContentView;
    private RecyclerView mRecyclerView;
    private CheckBox mCheckBox;

    private CameraChooserAdapter mAdapter;
    private List<ResolveInfo> mData;

    private QueryCameraAppsTask mQueryTask;

    private final BroadcastReceiver mItemClickReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(@NonNull Context context, @Nullable Intent intent) {
            requireNonNull(intent);
            final ResolveInfo resolveInfo = intent.getParcelableExtra(EXTRA_DATA);
            if (mCheckBox.isChecked()) {
                Settings.getInstance().setPreferredCamera(IntentUtils.toComponent(resolveInfo));
            }
            ((ProxyCameraActivity) getActivity())
                    .onStartCameraApp(IntentUtils.toComponent(resolveInfo));
        }
    };

    @NonNull
    public Context getThemedContext() {
        return new ContextThemeWrapper(getActivity(), android.R.style.Theme_Material_Light_Dialog);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mData = savedInstanceState.getParcelableArrayList(EXTRA_LIST_DATA);
        }

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                mItemClickReceiver,
                new IntentFilter(CameraChooserAdapter.ACTION_ITEM_CLICK));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mItemClickReceiver);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mQueryTask != null && !mQueryTask.isCancelled()) {
            mQueryTask.cancel(true);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mData != null) {
            outState.putParcelableArrayList(EXTRA_LIST_DATA, new ArrayList<>(mData));
        }
    }

    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getThemedContext());
        builder.setTitle(R.string.camera_chooser_dialog_title);
        builder.setView(mContentView = onCreateContentView(savedInstanceState));
        builder.setNegativeButton(android.R.string.cancel, null);
        return builder.create();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    @NonNull
    private View onCreateContentView(@Nullable Bundle savedInstanceState) {
        final View view = LayoutInflater.from(getThemedContext())
                .inflate(R.layout.dialog_camera_chooser_content, null);

        mRecyclerView = view.findViewById(android.R.id.list);
        mRecyclerView.setHasFixedSize(true);
        if (mAdapter == null) {
            mAdapter = new CameraChooserAdapter();
        }
        mRecyclerView.setAdapter(mAdapter);

        mCheckBox = view.findViewById(android.R.id.checkbox);

        if (mData != null) {
            mAdapter.submitList(mData);
        } else {
            mQueryTask = new QueryCameraAppsTask();
            mQueryTask.execute();
        }

        return view;
    }

    @SuppressLint("StaticFieldLeak")
    private class QueryCameraAppsTask extends AsyncTask<Void, Void, List<ResolveInfo>> {

        @Override
        protected List<ResolveInfo> doInBackground(Void... params) {
            return IntentUtils.queryCameraActivities(getActivity());
        }

        @Override
        protected void onPostExecute(@NonNull List<ResolveInfo> result) {
            if (mAdapter != null) {
                mAdapter.submitList(result);
            }
        }

    }

}
