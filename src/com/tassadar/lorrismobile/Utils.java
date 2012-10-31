package com.tassadar.lorrismobile;

import java.io.File;

import android.content.Context;
import android.os.Environment;

public class Utils {

    static File getDataFolder(Context ctx) {
        File f = ctx.getExternalFilesDir(null);

        if(f != null && f.exists())
            return f;

        f = ctx.getFilesDir();
        if(f != null && f.exists())
            return f;

        f = new File("/sdcard/Android/data/com.tassadar.lorrismobile/files/");
        f.mkdirs();
        if(f.exists())
            return f;
        return null;
    }
}