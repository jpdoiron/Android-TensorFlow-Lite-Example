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
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Created by amitshekhar on 17/03/18.
 */

public class YoloV3QuantClassifier implements Classifier {

    private static final int MAX_RESULTS = 3;
    private static final int BATCH_SIZE = 1;
    private static final int PIXEL_SIZE = 3;
    private static final float THRESHOLD = 0.1f;

    private Interpreter interpreter;
    private Interpreter interpreter2;
    private Interpreter interpreter3;
    private int inputSize;
    private List<String> labelList;


    private static final String MODEL_PATH = "Quant_YOLOv3-tiny-mobilenet.tflite";
    private static final String LABEL_PATH = "labels.txt";
    private static final int INPUT_SIZE = 224;

    private YoloV3QuantClassifier() {

    }

    static Classifier create(AssetManager assetManager,
                             String modelPath,
                             String labelPath,
                             int inputSize) throws IOException {

        YoloV3QuantClassifier classifier = new YoloV3QuantClassifier();
        classifier.interpreter = new Interpreter(classifier.loadModelFile(assetManager, MODEL_PATH));
        classifier.interpreter2 = new Interpreter(classifier.loadModelFile(assetManager, MODEL_PATH));
        classifier.interpreter3 = new Interpreter(classifier.loadModelFile(assetManager, MODEL_PATH));
        classifier.labelList = classifier.loadLabelList(assetManager, LABEL_PATH);
        classifier.inputSize = INPUT_SIZE;


        return classifier;
    }

    public static ByteBuffer DeepCopy(ByteBuffer original) {
        ByteBuffer clone = ByteBuffer.allocate(original.capacity());
        original.rewind();//copy from the beginning
        clone.put(original);
        original.rewind();
        clone.flip();
        return clone;
    }

    public class MyRunnable implements Runnable {

        ByteBuffer mBuffer;
        Interpreter mInterpreter;
        String mIdx;
        public MyRunnable(ByteBuffer buffer, Interpreter interpreter, String idx) {
            // store parameter for later user
            mBuffer = DeepCopy(buffer);
            mInterpreter = interpreter;
            mIdx = idx;
        }

        public void run() {
            long start = System.currentTimeMillis();
            byte[][][][] result = new byte[1][7][7][18] ;
            mInterpreter.run(mBuffer, result);

            long finish = System.currentTimeMillis();
            long timeElapsed = finish - start;
            Log.w("inference " + mIdx,"time : " + timeElapsed);
        }
    }

    @Override
    public void recognizeFloat(ByteBuffer data) {

    }

    @Override
    public List<Recognition> recognizeImage(Bitmap bitmap) {

        final ByteBuffer byteBuffer = convertBitmapToByteBuffer(bitmap);

        final byte[][][][] result = new byte[1][7][7][18] ;

        long start = System.currentTimeMillis();



        Runnable r1 = new MyRunnable(byteBuffer, interpreter2, "1");
        Thread t1 = new Thread(r1);
        t1.start();

        Runnable r2 = new MyRunnable(byteBuffer, interpreter3, "2");
        Thread t2 = new Thread(r2);
        t2.start();



        interpreter.run(byteBuffer, result);



        //Runnable r2 = new MyRunnable(byteBuffer);



       // Thread t2 = new Thread(r2);

        //t2.start();



        try { t1.join(); } catch (InterruptedException e) { e.printStackTrace(); }
        try{ t2.join(); } catch (InterruptedException e) { e.printStackTrace(); }



        long finish = System.currentTimeMillis();
        long timeElapsed = finish - start;

        Log.w("inference total","time : " + timeElapsed);


        byte[][] result2 = new byte[1][1573] ;


        return getSortedResult(result2);
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
    private List<Recognition> getSortedResult(byte[][] labelProbArray) {

        PriorityQueue<Recognition> pq =
                new PriorityQueue<>(
                        MAX_RESULTS,
                        new Comparator<Recognition>() {
                            @Override
                            public int compare(Recognition lhs, Recognition rhs) {
                                return Float.compare(rhs.getConfidence(), lhs.getConfidence());
                            }
                        });

        for (int i = 0; i < labelList.size(); ++i) {
            float confidence = (labelProbArray[0][i] & 0xff) / 255.0f;
            if (confidence > THRESHOLD) {
                pq.add(new Recognition("" + i,
                        labelList.size() > i ? labelList.get(i) : "unknown",
                        confidence));
            }
        }

        final ArrayList<Recognition> recognitions = new ArrayList<>();
        int recognitionsSize = Math.min(pq.size(), MAX_RESULTS);
        for (int i = 0; i < recognitionsSize; ++i) {
            recognitions.add(pq.poll());
        }

        return recognitions;
    }

}
