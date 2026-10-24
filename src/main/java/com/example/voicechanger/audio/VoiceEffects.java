package com.example.voicechanger.audio;

import com.example.voicechanger.VoiceConfig;

/**
 * The complete effect chain that is applied to the microphone audio of the
 * local player before it is encoded and sent to the server.
 * <p>
 * Chain: pitch shift -&gt; tone (low pass) -&gt; echo -&gt; volume.
 */
public class VoiceEffects {

    public static final VoiceEffects INSTANCE = new VoiceEffects();

    private final PitchShifter pitchShifter = new PitchShifter(1024);
    private final EchoEffect echo = new EchoEffect(48000);
    private final ToneFilter tone = new ToneFilter(48000);

    private boolean wasEnabled = true;

    /**
     * Processes one raw microphone frame (16 bit PCM, 48kHz, mono).
     *
     * @param input the raw audio frame
     * @return the processed audio frame, same length as the input
     */
    public synchronized short[] process(short[] input) {
        VoiceConfig config = VoiceConfig.get();

        if (!config.enabled) {
            if (wasEnabled) {
                // Reset the internal state so that no old audio is played back
                // when the voice changer is enabled again.
                pitchShifter.reset();
                echo.reset();
                wasEnabled = false;
            }
            return input;
        }
        wasEnabled = true;

        if (input.length == 0) {
            return input;
        }

        double[] samples = new double[input.length];
        for (int i = 0; i < input.length; i++) {
            samples[i] = input[i] / 32768.0D;
        }

        // 1. Pitch shift
        double ratio = config.getPitchRatio();
        if (Math.abs(ratio - 1.0D) > 0.005D) {
            samples = pitchShifter.process(samples, ratio);
        } else {
            pitchShifter.reset();
        }

        // 2. Tone / quality
        tone.setCutoff(config.getToneCutoffHz());
        if (!tone.isTransparent()) {
            for (int i = 0; i < samples.length; i++) {
                samples[i] = tone.process(samples[i]);
            }
        }

        // 3. Echo
        echo.setParams(config.echoDelayMs, config.getEchoFeedbackRatio(), config.getEchoWetRatio());
        if (config.echoWet > 0 && config.echoDelayMs > 0 && config.echoFeedback > 0) {
            samples = echo.process(samples);
        } else {
            // Keep the delay line in sync with the current audio.
            echo.process(samples);
        }

        // 4. Volume
        double gain = config.getGainRatio();
        short[] output = new short[input.length];
        for (int i = 0; i < input.length; i++) {
            double value = samples[i] * gain;
            if (value > 1.0D) {
                value = 1.0D;
            } else if (value < -1.0D) {
                value = -1.0D;
            }
            output[i] = (short) Math.round(value * 32767.0D);
        }
        return output;
    }
}
