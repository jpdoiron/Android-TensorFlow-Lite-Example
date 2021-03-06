package com.amitshekhar.tflite;

import android.annotation.SuppressLint;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
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

public class YoloV3Classifier implements Classifier {

    private static final int MAX_RESULTS = 3;
    private static final int BATCH_SIZE = 1;
    private static final int PIXEL_SIZE = 3*4;
    private static final float THRESHOLD = 0.1f;

    private Interpreter interpreter;
    private int inputSize;
    private List<String> labelList;

    private static final String MODEL_PATH = "YOLOv3-tiny-mobilenet.tflite";

    private static final String LABEL_PATH = "labels.txt";
    private static final int INPUT_SIZE = 224;

    protected float imgDataFloat[][][][] = new float[BATCH_SIZE][INPUT_SIZE][INPUT_SIZE][3];
    float[][][][] result = new float[1][7][7][18] ;

    private YoloV3Classifier() {

    }

    static Classifier create(AssetManager assetManager,
                             String modelPath,
                             String labelPath,
                             int inputSize) throws IOException {

        YoloV3Classifier classifier = new YoloV3Classifier();
        classifier.interpreter = new Interpreter(classifier.loadModelFile(assetManager, MODEL_PATH));
        classifier.labelList = classifier.loadLabelList(assetManager, LABEL_PATH);
        classifier.inputSize = INPUT_SIZE;

        return classifier;
    }


    @Override
    public void recognizeFloat(ByteBuffer data) {

    }

    @Override
    public List<Box> recognizeImage(Bitmap bitmap) {
        convertBitmapToByteBuffer(bitmap);

        //float[] myFloatArray = new float[BATCH_SIZE * inputSize * inputSize*3];
        //ByteBuffer.wrap(byteBuffer.array()).asFloatBuffer().get(myFloatArray);

        long start = System.currentTimeMillis();

        interpreter.run(imgDataFloat, result);

        long finish = System.currentTimeMillis();
        long timeElapsed = finish - start;
        Log.d("inference","time : " + timeElapsed);


        return getSortedResult(result);
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
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(BATCH_SIZE * inputSize * inputSize * PIXEL_SIZE);
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[inputSize * inputSize];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        int pixel = 0;
        for (int i = 0; i < inputSize; ++i) {
            for (int j = 0; j < inputSize; ++j) {
                final int val = intValues[pixel++];
                //byteBuffer.putFloat((val >> 16) & 0xFF);
                //byteBuffer.putFloat ((val >> 8) & 0xFF);
                //byteBuffer.putFloat(val & 0xFF);
                //byteBuffer.put((byte) ((val >> 16) & 0xFF));
                //byteBuffer.put((byte) ((val >> 8) & 0xFF));
                //byteBuffer.put((byte) (val & 0xFF));


                float r = (val >> 16 & 0xFF) / 127.5f - 1;
                float b = (val >> 0xFF) / 127.5f - 1;
                float g = (val >> 8 & 0xFF) / 127.5f - 1;

                imgDataFloat[0][i][j][0] = r;
                imgDataFloat[0][i][j][1] = g;
                imgDataFloat[0][i][j][2] = b;
            }
        }
        return byteBuffer;
    }


    @SuppressLint("DefaultLocale")
    private List<Box> getSortedResult(float[][][][]labelProbArray) {


        final List<Box> recognitions = new ArrayList<>();


        return recognitions;
    }

}
