import 'dart:developer';
import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'hh_screen_recorder_platform_interface.dart';
import 'package:flutter/services.dart';

RecordOutput recordOutputFromJson(String str) =>
    RecordOutput.fromJson(json.decode(str));

String recordOutputToJson(RecordOutput data) => json.encode(data.toJson());

class RecordOutput {
  RecordOutput({
    required this.success,
    required this.file,
    required this.msg,
  });

  bool success;
  File file;
  String msg;

  factory RecordOutput.fromJson(Map<String, dynamic> json) {
    return RecordOutput(
      success: json["success"],
      file: File(json["file"]),
      msg: json["msg"],
    );
  }

  Map<String, dynamic> toJson() => {
        "success": success,
        "file": file,
        "msg": msg,
      };
}

class HhScreenRecorder {
  static const MethodChannel _channel = MethodChannel('hh_screen_recorder');

  Future<String?> getPlatformVersion() {
    return HhScreenRecorderPlatform.instance.getPlatformVersion();
  }

  Future<bool> startRecording(
      {required String filename, String? foldername, bool? recordAudio}) async {
     try{
        var response = await _channel.invokeMethod('startRecording', {
          "filename": filename,
          "foldername": foldername,
          "recordAudio": recordAudio
        });
        return response;
    }
    on Exception catch(ex)
    {
      throw Exception(ex.toString());
    }
  }

  Future<bool> stopRecording() async {
     try{
       var response = await _channel.invokeMethod('stopRecording');
      return response;
    }
    on Exception catch(ex)
    {
      throw Exception(ex.toString());
    }
  }

  Future<bool> pauseRecording() async {
     try{
      var response = await _channel.invokeMethod('pauseRecording');
      return response;
    }
    on Exception catch(ex)
    {
      throw Exception(ex.toString());
    }
  }

  Future<bool> resumeRecording() async {
    try{
      var response = await _channel.invokeMethod('resumeRecording');
      return response;
    }
    on Exception catch(ex)
    {
      throw Exception(ex.toString());
    }
  }

  // Not all Android API's support MediaRecorder stop/pause
  // Do not let the user pause if so.
  // iOS Replay Kit does not support this at all atm.
  Future<bool> isPauseResumeEnabled() async {
    try{
      var response = await _channel.invokeMethod('isPauseResumeEnabled');
      return response;
    }
    on Exception catch(ex)
    {
      throw Exception(ex.toString());
    }
  }

  // Should check for device information,
  // Whether h264 encoder & MP4 file format is supported or not.
  // True for all iOS & macOS devices HH supports.
  Future<bool> isRecordingSupported() async {
    try{
      var response = await _channel.invokeMethod('isRecordingSupported');
      return response;
    }
    on Exception catch (ex) {
      throw Exception(ex.toString());
    }
  }
}
