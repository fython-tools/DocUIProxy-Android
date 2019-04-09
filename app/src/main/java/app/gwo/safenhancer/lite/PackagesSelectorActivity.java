package app.gwo.safenhancer.lite;

import android.app.ActionBar;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import app.gwo.safenhancer.lite.adapter.PackagesSelectorAdapter;
import app.gwo.safenhancer.lite.compat.CollectionsCompat;
import app.gwo.safenhancer.lite.model.CheckableAppInfo;
import app.gwo.safenhancer.lite.util.Settings;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

import static app.gwo.safenhancer.lite.Constants.WECHAT_PACKAGE_NAME;
import static java.util.Objects.requireNonNull;

public final class PackagesSelectorActivity extends BaseActivity {

    public static final String EXTRA_SELECTED = PackagesSelectorActivity.class.getName() +
            ".extra.SELECTED";
    public static final String EXTRA_PACKAGES_RESULT = PackagesSelectorActivity.class.getName() +
            ".extra.PACKAGES_RESULT";

    public static List<String> getResult(@NonNull Intent intent) {
        return intent.getStringArrayListExtra(EXTRA_PACKAGES_RESULT);
    }

    private RecyclerView mRecyclerView;
    private ProgressBar mProgressBar;

    private PackagesSelectorAdapter mAdapter = new PackagesSelectorAdapter();

    @Nullable
    private List<String> mLastSelectedPackages = null;

    private final CompositeDisposable mDisposables = new CompositeDisposable();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_packages_selector_content);

        final Intent intent = getIntent();
        if (intent != null) {
            mLastSelectedPackages = intent.getStringArrayListExtra(EXTRA_SELECTED);
        }

        final ActionBar actionBar = requireNonNull(getActionBar());
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_close_black_24dp);

        mRecyclerView = findViewById(android.R.id.list);
        mProgressBar = findViewById(android.R.id.progress);

        mAdapter.onRestoreInstanceState(savedInstanceState);
        mRecyclerView.setAdapter(mAdapter);

        if (savedInstanceState == null) {
            loadAppInfoListAsync();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDisposables.clear();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mAdapter.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_packages_selector, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (R.id.action_done == item.getItemId()) {
            done();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadAppInfoListAsync() {
        mProgressBar.setVisibility(View.VISIBLE);
        mDisposables.clear();
        mDisposables.add(Single.fromCallable(this::loadAppInfoList)
                .toFlowable()
                .flatMap(Flowable::fromIterable)
                .sorted()
                .toList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((res, err) -> {
                    mProgressBar.setVisibility(View.GONE);
                    if (err != null) {
                        err.printStackTrace();
                        return;
                    }
                    mAdapter.updateData(res);
                    if (mLastSelectedPackages != null) {
                        final List<String> packages = new ArrayList<>(mLastSelectedPackages);
                        if (!packages.contains(WECHAT_PACKAGE_NAME)) {
                            packages.add(WECHAT_PACKAGE_NAME);
                        }
                        mAdapter.updateCheckedPackages(packages);
                    }
                })
        );
    }

    private List<CheckableAppInfo> loadAppInfoList() {
        final PackageManager pm = getPackageManager();
        final List<String> checkedPacks = Settings.getInstance().getHandledApps();
        return CollectionsCompat.mapToList(pm.getInstalledApplications(0), item -> {
            final CheckableAppInfo cai = CheckableAppInfo.build(this, item);
            if (WECHAT_PACKAGE_NAME.equals(cai.getPackageName())) {
                cai.setChecked(true);
                cai.setCheckable(false);
            } else {
                cai.setChecked(CollectionsCompat.anyMatch(
                        checkedPacks, pack -> pack.equals(cai.getPackageName())));
            }
            return cai;
        });
    }

    private void done() {
        Intent resultIntent = new Intent();
        resultIntent.putStringArrayListExtra(EXTRA_PACKAGES_RESULT,
                Flowable.fromIterable(mAdapter.getCheckedData())
                        .filter(item -> !WECHAT_PACKAGE_NAME.equals(item.getPackageName()))
                        .map(CheckableAppInfo::getPackageName)
                        .toList()
                        .map(ArrayList::new)
                        .blockingGet()
        );
        setResult(RESULT_OK, resultIntent);
        finish();
    }

}
