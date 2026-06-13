package com.example.chatplus.chat;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ChatColorFormatter {
	private static final Pattern URL_PATTERN = Pattern.compile("https?://\\S+");

	private ChatColorFormatter() {
	}

	public static Component colorMessageBody(ChatMessage message, int bodyColor) {
		Component displayMessage = message.getDisplayMessage();
		int bodyStart = findBodyStart(displayMessage.getString(), message.getType());
		if (bodyStart <= 0) {
			return displayMessage.copy().withStyle(style -> style.withColor(bodyColor));
		}

		MutableComponent result = Component.empty();
		AtomicInteger position = new AtomicInteger();
		displayMessage.visit((style, text) -> {
			appendColoredSlice(result, text, style, position.get(), bodyStart, bodyColor);
			position.addAndGet(text.length());
			return Optional.empty();
		}, Style.EMPTY);
		return result;
	}

	public static Component linkifyUrls(Component message) {
		MutableComponent result = Component.empty();
		message.visit((style, text) -> {
			appendLinkifiedSlice(result, text, style);
			return Optional.empty();
		}, Style.EMPTY);
		return result;
	}

	private static void appendColoredSlice(MutableComponent result, String text, Style style, int position, int bodyStart, int bodyColor) {
		int textEnd = position + text.length();
		if (textEnd <= bodyStart) {
			result.append(Component.literal(text).withStyle(style));
			return;
		}

		if (position >= bodyStart) {
			result.append(Component.literal(text).withStyle(style.withColor(bodyColor)));
			return;
		}

		int splitIndex = bodyStart - position;
		result.append(Component.literal(text.substring(0, splitIndex)).withStyle(style));
		result.append(Component.literal(text.substring(splitIndex)).withStyle(style.withColor(bodyColor)));
	}

	private static void appendLinkifiedSlice(MutableComponent result, String text, Style style) {
		Matcher matcher = URL_PATTERN.matcher(text);
		int cursor = 0;
		while (matcher.find()) {
			if (matcher.start() > cursor) {
				result.append(Component.literal(text.substring(cursor, matcher.start())).withStyle(style));
			}

			String url = matcher.group();
			result.append(Component.literal(url).withStyle(style.withUnderlined(true).withClickEvent(createUrlClick(url))));
			cursor = matcher.end();
		}

		if (cursor < text.length()) {
			result.append(Component.literal(text.substring(cursor)).withStyle(style));
		}
	}

	private static ClickEvent createUrlClick(String url) {
		try {
			return new ClickEvent.OpenUrl(new URI(url));
		} catch (URISyntaxException exception) {
			return null;
		}
	}

	private static int findBodyStart(String message, ChatTabType type) {
		int separatorStart = findSeparatorStart(message);
		if (separatorStart >= 0) {
			return separatorStart + 3;
		}

		if (type == ChatTabType.WHISPER) {
			int colonIndex = message.indexOf(": ");
			if (colonIndex >= 0) {
				return colonIndex + 2;
			}
		}

		return type == ChatTabType.SYSTEM ? 0 : -1;
	}

	private static int findSeparatorStart(String message) {
		String[] separators = {" > ", " \u203a ", " \u00bb "};
		int bestIndex = -1;
		for (String separator : separators) {
			int index = message.indexOf(separator);
			if (index >= 0 && (bestIndex < 0 || index < bestIndex)) {
				bestIndex = index;
			}
		}
		return bestIndex;
	}
}
