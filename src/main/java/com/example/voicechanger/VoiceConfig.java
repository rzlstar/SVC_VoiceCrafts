package com.example.voicechanger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Persistent settings of the voice changer.
 * <p>
 * The values are the raw slider values. Use the helper methods below to get the
 * values in the unit the audio processing expects.
 */
public class VoiceConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static VoiceConfig instance = new VoiceConfig();

    /**
     * Whether the voice changer is active at all.
     */
    public boolean enabled = true;

    /**
     * Pitch offset in semitones, -24 to +24.
     */
    public double pitchSemitones = 0.0D;

    /**
     * Echo delay in milliseconds, 0 to 1000.
     */
    public int echoDelayMs = 150;

    /**
     * Echo feedback in percent, 0 to 90.
     */
    public int echoFeedback = 35;

    /**
     * How much echo is mixed into the signal, in percent, 0 to 100.
     */
    public int echoWet = 0;

    /**
     * Tone / "quality" slider in percent.
     * <p>
     * 0 = muffled / "telephone" like sound, 100 = clear.
     */
    public int tone = 100;

    /**
     * Output volume in percent, 50 to 200.
     */
    public int gain = 100;

    public static VoiceConfig get() {
        return instance;
    }

    public static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("voicechanger.json");
    }

    /**
     * Loads the config from disk. Falls back to the defaults if anything fails.
     */
    public static void load() {
        Path path = getConfigPath();
        if (!Files.exists(path)) {
            save();
            return;
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            VoiceConfig loaded = GSON.fromJson(reader, VoiceConfig.class);
            if (loaded != null) {
                loaded.validate();
                instance = loaded;
            }
        } catch (Exception e) {
            VoiceChangerMod.LOGGER.warn("Could not read the config, using defaults", e);
        }
    }

    public static void save() {
        instance.validate();
        Path path = getConfigPath();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(instance, writer);
            }
        } catch (IOException e) {
            VoiceChangerMod.LOGGER.warn("Could not save the config", e);
        }
    }

    /**
     * Clamps all values so that a hand edited config file cannot break the audio chain.
     */
    private void validate() {
        pitchSemitones = clamp(pitchSemitones, -24.0D, 24.0D);
        echoDelayMs = clamp(echoDelayMs, 0, 1000);
        echoFeedback = clamp(echoFeedback, 0, 90);
        echoWet = clamp(echoWet, 0, 100);
        tone = clamp(tone, 0, 100);
        gain = clamp(gain, 50, 200);
    }

    public void reset() {
        enabled = true;
        pitchSemitones = 0.0D;
        echoDelayMs = 150;
        echoFeedback = 35;
        echoWet = 0;
        tone = 100;
        gain = 100;
    }

    /**
     * @return the pitch factor, 1.0 means "no change", 2.0 one octave up
     */
    public double getPitchRatio() {
        return Math.pow(2.0D, pitchSemitones / 12.0D);
    }

    /**
     * Maps the tone slider (0-100) to a low pass cutoff frequency in Hz.
     *
     * @return the cutoff frequency in Hz
     */
    public double getToneCutoffHz() {
        double min = 400.0D;
        double max = 16000.0D;
        return min * Math.pow(max / min, tone / 100.0D);
    }

    public double getEchoFeedbackRatio() {
        return echoFeedback / 100.0D;
    }

    public double getEchoWetRatio() {
        return echoWet / 100.0D;
    }

    public double getGainRatio() {
        return gain / 100.0D;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
