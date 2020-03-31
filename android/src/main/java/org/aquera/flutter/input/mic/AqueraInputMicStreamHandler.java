/*
 * AqueraInputMicPlugin - Flutter microphone input plugin from Aquera Foundation
 * Copyright © 2020 - All rights reserved.
 */
package org.aquera.flutter.input.mic;

import android.media.AudioFormat;
import android.media.AudioRecord;

import io.flutter.plugin.common.EventChannel;

/**
 * Generate a stream of events with the data received from the microphone.
 * Will fill up a Stream of integer on dart side.
 *
 */
public class AqueraInputMicStreamHandler implements EventChannel.StreamHandler, Runnable {

    /**
     * Event sink to communicate with Flutter
     */
    private EventChannel.EventSink eventSink;
    /**
     * Microphone controller - low level interface
     */
    private MicrophoneController microphone;
    /**
     * Signal that the listener disconnected from the stream and the background thread should finalize.
     */
    private boolean listenerGone;


    public AqueraInputMicStreamHandler() {
        microphone = MicrophoneController.getInstance();
        listenerGone = true;
    }

    @Override
    public void onListen(Object arguments, EventChannel.EventSink events) {
        this.eventSink = events;
        listenerGone = false;
        new Thread(this).start();
    }

    @Override
    public void onCancel(Object arguments) {
        if (microphone.isRecording()) {
            microphone.pause();
        }
        listenerGone = true;
    }

    /**
     * get data from the microphone encoded as 8 BIT unsigned PCM and
     * render it as a signed short list.
     */
    protected void run_8bit() {
        int sample_count = microphone.getAudioBufferSize();

        short[] result = new short[sample_count];
        byte[] input = new byte[sample_count];

        while (!listenerGone) {
            if (!microphone.isRecording()) {
                continue;
            }

            microphone.getRecorder().read(input, 0, sample_count, AudioRecord.READ_BLOCKING);

            for (int i=0; i < sample_count; ++i) {
                result[i] = (short) (127 - (input[i] & 0x00ff));
            }

            eventSink.success(result);
        }

    }

    /**
     * get data from the microphone, encoded as 16 BIT signed PCM and
     * render it to the dart program.
     */
    protected void run_16bit() {
        int sample_count = microphone.getSamplesPerBuffer();

        short[] result = new short[sample_count];

        while (!listenerGone) {
            if (!microphone.isRecording()) {
                continue;
            }

            microphone.getRecorder().read(result, 0, sample_count, AudioRecord.READ_BLOCKING);
            eventSink.success(result);
        }

    }

    /**
     * Background thread method. Look the microphone configuration and call the
     * correct loop to convert the data.
     */
    public void run() {
        switch (microphone.getAudioEncoding()) {
            case AudioFormat.ENCODING_PCM_8BIT:
                run_8bit();
                break;
            case AudioFormat.ENCODING_PCM_16BIT:
                run_16bit();
                break;
            default:
                eventSink.error("InvalidFormat", "Invalid Audio Format", null);
        }
        eventSink.endOfStream();
    }
}
