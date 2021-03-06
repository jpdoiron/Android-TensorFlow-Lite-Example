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

public class YoloQuantClassifier implements Classifier {

    private static final int MAX_RESULTS = 3;
    private static final int BATCH_SIZE = 1;
    private static final int PIXEL_SIZE = 3;
    private static final float THRESHOLD = 0.1f;

    private AssetManager assetManager;
    private Interpreter interpreter;
    private int inputSize;
    private List<String> labelList;

    private static final String MODEL_PATH = "Quant_yolov1-tiny.tflite";
    private static final String LABEL_PATH = "labels.txt";
    private static final int INPUT_SIZE = 224;

    private YoloQuantClassifier() {

    }

    static Classifier create(AssetManager assetManager,
                             String modelPath,
                             String labelPath,
                             int inputSize) throws IOException {

        YoloQuantClassifier classifier = new YoloQuantClassifier();
        classifier.interpreter = new Interpreter(classifier.loadModelFile(assetManager, MODEL_PATH));
        classifier.labelList = classifier.loadLabelList(assetManager, LABEL_PATH);
        classifier.inputSize = INPUT_SIZE;
        classifier.assetManager = assetManager;

        return classifier;
    }

    @Override
    public void recognizeFloat(ByteBuffer data) {

    }

    @Override
    public List<Box> recognizeImage(Bitmap bitmap) {


        //AssetFileDescriptor fileDescriptor = assetManager.openFd(modelPath);

        //Bitmap bm = BitmapFactory.decodeResource(activity.getResources(), R.drawable.image);
        Bitmap bm =null;
        try {
            bm = BitmapFactory.decodeStream(assetManager.open("screen_1.png"));
        } catch(IOException e) {
            // handle exception
        }

        ByteBuffer byteBuffer = convertBitmapToByteBuffer(bm);
        byte[][] result = new byte[1][1573];

        long start = System.currentTimeMillis();

        interpreter.run(byteBuffer, result);

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
                byteBuffer.put((byte) ((val >> 16) & 0xFF));
                byteBuffer.put((byte) ((val >> 8) & 0xFF));
                byteBuffer.put((byte) (val & 0xFF));
            }
        }
        return byteBuffer;
    }

    @SuppressLint("DefaultLocale")
    private List<Box> getSortedResult(byte[][] labelProbArray) {


        final List<Box> recognitions = new ArrayList<>();


        return recognitions;
    }

}
