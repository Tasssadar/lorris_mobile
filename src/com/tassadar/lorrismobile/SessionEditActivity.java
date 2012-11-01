package com.tassadar.lorrismobile;

import java.util.List;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class SessionEditActivity extends Activity {

    static final int ACTCODE_CAMERA = 1;
    static final int ACTCODE_SELECT_IMG = 2;

    static final int SESSION_IMG_DIMENSION = 256;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.session_edit);

        if(Build.VERSION.SDK_INT >= 11)
            setUpActionBar();

        setTitle(R.string.new_session);

        // disable camera if not present
        if (!canTakePhoto()) {
            View btn = findViewById(R.id.take_photo_btn);
            if(btn != null)
                btn.setVisibility(View.GONE);
        }

        TextView name_edit = (TextView)findViewById(R.id.session_name_edit);
        if(name_edit != null)
            name_edit.setOnFocusChangeListener(new NameFocusChangedAdapter());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.session_edit, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId())
        {
        case android.R.id.home:
        case R.id.cancel_edit_session:
            setResult(RESULT_CANCELED);
            finish();
            return true;
        case R.id.save_session:
            on_saveSession_clicked(null);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode != Activity.RESULT_OK)
            return;

        switch(requestCode)
        {
        case ACTCODE_CAMERA:
        {
            Bundle extras = data.getExtras();
            Bitmap bmp = Utils.resizeBitmap((Bitmap)extras.get("data"), SESSION_IMG_DIMENSION, SESSION_IMG_DIMENSION);
            ImageView w = (ImageView)findViewById(R.id.session_image);
            if(w != null)
                w.setImageBitmap(bmp);
            m_image = bmp;
            break;
        }
        case ACTCODE_SELECT_IMG:
        {
            Uri selectedImage = data.getData();

            String[] filePathColumn = {MediaStore.Images.Media.DATA};
            Cursor cursor = getContentResolver().query(
                               selectedImage, filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String path = cursor.getString(columnIndex);
            cursor.close();

            m_image = Utils.resizeBitmap(BitmapFactory.decodeFile(path), SESSION_IMG_DIMENSION, SESSION_IMG_DIMENSION);
            ImageView w = (ImageView)findViewById(R.id.session_image);
            if(w != null && m_image != null)
                w.setImageBitmap(m_image);
            break;
        }
        default:
            super.onActivityResult(requestCode, resultCode, data);
            break;
        }
        
    }

    @TargetApi(11)
    private void setUpActionBar()
    {
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private boolean isIntentAvailable(String action) {
        final PackageManager packageManager = getPackageManager();
        final Intent intent = new Intent(action);
        List<ResolveInfo> list =
                packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    private boolean canTakePhoto() {
        return (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA) ||
                getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)) &&
                isIntentAvailable(MediaStore.ACTION_IMAGE_CAPTURE);
    }

    public void on_take_photo_bnt_clicked(View v) {
        if (!canTakePhoto())
            return;

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(takePictureIntent, ACTCODE_CAMERA);
    }

    public void on_selectImg_clicked(View v) {
        if(!isIntentAvailable(Intent.ACTION_PICK)) {
            Toast.makeText(this, R.string.no_pick_handler, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(intent, ACTCODE_SELECT_IMG);
    }

    public void setErrorText(boolean show, int textId) {
        LinearLayout l = (LinearLayout)findViewById(R.id.error_layout);
        TextView v = (TextView)findViewById(R.id.error_text);

        if(l == null || v == null)
            return;

        l.setVisibility(show ? View.VISIBLE : View.GONE);
        v.setText(textId);
    }
    
    private class NameFocusChangedAdapter implements OnFocusChangeListener {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            String text = ((TextView)v).getText().toString();
            setErrorText(!SessionMgr.isNameAvailable(text), R.string.name_taken);
        }
    }
    
    private void on_saveSession_clicked(View w) {
        TextView name = (TextView)findViewById(R.id.session_name_edit);
        TextView desc = (TextView)findViewById(R.id.session_notes);

        if(name == null || desc == null)
            return;
        
        String name_str = name.getText().toString();

        if(name_str == null || name_str.equals("")) {
            setErrorText(true, R.string.name_empty);
            return;
        }
            
        if(!SessionMgr.isNameAvailable(name_str)) {
            setErrorText(true, R.string.name_taken);
            return;
        }

        setErrorText(false, R.string.name_taken);

        Session session = SessionMgr.create(this, name_str);
        if(session == null)
        {
            Toast.makeText(this, R.string.session_create_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        session.setDesc(desc.getText().toString());
        session.setImage(m_image);

        session.save();

        setResult(RESULT_OK);
        finish();
    }
    
    private Bitmap m_image;
}