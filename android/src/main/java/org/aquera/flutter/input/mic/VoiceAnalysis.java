package org.aquera.flutter.input.mic;

public class VoiceAnalysis {
    public static final double LOWER_ALLOWED_FREQUENCY = 15;

    private double[] fftBuffer;
    private int bufferSize;
    private int cursor;
    private double freqBin;
    private double binOffset;
    private double lastFrequencyPoint;
    AqueraInputMicStreamHandler streamHandler;

    public VoiceAnalysis(int fftSize, int bufferSize, int sampleFrequency, AqueraInputMicStreamHandler streamHandler) {
        this.fftBuffer = new double[fftSize];
        this.bufferSize = bufferSize;
        this.cursor = fftSize - bufferSize;
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
        double normFactor = 1.0/Math.sqrt(data.length);
        for (int i = 0; i < data.length; ++i) {
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
        dbs = 2 * Math.log10(dbs);
        if (freq > 0) {
            emitFrequencyPoint(freq, dbs);
        }
    }

    public void shiftFFTBuffer() {
        int count = fftBuffer.length - bufferSize;
        for (int i = 0, j = bufferSize; i < count; ++i, ++j) {
            //fftBuffer[i] = fftBuffer[j];
            fftBuffer[0] = 0;
        }
        cursor = count;
    }

    public void samplesReceived(int[] samples) {
        for (int i = 0; i < samples.length; ++i) {
            fftBuffer[cursor] = (double) samples[i];
            cursor++;
            if (cursor >= fftBuffer.length) {
                processFFTBuffer();
                shiftFFTBuffer();
            }
        }
    }

}
