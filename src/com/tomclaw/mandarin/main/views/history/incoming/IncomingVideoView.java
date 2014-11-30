package com.tomclaw.mandarin.main.views.history.incoming;

import android.content.Context;
import android.util.AttributeSet;
import com.tomclaw.mandarin.R;

/**
 * Created by Solkin on 30.11.2014.
 */
public class IncomingVideoView extends IncomingPreviewView {

    public IncomingVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected int getThumbnailPlaceholder() {
        return R.drawable.video_placeholder;
    }
}
