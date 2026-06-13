package com.example.chatplus.chat;

import com.example.chatplus.ChatPlusMod;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public final class RawChatLog {
	private static final Path LOG_PATH = FabricLoader.getInstance()
			.getConfigDir()
			.resolve(ChatPlusMod.MOD_ID)
			.resolve("raw-chat.log");

	private RawChatLog() {
	}

	public static void append(ChatMessage message) {
		try {
			Files.createDirectories(LOG_PATH.getParent());

			try (BufferedWriter writer = Files.newBufferedWriter(
					LOG_PATH,
					StandardCharsets.UTF_8,
					StandardOpenOption.CREATE,
					StandardOpenOption.APPEND
			)) {
				writer.write(format(message));
				writer.newLine();
			}
		} catch (IOException exception) {
			ChatPlusMod.LOGGER.warn("Failed to write raw chat log", exception);
		}
	}

	public static List<ChatMessage> loadHistory(int retentionDays) {
		List<ChatMessage> messages = new ArrayList<>();
		if (!Files.exists(LOG_PATH)) {
			return messages;
		}

		try {
			for (String line : Files.readAllLines(LOG_PATH, StandardCharsets.UTF_8)) {
				ChatMessage message = parseLine(line);
				if (message != null) {
					messages.add(message);
				}
			}
		} catch (IOException exception) {
			ChatPlusMod.LOGGER.warn("Failed to read raw chat log", exception);
		}
		return messages;
	}

	public static void prune(int retentionDays) {
		if (retentionDays <= 0) {
			return;
		}

		if (!Files.exists(LOG_PATH)) {
			return;
		}

		long cutoff = System.currentTimeMillis() - Duration.ofDays(Math.max(1, retentionDays)).toMillis();
		try {
			List<String> keptLines = new ArrayList<>();
			for (String line : Files.readAllLines(LOG_PATH, StandardCharsets.UTF_8)) {
				long timestamp = parseTimestamp(line);
				if (timestamp == 0L || timestamp >= cutoff) {
					keptLines.add(line);
				}
			}
			Files.write(LOG_PATH, keptLines, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException exception) {
			ChatPlusMod.LOGGER.warn("Failed to prune raw chat log", exception);
		}
	}

	private static String format(ChatMessage message) {
		String direction = message.isOutgoing() ? "OUT" : "IN";
		return Instant.ofEpochMilli(message.getTimestamp())
				+ "\t" + direction
				+ "\t" + message.getType().name()
				+ "\t" + clean(message.getSender())
				+ "\t" + clean(message.getStyleTrace())
				+ "\t" + clean(message.getWhisperPartner())
				+ "\t" + clean(message.getRawMessage());
	}

	private static ChatMessage parseLine(String line) {
		String[] parts = line.split("\t", -1);
		if (parts.length < 6) {
			return null;
		}

		try {
			long timestamp = Instant.parse(parts[0]).toEpochMilli();
			boolean outgoing = "OUT".equalsIgnoreCase(parts[1]);
			ChatTabType type = ChatTabType.valueOf(parts[2]);
			String sender = unclean(parts[3]);
			String styleTrace = unclean(parts[4]);
			String whisperPartner;
			String rawMessage;
			if (parts.length >= 7) {
				whisperPartner = unclean(parts[5]);
				rawMessage = unclean(parts[6]);
			} else {
				rawMessage = unclean(parts[5]);
				whisperPartner = type == ChatTabType.WHISPER ? ChatMessageParser.extractPlayerName(sender) : "";
			}

			return new ChatMessage(
					rawMessage,
					Component.literal(rawMessage),
					type,
					sender,
					rawMessage,
					outgoing,
					styleTrace,
					whisperPartner,
					timestamp
			);
		} catch (IllegalArgumentException | DateTimeParseException exception) {
			return null;
		}
	}

	private static long parseTimestamp(String line) {
		int tabIndex = line.indexOf('\t');
		if (tabIndex <= 0) {
			return 0L;
		}

		try {
			return Instant.parse(line.substring(0, tabIndex)).toEpochMilli();
		} catch (DateTimeParseException exception) {
			return 0L;
		}
	}

	private static String clean(String value) {
		return value
				.replace("\\", "\\\\")
				.replace("\t", "\\t")
				.replace("\r", "\\r")
				.replace("\n", "\\n");
	}

	private static String unclean(String value) {
		StringBuilder result = new StringBuilder();
		boolean escaping = false;
		for (int i = 0; i < value.length(); i++) {
			char character = value.charAt(i);
			if (escaping) {
				result.append(switch (character) {
					case 't' -> '\t';
					case 'r' -> '\r';
					case 'n' -> '\n';
					default -> character;
				});
				escaping = false;
			} else if (character == '\\') {
				escaping = true;
			} else {
				result.append(character);
			}
		}
		return result.toString();
	}
}
