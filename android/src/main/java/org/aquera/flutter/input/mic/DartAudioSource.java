/*
 * AqueraInputMicPlugin - Flutter microphone input plugin from Aquera Foundation
 * Copyright © 2020 - All rights reserved.
 */
package org.aquera.flutter.input.mic;

import android.media.MediaRecorder;

/**
 * @Author Alexandre Machado
 * Audio Source enum mapping
 * Get the integer value from the dart part of the plugin and convert to Android specific values.
 */
public enum DartAudioSource {

    CURRENT(0, -1), // current selected value - no change
    DEFAULT(1, MediaRecorder.AudioSource.DEFAULT), // default input for the device
    MIC(2, MediaRecorder.AudioSource.MIC), // microphone input
    VOICE_COMMUNICATION(3, MediaRecorder.AudioSource.VOICE_COMMUNICATION), // voice communications input
    CAMCORDER(4, MediaRecorder.AudioSource.CAMCORDER), // camcorder (video recorder) sound input
    VOICE_RECOGNITION(5, MediaRecorder.AudioSource.VOICE_RECOGNITION), // voice recognition input
    VOICE_PERFORMANCE(6, MediaRecorder.AudioSource.VOICE_COMMUNICATION), // voice performance input
    UNPROCESSED(7, MediaRecorder.AudioSource.UNPROCESSED); // unprocessed (raw) microphone input

    /**
     * dart enum index (position)
     */
    int dartIndex;
    /**
     * audio source value for Android
     */
    int androidAudioSource;

    /**
     * Constructor
     * @param dartIndex
     * @param androidAudioSource
     */
    DartAudioSource(int dartIndex, int androidAudioSource) {
        this.dartIndex = dartIndex;
        this.androidAudioSource = androidAudioSource;
    }

    /**
     * Enum value from dart index position
     * @param code
     * @return audio source equivalent to code.
     */
    public static DartAudioSource fromDart(int code) {
        for (DartAudioSource v : DartAudioSource.values()) {
            if (v.dartIndex == code) {
                return v;
            }
        }
        return CURRENT;
    }

    public int getDartIndex() {
        return dartIndex;
    }

    public void setDartIndex(int dartIndex) {
        this.dartIndex = dartIndex;
    }

    public int getAndroidAudioSource() {
        return androidAudioSource;
    }

    public void setAndroidAudioSource(int androidAudioSource) {
        this.androidAudioSource = androidAudioSource;
    }

}
