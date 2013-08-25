package net.organizedConstructors.LastExileClock;

import android.content.Context;
import android.graphics.*;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.util.DisplayMetrics;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import java.util.Calendar;

public class ClockLiveWallpaperService  extends WallpaperService {
    private final Handler mHandler = new Handler();

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public Engine onCreateEngine() {
        return new ClockEngine();
    }

    class ClockEngine extends Engine {
        private static final int LOW_DPI_STATUS_BAR_HEIGHT = 19;
        private static final int MEDIUM_DPI_STATUS_BAR_HEIGHT = 25;
        private static final int HIGH_DPI_STATUS_BAR_HEIGHT = 38;
        private int statusBarHeight;

        private final Paint mPaint = new Paint();

        private Bitmap dial, hoursBmp, minutesBmp, secondsBmp;
        private Integer dialW, dialH, armW, armH;
        private long millisecsInDay = 24 * 60 * 60 * 1000;
        private Float degreeForOneHour = 360f / 12;
        private Float degreeForOneMinuteOrSecond = 360f / 60;
        private final Runnable mDrawClock = new Runnable() {
            public void run() {
                drawFrame();
            }
        };
        private boolean mVisible;

        ClockEngine() {
            // Create a Paint to draw the lines for our cube
            final Paint paint = mPaint;
            paint.setColor(0xffffffff);
            paint.setAntiAlias(true);
            paint.setFilterBitmap(true);
            paint.setDither(true);
            paint.setStrokeWidth(2);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStyle(Paint.Style.STROKE);
        }

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            dial = BitmapFactory.decodeResource(getResources(), R.drawable.dial);
            hoursBmp = BitmapFactory.decodeResource(getResources(), R.drawable.hand_hours);
            minutesBmp = BitmapFactory.decodeResource(getResources(), R.drawable.hand_minutes);
            secondsBmp = BitmapFactory.decodeResource(getResources(), R.drawable.hand_seconds);
            dialW = dial.getWidth();
            dialH = dial.getHeight();
            armW = hoursBmp.getWidth();
            armH = hoursBmp.getHeight();

            DisplayMetrics displayMetrics = new DisplayMetrics();
            ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(displayMetrics);

            switch (displayMetrics.densityDpi) {
                case DisplayMetrics.DENSITY_HIGH:
                    statusBarHeight = HIGH_DPI_STATUS_BAR_HEIGHT;
                    break;
                case DisplayMetrics.DENSITY_MEDIUM:
                    statusBarHeight = MEDIUM_DPI_STATUS_BAR_HEIGHT;
                    break;
                case DisplayMetrics.DENSITY_LOW:
                    statusBarHeight = LOW_DPI_STATUS_BAR_HEIGHT;
                    break;
                default:
                    statusBarHeight = MEDIUM_DPI_STATUS_BAR_HEIGHT;
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            mHandler.removeCallbacks(mDrawClock);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            mVisible = visible;
            if (visible) {
                drawFrame();
            } else {
                mHandler.removeCallbacks(mDrawClock);
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            drawFrame();
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            mVisible = false;
            mHandler.removeCallbacks(mDrawClock);
        }

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset,
                                     float xStep, float yStep, int xPixels, int yPixels) {
            drawFrame();
        }

        /*
         * Draw one frame of the animation. This method gets called repeatedly
         * by posting a delayed Runnable. You can do any drawing you want in
         * here.
         */
        void drawFrame() {
            final SurfaceHolder holder = getSurfaceHolder();

            Canvas c = null;
            try {
                c = holder.lockCanvas();
                if (c != null) {
                    drawClock(c);
                }
            } finally {
                if (c != null) holder.unlockCanvasAndPost(c);
            }

            // Reschedule the next redraw
            mHandler.removeCallbacks(mDrawClock);
            if (mVisible) {
                mHandler.postDelayed(mDrawClock, 1000 / 60);
            }
        }

        void drawClock(Canvas c) {
            c.save();
            c.translate(0, statusBarHeight);
            Calendar rightNow = Calendar.getInstance();
            // offset to add since we're not UTC
            long offset = rightNow.get(Calendar.ZONE_OFFSET) +
                    rightNow.get(Calendar.DST_OFFSET);

            long sinceMidnight = (rightNow.getTimeInMillis() + offset) %
                    (millisecsInDay);

            Float secs = sinceMidnight / 1000f % 60;
            Float minutes = sinceMidnight / (1000f * 60) % 60;
            Float hours = sinceMidnight / (1000f * 60 * 60);

            Float secondsDegree = degreeForOneMinuteOrSecond * secs;
            Float minutesDegree = degreeForOneMinuteOrSecond * minutes;
            Float hoursDegree = degreeForOneHour * hours;

            c.drawColor(0xff000000);
            c.drawBitmap(dial, 0, 0, mPaint);

            // Origin at center
            int MAGIC_SHIFT_MEH = -5;
            c.translate(dialW/2, dialH/4 + MAGIC_SHIFT_MEH);
            c.drawBitmap(secondsBmp, getRotator(secondsDegree, armW, armH), mPaint);
            c.drawBitmap(minutesBmp, getRotator(minutesDegree, armW, armH), mPaint);
            c.drawBitmap(hoursBmp, getRotator(hoursDegree, armW, armH), mPaint);

            c.restore();
        }

        Matrix getRotator(Float degrees, int width, int height) {
            Matrix rotator = new Matrix();
            int x = width/2;
            int y = height/2;
            rotator.postRotate(degrees, x, y);
            rotator.postTranslate(-width/2, -height/2);
            return rotator;
        }
    }
}
