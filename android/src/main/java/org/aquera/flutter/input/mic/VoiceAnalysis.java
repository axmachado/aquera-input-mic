package org.aquera.flutter.input.mic;

public class VoiceAnalysis {
    public static final double LOWER_ALLOWED_FREQUENCY = 15;

    private double[] fftBuffer;
    private int bufferSize;
    private int cursorStart;
    private int cursorEnd;
    private int cursor;
    private double freqBin;
    private double binOffset;
    private double lastFrequencyPoint;
    AqueraInputMicStreamHandler streamHandler;

    public VoiceAnalysis(int fftSize, int bufferSize, int sampleFrequency, AqueraInputMicStreamHandler streamHandler) {
        if (fftSize < bufferSize) {
            fftSize = bufferSize;
        }
        this.fftBuffer = new double[fftSize];
        this.bufferSize = bufferSize;
        this.cursorStart = (fftSize/2) - (bufferSize/2);
        if (cursorStart < 0) {
            cursorStart = 0;
        }
        this.cursorEnd = this.cursorStart + bufferSize;
        this.cursor = this.cursorStart;

        this.streamHandler = streamHandler;
        freqBin = (sampleFrequency / 2.0) / (fftSize/2);
        binOffset = freqBin/2;
        lastFrequencyPoint = 0;
    }


    public void emitFrequencyPoint(double frequency, double dbs) {
        streamHandler.sendFrequencyPoint(frequency, dbs);
        lastFrequencyPoint = frequency;
    }

    protected double[] meanSquareRoot(double[] data) {
        double[] result = new double[data.length];
        double normFactor = 1.0/Math.sqrt(this.bufferSize);
        for (int i = this.cursorStart; i < this.cursorEnd; ++i) {
            result[i] = data[i] * normFactor;
        }
        return result;
    }

    public void processFFTBuffer() {
        Complex[] fftResult = FFT.fft(meanSquareRoot(fftBuffer));
        double freq = 0;
        double dbs = 0;
        int max = fftBuffer.length / 2;
        for (int i = 1; i < max; ++i) {
            double modulus = fftResult[i].abs();
            double freqOfPoint = (i * freqBin) + binOffset;
            if (Math.abs(freqOfPoint-lastFrequencyPoint) > 0.001 && freqOfPoint >= LOWER_ALLOWED_FREQUENCY) {
                if (modulus > dbs) {
                    dbs = modulus;
                    freq = freqOfPoint;
                }
            }
        }
        dbs = 2.5 * Math.log10(dbs);
        if (freq > 0) {
            emitFrequencyPoint(freq, dbs);
        }
    }

    public void shiftFFTBuffer() {
        cursor = this.cursorStart;
    }

    public void samplesReceived(int[] samples) {
        for (int i = 0; i < samples.length; ++i) {
            fftBuffer[cursor] = (double) samples[i];
            cursor++;
            if (cursor >= this.cursorEnd) {
                processFFTBuffer();
                shiftFFTBuffer();
            }
        }
    }
}
