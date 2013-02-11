package com.tassadar.lorrismobile;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;

import android.content.Context;
import android.util.Log;

import com.tassadar.lorrismobile.modules.TabManager;

public class SessionMgr {
    
    public static Session create(Context ctx, String name) {
        if(m_sessions.containsKey(name))
            return null;

        File extPath = Utils.getDataFolder(ctx);
        if(extPath == null)
            return null;

        File path = new File(extPath, name);
        if(path.exists())
            return null;
        path.mkdirs();

        Session res = new Session(ctx, name, path.getAbsolutePath() + "/" + "data.sqlite");
        res.setChanged(Session.CHANGED_ALL);
        m_sessions.put(name, res);
        return res;
    }

    public static Session get(Context ctx, String name) {
        return m_sessions.get(name);
    }

    public static Session rename(String oldName, String newName) {
        Session s = m_sessions.get(oldName);

        assert s != null : "Failed to rename session " + oldName + " to " +
                            newName + " (Session does not exist)";

        m_sessions.remove(oldName);
        s.close();
        s = null;

        File extPath = Utils.getDataFolder(null);
        if(extPath == null)
            return null;

        File oldPath = new File(extPath, oldName);
        File newPath = new File(extPath, newName);
        if(!oldPath.exists() || newPath.exists()) {
            Log.e("Lorris", "Failed to move folder " + oldName + " to " + newName);
            return null;
        }
        oldPath.renameTo(newPath);


        s = loadSingleSession(LorrisApplication.getAppContext(), newName, extPath);
        if(s == null) {
            Log.e("Lorris", "Failed load renamed session " + newName);
            return null;
        }
        m_sessions.put(newName, s);
        return s;
    }

    public static void deleteSession(Context ctx, String name) {
        if(!m_sessions.containsKey(name))
            return;

        File extPath = Utils.getDataFolder(ctx);
        if(extPath == null)
            return;

        File path = new File(extPath.getAbsolutePath() + "/" + name + "/");
        if(!path.exists() || !path.isDirectory())
            return;
        
        File[] files = path.listFiles();
        for(File f : files) {
            f.delete();
        }
        path.delete();

        m_sessions.remove(name);
    }

    public static void loadSessions(Context ctx) {
        m_sessions.clear();

        File f = Utils.getDataFolder(ctx);
        if(f == null || !f.exists() || !f.canRead())
            return;
        
        File[] list = f.listFiles();
        for(File i : list) {
            if(!i.isDirectory())
                continue;

            Session s = loadSingleSession(ctx, i.getName(), f);
            if(s != null)
                m_sessions.put(s.getName(), s);
        }

        m_sessionsLoaded = true;
    }

    private static Session loadSingleSession(Context ctx, String name, File extPath) {
        File sessionPath = new File(extPath, name);
        File path = new File(sessionPath, "data.sqlite");
        if(!path.exists()) {
            File legacyPath = new File(sessionPath, name + ".sqlite");
            if(!legacyPath.exists() || !legacyPath.isFile())
                return null;

            legacyPath.renameTo(path);
        }

        if(!path.exists() || !path.isFile())
            return null;

        Session s = new Session(ctx, name, path.getAbsolutePath());
        s.loadBase();
        return s;
    }

    public static void ensureSessionsLoaded(Context ctx) {
        if(!m_sessionsLoaded)
            loadSessions(ctx);
    }

    public static boolean isNameAvailable(String name) {
        return !m_sessions.containsKey(name);
    }

    public static LinkedList<String> getSessionNames() {
        LinkedList<String> names = new LinkedList<String>();
        for(String name : m_sessions.keySet())
            names.add(name);
        Collections.sort(names);
        return names;
    }

    public static void setActiveSession(Session s) {
        m_activeSession = s;
    }

    public static Session getActiveSession() {
        return m_activeSession;
    }

    public static void saveAndClearSession() {
        SaveAndClearThread t = new SaveAndClearThread();
        t.start();
    }

    private static class SaveAndClearThread extends Thread {
        @Override
        public void run() {
            synchronized(m_activeSessionLock) {
                Session s = m_activeSession;
                m_activeSession = null;
                s.saveBase();
                TabManager.clearTabs();
            }
        }
    }

    static private HashMap<String, Session> m_sessions = new HashMap<String, Session>();
    static private boolean m_sessionsLoaded = false;
    static private Session m_activeSession;
    static private Object m_activeSessionLock = new Object();
}