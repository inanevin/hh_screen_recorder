import Flutter
import UIKit
import ReplayKit


public class SwiftHhScreenRecorderPlugin: NSObject, FlutterPlugin, RPPreviewViewControllerDelegate {
  
    var flutterRes : FlutterResult?
    
  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "hh_screen_recorder", binaryMessenger: registrar.messenger())
    let instance = SwiftHhScreenRecorderPlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {

    flutterRes = result
      
    if (call.method == "startRecording")
    {
        print("HHRecorder: Start Recording")
       
        RPScreenRecorder.shared().startRecording { err in
          guard err == nil else {
              print("HHRecorder: Error starting recording: \(err.debugDescription)")
              result(false)
              return }
            
            print("HHRecorder: Started recording.")
            result(true)
        }
        
    }
    else if (call.method == "stopRecording")
    {
        print("HHRecorder: Attempting to stop recording & show preview window")

        RPScreenRecorder.shared().stopRecording { preview, err in
          guard let preview = preview else {
              print("HHRecorder: Error stopping recording: no preview window!");
              result(false)
              return
          }
            
            if let err = err {
                print("HHRecorder: Error stopping recording: \(err.localizedDescription)")
                result(false)
                return
            }

            if UIDevice.current.userInterfaceIdiom == .pad {
            preview.modalPresentationStyle = .popover
            preview.popoverPresentationController?.sourceRect = .zero
            preview.popoverPresentationController?.sourceView =
                UIApplication.shared.delegate?.window??.rootViewController?.view
        }else {
          preview.modalPresentationStyle = .overFullScreen
        }
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
        // iOS 9.0+ is always supported on HH
        result(true)
    }
    else
    {
        result(false)
        // result(FlutterMethodNotImplemented)
    }
  }

  public func previewControllerDidFinish(_ previewController: RPPreviewViewController) {
      
      UIApplication.shared.delegate?.window??.rootViewController?.dismiss(animated: true)
      flutterRes?(true)
      print("HHRecorder: Stopped recording")

    }
}
