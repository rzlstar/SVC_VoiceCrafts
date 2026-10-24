package com.example.voicechanger.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;

/**
 * Client side entry point: registers the key binding that opens the
 * voice changer settings screen.
 */
public class VoiceChangerClient implements ClientModInitializer {

    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("voicechanger", "main"));

    private static KeyMapping openSettingsKey;

    @Override
    public void onInitializeClient() {
        openSettingsKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.voicechanger.open",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_B,
                CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openSettingsKey.consumeClick()) {
                client.gui.setScreen(createScreen(client.gui.screen()));
            }
        });
    }

    /**
     * Creates the settings screen.
     *
     * @param parent the screen that is shown again when the settings are closed
     * @return the settings screen
     */
    public static Screen createScreen(Screen parent) {
        return new VoiceChangerScreen(parent);
    }
}
