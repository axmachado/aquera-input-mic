#import "AqueraInputMicPlugin.h"
#if __has_include(<aquera_input_mic/aquera_input_mic-Swift.h>)
#import <aquera_input_mic/aquera_input_mic-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "aquera_input_mic-Swift.h"
#endif

@implementation AqueraInputMicPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftAqueraInputMicPlugin registerWithRegistrar:registrar];
}
@end
