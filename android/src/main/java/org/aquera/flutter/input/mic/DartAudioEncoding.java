package org.aquera.flutter.input.mic;

import android.media.AudioFormat;

public enum DartAudioEncoding {

    CURRENT(0,-1),
    PCM_16BIT (1, AudioFormat.ENCODING_PCM_16BIT),
    PCM_8BIT (2, AudioFormat.ENCODING_PCM_8BIT)
    ;

    int dartIndex;
    int androidEncoding;

    DartAudioEncoding(int dartIndex, int androidInputChannels) {
        this.dartIndex = dartIndex;
        this.androidEncoding = androidInputChannels;
    }

    public int getDartIndex() {
        return dartIndex;
    }

    public void setDartIndex(int dartIndex) {
        this.dartIndex = dartIndex;
    }

    public int getAndroidEncoding() {
        return androidEncoding;
    }

    public void setAndroidEncoding(int androidEncoding) {
        this.androidEncoding = androidEncoding;
    }

    public static DartAudioEncoding fromDart(int code) {
        for (DartAudioEncoding v : DartAudioEncoding.values()) {
            if (v.dartIndex == code) {
                return v;
            }
        }
        return CURRENT;
    }

}
