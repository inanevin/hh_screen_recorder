import Flutter
import UIKit
import ReplayKit

public class SwiftHhScreenRecorderPlugin: NSObject, FlutterPlugin {
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
            //code to display recording indicator
            result(true)
        }
        
    }
    else if (call.method == "stopRecording")
    {
        print("HHRecorder: Stop Recording")
        result(true)

        RPScreenRecorder.shared().stopRecording { preview, err in
          guard let preview = preview else { print("no preview window"); return }
          //update recording controls
          preview.modalPresentationStyle = .overFullScreen
          preview.previewControllerDelegate = self
          self.present(preview, animated: true)
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
    
    @objc func startRecording() {
            let recorder: RPScreenRecorder = RPScreenRecorder.shared()
            
            recorder.startRecording{ [unowned self] (error: Error?) in
                if let unwrappedError: Error = error {
                    print(unwrappedError.localizedDescription)
                } else {
                    
                }
            }
        }

        @objc func stopRecording() {
            let recorder: RPScreenRecorder = RPScreenRecorder.shared()
            recorder.stopRecording()
        }
}
