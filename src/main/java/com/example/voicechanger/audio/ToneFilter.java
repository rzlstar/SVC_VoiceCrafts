package com.example.voicechanger.audio;

/**
 * One pole low pass filter, used for the "tone" (quality) slider.
 * <p>
 * Lower cutoffs make the voice sound muffled, higher cutoffs keep it clear.
 */
public class ToneFilter {

    private final double sampleRate;

    private double cutoffHz = 16000.0D;
    private double coefficient = 1.0D;
    private double state;

    public ToneFilter(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public void setCutoff(double cutoffHz) {
        double clamped = Math.max(20.0D, Math.min(sampleRate / 2.0D - 100.0D, cutoffHz));
        if (clamped == this.cutoffHz) {
            return;
        }
        this.cutoffHz = clamped;
        this.coefficient = 1.0D - Math.exp(-2.0D * Math.PI * clamped / sampleRate);
    }

    /**
     * @return true if the filter is (almost) transparent
     */
    public boolean isTransparent() {
        return cutoffHz >= 15500.0D;
    }

    public double process(double sample) {
        state += coefficient * (sample - state);
        return state;
    }
}
