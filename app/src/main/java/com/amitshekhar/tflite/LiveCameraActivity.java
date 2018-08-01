package com.amitshekhar.tflite;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Process;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.TextureView;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.amitshekhar.tflite.gles.EglCore;
import com.amitshekhar.tflite.gles.OffscreenSurface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;


/**
 * More or less straight out of TextureView's doc.
 * <p>
 * TODO: add options for different display sizes, frame rates, camera selection, etc.
 */
public class LiveCameraActivity extends Activity implements TextureView.SurfaceTextureListener {
    private static final String TAG = MainActivity.TAG;

    private Camera mCamera;
    private SurfaceTexture mSurfaceTexture;

    private volatile boolean mIsCanceled = false;

    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int ITERATIONS = 100;

    TextureView mTextureView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTextureView = new TextureView(this);
        mTextureView.setSurfaceTextureListener(this);

        setContentView(mTextureView);
    }




    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {

        mSurfaceTexture = surface;
        if (!PermissionHelper.hasCameraPermission(this)) {
            PermissionHelper.requestCameraPermission(this, false);
        } else {
            startPreview();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        // Ignored, Camera does all the work for us
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        mCamera.stopPreview();
        mCamera.release();
        return true;
    }

    int[]surface_pixels;
    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // Invoked every time there's a new Camera preview frame
        Log.d(TAG, "updated, ts=" + surface.getTimestamp());
        //AlertDialog dialog = showProgressDialog();


        try
        {
            Bitmap bmp = mTextureView.getBitmap();
            Log.d(TAG,"width : " + bmp.getWidth());
            MainActivity.getInstance().ProcessImage(bmp);

        }  catch (Exception e) { }



        //ReadPixelsTask task = new ReadPixelsTask(null, R.id.gfxResult_text, WIDTH, HEIGHT, ITERATIONS,surface);
        //task.execute();

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (!PermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(this,
                    "Camera permission is needed to run this application", Toast.LENGTH_LONG).show();
            PermissionHelper.launchPermissionSettings(this);
            finish();
        } else {
            startPreview();
        }
    }

    private void startPreview() {
        mCamera = Camera.open();
        if (mCamera == null) {
            // Seeing this on Nexus 7 2012 -- I guess it wants a rear-facing camera, but
            // there isn't one.  TODO: fix
            throw new RuntimeException("Default camera not available");
        }

        try {



            mCamera.setPreviewTexture(mSurfaceTexture);
            Display display = ((WindowManager)getSystemService(WINDOW_SERVICE)).getDefaultDisplay();

            if(display.getRotation() == Surface.ROTATION_0) {
                mCamera.setDisplayOrientation(90);
            }
            if(display.getRotation() == Surface.ROTATION_270) {
                mCamera.setDisplayOrientation(180);
            }
            mCamera.startPreview();
        } catch (IOException ioe) {
            // Something bad happened
            Log.e(TAG,"Exception starting preview", ioe);
        }
    }

    /**
     * Sets the text in the message field.
     */
    void setMessage(int id, String msg) {
        TextView result = findViewById(id);
        //result.setText(msg);
    }

    /**
     * AsyncTask class that executes the test.
     */
    private class ReadPixelsTask extends AsyncTask<Void, Integer, Long> {
        private int mWidth;
        private int mHeight;
        private int mIterations;
        private int mResultTextId;
        private AlertDialog mDialog;
        private SurfaceTexture mSurface;
        private ProgressBar mProgressBar;

        /**
         * Prepare for the glReadPixels test.
         */
        public ReadPixelsTask(AlertDialog dialog, int resultTextId,
                              int width, int height, int iterations,SurfaceTexture surface) {
            mDialog = dialog;
            mResultTextId = resultTextId;
            mWidth = width;
            mHeight = height;
            mIterations = iterations;
            mSurface = surface;
            //mProgressBar = dialog.findViewById(R.id.work_progress);
            //mProgressBar.setMax(mIterations);
        }
        EglCore eglCore = null;


        @Override
        protected Long doInBackground(Void... params) {
            long result = -1;
            OffscreenSurface surface = null;

            // TODO: this should not use AsyncTask.  The AsyncTask worker thread is run at
            // a lower priority, making it unsuitable for benchmarks.  We can counteract
            // it in the current implementation, but this is not guaranteed to work in
            // future releases.
            Process.setThreadPriority(Process.THREAD_PRIORITY_FOREGROUND);

            try {
                eglCore = new EglCore(null, 0);
                //surface = eglCore.createWindowSurface(mSurface);
                //surface = (EGLSurface)mSurface;

                surface = new OffscreenSurface(eglCore, mWidth, mHeight);
                mSurface.attachToGLContext(surface.mEGLSurface.getHandle());
                //surface = new WindowSurface(eglCore,mSurface,true);
                //surface = eglCore.createWindowSurface(mSurface);

                Log.d(TAG, "Buffer size " + mWidth + "x" + mHeight);
                result = runGfxTest(surface);
            } finally {
                if (surface != null) {


                    surface.release();

                }
                //}
                if (eglCore != null) {
                    eglCore.release();
                }
            }
            return result < 0 ? result : result / mIterations;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            //mProgressBar.setProgress(progress[0]);
            Log.d(TAG,"update:" + progress);
        }

        @Override
        protected void onPostExecute(Long result) {
            Log.d(TAG, "onPostExecute result=" + result);
            //mDialog.dismiss();
            mDialog = null;

            Resources res = getResources();
            if (result < 0) {
                setMessage(mResultTextId, res.getString(R.string.did_not_complete));
            } else {
                setMessage(mResultTextId, (result / 1000) +
                        res.getString(R.string.usec_per_iteration));
            }
        }

        /**
         * Does a simple bit of rendering and then reads the pixels back.
         *
         * @return total time spent on glReadPixels()
         */
        private long runGfxTest(OffscreenSurface eglSurface) {
            long totalTime = 0;
            long startWhen = System.nanoTime();


            eglSurface.makeCurrent();
            ByteBuffer pixelBuf = ByteBuffer.allocateDirect(mWidth * mHeight * 4);
            pixelBuf.order(ByteOrder.LITTLE_ENDIAN);

            Log.d(TAG, "Running...");
            /*
            float colorMult = 1.0f / mIterations;
            for (int i = 0; i < mIterations; i++) {
                if (mIsCanceled) {
                    Log.d(TAG, "Canceled!");
                    totalTime = -2;
                    break;
                }
                if ((i % (mIterations / 8)) == 0) {
                    publishProgress(i);
                }

                // Clear the screen to a solid color, then add a rectangle.  Change the color
                // each time.
                float r = i * colorMult;
                float g = 1.0f - r;
                float b = (r + g) / 2.0f;
                GLES20.glClearColor(r, g, b, 1.0f);
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

                GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
                GLES20.glScissor(mWidth / 4, mHeight / 4, mWidth / 2, mHeight / 2);
                GLES20.glClearColor(b, g, r, 1.0f);
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
                GLES20.glDisable(GLES20.GL_SCISSOR_TEST);

                // Try to ensure that rendering has finished.

                GLES20.glReadPixels(0, 0, 1, 1,
                        GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixelBuf);

                // Time individual extraction.  Ideally we'd be timing a bunch of these calls
                // and measuring the aggregate time, but we want the isolated time, and if we
                // just read the same buffer repeatedly we might get some sort of cache effect.

            }
            */
            GLES20.glFinish();
            GLES20.glReadPixels(0, 0, mWidth, mHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixelBuf);
            totalTime += System.nanoTime() - startWhen;

            //Log.d(TAG, "done");

            if (true) {
                // save the last one off into a file
                //startWhen = System.nanoTime();
                /*
                try {
                    eglSurface.saveFrame(new File(Environment.getExternalStorageDirectory(), "test.png"));
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                }
                */
                Log.d(TAG, "Saved frame in " +(totalTime / 1000000) + "ms");
            }

            return totalTime;
        }
    }

}