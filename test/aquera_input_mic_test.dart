import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:aquera_input_mic/aquera_input_mic.dart';

void main() {
  const MethodChannel channel = MethodChannel('aquera_input_mic');

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  test('getPlatformVersion', () async {
    expect(await AqueraInputMic().platformVersion, '42');
  });
}
