package com.amitshekhar.tflite;

import android.annotation.SuppressLint;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by amitshekhar on 17/03/18.
 */

public class YoloClassifier implements Classifier {

    static final float WIDTH_NORM = 224;
    static final float HEIGHT_NORM = 224;
    static final float GRID_NUM = 11;
    static final float X_SPAN = WIDTH_NORM / GRID_NUM;
    static final float Y_SPAN = HEIGHT_NORM / GRID_NUM;
    static final float X_NORM = WIDTH_NORM / GRID_NUM;
    static final float Y_NORM = HEIGHT_NORM / GRID_NUM;

    private static final int MAX_RESULTS = 3;
    private static final int BATCH_SIZE = 1;
    private static final int PIXEL_SIZE = 3*4;
    private static final float THRESHOLD = 0.1f;

    private Interpreter interpreter;
    private int inputSize;
    private List<String> labelList;
    private AssetManager assetManager;


    private static final String MODEL_PATH = "YoloV1-v7.tflite";
    private static final String LABEL_PATH = "labels.txt";
    private static final int INPUT_SIZE = 224;

    protected float imgDataFloat[][][][] = new float[BATCH_SIZE][INPUT_SIZE][INPUT_SIZE][3];
    float[][] result = new float[1][1573];

    ByteBuffer byteBuffer = null;

    private YoloClassifier() {


        byteBuffer = ByteBuffer.allocateDirect(BATCH_SIZE * INPUT_SIZE * INPUT_SIZE * PIXEL_SIZE);
        byteBuffer.order(ByteOrder.nativeOrder());

    }

    static Classifier create(AssetManager assetManager,
                             String modelPath,
                             String labelPath,
                             int inputSize) throws IOException {

        YoloClassifier classifier = new YoloClassifier();
        classifier.interpreter = new Interpreter(classifier.loadModelFile(assetManager, MODEL_PATH));
        classifier.labelList = classifier.loadLabelList(assetManager, LABEL_PATH);
        classifier.inputSize = INPUT_SIZE;
        classifier.assetManager = assetManager;




        return classifier;
    }

    @Override
    public void recognizeFloat(ByteBuffer data)
    {
        long start = System.currentTimeMillis();

        //ByteBuffer bb = ByteBuffer.wrap(data);

        //Log.d("inference 2 bb:", data.getFloat() + ":" + data.getFloat() + ":" + data.getFloat());

        data.rewind();

        interpreter.run(data, result);

        long finish = System.currentTimeMillis();
        long timeElapsed = finish - start;
        Log.d("inference","time : " + timeElapsed);

        getSortedResult(result[0]);
    }

    @Override
    public List<Recognition> recognizeImage(Bitmap bitmap) {

        Bitmap bm =null;
        try {
            bm = BitmapFactory.decodeStream(assetManager.open("screen_1.png"));
        } catch(IOException e) {
            // handle exception
        }

       //float[] readback=new float[224*244*3];
        ByteBuffer bb = convertBitmapToByteBuffer(bitmap);
            //FloatBuffer a =  bb.asFloatBuffer();
    /*
            bb.rewind();

            Log.d("inference bb:", bb.getFloat() + ":" + bb.getFloat() + ":" + bb.getFloat());

            bb.rewind();
    */

        //float[] myFloatArray = new float[BATCH_SIZE * inputSize * inputSize*3];
        //ByteBuffer.wrap(byteBuffer.array()).asFloatBuffer().get(myFloatArray);

        long start = System.currentTimeMillis();

        interpreter.run(bb, result);

        long finish = System.currentTimeMillis();
        long timeElapsed = finish - start;
        Log.d("inference","time : " + timeElapsed);

        return getSortedResult(result[0]);
    }

    @Override
    public void close() {
        interpreter.close();
        interpreter = null;
    }

