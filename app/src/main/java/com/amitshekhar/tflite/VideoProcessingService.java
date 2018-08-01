package com.amitshekhar.tflite;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.util.Arrays;

public class VideoProcessingService extends Service {
    private static final String TAG = "VideoProcessing";
    private static final int CAMERA = CameraCharacteristics.LENS_FACING_FRONT;
    private CameraDevice mCamera;
    private CameraCaptureSession mSession;
    private ImageReader mImageReader;

    private CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            VideoProcessingService.this.mCamera = camera;

            try {
                mCamera.createCaptureSession(Arrays.asList(mImageReader.getSurface()), sessionStateCallback, null);
            } catch (CameraAccessException e){
                Log.e(TAG, e.getMessage());
            }
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
        }

        @Override
        public void onError(CameraDevice camera, int error) {
        }
    };

    private CameraCaptureSession.StateCallback sessionStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession session) {
            VideoProcessingService.this.mSession = session;
            try {
                session.setRepeatingRequest(createCaptureRequest(), null, null);
            } catch (CameraAccessException e) {
                Log.e(TAG, e.getMessage());
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
        }
    };

    private ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image img = reader.acquireLatestImage();
           // processImage(img);
            if(img!=null)
                img.close();
        }
    };

    @Override
    public void onCreate() {
        CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG,"Permission check");
                return;
            }
            manager.openCamera(getCamera(manager), cameraStateCallback, null);
            mImageReader = ImageReader.newInstance(320, 240, ImageFormat.YUV_420_888, 30); //fps * 10 min
            mImageReader.setOnImageAvailableListener(onImageAvailableListener, null);
        } catch (CameraAccessException e){
            Log.e(TAG, e.getMessage());
        }
    }

    /**
     *  Return the Camera Id which matches the field CAMERA.
     */
    public String getCamera(CameraManager manager){
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                int cOrientation = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (cOrientation == CameraCharacteristics.LENS_FACING_BACK ) {
                    return cameraId;
                }
            }
        } catch (CameraAccessException e){
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        try {
            mSession.abortCaptures();
        } catch (CameraAccessException e){
            Log.e(TAG, e.getMessage());
        }
        mSession.close();
    }

    /**
     *  Process image data as desired.
     */
    private void processImage(Bitmap image){
        //Process image data

        MainActivity.getInstance().ProcessImage(image);


    }

    private CaptureRequest createCaptureRequest() {
        try {
            CaptureRequest.Builder builder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            builder.addTarget(mImageReader.getSurface());


            return builder.build();
        } catch (CameraAccessException e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}