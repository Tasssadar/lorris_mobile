package com.tassadar.lorrismobile.connections;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tassadar.lorrismobile.R;

public class TCPConnFragment extends ConnFragment {

    private static final int PROTO_ADD = 0;
    private static final int PROTO_RM  = 1;

    @Override
    public void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.conn_tcp_fragment, container, false);

        new ProtoLoader().execute();

        return v;
    }

    @Override
    public void onCreateOptionsMenu (Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.conn_tcp_fragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item) {
        switch(item.getItemId()) {
            case R.id.add_tcp_conn:
                showConnDlg(null);
                return true;
        }
        return false;
    }

    private class ProtoLoader extends AsyncTask<Object, Void, ArrayList<TCPConnProto> > {
        @Override
        protected void onPreExecute() {
            setProgressVisible(true);
        }

        @Override
        protected ArrayList<TCPConnProto> doInBackground(Object... arg) {
            for(int i = 0; i < arg.length; ) {
                TCPConnProto p = (TCPConnProto)arg[i++];
                int act = (Integer)arg[i++];

                switch(act) {
                case PROTO_ADD:
                    TCPConnMgr.addProto(p);
                    break;
                case PROTO_RM:
                    TCPConnMgr.removeProto(p);
                    break;
                }
            }

            ArrayList<TCPConnProto> res = TCPConnMgr.getProtos();
            return res;
        }

        @Override
        protected void onPostExecute(ArrayList<TCPConnProto> res) {
            loadProtos(res);
            setProgressVisible(false);
        }
    };

    private void loadProtos(ArrayList<TCPConnProto> res) {
        View v = getView();
        if(v == null)
            return;

        LinearLayout l = (LinearLayout)v.findViewById(R.id.tcp_conns);
        l.removeAllViews();

        Activity act = getActivity();
        int size = res.size();
        TCPConnProto p;
        for(int i = 0; i < size; ++i) {
            p = res.get(i);

            View it = View.inflate(act, R.layout.tcp_list_item, null);

            TextView t = (TextView)it.findViewById(R.id.name);
            t.setText(p.name);
            t = (TextView)it.findViewById(R.id.address);
            t.setText(p.address + ":" + String.valueOf(p.port));
            
            ProtoClickListener listener = new ProtoClickListener(p);
            ImageButton b = (ImageButton)it.findViewById(R.id.edit);
            b.setOnClickListener(listener);
            b = (ImageButton)it.findViewById(R.id.delete);
            b.setOnClickListener(listener);

            LinearLayout lit = (LinearLayout)it.findViewById(R.id.tcp_list_item_layout);
            lit.setOnClickListener(listener);

            l.addView(it);
        }
    }
    
    private class ProtoClickListener implements OnClickListener, DialogInterface.OnClickListener {
        private TCPConnProto m_proto;
        private View m_disableBtn;

        public ProtoClickListener(TCPConnProto p) {
            m_proto = p;
        }

        @Override
        public void onClick(View v) {
            switch(v.getId()) {
                case R.id.tcp_list_item_layout:
                    v.setEnabled(false);
                    TCPConnection conn = ConnectionMgr.createTcpConn(m_proto);
                    m_interface.onConnectionSelected(conn);
                    break;
                case R.id.edit:
                    showConnDlg(m_proto);
                    break;
                case R.id.delete:
                    m_disableBtn = v;

                    String text = getResources().getString(R.string.erase_tcp_conn_text);

                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setMessage(String.format(text, m_proto.name))
                           .setTitle(R.string.erase_tcp_conn_title)
                           .setPositiveButton(R.string.ok, this)
                           .setNegativeButton(R.string.cancel, null)
                           .setIcon(R.drawable.alerts_and_states_warning);
                    builder.show();
                    break;
            }
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if(which != AlertDialog.BUTTON_POSITIVE)
                return;

            m_disableBtn.setEnabled(false);
            new ProtoLoader().execute(m_proto, Integer.valueOf(PROTO_RM));
        }
    }

    private void showConnDlg(TCPConnProto p) {

        View v = View.inflate(getActivity(), R.layout.new_tcp_conn, null);
        if(p != null) {
            EditText t = (EditText)v.findViewById(R.id.name);
            t.setText(p.name);
            t = (EditText)v.findViewById(R.id.address);
            t.setText(p.address);
            t = (EditText)v.findViewById(R.id.port);
            t.setText(String.valueOf(p.port));
        }

        AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
        b.setTitle(R.string.add_tcp_conn)
            .setView(v)
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null);

        AlertDialog d = b.create();
        new AddConnListener(p, v, d);
        d.show();
    }

    private class AddConnListener implements OnClickListener, OnShowListener {
        private View m_view;
        private AlertDialog m_dlg;
        private TCPConnProto m_editProto;

        public AddConnListener(TCPConnProto p, View v, AlertDialog d) {
            m_view = v;
            m_dlg = d;
            m_editProto = p;
            d.setOnShowListener(this);
        }

        @Override
        public void onShow(DialogInterface arg0) {
            Button b = m_dlg.getButton(AlertDialog.BUTTON_POSITIVE);
            b.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if(m_view == null)
                return;

            EditText t = (EditText)m_view.findViewById(R.id.name);
            String name = t.getText().toString();

            t = (EditText)m_view.findViewById(R.id.address);
            String address = t.getText().toString();
            
            t = (EditText)m_view.findViewById(R.id.port);
            String port = t.getText().toString();

            int error = 0;
            int portNum = 0;
            if(name.length() == 0 || address.length() == 0 || port.length() == 0)
                error = R.string.all_fields;
            else if((m_editProto == null || !m_editProto.name.equalsIgnoreCase(name))
                    && TCPConnMgr.contains(name)) {
                error = R.string.name_taken;
            } else {
                try { 
                    portNum = Integer.valueOf(port);
                    if(portNum < 0 || portNum > 65535)
                        error = R.string.invalid_port;
                } catch(NumberFormatException e) {
                    error = R.string.invalid_port;
                }
            }

            if(error != 0) {
                TextView err = (TextView)m_view.findViewById(R.id.error);
                err.setVisibility(View.VISIBLE);
                err.setText(error);
                return;
            }

            TCPConnProto p = new TCPConnProto();
            p.name = name;
            p.address = address;
            p.port = portNum;

            Object[] act = null;
            if(m_editProto == null) {
                act = new Object[2];
                act[0] = p;
                act[1] = Integer.valueOf(PROTO_ADD);
            } else {
                act = new Object[4];
                act[0] = m_editProto;
                act[1] = Integer.valueOf(PROTO_RM);
                act[2] = p;
                act[3] = Integer.valueOf(PROTO_ADD);
            }

            new ProtoLoader().execute(act);

            m_dlg.dismiss();
        }
    }
}
