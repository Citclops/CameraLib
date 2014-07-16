package com.citclops.cameralibrary;

import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CitclopsCameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private final String TAG = "Preview";
	private SurfaceHolder mHolder;
    private Camera mCamera;
    int cameraIdOpened = -1;				// Index de la càmera oberta
    Size mPreviewSize;
    List<Size> mSupportedPreviewSizes;
    Context _this;
    CameraCallback callback;
    
    @SuppressWarnings("deprecation")
	public CitclopsCameraPreview(Context context) {
        super(context);
        _this = context;

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }
    
    public void setCamera(Camera camera, int CameraIdOpened) {
        mCamera = camera;
        cameraIdOpened = CameraIdOpened;
        if (mCamera != null) {
            mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
            requestLayout();
        }
    }
	/*
	 * Estableix els callbacks del takepicture
	 */
    public void setCallback(CameraCallback callback){
        this.callback = callback;
    }	
    /*
     * Cridat quan el surface es creat
     */
    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {
                    if(null != callback) callback.onPreviewFrame(data, camera);
                }
            });
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }    
    /*
     * Cridat quan el Surface es destruit
     */
    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return, so stop the preview.
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }
    /*
     * Rearrnca el preview
     */
    public void restartPreview(){
        if (mHolder.getSurface() == null){
          // preview surface does not exist
          return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e){
          // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or reformatting changes here
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);  
        
        // Actualitzem la orientacio
        setCameraDisplayOrientation();

        // Actualitzem
        requestLayout();
        
        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
        } catch (Exception e){
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }    	
    }
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {    	
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.
    	restartPreview();
    }
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // We purposely disregard child measurements because act as a
        // wrapper to a SurfaceView that centers the camera preview instead
        // of stretching it.
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        setMeasuredDimension(width, height);

        if (mSupportedPreviewSizes != null) {
            mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width, height);
        }
    }
	 /*
	  * Gets Optimal Sizes
	  */
	 private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
	        final double ASPECT_TOLERANCE = 0.1;
	        double targetRatio = (double) w / h;
	        if (sizes == null) return null;

	        Size optimalSize = null;
	        double minDiff = Double.MAX_VALUE;

	        int targetHeight = h;

	        // Try to find an size match aspect ratio and size
	        for (Size size : sizes) {
	            double ratio = (double) size.width / size.height;
	            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
	            if (Math.abs(size.height - targetHeight) < minDiff) {
	                optimalSize = size;
	                minDiff = Math.abs(size.height - targetHeight);
	            }
	        }

	        // Cannot find the one match the aspect ratio, ignore the requirement
	        if (optimalSize == null) {
	            minDiff = Double.MAX_VALUE;
	            for (Size size : sizes) {
	                if (Math.abs(size.height - targetHeight) < minDiff) {
	                    optimalSize = size;
	                    minDiff = Math.abs(size.height - targetHeight);
	                }
	            }
	        }
	        return optimalSize;
	    }
	/*
    /*
     * Actualitza la orientacio del preview en funcio de la posicio del dispositiu
     */
    private void setCameraDisplayOrientation() {
        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraIdOpened, info);
        int rotation = ((Activity)_this).getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        mCamera.setDisplayOrientation(result);
    }
}
