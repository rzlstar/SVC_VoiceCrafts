package com.example.voicechanger;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common entry point of the mod.
 * <p>
 * The actual audio processing is registered through the Simple Voice Chat API
 * (see {@link com.example.voicechanger.voicechat.VoiceChangerPlugin}) and the
 * user interface is provided by the client entry point
 * (see {@link com.example.voicechanger.client.VoiceChangerClient}).
 */
public class VoiceChangerMod implements ModInitializer {

    public static final String MOD_ID = "voicechanger";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        VoiceConfig.load();
        LOGGER.info("Voice Changer loaded");
    }
}
