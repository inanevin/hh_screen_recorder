package com.frogmind.hypehype.hh_screen_recorder;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.plugin.common.PluginRegistry;

import java.util.HashMap;
import java.util.Map;

/** HhScreenRecorderPlugin */
public class HhScreenRecorderPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware, PluginRegistry.ActivityResultListener{
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private MethodChannel m_channel;
  private Result m_flutterResult;
  private FlutterPluginBinding m_flutterPluginBinding;
  private Context m_context;
  private Activity m_activity;
  private MediaProjectionManager m_projectionManager;
  private MediaProjection m_captureProjection;
  private VirtualDisplay m_virtualDisplay;
  private DisplayMetrics m_metrics;
  private MediaRecorder m_mediaRecorder;
  private boolean printLn = true;

  private String m_filename = "";

  private static final int SCREEN_RECORD_REQUEST_CODE = 777;
  private boolean m_isCapturing = false;

  @Override
  public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
    m_activity = binding.getActivity();
    m_metrics = new DisplayMetrics();
    m_activity.getWindowManager().getDefaultDisplay().getMetrics(m_metrics);
    binding.addActivityResultListener(this);
  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {

  }

  @Override
  public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {

  }

  @Override
  public void onDetachedFromActivity() {

  }

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    m_channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "hh_screen_recorder");
    m_channel.setMethodCallHandler(this);
    m_flutterPluginBinding = flutterPluginBinding;
    m_context = flutterPluginBinding.getApplicationContext();
    m_projectionManager = (MediaProjectionManager) m_flutterPluginBinding
            .getApplicationContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE);

    m_mediaRecorder = new MediaRecorder();
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    m_flutterResult = result;

    if (call.method.equals("getPlatformVersion")) {
      result.success("Android " + android.os.Build.VERSION.RELEASE);
    }
    if(call.method.equals("startRecording"))
    {
      m_filename = call.argument("filename");
      startRecording();
    }
    else if(call.method.equals("stopRecording"))
    {
      stopRecording();
    } else {
      result.notImplemented();
    }
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    m_channel.setMethodCallHandler(null);
  }

  void startRecording()
  {
    Intent permissionIntent = m_projectionManager != null
            ? m_projectionManager.createScreenCaptureIntent()
            : null;
    m_activity.startActivityForResult(permissionIntent, SCREEN_RECORD_REQUEST_CODE);
    if(printLn)
      System.out.println("HHRecorder: Start Recording -> Started permission prompt.");
  }

  @Override
  public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
    Map<Object, Object> dataMap = new HashMap<Object, Object>();
    dataMap.put("file", "");

    System.out.println("HHRecorder: DEBUG ON ACTIVITY RESULT");

    if (requestCode == SCREEN_RECORD_REQUEST_CODE) {
      if (resultCode == Activity.RESULT_OK) {
        if (data != null) {

          dataMap.put("success", true);
          dataMap.put("msg", "HHRecorder: Start Recording -> Started capturing.");

          initRecorder();
          m_isCapturing = true;
          m_captureProjection = m_projectionManager.getMediaProjection(resultCode, data);
          m_virtualDisplay = m_captureProjection.createVirtualDisplay("HHCapture", m_metrics.widthPixels, m_metrics.heightPixels, m_metrics.densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, m_mediaRecorder.getSurface(), null, null);
          m_mediaRecorder.start();

          if(printLn)
            System.out.println("HHRecorder: Start Recording -> Started capturing screen.");
        }
        else
        {
          dataMap.put("success", false);
          dataMap.put("msg", "HHRecorder: Start Recording -> Recording permission data is null, aborting.");

          if(printLn)
            System.out.println("HHRecorder: Start Recording -> Recording permission data is null, aborting.");
        }
      }
      else
      {
        dataMap.put("success", false);
        dataMap.put("msg", "HHRecorder: Start Recording -> Recording permission result is NOT OK, aborting.");

        if(printLn)
          System.out.println("HHRecorder: Start Recording -> Recording permission result is NOT OK, aborting.");
      }
    }

    JSONObject jsonObj = new JSONObject(dataMap);
    m_flutterResult.success(jsonObj.toString());
    return true;
  }

  void stopRecording()
  {
    Map<Object, Object> dataMap = new HashMap<Object, Object>();
    dataMap.put("file", "");

    if(!m_isCapturing)
    {
      dataMap.put("success", false);
      dataMap.put("msg", "HHRecorder: Stop Recording -> Can't stop recording as we are not capturing.");
      JSONObject jsonObj = new JSONObject(dataMap);
      m_flutterResult.success(jsonObj.toString());

      if(printLn)
        System.out.println("HHRecorder: Stop Recording -> Can't stop recording as we are not capturing.");
    }

    m_mediaRecorder.stop();

    dataMap.put("success", false);
    dataMap.put("msg", "HHRecorder: Stop Recording -> Successfully stopped recording.");
    JSONObject jsonObj = new JSONObject(dataMap);
    m_flutterResult.success(jsonObj.toString());

    if(printLn)
      System.out.println("HHRecorder: Stop Recording -> Successfully stopped recording.");
  }

  private void initRecorder()
  {
    //m_mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
    m_mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
    m_mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
    m_mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
    m_mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
    m_mediaRecorder.setVideoEncodingBitRate(512 * 1000);
    m_mediaRecorder.setVideoFrameRate(30);
    m_mediaRecorder.setVideoSize(m_metrics.widthPixels, m_metrics.heightPixels);
    m_mediaRecorder.setOutputFile("data/com.frogmind.hypehype/" + m_filename + ".mp4");
  }

}
