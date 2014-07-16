package com.citclops.cameralibrary;


import java.io.IOException;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;

import com.citclops.cameralibrary.CameraConstants.ANTIBANDING;
import com.citclops.cameralibrary.CameraConstants.COLOR_EFFECT;
import com.citclops.cameralibrary.CameraConstants.FLASH_MODE;
import com.citclops.cameralibrary.CameraConstants.FOCUS_MODE;
import com.citclops.cameralibrary.CameraConstants.IMAGEFORMAT;
import com.citclops.cameralibrary.CameraConstants.SCENE;
import com.citclops.cameralibrary.CameraConstants.WHITE_BALANCE;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Camera.Size;
import android.media.MediaRecorder;
import android.os.Handler;


public class CitclopsCameraLibrary {
	private Camera mCamera = null;								// Objecte Càmera
    private int numberOfCameras;								// Nº de càmeres del dispositiu
    private int cameraIdOpened = -1;							// Index de la càmera oberta
    private CitclopsCameraPreview mPreview = null;				// Per mostrar el preview
    private Context _this;										// Referencia self
    private CameraCallback callback = null;						// Callbacks de takepicutre de la camera
    private Handler handlerVideoEnd = null;						// Handler to control end video
    private Handler handlerVideoTorch = null;					// Handler to Start Torch in Video
	private Handler handlerTorch = null;						// Handler control torch    
    private List<COLOR_EFFECT> supportedColorEffects = null;	// Llista de efectes de colors permesos
    private List<Size> supportedPicureSizes = null;				// Llista de mides permeses
    private List<WHITE_BALANCE> supportedWhiteBalances = null;	// LLista de White Balances permesos
    private List<ANTIBANDING> supportedAntibanding = null;		// LLista de Antibanding permesos
    private List<SCENE> supportedScene = null;
    private List<FOCUS_MODE> supportedFocus = null;
    public MediaRecorder mrec = new MediaRecorder();			// For record video
    private int millisecondsTorchVideo = 0;						// Milliseconds to open torch in video capture
    private int VideoHeightReport = 0;							// Video Height
    private int VideoWidthReport = 0;							// Video Width
    endMeasureVideoListener mEndMeasureVideoListener = null;	// Listener End Video Capture
       
    /*
     * Constructor
     */
    public CitclopsCameraLibrary (Context context){
    	// Obtenim la referencioa local
    	_this = context;
    	
		// Obtenim el nº de càmeras
        numberOfCameras = Camera.getNumberOfCameras();

    	// Creem el preview
    	mPreview = new CitclopsCameraPreview(_this);
    }    
    /******************************************************/
    /***** M è t o d e s       P u b l i c s **************/
    /******************************************************/
    /*
     * Sets de End Video Measure Listener
     */
    public void setEndMeasureVideoListener(endMeasureVideoListener eventListener) {
    	mEndMeasureVideoListener = eventListener;
    }
	/*
	 * Obre la Càmera de manera segura
	 */
	public boolean openCamera() {
		int idCameraDefault = getDefaultCameraIndex();
		return openCamera(idCameraDefault);
	}
	/*
	 * Obre la Càmera de manera segura
	 */
	public boolean openCamera(int id) {
	    boolean qOpened = false;	  
	    try {	    	
	        releaseCameraAndPreview();
	        mCamera = Camera.open(id);
	        qOpened = (mCamera != null);
	        if (qOpened) {
	        	// Desem el index de càmera oberta
	        	cameraIdOpened = id;
	        	
	        	// L'assignem al preview
	            mPreview.setCamera(mCamera, cameraIdOpened);
	        }
	    } catch (Exception e) {
	    	// impossible obrir la càmera
	    	qOpened = false;
	        e.printStackTrace();
	    }
	    return qOpened;    
	}
	/*
	 * Retona el nº de càmera oberta
	 */
	public int getCameraId(){
		return cameraIdOpened;
	}
	/*
	 * Allibera la càmera i el preview
	 */
	public void releaseCameraAndPreview() {
		if (mPreview != null) mPreview.setCamera(null, -1);
	    if (mCamera != null) {
	    	mCamera.stopPreview();
	    	mCamera.setPreviewCallback(null);
	        mCamera.release();
	        mCamera = null;
	    }
	}	
	/*
	 * Obtè la llista dels efectes de color suportats
	 */
	public List<COLOR_EFFECT> getSupportedColorEffects(){
		if (supportedColorEffects == null) mGetSupportedColorEfects();
        return supportedColorEffects;
    }
	
	/*
	 * Obtè la llista dels modes de focus suportats
	 */
	public List<FOCUS_MODE> getSupportedFocusMode(){
		if (supportedFocus == null) mGetSupportedFocusMode();
		return supportedFocus;
	}
		
