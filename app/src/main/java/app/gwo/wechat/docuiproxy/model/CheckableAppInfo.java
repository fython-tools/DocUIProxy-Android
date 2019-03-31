package app.gwo.wechat.docuiproxy.model;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static java.util.Objects.requireNonNull;

public class CheckableAppInfo implements Parcelable, Comparable<CheckableAppInfo> {

    public static CheckableAppInfo build(@NonNull Context context, @NonNull ApplicationInfo ai) {
        requireNonNull(context);
        return new CheckableAppInfo(ai, ai.loadLabel(context.getPackageManager()).toString());
    }

    private final ApplicationInfo mApplicationInfo;
    private final String mTitle;
    private boolean isChecked = false;
    private boolean isCheckable = true;

    public CheckableAppInfo(@NonNull ApplicationInfo ai, @NonNull String title) {
        mApplicationInfo = requireNonNull(ai);
        mTitle = requireNonNull(title);
    }

    private CheckableAppInfo(Parcel in) {
        mApplicationInfo = in.readParcelable(ApplicationInfo.class.getClassLoader());
        mTitle = in.readString();
        isChecked = in.readByte() != 0;
        isCheckable = in.readByte() != 0;
    }

    @NonNull
    public ApplicationInfo getAppInfo() {
        return mApplicationInfo;
    }

    @NonNull
    public String getTitle() {
        return mTitle;
    }

    @NonNull
    public String getPackageName() {
        return mApplicationInfo.packageName;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public boolean isCheckable() {
        return isCheckable;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }

    public void setCheckable(boolean checkable) {
        isCheckable = checkable;
    }

    @Override
    public int compareTo(@NonNull CheckableAppInfo o) {
        return getTitle().toLowerCase().compareTo(o.getTitle().toLowerCase());
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null) return false;
        if (!(obj instanceof CheckableAppInfo)) return false;
        final CheckableAppInfo other = (CheckableAppInfo) obj;
        return getPackageName().equals(other.getPackageName());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(mApplicationInfo, flags);
        dest.writeString(mTitle);
        dest.writeByte((byte) (isChecked ? 1 : 0));
        dest.writeByte((byte) (isCheckable ? 1 : 0));
    }

    public static final Creator<CheckableAppInfo> CREATOR = new Creator<CheckableAppInfo>() {
        @Override
        public CheckableAppInfo createFromParcel(Parcel in) {
            return new CheckableAppInfo(in);
        }

        @Override
        public CheckableAppInfo[] newArray(int size) {
            return new CheckableAppInfo[size];
        }
    };

}
