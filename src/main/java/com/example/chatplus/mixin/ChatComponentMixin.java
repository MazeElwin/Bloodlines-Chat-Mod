package com.example.chatplus.mixin;

import com.example.chatplus.chat.ChatInputMode;
import com.example.chatplus.chat.ChatColorFormatter;
import com.example.chatplus.chat.ChatMessage;
import com.example.chatplus.chat.ChatMessageParser;
import com.example.chatplus.chat.ChatPlusConfig;
import com.example.chatplus.chat.ChatTabManager;
import com.example.chatplus.chat.ChatTabType;
import com.example.chatplus.chat.ChatWindowState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Mixin(ChatComponent.class)
public class ChatComponentMixin {
	private static final int PANEL_PADDING = 4;
	private static final int HUD_HEIGHT = 72;
	private static final int HUD_MAX_WIDTH = 520;
	private static final int HEAD_SIZE = 8;
	private static final int HEAD_GAP = 3;
	private static final int HEAD_Y_OFFSET = -2;
	private static final int BACKGROUND_X_PADDING = 5;
	private static final int BACKGROUND_Y_OFFSET = HEAD_Y_OFFSET;
	private static final int BACKGROUND_FADE_WIDTH = 28;
	private static final int PRIVATE_INDICATOR_HEIGHT = 14;
	private static final int DEATH_BACKGROUND_COLOR = 0x99AA2222;
	private static final long FADE_IN_MILLIS = 250L;
	private static final long HOLD_MILLIS = 7000L;
	private static final long FADE_OUT_MILLIS = 3000L;
	private static final FontDescription.Resource LEXEND_FONT = new FontDescription.Resource(Identifier.fromNamespaceAndPath("bloodline-chat", "lexend"));

	@Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
	private void chatplus$replaceVanillaChatHud(
			GuiGraphicsExtractor graphics,
			Font font,
			int tickCount,
			int mouseX,
			int mouseY,
			ChatComponent.DisplayMode displayMode,
			boolean focused,
			CallbackInfo ci
	) {
		ci.cancel();

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.screen instanceof ChatScreen || minecraft.options.hideGui) {
			return;
		}

		ChatPlusConfig.Settings settings = ChatPlusConfig.get();
		if (ChatTabManager.getInstance().hasUnreadWhispers()) {
			chatplus$renderPrivateIndicator(graphics, font, settings);
		}

		List<ChatMessage> messages = chatplus$getHudMessages();
		if (messages.isEmpty()) {
			return;
		}

		ChatWindowState.clampToScreen(graphics.guiWidth(), graphics.guiHeight());
		int panelX = ChatWindowState.getX();
		int panelWidth = Math.min(ChatWindowState.getWidth(graphics.guiWidth()), Math.min(HUD_MAX_WIDTH, graphics.guiWidth() - 8));
		int panelBottom = Math.min(graphics.guiHeight() - 4, ChatWindowState.getY(graphics.guiHeight()) + ChatWindowState.getHeight(graphics.guiHeight()));
		int panelY = Math.max(4, panelBottom - Math.min(HUD_HEIGHT, graphics.guiHeight() / 3));
		int left = panelX + PANEL_PADDING;
		int right = panelX + panelWidth - PANEL_PADDING;
		int bottom = panelBottom - PANEL_PADDING;
		int top = panelY + PANEL_PADDING;
		int clipTop = Math.max(0, top + Math.min(0, HEAD_Y_OFFSET));
		int textLeft = settings.chatHeadsEnabled ? left + HEAD_SIZE + HEAD_GAP : left;
		int textRight = right;
		float scale = settings.textSize / 100.0F;
		int lineHeight = Math.max(HEAD_SIZE, Math.round(9.0F * scale) + settings.lineSpacing);
		int lineY = bottom - lineHeight;
		long now = System.currentTimeMillis();

