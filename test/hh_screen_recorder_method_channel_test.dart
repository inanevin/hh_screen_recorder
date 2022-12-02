import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:hh_screen_recorder/hh_screen_recorder_method_channel.dart';

void main() {
  MethodChannelHhScreenRecorder platform = MethodChannelHhScreenRecorder();
  const MethodChannel channel = MethodChannel('hh_screen_recorder');

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  test('getPlatformVersion', () async {
    expect(await platform.getPlatformVersion(), '42');
  });
}
