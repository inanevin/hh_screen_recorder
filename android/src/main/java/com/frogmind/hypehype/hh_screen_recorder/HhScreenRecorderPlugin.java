package com.frogmind.hypehype.hh_screen_recorder;

import static android.content.Context.WINDOW_SERVICE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;

import org.json.JSONObject;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.plugin.common.PluginRegistry;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
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


  private String m_filename = "";
  private boolean m_recordAudio = false;
  private String m_foldername = "";

  private static final int SCREEN_RECORD_REQUEST_CODE = 777;
  private Intent service;
  public static HhScreenRecorderPlugin _instance;
  private boolean m_canResumePause = false;
  private boolean m_awatingFlutterResult = false;
  private CodecUtility m_codecUtility = null;
  private int m_width = 0;
  private int m_height = 0;
  private int m_density = 0;
  public static final String[] MIME_TYPE_MP4= new String[]{"video/mp4", "video/mp4v", "video/mp4v-es"};
  public static final String MIME_TYPE_FALLBACK = "video/3gpp";
  public static String SELECTED_MIME_TYPE = "";
  private boolean m_isRecordingSupported = false;
  private ContentValues m_contentValues = null;
  private String m_finalFullPath = "";
  private Uri m_uri = null;

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

  public Activity getActivity()
  {
    return m_activity;
  }

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    m_channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "hh_screen_recorder");
    m_channel.setMethodCallHandler(this);
    m_flutterPluginBinding = flutterPluginBinding;
    m_context = flutterPluginBinding.getApplicationContext();
    m_projectionManager = (MediaProjectionManager) m_flutterPluginBinding
            .getApplicationContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE);

    _instance = this;
    m_canResumePause = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
    m_codecUtility = new CodecUtility();
    m_codecUtility.setContext(m_context);
    CodecUtility._instance = m_codecUtility;
    m_width = CodecUtility._instance.getMaxSupportedWidth();
    m_height = CodecUtility._instance.getMaxSupportedHeight();

    DisplayMetrics displayMetrics = new DisplayMetrics();
    WindowManager wm = (WindowManager) m_context.getSystemService(WINDOW_SERVICE);
    wm.getDefaultDisplay().getRealMetrics(displayMetrics);
    m_density = displayMetrics.densityDpi;
    checkIfRecordingIsSupported();

  }

  private void checkIfRecordingIsSupported()
  {
    boolean mp4Supp = false;
    boolean tgpSupp = m_codecUtility.isMimeTypeSupported(MIME_TYPE_FALLBACK);
    String supportedMp4Mime = "";

    for(int i = 0; i < MIME_TYPE_MP4.length; i++)
    {
      if(m_codecUtility.isMimeTypeSupported(MIME_TYPE_MP4[i]))
      {
        supportedMp4Mime = MIME_TYPE_MP4[i];
        mp4Supp = true;
        break;
      }
    }

    if(!mp4Supp && !tgpSupp)
    {
      m_isRecordingSupported = false;
      return;
    }

    SELECTED_MIME_TYPE = mp4Supp ? supportedMp4Mime : MIME_TYPE_FALLBACK;

    boolean mp4SizeSupp = mp4Supp && m_codecUtility.isSizeSupported(m_width, m_height, supportedMp4Mime);
    boolean tgpSizeSupp = m_codecUtility.isSizeSupported(m_width, m_height, MIME_TYPE_FALLBACK);

    if(!mp4SizeSupp && !tgpSizeSupp)
    {
      m_isRecordingSupported = false;
      return;
    }

    m_isRecordingSupported = true;
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    m_flutterResult = result;

    if (call.method.equals("getPlatformVersion")) {
      result.success("Android " + android.os.Build.VERSION.RELEASE);
    }
    if(call.method.equals("startRecording"))
    {
      m_awatingFlutterResult = true;
      m_filename = call.argument("filename");
      m_recordAudio = Boolean.TRUE.equals(call.argument("recordAudio"));
      m_foldername = call.argument("foldername");

      startRecording();
    }
    else if(call.method.equals("stopRecording"))
    {
      m_awatingFlutterResult = true;
      stopRecording();
    }
    else if(call.method.equals("pauseRecording"))
    {
      m_awatingFlutterResult = true;
      pauseRecording();
    }
    else if(call.method.equals("resumeRecording"))
    {
      m_awatingFlutterResult = true;
      resumeRecording();
    }
    else if(call.method.equals("isPauseResumeEnabled"))
    {
      m_flutterResult.success(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N);
    }
    else if(call.method.equals("isRecordingSupported"))
    {
      m_flutterResult.success(m_isRecordingSupported);
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
    // Map<Object, Object> dataMap = new HashMap<Object, Object>();
    // dataMap.put("filename", m_filename);
    // dataMap.put("success", success);
    // dataMap.put("msg", msg);
    // JSONObject jsonObj = new JSONObject(dataMap);
    m_flutterResult.success(success);
    System.out.println(msg);
  }

  void startRecording()
  {
    Intent permissionIntent = m_projectionManager != null
            ? m_projectionManager.createScreenCaptureIntent()
            : null;


    m_activity.startActivityForResult(permissionIntent, SCREEN_RECORD_REQUEST_CODE);

    System.out.println("HHRecorder: Start Recording -> Started permission prompt.");
  }

  void stopRecording()
  {
    if(m_recordingState == RecordingState.Recording || m_recordingState == RecordingState.Paused)
    {
      sendFlutterResult(true, "HHRecorder: Stop Recording -> Successfully stopped media recording.");
      Intent service = new Intent(m_context, ScreenCaptureService.class);
      m_context.stopService(service);
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
      service.putExtra("recordAudio", m_recordAudio);
      service.putExtra("mediaProjCode", code);
      service.putExtra("mediaProjData", data);
      service.putExtra("width", m_width);
      service.putExtra("height", m_height);
      service.putExtra("density", m_density);
      service.putExtra("foldername", m_foldername);
      checkAddContentValues();

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

    System.out.println("HHRecorder: Media recorder error! Code: " + what);

    if(m_recordingState == RecordingState.Recording)
    {
      Intent service = new Intent(m_context, ScreenCaptureService.class);
      m_context.stopService(service);
    }

    if(m_awatingFlutterResult)
    {
      sendFlutterResult(false, "Media recorder error! Code: " + what);
    }
  }

  public void onMediaRecorderInfo(int what, int i1)
  {

  }

  public void onServiceDestroyed()
  {
    // Stopped recording

    // Share intent

    //File file = new File(m_finalFullPath);
    //Uri fileUri = FileProvider.getUriForFile(HhScreenRecorderPlugin._instance.getActivity().getApplicationContext(), "com.frogmind.hypehype.hh_screen_recorder.provider", file);
    Intent send = new Intent(Intent.ACTION_SEND);
    send.putExtra(Intent.EXTRA_STREAM, m_uri);
    send.setType("video/*");
    send.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    HhScreenRecorderPlugin._instance.getActivity().startActivity(Intent.createChooser(send, "Send Recording"));
  }

  private void checkAddContentValues()
  {
    m_finalFullPath =  getActivity().getExternalFilesDir(Environment.DIRECTORY_MOVIES).getAbsolutePath() + "/" + m_foldername + "/";

    String outputExtension = "";
    if(HhScreenRecorderPlugin.SELECTED_MIME_TYPE.equals(HhScreenRecorderPlugin.MIME_TYPE_FALLBACK))
      outputExtension = ".3gp";
    else
      outputExtension = ".mp4";

    String finalFileName = m_filename + "_" + getDateAndTime();
    m_finalFullPath += finalFileName + outputExtension;
    createFolder();

    String mimeType = HhScreenRecorderPlugin.SELECTED_MIME_TYPE.equals(HhScreenRecorderPlugin.MIME_TYPE_FALLBACK) ? "video/3gpp" : "video/mp4";
    m_contentValues = new ContentValues();
    m_contentValues.put(MediaStore.Video.Media.TITLE, finalFileName);
    m_contentValues.put(MediaStore.Video.Media.DISPLAY_NAME, finalFileName);
    m_contentValues.put(MediaStore.Video.Media.DESCRIPTION, "HypeHype Screen Recorder.");
    m_contentValues.put(MediaStore.Video.Media.MIME_TYPE, mimeType);

    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
      m_contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/" + m_foldername);
    else
      m_contentValues.put(MediaStore.Video.Media.DATA, m_finalFullPath);

    ContentResolver resolver = getActivity().getApplicationContext().getContentResolver();
    m_uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, m_contentValues);

    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
    {
      service.putExtra("uri", m_uri.toString());
      m_finalFullPath = m_uri.getPath();
    }
    else
    {
      service.putExtra("fullpath", m_finalFullPath);
    }
  }

  private void createFolder() {

    File f1 = new File(m_finalFullPath);
    if (!f1.exists()) {
      if (f1.mkdirs()) {
        Log.i("Folder ", "created");
      }
    }
  }

  private String getDateAndTime(){
    @SuppressLint("SimpleDateFormat") DateFormat dfDate = new SimpleDateFormat("yyyyMMdd");
    String date=dfDate.format(Calendar.getInstance().getTime());
    @SuppressLint("SimpleDateFormat") DateFormat dfTime = new SimpleDateFormat("HHmm");
    String time = dfTime.format(Calendar.getInstance().getTime());
    return date + "-" + time;
  }

}
