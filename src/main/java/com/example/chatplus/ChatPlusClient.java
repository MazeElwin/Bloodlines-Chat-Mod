package com.example.chatplus;

import com.example.chatplus.chat.ChatTabManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;

public class ChatPlusClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ChatPlusMod.LOGGER.info("Chat Plus client initialized!");

		ClientReceiveMessageEvents.CHAT.register((message, signedMessage, gameProfile, params, receptionTimestamp) -> {
			ChatTabManager.getInstance().handleIncomingMessage(message);
		});

		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			if (!overlay) {
				ChatTabManager.getInstance().handleIncomingMessage(message);
			}
		});

	}
}
