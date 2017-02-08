package com.tassadar.lorrismobile.connections;

import java.util.ArrayList;
import java.util.Collections;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.SparseArray;

import com.tassadar.lorrismobile.LorrisApplication;

public class TCPConnMgr {
    
    static private class TCPConnDb extends SQLiteOpenHelper {
        private static final String DB_NAME = "tcpconns";
        private static final int DB_VERSION = 1;

        public TCPConnDb(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE tcp_conns (" +
                    "name TEXT NOT NULL," +
                    "address TEXT NOT NULL, " +
                    "port INTEGER NOT NULL DEFAULT '0');");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { }

        public ArrayList<TCPConnProto> loadProtos() {
            ArrayList<TCPConnProto> res = new ArrayList<TCPConnProto>();

            SQLiteDatabase db = getReadableDatabase();
            Cursor c = db.rawQuery("SELECT name, address, port FROM tcp_conns ORDER BY name;", null);
            while(c.moveToNext()) {
                TCPConnProto p = new TCPConnProto();
                p.name = c.getString(0);
                p.address = c.getString(1);
                p.port = c.getInt(2);
                res.add(p);
            }
            c.close();
            db.close();
            return res;
        }

        public void addProto(TCPConnProto p) {
            SQLiteDatabase db = getWritableDatabase();

            ContentValues vals = new ContentValues();
            vals.put("name", p.name);
            vals.put("address", p.address);
            vals.put("port", p.port);

            db.insert("tcp_conns", null, vals);
            db.close();
        }

        public void removeProto(String name) {
            SQLiteDatabase db = getWritableDatabase();
            db.delete("tcp_conns", "name='" + name + "'", null);
            db.close();
        }
    }

    static public ArrayList<TCPConnProto> getProtos() {
        if(m_protos != null)
            return m_protos;
        loadProtos();
        return m_protos;
    }

    static private void loadProtos() {
        ensureDb();
        m_protos = m_db.loadProtos();
    }

    static public void addProto(TCPConnProto p) {
        ensureDb();
        m_db.addProto(p);
        m_protos.add(p);
        Collections.sort(m_protos);
    }

    static public void removeProto(TCPConnProto p) {
        ensureDb();
        m_db.removeProto(p.name);
        m_protos.remove(p);
    }

    static public void replaceProto(TCPConnProto before, TCPConnProto now) {
        ensureDb();
        m_db.removeProto(before.name);
        m_db.addProto(now);

        int size = m_protos.size();
        for(int i = 0; i < size; ++i) {
            if(m_protos.get(i).name.equals(before.name)) {
                m_protos.set(i, now);
                break;
            }
        }

        SparseArray<Connection> conns = ConnectionMgr.cloneConnArray();
        size = conns.size();
        for(int i = 0; i < size; ++i) {
            Connection c = conns.valueAt(i);
            if(c.getType() != Connection.CONN_TCP)
                continue;

            TCPConnProto tp = ((TCPConnection)c).getProto();
            if (tp.sameAs(before)) {
                ((TCPConnection)c).setProto(now);
                break;
            }
        }
    }

    static public TCPConnProto getProto(String name) {
        int size = m_protos.size();
        for(int i = 0; i < size; ++i) {
            if(m_protos.get(i).name.equals(name))
                return m_protos.get(i);
        }
        return null;
    }

    static boolean contains(String name) {
        int size = m_protos.size();
        for(int i = 0; i < size; ++i) {
            if(m_protos.get(i).name.equalsIgnoreCase(name))
                return true;
        }
        return false;
    }

    static private void ensureDb() {
        if(m_db == null)
            m_db = new TCPConnDb(LorrisApplication.getAppContext());
    }

    static private TCPConnDb m_db = null;
    static private ArrayList<TCPConnProto> m_protos = null;
}
