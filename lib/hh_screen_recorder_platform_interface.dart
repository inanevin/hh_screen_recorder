import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'hh_screen_recorder_method_channel.dart';

abstract class HhScreenRecorderPlatform extends PlatformInterface {
  /// Constructs a HhScreenRecorderPlatform.
  HhScreenRecorderPlatform() : super(token: _token);

  static final Object _token = Object();

  static HhScreenRecorderPlatform _instance = MethodChannelHhScreenRecorder();

  /// The default instance of [HhScreenRecorderPlatform] to use.
  ///
  /// Defaults to [MethodChannelHhScreenRecorder].
  static HhScreenRecorderPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [HhScreenRecorderPlatform] when
  /// they register themselves.
  static set instance(HhScreenRecorderPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }
}
