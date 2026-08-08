package com.example.chatplus.chat;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ChatColorFormatter {
	private static final Pattern URL_PATTERN = Pattern.compile("https?://\\S+");
	private static final Pattern HEART_PATTERN = Pattern.compile(":heart:", Pattern.CASE_INSENSITIVE);
	private static final Pattern BROKEN_HEART_PATTERN = Pattern.compile(":broken[ _-]?heart:", Pattern.CASE_INSENSITIVE);
	private static final Pattern FIRE_PATTERN = Pattern.compile(":fire:", Pattern.CASE_INSENSITIVE);
	private static final Pattern EYES_PATTERN = Pattern.compile(":eyes:", Pattern.CASE_INSENSITIVE);
	private static final Pattern ROSE_PATTERN = Pattern.compile(":rose:", Pattern.CASE_INSENSITIVE);
	private static final Pattern CHECK_PATTERN = Pattern.compile(":check:", Pattern.CASE_INSENSITIVE);
	private static final Pattern X_PATTERN = Pattern.compile(":x:", Pattern.CASE_INSENSITIVE);
	private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

	private ChatColorFormatter() {
	}

	public static Component colorMessageBody(ChatMessage message, int bodyColor) {
		return colorMessageBody(message, bodyColor, "");
	}

	public static Component colorMessageBody(ChatMessage message, int bodyColor, String playerName) {
		Component displayMessage = nicknamedDisplayMessage(message, playerName);
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

	public static Component prependTimestamp(Component message, long timestamp) {
		return Component.literal("[" + TIMESTAMP_FORMATTER.format(Instant.ofEpochMilli(timestamp)) + "] ")
				.withStyle(style -> style.withColor(0xFF888888))
				.append(message);
	}

	public static Component applyEmotes(Component message) {
		MutableComponent result = Component.empty();
		message.visit((style, text) -> {
			result.append(Component.literal(replaceEmotes(text)).withStyle(style));
			return Optional.empty();
		}, Style.EMPTY);
		return result;
	}

	public static Component discordStyle(ChatMessage message, int bodyColor) {
		return discordStyle(message, bodyColor, "");
	}

	public static Component discordStyle(ChatMessage message, int bodyColor, String playerName) {
		MutableComponent result = Component.empty();
		result.append(Component.literal("[" + TIMESTAMP_FORMATTER.format(Instant.ofEpochMilli(message.getTimestamp())) + "] ")
				.withStyle(style -> style.withColor(0xFF888888)));
		result.append(Component.literal(displaySender(message, playerName))
				.withStyle(style -> style.withColor(0xFFFFFFFF).withBold(true)));
		result.append(Component.literal(": ").withStyle(style -> style.withColor(0xFFAAAAAA)));
		result.append(Component.literal(message.getContent()).withStyle(style -> style.withColor(bodyColor)));
		return result;
	}

	private static Component nicknamedDisplayMessage(ChatMessage message, String playerName) {
		String target = usefulPlayerName(message, playerName);
		if (target.isBlank() || target.equals("You") || target.equals("SERVER") || target.equals("UNKNOWN")) {
			return message.getDisplayMessage();
		}

		String replacement = PlayerProfileStore.get(target).getNickname().trim();
		if (replacement.isBlank() || target.equals(replacement)) {
			return message.getDisplayMessage();
		}

		String fullText = message.getDisplayMessage().getString();
		int replacementStart = fullText.indexOf(target);
		if (replacementStart < 0) {
			return message.getDisplayMessage();
		}

		MutableComponent result = Component.empty();
		AtomicInteger position = new AtomicInteger();
		boolean[] inserted = {false};
		int replacementEnd = replacementStart + target.length();
		message.getDisplayMessage().visit((style, text) -> {
			appendReplacedSlice(result, text, style, position.get(), replacementStart, replacementEnd, replacement, inserted);
			position.addAndGet(text.length());
			return Optional.empty();
		}, Style.EMPTY);
		return inserted[0] ? result : message.getDisplayMessage();
	}

	private static String senderTextInDisplay(ChatMessage message) {
		String sender = message.getSender();
		if (sender.startsWith("You -> ")) {
			return sender.substring("You -> ".length()).trim();
		}
		return sender;
	}

	private static String displaySender(ChatMessage message, String playerName) {
		playerName = usefulPlayerName(message, playerName);
		if (playerName.isBlank() || playerName.equals("You") || playerName.equals("SERVER") || playerName.equals("UNKNOWN")) {
			return message.getSender();
		}
		return PlayerProfileStore.displayNameFor(playerName, message.getSender());
	}

	private static String usefulPlayerName(ChatMessage message, String playerName) {
		if (playerName != null && !playerName.isBlank()) {
			return playerName;
		}
		return ChatMessageParser.extractPlayerName(message.getSender());
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

	private static void appendReplacedSlice(MutableComponent result, String text, Style style, int position, int replacementStart, int replacementEnd, String replacement, boolean[] inserted) {
		int textEnd = position + text.length();
		if (textEnd <= replacementStart || position >= replacementEnd) {
			result.append(Component.literal(text).withStyle(style));
			return;
		}

		int prefixEnd = Math.max(0, replacementStart - position);
		if (prefixEnd > 0) {
			result.append(Component.literal(text.substring(0, prefixEnd)).withStyle(style));
		}

		if (!inserted[0]) {
			result.append(Component.literal(replacement).withStyle(style.withColor(0xFFFFFFFF)));
			inserted[0] = true;
		}

		int suffixStart = Math.max(0, replacementEnd - position);
		if (suffixStart < text.length()) {
			result.append(Component.literal(text.substring(suffixStart)).withStyle(style));
		}
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

		return type == ChatTabType.SYSTEM || type == ChatTabType.COREPROTECT ? 0 : -1;
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

	private static String replaceEmotes(String text) {
		String result = replaceEmoteCode(HEART_PATTERN, text, "\u2665");
		result = replaceEmoteCode(BROKEN_HEART_PATTERN, result, "\uD83D\uDC94");
		result = replaceEmoteCode(FIRE_PATTERN, result, "\uD83D\uDD25");
		result = replaceEmoteCode(EYES_PATTERN, result, "\uD83D\uDC40");
		result = replaceEmoteCode(ROSE_PATTERN, result, "\uD83C\uDF39");
		result = replaceEmoteCode(CHECK_PATTERN, result, "\u2713");
		result = replaceEmoteCode(X_PATTERN, result, "\u2715");
		return result
				.replace("<3", "\u2665")
				.replace("</3", "\uD83D\uDC94")
				.replace(":star:", "\u2605")
				.replace(":smile:", "\u263A")
				.replace(":sad:", "\u2639")
				.replace(":shrug:", "\u00AF\\_(\u30C4)_/\u00AF")
				.replace(":wilted_rose:", "\uD83E\uDD40")
				.replace(":skull:", "\u2620");
	}

	private static String replaceEmoteCode(Pattern pattern, String text, String replacement) {
		return pattern.matcher(text).replaceAll(Matcher.quoteReplacement(replacement));
	}
}
