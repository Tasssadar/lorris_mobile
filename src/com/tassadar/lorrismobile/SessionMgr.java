package com.tassadar.lorrismobile;

import java.io.File;
import java.util.Collections;
import java.util.LinkedList;

import android.content.Context;

public class SessionMgr {
    
    public static Session create(Context ctx, String name) {
        if(!isNameAvailable(name))
            return null;

        File extPath = Utils.getDataFolder(ctx);
        if(extPath == null)
            return null;

        File path = new File(extPath.getAbsolutePath() + "/" + name + "/");
        if(path.exists())
            return null;
        path.mkdirs();

        Session res = new Session(ctx, name, path.getAbsolutePath() + "/" + name + ".sqlite");
        res.setChanged(Session.CHANGED_ALL);
        return res;
    }

    public static Session get(Context ctx, String name) {
        if(isNameAvailable(name))
            return null;

        File extPath = Utils.getDataFolder(ctx);
        if(extPath == null)
            return null;

        File path = new File(extPath.getAbsolutePath() + "/" + name + "/" + name + ".sqlite");
        if(!path.exists() || !path.isFile())
            return null;
        
        Session res = new Session(ctx, name, path.getAbsolutePath());
        res.load();
        return res;
    }
    
    public static boolean isNameAvailable(String name) {
        return !m_sessionNames.contains(name);
    }

    public static void loadAvailableNames(Context ctx) {
        m_sessionNames.clear();
        
        File f = Utils.getDataFolder(ctx);
        if(f == null || !f.exists() || !f.canRead())
            return;
        
        File[] list = f.listFiles();
        for(File i : list) {
            if(i.isDirectory())
                m_sessionNames.add(i.getName());
        }

        Collections.sort(m_sessionNames);
    }

    @SuppressWarnings("unchecked")
    public static LinkedList<String> getSessionNames() {
        return (LinkedList<String>) m_sessionNames.clone();
    }

    static private LinkedList<String> m_sessionNames = new LinkedList<String>();
}