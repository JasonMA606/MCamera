/**
 * Author: Jason Ma - Zhicheng Ma
 * Published: 06/06/2019
 * <p>
 * This demo tries to use android.hardware.Camera API to create a customView camera application.
 * Although android.hardware.Camera API is deprecated since API 21 (Android 5.0) and replaced by android.hardware.Camera2,
 * we could still use Camera which is more simple for a tyro.
 * <p>
 * In this case, we need to achieve five goals:
 * ** Using surfaceView to preview Camera.
 * ** Using cardView to make the preview looks better.
 * ** Changing camera preview ratio to 4:3 (more suitable for cardView Camera, not for whole screen camera)
 * ** Setting picture size.
 * ** Saving the picture on our Android device.
 **/

package com.example.mcamera;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

public class MainActivity extends AppCompatActivity {
    private static final String FILE_PATH = "/sdcard/mCamera";
    private static final String TAG_CAMERA = "mCamera";
    private Camera mCamera;
    private MCameraPreview mPreview;
    private static final int MY_PERMISSION_REQUEST_CAMERA = 00;
    private static final int MY_PERMISSION_REQUEST_WRITE_STORAGE = 01;
    private ImageButton imgbtn;
    private static String flname = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, MY_PERMISSION_REQUEST_CAMERA);
        }else {
            startCamera();
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSION_REQUEST_WRITE_STORAGE);
        }
        imgbtn = findViewById(R.id.imageButton);
        imgbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCamera.takePicture(null, null, mPicture);
            }
        });
    }

    /**If your application does not properly release the camera,
     * all subsequent attempts to access the camera,
     * including those by your own application,
     * will fail and may cause your or other applications to be shut down.
     * **/
    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
    }

    private void startCamera() {
        mCamera = getCameraInstance();
        setCameraDisplayOrientation(this, 0, mCamera);
        mPreview = new MCameraPreview(this, mCamera);
        FrameLayout preview = findViewById(R.id.mcamera_view);
        preview.addView(mPreview);
    }

    private void releaseCamera(){
        if (mCamera != null){
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }

    private Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSION_REQUEST_CAMERA:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Granted for using camera", Toast.LENGTH_SHORT).show();
                    startCamera();
                } else {
                    Toast.makeText(this, "Cannot using camera", Toast.LENGTH_SHORT).show();
                }
                break;
            case MY_PERMISSION_REQUEST_WRITE_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Granted for writing storage", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Cannot write storage", Toast.LENGTH_SHORT).show();
                }
        }
    }

    public static void setCameraDisplayOrientation(Activity activity,
                                                   int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            if (pictureFile == null) {
                Log.d(TAG_CAMERA, "Error creating media file, check storage permissions");
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
                releaseCamera();
                //When we get the raw picture, we could use Matrix to rotate the picture into correct direction.
                rotateImg();
                startCamera();
            } catch (FileNotFoundException e) {
                Log.d(TAG_CAMERA, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG_CAMERA, "Error accessing file: " + e.getMessage());
            }
        }
    };


    private static File getOutputMediaFile(int type) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(FILE_PATH);
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(TAG_CAMERA, "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            flname = mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpeg";
            mediaFile = new File(flname);
        } else if (type == MEDIA_TYPE_VIDEO) {
            flname = mediaStorageDir.getPath() + File.separator + "VID_" + timeStamp + ".mp4";
            mediaFile = new File(flname);
        } else {
            return null;
        }

        return mediaFile;
    }

    //Rotate picture into correct direction.
    private void rotateImg() {
        Bitmap picture = BitmapFactory.decodeFile(flname);
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        Bitmap resizeBitmap = Bitmap.createBitmap(picture, 0, 0, picture.getWidth(), picture.getHeight(), matrix, true);
        saveBmpToPath(resizeBitmap,flname);
    }

    //Save the new picture replace the old picture.
    public boolean saveBmpToPath(final Bitmap bitmap, final String filePath) {
        if (bitmap == null || filePath == null) {
            return false;
        }
        boolean result = false; //默认结果
        File file = new File(filePath);
        OutputStream outputStream = null; //文件输出流
        try {
            outputStream = new FileOutputStream(file);
            result = bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream); //将图片压缩为JPEG格式写到文件输出流，100是最大的质量程度
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close(); //关闭输出流
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }
}
