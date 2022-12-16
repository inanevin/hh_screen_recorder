#
# To learn more about a Podspec see http://guides.cocoapods.org/syntax/podspec.html.
# Run `pod lib lint hh_screen_recorder.podspec` to validate before publishing.
#
Pod::Spec.new do |s|
  s.name             = 'hh_screen_recorder'
  s.version          = '1.0.0'
  s.summary          = 'Screen recorder.'
  s.description      = <<-DESC
Screen recorder.
                       DESC
  s.homepage         = 'http://inanevin.com'
  s.license          = { :file => '../LICENSE' }
  s.author           = { 'InanEvin' => 'inanevin@gmail.com' }

  s.source           = { :path => '.' }
  s.source_files     = 'Classes/**/*'
  s.dependency 'FlutterMacOS'

  s.platform = :osx, '10.11'
  s.pod_target_xcconfig = { 'DEFINES_MODULE' => 'YES' }
  s.swift_version = '5.0'
end
