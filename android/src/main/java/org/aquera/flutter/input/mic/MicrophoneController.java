package org.aquera.flutter.input.mic;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.AutomaticGainControl;

public class MicrophoneController {

    public static final int BUFFER_SIZE = 4000;
    public static final int FFT_SIZE = 4096;
    public static final int SAMPLE_RATE = 8000;
    private static MicrophoneController instance;

    private Context appContext;
    private int audioSource;
    private int audioEncoding;
    private int audioChannels;
    private int sampleRate;
    private int divisor;
    private int audioBufferSize;
    private int dataFlowBufferSize;
    private int fftBufferSize;

    private AudioRecord recorder;

    private MicrophoneController() {
        audioSource = MediaRecorder.AudioSource.UNPROCESSED;
        audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
        audioChannels = AudioFormat.CHANNEL_IN_MONO;
        sampleRate = SAMPLE_RATE;
        divisor = 1;
        recorder = null;
        audioBufferSize = 0;
        dataFlowBufferSize = BUFFER_SIZE;
        fftBufferSize = FFT_SIZE;
    }

    public static MicrophoneController getInstance() {
        if (instance == null) {
            instance = new MicrophoneController();
        }

        return instance;
    }

    public void start() throws MicrophoneError {
        if (!isActive()) {
            setupRecorder();
        }
        if (isPaused()) {
            resume();
        }
    }

    public void stop() {
        if (isActive()) {
            if (!isPaused()) {
                pause();
            }
            releaseResources();
        }
    }

    public void resume() {
        if (isPaused()) {
            recorder.startRecording();
        }
    }

    public void pause() {
        if (isActive()) {
            if (!isPaused()) {
                recorder.stop();
            }
        }
    }

    @SuppressLint("WrongConstant")
    private synchronized void setupRecorder() throws MicrophoneError {
        if (recorder != null) {
            releaseResources();
        }

        audioBufferSize = AudioRecord.getMinBufferSize(getSampleRate(), getAudioChannels(), getAudioEncoding());
        if ((getSamplesPerBuffer() % getDivisor()) != 0) {
            // ensure that the buffer size in samples is a multiple of the downsampling divisor.
            audioBufferSize = ((getSamplesPerBuffer() / divisor) * divisor) * getBytesPerSample();
        }

        AudioRecord.Builder recordBuilder = new AudioRecord.Builder();
        AudioFormat.Builder formatBuilder = new AudioFormat.Builder();

        formatBuilder.setEncoding(getAudioEncoding());
        formatBuilder.setSampleRate(getSampleRate());
        formatBuilder.setChannelMask(getAudioChannels());

        recordBuilder.setAudioSource(getAudioSource());
        recordBuilder.setAudioFormat(formatBuilder.build());
        recordBuilder.setBufferSizeInBytes(audioBufferSize);

        this.recorder = recordBuilder.build();

        int actualSampleRate = this.recorder.getSampleRate();
        if (actualSampleRate != this.sampleRate) {
            throw new MicrophoneError ("Sample Rate (" + this.getSampleRate() + "Hz) not supported");
        }

        if (getAudioSource() == DartAudioSource.UNPROCESSED.getAndroidAudioSource()) {
            if (AutomaticGainControl.isAvailable()) {
                AutomaticGainControl agc = AutomaticGainControl.create(this.recorder.getAudioSessionId());
                agc.setEnabled(false);
            }
        }

        if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
            releaseResources();
            throw new MicrophoneError("Failed to initialize recorder");
        }
    }

    private synchronized void releaseResources() {
        if (recorder != null) {
            recorder.release();
            recorder = null;
        }
    }

    public AudioRecord getRecorder() {
        return recorder;
    }


    public void setAudioSourceFromDart(int source) {
        DartAudioSource as = DartAudioSource.fromDart(source);
        if (as != DartAudioSource.CURRENT) {
            setAudioSource(as.getAndroidAudioSource());
        }
    }

    public void setAudioChannelsFromDart(int ch) {
        DartAudioChannels channels = DartAudioChannels.fromDart(ch);
        if (channels != DartAudioChannels.CURRENT) {
            setAudioChannels(channels.getAndroidInputChannels());
        }
    }

    public void setAudioEncodingFromDart(int enc) {
        DartAudioEncoding encoding = DartAudioEncoding.fromDart(enc);
        if (encoding != DartAudioEncoding.CURRENT) {
            setAudioEncoding(encoding.getAndroidEncoding());
        }
    }

    public int getAudioSource() {
        return audioSource;
    }

    public void setAudioSource(int audioSource) {
        if (!isActive()) {
            this.audioSource = audioSource;
        }
    }

    public int getAudioEncoding() {
        return audioEncoding;
    }

    public void setAudioEncoding(int audioEncoding) {
        if (!isActive()) {
            this.audioEncoding = audioEncoding;
        }
    }

    public int getAudioChannels() {
        return audioChannels;
    }

    public void setAudioChannels(int audioChannels) {
        if (!isActive()) {
            this.audioChannels = audioChannels;
        }
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(int sampleRate) {
        if (!isActive()) {
            this.sampleRate = sampleRate;
        }
    }

    public Context getAppContext() {
        return appContext;
    }

    public void setAppContext(Context appContext) {
        this.appContext = appContext;
    }

    public boolean isActive() {
        return recorder != null;
    }

    public boolean isPaused() {
        if (isActive()) {
            if (recorder.getRecordingState() == AudioRecord.RECORDSTATE_STOPPED) {
                return true;
            }
        }
        return false;
    }

    public boolean isRecording() {
        if (isActive()) {
            if (recorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                return true;
            }
        }
        return false;
    }

    public int getAudioBufferSize() {
        return audioBufferSize;
    }

    public int getBytesPerSample() {
        switch (getAudioEncoding()) {
            case AudioFormat.ENCODING_PCM_8BIT:
                return 1;
            case AudioFormat.ENCODING_PCM_16BIT:
                return 2;
        }
        return 1;
    }

    public int getSamplesPerBuffer() {
        return getAudioBufferSize() / getBytesPerSample();
    }

    public int getDivisor() {
        return divisor;
    }

    public void setDivisor(int divisor) {
        if (divisor <= 0) {
            divisor = 1;
        }
        this.divisor = divisor;
    }

    public int getDataFlowBufferSize() {
        return dataFlowBufferSize;
    }

    public void setDataFlowBufferSize(int dataFlowBufferSize) {
        this.dataFlowBufferSize = dataFlowBufferSize;
    }

    public int getFftBufferSize() {
        return fftBufferSize;
    }

    public void setFftBufferSize(int fftBufferSize) {
        this.fftBufferSize = fftBufferSize;
    }
}
