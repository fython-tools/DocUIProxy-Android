package app.gwo.wechat.docuiproxy.adapter;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import app.gwo.wechat.docuiproxy.R;
import app.gwo.wechat.docuiproxy.compat.Optional;

import static app.gwo.wechat.docuiproxy.Constants.EXTRA_DATA;
import static java.util.Objects.requireNonNull;

public final class CameraChooserAdapter
        extends ListAdapter<ResolveInfo, CameraChooserAdapter.ViewHolder> {

    public static final String ACTION_ITEM_CLICK = CameraChooserAdapter.class.getName()
            + ".action.ITEM_CLICK";

    private static final ResolveInfoDiffCallback sDiffCallback = new ResolveInfoDiffCallback();

    public CameraChooserAdapter() {
        super(sDiffCallback);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_activity_info, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.data = getItem(position);
        holder.onBind();
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position,
                                 @NonNull List<Object> payloads) {
        holder.data = getItem(position);
        holder.onBind(payloads);
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        holder.onRecycled();
    }

    static final class ViewHolder extends RecyclerView.ViewHolder {

        private final ImageView icon;
        private final TextView text1, text2;

        ResolveInfo data;

        private ImageLoadTask imageLoadTask;

        private ViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(android.R.id.icon);
            text1 = itemView.findViewById(android.R.id.text1);
            text2 = itemView.findViewById(android.R.id.text2);

            itemView.setOnClickListener(v -> LocalBroadcastManager.getInstance(v.getContext())
                    .sendBroadcast(new Intent(ACTION_ITEM_CLICK).putExtra(EXTRA_DATA, data)));
        }

        void onBind() {
            final PackageManager pm = itemView.getContext().getPackageManager();
            text1.setText(data.loadLabel(pm));
            text2.setText(data.activityInfo.packageName);

            imageLoadTask = new ImageLoadTask(icon);
            imageLoadTask.execute(data);
        }

        void onBind(@NonNull List<Object> payloads) {
            onBind();
        }

        void onRecycled() {
            if (imageLoadTask != null && !imageLoadTask.isCancelled()) {
                imageLoadTask.cancel(true);
            }
        }

        private static class ImageLoadTask
                extends AsyncTask<ResolveInfo, Void, Optional<Drawable>> {

            private final WeakReference<ImageView> mTarget;

            ImageLoadTask(@NonNull ImageView imageView) {
                mTarget = new WeakReference<>(imageView);
            }

            @Override
            protected Optional<Drawable> doInBackground(ResolveInfo... info) {
                requireNonNull(info[0]);
                return Optional.ofNullable(mTarget.get())
                        .map(view -> view.getContext().getPackageManager())
                        .map(pm -> info[0].loadIcon(pm));
            }

            @Override
            protected void onPostExecute(Optional<Drawable> drawableOptional) {
                if (mTarget.get() != null) {
                    drawableOptional.ifPresent(drawable ->
                            mTarget.get().setImageDrawable(drawable));
                }
            }

        }

    }

    private static final class ResolveInfoDiffCallback extends DiffUtil.ItemCallback<ResolveInfo> {

        @Override
        public boolean areItemsTheSame(@NonNull ResolveInfo old, @NonNull ResolveInfo now) {
            return Objects.equals(old.activityInfo.packageName, now.activityInfo.packageName) &&
                    Objects.equals(old.activityInfo.name, now.activityInfo.name);
        }

        @Override
        public boolean areContentsTheSame(@NonNull ResolveInfo old, @NonNull ResolveInfo now) {
            return areItemsTheSame(old, now);
        }

    }

}
