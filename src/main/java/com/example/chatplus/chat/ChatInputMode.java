package com.example.chatplus.chat;

public enum ChatInputMode {
	SERVER(">", ""),
	ALL("All", ""),
	GENERAL("General", ""),
	LOCAL("Local", "/lc "),
	TEAM("Team", "/tc "),
	COREPROTECT("CoreProtect", "");

	private static ChatInputMode selected = ALL;

	private final String label;
	private final String prefix;

	ChatInputMode(String label, String prefix) {
		this.label = label;
		this.prefix = prefix;
	}

	public String getLabel() {
		return label;
	}

	public String getPrefix() {
		return prefix;
	}

	public boolean hasPrefix() {
		return !prefix.isEmpty();
	}

	public static ChatInputMode getSelected() {
		return selected;
	}

	public static void setSelected(ChatInputMode mode) {
		selected = mode;
	}

	public static String applyPrefix(String message) {
		if (message == null) {
			return null;
		}

		ChatInputMode mode = getSelected();
		String trimmed = message.trim();
		if (!mode.hasPrefix() || trimmed.isEmpty() || trimmed.startsWith("/")) {
			return message;
		}

		return mode.getPrefix() + message;
	}
}
