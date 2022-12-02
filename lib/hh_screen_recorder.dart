import 'dart:developer';

import 'hh_screen_recorder_platform_interface.dart';
import 'package:flutter/services.dart';


class HhScreenRecorder {

    static const MethodChannel _channel = MethodChannel('hh_screen_recorder');

  Future<String?> getPlatformVersion() {
    return HhScreenRecorderPlatform.instance.getPlatformVersion();
  }

  void startRecording({required String filename, String? directory}) async
  {
    var response = await _channel.invokeMethod('startRecording',{ "filename": filename, "directory": directory});
    log("Got response!");
  }
}
