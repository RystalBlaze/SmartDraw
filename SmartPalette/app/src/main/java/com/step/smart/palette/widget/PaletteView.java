package com.step.smart.palette.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

import com.step.smart.palette.Constant.DrawMode;
import com.step.smart.palette.Constant.LineType;
import com.step.smart.palette.manager.FrameSizeManager;
import com.step.smart.palette.utils.BitmapUtils;
import com.step.smart.palette.utils.Preferences;

import java.io.File;

/**
 * Created by weflow on 2018/3/21.
 */

public class PaletteView extends FrameLayout {

    private static final String TAG = "PaletteFrameLayout";
    private PaletteSurfaceView mPaletteSurfaceView;
    private StrokeDrawView mStrokeDrawView;
    private FrameSizeManager mFrameManager;
    private DrawMode mCurrDrawMode = DrawMode.EDIT;
    private LineType mCurrStrokeType = LineType.DRAW;
    private PaletteInterface mPaletteInterface;
    private FrameLayout mFrame;

    public PaletteView(@NonNull Context context) {
        this(context, null);
    }

    public PaletteView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PaletteView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        if (context instanceof PaletteInterface) {
            mPaletteInterface = (PaletteInterface) context;
        }
        if (mStrokeDrawView == null) {
            mStrokeDrawView = new StrokeDrawView(context);
        }
        if (mPaletteSurfaceView == null) {
            mPaletteSurfaceView = new PaletteSurfaceView(context);
        }
        if (mFrame == null) {
            mFrame = new FrameLayout(context);
        }
        mPaletteSurfaceView.setSyncDrawInterface(mStrokeDrawView);
        this.mStrokeDrawView.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        this.mPaletteSurfaceView.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        this.mFrame.removeAllViews();
        this.mFrame.addView(mStrokeDrawView);
        this.mFrame.addView(mPaletteSurfaceView);
        this.addView(this.mFrame);
        this.mFrame.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        mFrameManager = new FrameSizeManager();
    }

    public void initDrawAreas() {
        if (this.getWidth() > 0 && this.getHeight() > 0) {
            label:
            {
                if (this.getWidth() <= this.getHeight()) {
                    break label;
                }
                Log.e(TAG, "initDrawAreas --> 3 w = " + getWidth() + "\nh = " + getHeight());
                mFrameManager.frameWidth = getWidth();
                mFrameManager.frameHeight = getHeight();
                initParams();
                return;
            }
        }
        this.postDelayed(new Runnable() {
            public void run() {
                PaletteView.this.initDrawAreas();
            }
        }, 100L);
    }

    private void initParams() {
        LayoutParams params = (LayoutParams) this.mFrame.getLayoutParams();
        mFrameManager.wholeWidth = (int)(getWidth() * 2);
        params.width = mFrameManager.wholeWidth;
        mFrameManager.wholeHeight = (int)(getHeight() * 2);
        params.height = mFrameManager.wholeHeight;
        this.mFrame.setLayoutParams(params);
        mFrameManager.posX = - (mFrameManager.wholeWidth - mFrameManager.frameWidth) / 2;
        mFrameManager.posY = - (mFrameManager.wholeHeight - mFrameManager.frameHeight) / 2;
        this.mFrame.setX(mFrameManager.posX);
        this.mFrame.setY(mFrameManager.posY);
        mFrameManager.calculate();
        Preferences.saveInt("screen_width", mFrameManager.wholeWidth);
        Preferences.saveInt("screen_height", mFrameManager.wholeHeight);
    }

    public void clear() {
        mStrokeDrawView.clear();
    }

    public void undo() {
        mStrokeDrawView.undo();
    }

    public void redo() {
        mStrokeDrawView.redo();
    }

    public boolean isEmpty() {
        return mStrokeDrawView.isEmpty();
    }

    public void setBackgroundColor(int color) {
        this.mFrame.setBackgroundColor(color);
    }

    public void screenShot(boolean wholeScreen) {
        long start = System.currentTimeMillis();
        String path = getContext().getExternalCacheDir().getAbsolutePath() + File.separator + System.currentTimeMillis() + ".png";
        if (wholeScreen) {
            Bitmap bmp = Bitmap.createBitmap(this.mFrame.getWidth(), this.mFrame.getHeight(), Bitmap.Config.ARGB_4444);
            Canvas canvas = new Canvas(bmp);
            this.mFrame.draw(canvas);
            byte[] data = BitmapUtils.bitmap2Bytes(bmp, 100);
            if (data.length > 0) {
                boolean result = BitmapUtils.saveByteData(data, path);
            }
        } else {
            Bitmap bmp = Bitmap.createBitmap(this.mFrame.getWidth(), this.mFrame.getHeight(), Bitmap.Config.ARGB_4444);
            Canvas canvas = new Canvas(bmp);
            this.mFrame.draw(canvas);
            Bitmap currBmp = Bitmap.createBitmap(this.mFrameManager.frameWidth, this.mFrameManager.frameHeight, Bitmap.Config.ARGB_4444);
            Canvas currCanvas = new Canvas(currBmp);
            currCanvas.drawBitmap(bmp,
                    new Rect((int)(Math.abs(this.mFrame.getX())), (int)Math.abs(this.mFrame.getY()), (int)(Math.abs(this.mFrame.getX()) + this.mFrameManager.frameWidth), (int)(Math.abs(this.mFrame.getX()) + this.mFrameManager.frameHeight)),
                    new Rect(0, 0, this.mFrameManager.frameWidth, this.mFrameManager.frameHeight),
                    null);
            byte[] data = BitmapUtils.bitmap2Bytes(currBmp, 100);
            if (data.length > 0) {
                boolean result = BitmapUtils.saveByteData(data, path);
            }
        }
        Log.e("PaletteView", "save finish --> " + (System.currentTimeMillis() - start));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mPaletteInterface.getCurrentMode() == DrawMode.MOVE) {
            onTouchHandle(event);
            return true;
        }
        return super.onTouchEvent(event);

    }

    private float mDownLastX;
    private float mDownLastY;
    private void onTouchHandle(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        int tt = (int) ((double) (this.mFrame.getY() + (y - this.mDownLastY)) + 0.5D);
        int tl = (int) ((double) (this.mFrame.getX() + (x - this.mDownLastX)) + 0.5D);
        int gh = this.mFrameManager.windowTop * 2;
        int gw = this.mFrameManager.windowLeft * 2;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                this.mFrame.clearAnimation();
                break;
            case MotionEvent.ACTION_UP:
                if (this.mFrameManager.windowTop < 0) {
                    if (tt < gh) {
                        tt = gh;
                    } else if (tt > 0) {
                        tt = 0;
                    }
                } else if (this.mFrameManager.windowTop > 0) {
                    if (tt > gh) {
                        tt = gh;
                    } else if (tt < 0) {
                        tt = 0;
                    }
                } else {
                    tt = 0;
                }

                if (this.mFrameManager.windowLeft < 0) {
                    if (tl < gw) {
                        tl = gw;
                    } else if (tl > 0) {
                        tl = 0;
                    }
                } else if (this.mFrameManager.windowLeft > 0) {
                    if (tl > gw) {
                        tl = gw;
                    } else if (tl < 0) {
                        tl = 0;
                    }
                } else {
                    tl = 0;
                }

                this.mFrame.animate().setDuration(200L).setInterpolator(new DecelerateInterpolator()).y((float) tt).x((float) tl);
                break;
            case MotionEvent.ACTION_MOVE:
                this.mFrame.setY((float) tt);
                this.mFrame.setX((float) tl);
        }

        this.mDownLastX = x;
        this.mDownLastY = y;

    }

    public interface PaletteInterface{

        DrawMode getCurrentMode();

        LineType getCurrStrokeType();

        float getStrokeWidth();

        int getStrokeColor();

        int getStrokeAlpha();//0 - 255;

        void onUndoRedoCountChanged(int redo, int undo);

        boolean isHighLighter();
    }
}
