package com.example.voicechanger.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Optional ModMenu integration, only loaded when ModMenu is installed.
 */
public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return VoiceChangerClient::createScreen;
    }
}
