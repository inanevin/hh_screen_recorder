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

  Future<Map<String, dynamic>> startRecording({required String filename, String? directory}) async
  {
    var response = await _channel.invokeMethod('startRecording',{ "filename": filename, "directory": directory});
    var formatResponse = RecordOutput.fromJson(json.decode(response));
    return formatResponse.toJson();
  }

   Future<Map<String, dynamic>> stopRecording() async
  {
    var response = await _channel.invokeMethod('stopRecording');
    var formatResponse = RecordOutput.fromJson(json.decode(response));
    return formatResponse.toJson();
  }
}
