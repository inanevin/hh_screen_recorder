#include "include/hh_screen_recorder/hh_screen_recorder_plugin_c_api.h"

#include <flutter/plugin_registrar_windows.h>

#include "hh_screen_recorder_plugin.h"

void HhScreenRecorderPluginCApiRegisterWithRegistrar(
    FlutterDesktopPluginRegistrarRef registrar) {
  hh_screen_recorder::HhScreenRecorderPlugin::RegisterWithRegistrar(
      flutter::PluginRegistrarManager::GetInstance()
          ->GetRegistrar<flutter::PluginRegistrarWindows>(registrar));
}
