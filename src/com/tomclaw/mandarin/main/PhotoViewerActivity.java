package com.tomclaw.mandarin.main;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.*;
import android.widget.Button;
import android.widget.TextView;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.BitmapCache;
import com.tomclaw.mandarin.core.PreferenceHelper;
import com.tomclaw.mandarin.core.TaskExecutor;
import com.tomclaw.mandarin.core.WeakObjectTask;
import com.tomclaw.mandarin.main.views.TouchImageView;
import com.tomclaw.mandarin.util.BitmapHelper;
import com.tomclaw.mandarin.util.FileHelper;

/**
 * Created by Solkin on 05.12.2014.
 */
public class PhotoViewerActivity extends AppCompatActivity {

    public static final String EXTRA_PICTURE_URI = "picture_uri";
    public static final String EXTRA_PICTURE_NAME = "picture_name";
    public static final String EXTRA_PREVIEW_HASH = "thumbnail_hash";
    public static final String EXTRA_SELECTED_COUNT = "sending_count";
    public static final String EXTRA_PHOTO_ENTRY = "photo_entry";

    public static final String SELECTED_PHOTO_ENTRY = "selected_image_id";

    public static final int ANIMATION_DURATION = 250;

    private TouchImageView imageView;

    private View pickerButtons;

    private View photoViewFailedView;
    private View doneButton;
    private TextView doneButtonTextView;
    private TextView doneButtonBadgeTextView;

    private Uri uri;
    private String name;
    private String previewHash;
    private int selectedCount;
    private PhotoEntry photoEntry;
    private boolean hasPreview = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(PreferenceHelper.isDarkTheme(this) ?
                R.style.Theme_Mandarin_Dark : R.style.Theme_Mandarin_Light);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.photo_viewer_activity);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Extract picture path we must show.
        Bundle extras = getIntent().getExtras();
        String uriString = extras.getString(EXTRA_PICTURE_URI);
        name = extras.getString(EXTRA_PICTURE_NAME);
        previewHash = extras.getString(EXTRA_PREVIEW_HASH);
        selectedCount = extras.getInt(EXTRA_SELECTED_COUNT, -1);
        photoEntry = (PhotoEntry) extras.getSerializable(EXTRA_PHOTO_ENTRY);
        // Check the parameters are correct.
        if (TextUtils.isEmpty(uriString) || TextUtils.isEmpty(name)) {
            finish();
        } else {
            uri = Uri.parse(uriString);
        }

        // Preparing for action bar.
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setDisplayShowTitleEnabled(true);
            bar.setDisplayHomeAsUpEnabled(true);
            bar.setTitle(name);
        }

        photoViewFailedView = findViewById(R.id.photo_view_failed);

        pickerButtons = findViewById(R.id.picker_buttons);

        Button cancelButton = (Button) findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        doneButton = findViewById(R.id.done_button);
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendSelectedPhotos();
            }
        });

        cancelButton.setText(getString(R.string.cancel).toUpperCase());
        doneButtonTextView = (TextView) doneButton.findViewById(R.id.done_button_text);
        doneButtonTextView.setText(getString(R.string.send).toUpperCase());
        doneButtonBadgeTextView = (TextView) doneButton.findViewById(R.id.done_button_badge);

        // Check for no selection here and...
        if (selectedCount == -1) {
            // ... hide picker buttons.
            pickerButtons.setVisibility(View.GONE);
        } else {
            // ... update picker buttons.
            updateSelectedCount();
        }

        imageView = (TouchImageView) findViewById(R.id.touch_image_view);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActionBar bar = getSupportActionBar();
                if (bar != null) {
                    if (bar.isShowing()) {
                        bar.hide();
                        hidePickerButtons();
                    } else {
                        bar.show();
                        showPickerButtons();
                    }
                }
            }
        });

        // Checking for preview hash is not empty and show it in a block way.
        if (!TextUtils.isEmpty(previewHash)) {
            // Preview hash seems to be in a heap cache.
            setBitmap(BitmapCache.getInstance().getBitmapSync(previewHash,
                    BitmapCache.BITMAP_SIZE_ORIGINAL, BitmapCache.BITMAP_SIZE_ORIGINAL, true, false));
        }

        // Sampling and showing picture.
        samplePicture();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.photo_viewer_activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                onBackPressed();
                return true;
            }
            case R.id.view_in_external_app_menu: {
                Intent intent = new Intent();
                intent.setAction(android.content.Intent.ACTION_VIEW);
                intent.setDataAndType(uri, FileHelper.getMimeType(name));
                startActivity(intent);
                return true;
            }
        }
        return false;
    }

    private void hidePickerButtons() {
        animatePickerButtons(new TranslateAnimation(0, 0, 0, pickerButtons.getHeight()), new AlphaAnimation(1, 0.0f),
                new AccelerateInterpolator());
    }

    private void showPickerButtons() {
        animatePickerButtons(new TranslateAnimation(0, 0, pickerButtons.getHeight(), 0), new AlphaAnimation(0.0f, 1),
                new DecelerateInterpolator());
    }

    private void animatePickerButtons(TranslateAnimation translateAnimation, AlphaAnimation alphaAnimation,
                                      Interpolator interpolator) {
        AnimationSet animationSet = new AnimationSet(true);
        animationSet.addAnimation(translateAnimation);
        animationSet.addAnimation(alphaAnimation);
        animationSet.setFillAfter(true);
        animationSet.setDuration(ANIMATION_DURATION);
        animationSet.setInterpolator(interpolator);

        pickerButtons.startAnimation(animationSet);
    }

    private void updateSelectedCount() {
        if (selectedCount <= 1) {
            doneButtonTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.selectphoto_small_active, 0, 0, 0);
            doneButtonBadgeTextView.setVisibility(View.GONE);
        } else {
            doneButtonTextView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            doneButtonBadgeTextView.setVisibility(View.VISIBLE);
            doneButtonBadgeTextView.setText("" + selectedCount);
        }
    }

    private void sendSelectedPhotos() {
        Intent intent = new Intent();
        intent.putExtra(SELECTED_PHOTO_ENTRY, photoEntry);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void samplePicture() {
        TaskExecutor.getInstance().execute(new PhotoSamplingTask(this, hasPreview));
    }

    protected void setBitmap(Bitmap bitmap) {
        if (bitmap != null) {
            hasPreview = true;
        }
        imageView.setImageBitmap(bitmap);
    }

    public class PhotoSamplingTask extends WeakObjectTask<PhotoViewerActivity> {

        private Bitmap bitmap;
        private boolean hasPreview;

        public PhotoSamplingTask(PhotoViewerActivity object, boolean hasPreview) {
            super(object);
            this.hasPreview = hasPreview;
        }

        @Override
        public void executeBackground() throws Throwable {
            PhotoViewerActivity activity = getWeakObject();
            if (activity != null) {
                bitmap = BitmapHelper.decodeSampledBitmapFromUri(activity, uri, 1024, 1024);
                if (bitmap == null && !hasPreview) {
                    throw new NullPointerException();
                }
            }
        }

        @Override
        public void onSuccessMain() {
            PhotoViewerActivity activity = getWeakObject();
            if (activity != null && bitmap != null) {
                activity.setBitmap(bitmap);
            }
        }

        @Override
        public void onFailMain() {
            photoViewFailedView.setVisibility(View.VISIBLE);
            imageView.setVisibility(View.GONE);
        }
    }
}
