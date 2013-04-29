package com.tassadar.lorrismobile;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;

import com.tassadar.lorrismobile.connections.Connection;
import com.tassadar.lorrismobile.modules.Tab;

public class SessionService extends Service {

    public static final String SAVE_SESSION = "com.tassadar.lorrismobile.SAVE_SESSION";
    public static final String CLOSE_SESSION = "com.tassadar.lorrismobile.CLOSE_SESSION";

    public interface SessionServiceListener {
        public void onConnsLoad(ArrayList<ContentValues> values);
        public void onTabsLoad(ArrayList<ContentValues> values);
    }

    public class SessionBinder extends Binder {
        SessionService getService() {
            return SessionService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return m_binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        Log.i("Lorris", "Starting SessionService");

        m_notificationMgr = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        showNotification();
    }

    @Override
    public void onDestroy() {
        m_notificationMgr.cancel(R.string.service);
    }

    private void showNotification() {
        NotificationCompat.Builder b = new NotificationCompat.Builder(this);

        String text = String.format(getText(R.string.service).toString(),
                SessionMgr.getActiveSession().getName());

        Intent i = new Intent(this, WorkspaceActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, i, 0);

        i = new Intent(CLOSE_SESSION);
        i.putExtra("sessionName", SessionMgr.getActiveSession().getName());
        PendingIntent closeIntent = PendingIntent.getBroadcast(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

        i = new Intent(SAVE_SESSION);
        i.putExtra("sessionName", SessionMgr.getActiveSession().getName());
        PendingIntent saveIntent = PendingIntent.getBroadcast(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

        b.setContentTitle(getText(R.string.app_name))
         .setContentText(text)
         .setOngoing(true)
         .setContentIntent(contentIntent)
         .setSmallIcon(R.drawable.ic_launcher)
         .addAction(R.drawable.content_save, getText(R.string.save), saveIntent)
         .addAction(R.drawable.navigation_cancel, getText(R.string.close), closeIntent);

        Notification n = b.build();
        startForeground(R.string.service, n);
    }

    public void saveSession(Session s, SparseArray<Tab> tabs,
            SparseArray<Connection> conns, boolean report) {
            Log.i("Lorris", "Spawning save thread\n");

            for(int i = 0; i < conns.size(); ++i)
                conns.valueAt(i).addRef();
            new SaveSessionThread(s, tabs, conns, report).start();
    }

    private class SaveSessionThread extends Thread {
        private SparseArray<Tab> m_tabs;
        private SparseArray<Connection> m_conns;
        private Session m_session;
        private boolean m_report;
        private ToastHandler m_handler;

        public SaveSessionThread(Session s, SparseArray<Tab> tabs,
                SparseArray<Connection> conns, boolean report) {
            m_tabs = tabs;
            m_conns = conns;
            m_session = s;
            m_report = report;

            if(m_report) {
                Toast.makeText(SessionService.this, R.string.saving_session, Toast.LENGTH_SHORT).show();
                m_handler = new ToastHandler(SessionService.this);
            }
        }

        @Override
        public void run() {
            synchronized(m_session) {
                m_session.acquireDBRef();
                m_session.saveBase();
                m_session.saveTabs(m_tabs);
                m_session.saveConns(m_conns);
                m_session.releaseDBRef();

                for(int i = 0; i < m_conns.size(); ++i)
                    m_conns.valueAt(i).rmRef();

                Log.i("Lorris", "Session " + m_session.getName() + " saved\n");
                if(m_report)
                    m_handler.obtainMessage(0, m_session.getName()).sendToTarget();
            }
        }
    }

    private static class ToastHandler extends Handler {
        private WeakReference<Context> m_ctx;

        public ToastHandler(Context ctx) {
            m_ctx = new WeakReference<Context>(ctx);
        }

        @Override
        public void handleMessage(Message msg) {
            Context c = m_ctx.get();
            if(c == null)
                return;

            String text = String.format(c.getText(R.string.session_saved).toString(), (String)msg.obj);
            Toast.makeText(c, text, Toast.LENGTH_SHORT).show();
        }
    }

    public void loadSession(Session s, SessionServiceListener listener) {
        new LoadSessionThread(s, listener).start();
    }

    private class LoadSessionThread extends Thread {
        private Session m_session;
        private WeakReference<SessionServiceListener> m_listener;

        public LoadSessionThread(Session s, SessionServiceListener listener) {
            m_session = s;
            m_listener = new WeakReference<SessionServiceListener>(listener);
        }

        @Override
        public void run() {
            ArrayList<ContentValues> values = m_session.loadConnections();

            SessionServiceListener listener = m_listener.get();
            if(listener != null)
                listener.onConnsLoad(values);

            values = m_session.loadTabs();
            listener = m_listener.get();
            if(listener != null)
                listener.onTabsLoad(values);
        }
    }

    private final IBinder m_binder = new SessionBinder();
    private NotificationManager m_notificationMgr;
}
