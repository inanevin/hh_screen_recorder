import 'package:flutter_test/flutter_test.dart';
import 'package:hh_screen_recorder/hh_screen_recorder.dart';
import 'package:hh_screen_recorder/hh_screen_recorder_platform_interface.dart';
import 'package:hh_screen_recorder/hh_screen_recorder_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockHhScreenRecorderPlatform
    with MockPlatformInterfaceMixin
    implements HhScreenRecorderPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final HhScreenRecorderPlatform initialPlatform = HhScreenRecorderPlatform.instance;

  test('$MethodChannelHhScreenRecorder is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelHhScreenRecorder>());
  });

  test('getPlatformVersion', () async {
    HhScreenRecorder hhScreenRecorderPlugin = HhScreenRecorder();
    MockHhScreenRecorderPlatform fakePlatform = MockHhScreenRecorderPlatform();
    HhScreenRecorderPlatform.instance = fakePlatform;

    expect(await hhScreenRecorderPlugin.getPlatformVersion(), '42');
  });
}
