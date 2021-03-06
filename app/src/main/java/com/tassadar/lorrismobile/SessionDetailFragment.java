package com.tassadar.lorrismobile;

import java.util.ArrayList;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

@TargetApi(11)
public class SessionDetailFragment extends Fragment {

    private static final int ACTCODE_EDIT_SESSION = 1;

    public interface OnSessionChangedListener {
        public void onSessionsChanged();
        public void deleteSession(String name);
        public void openSession(String name);
    }

    @Override
    public void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        m_session_name = null;
    }

    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.session_detail, container, false);
        Button b = (Button)v.findViewById(R.id.load_session);
        b.setOnClickListener(new LoadSessionListener());
        return v;
    }

    @Override
    public void onCreateOptionsMenu (Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.session_details, menu);
        m_menu = menu;
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item) {
        switch(item.getItemId()) {
        case R.id.edit_session:
            Intent i = new Intent(getActivity(), SessionEditActivity.class);
            i.putExtra("edit_session", m_session_name);
            startActivityForResult(i, ACTCODE_EDIT_SESSION);
            return true;
        case R.id.delete_session:
            ((OnSessionChangedListener)getActivity()).deleteSession(m_session_name);
            return true;
        }
        return false;
    }

    @Override
    public void onActivityResult (int requestCode, int resultCode, Intent data) {
        if(resultCode != Activity.RESULT_OK)
            return;
        
        switch(requestCode) {
            case ACTCODE_EDIT_SESSION:
                SessionMgr.ensureSessionsLoaded(getActivity());
                Session s = SessionMgr.get(getActivity(), data.getExtras().getString("session_name"));
                if(s != null)
                    loadSession(s);

                ((OnSessionChangedListener)getActivity()).onSessionsChanged();
                break;
        }
    }

    public void loadSession(Session session) {
        TextView name = (TextView)getView().findViewById(R.id.session_name);
        TextView desc = (TextView)getView().findViewById(R.id.session_desc);
        ImageView image = (ImageView)getView().findViewById(R.id.session_image);
        LinearLayout tab_desc = (LinearLayout)getView().findViewById(R.id.open_tabs);
        
        if(name == null || desc == null || image == null || tab_desc == null)
            return;

        m_session_name = session.getName();

        name.setText(m_session_name);
        desc.setText(session.getDesc());

        Bitmap bmp = session.getImage();
        if(bmp != null)
            image.setImageBitmap(session.getImage());
        else
            image.setImageResource(R.drawable.photo_ph);

        tab_desc.removeAllViews();
        if(session.hasTabDescLoaded())
            fillTabDetails(session.loadTabDesc());
        else
            new TabDescLoader().execute(session);
    }

    private class TabDescLoader extends AsyncTask<Session, Void, ArrayList<String> > {
        @Override
        protected ArrayList<String> doInBackground(Session... s) {
            ArrayList<String> res = s[0].loadTabDesc();
            if(res.isEmpty())
                return null;
            return res;
        }

        @Override
        protected void onPostExecute(ArrayList<String> res) {
            fillTabDetails(res);
        }
    };

    private void fillTabDetails(ArrayList<String> d) {
        if(d == null || d.isEmpty())
            return;

        Activity act = getActivity();
        if(act == null)
            return;

        LinearLayout tab_desc = (LinearLayout)getView().findViewById(R.id.open_tabs);
        tab_desc.removeAllViews();

        int size = d.size();
        for(int i = 0; i < size; ++i) {
            View v = View.inflate(act,  R.layout.opened_tab_item, null);

            TextView t = (TextView)v.findViewById(R.id.desc_text);
            t.setText(Html.fromHtml(d.get(i)));

            if(i == size-1)
                v.findViewById(R.id.separator).setVisibility(View.GONE);

            tab_desc.addView(v);
        }
    }

    
    private class LoadSessionListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            ((OnSessionChangedListener)getActivity()).openSession(m_session_name);
        }
    }

    public void setVisible(boolean visible) {
        if(m_menu != null)
        {
            final int[] items = { R.id.edit_session, R.id.delete_session };
            for(int i = 0; i < items.length; ++i)
            {
                MenuItem it = m_menu.findItem(items[i]);
                if(it != null)
                    it.setVisible(visible);
            }
        }
        getView().setVisibility(visible ? View.VISIBLE : View.GONE);
    }
    

    private String m_session_name;
    private Menu m_menu;
}