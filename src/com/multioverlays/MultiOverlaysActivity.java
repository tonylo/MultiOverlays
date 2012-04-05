package com.multioverlays;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import com.multioverlays.R;
import com.multioverlays.SurfaceTextureView;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


public class MultiOverlaysActivity extends Activity 
{
    private static final String TAG = "Multioverlays demo";
    private short MAX_VIDEOS = 8;
    private int LAYER_WIDTH = 1152;
    private int LAYER_HEIGHT = 768;
    private int[] mFrameViewIdArray;

    public void onCreate(Bundle icicle) {
    	   	
        super.onCreate(icicle);
        
        mFrameViewIdArray = new int[MAX_VIDEOS];
        mFrameViewIdArray[0] = R.id.frameLayout1;
        mFrameViewIdArray[1] = R.id.frameLayout2;
        mFrameViewIdArray[2] = R.id.frameLayout3;
        mFrameViewIdArray[3] = R.id.frameLayout4;
        mFrameViewIdArray[4] = R.id.frameLayout5;
        mFrameViewIdArray[5] = R.id.frameLayout6;
        mFrameViewIdArray[6] = R.id.frameLayout7;
        mFrameViewIdArray[7] = R.id.frameLayout8;

        Window window = getWindow();
        window.setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        Display display = getWindowManager().getDefaultDisplay(); 
        int width = display.getWidth();
        int height = display.getHeight();
        
        if(width == 1280 && height == 752)
        	setContentView(R.layout.main);
        else if(width == 864 && height == 480)
        	setContentView(R.layout.main_blaze);
        else
        {
        	// Hack but better than nothing
        	AlertDialog.Builder builder_res = new AlertDialog.Builder(this);
            builder_res.setTitle("Unhandled device resolution");
        	AlertDialog alert_res = builder_res.create();
            alert_res.show();
            if (alert_res.isShowing())
            	return;
        }
        
        
        final CharSequence[] items_res = {"640x480","1024x768", "1152x768", "1280x720", "1920x1080"};

        AlertDialog.Builder builder_res = new AlertDialog.Builder(this);
        builder_res.setTitle("Choose layers resolution");
        builder_res.setSingleChoiceItems(items_res, -1, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
            	if(items_res[item].toString().compareTo("640x480") == 0)
            	{
            		LAYER_WIDTH = 640;
            		LAYER_HEIGHT = 480;
            	} 
            	else if(items_res[item].toString().compareTo("1024x768") == 0)
            	{
            		LAYER_WIDTH = 1024;
            		LAYER_HEIGHT = 768;
            	}
            	else if(items_res[item].toString().compareTo("1152x768") == 0)
            	{
            		LAYER_WIDTH = 1152;
            		LAYER_HEIGHT = 768;
            	}
            	else if(items_res[item].toString().compareTo("1280x720") == 0)
            	{
            		LAYER_WIDTH = 1280;
            		LAYER_HEIGHT = 720;
            	}
            	else if(items_res[item].toString().compareTo("1920x1080") == 0)
            	{
            		LAYER_WIDTH = 1920;
            		LAYER_HEIGHT = 1080;
            	}
            	
            	RelativeLayout.LayoutParams rl1 = new RelativeLayout.LayoutParams(LAYER_WIDTH,LAYER_HEIGHT);
            	findViewById(R.id.frameLayout1).setLayoutParams(rl1);
            	
            	for (int flId = 1; flId < MAX_VIDEOS; flId++) {
            		int idprev = getFrameViewId(flId-1);
            		int id = getFrameViewId(flId);
            		RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(LAYER_WIDTH,LAYER_HEIGHT);
                	p.addRule(RelativeLayout.ALIGN_LEFT , findViewById(idprev).getId());
                	p.addRule(RelativeLayout.ALIGN_TOP , findViewById(idprev).getId());
                	p.setMargins(10, 10, 0, 0);
                	findViewById(id).setLayoutParams(p);
            	}

            	initSurfaceTextures();
    	    	((Button) findViewById(R.id.gobutton)).setVisibility(4);
    	    	((TextView) findViewById(R.id.textFPSAverage)).setVisibility(0);
    	    	((TextView) findViewById(R.id.textFPS1)).setVisibility(0);   
    	    	
    	    	((Button) findViewById(R.id.alphaUp)).bringToFront();
    	        ((Button) findViewById(R.id.alphaDown)).bringToFront();
                dialog.dismiss();
            }
        });
        AlertDialog alert_res = builder_res.create();
        alert_res.show();
                   
        final CharSequence[] items = {"1", "2", "3", "4", "5", "6", "7", "8"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose # of layers to be displayed");
        builder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
            	MAX_VIDEOS = (short) Integer.parseInt((String) items[item]);
                dialog.dismiss();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
        
        // setting TI logo
        ImageView logo = (ImageView) findViewById(R.id.imageViewLogo);
        logo.setImageResource(R.drawable.ti_logo);
        
        ((Button) findViewById(R.id.alphaUp)).setOnClickListener(mAlphaUpListener);
        ((Button) findViewById(R.id.alphaDown)).setOnClickListener(mAlphaDownListener);
        ((Button) findViewById(R.id.clearEveryFrame)).setOnClickListener(mClearEveryFrame);
        ((Button) findViewById(R.id.alignFrames)).setOnClickListener(mToggleOffsets);
    }   
    
    public int getMaxFrameViewIdx() { return MAX_VIDEOS; }
    public int getFrameViewId(int idx) { return mFrameViewIdArray[idx]; }

	class OffsetsClickListener implements OnClickListener {
		private MultiOverlaysActivity mActivity;
		private boolean mOffsetsOn = true;

		public OffsetsClickListener(MultiOverlaysActivity aActivity) {
			mActivity = aActivity;
		}
		@Override
    	public void onClick(View v) {
    		int maxid = mActivity.getMaxFrameViewIdx();
    		mOffsetsOn = !mOffsetsOn;
    		for (int i = 0; i < maxid; i++) {
    			int fvId = mActivity.getFrameViewId(i);
    			FrameLayout fl = (FrameLayout) mActivity.findViewById(fvId);
    			RelativeLayout.LayoutParams p = (RelativeLayout.LayoutParams)fl.getLayoutParams();
    			if (mOffsetsOn) {
    				p.setMargins(10, 10, 0, 0);
    			} else {
    				p.setMargins(0, 0, 0, 0);
    			}
    			fl.setLayoutParams(p);
    		}
    	}
    };
    
    OnClickListener mToggleOffsets = new OffsetsClickListener(this);

    OnClickListener mClearEveryFrame = new OnClickListener() {
        @Override
        public void onClick(View v) {
        	
        	if (((Button) findViewById(R.id.clearEveryFrame)).getText().toString().compareTo("glClear() on") == 0)
        		((Button) findViewById(R.id.clearEveryFrame)).setText("glClear() off");
        	else
        		((Button) findViewById(R.id.clearEveryFrame)).setText("glClear() on");
        	
        	for (int flId = 0 ; flId < MAX_VIDEOS; flId++ ) {
        		FrameLayout fl = (FrameLayout)findViewById(getFrameViewId(flId));
        		((SurfaceTextureView)(fl).getChildAt(0)).toggleClearEveryFrame();
        	}
        }
    };
    
    OnClickListener mAlphaUpListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
        	for (int flId = 0 ; flId < MAX_VIDEOS; flId++ ) {
        		FrameLayout fl = (FrameLayout)findViewById(getFrameViewId(flId));
        		((SurfaceTextureView)(fl).getChildAt(0)).setAlphaUp();
        	}
        }
    };
    
    OnClickListener mAlphaDownListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
        	for (int flId = 0 ; flId < MAX_VIDEOS; flId++ ) {
        		FrameLayout fl = (FrameLayout)findViewById(getFrameViewId(flId));
        		((SurfaceTextureView)(fl).getChildAt(0)).setAlphaDown();
        	}        	
        }
    };
    
    private static double timeFPS = 0;
    private static double prev_time = 0;
    private static double fps = 0;

