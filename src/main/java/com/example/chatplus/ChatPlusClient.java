package com.example.chatplus;

import com.example.chatplus.chat.ChatTabManager;
import com.example.chatplus.chat.GhostTimerManager;
import com.example.chatplus.chat.SkillProgressTracker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public class ChatPlusClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ChatPlusMod.LOGGER.info("Chat Plus client initialized!");

		ClientReceiveMessageEvents.CHAT.register((message, signedMessage, gameProfile, params, receptionTimestamp) -> {
			SkillProgressTracker.handleIncomingMessage(message);
			UUID senderId = gameProfile == null ? null : gameProfile.id();
			ChatTabManager.getInstance().handleIncomingMessage(message, senderId == null ? "" : senderId.toString(), localPlayerName());
		});

		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			if (!overlay) {
				SkillProgressTracker.handleIncomingMessage(message);
				ChatTabManager.getInstance().handleIncomingMessage(message, "", localPlayerName());
			}
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			GhostTimerManager.tick();
			SkillProgressTracker.tickAutoCheck();
		});

		ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (screen instanceof AbstractContainerScreen<?>) {
				ScreenEvents.afterExtract(screen).register((renderedScreen, graphics, mouseX, mouseY, tickProgress) -> {
					SkillProgressTracker.renderPopup(graphics, renderedScreen.getFont(), graphics.guiWidth());
				});
			}
		});

		HudElementRegistry.addLast(
				Identifier.fromNamespaceAndPath(ChatPlusMod.MOD_ID, "skill_progress_popup"),
				(graphics, deltaTracker) -> {
					Minecraft minecraft = Minecraft.getInstance();
					if (!(minecraft.screen instanceof AbstractContainerScreen<?>)) {
						SkillProgressTracker.renderPopup(graphics, minecraft.font, graphics.guiWidth());
					}
				}
		);
	}

	private static String localPlayerName() {
		return Minecraft.getInstance().getUser() == null ? "" : Minecraft.getInstance().getUser().getName();
	}
}
