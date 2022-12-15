import Cocoa
import FlutterMacOS
import ReplayKit
import AppKit

public class HhScreenRecorderPlugin: NSObject, FlutterPlugin,
                                     RPPreviewViewControllerDelegate
                                     {
    
    
    var flutterRes : FlutterResult?
    
  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "hh_screen_recorder", binaryMessenger: registrar.messenger)
    let instance = HhScreenRecorderPlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
      flutterRes = result
        
      if #available(OSX 11.0, *) {
          
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
              RPScreenRecorder.shared().stopRecording { previewViewController, err in
          
                  if let err = err {
                      print("HHRecorder: Error stopping recording: \(err.localizedDescription)")
                      result(false)
                      return
                  }
                  
                  if let previewViewController = previewViewController {
                      
                      previewViewController.previewControllerDelegate = self
                      let viewController = NSApplication.shared.keyWindow!.contentViewController
                      
                      viewController?.presentAsModalWindow(previewViewController)
                      
                  }
                  else
                  {
                      result(true)
                  }
                  
                  
         
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
              result(FlutterMethodNotImplemented)
          }
          
      } else {
          print("HHRecorder: ReplayKit is only availab on MacOS 11+")
          result(false)
      }
     
  }
    
    @available(OSX 11.0, *)
    public func previewControllerDidFinish(_ previewController: RPPreviewViewController) {
        
       // UIApplication.shared.delegate?.window??.rootViewController?.dismiss(animated: true)
        let viewController = NSApplication.shared.keyWindow!.contentViewController
        
        viewController?.dismiss(previewController)
        flutterRes?(true)
        print("HHRecorder: Stopped recording")

      }
    
}
