package com.multioverlays;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

class ClearGLSurfaceView extends GLSurfaceView {
    public ClearGLSurfaceView(Context context) {
        super(context);
        mRenderer = new ClearRenderer();
        setRenderer(mRenderer);
    }

//    public boolean onTouchEvent(final MotionEvent event) {
//        queueEvent(new Runnable(){
//            public void run() {
//                mRenderer.setColor(event.getX() / getWidth(),
//                        event.getY() / getHeight(), 1.0f);
//            }});
//            return true;
//        }

    public void resetFrameCounter() {
    	mFrameCounter = 0;
    }

    public int getFrameCounter() {
        return mFrameCounter;
    }
    
    public void setAlphaUp() {
    	mChangingAlpha = true;
    	mCount2Frames = 0;
    	if(mAlpha < 1)
    		mAlpha += 0.1;
    }
    
    public void setAlphaDown() {
    	mChangingAlpha = true;
    	mCount2Frames = 0;
    	if(mAlpha > 0.1)
    		mAlpha -= 0.1;
    }
    
    public void toggleClearEveryFrame() {
    	if(mClearEveryFrame == false)
    		mClearEveryFrame = true;
    	else
    		mClearEveryFrame = false;
    }
   
    public double getFPS()
    {
    	return 1/(mTimeFPS/1000);
    }

    ClearRenderer mRenderer;
    private float mAlpha = 0.3f;
    private int mFrameCounter = 0;
    private int mCount2Frames = 0;
    private boolean mChangingAlpha = false;
    private boolean mClearEveryFrame = false;
    public double mTimeFPS;
}

class ClearRenderer implements GLSurfaceView.Renderer {
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // Do nothing special.
    }

    public void onSurfaceChanged(GL10 gl, int w, int h) {
        gl.glViewport(0, 0, w, h);
    }

    public void onDrawFrame(GL10 gl) {
        gl.glClearColor(mRed, mGreen, mBlue, 1.0f);
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
    }

    public void setColor(float r, float g, float b) {
        mRed = r;
        mGreen = g;
        mBlue = b;
    }

    private float mRed;
    private float mGreen;
    private float mBlue;
}