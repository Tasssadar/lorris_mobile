package com.tassadar.lorrismobile;

import java.io.ByteArrayOutputStream;
import java.io.File;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Environment;

public class Utils {

    public static File getDataFolder(Context ctx) { 
        File f = ctx.getExternalFilesDir(null);

        if(f != null && f.exists())
            return f;

        f = ctx.getFilesDir();
        if(f != null && f.exists())
            return f;

        f = new File(Environment.getExternalStorageDirectory().getPath() +
                "/Android/data/com.tassadar.lorrismobile/files/");
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
    
    public static boolean compareBitmap(Bitmap bitmap1, Bitmap bitmap2){
        if(bitmap1 == bitmap2)
            return true;

        if(bitmap1 == null || bitmap2 == null)
            return false;

        try{
            ByteArrayOutputStream stream1 = new ByteArrayOutputStream();
            bitmap1.compress(Bitmap.CompressFormat.JPEG, 100, stream1);
            stream1.flush();
            byte[] bitmapdata1 = stream1.toByteArray();
            stream1.close();

            ByteArrayOutputStream stream2 = new ByteArrayOutputStream();
            bitmap2.compress(Bitmap.CompressFormat.JPEG, 100, stream2);
            stream2.flush();
            byte[] bitmapdata2 = stream2.toByteArray();
            stream2.close();

            return bitmapdata1.equals(bitmapdata2);
        }
        catch (Exception e) {
            // TODO: handle exception
        }
        return false;
    }

}
