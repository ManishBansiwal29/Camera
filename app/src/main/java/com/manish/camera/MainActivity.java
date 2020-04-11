package com.manish.camera;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Button flashB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    private static final String[] PERMISSIONS ={
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
    };
    private static final int REQUEST_PERMISSIONS_CODE=1000;

    private static final int PERMISSION_COUNT=4;

    @SuppressLint("NewApi")
    private boolean arePermissionDenied(){
        for(int i=0; i<PERMISSION_COUNT;i++){
            if (checkSelfPermission(PERMISSIONS[i])!= PackageManager.PERMISSION_GRANTED){
                return true;
            }
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode==REQUEST_PERMISSIONS_CODE && grantResults.length>0){
            if (arePermissionDenied()){
                ((ActivityManager)(this.getSystemService(ACTIVITY_SERVICE))).clearApplicationUserData();
                recreate();
            }else{
                onResume();
            }
        }
    }

    private boolean isCameraInitialized = false;


    public static Camera mCamera = null;
    private static SurfaceHolder myHolder;
    private static CameraPreview mPreview;
    private FrameLayout preview;
    private static OrientationEventListener orientationEventListener=null;
    private static boolean fM;
    private int effectIndex=0;
    private static boolean isRecording;
    private static MediaRecorder recorder = null;

    private void initCam(){
        if (!whichCamera) {
            mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
        } else {
            mCamera = Camera.open();
        }
        mPreview=new CameraPreview(this,mCamera);
        preview=findViewById(R.id.cameraPreview);
        preview.addView(mPreview);
        rotateCamera();
        flashB=findViewById(R.id.flash);
        if ( hasFlash() ){
            flashB.setVisibility(View.VISIBLE);

        }else {
            flashB.setVisibility(View.GONE);
        }
        flashB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                mCamera.setParameters(p);
            }
        });
    }

    private void init(){
        initCam();
        final Button switchCamera = findViewById(R.id.switchCamera);
        switchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCamera.release();
                switchCamera();
                rotateCamera();
                try {
                    mCamera.setPreviewDisplay(myHolder);
                    mCamera.startPreview();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if(hasFlash()){
                    flashB.setVisibility(View.VISIBLE);
                    //mCamera.setParameters(p);

                }else {
                    flashB.setVisibility(View.GONE);
                }

            }
        });

        final Button effectsBtn = findViewById(R.id.effects);
        effectsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                p.setColorEffect(camEffects.get(effectIndex));
                mCamera.setParameters(p);
                effectIndex++;
                if (effectIndex == camEffects.size()){
                    effectIndex=0;
                }
            }
        });

        if (camEffects.size()==0){
            effectsBtn.setVisibility(View.GONE);
        }

        final Button takePhotoBtn = findViewById(R.id.takePhoto);
        takePhotoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mCamera.takePicture(null,null,mPicture);
                    initCam();
                }catch (Exception e){

                }
            }
        });

        final Button recordVideo = findViewById(R.id.recordVideo);
        recordVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isRecording){
                    recorder.stop();
                    releaseRecorder();
                    isRecording=false;
                    effectsBtn.setVisibility(View.VISIBLE);
                    flashB.setVisibility(View.VISIBLE);
                    switchCamera.setVisibility(View.VISIBLE);
                    takePhotoBtn.setVisibility(View.VISIBLE);
                }else if(prepareVideoRecorder()){
                    recorder.start();
                    isRecording=true;
                    effectsBtn.setVisibility(View.GONE);
                    flashB.setVisibility(View.GONE);
                    switchCamera.setVisibility(View.GONE);
                    takePhotoBtn.setVisibility(View.GONE);
                    recordVideo.setText("Stop");
                }
            }
        });

        orientationEventListener=new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int orientation) {
                rotateCamera();
            }
        };
        orientationEventListener.enable();
