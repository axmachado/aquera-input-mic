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
 *
 * @author Alexandre Machado
 */
public class AqueraInputMicMethodHandler implements MethodChannel.MethodCallHandler {

    static final String[] setupMicrophoneArguments = {"source", "sampleRate", "channelConfig", "audioResolution"};

    static final int[] RATES_8K = {8000, 4000, 2666, 2000, 1600, 1333, 1142, 1000};
    static final int[] RATES_16K = {16000, 5333, 3200, 2285};
    static final int[] RATES_44K = {44100, 22050, 14700, 11025, 8820, 7350, 6300, 5512, 2756, 1838, 1102, 918};

    private MicrophoneController microphoneController;

    public AqueraInputMicMethodHandler(MicrophoneController microphoneController) {
        this.microphoneController = microphoneController;
    }

    protected boolean inArray(int[] array, int v) {
        for (int x : array) {
            if (v == x) {
                return true;
            }
        }
        return false;
    }

    protected int findSampleRate(int rate) {
        if (inArray(RATES_8K, rate)) {
            return 8000;
        } else if (inArray(RATES_16K, rate)) {
            return 16000;
        } else if (inArray(RATES_44K, rate)) {
            return 44100;
        }
        return -1;
    }

    protected int findDownsamplingDivisor(int rate, int actualRate) {
        return actualRate / rate;
    }

    /**
     * Configure the microphone parameters
     *
     * @param call
     * @param result
     */
    protected void setupMicrophone(MethodCall call, MethodChannel.Result result) {

        int intendedSampleRate = call.hasArgument("sampleRate") ? ((int) call.argument("sampleRate")) : MicrophoneController.SAMPLE_RATE;
        int source = call.hasArgument("source") ? ((int) call.argument("source")) : 0;
        int sampleRate = findSampleRate(intendedSampleRate);
        int divisor = findDownsamplingDivisor(intendedSampleRate, sampleRate);
        int channelConfig = call.hasArgument("channelConfig") ? ((int) call.argument("channelConfig")) : 0;
        int audioResolution = call.hasArgument("audioResolution") ? ((int) call.argument("audioResolution")) : 0;
        int fftBufferSize = call.hasArgument("fftSize") ? ((int) call.argument("fftSize")) : MicrophoneController.FFT_SIZE;
        int sampleBufferSize = call.hasArgument("bufferSize") ? ((int) call.argument("bufferSize")) : MicrophoneController.BUFFER_SIZE;

        if (sampleRate < 0) {
            result.error("InvalidSampleRate", "The requested sample rate (" + sampleRate + " Hz) is not supported", null);
            return;
        }

        microphoneController.setAudioSourceFromDart(source);
        microphoneController.setSampleRate(sampleRate);
        microphoneController.setDivisor(divisor);
        microphoneController.setAudioChannelsFromDart(channelConfig);
        microphoneController.setAudioEncodingFromDart(audioResolution);
        microphoneController.setFftBufferSize(fftBufferSize);
        microphoneController.setDataFlowBufferSize(sampleBufferSize);

        result.success(null);
    }

    /**
     * Start recording - and streaming microphone data
     *
     * @param call
     * @param result
     */
    protected void startRecording(MethodCall call, MethodChannel.Result result) {
        try {
            microphoneController.start();
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
        microphoneController.stop();
        result.success(null);
    }

    /**
     * Pause recording, but do not release the microphone control.
     *
     * @param call
     * @param result
     */
    protected void pauseRecording(MethodCall call, MethodChannel.Result result) {
        microphoneController.pause();
        result.success(null);
    }

    /**
     * Resume a paused recording
     *
     * @param call
     * @param result
     */
    protected void resumeRecording(MethodCall call, MethodChannel.Result result) {
        microphoneController.resume();
        result.success(null);
    }

    /**
     * Check if the microphone is recording
     *
     * @param call
     * @param result
     */
    protected void isRecording(MethodCall call, MethodChannel.Result result) {
        boolean state = microphoneController.isRecording();
        result.success(state);
    }

    /**
     * Check if the microphone is actively controlled by the application
     *
     * @param call
     * @param result
     */
    protected void isActive(MethodCall call, MethodChannel.Result result) {
        boolean state = microphoneController.isActive();
        result.success(state);
    }

    /**
     * Check ir the recording is paused.
     *
     * @param call
     * @param result
     */
    protected void isPaused(MethodCall call, MethodChannel.Result result) {
        boolean state = microphoneController.isPaused();
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
