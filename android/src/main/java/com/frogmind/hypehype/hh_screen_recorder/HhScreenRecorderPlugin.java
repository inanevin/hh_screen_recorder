package com.frogmind.hypehype.hh_screen_recorder;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import org.json.JSONObject;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.plugin.common.PluginRegistry;

import java.io.IOException;
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
  private String m_directory = "";

  private static final int SCREEN_RECORD_REQUEST_CODE = 777;
  private Intent service;
  public static HhScreenRecorderPlugin _instance;
  private boolean m_canResumePause = false;

  enum RecordingState
  {
    None,
    Recording,
    Paused
  }

  RecordingState m_recordingState;

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
    _instance = this;
    m_canResumePause = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
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
      m_directory = call.argument("directory");

      startRecording();
    }
    else if(call.method.equals("stopRecording"))
    {
      stopRecording();
    }
    else if(call.method.equals("pauseRecording"))
    {
      pauseRecording();
    }
    else if(call.method.equals("resumeRecording"))
    {
      resumeRecording();
    }
    else if(call.method.equals("isPauseResumeEnabled"))
    {
      m_flutterResult.success(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N);
    }
    else {
      result.notImplemented();
    }
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    m_channel.setMethodCallHandler(null);
  }

  void sendFlutterResult(boolean success, String msg)
  {
    Map<Object, Object> dataMap = new HashMap<Object, Object>();
    dataMap.put("filename", m_filename);
    dataMap.put("directory", m_directory);
    dataMap.put("success", success);
    dataMap.put("msg", msg);
    JSONObject jsonObj = new JSONObject(dataMap);
    m_flutterResult.success(jsonObj.toString());

    if(printLn)
      System.out.println(msg);
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

  void stopRecording()
  {
    if(m_recordingState == RecordingState.Recording || m_recordingState == RecordingState.Paused)
    {
      Intent service = new Intent(m_context, ScreenCaptureService.class);
      m_context.stopService(service);
      sendFlutterResult(true, "HHRecorder: Stop Recording -> Successfully stopped media recording.");
    }
    else
      sendFlutterResult(false, "HHRecorder: Stop Recording -> Can't stop recording as we are not capturing.");
  }

  void pauseRecording()
  {
    if(!m_canResumePause)
      sendFlutterResult(false, "HHRecorder: Pause Recording -> Can't pause recording as it's not supported with this API level.");

    if(m_recordingState == RecordingState.Recording)
    {
      Intent service = new Intent(m_context, ScreenCaptureService.class);
      service.setAction("pause");
      m_context.startService(service);
    }
    else
      sendFlutterResult(false, "HHRecorder: Pause Recording -> Can't pause recording as we are not capturing.");
  }

  void resumeRecording()
  {
    if(!m_canResumePause)
      sendFlutterResult(false, "HHRecorder: Resume Recording -> Can't resume recording as it's not supported with this API level.");

    if(m_recordingState == RecordingState.Paused)
    {
      Intent service = new Intent(m_context, ScreenCaptureService.class);
      service.setAction("resume");
      m_context.startService(service);
    }
    else
      sendFlutterResult(false, "HHRecorder: Resume Recording -> Can't resume recording as we are not paused.");
  }

  @Override
  public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == SCREEN_RECORD_REQUEST_CODE) {
      if (resultCode == Activity.RESULT_OK) {
        if (data != null) {
          startService(resultCode, data);
        }
        else
          sendFlutterResult(false, "HHRecorder: Start Recording -> Recording permission data is null, aborting.");
      }
      else
        sendFlutterResult(false, "HHRecorder: Start Recording -> Recording permission result is NOT OK, aborting.");
    }

    return true;
  }

  // ******************** SERVICE ************************

  private void startService(int code, Intent data)
  {
    try
    {
      service = new Intent(m_context, ScreenCaptureService.class);
      service.putExtra("filename", m_filename);
      service.putExtra("directory", m_directory);
      service.putExtra("data", data);
      service.putExtra("width", m_metrics.widthPixels);
      service.putExtra("height", m_metrics.heightPixels);
      service.putExtra("density", m_metrics.densityDpi);
      service.putExtra("mediaProjCode", code);
      service.putExtra("mediaProjData", data);

      System.out.println("HHRecorder: requesting to start the service");

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        m_context.startForegroundService(service);
      else
        m_context.startService(service);
    }
    catch(Exception e)
    {
      sendFlutterResult(true, "HHRecorder: Start Recording -> " + Log.getStackTraceString(e));
    }
  }

  public void onStartedCapture()
  {
    m_recordingState = RecordingState.Recording;
    sendFlutterResult(true, "HHRecorder: Start Recording -> Successfully started recording.");
  }

  public void onFailedToStartCapture(String reason)
  {
    m_recordingState = RecordingState.None;
    sendFlutterResult(true, "HHRecorder: Start Recording -> Error: " + reason);

    try {
      Intent mService = new Intent(m_context, ScreenCaptureService.class);
      m_context.stopService(mService);
    }catch (Exception e){
      // ignore
    }
  }

  public void onPausedRecording()
  {
    m_recordingState = RecordingState.Paused;
    sendFlutterResult(true, "HHRecorder: Pause Recording -> Successfully paused recording.");
  }

  public void onResumedRecording()
  {
    m_recordingState = RecordingState.Recording;
    sendFlutterResult(true, "HHRecorder: Resume Recording -> Successfully resumed recording.");
  }

  public void onMediaRecorderError(int what, int i1)
  {
    if (what == 268435556)
    {
      // too short frames.
    }
  }

  public void onMediaRecorderInfo(int what, int i1)
  {

  }

  public void onServiceDestroyed()
  {

  }


}
