package com.example.voicechanger.voicechat;

import com.example.voicechanger.audio.VoiceEffects;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.ClientSoundEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;

/**
 * Simple Voice Chat plugin that modifies the microphone audio of the local
 * player right before it is encoded and sent to the server.
 *
 * @see com.example.voicechanger.audio.VoiceEffects
 */
public class VoiceChangerPlugin implements VoicechatPlugin {

    public static final String PLUGIN_ID = "voicechanger";

    @Override
    public String getPluginId() {
        return PLUGIN_ID;
    }

    @Override
    public void initialize(VoicechatApi api) {
        // Nothing to do here
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        // Fired on the client, before the raw microphone audio is encoded.
        registration.registerEvent(ClientSoundEvent.class, this::onClientSound);
    }

    private void onClientSound(ClientSoundEvent event) {
        short[] audio = event.getRawAudio();
        if (audio == null || audio.length == 0) {
            return;
        }
        event.setRawAudio(VoiceEffects.INSTANCE.process(audio));
    }
}
