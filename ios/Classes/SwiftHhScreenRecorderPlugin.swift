import Flutter
import UIKit
import ReplayKit
import Photos

public class SwiftHhScreenRecorderPlugin: NSObject, FlutterPlugin{
  
  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "hh_screen_recorder", binaryMessenger: registrar.messenger())
    let instance = SwiftHhScreenRecorderPlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
      result(true)
  }

}
