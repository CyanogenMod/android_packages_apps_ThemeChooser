package org.cyanogenmod.theme.chooser;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import android.app.Application;

public class ThemeApplication extends Application {

    public static final String BASE_URL = "http://themestore.cyngn.com/api/v1/";

    private static RequestQueue mRequestQueue;

    @Override
    public void onCreate() {
        super.onCreate();
        mRequestQueue = Volley.newRequestQueue(getApplicationContext());
    }

    public static RequestQueue getQueue() {
        return mRequestQueue;
    }

}
