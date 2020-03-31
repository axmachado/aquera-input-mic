package org.aquera.flutter.input.mic;

import android.media.AudioFormat;

public enum DartAudioChannels {
    CURRENT(0,-1),
    MONO(1, AudioFormat.CHANNEL_IN_MONO),
    STEREO(2, AudioFormat.CHANNEL_IN_STEREO)
    ;

    int dartIndex;
    int androidInputChannels;

    DartAudioChannels(int dartIndex, int androidInputChannels) {
        this.dartIndex = dartIndex;
        this.androidInputChannels = androidInputChannels;
    }

    public int getDartIndex() {
        return dartIndex;
    }

    public void setDartIndex(int dartIndex) {
        this.dartIndex = dartIndex;
    }

    public int getAndroidInputChannels() {
        return androidInputChannels;
    }

    public void setAndroidInputChannels(int androidInputChannels) {
        this.androidInputChannels = androidInputChannels;
    }

    public static DartAudioChannels fromDart(int code) {
        for (DartAudioChannels v : DartAudioChannels.values()) {
            if (v.dartIndex == code) {
                return v;
            }
        }
        return CURRENT;
    }

}
