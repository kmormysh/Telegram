package com.katy.telegram.Activities;

import android.app.Activity;
import android.content.ContentValues;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.katy.telegram.R;

import java.io.File;

import butterknife.ButterKnife;
import butterknife.InjectView;
import uk.co.senab.photoview.PhotoViewAttacher;

public class ImageViewActivity extends BaseActivity {

    @InjectView(R.id.toolbar)
    Toolbar toolbar;
    @InjectView(R.id.image)
    ImageView image;

    private PhotoViewAttacher mAttacher;
    private String imagePath;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_view);
        ButterKnife.inject(this);

        toolbar.setNavigationIcon(this.getResources().getDrawable(R.drawable.ic_back));
        toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_back));
        toolbar.getBackground().setAlpha(1);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(null);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(Activity.RESULT_CANCELED);
                finish();
            }
        });

        imagePath = getIntent().getStringExtra("imagePath");
        image.setImageBitmap(BitmapFactory.decodeFile(imagePath));

        mAttacher = new PhotoViewAttacher(image);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.image_view, menu);
        getSupportActionBar().setTitle(null);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.save_to_gallery) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.MediaColumns.DATA, imagePath);
            getApplicationContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            Toast.makeText(getApplicationContext(), "File is saved", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.delete) {
            File file = new File(imagePath); //android.os.Environment.getExternalStorageDirectory() + imagePath);
            if (file.exists()) {
                file.delete();
                Toast.makeText(getApplicationContext(), "Deleted", Toast.LENGTH_SHORT).show();
            }
        }
        return super.onOptionsItemSelected(item);
    }
}
