package org.aquera.flutter.input.mic;

public class VoiceAnalysis {

    private double[] fftBuffer;
    private int bufferSize;
    private int cursor;
    private double freqBin;
    private double binOffset;
    AqueraInputMicStreamHandler streamHandler;

    public VoiceAnalysis(int fftSize, int bufferSize, int sampleFrequency, AqueraInputMicStreamHandler streamHandler) {
        this.fftBuffer = new double[fftSize];
        this.bufferSize = bufferSize;
        this.cursor = 0;
        this.streamHandler = streamHandler;
        freqBin = (sampleFrequency / 2.0) / (fftSize/2);
        binOffset = freqBin/2;
    }

    public void emitFrequencyPoint(double frequency, double dbs) {
        streamHandler.sendFrequencyPoint(frequency, dbs);
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
            if (modulus > dbs) {
                dbs = modulus;
                freq = (i * freqBin) + binOffset;
            }
        }
        dbs = 5 * Math.log10(dbs);
        emitFrequencyPoint(freq, dbs);
    }

    public void shiftFFTBuffer() {
        int count = fftBuffer.length - bufferSize;
        for (int i = 0, j = bufferSize; i < count; ++i, ++j) {
            fftBuffer[i] = fftBuffer[j];
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
