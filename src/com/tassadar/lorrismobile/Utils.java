package com.tassadar.lorrismobile;

import java.io.ByteArrayOutputStream;
import java.io.File;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Build;
import android.os.Environment;
import android.util.SparseArray;

public class Utils {

    public static File getDataFolder(Context ctx) {
        if(ctx == null)
            ctx = LorrisApplication.getAppContext();

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

    public static <E> SparseArray<E> cloneSparseArray(SparseArray<E> array) {
        if(Build.VERSION.SDK_INT >= 14)
            return array.clone();
        else
        {
            SparseArray<E> res = new SparseArray<E>();
            int size = array.size();
            for(int i = 0; i < size; ++i)
                res.append(array.keyAt(i), array.valueAt(i));
            return res;
        }
    }

    public static void writeIntToByteArray(int val, byte[] array, int offset) {
        array[offset++] = (byte)(val >> 24);
        array[offset++] = (byte)(val >> 16);
        array[offset++] = (byte)(val >> 8);
        array[offset++] = (byte)val;
    }
    
    public static int readIntFromByteArray(byte[] array, int offset) {
        int res = (array[offset++] & 0xFF) << 24;
        res |= (array[offset++] & 0xFF) << 16;
        res |= (array[offset++] & 0xFF) << 8;
        res |= array[offset++] & 0xFF;
        return res;
    }

    public static boolean byteArrayContains(final byte[] array, final byte what) {
        for(final byte e : array)
            if(e == what)
                return true;
        return false;
    }

    public static int max(int a, int b) {
        return a > b ? a : b;
    }

    public static int min(int a, int b) {
        return a < b ? a : b;
    }
}
