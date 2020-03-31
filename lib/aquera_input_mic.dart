import 'dart:async';

import 'package:flutter/services.dart';
import 'package:permission/permission.dart';

/*
 * Configuration Options
 */

// Audio source 
enum AudioSource {
  CURRENT,
  DEFAULT,
  MIC,
  VOICE_COMMUNICATION,
  CAMCORDER,
  VOICE_RECOGNITION,
  VOICE_PERFORMANCE,
  UNPROCESSED
}

// Microphone channels
enum ChannelConfig { CURRENT, MONO, STEREO }

// Audio resolution (bits per sample)
enum AudioResolution { CURRENT, PCM_16BIT, PCM_8BIT }

/*
 * Trying to execute a command when the recorder is in an invalid state
 */
class InvalidStateException implements Exception {
  String message;
  InvalidStateException(
      [String _message =
          "The component is in an invalid state for this operation"]) {
    this.message = _message;
  }
}

/*
 * Microphone input controller class 
 * 
 * Singleton - microphone controller
 * 
 */
class AqueraInputMic {
  // plugin communication channels
  EventChannel _eventChannel;
  MethodChannel _methodChannel;

  // configuration
  AudioSource _audioSource;
  ChannelConfig _channelConfig;
  AudioResolution _audioResolution;
  int _sampleRate;
  bool _streaming;

  // Event stream
  Stream<dynamic> _eventStream;

  // singleton instance
  static final AqueraInputMic _instance = AqueraInputMic._internal();

  // factory/constructor
  factory AqueraInputMic() {
    return _instance;
  }

  // real constructor - initialization code
  AqueraInputMic._internal() {
    // initialization code
    _eventChannel = EventChannel("org.aquera:input_mic:events");
    _methodChannel = MethodChannel('org.aquera:input_mic:methods');
    _audioSource = AudioSource.UNPROCESSED;
    _channelConfig = ChannelConfig.MONO;
    _audioResolution = AudioResolution.PCM_16BIT;
    _sampleRate = 8000; // 8KHz as default sample rate
    _streaming = false;
  }

  // getters
  AudioSource get audioSource => _audioSource;
  ChannelConfig get channelConfig => _channelConfig;
  AudioResolution get audioResolution => _audioResolution;
  int get sampleRate => _sampleRate;
  bool get streaming => _streaming;

  void _preventChangingParameterWhenStreaming() {
    if (streaming) {
      throw InvalidStateException(
          "Unable to change parameters while streaming");
    }
  }

  // setters
  set audioSource(AudioSource v) {
    if (v != audioSource) {
      _preventChangingParameterWhenStreaming();
      _audioSource = v;
    }
  }

  set channelConfig(ChannelConfig v) {
    if (v != _channelConfig) {
      _preventChangingParameterWhenStreaming();
      _channelConfig = v;
    }
  }

  set audioResolution(AudioResolution v) {
    if (v != _audioResolution) {
      _preventChangingParameterWhenStreaming();
      _audioResolution = v;
    }
  }

  set sampleRate(int v) {
    if (v != _sampleRate) {
      _preventChangingParameterWhenStreaming();
      _sampleRate = v;
    }
  }

  // Check if the access to the microphone is allowed.
  Future<bool> get permissionStatus async {
    Permissions permission =
        (await Permission.getPermissionsStatus([PermissionName.Microphone]))
            .first;

    if (permission.permissionStatus != PermissionStatus.allow) {
      permission =
          (await Permission.requestPermissions([PermissionName.Microphone]))
              .first;
    }

    return permission.permissionStatus == PermissionStatus.allow;
  }

  Future<String> get platformVersion async {
    final String version =
        await _methodChannel.invokeMethod('getPlatformVersion');
    return version;
  }

  Stream<List<int>> start(
      {AudioSource audioSource: AudioSource.CURRENT,
      int sampleRate: 0,
      ChannelConfig channelConfig: ChannelConfig.CURRENT,
      AudioResolution audioResolution: AudioResolution.CURRENT}) async* {
    if (streaming) {
      throw InvalidStateException("Already streaming");
    }

    if (!(await permissionStatus)) {
      throw PlatformException(
          code: "PermissionDenied",
          message: "Not allowed to access the microphone");
    }

    if (_eventStream == null) {
      await _methodChannel.invokeMethod('setupMicrophone', <String, dynamic>{
        'source': (audioSource == AudioSource.CURRENT
                ? this.audioSource
                : audioSource)
            .index,
        'sampleRate': sampleRate == 0 ? this.sampleRate : sampleRate,
        'channelConfig': (channelConfig == ChannelConfig.CURRENT
                ? this.channelConfig
                : channelConfig)
            .index,
        'audioResolution': (audioResolution == AudioResolution.CURRENT
                ? this.audioResolution
                : audioResolution)
            .index
      });

      _eventStream = _eventChannel.receiveBroadcastStream();
      _streaming = true;
    }

    await _methodChannel.invokeMethod('start');

    yield* (_parseStream(_eventStream));
  }

  void pause() async {
    if (streaming) {
      await _methodChannel.invokeMethod('pause');
    }
  }

  Future<bool> get paused  => _methodChannel.invokeMethod('isPaused');
  Future<bool> get recording  => _methodChannel.invokeMethod('isRecording');
  Future<bool> get active  => _methodChannel.invokeMethod('isActive');

  void resume() async {
    if (streaming) {
      await _methodChannel.invokeMethod('resume');
    }
  }

  void stop() async {
    if (streaming) {
      await _methodChannel.invokeMethod('stop');
    }
  }

  Stream<List<int>> _parseStream(Stream<List<dynamic>> micData) {
    return micData.map((List<dynamic> i) => i as List<int>);
  }
}
