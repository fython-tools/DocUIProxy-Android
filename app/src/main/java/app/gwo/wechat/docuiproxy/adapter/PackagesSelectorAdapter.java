package app.gwo.wechat.docuiproxy.adapter;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import app.gwo.wechat.docuiproxy.R;
import app.gwo.wechat.docuiproxy.compat.CollectionsCompat;
import app.gwo.wechat.docuiproxy.model.CheckableAppInfo;
import app.gwo.wechat.docuiproxy.util.AppIconCacheUtils;
import io.reactivex.disposables.CompositeDisposable;

import static java.util.Objects.requireNonNull;

public final class PackagesSelectorAdapter
        extends RecyclerView.Adapter<PackagesSelectorAdapter.ItemViewHolder> {

    private static final String STATE_DATA = "state.DATA";

    @NonNull
    private List<CheckableAppInfo> mData = new ArrayList<>();

    public PackagesSelectorAdapter() {

    }

    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putParcelableArrayList(STATE_DATA, new ArrayList<>(mData));
    }

    public void onRestoreInstanceState(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            List<CheckableAppInfo> saved = savedInstanceState.getParcelableArrayList(STATE_DATA);
            if (saved == null) {
                saved = new ArrayList<>();
            }
            mData = saved;
        }
    }

    public void updateData(@NonNull List<CheckableAppInfo> newData) {
        mData = requireNonNull(newData);
        notifyDataSetChanged();
    }

    public void updateCheckedPackages(@NonNull List<String> packages) {
        for (CheckableAppInfo info : mData) {
            info.setChecked(CollectionsCompat.anyMatch(packages,
                    pack -> pack.equals(info.getPackageName())));
        }
        notifyDataSetChanged();
    }

    @NonNull
    public List<CheckableAppInfo> getAllData() {
        return mData;
    }

    @NonNull
    public List<CheckableAppInfo> getCheckedData() {
        return CollectionsCompat.filterToList(mData, CheckableAppInfo::isChecked);
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ItemViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_packages_selector_choice, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        holder.onBind(mData.get(position));
    }

    @Override
    public void onViewRecycled(@NonNull ItemViewHolder holder) {
        holder.onRecycle();
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    class ItemViewHolder extends RecyclerView.ViewHolder {

        private CheckableAppInfo mData;

        private final CheckBox mCheckBox;
        private final ImageView mIcon;
        private final TextView mTitle, mSummary;

        private final CompositeDisposable mDisposables = new CompositeDisposable();

        ItemViewHolder(@NonNull View itemView) {
            super(itemView);

            mCheckBox = itemView.findViewById(android.R.id.checkbox);
            mIcon = itemView.findViewById(android.R.id.icon);
            mTitle = itemView.findViewById(android.R.id.title);
            mSummary = itemView.findViewById(android.R.id.summary);

            itemView.setOnClickListener(v -> mCheckBox.toggle());
            mCheckBox.setOnCheckedChangeListener((button, isChecked) -> {
                if (getData() == null || getData().isChecked() == isChecked) {
                    return;
                }
                getData().setChecked(isChecked);
            });
        }

        @Nullable
        public CheckableAppInfo getData() {
            return mData;
        }

        public void onBind(@NonNull CheckableAppInfo data) {
            this.mData = data;

            mDisposables.add(AppIconCacheUtils.loadIconBitmapToAsync(
                    itemView.getContext(), data.getPackageName(), mIcon
            ));
            itemView.setEnabled(data.isCheckable());
            mCheckBox.setEnabled(data.isCheckable());
            mCheckBox.setChecked(data.isChecked());
            mTitle.setText(data.getTitle());
            mSummary.setText(data.getPackageName());
        }

        public void onRecycle() {
            mDisposables.clear();
        }

    }

}