//    private static class UpdaterAVG1 extends TimerTask {
//	    private final TextView tvAVG;
//	    private final TextView tv1;
//	    private final SurfaceTextureView surfaceFPS1;
//	
//	    public UpdaterAVG1(TextView tv1, TextView tvAVG, SurfaceTextureView surfaceTextureView1) {
//	        this.tvAVG = tvAVG;
//	        this.tv1 = tv1;
//	        this.surfaceFPS1 = surfaceTextureView1;
//	    }
//	
//	    @Override
//	    public void run() {
//	    	tvAVG.post(new Runnable() {
//	
//	            public void run() {
//	            	timeFPS = System.currentTimeMillis() - prev_time;
//	            	prev_time = System.currentTimeMillis();
//	            	if(timeFPS > 0)
//	            		fps = 1/(timeFPS/1000);
//	            	Log.d("time", "time = " + timeFPS);
//	            	int averageFPS = surfaceFPS1.getFrameCounter();
//	            	tv1.setText("FPS = " + averageFPS);
//	            	tvAVG.setText("AVG FPS = " + averageFPS);
//	                surfaceFPS1.resetFrameCounter();
//	            }
//	        });
//	    }
//	}
//    
//    private static class UpdaterAVG2 extends TimerTask {
//        private final TextView tvAVG,tv1,tv2;
//        private final SurfaceTextureView surfaceFPS1,surfaceFPS2;
//
//        public UpdaterAVG2(TextView tv1,TextView tv2, TextView tvAVG, SurfaceTextureView surfaceTextureView1, SurfaceTextureView surfaceTextureView2) {
//           this.tvAVG = tvAVG;
//           this.tv1 = tv1;
//           this.tv2 = tv2;
//           this.surfaceFPS1 = surfaceTextureView1;
//           this.surfaceFPS2 = surfaceTextureView2;
//        }
//
//        @Override
//        public void run() {
//        	tvAVG.post(new Runnable() {
//
//                public void run() {
//	            	timeFPS = System.currentTimeMillis() - prev_time;
//	            	prev_time = System.currentTimeMillis();
//	            	if(timeFPS > 0)
//	            		fps = 1/(timeFPS/1000);
//	            	Log.d("time", "time = " + timeFPS);
//                	int FPS1 = surfaceFPS1.getFrameCounter();
//                	int FPS2 = surfaceFPS2.getFrameCounter();
//                	int averageFPS = ( FPS1 + FPS2 )/2;
//                	tv1.setText("FPS = " + FPS1);
//                	tv2.setText("FPS = " + FPS2);
//                	tvAVG.setText("AVG FPS = " + (int)averageFPS);
//                	surfaceFPS1.resetFrameCounter();
//                	surfaceFPS2.resetFrameCounter();
//                }
//            });
//        }
//    }
//
//    private static class UpdaterAVG3 extends TimerTask {
//        private final TextView tvAVG,tv1,tv2,tv3;
//        private final SurfaceTextureView surfaceFPS1,surfaceFPS2,surfaceFPS3;
//
//        public UpdaterAVG3(TextView tv1,TextView tv2,TextView tv3,TextView tvAVG, SurfaceTextureView surfaceTextureView1, SurfaceTextureView surfaceTextureView2, SurfaceTextureView surfaceTextureView3) {
//           this.tvAVG = tvAVG;
//           this.tv1 = tv1;
//           this.tv2 = tv2;
//           this.tv3 = tv3;
//           this.surfaceFPS1 = surfaceTextureView1;
//           this.surfaceFPS2 = surfaceTextureView2;
//           this.surfaceFPS3 = surfaceTextureView3;
//        }
//
//        @Override
//        public void run() {
//        	tvAVG.post(new Runnable() {
//
//                public void run() {
//	            	timeFPS = System.currentTimeMillis() - prev_time;
//	            	prev_time = System.currentTimeMillis();
//	            	if(timeFPS > 0)
//	            		fps = 1/(timeFPS/1000);
//	            	Log.d("time", "time = " + timeFPS);
//                	int FPS1 = surfaceFPS1.getFrameCounter();
//                	int FPS2 = surfaceFPS2.getFrameCounter();
//                	int FPS3 = surfaceFPS3.getFrameCounter();
//                	int averageFPS = ( FPS1 + FPS2 + FPS3)/3;
//                	tv1.setText("FPS = " + FPS1);
//                	tv2.setText("FPS = " + FPS2);
//                	tv3.setText("FPS = " + FPS3);
//                	tvAVG.setText("AVG FPS = " + (int)averageFPS);
//                	surfaceFPS1.resetFrameCounter();
//                	surfaceFPS2.resetFrameCounter();
//                	surfaceFPS3.resetFrameCounter();
//                }
//            });
//        }
//    }
//    
//    private static class UpdaterAVG4 extends TimerTask {
//        private final TextView tvAVG,tv1,tv2,tv3,tv4;
//        private final SurfaceTextureView surfaceFPS1,surfaceFPS2,surfaceFPS3,surfaceFPS4;
//
//        public UpdaterAVG4(TextView tv1,TextView tv2,TextView tv3,TextView tv4, TextView tvAVG, SurfaceTextureView surfaceTextureView1, SurfaceTextureView surfaceTextureView2, SurfaceTextureView surfaceTextureView3, SurfaceTextureView surfaceTextureView4) {
//           this.tvAVG = tvAVG;
//           this.tv1 = tv1;
//           this.tv2 = tv2;
//           this.tv3 = tv3;
//           this.tv4 = tv4;
//           this.surfaceFPS1 = surfaceTextureView1;
//           this.surfaceFPS2 = surfaceTextureView2;
//           this.surfaceFPS3 = surfaceTextureView3;
//           this.surfaceFPS4 = surfaceTextureView4;
//        }
//
//        @Override
//        public void run() {
//        	tvAVG.post(new Runnable() {
//
//                public void run() {
//	            	timeFPS = System.currentTimeMillis() - prev_time;
//	            	prev_time = System.currentTimeMillis();
//	            	if(timeFPS > 0)
//	            		fps = 1/(timeFPS/1000);
//	            	Log.d("time", "time = " + timeFPS);
//                	int FPS1 = surfaceFPS1.getFrameCounter();
//                	int FPS2 = surfaceFPS2.getFrameCounter();
//                	int FPS3 = surfaceFPS3.getFrameCounter();
//                	int FPS4 = surfaceFPS4.getFrameCounter();
//                	int averageFPS = ( FPS1 + FPS2 + FPS3 + FPS4)/4;
//                	tv1.setText("FPS = " + FPS1);
//                	tv2.setText("FPS = " + FPS2);
//                	tv3.setText("FPS = " + FPS3);
//                	tv4.setText("FPS = " + FPS4);
//                	tvAVG.setText("AVG FPS = " + (int)averageFPS);
//                	surfaceFPS1.resetFrameCounter();
//                	surfaceFPS2.resetFrameCounter();
//                	surfaceFPS3.resetFrameCounter();
//                	surfaceFPS4.resetFrameCounter();
//                }
//            });
//        }
//    }
//    
//    private static class UpdaterAVG5 extends TimerTask {
//        private final TextView tvAVG,tv1,tv2,tv3,tv4,tv5;
//        private final SurfaceTextureView surfaceFPS1,surfaceFPS2,surfaceFPS3,surfaceFPS4,surfaceFPS5;
//
//        public UpdaterAVG5(TextView tv1,TextView tv2,TextView tv3,TextView tv4,TextView tv5, TextView tvAVG, SurfaceTextureView surfaceTextureView1, SurfaceTextureView surfaceTextureView2, SurfaceTextureView surfaceTextureView3, SurfaceTextureView surfaceTextureView4, SurfaceTextureView surfaceTextureView5) {
//           this.tvAVG = tvAVG;
//           this.tv1 = tv1;
//           this.tv2 = tv2;
//           this.tv3 = tv3;
//           this.tv4 = tv4;
//           this.tv5 = tv5;
//           this.surfaceFPS1 = surfaceTextureView1;
//           this.surfaceFPS2 = surfaceTextureView2;
//           this.surfaceFPS3 = surfaceTextureView3;
//           this.surfaceFPS4 = surfaceTextureView4;
//           this.surfaceFPS5 = surfaceTextureView5;
//        }
//
//        @Override
//        public void run() {
//        	tvAVG.post(new Runnable() {
//
//                public void run() {
//	            	timeFPS = System.currentTimeMillis() - prev_time;
//	            	prev_time = System.currentTimeMillis();
//	            	if(timeFPS > 0)
//	            		fps = 1/(timeFPS/1000);
//	            	Log.d("time", "time = " + timeFPS);
//                	int FPS1 = surfaceFPS1.getFrameCounter();
//                	int FPS2 = surfaceFPS2.getFrameCounter();
//                	int FPS3 = surfaceFPS3.getFrameCounter();
//                	int FPS4 = surfaceFPS4.getFrameCounter();
//                	int FPS5 = surfaceFPS5.getFrameCounter();
//                	int averageFPS = ( FPS1 + FPS2 + FPS3 + FPS4 + FPS5 )/5;
//                	tv1.setText("FPS = " + FPS1);
//                	tv2.setText("FPS = " + FPS2);
//                	tv3.setText("FPS = " + FPS3);
//                	tv4.setText("FPS = " + FPS4);
//                	tv5.setText("FPS = " + FPS5);
//                	tvAVG.setText("AVG FPS = " + (int)averageFPS);
//                	surfaceFPS1.resetFrameCounter();
//                	surfaceFPS2.resetFrameCounter();
//                	surfaceFPS3.resetFrameCounter();
//                	surfaceFPS4.resetFrameCounter();
//                	surfaceFPS5.resetFrameCounter();
//                }
//            });
//        }
//    }
//    
//    private static class UpdaterAVG6 extends TimerTask {
//        private final TextView tvAVG,tv1,tv2,tv3,tv4,tv5,tv6;
//        private final SurfaceTextureView surfaceFPS1,surfaceFPS2,surfaceFPS3,surfaceFPS4,surfaceFPS5,surfaceFPS6;
//
//        public UpdaterAVG6(TextView tv1,TextView tv2,TextView tv3,TextView tv4,TextView tv5,TextView tv6, TextView tvAVG, SurfaceTextureView surfaceTextureView1, SurfaceTextureView surfaceTextureView2, SurfaceTextureView surfaceTextureView3, SurfaceTextureView surfaceTextureView4, SurfaceTextureView surfaceTextureView5, SurfaceTextureView surfaceTextureView6) {
//           this.tvAVG = tvAVG;
//           this.tv1 = tv1;
//           this.tv2 = tv2;
//           this.tv3 = tv3;
//           this.tv4 = tv4;
//           this.tv5 = tv5;
//           this.tv6 = tv6;
//           this.surfaceFPS1 = surfaceTextureView1;
//           this.surfaceFPS2 = surfaceTextureView2;
//           this.surfaceFPS3 = surfaceTextureView3;
//           this.surfaceFPS4 = surfaceTextureView4;
//           this.surfaceFPS5 = surfaceTextureView5;
//           this.surfaceFPS6 = surfaceTextureView6;
//        }
//
//        @Override
//        public void run() {
//        	tvAVG.post(new Runnable() {
//
//                public void run() {
//	            	timeFPS = System.currentTimeMillis() - prev_time;
//	            	prev_time = System.currentTimeMillis();
//	            	if(timeFPS > 0)
//	            		fps = 1/(timeFPS/1000);
//	            	Log.d("time", "time = " + timeFPS);
//                	int FPS1 = surfaceFPS1.getFrameCounter();
//                	int FPS2 = surfaceFPS2.getFrameCounter();
//                	int FPS3 = surfaceFPS3.getFrameCounter();
//                	int FPS4 = surfaceFPS4.getFrameCounter();
//                	int FPS5 = surfaceFPS5.getFrameCounter();
//                	int FPS6 = surfaceFPS6.getFrameCounter();
//                	int averageFPS = ( FPS1 + FPS2 + FPS3 + FPS4 + FPS5 + FPS6)/6;
//                	tv1.setText("FPS = " + FPS1);
//                	tv2.setText("FPS = " + FPS2);
//                	tv3.setText("FPS = " + FPS3);
//                	tv4.setText("FPS = " + FPS4);
//                	tv5.setText("FPS = " + FPS5);
//                	tv6.setText("FPS = " + FPS6);
//                	tvAVG.setText("AVG FPS = " + (int)averageFPS);
//                	surfaceFPS1.resetFrameCounter();
//                	surfaceFPS2.resetFrameCounter();
//                	surfaceFPS3.resetFrameCounter();
//                	surfaceFPS4.resetFrameCounter();
//                	surfaceFPS5.resetFrameCounter();
//                	surfaceFPS6.resetFrameCounter();
//                }
//            });
//        }
//    }
//    
//    private static class UpdaterAVG7 extends TimerTask {
//        private final TextView tvAVG,tv1,tv2,tv3,tv4,tv5,tv6,tv7;
//        private final SurfaceTextureView surfaceFPS1,surfaceFPS2,surfaceFPS3,surfaceFPS4,surfaceFPS5,surfaceFPS6,surfaceFPS7;
//
//        public UpdaterAVG7(TextView tv1,TextView tv2,TextView tv3,TextView tv4,TextView tv5,TextView tv6,TextView tv7, TextView tvAVG, SurfaceTextureView surfaceTextureView1, SurfaceTextureView surfaceTextureView2, SurfaceTextureView surfaceTextureView3, SurfaceTextureView surfaceTextureView4, SurfaceTextureView surfaceTextureView5, SurfaceTextureView surfaceTextureView6, SurfaceTextureView surfaceTextureView7) {
//           this.tvAVG = tvAVG;
//           this.tv1 = tv1;
//           this.tv2 = tv2;
//           this.tv3 = tv3;
//           this.tv4 = tv4;
//           this.tv5 = tv5;
//           this.tv6 = tv6;
//           this.tv7 = tv7;
//           this.surfaceFPS1 = surfaceTextureView1;
//           this.surfaceFPS2 = surfaceTextureView2;
//           this.surfaceFPS3 = surfaceTextureView3;
//           this.surfaceFPS4 = surfaceTextureView4;
//           this.surfaceFPS5 = surfaceTextureView5;
//           this.surfaceFPS6 = surfaceTextureView6;
//           this.surfaceFPS7 = surfaceTextureView7;
//        }
//
//        @Override
//        public void run() {
//        	tvAVG.post(new Runnable() {
//
//                public void run() {
//	            	timeFPS = System.currentTimeMillis() - prev_time;
//	            	prev_time = System.currentTimeMillis();
//	            	if(timeFPS > 0)
//	            		fps = 1/(timeFPS/1000);
//	            	Log.d("time", "time = " + timeFPS);
//                	int FPS1 = surfaceFPS1.getFrameCounter();
//                	int FPS2 = surfaceFPS2.getFrameCounter();
//                	int FPS3 = surfaceFPS3.getFrameCounter();
//                	int FPS4 = surfaceFPS4.getFrameCounter();
//                	int FPS5 = surfaceFPS5.getFrameCounter();
//                	int FPS6 = surfaceFPS6.getFrameCounter();
//                	int FPS7 = surfaceFPS7.getFrameCounter();
//                	int averageFPS = ( FPS1 + FPS2 + FPS3 + FPS4 + FPS5 + FPS6 + FPS7)/7;
//                	tv1.setText("FPS = " + FPS1);
//                	tv2.setText("FPS = " + FPS2);
//                	tv3.setText("FPS = " + FPS3);
//                	tv4.setText("FPS = " + FPS4);
//                	tv5.setText("FPS = " + FPS5);
//                	tv6.setText("FPS = " + FPS6);
//                	tv7.setText("FPS = " + FPS7);
//                	tvAVG.setText("AVG FPS = " + (int)averageFPS);
//                	surfaceFPS1.resetFrameCounter();
//                	surfaceFPS2.resetFrameCounter();
//                	surfaceFPS3.resetFrameCounter();
//                	surfaceFPS4.resetFrameCounter();
//                	surfaceFPS5.resetFrameCounter();
//                	surfaceFPS6.resetFrameCounter();
//                	surfaceFPS7.resetFrameCounter();
//                }
//            });
//        }
//    }
    
    private static class UpdaterAVG8 extends TimerTask {
    	private TextView[] mTvArray;
    	private SurfaceTextureView[] mStArray;
    	private int mCount;
    	
    	private TextView tvAVG;
        private TextView tv1,tv2,tv3,tv4,tv5,tv6,tv7,tv8;
        private SurfaceTextureView surfaceFPS1,surfaceFPS2,surfaceFPS3,surfaceFPS4,surfaceFPS5,surfaceFPS6,surfaceFPS7,surfaceFPS8;

        public UpdaterAVG8(TextView tv1,TextView tv2,TextView tv3,TextView tv4,TextView tv5,TextView tv6,TextView tv7,TextView tv8, TextView tvAVG, SurfaceTextureView surfaceTextureView1, SurfaceTextureView surfaceTextureView2, SurfaceTextureView surfaceTextureView3, SurfaceTextureView surfaceTextureView4, SurfaceTextureView surfaceTextureView5, SurfaceTextureView surfaceTextureView6, SurfaceTextureView surfaceTextureView7, SurfaceTextureView surfaceTextureView8) {
           this.tvAVG = tvAVG;
           mCount = 8;
           mTvArray = new TextView[mCount];
           mStArray = new SurfaceTextureView[mCount];
           mTvArray[0] = this.tv1 = tv1;
           mTvArray[1] = this.tv2 = tv2;
           mTvArray[2] = this.tv3 = tv3;
           mTvArray[3] = this.tv4 = tv4;
           mTvArray[4] = this.tv5 = tv5;
           mTvArray[5] = this.tv6 = tv6;
           mTvArray[6] = this.tv7 = tv7;
           mTvArray[7] = this.tv8 = tv8;
           mStArray[0] = this.surfaceFPS1 = surfaceTextureView1;
           mStArray[1] = this.surfaceFPS2 = surfaceTextureView2;
           mStArray[2] = this.surfaceFPS3 = surfaceTextureView3;
           mStArray[3] = this.surfaceFPS4 = surfaceTextureView4;
           mStArray[4] = this.surfaceFPS5 = surfaceTextureView5;
           mStArray[5] = this.surfaceFPS6 = surfaceTextureView6;
           mStArray[6] = this.surfaceFPS7 = surfaceTextureView7;
           mStArray[7] = this.surfaceFPS8 = surfaceTextureView8;
        }


        public UpdaterAVG8(int aCount, TextView[] aTvArray, TextView aTvAVG, SurfaceTextureView[] aStArray) {
        	tvAVG = tvAVG;
        	mCount = aCount;
            mTvArray =   aTvArray;
            mStArray = aStArray;
        }
        
        @Override
        public void run() {
        	tvAVG.post(new Runnable() {

                public void run() {
	            	timeFPS = System.currentTimeMillis() - prev_time;
	            	prev_time = System.currentTimeMillis();
	            	if(timeFPS > 0)
	            		fps = 1/(timeFPS/1000);
	            	Log.d("time", "time = " + timeFPS);
	            	
	            	int averageFPS = 0;
	            	for (int i = 0; i < mCount; i++) {
	            		int fps = mStArray[i].getFrameCounter();
	            		mTvArray[i].setText("FPS = " + fps);
	            		averageFPS += fps;
	            		mStArray[i].resetFrameCounter();
	            	}
	            	averageFPS /= mCount;
	            	tvAVG.setText("AVG FPS = " + (int)averageFPS);
                }
            });
        }
    }
    
    
    private void initSurfaceTextures()
    {
    	//FrameLayout fl;
    	TextView tv1, tv2, tv3, tv4, tv5, tv6, tv7, tv8, tvAVG1, tvAVG2, tvAVG3, tvAVG4, tvAVG5, tvAVG6, tvAVG7, tvAVG8;
    	FrameLayout previewBlock1, previewBlock2, previewBlock3, previewBlock4, previewBlock5, previewBlock6, previewBlock7, previewBlock8;
    	SurfaceTextureView surfaceTextureView1, surfaceTextureView2, surfaceTextureView3, surfaceTextureView4, surfaceTextureView5, surfaceTextureView6, surfaceTextureView7, surfaceTextureView8; 
    	Timer timing1, timing2, timing3, timing4, timing5, timing6, timing7, timing8, timingAVG1, timingAVG2, timingAVG3, timingAVG4, timingAVG5, timingAVG6, timingAVG7, timingAVG8;

		int maxid = getMaxFrameViewIdx();
		for (int i = 0; i < maxid; i++) {
			int fvId = getFrameViewId(i);
			FrameLayout fl = (FrameLayout)findViewById(fvId);
			fl.addView(new SurfaceTextureView(this));
		}
        
        switch(MAX_VIDEOS)
        {
        	case 8:
                tv1 = (TextView)findViewById(R.id.textFPS1); 
            	previewBlock1 = (FrameLayout)findViewById(R.id.frameLayout1);        
            	surfaceTextureView1 = (SurfaceTextureView)previewBlock1.getChildAt(0);
                
                tv2 = (TextView)findViewById(R.id.textFPS2);
                previewBlock2 = (FrameLayout)findViewById(R.id.frameLayout2);        
            	surfaceTextureView2 = (SurfaceTextureView)previewBlock2.getChildAt(0);
                
                tv3 = (TextView)findViewById(R.id.textFPS3);
                previewBlock3 = (FrameLayout)findViewById(R.id.frameLayout3);        
            	surfaceTextureView3 = (SurfaceTextureView)previewBlock3.getChildAt(0);
                
                tv4 = (TextView)findViewById(R.id.textFPS4);
                previewBlock4 = (FrameLayout)findViewById(R.id.frameLayout4);        
            	surfaceTextureView4 = (SurfaceTextureView)previewBlock4.getChildAt(0);
                
                tv5 = (TextView)findViewById(R.id.textFPS5);
                previewBlock5 = (FrameLayout)findViewById(R.id.frameLayout5);        
            	surfaceTextureView5 = (SurfaceTextureView)previewBlock5.getChildAt(0);
                
                tv6 = (TextView)findViewById(R.id.textFPS6);
                previewBlock6 = (FrameLayout)findViewById(R.id.frameLayout6);        
            	surfaceTextureView6 = (SurfaceTextureView)previewBlock6.getChildAt(0);
            	
            	tv7 = (TextView)findViewById(R.id.textFPS7);
                previewBlock7 = (FrameLayout)findViewById(R.id.frameLayout7);        
            	surfaceTextureView7 = (SurfaceTextureView)previewBlock7.getChildAt(0);
            	
            	tv8 = (TextView)findViewById(R.id.textFPS8);
                previewBlock8 = (FrameLayout)findViewById(R.id.frameLayout8);        
            	surfaceTextureView8 = (SurfaceTextureView)previewBlock8.getChildAt(0);
                
                tvAVG8 = (TextView)findViewById(R.id.textFPSAverage);
    	        timingAVG8 = new Timer();
    	        timingAVG8.scheduleAtFixedRate(new UpdaterAVG8(tv1,tv2,tv3,tv4,tv5,tv6,tv7,tv8,tvAVG8,surfaceTextureView1,surfaceTextureView2,surfaceTextureView3,surfaceTextureView4,surfaceTextureView5,surfaceTextureView6,surfaceTextureView7,surfaceTextureView8), 0, 1000);  	
            	break;
        }
        
    }
}