     /*
      * Obtè la llista dels White Balances suportats
      */
    public List<WHITE_BALANCE> getSupportedWhiteBalances(){
		if (supportedWhiteBalances == null) mGetSupportedWhiteBalances();
        return supportedWhiteBalances;
    }    
    /*
     * Obtè la llista dels Antibanding suportats
     */
    public List<ANTIBANDING> getSupportedAntibanding(){
    	if (supportedAntibanding == null) mGetSupportedAntibanding();
    	return supportedAntibanding;
    }
    /*
     * Obtè la llista dels possibles tamanys de les imatges 
     */
    public List<Size> getSupportedPicureSizes(){
    	if (supportedPicureSizes == null) supportedPicureSizes = mCamera.getParameters().getSupportedPictureSizes();
    	return supportedPicureSizes;
    }
    /*
     * Obtè la llista dels Scene suportats
     */
   public List<SCENE> getSupportedScene(){
		if (supportedScene == null) mGetSupportedScene();
       return supportedScene;
   }       
   /*
    * Obtè el White Balance Actual
   */
   public WHITE_BALANCE getCurrentWhiteBalance(){
	   WHITE_BALANCE retorn = WHITE_BALANCE.UNKNOWN;
	   String tmpCurrentWhiteBalance = "";
	   if (mCamera.getParameters().getWhiteBalance() != null) tmpCurrentWhiteBalance = mCamera.getParameters().getWhiteBalance().trim();
   		if (tmpCurrentWhiteBalance.equalsIgnoreCase(Camera.Parameters.WHITE_BALANCE_AUTO)) 					retorn = WHITE_BALANCE.AUTO;
	   	else if (tmpCurrentWhiteBalance.equalsIgnoreCase(Camera.Parameters.WHITE_BALANCE_CLOUDY_DAYLIGHT)) 	retorn = WHITE_BALANCE.CLOUDY_DAYLIGHT;
	   	else if (tmpCurrentWhiteBalance.equalsIgnoreCase(Camera.Parameters.WHITE_BALANCE_DAYLIGHT)) 		retorn = WHITE_BALANCE.DAYLIGHT;
	   	else if (tmpCurrentWhiteBalance.equalsIgnoreCase(Camera.Parameters.WHITE_BALANCE_FLUORESCENT)) 		retorn = WHITE_BALANCE.FLUORESCENT;
	   	else if (tmpCurrentWhiteBalance.equalsIgnoreCase(Camera.Parameters.WHITE_BALANCE_INCANDESCENT)) 	retorn = WHITE_BALANCE.INCANDESCENT;
	   	else if (tmpCurrentWhiteBalance.equalsIgnoreCase(Camera.Parameters.WHITE_BALANCE_SHADE)) 			retorn = WHITE_BALANCE.SHADE;
	   	else if (tmpCurrentWhiteBalance.equalsIgnoreCase(Camera.Parameters.WHITE_BALANCE_TWILIGHT)) 		retorn = WHITE_BALANCE.TWILIGHT;
	   	else if (tmpCurrentWhiteBalance.equalsIgnoreCase(Camera.Parameters.WHITE_BALANCE_WARM_FLUORESCENT)) retorn = WHITE_BALANCE.WARM_FLUORESCENT;
	   return retorn;
   }
   /*
    * Obtè el Color Effect Actual
    */
  public COLOR_EFFECT getCurrentColorEffect(){
	  COLOR_EFFECT retorn = COLOR_EFFECT.UNKNOWN;
	  String tmpCurrentColorEffect = "";
	  if (mCamera.getParameters().getColorEffect() != null) tmpCurrentColorEffect = mCamera.getParameters().getColorEffect().trim();
	  if (tmpCurrentColorEffect.equalsIgnoreCase(Camera.Parameters.EFFECT_NONE)) 			retorn = COLOR_EFFECT.NONE;
	  else if (tmpCurrentColorEffect.equalsIgnoreCase(Camera.Parameters.EFFECT_AQUA)) 		retorn = COLOR_EFFECT.AQUA;
	  else if (tmpCurrentColorEffect.equalsIgnoreCase(Camera.Parameters.EFFECT_BLACKBOARD)) retorn = COLOR_EFFECT.BLACKBOARD;
	  else if (tmpCurrentColorEffect.equalsIgnoreCase(Camera.Parameters.EFFECT_MONO)) 		retorn = COLOR_EFFECT.MONO;
	  else if (tmpCurrentColorEffect.equalsIgnoreCase(Camera.Parameters.EFFECT_NEGATIVE)) 	retorn = COLOR_EFFECT.NEGATIVE;
	  else if (tmpCurrentColorEffect.equalsIgnoreCase(Camera.Parameters.EFFECT_POSTERIZE)) 	retorn = COLOR_EFFECT.POSTERIZE;
	  else if (tmpCurrentColorEffect.equalsIgnoreCase(Camera.Parameters.EFFECT_SEPIA)) 		retorn = COLOR_EFFECT.SEPIA;
	  else if (tmpCurrentColorEffect.equalsIgnoreCase(Camera.Parameters.EFFECT_SOLARIZE)) 	retorn = COLOR_EFFECT.SOLARIZE;
	  else if (tmpCurrentColorEffect.equalsIgnoreCase(Camera.Parameters.EFFECT_WHITEBOARD)) retorn = COLOR_EFFECT.WHITEBOARD;
	  return retorn;
  }
  
  /*
   * Obtè el FOCUS MODE Actual
   */  
  public FOCUS_MODE getCurrentFocusMode(){
	  FOCUS_MODE retorn = FOCUS_MODE.UNKNOWN;
	  String tmpCurrentFocusMode = "";
	  if (mCamera.getParameters().getFocusMode() != null) tmpCurrentFocusMode = mCamera.getParameters().getFocusMode().trim();
	  if (tmpCurrentFocusMode.equalsIgnoreCase(Camera.Parameters.FOCUS_MODE_AUTO))			              retorn = FOCUS_MODE.FOCUS_AUTO;
	  else if (tmpCurrentFocusMode.equalsIgnoreCase(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))     retorn = FOCUS_MODE.FOCUS_CONTINUOUS_PICTURE;
	  else if (tmpCurrentFocusMode.equalsIgnoreCase(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))       retorn = FOCUS_MODE.FOCUS_CONTINUOUS_VID;
	  else if (tmpCurrentFocusMode.equalsIgnoreCase(Camera.Parameters.FOCUS_MODE_EDOF))     			  retorn = FOCUS_MODE.FOCUS_EDOF;
	  else if (tmpCurrentFocusMode.equalsIgnoreCase(Camera.Parameters.FOCUS_MODE_FIXED))     			  retorn = FOCUS_MODE.FOCUS_FIXED;
	  else if (tmpCurrentFocusMode.equalsIgnoreCase(Camera.Parameters.FOCUS_MODE_INFINITY))     		  retorn = FOCUS_MODE.FOCUS_INFINITY;
	  else if (tmpCurrentFocusMode.equalsIgnoreCase(Camera.Parameters.FOCUS_MODE_MACRO))     			  retorn = FOCUS_MODE.FOCUS_MACRO;
	  return retorn;
  }
  
