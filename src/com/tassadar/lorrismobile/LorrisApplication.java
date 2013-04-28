package com.tassadar.lorrismobile;

import java.util.concurrent.atomic.AtomicInteger;

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

    /**
     * Generate a value suitable for use in {@link #setId(int)}.
     * This value will not collide with ID values generated at build time by aapt for R.id.
     *
     * @return a generated ID value
     */
    public static int generateViewId() {
        for (;;) {
            final int result = m_viewIdGenerator.get();
            // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
            int newValue = result + 1;
            if (newValue > 0x00FFFFFF) newValue = 1; // Roll over to 1, not 0.
            if (m_viewIdGenerator.compareAndSet(result, newValue)) {
                return result;
            }
        }
    }

    private static final AtomicInteger m_viewIdGenerator = new AtomicInteger(1);
}