		graphics.enableScissor(left, clipTop, right, bottom);
		for (int i = messages.size() - 1; i >= 0 && lineY >= top; i--) {
			ChatMessage message = messages.get(i);
			float fade = chatplus$getFade(message, now);
			if (fade <= 0.0F) {
				continue;
			}

			Component displayMessage = chatplus$formatMessage(message, settings);
			List<FormattedCharSequence> lines = font.split(displayMessage, Math.max(20, (int) ((textRight - textLeft) / scale)));
			int messageHeight = lines.size() * lineHeight;
			int messageTop = lineY - messageHeight + lineHeight;
			boolean deathMessage = chatplus$isSystemDeathMessage(message);
			if (messageTop < top) {
				break;
			}

			for (int lineIndex = lines.size() - 1; lineIndex >= 0; lineIndex--) {
				int backgroundAlpha = settings.hudBackgroundEnabled ? Math.round(chatplus$opacityToAlpha(settings.backgroundOpacity) * 0.45F * fade) : 0;
				int textAlpha = Math.round(255.0F * fade);
				int backgroundColor = deathMessage ? chatplus$withAlpha(DEATH_BACKGROUND_COLOR, Math.round(153.0F * fade)) : chatplus$withAlpha(0x000000, backgroundAlpha);
				if (backgroundAlpha > 0 || deathMessage) {
					int textEnd = textLeft + Math.round(font.width(lines.get(lineIndex)) * scale) + BACKGROUND_X_PADDING;
					int backgroundTop = lineY + BACKGROUND_Y_OFFSET;
					chatplus$fillFadingBackground(graphics, left - 2, backgroundTop, Math.min(right + 2, textEnd), backgroundTop + lineHeight, backgroundColor);
				}
				chatplus$drawScaledString(graphics, font, lines.get(lineIndex), textLeft, lineY, chatplus$withAlpha(0xFFFFFF, textAlpha), scale, settings.fontStyleIndex == 0);
				lineY -= lineHeight;
			}

			if (settings.chatHeadsEnabled) {
				chatplus$renderPlayerHead(graphics, minecraft, message, left, messageTop);
			}
		}
		graphics.disableScissor();
	}

	private void chatplus$renderPrivateIndicator(GuiGraphicsExtractor graphics, Font font, ChatPlusConfig.Settings settings) {
		String label = "PM";
		int width = font.width(label) + 10;
		int x = Math.max(0, Math.min(settings.privateIndicatorX, graphics.guiWidth() - width));
		int y = Math.max(0, Math.min(settings.privateIndicatorY, graphics.guiHeight() - PRIVATE_INDICATOR_HEIGHT));
		boolean flash = (System.currentTimeMillis() / 350L) % 2L == 0L;
		graphics.fill(x, y, x + width, y + PRIVATE_INDICATOR_HEIGHT, flash ? 0xCC884444 : 0xAA333333);
		graphics.outline(x, y, width, PRIVATE_INDICATOR_HEIGHT, 0xFFFFAAAA);
		graphics.text(font, label, x + (width - font.width(label)) / 2, y + 3, 0xFFFFDDDD, false);
	}

	private float chatplus$getFade(ChatMessage message, long now) {
		long age = now - message.getTimestamp();
		if (age < 0L) {
			return 1.0F;
		}

		if (age < FADE_IN_MILLIS) {
			return Math.max(0.0F, age / (float) FADE_IN_MILLIS);
		}

		if (age <= HOLD_MILLIS) {
			return 1.0F;
		}

		long fadeOutAge = age - HOLD_MILLIS;
		if (fadeOutAge >= FADE_OUT_MILLIS) {
			return 0.0F;
		}

		return Math.max(0.0F, 1.0F - fadeOutAge / (float) FADE_OUT_MILLIS);
	}

	private List<ChatMessage> chatplus$getHudMessages() {
		long cutoff = System.currentTimeMillis() - (FADE_IN_MILLIS + HOLD_MILLIS + FADE_OUT_MILLIS);
		List<ChatMessage> selected = switch (ChatInputMode.getSelected()) {
			case SERVER -> ChatTabManager.getInstance().getParsedTabMessagesSince(ChatTabType.SYSTEM, cutoff);
			case LOCAL -> ChatTabManager.getInstance().getParsedTabMessagesSince(ChatTabType.LOCAL_CHAT, cutoff);
			case TEAM -> ChatTabManager.getInstance().getParsedTabMessagesSince(ChatTabType.TEAM_CHAT, cutoff);
			case GENERAL -> ChatTabManager.getInstance().getParsedTabMessagesSince(ChatTabType.GENERAL, cutoff);
			case COREPROTECT -> ChatTabManager.getInstance().getCoreProtectMessagesSince(cutoff);
			case ALL -> ChatTabManager.getInstance().getAllParsedMessagesSince(cutoff);
		};
		if (ChatInputMode.getSelected() == ChatInputMode.ALL) {
			return selected;
		}

		List<ChatMessage> merged = null;
		if (chatplus$showServerMessagesInSelectedTab()) {
			for (ChatMessage message : ChatTabManager.getInstance().getParsedTabMessagesSince(ChatTabType.SYSTEM, cutoff)) {
				if (!selected.contains(message) && (merged == null || !merged.contains(message))) {
					if (merged == null) {
						merged = new ArrayList<>(selected);
					}
					merged.add(message);
				}
			}
		}
		for (ChatMessage message : ChatTabManager.getInstance().getAllParsedMessagesSince(cutoff)) {
			if (chatplus$isSystemDeathMessage(message)
					&& !selected.contains(message)
					&& (merged == null || !merged.contains(message))) {
				if (merged == null) {
					merged = new ArrayList<>(selected);
				}
				merged.add(message);
			}
		}
		if (merged == null) {
			return selected;
		}
		merged.sort((first, second) -> Long.compare(first.getTimestamp(), second.getTimestamp()));
		return merged;
	}

	private boolean chatplus$showServerMessagesInSelectedTab() {
		ChatPlusConfig.Settings settings = ChatPlusConfig.get();
		return switch (ChatInputMode.getSelected()) {
			case GENERAL -> settings.showServerMessagesInGeneral;
			case LOCAL -> settings.showServerMessagesInLocal;
			case TEAM -> settings.showServerMessagesInTeam;
			default -> false;
		};
	}

	private int chatplus$getMessageColor(ChatMessage message) {
		ChatPlusConfig.Settings settings = ChatPlusConfig.get();
		return switch (message.getType()) {
			case LOCAL_CHAT -> settings.localColor;
			case WHISPER -> settings.whisperColor;
			case TEAM_CHAT -> settings.teamColor;
			case SYSTEM -> settings.systemColor;
			case COREPROTECT -> settings.systemColor;
			default -> settings.generalColor;
		};
	}

	private void chatplus$renderPlayerHead(GuiGraphicsExtractor graphics, Minecraft minecraft, ChatMessage message, int x, int y) {
		if (minecraft.getConnection() == null) {
			return;
		}

		PlayerInfo playerInfo = chatplus$playerInfoFor(minecraft, message);
		if (playerInfo == null) {
			return;
		}

		Identifier texture = playerInfo.getSkin().body().texturePath();
		int headY = y + HEAD_Y_OFFSET;
		graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, headY, 8.0F, 8.0F, HEAD_SIZE, HEAD_SIZE, 64, 64);
		graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, headY, 40.0F, 8.0F, HEAD_SIZE, HEAD_SIZE, 64, 64);
	}

	private PlayerInfo chatplus$playerInfoFor(Minecraft minecraft, ChatMessage message) {
		if (!message.getSenderId().isBlank()) {
			try {
				PlayerInfo playerInfo = minecraft.getConnection().getPlayerInfo(UUID.fromString(message.getSenderId()));
				if (playerInfo != null) {
					return playerInfo;
				}
			} catch (IllegalArgumentException ignored) {
			}
		}

		String playerName = chatplus$profileNameFor(minecraft, message);
		return playerName.isBlank() ? null : minecraft.getConnection().getPlayerInfo(playerName);
	}

	private String chatplus$profileNameFor(Minecraft minecraft, ChatMessage message) {
		if (message.getType() == ChatTabType.SYSTEM || message.getSender().isBlank() || message.getSender().equals("UNKNOWN")) {
			return "";
		}

		if (message.isOutgoing() && minecraft.getUser() != null) {
			return minecraft.getUser().getName();
		}

		if (!message.getSenderId().isBlank() && minecraft.getConnection() != null) {
			try {
				PlayerInfo playerInfo = minecraft.getConnection().getPlayerInfo(UUID.fromString(message.getSenderId()));
				if (playerInfo != null && playerInfo.getProfile().name() != null) {
					return playerInfo.getProfile().name();
				}
			} catch (IllegalArgumentException ignored) {
			}
		}

		String cleaned = message.getSender();
		if (cleaned.startsWith("You -> ")) {
			cleaned = cleaned.substring("You -> ".length());
		}

		String onlinePlayer = chatplus$onlinePlayerSuffix(minecraft, cleaned.trim());
		if (!onlinePlayer.isBlank()) {
			return onlinePlayer;
		}

		String[] parts = cleaned.trim().split("\\s+");
		return parts.length == 0 ? cleaned.trim() : parts[parts.length - 1];
	}

	private String chatplus$onlinePlayerSuffix(Minecraft minecraft, String displayName) {
		if (minecraft.getConnection() == null || displayName.isBlank()) {
			return "";
		}

		String bestMatch = "";
		for (PlayerInfo playerInfo : minecraft.getConnection().getOnlinePlayers()) {
			String name = playerInfo.getProfile().name();
			if (name == null || name.isBlank()) {
				continue;
			}
			if (displayName.endsWith(name) && name.length() > bestMatch.length()) {
				bestMatch = name;
			}
		}
		return bestMatch;
	}

	private Component chatplus$applySelectedFont(ChatMessage chatMessage, Component message, ChatPlusConfig.Settings settings) {
		if (chatMessage.getType() == ChatTabType.COREPROTECT || (settings.fontStyleIndex != 1 && settings.fontStyleIndex != 2)) {
			return message;
		}

		return message.copy().withStyle(style -> style.withFont(LEXEND_FONT));
	}

	private Component chatplus$formatMessage(ChatMessage message, ChatPlusConfig.Settings settings) {
		String playerName = chatplus$profileNameFor(Minecraft.getInstance(), message);
		Component formatted = settings.fontStyleIndex == 2 && message.getType() != ChatTabType.COREPROTECT
				? ChatColorFormatter.discordStyle(message, chatplus$getMessageColor(message), playerName)
				: ChatColorFormatter.colorMessageBody(message, chatplus$getMessageColor(message), playerName);
		if (settings.emotesEnabled && message.getType() != ChatTabType.COREPROTECT) {
			formatted = ChatColorFormatter.applyEmotes(formatted);
		}
		formatted = ChatColorFormatter.linkifyUrls(formatted);
		if (settings.timestampsEnabled && settings.fontStyleIndex != 2) {
			formatted = ChatColorFormatter.prependTimestamp(formatted, message.getTimestamp());
		}
		return chatplus$applySelectedFont(message, formatted, settings);
	}

	private void chatplus$drawScaledString(GuiGraphicsExtractor graphics, Font font, FormattedCharSequence text, int x, int y, int color, float scale, boolean shadow) {
		graphics.pose().pushMatrix();
		graphics.pose().scale(scale, scale);
		graphics.text(font, text, (int) (x / scale), (int) (y / scale), color, shadow);
		graphics.pose().popMatrix();
	}

	private int chatplus$opacityToAlpha(int opacity) {
		return Math.max(1, Math.min(255, Math.round(Math.max(1, Math.min(100, opacity)) * 255.0F / 100.0F)));
	}

	private int chatplus$withAlpha(int rgb, int alpha) {
		return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0x00FFFFFF);
	}

	private void chatplus$fillFadingBackground(GuiGraphicsExtractor graphics, int left, int top, int right, int bottom, int color) {
		int alpha = (color >>> 24) & 0xFF;
		if (alpha <= 0 || right <= left) {
			return;
		}

		int fadeStart = Math.max(left, right - BACKGROUND_FADE_WIDTH);
		if (fadeStart > left) {
			graphics.fill(left, top, fadeStart, bottom, color);
		}

		int fadeWidth = Math.max(1, right - fadeStart);
		for (int x = fadeStart; x < right; x++) {
			float remaining = (right - x) / (float) fadeWidth;
			graphics.fill(x, top, x + 1, bottom, chatplus$withAlpha(color, Math.round(alpha * remaining)));
		}
	}

	private boolean chatplus$isSystemDeathMessage(ChatMessage message) {
		return message.getType() == ChatTabType.SYSTEM && ChatMessageParser.isDeathMessage(message.getRawMessage());
	}
}
