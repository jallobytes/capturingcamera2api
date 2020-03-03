package com.jallobyte.camera2api;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.util.Arrays;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static androidx.core.app.ActivityCompat.requestPermissions;

public class Camera2API {
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private final Context context;
    private TextureView mTextureView;
    private CameraDevice mCameraDevice;
    private String mCameraID;
    private Size textureSize;
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);
    private CameraManager cameraManager;
    private CameraCaptureSession mCaptureSession;

    public Camera2API(TextureView mTextureView, Context context){
        this.mTextureView = mTextureView;
        this.context = context;
    }


    public void getCameraID() throws CameraAccessException {
        CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);

        for (String cameraID : cameraManager.getCameraIdList()) {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraID);

            Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);

            if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                mCameraID = cameraID;
            }
        }
    }
    public void openCamera(){
        cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);

        try {
            getCameraID();
            Log.d("exception", mCameraID);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        //Permission
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            Activity activity = (Activity) context;
            requestPermissions(activity, new String[] {Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }

        try {
            if (!mCameraOpenCloseLock.tryAcquire(3000, TimeUnit.MILLISECONDS)){
                throw new RuntimeException();
            }

            Log.d("exception", "openning camera");
            cameraManager.openCamera(mCameraID, openCameraCallBack, null);

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private final CameraDevice.StateCallback openCameraCallBack = new CameraDevice.StateCallback(){
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCameraOpenCloseLock.release();
            mCameraDevice = camera;
            CreateCameraPreviewSession();

        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            mCameraOpenCloseLock.release();
            mCameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            mCameraOpenCloseLock.release();
            mCameraDevice.close();
            mCameraDevice = null;
            Activity activity = (Activity) context;
            if (null != activity) {
                activity.finish();
            }
        }
    };

    public void CreateCameraPreviewSession(){
        SurfaceTexture texture = mTextureView.getSurfaceTexture();
        Surface surface = new Surface(texture);

        try {
            final CaptureRequest.Builder previewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG);
            previewBuilder.addTarget(surface);
            previewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

            CameraCaptureSession.StateCallback previewSessionCallBack = new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    mCaptureSession = session;
                    try {
                        session.setRepeatingRequest(previewBuilder.build(), null, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
            };

            mCameraDevice.createCaptureSession(Arrays.asList(surface), previewSessionCallBack, null);



        } catch (CameraAccessException e) {
            e.printStackTrace();
        }


    }


    }
