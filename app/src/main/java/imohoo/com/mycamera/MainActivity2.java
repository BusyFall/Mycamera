package imohoo.com.mycamera;

import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import imohoo.com.mycamera.unitl.CameraUtils;

/**
 * Created by xcs2 on 2017/1/20.
 */

public class MainActivity2 extends Activity implements View.OnClickListener {
    private SurfaceView mSurfaceView;
    private ImageView img_break;
    private ImageView img_switchCamera;
    private Button takepicture;
    private Camera cameraInst;
    private ImageView pic_image;
    private float pointX, pointY;
    static final int FOCUS = 1;            // 聚焦
    static final int ZOOM = 2;            // 缩放
    private int mode;                      //0是聚焦 1是放大
    private float dist;
    private View focusIndex;
    private Handler handler = new Handler();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_camera);
        initView();
        bindListener();
    }

    private void initView() {
        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        img_break = (ImageView) findViewById(R.id.img_back);
        img_switchCamera = (ImageView) findViewById(R.id.img_switchCamera);
        takepicture = (Button) findViewById(R.id.takepicture);
        pic_image = (ImageView) findViewById(R.id.image);
        focusIndex=findViewById(R.id.focus_index);
    }

    private void bindListener() {
        SurfaceHolder mSurfaceHolder = mSurfaceView.getHolder();
        //连续聚焦
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        //始终保持在屏幕上
        mSurfaceHolder.setKeepScreenOn(true);
        mSurfaceView.setFocusable(true);
        mSurfaceView.setBackgroundColor(TRIM_MEMORY_BACKGROUND);
        //添加回调
        mSurfaceHolder.addCallback(new SurfaceCallback());
        takepicture.setOnClickListener(this);
        mSurfaceView.setOnClickListener(this);
        mSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    // 主点按下
                    case MotionEvent.ACTION_DOWN:
                        pointX = event.getX();
                        pointY = event.getY();
                        mode = FOCUS;
                        break;
                    // 副点按下
                    case MotionEvent.ACTION_POINTER_DOWN:
                        dist = spacing(event);
                        // 如果连续两点距离大于10，则判定为多点模式
                        if (spacing(event) > 10f) {
                            mode = ZOOM;
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_POINTER_UP:
                        mode = FOCUS;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (mode == FOCUS) {
                            //pointFocus((int) event.getRawX(), (int) event.getRawY());
                        } else if (mode == ZOOM) {
                            float newDist = spacing(event);
                            if (newDist > 10f) {
                                float tScale = (newDist - dist) / dist;
                                if (tScale < 0) {
                                    tScale = tScale * 10;
                                }
                                addZoomIn((int) tScale);
                            }
                        }
                        break;
                }
                return false;
            }
        });
    }

    /**
     * 两点的距离
     */
    private float spacing(MotionEvent event) {
        if (event == null) {
            return 0;
        }
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float)Math.sqrt(x * x + y * y);
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.takepicture:
                try {
                    cameraInst.takePicture(null, null, new MyPictureCallback());
                } catch (Throwable t) {
                    t.printStackTrace();
                    Toast.makeText(this, "拍照失败，请重试！", Toast.LENGTH_LONG).show();
                    try {
                        cameraInst.startPreview();
                    } catch (Throwable e) {

                    }
                }
                break;
            case R.id.surfaceView:
                try {
                    pointFocus((int) pointX, (int) pointY);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                RelativeLayout.LayoutParams layout = new RelativeLayout.LayoutParams(focusIndex.getLayoutParams());
                layout.setMargins((int) pointX - 60, (int) pointY - 60, 0, 0);
                focusIndex.setLayoutParams(layout);
                focusIndex.setVisibility(View.VISIBLE);
                ScaleAnimation sa = new ScaleAnimation(3f, 1f, 3f, 1f,
                        ScaleAnimation.RELATIVE_TO_SELF, 0.5f, ScaleAnimation.RELATIVE_TO_SELF, 0.5f);
                sa.setDuration(800);
                focusIndex.startAnimation(sa);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                      focusIndex.setVisibility(View.INVISIBLE);
                    }
                }, 800);
                break;

        }
    }
    //定点对焦的代码
    private void pointFocus(int x, int y) {
        cameraInst.cancelAutoFocus();
        parameters = cameraInst.getParameters();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            showPoint(x, y);
        }
        cameraInst.setParameters(parameters);
        autoFocus();
    }
    //实现自动对焦
    private void autoFocus() {
        new Thread() {
            @Override
            public void run() {
                try {
                    sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (cameraInst == null) {
                    return;
                }
                cameraInst.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                        if (success) {
                            initCamera();//实现相机的参数初始化
                        }
                    }
                });
            }
        };
    }
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void showPoint(int x, int y) {
        if (parameters.getMaxNumMeteringAreas() > 0) {
            List<Camera.Area> areas = new ArrayList<Camera.Area>();
            //xy变换了
            int rectY = -x * 2000 / getResources().getDisplayMetrics().widthPixels + 1000;
            int rectX = y * 2000 / getResources().getDisplayMetrics().heightPixels - 1000;

            int left = rectX < -900 ? -1000 : rectX - 100;
            int top = rectY < -900 ? -1000 : rectY - 100;
            int right = rectX > 900 ? 1000 : rectX + 100;
            int bottom = rectY > 900 ? 1000 : rectY + 100;
            Rect area1 = new Rect(left, top, right, bottom);
            areas.add(new Camera.Area(area1, 800));
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
            parameters.setFocusAreas(areas);
        }

    }
    //放大缩小
    int curZoomValue = 0;

    private void addZoomIn(int delta) {

        try {
            Camera.Parameters params = cameraInst.getParameters();
            Log.d("Camera", "Is support Zoom " + params.isZoomSupported());
            if (!params.isZoomSupported()) {
                return;
            }
            curZoomValue += delta;
            if (curZoomValue < 0) {
                curZoomValue = 0;
            } else if (curZoomValue > params.getMaxZoom()) {
                curZoomValue = params.getMaxZoom();
            }

            if (!params.isSmoothZoomSupported()) {
                params.setZoom(curZoomValue);
                cameraInst.setParameters(params);
                return;
            } else {
                cameraInst.startSmoothZoom(curZoomValue);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private final class MyPictureCallback implements Camera.PictureCallback {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            new SavePicTask(data).execute();
            camera.startPreview(); // 拍完照后，重新开始预览
        }
    }

    private class SavePicTask extends AsyncTask<Void, Void, String> {
        private byte[] data;

        protected void onPreExecute() {
//            showProgressDialog("处理中");
        }

        SavePicTask(byte[] data) {
            this.data = data;
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                return saveToSDCard(data);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (!TextUtils.isEmpty(result)) {
//                dismissProgressDialog();
                pic_image.setImageURI(Uri.fromFile(new File(result)));
            } else {
                Toast.makeText(MainActivity2.this, "拍照失败，请稍后重试！", Toast.LENGTH_LONG).show();
            }
        }
    }

    private final class SurfaceCallback implements SurfaceHolder.Callback {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            if (null == cameraInst) {
                try {
                    cameraInst = Camera.open();
                    cameraInst.setPreviewDisplay(holder);
                    initCamera();
                    cameraInst.startPreview();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity2.this, "相机打开失败", Toast.LENGTH_SHORT).show();
                }
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            new Thread() {
                @Override
                public void run() {
                    try {
                        sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (cameraInst == null) {
                        return;
                    }
                    cameraInst.autoFocus(new Camera.AutoFocusCallback() {
                        @Override
                        public void onAutoFocus(boolean success, Camera camera) {
                            if (success) {
                                initCamera();//实现相机的参数初始化
                            }
                        }
                    });
                }
            };
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            try {
                if (cameraInst != null) {
                    cameraInst.stopPreview();
                    cameraInst.release();
                    cameraInst = null;
                }
            } catch (Exception e) {
                //相机已经关了
            }
        }
    }

    private Camera.Size adapterSize = null;
    private Camera.Size previewSize = null;
    private Camera.Parameters parameters = null;

    private void initCamera() {
        parameters = cameraInst.getParameters();
        parameters.setPictureFormat(PixelFormat.JPEG);
        setUpPicSize(parameters);
        setUpPreviewSize(parameters);
        if (adapterSize != null) {
            parameters.setPictureSize(adapterSize.width, adapterSize.height);
        }
        if (previewSize != null) {
            parameters.setPreviewSize(previewSize.width, previewSize.height);
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);//1连续对焦
        } else {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }
        setDispaly(parameters, cameraInst);
        try {
            cameraInst.setParameters(parameters);
        } catch (Exception e) {
            e.printStackTrace();
        }
        cameraInst.startPreview();
        cameraInst.cancelAutoFocus();// 2如果要实现连续的自动对焦，这一句必须加上
    }

    private void setUpPicSize(Camera.Parameters parameters) {
        if (adapterSize != null) {
            return;
        } else {
            adapterSize = CameraUtils.findBestPictureResolution(cameraInst, this);
            return;
        }
    }

    private void setUpPreviewSize(Camera.Parameters parameters) {
        if (previewSize != null) {
            return;
        } else {
            previewSize = CameraUtils.findBestPreviewResolution(cameraInst, this);
        }
    }

    //控制图像的正确显示方向
    private void setDispaly(Camera.Parameters parameters, Camera camera) {
        if (Build.VERSION.SDK_INT >= 8) {
            CameraUtils.setDisplayOrientation(camera, 90);
        } else {
            parameters.setRotation(90);
        }
    }

    private int PHOTO_SIZE = 2000;
    private int mCurrentCameraId = 0;  //1是前置 0是后置

    /**
     * 将拍下来的照片存放在SD卡中
     *
     * @param data
     * @throws IOException
     */
    public String saveToSDCard(byte[] data) throws IOException {
        Bitmap croppedImage;

        //获得图片大小
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, options);

        PHOTO_SIZE = options.outHeight > options.outWidth ? options.outWidth : options.outHeight;
        int height = options.outHeight > options.outWidth ? options.outHeight : options.outWidth;
        options.inJustDecodeBounds = false;
        Rect r;
        if (mCurrentCameraId == 1) {
            r = new Rect(height - PHOTO_SIZE, 0, height, PHOTO_SIZE);
        } else {
            r = new Rect(0, 0, PHOTO_SIZE, PHOTO_SIZE);
        }
        try {
            croppedImage = decodeRegionCrop(data, r);
        } catch (Exception e) {
            return null;
        }
        String imagePath = CameraUtils.saveToFile(CameraUtils.getSystemPhotoPath(), true,
                croppedImage);
        croppedImage.recycle();
        return imagePath;
    }

    private Bitmap decodeRegionCrop(byte[] data, Rect rect) throws IOException {

        InputStream is = null;
        System.gc();
        Bitmap croppedImage = null;
        try {
            is = new ByteArrayInputStream(data);
            BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(is, false);

            try {
                croppedImage = decoder.decodeRegion(rect, new BitmapFactory.Options());
            } catch (IllegalArgumentException e) {
            }
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            is.close();
        }
        Matrix m = new Matrix();
        m.setRotate(90, PHOTO_SIZE / 2, PHOTO_SIZE / 2);
        if (mCurrentCameraId == 1) {
            m.postScale(1, -1);
        }
        Bitmap rotatedImage = Bitmap.createBitmap(croppedImage, 0, 0, PHOTO_SIZE, PHOTO_SIZE, m, true);
        if (rotatedImage != croppedImage)
            croppedImage.recycle();
        return rotatedImage;
    }
}
