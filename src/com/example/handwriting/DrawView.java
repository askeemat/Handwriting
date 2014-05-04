package com.example.handwriting;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public final class DrawView extends View {
	private static final String TAG = "DrawView";
	private static final int PEN_MARGIN = 4;
	private static final int HANDLER_DURATION = 10;
	
	private Paint mPen;
	private Path mPath;
	private RectF mUpdateRect;
	private PointF mLastPoint;
	private Handler mHandler;
	private Bitmap mBitmap;
	private Canvas mCanvas;
	private boolean mHaltHandler;

	public DrawView(Context context) {
		super(context);
		init(context);
	}
	
	public DrawView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}
	
	public DrawView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context);
	}
	
	@Override
	public void onDraw(Canvas canvas) {
		synchronized(mCanvas) {
			canvas.drawBitmap(mBitmap, 0.0f, 0.0f, null);
		}
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		Log.i(TAG, "onSizeChanged");
		mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
		mCanvas = new Canvas(mBitmap);
		mCanvas.drawColor(Color.WHITE);
	}
	
	private void init(Context context) {
		mPen = new Paint();
		mPen.setColor(Color.BLACK);
		mPen.setStrokeWidth(2.0f);
		mPen.setStyle(Paint.Style.STROKE);
		
		mHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				DrawView.this.doInvalidate();
			}
		};
		
		this.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return DrawView.this.doTouchEvent(event);
			}
		});
		
		mPath = new Path();
		mUpdateRect = new RectF();
		mLastPoint = new PointF();
		mHaltHandler = false;
	}
	
	
	private void calcUpdateRegion(float x, float y, boolean firstPoint) {
		if (firstPoint == true) {
			mUpdateRect.left   = x - PEN_MARGIN;
			mUpdateRect.top    = y - PEN_MARGIN;
			mUpdateRect.right  = x + PEN_MARGIN;
			mUpdateRect.bottom = y + PEN_MARGIN;
			
			mPath.reset();
    		mPath.moveTo(x, y);
		}
		else {
			if ((x - PEN_MARGIN) < mUpdateRect.left) {
				mUpdateRect.left = x - PEN_MARGIN;
			}
			else if (mUpdateRect.right < (x + PEN_MARGIN)) {
				mUpdateRect.right = x + PEN_MARGIN;
			}
			
			if ((y - PEN_MARGIN) < mUpdateRect.top) {
				mUpdateRect.top = y - PEN_MARGIN;
			}
			else if (mUpdateRect.bottom < (y + PEN_MARGIN)) {
				mUpdateRect.bottom = y + PEN_MARGIN;
			}
			
			mPath.lineTo(x, y);
		}
	}
	
	private boolean doTouchEvent(MotionEvent event) {
		synchronized(mUpdateRect) {
			int action = event.getAction();
			switch (action) {
		    case MotionEvent.ACTION_DOWN:
		    	//Log.i(mTag, "action down");
		    	calcUpdateRegion(event.getX(), event.getY(), true);
		    	mHaltHandler = false;
		    	mHandler.sendMessageDelayed(Message.obtain(), HANDLER_DURATION);
		        break;
		    case MotionEvent.ACTION_MOVE:
		    case MotionEvent.ACTION_UP:
		    	//Log.i(mTag, "action move/up");
		    	if (mUpdateRect.left == 0 && mUpdateRect.top == 0 &&
		    			mUpdateRect.right == 0 && mUpdateRect.bottom == 0) {
		    		calcUpdateRegion(mLastPoint.x, mLastPoint.y, true);
		    	}
		    	calcUpdateRegion(event.getX(), event.getY(), false);
		    	if (action == MotionEvent.ACTION_UP) {
		    		mHaltHandler = true;
		    	}
		    	break;
		    default:
		    }
			mLastPoint.x = event.getX();
			mLastPoint.y = event.getY();
		}
	    return true;
	}

	
	private void doInvalidate() {
		int left, top, right, bottom;
		synchronized (mUpdateRect) {
			left = (int)mUpdateRect.left;
			top = (int)mUpdateRect.top;
			right = (int)mUpdateRect.right;
			bottom = (int)mUpdateRect.bottom;
			synchronized(mCanvas) {
				mCanvas.drawPath(mPath, mPen);
			}
			mUpdateRect.left = 0;
			mUpdateRect.top = 0;
			mUpdateRect.right = 0;
			mUpdateRect.bottom = 0;
		}
		invalidate(left, top, right, bottom);
		if (mHaltHandler == false) {
			mHandler.sendMessageDelayed(Message.obtain(), HANDLER_DURATION);
		}
	}

}
