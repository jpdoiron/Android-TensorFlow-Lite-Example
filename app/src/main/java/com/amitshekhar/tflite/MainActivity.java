package com.amitshekhar.tflite;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.renderscript.RenderScript;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.TextureView;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {


    private static final String MODEL_PATH = "mobilenet_quant_v1_224.tflite";
    private static final String LABEL_PATH = "labels.txt";
    private static final int INPUT_SIZE = 224;
    public static final String TAG = "MainActivity" ;

    private Classifier classifier;

    private Executor executor = Executors.newSingleThreadExecutor();

    private static MainActivity instance;

    public static MainActivity getInstance() {
        return instance;
    }

    public TextureView mTextureView;
    RenderScriptHelper mHelper = new RenderScriptHelper();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;

        mHelper.Init();

        setContentView(R.layout.activity_main);
        initTensorFlowAndLoadModel();

        mTextureView = findViewById( R.id.MyTexture );

        mRs = RenderScript.create(this);


        Intent intent = new Intent(this, LiveCameraActivity.class);
        startActivity(intent);



    }

    @Override
    protected void onResume() {
        super.onResume();
        //cameraView.start();
    }

    @Override
    protected void onPause() {
       // cameraView.stop();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                classifier.close();
            }
        });
    }

    public void onFrameArrayInt(int[] frameDataRgb) {
        Log.d("Main Activity", "OnFrame:" + frameDataRgb.length);
       /* Bitmap bitmap = Bitmap.createBitmap(frameDataRgb, mVideoSize.getWidth(), mVideoSize.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = mVideoView.lockCanvas();
        if (c != null) {
            c.drawColor(Color.BLACK);
            c.drawBitmap(Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), mBitMatrix, true), mRectSrc, mRectDest, paL);
            mVideoView.unlockCanvasAndPost(c);
        }*/
    }

    private void initTensorFlowAndLoadModel() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    //classifier = TensorFlowImageClassifier.create(
                    //classifier = YoloQuantClassifier.create(
                    classifier = YoloClassifier.create(
                    //classifier = YoloV3Classifier.create(
                    //classifier = YoloV3QuantClassifier.create(
                            getAssets(),
                            MODEL_PATH,
                            LABEL_PATH,
                            INPUT_SIZE);
                } catch (final Exception e) {
                    throw new RuntimeException("Error initializing TensorFlow!", e);
                }
            }
        });
    }

    private RenderScript mRs ;

    public void ProcessImage(Bitmap bmp, TextureView textureView) {

        if(bmp==null)
            return;

        //Size imageSize = new Size(320,240);

        //int size = bmp.getRowBytes() * bmp.getHeight();
        //ByteBuffer byteBuffer = ByteBuffer.allocate(size);
        //bmp.copyPixelsToBuffer(byteBuffer);
        //byte[] data = byteBuffer.array();


        //Bitmap myBitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length,null);

        //Bitmap rgbBitmap = RenderScriptHelper.convertYuvToRgbIntrinsic(mRs,data,imageSize );



        Bitmap rgbBitmap = mHelper.resizeBitmap2(mRs,bmp,224);

        //Log.d("Main Activity", "w: " + rgbBitmap.getWidth()+ " h: " + rgbBitmap.getHeight());

        List<Box> boxes =  classifier.recognizeImage(rgbBitmap);

        CustomView.getInstance().DrawingBox = boxes;
        CustomView.getInstance().invalidate();

/*
        if(rgbBitmap==null)return;

        try {
            TextureView r = findViewById( R.id.textureview );

            Canvas canvas = r.lockCanvas();
            if(canvas != null) {
                Paint myPaint = new Paint();
                myPaint.setStyle(Paint.Style.STROKE);
                myPaint.setColor(Color.rgb(0, 0, 0));
                myPaint.setStrokeWidth(10);
                canvas.drawRect(100, 100, 200, 200, myPaint);
                Log.d(TAG,"Canvas good");
            }else
            {
                Log.d(TAG,"Canvas null");
            }

        }catch (Exception e)
        {

        }
*/
        // Log.d("Main Activity", "w: " + rgbBitmap.getWidth() + " h: " + rgbBitmap.getHeight());

    }
}