    private MappedByteBuffer loadModelFile(AssetManager assetManager, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = assetManager.openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private List<String> loadLabelList(AssetManager assetManager, String labelPath) throws IOException {
        List<String> labelList = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(assetManager.open(labelPath)));
        String line;
        while ((line = reader.readLine()) != null) {
            labelList.add(line);
        }
        reader.close();
        return labelList;
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {


        byteBuffer.rewind();
        int[] intValues = new int[inputSize * inputSize];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        int pixel = 0;
        for (int i = 0; i < inputSize; ++i) {
            for (int j = 0; j < inputSize; ++j) {
                final int val = intValues[pixel++];

                //byteBuffer.put((byte) ((val >> 16) & 0xFF));
                //byteBuffer.put((byte) ((val >> 8) & 0xFF));
                //byteBuffer.put((byte) (val & 0xFF));

                float r = (val >> 16 & 0xFF) /127.5f - 1;
                float g = (val >> 8 & 0xFF) / 127.5f - 1;
                float b = (val & 0xFF) /127.5f - 1;

                byteBuffer.putFloat(r);
                byteBuffer.putFloat(g);
                byteBuffer.putFloat(b);

                //imgDataFloat[0][i][j][0] = r;
                //imgDataFloat[0][i][j][1] = g;
                //imgDataFloat[0][i][j][2] = b;
            }
        }

        return byteBuffer;
    }


    @SuppressLint("DefaultLocale")
    private List<Recognition> getSortedResult(float[] ResultArray) {

/*
            for (int h =0;h<10;h++)
            {
                float r1 = ResultArray[h];
                Log.d("inference","r1: " + r1);
            }
*/

        final ArrayList<Boxes> myBoxes = new ArrayList<>();



        for (int h =0;h<=10;h++)
        for (int w =0;w<=10;w++) {

            float c = ResultArray[(h * 11 * 3) + w * 3];
            float c1 = ResultArray[(h * 11 * 3) + w * 3 + 1];
            float c2 = ResultArray[(h * 11 * 3) + w * 3 + 2];


            float p1 = ResultArray[363 + (h * 11 + w) * 2];
            float p2 = ResultArray[363 + (h * 11 + w) * 2 + 1];

            //Log.d("inference",h + "," + w + " Prob : " + p1  +"-"+ p1);

            if (p1 > 0.3) {
                Boxes b = new Boxes();
                b.Probability = p1;


                //dims = 200 = dim imame
                //remplacé par camera size
                //angle ,30.93226,112.0588,51.99175,47.37777
                int pos = 605 + (h * 11 + w) * 8;
                float bx = sigmoid(ResultArray[pos]) * X_NORM + w * X_SPAN;  // * 200.0f / 224.0f
                float by = sigmoid(ResultArray[pos + 1]) * Y_NORM + h * Y_SPAN;  // * 200 / 224
                float bw = sigmoid(ResultArray[pos + 2]) * WIDTH_NORM;  // * 200 / 224
                float bh = sigmoid(ResultArray[pos + 3]) * HEIGHT_NORM;  // * 200 / 224

                b.x1 = (int)(bx - bw / 2);
                b.x2 = (int)(bx + bw / 2);
                b.y1 = (int)(by - bh / 2);
                b.y2 = (int)(by + bh / 2);

                myBoxes.add(b);

            }
            if (p2 > 0.3) {
                Boxes b = new Boxes();
                b.Probability = p2;


                //dims = 200 = dim imame
                //remplacé par camera size
                //angle ,30.93226,112.0588,51.99175,47.37777
                int pos = 605 + (h * 11 + w) * 8;
                float bx = sigmoid(ResultArray[pos + 4]) * X_NORM + w * X_SPAN;//* 200.0f / 224.0f
                float by = sigmoid(ResultArray[pos + 5]) * Y_NORM + h * Y_SPAN;//* 200 / 224
                float bw = sigmoid(ResultArray[pos + 6]) * WIDTH_NORM;//* 200 / 224
                float bh = sigmoid(ResultArray[pos + 7]) * HEIGHT_NORM;//* 200 / 224

                b.x1 = (int)(bx - bw / 2);
                b.x2 = (int)(bx + bw / 2);
                b.y1 = (int)(by - bh / 2);
                b.y2 = (int)(by + bh / 2);

                myBoxes.add(b);
            }
        }


        for(int x=0;x<myBoxes.size();x++)
        {
            Log.d("inference","Box : " + myBoxes.toString());

        }

        /*
        for (int h =0;h<10;h++)
        for (int w =0;w<10;w++) {
            Log.d("inference","line");
            float r1 = ResultArray[(h * 11 + w) * 3];
            float r2 = ResultArray[(h * 11 + w) * 3 + 1];
            float r3 =  ResultArray[(h * 11 + w) * 3 + 2];

            Log.d("inference","r1: " + r1);
            Log.d("inference","r1: " + r2);


        }
            Log.d("inference","r1: " + r3);
*/

        final ArrayList<Recognition> recognitions = new ArrayList<>();

        return recognitions;

    }


    private float sigmoid(float x){
        return (float)(1 / (1 + Math.exp(-x)));
    }

    class Boxes {
        public String toString(){
            String s = "p:" + Probability + " : " + x1 + "," + y1 + "," + x2 + "," + y2;
            return s;
        }

        float Probability  = 0.0f;
        int x1 = 0;
        int y1 = 0;
        int x2 = 0;
        int y2 = 0;


    }


}