//        preview.setOnLongClickListener(new View.OnLongClickListener() {
//            @Override
//            public boolean onLongClick(View v) {
//                if ( whichCamera ){
//                    if ( fM ){
//                        p.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
//                    }else{
//                        p.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
//                    }
//                    try {
//                        mCamera.setParameters(p);
//                    }catch (Exception e){
//                        e.printStackTrace();
//                    }
//                    fM=!fM;
//                }
//                return true;
//            }
//        });
        preview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (whichCamera) {
                    if (fM) {
                        p.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                    } else {
                        p.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                    }
                    try {
                        mCamera.setParameters(p);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    fM = !fM;
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && arePermissionDenied()){
            requestPermissions(PERMISSIONS,REQUEST_PERMISSIONS_CODE);
            return;
        }

        if (!isCameraInitialized){
           init();
           isCameraInitialized=true;
        }else {
            initCam();
        }
        }
    private static boolean prepareVideoRecorder(){
        recorder = new MediaRecorder();
        mCamera.unlock();
        recorder.setCamera(mCamera);
        recorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        recorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
        recorder.setOutputFile(saveFile(String.valueOf(Environment.getExternalStoragePublicDirectory
                (Environment.DIRECTORY_PICTURES))) +".mp4");
        recorder.setPreviewDisplay(mPreview.getHolder().getSurface());
        recorder.setOrientationHint(rotation);
        try {
            recorder.prepare();
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }
    private static void releaseRecorder(){
        recorder.reset();
        recorder.release();
        recorder = null;
        mCamera.lock();
    }

        private Camera.PictureCallback mPicture = new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data , Camera camera) {
                try{ FileOutputStream fos = new FileOutputStream(saveFile(String.valueOf(Environment.
                        getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES))) + ".jpg");
                    fos.write(data);
                    fos.close();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        };

    private static String saveFile(String filePath){
        File f = new File(filePath);
        if (!f.exists()){
            f.mkdir();
        }
        return f+"/"+new SimpleDateFormat("MMddHHmmss").format(Calendar.getInstance().getTime());
    }

    private void switchCamera() {
        if (whichCamera) {
            mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
        } else {
            mCamera = Camera.open();
        }
        whichCamera = !whichCamera;
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
    }

    private void releaseCamera(){
         if ( mCamera != null ){
             preview.removeView(mPreview);
             mCamera.release();
             orientationEventListener.disable();
             mCamera=null;
             whichCamera = !whichCamera;
         }
        }


        private static List<String> camEffects;
        private static boolean hasFlash(){
            camEffects=p.getSupportedColorEffects();
            final List<String> flashModes = p.getSupportedFlashModes();
            if(flashModes==null){
                return false;
            }
            for (String flashMode :flashModes){
                if ( Camera.Parameters.FLASH_MODE_ON.equals(flashMode) ){
                    return true;
                }
            }
            return false;
        }

    private static boolean whichCamera = true;
    private static Camera.Parameters p;
    private static int rotation;

    private void rotateCamera() {
        if ( mCamera != null ) {
            rotation = this.getWindowManager().getDefaultDisplay().getRotation();
            if ( rotation == 0 ) {
                rotation = 90;
            } else if ( rotation == 1 ) {
                rotation = 0;
            } else if ( rotation == 2 ) {
                rotation = 270;
            } else {
                rotation = 180;
            }
            mCamera.setDisplayOrientation(rotation);
            if ( !whichCamera ){
                if ( rotation==90 ){
                    rotation=270;
                }else if ( rotation==270 ){
                    rotation=90;
                }
            }
            p=mCamera.getParameters();
            p.setRotation(rotation);
            mCamera.setParameters(p);
        }
    }

    public static class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

        private static SurfaceHolder mHolder;
        private static Camera mCamera;

        public CameraPreview(Context context , Camera camera) {
            super(context);
            mCamera = camera;
            mHolder = getHolder();
            mHolder.addCallback(this);
            //noinspection deprecation
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        public void surfaceCreated(SurfaceHolder holder) {
            myHolder = holder;
            try {
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void surfaceDestroyed(SurfaceHolder holder) {

        }

        public void surfaceChanged(SurfaceHolder holder , int format , int w , int h) {

        }
    }
}

