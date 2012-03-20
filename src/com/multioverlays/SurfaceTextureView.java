// Copyright 2011 Google Inc. All Rights Reserved.

package com.multioverlays;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.graphics.SurfaceTexture.OnFrameAvailableListener;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

class SurfaceTextureView extends GLSurfaceView {
    static final private String TAG = "VideoChatTest";

    private int mTextureName;
    private SurfaceTexture mSurfaceTexture;
    
    private static final boolean kUseMultisampling = true;
    private MultisampleConfigChooser mConfigChooser;
    private float alpha = 0.3f;
    private int frameCounter = 0;
    private int frameCounterNotReset = 0;
    private int count2frames = 0;
    private boolean changingAlpha = false;
    private boolean clearEveryFrame = false;
    
    public int getTextureName() {
        return mTextureName;
    }
    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }

    public static int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        if (shader != 0) {
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                Log.e(TAG, "Could not compile shader " + shaderType + ":");
                Log.e(TAG, GLES20.glGetShaderInfoLog(shader));
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
        }
        return shader;
    }

    public static void checkGlError(String op) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, op + ": glError " + error);
            throw new RuntimeException(op + ": glError " + error);
        }
    }

    public static int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        if (vertexShader == 0) {
            return 0;
        }
        int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        if (pixelShader == 0) {
            return 0;
        }

        int program = GLES20.glCreateProgram();
        if (program != 0) {
            GLES20.glAttachShader(program, vertexShader);
            checkGlError("glAttachShader");
            GLES20.glAttachShader(program, pixelShader);
            checkGlError("glAttachShader");
            GLES20.glLinkProgram(program);
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] != GLES20.GL_TRUE) {
                Log.e(TAG, "Could not link program: ");
                Log.e(TAG, GLES20.glGetProgramInfoLog(program));
                GLES20.glDeleteProgram(program);
                program = 0;
            }
        }
        return program;
    }

    AtomicInteger mReportedFrameCount = new AtomicInteger();
    AtomicBoolean mCameraEnabled = new AtomicBoolean();
    AtomicInteger mCameraFrameCount = new AtomicInteger();

    /**
     * @param context
     */
    public SurfaceTextureView(Context context) {
        super(context);
        init();
    }

    public SurfaceTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    private void init() {
    	setZOrderOnTop(true); 
    	this.getHolder().setFormat(PixelFormat.TRANSPARENT);
        setEGLContextClientVersion(2);
        if(kUseMultisampling)
        	setEGLConfigChooser(mConfigChooser = new MultisampleConfigChooser());
        
        setRenderer(new Renderer());

    }

    public void setCameraEnabled(boolean enabled) {
        mCameraEnabled.set(enabled);
    }

    public void resetFrameCounter() {
    	frameCounter = 0;
    }

    public int getFrameCounter() {
        return frameCounter;
    }
    
    public void setAlphaUp() {
    	changingAlpha = true;
    	count2frames = 0;
    	if(alpha < 1)
    		alpha += 0.1;
    }
    
    public void setAlphaDown() {
    	changingAlpha = true;
    	count2frames = 0;
    	if(alpha > 0.1)
    		alpha -= 0.1;
    }
    
    public void toggleClearEveryFrame() {
    	if(clearEveryFrame == false)
    		clearEveryFrame = true;
    	else
    		clearEveryFrame = false;
    }
   
    public double timeFPS;
    public double getFPS()
    {
    	return 1/(timeFPS/1000);
    }

 
    
    class Renderer implements GLSurfaceView.Renderer {
        private final static String VERTEX_SHADER =
            "attribute vec4 vPosition;\n" +
            "attribute vec2 a_texCoord;\n" +
            "varying vec2 v_texCoord;\n" +
            "uniform mat4 u_xform;\n" +
            "void main() {\n" +
            "  gl_Position = vPosition;\n" +
            "  v_texCoord = vec2(u_xform * vec4(a_texCoord, 1.0, 1.0));\n" +
            "}\n";

        private final static String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
            "uniform samplerExternalOES s_texture;\n" +
            "varying vec2 v_texCoord;\n" +
            "void main() {\n" +
            "  gl_FragColor = texture2D(s_texture, v_texCoord);\n" +
            "}\n";

        private final float[] TEXTURE_VERTICES =
              { 1.0f, 1.0f, 
        		0.0f, 1.0f, 
        		0.0f, 0.0f, 
        		1.0f, 0.0f };
        

        private final float[] QUAD_VERTICES =
            	{ 1.0f, 1.0f, 
        		-1.0f,  1.0f, 
        		-1.0f,  -1.0f, 
        		 1.0f,  -1.0f };
        

        private final static int FLOAT_SIZE_BYTES = 4;

        private final FloatBuffer mTextureVertices;
        private final FloatBuffer mQuadVertices;

        
        private int mGLProgram;
        private int mTexCoordHandle;
        private int mTriangleVerticesHandle;
        private int mViewWidth;
        private int mViewHeight;
        private Random randColor;
        private float r,g,b;
        
        public Renderer() {
            mTextureVertices = ByteBuffer.allocateDirect(TEXTURE_VERTICES.length *
                    FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
            mTextureVertices.put(TEXTURE_VERTICES).position(0);
            mQuadVertices = ByteBuffer.allocateDirect(QUAD_VERTICES.length *
                    FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
            mQuadVertices.put(QUAD_VERTICES).position(0);

        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            mGLProgram = createProgram(VERTEX_SHADER, FRAGMENT_SHADER);

            mTexCoordHandle = GLES20.glGetAttribLocation(mGLProgram, "a_texCoord");
            mTriangleVerticesHandle = GLES20.glGetAttribLocation(mGLProgram, "vPosition");
            int[] textures = new int[1];
            GLES20.glGenTextures(1, textures, 0);
            mTextureName = textures[0];
            GLES20.glUseProgram(mGLProgram);
            GLES20.glVertexAttribPointer(mTexCoordHandle, 2, GLES20.GL_FLOAT,
                    false, 0, mTextureVertices);
            GLES20.glVertexAttribPointer(mTriangleVerticesHandle, 2, GLES20.GL_FLOAT,
                    false, 0, mQuadVertices);
            checkGlError("initialization");
            randColor = new Random();
            r = (float) (randColor.nextFloat());
            g = (float) (randColor.nextFloat());
            b = (float) (randColor.nextFloat());
        }


        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            mViewWidth = width;
            mViewHeight = height;
        }
                
        private static final int GL_TEXTURE_EXTERNAL_OES = 0x8D65;
        public double prev_time = 0;
        public double fps;
        @Override
        public void onDrawFrame(GL10 gl) {
        	if(frameCounterNotReset < 2 || changingAlpha == true || clearEveryFrame == true)
        	{           	
	            GLES20.glClearColor(r*alpha, g*alpha, b*alpha, alpha);
	            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);  
	            count2frames++;				// for double-buffering
	            if(count2frames >= 2)
		            changingAlpha = false;
	            frameCounterNotReset++;
        	}
        	frameCounter++;
       }     
    }
}