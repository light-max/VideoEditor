package com.lifengqiang.videoeditor.ui.mediatrackeditor.video;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lifengqiang.videoeditor.R;

public class MultiSectionVideoPlayerControllerView extends FrameLayout {
    private final MyHorizontalScrollView scrollView;
    private Callback callback = null;

    public MultiSectionVideoPlayerControllerView(@NonNull Context context) {
        this(context, null);
    }

    public MultiSectionVideoPlayerControllerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        View.inflate(context, R.layout.view_multi_section_video_player_controller, this);
        scrollView = findViewById(R.id.scroll);
        scrollView.setOnScrollChangedListener(new MyHorizontalScrollView.OnScrollChangedListener() {
            private long triggerTime = System.currentTimeMillis();

            @Override
            public void onScrollStateChanged(MyHorizontalScrollView view, boolean running) {
                if (callback != null) {
                    if (!running) {
//                        long intervalTime = System.currentTimeMillis() - triggerTime;
//                        if (200 - intervalTime > 0) {
//                            postDelayed(() -> {
                                performSeek(view.getScrollOffsetX(), view.getImageWidth());
                                callback.onFrameByFrameStateChanged(false);
//                            }, 300 - intervalTime);
//                        }
                    } else {
                        callback.onFrameByFrameStateChanged(true);
                    }
                }
            }

            @Override
            public void onScrollChanged(MyHorizontalScrollView view, boolean isUser, int dx) {
                if (System.currentTimeMillis() - triggerTime >= 100) {
                    if (callback != null && isUser) {
                        performSeek(dx, view.getImageWidth());
                    }
                    triggerTime = System.currentTimeMillis();
                }
            }

            private void performSeek(int dx, int positionWidth) {
                dx = (dx == 0) ? 0 : (dx - 1);
                int position = dx / positionWidth;
                float ratio = (float) (dx % positionWidth) / (float) positionWidth;
                callback.onFrameByFrameSeek(position, ratio);
            }
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    public void addVideoSection(VideoSectionData data) {
        scrollView.addVideoSection(data);
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public void setProgress(float ratio) {
        scrollView.scrollOffsetX(ratio);
    }

    public interface Callback {
        void onFrameByFrameStateChanged(boolean flag);

        void onFrameByFrameSeek(int position, float ratio);
    }
}
