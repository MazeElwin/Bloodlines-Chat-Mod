package com.example.chatplus.chat;

import com.example.chatplus.ChatPlusMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PlayerProfileStore {
	private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve(ChatPlusMod.MOD_ID);
	private static final Path FAMILY_PATH = CONFIG_DIR.resolve("player-family.tsv");
	private static final Path NOTES_PATH = CONFIG_DIR.resolve("player-notes.tsv");

	private static final Map<String, Family> families = new LinkedHashMap<>();
	private static final Map<String, String> notes = new LinkedHashMap<>();
	private static boolean loaded;

	private PlayerProfileStore() {
	}

	public static PlayerProfileData get(String playerName) {
		load();
		Family family = families.getOrDefault(playerName, new Family("", "", "", ""));
		return new PlayerProfileData(family.mother(), family.father(), family.familyName(), family.clan(), notes.getOrDefault(playerName, ""));
	}

	public static void save(String playerName, PlayerProfileData data) {
		load();
		families.put(playerName, new Family(data.getMother(), data.getFather(), data.getFamilyName(), data.getClan()));
		notes.put(playerName, data.getNote());
		writeFiles();
	}

	private static void load() {
		if (loaded) {
			return;
		}

		loaded = true;
		readFamilies();
		readNotes();
	}

	private static void readFamilies() {
		if (!Files.exists(FAMILY_PATH)) {
			return;
		}

		try {
			for (String line : Files.readAllLines(FAMILY_PATH, StandardCharsets.UTF_8)) {
				String[] parts = line.split("\t", -1);
				if (parts.length >= 3) {
					families.put(unescape(parts[0]), new Family(
							unescape(parts[1]),
							unescape(parts[2]),
							parts.length >= 4 ? unescape(parts[3]) : "",
							parts.length >= 5 ? unescape(parts[4]) : ""
					));
				}
			}
		} catch (IOException exception) {
			ChatPlusMod.LOGGER.warn("Failed to read player family data", exception);
		}
	}

	private static void readNotes() {
		if (!Files.exists(NOTES_PATH)) {
			return;
		}

		try {
			for (String line : Files.readAllLines(NOTES_PATH, StandardCharsets.UTF_8)) {
				String[] parts = line.split("\t", 2);
				if (parts.length == 2) {
					notes.put(unescape(parts[0]), unescape(parts[1]));
				}
			}
		} catch (IOException exception) {
			ChatPlusMod.LOGGER.warn("Failed to read player notes", exception);
		}
	}

	private static void writeFiles() {
		try {
			Files.createDirectories(CONFIG_DIR);
			Files.write(FAMILY_PATH, familyLines(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			Files.write(NOTES_PATH, noteLines(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException exception) {
			ChatPlusMod.LOGGER.warn("Failed to write player profile data", exception);
		}
	}

	private static List<String> familyLines() {
		return families.entrySet().stream()
				.map(entry -> escape(entry.getKey())
						+ "\t" + escape(entry.getValue().mother())
						+ "\t" + escape(entry.getValue().father())
						+ "\t" + escape(entry.getValue().familyName())
						+ "\t" + escape(entry.getValue().clan()))
				.toList();
	}

	private static List<String> noteLines() {
		return notes.entrySet().stream()
				.map(entry -> escape(entry.getKey()) + "\t" + escape(entry.getValue()))
				.toList();
	}

	private static String escape(String value) {
		return value.replace("\\", "\\\\").replace("\t", "\\t").replace("\r", "\\r").replace("\n", "\\n");
	}

	private static String unescape(String value) {
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

	private record Family(String mother, String father, String familyName, String clan) {
	}
}
