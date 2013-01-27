package com.tassadar.lorrismobile;

import android.app.Application;
import android.content.Context;

public class LorrisApplication extends Application {
    private static Context m_context;

    public void onCreate(){
        super.onCreate();
        LorrisApplication.m_context = getApplicationContext();
    }

    public static Context getAppContext() {
        return LorrisApplication.m_context;
    }
}