  /*
   * Obtè el Antibanding Actual
   */  
  public ANTIBANDING getCurrentAntibanding(){
	  ANTIBANDING retorn = ANTIBANDING.UNKNOWN;
	  String tmpCurrentAntibanding = "";
	  if (mCamera.getParameters().getAntibanding() != null) tmpCurrentAntibanding = mCamera.getParameters().getAntibanding().trim();
	  if (tmpCurrentAntibanding.equalsIgnoreCase(Camera.Parameters.ANTIBANDING_OFF)) 			retorn = ANTIBANDING.OFF;
	  else if (tmpCurrentAntibanding.equalsIgnoreCase(Camera.Parameters.ANTIBANDING_AUTO)) 		retorn = ANTIBANDING.AUTO;
	  else if (tmpCurrentAntibanding.equalsIgnoreCase(Camera.Parameters.ANTIBANDING_50HZ)) 		retorn = ANTIBANDING.FIFTY;
	  else if (tmpCurrentAntibanding.equalsIgnoreCase(Camera.Parameters.ANTIBANDING_60HZ)) 		retorn = ANTIBANDING.SIXTY;
	  return retorn;
  }
  /*
   * Obtè el Scene Actual
   */
  public SCENE getCurrentScene(){
	   SCENE retorn = SCENE.UNKNOWN;
	   String tmpCurrentScene = "";
	   if (mCamera.getParameters().getSceneMode() != null) tmpCurrentScene = mCamera.getParameters().getSceneMode().trim();
	   if (tmpCurrentScene.equalsIgnoreCase(Camera.Parameters.SCENE_MODE_AUTO)) 				retorn = SCENE.MODE_AUTO;
	   	else if (tmpCurrentScene.equalsIgnoreCase(Camera.Parameters.SCENE_MODE_ACTION)) 		retorn = SCENE.MODE_ACTION;
	   	else if (tmpCurrentScene.equalsIgnoreCase(Camera.Parameters.SCENE_MODE_BARCODE)) 		retorn = SCENE.MODE_BARCODE;
	   	else if (tmpCurrentScene.equalsIgnoreCase(Camera.Parameters.SCENE_MODE_BEACH)) 			retorn = SCENE.MODE_BEACH;
	   	else if (tmpCurrentScene.equalsIgnoreCase(Camera.Parameters.SCENE_MODE_CANDLELIGHT)) 	retorn = SCENE.MODE_CANDLELIGHT;
	   	else if (tmpCurrentScene.equalsIgnoreCase(Camera.Parameters.SCENE_MODE_FIREWORKS)) 		retorn = SCENE.MODE_FIREWORKS;
	   	else if (tmpCurrentScene.equalsIgnoreCase(Camera.Parameters.SCENE_MODE_LANDSCAPE)) 		retorn = SCENE.MODE_LANDSCAPE;
	   	else if (tmpCurrentScene.equalsIgnoreCase(Camera.Parameters.SCENE_MODE_NIGHT)) 			retorn = SCENE.MODE_NIGHT;
	   	else if (tmpCurrentScene.equalsIgnoreCase(Camera.Parameters.SCENE_MODE_NIGHT_PORTRAIT)) retorn = SCENE.MODE_NIGHT_PORTRAIT;
	   	else if (tmpCurrentScene.equalsIgnoreCase(Camera.Parameters.SCENE_MODE_PARTY)) 			retorn = SCENE.MODE_PARTY;
	   	else if (tmpCurrentScene.equalsIgnoreCase(Camera.Parameters.SCENE_MODE_PORTRAIT)) 		retorn = SCENE.MODE_PORTRAIT;
	   	else if (tmpCurrentScene.equalsIgnoreCase(Camera.Parameters.SCENE_MODE_SNOW)) 			retorn = SCENE.MODE_SNOW;
	   	else if (tmpCurrentScene.equalsIgnoreCase(Camera.Parameters.SCENE_MODE_SPORTS)) 		retorn = SCENE.MODE_SPORTS;
	   	else if (tmpCurrentScene.equalsIgnoreCase(Camera.Parameters.SCENE_MODE_STEADYPHOTO)) 	retorn = SCENE.MODE_STEADYPHOTO;
	   	else if (tmpCurrentScene.equalsIgnoreCase(Camera.Parameters.SCENE_MODE_SUNSET)) 		retorn = SCENE.MODE_SUNSET;
	   	else if (tmpCurrentScene.equalsIgnoreCase(Camera.Parameters.SCENE_MODE_THEATRE)) 		retorn = SCENE.MODE_THEATRE;
	   return retorn;
	 }
	 /*
	 * Obtè el valor de la qualitat/compressio del jpeg
	 */	 
	 public int getCurrentJpegQuality(){
		 return mCamera.getParameters().getJpegQuality();
	 }
	 /*
	 * Obtè el Picture Format Actual
	 */	 
	 public IMAGEFORMAT getPictureFormat(){
		 IMAGEFORMAT retorn = IMAGEFORMAT.UNKNOWN;
		 int actualPictureFormat = mCamera.getParameters().getPictureFormat();
		 switch (actualPictureFormat){
			 case ImageFormat.JPEG:
				 retorn = IMAGEFORMAT.JPEG;
				 break;
			 case ImageFormat.NV16:
				 retorn = IMAGEFORMAT.NV16;
				 break;
			 case ImageFormat.NV21:
				 retorn = IMAGEFORMAT.NV21;
				 break;
			 case ImageFormat.RGB_565:
				 retorn = IMAGEFORMAT.RGB_565;
				 break;
			 case ImageFormat.UNKNOWN:
				 retorn = IMAGEFORMAT.UNKNOWN;
				 break;
			 case ImageFormat.YUY2:
				 retorn = IMAGEFORMAT.YUY2;
				 break;
			 case ImageFormat.YV12:
				 retorn = IMAGEFORMAT.YV12;
				 break;
		 }
		 return retorn;
	 }
	/*
	* Obtè si el size de la Imatge 
	*/	 
	 public Size getCurrentPictureSize(){		 
		 return mCamera.getParameters().getPictureSize();	 
	 }
	/*
	* Obtè el valor de zoom actual 
	*/	 
	 public int getCurrentZoom(){
		 return mCamera.getParameters().getZoom();		 
	 }
	/*
	* Obtè el valor màxim de zoom permes 
	*/	 
	 public int getMaxZoom(){
		 return mCamera.getParameters().getMaxZoom();		 
	 }
 	/*
	* Obtè si el dispositiu té càmera 
	*/	
	public static boolean checkCameraHardware(Context context) {
	    if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
	        // El dispositiu te càmera
	        return true;
	    } else {
	        // El dispositiu NO té càmera
	        return false;
	    }
	}
	/*
	 * Obtè el suface del preview
	 */
	public CitclopsCameraPreview getSurfacePreview(){
		return mPreview;
	}	
	/*
	 * Estableix els callbacks del takepicture
	 */
    public void setCallback(CameraCallback callback){
        this.callback = callback;
        if (mPreview != null) mPreview.setCallback(callback);
    }
	/*
	 * Mètode per fer la foto
	 */
	public void takePicture() {
		takePicture(false, false, false);
	}
	/*
	 * Mètode per fer la foto
	 */
	public void darkMeasurementPicture() throws Exception {		
		// Fem foto
		takePicture(true, false, false);
    }
	/*
	 * Activa el mode Torch
	 */
	public void setTorchOn() throws Exception{
		setFlashMode(FLASH_MODE.TORCH);
	}
	/*
	 * Desactiva el mode Torch
	 */
	public void setTorchOff() throws Exception{
		setFlashMode(FLASH_MODE.OFF);
	}
	/*
	 * Activa el mode Torch els milisegons indicats
	 */
	public synchronized void setTorchMilliseconds(int milliseconds) throws Exception{
		if (handlerTorch == null){
			// Creem el Handler
			handlerTorch = new Handler();
			
			// Encenem el torch
			setFlashMode(FLASH_MODE.TORCH);
			
			// Cridem a apagar-lo un cop passats els milisegons
			handlerTorch.postDelayed(mRunnableTorchOff, milliseconds);
		}
	}
	/*
	 * Estableix el balanç de blancs
	 */	
	public void setWhiteBalance(WHITE_BALANCE value) throws Exception{		
		Parameters parameters = mCamera.getParameters();		
		switch (value) {
		case AUTO:
			parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
			break;			
		case CLOUDY_DAYLIGHT:
			parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_CLOUDY_DAYLIGHT);
			break;		
		case DAYLIGHT:
			parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_DAYLIGHT);
			break;			
		case FLUORESCENT:
			parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_FLUORESCENT);
			break;			
		case INCANDESCENT:
			parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_INCANDESCENT);
			break;
		case SHADE:
			parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_SHADE);
			break;
		case TWILIGHT:
			parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_TWILIGHT);
			break;
		case WARM_FLUORESCENT:
			parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_WARM_FLUORESCENT);
			break;
		}
		// Apliquem els canvis
		setParameters(parameters);
	}	
	/*
	 * Estableix el zoom de la càmera
	 */	
	public void setCameraZoom(int value) throws Exception{
		Parameters parameters = mCamera.getParameters();
		parameters.setZoom(Math.max(0, Math.min(parameters.getMaxZoom(), value)));
		setParameters(parameters);
	}	
	/*
	 * Estableix el mode de flash
	 */	
	public void setFlashMode(FLASH_MODE value) throws Exception{		
		Parameters parameters = mCamera.getParameters();		
		switch (value) {
		case AUTO:
			parameters.setFlashMode(Parameters.FLASH_MODE_AUTO);
			break;			
		case OFF:
			parameters.setFlashMode(Parameters.FLASH_MODE_OFF);		
			break;		
		case ON:
			parameters.setFlashMode(Parameters.FLASH_MODE_ON);		
			break;			
		case RED_EYE:
			parameters.setFlashMode(Parameters.FLASH_MODE_RED_EYE);		
			break;			
		case TORCH:
			parameters.setFlashMode(Parameters.FLASH_MODE_TORCH);		
			break;
		}
		
		// Apliquem els canvis
		setParameters(parameters);
	}		
	 /*
	 * Esatbleix el valor de la qualitat/compressio del jpeg
	 */	 
	 public void setCurrentJpegQuality(int quality) throws Exception{
		Parameters parameters = mCamera.getParameters();	
		parameters.setJpegQuality(Math.min(Math.max(0, quality), 100));
		setParameters(parameters);
	 }
	 /*
	 * Estableix el Picture Format Actual
	 */	 
	 public void setPictureFormat(IMAGEFORMAT imageFormat) throws Exception{
		 Parameters parameters = mCamera.getParameters();		
		 switch (imageFormat){
			 case JPEG:
				 parameters.setPictureFormat(ImageFormat.JPEG);
				 break;
			 case NV16:
				 parameters.setPictureFormat(ImageFormat.NV16);
				 break;
			 case NV21:
				 parameters.setPictureFormat(ImageFormat.NV21);
				 break;
			 case RGB_565:
				 parameters.setPictureFormat(ImageFormat.RGB_565);
				 break;
			 case UNKNOWN:
				 parameters.setPictureFormat(ImageFormat.UNKNOWN);
				 break;
			 case YUY2:
				 parameters.setPictureFormat(ImageFormat.YUY2);
				 break;
			 case YV12:
				 parameters.setPictureFormat(ImageFormat.YV12);
				 break;
		 }
		// Apliquem els canvis
		setParameters(parameters);
	 }
	 /*
	  * Gets Optimal Sizes
	  */
	 private Size getOptimalPictureSize(List<Size> sizes, int w, int h) {
	    	Size retorn = null;
	    	long minimalValue = 999999;
	    	long screenPixels = w * h;
	    	for (int i = 0; i < sizes.size(); i++){
    			long tmpValue = Math.abs((sizes.get(i).width * sizes.get(i).height) - screenPixels);
				if (tmpValue < minimalValue){
        			retorn = sizes.get(i);
        			minimalValue = tmpValue;
				}	    			
	    	}
	    	return retorn;
	    }
	/*
	* Obtè si el size de la Imatge 
	*/	 
	 public void setPictureSizeFitScreen(int screenWidth, int screenHeight) throws Exception{
		 // Canviem les mides 
		 Parameters parameters = mCamera.getParameters();
		 List<Camera.Size> lSizePS = parameters.getSupportedPictureSizes();
		 Camera.Size optimalSize = getOptimalPictureSize(lSizePS, screenWidth, screenHeight);
		 if (optimalSize != null) parameters.setPictureSize(optimalSize.width, optimalSize.height);

		// Apliquem els canvis
		setParameters(parameters);
	 }
	/*
	* Obtè si el size de la Imatge 
	*/	 
	 public void setPictureSize(int width, int height) throws Exception{
		 // Canviem les mides 
		 Parameters parameters = mCamera.getParameters();		 
		 parameters.setPictureSize(width, height);
		 
		// Apliquem els canvis
		setParameters(parameters);
	 }
	/*
	* Sets the preview Camera Size 
	*/	 
	 public void setPreviewSize(int width, int height) throws Exception{
		 // Canviem les mides 
		 Parameters parameters = mCamera.getParameters();		 
		 parameters.setPreviewSize(width, height);
		 
		// Apliquem els canvis
		setParameters(parameters);
	 }
	/*
	 * Estableix el effect Color
	 */	
	public void setColorEffect(COLOR_EFFECT value) throws Exception{		
		Parameters parameters = mCamera.getParameters();		
		switch (value) {
		case NONE:
			parameters.setColorEffect(Parameters.EFFECT_NONE);
			break;			
		case AQUA:
			parameters.setColorEffect(Parameters.EFFECT_AQUA);		
			break;		
		case BLACKBOARD:
			parameters.setColorEffect(Parameters.EFFECT_BLACKBOARD);		
			break;			
		case MONO:
			parameters.setColorEffect(Parameters.EFFECT_MONO);		
			break;			
		case NEGATIVE:
			parameters.setColorEffect(Parameters.EFFECT_NEGATIVE);		
			break;
		case POSTERIZE:
			parameters.setColorEffect(Parameters.EFFECT_POSTERIZE);		
			break;
		case SEPIA:
			parameters.setColorEffect(Parameters.EFFECT_SEPIA);		
			break;
		case SOLARIZE:
			parameters.setColorEffect(Parameters.EFFECT_SOLARIZE);		
			break;
		case WHITEBOARD:
			parameters.setColorEffect(Parameters.EFFECT_WHITEBOARD);		
			break;
		}		
		// Apliquem els canvis
		setParameters(parameters);
	}	
	/*
	 * Estableix el FocusMode
	 */	
	public void setFocusMode(FOCUS_MODE value) throws Exception{		
		Parameters parameters = mCamera.getParameters();
		boolean bSetParameter = false;		
		getSupportedFocusMode();
		if (supportedFocus != null){
			for (int i = 0; i < supportedFocus.size(); i++){
				if (supportedFocus.get(i).equals(value)){
					bSetParameter = true;
					break;					
				}
			}
		}
		if (bSetParameter){		
			switch (value) {
				case FOCUS_AUTO:
					parameters.setFocusMode(Parameters.FOCUS_MODE_AUTO);
					break;
				case FOCUS_CONTINUOUS_PICTURE:
					parameters.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
					break;
				case FOCUS_CONTINUOUS_VID:
					parameters.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
					break;
				case FOCUS_EDOF:
					parameters.setFocusMode(Parameters.FOCUS_MODE_EDOF);
					break;
				case FOCUS_FIXED:
					parameters.setFocusMode(Parameters.FOCUS_MODE_FIXED);
					break;
				case FOCUS_INFINITY:
					parameters.setFocusMode(Parameters.FOCUS_MODE_INFINITY);
					break;
				case FOCUS_MACRO:
					parameters.setFocusMode(Parameters.FOCUS_MODE_MACRO);
					break;
			}
			setParameters(parameters);
		}
	}
	/*
	 * Estableix el Antibanding
	 */	
	public void setAntibanding(ANTIBANDING value) throws Exception{		
		Parameters parameters = mCamera.getParameters();		
		switch (value) {
		case OFF:
			parameters.setAntibanding(Parameters.ANTIBANDING_OFF);		
			break;			
		case AUTO:
			parameters.setAntibanding(Parameters.ANTIBANDING_AUTO);		
			break;		
		case FIFTY:
			parameters.setAntibanding(Parameters.ANTIBANDING_50HZ);	
			break;			
		case SIXTY:
			parameters.setAntibanding(Parameters.ANTIBANDING_60HZ);	
			break;			
		}		
		// Apliquem els canvis
		setParameters(parameters);
	}
	/*
	 * Estableix el mode de Scene
	 */	
	public void setScene(SCENE value) throws Exception{		
		Parameters parameters = mCamera.getParameters();		
		switch (value) {
		case MODE_AUTO:
			parameters.setSceneMode(Parameters.SCENE_MODE_AUTO);		
			break;			
		case MODE_ACTION:
			parameters.setSceneMode(Parameters.SCENE_MODE_ACTION);		
			break;		
		case MODE_BARCODE:
			parameters.setSceneMode(Parameters.SCENE_MODE_BARCODE);		
			break;			
		case MODE_BEACH:
			parameters.setSceneMode(Parameters.SCENE_MODE_BEACH);		
			break;			
		case MODE_CANDLELIGHT:
			parameters.setFlashMode(Parameters.SCENE_MODE_CANDLELIGHT);		
			break;
		case MODE_FIREWORKS:
			parameters.setSceneMode(Parameters.SCENE_MODE_FIREWORKS);		
			break;
		case MODE_LANDSCAPE:
			parameters.setSceneMode(Parameters.SCENE_MODE_LANDSCAPE);		
			break;
		case MODE_NIGHT:
			parameters.setSceneMode(Parameters.SCENE_MODE_NIGHT);		
			break;
		case MODE_NIGHT_PORTRAIT:
			parameters.setSceneMode(Parameters.SCENE_MODE_NIGHT_PORTRAIT);		
			break;
		case MODE_PARTY:
			parameters.setSceneMode(Parameters.SCENE_MODE_PARTY);		
			break;
		case MODE_PORTRAIT:
			parameters.setSceneMode(Parameters.SCENE_MODE_PORTRAIT);		
			break;
		case MODE_SNOW:
			parameters.setSceneMode(Parameters.SCENE_MODE_SNOW);		
			break;
		case MODE_SPORTS:
			parameters.setSceneMode(Parameters.SCENE_MODE_SPORTS);		
			break;
		case MODE_STEADYPHOTO:
			parameters.setSceneMode(Parameters.SCENE_MODE_STEADYPHOTO);		
			break;
		case MODE_SUNSET:
			parameters.setSceneMode(Parameters.SCENE_MODE_SUNSET);		
			break;
		case MODE_THEATRE:
			parameters.setSceneMode(Parameters.SCENE_MODE_THEATRE);		
			break;
		}
		
		// Apliquem els canvis
		setParameters(parameters);
	}
	/*
	 * Obtè el valor del exposure compensation
	 */
	public int getExposureCompensation(){
		return mCamera.getParameters().getExposureCompensation();		
	}
	/*
	 * Estableix si el AutoExposure està bloquejat
	 */
	public void setExposureCompensation(int valor) throws Exception{	
		Parameters parameters = mCamera.getParameters();
		parameters.setExposureCompensation(valor);
		// Apliquem els canvis
		setParameters(parameters);
	}
	/*
	 * Obtè si el AutoExposure està bloquejat
	 */
	public boolean getAutoExposureLock(){		
		return mCamera.getParameters().getAutoExposureLock();	
	}
	/*
	 * Estableix si el AutoExposure està bloquejat
	 */
	public void setAutoExposureLock(boolean locked) throws Exception{	
		Parameters parameters = mCamera.getParameters();
		parameters.setAutoExposureLock(locked);
		// Apliquem els canvis
		setParameters(parameters);
	}
	/*
	 * Obtè si el AutoWhiteBalance està bloquejat
	 */
	public boolean getAutoWhiteBalanceLock(){		
		return mCamera.getParameters().getAutoWhiteBalanceLock();
	}
	/*
	 * Estableix si el AutoWhiteBalance està bloquejat
	 */
	public void setAutoWhiteBalanceLock(boolean locked) throws Exception{	
		Parameters parameters = mCamera.getParameters();
		parameters.setAutoWhiteBalanceLock(locked);
		// Apliquem els canvis
		setParameters(parameters);
	}
	/*
	 * Gets the Camera Supoorted Sizes
	 */
	public List<Size> getSupportedPreviewSizes(){
		return mCamera.getParameters().getSupportedPreviewSizes();
	}
    /******************************/
    /********* V i d e o **********/
	/******************************/
    /**
     * @throws IOException 
     * **/
	public void SaveMeasurementVideo(int beforeMilliseconds, int flashMilliseconds, int afterMilliseconds, int videoWidth, int videoHeight, String fileOutputName) throws IllegalStateException, IOException{
		
		// Gets and Save the Milliseconds for open torch
		millisecondsTorchVideo = flashMilliseconds;
		VideoWidthReport = videoWidth;					
		VideoHeightReport = videoHeight;	

		// Gets the suported Focus Mode
		getSupportedFocusMode();
		
		// Prepare Camera
		boolean bSetParameters = false;
		Camera.Parameters param = mCamera.getParameters();
				
		// Get supprted preview fps range
		int minFPSRange = 0;	int maxFPSRange = 0;	int sumFPSRange = 0;
        List<int[]> listSupportedPreviewFPS = param.getSupportedPreviewFpsRange();
        if (listSupportedPreviewFPS != null){
            for (int i = 0; i < listSupportedPreviewFPS.size(); i++){
            	if ((listSupportedPreviewFPS.get(i)[Camera.Parameters.PREVIEW_FPS_MIN_INDEX] + listSupportedPreviewFPS.get(i)[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]) > sumFPSRange){
            		minFPSRange = listSupportedPreviewFPS.get(i)[Camera.Parameters.PREVIEW_FPS_MIN_INDEX];
            		maxFPSRange = listSupportedPreviewFPS.get(i)[Camera.Parameters.PREVIEW_FPS_MAX_INDEX];
            		sumFPSRange = minFPSRange + maxFPSRange;
            	}
            }     
            bSetParameters = true;
            param.setPreviewFpsRange(minFPSRange, maxFPSRange);
        }
        
        // Gets if we can set Focus Mode		
		if (supportedFocus != null){
			for (int i = 0; i < supportedFocus.size(); i++){
				if (supportedFocus.get(i).equals(FOCUS_MODE.FOCUS_CONTINUOUS_VID)){
					bSetParameters = true;
					param.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
					break;					
				}
			}
		}
        
        // Set the parameters
        if (bSetParameters) mCamera.setParameters(param);        	
        
        
        mrec = new MediaRecorder();  // Works well
        mCamera.unlock();

        mrec.setCamera(mCamera);
        mrec.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mrec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mrec.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);
        mrec.setOrientationHint(90);
        mrec.setVideoSize(videoWidth, videoHeight);

        if (fileOutputName.length() > 0){
        	// Set the file output
            mrec.setOutputFile(fileOutputName); 

            // Start Recording
            mrec.prepare();
            mrec.start();
             
            // Prepare ends video
            if (handlerVideoTorch == null){
    			// Creem el Handler
            	handlerVideoTorch = new Handler();
    			
    			// Cridem a apagar-lo un cop passats els milisegons
            	handlerVideoTorch.postDelayed(mRunnableVideoTorch, beforeMilliseconds);
    		}
            
            // Prepare ends video
            if (handlerVideoEnd == null){
    			// Creem el Handler
            	handlerVideoEnd = new Handler();
    			
    			// Cridem a apagar-lo un cop passats els milisegons
            	handlerVideoEnd.postDelayed(mRunnableVideoEnd, beforeMilliseconds + flashMilliseconds + afterMilliseconds);
    		}
        } 
        
        // Prepare ends video
        if (handlerVideoEnd == null){
			// Creem el Handler
        	handlerVideoEnd = new Handler();
			
			// Cridem a apagar-lo un cop passats els milisegons
        	handlerVideoEnd.postDelayed(mRunnableVideoEnd, beforeMilliseconds + flashMilliseconds + afterMilliseconds);
		}
	}
    /****************************************/
    /***** I n t e r f a c e s **************/
    /****************************************/
	public interface endMeasureVideoListener extends EventListener{
		public void endMeasureVideo (int flashDuration, int VideoWidth, int VideoHeight);
	}
    /******************************************************/
    /***** M è t o d e s       P r i v a t s **************/
    /******************************************************/
	/*
	 * Obtè la càmera per defecte
	 */
	private int getDefaultCameraIndex(){
		int retorn = -1;		
        // Find the ID of the default camera
        CameraInfo cameraInfo = new CameraInfo();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {
            	retorn = i;
            	break;
            }
        }
		return retorn;
	}
	/*
	 * Runnbale que para la linterna
	 */
	private Runnable mRunnableTorchOff = new Runnable() {		
		@Override
		public void run() {
			// Parem el torch
			try {				
				setFlashMode(FLASH_MODE.OFF);
			} catch (Exception e) {
				// No hem pogut apagar la torcha en el Runnable. 
				// Ho haurà de fer l'usuari
			}			
			// Alliberem el Handler
			handlerTorch = null;
		}
	};
	/*
	 * Runnbale que para la linterna
	 */
	private Runnable mRunnableVideoEnd = new Runnable() {		
		@Override
		public void run() {
			// Parem el video
			try {	
				if (mrec != null){
					mrec.stop();
					mrec.release();
					mrec = null;
				}
							
				// Avisem del final del video
				if (mEndMeasureVideoListener != null) mEndMeasureVideoListener.endMeasureVideo(millisecondsTorchVideo, VideoWidthReport, VideoHeightReport);
		
			} catch (Exception e) {
				// Error al parar el video
			}			
			// Alliberem el Handler
			handlerVideoEnd = null;			
		}
	};
	/*
	 * Runnbale que para la linterna
	 */
	private Runnable mRunnableVideoTorch = new Runnable() {		
		@Override
		public void run() {
			// Parem el video
			try {	
				if (millisecondsTorchVideo > 0) setTorchMilliseconds(millisecondsTorchVideo);
			} catch (Exception e) {
				// Error al para el video
			}			
			// Alliberem el Handler
			handlerVideoTorch = null;			
		}
	};
	/*
	 * Mètode per fer la foto
	 */
	public void takePictureMillisecondsFlash(int milliseconds) throws Exception {
		if (handlerTorch == null){
			// Creem el Handler
			handlerTorch = new Handler();
			
			// Encenem el torch
			setFlashMode(FLASH_MODE.TORCH);
			
			takePicture(false, true, true);
		}
	}
	/*
	 * Mètode per fer la foto
	 */
	private void takePicture(boolean bDarkMeasurement, boolean bOpenFlash, boolean bCloseFlash) {
		final boolean bDrk = bDarkMeasurement;
		final boolean bClsFlash = bCloseFlash;
		
		// Realitzem la foto
        mCamera.takePicture(
            new ShutterCallback() {
                @Override
                public void onShutter(){
                    if(null != callback) callback.onShutter();
                }
            }, null, 
            new PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera camera){                        
                    // Apaguem el flash si és necessari
                    if ((handlerTorch != null)&& (bClsFlash))  {
                    	handlerTorch.postAtFrontOfQueue(mRunnableTorchOff);
                    }
                    
                    // Desem la foto
                    if(null != callback) callback.onJpegPictureTaken(data, camera, bDrk);
                }
            });
    }
	/*
	 * Obtè la llista de Parametres suportats
	 */
	private void mGetSupportedColorEfects(){
        final List<String> coloreffects = mCamera.getParameters().getSupportedColorEffects();
        supportedColorEffects = new ArrayList<COLOR_EFFECT>();
        if (coloreffects != null){
	        for (int i = 0; i < coloreffects.size(); i++){
	        	if (coloreffects.get(i).trim().equalsIgnoreCase(Camera.Parameters.EFFECT_NONE)) 			supportedColorEffects.add(COLOR_EFFECT.NONE);
	        	else if (coloreffects.get(i).trim().equalsIgnoreCase(Camera.Parameters.EFFECT_AQUA)) 		supportedColorEffects.add(COLOR_EFFECT.AQUA);
	        	else if (coloreffects.get(i).trim().equalsIgnoreCase(Camera.Parameters.EFFECT_BLACKBOARD)) 	supportedColorEffects.add(COLOR_EFFECT.BLACKBOARD);
	        	else if (coloreffects.get(i).trim().equalsIgnoreCase(Camera.Parameters.EFFECT_MONO)) 		supportedColorEffects.add(COLOR_EFFECT.MONO);
	        	else if (coloreffects.get(i).trim().equalsIgnoreCase(Camera.Parameters.EFFECT_NEGATIVE)) 	supportedColorEffects.add(COLOR_EFFECT.NEGATIVE);
	        	else if (coloreffects.get(i).trim().equalsIgnoreCase(Camera.Parameters.EFFECT_POSTERIZE)) 	supportedColorEffects.add(COLOR_EFFECT.POSTERIZE);
	        	else if (coloreffects.get(i).trim().equalsIgnoreCase(Camera.Parameters.EFFECT_SEPIA)) 		supportedColorEffects.add(COLOR_EFFECT.SEPIA);
	        	else if (coloreffects.get(i).trim().equalsIgnoreCase(Camera.Parameters.EFFECT_SOLARIZE)) 	supportedColorEffects.add(COLOR_EFFECT.SOLARIZE);
	        	else if (coloreffects.get(i).trim().equalsIgnoreCase(Camera.Parameters.EFFECT_WHITEBOARD)) 	supportedColorEffects.add(COLOR_EFFECT.WHITEBOARD);
	        }
        }
	}
	/*
	 * Obtè la llista de Parametres suportats
	 */	
	private void mGetSupportedFocusMode(){
		final List<String> focusmode = mCamera.getParameters().getSupportedFocusModes();
		supportedFocus = new ArrayList<FOCUS_MODE>();
		if (focusmode != null){
			for (int i = 0; i < focusmode.size(); i++){
				if (focusmode.get(i).trim().equalsIgnoreCase(Camera.Parameters.FOCUS_MODE_AUTO))					 supportedFocus.add(FOCUS_MODE.FOCUS_AUTO);
				else if (focusmode.get(i).trim().equalsIgnoreCase(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))	 supportedFocus.add(FOCUS_MODE.FOCUS_CONTINUOUS_PICTURE);
				else if (focusmode.get(i).trim().equalsIgnoreCase(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))	 supportedFocus.add(FOCUS_MODE.FOCUS_CONTINUOUS_VID);
				else if (focusmode.get(i).trim().equalsIgnoreCase(Camera.Parameters.FOCUS_MODE_EDOF))	 			 supportedFocus.add(FOCUS_MODE.FOCUS_EDOF);
				else if (focusmode.get(i).trim().equalsIgnoreCase(Camera.Parameters.FOCUS_MODE_FIXED))	 			 supportedFocus.add(FOCUS_MODE.FOCUS_FIXED);
				else if (focusmode.get(i).trim().equalsIgnoreCase(Camera.Parameters.FOCUS_MODE_INFINITY))	 		 supportedFocus.add(FOCUS_MODE.FOCUS_INFINITY);
				else if (focusmode.get(i).trim().equalsIgnoreCase(Camera.Parameters.FOCUS_MODE_MACRO))	 			 supportedFocus.add(FOCUS_MODE.FOCUS_MACRO);
			}
		}
	}
	
	/*
	 * Obtè la llista de Parametres suportats
	 */	
	private void mGetSupportedAntibanding(){
        final List<String> antibanding = mCamera.getParameters().getSupportedAntibanding();
        supportedAntibanding = new ArrayList<ANTIBANDING>();
        if (antibanding != null){
	        for (int i = 0; i < antibanding.size(); i++){
	        	if (antibanding.get(i).trim().equalsIgnoreCase(Camera.Parameters.ANTIBANDING_OFF)) 			supportedAntibanding.add(ANTIBANDING.OFF);
	        	else if (antibanding.get(i).trim().equalsIgnoreCase(Camera.Parameters.ANTIBANDING_AUTO)) 		supportedAntibanding.add(ANTIBANDING.AUTO);
	        	else if (antibanding.get(i).trim().equalsIgnoreCase(Camera.Parameters.ANTIBANDING_50HZ)) 	supportedAntibanding.add(ANTIBANDING.FIFTY);
	        	else if (antibanding.get(i).trim().equalsIgnoreCase(Camera.Parameters.ANTIBANDING_60HZ)) 		supportedAntibanding.add(ANTIBANDING.SIXTY);
	        }
        }
	}	
	/*
	 * Obtè la llista de Parametres suportats
	 */
	private void mGetSupportedScene(){
        final List<String> scene = mCamera.getParameters().getSupportedSceneModes();
        supportedScene = new ArrayList<SCENE>();
        if (scene != null){
	        for (int i = 0; i < scene.size(); i++){
	        	if (scene.get(i).trim().equalsIgnoreCase(Camera.Parameters.SCENE_MODE_AUTO)) 					supportedScene.add(SCENE.MODE_AUTO);
	        	else if (scene.get(i).trim().equalsIgnoreCase(Camera.Parameters.SCENE_MODE_ACTION)) 			supportedScene.add(SCENE.MODE_ACTION);
	        	else if (scene.get(i).trim().equalsIgnoreCase(Camera.Parameters.SCENE_MODE_BARCODE)) 			supportedScene.add(SCENE.MODE_BARCODE);
	        	else if (scene.get(i).trim().equalsIgnoreCase(Camera.Parameters.SCENE_MODE_BEACH)) 				supportedScene.add(SCENE.MODE_BEACH);
	        	else if (scene.get(i).trim().equalsIgnoreCase(Camera.Parameters.SCENE_MODE_CANDLELIGHT)) 		supportedScene.add(SCENE.MODE_CANDLELIGHT);
	        	else if (scene.get(i).trim().equalsIgnoreCase(Camera.Parameters.SCENE_MODE_FIREWORKS)) 			supportedScene.add(SCENE.MODE_FIREWORKS);
	        	else if (scene.get(i).trim().equalsIgnoreCase(Camera.Parameters.SCENE_MODE_LANDSCAPE)) 			supportedScene.add(SCENE.MODE_LANDSCAPE);
	        	else if (scene.get(i).trim().equalsIgnoreCase(Camera.Parameters.SCENE_MODE_NIGHT)) 				supportedScene.add(SCENE.MODE_NIGHT);
	        	else if (scene.get(i).trim().equalsIgnoreCase(Camera.Parameters.SCENE_MODE_NIGHT_PORTRAIT)) 	supportedScene.add(SCENE.MODE_NIGHT_PORTRAIT);
	        	else if (scene.get(i).trim().equalsIgnoreCase(Camera.Parameters.SCENE_MODE_PARTY)) 				supportedScene.add(SCENE.MODE_PARTY);
	        	else if (scene.get(i).trim().equalsIgnoreCase(Camera.Parameters.SCENE_MODE_PORTRAIT)) 			supportedScene.add(SCENE.MODE_PORTRAIT);
	        	else if (scene.get(i).trim().equalsIgnoreCase(Camera.Parameters.SCENE_MODE_SNOW)) 				supportedScene.add(SCENE.MODE_SNOW);
	        	else if (scene.get(i).trim().equalsIgnoreCase(Camera.Parameters.SCENE_MODE_SPORTS)) 			supportedScene.add(SCENE.MODE_SPORTS);
	        	else if (scene.get(i).trim().equalsIgnoreCase(Camera.Parameters.SCENE_MODE_STEADYPHOTO)) 		supportedScene.add(SCENE.MODE_STEADYPHOTO);
	        	else if (scene.get(i).trim().equalsIgnoreCase(Camera.Parameters.SCENE_MODE_SUNSET)) 			supportedScene.add(SCENE.MODE_SUNSET);
	        	else if (scene.get(i).trim().equalsIgnoreCase(Camera.Parameters.SCENE_MODE_THEATRE)) 			supportedScene.add(SCENE.MODE_THEATRE);
	        }
        }
	}
		
	/*
	 * Obtè la llista de Parametres suportats
	 */
	private void mGetSupportedWhiteBalances(){
        final List<String> whiteBalances = mCamera.getParameters().getSupportedWhiteBalance();
        supportedWhiteBalances = new ArrayList<WHITE_BALANCE>();
        if (whiteBalances != null){
	        for (int i = 0; i < whiteBalances.size(); i++){
	        	if (whiteBalances.get(i).trim().equalsIgnoreCase(Camera.Parameters.WHITE_BALANCE_AUTO)) 					supportedWhiteBalances.add(WHITE_BALANCE.AUTO);
	        	else if (whiteBalances.get(i).trim().equalsIgnoreCase(Camera.Parameters.WHITE_BALANCE_CLOUDY_DAYLIGHT)) 	supportedWhiteBalances.add(WHITE_BALANCE.CLOUDY_DAYLIGHT);
	        	else if (whiteBalances.get(i).trim().equalsIgnoreCase(Camera.Parameters.WHITE_BALANCE_DAYLIGHT)) 			supportedWhiteBalances.add(WHITE_BALANCE.DAYLIGHT);
	        	else if (whiteBalances.get(i).trim().equalsIgnoreCase(Camera.Parameters.WHITE_BALANCE_FLUORESCENT)) 		supportedWhiteBalances.add(WHITE_BALANCE.FLUORESCENT);
	        	else if (whiteBalances.get(i).trim().equalsIgnoreCase(Camera.Parameters.WHITE_BALANCE_INCANDESCENT)) 		supportedWhiteBalances.add(WHITE_BALANCE.INCANDESCENT);
	        	else if (whiteBalances.get(i).trim().equalsIgnoreCase(Camera.Parameters.WHITE_BALANCE_SHADE)) 				supportedWhiteBalances.add(WHITE_BALANCE.SHADE);
	        	else if (whiteBalances.get(i).trim().equalsIgnoreCase(Camera.Parameters.WHITE_BALANCE_TWILIGHT)) 			supportedWhiteBalances.add(WHITE_BALANCE.TWILIGHT);
	        	else if (whiteBalances.get(i).trim().equalsIgnoreCase(Camera.Parameters.WHITE_BALANCE_WARM_FLUORESCENT)) 	supportedWhiteBalances.add(WHITE_BALANCE.WARM_FLUORESCENT);
	        }
        }
	}
	/*
	 * Actualitza els paràmetrs i controla si hi ha hagut error 
	 */
	private void setParameters(Parameters parameters) throws Exception{
		try{
			mCamera.setParameters(parameters);
		} catch (Exception e){
			// Hi ha hagut error en el paràmetre. Reiniciem preview
			mPreview.restartPreview();
			
			// Tirem excepcio cap amunt
			throw e;
		} finally{
			
		}		
	}
	public String getVideoParametersString(int flashDuration, int VideoWidth, int VideoHeight) throws Exception{
		String retorn = "";
		Parameters parameters;
		try{
			// Obtenim els paràmetres
			parameters = mCamera.getParameters();
			int[] fpsrange = new int[2];
			parameters.getPreviewFpsRange(fpsrange);  
			
			// Serialitzem els paràmetres
			retorn += "<Parameters>\n";
			retorn += "<PreviewFpsMin>" + fpsrange[Camera.Parameters.PREVIEW_FPS_MIN_INDEX] + "</PreviewFpsMin>\n";
			retorn += "<PreviewFpsMax>" + fpsrange[Camera.Parameters.PREVIEW_FPS_MAX_INDEX] + "</PreviewFpsMax>\n";
			retorn += "<VideoSizeWidth>" + VideoWidth + "</VideoSizeWidth>\n";
			retorn += "<VideoSizeHeight>" + VideoHeight + "</VideoSizeHeight>\n";
			retorn += "<TorchMilliseconds>" + flashDuration + "</TorchMilliseconds>\n";
			retorn += "<Antibanding>" + parameters.getAntibanding() + "</Antibanding>\n";
			retorn += "<AutoExposureLock>" + parameters.getAutoExposureLock() + "</AutoExposureLock>\n";
			retorn += "<AutoWhiteBalanceLock>" + parameters.getAutoWhiteBalanceLock() + "</AutoWhiteBalanceLock>\n";
			retorn += "<ColorEffect>" + parameters.getColorEffect() + "</ColorEffect>\n";
			retorn += "<ExposureCompensation>" + parameters.getExposureCompensation() + "</ExposureCompensation>\n";
			retorn += "<FlashMode>" + parameters.getFlashMode() + "</FlashMode>\n";
			retorn += "<FocalLength>" + parameters.getFocalLength() + "</FocalLength>\n";
			retorn += "<FocusMode>" + parameters.getFocusMode() + "</FocusMode>\n";
			retorn += "<HorizontalViewAngle>" + parameters.getHorizontalViewAngle() + "</HorizontalViewAngle>\n";
			retorn += "<Scene>" + parameters.getSceneMode() + "</Scene>\n";
			retorn += "<VerticalViewAngle>" + parameters.getVerticalViewAngle() + "</VerticalViewAngle>\n";
			retorn += "<WhiteBalance>" + parameters.getWhiteBalance() + "</WhiteBalance>\n";
			retorn += "<Zoom>" + parameters.getZoom() + "</Zoom>\n";
			retorn += "</Parameters>\n";			
		} catch (Exception e){
			// Tirem excepcio cap amunt
			throw e;
		} finally{
			
		}	
		return retorn;		
	}
	/*
	 * Obtè els valors dels paràmetres de la foto
	 */
	public String getParametersString() throws Exception{
		String retorn = "";
		Parameters parameters;
		try{
			// Obtenim els paràmetres
			parameters = mCamera.getParameters();

			// Serialitzem els paràmetres
			retorn += "<Parameters>\n";
			retorn += "<Antibanding>" + parameters.getAntibanding() + "</Antibanding>\n";
			retorn += "<AutoExposureLock>" + parameters.getAutoExposureLock() + "</AutoExposureLock>\n";
			retorn += "<AutoWhiteBalanceLock>" + parameters.getAutoWhiteBalanceLock() + "</AutoWhiteBalanceLock>\n";
			retorn += "<ColorEffect>" + parameters.getColorEffect() + "</ColorEffect>\n";
			retorn += "<ExposureCompensation>" + parameters.getExposureCompensation() + "</ExposureCompensation>\n";
			retorn += "<FlashMode>" + parameters.getFlashMode() + "</FlashMode>\n";
			retorn += "<FocalLength>" + parameters.getFocalLength() + "</FocalLength>\n";
			retorn += "<FocusMode>" + parameters.getFocusMode() + "</FocusMode>\n";
			retorn += "<HorizontalViewAngle>" + parameters.getHorizontalViewAngle() + "</HorizontalViewAngle>\n";
			retorn += "<JpegQuality>" + parameters.getJpegQuality() + "</JpegQuality>\n";
			retorn += "<PictureFormat>" + parameters.getPictureFormat() + "</PictureFormat>\n";
			retorn += "<PictureSize>" + parameters.getPictureSize().width + "x" + parameters.getPictureSize().height + "</PictureSize>\n";
			retorn += "<Scene>" + parameters.getSceneMode() + "</Scene>\n";
			retorn += "<VerticalViewAngle>" + parameters.getVerticalViewAngle() + "</VerticalViewAngle>\n";
			retorn += "<WhiteBalance>" + parameters.getWhiteBalance() + "</WhiteBalance>\n";
			retorn += "<Zoom>" + parameters.getZoom() + "</Zoom>\n";
			retorn += "</Parameters>\n";			
		} catch (Exception e){
			// Tirem excepcio cap amunt
			throw e;
		} finally{
			
		}	
		return retorn;		
	}
	
	public Parameters getCameraParameters(){
		return mCamera.getParameters();
	}
}
