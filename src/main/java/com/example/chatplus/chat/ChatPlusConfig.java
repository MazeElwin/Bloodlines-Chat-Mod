package com.example.chatplus.chat;

import com.example.chatplus.ChatPlusMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ChatPlusConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH = FabricLoader.getInstance()
			.getConfigDir()
			.resolve(ChatPlusMod.MOD_ID)
			.resolve("settings.json");

	private static Settings settings;

	private ChatPlusConfig() {
	}

	public static Settings get() {
		if (settings == null) {
			settings = load();
		}
		return settings;
	}

	public static void setTextSize(int textSize) {
		get().textSize = clamp(textSize, 50, 200);
		save();
	}

	public static void setLineSpacing(int lineSpacing) {
		get().lineSpacing = clamp(lineSpacing, 0, 6);
		save();
	}

	public static void setFontStyleIndex(int fontStyleIndex) {
		get().fontStyleIndex = clamp(fontStyleIndex, 0, 2);
		save();
	}

	public static void setBackgroundOpacity(int backgroundOpacity) {
		get().backgroundOpacity = clamp(backgroundOpacity, 1, 100);
		save();
	}

	public static void setHudBackgroundEnabled(boolean hudBackgroundEnabled) {
		get().hudBackgroundEnabled = hudBackgroundEnabled;
		save();
	}

	public static void setChatHeadsEnabled(boolean chatHeadsEnabled) {
		get().chatHeadsEnabled = chatHeadsEnabled;
		save();
	}

	public static void setTimestampsEnabled(boolean timestampsEnabled) {
		get().timestampsEnabled = timestampsEnabled;
		save();
	}

	public static void setChatStyleIndex(int chatStyleIndex) {
		get().chatStyleIndex = clamp(chatStyleIndex, 0, 1);
		save();
	}

	public static void setEmotesEnabled(boolean emotesEnabled) {
		get().emotesEnabled = emotesEnabled;
		save();
	}

	public static void setCoordinateWarningsEnabled(boolean coordinateWarningsEnabled) {
		get().coordinateWarningsEnabled = coordinateWarningsEnabled;
		save();
	}

	public static void setSkillTrackerMessagesInAll(boolean skillTrackerMessagesInAll) {
		get().skillTrackerMessagesInAll = skillTrackerMessagesInAll;
		save();
	}

	public static void setGhostTimerMode(int ghostTimerMode) {
		Settings current = get();
		current.ghostTimerMode = clamp(ghostTimerMode, 0, 2);
		current.ghostTimerEnabled = false;
		save();
	}

	public static void setGeneralColor(int generalColor) {
		get().generalColor = sanitizeColor(generalColor, 0xFFFFFFFF);
		save();
	}

	public static void setLocalColor(int localColor) {
		get().localColor = sanitizeColor(localColor, 0xFF55FFFF);
		save();
	}

	public static void setTeamColor(int teamColor) {
		get().teamColor = sanitizeColor(teamColor, 0xFFAA55FF);
		save();
	}

	public static void setWhisperColor(int whisperColor) {
		get().whisperColor = sanitizeColor(whisperColor, 0xFFAAAAAA);
		save();
	}

	public static void setSystemColor(int systemColor) {
		get().systemColor = sanitizeColor(systemColor, 0xFFFFFF55);
		save();
	}

	public static void setHistoryRetentionDays(int historyRetentionDays) {
		get().historyRetentionDays = clamp(historyRetentionDays, 0, 365);
		save();
	}

	public static void setWindowPosition(int x, int y) {
		Settings current = get();
		current.windowX = Math.max(0, x);
		current.windowY = y;
		save();
	}

	public static void setWindowSize(int width, int height) {
		Settings current = get();
		current.windowWidth = clamp(width, 150, 2000);
		current.windowHeight = clamp(height, 70, 1200);
		save();
	}

	public static void setShowWhispersInTab(ChatTabType tabType, boolean show) {
		Settings current = get();
		switch (tabType) {
			case GENERAL -> current.showWhispersInGeneral = show;
			case LOCAL_CHAT -> current.showWhispersInLocal = show;
			case TEAM_CHAT -> current.showWhispersInTeam = show;
			case SYSTEM -> current.showWhispersInSystem = show;
			case COREPROTECT -> {
			}
			default -> {
			}
		}
		save();
	}

	public static void setShowServerMessagesInTab(ChatTabType tabType, boolean show) {
		Settings current = get();
		switch (tabType) {
			case GENERAL -> current.showServerMessagesInGeneral = show;
			case LOCAL_CHAT -> current.showServerMessagesInLocal = show;
			case TEAM_CHAT -> current.showServerMessagesInTeam = show;
			case COREPROTECT -> {
			}
			default -> {
			}
		}
		save();
	}

	public static void setPrivateIndicatorPosition(int x, int y) {
		Settings current = get();
		current.privateIndicatorX = Math.max(0, x);
		current.privateIndicatorY = Math.max(0, y);
		save();
	}

	public static void save() {
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
				GSON.toJson(get(), writer);
			}
		} catch (IOException exception) {
			ChatPlusMod.LOGGER.warn("Failed to write chat settings", exception);
		}
	}

	private static Settings load() {
		if (!Files.exists(CONFIG_PATH)) {
			Settings defaults = new Settings();
			settings = defaults;
			save();
			return defaults;
		}

		try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
			JsonElement configJson = JsonParser.parseReader(reader);
			Settings loaded = GSON.fromJson(configJson, Settings.class);
			if (loaded == null) {
				return new Settings();
			}
			boolean migrateLegacyGhostTimer = !configJson.isJsonObject()
					|| !configJson.getAsJsonObject().has("ghostTimerMode");
			loaded.sanitize(migrateLegacyGhostTimer);
			settings = loaded;
			save();
			return loaded;
		} catch (IOException | JsonParseException exception) {
			ChatPlusMod.LOGGER.warn("Failed to read chat settings", exception);
			return new Settings();
		}
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private static int sanitizeColor(int color, int fallback) {
		int rgb = color & 0x00FFFFFF;
		if (rgb == 0) {
			return fallback;
		}
		return 0xFF000000 | rgb;
	}

	public static class Settings {
		public int textSize = 100;
		public int lineSpacing = 0;
		public int fontStyleIndex = 0;
		public int backgroundOpacity = 60;
		public boolean hudBackgroundEnabled = true;
		public int chatHeadMode = 1;
		public Boolean chatHeadsEnabled = true;
		public Boolean timestampsEnabled = false;
		public int chatStyleIndex = 0;
		public Boolean emotesEnabled = true;
		public Boolean coordinateWarningsEnabled = true;
		public Boolean skillTrackerMessagesInAll = false;
		public int ghostTimerMode = 0;
		public Boolean ghostTimerEnabled = false;
		public int generalColor = 0xFFFFFFFF;
		public int localColor = 0xFF55FFFF;
		public int teamColor = 0xFFAA55FF;
		public int whisperColor = 0xFFAAAAAA;
		public int systemColor = 0xFFFFFF55;
		public int historyRetentionDays = 0;
		public int windowX = 4;
		public int windowY = -1;
		public int windowWidth = 520;
		public int windowHeight = 170;
		public Boolean showWhispersInGeneral = true;
		public Boolean showWhispersInLocal = true;
		public Boolean showWhispersInTeam = true;
		public Boolean showWhispersInSystem = true;
		public Boolean showServerMessagesInGeneral = false;
		public Boolean showServerMessagesInLocal = false;
		public Boolean showServerMessagesInTeam = false;
		public int privateIndicatorX = 0;
		public int privateIndicatorY = 24;

		private void sanitize(boolean migrateLegacyGhostTimer) {
			textSize = clamp(textSize, 50, 200);
			lineSpacing = clamp(lineSpacing, 0, 6);
			if (chatStyleIndex == 1 && fontStyleIndex == 0) {
				fontStyleIndex = 2;
				chatStyleIndex = 0;
			}
			fontStyleIndex = clamp(fontStyleIndex, 0, 2);
			backgroundOpacity = clamp(backgroundOpacity, 1, 100);
			if (chatHeadsEnabled == null) {
				chatHeadsEnabled = true;
			}
			if (timestampsEnabled == null) {
				timestampsEnabled = false;
			}
			chatStyleIndex = clamp(chatStyleIndex, 0, 1);
			if (emotesEnabled == null) {
				emotesEnabled = true;
			}
			if (coordinateWarningsEnabled == null) {
				coordinateWarningsEnabled = true;
			}
			if (skillTrackerMessagesInAll == null) {
				skillTrackerMessagesInAll = false;
			}
			if (ghostTimerEnabled == null) {
				ghostTimerEnabled = false;
			}
			if (migrateLegacyGhostTimer && ghostTimerMode == 0 && ghostTimerEnabled) {
				ghostTimerMode = 1;
			}
			ghostTimerEnabled = false;
			ghostTimerMode = clamp(ghostTimerMode, 0, 2);
			chatHeadMode = chatHeadsEnabled ? 1 : 0;
			generalColor = sanitizeColor(generalColor, 0xFFFFFFFF);
			localColor = sanitizeColor(localColor, 0xFF55FFFF);
			teamColor = sanitizeColor(teamColor, 0xFFAA55FF);
			whisperColor = sanitizeColor(whisperColor, 0xFFAAAAAA);
			systemColor = sanitizeColor(systemColor, 0xFFFFFF55);
			historyRetentionDays = clamp(historyRetentionDays, 0, 365);
			windowX = Math.max(0, windowX);
			windowWidth = clamp(windowWidth, 150, 2000);
			windowHeight = clamp(windowHeight, 70, 1200);
			if (showWhispersInGeneral == null) {
				showWhispersInGeneral = true;
			}
			if (showWhispersInLocal == null) {
				showWhispersInLocal = true;
			}
			if (showWhispersInTeam == null) {
				showWhispersInTeam = true;
			}
			if (showWhispersInSystem == null) {
				showWhispersInSystem = true;
			}
			if (showServerMessagesInGeneral == null) {
				showServerMessagesInGeneral = false;
			}
			if (showServerMessagesInLocal == null) {
				showServerMessagesInLocal = false;
			}
			if (showServerMessagesInTeam == null) {
				showServerMessagesInTeam = false;
			}
			privateIndicatorX = Math.max(0, privateIndicatorX);
			privateIndicatorY = Math.max(0, privateIndicatorY);
		}
	}
}
