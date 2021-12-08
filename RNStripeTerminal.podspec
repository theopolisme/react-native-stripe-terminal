require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = "RNStripeTerminal"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.description  = <<-DESC
                  RNStripeTerminal
                   DESC
  s.homepage     = "https://github.com/theopolisme/react-native-stripe-terminal"
  s.license      = "MIT"
  s.author       = { "author" => "theo@theopatt.com" }
  s.platform     = :ios, "11.0"
  s.source       = { :git => "https://github.com/theopolisme/react-native-stripe-terminal.git", :tag => "#{s.version}" }

  s.source_files = "ios/**/*.{h,m}"
  s.requires_arc = true

  s.dependency "React"
  s.dependency "StripeTerminal"
end
