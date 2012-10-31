package com.tassadar.lorrismobile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Calendar;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class Session extends SQLiteOpenHelper {
    
    private static final int DB_VERSION = 1;
    
    private static final int CHANGED_NAME      = 0x01;
    private static final int CHANGED_DESC      = 0x02;
    private static final int CHANGED_IMG       = 0x04;
    private static final int CHANGED_LAST_OPEN = 0x08;
    
    public static final int CHANGED_ALL       = 0xFF;

    public Session(Context ctx, String name, String path) {
        super(ctx, path, null, DB_VERSION);

        m_name = name;
        m_desc = "";
        m_changed = 0;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE session_info (" +
                "name TEXT NOT NULL," +
                "desc TEXT, " +
                "image BLOB," +
                "creation_time INTEGER NOT NULL DEFAULT '0'," +
                "last_open_time INTEGER NOT NULL DEFAULT '0');");
        
        ByteArrayOutputStream str = new ByteArrayOutputStream();
        if(m_image != null)
            m_image.compress(Bitmap.CompressFormat.JPEG, 100, str);

        try {
            str.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        ContentValues vals = new ContentValues();
        vals.put("name", m_name);
        vals.put("desc", m_desc);
        vals.put("image", str.toByteArray());
        vals.put("creation_time", Calendar.getInstance().getTimeInMillis()/1000);
        vals.put("last_open_time", 0);
        db.insert("session_info", null, vals);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub
        
    }
    
    public void save() {
        if(m_changed == 0)
            return;

        SQLiteDatabase db = getWritableDatabase();
        ContentValues vals = new ContentValues();
        
        if((m_changed & CHANGED_NAME) != 0)
            vals.put("name", m_name);
        
        if((m_changed & CHANGED_DESC) != 0)
            vals.put("desc", m_desc);
        
        if((m_changed & CHANGED_IMG) != 0) {
            ByteArrayOutputStream str = new ByteArrayOutputStream();
            if(m_image != null)
                m_image.compress(Bitmap.CompressFormat.JPEG, 100, str);

            try {
                str.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }

            vals.put("image", str.toByteArray());
        }
        
        if((m_changed & CHANGED_LAST_OPEN) != 0)
            vals.put("last_open_time", m_last_open);

        db.update("session_info", vals, null, null);
        db.close();
    }

    public boolean load() {
        SQLiteDatabase db = getReadableDatabase();

        Cursor c = db.query("session_info", new String[] { "desc", "image", "last_open_time", "name" }, null, null, null, null, null, null);

        c.moveToNext();

        m_desc = c.getString(0);
        m_last_open = c.getLong(2);

        byte[] imgData = c.getBlob(1);
        if(imgData != null && imgData.length != 0) {
            m_image = BitmapFactory.decodeByteArray(imgData, 0, imgData.length);
        }

        c.close();
        db.close();
        return true;
    }

    public String getName() {
        return m_name;
    }

    public String getDesc() {
        return m_desc;
    }

    public Bitmap getImage() {
        return m_image;
    }

    public void setName(String name) {
        if(name == m_name)
            return;
        m_changed |= CHANGED_NAME;
        m_name = name;
    }

    public void setDesc(String desc) {
        if(desc == m_desc)
            return;
        m_changed |= CHANGED_DESC;
        m_desc = desc;
    }
    
    public void setImage(Bitmap img) {
        if(m_image != null && m_image.equals(img))
            return;
        m_changed |= CHANGED_IMG;
        m_image = img;
    }
    
    public void setLastOpenTime() {
        m_last_open = (int) (Calendar.getInstance().getTimeInMillis()/1000);
        m_changed |= CHANGED_LAST_OPEN;
    }
    
    public void setChanged(int changed) {
        m_changed = changed;
    }

    private String m_name;
    private String m_desc;
    private Bitmap m_image;
    private int m_changed;
    private long m_last_open;
}