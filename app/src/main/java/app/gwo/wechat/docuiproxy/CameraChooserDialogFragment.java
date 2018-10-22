package app.gwo.wechat.docuiproxy;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class CameraChooserDialogFragment extends DialogFragment {

    private View mContentView;

    @NonNull
    public Context getThemedContext() {
        return new ContextThemeWrapper(getActivity(), android.R.style.Theme_Material_Dialog);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(
                getActivity(), android.R.style.Theme_Material_Dialog);
        builder.setTitle(R.string.camera_chooser_dialog_title);
        builder.setView(mContentView = onCreateContentView(savedInstanceState));
        return super.onCreateDialog(savedInstanceState);
    }

    private View onCreateContentView(@Nullable Bundle savedInstanceState) {
        final View view = LayoutInflater.from(getThemedContext())
                .inflate(R.layout.dialog_camera_chooser_content, null);



        return view;
    }

}
