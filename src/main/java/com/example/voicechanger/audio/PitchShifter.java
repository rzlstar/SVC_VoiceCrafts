package com.example.voicechanger.audio;

/**
 * Real time pitch shifter based on two modulated delay taps (a classic
 * "delay line pitch shifter").
 * <p>
 * Both taps read from the history buffer with delay
 * {@code D + a} and {@code D + a + L / 2}, where the accumulator {@code a} drifts
 * by {@code 1 - ratio} per sample and wraps around {@code L / 2}. The two taps are
 * cross faded power complementary (sin^2 / cos^2), so the wrap is inaudible.
 * <p>
 * The implementation keeps its state between calls, which means the incoming
 * 20ms audio frames of the voice chat are processed as one continuous stream.
 * <p>
 * This is a time domain effect: the pitch is shifted without changing the
 * duration of the signal. It is intentionally simple and cheap, the typical
 * artefacts of this kind of shifter ("chipmunk" / "monster" character) are
 * wanted for a voice changer.
 */
public class PitchShifter {

    /**
     * Grain period in samples. 1024 samples at 48kHz equals roughly 21ms.
     */
    private final int period;
    private final int halfPeriod;
    /**
     * Base delay of the read taps in samples.
     */
    private final double baseDelay;
    /**
     * Ring buffer for the input history.
     */
    private final float[] buffer;
    private final int bufferMask;
    /**
     * Absolute write position.
     */
    private int writePos;
    /**
     * Delay accumulator, always in {@code [0, halfPeriod)}.
     */
    private double delay;

    public PitchShifter(int period) {
        this.period = period;
        this.halfPeriod = period / 2;
        this.baseDelay = period + 64;
        int size = 1;
        while (size < 32768) {
            size <<= 1;
        }
        this.buffer = new float[size];
        this.bufferMask = size - 1;
        reset();
    }

    public void reset() {
        java.util.Arrays.fill(buffer, 0.0F);
        writePos = 0;
        delay = halfPeriod;
    }

    /**
     * Shifts the pitch of the given samples.
     *
     * @param input the input samples in the range {@code [-1, 1]}
     * @param ratio the pitch factor, {@code 1.0} is "no change"
     * @return the shifted samples, same length as the input
     */
    public double[] process(double[] input, double ratio) {
        double[] output = new double[input.length];
        double drift = 1.0D - ratio;
        for (int i = 0; i < input.length; i++) {
            buffer[writePos & bufferMask] = (float) input[i];

            delay += drift;
            if (delay < 0.0D) {
                delay += halfPeriod;
            }
            if (delay >= halfPeriod) {
                delay -= halfPeriod;
            }

            double t = delay / halfPeriod;
            double sin = Math.sin(Math.PI / 2.0D * t);
            double cos = Math.cos(Math.PI / 2.0D * t);
            double gainA = sin * sin;
            double gainB = cos * cos;

            double a = read(writePos - (baseDelay + delay));
            double b = read(writePos - (baseDelay + delay + halfPeriod));

            output[i] = gainA * a + gainB * b;
            writePos++;
        }
        return output;
    }

    /**
     * Cubic (Catmull-Rom) interpolated read from the history buffer.
     *
     * @param position the fractional read position
     * @return the interpolated sample
     */
    private double read(double position) {
        int index = (int) Math.floor(position);
        double frac = position - index;
        float p0 = buffer[(index - 1) & bufferMask];
        float p1 = buffer[index & bufferMask];
        float p2 = buffer[(index + 1) & bufferMask];
        float p3 = buffer[(index + 2) & bufferMask];
        return p1 + 0.5D * frac * (p2 - p0 + frac * (2.0D * p0 - 5.0D * p1 + 4.0D * p2 - p3
                + frac * (3.0D * (p1 - p2) + p3 - p0)));
    }
}
