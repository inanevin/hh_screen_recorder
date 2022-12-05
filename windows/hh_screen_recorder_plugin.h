#ifndef FLUTTER_PLUGIN_HH_SCREEN_RECORDER_PLUGIN_H_
#define FLUTTER_PLUGIN_HH_SCREEN_RECORDER_PLUGIN_H_

#include <flutter/method_channel.h>
#include <flutter/plugin_registrar_windows.h>

#include <memory>

namespace hh_screen_recorder {

class HhScreenRecorderPlugin : public flutter::Plugin {
 public:
  static void RegisterWithRegistrar(flutter::PluginRegistrarWindows *registrar);

  HhScreenRecorderPlugin();

  virtual ~HhScreenRecorderPlugin();

  // Disallow copy and assign.
  HhScreenRecorderPlugin(const HhScreenRecorderPlugin&) = delete;
  HhScreenRecorderPlugin& operator=(const HhScreenRecorderPlugin&) = delete;

 private:
  // Called when a method is called on this plugin's channel from Dart.
  void HandleMethodCall(
      const flutter::MethodCall<flutter::EncodableValue> &method_call,
      std::unique_ptr<flutter::MethodResult<flutter::EncodableValue>> result);
};

}  // namespace hh_screen_recorder

#endif  // FLUTTER_PLUGIN_HH_SCREEN_RECORDER_PLUGIN_H_
