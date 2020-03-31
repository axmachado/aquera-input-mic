/*
 * AqueraInputMicPlugin - Flutter microphone input plugin from Aquera Foundation
 * Copyright © 2020 - All rights reserved.
 */
package org.aquera.flutter.input.mic;

import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.PluginRegistry;

/**
 * Handle method calls from dart code.
 * @author Alexandre Machado
 */
public class AqueraInputMicMethodHandler implements MethodChannel.MethodCallHandler {

    static final String[] setupMicrophoneArguments = { "source", "sampleRate", "channelConfig", "audioResolution"};

    /**
     * Configure the microphon parameters
     *
     * @param call
     * @param result
     */
    protected void setupMicrophone(MethodCall call, MethodChannel.Result result) {

        int[] v = new int[4];
        for (int i=0; i < 4; ++i) {
            v[i] = call.hasArgument(setupMicrophoneArguments[i]) ? (int) call.argument(setupMicrophoneArguments[i]) : 0;
        }

        int source = v[0];
        int sampleRate = v[1];
        int channelConfig = v[2];
        int audioResolution = v[3];

        MicrophoneController controller = MicrophoneController.getInstance();
        controller.setAudioSourceFromDart(source);
        controller.setSampleRate(sampleRate);
        controller.setAudioChannelsFromDart(channelConfig);
        controller.setAudioEncodingFromDart(audioResolution);

        result.success(null);
    }

    /**
     * Start recording - and streaming microphone data
     *
     * @param call
     * @param result
     */
    protected void startRecording(MethodCall call, MethodChannel.Result result) {
        MicrophoneController controller = MicrophoneController.getInstance();
        try {
            controller.start();
            result.success(null);
        } catch (MicrophoneError microphoneError) {
            result.error("MicrophoneError", microphoneError.getMessage(), null);
        }
    }

    /**
     * Stop streaming data and release system resources
     *
     * @param call
     * @param result
     */
    protected void stopRecording(MethodCall call, MethodChannel.Result result) {
        MicrophoneController controller = MicrophoneController.getInstance();
        controller.stop();
        result.success(null);
    }

    /**
     * Pause recording, but do not release the microphone control.
     *
     * @param call
     * @param result
     */
    protected void pauseRecording(MethodCall call, MethodChannel.Result result) {
        MicrophoneController controller = MicrophoneController.getInstance();
        controller.pause();
        result.success(null);
    }

    /**
     * Resume a paused recording
     *
     * @param call
     * @param result
     */
    protected void resumeRecording(MethodCall call, MethodChannel.Result result) {
        MicrophoneController controller = MicrophoneController.getInstance();
        controller.resume();
        result.success(null);
    }

    /**
     * Check if the microphone is recording
     *
     * @param call
     * @param result
     */
    protected void isRecording(MethodCall call, MethodChannel.Result result) {
        MicrophoneController controller = MicrophoneController.getInstance();
        boolean state = controller.isRecording();
        result.success(state);
    }

    /**
     * Check if the microphone is actively controlled by the application
     *
     * @param call
     * @param result
     */
    protected void isActive(MethodCall call, MethodChannel.Result result) {
        MicrophoneController controller = MicrophoneController.getInstance();
        boolean state = controller.isActive();
        result.success(state);
    }

    /**
     * Check ir the recording is paused.
     *
     * @param call
     * @param result
     */
    protected void isPaused(MethodCall call, MethodChannel.Result result) {
        MicrophoneController controller = MicrophoneController.getInstance();
        boolean state = controller.isPaused();
        result.success(state);
    }


    /**
     * Handle method calls from dart code.
     *
     * @param call
     * @param result
     */
    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull MethodChannel.Result result) {
        if (call.method.equals("getPlatformVersion")) {
            result.success("Android " + android.os.Build.VERSION.RELEASE);
        } else if (call.method.equals("setupMicrophone")) {
            setupMicrophone(call, result);
        } else if (call.method.equals("start")) {
            startRecording(call, result);
        } else if (call.method.equals("stop")) {
            stopRecording(call, result);
        } else if (call.method.equals("pause")) {
            pauseRecording(call, result);
        } else if (call.method.equals("resume")) {
            resumeRecording(call, result);
        } else if (call.method.equals("isRecording")) {
            isRecording(call, result);
        } else if (call.method.equals("isActive")) {
            isActive(call, result);
        } else if (call.method.equals("isPaused")) {
            isPaused(call, result);
        } else {
            result.notImplemented();
        }
    }
}
