import Flutter
import UIKit
import ReplayKit
import Photos

public class SwiftHhScreenRecorderPlugin: NSObject, FlutterPlugin, RPPreviewViewControllerDelegate {
  
  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "hh_screen_recorder", binaryMessenger: registrar.messenger())
    let instance = SwiftHhScreenRecorderPlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {

    if (call.method == "startRecording")
    {
        print("HHRecorder: Start Recording")
       
        RPScreenRecorder.shared().startRecording { err in
          guard err == nil else {
              print(err.debugDescription);
              result(false)
              return }
            result(true)
        }
        
    }
    else if (call.method == "stopRecording")
    {
        print("HHRecorder: Stop Recording")
        result(true)

        RPScreenRecorder.shared().stopRecording { preview, err in
          guard let preview = preview else { print("no preview window"); return }
          preview.modalPresentationStyle = .overFullScreen
          preview.previewControllerDelegate = self
            UIApplication.shared.delegate?.window??.rootViewController?.present(preview, animated: true)
        }
        
    }
    else if (call.method == "pauseRecording") 
    {
      result(false)
    }
    else if (call.method == "resumeRecording") 
    {
      result(false)
    }
    else if (call.method == "isPauseResumeEnabled") 
    {
      result(false)
    }
    else if (call.method == "isRecordingSupported")
    {
      result(true)
    }
  }

  public func previewControllerDidFinish(_ previewController: RPPreviewViewController) {
      UIApplication.shared.delegate?.window??.rootViewController?.dismiss(animated: true)
    }
}
