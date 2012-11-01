package com.tassadar.lorrismobile;

import java.io.File;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;

public class Utils {

    public static File getDataFolder(Context ctx) {
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

    public static Bitmap resizeBitmap(Bitmap bmp, int targetW, int targetH) {
        if(bmp == null)
            return null;

        int w = bmp.getWidth();
        int h = bmp.getHeight();

        float scale = ((float)targetW) / w;
        if(h * scale > targetH)
            scale = ((float)targetH) / h;

        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);

        return Bitmap.createBitmap(bmp, 0, 0, w, h, matrix, false);
    }

}
