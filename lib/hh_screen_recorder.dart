
import 'hh_screen_recorder_platform_interface.dart';

class HhScreenRecorder {
  Future<String?> getPlatformVersion() {
    return HhScreenRecorderPlatform.instance.getPlatformVersion();
  }
}
