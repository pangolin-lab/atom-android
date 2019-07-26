package com.protonnetwork;

import android.app.Application;
import android.content.Context;

public class ProtonApplication extends Application {
    private static Context context;

    public void onCreate() {
        super.onCreate();
        ProtonApplication.context = getApplicationContext();
    }

    public static Context getAppContext() {
        return ProtonApplication.context;
    }
}
