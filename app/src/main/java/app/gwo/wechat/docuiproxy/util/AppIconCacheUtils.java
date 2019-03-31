package app.gwo.wechat.docuiproxy.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.widget.ImageView;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.LruCache;
import app.gwo.wechat.docuiproxy.R;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public final class AppIconCacheUtils {

    private AppIconCacheUtils() {}

    private static final LruCache<String, Bitmap> sLruCache;

    /**
     * A replacement of Schedulers.computation();
     */
    private static final Scheduler sLoadIconScheduler;

    static {
        // Initialize app icon lru cache
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int availableCacheSize = maxMemory / 4;

        sLruCache = new AppIconLruCache(availableCacheSize);

        // Initialize load icon scheduler
        int availableProcessorsCount = 1;
        try {
            availableProcessorsCount = Runtime.getRuntime().availableProcessors();
        } catch (Exception ignored) {

        }
        final int threadCount = Math.max(1, availableProcessorsCount / 2);
        final Executor loadIconExecutor = Executors.newFixedThreadPool(threadCount);
        sLoadIconScheduler = Schedulers.from(loadIconExecutor);
    }

    public static Scheduler scheduler() {
        return sLoadIconScheduler;
    }

    @Nullable
    public static Bitmap getBitmapFromCache(@NonNull String packageName) {
        return sLruCache.get(packageName);
    }

    public static void putBitmapToCache(@NonNull String packageName, @NonNull Bitmap bitmap) {
        if (getBitmapFromCache(packageName) == null) {
            sLruCache.put(packageName, bitmap);
        }
    }

    public static void removeCache(@NonNull String packageName) {
        sLruCache.remove(packageName);
    }

    @Nullable
    public static Bitmap loadIconBitmap(@NonNull Context context, @NonNull ApplicationInfo info) {
        final Bitmap cachedBitmap = getBitmapFromCache(info.packageName);
        if (cachedBitmap != null) {
            return cachedBitmap;
        }
        final PackageManager pm = context.getPackageManager();
        Bitmap loadedResult = null;

        try {
            final int maxSize = context.getResources()
                    .getDimensionPixelSize(R.dimen.expected_app_icon_max_size);
            final Drawable icon;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                icon = info.loadUnbadgedIcon(pm);
            } else {
                icon = info.loadIcon(pm);
            }
            loadedResult = DrawableUtils.toBitmap(icon, maxSize, maxSize, Bitmap.Config.ARGB_8888);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (loadedResult != null) {
            putBitmapToCache(info.packageName, loadedResult);
        }

        return loadedResult;
    }

    @Nullable
    public static Bitmap loadIconBitmap(@NonNull Context context,
                                        @NonNull String packageName) {
        final PackageManager pm = context.getPackageManager();
        try {
            return loadIconBitmap(context, pm.getApplicationInfo(packageName, 0));
        } catch (PackageManager.NameNotFoundException ignored) {
            return null;
        }
    }

    @NonNull
    public static Single<Bitmap> loadIconBitmapAsync(@NonNull Context context,
                                                     @NonNull ApplicationInfo info) {
        return Single.fromCallable(() -> loadIconBitmap(context, info))
                .subscribeOn(AppIconCacheUtils.scheduler())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @NonNull
    public static Single<Bitmap> loadIconBitmapAsync(@NonNull Context context,
                                                     @NonNull String packageName) {
        try {
            final PackageManager pm = context.getPackageManager();
            final ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
            return loadIconBitmapAsync(context, info);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @NonNull
    public static Disposable loadIconBitmapToAsync(@NonNull Context context,
                                                   @NonNull ApplicationInfo info,
                                                   @NonNull ImageView view) {
        return loadIconBitmapAsync(context, info).subscribe(
                view::setImageBitmap,
                e -> view.setImageResource(R.mipmap.ic_default_app_icon)
        );
    }

    @NonNull
    public static Disposable loadIconBitmapToAsync(@NonNull Context context,
                                                   @NonNull String packageName,
                                                   @NonNull ImageView view) {
        return loadIconBitmapAsync(context, packageName).subscribe(
                view::setImageBitmap,
                e -> view.setImageResource(R.mipmap.ic_default_app_icon)
        );
    }

    private static class AppIconLruCache extends LruCache<String, Bitmap> {

        AppIconLruCache(int maxSize) {
            super(maxSize);
        }

        @Override
        protected int sizeOf(@NonNull String key, @NonNull Bitmap bitmap) {
            return bitmap.getByteCount() / 1024;
        }

    }

}
