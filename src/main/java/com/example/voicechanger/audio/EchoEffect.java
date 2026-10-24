package com.example.voicechanger.audio;

/**
 * Simple feedback delay (echo) effect.
 */
public class EchoEffect {

    private final float[] buffer;
    private int writePos;

    private int delaySamples;
    private double feedback;
    private double wet;

    public EchoEffect(int sampleRate) {
        this.buffer = new float[sampleRate];
        this.delaySamples = 0;
    }

    public void setParams(int delayMs, double feedback, double wet) {
        this.delaySamples = Math.max(0, Math.min(buffer.length - 1, delayMs * 48));
        this.feedback = Math.max(0.0D, Math.min(0.9D, feedback));
        this.wet = Math.max(0.0D, Math.min(1.0D, wet));
    }

    public void reset() {
        java.util.Arrays.fill(buffer, 0.0F);
        writePos = 0;
    }

    /**
     * Adds an echo to the given samples.
     *
     * @param input the input samples
     * @return the processed samples, same length as the input
     */
    public double[] process(double[] input) {
        if (delaySamples <= 0 || wet <= 0.0D || feedback <= 0.0D) {
            // Still advance the write position so that switching the echo on later
            // does not produce a burst of old audio.
            for (int i = 0; i < input.length; i++) {
                buffer[writePos] = (float) input[i];
                writePos = (writePos + 1) % buffer.length;
            }
            return input;
        }
        double[] output = new double[input.length];
        for (int i = 0; i < input.length; i++) {
            int readPos = (writePos - delaySamples + buffer.length) % buffer.length;
            double delayed = buffer[readPos];
            buffer[writePos] = (float) (input[i] + feedback * delayed);
            writePos = (writePos + 1) % buffer.length;
            output[i] = input[i] + wet * delayed;
        }
        return output;
    }
}
