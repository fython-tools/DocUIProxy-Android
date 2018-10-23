package app.gwo.wechat.docuiproxy;

import android.app.Application;
import android.os.StrictMode;

import app.gwo.wechat.docuiproxy.util.Settings;

public final class ProxyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Settings.init(this);

        final StrictMode.VmPolicy vmPolicy = new StrictMode.VmPolicy.Builder().build();
        StrictMode.setVmPolicy(vmPolicy);
    }

}
