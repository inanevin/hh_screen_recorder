package com.frogmind.hypehype.hh_screen_recorder;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
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

import java.util.HashMap;
import java.util.Map;

/** HhScreenRecorderPlugin */
public class HhScreenRecorderPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware{
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private MethodChannel m_channel;
  private Result m_flutterResult;
  private FlutterPluginBinding m_flutterPluginBinding;
  private Context m_context;
  private Activity m_activity;
  private static final int SCREEN_RECORD_REQUEST_CODE = 777;

  @Override
  public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
    m_activity = binding.getActivity();
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
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    m_flutterResult = result;

    if (call.method.equals("getPlatformVersion")) {
      result.success("Android " + android.os.Build.VERSION.RELEASE);
    }
    if(call.method.equals("startRecording"))
    {
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
     // System.out.println("Inan android: start recording");
    MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) m_flutterPluginBinding
            .getApplicationContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE);

    Intent permissionIntent = mediaProjectionManager != null
            ? mediaProjectionManager.createScreenCaptureIntent()
            : null;

    m_activity.startActivityForResult(permissionIntent, SCREEN_RECORD_REQUEST_CODE);

    //  Map<Object, Object> dataMap = new HashMap<Object, Object>();
    //  dataMap.put("success", false);
    //  dataMap.put("file", "");
    //  dataMap.put("msg", "Can not start capturing because Android API version is smaller than LOLLIPOP ;).");
    //  JSONObject jsonObj = new JSONObject(dataMap);
    //  m_flutterResult.success(jsonObj.toString());

  }

  void stopRecording()
  {

  }

}
