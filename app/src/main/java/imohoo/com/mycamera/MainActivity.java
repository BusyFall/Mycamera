package imohoo.com.mycamera;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.system.ErrnoException;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import imohoo.com.mycamera.unitl.AnimatorUnitl;
import imohoo.com.mycamera.unitl.CustomOrientationEventListener;
import imohoo.com.mycamera.unitl.InterfaceOrientation;

public class MainActivity extends Activity implements View.OnClickListener, CustomOrientationEventListener.CustomOrientationDelegate, View.OnTouchListener {
    private SurfaceView surfaceView;
    private Camera mCamera;
    private Camera.Parameters parameters;
    private SurfaceHolder surfaceHolder;
    private ImageView img_icloud;
    private ImageView img_camera;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        //初始化重力传感器
        initsSensor();
        bindlistener();
    }

    private void bindlistener() {
        findViewById(R.id.btn_takephoto).setOnClickListener(this);
        findViewById(R.id.btn_duijiao).setOnClickListener(this);
        surfaceView.setOnTouchListener(this);
    }

    private void initsSensor() {
        CustomOrientationEventListener listener = new CustomOrientationEventListener(this);
        listener.enable();
        listener.setDelegate(this, null);
    }

    private void initView() {
        img_icloud = (ImageView) findViewById(R.id.img_icloud);
        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        img_camera = (ImageView) findViewById(R.id.img_camera);
        surfaceView.setOnTouchListener(this);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    mCamera = Camera.open();
                } catch (Exception e) {
                    MainActivity.this.finish();
                }
                parameters = mCamera.getParameters();
                //是否开启闪关灯
                parameters.setFlashMode("off");
                parameters.setRotation(0);
                mCamera.setParameters(parameters);
                try {
                    mCamera.setPreviewDisplay(holder);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mCamera.startPreview();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                int rotation = getDisplayOrientation();
                mCamera.setDisplayOrientation(rotation);
                Camera.Parameters parameters = mCamera.getParameters();
                parameters.setRotation(rotation);
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                mCamera.setParameters(parameters);
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                surfaceHolder.removeCallback(this);
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }
        });
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public int getDisplayOrientation() {

        android.hardware.Camera.CameraInfo camInfo =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, camInfo);


        Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int rotation = display.getRotation();
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

        int result = (camInfo.orientation - degrees + 360) % 360;
        return result;
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_takephoto:
                takePicture();
                break;
            case R.id.btn_duijiao:
                startActivity(new Intent(this, MainActivity2.class));
                break;
        }
    }

    private void takePicture() {
        mCamera.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                File pictureFile = getOutputMediaFile();
                if (pictureFile == null) {
                    return;
                }
                try {
                    FileOutputStream fos = new FileOutputStream(pictureFile);
                    fos.write(data);
                    fos.close();

                    img_camera.setImageURI(outputMediaFileUri);
                    camera.startPreview();
                } catch (FileNotFoundException e) {
                    Log.d("date", "File not found: " + e.getMessage());
                } catch (IOException e) {
                    Log.d("date", "Error accessing file: " + e.getMessage());
                }
            }
        });

    }

    @Override
    public void onOrientationChanged(InterfaceOrientation var1) {
        AnimatorUnitl.rotateAnimation(img_icloud, var1, 200);
    }

    private float oldDist = 1f;

    private static float getFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    private void handleZoom(boolean isZoomIn, Camera camera) {
        Camera.Parameters params = camera.getParameters();
        if (params.isZoomSupported()) {
            int maxZoom = params.getMaxZoom();
            int zoom = params.getZoom();
            if (isZoomIn && zoom < maxZoom) {
                zoom = zoom + 2;
            } else if (zoom > 0) {
                zoom = zoom - 2;
            }
            params.setZoom(zoom);
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            camera.setParameters(params);
        } else {

        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return false;
    }

    private Uri outputMediaFileUri;

    private File getOutputMediaFile() {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "pic");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                "IMG_" + timeStamp + ".jpg");
        outputMediaFileUri = Uri.fromFile(mediaFile);
        return mediaFile;
    }
}
