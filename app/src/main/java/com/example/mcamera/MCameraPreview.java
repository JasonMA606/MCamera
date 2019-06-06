package com.example.mcamera;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

public class MCameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private final static String TAG_CAMERA = "mCamera";
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private Camera.Parameters parameters;
    public MCameraPreview(Context context,Camera mCamera) {
        super(context);
        this.mCamera = mCamera;
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            mCamera.setPreviewDisplay(holder);
            parameters = mCamera.getParameters();
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
//            parameters.getSupportedPictureSizes();
            /**Set Preview Size
             * ** I choose 1280x960 to keep 4:3 ratio.
             * Preview Size Range:
             * 1920x1440,1920x1080,1600x1200,1920x960,1280x960,1280x768,1280x720,1024x768,800x400,800x600,800x480,720x480,640x400,640x480,640x360,352x288,320x240,176x144,160x120
             * **/
            parameters.setPreviewSize(1280, 960);
            /**Set Picture Size
             * ** There is a trade off between picture clarity and upload speed.
             * Picture Size Range:
             * 4032x3024,4000x3000,3840x2160,4000x2000,3264x2448,3200x2400,2688x1512,2592x1944,2048x1536,1920x1440,1920x1080,1600x1200,1920x960,1280x960,1280x768,1280x720,1024x768,800x400,800x600,800x480,720x480,640x400,640x480,640x360,352x288,320x240,176x144
             * **/
            parameters.setPictureSize(4032, 3024);
            mCamera.setParameters(parameters);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d(TAG_CAMERA, "surfaceCreated: " + e.getMessage());
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (mHolder.getSurface() == null) {
            return;
        }
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
        }
        try {
            mCamera.setPreviewDisplay(holder);
            parameters = mCamera.getParameters();
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            parameters.setPreviewSize(1280, 960);
            parameters.setPictureSize(1280, 960);
            mCamera.setParameters(parameters);
            mCamera.startPreview();
        } catch (Exception e) {
            Log.d(TAG_CAMERA, "Error starting camera Preview: " + e.getMessage());
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
}
